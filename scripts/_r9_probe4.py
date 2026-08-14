import sys
from pathlib import Path
ROOT = Path(r"d:\code\dz\basketball\scaffolding-v2")
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs
fs.ADMIN = "http://127.0.0.1:18080"
fs.DB["database"] = "basketball_r9_fresh"
token = fs.admin_login()
r = fs.req("http://127.0.0.1:18080/basket/v4/admin/stages/4/confirm-qualification", "POST", token=token)
Path(r"d:\code\dz\basketball\scaffolding-v2\scripts\_r9_msg.txt").write_text(str(r.get("msg") or ""), encoding="utf-8")
print("wrote")
st = fs.db_query("SELECT id, execution_status, confirmed_time FROM basket_division_stage WHERE id=4")
print(st)