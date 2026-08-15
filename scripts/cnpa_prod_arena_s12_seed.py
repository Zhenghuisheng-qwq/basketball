#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Seed CNPA Arena 2026 S12 (3V3, 20 teams) on remote production DB."""
from __future__ import annotations

import json
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e

fs.ADMIN = "http://127.0.0.1:18090"
fs.WEB = "http://127.0.0.1:8881"
fs.DB.update({
    "host": "111.229.241.29",
    "user": "root",
    "password": "zlzcnpa@@qaq",
    "database": "basketball",
})
fs.MARKER = "ARENA_S12_SUBMIT"
fs.ID_BASE = 2099401512300000000
e2e.ADMIN = fs.ADMIN
e2e.WEB = fs.WEB
e2e.fs = fs

ADMIN = fs.ADMIN
req = fs.req
SEASON_NAME = "S12"
SEASON_TITLE = "CNPA Arena 2026 S12"
DIV_NAME = "CNPA Arena 2026 S12 - PS/Xbox 3V3"
PASS = fs.MEMBER_PASS


def log(msg):
    print(msg, flush=True)


def need(resp, name):
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def team_defs():
    names = [
        "猎鹰", "雷霆", "风暴", "夜翼", "赤焰", "寒霜", "黑骑", "银狼",
        "金鲨", "青龙", "白虎", "朱雀", "玄武", "星火", "狂澜", "破晓",
        "余烬", "幻影", "铁壁", "疾风",
    ]
    out = []
    for i, name in enumerate(names, 1):
        code = f"A{i:02d}"
        out.append({
            "code": code,
            "name": f"S12{name}",
            "short": code,
            "players": [f"S12{name}C", f"S12{name}2", f"S12{name}3"],
        })
    return out


def set_reg_window(did, start, end):
    """Temporarily widen/restore registration dates so Web eligibility can pass."""
    fs.db_exec(
        "UPDATE basket_division SET registration_start_time=%s, registration_end_time=%s, update_time=NOW() "
        "WHERE id=%s AND is_delete=0",
        (start, end, did),
    )


def create_division(token, season_id):
    existing = e2e.find_division(DIV_NAME)
    if existing:
        return existing
    body = {
        "seasonId": season_id,
        "platformCode": "PS_XBOX",
        "gameMode": "THREE_V_THREE",
        "divisionName": DIV_NAME,
        "registrationMode": "TEAM",
        "formatTemplateId": 3,
        "maxTeamCount": 16,
        "maxPersonCount": 64,
        "lineupMinCount": 3,
        "lineupMaxCount": 8,
        "registrationStartTime": "2026-07-01 00:00:00",
        "registrationEndTime": "2026-07-31 23:59:59",
        "matchStartTime": "2026-08-01 00:00:00",
        "matchEndTime": "2026-09-30 23:59:59",
        "sortOrder": 1,
    }
    r = need(req(f"{ADMIN}/basket/v4/division", "POST", body, token), "create-div")
    return int(fs.extract_id(r))


