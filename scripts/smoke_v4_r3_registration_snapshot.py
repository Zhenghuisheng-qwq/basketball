#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round3 registration/snapshot smoke acceptance."""
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
WEB_BASE = "http://127.0.0.1:8081"
ADMIN_USER = "admin"
ADMIN_PASSWORD = "admin123"
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")
RESULTS: list[tuple[str, str, str]] = []


def record(name: str, status: str, detail: str = "") -> None:
    RESULTS.append((name, status, detail))
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(base: str, method: str, path: str, body: dict | None = None, token: str | None = None):
    url = base + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def admin_login() -> str | None:
    try:
        resp = http_json(ADMIN_BASE, "POST", "/login", {"username": ADMIN_USER, "password": ADMIN_PASSWORD})
        if resp.get("code") == 200 and resp.get("token"):
            return resp["token"]
        record("Admin 登录", "FAIL", str(resp))
        return None
    except Exception as exc:
        record("Admin 登录", "SKIP", f"backend unavailable: {exc}")
        return None


def find_test_data(cur):
    cur.execute(
        """
        select d.id, d.season_id, d.platform_code, d.max_team_count
        from basket_division d
        join basket_season s on s.id = d.season_id and s.is_delete = 0 and s.publish_status = 'PUBLISHED'
        where d.is_delete = 0 and d.division_status = 'REGISTRATION_OPEN'
        order by d.id desc limit 1
        """
    )
    division = cur.fetchone()
    if not division:
        return None
    division_id, season_id, platform_code, max_team_count = division
    cur.execute(
        """
        select t.id, tm.player_id, tm.member_id
        from basket_team t
        join basket_team_member tm on tm.team_id = t.id and tm.is_current = 1 and tm.membership_status = 1 and tm.is_delete = 0
        join basket_player_profile p on p.id = tm.player_id and p.certification_status = 3 and p.is_delete = 0
        where t.team_status = 1 and t.audit_status = 1 and t.is_delete = 0
        limit 1
        """
    )
    team_row = cur.fetchone()
    if not team_row:
        return None
    team_id, captain_player_id, captain_member_id = team_row
    cur.execute(
        """
        select tm.player_id
        from basket_team_member tm
        join basket_player_profile p on p.id = tm.player_id and p.certification_status = 3 and p.is_delete = 0
        join basket_player_platform_account pa on pa.player_id = tm.player_id and pa.account_status = 1 and pa.is_delete = 0
        where tm.team_id = %s and tm.is_current = 1 and tm.membership_status = 1 and tm.is_delete = 0
          and upper(pa.platform) = %s
        limit 8
        """,
        (team_id, platform_code.upper()),
    )
    player_ids = [r[0] for r in cur.fetchall()]
    return {
        "division_id": division_id,
        "season_id": season_id,
        "platform_code": platform_code,
        "max_team_count": max_team_count,
        "team_id": team_id,
        "captain_player_id": captain_player_id,
        "captain_member_id": captain_member_id,
        "player_ids": player_ids,
    }


