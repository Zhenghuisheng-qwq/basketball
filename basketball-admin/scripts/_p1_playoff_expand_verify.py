#!/usr/bin/env python3
"""P1: prove sequential next-link write fails; two-pass bulk insert succeeds; drive Admin UI."""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from full_smoke_common import (  # noqa: E402
    ADMIN,
    TEMPLATE_RR,
    admin_login,
    as_id,
    create_division,
    extract_id,
    req,
    sorted_stages,
    stage_payload,
    wipe_stages,
)

OUT = Path(__file__).resolve().parent / "_p1_playoff_expand_verify.json"
UI = "http://127.0.0.1"
SHOT = Path(__file__).resolve().parent / "_p1_shots"
SHOT.mkdir(exist_ok=True)


def snapshot(token: str, division_id: str) -> dict:
    r = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    return r.get("data") or {}


def playoff_payloads(team_count: int, start_order: int = 1, standing: bool = False) -> list[dict]:
    sizes = []
    n = team_count
    while n >= 2:
        sizes.append(n)
        n //= 2
    names = {16: "16强赛", 8: "8强赛", 4: "4强赛", 2: "总决赛"}
    codes = []
    for i, size in enumerate(sizes):
        is_final = i == len(sizes) - 1
        code = "PLAYOFF_FINAL" if is_final else f"PLAYOFF_{size}"
        if i == 0 and standing:
            code = f"{code}_STD"
        codes.append(code)
    out = []
    for i, size in enumerate(sizes):
        is_final = i == len(sizes) - 1
        out.append(
            stage_payload(
                codes[i],
                names.get(size, f"{size}强赛"),
                start_order + i,
                "SINGLE_ELIMINATION",
                1,
                "NONE" if is_final else "WINNER_ADVANCE",
                None if is_final else codes[i + 1],
                pairing="PLAYOFF_BRACKET",
            )
        )
    return out


def put_stage(token: str, division_id: str, stage_id, payload: dict) -> dict:
    return req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{stage_id}", "PUT", payload, token)


def add_stage(token: str, division_id: str, payload: dict) -> dict:
    return req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", payload, token)


def unlink(s: dict) -> dict:
    p = stage_payload(
        s["stageCode"],
        s.get("stageName") or s["stageCode"],
        s.get("stageOrder") or 1,
        s.get("competitionFormat") or "ROUND_ROBIN",
        s.get("bestOf") or 1,
        "NONE",
        pairing=s.get("pairingRule") or "NONE",
        org=s.get("organizationMode") or "UNGROUPED",
        grouping=s.get("groupingMode") or "NONE",
        group_count=s.get("groupCount"),
    )
    p["nextStageCode"] = ""
    if (s.get("competitionFormat") or "").upper() == "ROUND_ROBIN":
        p["roundRobinCycles"] = s.get("roundRobinCycles") or 1
        p["winPoints"] = s.get("winPoints") if s.get("winPoints") is not None else 2
        p["lossPoints"] = s.get("lossPoints") if s.get("lossPoints") is not None else 1
    return p


def two_pass_replace(token: str, division_id: str, payloads: list[dict]) -> list[dict]:
    errors = []
    stages = sorted_stages(snapshot(token, division_id))
    for s in stages:
        if s.get("nextStageCode"):
            r = put_stage(token, division_id, s["id"], unlink(s))
            if r.get("code") != 200:
                errors.append(("unlink", r.get("msg")))
    stages = sorted_stages(snapshot(token, division_id))
    while len(stages) > len(payloads):
        last = stages[-1]
        r = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{last['id']}", "DELETE", token=token)
        if r.get("code") != 200:
            errors.append(("delete", r.get("msg")))
        stages = sorted_stages(snapshot(token, division_id))
    for i, payload in enumerate(payloads):
        stub = dict(payload)
        stub["nextStageCode"] = ""
        stub["advancementType"] = "NONE"
        stub.pop("advancementCount", None)
        stages = sorted_stages(snapshot(token, division_id))
        if i < len(stages):
            r = put_stage(token, division_id, stages[i]["id"], stub)
        else:
            r = add_stage(token, division_id, stub)
        if r.get("code") != 200:
            errors.append((f"stub[{i}]", r.get("msg")))
    stages = sorted_stages(snapshot(token, division_id))
    for i, payload in enumerate(payloads):
        if i >= len(stages):
            errors.append((f"link[{i}]", "missing stage"))
            continue
        r = put_stage(token, division_id, stages[i]["id"], payload)
        if r.get("code") != 200:
            errors.append((f"link[{i}]", r.get("msg")))
    return errors


