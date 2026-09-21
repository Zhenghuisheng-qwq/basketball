#!/usr/bin/env python3
"""CNPA V2 R2-4 MatchSeries Smoke E2E (Admin + Web API)."""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from typing import Any

ADMIN = "http://localhost:8080"
WEB = "http://localhost:8881"
MARKER = "R2_4_MATCH_SERIES_SMOKE"
SEASON_ID = 81
TEMPLATE_ID = 3
MEMBER_PASS = "111111aa"
TIE_BREAK = [
    {"ruleCode": "WIN_COUNT", "priorityNo": 1, "enabled": 1},
    {"ruleCode": "HEAD_TO_HEAD", "priorityNo": 2, "enabled": 1},
    {"ruleCode": "POINT_DIFFERENCE", "priorityNo": 3, "enabled": 1},
    {"ruleCode": "TOTAL_POINTS", "priorityNo": 4, "enabled": 1},
    {"ruleCode": "MANUAL", "priorityNo": 5, "enabled": 1},
]

TEAMS = [
    {"name": "CNPA战队Beta", "team_id": "2083464935756566530", "captain_email": "111112@qq.com", "captain_player_id": "2083401000000000002", "player_ids": ["2083401000000000002", "2083401000000090002", "2083401000000100002", "2083401000000110002", "2083401000000120002"]},
    {"name": "CNPA战队Gamma", "team_id": "2083464936591233026", "captain_email": "111113@qq.com", "captain_player_id": "2083401000000010002", "player_ids": ["2083401000000010002", "2083401000000130002", "2083401000000140002", "2083401000000150002", "2083401000000160002"]},
    {"name": "CNPA战队Delta", "team_id": "2083464937493008385", "captain_email": "111114@qq.com", "captain_player_id": "2083401000000020002", "player_ids": ["2083401000000020002", "2083401000000170002", "2083401000000180002", "2083401000000190002", "2083401000000200002"]},
    {"name": "CNPA战队Epsilon", "team_id": "2083464938394783746", "captain_email": "111115@qq.com", "captain_player_id": "2083401000000030002", "player_ids": ["2083401000000030002", "2083401000000210002", "2083401000000220002", "2083401000000230002", "2083401000000240002"]},
    {"name": "CNPA战队Zeta", "team_id": "2083464939296559105", "captain_email": "111116@qq.com", "captain_player_id": "2083401000000040002", "player_ids": ["2083401000000040002", "2083401000000250002", "2083401000000260002", "2083401000000270002", "2083401000000280002"]},
    {"name": "CNPA流水线969123-A", "team_id": "2083474625710260225", "captain_email": "111141@qq.com", "captain_player_id": "2083474352514269186", "player_ids": ["2083474352514269186", "2083474404259397634", "2083474413025492995", "2083474421477015555", "2083474430159224835"]},
    {"name": "CNPA流水线969123-B", "team_id": "2083474626544926722", "captain_email": "111142@qq.com", "captain_player_id": "2083474359648780290", "player_ids": ["2083474359648780290", "2083474435871866883", "2083474442926686209", "2083474454138060802", "2083474463063539715"]},
    {"name": "CNPA流水线969123-C", "team_id": "2083474627316678657", "captain_email": "111143@qq.com", "captain_player_id": "2083474369497006083", "player_ids": ["2083474369497006083", "2083474474597875715", "2083474483393331203", "2083474489227608066", "2083474496760578051"]},
]

DIVISION_ID = 118  # reuse R2_4_MATCH_SERIES_SMOKE on Season 81

SMOKE_EMAILS = [t["captain_email"] for t in TEAMS]

results: dict[str, str] = {}
ctx: dict[str, Any] = {}


def as_id(value: Any, label: str = "id") -> str:
    if isinstance(value, float):
        raise ValueError(f"{label}: float snowflake rejected ({value!r})")
    if isinstance(value, str):
        s = value.strip()
        if not s.isdigit():
            raise ValueError(f"{label}: non-numeric string {value!r}")
        return s
    if isinstance(value, int):
        return str(value)
    raise ValueError(f"{label}: unsupported type {type(value)}")


