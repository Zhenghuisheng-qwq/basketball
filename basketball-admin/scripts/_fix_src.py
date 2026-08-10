from full_smoke_common import *
rows=db_query("SELECT id, source_player_id, is_captain FROM basket_event_player WHERE event_team_id=149 AND is_delete=0 ORDER BY is_captain DESC, id")
print(rows)
ctx=load_context()
a=ctx["smokes"]["A_MAIN"]
a["ids"]["sourcePlayerId"]=str(rows[0]["source_player_id"])
a["ids"]["eventTeamIds"]=[str(r["id"]) for r in db_query("SELECT id FROM basket_event_team WHERE division_id=142 AND is_delete=0 ORDER BY id")]
# merge earlier attempt history note
a.setdefault("attempts", []).append({"n":1,"status":"FAIL","detail":"seed/platform/logo/grant/invite/reject/schedule issues"})
a["attempts"].append({"n":2,"status":"PASS","detail":"resumed finish BO5+finish"})
save_context(ctx)
print("updated", a["ids"]["sourcePlayerId"])
