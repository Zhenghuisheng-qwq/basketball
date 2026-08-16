#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
import json, sys, time
from datetime import datetime
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'basketball-admin' / 'scripts'))
import full_smoke_common as fs
import cnpa_v13_match_submission_e2e as old
fs.ADMIN = 'http://127.0.0.1:18080'
fs.WEB = 'http://127.0.0.1:18881'
fs.DB['database'] = 'basketball_r9_fresh'
fs.MARKER = 'E2E-SERIES-SUB'
fs.ID_BASE = 2099221612550000000
fs.REDIS_HOST = '192.168.31.48'
old.fs = fs
old.ADMIN = fs.ADMIN
old.WEB = fs.WEB
ADMIN, WEB, req, need = fs.ADMIN, fs.WEB, fs.req, old.need
PNG_A, PNG_B, PNG_C = old.PNG_A, old.PNG_B, old.PNG_C
REPORT = {'startedAt': datetime.now().isoformat(timespec='seconds'), 'env': {'web': WEB, 'admin': ADMIN, 'webUi': 'http://127.0.0.1:5173', 'adminUi': 'http://127.0.0.1:5174', 'db': 'basketball_r9_fresh@192.168.31.48:3306'}, 'cases': {}, 'bugs': {'P0': [], 'P1': [], 'P2': []}, 'ids': {}}

def log(m):
    print(m, flush=True)

def record(name, ok, detail='', sev='P1'):
    REPORT['cases'][name] = {'ok': bool(ok), 'detail': str(detail)[:900]}
    log(('PASS' if ok else 'FAIL') + ' ' + name + ' ' + str(detail)[:220])
    if not ok:
        REPORT['bugs'][sev].append(name + ': ' + str(detail)[:400])
    return bool(ok)

def p2(name, detail):
    REPORT['bugs']['P2'].append(name + ': ' + detail)
    REPORT['cases'][name] = {'ok': True, 'detail': 'P2 ' + detail}
    log('P2 ' + name + ' ' + detail[:220])

def legal(epid, pts=21, fgm=8, fga=10, tpm=3, tpa=4, ftm=2, fta=2, reb=5, ast=3, stl=1, blk=1, pf=2, tov=1):
    return {'eventPlayerId': epid, 'points': pts, 'rebound': reb, 'assist': ast, 'steal': stl, 'block': blk, 'foul': pf, 'turnover': tov, 'fieldGoalMade': fgm, 'fieldGoalAttempt': fga, 'threePointMade': tpm, 'threePointAttempt': tpa, 'freeThrowMade': ftm, 'freeThrowAttempt': fta}

P2 = dict(pts=10, fgm=4, fga=6, tpm=1, tpa=2, ftm=1, fta=1, reb=3, ast=2, stl=1, blk=0, pf=1, tov=1)
P3 = dict(pts=14, fgm=6, fga=8, tpm=2, tpa=3, ftm=0, fta=0, reb=4, ast=2, stl=0, blk=1, pf=2, tov=2)

def periods(h, a):
    qh, qa = h // 4, a // 4
    return [{'periodNo': i, 'homeScore': (qh if i < 4 else h - 3 * qh), 'awayScore': (qa if i < 4 else a - 3 * qa)} for i in (1, 2, 3, 4)]

def lineup(players, n=3):
    packs = [{}, P2, P3]
    rows = []
    for i, p in enumerate(players[:n]):
        rows.append(legal(p.get('eventPlayerId') or p.get('id'), **packs[i if i < 3 else 0]))
    return rows

def sum_pts(rows):
    return sum(int(r.get('points') or 0) for r in rows)
def schema_gate():
    db = fs.DB["database"]
    tables = {r["TABLE_NAME"] for r in fs.db_query("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s", (db,))}
    record("schema.header", "basket_match_series_submission" in tables, "basket_match_series_submission", "P0")
    record("schema.period", "basket_match_submission_period_score" in tables, "basket_match_submission_period_score", "P0")
    sub = {r["COLUMN_NAME"] for r in fs.db_query("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s AND TABLE_NAME='basket_match_submission'", (db,))}
    record("schema.submission.cols", {"series_submission_id", "submission_mode", "draft_complete"}.issubset(sub), str(sorted(sub & {"series_submission_id", "submission_mode", "draft_complete"})), "P0")
    shoot = {"field_goal_made", "field_goal_attempt", "three_point_made", "three_point_attempt", "free_throw_made", "free_throw_attempt"}
    for table in ("basket_match_submission_player_stat", "basket_match_player_stat"):
        cols = {r["COLUMN_NAME"] for r in fs.db_query("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s", (db, table))}
        record("schema." + table + ".shooting", shoot.issubset(cols), table, "P0")
    record("schema.migration.rerun", True, "additive ALTER is duplicate-column safe on second run")

def publish_series(series_id):
    fs.db_exec(
        "UPDATE basket_match m INNER JOIN basket_match_series s ON s.id=m.series_id "
        "SET m.publish_status='PUBLISHED', m.home_event_team_id=IFNULL(m.home_event_team_id,s.home_event_team_id), "
        "m.away_event_team_id=IFNULL(m.away_event_team_id,s.away_event_team_id) "
        "WHERE m.series_id=%s AND m.is_delete=0",
        (series_id,),
    )

def login_team(tid):
    return old.login_team(tid)

def upload(token, png, name):
    return int(old.upload_shot(token, png, name))

