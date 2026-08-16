#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V1.3 Match Submission local E2E — isolated basketball_r9_fresh only."""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
fs.MARKER = "E2E-SUBMISSION"
fs.ID_BASE = 2099111612550000000
fs.REDIS_HOST = "192.168.31.48"

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req
as_id = fs.as_id

PNG_A = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x08\x00\x00\x00\x08\x08\x02"
    b"\x00\x00\x00K6\xf6\x1a\x00\x00\x00\x1cIDATx\x9cc\xf8\xcf\xc0\xc0\xc0\xc0"
    b"\xc0\xc0\xff\xff\x8f\x82\x11\x0c\x0c\x0c\x00\x3f\x06\x03\x01\x7f\xa3\x02\x8c"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)
PNG_B = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x08\x00\x00\x00\x08\x08\x02"
    b"\x00\x00\x00K6\xf6\x1a\x00\x00\x00\x1cIDATx\x9cc\x18\x05\x83\x18\x18\x18"
    b"\xf8\xff\xff\xa3`\x04\x03\x03\x03\x00\x3f\x06\x03\x01\x7f\xa3\x02\x8c"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)
PNG_C = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x08\x00\x00\x00\x08\x08\x02"
    b"\x00\x00\x00K6\xf6\x1a\x00\x00\x00\x18IDATx\x9cc\xf8\x0f\x04\x0c\x0c\x0c"
    b"\xff\x51\x30\x82\x81\x81\x01\x00\x1e\x03\x02\x01\xdc\xcc\xae\xd2"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "env": {
        "web": WEB,
        "admin": ADMIN,
        "webUi": "http://127.0.0.1:5173",
        "adminUi": "http://127.0.0.1:5174",
        "db": "basketball_r9_fresh@192.168.31.48:3306",
        "adminUser": "admin",
    },
    "cases": {},
    "bugs": {"P0": [], "P1": [], "P2": []},
    "ids": {},
    "browser": {},
}


def log(msg: str) -> None:
    print(msg, flush=True)


def record(name: str, ok: bool, detail: str = "", sev: str = "P1") -> bool:
    REPORT["cases"][name] = {"ok": ok, "detail": str(detail)[:800]}
    mark = "PASS" if ok else "FAIL"
    log(f"[{mark}] {name} {str(detail)[:220]}")
    if not ok:
        REPORT["bugs"][sev].append(f"{name}: {str(detail)[:400]}")
    return ok


def p2(name: str, detail: str) -> None:
    REPORT["bugs"]["P2"].append(f"{name}: {detail}")
    REPORT["cases"][name] = {"ok": True, "detail": f"P2 {detail}"[:800]}
    log(f"[P2] {name} {detail[:220]}")


def need(resp: dict, name: str) -> dict:
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def http_raw(url: str, token: str | None = None, method: str = "GET") -> tuple[int, bytes, str]:
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=30) as resp:
            return getattr(resp, "status", 200), resp.read(), resp.headers.get("Content-Type") or ""
    except urllib.error.HTTPError as e:
        return e.code, e.read(), e.headers.get("Content-Type") or ""


def captain_email(event_team_id: int) -> str:
    rows = fs.db_query(
        "SELECT ma.email FROM basket_event_team et "
        "JOIN basket_member_account ma ON ma.id=et.captain_member_id "
        "WHERE et.id=%s",
        (event_team_id,),
    )
    if not rows or not rows[0].get("email"):
        raise RuntimeError(f"no captain email for event team {event_team_id}")
    return rows[0]["email"]


def login_team(event_team_id: int) -> str:
    email = captain_email(event_team_id)
    fs.clear_login_locks([email])
    token = fs.web_login(email)
    REPORT["ids"].setdefault("accounts", {})[str(event_team_id)] = email
    return token


def schedule_items(resp: dict) -> list[dict]:
    data = resp.get("data")
    if isinstance(data, list):
        return [x for x in data if isinstance(x, dict)]
    if isinstance(data, dict):
        for key in ("items", "rows", "records", "list"):
            val = data.get(key)
            if isinstance(val, list):
                return [x for x in val if isinstance(x, dict)]
    return []


def eligible(token: str, match_id: int, team_id: int) -> list[dict]:
    r = req(f"{WEB}/basket/v4/player/match-submission/{match_id}/eligible-players?submitEventTeamId={team_id}", token=token)
    if r.get("code") == 200 and isinstance(r.get("data"), list):
        return r.get("data") or []
    d = detail(token, match_id, team_id)
    players = d.get("eligiblePlayers") or []
    if players:
        return players
    raise RuntimeError(f"eligible failed: {r}")


def detail(token: str, match_id: int, team_id: int) -> dict:
    r = need(
        req(f"{WEB}/basket/v4/player/match-submission/{match_id}/detail?submitEventTeamId={team_id}", token=token),
        "detail",
    )
    return r.get("data") or {}


def player_row(epid, pts=20, reb=10, ast=5, stl=2, blk=1, fls=3, to=2) -> dict:
    return {
        "eventPlayerId": epid,
        "points": pts,
        "rebound": reb,
        "assist": ast,
        "steal": stl,
        "block": blk,
        "turnover": to,
        "foul": fls,
    }


def team_stat(score: int) -> dict:
    return {
        "score": score,
        "rebound": 36,
        "assist": 18,
        "steal": 7,
        "block": 4,
        "turnover": 11,
        "foul": 16,
        "fieldGoalMade": 30,
        "fieldGoalAttempt": 62,
        "threePointMade": 8,
        "threePointAttempt": 22,
        "freeThrowMade": 12,
        "freeThrowAttempt": 16,
    }


