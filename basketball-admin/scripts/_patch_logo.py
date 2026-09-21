from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
old = 'cr = create_team_via_web(tok, division_id, t["name"], t["short"], logos[i % len(logos)])'
new = 'cr = create_team_via_web(tok, division_id, t["name"], t["short"], None)  # logo file ownership blocks smoke accounts'
if old not in t:
    raise SystemExit("create_team_via_web call not found")
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
print("logo removed from create_teams_flow")
# cleanup grants/teams for MAIN division if any
import pymysql
c = pymysql.connect(host="192.168.31.49", port=3306, user="root", password="123456", database="basketball", autocommit=True, cursorclass=pymysql.cursors.DictCursor)
cur = c.cursor()
cur.execute("SELECT id, division_name, division_status FROM basket_division WHERE division_name LIKE %s", ("%FULL_SMOKE_20260811%",))
print("divisions", cur.fetchall())
cur.execute("SELECT id, grant_status, team_id FROM basket_captain_grant WHERE remark LIKE %s OR division_id IN (SELECT id FROM basket_division WHERE division_name LIKE %s)", ("%FULL_SMOKE%", "%FULL_SMOKE_20260811%"))
print("grants", cur.fetchall())
cur.execute("SELECT id, team_name, team_status FROM basket_team WHERE description LIKE %s OR team_name LIKE %s", ("%FULL_SMOKE%", "FS_%"))
print("teams", cur.fetchall())
c.close()
