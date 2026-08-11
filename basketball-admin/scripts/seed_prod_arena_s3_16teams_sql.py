# -*- coding: utf-8 -*-
"""PROD SQL seed: 16 teams for Arena 2026 S3 PC 3V3 (season=131, division=166)."""
from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path

import pymysql

DB = dict(
    host="111.229.241.29",
    port=3306,
    user="root",
    password="zlzcnpa@@qaq",
    database="basketball",
    charset="utf8mb4",
)
SEASON_ID = 131
DIVISION_ID = 166
MARKER = "PROD_SQL_SEED_S3_16T_20260811"
PASS_HASH = "$2a$10$Kq/KEI3FV.cxoKnhm1O/ceh7Q5.8Pm3Vhpb30zzRPTqDEMYzTCOu."
MEMBER_PASS = "111111aa"
ID_BASE = 2099330811300000000
NOW = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
POSITIONS = ["PG", "SG", "SF", "PF", "C"]
OUT = Path(__file__).resolve().parent / "seed_prod_arena_s3_16teams_sql_result.json"
TEAM_COUNT = 16
ROSTER = 5


def conn():
    return pymysql.connect(
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
        connect_timeout=15,
        ssl_disabled=True,
        **DB,
    )


def one(cur, sql, args=None):
    cur.execute(sql, args or ())
    return cur.fetchone()


def build_teams():
    teams = []
    for ti in range(TEAM_COUNT):
        n = ti + 1
        players = []
        for pi in range(ROSTER):
            mid = ID_BASE + ti * 100 + pi
            pid = ID_BASE + 1_000_000 + ti * 100 + pi
            plat = ID_BASE + 50_000_000 + ti * 20 + pi
            nick = f"S3T{n:02d}P{pi+1}"
            email = f"prod_s3_t{n:02d}p{pi+1:02d}@cnpa.smoke"
            players.append(
                {
                    "mid": mid,
                    "pid": pid,
                    "plat": plat,
                    "nick": nick,
                    "gid": f"s3t{n:02d}p{pi+1:02d}",
                    "pos": POSITIONS[pi % len(POSITIONS)],
                    "email": email,
                    "username": f"prod_s3_t{n:02d}p{pi+1:02d}",
                    "player_no": f"CNPA-P-S3-{n:02d}{pi+1:02d}",
                }
            )
        teams.append(
            {
                "idx": ti,
                "team_id": ID_BASE + 20_000_000 + n,
                "name": f"ARENA S3 T{n:02d}",
                "short": f"S3{n:02d}",
                "team_no": f"CNPA-T-S3-{n:03d}",
                "grant_id": 370 + ti,
                "reg_id": 256 + ti,
                "et_id": 230 + ti,
                "players": players,
            }
        )
    return teams


def ensure_account(cur, p):
    if one(cur, "SELECT id FROM basket_member_account WHERE id=%s", (p["mid"],)):
        return
    cur.execute(
        "INSERT INTO basket_member_account "
        "(id, account_no, username, password_hash, email, email_verified, account_status, "
        "register_source, register_terminal, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,%s,%s,1,1,'WEB','PC',%s,%s,0)",
        (p["mid"], f"MBR{p['mid']}", p["username"], PASS_HASH, p["email"], NOW, NOW),
    )
    cur.execute(
        "INSERT INTO basket_member_profile "
        "(id, member_id, nickname, gender, profile_status, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,0,1,%s,%s,0)",
        (p["mid"], p["mid"], p["nick"], NOW, NOW),
    )
    cur.execute(
        "INSERT INTO basket_player_profile "
        "(id, member_id, player_no, game_terminal, game_account, game_nickname, position_code, "
        "certification_status, player_status, certified_time, profile_visible, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,'PC',%s,%s,%s,3,1,%s,1,%s,%s,0)",
        (p["pid"], p["mid"], p["player_no"], p["nick"], p["nick"], p["pos"], NOW, NOW, NOW),
    )
    if not one(
        cur,
        "SELECT id FROM basket_player_platform_account WHERE player_id=%s AND platform='PC' AND is_delete=0",
        (p["pid"],),
    ):
        cur.execute(
            "INSERT INTO basket_player_platform_account "
            "(id, player_id, platform, display_game_id, normalized_game_id, account_status, is_primary, "
            "approved_by, approved_time, effective_time, create_time, update_time, is_delete) "
            "VALUES (%s,%s,'PC',%s,%s,1,1,1,%s,%s,%s,%s,0)",
            (p["plat"], p["pid"], p["gid"], p["gid"], NOW, NOW, NOW, NOW),
        )


