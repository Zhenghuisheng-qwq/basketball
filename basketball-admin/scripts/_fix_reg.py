from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
old='''def register_all_teams(web_tokens: dict[str, str], admin_token: str, division_id: str, teams: list[dict]) -> int:
    for t in teams:
        tok = web_tokens[t["captain_email"]]
        rr = register_team(tok, division_id, t["team_id"], t["player_ids"][:5])
        if rr.get("code") != 200:
            raise RuntimeError(f"register failed {t['name']}: {rr}")
    return approve_all_registrations(admin_token, division_id)'''
new='''def register_all_teams(web_tokens: dict[str, str], admin_token: str, division_id: str, teams: list[dict]) -> int:
    existing = req(
        f"{ADMIN}/basket/v4/registration-audit/list?divisionId={division_id}&pageNum=1&pageSize=100",
        token=admin_token,
    )
    by_team = {as_id(r.get("teamId")): r for r in (existing.get("rows") or [])}
    for t in teams:
        st = (by_team.get(as_id(t["team_id"])) or {}).get("registrationStatus")
        if st in ("APPROVED", "PENDING_REVIEW"):
            continue
        tok = web_tokens[t["captain_email"]]
        rr = register_team(tok, division_id, t["team_id"], t["player_ids"][:5])
        if rr.get("code") != 200:
            raise RuntimeError(f"register failed {t['name']}: {rr}")
    return approve_all_registrations(admin_token, division_id)'''
if old not in t:
    raise SystemExit("register_all_teams not found")
p.write_text(t.replace(old,new), encoding="utf-8", newline="\n")
print("register idempotent")
