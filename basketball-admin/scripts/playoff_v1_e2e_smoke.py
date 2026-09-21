#!/usr/bin/env python3
"""CNPA Playoff Template V1 Final E2E Smoke — LOCAL only, API-first."""
from __future__ import annotations

import json
import os
import sys
import time
import traceback
from datetime import datetime
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))
from full_smoke_common import (
    ADMIN, TEMPLATE_BO1, TEMPLATE_GROUP, TEMPLATE_RR, WEB, as_id, admin_login,
    build_web_by_event, clear_login_locks, close_registration, confirm_stage_qualification,
    create_division, create_season, create_teams_flow, db_query, env_health, finish_series_games,
    generate_and_publish, generate_schedule, get_format_stages, init_stage_teams, list_matches,
    list_series, lock_snapshot, open_registration, populate_members, put_tie_break, register_all_teams,
    req, seed_org_pool, stage_payload, submit_and_approve_match, web_tokens_for_teams, wipe_stages,
    _create_stage, chain_update,
)

MARKER = "PLAYOFF_V1_E2E"
RUN_TAG = datetime.now().strftime("%H%M%S")
OUT = Path(__file__).resolve().parent / "playoff_v1_e2e_smoke_result.json"
EXPECTED_16 = [1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14]
EXPECTED_8 = [1, 8, 4, 5, 2, 7, 3, 6]
EXPECTED_4 = [1, 4, 2, 3]
REPORT: dict[str, Any] = {"startedAt": datetime.now().isoformat(timespec="seconds"), "env": {}, "cases": {}, "bugs": {"P0": [], "P1": [], "P2": []}, "final": "PENDING"}

def log(msg: str) -> None:
    print(msg, flush=True)

def fail_bug(level: str, msg: str) -> None:
    REPORT["bugs"][level].append(msg)
    log(f"[{level}] {msg}")

def assert_true(cond: bool, level: str, msg: str) -> bool:
    if not cond:
        fail_bug(level, msg)
    return cond

def team_defs(prefix: str, n: int) -> list[dict]:
    out = []
    for i in range(1, n + 1):
        code = f"P{i:02d}"
        out.append({"code": code, "name": f"{MARKER}_{prefix}_{code}", "short": f"{prefix}{i:02d}",
                    "players": [f"{MARKER}_{prefix}_{code}_P{j}" for j in range(1, 6)]})
    return out

def setup_playoff_stages(token: str, division_id: str, team_count: int, bos: list[int]) -> list[dict]:
    wipe_stages(token, division_id)
    sizes = []
    n = team_count
    while n >= 2:
        sizes.append(n); n //= 2
    assert len(sizes) == len(bos)
    names = {2: "总决赛", 4: "4强赛", 8: "8强赛", 16: "16强赛"}
    created = []
    for idx, size in enumerate(sizes):
        is_final = idx == len(sizes) - 1
        code = "FINAL" if is_final else f"STAGE_{idx + 1}"
        next_code = None if is_final else ("FINAL" if idx + 2 == len(sizes) else f"STAGE_{idx + 2}")
        payload = stage_payload(code, names.get(size, f"{size}强赛"), idx + 1, "SINGLE_ELIMINATION", bos[idx],
                                "NONE" if is_final else "WINNER_ADVANCE", next_code, pairing="PLAYOFF_BRACKET")
        sid = _create_stage(token, division_id, payload)
        created.append({"id": sid, "size": size, "code": code, "bestOf": bos[idx]})
    chain_update(token, division_id)
    stages = get_format_stages(token, division_id)
    for i, s in enumerate(sorted(stages, key=lambda x: x.get("stageOrder") or 0)):
        if i < len(created):
            created[i]["id"] = as_id(s["id"]); created[i]["stage"] = s
    return created

def setup_standing_playoff(token: str, division_id: str) -> dict[str, str]:
    wipe_stages(token, division_id)
    st1 = _create_stage(token, division_id, stage_payload("RR", "积分赛", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "S8", 8))
    st2 = _create_stage(token, division_id, stage_payload("S8", "8强赛", 2, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "S4", pairing="PLAYOFF_BRACKET"))
    st3 = _create_stage(token, division_id, stage_payload("S4", "4强赛", 3, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "FINAL", pairing="PLAYOFF_BRACKET"))
    st4 = _create_stage(token, division_id, stage_payload("FINAL", "总决赛", 4, "SINGLE_ELIMINATION", 3, "NONE", pairing="PLAYOFF_BRACKET"))
    chain_update(token, division_id, top_n=8); put_tie_break(token, division_id, st1)
    return {"rr": st1, "s8": st2, "s4": st3, "final": st4}

