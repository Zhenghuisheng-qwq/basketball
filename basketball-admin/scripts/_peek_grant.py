import pymysql
from full_smoke_common import admin_login, req, ADMIN, ID_BASE
c=pymysql.connect(host="192.168.31.49",port=3306,user="root",password="123456",database="basketball",cursorclass=pymysql.cursors.DictCursor)
cur=c.cursor()
cur.execute("SELECT id, team_name, team_status, audit_status, division_id, captain_player_id FROM basket_team WHERE division_id=142 OR team_name LIKE 'FS_%'")
print("teams", cur.fetchall())
cur.execute("SELECT id, player_id, grant_status, valid_from, valid_to, team_id, active_unique_key FROM basket_captain_grant WHERE division_id=142 ORDER BY id")
for g in cur.fetchall():
    print("grant", g)
# charlie captain player
pid = ID_BASE + 10000 + 1_000_000 + 2*100 + 0
print("expected charlie pid", pid)
cur.execute("SELECT id, platform, display_game_id, account_status FROM basket_player_platform_account WHERE player_id=%s", (pid,))
print("platform", cur.fetchall())
c.close()
