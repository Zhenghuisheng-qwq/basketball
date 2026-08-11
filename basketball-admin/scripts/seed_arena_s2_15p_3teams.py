# -*- coding: utf-8 -*-
"""Seed Arena 2026 S2 PC 3V3 with 15 players / 3 teams and approve registration.

Local note: divisionId=165 in current DB is League S4 PC 5V5 (FINISHED), which does NOT
match the caller's Arena/3V3 payload. This script creates the matching Arena season
and division, then seeds participants via real Admin/Web APIs (+ minimal account SQL).
"""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

from full_smoke_common import (
    ADMIN,
    WEB,
    MEMBER_PASS,
    admin_login,
    approve_all_registrations,
    approve_team,
    as_id,
    clear_login_locks,
    count_event_entities,
    create_captain_grant,
    create_team_via_web,
    db_query,
    extract_id,
    invite_and_accept,
    open_registration,
    register_team,
    req,
    seed_org_pool,
    setup_main_format,
    web_login,
)

MARKER = "ARENA_S2_15P_3T_20260811"
SERIES_ARENA = 2
OUT = Path(__file__).resolve().parent / "seed_arena_s2_15p_3teams_result.json"

TEAM_DEFS = [
    {"code": "A01", "name": "ARENA ALPHA", "short": "ALP", "players": [f"AA{i:03d}" for i in range(1, 6)]},
    {"code": "A02", "name": "ARENA BRAVO", "short": "BRV", "players": [f"AB{i:03d}" for i in range(1, 6)]},
    {"code": "A03", "name": "ARENA CHARLIE", "short": "CHA", "players": [f"AC{i:03d}" for i in range(1, 6)]},
]


def ensure_arena_season(token: str) -> str:
    season_name = "CNPA Arena 2026 S2"
    display = "CNPA Arena 2026 S2"
    lst = req(f"{ADMIN}/basket/v4/season/list?seriesId={SERIES_ARENA}&pageNum=1&pageSize=200", token=token)
    for row in lst.get("rows") or []:
        if row.get("seasonName") == season_name or row.get("displayTitle") == display:
            sid = as_id(row["id"])
            if row.get("publishStatus") != "PUBLISHED":
                pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
                if pr.get("code") != 200:
                    raise RuntimeError(f"publish season failed: {pr}")
            return sid
    body = {
        "seriesId": SERIES_ARENA,
        "seasonYear": 2026,
        "seasonName": season_name,
        "displayTitle": display,
        "description": MARKER,
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-12-31 23:59:59",
    }
    r = req(f"{ADMIN}/basket/v4/season", "POST", body, token)
    if r.get("code") != 200:
        raise RuntimeError(f"create season failed: {r}")
    sid = extract_id(r)
    pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
    if pr.get("code") != 200:
        raise RuntimeError(f"publish season failed: {pr}")
    return sid


def ensure_pc_3v3_division(token: str, season_id: str) -> str:
    division_name = "CNPA Arena 2026 S2 - PC 3V3"
    lst = req(f"{ADMIN}/basket/v4/division/list?seasonId={season_id}&pageNum=1&pageSize=50", token=token)
    for row in lst.get("rows") or []:
        if row.get("divisionName") == division_name or (
            row.get("platformCode") == "PC" and row.get("gameMode") == "THREE_V_THREE"
        ):
            return as_id(row["id"])
    body = {
        "seasonId": int(season_id),
        "platformCode": "PC",
        "gameMode": "THREE_V_THREE",
        "divisionName": division_name,
        "registrationMode": "TEAM",
        "formatType": "REGULAR_ROUND_ROBIN",
        "formatTemplateId": 3,
        "maxTeamCount": 6,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-31 23:59:59",
        "matchStartTime": "2026-09-01 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",
        "sortOrder": 1,
    }
    dr = req(f"{ADMIN}/basket/v4/division", "POST", body, token)
    if dr.get("code") != 200:
        raise RuntimeError(f"create division failed: {dr}")
    return extract_id(dr)


def resolve_team_id(cr: dict, division_id: str, captain_player_id: str) -> str:
    data = cr.get("data")
    if isinstance(data, dict):
        tid = data.get("teamId") or data.get("id")
        if tid:
            return as_id(tid)
    if isinstance(data, (int, str)) and str(data).isdigit():
        return as_id(data)
    rows = db_query(
        "SELECT id FROM basket_team WHERE division_id=%s AND captain_player_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
        (int(division_id), int(captain_player_id)),
    )
    if rows:
        return as_id(rows[0]["id"])
    raise RuntimeError(f"cannot resolve team id from create response: {cr}")



def submit_team_certification(token: str, team_id: str) -> dict:
    return req(f"{WEB}/basket/team/{team_id}/submit-certification", "POST", {}, token)