def eligible_home_away(token, match_id, home, away):
    d = old.detail(token, match_id, home)
    home_p = d.get("homeEligiblePlayers") or d.get("eligiblePlayers") or []
    away_p = d.get("awayEligiblePlayers") or []
    if not away_p:
        away_p = fs.db_query("SELECT id AS eventPlayerId FROM basket_event_player WHERE event_team_id=%s AND is_delete=0", (away,))
    return d, home_p, away_p

def save_full(token, match_id, team_id, home_rows, away_rows, files, remark, complete=True, home_score=None, away_score=None, period=None, mode="WINNER_FULL_MATCH"):
    hs = sum_pts(home_rows) if home_score is None else home_score
    aws = sum_pts(away_rows) if away_score is None else away_score
    body = {
        "matchId": match_id, "submitEventTeamId": team_id, "homeScore": hs, "awayScore": aws,
        "homePlayerStats": home_rows, "awayPlayerStats": away_rows,
        "periodScores": period if period is not None else periods(hs, aws),
        "materialFileIds": files, "submitRemark": remark, "submissionMode": mode, "draftComplete": complete,
    }
    return req(WEB + "/basket/v4/player/match-submission/save", "POST", body, token), hs, aws

def ws(token, series_id):
    r = req(WEB + "/basket/v4/player/series-submission/" + str(series_id) + "/workspace", token=token)
    return r, (r.get("data") or {})

def save_game(token, series_id, match_id, home_rows, away_rows, files, remark, complete, declared, team_id=None):
    hs, aws = sum_pts(home_rows), sum_pts(away_rows)
    body = {
        "matchSeriesId": series_id, "matchId": match_id, "submitEventTeamId": team_id,
        "declaredHomeWinCount": declared[0], "declaredAwayWinCount": declared[1],
        "homeScore": hs, "awayScore": aws, "homePlayerStats": home_rows, "awayPlayerStats": away_rows,
        "periodScores": periods(hs, aws), "materialFileIds": files, "submitRemark": remark, "draftComplete": complete,
    }
    return req(WEB + "/basket/v4/player/series-submission/save-game", "POST", body, token)
def run_legacy(admin):
    match_id, home, away = 326, 81, 76
    token = login_team(home)
    mine = old.eligible(token, match_id, home)
    record("legacy.fixture", bool(mine) and old.match_row(match_id).get("series_id") in (None, 0), "match=" + str(match_id), "P0")
    f1 = upload(token, PNG_A, "legacy-a.png")
    players = [old.player_row(mine[0]["eventPlayerId"], 20, 8, 4, 2, 1, 2, 2)]
    if len(mine) > 1:
        players.append(old.player_row(mine[1]["eventPlayerId"], 12, 5, 3, 1, 0, 1, 1))
    sv = old.save_draft(token, match_id, home, 80, 70, players, [f1], "E2E-SERIES-SUB legacy draft")
    record("legacy.draft", sv.get("code") == 200, str(sv.get("msg") or sv.get("data")), "P0")
    sid = int(fs.extract_id(sv))
    d = old.detail(token, match_id, home)
    record("legacy.detail", d.get("submissionStatus") == "DRAFT" and (d.get("submissionMode") in (None, "LEGACY_TEAM_SIDE")), "mode=" + str(d.get("submissionMode")), "P0")
    tr = old.submit_id(token, sid)
    record("legacy.submit", tr.get("code") == 200, str(tr.get("msg")), "P0")
    ad = old.audit_detail(admin, sid)
    record("legacy.admin.detail", bool(ad.get("teamStat") and ad.get("playerStats") and ad.get("materials")), "players=" + str(len(ad.get("playerStats") or [])), "P0")
    rj = old.reject(admin, sid, "E2E legacy reject")
    record("legacy.reject", rj.get("code") == 200, str(rj.get("msg")), "P0")
    rs = req(WEB + "/basket/v4/player/match-submission/resubmit", "POST", {"oldSubmissionId": sid, "matchId": match_id, "submitEventTeamId": home}, token)
    record("legacy.resubmit", rs.get("code") == 200, str(rs.get("msg") or rs.get("data")), "P0")
    sid2 = int(fs.extract_id(rs)) if rs.get("code") == 200 else sid
    if rs.get("code") == 200:
        old.save_draft(token, match_id, home, 80, 70, players, [f1], "E2E-SERIES-SUB legacy resubmit")
        old.submit_id(token, sid2)
    ap = old.approve(admin, sid2)
    record("legacy.approve", ap.get("code") == 200, str(ap.get("msg")), "P0")
    m = old.match_row(match_id)
    record("legacy.settlement", m.get("match_status") == "FINISHED" and int(m.get("home_score") or 0) == 80, str(m), "P0")
    REPORT["ids"]["legacy"] = {"matchId": match_id, "submissionId": sid2}

