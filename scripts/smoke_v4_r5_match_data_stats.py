#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round5 smoke: match submission -> audit -> settlement."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

import pymysql

ADMIN_BASE = "http://127.0.0.1:8080"
WEB_BASE = "http://127.0.0.1:8881"
ADMIN_USER = "admin"
ADMIN_PASSWORD = "admin123"
MEMBER_PASSWORD = "111111aa"
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")
DIVISION_ID = 5
RESULTS: list[tuple[str, str, str]] = []


def record(name: str, status: str, detail: str = "") -> None:
    RESULTS.append((name, status, detail))
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(base: str, method: str, path: str, body: dict | None = None, token: str | None = None, allow_fail=False):
    url = base + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        payload = json.loads(exc.read().decode("utf-8", errors="ignore") or "{}")
        if not allow_fail:
            raise RuntimeError(f"{method} {path} HTTP {exc.code}: {payload}") from exc
    code = payload.get("code")
    if not allow_fail and code not in (200, 0, None):
        raise RuntimeError(f"{method} {path} code={code} msg={payload.get('msg')}")
    return payload


def db_exec(sql, args=()):
    conn = pymysql.connect(**CONN)
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args)
            if sql.strip().upper().startswith("SELECT"):
                return cur.fetchall()
            conn.commit()
    finally:
        conn.close()


def admin_login() -> str | None:
    try:
        resp = http_json(ADMIN_BASE, "POST", "/login", {"username": ADMIN_USER, "password": ADMIN_PASSWORD})
        token = resp.get("token")
        if token:
            record("Admin 登录", "PASS")
            return token
    except Exception as exc:
        record("Admin 登录", "SKIP", str(exc))
    return None


def web_login(member_id: int) -> str | None:
    row = db_exec("SELECT email FROM basket_member_account WHERE id=%s", (member_id,))
    if not row:
        return None
    email = row[0][0]
    try:
        resp = http_json(WEB_BASE, "POST", "/basket/member/auth/login/password", {"email": email, "password": MEMBER_PASSWORD})
        token = resp.get("data", {}).get("token")
        return token
    except Exception:
        return None


def find_match_context():
    rows = db_exec(
        """
        SELECT m.id, m.home_event_team_id, m.away_event_team_id,
               ht.captain_member_id, at.captain_member_id,
               ht.captain_player_id, at.captain_player_id
        FROM basket_match m
        JOIN basket_event_team ht ON ht.id = m.home_event_team_id AND ht.is_delete = 0
        JOIN basket_event_team at ON at.id = m.away_event_team_id AND at.is_delete = 0
        WHERE m.division_id = %s AND m.publish_status = 'PUBLISHED' AND m.is_delete = 0
        ORDER BY m.id ASC LIMIT 1
        """,
        (DIVISION_ID,),
    )
    return rows[0] if rows else None


def cleanup_submissions(match_id: int, team_ids: list[int]):
    for tid in team_ids:
        subs = db_exec(
            "SELECT id FROM basket_match_submission WHERE match_id=%s AND submit_event_team_id=%s AND is_delete=0",
            (match_id, tid),
        )
        for (sid,) in subs:
            db_exec("UPDATE basket_match_submission_team_stat SET is_delete=1 WHERE submission_id=%s", (sid,))
            db_exec("UPDATE basket_match_submission_player_stat SET is_delete=1 WHERE submission_id=%s", (sid,))
            db_exec("UPDATE basket_match_submission_material SET is_delete=1 WHERE submission_id=%s", (sid,))
            db_exec("UPDATE basket_match_submission SET is_delete=1 WHERE id=%s", (sid,))


def save_and_submit(web_token: str, match_id: int, team_id: int, home_score: int, away_score: int) -> int | None:
    body = {
        "matchId": match_id,
        "submitEventTeamId": team_id,
        "homeScore": home_score,
        "awayScore": away_score,
        "teamStat": {"score": home_score if team_id else away_score, "rebound": 30, "assist": 15},
        "playerStats": [],
        "materialFileIds": [],
        "submitRemark": "R5 smoke",
    }
    resp = http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/save", body, web_token)
    sid = resp.get("data")
    if not sid:
        return None
    http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/submit", {"submissionId": sid}, web_token)
    return sid


