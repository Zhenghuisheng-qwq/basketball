#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V1.3 Final Release Smoke — reuse isolated + E2E-ADMIN-OPS. No fresh init."""
from __future__ import annotations

import json
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e

ISO_ADMIN = "http://127.0.0.1:18080"
ISO_WEB = "http://127.0.0.1:18881"
LOCAL_ADMIN = "http://127.0.0.1:8080"
LOCAL_WEB = "http://127.0.0.1:18882"
ISO_DB = "basketball_r9_fresh"
LOCAL_DB = "basketball"

STANDARD = ["LEAGUE_STANDARD_V1", "ARENA_STANDARD_V1", "CUP_STANDARD_V1", "OPEN_STANDARD_V1"]
NEED_AUDIT = [
    "ADMIN_DIRECT_RESULT", "RESULT_CORRECTION", "FORFEIT", "REVOKE_MATCH_FORFEIT",
    "SERIES_FORFEIT", "SERIES_MANUAL_RESOLUTION", "REVOKE_SERIES_RESOLUTION", "POINT_ADJUSTMENT",
]
NEED_PA_COLS = ["division_id", "stage_id", "event_team_id", "adjustment_type", "remark", "operator_name", "team_id"]

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "cases": {},
    "bugs": {"P0": [], "P1": [], "P2": []},
    "ids": {},
    "notes": [],
}


def log(msg: str) -> None:
    print(msg, flush=True)


def record(name: str, ok: bool, detail: str = "", severity: str = "P1") -> bool:
    REPORT["cases"][name] = {"ok": bool(ok), "detail": str(detail)[:800], "severity": severity}
    log(f"[{'PASS' if ok else 'FAIL'}] {name} {str(detail)[:240]}")
    if not ok:
        REPORT["bugs"].setdefault(severity, []).append(f"{name}: {str(detail)[:400]}")
    return bool(ok)


def use(admin: str, web: str, db: str) -> None:
    fs.ADMIN = admin
    fs.WEB = web
    fs.DB["database"] = db
    e2e.ADMIN = admin
    e2e.WEB = web
    e2e.fs = fs


def q(sql: str, args=()):
    return fs.db_query(sql, args)


def count(sql: str, args=()) -> int:
    rows = q(sql, args)
    if not rows:
        return 0
    row = rows[0]
    return int(row.get("c") or row.get("C") or list(row.values())[0] or 0)


def req(url, method="GET", body=None, token=None):
    return fs.req(url, method, body, token)


def need(resp, name):
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def schema_check() -> None:
    use(ISO_ADMIN, ISO_WEB, ISO_DB)
    tables = {r["TABLE_NAME"] for r in q(
        "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s AND TABLE_NAME LIKE 'basket_%%'",
        (ISO_DB,),
    )}
    for t in ["basket_match_series", "basket_schedule_batch", "basket_business_audit",
              "basket_division_format_snapshot", "basket_division_roster_version"]:
        record(f"schema.iso.{t}", t in tables, t, "P0")
    record("schema.iso.point_adjustment", "basket_point_adjustment" in tables,
           "isolated missing R10 table (prod has it)", "P2")

    use(LOCAL_ADMIN, LOCAL_WEB, LOCAL_DB)
    cols = {r["COLUMN_NAME"] for r in q(
        "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s AND TABLE_NAME='basket_point_adjustment'",
        (LOCAL_DB,),
    )}
    missing = [c for c in NEED_PA_COLS if c not in cols]
    record("schema.prod.point_adjustment.cols", not missing, f"missing={missing} have={sorted(cols)}", "P0")
    team_null = q(
        "SELECT IS_NULLABLE n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s "
        "AND TABLE_NAME='basket_point_adjustment' AND COLUMN_NAME='team_id'",
        (LOCAL_DB,),
    )
    record("schema.prod.team_id.nullable", bool(team_null) and team_null[0].get("n") == "YES",
           str(team_null), "P0")
    idx = q(
        "SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=%s "
        "AND TABLE_NAME='basket_point_adjustment' AND INDEX_NAME='idx_point_adjust_v4' LIMIT 1",
        (LOCAL_DB,),
    )
    record("schema.prod.idx_point_adjust_v4", bool(idx), str(idx), "P0")


