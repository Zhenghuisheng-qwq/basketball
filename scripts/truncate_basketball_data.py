#!/usr/bin/env python3
"""Truncate all basket_* tables in the basketball database."""

import os
import sys

import pymysql

HOST = os.environ.get("BASKETBALL_DB_HOST", "192.168.31.47")
PORT = int(os.environ.get("BASKETBALL_DB_PORT", "3306"))
USER = os.environ.get("BASKETBALL_DB_USER", "root")
PASSWORD = os.environ.get("BASKETBALL_DB_PASSWORD", "123456")
DATABASE = os.environ.get("BASKETBALL_DB_NAME", "basketball")


def main() -> int:
    conn = pymysql.connect(
        host=HOST,
        port=PORT,
        user=USER,
        password=PASSWORD,
        database=DATABASE,
        charset="utf8mb4",
        autocommit=True,
    )
    try:
        with conn.cursor() as cur:
            cur.execute("SET FOREIGN_KEY_CHECKS = 0")
            cur.execute(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = %s AND table_name LIKE 'basket_%%'
                ORDER BY table_name
                """,
                (DATABASE,),
            )
            tables = [row[0] for row in cur.fetchall()]
            if not tables:
                print("No basket_* tables found.")
                return 1

            for table in tables:
                cur.execute(f"TRUNCATE TABLE `{table}`")
                print(f"TRUNCATE {table}")

            cur.execute("SET FOREIGN_KEY_CHECKS = 1")
            print(f"\nDone. Truncated {len(tables)} tables in `{DATABASE}`.")
            return 0
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
