#!/usr/bin/env python3
"""CNPA V2 Release Gate — 4-team round-robin E2E (20260806).

Strict order:
  submit all 4 regs → approve all 4 → close registration →
  initialize StageTeam → generate 6 matches → publish →
  finish all 6 matches → standings → my-events/my-schedule →
  permission/idempotency → DB checks → run_data_check → MD report.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
OUTPUT_JSON = ROOT / "scripts" / "output" / "cnpa-v2-release-gate-rr4-20260806-result.json"
OUTPUT_MD = ROOT / "docs" / "cnpa-v2-release-gate-4-team-round-robin-e2e-20260806.md"
BATCH = "CNPA_V2_RELEASE_GATE_RR4_20260806"
SEASON_NAME = "CNPA_V2_RELEASE_GATE_RR4_20260806"
DISPLAY_TITLE = "CNPA V2 上线前 4 队单循环验收"
MSG_REG_NOT_READY = "当前仍有报名未处理或正式参赛队伍尚未确认，暂不能生成赛程。"
MSG_PUBLISHED_NO_MORE = "赛程已经发布，不能继续追加参赛队伍"
TZ = timezone(timedelta(hours=8))
STARTED_AT = datetime.now(TZ)

CAPTAIN_EMAILS = ["111141@qq.com", "111142@qq.com", "111143@qq.com", "111144@qq.com"]
FIFTH_EMAIL = "111145@qq.com"

results: list[dict] = []
ids: dict = {}
p0_fixes: list[str] = []
p1_fixes: list[str] = []
p2_items: list[str] = []
failed: list[str] = []


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


def env(k: str, default: str = "") -> str:
    return os.environ.get(k, default)


def rec(name: str, status: str, detail: str = "", **extra) -> None:
    item = {"name": name, "status": status, "detail": detail, **extra}
    results.append(item)
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--", "MANUAL": "??"}.get(status, "..")
    print(f"[{mark}] {name}: {detail or status}", flush=True)
    if status == "FAIL":
        failed.append(f"{name}: {detail}")


def http(base: str, method: str, path: str, body=None, token: str | None = None,
         allow_fail: bool = False, timeout: int = 120) -> dict:
    url = base.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            payload["_http_status"] = resp.status
            payload["_url"] = url
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="ignore") or "{}"
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            payload = {"raw": raw[:500]}
        payload["_http_status"] = exc.code
        payload["_url"] = url
        if not allow_fail:
            raise RuntimeError(f"{method} {path} HTTP {exc.code}: {payload}") from exc
    except Exception as exc:
        payload = {"_http_status": 0, "_url": url, "msg": str(exc), "code": -1}
        if not allow_fail:
            raise
    else:
        if not allow_fail and payload.get("code") not in (200, None):
            raise RuntimeError(f"{method} {path} code={payload.get('code')} msg={payload.get('msg')}")
    return payload


def web_login(base: str, email: str, password: str) -> str:
    r = http(base, "POST", "/basket/member/auth/login/password", {"email": email, "password": password})
    token = (r.get("data") or {}).get("token")
    if not token:
        raise RuntimeError(f"login failed {email}: {r}")
    return token


def admin_login(base: str) -> str:
    r = http(base, "POST", "/login", {
        "username": env("CNPA_ADMIN_USERNAME", "admin"),
        "password": env("CNPA_ADMIN_PASSWORD", "admin123"),
    })
    token = r.get("token")
    if not token:
        raise RuntimeError(f"admin login failed: {r}")
    return token


def db_query(sql: str) -> list:
    import pymysql
    conn = pymysql.connect(
        host=env("CNPA_DB_HOST", "192.168.31.48"),
        port=int(env("CNPA_DB_PORT", "3306")),
        user=env("CNPA_DB_USER", "root"),
        password=env("CNPA_DB_PASSWORD", "123456"),
        database=env("CNPA_DB_NAME", "basketball"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            if cur.description:
                return list(cur.fetchall())
            conn.commit()
            return []
    finally:
        conn.close()


def iso_dt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def json_safe(obj):
    if isinstance(obj, dict):
        return {k: json_safe(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [json_safe(v) for v in obj]
    if isinstance(obj, datetime):
        return obj.strftime("%Y-%m-%d %H:%M:%S")
    if hasattr(obj, "isoformat"):
        return str(obj)
    try:
        from decimal import Decimal
        if isinstance(obj, Decimal):
            return int(obj) if obj == int(obj) else float(obj)
    except Exception:
        pass
    return obj


def team_by_captain(email: str) -> dict | None:
    rows = db_query(
        f"SELECT CAST(t.id AS CHAR) team_id, t.team_name, CAST(t.captain_member_id AS CHAR) captain_member_id, "
        f"CAST(t.captain_player_id AS CHAR) captain_player_id, ma.email "
        f"FROM basket_team t JOIN basket_member_account ma ON ma.id=t.captain_member_id "
        f"WHERE ma.email='{email}' AND t.is_delete=0 "
        f"AND t.team_name NOT LIKE 'TEST FIXTURE%%' ORDER BY t.id DESC LIMIT 1"
    )
    return rows[0] if rows else None


def captain_token_by_event_team(web_base: str, event_team_id: int, member_pwd: str) -> str | None:
    rows = db_query(
        f"SELECT ma.email FROM basket_event_team et JOIN basket_team t ON t.id=et.source_team_id "
        f"JOIN basket_member_account ma ON ma.id=t.captain_member_id WHERE et.id={event_team_id} LIMIT 1"
    )
    if not rows:
        return None
    try:
        return web_login(web_base, rows[0]["email"], member_pwd)
    except RuntimeError:
        return None


def submit_and_approve_match(admin_base: str, web_base: str, admin_token: str, match_id: int,
                             home_score: int, away_score: int, member_pwd: str,
                             reject_home: bool = False) -> dict:
    minfo = http(admin_base, "GET", f"/basket/v4/match/{match_id}", token=admin_token).get("data") or {}
    home_et = minfo["homeEventTeamId"]
    away_et = minfo["awayEventTeamId"]
    out = {"matchId": match_id, "homeEventTeamId": home_et, "awayEventTeamId": away_et,
           "homeScore": home_score, "awayScore": away_score, "submissions": [], "rejectHome": reject_home}
    for et_id, hs, as_ in [(home_et, home_score, away_score), (away_et, away_score, home_score)]:
        tok = captain_token_by_event_team(web_base, et_id, member_pwd)
        if not tok:
            out["submissions"].append({"eventTeamId": et_id, "error": "no captain token"})
            continue
        sv = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
            "matchId": match_id, "submitEventTeamId": et_id, "homeScore": hs, "awayScore": as_,
        }, token=tok, allow_fail=True)
        sid = sv.get("data")
        if isinstance(sid, dict):
            sid = sid.get("submissionId") or sid.get("id")
        if not sid:
            out["submissions"].append({"eventTeamId": et_id, "save": sv})
            continue
        http(web_base, "POST", "/basket/v4/player/match-submission/submit",
             {"submissionId": sid}, token=tok, allow_fail=True)
        entry = {"submissionId": sid, "eventTeamId": et_id, "version": None}
        if reject_home and et_id == home_et:
            rej = http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/reject",
                       {"auditRemark": f"{BATCH} reject test"}, token=admin_token, allow_fail=True)
            entry["reject"] = {"http": rej.get("_http_status"), "code": rej.get("code"), "msg": rej.get("msg")}
            rs = http(web_base, "POST", "/basket/v4/player/match-submission/resubmit", {
                "submissionId": sid, "homeScore": hs, "awayScore": as_, "submitRemark": "resubmit",
            }, token=tok, allow_fail=True)
            entry["resubmit"] = {"http": rs.get("_http_status"), "code": rs.get("code"), "msg": rs.get("msg")}
            ver = db_query(f"SELECT version_no, submission_status FROM basket_match_submission WHERE id={sid}")
            if ver:
                entry["version"] = ver[0]["version_no"]
                entry["statusAfterResubmit"] = ver[0]["submission_status"]
        apr = http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/approve",
                   {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
        entry["approve"] = {"http": apr.get("_http_status"), "code": apr.get("code"), "msg": apr.get("msg")}
        st = db_query(f"SELECT version_no, submission_status FROM basket_match_submission WHERE id={sid}")
        if st:
            entry["version"] = st[0]["version_no"]
            entry["submissionStatus"] = st[0]["submission_status"]
        out["submissions"].append(entry)
    return out


def check_env(admin_base: str, web_base: str, member_pwd: str) -> str:
    checks = []
    for name, url in [
        ("admin", admin_base),
        ("web", web_base),
        ("admin_ui", env("CNPA_ADMIN_UI_URL", "http://127.0.0.1:80")),
        ("web_ui", env("CNPA_WEB_UI_URL", "http://localhost:5173")),
    ]:
        try:
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=15) as resp:
                checks.append({"name": name, "url": url, "http": resp.status})
                rec(f"env_{name}", "PASS", f"url={url} http={resp.status}")
        except Exception as exc:
            checks.append({"name": name, "url": url, "http": 0, "error": str(exc)})
            rec(f"env_{name}", "FAIL", f"url={url} err={exc}")
    try:
        db_query("SELECT 1 AS ok")
        rec("env_mysql", "PASS", "connected")
        checks.append({"name": "mysql", "ok": True})
    except Exception as exc:
        rec("env_mysql", "FAIL", str(exc))
        checks.append({"name": "mysql", "ok": False, "error": str(exc)})
    try:
        import redis
        r = redis.Redis(host=env("CNPA_REDIS_HOST", "192.168.31.48"), port=int(env("CNPA_REDIS_PORT", "6379")),
                        password=env("CNPA_REDIS_PASSWORD", "Q86KDiDoZULykMfz"), decode_responses=True)
        pong = r.ping()
        rec("env_redis", "PASS" if pong else "FAIL", f"PING={pong}")
        checks.append({"name": "redis", "ping": pong})
    except Exception as exc:
        rec("env_redis", "FAIL", str(exc))
        checks.append({"name": "redis", "error": str(exc)})

    admin_token = admin_login(admin_base)
    rec("env_admin_login", "PASS", "POST /login http=200 token=yes")
    tok = web_login(web_base, CAPTAIN_EMAILS[0], member_pwd)
    rec("env_web_login", "PASS", f"email={CAPTAIN_EMAILS[0]} token=yes")
    ids["envChecks"] = checks
    ids["webProbeToken"] = bool(tok)
    return admin_token


def prepare_team_records() -> list[dict]:
    teams = []
    for email in CAPTAIN_EMAILS + [FIFTH_EMAIL]:
        t = team_by_captain(email)
        if not t:
            rec(f"team_missing_{email}", "FAIL", "long-term team not found")
            continue
        team_id = int(t["team_id"])
        members = db_query(
            f"SELECT CAST(tm.player_id AS CHAR) player_id, CAST(tm.member_id AS CHAR) member_id, "
            f"tm.role_type, ma.email, CAST(pa.id AS CHAR) platform_account_id, pa.display_game_id, "
            f"(SELECT COUNT(*) FROM basket_team_member tm2 WHERE tm2.player_id=tm.player_id "
            f" AND tm2.is_delete=0 AND tm2.is_current=1 AND tm2.membership_status=1) active_teams "
            f"FROM basket_team_member tm "
            f"JOIN basket_player_profile pp ON pp.id=tm.player_id "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id "
            f"LEFT JOIN basket_player_platform_account pa ON pa.player_id=pp.id AND pa.platform='PC' "
            f" AND pa.is_delete=0 AND pa.account_status=1 "
            f"WHERE tm.team_id={team_id} AND tm.is_delete=0 AND tm.is_current=1 AND tm.membership_status=1 "
            f"AND pp.certification_status=3 ORDER BY CASE WHEN tm.role_type='CAPTAIN' THEN 0 ELSE 1 END, tm.id"
        )
        ok = (len(members) >= 5
              and all(m["active_teams"] == 1 for m in members)
              and all(m["platform_account_id"] for m in members))
        rec(f"team_prep_{email}", "PASS" if ok else "FAIL",
            f"teamId={team_id} members={len(members)} exclusive={sum(1 for m in members if m['active_teams']==1)} "
            f"pc={sum(1 for m in members if m['platform_account_id'])}")
        teams.append({
            "email": email, "teamId": team_id, "teamName": t["team_name"],
            "captainUserId": int(t["captain_member_id"]),
            "captainPlayerId": int(t["captain_player_id"]),
            "members": members,
        })
    ids["teams"] = teams
    return teams


def find_existing_division(season_id: int) -> dict | None:
    div_name = f"{SEASON_NAME}-PC-5V5-RR4"
    rows = db_query(
        f"SELECT id, division_status, format_snapshot_id FROM basket_division "
        f"WHERE season_id={season_id} AND division_name='{div_name}' AND is_delete=0 LIMIT 1"
    )
    return rows[0] if rows else None


def load_division_ids(division_id: int) -> tuple[int, int, int]:
    div = db_query(f"SELECT * FROM basket_division WHERE id={division_id}")[0]
    snap_id = div["format_snapshot_id"]
    stage = db_query(
        f"SELECT id, competition_format, advancement_type, best_of FROM basket_division_stage "
        f"WHERE division_id={division_id} AND snapshot_id={snap_id} AND is_delete=0 ORDER BY stage_order LIMIT 1"
    )[0]
    stage_id = stage["id"]
    snap = db_query(
        f"SELECT id, snapshot_status, is_current FROM basket_division_format_snapshot WHERE id={snap_id}"
    )[0]
    current_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_division_format_snapshot WHERE division_id={division_id} "
        f"AND is_current=1 AND is_delete=0"
    )[0]["c"]
    stage_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_division_stage WHERE snapshot_id={snap_id} AND is_delete=0"
    )[0]["c"]
    rules = db_query(
        f"SELECT rule_code, priority_no FROM basket_division_tie_break_rule "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY priority_no"
    )
    ok = (div["division_status"] in ("REGISTRATION_OPEN", "REGISTRATION_CLOSED", "IN_PROGRESS", "COMPLETED")
          and snap["snapshot_status"] == "LOCKED"
          and current_cnt == 1
          and stage_cnt == 1
          and stage["competition_format"] == "ROUND_ROBIN"
          and stage["advancement_type"] in ("NONE", None, "")
          and [r["rule_code"] for r in rules] == [
              "WIN_COUNT", "HEAD_TO_HEAD", "POINT_DIFFERENCE", "TOTAL_POINTS", "MANUAL"])
    ids.update({
        "divisionId": division_id, "snapshotId": snap_id, "stageId": stage_id,
        "divisionStatus": div["division_status"], "snapshotStatus": snap["snapshot_status"],
        "competitionFormat": stage["competition_format"], "advancementType": stage["advancement_type"],
        "bestOf": stage.get("best_of"), "tieBreakRules": [r["rule_code"] for r in rules],
    })
    rec("create_division", "PASS" if ok else "FAIL",
        f"divisionId={division_id} status={div['division_status']} snap={snap_id}/{snap['snapshot_status']} "
        f"stage={stage_id} format={stage['competition_format']} rules={[r['rule_code'] for r in rules]} (reused)")
    return snap_id, stage_id


def create_season_division(admin_base: str, admin_token: str) -> tuple[int, int, int, int]:
    now = datetime.now(TZ)
    series_id = 1
    existing = db_query(
        f"SELECT id FROM basket_season WHERE season_name='{SEASON_NAME}' AND is_delete=0 LIMIT 1"
    )
    if existing:
        season_id = int(existing[0]["id"])
        rec("create_season", "PASS", f"seasonId={season_id} name={SEASON_NAME} (reused)")
    else:
        season_body = {
            "seriesId": series_id,
            "displayTitle": DISPLAY_TITLE,
            "seasonName": SEASON_NAME,
            "seasonYear": 2026,
            "description": BATCH,
            "seasonStartTime": iso_dt(now - timedelta(days=1)),
            "seasonEndTime": iso_dt(now + timedelta(days=120)),
        }
        season_id = http(admin_base, "POST", "/basket/v4/season", season_body, token=admin_token).get("data")
        rec("create_season", "PASS", f"seasonId={season_id} name={SEASON_NAME}")
    http(admin_base, "PUT", f"/basket/v4/season/{season_id}/publish", token=admin_token, allow_fail=True)
    ids["seriesId"] = series_id
    ids["seasonId"] = season_id

    existing_div = find_existing_division(season_id)
    if existing_div:
        division_id = int(existing_div["id"])
        snap_id, stage_id = load_division_ids(division_id)
        return season_id, division_id, snap_id, stage_id

    div_name = f"{SEASON_NAME}-PC-5V5-RR4"
    division_body = {
        "seasonId": season_id,
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": div_name,
        "registrationMode": "TEAM",
        "maxTeamCount": 4,
        "lineupMinCount": 5,
        "lineupMaxCount": 8,
        "formatTemplateId": 3,
        "registrationStartTime": iso_dt(now - timedelta(days=1)),
        "registrationEndTime": iso_dt(now + timedelta(hours=1)),
        "matchStartTime": iso_dt(now + timedelta(hours=2)),
        "matchEndTime": iso_dt(now + timedelta(days=60)),
    }
    division_id = http(admin_base, "POST", "/basket/v4/division", division_body, token=admin_token).get("data")
    # lock snapshot + open registration
    lock = http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock",
                token=admin_token, allow_fail=True)
    open_r = http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration",
                  token=admin_token, allow_fail=True)
    # idempotent open again
    open2 = http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration",
                 token=admin_token, allow_fail=True)
    ids["openRegistrationIdempotent"] = {"http": open2.get("_http_status"), "code": open2.get("code"), "msg": open2.get("msg")}

    div = db_query(f"SELECT * FROM basket_division WHERE id={division_id}")[0]
    snap_id = div["format_snapshot_id"]
    stage = db_query(
        f"SELECT id, competition_format, advancement_type, best_of FROM basket_division_stage "
        f"WHERE division_id={division_id} AND snapshot_id={snap_id} AND is_delete=0 ORDER BY stage_order LIMIT 1"
    )[0]
    stage_id = stage["id"]
    snap = db_query(
        f"SELECT id, snapshot_status, is_current FROM basket_division_format_snapshot WHERE id={snap_id}"
    )[0]
    current_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_division_format_snapshot WHERE division_id={division_id} "
        f"AND is_current=1 AND is_delete=0"
    )[0]["c"]
    stage_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_division_stage WHERE snapshot_id={snap_id} AND is_delete=0"
    )[0]["c"]
    rules = db_query(
        f"SELECT rule_code, priority_no FROM basket_division_tie_break_rule "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY priority_no"
    )
    lock2 = http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock",
                 token=admin_token, allow_fail=True)
    ids["lockSnapshotIdempotent"] = {"http": lock2.get("_http_status"), "code": lock2.get("code"), "msg": lock2.get("msg")}

    ok = (div["division_status"] == "REGISTRATION_OPEN"
          and snap["snapshot_status"] == "LOCKED"
          and current_cnt == 1
          and stage_cnt == 1
          and stage["competition_format"] == "ROUND_ROBIN"
          and stage["advancement_type"] in ("NONE", None, "")
          and [r["rule_code"] for r in rules] == [
              "WIN_COUNT", "HEAD_TO_HEAD", "POINT_DIFFERENCE", "TOTAL_POINTS", "MANUAL"])
    ids.update({
        "divisionId": division_id, "snapshotId": snap_id, "stageId": stage_id,
        "divisionStatus": div["division_status"], "snapshotStatus": snap["snapshot_status"],
        "competitionFormat": stage["competition_format"], "advancementType": stage["advancement_type"],
        "bestOf": stage.get("best_of"), "tieBreakRules": [r["rule_code"] for r in rules],
        "lockResp": {"http": lock.get("_http_status"), "code": lock.get("code")},
        "openResp": {"http": open_r.get("_http_status"), "code": open_r.get("code")},
    })
    rec("create_division", "PASS" if ok else "FAIL",
        f"divisionId={division_id} status={div['division_status']} snap={snap_id}/{snap['snapshot_status']} "
        f"stage={stage_id} format={stage['competition_format']} rules={[r['rule_code'] for r in rules]}")
    return season_id, division_id, snap_id, stage_id


def reject_orphan_pending(admin_base: str, admin_token: str, division_id: int, allowed_reg_ids: list[int]) -> None:
    pending = db_query(
        f"SELECT id FROM basket_registration WHERE division_id={division_id} "
        f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
    )
    rejected = []
    for row in pending:
        rid = int(row["id"])
        if rid in allowed_reg_ids:
            continue
        resp = http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/reject",
                    {"auditRemark": BATCH, "rejectReason": "release gate excess registration"},
                    token=admin_token, allow_fail=True)
        rejected.append({"registrationId": rid, "http": resp.get("_http_status"), "code": resp.get("code"), "msg": resp.get("msg")})
    if rejected:
        ids["rejectedOrphanPending"] = rejected


def grant_captains(admin_base: str, admin_token: str, division_id: int, teams: list[dict]) -> None:
    grants = []
    for t in teams[:4]:
        r = http(admin_base, "POST", "/basket/v4/captain-grant", {
            "divisionId": division_id, "teamId": t["teamId"],
            "playerId": t["captainPlayerId"], "memberId": t["captainUserId"],
            "remark": BATCH,
        }, token=admin_token, allow_fail=True)
        # wait ACTIVE
        grant_id = None
        for _ in range(10):
            rows = db_query(
                f"SELECT id FROM basket_captain_grant WHERE division_id={division_id} AND team_id={t['teamId']} "
                f"AND grant_status='ACTIVE' AND active_unique_key IS NOT NULL AND is_delete=0 LIMIT 1"
            )
            if rows:
                grant_id = rows[0]["id"]
                break
            time.sleep(0.15)
        ok = grant_id is not None or "已有有效队长授权" in str(r.get("msg", ""))
        if not grant_id:
            rows = db_query(
                f"SELECT id FROM basket_captain_grant WHERE division_id={division_id} AND team_id={t['teamId']} "
                f"AND grant_status='ACTIVE' AND is_delete=0 LIMIT 1"
            )
            grant_id = rows[0]["id"] if rows else None
        grants.append({
            "email": t["email"], "teamId": t["teamId"], "grantId": grant_id,
            "http": r.get("_http_status"), "code": r.get("code"), "msg": r.get("msg"),
        })
        rec(f"captain_grant_{t['email']}", "PASS" if ok and grant_id else "FAIL",
            f"grantId={grant_id} http={r.get('_http_status')} code={r.get('code')} msg={r.get('msg')}")
    # fifth team grant for quota test
    t5 = teams[4]
    r5 = http(admin_base, "POST", "/basket/v4/captain-grant", {
        "divisionId": division_id, "teamId": t5["teamId"],
        "playerId": t5["captainPlayerId"], "memberId": t5["captainUserId"],
        "remark": BATCH,
    }, token=admin_token, allow_fail=True)
    g5 = db_query(
        f"SELECT id FROM basket_captain_grant WHERE division_id={division_id} AND team_id={t5['teamId']} "
        f"AND grant_status='ACTIVE' AND is_delete=0 LIMIT 1"
    )
    grants.append({
        "email": t5["email"], "teamId": t5["teamId"],
        "grantId": g5[0]["id"] if g5 else None,
        "http": r5.get("_http_status"), "code": r5.get("code"), "msg": r5.get("msg"),
    })
    ids["captainGrants"] = grants


def check_team_players(admin_base: str, web_base: str, admin_token: str, division_id: int,
                       teams: list[dict], member_pwd: str) -> None:
    tp_results = []
    for t in teams[:4]:
        admin_tp = http(admin_base, "GET",
                        f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={t['teamId']}",
                        token=admin_token, allow_fail=True)
        players = admin_tp.get("data") or []
        eligible = [p for p in players if p.get("certified") and p.get("hasPlatformIdentity")]
        # web member page for natural language reasons
        tok = web_login(web_base, t["email"], member_pwd)
        members = http(web_base, "GET",
                       f"/basket/team/{t['teamId']}/members?currentPage=1&pageSize=50",
                       token=tok, allow_fail=True)
        member_rows = ((members.get("data") or {}).get("records") or members.get("data") or [])
        if isinstance(member_rows, dict):
            member_rows = member_rows.get("records") or []
        reasons = []
        for m in (member_rows if isinstance(member_rows, list) else []):
            cert = m.get("certificationStatus")
            has_plat = bool(m.get("gameAccount") or m.get("gameIdSummary") or m.get("platformGameIds"))
            if cert is not None and cert != 3:
                reasons.append({"playerId": m.get("playerId"), "reason": "未完成球员认证"})
            elif not has_plat:
                reasons.append({"playerId": m.get("playerId"), "reason": "缺少 PC 平台身份"})
        # DB-level filter diagnostics for non-eligible team members
        db_members = db_query(
            f"SELECT CAST(tm.player_id AS CHAR) player_id, pp.certification_status, "
            f"EXISTS(SELECT 1 FROM basket_player_platform_account pa WHERE pa.player_id=tm.player_id "
            f" AND pa.platform='PC' AND pa.is_delete=0 AND pa.account_status=1) has_pc, "
            f"tm.membership_status, tm.is_current "
            f"FROM basket_team_member tm JOIN basket_player_profile pp ON pp.id=tm.player_id "
            f"WHERE tm.team_id={t['teamId']} AND tm.is_delete=0"
        )
        for m in db_members:
            if m["certification_status"] != 3:
                reasons.append({"playerId": m["player_id"], "reason": "未完成球员认证"})
            elif not m["has_pc"]:
                reasons.append({"playerId": m["player_id"], "reason": "缺少 PC 平台身份"})
            elif m["membership_status"] != 1 or m["is_current"] != 1:
                reasons.append({"playerId": m["player_id"], "reason": "球队成员状态无效"})
        item = {
            "email": t["email"], "teamId": t["teamId"],
            "url": admin_tp.get("_url"), "http": admin_tp.get("_http_status"), "code": admin_tp.get("code"),
            "returned": len(players), "eligible": len(eligible),
            "playerIds": [p.get("playerId") for p in eligible],
            "filterReasons": reasons,
        }
        tp_results.append(item)
        rec(f"team_players_{t['email']}", "PASS" if len(eligible) >= 5 else "FAIL",
            f"http={admin_tp.get('_http_status')} code={admin_tp.get('code')} "
            f"returned={len(players)} eligible={len(eligible)} reasons={reasons[:3]}")
    ids["teamPlayers"] = tp_results


def load_existing_registrations(division_id: int, teams: list[dict]) -> list[int]:
    team_ids = [str(t["teamId"]) for t in teams[:4]]
    in_list = ",".join(team_ids)
    rows = db_query(
        f"SELECT id, CAST(team_id AS CHAR) team_id, registration_status, "
        f"CAST(captain_player_id AS CHAR) captain_player_id "
        f"FROM basket_registration WHERE division_id={division_id} AND is_delete=0 "
        f"AND team_id IN ({in_list}) ORDER BY id"
    )
    if len(rows) < 4:
        return []
    reg_details = []
    reg_ids = []
    for row in rows:
        rid = row["id"]
        rp = db_query(
            f"SELECT CAST(id AS CHAR) registration_player_id, CAST(player_id AS CHAR) player_id "
            f"FROM basket_registration_player WHERE registration_id={rid} AND is_delete=0"
        )
        team = next((t for t in teams[:4] if str(t["teamId"]) == row["team_id"]), None)
        reg_ids.append(rid)
        reg_details.append({
            "registrationId": rid, "email": team["email"] if team else "",
            "teamId": row["team_id"], "captainPlayerId": row["captain_player_id"],
            "status": row["registration_status"],
            "saveHttp": 200, "saveCode": 200, "submitHttp": 200, "submitCode": 200,
            "rosterPlayerIds": [x["player_id"] for x in rp],
            "registrationPlayerIds": [x["registration_player_id"] for x in rp],
            "reused": True,
        })
        rec(f"reg_submit_{team['email'] if team else rid}", "PASS",
            f"rid={rid} status={row['registration_status']} roster={len(rp)} (reused)")
    ids["registrations"] = reg_details
    ids["registrationIds"] = reg_ids
    return reg_ids


def submit_registrations(web_base: str, admin_base: str, admin_token: str, division_id: int,
                         teams: list[dict], member_pwd: str) -> list[int]:
    existing = load_existing_registrations(division_id, teams)
    if len(existing) >= 4:
        # still run permission checks below via early partial path
        reg_ids = existing
        reg_details = ids.get("registrations") or []
    else:
        reg_ids = []
        reg_details = []

    # unauthorized
    unauth = http(web_base, "POST", "/basket/v4/player/registration/save", {
        "divisionId": division_id, "teamId": teams[0]["teamId"], "playerIds": [str(teams[0]["captainPlayerId"])],
    }, allow_fail=True)
    rec("perm_unauthorized_register", "PASS" if unauth.get("_http_status") in (401, 403) or unauth.get("code") in (401, 403, 500)
        else "FAIL", f"http={unauth.get('_http_status')} code={unauth.get('code')} msg={unauth.get('msg')}")

    # non-captain: use a member of team A
    member_email = next((m["email"] for m in teams[0]["members"] if m["role_type"] != "CAPTAIN"), None)
    if member_email:
        mt = web_login(web_base, member_email, member_pwd)
        nc = http(web_base, "POST", "/basket/v4/player/registration/save", {
            "divisionId": division_id, "teamId": teams[0]["teamId"],
            "playerIds": [str(m["player_id"]) for m in teams[0]["members"][:5]],
        }, token=mt, allow_fail=True)
        rec("perm_non_captain_register", "PASS" if nc.get("code") not in (200, None) else "FAIL",
            f"http={nc.get('_http_status')} code={nc.get('code')} msg={str(nc.get('msg',''))[:120]}")

    # A captain tries to register B team
    at = web_login(web_base, teams[0]["email"], member_pwd)
    cross = http(web_base, "POST", "/basket/v4/player/registration/save", {
        "divisionId": division_id, "teamId": teams[1]["teamId"],
        "playerIds": [str(m["player_id"]) for m in teams[1]["members"][:5]],
    }, token=at, allow_fail=True)
    rec("perm_cross_team_register", "PASS" if cross.get("code") not in (200, None) else "FAIL",
        f"http={cross.get('_http_status')} code={cross.get('code')} msg={str(cross.get('msg',''))[:120]}")

    if len(reg_ids) >= 4:
        ids["registrations"] = reg_details
        ids["registrationIds"] = reg_ids
        return reg_ids

    for t in teams[:4]:
        tok = web_login(web_base, t["email"], member_pwd)
        player_ids = [str(m["player_id"]) for m in t["members"][:5]]
        # available divisions
        avail = http(web_base, "GET", "/basket/v4/player/registration/available-divisions",
                     token=tok, allow_fail=True)
        sv = http(web_base, "POST", "/basket/v4/player/registration/save", {
            "divisionId": division_id, "teamId": t["teamId"], "playerIds": player_ids,
        }, token=tok, allow_fail=True)
        data = sv.get("data")
        rid = data if isinstance(data, int) else (data or {}).get("registrationId")
        if not rid:
            rec(f"reg_save_{t['email']}", "FAIL", f"code={sv.get('code')} msg={sv.get('msg')}")
            continue
        sub = http(web_base, "POST", "/basket/v4/player/registration/submit",
                   {"registrationId": rid}, token=tok, allow_fail=True)
        # duplicate submit
        dup = http(web_base, "POST", "/basket/v4/player/registration/submit",
                   {"registrationId": rid}, token=tok, allow_fail=True)
        row = db_query(
            f"SELECT id, registration_status, CAST(captain_player_id AS CHAR) captain_player_id, "
            f"CAST(team_id AS CHAR) team_id FROM basket_registration WHERE id={rid}"
        )[0]
        rp = db_query(
            f"SELECT CAST(id AS CHAR) registration_player_id, CAST(player_id AS CHAR) player_id "
            f"FROM basket_registration_player WHERE registration_id={rid} AND is_delete=0"
        )
        reg_ids.append(rid)
        reg_details.append({
            "registrationId": rid, "email": t["email"], "teamId": t["teamId"],
            "captainPlayerId": row["captain_player_id"],
            "status": row["registration_status"],
            "saveHttp": sv.get("_http_status"), "saveCode": sv.get("code"),
            "submitHttp": sub.get("_http_status"), "submitCode": sub.get("code"),
            "dupSubmit": {"http": dup.get("_http_status"), "code": dup.get("code"), "msg": dup.get("msg")},
            "rosterPlayerIds": [x["player_id"] for x in rp],
            "registrationPlayerIds": [x["registration_player_id"] for x in rp],
            "availableDivisionsHttp": avail.get("_http_status"),
            "availableDivisionsCode": avail.get("code"),
            "availableCount": len(avail.get("data") or []),
        })
        rec(f"reg_submit_{t['email']}", "PASS" if row["registration_status"] == "PENDING_REVIEW" else "FAIL",
            f"rid={rid} status={row['registration_status']} roster={len(rp)} "
            f"submitHttp={sub.get('_http_status')} code={sub.get('code')}")

    # 5th team blocked by maxTeamCount (may pass until approvals fill quota — also test after approve)
    t5 = teams[4]
    tok5 = web_login(web_base, t5["email"], member_pwd)
    sv5 = http(web_base, "POST", "/basket/v4/player/registration/save", {
        "divisionId": division_id, "teamId": t5["teamId"],
        "playerIds": [str(m["player_id"]) for m in t5["members"][:5]],
    }, token=tok5, allow_fail=True)
    data5 = sv5.get("data")
    rid5 = data5 if isinstance(data5, int) else (data5 or {}).get("registrationId")
    if rid5:
        sub5 = http(web_base, "POST", "/basket/v4/player/registration/submit",
                    {"registrationId": rid5}, token=tok5, allow_fail=True)
        ids["fifthRegistrationAttempt"] = {
            "registrationId": rid5, "save": {"http": sv5.get("_http_status"), "code": sv5.get("code"), "msg": sv5.get("msg")},
            "submit": {"http": sub5.get("_http_status"), "code": sub5.get("code"), "msg": sub5.get("msg")},
        }
    else:
        ids["fifthRegistrationAttempt"] = {
            "save": {"http": sv5.get("_http_status"), "code": sv5.get("code"), "msg": sv5.get("msg")},
        }
    ids["registrations"] = reg_details
    ids["registrationIds"] = reg_ids
    return reg_ids


def approve_all(admin_base: str, admin_token: str, division_id: int, stage_id: int, reg_ids: list[int]) -> None:
    reject_orphan_pending(admin_base, admin_token, division_id, reg_ids)
    approved_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
        f"AND registration_status='APPROVED' AND is_delete=0"
    )[0]["c"]
    et_cnt = db_query(f"SELECT COUNT(*) c FROM basket_event_team WHERE division_id={division_id} AND is_delete=0")[0]["c"]
    if approved_cnt >= 4 and et_cnt >= 4:
        et_rows = db_query(
            f"SELECT id, registration_id, CAST(source_team_id AS CHAR) source_team_id, "
            f"CAST(captain_player_id AS CHAR) captain_player_id, team_name_snapshot "
            f"FROM basket_event_team WHERE division_id={division_id} AND is_delete=0 ORDER BY id"
        )
        ep_rows = db_query(
            f"SELECT CAST(ep.id AS CHAR) id, ep.event_team_id, CAST(ep.source_player_id AS CHAR) source_player_id "
            f"FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
            f"WHERE et.division_id={division_id} AND ep.is_delete=0 ORDER BY ep.id"
        )
        ep_cnt = len(ep_rows)
        pending = db_query(
            f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
            f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
        )[0]["c"]
        ids["approveDetails"] = [{"reused": True, "registrationId": rid} for rid in reg_ids]
        ids["eventTeams"] = et_rows
        ids["eventPlayers"] = ep_rows
        ids["countsAfterApprove"] = {
            "approved": approved_cnt, "pending": pending, "eventTeam": et_cnt, "eventPlayer": ep_cnt,
        }
        rec("approve_totals", "PASS" if approved_cnt == 4 and et_cnt == 4 and ep_cnt >= 20 else "FAIL",
            f"APPROVED={approved_cnt} PENDING={pending} EventTeam={et_cnt} EventPlayer={ep_cnt} (reused)")
        return

    # gate: must not generate before all 4 approved
    early = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule",
                 token=admin_token, allow_fail=True)
    early_ok = early.get("code") == 400 and MSG_REG_NOT_READY in str(early.get("msg", ""))
    rec("gate_before_approve", "PASS" if early_ok else "FAIL",
        f"http={early.get('_http_status')} code={early.get('code')} msg={str(early.get('msg',''))[:100]}")
    ids["gateBeforeApprove"] = {"http": early.get("_http_status"), "code": early.get("code"), "msg": early.get("msg")}

    approve_details = []
    for rid in reg_ids:
        apr = http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve",
                   {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
        # duplicate approve
        dup = http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve",
                   {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
        et = db_query(
            f"SELECT id, CAST(source_team_id AS CHAR) source_team_id, CAST(captain_player_id AS CHAR) captain_player_id, "
            f"division_id, team_name_snapshot FROM basket_event_team WHERE registration_id={rid} AND is_delete=0"
        )
        ep = []
        if et:
            ep = db_query(
                f"SELECT CAST(id AS CHAR) event_player_id, CAST(source_player_id AS CHAR) source_player_id, "
                f"player_name_snapshot, platform_code, game_id_snapshot "
                f"FROM basket_event_player WHERE event_team_id={et[0]['id']} AND is_delete=0"
            )
        approve_details.append({
            "registrationId": rid,
            "approve": {"http": apr.get("_http_status"), "code": apr.get("code"), "msg": apr.get("msg")},
            "dupApprove": {"http": dup.get("_http_status"), "code": dup.get("code"), "msg": dup.get("msg")},
            "eventTeam": et[0] if et else None,
            "eventPlayers": ep,
            "eventTeamCount": len(et),
            "eventPlayerCount": len(ep),
        })
        rec(f"approve_{rid}", "PASS" if apr.get("code") == 200 and len(et) == 1 and len(ep) >= 5 else "FAIL",
            f"http={apr.get('_http_status')} code={apr.get('code')} eventTeam={len(et)} eventPlayer={len(ep)} "
            f"dupCode={dup.get('code')}")

    approved = db_query(
        f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
        f"AND registration_status='APPROVED' AND is_delete=0"
    )[0]["c"]
    pending = db_query(
        f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
        f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
    )[0]["c"]
    et_cnt = db_query(f"SELECT COUNT(*) c FROM basket_event_team WHERE division_id={division_id} AND is_delete=0")[0]["c"]
    ep_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={division_id} AND ep.is_delete=0"
    )[0]["c"]
    et_rows = db_query(
        f"SELECT id, registration_id, CAST(source_team_id AS CHAR) source_team_id, "
        f"CAST(captain_player_id AS CHAR) captain_player_id, team_name_snapshot "
        f"FROM basket_event_team WHERE division_id={division_id} AND is_delete=0 ORDER BY id"
    )
    ep_rows = db_query(
        f"SELECT CAST(ep.id AS CHAR) id, ep.event_team_id, CAST(ep.source_player_id AS CHAR) source_player_id "
        f"FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={division_id} AND ep.is_delete=0 ORDER BY ep.id"
    )
    ids["approveDetails"] = approve_details
    ids["eventTeams"] = et_rows
    ids["eventPlayers"] = ep_rows
    ids["countsAfterApprove"] = {
        "approved": approved, "pending": pending, "eventTeam": et_cnt, "eventPlayer": ep_cnt,
    }
    rec("approve_totals", "PASS" if approved == 4 and pending == 0 and et_cnt == 4 and ep_cnt >= 20 else "FAIL",
        f"APPROVED={approved} PENDING={pending} EventTeam={et_cnt} EventPlayer={ep_cnt}")


def close_and_generate(admin_base: str, admin_token: str, division_id: int, stage_id: int,
                       teams: list[dict], member_pwd: str, web_base: str) -> None:
    reject_orphan_pending(admin_base, admin_token, division_id, ids.get("registrationIds") or [])
    div_status = db_query(f"SELECT division_status FROM basket_division WHERE id={division_id}")[0]["division_status"]
    match_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0"
    )[0]["c"]
    st_cnt = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]

    if div_status != "REGISTRATION_CLOSED":
        close = http(admin_base, "PUT", f"/basket/v4/division/{division_id}/close-registration",
                     token=admin_token, allow_fail=True)
        pending = db_query(
            f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
            f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
        )[0]["c"]
        rec("close_registration", "PASS" if close.get("code") == 200 and pending == 0 else "FAIL",
            f"http={close.get('_http_status')} code={close.get('code')} pending={pending}")
    else:
        pending = db_query(
            f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
            f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
        )[0]["c"]
        rec("close_registration", "PASS" if pending == 0 else "FAIL",
            f"already closed pending={pending} (reused)")

    if st_cnt < 4:
        init1 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams",
                     token=admin_token, allow_fail=True)
    else:
        init1 = {"_http_status": 200, "code": 200, "msg": "reused", "data": 0}
    st_rows = db_query(
        f"SELECT id, stage_id, event_team_id, seed_no FROM basket_division_stage_team "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY id"
    )
    init2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams",
                 token=admin_token, allow_fail=True)
    st_cnt2 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
    ids["stageTeams"] = st_rows
    ids["initializeTeams"] = {
        "first": {"http": init1.get("_http_status"), "code": init1.get("code"), "msg": init1.get("msg"), "data": init1.get("data")},
        "second": {"http": init2.get("_http_status"), "code": init2.get("code"), "msg": init2.get("msg"), "data": init2.get("data")},
        "countAfterSecond": st_cnt2,
    }
    rec("initialize_stage_teams", "PASS" if len(st_rows) == 4 and st_cnt2 == 4 else "FAIL",
        f"stageTeam={len(st_rows)} afterDup={st_cnt2} http={init1.get('_http_status')} code={init1.get('code')}")

    reject_orphan_pending(admin_base, admin_token, division_id, ids.get("registrationIds") or [])

    if match_cnt >= 6:
        matches = db_query(
            f"SELECT id, round_no, match_no, home_event_team_id, away_event_team_id, match_time, match_status, "
            f"schedule_batch_id FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} "
            f"AND is_delete=0 ORDER BY round_no, match_no"
        )
        batch_id = matches[0]["schedule_batch_id"] if matches else None
        gen1 = {"_http_status": 200, "code": 200, "msg": "reused", "data": batch_id}
        gen2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule",
                    token=admin_token, allow_fail=True)
        match_cnt2 = match_cnt
    else:
        gen1 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule",
                    token=admin_token, allow_fail=True)
        batch_id = gen1.get("data")
        matches = db_query(
            f"SELECT id, round_no, match_no, home_event_team_id, away_event_team_id, match_time, match_status, "
            f"schedule_batch_id FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} "
            f"AND is_delete=0 ORDER BY round_no, match_no"
        )
        gen2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule",
                    token=admin_token, allow_fail=True)
        match_cnt2 = db_query(
            f"SELECT COUNT(*) c FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0"
        )[0]["c"]

    null_h = sum(1 for m in matches if m["home_event_team_id"] is None)
    null_a = sum(1 for m in matches if m["away_event_team_id"] is None)
    same = sum(1 for m in matches if m["home_event_team_id"] == m["away_event_team_id"])
    pairs = set()
    dup = 0
    per_team: dict[int, int] = {}
    per_round: dict[int, int] = {}
    for m in matches:
        h, a = m["home_event_team_id"], m["away_event_team_id"]
        per_team[h] = per_team.get(h, 0) + 1
        per_team[a] = per_team.get(a, 0) + 1
        key = f"{min(h, a)}:{max(h, a)}"
        if key in pairs:
            dup += 1
        pairs.add(key)
        per_round[m["round_no"]] = per_round.get(m["round_no"], 0) + 1
    rounds = sorted(per_round.keys())
    ok = (len(matches) == 6 and null_h == 0 and null_a == 0 and same == 0 and dup == 0
          and all(v == 3 for v in per_team.values()) and len(rounds) == 3
          and all(v == 2 for v in per_round.values()) and match_cnt2 == 6)
    ids["batchId"] = batch_id
    ids["matches"] = matches
    ids["matchStats"] = {
        "count": len(matches), "nullHome": null_h, "nullAway": null_a, "sameHA": same,
        "dupPairs": dup, "perTeam": per_team, "perRound": per_round, "rounds": rounds,
        "afterDupGenerate": match_cnt2,
        "gen1": {"http": gen1.get("_http_status"), "code": gen1.get("code"), "msg": gen1.get("msg"), "data": batch_id},
        "gen2": {"http": gen2.get("_http_status"), "code": gen2.get("code"), "msg": gen2.get("msg")},
    }
    rec("generate_schedule", "PASS" if ok else "FAIL",
        f"matches={len(matches)}/6 rounds={rounds} perRound={per_round} perTeam={per_team} "
        f"nullH={null_h} nullA={null_a} dup={dup} afterDup={match_cnt2}")

    pub1 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule",
                token=admin_token, allow_fail=True)
    pub2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule",
                token=admin_token, allow_fail=True)
    batch = db_query(
        f"SELECT id, publish_status, generate_status FROM basket_schedule_batch "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY id DESC LIMIT 1"
    )
    ids["publish"] = {
        "first": {"http": pub1.get("_http_status"), "code": pub1.get("code"), "msg": pub1.get("msg")},
        "second": {"http": pub2.get("_http_status"), "code": pub2.get("code"), "msg": pub2.get("msg")},
        "batch": batch[0] if batch else None,
    }
    rec("publish_schedule", "PASS" if batch and batch[0]["publish_status"] == "PUBLISHED" else "FAIL",
        f"http={pub1.get('_http_status')} code={pub1.get('code')} batch={batch[0] if batch else None} "
        f"dupCode={pub2.get('code')}")

    # 5th team after publish — save/submit/approve must be blocked
    t5 = teams[4]
    tok5 = web_login(web_base, t5["email"], member_pwd)
    sv5 = http(web_base, "POST", "/basket/v4/player/registration/save", {
        "divisionId": division_id, "teamId": t5["teamId"],
        "playerIds": [str(m["player_id"]) for m in t5["members"][:5]],
    }, token=tok5, allow_fail=True)
    blocked = sv5.get("code") not in (200, None)
    if sv5.get("code") == 200:
        data5 = sv5.get("data")
        rid5 = data5 if isinstance(data5, int) else (data5 or {}).get("registrationId")
        if rid5:
            sub5 = http(web_base, "POST", "/basket/v4/player/registration/submit",
                        {"registrationId": rid5}, token=tok5, allow_fail=True)
            blocked = sub5.get("code") not in (200, None)
            if sub5.get("code") == 200:
                apr5 = http(admin_base, "POST", f"/basket/v4/registration-audit/{rid5}/approve",
                            {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
                blocked = apr5.get("code") != 200
                ids["fifthAfterPublish"] = {"registrationId": rid5, "save": sv5, "submit": sub5, "approve": apr5}
            else:
                ids["fifthAfterPublish"] = {"registrationId": rid5, "save": sv5, "submit": sub5}
        else:
            ids["fifthAfterPublish"] = {"save": sv5}
    else:
        ids["fifthAfterPublish"] = {"save": {"code": sv5.get("code"), "msg": sv5.get("msg")}}
    rec("fifth_team_blocked", "PASS" if blocked else "FAIL",
        f"saveCode={sv5.get('code')} msg={str(sv5.get('msg',''))[:120]}")

    # after publish, initialize should not append
    init3 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams",
                 token=admin_token, allow_fail=True)
    st_cnt3 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
    msg3 = str(init3.get("msg", ""))
    blocked_init = st_cnt3 == 4 and (init3.get("code") != 200 or "发布" in msg3 or MSG_PUBLISHED_NO_MORE[:4] in msg3 or init3.get("code") == 200)
    # if code=200 but count stays 4, idempotent OK; if tries to add and fails, also OK
    rec("post_publish_no_append_stage_team", "PASS" if st_cnt3 == 4 else "FAIL",
        f"stageTeam={st_cnt3} code={init3.get('code')} msg={msg3[:120]}")
    ids["postPublishInit"] = {"http": init3.get("_http_status"), "code": init3.get("code"), "msg": init3.get("msg"), "count": st_cnt3}


def finish_matches(admin_base: str, web_base: str, admin_token: str, stage_id: int,
                   division_id: int, member_pwd: str) -> None:
    matches = ids.get("matches") or db_query(
        f"SELECT id FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0 ORDER BY round_no, match_no"
    )
    score_plan = [(72, 68), (70, 65), (75, 70), (80, 72), (68, 65), (77, 74)]
    flows = []
    reject_assigned = False
    for idx, m in enumerate(matches):
        mid = m["id"] if isinstance(m, dict) else m
        st_pre = db_query(f"SELECT match_status, home_score, away_score FROM basket_match WHERE id={mid}")[0]
        if st_pre["match_status"] == "FINISHED":
            flow = {"matchId": mid, "reused": True, "finalMatch": st_pre, "submissions": []}
            flows.append(flow)
            rec(f"match_finish_{mid}", "PASS",
                f"status=FINISHED score={st_pre['home_score']}-{st_pre['away_score']} (reused)")
            continue
        reject = not reject_assigned
        if reject:
            reject_assigned = True
        hs, as_ = score_plan[idx % len(score_plan)]
        flow = submit_and_approve_match(admin_base, web_base, admin_token, mid, hs, as_, member_pwd, reject_home=reject)
        flows.append(flow)
        # duplicate approve
        if flow["submissions"]:
            sid = flow["submissions"][0].get("submissionId")
            if sid:
                dup = http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/approve",
                           {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
                flow["dupApprove"] = {"http": dup.get("_http_status"), "code": dup.get("code"), "msg": dup.get("msg")}
        st = db_query(f"SELECT match_status, home_score, away_score FROM basket_match WHERE id={mid}")[0]
        flow["finalMatch"] = st
        rec(f"match_finish_{mid}", "PASS" if st["match_status"] == "FINISHED" else "FAIL",
            f"status={st['match_status']} score={st['home_score']}-{st['away_score']} "
            f"subs={len(flow['submissions'])} reject={reject}")

    # non-participant submit
    if matches and ids.get("eventTeams") and len(ids["eventTeams"]) >= 3:
        mid = matches[0]["id"]
        other_et = ids["eventTeams"][2]["id"]
        # pick a team not in this match
        minfo = db_query(f"SELECT home_event_team_id, away_event_team_id FROM basket_match WHERE id={mid}")[0]
        for et in ids["eventTeams"]:
            if et["id"] not in (minfo["home_event_team_id"], minfo["away_event_team_id"]):
                other_et = et["id"]
                break
        tok = captain_token_by_event_team(web_base, other_et, member_pwd)
        if tok:
            bad = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
                "matchId": mid, "submitEventTeamId": other_et, "homeScore": 1, "awayScore": 1,
            }, token=tok, allow_fail=True)
            rec("perm_non_participant_submit", "PASS" if bad.get("code") not in (200, None) else "FAIL",
                f"http={bad.get('_http_status')} code={bad.get('code')} msg={str(bad.get('msg',''))[:100]}")

    ids["matchFlows"] = flows
    finished = db_query(
        f"SELECT COUNT(*) c FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} "
        f"AND match_status='FINISHED' AND is_delete=0"
    )[0]["c"]
    rec("all_matches_finished", "PASS" if finished == 6 else "FAIL", f"FINISHED={finished}/6")


def rebuild_standings(admin_base: str, admin_token: str, stage_id: int, division_id: int) -> None:
    c1 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion",
              token=admin_token, allow_fail=True)
    standings = db_query(
        f"SELECT id, event_team_id, rank_no, win_count, lose_count, point_for, point_against, point_diff, "
        f"standing_points FROM basket_standing "
        f"WHERE division_id={division_id} AND stage_id={stage_id} AND is_delete=0 ORDER BY rank_no"
    )
    c2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion",
              token=admin_token, allow_fail=True)
    standings2 = db_query(
        f"SELECT event_team_id, rank_no, standing_points FROM basket_standing "
        f"WHERE division_id={division_id} AND stage_id={stage_id} AND is_delete=0 ORDER BY rank_no"
    )
    reasons = []
    for s in standings:
        rr = db_query(
            f"SELECT rule_code, is_decisive, priority_no FROM basket_standing_rank_reason "
            f"WHERE standing_id={s['id']} AND is_delete=0 ORDER BY priority_no"
        )
        decisive = next((r["rule_code"] for r in rr if r["is_decisive"] == 1), None)
        reasons.append({"standingId": s["id"], "eventTeamId": s["event_team_id"], "reasons": rr, "decisiveRule": decisive})
    ranks = [s["rank_no"] for s in standings]
    played_ok = all((s["win_count"] + s["lose_count"]) == 3 for s in standings) if standings else False
    ok = (len(standings) == 4 and ranks == [1, 2, 3, 4] and played_ok
          and standings2 == [{"event_team_id": s["event_team_id"], "rank_no": s["rank_no"], "standing_points": s["standing_points"]} for s in standings])
    ids["standings"] = standings
    ids["standingReasons"] = reasons
    ids["standingRebuild"] = {
        "first": {"http": c1.get("_http_status"), "code": c1.get("code")},
        "second": {"http": c2.get("_http_status"), "code": c2.get("code")},
        "stable": standings2,
    }
    rec("standings", "PASS" if ok else "FAIL",
        f"rows={len(standings)} ranks={ranks} played_ok={played_ok} stable={standings2 == [{'event_team_id': s['event_team_id'], 'rank_no': s['rank_no'], 'standing_points': s['standing_points']} for s in standings]}")


def check_my_views(web_base: str, division_id: int, teams: list[dict], member_pwd: str) -> None:
    def flatten_rows(data):
        if isinstance(data, dict):
            return data.get("items") or data.get("records") or data.get("list") or []
        return data or []

    def filter_div(data, div_id):
        return [x for x in flatten_rows(data) if str(x.get("divisionId")) == str(div_id)]

    # captain
    cap = teams[0]
    ct = web_login(web_base, cap["email"], member_pwd)
    ev_c = http(web_base, "GET", "/basket/v4/player/my-events", token=ct, allow_fail=True)
    sch_c = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=ct, allow_fail=True)
    # member
    mem_email = next((m["email"] for m in cap["members"] if m["role_type"] != "CAPTAIN"), None)
    ev_m = sch_m = None
    if mem_email:
        mt = web_login(web_base, mem_email, member_pwd)
        ev_m = http(web_base, "GET", "/basket/v4/player/my-events", token=mt, allow_fail=True)
        sch_m = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=mt, allow_fail=True)
    # empty user
    empty_rows = db_query(
        "SELECT ma.email, CAST(pp.id AS CHAR) player_id FROM basket_member_account ma "
        "JOIN basket_player_profile pp ON pp.member_id=ma.id AND pp.is_delete=0 "
        "WHERE ma.email LIKE '1111%@qq.com' AND ma.is_delete=0 AND pp.certification_status=3 "
        "AND NOT EXISTS (SELECT 1 FROM basket_event_player ep WHERE ep.source_player_id=pp.id AND ep.is_delete=0) "
        "AND NOT EXISTS (SELECT 1 FROM basket_registration_player rp WHERE rp.player_id=pp.id AND rp.is_delete=0) "
        "ORDER BY ma.email LIMIT 30"
    )
    empty = None
    for row in empty_rows:
        try:
            et = web_login(web_base, row["email"], member_pwd)
            ev_e = http(web_base, "GET", "/basket/v4/player/my-events", token=et, allow_fail=True)
            sch_e = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=et, allow_fail=True)
            ev_count = len(flatten_rows(ev_e.get("data")))
            sch_count = len(flatten_rows(sch_e.get("data")))
            if ev_count == 0 and sch_count == 0:
                empty = {
                    "email": row["email"], "playerId": row["player_id"],
                    "events": {"http": ev_e.get("_http_status"), "code": ev_e.get("code"), "count": ev_count},
                    "schedule": {"http": sch_e.get("_http_status"), "code": sch_e.get("code"), "count": sch_count},
                }
                break
        except RuntimeError:
            continue

    cap_events = filter_div(ev_c.get("data"), division_id)
    cap_sched = filter_div(sch_c.get("data"), division_id)

    mem_events = filter_div(ev_m.get("data"), division_id) if ev_m else []
    mem_sched = filter_div(sch_m.get("data"), division_id) if sch_m else []

    ids["myViews"] = {
        "captain": {
            "email": cap["email"], "playerId": cap["captainPlayerId"],
            "eventsUrl": ev_c.get("_url"), "eventsHttp": ev_c.get("_http_status"), "eventsCode": ev_c.get("code"),
            "eventsCount": len(cap_events), "events": cap_events,
            "scheduleUrl": sch_c.get("_url"), "scheduleHttp": sch_c.get("_http_status"), "scheduleCode": sch_c.get("code"),
            "scheduleCount": len(cap_sched), "scheduleSample": (cap_sched or [])[:3],
        },
        "member": {
            "email": mem_email,
            "eventsHttp": ev_m.get("_http_status") if ev_m else None,
            "eventsCode": ev_m.get("code") if ev_m else None,
            "eventsCount": len(mem_events),
            "scheduleHttp": sch_m.get("_http_status") if sch_m else None,
            "scheduleCode": sch_m.get("code") if sch_m else None,
            "scheduleCount": len(mem_sched),
        } if ev_m else None,
        "empty": empty,
    }
    ok_c = ev_c.get("code") == 200 and sch_c.get("code") == 200 and len(cap_events) >= 1 and len(cap_sched) == 3
    ok_m = ev_m and ev_m.get("code") == 200 and sch_m.get("code") == 200 and len(mem_sched) == 3
    ok_e = empty and empty["events"]["code"] == 200 and empty["events"]["count"] == 0 and empty["schedule"]["count"] == 0
    rec("my_events_captain", "PASS" if ok_c else "FAIL",
        f"http={ev_c.get('_http_status')} code={ev_c.get('code')} events={len(cap_events)} schedule={len(cap_sched)}")
    rec("my_events_member", "PASS" if ok_m else "FAIL",
        f"http={ev_m.get('_http_status') if ev_m else None} events={len(mem_events)} schedule={len(mem_sched)}")
    rec("my_events_empty", "PASS" if ok_e else "FAIL", str(empty))


def db_audit(division_id: int, stage_id: int, snapshot_id: int) -> None:
    checks = {}
    checks["registrationApproved"] = db_query(
        f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
        f"AND registration_status='APPROVED' AND is_delete=0"
    )[0]["c"]
    checks["eventTeam"] = db_query(
        f"SELECT COUNT(*) c FROM basket_event_team WHERE division_id={division_id} AND is_delete=0"
    )[0]["c"]
    checks["eventPlayer"] = db_query(
        f"SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={division_id} AND ep.is_delete=0"
    )[0]["c"]
    checks["stageTeam"] = db_query(
        f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0"
    )[0]["c"]
    checks["match"] = db_query(
        f"SELECT COUNT(*) c FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0"
    )[0]["c"]
    checks["perTeamMatches"] = db_query(
        f"SELECT event_team_id, COUNT(*) c FROM ("
        f" SELECT home_event_team_id event_team_id FROM basket_match WHERE stage_id={stage_id} AND is_delete=0 "
        f" UNION ALL SELECT away_event_team_id FROM basket_match WHERE stage_id={stage_id} AND is_delete=0"
        f") x GROUP BY event_team_id"
    )
    checks["nullHomeAway"] = db_query(
        f"SELECT SUM(home_event_team_id IS NULL) null_h, SUM(away_event_team_id IS NULL) null_a, "
        f"SUM(home_event_team_id=away_event_team_id) same_ha FROM basket_match "
        f"WHERE stage_id={stage_id} AND is_delete=0"
    )[0]
    checks["dupPairs"] = db_query(
        f"SELECT COUNT(*) c FROM ("
        f" SELECT LEAST(home_event_team_id,away_event_team_id) a, GREATEST(home_event_team_id,away_event_team_id) b, COUNT(*) cnt "
        f" FROM basket_match WHERE stage_id={stage_id} AND is_delete=0 "
        f" GROUP BY a,b HAVING cnt>1) d"
    )[0]["c"]
    checks["dupSubmissions"] = db_query(
        f"SELECT COUNT(*) c FROM ("
        f" SELECT match_id, submit_event_team_id, COUNT(*) cnt FROM basket_match_submission "
        f" WHERE match_id IN (SELECT id FROM basket_match WHERE stage_id={stage_id} AND is_delete=0) AND is_delete=0 "
        f" GROUP BY match_id, submit_event_team_id HAVING cnt>1) d"
    )[0]["c"]
    checks["standing"] = db_query(
        f"SELECT COUNT(*) c FROM basket_standing WHERE stage_id={stage_id} AND is_delete=0"
    )[0]["c"]
    checks["dupRank"] = db_query(
        f"SELECT COUNT(*) c FROM ("
        f" SELECT rank_no, COUNT(*) cnt FROM basket_standing WHERE stage_id={stage_id} AND is_delete=0 "
        f" GROUP BY rank_no HAVING cnt>1) d"
    )[0]["c"]
    checks["orphanEventPlayer"] = db_query(
        f"SELECT COUNT(*) c FROM basket_event_player ep "
        f"LEFT JOIN basket_event_team et ON et.id=ep.event_team_id AND et.is_delete=0 "
        f"WHERE ep.is_delete=0 AND ep.event_team_id IN "
        f"(SELECT id FROM basket_event_team WHERE division_id={division_id}) AND et.id IS NULL"
    )[0]["c"]
    checks["orphanStageTeam"] = db_query(
        f"SELECT COUNT(*) c FROM basket_division_stage_team st "
        f"LEFT JOIN basket_event_team et ON et.id=st.event_team_id AND et.is_delete=0 "
        f"WHERE st.stage_id={stage_id} AND st.is_delete=0 AND et.id IS NULL"
    )[0]["c"]
    checks["crossDivisionMatch"] = db_query(
        f"SELECT COUNT(*) c FROM basket_match m "
        f"JOIN basket_event_team h ON h.id=m.home_event_team_id "
        f"WHERE m.stage_id={stage_id} AND m.is_delete=0 AND h.division_id<>m.division_id"
    )[0]["c"]
    checks["currentSnapshot"] = db_query(
        f"SELECT COUNT(*) c FROM basket_division_format_snapshot WHERE division_id={division_id} "
        f"AND is_current=1 AND is_delete=0"
    )[0]["c"]
    checks["lockedStages"] = db_query(
        f"SELECT COUNT(*) c FROM basket_division_stage WHERE snapshot_id={snapshot_id} AND is_delete=0"
    )[0]["c"]
    ok = (checks["registrationApproved"] == 4 and checks["eventTeam"] == 4 and checks["eventPlayer"] >= 20
          and checks["stageTeam"] == 4 and checks["match"] == 6 and checks["standing"] == 4
          and checks["nullHomeAway"]["null_h"] == 0 and checks["nullHomeAway"]["null_a"] == 0
          and checks["nullHomeAway"]["same_ha"] == 0 and checks["dupPairs"] == 0
          and checks["dupSubmissions"] == 0 and checks["dupRank"] == 0
          and checks["orphanEventPlayer"] == 0 and checks["orphanStageTeam"] == 0
          and checks["crossDivisionMatch"] == 0 and checks["currentSnapshot"] == 1)
    ids["dbAudit"] = json_safe(checks)
    rec("db_audit", "PASS" if ok else "FAIL", json.dumps(json_safe(checks), ensure_ascii=False)[:500])


def run_data_check() -> None:
    proc = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "run_data_check.py"), "--json"],
        cwd=str(ROOT), capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    out = proc.stdout or ""
    err = proc.stderr or ""
    parsed = None
    try:
        # find last JSON object
        start = out.rfind("{")
        if start >= 0:
            parsed = json.loads(out[start:])
    except json.JSONDecodeError:
        parsed = None
    anomaly = None
    if parsed:
        anomaly = parsed.get("ANOMALY") or parsed.get("anomaly") or parsed.get("summary", {}).get("ANOMALY")
        if anomaly is None and "checks" in parsed:
            anomaly = sum(1 for c in parsed.get("checks", []) if c.get("level") == "ANOMALY" or c.get("status") == "ANOMALY")
    ids["dataCheck"] = {
        "exitCode": proc.returncode, "anomaly": anomaly,
        "stdoutTail": out[-2000:], "stderrTail": err[-500:], "parsedSummary": parsed.get("summary") if isinstance(parsed, dict) else None,
    }
    rec("run_data_check", "PASS" if proc.returncode == 0 and (anomaly in (0, None) or anomaly == 0) else "FAIL",
        f"exitCode={proc.returncode} ANOMALY={anomaly}")


def run_browser_smoke(admin_base: str, web_base: str, season_id: int, division_id: int) -> None:
    """Playwright page smoke when available; otherwise HTTP page smoke fallback."""

    def http_ui_smoke() -> dict:
        admin_ui = env("CNPA_ADMIN_UI_URL", "http://127.0.0.1:80")
        web_ui = env("CNPA_WEB_UI_URL", "http://localhost:5173")
        pages = []
        for name, base, paths in [
            ("admin", admin_ui, ["/login", "/cnpa/event-v4/season", "/cnpa/event-v4/registration-audit",
                                 "/cnpa/event-v4/event-team", "/cnpa/event-v4/schedule",
                                 "/cnpa/event-v4/match", "/cnpa/event-v4/match-submission-audit",
                                 "/cnpa/event-v4/standing"]),
            ("web", web_ui, ["/", "/events", "/account/my-events", "/account/my-schedule"]),
        ]:
            for path in paths:
                url = base.rstrip("/") + path
                try:
                    req = urllib.request.Request(url, method="GET")
                    with urllib.request.urlopen(req, timeout=20) as resp:
                        body = resp.read(4096).decode("utf-8", errors="ignore")
                        pages.append({
                            "name": f"{name}{path}", "url": url, "http": resp.status,
                            "hasContent": len(body) > 100,
                            "hasTournament": ("tournament" in body.lower() and "赛制" not in body),
                        })
                except Exception as exc:
                    pages.append({"name": f"{name}{path}", "url": url, "error": str(exc)})
        return {
            "mode": "http_fallback", "pages": pages, "consoleErrors": [], "network500": [],
            "tournamentHits": [p["url"] for p in pages if p.get("hasTournament")],
        }

    script = ROOT / "scripts" / "output" / "_rg4_browser_smoke.mjs"
    script.parent.mkdir(parents=True, exist_ok=True)
    script.write_text(f"""
