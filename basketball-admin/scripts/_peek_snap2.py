from full_smoke_common import admin_login, req, ADMIN, sorted_stages
tok=admin_login()
snap=req(f"{ADMIN}/basket/v4/divisions/142/format-snapshot", token=tok)
data=snap.get("data") or {}
print("code", snap.get("code"), "msg", snap.get("msg"))
print("top keys", list(data.keys()))
for k in ("snapshotStatus","lockStatus","locked","publishStatus","status"):
    if k in data: print(k, data[k])
stages=sorted_stages(data)
print("stages", [(s.get("id"), s.get("stageCode"), s.get("stageName"), s.get("competitionFormat"), s.get("bestOf"), s.get("pairingRule")) for s in stages])