def ensure_team(cur, t):
    cap = t["players"][0]
    if one(cur, "SELECT id FROM basket_team WHERE id=%s", (t["team_id"],)):
        cur.execute(
            "UPDATE basket_team SET team_name=%s, short_name=%s, team_no=%s, captain_member_id=%s, "
            "captain_player_id=%s, contact_member_id=%s, division_id=%s, max_member_count=8, "
            "current_member_count=%s, team_status=1, audit_status=1, public_status=1, description=%s, "
            "update_time=%s, is_delete=0 WHERE id=%s",
            (
                t["name"], t["short"], t["team_no"], cap["mid"], cap["pid"], cap["mid"],
                DIVISION_ID, ROSTER, MARKER, NOW, t["team_id"],
            ),
        )
        return
    cur.execute(
        "INSERT INTO basket_team "
        "(id, team_no, team_name, short_name, game_terminal, description, contact_member_id, captain_member_id, "
        "captain_player_id, division_id, max_member_count, current_member_count, team_status, audit_status, "
        "public_status, established_date, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,%s,'GENERAL',%s,%s,%s,%s,%s,8,%s,1,1,1,%s,%s,%s,0)",
        (
            t["team_id"], t["team_no"], t["name"], t["short"], MARKER,
            cap["mid"], cap["mid"], cap["pid"], DIVISION_ID, ROSTER, NOW[:10], NOW, NOW,
        ),
    )


def ensure_members(cur, t):
    for i, p in enumerate(t["players"]):
        role = "CAPTAIN" if i == 0 else "MEMBER"
        join_source = "CREATE" if i == 0 else "INVITE"
        slot = f"{DIVISION_ID}_{p['mid']}"
        exist = one(
            cur,
            "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND is_delete=0",
            (t["team_id"], p["pid"]),
        )
        if exist:
            cur.execute(
                "UPDATE basket_team_member SET membership_status=1, is_current=1, role_type=%s, "
                "active_member_slot=%s, division_id=%s, update_time=%s WHERE id=%s",
                (role, slot, DIVISION_ID, NOW, exist["id"]),
            )
            continue
        cur.execute(
            "UPDATE basket_team_member SET active_member_slot=NULL, is_current=0, membership_status=0, update_time=%s "
            "WHERE active_member_slot=%s AND is_delete=0",
            (NOW, slot),
        )
        mmid = ID_BASE + 21_000_000 + t["idx"] * 10 + i
        cur.execute(
            "INSERT INTO basket_team_member "
            "(id, membership_no, team_id, division_id, member_id, player_id, role_type, roster_type, "
            "join_source, membership_status, is_current, active_member_slot, join_time, create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,%s,%s,'OFFICIAL',%s,1,1,%s,%s,%s,%s,0)",
            (mmid, f"MB{mmid}", t["team_id"], DIVISION_ID, p["mid"], p["pid"], role, join_source, slot, NOW, NOW, NOW),
        )


def ensure_grant(cur, t):
    cap = t["players"][0]
    auk = f"{DIVISION_ID}_{cap['pid']}"
    if one(cur, "SELECT id FROM basket_captain_grant WHERE id=%s", (t["grant_id"],)):
        cur.execute(
            "UPDATE basket_captain_grant SET division_id=%s, season_id=%s, team_id=%s, player_id=%s, member_id=%s, "
            "grant_status='ACTIVE', active_unique_key=%s, valid_from=%s, valid_to='2026-08-31 00:00:00', "
            "granted_by=1, remark=%s, grant_source='MANUAL', update_time=%s, is_delete=0 WHERE id=%s",
            (DIVISION_ID, SEASON_ID, t["team_id"], cap["pid"], cap["mid"], auk, NOW, MARKER, NOW, t["grant_id"]),
        )
        return
    cur.execute(
        "UPDATE basket_captain_grant SET active_unique_key=NULL, grant_status='REVOKED', update_time=%s "
        "WHERE active_unique_key=%s",
        (NOW, auk),
    )
    cur.execute(
        "INSERT INTO basket_captain_grant "
        "(id, division_id, season_id, team_id, player_id, member_id, grant_status, active_unique_key, "
        "valid_from, valid_to, granted_by, remark, grant_source, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,%s,%s,%s,'ACTIVE',%s,%s,'2026-08-31 00:00:00',1,%s,'MANUAL',%s,%s,0)",
        (t["grant_id"], DIVISION_ID, SEASON_ID, t["team_id"], cap["pid"], cap["mid"], auk, NOW, MARKER, NOW, NOW),
    )


