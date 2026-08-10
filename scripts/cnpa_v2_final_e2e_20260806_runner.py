#!/usr/bin/env python3
"""CNPA V2 FINAL E2E 20260806 — full business chain acceptance runner."""
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
OUTPUT = ROOT / "scripts" / "output" / "cnpa-v2-final-e2e-20260806-result.json"
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
BATCH = "CNPA_V2_FINAL_E2E_20260806"
FIXTURE_GROUP = "CNPA_V2_E2E_FIXTURE_GROUP_20260806"
FIXTURE_PERSONAL = "CNPA_V2_E2E_FIXTURE_PERSONAL_20260806"
MSG_REG_NOT_READY = "当前仍有报名未处理或正式参赛队伍尚未确认，暂不能生成赛程。"
MSG_PUBLISHED_NO_MORE = "赛程已经发布，不能继续追加参赛队伍。如需调整，请先按赛事变更流程处理。"
TZ = timezone(timedelta(hours=8))
RUN_SUFFIX = datetime.now(TZ).strftime("%H%M%S")

results: list[dict] = []
ids: dict = {}
p0_fixes: list[str] = []
p1_fixes: list[str] = []
p2_items: list[str] = []


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
    mark = {"PASS": "OK", "FAIL": "!!", "MANUAL": "??", "SKIP": "--"}.get(status, "..")
    print(f"[{mark}] {name}: {detail or status}")


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
        payload = json.loads(exc.read().decode("utf-8", errors="ignore") or "{}")
        payload["_http_status"] = exc.code
        payload["_url"] = url
        if not allow_fail:
            raise RuntimeError(f"{method} {path} HTTP {exc.code}: {payload}") from exc
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
    r = http(base, "POST", "/login", {"username": env("CNPA_ADMIN_USERNAME", "admin"),
                                       "password": env("CNPA_ADMIN_PASSWORD", "admin123")})
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
            return list(cur.fetchall())
    finally:
        conn.close()


def iso_dt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


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
    out = {"matchId": match_id, "submissionIds": []}
    for et_id, hs, as_ in [(home_et, home_score, away_score), (away_et, away_score, home_score)]:
        tok = captain_token_by_event_team(web_base, et_id, member_pwd)
        if not tok:
            continue
        sv = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
            "matchId": match_id, "submitEventTeamId": et_id, "homeScore": hs, "awayScore": as_,
        }, token=tok, allow_fail=True)
        sid = sv.get("data")
        if not sid:
            continue
        http(web_base, "POST", "/basket/v4/player/match-submission/submit",
             {"submissionId": sid}, token=tok, allow_fail=True)
        out["submissionIds"].append({"submissionId": sid, "eventTeamId": et_id})
        if reject_home and et_id == home_et:
            http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/reject",
                 {"auditRemark": f"{BATCH} reject test"}, token=admin_token, allow_fail=True)
            http(web_base, "POST", "/basket/v4/player/match-submission/resubmit", {
                "submissionId": sid, "homeScore": hs, "awayScore": as_, "submitRemark": "resubmit",
            }, token=tok, allow_fail=True)
        http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/approve",
             {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
    return out


def team_player_ids(team_id: int, division_id: int, admin_base: str, admin_token: str, min_count: int) -> list[str]:
    players = http(admin_base, "GET",
                   f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={team_id}",
                   token=admin_token, allow_fail=True).get("data") or []
    if len(players) >= min_count:
        return [str(p["playerId"]) for p in players][:8]
    rows = db_query(
        f"SELECT CAST(tm.player_id AS CHAR) player_id FROM basket_team_member tm "
        f"JOIN basket_player_profile pp ON pp.id=tm.player_id "
        f"WHERE tm.team_id={team_id} AND tm.is_delete=0 AND tm.is_current=1 AND tm.membership_status=1 "
        f"AND pp.certification_status=3 ORDER BY tm.id LIMIT 8"
    )
    return [str(r["player_id"]) for r in rows]


def ensure_empty_user(web_base: str, admin_base: str, admin_token: str, member_pwd: str) -> None:
    """Ensure a login-capable player with zero event relations exists for my-events empty test."""
    rows = db_query(
        "SELECT ma.email, pp.id player_id FROM basket_member_account ma "
        "JOIN basket_player_profile pp ON pp.member_id=ma.id AND pp.is_delete=0 "
        "WHERE ma.email LIKE '1111%@qq.com' AND ma.is_delete=0 AND pp.certification_status=3 "
        "ORDER BY ma.email LIMIT 200"
    )
    reserved_prefixes = ("111160", "111161", "111162", "111163", "111164", "111165", "111166", "111167",
                         "111168", "111169", "111170", "111171", "111172", "111173", "111174", "111175",
                         "111176", "111177", "111178", "111179", "111180", "111181", "111182",
                         "111210", "111211", "111212", "111213", "111214", "111215")
    for row in rows:
        email = row["email"]
        if any(email.startswith(f"{p}@") for p in reserved_prefixes):
            continue
        try:
            web_login(web_base, email, member_pwd)
        except RuntimeError:
            continue
        pid = row["player_id"]
        has_reg = db_query(
            f"SELECT COUNT(*) c FROM basket_registration_player rp "
            f"WHERE rp.player_id={pid} AND rp.is_delete=0"
        )[0]["c"]
        has_pr = db_query(f"SELECT COUNT(*) c FROM basket_personal_registration WHERE player_id={pid} AND is_delete=0")[0]["c"]
        has_ep = db_query(f"SELECT COUNT(*) c FROM basket_event_player WHERE source_player_id={pid} AND is_delete=0")[0]["c"]
        if has_reg == 0 and has_pr == 0 and has_ep == 0:
            rec("ensure_empty_user", "PASS", f"email={email} playerId={pid}")
            ids["emptyUserEmail"] = email
            ids["emptyUserPlayerId"] = pid
            return
    rec("ensure_empty_user", "FAIL", "no suitable empty-event user found in pool")


def ensure_team_for_email(web_base: str, admin_base: str, admin_token: str, email: str, member_pwd: str,
                          min_members: int = 3) -> int | None:
    rows = db_query(
        f"SELECT t.id FROM basket_team t JOIN basket_player_profile pp ON pp.member_id=t.captain_member_id "
        f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND t.is_delete=0 LIMIT 1"
    )
    if rows:
        team_id = rows[0]["id"]
    else:
        try:
            tok = web_login(web_base, email, member_pwd)
        except RuntimeError:
            return None
        cr = http(web_base, "POST", "/basket/team/create", {
            "teamName": f"{BATCH}-{email.split('@')[0]}",
            "gameTerminal": "PS", "publicStatus": 0,
        }, token=tok, allow_fail=True)
        team_id = (cr.get("data") or {}).get("teamId") or (cr.get("data") or {}).get("id")
        if not team_id:
            return None
        http(admin_base, "POST", f"/basket/teamAudit/{team_id}/approve",
             {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
        captain_tok = tok
        member_cnt = db_query(
            f"SELECT COUNT(*) c FROM basket_team_member WHERE team_id={team_id} AND is_delete=0 AND is_current=1"
        )[0]["c"]
        if member_cnt < min_members:
            extras = db_query(
                f"SELECT pp.id AS player_id, pp.member_id, ma.email FROM basket_player_profile pp "
                f"JOIN basket_member_account ma ON ma.id=pp.member_id "
                f"WHERE pp.certification_status=3 AND ma.email LIKE '1111%@qq.com' "
                f"AND ma.email != '{email}' AND pp.is_delete=0 ORDER BY pp.id LIMIT 10"
            )
            for ex in extras:
                if member_cnt >= min_members:
                    break
                inv = http(web_base, "POST", f"/basket/team/{team_id}/invitation", {
                    "inviteePlayerId": str(ex["player_id"]),
                }, token=captain_tok, allow_fail=True)
                inv_id = (inv.get("data") or {}).get("invitationId") or (inv.get("data") or {}).get("id")
                if inv_id:
                    try:
                        mt = web_login(web_base, ex["email"], member_pwd)
                        http(web_base, "PUT", f"/basket/team/invitation/{inv_id}/respond",
                             {"action": "ACCEPT"}, token=mt, allow_fail=True)
                        member_cnt += 1
                    except RuntimeError:
                        pass
    return team_id


def create_season(admin_base: str, admin_token: str, marker: str, fresh: bool = True) -> int:
    title = f"{marker}-{RUN_SUFFIX}" if fresh else marker
    ex = db_query(f"SELECT id FROM basket_season WHERE display_title='{title}' AND is_delete=0 LIMIT 1")
    if ex and not fresh:
        return ex[0]["id"]
    now = datetime.now(TZ)
    sid = http(admin_base, "POST", "/basket/v4/season", {
        "seriesId": 1, "displayTitle": title, "seasonName": title, "seasonYear": 2026,
        "description": f"{BATCH} E2E",
        "seasonStartTime": iso_dt(now - timedelta(days=30)),
        "seasonEndTime": iso_dt(now + timedelta(days=120)),
    }, token=admin_token).get("data")
    http(admin_base, "PUT", f"/basket/v4/season/{sid}/publish", token=admin_token, allow_fail=True)
    return sid


def close_registration(admin_base: str, admin_token: str, division_id: int) -> None:
    http(admin_base, "PUT", f"/basket/v4/division/{division_id}/close-registration",
         token=admin_token, allow_fail=True)


def assert_no_pending_review(division_id: int) -> bool:
    pending = db_query(
        f"SELECT COUNT(*) c FROM basket_registration WHERE division_id={division_id} "
        f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
    )[0]["c"]
    pending += db_query(
        f"SELECT COUNT(*) c FROM basket_personal_registration WHERE division_id={division_id} "
        f"AND registration_status='PENDING_REVIEW' AND is_delete=0"
    )[0]["c"]
    return pending == 0


def create_division(admin_base: str, admin_token: str, season_id: int, div_name: str,
                    platform: str, game_mode: str, reg_mode: str, max_teams: int,
                    template_id: int | None = None, plan_code: str | None = None,
                    force_recreate: bool = False) -> int:
    unique_name = f"{div_name}-{RUN_SUFFIX}"
    ex = db_query(f"SELECT id FROM basket_division WHERE division_name='{unique_name}' AND is_delete=0 LIMIT 1")
    now = datetime.now(TZ)
    if ex and force_recreate:
        http(admin_base, "DELETE", f"/basket/v4/division/{ex[0]['id']}", token=admin_token, allow_fail=True)
        ex = []
    if ex:
        division_id = ex[0]["id"]
    else:
        body = {
            "seasonId": season_id, "platformCode": platform, "gameMode": game_mode,
            "divisionName": unique_name, "registrationMode": reg_mode, "maxTeamCount": max_teams,
            "registrationStartTime": iso_dt(now - timedelta(days=1)),
            "registrationEndTime": iso_dt(now + timedelta(days=30)),
            "matchStartTime": iso_dt(now + timedelta(days=31)),
            "matchEndTime": iso_dt(now + timedelta(days=90)),
        }
        if template_id:
            body["formatTemplateId"] = template_id
        division_id = http(admin_base, "POST", "/basket/v4/division", body, token=admin_token).get("data")
    if plan_code:
        http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-plan/apply", {
            "planCode": plan_code, "expectedTeamCount": max_teams,
            "groupCount": 4 if plan_code == "GROUP_TO_ELIM" else None,
            "roundRobinLegs": 1,
            "qualifiersPerGroup": 2 if plan_code == "GROUP_TO_ELIM" else None,
            "eliminationPairingRule": "HIGH_VS_LOW",
            "winPoints": 2, "lossPoints": 1, "drawPoints": 0,
        }, token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock",
         token=admin_token, allow_fail=True)
    http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration",
         token=admin_token, allow_fail=True)
    return division_id


