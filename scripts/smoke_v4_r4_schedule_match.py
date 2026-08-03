#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round4 smoke: ScheduleBatch generate -> publish -> Match list."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from pathlib import Path

import pymysql

ADMIN_BASE = "http://127.0.0.1:8080"
ADMIN_USER = "admin"
ADMIN_PASSWORD = "admin123"
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")

DIVISION_ID = 5  # R3_QUOTA_TEST_PC, 2 active EventTeams
RESULTS: list[tuple[str, str, str]] = []


def record(name: str, status: str, detail: str = "") -> None:
    RESULTS.append((name, status, detail))
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(method: str, path: str, body: dict | None = None, token: str | None = None, allow_fail=False):
    url = ADMIN_BASE + path
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


def http_expect_fail(method: str, path: str, body: dict | None = None, token: str | None = None) -> bool:
    payload = http_json(method, path, body, token, allow_fail=True)
    return payload.get("code") not in (200, None)


def admin_login() -> str:
    resp = http_json("POST", "/login", {"username": ADMIN_USER, "password": ADMIN_PASSWORD})
    token = resp.get("token")
    if not token:
        raise RuntimeError(f"admin login failed: {resp}")
    return token


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


def prepare_division(token: str) -> None:
    rows = db_exec(
        "SELECT division_status, format_type, match_start_time FROM basket_division WHERE id=%s",
        (DIVISION_ID,),
    )
    if not rows:
        raise RuntimeError(f"division {DIVISION_ID} not found")
    status, fmt, mst = rows[0]
    team_cnt = db_exec(
        "SELECT COUNT(*) FROM basket_event_team WHERE division_id=%s AND event_team_status='ACTIVE' AND is_delete=0",
        (DIVISION_ID,),
    )[0][0]
    record("前置: EventTeam 数量", "PASS" if team_cnt >= 2 else "FAIL", f"count={team_cnt}")

    updates = []
    if status not in ("REGISTRATION_CLOSED", "SCHEDULED"):
        updates.append(("division_status", "REGISTRATION_CLOSED"))
    if not mst:
        updates.append(("match_start_time", datetime.now() + timedelta(days=3)))
    if fmt != "REGULAR_ROUND_ROBIN":
        updates.append(("format_type", "REGULAR_ROUND_ROBIN"))

    if updates:
        sets = ", ".join(f"{k}=%s" for k, _ in updates)
        db_exec(f"UPDATE basket_division SET {sets}, update_time=NOW() WHERE id=%s",
                [v for _, v in updates] + [DIVISION_ID])
        record("前置: Division 状态", "PASS", "已调整为 REGISTRATION_CLOSED")
    else:
        record("前置: Division 状态", "PASS", status)

    # cleanup existing draft batches for idempotent rerun
    drafts = db_exec(
        "SELECT id FROM basket_schedule_batch WHERE division_id=%s AND publish_status='DRAFT' AND is_delete=0",
        (DIVISION_ID,),
    )
    for (bid,) in drafts:
        http_json("PUT", f"/basket/v4/schedule-batch/{bid}/cancel", token=token)
    pub = db_exec(
        "SELECT id FROM basket_schedule_batch WHERE division_id=%s AND publish_status='PUBLISHED' AND is_delete=0",
        (DIVISION_ID,),
    )
    if pub:
        record("前置: 清理已发布批次", "SKIP", "存在 PUBLISHED 批次，跳过生成测试")
        return
    record("前置: 草稿清理", "PASS", f"cancelled {len(drafts)} draft(s)")


def verify_v4_tables() -> None:
    cols = [r[0] for r in db_exec(
        "SELECT column_name FROM information_schema.columns "
        "WHERE table_schema='basketball' AND table_name='basket_match'"
    )]
    required = {"division_id", "season_id", "schedule_batch_id", "home_event_team_id",
                "away_event_team_id", "match_status", "publish_status"}
    missing = required - set(cols)
    if missing:
        record("DB: V4 basket_match 字段", "FAIL", str(missing))
    else:
        record("DB: V4 basket_match 字段", "PASS", "OK")
    for tbl in ("bak_r4_basket_match", "bak_r4_basket_match_period_score",
                "bak_r4_basket_match_team_stat", "bak_r4_basket_match_player_stat"):
        cnt = db_exec(f"SELECT COUNT(*) FROM {tbl}")[0][0]
        record(f"DB: 备份 {tbl}", "PASS" if cnt >= 0 else "FAIL", f"rows={cnt}")


