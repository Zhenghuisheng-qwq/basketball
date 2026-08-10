from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
old = '''def create_teams_flow(admin_token: str, division_id: str, teams: list[dict], logos: list[str] | None = None) -> list[str]:
    team_ids: list[str] = []
    logos = logos or LOGOS
    for i, t in enumerate(teams):
        gr = create_captain_grant(admin_token, division_id, t["captain_player_id"])
        if gr.get("code") != 200:
            msg = str(gr.get("msg") or "")
            if "已有" not in msg and "存在" not in msg:
                raise RuntimeError(f"grant failed team {t['code']}: {gr}")
        tok = web_login(t["captain_email"])
        cr = create_team_via_web(tok, division_id, t["name"], t["short"], None)  # logo file ownership blocks smoke accounts
        if cr.get("code") != 200:
            raise RuntimeError(f"create team failed {t['name']}: {cr}")
        tid = as_id((cr.get("data") or {}).get("teamId") or extract_id(cr))
        ar = approve_team(admin_token, tid)
        if ar.get("code") != 200:'''
# read actual function to see exact text
i = t.find("def create_teams_flow")
print(repr(t[i:i+900]))
