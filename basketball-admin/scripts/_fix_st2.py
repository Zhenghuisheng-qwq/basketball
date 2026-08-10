from full_smoke_common import admin_login, req, ADMIN, list_matches, generate_and_publish, confirm_stage_qualification
tok=admin_login()
print("st1 matches", len(list_matches(tok,"142",549)))
print("st2 matches", len(list_matches(tok,"142",550)))
# standings / qual
st=req(f"{ADMIN}/basket/v4/admin/stages/549/standings", token=tok)
print("standings groups", len(st.get("data") or []), "rows", len(((st.get("data") or [{}])[0].get("rows") or [])))
# try init+gen st2
r=req(f"{ADMIN}/basket/v4/admin/stages/550/initialize-teams","POST",token=tok)
print("init2", r)
r=req(f"{ADMIN}/basket/v4/admin/stages/550/generate-schedule","POST",token=tok)
print("gen2", r)
r=req(f"{ADMIN}/basket/v4/admin/stages/550/publish-schedule","POST",token=tok)
print("pub2", r)
from full_smoke_common import list_series
print("series", list_series(tok,550))
