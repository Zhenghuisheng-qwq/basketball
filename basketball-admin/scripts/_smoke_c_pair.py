from full_smoke_common import *
import json
admin_token=admin_login()
teams=seed_org_pool("ADP", ADV_TEAMS[:4], offset=42000)[:4]
for t in teams: t["name"]=t["name"].replace("FSA_","FSAP_")
season_id=create_season(admin_token, "FULL_SMOKE_20260811_ADVANCED_PAIR", "FULL_SMOKE_20260811_ADVANCED_PAIR Season")
division_id=create_division(admin_token, season_id, "FULL_SMOKE_20260811_ADVANCED_PAIR_PC_5V5", max_team=8, template_id=5)
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
req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/qualification-preview",token=admin_token)
req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/confirm-qualification","POST",token=admin_token)
init_stage_teams(admin_token, stages["st2"])
print("gen", generate_schedule(admin_token, stages["st2"]))
# DO NOT publish yet
s2=list_series(admin_token, stages["st2"])
print("draft series", [(s.get("id"), s.get("homeTeamName"), s.get("awayTeamName"), s.get("seriesStatus")) for s in s2])
before=[(as_id(s.get("homeEventTeamId")), as_id(s.get("awayEventTeamId"))) for s in s2]
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(stages["st2"]),"slots":[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2],"reason":MARKER},admin_token)
after=[(as_id(s.get("homeEventTeamId")), as_id(s.get("awayEventTeamId"))) for s in list_series(admin_token, stages["st2"])]
print("adj", adj.get("code"), adj.get("msg"), "before", before, "after", after)
publish_schedule(admin_token, stages["st2"])
pub_after=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in list_series(admin_token, stages["st2"])]
print("published", pub_after)
for s in list_series(admin_token, stages["st2"]):
    if s.get("seriesStatus")!="COMPLETED":
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
finish_division(admin_token, division_id)
ok=adj.get("code")==200 and before and before!=after
ctx=load_context(); prev=ctx["smokes"]["C_ADVANCED"]
cases=[c for c in prev["cases"] if c["name"]!="PairingAdjustment"]
cases.append({"name":"PairingAdjustment","expected":"draft adjust persist","actual":f"code={adj.get('code')} before={before} after={after} published={pub_after}","evidence":f"pairDiv={division_id}","result":"PASS" if ok else "FAIL"})
fails=sum(1 for c in cases if c["result"]=="FAIL")
ids=dict(prev.get("ids") or {}); ids["pairDivisionId"]=division_id; ids["pairSeriesId"]=as_id(s2[0]["id"]) if s2 else None
ctx["smokes"]["C_ADVANCED"]={"smoke":"C_ADVANCED","attempts":prev.get("attempts",[])+[{"n":12,"status":"PASS" if fails==0 else "FAIL","detail":"pairing adjust before publish"}],"cases":cases,"ids":ids,"pass":len(cases)-fails,"fail":fails,"skip":0,"fixes":(prev.get("fixes") or [])+[{"issue":"pairing-adjust only in draft before publish","note":"documented"}]}
save_context(ctx)
print("C FINAL", "PASS" if fails==0 else "FAIL", fails)
for c in cases: print(c["result"], c["name"])