def save_draft(token: str, match_id: int, team_id: int, hs: int, aws: int, players: list, files: list, remark: str) -> dict:
    body = {
        "matchId": match_id,
        "submitEventTeamId": team_id,
        "homeScore": hs,
        "awayScore": aws,
        "teamStat": team_stat(hs if True else aws),
        "playerStats": players,
        "materialFileIds": files,
        "submitRemark": remark,
    }
    return req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, token)


def upload_shot(token: str, png: bytes, name: str) -> str:
    r = fs.upload_file(token, png, name, "MATCH_DATA_SCREENSHOT")
    if r.get("code") != 200:
        raise RuntimeError(f"upload {name}: {r}")
    data = r.get("data") or {}
    fid = data.get("fileId") or data.get("id")
    if not fid:
        raise RuntimeError(f"upload no fileId: {r}")
    return str(fid)


def submit_id(token: str, submission_id) -> dict:
    return req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": submission_id}, token)


def audit_detail(admin_token: str, submission_id) -> dict:
    r = need(req(f"{ADMIN}/basket/v4/match-submission-audit/{submission_id}", token=admin_token), "audit-detail")
    return r.get("data") or {}


def approve(admin_token: str, submission_id) -> dict:
    return req(f"{ADMIN}/basket/v4/match-submission-audit/{submission_id}/approve", "POST", {}, admin_token)


def reject(admin_token: str, submission_id, reason: str) -> dict:
    return req(
        f"{ADMIN}/basket/v4/match-submission-audit/{submission_id}/reject",
        "POST",
        {"rejectReason": reason, "auditRemark": reason},
        admin_token,
    )


def match_row(match_id: int) -> dict:
    rows = fs.db_query(
        "SELECT id, series_id, series_game_no, match_status, publish_status, home_score, away_score, "
        "winner_event_team_id, loser_event_team_id, home_event_team_id, away_event_team_id, "
        "season_id, division_id FROM basket_match WHERE id=%s",
        (match_id,),
    )
    return rows[0] if rows else {}


def series_row(series_id: int) -> dict:
    rows = fs.db_query(
        "SELECT id, series_code, best_of, series_status, home_win_count, away_win_count, "
        "winner_event_team_id, home_event_team_id, away_event_team_id, division_id, stage_id "
        "FROM basket_match_series WHERE id=%s",
        (series_id,),
    )
    return rows[0] if rows else {}


def submission_rows(match_id: int, team_id: int | None = None) -> list[dict]:
    sql = (
        "SELECT id, match_id, submit_event_team_id, submission_status, version_no, "
        "home_score, away_score, submit_remark, reject_reason "
        "FROM basket_match_submission WHERE match_id=%s AND is_delete=0"
    )
    args: list = [match_id]
    if team_id is not None:
        sql += " AND submit_event_team_id=%s"
        args.append(team_id)
    sql += " ORDER BY version_no"
    return fs.db_query(sql, tuple(args))


def assert_audit_view(admin_token: str, sid, expect_series: bool) -> dict:
    d = audit_detail(admin_token, sid)
    ok_basic = all([
        d.get("homeTeamName"),
        d.get("awayTeamName"),
        d.get("submitTeamName"),
        d.get("homeScore") is not None,
        d.get("awayScore") is not None,
    ])
    players = d.get("playerStats") or []
    player_ok = bool(players) and all(
        p.get("playerName") and p.get("points") is not None and p.get("rebound") is not None
        and p.get("assist") is not None and p.get("steal") is not None and p.get("block") is not None
        and p.get("foul") is not None and p.get("turnover") is not None
        for p in players
    )
    team = d.get("teamStat") or {}
    team_ok = all(team.get(k) is not None for k in ("score", "rebound", "assist", "steal", "block", "turnover", "foul"))
    materials = d.get("materials") or []
    file_only = any(not (m.get("accessUrl") or m.get("fileId")) for m in materials) if materials else True
    shot_ok = bool(materials) and all(m.get("fileId") and m.get("accessUrl") for m in materials)
    ctx = d.get("seriesContext")
    if expect_series:
        ctx_ok = bool(ctx) and ctx.get("bestOf") and (ctx.get("seriesCode") or ctx.get("matchSeriesId"))
    else:
        ctx_ok = ctx in (None, {}, [])
    record("admin.audit.table", ok_basic and player_ok and team_ok, f"sid={sid} players={len(players)} team={bool(team)}")
    record("admin.audit.screenshot.meta", shot_ok, f"materials={len(materials)} fileOnly={file_only}")
    if expect_series:
        games = (ctx or {}).get("games") or []
        game_no = None
        for g in games:
            if str(g.get("matchId")) == str(d.get("matchId")):
                game_no = g.get("seriesGameNo")
        record(
            "admin.audit.seriesContext",
            bool(ctx_ok and game_no),
            f"code={ctx.get('seriesCode') if ctx else None} bo={ctx.get('bestOf') if ctx else None} game={game_no} score={ctx.get('homeWinCount') if ctx else None}:{ctx.get('awayWinCount') if ctx else None}",
        )
    return d


def file_preview_ok(token: str, file_id: str) -> tuple[bool, str]:
    status, body, ctype = http_raw(f"{WEB}/basket/account/file/{file_id}", token)
    ok = status == 200 and (ctype.startswith("image/") or body[:8] == b"\x89PNG\r\n\x1a\n" or len(body) > 20)
    return ok, f"status={status} ctype={ctype} bytes={len(body)}"


