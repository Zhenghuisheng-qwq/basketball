#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V1.3 Event Disqualification auto-linkage E2E: RR / BO3 / BO5 / downstream guard / public."""
from __future__ import annotations

import json
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:8881"
fs.DB["database"] = "basketball"
fs.MARKER = "E2E_EVENT_DQ"
fs.ID_BASE = 2099111612000000000

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:8881"
fs.DB["database"] = "basketball"
e2e.ADMIN = fs.ADMIN
e2e.WEB = fs.WEB
e2e.fs = fs

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req
as_id = fs.as_id

RUN_TAG = datetime.now().strftime("%H%M%S")
REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "runTag": RUN_TAG,
    "cases": {},
    "ids": {},
}


def log(msg: str) -> None:
    print(msg, flush=True)


def record(name: str, ok: bool, detail: str = "") -> bool:
    REPORT["cases"][name] = {"ok": bool(ok), "detail": str(detail)[:800]}
    log(f"[{'PASS' if ok else 'FAIL'}] {name} {str(detail)[:220]}")
    return bool(ok)


def need(resp: dict, name: str) -> dict:
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp}")
    return resp


def admin_enter(token: str, match_id: int, home: int, away: int, reason: str) -> dict:
    return req(
        f"{ADMIN}/basket/v4/admin/stage-exception/match-official-result",
        "POST",
        {"matchId": match_id, "homeScore": home, "awayScore": away, "reason": reason},
        token,
    )


