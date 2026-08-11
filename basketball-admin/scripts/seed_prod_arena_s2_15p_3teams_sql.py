# -*- coding: utf-8 -*-
"""Direct SQL seed: 3 teams / 15 players into PROD division 165 (no HTTP APIs)."""
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
DIVISION_ID = 165
SEASON_ID = 130
MARKER = "PROD_SQL_SEED_15P3T_20260811"
NOW = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
OUT = Path(__file__).resolve().parent / "seed_prod_arena_s2_15p_3teams_sql_result.json"

# Existing account pool from prior partial seed
TEAMS = [
    {
        "team_id": 2087130674674884609,  # already exists
        "name": "PROD ARENA ALPHA",
        "short": "PAL",
        "team_no": "CNPA-T-SEED901",
        "grant_id": 367,  # already exists
        "reg_id": 253,
        "et_id": 227,
        "players": [
            {"mid": 2099220811008800000, "pid": 2099220811009800000, "plat": 2099220811138000000, "nick": "PA001", "gid": "prodp0101", "pos": "PG", "email": "prod_ar2_p0101@cnpa.smoke"},
            {"mid": 2099220811008800001, "pid": 2099220811009800001, "plat": 2099220811138000010, "nick": "PA002", "gid": "prodp0102", "pos": "SG", "email": "prod_ar2_p0102@cnpa.smoke"},
            {"mid": 2099220811008800002, "pid": 2099220811009800002, "plat": 2099220811138000020, "nick": "PA003", "gid": "prodp0103", "pos": "SF", "email": "prod_ar2_p0103@cnpa.smoke"},
            {"mid": 2099220811008800003, "pid": 2099220811009800003, "plat": 2099220811138000030, "nick": "PA004", "gid": "prodp0104", "pos": "PF", "email": "prod_ar2_p0104@cnpa.smoke"},
            {"mid": 2099220811008800004, "pid": 2099220811009800004, "plat": 2099220811138000040, "nick": "PA005", "gid": "prodp0105", "pos": "C", "email": "prod_ar2_p0105@cnpa.smoke"},
        ],
    },
    {
        "team_id": 2099220811200000102,
        "name": "PROD ARENA BRAVO",
        "short": "PBR",
        "team_no": "CNPA-T-SEED902",
        "grant_id": 368,
        "reg_id": 254,
        "et_id": 228,
        "players": [
            {"mid": 2099220811008800100, "pid": 2099220811009800100, "plat": 2099220811138000200, "nick": "PB001", "gid": "prodp0201", "pos": "PG", "email": "prod_ar2_p0201@cnpa.smoke"},
            {"mid": 2099220811008800101, "pid": 2099220811009800101, "plat": 2099220811138000210, "nick": "PB002", "gid": "prodp0202", "pos": "SG", "email": "prod_ar2_p0202@cnpa.smoke"},
            {"mid": 2099220811008800102, "pid": 2099220811009800102, "plat": 2099220811138000220, "nick": "PB003", "gid": "prodp0203", "pos": "SF", "email": "prod_ar2_p0203@cnpa.smoke"},
            {"mid": 2099220811008800103, "pid": 2099220811009800103, "plat": 2099220811138000230, "nick": "PB004", "gid": "prodp0204", "pos": "PF", "email": "prod_ar2_p0204@cnpa.smoke"},
            {"mid": 2099220811008800104, "pid": 2099220811009800104, "plat": 2099220811138000240, "nick": "PB005", "gid": "prodp0205", "pos": "C", "email": "prod_ar2_p0205@cnpa.smoke"},
        ],
    },
    {
        "team_id": 2099220811200000103,
        "name": "PROD ARENA CHARLIE",
        "short": "PCH",
        "team_no": "CNPA-T-SEED903",
        "grant_id": 369,
        "reg_id": 255,
        "et_id": 229,
        "players": [
            {"mid": 2099220811008800200, "pid": 2099220811009800200, "plat": 2099220811138000400, "nick": "PC001", "gid": "prodp0301", "pos": "PG", "email": "prod_ar2_p0301@cnpa.smoke"},
            {"mid": 2099220811008800201, "pid": 2099220811009800201, "plat": 2099220811138000410, "nick": "PC002", "gid": "prodp0302", "pos": "SG", "email": "prod_ar2_p0302@cnpa.smoke"},
            {"mid": 2099220811008800202, "pid": 2099220811009800202, "plat": 2099220811138000420, "nick": "PC003", "gid": "prodp0303", "pos": "SF", "email": "prod_ar2_p0303@cnpa.smoke"},
            {"mid": 2099220811008800203, "pid": 2099220811009800203, "plat": 2099220811138000430, "nick": "PC004", "gid": "prodp0304", "pos": "PF", "email": "prod_ar2_p0304@cnpa.smoke"},
            {"mid": 2099220811008800204, "pid": 2099220811009800204, "plat": 2099220811138000440, "nick": "PC005", "gid": "prodp0305", "pos": "C", "email": "prod_ar2_p0305@cnpa.smoke"},
        ],
    },
]


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


