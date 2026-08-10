import json
from pathlib import Path
from playwright.sync_api import sync_playwright
from full_smoke_common import load_context

ctx=load_context()
am=ctx["smokes"]["A_MAIN"]["ids"]
season=am["seasonId"]; division=am["divisionId"]
champ=am["championEventTeamId"]; final_m=am["finalMatchId"]; player=am["sourcePlayerId"]
base="http://127.0.0.1:5173"
routes=[
    "/",
    "/tournament",
    f"/events/{season}?divisionId={division}",
    f"/events/{season}?divisionId={division}&tab=schedule",
    f"/events/{season}?divisionId={division}&tab=stage",
    f"/tournament/team/{champ}",
    "/tournament/team/compare",
    f"/player/{player}",
    "/data",
    f"/events/match/{final_m}",
    "/hall-of-fame",
    "/announcements",
    "/partners",
    "/account/team",
    "/account/my-events",
    "/account/notifications",
    "/account/settings",
]
bad_tokens=["undefined","NaN","[object Object]","\u7cfb\u7edf\u5f02\u5e38","NullPointer","Cannot read"]
results=[]
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True)
    page=browser.new_page()
    for path in routes:
        url=base+path
        cons=[]; nets=[]
        def on_console(msg):
            if msg.type in ("error","warning"):
                cons.append({"type":msg.type,"text":msg.text[:240]})
        def on_resp(resp):
            if resp.status>=500 and "/web-api" in resp.url:
                nets.append({"status":resp.status,"url":resp.url[:200]})
        page.on("console", on_console)
        page.on("response", on_resp)
        st="PASS"; notes=[]
        try:
            resp=page.goto(url, wait_until="domcontentloaded", timeout=45000)
            page.wait_for_timeout(2000)
            body=page.locator("body").inner_text(timeout=10000)[:2500]
            if resp and resp.status>=400:
                st="FAIL"; notes.append(f"http={resp.status}")
            for t in bad_tokens:
                if t in body:
                    st="FAIL"; notes.append(f"body:{t}")
            uncaught=[c for c in cons if c["type"]=="error" and "Uncaught" in c["text"]]
            if uncaught:
                st="FAIL"; notes.append("console uncaught")
            if nets:
                st="FAIL"; notes.append(f"api500={len(nets)}")
            if path.startswith("/account") and ("\u767b\u5f55" in body or "login" in body.lower()):
                notes.append("login_wall")
            results.append({"route":path,"finalUrl":page.url,"status":st,"notes":notes,"console":cons[:5],"api500":nets[:3],"snippet":body[:180].replace("\n"," ")})
        except Exception as e:
            results.append({"route":path,"status":"FAIL","notes":[repr(e)[:200]],"console":[],"api500":[]})
    browser.close()

out={"executed":True,"results":results,"pass":sum(1 for r in results if r["status"]=="PASS"),"fail":sum(1 for r in results if r["status"]=="FAIL")}
Path("full_smoke_browser_20260811.json").write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding="utf-8")
print("BROWSER", out["pass"], out["fail"])
for r in results:
    print(r["status"], r["route"], "|".join(r.get("notes") or []))