#!/usr/bin/env python3
"""为 8/1 已完赛但无比分的比赛批量写入比分并由主队队长提交（审核中）。"""

import json
import sys
import urllib.request

import pymysql

JDBC = dict(host="192.168.31.48", user="root", password="123456", database="basketball", charset="utf8mb4")
WEB_BASE = "http://127.0.0.1:8881"
MATCH_DATE = "2026-08-01"
PASSWORD = "Test1234"

SCORES = {
    900020084: (78, 62),
    900020085: (85, 71),
    900020086: (92, 58),
    900020087: (76, 68),
    900020088: (88, 74),
}


def split_score(total: int, count: int) -> list[int]:
    base, rem = divmod(total, count)
    return [base + (1 if i < rem else 0) for i in range(count)]


def post(path: str, body: dict, token: str | None = None) -> dict:
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        WEB_BASE + path,
        data=data,
        headers={"Content-Type": "application/json", **({"Authorization": "Bearer " + token} if token else {})},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(email: str) -> str:
    resp = post("/basket/member/auth/login/password", {"email": email, "password": PASSWORD})
    if resp.get("code") != 200:
        raise RuntimeError(f"login failed {email}: {resp.get('msg')}")
    return resp["data"]["token"]


def main() -> int:
    conn = pymysql.connect(**JDBC)
    ok = fail = 0
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT m.id, m.home_team_id, ht.team_name, at.team_name, ma.email
                FROM basket_match m
                JOIN basket_tournament_registration tr
                  ON tr.team_id = m.home_team_id AND tr.season_id = m.season_id AND tr.is_delete = 0
                JOIN basket_member_account ma ON ma.id = tr.captain_member_id AND ma.is_delete = 0
                LEFT JOIN basket_team ht ON ht.id = m.home_team_id
                LEFT JOIN basket_team at ON at.id = m.away_team_id
                WHERE DATE(m.scheduled_start_time) = %s
                  AND m.home_score = 0 AND m.away_score = 0 AND m.is_delete = 0
                  AND NOT EXISTS (
                    SELECT 1 FROM basket_match_data_submission s
                    WHERE s.match_id = m.id AND s.is_delete = 0 AND s.submission_status IN (0, 1)
                  )
                ORDER BY m.id
                """,
                (MATCH_DATE,),
            )
            matches = cur.fetchall()

        for match_id, home_team_id, home_name, away_name, captain_email in matches:
            if match_id not in SCORES:
                print(f"SKIP matchId={match_id} 未配置比分")
                continue
            home_score, away_score = SCORES[match_id]
            try:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        UPDATE basket_match
                        SET match_status = 3, home_score = %s, away_score = %s,
                            winner_team_id = CASE WHEN %s > %s THEN home_team_id
                                                WHEN %s > %s THEN away_team_id ELSE NULL END,
                            actual_end_time = COALESCE(actual_end_time, DATE_ADD(scheduled_start_time, INTERVAL 2 HOUR)),
                            update_time = NOW()
                        WHERE id = %s AND home_score = 0 AND away_score = 0
                        """,
                        (home_score, away_score, home_score, away_score, away_score, home_score, match_id),
                    )
                    if cur.rowcount != 1:
                        raise RuntimeError("更新比分失败")
                    cur.execute(
                        """
                        SELECT r.id, r.player_id
                        FROM basket_tournament_season_roster r
                        JOIN basket_match m ON m.home_team_id = r.team_id AND m.season_id = r.season_id
                        WHERE m.id = %s AND r.roster_status = 1 AND r.is_delete = 0
                        ORDER BY r.id
                        """,
                        (match_id,),
                    )
                    roster = cur.fetchall()
                conn.commit()

                points = split_score(home_score, len(roster))
                player_stats = []
                for i, (roster_id, player_id) in enumerate(roster):
                    player_stats.append(
                        {
                            "playerId": str(player_id),
                            "rosterId": str(roster_id),
                            "points": points[i],
                            "rebounds": 2 + (i % 4),
                            "assists": 1 + (i % 5),
                            "steals": i % 2,
                            "blocks": i % 3,
                            "turnovers": 1 + (i % 3),
                            "fouls": 1 + (i % 4),
                        }
                    )
                payload = {
                    "submitterTeamId": str(home_team_id),
                    "playerStats": player_stats,
                    "remark": "8月1日批量补录，待管理员审核",
                }
                token = login(captain_email)
                resp = post(f"/basket/player/match-data/{match_id}/team-player-stats/submit", payload, token)
                if resp.get("code") != 200:
                    raise RuntimeError(resp.get("msg"))
                data = resp.get("data") or {}
                ok += 1
                print(
                    f"OK matchId={match_id} {home_name} vs {away_name} "
                    f"score={home_score}-{away_score} captain={captain_email} "
                    f"submissionNo={data.get('submissionNo')}"
                )
            except Exception as ex:
                conn.rollback()
                fail += 1
                print(f"FAIL matchId={match_id} {ex}")
    finally:
        conn.close()
    print(f"DONE OK={ok} FAIL={fail}")
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
