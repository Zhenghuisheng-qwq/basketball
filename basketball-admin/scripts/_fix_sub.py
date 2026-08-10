from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
# Rewrite submit_and_approve_match to handle pending/approved states
start=t.find("def submit_and_approve_match")
end=t.find("\ndef ", start+1)
new='''def submit_and_approve_match(
    admin_token: str,
    web_token: str,
    match: dict,
    home_score: int,
    away_score: int,
    submit_event_team_id,
    reject_first: bool = False,
) -> dict:
    mid = match["id"]
    # If already finished, no-op
    if match.get("matchStatus") in ("FINISHED", "FORFEITED"):
        return {"matchId": mid, "skipped": True}

    lr0 = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=10", token=admin_token)
    rows0 = lr0.get("rows") or []
    pending0 = next((x for x in rows0 if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    approved0 = next((x for x in rows0 if x.get("submissionStatus") == "APPROVED"), None)
    if approved0 and match.get("matchStatus") in ("FINISHED", "FORFEITED", None):
        # may still need approve sync; if pending none and finished elsewhere
        pass
    if pending0 and not reject_first:
        ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending0['submissionId']}/approve", "POST", {}, admin_token)
        if ar.get("code") != 200:
            raise RuntimeError(f"approve pending failed match {mid}: {ar}")
        return {"submissionId": pending0["submissionId"], "matchId": mid, "approve": ar, "reusedPending": True}
    if pending0 and reject_first:
        rr = req(
            f"{ADMIN}/basket/v4/match-submission-audit/{pending0['submissionId']}/reject",
            "POST",
            {"rejectReason": "FULL_SMOKE reject then resubmit", "auditRemark": "FULL_SMOKE reject then resubmit"},
            admin_token,
        )
        if rr.get("code") != 200:
            raise RuntimeError(f"reject failed: {rr}")
        reject_first = False  # continue to resubmit below

    detail = req(
        f"{WEB}/basket/v4/player/match-submission/{mid}/detail?submitEventTeamId={submit_event_team_id}",
        token=web_token,
    )
    if detail.get("code") != 200:
        raise RuntimeError(f"submission detail failed match {mid}: {detail}")
    d = detail["data"]
    player_stats = []
    for i, ps in enumerate(d.get("playerStats") or []):
        base = 8 + (int(str(ps.get("eventPlayerId") or "0")[-2:]) % 17)
        own = as_id(ps.get("eventTeamId")) == as_id(submit_event_team_id)
        points = base + (6 if own else 2) + (i % 5)
        player_stats.append(
            {
                "eventPlayerId": ps["eventPlayerId"],
                "points": points,
                "rebound": 3 + (i % 7),
                "assist": 2 + (i % 6),
                "steal": 1 + (i % 3),
                "block": i % 2,
                "turnover": 1 + (i % 4),
            }
        )
    body = {
        "matchId": mid,
        "submitEventTeamId": submit_event_team_id,
        "homeScore": home_score,
        "awayScore": away_score,
        "teamStat": {
            "score": home_score if as_id(submit_event_team_id) == as_id(match.get("homeEventTeamId")) else away_score,
            "rebound": 28 + (home_score % 7),
            "assist": 14 + (away_score % 5),
            "steal": 5,
            "block": 2,
            "turnover": 9,
        },
        "playerStats": player_stats,
        "materialFileIds": [],
        "submitRemark": MARKER,
    }
    sr = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
    if sr.get("code") != 200:
        raise RuntimeError(f"save submission failed match {mid}: {sr}")
    sub_id = extract_id(sr)
    tr = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id}, web_token)
    if tr.get("code") != 200:
        raise RuntimeError(f"submit failed match {mid}: {tr}")
    lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
    pending = next((x for x in (lr.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
    if not pending:
        raise RuntimeError(f"no pending submission for match {mid}")
    if reject_first:
        rr = req(
            f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/reject",
            "POST",
            {"rejectReason": "FULL_SMOKE reject then resubmit", "auditRemark": "FULL_SMOKE reject then resubmit"},
            admin_token,
        )
        if rr.get("code") != 200:
            raise RuntimeError(f"reject failed: {rr}")
        sr2 = req(f"{WEB}/basket/v4/player/match-submission/save", "POST", body, web_token)
        if sr2.get("code") != 200:
            raise RuntimeError(f"resave failed: {sr2}")
        sub_id2 = extract_id(sr2)
        tr2 = req(f"{WEB}/basket/v4/player/match-submission/submit", "POST", {"submissionId": sub_id2}, web_token)
        if tr2.get("code") != 200:
            raise RuntimeError(f"resubmit failed: {tr2}")
        lr = req(f"{ADMIN}/basket/v4/match-submission-audit/list?matchId={mid}&pageNum=1&pageSize=5", token=admin_token)
        pending = next((x for x in (lr.get("rows") or []) if x.get("submissionStatus") == "PENDING_REVIEW"), None)
        if not pending:
            raise RuntimeError(f"no pending after resubmit match {mid}")
    ar = req(f"{ADMIN}/basket/v4/match-submission-audit/{pending['submissionId']}/approve", "POST", {}, admin_token)
    if ar.get("code") != 200:
        raise RuntimeError(f"approve failed match {mid}: {ar}")
    return {"submissionId": pending["submissionId"], "matchId": mid, "approve": ar}
'''
p.write_text(t[:start]+new+t[end:], encoding="utf-8", newline="\n")
import ast; ast.parse(p.read_text(encoding="utf-8")); print("ok")
