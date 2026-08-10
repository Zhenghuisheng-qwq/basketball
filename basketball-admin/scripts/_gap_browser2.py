# -*- coding: utf-8 -*-
from full_smoke_common import *
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

creds = json.load(open("gap_creds.json", encoding="utf-8"))
tok = web_login(creds["email"])
# refresh session payload
sess = req(f"{WEB}/basket/member/auth/session", token=tok)
session_data = sess.get("data") or {}

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("http://127.0.0.1:5173/login", wait_until="networkidle", timeout=60000)
    page.wait_for_timeout(800)
    page.evaluate(
        """({token, session}) => {
          localStorage.setItem('cnpa_web_token', token);
          sessionStorage.setItem('cnpa_web_token', token);
          localStorage.setItem('cnpa_web_auth_session', JSON.stringify(session));
          sessionStorage.setItem('cnpa_web_auth_session', JSON.stringify(session));
        }""",
        {"token": tok, "session": session_data},
    )
    # UI login as well
    emails = page.locator("input[type='email'], input[placeholder*='邮箱'], input[placeholder*='邮件']")
    if emails.count():
        emails.first.fill(creds["email"])
    else:
        page.locator("input").first.fill(creds["email"])
    page.locator("input[type='password']").first.fill(creds["password"])
    page.locator("button[type='submit'], button:has-text('登录')").first.click()
    page.wait_for_timeout(3000)
    # re-inject after login in case cleared
    page.evaluate(
        """({token, session}) => {
          localStorage.setItem('cnpa_web_token', token);
          localStorage.setItem('cnpa_web_auth_session', JSON.stringify(session));
        }""",
        {"token": tok, "session": session_data},
    )
    page.reload(wait_until="networkidle")
    page.wait_for_timeout(1500)

    acc = []
    for path in ["/account", "/account/player", "/account/team", "/account/my-events", "/account/notifications", "/account/settings", "/account/profile"]:
        cons = []
        nets = []
        def on_c(m):
            if m.type == "error":
                cons.append(m.text[:200])
        def on_r(r):
            if r.status >= 500 and "/web-api" in r.url:
                nets.append(r.url[:160])
        page.on("console", on_c)
        page.on("response", on_r)
        st = "PASS"
        notes = []
        page.goto("http://127.0.0.1:5173" + path, wait_until="networkidle", timeout=60000)
        page.wait_for_timeout(2000)
        body = page.locator("body").inner_text(timeout=15000)
        if "ACCOUNT LOGIN" in body or ("账号登录" in body and "密码" in body and "欢迎回来" in body):
            st = "FAIL"
            notes.append("login_wall")
        for b in ["undefined", "NaN", "[object Object]", "系统异常"]:
            if b in body:
                st = "FAIL"
                notes.append(b)
        if any("Uncaught" in x for x in cons):
            st = "FAIL"
            notes.append("uncaught")
        if nets:
            st = "FAIL"
            notes.append("api500")
        acc.append({"route": path, "status": st, "notes": notes, "snippet": body[:220].replace("\n", " "), "url": page.url})
        print(st, path, notes)
        try:
            page.remove_listener("console", on_c)
            page.remove_listener("response", on_r)
        except Exception:
            pass

    # Admin with precise menus
    adm = []
    page.goto("http://127.0.0.1:5174/login", wait_until="networkidle", timeout=60000)
    page.wait_for_timeout(800)
    page.locator("input[type='text'], input[placeholder*='用户']").first.fill("admin")
    page.locator("input[type='password']").first.fill("admin123")
    if page.locator("input[placeholder*='验证码']").count():
        page.locator("input[placeholder*='验证码']").first.fill("1")
    page.locator("button[type='submit'], button:has-text('登录'), button:has-text('登 录')").first.click()
    page.wait_for_timeout(3000)
    # expand menus and click exact items
    targets = [
        "赛事发布", "队长授权", "报名审核", "参赛队伍", "赛程管理", "比赛管理", "比赛数据审核",
        "球员认证审核", "平台游戏ID变更审核", "球队资料变更审核"
    ]
    for label in targets:
        try:
            # ensure parent expanded
            for parent in ["赛事管理", "比赛执行", "账号与认证"]:
                pl = page.get_by_text(parent, exact=True)
                if pl.count():
                    try:
                        pl.first.click(timeout=2000)
                    except Exception:
                        pass
            loc = page.get_by_role("menuitem", name=label)
            if loc.count() == 0:
                loc = page.get_by_text(label, exact=True)
            if loc.count() == 0:
                adm.append({"label": label, "status": "FAIL", "notes": ["menu_not_found"]})
                print("FAIL", label, "not found")
                continue
            loc.first.click(timeout=5000)
            page.wait_for_timeout(1500)
            body = page.locator("body").inner_text(timeout=10000)
            st = "FAIL" if ("系统异常" in body or "Cannot read" in body or "404" == body.strip()) else "PASS"
            adm.append({"label": label, "status": st, "route": page.url, "snippet": body[:160].replace("\n", " ")})
            print(st, label, page.url)
        except Exception as e:
            adm.append({"label": label, "status": "FAIL", "notes": [repr(e)[:160]]})
            print("FAIL", label, e)

    browser.close()

gap = json.loads(Path("full_smoke_gap_closure_20260811.json").read_text(encoding="utf-8"))
gap["browser_account"] = {"results": acc, "pass": sum(1 for x in acc if x["status"] == "PASS"), "fail": sum(1 for x in acc if x["status"] == "FAIL")}
gap["browser_admin"] = {"results": adm, "pass": sum(1 for x in adm if x["status"] == "PASS"), "fail": sum(1 for x in adm if x["status"] == "FAIL")}
# update cases
gap["cases"] = [c for c in gap.get("cases", []) if c.get("name") not in ("account_browser", "admin_ui_browser")]
gap["cases"].append({"name": "account_browser", "expected": "logged-in", "actual": f"pass={gap['browser_account']['pass']} fail={gap['browser_account']['fail']}", "evidence": "cnpa_web_token", "result": "PASS" if gap["browser_account"]["fail"] == 0 else "FAIL"})
gap["cases"].append({"name": "admin_ui_browser", "expected": "core menus", "actual": f"pass={gap['browser_admin']['pass']} fail={gap['browser_admin']['fail']}", "evidence": "5174", "result": "PASS" if gap["browser_admin"]["fail"] == 0 else "FAIL"})
Path("full_smoke_gap_closure_20260811.json").write_text(json.dumps(gap, ensure_ascii=False, indent=2), encoding="utf-8")
print("ACC", gap["browser_account"]["pass"], gap["browser_account"]["fail"])
print("ADM", gap["browser_admin"]["pass"], gap["browser_admin"]["fail"])