# -*- coding: utf-8 -*-
import sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs
fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
email = "fs_ld_ld0601@cnpa.smoke"
fs.clear_login_locks([email])
tok = fs.web_login(email)
d = fs.req(f"{fs.WEB}/basket/v4/player/match-submission/328/detail?submitEventTeamId=79", token=tok)
print("DETAIL_KEYS", sorted((d.get("data") or {}).keys()) if isinstance(d.get("data"), dict) else d)
data = d.get("data") or {}
print("CTX", {k: data.get(k) for k in ("seasonTitle","divisionName","gameMode","lineupMinCount","lineupMaxCount","roundName","eligiblePlayers")})
print("ELIG_IN_DETAIL", len(data.get("eligiblePlayers") or []))
e = fs.req(f"{fs.WEB}/basket/v4/player/match-submission/328/eligible-players?submitEventTeamId=79", token=tok)
print("ELIG", e)
s = fs.req(f"{fs.WEB}/basket/v4/player/my-schedule?divisionId=27&includeFinished=true", token=tok)
print("SCHED_TYPE", type(s.get("data")).__name__, list((s.get("data") or {}).keys()) if isinstance(s.get("data"), dict) else str(s.get("data"))[:200])
