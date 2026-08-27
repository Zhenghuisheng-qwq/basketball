from pathlib import Path

text = Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web-ui\src\views\events\v4\V4MatchDetailPage.vue").read_text(encoding="utf-8")
print("has_jin", "\u664b" in text)
print("has_ji", "\u7ea7" in text)
idx = text.find("winnerEventTeamId) return")
print(repr(text[idx:idx + 80]))
