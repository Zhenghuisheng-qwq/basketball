from pathlib import Path
p = Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts/playoff_v1_e2e_smoke.py")
text = p.read_text(encoding="utf-8")
text = text.replace("et.team_name AS teamName", "et.team_name_snapshot AS teamName")
text = text.replace("t.team_name name", "t.team_name name")  # ok
# case_b standing query uses et - check
old = """SELECT t.id teamId, t.team_name name, t.create_time ct, et.id eventTeamId
        FROM basket_event_team et JOIN basket_team t ON t.id = et.source_team_id
        WHERE et.division_id=%s AND et.is_delete=0 AND t.is_delete=0
        ORDER BY t.create_time ASC, t.id ASC"""
if old not in text:
    print("case_b query missing or different")
else:
    print("case_b query ok")
p.write_text(text, encoding="utf-8")
import py_compile; py_compile.compile(str(p), doraise=True)
print("fixed team_name")
