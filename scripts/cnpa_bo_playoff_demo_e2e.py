#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Create an isolated 8-team League season for BO3/BO5 playoff UI acceptance."""
from __future__ import annotations

import json
import random
import sys
import traceback
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:8881"
fs.DB.update({"host": "192.168.31.48", "database": "basketball", "user": "root", "password": "123456"})
fs.REDIS_HOST = "192.168.31.48"
fs.MARKER = "BO_PLAYOFF_DEMO_2026"
fs.ID_BASE = 2099260826000000000

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e
import cnpa_league16_real_data_e2e as l16

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:8881"
e2e.ADMIN = fs.ADMIN
e2e.WEB = fs.WEB
e2e.fs = fs
l16.ADMIN = fs.ADMIN
l16.WEB = fs.WEB
l16.MARKER = fs.MARKER

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req
as_id = fs.as_id

SEASON_NAME = "2026 BO Playoff Demo"
SEASON_TITLE = "2026 BO Playoff Demo"
DIV_NAME = "PC / BO TEST"
SERIES_ID = 1
MARKER = fs.MARKER
UI_BASE = "http://localhost:5175"


def log(msg: str) -> None:
    print(msg, flush=True)


def need(resp: dict, name: str) -> dict:
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def team_defs() -> list[dict]:
    out = []
    for i in range(1, 9):
        code = f"T{i:02d}"
        out.append({
            "code": code,
            "name": f"BO-{code}",
            "short": code,
            "players": [f"BO-{code}-P{j}" for j in range(1, 6)],
        })
    return out


def team_no(name: str | None) -> int:
    text = str(name or "")
    for i in range(8, 0, -1):
        if f"T{i:02d}" in text:
            return i
    return 99


def find_season() -> dict | None:
    rows = fs.db_query(
        "SELECT id, season_name, display_title, season_status, publish_status "
        "FROM basket_season WHERE is_delete=0 AND (season_name=%s OR display_title=%s) "
        "ORDER BY id DESC LIMIT 1",
        (SEASON_NAME, SEASON_TITLE),
    )
    return rows[0] if rows else None


def create_season(token: str) -> int:
    existing = find_season()
    if existing:
        sid = int(as_id(existing["id"]))
        if str(existing.get("publish_status") or "") != "PUBLISHED":
            req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
        return sid
    body = {
        "seriesId": SERIES_ID,
        "seasonYear": 2026,
        "seasonName": SEASON_NAME,
        "displayTitle": SEASON_TITLE,
        "description": MARKER,
        "registrationMode": "TEAM",
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-10-31 23:59:59",
    }
    r = need(req(f"{ADMIN}/basket/v4/season", "POST", body, token), "create-season")
    sid = int(fs.extract_id(r))
    pr = req(f"{ADMIN}/basket/v4/season/{sid}/publish", "PUT", token=token)
    if pr.get("code") != 200 and "已" not in str(pr.get("msg") or ""):
        raise RuntimeError(f"publish season {sid}: {pr}")
    return sid


def create_division(token: str, season_id: int) -> int:
    existing = e2e.find_division(DIV_NAME)
    if existing:
        return existing
    body = {
        "seasonId": season_id,
        "platformCode": "PC",
        "gameMode": "FIVE_V_FIVE",
        "divisionName": DIV_NAME,
        "registrationMode": "TEAM",
        "formatTemplateId": 2,
        "maxTeamCount": 8,
        "maxPersonCount": 64,
        "lineupMinCount": 5,
        "lineupMaxCount": 8,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-28 23:50:00",
        "matchStartTime": "2026-08-28 23:50:00",
        "matchEndTime": "2026-10-31 23:59:59",
        "sortOrder": 9,
    }
    r = need(req(f"{ADMIN}/basket/v4/division", "POST", body, token), "create-div")
    return int(fs.extract_id(r))


def align_windows(token: str, season_id: int, did: int) -> None:
    req(f"{ADMIN}/basket/v4/season/{season_id}", "PUT", {
        "id": season_id,
        "seasonName": SEASON_NAME,
        "displayTitle": SEASON_TITLE,
        "seasonStartTime": "2026-08-01 00:00:00",
        "seasonEndTime": "2026-10-31 23:59:59",
    }, token)
    dr = req(f"{ADMIN}/basket/v4/division/{did}", "PUT", {
        "id": did,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-28 23:50:00",
        "matchStartTime": "2026-08-28 23:50:00",
        "matchEndTime": "2026-10-31 23:59:59",
    }, token)
    if dr.get("code") != 200:
        raise RuntimeError(f"align window failed: {dr}")
    e2e.open_reg(token, did)