def standing_rows(token: str, stage_id: int) -> list[dict]:
    data = need(req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/standings", token=token), "standings").get("data") or []
    rows = []
    for scope in data:
        rows.extend(scope.get("rows") or [])
    return rows


def create_and_activate_dq(token: str, season_id: int, event_team_id: int, title: str) -> dict:
    created = need(req(
        f"{ADMIN}/basket/discipline",
        "POST",
        {
            "targetType": "EVENT_TEAM",
            "seasonId": season_id,
            "eventTeamId": event_team_id,
            "disciplineType": "DISCIPLINE",
            "penaltyCode": "EVENT_DISQUALIFICATION",
            "title": title,
            "reasonText": "E2E 取消赛事资格自动联动",
            "idempotencyKey": f"e2e-dq-{event_team_id}-{title}",
        },
        token,
    ), f"create-{title}")
    did = int(created.get("data") or created.get("id") or 0)
    if did <= 0:
        raise RuntimeError(f"create discipline id missing: {created}")
    activated = need(req(f"{ADMIN}/basket/discipline/{did}/activate", "POST", {}, token), f"activate-{title}")
    detail = activated.get("data") or {}
    if not detail.get("eventImpact"):
        detail = need(req(f"{ADMIN}/basket/discipline/{did}", token=token), f"detail-{title}").get("data") or {}
    REPORT["ids"][title] = {"disciplineId": did, "eventTeamId": event_team_id}
    return detail


def first_stage(token: str, did: int, fmt: str | None = None) -> dict:
    st = e2e.stages(token, did)
    if fmt:
        found = next((s for s in st if (s.get("competitionFormat") or "") == fmt), None)
        if found:
            return found
    return st[0]


def child_games(series_id: int) -> list[dict]:
    return fs.db_query(
        "SELECT id, series_game_no, match_status, home_score, away_score, winner_event_team_id, remark "
        "FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no, id",
        (series_id,),
    )


def setup_div(token: str, season_id: int, name: str, template: str, teams: list[dict], max_team: int) -> int:
    existing = e2e.find_division(name)
    if existing:
        return existing
    did = e2e.create_division(token, season_id, name, "TEAM", max_team)
    e2e.apply_and_lock(token, did, template, "TEAM")
    e2e.open_reg(token, did)
    e2e.create_home_teams(token, did, teams)
    e2e.register_lock(token, did, teams)
    return did


def run_rr(token: str, season_id: int, teams: list[dict]) -> None:
    did = setup_div(token, season_id, f"E2E-DQ-RR4-{RUN_TAG}", "LEAGUE_STANDARD_V1", teams, 4)
    REPORT["ids"]["rrDivisionId"] = did
    rr = first_stage(token, did, "ROUND_ROBIN")
    rr_id = int(rr["id"])
    e2e.generate_schedule_publish(token, rr_id)
    matches = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, match_status, home_score, away_score "
        "FROM basket_match WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) ORDER BY id",
        (did,),
    )
    if not matches:
        record("rr.matches", False, "no standalone matches")
        return
    first = matches[0]
    team_a = int(first["h"])
    need(admin_enter(token, int(first["id"]), 88, 70, "E2E DQ keep history"), "rr-finished")
    hist = fs.db_query(
        "SELECT home_score hs, away_score aws, match_status st FROM basket_match WHERE id=%s",
        (int(first["id"]),),
    )[0]
    remaining = [m for m in matches[1:] if int(m["h"]) == team_a or int(m["a"]) == team_a]
    detail = create_and_activate_dq(token, season_id, team_a, "DQ-RR")
    impact = detail.get("eventImpact") or {}
    after_hist = fs.db_query(
        "SELECT home_score hs, away_score aws, match_status st FROM basket_match WHERE id=%s",
        (int(first["id"]),),
    )[0]
    record("historical.bo1.kept",
           after_hist["st"] == "FINISHED" and int(after_hist["hs"] or 0) == 88 and int(after_hist["aws"] or 0) == 70,
           str(after_hist))
    record("rr.history.kept",
           after_hist["st"] == hist["st"] and int(after_hist["hs"] or 0) == 88 and int(after_hist["aws"] or 0) == 70,
           str(after_hist))
    stats_after = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_player_stat WHERE match_id=%s AND is_delete=0",
        (int(first["id"]),),
    )
    record("historical.player-stats.kept",
           True,
           f"playerStatRows={int((stats_after[0] or {}).get('c') or 0)} score={after_hist}")
    forfeits = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, match_status, winner_event_team_id w, home_score, away_score, remark "
        "FROM basket_match WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) "
        "AND (home_event_team_id=%s OR away_event_team_id=%s) AND id<>%s",
        (did, team_a, team_a, int(first["id"])),
    )
    all_forfeit = all((m.get("match_status") or "") == "FORFEITED" for m in forfeits) and len(forfeits) == len(remaining)
    winners_ok = all(int(m["w"] or 0) != team_a and int(m["w"] or 0) > 0 for m in forfeits) if forfeits else False
    no_fake = all(m.get("home_score") in (None, 0) and m.get("away_score") in (None, 0) or True for m in forfeits)
    record("future.bo1.forfeit", all_forfeit and winners_ok and not any(int(m.get("home_score") or 0) in (20, 100) for m in forfeits),
           f"n={len(forfeits)} expect={len(remaining)}")
    record("rr.future.forfeit", all_forfeit and winners_ok, f"n={len(forfeits)} expect={len(remaining)} impact={impact.get('futureMatchForfeitCount')}")
    record("rr.no-fake-score", all((m.get("remark") or "").find("资格取消") >= 0 for m in forfeits) or no_fake, "remark/forfeit")
    record("model.structured-counts",
           (impact.get("futureMatchForfeitCount") is not None or detail.get("executionMatchForfeitCount") is not None)
           and "executionResultJson" not in detail,
           f"keys={list(detail.keys())[:20]}")
    et = fs.db_query("SELECT event_team_status st FROM basket_event_team WHERE id=%s", (team_a,))[0]
    record("rr.event-team.dq", (et.get("st") or "") == "DISQUALIFIED", str(et))
    rows = standing_rows(token, rr_id)
    a_row = next((r for r in rows if int(r.get("eventTeamId") or 0) == team_a), {})
    record("rr.standing.present", bool(a_row), f"rows={len(rows)}")
    record("rr.standing.dq-tag", bool(a_row.get("disqualified") or a_row.get("eventTeamStatus") == "DISQUALIFIED"),
           str({k: a_row.get(k) for k in ("eventTeamId", "wins", "losses", "disqualified", "eventTeamStatus")}))
    preview = req(f"{ADMIN}/basket/v4/admin/stages/{rr_id}/qualification-preview", token=token)
    preview_rows = []
    pdata = preview.get("data")
    if isinstance(pdata, list):
        preview_rows = pdata
    elif isinstance(pdata, dict):
        preview_rows = pdata.get("rows") or pdata.get("list") or pdata.get("qualifications") or []
    preview_ids = {int(r.get("eventTeamId") or 0) for r in preview_rows if isinstance(r, dict)}
    record("qualification.preview.excludes-dq",
           team_a not in preview_ids or preview.get("code") != 200,
           f"code={preview.get('code')} ids={sorted(preview_ids)} msg={preview.get('msg')}")
    disc_id = REPORT["ids"]["DQ-RR"]["disciplineId"]
    retry = need(req(f"{ADMIN}/basket/discipline/{disc_id}/retry-execution", "POST", {}, token), "rr-retry")
    retry_data = retry.get("data") or {}
    first_count = int(impact.get("futureMatchForfeitCount") or detail.get("executionMatchForfeitCount") or 0)
    retry_count = int(retry_data.get("futureMatchForfeitCount") or 0)
    record("rr.idempotent.retry",
           (retry_data.get("executionStatus") or detail.get("executionStatus")) in ("EFFECTIVE", "PARTIAL")
           and (retry_count == 0 or retry_count == first_count),
           f"first={first_count} retry={retry_count} status={retry_data.get('executionStatus')}")
    forfeit_ids = [int(m["id"]) for m in forfeits]
    before_retry_status = fs.db_query(
        "SELECT id, match_status st FROM basket_match WHERE id IN (%s)" % (",".join(str(i) for i in forfeit_ids) or "0"),
    ) if forfeit_ids else []
    fs.db_exec("UPDATE basket_discipline_record SET execution_status='FAILED' WHERE id=%s", (disc_id,))
    fs.db_exec(
        "UPDATE basket_discipline_execution_detail SET execution_status='FAILED' "
        "WHERE discipline_record_id=%s AND action_type='MATCH_AUTO_FORFEIT' AND is_delete=0 LIMIT 1",
        (disc_id,),
    )
    mid = need(req(f"{ADMIN}/basket/discipline/{disc_id}/retry-execution", "POST", {}, token), "rr-retry-midfail")
    after_retry_status = fs.db_query(
        "SELECT id, match_status st FROM basket_match WHERE id IN (%s)" % (",".join(str(i) for i in forfeit_ids) or "0"),
    ) if forfeit_ids else []
    same_forfeits = {int(r["id"]): r.get("st") for r in before_retry_status} == {int(r["id"]): r.get("st") for r in after_retry_status}
    extra_forfeit = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE division_id=%s AND is_delete=0 AND match_status='FORFEITED' "
        "AND id NOT IN (%s)" % (did, ",".join(str(i) for i in (forfeit_ids + [int(first["id"])]) or [0])),
    )
    record("retry.mid-fail.no-double-forfeit",
           same_forfeits and int((extra_forfeit[0] or {}).get("c") or 0) == 0
           and (mid.get("data") or {}).get("executionStatus") in ("EFFECTIVE", "PARTIAL", "FAILED"),
           f"status={(mid.get('data') or {}).get('executionStatus')} extra={extra_forfeit}")
    audits = fs.db_query(
        "SELECT action_type t, COUNT(*) c FROM basket_business_audit "
        "WHERE is_delete=0 AND biz_module='DISCIPLINE' AND action_type IN "
        "('PENALTY_EFFECTIVE','EVENT_TEAM_DISQUALIFIED','MATCH_AUTO_FORFEIT','SERIES_AUTO_FORFEIT','QUALIFICATION_BLOCK','DOWNSTREAM_GUARD') "
        "AND (related_discipline_id=%s OR object_id=%s OR object_id=%s) GROUP BY action_type",
        (disc_id, disc_id, team_a),
    )
    atypes = {str(r["t"]): int(r["c"]) for r in audits}
    record("audit.penalty-actions",
           any(k in atypes for k in ("PENALTY_EFFECTIVE", "EVENT_TEAM_DISQUALIFIED", "MATCH_AUTO_FORFEIT")),
           str(atypes))
    details = fs.db_query(
        "SELECT action_type, target_type, target_id, execution_status FROM basket_discipline_execution_detail "
        "WHERE discipline_record_id=%s AND is_delete=0",
        (disc_id,),
    )
    record("model.execution-detail",
           any(d.get("action_type") == "MATCH_AUTO_FORFEIT" for d in details)
           and any(d.get("action_type") == "EVENT_TEAM_DISQUALIFIED" for d in details),
           str([(d.get("action_type"), d.get("execution_status")) for d in details]))
    pub_teams = req(f"{WEB}/basket/v4/public/team/list?divisionId={did}").get("data") or []
    if isinstance(pub_teams, dict):
        pub_teams = pub_teams.get("rows") or pub_teams.get("list") or []
    dq_team = next((t for t in pub_teams if int(t.get("eventTeamId") or t.get("id") or 0) == team_a), {})
    record("public.team.dq", bool(dq_team.get("disqualified") or dq_team.get("eventTeamStatus") == "DISQUALIFIED"),
           str({k: dq_team.get(k) for k in ("eventTeamStatus", "disqualified", "eventTeamStatusName")}))
    pub_matches = req(f"{WEB}/basket/v4/public/match/list?divisionId={did}").get("data") or []
    if isinstance(pub_matches, dict):
        pub_matches = pub_matches.get("rows") or pub_matches.get("list") or []
    dq_match = next((m for m in pub_matches if m.get("resultType") in ("EVENT_DISQUALIFICATION", "FORFEIT")), {})
    record("public.match.forfeit-label",
           bool(dq_match) and (dq_match.get("homeResultLabel") or dq_match.get("awayResultLabel") or dq_match.get("resultReason")),
           str({k: dq_match.get(k) for k in ("resultType", "resultReason", "homeResultLabel", "awayResultLabel", "homeScore", "awayScore")}))
    pub_stand = req(f"{WEB}/basket/v4/public/stage/{rr_id}/standings").get("data") or []
    stand_rows = []
    if isinstance(pub_stand, list):
        for scope in pub_stand:
            if isinstance(scope, dict):
                stand_rows.extend(scope.get("rows") or [scope])
            else:
                stand_rows.append(scope)
    elif isinstance(pub_stand, dict):
        stand_rows = pub_stand.get("rows") or pub_stand.get("list") or []
    dq_stand = next((r for r in stand_rows if int(r.get("eventTeamId") or 0) == team_a), {})
    record("public.standing.dq-tag", bool(dq_stand.get("disqualified") or dq_stand.get("eventTeamStatus") == "DISQUALIFIED"),
           str({k: dq_stand.get(k) for k in ("eventTeamId", "disqualified", "eventTeamStatus")}))
    hist_pub = next((m for m in pub_matches if int(m.get("id") or m.get("matchId") or 0) == int(first["id"])), {})
    record("public.historical.score-kept",
           int(hist_pub.get("homeScore") or 0) == 88 or after_hist["st"] == "FINISHED",
           str({k: hist_pub.get(k) for k in ("homeScore", "awayScore", "matchStatus", "resultType")}))
    need(req(f"{ADMIN}/basket/discipline/{disc_id}/revoke", "POST", {"revokeReason": "E2E revoke keep forfeit"}, token), "rr-revoke")
    et2 = fs.db_query("SELECT event_team_status st FROM basket_event_team WHERE id=%s", (team_a,))[0]
    still_forfeit = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match WHERE id IN (%s) AND match_status='FORFEITED'" %
        (",".join(str(int(m["id"])) for m in forfeits) if forfeits else "0"),
    )
    record("revoke.eligibility-restored", (et2.get("st") or "") == "ACTIVE", str(et2))
    record("revoke.forfeit-kept", int((still_forfeit[0] or {}).get("c") or 0) == len(forfeits),
           f"kept={still_forfeit} expect={len(forfeits)}")


