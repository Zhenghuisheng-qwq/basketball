import json
import urllib.request

import redis

keys = []
for host_pass in [None, "Q86KDiDoZULykMfz"]:
    try:
        r = redis.Redis(
            host="192.168.31.48",
            port=6379,
            password=host_pass,
            db=0,
            socket_connect_timeout=3,
            decode_responses=True,
        )
        found = list(r.scan_iter("member:login:fail:*"))
        print("auth", host_pass, "keys", found)
        for k in found:
            if "aops" in k:
                r.delete(k)
                print("deleted", k)
        r.delete("member:login:fail:fs_aops_aops0101@cnpa.smoke")
        print("ping", r.ping())
        break
    except Exception as ex:
        print("fail", host_pass, ex)

body = json.dumps({"email": "fs_aops_aops0101@cnpa.smoke", "password": "111111aa"}).encode()
req = urllib.request.Request(
    "http://127.0.0.1:8881/basket/member/auth/login/password",
    data=body,
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req, timeout=10) as resp:
    print(json.loads(resp.read().decode()))
