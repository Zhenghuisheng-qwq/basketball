# -*- coding: utf-8 -*-
import pymysql
c = pymysql.connect(host="192.168.31.48", user="root", password="123456", database="basketball_r9_fresh", charset="utf8mb4")
cur = c.cursor()
stmts = [
    "ALTER TABLE basket_match_submission ADD COLUMN home_score INT DEFAULT NULL COMMENT 'home score' AFTER version_no",
    "ALTER TABLE basket_match_submission ADD COLUMN away_score INT DEFAULT NULL COMMENT 'away score' AFTER home_score",
    "ALTER TABLE basket_match_submission ADD COLUMN submit_remark VARCHAR(512) DEFAULT NULL COMMENT 'submit remark' AFTER reject_reason",
]
for stmt in stmts:
    try:
        cur.execute(stmt)
        c.commit()
        print("OK", stmt[:70])
    except Exception as exc:
        print("SKIP", exc)
        c.rollback()
cur.execute("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='basketball_r9_fresh' AND TABLE_NAME='basket_match_submission' AND COLUMN_NAME IN ('home_score','away_score','submit_remark') ORDER BY COLUMN_NAME")
print("cols", cur.fetchall())
c.close()