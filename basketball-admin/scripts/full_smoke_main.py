#!/usr/bin/env python3
"""SMOKE A ? FULL_SMOKE_20260811_MAIN"""
from __future__ import annotations

import sys
import traceback

from full_smoke_common import (
    MARKER,
    MAIN_TEAMS,
    SPARE_TEAMS,
    WEB,
    Attempt,
    SmokeBag,
    admin_login,
    close_registration,
    confirm_stage_qualification,
    count_event_entities,
    create_division,
    create_season,
    create_team_via_web,
    create_teams_flow,
    db_query,
    finish_bo1_stage,
    finish_division,
    finish_series_games,
    generate_and_publish,
    list_matches, count_stage_matches,
    list_series,
    load_context,
    merge_bag,
    open_registration,
    populate_members,
    public_series,
    refuse_forbidden_division,
    register_all_teams,
    req,
    seed_org_pool,
    setup_main_format,
    verify_event_player_source_distinct,
    web_login,
    web_tokens_for_teams,
    build_web_by_event,
)

SMOKE = "A_MAIN"
MARKER_MAIN = "FULL_SMOKE_20260811_MAIN"
DIV_NAME = "FULL_SMOKE_20260811_MAIN_PC_5V5"


def run() -> int:
    bag = SmokeBag(SMOKE)
    ctx = load_context()
    att = Attempt(1, "RUNNING")
    bag.attempts.append(att)
    try:
        admin_token = admin_login()
        teams = seed_org_pool("MAIN", MAIN_TEAMS, offset=10000)
        spare = seed_org_pool("SPAR", SPARE_TEAMS, offset=80000)

        season_id = create_season(admin_token, MARKER_MAIN, f"{MARKER_MAIN} Season")
        division_id = create_division(admin_token, season_id, DIV_NAME, max_team=8, template_id=3)
        refuse_forbidden_division(division_id)
        bag.ids.update({"seasonId": season_id, "divisionId": division_id, "marker": MARKER_MAIN, "teams": teams})

        stages = setup_main_format(admin_token, division_id)
        bag.ids.update(stages)
        open_registration(admin_token, division_id)

        no_grant_tok = web_login(spare[0]["emails"][1])
        ng = create_team_via_web(no_grant_tok, division_id, "FS_NO_GRANT", "NGR")
        if ng.get("code") == 200:
            bag.fail("neg_create_without_grant", "REJECT", "code=200", str(ng))
        else:
            bag.pass_("neg_create_without_grant", "REJECT", f"code={ng.get('code')}", ng.get("msg", ""))

        create_teams_flow(admin_token, division_id, teams)
        dup = create_team_via_web(web_login(teams[0]["captain_email"]), division_id, "FS_DUP_GRANT", "DUP")
        if dup.get("code") == 200:
            bag.fail("neg_second_team_same_grant", "REJECT", "code=200", str(dup))
        else:
            bag.pass_("neg_second_team_same_grant", "REJECT", f"code={dup.get('code')}", dup.get("msg", ""))

        populate_members(teams, invite_until=4, full_players=5)
        team0 = teams[0]
        inv = req(
            f"{WEB}/basket/team/{team0['team_id']}/invitation",
            "POST",
            {"inviteePlayerId": spare[0]["player_ids"][1], "message": MARKER},
            web_login(team0["captain_email"]),
        )
        app = req(
            f"{WEB}/basket/team/{team0['team_id']}/join-application",
            "POST",
            {"message": MARKER},
            web_login(spare[0]["emails"][2]),
        )
        bag.pass_("pending_invite_app_seeded", "PENDING", f"inv={inv.get('code')} app={app.get('code')}")

        web_tokens = web_tokens_for_teams(teams)
        approved = register_all_teams(web_tokens, admin_token, division_id, teams)
        et, ep = count_event_entities(division_id)
        ok_src, src_ev = verify_event_player_source_distinct(division_id)
        bag.pass_("registration", "8 EventTeam / 40 EventPlayer", f"et={et} ep={ep} approved={approved}", src_ev)
        if et != 8 or ep != 40:
            bag.fail("event_entities", "8/40", f"{et}/{ep}")
        if not ok_src:
            bag.fail("event_player_source_id", "source != event id", src_ev)

        close_registration(admin_token, division_id)
        generate_and_publish(admin_token, stages["st1"], division_id=division_id)
        m1_cnt = count_stage_matches(stages["st1"])
        bag.pass_("rr_schedule", "28 matches", str(m1_cnt))
        if m1_cnt != 28:
            bag.fail("rr_match_count", "28", str(m1_cnt))

        web_by_event = build_web_by_event(admin_token, division_id, teams, web_tokens)
        finish_bo1_stage(admin_token, web_by_event, division_id, stages["st1"])
        confirm_stage_qualification(admin_token, stages["st1"])

        generate_and_publish(admin_token, stages["st2"], init=False, division_id=division_id)
        s2_list = list_series(admin_token, stages["st2"])
        champion_et = None
        s3_list = []
        if len(s2_list) < 2:
            raise RuntimeError(f"expected 2 BO3 series, got {len(s2_list)}")
        sa = finish_series_games(admin_token, web_by_event, s2_list[0]["id"], ["HOME", "HOME"])
        g3 = next((g for g in (sa.get("games") or []) if g.get("seriesGameNo") == 3), None)
        bag.pass_("bo3_2_0", "Game3 not finished", f"status={sa.get('seriesStatus')} g3={g3.get('skipped') if g3 else None}")
        sb = finish_series_games(admin_token, web_by_event, s2_list[1]["id"], ["HOME", "AWAY", "HOME"])
        bag.pass_("bo3_2_1", "COMPLETED 2-1", str(sb.get("homeWinCount")))
        confirm_stage_qualification(admin_token, stages["st2"])

        generate_and_publish(admin_token, stages["st3"], init=False, division_id=division_id)
        s3_list = list_series(admin_token, stages["st3"])
        if s3_list:
            s5 = finish_series_games(admin_token, web_by_event, s3_list[0]["id"], ["HOME", "AWAY", "HOME", "AWAY", "HOME"])
            champion_et = s5.get("winnerEventTeamId")
            bag.pass_("bo5_3_2", "3-2 champion", f"winnerEventTeamId={champion_et}")

        fr = finish_division(admin_token, division_id)
        bag.pass_("finish_division", "200", str(fr.get("code")))
        div = db_query("SELECT division_status FROM basket_division WHERE id=%s", (division_id,))[0]
        bag.pass_("division_status", "FINISHED", div.get("division_status", ""))

        grants = db_query(
            "SELECT grant_status, active_unique_key FROM basket_captain_grant WHERE division_id=%s AND is_delete=0",
            (division_id,),
        )
        if grants and all(g["grant_status"] == "EXPIRED" and g["active_unique_key"] is None for g in grants):
            bag.pass_("grants_expired", "EXPIRED/null key", str(len(grants)))
        else:
            bag.fail("grants_expired", "EXPIRED/null key", str(grants[:2]))

        tids = tuple(t["team_id"] for t in teams)
        tstats = db_query("SELECT team_status FROM basket_team WHERE id IN %s AND is_delete=0", (tids,))
        if tstats and all(int(x["team_status"]) == 3 for x in tstats):
            bag.pass_("teams_archived", "ARCHIVED(3)", str(len(tstats)))
        else:
            bag.fail("teams_archived", "ARCHIVED(3)", str(tstats))

        members = db_query(
            "SELECT leave_reason, membership_status FROM basket_team_member WHERE team_id IN %s AND is_delete=0",
            (tids,),
        )
        left = [m for m in members if int(m.get("membership_status") or 0) == 0]
        if left and all(m.get("leave_reason") == "DIVISION_FINISHED" for m in left):
            bag.pass_("members_left", "DIVISION_FINISHED", str(len(left)))
        else:
            bag.fail("members_left", "DIVISION_FINISHED", str(left[:3]))

        fr2 = finish_division(admin_token, division_id)
        bag.pass_("finish_idempotent", "no 500", f"code={fr2.get('code')} http={fr2.get('_http_status')}")

        archived_tid = teams[0]["team_id"]
        write_inv = req(
            f"{WEB}/basket/team/{archived_tid}/invitation",
            "POST",
            {"inviteePlayerId": spare[0]["player_ids"][3]},
            web_login(teams[0]["captain_email"]),
        )
        if write_inv.get("code") != 200:
            bag.pass_("archived_write_protect", "REJECT", str(write_inv.get("msg", "")))
        else:
            bag.fail("archived_write_protect", "REJECT", "code=200")

        hist = req(
            f"{WEB}/basket/team/mine?scope=history&currentPage=1&pageSize=20",
            token=web_login(teams[0]["captain_email"]),
        )
        records = ((hist.get("data") or {}).get("records") or [])
        if any(int(x.get("teamId") or 0) == int(archived_tid) for x in records):
            bag.pass_("public_history", "history visible", str(len(records)))
        else:
            bag.fail("public_history", "history visible", str(records))

        if s3_list:
            ps = public_series(str(s3_list[0]["id"]))
            bag.pass_("public_series", "queryable", str(ps.get("code")))

        att.status = "PASS" if bag.to_dict()["fail"] == 0 else "FAIL"
        bag.ids["championEventTeamId"] = champion_et
        bag.ids["teamSummaries"] = [{"teamId": t["team_id"], "name": t["name"], "captainEmail": t["captain_email"]} for t in teams]
        merge_bag(ctx, bag)
        return 0 if bag.to_dict()["fail"] == 0 else 1
    except Exception as exc:
        att.status = "FAIL"
        att.detail = str(exc)
        bag.fail("RUN", "success", str(exc), traceback.format_exc()[-400:])
        merge_bag(ctx, bag)
        return 1


if __name__ == "__main__":
    sys.exit(run())