def ensure_team(cur, t):
    cap = t["players"][0]
    row = one(cur, "SELECT id, audit_status FROM basket_team WHERE id=%s", (t["team_id"],))
    if row:
        cur.execute(
            "UPDATE basket_team SET team_name=%s, short_name=%s, team_no=%s, captain_member_id=%s, captain_player_id=%s, "
            "contact_member_id=%s, division_id=%s, max_member_count=8, current_member_count=5, team_status=1, "
            "audit_status=1, public_status=1, description=%s, update_time=%s, is_delete=0 WHERE id=%s",
            (t["name"], t["short"], t["team_no"], cap["mid"], cap["pid"], cap["mid"], DIVISION_ID, MARKER, NOW, t["team_id"]),
        )
        print(f"team update {t['name']} id={t['team_id']}")
        return
    cur.execute(
        "INSERT INTO basket_team "
        "(id, team_no, team_name, short_name, game_terminal, description, contact_member_id, captain_member_id, "
        "captain_player_id, division_id, max_member_count, current_member_count, team_status, audit_status, "
        "public_status, established_date, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,%s,'GENERAL',%s,%s,%s,%s,%s,8,5,1,1,1,%s,%s,%s,0)",
        (
            t["team_id"], t["team_no"], t["name"], t["short"], MARKER,
            cap["mid"], cap["mid"], cap["pid"], DIVISION_ID, NOW[:10], NOW, NOW,
        ),
    )
    print(f"team insert {t['name']} id={t['team_id']}")


def ensure_members(cur, t):
    for i, p in enumerate(t["players"]):
        role = "CAPTAIN" if i == 0 else "MEMBER"
        join_source = "CREATE" if i == 0 else "INVITE"
        slot = f"{DIVISION_ID}_{p['mid']}"
        exist = one(
            cur,
            "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND is_delete=0 LIMIT 1",
            (t["team_id"], p["pid"]),
        )
        if exist:
            cur.execute(
                "UPDATE basket_team_member SET membership_status=1, is_current=1, role_type=%s, "
                "active_member_slot=%s, division_id=%s, update_time=%s WHERE id=%s",
                (role, slot, DIVISION_ID, NOW, exist["id"]),
            )
            continue
        # clear conflicting active slot if any
        cur.execute(
            "UPDATE basket_team_member SET active_member_slot=NULL, is_current=0, membership_status=0, update_time=%s "
            "WHERE active_member_slot=%s AND is_delete=0",
            (NOW, slot),
        )
        mid = 2099220811210000000 + (t["team_id"] % 1000) * 10 + i
        mno = f"MB{mid}"
        cur.execute(
            "INSERT INTO basket_team_member "
            "(id, membership_no, team_id, division_id, member_id, player_id, role_type, roster_type, "
            "join_source, membership_status, is_current, active_member_slot, join_time, create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,%s,%s,'OFFICIAL',%s,1,1,%s,%s,%s,%s,0)",
            (mid, mno, t["team_id"], DIVISION_ID, p["mid"], p["pid"], role, join_source, slot, NOW, NOW, NOW),
        )
    print(f"members ok {t['name']}")


