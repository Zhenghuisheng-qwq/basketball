from full_smoke_common import admin_login, req, ADMIN, db_query, list_series, get_series, finish_series_games, build_web_by_event, seed_org_pool, MAIN_TEAMS, web_tokens_for_teams, clear_login_locks
print(db_query("SELECT id, stage_code, stage_status, execution_status, confirmed_time, qualified_count FROM basket_division_stage WHERE division_id=142 ORDER BY id"))
tok=admin_login()
# confirm st2
cf=req(f"{ADMIN}/basket/v4/admin/stages/550/confirm-qualification","POST",token=tok)
print("confirm2", cf)
# init gen st3
for step in ("initialize-teams","generate-schedule","publish-schedule"):
    r=req(f"{ADMIN}/basket/v4/admin/stages/551/{step}","POST",token=tok)
    print(step, r.get("code"), r.get("msg"), r.get("data"))
s3=list_series(tok,551)
print("s3", s3)
