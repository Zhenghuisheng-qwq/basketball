# -*- coding: utf-8 -*-
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
sys.path.insert(0, str(ROOT / "scripts"))
import full_smoke_common as fs
import cnpa_v13_match_submission_e2e as e2e

fs.ADMIN = e2e.ADMIN
fs.WEB = e2e.WEB
fs.DB["database"] = "basketball_r9_fresh"
admin = fs.admin_login()

fid = "2088854453879885826"
st, body, ctype = e2e.http_raw(f"{e2e.ADMIN}/basket/v4/match-submission-audit/material/{fid}", admin)
ok_stream = st == 200 and (ctype.startswith("image/") or body[:8] == b"\x89PNG\r\n\x1a\n")
e2e.record("admin.audit.screenshot.stream", ok_stream, f"status={st} ctype={ctype} bytes={len(body)}", "P1")

for sid in (234, 235, 236):
    r = e2e.approve(admin, sid)
    print("APPROVE", sid, r.get("code"), str(r.get("msg"))[:160], flush=True)
    e2e.record(f"resume.approve.{sid}", r.get("code") == 200, str(r.get("msg")), "P0")

m = e2e.match_row(328)
e2e.record(
    "bo1.settlement",
    m.get("match_status") == "FINISHED" and int(m.get("home_score") or -1) == 80 and int(m.get("winner_event_team_id") or 0) == 79,
    str(m),
    "P0",
)
official = fs.db_query(
    "SELECT event_player_id, event_team_id, points, rebound, assist, steal, `block`, foul, turnover "
    "FROM basket_match_player_stat WHERE match_id=328 AND is_delete=0"
)
e2e.record("bo1.official.playerStat", official and any(int(r.get("points") or 0) == 22 for r in official), str(official))
e2e.record("bo1.approve", m.get("match_status") == "FINISHED", str(m))

# continue BO3 from series 82 after G1
s1 = e2e.series_row(82)
e2e.record("bo3.game1.finished", e2e.match_row(428).get("match_status") == "FINISHED", str(e2e.match_row(428)))
e2e.record("bo3.score.1-0", int(s1.get("home_win_count") or 0) == 1 and s1.get("series_status") != "COMPLETED", str(s1), "P0")
home, token = 116, e2e.login_team(116)
if int(s1.get("home_win_count") or 0) < 2:
    e2e.play_one_game(admin, token, 429, home, 81, 74, "E2E BO3 G2", e2e.PNG_B)
s2 = e2e.series_row(82)
e2e.record("bo3.score.2-0.completed", int(s2.get("home_win_count") or 0) == 2 and s2.get("series_status") == "COMPLETED" and int(s2.get("winner_event_team_id") or 0) == home, str(s2), "P0")
next_series = fs.db_query(
    "SELECT id, home_event_team_id, away_event_team_id, series_status FROM basket_match_series "
    "WHERE stage_id=%s AND is_delete=0 AND (home_event_team_id=%s OR away_event_team_id=%s) AND id<>82",
    (s2.get("stage_id"), home, home),
)
e2e.record("bo3.winnerAdvance", any(int(x.get("home_event_team_id") or 0) == home or int(x.get("away_event_team_id") or 0) == home for x in next_series), str(next_series), "P0")
d3 = e2e.detail(token, 430, home)
e2e.record("bo3.game3.skipped.detail", bool((d3.get("seriesContext") or {}).get("skipped")), str(d3.get("seriesContext")), "P1")
sched2 = e2e.need(e2e.req(f"{e2e.WEB}/basket/v4/player/my-schedule?divisionId={s2['division_id']}&includeFinished=true", token=token), "my-schedule-2")
g3_row = next((r for r in e2e.schedule_items(sched2) if str(r.get("matchId")) == "430"), None)
e2e.record("bo3.game3.skipped.schedule", bool(g3_row) and (g3_row.get("userActionType") == "NONE" or "无需进行" in str(g3_row.get("cannotSubmitReason") or g3_row.get("userActionText") or "")), str(g3_row), "P1")
before_n = len(e2e.submission_rows(430, home))
blocked = e2e.save_draft(token, 430, home, 1, 0, [e2e.player_row(e2e.eligible(token, 430, home)[0]["eventPlayerId"])], [], "should fail")
e2e.record("bo3.game3.rejectNewSubmission", blocked.get("code") != 200 and len(e2e.submission_rows(430, home)) == before_n, str(blocked.get("msg")), "P1")
e2e.assert_audit_view(admin, 235, expect_series=True)

