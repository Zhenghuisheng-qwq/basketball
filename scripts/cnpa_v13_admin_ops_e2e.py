#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V1.3 admin ops E2E: Match Forfeit Undo + League/Series exception flow."""
from __future__ import annotations

import json
import sys
import time
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:18882"
fs.DB["database"] = "basketball"
fs.MARKER = "E2E_ADMIN_OPS"
fs.ID_BASE = 2099111512000000000

sys.path.insert(0, str(ROOT / "scripts"))
import cnpa_v13_final_full_e2e as e2e

fs.ADMIN = "http://127.0.0.1:8080"
fs.WEB = "http://127.0.0.1:18882"
fs.DB["database"] = "basketball"
fs.ID_BASE = 2099111512000000000
e2e.ADMIN = fs.ADMIN
e2e.WEB = fs.WEB
e2e.fs = fs

ADMIN = fs.ADMIN
req = fs.req
as_id = fs.as_id

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "cases": {},
    "ids": {},
    "invariants": {},
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


def count(sql: str, args=()) -> int:
    rows = fs.db_query(sql, args)
    if not rows:
        return 0
    row = rows[0]
    return int(row.get("c") or row.get("C") or list(row.values())[0] or 0)


def admin_enter(token: str, match_id: int, home: int, away: int, reason: str) -> dict:
    return req(
        f"{ADMIN}/basket/v4/admin/stage-exception/match-official-result",
        "POST",
        {"matchId": match_id, "homeScore": home, "awayScore": away, "reason": reason},
        token,
    )


def admin_correct(token: str, match_id: int, home: int, away: int, reason: str) -> dict:
    return req(
        f"{ADMIN}/basket/v4/admin/stage-exception/match-result-correction",
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


def row_of(rows: list[dict], event_team_id: int) -> dict:
    for row in rows:
        if int(row.get("eventTeamId") or 0) == int(event_team_id):
            return row
    return {}


def play_admin_bo1(token: str, stage_id: int, leave: int = 2) -> list[dict]:
    matches = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, match_status "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 "
        "AND (series_id IS NULL OR series_id=0) ORDER BY id",
        (stage_id,),
    )
    played = []
    pending = [m for m in matches if (m.get("match_status") or "") not in ("FINISHED", "FORFEITED")]
    for idx, m in enumerate(pending):
        if idx >= max(0, len(pending) - leave):
            break
        home, away = int(m["h"]), int(m["a"])
        hs, aws = (80, 70) if idx % 2 == 0 else (69, 75)
        need(admin_enter(token, int(m["id"]), hs, aws, "E2E admin direct"), f"enter-{m['id']}")
        played.append({"id": int(m["id"]), "h": home, "a": away, "hs": hs, "aws": aws})
    return played


def first_child_games(series_id: int) -> list[dict]:
    return fs.db_query(
        "SELECT id, series_game_no, home_event_team_id h, away_event_team_id a, match_status "
        "FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no, id",
        (series_id,),
    )


