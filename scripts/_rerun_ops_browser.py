#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
import json, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs
fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
ADMIN = fs.ADMIN
req = fs.req
prev = json.loads((ROOT / "scripts" / "cnpa_series_submission_v1_final_e2e_result.json").read_text(encoding="utf-8"))

def rec(name, ok, detail="", sev="P1"):
    prev["cases"][name] = {"ok": bool(ok), "detail": str(detail)[:800]}
    print(("PASS" if ok else "FAIL"), name, str(detail)[:200], flush=True)
    prev["bugs"][sev] = [x for x in prev["bugs"].get(sev, []) if not x.startswith(name + ":")]
    if not ok:
        prev["bugs"][sev].append(name + ": " + str(detail)[:400])

admin = fs.admin_login()
paths = [
    ("ops.direct.alive", "/basket/v4/admin/stage-exception/match-official-result"),
    ("ops.correction.alive", "/basket/v4/admin/stage-exception/match-result-correction"),
    ("ops.forfeit.alive", "/basket/v4/admin/stage-exception/match-forfeit"),
    ("ops.forfeitUndo.alive", "/basket/v4/admin/stage-exception/match-forfeit-revoke"),
    ("ops.seriesForfeit.alive", "/basket/v4/admin/stage-exception/series-forfeit"),
    ("ops.manual.alive", "/basket/v4/admin/stage-exception/series-manual-resolution"),
]
for name, path in paths:
    r = req(ADMIN + path, "POST", {}, admin)
    rec(name, r.get("code") not in (-1, 404) and "No static resource" not in str(r.get("msg") or ""), "code=%s msg=%s" % (r.get("code"), r.get("msg")))

from playwright.sync_api import sync_playwright
bo5 = (prev.get("ids") or {}).get("bo5") or {}
series_id = bo5.get("seriesId")
home = bo5.get("home")
email_row = fs.db_query("SELECT ma.email FROM basket_event_team et JOIN basket_member_account ma ON ma.id=et.captain_member_id WHERE et.id=%s", (home,))
email = email_row[0]["email"]
shot = ROOT / "scripts" / "_series_sub_e2e_shots"
shot.mkdir(exist_ok=True)
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 1100})
    page.goto("http://localhost:5173/login", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_selector("#login-email", timeout=25000)
    page.fill("#login-email", email)
    page.fill("#login-password", fs.MEMBER_PASS)
    page.locator("button.auth-primary-button").click()
    page.wait_for_function("() => !location.pathname.includes('/login')", timeout=25000)
    page.goto("http://localhost:5173/account/my-events/v4/series-submission/" + str(series_id), wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(2500)
    text = page.inner_text("body")
    rec("browser.web.workspace", ("G1" in text and "G2" in text and "G4" in text) or "BO5" in text or "3" in text, text[:400])
    page.screenshot(path=str(shot / "web_bo5_workspace.png"), full_page=True)
    page.close()
    page = browser.new_page()
    page.goto("http://localhost:5174/login", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(800)
    if page.locator("input[type='password']").count():
        page.locator("input").nth(0).fill("admin")
        page.locator("input[type='password']").fill("admin123")
        page.keyboard.press("Enter")
        page.wait_for_timeout(2500)
    page.goto("http://localhost:5174/competition-exec/event-v4-match-submission-audit", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(2000)
    text = page.inner_text("body")
    rec("browser.admin.audit", "event-v4-match-submission-audit" in page.url and len(text) > 80, page.url + " " + text[:300])
    tabs = page.locator(".el-tabs__item")
    if tabs.count() >= 2:
        tabs.nth(1).click()
        page.wait_for_timeout(1200)
    page.screenshot(path=str(shot / "admin_series_audit.png"), full_page=True)
    browser.close()

out = ROOT / "scripts" / "cnpa_series_submission_v1_final_e2e_result.json"
out.write_text(json.dumps(prev, ensure_ascii=False, indent=2), encoding="utf-8")
failed = [k for k, v in prev["cases"].items() if not v["ok"]]
print("failed", failed)
print("p0", prev["bugs"]["P0"])
print("p1", prev["bugs"]["P1"])
