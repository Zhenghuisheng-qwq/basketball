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

# R5 checks where cnt=0 means OK (column/template should NOT exist)
ZERO_OK_CHECKS = {
    "check_09_rank_explain_json_column",
    "check_17_v4_legacy_template",
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


def is_zero_ok_check(name: str) -> bool:
    return any(key in name for key in ZERO_OK_CHECKS)


def check_rank_explain_json_column(cur) -> dict:
    """Verify rank_explain_json column is physically removed from basket_standing."""
    sql = (
        "SELECT COUNT(*) AS cnt FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA = DATABASE() "
        "AND TABLE_NAME = 'basket_standing' "
        "AND COLUMN_NAME = 'rank_explain_json'"
    )
    cur.execute(sql)
    row = cur.fetchone() or {}
    cnt = row.get("cnt", row) if isinstance(row, dict) else row
    cnt = int(cnt)
    item = {
        "name": "python_check_rank_explain_json_column",
        "sql": sql,
        "count": cnt,
        "status": "OK" if cnt == 0 else "ANOMALY",
        "blocking": cnt != 0,
        "samples": [],
    }
    print(f"[{item['status']}] {item['name']}: column_exists={cnt}")
    return item


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "sql_file",
        nargs="?",
        default="cnpa-dynamic-format-r5-data-check.sql",
        help="SQL file under docs/sql/ (default: R5 data-check)",
    )
    parser.add_argument("--json", action="store_true")
    parser.add_argument(
        "--skip-python-rank-check",
        action="store_true",
        help="Skip standalone information_schema rank_explain_json check",
    )
    args = parser.parse_args()

    sql_path = ROOT / "docs" / "sql" / args.sql_file
    if not sql_path.exists():
        print(f"SQL file not found: {sql_path}", file=sys.stderr)
        return 1

    content = sql_path.read_text(encoding="utf-8")
    checks = split_checks(content)
    conn = pymysql.connect(**db_config())
    results: list[dict] = []
    anomaly_count = 0
    try:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            if not args.skip_python_rank_check:
                rank_item = check_rank_explain_json_column(cur)
                results.append(rank_item)
                if rank_item["blocking"]:
                    anomaly_count += 1

            for name, sql in checks:
                if not sql.upper().startswith("SELECT"):
                    continue
                item = {"name": name, "sql": sql[:200], "count": 0, "status": "OK", "blocking": False, "samples": []}
                try:
                    cur.execute(sql)
                    rows = cur.fetchall()
                    count = len(rows)
                    # Single-row aggregate checks (check_id, cnt)
                    if count == 1 and rows and isinstance(rows[0], dict) and "cnt" in rows[0]:
                        count = int(rows[0]["cnt"])
                        item["samples"] = [rows[0]]
                    elif count > 0 and not is_stat_check(name):
                        item["samples"] = rows[:5]

                    item["count"] = count
                    if is_zero_ok_check(name):
                        if count == 0:
                            item["status"] = "OK"
                        else:
                            item["status"] = "ANOMALY"
                            item["blocking"] = True
                            anomaly_count += 1
                    elif count == 0:
                        item["status"] = "OK"
                    elif is_stat_check(name):
                        item["status"] = "STAT"
                        if count == 1 and rows:
                            item["samples"] = [rows[0]]
                    else:
                        item["status"] = "ANOMALY"
                        item["blocking"] = True
                        anomaly_count += 1
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
        out_path = OUTPUT_DIR / "r5-data-check-result.json"
        out_path.write_text(json.dumps({"checks": results}, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"JSON written: {out_path}")

    return 1 if anomaly_count else 0


if __name__ == "__main__":
    raise SystemExit(main())
