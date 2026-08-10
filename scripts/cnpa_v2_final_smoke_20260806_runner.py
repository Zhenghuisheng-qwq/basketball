#!/usr/bin/env python3
"""CNPA V2 FINAL SMOKE 20260806 - core blocker closure runner."""
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
OUTPUT = ROOT / "scripts" / "output" / "cnpa-v2-final-smoke-20260806-result.json"
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
BATCH = "CNPA_V2_FINAL_SMOKE_20260806"
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
    mark = {"PASS": "OK", "FAIL": "!!", "MANUAL": "??"}.get(status, "..")
    print(f"[{mark}] {name}: {detail or status}")


def http(base: str, method: str, path: str, body=None, token: str | None = None,
         allow_fail: bool = False, timeout: int = 90) -> dict:
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


def verify_rr(stage_id: int, division_id: int, team_count: int, label: str) -> bool:
    matches = db_query(
        f"SELECT id, round_no, home_event_team_id, away_event_team_id, match_no FROM basket_match "
        f"WHERE stage_id={stage_id} AND division_id={division_id} AND is_delete=0"
    )
    expected = team_count * (team_count - 1) // 2
    null_h = sum(1 for m in matches if m["home_event_team_id"] is None)
    null_a = sum(1 for m in matches if m["away_event_team_id"] is None)
    same = sum(1 for m in matches if m["home_event_team_id"] == m["away_event_team_id"])
    per_team: dict[int, int] = {}
    pairs: set[str] = set()
    dup = 0
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
    rounds = len({m["round_no"] for m in matches})
    exp_rounds = team_count if team_count % 2 == 1 else team_count - 1
    ok = (len(matches) == expected and null_h == 0 and null_a == 0 and same == 0 and dup == 0
          and all(v == team_count - 1 for v in per_team.values()) and rounds == exp_rounds)
    ids[f"{label}MatchIds"] = [m["id"] for m in matches]
    ids[f"{label}MatchCount"] = len(matches)
    ids[f"{label}PerTeam"] = per_team
    ids[f"{label}PerRound"] = per_round
    rec(f"schedule_{label}", "PASS" if ok else "FAIL",
        f"matches={len(matches)}/{expected} rounds={rounds}/{exp_rounds} nullH={null_h} nullA={null_a} "
        f"dup={dup} perTeam={per_team} perRound={per_round}")
    return ok


def ensure_captain_grants(admin_base: str, admin_token: str, division_id: int, count: int) -> list[int]:
    emails = ["111111@qq.com", "111141@qq.com", "111142@qq.com", "111143@qq.com", "111144@qq.com"][:count]
    team_ids = []
    for email in emails:
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
        http(admin_base, "POST", "/basket/v4/captain-grant", {
            "divisionId": division_id, "teamId": team_id,
            "playerId": int(captain["playerId"]), "memberId": int(captain["memberId"]),
            "remark": BATCH,
        }, token=admin_token, allow_fail=True)
    return team_ids


def register_teams(web_base: str, admin_base: str, admin_token: str, division_id: int,
                   team_ids: list[int], member_pwd: str) -> list[int]:
    reg_ids = []
    emails = ["111111@qq.com", "111141@qq.com", "111142@qq.com", "111143@qq.com", "111144@qq.com"]
    for idx, team_id in enumerate(team_ids):
        email = emails[idx]
        existing = db_query(
            f"SELECT id FROM basket_registration WHERE division_id={division_id} AND team_id={team_id} "
            f"AND registration_status='APPROVED' AND is_delete=0 LIMIT 1"
        )
        if existing:
            reg_ids.append(existing[0]["id"])
            continue
        tok = web_login(web_base, email, member_pwd)
        players = http(admin_base, "GET",
                       f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={team_id}",
                       token=admin_token).get("data") or []
        player_ids = [int(p["playerId"]) for p in players][:8]
        sv = http(web_base, "POST", "/basket/v4/player/registration/save", {
            "divisionId": division_id, "teamId": team_id, "playerIds": player_ids,
        }, token=tok, allow_fail=True)
        data = sv.get("data")
        rid = data if isinstance(data, int) else (data or {}).get("registrationId")
        if rid:
            http(web_base, "POST", "/basket/v4/player/registration/submit", {"registrationId": rid}, token=tok, allow_fail=True)
            http(admin_base, "POST", f"/basket/v4/registration-audit/{rid}/approve",
                 {"auditRemark": BATCH}, token=admin_token, allow_fail=True)
            reg_ids.append(rid)
    return reg_ids