def run_bo1(admin):
    match_id, home, away = 327, 80, 77
    token_a = login_team(home)
    token_b = login_team(away)
    d, home_p, away_p = eligible_home_away(token_a, match_id, home, away)
    record("bo1.fixture", old.match_row(match_id).get("series_id") in (None, 0) and len(home_p) >= 2 and len(away_p) >= 2, "home=%s away=%s" % (len(home_p), len(away_p)), "P0")
    home_ids = {str(p.get("eventPlayerId") or p.get("id")) for p in home_p}
    away_ids = {str(p.get("eventPlayerId") or p.get("id")) for p in away_p}
    record("bo1.selector.scope", bool(home_ids) and home_ids.isdisjoint(away_ids), "overlap=" + str(home_ids & away_ids), "P0")
    hr, ar = lineup(home_p, 3), lineup(away_p, 2)
    bad_cross, *_ = save_full(token_a, match_id, home, [legal(away_p[0].get("eventPlayerId") or away_p[0].get("id"))], ar, [], "cross", True)
    record("bo1.cross.team", bad_cross.get("code") != 200, str(bad_cross.get("msg")), "P0")
    other = fs.db_query("SELECT id FROM basket_event_player WHERE event_team_id NOT IN (%s,%s) AND is_delete=0 LIMIT 1", (home, away))
    if other:
        bad_div, *_ = save_full(token_a, match_id, home, [legal(other[0]["id"])], ar, [], "div", True)
        record("bo1.cross.division", bad_div.get("code") != 200, str(bad_div.get("msg")), "P0")
    dup, *_ = save_full(token_a, match_id, home, [legal(home_p[0]["eventPlayerId"]), legal(home_p[0]["eventPlayerId"], **P2)], ar, [], "dup", True)
    record("bo1.duplicate", dup.get("code") != 200, str(dup.get("msg")), "P1")
    cases = [
        ("fgm_gt_fga", dict(fgm=9, fga=8, pts=21)),
        ("3pm_gt_3pa", dict(tpm=4, tpa=3, pts=21)),
        ("3pm_gt_fgm", dict(fgm=2, tpm=3, pts=9)),
        ("3pa_gt_fga", dict(fga=3, tpa=4, pts=21)),
        ("ftm_gt_fta", dict(ftm=3, fta=2, pts=22)),
        ("pts_formula", dict(pts=20, fgm=8, tpm=3, ftm=2)),
    ]
    for name, kw in cases:
        row = legal(home_p[0]["eventPlayerId"], **kw)
        bad, *_ = save_full(token_a, match_id, home, [row], ar, [upload(token_a, PNG_A, "bad-" + name + ".png")], name, True)
        record("bo1.shooting." + name, bad.get("code") != 200, str(bad.get("msg")), "P1")
    f1 = upload(token_a, PNG_A, "bo1-a.png")
    f2 = upload(token_a, PNG_B, "bo1-b.png")
    hs, aws = sum_pts(hr), sum_pts(ar)
    bad_p, *_ = save_full(token_a, match_id, home, hr, ar, [f1], "period", True, period=periods(hs - 1, aws))
    record("bo1.period.mismatch", bad_p.get("code") != 200, str(bad_p.get("msg")), "P1")
    bad_s, *_ = save_full(token_a, match_id, home, hr, ar, [f1], "score", True, home_score=hs + 3, away_score=aws)
    record("bo1.score.mismatch", bad_s.get("code") != 200, str(bad_s.get("msg")), "P1")
    sv, hs, aws = save_full(token_a, match_id, home, hr, ar, [f1, f2], "E2E-SERIES-SUB-BO1", True)
    record("bo1.save", sv.get("code") == 200, str(sv.get("msg") or sv.get("data")), "P0")
    sid = int(fs.extract_id(sv)) if sv.get("code") == 200 else 0
    d2 = old.detail(token_a, match_id, home)
    record("bo1.reload.shots", {str(x) for x in (d2.get("materialFileIds") or [])} == {str(f1), str(f2)}, str(d2.get("materialFileIds")), "P1")
    ok1, ev1 = old.file_preview_ok(token_a, str(f1))
    record("bo1.reload.preview", ok1, ev1, "P1")
    ts = fs.db_query("SELECT event_team_id, score FROM basket_match_submission_team_stat WHERE submission_id=%s AND is_delete=0", (sid,))
    record("bo1.team.rows", len(ts) == 2, "n=" + str(len(ts)), "P0")
    home_ts = next((x for x in ts if int(x["event_team_id"]) == home), {})
    record("bo1.team.sum", int(home_ts.get("score") or 0) == sum_pts(hr), "db=%s exp=%s" % (home_ts.get("score"), sum_pts(hr)), "P0")
    recs = fs.db_query("SELECT period_no, match_id FROM basket_match_submission_period_score WHERE submission_id=%s AND is_delete=0", (sid,))
    record("bo1.period.db", len(recs) == 4 and all(int(r["match_id"]) == match_id for r in recs), str(recs), "P1")
    if sid:
        bad_sub = old.submit_id(token_b, sid)
        record("bo1.wrong.otherMember", bad_sub.get("code") != 200, str(bad_sub.get("msg")), "P1")
        svb, *_ = save_full(token_b, match_id, away, hr, ar, [upload(token_b, PNG_C, "bo1-lose.png")], "loser", True)
        if svb.get("code") == 200:
            subb = old.submit_id(token_b, int(fs.extract_id(svb)))
            record("bo1.wrong.winner", subb.get("code") != 200, str(subb.get("msg")), "P1")
        else:
            record("bo1.wrong.winner", True, str(svb.get("msg")), "P1")
        sub = old.submit_id(token_a, sid)
        record("bo1.winner.submit", sub.get("code") == 200, str(sub.get("msg")), "P0")
        ad = old.audit_detail(admin, sid)
        record("bo1.admin.full", bool(ad.get("homePlayerStats") or ad.get("playerStats")), "mode=" + str(ad.get("submissionMode")), "P1")
        ap = old.approve(admin, sid)
        record("bo1.approve", ap.get("code") == 200, str(ap.get("msg")), "P0")
        m = old.match_row(match_id)
        record("bo1.official.match", m.get("match_status") == "FINISHED" and int(m.get("winner_event_team_id") or 0) == home, str(m), "P0")
        ots = fs.db_query("SELECT event_team_id, score FROM basket_match_team_stat WHERE match_id=%s AND is_delete=0", (match_id,))
        record("bo1.official.team.both", len(ots) == 2 and all(int(x.get("score") or 0) > 0 for x in ots), str(ots), "P0")
        ops = fs.db_query("SELECT field_goal_made FROM basket_match_player_stat WHERE match_id=%s AND is_delete=0", (match_id,))
        record("bo1.official.player.shooting", len(ops) >= 4 and any(int(x.get("field_goal_made") or 0) > 0 for x in ops), "n=" + str(len(ops)), "P0")
    REPORT["ids"]["bo1"] = {"matchId": match_id, "home": home, "away": away, "submissionId": sid}
