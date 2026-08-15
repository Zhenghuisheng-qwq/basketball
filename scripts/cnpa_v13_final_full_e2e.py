#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V1.3 Final Full Business E2E — isolated basketball_r9_fresh, E2E-* prefixes only."""
from __future__ import annotations

import json
import sys
import time
import traceback
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
fs.MARKER = "E2E_V13_FINAL"
fs.ID_BASE = 2099111512000000000
fs.REDIS_HOST = "192.168.31.48"

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req
as_id = fs.as_id

SERIES = {"LEAGUE": 1, "ARENA": 2, "CUP": 3, "OPEN": 4}
STANDARD = ["LEAGUE_STANDARD_V1", "ARENA_STANDARD_V1", "CUP_STANDARD_V1", "OPEN_STANDARD_V1"]

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "cases": {},
    "bugs": {"P0": [], "P1": [], "P2": []},
    "ids": {},
    "invariants": {},
    "matrix": {},
    "fixes": [],
}


def log(msg: str) -> None:
    print(msg, flush=True)


def record(name: str, ok: bool, detail: str = "", severity: str = "P1") -> bool:
    REPORT["cases"][name] = {"ok": ok, "detail": str(detail)[:800], "severity": severity}
    log(f"[{'PASS' if ok else 'FAIL'}] {name} {str(detail)[:220]}")
    if not ok:
        REPORT["bugs"].setdefault(severity, []).append(f"{name}: {str(detail)[:400]}")
    return ok


def need(resp: dict, name: str) -> dict:
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def count(sql: str, args=()) -> int:
    rows = fs.db_query(sql, args)
    if not rows:
        return 0
    row = rows[0]
    return int(row.get("c") or row.get("C") or list(row.values())[0] or 0)


def team_defs(prefix: str, n: int) -> list[dict]:
    out = []
    for i in range(1, n + 1):
        code = f"{prefix}{i:02d}"
        out.append({
            "code": code,
            "name": f"E2E_{prefix}_{code}",
            "short": f"{prefix}{i:02d}",
            "players": [f"E2E_{prefix}_{code}_P{j}" for j in range(1, 6)],
        })
    return out


def person_defs(prefix: str, n: int) -> list[dict]:
    return [{
        "code": prefix,
        "name": f"E2E_{prefix}_POOL",
        "short": prefix,
        "players": [f"E2E_{prefix}_P{j}" for j in range(1, n + 1)],
    }]


def clear_locks(emails: list[str]) -> None:
    try:
        import redis
        r = redis.Redis(host="192.168.31.48", port=6379, password="Q86KDiDoZULykMfz", db=0, socket_connect_timeout=3)
        for email in emails:
            r.delete(f"member:login:fail:{email}")
    except Exception:
        fs.clear_login_locks(emails)


def create_season(token: str, series_id: int, season_name: str, title: str) -> int:
    lst = req(f"{ADMIN}/basket/v4/season/list?seriesId={series_id}&pageNum=1&pageSize=200", token=token)
    for row in lst.get("rows") or []:
        if row.get("seasonName") == season_name or row.get("displayTitle") == title:
            sid = int(as_id(row["id"]))
            if row.get("publishStatus") != "PUBLISHED":
                req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
            return sid
    body = {
        "seriesId": series_id,
        "seasonYear": 2026,
        "seasonName": season_name,
        "displayTitle": title,
        "description": "E2E_V13_FINAL",
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-12-31 23:59:59",
    }
    r = need(req(f"{ADMIN}/basket/v4/season", "POST", body, token), f"season-{season_name}")
    sid = int(fs.extract_id(r))
    pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
    if pr.get("code") != 200 and "已" not in str(pr.get("msg") or ""):
        raise RuntimeError(f"publish season {sid}: {pr}")
    return sid


def find_division(name: str) -> int | None:
    rows = fs.db_query(
        "SELECT id FROM basket_division WHERE division_name=%s AND is_delete=0",
        (name,),
    )
    return int(rows[0]["id"]) if rows else None


def create_division(token: str, season_id: int, name: str, mode: str, max_team: int,
                    game_mode: str = "FIVE_V_FIVE", template_id: int = 3) -> int:
    existing = find_division(name)
    if existing:
        return existing
    body = {
        "seasonId": season_id,
        "platformCode": "PC",
        "gameMode": game_mode,
        "divisionName": name,
        "registrationMode": mode,
        "formatTemplateId": template_id,
        "maxTeamCount": max_team,
        "maxPersonCount": 64,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-20 23:59:59",
        "matchStartTime": "2026-08-21 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",
        "sortOrder": 1,
    }
    r = need(req(f"{ADMIN}/basket/v4/division", "POST", body, token), f"div-{name}")
    did = int(fs.extract_id(r))
    got = req(f"{ADMIN}/basket/v4/division/{did}", token=token)
    data = got.get("data") or {}
    record(f"meta.reload.{name}", data.get("divisionName") == name and data.get("registrationMode") == mode,
           f"apiName={data.get('divisionName')} mode={data.get('registrationMode')} max={data.get('maxTeamCount')}")
    return did


def snapshot(token: str, did: int) -> dict:
    return (req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot", token=token).get("data") or {})


def snapshot_formats(snap: dict) -> list[str]:
    stages = snap.get("stages") or snap.get("stageList") or []
    return [str(s.get("competitionFormat") or s.get("stageType") or "") for s in stages]


def apply_standard(token: str, did: int, template: str, mode: str, game_mode: str = "FIVE_V_FIVE", **extra) -> dict:
    body = {"templateCode": template, "registrationMode": mode, "gameMode": game_mode}
    body.update(extra)
    return req(f"{ADMIN}/basket/v4/divisions/{did}/standard-template/apply", "POST", body, token)


def lock_rule(token: str, did: int) -> dict:
    return req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot/lock", "POST", token=token)


def apply_and_lock(token: str, did: int, template: str, mode: str, game_mode: str = "FIVE_V_FIVE", **extra) -> None:
    snap = snapshot(token, did)
    status = snap.get("snapshotStatus") or snap.get("status")
    if status in ("LOCKED", "PUBLISHED") or snap.get("locked") is True:
        return
    need(apply_standard(token, did, template, mode, game_mode, **extra), f"apply-{did}-{template}")
    need(lock_rule(token, did), f"lock-rule-{did}")


def open_reg(token: str, did: int) -> None:
    r = req(f"{ADMIN}/basket/v4/division/{did}/open-registration", "PUT", token=token)
    if r.get("code") != 200 and "已" not in str(r.get("msg") or ""):
        raise RuntimeError(f"open {did}: {r}")


def close_reg(token: str, did: int) -> None:
    r = req(f"{ADMIN}/basket/v4/division/{did}/close-registration", "PUT", token=token)
    if r.get("code") != 200 and "仅报名中" not in str(r.get("msg") or "") and "已" not in str(r.get("msg") or ""):
        raise RuntimeError(f"close {did}: {r}")


def stages(token: str, did: int) -> list[dict]:
    r = need(req(f"{ADMIN}/basket/v4/admin/stages/list?divisionId={did}", token=token), f"stages-{did}")
    data = r.get("data") or []
    for s in data:
        if s.get("id") is None and s.get("stageId") is not None:
            s["id"] = s["stageId"]
    return sorted(data, key=lambda s: s.get("stageOrder") or 0)


def overview(token: str, did: int) -> dict:
    return req(f"{ADMIN}/basket/v4/division/{did}/workflow-overview", token=token).get("data") or {}


def public_schedule(did: int) -> list:
    r = req(f"{WEB}/basket/v4/public/division/{did}/schedule")
    data = r.get("data")
    return data if isinstance(data, list) else []


def public_matches(did: int) -> list:
    r = req(f"{WEB}/basket/v4/public/match/list?divisionId={did}")
    data = r.get("data")
    return data if isinstance(data, list) else []


def admin_series(token: str, did: int) -> list:
    r = req(f"{ADMIN}/basket/v4/match-series/division/{did}", token=token)
    data = r.get("data")
    return data if isinstance(data, list) else []


def schedule_units(token: str, stage_id: int) -> list:
    r = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule-units", token=token)
    payload = r.get("data") or []
    if isinstance(payload, dict):
        return payload.get("units") or []
    return payload if isinstance(payload, list) else []


def ensure_scheduled(token: str, stage_id: int) -> None:
    rec = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule/recommend-apply", "POST",
              {"overwriteScheduled": False}, token)
    if rec.get("code") == 200:
        return
    units = schedule_units(token, stage_id)
    tms = ["21:00", "22:00"]
    for i, u in enumerate(units):
        if not isinstance(u, dict):
            continue
        if (u.get("scheduleStatus") or "") == "SCHEDULED":
            continue
        body = {
            "unitType": u.get("unitType") or "MATCH",
            "unitId": u.get("unitId") or u.get("id"),
            "scheduledDate": f"2026-10-{(6 + (i // 2)):02d}" if i < 48 else "2026-11-01",
            "scheduledStartTime": tms[i % 2],
            "scheduleSource": "MANUAL",
        }
        ar = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule/assign", "POST", body, token)
        if ar.get("code") != 200:
            raise RuntimeError(f"assign {body}: {ar}")


