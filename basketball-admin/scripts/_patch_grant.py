from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
old = '''        gr = create_captain_grant(admin_token, division_id, t["captain_player_id"])
        if gr.get("code") != 200:
            raise RuntimeError(f"grant failed team {t['code']}: {gr}")'''
new = '''        gr = create_captain_grant(admin_token, division_id, t["captain_player_id"])
        if gr.get("code") != 200:
            msg = str(gr.get("msg") or "")
            if "已有" not in msg and "存在" not in msg:
                raise RuntimeError(f"grant failed team {t['code']}: {gr}")'''
if old not in t:
    raise SystemExit("grant check not found")
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
print("grant tolerate existing")
