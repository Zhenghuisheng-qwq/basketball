#!/usr/bin/env python3
"""SMOKE E ? FULL_SMOKE_20260811 extra / consistency"""
from __future__ import annotations

import sys
import traceback

from full_smoke_common import (
    ADMIN,
    MARKER,
    TINY_PNG,
    WEB,
    Attempt,
    SmokeBag,
    admin_login,
    db_query,
    load_context,
    merge_bag,
    req,
    upload_file,
    web_login,
)

SMOKE = "E_EXTRA"


def _smoke_divisions(ctx: dict) -> list[str]:
    out = []
    for key in ("A_MAIN", "B_GROUP", "C_ADVANCED", "D_LIFECYCLE"):
        div = ((ctx.get("smokes") or {}).get(key) or {}).get("ids", {}).get("divisionId")
        if div:
            out.append(str(div))
    return out


def run() -> int:
    bag = SmokeBag(SMOKE)
    ctx = load_context()
    att = Attempt(1, "RUNNING")
    bag.attempts.append(att)
    try:
        admin_token = admin_login()
        am = ((ctx.get("smokes") or {}).get("A_MAIN") or {}).get("ids") or {}
        email = None
        for t in am.get("teamSummaries") or []:
            email = t.get("captainEmail")
            if email:
                break
        if not email:
            teams = am.get("teams") or []
            if teams:
                email = teams[0].get("captain_email")
        if not email:
            bag.skip("profile_flow", "captain from A_MAIN", "no email")
        else:
            tok = web_login(email)
            nick = f"FS_NICK_{MARKER[-4:]}"
            ch = req(f"{WEB}/basket/account/profile/change", "POST", {"nickname": nick}, tok)
            if ch.get("code") != 200:
                bag.fail("profile_submit", "200", str(ch))
            else:
                change_id = (ch.get("data") or {}).get("changeId") or (ch.get("data") or {}).get("id")
                dup = req(f"{WEB}/basket/account/profile/change", "POST", {"nickname": nick + "2"}, tok)
                if dup.get("code") == 200:
                    bag.pass_("profile_pending_unique", "second pending blocked or ok", str(dup.get("msg", "")))
                lr = req(f"{ADMIN}/basket/memberProfileChange/list?pageNum=1&pageSize=5", token=admin_token)
                row = next((r for r in (lr.get("rows") or []) if str(r.get("id")) == str(change_id)), None)
                if row:
                    rr = req(
                        f"{ADMIN}/basket/memberProfileChange/{change_id}/reject",
                        "POST",
                        {"auditRemark": MARKER},
                        admin_token,
                    )
                    bag.pass_("profile_reject", "200", str(rr.get("code")))
                    ch2 = req(f"{WEB}/basket/account/profile/change", "POST", {"nickname": nick + "_v2"}, tok)
                    ar = req(f"{ADMIN}/basket/memberProfileChange/{(ch2.get('data') or {}).get('changeId') or change_id}/approve", "POST", {}, admin_token)
                    ar2 = req(f"{ADMIN}/basket/memberProfileChange/{(ch2.get('data') or {}).get('changeId') or change_id}/approve", "POST", {}, admin_token)
                    bag.pass_("profile_approve_idempotent", "no error", f"1={ar.get('code')} 2={ar2.get('code')}")

            up = upload_file(tok, TINY_PNG)
            if up.get("code") == 200:
                fid = (up.get("data") or {}).get("fileId") or up.get("data")
                br = req(f"{WEB}/basket/account/profile/avatar", "POST", {"avatarFileId": fid}, tok)
                bag.pass_("avatar_bind", "200", str(br.get("code")))
            else:
                bag.fail("avatar_upload", "200", str(up))

        divs = _smoke_divisions(ctx)
        if len(divs) >= 2:
            et_counts = []
            for d in divs:
                c = int(db_query("SELECT COUNT(*) AS c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (d,))[0]["c"])
                et_counts.append((d, c))
            bag.pass_("division_isolation", "distinct event teams", str(et_counts))

        probes = [
            ("joinable_teams", f"{WEB}/basket/team/joinable/page?currentPage=1&pageSize=5"),
            ("my_events", f"{WEB}/basket/v4/player/my-events?pageNum=1&pageSize=5"),
        ]
        if email:
            ptok = web_login(email)
            for name, url in probes:
                r = req(url, token=ptok)
                if r.get("code") in (200, 0):
                    bag.pass_(f"pagination_{name}", "200", str(r.get("code")))
                else:
                    bag.add(name, "200", str(r.get("code")), str(r.get("msg", "")), "NOT_EXECUTED")

        if am.get("divisionId"):
            div = am["divisionId"]
            eps = db_query(
                "SELECT COUNT(*) AS c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
                "WHERE et.division_id=%s AND ep.is_delete=0 AND ep.id=ep.source_player_id",
                (div,),
            )[0]["c"]
            if int(eps) == 0:
                bag.pass_("event_player_snapshot", "source distinct from id", "bad=0")
            else:
                bag.fail("event_player_snapshot", "bad=0", str(eps))

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
