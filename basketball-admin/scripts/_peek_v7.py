from full_smoke_common import *
# inspect unfinished V7 division if not finished wait - it was finished. Check stage status before finish from DB history
print(db_query("SELECT id, stage_code, execution_status, stage_status, confirmed_time FROM basket_division_stage WHERE division_id=149"))
print(db_query("SELECT id, match_status FROM basket_match WHERE stage_id=572 AND is_delete=0"))
# check-completion return data more carefully on a new run? 
# Read service for confirm prerequisites