def widen_after_lock(token: str, did: int) -> None:
    wr = req(f"{ADMIN}/basket/v4/division/{did}", "PUT", {
        "id": did,
        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-26 00:00:00",
        "matchStartTime": "2026-08-26 00:00:00",
        "matchEndTime": "2026-10-31 23:59:59",
    }, token)
    log(f"post-lock window {wr.get('code')} {wr.get('msg')}")


def event_team_map(did: int) -> dict[str, dict]:
    rows = fs.db_query(
        "SELECT id, team_name_snapshot, captain_member_id "
        "FROM basket_event_team WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    out = {}
    for r in rows:
        eid = as_id(r["id"])
        out[eid] = {
            "id": eid,
            "name": r.get("team_name_snapshot") or "",
            "no": team_no(r.get("team_name_snapshot")),
            "captain_member_id": str(r.get("captain_member_id") or ""),
        }
    return out


def play_regular(admin_token, web_by, et_map, did, stage_id, teams) -> int:
    matches = fs.list_matches(admin_token, str(did), stage_id)
    email_by = {}
    for t in teams:
        for eid, meta in et_map.items():
            if meta["no"] == team_no(t["name"]):
                email_by[eid] = t["captain_email"]
    proof_cache = {}
    rng = random.Random(20260910)
    played = 0
    for m in matches:
        if m.get("seriesId") or m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home, away = m.get("homeEventTeamId"), m.get("awayEventTeamId")
        if not home or not away:
            continue
        hn = et_map.get(as_id(home), {}).get("no", 99)
        an = et_map.get(as_id(away), {}).get("no", 99)
        win = rng.randint(82, 104)
        lose = rng.randint(64, win - 4)
        if hn < an:
            hs, aws, submit = win, lose, home
        else:
            hs, aws, submit = lose, win, away
        wt = web_by.get(as_id(submit))
        proof = l16.upload_proof(wt, proof_cache, email_by.get(as_id(submit)) or as_id(submit))
        l16.submit_full_match(admin_token, wt, m, hs, aws, submit, rng, proof)
        played += 1
        if played % 7 == 0:
            log(f"  RR played {played}")
    return played


def series_rows(stage_id: int) -> list[dict]:
    return fs.db_query(
        "SELECT s.id, s.round_no, s.best_of, s.home_event_team_id h, s.away_event_team_id a, "
        "s.home_win_count, s.away_win_count, s.winner_event_team_id, s.series_status, "
        "eh.team_name_snapshot hn, ea.team_name_snapshot an "
        "FROM basket_match_series s "
        "LEFT JOIN basket_event_team eh ON eh.id=s.home_event_team_id "
        "LEFT JOIN basket_event_team ea ON ea.id=s.away_event_team_id "
        "WHERE s.stage_id=%s AND s.is_delete=0 ORDER BY s.round_no, s.id",
        (stage_id,),
    )


def play_series_scripted(admin_token, web_by, et_map, teams, series_id: int,
                         winner_id, score_pairs: list[tuple[int, int]], rng) -> None:
    email_by = {}
    for t in teams:
        for eid, meta in et_map.items():
            if meta["no"] == team_no(t["name"]):
                email_by[eid] = t["captain_email"]
    proof_cache = {}
    winner_id = as_id(winner_id)
    for fav, dog in score_pairs:
        e2e.sync_series_child_teams()
        s = fs.get_series(admin_token, series_id)
        if s.get("winnerEventTeamId") or s.get("seriesStatus") in ("FINISHED", "COMPLETED"):
            return
        games = sorted(s.get("games") or [], key=lambda g: g.get("seriesGameNo") or 0)
        game = next((g for g in games if (g.get("matchStatus") or "") not in ("FINISHED", "FORFEITED")
                     and not g.get("skipped")), None)
        if not game:
            return
        mid = game.get("matchId") or game.get("id")
        m = (req(f"{ADMIN}/basket/v4/match/{mid}", token=admin_token).get("data") or {})
        ghome, gaway = m.get("homeEventTeamId"), m.get("awayEventTeamId")
        if as_id(ghome) == winner_id:
            hs, aws = fav, dog
            submit = ghome if fav > dog else gaway
        else:
            hs, aws = dog, fav
            submit = gaway if fav > dog else ghome
        wt = web_by.get(as_id(submit))
        proof = l16.upload_proof(wt, proof_cache, email_by.get(as_id(submit)) or as_id(submit))
        l16.submit_full_match(admin_token, wt, m, hs, aws, submit, rng, proof)


def play_series_sweep(admin_token, web_by, et_map, teams, series_id: int, winner_id, rng) -> None:
    best = int((fs.get_series(admin_token, series_id).get("bestOf") or 3))
    need_wins = best // 2 + 1
    pairs = [(88, 70)] * need_wins
    play_series_scripted(admin_token, web_by, et_map, teams, series_id, winner_id, pairs, rng)


def dump_series_detail(series_id: int) -> dict:
    s = fs.db_query(
        "SELECT s.id, s.round_no, s.best_of, s.home_win_count, s.away_win_count, "
        "s.winner_event_team_id, s.series_status, "
        "eh.team_name_snapshot home, ea.team_name_snapshot away "
        "FROM basket_match_series s "
        "LEFT JOIN basket_event_team eh ON eh.id=s.home_event_team_id "
        "LEFT JOIN basket_event_team ea ON ea.id=s.away_event_team_id "
        "WHERE s.id=%s",
        (series_id,),
    )[0]
    games = fs.db_query(
        "SELECT id, series_game_no, home_score, away_score, match_status, winner_event_team_id, match_time "
        "FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no, id",
        (series_id,),
    )
    return {"series": s, "games": games}


def verify_public(did: int, season_id: int, series_ids: list[int]) -> dict:
    out = {}
    sch = req(f"{WEB}/basket/v4/public/division/{did}/schedule")
    out["schedule"] = {"code": sch.get("code"), "n": len(sch.get("data") or [])}
    det = req(f"{WEB}/basket/v4/public/season/{season_id}")
    out["season"] = {"code": det.get("code"), "title": (det.get("data") or {}).get("displayTitle")}
    series_ok = []
    for sid in series_ids:
        r = req(f"{WEB}/basket/v4/public/match-series/{sid}")
        series_ok.append({
            "id": sid,
            "code": r.get("code"),
            "bestOf": (r.get("data") or {}).get("bestOf"),
            "homeWin": (r.get("data") or {}).get("homeWinCount"),
            "awayWin": (r.get("data") or {}).get("awayWinCount"),
            "games": len((r.get("data") or {}).get("games") or []),
        })
    out["series"] = series_ok
    return out


def emit_report(did: int, season_id: int, po_id: int) -> int:
    rows = series_rows(po_id)
    bo3 = next((r for r in rows if int(r.get("best_of") or 0) == 3
                and {r.get("hn"), r.get("an")} == {"BO-T01", "BO-T08"}), None)
    if not bo3:
        bo3 = next((r for r in rows if int(r.get("best_of") or 0) == 3), None)
    bo5 = next((r for r in rows if int(r.get("best_of") or 0) == 5
                and "BO-T01" in {r.get("hn"), r.get("an")}), None)
    if not bo5:
        bo5 = next((r for r in rows if int(r.get("best_of") or 0) == 5), None)
    d3 = dump_series_detail(int(bo3["id"])) if bo3 else None
    d5 = dump_series_detail(int(bo5["id"])) if bo5 else None
    pub = verify_public(did, season_id, [int(x["id"]) for x in (bo3, bo5) if x])
    report = {
        "seasonId": season_id,
        "seasonTitle": SEASON_TITLE,
        "divisionId": did,
        "divisionName": DIV_NAME,
        "bo3": d3,
        "bo5": d5,
        "public": pub,
        "urls": {
            "season": f"{UI_BASE}/events/{season_id}?tab=stage",
            "schedule": f"{UI_BASE}/events/{season_id}?tab=schedule",
            "bo3Series": f"{UI_BASE}/events/match-series/{d3['series']['id']}" if d3 else None,
            "bo5Series": f"{UI_BASE}/events/match-series/{d5['series']['id']}" if d5 else None,
        },
    }
    if d3:
        report["urls"]["bo3Games"] = [
            f"{UI_BASE}/events/match/{g['id']}?seasonId={season_id}&divisionId={did}" for g in d3["games"]
        ]
    if d5:
        report["urls"]["bo5Games"] = [
            f"{UI_BASE}/events/match/{g['id']}?seasonId={season_id}&divisionId={did}" for g in d5["games"]
        ]
    Path(__file__).with_name("cnpa_bo_playoff_demo_result.json").write_text(
        json.dumps(report, ensure_ascii=False, default=str, indent=2), encoding="utf-8"
    )
    log(json.dumps(report, ensure_ascii=False, default=str, indent=2))
    return 0


def main() -> int:
    if not fs.wait_server(f"{ADMIN}/", 20):
        raise RuntimeError("admin 8080 not up")
    if not fs.wait_server(f"{WEB}/", 10):
        raise RuntimeError("web 8881 not up")
    token = fs.admin_login()
    log("admin login ok")

    existing = find_season()
    if existing:
        sid = int(as_id(existing["id"]))
        did = e2e.find_division(DIV_NAME)
        po_n = 0
        if did:
            po_n = int((fs.db_query(
                "SELECT COUNT(*) c FROM basket_match_series s "
                "JOIN basket_division_stage st ON st.id=s.stage_id "
                "WHERE st.division_id=%s AND s.is_delete=0 AND s.winner_event_team_id IS NOT NULL",
                (did,),
            ) or [{"c": 0}])[0]["c"])
        if did and po_n >= 6:
            log(f"existing demo season {sid} already complete; dump only")
            st = e2e.stages(token, did)
            po = next(s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
            return emit_report(did, sid, int(po["id"]))

    defs = team_defs()
    teams = fs.seed_org_pool("BODEMO", defs, offset=8000)
    e2e.clear_locks([t["captain_email"] for t in teams] + [e for t in teams for e in t["emails"]])
    log(f"org pool {len(teams)} teams")

    season_id = create_season(token)
    did = create_division(token, season_id)
    log(f"season {season_id} division {did}")
    e2e.apply_and_lock(
        token, did, "LEAGUE_STANDARD_V1", "TEAM", "FIVE_V_FIVE",
        playoffSlots=8, knockoutBestOf=3, semifinalBestOf=5, finalBestOf=7,
    )
    align_windows(token, season_id, did)
    e2e.create_home_teams(token, did, teams)
    tokens = e2e.register_lock(token, did, teams)
    widen_after_lock(token, did)
    et_n = int((fs.db_query(
        "SELECT COUNT(*) c FROM basket_event_team WHERE division_id=%s AND is_delete=0", (did,)
    ) or [{"c": 0}])[0]["c"])
    ep_n = int((fs.db_query(
        "SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        "WHERE et.division_id=%s AND ep.is_delete=0 AND et.is_delete=0", (did,)
    ) or [{"c": 0}])[0]["c"])
    if et_n != 8 or ep_n != 40:
        raise RuntimeError(f"snapshot unexpected eventTeam={et_n} eventPlayer={ep_n}")
    log(f"snapshot eventTeam={et_n} eventPlayer={ep_n}")

    st = e2e.stages(token, did)
    rr = next(s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN")
    po = next(s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
    l16.generate_and_publish(token, int(rr["id"]), date(2026, 8, 26), date(2026, 8, 27), audit_auto=False)
    mt = e2e.member_tokens(teams, tokens)
    web_by = e2e.event_web_map(did, mt)
    et_map = event_team_map(did)
    played = play_regular(token, web_by, et_map, did, int(rr["id"]), teams)
    log(f"regular played {played}")
    l16.print_standings(token, int(rr["id"]))
    fs.confirm_stage_qualification(token, str(rr["id"]))
    l16.generate_and_publish(token, int(po["id"]), date(2026, 9, 10), date(2026, 9, 30), prefer_week_tail=False)

    rng = random.Random(20260911)
    name_to_id = {meta["name"]: eid for eid, meta in et_map.items()}

    def play_open_series() -> int:
        progressed = 0
        e2e.sync_series_child_teams()
        for row in series_rows(int(po["id"])):
            if row.get("winner_event_team_id") or (row.get("series_status") or "") in ("FINISHED", "COMPLETED"):
                continue
            if not row.get("h") or not row.get("a"):
                continue
            hname, aname = row.get("hn") or "", row.get("an") or ""
            hid, aid = as_id(row["h"]), as_id(row["a"])
            names = {hname, aname}
            rnd = int(row.get("round_no") or 0)
            best = int(row.get("best_of") or 0)
            sid = int(row["id"])
            if best == 3 and {"BO-T01", "BO-T08"} <= names:
                play_series_scripted(token, web_by, et_map, teams, sid, name_to_id["BO-T01"],
                                     [(78, 72), (69, 75), (84, 80)], rng)
            elif best == 5 and "BO-T01" in names:
                play_series_scripted(token, web_by, et_map, teams, sid, name_to_id["BO-T01"],
                                     [(82, 75), (71, 79), (88, 83), (74, 81), (91, 86)], rng)
            else:
                winner = name_to_id.get("BO-T01") if "BO-T01" in names else (
                    hid if team_no(hname) < team_no(aname) else aid
                )
                play_series_sweep(token, web_by, et_map, teams, sid, winner, rng)
            progressed += 1
            log(f"  played series {sid} R{rnd} {hname} vs {aname} BO{best}")
        return progressed

    for wave in range(8):
        n = play_open_series()
        log(f"playoff wave {wave + 1} progressed={n}")
        if n == 0:
            break

    fin = fs.finish_division(token, str(did), MARKER)
    log(f"division finish {fin.get('code')} {fin.get('msg')}")
    req(f"{ADMIN}/basket/v4/season/{season_id}/publish", "PUT", token=token)
    return emit_report(did, season_id, int(po["id"]))


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        traceback.print_exc()
        raise