def run_bo1(admin_token: str) -> None:
    match_id = 328
    home, away = 79, 78
    m0 = match_row(match_id)
    record("bo1.fixture", m0.get("series_id") in (None, 0) and m0.get("publish_status") == "PUBLISHED" and m0.get("match_status") not in ("FINISHED", "CANCELLED"), str(m0), "P0")
    REPORT["ids"]["bo1"] = {"matchId": match_id, "homeEventTeamId": home, "awayEventTeamId": away, "season": "E2E-LEAGUE-DRR-S"}

    token = login_team(home)
    other = login_team(away)
    d = detail(token, match_id, home)
    record(
        "bo1.entry.direct",
        d.get("matchId") in (match_id, str(match_id)) and d.get("seriesContext") in (None, {}),
        f"route=/account/my-events/v4/match-submission/{match_id} seriesContext={d.get('seriesContext')}",
    )
    if d.get("seriesContext"):
        REPORT["bugs"]["P1"].append("BO1 详情出现了 Series Context")
    else:
        p2("bo1.context.label", "BO1 不展示 Series Score/Game X；页面未显式写 BO1/单场，靠缺省 seriesContext 表达")

    record(
        "bo1.page.context",
        bool(d.get("seasonTitle") and d.get("divisionName") and d.get("homeTeamName") and d.get("awayTeamName")),
        f"{d.get('seasonTitle')} / {d.get('divisionName')} / {d.get('homeTeamName')} vs {d.get('awayTeamName')} time={d.get('matchTime')}",
    )
    record(
        "bo1.lineup.notHardcoded5",
        (d.get("lineupMinCount") or 0) > 0 and d.get("gameMode") == "FIVE_V_FIVE",
        f"gameMode={d.get('gameMode')} min={d.get('lineupMinCount')} max={d.get('lineupMaxCount')}",
    )

    mine = eligible(token, match_id, home)
    opp = fs.db_query(
        "SELECT id FROM basket_event_player WHERE event_team_id=%s AND is_delete=0",
        (away,),
    )
    mine_ids = {str(p.get("eventPlayerId")) for p in mine}
    opp_ids = {str(r["id"]) for r in opp}
    record("playerSelector.ownTeamOnly", bool(mine) and mine_ids.isdisjoint(opp_ids), f"mine={len(mine)} overlap={len(mine_ids & opp_ids)}")
    record("playerSelector.5v5.rows", len(mine) >= (d.get("lineupMinCount") or 5), f"eligible={len(mine)}")

    if len(mine) < 1:
        raise RuntimeError("no eligible players for BO1")
    p_a = mine[0]["eventPlayerId"]
    extra = [p_a]
    if len(mine) > 1:
        extra.append(mine[1]["eventPlayerId"])
    else:
        extra.append(p_a)
    dup = save_draft(token, match_id, home, 80, 75, [player_row(p_a), player_row(p_a, pts=10)], [], "dup")
    record("playerSelector.dedup.api", dup.get("code") != 200 and "重复" in str(dup.get("msg") or ""), str(dup.get("msg")), "P1")

    if opp:
        bad = save_draft(token, match_id, home, 80, 75, [player_row(opp[0]["id"])], [], "opp")
        record("acl.opponent.player", bad.get("code") != 200, str(bad.get("msg")), "P0")

    f1 = upload_shot(token, PNG_A, "bo1-a.png")
    f2 = upload_shot(token, PNG_B, "bo1-b.png")
    players = [player_row(p_a, 20, 10, 5, 2, 1, 3, 2)]
    if len(mine) > 1:
        players.append(player_row(mine[1]["eventPlayerId"], 12, 4, 3, 1, 0, 2, 1))
    sv = save_draft(token, match_id, home, 80, 75, players, [int(f1), int(f2)], "E2E BO1 draft")
    record("bo1.draft.save", sv.get("code") == 200, str(sv.get("msg") or sv.get("data")))
    sid = fs.extract_id(sv)

    restored = detail(token, match_id, home)
    ps0 = (restored.get("playerStats") or [{}])[0]
    ts = restored.get("teamStat") or {}
    record(
        "bo1.draft.restore",
        restored.get("submissionStatus") == "DRAFT"
        and restored.get("homeScore") == 80
        and restored.get("awayScore") == 75
        and int(ps0.get("points") or 0) == 20
        and int(ps0.get("rebound") or 0) == 10
        and int(ts.get("rebound") or 0) == 36
        and int(ts.get("fieldGoalMade") or 0) == 30
        and restored.get("submitRemark") == "E2E BO1 draft"
        and {str(x) for x in (restored.get("materialFileIds") or [])} == {f1, f2},
        f"status={restored.get('submissionStatus')} score={restored.get('homeScore')}:{restored.get('awayScore')} pts={ps0.get('points')} files={restored.get('materialFileIds')}",
    )
    ok1, ev1 = file_preview_ok(token, f1)
    ok2, ev2 = file_preview_ok(token, f2)
    record("screenshot.draft.reload", ok1 and ok2, f"{ev1}; {ev2}", "P1")

    other_status, _, _ = http_raw(f"{WEB}/basket/account/file/{f1}", other)
    record("screenshot.acl.otherCaptain", other_status in (401, 403), f"other GET file={other_status}", "P0")

    mats = fs.db_query(
        "SELECT id, submission_id, match_id, file_id FROM basket_match_submission_material "
        "WHERE submission_id=%s AND is_delete=0",
        (int(sid),),
    )
    record(
        "screenshot.db.matchBind",
        len(mats) == 2 and all(int(x["match_id"]) == match_id and int(x["submission_id"]) == int(sid) for x in mats),
        f"n={len(mats)} {mats}",
        "P0",
    )

    db_ps = fs.db_query(
        "SELECT event_player_id, points, rebound, assist, steal, `block`, foul, turnover "
        "FROM basket_match_submission_player_stat WHERE submission_id=%s AND is_delete=0",
        (int(sid),),
    )
    record("stats.player.db", any(int(r.get("points") or 0) == 20 and int(r.get("rebound") or 0) == 10 for r in db_ps), str(db_ps))
    db_ts = fs.db_query(
        "SELECT score, rebound, assist, field_goal_made, three_point_made, free_throw_made "
        "FROM basket_match_submission_team_stat WHERE submission_id=%s AND is_delete=0",
        (int(sid),),
    )
    record("stats.team.db", bool(db_ts) and int(db_ts[0].get("field_goal_made") or 0) == 30, str(db_ts))
    record("stats.score.manual", restored.get("homeScore") == 80 and restored.get("awayScore") == 75, "home/away not overwritten by PTS sum")

    tr = submit_id(token, sid)
    record("bo1.submit", tr.get("code") == 200, str(tr.get("msg")))
    after = submission_rows(match_id, home)[-1]
    m1 = match_row(match_id)
    record("bo1.submit.status", after.get("submission_status") == "PENDING_REVIEW", f"status={after.get('submission_status')} (业务码=PENDING_REVIEW，不是 SUBMITTED)")
    record("bo1.submit.notFinished", m1.get("match_status") != "FINISHED", f"match={m1.get('match_status')}", "P0")

    ad = assert_audit_view(admin_token, sid, expect_series=False)
    record("admin.audit.remark", (ad.get("submitRemark") or "") == "E2E BO1 draft", ad.get("submitRemark"))
    st, body, ctype = http_raw(f"{ADMIN}/basket/v4/match-submission-audit/material/{f1}", admin_token)
    record("admin.audit.screenshot.stream", st == 200 and (ctype.startswith("image/") or body[:8] == b"\x89PNG\r\n\x1a\n"), f"status={st} ctype={ctype} bytes={len(body)}", "P1")

    rj = reject(admin_token, sid, "E2E 测试驳回")
    record("bo1.reject", rj.get("code") == 200, str(rj.get("msg")))
    rejected = detail(token, match_id, home)
    record(
        "bo1.reject.restore",
        rejected.get("submissionStatus") == "REJECTED"
        and rejected.get("rejectReason") == "E2E 测试驳回"
        and rejected.get("homeScore") == 80
        and {str(x) for x in (rejected.get("materialFileIds") or [])} == {f1, f2},
        f"reason={rejected.get('rejectReason')} files={rejected.get('materialFileIds')}",
        "P1",
    )
    ok_rej, ev_rej = file_preview_ok(token, f1)
    record("screenshot.rejected.reload", ok_rej, ev_rej, "P1")

    f3 = upload_shot(token, PNG_C, "bo1-c.png")
    players2 = [player_row(p_a, 22, 10, 5, 2, 1, 3, 2)]
    if len(mine) > 1:
        players2.append(player_row(mine[1]["eventPlayerId"], 12, 4, 3, 1, 0, 2, 1))
    rs = req(
        f"{WEB}/basket/v4/player/match-submission/resubmit",
        "POST",
        {
            "oldSubmissionId": sid,
            "homeScore": 80,
            "awayScore": 75,
            "teamStat": team_stat(80),
            "playerStats": players2,
            "materialFileIds": [int(f2), int(f3)],
            "submitRemark": "E2E BO1 resubmit",
        },
        token,
    )
    record("bo1.resubmit.api", rs.get("code") == 200, str(rs.get("msg") or rs.get("data")))
    sid2 = fs.extract_id(rs)
    versions = submission_rows(match_id, home)
    v1 = next((x for x in versions if int(x["id"]) == int(sid)), None)
    v2 = next((x for x in versions if int(x["id"]) == int(sid2)), None)
    record(
        "submission.version",
        v1 and v2 and int(v1["version_no"]) == 1 and int(v2["version_no"]) == 2 and v1["submission_status"] == "REJECTED",
        f"v1={v1} v2={v2}",
        "P1",
    )
    v1_files = {str(x["file_id"]) for x in fs.db_query(
        "SELECT file_id FROM basket_match_submission_material WHERE submission_id=%s AND is_delete=0", (int(sid),)
    )}
    v2_files = {str(x["file_id"]) for x in fs.db_query(
        "SELECT file_id FROM basket_match_submission_material WHERE submission_id=%s AND is_delete=0", (int(sid2),)
    )}
    record("screenshot.version.isolation", v1_files == {f1, f2} and v2_files == {f2, f3}, f"v1={v1_files} v2={v2_files}", "P0")

    tr2 = submit_id(token, sid2)
    record("bo1.resubmit.submit", tr2.get("code") == 200, str(tr2.get("msg")))
    ap = approve(admin_token, sid2)
    record("bo1.approve", ap.get("code") == 200, str(ap.get("msg")))
    settled = match_row(match_id)
    record(
        "bo1.settlement",
        settled.get("match_status") == "FINISHED"
        and int(settled.get("home_score") or -1) == 80
        and int(settled.get("away_score") or -1) == 75
        and settled.get("winner_event_team_id") not in (None, 0)
        and int(settled.get("winner_event_team_id")) == home,
        str(settled),
        "P0",
    )
    official = fs.db_query(
        "SELECT event_player_id, event_team_id, points, rebound, assist, steal, `block`, foul, turnover "
        "FROM basket_match_player_stat WHERE match_id=%s AND is_delete=0",
        (match_id,),
    )
    record(
        "bo1.official.playerStat",
        official and all(int(r["event_team_id"]) == home for r in official) and any(int(r.get("points") or 0) == 22 for r in official),
        str(official),
    )
    REPORT["ids"]["bo1"].update({"v1": int(sid), "v2": int(sid2), "files": [f1, f2, f3]})