def ensure_grant(cur, t):
    cap = t["players"][0]
    auk = f"{DIVISION_ID}_{cap['pid']}"
    row = one(cur, "SELECT id FROM basket_captain_grant WHERE id=%s", (t["grant_id"],))
    if row:
        cur.execute(
            "UPDATE basket_captain_grant SET division_id=%s, season_id=%s, team_id=%s, player_id=%s, member_id=%s, "
            "grant_status='ACTIVE', active_unique_key=%s, valid_from=%s, valid_to='2026-08-31 00:00:00', "
            "granted_by=1, remark=%s, grant_source='MANUAL', update_time=%s, is_delete=0 WHERE id=%s",
            (DIVISION_ID, SEASON_ID, t["team_id"], cap["pid"], cap["mid"], auk, NOW, MARKER, NOW, t["grant_id"]),
        )
        print(f"grant update {t['grant_id']}")
        return
    # clear other active key conflict
    cur.execute(
        "UPDATE basket_captain_grant SET active_unique_key=NULL, grant_status='REVOKED', update_time=%s "
        "WHERE active_unique_key=%s AND id<>%s",
        (NOW, auk, t["grant_id"]),
    )
    cur.execute(
        "INSERT INTO basket_captain_grant "
        "(id, division_id, season_id, team_id, player_id, member_id, grant_status, active_unique_key, "
        "valid_from, valid_to, granted_by, remark, grant_source, create_time, update_time, is_delete) "
        "VALUES (%s,%s,%s,%s,%s,%s,'ACTIVE',%s,%s,'2026-08-31 00:00:00',1,%s,'MANUAL',%s,%s,0)",
        (t["grant_id"], DIVISION_ID, SEASON_ID, t["team_id"], cap["pid"], cap["mid"], auk, NOW, MARKER, NOW, NOW),
    )
    print(f"grant insert {t['grant_id']}")


def ensure_registration(cur, t):
    cap = t["players"][0]
    row = one(cur, "SELECT id FROM basket_registration WHERE id=%s", (t["reg_id"],))
    if not row:
        # also skip if team already has approved reg
        exist = one(
            cur,
            "SELECT id FROM basket_registration WHERE division_id=%s AND team_id=%s AND is_delete=0 "
            "AND registration_status='APPROVED' LIMIT 1",
            (DIVISION_ID, t["team_id"]),
        )
        if exist:
            t["reg_id"] = exist["id"]
            print(f"reg reuse {t['reg_id']} for {t['name']}")
        else:
            cur.execute(
                "INSERT INTO basket_registration "
                "(id, season_id, division_id, registration_mode, registration_source, team_id, captain_player_id, "
                "captain_member_id, registration_status, lineup_count, submit_time, approve_time, audit_user_id, "
                "create_time, update_time, is_delete) "
                "VALUES (%s,%s,%s,'TEAM','TEAM_DIRECT',%s,%s,%s,'APPROVED',5,%s,%s,1,%s,%s,0)",
                (t["reg_id"], SEASON_ID, DIVISION_ID, t["team_id"], cap["pid"], cap["mid"], NOW, NOW, NOW, NOW),
            )
            print(f"reg insert {t['reg_id']}")
    else:
        cur.execute(
            "UPDATE basket_registration SET registration_status='APPROVED', lineup_count=5, approve_time=%s, "
            "audit_user_id=1, update_time=%s, is_delete=0 WHERE id=%s",
            (NOW, NOW, t["reg_id"]),
        )
        print(f"reg update {t['reg_id']}")

    # registration players
    for i, p in enumerate(t["players"]):
        rp = one(
            cur,
            "SELECT id FROM basket_registration_player WHERE registration_id=%s AND player_id=%s AND is_delete=0",
            (t["reg_id"], p["pid"]),
        )
        if rp:
            continue
        rpid = 1300 + (t["reg_id"] - 253) * 10 + i
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
    print(f"reg players ok {t['name']}")


