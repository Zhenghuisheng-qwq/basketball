#!/usr/bin/env python3
"""CNPA Boss Review Demo Data — reuse R2-4 MatchSeries business chain with clean names.

Creates a NEW season/division (does NOT touch Division 118).
"""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta
from typing import Any

import pymysql

ADMIN = "http://localhost:8080"
WEB = "http://localhost:8881"
DB = dict(host="192.168.31.48", port=3306, user="root", password="123456", database="basketball", charset="utf8mb4")
INTERNAL_MARK = "BOSS_REVIEW_2026"
MEMBER_PASS = "111111aa"
PASS_HASH = "$2a$10$Kq/KEI3FV.cxoKnhm1O/ceh7Q5.8Pm3Vhpb30zzRPTqDEMYzTCOu."
TEMPLATE_ID = 3
SERIES_ID = 1
ID_BASE = 2099100811000000000

# Existing logo assets (known good COS files)
LOGOS = [
    2083607366829346818,
    2083654311425884161,
    2083607291533201409,
    2081047953174171650,
    2081067518675390465,
    2083607240383664129,
    2081040245142446081,
    2082478424710045697,
]
AVATARS = [
    2081079552771129346,
    2082478424710045697,
]

TEAM_DEFS = [
    {"code": "RNG", "name": "RNG", "short": "RNG", "players": ["RNG_Ace", "RNG_King", "RNG_Nova", "RNG_Ray", "RNG_Zero"]},
    {"code": "PHX", "name": "PHOENIX", "short": "PHX", "players": ["PHX_Fire", "PHX_Sky", "PHX_Wing", "PHX_Ember", "PHX_Flash"]},
    {"code": "TTN", "name": "TITAN", "short": "TTN", "players": ["TTN_Steel", "TTN_Forge", "TTN_Apex", "TTN_Core", "TTN_Bolt"]},
    {"code": "WLV", "name": "WOLVES", "short": "WLV", "players": ["WLV_Howl", "WLV_Fang", "WLV_Night", "WLV_Frost", "WLV_Hunt"]},
    {"code": "DRG", "name": "DRAGONS", "short": "DRG", "players": ["DRG_Flame", "DRG_Scale", "DRG_Roar", "DRG_Tide", "DRG_Spark"]},
    {"code": "KNT", "name": "KNIGHTS", "short": "KNT", "players": ["KNT_Guard", "KNT_Blade", "KNT_Shield", "KNT_Honor", "KNT_Crest"]},
    {"code": "STM", "name": "STORM", "short": "STM", "players": ["STM_Surge", "STM_Cloud", "STM_Thunder", "STM_Rain", "STM_Gale"]},
    {"code": "BLZ", "name": "BLAZE", "short": "BLZ", "players": ["BLZ_Heat", "BLZ_Spark", "BLZ_Ash", "BLZ_Inferno", "BLZ_Pulse"]},
]
POSITIONS = ["PG", "SG", "SF", "PF", "C"]

TIE_BREAK = [
    {"ruleCode": "WIN_COUNT", "priorityNo": 1, "enabled": 1},
    {"ruleCode": "HEAD_TO_HEAD", "priorityNo": 2, "enabled": 1},
    {"ruleCode": "POINT_DIFFERENCE", "priorityNo": 3, "enabled": 1},
    {"ruleCode": "TOTAL_POINTS", "priorityNo": 4, "enabled": 1},
    {"ruleCode": "MANUAL", "priorityNo": 5, "enabled": 1},
]

results: dict[str, str] = {}
TEAMS: list[dict[str, Any]] = []


def log(case: str, status: str, detail: str = "") -> None:
    results[case] = f"{status}" + (f" | {detail}" if detail else "")
    print(f"[{status}] {case}" + (f" — {detail}" if detail else ""))


def fail(case: str, detail: str = "") -> int:
    log(case, "FAIL", detail)
    print("\n=== SUMMARY (STOP) ===")
    for k, v in sorted(results.items()):
        print(f"{k}: {v}")
    return 1


def as_id(value: Any, label: str = "id") -> str:
    if isinstance(value, float):
        raise ValueError(f"{label}: float snowflake rejected ({value!r})")
    if isinstance(value, str):
        s = value.strip()
        if not s.isdigit():
            raise ValueError(f"{label}: non-numeric string {value!r}")
        return s
    if isinstance(value, int):
        return str(value)
    raise ValueError(f"{label}: unsupported type {type(value)}")


def json_body(obj: Any) -> bytes:
    def enc(o: Any):
        if isinstance(o, dict):
            return {k: enc(v) for k, v in o.items()}
        if isinstance(o, list):
            return [enc(v) for v in o]
        if isinstance(o, int) and abs(o) > 9007199254740991:
            return str(o)
        return o

    return json.dumps(enc(obj), ensure_ascii=False).encode()


def req(url: str, method: str = "GET", body: Any = None, token: str | None = None) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json_body(body) if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=180) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"code": e.code, "msg": raw}
        parsed["_http_status"] = e.code
        return parsed


def wait_server(url: str, sec: int = 60) -> bool:
    for _ in range(sec):
        try:
            urllib.request.urlopen(url, timeout=2)
            return True
        except Exception:
            time.sleep(1)
    return False


