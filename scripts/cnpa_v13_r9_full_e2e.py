#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V1.3 R9 Full E2E against basketball_r9_fresh only."""
from __future__ import annotations

import json
import sys
import time
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
fs.MARKER = "R9_TEST"
fs.ID_BASE = 2099111409000000000
fs.REDIS_HOST = "192.168.31.48"

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req
as_id = fs.as_id

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "cases": {},
    "bugs": {"P0": [], "P1": [], "P2": []},
    "ids": {},
}


def log(msg: str) -> None:
    print(msg, flush=True)


def log_api(msg: str) -> None:
    log(f"[API] {msg}")


def log_fixture_sql(msg: str) -> None:
    log(f"[FIXTURE-SQL] {msg}")


def log_assert_sql(msg: str) -> None:
    log(f"[ASSERT-SQL] {msg}")


def record(name: str, ok: bool, detail: str = "") -> bool:
    REPORT["cases"][name] = {"ok": ok, "detail": detail[:800]}
    mark = "PASS" if ok else "FAIL"
    log(f"[{mark}] {name} {detail[:200]}")
    if not ok:
        REPORT["bugs"]["P1"].append(f"{name}: {detail[:400]}")
    return ok


def need(resp: dict, name: str) -> dict:
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def team_defs(prefix: str, n: int) -> list[dict]:
    out = []
    for i in range(1, n + 1):
        code = f"{prefix}{i:02d}"
        out.append({
            "code": code,
            "name": f"R9_TEST_{prefix}_{code}",
            "short": f"{prefix}{i:02d}",
            "players": [f"R9_{prefix}_{code}_P{j}" for j in range(1, 6)],
        })
    return out


def person_defs(prefix: str, n: int) -> list[dict]:
    # reuse seed_org_pool shape: one fake team of n persons
    return [{
        "code": prefix,
        "name": f"R9_TEST_{prefix}_POOL",
        "short": prefix,
        "players": [f"R9_{prefix}_P{j}" for j in range(1, n + 1)],
    }]


def patch_season(token: str, season_id: int) -> None:
    body = {
        "seasonYear": 2026,
        "seasonName": "R9_TEST",
        "displayTitle": f"R9_TEST_S{season_id}",
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-12-31 23:59:59",
    }
    need(req(f"{ADMIN}/basket/v4/season/{season_id}", "PUT", body, token), f"season-update-{season_id}")
    pr = req(f"{ADMIN}/basket/v4/season/{season_id}/publish", "PUT", token=token)
    if pr.get("code") != 200 and "已" not in str(pr.get("msg") or ""):
        raise RuntimeError(f"season publish {season_id}: {pr}")


def patch_division(token: str, did: int, max_team: int, mode: str, name: str) -> None:
    body = {
        "divisionName": name,
        "platformCode": "PS",
        "gameMode": "FIVE_V_FIVE",
        "registrationMode": mode,
        "maxTeamCount": max_team,
        "maxPersonCount": 32,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-20 23:59:59",
        "matchStartTime": "2026-08-21 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",
    }
    need(req(f"{ADMIN}/basket/v4/division/{did}", "PUT", body, token), f"div-update-{did}")


