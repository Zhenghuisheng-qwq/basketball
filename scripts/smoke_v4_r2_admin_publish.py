#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round2 admin publish smoke acceptance."""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
ADMIN_BASE = "http://127.0.0.1:8080"
ADMIN_USER = "admin"
ADMIN_PASSWORD = "admin123"
CONN = dict(
    host="192.168.31.47",
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
)

RESULTS: list[tuple[str, str, str]] = []


def record(name: str, status: str, detail: str = "") -> None:
    RESULTS.append((name, status, detail))
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(method: str, path: str, body: dict | None = None, token: str | None = None):
    url = ADMIN_BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def admin_login() -> str | None:
    try:
        resp = http_json("POST", "/login", {"username": ADMIN_USER, "password": ADMIN_PASSWORD})
        if resp.get("code") == 200 and resp.get("token"):
            return resp["token"]
        record("Admin 登录", "FAIL", str(resp))
        return None
    except Exception as exc:
        record("Admin 登录", "SKIP", f"backend unavailable: {exc}")
        return None


def assert_ok(resp: dict, msg: str) -> None:
    if resp.get("code") != 200:
        raise RuntimeError(f"{msg}: {resp}")


def verify_sql_state(cur) -> None:
    cur.execute("SELECT COUNT(*) FROM basket_series WHERE is_delete=0")
    series_cnt = cur.fetchone()[0]
    record("basket_series 存在", "PASS" if series_cnt >= 4 else "FAIL", f"count={series_cnt}")

    cur.execute(
        "SELECT series_code FROM basket_series WHERE is_delete=0 ORDER BY sort_order, id"
    )
    codes = {r[0] for r in cur.fetchall()}
    expected = {"LEAGUE", "ARENA", "CUP", "OPEN"}
    record(
        "Series 四条种子",
        "PASS" if expected.issubset(codes) else "FAIL",
        ",".join(sorted(codes)),
    )

    cur.execute(
        "SELECT id, display_title, publish_status FROM basket_season WHERE is_delete=0 ORDER BY id"
    )
    seasons = cur.fetchall()
    record("basket_season 示例/数据", "PASS" if seasons else "FAIL", str(seasons))

    cur.execute(
        "SELECT id, platform_code, division_name, division_status FROM basket_division WHERE is_delete=0 ORDER BY id"
    )
    divisions = cur.fetchall()
    record("basket_division 示例/数据", "PASS" if divisions else "FAIL", str(divisions))

    cur.execute(
        "SELECT menu_id, menu_name, component, visible FROM sys_menu WHERE menu_id=2280"
    )
    menu = cur.fetchone()
    record(
        "V4 菜单 2280",
        "PASS" if menu and menu[2] == "cnpa/event-v4/publish/index" else "FAIL",
        str(menu),
    )

    cur.execute(
        "SELECT menu_id, visible FROM sys_menu WHERE menu_id IN (2004,2005,2006,2038,2039) ORDER BY menu_id"
    )
    old = cur.fetchall()
    hidden = all(v == "1" for _, v in old)
    record("旧发布菜单隐藏", "PASS" if hidden else "FAIL", str(old))

    for tbl in [
        "basket_match",
        "basket_match_period_score",
        "basket_match_team_stat",
        "basket_match_player_stat",
    ]:
        cur.execute(
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=%s AND COLUMN_NAME='division_id'",
            (tbl,),
        )
        has_v4 = cur.fetchone()[0] > 0
        cur.execute(f"SELECT COUNT(*) FROM `{tbl}`")
        row_cnt = cur.fetchone()[0]
        record(
            f"旧同名表 {tbl}",
            "PASS",
            f"exists old-schema={not has_v4}, rows={row_cnt}",
        )


def verify_deprecated_ui() -> None:
    pages = [
        "basketball-admin-ui/src/views/cnpa/event/list/index.vue",
        "basketball-admin-ui/src/views/cnpa/event/season/index.vue",
        "basketball-admin-ui/src/views/cnpa/event/rule/index.vue",
        "basketball-admin-ui/src/views/cnpa/event/stage/index.vue",
        "basketball-admin-ui/src/views/cnpa/event/group/index.vue",
    ]
    missing = []
    for rel in pages:
        text = (ROOT / rel).read_text(encoding="utf-8")
        if "已废弃" not in text:
            missing.append(rel)
    record(
        "旧页面废弃提示",
        "PASS" if not missing else "FAIL",
        "all ok" if not missing else ",".join(missing),
    )