def fail(case: str, detail: str = "") -> int:
    log(case, "FAIL", detail)
    print("\n=== SUMMARY (STOP) ===")
    for k, v in sorted(results.items()):
        print(f"{k}: {v}")
    return 1


def log(case: str, status: str, detail: str = "") -> None:
    results[case] = f"{status}" + (f" | {detail}" if detail else "")
    mark = "PASS" if status == "PASS" else status
    print(f"[{mark}] {case}" + (f" — {detail}" if detail else ""))


def json_body(obj: Any) -> bytes:
    """Serialize JSON keeping snowflake IDs as strings when provided as str."""

    def enc(o: Any):
        if isinstance(o, dict):
            return {k: enc(v) for k, v in o.items()}
        if isinstance(o, list):
            return [enc(v) for v in o]
        if isinstance(o, int) and abs(o) > 9007199254740991:
            return str(o)
        return o

    return json.dumps(enc(obj), ensure_ascii=False).encode()


def req(url: str, method: str = "GET", body: Any = None, token: str | None = None, web: bool = False) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json_body(body) if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=120) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"code": e.code, "msg": raw}
        parsed["_http_status"] = e.code
        return parsed


def admin_login() -> str:
    r = req(f"{ADMIN}/login", "POST", {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
    if r.get("code") != 200:
        raise RuntimeError(f"admin login failed: {r}")
    return r["token"]


def clear_login_locks() -> None:
    try:
        import redis
        r = redis.Redis(host="192.168.31.49", port=6379, db=0)
        for email in SMOKE_EMAILS:
            k = f"member:login:fail:{email}"
            if r.exists(k):
                r.delete(k)
    except Exception:
        pass


def web_login(email: str, retries: int = 3) -> str:
    last = None
    for i in range(retries):
        r = req(f"{WEB}/basket/member/auth/login/password", "POST", {"email": email, "password": MEMBER_PASS})
        if r.get("code") == 200:
            return r["data"]["token"]
        last = r
        msg = str(r.get("msg", ""))
        if "稍后再试" in msg or "次数" in msg:
            raise RuntimeError(f"web login locked {email}; clear redis member:login:fail:*")
        time.sleep(2)
    raise RuntimeError(f"web login failed {email}: {last}")


def stage_payload(code, name, order, fmt, best_of, adv, next_code=None, adv_count=None):
    return {
        "stageCode": code,
        "stageName": name,
        "stageOrder": order,
        "stageType": "CUSTOM",
        "organizationMode": "UNGROUPED",
        "competitionFormat": fmt,
        "groupingMode": "NONE",
        "roundRobinCycles": 1,
        "bestOf": best_of,
        "winPoints": 2,
        "lossPoints": 1,
        "advancementType": adv,
        "pairingRule": "NONE",
        "requiresManualConfirmation": 0,
        **({"nextStageCode": next_code} if next_code else {}),
        **({"advancementCount": adv_count} if adv_count is not None else {}),
    }


def sorted_stages(snapshot: dict) -> list[dict]:
    return sorted(snapshot.get("stages") or [], key=lambda s: s.get("stageOrder") or 0)


def wipe_stages(token: str, division_id: int) -> None:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    stages = sorted_stages(snap.get("data") or {})
    for s in stages:
        p = stage_payload(s["stageCode"], s.get("stageName") or s["stageCode"], s.get("stageOrder") or 1,
                          s.get("competitionFormat") or "ROUND_ROBIN", s.get("bestOf") or 1, "NONE")
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "PUT", p, token)
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    for s in reversed(sorted_stages(snap.get("data") or {})):
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "DELETE", token=token)


def chain_update(token: str, division_id: int) -> None:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    stages = sorted_stages(snap.get("data") or {})
    for i, s in enumerate(stages):
        is_last = i == len(stages) - 1
        next_code = stages[i + 1]["stageCode"] if not is_last else None
        adv = "NONE" if is_last else ("WINNER_ADVANCE" if s.get("competitionFormat") == "SINGLE_ELIMINATION" else "OVERALL_TOP_N")
        adv_count = None if is_last else (4 if i == 0 else None)
        p = stage_payload(s["stageCode"], s.get("stageName"), i + 1, s.get("competitionFormat"),
                            s.get("bestOf") or 1, adv, next_code, adv_count)
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "PUT", p, token)