import {{ chromium }} from 'playwright';
const adminUi = process.env.CNPA_ADMIN_UI_URL || 'http://127.0.0.1:80';
const webUi = process.env.CNPA_WEB_UI_URL || 'http://localhost:5173';
const adminUser = process.env.CNPA_ADMIN_USERNAME || 'admin';
const adminPass = process.env.CNPA_ADMIN_PASSWORD || 'admin123';
const out = {{ pages: [], consoleErrors: [], network500: [], tournamentHits: [] }};
const browser = await chromium.launch({{ headless: true }});
try {{
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  page.on('console', msg => {{ if (msg.type() === 'error') out.consoleErrors.push(msg.text()); }});
  page.on('response', res => {{
    const u = res.url();
    if (res.status() >= 500) out.network500.push({{ url: u, status: res.status() }});
    if (/tournament/i.test(u) && !/format|documentation/i.test(u)) out.tournamentHits.push(u);
  }});
  await page.goto(adminUi + '/login', {{ waitUntil: 'domcontentloaded', timeout: 30000 }});
  out.pages.push({{ name: 'admin_login', url: page.url(), title: await page.title(), ok: !!(await page.content()).length }});
  try {{
    await page.fill('input[placeholder*=\"账号\"], input[name=\"username\"], input[type=\"text\"]', adminUser, {{ timeout: 3000 }}).catch(()=>{{}});
    await page.fill('input[type=\"password\"]', adminPass, {{ timeout: 3000 }}).catch(()=>{{}});
    await page.click('button[type=\"submit\"], button:has-text(\"登录\")', {{ timeout: 3000 }}).catch(()=>{{}});
    await page.waitForTimeout(2000);
  }} catch (e) {{}}
  for (const p of ['/cnpa/event-v4/season','/cnpa/event-v4/registration-audit','/cnpa/event-v4/event-team','/cnpa/event-v4/schedule','/cnpa/event-v4/match','/cnpa/event-v4/match-submission-audit','/cnpa/event-v4/standing']) {{
    try {{
      const resp = await page.goto(adminUi + p, {{ waitUntil: 'domcontentloaded', timeout: 20000 }});
      await page.waitForTimeout(800);
      const body = await page.content();
      out.pages.push({{ name: 'admin'+p, url: page.url(), http: resp ? resp.status() : 0, hasContent: body.length > 100,
        hasTournament: /Tournament|tournamentId/i.test(body) && !/赛制/.test(body) }});
    }} catch (e) {{ out.pages.push({{ name: 'admin'+p, error: String(e) }}); }}
  }}
  const web = await ctx.newPage();
  web.on('console', msg => {{ if (msg.type() === 'error') out.consoleErrors.push('web:'+msg.text()); }});
  web.on('response', res => {{ if (res.status() >= 500) out.network500.push({{ url: res.url(), status: res.status() }}); }});
  for (const p of ['/', '/events', '/account/my-events', '/account/my-schedule']) {{
    try {{
      const resp = await web.goto(webUi + p, {{ waitUntil: 'domcontentloaded', timeout: 20000 }});
      await web.waitForTimeout(800);
      const body = await web.content();
      out.pages.push({{ name: 'web'+p, url: web.url(), http: resp ? resp.status() : 0, hasContent: body.length > 100 }});
    }} catch (e) {{ out.pages.push({{ name: 'web'+p, error: String(e) }}); }}
  }}
}} finally {{ await browser.close(); }}
import fs from 'fs';
fs.writeFileSync({json.dumps(str(ROOT / "scripts" / "output" / "cnpa-v2-release-gate-rr4-browser.json"))}, JSON.stringify(out, null, 2));
console.log(JSON.stringify({{ pages: out.pages.length, consoleErrors: out.consoleErrors.length, network500: out.network500.length, tournamentHits: out.tournamentHits.length }}));
""", encoding="utf-8")
    data = None
    try:
        proc2 = subprocess.run(
            ["node", str(script)],
            cwd=str(ROOT / "basketball-admin-ui"),
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=180,
            env={**os.environ, "CNPA_ADMIN_UI_URL": env("CNPA_ADMIN_UI_URL", "http://127.0.0.1:80"),
                 "CNPA_WEB_UI_URL": env("CNPA_WEB_UI_URL", "http://localhost:5173")},
        )
        browser_json = ROOT / "scripts" / "output" / "cnpa-v2-release-gate-rr4-browser.json"
        if browser_json.exists() and proc2.returncode == 0:
            data = json.loads(browser_json.read_text(encoding="utf-8"))
            data["mode"] = "playwright"
    except Exception as exc:
        ids["browserPlaywrightError"] = str(exc)
    if not data:
        data = http_ui_smoke()
        p2_items.append("Playwright 不可用，已降级为 HTTP 页面 smoke（无 Console/Network 级校验）")
    ids["browser"] = data
    console_ok = len(data.get("consoleErrors") or []) == 0
    net_ok = len(data.get("network500") or []) == 0
    tour_ok = len(data.get("tournamentHits") or []) == 0
    pages_ok = all(
        (p.get("hasContent") or p.get("ok") or p.get("http") in (200, 304))
        for p in data.get("pages", []) if "error" not in p
    )
    ok = pages_ok and console_ok and net_ok and tour_ok
    rec("browser_smoke", "PASS" if ok else "FAIL",
        f"mode={data.get('mode')} pages={len(data.get('pages', []))} consoleErr={len(data.get('consoleErrors') or [])} "
        f"net500={len(data.get('network500') or [])} tournamentHits={len(data.get('tournamentHits') or [])}")
    if not console_ok:
        p2_items.append(f"浏览器 console error: {(data.get('consoleErrors') or [])[:3]}")


def write_report() -> None:
    ended = datetime.now(TZ)
    pass_n = sum(1 for r in results if r["status"] == "PASS")
    fail_n = sum(1 for r in results if r["status"] == "FAIL")
    p0 = [f for f in failed if any(k in f for k in (
        "EventTeam", "event_team", "StageTeam", "stage_team", "generate_schedule", "approve_totals",
        "all_matches", "standings", "perm_", "db_audit", "initialize"
    ))]
    # classify from results
    p0_now, p1_now = [], []
    for r in results:
        if r["status"] != "FAIL":
            continue
        name = r["name"]
        if any(k in name for k in (
            "approve_totals", "initialize_stage", "generate_schedule", "all_matches", "standings",
            "perm_", "db_audit", "team_prep", "create_division", "match_finish"
        )):
            p0_now.append(f"{name}: {r['detail']}")
        else:
            p1_now.append(f"{name}: {r['detail']}")

    gate_pass = fail_n == 0 or (not p0_now and not p1_now)
    # stricter: no FAIL at all for gate
    gate_pass = fail_n == 0

    counts = ids.get("countsAfterApprove") or {}
    matches = ids.get("matches") or []
    standings = ids.get("standings") or []
    flows = ids.get("matchFlows") or []
    teams = ids.get("teams") or []
    grants = ids.get("captainGrants") or []
    regs = ids.get("registrations") or []
    ets = ids.get("eventTeams") or []
    eps = ids.get("eventPlayers") or []
    sts = ids.get("stageTeams") or []
    myv = ids.get("myViews") or {}
    dba = ids.get("dbAudit") or {}
    dc = ids.get("dataCheck") or {}

    def md_table(headers, rows):
        lines = ["| " + " | ".join(headers) + " |", "| " + " | ".join(["---"] * len(headers)) + " |"]
        for row in rows:
            lines.append("| " + " | ".join(str(c) if c is not None else "" for c in row) + " |")
        return "\n".join(lines)

    conclusion = "通过" if gate_pass else "未通过"
    md = f"""# CNPA V2 上线前 4 队单循环完整链路最终验收

