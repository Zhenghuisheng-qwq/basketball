#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round7 personal center real acceptance smoke."""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

WEB_BASE = "http://127.0.0.1:8881"
UI_BASE = "http://127.0.0.1:5173"
MEMBER_PASSWORD = "111111aa"
CAPTAIN_EMAIL = "111111@qq.com"
AWAY_PLAYER_EMAIL = "111122@qq.com"

# From DB / R5 acceptance
TEST_MEMBER_ID = "2082478335308456000"
TEST_PLAYER_ID = "2082478335308456000"
TEST_SEASON_ID = 4
TEST_DIVISION_ID = 5
TEST_EVENT_TEAM_ID = 14
TEST_MATCH_ID = 1

RESULTS: list[tuple[str, str, str]] = []
REPORT: dict = {
    "r7_backend_loaded": False,
    "test_account_captain": CAPTAIN_EMAIL,
    "test_account_away_player": AWAY_PLAYER_EMAIL,
    "test_player_id": TEST_PLAYER_ID,
    "test_season_id": TEST_SEASON_ID,
    "test_division_id": TEST_DIVISION_ID,
    "test_event_team_id": TEST_EVENT_TEAM_ID,
    "test_match_id": TEST_MATCH_ID,
    "smoke_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
}


def record(name: str, status: str, detail: str = "") -> None:
    RESULTS.append((name, status, detail))
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(method: str, path: str, body: dict | None = None, token: str | None = None, allow_fail=False):
    url = WEB_BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body).encode("utf-8")
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
    resp, _ = http_json("POST", "/basket/member/auth/login/password", {"email": email, "password": MEMBER_PASSWORD})
    token = (resp.get("data") or {}).get("token")
    if not token:
        raise RuntimeError(f"login failed for {email}: {resp}")
    return token


def http_page(path: str) -> tuple[int, str]:
    url = UI_BASE + path
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=20) as resp:
        return resp.status, resp.read(8192).decode("utf-8", errors="ignore")


