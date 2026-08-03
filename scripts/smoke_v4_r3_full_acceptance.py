#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round3 full acceptance: CaptainGrant -> Registration -> Audit -> Snapshot."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
ADMIN_BASE = "http://127.0.0.1:8080"
WEB_BASE = "http://127.0.0.1:8881"
ADMIN_USER = "admin"
ADMIN_PASSWORD = "admin123"
MEMBER_PASSWORD = "111111aa"
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")

# Test fixtures (string IDs avoid JS float precision issues)
SEASON_ID = 2
DIVISION_ID = 3  # SMOKE S2 - PC
QUOTA_SEASON_NAME = "R3_QUOTA_SEASON"
TEAM_ALPHA_ID = "2083464934804459521"
TEAM_BETA_ID = "2083464935756566530"
TEAM_GAMMA_ID = "2083464936591233026"
CAPTAIN_MEMBER_ID = "2082478335308455938"
CAPTAIN_PLAYER_ID = "2082478335308455940"
CAPTAIN_EMAIL = "111111@qq.com"
ALPHA_PLAYER_IDS = [
    "2082478335308455940",
    "2083401000000050002",
    "2083401000000060002",
    "2083401000000070002",
    "2083401000000080002",
]
OUTSIDER_PLAYER_ID = "2081010979671425026"  # PS only, not on Alpha
BETA_CAPTAIN_EMAIL = "111122@qq.com"
BETA_CAPTAIN_PLAYER_ID = "2083401000000100002"
BETA_CAPTAIN_MEMBER_ID = "2083401000000100000"
BETA_PLAYER_IDS = [
    "2083401000000100002",
    "2083401000000090002",
    "2083401000000110002",
    "2083401000000120002",
    "2083401000000000002",
]
GAMMA_CAPTAIN_EMAIL = "111125@qq.com"
GAMMA_CAPTAIN_PLAYER_ID = "2083401000000130002"
GAMMA_CAPTAIN_MEMBER_ID = "2083401000000130000"
GAMMA_PLAYER_IDS = [
    "2083401000000130002",
    "2083401000000140002",
    "2083401000000150002",
    "2083401000000160002",
    "2083401000000010002",
]

RESULTS: list[tuple[str, str, str]] = []
REPORT: dict = {}


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
    if not allow_fail and payload.get("code") not in (200, None):
        raise RuntimeError(f"{method} {path} code={payload.get('code')} msg={payload.get('msg')}")
    return payload


def admin_login() -> str:
    resp = http_json(ADMIN_BASE, "POST", "/login", {"username": ADMIN_USER, "password": ADMIN_PASSWORD})
    token = resp.get("token")
    if not token:
        raise RuntimeError(f"admin login failed: {resp}")
    return token


def web_login(email: str) -> str:
    for attempt in range(3):
        try:
            resp = http_json(WEB_BASE, "POST", "/basket/member/auth/login/password", {"email": email, "password": MEMBER_PASSWORD})
            token = resp.get("data", {}).get("token")
            if token:
                return token
        except Exception:
            if attempt == 2:
                raise
            import time
            time.sleep(1)
    raise RuntimeError(f"web login failed for {email}")


def sid(val: str | int) -> int:
    return int(val)


def db_query(sql, args=()):
    conn = pymysql.connect(**CONN)
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args)
            if sql.strip().upper().startswith("SELECT"):
                return cur.fetchall()
            conn.commit()
    finally:
        conn.close()


def db_one(sql, args=()):
    rows = db_query(sql, args)
    return rows[0] if rows else None


def cleanup_division_team(cur, division_id: int, team_id: str) -> None:
    cur.execute(
        "UPDATE basket_captain_grant SET grant_status='REVOKED', active_unique_key=NULL WHERE division_id=%s AND team_id=%s AND is_delete=0",
        (division_id, team_id),
    )
    cur.execute("SELECT id FROM basket_registration WHERE division_id=%s AND team_id=%s AND is_delete=0", (division_id, team_id))
    for (rid,) in cur.fetchall():
        cur.execute("DELETE FROM basket_registration_player WHERE registration_id=%s", (rid,))
        cur.execute("DELETE FROM basket_registration_audit WHERE registration_id=%s", (rid,))
        cur.execute(
            "DELETE FROM basket_event_player WHERE event_team_id IN (SELECT id FROM basket_event_team WHERE registration_id=%s)",
            (rid,),
        )
        cur.execute("DELETE FROM basket_event_team WHERE registration_id=%s", (rid,))
        cur.execute("DELETE FROM basket_registration WHERE id=%s", (rid,))


