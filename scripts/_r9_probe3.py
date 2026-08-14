import sys, json
from pathlib import Path
ROOT = Path(r"d:\code\dz\basketball\scaffolding-v2")
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs
fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
token = fs.admin_login()
r = fs.req("http://127.0.0.1:18080/basket/v4/admin/stages/4/confirm-qualification", "POST", token=token)
print(repr(r.get("msg")))