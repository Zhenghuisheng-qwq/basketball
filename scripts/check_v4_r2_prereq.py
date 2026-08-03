#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""CNPA V4 Round2 prerequisite DB checks."""
import pymysql

conn = pymysql.connect(
    host="192.168.31.47",
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
)
cur = conn.cursor()

queries = [
    "SHOW TABLES LIKE 'basket_series'",
    "SHOW TABLES LIKE 'basket_season'",
    "SHOW TABLES LIKE 'basket_division'",
    "SELECT id, series_code, series_name, enabled FROM basket_series WHERE is_delete=0 ORDER BY sort_order, id",
    "SELECT id, season_year, season_name, display_title, publish_status, season_status FROM basket_season WHERE is_delete=0 LIMIT 20",
    "SELECT id, season_id, platform_code, division_name, division_status FROM basket_division WHERE is_delete=0 LIMIT 20",
    """SELECT job_id, job_name, invoke_target, status FROM sys_job
       WHERE invoke_target LIKE '%scheduleAutoGenerateJob%'
          OR invoke_target LIKE '%matchStatusAutoFinishJob%'""",
    """SELECT menu_id, menu_name, parent_id, path, component, visible, status, perms
       FROM sys_menu
       WHERE component LIKE '%cnpa/event%'
          OR component LIKE '%tournament%'
          OR component LIKE '%competition%'
          OR path IN ('list','season','rule','stage','group','tournament','event')
          OR menu_name LIKE '%赛季%'
          OR menu_name LIKE '%赛事%'
       ORDER BY menu_id""",
]

for q in queries:
    print("===", q.replace("\n", " ")[:120])
    try:
        cur.execute(q)
        rows = cur.fetchall()
        if not rows:
            print("(empty)")
        for r in rows:
            print(r)
    except Exception as e:
        print("ERR", e)
    print()

conn.close()