def run_api_smoke(token: str, cur) -> None:
    cur.execute("SELECT id FROM basket_series WHERE series_code='LEAGUE' AND is_delete=0 LIMIT 1")
    league_id = cur.fetchone()[0]

    resp = http_json("GET", "/basket/v4/series/list", token=token)
    assert_ok(resp, "Series list")
    rows = resp.get("rows") or resp.get("data") or []
    record("查看 Series", "PASS", f"count={len(rows)}")

    now = datetime.now()
    suffix = now.strftime("%H%M%S")
    season_body = {
        "seriesId": league_id,
        "seasonYear": 2026,
        "seasonName": f"SMOKE_{suffix}",
        "displayTitle": f"CNPA League 2026 SMOKE {suffix}",
        "description": "round2 smoke season",
        "registrationMode": "TEAM",
        "formatType": "REGULAR_ROUND_ROBIN",
        "registrationStartTime": (now + timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S"),
        "registrationEndTime": (now + timedelta(days=10)).strftime("%Y-%m-%d %H:%M:%S"),
        "matchStartTime": (now + timedelta(days=11)).strftime("%Y-%m-%d %H:%M:%S"),
        "matchEndTime": (now + timedelta(days=60)).strftime("%Y-%m-%d %H:%M:%S"),
    }
    resp = http_json("POST", "/basket/v4/season", season_body, token)
    assert_ok(resp, "create season")
    season_id = resp["data"]
    record("创建 Season", "PASS", f"id={season_id}")

    edit_body = dict(season_body)
    edit_body["displayTitle"] = f"CNPA League 2026 SMOKE {suffix} edited"
    resp = http_json("PUT", f"/basket/v4/season/{season_id}", edit_body, token)
    assert_ok(resp, "edit season")
    record("编辑 Season", "PASS", f"id={season_id}")

    div_body = {
        "seasonId": season_id,
        "platformCode": "PC",
        "divisionName": f"SMOKE {suffix} - PC",
        "maxTeamCount": 16,
        "lineupMinCount": 5,
        "lineupMaxCount": 8,
    }
    resp = http_json("POST", "/basket/v4/division", div_body, token)
    assert_ok(resp, "create division")
    division_id = resp["data"]
    record("创建 Division", "PASS", f"id={division_id}")

    dup_failed = False
    try:
        dup_resp = http_json("POST", "/basket/v4/division", div_body, token)
        dup_failed = dup_resp.get("code") != 200
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        dup_failed = exc.code == 500 or "平台" in body
    except RuntimeError:
        dup_failed = True
    record("同平台 Division 不可重复", "PASS" if dup_failed else "FAIL")

    bad_personal = False
    try:
        personal_resp = http_json(
            "POST",
            "/basket/v4/season",
            {**season_body, "seasonName": f"SMOKE_BAD_P_{suffix}", "registrationMode": "PERSONAL"},
            token,
        )
        bad_personal = personal_resp.get("code") != 200
    except Exception:
        bad_personal = True
    record("PERSONAL 提交拒绝", "PASS" if bad_personal else "FAIL")

    bad_format = False
    try:
        format_resp = http_json(
            "POST",
            "/basket/v4/season",
            {**season_body, "seasonName": f"SMOKE_BAD_F_{suffix}", "formatType": "KNOCKOUT"},
            token,
        )
        bad_format = format_resp.get("code") != 200
    except Exception:
        bad_format = True
    record("非 REGULAR_ROUND_ROBIN 拒绝", "PASS" if bad_format else "FAIL")

    resp = http_json("PUT", f"/basket/v4/season/{season_id}/publish", token=token)
    assert_ok(resp, "publish season")
    record("发布 Season", "PASS", f"id={season_id}")

    resp = http_json("PUT", f"/basket/v4/division/{division_id}/open-registration", token=token)
    assert_ok(resp, "open registration")
    record("开放 Division 报名", "PASS", f"id={division_id}")

    resp = http_json("PUT", f"/basket/v4/division/{division_id}/close-registration", token=token)
    assert_ok(resp, "close registration")
    record("关闭 Division 报名", "PASS", f"id={division_id}")

    resp = http_json("PUT", f"/basket/v4/season/{season_id}/offline", token=token)
    assert_ok(resp, "offline season")
    record("下线 Season", "PASS", f"id={season_id}")


def wait_backend(timeout_sec: int = 180) -> bool:
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        try:
            urllib.request.urlopen(ADMIN_BASE + "/login", timeout=3)
            return True
        except Exception:
            time.sleep(3)
    return False


def main() -> int:
    conn = pymysql.connect(**CONN)
    cur = conn.cursor()
    try:
        verify_sql_state(cur)
        verify_deprecated_ui()

        if not wait_backend(5):
            record("后台 API 冒烟", "SKIP", "localhost:8080 未启动，仅完成 SQL/静态验收")
            return 0 if all(s != "FAIL" for _, s, _ in RESULTS) else 1

        token = admin_login()
        if not token:
            return 1
        run_api_smoke(token, cur)
        return 0 if all(s != "FAIL" for _, s, _ in RESULTS) else 1
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