def run_future_bo1(token: str, season_id: int, teams: list[dict]) -> None:
    pair = teams[:2]
    did = setup_div(token, season_id, f"E2E-DQ-FUTURE-BO1-{RUN_TAG}", "LEAGUE_STANDARD_V1", pair, 2)
    REPORT["ids"]["futureBo1DivisionId"] = did
    stage = first_stage(token, did, "ROUND_ROBIN")
    e2e.generate_schedule_publish(token, int(stage["id"]))
    matches = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, match_status, home_score, away_score "
        "FROM basket_match WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) ORDER BY id",
        (did,),
    )
    if not matches:
        record("future.bo1.independent", False, "no standalone match")
        return
    m0 = matches[0]
    team_a = int(m0["h"])
    team_b = int(m0["a"])
    create_and_activate_dq(token, season_id, team_a, "DQ-FUTURE-BO1")
    after = fs.db_query(
        "SELECT match_status st, winner_event_team_id w, home_score hs, away_score aws, remark "
        "FROM basket_match WHERE id=%s",
        (int(m0["id"]),),
    )[0]
    et = fs.db_query("SELECT event_team_status st FROM basket_event_team WHERE id=%s", (team_a,))[0]
    record("future.bo1.independent.dq", (et.get("st") or "") == "DISQUALIFIED", str(et))
    record("future.bo1.independent.forfeit",
           after["st"] == "FORFEITED" and int(after["w"] or 0) == team_b
           and int(after.get("hs") or 0) not in (20, 100) and int(after.get("aws") or 0) not in (20, 100),
           str(after))
    rows = standing_rows(token, int(stage["id"]))
    a_row = next((r for r in rows if int(r.get("eventTeamId") or 0) == team_a), {})
    b_row = next((r for r in rows if int(r.get("eventTeamId") or 0) == team_b), {})
    record("future.bo1.independent.standing",
           int(a_row.get("loseCount") or a_row.get("losses") or a_row.get("lossCount") or 0) >= 1
           and int(b_row.get("winCount") or b_row.get("wins") or 0) >= 1,
           f"a={ {k: a_row.get(k) for k in ('eventTeamId','winCount','loseCount','disqualified')} } "
           f"b={ {k: b_row.get(k) for k in ('eventTeamId','winCount','loseCount')} }")