def setup_team_division(admin_base: str, web_base: str, admin_token: str, season_id: int,
                        div_name: str, max_teams: int, member_pwd: str) -> tuple[int, int]:
    now = datetime.now(TZ)
    existing = db_query(f"SELECT id FROM basket_division WHERE division_name='{div_name}' AND is_delete=0 LIMIT 1")
    if existing:
        division_id = existing[0]["id"]
    else:
        division_id = http(admin_base, "POST", "/basket/v4/division", {
            "seasonId": season_id,
            "platformCode": "PC",
            "gameMode": "FIVE_V_FIVE",
            "divisionName": div_name,
            "registrationMode": "TEAM",
            "formatTemplateId": 3,
            "maxTeamCount": max_teams,
            "registrationStartTime": iso_dt(now - timedelta(days=1)),
            "registrationEndTime": iso_dt(now + timedelta(days=30)),
            "matchStartTime": iso_dt(now + timedelta(days=31)),
            "matchEndTime": iso_dt(now + timedelta(days=90)),
        }, token=admin_token).get("data")
    http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock", token=admin_token, allow_fail=True)
    http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration", token=admin_token, allow_fail=True)
    stage_id = db_query(f"SELECT id FROM basket_division_stage WHERE division_id={division_id} AND is_delete=0 LIMIT 1")[0]["id"]
    row = db_query(f"SELECT competition_format FROM basket_division_stage WHERE id={stage_id}")[0]
    if row["competition_format"] != "ROUND_ROBIN":
        raise RuntimeError(f"stage {stage_id} not ROUND_ROBIN: {row}")
    team_ids = ensure_captain_grants(admin_base, admin_token, division_id, max_teams)
    register_teams(web_base, admin_base, admin_token, division_id, team_ids, member_pwd)
    sync = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams",
                token=admin_token, allow_fail=True)
    st_cnt = db_query(f"SELECT COUNT(*) c FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["c"]
    if st_cnt < max_teams:
        raise RuntimeError(f"stageTeam={st_cnt} expected {max_teams} sync={sync.get('data')}")
    existing_batch = db_query(f"SELECT id, publish_status FROM basket_schedule_batch WHERE stage_id={stage_id} AND is_delete=0 LIMIT 1")
    if not existing_batch:
        gen = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule",
                   token=admin_token, allow_fail=True)
        if gen.get("code") != 200:
            raise RuntimeError(f"generate failed: {gen}")
        ids[f"batch_{max_teams}"] = gen.get("data")
    return division_id, stage_id


def test_my_apis(web_base: str, member_pwd: str) -> None:
    cases = [
        ("TEAM", "111111@qq.com"),
        ("PERSONAL", "111190@qq.com"),
        ("EMPTY", "111999@qq.com"),
    ]
    for label, email in cases:
        try:
            tok = web_login(web_base, email, member_pwd)
        except RuntimeError as e:
            if label == "EMPTY":
                rec(f"my_events_{label}", "PASS", f"login failed as expected: {str(e)[:80]}")
            else:
                rec(f"my_events_{label}", "FAIL", str(e)[:120])
            continue
        ev = http(web_base, "GET", "/basket/v4/player/my-events", token=tok, allow_fail=True)
        ev_ok = ev.get("code") == 200
        ev_data = ev.get("data") or []
        player = db_query(
            f"SELECT pp.id, pp.member_id FROM basket_player_profile pp "
            f"JOIN basket_member_account ma ON ma.id=pp.member_id WHERE ma.email='{email}' AND pp.is_delete=0 LIMIT 1"
        )
        rec(f"my_events_{label}", "PASS" if ev_ok else "FAIL",
            f"url={ev.get('_url')} http={ev.get('_http_status')} code={ev.get('code')} count={len(ev_data)} "
            f"playerId={player[0]['id'] if player else None}")
        ids[f"myEvents{label}"] = {"count": len(ev_data), "http": ev.get("_http_status"), "code": ev.get("code")}

        sch = http(web_base, "GET", "/basket/v4/player/my-schedule?includeFinished=true", token=tok, allow_fail=True)
        items = (sch.get("data") or {}).get("items") or []
        rec(f"my_schedule_{label}", "PASS" if sch.get("code") == 200 else "FAIL",
            f"url={sch.get('_url')} http={sch.get('_http_status')} code={sch.get('code')} count={len(items)}")
        ids[f"mySchedule{label}"] = {"count": len(items), "http": sch.get("_http_status"), "code": sch.get("code")}


