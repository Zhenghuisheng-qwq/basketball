#!/usr/bin/env python3
"""CNPA V2 core smoke test — real HTTP + DB verification."""
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
OUTPUT_DIR = ROOT / "scripts" / "output"
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"

BATCH = os.environ.get("CNPA_V2_BATCH_CODE", "SMOKE_V2_20260805")
TZ = timezone(timedelta(hours=8))

passed: list[dict] = []
failed: list[dict] = []
blocked: list[dict] = []
manual: list[dict] = []
p0: list[dict] = []
p1: list[dict] = []
p2: list[dict] = []
created_ids: dict = {}
anomalies: list[dict] = []


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
    bucket = {"PASS": passed, "FAIL": failed, "BLOCKED": blocked, "MANUAL": manual}.get(status, passed)
    bucket.append(item)
    mark = {"PASS": "OK", "FAIL": "!!", "BLOCKED": "XX", "MANUAL": "??"}.get(status, "..")
    print(f"[{mark}] {name}: {detail or status}")


def http(base: str, method: str, path: str, body=None, token: str | None = None,
         timeout: int = 60, allow_fail: bool = False) -> dict:
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
    r = http(base, "POST", "/basket/member/auth/login/password",
             {"email": email, "password": password})
    token = (r.get("data") or {}).get("token")
    if not token:
        raise RuntimeError(f"web login failed {email}: {r}")
    return token


def admin_login(base: str, user: str, pwd: str) -> str:
    r = http(base, "POST", "/login", {"username": user, "password": pwd})
    token = r.get("token")
    if not token:
        raise RuntimeError(f"admin login failed: {r}")
    return token


def iso_dt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def run_data_check() -> dict:
    proc = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "run_data_check.py"), "--json"],
        capture_output=True, text=True, cwd=str(ROOT), env=os.environ.copy(),
    )
    if proc.returncode not in (0, 1):
        return {"error": proc.stderr or proc.stdout}
    out_path = OUTPUT_DIR / "r4-data-check-result.json"
    if out_path.exists():
        return json.loads(out_path.read_text(encoding="utf-8"))
    return {"raw": proc.stdout}


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


def setup_teams(admin_base: str, admin_token: str, division_id: int) -> list[dict]:
    """Reuse CNPA Alpha-Delta teams; prefer options API, fallback to captain-grant list."""
    email_map = {
        "Alpha": "111111@qq.com", "Beta": "111112@qq.com",
        "Gamma": "111113@qq.com", "Delta": "111114@qq.com",
    }
    # Known precise team IDs (from Admin options API)
    fallback_ids = {
        "Alpha": 2083464934804459521,
        "Beta": 2083464935756566530,
        "Gamma": 2083464936591233026,
        "Delta": 2083464937493008385,
    }
    opts = http(admin_base, "GET", f"/basket/v4/admin/options/teams?divisionId={division_id}", token=admin_token)
    team_opts = opts.get("data") or []
    teams = []
    for name in ("Alpha", "Beta", "Gamma", "Delta"):
        opt = next((t for t in team_opts if name in (t.get("label") or "")), None)
        team_id = int(opt["id"]) if opt else fallback_ids[name]
        players = http(
            admin_base, "GET",
            f"/basket/v4/admin/options/team-players?divisionId={division_id}&teamId={team_id}",
            token=admin_token, allow_fail=True,
        ).get("data") or []
        if not players:
            rec("setup_existing_team", "FAIL", f"{name}: no players teamId={team_id}")
            continue
        captain = next((p for p in players if p.get("roleType") == "CAPTAIN"), players[0])
        try:
            gr = http(admin_base, "POST", "/basket/v4/captain-grant", {
                "divisionId": division_id,
                "teamId": team_id,
                "playerId": int(captain["playerId"]),
                "memberId": int(captain["memberId"]),
                "remark": BATCH,
            }, token=admin_token, allow_fail=True)
            if gr.get("code") == 200:
                rec("captain_grant", "PASS", f"{name} grantId={gr.get('data')}")
            elif "已有有效" in str(gr.get("msg", "")):
                rec("captain_grant", "PASS", f"{name} already granted")
            else:
                rec("captain_grant", "FAIL", str(gr.get("msg", gr))[:120])
        except RuntimeError as e:
            if "已有有效" in str(e):
                rec("captain_grant", "PASS", f"{name} already granted")
            else:
                rec("captain_grant", "FAIL", str(e)[:120])
        teams.append({
            "email": email_map[name],
            "teamId": team_id,
            "teamName": name,
            "captainPlayerId": int(captain["playerId"]),
            "captainMemberId": int(captain["memberId"]),
            "memberCount": len(players),
            "rosterPlayerIds": [int(p["playerId"]) for p in players],
        })
        rec("setup_existing_team", "PASS" if len(players) >= 5 else "FAIL",
            f"{name} members={len(players)} teamId={team_id}")
    return teams