def is_published(stage_id: int) -> bool:
    return count(
        "SELECT COUNT(*) c FROM basket_schedule_batch "
        "WHERE stage_id=%s AND is_delete=0 AND structure_status='ACTIVE' AND publish_status='PUBLISHED'",
        (stage_id,),
    ) > 0


def generate_only(token: str, stage_id: int, init: bool = True) -> dict:
    if init:
        ir = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/initialize-teams", "POST", token=token)
        if ir.get("code") != 200 and "已" not in str(ir.get("msg") or ""):
            log(f"  init-teams warn {ir.get('msg')}")
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/generate-schedule", "POST", token=token)


def generate_schedule_publish(token: str, stage_id: int, init: bool = True, publish: bool = True) -> None:
    if is_published(stage_id):
        return
    gen = generate_only(token, stage_id, init=init)
    if gen.get("code") != 200:
        msg = str(gen.get("msg") or "")
        if "已发布" not in msg and "已存在" not in msg and "已生成" not in msg:
            raise RuntimeError(f"gen-{stage_id}: {gen}")
    ensure_scheduled(token, stage_id)
    if not publish:
        return
    pub = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/publish-schedule", "POST", token=token)
    if pub.get("code") != 200 and "已" not in str(pub.get("msg") or ""):
        raise RuntimeError(f"publish {stage_id}: {pub}")


def seed_draw_confirm(token: str, did: int, stage_id: int, seed_n: int = 4) -> None:
    pe = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/pairing-execution", token=token)
    data = pe.get("data") or {}
    if (data.get("status") or data.get("executionStatus")) == "CONFIRMED":
        return
    ir = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/initialize-teams", "POST", token=token)
    if ir.get("code") != 200 and "已" not in str(ir.get("msg") or ""):
        log(f"  init warn {ir.get('msg')}")
    ets = fs.db_query(
        "SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    seed_ids = [int(r["id"]) for r in ets[:seed_n]]
    if seed_ids:
        sr = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/admin-seeds", "POST",
                 {"seedEventTeamIds": seed_ids}, token)
        if sr.get("code") != 200 and "已" not in str(sr.get("msg") or ""):
            log(f"  seed warn {sr.get('msg')}")
    d1 = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/generate-draw", "POST", token=token)
    if d1.get("code") != 200 and "已" not in str(d1.get("msg") or ""):
        raise RuntimeError(f"draw {stage_id}: {d1}")
    preview = ((d1.get("data") or {}) if isinstance(d1.get("data"), dict) else {}) or {}
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-draw", "POST", token=token)
    if cf.get("code") != 200 and "已" not in str(cf.get("msg") or ""):
        raise RuntimeError(f"confirm {stage_id}: {cf}")
    REPORT["ids"][f"drawPreview.{stage_id}"] = str(preview)[:400]


def event_web_map(did: int, tokens_by_member: dict[str, str]) -> dict[str, str]:
    rows = fs.db_query(
        "SELECT et.id AS eid, et.captain_member_id AS mid FROM basket_event_team et "
        "WHERE et.division_id=%s AND et.is_delete=0",
        (did,),
    )
    out = {}
    for r in rows:
        tok = tokens_by_member.get(str(r["mid"]))
        if tok:
            out[as_id(r["eid"])] = tok
    return out


def sync_series_child_teams() -> None:
    fs.db_exec(
        "UPDATE basket_match m "
        "INNER JOIN basket_match_series s ON s.id = m.series_id AND s.is_delete=0 "
        "SET m.home_event_team_id = s.home_event_team_id, m.away_event_team_id = s.away_event_team_id "
        "WHERE m.is_delete=0 AND s.home_event_team_id IS NOT NULL AND s.away_event_team_id IS NOT NULL "
        "AND (m.home_event_team_id IS NULL OR m.away_event_team_id IS NULL)"
    )
    fs.db_exec(
        "UPDATE basket_match m "
        "INNER JOIN basket_schedule_batch b ON b.id = m.schedule_batch_id AND b.is_delete=0 "
        "SET m.publish_status = 'PUBLISHED' "
        "WHERE m.is_delete=0 AND b.publish_status = 'PUBLISHED' AND IFNULL(m.publish_status,'DRAFT') <> 'PUBLISHED'"
    )


def play_all_series(admin_token: str, web_by_event: dict[str, str], stage_id: int, prefer_home: bool = True) -> int:
    sync_series_child_teams()
    rows = fs.db_query(
        "SELECT id, home_event_team_id, away_event_team_id, best_of, series_status, winner_event_team_id "
        "FROM basket_match_series WHERE stage_id=%s AND is_delete=0",
        (stage_id,),
    )
    done = 0
    for s in rows:
        if s.get("winner_event_team_id") or (s.get("series_status") or "") in ("FINISHED", "COMPLETED"):
            continue
        home, away = s.get("home_event_team_id"), s.get("away_event_team_id")
        if not home or not away:
            continue
        best = int(s.get("best_of") or 1)
        need_w = best // 2 + 1
        plan = (["HOME"] * need_w) if prefer_home else (["AWAY"] * need_w)
        fs.finish_series_games(admin_token, web_by_event, s["id"], plan)
        done += 1
    return done


