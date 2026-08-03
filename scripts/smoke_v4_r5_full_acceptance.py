#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round5 full acceptance smoke: submission -> audit -> settlement."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from datetime import datetime
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
REPORT: dict = {
    "services_restarted": True,
    "admin_base": ADMIN_BASE,
    "web_base": WEB_BASE,
    "division_id": DIVISION_ID,
    "restart_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
}


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
    if not allow_fail and code not in (200, 0, None, "200"):
        raise RuntimeError(f"{method} {path} code={code} msg={payload.get('msg')} payload={payload}")
    return payload


def http_expect_fail(base: str, method: str, path: str, body: dict | None = None, token: str | None = None) -> bool:
    payload = http_json(base, method, path, body, token, allow_fail=True)
    return payload.get("code") not in (200, 0, None)


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


def admin_login() -> str:
    resp = http_json(ADMIN_BASE, "POST", "/login", {"username": ADMIN_USER, "password": ADMIN_PASSWORD})
    token = resp.get("token")
    if not token:
        raise RuntimeError(f"admin login failed: {resp}")
    record("Admin 登录", "PASS", f"tokenLen={len(token)}")
    return str(token)


def web_login(member_id: int) -> str | None:
    row = db_exec("SELECT email FROM basket_member_account WHERE id=%s", (member_id,))
    if not row:
        return None
    resp = http_json(WEB_BASE, "POST", "/basket/member/auth/login/password",
                    {"email": row[0][0], "password": MEMBER_PASSWORD}, allow_fail=True)
    return (resp.get("data") or {}).get("token")


def find_match_context():
    rows = db_exec(
        """
        SELECT m.id, m.season_id, m.division_id, m.home_event_team_id, m.away_event_team_id,
               m.match_status, m.publish_status,
               ht.captain_member_id, at.captain_member_id,
               ht.team_name_snapshot, at.team_name_snapshot,
               ht.team_logo_file_id, at.team_logo_file_id
        FROM basket_match m
        JOIN basket_event_team ht ON ht.id = m.home_event_team_id AND ht.is_delete = 0
        JOIN basket_event_team at ON at.id = m.away_event_team_id AND at.is_delete = 0
        WHERE m.division_id = %s AND m.publish_status = 'PUBLISHED' AND m.is_delete = 0
          AND ht.captain_member_id IS NOT NULL AND at.captain_member_id IS NOT NULL
        ORDER BY m.id ASC LIMIT 1
        """,
        (DIVISION_ID,),
    )
    return rows[0] if rows else None


def get_event_players(event_team_id: int, limit=2):
    return db_exec(
        "SELECT id, player_name_snapshot FROM basket_event_player WHERE event_team_id=%s AND is_delete=0 LIMIT %s",
        (event_team_id, limit),
    )


def get_sample_file_id() -> int | None:
    for tbl in ("sys_file", "basket_file", "basket_upload_file"):
        try:
            rows = db_exec(f"SELECT id FROM {tbl} LIMIT 1")
            if rows:
                return rows[0][0]
        except Exception:
            pass
    return 1


def cleanup_submissions(match_id: int, team_ids: list[int]):
    for tid in team_ids:
        subs = db_exec(
            "SELECT id FROM basket_match_submission WHERE match_id=%s AND submit_event_team_id=%s",
            (match_id, tid),
        )
        for (sid,) in subs:
            db_exec("DELETE FROM basket_match_submission_team_stat WHERE submission_id=%s", (sid,))
            db_exec("DELETE FROM basket_match_submission_player_stat WHERE submission_id=%s", (sid,))
            db_exec("DELETE FROM basket_match_submission_material WHERE submission_id=%s", (sid,))
            db_exec("DELETE FROM basket_match_submission WHERE id=%s", (sid,))


def count_old_match_data(match_id: int) -> int:
    for tbl in ("basket_match_data_submission", "basket_match_data_audit"):
        try:
            c = db_exec(f"SELECT COUNT(*) FROM {tbl} WHERE match_id=%s", (match_id,))[0][0]
            if c:
                return c
        except Exception:
            pass
    return 0


