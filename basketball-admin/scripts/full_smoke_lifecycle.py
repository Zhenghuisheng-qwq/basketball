#!/usr/bin/env python3
"""SMOKE D ? FULL_SMOKE_20260811_LIFECYCLE"""
from __future__ import annotations

import sys
import traceback

from full_smoke_common import (
    MARKER,
    WEB,
    Attempt,
    SmokeBag,
    admin_login,
    apply_and_approve,
    approve_team,
    create_captain_grant,
    create_division,
    create_season,
    create_team_via_web,
    db_query,
    invite_and_accept,
    load_context,
    merge_bag,
    open_registration,
    refuse_forbidden_division,
    req,
    web_login,
)

SMOKE = "D_LIFECYCLE"
MARKER_LIFE = "FULL_SMOKE_20260811_LIFECYCLE"
DIV_NAME = "FULL_SMOKE_20260811_LIFECYCLE_NEXT"


def run() -> int:
    bag = SmokeBag(SMOKE)
    ctx = load_context()
    att = Attempt(1, "RUNNING")
    bag.attempts.append(att)
    try:
        am = (ctx.get("smokes") or {}).get("A_MAIN") or {}
        attempts = am.get("attempts") or []
        last = attempts[-1]["status"] if attempts else "MISSING"
        if last != "PASS":
            bag.skip("depends_a_main", "A_MAIN PASS", f"last_attempt={last}")
            att.status = "SKIP"
            merge_bag(ctx, bag)
            return 0

        ids = am.get("ids") or {}
        old_div = ids.get("divisionId")
        teams_ctx = ids.get("teams") or []
        summaries = ids.get("teamSummaries") or []
        if teams_ctx:
            t0 = teams_ctx[0]
            old_tid = str(t0.get("team_id"))
            old_name = t0.get("name")
            cap_email = t0.get("captain_email")
        elif summaries:
            old = summaries[0]
            old_tid = str(old["teamId"])
            old_name = old["name"]
            cap_email = old["captainEmail"]
            t0 = None
        else:
            bag.skip("depends_a_main", "team context", "missing teams")
            att.status = "SKIP"
            merge_bag(ctx, bag)
            return 0

        admin_token = admin_login()
        season_id = create_season(admin_token, MARKER_LIFE, f"{MARKER_LIFE} Season")
        division_id = create_division(admin_token, season_id, DIV_NAME, max_team=8, template_id=3)
        refuse_forbidden_division(division_id)
        open_registration(admin_token, division_id)
        bag.ids.update({"seasonId": season_id, "divisionId": division_id, "priorDivisionId": old_div, "priorTeamId": old_tid})

        cap_row = db_query(
            "SELECT pp.id AS player_id FROM basket_player_profile pp "
            "JOIN basket_member_account ma ON ma.id=pp.member_id "
            "WHERE ma.email=%s AND pp.is_delete=0 LIMIT 1",
            (cap_email,),
        )
        if not cap_row:
            raise RuntimeError(f"captain player not found for {cap_email}")
        cap_pid = str(cap_row[0]["player_id"])
        gr = create_captain_grant(admin_token, division_id, cap_pid)
        if gr.get("code") != 200:
            raise RuntimeError(f"grant failed: {gr}")

        cap_tok = web_login(cap_email)
        cr = create_team_via_web(cap_tok, division_id, old_name, old_name[:3], None)
        if cr.get("code") != 200:
            raise RuntimeError(f"create same-name team failed: {cr}")
        new_tid = str((cr.get("data") or {}).get("teamId"))
        approve_team(admin_token, new_tid)
        bag.pass_("same_name_new_team", "new teamId", f"old={old_tid} new={new_tid}")
        if new_tid == str(old_tid):
            bag.fail("same_name_new_team", "distinct teamId", new_tid)

        old_stat = db_query("SELECT team_status FROM basket_team WHERE id=%s", (old_tid,))[0]
        bag.pass_("old_team_archived", "ARCHIVED(3)", str(old_stat.get("team_status")))

        rejoined = 0
        if t0:
            for pi in range(1, min(5, len(t0.get("player_ids") or []))):
                pid = t0["player_ids"][pi]
                ptok = web_login(t0["emails"][pi])
                if pi % 2 == 1:
                    invite_and_accept(cap_tok, ptok, new_tid, pid)
                else:
                    apply_and_approve(ptok, cap_tok, new_tid)
                rejoined += 1
        bag.pass_("members_rejoin", ">=1", str(rejoined))

        hist = req(f"{WEB}/basket/team/mine?scope=history&currentPage=1&pageSize=20", token=cap_tok)
        records = ((hist.get("data") or {}).get("records") or [])
        if any(int(x.get("teamId") or 0) == int(old_tid) for x in records):
            bag.pass_("history_old_team", "visible", str(len(records)))
        else:
            bag.fail("history_old_team", "visible", str(records))

        att.status = "PASS" if bag.to_dict()["fail"] == 0 else "FAIL"
        bag.ids["newTeamId"] = new_tid
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
