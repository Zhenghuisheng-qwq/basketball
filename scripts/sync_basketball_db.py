#!/usr/bin/env python3
"""One-off full database sync preserving table structure, data and IDs."""

from __future__ import annotations

import sys
from typing import Any

import pymysql
from pymysql.connections import Connection

SOURCE = {
    "host": "192.168.31.47",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "basketball",
    "charset": "utf8mb4",
}

TARGET = {
    "host": "111.229.241.29",
    "port": 3306,
    "user": "root",
    "password": "zlzcnpa@@qaq",
    "database": "basketball",
    "charset": "utf8mb4",
}

BATCH_SIZE = 500


def connect(cfg: dict[str, Any]) -> Connection:
    return pymysql.connect(
        host=cfg["host"],
        port=cfg["port"],
        user=cfg["user"],
        password=cfg["password"],
        database=cfg["database"],
        charset=cfg["charset"],
        connect_timeout=30,
        read_timeout=600,
        write_timeout=600,
        autocommit=False,
    )


def list_tables(conn: Connection) -> list[str]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """
        )
        return [row[0] for row in cur.fetchall()]


def recreate_target_schema(src: Connection, tgt: Connection, tables: list[str]) -> None:
    with tgt.cursor() as cur:
        cur.execute("SET FOREIGN_KEY_CHECKS=0")
        cur.execute("SET UNIQUE_CHECKS=0")
        cur.execute("SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO'")
        for table in tables:
            cur.execute(f"DROP TABLE IF EXISTS `{table}`")

    with src.cursor() as src_cur, tgt.cursor() as tgt_cur:
        for table in tables:
            src_cur.execute(f"SHOW CREATE TABLE `{table}`")
            create_sql = src_cur.fetchone()[1]
            tgt_cur.execute(create_sql)
    tgt.commit()


def copy_table_data(src: Connection, tgt: Connection, table: str) -> int:
    copied = 0
    with src.cursor() as src_cur, tgt.cursor() as tgt_cur:
        src_cur.execute(f"SELECT * FROM `{table}`")
        columns = [desc[0] for desc in src_cur.description]
        if not columns:
            return 0
        col_sql = ", ".join(f"`{col}`" for col in columns)
        placeholders = ", ".join(["%s"] * len(columns))
        insert_sql = f"INSERT INTO `{table}` ({col_sql}) VALUES ({placeholders})"

        while True:
            rows = src_cur.fetchmany(BATCH_SIZE)
            if not rows:
                break
            tgt_cur.executemany(insert_sql, rows)
            copied += len(rows)
    tgt.commit()
    return copied


def verify_counts(src: Connection, tgt: Connection, tables: list[str]) -> list[tuple[str, int, int]]:
    mismatches: list[tuple[str, int, int]] = []
    with src.cursor() as src_cur, tgt.cursor() as tgt_cur:
        for table in tables:
            src_cur.execute(f"SELECT COUNT(*) FROM `{table}`")
            tgt_cur.execute(f"SELECT COUNT(*) FROM `{table}`")
            src_count = src_cur.fetchone()[0]
            tgt_count = tgt_cur.fetchone()[0]
            if src_count != tgt_count:
                mismatches.append((table, src_count, tgt_count))
    return mismatches


def main() -> int:
    print(f"Source: {SOURCE['host']}:{SOURCE['port']}/{SOURCE['database']}")
    print(f"Target: {TARGET['host']}:{TARGET['port']}/{TARGET['database']}")

    src = connect(SOURCE)
    tgt = connect(TARGET)
    try:
        tables = list_tables(src)
        print(f"Tables to sync: {len(tables)}")
        if not tables:
            print("Source has no tables, abort.")
            return 1

        print("Recreating target schema...")
        recreate_target_schema(src, tgt, tables)

        total_rows = 0
        for index, table in enumerate(tables, start=1):
            rows = copy_table_data(src, tgt, table)
            total_rows += rows
            print(f"[{index}/{len(tables)}] {table}: {rows} rows")

        with tgt.cursor() as cur:
            cur.execute("SET FOREIGN_KEY_CHECKS=1")
            cur.execute("SET UNIQUE_CHECKS=1")
        tgt.commit()

        print("Verifying row counts...")
        mismatches = verify_counts(src, tgt, tables)
        if mismatches:
            print("Row count mismatches:")
            for table, src_count, tgt_count in mismatches:
                print(f"  {table}: source={src_count}, target={tgt_count}")
            return 2

        print(f"Sync completed successfully. Tables={len(tables)}, rows={total_rows}")
        return 0
    finally:
        src.close()
        tgt.close()


if __name__ == "__main__":
    sys.exit(main())
