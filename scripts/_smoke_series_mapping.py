import sys
from pathlib import Path

sys.path.insert(0, str(Path(r"d:\code\dz\basketball\scaffolding-v2") / "basketball-admin" / "scripts"))
import full_smoke_common as fs

fs.WEB = "http://127.0.0.1:8881"
req = fs.req


def mapped(series):
    left_id = str(series.get("awayEventTeamId"))
    right_id = str(series.get("homeEventTeamId"))
    rows = []
    for game in series.get("games") or []:
        left = game.get("awayScore")
        right = game.get("homeScore")
        winner = str(game.get("winnerEventTeamId") or "")
        if winner == left_id:
            who = series.get("awayTeamName")
        elif winner == right_id:
            who = series.get("homeTeamName")
        else:
            who = None
        rows.append((game.get("seriesGameNo"), left, right, who, game.get("matchId")))
    return rows


def main() -> None:
    sch = req(f"{fs.WEB}/basket/v4/public/division/3/schedule").get("data") or []
    by_id = {str(item.get("id")): item.get("roundName") for item in sch if item.get("unitType") == "MATCH_SERIES"}
    for sid, expect_n, expect_round in ((19, 5, "SF"), (15, 3, "R8")):
        series = req(f"{fs.WEB}/basket/v4/public/match-series/{sid}").get("data") or {}
        games = mapped(series)
        print("series", sid, "left", series.get("awayTeamName"), "right", series.get("homeTeamName"))
        print("score", series.get("awayWinCount"), series.get("homeWinCount"), "winner", series.get("winnerTeamName"))
        print("roundName", by_id.get(str(sid)), "expect", expect_round)
        print("games", games)
        assert len(games) == expect_n
        assert by_id.get(str(sid)) == expect_round
        assert all(row[1] is not None and row[2] is not None and row[3] for row in games)
    print("mapping PASS")


if __name__ == "__main__":
    main()
