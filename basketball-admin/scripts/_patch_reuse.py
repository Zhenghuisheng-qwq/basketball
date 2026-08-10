from pathlib import Path
p = Path("full_smoke_common.py")
t = p.read_text(encoding="utf-8")
old = '''def setup_main_format(token: str, division_id: str) -> dict[str, str]:
    wipe_stages(token, division_id)
    st1 = _create_stage(token, division_id, stage_payload("ST1", "RR Top4", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "ST2", 4))'''
new = '''def setup_main_format(token: str, division_id: str) -> dict[str, str]:
    snap = req(f"{ADMIN}/basket/v4/divisions/{division_id}/format-snapshot", token=token)
    data = snap.get("data") or {}
    stages = sorted_stages(data)
    if data.get("locked") or data.get("snapshotStatus") == "LOCKED":
        if len(stages) >= 3:
            return {"st1": as_id(stages[0]["id"]), "st2": as_id(stages[1]["id"]), "st3": as_id(stages[2]["id"])}
    wipe_stages(token, division_id)
    st1 = _create_stage(token, division_id, stage_payload("ST1", "RR Top4", 1, "ROUND_ROBIN", 1, "OVERALL_TOP_N", "ST2", 4))'''
if old not in t:
    raise SystemExit("setup_main_format not found")
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
# same for setup_group_format start
print("patched setup_main_format reuse")