# continue BO5 3:1 from series 93 after G1
s5 = e2e.series_row(93)
games = fs.db_query("SELECT id, series_game_no FROM basket_match WHERE series_id=93 AND is_delete=0 ORDER BY series_game_no")
home5, away5 = int(s5["home_event_team_id"]), int(s5["away_event_team_id"])
ht, at = e2e.login_team(home5), e2e.login_team(away5)
plan = [
    (int(games[1]["id"]), at, away5, 68, 77, e2e.PNG_B, (1, 1)),
    (int(games[2]["id"]), ht, home5, 82, 75, e2e.PNG_A, (2, 1)),
    (int(games[3]["id"]), ht, home5, 79, 71, e2e.PNG_C, (3, 1)),
]
cur = e2e.series_row(93)
e2e.record("bo5.score.1-0", int(cur.get("home_win_count") or 0) == 1, str(cur), "P0")
for mid, tok, tid, hs, aws, png, (eh, ea) in plan:
    cur = e2e.series_row(93)
    if int(cur.get("home_win_count") or 0) == eh and int(cur.get("away_win_count") or 0) == ea:
        e2e.record(f"bo5.score.{eh}-{ea}", True, str(cur), "P0")
        continue
    e2e.play_one_game(admin, tok, mid, tid, hs, aws, f"E2E BO5 G{eh+ea}", png)
    cur = e2e.series_row(93)
    e2e.record(f"bo5.score.{eh}-{ea}", int(cur.get("home_win_count") or 0) == eh and int(cur.get("away_win_count") or 0) == ea, str(cur), "P0")
cur = e2e.series_row(93)
e2e.record("bo5.completed.3-1", cur.get("series_status") == "COMPLETED" and int(cur.get("winner_event_team_id") or 0) == home5, str(cur), "P0")
g5 = int(games[4]["id"])
d5 = e2e.detail(ht, g5, home5)
e2e.record("bo5.game5.skipped", bool((d5.get("seriesContext") or {}).get("skipped")), str(d5.get("seriesContext")), "P1")
blocked5 = e2e.save_draft(ht, g5, home5, 1, 0, [e2e.player_row(e2e.eligible(ht, g5, home5)[0]["eventPlayerId"])], [], "g5")
e2e.record("bo5.game5.noSubmit", blocked5.get("code") != 200, str(blocked5.get("msg")), "P1")
sched = e2e.need(e2e.req(f"{e2e.WEB}/basket/v4/player/my-schedule?divisionId={s5['division_id']}&includeFinished=true", token=ht), "bo5-schedule")
srows = [r for r in e2e.schedule_items(sched) if str(r.get("matchSeriesId") or "") == "93"]
e2e.record("bo5.ui.seriesScore", any(int(r.get("seriesHomeWinCount") or 0) == 3 and int(r.get("seriesAwayWinCount") or 0) == 1 for r in srows), str(srows[0] if srows else None), "P1")
g5_row = next((r for r in srows if str(r.get("matchId")) == str(g5)), None)
e2e.record("bo5.ui.game5.无需进行", bool(g5_row) and (g5_row.get("userActionType") == "NONE" or "无需进行" in str(g5_row.get("userActionText") or g5_row.get("cannotSubmitReason") or "")), str(g5_row), "P1")
e2e.assert_audit_view(admin, e2e.submission_rows(int(games[3]["id"]), home5)[-1]["id"], expect_series=True)

e2e.REPORT["ids"].update({
    "bo1": {"matchId": 328, "homeEventTeamId": 79, "awayEventTeamId": 78, "v1": 233, "v2": 234},
    "bo3": {"seriesId": 82, "games": [428, 429, 430], "home": 116, "away": 119},
    "bo5": {"seriesId": 93, "divisionId": int(s5["division_id"]), "games": [int(g["id"]) for g in games], "home": home5, "away": away5, "seriesCode": s5.get("series_code")},
    "accounts": {"79": "fs_ld_ld0601@cnpa.smoke", "116": "fs_ib_ib0301@cnpa.smoke", str(home5): e2e.captain_email(home5)},
    "adminLogin": "admin / admin123",
})
e2e.run_invariants()
out = Path(__file__).with_name("cnpa_v13_match_submission_e2e_result.json")
prev = json.loads(out.read_text(encoding="utf-8")) if out.exists() else {}
merged_cases = {**(prev.get("cases") or {}), **e2e.REPORT["cases"]}
e2e.REPORT["cases"] = merged_cases
e2e.REPORT["bugs"] = {"P0": [], "P1": [], "P2": list(prev.get("bugs", {}).get("P2") or [])}
for name, c in merged_cases.items():
    if not c.get("ok"):
        sev = "P0" if any(x in name for x in ("settlement", "approve", "score", "Advance", "acl", "matchBind", "isolation", "fixture")) else "P1"
        e2e.REPORT["bugs"][sev].append(f"{name}: {c.get('detail')}")
out.write_text(json.dumps(e2e.REPORT, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
print("wrote", out, "p0", e2e.REPORT["bugs"]["P0"], "p1", e2e.REPORT["bugs"]["P1"], flush=True)