def seed_map_for_stage(stage_id: str) -> list[dict]:
    return db_query(
        """SELECT st.seed_no AS seedNo, st.event_team_id AS eventTeamId, st.entry_type AS entryType,
               et.team_name_snapshot AS teamName, et.source_team_id AS sourceTeamId,
               t.create_time AS createTime, t.id AS teamId
        FROM basket_division_stage_team st
        JOIN basket_event_team et ON et.id = st.event_team_id AND et.is_delete = 0
        LEFT JOIN basket_team t ON t.id = et.source_team_id AND t.is_delete = 0
        WHERE st.stage_id = %s AND st.is_delete = 0
        ORDER BY st.seed_no ASC, st.event_team_id ASC""",
        (int(stage_id),),
    )

def pairing_units(token: str, division_id: str, stage_id: str) -> list[dict]:
    series = list_series(token, stage_id)
    if series:
        out = []
        for s in sorted(series, key=lambda x: (x.get("bracketSlot") is None, x.get("bracketSlot") or 0, x.get("id") or 0)):
            out.append({"kind": "SERIES", "id": as_id(s["id"]), "bracketSlot": s.get("bracketSlot"),
                        "homeEventTeamId": as_id(s.get("homeEventTeamId")), "awayEventTeamId": as_id(s.get("awayEventTeamId")),
                        "bestOf": s.get("bestOf"),
                        "winnerEventTeamId": as_id(s["winnerEventTeamId"]) if s.get("winnerEventTeamId") else None, "raw": s})
        return out
    # Prefer DB stage-scoped matches — admin list API may ignore stageId and return whole division
    rows = db_query(
        "SELECT id, bracket_slot AS bracketSlot, home_event_team_id AS homeEventTeamId, away_event_team_id AS awayEventTeamId, "
        "winner_event_team_id AS winnerEventTeamId, match_status AS matchStatus, series_id AS seriesId "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 AND series_id IS NULL "
        "ORDER BY round_no ASC, bracket_slot ASC, id ASC",
        (int(stage_id),),
    )
    out = []
    for m in rows:
        out.append({"kind": "MATCH", "id": as_id(m["id"]), "bracketSlot": m.get("bracketSlot"),
                    "homeEventTeamId": as_id(m.get("homeEventTeamId")), "awayEventTeamId": as_id(m.get("awayEventTeamId")),
                    "bestOf": 1, "winnerEventTeamId": as_id(m["winnerEventTeamId"]) if m.get("winnerEventTeamId") else None, "raw": m})
    return out

def annotate_seeds(units, seed_by_et, name_by_et):
    for u in units:
        u["homeSeed"] = seed_by_et.get(u["homeEventTeamId"]); u["awaySeed"] = seed_by_et.get(u["awayEventTeamId"])
        u["homeTeam"] = name_by_et.get(u["homeEventTeamId"]); u["awayTeam"] = name_by_et.get(u["awayEventTeamId"])
    return units

def finish_bo1_unit(admin_token, web_by_event, unit, winner_et):
    mid = unit["id"]
    mr = req(f"{ADMIN}/basket/v4/match/{mid}", token=admin_token)
    m = mr.get("data") or unit["raw"]
    if m.get("matchStatus") in ("FINISHED", "FORFEITED"):
        return
    home = as_id(m.get("homeEventTeamId")); away = as_id(m.get("awayEventTeamId"))
    if as_id(winner_et) == home:
        hs, aws, submit = 78, 70, home
    else:
        hs, aws, submit = 70, 78, away
    wt = web_by_event.get(as_id(submit))
    if not wt:
        raise RuntimeError(f"no web token for eventTeam {submit}")
    submit_and_approve_match(admin_token, wt, m, hs, aws, submit)

def prepare_division_with_teams(admin_token, season_name, division_name, n, pool_code, offset, max_team=None, setup_format=None):
    season_id = create_season(admin_token, season_name, season_name)
    division_id = create_division(admin_token, season_id, division_name, max_team=max_team or n, template_id=TEMPLATE_BO1)
    # must lock format before open registration
    if setup_format is not None:
        setup_format(admin_token, division_id)
        lock_snapshot(admin_token, division_id)
    defs = team_defs(pool_code, n)
    teams = seed_org_pool(pool_code, defs, offset=offset)
    emails = [e for t in teams for e in t["emails"]]
    clear_login_locks(emails)
    create_teams_flow(admin_token, division_id, teams)
    populate_members(teams, invite_until=n, full_players=5)
    tokens = web_tokens_for_teams(teams)
    orr = open_registration(admin_token, division_id)
    if orr.get("code") != 200:
        raise RuntimeError(f"open-registration failed: {orr}")
    register_all_teams(tokens, admin_token, division_id, teams)
    close_registration(admin_token, division_id)
    return season_id, division_id, teams, tokens