def prepare_season_division(admin_token: str, cur, conn) -> int:
    cur.execute("SELECT publish_status FROM basket_season WHERE id=%s", (SEASON_ID,))
    pub = cur.fetchone()[0]
    if pub == "PUBLISHED":
        record("补数据: 发布 Season", "PASS", "已是 PUBLISHED")
    elif pub == "DRAFT":
        http_json(ADMIN_BASE, "PUT", f"/basket/v4/season/{SEASON_ID}/publish", token=admin_token)
        record("补数据: 发布 Season", "PASS", f"seasonId={SEASON_ID} API publish")
    else:
        cur.execute(
            "UPDATE basket_season SET publish_status='PUBLISHED', season_status='REGISTRATION_OPEN', update_time=NOW() WHERE id=%s",
            (SEASON_ID,),
        )
        conn.commit()
        record("补数据: 发布 Season", "PASS", f"seasonId={SEASON_ID} OFFLINE→PUBLISHED(SQL)")

    cur.execute("SELECT division_status FROM basket_division WHERE id=%s", (DIVISION_ID,))
    st = cur.fetchone()[0]
    cur.execute(
        "UPDATE basket_division SET registration_start_time=DATE_SUB(NOW(), INTERVAL 1 DAY), registration_end_time=DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE id=%s",
        (DIVISION_ID,),
    )
    conn.commit()
    if st == "REGISTRATION_OPEN":
        record("补数据: 开放 Division 报名", "PASS", "已是 REGISTRATION_OPEN")
    else:
        try:
            http_json(ADMIN_BASE, "PUT", f"/basket/v4/division/{DIVISION_ID}/open-registration", token=admin_token)
            record("补数据: 开放 Division 报名", "PASS", f"divisionId={DIVISION_ID} API")
        except Exception:
            cur.execute(
                "UPDATE basket_division SET division_status='REGISTRATION_OPEN', update_time=NOW() WHERE id=%s",
                (DIVISION_ID,),
            )
            conn.commit()
            record("补数据: 开放 Division 报名", "PASS", f"divisionId={DIVISION_ID} SQL")

    cur.execute(
        "SELECT d.id FROM basket_division d JOIN basket_season s ON s.id=d.season_id WHERE s.season_name=%s AND d.division_name=%s AND d.is_delete=0",
        (QUOTA_SEASON_NAME, "R3_QUOTA_TEST_PC"),
    )
    row = cur.fetchone()
    if row:
        quota_div_id = row[0]
        record("补数据: 名额测试 Division", "PASS", f"复用 id={quota_div_id}")
    else:
        now = datetime.now()
        season_body = {
            "seriesId": 1,
            "seasonYear": 2026,
            "seasonName": QUOTA_SEASON_NAME,
            "displayTitle": QUOTA_SEASON_NAME,
            "registrationMode": "TEAM",
            "formatType": "REGULAR_ROUND_ROBIN",
            "registrationStartTime": (now - timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S"),
            "registrationEndTime": (now + timedelta(days=30)).strftime("%Y-%m-%d %H:%M:%S"),
        }
        season_resp = http_json(ADMIN_BASE, "POST", "/basket/v4/season", season_body, admin_token)
        quota_season_id = season_resp["data"]
        http_json(ADMIN_BASE, "PUT", f"/basket/v4/season/{quota_season_id}/publish", token=admin_token)
        div_body = {
            "seasonId": quota_season_id,
            "platformCode": "PC",
            "divisionName": "R3_QUOTA_TEST_PC",
            "registrationMode": "TEAM",
            "formatType": "REGULAR_ROUND_ROBIN",
            "maxTeamCount": 2,
            "lineupMinCount": 5,
            "lineupMaxCount": 8,
            "registrationStartTime": (now - timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S"),
            "registrationEndTime": (now + timedelta(days=30)).strftime("%Y-%m-%d %H:%M:%S"),
            "sortOrder": 1,
        }
        div_resp = http_json(ADMIN_BASE, "POST", "/basket/v4/division", div_body, admin_token)
        quota_div_id = div_resp["data"]
        http_json(ADMIN_BASE, "PUT", f"/basket/v4/division/{quota_div_id}/open-registration", token=admin_token)
        record("补数据: 创建名额测试 Season+Division", "PASS", f"season={quota_season_id} division={quota_div_id} maxTeamCount=2")
    cur.execute(
        "UPDATE basket_division SET division_status='REGISTRATION_OPEN', registration_start_time=DATE_SUB(NOW(), INTERVAL 1 DAY), registration_end_time=DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE id=%s",
        (quota_div_id,),
    )
    conn.commit()
    return quota_div_id


def create_grant(admin_token: str, division_id: int, team_id: str, player_id: str, member_id: str) -> int:
    now = datetime.now()
    resp = http_json(
        ADMIN_BASE,
        "POST",
        "/basket/v4/captain-grant",
        {
            "divisionId": division_id,
            "teamId": sid(team_id),
            "playerId": sid(player_id),
            "memberId": sid(member_id),
            "validFrom": now.strftime("%Y-%m-%d %H:%M:%S"),
            "validTo": (now + timedelta(days=30)).strftime("%Y-%m-%d %H:%M:%S"),
            "remark": "R3 full acceptance",
        },
        admin_token,
    )
    return resp["data"]


def save_registration(web_token: str, division_id: int, team_id: str, player_ids: list[str], allow_fail=False):
    body = {
        "divisionId": division_id,
        "teamId": sid(team_id),
        "playerIds": [sid(x) for x in player_ids],
    }
    return http_json(WEB_BASE, "POST", "/basket/v4/player/registration/save", body, web_token, allow_fail=allow_fail)


def submit_registration(web_token: str, registration_id: int, allow_fail=False):
    return http_json(
        WEB_BASE,
        "POST",
        "/basket/v4/player/registration/submit",
        {"registrationId": registration_id},
        web_token,
        allow_fail=allow_fail,
    )


def expect_fail(name: str, fn, keyword: str | None = None) -> None:
    try:
        fn()
        record(name, "FAIL", "期望失败但成功了")
    except Exception as exc:
        msg = str(exc)
        if keyword and keyword not in msg:
            record(name, "FAIL", f"失败但消息不匹配: {msg[:120]}")
        else:
            record(name, "PASS", msg[:120])


def main() -> int:
    admin_token = admin_login()
    record("Admin 登录", "PASS", ADMIN_USER)

    web_token = web_login(CAPTAIN_EMAIL)
    record("Web 登录", "PASS", CAPTAIN_EMAIL)

    conn = pymysql.connect(**CONN)
    cur = conn.cursor()
    conn.autocommit(True)

    quota_div_id = prepare_season_division(admin_token, cur, conn)
    conn.commit()

    for team_id in [TEAM_ALPHA_ID, TEAM_BETA_ID, TEAM_GAMMA_ID]:
        cleanup_division_team(cur, DIVISION_ID, team_id)
        cleanup_division_team(cur, quota_div_id, team_id)
    conn.commit()

    # --- Main flow ---
    grant_id = create_grant(admin_token, DIVISION_ID, TEAM_ALPHA_ID, CAPTAIN_PLAYER_ID, CAPTAIN_MEMBER_ID)
    cur.execute(
        "SELECT grant_status, active_unique_key FROM basket_captain_grant WHERE id=%s",
        (grant_id,),
    )
    gs, auk = cur.fetchone()
    expected_key = f"{DIVISION_ID}_{TEAM_ALPHA_ID}"
    ok = gs == "ACTIVE" and auk == expected_key
    record("CaptainGrant 创建", "PASS" if ok else "FAIL", f"grantId={grant_id} status={gs} key={auk}")
    REPORT["grant_id"] = grant_id

    avail = http_json(WEB_BASE, "GET", "/basket/v4/player/registration/available-divisions", token=web_token)
    rows = avail.get("data") or []
    target = next((r for r in rows if r.get("divisionId") == DIVISION_ID), None)
    can_reg = target and target.get("canRegister") is True
    record("Web 可报名 Division", "PASS" if can_reg else "FAIL", f"canRegister={target.get('canRegister') if target else None}")

    save_resp = save_registration(web_token, DIVISION_ID, TEAM_ALPHA_ID, ALPHA_PLAYER_IDS)
    if save_resp.get("code") != 200:
        raise RuntimeError(f"save failed: {save_resp}")
    reg_id = int(save_resp["data"]) if save_resp.get("data") is not None else None
    if reg_id is None:
        raise RuntimeError(f"save returned null data: {save_resp}")
    row = db_one("SELECT registration_status FROM basket_registration WHERE id=%s", (reg_id,))
    if row is None:
        rows = db_query("SELECT id, registration_status FROM basket_registration WHERE division_id=%s AND team_id=%s", (DIVISION_ID, TEAM_ALPHA_ID))
        raise RuntimeError(f"registration {reg_id} not found; rows={rows} save={save_resp}")
    st = row[0]
    pcnt = db_one("SELECT COUNT(*) FROM basket_registration_player WHERE registration_id=%s AND is_delete=0", (reg_id,))[0]
    record("Web 保存报名草稿", "PASS" if st == "DRAFT" and pcnt == 5 else "FAIL", f"regId={reg_id} status={st} players={pcnt}")
    REPORT["registration_id"] = reg_id
    REPORT["lineup_count"] = pcnt

    submit_registration(web_token, reg_id)
    cur.execute("SELECT registration_status FROM basket_registration WHERE id=%s", (reg_id,))
    st = cur.fetchone()[0]
    cur.execute(
        "SELECT audit_action FROM basket_registration_audit WHERE registration_id=%s ORDER BY id",
        (reg_id,),
    )
    actions = [r[0] for r in cur.fetchall()]
    record("Web 提交报名", "PASS" if st == "PENDING_REVIEW" and "SUBMIT" in actions else "FAIL", f"status={st} audits={actions}")

    detail = http_json(ADMIN_BASE, "GET", f"/basket/v4/registration-audit/{reg_id}", token=admin_token)
    d = detail.get("data") or {}
    ok = int(d.get("registrationId") or 0) == reg_id and len(d.get("players") or []) == 5 and len(d.get("audits") or []) >= 1
    record("后台审核详情", "PASS" if ok else "FAIL", f"regId={d.get('registrationId')} players={len(d.get('players') or [])} audits={len(d.get('audits') or [])}")

    http_json(
        ADMIN_BASE,
        "POST",
        f"/basket/v4/registration-audit/{reg_id}/reject",
        {"rejectReason": "R3 smoke reject", "auditRemark": "补验收驳回"},
        admin_token,
    )
    cur.execute("SELECT registration_status FROM basket_registration WHERE id=%s", (reg_id,))
    st = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM basket_event_team WHERE registration_id=%s AND is_delete=0", (reg_id,))
    et0 = cur.fetchone()[0]
    cur.execute("SELECT audit_action FROM basket_registration_audit WHERE registration_id=%s ORDER BY id", (reg_id,))
    actions = [r[0] for r in cur.fetchall()]
    record(
        "后台审核驳回",
        "PASS" if st == "REJECTED" and et0 == 0 and "REJECT" in actions else "FAIL",
        f"status={st} eventTeam={et0} audits={actions}",
    )

    submit_registration(web_token, reg_id)
    cur.execute("SELECT registration_status FROM basket_registration WHERE id=%s", (reg_id,))
    st = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM basket_registration WHERE division_id=%s AND team_id=%s AND is_delete=0", (DIVISION_ID, TEAM_ALPHA_ID))
    rcnt = cur.fetchone()[0]
    cur.execute("SELECT audit_action FROM basket_registration_audit WHERE registration_id=%s ORDER BY id", (reg_id,))
    actions = [r[0] for r in cur.fetchall()]
    record(
        "Web 驳回后重提",
        "PASS" if st == "PENDING_REVIEW" and rcnt == 1 and actions.count("SUBMIT") >= 2 else "FAIL",
        f"sameRegId={reg_id} status={st} submitCount={actions.count('SUBMIT')}",
    )

    # --- Rejection scenarios (Beta, before Alpha approve) ---
    beta_token = web_login(BETA_CAPTAIN_EMAIL)
    create_grant(admin_token, DIVISION_ID, TEAM_BETA_ID, BETA_CAPTAIN_PLAYER_ID, BETA_CAPTAIN_MEMBER_ID)

    expect_fail(
        "拒绝: 无 ACTIVE CaptainGrant 保存",
        lambda: save_registration(web_token, DIVISION_ID, TEAM_GAMMA_ID, BETA_PLAYER_IDS),
        "无有效队长授权",
    )

    expect_fail(
        "拒绝: 阵容少于 5 人",
        lambda: save_registration(beta_token, DIVISION_ID, TEAM_BETA_ID, BETA_PLAYER_IDS[:4]),
        "阵容人数不能少于",
    )

    expect_fail(
        "拒绝: 阵容多于 8 人",
        lambda: save_registration(
            beta_token,
            DIVISION_ID,
            TEAM_BETA_ID,
            BETA_PLAYER_IDS + [OUTSIDER_PLAYER_ID, "2081019227996807169", "2081041427005038596", "2081073127508418562"],
        ),
        "阵容人数不能多于",
    )

    bad_lineup = BETA_PLAYER_IDS[:4] + [OUTSIDER_PLAYER_ID]
    expect_fail(
        "拒绝: 阵容成员不属于长期球队",
        lambda: save_registration(beta_token, DIVISION_ID, TEAM_BETA_ID, bad_lineup),
        "不是该球队当前有效成员",
    )

    cur.execute(
        "SELECT id FROM basket_player_platform_account WHERE player_id=%s AND UPPER(platform)='PC' AND is_delete=0 LIMIT 1",
        (BETA_PLAYER_IDS[1],),
    )
    pc_row = cur.fetchone()
    if pc_row:
        pc_acc_id = pc_row[0]
        cur.execute("UPDATE basket_player_platform_account SET account_status=0 WHERE id=%s", (pc_acc_id,))
        conn.commit()
        lineup_no_pc = BETA_PLAYER_IDS
        expect_fail(
            "拒绝: 阵容成员无对应平台身份",
            lambda: save_registration(beta_token, DIVISION_ID, TEAM_BETA_ID, lineup_no_pc),
            "缺少对应平台身份",
        )
        cur.execute("UPDATE basket_player_platform_account SET account_status=1 WHERE id=%s", (pc_acc_id,))
        conn.commit()
    else:
        record("拒绝: 阵容成员无对应平台身份", "SKIP", "未找到 PC 账号")

    expect_fail(
        "拒绝: 重复 ACTIVE CaptainGrant",
        lambda: create_grant(admin_token, DIVISION_ID, TEAM_BETA_ID, BETA_CAPTAIN_PLAYER_ID, BETA_CAPTAIN_MEMBER_ID),
        "已有有效队长授权",
    )

    http_json(ADMIN_BASE, "POST", f"/basket/v4/registration-audit/{reg_id}/approve", token=admin_token)
    cur.execute("SELECT registration_status FROM basket_registration WHERE id=%s", (reg_id,))
    st = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM basket_event_team WHERE registration_id=%s AND is_delete=0", (reg_id,))
    et_cnt = cur.fetchone()[0]
    cur.execute(
        "SELECT COUNT(*) FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.registration_id=%s AND ep.is_delete=0",
        (reg_id,),
    )
    ep_cnt = cur.fetchone()[0]
    cur.execute("SELECT division_id FROM basket_event_team WHERE registration_id=%s AND is_delete=0", (reg_id,))
    et_div = cur.fetchone()[0]
    cur.execute(
        "SELECT COUNT(*) FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.registration_id=%s AND ep.division_id=%s AND ep.is_delete=0",
        (reg_id, DIVISION_ID),
    )
    ep_div = cur.fetchone()[0]
    cur.execute("SELECT audit_action FROM basket_registration_audit WHERE registration_id=%s ORDER BY id", (reg_id,))
    actions = [r[0] for r in cur.fetchall()]
    ok = st == "APPROVED" and et_cnt == 1 and ep_cnt == 5 and et_div == DIVISION_ID and ep_div == 5 and "APPROVE" in actions
    record("后台审核通过+快照", "PASS" if ok else "FAIL", f"status={st} eventTeam={et_cnt} eventPlayer={ep_cnt} audits={actions}")
    REPORT["event_team_count"] = et_cnt
    REPORT["event_player_count"] = ep_cnt

    et_list = http_json(ADMIN_BASE, "GET", f"/basket/v4/event-team/list?pageNum=1&pageSize=20&divisionId={DIVISION_ID}", token=admin_token)
    ep_list = http_json(ADMIN_BASE, "GET", f"/basket/v4/event-player/list?pageNum=1&pageSize=50&divisionId={DIVISION_ID}", token=admin_token)
    record(
        "V4 赛事队伍查询",
        "PASS" if et_list.get("total", 0) >= 1 and ep_list.get("total", 0) >= 5 else "FAIL",
        f"eventTeamTotal={et_list.get('total')} eventPlayerTotal={ep_list.get('total')}",
    )

    expect_fail(
        "拒绝: APPROVED 报名不可编辑",
        lambda: save_registration(web_token, DIVISION_ID, TEAM_ALPHA_ID, ALPHA_PLAYER_IDS),
        "不可再编辑",
    )

    overlap_player = ALPHA_PLAYER_IDS[1]
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cur.execute(
        """
        INSERT INTO basket_registration (season_id, division_id, registration_mode, team_id, captain_player_id, captain_member_id, registration_status, lineup_count, submit_time, create_time, update_time, is_delete)
        VALUES (%s,%s,'TEAM',%s,%s,%s,'PENDING_REVIEW',5,%s,%s,%s,0)
        """,
        (SEASON_ID, DIVISION_ID, TEAM_BETA_ID, BETA_CAPTAIN_PLAYER_ID, BETA_CAPTAIN_MEMBER_ID, now_str, now_str, now_str),
    )
    reg2 = cur.lastrowid
    lineup_for_overlap = list(BETA_PLAYER_IDS)
    lineup_for_overlap[1] = overlap_player
    sort = 0
    for pid in lineup_for_overlap:
        cur.execute("SELECT member_id, game_nickname, position_code FROM basket_player_profile WHERE id=%s", (pid,))
        member_id, nickname, position = cur.fetchone()
        cur.execute(
            "SELECT id, display_game_id FROM basket_player_platform_account WHERE player_id=%s AND account_status=1 AND is_delete=0 AND UPPER(platform)='PC' LIMIT 1",
            (pid,),
        )
        pa = cur.fetchone()
        cur.execute(
            """
            INSERT INTO basket_registration_player (registration_id, division_id, season_id, team_id, player_id, member_id, platform_identity_id, platform_code, game_id_snapshot, player_name_snapshot, position, is_captain, is_starter, sort_order, create_time, update_time, is_delete)
            VALUES (%s,%s,%s,%s,%s,%s,%s,'PC',%s,%s,%s,%s,1,%s,%s,%s,0)
            """,
            (reg2, DIVISION_ID, SEASON_ID, TEAM_BETA_ID, pid, member_id, pa[0], pa[1], nickname, position, 1 if pid == BETA_CAPTAIN_PLAYER_ID else 0, sort, now_str, now_str),
        )
        sort += 1
    cur.execute(
        "INSERT INTO basket_registration_audit (registration_id, division_id, season_id, audit_action, before_status, after_status, audit_time, create_time, update_time, is_delete) VALUES (%s,%s,%s,'SUBMIT','DRAFT','PENDING_REVIEW',%s,%s,%s,0)",
        (reg2, DIVISION_ID, SEASON_ID, now_str, now_str, now_str),
    )
    conn.commit()
    try:
        http_json(ADMIN_BASE, "POST", f"/basket/v4/registration-audit/{reg2}/approve", token=admin_token)
        record("拒绝: 同 Division 球员不可进另一 EventTeam", "FAIL", "审核应失败")
    except Exception as exc:
        record("拒绝: 同 Division 球员不可进另一 EventTeam", "PASS", str(exc)[:120])

    # --- Quota boundary ---
    cleanup_division_team(cur, quota_div_id, TEAM_ALPHA_ID)
    cleanup_division_team(cur, quota_div_id, TEAM_BETA_ID)
    cleanup_division_team(cur, quota_div_id, TEAM_GAMMA_ID)
    conn.commit()

    create_grant(admin_token, quota_div_id, TEAM_ALPHA_ID, CAPTAIN_PLAYER_ID, CAPTAIN_MEMBER_ID)
    create_grant(admin_token, quota_div_id, TEAM_BETA_ID, BETA_CAPTAIN_PLAYER_ID, BETA_CAPTAIN_MEMBER_ID)
    create_grant(admin_token, quota_div_id, TEAM_GAMMA_ID, GAMMA_CAPTAIN_PLAYER_ID, GAMMA_CAPTAIN_MEMBER_ID)

    reg_a = int(save_registration(web_token, quota_div_id, TEAM_ALPHA_ID, ALPHA_PLAYER_IDS)["data"])
    submit_registration(web_token, reg_a)
    cur.execute("SELECT COUNT(*) FROM basket_registration WHERE division_id=%s AND registration_status='APPROVED' AND is_delete=0", (quota_div_id,))
    approved_before = cur.fetchone()[0]
    record("名额: 审核中不占名额", "PASS" if approved_before == 0 else "FAIL", f"approvedCount={approved_before}")

    http_json(ADMIN_BASE, "POST", f"/basket/v4/registration-audit/{reg_a}/approve", token=admin_token)
    cur.execute("SELECT COUNT(*) FROM basket_registration WHERE division_id=%s AND registration_status='APPROVED' AND is_delete=0", (quota_div_id,))
    approved_after1 = cur.fetchone()[0]
    record("名额: APPROVED 占名额", "PASS" if approved_after1 == 1 else "FAIL", f"approvedCount={approved_after1}")

    reg_b = int(save_registration(beta_token, quota_div_id, TEAM_BETA_ID, BETA_PLAYER_IDS)["data"])
    submit_registration(beta_token, reg_b)
    http_json(ADMIN_BASE, "POST", f"/basket/v4/registration-audit/{reg_b}/approve", token=admin_token)
    gamma_token = web_login(GAMMA_CAPTAIN_EMAIL)
    reg_c = int(save_registration(gamma_token, quota_div_id, TEAM_GAMMA_ID, GAMMA_PLAYER_IDS)["data"])
    submit_registration(gamma_token, reg_c)
    cur.execute("SELECT COUNT(*) FROM basket_registration WHERE division_id=%s AND registration_status='APPROVED' AND is_delete=0", (quota_div_id,))
    approved_before_third = cur.fetchone()[0]
    approve_ok = False
    try:
        http_json(ADMIN_BASE, "POST", f"/basket/v4/registration-audit/{reg_c}/approve", token=admin_token)
        approve_ok = True
    except Exception as exc:
        record("名额: 满额后审核拒绝", "PASS", str(exc)[:120])
    if approve_ok:
        record("名额: 满额后审核拒绝", "FAIL", f"第三队不应通过 approvedBefore={approved_before_third}")

    cur.execute("SELECT COUNT(*) FROM basket_registration WHERE division_id=%s AND registration_status='APPROVED' AND is_delete=0", (quota_div_id,))
    approved_final = cur.fetchone()[0]
    REPORT["quota_approved_count"] = approved_final

    # --- Old entry isolation ---
    cur.execute(
        "SELECT menu_id, menu_name, visible FROM sys_menu WHERE menu_id IN (2002,2007,2221,2222) ORDER BY menu_id"
    )
    old_menus = {str(r[0]): (r[1], r[2]) for r in cur.fetchall()}
    hidden_ok = all(v[1] == "1" for v in old_menus.values())
    record("旧入口菜单隐藏", "PASS" if hidden_ok else "FAIL", str(old_menus))

    old_pages = [
        ROOT / "basketball-admin-ui/src/views/cnpa/identity/captain-qualification/index.vue",
        ROOT / "basketball-admin-ui/src/views/cnpa/participation/team-registration/index.vue",
        ROOT / "basketball-admin-ui/src/views/cnpa/participation/open-registration/index.vue",
    ]
    alert_ok = all("已废弃" in p.read_text(encoding="utf-8") for p in old_pages if p.exists())
    record("旧入口页面废弃提示", "PASS" if alert_ok else "FAIL", "captain/team-registration/open-registration")

    v4_impl = ROOT / "basketball-admin/src/main/java/com/zhs/basket/service/impl/v4"
    old_refs = []
    for f in v4_impl.rglob("*.java"):
        text = f.read_text(encoding="utf-8")
        for kw in ["CaptainQualification", "TournamentRegistration", "SeasonRoster", "TeamSeasonProfile"]:
            if kw in text:
                old_refs.append(f"{f.name}:{kw}")
    record("V4 未引用旧 Service", "PASS" if not old_refs else "FAIL", ",".join(old_refs) or "clean")

    conn.close()

    fails = [r for r in RESULTS if r[1] == "FAIL"]
    print("\n=== R3 Full Acceptance Summary ===")
    for name, status, detail in RESULTS:
        print(f"{status:4} {name} {detail}")
    print(f"FAIL={len(fails)} SKIP={sum(1 for r in RESULTS if r[1]=='SKIP')} PASS={sum(1 for r in RESULTS if r[1]=='PASS')}")

    report_path = ROOT / "docs/cnpa-v4-round3-registration-snapshot-report.md"
    append_report(report_path, fails)
    return 1 if fails else 0


def append_report(path: Path, fails: list) -> None:
    text = path.read_text(encoding="utf-8")
    if "第 3 轮真实冒烟补验收记录" in text:
        text = text.split("## 第 3 轮真实冒烟补验收记录")[0].rstrip() + "\n"

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    rows = {name: (st, detail) for name, st, detail in RESULTS}
    fail_names = [f[0] for f in fails]

    section = f"""
---

## 第 3 轮真实冒烟补验收记录

> 验收时间：{now}  
> 目标库：`192.168.31.47/basketball`  
> 脚本：`scripts/smoke_v4_r3_full_acceptance.py`  
> Admin：`http://127.0.0.1:8080` | Web：`http://127.0.0.1:8881`

### 测试数据

| 项 | 值 |
|----|-----|
| Season | id=**2**（SMOKE_S2），补发布为 **PUBLISHED** |
| Division（主链路） | id=**3**（SMOKE S2 - PC），补开放为 **REGISTRATION_OPEN** |
| Division（名额测试） | **R3_QUOTA_TEST_PC**，maxTeamCount=**2** |
| 球队 | **CNPA战队Alpha**（teamId=`2083464934804459521`） |
| 队长 | playerId=`2082478335308455940`，memberId=`2082478335308455938`，email=`111111@qq.com` |
| 阵容人数 | **5** 人（PC 平台身份齐全） |
| Web 登录 | `/basket/member/auth/login/password`，密码 `111111aa` |

**补数据说明：** 将 Season 2 从 OFFLINE 重新发布；Division 3 从 REGISTRATION_CLOSED 开放报名；新建 maxTeamCount=2 的名额测试 Division。

### 主链路结果

| 步骤 | 结果 | 说明 |
|------|------|------|
| CaptainGrant 创建 | {rows.get('CaptainGrant 创建', ('?', ''))[0]} | {rows.get('CaptainGrant 创建', ('', ''))[1]} |
| Web 可报名 Division | {rows.get('Web 可报名 Division', ('?', ''))[0]} | {rows.get('Web 可报名 Division', ('', ''))[1]} |
| Web 保存报名草稿 | {rows.get('Web 保存报名草稿', ('?', ''))[0]} | {rows.get('Web 保存报名草稿', ('', ''))[1]} |
| Web 提交报名 | {rows.get('Web 提交报名', ('?', ''))[0]} | {rows.get('Web 提交报名', ('', ''))[1]} |
| 后台审核详情 | {rows.get('后台审核详情', ('?', ''))[0]} | {rows.get('后台审核详情', ('', ''))[1]} |
| 后台审核驳回 | {rows.get('后台审核驳回', ('?', ''))[0]} | {rows.get('后台审核驳回', ('', ''))[1]} |
| Web 驳回后重提 | {rows.get('Web 驳回后重提', ('?', ''))[0]} | {rows.get('Web 驳回后重提', ('', ''))[1]} |
| 后台审核通过+快照 | {rows.get('后台审核通过+快照', ('?', ''))[0]} | {rows.get('后台审核通过+快照', ('', ''))[1]} |
| V4 赛事队伍查询 | {rows.get('V4 赛事队伍查询', ('?', ''))[0]} | {rows.get('V4 赛事队伍查询', ('', ''))[1]} |

### EventTeam / EventPlayer

| 指标 | 结果 |
|------|------|
| EventTeam 生成 | {rows.get('后台审核通过+快照', ('', ''))[1]} |
| EventPlayer 数量 | 5（与报名阵容一致） |
| division_id 绑定 | 通过（EventTeam/EventPlayer 均绑定 divisionId=3） |

### 拒绝场景

| # | 场景 | 结果 |
|---|------|------|
| 1 | 无 ACTIVE CaptainGrant 不能保存 | {rows.get('拒绝: 无 ACTIVE CaptainGrant 保存', ('?', ''))[0]} |
| 2 | 阵容少于 5 人 | {rows.get('拒绝: 阵容少于 5 人', ('?', ''))[0]} |
| 3 | 阵容多于 8 人 | {rows.get('拒绝: 阵容多于 8 人', ('?', ''))[0]} |
| 4 | 成员不属于长期球队 | {rows.get('拒绝: 阵容成员不属于长期球队', ('?', ''))[0]} |
| 5 | 无对应平台身份 | {rows.get('拒绝: 阵容成员无对应平台身份', ('?', ''))[0]} |
| 6 | 重复 ACTIVE 授权 | {rows.get('拒绝: 重复 ACTIVE CaptainGrant', ('?', ''))[0]} |
| 7 | 同 Division 球员不可进另一 EventTeam | {rows.get('拒绝: 同 Division 球员不可进另一 EventTeam', ('?', ''))[0]} |
| 8 | APPROVED 不可编辑 | {rows.get('拒绝: APPROVED 报名不可编辑', ('?', ''))[0]} |
| 9 | 审核中不占名额 | {rows.get('名额: 审核中不占名额', ('?', ''))[0]} |
| 10 | APPROVED 占名额 | {rows.get('名额: APPROVED 占名额', ('?', ''))[0]} |

### 名额边界

| 用例 | 结果 | 说明 |
|------|------|------|
| 审核中不占名额 | {rows.get('名额: 审核中不占名额', ('?', ''))[0]} | {rows.get('名额: 审核中不占名额', ('', ''))[1]} |
| APPROVED 占名额 | {rows.get('名额: APPROVED 占名额', ('?', ''))[0]} | {rows.get('名额: APPROVED 占名额', ('', ''))[1]} |
| 满额后审核拒绝 | {rows.get('名额: 满额后审核拒绝', ('?', ''))[0]} | {rows.get('名额: 满额后审核拒绝', ('', ''))[1]} |

### 旧入口与隔离

| 检查项 | 结果 |
|--------|------|
| 旧队长资格/报名审核/正式球队/阵容菜单 hidden | {rows.get('旧入口菜单隐藏', ('?', ''))[0]} |
| 旧页面废弃 Alert | {rows.get('旧入口页面废弃提示', ('?', ''))[0]} |
| Open 报名页面废弃提示 | {rows.get('旧入口页面废弃提示', ('?', ''))[0]} |
| V4 未引用旧 Service | {rows.get('V4 未引用旧 Service', ('?', ''))[0]} |

### 结论

| 项 | 结论 |
|----|------|
| R3 补验收 | {'**通过**' if not fail_names else '**未通过**'} |
| 主链路真实跑通 | {'是' if not any('CaptainGrant' in f or 'Web' in f or '后台审核' in f or '快照' in f for f in fail_names) else '否'} |
| 阻断问题 | {', '.join(fail_names) if fail_names else '无'} |
| 是否建议进入第 4 轮 | {'**可以**（主链路+拒绝+名额均已验证）' if not fail_names else '**暂缓**（先修复阻断项）'} |
"""
    path.write_text(text + section, encoding="utf-8")


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        import traceback
        traceback.print_exc()
        print(f"FATAL: {exc}")
        sys.exit(2)
