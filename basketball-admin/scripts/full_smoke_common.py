#!/usr/bin/env python3
"""FULL_SMOKE_20260811 shared helpers — API-first, minimal SQL seed only for accounts/players."""
from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any

import pymysql

ADMIN = "http://127.0.0.1:8080"
WEB = "http://127.0.0.1:8881"
WEBUI = "http://127.0.0.1:5173"
DB = dict(
    host="192.168.31.48",
    port=3306,
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
)
REDIS_HOST = "192.168.31.48"
MARKER = "FULL_SMOKE_20260811"
MEMBER_PASS = "111111aa"
PASS_HASH = "$2a$10$Kq/KEI3FV.cxoKnhm1O/ceh7Q5.8Pm3Vhpb30zzRPTqDEMYzTCOu."
SERIES_ID = 1
TEMPLATE_RR = 3
TEMPLATE_GROUP = 4
TEMPLATE_BO1 = 5
ID_BASE = 2099110811000000000
LOGOS = [
    "2081040245142446081",
    "2081046848142958594",
    "2081011196244312065",
    "2081011198194663426",
    "2081013405434560513",
    "2081013407254888450",
    "2081019389284573186",
    "2081019391620800514",
]
POSITIONS = ["PG", "SG", "SF", "PF", "C"]
TIE_BREAK = [
    {"ruleCode": "WIN_COUNT", "priorityNo": 1, "enabled": 1},
    {"ruleCode": "HEAD_TO_HEAD", "priorityNo": 2, "enabled": 1},
    {"ruleCode": "POINT_DIFFERENCE", "priorityNo": 3, "enabled": 1},
    {"ruleCode": "TOTAL_POINTS", "priorityNo": 4, "enabled": 1},
    {"ruleCode": "MANUAL", "priorityNo": 5, "enabled": 1},
]
CTX_PATH = Path(__file__).resolve().parent / "full_smoke_20260811_context.json"
OUT_DIR = Path(__file__).resolve().parent


@dataclass
class Case:
    name: str
    expected: str
    actual: str = ""
    evidence: str = ""
    result: str = "PENDING"  # PASS / FAIL / SKIP / NOT_EXECUTED / PARTIAL


@dataclass
class Attempt:
    n: int
    status: str
    detail: str = ""


@dataclass
class SmokeBag:
    smoke: str
    attempts: list[Attempt] = field(default_factory=list)
    cases: list[Case] = field(default_factory=list)
    ids: dict[str, Any] = field(default_factory=dict)
    fixes: list[dict[str, Any]] = field(default_factory=list)

    def add(self, name: str, expected: str, actual: str, evidence: str, result: str) -> Case:
        c = Case(name, expected, actual, evidence, result)
        self.cases.append(c)
        mark = result
        print(f"[{mark}] {self.smoke}.{name} — {actual}" + (f" | {evidence}" if evidence else ""))
        return c

    def pass_(self, name: str, expected: str, actual: str = "", evidence: str = "") -> None:
        self.add(name, expected, actual or expected, evidence, "PASS")

    def fail(self, name: str, expected: str, actual: str, evidence: str = "") -> None:
        self.add(name, expected, actual, evidence, "FAIL")

    def skip(self, name: str, expected: str, reason: str) -> None:
        self.add(name, expected, reason, "", "NOT_EXECUTED")

    def to_dict(self) -> dict:
        return {
            "smoke": self.smoke,
            "attempts": [asdict(a) for a in self.attempts],
            "cases": [asdict(c) for c in self.cases],
            "ids": self.ids,
            "fixes": self.fixes,
            "pass": sum(1 for c in self.cases if c.result == "PASS"),
            "fail": sum(1 for c in self.cases if c.result == "FAIL"),
            "skip": sum(1 for c in self.cases if c.result in ("SKIP", "NOT_EXECUTED")),
        }


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


def req(url: str, method: str = "GET", body: Any = None, token: str | None = None, timeout: int = 180) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json_body(body) if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as resp:
            raw = resp.read().decode()
            parsed = json.loads(raw) if raw else {}
            parsed["_http_status"] = getattr(resp, "status", 200)
            return parsed
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"code": e.code, "msg": raw}
        parsed["_http_status"] = e.code
        return parsed
    except Exception as e:
        return {"code": -1, "msg": str(e), "_http_status": -1}


def upload_file(token: str, file_bytes: bytes, filename: str = "smoke_avatar.png", usage_type: str = "ACCOUNT_AVATAR") -> dict:
    boundary = "----FullSmokeBoundary7MA4YWxkTrZu0gW"
    parts = []
    parts.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"usageType\"\r\n\r\n{usage_type}\r\n".encode())
    parts.append(
        (
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\n"
            f"Content-Type: image/png\r\n\r\n"
        ).encode()
        + file_bytes
        + b"\r\n"
    )
    parts.append(f"--{boundary}--\r\n".encode())
    body = b"".join(parts)
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Authorization": f"Bearer {token}",
    }
    request = urllib.request.Request(f"{WEB}/basket/account/file/upload", data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=60) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return json.loads(raw)
        except Exception:
            return {"code": e.code, "msg": raw}


def wait_server(url: str, sec: int = 30) -> bool:
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

        r = redis.Redis(host=REDIS_HOST, port=6379, db=0, socket_connect_timeout=3)
        for email in emails:
            k = f"member:login:fail:{email}"
            if r.exists(k):
                r.delete(k)
    except Exception:
        pass


def web_login(email: str, retries: int = 4) -> str:
    last = None
    for _ in range(retries):
        r = req(f"{WEB}/basket/member/auth/login/password", "POST", {"email": email, "password": MEMBER_PASS})
        if r.get("code") == 200:
            return r["data"]["token"]
        last = r
        time.sleep(1.2)
    raise RuntimeError(f"web login failed {email}: {last}")


def db_conn():
    return pymysql.connect(cursorclass=pymysql.cursors.DictCursor, autocommit=False, **DB)


def db_query(sql: str, args: tuple | list | None = None) -> list[dict]:
    conn = db_conn()
    try:
        cur = conn.cursor()
        cur.execute(sql, args or ())
        return list(cur.fetchall())
    finally:
        conn.close()