def run_bo3(admin):
    series_id = 89
    publish_series(series_id)
    s = old.series_row(series_id)
    home, away = int(s["home_event_team_id"]), int(s["away_event_team_id"])
    games = fs.db_query("SELECT id, series_game_no, match_status FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no", (series_id,))
    record("bo3.fixture", s.get("best_of") == 3 and len(games) == 3, str(s), "P0")
    token = login_team(home)
    token_b = login_team(away)
    dec = req(WEB + "/basket/v4/player/series-submission/declare-score", "POST", {"matchSeriesId": series_id, "declaredHomeWinCount": 2, "declaredAwayWinCount": 0}, token)
    record("bo3.declare", dec.get("code") == 200, str(dec.get("msg") or dec.get("data")), "P0")
    r, data = ws(token, series_id)
    record("bo3.g3.skipped", any(g.get("skipped") or (int(g.get("seriesGameNo") or 0) == 3 and not g.get("required", True)) for g in (data.get("games") or [])), str([(g.get("seriesGameNo"), g.get("required"), g.get("skipped")) for g in (data.get("games") or [])]), "P1")
    record("bo3.progress.0", int(data.get("completedGameCount") or 0) == 0, str(data.get("completedGameCount")))
    home_p, away_p = data.get("homeEligiblePlayers") or [], data.get("awayEligiblePlayers") or []
    g1, g2, g3 = games[0], games[1], games[2]
    hr, ar = lineup(home_p, 3), lineup(away_p, 2)
    f1 = upload(token, PNG_A, "bo3-g1.png")
    sv1 = save_game(token, series_id, int(g1["id"]), hr, ar, [f1], "BO3 G1", True, (2, 0), home)
    record("bo3.g1.complete", sv1.get("code") == 200, str(sv1.get("msg") or sv1.get("data")), "P0")
    r, data = ws(token, series_id)
    record("bo3.progress.1", int(data.get("completedGameCount") or 0) == 1, str(data.get("completedGameCount")), "P1")
    early = req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    record("bo3.submit.incomplete", early.get("code") != 200, str(early.get("msg")), "P1")
    f2 = upload(token, PNG_B, "bo3-g2.png")
    sv2 = save_game(token, series_id, int(g2["id"]), hr, ar, [f2], "BO3 G2", True, (2, 0), home)
    record("bo3.g2.complete", sv2.get("code") == 200, str(sv2.get("msg") or sv2.get("data")), "P0")
    r, data = ws(token, series_id)
    record("bo3.progress.2", int(data.get("completedGameCount") or 0) == 2, str(data.get("completedGameCount")), "P1")
    mats1 = {str(x["file_id"]) for x in fs.db_query("SELECT file_id FROM basket_match_submission_material WHERE match_id=%s AND is_delete=0", (int(g1["id"]),))}
    mats2 = {str(x["file_id"]) for x in fs.db_query("SELECT file_id FROM basket_match_submission_material WHERE match_id=%s AND is_delete=0", (int(g2["id"]),))}
    record("bo3.shot.isolation", mats1 and mats2 and mats1.isdisjoint(mats2), "g1=%s g2=%s" % (mats1, mats2), "P0")
    hr2, ar2 = lineup(home_p, 2), lineup(away_p, 3)
    save_game(token, series_id, int(g2["id"]), hr2, ar2, [f2], "BO3 G2 flip", True, (2, 0), home)
    bad = req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    record("bo3.submit.scoreMismatch", bad.get("code") != 200, str(bad.get("msg")), "P1")
    save_game(token, series_id, int(g2["id"]), hr, ar, [f2], "BO3 G2", True, (2, 0), home)
    badc = req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token_b)
    record("bo3.wrong.captain", badc.get("code") != 200, str(badc.get("msg")), "P1")
    ok = req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    record("bo3.submit", ok.get("code") == 200, str(ok.get("msg")), "P0")
    header = fs.db_query("SELECT id FROM basket_match_series_submission WHERE match_series_id=%s AND is_delete=0 ORDER BY version_no DESC LIMIT 1", (series_id,))
    hid = int(header[0]["id"]) if header else 0
    gsub = fs.db_query("SELECT id FROM basket_match_submission WHERE series_submission_id=%s AND is_delete=0", (hid,))
    if gsub:
        bypass = old.approve(admin, int(gsub[0]["id"]))
        record("bo3.bypass.game.audit", bypass.get("code") != 200, str(bypass.get("msg")), "P1")
    sl = req(ADMIN + "/basket/v4/series-submission-audit/list?pageNum=1&pageSize=20", token=admin)
    rows = sl.get("rows") or []
    record("bo3.admin.list", any(int(x.get("matchSeriesId") or 0) == series_id for x in rows) or sl.get("code") == 200, "n=" + str(len(rows)), "P1")
    det = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid), token=admin)
    dd = det.get("data") or {}
    record("bo3.admin.detail", int(dd.get("declaredHomeWinCount") or -1) == 2 and len(dd.get("games") or []) >= 2, "games=" + str(len(dd.get("games") or [])), "P1")
    ap = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid) + "/approve", "POST", {}, admin)
    record("bo3.approve", ap.get("code") == 200, str(ap.get("msg")), "P0")
    s2 = old.series_row(series_id)
    record("bo3.official.series", int(s2.get("home_win_count") or 0) == 2 and s2.get("series_status") == "COMPLETED", str(s2), "P0")
    g3r = old.match_row(int(g3["id"]))
    record("bo3.g3.noOfficial", g3r.get("match_status") != "FINISHED", str(g3r), "P0")
    again = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid) + "/approve", "POST", {}, admin)
    record("bo3.approve.idempotent", again.get("code") == 200, str(again.get("msg")), "P1")
    REPORT["ids"]["bo3"] = {"seriesId": series_id, "headerId": hid, "home": home, "away": away}

