import sys, json
sys.path.insert(0, r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts")
from full_smoke_common import *

DID = 171
EXPECTED_16 = [1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14]

stages = db_query(
    "SELECT id, stage_code, stage_name, stage_order, best_of, advancement_type, pairing_rule FROM basket_division_stage WHERE division_id=%s AND is_delete=0 ORDER BY stage_order",
    (DID,),
)
st1 = stages[0]["id"]
seeds = db_query(
    """SELECT st.seed_no seed, et.team_name_snapshot team, t.create_time createTime, t.id teamId, et.id eventTeamId
       FROM basket_division_stage_team st
       JOIN basket_event_team et ON et.id=st.event_team_id
       JOIN basket_team t ON t.id=et.source_team_id
       WHERE st.stage_id=%s AND st.is_delete=0 ORDER BY st.seed_no""",
    (st1,),
)
seed_by_et = {str(r["eventTeamId"]): int(r["seed"]) for r in seeds}
print("=== SEEDS ===")
for r in seeds:
    print(r["seed"], r["team"], r["createTime"], r["teamId"])

def units(stage_id):
    rows = db_query(
        "SELECT bracket_slot slot, home_event_team_id home, away_event_team_id away, winner_event_team_id winner FROM basket_match WHERE stage_id=%s AND is_delete=0 AND series_id IS NULL ORDER BY bracket_slot",
        (stage_id,),
    )
    if rows:
        return rows
    return db_query(
        "SELECT bracket_slot slot, home_event_team_id home, away_event_team_id away, winner_event_team_id winner, best_of FROM basket_match_series WHERE stage_id=%s AND is_delete=0 ORDER BY bracket_slot",
        (stage_id,),
    )

print("=== STAGE1 ===")
u1 = units(st1)
order=[]
for u in u1:
    hs=seed_by_et[str(u["home"])]; aws=seed_by_et[str(u["away"])]
    order.extend([hs, aws])
    print(u["slot"], hs, "vs", aws, "winnerSeed", seed_by_et.get(str(u["winner"])))
print("order_ok", order==EXPECTED_16)

print("=== STAGE2 ===")
for u in units(stages[1]["id"]):
    print(u["slot"], seed_by_et[str(u["home"])], "vs", seed_by_et[str(u["away"])], "W", seed_by_et.get(str(u["winner"])) if u["winner"] else None)

print("=== STAGE3 series ===")
for u in db_query("SELECT bracket_slot slot, home_event_team_id home, away_event_team_id away, winner_event_team_id winner, home_win_count, away_win_count, best_of FROM basket_match_series WHERE stage_id=%s AND is_delete=0 ORDER BY bracket_slot", (stages[2]["id"],)):
    print(u)

print("=== FINAL ===")
for u in db_query("SELECT bracket_slot slot, home_event_team_id home, away_event_team_id away, winner_event_team_id winner, home_win_count, away_win_count, best_of FROM basket_match_series WHERE stage_id=%s AND is_delete=0 ORDER BY bracket_slot", (stages[3]["id"],)):
    print(u)
    print("champ seed", seed_by_et.get(str(u["winner"])))

print("counts series", db_query("SELECT COUNT(*) c FROM basket_match_series WHERE stage_id IN (%s,%s,%s,%s) AND is_delete=0",(stages[0]["id"],stages[1]["id"],stages[2]["id"],stages[3]["id"]))[0])
print("counts match", db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id IN (%s,%s,%s,%s) AND is_delete=0",(stages[0]["id"],stages[1]["id"],stages[2]["id"],stages[3]["id"]))[0])
print("season", db_query("SELECT season_id FROM basket_division WHERE id=%s",(DID,))[0])