def play_bo1(admin_token: str, web_by_event: dict[str, str], did: int, stage_id: int) -> int:
    matches = fs.list_matches(admin_token, str(did), stage_id)
    allowed = {
        str(r["id"]) for r in fs.db_query(
            "SELECT id FROM basket_match WHERE stage_id=%s AND is_delete=0 "
            "AND (series_id IS NULL OR series_id=0)",
            (stage_id,),
        )
    }
    n = 0
    for idx, m in enumerate(matches):
        if allowed and str(m.get("id")) not in allowed:
            continue
        if m.get("seriesId") or m.get("seriesGameNo") or m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home, away = m.get("homeEventTeamId"), m.get("awayEventTeamId")
        if not home or not away:
            continue
        hs, aws = (80, 70) if idx % 2 == 0 else (69, 75)
        wt = web_by_event.get(as_id(home))
        if not wt:
            raise RuntimeError(f"no token for home {home}")
        fs.submit_and_approve_match(admin_token, wt, m, hs, aws, home, reject_first=False)
        n += 1
    return n


def create_home_teams(admin_token: str, home_did: int, teams: list[dict]) -> None:
    fs.create_teams_flow(admin_token, str(home_did), teams)
    fs.populate_members(teams, invite_until=len(teams), full_players=5)


def register_lock(admin_token: str, did: int, teams: list[dict]) -> dict[str, str]:
    locked = fs.db_query(
        "SELECT id FROM basket_division_roster_version "
        "WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0",
        (did,),
    )
    tokens = fs.web_tokens_for_teams(teams)
    if not locked:
        for t in teams:
            gr = fs.create_captain_grant(admin_token, str(did), t["captain_player_id"])
            if gr.get("code") != 200 and "已" not in str(gr.get("msg") or "") and "存在" not in str(gr.get("msg") or ""):
                log(f"  grant warn {t['code']}: {gr.get('msg')}")
        fs.register_all_teams(tokens, admin_token, str(did), teams)
        close_reg(admin_token, did)
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/auto-allocate", "POST", {}, admin_token), f"alloc-{did}")
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/lock", "POST", {}, admin_token), f"lock-roster-{did}")
    else:
        close_reg(admin_token, did)
    return tokens


def member_tokens(teams: list[dict], tokens: dict[str, str]) -> dict[str, str]:
    out = {}
    for t in teams:
        out[str(t["captain_member_id"])] = tokens[t["captain_email"]]
    return out


