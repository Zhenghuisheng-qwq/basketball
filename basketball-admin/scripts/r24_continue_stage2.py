#!/usr/bin/env python3
"""Continue R2-4 smoke from Stage2 (Division 118 already has Stage1 complete)."""
from __future__ import annotations

import sys

import r24_match_series_smoke as s

DIVISION_ID = "118"
ST2_ID = "485"
ST3_ID = "486"


def main() -> int:
    s.clear_login_locks()
    admin = s.admin_login()
    web_tokens: dict[str, str] = {}
    for i, t in enumerate(s.TEAMS):
        if i:
            __import__("time").sleep(1)
        web_tokens[s.as_id(t["team_id"])] = s.web_login(t["captain_email"])

    etr = s.req(f"{s.ADMIN}/basket/v4/event-team/list?divisionId={DIVISION_ID}&pageNum=1&pageSize=20", token=admin)
    event_teams = {row["teamNameSnapshot"]: row for row in (etr.get("rows") or [])}
    web_by_event: dict[str, str] = {}
    for t in s.TEAMS:
        et = event_teams.get(t["name"]) or next((v for k, v in event_teams.items() if t["name"] in k), None)
        if et:
            web_by_event[s.as_id(et["id"])] = web_tokens[s.as_id(t["team_id"])]

    # Stage2 already RUNNING if schedule was published in a prior attempt
    series_list = s.list_series(admin, int(ST2_ID))
    if len(series_list) != 2:
        s.log("STAGE2_SERIES_COUNT", "FAIL", f"got {len(series_list)}")
        return 1
    s.log("STAGE2_SERIES_COUNT", "PASS", f"series={len(series_list)}")

    sa, sb = series_list[0], series_list[1]
    sa_final = s.finish_series_games(admin, web_by_event, sa["id"], ["HOME", "HOME"])
    s.log("BO3_2_0", "PASS" if sa_final.get("seriesStatus") == "COMPLETED" and sa_final.get("homeWinCount") == 2 else "FAIL", str(sa_final))

    g3 = next((g for g in sa_final.get("games") or [] if g.get("seriesGameNo") == 3), None)
    if g3:
        wt = web_by_event.get(s.as_id(sa_final.get("homeEventTeamId")))
        br = s.req(f"{s.WEB}/basket/v4/player/match-submission/save", "POST", {
            "matchId": g3["matchId"], "submitEventTeamId": sa_final.get("homeEventTeamId"),
            "homeScore": 64, "awayScore": 55,
            "teamStat": {"score": 64, "rebound": 1, "assist": 1, "steal": 0, "block": 0, "turnover": 0},
            "playerStats": [], "materialFileIds": [],
        }, wt)
        s.log("GAME3_SUBMIT_GATE", "PASS" if br.get("code") != 200 else "FAIL", br.get("msg", ""))

    sb_final = s.finish_series_games(admin, web_by_event, sb["id"], ["HOME", "AWAY", "HOME"])
    s.log("BO3_2_1", "PASS" if sb_final.get("seriesStatus") == "COMPLETED" and sb_final.get("homeWinCount") == 2 else "FAIL", str(sb_final))

    try:
        s.confirm_stage_qualification(admin, ST2_ID)
        s.log("QUAL_CONFIRM", "PASS")
    except Exception as e:
        s.log("QUAL_CONFIRM", "FAIL", str(e))
        return 1

    s.req(f"{s.ADMIN}/basket/v4/admin/stages/{ST3_ID}/initialize-teams", "POST", token=admin)
    gen3 = s.req(f"{s.ADMIN}/basket/v4/admin/stages/{ST3_ID}/generate-schedule", "POST", token=admin)
    pub3 = s.req(f"{s.ADMIN}/basket/v4/admin/stages/{ST3_ID}/publish-schedule", "POST", token=admin)
    if pub3.get("code") != 200:
        s.log("STAGE3_PUBLISH", "FAIL", str(pub3))
        return 1
    s3_series = s.list_series(admin, int(ST3_ID))
    if len(s3_series) != 1:
        s.log("BO5_SERIES", "FAIL", str(s3_series))
        return 1
    s5 = s.finish_series_games(admin, web_by_event, s3_series[0]["id"], ["HOME", "AWAY", "HOME", "AWAY", "HOME"])
    s.log("BO5_3_2", "PASS" if s5.get("seriesStatus") == "COMPLETED" and s5.get("homeWinCount") == 3 else "FAIL", str(s5))
    s.log("FINAL_WINNER", "PASS", s5.get("winnerTeamName", ""))

    ps = s.public_series(sa["id"])
    s.log("WEB_SERIES_API", "PASS" if ps.get("id") else "FAIL")
    s.log("ADMIN_SERIES_API", "PASS" if s.get_series(admin, sa["id"]).get("id") else "FAIL")

    fails = [k for k, v in s.results.items() if not v.startswith("PASS")]
    print("\n=== CONTINUE SUMMARY ===")
    for k, v in sorted(s.results.items()):
        print(f"{k}: {v}")
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main())
