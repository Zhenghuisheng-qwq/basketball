from full_smoke_common import *
ctx=load_context()
e=ctx["smokes"]["E_EXTRA"]
tok=web_login("fs_main_alp01@cnpa.smoke")
r=req(f"{WEB}/basket/member/auth/session", token=tok)
nick=(r.get("data") or {}).get("nickname")
cases=[]
for c in e.get("cases") or []:
    if c["name"]=="session_nickname":
        c={"name":"session_nickname","expected":"FS_E_0811_OK","actual":str(nick),"evidence":f"/basket/member/auth/session code={r.get('code')}","result":"PASS" if nick=="FS_E_0811_OK" else "FAIL"}
    cases.append(c)
fails=sum(1 for c in cases if c["result"]=="FAIL")
e["cases"]=cases; e["fail"]=fails; e["pass"]=len(cases)-fails
e["attempts"]=e.get("attempts",[])+[{"n":3,"status":"PASS" if fails==0 else "FAIL","detail":"session path /basket/member/auth/session"}]
ctx["smokes"]["E_EXTRA"]=e; save_context(ctx)
print("E final", "PASS" if fails==0 else "FAIL", fails, nick)