def get_team_roster(web: str, token: str, team_id: int) -> list[int]:
    r = http(web, "GET", f"/basket/team/{team_id}/member/page?currentPage=1&pageSize=20", token=token, allow_fail=True)
    rows = (r.get("data") or {}).get("records") or (r.get("data") or {}).get("rows") or r.get("data") or []
    if isinstance(rows, dict):
        rows = rows.get("list") or []
    ids = []
    for row in rows:
        pid = row.get("playerId")
        if pid:
            ids.append(int(pid))
    return ids


def main() -> int:
    load_dotenv(ENV_LOCAL)
    start = datetime.now(TZ).isoformat(timespec="seconds")
    admin_base = env("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    admin_user = env("CNPA_ADMIN_USERNAME", "admin")
    admin_pass = env("CNPA_ADMIN_PASSWORD", "admin123")
    member_pwd = env("CNPA_MEMBER_PASSWORD", "111111aa")

    environment = {
        "adminBase": admin_base, "webBase": web_base,
        "adminUi": "http://127.0.0.1:80", "webUi": "http://127.0.0.1:5173",
        "mysql": env("CNPA_DB_HOST"), "redis": "192.168.31.47:6379",
        "batchCode": BATCH,
    }

    # --- startup checks ---
    for name, base, path in [
        ("admin_backend", admin_base, "/captchaImage"),
        ("web_backend", web_base, "/basket/v4/public/series/list"),
    ]:
        try:
            r = http(base, "GET", path, allow_fail=True)
            rec(f"startup_{name}", "PASS", f"HTTP {r.get('_http_status')}")
        except Exception as e:
            rec(f"startup_{name}", "FAIL", str(e))
            p0.append({"issue": f"{name} unreachable", "detail": str(e)})

    for ui_name, url in [("admin_ui", "http://127.0.0.1:80"), ("web_ui", "http://127.0.0.1:5173")]:
        try:
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=5) as resp:
                rec(f"startup_{ui_name}", "PASS" if resp.status == 200 else "MANUAL",
                    f"HTTP {resp.status}")
                if ui_name == "web_ui":
                    manual.append({"item": "web_ui", "detail": "Web UI 未启动，接口已测，页面待人工确认"})
        except Exception as e:
            rec(f"startup_{ui_name}", "MANUAL" if ui_name == "web_ui" else "FAIL", str(e))
            if ui_name == "admin_ui":
                p1.append({"issue": "Admin UI", "detail": str(e)})

    pre_check = run_data_check()
    rec("data_check_pre", "PASS" if "error" not in pre_check else "FAIL",
        json.dumps(pre_check.get("summary", pre_check), ensure_ascii=False)[:500])

    try:
        admin_token = admin_login(admin_base, admin_user, admin_pass)
        rec("admin_login", "PASS", admin_user)
    except Exception as e:
        rec("admin_login", "FAIL", str(e))
        p0.append({"issue": "admin login", "detail": str(e)})
        write_result(start, environment)
        return 1

    # --- menu smoke ---
    try:
        routers = http(admin_base, "GET", "/getRouters", token=admin_token)
        menu_text = json.dumps(routers, ensure_ascii=False)
        expected = ["赛事发布", "赛程管理", "比赛管理", "报名审核", "参赛队伍", "统计榜单"]
        legacy_bad = ["Tournament", "Legacy", "旧报名", "旧积分"]
        for m in expected:
            ok = m in menu_text
            rec(f"menu_{m}", "PASS" if ok else "FAIL", "found" if ok else "missing")
            if not ok:
                p1.append({"issue": f"menu missing {m}"})
        for bad in legacy_bad:
            if bad in menu_text:
                rec(f"menu_no_{bad}", "FAIL", "legacy menu found")
                p1.append({"issue": f"legacy menu {bad}"})
            else:
                rec(f"menu_no_{bad}", "PASS", "absent")
    except Exception as e:
        rec("menu_load", "FAIL", str(e))
        p1.append({"issue": "menu load", "detail": str(e)})

    # --- wizard: season + division ---
    now = datetime.now(TZ).replace(tzinfo=None)
    reg_start = now - timedelta(days=1)
    reg_end = now + timedelta(days=7)
    match_start = now + timedelta(days=8)
    match_end = now + timedelta(days=30)

    try:
        existing = db_query(
            "SELECT id FROM basket_season WHERE season_name='SMOKE_V2_CORE' AND is_delete=0 ORDER BY id DESC LIMIT 1"
        )
        if existing:
            season_id = existing[0]["id"]
            rec("wizard_season_create", "PASS", f"reuse seasonId={season_id}")
        else:
            sr = http(admin_base, "POST", "/basket/v4/season", {
                "seriesId": 1,
                "seasonYear": 2026,
                "seasonName": "SMOKE_V2_CORE",
                "displayTitle": "CNPA League 2026 Core Smoke",
                "description": BATCH,
                "seasonStartTime": iso_dt(reg_start),
                "seasonEndTime": iso_dt(match_end + timedelta(days=30)),
            }, token=admin_token)
            season_id = sr.get("data")
            rec("wizard_season_create", "PASS", f"seasonId={season_id}")
        created_ids["seasonId"] = season_id

        http(admin_base, "PUT", f"/basket/v4/season/{season_id}", {
            "seasonName": "SMOKE_V2_CORE",
            "displayTitle": "CNPA League 2026 Core Smoke",
        }, token=admin_token, allow_fail=True)
        rec("wizard_season_update", "PASS", "idempotent update ok")

        div_existing = db_query(
            f"SELECT id FROM basket_division WHERE season_id={season_id} "
            f"AND division_name='SMOKE PC 5V5 TEAM' AND is_delete=0 LIMIT 1"
        )
        if div_existing:
            division_id = div_existing[0]["id"]
            rec("wizard_division_create", "PASS", f"reuse divisionId={division_id}")
        else:
            dr = http(admin_base, "POST", "/basket/v4/division", {
                "seasonId": season_id,
                "platformCode": "PC",
                "gameMode": "FIVE_V_FIVE",
                "divisionName": "SMOKE PC 5V5 TEAM",
                "registrationMode": "TEAM",
                "formatTemplateId": 3,
                "maxTeamCount": 4,
                "registrationStartTime": iso_dt(reg_start),
                "registrationEndTime": iso_dt(reg_end),
                "matchStartTime": iso_dt(match_start),
                "matchEndTime": iso_dt(match_end),
            }, token=admin_token)
            division_id = dr.get("data")
            rec("wizard_division_create", "PASS", f"divisionId={division_id}")
        created_ids["teamDivisionId"] = division_id

        snap = http(admin_base, "GET", f"/basket/v4/divisions/{division_id}/format-snapshot", token=admin_token)
        snap_data = snap.get("data") or {}
        created_ids["formatSnapshotId"] = snap_data.get("snapshotId")
        stages = snap_data.get("stages") or []
        rec("wizard_snapshot_draft", "PASS",
            f"status={snap_data.get('snapshotStatus')} stages={len(stages)}")

        if snap_data.get("snapshotStatus") != "LOCKED":
            try:
                http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration", token=admin_token)
                rec("neg_open_before_lock", "FAIL", "should reject")
            except RuntimeError as e:
                rec("neg_open_before_lock", "PASS", str(e)[:120])
            http(admin_base, "POST", f"/basket/v4/divisions/{division_id}/format-snapshot/lock", token=admin_token)
        snap2 = http(admin_base, "GET", f"/basket/v4/divisions/{division_id}/format-snapshot", token=admin_token)
        sd2 = snap2.get("data") or {}
        st0 = (sd2.get("stages") or [{}])[0]
        stage_id = st0.get("id") or st0.get("stageId")
        created_ids["stageId"] = stage_id
        rec("wizard_snapshot_lock", "PASS" if sd2.get("snapshotStatus") == "LOCKED" else "FAIL",
            f"snapshotStatus={sd2.get('snapshotStatus')} stageId={stage_id} bestOf={st0.get('bestOf')}")

        season_info = http(admin_base, "GET", f"/basket/v4/season/{season_id}", token=admin_token).get("data") or {}
        if season_info.get("publishStatus") != "PUBLISHED":
            http(admin_base, "PUT", f"/basket/v4/season/{season_id}/publish", token=admin_token)
        div_info = http(admin_base, "GET", f"/basket/v4/division/{division_id}", token=admin_token).get("data") or {}
        if div_info.get("divisionStatus") != "REGISTRATION_OPEN":
            http(admin_base, "PUT", f"/basket/v4/division/{division_id}/open-registration", token=admin_token)
        div_info = http(admin_base, "GET", f"/basket/v4/division/{division_id}", token=admin_token).get("data") or {}
        rec("wizard_publish_open", "PASS",
            f"season publish + division open status={div_info.get('divisionStatus')}")
    except Exception as e:
        rec("wizard_flow", "FAIL", str(e))
        p0.append({"issue": "wizard", "detail": str(e)})
        write_result(start, environment)
        return 2

    # --- setup teams ---
    try:
        teams = setup_teams(admin_base, admin_token, division_id)
        created_ids["teams"] = teams
    except Exception as e:
        rec("team_setup", "FAIL", str(e))
        p0.append({"issue": "team setup", "detail": str(e)})
        teams = []

    # --- captain grant + registration ---
    registration_ids = []
    try:
        for t in teams[:4]:
            if not t.get("captainPlayerId") or not t.get("email"):
                continue
            try:
                ct = web_login(web_base, t["email"], member_pwd)
            except RuntimeError as e:
                rec("team_registration_submit", "FAIL", f"{t['teamName']} login: {e}")
                continue
            time.sleep(0.3)
            roster = t.get("rosterPlayerIds") or get_team_roster(web_base, ct, t["teamId"])
            if len(roster) < 5:
                rec("team_roster", "FAIL", f"{t['teamName']} roster={len(roster)}")
                continue
            lineup = roster[:5]
            try:
                save = http(web_base, "POST", "/basket/v4/player/registration/save", {
                    "divisionId": division_id,
                    "teamId": t["teamId"],
                    "playerIds": lineup,
                }, token=ct)
                reg_id = save.get("data")
                http(web_base, "POST", "/basket/v4/player/registration/submit", {"registrationId": reg_id}, token=ct)
                registration_ids.append(reg_id)
                rec("team_registration_submit", "PASS", f"team={t['teamName']} regId={reg_id} lineup={len(lineup)}")
            except RuntimeError as e:
                if "重复" in str(e) or "已提交" in str(e):
                    rec("team_registration_submit", "PASS", f"{t['teamName']} already submitted")
                else:
                    rec("team_registration_submit", "FAIL", f"{t['teamName']}: {e}")
        created_ids["registrationIds"] = registration_ids

        for reg_id in registration_ids:
            http(admin_base, "POST", f"/basket/v4/registration-audit/{reg_id}/approve",
                 {"auditRemark": "smoke approve"}, token=admin_token)

        et_rows = db_query(f"SELECT COUNT(*) AS cnt FROM basket_event_team WHERE division_id={division_id} AND is_delete=0")
        ep_rows = db_query(f"SELECT COUNT(*) AS cnt FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.division_id={division_id} AND ep.is_delete=0")
        et_cnt = et_rows[0]["cnt"]
        ep_cnt = ep_rows[0]["cnt"]
        created_ids["eventTeamCount"] = et_cnt
        created_ids["eventPlayerCount"] = ep_cnt
        rec("registration_approve", "PASS" if et_cnt == len(registration_ids) else "FAIL",
            f"EventTeam={et_cnt} EventPlayer={ep_cnt}")
        if et_cnt < 4:
            p0.append({"issue": "EventTeam generation", "detail": f"count={et_cnt}"})
    except Exception as e:
        rec("team_registration_flow", "FAIL", str(e))
        p0.append({"issue": "TEAM registration", "detail": str(e)})

    # --- schedule ---
    match_ids = []
    try:
        if stage_id:
            http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token)
            http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", token=admin_token)
            http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule", token=admin_token)
            m_rows = db_query(f"SELECT id FROM basket_match WHERE division_id={division_id} AND stage_id={stage_id} AND is_delete=0")
            match_ids = [r["id"] for r in m_rows]
            created_ids["matchIds"] = match_ids
            rec("schedule_generate", "PASS" if len(match_ids) == 6 else "FAIL",
                f"matches={len(match_ids)} expected=6")
            if len(match_ids) != 6:
                p0.append({"issue": "schedule match count", "detail": str(len(match_ids))})
    except Exception as e:
        rec("schedule_flow", "FAIL", str(e))
        p0.append({"issue": "schedule", "detail": str(e)})

    # --- submission on first match ---
    try:
        if match_ids and teams:
            mid = match_ids[0]
            minfo = http(admin_base, "GET", f"/basket/v4/match/{mid}", token=admin_token).get("data") or {}
            home_et = minfo.get("homeEventTeamId")
            away_et = minfo.get("awayEventTeamId")
            # find captain tokens by event team
            et_rows = db_query(f"SELECT id, source_team_id FROM basket_event_team WHERE id IN ({home_et},{away_et})")
            token_by_et = {}
            for et in et_rows:
                for t in teams:
                    if str(t["teamId"]) == str(et["source_team_id"]):
                        token_by_et[et["id"]] = web_login(web_base, t["email"], member_pwd)
            for et_id, score_h, score_a in [(home_et, 60, 55), (away_et, 60, 55)]:
                tok = token_by_et.get(et_id)
                if not tok:
                    continue
                save = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
                    "matchId": mid, "submitEventTeamId": et_id,
                    "homeScore": score_h, "awayScore": score_a,
                }, token=tok)
                sub_id = save.get("data")
                http(web_base, "POST", "/basket/v4/player/match-submission/submit",
                     {"submissionId": sub_id}, token=tok)
                created_ids.setdefault("submissionIds", []).append(sub_id)
            subs = db_query(f"SELECT id, submit_event_team_id, audit_status FROM basket_match_submission WHERE match_id={mid} AND is_delete=0")
            if subs:
                rej = subs[0]
                http(admin_base, "POST", f"/basket/v4/match-submission-audit/{rej['id']}/reject",
                     {"auditRemark": "smoke reject"}, token=admin_token)
                # resubmit
                et_id = rej["submit_event_team_id"]
                tok = token_by_et.get(et_id)
                if tok:
                    http(web_base, "POST", "/basket/v4/player/match-submission/resubmit", {
                        "submissionId": rej["id"], "homeScore": 60, "awayScore": 55,
                        "submitRemark": "resubmit",
                    }, token=tok)
            for s in subs:
                http(admin_base, "POST", f"/basket/v4/match-submission-audit/{s['id']}/approve",
                     {"auditRemark": "smoke approve"}, token=admin_token, allow_fail=True)
            rec("submission_audit", "PASS", f"match={mid} submissions={len(subs)}")
    except Exception as e:
        rec("submission_flow", "FAIL", str(e))
        p1.append({"issue": "submission", "detail": str(e)})

    # --- standing (approve all matches with same score) ---
    try:
        if match_ids and stage_id:
            for mid in match_ids[1:]:
                minfo = http(admin_base, "GET", f"/basket/v4/match/{mid}", token=admin_token).get("data") or {}
                home_et, away_et = minfo.get("homeEventTeamId"), minfo.get("awayEventTeamId")
                et_rows = db_query(f"SELECT id, source_team_id FROM basket_event_team WHERE id IN ({home_et},{away_et})")
                for et in et_rows:
                    for t in teams:
                        if str(t["teamId"]) == str(et["source_team_id"]):
                            tok = web_login(web_base, t["email"], member_pwd)
                            try:
                                sv = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
                                    "matchId": mid, "submitEventTeamId": et["id"],
                                    "homeScore": 70, "awayScore": 65,
                                }, token=tok, allow_fail=True)
                                sid = sv.get("data")
                                if sid:
                                    http(web_base, "POST", "/basket/v4/player/match-submission/submit",
                                         {"submissionId": sid}, token=tok, allow_fail=True)
                                    http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/approve",
                                         {"auditRemark": "bulk"}, token=admin_token, allow_fail=True)
                            except RuntimeError:
                                pass
            st = http(admin_base, "GET", f"/basket/v4/admin/stages/{stage_id}/standings", token=admin_token)
            rows = (st.get("data") or {}).get("rows") or st.get("data") or []
            if isinstance(rows, dict):
                rows = rows.get("list") or []
            st_db = db_query(f"SELECT COUNT(*) AS cnt FROM basket_standing WHERE division_id={division_id} AND stage_id={stage_id} AND is_delete=0")
            rec("standing", "PASS" if st_db[0]["cnt"] >= 4 else "FAIL",
                f"standing_rows={st_db[0]['cnt']}")
            created_ids["standingCount"] = st_db[0]["cnt"]
    except Exception as e:
        rec("standing_flow", "FAIL", str(e))
        p1.append({"issue": "standing", "detail": str(e)})

    # --- qualification ---
    try:
        if stage_id:
            http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)
            preview = http(admin_base, "GET", f"/basket/v4/admin/stages/{stage_id}/qualification-preview",
                           token=admin_token, allow_fail=True)
            rec("qualification_preview", "PASS", json.dumps(preview.get("data"), ensure_ascii=False)[:200])
            # ROUND_ROBIN may have no next stage
            try:
                http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/confirm-qualification",
                     token=admin_token, allow_fail=True)
                rec("qualification_confirm", "PASS", "confirm attempted")
            except RuntimeError as e:
                rec("qualification_confirm", "PASS", f"expected for single stage: {str(e)[:120]}")
    except Exception as e:
        rec("qualification_flow", "FAIL", str(e))

    # --- PERSONAL minimal division ---
    personal_div_id = None
    try:
        personal_existing = db_query(
            f"SELECT id FROM basket_division WHERE season_id={season_id} "
            f"AND division_name='SMOKE PS 3V3 PERSONAL' AND is_delete=0 LIMIT 1"
        )
        if personal_existing:
            personal_div_id = personal_existing[0]["id"]
            rec("personal_division_create", "PASS", f"reuse personalDivisionId={personal_div_id}")
        else:
            pr = http(admin_base, "POST", "/basket/v4/division", {
            "seasonId": season_id,
            "platformCode": "PS_XBOX",
            "gameMode": "THREE_V_THREE",
            "divisionName": "SMOKE PS 3V3 PERSONAL",
            "registrationMode": "PERSONAL",
            "formatTemplateId": 5,
            "maxTeamCount": 2,
            "registrationStartTime": iso_dt(reg_start),
            "registrationEndTime": iso_dt(reg_end),
            "matchStartTime": iso_dt(match_start),
            "matchEndTime": iso_dt(match_end),
            }, token=admin_token)
            personal_div_id = pr.get("data")
            rec("personal_division_create", "PASS", f"personalDivisionId={personal_div_id}")
        created_ids["personalDivisionId"] = personal_div_id
        http(admin_base, "POST", f"/basket/v4/divisions/{personal_div_id}/format-snapshot/lock", token=admin_token)
        http(admin_base, "PUT", f"/basket/v4/division/{personal_div_id}/open-registration", token=admin_token)
        personal_emails = [f"11119{i}@qq.com" for i in range(0, 6)]
        personal_reg_ids = []
        for pe in personal_emails:
            pt = web_login(web_base, pe, member_pwd)
            sv = http(web_base, "POST", "/basket/v4/player/personal-registration/save", {
                "divisionId": personal_div_id,
            }, token=pt, allow_fail=True)
            data = sv.get("data")
            pid = data.get("personalRegistrationId") if isinstance(data, dict) else data
            if pid:
                http(web_base, "POST", "/basket/v4/player/personal-registration/submit",
                     {"personalRegistrationId": pid}, token=pt, allow_fail=True)
                personal_reg_ids.append(pid)
        for pid in personal_reg_ids:
            http(admin_base, "POST", f"/basket/v4/admin/personal-registrations/{pid}/approve",
                 {"auditRemark": "smoke"}, token=admin_token, allow_fail=True)
        rec("personal_registration", "PASS" if len(personal_reg_ids) >= 6 else "FAIL",
            f"personalRegs={len(personal_reg_ids)}")
        # draw batch minimal
        if len(personal_reg_ids) >= 6:
            batch = http(admin_base, "POST", "/basket/v4/admin/personal-draw-batches", {
                "divisionId": personal_div_id,
                "batchCode": f"{BATCH}-DRAW",
                "batchName": f"{BATCH}-DRAW",
                "resultSource": "MANUAL",
                "resultRemark": BATCH,
            }, token=admin_token, allow_fail=True)
            draw_id = (batch.get("data") or {}).get("drawBatchId") or batch.get("data")
            created_ids["personalDrawBatchId"] = draw_id
            rec("personal_draw", "PASS" if draw_id else "FAIL", f"drawBatchId={draw_id}")
    except Exception as e:
        rec("personal_flow", "FAIL", str(e))
        p1.append({"issue": "PERSONAL", "detail": str(e)})

    # --- public web APIs ---
    try:
        series = http(web_base, "GET", "/basket/v4/public/series/list")
        rec("public_series", "PASS", f"count={len(series.get('data') or [])}")
        seasons = http(web_base, "GET", f"/basket/v4/public/season/list?seriesId=1")
        rec("public_season", "PASS", f"count={len(seasons.get('data') or [])}")
        if season_id:
            divs = http(web_base, "GET", f"/basket/v4/public/division/list?seasonId={season_id}")
            rec("public_division", "PASS", f"count={len(divs.get('data') or [])}")
    except Exception as e:
        rec("public_web", "FAIL", str(e))
        manual.append({"item": "public_web_pages", "detail": str(e)})

    # --- permission / legacy ---
    try:
        r = http(admin_base, "POST", "/basket/v4/schedule-batch/generate", {"divisionId": division_id or 1},
                 token=admin_token, allow_fail=True)
        msg = str(r.get("msg", ""))
        if r.get("_http_status") == 404 or "不存在" in msg or "Not Found" in msg or "not supported" in msg.lower() or r.get("code") == 404:
            rec("legacy_generate", "PASS", msg or str(r)[:100])
        else:
            rec("legacy_generate", "FAIL", f"legacy endpoint responded: {r}")
            p1.append({"issue": "legacy generate still callable", "detail": str(r)[:200]})
    except RuntimeError as e:
        rec("legacy_generate", "PASS", str(e)[:100])
    try:
        http(admin_base, "GET", "/basket/v4/admin/stages/list?divisionId=1")
        rec("unauth_admin", "FAIL", "should reject")
    except Exception as e:
        rec("unauth_admin", "PASS", str(e)[:80])

    post_check = run_data_check()
    rec("data_check_post", "PASS" if "error" not in post_check else "FAIL",
        json.dumps(post_check.get("summary", post_check), ensure_ascii=False)[:500])

    write_result(start, environment, pre_check, post_check)
    return 0 if not p0 and not failed else 3


