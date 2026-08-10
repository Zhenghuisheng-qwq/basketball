#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply CNPA V4 Round1+Round2 SQL scripts to dev database."""
from __future__ import annotations

import re
import sys
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
SQL_FILES = [
    ROOT / "docs/sql/cnpa-v4-r1-ddl.sql",
    ROOT / "docs/sql/cnpa-v4-r1-seed.sql",
    ROOT / "docs/sql/cnpa-v4-round2-menu.sql",
]

CONN = dict(
    host="192.168.31.48",
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
)


def split_sql(content: str) -> list[str]:
    """Split SQL file into executable statements (skip comments-only blocks)."""
    # Remove block comments
    content = re.sub(r"/\*.*?\*/", "", content, flags=re.S)
    statements: list[str] = []
    buf: list[str] = []
    for line in content.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("--"):
            continue
        buf.append(line)
        if stripped.endswith(";"):
            stmt = "\n".join(buf).strip()
            if stmt:
                statements.append(stmt)
            buf = []
    if buf:
        stmt = "\n".join(buf).strip()
        if stmt:
            statements.append(stmt)
    return statements


def main() -> int:
    conn = pymysql.connect(**CONN)
    cur = conn.cursor()
    total_ok = 0
    try:
        for sql_file in SQL_FILES:
            print(f"\n=== Applying {sql_file.relative_to(ROOT)} ===")
            content = sql_file.read_text(encoding="utf-8")
            for stmt in split_sql(content):
                preview = re.sub(r"\s+", " ", stmt)[:100]
                try:
                    cur.execute(stmt)
                    conn.commit()
                    total_ok += 1
                    print(f"OK: {preview}...")
                except Exception as exc:
                    print(f"ERR: {preview}...")
                    print(f"     {exc}")
                    conn.rollback()
                    return 1
        print(f"\nDone. {total_ok} statements applied.")
        return 0
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
