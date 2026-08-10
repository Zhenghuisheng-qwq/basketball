#!/usr/bin/env python3
"""SMOKE B ? FULL_SMOKE_20260811_GROUP"""
from __future__ import annotations

import sys
import traceback

from full_smoke_common import (
    ADMIN,
    MARKER,
    GROUP_TEAMS,
    Attempt,
    SmokeBag,
    admin_login,
    as_id,
    close_registration,
    confirm_stage_qualification,
    create_division,
    create_season,
    create_teams_flow,
    db_query,
    finish_bo1_stage,
    finish_series_games,
    generate_and_publish,
    grouping_assign_and_lock,
    grouping_batch_create,
    list_matches,
    list_series,
    load_context,
    merge_bag,
    open_registration,
    populate_members,
    refuse_forbidden_division,
    register_all_teams,
    req,
    seed_org_pool,
    setup_group_format,
    web_tokens_for_teams,
    build_web_by_event,
)

SMOKE = "B_GROUP"
MARKER_GROUP = "FULL_SMOKE_20260811_GROUP"
DIV_NAME = "FULL_SMOKE_20260811_GROUP_PC_5V5"


def _event_team_ids(admin_token: str, division_id: str) -> list[str]:
    r = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=50", token=admin_token)
    rows = sorted(r.get("rows") or [], key=lambda x: x.get("teamNameSnapshot") or "")
    return [as_id(x["id"]) for x in rows]


def _assign_two_groups(token: str, batch_id: str, et_ids: list[str]) -> None:
    detail = req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}", token=token).get("data") or {}
    groups = sorted(detail.get("groups") or [], key=lambda g: g.get("groupCode") or g.get("groupName") or "")
    if len(groups) < 2:
        raise RuntimeError(f"expected >=2 groups, got {groups}")
    gmap = {}
    for i, g in enumerate(groups[:2]):
        code = (g.get("groupCode") or g.get("groupName") or str(i)).replace("Group ", "").strip()
        gmap[code] = et_ids[i * 4 : (i + 1) * 4]
    grouping_assign_and_lock(token, batch_id, gmap)


def _no_cross_group_matches(division_id: str, stage_id: str) -> tuple[bool, str]:
    rows = db_query(
        "SELECT m.id, m.group_id, gh.group_id AS hg, ga.group_id AS ag "
        "FROM basket_match m "
        "LEFT JOIN basket_division_group_team gh ON gh.event_team_id=m.home_event_team_id AND gh.is_delete=0 "
        "LEFT JOIN basket_division_group_team ga ON ga.event_team_id=m.away_event_team_id AND ga.is_delete=0 "
        "WHERE m.division_id=%s AND m.stage_id=%s AND m.is_delete=0",
        (division_id, stage_id),
    )
    bad = [r for r in rows if r.get("hg") and r.get("ag") and r["hg"] != r["ag"]]
    return len(bad) == 0, f"matches={len(rows)} cross={len(bad)}"


def run() -> int:
    bag = SmokeBag(SMOKE)
    ctx = load_context()
    att = Attempt(1, "RUNNING")
    bag.attempts.append(att)
    try:
        admin_token = admin_login()
        teams = seed_org_pool("GROUP", GROUP_TEAMS, offset=20000)
        season_id = create_season(admin_token, MARKER_GROUP, f"{MARKER_GROUP} Season")
        division_id = create_division(admin_token, season_id, DIV_NAME, max_team=8, template_id=4)
        refuse_forbidden_division(division_id)
        bag.ids.update({"seasonId": season_id, "divisionId": division_id})

        stages = setup_group_format(admin_token, division_id, bag)
        bag.ids.update(stages)
        open_registration(admin_token, division_id)
        create_teams_flow(admin_token, division_id, teams)
        populate_members(teams)
        web_tokens = web_tokens_for_teams(teams)
        register_all_teams(web_tokens, admin_token, division_id, teams)
        close_registration(admin_token, division_id)

        batch_id = grouping_batch_create(admin_token, division_id, stages["st1"])
        et_ids = _event_team_ids(admin_token, division_id)
        _assign_two_groups(admin_token, batch_id, et_ids)
        bag.ids["groupingBatchId"] = batch_id
        bag.pass_("grouping_batch", "locked 4+4", f"batch={batch_id} teams={len(et_ids)}")

        generate_and_publish(admin_token, stages["st1"], init=False)
        m1 = list_matches(admin_token, division_id, stages["st1"])
        ok_cross, ev = _no_cross_group_matches(division_id, stages["st1"])
        bag.pass_("group_rr_no_cross", "no cross-group", ev)
        if not ok_cross:
            bag.fail("group_rr_no_cross", "no cross-group", ev)
        expect = 12
        bag.pass_("group_rr_count", f"{expect} matches", str(len(m1)))
        if len(m1) != expect:
            bag.fail("group_rr_count", str(expect), str(len(m1)))

        web_by_event = build_web_by_event(admin_token, division_id, teams, web_tokens)
        finish_bo1_stage(admin_token, web_by_event, division_id, stages["st1"])
        confirm_stage_qualification(admin_token, stages["st1"])

        generate_and_publish(admin_token, stages["st2"], init=False)
        s2 = list_series(admin_token, stages["st2"])
        for i, ser in enumerate(s2[:2]):
            finish_series_games(admin_token, web_by_event, ser["id"], ["HOME", "HOME"] if i == 0 else ["HOME", "AWAY", "HOME"])
        confirm_stage_qualification(admin_token, stages["st2"])

        generate_and_publish(admin_token, stages["st3"], init=False)
        s3 = list_series(admin_token, stages["st3"])
        if s3:
            finish_series_games(admin_token, web_by_event, s3[0]["id"], ["HOME", "AWAY", "HOME", "AWAY", "HOME"])
            bag.ids["championEventTeamId"] = get_winner(s3[0]["id"], admin_token)

        att.status = "PASS" if bag.to_dict()["fail"] == 0 else "FAIL"
        merge_bag(ctx, bag)
        return 0 if bag.to_dict()["fail"] == 0 else 1
    except Exception as exc:
        att.status = "FAIL"
        att.detail = str(exc)
        bag.fail("RUN", "success", str(exc), traceback.format_exc()[-400:])
        merge_bag(ctx, bag)
        return 1


def get_winner(series_id, admin_token):
    from full_smoke_common import get_series

    return get_series(admin_token, series_id).get("winnerEventTeamId")


if __name__ == "__main__":
    sys.exit(run())
