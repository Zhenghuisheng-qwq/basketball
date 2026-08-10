from full_smoke_common import admin_login, req, ADMIN, list_series, list_matches, get_series
tok=admin_login()
for sid in (549,550,551):
    st=req(f"{ADMIN}/basket/v4/admin/stages/{sid}", token=tok)
    d=st.get("data") or {}
    print("stage", sid, "status", d.get("stageStatus") or d.get("status"), "keys", list(d.keys())[:15] if d else st)
s2=list_series(tok,550)
print("series count", len(s2))
for s in s2:
    g=get_series(tok,s["id"])
    print("series", s["id"], g.get("seriesStatus"), g.get("homeWinCount"), g.get("awayWinCount"), g.get("winnerEventTeamId"), [(x.get("seriesGameNo"), x.get("matchStatus"), x.get("skipped")) for x in (g.get("games") or [])])
cc=req(f"{ADMIN}/basket/v4/admin/stages/550/check-completion","POST",token=tok)
print("check", cc)
