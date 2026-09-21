import pymysql, json
from full_smoke_common import admin_login, req, ADMIN, sorted_stages
c=pymysql.connect(host="192.168.31.49",port=3306,user="root",password="123456",database="basketball",cursorclass=pymysql.cursors.DictCursor)
cur=c.cursor()
cur.execute("SELECT id, division_status FROM basket_division WHERE id=142")
print("div", cur.fetchone())
cur.execute("SELECT id, snapshot_status, locked FROM basket_format_snapshot WHERE division_id=142")
print("snap cols try")
try:
    print(cur.fetchall())
except Exception as e:
    print(e)
cur.execute("SHOW COLUMNS FROM basket_format_snapshot")
print([r["Field"] for r in cur.fetchall()])
cur.execute("SELECT * FROM basket_format_snapshot WHERE division_id=142")
rows=cur.fetchall()
for r in rows:
    print({k:r[k] for k in r if k in ("id","division_id","snapshot_status","lock_status","publish_status","is_locked","status") or "status" in k or "lock" in k})
c.close()
tok=admin_login()
snap=req(f"{ADMIN}/basket/v4/divisions/142/format-snapshot", token=tok)
print("api code", snap.get("code"), "keys", list((snap.get("data") or {}).keys())[:20])
print("stages", len(sorted_stages(snap.get("data") or {})))
