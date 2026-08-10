from full_smoke_common import web_login, req, WEB, clear_login_locks, db_query
# try invite one player manually
team_id="2086875000714924034"
cap=web_login("fs_main_alp01@cnpa.smoke")
pid="2099110811001010001"
clear_login_locks(["fs_main_alp02@cnpa.smoke"])
pt=web_login("fs_main_alp02@cnpa.smoke")
ir=req(f"{WEB}/basket/team/{team_id}/invitation","POST",{"inviteePlayerId":pid,"message":"test"},cap)
print("invite", ir)
if ir.get("code")==200:
    data=ir.get("data") or {}
    inv_id=data.get("id") or data.get("invitationId")
    print("inv_id", inv_id, "data keys", list(data.keys()) if isinstance(data,dict) else data)
    for body in ({"accept":True},{"accepted":True},{"action":"ACCEPT"},{"response":"ACCEPT"}):
        rr=req(f"{WEB}/basket/team/invitation/{inv_id}/respond","PUT",body,pt)
        print("respond", body, rr.get("code"), rr.get("msg"))
        if rr.get("code")==200:
            break
print("members", db_query("SELECT player_id, membership_status FROM basket_team_member WHERE team_id=%s AND is_delete=0",(int(team_id),)))