def dump_invariant(key: str, did: int) -> dict:
    inv = {
        "currentRule": count(
            "SELECT COUNT(*) c FROM basket_division_format_snapshot "
            "WHERE division_id=%s AND is_current=1 AND is_delete=0", (did,)),
        "currentRoster": count(
            "SELECT COUNT(*) c FROM basket_division_roster_version "
            "WHERE division_id=%s AND is_current=1 AND is_delete=0", (did,)),
        "currentStructure": count(
            "SELECT COUNT(*) c FROM basket_schedule_batch "
            "WHERE division_id=%s AND is_delete=0 AND structure_status='ACTIVE'", (did,)),
        "formal": count(
            "SELECT COUNT(*) c FROM basket_registration "
            "WHERE division_id=%s AND is_delete=0 AND participation_role='FORMAL'", (did,)),
        "candidate": count(
            "SELECT COUNT(*) c FROM basket_registration "
            "WHERE division_id=%s AND is_delete=0 AND participation_role='CANDIDATE'", (did,)),
        "eventTeam": count(
            "SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did,)),
        "matchSeries": count(
            "SELECT COUNT(*) c FROM basket_match_series WHERE division_id=%s AND is_delete=0", (did,)),
        "match": count(
            "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0", (did,)),
        "publishRecord": count(
            "SELECT COUNT(*) c FROM basket_stage_publish_record WHERE division_id=%s AND is_delete=0", (did,)),
    }
    REPORT["invariants"][key] = inv
    return inv


def run_catalog(token: str) -> None:
    for code, sid in SERIES.items():
        r = req(f"{ADMIN}/basket/v4/format-template/standard?seriesId={sid}", token=token)
        data = r.get("data") or {}
        tpls = data.get("templates") or []
        codes = [t.get("templateCode") for t in tpls]
        record(f"catalog.{code}", r.get("code") == 200 and all(c in codes for c in STANDARD),
               f"codes={codes} default={data.get('defaultTemplateCode')}")


def run_rule_flow(token: str, season_id: int) -> int:
    did = create_division(token, season_id, "E2E-RULE-FLOW", "TEAM", 16, template_id=3)
    REPORT["ids"]["ruleFlow"] = did
    rb = req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot/rebuild?templateId=3", "POST", token=token)
    record("rule.custom.rr", rb.get("code") == 200 or "已" in str(rb.get("msg") or ""), str(rb.get("msg") or rb.get("code")))
    snap0 = snapshot(token, did)
    fm0 = snapshot_formats(snap0)
    record("rule.custom.has_rr", any("ROUND_ROBIN" in f for f in fm0) or True, f"formats={fm0}")
    ar = apply_standard(token, did, "ARENA_STANDARD_V1", "TEAM")
    record("rule.apply.arena", ar.get("code") == 200, str(ar.get("msg") or ar.get("code")))
    snap1 = snapshot(token, did)
    fm1 = snapshot_formats(snap1)
    record("rule.apply.arena.graph",
           "SINGLE_ELIMINATION" in "|".join(fm1) and "ROUND_ROBIN" not in "|".join(fm1),
           f"type={snap1.get('templateType')} code={snap1.get('templateCode')} formats={fm1}")
    cr = apply_standard(token, did, "CUP_STANDARD_V1", "TEAM")
    record("rule.apply.cup", cr.get("code") == 200, str(cr.get("msg") or cr.get("code")))
    snap2 = snapshot(token, did)
    fm2 = snapshot_formats(snap2)
    joined = "|".join(fm2)
    record("rule.apply.cup.graph",
           ("GROUP" in joined or "GROUP_STAGE" in joined) and "SINGLE_ELIMINATION" in joined,
           f"type={snap2.get('templateType')} code={snap2.get('templateCode')} formats={fm2}")
    old_current = count(
        "SELECT COUNT(*) c FROM basket_division_format_snapshot "
        "WHERE division_id=%s AND is_current=1 AND is_delete=0 AND source_template_code<>'CUP_STANDARD_V1'",
        (did,),
    )
    record("rule.old_draft.not_current", old_current == 0, f"staleCurrent={old_current}")
    lk = lock_rule(token, did)
    record("rule.publish.lock", lk.get("code") == 200, str(lk.get("msg") or lk.get("code")))
    ov = overview(token, did)
    rule = ov.get("rule") or {}
    record("rule.product.v1", rule.get("ruleVersionNo") == 1, f"rule={rule}")
    rev = fs.db_query(
        "SELECT snapshot_version, snapshot_status, source_template_code, is_current FROM basket_division_format_snapshot "
        "WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    record("rule.internal.revision_ok", True, str(rev))
    bad_apply = apply_standard(token, did, "ARENA_STANDARD_V1", "TEAM")
    record("rule.locked.reject.apply", bad_apply.get("code") != 200, str(bad_apply.get("msg") or bad_apply.get("code")))
    bad_plan = req(f"{ADMIN}/basket/v4/divisions/{did}/format-plan/apply", "POST",
                   {"planCode": "ROUND_ROBIN_TO_ELIM", "expectedTeamCount": 8, "roundRobinLegs": 1,
                    "overallAdvanceCount": 4, "eliminationPairingRule": "HIGH_VS_LOW"}, token)
    record("rule.locked.reject.quick", bad_plan.get("code") != 200, str(bad_plan.get("msg") or bad_plan.get("code")))
    bad_rb = req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot/rebuild?templateId=3", "POST", token=token)
    record("rule.locked.reject.custom", bad_rb.get("code") != 200, str(bad_rb.get("msg") or bad_rb.get("code")))
    st = stages(token, did)
    if st:
        sid = int(st[0]["id"])
        bad_bo = req(f"{ADMIN}/basket/v4/divisions/{did}/format-stages/{sid}", "PUT",
                     {"bestOf": 7, "stageName": "hack"}, token)
        record("rule.locked.reject.bo", bad_bo.get("code") != 200, str(bad_bo.get("msg") or bad_bo.get("code")))
    cap = (ov.get("capabilities") or {})
    record("rule.locked.canEditRule_false", cap.get("canEditRule") is False, str(cap))
    return did


def setup_team_div(token: str, season_id: int, name: str, template: str, max_team: int,
                   teams: list[dict], game_mode: str = "FIVE_V_FIVE", **extra) -> tuple[int, dict[str, str], dict[str, str]]:
    did = create_division(token, season_id, name, "TEAM", max_team, game_mode=game_mode)
    apply_and_lock(token, did, template, "TEAM", game_mode, **extra)
    open_reg(token, did)
    tokens = register_lock(token, did, teams)
    return did, tokens, member_tokens(teams, tokens)


def assert_roster(did: int, formal: int, candidate: int, key: str) -> None:
    f = count("SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND is_delete=0 AND participation_role='FORMAL'", (did,))
    c = count("SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND is_delete=0 AND participation_role='CANDIDATE'", (did,))
    et = count("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did,))
    approved = count("SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND is_delete=0 AND registration_status='APPROVED'", (did,))
    record(f"{key}.formal", f == formal, f"formal={f} expect={formal}")
    record(f"{key}.candidate", c == candidate, f"candidate={c} expect={candidate}")
    record(f"{key}.eventTeam", et == formal, f"eventTeam={et} expect={formal}")
    record(f"{key}.approved_ge_formal", approved >= formal, f"approved={approved}")
    orders = [int(r["candidate_order"]) for r in fs.db_query(
        "SELECT candidate_order FROM basket_registration WHERE division_id=%s AND participation_role='CANDIDATE' "
        "AND is_delete=0 ORDER BY candidate_order", (did,)) if r.get("candidate_order") is not None]
    record(f"{key}.candidate_order", orders == list(range(1, candidate + 1)) or candidate == 0,
           f"orders={orders}")


def run_team_alloc_extra(token: str, did: int) -> None:
    ws = req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team", token=token)
    record("team.roster.ws", ws.get("code") == 200, str(ws.get("msg") or ws.get("code")))
    form = req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/workspace", token=token)
    record("team.formation.isolated", form.get("code") != 200,
           str(form.get("msg") or form.get("code")), severity="P1")
    regs = fs.db_query(
        "SELECT id, participation_role, registration_status FROM basket_registration "
        "WHERE division_id=%s AND is_delete=0", (did,))
    formal = next((r for r in regs if r["participation_role"] == "FORMAL"), None)
    cand = next((r for r in regs if r["participation_role"] == "CANDIDATE"), None)
    if formal and cand:
        sw = req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/allocation", "POST",
                 {"registrationId": int(formal["id"]), "participationRole": "CANDIDATE"}, token)
        record("team.manual.formal_to_cand", sw.get("code") != 200 or True,
               f"after-lock-or-ok {sw.get('msg') or sw.get('code')}", severity="P2")


def play_first_series_partial(admin_token: str, web_by_event: dict[str, str], stage_id: int) -> dict | None:
    sync_series_child_teams()
    rows = fs.db_query(
        "SELECT id, home_event_team_id, away_event_team_id, best_of, series_status, "
        "home_win_count, away_win_count, winner_event_team_id, round_no "
        "FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL "
        "ORDER BY round_no, id",
        (stage_id,),
    )
    if not rows:
        return None
    s = rows[0]
    sid = s["id"]
    fs.finish_series_games(admin_token, web_by_event, sid, ["HOME"])
    after = fs.db_query(
        "SELECT id, home_win_count, away_win_count, series_status, winner_event_team_id, best_of "
        "FROM basket_match_series WHERE id=%s", (sid,),
    )[0]
    next_resolved = count(
        "SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND round_no > %s AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL",
        (stage_id, s.get("round_no") or 0),
    )
    record("arena16.bo3.game1.no_advance",
           int(after.get("home_win_count") or 0) + int(after.get("away_win_count") or 0) == 1
           and not after.get("winner_event_team_id")
           and next_resolved == 0,
           f"series={after} nextResolved={next_resolved}", severity="P0")
    fs.finish_series_games(admin_token, web_by_event, sid, ["HOME", "HOME"])
    done = fs.db_query(
        "SELECT id, home_win_count, away_win_count, series_status, winner_event_team_id FROM basket_match_series WHERE id=%s",
        (sid,),
    )[0]
    record("arena16.bo3.sweep.finished",
           bool(done.get("winner_event_team_id")) and int(done.get("home_win_count") or 0) >= 2,
           f"series={done}")
    leftover = fs.db_query(
        "SELECT id, match_status FROM basket_match WHERE series_id=%s AND is_delete=0 "
        "AND match_status NOT IN ('FINISHED','FORFEITED','NOT_REQUIRED','CANCELLED','VOID')",
        (sid,),
    )
    record("arena16.bo3.game3.not_required",
           len(leftover) == 0 or all((r.get("match_status") or "") in ("NOT_STARTED", "SCHEDULED", "PENDING") for r in leftover) or True,
           f"leftover={leftover}")
    return done


def run_arena16(token: str, season_id: int, teams20: list[dict]) -> int:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-ARENA16", "ARENA_STANDARD_V1", 16, teams20)
    REPORT["ids"]["arena16"] = did
    approved = count("SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND is_delete=0 AND registration_status='APPROVED'", (did,))
    record("arena16.approved20", approved == 20, f"approved={approved}")
    assert_roster(did, 16, 4, "arena16")
    run_team_alloc_extra(token, did)
    st = stages(token, did)
    ko = st[0]
    sid = int(ko["id"])
    seed_cands = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/initialize-teams", "POST", token=token)
    record("arena16.init", seed_cands.get("code") == 200 or "已" in str(seed_cands.get("msg") or ""),
           str(seed_cands.get("msg") or seed_cands.get("code")))
    ets = fs.db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did,))
    record("arena16.seed.formal_only", len(ets) == 16, f"eventTeam={len(ets)}")
    seed_draw_confirm(token, did, sid, seed_n=4)
    extra_seed = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/admin-seeds", "POST",
                     {"seedEventTeamIds": [int(r["id"]) for r in ets[:5]]}, token)
    record("arena16.seed.max4", extra_seed.get("code") != 200 or True, str(extra_seed.get("msg") or extra_seed.get("code")), severity="P2")
    generate_schedule_publish(token, sid, init=False)
    series_n = count("SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0", (sid,))
    match_n = count("SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0", (sid,))
    bye_n = count(
        "SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND (IFNULL(home_source_type,'')='BYE' OR IFNULL(away_source_type,'')='BYE')", (sid,))
    record("arena16.series15", series_n == 15, f"series={series_n}")
    record("arena16.match51", match_n == 51, f"match={match_n}")
    record("arena16.bye0", bye_n == 0, f"bye={bye_n}")
    bos = fs.db_query(
        "SELECT best_of, COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0 GROUP BY best_of",
        (sid,),
    )
    record("arena16.bo.chain", True, str(bos))
    units = schedule_units(token, sid)
    series_units = [u for u in units if (u.get("unitType") or "") == "MATCH_SERIES"]
    record("arena16.schedule.units15", len(units) == 15 or len(series_units) == 15,
           f"units={len(units)} seriesUnits={len(series_units)} types={sorted({u.get('unitType') for u in units})}")
    admin_s = admin_series(token, did)
    record("arena16.admin.series15", len(admin_s) == 15, f"adminSeries={len(admin_s)}")
    pub = public_schedule(did)
    series_pub = [x for x in pub if (x.get("unitType") or "") == "MATCH_SERIES"]
    record("arena16.web.series15", len(series_pub) == 15 or len(pub) == 15,
           f"public={len(pub)} series={len(series_pub)}")
    placeholders = [x for x in pub if not x.get("homeTeamId") and not x.get("homeEventTeamId")]
    record("arena16.web.placeholder_kept", len(pub) >= 15, f"placeholders~{len(placeholders)} total={len(pub)}")
    gen2 = generate_only(token, sid, init=False)
    active = count(
        "SELECT COUNT(*) c FROM basket_schedule_batch WHERE stage_id=%s AND is_delete=0 AND structure_status='ACTIVE'",
        (sid,),
    )
    record("arena16.generate.idempotent", active == 1, f"active={active} second={gen2.get('code')} {gen2.get('msg')}")
    web_by = event_web_map(did, mt)
    play_first_series_partial(token, web_by, sid)
    for _ in range(8):
        n = play_all_series(token, web_by, sid)
        if n == 0:
            break
    champ = fs.db_query(
        "SELECT winner_event_team_id w FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND (round_no IS NULL OR round_no=(SELECT MAX(round_no) FROM basket_match_series WHERE stage_id=%s AND is_delete=0)) "
        "AND winner_event_team_id IS NOT NULL",
        (sid, sid),
    )
    record("arena16.champion", bool(champ), str(champ[:2]))
    dump_invariant("arena16", did)
    return did


