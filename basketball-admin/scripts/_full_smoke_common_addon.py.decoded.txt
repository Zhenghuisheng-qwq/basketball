FORBIDDEN_DIVISION_IDS = frozenset({"118", "141"})
SPARE_TEAMS = [
    {"code": "SPR", "name": "FS_SPARE", "short": "SPR", "players": ["SPR_C", "SPR_1", "SPR_2", "SPR_3", "SPR_4"]},
]


def refuse_forbidden_division(division_id: str) -> None:
    if as_id(division_id) in FORBIDDEN_DIVISION_IDS:
        raise RuntimeError(f"Refusing hardcoded legacy division id {division_id}")


def lock_snapshot(token: str, division_id: str) -> dict:
    r = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot/lock", "POST", token=token)
    if r.get("code") != 200:
        raise RuntimeError(f"lock snapshot failed: {r}")
    return r


def get_format_stages(token: str, division_id: str) -> list[dict]:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    return sorted_stages(snap.get("data") or {})


def put_tie_break(token: str, division_id: str, stage_id: str) -> dict:
    return req(
        f"{ADMIN}/basket/v4/divisions/{division_id}/tie-break-rules",
        "PUT",
        {"stageId": int(stage_id), "rules": TIE_BREAK},
        token,
    )


def init_stage_teams(token: str, stage_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/initialize-teams", "POST", token=token)


def generate_schedule(token: str, stage_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/generate-schedule", "POST", token=token)


def publish_schedule(token: str, stage_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/publish-schedule", "POST", token=token)


def generate_and_publish(token: str, stage_id: str, init: bool = True) -> dict:
    if init:
        init_stage_teams(token, stage_id)
    gen = generate_schedule(token, stage_id)
    if gen.get("code") != 200:
        raise RuntimeError(f"generate-schedule failed stage {stage_id}: {gen}")
    pub = publish_schedule(token, stage_id)
    if pub.get("code") != 200:
        raise RuntimeError(f"publish-schedule failed stage {stage_id}: {pub}")
    return {"generate": gen, "publish": pub}


def _create_stage(token: str, division_id: str, payload: dict) -> str:
    r = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", payload, token)
    if r.get("code") != 200:
        raise RuntimeError(f"create stage failed: {r}")
    data = r.get("data")
    return as_id(data if isinstance(data, (int, str)) else extract_id(r))


def setup_main_format(token: str, division_id: str) -> dict[str, str]:
    wipe_stages(token, division_id)
    st1 = _create_stage(token, division_id, stage_payload("ST1", "RR Top4", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "ST2", 4))
    st2 = _create_stage(
        token,
        division_id,
        stage_payload("ST2", "Semi BO3", 2, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "ST3", pairing="HIGH_VS_LOW"),
    )
    st3 = _create_stage(
        token,
        division_id,
        stage_payload("ST3", "Final BO5", 3, "SINGLE_ELIMINATION", 5, "NONE", pairing="HIGH_VS_LOW"),
    )
    chain_update(token, division_id)
    put_tie_break(token, division_id, st1)
    lock_snapshot(token, division_id)
    return {"st1": st1, "st2": st2, "st3": st3}


def setup_group_format(token: str, division_id: str, bag: SmokeBag | None = None) -> dict[str, str]:
    wipe_stages(token, division_id)
    adv = "GROUP_TOP_N"
    p1 = stage_payload("G1", "Group RR", 1, "ROUND_ROBIN", 1, adv, "G2", 2, org="GROUPED", grouping="MANUAL", group_count=2)
    r1 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", p1, token)
    if r1.get("code") != 200:
        adv = "OVERALL_TOP_N"
        p1["advancementType"] = adv
        r1 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", p1, token)
        if bag is not None:
            bag.fixes.append({"topic": "advancementType", "used": adv, "reason": "GROUP_TOP_N rejected on create"})
    if r1.get("code") != 200:
        raise RuntimeError(f"create group stage failed: {r1}")
    st1 = as_id(r1.get("data") if isinstance(r1.get("data"), (int, str)) else extract_id(r1))
    st2 = _create_stage(
        token,
        division_id,
        stage_payload("G2", "Playoff BO3", 2, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "G3", pairing="HIGH_VS_LOW"),
    )
    st3 = _create_stage(
        token,
        division_id,
        stage_payload("G3", "Final BO5", 3, "SINGLE_ELIMINATION", 5, "NONE", pairing="HIGH_VS_LOW"),
    )
    chain_update(token, division_id, top_n=2)
    put_tie_break(token, division_id, st1)
    lr = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot/lock", "POST", token=token)
    if lr.get("code") != 200 and adv == "GROUP_TOP_N":
        adv = "OVERALL_TOP_N"
        stages = get_format_stages(token, division_id)
        if stages:
            s0 = stages[0]
            p = stage_payload(
                s0["stageCode"],
                s0.get("stageName"),
                s0.get("stageOrder") or 1,
                s0.get("competitionFormat") or "ROUND_ROBIN",
                s0.get("bestOf") or 1,
                adv,
                s0.get("nextStageCode"),
                2,
                org="GROUPED",
                grouping="MANUAL",
                group_count=2,
            )
            req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s0['id']}", "PUT", p, token)
            if bag is not None:
                bag.fixes.append({"topic": "advancementType", "used": adv, "reason": "lock rejected GROUP_TOP_N"})
        lock_snapshot(token, division_id)
    elif lr.get("code") != 200:
        raise RuntimeError(f"lock snapshot failed: {lr}")
    return {"st1": st1, "st2": st2, "st3": st3, "advancementType": adv}


def setup_advanced_format(token: str, division_id: str) -> dict[str, str]:
    wipe_stages(token, division_id)
    st1 = _create_stage(
        token,
        division_id,
        stage_payload("A1", "SE BO1 BYE", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "A2", pairing="HIGH_VS_LOW"),
    )
    st2 = _create_stage(
        token,
        division_id,
        stage_payload("A2", "SE BO3 Random", 2, "SINGLE_ELIMINATION", 3, "NONE", pairing="RANDOM_DRAW"),
    )
    chain_update(token, division_id, top_n=2)
    lock_snapshot(token, division_id)
    return {"st1": st1, "st2": st2}


def web_tokens_for_teams(teams: list[dict]) -> dict[str, str]:
    emails = [t["captain_email"] for t in teams]
    clear_login_locks(emails)
    return {t["captain_email"]: web_login(t["captain_email"]) for t in teams}


def build_web_by_event(admin_token: str, division_id: str, teams: list[dict], tokens: dict[str, str]) -> dict[str, str]:
    etr = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=50", token=admin_token)
    rows = etr.get("rows") or []
    by_name = {(r.get("teamNameSnapshot") or r.get("teamName") or ""): r for r in rows}
    web_by_event: dict[str, str] = {}
    for t in teams:
        et = by_name.get(t["name"]) or next((v for k, v in by_name.items() if t["name"] in k), None)
        if et:
            web_by_event[as_id(et["id"])] = tokens[t["captain_email"]]
    return web_by_event


def count_event_entities(division_id: str) -> tuple[int, int]:
    et = int(db_query("SELECT COUNT(*) AS c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (division_id,))[0]["c"])
    ep = int(
        db_query(
            "SELECT COUNT(*) AS c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
            "WHERE et.division_id=%s AND ep.is_delete=0",
            (division_id,),
        )[0]["c"]
    )
    return et, ep


def verify_event_player_source_distinct(division_id: str) -> tuple[bool, str]:
    rows = db_query(
        "SELECT ep.id, ep.source_player_id FROM basket_event_player ep "
        "JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.division_id=%s AND ep.is_delete=0",
        (division_id,),
    )
    bad = [r for r in rows if str(r["id"]) == str(r["source_player_id"])]
    return len(bad) == 0, f"bad={len(bad)} total={len(rows)}"


def create_teams_flow(admin_token: str, division_id: str, teams: list[dict], logos: list[str] | None = None) -> list[str]:
    team_ids: list[str] = []
    logos = logos or LOGOS
    for i, t in enumerate(teams):
        gr = create_captain_grant(admin_token, division_id, t["captain_player_id"])
        if gr.get("code") != 200:
            raise RuntimeError(f"grant failed team {t['code']}: {gr}")
        tok = web_login(t["captain_email"])
        cr = create_team_via_web(tok, division_id, t["name"], t["short"], logos[i % len(logos)])
        if cr.get("code") != 200:
            raise RuntimeError(f"create team failed {t['name']}: {cr}")
        tid = as_id((cr.get("data") or {}).get("teamId") or extract_id(cr))
        ar = approve_team(admin_token, tid)
        if ar.get("code") != 200:
            raise RuntimeError(f"approve team failed {tid}: {ar}")
        team_ids.append(tid)
        t["team_id"] = tid
    return team_ids


def populate_members(teams: list[dict], invite_until: int = 4, full_players: int = 5) -> None:
    for ti, t in enumerate(teams):
        cap_tok = web_login(t["captain_email"])
        tid = t["team_id"]
        for pi in range(1, full_players):
            p_email = t["emails"][pi]
            p_pid = t["player_ids"][pi]
            p_tok = web_login(p_email)
            if ti < invite_until:
                invite_and_accept(cap_tok, p_tok, tid, p_pid)
            else:
                apply_and_approve(p_tok, cap_tok, tid)


def register_all_teams(web_tokens: dict[str, str], admin_token: str, division_id: str, teams: list[dict]) -> int:
    for t in teams:
        tok = web_tokens[t["captain_email"]]
        rr = register_team(tok, division_id, t["team_id"], t["player_ids"][:5])
        if rr.get("code") != 200:
            raise RuntimeError(f"register failed {t['name']}: {rr}")
    return approve_all_registrations(admin_token, division_id)


def baseline_counts() -> dict[str, int]:
    out: dict[str, int] = {}
    for tb in (
        "basket_division",
        "basket_event_team",
        "basket_event_player",
        "basket_match",
        "basket_match_series",
        "basket_captain_grant",
        "basket_team",
    ):
        try:
            out[tb] = int(db_query(f"SELECT COUNT(*) AS c FROM {tb} WHERE is_delete=0")[0]["c"])
        except Exception:
            out[tb] = -1
    return out


def env_health() -> dict[str, str]:
    health = {
        "admin": "UP" if wait_server(f"{ADMIN}/", 3) else "DOWN",
        "web": "UP" if wait_server(f"{WEB}/", 3) else "DOWN",
    }
    try:
        db_query("SELECT 1 AS ok")
        health["mysql"] = "UP"
    except Exception as exc:
        health["mysql"] = f"DOWN:{exc}"
    try:
        import redis

        r = redis.Redis(host=REDIS_HOST, port=6379, db=0, socket_connect_timeout=3)
        r.ping()
        health["redis"] = "UP"
    except Exception as exc:
        health["redis"] = f"DOWN:{exc}"
    return health


def public_series(series_id: str) -> dict:
    return req(f"{WEB}/basket/v4/public/match-series/{series_id}")


def grouping_batch_create(token: str, division_id: str, stage_id: str) -> str:
    r = req(
        f"{ADMIN}/basket/v4/admin/division-grouping-batches",
        "POST",
        {"divisionId": int(division_id), "stageId": int(stage_id), "resultSource": "MANUAL"},
        token,
    )
    if r.get("code") != 200:
        raise RuntimeError(f"grouping batch create failed: {r}")
    return as_id(r.get("data"))


def grouping_assign_and_lock(token: str, batch_id: str, group_event_map: dict[str, list[str]]) -> None:
    detail = req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}", token=token).get("data") or {}
    groups = detail.get("groups") or []
    gmap: dict[str, str] = {}
    for g in groups:
        code = (g.get("groupCode") or g.get("groupName") or "").replace("Group ", "").strip()
        gid = g.get("id") or g.get("groupId")
        if gid:
            gmap[code] = as_id(gid)
    for code, et_ids in group_event_map.items():
        gid = gmap.get(code)
        if not gid:
            continue
        for et_id in et_ids:
            req(
                f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/groups/{gid}/teams",
                "POST",
                {"eventTeamId": int(et_id)},
                token,
            )
    lr = req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/lock", "POST", token=token)
    if lr.get("code") != 200:
        raise RuntimeError(f"grouping lock failed: {lr}")