def ensure_team_approved(admin: str, captain_token: str, team_id: str) -> None:
    rows = db_query(
        "SELECT CAST(id AS CHAR) id, audit_status FROM basket_team WHERE id=%s",
        (int(team_id),),
    )
    if not rows:
        raise RuntimeError(f"team not found {team_id}")
    audit = int(rows[0]["audit_status"] or -1)
    if audit == 1:
        return
    if audit in (0, 2, 4):  # PENDING / REJECTED / DRAFT
        if audit in (2, 4):
            sr = submit_team_certification(captain_token, team_id)
            if sr.get("code") != 200:
                raise RuntimeError(f"submit certification failed {team_id}: {sr}")
        ar = approve_team(admin, team_id)
        if ar.get("code") != 200:
            # retry approve alone if start-review raced
            ar2 = req(
                f"{ADMIN}/basket/teamAudit/{team_id}/approve",
                "POST",
                {"auditRemark": MARKER},
                admin,
            )
            if ar2.get("code") != 200:
                raise RuntimeError(f"approve team failed {team_id}: {ar} / {ar2}")
        return
    raise RuntimeError(f"unexpected audit_status={audit} team={team_id}")


def main() -> int:
    print("=== Arena S2 PC 3V3 · seed 15 players / 3 teams ===")
    clear_login_locks([])
    admin = admin_login()
    season_id = ensure_arena_season(admin)
    print(f"seasonId={season_id}")
    division_id = ensure_pc_3v3_division(admin, season_id)
    print(f"divisionId={division_id}")

    setup_main_format(admin, division_id)
    print("format snapshot locked")
    orr = open_registration(admin, division_id)
    print(f"open_registration: {orr.get('code')} {orr.get('msg')}")

    teams = seed_org_pool("AR2", TEAM_DEFS, offset=7_700_000)
    print(f"seeded account/player pool: {sum(len(t['player_ids']) for t in teams)} players")

    result_teams = []
    for td in teams:
        clear_login_locks([td["captain_email"]])
        cap_tok = web_login(td["captain_email"])
        gr = create_captain_grant(admin, division_id, td["captain_player_id"], remark=MARKER)
        if gr.get("code") != 200:
            msg = str(gr.get("msg") or "")
            if "已" not in msg:
                raise RuntimeError(f"grant failed {td['name']}: {gr}")
            grant_id = None
        else:
            grant_id = extract_id(gr)
        print(f"grant {td['name']} grantId={grant_id}")

        cr = create_team_via_web(cap_tok, division_id, td["name"], td["short"])
        rows = db_query(
            "SELECT CAST(id AS CHAR) id, audit_status FROM basket_team "
            "WHERE division_id=%s AND captain_player_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
            (int(division_id), int(td["captain_player_id"])),
        )
        if cr.get("code") != 200 and not rows:
            raise RuntimeError(f"create team failed {td['name']}: {cr}")
        if rows:
            team_id = as_id(rows[0]["id"])
        else:
            team_id = resolve_team_id(cr, division_id, td["captain_player_id"])
        ensure_team_approved(admin, cap_tok, team_id)
        print(f"team {td['name']} teamId={team_id} approved")

        for pid, email in zip(td["player_ids"][1:], td["emails"][1:]):
            clear_login_locks([email])
            inv_tok = web_login(email)
            ir = invite_and_accept(cap_tok, inv_tok, team_id, pid)
            if ir.get("code") != 200:
                raise RuntimeError(f"invite/accept failed {email}: {ir}")
        print(f"team {td['name']} roster ready ({len(td['player_ids'])})")

        clear_login_locks([td["captain_email"]])
        cap_tok = web_login(td["captain_email"])
        rr = register_team(cap_tok, division_id, team_id, td["player_ids"])
        if rr.get("code") != 200:
            raise RuntimeError(f"register failed {td['name']}: {rr}")
        print(f"team {td['name']} registration submitted")

        result_teams.append(
            {
                "name": td["name"],
                "short": td["short"],
                "teamId": team_id,
                "grantId": grant_id,
                "captainEmail": td["captain_email"],
                "emails": td["emails"],
                "playerIds": td["player_ids"],
                "memberIds": td["member_ids"],
            }
        )
        time.sleep(0.2)

    approved = approve_all_registrations(admin, division_id)
    print(f"approved registrations: {approved}")
    counts = count_event_entities(division_id)
    print(f"event entities: {counts}")

    out = {
        "marker": MARKER,
        "password": MEMBER_PASS,
        "seasonId": season_id,
        "divisionId": division_id,
        "divisionName": "CNPA Arena 2026 S2 - PC 3V3",
        "series": "Arena",
        "gameMode": "THREE_V_THREE",
        "platformCode": "PC",
        "teams": result_teams,
        "approvedRegistrations": approved,
        "eventCounts": counts,
        "note": (
            "Caller JSON pointed at divisionId=165 / Arena 3V3, but local DB 165 is "
            "League S4 PC 5V5 FINISHED. Created matching Arena S2 PC 3V3 instead."
        ),
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"wrote {OUT}")
    print("=== DONE ===")
    for t in result_teams:
        print(f"- {t['name']} ({t['short']}) teamId={t['teamId']} captain={t['captainEmail']}")
        for e in t["emails"]:
            print(f"    {e} / {MEMBER_PASS}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"FAILED: {exc}", file=sys.stderr)
        raise
