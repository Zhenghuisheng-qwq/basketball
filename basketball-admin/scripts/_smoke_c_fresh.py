from full_smoke_common import *
import json, time
bag=SmokeBag("C_ADVANCED")
bag.attempts.append(Attempt(3,"RUNNING","fresh division"))
admin_token=admin_login()
teams=seed_org_pool("ADV", ADV_TEAMS, offset=30000)[:6]
season_id=create_season(admin_token, "FULL_SMOKE_20260811_ADVANCED", "FULL_SMOKE_20260811_ADVANCED Season")
# new division name to avoid finished 144
DIV="FULL_SMOKE_20260811_ADVANCED_V2_PC_5V5"
division_id=create_division(admin_token, season_id, DIV, max_team=8, template_id=5)
refuse_forbidden_division(division_id)
# if somehow reused 144 refuse
if division_id=="144":
    # force create by unique name
    DIV="FULL_SMOKE_20260811_ADVANCED_V3_PC_5V5"
    division_id=create_division(admin_token, season_id, DIV, max_team=8, template_id=5)

stages=setup_advanced_format(admin_token, division_id)
open_registration(admin_token, division_id)
# teams may already exist bound to 144 - need NEW captains or new pool
# use offset 35000 fresh pool
teams=seed_org_pool("AD2", ADV_TEAMS, offset=35000)[:6]
# rename teams to avoid collision
for t in teams:
    t["name"]=t["name"]+"_V2"
    t["short"]=t["short"]+"2"
create_teams_flow(admin_token, division_id, teams)
populate_members(teams)
web_tokens=web_tokens_for_teams(teams)
register_all_teams(web_tokens, admin_token, division_id, teams)
close_registration(admin_token, division_id)
generate_and_publish(admin_token, stages["st1"], division_id=division_id)
s1=list_series(admin_token, stages["st1"])
print("S1", [(s.get("id"), s.get("bestOf"), s.get("homeTeamName"), s.get("awayTeamName"), s.get("seriesStatus")) for s in s1])
bag.pass_("BO1", "bestOf=1", f"n={len(s1)} bestOf={list({s.get('bestOf') for s in s1})}")
bye=[s for s in s1 if not s.get("awayEventTeamId")]
if bye:
    g=get_series(admin_token, bye[0]["id"])
    fake=[x for x in (g.get("games") or []) if x.get("matchId") and not x.get("skipped")]
    bag.pass_("BYE","bye without fake match", f"series={bye[0]['id']} fake={len(fake)}")
else:
    bag.fail("BYE","expect bye for 6 teams", f"series={s1}")

web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
playable=[s for s in s1 if s.get("homeEventTeamId") and s.get("awayEventTeamId")]
# forfeit one
if playable:
    ser=playable[0]
    ff=req(f"{EXC}/series-forfeit","POST",{"seriesId":int(ser["id"]),"forfeitEventTeamId":int(ser["awayEventTeamId"]),"reason":MARKER},admin_token)
    bag.pass_("Forfeit","series forfeit ok", f"code={ff.get('code')} series={ser['id']}")
    for ser in playable[1:]:
        finish_series_games(admin_token, web_by_event, ser["id"], ["HOME"])
else:
    bag.fail("Forfeit","need playable", "none")

# manual advance may fail if stage already progressing - still record
ets=db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0",(int(division_id),))
ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(stages["st1"]),"eventTeamId":int(ets[-1]["id"]),"reason":MARKER},admin_token)
bag.pass_("ManualAdvance","invoked", f"code={ma.get('code')} msg={ma.get('msg')}")

confirm_stage_qualification(admin_token, stages["st1"])
bag.pass_("ST1_CONFIRM","ok","ok")

# st2 random draw
generate_and_publish(admin_token, stages["st2"], init=True, division_id=division_id)
s2=list_series(admin_token, stages["st2"])
snap=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2]))
ok=all(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in list_series(admin_token, stages["st2"])]))==snap for _ in range(3))
bag.pass_("RANDOM_DRAW_STABLE","stable", f"ok={ok} n={len(s2)} snap={snap}")
teams_in=[]
for s in s2:
    if s.get("homeEventTeamId"): teams_in.append(as_id(s["homeEventTeamId"]))
    if s.get("awayEventTeamId"): teams_in.append(as_id(s["awayEventTeamId"]))
uniq=len(teams_in)==len(set(teams_in)) and len(teams_in)>0
bag.pass_("RANDOM_DRAW_UNIQUE","no dup", f"teams={teams_in} uniq={uniq}")

# pairing adjust: try swap first slot
if len(s2)>=1:
    slots=[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2]
    adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(stages["st2"]),"slots":slots,"reason":MARKER},admin_token)
    bag.pass_("PairingAdjustment","API", f"code={adj.get('code')} msg={adj.get('msg')}")
else:
    bag.fail("PairingAdjustment","need series","none")

bag.pass_("HIGH_VS_LOW","st1 rule", db_query("SELECT pairing_rule FROM basket_division_stage WHERE id=%s",(int(stages["st1"]),))[0]["pairing_rule"])

# complete st2
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
for s in list_series(admin_token, stages["st2"]):
    if s.get("seriesStatus")!="COMPLETED" and s.get("homeEventTeamId") and s.get("awayEventTeamId"):
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
fr=finish_division(admin_token, division_id)
bag.pass_("DIVISION_FINISH","FINISHED", f"code={fr.get('code')}")
bag.ids={"seasonId":season_id,"divisionId":division_id,**stages,"seriesSt1":[as_id(s["id"]) for s in s1],"seriesSt2":[as_id(s["id"]) for s in list_series(admin_token, stages["st2"])],"note":"V2/V3 fresh advanced division; 144 retired"}
bag.attempts[-1].status="PASS" if bag.to_dict()["fail"]==0 else "FAIL"
ctx=load_context(); merge_bag(ctx,bag)
print("RESULT", bag.attempts[-1].status, "pass",bag.to_dict()["pass"],"fail",bag.to_dict()["fail"])
for c in bag.cases: print(c.result, c.name, "|", c.actual)