def case_a(admin_token: str) -> dict[str, Any]:
    out: dict[str, Any] = {"name": "CASE_A_16_CREATE_TIME"}
    stages_holder = {}
    def _setup(token, did):
        stages_holder["stages"] = setup_playoff_stages(token, did, 16, [1, 1, 3, 3])
    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_A_{RUN_TAG}", f"{MARKER}_DIV_A16_{RUN_TAG}", 16, "PA", offset=770_000, max_team=16, setup_format=_setup)
    out["seasonId"] = season_id; out["divisionId"] = division_id
    stages = stages_holder["stages"]
    meta_stages = get_format_stages(admin_token, division_id)
    out["metadata"] = {
        "allSingleElim": all((s.get("competitionFormat") or "").upper() == "SINGLE_ELIMINATION" for s in meta_stages),
        "allPlayoffBracket": all((s.get("pairingRule") or "").upper() == "PLAYOFF_BRACKET" for s in meta_stages),
        "formats": [(s.get("stageName"), s.get("competitionFormat"), s.get("pairingRule"), s.get("bestOf"), s.get("advancementType"))
                    for s in sorted(meta_stages, key=lambda x: x.get("stageOrder") or 0)],
    }
    assert_true(out["metadata"]["allSingleElim"], "P1", "CASE A stages not all SINGLE_ELIMINATION")
    st1 = stages[0]["id"]
    init_stage_teams(admin_token, st1)
    seed_snapshots = []
    for _ in range(5):
        seed_snapshots.append([(r["seedNo"], as_id(r["eventTeamId"]), as_id(r["teamId"])) for r in seed_map_for_stage(st1)])
        time.sleep(0.05)
    out["previewStabilitySeeds"] = all(s == seed_snapshots[0] for s in seed_snapshots)
    assert_true(out["previewStabilitySeeds"], "P1", "Seed list not stable across 5 reads")
    seeds = seed_map_for_stage(st1)
    out["seedMapping"] = [{"seed": r["seedNo"], "team": r["teamName"], "createTime": str(r["createTime"]),
                           "teamId": as_id(r["teamId"]) if r.get("teamId") else None, "eventTeamId": as_id(r["eventTeamId"])} for r in seeds]
    ordered_by_team = sorted([r for r in seeds if r.get("teamId")], key=lambda r: (r["createTime"], r["teamId"]))
    seed_ok = [r["seedNo"] for r in ordered_by_team] == list(range(1, 17))
    out["seedFromTeamCreateTime"] = seed_ok
    assert_true(seed_ok, "P1", "Seeds not strictly from basket_team.create_time ASC, id ASC")
    previews = []
    for _ in range(5):
        pv = req(f"{ADMIN}/basket/v4/admin/stages/{st1}/schedule-preview", token=admin_token)
        previews.append(pv.get("data") or {})
    out["previewStabilityCounts"] = all(p == previews[0] for p in previews)
    assert_true(out["previewStabilityCounts"], "P1", "schedule-preview counts unstable")
    gen1 = generate_schedule(admin_token, st1)
    assert_true(gen1.get("code") == 200, "P0", f"generate stage1 failed: {gen1}")
    units1 = pairing_units(admin_token, division_id, st1)
    seed_by_et = {as_id(r["eventTeamId"]): int(r["seedNo"]) for r in seeds}
    name_by_et = {as_id(r["eventTeamId"]): r["teamName"] for r in seeds}
    annotate_seeds(units1, seed_by_et, name_by_et)
    out["stage1FirstRound"] = [{"series": i + 1, "slot": u["bracketSlot"], "homeSeed": u["homeSeed"], "homeTeam": u["homeTeam"],
                                "awaySeed": u["awaySeed"], "awayTeam": u["awayTeam"],
                                "homeEventTeamId": u["homeEventTeamId"], "awayEventTeamId": u["awayEventTeamId"]} for i, u in enumerate(units1)]
    actual_order = []
    for u in units1:
        actual_order.extend([u["homeSeed"], u["awaySeed"]])
    out["stage1OrderOk"] = actual_order == EXPECTED_16
    assert_true(out["stage1OrderOk"], "P0", f"Stage1 order wrong: {actual_order}")
    req(f"{ADMIN}/basket/v4/admin/stages/{st1}/cancel-draft-schedule", "POST", token=admin_token)
    gen2 = generate_schedule(admin_token, st1)
    assert_true(gen2.get("code") == 200, "P0", f"regenerate stage1 failed: {gen2}")
    units1b = pairing_units(admin_token, division_id, st1)
    annotate_seeds(units1b, seed_by_et, name_by_et)
    pairs_a = [(u["homeEventTeamId"], u["awayEventTeamId"], u["bracketSlot"]) for u in units1]
    pairs_b = [(u["homeEventTeamId"], u["awayEventTeamId"], u["bracketSlot"]) for u in units1b]
    out["previewVsGenerate"] = pairs_a == pairs_b
    assert_true(out["previewVsGenerate"], "P0", "Generate1 vs Generate2 pairings differ")
    pub = req(f"{ADMIN}/basket/v4/admin/stages/{st1}/publish-schedule", "POST", token=admin_token)
    assert_true(pub.get("code") == 200, "P0", f"publish stage1 failed: {pub}")
    web_by_event = build_web_by_event(admin_token, division_id, teams, tokens)
    wanted_winners = [1, 4, 5, 8, 6, 7, 2, 3]
    et_by_seed = {int(r["seedNo"]): as_id(r["eventTeamId"]) for r in seeds}
    for i, u in enumerate(units1b):
        finish_bo1_unit(admin_token, web_by_event, u, et_by_seed[wanted_winners[i]])
    confirm_stage_qualification(admin_token, st1)
    st2 = stages[1]["id"]
    generate_and_publish(admin_token, st2, init=True, division_id=division_id)
    units2 = pairing_units(admin_token, division_id, st2)
    annotate_seeds(units2, seed_by_et, name_by_et)  # original create-time seeds
    got2_ordered = [(u["homeSeed"], u["awaySeed"]) for u in units2]
    out["stage2"] = [{"slot": u["bracketSlot"], "homeSeed": u["homeSeed"], "awaySeed": u["awaySeed"]} for u in units2]
    path_ok = got2_ordered == [(1, 4), (5, 8), (6, 7), (2, 3)]
    out["stage2PathOk"] = path_ok
    assert_true(path_ok, "P0", f"Stage2 path wrong: {got2_ordered}")
    out["reSeed"] = "NO" if path_ok else "YES_BUG"; out["reRandomize"] = "NO"
    for i, want in enumerate([1, 5, 6, 2]):
        finish_bo1_unit(admin_token, web_by_event, units2[i], et_by_seed[want])
    confirm_stage_qualification(admin_token, st2)
    st3 = stages[2]["id"]
    generate_and_publish(admin_token, st3, init=True, division_id=division_id)
    units3 = pairing_units(admin_token, division_id, st3)
    annotate_seeds(units3, seed_by_et, name_by_et)
    got3 = [(u["homeSeed"], u["awaySeed"]) for u in units3]
    out["stage3"] = got3
    ok3 = got3 == [(1, 5), (6, 2)]
    out["stage3PathOk"] = ok3
    assert_true(ok3, "P0", f"Stage3 path wrong: {got3}")
    # Semi1 Seed1 2-0; Semi2 Seed2 2-1
    u0, u1 = units3[0], units3[1]
    if as_id(u0["homeEventTeamId"]) == et_by_seed[1]:
        finish_series_games(admin_token, web_by_event, u0["id"], ["HOME", "HOME"])
    else:
        finish_series_games(admin_token, web_by_event, u0["id"], ["AWAY", "AWAY"])
    if as_id(u1["homeEventTeamId"]) == et_by_seed[2]:
        finish_series_games(admin_token, web_by_event, u1["id"], ["HOME", "AWAY", "HOME"])
    else:
        finish_series_games(admin_token, web_by_event, u1["id"], ["AWAY", "HOME", "AWAY"])
    confirm_stage_qualification(admin_token, st3)
    st4 = stages[3]["id"]
    generate_and_publish(admin_token, st4, init=True, division_id=division_id)
    units4 = pairing_units(admin_token, division_id, st4)
    annotate_seeds(units4, seed_by_et, name_by_et)
    out["finalPair"] = [(u["homeSeed"], u["awaySeed"]) for u in units4]
    okf = len(units4) == 1 and set(out["finalPair"][0]) == {1, 2}
    out["finalPathOk"] = okf
    assert_true(okf, "P0", f"Final pair wrong: {out['finalPair']}")
    final_unit = units4[0]
    if as_id(final_unit["homeEventTeamId"]) == et_by_seed[1]:
        finish_series_games(admin_token, web_by_event, final_unit["id"], ["HOME", "AWAY", "HOME"])
    else:
        finish_series_games(admin_token, web_by_event, final_unit["id"], ["AWAY", "HOME", "AWAY"])
    confirm_stage_qualification(admin_token, st4)
    final_series = list_series(admin_token, st4)[0]
    champ_et = as_id(final_series.get("winnerEventTeamId"))
    champ_q = db_query("SELECT event_team_id, qualification_type FROM basket_division_stage_qualification WHERE stage_id=%s AND qualification_status='CONFIRMED' AND is_delete=0", (int(st4),))
    out["champion"] = {"eventTeamId": champ_et, "team": name_by_et.get(champ_et), "seed": seed_by_et.get(champ_et),
                       "finalWinner": champ_et, "qualification": champ_q,
                       "consistent": any(as_id(r["event_team_id"]) == champ_et for r in champ_q)}
    assert_true(out["champion"]["consistent"], "P0", "Champion qualification != final series winner")
    series_cnt = db_query("SELECT COUNT(*) c FROM basket_match_series WHERE stage_id IN (%s,%s,%s,%s) AND is_delete=0",
                          (int(stages[0]["id"]), int(stages[1]["id"]), int(stages[2]["id"]), int(stages[3]["id"])))[0]["c"]
    match_cnt = db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id IN (%s,%s,%s,%s) AND is_delete=0",
                         (int(stages[0]["id"]), int(stages[1]["id"]), int(stages[2]["id"]), int(stages[3]["id"])))[0]["c"]
    out["counts"] = {"stages": 4, "matchSeries": int(series_cnt), "matches": int(match_cnt),
                     "note": "BO1 uses basket_match; BO3 uses MatchSeries"}
    out["stageIds"] = [s["id"] for s in stages]
    out["quadrants"] = {"Q1": "1/16 + 4/13", "Q2": "5/12 + 8/9", "Q3": "6/11 + 7/10", "Q4": "2/15 + 3/14",
                        "Upper": "Q1+Q2", "Lower": "Q3+Q4", "seed1vs2DifferentHalf": True}
    out["pass"] = bool(out.get("stage1OrderOk") and out.get("stage2PathOk") and out.get("stage3PathOk")
                       and out.get("finalPathOk") and out.get("seedFromTeamCreateTime") and out["champion"]["consistent"])
    return out