def ensure_registration(cur, t):
    cap = t["players"][0]
    if not one(cur, "SELECT id FROM basket_registration WHERE id=%s", (t["reg_id"],)):
        exist = one(
            cur,
            "SELECT id FROM basket_registration WHERE division_id=%s AND team_id=%s AND is_delete=0 "
            "AND registration_status='APPROVED' LIMIT 1",
            (DIVISION_ID, t["team_id"]),
        )
        if exist:
            t["reg_id"] = exist["id"]
        else:
            cur.execute(
                "INSERT INTO basket_registration "
                "(id, season_id, division_id, registration_mode, registration_source, team_id, captain_player_id, "
                "captain_member_id, registration_status, lineup_count, submit_time, approve_time, audit_user_id, "
                "create_time, update_time, is_delete) "
                "VALUES (%s,%s,%s,'TEAM','TEAM_DIRECT',%s,%s,%s,'APPROVED',%s,%s,%s,1,%s,%s,0)",
                (
                    t["reg_id"], SEASON_ID, DIVISION_ID, t["team_id"], cap["pid"], cap["mid"],
                    ROSTER, NOW, NOW, NOW, NOW,
                ),
            )
    for i, p in enumerate(t["players"]):
        if one(
            cur,
            "SELECT id FROM basket_registration_player WHERE registration_id=%s AND player_id=%s AND is_delete=0",
            (t["reg_id"], p["pid"]),
        ):
            continue
        rpid = 1400 + t["idx"] * 10 + i
        cur.execute(
            "INSERT INTO basket_registration_player "
            "(id, registration_id, division_id, season_id, team_id, player_id, member_id, platform_identity_id, "
            "platform_code, game_id_snapshot, player_name_snapshot, position, is_captain, is_starter, sort_order, "
            "create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,'PC',%s,%s,%s,%s,1,%s,%s,%s,0)",
            (
                rpid, t["reg_id"], DIVISION_ID, SEASON_ID, t["team_id"], p["pid"], p["mid"], p["plat"],
                p["gid"], p["nick"], p["pos"], 1 if i == 0 else 0, i, NOW, NOW,
            ),
        )


def ensure_event(cur, t):
    cap = t["players"][0]
    if not one(cur, "SELECT id FROM basket_event_team WHERE id=%s", (t["et_id"],)):
        exist = one(
            cur,
            "SELECT id FROM basket_event_team WHERE division_id=%s AND source_team_id=%s AND is_delete=0",
            (DIVISION_ID, t["team_id"]),
        )
        if exist:
            t["et_id"] = exist["id"]
        else:
            cur.execute(
                "INSERT INTO basket_event_team "
                "(id, season_id, division_id, registration_id, source_team_id, team_name_snapshot, team_short_name, "
                "captain_player_id, captain_member_id, event_team_no, event_team_status, platform_code, "
                "create_time, update_time, is_delete) "
                "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,'ACTIVE','PC',%s,%s,0)",
                (
                    t["et_id"], SEASON_ID, DIVISION_ID, t["reg_id"], t["team_id"], t["name"], t["short"],
                    cap["pid"], cap["mid"], t["team_no"], NOW, NOW,
                ),
            )
    for i, p in enumerate(t["players"]):
        if one(
            cur,
            "SELECT id FROM basket_event_player WHERE event_team_id=%s AND source_player_id=%s AND is_delete=0",
            (t["et_id"], p["pid"]),
        ):
            continue
        epid = 1200 + t["idx"] * 10 + i
        cur.execute(
            "INSERT INTO basket_event_player "
            "(id, season_id, division_id, event_team_id, source_player_id, member_id, platform_identity_id, "
            "platform_code, game_id_snapshot, player_name_snapshot, position, is_captain, is_starter, "
            "event_player_status, create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,%s,%s,'PC',%s,%s,%s,%s,1,'ACTIVE',%s,%s,0)",
            (
                epid, SEASON_ID, DIVISION_ID, t["et_id"], p["pid"], p["mid"], p["plat"],
                p["gid"], p["nick"], p["pos"], 1 if i == 0 else 0, NOW, NOW,
            ),
        )


