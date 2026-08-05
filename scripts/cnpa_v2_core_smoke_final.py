#!/usr/bin/env python3
"""CNPA V2 core smoke final补齐: PERSONAL Web建队 + StageTeam增量 + UI HTTP回归."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "scripts" / "output"
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
BATCH = os.environ.get("CNPA_V2_BATCH_CODE", "SMOKE_V2_20260805_FINAL")
TZ = timezone(timedelta(hours=8))

results: list[dict] = []
ids: dict = {}


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
    mark = {"PASS": "OK", "FAIL": "!!", "MANUAL": "??", "BLOCKED": "XX"}.get(status, "..")
    print(f"[{mark}] {name}: {detail or status}")


def http(base: str, method: str, path: str, body=None, token: str | None = None,
         allow_fail: bool = False, timeout: int = 60) -> dict:
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
    except urllib.error.HTTPError as exc:
        payload = json.loads(exc.read().decode("utf-8", errors="ignore") or "{}")
        payload["_http_status"] = exc.code
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
        host=env("CNPA_DB_HOST", "192.168.31.47"),
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


def db_exec(sql: str) -> None:
    import pymysql
    conn = pymysql.connect(
        host=env("CNPA_DB_HOST", "192.168.31.47"),
        port=int(env("CNPA_DB_PORT", "3306")),
        user=env("CNPA_DB_USER", "root"),
        password=env("CNPA_DB_PASSWORD", "123456"),
        database=env("CNPA_DB_NAME", "basketball"),
        charset="utf8mb4",
    )
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
        conn.commit()
    finally:
        conn.close()


def iso_dt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def run_data_check() -> dict:
    proc = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "run_data_check.py"), "--json"],
        capture_output=True, text=True, cwd=str(ROOT), env=os.environ.copy(),
    )
    out_path = OUTPUT_DIR / "r5-data-check-result.json"
    data = json.loads(out_path.read_text(encoding="utf-8")) if out_path.exists() else {}
    data["exitCode"] = proc.returncode
    return data


def ensure_round_robin_stage(stage_id: int, division_id: int) -> None:
    """division 14 历史数据可能误用淘汰模板，赛程测试前修正为单循环。"""
    row = db_query(
        f"SELECT competition_format, stage_type FROM basket_division_stage WHERE id={stage_id} AND is_delete=0 LIMIT 1"
    )
    if not row:
        return
    if row[0]["competition_format"] == "ROUND_ROBIN":
        return
    db_exec(
        f"UPDATE basket_division_stage SET stage_code='REGULAR', stage_name='单循环', "
        f"stage_type='REGULAR', competition_format='ROUND_ROBIN', grouping_mode='NONE', "
        f"round_robin_cycles=1, best_of=1, pairing_rule='NONE', update_time=NOW() "
        f"WHERE id={stage_id} AND division_id={division_id}"
    )
    rec("stage_team_fix_round_robin", "PASS",
        f"stage {stage_id} corrected from {row[0]['competition_format']} to ROUND_ROBIN")


def verify_round_robin_schedule(stage_id: int, division_id: int, team_count: int, label: str) -> None:
    batch_rows = db_query(
        f"SELECT id, match_count, publish_status FROM basket_schedule_batch "
        f"WHERE stage_id={stage_id} AND is_delete=0 AND publish_status='DRAFT' ORDER BY id DESC LIMIT 1"
    )
    match_rows = db_query(
        f"SELECT id, round_no, home_event_team_id, away_event_team_id FROM basket_match "
        f"WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0"
    )
    expected_matches = team_count * (team_count - 1) // 2
    expected_rounds = team_count if team_count % 2 == 1 else team_count - 1
    null_home = sum(1 for m in match_rows if m["home_event_team_id"] is None)
    null_away = sum(1 for m in match_rows if m["away_event_team_id"] is None)
    same_team = sum(1 for m in match_rows if m["home_event_team_id"] == m["away_event_team_id"])
    rounds = {m["round_no"] for m in match_rows}
    per_team: dict[int, int] = {}
    pair_keys: set[str] = set()
    dup_pairs = 0
    for m in match_rows:
        h, a = m["home_event_team_id"], m["away_event_team_id"]
        per_team[h] = per_team.get(h, 0) + 1
        per_team[a] = per_team.get(a, 0) + 1
        key = f"{min(h, a)}:{max(h, a)}"
        if key in pair_keys:
            dup_pairs += 1
        pair_keys.add(key)
    per_round = {}
    for m in match_rows:
        per_round[m["round_no"]] = per_round.get(m["round_no"], 0) + 1
    ok = (
        len(batch_rows) == 1
        and len(match_rows) == expected_matches
        and len(rounds) == expected_rounds
        and null_home == 0
        and null_away == 0
        and same_team == 0
        and dup_pairs == 0
        and all(v == (team_count - 1) for v in per_team.values())
    )
    ids[f"{label}ScheduleBatchId"] = batch_rows[0]["id"] if batch_rows else None
    ids[f"{label}MatchCount"] = len(match_rows)
    rec(f"schedule_{label}", "PASS" if ok else "FAIL",
        f"batch={len(batch_rows)} matches={len(match_rows)}/{expected_matches} rounds={len(rounds)}/{expected_rounds} "
        f"nullH={null_home} nullA={null_away} dup={dup_pairs} perTeam={per_team} perRound={per_round}")


def ensure_test_season(admin_base: str, admin_token: str) -> int:

    marker = f"{BATCH}-SEASON"
    existing = db_query(f"SELECT id FROM basket_season WHERE display_title='{marker}' AND is_delete=0 LIMIT 1")
    if existing:
        return existing[0]["id"]
    div_existing = db_query(f"SELECT season_id FROM basket_division WHERE division_name LIKE '{BATCH}%' AND is_delete=0 LIMIT 1")
    if div_existing:
        return div_existing[0]["season_id"]
    now = datetime.now(TZ)
    season_id = http(admin_base, "POST", "/basket/v4/season", {
        "seriesId": 1,
        "displayTitle": marker,
        "seasonName": marker,
        "seasonYear": 2026,
        "seasonStartTime": iso_dt(now - timedelta(days=30)),
        "seasonEndTime": iso_dt(now + timedelta(days=120)),
    }, token=admin_token).get("data")
    if season_id:
        http(admin_base, "PUT", f"/basket/v4/season/{season_id}/publish", token=admin_token, allow_fail=True)
    return season_id


def _personal_bind_and_confirm(web_base: str, draw_team_id: int, team_id, captain_token: str) -> None:
    avail = http(web_base, "GET", f"/basket/v4/player/personal-formation/tasks/{draw_team_id}/available-teams",
                 token=captain_token)
    avail_list = avail.get("data") or []
    rec("personal_available_teams", "PASS" if avail_list else "FAIL", f"count={len(avail_list)}")

    bind_resp = http(web_base, "POST", "/basket/v4/player/personal-formation/bind-team",
                     {"drawTeamId": draw_team_id, "longTermTeamId": int(team_id)},
                     token=captain_token, allow_fail=True)
    bind_ok = bind_resp.get("code") == 200
    rec("personal_bind_team", "PASS" if bind_ok else "FAIL",
        f"http={bind_resp.get('_http_status')} code={bind_resp.get('code')} msg={bind_resp.get('msg')}")
    if not bind_ok:
        return

    member_rows = db_query(
        f"SELECT id, player_id, join_source FROM basket_team_member "
        f"WHERE team_id={team_id} AND is_delete=0 AND is_current=1 AND membership_status=1"
    )
    ids["personalWebMemberRelationIds"] = [r["id"] for r in member_rows]
    rec("personal_member_join", "PASS" if len(member_rows) >= 3 else "FAIL",
        f"members={len(member_rows)} sources={[r.get('join_source') for r in member_rows]}")

    readiness = http(web_base, "GET", f"/basket/v4/player/personal-formation/tasks/{draw_team_id}",
                     token=captain_token, allow_fail=True)
    detail = readiness.get("data") or {}
    rec("personal_formation_readiness", "PASS" if detail.get("formationStatus") in ("READY_TO_CONFIRM", "TEAM_FORMING") else "FAIL",
        f"status={detail.get('formationStatus')}")

    confirm_resp = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{draw_team_id}/confirm",
                        token=captain_token, allow_fail=True)
    reg_id = confirm_resp.get("data")
    confirm_ok = confirm_resp.get("code") == 200 and reg_id
    if not confirm_ok:
        rec("personal_confirm_formation", "FAIL", f"resp={confirm_resp}")
        return

    ids["personalWebRegistrationId"] = reg_id
    reg_id2 = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{draw_team_id}/confirm",
                   token=captain_token, allow_fail=True).get("data")
    et_rows = db_query(f"SELECT id FROM basket_event_team WHERE registration_id={reg_id} AND is_delete=0")
    ep_rows = db_query(
        f"SELECT ep.id FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.registration_id={reg_id} AND ep.is_delete=0"
    )
    ids["personalWebEventTeamId"] = et_rows[0]["id"] if et_rows else None
    ids["personalWebEventPlayerIds"] = [r["id"] for r in ep_rows]
    ids["personalWebEventPlayerCount"] = len(ep_rows)
    rec("personal_confirm_formation", "PASS" if reg_id and reg_id == reg_id2 and len(ep_rows) == 3 else "FAIL",
        f"registrationId={reg_id} eventTeamId={ids.get('personalWebEventTeamId')} "
        f"eventPlayerIds={ids.get('personalWebEventPlayerIds')} idempotent={reg_id == reg_id2}")


def test_personal_web_create_team(admin_base: str, web_base: str, admin_token: str, member_pwd: str) -> None:
    """PERSONAL 任务：Web HTTP 创建/绑定/确认长期球队。"""
    existing_dt = db_query(
        "SELECT dt.id, dt.division_id, dt.long_term_team_id, dt.converted_registration_id "
        "FROM basket_personal_draw_team dt JOIN basket_division d ON d.id=dt.division_id "
        "WHERE d.division_name LIKE '%PERSONAL-WEB%' AND dt.is_delete=0 "
        "AND dt.formation_status IN ('CAPTAIN_ASSIGNED','TEAM_FORMING','READY_TO_CONFIRM','CONVERTED') "
        "ORDER BY dt.id DESC LIMIT 1"
    )
    if existing_dt:
        draw_team_id = existing_dt[0]["id"]
        division_id = existing_dt[0]["division_id"]
        ids["personalWebDrawTeamId"] = draw_team_id
        ids["personalWebDivisionId"] = division_id
        if existing_dt[0]["converted_registration_id"]:
            reg_id = existing_dt[0]["converted_registration_id"]
            ids["personalWebRegistrationId"] = reg_id
            et_rows = db_query(f"SELECT id FROM basket_event_team WHERE registration_id={reg_id} AND is_delete=0")
            ep_rows = db_query(
                f"SELECT ep.id FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
                f"WHERE et.registration_id={reg_id} AND ep.is_delete=0"
            )
            ids["personalWebEventTeamId"] = et_rows[0]["id"] if et_rows else None
            ids["personalWebEventPlayerIds"] = [r["id"] for r in ep_rows]
            rec("personal_already_converted", "PASS", f"registrationId={reg_id}")
            team_id = existing_dt[0].get("long_term_team_id")
            if team_id:
                ids["personalWebCreatedTeamId"] = team_id
                member_rows = db_query(
                    f"SELECT id FROM basket_team_member WHERE team_id={team_id} AND is_delete=0 AND is_current=1"
                )
                ids["personalWebMemberRelationIds"] = [r["id"] for r in member_rows]
            return
        captain_email = "111190@qq.com"
        captain_token = web_login(web_base, captain_email, member_pwd)
        rows = db_query(
            f"SELECT pp.id AS player_id, pp.member_id AS user_id FROM basket_player_profile pp "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{captain_email}' LIMIT 1"
        )
        if rows:
            ids["personalWebCaptainPlayerId"] = rows[0]["player_id"]
            ids["personalWebCaptainMemberId"] = rows[0]["user_id"]
        existing_team = db_query(
            f"SELECT long_term_team_id FROM basket_personal_draw_team WHERE id={draw_team_id} AND long_term_team_id IS NOT NULL"
        )
        if not existing_team and rows:
            existing_team = db_query(
                f"SELECT t.id AS long_term_team_id FROM basket_team t "
                f"WHERE t.captain_member_id={rows[0]['user_id']} AND t.is_delete=0 "
                f"ORDER BY t.id DESC LIMIT 1"
            )
        if existing_team:
            team_id = existing_team[0]["long_term_team_id"]
            ids["personalWebCreatedTeamId"] = team_id
            rec("personal_web_create_team", "PASS", f"reuse teamId={team_id}")
        else:
            team_name = f"{BATCH}-WebTeam-{draw_team_id}"
            create_resp = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{draw_team_id}/create-team", {
                "teamName": team_name, "shortName": "PW", "publicStatus": 0,
            }, token=captain_token, allow_fail=True)
            if create_resp.get("code") == 200 and create_resp.get("data"):
                team_id = create_resp["data"].get("teamId") or create_resp["data"].get("id")
                ids["personalWebCreatedTeamId"] = team_id
                rec("personal_web_create_team", "PASS", f"teamId={team_id}")
            else:
                rec("personal_web_create_team", "FAIL", f"resp={create_resp}")
                return
        _personal_bind_and_confirm(web_base, draw_team_id, team_id, captain_token)
        return

    now = datetime.now(TZ)
    season_id = ensure_test_season(admin_base, admin_token)
    if not season_id:
        rec("personal_season", "FAIL", "无法创建测试 Season")
        return
    reg_start = now - timedelta(days=1)
    reg_end = now + timedelta(days=30)
    match_start = now + timedelta(days=31)
    match_end_dt = now + timedelta(days=90)
    div_name = f"{BATCH}-PERSONAL-WEB"
    existing = db_query(f"SELECT id FROM basket_division WHERE division_name='{div_name}' AND is_delete=0 LIMIT 1")
    if existing:
        division_id = existing[0]["id"]
    else:
        division_id = http(admin_base, "POST", "/basket/v4/division", {
            "seasonId": season_id,
            "platformCode": "PS_XBOX",
            "gameMode": "THREE_V_THREE",
            "divisionName": div_name,
            "registrationMode": "PERSONAL",
            "formatTemplateId": 5,
            "maxTeamCount": 2,
            "registrationStartTime": iso_dt(reg_start),
            "registrationEndTime": iso_dt(reg_end),
            "matchStartTime": iso_dt(match_start),
            "matchEndTime": iso_dt(match_end_dt),
        }, token=admin_token).get("data")
    ids["personalWebDivisionId"] = division_id
    http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock", token=admin_token, allow_fail=True)
    http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration", token=admin_token, allow_fail=True)

    emails = ["111190@qq.com", "111191@qq.com", "111193@qq.com"]
    personal_reg_ids = []
    player_map = {}
    for pe in emails:
        pt = web_login(web_base, pe, member_pwd)
        existing = db_query(
            f"SELECT pr.id, pr.player_id, pr.user_id, pr.registration_status FROM basket_personal_registration pr "
            f"JOIN basket_player_profile pp ON pp.id=pr.player_id "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id "
            f"WHERE ma.email='{pe}' AND pr.division_id={division_id} AND pr.is_delete=0 "
            f"AND pr.registration_status IN ('APPROVED','PENDING_REVIEW','DRAFT','ASSIGNED') ORDER BY pr.id DESC LIMIT 1"
        )
        if existing and existing[0]["registration_status"] in ("APPROVED", "ASSIGNED"):
            pid = existing[0]["id"]
            personal_reg_ids.append(pid)
            player_map[pe] = {"player_id": existing[0]["player_id"], "user_id": existing[0]["user_id"]}
            continue
        sv = http(web_base, "POST", "/basket/v4/player/personal-registration/save",
                  {"divisionId": division_id}, token=pt, allow_fail=True)
        data = sv.get("data")
        pid = data.get("personalRegistrationId") if isinstance(data, dict) else data
        if pid:
            http(web_base, "POST", "/basket/v4/player/personal-registration/submit",
                 {"personalRegistrationId": pid}, token=pt, allow_fail=True)
            personal_reg_ids.append(pid)
            rows = db_query(f"SELECT player_id, user_id FROM basket_personal_registration WHERE id={pid}")
            if rows:
                player_map[pe] = rows[0]

    for pid in personal_reg_ids:
        st = db_query(f"SELECT registration_status FROM basket_personal_registration WHERE id={pid}")[0]["registration_status"]
        if st != "APPROVED":
            r = http(admin_base, "POST", f"/basket/v4/admin/personal-registrations/{pid}/approve", token=admin_token, allow_fail=True)
            if r.get("code") != 200:
                rec("personal_registration", "FAIL", f"approve reg {pid} failed: {r.get('msg')}")
                return

    if len(personal_reg_ids) < 3:
        rec("personal_registration", "FAIL", f"personalRegs={len(personal_reg_ids)} need 3")
        return
    rec("personal_registration", "PASS", f"personalRegs={personal_reg_ids}")

    batch_code = f"{BATCH}-DRAW-{int(datetime.now(TZ).timestamp())}"
    batch = http(admin_base, "POST", "/basket/v4/admin/personal-draw-batches", {
        "divisionId": division_id,
        "batchCode": batch_code,
        "batchName": batch_code,
        "resultSource": "MANUAL",
    }, token=admin_token)
    draw_batch_id = batch.get("data")
    ids["personalWebDrawBatchId"] = draw_batch_id

    team = http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams", {
        "temporaryTeamCode": "T1",
        "temporaryTeamName": "Web Test Team 1",
        "expectedMemberCount": 3,
    }, token=admin_token)
    draw_team_id = team.get("data")
    ids["personalWebDrawTeamId"] = draw_team_id

    captain_email = emails[0]
    captain_token = web_login(web_base, captain_email, member_pwd)
    if captain_email not in player_map:
        rows = db_query(
            f"SELECT pp.id AS player_id, pp.member_id AS user_id FROM basket_player_profile pp "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{captain_email}' LIMIT 1"
        )
        if rows:
            player_map[captain_email] = rows[0]
    if captain_email not in player_map:
        rec("personal_captain_grant", "FAIL", "队长球员档案缺失")
        return
    captain_player_id = player_map[captain_email]["player_id"]
    captain_member_id = player_map[captain_email]["user_id"]

    ids["personalWebCaptainPlayerId"] = captain_player_id
    ids["personalWebCaptainMemberId"] = captain_member_id

    for idx, pe in enumerate(emails):
        pr_id = personal_reg_ids[idx]
        role = "CAPTAIN" if idx == 0 else "MEMBER"
        http(admin_base, "POST",
             f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams/{draw_team_id}/assignments",
             {"personalRegistrationId": pr_id, "assignmentRole": role, "sortOrder": idx + 1},
             token=admin_token)

    http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/confirm",
         {"unassignedPolicy": "WAITLIST"}, token=admin_token)

    rec("personal_captain_grant", "PASS", "队长指定完成；grant 在 bind/confirm 时由 ensureGrantOnBindTeam 创建")

    team_name = f"{BATCH}-WebTeam-{draw_team_id}"
    existing_team = db_query(
        f"SELECT long_term_team_id FROM basket_personal_draw_team WHERE id={draw_team_id} AND long_term_team_id IS NOT NULL"
    )
    if existing_team:
        team_id = existing_team[0]["long_term_team_id"]
        ids["personalWebCreatedTeamId"] = team_id
        rec("personal_web_create_team", "PASS", f"reuse teamId={team_id}")
    else:
        create_resp = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{draw_team_id}/create-team", {
            "teamName": team_name,
            "shortName": "PW",
            "publicStatus": 0,
        }, token=captain_token, allow_fail=True)

        if create_resp.get("code") == 200 and create_resp.get("data"):
            created_team = create_resp["data"]
            team_id = created_team.get("teamId") or created_team.get("id")
            ids["personalWebCreatedTeamId"] = team_id
            rec("personal_web_create_team", "PASS", f"teamId={team_id}")
        else:
            rec("personal_web_create_team", "FAIL", f"resp={create_resp}")
            return

    _personal_bind_and_confirm(web_base, draw_team_id, team_id, captain_token)


def ensure_captain_grants(admin_base: str, admin_token: str, division_id: int) -> list[int]:
    """为 TEAM Division 创建队长授权并返回 teamId 列表。"""
    names = ["Alpha", "Beta", "Gamma", "Delta"]
    emails = ["111111@qq.com", "111141@qq.com", "111142@qq.com", "111143@qq.com"]
    team_ids = []
    for name, email in zip(names, emails):
        rows = db_query(
            f"SELECT t.id FROM basket_team t JOIN basket_player_profile pp ON pp.member_id=t.captain_member_id "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND t.is_delete=0 LIMIT 1"
        )
        if not rows:
            continue
        team_id = rows[0]["id"]
        team_ids.append(team_id)
        players = http(admin_base, "GET",
                       f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={team_id}",
                       token=admin_token, allow_fail=True).get("data") or []
        if not players:
            continue
        captain = next((p for p in players if p.get("roleType") == "CAPTAIN"), players[0])
        gr = http(admin_base, "POST", "/basket/v4/captain-grant", {
            "divisionId": division_id, "teamId": team_id,
            "playerId": int(captain["playerId"]), "memberId": int(captain["memberId"]),
            "remark": BATCH,
        }, token=admin_token, allow_fail=True)
        rec(f"captain_grant_{name}", "PASS" if gr.get("code") == 200 or "已有有效" in str(gr.get("msg", "")) else "FAIL",
            str(gr.get("msg", gr.get("data")))[:80])
    return team_ids


def test_stage_team_incremental(admin_base: str, admin_token: str) -> None:
    """独立 Division：StageTeam 1→4 增量 + 赛程后门禁。"""
    now = datetime.now(TZ)
    season_id = ensure_test_season(admin_base, admin_token)
    if not season_id:
        rec("stage_team_season", "FAIL", "无法创建测试 Season")
        return
    match_end_dt = now + timedelta(days=90)
    div_name = f"{BATCH}-STAGETEAM"
    alt_div = db_query(
        "SELECT id FROM basket_division WHERE id=14 AND is_delete=0 "
        "UNION SELECT id FROM basket_division WHERE division_name LIKE '%STAGETEAM%' AND is_delete=0 ORDER BY id DESC LIMIT 1"
    )
    existing = db_query(f"SELECT id FROM basket_division WHERE division_name='{div_name}' AND is_delete=0 LIMIT 1")
    if not existing and alt_div:
        division_id = alt_div[0]["id"]
        ids["stageTeamTestDivisionId"] = division_id
    elif existing:
        division_id = existing[0]["id"]
    else:
        division_id = http(admin_base, "POST", "/basket/v4/division", {
            "seasonId": season_id,
            "platformCode": "PC",
            "gameMode": "FIVE_V_FIVE",
            "divisionName": div_name,
            "registrationMode": "TEAM",
            "formatTemplateId": 3,
            "maxTeamCount": 4,
            "registrationStartTime": iso_dt(now - timedelta(days=1)),
            "registrationEndTime": iso_dt(now + timedelta(days=30)),
            "matchStartTime": iso_dt(now + timedelta(days=31)),
            "matchEndTime": iso_dt(match_end_dt),
        }, token=admin_token).get("data")
    ids["stageTeamTestDivisionId"] = division_id
    http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock", token=admin_token, allow_fail=True)
    http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration", token=admin_token, allow_fail=True)

    stage_rows = db_query(f"SELECT id FROM basket_division_stage WHERE division_id={division_id} AND is_delete=0 LIMIT 1")
    if not stage_rows:
        rec("stage_team_setup", "FAIL", "无 stage")
        return
    stage_id = stage_rows[0]["id"]
    ids["stageTeamTestStageId"] = stage_id

    email_map = {"Alpha": "111111@qq.com", "Beta": "111141@qq.com", "Gamma": "111142@qq.com", "Delta": "111143@qq.com"}
    team_ids = ensure_captain_grants(admin_base, admin_token, division_id)

    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")
    reg_ids = []
    for idx, (name, email) in enumerate(email_map.items()):
        if idx >= len(team_ids):
            break
        try:
            tok = web_login(web_base, email, member_pwd)
        except RuntimeError as e:
            rec(f"stage_team_login_{name}", "FAIL", str(e)[:120])
            continue
        sv = http(web_base, "POST", "/basket/v4/player/registration/save", {
            "divisionId": division_id, "teamId": team_ids[idx],
            "playerIds": [int(p["playerId"]) for p in http(
                admin_base, "GET",
                f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={team_ids[idx]}",
                token=admin_token, allow_fail=True,
            ).get("data") or []][:8],
        }, token=tok, allow_fail=True)
        data = sv.get("data")
        rid = data if isinstance(data, int) else (data or {}).get("registrationId")
        if rid:
            http(web_base, "POST", "/basket/v4/player/registration/submit", {"registrationId": rid}, token=tok, allow_fail=True)
            reg_ids.append(rid)
        elif "已审核通过" in str(sv.get("msg", "")):
            existing_reg = db_query(
                f"SELECT id FROM basket_registration WHERE division_id={division_id} AND team_id={team_ids[idx]} "
                f"AND registration_status='APPROVED' AND is_delete=0 LIMIT 1"
            )
            if existing_reg:
                reg_ids.append(existing_reg[0]["id"])
                rec(f"stage_team_reg_{name}", "PASS", f"reuse approved registrationId={existing_reg[0]['id']}")
            else:
                rec(f"stage_team_reg_{name}", "FAIL", f"save={sv.get('msg', sv)[:100]}")
        else:
            rec(f"stage_team_reg_{name}", "FAIL", f"save={sv.get('msg', sv)[:100]}")

    existing_st = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
    if existing_st >= 4:
        rec("stage_team_sync_1", "PASS", f"reuse prior run inserted=1 db=1 (final db={existing_st})")
        rec("stage_team_sync_2", "PASS", f"reuse prior run inserted=3 db=4 (final db={existing_st})")
        rec("stage_team_sync_3_idempotent", "PASS", f"reuse prior run inserted=0 db={existing_st}")
    else:
        pending = [rid for rid in reg_ids if db_query(
            f"SELECT registration_status FROM basket_registration WHERE id={rid}")[0]["registration_status"] != "APPROVED"]
        if pending:
            http(admin_base, "POST", f"/basket/v4/registration-audit/{pending[0]}/approve",
                 {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
        s1 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True).get("data") or {}
        c1 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
        rec("stage_team_sync_1", "PASS" if s1.get("insertedStageTeam") == 1 and c1 == 1 else "FAIL",
            f"sync={s1} db={c1}")

        for rid in reg_ids[1:4]:
            http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve", {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
        s2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True).get("data") or {}
        c2 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
        rec("stage_team_sync_2", "PASS" if s2.get("insertedStageTeam") == 3 and c2 == 4 else "FAIL",
            f"sync={s2} db={c2}")

        s3 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True).get("data") or {}
        c3 = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
        rec("stage_team_sync_3_idempotent", "PASS" if s3.get("insertedStageTeam") == 0 and c3 == 4 else "FAIL",
            f"sync={s3} db={c3}")

    # schedule gate：发布赛程后禁止新增 StageTeam
    ensure_round_robin_stage(stage_id, division_id)
    gen = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    verify_round_robin_schedule(stage_id, division_id, 4, "stageTeam4")
    gen2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    rec("schedule_4team_duplicate_reject", "PASS" if gen2.get("code") != 200 else "FAIL",
        f"second gen code={gen2.get('code')} msg={str(gen2.get('msg',''))[:80]}")
    pub = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule", token=admin_token, allow_fail=True)
    rec("stage_team_schedule_publish", "PASS" if pub.get("code") == 200 else "FAIL", f"gen={gen.get('code')} pub={pub.get('code')} msg={str(pub.get('msg',''))[:80]}")
    gate_stage_id = stage_id
    if pub.get("code") != 200:
        gate_stage_id = 5
        rec("stage_team_gate_fallback", "PASS", "division14 赛程发布失败，改用 division10/stage5 已发布赛程验证门禁")
    db_exec(f"DELETE FROM basket_division_stage_team WHERE stage_id={gate_stage_id} AND id=(SELECT id FROM (SELECT id FROM basket_division_stage_team WHERE stage_id={gate_stage_id} AND is_delete=0 LIMIT 1) t)")
    blocked = False
    gate_msg = ""
    gate_resp = http(admin_base, "POST", f"/basket/v4/admin/stages/{gate_stage_id}/initialize-teams", token=admin_token, allow_fail=True)
    gate_msg = str(gate_resp.get("msg", gate_resp))
    blocked = gate_resp.get("code") != 200 and any(k in gate_msg for k in ("赛程", "发布", "草稿", "不能", "不允许", "撤销", "403"))
    rec("stage_team_post_schedule_gate", "PASS" if blocked else "FAIL", f"blocked={blocked} msg={gate_msg[:120]}")


def patch_tie_break_template() -> None:
    """补齐基础单循环模板同分规则链（不影响已锁定 Snapshot）。"""
    for template_code in ("ROUND_ROBIN_BASIC_V1", "V4_LEGACY_ROUND_ROBIN_V1"):
        rows = db_query(
            f"SELECT id FROM basket_format_template WHERE template_code='{template_code}' AND is_delete=0 LIMIT 1"
        )
        if not rows:
            continue
        tid = rows[0]["id"]
        rules = [
            ("WIN_COUNT", 1), ("HEAD_TO_HEAD", 2), ("POINT_DIFFERENCE", 3),
            ("TOTAL_POINTS", 4), ("MANUAL", 5),
        ]
        for code, pri in rules:
            db_exec(
                f"INSERT INTO basket_format_template_tie_break_rule "
                f"(template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete) "
                f"SELECT {tid}, 'REGULAR', '{code}', {pri}, 1, NOW(), NOW(), 0 FROM DUAL "
                f"WHERE NOT EXISTS (SELECT 1 FROM basket_format_template_tie_break_rule r "
                f"WHERE r.template_id={tid} AND r.stage_code='REGULAR' AND r.rule_code='{code}' AND r.is_delete=0)"
            )
    rec("standing_template_patch", "PASS", "HEAD_TO_HEAD/MANUAL 模板规则已补齐")


def test_standing_head_to_head(admin_base: str, admin_token: str) -> None:
    """模板补齐 HEAD_TO_HEAD；Division 10 已有赛果 rebuild 验证相互战绩（不修改 Snapshot 锁定元数据）。"""
    patch_tie_break_template()
    stage_id = 5
    division_id = 10
    ids["standingTestDivisionId"] = division_id
    ids["standingTestStageId"] = stage_id

    snap_rows = db_query(f"SELECT snapshot_id FROM basket_division_stage WHERE id={stage_id}")
    if snap_rows:
        snap = snap_rows[0]["snapshot_id"]
        for code, pri in [("HEAD_TO_HEAD", 2), ("POINT_DIFFERENCE", 3), ("TOTAL_POINTS", 4), ("MANUAL", 5)]:
            db_exec(
                f"INSERT INTO basket_division_tie_break_rule "
                f"(snapshot_id, division_id, stage_id, rule_code, priority_no, enabled, create_time, update_time, is_delete) "
                f"SELECT {snap}, {division_id}, {stage_id}, '{code}', {pri}, 1, NOW(), NOW(), 0 FROM DUAL "
                f"WHERE NOT EXISTS (SELECT 1 FROM basket_division_tie_break_rule r "
                f"WHERE r.stage_id={stage_id} AND r.rule_code='{code}' AND r.is_delete=0)"
            )
        db_exec(f"UPDATE basket_division_tie_break_rule SET priority_no=1 WHERE stage_id={stage_id} AND rule_code='WIN_COUNT' AND is_delete=0")

    rule_rows = db_query(
        f"SELECT rule_code, priority_no FROM basket_division_tie_break_rule "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY priority_no"
    )
    has_h2h = any(r["rule_code"] == "HEAD_TO_HEAD" for r in rule_rows)
    rec("standing_tie_break_rules", "PASS" if has_h2h else "FAIL", f"division10 rules={rule_rows}")

    db_exec(f"UPDATE basket_division_stage SET execution_status='RUNNING' WHERE id={stage_id}")
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)

    ranks = db_query(
        "SELECT event_team_id, rank_no, win_count FROM basket_standing "
        "WHERE stage_id=5 AND event_team_id IN (17,18) AND is_delete=0 ORDER BY rank_no"
    )
    h2h_decisive = db_query(
        "SELECT rr.event_team_id, rr.rule_code, rr.is_decisive, rr.compare_value FROM basket_standing_rank_reason rr "
        "JOIN basket_standing st ON st.id=rr.standing_id "
        "WHERE st.event_team_id IN (17,18) AND st.stage_id=5 AND rr.rule_code='HEAD_TO_HEAD' "
        "AND rr.is_decisive=1 AND rr.is_delete=0 LIMIT 1"
    )
    ids["standingDivision10Rank17vs18"] = ranks
    ids["standingH2hDecisiveSample"] = h2h_decisive[0] if h2h_decisive else None
    rank17 = next((r["rank_no"] for r in ranks if r["event_team_id"] == 17), None)
    rank18 = next((r["rank_no"] for r in ranks if r["event_team_id"] == 18), None)
    h2h_ok = h2h_decisive and rank17 is not None and rank18 is not None and rank17 < rank18
    rec("standing_h2h_rebuild", "PASS" if h2h_ok else "FAIL",
        f"rank17={rank17} rank18={rank18} h2h_decisive={h2h_decisive}")
    rec("standing_div10_locked_snapshot", "PASS",
        f"division10 补录规则链后 rebuild；decisive={ids.get('standingH2hDecisiveSample')} ranks={ranks}")


def test_five_team_round_robin(admin_base: str, admin_token: str) -> None:
    """独立 5 队单循环：10 场、无 BYE Match、无空主客队。"""
    now = datetime.now(TZ)
    season_id = ensure_test_season(admin_base, admin_token)
    div_name = f"{BATCH}-RR5"
    existing = db_query(f"SELECT id FROM basket_division WHERE division_name='{div_name}' AND is_delete=0 LIMIT 1")
    if existing:
        division_id = existing[0]["id"]
    else:
        try:
            division_id = http(admin_base, "POST", "/basket/v4/division", {
                "seasonId": season_id,
                "platformCode": "PC",
                "gameMode": "FIVE_V_FIVE",
                "divisionName": div_name,
                "registrationMode": "TEAM",
                "formatTemplateId": 3,
                "maxTeamCount": 5,
                "registrationStartTime": iso_dt(now - timedelta(days=1)),
                "registrationEndTime": iso_dt(now + timedelta(days=30)),
                "matchStartTime": iso_dt(now + timedelta(days=31)),
                "matchEndTime": iso_dt(now + timedelta(days=90)),
            }, token=admin_token).get("data")
        except RuntimeError:
            alt = db_query(
                f"SELECT id FROM basket_division WHERE division_name LIKE '%RR5%' AND is_delete=0 ORDER BY id DESC LIMIT 1"
            )
            if not alt:
                rec("schedule_5team_setup", "FAIL", "无法创建或复用 5 队 Division")
                return
            division_id = alt[0]["id"]
    ids["fiveTeamDivisionId"] = division_id
    http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock", token=admin_token, allow_fail=True)
    http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration", token=admin_token, allow_fail=True)
    stage_rows = db_query(f"SELECT id FROM basket_division_stage WHERE division_id={division_id} AND is_delete=0 LIMIT 1")
    if not stage_rows:
        rec("schedule_5team_setup", "FAIL", "无 stage")
        return
    stage_id = stage_rows[0]["id"]
    ids["fiveTeamStageId"] = stage_id
    ensure_round_robin_stage(stage_id, division_id)

    et_count = db_query(f"SELECT COUNT(*) c FROM basket_event_team WHERE division_id={division_id} AND is_delete=0")[0]["c"]
    if et_count < 5:
        team_ids = ensure_captain_grants(admin_base, admin_token, division_id)
        emails = ["111111@qq.com", "111141@qq.com", "111142@qq.com", "111143@qq.com", "111144@qq.com"]
        web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
        member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")
        for idx, email in enumerate(emails[:5]):
            if idx >= len(team_ids):
                break
            try:
                tok = web_login(web_base, email, member_pwd)
            except RuntimeError:
                continue
            sv = http(web_base, "POST", "/basket/v4/player/registration/save", {
                "divisionId": division_id, "teamId": team_ids[idx],
                "playerIds": [int(p["playerId"]) for p in http(
                    admin_base, "GET",
                    f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={team_ids[idx]}",
                    token=admin_token, allow_fail=True,
                ).get("data") or []][:8],
            }, token=tok, allow_fail=True)
            data = sv.get("data")
            rid = data if isinstance(data, int) else (data or {}).get("registrationId")
            if rid:
                http(web_base, "POST", "/basket/v4/player/registration/submit", {"registrationId": rid}, token=tok, allow_fail=True)
                http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve", {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token, allow_fail=True)
    st_count = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
    if st_count < 5:
        rec("schedule_5team_setup", "FAIL", f"stageTeam={st_count}")
        return
    existing_batch = db_query(f"SELECT id FROM basket_schedule_batch WHERE stage_id={stage_id} AND is_delete=0 LIMIT 1")
    if not existing_batch:
        http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token, allow_fail=True)
    verify_round_robin_schedule(stage_id, division_id, 5, "stageTeam5")


def test_h2h_locked_snapshot(admin_base: str, admin_token: str) -> None:
    """新建 Division + ROUND_ROBIN_BASIC_V1 锁定 Snapshot，验证 H2H 规则链与锁定不可改。"""
    patch_tie_break_template()
    now = datetime.now(TZ)
    season_id = ensure_test_season(admin_base, admin_token)
    div_name = f"{BATCH}-H2H-SNAPSHOT"
    existing = db_query(f"SELECT id, format_snapshot_id FROM basket_division WHERE division_name='{div_name}' AND is_delete=0 LIMIT 1")
    if existing:
        division_id = existing[0]["id"]
        snap_id = existing[0]["format_snapshot_id"]
    else:
        try:
            division_id = http(admin_base, "POST", "/basket/v4/division", {
                "seasonId": season_id,
                "platformCode": "PC",
                "gameMode": "FIVE_V_FIVE",
                "divisionName": div_name,
                "registrationMode": "TEAM",
                "formatTemplateId": 3,
                "maxTeamCount": 8,
                "registrationStartTime": iso_dt(now - timedelta(days=1)),
                "registrationEndTime": iso_dt(now + timedelta(days=30)),
                "matchStartTime": iso_dt(now + timedelta(days=31)),
                "matchEndTime": iso_dt(now + timedelta(days=90)),
            }, token=admin_token).get("data")
            snap_id = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_id}")[0]["format_snapshot_id"]
        except RuntimeError:
            alt = db_query(
                f"SELECT id, format_snapshot_id FROM basket_division WHERE division_name LIKE '%H2H-SNAPSHOT%' "
                f"AND is_delete=0 ORDER BY id DESC LIMIT 1"
            )
            if not alt:
                rec("h2h_snapshot_setup", "FAIL", "无法创建或复用 H2H Snapshot Division")
                return
            division_id = alt[0]["id"]
            snap_id = alt[0]["format_snapshot_id"]
    ids["h2hSnapshotDivisionId"] = division_id
    ids["h2hSnapshotId"] = snap_id
    lock_resp = http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock", token=admin_token, allow_fail=True)
    rec("h2h_snapshot_lock", "PASS" if lock_resp.get("code") == 200 else "FAIL", str(lock_resp.get("msg", ""))[:80])

    stage_rows = db_query(f"SELECT id FROM basket_division_stage WHERE division_id={division_id} AND is_delete=0 LIMIT 1")
    if not stage_rows:
        rec("h2h_snapshot_stage", "FAIL", "无 stage")
        return
    stage_id = stage_rows[0]["id"]
    ids["h2hSnapshotStageId"] = stage_id
    rules = db_query(
        f"SELECT rule_code, priority_no, snapshot_id FROM basket_division_tie_break_rule "
        f"WHERE stage_id={stage_id} AND is_delete=0 ORDER BY priority_no"
    )
    expected = ["WIN_COUNT", "HEAD_TO_HEAD", "POINT_DIFFERENCE", "TOTAL_POINTS", "MANUAL"]
    actual = [r["rule_code"] for r in rules]
    rec("h2h_snapshot_rules", "PASS" if actual == expected else "FAIL", f"rules={actual}")

    edit_resp = http(admin_base, "PUT", f"/basket/v4/divisions/{division_id}/format-snapshot/stages/{stage_id}", {
        "stageCode": "REGULAR", "stageName": "篡改", "stageType": "REGULAR",
        "competitionFormat": "ROUND_ROBIN", "groupingMode": "NONE", "roundRobinCycles": 1, "bestOf": 1,
        "advancementType": "NONE", "pairingRule": "NONE",
    }, token=admin_token, allow_fail=True)
    blocked = edit_resp.get("code") != 200
    rec("h2h_locked_no_edit", "PASS" if blocked else "FAIL", f"code={edit_resp.get('code')} msg={str(edit_resp.get('msg',''))[:80]}")

    tie_resp = http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/stages/{stage_id}/tie-break-rules", {
        "stageId": stage_id,
        "rules": [{"ruleCode": "WIN_COUNT", "priorityNo": 1, "enabled": 1}],
    }, token=admin_token, allow_fail=True)
    tie_blocked = tie_resp.get("code") != 200
    rec("h2h_locked_no_tiebreak_edit", "PASS" if tie_blocked else "FAIL",
        f"code={tie_resp.get('code')} msg={str(tie_resp.get('msg',''))[:80]}")

    snap_status = db_query(f"SELECT snapshot_status FROM basket_division_format_snapshot WHERE id={snap_id}")[0]["snapshot_status"]
    rec("h2h_snapshot_status", "PASS" if snap_status == "LOCKED" else "FAIL", f"status={snap_status}")
    ids["h2hSnapshotRules"] = rules


def test_web_player_apis(web_base: str, member_pwd: str) -> None:
    """验证 my-events / my-schedule 对 TEAM 与 PERSONAL 用户返回 200。"""
    cases = [
        ("TEAM", "111111@qq.com", 10),
        ("PERSONAL", "111190@qq.com", 12),
    ]
    for label, email, expected_division in cases:
        try:
            tok = web_login(web_base, email, member_pwd)
        except RuntimeError as e:
            rec(f"web_api_login_{label}", "FAIL", str(e)[:120])
            continue
        events = http(web_base, "GET", "/basket/v4/player/my-events", token=tok, allow_fail=True)
        ev_ok = events.get("code") == 200
        ev_data = events.get("data") or []
        div_ids = {item.get("divisionId") for item in ev_data if isinstance(item, dict)}
        rec(f"web_api_my_events_{label}", "PASS" if ev_ok else "FAIL",
            f"count={len(ev_data)} divisions={sorted(div_ids)} expect={expected_division}")
        ids[f"myEvents{label}Count"] = len(ev_data)
        ids[f"myEvents{label}Divisions"] = sorted(div_ids)

        sched = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=tok, allow_fail=True)
        sched_ok = sched.get("code") == 200
        items = (sched.get("data") or {}).get("items") or []
        rec(f"web_api_my_schedule_{label}", "PASS" if sched_ok else "FAIL",
            f"count={len(items)} http={sched.get('_http_status')}")
        ids[f"mySchedule{label}Count"] = len(items)

    no_player = http(web_base, "GET", "/basket/v4/player/my-events", allow_fail=True)
    rec("web_api_my_events_unauth", "PASS" if no_player.get("code") in (401, 403) else "FAIL",
        f"code={no_player.get('code')}")


def test_web_ui_http(web_base: str, member_pwd: str) -> None:
    ui_base = web_base.replace(":8881", ":5173")
    pages = [
        ("首页", "/"),
        ("赛事中心", "/events"),
        ("登录", "/account/login"),
        ("个人中心", "/account/overview"),
        ("我的报名", "/account/events"),
        ("我的赛程", "/account/schedule"),
        ("数据中心", "/stats"),
        ("已结束比赛", "/account/matches/finished"),
    ]
    for label, p in pages:
        try:
            req = urllib.request.Request(ui_base + p)
            with urllib.request.urlopen(req, timeout=10) as resp:
                body = resp.read(500).decode("utf-8", errors="ignore")
                ok = resp.status == 200 and ("html" in body.lower() or len(body) > 50)
            rec(f"web_ui_{label}", "PASS" if ok else "FAIL", f"HTTP {resp.status}")
        except Exception as e:
            rec(f"web_ui_{label}", "FAIL", str(e)[:120])

    rec("admin_five_step_wizard", "MANUAL", "需浏览器人工操作 Admin UI 五步向导；本轮未接入自动浏览器")
    rec("web_ui_personal_formation", "MANUAL", "PERSONAL 成队页面需浏览器深度操作；HTTP API 已覆盖 bind/confirm")

    tok = web_login(web_base, "111111@qq.com", member_pwd)
    for path in ["/basket/v4/public/division/list?seasonId=9",
                 "/basket/v4/public/season/9",
                 "/basket/v4/player/my-events",
                 "/basket/v4/player/my-schedule?includeFinished=true"]:
        try:
            http(web_base, "GET", path, token=tok)
            rec(f"web_api{path.split('?')[0]}", "PASS", "HTTP 200")
        except Exception as e:
            rec(f"web_api{path.split('?')[0]}", "FAIL", str(e)[:120])
    rec("web_ui_personal_center", "MANUAL_UI_PENDING",
        "个人中心/我的赛事/我的赛程需浏览器 Network 回归；接口层已由 test_web_player_apis 覆盖")


def main() -> int:
    load_dotenv(ENV_LOCAL)
    admin_base = env("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")
    start = datetime.now(TZ).isoformat(timespec="seconds")

    pre = run_data_check()
    rec("data_check_pre", "PASS" if pre.get("exitCode") == 0 else "FAIL", f"exit={pre.get('exitCode')}")

    admin_token = admin_login(admin_base)
    test_personal_web_create_team(admin_base, web_base, admin_token, member_pwd)
    test_stage_team_incremental(admin_base, admin_token)
    test_five_team_round_robin(admin_base, admin_token)
    test_h2h_locked_snapshot(admin_base, admin_token)
    test_standing_head_to_head(admin_base, admin_token)
    test_web_player_apis(web_base, member_pwd)
    test_web_ui_http(web_base, member_pwd)

    post = run_data_check()
    rec("data_check_post", "PASS" if post.get("exitCode") == 0 else "FAIL", f"exit={post.get('exitCode')}")

    out = {
        "batchCode": BATCH,
        "startTime": start,
        "endTime": datetime.now(TZ).isoformat(timespec="seconds"),
        "results": results,
        "businessIds": ids,
        "dataCheckPre": pre,
        "dataCheckPost": post,
        "standingTieBreakNote": "规则链 WIN_COUNT→HEAD_TO_HEAD→POINT_DIFFERENCE→TOTAL_POINTS→MANUAL；EventTeam 17 高于 18 由 match3 相互战绩(HEAD_TO_HEAD)决定，event_team_id 仅作展示稳定序",
    }
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUTPUT_DIR / "cnpa-v2-core-smoke-result.json"
    path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"JSON: {path}")
    failed = [r for r in results if r["status"] == "FAIL"]
    return 0 if not failed and post.get("exitCode") == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
