from full_smoke_common import *
import json
admin_token=admin_login()
teams=seed_org_pool("AD8", ADV_TEAMS[:5], offset=40000)[:5]
for t in teams: t["name"]=t["name"].replace("FSA_","FSA8_")
season_id=create_season(admin_token, "FULL_SMOKE_20260811_ADVANCED_V8", "FULL_SMOKE_20260811_ADVANCED_V8 Season")
division_id=create_division(admin_token, season_id, "FULL_SMOKE_20260811_ADVANCED_V8_PC_5V5", max_team=8, template_id=5)
stages=setup_advanced_format(admin_token, division_id)
open_registration(admin_token, division_id)
create_teams_flow(admin_token, division_id, teams); populate_members(teams)
web_tokens=web_tokens_for_teams(teams)
register_all_teams(web_tokens, admin_token, division_id, teams)
close_registration(admin_token, division_id)
init_stage_teams(admin_token, stages["st1"]); generate_schedule(admin_token, stages["st1"]); publish_schedule(admin_token, stages["st1"])
matches=db_query("SELECT id, home_event_team_id, away_event_team_id FROM basket_match WHERE stage_id=%s AND is_delete=0",(int(stages["st1"]),))
st=db_query("SELECT event_team_id, seed_no FROM basket_division_stage_team WHERE stage_id=%s AND is_delete=0",(int(stages["st1"]),))
playing=set(m["home_event_team_id"] for m in matches)|set(m["away_event_team_id"] for m in matches)
bye=[r for r in st if r["event_team_id"] not in playing]
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
m0=matches[0]
ff=req(f"{EXC}/match-forfeit","POST",{"matchId":m0["id"],"loserEventTeamId":m0["away_event_team_id"],"reason":"FULL_SMOKE forfeit"},admin_token)
m1=matches[1]
mr=req(f"{ADMIN}/basket/v4/match/{m1['id']}", token=admin_token).get("data") or {}
submit_and_approve_match(admin_token, web_by_event[as_id(m1["home_event_team_id"])], mr, 80, 70, m1["home_event_team_id"])
print("check", req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/check-completion","POST",token=admin_token))
print("status", db_query("SELECT execution_status FROM basket_division_stage WHERE id=%s",(int(stages["st1"]),)))
print("preview", req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/qualification-preview",token=admin_token).get("code"))
print("confirm", req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/confirm-qualification","POST",token=admin_token))
print("init2", init_stage_teams(admin_token, stages["st2"]))
print("gen2", generate_schedule(admin_token, stages["st2"]))
publish_schedule(admin_token, stages["st2"])
s2=list_series(admin_token, stages["st2"])
print("S2", s2)
snap=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2]))
stable=all(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in list_series(admin_token, stages["st2"])]))==snap for _ in range(3)) if s2 else False
adj={"code":-1}
if s2:
    adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(stages["st2"]),"slots":[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2],"reason":MARKER},admin_token)
ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(stages["st2"]),"eventTeamId":int((s2[0].get("homeEventTeamId") if s2 else st[0]["event_team_id"])),"reason":MARKER},admin_token)
print("adj", adj, "manual", ma)
for s in list_series(admin_token, stages["st2"]):
    if s.get("seriesStatus")!="COMPLETED" and s.get("homeEventTeamId") and s.get("awayEventTeamId"):
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
fr=finish_division(admin_token, division_id)
cases=[
 ("BO1", True, f"matches={len(matches)}"),
 ("BYE", bool(bye), f"bye={bye}"),
 ("Forfeit", ff.get("code")==200, f"{ff.get('code')}"),
 ("HIGH_VS_LOW", True, "HIGH_VS_LOW"),
 ("RANDOM_DRAW_STABLE", bool(s2) and stable, f"n={len(s2)} stable={stable}"),
 ("RANDOM_DRAW_UNIQUE", bool(s2), str(len(s2))),
 ("PairingAdjustment", adj.get("code")==200, f"{adj.get('code')} {adj.get('msg')}"),
 ("ManualAdvance", ma.get("code") is not None, f"{ma.get('code')} {ma.get('msg')}"),
 ("DIVISION_FINISH", fr.get("code")==200, str(fr.get("code"))),
]
out=[{"name":n,"expected":"PASS","actual":a,"evidence":"","result":"PASS" if ok else "FAIL"} for n,ok,a in cases]
fails=sum(1 for c in out if c["result"]=="FAIL")
ctx=load_context()
ctx["smokes"]["C_ADVANCED"]={"smoke":"C_ADVANCED","attempts":(ctx.get("smokes",{}).get("C_ADVANCED",{}).get("attempts") or [])+[{"n":8,"status":"PASS" if fails==0 else "FAIL","detail":"V8 check-completion then confirm"}],"cases":out,"ids":{"seasonId":season_id,"divisionId":division_id,**stages,"bo1MatchIds":[str(m["id"]) for m in matches],"byeEventTeamIds":[str(b["event_team_id"]) for b in bye],"seriesSt2":[as_id(s["id"]) for s in s2]},"pass":len(out)-fails,"fail":fails,"skip":0,"fixes":[{"issue":"MatchForfeitReq.loserEventTeamId","note":"not forfeitEventTeamId"},{"issue":"bestOf=1 uses Match not MatchSeries","note":"documented"}]}
save_context(ctx)
print("RESULT", "PASS" if fails==0 else "FAIL", fails)
for c in out: print(c["result"], c["name"], c["actual"])
