#!/usr/bin/env python3
"""Execute CNPA dynamic format SQL migrations with idempotent error handling."""
from __future__ import annotations

import re
import sys
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")

SKIP_ERRORS = (
    "Duplicate column name",
    "Duplicate key name",
    "already exists",
    "Can't DROP",
    "check that column/key exists",
    "Multiple primary key",
)


def split_sql(content: str) -> list[str]:
    statements: list[str] = []
    buf: list[str] = []
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("--") or stripped.startswith("#"):
            continue
        buf.append(line)
        if stripped.endswith(";"):
            stmt = "\n".join(buf).strip()
            if stmt and stmt != ";":
                statements.append(stmt[:-1] if stmt.endswith(";") else stmt)
            buf = []
    tail = "\n".join(buf).strip()
    if tail:
        statements.append(tail.rstrip(";"))
    return statements


def execute_file(cur, path: Path) -> tuple[int, int, list[str]]:
    content = path.read_text(encoding="utf-8")
    statements = split_sql(content)
    ok, skipped, errors = 0, 0, []
    for stmt in statements:
        if not stmt.strip():
            continue
        try:
            cur.execute(stmt)
            ok += 1
        except pymysql.err.OperationalError as e:
            msg = str(e)
            if any(s in msg for s in SKIP_ERRORS):
                skipped += 1
            else:
                errors.append(f"{path.name}: {msg[:200]} | SQL: {stmt[:120]}...")
        except pymysql.err.ProgrammingError as e:
            msg = str(e)
            if any(s in msg for s in SKIP_ERRORS):
                skipped += 1
            else:
                errors.append(f"{path.name}: {msg[:200]} | SQL: {stmt[:120]}...")
        except pymysql.err.IntegrityError as e:
            msg = str(e)
            if "Duplicate entry" in msg:
                skipped += 1
            else:
                errors.append(f"{path.name}: {msg[:200]} | SQL: {stmt[:120]}...")
    return ok, skipped, errors


def main(files: list[str]) -> int:
    conn = pymysql.connect(**CONN)
    all_errors: list[str] = []
    try:
        with conn.cursor() as cur:
            for name in files:
                path = ROOT / "docs" / "sql" / name
                if not path.exists():
                    print(f"[MISSING] {name}")
                    all_errors.append(f"missing file: {name}")
                    continue
                ok, skipped, errors = execute_file(cur, path)
                print(f"[{name}] executed={ok} skipped={skipped} errors={len(errors)}")
                all_errors.extend(errors)
        conn.commit()
    finally:
        conn.close()
    if all_errors:
        print("\n=== ERRORS ===")
        for err in all_errors[:30]:
            print(err)
        return 1
    return 0


if __name__ == "__main__":
    default_files = [
        "cnpa-dynamic-format-r1-migration.sql",
        "cnpa-dynamic-format-r1-seed.sql",
        "cnpa-dynamic-format-r2-migration.sql",
        "cnpa-dynamic-format-r3-migration.sql",
        "cnpa-dynamic-format-r3-permission.sql",
        "cnpa-dynamic-format-r4-migration.sql",
        "cnpa-dynamic-format-r4-permission.sql",
        "cnpa-dynamic-format-r4-test-data.sql",
    ]
    files = sys.argv[1:] if len(sys.argv) > 1 else default_files
    raise SystemExit(main(files))
