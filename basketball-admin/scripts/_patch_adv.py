from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
old='''def setup_advanced_format(token: str, division_id: str) -> dict[str, str]:
    wipe_stages(token, division_id)
    st1 = _create_stage('''
new='''def setup_advanced_format(token: str, division_id: str) -> dict[str, str]:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    data = snap.get("data") or {}
    stages = sorted_stages(data)
    if (data.get("locked") or data.get("snapshotStatus") == "LOCKED") and len(stages) >= 2:
        return {"st1": as_id(stages[0]["id"]), "st2": as_id(stages[1]["id"])}
    wipe_stages(token, division_id)
    st1 = _create_stage('''
if old not in t:
    raise SystemExit("setup_advanced not found")
p.write_text(t.replace(old,new), encoding="utf-8", newline="\n")
print("advanced reuse ok")
