from pathlib import Path
p = Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts/playoff_v1_e2e_smoke.py")
text = p.read_text(encoding="utf-8-sig")

old = '''def prepare_division_with_teams(admin_token, season_name, division_name, n, pool_code, offset, max_team=None):
    season_id = create_season(admin_token, season_name, season_name)
    division_id = create_division(admin_token, season_id, division_name, max_team=max_team or n, template_id=TEMPLATE_BO1)
    defs = team_defs(pool_code, n)
    teams = seed_org_pool(pool_code, defs, offset=offset)
    emails = [e for t in teams for e in t["emails"]]
    clear_login_locks(emails)
    create_teams_flow(admin_token, division_id, teams)
    populate_members(teams, invite_until=n, full_players=5)
    tokens = web_tokens_for_teams(teams)
    open_registration(admin_token, division_id)
    register_all_teams(tokens, admin_token, division_id, teams)
    close_registration(admin_token, division_id)
    return season_id, division_id, teams, tokens'''

new = '''def prepare_division_with_teams(admin_token, season_name, division_name, n, pool_code, offset, max_team=None, setup_format=None):
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
    return season_id, division_id, teams, tokens'''

if old not in text:
    raise SystemExit("prepare block not found")
text = text.replace(old, new)

old_a = '''    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_A", f"{MARKER}_DIV_A16", 16, "PA", offset=770_000, max_team=16)
    out["seasonId"] = season_id; out["divisionId"] = division_id
    stages = setup_playoff_stages(admin_token, division_id, 16, [1, 1, 3, 3])
    meta_stages = get_format_stages(admin_token, division_id)
    out["metadata"] = {
        "allSingleElim": all((s.get("competitionFormat") or "").upper() == "SINGLE_ELIMINATION" for s in meta_stages),
        "allPlayoffBracket": all((s.get("pairingRule") or "").upper() == "PLAYOFF_BRACKET" for s in meta_stages),
        "formats": [(s.get("stageName"), s.get("competitionFormat"), s.get("pairingRule"), s.get("bestOf"), s.get("advancementType"))
                    for s in sorted(meta_stages, key=lambda x: x.get("stageOrder") or 0)],
    }
    assert_true(out["metadata"]["allSingleElim"], "P1", "CASE A stages not all SINGLE_ELIMINATION")
    lock_snapshot(admin_token, division_id)
    st1 = stages[0]["id"]'''

new_a = '''    stages_holder = {}
    def _setup(token, did):
        stages_holder["stages"] = setup_playoff_stages(token, did, 16, [1, 1, 3, 3])
    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_A", f"{MARKER}_DIV_A16", 16, "PA", offset=770_000, max_team=16, setup_format=_setup)
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
    st1 = stages[0]["id"]'''

if old_a not in text:
    raise SystemExit("case_a block not found")
text = text.replace(old_a, new_a)

old_b = '''    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_B", f"{MARKER}_DIV_B8", 8, "PB", offset=780_000, max_team=8)
    out["seasonId"] = season_id; out["divisionId"] = division_id
    ids = setup_standing_playoff(admin_token, division_id)
    lock_snapshot(admin_token, division_id)'''

new_b = '''    ids_holder = {}
    def _setup_b(token, did):
        ids_holder["ids"] = setup_standing_playoff(token, did)
    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_B", f"{MARKER}_DIV_B8", 8, "PB", offset=780_000, max_team=8, setup_format=_setup_b)
    out["seasonId"] = season_id; out["divisionId"] = division_id
    ids = ids_holder["ids"]'''

if old_b not in text:
    raise SystemExit("case_b block not found")
text = text.replace(old_b, new_b)

old_r = '''    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_R", f"{MARKER}_DIV_RAND8", 8, "PR", offset=790_000, max_team=8)
    wipe_stages(admin_token, division_id)
    st1 = _create_stage(admin_token, division_id, stage_payload("R1", "随机首轮", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "R2", pairing="RANDOM_DRAW"))
    st2 = _create_stage(admin_token, division_id, stage_payload("R2", "继承轮", 2, "SINGLE_ELIMINATION", 1, "NONE", pairing="PLAYOFF_BRACKET"))
    chain_update(admin_token, division_id); lock_snapshot(admin_token, division_id)'''

new_r = '''    ids_holder = {}
    def _setup_r(token, did):
        wipe_stages(token, did)
        st1 = _create_stage(token, did, stage_payload("R1", "随机首轮", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "R2", pairing="RANDOM_DRAW"))
        st2 = _create_stage(token, did, stage_payload("R2", "继承轮", 2, "SINGLE_ELIMINATION", 1, "NONE", pairing="PLAYOFF_BRACKET"))
        chain_update(token, did)
        ids_holder["st1"] = st1
        ids_holder["st2"] = st2
    season_id, division_id, teams, tokens = prepare_division_with_teams(
        admin_token, f"{MARKER}_SEASON_R", f"{MARKER}_DIV_RAND8", 8, "PR", offset=790_000, max_team=8, setup_format=_setup_r)
    st1 = ids_holder["st1"]
    st2 = ids_holder["st2"]'''

if old_r not in text:
    raise SystemExit("random block not found")
text = text.replace(old_r, new_r)

p.write_text(text, encoding="utf-8")
import py_compile
py_compile.compile(str(p), doraise=True)
print("patched ok")