def one_pass_replace(token: str, division_id: str, payloads: list[dict]) -> dict:
    stages = sorted_stages(snapshot(token, division_id))
    first = payloads[0]
    if not stages:
        return {"code": -1, "msg": "no existing stage"}
    return put_stage(token, division_id, stages[0]["id"], first)


def summarize(token: str, division_id: str) -> list[dict]:
    rows = []
    for s in sorted_stages(snapshot(token, division_id)):
        rows.append(
            {
                "code": s.get("stageCode"),
                "name": s.get("stageName"),
                "format": s.get("competitionFormat"),
                "pairing": s.get("pairingRule"),
                "adv": s.get("advancementType"),
                "count": s.get("advancementCount"),
                "next": s.get("nextStageCode"),
                "order": s.get("stageOrder"),
            }
        )
    return rows


def create_draft_season(token: str, name: str, max_team: int) -> tuple[str, str]:
    body = {
        "seriesId": 1,
        "seasonYear": 2026,
        "seasonName": name,
        "displayTitle": name,
        "description": "P1 playoff expand verify",
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-12-31 23:59:59",
    }
    r = req(f"{ADMIN}/basket/v4/season", "POST", body, token)
    if r.get("code") != 200:
        raise RuntimeError(f"create season failed: {r}")
    season_id = extract_id(r)
    div_id = create_division(token, season_id, f"{name}-DIV", max_team=max_team, template_id=TEMPLATE_RR)
    return season_id, div_id


def find_publish_path(token: str) -> str:
    r = req(f"{ADMIN}/getRouters", token=token)
    found = []

    def walk(nodes, prefix=""):
        for n in nodes or []:
            path = n.get("path") or ""
            full = path if path.startswith("/") else (f"{prefix}/{path}".replace("//", "/"))
            if "event-v4-publish" in (path or "") or "event-v4-publish" in (n.get("component") or ""):
                found.append(full if full.startswith("/") else "/" + full.lstrip("/"))
            walk(n.get("children") or [], full)

    walk(r.get("data") or [])
    return found[0] if found else "/event-v4-publish"


def run_api_cases(token: str) -> dict:
    results = {}
    season_id, div16 = create_draft_season(token, f"P1PO16-{int(time.time())}", 16)
    wipe_stages(token, div16)
    add_stage(
        token,
        div16,
        stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "NONE"),
    )
    old = one_pass_replace(token, div16, playoff_payloads(16))
    results["one_pass_16"] = {
        "code": old.get("code"),
        "msg": old.get("msg"),
        "expect_fail": True,
        "pass": old.get("code") != 200 and "PLAYOFF_8" in str(old.get("msg") or ""),
        "stages": summarize(token, div16),
    }

    wipe_stages(token, div16)
    add_stage(token, div16, stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "NONE"))
    errs = two_pass_replace(token, div16, playoff_payloads(16))
    stages = summarize(token, div16)
    codes = [s["code"] for s in stages]
    names = [s["name"] for s in stages]
    results["two_pass_16"] = {
        "errors": errs,
        "stages": stages,
        "pass": not errs
        and codes == ["PLAYOFF_16", "PLAYOFF_8", "PLAYOFF_4", "PLAYOFF_FINAL"]
        and names == ["16强赛", "8强赛", "4强赛", "总决赛"]
        and stages[0]["next"] == "PLAYOFF_8"
        and stages[-1]["adv"] == "NONE"
        and all(s["adv"] == "WINNER_ADVANCE" for s in stages[:-1]),
    }

    _, div8 = create_draft_season(token, f"P1PO8-{int(time.time())}", 16)
    wipe_stages(token, div8)
    add_stage(
        token,
        div8,
        stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", None, 8),
    )
    add_stage(token, div8, stage_payload("STAGE_2", "阶段 2", 2, "ROUND_ROBIN", 1, "NONE"))
    rr = stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "PLAYOFF_8_STD", 8)
    payloads = [rr] + playoff_payloads(8, start_order=2, standing=True)
    errs = two_pass_replace(token, div8, payloads)
    stages = summarize(token, div8)
    results["two_pass_rr_top8"] = {
        "errors": errs,
        "stages": stages,
        "pass": not errs
        and stages[0]["code"] == "REGULAR"
        and stages[0]["count"] == 8
        and stages[0]["adv"] == "OVERALL_TOP_N"
        and stages[0]["next"] == stages[1]["code"]
        and len(stages) == 4
        and stages[-1]["name"] == "总决赛"
        and stages[1]["name"] == "8强赛",
    }

    for n, expect_len, expect_names in (
        (4, 2, ["4强赛", "总决赛"]),
        (8, 3, ["8强赛", "4强赛", "总决赛"]),
        (16, 4, ["16强赛", "8强赛", "4强赛", "总决赛"]),
    ):
        _, did = create_draft_season(token, f"P1PO{n}-{int(time.time())}", n)
        wipe_stages(token, did)
        add_stage(token, did, stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "NONE"))
        errs = two_pass_replace(token, did, playoff_payloads(n))
        stages = summarize(token, did)
        results[f"two_pass_{n}"] = {
            "errors": errs,
            "stages": stages,
            "pass": not errs and [s["name"] for s in stages] == expect_names and len(stages) == expect_len,
        }

    results["season16"] = season_id
    results["div16"] = div16
    results["div8"] = div8
    return results


