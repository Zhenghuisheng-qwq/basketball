import sys, json
sys.path.insert(0, r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts")
from full_smoke_common import *
divs = db_query(
    "SELECT id, division_name FROM basket_division WHERE division_name LIKE %s ORDER BY id DESC LIMIT 3",
    ("%PLAYOFF_V1_E2E_DIV_A16%",),
)
print("divs", divs)
did = divs[0]["id"]
stages = db_query(
    "SELECT id, stage_code, stage_name, stage_order, best_of, advancement_type, pairing_rule, next_stage_code, participant_count, execution_status FROM basket_division_stage WHERE division_id=%s AND is_delete=0 ORDER BY stage_order",
    (did,),
)
print(json.dumps(stages, default=str, indent=2, ensure_ascii=False))
for s in stages:
    m = db_query(
        "SELECT id, round_no, bracket_slot, home_event_team_id, away_event_team_id, match_status, winner_event_team_id FROM basket_match WHERE stage_id=%s AND is_delete=0 ORDER BY round_no, bracket_slot, id",
        (s["id"],),
    )
    print("stage", s["stage_code"], "matches", len(m))
    for row in m:
        print(" ", row)
    st = db_query(
        "SELECT seed_no, event_team_id, entry_type, source_rank FROM basket_division_stage_team WHERE stage_id=%s AND is_delete=0 ORDER BY seed_no",
        (s["id"],),
    )
    print("  teams", st)