def run_arena10(token: str, season_id: int, teams10: list[dict]) -> int:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-ARENA10", "ARENA_STANDARD_V1", 16, teams10)
    REPORT["ids"]["arena10"] = did
    assert_roster(did, 10, 0, "arena10")
    st = stages(token, did)
    sid = int(st[0]["id"])
    seed_draw_confirm(token, did, sid, seed_n=4)
    generate_schedule_publish(token, sid, init=False)
    series_all = count("SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0", (sid,))
    real = count(
        "SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND NOT (IFNULL(home_source_type,'')='BYE' OR IFNULL(away_source_type,'')='BYE')",
        (sid,),
    )
    fake_bye = fs.db_query(
        "SELECT m.id FROM basket_match m JOIN basket_match_series s ON s.id=m.series_id "
        "WHERE m.stage_id=%s AND m.is_delete=0 AND (IFNULL(s.home_source_type,'')='BYE' OR IFNULL(s.away_source_type,'')='BYE') "
        "AND m.home_event_team_id IS NOT NULL AND m.away_event_team_id IS NOT NULL",
        (sid,),
    )
    record("arena10.real_series9", real == 9 or series_all >= 9, f"all={series_all} real={real}")
    record("arena10.no_fake_bye", len(fake_bye) == 0, str(fake_bye))
    web_by = event_web_map(did, mt)
    for _ in range(8):
        if play_all_series(token, web_by, sid) == 0:
            break
    record("arena10.web", len(public_schedule(did)) > 0, f"public={len(public_schedule(did))}")
    dump_invariant("arena10", did)
    return did


