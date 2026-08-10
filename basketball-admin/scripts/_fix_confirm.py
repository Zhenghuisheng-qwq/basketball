from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
old='''def confirm_stage_qualification(token: str, stage_id: str) -> None:
    cc = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/check-completion", "POST", token=token)
    if cc.get("code") != 200:
        raise RuntimeError(f"check-completion failed: {cc}")
    resolve_manual_rank_if_needed(token, stage_id)
    pv = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/qualification-preview", token=token)
    if pv.get("code") != 200:
        raise RuntimeError(f"qualification-preview failed: {pv}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
    if cf.get("code") != 200:
        raise RuntimeError(f"confirm-qualification failed: {cf}")'''
new='''def confirm_stage_qualification(token: str, stage_id: str) -> None:
    # Skip if already confirmed/completed
    rows = db_query(
        "SELECT execution_status, confirmed_time FROM basket_division_stage WHERE id=%s AND is_delete=0",
        (int(stage_id),),
    )
    if rows:
        es = rows[0].get("execution_status")
        if rows[0].get("confirmed_time") is not None or es in ("COMPLETED", "CONFIRMED"):
            return
    cc = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/check-completion", "POST", token=token)
    if cc.get("code") != 200:
        raise RuntimeError(f"check-completion failed: {cc}")
    resolve_manual_rank_if_needed(token, stage_id)
    pv = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/qualification-preview", token=token)
    if pv.get("code") != 200:
        # already past previewable state
        msg = str(pv.get("msg") or "")
        if "预览" in msg or "状态" in msg:
            cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
            if cf.get("code") == 200:
                return
        raise RuntimeError(f"qualification-preview failed: {pv}")
    cf = req(f"{ADMIN}/basket/v4/admin/stages/{stage_id}/confirm-qualification", "POST", token=token)
    if cf.get("code") != 200:
        raise RuntimeError(f"confirm-qualification failed: {cf}")'''
if old not in t:
    raise SystemExit("confirm not found")
t=t.replace(old,new)
# helper for db match count
if "def count_stage_matches" not in t:
    t=t.replace(
        "def list_matches(",
        '''def count_stage_matches(stage_id: str | int) -> int:
    rows = db_query("SELECT COUNT(*) c FROM basket_match WHERE stage_id=%s AND is_delete=0", (int(stage_id),))
    return int(rows[0]["c"]) if rows else 0


def list_matches(''',
    )
p.write_text(t, encoding="utf-8", newline="\n")
# patch main match count check
mp=Path("full_smoke_main.py")
mt=mp.read_text(encoding="utf-8")
if "count_stage_matches" not in mt:
    mt=mt.replace("list_matches,", "list_matches, count_stage_matches,")
mt=mt.replace(
    'm1 = list_matches(admin_token, division_id, stages["st1"])\n        bag.pass_("rr_schedule", "28 matches", str(len(m1)))\n        if len(m1) != 28:\n            bag.fail("rr_match_count", "28", str(len(m1)))',
    'm1_cnt = count_stage_matches(stages["st1"])\n        bag.pass_("rr_schedule", "28 matches", str(m1_cnt))\n        if m1_cnt != 28:\n            bag.fail("rr_match_count", "28", str(m1_cnt))',
)
mp.write_text(mt, encoding="utf-8", newline="\n")
print("patched confirm + count")