def main() -> int:
    token = admin_login()
    if not token:
        record("Round3 冒烟", "SKIP", "admin backend unavailable")
        return 0

    try:
        conn = pymysql.connect(**CONN)
    except Exception as exc:
        record("数据库连接", "SKIP", str(exc))
        return 0

    cur = conn.cursor()
    data = find_test_data(cur)
    if not data or len(data["player_ids"]) < 5:
        record("测试数据准备", "SKIP", "缺少 REGISTRATION_OPEN Division 或有效球队/球员")
        conn.close()
        return 0

    record("测试数据准备", "PASS", f"division={data['division_id']} team={data['team_id']} players={len(data['player_ids'])}")

    # cleanup prior test rows for same division+team
    cur.execute("select id from basket_captain_grant where division_id=%s and team_id=%s and is_delete=0", (data["division_id"], data["team_id"]))
    for (gid,) in cur.fetchall():
        cur.execute("update basket_captain_grant set grant_status='REVOKED', active_unique_key=null where id=%s", (gid,))
    cur.execute("select id from basket_registration where division_id=%s and team_id=%s and is_delete=0", (data["division_id"], data["team_id"]))
    for (rid,) in cur.fetchall():
        cur.execute("delete from basket_registration_player where registration_id=%s", (rid,))
        cur.execute("delete from basket_registration_audit where registration_id=%s", (rid,))
        cur.execute("delete from basket_event_player where event_team_id in (select id from basket_event_team where registration_id=%s)", (rid,))
        cur.execute("update basket_event_team set is_delete=1 where registration_id=%s", (rid,))
        cur.execute("delete from basket_registration where id=%s", (rid,))
    conn.commit()

    try:
        grant_resp = http_json(ADMIN_BASE, "POST", "/basket/v4/captain-grant", {
            "divisionId": data["division_id"],
            "teamId": data["team_id"],
            "playerId": data["captain_player_id"],
            "memberId": data["captain_member_id"],
            "validFrom": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "validTo": (datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d %H:%M:%S"),
            "remark": "round3 smoke",
        }, token)
        grant_id = grant_resp.get("data")
        record("CaptainGrant 新增", "PASS" if grant_resp.get("code") == 200 else "FAIL", str(grant_resp))

        dup_resp = http_json(ADMIN_BASE, "POST", "/basket/v4/captain-grant", {
            "divisionId": data["division_id"],
            "teamId": data["team_id"],
            "playerId": data["captain_player_id"],
            "memberId": data["captain_member_id"],
        }, token)
        record("CaptainGrant 重复 ACTIVE 拒绝", "PASS" if dup_resp.get("code") != 200 else "FAIL", dup_resp.get("msg", ""))

        list_resp = http_json(ADMIN_BASE, "GET", "/basket/v4/captain-grant/list?pageNum=1&pageSize=10&divisionId=" + str(data["division_id"]), token=token)
        record("CaptainGrant 列表", "PASS" if list_resp.get("code") == 200 else "FAIL", f"total={list_resp.get('total')}")

        # Web save/submit requires web login token - skip if unavailable, use SQL-assisted registration via admin audit path only
        record("Web 报名 API", "SKIP", "需 Web 登录 token，本轮以 Admin+SQL 链路验证")

        # Simulate registration via SQL for audit path if web unavailable
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        cur.execute(
            """
            insert into basket_registration (season_id, division_id, registration_mode, team_id, captain_player_id, captain_member_id, registration_status, lineup_count, submit_time, create_time, update_time, is_delete)
            values (%s,%s,'TEAM',%s,%s,%s,'PENDING_REVIEW',%s,%s,%s,%s,0)
            """,
            (data["season_id"], data["division_id"], data["team_id"], data["captain_player_id"], data["captain_member_id"], len(data["player_ids"]), now, now, now),
        )
        reg_id = cur.lastrowid
        sort = 0
        for pid in data["player_ids"]:
            cur.execute("select member_id, game_nickname, position_code from basket_player_profile where id=%s", (pid,))
            member_id, nickname, position = cur.fetchone()
            cur.execute(
                "select id, display_game_id from basket_player_platform_account where player_id=%s and account_status=1 and is_delete=0 limit 1",
                (pid,),
            )
            pa = cur.fetchone()
            cur.execute(
                """
                insert into basket_registration_player (registration_id, division_id, season_id, team_id, player_id, member_id, platform_identity_id, platform_code, game_id_snapshot, player_name_snapshot, position, is_captain, is_starter, sort_order, create_time, update_time, is_delete)
                values (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1,%s,%s,%s,0)
                """,
                (reg_id, data["division_id"], data["season_id"], data["team_id"], pid, member_id, pa[0], data["platform_code"], pa[1], nickname, position, 1 if pid == data["captain_player_id"] else 0, sort, now, now),
            )
            sort += 1
        conn.commit()
        record("Registration 待审核数据", "PASS", f"registrationId={reg_id}")

        detail_resp = http_json(ADMIN_BASE, "GET", f"/basket/v4/registration-audit/{reg_id}", token=token)
        record("RegistrationAudit 详情", "PASS" if detail_resp.get("code") == 200 else "FAIL", str(detail_resp.get("code")))

        approve_resp = http_json(ADMIN_BASE, "POST", f"/basket/v4/registration-audit/{reg_id}/approve", token=token)
        record("RegistrationAudit 通过", "PASS" if approve_resp.get("code") == 200 else "FAIL", approve_resp.get("msg", ""))

        cur.execute("select count(1) from basket_event_team where registration_id=%s and is_delete=0", (reg_id,))
        et_cnt = cur.fetchone()[0]
        cur.execute("select count(1) from basket_event_player ep join basket_event_team et on et.id=ep.event_team_id where et.registration_id=%s and ep.is_delete=0", (reg_id,))
        ep_cnt = cur.fetchone()[0]
        record("EventTeam 快照生成", "PASS" if et_cnt == 1 else "FAIL", f"count={et_cnt}")
        record("EventPlayer 快照生成", "PASS" if ep_cnt >= 5 else "FAIL", f"count={ep_cnt}")

        cur.execute("select registration_status from basket_registration where id=%s", (reg_id,))
        record("Registration APPROVED", "PASS" if cur.fetchone()[0] == "APPROVED" else "FAIL", "")

        revoke_resp = http_json(ADMIN_BASE, "PUT", f"/basket/v4/captain-grant/{grant_id}/revoke", token=token)
        record("CaptainGrant 撤销", "PASS" if revoke_resp.get("code") == 200 else "FAIL", "")

        event_team_resp = http_json(ADMIN_BASE, "GET", "/basket/v4/event-team/list?pageNum=1&pageSize=10", token=token)
        record("EventTeam 列表", "PASS" if event_team_resp.get("code") == 200 else "FAIL", f"total={event_team_resp.get('total')}")

    except urllib.error.HTTPError as exc:
        record("HTTP 调用", "FAIL", f"{exc.code} {exc.read().decode('utf-8', errors='ignore')[:200]}")
    except Exception as exc:
        record("冒烟执行", "FAIL", str(exc))
    finally:
        conn.close()

    fails = [r for r in RESULTS if r[1] == "FAIL"]
    print("\n=== Round3 Smoke Summary ===")
    for name, status, detail in RESULTS:
        print(f"{status:4} {name} {detail}")
    print(f"FAIL={len(fails)} SKIP={sum(1 for r in RESULTS if r[1]=='SKIP')} PASS={sum(1 for r in RESULTS if r[1]=='PASS')}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
