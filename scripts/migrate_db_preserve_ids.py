#!/usr/bin/env python3
"""Migrate all tables and data from source MySQL to target, preserving IDs and AUTO_INCREMENT."""

from __future__ import annotations

import sys
import time
from typing import Any

import pymysql
from pymysql.cursors import DictCursor

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


def connect(cfg: dict[str, Any]) -> pymysql.connections.Connection:
    return pymysql.connect(
        host=cfg["host"],
        port=cfg["port"],
        user=cfg["user"],
        password=cfg["password"],
        database=cfg["database"],
        charset=cfg["charset"],
        cursorclass=DictCursor,
        autocommit=False,
    )


def fetch_tables(conn: pymysql.connections.Connection) -> list[str]:
    with conn.cursor() as cur:
        cur.execute("SHOW TABLES")
        key = f"Tables_in_{SOURCE['database']}"
        return [row[key] for row in cur.fetchall()]


def fetch_create_sql(conn: pymysql.connections.Connection, table: str) -> str:
    with conn.cursor() as cur:
        cur.execute(f"SHOW CREATE TABLE `{table}`")
        row = cur.fetchone()
        return row["Create Table"]


def fetch_columns(conn: pymysql.connections.Connection, table: str) -> list[str]:
    with conn.cursor() as cur:
        cur.execute(f"SHOW COLUMNS FROM `{table}`")
        return [row["Field"] for row in cur.fetchall()]


def fetch_auto_increment(conn: pymysql.connections.Connection, table: str) -> int | None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT AUTO_INCREMENT
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
            """,
            (SOURCE["database"], table),
        )
        row = cur.fetchone()
        if not row or row["AUTO_INCREMENT"] is None:
            return None
        return int(row["AUTO_INCREMENT"])


def quote_ident(name: str) -> str:
    return "`" + name.replace("`", "``") + "`"


def migrate_table(
    src: pymysql.connections.Connection,
    dst: pymysql.connections.Connection,
    table: str,
) -> tuple[int, float]:
    started = time.time()
    create_sql = fetch_create_sql(src, table)
    columns = fetch_columns(src, table)
    auto_inc = fetch_auto_increment(src, table)

    col_list = ", ".join(quote_ident(c) for c in columns)
    placeholders = ", ".join(["%s"] * len(columns))
    insert_sql = f"INSERT INTO {quote_ident(table)} ({col_list}) VALUES ({placeholders})"

    with dst.cursor() as dst_cur:
        dst_cur.execute(f"DROP TABLE IF EXISTS {quote_ident(table)}")
        dst_cur.execute(create_sql)

        with src.cursor() as src_cur:
            src_cur.execute(f"SELECT {col_list} FROM {quote_ident(table)}")
            total = 0
            while True:
                rows = src_cur.fetchmany(BATCH_SIZE)
                if not rows:
                    break
                values = [tuple(row[c] for c in columns) for row in rows]
                dst_cur.executemany(insert_sql, values)
                total += len(values)

        if auto_inc is not None:
            dst_cur.execute(
                f"ALTER TABLE {quote_ident(table)} AUTO_INCREMENT = %s",
                (auto_inc,),
            )

    dst.commit()
    return total, time.time() - started


def main() -> int:
    print("Connecting to source and target...")
    src = connect(SOURCE)
    dst = connect(TARGET)

    try:
        with dst.cursor() as cur:
            cur.execute("SET NAMES utf8mb4")
            cur.execute("SET FOREIGN_KEY_CHECKS = 0")
            cur.execute("SET UNIQUE_CHECKS = 0")
            cur.execute("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO'")
        dst.commit()

        tables = fetch_tables(src)
        print(f"Found {len(tables)} tables in source.")

        summary: list[tuple[str, int, float]] = []
        for idx, table in enumerate(tables, start=1):
            try:
                count, elapsed = migrate_table(src, dst, table)
                summary.append((table, count, elapsed))
                print(f"[{idx}/{len(tables)}] {table}: {count} rows ({elapsed:.1f}s)")
            except Exception as exc:
                dst.rollback()
                print(f"[{idx}/{len(tables)}] FAILED {table}: {exc}", file=sys.stderr)
                raise

        with dst.cursor() as cur:
            cur.execute("SET FOREIGN_KEY_CHECKS = 1")
            cur.execute("SET UNIQUE_CHECKS = 1")
        dst.commit()

        total_rows = sum(c for _, c, _ in summary)
        print(f"\nMigration complete: {len(summary)} tables, {total_rows} rows total.")

        # Quick verification sample
        with src.cursor() as s_cur, dst.cursor() as d_cur:
            for table, count, _ in summary[:5]:
                d_cur.execute(f"SELECT COUNT(*) AS c FROM {quote_ident(table)}")
                dst_count = d_cur.fetchone()["c"]
                if dst_count != count:
                    print(
                        f"WARNING: row count mismatch on {table}: source={count}, target={dst_count}",
                        file=sys.stderr,
                    )

        return 0
    finally:
        src.close()
        dst.close()


if __name__ == "__main__":
    raise SystemExit(main())
