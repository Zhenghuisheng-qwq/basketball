#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import pymysql

p = Path(r"d:\code\dz\basketball\scaffolding-v2\docs\sql\cnpa-v13-r9-final-data-check.sql")
raw = p.read_bytes()
if raw.startswith(b"\xff\xfe") or b"\x00" in raw[:40]:
    p.write_bytes(raw.decode("utf-16").encode("utf-8"))
    print("converted data-check")

conn = pymysql.connect(
    host="192.168.31.48",
    user="root",
    password="123456",
    database="basketball_r9_fresh",
    charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
)
cur = conn.cursor()


def run(sql, title):
    print("---", title)
    cur.execute(sql)
    rows = cur.fetchall()
    if not rows:
        print("  (0 rows)")
        return rows
    for row in rows:
        print(" ", row)
    return rows


run(
    """
SELECT table_name FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN (
  'basket_division_roster_version','basket_division_roster_entry',
  'basket_stage_pairing_execution','basket_stage_pairing_entry',
  'basket_stage_publish_record','basket_match_series','basket_series')
ORDER BY table_name
""",
    "required tables",
)

run(
    """
SELECT template_code, template_type, default_registration_mode, enabled, is_default
FROM basket_format_template
WHERE template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND version_no=1 AND is_delete=0
ORDER BY template_code
""",
    "STANDARD_V1",
)

run(
    """
SELECT t.template_code, s.stage_code, s.competition_format, s.pairing_rule, s.advancement_type,
       s.best_of, s.semifinal_best_of, s.final_best_of, s.organization_mode, s.grouping_mode
FROM basket_format_template t
JOIN basket_format_template_stage s ON s.template_id=t.id AND s.is_delete=0
WHERE t.template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND t.version_no=1 AND t.is_delete=0
ORDER BY t.template_code, s.stage_order
""",
    "STANDARD graph+BO",
)

run(
    """
SELECT COUNT(*) AS cnt FROM basket_format_template t
JOIN basket_format_template_stage s ON s.template_id=t.id AND s.is_delete=0
WHERE t.template_code='OPEN_STANDARD_V1'
  AND (s.stage_code LIKE '%FORMATION%' OR s.stage_type='FORMATION')
""",
    "OPEN formation stage",
)

run(
    """
SELECT menu_id, menu_name, parent_id, order_num, path FROM sys_menu
WHERE menu_id IN (2152,2280,2301,2350,2351,2311,2352)
ORDER BY parent_id, order_num, menu_id
""",
    "R8 menus",
)

run(
    """
SELECT table_name, column_name, is_nullable FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name IN ('basket_match_series','basket_match')
  AND column_name IN ('home_event_team_id','away_event_team_id')
ORDER BY table_name, column_name
""",
    "nullable teams",
)

run(
    """
SELECT routine_name FROM information_schema.routines
WHERE routine_schema=DATABASE() AND routine_name LIKE 'cnpa_r7%'
""",
    "reconcile procs",
)

run(
    """
SELECT division_id, COUNT(*) c FROM basket_division_roster_version
WHERE is_delete=0 AND is_current=1 GROUP BY division_id HAVING COUNT(*)>1
""",
    "P0 dup roster",
)
run(
    """
SELECT stage_id, COUNT(*) c FROM basket_stage_pairing_execution
WHERE is_delete=0 AND is_current=1 GROUP BY stage_id HAVING COUNT(*)>1
""",
    "P0 dup pairing",
)
run(
    """
SELECT id FROM basket_schedule_batch
WHERE is_delete=0 AND publish_status='PUBLISHED' AND structure_status<>'ACTIVE'
""",
    "P0 published stale",
)
run("SELECT COUNT(*) AS divisions FROM basket_division", "divisions")
run("SELECT series_code, series_name FROM basket_series WHERE is_delete=0 ORDER BY sort_order", "series")

cur.close()
conn.close()
