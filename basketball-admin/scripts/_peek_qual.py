from full_smoke_common import admin_login, req, ADMIN, db_query, list_series, get_series, finish_series_games, web_login, build_web_by_event, MAIN_TEAMS, seed_org_pool
print(db_query("SHOW COLUMNS FROM basket_division_stage"))
print(db_query("SELECT id, stage_code, stage_name, stage_status FROM basket_division_stage WHERE division_id=142"))
# try confirm already done?
tok=admin_login()
for sid in (549,550,551):
    cc=req(f"{ADMIN}/basket/v4/admin/stages/{sid}/check-completion","POST",token=tok)
    print("check", sid, cc.get("code"), cc.get("msg"), cc.get("data"))
    pv=req(f"{ADMIN}/basket/v4/admin/stages/{sid}/qualification-preview",token=tok)
    print("preview", sid, pv.get("code"), pv.get("msg"))
s2=list_series(tok,550)
print("s2", [(s["id"], s.get("seriesStatus")) for s in s2])
