#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.error
import urllib.request

import pymysql


def http(base, method, path, body=None, token=None):
    url = base.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            payload["_http"] = resp.status
            return payload
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "ignore") or "{}"
        try:
            payload = json.loads(raw)
        except Exception:
            payload = {"_raw": raw[:400]}
        payload["_http"] = exc.code
        return payload


admin = "http://127.0.0.1:18080"
r = http(admin, "POST", "/login", {"username": "admin", "password": "admin123"})
token = r.get("token")
print("login", r.get("code"), bool(token))
if not token:
    raise SystemExit("login failed")

s = http(admin, "GET", "/basket/v4/division/selector-list?pageNum=1&pageSize=50", token=token)
data = s.get("rows") or s.get("data") or []
if isinstance(data, dict):
    data = data.get("rows") or []
print("selector", s.get("code"), "total", s.get("total"), "n", len(data) if isinstance(data, list) else type(data))
if isinstance(data, list):
    for item in data:
        print(" ", item.get("divisionId"), item.get("divisionName"), item.get("seriesCode"), item.get("selectable"))

conn = pymysql.connect(host="192.168.31.48", user="root", password="123456",
                       database="basketball_r9_fresh", charset="utf8mb4",
                       cursorclass=pymysql.cursors.DictCursor)
cur = conn.cursor()
cur.execute("SELECT id, series_code FROM basket_series WHERE is_delete=0 ORDER BY id")
series = {row["series_code"]: row["id"] for row in cur.fetchall()}
print("series", series)
cur.close()
conn.close()

st = http(admin, "GET", f"/basket/v4/format-template/standard?seriesId={series['LEAGUE']}", token=token)
print("league standard", st.get("code"), str(st.get("data"))[:400])

applies = [
    (3, "LEAGUE_STANDARD_V1", "TEAM"),
    (4, "ARENA_STANDARD_V1", "TEAM"),
    (5, "CUP_STANDARD_V1", "TEAM"),
    (6, "OPEN_STANDARD_V1", "PERSONAL"),
]
for did, code, mode in applies:
    body = {"templateCode": code, "registrationMode": mode, "gameMode": "FIVE_V_FIVE"}
    resp = http(admin, "POST", f"/basket/v4/divisions/{did}/standard-template/apply", body, token)
    print("apply", did, code, resp.get("code"), resp.get("msg"))
    lock = http(admin, "POST", f"/basket/v4/divisions/{did}/format-snapshot/lock", {}, token)
    print("lock", did, lock.get("code"), lock.get("msg"))

for did in (3, 4, 5, 6):
    ov = http(admin, "GET", f"/basket/v4/division/{did}/workflow-overview", token=token)
    data = ov.get("data") or {}
    rule = data.get("rule") or {}
    caps = data.get("capabilities") or {}
    print(
        "after",
        did,
        "rule",
        rule.get("status") or rule.get("ruleStatus") or rule.get("snapshotStatus"),
        "tpl",
        (data.get("context") or {}).get("templateCode"),
        "canLockRoster",
        caps.get("canLockRoster"),
        "canGenerate",
        caps.get("canGenerateStructure"),
        "hint",
        data.get("workflowHint"),
    )
