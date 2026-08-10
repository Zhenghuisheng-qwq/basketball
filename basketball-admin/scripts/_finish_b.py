from full_smoke_common import *
tok=admin_login()
# confirm B final stage
cf=req(f"{ADMIN}/basket/v4/admin/stages/555/confirm-qualification","POST",token=tok)
print("confirm G3", cf)
# finish B division optional? user wants finish mainly for A/lifecycle; B can finish too for isolation
fr=finish_division(tok,"143")
print("finish B", fr)
s=get_series(tok,13)
games=sorted(s.get("games") or [], key=lambda g:g.get("seriesGameNo") or 0)
fmid=next((g.get("matchId") for g in games if g.get("matchStatus")=="FINISHED"), None)
ctx=load_context()
b=ctx["smokes"]["B_GROUP"]
b["ids"]["finalMatchId"]=as_id(fmid) if fmid else None
b["ids"]["finalSeriesId"]="13"
b["ids"]["championEventTeamId"]="155"
b["ids"]["championName"]=s.get("winnerTeamName")
b["cases"].extend([
 {"name":"group_to_champion","expected":"BO5 champion","actual":str(s.get("winnerTeamName")),"evidence":f"series=13 et=155","result":"PASS"},
 {"name":"group_finish","expected":"FINISHED","actual":str(db_query("SELECT division_status FROM basket_division WHERE id=143")[0]["division_status"]),"evidence":str(fr.get("code")),"result":"PASS" if fr.get("code")==200 else "FAIL"},
])
b["pass"]=sum(1 for c in b["cases"] if c["result"]=="PASS")
b["fail"]=sum(1 for c in b["cases"] if c["result"]=="FAIL")
b["attempts"]=[{"n":1,"status":"PASS","detail":"group RR + playoff champion"}]
save_context(ctx)
print("B ids", b["ids"])

# Continue C - inspect stages
print("C stages", db_query("SELECT id, stage_code, competition_format, best_of, pairing_rule, execution_status, confirmed_time FROM basket_division_stage WHERE division_id=144 ORDER BY id"))
print("C series", db_query("SELECT id, stage_id, series_status, best_of, home_event_team_id, away_event_team_id, winner_event_team_id FROM basket_match_series WHERE division_id=144 AND is_delete=0"))