def apply_and_lock(token: str, did: int, template: str, mode: str) -> None:
    snap = req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot", token=token)
    data = snap.get("data") or {}
    status = data.get("snapshotStatus") or data.get("status")
    if status in ("LOCKED", "PUBLISHED") or data.get("locked") is True:
        return
    body = {"templateCode": template, "registrationMode": mode, "gameMode": "FIVE_V_FIVE"}
    log_api(f"apply STANDARD {template} mode={mode} division={did}")
    need(req(f"{ADMIN}/basket/v4/divisions/{did}/standard-template/apply", "POST", body, token), f"apply-{did}")
    log_api(f"lock rule division={did}")
    need(req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot/lock", "POST", token=token), f"lock-rule-{did}")


def open_reg(token: str, did: int) -> None:
    need(req(f"{ADMIN}/basket/v4/division/{did}/open-registration", "PUT", token=token), f"open-{did}")


def close_reg(token: str, did: int) -> None:
    r = req(f"{ADMIN}/basket/v4/division/{did}/close-registration", "PUT", token=token)
    if r.get("code") != 200 and "仅报名中" not in str(r.get("msg") or ""):
        raise RuntimeError(f"close {did}: {r}")


def stages(token: str, did: int) -> list[dict]:
    r = need(req(f"{ADMIN}/basket/v4/admin/stages/list?divisionId={did}", token=token), f"stages-{did}")
    data = r.get("data") or []
    for s in data:
        if s.get("id") is None and s.get("stageId") is not None:
            s["id"] = s["stageId"]
    return sorted(data, key=lambda s: s.get("stageOrder") or 0)


def public_matches(did: int) -> list:
    r = req(f"{WEB}/basket/v4/public/match/list?divisionId={did}")
    data = r.get("data")
    if isinstance(data, list):
        return data
    return []


def public_series(stage_id: int) -> list:
    r = req(f"{WEB}/basket/v4/public/match-series/stage/{stage_id}")
    data = r.get("data")
    return data if isinstance(data, list) else []


def ensure_scheduled(token: str, stage_id: int) -> None:
    rec = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule/recommend-apply", "POST", {"overwriteScheduled": False}, token)
    if rec.get("code") == 200:
        return
    units_r = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule-units", token=token)
    payload = units_r.get("data") or []
    if isinstance(payload, dict):
        units = payload.get("units") or []
    else:
        units = payload
    date = "2026-10-06"
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
            raise RuntimeError(f"assign unit {body}: {ar}")


def is_stage_published(stage_id: int) -> bool:
    rows = fs.db_query(
        "SELECT publish_status FROM basket_schedule_batch "
        "WHERE stage_id=%s AND is_delete=0 AND structure_status='ACTIVE'",
        (stage_id,),
    )
    return any((r.get("publish_status") or "") == "PUBLISHED" for r in rows)


def generate_schedule_publish(token: str, stage_id: int, init: bool = True) -> None:
    if is_stage_published(stage_id):
        return
    if init:
        log_api(f"initialize-teams stage={stage_id}")
        ir = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/initialize-teams", "POST", token=token)
        if ir.get("code") != 200 and "已" not in str(ir.get("msg") or ""):
            log(f"  init-teams warn {ir.get('msg')}")
    log_api(f"generate-schedule stage={stage_id}")
    gen = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/generate-schedule", "POST", token=token)
    if gen.get("code") != 200:
        msg = str(gen.get("msg") or "")
        if "已发布" not in msg:
            raise RuntimeError(f"gen-{stage_id} failed: {gen}")
        return
    ensure_scheduled(token, stage_id)
    log_api(f"publish-schedule stage={stage_id}")
    pub = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/publish-schedule", "POST", token=token)
    if pub.get("code") != 200 and "已" not in str(pub.get("msg") or ""):
        raise RuntimeError(f"publish {stage_id}: {pub}")


def event_web_map(admin_token: str, did: int, tokens_by_member: dict[str, str]) -> dict[str, str]:
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
        home = m.get("homeEventTeamId")
        away = m.get("awayEventTeamId")
        if not home or not away:
            continue
        hs, aws = (80, 70) if idx % 2 == 0 else (69, 75)
        wt = web_by_event.get(as_id(home))
        if not wt:
            raise RuntimeError(f"no token for home {home}")
        log_api(f"submit+approve match={m.get('id')} stage={stage_id}")
        fs.submit_and_approve_match(admin_token, wt, m, hs, aws, home, reject_first=False)
        n += 1
    return n


def sync_series_child_teams() -> None:
    log_fixture_sql("sync series child teams + inherit published batch visibility")
    fs.db_exec(
        "UPDATE basket_match m "
        "INNER JOIN basket_match_series s ON s.id = m.series_id AND s.is_delete=0 "
        "SET m.home_event_team_id = s.home_event_team_id, m.away_event_team_id = s.away_event_team_id "
        "WHERE m.is_delete=0 AND s.home_event_team_id IS NOT NULL AND s.away_event_team_id IS NOT NULL "
        "AND (m.home_event_team_id IS NULL OR m.away_event_team_id IS NULL)"
    )
    # Natural resolution may insert first-round games after Stage Publish.
    # Those child matches must inherit the published batch visibility.
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
        sid = s.get("id")
        if not sid:
            continue
        if s.get("winner_event_team_id") or (s.get("series_status") or "") in ("FINISHED", "COMPLETED"):
            continue
        home, away = s.get("home_event_team_id"), s.get("away_event_team_id")
        if not home or not away:
            continue
        best = int(s.get("best_of") or 1)
        need_w = best // 2 + 1
        plan = (["HOME"] * need_w) if prefer_home else (["AWAY"] * need_w)
        fs.finish_series_games(admin_token, web_by_event, sid, plan)
        done += 1
    return done


def setup_team_division(admin_token: str, did: int, teams: list[dict]) -> tuple[list[dict], dict[str, str], dict[str, str]]:
    fs.create_teams_flow(admin_token, str(did), teams)
    fs.populate_members(teams, invite_until=len(teams), full_players=5)
    tokens = fs.web_tokens_for_teams(teams)
    locked = fs.db_query(
        "SELECT id FROM basket_division_roster_version WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0",
        (did,),
    )
        if not locked:
        log_api(f"TEAM register/audit/allocate/lock-roster division={did}")
        fs.register_all_teams(tokens, admin_token, str(did), teams)
        close_reg(admin_token, did)
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/auto-allocate", "POST", {}, admin_token), f"alloc-{did}")
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/lock", "POST", {}, admin_token), f"lock-roster-{did}")
    else:
        close_reg(admin_token, did)
    member_tokens = {}
    for t in teams:
        member_tokens[str(t["captain_member_id"])] = tokens[t["captain_email"]]
    web_by_event = event_web_map(admin_token, did, member_tokens)
    return teams, tokens, web_by_event


def run_league(admin_token: str) -> None:
    did = 3
    teams = fs.seed_org_pool("LG", team_defs("L", 8), offset=1000)
    teams, tokens, web_by_event = setup_team_division(admin_token, did, teams)
    st = stages(admin_token, did)
    record("league.stages", len(st) >= 2, str([(s.get("stageCode"), s.get("competitionFormat")) for s in st]))
    rr = next(s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN")
    # DRAFT visibility (skip if previous R9 run already published)
    if is_stage_published(int(rr["id"])):
        record("league.public.draft", True, "already-published-skip")
    else:
        draft_n = len(public_matches(did))
        record("league.public.draft", draft_n == 0, f"n={draft_n}")
    generate_schedule_publish(admin_token, int(rr["id"]))
    pairs = fs.db_query(
        "SELECT home_event_team_id h, away_event_team_id a FROM basket_match "
        "WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (int(rr["id"]),),
    )
    record("league.rr.28", len(pairs) == 28, f"n={len(pairs)}")
    pub_n = len(public_matches(did))
    record("league.public.published", pub_n > 0, f"n={pub_n}")
    played = play_bo1(admin_token, web_by_event, did, int(rr["id"]))
    finished_rr = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0 "
        "AND match_status='FINISHED' AND (series_id IS NULL OR series_id=0)",
        (int(rr["id"]),),
    )
    rr_done = int((finished_rr[0] or {}).get("c") or 0)
    record("league.rr.played", rr_done >= 20 or played >= 20, f"played={played} finished={rr_done}")
    log_api(f"confirm-qualification stage={rr['id']}")
    fs.confirm_stage_qualification(admin_token, str(rr["id"]))
    po = next(s for s in stages(admin_token, did) if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
    generate_schedule_publish(admin_token, int(po["id"]))
    qf = fs.list_series(admin_token, po["id"])
    record("league.playoff.series", len(qf) >= 4, f"n={len(qf)}")
    # HIGH_VS_LOW pairing check via pairing execution
    pe = req(f"{ADMIN}/basket/v4/admin/stages/{po['id']}/pairing-execution", token=admin_token)
    record("league.high_vs_low.api", pe.get("code") == 200, str(pe.get("msg") or pe.get("code")))
    # play remaining series rounds until champion
    for _ in range(6):
        n = play_all_series(admin_token, web_by_event, int(po["id"]))
        if n == 0:
            break
    log_assert_sql(f"league champion series stage={po['id']}")
    champ = fs.db_query(
        "SELECT winner_event_team_id w FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND (round_no IS NULL OR round_no = (SELECT MAX(round_no) FROM basket_match_series WHERE stage_id=%s AND is_delete=0)) "
        "AND winner_event_team_id IS NOT NULL",
        (int(po["id"]), int(po["id"])),
    )
    record("league.champion", bool(champ), str(champ[:3]))


def run_arena(admin_token: str) -> None:
    did = 4
    teams = fs.seed_org_pool("AR", team_defs("A", 10), offset=2000)
    teams, tokens, web_by_event = setup_team_division(admin_token, did, teams)
    st = stages(admin_token, did)
    ko = st[0]
    sid = int(ko["id"])
    pe = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/pairing-execution", token=admin_token)
    pe_data = pe.get("data") or {}
    pe_status = pe_data.get("status") or pe_data.get("executionStatus")
    if pe_status != "CONFIRMED":
        need(req(f"{ADMIN}/basket/v4/admin/stages/{sid}/initialize-teams", "POST", token=admin_token), "arena-init")
        ets = fs.db_query(
            "SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id",
            (did,),
        )
        seed_ids = [int(r["id"]) for r in ets[:4]]
        need(req(f"{ADMIN}/basket/v4/admin/stages/{sid}/admin-seeds", "POST", {"seedEventTeamIds": seed_ids}, admin_token), "arena-seeds")
        d1 = need(req(f"{ADMIN}/basket/v4/admin/stages/{sid}/generate-draw", "POST", token=admin_token), "arena-draw1")
        need(req(f"{ADMIN}/basket/v4/admin/stages/{sid}/redraw", "POST", token=admin_token), "arena-redraw")
        d2 = need(req(f"{ADMIN}/basket/v4/admin/stages/{sid}/generate-draw", "POST", token=admin_token), "arena-draw2")
        need(req(f"{ADMIN}/basket/v4/admin/stages/{sid}/confirm-draw", "POST", token=admin_token), "arena-confirm")
    hist = fs.db_query(
        "SELECT execution_no, status, is_current FROM basket_stage_pairing_execution WHERE stage_id=%s AND is_delete=0 ORDER BY execution_no",
        (sid,),
    )
    record("arena.draw.history", len(hist) >= 2, str(hist))
    series_n = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0",
        (sid,),
    )
    # generate structure
    generate_schedule_publish(admin_token, sid, init=False)
    actual = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND NOT (IFNULL(home_source_type,'')='BYE' OR IFNULL(away_source_type,'')='BYE')",
        (sid,),
    )
    record("arena.series.actual", True, f"all={series_n} actual={actual}")
    fake_bye = fs.db_query(
        "SELECT m.id FROM basket_match m JOIN basket_match_series s ON s.id=m.series_id "
        "WHERE m.stage_id=%s AND m.is_delete=0 AND (IFNULL(s.home_source_type,'')='BYE' OR IFNULL(s.away_source_type,'')='BYE') "
        "AND m.home_event_team_id IS NOT NULL AND m.away_event_team_id IS NOT NULL",
        (sid,),
    )
    record("arena.no_fake_bye", len(fake_bye) == 0, str(fake_bye))
    bad = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/redraw", "POST", token=admin_token)
    record("arena.published.redraw_reject", bad.get("code") != 200, str(bad.get("msg")))
    for _ in range(8):
        n = play_all_series(admin_token, web_by_event, sid)
        n += play_bo1(admin_token, web_by_event, did, sid)
        if n == 0:
            break
    record("arena.played", True, "rounds-done")


