#!/usr/bin/env python3
"""Run CNPA data-check SQL files and print results."""
from __future__ import annotations

import sys
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")


def split_checks(content: str) -> list[tuple[str, str]]:
    checks: list[tuple[str, str]] = []
    current_name = "check"
    buf: list[str] = []
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("-- ") and not stripped.startswith("-- ="):
            if buf:
                checks.append((current_name, "\n".join(buf).strip()))
                buf = []
            current_name = stripped[3:].strip()
            continue
        if stripped.startswith("--") or not stripped:
            continue
        buf.append(line)
        if stripped.endswith(";"):
            checks.append((current_name, "\n".join(buf).strip().rstrip(";")))
            buf = []
    if buf:
        checks.append((current_name, "\n".join(buf).strip().rstrip(";")))
    return checks


def main(path: str) -> int:
    sql_path = ROOT / "docs" / "sql" / path
    content = sql_path.read_text(encoding="utf-8")
    checks = split_checks(content)
    conn = pymysql.connect(**CONN)
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            for name, sql in checks:
                if not sql.upper().startswith("SELECT"):
                    continue
                try:
                    cur.execute(sql)
                    rows = cur.fetchall()
                    count = len(rows)
                    status = "OK" if count == 0 else "ANOMALY"
                    print(f"[{status}] {name}: rows={count}")
                    if count and count <= 5:
                        for row in rows:
                            print(f"  {row}")
                except Exception as e:
                    print(f"[ERROR] {name}: {e}")
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "cnpa-dynamic-format-r4-data-check.sql"
    raise SystemExit(main(target))
