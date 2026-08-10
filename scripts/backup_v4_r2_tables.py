#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Backup V4 round2 related tables before smoke acceptance."""
from __future__ import annotations

import datetime
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
BACKUP_DIR = ROOT / "docs" / "sql" / "backups"
TABLES = [
    "basket_series",
    "basket_season",
    "basket_division",
    "basket_captain_grant",
    "basket_registration",
    "basket_registration_player",
    "basket_registration_audit",
    "basket_event_team",
    "basket_event_player",
    "basket_schedule_batch",
    "basket_match_submission",
    "basket_match_submission_team_stat",
    "basket_match_submission_player_stat",
    "basket_match_submission_material",
    "basket_standing",
    "basket_team_stat",
    "basket_player_stat",
    "basket_rank",
    "sys_menu",
    "sys_role_menu",
]

CONN = dict(
    host="192.168.31.48",
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
)


def esc(val):
    if val is None:
        return "NULL"
    if isinstance(val, (int, float)):
        return str(val)
    s = str(val).replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


def backup_table(cur, table: str, out) -> int:
    cur.execute(f"SHOW TABLES LIKE %s", (table,))
    if not cur.fetchone():
        out.write(f"-- table {table} not exists\n")
        return 0
    cur.execute(f"SELECT * FROM `{table}`")
    rows = cur.fetchall()
    if not rows:
        out.write(f"-- table {table}: 0 rows\n")
        return 0
    cols = [d[0] for d in cur.description]
    out.write(f"\n-- backup {table}: {len(rows)} rows\n")
    for row in rows:
        vals = ", ".join(esc(v) for v in row)
        out.write(f"INSERT INTO `{table}` ({', '.join('`' + c + '`' for c in cols)}) VALUES ({vals});\n")
    return len(rows)


def main() -> None:
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    path = BACKUP_DIR / f"backup-cnpa-v4-r2-smoke-{ts}.sql"
    conn = pymysql.connect(**CONN)
    cur = conn.cursor()
    total = 0
    with path.open("w", encoding="utf-8") as out:
        out.write("-- CNPA V4 Round2 smoke backup\n")
        out.write(f"-- generated at {ts}\n")
        out.write("SET NAMES utf8mb4;\n")
        for table in TABLES:
            total += backup_table(cur, table, out)
    cur.close()
    conn.close()
    print(f"backup written: {path} ({total} rows)")


if __name__ == "__main__":
    main()
