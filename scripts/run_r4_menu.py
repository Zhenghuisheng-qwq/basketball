#!/usr/bin/env python3
import re
from pathlib import Path
import pymysql

SQL_FILE = Path(__file__).resolve().parents[1] / "docs/sql/cnpa-v4-round4-menu.sql"
conn = pymysql.connect(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")
sql = "\n".join(l for l in SQL_FILE.read_text(encoding="utf-8").splitlines() if not l.strip().startswith("--"))
cur = conn.cursor()
for stmt in re.split(r";\s*\n", sql):
    s = stmt.strip()
    if s:
        cur.execute(s)
conn.commit()
cur.execute("SELECT menu_id, menu_name FROM sys_menu WHERE menu_id BETWEEN 2311 AND 2319")
print(cur.fetchall())
conn.close()