def db_exec(sql: str, args: tuple | list | None = None) -> int:
    conn = db_conn()
    try:
        cur = conn.cursor()
        n = cur.execute(sql, args or ())
        conn.commit()
        return n
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def extract_id(resp: dict, key: str = "data") -> str:
    data = resp.get(key)
    if isinstance(data, dict):
        for k in ("invitationId", "applicationId", "registrationId", "submissionId", "changeId", "id", "teamId"):
            if data.get(k) is not None:
                return as_id(data[k], k)
        raise ValueError(f"no id in response data: {data}")
    return as_id(data, key)


def stage_payload(
    code,
    name,
    order,
    fmt,
    best_of,
    adv,
    next_code=None,
    adv_count=None,
    org="UNGROUPED",
    grouping="NONE",
    group_count=None,
    pairing="NONE",
):
    p = {
        "stageCode": code,
        "stageName": name,
        "stageOrder": order,
        "stageType": "CUSTOM",
        "organizationMode": org,
        "competitionFormat": fmt,
        "groupingMode": grouping,
        "roundRobinCycles": 1,
        "bestOf": best_of,
        "winPoints": 2,
        "lossPoints": 1,
        "advancementType": adv,
        "pairingRule": pairing,
        "requiresManualConfirmation": 0,
    }
    if next_code:
        p["nextStageCode"] = next_code
    if adv_count is not None:
        p["advancementCount"] = adv_count
    if group_count is not None:
        p["groupCount"] = group_count
    return p


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


def chain_update(token: str, division_id: str, top_n: int | None = 4) -> None:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    stages = sorted_stages(snap.get("data") or {})
    for i, s in enumerate(stages):
        is_last = i == len(stages) - 1
        next_code = stages[i + 1]["stageCode"] if not is_last else None
        fmt = s.get("competitionFormat") or "ROUND_ROBIN"
        adv = "NONE" if is_last else ("WINNER_ADVANCE" if fmt == "SINGLE_ELIMINATION" else "OVERALL_TOP_N")
        adv_count = None if is_last else (top_n if i == 0 and fmt != "SINGLE_ELIMINATION" else None)
        if fmt == "ROUND_ROBIN" and s.get("organizationMode") == "GROUPED" and not is_last:
            adv = "GROUP_TOP_N"
            adv_count = 2
        p = stage_payload(
            s["stageCode"],
            s.get("stageName"),
            i + 1,
            fmt,
            s.get("bestOf") or 1,
            adv,
            next_code,
            adv_count,
            org=s.get("organizationMode") or "UNGROUPED",
            grouping=s.get("groupingMode") or "NONE",
            group_count=s.get("groupCount"),
            pairing=s.get("pairingRule") or "NONE",
        )
        req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s['id']}", "PUT", p, token)


def count_stage_matches(stage_id: str | int) -> int:
    rows = db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0", (int(stage_id),))
    return int(rows[0]["c"]) if rows else 0


def list_matches(token: str, division_id: str, stage_id: int | str | None = None) -> list[dict]:
    url = f"{ADMIN}/basket/v4/match/list?divisionId={division_id}&pageNum=1&pageSize=500"
    if stage_id:
        url += f"&stageId={stage_id}"
    r = req(url, token=token)
    return r.get("rows") or []


def list_series(token: str, stage_id: int | str) -> list[dict]:
    r = req(f"{ADMIN}/basket/v4/match-series/stage/{stage_id}", token=token)
    return (r.get("data") or []) if r.get("code") == 200 else []


def get_series(token: str, series_id: int | str) -> dict:
    r = req(f"{ADMIN}/basket/v4/match-series/{series_id}", token=token)
    return r.get("data") or {}


def create_season(token: str, season_name: str, display_title: str, year: int = 2026) -> str:
    body = {
        "seriesId": SERIES_ID,
        "seasonYear": year,
        "seasonName": season_name,
        "displayTitle": display_title,
        "description": f"{MARKER} {season_name}",
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-12-31 23:59:59",
    }
    lst = req(f"{ADMIN}/basket/v4/season/list?seriesId={SERIES_ID}&pageNum=1&pageSize=200", token=token)
    for row in lst.get("rows") or []:
        if row.get("seasonName") == season_name or row.get("displayTitle") == display_title:
            sid = as_id(row["id"])
            if row.get("publishStatus") != "PUBLISHED":
                req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
            return sid
    r = req(f"{ADMIN}/basket/v4/season", "POST", body, token)
    if r.get("code") != 200:
        raise RuntimeError(f"create season failed: {r}")
    sid = extract_id(r)
    pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
    if pr.get("code") != 200:
        raise RuntimeError(f"publish season failed: {pr}")
    return sid


def create_division(
    token: str,
    season_id: str,
    division_name: str,
    max_team: int = 8,
    template_id: int = TEMPLATE_RR,
) -> str:
    dl = req(f"{ADMIN}/basket/v4/division/list?seasonId={season_id}&pageNum=1&pageSize=50", token=token)
    existing = next((r for r in (dl.get("rows") or []) if r.get("divisionName") == division_name), None)
    if existing:
        return as_id(existing["id"])
    body = {
        "seasonId": int(season_id),
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": division_name,
        "registrationMode": "TEAM",
        "formatType": "REGULAR_ROUND_ROBIN",
        "formatTemplateId": template_id,
        "maxTeamCount": max_team,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-20 23:59:59",
        "matchStartTime": "2026-08-21 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",
        "sortOrder": 1,
    }
    dr = req(f"{ADMIN}/basket/v4/division", "POST", body, token)
    if dr.get("code") != 200:
        raise RuntimeError(f"create division failed: {dr}")
    return extract_id(dr)