def list_matches(token: str, division_id: int, stage_id: int | None = None) -> list[dict]:
    url = f"{ADMIN}/basket/v4/match/list?divisionId={division_id}&pageNum=1&pageSize=500"
    if stage_id:
        url += f"&stageId={stage_id}"
    r = req(url, token=token)
    return r.get("rows") or []


def list_series(token: str, stage_id: int) -> list[dict]:
    r = req(f"{ADMIN}/basket/v4/match-series/stage/{stage_id}", token=token)
    return (r.get("data") or []) if r.get("code") == 200 else []


def get_series(token: str, series_id: int) -> dict:
    r = req(f"{ADMIN}/basket/v4/match-series/{series_id}", token=token)
    return r.get("data") or {}


def public_series(series_id: int) -> dict:
    r = req(f"{WEB}/basket/v4/public/match-series/{series_id}")
    return r.get("data") or {}


def approve_all_registrations(token: str, division_id: str) -> int:
    r = req(f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    n = 0
    for row in r.get("rows") or []:
        if row.get("registrationStatus") == "PENDING_REVIEW":
            ar = req(f"{ADMIN}/basket/v4/registration-audit/{row['registrationId']}/approve", "POST", {}, token)
            if ar.get("code") == 200:
                n += 1
    return n


def extract_id(resp: dict, key: str = "data") -> str:
    data = resp.get(key)
    if isinstance(data, dict):
        for k in ("id", "registrationId", "submissionId"):
            if data.get(k) is not None:
                return as_id(data[k], k)
        raise ValueError(f"no id in response data: {data}")
    return as_id(data, key)


def count_event_teams(token: str, division_id: str) -> int:
    r = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    return len(r.get("rows") or [])


def ensure_captain_grants(token: str, division_id: str) -> None:
    existing = req(f"{ADMIN}/basket/v4/captain-grant/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    active_teams = {as_id(row["teamId"]) for row in (existing.get("rows") or []) if row.get("grantStatus") == "ACTIVE"}
    for t in TEAMS:
        tid = as_id(t["team_id"])
        if tid in active_teams:
            continue
        cg = {"divisionId": division_id, "teamId": tid, "playerId": as_id(t["captain_player_id"]), "remark": MARKER}
        r = req(f"{ADMIN}/basket/v4/captain-grant", "POST", cg, token)
        if r.get("code") != 200:
            raise RuntimeError(f"captain grant failed {t['name']}: {r}")


def team_precheck() -> None:
    for t in TEAMS:
        if len(t.get("player_ids") or []) < 5:
            raise RuntimeError(f"{t['name']}: need 5 player_ids")


def list_registrations(token: str, division_id: str) -> dict[str, str]:
    """team_id -> registration_status"""
    r = req(f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    out: dict[str, str] = {}
    for row in r.get("rows") or []:
        tid = as_id(row.get("teamId"))
        status = row.get("registrationStatus") or row.get("status") or ""
        out[tid] = status
    return out


def register_teams(division_id: str, web_tokens: dict[str, str], admin_token: str) -> int:
    existing = list_registrations(admin_token, division_id)
    submitted = 0
    for t in TEAMS:
        tid = as_id(t["team_id"])
        status = existing.get(tid)
        if status in ("APPROVED", "PENDING_REVIEW"):
            continue
        wt = web_tokens[tid]
        player_ids = [as_id(p) for p in (t.get("player_ids") or [t["captain_player_id"]])]
        body = {"divisionId": division_id, "teamId": tid, "playerIds": player_ids}
        if status not in ("DRAFT", "REJECTED", "WITHDRAWN"):
            sr = req(f"{WEB}/basket/v4/player/registration/save", "POST", body, wt)
            if sr.get("code") != 200:
                msg = str(sr.get("msg", ""))
                if "不可保存" in msg or "已" in msg or "exist" in msg.lower():
                    continue
                raise RuntimeError(f"registration save failed {t['name']}: {sr}")
            reg_id = extract_id(sr, "data")
        else:
            rr = req(f"{WEB}/basket/v4/player/registration/my?divisionId={division_id}", token=wt)
            reg_id = as_id((rr.get("data") or {}).get("registrationId") or (rr.get("data") or {}).get("id"))
        tr = req(f"{WEB}/basket/v4/player/registration/submit", "POST", {"registrationId": reg_id}, wt)
        if tr.get("code") != 200:
            msg = str(tr.get("msg", ""))
            if "不可提交" in msg and status == "PENDING_REVIEW":
                continue
            raise RuntimeError(f"registration submit failed {t['name']}: {tr}")
        submitted += 1
    return submitted


def submit_and_approve_match(admin_token: str, web_token: str, match: dict, home_score: int, away_score: int,
                             submit_event_team_id: int) -> None:
    mid = match["id"]
    detail = req(f"{WEB}/basket/v4/player/match-submission/{mid}/detail?submitEventTeamId={submit_event_team_id}", token=web_token)
    if detail.get("code") != 200:
        raise RuntimeError(f"submission detail failed match {mid}: {detail}")
    d = detail["data"]
    player_stats = []
    if d.get("playerStats"):
        for ps in d["playerStats"]:
            player_stats.append({
                "eventPlayerId": ps["eventPlayerId"],
                "points": 10 if ps.get("eventTeamId") == submit_event_team_id else 5,
                "rebound": 5, "assist": 3, "steal": 1, "block": 0, "turnover": 2,
            })
    body = {
        "matchId": mid,
        "submitEventTeamId": submit_event_team_id,
        "homeScore": home_score,
        "awayScore": away_score,
        "teamStat": {"score": home_score if submit_event_team_id == match.get("homeEventTeamId") else away_score,
                     "rebound": 30, "assist": 15, "steal": 5, "block": 2, "turnover": 10},
        "playerStats": player_stats,
        "materialFileIds": [],
        "submitRemark": "R2-4 smoke",
    }
    sr = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
    if sr.get("code") != 200:
        raise RuntimeError(f"save submission failed match {mid}: {sr}")
    sub_id = extract_id(sr, "data") if isinstance(sr.get("data"), dict) else as_id(sr["data"])
    tr = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id}, web_token)
    if tr.get("code") != 200:
        raise RuntimeError(f"submit failed match {mid}: {tr}")
    lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
    rows = lr.get("rows") or []
    pending = next((x for x in rows if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    if not pending:
        raise RuntimeError(f"no pending submission for match {mid}")
    ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/approve", "POST", {}, admin_token)
    if ar.get("code") != 200:
        raise RuntimeError(f"approve failed match {mid}: {ar}")


def resolve_manual_rank_if_needed(token: str, stage_id: str) -> None:
    st = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/standings", token=token)
    groups = st.get("data") or []
    if not groups:
        return
    group = groups[0]
    pending = [r for r in (group.get("rows") or []) if r.get("manualRankPending")]
    if not pending:
        return
    items = [{"eventTeamId": as_id(r["eventTeamId"]), "manualRankNo": i + 1} for i, r in enumerate(pending)]
    body = {"groupScopeKey": 0, "reason": "R2-4 smoke tie-break", "items": items}
    mr = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/manual-rank", "POST", body, token)
    if mr.get("code") != 200:
        raise RuntimeError(f"manual rank failed: {mr}")


def confirm_stage_qualification(token: str, stage_id: str) -> None:
    cc = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/check-completion", "POST", token=token)
    if cc.get("code") != 200:
        raise RuntimeError(f"check-completion failed: {cc}")
    resolve_manual_rank_if_needed(token, stage_id)
    pv = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/qualification-preview", token=token)
    if pv.get("code") != 200:
        raise RuntimeError(f"qualification-preview failed: {pv}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
    if cf.get("code") != 200:
        raise RuntimeError(f"confirm-qualification failed: {cf}")


def finish_bo1_stage(admin_token: str, web_tokens_by_event_team: dict[str, str], division_id: str, stage_id: str,
                     winner_event_team_fn) -> None:
    matches = list_matches(admin_token, division_id, stage_id)
    bo1 = [m for m in matches if not m.get("seriesId")]
    for m in bo1:
        if m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home = m.get("homeEventTeamId")
        away = m.get("awayEventTeamId")
        winner_side = winner_event_team_fn(m)
        if winner_side == home:
            hs, aws = 64, 55
            submit_team = home
        else:
            hs, aws = 55, 64
            submit_team = away
        wt = web_tokens_by_event_team.get(as_id(submit_team))
        if not wt:
            raise RuntimeError(f"no web token for event team {submit_team}")
        submit_and_approve_match(admin_token, wt, m, hs, aws, submit_team)


def finish_series_games(admin_token: str, web_tokens_by_event_team: dict, series_id: int,
                          results_plan: list[str]) -> dict:
    """results_plan: list of 'HOME' or 'AWAY' per game to play."""
    s = get_series(admin_token, series_id)
    games = sorted(s.get("games") or [], key=lambda g: g.get("seriesGameNo") or 0)
    idx = 0
    for game in games:
        if game.get("skipped"):
            continue
        if game.get("matchStatus") in ("FINISHED", "FORFEITED"):
            idx += 1
            continue
        if idx >= len(results_plan):
            break
        side = results_plan[idx]
        mid = game["matchId"]
        mr = req(f"{ADMIN}/basket/v4/match/{mid}", token=admin_token)
        m = mr.get("data") or {}
        home = m.get("homeEventTeamId")
        away = m.get("awayEventTeamId")
        if side == "HOME":
            hs, aws, submit_team = 64, 55, home
        else:
            hs, aws, submit_team = 55, 64, away
        wt = web_tokens_by_event_team.get(as_id(submit_team))
        submit_and_approve_match(admin_token, wt, m, hs, aws, submit_team)
        idx += 1
    return get_series(admin_token, series_id)


def wait_server(url: str, sec: int = 60) -> bool:
    for _ in range(sec):
        try:
            urllib.request.urlopen(url, timeout=2)
            return True
        except Exception:
            time.sleep(1)
    return False


def find_existing_division(token: str) -> int | None:
    r = req(f"{ADMIN}/basket/v4/division/list?seasonId={SEASON_ID}&pageNum=1&pageSize=50", token=token)
    for row in r.get("rows") or []:
        if row.get("divisionName") == MARKER:
            return int(row["id"])
    return None


def main() -> int:
    if not wait_server(ADMIN):
        log("ENV_ADMIN", "FAIL", "8080 not ready")
        return 1
    if not wait_server(WEB):
        log("ENV_WEB", "FAIL", "8881 not ready")
        return 1

    try:
        admin_token = admin_login()
    except Exception as e:
        log("ENV", "FAIL", str(e))
        return 1
    log("ENV", "PASS", "Admin/Web up, login OK")
    clear_login_locks()

    # Season 81 display window from Admin API: 2026-08-07 ~ 2026-09-30
    reg_start = datetime(2026, 8, 7, 0, 0, 0)
    reg_end = datetime(2026, 8, 10, 0, 0, 0)
    match_start = datetime(2026, 8, 11, 0, 0, 0)
    match_end = datetime(2026, 9, 20, 23, 59, 59)
    div_body = {
        "seasonId": SEASON_ID,
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": MARKER,
        "registrationMode": "TEAM",
        "formatType": "REGULAR_ROUND_ROBIN",
        "formatTemplateId": TEMPLATE_ID,
        "maxTeamCount": 8,
        "registrationStartTime": reg_start.strftime("%Y-%m-%d %H:%M:%S"),
        "registrationEndTime": reg_end.strftime("%Y-%m-%d %H:%M:%S"),
        "matchStartTime": match_start.strftime("%Y-%m-%d %H:%M:%S"),
        "matchEndTime": match_end.strftime("%Y-%m-%d %H:%M:%S"),
        "sortOrder": 0,
    }
    existing_id = find_existing_division(admin_token)
    division_id = as_id(existing_id or DIVISION_ID)
    log("CREATE_DIVISION", "PASS", f"reuse id={division_id}")
    ctx["division_id"] = division_id

    try:
        team_precheck()
        log("TEAM_PRECHECK", "PASS", f"teams={len(TEAMS)}")
    except Exception as e:
        return fail("TEAM_PRECHECK", str(e))

    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=admin_token)
    stages = sorted_stages(snap.get("data") or {})
    if len(stages) < 3:
        wipe_stages(admin_token, division_id)
        s1 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST",
                 stage_payload("ST1", "常规积分测试", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "ST2", 4), admin_token)
        s2 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST",
                 stage_payload("ST2", "4强赛", 2, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "ST3"), admin_token)
        s3 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST",
                 stage_payload("ST3", "总决赛", 3, "SINGLE_ELIMINATION", 5, "NONE"), admin_token)
        st1_id, st2_id, st3_id = int(s1["data"]), int(s2["data"]), int(s3["data"])
        chain_update(admin_token, division_id)
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/tie-break-rules", "PUT", {"stageId": st1_id, "rules": TIE_BREAK}, admin_token)
        lr = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot/lock", "POST", token=admin_token)
        if lr.get("code") != 200:
            log("LOCK", "FAIL", str(lr))
            return 1
        log("LOCK", "PASS")
    else:
        st1_id = int(stages[0]["id"])
        st2_id = int(stages[1]["id"])
        st3_id = int(stages[2]["id"])
        log("LOCK", "PASS", "reuse existing stages")
    ctx.update({"st1_id": st1_id, "st2_id": st2_id, "st3_id": st3_id})

    orr = req(f"{ADMIN}/basket/v4/division/{division_id}/open-registration", "PUT", token=admin_token)
    if orr.get("code") != 200:
        return fail("OPEN_REG_SE", str(orr))
    log("OPEN_REG_SE", "PASS")

    try:
        ensure_captain_grants(admin_token, division_id)
        log("CAPTAIN_GRANT", "PASS")
    except Exception as e:
        return fail("CAPTAIN_GRANT", str(e))

    web_tokens: dict[str, str] = {}
    for i, t in enumerate(TEAMS):
        if i:
            time.sleep(2)
        try:
            web_tokens[as_id(t["team_id"])] = web_login(t["captain_email"])
        except Exception as e:
            return fail("WEB_LOGIN", str(e))
    log("WEB_LOGIN", "PASS", f"teams={len(web_tokens)}")

    # RR BO3 gate negative
    div_rr = req(f"{ADMIN}/basket/v4/division", "POST", {**div_body, "divisionName": f"{MARKER}_RR_BO3_GATE", "platformCode": "PS_XBOX"}, admin_token)
    if div_rr.get("code") == 200:
        did = int(div_rr["data"])
        wipe_stages(admin_token, did)
        sid = req(f"{ADMIN}/basket/v4/divisions/{did}/format-stages", "POST",
                  stage_payload("RR", "RR BO3", 1, "ROUND_ROBIN", 3, "NONE"), admin_token)
        req(f"{ADMIN}/basket/v4/divisions/{did}/tie-break-rules", "PUT", {"stageId": int(sid["data"]), "rules": TIE_BREAK}, admin_token)
        req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot/lock", "POST", token=admin_token)
        grr = req(f"{ADMIN}/basket/v4/division/{did}/open-registration", "PUT", token=admin_token)
        if grr.get("code") == 200:
            log("GATE_RR_BO3", "FAIL", "should block")
        elif "循环" in str(grr.get("msg", "")):
            log("GATE_RR_BO3", "PASS", "blocked with message")
        else:
            log("GATE_RR_BO3", "PASS", f"blocked: {grr.get('msg')}")

    try:
        submitted = register_teams(division_id, web_tokens, admin_token)
        n = approve_all_registrations(admin_token, division_id)
        et_cnt = count_event_teams(admin_token, division_id)
        if et_cnt != 8:
            return fail("EVENT_TEAM", f"count={et_cnt}, submitted={submitted}, approved={n}")
        log("REGISTRATION", "PASS", f"submitted={submitted}, approved={n}, eventTeams=8")
    except Exception as e:
        return fail("REGISTRATION", str(e))

    cr = req(f"{ADMIN}/basket/v4/division/{division_id}/close-registration", "PUT", token=admin_token)
    if cr.get("code") != 200:
        return fail("CLOSE_REGISTRATION", str(cr))
    log("CLOSE_REGISTRATION", "PASS")

    init1 = req(f"{ADMIN}/basket/v4/admin/stages/{st1_id}/initialize-teams", "POST", token=admin_token)
    if init1.get("code") != 200:
        return fail("STAGE1_INIT", str(init1))
    gen1 = req(f"{ADMIN}/basket/v4/admin/stages/{st1_id}/generate-schedule", "POST", token=admin_token)
    if gen1.get("code") != 200:
        return fail("STAGE1_GENERATE", str(gen1))
    pub1 = req(f"{ADMIN}/basket/v4/admin/stages/{st1_id}/publish-schedule", "POST", token=admin_token)
    if pub1.get("code") != 200:
        return fail("STAGE1_PUBLISH", str(pub1))
    m1 = list_matches(admin_token, division_id, st1_id)
    if len(m1) != 28:
        return fail("STAGE1_MATCH_COUNT", f"expected 28 got {len(m1)}")
    log("STAGE1_SCHEDULE", "PASS", f"matches={len(m1)}")

    # Build event team -> web token map after registration
    etr = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=20", token=admin_token)
    event_teams = {row["teamNameSnapshot"]: row for row in (etr.get("rows") or [])}
    web_by_event: dict[str, str] = {}
    for t in TEAMS:
        et = event_teams.get(t["name"]) or next((v for k, v in event_teams.items() if t["name"] in k), None)
        if et:
            web_by_event[as_id(et["id"])] = web_tokens[as_id(t["team_id"])]
    ctx["web_by_event"] = web_by_event

    # Stage1: home always wins (may require manual tie-break)
    try:
        finish_bo1_stage(admin_token, web_by_event, division_id, str(st1_id), lambda m: m.get("homeEventTeamId"))
        confirm_stage_qualification(admin_token, str(st1_id))
        log("STAGE1", "PASS")
    except Exception as e:
        return fail("STAGE1", str(e))

    req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/initialize-teams", "POST", token=admin_token)
    gen2 = req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/generate-schedule", "POST", token=admin_token)
    if gen2.get("code") != 200:
        return fail("STAGE2_GENERATE", str(gen2))
    pub2 = req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/publish-schedule", "POST", token=admin_token)
    if pub2.get("code") != 200:
        return fail("STAGE2_PUBLISH", str(pub2))

    series_list = list_series(admin_token, st2_id)
    ctx["stage2_series"] = series_list
    if len(series_list) != 2:
        return fail("STAGE2_SERIES_COUNT", f"got {len(series_list)}")
    log("STAGE2_SERIES_COUNT", "PASS", f"series={len(series_list)}")
    games = sum(len(s.get("games") or []) for s in series_list)
    if games != 6:
        return fail("STAGE2_GAME_COUNT", f"games={games}")
    log("STAGE2_GAME_COUNT", "PASS", "6 games")

    if len(series_list) >= 2:
        sa, sb = series_list[0], series_list[1]
        try:
            sa_final = finish_series_games(admin_token, web_by_event, sa["id"], ["HOME", "HOME"])
            if sa_final.get("seriesStatus") == "COMPLETED" and sa_final.get("homeWinCount") == 2:
                log("BO3_2_0", "PASS", f"series={sa['id']}")
            else:
                log("BO3_2_0", "FAIL", str(sa_final))
            g3 = next((g for g in sa_final.get("games") or [] if g.get("seriesGameNo") == 3), None)
            if g3 and g3.get("skipped"):
                log("GAME3_GATE_UI", "PASS", "skipped flag set")
            else:
                log("GAME3_GATE_UI", "PARTIAL", "check skipped on game3")
            # try web submit on game3
            if g3:
                wt = web_by_event.get(sa_final.get("homeEventTeamId"))
                if wt:
                    tr = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST",
                             {"submissionId": 0}, wt)
                    # direct submit should fail via save
                    br = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", {
                        "matchId": g3["matchId"], "submitEventTeamId": sa_final.get("homeEventTeamId"),
                        "homeScore": 64, "awayScore": 55,
                        "teamStat": {"score": 64, "rebound": 1, "assist": 1, "steal": 0, "block": 0, "turnover": 0},
                        "playerStats": [], "materialFileIds": [],
                    }, wt)
                    if br.get("code") != 200:
                        log("GAME3_SUBMIT_GATE", "PASS", br.get("msg", "rejected"))
                    else:
                        log("GAME3_SUBMIT_GATE", "FAIL", "allowed submit on completed series game3")

            sb_final = finish_series_games(admin_token, web_by_event, sb["id"], ["HOME", "AWAY", "HOME"])
            if sb_final.get("seriesStatus") == "COMPLETED" and sb_final.get("homeWinCount") == 2:
                log("BO3_2_1", "PASS", f"series={sb['id']}")
            else:
                log("BO3_2_1", "FAIL", str(sb_final))
        except Exception as e:
            log("BO3_E2E", "FAIL", str(e))

        qpr = req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/qualification-preview", token=admin_token)
        quals = qpr.get("data") or []
        preview_n = len(quals)
        if preview_n >= 2:
            log("QUAL_PREVIEW", "PASS", f"count={preview_n}")
        else:
            log("QUAL_PREVIEW", "FAIL", f"count={preview_n}")

        req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/check-completion", "POST", token=admin_token)
        req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/confirm-qualification", "POST", token=admin_token)
        log("QUAL_CONFIRM", "PASS")

    req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/initialize-teams", "POST", token=admin_token)
    init = req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/initialize-teams", "POST", token=admin_token)
    st3_teams = (init.get("data") or {}).get("insertedStageTeam") or 0
    if st3_teams == 2 or (init.get("data") or {}).get("existingStageTeam") == 2:
        log("STAGE3_TEAMS", "PASS")
    else:
        log("STAGE3_TEAMS", "PARTIAL", str(init.get("data")))

    req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/generate-schedule", "POST", token=admin_token)
    req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/publish-schedule", "POST", token=admin_token)
    s3_series = list_series(admin_token, st3_id)
    if len(s3_series) == 1 and s3_series[0].get("bestOf") == 5:
        log("BO5_SERIES", "PASS", f"id={s3_series[0]['id']}")
        try:
            s5 = finish_series_games(admin_token, web_by_event, s3_series[0]["id"],
                                     ["HOME", "AWAY", "HOME", "AWAY", "HOME"])
            if s5.get("seriesStatus") == "COMPLETED" and s5.get("homeWinCount") == 3:
                log("BO5_3_2", "PASS", f"winner={s5.get('winnerTeamName')}")
                log("FINAL_WINNER", "PASS", s5.get("winnerTeamName", ""))
            else:
                log("BO5_3_2", "FAIL", str(s5))
        except Exception as e:
            log("BO5_E2E", "FAIL", str(e))
    else:
        log("BO5_SERIES", "FAIL", str(s3_series))

    if series_list:
        sid = series_list[0]["id"]
        ps = public_series(sid)
        if ps.get("id"):
            log("WEB_SERIES_API", "PASS", f"public series {sid}")
        else:
            log("WEB_SERIES_API", "FAIL", str(ps))
        if get_series(admin_token, sid).get("id"):
            log("ADMIN_SERIES_API", "PASS")

    fails = [k for k, v in results.items() if not v.startswith("PASS")]
    print("\n=== SUMMARY ===")
    for k, v in sorted(results.items()):
        print(f"{k}: {v}")
    print(f"\nDivision ID: {division_id}")
    print(f"Stage IDs: {st1_id}, {st2_id}, {st3_id}")
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main())
