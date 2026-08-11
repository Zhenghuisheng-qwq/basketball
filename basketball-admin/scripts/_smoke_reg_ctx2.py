import json, urllib.request

WEB="http://127.0.0.1:8881"
PASS="111111aa"

def req(url, method="GET", data=None, token=None):
    headers={"Content-Type":"application/json"}
    if token: headers["Authorization"]= f"Bearer {token}"
    body=None if data is None else json.dumps(data).encode()
    r=urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(r, timeout=30) as resp:
        return json.loads(resp.read().decode())

login=req(f"{WEB}/basket/member/auth/login/password","POST",{"email":"2026001@qq.com","password":PASS})
print("login code", login.get("code"), login.get("msg"))
token=login.get("token") or (login.get("data") or {}).get("token")
if not token:
    # try data structure
    print(json.dumps(login, ensure_ascii=False)[:500])
    raise SystemExit(1)

def data(r):
    return r.get("data")

ctx=req(f"{WEB}/basket/v4/player/registration/divisions/166/context", token=token)
print("CTX166", json.dumps(data(ctx), ensure_ascii=False, default=str)[:800])

missing=req(f"{WEB}/basket/v4/player/registration/divisions/999999999/context", token=token)
print("MISSING", json.dumps(data(missing), ensure_ascii=False, default=str)[:500])

avail=req(f"{WEB}/basket/v4/player/registration/available-divisions", token=token)
rows=data(avail) or []
row=next((x for x in rows if str(x.get("divisionId"))=="166"), None)
print("AVAIL166 keys", None if not row else {k:row.get(k) for k in ["divisionId","canRegister","registrationWindowStatus","registrationWindowStatusLabel","cannotRegisterReason","cannotRegisterReasonCode","myGrantStatus","grantTeamId"]})
print("divisionId type", None if not row else type(row.get("divisionId")).__name__, row.get("divisionId") if row else None)

# finished/non-open context still loads
ctx164=req(f"{WEB}/basket/v4/player/registration/divisions/164/context", token=token)
d164=data(ctx164)
print("CTX164", {k:d164.get(k) for k in ["divisionExists","divisionId","canRegister","registrationWindowStatusLabel","cannotRegisterReason","cannotRegisterReasonCode","divisionStatus","seriesName","seasonTitle"]})