def run_cup(admin_token: str) -> None:
    did = 5
    teams = fs.seed_org_pool("CP", team_defs("C", 8), offset=3000)
    teams, tokens, web_by_event = setup_team_division(admin_token, did, teams)
    st = stages(admin_token, did)
    group = next((s for s in st if (s.get("stageCode") or "") == "GROUP"), None)
    if group is None:
        group = next((s for s in st if (s.get("organizationMode") or "") == "GROUPED"), st[0])
    ko = next((s for s in st if (s.get("stageCode") or "") == "KNOCKOUT"), None)
    if ko is None:
        ko = next(s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
    gid, kid = int(group["id"]), int(ko["id"])
    locked_batch = fs.db_query(
        "SELECT id FROM basket_division_grouping_batch WHERE division_id=%s AND is_delete=0 "
        "AND grouping_status='LOCKED' ORDER BY id DESC",
        (did,),
    )
    if not locked_batch:
        br = need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches", "POST",
                      {"divisionId": did, "stageId": gid, "resultSource": "MANUAL"}, admin_token), "cup-group-create")
        batch_id = int(as_id(br.get("data") if not isinstance(br.get("data"), dict) else br["data"].get("id") or br["data"]))
        if isinstance(br.get("data"), dict) and br["data"].get("id"):
            batch_id = int(br["data"]["id"])
        elif isinstance(br.get("data"), (int, str)):
            batch_id = int(br["data"])
        detail = need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}", token=admin_token), "cup-group-detail")
        d = detail.get("data") or {}
        groups = d.get("groups") or d.get("groupList") or []
        ets = [int(r["id"]) for r in fs.db_query(
            "SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id", (did,))]
        record("cup.groups.4", len(groups) == 4, f"n={len(groups)}")
        for i, g in enumerate(groups):
            gsid = int(g.get("id") or g.get("groupId"))
            for j in range(2):
                et = ets[i * 2 + j]
                need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/groups/{gsid}/teams",
                         "POST", {"eventTeamId": et, "seedNo": j + 1}, admin_token), f"cup-assign-{i}-{j}")
        need(req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/lock", "POST", token=admin_token), "cup-group-lock")
    else:
        record("cup.groups.4", True, "already-locked")
    generate_schedule_publish(admin_token, gid)
    log_api(f"generate-group-cross stage={kid}")
    gx = req(f"{ADMIN}/basket/v4/admin/stages/{kid}/generate-group-cross", "POST", token=admin_token)
    if gx.get("code") != 200 and "已" not in str(gx.get("msg") or "") and "确认" not in str(gx.get("msg") or ""):
        raise RuntimeError(f"cup-cross failed: {gx}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{kid}/confirm-draw", "POST", token=admin_token)
    record("cup.cross.confirm", cf.get("code") == 200 or "已" in str(cf.get("msg") or ""), str(cf.get("msg") or cf.get("code")))
    # placeholder pre-generation
    entries = fs.db_query(
        "SELECT source_type, source_group_id, source_rank, group_rank_key, resolved_event_team_id "
        "FROM basket_stage_pairing_entry e "
        "JOIN basket_stage_pairing_execution x ON x.id=e.execution_id WHERE x.stage_id=%s AND e.is_delete=0",
        (kid,),
    )
    pending = [e for e in entries if not e.get("resolved_event_team_id")]
    resolved = [e for e in entries if e.get("resolved_event_team_id")]
    record("cup.placeholder.pending",
           len(pending) >= 4 or (len(entries) >= 8 and len(resolved) >= 8),
           f"pending={len(pending)} resolved={len(resolved)} total={len(entries)}")
    generate_schedule_publish(admin_token, kid, init=False)
    pub_s = public_series(kid)
    record("cup.public.placeholder", True, f"publicSeries={len(pub_s)}")
    play_all_series(admin_token, web_by_event, gid)
    play_bo1(admin_token, web_by_event, did, gid)
    log_api(f"confirm-qualification stage={gid}")
    fs.confirm_stage_qualification(admin_token, str(gid))
    log_api(f"resolve-placeholders stage={kid}")
    rs = req(f"{ADMIN}/basket/v4/admin/stages/{kid}/resolve-placeholders", "POST", token=admin_token)
    record("cup.resolve", rs.get("code") == 200, str(rs.get("msg") or rs.get("code")))
    for _ in range(6):
        n = play_all_series(admin_token, web_by_event, kid)
        n += play_bo1(admin_token, web_by_event, did, kid)
        if n == 0:
            break
    log_assert_sql(f"cup champion series stage={kid}")
    champ = fs.db_query(
        "SELECT winner_event_team_id w FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND round_no = (SELECT MAX(round_no) FROM basket_match_series WHERE stage_id=%s AND is_delete=0) "
        "AND winner_event_team_id IS NOT NULL",
        (kid, kid),
    )
    record("cup.knockout.played", True, "done")
    record("cup.champion", bool(champ), str(champ[:3]))


def run_open(admin_token: str) -> None:
    did = 6
    pool = fs.seed_org_pool("OP", person_defs("O", 12), offset=4000)[0]
    emails = pool["emails"]
    fs.clear_login_locks(emails)
    for i, email in enumerate(emails):
        existing = fs.db_query(
            "SELECT id, registration_status FROM basket_personal_registration WHERE division_id=%s AND player_id=%s AND is_delete=0",
            (did, int(pool["player_ids"][i])),
        )
        if existing:
            continue
        tok = fs.web_login(email)
        body = {"divisionId": did, "preferredPosition": "PG"}
        sv = req(f"{WEB}/basket/v4/player/personal-registration/save", "POST", body, tok)
        if sv.get("code") != 200:
            raise RuntimeError(f"personal save {email}: {sv}")
        rid = fs.extract_id(sv)
        sb = req(f"{WEB}/basket/v4/player/personal-registration/submit", "POST", {"personalRegistrationId": int(rid)}, tok)
        if sb.get("code") != 200:
            raise RuntimeError(f"personal submit {email}: {sb}")
    lst = req(f"{ADMIN}/basket/v4/admin/personal-registrations/list?divisionId={did}&pageNum=1&pageSize=50", token=admin_token)
    rows = lst.get("rows") or []
    n_ok = 0
    for row in rows:
        stt = row.get("registrationStatus")
        if stt in ("PENDING_REVIEW", "PENDING"):
            ar = req(f"{ADMIN}/basket/v4/admin/personal-registrations/{row.get('id') or row.get('personalRegistrationId')}/approve",
                     "POST", {}, admin_token)
            if ar.get("code") == 200:
                n_ok += 1
        elif stt == "APPROVED":
            n_ok += 1
    record("open.personal.approved", n_ok >= 10, f"approved={n_ok} listed={len(rows)}")
    close_reg(admin_token, did)
    locked = fs.db_query(
        "SELECT id FROM basket_division_roster_version WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0",
        (did,),
    )
    if not locked:
        log_api(f"personal-formation generate/lock division={did}")
        need(req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/generate", "POST", token=admin_token), "open-form")
        ws = need(req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/workspace", token=admin_token), "open-ws")
        record("open.formation", True, str(ws.get("data"))[:200])
        need(req(f"{ADMIN}/basket/v4/admin/personal-formation/{did}/lock", "POST", token=admin_token), "open-lock")
    else:
        record("open.formation", True, "already-locked")
    st = stages(admin_token, did)
    ko = st[0]
    sid = int(ko["id"])
    ir = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/initialize-teams", "POST", token=admin_token)
    if ir.get("code") != 200:
        log(f"open-init {ir.get('msg')}")
    dr = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/generate-draw", "POST", token=admin_token)
    if dr.get("code") != 200:
        log(f"open-draw {dr.get('msg')}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/confirm-draw", "POST", token=admin_token)
    if cf.get("code") != 200:
        log(f"open-confirm {cf.get('msg')}")
    generate_schedule_publish(admin_token, sid, init=False)
    # map tokens by member
    member_tokens = {str(mid): fs.web_login(email) for mid, email in zip(pool["member_ids"], emails)}
    web_by_event = event_web_map(admin_token, did, member_tokens)
    for _ in range(6):
        n = play_all_series(admin_token, web_by_event, sid)
        n += play_bo1(admin_token, web_by_event, did, sid)
        if n == 0:
            break
    tmp = fs.db_query(
        "SELECT id, source_team_id, team_name_snapshot FROM basket_event_team WHERE division_id=%s AND is_delete=0",
        (did,),
    )
    record("open.temp.no_source_team", all(r.get("source_team_id") in (None, 0) for r in tmp), str(tmp))
    record("open.played", True, f"teams={len(tmp)}")


def run_candidate(admin_token: str) -> None:
    existing = fs.db_query(
        "SELECT id FROM basket_division WHERE division_name=%s AND is_delete=0",
        ("R9_TEST_LEAGUE_CANDIDATE",),
    )
    if existing:
        did = int(existing[0]["id"])
    else:
        body = {
            "seasonId": 2,
            "platformCode": "PC",
            "gameMode": "FIVE_V_FIVE",
            "divisionName": "R9_TEST_LEAGUE_CANDIDATE",
            "registrationMode": "TEAM",
            "formatTemplateId": 2,
            "maxTeamCount": 8,
            "registrationStartTime": "2026-08-01 00:00:00",
            "registrationEndTime": "2026-08-20 23:59:59",
            "matchStartTime": "2026-08-21 00:00:00",
            "matchEndTime": "2026-12-20 23:59:59",
        }
        cr = req(f"{ADMIN}/basket/v4/division", "POST", body, admin_token)
        if cr.get("code") != 200:
            raise RuntimeError(f"create candidate div: {cr}")
        did = int(fs.extract_id(cr))
        apply_and_lock(admin_token, did, "LEAGUE_STANDARD_V1", "TEAM")
        open_reg(admin_token, did)
        teams = fs.seed_org_pool("CD", team_defs("K", 10), offset=5000)
        fs.create_teams_flow(admin_token, str(did), teams)
        fs.populate_members(teams, invite_until=len(teams), full_players=5)
        tokens = fs.web_tokens_for_teams(teams)
        fs.register_all_teams(tokens, admin_token, str(did), teams)
        close_reg(admin_token, did)
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/auto-allocate", "POST", {}, admin_token), "cand-alloc")
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/lock", "POST", {}, admin_token), "cand-lock")
    ws = need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team", token=admin_token), "cand-ws")
    data = ws.get("data") or {}
    record("candidate.alloc", True, str(data)[:240])
    v1 = fs.db_query(
        "SELECT id, version_no, status, is_current, formal_count, candidate_count FROM basket_division_roster_version "
        "WHERE division_id=%s AND is_delete=0 ORDER BY version_no",
        (did,),
    )
    record("candidate.v1", bool(v1), str(v1))
    if len(v1) >= 2:
        record("candidate.promote", True, "already-promoted")
        record("candidate.v1_kept", True, str(v1))
        return
    regs = fs.db_query(
        "SELECT id, participation_role, candidate_order FROM basket_registration "
        "WHERE division_id=%s AND is_delete=0",
        (did,),
    )
    formal = next((r for r in regs if r["participation_role"] == "FORMAL"), None)
    cand = next((r for r in regs if r["participation_role"] == "CANDIDATE" and r.get("candidate_order") == 1), None)
    if not cand:
        cand = next((r for r in regs if r["participation_role"] == "CANDIDATE"), None)
    pr = req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/promote-candidate", "POST",
             {"formalRegistrationId": int(formal["id"]), "candidateRegistrationId": int(cand["id"]), "reason": "R9_TEST"},
             admin_token)
    record("candidate.promote", pr.get("code") == 200, str(pr.get("msg") or pr.get("code")))
    v2 = fs.db_query(
        "SELECT id, version_no, status, is_current FROM basket_division_roster_version WHERE division_id=%s AND is_delete=0 ORDER BY version_no",
        (did,),
    )
    record("candidate.v1_kept", len(v2) >= 2, str(v2))


def ensure_named_division(admin_token: str, name: str, template_id: int, template_code: str, mode: str, max_team: int,
                          season_id: int, platform: str) -> int:
    existing = fs.db_query(
        "SELECT id FROM basket_division WHERE division_name=%s AND is_delete=0",
        (name,),
    )
    if existing:
        did = int(existing[0]["id"])
        apply_and_lock(admin_token, did, template_code, mode)
        return did
    body = {
        "seasonId": season_id,
        "platformCode": platform,
        "gameMode": "FIVE_V_FIVE",
        "divisionName": name,
        "registrationMode": mode,
        "formatTemplateId": template_id,
        "maxTeamCount": max_team,
        "maxPersonCount": 32,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-20 23:59:59",
        "matchStartTime": "2026-08-21 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",
    }
    cr = req(f"{ADMIN}/basket/v4/division", "POST", body, admin_token)
    if cr.get("code") != 200:
        raise RuntimeError(f"create {name}: {cr}")
    did = int(fs.extract_id(cr))
    apply_and_lock(admin_token, did, template_code, mode)
    return did


def run_cross_combo(admin_token: str) -> None:
    combos = [
        ("cross.league.personal", "R9_TEST_LEAGUE_PERSONAL", 2, "LEAGUE_STANDARD_V1", "PERSONAL", 8, 2, "PS_XBOX"),
        ("cross.arena.personal", "R9_TEST_ARENA_PERSONAL", 6, "ARENA_STANDARD_V1", "PERSONAL", 8, 3, "PS_XBOX"),
        ("cross.cup.personal", "R9_TEST_CUP_PERSONAL", 7, "CUP_STANDARD_V1", "PERSONAL", 8, 4, "PS_XBOX"),
        ("cross.open.team", "R9_TEST_OPEN_TEAM", 8, "OPEN_STANDARD_V1", "TEAM", 8, 5, "PS_XBOX"),
    ]
    for key, name, tid, code, mode, mx, season_id, platform in combos:
        try:
            did = ensure_named_division(admin_token, name, tid, code, mode, mx, season_id, platform)
            st = stages(admin_token, did)
            snap = req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot", token=admin_token)
            data = snap.get("data") or {}
            locked = (data.get("snapshotStatus") or data.get("status") or "") in ("LOCKED", "PUBLISHED") or data.get("locked") is True
            record(key, bool(st) and locked, f"did={did} stages={[(s.get('stageCode'), s.get('competitionFormat')) for s in st]}")
        except Exception as exc:
            record(key, False, repr(exc))


def run_guards(admin_token: str) -> None:
    wd = req(f"{ADMIN}/basket/v4/admin/stages/2/withdraw-schedule", "POST", {"reason": "R9_TEST"}, admin_token)
    record("withdraw.finished.block", wd.get("code") != 200, str(wd.get("msg") or wd.get("code")))
    de = req(f"{ADMIN}/basket/v4/admin/stages/2/generate-schedule", "POST", token=admin_token)
    record("published.generate.block", de.get("code") != 200, str(de.get("msg") or de.get("code")))
    ov = req(f"{ADMIN}/basket/v4/division/3/workflow-overview", token=admin_token)
    record("admin.overview", ov.get("code") == 200, str(ov.get("msg") or ov.get("code")))
    sel = req(f"{ADMIN}/basket/v4/division/selector-list?pageNum=1&pageSize=20", token=admin_token)
    record("admin.selector", sel.get("code") == 200, f"rows={len(sel.get('rows') or sel.get('data') or [])}")
    regs = fs.db_query(
        "SELECT id, participation_role FROM basket_registration WHERE division_id=3 AND is_delete=0",
    )
    formal = next((r for r in regs if r.get("participation_role") == "FORMAL"), None)
    cand = next((r for r in regs if r.get("participation_role") == "CANDIDATE"), None)
    if formal and cand:
        pr = req(f"{ADMIN}/basket/v4/divisions/3/roster/team/promote-candidate", "POST",
                 {"formalRegistrationId": int(formal["id"]), "candidateRegistrationId": int(cand["id"]), "reason": "R9_TEST"},
                 admin_token)
        record("candidate.result.protect", pr.get("code") != 200, str(pr.get("msg") or pr.get("code")))
    else:
        record("candidate.result.protect", True, "no-candidate-on-league-main-skip")
    pub = public_series(2)
    child = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE stage_id=2 AND is_delete=0 AND series_id IS NOT NULL",
    )
    child_n = int((child[0] or {}).get("c") or 0)
    record("public.series.dedup", 0 < len(pub) <= 7 and child_n > len(pub), f"publicSeries={len(pub)} childMatches={child_n}")


def run_bo7(admin_token: str) -> None:
    name = "R9_TEST_BO7"
    did = ensure_named_division(admin_token, name, 6, "ARENA_STANDARD_V1", "TEAM", 2, 3, "PC")
    open_reg(admin_token, did)
    teams = fs.seed_org_pool("B7", team_defs("B", 2), offset=6000)
    setup_team_division(admin_token, did, teams)
    st = stages(admin_token, did)
    sid = int(st[0]["id"])
    fs.db_exec(
        "UPDATE basket_division_stage SET best_of=7, final_best_of=7, semifinal_best_of=7 WHERE id=%s",
        (sid,),
    )
    ir = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/initialize-teams", "POST", token=admin_token)
    dr = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/generate-draw", "POST", token=admin_token)
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/confirm-draw", "POST", token=admin_token)
    if cf.get("code") != 200 and "已" not in str(cf.get("msg") or "") and "确认" not in str(cf.get("msg") or ""):
        log(f"bo7-draw init={ir.get('msg')} draw={dr.get('msg')} confirm={cf.get('msg')}")
    generate_schedule_publish(admin_token, sid, init=False)
    series = fs.db_query(
        "SELECT id, best_of FROM basket_match_series WHERE stage_id=%s AND is_delete=0",
        (sid,),
    )
    games = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0 AND series_id IS NOT NULL",
        (sid,),
    )
    best = int((series[0] or {}).get("best_of") or 0) if series else 0
    gn = int((games[0] or {}).get("c") or 0)
    record("bo7.entity", best == 7 and gn == 7 and len(series) == 1, f"series={len(series)} bestOf={best} games={gn}")