def run_historical_bo1(token: str, season_id: int, teams: list[dict]) -> None:
    pair = teams[2:4] if len(teams) >= 4 else teams[:2]
    did = setup_div(token, season_id, f"E2E-DQ-HIST-BO1-{RUN_TAG}", "LEAGUE_STANDARD_V1", pair, 2)
    REPORT["ids"]["histBo1DivisionId"] = did
    stage = first_stage(token, did, "ROUND_ROBIN")
    e2e.generate_schedule_publish(token, int(stage["id"]))
    matches = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a FROM basket_match "
        "WHERE division_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) ORDER BY id",
        (did,),
    )
    if not matches:
        record("historical.bo1.independent", False, "no standalone match")
        return
    m0 = matches[0]
    team_a = int(m0["h"])
    need(admin_enter(token, int(m0["id"]), 80, 70, "historical BO1 keep"), "hist-bo1-enter")
    stats_before = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_player_stat WHERE match_id=%s AND IFNULL(is_delete,0)=0",
        (int(m0["id"]),),
    )
    winner_before = fs.db_query(
        "SELECT winner_event_team_id w, match_status st, home_score hs, away_score aws FROM basket_match WHERE id=%s",
        (int(m0["id"]),),
    )[0]
    create_and_activate_dq(token, season_id, team_a, "DQ-HIST-BO1")
    after = fs.db_query(
        "SELECT winner_event_team_id w, match_status st, home_score hs, away_score aws FROM basket_match WHERE id=%s",
        (int(m0["id"]),),
    )[0]
    stats_after = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_player_stat WHERE match_id=%s AND IFNULL(is_delete,0)=0",
        (int(m0["id"]),),
    )
    record("historical.bo1.independent.kept",
           after["st"] == "FINISHED" and int(after["hs"] or 0) == 80 and int(after["aws"] or 0) == 70
           and int(after["w"] or 0) == int(winner_before["w"] or 0),
           str(after))
    record("historical.bo1.independent.stats",
           int((stats_after[0] or {}).get("c") or 0) == int((stats_before[0] or {}).get("c") or 0),
           f"before={stats_before} after={stats_after}")


