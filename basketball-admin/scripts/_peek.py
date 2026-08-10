from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
# find create_teams_flow logo usage
idx = t.find("def create_teams_flow")
print(t[idx:idx+800])
