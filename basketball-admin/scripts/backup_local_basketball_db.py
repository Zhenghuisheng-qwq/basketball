#!/usr/bin/env python3
"""
LOCAL DEVELOPMENT ONLY — 用 pymysql 做 basketball 全库逻辑备份（无需 mysqldump）。

用法：
  python backup_local_basketball_db.py
  python backup_local_basketball_db.py --out ../../backup/basketball_before_cleanup_20260811.sql

安全：仅允许 host=192.168.31.48 database=basketball
"""
from __future__ import annotations

import argparse
import os
import sys
from datetime import date, datetime, time, timedelta
from decimal import Decimal
from pathlib import Path

import pymysql
from pymysql.constants import FIELD_TYPE

ALLOWED_HOST = "192.168.31.48"
ALLOWED_DB = "basketball"

DB = dict(
    host=ALLOWED_HOST,
    port=3306,
    user="root",
    password="123456",
    database=ALLOWED_DB,
    charset="utf8mb4",
    cursorclass=pymysql.cursors.SSCursor,
    autocommit=True,
)


def sql_literal(val, field_type: int | None = None) -> str:
    if val is None:
        return "NULL"
    if isinstance(val, bool):
        return "1" if val else "0"
    if isinstance(val, int) and not isinstance(val, bool):
        return str(val)
    if isinstance(val, float):
        return repr(val)
    if isinstance(val, Decimal):
        return format(val, "f")
    if isinstance(val, datetime):
        return "'" + val.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(val, date):
        return "'" + val.isoformat() + "'"
    if isinstance(val, time):
        return "'" + val.strftime("%H:%M:%S") + "'"
    if isinstance(val, timedelta):
        total = int(val.total_seconds())
        sign = "-" if total < 0 else ""
        total = abs(total)
        h, rem = divmod(total, 3600)
        m, s = divmod(rem, 60)
        return f"'{sign}{h:02d}:{m:02d}:{s:02d}'"
    if isinstance(val, (bytes, bytearray, memoryview)):
        b = bytes(val)
        # BIT / binary → hex
        if field_type in (FIELD_TYPE.BIT, FIELD_TYPE.BLOB, FIELD_TYPE.TINY_BLOB, FIELD_TYPE.MEDIUM_BLOB, FIELD_TYPE.LONG_BLOB):
            return "0x" + b.hex() if b else "''"
        try:
            s = b.decode("utf-8")
            return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"
        except UnicodeDecodeError:
            return "0x" + b.hex() if b else "''"
    s = str(val)
    return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'"


def main() -> int:
    parser = argparse.ArgumentParser(description="LOCAL ONLY: logical dump basketball DB via pymysql")
    default_out = Path(__file__).resolve().parents[2] / "backup" / "basketball_before_cleanup_20260811.sql"
    parser.add_argument("--out", default=str(default_out), help="output .sql path")
    args = parser.parse_args()

    if DB["host"] != ALLOWED_HOST or DB["database"] != ALLOWED_DB:
        print(f"BACKUP FAILED: refused host/db {DB['host']}/{DB['database']}", file=sys.stderr)
        return 2

    out_path = Path(args.out).resolve()
    out_path.parent.mkdir(parents=True, exist_ok=True)

    print(f"HOST={ALLOWED_HOST}")
    print(f"DB={ALLOWED_DB}")
    print(f"OUT={out_path}")

    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT DATABASE()")
            db_name = cur.fetchone()[0]
            if db_name != ALLOWED_DB:
                print(f"BACKUP FAILED: DATABASE()={db_name}", file=sys.stderr)
                return 2
            cur.execute("SHOW FULL TABLES WHERE Table_type='BASE TABLE'")
            tables = [r[0] for r in cur.fetchall()]
            tables.sort()

        tmp_path = out_path.with_suffix(out_path.suffix + ".tmp")
        row_total = 0
        with tmp_path.open("w", encoding="utf-8", newline="\n") as f:
            f.write("-- CNPA LOCAL DEVELOPMENT ONLY\n")
            f.write(f"-- Logical dump of `{ALLOWED_DB}` via pymysql\n")
            f.write(f"-- host={ALLOWED_HOST} generated_at={datetime.now().isoformat(timespec='seconds')}\n")
            f.write("SET NAMES utf8mb4;\n")
            f.write("SET FOREIGN_KEY_CHECKS=0;\n")
            f.write("SET UNIQUE_CHECKS=0;\n")
            f.write("SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';\n\n")

            for table in tables:
                print(f"dumping {table} ...", flush=True)
                with conn.cursor() as cur:
                    cur.execute(f"SHOW CREATE TABLE `{table}`")
                    create_sql = cur.fetchone()[1]
                    f.write(f"\n-- ----------------------------\n-- Table `{table}`\n-- ----------------------------\n")
                    f.write(f"DROP TABLE IF EXISTS `{table}`;\n")
                    f.write(create_sql + ";\n\n")

                    cur.execute(f"SELECT * FROM `{table}`")
                    cols = [d[0] for d in cur.description]
                    field_types = [d[1] for d in cur.description]
                    col_list = ",".join(f"`{c}`" for c in cols)
                    batch: list[str] = []
                    batch_size = 200

                    def flush_batch() -> None:
                        nonlocal batch, row_total
                        if not batch:
                            return
                        f.write(f"INSERT INTO `{table}` ({col_list}) VALUES\n")
                        f.write(",\n".join(batch))
                        f.write(";\n")
                        row_total += len(batch)
                        batch = []

                    while True:
                        rows = cur.fetchmany(500)
                        if not rows:
                            break
                        for row in rows:
                            values = ",".join(sql_literal(v, field_types[i]) for i, v in enumerate(row))
                            batch.append(f"({values})")
                            if len(batch) >= batch_size:
                                flush_batch()
                    flush_batch()
                    f.write("\n")

            f.write("SET FOREIGN_KEY_CHECKS=1;\n")
            f.write("SET UNIQUE_CHECKS=1;\n")
            f.write(f"-- DUMP COMPLETE tables={len(tables)} rows≈{row_total}\n")

        os.replace(tmp_path, out_path)
        size = out_path.stat().st_size
        print("BACKUP SUCCESS")
        print(f"- path: {out_path}")
        print(f"- tables: {len(tables)}")
        print(f"- approx_rows: {row_total}")
        print(f"- bytes: {size}")
        if size < 1024:
            print("BACKUP FAILED: dump file too small", file=sys.stderr)
            return 3
        return 0
    except Exception as e:
        print(f"BACKUP FAILED: {e}", file=sys.stderr)
        return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