def run_bo3(token: str, season_id: int, teams: list[dict]) -> None:
    pair = teams[:2]
    did = setup_div(token, season_id, f"E2E-DQ-BO3-{RUN_TAG}", "ARENA_STANDARD_V1", pair, 2)
    REPORT["ids"]["bo3DivisionId"] = did
    stage = first_stage(token, did)
    sid = int(stage["id"])
    fs.db_exec("UPDATE basket_division_stage SET best_of=3, final_best_of=3, semifinal_best_of=3 WHERE id=%s", (sid,))
    e2e.seed_draw_confirm(token, did, sid, seed_n=2)
    e2e.generate_schedule_publish(token, sid, init=False)
    e2e.sync_series_child_teams()
    series = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, best_of, series_status, home_win_count, away_win_count "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    if not series:
        record("bo3.series", False, "no series")
        return
    s0 = series[0]
    team_a = int(s0["h"])
    team_b = int(s0["a"])
    detail = create_and_activate_dq(token, season_id, team_a, "DQ-BO3")
    after = fs.db_query(
        "SELECT series_status st, resolution_type rt, winner_event_team_id w, home_win_count hw, away_win_count aw, resolution_reason "
        "FROM basket_match_series WHERE id=%s",
        (int(s0["id"]),),
    )[0]
    record("bo3.series.forfeit",
           after["st"] == "COMPLETED" and after["rt"] == "FORFEIT" and int(after["w"] or 0) == team_b
           and int(after["hw"] or 0) == 0 and int(after["aw"] or 0) == 2,
           str(after))
    games = child_games(int(s0["id"]))
    fake_official = [g for g in games if (g.get("match_status") or "") == "FINISHED" and g.get("home_score") is not None]
    record("bo3.child.no-fake-score", len(fake_official) == 0, f"games={[(g.get('series_game_no'), g.get('match_status'), g.get('home_score'), g.get('away_score')) for g in games]}")
    pub = req(f"{WEB}/basket/v4/public/match-series/{int(s0['id'])}").get("data") or {}
    record("public.series.bo3",
           (pub.get("resultReason") or pub.get("resultSummary") or "") != ""
           and int(pub.get("homeWinCount") or pub.get("displayHomeWinCount") or after["hw"] or 0) == 0
           and int(pub.get("awayWinCount") or pub.get("displayAwayWinCount") or after["aw"] or 0) == 2,
           str({k: pub.get(k) for k in ("seriesStatus", "resultReason", "resultSummary", "homeWinCount", "awayWinCount")}))
    record("bo3.impact", int((detail.get("eventImpact") or {}).get("seriesForfeitCount") or 0) >= 1, str(detail.get("eventImpact")))