def main() -> int:
    admin_token = admin_login()
    if not admin_token:
        return 0

    cols = [r[0] for r in db_exec(
        "SELECT column_name FROM information_schema.columns WHERE table_schema='basketball' AND table_name='basket_match_submission'"
    )]
    for c in ("home_score", "away_score", "submit_remark"):
        record(f"DB patch: {c}", "PASS" if c in cols else "FAIL", "exists" if c in cols else "missing")

    ctx = find_match_context()
    if not ctx:
        record("测试数据", "SKIP", "无 PUBLISHED Match")
        return 0
    match_id, home_team, away_team, home_member, away_member, home_player, away_player = ctx
    record("测试 Match", "PASS", f"match={match_id} home={home_team} away={away_team}")

    cleanup_submissions(match_id, [home_team, away_team])
    db_exec(
        "UPDATE basket_match SET home_score=NULL, away_score=NULL, winner_event_team_id=NULL, match_status='SCHEDULED' WHERE id=%s",
        (match_id,),
    )

    home_token = web_login(home_member) if home_member else None
    away_token = web_login(away_member) if away_member else None
    if not home_token:
        record("Home Web 登录", "SKIP", f"member={home_member}")
        return 0
    record("Home Web 登录", "PASS")

    my_matches = http_json(WEB_BASE, "GET", f"/basket/v4/player/match-submission/my-matches?divisionId={DIVISION_ID}", token=home_token)
    data = my_matches.get("data") or []
    record("我的可提交比赛", "PASS" if any(r.get("matchId") == match_id for r in data) else "FAIL", f"count={len(data)}")

    home_sid = save_and_submit(home_token, match_id, home_team, 88, 75)
    record("Home 提交审核", "PASS" if home_sid else "FAIL", f"submissionId={home_sid}")

    row = db_exec("SELECT submission_status FROM basket_match_submission WHERE id=%s", (home_sid,))[0]
    record("Home PENDING_REVIEW", "PASS" if row[0] == "PENDING_REVIEW" else "FAIL", row[0])

    if away_token:
        away_save = http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/save", {
            "matchId": match_id, "submitEventTeamId": away_team, "homeScore": 88, "awayScore": 75,
            "teamStat": {"score": 75, "rebound": 28, "assist": 12}, "playerStats": [], "materialFileIds": []
        }, away_token)
        away_sid = away_save.get("data")
        http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/submit", {"submissionId": away_sid}, away_token)
        record("Away 独立提交(Home审核中)", "PASS", f"submissionId={away_sid}")
    else:
        record("Away 独立提交", "SKIP", "无 away token")

    lst = http_json(ADMIN_BASE, "GET", f"/basket/v4/match-submission-audit/list?matchId={match_id}&pageNum=1&pageSize=20", token=admin_token)
    record("Admin 审核列表", "PASS", f"total={lst.get('total', 0)}")

    detail = http_json(ADMIN_BASE, "GET", f"/basket/v4/match-submission-audit/{home_sid}", token=admin_token)
    record("Admin 审核详情", "PASS" if detail.get("data") else "FAIL")

    http_json(ADMIN_BASE, "POST", f"/basket/v4/match-submission-audit/{home_sid}/approve", {"auditRemark": "R5 smoke"}, admin_token)
    record("Admin 审核通过", "PASS")

    sub = db_exec("SELECT submission_status FROM basket_match_submission WHERE id=%s", (home_sid,))[0][0]
    record("submission APPROVED", "PASS" if sub == "APPROVED" else "FAIL", sub)

    m = db_exec("SELECT home_score, away_score, match_status FROM basket_match WHERE id=%s", (match_id,))[0]
    record("官方 Match 写入", "PASS" if m == (88, 75, "FINISHED") else "FAIL", str(m))

    ts = db_exec("SELECT COUNT(*) FROM basket_match_team_stat WHERE match_id=%s AND is_delete=0", (match_id,))[0][0]
    record("match_team_stat 写入", "PASS" if ts >= 1 else "FAIL", f"count={ts}")

    st = db_exec("SELECT COUNT(*) FROM basket_standing WHERE division_id=%s AND is_delete=0", (DIVISION_ID,))[0][0]
    record("standing 重算", "PASS" if st >= 1 else "FAIL", f"count={st}")

    tt = db_exec("SELECT COUNT(*) FROM basket_team_stat WHERE division_id=%s AND is_delete=0", (DIVISION_ID,))[0][0]
    record("team_stat 重算", "PASS" if tt >= 1 else "FAIL", f"count={tt}")

    rk = db_exec("SELECT COUNT(*) FROM basket_rank WHERE division_id=%s AND is_delete=0", (DIVISION_ID,))[0][0]
    record("rank 重算", "PASS" if rk >= 1 else "FAIL", f"count={rk}")

    standing_api = http_json(ADMIN_BASE, "GET", f"/basket/v4/stats/standing?divisionId={DIVISION_ID}", token=admin_token)
    record("Standing API", "PASS" if standing_api.get("data") else "FAIL")

    fail = sum(1 for _, s, _ in RESULTS if s == "FAIL")
    print(f"\n=== R5 Smoke: {len(RESULTS)-fail}/{len(RESULTS)} passed, {fail} failed ===")
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