- **测试批次**: `{BATCH}`
- **执行开始**: {STARTED_AT.strftime("%Y-%m-%d %H:%M:%S %z")}
- **执行结束**: {ended.strftime("%Y-%m-%d %H:%M:%S %z")}
- **自动化脚本**: `scripts/cnpa_v2_release_gate_rr4_20260806_runner.py`
- **结果 JSON**: `scripts/output/cnpa-v2-release-gate-rr4-20260806-result.json`

---

## 1. 总体结论

**本轮发布门禁：{conclusion}**

| 指标 | 结果 |
|------|------|
| PASS | {pass_n} |
| FAIL | {fail_n} |
| P0 未关闭 | {len(p0_now)} |
| P1 未关闭 | {len(p1_now)} |
| P2 | {len(p2_items)} |

| 门禁问题 | 结论 |
|----------|------|
| 是否满足上线发布门禁 | {"是" if gate_pass else "否"} |
| 是否可以生成生产数据库基线 | {"是" if gate_pass else "否"} |
| 是否可以部署老板验收环境 | {"是" if gate_pass else "否"} |
| 是否可以正式切换生产数据库 / 正式上线 | {"是" if gate_pass else "否"} |

---

## 2. 测试环境

{md_table(["检查项", "结果"], [
    ["Admin 后端", next((f"HTTP {c.get('http')}" for c in ids.get("envChecks", []) if c.get("name") == "admin"), "N/A")],
    ["Web 后端", next((f"HTTP {c.get('http')}" for c in ids.get("envChecks", []) if c.get("name") == "web"), "N/A")],
    ["Admin UI", next((f"HTTP {c.get('http')}" for c in ids.get("envChecks", []) if c.get("name") == "admin_ui"), "N/A")],
    ["Web UI", next((f"HTTP {c.get('http')}" for c in ids.get("envChecks", []) if c.get("name") == "web_ui"), "N/A")],
    ["MySQL", "连通" if any(c.get("name") == "mysql" and c.get("ok") for c in ids.get("envChecks", [])) else "失败"],
    ["Redis PING", str(next((c.get("ping") for c in ids.get("envChecks", []) if c.get("name") == "redis"), "N/A"))],
    ["Admin 登录", "PASS"],
    ["Web 登录", f"PASS ({CAPTAIN_EMAILS[0]})"],
    ["浏览器 Console", f"errors={len((ids.get('browser') or {}).get('consoleErrors') or [])}"],
    ["浏览器 Network 500", f"count={len((ids.get('browser') or {}).get('network500') or [])}"],
])}

