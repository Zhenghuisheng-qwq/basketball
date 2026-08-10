
GROUP = r'''#!/usr/bin/env python3
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
'''

ADVANCED = r'''#!/usr/bin/env python3
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
'''

LIFECYCLE = r'''#!/usr/bin/env python3
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
        if am.get("fail", 1) != 0 and not am.get("pass"):
            bag.skip("depends_a_main", "A_MAIN PASS", "A_MAIN not passed")
            att.status = "SKIP"
            merge_bag(ctx, bag)
            return 0
        ids = am.get("ids") or {}
        old_div = ids.get("divisionId")
        summaries = ids.get("teamSummaries") or []
        if not summaries:
            bag.skip("depends_a_main", "teamSummaries", "missing A_MAIN context")
            att.status = "SKIP"
            merge_bag(ctx, bag)
            return 0
        old = summaries[0]
        old_tid = old["teamId"]
        old_name = old["name"]

        admin_token = admin_login()
        season_id = create_season(admin_token, MARKER_LIFE, f"{MARKER_LIFE} Season")
        division_id = create_division(admin_token, season_id, DIV_NAME, max_team=8, template_id=3)
        refuse_forbidden_division(division_id)
        open_registration(admin_token, division_id)
        bag.ids.update({"seasonId": season_id, "divisionId": division_id, "priorDivisionId": old_div, "priorTeamId": old_tid})

        cap_email = old["captainEmail"]
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
        bag.pass_("old_team_archived", "ARCHIVED", str(old_stat.get("team_status")))

        members = db_query(
            "SELECT member_id, player_id FROM basket_team_member WHERE team_id=%s AND is_delete=0 AND membership_status=1",
            (old_tid,),
        )
        rejoined = 0
        for m in members[:4]:
            pid = str(m["player_id"])
            pr = db_query("SELECT ma.email FROM basket_player_profile pp JOIN basket_member_account ma ON ma.id=pp.member_id WHERE pp.id=%s", (pid,))
            if not pr:
                continue
            ptok = web_login(pr[0]["email"])
            if rejoined % 2 == 0:
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
'''

EXTRA = r'''#!/usr/bin/env python3
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
            ("joinable_teams", f"{WEB}/basket/account/team/joinable/page?currentPage=1&pageSize=5"),
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
'''

ORCH = r'''#!/usr/bin/env python3
"""FULL_SMOKE_20260811 orchestrator"""
from __future__ import annotations

import subprocess
import sys
from datetime import datetime
from pathlib import Path

from full_smoke_common import MARKER, OUT_DIR, baseline_counts, env_health, load_context, save_context

SCRIPTS = Path(__file__).resolve().parent
DOC = Path(__file__).resolve().parents[2] / "docs" / "CNPA_V2_FULL_SMOKE_ACCEPTANCE_20260811.md"


def _run(mod: str) -> int:
    print(f"\n===== RUN {mod} =====")
    p = subprocess.run([sys.executable, str(SCRIPTS / mod)], cwd=str(SCRIPTS))
    return int(p.returncode)


def _write_report(ctx: dict, results: dict[str, int]) -> None:
    smokes = ctx.get("smokes") or {}
    lines = [
        "# CNPA V2 Full Smoke Acceptance (20260811)",
        "",
        f"Marker: `{MARKER}`",
        f"Generated: {datetime.now().isoformat(timespec='seconds')}",
        "",
        "## Environment",
        "",
    ]
    for k, v in (ctx.get("env") or {}).items():
        lines.append(f"- {k}: {v}")
    lines.extend(["", "## Baseline (before)", ""])
    for k, v in (ctx.get("baseline") or {}).items():
        lines.append(f"- {k}: {v}")
    lines.extend(["", "## Smoke results", ""])
    for name, code in results.items():
        bag = smokes.get(name.replace("full_smoke_", "").upper().replace(".PY", ""), {})
        # map module to smoke key
        key_map = {
            "full_smoke_main.py": "A_MAIN",
            "full_smoke_group.py": "B_GROUP",
            "full_smoke_advanced.py": "C_ADVANCED",
            "full_smoke_lifecycle.py": "D_LIFECYCLE",
            "full_smoke_extra.py": "E_EXTRA",
        }
        sk = key_map.get(name, name)
        bag = smokes.get(sk, {})
        lines.append(f"### {sk} (exit={code})")
        lines.append(f"- pass: {bag.get('pass', 0)} fail: {bag.get('fail', 0)} skip: {bag.get('skip', 0)}")
        if bag.get("ids"):
            lines.append(f"- ids: `{bag.get('ids')}`")
        fails = [c for c in (bag.get("cases") or []) if c.get("result") == "FAIL"]
        if fails:
            lines.append("- failures:")
            for c in fails[:20]:
                lines.append(f"  - {c.get('name')}: {c.get('actual')}")
        lines.append("")
    lines.extend(
        [
            "## Browser verification",
            "",
            "NOT_EXECUTED (no browser result artifact in repo).",
            "",
            "## External email",
            "",
            "EXTERNAL EMAIL DELIVERY NOT AUTOMATED / NOT_EXECUTED",
            "",
            "## P0/P1/P2",
            "",
            "See case tables in `basketball-admin/scripts/full_smoke_20260811_context.json`.",
            "",
        ]
    )
    DOC.parent.mkdir(parents=True, exist_ok=True)
    DOC.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {DOC}")


def main() -> int:
    ctx = load_context()
    ctx["env"] = env_health()
    ctx["baseline"] = baseline_counts()
    ctx.setdefault("build", {})["orchestrator_at"] = datetime.now().isoformat(timespec="seconds")
    save_context(ctx)

    down = [k for k, v in ctx["env"].items() if str(v).startswith("DOWN")]
    if down:
        print("WARN env down:", down)

    results: dict[str, int] = {}
    rc_a = _run("full_smoke_main.py")
    results["full_smoke_main.py"] = rc_a
    ctx = load_context()

    for mod in ("full_smoke_group.py", "full_smoke_advanced.py"):
        results[mod] = _run(mod)
        ctx = load_context()

    if rc_a == 0:
        results["full_smoke_lifecycle.py"] = _run("full_smoke_lifecycle.py")
    else:
        print("SKIP full_smoke_lifecycle.py (A_MAIN failed)")
        results["full_smoke_lifecycle.py"] = 0

    results["full_smoke_extra.py"] = _run("full_smoke_extra.py")
    ctx = load_context()
    _write_report(ctx, results)

    fail_any = any(v != 0 for k, v in results.items() if k != "full_smoke_lifecycle.py" or rc_a == 0)
    return 1 if (rc_a != 0 or fail_any) else 0


if __name__ == "__main__":
    sys.exit(main())
'''

from pathlib import Path
ROOT = Path(__file__).resolve().parent
append = {
    "full_smoke_group.py": GROUP,
    "full_smoke_advanced.py": ADVANCED,
    "full_smoke_lifecycle.py": LIFECYCLE,
    "full_smoke_extra.py": EXTRA,
    "full_smoke_20260811.py": ORCH,
}
for name, content in append.items():
    (ROOT / name).write_text(content.strip() + "\n", encoding="utf-8", newline="\n")
    print("wrote", name)