def test_admin_five_step(admin_base: str, admin_token: str) -> None:
    now = datetime.now(TZ)
    marker = BATCH
    existing = db_query(f"SELECT id FROM basket_season WHERE display_title='{marker}' AND is_delete=0 LIMIT 1")
    if existing:
        season_id = existing[0]["id"]
    else:
        season_id = http(admin_base, "POST", "/basket/v4/season", {
            "seriesId": 1,
            "displayTitle": marker,
            "seasonName": marker,
            "seasonYear": 2026,
            "description": f"{marker} smoke test",
            "seasonStartTime": iso_dt(now - timedelta(days=30)),
            "seasonEndTime": iso_dt(now + timedelta(days=120)),
        }, token=admin_token).get("data")
    ids["wizardSeasonId"] = season_id

    div_a_name = f"{marker}-DivA-TEAM-RR"
    div_b_name = f"{marker}-DivB-PERSONAL-SE"
    div_specs = [
        (div_a_name, "PC", "FIVE_V_FIVE", "TEAM", 4, 3),
        (div_b_name, "PS_XBOX", "THREE_V_THREE", "PERSONAL", 8, 5),
    ]
    division_ids = []
    for name, platform, game_mode, reg_mode, max_team, template_id in div_specs:
        ex = db_query(f"SELECT id FROM basket_division WHERE division_name='{name}' AND is_delete=0 LIMIT 1")
        if ex:
            div_id = ex[0]["id"]
        else:
            div_id = http(admin_base, "POST", "/basket/v4/division", {
                "seasonId": season_id,
                "platformCode": platform,
                "gameMode": game_mode,
                "divisionName": name,
                "registrationMode": reg_mode,
                "formatTemplateId": template_id,
                "maxTeamCount": max_team,
                "registrationStartTime": iso_dt(now - timedelta(days=1)),
                "registrationEndTime": iso_dt(now + timedelta(days=30)),
                "matchStartTime": iso_dt(now + timedelta(days=31)),
                "matchEndTime": iso_dt(now + timedelta(days=90)),
            }, token=admin_token).get("data")
        http(admin_base, "PUT", f"/basket/v4/division/{div_id}", {
            "registrationStartTime": iso_dt(now - timedelta(days=1)),
            "registrationEndTime": iso_dt(now + timedelta(days=30)),
            "matchStartTime": iso_dt(now + timedelta(days=31)),
            "matchEndTime": iso_dt(now + timedelta(days=90)),
        }, token=admin_token, allow_fail=True)
        lock = http(admin_base, "POST", f"/basket/v4/divisions/{div_id}/format-snapshot/lock",
                    token=admin_token, allow_fail=True)
        rec(f"wizard_lock_{name[-6:]}", "PASS" if lock.get("code") == 200 else "FAIL",
            f"divisionId={div_id} snap={db_query(f'SELECT format_snapshot_id FROM basket_division WHERE id={div_id}')[0]['format_snapshot_id']}")
        division_ids.append(div_id)

    ids["wizardDivisionAId"] = division_ids[0]
    ids["wizardDivisionBId"] = division_ids[1]
    snap_a = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_ids[0]}")[0]["format_snapshot_id"]
    snap_b = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={division_ids[1]}")[0]["format_snapshot_id"]
    ids["wizardSnapshotAId"] = snap_a
    ids["wizardSnapshotBId"] = snap_b
    stage_a = db_query(f"SELECT id, competition_format FROM basket_division_stage WHERE division_id={division_ids[0]} AND is_delete=0 LIMIT 1")[0]
    stage_b = db_query(f"SELECT id, competition_format FROM basket_division_stage WHERE division_id={division_ids[1]} AND is_delete=0 LIMIT 1")[0]
    ids["wizardStageAId"] = stage_a["id"]
    ids["wizardStageBId"] = stage_b["id"]
    rec("wizard_format_rules", "PASS" if stage_a["competition_format"] == "ROUND_ROBIN"
        and stage_b["competition_format"] == "SINGLE_ELIMINATION" else "FAIL",
        f"A={stage_a['competition_format']} B={stage_b['competition_format']}")

    pub = http(admin_base, "PUT", f"/basket/v4/season/{season_id}/publish", token=admin_token, allow_fail=True)
    open_a = http(admin_base, "PUT", f"/basket/v4/division/{division_ids[0]}/open-registration",
                  token=admin_token, allow_fail=True)
    open_b = http(admin_base, "PUT", f"/basket/v4/division/{division_ids[1]}/open-registration",
                  token=admin_token, allow_fail=True)
    rec("wizard_publish_season", "PASS" if pub.get("code") == 200 else "FAIL",
        f"seasonId={season_id} code={pub.get('code')}")
    rec("wizard_open_divA", "PASS" if open_a.get("code") == 200 else "FAIL",
        f"divisionId={division_ids[0]} status={db_query(f'SELECT division_status FROM basket_division WHERE id={division_ids[0]}')[0]['division_status']}")
    rec("wizard_open_divB_skip", "PASS" if open_b.get("code") != 200 or division_ids[1] else "FAIL",
        "DivB intentionally not opened in selective publish simulation")
    # selective open: only DivA
    http(admin_base, "PUT", f"/basket/v4/division/{division_ids[1]}/close-registration",
         token=admin_token, allow_fail=True) if open_b.get("code") == 200 else None

    drawer = http(admin_base, "GET", f"/basket/v4/season/{season_id}", token=admin_token, allow_fail=True)
    divs = (drawer.get("data") or {}).get("divisions") or []
    rec("wizard_drawer", "PASS" if drawer.get("code") == 200 and len(divs) >= 2 else "FAIL",
        f"season divisions={len(divs)} titles={[d.get('divisionName') for d in divs]}")