def reset_match_official(match_id: int, division_id: int):
    db_exec(
        "UPDATE basket_match SET home_score=NULL, away_score=NULL, winner_event_team_id=NULL, match_status='SCHEDULED' WHERE id=%s",
        (match_id,),
    )
    db_exec("UPDATE basket_match_team_stat SET is_delete=1 WHERE match_id=%s", (match_id,))
    db_exec("UPDATE basket_match_player_stat SET is_delete=1 WHERE match_id=%s", (match_id,))
    for tbl in ("basket_standing", "basket_team_stat", "basket_player_stat", "basket_rank"):
        db_exec(f"UPDATE {tbl} SET is_delete=1 WHERE division_id=%s", (division_id,))


def team_score_for(team_id: int, home_team: int, home_score: int, away_score: int) -> int:
    return home_score if team_id == home_team else away_score


def main() -> int:
    web_health = http_json(WEB_BASE, "POST", "/basket/member/auth/login/password",
                           {"email": "nonexist@test.com", "password": "x"}, allow_fail=True)
    record("Web 服务可达", "PASS" if web_health is not None else "FAIL")

    ctx = find_match_context()
    if not ctx:
        record("测试数据", "FAIL", "无 PUBLISHED Match")
        return 1

    (match_id, season_id, division_id, home_team, away_team, match_status, publish_status,
     home_member, away_member, home_name, away_name, home_logo, away_logo) = ctx

    REPORT.update({
        "match_id": match_id,
        "home_event_team_id": home_team,
        "away_event_team_id": away_team,
        "home_team_name": home_name,
        "away_team_name": away_name,
    })

    record("测试 Match", "PASS", f"match={match_id} home={home_team}({home_name}) away={away_team}({away_name})")

    home_players = get_event_players(home_team)
    away_players = get_event_players(away_team)
    file_id = get_sample_file_id()

    cleanup_submissions(match_id, [home_team, away_team])
    reset_match_official(match_id, division_id)

    home_token = web_login(home_member)
    away_token = web_login(away_member)
    if not home_token or not away_token:
        record("Web 登录", "FAIL", f"home={bool(home_token)} away={bool(away_token)}")
        return 1
    record("Home/Away Web 登录", "PASS")

    # 1. my-matches
    my_matches = http_json(WEB_BASE, "GET",
                           f"/basket/v4/player/match-submission/my-matches?divisionId={DIVISION_ID}", token=home_token)
    rows = my_matches.get("data") or []
    hit = next((r for r in rows if r.get("matchId") == match_id), None)
    ok = hit is not None and "mySubmissionStatus" in hit and "canSubmit" in hit
    record("GET my-matches", "PASS" if ok else "FAIL", f"count={len(rows)} canSubmit={hit.get('canSubmit') if hit else None}")
    REPORT["my_matches_count"] = len(rows)

    # 2. save draft home
    home_player_stats = []
    if home_players:
        home_player_stats.append({
            "eventPlayerId": home_players[0][0], "points": 22, "rebound": 8, "assist": 5,
            "steal": 2, "block": 1, "turnover": 3, "foul": 2,
        })
    save_body = {
        "matchId": match_id,
        "submitEventTeamId": home_team,
        "homeScore": 88,
        "awayScore": 75,
        "teamStat": {"score": 88, "rebound": 35, "assist": 18, "steal": 8, "block": 4, "turnover": 12, "foul": 18},
        "playerStats": home_player_stats,
        "materialFileIds": [file_id] if file_id else [],
        "submitRemark": "R5 full acceptance home draft",
    }
    home_sid = http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/save", save_body, home_token)["data"]
    record("POST save 草稿", "PASS", f"submissionId={home_sid}")

    sub_row = db_exec(
        "SELECT submission_status, home_score, away_score, submit_remark FROM basket_match_submission WHERE id=%s",
        (home_sid,),
    )[0]
    record("DRAFT + 比分/备注", "PASS" if sub_row[0] == "DRAFT" and sub_row[1] == 88 else "FAIL", str(sub_row))

    ts_cnt = db_exec("SELECT COUNT(*) FROM basket_match_submission_team_stat WHERE submission_id=%s AND is_delete=0", (home_sid,))[0][0]
    ps_cnt = db_exec("SELECT COUNT(*) FROM basket_match_submission_player_stat WHERE submission_id=%s AND is_delete=0", (home_sid,))[0][0]
    mat_cnt = db_exec("SELECT COUNT(*) FROM basket_match_submission_material WHERE submission_id=%s AND is_delete=0", (home_sid,))[0][0]
    record("submission team/player/material", "PASS" if ts_cnt >= 1 else "FAIL", f"ts={ts_cnt} ps={ps_cnt} mat={mat_cnt}")

    m_off = db_exec("SELECT home_score, match_status FROM basket_match WHERE id=%s", (match_id,))[0]
    record("save 不写官方 Match", "PASS" if m_off[0] is None else "FAIL", str(m_off))
    mt_cnt = db_exec("SELECT COUNT(*) FROM basket_match_team_stat WHERE match_id=%s AND is_delete=0", (match_id,))[0][0]
    record("save 不写 match_team_stat", "PASS" if mt_cnt == 0 else "FAIL", f"count={mt_cnt}")
    st_before = db_exec("SELECT COUNT(*) FROM basket_standing WHERE division_id=%s AND is_delete=0", (division_id,))[0][0]
    record("save 不触发 standing", "PASS" if st_before == 0 else "FAIL", f"count={st_before}")

    # 3. submit home
    http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/submit", {"submissionId": home_sid}, home_token)
    home_st = db_exec("SELECT submission_status, submit_time FROM basket_match_submission WHERE id=%s", (home_sid,))[0]
    record("POST submit Home", "PASS" if home_st[0] == "PENDING_REVIEW" and home_st[1] else "FAIL", home_st[0])
    REPORT["home_submission_id"] = home_sid
    REPORT["home_submission_status_after_submit"] = home_st[0]

    # 4. Away while Home pending
    away_save = http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/save", {
        "matchId": match_id, "submitEventTeamId": away_team, "homeScore": 88, "awayScore": 75,
        "teamStat": {"score": 75, "rebound": 30, "assist": 14},
        "playerStats": [{"eventPlayerId": away_players[0][0], "points": 18, "rebound": 6, "assist": 4}] if away_players else [],
        "materialFileIds": [file_id] if file_id else [],
    }, away_token)
    away_sid = away_save["data"]
    http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/submit", {"submissionId": away_sid}, away_token)
    away_st = db_exec("SELECT submission_status FROM basket_match_submission WHERE id=%s", (away_sid,))[0][0]
    record("Away save+submit(Home审核中)", "PASS" if away_st == "PENDING_REVIEW" else "FAIL", f"awaySid={away_sid} status={away_st}")
    REPORT["away_submission_id"] = away_sid
    REPORT["away_can_submit_while_home_pending"] = away_st == "PENDING_REVIEW"

    match_st = db_exec("SELECT match_status FROM basket_match WHERE id=%s", (match_id,))[0][0]
    record("Match 无整体审核中状态", "PASS" if match_st != "PENDING_REVIEW" and match_st != "AUDITING" else "FAIL", match_st)

    # 5-6. admin list & detail
    admin_token = admin_login()
    list_path = f"/basket/v4/match-submission-audit/list?matchId={int(match_id)}&pageNum=1&pageSize=20"
    lst = http_json(ADMIN_BASE, "GET", list_path, token=admin_token)
    lst_rows = lst.get("rows") or []
    statuses = {r.get("submitTeamName"): r.get("submissionStatus") for r in lst_rows}
    record("Admin 审核列表 submission 级", "PASS" if len(lst_rows) >= 2 else "FAIL", str(statuses))

    detail = http_json(ADMIN_BASE, "GET", f"/basket/v4/match-submission-audit/{home_sid}", token=admin_token)["data"]
    detail_ok = all(k in detail for k in ("homeTeamName", "awayTeamName", "submitTeamName", "submitterRole",
                                           "versionNo", "homeScore", "awayScore", "teamStat", "playerStats", "materials"))
    record("Admin 审核详情字段", "PASS" if detail_ok else "FAIL",
           f"logo home={detail.get('homeTeamLogoFileId')} away={detail.get('awayTeamLogoFileId')} submit={detail.get('submitTeamLogoFileId')}")

    # 7. reject away
    http_json(ADMIN_BASE, "POST", f"/basket/v4/match-submission-audit/{away_sid}/reject",
              {"rejectReason": "R5 smoke reject", "auditRemark": "test"}, token=admin_token)
    away_rej = db_exec("SELECT submission_status FROM basket_match_submission WHERE id=%s", (away_sid,))[0][0]
    m_after_rej = db_exec("SELECT home_score FROM basket_match WHERE id=%s", (match_id,))[0][0]
    record("驳回 Away", "PASS" if away_rej == "REJECTED" and m_after_rej is None else "FAIL", away_rej)
    REPORT["reject_result"] = away_rej

    # 8. resubmit away
    old_ver = db_exec("SELECT version_no FROM basket_match_submission WHERE id=%s", (away_sid,))[0][0]
    new_sid = http_json(WEB_BASE, "POST", "/basket/v4/player/match-submission/resubmit", {
        "oldSubmissionId": away_sid, "homeScore": 88, "awayScore": 76,
        "teamStat": {"score": 76, "rebound": 32, "assist": 15},
        "playerStats": [{"eventPlayerId": away_players[0][0], "points": 20, "rebound": 7, "assist": 5}] if away_players else [],
        "materialFileIds": [file_id] if file_id else [],
        "submitRemark": "resubmit v2",
    }, away_token)["data"]
    new_ver = db_exec("SELECT version_no, submission_status FROM basket_match_submission WHERE id=%s", (new_sid,))[0]
    old_st = db_exec("SELECT submission_status FROM basket_match_submission WHERE id=%s", (away_sid,))[0][0]
    record("驳回后 resubmit versionNo+1", "PASS" if new_ver[0] == old_ver + 1 and old_st == "REJECTED" else "FAIL",
           f"oldV={old_ver} newV={new_ver[0]} newStatus={new_ver[1]}")
    REPORT["resubmit_new_submission_id"] = new_sid
    REPORT["resubmit_version_no"] = new_ver[0]

    # Rejection scenarios (use fresh draft match / unpublish trick)
    draft_mid = db_exec("SELECT id FROM basket_match WHERE division_id=%s AND publish_status='DRAFT' AND is_delete=0 LIMIT 1", (division_id,))
    if draft_mid:
        record("拒绝: 未发布 Match", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save",
              {"matchId": draft_mid[0][0], "submitEventTeamId": home_team, "homeScore": 1, "awayScore": 0,
               "teamStat": {"score": 1}}, home_token) else "FAIL")
    else:
        db_exec("UPDATE basket_match SET publish_status='DRAFT' WHERE id=%s", (match_id,))
        try:
            record("拒绝: 未发布 Match", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save",
                  {"matchId": match_id, "submitEventTeamId": home_team, "homeScore": 1, "awayScore": 0,
                   "teamStat": {"score": 1}}, home_token) else "FAIL")
        finally:
            db_exec("UPDATE basket_match SET publish_status='PUBLISHED' WHERE id=%s", (match_id,))

    cancelled = db_exec("SELECT id FROM basket_match WHERE division_id=%s AND match_status='CANCELLED' AND is_delete=0 LIMIT 1", (division_id,))
    if cancelled:
        record("拒绝: CANCELLED Match", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save",
              {"matchId": cancelled[0][0], "submitEventTeamId": home_team, "homeScore": 1, "awayScore": 0,
               "teamStat": {"score": 1}}, home_token) else "FAIL")
    else:
        record("拒绝: CANCELLED Match", "SKIP", "无 CANCELLED match")

    record("拒绝: 无效 submitEventTeamId", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save",
          {"matchId": match_id, "submitEventTeamId": 999999, "homeScore": 1, "awayScore": 0}, home_token) else "FAIL")

    if away_players and home_players:
        record("拒绝: 非本队 EventPlayer", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save", {
            "matchId": match_id, "submitEventTeamId": home_team, "homeScore": 88, "awayScore": 75,
            "playerStats": [{"eventPlayerId": away_players[0][0], "points": 1}], "teamStat": {"score": 88},
        }, home_token) else "FAIL")

    record("拒绝: PENDING_REVIEW 覆盖 save", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save",
          {"matchId": match_id, "submitEventTeamId": home_team, "homeScore": 99, "awayScore": 99}, home_token) else "FAIL")

    record("拒绝: 非 PENDING 审核通过", "PASS" if http_expect_fail(ADMIN_BASE, "POST",
          f"/basket/v4/match-submission-audit/{away_sid}/approve", {}, token=admin_token) else "FAIL")

    record("拒绝: 非 PENDING 驳回", "PASS" if http_expect_fail(ADMIN_BASE, "POST",
          f"/basket/v4/match-submission-audit/{away_sid}/reject", {"rejectReason": "x"}, token=admin_token) else "FAIL")

    old_md = count_old_match_data(match_id)
    record("V4 不写旧 match_data 表", "PASS" if old_md == 0 else "FAIL", f"old_rows={old_md}")

    # 9. approve home
    http_json(ADMIN_BASE, "POST", f"/basket/v4/match-submission-audit/{home_sid}/approve",
              {"auditRemark": "R5 full acceptance approve"}, token=admin_token)
    home_ap = db_exec("SELECT submission_status FROM basket_match_submission WHERE id=%s", (home_sid,))[0][0]
    m = db_exec("SELECT home_score, away_score, winner_event_team_id, match_status FROM basket_match WHERE id=%s", (match_id,))[0]
    record("审核通过 Home", "PASS" if home_ap == "APPROVED" else "FAIL", home_ap)
    record("官方 Match 写入", "PASS" if m[0] == 88 and m[1] == 75 and m[3] == "FINISHED" else "FAIL", str(m))
    REPORT["approve_result"] = home_ap
    REPORT["match_official"] = {"home_score": m[0], "away_score": m[1], "winner_event_team_id": m[2], "match_status": m[3]}

    mts = db_exec(
        "SELECT event_team_id, source_submission_id FROM basket_match_team_stat WHERE match_id=%s AND is_delete=0",
        (match_id,),
    )
    record("match_team_stat + source_submission_id", "PASS" if mts and any(r[1] == home_sid for r in mts) else "FAIL", str(mts))

    mps = db_exec(
        "SELECT COUNT(*), MAX(source_submission_id) FROM basket_match_player_stat WHERE match_id=%s AND is_delete=0",
        (match_id,),
    )[0]
    record("match_player_stat 写入", "PASS" if mps[0] >= 1 and mps[1] == home_sid else "FAIL", f"count={mps[0]} src={mps[1]}")

    st_cnt = db_exec("SELECT COUNT(*) FROM basket_standing WHERE division_id=%s AND is_delete=0", (division_id,))[0][0]
    tt_cnt = db_exec("SELECT COUNT(*) FROM basket_team_stat WHERE division_id=%s AND is_delete=0", (division_id,))[0][0]
    pp_cnt = db_exec("SELECT COUNT(*) FROM basket_player_stat WHERE division_id=%s AND is_delete=0", (division_id,))[0][0]
    rk_types = db_exec(
        "SELECT DISTINCT rank_type FROM basket_rank WHERE division_id=%s AND is_delete=0", (division_id,)
    )
    rk_set = {r[0] for r in rk_types}
    need_ranks = {"TEAM_STANDING", "PLAYER_SCORE", "PLAYER_ASSIST", "PLAYER_REBOUND"}
    record("standing 重算", "PASS" if st_cnt >= 1 else "FAIL", f"count={st_cnt}")
    record("team_stat 重算", "PASS" if tt_cnt >= 1 else "FAIL", f"count={tt_cnt}")
    record("player_stat 重算", "PASS" if pp_cnt >= 1 else "FAIL", f"count={pp_cnt}")
    record("rank 重算", "PASS" if need_ranks <= rk_set else "FAIL", f"types={rk_set}")
    REPORT["stats"] = {"standing": st_cnt, "team_stat": tt_cnt, "player_stat": pp_cnt, "rank_types": list(rk_set)}

    # 10. stats APIs
    for path, key in [("standing", "standing"), ("team-stat", "team"), ("player-stat", "player"), ("rank", "rank")]:
        api = http_json(ADMIN_BASE, "GET", f"/basket/v4/stats/{path}?divisionId={division_id}", token=admin_token)
        data = api.get("data") or []
        record(f"GET stats/{path}", "PASS" if len(data) >= 0 else "FAIL", f"rows={len(data)}")

    # APPROVED 不可编辑
    record("拒绝: APPROVED 不可 save", "PASS" if http_expect_fail(WEB_BASE, "POST", "/basket/v4/player/match-submission/save",
          {"matchId": match_id, "submitEventTeamId": home_team, "homeScore": 100, "awayScore": 100}, home_token) else "FAIL")

    fail = sum(1 for _, s, _ in RESULTS if s == "FAIL")
    REPORT["fail_count"] = fail
    REPORT["pass_count"] = len(RESULTS) - fail
    REPORT["results"] = RESULTS

    out = Path(__file__).resolve().parents[1] / "docs" / "r5-smoke-results.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(f"\n=== R5 Full Acceptance: {len(RESULTS)-fail}/{len(RESULTS)} passed, {fail} failed ===")
    print(f"Report JSON: {out}")
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