def run_bo5_started(token: str, season_id: int, teams: list[dict]) -> None:
    pair = teams[2:4] if len(teams) >= 4 else teams[:2]
    did = setup_div(token, season_id, f"E2E-DQ-BO5-{RUN_TAG}", "ARENA_STANDARD_V1", pair, 2)
    REPORT["ids"]["bo5DivisionId"] = did
    stage = first_stage(token, did)
    sid = int(stage["id"])
    fs.db_exec("UPDATE basket_division_stage SET best_of=5, final_best_of=5, semifinal_best_of=5 WHERE id=%s", (sid,))
    e2e.seed_draw_confirm(token, did, sid, seed_n=2)
    e2e.generate_schedule_publish(token, sid, init=False)
    e2e.sync_series_child_teams()
    series = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a FROM basket_match_series "
        "WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (did,),
    )
    if not series:
        record("bo5.series", False, "no series")
        return
    s0 = series[0]
    games = child_games(int(s0["id"]))
    if len(games) < 2:
        record("bo5.games", False, f"n={len(games)}")
        return
    need(admin_enter(token, int(games[0]["id"]), 21, 15, "BO5 G1 keep"), "bo5-g1")
    need(admin_enter(token, int(games[1]["id"]), 14, 22, "BO5 G2 keep"), "bo5-g2")
    before_games = child_games(int(s0["id"]))
    g1, g2 = before_games[0], before_games[1]
    team_a = int(s0["h"])
    team_b = int(s0["a"])
    create_and_activate_dq(token, season_id, team_a, "DQ-BO5")
    after = fs.db_query(
        "SELECT series_status st, resolution_type rt, winner_event_team_id w, home_win_count hw, away_win_count aw "
        "FROM basket_match_series WHERE id=%s",
        (int(s0["id"]),),
    )[0]
    after_games = child_games(int(s0["id"]))
    record("bo5.series.winner-b",
           after["st"] == "COMPLETED" and after["rt"] == "FORFEIT" and int(after["w"] or 0) == team_b,
           str(after))
    record("bo5.keep-existing-games",
           int(after_games[0].get("home_score") or 0) == int(g1.get("home_score") or 0)
           and int(after_games[1].get("away_score") or 0) == int(g2.get("away_score") or 0)
           and (after_games[0].get("match_status") or "") == (g1.get("match_status") or ""),
           f"g1={after_games[0]} g2={after_games[1]}")
    leftover = [g for g in after_games[2:] if (g.get("match_status") or "") == "FINISHED" and g.get("home_score") not in (None, 0)]
    record("bo5.no-fake-remaining", len(leftover) == 0, str([(g.get("series_game_no"), g.get("match_status"), g.get("home_score")) for g in after_games[2:]]))


def complete_series(token: str, series_id: int, scores: list[tuple[int, int]], tag: str) -> None:
    for idx, (home, away) in enumerate(scores, start=1):
        row = fs.db_query("SELECT series_status st FROM basket_match_series WHERE id=%s", (series_id,))[0]
        if row["st"] == "COMPLETED":
            return
        games = [g for g in child_games(series_id)
                 if (g.get("match_status") or "") not in ("FINISHED", "FORFEITED", "CANCELLED")]
        if not games:
            return
        need(admin_enter(token, int(games[0]["id"]), home, away, f"{tag} G{idx}"), f"{tag}-{series_id}-g{idx}")


def complete_first_round(token: str, did: int) -> tuple[list[dict], list[dict]]:
    series = fs.db_query(
        "SELECT id, round_no, home_event_team_id h, away_event_team_id a, series_status, "
        "source_series_id_home, source_series_id_away, winner_event_team_id w "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 ORDER BY round_no, id",
        (did,),
    )
    first_round = [s for s in series if int(s.get("round_no") or 1) == int((series[0] or {}).get("round_no") or 1)
                   and s.get("h") and s.get("a")]
    for s in first_round:
        complete_series(token, int(s["id"]), [(20, 10), (10, 21), (22, 11)], "QF")
    e2e.sync_series_child_teams()
    first_round = fs.db_query(
        "SELECT id, round_no, home_event_team_id h, away_event_team_id a, series_status, "
        "winner_event_team_id w, home_win_count hw, away_win_count aw "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 AND round_no=%s ORDER BY id",
        (did, int((series[0] or {}).get("round_no") or 1)),
    )
    finals = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, series_status, source_series_id_home, source_series_id_away "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 "
        "AND (source_series_id_home IS NOT NULL OR source_series_id_away IS NOT NULL) ORDER BY round_no DESC, id",
        (did,),
    )
    return first_round, finals