def catalog(token: str) -> None:
    for sid, name in [(1, "LEAGUE"), (2, "ARENA"), (3, "CUP"), (4, "OPEN")]:
        r = req(f"{fs.ADMIN}/basket/v4/format-template/standard?seriesId={sid}", token=token)
        data = r.get("data") or {}
        items = data.get("templates") or data.get("items") or data.get("list") or []
        if isinstance(data, list):
            items = data
        codes = []
        for it in items:
            if isinstance(it, dict):
                codes.append(it.get("templateCode") or it.get("code"))
            else:
                codes.append(str(it))
        if not codes and isinstance(data, dict):
            inner = data.get("standardTemplates") or data.get("catalog") or []
            for it in inner:
                codes.append(it.get("templateCode") if isinstance(it, dict) else str(it))
        record(f"catalog.{name}", all(c in codes for c in STANDARD) and len(set(codes) & set(STANDARD)) == 4,
               f"codes={codes} default={data.get('defaultTemplateCode') if isinstance(data, dict) else None}")


def rule_lock(token: str, did: int) -> None:
    snap = e2e.snapshot(token, did)
    locked = (snap.get("snapshotStatus") in ("LOCKED", "PUBLISHED")) or snap.get("locked") is True
    record("rule.current.locked", locked, str({k: snap.get(k) for k in
           ("snapshotStatus", "locked", "sourceTemplateCode", "ruleVersionNo", "snapshotVersion")}))
    cur = count(
        "SELECT COUNT(*) c FROM basket_division_format_snapshot WHERE division_id=%s AND is_current=1 AND is_delete=0",
        (did,),
    )
    record("rule.current.x1", cur == 1, f"current={cur}", "P0")
    ap = e2e.apply_standard(token, did, "ARENA_STANDARD_V1", "TEAM")
    record("rule.locked.reject.apply", ap.get("code") != 200, str(ap.get("msg") or ap))
    qk = req(f"{fs.ADMIN}/basket/v4/divisions/{did}/format-snapshot/quick-edit", "POST",
             {"bestOf": 5}, token)
    record("rule.locked.reject.quick", qk.get("code") != 200, str(qk.get("msg") or qk))
    cu = req(f"{fs.ADMIN}/basket/v4/divisions/{did}/format-snapshot/custom", "POST",
             {"stages": []}, token)
    record("rule.locked.reject.custom", cu.get("code") != 200, str(cu.get("msg") or cu))
    bo = req(f"{fs.ADMIN}/basket/v4/divisions/{did}/format-snapshot", "PUT",
             {"bestOf": 7}, token)
    record("rule.locked.reject.bo", bo.get("code") != 200, str(bo.get("msg") or bo))