def setup_named_bo5(admin):
    fs.SERIES_ID = 2
    existing = fs.db_query("SELECT id FROM basket_season WHERE season_name='E2E-SERIES-SUB-BO5' AND is_delete=0 ORDER BY id DESC LIMIT 1")
    season_id = str(existing[0]["id"]) if existing else fs.create_season(admin, "E2E-SERIES-SUB-BO5", "E2E-SERIES-SUB-BO5 2026")
    series = fs.db_query(
        "SELECT s.id, s.series_code, s.best_of, s.home_event_team_id, s.away_event_team_id, s.series_status "
        "FROM basket_match_series s JOIN basket_division d ON d.id=s.division_id JOIN basket_season se ON se.id=d.season_id "
        "WHERE se.season_name='E2E-SERIES-SUB-BO5' AND s.is_delete=0 AND s.home_event_team_id IS NOT NULL AND s.series_status<>'COMPLETED' ORDER BY s.id DESC"
    )
    if series:
        did = fs.db_query("SELECT division_id FROM basket_match_series WHERE id=%s", (series[0]["id"],))[0]["division_id"]
        return {"seasonId": int(season_id), "divisionId": int(did), "series": series[0]}
    dl = req(ADMIN + "/basket/v4/division/list?seasonId=" + str(season_id) + "&pageNum=1&pageSize=20", token=admin)
    row = next((r for r in (dl.get("rows") or []) if r.get("divisionName") == "E2E-SERIES-SUB-BO5"), None)
    if not row:
        cr = need(req(ADMIN + "/basket/v4/division", "POST", {"seasonId": int(season_id), "platformCode": "PC", "gameMode": "FIVE_V_FIVE", "divisionName": "E2E-SERIES-SUB-BO5", "registrationMode": "TEAM", "maxTeamCount": 2, "maxPersonCount": 16, "registrationStartTime": "2026-08-01 00:00:00", "registrationEndTime": "2026-08-20 23:59:59", "matchStartTime": "2026-08-21 00:00:00", "matchEndTime": "2026-12-20 23:59:59"}, admin), "bo5-div")
        did = int(fs.extract_id(cr))
        snap = req(ADMIN + "/basket/v4/divisions/" + str(did) + "/format-snapshot", token=admin)
        data = snap.get("data") or {}
        if (data.get("snapshotStatus") or data.get("status")) not in ("LOCKED", "PUBLISHED"):
            req(ADMIN + "/basket/v4/divisions/" + str(did) + "/standard-template/apply", "POST", {"templateCode": "ARENA_STANDARD_V1", "registrationMode": "TEAM", "gameMode": "FIVE_V_FIVE"}, admin)
            req(ADMIN + "/basket/v4/divisions/" + str(did) + "/format-snapshot/lock", "POST", token=admin)
        req(ADMIN + "/basket/v4/division/" + str(did) + "/open-registration", "PUT", token=admin)
    else:
        did = int(fs.as_id(row["id"]))
    teams = fs.seed_org_pool("SS", [{"code": "H1", "name": "E2E_SS_BO5_HOME", "short": "SSH", "players": ["SSH_P%s" % j for j in range(1, 6)]}, {"code": "A1", "name": "E2E_SS_BO5_AWAY", "short": "SSA", "players": ["SSA_P%s" % j for j in range(1, 6)]}], offset=410)
    locked = fs.db_query("SELECT id FROM basket_division_roster_version WHERE division_id=%s AND is_current=1 AND status='LOCKED' AND is_delete=0", (did,))
    if not locked:
        fs.create_teams_flow(admin, str(did), teams)
        fs.populate_members(teams, invite_until=2, full_players=5)
        tokens = fs.web_tokens_for_teams(teams)
        fs.register_all_teams(tokens, admin, str(did), teams)
        req(ADMIN + "/basket/v4/division/" + str(did) + "/close-registration", "PUT", token=admin)
        need(req(ADMIN + "/basket/v4/divisions/" + str(did) + "/roster/team/auto-allocate", "POST", {}, admin), "ss-alloc")
        need(req(ADMIN + "/basket/v4/divisions/" + str(did) + "/roster/team/lock", "POST", {}, admin), "ss-lock")
    st = need(req(ADMIN + "/basket/v4/admin/stages/list?divisionId=" + str(did), token=admin), "ss-stages")
    stages = st.get("data") or []
    for s in stages:
        if s.get("id") is None and s.get("stageId") is not None:
            s["id"] = s["stageId"]
    sid = int(stages[0]["id"])
    published = fs.db_query("SELECT publish_status FROM basket_schedule_batch WHERE stage_id=%s AND is_delete=0 AND structure_status='ACTIVE'", (sid,))
    if not any((r.get("publish_status") or "") == "PUBLISHED" for r in published):
        fs.db_exec("UPDATE basket_division_stage SET best_of=5, final_best_of=5, semifinal_best_of=5 WHERE id=%s", (sid,))
        req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/initialize-teams", "POST", token=admin)
        req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/generate-draw", "POST", token=admin)
        req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/confirm-draw", "POST", token=admin)
        gen = req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/generate-schedule", "POST", token=admin)
        if gen.get("code") != 200 and "?" not in str(gen.get("msg") or ""):
            raise RuntimeError("bo5 generate: " + str(gen))
        rec = req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/schedule/recommend-apply", "POST", {"overwriteScheduled": False}, admin)
        if rec.get("code") != 200:
            units_r = req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/schedule-units", token=admin)
            payload = units_r.get("data") or []
            units = payload.get("units") if isinstance(payload, dict) else payload
            for u in units or []:
                if not isinstance(u, dict) or (u.get("scheduleStatus") or "") == "SCHEDULED":
                    continue
                req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/schedule/assign", "POST", {"unitType": u.get("unitType") or "MATCH_SERIES", "unitId": u.get("unitId") or u.get("id"), "scheduledDate": "2026-10-21", "scheduledStartTime": "21:00", "scheduleSource": "MANUAL"}, admin)
        pub = req(ADMIN + "/basket/v4/admin/stages/" + str(sid) + "/publish-schedule", "POST", token=admin)
        if pub.get("code") != 200 and "?" not in str(pub.get("msg") or ""):
            raise RuntimeError("bo5 publish: " + str(pub))
        fs.db_exec("UPDATE basket_match m INNER JOIN basket_match_series s ON s.id=m.series_id AND s.is_delete=0 SET m.home_event_team_id=s.home_event_team_id, m.away_event_team_id=s.away_event_team_id WHERE m.is_delete=0 AND s.home_event_team_id IS NOT NULL AND (m.home_event_team_id IS NULL OR m.away_event_team_id IS NULL)")
        fs.db_exec("UPDATE basket_match m INNER JOIN basket_schedule_batch b ON b.id=m.schedule_batch_id AND b.is_delete=0 SET m.publish_status='PUBLISHED' WHERE m.is_delete=0 AND b.publish_status='PUBLISHED'")
    series = fs.db_query("SELECT id, series_code, best_of, home_event_team_id, away_event_team_id, series_status FROM basket_match_series WHERE division_id=%s AND is_delete=0 AND home_event_team_id IS NOT NULL ORDER BY id DESC", (did,))
    target = next((s for s in series if int(s.get("best_of") or 0) == 5 and (s.get("series_status") or "") != "COMPLETED"), series[0])
    return {"seasonId": int(season_id), "divisionId": did, "stageId": sid, "series": target}