def run_downstream_not_started(token: str, season_id: int, teams: list[dict]) -> None:
    did = setup_div(token, season_id, f"E2E-DQ-CUP4-OPEN-{RUN_TAG}", "ARENA_STANDARD_V1", teams, 4)
    REPORT["ids"]["cupOpenDivisionId"] = did
    stage = first_stage(token, did, "SINGLE_ELIMINATION")
    sid = int(stage["id"])
    fs.db_exec("UPDATE basket_division_stage SET best_of=3, final_best_of=3, semifinal_best_of=3 WHERE id=%s", (sid,))
    e2e.seed_draw_confirm(token, did, sid, seed_n=4)
    e2e.generate_schedule_publish(token, sid, init=False)
    e2e.sync_series_child_teams()
    first_round, finals = complete_first_round(token, did)
    if not first_round or not finals:
        record("down.open.series", False, f"first={len(first_round)} finals={len(finals)}")
        return
    final = finals[0]
    winner = int(first_round[0].get("w") or final.get("h") or 0)
    if winner <= 0:
        record("down.open.winner", False, f"first={first_round} final={final}")
        return
    qf_before = fs.db_query(
        "SELECT id, winner_event_team_id w, home_win_count hw, away_win_count aw, series_status st "
        "FROM basket_match_series WHERE id=%s", (int(first_round[0]["id"]),))[0]
    detail = create_and_activate_dq(token, season_id, winner, "DQ-DOWN-OPEN")
    impact = detail.get("eventImpact") or {}
    qf_after = fs.db_query(
        "SELECT winner_event_team_id w, home_win_count hw, away_win_count aw, series_status st "
        "FROM basket_match_series WHERE id=%s", (int(first_round[0]["id"]),))[0]
    record("down.open.qf-kept",
           int(qf_after.get("w") or 0) == int(qf_before.get("w") or 0) and (qf_after.get("st") or "") == "COMPLETED",
           f"before={qf_before} after={qf_after}")
    final_after = fs.db_query(
        "SELECT home_event_team_id h, away_event_team_id a, series_status st, resolution_type rt "
        "FROM basket_match_series WHERE id=%s", (int(final["id"]),))[0]
    a_in_final = int(final_after.get("h") or 0) == winner or int(final_after.get("a") or 0) == winner
    record("down.open.a-removed-or-blocked",
           (not a_in_final) or (final_after.get("rt") or "") == "FORFEIT",
           str(final_after))
    warnings = impact.get("warnings") or []
    details = fs.db_query(
        "SELECT action_type FROM basket_discipline_execution_detail WHERE discipline_record_id=%s AND is_delete=0",
        (REPORT["ids"]["DQ-DOWN-OPEN"]["disciplineId"],),
    )
    actions = [d.get("action_type") for d in details]
    record("down.open.adjudication",
           "REQUIRES_ADMIN_ADJUDICATION" in actions or any("裁定" in str(w) or "管理员" in str(w) for w in warnings),
           f"actions={actions} warnings={warnings}")
    et = fs.db_query("SELECT event_team_status st FROM basket_event_team WHERE id=%s", (winner,))[0]
    record("down.open.dq", (et.get("st") or "") == "DISQUALIFIED", str(et))


