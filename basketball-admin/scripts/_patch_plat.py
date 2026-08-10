from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
t2 = t.replace(
    "plat_id = ID_BASE + 9_000_000 + slot * 10 + pidx",
    "plat_id = ID_BASE + 50_000_000 + int(slot) * 10 + pidx",
)
t2 = t2.replace(
    "ensure_platform_accounts(cur, player_id, game_id, now, slot=offset // 1000 + ti * 10 + pi)",
    "ensure_platform_accounts(cur, player_id, game_id, now, slot=offset + ti * 20 + pi)",
)
if t2 == t:
    raise SystemExit("no changes applied")
p.write_text(t2, encoding="utf-8", newline="\n")
print("patched ok")
import pymysql
c = pymysql.connect(host="192.168.31.48", port=3306, user="root", password="123456", database="basketball", cursorclass=pymysql.cursors.DictCursor)
cur = c.cursor()
cur.execute("SELECT COUNT(*) AS c FROM basket_member_account WHERE email LIKE %s", ("fs_%@cnpa.smoke",))
print("smoke_members", cur.fetchone())
c.close()