def play_one_game(admin_token: str, token: str, match_id: int, team_id: int, hs: int, aws: int, remark: str, png: bytes, expect_series_score=None) -> dict:
    players_src = eligible(token, match_id, team_id)
    if not players_src:
        raise RuntimeError(f"no eligible players match={match_id} team={team_id}")
    players = [player_row(players_src[0]["eventPlayerId"], 18, 8, 4, 2, 1, 2, 2)]
    if len(players_src) > 1:
        players.append(player_row(players_src[1]["eventPlayerId"], 10, 5, 2, 1, 0, 1, 1))
    fid = upload_shot(token, png, f"g-{match_id}.png")
    sv = save_draft(token, match_id, team_id, hs, aws, players, [int(fid)], remark)
    if sv.get("code") != 200:
        raise RuntimeError(f"save game {match_id}: {sv}")
    sid = fs.extract_id(sv)
    tr = submit_id(token, sid)
    if tr.get("code") != 200:
        raise RuntimeError(f"submit game {match_id}: {tr}")
    m_before = match_row(match_id)
    if m_before.get("match_status") == "FINISHED":
        raise RuntimeError(f"match {match_id} finished before approve")
    ap = approve(admin_token, sid)
    if ap.get("code") != 200:
        raise RuntimeError(f"approve game {match_id}: {ap}")
    return {"submissionId": int(sid), "fileId": fid, "match": match_row(match_id)}