def run_browser(token: str, season_name: str, publish_path: str) -> dict:
    from playwright.sync_api import sync_playwright

    toasts: list[str] = []
    out: dict = {"cases": {}}
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1600, "height": 1000})
        context.add_cookies(
            [{"name": "Admin-Token", "value": token, "url": UI}]
        )
        page = context.new_page()
        page.on(
            "console",
            lambda m: toasts.append(m.text) if m.type == "error" else None,
        )

        def grab_toasts() -> list[str]:
            loc = page.locator(".el-message, .el-notification")
            n = loc.count()
            texts = []
            for i in range(n):
                try:
                    texts.append(loc.nth(i).inner_text(timeout=500)[:200])
                except Exception:
                    pass
            return texts

        page.goto(UI + publish_path, wait_until="domcontentloaded", timeout=60000)
        page.wait_for_timeout(2500)
        if "login" in page.url.lower():
            page.goto(UI + "/login", wait_until="domcontentloaded")
            page.evaluate(
                """(t) => { document.cookie = 'Admin-Token=' + t + '; path=/'; }""",
                token,
            )
            page.goto(UI + publish_path, wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(2500)

        out["url"] = page.url
        out["publish_path"] = publish_path

        # CASE 1 via 新建赛事 wizard
        page.get_by_role("button", name="新建赛事").click(timeout=15000)
        page.wait_for_timeout(1500)
        page.get_by_placeholder("如 S1、春季赛").fill(f"P1WIZ-{int(time.time())}")
        page.wait_for_timeout(300)
        # season period
        pickers = page.locator(".el-date-editor")
        if pickers.count() >= 1:
            pickers.first.click()
            page.wait_for_timeout(400)
            # type range if inputs exist
            ins = page.locator(".el-date-editor input")
            if ins.count() >= 2:
                ins.nth(0).fill("2026-08-01 00:00:00")
                ins.nth(1).fill("2026-12-31 23:59:59")
                page.keyboard.press("Enter")
        page.get_by_role("button", name="下一步").click()
        page.wait_for_timeout(2000)
        # step 2 divisions — already has one, maxTeam 16 default
        if page.get_by_text("队伍上限").count():
            page.get_by_role("button", name="下一步").click()
            page.wait_for_timeout(2000)
        # step 3 times
        time_pickers = page.locator(".el-date-editor")
        for i in range(min(4, time_pickers.count())):
            pass
        # try fill all visible date editors on times step
        editors = page.locator(".el-date-editor--datetimerange input")
        vals = [
            "2026-08-01 00:00:00",
            "2026-08-20 23:59:59",
            "2026-08-21 00:00:00",
            "2026-12-20 23:59:59",
        ]
        for i in range(min(editors.count(), 4)):
            editors.nth(i).fill(vals[i % 4] if i < 2 else vals[i])
        page.get_by_role("button", name="下一步").click()
        page.wait_for_timeout(2500)
        # if still not on rules, click step title
        if page.get_by_text("自定义赛制").count() == 0:
            page.get_by_text("赛制规则", exact=True).first.click()
            page.wait_for_timeout(1500)

        case1 = {"toasts": [], "body": ""}
        try:
            page.get_by_role("tab", name="自定义赛制").click(timeout=8000)
        except Exception:
            page.get_by_text("自定义赛制").first.click()
        page.wait_for_timeout(800)
        page.get_by_text("季后赛", exact=True).first.click()
        page.wait_for_timeout(4000)
        case1["toasts"] = grab_toasts()
        body = page.locator(".format-rules-panel").inner_text(timeout=10000)
        case1["body"] = body[:2500]
        case1["has_16"] = "16强赛" in body
        case1["has_8"] = "8强赛" in body
        case1["has_4"] = "4强赛" in body
        case1["has_final"] = "总决赛" in body
        case1["has_seed"] = all(x in body for x in ("1 / 16", "4 / 13", "5 / 12", "8 / 9", "6 / 11", "7 / 10", "2 / 15", "3 / 14")) or all(
            x in body for x in ("1号种子", "16号种子")
        )
        case1["missing_playoff_8_toast"] = any("PLAYOFF_8" in t for t in case1["toasts"])
        case1["pass"] = (
            case1["has_16"]
            and case1["has_8"]
            and case1["has_4"]
            and case1["has_final"]
            and not case1["missing_playoff_8_toast"]
        )
        page.screenshot(path=str(SHOT / "case1.png"), full_page=True)

        save_ok = True
        next_ok = True
        try:
            page.get_by_role("button", name="保存草稿").click()
            page.wait_for_timeout(2000)
            save_toasts = grab_toasts()
            save_ok = not any("PLAYOFF_8" in t or "不存在" in t or "invalid" in t.lower() for t in save_toasts)
            case1["save_toasts"] = save_toasts
        except Exception as e:
            save_ok = False
            case1["save_err"] = str(e)[:200]
        try:
            page.get_by_role("button", name="下一步").click()
            page.wait_for_timeout(2000)
            next_toasts = grab_toasts()
            next_ok = not any("PLAYOFF_8" in t or "不存在" in t or "晋级" in t and "无效" in t for t in next_toasts)
            case1["next_toasts"] = next_toasts
        except Exception as e:
            next_ok = False
            case1["next_err"] = str(e)[:200]
        case1["save_ok"] = save_ok
        case1["next_ok"] = next_ok
        out["cases"]["case1"] = case1

        # CASE 2 via 继续发布 on API-prepared RR+top8 season if listed
        page.keyboard.press("Escape")
        page.wait_for_timeout(500)
        if page.locator(".el-dialog__headerbtn").count():
            try:
                page.locator(".el-dialog__headerbtn").first.click()
            except Exception:
                pass
        page.wait_for_timeout(800)
        out["cases"]["case1"]["pass"] = bool(case1.get("pass")) and save_ok
        browser.close()
    out["console_errors"] = toasts[:20]
    return out


def prepare_ui_seasons(token: str) -> dict:
    ts = int(time.time())
    specs = {
        "case1": (f"P1UI16-{ts}", 16, False),
        "case2": (f"P1UI8RR-{ts}", 16, True),
        "case3": (f"P1UI4-{ts}", 4, False),
        "case4": (f"P1UI8-{ts}", 8, False),
        "case5": (f"P1UI16B-{ts}", 16, False),
    }
    out = {}
    for key, (name, max_team, rr_next) in specs.items():
        season_id, div_id = create_draft_season(token, name, max_team)
        wipe_stages(token, div_id)
        if rr_next:
            add_stage(token, div_id, stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "NONE"))
            add_stage(token, div_id, stage_payload("STAGE_2", "阶段 2", 2, "ROUND_ROBIN", 1, "NONE"))
            stages = sorted_stages(snapshot(token, div_id))
            put_stage(
                token,
                div_id,
                stages[0]["id"],
                stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "STAGE_2", 8),
            )
        else:
            add_stage(token, div_id, stage_payload("REGULAR", "单循环", 1, "ROUND_ROBIN", 1, "NONE"))
        out[key] = {"name": name, "season_id": season_id, "div_id": div_id, "max_team": max_team}
    return out