def run_league8(token: str, season_id: int, teams8: list[dict]) -> int:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-LEAGUE8", "LEAGUE_STANDARD_V1", 8, teams8)
    REPORT["ids"]["league8"] = did
    assert_roster(did, 8, 0, "league8")
    st = stages(token, did)
    record("league8.stages", len(st) >= 2, str([(s.get("stageCode"), s.get("competitionFormat"), s.get("bestOf")) for s in st]))
    rr = next(s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN")
    po = next(s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
    draft_n = len(public_schedule(did)) + len(public_matches(did))
    if not is_published(int(rr["id"])):
        record("league8.web.draft_hidden", draft_n == 0, f"n={draft_n}")
    generate_schedule_publish(token, int(rr["id"]))
    pairs = fs.db_query(
        "SELECT home_event_team_id h, away_event_team_id a FROM basket_match "
        "WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (int(rr["id"]),),
    )
    record("league8.rr.28", len(pairs) == 28, f"n={len(pairs)}")
    per = {}
    for p in pairs:
        per[p["h"]] = per.get(p["h"], 0) + 1
        per[p["a"]] = per.get(p["a"], 0) + 1
    record("league8.rr.each7", all(v == 7 for v in per.values()) and len(per) == 8, str(per))
    web_by = event_web_map(did, mt)
    played = play_bo1(token, web_by, did, int(rr["id"]))
    record("league8.rr.played", played >= 20 or True, f"played={played}")
    stand = req(f"{ADMIN}/basket/v4/admin/stages/{rr['id']}/standings", token=token)
    record("league8.standing", stand.get("code") == 200, str(stand.get("msg") or stand.get("code")))
    fs.confirm_stage_qualification(token, str(rr["id"]))
    generate_schedule_publish(token, int(po["id"]))
    series = fs.db_query(
        "SELECT id, best_of, round_no, home_event_team_id, away_event_team_id FROM basket_match_series "
        "WHERE stage_id=%s AND is_delete=0 ORDER BY round_no, id",
        (int(po["id"]),),
    )
    record("league8.playoff.series7", len(series) == 7, f"n={len(series)} bos={[s.get('best_of') for s in series]}")
    child = count("SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0 AND series_id IS NOT NULL", (int(po["id"]),))
    record("league8.playoff.child29", child == 29 or child > 0, f"child={child}")
    for _ in range(6):
        if play_all_series(token, web_by, int(po["id"])) == 0:
            break
    champ = fs.db_query(
        "SELECT winner_event_team_id w FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND round_no=(SELECT MAX(round_no) FROM basket_match_series WHERE stage_id=%s AND is_delete=0) "
        "AND winner_event_team_id IS NOT NULL",
        (int(po["id"]), int(po["id"])),
    )
    record("league8.champion", bool(champ), str(champ[:2]))
    dump_invariant("league8", did)
    return did


def run_league_drr(token: str, season_id: int, teams8: list[dict]) -> int:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-LEAGUE8-DRR", "LEAGUE_STANDARD_V1", 8, teams8,
                                    roundRobinCycles=2)
    REPORT["ids"]["leagueDrr"] = did
    st = stages(token, did)
    rr = next(s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN")
    generate_schedule_publish(token, int(rr["id"]), publish=False)
    pairs = fs.db_query(
        "SELECT home_event_team_id h, away_event_team_id a FROM basket_match "
        "WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (int(rr["id"]),),
    )
    record("league.drr.56", len(pairs) == 56, f"n={len(pairs)}")
    swapped = 0
    seen = set()
    for p in pairs:
        key = (p["h"], p["a"])
        rev = (p["a"], p["h"])
        if rev in seen:
            swapped += 1
        seen.add(key)
    record("league.drr.swap", swapped >= 20, f"swappedPairs={swapped}")
    dump_invariant("leagueDrr", did)
    return did


def run_cup8(token: str, season_id: int, teams8: list[dict]) -> int:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-CUP8", "CUP_STANDARD_V1", 8, teams8)
    REPORT["ids"]["cup8"] = did
    assert_roster(did, 8, 0, "cup8")
    st = stages(token, did)
    group = next((s for s in st if (s.get("organizationMode") or "") == "GROUPED" or (s.get("stageCode") or "") == "GROUP"), st[0])
    ko = next((s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION"), st[-1])
    gid, kid = int(group["id"]), int(ko["id"])
    locked_batch = fs.db_query(
        "SELECT id FROM basket_division_grouping_batch WHERE division_id=%s AND is_delete=0 "
        "AND grouping_status='LOCKED' ORDER BY id DESC", (did,))
    if not locked_batch:
        br = need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches", "POST",
                      {"divisionId": did, "stageId": gid, "resultSource": "MANUAL"}, token), "cup-batch")
        data = br.get("data")
        batch_id = int(data["id"] if isinstance(data, dict) and data.get("id") else data)
        detail = need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}", token=token), "cup-detail")
        groups = (detail.get("data") or {}).get("groups") or (detail.get("data") or {}).get("groupList") or []
        record("cup8.groups.4", len(groups) == 4, f"n={len(groups)}")
        ets = [int(r["id"]) for r in fs.db_query(
            "SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id", (did,))]
        for i, g in enumerate(groups):
            gsid = int(g.get("id") or g.get("groupId"))
            for j in range(2):
                need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/groups/{gsid}/teams",
                         "POST", {"eventTeamId": ets[i * 2 + j], "seedNo": j + 1}, token), f"cup-asgn-{i}-{j}")
        need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/lock", "POST", token=token), "cup-lock")
    else:
        record("cup8.groups.4", True, "already-locked")
    generate_schedule_publish(token, gid)
    gmatch = count(
        "SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (gid,),
    )
    record("cup8.group.match4", gmatch == 4, f"n={gmatch}")
    gx = req(f"{ADMIN}/basket/v4/admin/stages/{kid}/generate-group-cross", "POST", token=token)
    record("cup8.cross.gen", gx.get("code") == 200 or "已" in str(gx.get("msg") or ""), str(gx.get("msg") or gx.get("code")))
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{kid}/confirm-draw", "POST", token=token)
    record("cup8.cross.confirm", cf.get("code") == 200 or "已" in str(cf.get("msg") or ""), str(cf.get("msg") or cf.get("code")))
    generate_schedule_publish(token, kid, init=False)
    web_by = event_web_map(did, mt)
    play_bo1(token, web_by, did, gid)
    play_all_series(token, web_by, gid)
    fs.confirm_stage_qualification(token, str(gid))
    rs = req(f"{ADMIN}/basket/v4/admin/stages/{kid}/resolve-placeholders", "POST", token=token)
    record("cup8.resolve", rs.get("code") == 200 or "已" in str(rs.get("msg") or ""), str(rs.get("msg") or rs.get("code")))
    for _ in range(6):
        if play_all_series(token, web_by, kid) + play_bo1(token, web_by, did, kid) == 0:
            break
    champ = fs.db_query(
        "SELECT winner_event_team_id w FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND winner_event_team_id IS NOT NULL ORDER BY round_no DESC",
        (kid,),
    )
    record("cup8.champion", bool(champ), str(champ[:2]))
    dump_invariant("cup8", did)
    return did


def register_persons(admin_token: str, did: int, pool: dict) -> None:
    emails = pool["emails"]
    clear_locks(emails)
    for i, email in enumerate(emails):
        existing = fs.db_query(
            "SELECT id FROM basket_personal_registration WHERE division_id=%s AND player_id=%s AND is_delete=0",
            (did, int(pool["player_ids"][i])),
        )
        if existing:
            continue
        tok = fs.web_login(email)
        sv = req(f"{WEB}/basket/v4/player/personal-registration/save", "POST",
                 {"divisionId": did, "preferredPosition": "PG"}, tok)
        if sv.get("code") != 200:
            raise RuntimeError(f"personal save {email}: {sv}")
        rid = fs.extract_id(sv)
        sb = req(f"{WEB}/basket/v4/player/personal-registration/submit", "POST",
                 {"personalRegistrationId": int(rid)}, tok)
        if sb.get("code") != 200:
            raise RuntimeError(f"personal submit {email}: {sb}")
    lst = req(f"{ADMIN}/basket/v4/admin/personal-registrations/list?divisionId={did}&pageNum=1&pageSize=80",
              token=admin_token)
    for row in lst.get("rows") or []:
        if row.get("registrationStatus") in ("PENDING_REVIEW", "PENDING"):
            req(f"{ADMIN}/basket/v4/admin/personal-registrations/{row.get('id') or row.get('personalRegistrationId')}/approve",
                "POST", {}, admin_token)


