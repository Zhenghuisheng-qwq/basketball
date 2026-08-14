#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Read-only Legacy smoke: latest Admin 18081 + Web 18882 against historical basketball."""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request

ADMIN = "http://127.0.0.1:18081"
WEB = "http://127.0.0.1:18882"
DIVISION_ID = 189
SEASON_ID = 154


def http(base: str, method: str, path: str, body=None, token=None, timeout=40):
    url = base.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            payload["_http"] = resp.status
            return payload
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "ignore") or "{}"
        try:
            payload = json.loads(raw)
        except Exception:
            payload = {"_raw": raw[:400]}
        payload["_http"] = exc.code
        return payload
    except Exception as exc:
        return {"_http": 0, "code": 0, "msg": str(exc)}


def wait_up(base: str, path: str, tries: int = 90) -> bool:
    for _ in range(tries):
        r = http(base, "GET", path)
        if r.get("_http") in (200, 401, 403, 404) or r.get("code") in (200, 401, 403, 500):
            return True
        time.sleep(2)
    return False


def ok(name: str, cond: bool, detail: str = "") -> bool:
    mark = "PASS" if cond else "FAIL"
    print(f"[{mark}] {name} {detail[:240]}", flush=True)
    return cond


def main() -> int:
    print("wait admin 18081...", flush=True)
    if not wait_up(ADMIN, "/login"):
        print("BLOCKED admin 18081 not up")
        return 2
    print("wait web 18882...", flush=True)
    if not wait_up(WEB, "/basket/v4/public/season/list"):
        print("BLOCKED web 18882 not up")
        return 2

    login = http(ADMIN, "POST", "/login", {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
    token = login.get("token")
    if not ok("admin.login", login.get("code") == 200 and bool(token), str(login.get("msg") or login.get("code"))):
        return 1

    fails = 0

    detail = http(ADMIN, "GET", f"/basket/v4/division/{DIVISION_ID}", token=token)
    d = detail.get("data") or {}
    fails += not ok(
        "admin.division.detail",
        detail.get("_http") == 200 and detail.get("code") == 200 and str(d.get("id") or d.get("divisionId") or "") == str(DIVISION_ID),
        f"http={detail.get('_http')} code={detail.get('code')} msg={detail.get('msg')} name={d.get('divisionName')}",
    )

    ov = http(ADMIN, "GET", f"/basket/v4/division/{DIVISION_ID}/workflow-overview", token=token)
    ovd = ov.get("data") or {}
    msg = str(ov.get("msg") or "")
    fails += not ok(
        "admin.workflow-overview",
        ov.get("_http") == 200 and ov.get("code") == 200 and isinstance(ovd, dict) and ovd.get("legacy") is True,
        f"http={ov.get('_http')} code={ov.get('code')} msg={msg} legacy={ovd.get('legacy')} readOnly={ovd.get('readOnly')} hint={ovd.get('workflowHint')}",
    )
    if "锁定" in msg and "名单" in msg and ov.get("code") != 200:
        fails += not ok("admin.workflow-overview.no-roster-required", False, msg)

    stages = http(ADMIN, "GET", f"/basket/v4/admin/stages/list?divisionId={DIVISION_ID}", token=token)
    srows = stages.get("data") or stages.get("rows") or []
    if isinstance(srows, dict):
        srows = srows.get("rows") or srows.get("list") or []
    fails += not ok(
        "admin.stage.list",
        stages.get("_http") == 200 and stages.get("code") == 200 and isinstance(srows, list) and len(srows) > 0,
        f"http={stages.get('_http')} code={stages.get('code')} n={len(srows) if isinstance(srows, list) else type(srows)}",
    )

    matches = http(ADMIN, "GET", f"/basket/v4/match/list?divisionId={DIVISION_ID}&pageNum=1&pageSize=50", token=token)
    mrows = matches.get("rows") or matches.get("data") or []
    if isinstance(mrows, dict):
        mrows = mrows.get("rows") or mrows.get("list") or []
    fails += not ok(
        "admin.match.list",
        matches.get("_http") == 200 and matches.get("code") == 200 and isinstance(mrows, list) and len(mrows) > 0,
        f"http={matches.get('_http')} code={matches.get('code')} n={len(mrows) if isinstance(mrows, list) else type(mrows)} msg={matches.get('msg')}",
    )

    if isinstance(mrows, list) and mrows:
        mid = mrows[0].get("id") or mrows[0].get("matchId")
        md = http(ADMIN, "GET", f"/basket/v4/match/{mid}", token=token)
        fails += not ok(
            "admin.match.detail",
            md.get("_http") in (200, 404) and md.get("code") in (200, 404, 500) and md.get("_http") != 500,
            f"http={md.get('_http')} code={md.get('code')} msg={md.get('msg')}",
        )
        if md.get("code") == 500:
            fails += 1

    sel = http(ADMIN, "GET", f"/basket/v4/division/selector-list?pageNum=1&pageSize=200", token=token)
    sdata = sel.get("rows") or sel.get("data") or []
    if isinstance(sdata, dict):
        sdata = sdata.get("rows") or sdata.get("list") or []
    found = False
    if isinstance(sdata, list):
        for item in sdata:
            iid = item.get("divisionId") or item.get("id")
            if str(iid) == str(DIVISION_ID):
                found = True
                break
    fails += not ok(
        "admin.selector-list.contains-189",
        sel.get("_http") == 200 and sel.get("code") == 200 and found,
        f"http={sel.get('_http')} code={sel.get('code')} found={found} n={len(sdata) if isinstance(sdata, list) else type(sdata)}",
    )

    season = http(WEB, "GET", f"/basket/v4/public/season/{SEASON_ID}")
    fails += not ok(
        "web.public.season",
        season.get("_http") == 200 and season.get("code") == 200,
        f"http={season.get('_http')} code={season.get('code')} msg={season.get('msg')}",
    )

    div = http(WEB, "GET", f"/basket/v4/public/division/{DIVISION_ID}")
    fails += not ok(
        "web.public.division",
        div.get("_http") == 200 and div.get("code") == 200,
        f"http={div.get('_http')} code={div.get('code')} msg={div.get('msg')}",
    )

    plist = http(WEB, "GET", f"/basket/v4/public/match/list?divisionId={DIVISION_ID}")
    pdata = plist.get("data") or plist.get("rows") or []
    if isinstance(pdata, dict):
        pdata = pdata.get("rows") or pdata.get("list") or pdata.get("records") or []
    fails += not ok(
        "web.public.match.list.not-empty",
        plist.get("_http") == 200 and plist.get("code") == 200 and isinstance(pdata, list) and len(pdata) > 0,
        f"http={plist.get('_http')} code={plist.get('code')} n={len(pdata) if isinstance(pdata, list) else type(pdata)} msg={plist.get('msg')}",
    )

    if isinstance(pdata, list) and pdata:
        pmid = pdata[0].get("id") or pdata[0].get("matchId")
        pmd = http(WEB, "GET", f"/basket/v4/public/match/{pmid}")
        fails += not ok(
            "web.public.match.detail",
            pmd.get("_http") == 200 and pmd.get("code") == 200,
            f"http={pmd.get('_http')} code={pmd.get('code')} msg={pmd.get('msg')}",
        )

    teams = http(WEB, "GET", f"/basket/v4/public/team/list?divisionId={DIVISION_ID}")
    tdata = teams.get("data") or teams.get("rows") or []
    if isinstance(tdata, dict):
        tdata = tdata.get("rows") or tdata.get("list") or []
    fails += not ok(
        "web.public.team.list",
        teams.get("_http") == 200 and teams.get("code") == 200,
        f"http={teams.get('_http')} code={teams.get('code')} n={len(tdata) if isinstance(tdata, list) else type(tdata)}",
    )

    if isinstance(srows, list) and srows:
        sid = srows[0].get("id") or srows[0].get("stageId")
        br = http(WEB, "GET", f"/basket/v4/public/stage/{sid}/bracket")
        fails += not ok(
            "web.public.bracket",
            br.get("_http") == 200 and br.get("code") in (200, 500) and br.get("_http") != 500,
            f"http={br.get('_http')} code={br.get('code')} msg={br.get('msg')}",
        )
        if br.get("code") == 500:
            fails += 1

    print(f"FAILS={fails}", flush=True)
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