def run_bo3(admin_token: str) -> None:
    series_id = 82
    s0 = series_row(series_id)
    games = fs.db_query(
        "SELECT id, series_game_no, match_status, home_event_team_id, away_event_team_id "
        "FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no",
        (series_id,),
    )
    record("bo3.fixture", s0.get("best_of") == 3 and len(games) == 3 and s0.get("series_status") not in ("COMPLETED", "CANCELLED"), f"{s0} games={games}", "P0")
    g1, g2, g3 = games[0], games[1], games[2]
    home, away = int(s0["home_event_team_id"]), int(s0["away_event_team_id"])
    REPORT["ids"]["bo3"] = {"seriesId": series_id, "seriesCode": s0.get("series_code"), "games": [int(g["id"]) for g in games], "home": home, "away": away}

    token = login_team(home)
    sched = need(req(f"{WEB}/basket/v4/player/my-schedule?divisionId={s0['division_id']}&includeFinished=true", token=token), "my-schedule")
    rows = schedule_items(sched)
    series_rows = [r for r in rows if str(r.get("matchSeriesId") or r.get("seriesId") or "") == str(series_id)]
    record(
        "bo3.schedule.card",
        len(series_rows) >= 3 and any((r.get("bestOf") or r.get("seriesBestOf")) in (3, "3") for r in series_rows),
        f"rows={len(series_rows)} sample={series_rows[0] if series_rows else None}",
    )

    d1 = detail(token, int(g1["id"]), home)
    ctx = d1.get("seriesContext") or {}
    record(
        "bo3.game1.context",
        ctx.get("bestOf") == 3 and int(ctx.get("seriesGameNo") or 0) == 1 and int(ctx.get("homeWinCount") or 0) == 0 and int(ctx.get("awayWinCount") or 0) == 0,
        str(ctx),
        "P1",
    )

    r1 = play_one_game(admin_token, token, int(g1["id"]), home, 78, 70, "E2E BO3 G1", PNG_A)
    s1 = series_row(series_id)
    record("bo3.game1.finished", r1["match"].get("match_status") == "FINISHED", str(r1["match"]))
    record("bo3.score.1-0", int(s1.get("home_win_count") or 0) == 1 and int(s1.get("away_win_count") or 0) == 0 and s1.get("series_status") != "COMPLETED", str(s1), "P0")
    record("bo3.game1.noAdvanceComplete", s1.get("winner_event_team_id") in (None, 0), f"winner={s1.get('winner_event_team_id')}", "P0")
    assert_audit_view(admin_token, r1["submissionId"], expect_series=True)

    r2 = play_one_game(admin_token, token, int(g2["id"]), home, 81, 74, "E2E BO3 G2", PNG_B)
    s2 = series_row(series_id)
    record("bo3.score.2-0.completed", int(s2.get("home_win_count") or 0) == 2 and s2.get("series_status") == "COMPLETED" and int(s2.get("winner_event_team_id") or 0) == home, str(s2), "P0")

    next_series = fs.db_query(
        "SELECT id, home_event_team_id, away_event_team_id, series_status FROM basket_match_series "
        "WHERE stage_id=%s AND is_delete=0 AND (home_event_team_id=%s OR away_event_team_id=%s) AND id<>%s",
        (s2.get("stage_id"), home, home, series_id),
    )
    record("bo3.winnerAdvance", any(int(x.get("home_event_team_id") or 0) == home or int(x.get("away_event_team_id") or 0) == home for x in next_series), str(next_series), "P0")

    d3 = detail(token, int(g3["id"]), home)
    ctx3 = d3.get("seriesContext") or {}
    record("bo3.game3.skipped.detail", bool(ctx3.get("skipped")), str(ctx3), "P1")
    sched2 = need(req(f"{WEB}/basket/v4/player/my-schedule?divisionId={s0['division_id']}&includeFinished=true", token=token), "my-schedule-2")
    rows2 = schedule_items(sched2)
    g3_row = next((r for r in rows2 if str(r.get("matchId")) == str(g3["id"])), None)
    skipped_ui = bool(g3_row) and (
        g3_row.get("userActionType") == "NONE"
        or "无需进行" in str(g3_row.get("cannotSubmitReason") or "")
        or "无需进行" in str(g3_row.get("userActionText") or "")
    )
    record("bo3.game3.skipped.schedule", skipped_ui, str(g3_row), "P1")

    before_n = len(submission_rows(int(g3["id"]), home))
    blocked = save_draft(token, int(g3["id"]), home, 1, 0, [player_row(eligible(token, int(g3["id"]), home)[0]["eventPlayerId"])], [], "should fail")
    after_n = len(submission_rows(int(g3["id"]), home))
    record("bo3.game3.rejectNewSubmission", blocked.get("code") != 200 and after_n == before_n, f"save={blocked.get('msg')} n={before_n}->{after_n}", "P1")

    g1_files = fs.db_query(
        "SELECT file_id, match_id, submission_id FROM basket_match_submission_material "
        "WHERE match_id=%s AND is_delete=0",
        (int(g1["id"]),),
    )
    g2_files = fs.db_query(
        "SELECT file_id, match_id, submission_id FROM basket_match_submission_material "
        "WHERE match_id=%s AND is_delete=0",
        (int(g2["id"]),),
    )
    record(
        "screenshot.gameIsolation",
        g1_files and g2_files and {str(x["file_id"]) for x in g1_files}.isdisjoint({str(x["file_id"]) for x in g2_files}),
        f"g1={g1_files} g2={g2_files}",
        "P0",
    )
    REPORT["ids"]["bo3"].update({"g1": r1, "g2": r2})