def register_team_division(web_base: str, admin_base: str, admin_token: str, division_id: int,
                           team_emails: list[str], member_pwd: str, min_players: int = 5) -> list[int]:
    reg_ids = []
    for email in team_emails:
        rows = db_query(
            f"SELECT t.id FROM basket_team t JOIN basket_player_profile pp ON pp.member_id=t.captain_member_id "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND t.is_delete=0 "
            f"ORDER BY CASE WHEN t.team_name LIKE '%{FIXTURE_GROUP}%' THEN 0 ELSE 1 END, t.id DESC LIMIT 1"
        )
        if not rows:
            ensure_team_for_email(web_base, admin_base, admin_token, email, member_pwd)
            rows = db_query(
                f"SELECT t.id FROM basket_team t JOIN basket_player_profile pp ON pp.member_id=t.captain_member_id "
                f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND t.is_delete=0 LIMIT 1"
            )
        if not rows:
            continue
        team_id = rows[0]["id"]
        existing = db_query(
            f"SELECT id, registration_status FROM basket_registration WHERE division_id={division_id} AND team_id={team_id} "
            f"AND registration_status='APPROVED' AND is_delete=0 LIMIT 1"
        )
        if existing:
            reg_ids.append(existing[0]["id"])
            continue
        pending = db_query(
            f"SELECT id FROM basket_registration WHERE division_id={division_id} AND team_id={team_id} "
            f"AND registration_status='PENDING_REVIEW' AND is_delete=0 LIMIT 1"
        )
        if pending:
            apr = http(admin_base, "POST", f"/basket/v4/registration-audit/{pending[0]['id']}/approve",
                       {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
            if apr.get("code") == 200:
                reg_ids.append(pending[0]["id"])
            continue
        draft = db_query(
            f"SELECT id FROM basket_registration WHERE division_id={division_id} AND team_id={team_id} "
            f"AND registration_status='DRAFT' AND is_delete=0 LIMIT 1"
        )
        if draft:
            tok = web_login(web_base, email, member_pwd)
            http(web_base, "POST", "/basket/v4/player/registration/submit",
                 {"registrationId": draft[0]["id"]}, token=tok, allow_fail=True)
            apr = http(admin_base, "POST", f"/basket/v4/registration-audit/{draft[0]['id']}/approve",
                       {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
            if apr.get("code") == 200:
                reg_ids.append(draft[0]["id"])
            continue
        grant = http(admin_base, "POST", "/basket/v4/captain-grant", {
            "divisionId": division_id, "teamId": team_id,
            "playerId": int(db_query(
                f"SELECT pp.id FROM basket_player_profile pp JOIN basket_member_account ma ON ma.id=pp.member_id "
                f"WHERE ma.email='{email}' LIMIT 1"
            )[0]["id"]),
            "memberId": int(db_query(f"SELECT id FROM basket_member_account WHERE email='{email}' LIMIT 1")[0]["id"]),
            "remark": BATCH,
        }, token=admin_token, allow_fail=True)
        if grant.get("code") not in (200, None) and "已有有效队长授权" not in str(grant.get("msg", "")):
            rec(f"register_grant_{email}", "FAIL", f"code={grant.get('code')} msg={str(grant.get('msg',''))[:120]}")
            continue
        grant_ok = False
        for _ in range(8):
            chk = db_query(
                f"SELECT id FROM basket_captain_grant WHERE division_id={division_id} AND team_id={team_id} "
                f"AND grant_status='ACTIVE' AND active_unique_key IS NOT NULL AND is_delete=0 LIMIT 1"
            )
            if chk:
                grant_ok = True
                break
            time.sleep(0.15)
        if not grant_ok:
            rec(f"register_grant_verify_{email}", "FAIL", f"teamId={team_id}")
            continue
        tok = web_login(web_base, email, member_pwd)
        player_ids = team_player_ids(team_id, division_id, admin_base, admin_token, min_players)
        if len(player_ids) < min_players:
            rec(f"register_roster_{email}", "FAIL", f"players={len(player_ids)} need>={min_players} teamId={team_id}")
            continue
        sv = None
        for _ in range(3):
            sv = http(web_base, "POST", "/basket/v4/player/registration/save", {
                "divisionId": division_id, "teamId": team_id, "playerIds": player_ids,
            }, token=tok, allow_fail=True)
            if sv.get("code") == 200:
                break
            time.sleep(0.2)
        if sv.get("code") != 200:
            rec(f"register_save_{email}", "FAIL", f"code={sv.get('code')} msg={str(sv.get('msg',''))[:120]}")
            continue
        data = sv.get("data")
        rid = data if isinstance(data, int) else (data or {}).get("registrationId")
        if rid:
            http(web_base, "POST", "/basket/v4/player/registration/submit", {"registrationId": rid}, token=tok, allow_fail=True)
            apr = http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve",
                 {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
            if apr.get("code") == 200:
                reg_ids.append(rid)
            else:
                rec(f"register_approve_{email}", "FAIL", f"rid={rid} code={apr.get('code')} msg={str(apr.get('msg',''))[:80]}")
    return reg_ids


def scenario_a_team_rr(admin_base: str, web_base: str, admin_token: str, member_pwd: str) -> None:
    marker = f"{BATCH}_TEAM_RR"
    season_id = create_season(admin_base, admin_token, marker)
    div_name = f"{marker}-PC-5V5-RR4"
    division_id = create_division(admin_base, admin_token, season_id, div_name,
                                  "PC", "FIVE_V_FIVE", "TEAM", 4, template_id=3)
    snap_id = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_id}")[0]["format_snapshot_id"]
    stage_id = db_query(
        f"SELECT id FROM basket_division_stage WHERE division_id={division_id} AND snapshot_id={snap_id} AND is_delete=0 ORDER BY stage_order LIMIT 1"
    )[0]["id"]
    ids.update({"teamRrSeasonId": season_id, "teamRrDivisionId": division_id,
                "teamRrSnapshotId": snap_id, "teamRrStageId": stage_id})

    emails = [f"111{176 + i}@qq.com" for i in range(4)]
    reg_ids = []
    for idx, email in enumerate(emails):
        batch_ids = register_team_division(web_base, admin_base, admin_token, division_id, [email], member_pwd, 5)
        reg_ids.extend(batch_ids)
        if idx < 3:
            blocked = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule",
                           token=admin_token, allow_fail=True)
            rec(f"team_rr_gate_before_{idx + 1}_teams", "PASS" if MSG_REG_NOT_READY in str(blocked.get("msg", "")) and blocked.get("code") == 400 else "FAIL",
                f"teams={idx + 1} code={blocked.get('code')} msg={str(blocked.get('msg', ''))[:80]}")
        reg_ids = list(dict.fromkeys(reg_ids))
    et_rows = db_query(f"SELECT id, registration_id FROM basket_event_team WHERE division_id={division_id} AND is_delete=0")
    ep_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={division_id} AND ep.is_delete=0"
    )[0]["c"]
    ids["teamRrRegistrationIds"] = reg_ids
    ids["teamRrEventTeamIds"] = [r["id"] for r in et_rows]
    ids["teamRrEventPlayerCount"] = ep_cnt
    rec("team_rr_registration", "PASS" if len(reg_ids) == 4 and len(et_rows) == 4 and ep_cnt >= 20 else "FAIL",
        f"reg={len(reg_ids)} eventTeam={len(et_rows)} eventPlayer={ep_cnt}")

    close_registration(admin_base, admin_token, division_id)
    rec("team_rr_close_registration", "PASS" if assert_no_pending_review(division_id) else "FAIL", "pending=0")

    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True)
    st_cnt = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
    rec("team_rr_stage_team", "PASS" if st_cnt == 4 else "FAIL", f"stageTeam={st_cnt}")

    gen = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    ids["teamRrBatchId"] = gen.get("data")
    matches = db_query(
        f"SELECT id, round_no, match_no, home_event_team_id, away_event_team_id, match_time, match_status "
        f"FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0 ORDER BY round_no, match_no"
    )
    ids["teamRrMatchIds"] = [m["id"] for m in matches]
    rec("team_rr_schedule", "PASS" if len(matches) == 6 and st_cnt == 4 else "FAIL",
        f"stageTeam={st_cnt} matches={len(matches)} rounds={len({m['round_no'] for m in matches})}")

    pub = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule", token=admin_token, allow_fail=True)
    rec("team_rr_publish", "PASS" if pub.get("code") == 200 else "FAIL", f"http={pub.get('_http_status')} code={pub.get('code')}")

    if len(matches) >= 2:
        m1 = matches[0]["id"]
        sub1 = submit_and_approve_match(admin_base, web_base, admin_token, m1, 72, 68, member_pwd, reject_home=True)
        ids["teamRrSubmissionFlow1"] = sub1
        rec("team_rr_submission_reject_resubmit", "PASS" if sub1.get("submissionIds") else "FAIL", str(sub1))
        m2 = matches[1]["id"]
        away_first = submit_and_approve_match(admin_base, web_base, admin_token, m2, 70, 65, member_pwd)
        ids["teamRrSubmissionFlow2"] = away_first
        rec("team_rr_submission_both_sides", "PASS" if len(away_first.get("submissionIds", [])) >= 2 else "FAIL", str(away_first))

    # Complete remaining with varied scores for standings
    score_plan = [(75, 70), (80, 72), (68, 65), (77, 74), (71, 69), (73, 71)]
    for idx, m in enumerate(matches):
        if m["match_status"] == "FINISHED" if "match_status" in m else False:
            continue
        st = db_query(f"SELECT match_status FROM basket_match WHERE id={m['id']}")[0]["match_status"]
        if st == "FINISHED":
            continue
        hs, as_ = score_plan[idx % len(score_plan)]
        submit_and_approve_match(admin_base, web_base, admin_token, m["id"], hs, as_, member_pwd)

    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)
    standings = db_query(
        f"SELECT id, event_team_id, rank_no, win_count, lose_count, point_for, point_against, point_diff, standing_points "
        f"FROM basket_standing WHERE division_id={division_id} AND stage_id={stage_id} AND is_delete=0 ORDER BY rank_no"
    )
    rules = db_query(
        f"SELECT rule_code, priority_no FROM basket_division_tie_break_rule "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY priority_no"
    )
    ids["teamRrStandings"] = standings
    ids["teamRrTieBreakRules"] = [r["rule_code"] for r in rules]
    ranks = [s["rank_no"] for s in standings]
    rec("team_rr_standings", "PASS" if len(standings) == 4 and ranks == [1, 2, 3, 4] else "FAIL",
        f"rows={len(standings)} ranks={ranks} rules={ids['teamRrTieBreakRules']}")
    if standings:
        rr = db_query(
            f"SELECT rule_code, is_decisive FROM basket_standing_rank_reason "
            f"WHERE standing_id={standings[0]['id']} AND is_delete=0 ORDER BY priority_no"
        )
        ids["teamRrRankReasonSample"] = rr
        rec("team_rr_rank_reason", "PASS" if rr else "FAIL", str(rr[:3]))