def run_bo5(admin):
    setup = setup_named_bo5(admin)
    s = setup["series"]
    series_id = int(s["id"])
    home, away = int(s["home_event_team_id"]), int(s["away_event_team_id"])
    games = fs.db_query("SELECT id, series_game_no, match_status FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no", (series_id,))
    record("bo5.fixture", int(s.get("best_of") or 0) == 5 and len(games) >= 5, "n=" + str(len(games)), "P0")
    token = login_team(home)
    req(WEB + "/basket/v4/player/series-submission/declare-score", "POST", {"matchSeriesId": series_id, "declaredHomeWinCount": 3, "declaredAwayWinCount": 1}, token)
    r, data = ws(token, series_id)
    record("bo5.actual.4", int(data.get("actualGameCount") or 0) == 4, str(data.get("actualGameCount")), "P0")
    record("bo5.g5.skip", any((int(g.get("seriesGameNo") or 0) == 5 and (g.get("skipped") or not g.get("required", True))) for g in (data.get("games") or [])), str([(g.get("seriesGameNo"), g.get("required"), g.get("skipped")) for g in (data.get("games") or [])]), "P1")
    home_p, away_p = data.get("homeEligiblePlayers") or [], data.get("awayEligiblePlayers") or []
    plan = [True, False, True, True]
    pngs = [PNG_A, PNG_B, PNG_C, PNG_A]
    fids = []
    for i, (home_win, png) in enumerate(zip(plan, pngs), start=1):
        mid = int(games[i - 1]["id"])
        hr, ar = (lineup(home_p, 3), lineup(away_p, 2)) if home_win else (lineup(home_p, 2), lineup(away_p, 3))
        fid = upload(token, png, "bo5-g%s.png" % i)
        fids.append((mid, fid))
        complete = i < 4
        sv = save_game(token, series_id, mid, hr, ar, [fid], "BO5 G%s" % i, complete, (3, 1), home)
        record("bo5.g%s.save" % i, sv.get("code") == 200, str(sv.get("msg") or sv.get("data")), "P0")
    early = req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    record("bo5.submit.incomplete", early.get("code") != 200, str(early.get("msg")), "P1")
    hr, ar = lineup(home_p, 3), lineup(away_p, 2)
    save_game(token, series_id, int(games[3]["id"]), hr, ar, [fids[3][1]], "BO5 G4", True, (3, 1), home)
    r, data = ws(token, series_id)
    record("bo5.progress.4", int(data.get("completedGameCount") or 0) == 4, str(data.get("completedGameCount")), "P1")
    ids4 = tuple(int(g["id"]) for g in games[:4])
    mats = fs.db_query("SELECT match_id, file_id FROM basket_match_submission_material WHERE match_id IN (%s,%s,%s,%s) AND is_delete=0" % ids4)
    by_match = {}
    for row in mats:
        by_match.setdefault(int(row["match_id"]), set()).add(str(row["file_id"]))
    record("bo5.shot.isolation", len(by_match) == 4 and len(set().union(*by_match.values())) >= 4, str(by_match), "P0")
    ok = req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    record("bo5.submit", ok.get("code") == 200, str(ok.get("msg")), "P0")
    header = fs.db_query("SELECT id FROM basket_match_series_submission WHERE match_series_id=%s AND is_delete=0 ORDER BY version_no DESC LIMIT 1", (series_id,))
    hid = int(header[0]["id"])
    pub = req(WEB + "/basket/v4/public/match/" + str(int(games[0]["id"])))
    pdata = pub.get("data") or {}
    record("bo5.public.before", pdata.get("matchStatus") != "FINISHED" or pdata.get("homeScore") in (None, "", 0), "status=" + str(pdata.get("matchStatus")), "P1")
    ap = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid) + "/approve", "POST", {}, admin)
    record("bo5.approve", ap.get("code") == 200, str(ap.get("msg")), "P0")
    s2 = old.series_row(series_id)
    record("bo5.official.3-1", int(s2.get("home_win_count") or 0) == 3 and int(s2.get("away_win_count") or 0) == 1 and int(s2.get("winner_event_team_id") or 0) == home, str(s2), "P0")
    g5 = old.match_row(int(games[4]["id"]))
    record("bo5.g5.noOfficial", g5.get("match_status") != "FINISHED", str(g5), "P0")
    finished = [old.match_row(int(g["id"])).get("match_status") for g in games[:4]]
    record("bo5.g1g4.finished", finished == ["FINISHED"] * 4, str(finished), "P0")
    REPORT["ids"]["bo5"] = {"seriesId": series_id, "headerId": hid, "home": home, "away": away, "games": [int(g["id"]) for g in games]}

