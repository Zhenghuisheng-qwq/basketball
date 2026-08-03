#!/usr/bin/env python3
"""Execute CNPA V4 Round4 match table rebuild SQL."""
import re
from pathlib import Path

import pymysql

SQL_FILE = Path(__file__).resolve().parents[1] / "docs/sql/cnpa-v4-round4-match-rebuild.sql"
CONN = dict(host="192.168.31.47", user="root", password="123456", database="basketball", charset="utf8mb4")


def strip_comments(text: str) -> str:
    lines = []
    for line in text.splitlines():
        if line.strip().startswith("--"):
            continue
        lines.append(line)
    return "\n".join(lines)


def main() -> None:
    sql = strip_comments(SQL_FILE.read_text(encoding="utf-8"))
    conn = pymysql.connect(**CONN)
    cur = conn.cursor()

    # backup already done; finish drop + create
    for stmt in [
        "DROP TABLE IF EXISTS basket_match_player_stat",
        "DROP TABLE IF EXISTS basket_match_team_stat",
        "DROP TABLE IF EXISTS basket_match_period_score",
        "DROP TABLE IF EXISTS basket_match",
    ]:
        print("EXEC:", stmt)
        cur.execute(stmt)

    creates = re.findall(r"CREATE TABLE IF NOT EXISTS.*?\) ENGINE=InnoDB[^;]*;", sql, re.S | re.I)
    print("CREATE blocks:", len(creates))
    for block in creates:
        name = re.search(r"CREATE TABLE IF NOT EXISTS `([^`]+)`", block).group(1)
        print("CREATE:", name)
        cur.execute(block)

    conn.commit()

    cur.execute(
        "SELECT column_name FROM information_schema.columns "
        "WHERE table_schema='basketball' AND table_name='basket_match' ORDER BY ordinal_position"
    )
    cols = [r[0] for r in cur.fetchall()]
    print("basket_match columns:", cols)

    required = {"division_id", "season_id", "schedule_batch_id", "home_event_team_id",
                "away_event_team_id", "match_status", "publish_status"}
    missing = required - set(cols)
    if missing:
        raise SystemExit(f"Missing V4 columns: {missing}")

    cur.execute("SELECT COUNT(*) FROM bak_r4_basket_match")
    print("backup rows:", cur.fetchone()[0])
    conn.close()
    print("OK")


if __name__ == "__main__":
    main()