def ensure_event(cur, t):
    cap = t["players"][0]
    et = one(cur, "SELECT id FROM basket_event_team WHERE id=%s", (t["et_id"],))
    if not et:
        exist = one(
            cur,
            "SELECT id FROM basket_event_team WHERE division_id=%s AND source_team_id=%s AND is_delete=0",
            (DIVISION_ID, t["team_id"]),
        )
        if exist:
            t["et_id"] = exist["id"]
            print(f"et reuse {t['et_id']}")
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
            print(f"et insert {t['et_id']}")
    else:
        cur.execute(
            "UPDATE basket_event_team SET registration_id=%s, team_name_snapshot=%s, team_short_name=%s, "
            "event_team_status='ACTIVE', update_time=%s, is_delete=0 WHERE id=%s",
            (t["reg_id"], t["name"], t["short"], NOW, t["et_id"]),
        )

    for i, p in enumerate(t["players"]):
        ep = one(
            cur,
            "SELECT id FROM basket_event_player WHERE event_team_id=%s AND source_player_id=%s AND is_delete=0",
            (t["et_id"], p["pid"]),
        )
        if ep:
            continue
        epid = 1100 + (t["et_id"] - 227) * 10 + i
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
    print(f"event players ok {t['name']}")


def main():
    print("=== PROD SQL seed 15p/3t division 165 ===")
    c = conn()
    cur = c.cursor()
    try:
        # capacity
        cur.execute("UPDATE basket_division SET max_team_count=GREATEST(max_team_count, 4) WHERE id=%s", (DIVISION_ID,))
        for t in TEAMS:
            # verify accounts exist
            for p in t["players"]:
                a = one(cur, "SELECT id FROM basket_member_account WHERE id=%s AND is_delete=0", (p["mid"],))
                if not a:
                    raise RuntimeError(f"missing account {p['email']} id={p['mid']}")
            ensure_team(cur, t)
            ensure_members(cur, t)
            ensure_grant(cur, t)
            ensure_registration(cur, t)
            ensure_event(cur, t)
        c.commit()
    except Exception:
        c.rollback()
        raise
    finally:
        c.close()

    # verify
    c = conn()
    cur = c.cursor()
    cur.execute(
        "SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0",
        (DIVISION_ID,),
    )
    et = cur.fetchone()["c"]
    cur.execute(
        "SELECT COUNT(*) c FROM basket_event_player ep "
        "JOIN basket_event_team et ON et.id=ep.event_team_id "
        "WHERE et.division_id=%s AND ep.is_delete=0 AND et.is_delete=0",
        (DIVISION_ID,),
    )
    ep = cur.fetchone()["c"]
    cur.execute(
        "SELECT id, registration_status, CAST(team_id AS CHAR) team_id FROM basket_registration "
        "WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (DIVISION_ID,),
    )
    regs = cur.fetchall()
    cur.execute(
        "SELECT CAST(id AS CHAR) id, team_name_snapshot FROM basket_event_team WHERE division_id=%s AND is_delete=0",
        (DIVISION_ID,),
    )
    ets = cur.fetchall()
    c.close()

    out = {
        "db": DB["host"],
        "divisionId": DIVISION_ID,
        "password": "111111aa",
        "eventTeamCount": et,
        "eventPlayerCount": ep,
        "registrations": regs,
        "eventTeams": ets,
        "teams": [
            {
                "name": t["name"],
                "teamId": str(t["team_id"]),
                "regId": t["reg_id"],
                "eventTeamId": t["et_id"],
                "emails": [p["email"] for p in t["players"]],
            }
            for t in TEAMS
        ],
        "marker": MARKER,
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(f"event_team={et} event_player={ep}")
    print("regs", regs)
    print("ets", ets)
    print(f"wrote {OUT}")
    print("=== DONE ===")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())