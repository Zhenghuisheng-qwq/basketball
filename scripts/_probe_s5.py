import pymysql
c=pymysql.connect(host="111.229.241.29",user="root",password="zlzcnpa@@qaq",database="basketball",charset="utf8mb4",cursorclass=pymysql.cursors.DictCursor)
cur=c.cursor()
cur.execute("SELECT id, platform_code, game_mode, lineup_min_count, lineup_max_count, max_team_count, registration_start_time, registration_end_time, division_status FROM basket_division WHERE id=174")
print(cur.fetchone())
cur.execute("SELECT season_name FROM basket_season WHERE season_name IN ('S12','S6') AND series_id=2")
print("S12", cur.fetchall())
c.close()
