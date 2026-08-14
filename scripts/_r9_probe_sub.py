import sys
from pathlib import Path
ROOT = Path(r"d:\code\dz\basketball\scaffolding-v2")
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs
fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
fs.ID_BASE = 2099111409000000000
fs.MARKER = "R9_TEST"
rows = fs.db_query("SELECT m.id, m.home_event_team_id h, m.away_event_team_id a, et.captain_member_id mid FROM basket_match m JOIN basket_event_team et ON et.id=m.home_event_team_id WHERE m.division_id=3 AND m.publish_status='PUBLISHED' AND m.is_delete=0 LIMIT 1")
print("match", rows)
acc = fs.db_query("SELECT email FROM basket_member_account WHERE id=%s", (int(rows[0]["mid"]),))
print("email", acc)
tok = fs.web_login(acc[0]["email"])
r = fs.req(f"{fs.WEB}/basket/v4/player/match-submission/{rows[0]['id']}/detail?submitEventTeamId={rows[0]['h']}", token=tok)
print("code", r.get("code"), "msg", r.get("msg"), "keys", list((r.get("data") or {}).keys())[:12])