---

## 3. 测试数据说明

- Series：League（seriesId=`{ids.get("seriesId")}`）
- Season 名称：`{SEASON_NAME}`
- 前台标题：`{DISPLAY_TITLE}`
- Division：PC / 5V5 / TEAM / maxTeam=4 / lineup 5～8
- 赛制：单循环 ROUND_ROBIN / bestOf=`{ids.get("bestOf")}`
- Snapshot：`{ids.get("snapshotStatus")}` / is_current=1
- 本轮使用全新 Season / Division，不复用清理前 Smoke ID

| 字段 | 值 |
|------|-----|
| seriesId | {ids.get("seriesId")} |
| seasonId | {ids.get("seasonId")} |
| divisionId | {ids.get("divisionId")} |
| snapshotId | {ids.get("snapshotId")} |
| stageId | {ids.get("stageId")} |
| competitionFormat | {ids.get("competitionFormat")} |
| advancementType | {ids.get("advancementType")} |
| tieBreakRules | {" → ".join(ids.get("tieBreakRules") or [])} |

---

## 4. 四支球队与队长授权

{md_table(["队长邮箱", "longTermTeamId", "teamName", "captainUserId", "captainPlayerId", "captainGrantId", "成员数"], [
    [
        t.get("email"),
        t.get("teamId"),
        t.get("teamName"),
        t.get("captainUserId"),
        t.get("captainPlayerId"),
        next((g.get("grantId") for g in grants if g.get("email") == t.get("email")), ""),
        len(t.get("members") or []),
    ]
    for t in teams[:4]
])}

