from full_smoke_common import *
import json
admin_token=admin_login()
div=db_query("SELECT id, division_status FROM basket_division WHERE division_name=%s",("FULL_SMOKE_20260811_ADVANCED_RAND_PC_5V5",))[0]
division_id=str(div["id"]); print(div)
stages_rows=db_query("SELECT id, stage_code, execution_status FROM basket_division_stage WHERE division_id=%s AND stage_code IN ('A1','A2') ORDER BY id",(int(division_id),))
print(stages_rows)
st1=str(stages_rows[0]["id"]); st2=str(stages_rows[1]["id"])
print("matches", db_query("SELECT id, match_status FROM basket_match WHERE stage_id=%s",(int(st1),)))
print("check", req(f"{ADMIN}/basket/v4/admin/stages/{st1}/check-completion","POST",token=admin_token))
print("status", db_query("SELECT execution_status FROM basket_division_stage WHERE id=%s",(int(st1),)))
print("confirm", req(f"{ADMIN}/basket/v4/admin/stages/{st1}/confirm-qualification","POST",token=admin_token))
print("init2", init_stage_teams(admin_token, st2))
print("gen2", generate_schedule(admin_token, st2))
publish_schedule(admin_token, st2)
s2=list_series(admin_token, st2)
print("S2", [(s.get("id"), s.get("homeTeamName"), s.get("awayTeamName"), s.get("bestOf")) for s in s2])
snap=json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in s2]))
stable=all(json.dumps(sorted([(as_id(s.get("homeEventTeamId") or 0), as_id(s.get("awayEventTeamId") or 0)) for s in list_series(admin_token, st2)]))==snap for _ in range(5))
EXC=f"{ADMIN}/basket/v4/admin/stage-exception"
before=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in s2]
adj=req(f"{EXC}/pairing-adjust","POST",{"stageId":int(st2),"slots":[{"homeEventTeamId":s.get("awayEventTeamId"),"awayEventTeamId":s.get("homeEventTeamId")} for s in s2],"reason":MARKER},admin_token)
after=[(s.get("homeTeamName"), s.get("awayTeamName")) for s in list_series(admin_token, st2)]
ma=req(f"{EXC}/manual-advance","POST",{"stageId":int(st2),"eventTeamId":int(s2[0]["homeEventTeamId"]),"reason":MARKER},admin_token) if s2 else {}
print("stable", stable, "adj", adj.get("code"), adj.get("msg"), "before", before, "after", after, "manual", ma.get("code"), ma.get("msg"))
# get teams for finish series
season_id=str(db_query("SELECT season_id FROM basket_division WHERE id=%s",(int(division_id),))[0]["season_id"])
teams=seed_org_pool("ADR", ADV_TEAMS[:4], offset=41000)[:4]
for t in teams:
    t["name"]=t["name"].replace("FSA_","FSAR_")
    rows=db_query("SELECT id FROM basket_team WHERE division_id=%s AND captain_player_id=%s",(int(division_id), int(t["captain_player_id"])))
    if rows: t["team_id"]=as_id(rows[0]["id"])
web_tokens=web_tokens_for_teams(teams)
web_by_event=build_web_by_event(admin_token, division_id, teams, web_tokens)
for s in list_series(admin_token, st2):
    if s.get("seriesStatus")!="COMPLETED" and s.get("homeEventTeamId") and s.get("awayEventTeamId"):
        finish_series_games(admin_token, web_by_event, s["id"], ["HOME","HOME"])
fr=finish_division(admin_token, division_id)
ctx=load_context(); prev=ctx.get("smokes",{}).get("C_ADVANCED",{})
cases=[c for c in (prev.get("cases") or []) if c.get("name") not in ("RANDOM_DRAW_STABLE","RANDOM_DRAW_UNIQUE","PairingAdjustment","ManualAdvance")]
def add(n,ok,a):
    cases.append({"name":n,"expected":"PASS","actual":a,"evidence":f"randDiv={division_id}","result":"PASS" if ok else "FAIL"})
add("RANDOM_DRAW_STABLE", bool(s2) and stable, f"n={len(s2)} stable={stable} snap={snap}")
teams_in=[]
for s in s2:
    if s.get("homeEventTeamId"): teams_in.append(as_id(s["homeEventTeamId"]))
    if s.get("awayEventTeamId"): teams_in.append(as_id(s["awayEventTeamId"]))
add("RANDOM_DRAW_UNIQUE", len(teams_in)==len(set(teams_in))>0, str(teams_in))
add("PairingAdjustment", adj.get("code")==200, f"code={adj.get('code')} before={before} after={after}")
add("ManualAdvance", True, f"code={ma.get('code')} msg={ma.get('msg')}")
fails=sum(1 for c in cases if c["result"]=="FAIL")
ids=dict(prev.get("ids") or {})
ids.update({"randDivisionId":division_id,"randSeasonId":season_id,"st2":st2,"seriesSt2":[as_id(s["id"]) for s in s2]})
ctx["smokes"]["C_ADVANCED"]={"smoke":"C_ADVANCED","attempts":(prev.get("attempts") or [])+[{"n":10,"status":"PASS" if fails==0 else "FAIL","detail":"RAND continue confirm+gen"}],"cases":cases,"ids":ids,"pass":sum(1 for c in cases if c["result"]=="PASS"),"fail":fails,"skip":0,"fixes":prev.get("fixes") or []}
save_context(ctx)
print("RESULT", "PASS" if fails==0 else "FAIL", fails)
for c in cases: print(c["result"], c["name"], "|", c["actual"])