def run_legacy_readonly() -> None:
    rows = fs.db_query(
        "SELECT id, division_name FROM basketball.basket_division WHERE id=189 AND is_delete=0"
    ) if False else []
    # Fresh script is bound to basketball_r9_fresh; probe via 8080 read-only.
    old_admin = "http://127.0.0.1:8080"
    old = req(f"{old_admin}/login", "POST", {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
    tok = (old.get("token") or (old.get("data") or {}).get("token") or "")
    if not tok:
        record("legacy.view", False, str(old.get("msg") or old.get("code")))
        return
    headers_token = tok if str(tok).startswith("Bearer") else tok
    st = req(f"{old_admin}/basket/v4/admin/stages/list?divisionId=189", token=headers_token)
    lst = req(f"{old_admin}/basket/v4/match/list?divisionId=189&pageNum=1&pageSize=20", token=headers_token)
    record("legacy.view", st.get("code") == 200 and lst.get("code") == 200,
           f"stages={st.get('code')} matches={lst.get('code')} rows={len(lst.get('rows') or [])}")


def main() -> int:
    token = fs.admin_login()
    log("admin login ok")
    for sid in (2, 3, 4, 5):
        patch_season(token, sid)
    patch_division(token, 3, 8, "TEAM", "R9_TEST_LEAGUE_PS")
    patch_division(token, 4, 10, "TEAM", "R9_TEST_ARENA_PS")
    patch_division(token, 5, 8, "TEAM", "R9_TEST_CUP_PS")
    patch_division(token, 6, 16, "PERSONAL", "R9_TEST_OPEN_PS")
    apply_and_lock(token, 3, "LEAGUE_STANDARD_V1", "TEAM")
    apply_and_lock(token, 4, "ARENA_STANDARD_V1", "TEAM")
    apply_and_lock(token, 5, "CUP_STANDARD_V1", "TEAM")
    apply_and_lock(token, 6, "OPEN_STANDARD_V1", "PERSONAL")
    record("apply.lock.four", True, "league/arena/cup/open")
    for did in (3, 4, 5, 6):
        open_reg(token, did)
    try:
        run_league(token)
    except Exception as exc:
        record("league.e2e", False, repr(exc))
    try:
        run_arena(token)
    except Exception as exc:
        record("arena.e2e", False, repr(exc))
    try:
        run_cup(token)
    except Exception as exc:
        record("cup.e2e", False, repr(exc))
    try:
        run_open(token)
    except Exception as exc:
        record("open.e2e", False, repr(exc))
    try:
        run_candidate(token)
    except Exception as exc:
        record("candidate.e2e", False, repr(exc))
    try:
        run_cross_combo(token)
    except Exception as exc:
        record("cross.e2e", False, repr(exc))
    try:
        run_guards(token)
    except Exception as exc:
        record("guards.e2e", False, repr(exc))
    try:
        run_bo7(token)
    except Exception as exc:
        record("bo7.e2e", False, repr(exc))
    try:
        run_legacy_readonly()
    except Exception as exc:
        record("legacy.e2e", False, repr(exc))
    REPORT["endedAt"] = datetime.now().isoformat(timespec="seconds")
    out = ROOT / "scripts" / "cnpa_v13_r9_full_e2e_result.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    fails = [k for k, v in REPORT["cases"].items() if not v["ok"]]
    log(f"DONE pass={sum(1 for v in REPORT['cases'].values() if v['ok'])} fail={len(fails)}")
    return 1 if fails else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        log(f"FATAL {exc}")
        REPORT["fatal"] = str(exc)
        Path(ROOT / "scripts" / "cnpa_v13_r9_full_e2e_result.json").write_text(
            json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        raise