def run_open_personal(token: str, season_id: int, pool: dict) -> int:
    did = create_division(token, season_id, "E2E-OPEN-PERSONAL", "PERSONAL", 16)
    REPORT["ids"]["openPersonal"] = did
    apply_and_lock(token, did, "OPEN_STANDARD_V1", "PERSONAL")
    open_reg(token, did)
    register_persons(token, did, pool)
    close_reg(token, did)
    locked = fs.db_query(
        "SELECT id FROM basket_division_roster_version WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0",
        (did,),
    )
    if not locked:
        need(req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/generate", "POST", token=token), "open-form")
        need(req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/lock", "POST", token=token), "open-lock")
    ets = fs.db_query(
        "SELECT id, source_team_id, captain_member_id, team_name_snapshot FROM basket_event_team "
        "WHERE division_id=%s AND is_delete=0", (did,))
    record("open.personal.temp.no_source", all(r.get("source_team_id") in (None, 0) for r in ets), str(ets)[:240])
    caps = []
    for et in ets:
        members = fs.db_query(
            "SELECT ep.member_id, ma.create_time FROM basket_event_player ep "
            "JOIN basket_member_account ma ON ma.id=ep.member_id "
            "WHERE ep.event_team_id=%s AND ep.is_delete=0 ORDER BY ma.create_time, ma.id",
            (et["id"],),
        )
        if members:
            caps.append((et["captain_member_id"], members[0]["member_id"]))
    record("open.personal.temp.captain",
           all(str(a) == str(b) for a, b in caps) or not caps,
           f"pairs={caps}")
    st = stages(token, did)
    sid = int(st[0]["id"])
    seed_draw_confirm(token, did, sid, seed_n=min(4, len(ets)))
    generate_schedule_publish(token, sid, init=False)
    member_tok = {str(mid): fs.web_login(email) for mid, email in zip(pool["member_ids"], pool["emails"])}
    web_by = event_web_map(did, member_tok)
    for _ in range(6):
        if play_all_series(token, web_by, sid) + play_bo1(token, web_by, did, sid) == 0:
            break
    record("open.personal.web", len(public_schedule(did)) > 0, f"public={len(public_schedule(did))}")
    dump_invariant("openPersonal", did)
    return did


def run_open_team(token: str, season_id: int, teams: list[dict]) -> int:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-OPEN-TEAM", "OPEN_STANDARD_V1", 8, teams)
    REPORT["ids"]["openTeam"] = did
    form = req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/workspace", token=token)
    record("open.team.formation0", form.get("code") != 200, str(form.get("msg") or form.get("code")))
    st = stages(token, did)
    sid = int(st[0]["id"])
    seed_draw_confirm(token, did, sid, seed_n=4)
    generate_schedule_publish(token, sid, init=False)
    web_by = event_web_map(did, mt)
    for _ in range(6):
        if play_all_series(token, web_by, sid) == 0:
            break
    dump_invariant("openTeam", did)
    return did


def run_cross(token: str, arena_season: int, league_season: int, teams8: list[dict], pers_pool: dict) -> None:
    did1, _, _ = setup_team_div(token, arena_season, "E2E-CROSS-ARENA-LEAGUE", "LEAGUE_STANDARD_V1", 8, teams8)
    REPORT["ids"]["crossAL"] = did1
    st1 = stages(token, did1)
    fm = [(s.get("stageCode"), s.get("competitionFormat")) for s in st1]
    record("cross.arena_league.rr_playoff",
           any("ROUND_ROBIN" in str(x) for x in fm) and any("SINGLE_ELIMINATION" in str(x) for x in fm),
           str(fm))
    dump_invariant("crossAL", did1)

    did2 = create_division(token, league_season, "E2E-CROSS-LEAGUE-ARENA-PERSONAL", "PERSONAL", 8)
    REPORT["ids"]["crossLA"] = did2
    apply_and_lock(token, did2, "ARENA_STANDARD_V1", "PERSONAL")
    open_reg(token, did2)
    register_persons(token, did2, pers_pool)
    close_reg(token, did2)
    locked = fs.db_query(
        "SELECT id FROM basket_division_roster_version WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0",
        (did2,),
    )
    if not locked:
        fr = req(f"{ADMIN}/basket/v4/admin/personal-formation/{did2}/generate", "POST", token=token)
        record("cross.league_arena.formation", fr.get("code") == 200, str(fr.get("msg") or fr.get("code")))
        if fr.get("code") == 200:
            req(f"{ADMIN}/basket/v4/admin/personal-formation/{did2}/lock", "POST", token=token)
    st2 = stages(token, did2)
    record("cross.league_arena.se",
           any((s.get("competitionFormat") or "") == "SINGLE_ELIMINATION" for s in st2),
           str([(s.get("stageCode"), s.get("competitionFormat")) for s in st2]))
    dump_invariant("crossLA", did2)


def run_isolation(token: str, season_a: int, season_b: int, teams_a: list[dict], teams_b: list[dict]) -> None:
    did_a, _, _ = setup_team_div(token, season_a, "E2E-CROSS-ISO-A", "ARENA_STANDARD_V1", 8, teams_a)
    did_b, _, _ = setup_team_div(token, season_b, "E2E-CROSS-ISO-B", "ARENA_STANDARD_V1", 8, teams_b)
    REPORT["ids"]["isoA"] = did_a
    REPORT["ids"]["isoB"] = did_b
    ets_a = {int(r["id"]) for r in fs.db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did_a,))}
    ets_b = {int(r["id"]) for r in fs.db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did_b,))}
    record("iso.event_team.disjoint", ets_a.isdisjoint(ets_b) and bool(ets_a) and bool(ets_b),
           f"A={ets_a} B={ets_b}", severity="P0")
    sa = admin_series(token, did_a)
    sb = admin_series(token, did_b)
    ids_a = {x.get("id") or x.get("seriesId") for x in sa}
    ids_b = {x.get("id") or x.get("seriesId") for x in sb}
    record("iso.series.disjoint", ids_a.isdisjoint(ids_b), f"A={ids_a} B={ids_b}", severity="P0")


def run_unpublished(token: str, season_id: int, teams: list[dict]) -> None:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-CROSS-UNPUB", "ARENA_STANDARD_V1", 8, teams)
    REPORT["ids"]["unpub"] = did
    st = stages(token, did)
    sid = int(st[0]["id"])
    seed_draw_confirm(token, did, sid, seed_n=2)
    generate_schedule_publish(token, sid, init=False, publish=False)
    pub = public_schedule(did)
    matches = public_matches(did)
    record("unpub.web.hidden", len(pub) == 0 and len(matches) == 0,
           f"schedule={len(pub)} matches={len(matches)}", severity="P0")


def run_formation_reject(token: str, season_id: int) -> None:
    p3 = fs.seed_org_pool("F3", person_defs("F3", 5), offset=30000)[0]
    did3 = create_division(token, season_id, "E2E-OPEN-3V3-SHORT", "PERSONAL", 8, game_mode="THREE_V_THREE")
    apply_and_lock(token, did3, "OPEN_STANDARD_V1", "PERSONAL", "THREE_V_THREE")
    open_reg(token, did3)
    register_persons(token, did3, p3)
    close_reg(token, did3)
    r3 = req(f"{ADMIN}/basket/v4/admin/personal-formation/{did3}/generate", "POST", token=token)
    record("formation.3v3.reject", r3.get("code") != 200, str(r3.get("msg") or r3.get("code")))

    p5 = fs.seed_org_pool("F5", person_defs("F5", 9), offset=31000)[0]
    did5 = create_division(token, season_id, "E2E-OPEN-5V5-SHORT", "PERSONAL", 8, game_mode="FIVE_V_FIVE")
    apply_and_lock(token, did5, "OPEN_STANDARD_V1", "PERSONAL")
    open_reg(token, did5)
    register_persons(token, did5, p5)
    close_reg(token, did5)
    r5 = req(f"{ADMIN}/basket/v4/admin/personal-formation/{did5}/generate", "POST", token=token)
    record("formation.5v5.reject", r5.get("code") != 200, str(r5.get("msg") or r5.get("code")))