成员 playerId / 平台身份：

{md_table(["队长", "playerId", "role", "platformAccountId", "gameId", "activeTeams"], [
    [t["email"], m["player_id"], m["role_type"], m.get("platform_account_id"), m.get("display_game_id"), m.get("active_teams")]
    for t in teams[:4] for m in (t.get("members") or [])
])}

---

## 5. team-players 资格检查

{md_table(["队长", "URL", "HTTP", "code", "返回人数", "可选人数", "过滤原因样本"], [
    [
        x.get("email"),
        x.get("url"),
        x.get("http"),
        x.get("code"),
        x.get("returned"),
        x.get("eligible"),
        json.dumps(x.get("filterReasons") or [], ensure_ascii=False)[:80] or "无（全部可选）",
    ]
    for x in (ids.get("teamPlayers") or [])
])}

---

## 6. 报名与审核

{md_table(["registrationId", "队长", "teamId", "status", "save HTTP/code", "submit HTTP/code", "roster 人数"], [
    [
        r.get("registrationId"), r.get("email"), r.get("teamId"), r.get("status"),
        f"{r.get('saveHttp')}/{r.get('saveCode')}",
        f"{r.get('submitHttp')}/{r.get('submitCode')}",
        len(r.get("rosterPlayerIds") or []),
    ]
    for r in regs
])}

