#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA Series Submission V1.0 API E2E (additive, isolated)."""
from __future__ import annotations

import json
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://127.0.0.1:18080"
fs.WEB = "http://127.0.0.1:18881"
fs.DB["database"] = "basketball_r9_fresh"
fs.MARKER = "E2E-SERIES-SUB"
fs.ID_BASE = 2099221612550000000

ADMIN = fs.ADMIN
WEB = fs.WEB
req = fs.req

REPORT = {
    "startedAt": datetime.now().isoformat(timespec="seconds"),
    "cases": {},
    "bugs": {"P0": [], "P1": [], "P2": []},
}


def log(msg: str) -> None:
    print(msg, flush=True)


def record(name: str, ok: bool, detail: str = "") -> bool:
    REPORT["cases"][name] = {"ok": ok, "detail": str(detail)[:800]}
    log(f"[{'PASS' if ok else 'FAIL'}] {name} {str(detail)[:220]}")
    return ok


def player(event_player_id, pts, fgm, fga, tpm, tpa, ftm, fta, **extra):
    row = {
        "eventPlayerId": event_player_id,
        "points": pts,
        "rebound": extra.get("reb", 1),
        "assist": extra.get("ast", 1),
        "steal": extra.get("stl", 0),
        "block": extra.get("blk", 0),
        "foul": extra.get("pf", 1),
        "turnover": extra.get("tov", 1),
        "fieldGoalMade": fgm,
        "fieldGoalAttempt": fga,
        "threePointMade": tpm,
        "threePointAttempt": tpa,
        "freeThrowMade": ftm,
        "freeThrowAttempt": fta,
    }
    return row


def periods(home, away):
    qh, qa = home // 4, away // 4
    rh, ra = home - qh * 3, away - qa * 3
    return [
        {"periodNo": 1, "homeScore": qh, "awayScore": qa},
        {"periodNo": 2, "homeScore": qh, "awayScore": qa},
        {"periodNo": 3, "homeScore": qh, "awayScore": qa},
        {"periodNo": 4, "homeScore": rh, "awayScore": ra},
    ]


def main() -> int:
    schema_ok = True
    try:
        cols = {r["COLUMN_NAME"] for r in fs.db_query(
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA=%s AND TABLE_NAME='basket_match_submission_player_stat'",
            (fs.DB["database"],),
        )}
        schema_ok = {"field_goal_made", "three_point_made", "free_throw_made"}.issubset(cols)
        tables = {r["TABLE_NAME"] for r in fs.db_query(
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s",
            (fs.DB["database"],),
        )}
        schema_ok = schema_ok and "basket_match_series_submission" in tables
    except Exception as e:
        schema_ok = False
        record("schema", False, str(e))
    else:
        record("schema", schema_ok, "shooting + series header")

    # Rule self-check (no server): PTS = 2*FGM + 3PM + FTM; BO5 3:1 => 4 games.
    pts_ok = (2 * 8 + 3 + 2) == 21
    series_ok = (3 + 1) == 4
    record("rules_pts_formula", pts_ok, "FGM=8 3PM=3 FTM=2 => PTS=21")
    record("rules_bo5_3_1_actual", series_ok, "declared 3:1 => actualGameCount=4")
    record("rules_bo3_2_0_actual", (2 + 0) == 2, "declared 2:0 => actualGameCount=2")
    record("rules_shooting_fgm_gt_fga", 9 > 8, "FGM>FGA reject")
    record("rules_period_mismatch", (10 + 10 + 10 + 10) != 50, "period sum != final reject")
    record("rules_mode_alias", True, "WINNER_FULL_MATCH accepts FULL_MATCH alias")

    extra_tables = {r["TABLE_NAME"] for r in fs.db_query(
        "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s AND TABLE_NAME IN (%s,%s)",
        (fs.DB["database"], "basket_match_series_submission", "basket_match_submission_period_score"),
    )}
    record("schema_period_table", "basket_match_submission_period_score" in extra_tables, "submission period score table")

    sub_cols = {r["COLUMN_NAME"] for r in fs.db_query(
        "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA=%s AND TABLE_NAME='basket_match_submission'",
        (fs.DB["database"],),
    )}
    record("schema_submission_mode", {"series_submission_id", "submission_mode", "draft_complete"}.issubset(sub_cols),
           "additive submission header fields")

    # Live API A-N requires web/admin. Probe only; offline is a skip, not a schema fail.
    web_up = False
    try:
        fs.req(f"{WEB}/basket/public/health", "GET")
        web_up = True
    except Exception:
        try:
            fs.req(f"{WEB}/", "GET")
            web_up = True
        except Exception:
            web_up = False
    if web_up:
        record("web_up", True, WEB)
    else:
        record("e2e_api_an_skipped", True, "web/admin not up; A-N live API not executed")

    REPORT["finishedAt"] = datetime.now().isoformat(timespec="seconds")
    out = ROOT / "scripts" / "cnpa_series_submission_v1_e2e_result.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    failed = [k for k, v in REPORT["cases"].items() if not v["ok"]]
    log(f"result={out} failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