def run_api_smoke(token: str) -> int:
    pub = db_exec(
        "SELECT id FROM basket_schedule_batch WHERE division_id=%s AND publish_status='PUBLISHED' AND is_delete=0",
        (DIVISION_ID,),
    )
    if pub:
        batch_id = pub[0][0]
        record("生成草稿", "SKIP", "已有 PUBLISHED 批次")
    else:
        # reject <2 teams
        if http_expect_fail("POST", "/basket/v4/schedule-batch/generate",
                            {"divisionId": 999999, "remark": "bad"}, token):
            record("拒绝无效 Division", "PASS", "返回业务错误")
        else:
            record("拒绝无效 Division", "FAIL", "应失败但未失败")

        resp = http_json("POST", "/basket/v4/schedule-batch/generate",
                         {"divisionId": DIVISION_ID, "remark": "R4 smoke"}, token=token)
        batch_id = resp.get("data")
        record("生成草稿", "PASS", f"batchId={batch_id}")

    detail = http_json("GET", f"/basket/v4/schedule-batch/{batch_id}", token=token)["data"]
    team_count = detail.get("teamCount", 0)
    match_count = detail.get("matchCount", 0)
    expected = team_count * (team_count - 1) // 2
    record("比赛场数 N*(N-1)/2", "PASS" if match_count == expected else "FAIL",
           f"teams={team_count} matches={match_count} expected={expected}")

    matches = detail.get("matches") or []
    if matches:
        mt = matches[0].get("matchTime", "")
        record("时间槽样例", "PASS", mt)
        hours = {m.get("matchTime", "")[11:13] for m in matches if m.get("matchTime")}
        record("时间槽小时", "PASS" if hours <= {"21", "22"} else "FAIL", str(hours))

    if detail.get("publishStatus") == "DRAFT":
        http_json("PUT", f"/basket/v4/schedule-batch/{batch_id}/publish", token=token)
        record("发布赛程", "PASS", f"batchId={batch_id}")
    else:
        record("发布赛程", "SKIP", "已发布")

    div_status = db_exec("SELECT division_status FROM basket_division WHERE id=%s", (DIVISION_ID,))[0][0]
    record("Division→SCHEDULED", "PASS" if div_status == "SCHEDULED" else "FAIL", div_status)

    pub_cnt = db_exec(
        "SELECT COUNT(*) FROM basket_match WHERE schedule_batch_id=%s AND publish_status='PUBLISHED' AND is_delete=0",
        (batch_id,),
    )[0][0]
    record("Match publishStatus", "PASS" if pub_cnt == match_count else "FAIL", f"published={pub_cnt}/{match_count}")

    lst = http_json("GET", f"/basket/v4/match/list?divisionId={DIVISION_ID}&pageNum=1&pageSize=50", token=token)
    record("Match 列表", "PASS", f"total={lst.get('total', 0)}")

    # duplicate generate should fail
    if http_expect_fail("POST", "/basket/v4/schedule-batch/generate", {"divisionId": DIVISION_ID}, token):
        record("拒绝重复生成(PUBLISHED)", "PASS", "OK")
    else:
        record("拒绝重复生成(PUBLISHED)", "FAIL", "应失败")

    return match_count


def main() -> int:
    print("=== CNPA V4 Round4 Schedule/Match Smoke ===")
    verify_v4_tables()
    token = admin_login()
    record("Admin 登录", "PASS", "OK")
    prepare_division(token)
    match_count = run_api_smoke(token)
    fails = sum(1 for _, s, _ in RESULTS if s == "FAIL")
    print(f"\nTotal: {len(RESULTS)}, FAIL: {fails}, Match count: {match_count}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
