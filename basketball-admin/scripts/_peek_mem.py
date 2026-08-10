import pymysql
from full_smoke_common import ID_BASE
c=pymysql.connect(host="192.168.31.48",port=3306,user="root",password="123456",database="basketball",cursorclass=pymysql.cursors.DictCursor)
cur=c.cursor()
cur.execute("SELECT id, team_name, current_member_count FROM basket_team WHERE division_id=142 ORDER BY id")
teams=cur.fetchall(); print("teams", teams)
for t in teams:
    cur.execute("SELECT player_id, membership_status, is_current, role_type FROM basket_team_member WHERE team_id=%s AND is_delete=0", (t["id"],))
    print(t["team_name"], cur.fetchall())
c.close()
# peek populate_members and register_all_teams
from pathlib import Path
t=Path("full_smoke_common.py").read_text(encoding="utf-8")
for name in ("def populate_members", "def register_all_teams", "def register_team"):
    i=t.find(name)
    print("====", name, "====")
    print(t[i:i+700])
