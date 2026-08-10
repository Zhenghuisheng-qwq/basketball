from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
start=t.find("def apply_and_approve")
end=t.find("\ndef ", start+1)
new='''def apply_and_approve(player_token: str, captain_token: str, team_id: str) -> dict:
    # resolve player member id from session if needed
    sess = req(f"{WEB}/basket/member/auth/session", token=player_token)
    member_id = None
    if sess.get("code") == 200:
        member_id = as_id(((sess.get("data") or {}).get("memberId") or (sess.get("data") or {}).get("id") or "0"))
        if member_id == "0":
            member_id = None

    ar = req(f"{WEB}/basket/team/{team_id}/join-application", "POST", {"message": MARKER}, player_token)
    app_id = None
    if ar.get("code") == 200:
        data = ar.get("data") or {}
        app_id = as_id(data.get("applicationId") or data.get("id") or extract_id(ar))
    else:
        page = req(f"{WEB}/basket/team/{team_id}/join-application/page?pageNum=1&pageSize=50", token=captain_token)
        records = ((page.get("data") or {}).get("records") or page.get("rows") or [])
        def pending(r):
            st = r.get("applicationStatus")
            return st in (0, "0", "PENDING", None) or r.get("canProcess") is True
        hits = [r for r in records if pending(r)]
        if member_id:
            hits = [r for r in hits if as_id(r.get("applicantMemberId") or r.get("memberId") or "0") in (member_id, "0") or True]
            # prefer exact member match
            exact = [r for r in hits if as_id(r.get("applicantMemberId") or r.get("memberId") or "0") == member_id]
            if exact:
                hits = exact
        if not hits:
            mine = req(f"{WEB}/basket/team/join-application/mine/page?pageNum=1&pageSize=50", token=player_token)
            records = ((mine.get("data") or {}).get("records") or [])
            hits = [r for r in records if as_id(r.get("teamId") or team_id) == as_id(team_id) and pending(r)]
        if not hits:
            return ar
        app_id = as_id(hits[0].get("applicationId") or hits[0].get("id"))
    return req(
        f"{WEB}/basket/team/join-application/{app_id}/process",
        "PUT",
        {"approve": True, "action": "APPROVE", "remark": MARKER},
        captain_token,
    )
'''
p.write_text(t[:start]+new+t[end:], encoding="utf-8", newline="\n")
# debug one pending list
from full_smoke_common import web_login, req, WEB, clear_login_locks
clear_login_locks(["fs_main_ech01@cnpa.smoke"])
cap=web_login("fs_main_ech01@cnpa.smoke")
page=req(f"{WEB}/basket/team/2086875174992449537/join-application/page?pageNum=1&pageSize=50", token=cap)
print(page)
