#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply CNPA V4 Round5 SQL patch."""
from __future__ import annotations

import sys
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")
PATCH_STATEMENTS = [
    "ALTER TABLE basket_match_submission ADD COLUMN home_score INT DEFAULT NULL COMMENT 'home' AFTER version_no",
    "ALTER TABLE basket_match_submission ADD COLUMN away_score INT DEFAULT NULL COMMENT 'away' AFTER home_score",
    "ALTER TABLE basket_match_submission ADD COLUMN submit_remark VARCHAR(512) DEFAULT NULL COMMENT 'remark' AFTER reject_reason",
]
MENU = ROOT / "docs/sql/cnpa-v4-round5-menu.sql"


def exec_stmt(cur, stmt: str) -> None:
    try:
        cur.execute(stmt)
        print(f"OK: {stmt[:70]}...")
    except pymysql.err.OperationalError as exc:
        if exc.args[0] == 1060:
            print(f"SKIP duplicate: {stmt[:50]}...")
        else:
            raise


def run_menu(path: Path) -> None:
    sql = path.read_text(encoding="utf-8")
    conn = pymysql.connect(**CONN)
    try:
        with conn.cursor() as cur:
            for raw in sql.split(";"):
                stmt = raw.strip()
                if not stmt or stmt.startswith("--"):
                    continue
                cur.execute(stmt)
        conn.commit()
        print(f"OK: {path.name}")
    finally:
        conn.close()


def main() -> int:
    conn = pymysql.connect(**CONN)
    try:
        with conn.cursor() as cur:
            for stmt in PATCH_STATEMENTS:
                exec_stmt(cur, stmt)
        conn.commit()
    finally:
        conn.close()
    run_menu(MENU)
    return 0


if __name__ == "__main__":
    sys.exit(main())
