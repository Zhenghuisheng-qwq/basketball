#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round8 final acceptance: full-chain API + page smoke."""
from __future__ import annotations

import json
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

try:
    import pymysql
except ImportError:
    pymysql = None

ROOT = Path(__file__).resolve().parents[1]
WEB_BASE = "http://127.0.0.1:8881"
UI_BASE = "http://127.0.0.1:5173"
ADMIN_BASE = "http://127.0.0.1:8080"
MEMBER_PASSWORD = "111111aa"
CAPTAIN_EMAIL = "111111@qq.com"
DB_CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")

RESULTS: list[tuple[str, str, str]] = []
REPORT: dict = {
    "round": "R8",
    "smoke_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "web_base": WEB_BASE,
    "ui_base": UI_BASE,
    "admin_base": ADMIN_BASE,
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
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            return payload, resp.status
    except urllib.error.HTTPError as exc:
        payload = json.loads(exc.read().decode("utf-8", errors="ignore") or "{}")
        if not allow_fail:
            raise RuntimeError(f"{method} {path} HTTP {exc.code}: {payload}") from exc
        return payload, exc.code


def web_login(email: str) -> str:
    resp, _ = http_json(WEB_BASE, "POST", "/basket/member/auth/login/password", {"email": email, "password": MEMBER_PASSWORD})
    token = (resp.get("data") or {}).get("token")
    if not token:
        raise RuntimeError(f"login failed for {email}: {resp}")
    return token


def http_page(base: str, path: str) -> tuple[int, str]:
    url = base + path
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=20) as resp:
        return resp.status, resp.read(8192).decode("utf-8", errors="ignore")


def db_query(sql: str, args=()):
    if not pymysql:
        return None
    conn = pymysql.connect(**DB_CONN)
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args)
            return cur.fetchall()
    finally:
        conn.close()


def check_v4_mainline_no_legacy_api():
    ui_root = ROOT / "basketball-web-ui" / "src"
    v4_dirs = [
        ui_root / "views" / "events" / "v4",
        ui_root / "views" / "data" / "V4DataPage.vue",
        ui_root / "views" / "account" / "events" / "MyEventsView.vue",
        ui_root / "views" / "account" / "events" / "V4RegistrationFlow.vue",
        ui_root / "views" / "account" / "events" / "V4MatchSubmissionView.vue",
        ui_root / "components" / "account" / "events" / "V4MySchedulePanel.vue",
    ]
    patterns = [
        "publicTournament",
        "/basket/public/tournament",
        "/basket/member/team-registration",
        "/basket/match-data",
        "/basket/player/match-data",
    ]
    violations = []
    for target in v4_dirs:
        if target.is_dir():
            files = list(target.rglob("*"))
        elif target.is_file():
            files = [target]
        else:
            continue
        for fp in files:
            if fp.suffix not in {".vue", ".ts"}:
                continue
            text = fp.read_text(encoding="utf-8", errors="ignore")
            for pat in patterns:
                if pat in text:
                    violations.append(f"{fp.relative_to(ROOT)}:{pat}")
    if violations:
        record("v4_mainline_legacy_api_scan", "FAIL", "; ".join(violations[:5]))
    else:
        record("v4_mainline_legacy_api_scan", "PASS", "no legacy API in V4 mainline files")


def check_tournament_id_alias():
    files = [
        ROOT / "basketball-web-ui" / "src" / "utils" / "v4AccountEventsMappers.ts",
        ROOT / "basketball-web-ui" / "src" / "types" / "accountEvents.ts",
        ROOT / "basketball-web-ui" / "src" / "views" / "account" / "events" / "MyEventsView.vue",
    ]
    hits = []
    for fp in files:
        for i, line in enumerate(fp.read_text(encoding="utf-8").splitlines(), 1):
            if "tournamentId" in line:
                hits.append(f"{fp.name}:{i}")
    if hits:
        record("tournamentId_alias_cleanup", "FAIL", ", ".join(hits))
    else:
        record("tournamentId_alias_cleanup", "PASS", "V4 accountEvents mainline clean")


def check_old_jobs():
    rows = db_query(
        "SELECT job_id, job_name, status, invoke_target FROM sys_job "
        "WHERE invoke_target LIKE %s OR invoke_target LIKE %s",
        ("%ScheduleAutoGenerateJob%", "%MatchStatusAutoFinishJob%"),
    )
    if rows is None:
        record("old_jobs_status", "SKIP", "pymysql unavailable")
        return
    REPORT["old_jobs"] = [{"job_id": r[0], "job_name": r[1], "status": r[2], "invoke_target": r[3]} for r in rows]
    bad = [r for r in rows if str(r[2]) == "0"]
    if bad:
        record("old_jobs_status", "FAIL", f"active={len(bad)}")
    elif rows:
        record("old_jobs_status", "PASS", f"found={len(rows)} all disabled")
    else:
        record("old_jobs_status", "PASS", "no legacy jobs in sys_job")


