from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
old='''def generate_and_publish(token: str, stage_id: str, init: bool = True) -> dict:
    if init:
        ir = init_stage_teams(token, stage_id)
        if ir.get("code") != 200:
            msg = str(ir.get("msg") or "")
            if "已" not in msg and "already" not in msg.lower():
                raise RuntimeError(f"initialize-teams failed stage {stage_id}: {ir}")
    gen = generate_schedule(token, stage_id)
    if gen.get("code") != 200:
        msg = str(gen.get("msg") or "")
        if "已" not in msg and "already" not in msg.lower() and "存在" not in msg:
            raise RuntimeError(f"generate-schedule failed stage {stage_id}: {gen}")
    pub = publish_schedule(token, stage_id)
    if pub.get("code") != 200:
        msg = str(pub.get("msg") or "")
        if "已" not in msg and "PUBLISH" not in msg.upper() and "发布" not in msg:
            raise RuntimeError(f"publish-schedule failed stage {stage_id}: {pub}")
    return {"generate": gen, "publish": pub}'''
new='''def generate_and_publish(token: str, stage_id: str, init: bool = True, division_id: str | None = None) -> dict:
    # If matches already exist for stage, skip generate and only ensure publish.
    existing = []
    if division_id:
        existing = [m for m in list_matches(token, division_id, stage_id)]
    if existing:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing", "count": len(existing)}, "publish": pub}
    if init:
        ir = init_stage_teams(token, stage_id)
        if ir.get("code") != 200:
            msg = str(ir.get("msg") or "")
            if "已" not in msg and "already" not in msg.lower():
                raise RuntimeError(f"initialize-teams failed stage {stage_id}: {ir}")
    gen = generate_schedule(token, stage_id)
    if gen.get("code") != 200:
        raise RuntimeError(f"generate-schedule failed stage {stage_id}: {gen}")
    pub = publish_schedule(token, stage_id)
    if pub.get("code") != 200:
        msg = str(pub.get("msg") or "")
        if "已" not in msg and "PUBLISH" not in msg.upper() and "发布" not in msg:
            raise RuntimeError(f"publish-schedule failed stage {stage_id}: {pub}")
    return {"generate": gen, "publish": pub}'''
if old not in t:
    raise SystemExit("block not found")
p.write_text(t.replace(old,new), encoding="utf-8", newline="\n")
# update call sites in main.py
mp=Path("full_smoke_main.py")
mt=mp.read_text(encoding="utf-8")
mt2=mt.replace('generate_and_publish(admin_token, stages["st1"])', 'generate_and_publish(admin_token, stages["st1"], division_id=division_id)')
mt2=mt2.replace('generate_and_publish(admin_token, stages["st2"], init=False)', 'generate_and_publish(admin_token, stages["st2"], init=False, division_id=division_id)')
mt2=mt2.replace('generate_and_publish(admin_token, stages["st3"], init=False)', 'generate_and_publish(admin_token, stages["st3"], init=False, division_id=division_id)')
mp.write_text(mt2, encoding="utf-8", newline="\n")
print("updated generate_and_publish + main calls")
