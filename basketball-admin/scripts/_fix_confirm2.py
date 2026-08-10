from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
# broaden skip: after preview fail, if confirm succeeds OR msg contains common patterns, return
old='''    if pv.get("code") != 200:
        # already past previewable state
        msg = str(pv.get("msg") or "")
        if "预览" in msg or "状态" in msg:
            cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
            if cf.get("code") == 200:
                return
        raise RuntimeError(f"qualification-preview failed: {pv}")'''
new='''    if pv.get("code") != 200:
        # already past previewable / already confirmed
        cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
        if cf.get("code") == 200:
            return
        rows2 = db_query(
            "SELECT execution_status, confirmed_time FROM basket_division_stage WHERE id=%s AND is_delete=0",
            (int(stage_id),),
        )
        if rows2 and (rows2[0].get("confirmed_time") is not None or rows2[0].get("execution_status") in ("COMPLETED", "CONFIRMED")):
            return
        raise RuntimeError(f"qualification-preview failed: {pv}; confirm={cf}")'''
if old not in t:
    raise SystemExit("preview block missing")
p.write_text(t.replace(old,new), encoding="utf-8", newline="\n")
print("confirm hardened")