def assign_august(token, stage_id):
    units = e2e.schedule_units(token, stage_id)
    tms = ["20:00", "21:00", "22:00"]
    for i, u in enumerate(units):
        if not isinstance(u, dict):
            continue
        if (u.get("scheduleStatus") or "") == "SCHEDULED" and u.get("scheduledDate"):
            continue
        day = 1 + (i // 3)
        body = {
            "unitType": u.get("unitType") or "MATCH_SERIES",
            "unitId": u.get("unitId") or u.get("id"),
            "scheduledDate": f"2026-08-{day:02d}",
            "scheduledStartTime": tms[i % 3],
            "scheduleSource": "MANUAL",
        }
        ar = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/schedule/assign", "POST", body, token)
        if ar.get("code") != 200:
            log(f"  assign warn {body}: {ar.get('msg')}")


def admin_enter(token, match_id, home, away, reason):
    return req(
        f"{ADMIN}/basket/v4/admin/stage-exception/match-official-result",
        "POST",
        {"matchId": match_id, "homeScore": home, "awayScore": away, "reason": reason},
        token,
    )


def finish_two_r16(token, stage_id):
    series = fs.db_query(
        "SELECT id FROM basket_match_series WHERE stage_id=%s AND is_delete=0 AND round_no=1 "
        "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL ORDER BY id LIMIT 2",
        (stage_id,),
    )
    done = []
    for s in series:
        sid = int(s["id"])
        games = fs.db_query(
            "SELECT id FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no, id",
            (sid,),
        )
        wins = 0
        for g in games:
            if wins >= 2:
                break
            r = admin_enter(token, int(g["id"]), 15, 8, "S12 seed played games")
            if r.get("code") != 200:
                log(f"  enter warn {g['id']}: {r.get('msg')}")
                continue
            wins += 1
        after = fs.db_query(
            "SELECT series_status, winner_event_team_id FROM basket_match_series WHERE id=%s",
            (sid,),
        )
        done.append({"seriesId": sid, "after": after})
    return done


def main():
    if not fs.wait_server(f"{ADMIN}/", 90):
        raise RuntimeError("remote admin 18090 not up")
    if not fs.wait_server(f"{fs.WEB}/", 10):
        raise RuntimeError("web 8881 not up")
    token = fs.admin_login()
    log("admin login ok")
    defs = team_defs()
    teams = fs.seed_org_pool("S12", defs, offset=40000)
    log(f"org pool {len(teams)} teams")
    emails = [e for t in teams for e in t["emails"]]
    e2e.clear_locks([t["captain_email"] for t in teams] + emails)
    season_id = e2e.create_season(token, 2, SEASON_NAME, SEASON_TITLE)
    log(f"season {season_id}")
    req(f"{ADMIN}/basket/v4/season/{season_id}", "PUT", {
        "id": season_id,
        "seasonStartTime": "2026-07-01 00:00:00",
        "seasonEndTime": "2026-09-30 23:59:59",
        "displayTitle": SEASON_TITLE,
        "seasonName": SEASON_NAME,
    }, token)
    did = create_division(token, season_id)
    log(f"division {did}")
    e2e.apply_and_lock(token, did, "ARENA_STANDARD_V1", "TEAM", "THREE_V_THREE")
    e2e.open_reg(token, did)
    fs.create_teams_flow(token, str(did), teams)
    log("teams created/approved")
    fs.populate_members(teams, invite_until=len(teams), full_players=3)
    log("members populated")
    # 报名展示仍为 7 月；今天已过窗口，临时拉开以便走完报名审核
    set_reg_window(did, "2026-07-01 00:00:00", "2026-08-31 23:59:59")
    e2e.register_lock(token, did, teams)
    set_reg_window(did, "2026-07-01 00:00:00", "2026-07-31 23:59:59")
    log("registered + roster locked, July window restored")
    st = e2e.stages(token, did)
    if not st:
        raise RuntimeError("no stages")
    stage_id = int(st[0]["id"])
    e2e.seed_draw_confirm(token, did, stage_id, seed_n=4)
    if not e2e.is_published(stage_id):
        gen = e2e.generate_only(token, stage_id, init=False)
        if gen.get("code") != 200 and "已" not in str(gen.get("msg") or ""):
            raise RuntimeError(f"generate: {gen}")
        assign_august(token, stage_id)
        pub = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/publish-schedule", "POST", token=token)
        if pub.get("code") != 200 and "已" not in str(pub.get("msg") or ""):
            raise RuntimeError(f"publish: {pub}")
    finished = finish_two_r16(token, stage_id)
    formal = fs.db_query(
        "SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND participation_role='FORMAL' AND is_delete=0",
        (did,),
    )[0]["c"]
    cand = fs.db_query(
        "SELECT COUNT(*) c FROM basket_registration WHERE division_id=%s AND participation_role='CANDIDATE' AND is_delete=0",
        (did,),
    )[0]["c"]
    series_n = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_series WHERE division_id=%s AND is_delete=0", (did,)
    )[0]["c"]
    match_n = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0", (did,)
    )[0]["c"]
    fin_n = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND match_status='FINISHED' AND is_delete=0",
        (did,),
    )[0]["c"]
    open_n = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND match_status IN ('SCHEDULED','READY') AND is_delete=0",
        (did,),
    )[0]["c"]
    captains = [{
        "team": t["name"],
        "short": t["short"],
        "email": t["captain_email"],
        "password": PASS,
        "teamId": t.get("team_id"),
    } for t in teams]
    out = {
        "seasonId": season_id,
        "seasonName": SEASON_TITLE,
        "divisionId": did,
        "divisionName": DIV_NAME,
        "stageId": stage_id,
        "formal": int(formal),
        "candidate": int(cand),
        "series": int(series_n),
        "match": int(match_n),
        "finishedMatch": int(fin_n),
        "openMatch": int(open_n),
        "finishedSeries": finished,
        "captains": captains,
        "at": datetime.now().isoformat(timespec="seconds"),
    }
    path = ROOT / "scripts" / "cnpa_prod_arena_s12_seed_result.json"
    path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    log(json.dumps(out, ensure_ascii=False, indent=2))
    log(f"WROTE {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