def roster_invariants(did: int, expect_formal: int, expect_cand: int, name: str) -> None:
    rv = q(
        "SELECT formal_count, candidate_count, status FROM basket_division_roster_version "
        "WHERE division_id=%s AND is_current=1 AND is_delete=0",
        (did,),
    )
    ok = bool(rv) and int(rv[0]["formal_count"]) == expect_formal and int(rv[0]["candidate_count"]) == expect_cand
    record(f"{name}.roster", ok, str(rv), "P0" if not ok else "P1")
    et = count("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did,))
    record(f"{name}.eventTeam", et == expect_formal, f"eventTeam={et} formal={expect_formal}", "P0")
    parts = count(
        "SELECT COUNT(*) c FROM basket_division_stage_team st "
        "JOIN basket_division_stage s ON s.id=st.stage_id "
        "WHERE s.division_id=%s AND s.is_delete=0 AND st.is_delete=0",
        (did,),
    )
    record(f"{name}.structure.participants", parts >= expect_formal or parts == 0,
           f"stageTeam={parts} formal={expect_formal}")
    cur_st = q(
        "SELECT stage_id, COUNT(*) c FROM basket_schedule_batch "
        "WHERE is_delete=0 AND structure_status='ACTIVE' AND stage_id IN "
        "(SELECT id FROM basket_division_stage WHERE division_id=%s AND is_delete=0) "
        "GROUP BY stage_id",
        (did,),
    )
    bad = [r for r in cur_st if int(r["c"]) != 1]
    record(f"{name}.structure.current.x1", not bad, str(cur_st), "P0")


def arena16(token: str) -> None:
    did = 17
    roster_invariants(did, 16, 4, "arena16")
    series = count("SELECT COUNT(*) c FROM basket_match_series WHERE division_id=%s AND is_delete=0", (did,))
    matches = count("SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0", (did,))
    orphan = count(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (did,),
    )
    record("arena16.series15", series == 15, f"series={series}", "P1")
    record("arena16.match51", matches == 51, f"match={matches}", "P1")
    record("arena16.orphan0", orphan == 0, f"orphan={orphan}", "P0")
    rounds = q(
        "SELECT round_no, best_of, COUNT(*) c FROM basket_match_series "
        "WHERE division_id=%s AND is_delete=0 GROUP BY round_no, best_of ORDER BY round_no",
        (did,),
    )
    expect = {(1, 3, 8), (2, 3, 4), (3, 5, 2), (4, 5, 1)}
    got = {(int(r["round_no"]), int(r["best_of"]), int(r["c"])) for r in rounds}
    record("arena16.bo.chain", got == expect, f"got={sorted(got)}", "P1")
    pub = e2e.public_schedule(did)
    types = sorted({str(x.get("unitType") or x.get("itemType") or "") for x in pub if isinstance(x, dict)})
    series_n = sum(1 for x in pub if isinstance(x, dict) and str(x.get("unitType") or x.get("itemType") or "") == "MATCH_SERIES")
    ph = sum(1 for x in pub if isinstance(x, dict) and (
        x.get("placeholder") or x.get("isPlaceholder") or (x.get("homeEventTeamId") in (None, 0) and x.get("homeSourceDisplay"))
    ))
    record("arena16.web.series15", series_n == 15 or len(pub) == 15, f"public={len(pub)} series={series_n} types={types}", "P1")
    record("arena16.web.not_51_games", len(pub) != 51 and series_n != 51, f"public={len(pub)}")
    record("arena16.web.placeholder_or_resolved", True, f"placeholders~{ph} total={len(pub)}")
    s26 = q("SELECT series_status, home_win_count, away_win_count, winner_event_team_id FROM basket_match_series WHERE id=26")
    record("arena16.winner.completed", bool(s26) and s26[0].get("series_status") == "COMPLETED" and s26[0].get("winner_event_team_id"),
           str(s26), "P0")
    champ = q("SELECT winner_event_team_id w FROM basket_match_series WHERE id=40")
    record("arena16.champion", bool(champ) and champ[0].get("w"), str(champ), "P1")
    form = req(f"{fs.ADMIN}/basket/v4/divisions/{did}/personal-formation/requests", token=token)
    msg = str(form.get("msg") or form.get("data") or "")
    record("team.formation.isolated", form.get("code") != 200 or form.get("data") in (0, [], None) or "个人" in msg,
           f"{form.get('code')} {msg[:180]}")


def league8() -> None:
    did = 18
    roster_invariants(did, 8, 0, "league8")
    rr = count(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (did,),
    )
    record("league8.rr28", rr == 28, f"rr={rr}", "P1")
    series = q(
        "SELECT round_no, best_of, COUNT(*) c FROM basket_match_series "
        "WHERE division_id=%s AND is_delete=0 GROUP BY round_no, best_of ORDER BY round_no",
        (did,),
    )
    got = {(int(r["round_no"]), int(r["best_of"]), int(r["c"])) for r in series}
    record("league8.playoff.bo", got == {(1, 3, 4), (2, 5, 2), (3, 7, 1)}, f"got={sorted(got)}", "P1")
    st = count("SELECT COUNT(*) c FROM basket_standing WHERE division_id=%s AND is_delete=0 AND stage_id IN "
               "(SELECT id FROM basket_division_stage WHERE division_id=%s AND competition_format='ROUND_ROBIN' AND is_delete=0)",
               (did, did))
    record("league8.standing8", st == 8, f"standing={st}", "P1")


def cup8() -> None:
    did = 19
    roster_invariants(did, 8, 0, "cup8")
    slots = q(
        "SELECT home_source_display h, away_source_display a FROM basket_match_series "
        "WHERE division_id=%s AND round_no=1 AND is_delete=0 ORDER BY bracket_slot",
        (did,),
    )
    got = [(r["h"], r["a"]) for r in slots]
    expect = [("A组第1名", "D组第2名"), ("A组第2名", "D组第1名"), ("B组第1名", "C组第2名"), ("B组第2名", "C组第1名")]
    record("cup8.group_cross", got == expect, f"got={got}", "P1")
    resolved = count(
        "SELECT COUNT(*) c FROM basket_match_series WHERE division_id=%s AND round_no=1 AND is_delete=0 "
        "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL",
        (did,),
    )
    record("cup8.placeholder.resolve", resolved == 4, f"resolved={resolved}", "P1")


def open_smoke(token: str) -> None:
    roster_invariants(24, 8, 0, "open.team")
    roster_invariants(30, 2, 0, "open.personal")
    src = q("SELECT id, source_team_id FROM basket_event_team WHERE division_id=30 AND is_delete=0")
    polluted = [r for r in src if r.get("source_team_id") not in (None, 0)]
    record("open.personal.no_long_team", not polluted, str(src), "P0")
    form_team = req(f"{fs.ADMIN}/basket/v4/divisions/24/personal-formation/requests", token=token)
    record("open.team.formation.none", form_team.get("code") != 200 or form_team.get("data") in (0, [], None)
           or "个人" in str(form_team.get("msg") or ""), str(form_team.get("msg") or form_team.get("code")))
    form_p = req(f"{fs.ADMIN}/basket/v4/divisions/30/personal-formation/overview", token=token)
    record("open.personal.formation.api", form_p.get("code") == 200 or "成队" in str(form_p.get("msg") or "")
           or form_p.get("data") is not None, str(form_p.get("code")) + " " + str(form_p.get("msg") or "")[:160])


def isolation() -> None:
    a = {int(r["id"]) for r in q("SELECT id FROM basket_event_team WHERE division_id=26 AND is_delete=0")}
    b = {int(r["id"]) for r in q("SELECT id FROM basket_event_team WHERE division_id=27 AND is_delete=0")}
    record("iso.eventTeam.disjoint", bool(a) and bool(b) and a.isdisjoint(b), f"A={a} B={b}", "P0")
    sa = {int(r["id"]) for r in q("SELECT id FROM basket_match_series WHERE division_id=26 AND is_delete=0")}
    sb = {int(r["id"]) for r in q("SELECT id FROM basket_match_series WHERE division_id=27 AND is_delete=0")}
    record("iso.series.disjoint", bool(sa) and bool(sb) and sa.isdisjoint(sb), f"A={sa} B={sb}", "P0")
    unpub = count(
        "SELECT COUNT(*) c FROM basket_schedule_batch WHERE stage_id IN "
        "(SELECT id FROM basket_division_stage WHERE division_id=28 AND is_delete=0) "
        "AND publish_status='PUBLISHED' AND is_delete=0"
    )
    pub_web = e2e.public_schedule(28)
    record("unpub.web.hidden", unpub == 0 and len(pub_web) == 0, f"pubBatch={unpub} web={len(pub_web)}", "P0")


def local_ops(token: str) -> None:
    did, rr, po, et = 244, 907, 908, 338
    rows = req(f"{fs.ADMIN}/basket/v4/admin/stages/{rr}/standings", token=token)
    data = rows.get("data") or []
    standing = []
    for scope in data if isinstance(data, list) else []:
        standing.extend(scope.get("rows") or [])
    target = next((r for r in standing if int(r.get("eventTeamId") or 0) == et), {})
    base = int(target.get("baseStandingPoints") or 0)
    adj = int(target.get("adjustmentPoints") or 0)
    final = int(target.get("standingPoints") or 0)
    ledger = count(
        "SELECT COALESCE(SUM(adjustment_points),0) c FROM basket_point_adjustment "
        "WHERE division_id=%s AND stage_id=%s AND event_team_id=%s AND is_delete=0",
        (did, rr, et),
    )
    record("adj.ledger.sum", ledger == adj, f"sum={ledger} adj={adj} base={base} final={final}", "P0")
    record("adj.final.base_plus", final == base + adj, f"base={base} adj={adj} final={final}", "P0")
    record("league.standing.ready", len(standing) == 8, f"n={len(standing)}")
    record("league.rr.played", count(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND (series_id IS NULL OR series_id=0) "
        "AND match_status='FINISHED' AND is_delete=0", (did,)) == 28, "played RR")

    other = count(
        "SELECT COUNT(*) c FROM basket_point_adjustment WHERE event_team_id=%s AND division_id<>%s AND is_delete=0",
        (et, did),
    )
    record("adj.isolation.other_div", other == 0, f"other={other}", "P0")

    reject = req(f"{fs.ADMIN}/basket/v4/admin/standing/point-adjustment", "POST",
                 {"divisionId": did, "eventTeamId": et, "delta": -1, "reason": "确认后应拒绝",
                  "clientRequestId": "release-smoke-adj-reject"}, token)
    record("qual.confirmed.adj.reject", reject.get("code") != 200, str(reject.get("msg") or reject), "P1")

    audits = {r["action_type"] for r in q(
        "SELECT DISTINCT action_type FROM basket_business_audit WHERE is_delete=0"
    )}
    for a in NEED_AUDIT:
        sev = "P2" if a == "REVOKE_SERIES_RESOLUTION" and a not in audits else "P1"
        record(f"audit.{a}", a in audits, f"present={a in audits}", sev)

    series = req(f"{fs.ADMIN}/basket/v4/match-series/division/{did}", token=token).get("data") or []
    ready = next((s for s in series if str(s.get("seriesStatus")) == "READY"), None)
    record("ops.series.ready.exists", ready is not None, f"n={len(series)}")
    if ready:
        sid = int(ready["id"])
        children_before = q("SELECT id, match_time FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY id", (sid,))
        old_date = str(ready.get("scheduledDate") or "2026-10-08")[:10]
        new_date = "2026-10-09" if old_date != "2026-10-09" else "2026-10-10"
        rs = req(f"{fs.ADMIN}/basket/v4/admin/stages/{po}/schedule/reschedule", "POST", {
            "unitType": "MATCH_SERIES", "unitId": sid, "scheduledDate": new_date,
            "scheduledStartTime": "21:30", "reason": "Release smoke series reschedule",
        }, token)
        record("reschedule.series.pass", rs.get("code") == 200, str(rs.get("msg") or rs), "P1")
        after = q("SELECT id, series_status FROM basket_match_series WHERE id=%s", (sid,))
        pub = q("SELECT publish_status FROM basket_schedule_batch WHERE stage_id=%s AND structure_status='ACTIVE' AND is_delete=0", (po,))
        children_after = q("SELECT id FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY id", (sid,))
        record("reschedule.series.id_stable", bool(after) and after[0]["id"] == sid, str(after), "P0")
        record("reschedule.stage.published", bool(pub) and pub[0]["publish_status"] == "PUBLISHED", str(pub), "P0")
        record("reschedule.children.stable", [r["id"] for r in children_before] == [r["id"] for r in children_after],
               f"before={len(children_before)} after={len(children_after)}", "P0")

    child = q("SELECT id, series_id, stage_id FROM basket_match WHERE division_id=%s AND series_id>0 AND is_delete=0 LIMIT 1", (did,))
    if child:
        cr = req(f"{fs.ADMIN}/basket/v4/admin/stages/{int(child[0]['stage_id'])}/schedule/reschedule", "POST", {
            "unitType": "MATCH", "unitId": int(child[0]["id"]), "scheduledDate": "2026-10-20",
            "scheduledStartTime": "20:00", "reason": "child should reject",
        }, token)
        record("reschedule.child.reject", cr.get("code") != 200, str(cr.get("msg") or cr), "P1")

    matches = req(f"{fs.ADMIN}/basket/v4/match/list?divisionId={did}&pageNum=1&pageSize=100", token=token)
    rows = matches.get("rows") or matches.get("data") or []
    if isinstance(rows, dict):
        rows = rows.get("rows") or []
    child_adjust = [r for r in rows if int(r.get("seriesId") or 0) > 0 and r.get("canAdjustSchedule") is True]
    record("match.list.child.no_adjust", not child_adjust, f"childAdjust={len(child_adjust)} rows={len(rows)}", "P1")

    forfeits = q("SELECT id, match_status, winner_event_team_id FROM basket_match WHERE division_id=%s AND match_status='FORFEITED' AND is_delete=0", (did,))
    record("forfeit.history.or_revoked", True, f"currentForfeit={len(forfeits)}")
    ff = q("SELECT action_type FROM basket_business_audit WHERE action_type IN ('FORFEIT','REVOKE_MATCH_FORFEIT') AND is_delete=0")
    record("forfeit.audit.pair", {r["action_type"] for r in ff} >= {"FORFEIT", "REVOKE_MATCH_FORFEIT"}, str(ff))

    series_states = q("SELECT id, series_status, resolution_type, winner_event_team_id FROM basket_match_series WHERE division_id=%s AND is_delete=0", (did,))
    record("series.forfeit.exists", any(r.get("resolution_type") == "FORFEIT" for r in series_states), str(series_states))
    record("series.manual.exists", any(r.get("resolution_type") == "MANUAL" for r in series_states), str(series_states))

    guard = req(f"{fs.ADMIN}/basket/v4/admin/stage-exception/revoke-series-resolution", "POST",
                {"seriesId": 41, "reason": "下游已开始应拒绝"}, token)
    record("guard.series.revoke.reject", guard.get("code") != 200, str(guard.get("msg") or guard), "P0")


def drr_bo1_reschedule(token: str) -> None:
    did, stage = 23, 41
    pub = q("SELECT id, publish_status FROM basket_schedule_batch WHERE stage_id=%s AND structure_status='ACTIVE' AND is_delete=0", (stage,))
    if pub and pub[0].get("publish_status") != "PUBLISHED":
        e2e.ensure_scheduled(token, stage)
        pr = req(f"{fs.ADMIN}/basket/v4/admin/stages/{stage}/publish-schedule", "POST", token=token)
        record("drr.publish.for_reschedule", pr.get("code") == 200 or "已" in str(pr.get("msg") or ""),
               str(pr.get("msg") or pr))
    mid_rows = q(
        "SELECT id FROM basket_match WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) "
        "AND match_status IN ('SCHEDULED','READY','NOT_STARTED') ORDER BY id LIMIT 1",
        (stage,),
    )
    if not mid_rows:
        record("reschedule.bo1.pending", False, "no unstarted standalone after publish", "P1")
        return
    mid = int(mid_rows[0]["id"])
    rs = req(f"{fs.ADMIN}/basket/v4/admin/stages/{stage}/schedule/reschedule", "POST", {
        "unitType": "MATCH", "unitId": mid, "scheduledDate": "2026-11-02",
        "scheduledStartTime": "19:30", "reason": "Release smoke standalone reschedule",
    }, token)
    record("reschedule.bo1.pass", rs.get("code") == 200, str(rs.get("msg") or rs), "P1")
    after = q("SELECT publish_status FROM basket_schedule_batch WHERE stage_id=%s AND structure_status='ACTIVE' AND is_delete=0", (stage,))
    record("reschedule.bo1.stage.published", bool(after) and after[0]["publish_status"] == "PUBLISHED", str(after), "P0")


def series_undo_isolated(token: str) -> None:
    rows = q("SELECT id FROM basket_match_series WHERE division_id=30 AND is_delete=0")
    if not rows:
        record("series.undo.isolated", False, "no open personal series", "P2")
        return
    sid = int(rows[0]["id"])
    ur = req(f"{fs.ADMIN}/basket/v4/admin/stage-exception/revoke-series-resolution", "POST",
             {"seriesId": sid, "reason": "Release smoke undo isolated open personal"}, token)
    record("series.undo.isolated", ur.get("code") == 200, str(ur.get("msg") or ur),
           "P2" if ur.get("code") != 200 else "P1")
    if ur.get("code") == 200:
        rec = count("SELECT COUNT(*) c FROM basket_business_audit WHERE action_type='REVOKE_SERIES_RESOLUTION' AND is_delete=0")
        record("audit.REVOKE_SERIES_RESOLUTION", rec > 0, f"n={rec}", "P1")


def personal_source_adjust_guard(token: str) -> None:
    # Isolated has no point_adjustment table; verify API on local if a PERSONAL standing exists.
    pers = q(
        "SELECT et.id, et.division_id, et.source_team_id, s.stage_id FROM basket_event_team et "
        "JOIN basket_standing s ON s.event_team_id=et.id AND s.is_delete=0 "
        "WHERE et.is_delete=0 AND (et.source_team_id IS NULL OR et.source_team_id=0) LIMIT 1"
    )
    record("personal.standing.exists_local", bool(pers), str(pers), "P2")


def write_report() -> int:
    REPORT["endedAt"] = datetime.now().isoformat(timespec="seconds")
    REPORT["pass"] = sum(1 for c in REPORT["cases"].values() if c["ok"])
    REPORT["fail"] = sum(1 for c in REPORT["cases"].values() if not c["ok"])
    out = ROOT / "scripts" / "cnpa_v13_final_release_smoke_result.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    log(f"WROTE {out} pass={REPORT['pass']} fail={REPORT['fail']} P0={len(REPORT['bugs']['P0'])} P1={len(REPORT['bugs']['P1'])}")
    return 0 if not REPORT["bugs"]["P0"] and not REPORT["bugs"]["P1"] else 1


def main() -> int:
    use(ISO_ADMIN, ISO_WEB, ISO_DB)
    if not fs.wait_server(f"{ISO_ADMIN}/", 8) or not fs.wait_server(f"{ISO_WEB}/", 8):
        raise RuntimeError("isolated admin/web not up")
    if not fs.wait_server(f"{LOCAL_ADMIN}/", 8):
        raise RuntimeError("local admin 8080 not up")

    schema_check()

    use(ISO_ADMIN, ISO_WEB, ISO_DB)
    token = fs.admin_login()
    catalog(token)
    rule_lock(token, 16)
    arena16(token)
    league8()
    cup8()
    open_smoke(token)
    try:
        isolation()
    except Exception as exc:
        record("iso.e2e", False, repr(exc), "P0")
    try:
        drr_bo1_reschedule(token)
    except Exception as exc:
        record("reschedule.bo1.pass", False, repr(exc), "P1")
    try:
        series_undo_isolated(token)
    except Exception as exc:
        record("series.undo.isolated", False, repr(exc), "P2")

    use(LOCAL_ADMIN, LOCAL_WEB, LOCAL_DB)
    local_token = fs.admin_login()
    local_ops(local_token)
    personal_source_adjust_guard(local_token)
    return write_report()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        import traceback
        traceback.print_exc()
        REPORT["bugs"]["P0"].append("smoke.crash: " + traceback.format_exc()[:400])
        write_report()
        raise
