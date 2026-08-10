from full_smoke_common import *
# latest division
divs=db_query("SELECT id, division_name FROM basket_division WHERE division_name LIKE %s ORDER BY id DESC LIMIT 5", ("%ADVANCED%",))
print(divs)
did=divs[0]["id"]
stages=db_query("SELECT id, stage_code, best_of, pairing_rule, execution_status FROM basket_division_stage WHERE division_id=%s ORDER BY id",(did,))
print(stages)
sid=stages[-2]["id"] if len(stages)>1 else stages[0]["id"]
# find A1
a1=[s for s in stages if s["stage_code"]=="A1"][0]["id"]
print("matches", db_query("SELECT id, series_id, home_event_team_id, away_event_team_id, match_status FROM basket_match WHERE stage_id=%s AND is_delete=0",(a1,)))
print("series", db_query("SELECT id, stage_id, best_of, home_event_team_id, away_event_team_id, series_status FROM basket_match_series WHERE stage_id=%s AND is_delete=0",(a1,)))
print("series all div", db_query("SELECT id, stage_id, best_of, home_event_team_id, away_event_team_id, series_status FROM basket_match_series WHERE division_id=%s AND is_delete=0",(did,)))
tok=admin_login()
print("api series", req(f"{ADMIN}/basket/v4/match-series/stage/{a1}", token=tok))
