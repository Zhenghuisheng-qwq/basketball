from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
start = t.find("def create_teams_flow")
end = t.find("\ndef ", start + 1)
if start < 0 or end < 0:
    raise SystemExit(f"bounds {start} {end}")
new_fn = '''def create_teams_flow(admin_token: str, division_id: str, teams: list[dict], logos: list[str] | None = None) -> list[str]:
    """Create/approve teams via Web API. Idempotent if team already exists for captain in division."""
    team_ids: list[str] = []
    for i, t in enumerate(teams):
        # reuse existing ACTIVE team for this captain+division
        existing = db_query(
            "SELECT id, audit_status FROM basket_team WHERE division_id=%s AND captain_player_id=%s "
            "AND team_status=1 AND is_delete=0 ORDER BY id DESC LIMIT 1",
            (int(division_id), int(t["captain_player_id"])),
        )
        if existing:
            tid = as_id(existing[0]["id"])
            t["team_id"] = tid
            if int(existing[0].get("audit_status") or 0) != 1:
                ar = approve_team(admin_token, tid)
                if ar.get("code") != 200:
                    raise RuntimeError(f"approve existing team failed {t['name']}: {ar}")
            team_ids.append(tid)
            continue

        gr = create_captain_grant(admin_token, division_id, t["captain_player_id"])
        if gr.get("code") != 200:
            # tolerate already-granted (avoid encoding-sensitive Chinese match)
            msg = str(gr.get("msg") or "")
            if gr.get("code") not in (500, 400, 403) or ("grant" not in msg.lower() and "授权" not in msg and "已" not in msg):
                # still continue if looks like duplicate; else raise
                if "已" not in msg and "存在" not in msg and "有效" not in msg:
                    raise RuntimeError(f"grant failed team {t['code']}: {gr}")
        tok = web_login(t["captain_email"])
        # small delay to avoid clock-skew on brand-new grants
        time.sleep(0.3)
        cr = create_team_via_web(tok, division_id, t["name"], t["short"], None)
        if cr.get("code") != 200:
            raise RuntimeError(f"create team failed {t['name']}: {cr}")
        tid = as_id((cr.get("data") or {}).get("teamId") or (cr.get("data") or {}).get("id") or extract_id(cr))
        t["team_id"] = tid
        ar = approve_team(admin_token, tid)
        if ar.get("code") != 200:
            raise RuntimeError(f"approve team failed {t['name']}: {ar}")
        team_ids.append(tid)
    return team_ids
'''
p.write_text(t[:start] + new_fn + t[end:], encoding="utf-8", newline="\n")
print("rewrote create_teams_flow")
import ast
ast.parse(p.read_text(encoding="utf-8"))
print("syntax ok")
