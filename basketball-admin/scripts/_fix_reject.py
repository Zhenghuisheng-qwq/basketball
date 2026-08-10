from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
t=t.replace(
    '{"auditRemark": "FULL_SMOKE reject then resubmit"}',
    '{"auditRemark": "FULL_SMOKE reject then resubmit", "rejectReason": "FULL_SMOKE reject then resubmit", "reason": "FULL_SMOKE reject then resubmit"}',
)
p.write_text(t, encoding="utf-8", newline="\n")
print("patched reject body")