def scenario_b_group_elim(admin_base: str, web_base: str, admin_token: str, member_pwd: str) -> None:
    marker = f"{BATCH}_GROUP_ELIM"
    season_id = create_season(admin_base, admin_token, marker)
    div_name = f"{marker}-PS-3V3-GTE"
    division_id = create_division(admin_base, admin_token, season_id, div_name,
                                  "PS_XBOX", "THREE_V_THREE", "TEAM", 16, plan_code="GROUP_TO_ELIM")
    snap_id = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_id}")[0]["format_snapshot_id"]
    stages = db_query(
        f"SELECT id, stage_code, competition_format, stage_order, group_count, advancement_type, next_stage_code "
        f"FROM basket_division_stage WHERE snapshot_id={snap_id} AND is_delete=0 ORDER BY stage_order"
    )
    ids["groupElimSnapshotId"] = snap_id
    ids["groupElimStagesOnSnapshot"] = stages
    rec("group_elim_stage_count", "PASS" if len(stages) == 2 else "FAIL", f"snapshot={snap_id} stages={len(stages)} detail={stages}")
    group_stage_id = stages[0]["id"]
    elim_stage_id = stages[1]["id"] if len(stages) > 1 else None
    ids.update({"groupElimSeasonId": season_id, "groupElimDivisionId": division_id,
                "groupStageId": group_stage_id, "elimStageId": elim_stage_id})

    team_emails = [f"111{160 + i}@qq.com" for i in range(16)]
    for email in team_emails:
        rows = db_query(
            f"SELECT t.id FROM basket_team t JOIN basket_player_profile pp ON pp.member_id=t.captain_member_id "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND t.is_delete=0 "
            f"ORDER BY CASE WHEN t.team_name LIKE '%{FIXTURE_GROUP}%' THEN 0 ELSE 1 END, t.id DESC LIMIT 1"
        )
        if not rows:
            ensure_team_for_email(web_base, admin_base, admin_token, email, member_pwd)
            rows = db_query(
                f"SELECT t.id FROM basket_team t JOIN basket_player_profile pp ON pp.member_id=t.captain_member_id "
                f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND t.is_delete=0 LIMIT 1"
            )
        if not rows:
            rec(f"group_team_missing_{email}", "FAIL", "no long-term team")
            continue
        try:
            web_login(web_base, email, member_pwd)
        except RuntimeError as e:
            rec(f"group_login_{email}", "FAIL", str(e)[:80])
            continue
    reg_ids = register_team_division(web_base, admin_base, admin_token, division_id, team_emails, member_pwd, 3)
    et_rows = db_query(f"SELECT id FROM basket_event_team WHERE division_id={division_id} AND is_delete=0 ORDER BY id")
    ep_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={division_id} AND ep.is_delete=0"
    )[0]["c"]
    ids["groupElimRegistrationIds"] = reg_ids
    ids["groupElimEventTeamIds"] = [r["id"] for r in et_rows]
    rec("group_elim_16_teams", "PASS" if len(reg_ids) == 16 and len(et_rows) == 16 and ep_cnt >= 48 else "FAIL",
        f"reg={len(reg_ids)} eventTeam={len(et_rows)} eventPlayer={ep_cnt}")

    close_registration(admin_base, admin_token, division_id)
    rec("group_elim_close_registration", "PASS" if assert_no_pending_review(division_id) else "FAIL", "pending=0")

    http(admin_base, "POST", f"/basket/v4/admin/stages/{group_stage_id}/initialize-teams", token=admin_token, allow_fail=True)
    batch_resp = http(admin_base, "POST", "/basket/v4/admin/division-grouping-batches", {
        "divisionId": division_id, "stageId": group_stage_id, "resultSource": "MANUAL",
    }, token=admin_token, allow_fail=True)
    grouping_batch_id = batch_resp.get("data")
    if not grouping_batch_id:
        ex = db_query(
            f"SELECT id FROM basket_division_grouping_batch WHERE division_id={division_id} AND is_delete=0 LIMIT 1"
        )
        grouping_batch_id = ex[0]["id"] if ex else None
    ids["groupingBatchId"] = grouping_batch_id
    detail = http(admin_base, "GET", f"/basket/v4/admin/division-grouping-batches/{grouping_batch_id}",
                  token=admin_token, allow_fail=True).get("data") or {}
    groups = detail.get("groups") or []
    group_map = {}
    for g in groups:
        gid = g.get("id") or g.get("groupId")
        code = g.get("groupCode") or g.get("groupName")
        if gid and code:
            group_map[str(code).replace("Group ", "").strip()] = gid
    if len(group_map) < 4:
        groups = db_query(
            f"SELECT id, group_code, group_name FROM basket_division_group "
            f"WHERE grouping_batch_id={grouping_batch_id} AND is_delete=0 ORDER BY group_code"
        )
        group_map = {g["group_code"]: g["id"] for g in groups}
    event_team_ids = [r["id"] for r in et_rows]
    group_codes = ["A", "B", "C", "D"]
    for gi, code in enumerate(group_codes):
        gid = group_map.get(code) or group_map.get(f"Group {code}") or (groups[gi]["id"] if gi < len(groups) else None)
        if not gid:
            continue
        for et_id in event_team_ids[gi * 4:(gi + 1) * 4]:
            http(admin_base, "POST",
                 f"/basket/v4/admin/division-grouping-batches/{grouping_batch_id}/groups/{gid}/teams",
                 {"eventTeamId": et_id}, token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/admin/division-grouping-batches/{grouping_batch_id}/lock",
         token=admin_token, allow_fail=True)
    ids["groupAssignments"] = db_query(
        f"SELECT gt.id, gt.group_id, gt.event_team_id, g.group_code FROM basket_division_group_team gt "
        f"JOIN basket_division_group g ON g.id=gt.group_id WHERE g.grouping_batch_id={grouping_batch_id} AND gt.is_delete=0"
    )
    rec("group_elim_grouping", "PASS" if len(ids["groupAssignments"]) == 16 else "FAIL",
        f"assignments={len(ids['groupAssignments'])}")

    if not db_query(f"SELECT id FROM basket_schedule_batch WHERE stage_id={group_stage_id} AND is_delete=0 LIMIT 1"):
        http(admin_base, "POST", f"/basket/v4/admin/stages/{group_stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{group_stage_id}/publish-schedule", token=admin_token, allow_fail=True)
    g_matches = db_query(
        f"SELECT id, group_id, home_event_team_id, away_event_team_id FROM basket_match "
        f"WHERE stage_id={group_stage_id} AND division_id={division_id} AND is_delete=0"
    )
    ids["groupStageMatchIds"] = [m["id"] for m in g_matches]
    rec("group_elim_group_matches", "PASS" if len(g_matches) == 24 else "FAIL", f"matches={len(g_matches)}")

    for m in g_matches:
        st = db_query(f"SELECT match_status FROM basket_match WHERE id={m['id']}")[0]["match_status"]
        if st != "FINISHED":
            submit_and_approve_match(admin_base, web_base, admin_token, m["id"], 21, 18, member_pwd)

    http(admin_base, "POST", f"/basket/v4/admin/stages/{group_stage_id}/check-completion", token=admin_token, allow_fail=True)
    preview = http(admin_base, "GET", f"/basket/v4/admin/stages/{group_stage_id}/qualification-preview",
                   token=admin_token, allow_fail=True)
    preview_items = (preview.get("data") or {}).get("items") or preview.get("data") or []
    if isinstance(preview_items, dict):
        preview_items = preview_items.get("items") or []
    http(admin_base, "POST", f"/basket/v4/admin/stages/{group_stage_id}/confirm-qualification",
         token=admin_token, allow_fail=True)
    quals = db_query(
        f"SELECT id, event_team_id, group_id, source_rank, qualification_type, qualification_status "
        f"FROM basket_division_stage_qualification WHERE stage_id={group_stage_id} AND qualification_status='CONFIRMED' AND is_delete=0"
    )
    ids["groupQualificationIds"] = [q["id"] for q in quals]
    ids["groupQualifications"] = quals
    rec("group_elim_qualification", "PASS" if len(quals) == 8 else "FAIL",
        f"qualifications={len(quals)} preview={len(preview_items) if isinstance(preview_items, list) else '?'}")

    if elim_stage_id:
        http(admin_base, "POST", f"/basket/v4/admin/stages/{elim_stage_id}/initialize-teams", token=admin_token, allow_fail=True)
        elim_st = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={elim_stage_id} AND is_delete=0")[0]["c"]
        ids["elimStageTeamCount"] = elim_st
        if not db_query(f"SELECT id FROM basket_schedule_batch WHERE stage_id={elim_stage_id} AND is_delete=0 LIMIT 1"):
            http(admin_base, "POST", f"/basket/v4/admin/stages/{elim_stage_id}/generate-schedule", token=admin_token, allow_fail=True)
        http(admin_base, "POST", f"/basket/v4/admin/stages/{elim_stage_id}/publish-schedule", token=admin_token, allow_fail=True)
        elim_matches = db_query(
            f"SELECT id, round_no, home_event_team_id, away_event_team_id, source_match_id_home, source_match_id_away "
            f"FROM basket_match WHERE stage_id={elim_stage_id} AND division_id={division_id} AND is_delete=0 ORDER BY round_no, id"
        )
        ids["elimMatchIds"] = [m["id"] for m in elim_matches]
        rec("group_elim_knockout_schedule", "PASS" if len(elim_matches) == 7 else "FAIL",
            f"matches={len(elim_matches)} stageTeam={elim_st}")

        # Complete round by round
        for round_no in sorted({m["round_no"] for m in elim_matches}):
            round_ms = [m for m in elim_matches if m["round_no"] == round_no]
            for m in round_ms:
                cur = db_query(f"SELECT match_status, home_event_team_id, away_event_team_id FROM basket_match WHERE id={m['id']}")[0]
                if cur["match_status"] == "FINISHED":
                    continue
                if cur["home_event_team_id"] and cur["away_event_team_id"]:
                    submit_and_approve_match(admin_base, web_base, admin_token, m["id"], 21, 19, member_pwd)
            http(admin_base, "POST", f"/basket/v4/admin/stages/{elim_stage_id}/check-completion", token=admin_token, allow_fail=True)

        champion = db_query(
            f"SELECT m.id, m.home_event_team_id, m.away_event_team_id, m.winner_event_team_id "
            f"FROM basket_match m WHERE m.stage_id={elim_stage_id} AND m.round_no="
            f"(SELECT MAX(round_no) FROM basket_match WHERE stage_id={elim_stage_id} AND is_delete=0) "
            f"AND m.is_delete=0 LIMIT 1"
        )
        ids["groupElimChampion"] = champion[0] if champion else None
        rec("group_elim_champion", "PASS" if champion and champion[0].get("winner_event_team_id") else "FAIL",
            str(champion[0] if champion else None))