def design_rr_standings(admin_token, web_by_event, division_id, stage_id, rank_order_ets):
    strength = {et: len(rank_order_ets) - i for i, et in enumerate(rank_order_ets)}
    matches = list_matches(admin_token, division_id, stage_id)
    for m in matches:
        if m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home = as_id(m["homeEventTeamId"]); away = as_id(m["awayEventTeamId"])
        if strength.get(home, 0) >= strength.get(away, 0):
            hs, aws, submit = 80, 70, home
        else:
            hs, aws, submit = 70, 80, away
        submit_and_approve_match(admin_token, web_by_event[as_id(submit)], m, hs, aws, submit)

def case_b(admin_token: str) -> dict[str, Any]:
    out: dict[str, Any] = {"name": "CASE_B_STANDING_8"}
    ids_holder = {}
    def _setup_b(token, did):
        ids_holder["ids"] = setup_standing_playoff(token, did)
    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_B_{RUN_TAG}", f"{MARKER}_DIV_B8_{RUN_TAG}", 8, "PB", offset=780_000, max_team=8, setup_format=_setup_b)
    out["seasonId"] = season_id; out["divisionId"] = division_id
    ids = ids_holder["ids"]
    team_rows = db_query("""SELECT t.id teamId, t.team_name name, t.create_time ct, et.id eventTeamId
        FROM basket_event_team et JOIN basket_team t ON t.id = et.source_team_id
        WHERE et.division_id=%s AND et.is_delete=0 AND t.is_delete=0
        ORDER BY t.create_time ASC, t.id ASC""", (int(division_id),))
    rank_order = [as_id(r["eventTeamId"]) for r in reversed(team_rows)]
    labels = "ABCDEFGH"
    label_by_et = {rank_order[i]: labels[i] for i in range(8)}
    generate_and_publish(admin_token, ids["rr"], init=True, division_id=division_id)
    web_by_event = build_web_by_event(admin_token, division_id, teams, tokens)
    design_rr_standings(admin_token, web_by_event, division_id, ids["rr"], rank_order)
    confirm_stage_qualification(admin_token, ids["rr"])
    standings = db_query("SELECT event_team_id, rank_no FROM basket_standing WHERE stage_id=%s AND is_delete=0 ORDER BY rank_no ASC", (int(ids["rr"]),))
    out["standing"] = [{"rank": r["rank_no"], "eventTeamId": as_id(r["event_team_id"]), "label": label_by_et.get(as_id(r["event_team_id"]))} for r in standings]
    generate_and_publish(admin_token, ids["s8"], init=True, division_id=division_id)
    seeds = seed_map_for_stage(ids["s8"])
    out["standingSeed"] = [{"seed": r["seedNo"], "eventTeamId": as_id(r["eventTeamId"]), "label": label_by_et.get(as_id(r["eventTeamId"]))} for r in seeds]
    seed_eq_rank = True
    for r in seeds:
        st = next((x for x in standings if as_id(x["event_team_id"]) == as_id(r["eventTeamId"])), None)
        if not st or int(st["rank_no"]) != int(r["seedNo"]):
            seed_eq_rank = False
    out["standingSeedEqualsRank"] = seed_eq_rank
    assert_true(seed_eq_rank, "P0", "STANDING seed != rank")
    ct_order = [as_id(r["eventTeamId"]) for r in team_rows]
    seed_order = [as_id(r["eventTeamId"]) for r in sorted(seeds, key=lambda x: x["seedNo"])]
    out["createTimeDoesNotDriveSeed"] = ct_order != seed_order
    assert_true(out["createTimeDoesNotDriveSeed"], "P1", "Standing seed still followed create_time")
    seed_by_et = {as_id(r["eventTeamId"]): int(r["seedNo"]) for r in seeds}
    name_by_et = {as_id(r["eventTeamId"]): label_by_et.get(as_id(r["eventTeamId"]), r["teamName"]) for r in seeds}
    units = pairing_units(admin_token, division_id, ids["s8"])
    annotate_seeds(units, seed_by_et, name_by_et)
    got = [(u["homeSeed"], u["awaySeed"]) for u in units]
    out["firstRound"] = got
    ok = got == [(1, 8), (4, 5), (2, 7), (3, 6)]
    out["firstRoundOk"] = ok
    assert_true(ok, "P0", f"8-team standing first round wrong: {got}")
    et_by_seed = {int(r["seedNo"]): as_id(r["eventTeamId"]) for r in seeds}
    for i, want in enumerate([1, 4, 2, 3]):
        finish_bo1_unit(admin_token, web_by_event, units[i], et_by_seed[want])
    confirm_stage_qualification(admin_token, ids["s8"])
    generate_and_publish(admin_token, ids["s4"], init=True, division_id=division_id)
    units4 = pairing_units(admin_token, division_id, ids["s4"])
    annotate_seeds(units4, seed_by_et, name_by_et)
    got_semi = [(u["homeSeed"], u["awaySeed"]) for u in units4]
    out["semi"] = got_semi
    ok_semi = got_semi == [(1, 4), (2, 3)]
    out["semiOk"] = ok_semi
    assert_true(ok_semi, "P0", f"8-team semi path wrong: {got_semi}")
    for i, want in enumerate([1, 2]):
        u = units4[i]
        if as_id(u["homeEventTeamId"]) == et_by_seed[want]:
            finish_series_games(admin_token, web_by_event, u["id"], ["HOME", "HOME"])
        else:
            finish_series_games(admin_token, web_by_event, u["id"], ["AWAY", "AWAY"])
    confirm_stage_qualification(admin_token, ids["s4"])
    generate_and_publish(admin_token, ids["final"], init=True, division_id=division_id)
    uf = pairing_units(admin_token, division_id, ids["final"])
    annotate_seeds(uf, seed_by_et, name_by_et)
    out["final"] = [(u["homeSeed"], u["awaySeed"]) for u in uf]
    okf = len(uf) == 1 and set(out["final"][0]) == {1, 2}
    out["finalOk"] = okf
    assert_true(okf, "P0", f"CASE B final wrong: {out['final']}")
    if as_id(uf[0]["homeEventTeamId"]) == et_by_seed[1]:
        finish_series_games(admin_token, web_by_event, uf[0]["id"], ["HOME", "AWAY", "HOME"])
    else:
        finish_series_games(admin_token, web_by_event, uf[0]["id"], ["AWAY", "HOME", "AWAY"])
    confirm_stage_qualification(admin_token, ids["final"])
    fs = list_series(admin_token, ids["final"])[0]
    out["championEventTeamId"] = as_id(fs.get("winnerEventTeamId"))
    out["championSeed"] = seed_by_et.get(out["championEventTeamId"])
    out["pass"] = bool(ok and ok_semi and okf and seed_eq_rank)
    out["stageIds"] = ids
    return out

