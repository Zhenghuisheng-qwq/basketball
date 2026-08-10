#!/usr/bin/env python3
"""Migrate all tables/data from local MySQL to remote, preserving IDs and AUTO_INCREMENT.

Source: 192.168.31.48 / basketball
Target: 111.229.241.29 / basketball

Skips GENERATED columns on INSERT (e.g. pending_slot).
"""

from __future__ import annotations

import argparse
import sys
import time
from typing import Any

import pymysql
from pymysql.cursors import DictCursor

SOURCE = {
    "host": "192.168.31.48",
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


def connect(cfg: dict[str, Any], with_db: bool = True) -> pymysql.connections.Connection:
    kwargs: dict[str, Any] = {
        "host": cfg["host"],
        "port": cfg["port"],
        "user": cfg["user"],
        "password": cfg["password"],
        "charset": cfg["charset"],
        "cursorclass": DictCursor,
        "autocommit": False,
        "ssl_disabled": True,
    }
    if with_db:
        kwargs["database"] = cfg["database"]
    return pymysql.connect(**kwargs)


def quote_ident(name: str) -> str:
    return "`" + name.replace("`", "``") + "`"


def ensure_database(cfg: dict[str, Any]) -> None:
    conn = connect(cfg, with_db=False)
    try:
        with conn.cursor() as cur:
            cur.execute(
                f"CREATE DATABASE IF NOT EXISTS {quote_ident(cfg['database'])} "
                "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            )
        conn.commit()
    finally:
        conn.close()


def fetch_tables(conn: pymysql.connections.Connection, schema: str) -> list[str]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT TABLE_NAME AS name
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
            """,
            (schema,),
        )
        return [row["name"] for row in cur.fetchall()]


def fetch_views(conn: pymysql.connections.Connection, schema: str) -> list[tuple[str, str]]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT TABLE_NAME AS name
            FROM information_schema.VIEWS
            WHERE TABLE_SCHEMA = %s
            ORDER BY TABLE_NAME
            """,
            (schema,),
        )
        views = []
        for row in cur.fetchall():
            name = row["name"]
            cur.execute(f"SHOW CREATE VIEW {quote_ident(name)}")
            create_row = cur.fetchone()
            views.append((name, create_row["Create View"]))
        return views


def fetch_create_sql(conn: pymysql.connections.Connection, table: str) -> str:
    with conn.cursor() as cur:
        cur.execute(f"SHOW CREATE TABLE {quote_ident(table)}")
        row = cur.fetchone()
        return row["Create Table"]


def fetch_writable_columns(conn: pymysql.connections.Connection, table: str) -> list[str]:
    """Exclude GENERATED ALWAYS columns (VIRTUAL/STORED)."""
    with conn.cursor() as cur:
        cur.execute(f"SHOW COLUMNS FROM {quote_ident(table)}")
        cols = []
        for row in cur.fetchall():
            extra = (row.get("Extra") or "").upper()
            if "GENERATED" in extra:
                continue
            cols.append(row["Field"])
        return cols


def fetch_auto_increment(conn: pymysql.connections.Connection, schema: str, table: str) -> int | None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT AUTO_INCREMENT
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
            """,
            (schema, table),
        )
        row = cur.fetchone()
        if not row or row["AUTO_INCREMENT"] is None:
            return None
        return int(row["AUTO_INCREMENT"])


def migrate_table(
    src: pymysql.connections.Connection,
    dst: pymysql.connections.Connection,
    schema: str,
    table: str,
) -> tuple[int, float]:
    started = time.time()
    create_sql = fetch_create_sql(src, table)
    columns = fetch_writable_columns(src, table)
    auto_inc = fetch_auto_increment(src, schema, table)

    if not columns:
        raise RuntimeError(f"no writable columns for {table}")

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


def migrate_views(
    src: pymysql.connections.Connection,
    dst: pymysql.connections.Connection,
    schema: str,
) -> int:
    views = fetch_views(src, schema)
    with dst.cursor() as cur:
        for name, _ in views:
            cur.execute(f"DROP VIEW IF EXISTS {quote_ident(name)}")
        for name, create_sql in views:
            # SHOW CREATE VIEW may include DEFINER; keep as-is when possible
            cur.execute(create_sql)
    dst.commit()
    return len(views)


def main() -> int:
    parser = argparse.ArgumentParser(description="Migrate basketball DB preserving IDs")
    parser.add_argument("--yes", action="store_true", help="skip interactive confirm")
    args = parser.parse_args()

    print("=== CNPA DB migrate (preserve IDs, skip GENERATED cols) ===")
    print(f"Source : {SOURCE['user']}@{SOURCE['host']}:{SOURCE['port']}/{SOURCE['database']}")
    print(f"Target : {TARGET['user']}@{TARGET['host']}:{TARGET['port']}/{TARGET['database']}")
    print("WARNING: target tables will be dropped and recreated.")
    if not args.yes:
        ans = input("Type YES to continue: ").strip()
        if ans != "YES":
            print("Aborted.")
            return 1

    ensure_database(TARGET)
    print("Connecting...")
    src = connect(SOURCE)
    dst = connect(TARGET)

    try:
        with dst.cursor() as cur:
            cur.execute("SET NAMES utf8mb4")
            cur.execute("SET FOREIGN_KEY_CHECKS = 0")
            cur.execute("SET UNIQUE_CHECKS = 0")
            cur.execute("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO'")
        dst.commit()

        tables = fetch_tables(src, SOURCE["database"])
        print(f"Found {len(tables)} base tables.")

        summary: list[tuple[str, int, float]] = []
        for idx, table in enumerate(tables, start=1):
            count, elapsed = migrate_table(src, dst, SOURCE["database"], table)
            summary.append((table, count, elapsed))
            print(f"[{idx}/{len(tables)}] {table}: {count} rows ({elapsed:.1f}s)")

        try:
            view_count = migrate_views(src, dst, SOURCE["database"])
            print(f"Views migrated: {view_count}")
        except Exception as exc:
            print(f"WARNING: view migrate skipped/failed: {exc}", file=sys.stderr)

        with dst.cursor() as cur:
            cur.execute("SET FOREIGN_KEY_CHECKS = 1")
            cur.execute("SET UNIQUE_CHECKS = 1")
        dst.commit()

        total_rows = sum(c for _, c, _ in summary)
        print(f"\nMigration complete: {len(summary)} tables, {total_rows} rows total.")

        # Verify sampled tables: count + max(id) when id exists
        sample = [
            "basket_member_account",
            "basket_member_profile",
            "basket_member_profile_change",
            "basket_team",
            "basket_series",
            "basket_season",
            "basket_division",
            "basket_match",
        ]
        mismatch = 0
        with src.cursor() as s_cur, dst.cursor() as d_cur:
            for table in sample:
                if table not in {t for t, _, _ in summary}:
                    continue
                s_cur.execute(f"SELECT COUNT(*) AS c FROM {quote_ident(table)}")
                d_cur.execute(f"SELECT COUNT(*) AS c FROM {quote_ident(table)}")
                sc = s_cur.fetchone()["c"]
                dc = d_cur.fetchone()["c"]
                try:
                    s_cur.execute(f"SELECT COALESCE(MAX(id),0) AS m FROM {quote_ident(table)}")
                    d_cur.execute(f"SELECT COALESCE(MAX(id),0) AS m FROM {quote_ident(table)}")
                    sm = s_cur.fetchone()["m"]
                    dm = d_cur.fetchone()["m"]
                except Exception:
                    sm = dm = "-"
                ok = sc == dc and sm == dm
                if not ok:
                    mismatch += 1
                print(f"[{'OK' if ok else 'DIFF'}] {table}: count {sc}->{dc}, max(id) {sm}->{dm}")

        if mismatch:
            print(f"DONE with {mismatch} DIFF(s).", file=sys.stderr)
            return 2
        print("DONE. Sampled checks passed.")
        return 0
    finally:
        src.close()
        dst.close()


if __name__ == "__main__":
    raise SystemExit(main())