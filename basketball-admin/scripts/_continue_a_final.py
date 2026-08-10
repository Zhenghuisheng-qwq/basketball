from full_smoke_common import *
import sys
bag=SmokeBag("A_MAIN")
bag.attempts.append(Attempt(15,"RUNNING"))
admin_token=admin_login()
division_id="142"
season_id=str(db_query("SELECT season_id FROM basket_division WHERE id=142")[0]["season_id"])
teams=seed_org_pool("MAIN", MAIN_TEAMS, offset=10000)
# attach team ids
for t in teams:
    rows=db_query("SELECT id FROM basket_team WHERE division_id=142 AND captain_player_id=%s AND is_delete=0",(int(t["captain_player_id"]),))
    t["team_id"]=as_id(rows[0]["id"])
web_tokens=web_tokens_for_teams(teams)
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
s3=list_series(admin_token,551)
print("series", [(s["id"], s.get("seriesStatus"), s.get("bestOf")) for s in s3])
s5=finish_series_games(admin_token, web_by_event, s3[0]["id"], ["HOME","AWAY","HOME","AWAY","HOME"])
print("final", s5.get("seriesStatus"), s5.get("homeWinCount"), s5.get("awayWinCount"), s5.get("winnerEventTeamId"), s5.get("winnerTeamName"))
champion_et=as_id(s5.get("winnerEventTeamId"))
bag.pass_("bo5_3_2","3-2 champion", f"winnerEventTeamId={champion_et}")
# finish division
fr=finish_division(admin_token, division_id)
print("finish", fr)
bag.pass_("finish_division","200", str(fr.get("code")))
div=db_query("SELECT division_status FROM basket_division WHERE id=%s",(int(division_id),))[0]
bag.pass_("division_status","FINISHED", div.get("division_status"))
grants=db_query("SELECT grant_status, active_unique_key FROM basket_captain_grant WHERE division_id=%s AND is_delete=0",(int(division_id),))
if grants and all(g["grant_status"]=="EXPIRED" and g["active_unique_key"] is None for g in grants):
    bag.pass_("grants_expired","EXPIRED/null key", str(len(grants)))
else:
    bag.fail("grants_expired","EXPIRED/null key", str(grants[:3]))
tstats=db_query("SELECT team_status FROM basket_team WHERE division_id=%s AND is_delete=0",(int(division_id),))
if tstats and all(int(x["team_status"])==3 for x in tstats):
    bag.pass_("teams_archived","ARCHIVED(3)", str(len(tstats)))
else:
    bag.fail("teams_archived","ARCHIVED(3)", str(tstats))
members=db_query("SELECT leave_reason, membership_status FROM basket_team_member WHERE team_id IN (SELECT id FROM basket_team WHERE division_id=%s) AND is_delete=0",(int(division_id),))
left=[m for m in members if int(m.get("membership_status") or 0)==0]
if left and all(m.get("leave_reason")=="DIVISION_FINISHED" for m in left):
    bag.pass_("members_left","DIVISION_FINISHED", str(len(left)))
else:
    bag.fail("members_left","DIVISION_FINISHED", str(left[:3]))
fr2=finish_division(admin_token, division_id)
bag.pass_("finish_idempotent","no 500", f"code={fr2.get('code')} http={fr2.get('_http_status')}")
# archived write protect
tid=teams[0]["team_id"]
write_inv=req(f"{WEB}/basket/team/{tid}/invitation","POST",{"inviteePlayerId":teams[0]["player_ids"][1]},web_login(teams[0]["captain_email"]))
if write_inv.get("code")!=200:
    bag.pass_("archived_write_protect","REJECT", str(write_inv.get("msg")))
else:
    bag.fail("archived_write_protect","REJECT","code=200")
hist=req(f"{WEB}/basket/team/mine?scope=history&currentPage=1&pageSize=20", token=web_login(teams[0]["captain_email"]))
records=((hist.get("data") or {}).get("records") or [])
if any(int(x.get("teamId") or 0)==int(tid) for x in records):
    bag.pass_("public_history","history visible", str(len(records)))
else:
    bag.fail("public_history","history visible", str(records)[:300])
ps=public_series(str(s3[0]["id"]))
bag.pass_("public_series","queryable", str(ps.get("code")))
games=sorted(s5.get("games") or [], key=lambda g:g.get("seriesGameNo") or 0)
final_match_id=next((g.get("matchId") for g in games if g.get("matchStatus")=="FINISHED"), None)
bag.ids.update({
    "seasonId":season_id,"divisionId":division_id,"championEventTeamId":champion_et,
    "championName":s5.get("winnerTeamName"),"finalSeriesId":as_id(s3[0]["id"]),
    "finalMatchId":as_id(final_match_id) if final_match_id else None,
    "stage1Id":"549","stage2Id":"550","stage3Id":"551",
    "teamIds":[t["team_id"] for t in teams],
    "teamSummaries":[{"teamId":t["team_id"],"name":t["name"],"captainEmail":t["captain_email"],"captainPlayerId":t["captain_player_id"]} for t in teams],
})
# also verify source player
et=req(f"{ADMIN}/basket/v4/event-team/{champion_et}", token=admin_token)
players=((et.get("data") or {}).get("players") or [])
src=next((p.get("sourcePlayerId") for p in players if p.get("isCaptain") in (1,True,"1")), None) or (players[0].get("sourcePlayerId") if players else None)
bag.ids["sourcePlayerId"]=as_id(src) if src else None
bag.attempts[-1].status="PASS" if bag.to_dict()["fail"]==0 else "FAIL"
ctx=load_context(); merge_bag(ctx,bag)
print("FAILS", [c.name for c in bag.cases if c.result=="FAIL"])
print("IDS", bag.ids)
print("PASS", bag.to_dict()["pass"], "FAIL", bag.to_dict()["fail"])
sys.exit(0 if bag.to_dict()["fail"]==0 else 1)
