from full_smoke_common import web_login, req, WEB, clear_login_locks, db_query
team_id="2086875000714924034"
clear_login_locks(["fs_main_alp02@cnpa.smoke","fs_main_alp01@cnpa.smoke"])
pt=web_login("fs_main_alp02@cnpa.smoke")
mine=req(f"{WEB}/basket/team/invitation/mine/page?pageNum=1&pageSize=20", token=pt)
print("mine", mine)
rows=(mine.get("data") or {}).get("records") or (mine.get("data") or {}).get("list") or mine.get("rows") or []
if not rows and isinstance(mine.get("data"), list):
    rows=mine.get("data")
print("rows", rows)
for row in rows:
    inv_id=row.get("id") or row.get("invitationId")
    rr=req(f"{WEB}/basket/team/invitation/{inv_id}/respond","PUT",{"accept":True,"action":"ACCEPT"},pt)
    print("accept", inv_id, rr)
print("members", db_query("SELECT player_id, membership_status FROM basket_team_member WHERE team_id=%s AND is_delete=0",(int(team_id),)))
