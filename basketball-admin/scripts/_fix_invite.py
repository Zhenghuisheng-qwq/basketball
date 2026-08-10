from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
t=t.replace(
    'for k in ("id", "registrationId", "submissionId", "teamId", "changeId"):',
    'for k in ("invitationId", "applicationId", "registrationId", "submissionId", "changeId", "id", "teamId"):',
)
# harden invite_and_accept / apply_and_approve / populate_members
old='''def invite_and_accept(captain_token: str, invitee_token: str, team_id: str, invitee_player_id: str) -> dict:
    ir = req(
        f"{WEB}/basket/team/{team_id}/invitation",
        "POST",
        {"inviteePlayerId": invitee_player_id, "message": MARKER},
        captain_token,
    )
    if ir.get("code") != 200:
        return ir
    inv_id = extract_id(ir)
    return req(
        f"{WEB}/basket/team/invitation/{inv_id}/respond",
        "PUT",
        {"accept": True},
        invitee_token,
    )'''
new='''def invite_and_accept(captain_token: str, invitee_token: str, team_id: str, invitee_player_id: str) -> dict:
    ir = req(
        f"{WEB}/basket/team/{team_id}/invitation",
        "POST",
        {"inviteePlayerId": invitee_player_id, "message": MARKER},
        captain_token,
    )
    if ir.get("code") != 200:
        # if pending already exists, accept via mine page
        if "已" in str(ir.get("msg") or "") or ir.get("code") == 400:
            mine = req(f"{WEB}/basket/team/invitation/mine/page?pageNum=1&pageSize=50", token=invitee_token)
            records = ((mine.get("data") or {}).get("records") or [])
            hit = next((r for r in records if as_id(r.get("teamId")) == as_id(team_id) and r.get("canRespond")), None)
            if not hit:
                return ir
            inv_id = as_id(hit.get("invitationId") or hit.get("id"))
            return req(f"{WEB}/basket/team/invitation/{inv_id}/respond", "PUT", {"accept": True, "action": "ACCEPT"}, invitee_token)
        return ir
    data = ir.get("data") or {}
    inv_id = as_id(data.get("invitationId") or data.get("id") or extract_id(ir))
    return req(
        f"{WEB}/basket/team/invitation/{inv_id}/respond",
        "PUT",
        {"accept": True, "action": "ACCEPT"},
        invitee_token,
    )'''
if old not in t:
    raise SystemExit("invite_and_accept block not found")
t=t.replace(old,new)
old2='''def populate_members(teams: list[dict], invite_until: int = 4, full_players: int = 5) -> None:
    for ti, t in enumerate(teams):
        cap_tok = web_login(t["captain_email"])
        tid = t["team_id"]
        for pi in range(1, full_players):
            p_email = t["emails"][pi]
            p_pid = t["player_ids"][pi]
            p_tok = web_login(p_email)
            if ti < invite_until:
                invite_and_accept(cap_tok, p_tok, tid, p_pid)
            else:
                apply_and_approve(p_tok, cap_tok, tid)'''
new2='''def populate_members(teams: list[dict], invite_until: int = 4, full_players: int = 5) -> None:
    for ti, t in enumerate(teams):
        cap_tok = web_login(t["captain_email"])
        tid = t["team_id"]
        for pi in range(1, full_players):
            p_email = t["emails"][pi]
            p_pid = t["player_ids"][pi]
            # already member?
            rows = db_query(
                "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND membership_status=1 AND is_delete=0",
                (int(tid), int(p_pid)),
            )
            if rows:
                continue
            p_tok = web_login(p_email)
            if ti < invite_until:
                r = invite_and_accept(cap_tok, p_tok, tid, p_pid)
            else:
                r = apply_and_approve(p_tok, cap_tok, tid)
            if r.get("code") != 200:
                raise RuntimeError(f"join failed {t['name']} player#{pi}: {r}")'''
if old2 not in t:
    raise SystemExit("populate_members not found")
t=t.replace(old2,new2)
# fix apply_and_approve similarly
old3='''def apply_and_approve(player_token: str, captain_token: str, team_id: str) -> dict:
    ar = req(f"{WEB}/basket/team/{team_id}/join-application", "POST", {"message": MARKER}, player_token)
    if ar.get("code") != 200:
        return ar
    app_id = extract_id(ar)
    return req(
        f"{WEB}/basket/team/join-application/{app_id}/process",
        "PUT",
        {"approve": True, "remark": MARKER},
        captain_token,
    )'''
new3='''def apply_and_approve(player_token: str, captain_token: str, team_id: str) -> dict:
    ar = req(f"{WEB}/basket/team/{team_id}/join-application", "POST", {"message": MARKER}, player_token)
    if ar.get("code") != 200:
        return ar
    data = ar.get("data") or {}
    app_id = as_id(data.get("applicationId") or data.get("id") or extract_id(ar))
    return req(
        f"{WEB}/basket/team/join-application/{app_id}/process",
        "PUT",
        {"approve": True, "action": "APPROVE", "remark": MARKER},
        captain_token,
    )'''
if old3 not in t:
    raise SystemExit("apply_and_approve not found")
t=t.replace(old3,new3)
p.write_text(t, encoding="utf-8", newline="\n")
import ast; ast.parse(p.read_text(encoding="utf-8"))
print("patched ok")
