from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
start=t.find("def apply_and_approve")
end=t.find("\ndef ", start+1)
new='''def apply_and_approve(player_token: str, captain_token: str, team_id: str) -> dict:
    ar = req(f"{WEB}/basket/team/{team_id}/join-application", "POST", {"message": MARKER}, player_token)
    app_id = None
    if ar.get("code") == 200:
        data = ar.get("data") or {}
        app_id = as_id(data.get("applicationId") or data.get("id") or extract_id(ar))
    else:
        # reuse pending application
        page = req(f"{WEB}/basket/team/{team_id}/join-application/page?pageNum=1&pageSize=50", token=captain_token)
        records = ((page.get("data") or {}).get("records") or page.get("rows") or [])
        # also try mine page
        if not records:
            mine = req(f"{WEB}/basket/team/join-application/mine/page?pageNum=1&pageSize=50", token=player_token)
            records = ((mine.get("data") or {}).get("records") or [])
        hit = next((r for r in records if as_id(r.get("teamId") or team_id) == as_id(team_id)), None)
        if not hit:
            return ar
        app_id = as_id(hit.get("applicationId") or hit.get("id"))
    return req(
        f"{WEB}/basket/team/join-application/{app_id}/process",
        "PUT",
        {"approve": True, "action": "APPROVE", "remark": MARKER},
        captain_token,
    )
'''
p.write_text(t[:start]+new+t[end:], encoding="utf-8", newline="\n")
import ast; ast.parse(p.read_text(encoding="utf-8")); print("ok")