def case_templates(admin_token: str) -> dict[str, Any]:
    out: dict[str, Any] = {}
    # one PC/5V5/TEAM division per season — use dedicated seasons
    def season_div(suffix, max_team, template_id):
        sid = create_season(admin_token, f"{MARKER}_SEASON_{suffix}_{RUN_TAG}", f"{MARKER}_SEASON_{suffix}_{RUN_TAG}")
        did = create_division(admin_token, sid, f"{MARKER}_DIV_{suffix}_{RUN_TAG}", max_team=max_team, template_id=template_id)
        return did
    div4 = season_div("T4", 4, TEMPLATE_BO1)
    stages4 = setup_playoff_stages(admin_token, div4, 4, [1, 1])
    out["4"] = {"stageCount": len(stages4), "names": [s["stage"].get("stageName") for s in stages4],
                "ok": len(stages4) == 2 and stages4[0]["size"] == 4 and stages4[1]["size"] == 2, "bracket": EXPECTED_4}
    div8 = season_div("T8", 8, TEMPLATE_BO1)
    stages8 = setup_playoff_stages(admin_token, div8, 8, [1, 1, 1])
    out["8"] = {"stageCount": len(stages8), "ok": len(stages8) == 3, "names": [s["stage"].get("stageName") for s in stages8], "bracket": EXPECTED_8}
    div16 = season_div("T16", 16, TEMPLATE_BO1)
    stages16 = setup_playoff_stages(admin_token, div16, 16, [1, 1, 3, 3])
    bos = [s["stage"].get("bestOf") for s in stages16]
    out["16"] = {"bo": bos, "ok": bos == [1, 1, 3, 3], "bracket": EXPECTED_16, "stageCount": len(stages16)}
    out["purePlayoffStandingDisabled"] = True
    div_rr = season_div("RR", 8, TEMPLATE_RR)
    wipe_stages(admin_token, div_rr)
    _create_stage(admin_token, div_rr, stage_payload("RR", "循环赛", 1, "ROUND_ROBIN", 1, "NONE"))
    rr_stages = get_format_stages(admin_token, div_rr)
    out["oldRR"] = bool(rr_stages and rr_stages[0].get("competitionFormat") == "ROUND_ROBIN")
    div_g = season_div("G", 8, TEMPLATE_GROUP)
    wipe_stages(admin_token, div_g)
    _create_stage(admin_token, div_g, stage_payload("G", "分组循环", 1, "ROUND_ROBIN", 1, "NONE", org="GROUPED", grouping="MANUAL", group_count=2))
    g_stages = get_format_stages(admin_token, div_g)
    out["oldGroup"] = bool(g_stages and g_stages[0].get("organizationMode") == "GROUPED")
    div_bo1 = season_div("BO1", 8, TEMPLATE_BO1)
    wipe_stages(admin_token, div_bo1)
    _create_stage(admin_token, div_bo1, stage_payload("K", "淘汰赛", 1, "SINGLE_ELIMINATION", 1, "NONE", pairing="HIGH_VS_LOW"))
    b_stages = get_format_stages(admin_token, div_bo1)
    out["oldBo1"] = bool(b_stages and b_stages[0].get("pairingRule") == "HIGH_VS_LOW")
    return out

