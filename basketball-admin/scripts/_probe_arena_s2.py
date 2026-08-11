import pymysql
from datetime import datetime
host,pwd=("111.229.241.29","zlzcnpa@@qaq")
try:
    c=pymysql.connect(host=host,user="root",password=pwd,database="basketball",cursorclass=pymysql.cursors.DictCursor,connect_timeout=8,ssl_disabled=True)
    cur=c.cursor()
    cur.execute("""
SELECT d.id division_id, d.season_id, d.division_name, d.division_status,
       d.registration_start_time, d.registration_end_time, d.platform_code,
       se.display_title, ser.series_name, NOW() db_now
FROM basket_division d
JOIN basket_season se ON se.id=d.season_id
JOIN basket_series ser ON ser.id=se.series_id
WHERE ser.series_name LIKE %s OR se.display_title LIKE %s
ORDER BY d.id DESC LIMIT 20
""", ("%Arena%","%Arena%"))
    rows=cur.fetchall()
    print("rows", len(rows))
    for r in rows:
        print(r)
    if rows:
        did=rows[0]["division_id"]
        cur.execute("""
SELECT cg.id, cg.player_id, cg.team_id, cg.grant_status, t.team_name, t.audit_status, t.team_status
FROM basket_captain_grant cg
LEFT JOIN basket_team t ON t.id=cg.team_id
WHERE cg.division_id=%s AND cg.is_delete=0 AND t.team_name LIKE %s
LIMIT 10
""", (did, "%ZLZ%"))
        print("grants", cur.fetchall())
    c.close()
except Exception as e:
    print("ERR", type(e), e)