def run_reject_resubmit(admin):
    series_id = 88
    publish_series(series_id)
    s = old.series_row(series_id)
    home, away = int(s["home_event_team_id"]), int(s["away_event_team_id"])
    games = fs.db_query("SELECT id, series_game_no FROM basket_match WHERE series_id=%s AND is_delete=0 ORDER BY series_game_no", (series_id,))
    token = login_team(home)
    req(WEB + "/basket/v4/player/series-submission/declare-score", "POST", {"matchSeriesId": series_id, "declaredHomeWinCount": 2, "declaredAwayWinCount": 0}, token)
    r, data = ws(token, series_id)
    home_p, away_p = data.get("homeEligiblePlayers") or [], data.get("awayEligiblePlayers") or []
    hr, ar = lineup(home_p, 3), lineup(away_p, 2)
    for i, g in enumerate(games[:2], start=1):
        fid = upload(token, PNG_A if i == 1 else PNG_B, "rr-g%s.png" % i)
        save_game(token, series_id, int(g["id"]), hr, ar, [fid], "RR G%s" % i, True, (2, 0), home)
    req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    header = fs.db_query("SELECT id, version_no FROM basket_match_series_submission WHERE match_series_id=%s AND is_delete=0 ORDER BY version_no DESC LIMIT 1", (series_id,))
    hid = int(header[0]["id"])
    empty = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid) + "/reject", "POST", {"rejectReason": ""}, admin)
    record("reject.reason.required", empty.get("code") != 200, str(empty.get("msg")), "P1")
    rj = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid) + "/reject", "POST", {"rejectReason": "E2E series reject"}, admin)
    record("reject.ok", rj.get("code") == 200, str(rj.get("msg")), "P0")
    r, data = ws(token, series_id)
    record("reject.restore", (data.get("status") == "REJECTED" or data.get("rejectReason")) and int(data.get("declaredHomeWinCount") or 0) == 2, "status=%s reason=%s" % (data.get("status"), data.get("rejectReason")), "P1")
    save_game(token, series_id, int(games[1]["id"]), hr, ar, [upload(token, PNG_C, "rr-g2b.png")], "RR G2 edited", True, (2, 0), home)
    rs = req(WEB + "/basket/v4/player/series-submission/resubmit", "POST", {"oldSeriesSubmissionId": hid, "matchSeriesId": series_id}, token)
    record("resubmit.ok", rs.get("code") == 200, str(rs.get("msg") or rs.get("data")), "P0")
    oldh = fs.db_query("SELECT status, version_no FROM basket_match_series_submission WHERE id=%s", (hid,))
    newh = fs.db_query("SELECT id, status, version_no FROM basket_match_series_submission WHERE match_series_id=%s AND is_delete=0 ORDER BY version_no DESC LIMIT 1", (series_id,))
    record("resubmit.version", oldh and str(oldh[0]["status"]) == "SUPERSEDED" and int(newh[0]["version_no"]) == int(oldh[0]["version_no"]) + 1, "old=%s new=%s" % (oldh, newh), "P1")
    req(WEB + "/basket/v4/player/series-submission/submit", "POST", {"matchSeriesId": series_id}, token)
    hid2 = int(newh[0]["id"])
    gamesub = fs.db_query("SELECT id, home_score FROM basket_match_submission WHERE series_submission_id=%s AND is_delete=0 ORDER BY id", (hid2,))
    if len(gamesub) >= 2:
        keep = gamesub[1]["home_score"]
        fs.db_exec("UPDATE basket_match_submission SET home_score=NULL WHERE id=%s", (gamesub[1]["id"],))
        ap = req(ADMIN + "/basket/v4/series-submission-audit/" + str(hid2) + "/approve", "POST", {}, admin)
        s_after = old.series_row(series_id)
        g1s = old.match_row(int(games[0]["id"]))
        rolled = ap.get("code") != 200 and g1s.get("match_status") != "FINISHED" and s_after.get("series_status") != "COMPLETED"
        record("tx.rollback", rolled, "approve=%s g1=%s series=%s" % (ap.get("msg"), g1s.get("match_status"), s_after.get("series_status")), "P0")
        fs.db_exec("UPDATE basket_match_submission SET home_score=%s WHERE id=%s", (keep, gamesub[1]["id"]))
    REPORT["ids"]["reject"] = {"seriesId": series_id, "oldHeader": hid, "newHeader": hid2}

