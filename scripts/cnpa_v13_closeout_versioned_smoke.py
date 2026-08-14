#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json, sys, time, urllib.error, urllib.request

ADMIN = "http://127.0.0.1:18080"
WEB = "http://127.0.0.1:18881"

def http(base, method, path, body=None, token=None, timeout=40):
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

def wait_up(base, path, tries=80):
    for _ in range(tries):
        r = http(base, "GET", path)
        if r.get("_http") in (200, 401, 403, 404) or r.get("code") in (200, 401, 403, 500):
            return True
        time.sleep(2)
    return False

def ok(name, cond, detail=""):
    print(f"[{'PASS' if cond else 'FAIL'}] {name} {str(detail)[:260]}", flush=True)
    return cond

def rows_of(resp):
    data = resp.get("data") or resp.get("rows") or []
    if isinstance(data, dict):
        data = data.get("rows") or data.get("list") or data.get("records") or []
    return data if isinstance(data, list) else []

def main():
    if not wait_up(ADMIN, "/login"):
        print("BLOCKED fresh admin 18080")
        return 2
    if not wait_up(WEB, "/basket/v4/public/season/list"):
        print("BLOCKED fresh web 18881")
        return 2
    login = http(ADMIN, "POST", "/login", {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
    token = login.get("token")
    if not ok("fresh.admin.login", login.get("code") == 200 and bool(token), login.get("msg")):
        return 1
    fails = 0
    std = http(ADMIN, "GET", "/basket/v4/format-template/standard", token=token)
    codes = [x.get("templateCode") for x in rows_of(std) or (std.get("data") if isinstance(std.get("data"), list) else [])]
    if not codes and isinstance(std.get("data"), list):
        codes = [x.get("templateCode") for x in std["data"]]
    fails += not ok("01.standard.templates", std.get("code") == 200 and all(c in codes for c in ("LEAGUE_STANDARD_V1","ARENA_STANDARD_V1","CUP_STANDARD_V1","OPEN_STANDARD_V1")), codes)

    sel = http(ADMIN, "GET", "/basket/v4/division/selector-list?pageNum=1&pageSize=50", token=token)
    srows = rows_of(sel)
    names = {str(x.get("divisionId") or x.get("id")): x.get("divisionName") for x in srows}
    fails += not ok("selector.r9", "3" in names and "5" in names, names)

    for did, label in ((3, "LEAGUE"), (5, "CUP"), (6, "OPEN"), (14, "OPEN_TEAM"), (11, "LEAGUE_PERSONAL")):
        ov = http(ADMIN, "GET", f"/basket/v4/division/{did}/workflow-overview", token=token)
        d = ov.get("data") or {}
        ctx = d.get("context") or {}
        fails += not ok(f"03.overview.{label}", ov.get("code") == 200 and d.get("legacy") is False, f"legacy={d.get('legacy')} stages={len(d.get('stages') or [])} mode={ctx.get('registrationMode')} hint={d.get('workflowHint')}")
        snap = http(ADMIN, "GET", f"/basket/v4/divisions/{did}/format-snapshot", token=token)
        fails += not ok(f"01.snapshot.{label}", snap.get("code") == 200, snap.get("msg"))
        stages = http(ADMIN, "GET", f"/basket/v4/admin/stages/list?divisionId={did}", token=token)
        st = stages.get("data") if isinstance(stages.get("data"), list) else rows_of(stages)
        fails += not ok(f"04.stages.{label}", stages.get("code") == 200 and len(st) > 0, f"n={len(st)}")
        if st:
            sid = st[0].get("id") or st[0].get("stageId")
            units = http(ADMIN, "GET", f"/basket/v4/admin/stages/{sid}/schedule-units", token=token)
            fails += not ok(f"05.schedule-units.{label}", units.get("code") == 200, units.get("msg"))
            prev = http(ADMIN, "GET", f"/basket/v4/admin/stages/{sid}/publish-preview", token=token)
            fails += not ok(f"06.publish-preview.{label}", prev.get("code") == 200, prev.get("msg"))

    regs = http(ADMIN, "GET", "/basket/v4/admin/registrations/list?divisionId=3&pageNum=1&pageSize=20", token=token)
    if regs.get("code") != 200:
        regs = http(ADMIN, "GET", "/basket/v4/registration-audit/list?divisionId=3&pageNum=1&pageSize=20", token=token)
    fails += not ok("02.team.regs", regs.get("code") == 200, f"code={regs.get('code')} msg={regs.get('msg')} n={len(rows_of(regs))}")

    roster = http(ADMIN, "GET", "/basket/v4/divisions/3/roster/team", token=token)
    rd = roster.get("data") or {}
    fails += not ok("02.team.roster", roster.get("code") == 200, f"status={rd.get('rosterStatus')} formal={rd.get('formalCount')} cand={rd.get('candidateCount')}")

    form_open = http(ADMIN, "GET", "/basket/v4/admin/personal-formation/6/workspace", token=token)
    fails += not ok("02.open.formation", form_open.get("code") == 200, form_open.get("msg"))
    form_league_p = http(ADMIN, "GET", "/basket/v4/admin/personal-formation/11/workspace", token=token)
    fails += not ok("02.league.personal.formation", form_league_p.get("code") == 200, form_league_p.get("msg"))

    cup_st = http(ADMIN, "GET", "/basket/v4/admin/stages/list?divisionId=5", token=token)
    cst = cup_st.get("data") if isinstance(cup_st.get("data"), list) else rows_of(cup_st)
    kid = None
    for s in cst:
        if (s.get("stageCode") or "") == "KNOCKOUT" or (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION":
            kid = s.get("id") or s.get("stageId")
    if kid:
        pe = http(ADMIN, "GET", f"/basket/v4/admin/stages/{kid}/pairing-execution", token=token)
        entries = ((pe.get("data") or {}).get("entries") or (pe.get("data") or {}).get("entryList") or [])
        keys = [str(e.get("groupRankKey") or e.get("sourceLabel") or e.get("slotLabel") or "") for e in entries]
        joined = " ".join(keys)
        fails += not ok("04.cup.group_cross.backend", pe.get("code") == 200, f"keys={keys[:12]}")
        pub_s = http(WEB, "GET", f"/basket/v4/public/match-series/stage/{kid}")
        fails += not ok("web.cup.series", pub_s.get("code") == 200, f"n={len(rows_of(pub_s) or (pub_s.get('data') if isinstance(pub_s.get('data'), list) else []))}")

    league_st = http(ADMIN, "GET", "/basket/v4/admin/stages/list?divisionId=3", token=token)
    lst = league_st.get("data") if isinstance(league_st.get("data"), list) else rows_of(league_st)
    po = next((s for s in lst if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION"), None)
    if po:
        psid = po.get("id") or po.get("stageId")
        series = http(ADMIN, "GET", f"/basket/v4/match-series/stage/{psid}", token=token)
        sdata = series.get("data") if isinstance(series.get("data"), list) else rows_of(series)
        fails += not ok("05.league.series", series.get("code") == 200 and len(sdata) > 0, f"n={len(sdata)}")
        pub = http(WEB, "GET", f"/basket/v4/public/match-series/stage/{psid}")
        pdata = pub.get("data") if isinstance(pub.get("data"), list) else rows_of(pub)
        fails += not ok("web.league.bo.series.cards", pub.get("code") == 200 and 0 < len(pdata) <= 8, f"n={len(pdata)}")

    open_teams = http(WEB, "GET", "/basket/v4/public/team/list?divisionId=6")
    otd = open_teams.get("data") if isinstance(open_teams.get("data"), list) else rows_of(open_teams)
    fails += not ok("web.open.temp.teams", open_teams.get("code") == 200 and len(otd) >= 1, f"n={len(otd)}")
    if otd:
        tid = otd[0].get("id") or otd[0].get("eventTeamId")
        td = http(WEB, "GET", f"/basket/v4/public/team/{tid}")
        tdd = td.get("data") or {}
        fails += not ok("web.open.temp.team.detail", td.get("code") == 200 and bool(tdd.get("teamName") or tdd.get("eventTeamName")), str(tdd)[:180])

    season = http(WEB, "GET", "/basket/v4/public/season/list")
    fails += not ok("web.season.list", season.get("code") == 200, season.get("msg"))

    print(f"FAILS={fails}", flush=True)
    return 1 if fails else 0

if __name__ == "__main__":
    sys.exit(main())