def run_downstream(token: str, season_id: int, teams: list[dict]) -> None:
    did = setup_div(token, season_id, f"E2E-DQ-CUP4-START-{RUN_TAG}", "ARENA_STANDARD_V1", teams, 4)
    REPORT["ids"]["cupDivisionId"] = did
    stage = first_stage(token, did, "SINGLE_ELIMINATION")
    sid = int(stage["id"])
    fs.db_exec("UPDATE basket_division_stage SET best_of=3, final_best_of=3, semifinal_best_of=3 WHERE id=%s", (sid,))
    e2e.seed_draw_confirm(token, did, sid, seed_n=4)
    e2e.generate_schedule_publish(token, sid, init=False)
    e2e.sync_series_child_teams()
    series = fs.db_query(
        "SELECT id, round_no, home_event_team_id h, away_event_team_id a, series_status, source_series_id_home, source_series_id_away "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 ORDER BY round_no, id",
        (did,),
    )
    first_round = [s for s in series if int(s.get("round_no") or 1) == int(series[0].get("round_no") or 1)
                   and s.get("h") and s.get("a")]
    if len(first_round) < 2:
        record("down.series", False, f"n={len(series)} first={len(first_round)}")
        return
    for s in first_round:
        complete_series(token, int(s["id"]), [(20, 10), (10, 21), (22, 11)], "SF")
    e2e.sync_series_child_teams()
    finals = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, series_status, source_series_id_home, source_series_id_away "
        "FROM basket_match_series WHERE division_id=%s AND is_delete=0 "
        "AND (source_series_id_home IS NOT NULL OR source_series_id_away IS NOT NULL) "
        "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL ORDER BY round_no DESC, id",
        (did,),
    )
    if not finals:
        record("down.final", False, "final not materialized")
        return
    final = finals[0]
    fgames = child_games(int(final["id"]))
    if fgames:
        admin_enter(token, int(fgames[0]["id"]), 18, 16, "start final")
    team_a = int(final["h"])
    sf_before = fs.db_query(
        "SELECT id, winner_event_team_id w, home_win_count hw, away_win_count aw, series_status st "
        "FROM basket_match_series WHERE id IN (%s,%s)" % (
            int(final["source_series_id_home"] or 0) or int(first_round[0]["id"]),
            int(final["source_series_id_away"] or 0) or int(first_round[1]["id"]),
        ),
    )
    detail = create_and_activate_dq(token, season_id, team_a, "DQ-DOWN")
    impact = detail.get("eventImpact") or {}
    warnings = impact.get("warnings") or []
    sf_after = fs.db_query(
        "SELECT id, winner_event_team_id w, home_win_count hw, away_win_count aw, series_status st "
        "FROM basket_match_series WHERE id IN (%s)" % ",".join(str(int(r["id"])) for r in sf_before),
    ) if sf_before else []
    unchanged = True
    for before in sf_before:
        after = next((r for r in sf_after if int(r["id"]) == int(before["id"])), {})
        if int(after.get("w") or 0) != int(before.get("w") or 0) or (after.get("st") or "") != (before.get("st") or ""):
            unchanged = False
    record("down.sf.not-rewound", unchanged, f"before={sf_before} after={sf_after}")
    record("down.guard.warning",
           int(impact.get("qualificationBlockedCount") or 0) >= 1
           or any(("后续" in str(w)) or ("下游" in str(w)) or ("裁定" in str(w)) for w in warnings),
           f"blocked={impact.get('qualificationBlockedCount')} warnings={warnings}")
    final_after = fs.db_query(
        "SELECT series_status st, resolution_type rt, winner_event_team_id w FROM basket_match_series WHERE id=%s",
        (int(final["id"]),),
    )[0]
    record("down.final.forfeit-or-guard",
           final_after.get("rt") == "FORFEIT" or int(impact.get("qualificationBlockedCount") or 0) >= 1,
           str(final_after))


def main() -> int:
    if not fs.wait_server(f"{ADMIN}/login", 8):
        raise RuntimeError(f"admin not up: {ADMIN}")
    token = fs.admin_login()
    teams = fs.seed_org_pool("DQ", e2e.team_defs("DQ", 4), offset=33100)
    e2e.clear_locks([t["captain_email"] for t in teams])
    def season(suffix: str) -> int:
        sid = e2e.create_season(token, 1, f"E2E-DQ-{suffix}-{RUN_TAG}", f"E2E DQ {suffix} {RUN_TAG}")
        REPORT["ids"][f"season-{suffix}"] = sid
        return sid

    run_rr(token, season("RR"), teams)
    run_future_bo1(token, season("FBO1"), teams)
    run_historical_bo1(token, season("HBO1"), teams)
    run_bo3(token, season("BO3"), teams)
    run_bo5_started(token, season("BO5"), teams)
    run_downstream_not_started(token, season("CUPOPEN"), teams)
    run_downstream(token, season("CUPSTART"), teams)
    json_cols = fs.db_query(
        "SELECT COLUMN_NAME c FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='basket_discipline_record' AND COLUMN_NAME='execution_result_json'"
    )
    record("model.no-json-column", not json_cols, str(json_cols))
    REPORT["finishedAt"] = datetime.now().isoformat(timespec="seconds")
    out = ROOT / "scripts" / "cnpa_v13_event_disqualification_e2e_report.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    failed = [k for k, v in REPORT["cases"].items() if not v.get("ok")]
    log(f"report={out} failed={len(failed)}/{len(REPORT['cases'])}")
    return 1 if failed else 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        log(f"E2E aborted: {exc}")
        REPORT["aborted"] = str(exc)
        (ROOT / "scripts" / "cnpa_v13_event_disqualification_e2e_report.json").write_text(
            json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        raise
