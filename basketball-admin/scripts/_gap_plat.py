from full_smoke_common import *
import json, time
admin = admin_login()
creds = json.load(open("gap_creds.json", encoding="utf-8"))
tok = web_login(creds["email"])
pl = req(f"{WEB}/basket/player/platform-account/list", token=tok)
pc = [x for x in (pl.get("data") or []) if x.get("platform") == "PC" and x.get("platformAccountId")][0]
print("PC", pc)
pf = upload_file(tok, TINY_PNG, filename="chg.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
chg = req(
    f"{WEB}/basket/player/platform-account/{pc['platformAccountId']}/change-game-id",
    "POST",
    {"newGameId": f"NEWGID{int(time.time())%10000}", "proofFileId": str(pf["data"]["fileId"])},
    tok,
)
print("CHG", chg.get("code"), chg.get("msg"), chg.get("data"))
cid = str((chg.get("data") or {}).get("changeId") or (chg.get("data") or {}).get("id") or "")
if not cid:
    lr = req(f"{ADMIN}/basket/playerPlatformChange/list?pageNum=1&pageSize=10", token=admin)
    print("rows", [(r.get("id"), r.get("auditStatus"), r.get("newGameId"), r.get("memberId")) for r in (lr.get("rows") or [])[:8]])
    for r in lr.get("rows") or []:
        if str(r.get("memberId")) == str(creds["memberId"]) or str(r.get("playerId")) == str(creds.get("playerId")):
            cid = str(r.get("id"))
            break
pap = req(f"{ADMIN}/basket/playerPlatformChange/{cid}/approve", "POST", {"auditRemark": MARKER}, admin) if cid else {}
print("PAP", cid, pap.get("code"), pap.get("msg"))
pl2 = req(f"{WEB}/basket/player/platform-account/list", token=tok)
print("AFTER", [x for x in (pl2.get("data") or []) if x.get("platform") == "PC"])
hist = req(f"{WEB}/basket/player/platform-account/change-history?currentPage=1&pageSize=5", token=tok)
print("HIST", hist.get("code"), str(hist.get("data"))[:400])
creds["platformChangeId"] = cid
creds["platformAfter"] = [x for x in (pl2.get("data") or []) if x.get("platform") == "PC"]
json.dump(creds, open("gap_creds.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)