def prepare_division(cur):
    cur.execute(
        "UPDATE basket_division SET max_team_count=16, update_time=%s WHERE id=%s",
        (NOW, DIVISION_ID),
    )
    # lock snapshot + close registration so schedule path can proceed after seed
    cur.execute(
        "UPDATE basket_division_format_snapshot SET snapshot_status='LOCKED', locked_time=%s, locked_by=1, "
        "update_time=%s WHERE division_id=%s AND is_delete=0 AND is_current=1",
        (NOW, NOW, DIVISION_ID),
    )
    cur.execute(
        "UPDATE basket_division SET division_status='REGISTRATION_CLOSED', format_snapshot_id="
        "(SELECT id FROM basket_division_format_snapshot WHERE division_id=%s AND is_delete=0 AND is_current=1 LIMIT 1), "
        "update_time=%s WHERE id=%s",
        (DIVISION_ID, NOW, DIVISION_ID),
    )


def main():
    print("=== PROD seed Arena S3 16 teams division=166 ===")
    teams = build_teams()
    c = conn()
    cur = c.cursor()
    try:
        div = one(cur, "SELECT id, season_id, division_name, max_team_count FROM basket_division WHERE id=%s", (DIVISION_ID,))
        if not div or int(div["season_id"]) != SEASON_ID:
            raise RuntimeError(f"division mismatch: {div}")
        print("division", div)
        prepare_division(cur)
        for t in teams:
            for p in t["players"]:
                ensure_account(cur, p)
            ensure_team(cur, t)
            ensure_members(cur, t)
            ensure_grant(cur, t)
            ensure_registration(cur, t)
            ensure_event(cur, t)
            print(f"ok {t['name']} teamId={t['team_id']} et={t['et_id']}")
        c.commit()
    except Exception:
        c.rollback()
        raise
    finally:
        c.close()

    c = conn()
    cur = c.cursor()
    cur.execute("SELECT division_status, max_team_count FROM basket_division WHERE id=%s", (DIVISION_ID,))
    print("DIV_AFTER", cur.fetchone())
    cur.execute("SELECT snapshot_status FROM basket_division_format_snapshot WHERE division_id=%s AND is_current=1", (DIVISION_ID,))
    print("SNAP", cur.fetchone())
    cur.execute("SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND registration_status='APPROVED' AND is_delete=0", (DIVISION_ID,))
    reg = cur.fetchone()["c"]
    cur.execute("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (DIVISION_ID,))
    et = cur.fetchone()["c"]
    cur.execute(
        "SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        "WHERE et.division_id=%s AND et.is_delete=0 AND ep.is_delete=0",
        (DIVISION_ID,),
    )
    ep = cur.fetchone()["c"]
    cur.execute("SELECT CAST(id AS CHAR) id, team_name_snapshot FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id", (DIVISION_ID,))
    ets = cur.fetchall()
    c.close()

    out = {
        "db": DB["host"],
        "seasonId": SEASON_ID,
        "divisionId": DIVISION_ID,
        "password": MEMBER_PASS,
        "marker": MARKER,
        "approvedRegistrations": reg,
        "eventTeamCount": et,
        "eventPlayerCount": ep,
        "eventTeams": ets,
        "teams": [
            {
                "name": t["name"],
                "teamId": str(t["team_id"]),
                "eventTeamId": t["et_id"],
                "captainEmail": t["players"][0]["email"],
                "emails": [p["email"] for p in t["players"]],
            }
            for t in teams
        ],
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(f"reg={reg} et={et} ep={ep}")
    print(f"wrote {OUT}")
    print("=== DONE ===")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())