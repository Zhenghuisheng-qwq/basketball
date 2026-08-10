from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
old = '''        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-09-30 23:59:59",
        "matchStartTime": "2026-08-12 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",'''
new = '''        "registrationStartTime": "2026-08-01 00:00:00",
        "registrationEndTime": "2026-08-20 23:59:59",
        "matchStartTime": "2026-08-21 00:00:00",
        "matchEndTime": "2026-12-20 23:59:59",'''
if old not in t:
    raise SystemExit("create_division times not found")
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
print("division times fixed")
