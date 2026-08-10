from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
# replace apply_and_approve signature to accept applicant_member_id
start=t.find("def apply_and_approve")
end=t.find("\ndef ", start+1)
new='''def apply_and_approve(player_token: str, captain_token: str, team_id: str, applicant_member_id: str | None = None) -> dict:
    ar = req(f"{WEB}/basket/team/{team_id}/join-application", "POST", {"message": MARKER}, player_token)
    if ar.get("code") == 200:
        data = ar.get("data") or {}
        app_id = as_id(data.get("applicationId") or data.get("id") or extract_id(ar))
    else:
        page = req(f"{WEB}/basket/team/{team_id}/join-application/page?pageNum=1&pageSize=50", token=captain_token)
        records = ((page.get("data") or {}).get("records") or [])
        hits = [r for r in records if r.get("canProcess") or r.get("applicationStatus") in (0, "0", "PENDING")]
        if applicant_member_id:
            hits = [r for r in hits if as_id(r.get("applicantMemberId") or "0") == as_id(applicant_member_id)]
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
t=t[:start]+new+t[end:]
# update populate_members call
t=t.replace(
    "r = apply_and_approve(p_tok, cap_tok, tid)",
    'r = apply_and_approve(p_tok, cap_tok, tid, t["member_ids"][pi])',
)
p.write_text(t, encoding="utf-8", newline="\n")
import ast; ast.parse(p.read_text(encoding="utf-8")); print("ok")
