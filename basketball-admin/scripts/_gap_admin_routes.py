from playwright.sync_api import sync_playwright
import json
from pathlib import Path

paths = [
  "/index",
  "/event/series",
  "/event/season",
  "/event/division",
  "/event/captain-grant",
  "/event/registration-audit",
  "/event/event-team",
  "/competition-exec/event-v4-schedule",
  "/competition-exec/event-v4-match",
  "/competition-exec/event-v4-match-submission-audit",
  "/identity/player-certification",
  "/identity/platform-change",
  "/identity/team-profile-change",
  "/identity/member-profile-change",
]
# also try discovering from sidebar hrefs after login
with sync_playwright() as p:
    b=p.chromium.launch(headless=True)
    page=b.new_page()
    page.goto("http://127.0.0.1:5174/login", wait_until="networkidle")
    page.locator("input[type='text'], input[placeholder*='用户']").first.fill("admin")
    page.locator("input[type='password']").first.fill("admin123")
    if page.locator("input[placeholder*='验证码']").count():
        page.locator("input[placeholder*='验证码']").first.fill("1")
    page.locator("button[type='submit'], button:has-text('登录'), button:has-text('登 录')").first.click()
    page.wait_for_timeout(3000)
    # collect hrefs
    hrefs=page.eval_on_selector_all("a", "els=>els.map(e=>e.getAttribute('href')).filter(Boolean)")
    hrefs=sorted(set([h for h in hrefs if h and h.startswith('/')]))
    print("HREFS", hrefs[:80])
    results=[]
    # prefer discovered event-related
    candidates=[h for h in hrefs if any(k in h for k in ["event","competition","identity","captain","registration","schedule","match","division","season","series","submission","profile","grant","team"])]
    if not candidates:
        candidates=paths
    for path in candidates:
        try:
            resp=page.goto("http://127.0.0.1:5174"+path, wait_until="domcontentloaded", timeout=30000)
            page.wait_for_timeout(1200)
            body=page.locator("body").inner_text(timeout=10000)
            st="PASS"
            notes=[]
            if "系统异常" in body or "Cannot read" in body: st="FAIL"; notes.append("broken")
            if "login" in page.url.lower(): st="FAIL"; notes.append("login")
            results.append({"route":path,"status":st,"notes":notes,"url":page.url,"snippet":body[:140].replace("\n"," ")})
            print(st, path)
        except Exception as e:
            results.append({"route":path,"status":"FAIL","notes":[repr(e)[:120]]})
            print("FAIL", path, e)
    b.close()

gap=json.loads(Path("full_smoke_gap_closure_20260811.json").read_text(encoding="utf-8"))
# keep account browser, replace admin
gap["browser_admin"]={"results":results,"pass":sum(1 for r in results if r["status"]=="PASS"),"fail":sum(1 for r in results if r["status"]=="FAIL"),"discovered_hrefs":hrefs[:100]}
gap["cases"]=[c for c in gap.get("cases",[]) if c.get("name")!="admin_ui_browser"]
gap["cases"].append({"name":"admin_ui_browser","expected":"core menus via route","actual":f"pass={gap['browser_admin']['pass']} fail={gap['browser_admin']['fail']}","evidence":"direct route navigation after login","result":"PASS" if gap["browser_admin"]["fail"]==0 else "FAIL"})
Path("full_smoke_gap_closure_20260811.json").write_text(json.dumps(gap,ensure_ascii=False,indent=2),encoding="utf-8")
print("ADM", gap["browser_admin"]["pass"], gap["browser_admin"]["fail"])