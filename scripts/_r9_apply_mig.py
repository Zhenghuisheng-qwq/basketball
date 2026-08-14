import pymysql
from pathlib import Path
sql = Path(r"d:\code\dz\basketball\scaffolding-v2\docs\sql\cnpa-v13-r9-final-migration.sql").read_text(encoding="utf-8")
c = pymysql.connect(host="192.168.31.48", user="root", password="123456", database="basketball_r9_fresh", charset="utf8mb4")
cur = c.cursor()
for stmt in [s.strip() for s in sql.split(";") if s.strip() and not s.strip().lstrip().startswith("--")]:
    if stmt.upper().startswith("SET "):
        cur.execute(stmt)
        continue
    cur.execute(stmt)
    c.commit()
    print("ok", stmt[:70].replace("\n", " "))
cur.execute(
    "SELECT table_name FROM information_schema.tables WHERE table_schema=%s AND table_name IN (%s,%s,%s,%s,%s)",
    ("basketball_r9_fresh", "basket_player_platform_account", "basket_player_platform_change_request",
     "basket_player_platform_identity_history", "basket_business_audit", "basket_business_audit_field"),
)
print("tables", [r[0] for r in cur.fetchall()])
c.close()