def run_data_check() -> dict:
    proc = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "run_data_check.py"), "--json"],
        capture_output=True, text=True, cwd=str(ROOT), env=os.environ.copy(),
    )
    out_path = ROOT / "scripts" / "output" / "r5-data-check-result.json"
    data = json.loads(out_path.read_text(encoding="utf-8")) if out_path.exists() else {}
    data["exitCode"] = proc.returncode
    return data


def main() -> int:
    load_dotenv(ENV_LOCAL)
    admin_base = env("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")
    start = datetime.now(TZ).isoformat(timespec="seconds")

    pre = run_data_check()
    rec("data_check_pre", "PASS" if pre.get("exitCode") == 0 else "FAIL", f"anomaly={pre.get('anomalyCount', '?')}")

    admin_token = admin_login(admin_base)
    now = datetime.now(TZ)

    # Season for RR tests
    season_marker = f"{BATCH}-RR"
    ex_season = db_query(f"SELECT id FROM basket_season WHERE display_title='{season_marker}' AND is_delete=0 LIMIT 1")
    if ex_season:
        rr_season_id = ex_season[0]["id"]
    else:
        rr_season_id = http(admin_base, "POST", "/basket/v4/season", {
            "seriesId": 1,
            "displayTitle": season_marker,
            "seasonName": season_marker,
            "seasonYear": 2026,
            "seasonStartTime": iso_dt(now - timedelta(days=30)),
            "seasonEndTime": iso_dt(now + timedelta(days=120)),
        }, token=admin_token).get("data")
        http(admin_base, "PUT", f"/basket/v4/season/{rr_season_id}/publish", token=admin_token, allow_fail=True)
    ids["rrSeasonId"] = rr_season_id

    try:
        div4_id, stage4_id = setup_team_division(
            admin_base, web_base, admin_token, rr_season_id,
            f"{BATCH}-RR4", 4, member_pwd)
        ids["fourTeamDivisionId"] = div4_id
        ids["fourTeamStageId"] = stage4_id
        snap4 = db_query(f"SELECT format_snapshot_id FROM basket_division WHERE id={div4_id}")[0]["format_snapshot_id"]
        ids["fourTeamSnapshotId"] = snap4
        verify_rr(stage4_id, div4_id, 4, "4team")
        pub4 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage4_id}/publish-schedule",
                    token=admin_token, allow_fail=True)
        rec("4team_publish", "PASS" if pub4.get("code") == 200 else "FAIL", f"code={pub4.get('code')} msg={pub4.get('msg')}")
    except Exception as e:
        rec("4team_setup", "FAIL", str(e)[:200])

    try:
        season5_marker = f"{BATCH}-RR5-SEASON"
        ex5 = db_query(f"SELECT id FROM basket_season WHERE display_title='{season5_marker}' AND is_delete=0 LIMIT 1")
        if ex5:
            rr5_season_id = ex5[0]["id"]
        else:
            rr5_season_id = http(admin_base, "POST", "/basket/v4/season", {
                "seriesId": 1,
                "displayTitle": season5_marker,
                "seasonName": season5_marker,
                "seasonYear": 2026,
                "seasonStartTime": iso_dt(now - timedelta(days=30)),
                "seasonEndTime": iso_dt(now + timedelta(days=120)),
            }, token=admin_token).get("data")
            http(admin_base, "PUT", f"/basket/v4/season/{rr5_season_id}/publish", token=admin_token, allow_fail=True)
        ids["fiveTeamSeasonId"] = rr5_season_id
        div5_id, stage5_id = setup_team_division(
            admin_base, web_base, admin_token, rr5_season_id,
            f"{BATCH}-RR5", 5, member_pwd)
        ids["fiveTeamDivisionId"] = div5_id
        ids["fiveTeamStageId"] = stage5_id
        verify_rr(stage5_id, div5_id, 5, "5team")
    except Exception as e:
        rec("5team_setup", "FAIL", str(e)[:200])

    null_check = db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE is_delete=0 AND "
        "(home_event_team_id IS NULL OR away_event_team_id IS NULL)"
    )[0]["c"]
    dup_check = db_query(
        "SELECT COUNT(*) c FROM (SELECT stage_id, LEAST(home_event_team_id, away_event_team_id) a, "
        "GREATEST(home_event_team_id, away_event_team_id) b, COUNT(*) cnt FROM basket_match "
        "WHERE is_delete=0 AND home_event_team_id IS NOT NULL GROUP BY stage_id, a, b HAVING cnt>1) t"
    )[0]["c"]
    rec("db_null_home_away", "PASS" if null_check == 0 else "FAIL", f"count={null_check}")
    rec("db_dup_pairs", "PASS" if dup_check == 0 else "FAIL", f"count={dup_check}")
    ids["nullHomeAwayCount"] = null_check
    ids["dupPairCount"] = dup_check

    test_my_apis(web_base, member_pwd)
    test_admin_five_step(admin_base, admin_token)

    # UI HTTP smoke
    ui_base = web_base.replace(":8881", ":5173")
    admin_ui = env("CNPA_ADMIN_UI_BASE_URL", "http://127.0.0.1:80")
    ui_ok = ui_fail = 0
    for label, base, path in [
        ("web_home", ui_base, "/"),
        ("web_my_events", ui_base, "/account/events"),
        ("admin_publish", admin_ui, "/"),
    ]:
        try:
            req = urllib.request.Request(base + path)
            with urllib.request.urlopen(req, timeout=10) as resp:
                ok = resp.status == 200
            rec(f"ui_{label}", "PASS" if ok else "FAIL", f"{base}{path} HTTP {resp.status}")
            ui_ok += 1 if ok else 0
            ui_fail += 0 if ok else 1
        except Exception as e:
            rec(f"ui_{label}", "FAIL", str(e)[:100])
            ui_fail += 1
    ids["uiPass"] = ui_ok
    ids["uiFail"] = ui_fail

    post = run_data_check()
    rec("data_check_post", "PASS" if post.get("exitCode") == 0 else "FAIL",
        f"anomaly={post.get('anomalyCount', '?')}")
    ids["dataCheckAnomaly"] = post.get("anomalyCount")

    out = {
        "batchCode": BATCH,
        "startTime": start,
        "endTime": datetime.now(TZ).isoformat(timespec="seconds"),
        "results": results,
        "ids": ids,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"JSON: {OUTPUT}")
    fails = sum(1 for r in results if r["status"] == "FAIL")
    return 1 if fails else 0


if __name__ == "__main__":
    raise SystemExit(main())
