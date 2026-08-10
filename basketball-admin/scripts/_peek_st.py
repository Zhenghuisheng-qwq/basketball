from full_smoke_common import admin_login, req, ADMIN, list_matches, db_query
tok=admin_login()
m1=list_matches(tok,"142",549)
print("api matches st1", len(m1), "sample stageIds", set(str(x.get("stageId")) for x in m1[:10]), "series?", sum(1 for x in m1 if x.get("seriesId")))
print("db st1", db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id=549 AND is_delete=0")[0])
print("db st2", db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id=550 AND is_delete=0")[0])
print("db finished st1", db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id=549 AND match_status='FINISHED' AND is_delete=0")[0])
# stage team / qual
print(db_query("SELECT stage_id, stage_status, schedule_status, qualification_status FROM basket_division_stage WHERE id IN (549,550,551)"))
# try columns
print(db_query("SHOW COLUMNS FROM basket_division_stage"))