def scenario_c_personal(admin_base: str, web_base: str, admin_token: str, member_pwd: str) -> None:
    marker = f"{BATCH}_PERSONAL"
    season_id = create_season(admin_base, admin_token, marker)
    div_name = f"{marker}-PS-3V3-SE"
    division_id = create_division(admin_base, admin_token, season_id, div_name,
                                  "PS_XBOX", "THREE_V_THREE", "PERSONAL", 2, template_id=5)
    snap_id = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_id}")[0]["format_snapshot_id"]
    stage_id = db_query(f"SELECT id FROM basket_division_stage WHERE division_id={division_id} AND is_delete=0 LIMIT 1")[0]["id"]
    ids.update({"personalSeasonId": season_id, "personalDivisionId": division_id,
                "personalSnapshotId": snap_id, "personalStageId": stage_id})

    emails = [f"111{210 + i}@qq.com" for i in range(6)]
    personal_reg_ids = []
    for pe in emails:
        pt = web_login(web_base, pe, member_pwd)
        ex = db_query(
            f"SELECT pr.id FROM basket_personal_registration pr "
            f"JOIN basket_player_profile pp ON pp.id=pr.player_id "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id "
            f"WHERE pr.division_id={division_id} AND ma.email='{pe}' "
            f"AND pr.registration_status='APPROVED' AND pr.is_delete=0 LIMIT 1"
        )
        if ex:
            personal_reg_ids.append(ex[0]["id"])
            continue
        sv = http(web_base, "POST", "/basket/v4/player/personal-registration/save",
                  {"divisionId": division_id}, token=pt, allow_fail=True)
        if sv.get("code") != 200:
            rec(f"personal_save_{pe}", "FAIL", f"code={sv.get('code')} msg={str(sv.get('msg',''))[:120]}")
            continue
        data = sv.get("data")
        pid = data.get("personalRegistrationId") if isinstance(data, dict) else data
        if pid:
            http(web_base, "POST", "/basket/v4/player/personal-registration/submit",
                 {"personalRegistrationId": pid}, token=pt, allow_fail=True)
            http(admin_base, "POST", f"/basket/v4/admin/personal-registrations/{pid}/approve",
                 {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
            personal_reg_ids.append(pid)
    ids["personalRegistrationIds"] = personal_reg_ids
    rec("personal_6_regs", "PASS" if len(personal_reg_ids) == 6 else "FAIL", f"ids={personal_reg_ids}")

    draw_batch_id = None
    draw_team_ids = []
    ex_batch = db_query(
        f"SELECT id, batch_status FROM basket_personal_draw_batch WHERE division_id={division_id} AND is_delete=0 ORDER BY id DESC LIMIT 1"
    )
    if ex_batch:
        draw_batch_id = ex_batch[0]["id"]
        draw_team_ids = [r["id"] for r in db_query(
            f"SELECT id FROM basket_personal_draw_team WHERE draw_batch_id={draw_batch_id} AND is_delete=0 ORDER BY id"
        )]
    if not draw_batch_id:
        batch = http(admin_base, "POST", "/basket/v4/admin/personal-draw-batches", {
            "divisionId": division_id, "batchCode": f"{BATCH}-DRAW", "batchName": f"{BATCH}-DRAW",
            "resultSource": "MANUAL",
        }, token=admin_token, allow_fail=True)
        draw_batch_id = batch.get("data")
    if len(draw_team_ids) < 2:
        for code, name in [("T1", "E2E Alpha"), ("T2", "E2E Beta")]:
            tr = http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams", {
                "temporaryTeamCode": code, "temporaryTeamName": name, "expectedMemberCount": 3,
            }, token=admin_token, allow_fail=True)
            if tr.get("data") and tr["data"] not in draw_team_ids:
                draw_team_ids.append(tr["data"])
    batch_status = ex_batch[0]["batch_status"] if ex_batch else "DRAFT"
    if draw_team_ids and batch_status != "CONFIRMED":
        approved_regs = db_query(
            f"SELECT id FROM basket_personal_registration WHERE division_id={division_id} "
            f"AND registration_status='APPROVED' AND is_delete=0 ORDER BY id LIMIT 6"
        )
        if len(draw_team_ids) >= 2 and len(approved_regs) >= 6:
            for i, reg in enumerate(approved_regs[:3]):
                http(admin_base, "POST",
                     f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams/{draw_team_ids[0]}/assignments",
                     {"personalRegistrationId": reg["id"], "assignmentRole": "CAPTAIN" if i == 0 else "MEMBER", "sortOrder": i + 1},
                     token=admin_token, allow_fail=True)
            for i, reg in enumerate(approved_regs[3:6]):
                http(admin_base, "POST",
                     f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams/{draw_team_ids[1]}/assignments",
                     {"personalRegistrationId": reg["id"], "assignmentRole": "CAPTAIN" if i == 0 else "MEMBER", "sortOrder": i + 1},
                     token=admin_token, allow_fail=True)
            http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/confirm",
                 {"unassignedPolicy": "IGNORE"}, token=admin_token, allow_fail=True)

    ids["personalDrawBatchId"] = draw_batch_id
    ids["personalDrawTeamIds"] = draw_team_ids

    for dt in db_query(
        f"SELECT id, captain_user_id, converted_registration_id, long_term_team_id "
        f"FROM basket_personal_draw_team WHERE draw_batch_id={draw_batch_id} AND is_delete=0 ORDER BY id"
    ):
        dt_id = dt["id"]
        if dt.get("converted_registration_id"):
            continue
        cap_rows = db_query(
            f"SELECT ma.email FROM basket_personal_draw_team dt "
            f"JOIN basket_member_account ma ON ma.id=dt.captain_user_id "
            f"WHERE dt.id={dt_id} AND dt.captain_user_id IS NOT NULL LIMIT 1"
        )
        if not cap_rows:
            rec(f"personal_draw_captain_{dt_id}", "FAIL", "captain_user_id missing")
            continue
        cap = cap_rows[0]["email"]
        ct = web_login(web_base, cap, member_pwd)
        cr = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{dt_id}/create-team", {
            "teamName": f"{BATCH}-P-{dt_id}", "shortName": "E2E", "publicStatus": 0,
        }, token=ct, allow_fail=True)
        team_id = (cr.get("data") or {}).get("teamId") or (cr.get("data") or {}).get("id")
        if not team_id:
            mine = http(web_base, "GET", "/basket/team/mine?pageSize=20", token=ct, allow_fail=True)
            recs = ((mine.get("data") or {}).get("records") or [])
            if recs:
                team_id = recs[0].get("teamId")
        if team_id:
            http(web_base, "POST", "/basket/v4/player/personal-formation/bind-team",
                 {"drawTeamId": dt_id, "longTermTeamId": int(team_id)}, token=ct, allow_fail=True)
            cf = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{dt_id}/confirm",
                      token=ct, allow_fail=True)
            ids.setdefault("personalFormalRegistrationIds", []).append(cf.get("data"))

    et_cnt = db_query(f"SELECT COUNT(*) c FROM basket_event_team WHERE division_id={division_id} AND is_delete=0")[0]["c"]
    ep_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={division_id} AND ep.is_delete=0"
    )[0]["c"]
    ids["personalEventTeamCount"] = et_cnt
    ids["personalEventPlayerCount"] = ep_cnt
    rec("personal_event_teams", "PASS" if et_cnt == 2 and ep_cnt == 6 else "FAIL", f"eventTeam={et_cnt} eventPlayer={ep_cnt}")

    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True)
    if not db_query(f"SELECT id FROM basket_schedule_batch WHERE stage_id={stage_id} AND is_delete=0 LIMIT 1"):
        http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule", token=admin_token, allow_fail=True)
    p_matches = db_query(f"SELECT id FROM basket_match WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0")
    ids["personalMatchIds"] = [m["id"] for m in p_matches]
    for m in p_matches:
        st = db_query(f"SELECT match_status FROM basket_match WHERE id={m['id']}")[0]["match_status"]
        if st != "FINISHED":
            submit_and_approve_match(admin_base, web_base, admin_token, m["id"], 21, 18, member_pwd)
        rec("personal_bo1", "PASS" if len(p_matches) == 1 else "FAIL", f"matches={len(p_matches)}")


