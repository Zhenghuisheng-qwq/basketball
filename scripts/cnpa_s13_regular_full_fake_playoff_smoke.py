#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""SMOKE DATA ONLY.

Fill CNPA League 2026 S13 regular-season official results, rebuild standing,
confirm TopN qualification, and generate playoff HIGH_VS_LOW structure.

Does not modify production business code. Targets only the S13 smoke division.
"""
from __future__ import annotations

import json
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.ADMIN = "http://111.229.241.29:8080"
fs.WEB = "http://111.229.241.29:8881"
fs.DB.update({
    "host": "111.229.241.29",
    "user": "root",
    "password": "zlzcnpa@@qaq",
    "database": "basketball",
})

ADMIN = fs.ADMIN
req = fs.req
OUT = Path(__file__).with_name("cnpa_s13_regular_full_fake_playoff_smoke_result.json")

DIVISION_ID = 177
SEASON_ID = 142
SERIES_ID = 1
REGULAR_STAGE_ID = 645
PLAYOFF_STAGE_ID = 646
EXPECTED_NAME = "CNPA League 2026 S13 - PC 5V5"
REASON = "S13 SMOKE DATA ONLY regular official result"


def log(msg: str) -> None:
    print(msg, flush=True)


def need(resp: dict, name: str) -> dict:
    if resp.get("code") != 200:
        raise RuntimeError(f"{name} failed: {resp.get('msg') or resp}")
    return resp


def confirm_target() -> dict:
    div = fs.db_query(
        "SELECT d.id, d.division_name, d.division_status, s.id season_id, s.display_title, "
        "s.season_name, s.series_id FROM basket_division d "
        "JOIN basket_season s ON s.id=d.season_id "
        "WHERE d.id=%s AND d.is_delete=0",
        (DIVISION_ID,),
    )
    if not div:
        raise RuntimeError("division 177 not found")
    row = div[0]
    if int(row["season_id"]) != SEASON_ID or int(row["series_id"]) != SERIES_ID:
        raise RuntimeError(f"unexpected season/series: {row}")
    if row["division_name"] != EXPECTED_NAME:
        raise RuntimeError(f"unexpected division name: {row['division_name']}")
    teams = fs.db_query(
        "SELECT id, team_name_snapshot FROM basket_event_team "
        "WHERE division_id=%s AND is_delete=0 ORDER BY id",
        (DIVISION_ID,),
    )
    if len(teams) != 16 or not all(str(t["team_name_snapshot"]).startswith("S13") for t in teams):
        raise RuntimeError(f"not S13 smoke teams: {teams}")
    stages = fs.db_query(
        "SELECT id, stage_code, stage_name, competition_format, pairing_rule, best_of, "
        "semifinal_best_of, final_best_of, win_points, loss_points, forfeit_win_points, "
        "forfeit_loss_points, advancement_type, advancement_count, execution_status "
        "FROM basket_division_stage WHERE division_id=%s AND is_delete=0 ORDER BY stage_order",
        (DIVISION_ID,),
    )
    snap = fs.db_query(
        "SELECT source_template_code, snapshot_status FROM basket_division_format_snapshot "
        "WHERE division_id=%s AND is_current=1 AND is_delete=0",
        (DIVISION_ID,),
    )
    return {"division": row, "teams": teams, "stages": stages, "snapshot": snap[0] if snap else {}}


def pair_key(a: int, b: int) -> tuple[int, int]:
    return (a, b) if a < b else (b, a)


def build_plan(teams: list[dict], matches: list[dict], win_points: int, loss_points: int) -> dict:
    ordered = list(teams)
    ids = [int(t["id"]) for t in ordered]
    names = {int(t["id"]): t["team_name_snapshot"] for t in ordered}
    rank_of = {tid: i for i, tid in enumerate(ids)}  # 0 = expected R1

    # Default: higher expected rank beats lower. Flips create same-points pairs.
    flips = {
        pair_key(ids[3], ids[14]),  # R4 loses to R15 -> R4 and R5 both 11-4
        pair_key(ids[6], ids[14]),  # R7 loses to R15 -> R7 and R8 both 8-7
    }

    def winner_of(home: int, away: int) -> int:
        hi, ai = rank_of[home], rank_of[away]
        stronger, weaker = (home, away) if hi < ai else (away, home)
        if pair_key(home, away) in flips:
            return weaker
        return stronger

    def base_margin(winner: int, loser: int) -> int:
        gap = abs(rank_of[winner] - rank_of[loser])
        if pair_key(winner, loser) in flips:
            return 3
        return 8 + min(max(gap, 1), 12)

    planned = []
    for m in matches:
        mid = int(m["id"])
        home = int(m["home_event_team_id"])
        away = int(m["away_event_team_id"])
        w = winner_of(home, away)
        margin = base_margin(w, home if w == away else away)
        base = 70 + ((mid + home + away) % 8)
        if w == home:
            hs, aws = base + margin, base
        else:
            hs, aws = base, base + margin
        planned.append({
            "matchId": mid,
            "home": home,
            "away": away,
            "homeName": names[home],
            "awayName": names[away],
            "homeScore": hs,
            "awayScore": aws,
            "winner": w,
            "alreadyFinished": (m.get("match_status") or "") == "FINISHED",
        })

    def simulate(rows: list[dict]) -> dict[int, dict]:
        stats = {
            tid: {"w": 0, "l": 0, "pf": 0, "pa": 0, "pts": 0}
            for tid in ids
        }
        h2h = {}
        for row in rows:
            h, a = row["home"], row["away"]
            hs, aws = row["homeScore"], row["awayScore"]
            stats[h]["pf"] += hs
            stats[h]["pa"] += aws
            stats[a]["pf"] += aws
            stats[a]["pa"] += hs
            if hs > aws:
                stats[h]["w"] += 1
                stats[a]["l"] += 1
                stats[h]["pts"] += win_points
                stats[a]["pts"] += loss_points
                h2h[(h, a)] = h
            else:
                stats[a]["w"] += 1
                stats[h]["l"] += 1
                stats[a]["pts"] += win_points
                stats[h]["pts"] += loss_points
                h2h[(h, a)] = a
        for tid, st in stats.items():
            st["diff"] = st["pf"] - st["pa"]
            st["winRate"] = round(st["w"] / (st["w"] + st["l"]) * 100, 1) if (st["w"] + st["l"]) else 0.0
        return stats, h2h

    def sort_teams(stats: dict, h2h: dict) -> list[int]:
        def h2h_cmp(a: int, b: int) -> int:
            for row in planned:
                if {row["home"], row["away"]} == {a, b}:
                    return 1 if row["winner"] == a else -1
            return 0

        def key(tid: int):
            st = stats[tid]
            return (-st["pts"], -st["w"], tid)

        groups = defaultdict(list)
        for tid in ids:
            st = stats[tid]
            groups[(st["pts"], st["w"])].append(tid)
        ranked = []
        for _pts, _w in sorted(groups.keys(), key=lambda x: (-x[0], -x[1])):
            bucket = groups[(_pts, _w)]
            if len(bucket) == 2:
                a, b = bucket
                cmp = h2h_cmp(a, b)
                if cmp > 0:
                    ordered_b = [a, b]
                elif cmp < 0:
                    ordered_b = [b, a]
                elif stats[a]["diff"] != stats[b]["diff"]:
                    ordered_b = [a, b] if stats[a]["diff"] > stats[b]["diff"] else [b, a]
                else:
                    ordered_b = [a, b] if stats[a]["pf"] >= stats[b]["pf"] else [b, a]
                ranked.extend(ordered_b)
            else:
                ranked.extend(sorted(bucket, key=lambda t: (-stats[t]["diff"], -stats[t]["pf"], t)))
        return ranked

    # Equalize R7/R8 pointDiff: shrink the leader's winning margins, grow the trailer.
    r7, r8 = ids[6], ids[7]
    stats, h2h = simulate(planned)

    def nudge(team_id: int, step: int) -> bool:
        for row in planned:
            if row["winner"] != team_id:
                continue
            if {row["home"], row["away"]} == {r7, r8}:
                continue
            if row["winner"] == row["home"]:
                nxt = row["homeScore"] + step
                if nxt > row["awayScore"] and 60 <= nxt <= 125:
                    row["homeScore"] = nxt
                    return True
            else:
                nxt = row["awayScore"] + step
                if nxt > row["homeScore"] and 60 <= nxt <= 125:
                    row["awayScore"] = nxt
                    return True
        return False

    for _ in range(80):
        d7, d8 = stats[r7]["diff"], stats[r8]["diff"]
        if d7 == d8:
            break
        if d7 > d8:
            changed = nudge(r7, -1) or nudge(r8, 1)
        else:
            changed = nudge(r8, -1) or nudge(r7, 1)
        if not changed:
            break
        stats, h2h = simulate(planned)

    stats, h2h = simulate(planned)
    ranking = sort_teams(stats, h2h)
    expected = []
    for i, tid in enumerate(ranking, 1):
        st = stats[tid]
        expected.append({
            "rank": i,
            "eventTeamId": tid,
            "teamName": names[tid],
            "w": st["w"],
            "l": st["l"],
            "points": st["pts"],
            "winRate": st["winRate"],
            "pf": st["pf"],
            "pa": st["pa"],
            "diff": st["diff"],
            "qualified": i <= 8,
        })
    return {
        "planned": planned,
        "expected": expected,
        "caseA": (ids[3], ids[4]),
        "caseB": (ids[6], ids[7]),
        "names": names,
        "stats": {str(k): v for k, v in stats.items()},
    }


def snapshot_before() -> dict:
    matches = fs.db_query(
        "SELECT match_status, COUNT(*) c FROM basket_match "
        "WHERE division_id=%s AND stage_id=%s AND is_delete=0 GROUP BY match_status",
        (DIVISION_ID, REGULAR_STAGE_ID),
    )
    standing = fs.db_query(
        "SELECT COUNT(*) c FROM basket_standing WHERE division_id=%s AND is_delete=0",
        (DIVISION_ID,),
    )
    quals = fs.db_query(
        "SELECT COUNT(*) c FROM basket_division_stage_qualification q "
        "JOIN basket_division_stage st ON st.id=q.stage_id "
        "WHERE st.division_id=%s AND q.is_delete=0",
        (DIVISION_ID,),
    )
    series = fs.db_query(
        "SELECT COUNT(*) c FROM basket_match_series WHERE division_id=%s AND is_delete=0",
        (DIVISION_ID,),
    )
    return {
        "matchStatus": matches,
        "standingCount": int((standing[0] or {}).get("c") or 0),
        "qualificationCount": int((quals[0] or {}).get("c") or 0),
        "seriesCount": int((series[0] or {}).get("c") or 0),
    }


def apply_results(token: str, planned: list[dict]) -> dict:
    entered = 0
    corrected = 0
    failed = []
    for i, row in enumerate(planned, 1):
        body = {
            "matchId": row["matchId"],
            "homeScore": row["homeScore"],
            "awayScore": row["awayScore"],
            "reason": REASON,
        }
        if row["alreadyFinished"]:
            url = f"{ADMIN}/basket/v4/admin/stage-exception/match-result-correction"
            kind = "correct"
        else:
            url = f"{ADMIN}/basket/v4/admin/stage-exception/match-official-result"
            kind = "enter"
        resp = req(url, "POST", body, token, timeout=180)
        if resp.get("code") != 200:
            # already finished after a partial rerun
            if kind == "enter":
                resp2 = req(
                    f"{ADMIN}/basket/v4/admin/stage-exception/match-result-correction",
                    "POST", body, token, timeout=180,
                )
                if resp2.get("code") == 200:
                    corrected += 1
                    log(f"  [{i}/{len(planned)}] corrected {row['matchId']}")
                    continue
            failed.append({"matchId": row["matchId"], "resp": resp})
            log(f"  [{i}/{len(planned)}] FAIL {row['matchId']} {resp.get('msg')}")
            continue
        if kind == "correct":
            corrected += 1
        else:
            entered += 1
        if i % 10 == 0 or i == len(planned):
            log(f"  [{i}/{len(planned)}] applied")
    return {"entered": entered, "corrected": corrected, "failed": failed}


def load_admin_standing(token: str) -> list[dict]:
    data = need(req(f"{ADMIN}/basket/v4/admin/stages/{REGULAR_STAGE_ID}/standings", token=token), "standings").get("data") or []
    rows = []
    for scope in data:
        rows.extend(scope.get("rows") or [])
    rows.sort(key=lambda r: int(r.get("rankNo") or 999))
    return rows


def assert_tie_cases(standing: list[dict], plan: dict) -> dict:
    by_id = {int(r["eventTeamId"]): r for r in standing}
    a1, a2 = plan["caseA"]
    b1, b2 = plan["caseB"]
    ra, rb = by_id[a1], by_id[a2]
    out = {
        "caseA": {
            "teams": [plan["names"][a1], plan["names"][a2]],
            "points": [ra.get("standingPoints"), rb.get("standingPoints")],
            "diff": [ra.get("pointDiff"), rb.get("pointDiff")],
            "wins": [ra.get("winCount"), rb.get("winCount")],
            "ranks": [ra.get("rankNo"), rb.get("rankNo")],
            "result": "PASS" if int(ra["rankNo"]) < int(rb["rankNo"]) and ra["standingPoints"] == rb["standingPoints"] else "FAIL",
        },
        "caseB": {
            "teams": [plan["names"][b1], plan["names"][b2]],
            "points": [by_id[b1].get("standingPoints"), by_id[b2].get("standingPoints")],
            "diff": [by_id[b1].get("pointDiff"), by_id[b2].get("pointDiff")],
            "h2hWinner": plan["names"][b1],
            "ranks": [by_id[b1].get("rankNo"), by_id[b2].get("rankNo")],
            "result": "PASS" if int(by_id[b1]["rankNo"]) < int(by_id[b2]["rankNo"])
            and by_id[b1]["standingPoints"] == by_id[b2]["standingPoints"]
            and by_id[b1]["pointDiff"] == by_id[b2]["pointDiff"] else "FAIL",
        },
    }
    return out


def playoff_pairs(token: str) -> list[dict]:
    series = fs.db_query(
        "SELECT id, round_no, bracket_slot, best_of, home_event_team_id, away_event_team_id, "
        "source_series_id_home, source_series_id_away, series_status "
        "FROM basket_match_series WHERE division_id=%s AND stage_id=%s AND is_delete=0 "
        "ORDER BY round_no, bracket_slot",
        (DIVISION_ID, PLAYOFF_STAGE_ID),
    )
    names = {
        int(t["id"]): t["team_name_snapshot"]
        for t in fs.db_query(
            "SELECT id, team_name_snapshot FROM basket_event_team WHERE division_id=%s AND is_delete=0",
            (DIVISION_ID,),
        )
    }
    seeds = {
        int(t["event_team_id"]): t.get("seed_no")
        for t in fs.db_query(
            "SELECT event_team_id, seed_no FROM basket_division_stage_team "
            "WHERE stage_id=%s AND is_delete=0",
            (PLAYOFF_STAGE_ID,),
        )
    }
    out = []
    for s in series:
        hid, aid = s.get("home_event_team_id"), s.get("away_event_team_id")
        out.append({
            "seriesId": s["id"],
            "roundNo": s["round_no"],
            "slot": s["bracket_slot"],
            "bestOf": s["best_of"],
            "home": names.get(int(hid)) if hid else None,
            "away": names.get(int(aid)) if aid else None,
            "homeSeed": seeds.get(int(hid)) if hid else None,
            "awaySeed": seeds.get(int(aid)) if aid else None,
            "sourceHome": s.get("source_series_id_home"),
            "sourceAway": s.get("source_series_id_away"),
            "status": s.get("series_status"),
        })
    return out


def main() -> int:
    target = confirm_target()
    regular = next(s for s in target["stages"] if s["id"] == REGULAR_STAGE_ID)
    playoff = next(s for s in target["stages"] if s["id"] == PLAYOFF_STAGE_ID)
    win_points = int(regular["win_points"])
    loss_points = int(regular["loss_points"])
    top_n = int(regular["advancement_count"] or 0)
    log(f"TARGET series={SERIES_ID} season={SEASON_ID} division={DIVISION_ID}")
    log(f"  {target['division']['display_title']} / {target['division']['division_name']}")
    log(f"  template={target['snapshot'].get('source_template_code')} regular={REGULAR_STAGE_ID} playoff={PLAYOFF_STAGE_ID}")
    log(f"  win={win_points} loss={loss_points} topN={top_n}")
    log("TEST-ONLY confirmed: S13* smoke teams + @cnpa.smoke seed league")

    before = snapshot_before()
    log(f"BEFORE {before}")

    matches = fs.db_query(
        "SELECT id, home_event_team_id, away_event_team_id, match_status, publish_status, "
        "home_score, away_score, winner_event_team_id "
        "FROM basket_match WHERE division_id=%s AND stage_id=%s AND is_delete=0 ORDER BY id",
        (DIVISION_ID, REGULAR_STAGE_ID),
    )
    if len(matches) != 120:
        raise RuntimeError(f"expected 120 regular matches, got {len(matches)}")
    unpublished = [m for m in matches if (m.get("publish_status") or "") != "PUBLISHED"]
    if unpublished:
        raise RuntimeError(f"unpublished regular matches: {len(unpublished)}")

    plan = build_plan(target["teams"], matches, win_points, loss_points)
    log("EXPECTED RANKING")
    for row in plan["expected"]:
        flag = "QUALIFIED" if row["qualified"] else "ELIMINATED"
        log(
            f"  R{row['rank']:02d} {row['teamName']} {row['w']}-{row['l']} "
            f"pts={row['points']} wr={row['winRate']} pf={row['pf']} pa={row['pa']} "
            f"diff={row['diff']:+d} {flag}"
        )

    token = fs.admin_login()
    log("admin login ok")
    applied = apply_results(token, plan["planned"])
    log(f"APPLY entered={applied['entered']} corrected={applied['corrected']} failed={len(applied['failed'])}")
    if applied["failed"]:
        raise RuntimeError(f"result apply failed: {applied['failed'][:3]}")

    after_counts = fs.db_query(
        "SELECT match_status, COUNT(*) c FROM basket_match "
        "WHERE division_id=%s AND stage_id=%s AND is_delete=0 GROUP BY match_status",
        (DIVISION_ID, REGULAR_STAGE_ID),
    )
    log(f"MATCH STATUS AFTER {after_counts}")

    need(req(f"{ADMIN}/basket/v4/admin/stages/{REGULAR_STAGE_ID}/check-completion", "POST", token=token), "check-completion")
    standing = load_admin_standing(token)
    log("ADMIN STANDING")
    standing_out = []
    for r in standing:
        played = int(r.get("winCount") or 0) + int(r.get("loseCount") or 0)
        wr = round(int(r.get("winCount") or 0) / played * 100, 1) if played else 0.0
        item = {
            "rank": r.get("rankNo"),
            "eventTeamId": r.get("eventTeamId"),
            "team": r.get("teamName"),
            "w": r.get("winCount"),
            "l": r.get("loseCount"),
            "points": r.get("standingPoints"),
            "winRate": wr,
            "pf": r.get("pointFor"),
            "pa": r.get("pointAgainst"),
            "diff": r.get("pointDiff"),
        }
        standing_out.append(item)
        log(
            f"  R{item['rank']} {item['team']} {item['w']}-{item['l']} pts={item['points']} "
            f"wr={item['winRate']} pf={item['pf']} pa={item['pa']} diff={item['diff']}"
        )

    ties = assert_tie_cases(standing, plan)
    log(f"TIE A {ties['caseA']}")
    log(f"TIE B {ties['caseB']}")

    preview = need(
        req(f"{ADMIN}/basket/v4/admin/stages/{REGULAR_STAGE_ID}/qualification-preview", token=token),
        "qual-preview",
    ).get("data") or {}
    need(
        req(f"{ADMIN}/basket/v4/admin/stages/{REGULAR_STAGE_ID}/confirm-qualification", "POST", token=token),
        "qual-confirm",
    )
    gen = req(f"{ADMIN}/basket/v4/admin/stages/{PLAYOFF_STAGE_ID}/generate-schedule", "POST", token=token)
    log(f"playoff generate {gen.get('code')} {gen.get('msg')}")
    if gen.get("code") != 200:
        init = req(f"{ADMIN}/basket/v4/admin/stages/{PLAYOFF_STAGE_ID}/initialize-teams", "POST", token=token)
        log(f"playoff init {init.get('code')} {init.get('msg')}")
        gen = need(
            req(f"{ADMIN}/basket/v4/admin/stages/{PLAYOFF_STAGE_ID}/generate-schedule", "POST", token=token),
            "playoff-generate",
        )

    pairs = playoff_pairs(token)
    log("PLAYOFF SERIES")
    for p in pairs:
        log(f"  R{p['roundNo']} slot{p['slot']} BO{p['bestOf']} #{p['homeSeed']} {p['home']} vs #{p['awaySeed']} {p['away']} src={p['sourceHome']}/{p['sourceAway']}")

    qf = [p for p in pairs if int(p["roundNo"] or 0) == 1]
    seed_pairs = sorted(
        {(min(p["homeSeed"] or 99, p["awaySeed"] or 99), max(p["homeSeed"] or 99, p["awaySeed"] or 99)) for p in qf}
    )
    pairing_ok = seed_pairs == [(1, 8), (2, 7), (3, 6), (4, 5)]
    sf = [p for p in pairs if int(p["roundNo"] or 0) == 2]
    advance_ok = all(p.get("sourceHome") and p.get("sourceAway") for p in sf) and len(sf) == 2

    result = {
        "target": {
            "seriesId": SERIES_ID,
            "seasonId": SEASON_ID,
            "divisionId": DIVISION_ID,
            "regularStageId": REGULAR_STAGE_ID,
            "playoffStageId": PLAYOFF_STAGE_ID,
            "templateCode": target["snapshot"].get("source_template_code"),
            "testOnly": True,
        },
        "before": before,
        "matchStatusAfter": after_counts,
        "applied": {k: applied[k] for k in ("entered", "corrected") if k in applied},
        "failedCount": len(applied["failed"]),
        "scoring": {
            "winPoints": win_points,
            "lossPoints": loss_points,
            "pointDeductionUsed": False,
        },
        "expected": plan["expected"],
        "standing": standing_out,
        "ties": ties,
        "topN": top_n,
        "qualificationPreview": preview,
        "playoffPairs": pairs,
        "pairingOk": pairing_ok,
        "winnerAdvanceOk": advance_ok,
        "bo": {
            "regular": regular.get("best_of"),
            "firstRound": playoff.get("best_of"),
            "semi": playoff.get("semifinal_best_of"),
            "final": playoff.get("final_best_of"),
        },
    }
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    log(f"WROTE {OUT}")
    log(f"PAIRING {'PASS' if pairing_ok else 'FAIL'} {seed_pairs}")
    log(f"WINNER ADVANCE {'PASS' if advance_ok else 'FAIL'}")
    return 0 if pairing_ok and not applied["failed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
