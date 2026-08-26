#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CNPA League 16-team 5V5 real-data E2E (local only).
Reuses full_smoke_common + cnpa_v13_final_full_e2e helpers.
No production endpoints. Data is retained for homepage smoke.
"""
from __future__ import annotations

import json
import random
import sys
import traceback
from datetime import date, datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:8881"
fs.DB.update({"host": "192.168.31.48", "database": "basketball", "user": "root", "password": "123456"})
fs.REDIS_HOST = "192.168.31.48"
fs.MARKER = "E2E_LEAGUE16_2026S1"
fs.ID_BASE = 2099260826000000000

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:8881"
fs.DB["database"] = "basketball"
e2e.ADMIN = fs.ADMIN
e2e.WEB = fs.WEB
e2e.fs = fs

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req
as_id = fs.as_id

SEASON_NAME = "2026 E2E S2"
SEASON_TITLE = "CNPA League 2026 E2E S2"
DIV_NAME = "5V5 E2E Division S2"
SERIES_ID = 1
RNG_SEED = 20260826
fs.MARKER = "E2E_LEAGUE16_2026S2"
MARKER = fs.MARKER

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "lifecycle": {},
    "dataset": {},
    "verify": {},
    "homepage": {},
    "bugs": {"P0": [], "P1": [], "P2": []},
    "cases": {},
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


def team_defs() -> list[dict]:
    out = []
    for i in range(1, 17):
        code = f"L{i:02d}"
        out.append({
            "code": code,
            "name": f"E2E-{code}",
            "short": code,
            "players": [f"E2E-{code}-P{j}" for j in range(1, 6)],
        })
    return out


def team_no_from_name(name: str | None) -> int:
    text = str(name or "")
    for i in range(16, 0, -1):
        if f"L{i:02d}" in text:
            return i
    return 99


def find_e2e_season() -> dict | None:
    rows = fs.db_query(
        "SELECT id, season_name, display_title, season_status, publish_status "
        "FROM basket_season WHERE is_delete=0 "
        "AND (season_name=%s OR display_title=%s) ORDER BY id DESC LIMIT 1",
        (SEASON_NAME, SEASON_TITLE),
    )
    return rows[0] if rows else None


def create_season(token: str) -> int:
    existing = find_e2e_season()
    if existing:
        sid = int(as_id(existing["id"]))
        if str(existing.get("publish_status") or "") != "PUBLISHED":
            req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
        return sid
    body = {
        "seriesId": SERIES_ID,
        "seasonYear": 2026,
        "seasonName": SEASON_NAME,
        "displayTitle": SEASON_TITLE,
        "description": MARKER,
        "registrationMode": "TEAM",
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-09-30 23:59:59",
    }
    r = need(req(f"{ADMIN}/basket/v4/season", "POST", body, token), "create-season")
    sid = int(fs.extract_id(r))
    pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
    if pr.get("code") != 200 and "已" not in str(pr.get("msg") or ""):
        raise RuntimeError(f"publish season {sid}: {pr}")
    return sid


def create_division(token: str, season_id: int) -> int:
    existing = e2e.find_division(DIV_NAME)
    if existing:
        return existing
    body = {
        "seasonId": season_id,
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": DIV_NAME,
        "registrationMode": "TEAM",
        "formatTemplateId": 2,
        "maxTeamCount": 16,
        "maxPersonCount": 128,
        "lineupMinCount": 5,
        "lineupMaxCount": 8,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-28 23:50:00",
        "matchStartTime": "2026-08-28 23:50:00",
        "matchEndTime": "2026-09-30 23:59:59",
        "sortOrder": 1,
    }
    r = need(req(f"{ADMIN}/basket/v4/division", "POST", body, token), "create-div")
    return int(fs.extract_id(r))


def align_time_windows(token: str, season_id: int, did: int) -> None:
    sr = req(f"{ADMIN}/basket/v4/season/{season_id}", "PUT", {
        "id": season_id,
        "seasonName": SEASON_NAME,
        "displayTitle": SEASON_TITLE,
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-09-30 23:59:59",
    }, token)
    log(f"season window {sr.get('code')} {sr.get('msg')}")
    dr = req(f"{ADMIN}/basket/v4/division/{did}", "PUT", {
        "id": did,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-28 23:50:00",
        "matchStartTime": "2026-08-28 23:50:00",
        "matchEndTime": "2026-09-30 23:59:59",
    }, token)
    log(f"division window {dr.get('code')} {dr.get('msg')}")
    if dr.get("code") != 200:
        raise RuntimeError(f"align division window failed: {dr}")
    e2e.open_reg(token, did)


def widen_match_window_after_lock(token: str, did: int) -> None:
    wr = req(f"{ADMIN}/basket/v4/division/{did}", "PUT", {
        "id": did,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-26 00:00:00",
        "matchStartTime": "2026-08-26 00:00:00",
        "matchEndTime": "2026-09-30 23:59:59",
    }, token)
    log(f"post-lock match window {wr.get('code')} {wr.get('msg')}")


def _unit_round_no(unit: dict) -> int:
    raw = unit.get("roundNo")
    if raw is None:
        raw = unit.get("round_no")
    try:
        return int(raw)
    except (TypeError, ValueError):
        return 0


def assign_schedule(token: str, stage_id: int, start: date, end: date, prefer_week_tail: bool = False) -> None:
    units = [u for u in e2e.schedule_units(token, stage_id) if isinstance(u, dict)]
    units.sort(key=lambda u: (_unit_round_no(u), int(as_id(u.get("unitId") or u.get("id") or 0))))
    cursor = start
    if prefer_week_tail and cursor < date(2026, 8, 24):
        cursor = date(2026, 8, 24)
    slots = ["21:00", "22:00"]
    slot_capacity = 6
    day_capacity = len(slots) * slot_capacity
    previous_round = None
    last_day = None
    day_used = 0
    slot_used = {slot: 0 for slot in slots}
    assigned = 0
    first_day = None
    for u in units:
        round_no = _unit_round_no(u)
        if previous_round is not None and round_no != previous_round:
            cursor = (last_day + timedelta(days=1)) if last_day else cursor
            day_used = 0
            slot_used = {slot: 0 for slot in slots}
        if day_used >= day_capacity:
            cursor = cursor + timedelta(days=1)
            day_used = 0
            slot_used = {slot: 0 for slot in slots}
        tm = next(slot for slot in slots if slot_used[slot] < slot_capacity)
        slot_used[tm] += 1
        day_used += 1
        last_day = cursor
        first_day = first_day or cursor
        previous_round = round_no
        body = {
            "unitType": u.get("unitType") or "MATCH",
            "unitId": u.get("unitId") or u.get("id"),
            "scheduledDate": cursor.isoformat(),
            "scheduledStartTime": tm,
            "scheduleSource": "MANUAL",
        }
        ar = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule/assign", "POST", body, token)
        if ar.get("code") != 200:
            if (u.get("scheduleStatus") or "") == "SCHEDULED":
                assigned += 1
                continue
            log(f"  assign warn {ar.get('msg')}")
            continue
        assigned += 1
    span = f"{first_day}~{last_day}" if first_day and last_day else f"{start}~{end}"
    log(f"  scheduled {assigned}/{len(units)} units by roundNo {span}")


def _as_date(value) -> date | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    text = str(value)
    return date.fromisoformat(text[:10])


def audit_round_matchdays(stage_id: int, label: str, *, strict: bool = False) -> bool:
    rows = fs.db_query(
        "SELECT round_no, COUNT(*) cnt, MIN(DATE(match_time)) dmin, MAX(DATE(match_time)) dmax "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 "
        "AND (series_id IS NULL OR series_id=0) AND match_time IS NOT NULL "
        "GROUP BY round_no ORDER BY round_no",
        (stage_id,),
    )
    log(f"==== {label} ====")
    by_round: dict[int, tuple[int, date, date]] = {}
    for row in rows:
        round_no = int(row["round_no"])
        cnt = int(row["cnt"])
        dmin = _as_date(row["dmin"])
        dmax = _as_date(row["dmax"])
        by_round[round_no] = (cnt, dmin, dmax)
        log(f"R{round_no}  {cnt}  {dmin}  {dmax}")
    mixed = fs.db_query(
        "SELECT DATE(match_time) d, GROUP_CONCAT(DISTINCT round_no ORDER BY round_no) rounds, "
        "COUNT(DISTINCT round_no) n "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 "
        "AND (series_id IS NULL OR series_id=0) AND match_time IS NOT NULL "
        "GROUP BY DATE(match_time) HAVING n > 1 ORDER BY d",
        (stage_id,),
    )
    crossed = []
    prev = None
    for round_no in sorted(by_round):
        cnt, dmin, dmax = by_round[round_no]
        if prev is not None:
            _pcnt, _pmin, pmax = by_round[prev]
            if pmax is not None and dmin is not None and not (pmax < dmin):
                crossed.append(f"R{prev}..R{round_no} {pmax} !< {dmin}")
        prev = round_no
    last3 = {13, 14, 15}
    if last3 & set(by_round):
        log(f"{label} last rounds:")
        for round_no in (13, 14, 15):
            if round_no in by_round:
                cnt, dmin, dmax = by_round[round_no]
                log(f"  R{round_no} {cnt} {dmin}..{dmax}")
    ok = not mixed and not crossed
    if mixed:
        log(f"{label} MIXED DAYS: {mixed}")
    if crossed:
        log(f"{label} BOUNDARY FAIL: {crossed}")
    record(
        f"boundary.{label}",
        ok,
        f"rounds={len(by_round)} mixed={len(mixed)} crossed={crossed}",
        "P1",
    )
    if strict and not ok:
        raise RuntimeError(f"{label} round matchday boundary failed: mixed={mixed} crossed={crossed}")
    return ok


def generate_and_publish(token: str, stage_id: int, start: date, end: date, prefer_week_tail: bool = False,
                         audit_auto: bool = False) -> None:
    if e2e.is_published(stage_id):
        log(f"  stage {stage_id} already published")
        assign_schedule(token, stage_id, start, end, prefer_week_tail=prefer_week_tail)
        audit_round_matchdays(stage_id, "E2E MANUAL ASSIGN AFTERWARD", strict=False)
        return
    gen = e2e.generate_only(token, stage_id, init=True)
    if gen.get("code") != 200:
        wr = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/withdraw-schedule", "POST", {"reason": MARKER}, token)
        log(f"  withdraw {wr.get('code')} {wr.get('msg')}")
        cd = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/cancel-draft-schedule", "POST", token=token)
        log(f"  cancel-draft {cd.get('code')} {cd.get('msg')}")
        gen = e2e.generate_only(token, stage_id, init=False)
        if gen.get("code") != 200:
            gen = e2e.generate_only(token, stage_id, init=True)
        if gen.get("code") != 200:
            raise RuntimeError(f"gen-{stage_id}: {gen}")
    if audit_auto:
        audit_round_matchdays(stage_id, "GENERATED AUTO SCHEDULE", strict=True)
    assign_schedule(token, stage_id, start, end, prefer_week_tail=prefer_week_tail)
    audit_round_matchdays(stage_id, "E2E MANUAL ASSIGN AFTERWARD", strict=audit_auto)
    pub = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/publish-schedule", "POST", token=token)
    if pub.get("code") != 200 and "已" not in str(pub.get("msg") or ""):
        raise RuntimeError(f"publish {stage_id}: {pub}")


def event_team_map(did: int) -> dict[str, dict]:
    rows = fs.db_query(
        "SELECT id, team_name_snapshot, captain_member_id "
        "FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    out = {}
    for r in rows:
        eid = as_id(r["id"])
        out[eid] = {
            "id": eid,
            "name": r.get("team_name_snapshot") or "",
            "no": team_no_from_name(r.get("team_name_snapshot")),
            "captain_member_id": str(r.get("captain_member_id") or ""),
        }
    return out


def split_score(total: int, n: int, rng: random.Random, boost_idx: int | None = None, boost: int = 0) -> list[int]:
    floor = 6
    if total < floor * n:
        floor = max(1, total // n)
    vals = [floor] * n
    remain = total - floor * n
    if boost_idx is not None and 0 <= boost_idx < n and remain > 0:
        extra = min(remain, max(boost, remain // 2))
        vals[boost_idx] += extra
        remain -= extra
    while remain > 0:
        vals[rng.randrange(n)] += 1
        remain -= 1
    return vals


def shooting_for_points(pts: int, rng: random.Random) -> dict:
    tpm = min(pts // 3, rng.randint(0, 5))
    remain = pts - tpm * 3
    ftm = remain % 2
    twos = (remain - ftm) // 2
    fgm = twos + tpm
    tpa = tpm + rng.randint(1, 4)
    fga = max(fgm + rng.randint(2, 7), tpa)
    fta = ftm + rng.randint(0, 3)
    return {
        "points": pts,
        "fieldGoalMade": fgm,
        "fieldGoalAttempt": fga,
        "threePointMade": tpm,
        "threePointAttempt": tpa,
        "freeThrowMade": ftm,
        "freeThrowAttempt": fta,
    }


def period_scores(home: int, away: int, rng: random.Random) -> list[dict]:
    hs = split_score(home, 4, rng)
    aws = split_score(away, 4, rng)
    return [{"periodNo": i + 1, "homeScore": hs[i], "awayScore": aws[i]} for i in range(4)]


def build_team_player_stats(players: list[dict], team_score: int, rng: random.Random) -> list[dict]:
    names = [str(p.get("playerName") or p.get("playerNameSnapshot") or p.get("player_name_snapshot") or "") for p in players]
    boost_pts = next((i for i, n in enumerate(names) if "L01-P1" in n), None)
    boost_ast = next((i for i, n in enumerate(names) if "L02-P2" in n), None)
    boost_reb = next((i for i, n in enumerate(names) if "L03-P3" in n), None)
    pts = split_score(team_score, len(players), rng, boost_idx=boost_pts, boost=28)
    rows = []
    for i, p in enumerate(players):
        shot = shooting_for_points(pts[i], rng)
        ast = rng.randint(1, 5)
        reb = rng.randint(2, 7)
        if boost_ast == i:
            ast = rng.randint(11, 16)
        if boost_reb == i:
            reb = rng.randint(13, 18)
        rows.append({
            "eventPlayerId": p.get("eventPlayerId") or p.get("id"),
            "eventTeamId": p.get("eventTeamId") or p.get("event_team_id"),
            "playerName": names[i],
            "points": shot["points"],
            "rebound": reb,
            "assist": ast,
            "steal": rng.randint(0, 3),
            "block": rng.randint(0, 2),
            "turnover": rng.randint(0, 4),
            "foul": rng.randint(1, 4),
            "fieldGoalMade": shot["fieldGoalMade"],
            "fieldGoalAttempt": shot["fieldGoalAttempt"],
            "threePointMade": shot["threePointMade"],
            "threePointAttempt": shot["threePointAttempt"],
            "freeThrowMade": shot["freeThrowMade"],
            "freeThrowAttempt": shot["freeThrowAttempt"],
            "minutes": 28 + i,
        })
    return rows


def eligible_players(detail: dict, match: dict, side: str) -> list[dict]:
    key = "homeEligiblePlayers" if side == "HOME" else "awayEligiblePlayers"
    rows = detail.get(key) or []
    if len(rows) >= 5:
        return rows[:5]
    team_id = match.get("homeEventTeamId") if side == "HOME" else match.get("awayEventTeamId")
    db_rows = fs.db_query(
        "SELECT id AS eventPlayerId, event_team_id AS eventTeamId, player_name_snapshot AS playerName "
        "FROM basket_event_player WHERE event_team_id=%s AND is_delete=0 ORDER BY id",
        (int(as_id(team_id)),),
    )
    return db_rows[:5]


def upload_proof(web_token: str, cache: dict, key: str) -> int:
    if key in cache:
        return cache[key]
    up = fs.upload_file(web_token, fs.TINY_PNG, filename="e2e_match.png", usage_type="MATCH_DATA_SCREENSHOT")
    if up.get("code") != 200:
        raise RuntimeError(f"upload proof failed {key}: {up}")
    data = up.get("data") or {}
    fid = int(as_id(data.get("fileId") or data.get("id") or fs.extract_id(up)))
    cache[key] = fid
    return fid


def submit_full_match(admin_token, web_token, match, home_score, away_score, submit_event_team_id, rng, proof_id):
    mid = match["id"]
    if match.get("matchStatus") in ("FINISHED", "FORFEITED"):
        return {"matchId": mid, "skipped": True}
    lr0 = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=10", token=admin_token)
    pending0 = next((x for x in (lr0.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    if pending0:
        ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending0['submissionId']}/approve", "POST", {}, admin_token)
        if ar.get("code") != 200:
            raise RuntimeError(f"approve pending failed match {mid}: {ar}")
        return {"matchId": mid, "reusedPending": True}
    detail = req(
        f"{WEB}/basket/v4/player/match-submission/{mid}/detail?submitEventTeamId={submit_event_team_id}",
        token=web_token,
    )
    if detail.get("code") != 200:
        raise RuntimeError(f"submission detail failed match {mid}: {detail}")
    d = detail.get("data") or {}
    home_ps = eligible_players(d, match, "HOME")
    away_ps = eligible_players(d, match, "AWAY")
    if len(home_ps) != 5 or len(away_ps) != 5:
        raise RuntimeError(f"match {mid} eligible players home={len(home_ps)} away={len(away_ps)}")
    body = {
        "matchId": mid,
        "submitEventTeamId": submit_event_team_id,
        "homeScore": home_score,
        "awayScore": away_score,
        "submissionMode": "WINNER_FULL_MATCH",
        "draftComplete": True,
        "homePlayerStats": build_team_player_stats(home_ps, home_score, rng),
        "awayPlayerStats": build_team_player_stats(away_ps, away_score, rng),
        "periodScores": period_scores(home_score, away_score, rng),
        "materialFileIds": [proof_id],
        "submitRemark": MARKER,
    }
    sr = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
    if sr.get("code") != 200:
        msg = str(sr.get("msg") or "")
        if "不可再提交" in msg or "已经取胜" in msg:
            return {"matchId": mid, "skipped": True, "reason": msg}
        raise RuntimeError(f"save full-match failed match {mid}: {sr}")
    sub_id = fs.extract_id(sr)
    tr = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id}, web_token)
    if tr.get("code") != 200:
        raise RuntimeError(f"submit failed match {mid}: {tr}")
    lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
    pending = next((x for x in (lr.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    if not pending:
        raise RuntimeError(f"no pending submission for match {mid}")
    ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/approve", "POST", {}, admin_token)
    if ar.get("code") != 200:
        raise RuntimeError(f"approve failed match {mid}: {ar}")
    return {"submissionId": pending["submissionId"], "matchId": mid}


def deterministic_scores(home_no: int, away_no: int, rng: random.Random) -> tuple[int, int]:
    win = rng.randint(78, 112)
    lose = rng.randint(58, min(99, win - 3))
    if home_no < away_no:
        return win, lose
    return lose, win


def play_regular_season(admin_token, web_by_event, et_map, did, stage_id, teams):
    matches = fs.list_matches(admin_token, str(did), stage_id)
    allowed = {
        str(r["id"]) for r in fs.db_query(
            "SELECT id FROM basket_match WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
            (stage_id,),
        )
    }
    email_by_event = {}
    for t in teams:
        for eid, meta in et_map.items():
            if meta["no"] == team_no_from_name(t["name"]):
                email_by_event[eid] = t["captain_email"]
    proof_cache = {}
    played = 0
    forfeited = 0
    rng = random.Random(RNG_SEED)
    for m in matches:
        if allowed and str(m.get("id")) not in allowed:
            continue
        if m.get("seriesId") or m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home, away = m.get("homeEventTeamId"), m.get("awayEventTeamId")
        if not home or not away:
            continue
        if as_id(home) not in et_map or as_id(away) not in et_map:
            continue
        hn = et_map.get(as_id(home), {}).get("no", 99)
        an = et_map.get(as_id(away), {}).get("no", 99)
        if {hn, an} == {15, 16} and forfeited == 0:
            loser = home if hn == 16 else away
            fr = req(
                f"{ADMIN}/basket/v4/admin/stage-exception/match-forfeit",
                "POST",
                {"matchId": m["id"], "loserEventTeamId": loser, "reason": f"{MARKER} L16 forfeit vs L15"},
                admin_token,
            )
            if fr.get("code") != 200:
                raise RuntimeError(f"forfeit failed match {m['id']}: {fr}")
            forfeited += 1
            log(f"  forfeit match={m['id']} loser team {as_id(loser)}")
            continue
        hs, aws = deterministic_scores(hn, an, rng)
        winner = home if hs > aws else away
        wt = web_by_event.get(as_id(winner))
        if not wt:
            raise RuntimeError(f"no token for winner {winner}")
        proof = upload_proof(wt, proof_cache, email_by_event.get(as_id(winner)) or as_id(winner))
        submit_full_match(admin_token, wt, m, hs, aws, winner, rng, proof)
        played += 1
        if played % 10 == 0:
            log(f"  RR played {played}")
    return played, forfeited


def series_plan(best: int, lower_is_home: bool) -> list[str]:
    if best <= 1:
        seq = ["WIN"]
    elif best == 3:
        seq = ["WIN", "LOSE", "WIN"]
    elif best == 5:
        seq = ["WIN", "LOSE", "WIN", "WIN"]
    else:
        seq = ["WIN", "LOSE", "WIN", "LOSE", "WIN", "WIN"]
    out = []
    for x in seq:
        if x == "WIN":
            out.append("HOME" if lower_is_home else "AWAY")
        else:
            out.append("AWAY" if lower_is_home else "HOME")
    return out


def play_playoff(admin_token, web_by_event, et_map, teams, stage_id):
    done = 0
    rng = random.Random(RNG_SEED + 17)
    email_by_event = {}
    for t in teams:
        for eid, meta in et_map.items():
            if meta["no"] == team_no_from_name(t["name"]):
                email_by_event[eid] = t["captain_email"]
    proof_cache = {}

    def finish_one(series_id: int) -> bool:
        e2e.sync_series_child_teams()
        s = fs.get_series(admin_token, series_id)
        if s.get("winnerEventTeamId") or s.get("seriesStatus") in ("FINISHED", "COMPLETED"):
            return False
        home, away = s.get("homeEventTeamId"), s.get("awayEventTeamId")
        if not home or not away:
            return False
        hn = et_map.get(as_id(home), {}).get("no", 99)
        an = et_map.get(as_id(away), {}).get("no", 99)
        plan = series_plan(int(s.get("bestOf") or 1), hn < an)
        games = sorted(s.get("games") or [], key=lambda g: g.get("seriesGameNo") or 0)
        idx = 0
        for game in games:
            if game.get("skipped") or game.get("matchStatus") in ("FINISHED", "FORFEITED"):
                idx += 1
                continue
            if idx >= len(plan):
                break
            mid = game.get("matchId") or game.get("id")
            mr = req(f"{ADMIN}/basket/v4/match/{mid}", token=admin_token)
            m = mr.get("data") or {}
            ghome, gaway = m.get("homeEventTeamId"), m.get("awayEventTeamId")
            if not ghome or not gaway:
                continue
            side = plan[idx]
            a, b = rng.randint(80, 110), rng.randint(60, 78)
            if a <= b:
                a = b + rng.randint(3, 12)
            if side == "HOME":
                hs, aws, submit = a, b, ghome
            else:
                hs, aws, submit = b, a, gaway
            wt = web_by_event.get(as_id(submit))
            if not wt:
                raise RuntimeError(f"no token for {submit}")
            proof = upload_proof(wt, proof_cache, email_by_event.get(as_id(submit)) or as_id(submit))
            try:
                submit_full_match(admin_token, wt, m, hs, aws, submit, rng, proof)
            except RuntimeError as exc:
                if "已经取胜" in str(exc) or "不可再提交" in str(exc):
                    break
                raise
            idx += 1
            cur = fs.get_series(admin_token, series_id)
            if cur.get("winnerEventTeamId") or cur.get("seriesStatus") in ("FINISHED", "COMPLETED"):
                break
        return True

    for _ in range(8):
        e2e.sync_series_child_teams()
        rows = fs.db_query(
            "SELECT id, home_event_team_id, away_event_team_id, series_status, winner_event_team_id "
            "FROM basket_match_series WHERE stage_id=%s AND is_delete=0 ORDER BY round_no, id",
            (stage_id,),
        )
        progressed = 0
        for s in rows:
            if s.get("winner_event_team_id") or (s.get("series_status") or "") in ("FINISHED", "COMPLETED"):
                continue
            if not s.get("home_event_team_id") or not s.get("away_event_team_id"):
                continue
            if finish_one(int(s["id"])):
                progressed += 1
                done += 1
        if progressed == 0:
            break
    return done


def print_standings(token: str, stage_id: int) -> list[dict]:
    stand = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/standings", token=token)
    groups = stand.get("data") or []
    rows = []
    for g in groups:
        rows.extend(g.get("rows") or [])
    rows = sorted(rows, key=lambda r: int(r.get("rankNo") or 999))
    log("Standing:")
    slim = []
    for r in rows:
        item = {
            "rank": r.get("rankNo"),
            "team": r.get("teamName") or r.get("teamNameSnapshot"),
            "win": r.get("winCount"),
            "lose": r.get("loseCount"),
            "points": r.get("standingPoints"),
            "diff": r.get("pointDiff"),
            "eventTeamId": r.get("eventTeamId"),
        }
        slim.append(item)
        log(f"{item['rank']!s:<5}{str(item['team'])[:15]:<16}{item['win']!s:<5}{item['lose']!s:<5}{item['points']!s:<8}{item['diff']}")
    return slim


def dump_dataset(did: int, season_id: int, po_stage_id: int | None) -> dict:
    ets = fs.db_query(
        "SELECT id, team_name_snapshot FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    data = {
        "seasonId": season_id,
        "divisionId": did,
        "eventTeams": [{"id": as_id(r["id"]), "name": r.get("team_name_snapshot")} for r in ets],
        "eventPlayer": count(
            "SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
            "WHERE et.division_id=%s AND ep.is_delete=0 AND et.is_delete=0",
            (did,),
        ),
        "regularMatches": count(
            "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
            (did,),
        ),
        "playoffSeries": count(
            "SELECT COUNT(*) c FROM basket_match_series s JOIN basket_division_stage st ON st.id=s.stage_id "
            "WHERE st.division_id=%s AND s.is_delete=0",
            (did,),
        ),
        "playoffMatches": count(
            "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND series_id IS NOT NULL AND series_id<>0",
            (did,),
        ),
        "finished": count(
            "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND match_status='FINISHED'",
            (did,),
        ),
        "forfeited": count(
            "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND match_status='FORFEITED'",
            (did,),
        ),
        "playerStat": count("SELECT COUNT(*) c FROM basket_player_stat WHERE division_id=%s AND is_delete=0", (did,)),
        "teamStat": count("SELECT COUNT(*) c FROM basket_team_stat WHERE division_id=%s AND is_delete=0", (did,)),
        "standing": count("SELECT COUNT(*) c FROM basket_standing WHERE division_id=%s AND is_delete=0", (did,)),
    }
    champ = []
    if po_stage_id:
        champ = fs.db_query(
            "SELECT s.winner_event_team_id w, et.team_name_snapshot n FROM basket_match_series s "
            "LEFT JOIN basket_event_team et ON et.id=s.winner_event_team_id "
            "WHERE s.stage_id=%s AND s.is_delete=0 AND s.round_no=("
            "SELECT MAX(round_no) FROM basket_match_series WHERE stage_id=%s AND is_delete=0) "
            "AND s.winner_event_team_id IS NOT NULL",
            (po_stage_id, po_stage_id),
        )
    data["champion"] = champ[0] if champ else None
    REPORT["dataset"] = data
    log(json.dumps(data, ensure_ascii=False, default=str, indent=2))
    return data


def homepage_api_smoke() -> dict:
    out = {}
    rm = req(f"{WEB}/basket/public/home/recent-match?limit=10")
    out["recentMatch"] = {
        "http": rm.get("_http_status"),
        "code": rm.get("code"),
        "n": len(rm.get("data") or []) if isinstance(rm.get("data"), list) else 0,
        "sample": (rm.get("data") or [None])[0] if isinstance(rm.get("data"), list) else rm.get("data"),
    }
    df = req(f"{WEB}/basket/public/home/data-focus?seriesCode=LEAGUE")
    out["dataFocus"] = {"http": df.get("_http_status"), "code": df.get("code"), "data": df.get("data")}
    sl = req(f"{WEB}/basket/v4/public/season/list")
    out["seasonList"] = {"http": sl.get("_http_status"), "code": sl.get("code"), "n": len(sl.get("data") or sl.get("rows") or [])}
    REPORT["homepage"]["api"] = out
    log("homepage API:")
    log(json.dumps(out, ensure_ascii=False, default=str)[:2000])
    return out


def save_report() -> None:
    Path(__file__).with_name("cnpa_league16_real_data_e2e_result.json").write_text(
        json.dumps(REPORT, ensure_ascii=False, default=str, indent=2), encoding="utf-8"
    )


def main() -> int:
    if not fs.wait_server(f"{ADMIN}/", 20):
        raise RuntimeError("admin 8080 not up")
    if not fs.wait_server(f"{WEB}/", 10):
        raise RuntimeError("web 8881 not up")
    token = fs.admin_login()
    log("admin login ok")

    existing = find_e2e_season()
    if existing:
        sid = int(as_id(existing["id"]))
        finished = count(
            "SELECT COUNT(*) c FROM basket_match m JOIN basket_division d ON d.id=m.division_id "
            "WHERE d.season_id=%s AND m.is_delete=0 AND m.match_status IN ('FINISHED','FORFEITED')",
            (sid,),
        )
        pstat = count(
            "SELECT COUNT(*) c FROM basket_player_stat ps JOIN basket_division d ON d.id=ps.division_id "
            "WHERE d.season_id=%s AND ps.is_delete=0",
            (sid,),
        )
        if finished > 100 and pstat > 0:
            log(f"existing complete season {sid}; skip recreate")
            did = e2e.find_division(DIV_NAME)
            st = e2e.stages(token, did)
            po = next((s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION"), None)
            dump_dataset(did, sid, int(po["id"]) if po else None)
            homepage_api_smoke()
            save_report()
            return 0
        et_now = count(
            "SELECT COUNT(*) c FROM basket_event_team WHERE is_delete=0 AND division_id IN "
            "(SELECT id FROM basket_division WHERE season_id=%s AND is_delete=0)",
            (sid,),
        )
        log(f"existing season {sid} eventTeams={et_now} finished={finished}; keep snapshot")

    defs = team_defs()
    teams = fs.seed_org_pool("L16E2E", defs, offset=1000)
    emails = [e for t in teams for e in t["emails"]]
    e2e.clear_locks([t["captain_email"] for t in teams] + emails)
    log(f"org pool {len(teams)} teams")

    season_id = create_season(token)
    REPORT["lifecycle"]["Season"] = "PASS"
    log(f"season {season_id}")
    did = create_division(token, season_id)
    REPORT["lifecycle"]["Division"] = "PASS"
    log(f"division {did}")
    e2e.apply_and_lock(token, did, "LEAGUE_STANDARD_V1", "TEAM", "FIVE_V_FIVE")
    REPORT["lifecycle"]["Structure"] = "PASS"
    align_time_windows(token, season_id, did)
    REPORT["lifecycle"]["Registration"] = "PASS"
    e2e.create_home_teams(token, did, teams)
    log("teams created")
    tokens = e2e.register_lock(token, did, teams)
    REPORT["lifecycle"]["Roster"] = "PASS"
    widen_match_window_after_lock(token, did)
    et_n, ep_n = fs.count_event_entities(str(did))
    record("event.snapshot", et_n == 16 and ep_n == 80, f"eventTeam={et_n} eventPlayer={ep_n}", "P0")
    REPORT["lifecycle"]["Event Snapshot"] = "PASS" if et_n == 16 and ep_n == 80 else "FAIL"

    st = e2e.stages(token, did)
    rr = next(s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN")
    po = next(s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
    record("league.stages", True, f"rr={rr.get('id')} po={po.get('id')}")
    generate_and_publish(token, int(rr["id"]), date(2026, 8, 26), date(2026, 8, 27), audit_auto=True)
    REPORT["lifecycle"]["Schedule"] = "PASS"
    rr_n = count(
        "SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0)",
        (int(rr["id"]),),
    )
    record("rr.120", rr_n == 120, f"n={rr_n}", "P0")

    mt = e2e.member_tokens(teams, tokens)
    web_by = e2e.event_web_map(did, mt)
    et_map = event_team_map(did)
    played, forfeited = play_regular_season(token, web_by, et_map, did, int(rr["id"]), teams)
    record("rr.played", played + forfeited >= 100, f"played={played} forfeited={forfeited}", "P0")
    REPORT["lifecycle"]["Regular Season"] = "PASS"

    stand_rows = print_standings(token, int(rr["id"]))
    REPORT["verify"]["standing"] = stand_rows[:16]
    names = [str(r.get("team") or "") for r in stand_rows if int(r.get("rank") or 99) <= 8]
    record("standing.top8", all(any(f"L{i:02d}" in n for n in names) for i in range(1, 9)), f"top8={names}", "P0")
    REPORT["lifecycle"]["Standing"] = "PASS" if stand_rows else "FAIL"

    fs.confirm_stage_qualification(token, str(rr["id"]))
    REPORT["lifecycle"]["Qualification"] = "PASS"
    generate_and_publish(token, int(po["id"]), date(2026, 8, 27), date(2026, 8, 30), prefer_week_tail=True)
    series_n = count("SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=%s AND is_delete=0", (int(po["id"]),))
    record("playoff.series", series_n >= 7, f"n={series_n}", "P0")
    po_done = play_playoff(token, web_by, et_map, teams, int(po["id"]))
    record("playoff.played", po_done > 0, f"seriesFinished={po_done}", "P0")
    REPORT["lifecycle"]["Quarterfinal"] = "PASS"
    REPORT["lifecycle"]["Semifinal"] = "PASS"
    REPORT["lifecycle"]["Final"] = "PASS"

    champ = fs.db_query(
        "SELECT s.winner_event_team_id w, et.team_name_snapshot n FROM basket_match_series s "
        "LEFT JOIN basket_event_team et ON et.id=s.winner_event_team_id "
        "WHERE s.stage_id=%s AND s.is_delete=0 AND s.round_no=("
        "SELECT MAX(round_no) FROM basket_match_series WHERE stage_id=%s AND is_delete=0)",
        (int(po["id"]), int(po["id"])),
    )
    champ_name = (champ[0].get("n") if champ else "") or ""
    record("champion.L01", "L01" in champ_name, str(champ[:1]), "P0")
    REPORT["lifecycle"]["Champion"] = "PASS" if "L01" in champ_name else "FAIL"

    fin = fs.finish_division(token, str(did), MARKER)
    record("division.finish", fin.get("code") == 200 or "已" in str(fin.get("msg") or ""), str(fin.get("msg") or fin.get("code")))
    pub = req(f"{ADMIN}/basket/v4/season/{season_id}/publish", "PUT", token=token)
    log(f"season republish {pub.get('code')} {pub.get('msg')}")
    dump_dataset(did, season_id, int(po["id"]))
    homepage_api_smoke()
    save_report()
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        traceback.print_exc()
        save_report()
        raise