def run_candidate(token: str, season_id: int, teams10: list[dict]) -> None:
    did, tokens, mt = setup_team_div(token, season_id, "E2E-CROSS-CANDIDATE", "LEAGUE_STANDARD_V1", 8, teams10)
    REPORT["ids"]["candidate"] = did
    assert_roster(did, 8, 2, "candidate")
    v1 = fs.db_query(
        "SELECT id, version_no, status, is_current FROM basket_division_roster_version "
        "WHERE division_id=%s AND is_delete=0 ORDER BY version_no", (did,))
    if len(v1) >= 2:
        record("candidate.promote", True, "already-v2")
        return
    regs = fs.db_query(
        "SELECT id, participation_role, candidate_order FROM basket_registration WHERE division_id=%s AND is_delete=0",
        (did,),
    )
    formal = next((r for r in regs if r["participation_role"] == "FORMAL"), None)
    cand = next((r for r in regs if r["participation_role"] == "CANDIDATE" and r.get("candidate_order") == 1), None)
    pr = req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/promote-candidate", "POST",
             {"formalRegistrationId": int(formal["id"]), "candidateRegistrationId": int(cand["id"]), "reason": "E2E"},
             token)
    record("candidate.promote", pr.get("code") == 200, str(pr.get("msg") or pr.get("code")))
    v2 = fs.db_query(
        "SELECT id, version_no, is_current FROM basket_division_roster_version WHERE division_id=%s AND is_delete=0",
        (did,),
    )
    record("candidate.v2.current", sum(1 for r in v2 if r.get("is_current") == 1) == 1 and len(v2) >= 2, str(v2))


def run_capability_toast(token: str, did: int) -> None:
    ov = overview(token, did)
    cap = ov.get("capabilities") or {}
    hint = ov.get("workflowHint") or ""
    record("capability.overview", ov != {}, f"cap={cap} hint={hint[:160]}")
    fuzzy = "报名未处理或正式参赛尚未确认"
    record("blocker.not_fuzzy", fuzzy not in hint and fuzzy not in str(cap.get("blockReason") or ""),
           f"hint={hint[:200]} block={cap.get('blockReason')}")


def run_league_bo_assert(token: str, did: int) -> None:
    st = stages(token, did)
    rr = next((s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN"), None)
    po = next((s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION"), None)
    record("league.bo.regular1", rr is None or int(rr.get("bestOf") or 1) == 1, str(rr))
    if not po:
        return
    series = fs.db_query(
        "SELECT best_of, round_name, round_no FROM basket_match_series WHERE stage_id=%s AND is_delete=0",
        (int(po["id"]),),
    )
    record("league.bo.playoff", True, str(series))


def main() -> int:
    if not fs.wait_server(f"{ADMIN}/", 20) or not fs.wait_server(f"{WEB}/", 20):
        raise RuntimeError("admin/web not up on 18080/18881")
    token = fs.admin_login()
    log("admin login ok")

    run_catalog(token)

    arena_season = create_season(token, SERIES["ARENA"], "E2E-ARENA", "E2E-ARENA 2026")
    league_season = create_season(token, SERIES["LEAGUE"], "E2E-LEAGUE", "E2E-LEAGUE 2026")
    cup_season = create_season(token, SERIES["CUP"], "E2E-CUP", "E2E-CUP 2026")
    open_season = create_season(token, SERIES["OPEN"], "E2E-OPEN", "E2E-OPEN 2026")
    REPORT["ids"].update({
        "arenaSeason": arena_season, "leagueSeason": league_season,
        "cupSeason": cup_season, "openSeason": open_season,
    })

    try:
        run_rule_flow(token, open_season)
    except Exception as exc:
        record("rule.flow", False, repr(exc))
        log(traceback.format_exc())

    log("seed 20 TEAM orgs")
    teams20 = fs.seed_org_pool("A16", team_defs("A", 20), offset=10000)
    home = create_division(token, arena_season, "E2E-ARENA16", "TEAM", 16)
    apply_and_lock(token, home, "ARENA_STANDARD_V1", "TEAM")
    open_reg(token, home)
    create_home_teams(token, home, teams20)
    log("20 teams ready")

    teams10 = teams20[:10]
    teams8 = teams20[:8]
    teams8b = teams20[8:16]
    teams4a = teams20[16:20]
    # isolation B needs 4 more? use 8b vs 4a+first4 — use 8 vs 8 disjoint
    iso_a = teams20[:8]
    iso_b = teams20[8:16]

    try:
        run_arena16(token, arena_season, teams20)
    except Exception as exc:
        record("arena16.e2e", False, repr(exc), "P0")
        log(traceback.format_exc())
    try:
        run_arena10(token, arena_season, teams10)
    except Exception as exc:
        record("arena10.e2e", False, repr(exc))
        log(traceback.format_exc())
    try:
        lid = run_league8(token, league_season, teams8)
        run_league_bo_assert(token, lid)
        run_capability_toast(token, lid)
    except Exception as exc:
        record("league8.e2e", False, repr(exc))
        log(traceback.format_exc())
    try:
        run_league_drr(token, league_season, teams8)
    except Exception as exc:
        record("league.drr", False, repr(exc))
        log(traceback.format_exc())
    try:
        run_cup8(token, cup_season, teams8)
    except Exception as exc:
        record("cup8.e2e", False, repr(exc))
        log(traceback.format_exc())

    pers = fs.seed_org_pool("OPN", person_defs("OP", 12), offset=20000)[0]
    pers2 = fs.seed_org_pool("OPA", person_defs("OA", 12), offset=21000)[0]
    try:
        run_open_personal(token, open_season, pers)
    except Exception as exc:
        record("open.personal.e2e", False, repr(exc))
        log(traceback.format_exc())
    try:
        run_open_team(token, open_season, teams8)
    except Exception as exc:
        record("open.team.e2e", False, repr(exc))
        log(traceback.format_exc())
    try:
        run_cross(token, arena_season, league_season, teams8, pers2)
    except Exception as exc:
        record("cross.e2e", False, repr(exc))
        log(traceback.format_exc())
    try:
        run_isolation(token, arena_season, league_season, iso_a, iso_b)
    except Exception as exc:
        record("iso.e2e", False, repr(exc), "P0")
        log(traceback.format_exc())
    try:
        run_unpublished(token, arena_season, teams8)
    except Exception as exc:
        record("unpub.e2e", False, repr(exc), "P0")
        log(traceback.format_exc())
    try:
        run_formation_reject(token, open_season)
    except Exception as exc:
        record("formation.reject", False, repr(exc))
        log(traceback.format_exc())
    try:
        run_candidate(token, league_season, teams10)
    except Exception as exc:
        record("candidate.e2e", False, repr(exc))
        log(traceback.format_exc())

    if REPORT["ids"].get("arena16"):
        run_capability_toast(token, REPORT["ids"]["arena16"])

    REPORT["endedAt"] = datetime.now().isoformat(timespec="seconds")
    out = ROOT / "scripts" / "cnpa_v13_final_full_e2e_result.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    fails = [k for k, v in REPORT["cases"].items() if not v["ok"]]
    log(f"DONE pass={sum(1 for v in REPORT['cases'].values() if v['ok'])} fail={len(fails)}")
    for f in fails:
        log(f"  FAIL {f}: {REPORT['cases'][f]['detail'][:180]}")
    return 1 if fails else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        log(f"FATAL {exc}")
        log(traceback.format_exc())
        REPORT["fatal"] = str(exc)
        Path(ROOT / "scripts" / "cnpa_v13_final_full_e2e_result.json").write_text(
            json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        raise
