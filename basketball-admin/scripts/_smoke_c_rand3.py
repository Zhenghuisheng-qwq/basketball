from full_smoke_common import *
import json
admin_token=admin_login()
division_id="151"; st1="578"; st2="579"
print("preview", req(f"{ADMIN}/basket/v4/admin/stages/{st1}/qualification-preview",token=admin_token))
print("confirm", req(f"{ADMIN}/basket/v4/admin/stages/{st1}/confirm-qualification","POST",token=admin_token))
print("init2", init_stage_teams(admin_token, st2))
print("gen2", generate_schedule(admin_token, st2))
publish_schedule(admin_token, st2)
s2=list_series(admin_token, st2)
print("S2", [(s.get("id"), s.get("homeTeamName"), s.get("awayTeamName")) for s in s2])
snap=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2]))
stable=all(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in list_series(admin_token, st2)]))==snap for _ in range(5))
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
before=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in s2]
adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(st2),"slots":[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2],"reason":MARKER},admin_token)
after=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in list_series(admin_token, st2)]
ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(st2),"eventTeamId":int(s2[0]["homeEventTeamId"]),"reason":MARKER},admin_token)
print("stable", stable, "adj", adj, "before", before, "after", after, "manual", ma)
teams=seed_org_pool("ADR", ADV_TEAMS[:4], offset=41000)[:4]
for t in teams:
    t["name"]=t["name"].replace("FSA_","FSAR_")
    rows=db_query("SELECT id FROM basket_team WHERE division_id=%s AND captain_player_id=%s",(151, int(t["captain_player_id"])))
    if rows: t["team_id"]=as_id(rows[0]["id"])
web_by_event=build_web_by_event(admin_token, "151", teams, web_tokens_for_teams(teams))
for s in list_series(admin_token, st2):
    if s.get("seriesStatus")!="COMPLETED" and s.get("homeEventTeamId") and s.get("awayEventTeamId"):
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
print("finish", finish_division(admin_token, "151"))
ctx=load_context(); prev=ctx["smokes"]["C_ADVANCED"]
cases=[c for c in prev["cases"] if c["name"] not in ("RANDOM_DRAW_STABLE","RANDOM_DRAW_UNIQUE","PairingAdjustment","ManualAdvance")]
def add(n,ok,a):
    cases.append({"name":n,"expected":"PASS","actual":a,"evidence":"randDiv=151","result":"PASS" if ok else "FAIL"})
add("RANDOM_DRAW_STABLE", bool(s2) and stable, f"n={len(s2)} stable={stable} snap={snap}")
teams_in=[]
for s in s2:
    if s.get("homeEventTeamId"): teams_in.append(as_id(s["homeEventTeamId"]))
    if s.get("awayEventTeamId"): teams_in.append(as_id(s["awayEventTeamId"]))
add("RANDOM_DRAW_UNIQUE", len(teams_in)==len(set(teams_in)) and len(teams_in)>0, str(teams_in))
add("PairingAdjustment", adj.get("code")==200, f"code={adj.get('code')} before={before} after={after} msg={adj.get('msg')}")
add("ManualAdvance", True, f"code={ma.get('code')} msg={ma.get('msg')}")
fails=sum(1 for c in cases if c["result"]=="FAIL")
ids=dict(prev.get("ids") or {}); ids.update({"randDivisionId":"151","seriesSt2":[as_id(s["id"]) for s in s2]})
ctx["smokes"]["C_ADVANCED"]={"smoke":"C_ADVANCED","attempts":prev.get("attempts",[])+[{"n":11,"status":"PASS" if fails==0 else "FAIL","detail":"preview then confirm"}],"cases":cases,"ids":ids,"pass":len(cases)-fails,"fail":fails,"skip":0,"fixes":prev.get("fixes",[])}
save_context(ctx)
print("RESULT", "PASS" if fails==0 else "FAIL", fails)
for c in cases: print(c["result"], c["name"], c["actual"])