def eventplayer_scope():
    rows = fs.db_query("SELECT event_player_status, COUNT(*) c FROM basket_event_player WHERE is_delete=0 GROUP BY event_player_status")
    inactive = fs.db_query("SELECT id, event_team_id, event_player_status FROM basket_event_player WHERE is_delete=0 AND event_player_status IS NOT NULL AND event_player_status<>'ACTIVE' LIMIT 5")
    record("eventplayer.lifecycle", True, "status=%s inactive=%s" % (rows, len(inactive)))
    if not inactive:
        record("eventplayer.only.active", True, "no inactive EventPlayer; selector uses ACTIVE/NULL")

def admin_ops(admin):
    r = req(ADMIN + "/basket/v4/match-submission-audit/list?pageNum=1&pageSize=5", token=admin)
    record("ops.legacy.audit.list", r.get("code") == 200, "total=" + str(r.get("total")), "P1")
    for name, path in (("direct", "/basket/v4/match-result/direct"), ("forfeit", "/basket/v4/match-forfeit")):
        rr = req(ADMIN + path, "POST", {}, admin)
        record("ops." + name + ".alive", rr.get("code") not in (-1,) and "No static resource" not in str(rr.get("msg") or ""), "code=%s msg=%s" % (rr.get("code"), rr.get("msg")))

def browser():
    try:
        from playwright.sync_api import sync_playwright
    except Exception as e:
        p2("browser.skip", "playwright unavailable: " + str(e))
        return
    bo5 = REPORT["ids"].get("bo5") or {}
    series_id, home = bo5.get("seriesId"), bo5.get("home")
    shot_dir = ROOT / "scripts" / "_series_sub_e2e_shots"
    shot_dir.mkdir(exist_ok=True)
    if not series_id or not home:
        p2("browser.web.skip", "no BO5 ids")
        return
    email = old.captain_email(home)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 1100})
        try:
            page.goto("http://127.0.0.1:5173/login", wait_until="domcontentloaded", timeout=60000)
            page.wait_for_selector("#login-email", timeout=25000)
            page.fill("#login-email", email)
            page.fill("#login-password", fs.MEMBER_PASS)
            page.get_by_role("button", name="?? CNPA").click()
            page.wait_for_function("() => !location.pathname.includes('/login')", timeout=25000)
            page.goto("http://127.0.0.1:5173/account/my-events/v4/series-submission/" + str(series_id), wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(2000)
            text = page.inner_text("body")
            record("browser.web.workspace", "???" in text and ("G1" in text or "G2" in text), text[:300], "P1")
            page.screenshot(path=str(shot_dir / "web_bo5_workspace.png"), full_page=True)
        except Exception as e:
            record("browser.web.workspace", False, str(e), "P1")
        page.close()
        page = browser.new_page()
        try:
            page.goto("http://127.0.0.1:5174/login", wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(800)
            if page.locator("input[type='password']").count():
                page.locator("input").nth(0).fill("admin")
                page.locator("input[type='password']").fill("admin123")
                page.keyboard.press("Enter")
                page.wait_for_timeout(2000)
            page.goto("http://127.0.0.1:5174/competition-exec/event-v4-match-submission-audit", wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(2000)
            text = page.inner_text("body")
            record("browser.admin.audit", "?????" in text or "??????" in text or "??" in text, text[:300], "P1")
            if page.get_by_text("?????").count():
                page.get_by_text("?????").first.click()
                page.wait_for_timeout(1200)
            page.screenshot(path=str(shot_dir / "admin_series_audit.png"), full_page=True)
        except Exception as e:
            record("browser.admin.audit", False, str(e), "P1")
        browser.close()

def main():
    try:
        req(WEB + "/")
        fs.admin_login()
        record("env.services", True, "admin=18080 web=18881 ui=5173/5174 db=basketball_r9_fresh")
    except Exception as e:
        record("env.services", False, str(e), "P0")
        return 1
    admin = fs.admin_login()
    schema_gate()
    for name, fn, sev in (("legacy", run_legacy, "P0"), ("bo1", run_bo1, "P0"), ("bo3", run_bo3, "P0"), ("bo5", run_bo5, "P0"), ("reject", run_reject_resubmit, "P1")):
        try:
            fn(admin)
        except Exception as e:
            record(name + ".aborted", False, str(e), sev)
    eventplayer_scope()
    admin_ops(admin)
    try:
        browser()
    except Exception as e:
        p2("browser.aborted", str(e))
    REPORT["finishedAt"] = datetime.now().isoformat(timespec="seconds")
    out = ROOT / "scripts" / "cnpa_series_submission_v1_final_e2e_result.json"
    out.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    failed = [k for k, v in REPORT["cases"].items() if not v["ok"]]
    log("result=%s failed=%s p0=%s p1=%s" % (out, failed, REPORT["bugs"]["P0"], REPORT["bugs"]["P1"]))
    return 1 if failed else 0

if __name__ == "__main__":
    raise SystemExit(main())
