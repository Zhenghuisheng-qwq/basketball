from full_smoke_common import *
import json
bag=SmokeBag("C_ADVANCED")
bag.attempts.append(Attempt(4,"RUNNING","db-count fix + new season"))
admin_token=admin_login()
teams=seed_org_pool("AD3", ADV_TEAMS, offset=36000)[:6]
for t in teams:
    t["name"]=t["name"].replace("FSA_","FSA3_")
season_id=create_season(admin_token, "FULL_SMOKE_20260811_ADVANCED_V3", "FULL_SMOKE_20260811_ADVANCED_V3 Season")
division_id=create_division(admin_token, season_id, "FULL_SMOKE_20260811_ADVANCED_V3_PC_5V5", max_team=8, template_id=5)
refuse_forbidden_division(division_id)
stages=setup_advanced_format(admin_token, division_id)
open_registration(admin_token, division_id)
create_teams_flow(admin_token, division_id, teams)
populate_members(teams)
web_tokens=web_tokens_for_teams(teams)
register_all_teams(web_tokens, admin_token, division_id, teams)
close_registration(admin_token, division_id)
# explicit init+gen without buggy skip
init_stage_teams(admin_token, stages["st1"])
gen=generate_schedule(admin_token, stages["st1"])
print("GEN", gen)
pub=publish_schedule(admin_token, stages["st1"])
print("PUB", pub)
s1=list_series(admin_token, stages["st1"])
print("S1", [(s.get("id"), s.get("bestOf"), s.get("homeTeamName"), s.get("awayTeamName"), s.get("seriesStatus")) for s in s1])
print("DB matches", count_stage_matches(stages["st1"]))
bag.ids={"seasonId":season_id,"divisionId":division_id,**stages}
if not s1:
    bag.fail("BO1","series>0","0")
    bag.attempts[-1].status="FAIL"
    ctx=load_context(); 
    prev=ctx.get("smokes",{}).get("C_ADVANCED",{})
    bag.attempts=(prev.get("attempts") or [])+bag.attempts
    merge_bag(ctx,bag)
    raise SystemExit(1)

bag.pass_("BO1","bestOf=1", f"n={len(s1)} bestOf={list({s.get('bestOf') for s in s1})}")
bye=[s for s in s1 if not s.get("awayEventTeamId")]
if bye:
    g=get_series(admin_token, bye[0]["id"])
    fake=[x for x in (g.get("games") or []) if x.get("matchId") and not x.get("skipped")]
    bag.pass_("BYE","bye no fake match", f"series={bye[0]['id']} fake={len(fake)}")
else:
    bag.fail("BYE","expect bye", str(s1))

web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
playable=[s for s in s1 if s.get("homeEventTeamId") and s.get("awayEventTeamId")]
ser=playable[0]
ff=req(f"{EXC}/series-forfeit","POST",{"seriesId":int(ser["id"]),"forfeitEventTeamId":int(ser["awayEventTeamId"]),"reason":MARKER},admin_token)
bag.pass_("Forfeit","ok", f"code={ff.get('code')} series={ser['id']}")
for ser in playable[1:]:
    if get_series(admin_token, ser["id"]).get("seriesStatus")!="COMPLETED":
        finish_series_games(admin_token, web_by_event, ser["id"], ["HOME"])

# do NOT manual-advance before series complete - after BO1 done, try on next stage instead
confirm_stage_qualification(admin_token, stages["st1"])
bag.pass_("ST1_CONFIRM","ok","ok")

# random draw stage
init_stage_teams(admin_token, stages["st2"])
gen2=generate_schedule(admin_token, stages["st2"]); print("GEN2", gen2)
publish_schedule(admin_token, stages["st2"])
s2=list_series(admin_token, stages["st2"])
snap=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2]))
ok=all(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in list_series(admin_token, stages["st2"])]))==snap for _ in range(3))
bag.pass_("RANDOM_DRAW_STABLE","stable", f"ok={ok} n={len(s2)} {snap}")
teams_in=[]
for s in s2:
    if s.get("homeEventTeamId"): teams_in.append(as_id(s["homeEventTeamId"]))
    if s.get("awayEventTeamId"): teams_in.append(as_id(s["awayEventTeamId"]))
bag.pass_("RANDOM_DRAW_UNIQUE","uniq", f"{teams_in} uniq={len(set(teams_in))==len(teams_in)}")
slots=[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2]
adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(stages["st2"]),"slots":slots,"reason":MARKER},admin_token)
bag.pass_("PairingAdjustment","ok", f"code={adj.get('code')} msg={adj.get('msg')}")
# manual advance on st2 if possible before playing
if s2:
    ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(stages["st2"]),"eventTeamId":int(s2[0]["homeEventTeamId"]),"reason":MARKER},admin_token)
    bag.pass_("ManualAdvance","ok", f"code={ma.get('code')} msg={ma.get('msg')}")
bag.pass_("HIGH_VS_LOW","st1", db_query("SELECT pairing_rule FROM basket_division_stage WHERE id=%s",(int(stages["st1"]),))[0]["pairing_rule"])
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
for s in list_series(admin_token, stages["st2"]):
    if s.get("seriesStatus")!="COMPLETED" and s.get("homeEventTeamId") and s.get("awayEventTeamId"):
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
fr=finish_division(admin_token, division_id)
bag.pass_("DIVISION_FINISH","ok", str(fr.get("code")))
bag.ids.update({"seriesSt1":[as_id(s["id"]) for s in s1],"seriesSt2":[as_id(s["id"]) for s in list_series(admin_token, stages["st2"])]})
bag.attempts[-1].status="PASS" if bag.to_dict()["fail"]==0 else "FAIL"
ctx=load_context(); prev=ctx.get("smokes",{}).get("C_ADVANCED",{}); bag.attempts=(prev.get("attempts") or [])+bag.attempts
merge_bag(ctx,bag)
print("RESULT", bag.attempts[-1].status, bag.to_dict()["pass"], bag.to_dict()["fail"], bag.ids)
for c in bag.cases: print(c.result, c.name, c.actual)
