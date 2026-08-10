from full_smoke_common import *
# B status
print("B stages", db_query("SELECT id, stage_code, stage_status, execution_status, confirmed_time FROM basket_division_stage WHERE division_id=143 ORDER BY id"))
print("B matches", db_query("SELECT stage_id, COUNT(*) c, SUM(match_status='FINISHED') f FROM basket_match WHERE division_id=143 AND is_delete=0 GROUP BY stage_id"))
print("B series", db_query("SELECT id, stage_id, series_status, best_of, winner_event_team_id FROM basket_match_series WHERE division_id=143 AND is_delete=0"))
print("B div", db_query("SELECT division_status FROM basket_division WHERE id=143"))
# C status  
print("C divs", db_query("SELECT id, division_name, division_status FROM basket_division WHERE division_name LIKE %s", ("%ADVANCED%",)))