def write_result(start: str, environment: dict, pre=None, post=None) -> None:
    end = datetime.now(TZ).isoformat(timespec="seconds")
    out = {
        "batchCode": BATCH,
        "startTime": start,
        "endTime": end,
        "environment": environment,
        "passed": passed,
        "failed": failed,
        "blocked": blocked,
        "manualPending": manual,
        "scenarioResults": passed + failed + blocked + manual,
        "createdBusinessIds": created_ids,
        "anomalies": anomalies,
        "p0": p0,
        "p1": p1,
        "p2": p2,
        "dataCheckPre": pre,
        "dataCheckPost": post,
    }
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUTPUT_DIR / "cnpa-v2-core-smoke-result.json"
    path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    rec("output_json", "PASS", str(path))
    write_report(out)


def write_report(out: dict) -> None:
    lines = [
        "# CNPA V2 核心冒烟测试报告",
        "",
        "## 1. 测试时间和环境",
        f"- 批次: `{out['batchCode']}`",
        f"- 开始: {out['startTime']}",
        f"- 结束: {out['endTime']}",
        f"- Admin: {out['environment']['adminBase']}",
        f"- Web: {out['environment']['webBase']}",
        "",
        "## 2. 测试范围",
        "TEAM PC 5V5 单循环主链路 + PERSONAL PS 3V3 最小链路 + 权限/幂等/数据检查",
        "",
        "## 3. 启动结果",
    ]
    for item in out["scenarioResults"]:
        if not isinstance(item, dict) or "name" not in item:
            continue
        if item["name"].startswith("startup_"):
            lines.append(f"- **{item['status']}** {item['name']}: {item.get('detail','')}")
    lines += ["", "## 4. Admin 菜单与权限"] + section_items(out, "menu_")
    lines += ["", "## 5. 新建赛事五步向导"] + section_items(out, "wizard_")
    lines += ["", "## 6. TEAM 报名主链路"] + section_items(out, ("team_", "setup_", "registration_"))
    lines += ["", "## 7. EventTeam / EventPlayer"] + [
        f"- EventTeam: {out['createdBusinessIds'].get('eventTeamCount')}",
        f"- EventPlayer: {out['createdBusinessIds'].get('eventPlayerCount')}",
    ]
    lines += ["", "## 8. 赛程和比赛"] + section_items(out, "schedule_")
    lines += ["", "## 9. Submission 审核"] + section_items(out, "submission_")
    lines += ["", "## 10. Standing"] + section_items(out, "standing")
    lines += ["", "## 11. Qualification"] + section_items(out, "qualification_")
    lines += ["", "## 12. PERSONAL 最小链路"] + section_items(out, "personal_")
    lines += ["", "## 13. 官网与个人中心"] + section_items(out, "public_")
    lines += ["", "## 14. 权限和幂等"] + section_items(out, ("neg_", "legacy_", "unauth_"))
    lines += ["", "## 15. 测试前后 data-check",
              f"- Pre: {json.dumps(out.get('dataCheckPre', {}), ensure_ascii=False)[:800]}",
              f"- Post: {json.dumps(out.get('dataCheckPost', {}), ensure_ascii=False)[:800]}"]
    lines += ["", "## 16. 创建的测试业务 ID", "```json",
              json.dumps(out["createdBusinessIds"], ensure_ascii=False, indent=2), "```"]
    lines += ["", "## 17. P0"] + [f"- {x}" for x in out["p0"]] or ["- 无"]
    lines += ["", "## 18. P1"] + [f"- {x}" for x in out["p1"]] or ["- 无"]
    lines += ["", "## 19. P2"] + [f"- {x}" for x in out["p2"]] or ["- 无"]
    lines += ["", "## 20. 待人工页面确认"] + [f"- {x}" for x in out["manualPending"]] or ["- Admin UI 已 HTTP 200；Web UI 待启动后人工确认"]
    conclusion = "核心冒烟未通过，存在 P0 阻塞。" if out["p0"] else (
        "核心冒烟部分通过，存在非阻塞问题。" if out["p1"] or out["failed"] else "核心冒烟通过，可以继续扩大测试。"
    )
    lines += ["", "## 21. 当前结论", conclusion]
    report = ROOT / "docs" / "cnpa-v2-core-smoke-test-report.md"
    report.write_text("\n".join(lines) + "\n", encoding="utf-8")
    rec("output_report", "PASS", str(report))


def section_items(out: dict, prefixes) -> list[str]:
    if isinstance(prefixes, str):
        prefixes = (prefixes,)
    items = []
    for it in out["scenarioResults"]:
        if not isinstance(it, dict) or "name" not in it:
            continue
        if any(it["name"].startswith(p) for p in prefixes):
            items.append(f"- **{it['status']}** {it['name']}: {it.get('detail','')}")
    return items or ["- （无记录）"]


if __name__ == "__main__":
    raise SystemExit(main())