def admin_login() -> str:
    r = req(f"{ADMIN}/login", "POST", {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
    if r.get("code") != 200:
        raise RuntimeError(f"admin login failed: {r}")
    return r["token"]


def clear_login_locks(emails: list[str]) -> None:
    try:
        import redis

        r = redis.Redis(host="192.168.31.48", port=6379, db=0)
        for email in emails:
            k = f"member:login:fail:{email}"
            if r.exists(k):
                r.delete(k)
    except Exception:
        pass


def web_login(email: str, retries: int = 3) -> str:
    last = None
    for _ in range(retries):
        r = req(f"{WEB}/basket/member/auth/login/password", "POST", {"email": email, "password": MEMBER_PASS})
        if r.get("code") == 200:
            return r["data"]["token"]
        last = r
        time.sleep(1.5)
    raise RuntimeError(f"web login failed {email}: {last}")


def db_conn():
    return pymysql.connect(cursorclass=pymysql.cursors.DictCursor, autocommit=False, **DB)


def ensure_platform_accounts(cur, player_id: int, game_id: str, now: str, slot: int = 0) -> None:
    for pidx, platform in enumerate(("PC", "PS", "PS_XBOX")):
        cur.execute(
            "SELECT id FROM basket_player_platform_account "
            "WHERE player_id=%s AND platform=%s AND is_delete=0 LIMIT 1",
            (player_id, platform),
        )
        if cur.fetchone():
            continue
        plat_id = ID_BASE + 4_000_000 + slot * 10 + pidx
        cur.execute(
            "INSERT INTO basket_player_platform_account "
            "(id, player_id, platform, display_game_id, normalized_game_id, account_status, is_primary, "
            "approved_by, approved_time, effective_time, create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,1,1,1,%s,%s,%s,%s,0)",
            (plat_id, player_id, platform, game_id, game_id.lower(), now, now, now, now),
        )


def load_existing_demo_teams(cur) -> list[dict[str, Any]] | None:
    cur.execute("SELECT id, team_name, team_no, captain_player_id FROM basket_team WHERE team_no LIKE 'CNPA-BR-%' AND is_delete=0 ORDER BY id")
    rows = cur.fetchall()
    if len(rows) != 8:
        return None
    teams: list[dict[str, Any]] = []
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    for row in rows:
        cur.execute(
            "SELECT player_id FROM basket_team_member WHERE team_id=%s AND is_delete=0 ORDER BY CASE WHEN role_type='CAPTAIN' THEN 0 ELSE 1 END, id",
            (row["id"],),
        )
        player_ids = [str(r["player_id"]) for r in cur.fetchall()]
        cur.execute(
            "SELECT ma.email FROM basket_member_account ma "
            "JOIN basket_player_profile pp ON pp.member_id=ma.id WHERE pp.id=%s",
            (row["captain_player_id"],),
        )
        email_row = cur.fetchone()
        if not email_row:
            raise RuntimeError(f"missing captain email for team {row['team_name']}")
        for pidx, pid in enumerate(player_ids):
            cur.execute("SELECT game_nickname FROM basket_player_profile WHERE id=%s", (int(pid),))
            gn = (cur.fetchone() or {}).get("game_nickname") or f"player{str(pid)[-6:]}"
            # slot derived from team order * 10 + player index for stable unique platform ids
            team_idx = len(teams)
            ensure_platform_accounts(cur, int(pid), gn, now, slot=team_idx * 10 + pidx)
        teams.append(
            {
                "name": row["team_name"],
                "team_id": str(row["id"]),
                "captain_email": email_row["email"],
                "captain_player_id": str(row["captain_player_id"]),
                "player_ids": player_ids,
            }
        )
    return teams


def provision_demo_org() -> list[dict[str, Any]]:
    """Create 8 long-lived teams + 40 certified players with clean public names."""
    teams: list[dict[str, Any]] = []
    conn = db_conn()
    cur = conn.cursor()
    try:
        existing = load_existing_demo_teams(cur)
        if existing:
            conn.commit()
            return existing

        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        for ti, td in enumerate(TEAM_DEFS):
            team_id = ID_BASE + 2_000_000 + ti
            captain_member_id = ID_BASE + ti * 100 + 0
            captain_player_id = ID_BASE + 1_000_000 + ti * 100 + 0
            logo = LOGOS[ti % len(LOGOS)]
            player_ids: list[str] = []
            emails: list[str] = []

            for pi, pname in enumerate(td["players"]):
                member_id = ID_BASE + ti * 100 + pi
                player_id = ID_BASE + 1_000_000 + ti * 100 + pi
                email = f"boss_{td['code'].lower()}{pi + 1:02d}@cnpa.demo"
                account_no = f"MBR{member_id}"
                player_no = f"CNPA-P-BR-{td['code']}{pi + 1:02d}"
                avatar = AVATARS[pi % len(AVATARS)]
                game_id = f"{td['code'].lower()}{pi + 1:02d}"

                cur.execute(
                    "INSERT INTO basket_member_account "
                    "(id, account_no, username, password_hash, email, email_verified, account_status, "
                    "register_source, register_terminal, create_time, update_time, is_delete) "
                    "VALUES (%s,%s,%s,%s,%s,1,1,'WEB','PC',%s,%s,0)",
                    (member_id, account_no, f"boss_{td['code'].lower()}{pi + 1:02d}", PASS_HASH, email, now, now),
                )
                cur.execute(
                    "INSERT INTO basket_member_profile "
                    "(id, member_id, nickname, avatar_file_id, gender, profile_status, create_time, update_time, is_delete) "
                    "VALUES (%s,%s,%s,%s,0,1,%s,%s,0)",
                    (member_id, member_id, pname, avatar, now, now),
                )
                cur.execute(
                    "INSERT INTO basket_player_profile "
                    "(id, member_id, player_no, game_terminal, game_account, game_nickname, position_code, "
                    "certification_status, player_status, certified_time, profile_visible, create_time, update_time, is_delete) "
                    "VALUES (%s,%s,%s,'PC',%s,%s,%s,3,1,%s,1,%s,%s,0)",
                    (
                        player_id,
                        member_id,
                        player_no,
                        pname,
                        pname,
                        POSITIONS[pi % len(POSITIONS)],
                        now,
                        now,
                        now,
                    ),
                )
                ensure_platform_accounts(cur, player_id, game_id, now, slot=ti * 10 + pi)
                player_ids.append(str(player_id))
                emails.append(email)

            cur.execute(
                "INSERT INTO basket_team "
                "(id, team_no, team_name, short_name, english_name, logo_file_id, game_terminal, description, "
                "captain_member_id, captain_player_id, max_member_count, current_member_count, team_status, "
                "audit_status, public_status, create_time, update_time, is_delete) "
                "VALUES (%s,%s,%s,%s,%s,%s,'PC',%s,%s,%s,8,5,1,1,1,%s,%s,0)",
                (
                    team_id,
                    f"CNPA-BR-{td['code']}",
                    td["name"],
                    td["short"],
                    td["name"],
                    logo,
                    f"{INTERNAL_MARK} demo team",
                    captain_member_id,
                    captain_player_id,
                    now,
                    now,
                ),
            )
            for pi, player_id_s in enumerate(player_ids):
                member_id = ID_BASE + ti * 100 + pi
                player_id = int(player_id_s)
                membership_id = ID_BASE + 3_000_000 + ti * 100 + pi
                role = "CAPTAIN" if pi == 0 else "MEMBER"
                join_source = "CREATE" if pi == 0 else "APPLY"
                cur.execute(
                    "INSERT INTO basket_team_member "
                    "(id, membership_no, team_id, member_id, player_id, role_type, roster_type, join_source, "
                    "membership_status, is_current, active_member_slot, join_time, remark, create_time, update_time, is_delete) "
                    "VALUES (%s,%s,%s,%s,%s,%s,'OFFICIAL',%s,1,1,%s,%s,%s,%s,%s,0)",
                    (
                        membership_id,
                        f"MB{membership_id}",
                        team_id,
                        member_id,
                        player_id,
                        role,
                        join_source,
                        str(member_id),
                        now,
                        INTERNAL_MARK,
                        now,
                        now,
                    ),
                )
                cur.execute(
                    "UPDATE basket_player_profile SET current_team_id=%s WHERE id=%s",
                    (team_id, player_id),
                )

            teams.append(
                {
                    "name": td["name"],
                    "team_id": str(team_id),
                    "captain_email": emails[0],
                    "captain_player_id": str(captain_player_id),
                    "player_ids": player_ids,
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


def stage_payload(code, name, order, fmt, best_of, adv, next_code=None, adv_count=None):
    return {
        "stageCode": code,
        "stageName": name,
        "stageOrder": order,
        "stageType": "CUSTOM",
        "organizationMode": "UNGROUPED",
        "competitionFormat": fmt,
        "groupingMode": "NONE",
        "roundRobinCycles": 1,
        "bestOf": best_of,
        "winPoints": 2,
        "lossPoints": 1,
        "advancementType": adv,
        "pairingRule": "NONE",
        "requiresManualConfirmation": 0,
        **({"nextStageCode": next_code} if next_code else {}),
        **({"advancementCount": adv_count} if adv_count is not None else {}),
    }


def sorted_stages(snapshot: dict) -> list[dict]:
    return sorted(snapshot.get("stages") or [], key=lambda s: s.get("stageOrder") or 0)


def wipe_stages(token: str, division_id: str) -> None:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    stages = sorted_stages(snap.get("data") or {})
    for s in stages:
        p = stage_payload(
            s["stageCode"],
            s.get("stageName") or s["stageCode"],
            s.get("stageOrder") or 1,
            s.get("competitionFormat") or "ROUND_ROBIN",
            s.get("bestOf") or 1,
            "NONE",
        )
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "PUT", p, token)
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    for s in reversed(sorted_stages(snap.get("data") or {})):
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "DELETE", token=token)


def chain_update(token: str, division_id: str) -> None:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    stages = sorted_stages(snap.get("data") or {})
    for i, s in enumerate(stages):
        is_last = i == len(stages) - 1
        next_code = stages[i + 1]["stageCode"] if not is_last else None
        adv = "NONE" if is_last else ("WINNER_ADVANCE" if s.get("competitionFormat") == "SINGLE_ELIMINATION" else "OVERALL_TOP_N")
        adv_count = None if is_last else (4 if i == 0 else None)
        p = stage_payload(
            s["stageCode"],
            s.get("stageName"),
            i + 1,
            s.get("competitionFormat"),
            s.get("bestOf") or 1,
            adv,
            next_code,
            adv_count,
        )
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "PUT", p, token)


def list_matches(token: str, division_id: str, stage_id: int | None = None) -> list[dict]:
    url = f"{ADMIN}/basket/v4/match/list?divisionId={division_id}&pageNum=1&pageSize=500"
    if stage_id:
        url += f"&stageId={stage_id}"
    r = req(url, token=token)
    return r.get("rows") or []


def list_series(token: str, stage_id: int) -> list[dict]:
    r = req(f"{ADMIN}/basket/v4/match-series/stage/{stage_id}", token=token)
    return (r.get("data") or []) if r.get("code") == 200 else []


def get_series(token: str, series_id: int) -> dict:
    r = req(f"{ADMIN}/basket/v4/match-series/{series_id}", token=token)
    return r.get("data") or {}


def extract_id(resp: dict, key: str = "data") -> str:
    data = resp.get(key)
    if isinstance(data, dict):
        for k in ("id", "registrationId", "submissionId"):
            if data.get(k) is not None:
                return as_id(data[k], k)
        raise ValueError(f"no id in response data: {data}")
    return as_id(data, key)


def ensure_captain_grants(token: str, division_id: str) -> None:
    # Repair earlier grants created before team.division_id bind (team_id null)
    conn = db_conn()
    cur = conn.cursor()
    try:
        for t in TEAMS:
            cur.execute(
                "UPDATE basket_captain_grant SET team_id=%s, update_time=NOW() "
                "WHERE division_id=%s AND player_id=%s AND is_delete=0 AND grant_status='ACTIVE'",
                (int(t["team_id"]), int(division_id), int(t["captain_player_id"])),
            )
        conn.commit()
    finally:
        conn.close()

    existing = req(f"{ADMIN}/basket/v4/captain-grant/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    active_players: set[str] = set()
    for row in existing.get("rows") or []:
        if row.get("grantStatus") != "ACTIVE" or row.get("playerId") is None:
            continue
        active_players.add(as_id(row["playerId"]))
    for t in TEAMS:
        pid = as_id(t["captain_player_id"])
        if pid in active_players:
            continue
        cg = {
            "divisionId": division_id,
            "teamId": as_id(t["team_id"]),
            "playerId": pid,
            "remark": INTERNAL_MARK,
        }
        r = req(f"{ADMIN}/basket/v4/captain-grant", "POST", cg, token)
        if r.get("code") != 200:
            msg = str(r.get("msg") or "")
            if "已" in msg or "存在" in msg:
                continue
            raise RuntimeError(f"captain grant failed {t['name']}: {r}")


def list_registrations(token: str, division_id: str) -> dict[str, str]:
    r = req(f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    out: dict[str, str] = {}
    for row in r.get("rows") or []:
        out[as_id(row.get("teamId"))] = row.get("registrationStatus") or row.get("status") or ""
    return out


def register_teams(division_id: str, web_tokens: dict[str, str], admin_token: str) -> int:
    existing = list_registrations(admin_token, division_id)
    submitted = 0
    for t in TEAMS:
        tid = as_id(t["team_id"])
        status = existing.get(tid)
        if status in ("APPROVED", "PENDING_REVIEW"):
            continue
        wt = web_tokens[tid]
        player_ids = [as_id(p) for p in t["player_ids"]]
        body = {"divisionId": division_id, "teamId": tid, "playerIds": player_ids}
        if status not in ("DRAFT", "REJECTED", "WITHDRAWN"):
            sr = req(f"{WEB}/basket/v4/player/registration/save", "POST", body, wt)
            if sr.get("code") != 200:
                raise RuntimeError(f"registration save failed {t['name']}: {sr}")
            reg_id = extract_id(sr, "data")
        else:
            rr = req(f"{WEB}/basket/v4/player/registration/my?divisionId={division_id}", token=wt)
            reg_id = as_id((rr.get("data") or {}).get("registrationId") or (rr.get("data") or {}).get("id"))
        tr = req(f"{WEB}/basket/v4/player/registration/submit", "POST", {"registrationId": reg_id}, wt)
        if tr.get("code") != 200:
            raise RuntimeError(f"registration submit failed {t['name']}: {tr}")
        submitted += 1
    return submitted


def approve_all_registrations(token: str, division_id: str) -> int:
    r = req(f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=50", token=token)
    n = 0
    for row in r.get("rows") or []:
        if row.get("registrationStatus") == "PENDING_REVIEW":
            ar = req(f"{ADMIN}/basket/v4/registration-audit/{row['registrationId']}/approve", "POST", {}, token)
            if ar.get("code") == 200:
                n += 1
    return n


def submit_and_approve_match(
    admin_token: str,
    web_token: str,
    match: dict,
    home_score: int,
    away_score: int,
    submit_event_team_id: Any,
) -> None:
    mid = match["id"]
    detail = req(
        f"{WEB}/basket/v4/player/match-submission/{mid}/detail?submitEventTeamId={submit_event_team_id}",
        token=web_token,
    )
    if detail.get("code") != 200:
        raise RuntimeError(f"submission detail failed match {mid}: {detail}")
    d = detail["data"]
    player_stats = []
    for i, ps in enumerate(d.get("playerStats") or []):
        # varied scoring so Season Leaders / Data Center are not flat
        base = 8 + (int(str(ps.get("eventPlayerId") or "0")[-2:]) % 17)
        own = as_id(ps.get("eventTeamId")) == as_id(submit_event_team_id)
        points = base + (6 if own else 2) + (i % 5)
        player_stats.append(
            {
                "eventPlayerId": ps["eventPlayerId"],
                "points": points,
                "rebound": 3 + (i % 7),
                "assist": 2 + (i % 6),
                "steal": 1 + (i % 3),
                "block": i % 2,
                "turnover": 1 + (i % 4),
            }
        )
    body = {
        "matchId": mid,
        "submitEventTeamId": submit_event_team_id,
        "homeScore": home_score,
        "awayScore": away_score,
        "teamStat": {
            "score": home_score if as_id(submit_event_team_id) == as_id(match.get("homeEventTeamId")) else away_score,
            "rebound": 28 + (home_score % 7),
            "assist": 14 + (away_score % 5),
            "steal": 5,
            "block": 2,
            "turnover": 9,
        },
        "playerStats": player_stats,
        "materialFileIds": [],
        "submitRemark": INTERNAL_MARK,
    }
    sr = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
    if sr.get("code") != 200:
        raise RuntimeError(f"save submission failed match {mid}: {sr}")
    sub_id = extract_id(sr, "data") if isinstance(sr.get("data"), dict) else as_id(sr["data"])
    tr = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id}, web_token)
    if tr.get("code") != 200:
        raise RuntimeError(f"submit failed match {mid}: {tr}")
    lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
    pending = next((x for x in (lr.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    if not pending:
        raise RuntimeError(f"no pending submission for match {mid}")
    ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/approve", "POST", {}, admin_token)
    if ar.get("code") != 200:
        raise RuntimeError(f"approve failed match {mid}: {ar}")


def resolve_manual_rank_if_needed(token: str, stage_id: str) -> None:
    st = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/standings", token=token)
    groups = st.get("data") or []
    if not groups:
        return
    pending = [r for r in (groups[0].get("rows") or []) if r.get("manualRankPending")]
    if not pending:
        return
    # Prefer configured display order: RNG..BLAZE when names match
    name_rank = {td["name"]: i + 1 for i, td in enumerate(TEAM_DEFS)}
    pending_sorted = sorted(pending, key=lambda r: name_rank.get(r.get("teamName") or "", 99))
    items = [{"eventTeamId": as_id(r["eventTeamId"]), "manualRankNo": i + 1} for i, r in enumerate(pending_sorted)]
    body = {"groupScopeKey": 0, "reason": INTERNAL_MARK, "items": items}
    mr = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/manual-rank", "POST", body, token)
    if mr.get("code") != 200:
        raise RuntimeError(f"manual rank failed: {mr}")


def confirm_stage_qualification(token: str, stage_id: str) -> None:
    cc = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/check-completion", "POST", token=token)
    if cc.get("code") != 200:
        raise RuntimeError(f"check-completion failed: {cc}")
    resolve_manual_rank_if_needed(token, stage_id)
    pv = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/qualification-preview", token=token)
    if pv.get("code") != 200:
        raise RuntimeError(f"qualification-preview failed: {pv}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
    if cf.get("code") != 200:
        raise RuntimeError(f"confirm-qualification failed: {cf}")


def finish_bo1_stage(admin_token: str, web_by_event: dict[str, str], division_id: str, stage_id: str) -> None:
    matches = list_matches(admin_token, division_id, int(stage_id))
    bo1 = [m for m in matches if not m.get("seriesId")]
    score_pool = [(72, 66), (81, 75), (68, 70), (77, 71), (84, 79), (69, 63), (73, 68)]
    for idx, m in enumerate(bo1):
        if m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home = m.get("homeEventTeamId")
        away = m.get("awayEventTeamId")
        hs, aws = score_pool[idx % len(score_pool)]
        # ensure home preference for ranking stability unless swapped score
        if hs < aws:
            hs, aws = aws, hs
        # slightly stagger times via admin is not required; scores stay realistic
        wt = web_by_event.get(as_id(home))
        if not wt:
            raise RuntimeError(f"no web token for event team {home}")
        submit_and_approve_match(admin_token, wt, m, hs, aws, home)


def finish_series_games(admin_token: str, web_by_event: dict, series_id: int, results_plan: list[str]) -> dict:
    s = get_series(admin_token, series_id)
    games = sorted(s.get("games") or [], key=lambda g: g.get("seriesGameNo") or 0)
    idx = 0
    score_pool = [(78, 72), (71, 76), (84, 80), (69, 74), (82, 77)]
    for game in games:
        if game.get("skipped"):
            continue
        if game.get("matchStatus") in ("FINISHED", "FORFEITED"):
            idx += 1
            continue
        if idx >= len(results_plan):
            break
        side = results_plan[idx]
        mid = game["matchId"]
        mr = req(f"{ADMIN}/basket/v4/match/{mid}", token=admin_token)
        m = mr.get("data") or {}
        home = m.get("homeEventTeamId")
        away = m.get("awayEventTeamId")
        a, b = score_pool[idx % len(score_pool)]
        if side == "HOME":
            hs, aws, submit_team = max(a, b), min(a, b), home
        else:
            hs, aws, submit_team = min(a, b), max(a, b), away
        wt = web_by_event.get(as_id(submit_team))
        submit_and_approve_match(admin_token, wt, m, hs, aws, submit_team)
        idx += 1
    return get_series(admin_token, series_id)


def create_season(token: str) -> str:
    body = {
        "seriesId": SERIES_ID,
        "seasonYear": 2026,
        "seasonName": "S10",
        "displayTitle": "CNPA League 2026 S10",
        "description": "CNPA League 正式演示赛季",
        "seasonStartTime": "2026-09-01 00:00:00",
        "seasonEndTime": "2026-11-30 23:59:59",
    }
    # avoid duplicate
    lst = req(f"{ADMIN}/basket/v4/season/list?seriesId={SERIES_ID}&pageNum=1&pageSize=100", token=token)
    for row in lst.get("rows") or []:
        if row.get("seasonName") == "S10" or row.get("displayTitle") == "CNPA League 2026 S10":
            sid = as_id(row["id"])
            if row.get("publishStatus") != "PUBLISHED":
                req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
            return sid
    r = req(f"{ADMIN}/basket/v4/season", "POST", body, token)
    if r.get("code") != 200:
        raise RuntimeError(f"create season failed: {r}")
    sid = as_id(r["data"] if not isinstance(r.get("data"), dict) else r["data"].get("id", r["data"]))
    pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
    if pr.get("code") != 200:
        raise RuntimeError(f"publish season failed: {pr}")
    return sid


def main() -> int:
    global TEAMS
    if not wait_server(ADMIN) or not wait_server(WEB):
        return fail("ENV", "Admin/Web not ready")
    try:
        admin_token = admin_login()
    except Exception as e:
        return fail("ENV", str(e))
    log("ENV", "PASS", "Admin/Web up")

    try:
        TEAMS = provision_demo_org()
        log("PROVISION_TEAMS", "PASS", f"teams={len(TEAMS)}")
    except Exception as e:
        return fail("PROVISION_TEAMS", str(e))

    emails = [t["captain_email"] for t in TEAMS]
    clear_login_locks(emails)

    try:
        season_id = create_season(admin_token)
        log("CREATE_SEASON", "PASS", f"id={season_id}")
    except Exception as e:
        return fail("CREATE_SEASON", str(e))

    reg_start = datetime(2026, 9, 1, 0, 0, 0)
    reg_end = datetime(2026, 9, 10, 23, 59, 59)
    match_start = datetime(2026, 9, 12, 0, 0, 0)
    match_end = datetime(2026, 11, 20, 23, 59, 59)
    div_body = {
        "seasonId": int(season_id),
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": "PC 5V5",
        "registrationMode": "TEAM",
        "formatType": "REGULAR_ROUND_ROBIN",
        "formatTemplateId": TEMPLATE_ID,
        "maxTeamCount": 8,
        "registrationStartTime": reg_start.strftime("%Y-%m-%d %H:%M:%S"),
        "registrationEndTime": reg_end.strftime("%Y-%m-%d %H:%M:%S"),
        "matchStartTime": match_start.strftime("%Y-%m-%d %H:%M:%S"),
        "matchEndTime": match_end.strftime("%Y-%m-%d %H:%M:%S"),
        "sortOrder": 1,
    }

    # reuse if already created
    dl = req(f"{ADMIN}/basket/v4/division/list?seasonId={season_id}&pageNum=1&pageSize=50", token=admin_token)
    existing = next((r for r in (dl.get("rows") or []) if r.get("divisionName") == "PC 5V5"), None)
    if existing:
        division_id = as_id(existing["id"])
        log("CREATE_DIVISION", "PASS", f"reuse id={division_id}")
    else:
        dr = req(f"{ADMIN}/basket/v4/division", "POST", div_body, admin_token)
        if dr.get("code") != 200:
            return fail("CREATE_DIVISION", str(dr))
        division_id = as_id(dr["data"] if not isinstance(dr.get("data"), dict) else dr["data"].get("id", dr["data"]))
        log("CREATE_DIVISION", "PASS", f"id={division_id}")

    # never touch 118
    if division_id == "118":
        return fail("SAFETY", "refusing to use division 118")

    # V2 writable team must bind division_id (null => treated as historical/legacy)
    try:
        conn = db_conn()
        cur = conn.cursor()
        for t in TEAMS:
            cur.execute(
                "UPDATE basket_team SET division_id=%s, game_terminal='PC', update_time=NOW() "
                "WHERE id=%s AND is_delete=0",
                (int(division_id), int(t["team_id"])),
            )
        conn.commit()
        conn.close()
        log("BIND_TEAM_DIVISION", "PASS", f"divisionId={division_id}")
    except Exception as e:
        return fail("BIND_TEAM_DIVISION", str(e))

    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=admin_token)
    stages = sorted_stages(snap.get("data") or {})
    if len(stages) >= 3 and all(stages[i].get("stageName") for i in range(3)):
        st1_id, st2_id, st3_id = int(stages[0]["id"]), int(stages[1]["id"]), int(stages[2]["id"])
        log("LOCK", "PASS", f"reuse stages={st1_id},{st2_id},{st3_id}")
    else:
        wipe_stages(admin_token, division_id)
        s1 = req(
            f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages",
            "POST",
            stage_payload("ST1", "常规赛", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "ST2", 4),
            admin_token,
        )
        s2 = req(
            f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages",
            "POST",
            stage_payload("ST2", "4强赛", 2, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "ST3"),
            admin_token,
        )
        s3 = req(
            f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages",
            "POST",
            stage_payload("ST3", "总决赛", 3, "SINGLE_ELIMINATION", 5, "NONE"),
            admin_token,
        )
        if min(s1.get("code", 0), s2.get("code", 0), s3.get("code", 0)) != 200:
            return fail("STAGES", f"{s1} | {s2} | {s3}")
        st1_id, st2_id, st3_id = int(s1["data"]), int(s2["data"]), int(s3["data"])
        chain_update(admin_token, division_id)
        req(
            f"{ADMIN}/basket/v4/divisions/{division_id}/tie-break-rules",
            "PUT",
            {"stageId": st1_id, "rules": TIE_BREAK},
            admin_token,
        )
        lr = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot/lock", "POST", token=admin_token)
        if lr.get("code") != 200:
            return fail("LOCK", str(lr))
        log("LOCK", "PASS", f"stages={st1_id},{st2_id},{st3_id}")

    orr = req(f"{ADMIN}/basket/v4/division/{division_id}/open-registration", "PUT", token=admin_token)
    if orr.get("code") != 200:
        msg = str(orr.get("msg") or "")
        if "已" in msg or "OPEN" in msg.upper() or "报名" in msg:
            log("OPEN_REG", "PASS", f"already open: {msg}")
        else:
            return fail("OPEN_REG", str(orr))
    else:
        log("OPEN_REG", "PASS")

    try:
        ensure_captain_grants(admin_token, division_id)
        log("CAPTAIN_GRANT", "PASS")
    except Exception as e:
        return fail("CAPTAIN_GRANT", str(e))

    web_tokens: dict[str, str] = {}
    for i, t in enumerate(TEAMS):
        if i:
            time.sleep(1.2)
        try:
            web_tokens[as_id(t["team_id"])] = web_login(t["captain_email"])
        except Exception as e:
            return fail("WEB_LOGIN", str(e))
    log("WEB_LOGIN", "PASS", f"n={len(web_tokens)}")

    try:
        submitted = register_teams(division_id, web_tokens, admin_token)
        n = approve_all_registrations(admin_token, division_id)
        etr = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=20", token=admin_token)
        et_cnt = len(etr.get("rows") or [])
        if et_cnt != 8:
            return fail("EVENT_TEAM", f"count={et_cnt}, submitted={submitted}, approved={n}")
        log("REGISTRATION", "PASS", f"submitted={submitted}, approved={n}")
    except Exception as e:
        return fail("REGISTRATION", str(e))

    cr = req(f"{ADMIN}/basket/v4/division/{division_id}/close-registration", "PUT", token=admin_token)
    if cr.get("code") != 200:
        return fail("CLOSE_REG", str(cr))
    log("CLOSE_REG", "PASS")

    for step, sid in (("INIT1", st1_id),):
        r = req(f"{ADMIN}/basket/v4/admin/stages/{sid}/initialize-teams", "POST", token=admin_token)
        if r.get("code") != 200:
            return fail(step, str(r))
    gen1 = req(f"{ADMIN}/basket/v4/admin/stages/{st1_id}/generate-schedule", "POST", token=admin_token)
    if gen1.get("code") != 200:
        return fail("STAGE1_GENERATE", str(gen1))
    pub1 = req(f"{ADMIN}/basket/v4/admin/stages/{st1_id}/publish-schedule", "POST", token=admin_token)
    if pub1.get("code") != 200:
        return fail("STAGE1_PUBLISH", str(pub1))
    m1 = list_matches(admin_token, division_id, st1_id)
    if len(m1) != 28:
        return fail("STAGE1_MATCH_COUNT", f"got {len(m1)}")
    log("STAGE1_SCHEDULE", "PASS", "matches=28")

    etr = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=20", token=admin_token)
    event_teams = {row["teamNameSnapshot"]: row for row in (etr.get("rows") or [])}
    web_by_event: dict[str, str] = {}
    for t in TEAMS:
        et = event_teams.get(t["name"])
        if not et:
            return fail("MAP_EVENT_TEAM", t["name"])
        web_by_event[as_id(et["id"])] = web_tokens[as_id(t["team_id"])]

    try:
        finish_bo1_stage(admin_token, web_by_event, division_id, str(st1_id))
        confirm_stage_qualification(admin_token, str(st1_id))
        log("STAGE1", "PASS")
    except Exception as e:
        return fail("STAGE1", str(e))

    req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/initialize-teams", "POST", token=admin_token)
    gen2 = req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/generate-schedule", "POST", token=admin_token)
    if gen2.get("code") != 200:
        return fail("STAGE2_GENERATE", str(gen2))
    pub2 = req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/publish-schedule", "POST", token=admin_token)
    if pub2.get("code") != 200:
        return fail("STAGE2_PUBLISH", str(pub2))
    series_list = list_series(admin_token, st2_id)
    if len(series_list) != 2:
        return fail("STAGE2_SERIES", f"got {len(series_list)}")
    log("STAGE2_SERIES", "PASS", "2 series")

    try:
        sa, sb = series_list[0], series_list[1]
        sa_final = finish_series_games(admin_token, web_by_event, sa["id"], ["HOME", "HOME"])
        if sa_final.get("seriesStatus") != "COMPLETED":
            return fail("BO3_2_0", str(sa_final))
        log("BO3_2_0", "PASS", f"series={sa['id']} winner={sa_final.get('winnerTeamName')}")
        sb_final = finish_series_games(admin_token, web_by_event, sb["id"], ["HOME", "AWAY", "HOME"])
        if sb_final.get("seriesStatus") != "COMPLETED":
            return fail("BO3_2_1", str(sb_final))
        log("BO3_2_1", "PASS", f"series={sb['id']} winner={sb_final.get('winnerTeamName')}")
        req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/check-completion", "POST", token=admin_token)
        cq = req(f"{ADMIN}/basket/v4/admin/stages/{st2_id}/confirm-qualification", "POST", token=admin_token)
        if cq.get("code") != 200:
            return fail("QUAL_CONFIRM", str(cq))
        log("QUAL_CONFIRM", "PASS")
    except Exception as e:
        return fail("BO3", str(e))

    req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/initialize-teams", "POST", token=admin_token)
    gen3 = req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/generate-schedule", "POST", token=admin_token)
    if gen3.get("code") != 200:
        return fail("STAGE3_GENERATE", str(gen3))
    pub3 = req(f"{ADMIN}/basket/v4/admin/stages/{st3_id}/publish-schedule", "POST", token=admin_token)
    if pub3.get("code") != 200:
        return fail("STAGE3_PUBLISH", str(pub3))
    s3_series = list_series(admin_token, st3_id)
    if len(s3_series) != 1 or s3_series[0].get("bestOf") != 5:
        return fail("BO5_SERIES", str(s3_series))
    try:
        s5 = finish_series_games(
            admin_token,
            web_by_event,
            s3_series[0]["id"],
            ["HOME", "AWAY", "HOME", "AWAY", "HOME"],
        )
        if s5.get("seriesStatus") != "COMPLETED" or s5.get("homeWinCount") != 3:
            return fail("BO5_3_2", str(s5))
        champion = s5.get("winnerTeamName") or ""
        champion_et = as_id(s5.get("winnerEventTeamId"))
        log("BO5_3_2", "PASS", f"champion={champion} eventTeamId={champion_et}")
    except Exception as e:
        return fail("BO5", str(e))

    # Finish division for historical public semantics
    fr = req(f"{ADMIN}/basket/v4/division/{division_id}/finish", "POST", {"reason": INTERNAL_MARK}, admin_token)
    if fr.get("code") == 200:
        log("DIVISION_FINISH", "PASS")
    else:
        log("DIVISION_FINISH", "PARTIAL", str(fr.get("msg") or fr))

    # Collect recommended IDs
    games = sorted(s5.get("games") or [], key=lambda g: g.get("seriesGameNo") or 0)
    final_match_id = next((g.get("matchId") for g in games if g.get("matchStatus") == "FINISHED"), None)
    # pick champion captain/source player
    et_detail = req(f"{ADMIN}/basket/v4/event-team/{champion_et}", token=admin_token)
    players = (et_detail.get("data") or {}).get("players") or []
    source_player = None
    for p in players:
        if p.get("isCaptain") in (1, True, "1"):
            source_player = p.get("sourcePlayerId")
            break
    if not source_player and players:
        source_player = players[0].get("sourcePlayerId")

    print("\n=== BOSS DEMO IDS ===")
    print(f"seriesId=1")
    print(f"seasonId={season_id}")
    print(f"divisionId={division_id}")
    print(f"stages={st1_id},{st2_id},{st3_id}")
    print(f"championEventTeamId={champion_et}")
    print(f"championName={champion}")
    print(f"sourcePlayerId={source_player}")
    print(f"finalMatchId={final_match_id}")
    print(f"url=/events/{season_id}?divisionId={division_id}")

    fails = [k for k, v in results.items() if not v.startswith("PASS") and not v.startswith("PARTIAL")]
    print("\n=== SUMMARY ===")
    for k, v in sorted(results.items()):
        print(f"{k}: {v}")
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main())
