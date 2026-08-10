from pathlib import Path
from datetime import datetime, timedelta
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
old = '''def create_captain_grant(token: str, division_id: str, player_id: str, remark: str = MARKER) -> dict:
    return req(
        f"{ADMIN}/basket/v4/captain-grant",
        "POST",
        {"divisionId": division_id, "playerId": player_id, "remark": remark},
        token,
    )'''
new = '''def create_captain_grant(token: str, division_id: str, player_id: str, remark: str = MARKER) -> dict:
    # validFrom slightly in the past to avoid Admin/Web clock skew "尚未生效"
    vf = (datetime.now() - timedelta(hours=1)).strftime("%Y-%m-%d %H:%M:%S")
    return req(
        f"{ADMIN}/basket/v4/captain-grant",
        "POST",
        {"divisionId": division_id, "playerId": player_id, "remark": remark, "validFrom": vf},
        token,
    )'''
if old not in t:
    raise SystemExit("create_captain_grant not found")
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
# also fix existing charlie grant valid_from
import pymysql
c=pymysql.connect(host="192.168.31.48",port=3306,user="root",password="123456",database="basketball",autocommit=True)
cur=c.cursor()
cur.execute("UPDATE basket_captain_grant SET valid_from=DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE division_id=142 AND team_id IS NULL")
print("updated grants", cur.rowcount)
# make create_teams_flow skip teams that already exist by name in this division
t=p.read_text(encoding="utf-8")
idx=t.find("def create_teams_flow")
print("has create_teams_flow", idx>0)
