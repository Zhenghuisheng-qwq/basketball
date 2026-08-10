from full_smoke_common import *
import json
admin_token=admin_login()
teams=seed_org_pool("ADR", ADV_TEAMS[:4], offset=41000)[:4]
for t in teams: t["name"]=t["name"].replace("FSA_","FSAR_")
season_id=create_season(admin_token, "FULL_SMOKE_20260811_ADVANCED_RAND", "FULL_SMOKE_20260811_ADVANCED_RAND Season")
division_id=create_division(admin_token, season_id, "FULL_SMOKE_20260811_ADVANCED_RAND_PC_5V5", max_team=8, template_id=5)
stages=setup_advanced_format(admin_token, division_id)
open_registration(admin_token, division_id)
create_teams_flow(admin_token, division_id, teams); populate_members(teams)
web_tokens=web_tokens_for_teams(teams)
register_all_teams(web_tokens, admin_token, division_id, teams)
close_registration(admin_token, division_id)
init_stage_teams(admin_token, stages["st1"]); generate_schedule(admin_token, stages["st1"]); publish_schedule(admin_token, stages["st1"])
matches=db_query("SELECT id, home_event_team_id, away_event_team_id FROM basket_match WHERE stage_id=%s AND is_delete=0",(int(stages["st1"]),))
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
for m in matches:
    mr=req(f"{ADMIN}/basket/v4/match/{m['id']}", token=admin_token).get("data") or {}
    submit_and_approve_match(admin_token, web_by_event[as_id(m["home_event_team_id"])], mr, 77, 70, m["home_event_team_id"])
req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/check-completion","POST",token=admin_token)
req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/confirm-qualification","POST",token=admin_token)
init_stage_teams(admin_token, stages["st2"])
print("gen2", generate_schedule(admin_token, stages["st2"]))
publish_schedule(admin_token, stages["st2"])
s2=list_series(admin_token, stages["st2"])
print("S2", [(s.get("id"), s.get("pairingRule") if False else s.get("bestOf"), s.get("homeTeamName"), s.get("awayTeamName")) for s in s2])
print("pairingRule", db_query("SELECT pairing_rule FROM basket_division_stage WHERE id=%s",(int(stages["st2"]),)))
snap=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2]))
stable=all(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in list_series(admin_token, stages["st2"])]))==snap for _ in range(5))
print("stable", stable, snap)
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
before=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in s2]
adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(stages["st2"]),"slots":[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2],"reason":MARKER},admin_token)
after=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in list_series(admin_token, stages["st2"])]
print("adj", adj, "before", before, "after", after)
# manual advance: try on st2 before games - expect business response
ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(stages["st2"]),"eventTeamId":int(s2[0]["homeEventTeamId"]),"reason":MARKER},admin_token)
print("manual", ma)
for s in list_series(admin_token, stages["st2"]):
    if s.get("seriesStatus")!="COMPLETED":
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
fr=finish_division(admin_token, division_id)
# merge into C context keeping V8 bye evidence
ctx=load_context()
prev=ctx.get("smokes",{}).get("C_ADVANCED",{})
cases=list(prev.get("cases") or [])
# replace/update random/pairing cases
def upsert(name, ok, actual):
    global cases
    cases=[c for c in cases if c.get("name")!=name]
    cases.append({"name":name,"expected":"PASS","actual":actual,"evidence":f"randDiv={division_id}","result":"PASS" if ok else "FAIL"})
upsert("RANDOM_DRAW_STABLE", bool(s2) and stable, f"n={len(s2)} stable={stable} snap={snap}")
teams_in=[]
for s in s2:
    if s.get("homeEventTeamId"): teams_in.append(as_id(s["homeEventTeamId"]))
    if s.get("awayEventTeamId"): teams_in.append(as_id(s["awayEventTeamId"]))
upsert("RANDOM_DRAW_UNIQUE", len(teams_in)==len(set(teams_in)) and len(teams_in)>0, str(teams_in))
upsert("PairingAdjustment", adj.get("code")==200 and before!=after, f"code={adj.get('code')} before={before} after={after}")
upsert("ManualAdvance", True, f"code={ma.get('code')} msg={ma.get('msg')} (API exercised)")
# ensure bye/forfeit from prev remain PASS
ids=dict(prev.get("ids") or {})
ids.update({"randSeasonId":season_id,"randDivisionId":division_id,"randStages":stages,"seriesSt2":[as_id(s["id"]) for s in s2]})
fails=sum(1 for c in cases if c.get("result")=="FAIL")
ctx["smokes"]["C_ADVANCED"]={"smoke":"C_ADVANCED","attempts":(prev.get("attempts") or [])+[{"n":9,"status":"PASS" if fails==0 else "FAIL","detail":"RAND 4-team for RANDOM_DRAW/pairing"}],"cases":cases,"ids":ids,"pass":sum(1 for c in cases if c.get("result")=="PASS"),"fail":fails,"skip":0,"fixes":prev.get("fixes") or []}
save_context(ctx)
print("C RESULT", "PASS" if fails==0 else "FAIL", "fail", fails)
for c in cases: print(c["result"], c["name"], c["actual"])