def setup_bo5(admin_token: str) -> dict:
    existing = fs.db_query(
        "SELECT id FROM basket_season WHERE season_name='E2E-SUBMISSION-BO5' AND is_delete=0 ORDER BY id DESC LIMIT 1"
    )
    fs.SERIES_ID = 2
    if existing:
        season_id = str(existing[0]["id"])
    else:
        season_id = fs.create_season(admin_token, "E2E-SUBMISSION-BO5", "E2E-SUBMISSION-BO5 2026")
    dl = req(f"{ADMIN}/basket/v4/division/list?seasonId={season_id}&pageNum=1&pageSize=20", token=admin_token)
    row = next((r for r in (dl.get("rows") or []) if r.get("divisionName") == "E2E-SUBMISSION-BO5"), None)
    if row:
        did = int(as_id(row["id"]))
    else:
        body = {
            "seasonId": int(season_id),
            "platformCode": "PC",
            "gameMode": "FIVE_V_FIVE",
            "divisionName": "E2E-SUBMISSION-BO5",
            "registrationMode": "TEAM",
            "maxTeamCount": 2,
            "maxPersonCount": 16,
            "registrationStartTime": "2026-08-01 00:00:00",
            "registrationEndTime": "2026-08-20 23:59:59",
            "matchStartTime": "2026-08-21 00:00:00",
            "matchEndTime": "2026-12-20 23:59:59",
        }
        cr = need(req(f"{ADMIN}/basket/v4/division", "POST", body, admin_token), "bo5-div")
        did = int(fs.extract_id(cr))
        snap = req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot", token=admin_token)
        data = snap.get("data") or {}
        status = data.get("snapshotStatus") or data.get("status")
        if status not in ("LOCKED", "PUBLISHED") and data.get("locked") is not True:
            need(req(f"{ADMIN}/basket/v4/divisions/{did}/standard-template/apply", "POST",
                     {"templateCode": "ARENA_STANDARD_V1", "registrationMode": "TEAM", "gameMode": "FIVE_V_FIVE"}, admin_token), "bo5-apply")
            need(req(f"{ADMIN}/basket/v4/divisions/{did}/format-snapshot/lock", "POST", token=admin_token), "bo5-lock")
        req(f"{ADMIN}/basket/v4/division/{did}/open-registration", "PUT", token=admin_token)

    teams = fs.seed_org_pool(
        "SB",
        [
            {"code": "H1", "name": "E2E_SUB_BO5_HOME", "short": "SBH", "players": [f"SBH_P{j}" for j in range(1, 6)]},
            {"code": "A1", "name": "E2E_SUB_BO5_AWAY", "short": "SBA", "players": [f"SBA_P{j}" for j in range(1, 6)]},
        ],
        offset=300,
    )
    locked = fs.db_query(
        "SELECT id FROM basket_division_roster_version WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0",
        (did,),
    )
    if not locked:
        fs.create_teams_flow(admin_token, str(did), teams)
        fs.populate_members(teams, invite_until=2, full_players=5)
        tokens = fs.web_tokens_for_teams(teams)
        fs.register_all_teams(tokens, admin_token, str(did), teams)
        req(f"{ADMIN}/basket/v4/division/{did}/close-registration", "PUT", token=admin_token)
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/auto-allocate", "POST", {}, admin_token), "bo5-alloc")
        need(req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/lock", "POST", {}, admin_token), "bo5-roster-lock")
    else:
        req(f"{ADMIN}/basket/v4/division/{did}/close-registration", "PUT", token=admin_token)

    st = need(req(f"{ADMIN}/basket/v4/admin/stages/list?divisionId={did}", token=admin_token), "bo5-stages")
    stages = st.get("data") or []
    for s in stages:
        if s.get("id") is None and s.get("stageId") is not None:
            s["id"] = s["stageId"]
    if not stages:
        raise RuntimeError("BO5 no stages")
    sid = int(stages[0]["id"])
    published = fs.db_query(
        "SELECT publish_status FROM basket_schedule_batch WHERE stage_id=%s AND is_delete=0 AND structure_status='ACTIVE'",
        (sid,),
    )
    already = any((r.get("publish_status") or "") == "PUBLISHED" for r in published)
    if not already:
        fs.db_exec(
            "UPDATE basket_division_stage SET best_of=5, final_best_of=5, semifinal_best_of=5 WHERE id=%s",
            (sid,),
        )
        ir = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/initialize-teams", "POST", token=admin_token)
        if ir.get("code") != 200 and "已" not in str(ir.get("msg") or ""):
            log(f"bo5 init-teams {ir}")
        dr = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/generate-draw", "POST", token=admin_token)
        cf = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/confirm-draw", "POST", token=admin_token)
        log(f"bo5 draw init={ir.get('msg')} draw={dr.get('msg')} confirm={cf.get('msg')}")
        gen = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/generate-schedule", "POST", token=admin_token)
        if gen.get("code") != 200 and "已" not in str(gen.get("msg") or ""):
            raise RuntimeError(f"bo5 generate: {gen}")
        rec = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/schedule/recommend-apply", "POST", {"overwriteScheduled": False}, admin_token)
        if rec.get("code") != 200:
            units_r = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/schedule-units", token=admin_token)
            payload = units_r.get("data") or []
            units = payload.get("units") if isinstance(payload, dict) else payload
            for i, u in enumerate(units or []):
                if not isinstance(u, dict) or (u.get("scheduleStatus") or "") == "SCHEDULED":
                    continue
                req(
                    f"{ADMIN}/basket/v4/admin/stages/{sid}/schedule/assign",
                    "POST",
                    {
                        "unitType": u.get("unitType") or "MATCH_SERIES",
                        "unitId": u.get("unitId") or u.get("id"),
                        "scheduledDate": "2026-10-20",
                        "scheduledStartTime": "21:00",
                        "scheduleSource": "MANUAL",
                    },
                    admin_token,
                )
        pub = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/publish-schedule", "POST", token=admin_token)
        if pub.get("code") != 200 and "已" not in str(pub.get("msg") or ""):
            raise RuntimeError(f"bo5 publish: {pub}")
        fs.db_exec(
            "UPDATE basket_match m INNER JOIN basket_match_series s ON s.id=m.series_id AND s.is_delete=0 "
            "SET m.home_event_team_id=s.home_event_team_id, m.away_event_team_id=s.away_event_team_id "
            "WHERE m.is_delete=0 AND s.home_event_team_id IS NOT NULL AND s.away_event_team_id IS NOT NULL "
            "AND (m.home_event_team_id IS NULL OR m.away_event_team_id IS NULL)"
        )
        fs.db_exec(
            "UPDATE basket_match m INNER JOIN basket_schedule_batch b ON b.id=m.schedule_batch_id AND b.is_delete=0 "
            "SET m.publish_status='PUBLISHED' "
            "WHERE m.is_delete=0 AND b.publish_status='PUBLISHED' AND IFNULL(m.publish_status,'DRAFT')<>'PUBLISHED'"
        )

    series = fs.db_query(
        "SELECT id, series_code, best_of, home_event_team_id, away_event_team_id, series_status, home_win_count, away_win_count "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 AND home_event_team_id IS NOT NULL "
        "ORDER BY id DESC",
        (did,),
    )
    if not series:
        raise RuntimeError("BO5 series not created")
    target = next((s for s in series if int(s.get("best_of") or 0) == 5 and (s.get("series_status") or "") != "COMPLETED"), series[0])
    return {"seasonId": int(season_id), "divisionId": did, "stageId": sid, "series": target, "teams": teams}


def run_bo5(admin_token: str) -> None:
    setup = setup_bo5(admin_token)
    s = setup["series"]
    series_id = int(s["id"])
    home, away = int(s["home_event_team_id"]), int(s["away_event_team_id"])
    games = fs.db_query(
        "SELECT id, series_game_no, match_status FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no",
        (series_id,),
    )
    record("bo5.fixture", int(s.get("best_of") or 0) == 5 and len(games) >= 5, f"series={s} games={games}", "P0")
    REPORT["ids"]["bo5"] = {
        "seasonId": setup["seasonId"],
        "divisionId": setup["divisionId"],
        "seriesId": series_id,
        "seriesCode": s.get("series_code"),
        "games": [int(g["id"]) for g in games],
        "home": home,
        "away": away,
    }
    home_tok = login_team(home)
    away_tok = login_team(away)
    plan = [
        (int(games[0]["id"]), home_tok, home, 80, 70, PNG_A),
        (int(games[1]["id"]), away_tok, away, 68, 77, PNG_B),
        (int(games[2]["id"]), home_tok, home, 82, 75, PNG_A),
        (int(games[3]["id"]), home_tok, home, 79, 71, PNG_C),
    ]
    expected = [(1, 0), (1, 1), (2, 1), (3, 1)]
    for i, ((mid, tok, tid, hs, aws, png), (eh, ea)) in enumerate(zip(plan, expected), start=1):
        play_one_game(admin_token, tok, mid, tid, hs, aws, f"E2E BO5 G{i}", png)
        cur = series_row(series_id)
        record(
            f"bo5.score.{eh}-{ea}",
            int(cur.get("home_win_count") or 0) == eh and int(cur.get("away_win_count") or 0) == ea,
            str(cur),
            "P0",
        )
        if i == 4:
            record("bo5.completed.3-1", cur.get("series_status") == "COMPLETED" and int(cur.get("winner_event_team_id") or 0) == home, str(cur), "P0")
            assert_audit_view(admin_token, submission_rows(mid, tid)[-1]["id"], expect_series=True)

    g5 = games[4]
    d5 = detail(home_tok, int(g5["id"]), home)
    ctx5 = d5.get("seriesContext") or {}
    record("bo5.game5.skipped", bool(ctx5.get("skipped")), str(ctx5), "P1")
    blocked = save_draft(home_tok, int(g5["id"]), home, 1, 0, [player_row(eligible(home_tok, int(g5["id"]), home)[0]["eventPlayerId"])], [], "g5")
    record("bo5.game5.noSubmit", blocked.get("code") != 200, str(blocked.get("msg")), "P1")

    sched = need(req(f"{WEB}/basket/v4/player/my-schedule?divisionId={setup['divisionId']}&includeFinished=true", token=home_tok), "bo5-schedule")
    rows = schedule_items(sched)
    srows = [r for r in rows if str(r.get("matchSeriesId") or r.get("seriesId") or "") == str(series_id)]
    score_ok = any(int(r.get("seriesHomeWinCount") or r.get("homeWinCount") or 0) == 3 and int(r.get("seriesAwayWinCount") or r.get("awayWinCount") or 0) == 1 for r in srows)
    g5_row = next((r for r in srows if str(r.get("matchId")) == str(g5["id"])), None)
    g5_skip = bool(g5_row) and (
        g5_row.get("userActionType") == "NONE" or "无需进行" in str(g5_row.get("userActionText") or g5_row.get("cannotSubmitReason") or "")
    )
    record("bo5.ui.seriesScore", score_ok, f"rows={len(srows)} sample={srows[0] if srows else None}", "P1")
    record("bo5.ui.game5.无需进行", g5_skip, str(g5_row), "P1")

    pub = req(f"{WEB}/basket/v4/public/match-series/stage/{setup['stageId']}")
    pdata = pub.get("data") if isinstance(pub.get("data"), list) else []
    pub_hit = next((x for x in pdata if str(x.get("id") or x.get("matchSeriesId") or "") == str(series_id)), None)
    if pub_hit is None and pdata:
        pub_hit = next((x for x in pdata if (x.get("seriesCode") or "") == (s.get("series_code") or "")), pdata[0] if pdata else None)
    db_s = series_row(series_id)
    pub_score = None
    if pub_hit:
        pub_score = (
            pub_hit.get("homeWinCount"),
            pub_hit.get("awayWinCount"),
        )
        if pub_score == (None, None):
            pub_score = (pub_hit.get("seriesHomeWinCount"), pub_hit.get("seriesAwayWinCount"))
    record(
        "public.series.consistent",
        pub_score == (3, 1) or (pub_hit and str(pub_hit.get("seriesStatus") or "") in ("COMPLETED", "FINISHED")),
        f"public={pub_hit} db={db_s}",
    )


def run_invariants() -> None:
    rows = fs.db_query(
        "SELECT s.id, s.match_id, s.submit_event_team_id, m.series_id "
        "FROM basket_match_submission s JOIN basket_match m ON m.id=s.match_id "
        "WHERE s.submit_remark LIKE %s AND s.is_delete=0",
        ("E2E%",),
    )
    bad = [r for r in rows if r.get("match_id") in (None, 0)]
    record("db.submission.matchId", not bad, f"n={len(rows)} bad={bad}", "P0")
    child_bad = []
    for r in rows:
        if r.get("series_id"):
            m = match_row(int(r["match_id"]))
            if int(m.get("id") or 0) != int(r["match_id"]):
                child_bad.append(r)
    record("db.submission.childMatch", not child_bad, str(child_bad), "P0")
    p2("player.shooting.gap", "球员级 FGM/FGA 3PM/3PA FTM/FTA 本轮 NOT TESTED / NOT SUPPORTED")


def main() -> int:
    if not fs.wait_server(f"{ADMIN}/", 8) or not fs.wait_server(f"{WEB}/", 8):
        record("env.services", False, "admin 18080 / web 18881 not up", "P0")
        Path(__file__).with_name("cnpa_v13_match_submission_e2e_result.json").write_text(
            json.dumps(REPORT, ensure_ascii=False, indent=2, default=str), encoding="utf-8"
        )
        return 2
    record("env.services", True, "admin=18080 web=18881 db=basketball_r9_fresh")
    admin_token = fs.admin_login()
    REPORT["ids"]["adminLogin"] = "admin / admin123"
    try:
        run_bo1(admin_token)
    except Exception as exc:
        record("bo1.chain", False, repr(exc), "P0")
        log(f"BO1 aborted: {exc}")
    try:
        run_bo3(admin_token)
    except Exception as exc:
        record("bo3.chain", False, repr(exc), "P0")
        log(f"BO3 aborted: {exc}")
    try:
        run_bo5(admin_token)
    except Exception as exc:
        record("bo5.chain", False, repr(exc), "P0")
        log(f"BO5 aborted: {exc}")
    run_invariants()
    REPORT["endedAt"] = datetime.now().isoformat(timespec="seconds")
    out = Path(__file__).with_name("cnpa_v13_match_submission_e2e_result.json")
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    log(f"wrote {out}")
    p0 = len(REPORT["bugs"]["P0"])
    p1 = len(REPORT["bugs"]["P1"])
    return 1 if p0 or p1 else 0


if __name__ == "__main__":
    raise SystemExit(main())