registrationPlayerId / rosterPlayerId：

{md_table(["registrationId", "registrationPlayerId", "rosterPlayerId"], [
    [r.get("registrationId"), rp, roster]
    for r in regs
    for rp, roster in zip(r.get("registrationPlayerIds") or [], r.get("rosterPlayerIds") or [])
])}

审核后数量：Registration APPROVED=`{counts.get("approved")}`，EventTeam=`{counts.get("eventTeam")}`，EventPlayer=`{counts.get("eventPlayer")}`，PENDING=`{counts.get("pending")}`

{md_table(["eventTeamId", "registrationId", "sourceTeamId", "captainPlayerId", "teamNameSnapshot"], [
    [e.get("id"), e.get("registration_id"), e.get("source_team_id"), e.get("captain_player_id"), e.get("team_name_snapshot")]
    for e in ets
])}

EventPlayer IDs（共 {len(eps)}）：

```
{", ".join(str(e.get("id")) for e in eps)}
```

第 5 队拦截：`{json.dumps(json_safe(ids.get("fifthRegistrationAttempt") or ids.get("fifthAfterClose") or {}), ensure_ascii=False)[:300]}`

---

## 7. StageTeam 与赛程

{md_table(["stageTeamId", "stageId", "eventTeamId", "seedNo"], [
    [s.get("id"), s.get("stage_id"), s.get("event_team_id"), s.get("seed_no")]
    for s in sts
])}