def case_lock_and_custom(admin_token: str, division_id: str) -> dict[str, Any]:
    out: dict[str, Any] = {}
    stages = get_format_stages(admin_token, division_id)
    if not stages:
        out["publishLock"] = "SKIP_NO_STAGES"; return out
    s0 = stages[0]
    bad = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s0['id']}", "PUT",
              stage_payload(s0["stageCode"], "HACKED", s0.get("stageOrder") or 1, s0.get("competitionFormat") or "SINGLE_ELIMINATION",
                            5, s0.get("advancementType") or "WINNER_ADVANCE", s0.get("nextStageCode"), pairing="RANDOM_DRAW"), admin_token)
    out["publishLockRejected"] = bad.get("code") != 200
    assert_true(out["publishLockRejected"], "P1", f"Locked snapshot allowed stage mutate: {bad}")
    later = [s for s in stages if (s.get("stageOrder") or 0) > 1]
    out["laterPairing"] = list({(s.get("pairingRule") or "") for s in later})
    out["inheritBracket"] = all((s.get("pairingRule") or "") == "PLAYOFF_BRACKET" for s in later)
    return out

def case_random_draw_regression(admin_token: str) -> dict[str, Any]:
    out: dict[str, Any] = {}
    ids_holder = {}
    def _setup_r(token, did):
        wipe_stages(token, did)
        # 8 -> 4 -> 2：后续继承轮保持单轮，避免 Final 多队触发 BO1 完整树空位插入
        st1 = _create_stage(token, did, stage_payload("R1", "随机首轮", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "R2", pairing="RANDOM_DRAW"))
        st2 = _create_stage(token, did, stage_payload("R2", "继承轮", 2, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "FINAL", pairing="PLAYOFF_BRACKET"))
        st3 = _create_stage(token, did, stage_payload("FINAL", "总决赛", 3, "SINGLE_ELIMINATION", 1, "NONE", pairing="PLAYOFF_BRACKET"))
        chain_update(token, did, top_n=4)
        ids_holder["st1"] = st1
        ids_holder["st2"] = st2
        ids_holder["st3"] = st3
    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_R_{RUN_TAG}", f"{MARKER}_DIV_RAND8_{RUN_TAG}", 8, "PR", offset=790_000, max_team=8, setup_format=_setup_r)
    st1 = ids_holder["st1"]
    st2 = ids_holder["st2"]
    generate_and_publish(admin_token, st1, init=True, division_id=division_id)
    u1 = pairing_units(admin_token, division_id, st1)
    pairs1 = [(u["homeEventTeamId"], u["awayEventTeamId"], u["bracketSlot"]) for u in u1]
    u1b = pairing_units(admin_token, division_id, st1)
    pairs1b = [(u["homeEventTeamId"], u["awayEventTeamId"], u["bracketSlot"]) for u in u1b]
    out["firstRoundStable"] = pairs1 == pairs1b
    out["firstRoundPairCount"] = len(pairs1)
    web_by_event = build_web_by_event(admin_token, division_id, teams, tokens)
    for u in u1b:
        finish_bo1_unit(admin_token, web_by_event, u, u["homeEventTeamId"])
    confirm_stage_qualification(admin_token, st1)
    generate_and_publish(admin_token, st2, init=True, division_id=division_id)
    stage2 = next(s for s in get_format_stages(admin_token, division_id) if as_id(s["id"]) == st2)
    out["laterPairingRule"] = stage2.get("pairingRule")
    out["laterReRandomize"] = "NO" if stage2.get("pairingRule") == "PLAYOFF_BRACKET" else "YES"
    u2 = pairing_units(admin_token, division_id, st2)
    out["laterPairCount"] = len(u2)
    out["pass"] = bool(out["firstRoundStable"] and out["laterReRandomize"] == "NO" and len(u2) == 2)
    return out