def open_registration(token: str, division_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/division/{division_id}/open-registration", "PUT", token=token)


def close_registration(token: str, division_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/division/{division_id}/close-registration", "PUT", token=token)


def ensure_platform_accounts(cur, player_id: int, game_id: str, now: str, slot: int) -> None:
    for pidx, platform in enumerate(("PC", "PS", "PS_XBOX")):
        cur.execute(
            "SELECT id FROM basket_player_platform_account "
            "WHERE player_id=%s AND platform=%s AND is_delete=0 LIMIT 1",
            (player_id, platform),
        )
        if cur.fetchone():
            continue
        plat_id = ID_BASE + 50_000_000 + int(slot) * 10 + pidx
        cur.execute(
            "INSERT INTO basket_player_platform_account "
            "(id, player_id, platform, display_game_id, normalized_game_id, account_status, is_primary, "
            "approved_by, approved_time, effective_time, create_time, update_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,1,1,1,%s,%s,%s,%s,0)",
            (plat_id, player_id, platform, game_id, game_id.lower(), now, now, now, now),
        )


def seed_org_pool(pool_code: str, team_defs: list[dict], offset: int) -> list[dict]:
    """Minimal SQL seed: member/profile/player/platform only. Teams created via Web API."""
    teams: list[dict] = []
    conn = db_conn()
    cur = conn.cursor()
    try:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        for ti, td in enumerate(team_defs):
            player_ids: list[str] = []
            member_ids: list[str] = []
            emails: list[str] = []
            for pi, pname in enumerate(td["players"]):
                member_id = ID_BASE + offset + ti * 100 + pi
                player_id = ID_BASE + offset + 1_000_000 + ti * 100 + pi
                email = f"fs_{pool_code.lower()}_{td['code'].lower()}{pi + 1:02d}@cnpa.smoke"
                # skip if already seeded
                cur.execute("SELECT id FROM basket_member_account WHERE id=%s", (member_id,))
                if not cur.fetchone():
                    cur.execute(
                        "INSERT INTO basket_member_account "
                        "(id, account_no, username, password_hash, email, email_verified, account_status, "
                        "register_source, register_terminal, create_time, update_time, is_delete) "
                        "VALUES (%s,%s,%s,%s,%s,1,1,'WEB','PC',%s,%s,0)",
                        (
                            member_id,
                            f"MBR{member_id}",
                            f"fs_{pool_code.lower()}_{td['code'].lower()}{pi + 1:02d}",
                            PASS_HASH,
                            email,
                            now,
                            now,
                        ),
                    )
                    cur.execute(
                        "INSERT INTO basket_member_profile "
                        "(id, member_id, nickname, avatar_file_id, gender, profile_status, create_time, update_time, is_delete) "
                        "VALUES (%s,%s,%s,%s,0,1,%s,%s,0)",
                        (member_id, member_id, pname, LOGOS[pi % len(LOGOS)], now, now),
                    )
                    cur.execute(
                        "INSERT INTO basket_player_profile "
                        "(id, member_id, player_no, game_terminal, game_account, game_nickname, position_code, "
                        "certification_status, player_status, certified_time, profile_visible, create_time, update_time, is_delete) "
                        "VALUES (%s,%s,%s,'PC',%s,%s,%s,3,1,%s,1,%s,%s,0)",
                        (
                            player_id,
                            member_id,
                            f"CNPA-P-FS-{pool_code}-{td['code']}{pi + 1:02d}",
                            pname,
                            pname,
                            POSITIONS[pi % len(POSITIONS)],
                            now,
                            now,
                            now,
                        ),
                    )
                else:
                    cur.execute("SELECT email FROM basket_member_account WHERE id=%s", (member_id,))
                    email = cur.fetchone()["email"]
                game_id = f"{pool_code.lower()}{td['code'].lower()}{pi + 1:02d}"
                ensure_platform_accounts(cur, player_id, game_id, now, slot=offset + ti * 20 + pi)
                player_ids.append(str(player_id))
                member_ids.append(str(member_id))
                emails.append(email)
            teams.append(
                {
                    "code": td["code"],
                    "name": td["name"],
                    "short": td.get("short") or td["code"],
                    "captain_email": emails[0],
                    "captain_player_id": player_ids[0],
                    "captain_member_id": member_ids[0],
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


def create_captain_grant(token: str, division_id: str, player_id: str, remark: str = MARKER) -> dict:
    # validFrom slightly in the past to avoid Admin/Web clock skew "尚未生效"
    vf = (datetime.now() - timedelta(hours=1)).strftime("%Y-%m-%d %H:%M:%S")
    return req(
        f"{ADMIN}/basket/v4/captain-grant",
        "POST",
        {"divisionId": division_id, "playerId": player_id, "remark": remark, "validFrom": vf},
        token,
    )


def create_team_via_web(token: str, division_id: str, name: str, short: str, logo: str | None = None) -> dict:
    body: dict[str, Any] = {
        "divisionId": division_id,
        "teamName": name,
        "shortName": short,
        "publicStatus": 1,
        "description": MARKER,
    }
    if logo:
        body["logoFileId"] = logo
    return req(f"{WEB}/basket/team/create", "POST", body, token)


def approve_team(token: str, team_id: str) -> dict:
    req(f"{ADMIN}/basket/teamAudit/{team_id}/start-review", "POST", {}, token)
    return req(f"{ADMIN}/basket/teamAudit/{team_id}/approve", "POST", {"auditRemark": MARKER}, token)


def invite_and_accept(captain_token: str, invitee_token: str, team_id: str, invitee_player_id: str) -> dict:
    ir = req(
        f"{WEB}/basket/team/{team_id}/invitation",
        "POST",
        {"inviteePlayerId": invitee_player_id, "message": MARKER},
        captain_token,
    )
    if ir.get("code") != 200:
        # if pending already exists, accept via mine page
        if "已" in str(ir.get("msg") or "") or ir.get("code") == 400:
            mine = req(f"{WEB}/basket/team/invitation/mine/page?pageNum=1&pageSize=50", token=invitee_token)
            records = ((mine.get("data") or {}).get("records") or [])
            hit = next((r for r in records if as_id(r.get("teamId")) == as_id(team_id) and r.get("canRespond")), None)
            if not hit:
                return ir
            inv_id = as_id(hit.get("invitationId") or hit.get("id"))
            return req(f"{WEB}/basket/team/invitation/{inv_id}/respond", "PUT", {"accept": True, "action": "ACCEPT"}, invitee_token)
        return ir
    data = ir.get("data") or {}
    inv_id = as_id(data.get("invitationId") or data.get("id") or extract_id(ir))
    return req(
        f"{WEB}/basket/team/invitation/{inv_id}/respond",
        "PUT",
        {"accept": True, "action": "ACCEPT"},
        invitee_token,
    )


def apply_and_approve(player_token: str, captain_token: str, team_id: str, applicant_member_id: str | None = None) -> dict:
    ar = req(f"{WEB}/basket/team/{team_id}/join-application", "POST", {"message": MARKER}, player_token)
    if ar.get("code") == 200:
        data = ar.get("data") or {}
        app_id = as_id(data.get("applicationId") or data.get("id") or extract_id(ar))
    else:
        page = req(f"{WEB}/basket/team/{team_id}/join-application/page?pageNum=1&pageSize=50", token=captain_token)
        records = ((page.get("data") or {}).get("records") or [])
        hits = [r for r in records if r.get("canProcess") or r.get("applicationStatus") in (0, "0", "PENDING")]
        if applicant_member_id:
            hits = [r for r in hits if as_id(r.get("applicantMemberId") or "0") == as_id(applicant_member_id)]
        if not hits:
            return ar
        app_id = as_id(hits[0].get("applicationId") or hits[0].get("id"))
    return req(
        f"{WEB}/basket/team/join-application/{app_id}/process",
        "PUT",
        {"approve": True, "action": "APPROVE", "remark": MARKER},
        captain_token,
    )

def register_team(web_token: str, division_id: str, team_id: str, player_ids: list[str]) -> dict:
    body = {"divisionId": division_id, "teamId": team_id, "playerIds": [as_id(p) for p in player_ids]}
    sr = req(f"{WEB}/basket/v4/player/registration/save", "POST", body, web_token)
    if sr.get("code") != 200:
        return sr
    reg_id = extract_id(sr)
    return req(f"{WEB}/basket/v4/player/registration/submit", "POST", {"registrationId": reg_id}, web_token)


def approve_all_registrations(token: str, division_id: str) -> int:
    r = req(f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=100", token=token)
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
    submit_event_team_id,
    reject_first: bool = False,
) -> dict:
    mid = match["id"]
    # If already finished, no-op
    if match.get("matchStatus") in ("FINISHED", "FORFEITED"):
        return {"matchId": mid, "skipped": True}

    lr0 = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=10", token=admin_token)
    rows0 = lr0.get("rows") or []
    pending0 = next((x for x in rows0 if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    approved0 = next((x for x in rows0 if x.get("submissionStatus") == "APPROVED"), None)
    if approved0 and match.get("matchStatus") in ("FINISHED", "FORFEITED", None):
        # may still need approve sync; if pending none and finished elsewhere
        pass
    if pending0 and not reject_first:
        ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending0['submissionId']}/approve", "POST", {}, admin_token)
        if ar.get("code") != 200:
            raise RuntimeError(f"approve pending failed match {mid}: {ar}")
        return {"submissionId": pending0["submissionId"], "matchId": mid, "approve": ar, "reusedPending": True}
    if pending0 and reject_first:
        rr = req(
            f"{ADMIN}/basket/v4/match-submission-audit/{pending0['submissionId']}/reject",
            "POST",
            {"rejectReason": "FULL_SMOKE reject then resubmit", "auditRemark": "FULL_SMOKE reject then resubmit"},
            admin_token,
        )
        if rr.get("code") != 200:
            raise RuntimeError(f"reject failed: {rr}")
        reject_first = False  # continue to resubmit below

    detail = req(
        f"{WEB}/basket/v4/player/match-submission/{mid}/detail?submitEventTeamId={submit_event_team_id}",
        token=web_token,
    )
    if detail.get("code") != 200:
        raise RuntimeError(f"submission detail failed match {mid}: {detail}")
    d = detail["data"]
    player_stats = []
    for i, ps in enumerate(d.get("playerStats") or []):
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
        "submitRemark": MARKER,
    }
    sr = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
    if sr.get("code") != 200:
        raise RuntimeError(f"save submission failed match {mid}: {sr}")
    sub_id = extract_id(sr)
    tr = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id}, web_token)
    if tr.get("code") != 200:
        raise RuntimeError(f"submit failed match {mid}: {tr}")
    lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
    pending = next((x for x in (lr.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    if not pending:
        raise RuntimeError(f"no pending submission for match {mid}")
    if reject_first:
        rr = req(
            f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/reject",
            "POST",
            {"rejectReason": "FULL_SMOKE reject then resubmit", "auditRemark": "FULL_SMOKE reject then resubmit"},
            admin_token,
        )
        if rr.get("code") != 200:
            raise RuntimeError(f"reject failed: {rr}")
        sr2 = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
        if sr2.get("code") != 200:
            raise RuntimeError(f"resave failed: {sr2}")
        sub_id2 = extract_id(sr2)
        tr2 = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id2}, web_token)
        if tr2.get("code") != 200:
            raise RuntimeError(f"resubmit failed: {tr2}")
        lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
        pending = next((x for x in (lr.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
        if not pending:
            raise RuntimeError(f"no pending after resubmit match {mid}")
    ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/approve", "POST", {}, admin_token)
    if ar.get("code") != 200:
        raise RuntimeError(f"approve failed match {mid}: {ar}")
    return {"submissionId": pending["submissionId"], "matchId": mid, "approve": ar}

def finish_bo1_stage(admin_token: str, web_by_event: dict[str, str], division_id: str, stage_id: str) -> int:
    matches = list_matches(admin_token, division_id, stage_id)
    bo1 = [m for m in matches if not m.get("seriesId")]
    score_pool = [(72, 66), (81, 75), (68, 70), (77, 71), (84, 79), (69, 63), (73, 68), (88, 82)]
    done = 0
    for idx, m in enumerate(bo1):
        if m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home = m.get("homeEventTeamId")
        hs, aws = score_pool[idx % len(score_pool)]
        if hs == aws:
            hs += 1
        # deterministic winner preference by index for standing variety
        if idx % 3 == 0 and hs > aws:
            hs, aws = aws, hs
        wt = web_by_event.get(as_id(home))
        if not wt:
            raise RuntimeError(f"no web token for event team {home}")
        reject_first = done == 0
        submit_and_approve_match(admin_token, wt, m, hs, aws, home, reject_first=reject_first)
        done += 1
    return done


def finish_series_games(admin_token: str, web_by_event: dict, series_id: int | str, results_plan: list[str]) -> dict:
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


def resolve_manual_rank_if_needed(token: str, stage_id: str) -> None:
    st = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/standings", token=token)
    groups = st.get("data") or []
    if not groups:
        return
    for g in groups:
        pending = [r for r in (g.get("rows") or []) if r.get("manualRankPending")]
        if not pending:
            continue
        items = [{"eventTeamId": as_id(r["eventTeamId"]), "manualRankNo": i + 1} for i, r in enumerate(pending)]
        body = {"groupScopeKey": g.get("groupScopeKey") or 0, "reason": MARKER, "items": items}
        mr = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/manual-rank", "POST", body, token)
        if mr.get("code") != 200:
            raise RuntimeError(f"manual rank failed: {mr}")


def confirm_stage_qualification(token: str, stage_id: str) -> None:
    # Skip if already confirmed/completed
    rows = db_query(
        "SELECT execution_status, confirmed_time FROM basket_division_stage WHERE id=%s AND is_delete=0",
        (int(stage_id),),
    )
    if rows:
        es = rows[0].get("execution_status")
        if rows[0].get("confirmed_time") is not None or es in ("COMPLETED", "CONFIRMED"):
            return
    cc = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/check-completion", "POST", token=token)
    if cc.get("code") != 200:
        raise RuntimeError(f"check-completion failed: {cc}")
    resolve_manual_rank_if_needed(token, stage_id)
    pv = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/qualification-preview", token=token)
    if pv.get("code") != 200:
        # already past previewable / already confirmed
        cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
        if cf.get("code") == 200:
            return
        rows2 = db_query(
            "SELECT execution_status, confirmed_time FROM basket_division_stage WHERE id=%s AND is_delete=0",
            (int(stage_id),),
        )
        if rows2 and (rows2[0].get("confirmed_time") is not None or rows2[0].get("execution_status") in ("COMPLETED", "CONFIRMED")):
            return
        raise RuntimeError(f"qualification-preview failed: {pv}; confirm={cf}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
    if cf.get("code") != 200:
        raise RuntimeError(f"confirm-qualification failed: {cf}")


def finish_division(token: str, division_id: str, reason: str = MARKER) -> dict:
    return req(f"{ADMIN}/basket/v4/division/{division_id}/finish", "POST", {"reason": reason}, token)


def load_context() -> dict:
    if CTX_PATH.exists():
        return json.loads(CTX_PATH.read_text(encoding="utf-8"))
    return {"created_at": datetime.now().isoformat(timespec="seconds"), "smokes": {}, "baseline": {}, "build": {}}


def save_context(ctx: dict) -> None:
    CTX_PATH.write_text(json.dumps(ctx, ensure_ascii=False, indent=2), encoding="utf-8")


def merge_bag(ctx: dict, bag: SmokeBag) -> None:
    ctx.setdefault("smokes", {})[bag.smoke] = bag.to_dict()
    save_context(ctx)


TINY_PNG = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde"
    b"\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
)

MAIN_TEAMS = [
    {"code": "ALP", "name": "FS_ALPHA", "short": "ALP", "players": ["ALP_C", "ALP_1", "ALP_2", "ALP_3", "ALP_4"]},
    {"code": "BRA", "name": "FS_BRAVO", "short": "BRA", "players": ["BRA_C", "BRA_1", "BRA_2", "BRA_3", "BRA_4"]},
    {"code": "CHR", "name": "FS_CHARLIE", "short": "CHR", "players": ["CHR_C", "CHR_1", "CHR_2", "CHR_3", "CHR_4"]},
    {"code": "DEL", "name": "FS_DELTA", "short": "DEL", "players": ["DEL_C", "DEL_1", "DEL_2", "DEL_3", "DEL_4"]},
    {"code": "ECH", "name": "FS_ECHO", "short": "ECH", "players": ["ECH_C", "ECH_1", "ECH_2", "ECH_3", "ECH_4"]},
    {"code": "FOX", "name": "FS_FOXTROT", "short": "FOX", "players": ["FOX_C", "FOX_1", "FOX_2", "FOX_3", "FOX_4"]},
    {"code": "GLF", "name": "FS_GOLF", "short": "GLF", "players": ["GLF_C", "GLF_1", "GLF_2", "GLF_3", "GLF_4"]},
    {"code": "HTL", "name": "FS_HOTEL", "short": "HTL", "players": ["HTL_C", "HTL_1", "HTL_2", "HTL_3", "HTL_4"]},
]

GROUP_TEAMS = [
    {"code": "G01", "name": "FSG_NORTH", "short": "NTH", "players": ["NTH_C", "NTH_1", "NTH_2", "NTH_3", "NTH_4"]},
    {"code": "G02", "name": "FSG_SOUTH", "short": "STH", "players": ["STH_C", "STH_1", "STH_2", "STH_3", "STH_4"]},
    {"code": "G03", "name": "FSG_EAST", "short": "EST", "players": ["EST_C", "EST_1", "EST_2", "EST_3", "EST_4"]},
    {"code": "G04", "name": "FSG_WEST", "short": "WST", "players": ["WST_C", "WST_1", "WST_2", "WST_3", "WST_4"]},
    {"code": "G05", "name": "FSG_CENTRAL", "short": "CTL", "players": ["CTL_C", "CTL_1", "CTL_2", "CTL_3", "CTL_4"]},
    {"code": "G06", "name": "FSG_COAST", "short": "CST", "players": ["CST_C", "CST_1", "CST_2", "CST_3", "CST_4"]},
    {"code": "G07", "name": "FSG_RIVER", "short": "RVR", "players": ["RVR_C", "RVR_1", "RVR_2", "RVR_3", "RVR_4"]},
    {"code": "G08", "name": "FSG_PEAK", "short": "PEK", "players": ["PEK_C", "PEK_1", "PEK_2", "PEK_3", "PEK_4"]},
]

ADV_TEAMS = [
    {"code": "A01", "name": "FSA_ONE", "short": "A1", "players": ["A1_C", "A1_1", "A1_2", "A1_3", "A1_4"]},
    {"code": "A02", "name": "FSA_TWO", "short": "A2", "players": ["A2_C", "A2_1", "A2_2", "A2_3", "A2_4"]},
    {"code": "A03", "name": "FSA_THREE", "short": "A3", "players": ["A3_C", "A3_1", "A3_2", "A3_3", "A3_4"]},
    {"code": "A04", "name": "FSA_FOUR", "short": "A4", "players": ["A4_C", "A4_1", "A4_2", "A4_3", "A4_4"]},
    {"code": "A05", "name": "FSA_FIVE", "short": "A5", "players": ["A5_C", "A5_1", "A5_2", "A5_3", "A5_4"]},
    {"code": "A06", "name": "FSA_SIX", "short": "A6", "players": ["A6_C", "A6_1", "A6_2", "A6_3", "A6_4"]},
]

FORBIDDEN_DIVISION_IDS = frozenset({"118", "141"})
SPARE_TEAMS = [
    {"code": "SPR", "name": "FS_SPARE", "short": "SPR", "players": ["SPR_C", "SPR_1", "SPR_2", "SPR_3", "SPR_4"]},
]


def refuse_forbidden_division(division_id: str) -> None:
    if as_id(division_id) in FORBIDDEN_DIVISION_IDS:
        raise RuntimeError(f"Refusing hardcoded legacy division id {division_id}")


def lock_snapshot(token: str, division_id: str) -> dict:
    r = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot/lock", "POST", token=token)
    if r.get("code") != 200:
        raise RuntimeError(f"lock snapshot failed: {r}")
    return r


def get_format_stages(token: str, division_id: str) -> list[dict]:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    return sorted_stages(snap.get("data") or {})


def put_tie_break(token: str, division_id: str, stage_id: str) -> dict:
    return req(
        f"{ADMIN}/basket/v4/divisions/{division_id}/tie-break-rules",
        "PUT",
        {"stageId": int(stage_id), "rules": TIE_BREAK},
        token,
    )


def init_stage_teams(token: str, stage_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/initialize-teams", "POST", token=token)


def generate_schedule(token: str, stage_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/generate-schedule", "POST", token=token)


def publish_schedule(token: str, stage_id: str) -> dict:
    return req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/publish-schedule", "POST", token=token)


def generate_and_publish(token: str, stage_id: str, init: bool = True, division_id: str | None = None) -> dict:
    # Prefer series presence for elimination stages; for RR use stage-scoped matches.
    series_existing = list_series(token, stage_id)
    if series_existing:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing-series", "count": len(series_existing)}, "publish": pub}

    # Prefer DB count — admin match list may omit stageId and over-match.
    db_cnt = count_stage_matches(stage_id)
    if db_cnt > 0:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing-matches-db", "count": db_cnt}, "publish": pub}

    if init:
        ir = init_stage_teams(token, stage_id)
        if ir.get("code") != 200:
            msg = str(ir.get("msg") or "")
            if "已" not in msg and "already" not in msg.lower():
                raise RuntimeError(f"initialize-teams failed stage {stage_id}: {ir}")
    gen = generate_schedule(token, stage_id)
    if gen.get("code") != 200:
        raise RuntimeError(f"generate-schedule failed stage {stage_id}: {gen}")
    pub = publish_schedule(token, stage_id)
    if pub.get("code") != 200:
        msg = str(pub.get("msg") or "")
        if "已" not in msg and "PUBLISH" not in msg.upper() and "发布" not in msg:
            raise RuntimeError(f"publish-schedule failed stage {stage_id}: {pub}")
    return {"generate": gen, "publish": pub}

def _create_stage(token: str, division_id: str, payload: dict) -> str:
    r = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", payload, token)
    if r.get("code") != 200:
        raise RuntimeError(f"create stage failed: {r}")
    data = r.get("data")
    return as_id(data if isinstance(data, (int, str)) else extract_id(r))


def setup_main_format(token: str, division_id: str) -> dict[str, str]:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    data = snap.get("data") or {}
    stages = sorted_stages(data)
    if data.get("locked") or data.get("snapshotStatus") == "LOCKED":
        if len(stages) >= 3:
            return {"st1": as_id(stages[0]["id"]), "st2": as_id(stages[1]["id"]), "st3": as_id(stages[2]["id"])}
    wipe_stages(token, division_id)
    st1 = _create_stage(token, division_id, stage_payload("ST1", "RR Top4", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "ST2", 4))
    st2 = _create_stage(
        token,
        division_id,
        stage_payload("ST2", "Semi BO3", 2, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "ST3", pairing="HIGH_VS_LOW"),
    )
    st3 = _create_stage(
        token,
        division_id,
        stage_payload("ST3", "Final BO5", 3, "SINGLE_ELIMINATION", 5, "NONE", pairing="HIGH_VS_LOW"),
    )
    chain_update(token, division_id)
    put_tie_break(token, division_id, st1)
    lock_snapshot(token, division_id)
    return {"st1": st1, "st2": st2, "st3": st3}


def setup_group_format(token: str, division_id: str, bag: SmokeBag | None = None) -> dict[str, str]:
    wipe_stages(token, division_id)
    adv = "GROUP_TOP_N"
    p1 = stage_payload("G1", "Group RR", 1, "ROUND_ROBIN", 1, adv, "G2", 2, org="GROUPED", grouping="MANUAL", group_count=2)
    r1 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", p1, token)
    if r1.get("code") != 200:
        adv = "OVERALL_TOP_N"
        p1["advancementType"] = adv
        r1 = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages", "POST", p1, token)
        if bag is not None:
            bag.fixes.append({"topic": "advancementType", "used": adv, "reason": "GROUP_TOP_N rejected on create"})
    if r1.get("code") != 200:
        raise RuntimeError(f"create group stage failed: {r1}")
    st1 = as_id(r1.get("data") if isinstance(r1.get("data"), (int, str)) else extract_id(r1))
    st2 = _create_stage(
        token,
        division_id,
        stage_payload("G2", "Playoff BO3", 2, "SINGLE_ELIMINATION", 3, "WINNER_ADVANCE", "G3", pairing="HIGH_VS_LOW"),
    )
    st3 = _create_stage(
        token,
        division_id,
        stage_payload("G3", "Final BO5", 3, "SINGLE_ELIMINATION", 5, "NONE", pairing="HIGH_VS_LOW"),
    )
    chain_update(token, division_id, top_n=2)
    put_tie_break(token, division_id, st1)
    lr = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot/lock", "POST", token=token)
    if lr.get("code") != 200 and adv == "GROUP_TOP_N":
        adv = "OVERALL_TOP_N"
        stages = get_format_stages(token, division_id)
        if stages:
            s0 = stages[0]
            p = stage_payload(
                s0["stageCode"],
                s0.get("stageName"),
                s0.get("stageOrder") or 1,
                s0.get("competitionFormat") or "ROUND_ROBIN",
                s0.get("bestOf") or 1,
                adv,
                s0.get("nextStageCode"),
                2,
                org="GROUPED",
                grouping="MANUAL",
                group_count=2,
            )
            req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-stages/{s0['id']}", "PUT", p, token)
            if bag is not None:
                bag.fixes.append({"topic": "advancementType", "used": adv, "reason": "lock rejected GROUP_TOP_N"})
        lock_snapshot(token, division_id)
    elif lr.get("code") != 200:
        raise RuntimeError(f"lock snapshot failed: {lr}")
    return {"st1": st1, "st2": st2, "st3": st3, "advancementType": adv}


def setup_advanced_format(token: str, division_id: str) -> dict[str, str]:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    data = snap.get("data") or {}
    stages = sorted_stages(data)
    if (data.get("locked") or data.get("snapshotStatus") == "LOCKED") and len(stages) >= 2:
        return {"st1": as_id(stages[0]["id"]), "st2": as_id(stages[1]["id"])}
    wipe_stages(token, division_id)
    st1 = _create_stage(
        token,
        division_id,
        stage_payload("A1", "SE BO1 BYE", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "A2", pairing="HIGH_VS_LOW"),
    )
    st2 = _create_stage(
        token,
        division_id,
        stage_payload("A2", "SE BO3 Random", 2, "SINGLE_ELIMINATION", 3, "NONE", pairing="RANDOM_DRAW"),
    )
    chain_update(token, division_id, top_n=2)
    lock_snapshot(token, division_id)
    return {"st1": st1, "st2": st2}


def web_tokens_for_teams(teams: list[dict]) -> dict[str, str]:
    emails = [t["captain_email"] for t in teams]
    clear_login_locks(emails)
    return {t["captain_email"]: web_login(t["captain_email"]) for t in teams}


def build_web_by_event(admin_token: str, division_id: str, teams: list[dict], tokens: dict[str, str]) -> dict[str, str]:
    etr = req(f"{ADMIN}/basket/v4/event-team/list?divisionId={division_id}&pageNum=1&pageSize=50", token=admin_token)
    rows = etr.get("rows") or []
    by_name = {(r.get("teamNameSnapshot") or r.get("teamName") or ""): r for r in rows}
    web_by_event: dict[str, str] = {}
    for t in teams:
        et = by_name.get(t["name"]) or next((v for k, v in by_name.items() if t["name"] in k), None)
        if et:
            web_by_event[as_id(et["id"])] = tokens[t["captain_email"]]
    return web_by_event


def count_event_entities(division_id: str) -> tuple[int, int]:
    et = int(db_query("SELECT COUNT(*) AS c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (division_id,))[0]["c"])
    ep = int(
        db_query(
            "SELECT COUNT(*) AS c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
            "WHERE et.division_id=%s AND ep.is_delete=0",
            (division_id,),
        )[0]["c"]
    )
    return et, ep


def verify_event_player_source_distinct(division_id: str) -> tuple[bool, str]:
    rows = db_query(
        "SELECT ep.id, ep.source_player_id FROM basket_event_player ep "
        "JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.division_id=%s AND ep.is_delete=0",
        (division_id,),
    )
    bad = [r for r in rows if str(r["id"]) == str(r["source_player_id"])]
    return len(bad) == 0, f"bad={len(bad)} total={len(rows)}"


def create_teams_flow(admin_token: str, division_id: str, teams: list[dict], logos: list[str] | None = None) -> list[str]:
    """Create/approve teams via Web API. Idempotent if team already exists for captain in division."""
    team_ids: list[str] = []
    for i, t in enumerate(teams):
        # reuse existing ACTIVE team for this captain+division
        existing = db_query(
            "SELECT id, audit_status FROM basket_team WHERE division_id=%s AND captain_player_id=%s "
            "AND team_status=1 AND is_delete=0 ORDER BY id DESC LIMIT 1",
            (int(division_id), int(t["captain_player_id"])),
        )
        if existing:
            tid = as_id(existing[0]["id"])
            t["team_id"] = tid
            audit = int(existing[0].get("audit_status") or 0)
            if audit != 1:
                tok = web_login(t["captain_email"])
                if audit == 4:  # DRAFT
                    sr = req(f"{WEB}/basket/team/{tid}/submit-certification", "POST", {}, tok)
                    if sr.get("code") != 200:
                        msg = str(sr.get("msg") or "")
                        if "审核中" not in msg and "已完成" not in msg and "已通过" not in msg:
                            raise RuntimeError(f"submit certification failed existing {t['name']}: {sr}")
                ar = approve_team(admin_token, tid)
                if ar.get("code") != 200:
                    raise RuntimeError(f"approve existing team failed {t['name']}: {ar}")
            team_ids.append(tid)
            continue

        gr = create_captain_grant(admin_token, division_id, t["captain_player_id"])
        if gr.get("code") != 200:
            # tolerate already-granted (avoid encoding-sensitive Chinese match)
            msg = str(gr.get("msg") or "")
            if gr.get("code") not in (500, 400, 403) or ("grant" not in msg.lower() and "授权" not in msg and "已" not in msg):
                # still continue if looks like duplicate; else raise
                if "已" not in msg and "存在" not in msg and "有效" not in msg:
                    raise RuntimeError(f"grant failed team {t['code']}: {gr}")
        tok = web_login(t["captain_email"])
        # small delay to avoid clock-skew on brand-new grants
        time.sleep(0.3)
        # logo 需归属队长本人文件资产；冒烟默认不传，避免「无权使用该文件」
        logo = logos[i % len(logos)] if logos else None
        cr = create_team_via_web(tok, division_id, t["name"], t["short"], logo)
        if cr.get("code") != 200:
            raise RuntimeError(f"create team failed {t['name']}: {cr}")
        tid = as_id((cr.get("data") or {}).get("teamId") or (cr.get("data") or {}).get("id") or extract_id(cr))
        t["team_id"] = tid
        # 球队创建后为 DRAFT，需先提交认证再进入 Admin 审核
        sr = req(f"{WEB}/basket/team/{tid}/submit-certification", "POST", {}, tok)
        if sr.get("code") != 200:
            msg = str(sr.get("msg") or "")
            if "审核中" not in msg and "已完成" not in msg and "已通过" not in msg:
                raise RuntimeError(f"submit certification failed {t['name']}: {sr}")
        ar = approve_team(admin_token, tid)
        if ar.get("code") != 200:
            raise RuntimeError(f"approve team failed {t['name']}: {ar}")
        team_ids.append(tid)
    return team_ids

def populate_members(teams: list[dict], invite_until: int = 4, full_players: int = 5) -> None:
    for ti, t in enumerate(teams):
        cap_tok = web_login(t["captain_email"])
        tid = t["team_id"]
        for pi in range(1, full_players):
            p_email = t["emails"][pi]
            p_pid = t["player_ids"][pi]
            # already member?
            rows = db_query(
                "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND membership_status=1 AND is_delete=0",
                (int(tid), int(p_pid)),
            )
            if rows:
                continue
            p_tok = web_login(p_email)
            if ti < invite_until:
                r = invite_and_accept(cap_tok, p_tok, tid, p_pid)
            else:
                r = apply_and_approve(p_tok, cap_tok, tid, t["member_ids"][pi])
            if r.get("code") != 200:
                raise RuntimeError(f"join failed {t['name']} player#{pi}: {r}")


def register_all_teams(web_tokens: dict[str, str], admin_token: str, division_id: str, teams: list[dict]) -> int:
    existing = req(
        f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=100",
        token=admin_token,
    )
    by_team = {as_id(r.get("teamId")): r for r in (existing.get("rows") or [])}
    for t in teams:
        st = (by_team.get(as_id(t["team_id"])) or {}).get("registrationStatus")
        if st in ("APPROVED", "PENDING_REVIEW"):
            continue
        tok = web_tokens[t["captain_email"]]
        rr = register_team(tok, division_id, t["team_id"], t["player_ids"][:5])
        if rr.get("code") != 200:
            raise RuntimeError(f"register failed {t['name']}: {rr}")
    return approve_all_registrations(admin_token, division_id)


def baseline_counts() -> dict[str, int]:
    out: dict[str, int] = {}
    for tb in (
        "basket_division",
        "basket_event_team",
        "basket_event_player",
        "basket_match",
        "basket_match_series",
        "basket_captain_grant",
        "basket_team",
    ):
        try:
            out[tb] = int(db_query(f"SELECT COUNT(*) AS c FROM {tb} WHERE is_delete=0")[0]["c"])
        except Exception:
            out[tb] = -1
    return out


def env_health() -> dict[str, str]:
    health = {
        "admin": "UP" if wait_server(f"{ADMIN}/", 3) else "DOWN",
        "web": "UP" if wait_server(f"{WEB}/", 3) else "DOWN",
    }
    try:
        db_query("SELECT 1 AS ok")
        health["mysql"] = "UP"
    except Exception as exc:
        health["mysql"] = f"DOWN:{exc}"
    try:
        import redis

        r = redis.Redis(host=REDIS_HOST, port=6379, db=0, socket_connect_timeout=3)
        r.ping()
        health["redis"] = "UP"
    except Exception as exc:
        health["redis"] = f"DOWN:{exc}"
    return health


def public_series(series_id: str) -> dict:
    return req(f"{WEB}/basket/v4/public/match-series/{series_id}")


def grouping_batch_create(token: str, division_id: str, stage_id: str) -> str:
    r = req(
        f"{ADMIN}/basket/v4/admin/division-grouping-batches",
        "POST",
        {"divisionId": int(division_id), "stageId": int(stage_id), "resultSource": "MANUAL"},
        token,
    )
    if r.get("code") != 200:
        raise RuntimeError(f"grouping batch create failed: {r}")
    return as_id(r.get("data"))


def grouping_assign_and_lock(token: str, batch_id: str, group_event_map: dict[str, list[str]]) -> None:
    detail = req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}", token=token).get("data") or {}
    groups = detail.get("groups") or []
    gmap: dict[str, str] = {}
    for g in groups:
        code = (g.get("groupCode") or g.get("groupName") or "").replace("Group ", "").strip()
        gid = g.get("id") or g.get("groupId")
        if gid:
            gmap[code] = as_id(gid)
    for code, et_ids in group_event_map.items():
        gid = gmap.get(code)
        if not gid:
            continue
        for et_id in et_ids:
            req(
                f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/groups/{gid}/teams",
                "POST",
                {"eventTeamId": int(et_id)},
                token,
            )
    lr = req(f"{ADMIN}/basket/v4/admin/division-grouping-batches/{batch_id}/lock", "POST", token=token)
    if lr.get("code") != 200:
        raise RuntimeError(f"grouping lock failed: {lr}")
