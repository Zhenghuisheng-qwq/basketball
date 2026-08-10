from full_smoke_common import *
tok=admin_login()
# inspect 145
print(db_query("SELECT id, stage_code, competition_format, best_of, pairing_rule, execution_status, stage_status, participant_count FROM basket_division_stage WHERE division_id=145"))
print("event teams", db_query("SELECT id, team_name_snapshot FROM basket_event_team WHERE division_id=145 AND is_delete=0"))
print("stage teams", db_query("SELECT * FROM basket_division_stage_team WHERE stage_id IN (SELECT id FROM basket_division_stage WHERE division_id=145) LIMIT 20"))
# try generate again with full response
st1=db_query("SELECT id FROM basket_division_stage WHERE division_id=145 AND stage_code='A1'")[0]["id"]
print("st1", st1)
print("init", req(f"{ADMIN}/basket/v4/admin/stages/{st1}/initialize-teams","POST",token=tok))
print("gen", req(f"{ADMIN}/basket/v4/admin/stages/{st1}/generate-schedule","POST",token=tok))
print("series", list_series(tok, st1))
print("matches", db_query("SELECT id, match_code, home_event_team_id, away_event_team_id, series_id FROM basket_match WHERE stage_id=%s",(st1,)))