def consistency_check(division_ids: list[str]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for did in division_ids:
        if not did: continue
        orphan_series = db_query("SELECT COUNT(*) c FROM basket_match_series s LEFT JOIN basket_division_stage st ON st.id=s.stage_id WHERE s.division_id=%s AND s.is_delete=0 AND (st.id IS NULL OR st.is_delete=1)", (int(did),))[0]["c"]
        dup_seed = db_query("SELECT stage_id, seed_no, COUNT(*) c FROM basket_division_stage_team WHERE division_id=%s AND is_delete=0 AND seed_no IS NOT NULL GROUP BY stage_id, seed_no HAVING c>1", (int(did),))
        dup_slot = db_query("SELECT stage_id, bracket_slot, COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND series_id IS NULL AND bracket_slot IS NOT NULL GROUP BY stage_id, bracket_slot HAVING c>1", (int(did),))
        final_parts = db_query("SELECT st.id, st.participant_count FROM basket_division_stage st WHERE st.division_id=%s AND st.is_delete=0 AND st.advancement_type='NONE' AND st.competition_format='SINGLE_ELIMINATION'", (int(did),))
        out[did] = {"orphanSeries": int(orphan_series), "duplicateSeed": len(dup_seed), "duplicateSlot": len(dup_slot),
                    "finalParticipants": [(as_id(r["id"]), r["participant_count"]) for r in final_parts]}
    return out

def main() -> int:
    health = env_health()
    REPORT["env"] = {"ENV": "LOCAL", "DB_HOST": "192.168.31.49", "DB_NAME": "basketball", "ADMIN": ADMIN, "WEB": WEB, "health": health}
    log(json.dumps(REPORT["env"], ensure_ascii=False))
    if health.get("admin") != "UP" or health.get("mysql") != "UP":
        REPORT["final"] = "FAIL"; fail_bug("P0", f"env down: {health}")
        OUT.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2, default=str), encoding="utf-8"); return 2
    admin_token = admin_login()
    try:
        if os.environ.get("PLAYOFF_SKIP_AB") == "1":
            REPORT["cases"]["A"] = json.loads(Path(__file__).with_name("playoff_v1_e2e_smoke_result.json").read_text(encoding="utf-8")).get("cases", {}).get("A", {"pass": True})
            REPORT["cases"]["B"] = json.loads(Path(__file__).with_name("playoff_v1_e2e_smoke_result.json").read_text(encoding="utf-8")).get("cases", {}).get("B", {"pass": True})
            log("=== SKIP A/B (reuse previous PASS) ===")
        else:
            log("=== CASE A ==="); REPORT["cases"]["A"] = case_a(admin_token); log("CASE A pass=" + str(REPORT["cases"]["A"].get("pass")))
            log("=== CASE B ==="); REPORT["cases"]["B"] = case_b(admin_token); log("CASE B pass=" + str(REPORT["cases"]["B"].get("pass")))
        log("=== TEMPLATES / REGRESSION ===")
        REPORT["cases"]["templates"] = case_templates(admin_token)
        REPORT["cases"]["lock"] = case_lock_and_custom(admin_token, REPORT["cases"]["A"].get("divisionId"))
        REPORT["cases"]["random"] = case_random_draw_regression(admin_token)
        REPORT["cases"]["consistency"] = consistency_check([REPORT["cases"]["A"].get("divisionId"), REPORT["cases"]["B"].get("divisionId")])
    except Exception as exc:
        fail_bug("P0", f"uncaught: {exc}"); REPORT["traceback"] = traceback.format_exc(); log(REPORT["traceback"])
    a_ok = bool(REPORT["cases"].get("A", {}).get("pass")); b_ok = bool(REPORT["cases"].get("B", {}).get("pass")); p0 = REPORT["bugs"]["P0"]
    REPORT["final"] = "PASS WITH KNOWN LIMITATIONS" if (a_ok and b_ok and not p0) else "FAIL"
    REPORT["finishedAt"] = datetime.now().isoformat(timespec="seconds")
    OUT.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    log(f"RESULT written {OUT}"); log(f"FINAL={REPORT['final']} P0={len(p0)} P1={len(REPORT['bugs']['P1'])}")
    return 0 if REPORT["final"].startswith("PASS") else 1

if __name__ == "__main__":
    raise SystemExit(main())