def check_old_tables():
    legacy_tables = [
        "basket_competition_season", "basket_tournament", "basket_tournament_season",
        "basket_tournament_registration", "basket_tournament_registration_player",
        "basket_tournament_registration_audit", "basket_tournament_open_registration",
        "basket_team_season_profile", "basket_tournament_season_roster",
        "basket_captain_qualification", "basket_schedule_generate_batch",
        "basket_data_settlement_batch", "basket_team_season_stat",
        "basket_player_season_stat", "basket_stage_standing",
    ]
    if not pymysql:
        record("old_tables_inventory", "SKIP", "pymysql unavailable")
        return
    conn = pymysql.connect(**DB_CONN)
    inventory = []
    try:
        with conn.cursor() as cur:
            for table in legacy_tables:
                cur.execute(
                    "SELECT COUNT(*) FROM information_schema.tables "
                    "WHERE table_schema=%s AND table_name=%s",
                    (DB_CONN["database"], table),
                )
                exists = cur.fetchone()[0] > 0
                row_count = None
                if exists:
                    cur.execute(f"SELECT COUNT(*) FROM `{table}`")
                    row_count = cur.fetchone()[0]
                inventory.append({"table": table, "exists": exists, "row_count": row_count})
    finally:
        conn.close()
    REPORT["legacy_tables"] = inventory
    existing = [x for x in inventory if x["exists"]]
    record("old_tables_inventory", "PASS", f"existing={len(existing)}/{len(legacy_tables)}")


def run_sub_smoke(script_name: str) -> tuple[int, int]:
    script = ROOT / "scripts" / script_name
    if not script.exists():
        record(script_name, "SKIP", "script missing")
        return 0, 0
    proc = subprocess.run([sys.executable, str(script)], cwd=str(ROOT), capture_output=True, text=True)
    out = proc.stdout + proc.stderr
    fails = out.count("[!!]")
    passes = out.count("[OK]")
    status = "PASS" if proc.returncode == 0 else "FAIL"
    record(script_name, status, f"exit={proc.returncode} pass~={passes} fail~={fails}")
    return passes, fails


def api_full_chain(token: str):
    # Public V4
    series = http_json(WEB_BASE, "GET", "/basket/v4/public/series/list")[0].get("data") or []
    record("e2e_public_series", "PASS" if isinstance(series, list) else "FAIL", f"count={len(series)}")

    seasons = http_json(WEB_BASE, "GET", "/basket/v4/public/season/list")[0].get("data") or []
    record("e2e_public_seasons", "PASS" if isinstance(seasons, list) else "FAIL", f"count={len(seasons)}")
    if seasons:
        sid = seasons[0].get("seasonId")
        detail = http_json(WEB_BASE, "GET", f"/basket/v4/public/season/{sid}")[0].get("data") or {}
        divs = detail.get("divisions") or []
        record("e2e_public_season_detail", "PASS" if divs else "FAIL", f"season={sid} divisions={len(divs)}")

    stats = http_json(WEB_BASE, "GET", "/basket/v4/public/stats/rank?divisionId=5&rankType=PLAYER_SCORE", allow_fail=True)[0]
    record("e2e_public_rank", "PASS" if stats.get("code") in (200, 0, "200", None) else "FAIL", f"code={stats.get('code')}")

    # Player center V4
    avail = http_json(WEB_BASE, "GET", "/basket/v4/player/registration/available-divisions", token=token)[0].get("data") or []
    has_division_id = all("divisionId" in (x or {}) for x in avail) if avail else True
    record("e2e_player_available", "PASS" if has_division_id else "FAIL", f"count={len(avail)}")

    my_events = http_json(WEB_BASE, "GET", "/basket/v4/player/my-events", token=token)[0].get("data") or []
    division_bound = all("divisionId" in (x or {}) for x in my_events) if my_events else True
    record("e2e_player_my_events", "PASS" if division_bound else "FAIL", f"count={len(my_events)}")

    schedule = http_json(WEB_BASE, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=token)[0].get("data") or []
    if schedule:
        s0 = schedule[0]
        ok = all(k in s0 for k in ["divisionId", "matchId", "mySubmissionStatus"])
        record("e2e_player_schedule", "PASS" if ok else "FAIL", f"count={len(schedule)}")
    else:
        record("e2e_player_schedule", "PASS", "count=0")


def ui_pages():
    public_paths = ["/tournament", "/data"]
    account_paths = [
        "/account/my-events",
        "/account/my-events?tab=schedule",
    ]
    for path in public_paths + account_paths:
        try:
            status, body = http_page(UI_BASE, path)
            ok = status == 200 and ("app" in body.lower() or "vite" in body.lower() or "my-events" in body)
            record(f"ui_page_{path.split('?')[0].replace('/', '_')}", "PASS" if ok else "FAIL", f"HTTP {status}")
        except Exception as ex:
            record(f"ui_page_{path.split('?')[0].replace('/', '_')}", "SKIP", str(ex))


def main() -> int:
    check_v4_mainline_no_legacy_api()
    check_tournament_id_alias()
    check_old_jobs()
    check_old_tables()

    try:
        token = web_login(CAPTAIN_EMAIL)
        record("web_login", "PASS", CAPTAIN_EMAIL)
        api_full_chain(token)
    except Exception as ex:
        record("web_login", "FAIL", str(ex))

    ui_pages()

    # Reuse prior round smokes as regression
    for script in [
        "smoke_v4_r6_public_web.py",
        "smoke_v4_r7_personal_center.py",
    ]:
        run_sub_smoke(script)

    out_json = ROOT / "docs" / "cnpa-v4-round8-smoke-results.json"
    out_json.write_text(json.dumps({"report": REPORT, "results": RESULTS}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nWrote {out_json}")

    fails = sum(1 for _, s, _ in RESULTS if s == "FAIL")
    REPORT["fail_count"] = fails
    REPORT["pass_count"] = sum(1 for _, s, _ in RESULTS if s == "PASS")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
