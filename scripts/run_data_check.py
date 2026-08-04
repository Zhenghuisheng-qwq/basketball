#!/usr/bin/env python3
"""Run CNPA data-check SQL files and print structured results."""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
ENV_LOCAL = ROOT / "scripts" / ".env.r4.local"
OUTPUT_DIR = ROOT / "scripts" / "output"

# Checks that always return rows but are informational (not anomalies)
STAT_CHECKS = {
    "Legacy Division 数量",
    "Legacy ScheduleBatch 数量",
    "Legacy Match 数量",
    "无 stage_id Standing 数量",
}


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, val = line.split("=", 1)
        os.environ.setdefault(key.strip(), val.strip().strip('"').strip("'"))


def db_config() -> dict:
    load_dotenv(ENV_LOCAL)
    return {
        "host": os.environ.get("CNPA_DB_HOST", "127.0.0.1"),
        "port": int(os.environ.get("CNPA_DB_PORT", "3306")),
        "user": os.environ.get("CNPA_DB_USER", "root"),
        "password": os.environ.get("CNPA_DB_PASSWORD", ""),
        "database": os.environ.get("CNPA_DB_NAME", "basketball"),
        "charset": "utf8mb4",
    }


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


def is_stat_check(name: str) -> bool:
    return any(key in name for key in STAT_CHECKS)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("sql_file", nargs="?", default="cnpa-dynamic-format-r4-data-check.sql")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    sql_path = ROOT / "docs" / "sql" / args.sql_file
    content = sql_path.read_text(encoding="utf-8")
    checks = split_checks(content)
    conn = pymysql.connect(**db_config())
    results: list[dict] = []
    anomaly_count = 0
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            for name, sql in checks:
                if not sql.upper().startswith("SELECT"):
                    continue
                item = {"name": name, "sql": sql[:200], "count": 0, "status": "OK", "blocking": False, "samples": []}
                try:
                    cur.execute(sql)
                    rows = cur.fetchall()
                    count = len(rows)
                    item["count"] = count
                    if count == 0:
                        item["status"] = "OK"
                    elif is_stat_check(name):
                        item["status"] = "STAT"
                        if count == 1 and rows:
                            item["samples"] = [rows[0]]
                    else:
                        item["status"] = "ANOMALY"
                        item["blocking"] = True
                        anomaly_count += 1
                        item["samples"] = rows[:5]
                    print(f"[{item['status']}] {name}: rows={count}")
                    for row in item["samples"]:
                        print(f"  {row}")
                except Exception as e:
                    item["status"] = "ERROR"
                    item["detail"] = str(e)
                    item["blocking"] = True
                    anomaly_count += 1
                    print(f"[ERROR] {name}: {e}")
                results.append(item)
    finally:
        conn.close()

    if args.json:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        out_path = OUTPUT_DIR / "r4-data-check-result.json"
        out_path.write_text(json.dumps({"checks": results}, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"JSON written: {out_path}")

    return 1 if anomaly_count else 0


if __name__ == "__main__":
    raise SystemExit(main())