def scenario_d_tiebreak(admin_base: str, web_base: str, admin_token: str, member_pwd: str) -> None:
    """三队循环同分 + 两队直接交锋同分 HEAD_TO_HEAD 回归。"""
    marker = f"{BATCH}_TIEBREAK"
    season_id = create_season(admin_base, admin_token, marker)
    div_name = f"{marker}-PC-5V5-RR3"
    division_id = create_division(admin_base, admin_token, season_id, div_name,
                                  "PC", "FIVE_V_FIVE", "TEAM", 3, template_id=3)
    snap_id = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_id}")[0]["format_snapshot_id"]
    stage_id = db_query(
        f"SELECT id FROM basket_division_stage WHERE snapshot_id={snap_id} AND is_delete=0 ORDER BY stage_order LIMIT 1"
    )[0]["id"]
    emails3 = [f"111{180 + i}@qq.com" for i in range(3)]
    reg_ids = register_team_division(web_base, admin_base, admin_token, division_id, emails3, member_pwd, 5)
    close_registration(admin_base, admin_token, division_id)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True)
    gen = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    if gen.get("code") != 200:
        rec("tiebreak_3_setup", "FAIL", str(gen)[:200])
        return
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule", token=admin_token, allow_fail=True)
    et_map = {r["id"]: r for r in db_query(
        f"SELECT et.id, et.team_name_snapshot, ma.email FROM basket_event_team et "
        f"JOIN basket_registration r ON r.id=et.registration_id "
        f"JOIN basket_team t ON t.id=r.team_id JOIN basket_member_account ma ON ma.id=t.captain_member_id "
        f"WHERE et.division_id={division_id} AND et.is_delete=0"
    )}
    email_to_et = {}
    for et_id, row in et_map.items():
        email_to_et[row["email"]] = et_id
    ordered = [email_to_et[e] for e in emails3 if e in email_to_et]
    if len(ordered) != 3:
        rec("tiebreak_3_setup", "FAIL", f"eventTeams={ordered}")
        return
    a, b, c = ordered
    matches = db_query(
        f"SELECT id, home_event_team_id, away_event_team_id FROM basket_match "
        f"WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0"
    )
    score_plan = {
        (a, b): (71, 68), (b, a): (71, 68),
        (b, c): (69, 67), (c, b): (69, 67),
        (c, a): (69, 68), (a, c): (69, 68),
    }
    for m in matches:
        key = (m["home_event_team_id"], m["away_event_team_id"])
        rev = (m["away_event_team_id"], m["home_event_team_id"])
        hs, aw = score_plan.get(key) or score_plan.get(rev, (70, 68))
        if key not in score_plan and rev in score_plan:
            hs, aw = score_plan[rev][1], score_plan[rev][0]
        submit_and_approve_match(admin_base, web_base, admin_token, m["id"], hs, aw, member_pwd)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)
    standings = db_query(
        f"SELECT st.id, st.event_team_id, st.rank_no, st.point_diff, st.point_for, st.standing_points, st.win_count "
        f"FROM basket_standing st WHERE st.stage_id={stage_id} AND st.is_delete=0 ORDER BY st.rank_no"
    )
    rank_by_et = {s["event_team_id"]: s for s in standings}
    rank_a = rank_by_et[a]["rank_no"]
    rank_b = rank_by_et[b]["rank_no"]
    rank_c = rank_by_et[c]["rank_no"]
    reasons_a = db_query(
        f"SELECT rule_code, is_decisive FROM basket_standing_rank_reason WHERE standing_id={rank_by_et[a]['id']} AND is_delete=0"
    )
    reasons_b = db_query(
        f"SELECT rule_code, is_decisive FROM basket_standing_rank_reason WHERE standing_id={rank_by_et[b]['id']} AND is_delete=0"
    )
    reasons_c = db_query(
        f"SELECT rule_code, is_decisive FROM basket_standing_rank_reason WHERE standing_id={rank_by_et[c]['id']} AND is_delete=0"
    )
    ids["tiebreak3Standings"] = standings
    ids["tiebreak3RankReasonA"] = reasons_a
    ids["tiebreak3RankReasonB"] = reasons_b
    ids["tiebreak3RankReasonC"] = reasons_c
    h2h_decisive_3 = any(r["rule_code"] == "HEAD_TO_HEAD" and r["is_decisive"] == 1 for r in reasons_a + reasons_b + reasons_c)
    ok3 = (rank_a == 1 and rank_b == 2 and rank_c == 3
           and rank_by_et[a]["point_diff"] == 2
           and rank_by_et[b]["point_for"] == 137 and rank_by_et[b]["point_diff"] == -1
           and rank_by_et[c]["point_for"] == 136 and rank_by_et[c]["point_diff"] == -1
           and not h2h_decisive_3
           and any(r["rule_code"] == "POINT_DIFFERENCE" and r["is_decisive"] == 1 for r in reasons_a)
           and any(r["rule_code"] == "TOTAL_POINTS" and r["is_decisive"] == 1 for r in reasons_c))
    rec("tiebreak_3way_cycle", "PASS" if ok3 else "FAIL",
        f"ranks={[rank_a, rank_b, rank_c]} A_pd={rank_by_et[a]['point_diff']} B_pf={rank_by_et[b]['point_for']} C_pf={rank_by_et[c]['point_for']} h2h_decisive={h2h_decisive_3}")

    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)
    standings2 = db_query(
        f"SELECT event_team_id, rank_no FROM basket_standing WHERE stage_id={stage_id} AND is_delete=0 ORDER BY rank_no"
    )
    stable = standings2 == [{"event_team_id": s["event_team_id"], "rank_no": s["rank_no"]} for s in standings]
    rec("tiebreak_3way_rebuild_stable", "PASS" if stable else "FAIL", str(standings2))

    marker2 = f"{BATCH}_TIEBREAK2"
    season2 = create_season(admin_base, admin_token, marker2)
    div2 = create_division(admin_base, admin_token, season2, f"{marker2}-PC-5V5-RR2",
                           "PC", "FIVE_V_FIVE", "TEAM", 2, template_id=3)
    snap2 = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={div2}")[0]["format_snapshot_id"]
    stage2 = db_query(f"SELECT id FROM basket_division_stage WHERE snapshot_id={snap2} AND is_delete=0 LIMIT 1")[0]["id"]
    emails2 = [f"111{176 + i}@qq.com" for i in range(2)]
    register_team_division(web_base, admin_base, admin_token, div2, emails2, member_pwd, 5)
    close_registration(admin_base, admin_token, div2)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage2}/initialize-teams", token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage2}/generate-schedule", token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage2}/publish-schedule", token=admin_token, allow_fail=True)
    et2 = db_query(
        f"SELECT et.id, ma.email FROM basket_event_team et JOIN basket_registration r ON r.id=et.registration_id "
        f"JOIN basket_team t ON t.id=r.team_id JOIN basket_member_account ma ON ma.id=t.captain_member_id "
        f"WHERE et.division_id={div2} AND et.is_delete=0 ORDER BY et.id"
    )
    w_et = next(r["id"] for r in et2 if r["email"] == emails2[0])
    l_et = next(r["id"] for r in et2 if r["email"] == emails2[1])
    m2 = db_query(f"SELECT id, home_event_team_id, away_event_team_id FROM basket_match WHERE stage_id={stage2} AND is_delete=0 LIMIT 1")[0]
    hs, aw = (72, 70) if m2["home_event_team_id"] == w_et else (70, 72)
    submit_and_approve_match(admin_base, web_base, admin_token, m2["id"], hs, aw, member_pwd)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage2}/check-completion", token=admin_token, allow_fail=True)
    st2 = db_query(f"SELECT id, event_team_id, rank_no FROM basket_standing WHERE stage_id={stage2} AND is_delete=0 ORDER BY rank_no")
    winner = next(s for s in st2 if s["event_team_id"] == w_et)
    rr_w = db_query(
        f"SELECT rule_code, is_decisive FROM basket_standing_rank_reason WHERE standing_id={winner['id']} AND is_delete=0"
    )
    h2h_ok = any(r["rule_code"] == "HEAD_TO_HEAD" and r["is_decisive"] == 1 for r in rr_w)
    ids["tiebreak2HeadToHeadDecisive"] = rr_w
    rec("tiebreak_2way_h2h", "PASS" if winner["rank_no"] == 1 and h2h_ok else "FAIL",
        f"winnerRank={winner['rank_no']} reasons={rr_w}")


