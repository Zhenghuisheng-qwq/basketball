#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

UI = "http://127.0.0.1"
SHOT = Path(__file__).resolve().parent / "_p1_shots"
SHOT.mkdir(exist_ok=True)


def run_browser(token: str, seasons: dict, publish_path: str) -> dict:
    from playwright.sync_api import sync_playwright

    out = {"cases": {}, "url": ""}
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1680, "height": 1100})
        page = context.new_page()

        def dismiss_overlays():
            for _ in range(6):
                box = page.locator(".el-message-box__btns .el-button, .el-overlay-message-box .el-button")
                if box.count() == 0:
                    break
                try:
                    page.locator(".el-message-box__btns .el-button--primary").first.click(timeout=600)
                except Exception:
                    try:
                        box.first.click(timeout=600)
                    except Exception:
                        break
                page.wait_for_timeout(300)

        page.goto(UI + "/login", wait_until="domcontentloaded", timeout=60000)
        page.evaluate("token => { document.cookie = 'Admin-Token=' + token + '; path=/'; }", token)
        dismiss_overlays()
        page.goto(UI + publish_path, wait_until="domcontentloaded", timeout=60000)
        page.wait_for_timeout(2000)
        dismiss_overlays()
        if page.get_by_placeholder("账号").count() and page.get_by_placeholder("账号").first.is_visible():
            page.get_by_placeholder("账号").fill("admin")
            page.get_by_placeholder("密码").fill("admin123")
            if page.get_by_placeholder("验证码").count() and page.get_by_placeholder("验证码").first.is_visible():
                raise RuntimeError("captcha enabled, cannot UI login")
            page.get_by_role("button", name="登 录").click()
            page.wait_for_timeout(2500)
            page.goto(UI + publish_path, wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(2000)
        dismiss_overlays()
        out["url"] = page.url
        page.screenshot(path=str(SHOT / "home.png"), full_page=True)
        if "login" in page.url.lower():
            raise RuntimeError("still on login: " + page.url)

        def grab_toasts():
            loc = page.locator(".el-message__content, .el-notification__content")
            texts = []
            for i in range(loc.count()):
                try:
                    texts.append(loc.nth(i).inner_text(timeout=300)[:240])
                except Exception:
                    pass
            return texts

        def close_dialogs():
            dismiss_overlays()
            for _ in range(4):
                btns = page.locator(".el-dialog__headerbtn")
                if btns.count() == 0:
                    break
                try:
                    btns.last.click(timeout=800)
                    page.wait_for_timeout(300)
                except Exception:
                    break

        def open_rules(season_name: str):
            close_dialogs()
            page.wait_for_timeout(400)
            page.get_by_placeholder("赛季名称").fill(season_name)
            dismiss_overlays()
            page.get_by_role("button", name="搜索").click(force=True)
            page.wait_for_timeout(1400)
            page.get_by_role("button", name="继续发布").first.click(timeout=20000)
            page.wait_for_timeout(2000)
            page.locator(".el-step__title").filter(has_text="赛制规则").first.click()
            page.wait_for_timeout(1800)
            tab = page.locator(".el-tabs__item").filter(has_text="自定义赛制")
            if tab.count():
                tab.first.click()
            else:
                page.get_by_text("自定义赛制").first.click()
            page.wait_for_timeout(900)

        def click_playoff_on_stage(nth: int = 0):
            card = page.locator(".stage-config-card").nth(nth)
            card.get_by_text("季后赛", exact=True).first.click()
            page.wait_for_timeout(5000)

        def panel_text():
            panel = page.locator(".format-rules-panel")
            return panel.inner_text(timeout=12000) if panel.count() else page.locator("body").inner_text()

        open_rules(seasons["case1"]["name"])
        click_playoff_on_stage(0)
        body = panel_text()
        toasts = grab_toasts()
        page.screenshot(path=str(SHOT / "case1.png"), full_page=True)
        case1 = {
            "toasts": toasts,
            "has_16": "16强赛" in body,
            "has_8": "8强赛" in body,
            "has_4": "4强赛" in body,
            "has_final": "总决赛" in body,
            "has_bracket": ("1 / 16" in body) or ("1号种子" in body),
            "playoff_8_toast": any("PLAYOFF_8" in t for t in toasts),
            "toast_count_playoff8": sum(1 for t in toasts if "PLAYOFF_8" in t),
            "snippet": body[:1800],
        }
        try:
            page.get_by_role("button", name="保存草稿").click()
            page.wait_for_timeout(2000)
            case1["save_toasts"] = grab_toasts()
            case1["save_ok"] = not any("PLAYOFF_8" in t or "编码不存在" in t for t in case1["save_toasts"])
        except Exception as e:
            case1["save_ok"] = False
            case1["save_err"] = str(e)[:180]
        try:
            page.get_by_role("button", name="下一步").click()
            page.wait_for_timeout(2000)
            case1["next_toasts"] = grab_toasts()
            case1["next_ok"] = not any(
                ("PLAYOFF_8" in t) or ("编码不存在" in t) or ("赛制配置不完整" in t) for t in case1["next_toasts"]
            )
        except Exception as e:
            case1["next_ok"] = False
            case1["next_err"] = str(e)[:180]
        case1["pass"] = (
            case1["has_16"]
            and case1["has_8"]
            and case1["has_4"]
            and case1["has_final"]
            and not case1["playoff_8_toast"]
            and case1.get("save_ok")
            and case1.get("next_ok")
        )
        out["cases"]["case1"] = case1

        open_rules(seasons["case2"]["name"])
        n_cards = page.locator(".stage-config-card").count()
        click_playoff_on_stage(max(0, n_cards - 1))
        body = panel_text()
        toasts = grab_toasts()
        page.screenshot(path=str(SHOT / "case2.png"), full_page=True)
        case2 = {
            "toasts": toasts,
            "has_8": "8强赛" in body,
            "has_4": "4强赛" in body,
            "has_final": "总决赛" in body,
            "no_16": "16强赛" not in body,
            "standing_ui": "种子来源" in body,
            "playoff_8_toast": any("PLAYOFF_8" in t for t in toasts),
            "snippet": body[:1600],
        }
        case2["pass"] = (
            case2["has_8"] and case2["has_4"] and case2["has_final"] and case2["no_16"] and not case2["playoff_8_toast"]
        )
        out["cases"]["case2"] = case2

        specs = [
            ("case3", ["4强赛", "总决赛"], ["16强赛", "8强赛"], "case3.png"),
            ("case4", ["8强赛", "4强赛", "总决赛"], ["16强赛"], "case4.png"),
            ("case5", ["16强赛", "8强赛", "4强赛", "总决赛"], [], "case5.png"),
        ]
        for key, need, forbid, shot in specs:
            open_rules(seasons[key]["name"])
            click_playoff_on_stage(0)
            body = panel_text()
            toasts = grab_toasts()
            rec = {
                "toasts": toasts,
                "names_ok": all(n in body for n in need),
                "forbid_ok": all(n not in body for n in forbid),
                "playoff_8_toast": any("PLAYOFF_8" in t for t in toasts),
                "snippet": body[:1200],
            }
            rec["pass"] = rec["names_ok"] and rec["forbid_ok"] and not rec["playoff_8_toast"]
            page.screenshot(path=str(SHOT / shot), full_page=True)
            out["cases"][key] = rec

        browser.close()
    return out
