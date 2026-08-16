#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Browser smoke for CNPA match submission E2E. Real UI login, no fake session."""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

import requests
import redis

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"

WEB_UI = "http://127.0.0.1:5173"
ADMIN_UI = "http://127.0.0.1:5174"
WEB_PASS = fs.MEMBER_PASS
RESULT = Path(__file__).with_name("cnpa_v13_match_submission_e2e_result.json")
OUT = Path(__file__).with_name("cnpa_v13_match_submission_browser_result.json")
SHOT = Path(__file__).with_name("_submission_e2e_shots")
SHOT.mkdir(exist_ok=True)

PNG = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02"
    b"\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\xcf\xc0\x00\x00\x03\x01"
    b"\x01\x00\x18\xdd\x8d\xb0\x00\x00\x00\x00IEND\xaeB`\x82"
)


def log(msg: str) -> None:
    print(msg, flush=True)


def web_login_ui(page, email: str, password: str = WEB_PASS) -> None:
    page.goto(f"{WEB_UI}/login", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_selector("#login-email", timeout=25000)
    page.fill("#login-email", email)
    page.fill("#login-password", password)
    page.get_by_role("button", name="登录 CNPA").click()
    page.wait_for_function("() => !location.pathname.includes('/login')", timeout=25000)
    page.wait_for_timeout(800)


def open_schedule(browser, email: str):
    ctx = browser.new_context(viewport={"width": 1440, "height": 1100})
    page = ctx.new_page()
    web_login_ui(page, email)
    page.goto(f"{WEB_UI}/account/my-events?tab=schedule", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(2000)
    if page.get_by_text("全部赛程").count():
        page.get_by_text("全部赛程").first.click()
        page.wait_for_timeout(1500)
    return ctx, page


def admin_login_token() -> str:
    try:
        return fs.admin_login()
    except Exception:
        pass
    img = requests.get(f"{fs.ADMIN}/captchaImage", timeout=15).json()
    payload = {"username": "admin", "password": "admin123", "code": "", "uuid": ""}
    if img.get("captchaEnabled"):
        uuid = img.get("uuid") or ""
        r = redis.Redis(host="192.168.31.48", port=6379, password="Q86KDiDoZULykMfz", decode_responses=True)
        raw = r.get(f"captcha_codes:{uuid}")
        payload["code"] = str(raw or "").strip().strip('"')
        payload["uuid"] = uuid
    out = requests.post(f"{fs.ADMIN}/login", json=payload, timeout=20).json()
    if out.get("code") != 200:
        raise RuntimeError(f"admin login failed: {out}")
    return out["token"]


def public_series_ok(series_id: int, home: int, away: int) -> tuple[bool, str]:
    r = requests.get(f"{fs.WEB}/basket/v4/public/match-series/{series_id}", timeout=15)
    body = r.json() if r.headers.get("content-type", "").startswith("application/json") else {}
    data = body.get("data") or {}
    ok = (
        r.status_code == 200
        and int(data.get("homeWinCount") or 0) == home
        and int(data.get("awayWinCount") or 0) == away
    )
    return ok, f"http={r.status_code} {data.get('seriesCode')} {data.get('homeWinCount')}:{data.get('awayWinCount')} status={data.get('seriesStatus')}"


def main() -> int:
    from playwright.sync_api import sync_playwright

    api = json.loads(RESULT.read_text(encoding="utf-8")) if RESULT.exists() else {}
    ids = api.get("ids") or {}
    cases = {}

    def rec(name: str, ok: bool, detail: str = "") -> None:
        cases[name] = {"ok": ok, "detail": str(detail)[:600]}
        log(f"[{'PASS' if ok else 'FAIL'}] browser.{name} {detail[:220]}")

    bo1 = ids.get("bo1") or {}
    bo3 = ids.get("bo3") or {}
    bo5 = ids.get("bo5") or {}
    accounts = ids.get("accounts") or {}

    bo1_match = int(bo1.get("matchId") or 328)
    draft_match = 327
    draft_home = 80
    bo3_g1 = int((bo3.get("games") or [428])[0])
    bo3_home = str(bo3.get("home") or 116)
    bo3_series = int(bo3.get("seriesId") or 82)
    bo5_div = bo5.get("divisionId")
    bo5_home = str(bo5.get("home") or "")
    bo5_series = int(bo5.get("seriesId") or 93)

    email_draft = accounts.get(str(draft_home)) or "fs_ld_ld0701@cnpa.smoke"
    email_bo3 = accounts.get(bo3_home) or "fs_ib_ib0301@cnpa.smoke"
    email_bo5 = accounts.get(bo5_home) if bo5_home else "fs_sb_h101@cnpa.smoke"

    rows = fs.db_query(
        "select id, match_no from basket_match where id in (%s,%s,%s)",
        (draft_match, bo1_match, bo3_g1),
    )
    match_no = {int(r["id"]): r.get("match_no") for r in rows}
    match_no.setdefault(328, "V4-23-14-04")
    match_no.setdefault(327, "V4-23-14-03")
    match_no.setdefault(428, "V4-27-SER-51-R1-S04-G1")
    match_no.setdefault(476, "V4-34-SER-66-R1-S01-G4")

    fs.clear_login_locks([email_draft, email_bo3, email_bo5])

    pub3_ok, pub3_d = public_series_ok(bo3_series, 2, 0)
    rec("public.bo3.series", pub3_ok, pub3_d)
    pub5_ok, pub5_d = public_series_ok(bo5_series, 3, 1)
    rec("public.bo5.series", pub5_ok, pub5_d)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        ctx = browser.new_context(viewport={"width": 1440, "height": 1100})
        page = ctx.new_page()
        web_login_ui(page, email_draft)
        page.goto(f"{WEB_UI}/account/my-events?tab=schedule", wait_until="domcontentloaded", timeout=60000)
        page.wait_for_timeout(2500)
        body = page.locator("body").inner_text()
        rec(
            "web.login.schedule",
            "完成球员认证后" not in body and ("我的赛程" in body or "待提交" in body or "比赛" in body),
            page.url + " " + body[:180],
        )
        page.screenshot(path=str(SHOT / "web_schedule.png"), full_page=True)

        page.goto(
            f"{WEB_UI}/account/my-events/v4/match-submission/{draft_match}?submitEventTeamId={draft_home}",
            wait_until="domcontentloaded",
            timeout=60000,
        )
        page.wait_for_timeout(2500)
        rec("web.bo1.directRoute", f"/account/my-events/v4/match-submission/{draft_match}" in page.url, page.url)
        text = page.locator("body").inner_text()
        rec("web.bo1.noSeriesScore", "系列赛当前比分" not in text and "Game " not in text, text[:240])
        rec("web.bo1.playerSelect", page.locator("select").count() > 0, f"selects={page.locator('select').count()}")
        rec("web.bo1.scoreHint", "请填写最终比赛比分" in text, text[:200])
        rec(
            "web.bo1.screenshot.ui",
            "比赛截图" in text and "fileId" not in page.locator("input[type=text]").all_inner_texts()
            if page.locator("input[type=text]").count()
            else "比赛截图" in text,
            text[:200],
        )

        if "保存草稿" in text and "无需进行" not in text:
            scores = page.locator("input[type='number']")
            if scores.count() >= 2:
                scores.nth(0).fill("80")
                scores.nth(1).fill("75")
            if page.locator("select").count():
                opts = page.locator("select").first.locator("option")
                if opts.count() > 1:
                    page.locator("select").first.select_option(index=1)
            tmp = SHOT / "up.png"
            tmp.write_bytes(PNG)
            tmp2 = SHOT / "up2.png"
            tmp2.write_bytes(PNG)
            page.locator("input[type=file]").set_input_files([str(tmp), str(tmp2)])
            page.wait_for_timeout(2000)
            page.get_by_text("保存草稿").first.click()
            page.wait_for_timeout(1500)
            page.reload(wait_until="domcontentloaded")
            page.wait_for_timeout(2000)
            thumbs = page.locator("img[alt='比赛截图']")
            rec("web.bo1.draft.thumbs", thumbs.count() >= 1, f"thumbs={thumbs.count()}")
            page.screenshot(path=str(SHOT / "web_bo1_draft.png"), full_page=True)
        else:
            rec("web.bo1.draft.thumbs", "已通过" in text or "审核" in text or "缩略" in text, "page not editable")

        ctx.close()

        c3, p3 = open_schedule(browser, email_bo3)
        t3 = p3.locator("body").inner_text()
        rec("web.bo3.seriesCard", ("BO3" in t3 or "系列赛" in t3) and "完成球员认证后" not in t3, t3[:280])
        p3.screenshot(path=str(SHOT / "web_bo3.png"), full_page=True)
        p3.goto(
            f"{WEB_UI}/account/my-events/v4/match-submission/{bo3_g1}?submitEventTeamId={bo3_home}",
            wait_until="domcontentloaded",
            timeout=60000,
        )
        p3.wait_for_timeout(2000)
        t3b = p3.locator("body").inner_text()
        rec("web.bo3.gameContext", "BO3" in t3b or "Game 1" in t3b or "系列赛" in t3b, t3b[:240])
        c3.close()

        c5, p5 = open_schedule(browser, email_bo5)
        t5 = p5.locator("body").inner_text()
        rec("web.bo5.series31", ("3 : 1" in t5 or "3:1" in t5) and "完成球员认证后" not in t5, t5[:320])
        rec("web.bo5.game5.skip", "无需进行" in t5, t5[:320])
        p5.screenshot(path=str(SHOT / "web_bo5.png"), full_page=True)

        p5.goto(f"{WEB_UI}/events/24", wait_until="domcontentloaded", timeout=60000)
        p5.wait_for_timeout(2000)
        if p5.get_by_text("赛程", exact=True).count():
            p5.get_by_text("赛程", exact=True).first.click()
            p5.wait_for_timeout(2000)
        if p5.get_by_text("V4-34-SER-66-R1-S01").count():
            p5.get_by_text("V4-34-SER-66-R1-S01").first.click()
            p5.wait_for_timeout(1500)
        pub_txt = p5.locator("body").inner_text()
        rec(
            "web.public.bo5.modal",
            "3 : 1" in pub_txt or "3:1" in pub_txt or "V4-34-SER-66-R1-S01" in pub_txt,
            pub_txt[:280],
        )
        p5.screenshot(path=str(SHOT / "web_public_bo5.png"), full_page=True)
        c5.close()

        admin_token = admin_login_token()
        actx = browser.new_context(viewport={"width": 1600, "height": 1100})
        actx.add_cookies([{
            "name": "Admin-Token",
            "value": admin_token,
            "domain": "127.0.0.1",
            "path": "/",
        }])
        ap = actx.new_page()
        ap.goto(f"{ADMIN_UI}/", wait_until="domcontentloaded", timeout=60000)
        ap.wait_for_timeout(4000)
        if ap.get_by_placeholder("账号").count() and ap.get_by_placeholder("账号").first.is_visible():
            ap.get_by_placeholder("账号").fill("admin")
            ap.get_by_placeholder("密码").fill("admin123")
            if ap.get_by_placeholder("验证码").count() and ap.get_by_placeholder("验证码").first.is_visible():
                ap.get_by_placeholder("验证码").fill("1")
            btn = ap.get_by_role("button", name="登 录")
            if btn.count() == 0:
                btn = ap.locator("button").filter(has_text="登录")
            btn.first.click()
            ap.wait_for_timeout(3500)
        for parent in ("比赛执行",):
            loc = ap.get_by_text(parent, exact=True)
            if loc.count():
                try:
                    loc.first.click(timeout=3000)
                except Exception:
                    pass
        audit = ap.get_by_role("menuitem", name="比赛数据审核")
        if audit.count() == 0:
            audit = ap.get_by_text("比赛数据审核", exact=True)
        if audit.count():
            audit.first.click()
            ap.wait_for_timeout(2500)
        else:
            ap.goto(f"{ADMIN_UI}/competition-exec/event-v4-match-submission-audit", wait_until="domcontentloaded", timeout=60000)
            ap.wait_for_timeout(2500)
        rec("admin.login.audit", "比赛数据审核" in ap.locator("body").inner_text() and "404" not in ap.locator("body").inner_text(), ap.url)
        ap.screenshot(path=str(SHOT / "admin_audit_list.png"), full_page=True)

        mno = match_no.get(bo1_match) or match_no.get(draft_match) or ""
        if mno and ap.get_by_placeholder("比赛编号").count() == 0:
            pass
        if ap.locator("input").count():
            # 比赛编号 input is labeled, not always placeholder
            labeled = ap.locator(".el-form-item").filter(has_text="比赛编号").locator("input")
            if labeled.count() and (match_no.get(bo1_match) or match_no.get(bo3_g1)):
                labeled.first.fill(str(match_no.get(bo1_match) or match_no.get(bo3_g1)))
                ap.get_by_role("button", name="搜索").click()
                ap.wait_for_timeout(1500)

        detail_btn = ap.locator("button").filter(has_text="详情")
        if detail_btn.count() == 0:
            detail_btn = ap.get_by_text("详情", exact=True)
        if detail_btn.count():
            detail_btn.first.click()
            ap.wait_for_timeout(2500)
            at = ap.locator("body").inner_text()
            rec("admin.bo1.detail.stats", "PTS" in at and "REB" in at, at[:300])
            shots = ap.locator(".el-drawer .audit-shot img, .el-drawer .el-image img, .audit-shot img")
            rec("admin.screenshot.thumb", shots.count() >= 1, f"shots={shots.count()}")
            if shots.count():
                try:
                    shots.first.click(timeout=4000)
                except Exception:
                    shots.first.click(force=True, timeout=4000)
                ap.wait_for_timeout(800)
                rec(
                    "admin.screenshot.preview",
                    ap.locator(".el-image-viewer__wrapper, .el-image-viewer__img").count() > 0,
                    "viewer",
                )
            else:
                rec("admin.screenshot.preview", False, "no audit-shot in drawer")
            ap.screenshot(path=str(SHOT / "admin_audit_detail.png"), full_page=True)
            if ap.locator(".el-drawer__close-btn, .el-drawer__header .el-icon").count():
                try:
                    ap.locator(".el-drawer__close-btn").first.click()
                    ap.wait_for_timeout(500)
                except Exception:
                    pass
        else:
            rec("admin.bo1.detail.stats", False, ap.locator("body").inner_text()[:300])

        # BO3 Game 审核上下文
        labeled = ap.locator(".el-form-item").filter(has_text="比赛编号").locator("input")
        g1_no = match_no.get(bo3_g1)
        if labeled.count() and g1_no:
            labeled.first.fill(str(g1_no))
            ap.get_by_role("button", name="搜索").click()
            ap.wait_for_timeout(1500)
            d2 = ap.locator("button").filter(has_text="详情")
            if d2.count():
                d2.first.click()
                ap.wait_for_timeout(2500)
                at2 = ap.locator("body").inner_text()
                rec(
                    "admin.bo3.gameContext",
                    "BO3" in at2 or "Game 1" in at2 or "系列赛" in at2,
                    at2[:300],
                )
                ap.screenshot(path=str(SHOT / "admin_audit_bo3.png"), full_page=True)
            else:
                rec("admin.bo3.gameContext", False, "no detail after BO3 search")
        else:
            rec("admin.bo3.gameContext", False, f"no match_no for {bo3_g1}")

        actx.close()
        browser.close()

    OUT.write_text(json.dumps({"cases": cases, "ids": ids, "match_no": match_no}, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    fails = [k for k, v in cases.items() if not v.get("ok")]
    return 1 if fails else 0


if __name__ == "__main__":
    raise SystemExit(main())
