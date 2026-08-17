#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Seed CNPA League 2026 S13 (5V5, 16 teams x 5 players) on remote production. Stop after registration."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e

fs.ADMIN = "http://111.229.241.29:8080"
fs.WEB = "http://111.229.241.29:8881"
fs.DB.update({
    "host": "111.229.241.29",
    "user": "root",
    "password": "zlzcnpa@@qaq",
    "database": "basketball",
})
fs.REDIS_HOST = "106.55.61.223"
fs.MARKER = "LEAGUE_S13_REG"
fs.ID_BASE = 2099501719000000000
e2e.ADMIN = fs.ADMIN
e2e.WEB = fs.WEB
e2e.fs = fs

ADMIN = fs.ADMIN
req = fs.req
SEASON_NAME = "S13"
SEASON_TITLE = "CNPA League 2026 S13"
DIV_NAME = "CNPA League 2026 S13 - PC 5V5"
PASS = fs.MEMBER_PASS

TEAM_NAMES = [
    "\u730e\u9e70", "\u96f7\u9706", "\u98ce\u66b4", "\u591c\u7ffc",
    "\u8d64\u7130", "\u5bd2\u971c", "\u9ed1\u9a91", "\u94f6\u72fc",
    "\u91d1\u9ca8", "\u9752\u9f99", "\u767d\u864e", "\u6731\u96c0",
    "\u7384\u6b66", "\u661f\u706b", "\u72c2\u6f9c", "\u7834\u6653",
]


def log(msg):
    print(msg, flush=True)


def need(resp, name):
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def team_defs():
    out = []
    for i, name in enumerate(TEAM_NAMES, 1):
        code = f"L{i:02d}"
        out.append({
            "code": code,
            "name": f"S13{name}",
            "short": code,
            "players": [f"{name}\u961f\u957f", f"{name}2", f"{name}3", f"{name}4", f"{name}5"],
        })
    return out


def create_division(token, season_id):
    existing = e2e.find_division(DIV_NAME)
    if existing:
        return existing
    body = {
        "seasonId": season_id,
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": DIV_NAME,
        "registrationMode": "TEAM",
        "formatTemplateId": 2,
        "maxTeamCount": 16,
        "maxPersonCount": 128,
        "lineupMinCount": 5,
        "lineupMaxCount": 8,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-09-30 23:59:59",
        "matchStartTime": "2026-10-01 00:00:00",
        "matchEndTime": "2026-12-31 23:59:59",
        "sortOrder": 1,
    }
    r = need(req(f"{ADMIN}/basket/v4/division", "POST", body, token), "create-div")
    return int(fs.extract_id(r))


def apply_league_rule(token, did):
    snap = e2e.snapshot(token, did)
    status = snap.get("snapshotStatus") or snap.get("status")
    if status in ("LOCKED", "PUBLISHED") or snap.get("locked") is True:
        log(f"rule already locked status={status}")
        return "LEAGUE_STANDARD_V1"
    applied = e2e.apply_standard(token, did, "LEAGUE_STANDARD_V1", "TEAM", "FIVE_V_FIVE")
    if applied.get("code") != 200:
        log(f"LEAGUE_STANDARD_V1 apply failed: {applied.get('msg')}; fallback ROUND_ROBIN_BASIC_V1")
        need(e2e.apply_standard(token, did, "ROUND_ROBIN_BASIC_V1", "TEAM", "FIVE_V_FIVE"), "apply-rr")
        used = "ROUND_ROBIN_BASIC_V1"
    else:
        used = "LEAGUE_STANDARD_V1"
    need(e2e.lock_rule(token, did), "lock-rule")
    return used


def register_only(admin_token, did, teams):
    tokens = fs.web_tokens_for_teams(teams)
    for t in teams:
        gr = fs.create_captain_grant(admin_token, str(did), t["captain_player_id"])
        msg = str(gr.get("msg") or "")
        if gr.get("code") != 200 and "\u5df2" not in msg and "\u5b58\u5728" not in msg:
            log(f"  grant warn {t['code']}: {gr.get('msg')}")
    n = fs.register_all_teams(tokens, admin_token, str(did), teams)
    log(f"approved registrations this pass: {n}")
    et = fs.db_query(
        "SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0",
        (did,),
    )
    et_n = int((et[0] or {}).get("c") or 0) if et else 0
    if et_n < 16:
        cr = req(f"{ADMIN}/basket/v4/division/{did}/close-registration", "PUT", token=admin_token)
        log(f"close-reg: {cr.get('code')} {cr.get('msg')}")
        ar = req(f"{ADMIN}/basket/v4/divisions/{did}/roster/team/auto-allocate", "POST", {}, admin_token)
        log(f"auto-allocate: {ar.get('code')} {ar.get('msg')}")
    return tokens


def main():
    if not fs.wait_server(f"{ADMIN}/", 20):
        raise RuntimeError("remote admin 8080 not up")
    if not fs.wait_server(f"{fs.WEB}/", 10):
        raise RuntimeError("remote web 8881 not up")
    token = fs.admin_login()
    log("admin login ok")
    defs = team_defs()
    teams = fs.seed_org_pool("S13L", defs, offset=60000)
    log(f"org pool {len(teams)} teams")
    emails = [e for t in teams for e in t["emails"]]
    e2e.clear_locks([t["captain_email"] for t in teams] + emails)
    season_id = e2e.create_season(token, 1, SEASON_NAME, SEASON_TITLE)
    log(f"season {season_id}")
    req(f"{ADMIN}/basket/v4/season/{season_id}", "PUT", {
        "id": season_id,
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-12-31 23:59:59",
        "displayTitle": SEASON_TITLE,
        "seasonName": SEASON_NAME,
    }, token)
    did = create_division(token, season_id)
    log(f"division {did}")
    used = apply_league_rule(token, did)
    log(f"template {used}")
    e2e.open_reg(token, did)
    fs.create_teams_flow(token, str(did), teams)
    log("teams created/approved")
    fs.populate_members(teams, invite_until=len(teams), full_players=5)
    log("members populated")
    register_only(token, did, teams)
    rows = fs.db_query(
        "SELECT et.id event_team_id, et.team_name_snapshot, r.registration_status, r.participation_role "
        "FROM basket_event_team et LEFT JOIN basket_registration r ON r.id=et.registration_id "
        "WHERE et.division_id=%s AND et.is_delete=0 ORDER BY et.id",
        (did,),
    )
    log(f"event_teams={len(rows)}")
    captains = []
    for i, t in enumerate(teams):
        captains.append({
            "team": t["name"],
            "captainName": defs[i]["players"][0],
            "captainEmail": t["captain_email"],
            "teamId": t.get("team_id"),
        })
    out = {
        "seasonId": season_id,
        "seasonTitle": SEASON_TITLE,
        "divisionId": did,
        "divisionName": DIV_NAME,
        "template": used,
        "eventTeamCount": len(rows),
        "password": PASS,
        "captains": captains,
    }
    Path(__file__).with_name("cnpa_prod_league_s13_seed_result.json").write_text(
        json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps(out, ensure_ascii=False, indent=2), flush=True)


if __name__ == "__main__":
    main()
