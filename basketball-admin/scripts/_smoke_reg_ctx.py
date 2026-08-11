import json, urllib.request

def req(url, method="GET", data=None, token=None):
    headers={"Content-Type":"application/json"}
    if token: headers["Authorization"]=f"Bearer {token}"
    body=None if data is None else json.dumps(data).encode()
    r=urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(r, timeout=20) as resp:
        return json.loads(resp.read().decode())

# login a known smoke user if possible - try from env pattern
# first probe not-found without auth should 401
try:
    req("http://127.0.0.1:8881/basket/v4/player/registration/divisions/999999999/context")
except Exception as e:
    print("unauth", type(e).__name__, e)
print("server up check", end=" ")
try:
    urllib.request.urlopen("http://127.0.0.1:8881/", timeout=5)
except Exception as e:
    print(e)
