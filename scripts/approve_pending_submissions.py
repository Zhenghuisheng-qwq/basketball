#!/usr/bin/env python3
import json, os, urllib.request, urllib.error, pymysql
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
for line in (ROOT / "scripts" / ".env.v2.local").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip())

admin = "http://127.0.0.1:8080"
req = urllib.request.Request(admin + "/login", json.dumps({"username": "admin", "password": "admin123"}).encode(),
                             {"Content-Type": "application/json"}, method="POST")
with urllib.request.urlopen(req) as r:
    token = json.loads(r.read())["token"]

for sid in range(15, 25):
    req = urllib.request.Request(
        admin + f"/basket/v4/match-submission-audit/{sid}/approve",
        json.dumps({"auditRemark": "p0 bulk approve"}).encode(),
        {"Content-Type": "application/json", "Authorization": "Bearer " + token},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req) as r:
            p = json.loads(r.read())
            print(f"approve {sid}: code={p.get('code')} msg={str(p.get('msg',''))[:80]}")
    except urllib.error.HTTPError as e:
        print(f"approve {sid}: HTTP {e.code} {e.read().decode('utf-8','ignore')[:200]}")

req = urllib.request.Request(
    admin + "/basket/v4/admin/stages/5/check-completion",
    b"{}",
    {"Content-Type": "application/json", "Authorization": "Bearer " + token},
    method="POST",
)
with urllib.request.urlopen(req) as r:
    print("check-completion:", json.loads(r.read()).get("code"))

conn = pymysql.connect(
    host=os.environ["CNPA_DB_HOST"], user=os.environ["CNPA_DB_USER"],
    password=os.environ["CNPA_DB_PASSWORD"], database=os.environ["CNPA_DB_NAME"],
    charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor,
)
cur = conn.cursor()
cur.execute(
    "SELECT COUNT(*) AS c FROM basket_match WHERE division_id=10 AND stage_id=5 "
    "AND match_status='FINISHED' AND is_delete=0"
)
print("finished_matches:", cur.fetchone()["c"])
cur.execute(
    "SELECT event_team_id, rank_no, win_count, lose_count, point_for, point_against, standing_points "
    "FROM basket_standing WHERE division_id=10 AND stage_id=5 AND is_delete=0 ORDER BY rank_no"
)
print("standing:", cur.fetchall())
conn.close()