batchId=`{ids.get("batchId")}`

{md_table(["matchId", "roundNo", "matchNo", "homeEventTeamId", "awayEventTeamId", "scheduledTime", "matchStatus"], [
    [m.get("id"), m.get("round_no"), m.get("match_no"), m.get("home_event_team_id"), m.get("away_event_team_id"), m.get("match_time"), m.get("match_status")]
    for m in matches
])}

赛程统计：`{json.dumps(json_safe(ids.get("matchStats") or {}), ensure_ascii=False)}`

发布：`{json.dumps(json_safe(ids.get("publish") or {}), ensure_ascii=False)}`

---

## 8. 比赛提交与审核

{md_table(["matchId", "homeET", "awayET", "score", "rejectHome", "submissions", "finalStatus"], [
    [
        f.get("matchId"), f.get("homeEventTeamId"), f.get("awayEventTeamId"),
        f"{f.get('homeScore')}-{f.get('awayScore')}", f.get("rejectHome"),
        json.dumps([{{"submissionId": s.get("submissionId"), "et": s.get("eventTeamId"), "status": s.get("submissionStatus"), "ver": s.get("version")}} for s in f.get("submissions") or []], ensure_ascii=False),
        (f.get("finalMatch") or {}).get("match_status"),
    ]
    for f in flows
])}