def test_my_events_schedule(web_base: str, member_pwd: str) -> None:
    cases = [
        ("team_captain", f"111176@qq.com"),
        ("team_member", f"111177@qq.com"),
        ("personal_player", f"111210@qq.com"),
        ("pending_player", f"111215@qq.com"),
        ("empty_user", ids.get("emptyUserEmail", "111150@qq.com")),
    ]
    for label, email in cases:
        try:
            tok = web_login(web_base, email, member_pwd)
        except RuntimeError as e:
            rec(f"my_login_{label}", "FAIL", str(e)[:100])
            continue
        ev = http(web_base, "GET", "/basket/v4/player/my-events", token=tok, allow_fail=True)
        sch_f = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=false", token=tok, allow_fail=True)
        sch_t = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=tok, allow_fail=True)
        ev_items = ev.get("data") or []
        sch_items_f = (sch_f.get("data") or {}).get("items") or []
        sch_items_t = (sch_t.get("data") or {}).get("items") or []
        ok = ev.get("code") == 200 and sch_f.get("code") == 200 and sch_t.get("code") == 200
        if label == "empty_user":
            ok = ok and len(ev_items) == 0
        rec(f"my_apis_{label}", "PASS" if ok else "FAIL",
            f"events http={ev.get('_http_status')} code={ev.get('code')} count={len(ev_items)} "
            f"scheduleF={len(sch_items_f)} scheduleT={len(sch_items_t)}")
        ids[f"myEvents_{label}"] = {"count": len(ev_items), "http": ev.get("_http_status")}
        ids[f"mySchedule_{label}"] = {"finishedFalse": len(sch_items_f), "finishedTrue": len(sch_items_t)}


