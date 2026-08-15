import json
import urllib.error
import urllib.request

import pymysql

conn = pymysql.connect(
    host="192.168.31.48",
    port=3306,
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
)
cur = conn.cursor()
cur.execute(
    "SELECT CAST(id AS CHAR) AS mid, email, email_verified, account_status, "
    "CHAR_LENGTH(password_hash) cl FROM basket_member_account "
    "WHERE email IN (%s,%s) AND is_delete=0",
    ("fs_aops_aops0101@cnpa.smoke", "fs_ad2_a0101@cnpa.smoke"),
)
print("DB", cur.fetchall())
conn.close()

for email in [
    "fs_aops_aops0101@cnpa.smoke",
    "fs_ad2_a0101@cnpa.smoke",
    "fs_ad3_a0101@cnpa.smoke",
]:
    body = json.dumps({"email": email, "password": "111111aa"}).encode()
    req = urllib.request.Request(
        "http://127.0.0.1:8881/basket/member/auth/login/password",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            d = json.loads(r.read().decode())
            print(email, d.get("code"), d.get("msg"))
    except urllib.error.HTTPError as e:
        print(email, "HTTP", e.code, e.read()[:200])
