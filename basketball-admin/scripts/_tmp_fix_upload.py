import re
from pathlib import Path
p = Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-admin\scripts\full_smoke_common.py")
text = p.read_bytes().replace(b"\x00", b"").decode("utf-8")
text2, n = re.subn(r"usage_type:\s*str\s*=\s*[\"']AVATAR[\"']", 'usage_type: str = "ACCOUNT_AVATAR"', text, count=1)
print("replaced", n)
if n:
    p.write_bytes(text2.encode("utf-8"))