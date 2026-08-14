# -*- coding: utf-8 -*-
"""Seed 20 TEAM / 60 players into PROD CNPA Arena 2026 S5 PS/Xbox 3V3 (divisionId=174)."""
from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any

import pymysql

ADMIN = "http://111.229.241.29:8080"
WEB = "http://111.229.241.29:8881"
REDIS_HOST = "106.55.61.223"
DB = dict(
    host="111.229.241.29",
    port=3306,
    user="root",
    password="zlzcnpa@@qaq",
    database="basketball",
    charset="utf8mb4",
)
DIVISION_ID = "174"
MARKER = "PROD_ARENA_S5_20T_60P_20260814"
MEMBER_PASS = "111111aa"
PASS_HASH = "$2a$10$Kq/KEI3FV.cxoKnhm1O/ceh7Q5.8Pm3Vhpb30zzRPTqDEMYzTCOu."
ID_BASE = 2099220814210000000
POSITIONS = ["PG", "SG", "SF"]
OUT = Path(__file__).resolve().parent / "seed_prod_arena_s5_20t_60p_result.json"

TEAM_DEFS = [
    {"code": "T01", "name": "S5 WOLVES", "short": "WLV"},
    {"code": "T02", "name": "S5 TIGERS", "short": "TGR"},
    {"code": "T03", "name": "S5 SHARKS", "short": "SHK"},
    {"code": "T04", "name": "S5 EAGLES", "short": "EGL"},
    {"code": "T05", "name": "S5 DRAGONS", "short": "DRG"},
    {"code": "T06", "name": "S5 PANTHERS", "short": "PNT"},
    {"code": "T07", "name": "S5 RAPTORS", "short": "RAP"},
    {"code": "T08", "name": "S5 KNIGHTS", "short": "KNT"},
    {"code": "T09", "name": "S5 COBRAS", "short": "CBR"},
    {"code": "T10", "name": "S5 FALCONS", "short": "FAL"},
    {"code": "T11", "name": "S5 VIPERS", "short": "VIP"},
    {"code": "T12", "name": "S5 STORM", "short": "STM"},
    {"code": "T13", "name": "S5 THUNDER", "short": "THN"},
    {"code": "T14", "name": "S5 BLAZE", "short": "BLZ"},
    {"code": "T15", "name": "S5 PHANTOM", "short": "PHN"},
    {"code": "T16", "name": "S5 TITANS", "short": "TTN"},
    {"code": "T17", "name": "S5 HORNETS", "short": "HRN"},
    {"code": "T18", "name": "S5 COMETS", "short": "CMT"},
    {"code": "T19", "name": "S5 SPARTANS", "short": "SPT"},
    {"code": "T20", "name": "S5 LEGENDS", "short": "LGD"},
]


def as_id(v: Any) -> str:
    return str(v)


def _req_once(url: str, method: str = "GET", body: dict | None = None, token: str | None = None) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            out = json.loads(raw) if raw else {}
            out["_http_status"] = resp.status
            return out
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            out = json.loads(raw) if raw else {}
        except Exception:
            out = {"msg": raw}
        out["code"] = out.get("code", e.code)
        out["_http_status"] = e.code
        return out
    except Exception as e:
        return {"code": 502, "msg": str(e), "_http_status": 502}


def req(url: str, method: str = "GET", body: dict | None = None, token: str | None = None) -> dict:
    last = None
    for i in range(6):
        out = _req_once(url, method, body, token)
        if out.get("_http_status") != 502 and out.get("code") != 502:
            return out
        last = out
        time.sleep(1.5 * (i + 1))
    return last or {"code": 502, "msg": "gateway retry exhausted", "_http_status": 502}


def extract_id(r: dict) -> str:
    data = r.get("data")
    if isinstance(data, dict):
        for k in ("id", "teamId", "grantId", "registrationId"):
            if data.get(k) is not None:
                return as_id(data[k])
    if isinstance(data, (int, str)) and str(data).isdigit():
        return as_id(data)
    raise RuntimeError(f"cannot extract id: {r}")


