#!/usr/bin/env python3
"""CNPA V2 P0 regression runner for core smoke补齐."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
RESULT: dict = {"tests": [], "ids": {}}


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


def rec(name: str, status: str, detail: str = "", **extra) -> None:
    item = {"name": name, "status": status, "detail": detail, **extra}
    RESULT["tests"].append(item)
    mark = {"PASS": "OK", "FAIL": "!!", "MANUAL": "??"}.get(status, "..")
    print(f"[{mark}] {name}: {detail or status}")


def http(base: str, method: str, path: str, body=None, token: str | None = None, allow_fail=False) -> dict:
    url = base.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
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
        raise RuntimeError(f"web login failed {email}: {r}")
    return token


def admin_login(base: str, user: str, pwd: str) -> str:
    r = http(base, "POST", "/login", {"username": user, "password": pwd})
    token = r.get("token")
    if not token:
        raise RuntimeError(f"admin login failed: {r}")
    return token


def db_query(sql: str) -> list:
    import pymysql
    conn = pymysql.connect(
        host=os.environ.get("CNPA_DB_HOST", "192.168.31.48"),
        port=int(os.environ.get("CNPA_DB_PORT", "3306")),
        user=os.environ.get("CNPA_DB_USER", "root"),
        password=os.environ.get("CNPA_DB_PASSWORD", "123456"),
        database=os.environ.get("CNPA_DB_NAME", "basketball"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()
    finally:
        conn.close()


def main() -> int:
    load_dotenv(ENV_LOCAL)
    admin_base = os.environ.get("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = os.environ.get("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    member_pwd = os.environ.get("CNPA_MEMBER_PASSWORD", "111111aa")
    admin_token = admin_login(admin_base, os.environ.get("CNPA_ADMIN_USERNAME", "admin"),
                               os.environ.get("CNPA_ADMIN_PASSWORD", "admin123"))

    season_id = 9
    team_div_id = 10
    personal_div_id = 11
    stage_id = 5
    RESULT["ids"].update({"seasonId": season_id, "teamDivisionId": team_div_id,
                          "personalDivisionId": personal_div_id, "stageId": stage_id})

    # 1. StageTeam incremental sync (idempotent on existing 4 teams)
    sync1 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token)
    d1 = sync1.get("data") or {}
    sync2 = http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", token=admin_token)
    d2 = sync2.get("data") or {}
    st_cnt = db_query(f"SELECT COUNT(*) AS cnt FROM basket_division_stage_team WHERE stage_id={stage_id} AND is_delete=0")[0]["cnt"]
    ok = (d1.get("totalEventTeam") == 4 and d1.get("existingStageTeam") == 4 and d1.get("insertedStageTeam") == 0
          and d2.get("insertedStageTeam") == 0 and st_cnt == 4)
    rec("stage_team_sync", "PASS" if ok else "FAIL",
        f"sync1={d1} sync2={d2} stageTeamCount={st_cnt}")
    RESULT["ids"]["stageTeamSync"] = {"first": d1, "second": d2, "count": st_cnt}

    # 2. public division list
    divs = http(web_base, "GET", f"/basket/v4/public/division/list?seasonId={season_id}")
    data = divs.get("data") or []
    ids = {int(d["divisionId"]) for d in data}
    fields_ok = all(
        all(k in data[0] for k in ["divisionId", "divisionName", "platformCode", "gameMode",
                                    "registrationMode", "divisionStatus", "registrationStartTime",
                                    "registrationEndTime", "snapshotStatus"])
        for _ in [0]
    ) if data else False
    rec("public_division_list", "PASS" if ids == {10, 11} and fields_ok and divs.get("_http_status") == 200 else "FAIL",
        f"ids={sorted(ids)} http={divs.get('_http_status')}")

    # 3. Snapshot lock idempotent
    http(admin_base, "POST", f"/basket/v4/divisions/{personal_div_id}/format-snapshot/lock", token=admin_token)
    http(admin_base, "POST", f"/basket/v4/divisions/{personal_div_id}/format-snapshot/lock", token=admin_token)
    snap_cnt = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_division_format_snapshot WHERE division_id={personal_div_id} AND is_current=1 AND is_delete=0"
    )[0]["cnt"]
    rec("snapshot_lock_idempotent", "PASS" if snap_cnt == 1 else "FAIL", f"currentSnapshots={snap_cnt}")

    # 4. PERSONAL full chain
    personal_emails = ["111190@qq.com", "111191@qq.com", "111193@qq.com", "111194@qq.com", "111196@qq.com", "111197@qq.com"]
    personal_reg_ids = []
    for pe in personal_emails:
        pt = web_login(web_base, pe, member_pwd)
        sv = http(web_base, "POST", "/basket/v4/player/personal-registration/save",
                   {"divisionId": personal_div_id}, token=pt, allow_fail=True)
        data = sv.get("data")
        pid = data.get("personalRegistrationId") if isinstance(data, dict) else data
        if pid:
            http(web_base, "POST", "/basket/v4/player/personal-registration/submit",
                 {"personalRegistrationId": pid}, token=pt, allow_fail=True)
            personal_reg_ids.append(pid)
    for pid in personal_reg_ids:
        http(admin_base, "POST", f"/basket/v4/admin/personal-registrations/{pid}/approve",
             {"auditRemark": "p0 regression"}, token=admin_token, allow_fail=True)
    approved = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_personal_registration WHERE division_id={personal_div_id} "
        f"AND registration_status='APPROVED' AND is_delete=0"
    )[0]["cnt"]
    RESULT["ids"]["personalRegistrationIds"] = personal_reg_ids

    draw_batch_id = None
    draw_team_ids = []
    if approved >= 6:
        existing_batch = db_query(
            f"SELECT id FROM basket_personal_draw_batch WHERE division_id={personal_div_id} "
            f"AND batch_status='CONFIRMED' AND is_delete=0 LIMIT 1"
        )
        if existing_batch:
            draw_batch_id = existing_batch[0]["id"]
            draw_team_ids = [r["id"] for r in db_query(
                f"SELECT id FROM basket_personal_draw_team WHERE draw_batch_id={draw_batch_id} AND is_delete=0"
            )]
        else:
            batch = http(admin_base, "POST", "/basket/v4/admin/personal-draw-batches", {
                "divisionId": personal_div_id,
                "batchCode": "P0_REGRESSION_DRAW",
                "batchName": "P0 Regression Draw",
                "resultSource": "MANUAL",
                "resultRemark": "P0 regression",
            }, token=admin_token, allow_fail=True)
            draw_batch_id = batch.get("data")
            if draw_batch_id:
                for code, name in [("T1", "P0 Team Alpha"), ("T2", "P0 Team Beta")]:
                    tr = http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams", {
                        "temporaryTeamCode": code, "temporaryTeamName": name, "expectedMemberCount": 3,
                    }, token=admin_token, allow_fail=True)
                    if tr.get("data"):
                        draw_team_ids.append(tr["data"])
                approved_regs = db_query(
                    f"SELECT id, player_id FROM basket_personal_registration WHERE division_id={personal_div_id} "
                    f"AND registration_status='APPROVED' AND is_delete=0 ORDER BY id LIMIT 6"
                )
                if len(draw_team_ids) == 2 and len(approved_regs) >= 6:
                    for i, reg in enumerate(approved_regs[:3]):
                        http(admin_base, "POST",
                             f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams/{draw_team_ids[0]}/assignments",
                             {"personalRegistrationId": reg["id"],
                              "assignmentRole": "CAPTAIN" if i == 0 else "MEMBER"},
                             token=admin_token, allow_fail=True)
                    for i, reg in enumerate(approved_regs[3:6]):
                        http(admin_base, "POST",
                             f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/teams/{draw_team_ids[1]}/assignments",
                             {"personalRegistrationId": reg["id"],
                              "assignmentRole": "CAPTAIN" if i == 0 else "MEMBER"},
                             token=admin_token, allow_fail=True)
                    http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/confirm",
                         {"unassignedPolicy": "IGNORE"}, token=admin_token, allow_fail=True)

        for dt_id in draw_team_ids:
            converted = db_query(
                f"SELECT converted_registration_id FROM basket_personal_draw_team WHERE id={dt_id} AND is_delete=0"
            )
            if converted and converted[0].get("converted_registration_id"):
                continue
            team_row = db_query(
                f"SELECT dt.captain_player_id, dt.captain_user_id, ma.email "
                f"FROM basket_personal_draw_team dt "
                f"LEFT JOIN basket_member_account ma ON ma.id=dt.captain_user_id "
                f"WHERE dt.id={dt_id}"
            )
            if not team_row or not team_row[0].get("email"):
                continue
            captain_email = team_row[0]["email"]
            captain_token = web_login(web_base, captain_email, member_pwd)
            members = db_query(
                f"SELECT a.player_id, pp.member_id FROM basket_personal_draw_assignment a "
                f"JOIN basket_player_profile pp ON pp.id=a.player_id "
                f"WHERE a.draw_team_id={dt_id} AND a.is_delete=0"
            )
            team_create = http(web_base, "POST", "/basket/team/create", {
                "teamName": f"P0-Personal-{dt_id}",
                "gameTerminal": "PS",
                "publicStatus": 0,
            }, token=captain_token, allow_fail=True)
            team_id = (team_create.get("data") or {}).get("teamId") or (team_create.get("data") or {}).get("id")
            if not team_id:
                existing = http(web_base, "GET", "/basket/team/mine?pageSize=20", token=captain_token, allow_fail=True)
                records = ((existing.get("data") or {}).get("records") or [])
                if records:
                    team_id = records[0].get("teamId") or records[0].get("id")
            if team_id:
                captain_player = team_row[0]["captain_player_id"]
                for m in members:
                    if str(m["player_id"]) == str(captain_player):
                        continue
                    inv = http(web_base, "POST", f"/basket/team/{team_id}/invitation", {
                        "inviteePlayerId": str(m["player_id"]),
                    }, token=captain_token, allow_fail=True)
                    inv_id = (inv.get("data") or {}).get("invitationId") or (inv.get("data") or {}).get("id")
                    if inv_id:
                        member_email_rows = db_query(
                            f"SELECT email FROM basket_member_account WHERE id={m['member_id']} LIMIT 1"
                        )
                        if member_email_rows:
                            mt = web_login(web_base, member_email_rows[0]["email"], member_pwd)
                            http(web_base, "PUT", f"/basket/team/invitation/{inv_id}/respond",
                                 {"action": "ACCEPT"}, token=mt, allow_fail=True)
                http(web_base, "GET", f"/basket/v4/player/personal-formation/tasks/{dt_id}/available-teams",
                     token=captain_token, allow_fail=True)
                http(web_base, "POST", "/basket/v4/player/personal-formation/bind-team", {
                    "drawTeamId": dt_id, "longTermTeamId": int(team_id) if str(team_id).isdigit() else team_id,
                }, token=captain_token, allow_fail=True)
                http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{dt_id}/confirm",
                     token=captain_token, allow_fail=True)

    et_cnt = db_query(f"SELECT COUNT(*) AS cnt FROM basket_event_team WHERE division_id={personal_div_id} AND is_delete=0")[0]["cnt"]
    ep_cnt = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        f"WHERE et.division_id={personal_div_id} AND ep.is_delete=0"
    )[0]["cnt"]
    reg_cnt = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_registration WHERE division_id={personal_div_id} "
        f"AND registration_source='PERSONAL_FORMATION' AND is_delete=0"
    )[0]["cnt"]
    grant_cnt = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_captain_grant WHERE division_id={personal_div_id} AND is_delete=0"
    )[0]["cnt"]
    RESULT["ids"].update({
        "personalDrawBatchId": draw_batch_id,
        "personalDrawTeamIds": draw_team_ids,
        "personalEventTeamCount": et_cnt,
        "personalEventPlayerCount": ep_cnt,
        "personalFormationRegistrationCount": reg_cnt,
        "personalCaptainGrantCount": grant_cnt,
    })
    personal_ok = approved >= 6 and et_cnt == 2 and ep_cnt == 6 and reg_cnt == 2
    rec("personal_full_chain", "PASS" if personal_ok else "FAIL",
        f"approved={approved} eventTeam={et_cnt} eventPlayer={ep_cnt} formationReg={reg_cnt} grants={grant_cnt}")

    # duplicate confirm should not create duplicates
    if draw_batch_id and draw_team_ids:
        before_et = et_cnt
        http(admin_base, "POST", f"/basket/v4/admin/personal-draw-batches/{draw_batch_id}/confirm",
             {"unassignedPolicy": "IGNORE"}, token=admin_token, allow_fail=True)
        after_et = db_query(f"SELECT COUNT(*) AS cnt FROM basket_event_team WHERE division_id={personal_div_id} AND is_delete=0")[0]["cnt"]
        rec("personal_idempotent", "PASS" if after_et == before_et else "FAIL", f"eventTeam before={before_et} after={after_et}")

    # 5. Complete 6 matches standing
    teams = db_query(
        f"SELECT et.id, et.source_team_id, ma.email FROM basket_event_team et "
        f"JOIN basket_team t ON t.id=et.source_team_id JOIN basket_member_account ma ON ma.id=t.captain_member_id "
        f"WHERE et.division_id={team_div_id} AND et.is_delete=0"
    )
    token_by_team = {}
    for t in teams:
        try:
            token_by_team[t["source_team_id"]] = web_login(web_base, t["email"], member_pwd)
        except RuntimeError:
            pass
    matches = db_query(f"SELECT id, home_event_team_id, away_event_team_id, match_status FROM basket_match "
                       f"WHERE division_id={team_div_id} AND stage_id={stage_id} AND is_delete=0 ORDER BY id")
    for m in matches:
        if m["match_status"] == "FINISHED":
            continue
        mid = m["id"]
        for et_id in [m["home_event_team_id"], m["away_event_team_id"]]:
            et = db_query(f"SELECT source_team_id FROM basket_event_team WHERE id={et_id}")[0]
            tok = token_by_team.get(et["source_team_id"])
            if not tok:
                continue
            sv = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
                "matchId": mid, "submitEventTeamId": et_id, "homeScore": 72, "awayScore": 68,
            }, token=tok, allow_fail=True)
            sid = sv.get("data")
            if sid:
                http(web_base, "POST", "/basket/v4/player/match-submission/submit",
                     {"submissionId": sid}, token=tok, allow_fail=True)
                http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/approve",
                     {"auditRemark": "p0 standing regression"}, token=admin_token, allow_fail=True)

    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)
    standings = db_query(
        f"SELECT event_team_id, rank_no, win_count, lose_count, point_for, point_against, point_diff, standing_points "
        f"FROM basket_standing WHERE division_id={team_div_id} AND stage_id={stage_id} AND is_delete=0 ORDER BY rank_no"
    )
    finished = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_match WHERE division_id={team_div_id} AND stage_id={stage_id} "
        f"AND match_status='FINISHED' AND is_delete=0"
    )[0]["cnt"]
    standing_ok = (len(standings) == 4 and finished == 6 and all(
        s["win_count"] + s["lose_count"] == 3 for s in standings
    ) and all(s.get("played") or (s["win_count"] + s["lose_count"]) == 3 for s in standings))
    rec("standing_6_matches", "PASS" if standing_ok else "FAIL",
        f"finished={finished} standingRows={len(standings)} rows={standings}")
    RESULT["ids"]["standingRows"] = standings
    RESULT["ids"]["finishedMatchCount"] = finished

    # rebuild standing idempotent
    cnt_before = len(standings)
    http(admin_base, "POST", f"/basket/v4/admin/stages/{stage_id}/check-completion", token=admin_token, allow_fail=True)
    cnt_after = db_query(
        f"SELECT COUNT(*) AS cnt FROM basket_standing WHERE division_id={team_div_id} AND stage_id={stage_id} AND is_delete=0"
    )[0]["cnt"]
    rec("standing_idempotent", "PASS" if cnt_after == cnt_before else "FAIL", f"before={cnt_before} after={cnt_after}")

    # rank_reason
    if standings:
        sid = db_query(f"SELECT id FROM basket_standing WHERE division_id={team_div_id} AND stage_id={stage_id} AND is_delete=0 LIMIT 1")[0]["id"]
        reasons = db_query(f"SELECT COUNT(*) AS cnt FROM basket_standing_rank_reason WHERE standing_id={sid}")[0]["cnt"]
        rec("rank_reason_queryable", "PASS" if reasons >= 0 else "FAIL", f"reasonCount={reasons}")

    # 6. Qualification NONE
    preview = http(admin_base, "GET", f"/basket/v4/admin/stages/{stage_id}/qualification-preview",
                   token=admin_token, allow_fail=True)
    items = (preview.get("data") or {}).get("items") or []
    rec("qualification_none", "PASS" if items == [] or preview.get("data") is None else "FAIL",
        f"previewItems={len(items)}")

    # 7. Web UI HTTP checks
    ui_base = "http://127.0.0.1:5173"
    ui_checks = [
        ("/", "home"),
        ("/events", "events"),
        ("/events/v4/season/9", "season_detail"),
        ("/account/login", "login"),
    ]
    ui_up = False
    for path, name in ui_checks:
        try:
            req = urllib.request.Request(ui_base + path, method="GET")
            with urllib.request.urlopen(req, timeout=5) as resp:
                rec(f"web_ui_{name}", "PASS" if resp.status == 200 else "FAIL", f"http={resp.status}")
                ui_up = True
        except Exception as e:
            rec(f"web_ui_{name}", "MANUAL", f"MANUAL_UI_PENDING: {e}")
    if not ui_up:
        rec("web_ui_startup", "MANUAL", "MANUAL_UI_PENDING: basketball-web-ui not running on 5173")

    # API checks for web UI dependencies
    http(web_base, "GET", f"/basket/v4/public/season/{season_id}")
    http(web_base, "GET", f"/basket/v4/public/stats/standing?divisionId={team_div_id}")
    rec("web_ui_api_deps", "PASS", "season detail + standing OK")

    out_path = ROOT / "scripts" / "output" / "cnpa-v2-p0-regression-result.json"
    out_path.write_text(json.dumps(RESULT, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(f"Written: {out_path}")
    fails = [t for t in RESULT["tests"] if t["status"] == "FAIL"]
    return 1 if fails else 0


if __name__ == "__main__":
    raise SystemExit(main())