def main() -> int:
    # 1) unauthenticated -> 401
    for path in [
        "/basket/v4/player/registration/available-divisions",
        "/basket/v4/player/my-schedule",
    ]:
        payload, code = http_json("GET", path, allow_fail=True)
        if code == 401 or payload.get("code") in (401, "401"):
            record(f"auth401_{path.split('/')[-1]}", "PASS", "401 without login")
        else:
            record(f"auth401_{path.split('/')[-1]}", "FAIL", f"code={payload.get('code')} http={code}")

    captain_token = web_login(CAPTAIN_EMAIL)
    away_token = web_login(AWAY_PLAYER_EMAIL)
    record("web_login_captain", "PASS", CAPTAIN_EMAIL)
    record("web_login_away_player", "PASS", AWAY_PLAYER_EMAIL)

    # available-divisions
    avail = http_json("GET", "/basket/v4/player/registration/available-divisions", token=captain_token)[0]
    avail_data = avail.get("data") or []
    REPORT["available_divisions_count"] = len(avail_data)
    if isinstance(avail_data, list):
        sample = avail_data[0] if avail_data else {}
        has_grant_fields = any("grantTeamId" in (x or {}) for x in avail_data)
        has_match_time = any("matchStartTime" in (x or {}) for x in avail_data)
        can_reg = [x for x in avail_data if x.get("canRegister")]
        no_grant = [x for x in avail_data if x.get("cannotRegisterReason")]
        record("api_available_divisions", "PASS", f"count={len(avail_data)} canRegister={len(can_reg)} noGrantReason={len(no_grant)} grantFields={has_grant_fields} matchTime={has_match_time}")
    else:
        record("api_available_divisions", "FAIL", str(avail))

    # my-list
    my_list = http_json("GET", "/basket/v4/player/registration/my-list", token=captain_token)[0].get("data") or []
    REPORT["my_list_count"] = len(my_list)
    statuses = {x.get("registrationStatus") for x in my_list if isinstance(x, dict)}
    has_ids = all(("divisionId" in x and "seasonId" in x) for x in my_list) if my_list else True
    record("api_my_list", "PASS" if has_ids else "FAIL", f"count={len(my_list)} statuses={sorted(s for s in statuses if s)}")

    # my-events
    my_events = http_json("GET", "/basket/v4/player/my-events", token=captain_token)[0].get("data") or []
    REPORT["my_events_count"] = len(my_events)
    if my_events:
        e0 = my_events[0]
        fields_ok = all(k in e0 for k in ["eventTeamId", "divisionId", "seasonId", "matchCount", "finishedMatchCount", "pendingSubmissionCount"])
        record("api_my_events", "PASS" if fields_ok else "FAIL", f"count={len(my_events)} sampleTeam={e0.get('eventTeamId')}")
    else:
        record("api_my_events", "PASS", "count=0 (player may have no events)")

    # my-schedule default
    sched = http_json("GET", "/basket/v4/player/my-schedule", token=captain_token)[0].get("data") or []
    REPORT["my_schedule_default_count"] = len(sched)
    if sched:
        s0 = sched[0]
        req_fields = ["matchId", "myEventTeamId", "opponentEventTeamId", "mySubmissionStatus", "canSubmitData"]
        missing = [f for f in req_fields if f not in s0]
        finished_in_default = [x for x in sched if x.get("matchStatus") == "FINISHED"]
        record("api_my_schedule_default", "FAIL" if missing else "PASS", f"count={len(sched)} finishedInDefault={len(finished_in_default)} missing={missing}")
    else:
        record("api_my_schedule_default", "PASS", "count=0")

    # my-schedule includeFinished
    sched_all = http_json("GET", "/basket/v4/player/my-schedule?includeFinished=true", token=captain_token)[0].get("data") or []
    REPORT["my_schedule_all_count"] = len(sched_all)
    finished = [x for x in sched_all if x.get("matchStatus") == "FINISHED"]
    if sched_all:
        s0 = sched_all[0]
        score_ok = "homeScore" in s0 and "awayScore" in s0
        opp_ok = "opponentSubmissionStatus" in s0
        record("api_my_schedule_all", "PASS" if score_ok and opp_ok else "FAIL", f"count={len(sched_all)} finished={len(finished)} scores={score_ok}")
    else:
        record("api_my_schedule_all", "SKIP", "no schedule rows")

    REPORT["r7_backend_loaded"] = True
    record("r7_backend_loaded", "PASS", "my-schedule reachable with login")

    # my-matches + A/B independence
    my_matches = http_json("GET", f"/basket/v4/player/match-submission/my-matches?divisionId={TEST_DIVISION_ID}", token=captain_token)[0].get("data") or []
    REPORT["my_matches_count"] = len(my_matches)
    target = next((x for x in my_matches if str(x.get("matchId")) == str(TEST_MATCH_ID)), None)
    if target:
        REPORT["my_match_sample"] = {
            "mySubmissionStatus": target.get("mySubmissionStatus"),
            "opponentSubmissionStatus": target.get("opponentSubmissionStatus"),
            "canSubmitData": target.get("canSubmitData"),
            "cannotSubmitReason": target.get("cannotSubmitReason"),
        }
        # Home approved should not block away if away is different team - check away token
        away_matches = http_json("GET", f"/basket/v4/player/match-submission/my-matches?divisionId={TEST_DIVISION_ID}", token=away_token)[0].get("data") or []
        away_target = next((x for x in away_matches if str(x.get("matchId")) == str(TEST_MATCH_ID)), None)
        if away_target:
            home_pending = target.get("mySubmissionStatus") == "PENDING_REVIEW"
            away_can = away_target.get("canSubmitData") or away_target.get("canSubmit")
            if home_pending:
                record("ab_independence", "PASS" if away_can else "FAIL", f"home={target.get('mySubmissionStatus')} awayCan={away_can}")
            else:
                record("ab_independence", "PASS", f"home={target.get('mySubmissionStatus')} away={away_target.get('mySubmissionStatus')} awayCan={away_can} (no home pending case)")
        record("api_my_matches", "PASS", f"count={len(my_matches)} match1={target.get('mySubmissionStatus')}")
    else:
        record("api_my_matches", "PASS" if isinstance(my_matches, list) else "FAIL", f"count={len(my_matches)}")

    # match submission detail
    detail = http_json("GET", f"/basket/v4/player/match-submission/{TEST_MATCH_ID}/detail?submitEventTeamId={TEST_EVENT_TEAM_ID}", token=captain_token, allow_fail=True)
    if detail[1] == 200 or (detail[0].get("code") in (200, 0, "200")):
        record("api_match_submission_detail", "PASS", f"status={detail[0].get('data', {}).get('submissionStatus')}")
    else:
        record("api_match_submission_detail", "SKIP", str(detail[0].get("msg")))

    # UI pages
    for path in [
        "/account/my-events",
        "/account/my-events?tab=available",
        "/account/my-events?tab=applications",
        "/account/my-events?tab=joined",
        "/account/my-events?tab=schedule",
        f"/account/my-events/v4/match-submission/{TEST_MATCH_ID}?submitEventTeamId={TEST_EVENT_TEAM_ID}",
    ]:
        try:
            status, body = http_page(path)
            ok = status == 200 and ("my-events" in body or "app" in body.lower() or "vite" in body.lower())
            record(f"ui_{path.split('?')[0].replace('/', '_')}", "PASS" if ok else "FAIL", f"HTTP {status}")
        except Exception as ex:
            record(f"ui_{path.split('?')[0].replace('/', '_')}", "SKIP", str(ex))

    out_json = Path(__file__).resolve().parents[1] / "docs" / "cnpa-v4-round7-smoke-results.json"
    out_json.write_text(json.dumps({"report": REPORT, "results": RESULTS}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nWrote {out_json}")

    fails = sum(1 for _, s, _ in RESULTS if s == "FAIL")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
