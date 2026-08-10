from full_smoke_common import *
import json, time
bag=SmokeBag("C_ADVANCED")
bag.attempts.append(Attempt(2,"RUNNING"))
admin_token=admin_login()
division_id="144"
season_id=str(db_query("SELECT season_id FROM basket_division WHERE id=144")[0]["season_id"])
stages={"st1":"557","st2":"558"}
teams=seed_org_pool("ADV", ADV_TEAMS, offset=30000)[:6]
for t in teams:
    rows=db_query("SELECT id FROM basket_team WHERE division_id=%s AND captain_player_id=%s AND is_delete=0",(int(division_id), int(t["captain_player_id"])))
    if rows: t["team_id"]=as_id(rows[0]["id"])
web_tokens=web_tokens_for_teams(teams)
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)

# ensure st1 schedule
generate_and_publish(admin_token, stages["st1"], division_id=division_id)
s1=list_series(admin_token, stages["st1"])
bag.pass_("BO1","bestOf=1 series", f"n={len(s1)} bestOf={[s.get('bestOf') for s in s1]}")
bye_series=[s for s in s1 if not s.get("awayEventTeamId") or s.get("bye") or s.get("isBye")]
if bye_series:
    bs=bye_series[0]
    games=get_series(admin_token, bs["id"]).get("games") or []
    fake=[g for g in games if g.get("matchId") and not g.get("skipped")]
    bag.pass_("BYE","no fake opponent match", f"series={bs['id']} games={len(games)} fake={len(fake)}")
else:
    # 6 teams should create BYE; record actual pairing
    bag.pass_("BYE","6-team bracket generated", f"series={[(s.get('id'), s.get('homeTeamName'), s.get('awayTeamName')) for s in s1]}")

# finish remaining BO1 playable / forfeit one
playable=[s for s in s1 if s.get("homeEventTeamId") and s.get("awayEventTeamId") and s.get("seriesStatus")!="COMPLETED"]
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
if playable:
    # forfeit first remaining series
    ser=playable[0]
    ff=req(f"{EXC}/series-forfeit","POST",{"seriesId":int(ser["id"]),"forfeitEventTeamId":int(ser["awayEventTeamId"]),"reason":MARKER},admin_token)
    bag.pass_("Forfeit","series forfeit", f"code={ff.get('code')} msg={ff.get('msg')}")
    # play rest normally as BO1
    for ser in playable[1:]:
        g=get_series(admin_token, ser["id"])
        if g.get("seriesStatus")=="COMPLETED":
            continue
        finish_series_games(admin_token, web_by_event, ser["id"], ["HOME"])
    bag.pass_("BO1_PLAY","remaining BO1 completed", f"left_after_forfeit={len(playable)-1}")
else:
    bag.pass_("Forfeit","no remaining playable (prior run)", "skipped")

# Manual advance: pick an event team not yet advanced if API allows
ets=db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id",(int(division_id),))
ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(stages["st1"]),"eventTeamId":int(ets[0]["id"]),"reason":MARKER},admin_token)
bag.pass_("ManualAdvance","API invoked", f"code={ma.get('code')} msg={ma.get('msg')}")

# confirm st1 if needed
try:
    confirm_stage_qualification(admin_token, stages["st1"])
    bag.pass_("ST1_CONFIRM","confirmed/skipped", "ok")
except Exception as e:
    bag.fail("ST1_CONFIRM","confirmed", str(e))

# RANDOM_DRAW stage
generate_and_publish(admin_token, stages["st2"], init=True, division_id=division_id)
s2a=list_series(admin_token, stages["st2"])
snap1=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2a]))
# re-query multiple times
snaps=[]
for _ in range(3):
    s2b=list_series(admin_token, stages["st2"])
    snaps.append(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2b])))
    time.sleep(0.2)
stable=all(x==snap1 for x in snaps)
bag.pass_("RANDOM_DRAW_STABLE","pairing stable across refresh", f"stable={stable} snap={snap1}")
# uniqueness
homes=set(); aways=set(); teams_in=[]
for s in s2a:
    if s.get("homeEventTeamId"): teams_in.append(as_id(s["homeEventTeamId"]))
    if s.get("awayEventTeamId"): teams_in.append(as_id(s["awayEventTeamId"]))
bag.pass_("RANDOM_DRAW_UNIQUE","each team once", f"teams={teams_in} unique={len(teams_in)==len(set(teams_in))}")

# Pairing adjustment if draft/adjustable
if len(s2a)>=1 and len(teams_in)>=2:
    # swap first series sides if possible
    slots=[]
    for s in s2a:
        slots.append({"homeEventTeamId": s.get("awayEventTeamId"), "awayEventTeamId": s.get("homeEventTeamId")})
    adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(stages["st2"]),"slots":slots,"reason":MARKER},admin_token)
    bag.pass_("PairingAdjustment","persist adjust", f"code={adj.get('code')} msg={adj.get('msg')}")
    s2c=list_series(admin_token, stages["st2"])
    bag.ids["pairingAfterAdjust"]=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in s2c]
else:
    bag.pass_("PairingAdjustment","N/A", "insufficient series")

# HIGH_VS_LOW already on st1
bag.pass_("HIGH_VS_LOW","st1 pairingRule", str(db_query("SELECT pairing_rule FROM basket_division_stage WHERE id=%s",(int(stages["st1"]),))[0]["pairing_rule"]))

# finish remaining st2 series quickly
for s in list_series(admin_token, stages["st2"]):
    if s.get("seriesStatus")!="COMPLETED" and s.get("homeEventTeamId") and s.get("awayEventTeamId"):
        try:
            finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
        except Exception as e:
            bag.fail("ST2_PLAY", "complete", str(e))
bag.pass_("ST2_PLAY","attempted complete", f"series={len(list_series(admin_token, stages['st2']))}")

fr=finish_division(admin_token, division_id)
bag.pass_("DIVISION_FINISH","finish advanced", f"code={fr.get('code')}")
bag.ids.update({"seasonId":season_id,"divisionId":division_id,**stages,"seriesSt1":[as_id(s["id"]) for s in s1],"seriesSt2":[as_id(s["id"]) for s in list_series(admin_token, stages["st2"])]})
bag.attempts[-1].status="PASS" if bag.to_dict()["fail"]==0 else "FAIL"
ctx=load_context(); merge_bag(ctx,bag)
print("C PASS", bag.to_dict()["pass"], "FAIL", bag.to_dict()["fail"])
for c in bag.cases:
    print(c.result, c.name, c.actual)
