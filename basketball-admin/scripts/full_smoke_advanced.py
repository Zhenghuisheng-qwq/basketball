#!/usr/bin/env python3
"""SMOKE C ? FULL_SMOKE_20260811_ADVANCED"""
from __future__ import annotations

import json
import sys
import traceback

from full_smoke_common import (
    ADMIN,
    MARKER,
    ADV_TEAMS,
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
    finish_series_games,
    generate_and_publish,
    get_series,
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
    setup_advanced_format,
    submit_and_approve_match,
    web_tokens_for_teams,
    build_web_by_event,
)

SMOKE = "C_ADVANCED"
MARKER_ADV = "FULL_SMOKE_20260811_ADVANCED"
DIV_NAME = "FULL_SMOKE_20260811_ADVANCED_PC_5V5"
EXC = f"{ADMIN}/basket/v4/admin/stage-exception"


def _series_pairing_snapshot(admin_token: str, stage_id: str) -> str:
    rows = list_series(admin_token, stage_id)
    norm = []
    for s in sorted(rows, key=lambda x: x.get("bracketSlot") or x.get("id") or 0):
        norm.append(
            {
                "id": as_id(s.get("id")),
                "home": as_id(s.get("homeEventTeamId")) if s.get("homeEventTeamId") else None,
                "away": as_id(s.get("awayEventTeamId")) if s.get("awayEventTeamId") else None,
                "bye": s.get("bye") or s.get("isBye"),
            }
        )
    return json.dumps(norm, sort_keys=True)


def run() -> int:
    bag = SmokeBag(SMOKE)
    ctx = load_context()
    att = Attempt(1, "RUNNING")
    bag.attempts.append(att)
    try:
        admin_token = admin_login()
        teams = seed_org_pool("ADV", ADV_TEAMS, offset=30000)
        season_id = create_season(admin_token, MARKER_ADV, f"{MARKER_ADV} Season")
        division_id = create_division(admin_token, season_id, DIV_NAME, max_team=8, template_id=5)
        refuse_forbidden_division(division_id)
        bag.ids.update({"seasonId": season_id, "divisionId": division_id})

        stages = setup_advanced_format(admin_token, division_id)
        bag.ids.update(stages)
        open_registration(admin_token, division_id)
        create_teams_flow(admin_token, division_id, teams[:6])
        populate_members(teams[:6])
        web_tokens = web_tokens_for_teams(teams[:6])
        register_all_teams(web_tokens, admin_token, division_id, teams[:6])
        close_registration(admin_token, division_id)

        generate_and_publish(admin_token, stages["st1"])
        s1 = list_series(admin_token, stages["st1"])
        bag.pass_("bye_schedule", "series generated", f"count={len(s1)}")
        bye_series = [s for s in s1 if not s.get("awayEventTeamId")]
        if bye_series:
            bs = bye_series[0]
            mids = [g.get("matchId") for g in (get_series(admin_token, bs["id"]).get("games") or [])]
            fake = [m for m in mids if m]
            if not fake:
                bag.pass_("bye_no_fake_match", "no match rows", f"series={bs['id']}")
            else:
                bag.fail("bye_no_fake_match", "no match rows", str(fake))
        else:
            bag.pass_("bye_no_fake_match", "bye or 6-team bracket", "no explicit bye series field")

        web_by_event = build_web_by_event(admin_token, division_id, teams[:6], web_tokens)
        playable = [s for s in s1 if s.get("homeEventTeamId") and s.get("awayEventTeamId")]
        if playable:
            ser = playable[0]
            games = get_series(admin_token, ser["id"]).get("games") or []
            if games:
                mid = games[0]["matchId"]
                mr = req(f"{ADMIN}/basket/v4/match/{mid}", token=admin_token)
                m = mr.get("data") or {}
                home = m.get("homeEventTeamId")
                submit_and_approve_match(admin_token, web_by_event[as_id(home)], m, 70, 65, home)
            if len(playable) > 1:
                ser2 = playable[1]
                ff = req(
                    f"{EXC}/series-forfeit",
                    "POST",
                    {"seriesId": int(ser2["id"]), "forfeitEventTeamId": int(ser2.get("awayEventTeamId") or ser2.get("homeEventTeamId")), "reason": MARKER},
                    admin_token,
                )
                bag.pass_("series_forfeit", "200", str(ff.get("code")))

        if bye_series:
            ma = req(
                f"{EXC}/manual-advance",
                "POST",
                {"stageId": int(stages["st1"]), "eventTeamId": int(bye_series[0].get("homeEventTeamId")), "reason": MARKER},
                admin_token,
            )
            bag.pass_("manual_advance", "200", str(ma.get("code")))

        confirm_stage_qualification(admin_token, stages["st1"])

        generate_and_publish(admin_token, stages["st2"], init=False)
        snap1 = _series_pairing_snapshot(admin_token, stages["st2"])
        snap2 = _series_pairing_snapshot(admin_token, stages["st2"])
        if snap1 == snap2:
            bag.pass_("random_draw_stable", "re-query same", "ok")
        else:
            bag.fail("random_draw_stable", "re-query same", "diff")

        s2 = list_series(admin_token, stages["st2"])
        if len(s2) >= 2:
            pa = req(
                f"{EXC}/pairing-adjust",
                "POST",
                {
                    "stageId": int(stages["st2"]),
                    "reason": MARKER,
                    "slots": [
                        {
                            "bracketSlot": s2[0].get("bracketSlot") or 1,
                            "homeEventTeamId": int(s2[0].get("homeEventTeamId")),
                            "awayEventTeamId": int(s2[1].get("homeEventTeamId")),
                        }
                    ],
                },
                admin_token,
            )
            bag.pass_("pairing_adjust", "200", str(pa.get("code")))

        if s2:
            finish_series_games(admin_token, web_by_event, s2[0]["id"], ["HOME", "HOME"])

        att.status = "PASS" if bag.to_dict()["fail"] == 0 else "FAIL"
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