def db_conn():
    return pymysql.connect(cursorclass=pymysql.cursors.DictCursor, autocommit=False, connect_timeout=15, ssl_disabled=True, **DB)


def db_query(sql: str, args=None) -> list[dict]:
    conn = db_conn()
    try:
        cur = conn.cursor()
        cur.execute(sql, args or ())
        return list(cur.fetchall())
    finally:
        conn.close()


def admin_login() -> str:
    last = None
    for i in range(8):
        r = req(f"{ADMIN}/login", "POST", {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
        if r.get("code") == 200 and r.get("token"):
            return r["token"]
        last = r
        time.sleep(2 * (i + 1))
    raise RuntimeError(f"admin login failed: {last}")


def clear_login_locks(emails: list[str]) -> None:
    try:
        import redis
        r = redis.Redis(host=REDIS_HOST, port=6379, db=0, socket_connect_timeout=3)
        for email in emails:
            k = f"member:login:fail:{email}"
            if r.exists(k):
                r.delete(k)
    except Exception:
        pass


def web_login(email: str, retries: int = 5) -> str:
    last = None
    for _ in range(retries):
        clear_login_locks([email])
        r = req(f"{WEB}/basket/member/auth/login/password", "POST", {"email": email, "password": MEMBER_PASS})
        if r.get("code") == 200:
            return r["data"]["token"]
        last = r
        time.sleep(1.2)
    raise RuntimeError(f"web login failed {email}: {last}")


def ensure_platform_accounts(cur, player_id: int, game_id: str, now: str, slot: int) -> None:
    for pidx, platform in enumerate(("PC", "PS", "PS_XBOX")):
        cur.execute(
            "SELECT id FROM basket_player_platform_account WHERE player_id=%s AND platform=%s AND is_delete=0 LIMIT 1",
            (player_id, platform),
        )
        if cur.fetchone():
            continue
        plat_id = ID_BASE + 50_000 + int(slot) * 10 + pidx
        cur.execute(
            "INSERT INTO basket_player_platform_account "
            "(id, player_id, platform, display_game_id, normalized_game_id, account_status, is_primary, "
            "approved_by, approved_time, effective_time, create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,1,1,1,%s,%s,%s,%s,0)",
            (plat_id, player_id, platform, game_id, game_id.lower(), now, now, now, now),
        )


def extend_registration_window() -> None:
    rows = db_query(
        "SELECT registration_end_time, division_status FROM basket_division WHERE id=%s",
        (int(DIVISION_ID),),
    )
    if not rows:
        raise RuntimeError("division 174 not found")
    end = rows[0]["registration_end_time"]
    conn = db_conn()
    try:
        cur = conn.cursor()
        cur.execute(
            "UPDATE basket_division SET registration_end_time=%s, division_status='REGISTRATION_OPEN' WHERE id=%s",
            ("2026-08-31 23:59:59", int(DIVISION_ID)),
        )
        conn.commit()
        print(f"extended registration_end_time {end} -> 2026-08-31 23:59:59")
    finally:
        conn.close()


def seed_org_pool(team_defs: list[dict]) -> list[dict]:
    teams: list[dict] = []
    conn = db_conn()
    cur = conn.cursor()
    try:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        for ti, td in enumerate(team_defs):
            player_ids, member_ids, emails = [], [], []
            for pi in range(3):
                pname = f"{td['short']}{pi + 1:02d}"
                member_id = ID_BASE + ti * 10 + pi
                player_id = ID_BASE + 1_000 + ti * 10 + pi
                email = f"s5a20_{td['code'].lower()}p{pi + 1:02d}@cnpa.smoke"
                cur.execute("SELECT id FROM basket_member_account WHERE id=%s", (member_id,))
                if not cur.fetchone():
                    cur.execute(
                        "INSERT INTO basket_member_account "
                        "(id, account_no, username, password_hash, email, email_verified, account_status, "
                        "register_source, register_terminal, create_time, update_time, is_delete) "
                        "VALUES (%s,%s,%s,%s,%s,1,1,'WEB','PC',%s,%s,0)",
                        (member_id, f"MBR{member_id}", f"s5a20_{td['code'].lower()}p{pi + 1:02d}", PASS_HASH, email, now, now),
                    )
                    cur.execute(
                        "INSERT INTO basket_member_profile "
                        "(id, member_id, nickname, gender, profile_status, create_time, update_time, is_delete) "
                        "VALUES (%s,%s,%s,0,1,%s,%s,0)",
                        (member_id, member_id, pname, now, now),
                    )
                    cur.execute(
                        "INSERT INTO basket_player_profile "
                        "(id, member_id, player_no, game_terminal, game_account, game_nickname, position_code, "
                        "certification_status, player_status, certified_time, profile_visible, create_time, update_time, is_delete) "
                        "VALUES (%s,%s,%s,'PS_XBOX',%s,%s,%s,3,1,%s,1,%s,%s,0)",
                        (player_id, member_id, f"CNPA-P-S5A20-{td['code']}{pi + 1:02d}", pname, pname, POSITIONS[pi], now, now, now),
                    )
                else:
                    cur.execute("SELECT email FROM basket_member_account WHERE id=%s", (member_id,))
                    email = cur.fetchone()["email"]
                game_id = f"s5a20{td['code'].lower()}p{pi + 1:02d}"
                ensure_platform_accounts(cur, player_id, game_id, now, slot=ti * 10 + pi)
                player_ids.append(str(player_id))
                member_ids.append(str(member_id))
                emails.append(email)
            teams.append(
                {
                    "code": td["code"],
                    "name": td["name"],
                    "short": td["short"],
                    "captain_email": emails[0],
                    "captain_player_id": player_ids[0],
                    "player_ids": player_ids,
                    "member_ids": member_ids,
                    "emails": emails,
                }
            )
        conn.commit()
        return teams
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def create_captain_grant(token: str, player_id: str) -> dict:
    vf = (datetime.now() - timedelta(hours=1)).strftime("%Y-%m-%d %H:%M:%S")
    return req(
        f"{ADMIN}/basket/v4/captain-grant",
        "POST",
        {"divisionId": DIVISION_ID, "playerId": player_id, "remark": MARKER, "validFrom": vf},
        token,
    )


def create_team_via_web(token: str, name: str, short: str) -> dict:
    return req(
        f"{WEB}/basket/team/create",
        "POST",
        {"divisionId": DIVISION_ID, "teamName": name, "shortName": short, "publicStatus": 1, "description": MARKER},
        token,
    )


def submit_cert(token: str, team_id: str) -> dict:
    return req(f"{WEB}/basket/team/{team_id}/submit-certification", "POST", {}, token)


def approve_team(token: str, team_id: str) -> dict:
    req(f"{ADMIN}/basket/teamAudit/{team_id}/start-review", "POST", {}, token)
    return req(f"{ADMIN}/basket/teamAudit/{team_id}/approve", "POST", {"auditRemark": MARKER}, token)


def ensure_team_approved(admin: str, captain_token: str, team_id: str) -> None:
    rows = db_query("SELECT CAST(id AS CHAR) id, audit_status FROM basket_team WHERE id=%s", (int(team_id),))
    if not rows:
        raise RuntimeError(f"team not found {team_id}")
    audit = int(rows[0]["audit_status"] or -1)
    if audit == 1:
        return
    if audit in (2, 4):
        sr = submit_cert(captain_token, team_id)
        if sr.get("code") != 200:
            raise RuntimeError(f"submit certification failed: {sr}")
    ar = approve_team(admin, team_id)
    if ar.get("code") != 200:
        ar2 = req(f"{ADMIN}/basket/teamAudit/{team_id}/approve", "POST", {"auditRemark": MARKER}, admin)
        if ar2.get("code") != 200:
            raise RuntimeError(f"approve failed: {ar} / {ar2}")


def invite_and_accept(captain_token: str, invitee_token: str, team_id: str, invitee_player_id: str) -> dict:
    ir = req(
        f"{WEB}/basket/team/{team_id}/invitation",
        "POST",
        {"inviteePlayerId": invitee_player_id, "message": MARKER},
        captain_token,
    )
    if ir.get("code") != 200:
        mine = req(f"{WEB}/basket/team/invitation/mine/page?pageNum=1&pageSize=50", token=invitee_token)
        records = ((mine.get("data") or {}).get("records") or [])
        hit = next((r for r in records if as_id(r.get("teamId")) == as_id(team_id) and r.get("canRespond")), None)
        if not hit:
            return ir
        inv_id = as_id(hit.get("invitationId") or hit.get("id"))
        return req(f"{WEB}/basket/team/invitation/{inv_id}/respond", "PUT", {"accept": True, "action": "ACCEPT"}, invitee_token)
    data = ir.get("data") or {}
    inv_id = as_id(data.get("invitationId") or data.get("id") or extract_id(ir))
    return req(f"{WEB}/basket/team/invitation/{inv_id}/respond", "PUT", {"accept": True, "action": "ACCEPT"}, invitee_token)


def register_team(web_token: str, team_id: str, player_ids: list[str]) -> dict:
    body = {"divisionId": DIVISION_ID, "teamId": team_id, "playerIds": [as_id(p) for p in player_ids]}
    sr = req(f"{WEB}/basket/v4/player/registration/save", "POST", body, web_token)
    if sr.get("code") != 200:
        return sr
    data = sr.get("data")
    if isinstance(data, dict) and (data.get("registrationId") or data.get("id")):
        reg_id = as_id(data.get("registrationId") or data.get("id"))
    else:
        reg_id = extract_id(sr)
    return req(f"{WEB}/basket/v4/player/registration/submit", "POST", {"registrationId": reg_id}, web_token)


def approve_all_registrations(token: str) -> int:
    r = req(f"{ADMIN}/basket/v4/registration-audit/list?divisionId={DIVISION_ID}&pageNum=1&pageSize=100", token=token)
    n = 0
    for row in r.get("rows") or []:
        if row.get("registrationStatus") == "PENDING_REVIEW":
            ar = req(f"{ADMIN}/basket/v4/registration-audit/{row['registrationId']}/approve", "POST", {}, token)
            if ar.get("code") == 200:
                n += 1
    return n


def main() -> int:
    print("=== PROD Arena S5 PS/Xbox 3V3 · seed 20 teams / 60 players ===")
    div = db_query(
        "SELECT id, division_name, division_status, max_team_count, lineup_min_count, registration_end_time FROM basket_division WHERE id=%s",
        (int(DIVISION_ID),),
    )
    if not div:
        raise RuntimeError("division 174 not found on prod")
    print("division:", div[0])
    print("formalCap max_team_count stays", div[0]["max_team_count"])
    extend_registration_window()

    admin = admin_login()
    print("admin login ok")
    teams = seed_org_pool(TEAM_DEFS)
    print(f"seeded {sum(len(t['player_ids']) for t in teams)} accounts/players")

    result_teams = []
    for td in teams:
        cap_tok = web_login(td["captain_email"])
        gr = create_captain_grant(admin, td["captain_player_id"])
        if gr.get("code") != 200:
            msg = str(gr.get("msg") or "")
            if "已" not in msg:
                raise RuntimeError(f"grant failed {td['name']}: {gr}")
            grant_id = None
        else:
            grant_id = extract_id(gr)
        print(f"grant {td['name']} => {grant_id}")

        cr = create_team_via_web(cap_tok, td["name"], td["short"])
        rows = db_query(
            "SELECT CAST(id AS CHAR) id FROM basket_team "
            "WHERE division_id=%s AND captain_player_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
            (int(DIVISION_ID), int(td["captain_player_id"])),
        )
        if cr.get("code") != 200 and not rows:
            raise RuntimeError(f"create team failed {td['name']}: {cr}")
        team_id = as_id(rows[0]["id"]) if rows else extract_id(cr)
        ensure_team_approved(admin, cap_tok, team_id)
        print(f"team {td['name']} id={team_id} approved")

        for pid, email in zip(td["player_ids"][1:], td["emails"][1:]):
            already = db_query(
                "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND is_delete=0 AND membership_status=1 LIMIT 1",
                (int(team_id), int(pid)),
            )
            if already:
                continue
            last = None
            for attempt in range(6):
                inv_tok = web_login(email)
                if attempt and attempt % 2 == 0:
                    cap_tok = web_login(td["captain_email"])
                ir = invite_and_accept(cap_tok, inv_tok, team_id, pid)
                if ir.get("code") == 200:
                    last = ir
                    break
                last = ir
                time.sleep(1.2 * (attempt + 1))
            if not last or last.get("code") != 200:
                already = db_query(
                    "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND is_delete=0 AND membership_status=1 LIMIT 1",
                    (int(team_id), int(pid)),
                )
                if not already:
                    raise RuntimeError(f"invite/accept failed {email}: {last}")
        print(f"roster ready {td['name']}")

        cap_tok = web_login(td["captain_email"])
        existing_reg = db_query(
            "SELECT id, registration_status FROM basket_registration WHERE division_id=%s AND team_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
            (int(DIVISION_ID), int(team_id)),
        )
        if existing_reg and existing_reg[0]["registration_status"] in ("PENDING_REVIEW", "APPROVED", "SUBMITTED"):
            print(f"register skip existing {td['name']} status={existing_reg[0]['registration_status']}")
        else:
            last = None
            for attempt in range(5):
                rr = register_team(cap_tok, team_id, td["player_ids"])
                if rr.get("code") == 200:
                    last = rr
                    break
                last = rr
                time.sleep(1.2 * (attempt + 1))
                cap_tok = web_login(td["captain_email"])
            if not last or last.get("code") != 200:
                raise RuntimeError(f"register failed {td['name']}: {last}")
        print(f"registered {td['name']}")
        result_teams.append(
            {
                "name": td["name"],
                "short": td["short"],
                "teamId": team_id,
                "grantId": grant_id,
                "captainEmail": td["captain_email"],
                "emails": td["emails"],
                "playerIds": td["player_ids"],
            }
        )
        time.sleep(0.8)

    approved = approve_all_registrations(admin)
    print(f"approved registrations: {approved}")
    regs = db_query(
        "SELECT registration_status, participation_role, COUNT(*) c FROM basket_registration "
        "WHERE division_id=%s AND is_delete=0 GROUP BY registration_status, participation_role",
        (int(DIVISION_ID),),
    )
    print("registration summary:", regs)
    members = db_query(
        "SELECT COUNT(*) c FROM basket_team_member tm JOIN basket_team t ON t.id=tm.team_id "
        "WHERE t.division_id=%s AND tm.is_delete=0 AND t.is_delete=0 AND tm.membership_status=1",
        (int(DIVISION_ID),),
    )[0]["c"]
    print(f"active members={members}")

    out = {
        "db": DB["host"],
        "divisionId": DIVISION_ID,
        "password": MEMBER_PASS,
        "marker": MARKER,
        "formalCap": 16,
        "approvedRegistrations": approved,
        "activeMembers": members,
        "teams": result_teams,
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"wrote {OUT}")
    for t in result_teams:
        print(f"- {t['name']} teamId={t['teamId']} captain={t['captainEmail']}")
    print("=== DONE ===")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())