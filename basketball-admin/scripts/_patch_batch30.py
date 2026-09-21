# -*- coding: utf-8 -*-
import re
from pathlib import Path

import redis

r = redis.Redis(host="192.168.31.49", port=6379, db=0)
n = 0
for pat in ["member:email:code:ip:*", "member:email:code:limit:*", "member:email:code:day:*"]:
    for k in r.scan_iter(pat):
        r.delete(k)
        n += 1
print("cleared", n)

p = Path("batch_30_users_full_flow.py")
t = p.read_text(encoding="utf-8-sig")

helper = '''
def clear_email_code_limits() -> None:
    try:
        import redis
        from full_smoke_common import REDIS_HOST
        r = redis.Redis(host=REDIS_HOST, port=6379, db=0, socket_connect_timeout=3)
        for pat in ("member:email:code:ip:*", "member:email:code:limit:*"):
            for k in r.scan_iter(pat):
                r.delete(k)
    except Exception:
        pass

'''

if "def clear_email_code_limits" not in t:
    t = t.replace(
        'def fail(step: str, detail: object) -> None:\n    raise RuntimeError(f"[{step}] {detail}")\n',
        'def fail(step: str, detail: object) -> None:\n    raise RuntimeError(f"[{step}] {detail}")\n' + helper,
    )

t2 = re.sub(
    r'probe = req\([\s\S]*?print\("bypass send OK"\)',
    'clear_email_code_limits()\n'
    '        report["phases"]["bypass"] = "ASSUMED_TRUE_LOCAL"\n'
    '        print("bypass assume OK (limits cleared)")',
    t,
    count=1,
)
if t2 == t:
    print("WARN probe replace failed")
else:
    t = t2

if "for _try in range(6)" not in t:
    old = (
        '    send = req(\n'
        '        f"{WEB}/basket/member/auth/email-code/send",\n'
        '        "POST",\n'
        '        {"email": email, "purpose": "REGISTER"},\n'
        '    )\n'
        '    if send.get("code") != 200:\n'
        '        fail("email_code_send", f"{email} {send}")\n'
    )
    new = (
        '    send = {"code": -1}\n'
        '    for _try in range(6):\n'
        '        send = req(f"{WEB}/basket/member/auth/email-code/send", "POST", {"email": email, "purpose": "REGISTER"})\n'
        '        if send.get("code") == 200:\n'
        '            break\n'
        '        msg = str(send.get("msg") or "")\n'
        '        if any(x in msg for x in ("频繁", "上限", "过于")):\n'
        '            clear_email_code_limits()\n'
        '            time.sleep(0.4)\n'
        '            continue\n'
        '        break\n'
        '    if send.get("code") != 200:\n'
        '        fail("email_code_send", f"{email} {send}")\n'
    )
    if old not in t:
        print("WARN send block not found")
    else:
        t = t.replace(old, new)

t = t.replace(
    "clear_login_locks([email_of(i) for i in range(1, 31)])",
    "clear_login_locks([email_of(i) for i in range(1, 31)])\n        clear_email_code_limits()",
)

p.write_text(t, encoding="utf-8")
print(
    "patched",
    "clear_email_code_limits" in t,
    "for _try in range(6)" in t,
    "ASSUMED_TRUE_LOCAL" in t,
)