def test_permissions(web_base: str, admin_base: str, admin_token: str, member_pwd: str) -> None:
    div_id = ids.get("teamRrDivisionId")
    if not div_id:
        return
    # non-captain submit
    bad = http(web_base, "POST", "/basket/v4/player/registration/save", {
        "divisionId": div_id,
        "teamId": db_query(f"SELECT source_team_id FROM basket_event_team WHERE division_id={div_id} LIMIT 1")[0]["source_team_id"] if db_query(f"SELECT source_team_id FROM basket_event_team WHERE division_id={div_id} LIMIT 1") else 0,
        "playerIds": [],
    }, token=web_login(web_base, "111142@qq.com", member_pwd), allow_fail=True)
    rec("perm_non_captain_reg", "PASS" if bad.get("code") != 200 else "FAIL", f"code={bad.get('code')}")
    # unauth admin
    unauth = http(admin_base, "POST", f"/basket/v4/registration-audit/1/approve", allow_fail=True)
    rec("perm_unauth_admin", "PASS" if unauth.get("_http_status") in (401, 403) or unauth.get("code") != 200 else "FAIL",
        f"http={unauth.get('_http_status')}")


def test_idempotency(admin_base: str, admin_token: str) -> None:
    stage_id = ids.get("teamRrStageId")
    rid = (ids.get("teamRrRegistrationIds") or [None])[0]
    if stage_id:
        c1 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
        http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True)
        c2 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
        rec("idempotent_stage_team", "PASS" if c1 == c2 else "FAIL", f"before={c1} after={c2}")
    if rid:
        http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve", {"auditRemark": "dup"}, token=admin_token, allow_fail=True)
        et_cnt = db_query(f"SELECT COUNT(*) c FROM basket_event_team WHERE registration_id={rid} AND is_delete=0")[0]["c"]
        rec("idempotent_reg_approve", "PASS" if et_cnt == 1 else "FAIL", f"eventTeam={et_cnt}")


