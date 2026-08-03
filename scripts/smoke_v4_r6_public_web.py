#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round6 public web page/API smoke acceptance."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

WEB_BASE = "http://127.0.0.1:8881"
UI_BASE = "http://127.0.0.1:5173"
SEASON_ID = 2
DIVISION_ID = 3
EVENT_TEAM_ID = 13
MATCH_SEASON_ID = 4
MATCH_DIVISION_ID = 5
MATCH_ID = 1

RESULTS: list[tuple[str, str, str]] = []


def record(name: str, status: str, detail: str = "") -> None:
    RESULTS.append((name, status, detail))
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(base: str, path: str, allow_fail=False):
    url = base + path
    req = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            http_code = resp.status
    except urllib.error.HTTPError as exc:
        payload = json.loads(exc.read().decode("utf-8", errors="ignore") or "{}")
        http_code = exc.code
        if not allow_fail:
            raise RuntimeError(f"GET {path} HTTP {http_code}: {payload}") from exc
    code = payload.get("code")
    if not allow_fail and code not in (200, 0, None, "200"):
        raise RuntimeError(f"GET {path} code={code} msg={payload.get('msg')} payload={payload}")
    return payload


def http_page(base: str, path: str) -> tuple[int, str]:
    url = base + path
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=20) as resp:
        body = resp.read(4096).decode("utf-8", errors="ignore")
        return resp.status, body


def main() -> int:
    # API smoke
    try:
        series = http_json(WEB_BASE, "/basket/v4/public/series/list")
        record("api_series_list", "PASS", f"count={len(series.get('data') or [])}")
    except Exception as ex:
        record("api_series_list", "FAIL", str(ex))

    try:
        seasons = http_json(WEB_BASE, "/basket/v4/public/season/list")
        data = seasons.get("data") or []
        record("api_season_list", "PASS", f"count={len(data)}")
        league = http_json(WEB_BASE, "/basket/v4/public/season/list?seriesCode=LEAGUE")
        record("api_season_list_league", "PASS", f"count={len(league.get('data') or [])}")
    except Exception as ex:
        record("api_season_list", "FAIL", str(ex))

    try:
        detail = http_json(WEB_BASE, f"/basket/v4/public/season/{SEASON_ID}")
        d = detail.get("data") or {}
        record("api_season_detail", "PASS", f"title={d.get('displayTitle')} divisions={len(d.get('divisions') or [])}")
    except Exception as ex:
        record("api_season_detail", "FAIL", str(ex))

    try:
        teams = http_json(WEB_BASE, f"/basket/v4/public/team/list?divisionId={DIVISION_ID}")
        team_detail = http_json(WEB_BASE, f"/basket/v4/public/team/{EVENT_TEAM_ID}")
        players = (team_detail.get("data") or {}).get("players") or []
        record("api_team_roster", "PASS", f"teams={len(teams.get('data') or [])} players={len(players)}")
    except Exception as ex:
        record("api_team_roster", "FAIL", str(ex))

    try:
        matches = http_json(WEB_BASE, f"/basket/v4/public/match/list?divisionId={MATCH_DIVISION_ID}")
        mlist = matches.get("data") or []
        finished = [m for m in mlist if m.get("matchStatus") == "FINISHED"]
        record("api_match_list", "PASS", f"count={len(mlist)} finished={len(finished)}")
    except Exception as ex:
        record("api_match_list", "FAIL", str(ex))

    try:
        match = http_json(WEB_BASE, f"/basket/v4/public/match/{MATCH_ID}")
        md = match.get("data") or {}
        record(
            "api_match_detail",
            "PASS",
            f"score={md.get('homeScore')}-{md.get('awayScore')} teamStats={len(md.get('teamStats') or [])} playerStats={len(md.get('playerStats') or [])}",
        )
    except Exception as ex:
        record("api_match_detail", "FAIL", str(ex))

    for name, path in [
        ("api_standing", f"/basket/v4/public/stats/standing?divisionId={MATCH_DIVISION_ID}"),
        ("api_team_stat", f"/basket/v4/public/stats/team-stat?divisionId={MATCH_DIVISION_ID}"),
        ("api_player_stat", f"/basket/v4/public/stats/player-stat?divisionId={MATCH_DIVISION_ID}"),
        ("api_rank", f"/basket/v4/public/stats/rank?divisionId={MATCH_DIVISION_ID}&rankType=PLAYER_SCORE"),
    ]:
        try:
            payload = http_json(WEB_BASE, path)
            record(name, "PASS", f"count={len(payload.get('data') or [])}")
        except Exception as ex:
            record(name, "FAIL", str(ex))

    old = http_json(WEB_BASE, "/basket/public/tournament/page?currentPage=1&pageSize=1", allow_fail=True)
    record("old_api_still_exists", "PASS" if old.get("code") == 200 else "SKIP", "legacy endpoint still alive but not used by V4 pages")

    # UI page smoke (if dev server up)
    pages = [
        ("page_tournament", "/tournament"),
        ("page_season_detail", f"/events/{SEASON_ID}"),
        ("page_match_detail", f"/events/match/{MATCH_ID}"),
        ("page_data", "/data"),
    ]
    ui_up = True
    for name, path in pages:
        try:
            status, body = http_page(UI_BASE, path)
            ok = status == 200 and ("app" in body.lower() or "vite" in body.lower() or "cnpa" in body.lower())
            record(name, "PASS" if ok else "FAIL", f"HTTP {status}")
        except Exception as ex:
            ui_up = False
            record(name, "SKIP", f"ui not running: {ex}")

    if not ui_up:
        record("ui_note", "SKIP", "start basketball-web-ui dev server for full page HTML smoke")

    fails = [r for r in RESULTS if r[1] == "FAIL"]
    print("\n=== SUMMARY ===")
    print(f"PASS={sum(1 for r in RESULTS if r[1]=='PASS')} FAIL={len(fails)} SKIP={sum(1 for r in RESULTS if r[1]=='SKIP')}")
    report = Path(__file__).resolve().parents[1] / "docs" / "cnpa-v4-round6-smoke-output.json"
    report.write_text(json.dumps({
        "seasonId": SEASON_ID,
        "divisionId": DIVISION_ID,
        "matchSeasonId": MATCH_SEASON_ID,
        "matchDivisionId": MATCH_DIVISION_ID,
        "matchId": MATCH_ID,
        "eventTeamId": EVENT_TEAM_ID,
        "results": [{"name": a, "status": b, "detail": c} for a, b, c in RESULTS],
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"report={report}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
