#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Round-2 remaining E2E: one season per division, teams created in that division."""
from __future__ import annotations

import importlib.util
import json
import sys
import traceback
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("e2e1", ROOT / "scripts" / "cnpa_v13_final_full_e2e.py")
e = importlib.util.module_from_spec(spec)
spec.loader.exec_module(e)
fs = e.fs

orig_setup = e.setup_team_div


def setup_team_div(token, season_id, name, template, max_team, teams, game_mode="FIVE_V_FIVE", **extra):
    did = e.create_division(token, season_id, name, "TEAM", max_team, game_mode=game_mode)
    e.apply_and_lock(token, did, template, "TEAM", game_mode, **extra)
    e.open_reg(token, did)
    e.create_home_teams(token, did, teams)
    tokens = e.register_lock(token, did, teams)
    return did, tokens, e.member_tokens(teams, tokens)


e.setup_team_div = setup_team_div


def season(token, series, name):
    return e.create_season(token, e.SERIES[series], name, name + " 2026")


def teams_pool(prefix, n, offset):
    return fs.seed_org_pool(prefix, e.team_defs(prefix, n), offset=offset)


def main():
    token = fs.admin_login()
    e.log("round2 admin login ok")

    # Cup graph re-assert on RULE-FLOW using organizationMode
    rule_did = e.find_division("E2E-RULE-FLOW")
    if rule_did:
        st = e.stages(token, rule_did)
        grouped = any((s.get("organizationMode") or "") == "GROUPED" or (s.get("groupCount") or 0) for s in st)
        ko = any((s.get("competitionFormat") or "") == "SINGLE_ELIMINATION" for s in st)
        e.record("rule.apply.cup.graph", grouped and ko, str([(s.get("stageCode"), s.get("organizationMode"), s.get("competitionFormat"), s.get("groupCount")) for s in st]))

    cases = [
        ("arena10", "ARENA", "E2E-ARENA10-S", "E2E-ARENA10", "ARENA_STANDARD_V1", 16, 10, "A10", 40000, e.run_arena10, {}),
        ("league8", "LEAGUE", "E2E-LEAGUE8-S", "E2E-LEAGUE8", "LEAGUE_STANDARD_V1", 8, 8, "L8", 41000, e.run_league8, {}),
        ("leagueDrr", "LEAGUE", "E2E-LEAGUE-DRR-S", "E2E-LEAGUE8-DRR", "LEAGUE_STANDARD_V1", 8, 8, "LD", 42000, e.run_league_drr, {"roundRobinCycles": 2}),
        ("cup8", "CUP", "E2E-CUP8-S", "E2E-CUP8", "CUP_STANDARD_V1", 8, 8, "C8", 43000, e.run_cup8, {}),
        ("openTeam", "OPEN", "E2E-OPEN-TEAM-S", "E2E-OPEN-TEAM", "OPEN_STANDARD_V1", 8, 8, "OT", 44000, e.run_open_team, {}),
        ("crossAL", "ARENA", "E2E-CROSS-AL-S", "E2E-CROSS-ARENA-LEAGUE", "LEAGUE_STANDARD_V1", 8, 8, "XA", 45000, None, {}),
        ("isoA", "ARENA", "E2E-ISO-A-S", "E2E-CROSS-ISO-A", "ARENA_STANDARD_V1", 8, 8, "IA", 46000, None, {}),
        ("isoB", "LEAGUE", "E2E-ISO-B-S", "E2E-CROSS-ISO-B", "ARENA_STANDARD_V1", 8, 8, "IB", 47000, None, {}),
        ("unpub", "ARENA", "E2E-UNPUB-S", "E2E-CROSS-UNPUB", "ARENA_STANDARD_V1", 8, 8, "UP", 48000, None, {}),
        ("candidate", "LEAGUE", "E2E-CAND-S", "E2E-CROSS-CANDIDATE", "LEAGUE_STANDARD_V1", 8, 10, "CD", 49000, None, {}),
    ]

    prepared = {}
    for key, series, sname, dname, tpl, mx, n, prefix, offset, fn, extra in cases:
        try:
            sid = season(token, series, sname)
            tms = teams_pool(prefix, n, offset)
            if fn is e.run_arena10:
                e.run_arena10(token, sid, tms)
            elif fn is e.run_league8:
                lid = e.run_league8(token, sid, tms)
                e.run_league_bo_assert(token, lid)
                e.run_capability_toast(token, lid)
            elif fn is e.run_league_drr:
                e.run_league_drr(token, sid, tms)
            elif fn is e.run_cup8:
                e.run_cup8(token, sid, tms)
            elif fn is e.run_open_team:
                e.run_open_team(token, sid, tms)
            else:
                did, tokens, mt = setup_team_div(token, sid, dname, tpl, mx, tms, **extra)
                prepared[key] = (did, tokens, mt, tms, sid)
                e.REPORT["ids"][key] = did
        except Exception as exc:
            e.record(key + ".e2e", False, repr(exc))
            e.log(traceback.format_exc())

    try:
        pers = fs.seed_org_pool("OP2", e.person_defs("OP", 12), offset=50000)[0]
        osid = season(token, "OPEN", "E2E-OPEN-PER-S")
        e.run_open_personal(token, osid, pers)
    except Exception as exc:
        e.record("open.personal.e2e", False, repr(exc))
        e.log(traceback.format_exc())

    try:
        pers2 = fs.seed_org_pool("OA2", e.person_defs("OA", 12), offset=51000)[0]
        lsid = season(token, "LEAGUE", "E2E-CROSS-LA-S")
        did2 = e.create_division(token, lsid, "E2E-CROSS-LEAGUE-ARENA-PERSONAL", "PERSONAL", 8)
        e.apply_and_lock(token, did2, "ARENA_STANDARD_V1", "PERSONAL")
        e.open_reg(token, did2)
        e.register_persons(token, did2, pers2)
        e.close_reg(token, did2)
        fr = e.req(f"{e.ADMIN}/basket/v4/admin/personal-formation/{did2}/generate", "POST", token=token)
        e.record("cross.league_arena.formation", fr.get("code") == 200, str(fr.get("msg") or fr.get("code")))
        if fr.get("code") == 200:
            e.req(f"{e.ADMIN}/basket/v4/admin/personal-formation/{did2}/lock", "POST", token=token)
        st2 = e.stages(token, did2)
        e.record("cross.league_arena.se", any((s.get("competitionFormat") or "") == "SINGLE_ELIMINATION" for s in st2),
                 str([(s.get("stageCode"), s.get("competitionFormat")) for s in st2]))
        if "crossAL" in prepared:
            st1 = e.stages(token, prepared["crossAL"][0])
            fm = [(s.get("stageCode"), s.get("competitionFormat")) for s in st1]
            e.record("cross.arena_league.rr_playoff",
                     any("ROUND_ROBIN" in str(x) for x in fm) and any("SINGLE_ELIMINATION" in str(x) for x in fm), str(fm))
        e.dump_invariant("crossLA", did2)
    except Exception as exc:
        e.record("cross.e2e", False, repr(exc))
        e.log(traceback.format_exc())

    try:
        if "isoA" in prepared and "isoB" in prepared:
            did_a, did_b = prepared["isoA"][0], prepared["isoB"][0]
            for d, name in ((did_a, "isoA"), (did_b, "isoB")):
                st = e.stages(token, d)
                e.seed_draw_confirm(token, d, int(st[0]["id"]), seed_n=4)
                e.generate_schedule_publish(token, int(st[0]["id"]), init=False)
            ets_a = {int(r["id"]) for r in fs.db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did_a,))}
            ets_b = {int(r["id"]) for r in fs.db_query("SELECT id FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did_b,))}
            e.record("iso.event_team.disjoint", ets_a.isdisjoint(ets_b) and bool(ets_a) and bool(ets_b), f"A={ets_a} B={ets_b}", "P0")
            sa, sb = e.admin_series(token, did_a), e.admin_series(token, did_b)
            ids_a = {x.get("id") or x.get("seriesId") for x in sa}
            ids_b = {x.get("id") or x.get("seriesId") for x in sb}
            e.record("iso.series.disjoint", ids_a.isdisjoint(ids_b), f"A={ids_a} B={ids_b}", "P0")
    except Exception as exc:
        e.record("iso.e2e", False, repr(exc), "P0")
        e.log(traceback.format_exc())

    try:
        if "unpub" in prepared:
            did = prepared["unpub"][0]
            st = e.stages(token, did)
            sid = int(st[0]["id"])
            e.seed_draw_confirm(token, did, sid, seed_n=2)
            e.generate_schedule_publish(token, sid, init=False, publish=False)
            pub, matches = e.public_schedule(did), e.public_matches(did)
            e.record("unpub.web.hidden", len(pub) == 0 and len(matches) == 0, f"schedule={len(pub)} matches={len(matches)}", "P0")
    except Exception as exc:
        e.record("unpub.e2e", False, repr(exc), "P0")
        e.log(traceback.format_exc())

    try:
        if "candidate" in prepared:
            did = prepared["candidate"][0]
            e.assert_roster(did, 8, 2, "candidate")
            regs = fs.db_query("SELECT id, participation_role, candidate_order FROM basket_registration WHERE division_id=%s AND is_delete=0", (did,))
            formal = next(r for r in regs if r["participation_role"] == "FORMAL")
            cand = next(r for r in regs if r["participation_role"] == "CANDIDATE" and r.get("candidate_order") == 1)
            pr = e.req(f"{e.ADMIN}/basket/v4/divisions/{did}/roster/team/promote-candidate", "POST",
                       {"formalRegistrationId": int(formal["id"]), "candidateRegistrationId": int(cand["id"]), "reason": "E2E"}, token)
            e.record("candidate.promote", pr.get("code") == 200, str(pr.get("msg") or pr.get("code")))
    except Exception as exc:
        e.record("candidate.e2e", False, repr(exc))
        e.log(traceback.format_exc())

    try:
        p3 = fs.seed_org_pool("F3b", e.person_defs("F3", 5), offset=52000)[0]
        s3 = season(token, "OPEN", "E2E-3V3-S")
        did3 = e.create_division(token, s3, "E2E-OPEN-3V3-SHORT", "PERSONAL", 8, game_mode="THREE_V_THREE")
        e.apply_and_lock(token, did3, "OPEN_STANDARD_V1", "PERSONAL", "THREE_V_THREE")
        e.open_reg(token, did3)
        e.register_persons(token, did3, p3)
        e.close_reg(token, did3)
        r3 = e.req(f"{e.ADMIN}/basket/v4/admin/personal-formation/{did3}/generate", "POST", token=token)
        e.record("formation.3v3.reject", r3.get("code") != 200, str(r3.get("msg") or r3.get("code")))
        p5 = fs.seed_org_pool("F5b", e.person_defs("F5", 9), offset=53000)[0]
        s5 = season(token, "OPEN", "E2E-5V5-S")
        did5 = e.create_division(token, s5, "E2E-OPEN-5V5-SHORT", "PERSONAL", 8)
        e.apply_and_lock(token, did5, "OPEN_STANDARD_V1", "PERSONAL")
        e.open_reg(token, did5)
        e.register_persons(token, did5, p5)
        e.close_reg(token, did5)
        r5 = e.req(f"{e.ADMIN}/basket/v4/admin/personal-formation/{did5}/generate", "POST", token=token)
        e.record("formation.5v5.reject", r5.get("code") != 200, str(r5.get("msg") or r5.get("code")))
    except Exception as exc:
        e.record("formation.reject", False, repr(exc))
        e.log(traceback.format_exc())

    e.REPORT["endedAt"] = datetime.now().isoformat(timespec="seconds")
    e.REPORT["round"] = 2
    out = ROOT / "scripts" / "cnpa_v13_final_full_e2e_round2.json"
    out.write_text(json.dumps(e.REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    fails = [k for k, v in e.REPORT["cases"].items() if not v["ok"]]
    e.log(f"ROUND2 DONE pass={sum(1 for v in e.REPORT['cases'].values() if v['ok'])} fail={len(fails)}")
    for f in fails:
        e.log(f"  FAIL {f}: {e.REPORT['cases'][f]['detail'][:180]}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