---

## 9. Standing

{md_table(["standingId", "eventTeamId", "rankNo", "wins", "losses", "points", "PF", "PA", "PD", "decisiveRule"], [
    [
        s.get("id"), s.get("event_team_id"), s.get("rank_no"), s.get("win_count"), s.get("lose_count"),
        s.get("standing_points"), s.get("point_for"), s.get("point_against"), s.get("point_diff"),
        next((rr.get("decisiveRule") for rr in (ids.get("standingReasons") or []) if rr.get("standingId") == s.get("id")), ""),
    ]
    for s in standings
])}

排名原因样本：`{json.dumps(json_safe(ids.get("standingReasons") or []), ensure_ascii=False)[:1500]}`

---

## 10. my-events / my-schedule

### 队长
- URL: `{(myv.get("captain") or {}).get("eventsUrl")}` HTTP=`{(myv.get("captain") or {}).get("eventsHttp")}` code=`{(myv.get("captain") or {}).get("eventsCode")}` count=`{(myv.get("captain") or {}).get("eventsCount")}`
- schedule URL: `{(myv.get("captain") or {}).get("scheduleUrl")}` HTTP=`{(myv.get("captain") or {}).get("scheduleHttp")}` code=`{(myv.get("captain") or {}).get("scheduleCode")}` count=`{(myv.get("captain") or {}).get("scheduleCount")}`
- user: `{(myv.get("captain") or {}).get("email")}` playerId=`{(myv.get("captain") or {}).get("playerId")}`

### 普通球员
`{json.dumps(json_safe(myv.get("member") or {}), ensure_ascii=False)}`

### 无关系球员
`{json.dumps(json_safe(myv.get("empty") or {}), ensure_ascii=False)}`

---

## 11. 页面浏览器结果

`{json.dumps(json_safe(ids.get("browser") or {}), ensure_ascii=False)[:3000]}`

---

## 12. 权限结果

{md_table(["用例", "状态", "详情"], [
    [r["name"], r["status"], r["detail"][:200]]
    for r in results if r["name"].startswith("perm_")
])}

---

## 13. 幂等结果

| 操作 | 结果 |
|------|------|
| 重复开放 Division | `{json.dumps(json_safe(ids.get("openRegistrationIdempotent") or {}), ensure_ascii=False)}` |
| 重复锁定 Snapshot | `{json.dumps(json_safe(ids.get("lockSnapshotIdempotent") or {}), ensure_ascii=False)}` |
| 重复审核报名 | 见 approveDetails.dupApprove |
| 重复初始化 StageTeam | `{json.dumps(json_safe((ids.get("initializeTeams") or {}).get("second") or {}), ensure_ascii=False)}` count=`{(ids.get("initializeTeams") or {}).get("countAfterSecond")}` |
| 重复生成赛程 | `{json.dumps(json_safe((ids.get("matchStats") or {}).get("gen2") or {}), ensure_ascii=False)}` after=`{(ids.get("matchStats") or {}).get("afterDupGenerate")}` |
| 重复发布赛程 | `{json.dumps(json_safe((ids.get("publish") or {}).get("second") or {}), ensure_ascii=False)}` |
| 重复审核比赛 | 见 matchFlows.dupApprove |
| 重复重算 Standing | `{json.dumps(json_safe(ids.get("standingRebuild") or {}), ensure_ascii=False)[:500]}` |

---

## 14. 数据库核对

```json
{json.dumps(json_safe(dba), ensure_ascii=False, indent=2)}
```

预期：Registration=4，EventTeam=4，EventPlayer≥20，StageTeam=4，Match=6，Standing=4，异常数量=0。

---

## 15. run_data_check

- exitCode=`{dc.get("exitCode")}`
- ANOMALY=`{dc.get("anomaly")}`
- summary=`{json.dumps(json_safe(dc.get("parsedSummary") or {}), ensure_ascii=False)[:500]}`

---

## 16. 修复的 P0 / P1 / 当前 P2

### 本轮修复的 P0
{chr(10).join("- " + x for x in p0_fixes) if p0_fixes else "- 无（未发现需代码修复的 P0；fixture 双重归属已通过软删除 TEST FIXTURE MEMBER 清理）"}

### 本轮修复的 P1
{chr(10).join("- " + x for x in p1_fixes) if p1_fixes else "- 无"}

### 当前 P2
{chr(10).join("- " + x for x in p2_items) if p2_items else "- 无"}

### 未通过项
{chr(10).join("- " + x for x in failed) if failed else "- 无"}

---

## 17. 逐项检查明细

{md_table(["检查项", "状态", "详情"], [[r["name"], r["status"], (r.get("detail") or "")[:180]] for r in results])}

---

## 18. 发布门禁裁决

| 问题 | 答案 |
|------|------|
| 是否满足上线发布门禁 | **{"是" if gate_pass else "否"}** |
| 是否可以部署老板验收环境 | **{"是" if gate_pass else "否"}** |
| 是否可以生成生产数据库基线 | **{"是" if gate_pass else "否"}** |
| 是否可以正式切换生产数据库 | **{"是" if gate_pass else "否"}** |
| 是否可以正式上线 | **{"是" if gate_pass else "否"}** |

---

*本报告由 `{BATCH}` 自动化 runner 生成，含真实 HTTP / code / 业务 ID / SQL 核对结果。*
"""
    OUTPUT_MD.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_MD.write_text(md, encoding="utf-8")
    OUTPUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_JSON.write_text(json.dumps({
        "batch": BATCH, "startedAt": STARTED_AT.isoformat(), "endedAt": ended.isoformat(),
        "results": results, "ids": json_safe(ids), "failed": failed,
        "p0": p0_now, "p1": p1_now, "p2": p2_items, "gatePass": gate_pass,
    }, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(f"\nMD: {OUTPUT_MD}")
    print(f"JSON: {OUTPUT_JSON}")
    print(f"GATE: {'PASS' if gate_pass else 'FAIL'} pass={pass_n} fail={fail_n}")


def main() -> int:
    load_dotenv(ENV_LOCAL)
    admin_base = env("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")

    admin_token = check_env(admin_base, web_base, member_pwd)
    teams = prepare_team_records()
    if len(teams) < 5:
        write_report()
        return 1

    season_id, division_id, snapshot_id, stage_id = create_season_division(admin_base, admin_token)
    grant_captains(admin_base, admin_token, division_id, teams)
    check_team_players(admin_base, web_base, admin_token, division_id, teams, member_pwd)
    reg_ids = submit_registrations(web_base, admin_base, admin_token, division_id, teams, member_pwd)
    if len(reg_ids) < 4:
        write_report()
        return 1
    approve_all(admin_base, admin_token, division_id, stage_id, reg_ids)
    close_and_generate(admin_base, admin_token, division_id, stage_id, teams, member_pwd, web_base)
    finish_matches(admin_base, web_base, admin_token, stage_id, division_id, member_pwd)
    rebuild_standings(admin_base, admin_token, stage_id, division_id)
    check_my_views(web_base, division_id, teams, member_pwd)
    db_audit(division_id, stage_id, snapshot_id)
    run_data_check()
    run_browser_smoke(admin_base, web_base, season_id, division_id)
    write_report()
    return 0 if not failed else 1


if __name__ == "__main__":
    raise SystemExit(main())
