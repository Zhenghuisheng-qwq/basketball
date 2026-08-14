import sys
from pathlib import Path
ROOT = Path(r"d:\code\dz\basketball\scaffolding-v2")
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs
fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
fs.ID_BASE = 2099111409000000000
print("db", fs.DB["database"])
rows = fs.db_query("SELECT id, home_event_team_id, away_event_team_id, series_status, winner_event_team_id, best_of FROM basket_match_series WHERE stage_id=%s AND is_delete=0", (2,))
print("n", len(rows))
for r in rows:
    print(r)
    print(" keys", list(r.keys()))
games = fs.db_query("SELECT id, series_id, series_game_no, match_status, home_event_team_id FROM basket_match WHERE series_id=15 AND is_delete=0")
print("games15", games)
print("cup msg sample")
token = fs.admin_login()
r = fs.req("http://127.0.0.1:18080/basket/v4/admin/stages/4/confirm-qualification", "POST", token=token)
print(r)