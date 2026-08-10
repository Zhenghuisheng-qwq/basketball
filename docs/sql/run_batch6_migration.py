#!/usr/bin/env python3
"""Run CNPA batch6 migration on local dev DB."""
import pymysql

HOST = "192.168.31.48"
USER = "root"
PASSWORD = "123456"
DATABASE = "basketball"
MIGRATION_FILE = r"d:\code\dz\basketball\scaffolding-v2\docs\sql\cnpa-batch6-migration.sql"


def run_file(cur, path):
    sql = open(path, encoding="utf-8").read()
    stmts, buf = [], []
    for line in sql.splitlines():
        s = line.strip()
        if not s or s.startswith("--"):
            continue
        buf.append(line)
        if s.endswith(";"):
            stmts.append("\n".join(buf).rstrip(";").strip())
            buf = []
    return stmts


def main():
    conn = pymysql.connect(host=HOST, user=USER, password=PASSWORD, database=DATABASE, charset="utf8mb4")
    cur = conn.cursor()
    skip = ("Duplicate column", "Duplicate key", "already exists")
    for stmt in run_file(cur, MIGRATION_FILE):
        try:
            cur.execute(stmt)
            print("OK:", stmt[:90].replace("\n", " "))
        except Exception as e:
            if any(x in str(e) for x in skip):
                print("SKIP:", str(e)[:120])
            else:
                raise
    conn.commit()
    cur.close()
    conn.close()
    print("BATCH6 MIGRATION DONE")


if __name__ == "__main__":
    main()
