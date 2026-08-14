#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.error
import urllib.request

import pymysql

conn = pymysql.connect(
    host="192.168.31.48",
    user="root",
    password="123456",
    database="basketball_r9_fresh",
    charset="utf8mb4",
    autocommit=True,
)
cur = conn.cursor()
cur.execute(
    "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'"
)
print("captcha updated", cur.rowcount)
cur.close()
conn.close()


def http(base, method, path, body=None, token=None):
    url = base.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            payload["_http"] = resp.status
            return payload
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "ignore") or "{}"
        try:
            payload = json.loads(raw)
        except Exception:
            payload = {"_raw": raw[:300]}
        payload["_http"] = exc.code
        return payload


admin = "http://127.0.0.1:18080"
web = "http://127.0.0.1:18881"
r = http(admin, "POST", "/login", {"username": "admin", "password": "admin123"})
print("login", r.get("_http"), r.get("code"), r.get("msg"), "token", bool(r.get("token")))
token = r.get("token")
if not token:
    raise SystemExit("login failed")

s = http(admin, "GET", "/basket/v4/division/selector-list", token=token)
print("selector", s.get("_http"), s.get("code"), s.get("msg"))
data = s.get("data")
rows = data if isinstance(data, list) else (data or {}).get("rows") or (data or {}).get("list") or []
print("selector count", len(rows) if isinstance(rows, list) else type(rows))
if isinstance(rows, list):
    for item in rows:
        print(
            " ",
            item.get("divisionId") or item.get("id"),
            item.get("divisionName"),
            item.get("seriesCode"),
            item.get("registrationMode"),
            item.get("selectable"),
            item.get("blockReason"),
        )

t = http(admin, "GET", "/basket/v4/format-template/standard", token=token)
print("standard", t.get("_http"), t.get("code"), t.get("msg"))
td = t.get("data")
if isinstance(td, list):
    for x in td:
        print(" ", x.get("templateCode"), x.get("templateType"), x.get("defaultRegistrationMode"))
else:
    print(" ", str(td)[:300])

for did in (3, 4, 5, 6):
    ov = http(admin, "GET", f"/basket/v4/division/{did}/workflow-overview", token=token)
    data = ov.get("data") or {}
    ctx = data.get("context") or {}
    caps = data.get("capabilities") or {}
    print(
        "overview",
        did,
        ov.get("code"),
        ctx.get("divisionName"),
        ctx.get("seriesCode"),
        ctx.get("registrationMode"),
        "canPublishRule",
        caps.get("canPublishRule"),
        "canGenerate",
        caps.get("canGenerateStructure"),
        "hint",
        data.get("workflowHint"),
    )

p = http(web, "GET", "/basket/v4/public/season/list")
print("public seasons", p.get("_http"), p.get("code"), "n", len(p.get("data") or []) if isinstance(p.get("data"), list) else p.get("data"))
p = http(web, "GET", "/basket/v4/public/series/list")
print("public series", p.get("_http"), p.get("code"), p.get("msg"), "n", len(p.get("data") or []) if isinstance(p.get("data"), list) else str(p.get("data"))[:200])
p = http(web, "GET", "/basket/v4/public/division/3")
print("public div3", p.get("_http"), p.get("code"), p.get("msg"))
p = http(web, "GET", "/basket/v4/public/stage/list?divisionId=3")
print("public stages", p.get("_http"), p.get("code"), p.get("msg"), "n", len(p.get("data") or []) if isinstance(p.get("data"), list) else p.get("data"))
