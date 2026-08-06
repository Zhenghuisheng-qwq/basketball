#!/usr/bin/env python3
"""Complete PERSONAL E2E on division 39."""
import json, os, urllib.request, pymysql
from pathlib import Path

for line in Path("scripts/.env.v2.local").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        os.environ[k.strip()] = v.strip()

web = "http://127.0.0.1:8881"
pwd = "111111aa"
admin = json.loads(urllib.request.urlopen(urllib.request.Request(
    "http://127.0.0.1:8080/login",
    data=json.dumps({"username": "admin", "password": "admin123"}).encode(),
    headers={"Content-Type": "application/json"}, method="POST")).read())["token"]
div = 39

def api(base, method, path, body=None, token=None):
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body else None,
                                 headers={"Content-Type": "application/json", **({"Authorization": f"Bearer {token}"} if token else {})},
                                 method=method)
    return json.loads(urllib.request.urlopen(req).read())

for pid in [13, 14, 15, 16, 17, 18]:
    r = api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/personal-registrations/{pid}/approve",
            {"auditRemark": "E2E"}, admin)
    print("approve", pid, r.get("code"))

batch = api("http://127.0.0.1:8080", "POST", "/basket/v4/admin/personal-draw-batches",
            {"divisionId": div, "batchCode": "CNPA_V2_FINAL_E2E_20260806_PERSONAL_DRAW",
             "batchName": "E2E Draw", "resultSource": "MANUAL"}, admin)
bid = batch.get("data")
print("batch", bid)
t1 = api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/personal-draw-batches/{bid}/teams",
         {"temporaryTeamCode": "T1", "temporaryTeamName": "Alpha", "expectedMemberCount": 3}, admin).get("data")
t2 = api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/personal-draw-batches/{bid}/teams",
         {"temporaryTeamCode": "T2", "temporaryTeamName": "Beta", "expectedMemberCount": 3}, admin).get("data")
regs = [13, 14, 15, 16, 17, 18]
for i, r in enumerate(regs[:3]):
    api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/personal-draw-batches/{bid}/teams/{t1}/assignments",
        {"personalRegistrationId": r, "assignmentRole": "CAPTAIN" if i == 0 else "MEMBER", "sortOrder": i + 1}, admin)
for i, r in enumerate(regs[3:6]):
    api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/personal-draw-batches/{bid}/teams/{t2}/assignments",
        {"personalRegistrationId": r, "assignmentRole": "CAPTAIN" if i == 0 else "MEMBER", "sortOrder": i + 1}, admin)
print("confirm", api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/personal-draw-batches/{bid}/confirm",
                   {"unassignedPolicy": "IGNORE"}, admin).get("code"))

conn = pymysql.connect(host=os.environ["CNPA_DB_HOST"], user=os.environ["CNPA_DB_USER"],
                       password=os.environ["CNPA_DB_PASSWORD"], database=os.environ["CNPA_DB_NAME"],
                       charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor)
cur = conn.cursor()
cur.execute("SELECT id, captain_user_id FROM basket_personal_draw_team WHERE draw_batch_id=%s AND is_delete=0", (bid,))
for dt in cur.fetchall():
    cur.execute("SELECT email FROM basket_member_account WHERE id=%s", (dt["captain_user_id"],))
    email = cur.fetchone()["email"]
    tok = api(web, "POST", "/basket/member/auth/login/password", {"email": email, "password": pwd})["data"]["token"]
    cr = api(web, "POST", f"/basket/v4/player/personal-formation/tasks/{dt['id']}/create-team",
             {"teamName": f"E2E-P-{dt['id']}", "shortName": "EP", "publicStatus": 0}, tok)
    tid = (cr.get("data") or {}).get("teamId")
    print("create", dt["id"], email, cr.get("code"), tid)
    if tid:
        api(web, "POST", "/basket/v4/player/personal-formation/bind-team",
            {"drawTeamId": dt["id"], "longTermTeamId": int(tid)}, tok)
        cfm = api(web, "POST", f"/basket/v4/player/personal-formation/tasks/{dt['id']}/confirm", None, tok)
        print("confirm", dt["id"], cfm.get("data"), cfm.get("code"))

stage_id = 89
api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/stages/{stage_id}/initialize-teams", None, admin)
api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/stages/{stage_id}/generate-schedule", None, admin)
api("http://127.0.0.1:8080", "POST", f"/basket/v4/admin/stages/{stage_id}/publish-schedule", None, admin)
cur.execute("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=39 AND is_delete=0")
print("eventTeam", cur.fetchone())
cur.execute("SELECT id FROM basket_match WHERE division_id=39 AND is_delete=0")
print("matches", cur.fetchall())