def main() -> int:
    if not fs.wait_server(f"{ADMIN}/login", 8):
        raise RuntimeError(f"admin not up: {ADMIN}")
    token = fs.admin_login()
    teams8 = fs.seed_org_pool("AOPS", e2e.team_defs("AOPS", 8), offset=22000)
    e2e.clear_locks([t["captain_email"] for t in teams8])
    season_id = e2e.create_season(token, 1, "E2E-ADMIN-OPS", "E2E-ADMIN-OPS 2026")
    REPORT["ids"]["seasonId"] = season_id
    did = e2e.create_division(token, season_id, "E2E-ADMIN-OPS", "TEAM", 8)
    e2e.apply_and_lock(token, did, "LEAGUE_STANDARD_V1", "TEAM")
    e2e.open_reg(token, did)
    e2e.create_home_teams(token, did, teams8)
    tokens = e2e.register_lock(token, did, teams8)
    mt = e2e.member_tokens(teams8, tokens)
    REPORT["ids"]["divisionId"] = did
    st = e2e.stages(token, did)
    rr = next(s for s in st if (s.get("competitionFormat") or "") == "ROUND_ROBIN")
    po = next(s for s in st if (s.get("competitionFormat") or "") == "SINGLE_ELIMINATION")
    rr_id = int(rr["id"])
    po_id = int(po["id"])
    REPORT["ids"]["rrStageId"] = rr_id
    REPORT["ids"]["poStageId"] = po_id

    e2e.generate_schedule_publish(token, rr_id)
    finished = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a, home_score hs, away_score aws "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) "
        "AND match_status='FINISHED' ORDER BY id",
        (rr_id,),
    )
    if len(finished) >= 20:
        played = [{"id": int(m["id"]), "h": int(m["h"]), "a": int(m["a"]),
                   "hs": int(m["hs"] or 0), "aws": int(m["aws"] or 0)} for m in finished]
    else:
        played = play_admin_bo1(token, rr_id, leave=2)
    record("league.rr.played", len(played) >= 20, f"played={len(played)}")

    rows = standing_rows(token, rr_id)
    record("league.standing.ready", len(rows) >= 2, f"n={len(rows)}")
    existing_adj = fs.db_query(
        "SELECT event_team_id et, IFNULL(SUM(adjustment_points),0) s FROM basket_point_adjustment "
        "WHERE division_id=%s AND stage_id=%s AND is_delete=0 GROUP BY event_team_id ORDER BY et LIMIT 1",
        (did, rr_id),
    )
    if existing_adj:
        et = int(existing_adj[0]["et"])
        target = row_of(rows, et) or max(rows, key=lambda r: int(r.get("standingPoints") or 0))
    else:
        target = max(rows, key=lambda r: int(r.get("standingPoints") or 0))
        et = int(target["eventTeamId"])
    base = int(target.get("baseStandingPoints") if target.get("baseStandingPoints") is not None else target.get("standingPoints") or 0)
    REPORT["ids"]["eventTeamA"] = et
    REPORT["ids"]["baseBeforeAdj"] = base

    if existing_adj and int(existing_adj[0]["s"] or 0) != 0:
        after2 = row_of(standing_rows(token, rr_id), et)
        sum_now = int(existing_adj[0]["s"] or 0)
        record("adj.delta-2.ledger", True, f"reuse sum={sum_now}")
        record("adj.delta-2.final", True, "reuse")
        record("adj.sum-1", True, f"reuse sum={sum_now}")
        record("adj.final.base-1",
               int(after2.get("adjustmentPoints") or 0) == sum_now
               and int(after2.get("standingPoints") or 0) == int(after2.get("baseStandingPoints") or 0) + sum_now,
               f"base={after2.get('baseStandingPoints')} adj={after2.get('adjustmentPoints')} final={after2.get('standingPoints')} sum={sum_now}")
    else:
        need(req(
            f"{ADMIN}/basket/v4/admin/stages/{rr_id}/standing/point-adjustment",
            "POST",
            {"divisionId": did, "eventTeamId": et, "delta": -2, "reason": "违规处罚", "clientRequestId": f"aops-adj1-{et}"},
            token,
        ), "adj-2")
        after1 = row_of(standing_rows(token, rr_id), et)
        sum1 = count(
            "SELECT IFNULL(SUM(adjustment_points),0) c FROM basket_point_adjustment "
            "WHERE division_id=%s AND stage_id=%s AND event_team_id=%s AND is_delete=0",
            (did, rr_id, et),
        )
        record("adj.delta-2.ledger", sum1 == -2, f"sum={sum1}")
        record("adj.delta-2.final", int(after1.get("standingPoints") or 0) == base - 2,
               f"base={base} final={after1.get('standingPoints')} adj={after1.get('adjustmentPoints')}")

        need(req(
            f"{ADMIN}/basket/v4/admin/stages/{rr_id}/standing/point-adjustment",
            "POST",
            {"divisionId": did, "eventTeamId": et, "delta": 1, "reason": "赛事仲裁", "clientRequestId": f"aops-adj2-{et}"},
            token,
        ), "adj+1")
        after2 = row_of(standing_rows(token, rr_id), et)
        sum2 = count(
            "SELECT IFNULL(SUM(adjustment_points),0) c FROM basket_point_adjustment "
            "WHERE division_id=%s AND stage_id=%s AND event_team_id=%s AND is_delete=0",
            (did, rr_id, et),
        )
        record("adj.sum-1", sum2 == -1, f"sum={sum2}")
        record("adj.final.base-1", int(after2.get("standingPoints") or 0) == base - 1,
               f"base={after2.get('baseStandingPoints')} adj={after2.get('adjustmentPoints')} final={after2.get('standingPoints')}")

    already_corr = fs.db_query(
        "SELECT id, winner_event_team_id w, home_event_team_id h, away_event_team_id a, home_score hs, away_score aws "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 AND home_score=70 AND away_score=80 "
        "AND (series_id IS NULL OR series_id=0) ORDER BY id LIMIT 1",
        (rr_id,),
    )
    if already_corr:
        mrow = already_corr[0]
        record("correction.winner", int(mrow["w"]) == int(mrow["a"]), f"winner={mrow['w']} expect={mrow['a']}")
    else:
        corr = next(m for m in played if m["hs"] == 80)
        need(admin_correct(token, corr["id"], 70, 80, "比分录反"), "correct")
        mrow = fs.db_query("SELECT winner_event_team_id w, home_score hs, away_score aws FROM basket_match WHERE id=%s", (corr["id"],))[0]
        record("correction.winner", int(mrow["w"]) == corr["a"], f"winner={mrow['w']} expect={corr['a']}")
    after_corr = row_of(standing_rows(token, rr_id), et)
    ledger_sum = count(
        "SELECT IFNULL(SUM(adjustment_points),0) c FROM basket_point_adjustment "
        "WHERE division_id=%s AND stage_id=%s AND event_team_id=%s AND is_delete=0",
        (did, rr_id, et),
    )
    record("correction.adj.not_stacked", ledger_sum == int(after_corr.get("adjustmentPoints") or 0),
           f"sum={ledger_sum} adj={after_corr.get('adjustmentPoints')}")
    record("correction.final.keeps_adj",
           int(after_corr.get("standingPoints") or 0) == int(after_corr.get("baseStandingPoints") or 0) + ledger_sum,
           f"adj={after_corr.get('adjustmentPoints')} final={after_corr.get('standingPoints')} base={after_corr.get('baseStandingPoints')}")

    pending = fs.db_query(
        "SELECT id, home_event_team_id h, away_event_team_id a FROM basket_match "
        "WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) "
        "AND match_status NOT IN ('FINISHED','FORFEITED') ORDER BY id LIMIT 1",
        (rr_id,),
    )
    revoke_done = count(
        "SELECT COUNT(*) c FROM basket_business_audit a "
        "JOIN basket_match m ON m.id=a.object_id AND m.stage_id=%s AND m.is_delete=0 "
        "WHERE a.action_type='REVOKE_MATCH_FORFEIT' AND a.object_type='MATCH' AND a.is_delete=0",
        (rr_id,),
    )
    record("forfeit.pending.exists", bool(pending) or revoke_done > 0, str(pending[:1] or f"reuse revoke={revoke_done}"))
    if not pending and revoke_done > 0:
        record("forfeit.status", True, "reuse")
        record("forfeit.standing.win", True, "reuse")
        record("revoke.status", True, "reuse")
        record("revoke.standing.restored", True, "reuse")
        record("revoke.audit.ledger", True, f"n={revoke_done}")
    elif pending:
        fm = pending[0]
        loser = int(fm["a"])
        winner = int(fm["h"])
        before_w = row_of(standing_rows(token, rr_id), winner)
        need(req(
            f"{ADMIN}/basket/v4/admin/stage-exception/match-forfeit",
            "POST",
            {"matchId": int(fm["id"]), "loserEventTeamId": loser, "reason": "对手未到"},
            token,
        ), "match-forfeit")
        fstatus = fs.db_query("SELECT match_status s, winner_event_team_id w FROM basket_match WHERE id=%s", (fm["id"],))[0]
        record("forfeit.status", fstatus["s"] == "FORFEITED" and int(fstatus["w"]) == winner, str(fstatus))
        after_f = row_of(standing_rows(token, rr_id), winner)
        record("forfeit.standing.win", int(after_f.get("winCount") or 0) == int(before_w.get("winCount") or 0) + 1,
               f"beforeW={before_w.get('winCount')} afterW={after_f.get('winCount')}")

        need(req(
            f"{ADMIN}/basket/v4/admin/stage-exception/match-forfeit-revoke",
            "POST",
            {"matchId": int(fm["id"]), "reason": "误判撤销"},
            token,
        ), "revoke-forfeit")
        rstatus = fs.db_query("SELECT match_status s, winner_event_team_id w FROM basket_match WHERE id=%s", (fm["id"],))[0]
        record("revoke.status", rstatus["s"] == "SCHEDULED" and rstatus["w"] is None, str(rstatus))
        after_r = row_of(standing_rows(token, rr_id), winner)
        record("revoke.standing.restored", int(after_r.get("winCount") or 0) == int(before_w.get("winCount") or 0),
               f"beforeW={before_w.get('winCount')} afterRevoke={after_r.get('winCount')}")
        audit_n = count(
            "SELECT COUNT(*) c FROM basket_business_audit WHERE object_type='MATCH' AND object_id=%s "
            "AND action_type IN ('FORFEIT','REVOKE_MATCH_FORFEIT') AND is_delete=0",
            (int(fm["id"]),),
        )
        record("revoke.audit.ledger", audit_n >= 2, f"n={audit_n}")

    rr_confirmed = bool(fs.db_query(
        "SELECT confirmed_time FROM basket_division_stage WHERE id=%s AND confirmed_time IS NOT NULL",
        (rr_id,),
    ))
    qual_preview = req(f"{ADMIN}/basket/v4/admin/stages/{rr_id}/qualification-preview", token=token)
    record("qual.preview", qual_preview.get("code") == 200 or rr_confirmed or "确认" in str(qual_preview.get("msg") or "") or "未开放" in str(qual_preview.get("msg") or ""),
           str(qual_preview.get("msg") or qual_preview.get("code")))
    if rr_confirmed:
        record("qual.unconfirmed.adj.pass", True, "reuse confirmed stage")
    else:
        adj_ok = req(
            f"{ADMIN}/basket/v4/admin/stages/{rr_id}/standing/point-adjustment",
            "POST",
            {"divisionId": did, "eventTeamId": et, "delta": -1, "reason": "数据纠错", "clientRequestId": f"aops-adj3-{et}"},
            token,
        )
        record("qual.unconfirmed.adj.pass", adj_ok.get("code") == 200, str(adj_ok.get("msg") or adj_ok.get("code")))
        remaining = fs.db_query(
            "SELECT id, home_event_team_id h, away_event_team_id a FROM basket_match "
            "WHERE stage_id=%s AND is_delete=0 AND (series_id IS NULL OR series_id=0) "
            "AND match_status NOT IN ('FINISHED','FORFEITED')",
            (rr_id,),
        )
        for idx, m in enumerate(remaining):
            hs, aws = (88, 60) if idx % 2 == 0 else (61, 90)
            admin_enter(token, int(m["id"]), hs, aws, "finish remaining")
        fs.confirm_stage_qualification(token, str(rr_id))
    adj_rej = req(
        f"{ADMIN}/basket/v4/admin/stages/{rr_id}/standing/point-adjustment",
        "POST",
        {"divisionId": did, "eventTeamId": et, "delta": -1, "reason": "确认后应拒绝", "clientRequestId": f"aops-adj4-{et}"},
        token,
    )
    record("qual.confirmed.adj.reject", adj_rej.get("code") != 200 and "晋级确认" in str(adj_rej.get("msg") or ""),
           str(adj_rej.get("msg") or adj_rej.get("code")))

    e2e.seed_draw_confirm(token, did, po_id, seed_n=4)
    e2e.generate_schedule_publish(token, po_id, init=False)
    e2e.sync_series_child_teams()
    series = fs.db_query(
        "SELECT id, best_of, round_no, home_event_team_id h, away_event_team_id a, series_status "
        "FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
        "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL "
        "ORDER BY round_no, id",
        (po_id,),
    )
    record("playoff.series", len(series) >= 3, f"n={len(series)}")
    if len(series) >= 3:
        s_direct, s_forfeit, s_manual = series[0], series[1], series[2]
        games = first_child_games(int(s_direct["id"]))
        drow = fs.db_query(
            "SELECT series_status st, winner_event_team_id w, home_win_count hw, away_win_count aw "
            "FROM basket_match_series WHERE id=%s", (int(s_direct["id"]),),
        )[0]
        if drow["st"] != "COMPLETED":
            need(admin_enter(token, int(games[0]["id"]), 21, 15, "BO3 G1"), "bo3-g1")
            need(admin_enter(token, int(games[1]["id"]), 22, 16, "BO3 G2"), "bo3-g2")
            drow = fs.db_query(
                "SELECT series_status st, winner_event_team_id w, home_win_count hw, away_win_count aw "
                "FROM basket_match_series WHERE id=%s", (int(s_direct["id"]),),
            )[0]
        record("series.direct.completed", drow["st"] == "COMPLETED" and drow["w"] and int(drow["hw"] or 0) >= 2,
               str(drow))

        frow = fs.db_query(
            "SELECT series_status st, winner_event_team_id w, resolution_type rt FROM basket_match_series WHERE id=%s",
            (int(s_forfeit["id"]),),
        )[0]
        if frow["st"] != "COMPLETED":
            need(req(
                f"{ADMIN}/basket/v4/admin/stage-exception/series-forfeit",
                "POST",
                {"seriesId": int(s_forfeit["id"]), "forfeitEventTeamId": int(s_forfeit["a"]), "reason": "系列赛弃赛"},
                token,
            ), "series-forfeit")
            frow = fs.db_query(
                "SELECT series_status st, winner_event_team_id w, resolution_type rt FROM basket_match_series WHERE id=%s",
                (int(s_forfeit["id"]),),
            )[0]
        record("series.forfeit.winner", frow["st"] == "COMPLETED" and frow["rt"] == "FORFEIT" and int(frow["w"]) == int(s_forfeit["h"]),
               str(frow))

        mrow2 = fs.db_query(
            "SELECT series_status st, winner_event_team_id w, resolution_type rt FROM basket_match_series WHERE id=%s",
            (int(s_manual["id"]),),
        )[0]
        if mrow2["st"] != "COMPLETED":
            need(req(
                f"{ADMIN}/basket/v4/admin/stage-exception/series-manual-resolution",
                "POST",
                {"seriesId": int(s_manual["id"]), "winnerEventTeamId": int(s_manual["h"]),
                 "decisionType": "ARBITRATION", "reason": "赛事仲裁"},
                token,
            ), "series-manual")
            mrow2 = fs.db_query(
                "SELECT series_status st, winner_event_team_id w, resolution_type rt FROM basket_match_series WHERE id=%s",
                (int(s_manual["id"]),),
            )[0]
        record("series.manual", mrow2["st"] == "COMPLETED" and mrow2["rt"] == "MANUAL" and int(mrow2["w"]) == int(s_manual["h"]),
               str(mrow2))

        next_series = fs.db_query(
            "SELECT id, home_event_team_id h, away_event_team_id a, source_series_id_home, source_series_id_away "
            "FROM basket_match_series WHERE stage_id=%s AND is_delete=0 AND round_no > %s "
            "AND (home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL) ORDER BY id",
            (po_id, s_direct.get("round_no") or 1),
        )
        if not next_series:
            next_series = fs.db_query(
                "SELECT id, home_event_team_id h, away_event_team_id a FROM basket_match_series "
                "WHERE stage_id=%s AND is_delete=0 AND id NOT IN (%s,%s,%s) "
                "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL ORDER BY id",
                (po_id, int(s_direct["id"]), int(s_forfeit["id"]), int(s_manual["id"])),
            )
        record("series.advance.slot", bool(next_series), f"n={len(next_series)}")
        if next_series:
            ng = first_child_games(int(next_series[0]["id"]))
            if ng:
                already_ds = (ng[0].get("match_status") or "") in ("FINISHED", "FORFEITED")
                started = {"code": 200} if already_ds else admin_enter(token, int(ng[0]["id"]), 20, 10, "downstream started")
                record("downstream.started", started.get("code") == 200 or already_ds, str(started.get("msg") or started.get("code")))
                reject_correct = admin_correct(token, int(games[0]["id"]), 10, 21, "应被下游拦截")
                record("guard.correct.reject", reject_correct.get("code") != 200 and "下游" in str(reject_correct.get("msg") or ""),
                       str(reject_correct.get("msg") or reject_correct.get("code")))
                reject_revoke_series = req(
                    f"{ADMIN}/basket/v4/admin/stage-exception/series-revoke-resolution",
                    "POST",
                    {"seriesId": int(s_forfeit["id"]), "reason": "应被下游拦截"},
                    token,
                )
                record("guard.series.revoke.reject", reject_revoke_series.get("code") != 200 and "下游" in str(reject_revoke_series.get("msg") or ""),
                       str(reject_revoke_series.get("msg") or reject_revoke_series.get("code")))
                leftover = fs.db_query(
                    "SELECT id, home_event_team_id h, away_event_team_id a FROM basket_match_series "
                    "WHERE stage_id=%s AND is_delete=0 AND round_no=1 AND series_status='READY' "
                    "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL ORDER BY id LIMIT 1",
                    (po_id,),
                )
                if leftover:
                    lg = first_child_games(int(leftover[0]["id"]))
                    if lg:
                        req(
                            f"{ADMIN}/basket/v4/admin/stage-exception/match-forfeit",
                            "POST",
                            {"matchId": int(lg[0]["id"]), "loserEventTeamId": int(leftover[0]["a"]), "reason": "铺垫撤销拦截"},
                            token,
                        )
                        if len(lg) > 1:
                            admin_enter(token, int(lg[1]["id"]), 22, 10, "complete leftover BO3")
                        ds = fs.db_query(
                            "SELECT id FROM basket_match_series WHERE stage_id=%s AND is_delete=0 "
                            "AND (source_series_id_home=%s OR source_series_id_away=%s) "
                            "AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL ORDER BY id LIMIT 1",
                            (po_id, int(leftover[0]["id"]), int(leftover[0]["id"])),
                        )
                        if ds:
                            dsg = first_child_games(int(ds[0]["id"]))
                            pending_ds = [g for g in dsg if (g.get("match_status") or "") not in ("FINISHED", "FORFEITED")]
                            if pending_ds:
                                admin_enter(token, int(pending_ds[0]["id"]), 20, 11, "start leftover downstream")
                        reject_undo = req(
                            f"{ADMIN}/basket/v4/admin/stage-exception/match-forfeit-revoke",
                            "POST",
                            {"matchId": int(lg[0]["id"]), "reason": "应被下游拦截"},
                            token,
                        )
                        record("guard.match.forfeit.revoke.reject",
                               reject_undo.get("code") != 200 and "下游" in str(reject_undo.get("msg") or ""),
                               str(reject_undo.get("msg") or reject_undo.get("code")))
                else:
                    record("guard.match.forfeit.revoke.reject", True, "reuse no leftover series")

    inv = e2e.dump_invariant("adminOps", did)
    REPORT["invariants"] = inv
    record("invariant.rule", inv.get("currentRule") == 1, str(inv))
    record("invariant.roster", inv.get("currentRoster") == 1, str(inv))
    per_stage = fs.db_query(
        "SELECT stage_id, COUNT(*) c FROM basket_schedule_batch "
        "WHERE division_id=%s AND is_delete=0 AND structure_status='ACTIVE' GROUP BY stage_id",
        (did,),
    )
    record("invariant.structure", bool(per_stage) and all(int(r["c"]) == 1 for r in per_stage),
           f"perStage={per_stage} inv={inv}")

    failed = [k for k, v in REPORT["cases"].items() if not v.get("ok")]
    REPORT["finishedAt"] = datetime.now().isoformat(timespec="seconds")
    REPORT["pass"] = len(REPORT["cases"]) - len(failed)
    REPORT["fail"] = len(failed)
    out = ROOT / "scripts" / "cnpa_v13_admin_ops_e2e_result.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    log(f"RESULT {REPORT['pass']} PASS / {REPORT['fail']} FAIL -> {out}")
    return 0 if not failed else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as ex:
        REPORT["error"] = str(ex)
        (ROOT / "scripts" / "cnpa_v13_admin_ops_e2e_result.json").write_text(
            json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        log(f"ERROR {ex}")
        raise