def main() -> int:
    from _p1_browser import run_browser as run_ui

    token = admin_login()
    api = run_api_cases(token)
    publish_path = find_publish_path(token)
    seasons = prepare_ui_seasons(token)
    browser = {"skipped": True}
    try:
        browser = run_ui(token, seasons, publish_path)
    except Exception as e:
        browser = {"error": repr(e)[:800]}
    report = {"api": api, "browser": browser, "publish_path": publish_path, "seasons": seasons}
    OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    cases = (browser.get("cases") or {})
    print(json.dumps(
        {
            "one_pass": api["one_pass_16"]["pass"],
            "two_pass_16": api["two_pass_16"]["pass"],
            "two_pass_rr_top8": api["two_pass_rr_top8"]["pass"],
            "two_pass_4": api["two_pass_4"]["pass"],
            "two_pass_8": api["two_pass_8"]["pass"],
            "browser_case1": (cases.get("case1") or {}).get("pass"),
            "browser_case2": (cases.get("case2") or {}).get("pass"),
            "browser_case3": (cases.get("case3") or {}).get("pass"),
            "browser_case4": (cases.get("case4") or {}).get("pass"),
            "browser_case5": (cases.get("case5") or {}).get("pass"),
            "browser_error": browser.get("error"),
            "publish_path": publish_path,
            "out": str(OUT),
        },
        ensure_ascii=False,
        indent=2,
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