def supplemental_data_checks() -> list[dict]:
    checks = []
    sqls = [
        ("reg_multi_event_team", "SELECT registration_id, COUNT(*) c FROM basket_event_team WHERE is_delete=0 GROUP BY registration_id HAVING c>1"),
        ("event_team_no_player", "SELECT et.id FROM basket_event_team et LEFT JOIN basket_event_player ep ON ep.event_team_id=et.id AND ep.is_delete=0 WHERE et.is_delete=0 AND ep.id IS NULL AND et.division_id IN (SELECT id FROM basket_division WHERE division_name LIKE '%CNPA_V2_FINAL_E2E_20260806%')"),
        ("stage_team_dup", "SELECT stage_id, event_team_id, COUNT(*) c FROM basket_division_stage_team WHERE is_delete=0 GROUP BY stage_id, event_team_id HAVING c>1"),
        ("match_null_teams", "SELECT id FROM basket_match WHERE is_delete=0 AND home_event_team_id IS NULL AND away_event_team_id IS NULL AND source_match_id_home IS NULL"),
        ("match_same_team", "SELECT id FROM basket_match WHERE is_delete=0 AND home_event_team_id=away_event_team_id AND home_event_team_id IS NOT NULL"),
        ("qual_dup", "SELECT stage_id, event_team_id, COUNT(*) c FROM basket_division_stage_qualification WHERE is_delete=0 AND qualification_status='CONFIRMED' GROUP BY stage_id, event_team_id HAVING c>1"),
        ("standing_rank_dup", "SELECT stage_id, rank_no, COUNT(*) c FROM basket_standing WHERE is_delete=0 GROUP BY stage_id, rank_no HAVING c>1"),
    ]
    for name, sql in sqls:
        rows = db_query(sql)
        checks.append({"name": name, "count": len(rows), "rows": rows[:5]})
        rec(f"supplemental_{name}", "PASS" if len(rows) == 0 else "FAIL", f"anomalies={len(rows)}")
    return checks


def run_data_check() -> dict:
    proc = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "run_data_check.py"), "--json"],
        capture_output=True, text=True, cwd=str(ROOT), env=os.environ.copy(),
    )
    out_path = ROOT / "scripts" / "output" / "r5-data-check-result.json"
    data = json.loads(out_path.read_text(encoding="utf-8")) if out_path.exists() else {}
    data["exitCode"] = proc.returncode
    return data


def check_environment(admin_base: str, web_base: str) -> None:
    for name, url in [
        ("admin8080", admin_base),
        ("web8881", web_base),
        ("adminUI80", env("CNPA_ADMIN_UI_BASE_URL", "http://127.0.0.1:80")),
        ("webUI5173", web_base.replace(":8881", ":5173")),
    ]:
        try:
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=10) as resp:
                rec(f"env_{name}", "PASS", f"HTTP {resp.status}")
        except Exception as e:
            rec(f"env_{name}", "FAIL", str(e)[:100])
    admin_token = admin_login(admin_base)
    rec("env_admin_login", "PASS", "token ok")
    web_login(web_base, "111111@qq.com", env("CNPA_MEMBER_PASSWORD", "111111aa"))
    rec("env_web_login", "PASS", "111111@qq.com ok")
    return admin_token


def inspect_snapshot_76() -> None:
    rows = db_query(
        "SELECT id, snapshot_id, stage_code, stage_name, stage_order, stage_type, competition_format, "
        "group_count, advancement_type, next_stage_code, is_delete "
        "FROM basket_division_stage WHERE snapshot_id=76 OR id IN (86,87,88) ORDER BY snapshot_id, stage_order, id"
    )
    active = [r for r in rows if r["snapshot_id"] == 76 and r["is_delete"] == 0]
    ids["snapshot76StageAudit"] = rows
    rec("snapshot_76_stage_audit", "PASS" if len(active) == 2 else "FAIL",
        f"activeOnSnapshot76={len(active)} allRows={len(rows)}")


def main() -> int:
    load_dotenv(ENV_LOCAL)
    admin_base = env("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")
    start = datetime.now(TZ).isoformat(timespec="seconds")

    inspect_snapshot_76()
    pre = run_data_check()
    rec("data_check_pre", "PASS" if pre.get("exitCode") == 0 else "FAIL", f"anomaly={pre.get('anomalyCount', '?')}")

    admin_token = check_environment(admin_base, web_base)
    fixture_proc = subprocess.run([sys.executable, str(ROOT / "scripts" / "cnpa_v2_e2e_fixture_prepare.py")],
                                    capture_output=True, text=True, cwd=str(ROOT), env=os.environ.copy())
    rec("fixture_prepare", "PASS" if fixture_proc.returncode == 0 else "FAIL",
        (fixture_proc.stdout or fixture_proc.stderr or "")[-300:])
    ensure_empty_user(web_base, admin_base, admin_token, member_pwd)

    try:
        scenario_a_team_rr(admin_base, web_base, admin_token, member_pwd)
    except Exception as e:
        rec("scenario_a", "FAIL", str(e)[:300])
    try:
        scenario_b_group_elim(admin_base, web_base, admin_token, member_pwd)
    except Exception as e:
        rec("scenario_b", "FAIL", str(e)[:300])
    try:
        scenario_d_tiebreak(admin_base, web_base, admin_token, member_pwd)
    except Exception as e:
        rec("scenario_d", "FAIL", str(e)[:300])
    try:
        scenario_c_personal(admin_base, web_base, admin_token, member_pwd)
    except Exception as e:
        rec("scenario_c", "FAIL", str(e)[:300])

    test_my_events_schedule(web_base, member_pwd)
    test_permissions(web_base, admin_base, admin_token, member_pwd)
    test_idempotency(admin_base, admin_token)
    supplemental = supplemental_data_checks()

    post = run_data_check()
    rec("data_check_post", "PASS" if post.get("exitCode") == 0 else "FAIL", f"anomaly={post.get('anomalyCount', '?')}")

    out = {
        "batchCode": BATCH,
        "startTime": start,
        "endTime": datetime.now(TZ).isoformat(timespec="seconds"),
        "results": results,
        "ids": ids,
        "supplementalChecks": supplemental,
        "dataCheckPre": pre,
        "dataCheckPost": post,
        "p0Fixes": p0_fixes,
        "p1Fixes": p1_fixes,
        "p2Items": p2_items,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(out, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(f"JSON: {OUTPUT}")
    fails = sum(1 for r in results if r["status"] == "FAIL")
    return 1 if fails else 0


if __name__ == "__main__":
    raise SystemExit(main())
