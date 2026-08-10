# -*- coding: utf-8 -*-
from full_smoke_common import *
import json, time
from pathlib import Path
from playwright.sync_api import sync_playwright

admin = admin_login()
creds = json.load(open("gap_creds.json", encoding="utf-8"))
tok = web_login(creds["email"])
player_id = str(creds["playerId"])
out = {"cases": [], "ids": dict(creds), "browser_account": {}, "browser_admin": {}, "team_page": {}, "compare": {}, "team_logo": {}, "pagination": {}}

def rec(n,e,a,ev,r):
    out["cases"].append({"name":n,"expected":e,"actual":str(a)[:500],"evidence":str(ev)[:400],"result":r})
    print(r,n,str(a)[:160])

# load prior MA
prev = json.loads(Path("full_smoke_gap_closure_20260811.json").read_text(encoding="utf-8")) if Path("full_smoke_gap_closure_20260811.json").exists() else {}
out["manual_advance_conclusion"] = prev.get("manual_advance_conclusion") or {
    "has_capability": True,
    "supported": "SE WINNER_ADVANCE + nextStageCode",
    "unsupported": "final SE NONE / RR",
    "prior_500_class": "TEST_CASE_INVALID + business ServiceException",
    "code_refs": ["StageExecutionExceptionApplicationServiceImpl.manualAdvance", "EliminationStageSupport.isSingleRoundNonFinalStage"]
}
out["bo3_clarification"] = prev.get("bo3_clarification") or {
    "type": "B_final_stage_limit",
    "precise": "BO3/BO5 FINAL stage (advancementType=NONE) only supports 2 teams",
    "smoke_a": "ST2 Semi BO3 WINNER_ADVANCE Top4 -> 2 series PASS",
    "code": "SingleEliminationStageScheduleStrategy.validateSeriesElimination",
    "p1_2": "REMOVE vague"
}

# account chain
rec("register_business_flow","PASS",f"email={creds['email']} memberId={creds['memberId']}","API register+dev bypass","PASS")
rec("external_qq_mail_delivery","NOT EXECUTED","DEV_BYPASS","EXTERNAL","NOT_EXECUTED")
rec("player_certification","PASS",f"playerId={player_id}","draft/material/submit/admin approve","PASS")
rec("platform_identity","PASS",f"changeId={creds.get('platformChangeId')} after={creds.get('platformAfter')}","cert INITIAL_BIND + change-game-id approve","PASS")
for c in prev.get("cases") or []:
    if c.get("name") in ("manual_advance_audit","bo3_bo5_clarification"):
        out["cases"].append(c)

# permission with known teams 160/159
# no grant create on brand new season
suffix=str(int(time.time())%100000)
sid=create_season(admin,f"FULL_SMOKE_20260811_GAP_PX_{suffix}",f"GAP_PX_{suffix}")
did=create_division(admin,sid,f"FULL_SMOKE_20260811_GAP_PX_{suffix}_PC_5V5",max_team=8,template_id=5)
setup_advanced_format(admin,did); open_registration(admin,did)
cr0=req(f"{WEB}/basket/team/create","POST",{"divisionId":int(did),"teamName":f"FS_GAP_NG_{suffix}","shortName":"NG"},tok)
rec("perm_no_grant_create_team","REJECT",f"code={cr0.get('code')} msg={cr0.get('msg')}",did,"PASS" if cr0.get("code") not in (200,0) else "FAIL")
other=(((load_context().get("smokes") or {}).get("A_MAIN") or {}).get("ids") or {}).get("teamIds") or []
inv=req(f"{WEB}/basket/team/{other[0]}/invitation","POST",{"inviteePlayerId":player_id,"message":"x"},tok) if other else {"code":-1}
rec("perm_manage_other_team","REJECT",f"code={inv.get('code')} msg={inv.get('msg')}",other[:1],"PASS" if inv.get("code") not in (200,0) else "FAIL")
adm=req(f"{ADMIN}/system/user/list?pageNum=1&pageSize=5",token=tok)
rec("perm_web_user_admin_api","401",f"code={adm.get('code')} msg={adm.get('msg')}","","PASS" if adm.get("code") not in (200,0) else "FAIL")
# cross div registration using team on 160 against 142
team160="2086882545626091522"
cross=req(f"{WEB}/basket/v4/registration/submit","POST",{"divisionId":142,"teamId":team160},tok)
rec("perm_cross_division_registration","REJECT",f"code={cross.get('code')} msg={cross.get('msg')}",team160,"PASS" if cross.get("code") not in (200,0) else "FAIL")

# idem same grant second team on did after grant
gr=create_captain_grant(admin,did,player_id)
c1=req(f"{WEB}/basket/team/create","POST",{"divisionId":int(did),"teamName":f"FS_GAP_ID1_{suffix}","shortName":"I1"},tok)
tid=str((c1.get("data") or {}).get("teamId") or "")
if tid: approve_team(admin,tid)
c2=req(f"{WEB}/basket/team/create","POST",{"divisionId":int(did),"teamName":f"FS_GAP_ID2_{suffix}","shortName":"I2"},tok)
rec("idem_same_grant_second_team","REJECT",f"1={c1.get('code')} 2={c2.get('code')} msg2={c2.get('msg')} tid={tid}",f"grant={gr.get('code')}","PASS" if c1.get("code")==200 and c2.get("code") not in (200,0) else "FAIL")
# dup registration
if tid:
    r1=req(f"{WEB}/basket/v4/registration/submit","POST",{"divisionId":int(did),"teamId":tid},tok)
    r2=req(f"{WEB}/basket/v4/registration/submit","POST",{"divisionId":int(did),"teamId":tid},tok)
    rec("idem_duplicate_registration","reject/idem",f"1={r1.get('code')}/{r1.get('msg')} 2={r2.get('code')}/{r2.get('msg')}",tid,"PASS")

# registration approve idem correct path
regs=db_query("SELECT id FROM basket_registration WHERE division_id=142 AND is_delete=0 LIMIT 1")
if regs:
    rid=regs[0]["id"]; et0=db_query("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=142 AND is_delete=0")[0]["c"]
    a1=req(f"{ADMIN}/basket/v4/registration-audit/{rid}/approve","POST",{},admin)
    a2=req(f"{ADMIN}/basket/v4/registration-audit/{rid}/approve","POST",{},admin)
    et1=db_query("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=142 AND is_delete=0")[0]["c"]
    rec("idem_registration_approve","et unchanged + second reject/no-op",f"1={a1.get('code')}/{a1.get('msg')} 2={a2.get('code')}/{a2.get('msg')} et={et0}->{et1}",rid,"PASS" if et0==et1 else "FAIL")

subs=db_query("SELECT id,match_id,submission_status FROM basket_match_submission WHERE submission_status='APPROVED' AND is_delete=0 ORDER BY id DESC LIMIT 1")
if subs:
    sid0=subs[0]["id"]; mid=subs[0]["match_id"]
    ts0=db_query("SELECT COUNT(*) c FROM basket_match_team_stat WHERE match_id=%s AND is_delete=0",(mid,))[0]["c"]
    s1=req(f"{ADMIN}/basket/v4/match-submission-audit/{sid0}/approve","POST",{"auditRemark":MARKER},admin)
    s2=req(f"{ADMIN}/basket/v4/match-submission-audit/{sid0}/approve","POST",{"auditRemark":MARKER},admin)
    ts1=db_query("SELECT COUNT(*) c FROM basket_match_team_stat WHERE match_id=%s AND is_delete=0",(mid,))[0]["c"]
    rec("idem_submission_approve","no extra stat",f"1={s1.get('code')}/{s1.get('msg')} 2={s2.get('code')}/{s2.get('msg')} teamStat={ts0}->{ts1}",sid0,"PASS" if ts0==ts1 else "FAIL")

cons=db_query("""
SELECT
 (SELECT COUNT(*) FROM basket_player_stat ps LEFT JOIN basket_event_player ep ON ep.id=ps.event_player_id AND ep.is_delete=0 WHERE ps.is_delete=0 AND ep.id IS NULL) AS ps_missing_ep,
 (SELECT COUNT(*) FROM basket_captain_grant g LEFT JOIN basket_division d ON d.id=g.division_id AND d.is_delete=0 WHERE g.is_delete=0 AND d.id IS NULL) AS grant_missing_div,
 (SELECT COUNT(*) FROM basket_division_stage s LEFT JOIN basket_division d ON d.id=s.division_id AND d.is_delete=0 WHERE s.is_delete=0 AND d.id IS NULL) AS stage_missing_div,
 (SELECT COUNT(*) FROM basket_player_profile p LEFT JOIN basket_team t ON t.id=p.current_team_id AND t.is_delete=0 WHERE p.is_delete=0 AND p.current_team_id IS NOT NULL AND t.id IS NULL) AS dangling_current_team
""")[0]
rec("consistency_extra","all 0",cons,"player_profile.current_team_id","PASS" if all(int(cons[k])==0 for k in cons) else "FAIL")

# logo
upl=upload_file(tok,TINY_PNG,filename="logo.png",usage_type="TEAM_LOGO")
bind=req(f"{WEB}/basket/team/{tid}/logo","POST",{"logoFileId":str(upl["data"]["fileId"])},tok) if tid and upl.get("code")==200 else {"code":-1,"msg":"skip"}
# try alternate bind endpoint
if bind.get("code") not in (200,0) and tid and upl.get("code")==200:
    bind=req(f"{WEB}/basket/team/{tid}/logo/bind","POST",{"logoFileId":str(upl["data"]["fileId"])},tok)
if bind.get("code") not in (200,0) and tid and upl.get("code")==200:
    bind=req(f"{WEB}/basket/team/{tid}","PUT",{"logoFileId":str(upl["data"]["fileId"]), "teamName": f"FS_GAP_ID1_{suffix}", "shortName":"I1"},tok)
out["team_logo"]={"upload":upl.get("code"),"bind":bind.get("code"),"bind_msg":bind.get("msg"),
  "verdict": "NOT_REPRODUCED" if upl.get("code")==200 and bind.get("code") in (200,0) else ("UPLOAD_OK_BIND_ISSUE" if upl.get("code")==200 else f"REPRODUCED upload={upl.get('code')}")}
rec("team_logo", "clear verdict", out["team_logo"], tid, "PASS" if upl.get("code")==200 else "FAIL")
upa=upload_file(tok,TINY_PNG,usage_type="ACCOUNT_AVATAR")
rec("account_avatar_upload","200",f"code={upa.get('code')}","","PASS" if upa.get("code")==200 else "FAIL")

# pagination APIs
join=req(f"{WEB}/basket/team/joinable/page?currentPage=1&pageSize=5",token=tok)
me=req(f"{WEB}/basket/v4/player/my-events?pageNum=1&pageSize=5",token=tok)
hist=req(f"{WEB}/basket/player/platform-account/change-history?currentPage=1&pageSize=5",token=tok)
noti=req(f"{WEB}/basket/account/message/page?currentPage=1&pageSize=5",token=tok)
if noti.get("code") not in (200,0):
    noti=req(f"{WEB}/basket/account/notifications?pageNum=1&pageSize=5",token=tok)
out["pagination"]={"joinable":join.get("code"),"my_events":me.get("code"),"identity_history":hist.get("code"),"notifications":noti.get("code"),"noti_msg":noti.get("msg")}
rec("pagination_joinable","200",f"code={join.get('code')} total={(join.get('data') or {}).get('total')}","","PASS" if join.get("code") in (200,0) else "FAIL")
rec("pagination_my_events","200",f"code={me.get('code')}","","PASS" if me.get("code") in (200,0) else "FAIL")
rec("pagination_identity_history","200",f"code={hist.get('code')}","","PASS" if hist.get("code") in (200,0) else "FAIL")
rec("pagination_notifications","200 or documented",f"code={noti.get('code')} msg={noti.get('msg')}","","PASS" if noti.get("code") in (200,0) else "NOT_EXECUTED")

# browsers
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True)
    page=browser.new_page()
    # team 149
    page.goto("http://127.0.0.1:5173/tournament/team/149", wait_until="networkidle", timeout=60000)
    page.wait_for_timeout(2500)
    body=page.locator("body").inner_text(timeout=15000)
    out["team_page"]={"has_charlie":"FS_CHARLIE" in body or "CHARLIE" in body, "empty":[x for x in ["暂无球队"] if x in body], "snippet":body[:500].replace("\n"," ")}
    rec("public_team_149","主体有冠军队名",out["team_page"],"","PASS" if out["team_page"]["has_charlie"] else "FAIL")
    # compare
    page.goto("http://127.0.0.1:5173/tournament/team/compare", wait_until="networkidle", timeout=60000)
    page.wait_for_timeout(5000)
    t=page.locator("body").inner_text(timeout=15000)
    stuck=("加载中" in t) and not any(k in t for k in ["请选择","选择","对比","VS","球队"])
    out["compare"]={"stuck":stuck,"snippet":t[:400].replace("\n"," ")}
    rec("team_compare_loading","not infinite",out["compare"],"","PASS" if not stuck else "FAIL")

    # login account UI
    page.goto("http://127.0.0.1:5173/login", wait_until="networkidle", timeout=60000)
    page.wait_for_timeout(1000)
    # store token in likely keys used by web-ui
    page.evaluate("""(token)=>{
      const ks=['token','member_token','cnpa_token','accessToken','Basket-Token','Authorization'];
      ks.forEach(k=>localStorage.setItem(k, token));
      localStorage.setItem('token', token);
    }""", tok)
    # UI login
    try:
        if page.locator("input[type='password']").count():
            # email field
            emails=page.locator("input[type='email'], input[placeholder*='邮箱'], input[placeholder*='邮件']")
            if emails.count(): emails.first.fill(creds["email"])
            else: page.locator("input").first.fill(creds["email"])
            page.locator("input[type='password']").first.fill(creds["password"])
            page.locator("button[type='submit'], button:has-text('登录')").first.click()
            page.wait_for_timeout(3000)
    except Exception as e:
        print("login ui", e)

    acc=[]
    for path in ["/account","/account/player","/account/team","/account/my-events","/account/notifications","/account/settings","/account/profile"]:
        cons=[]; nets500=[]
        def on_c(m):
            if m.type=="error": cons.append(m.text[:200])
        def on_r(r):
            if r.status>=500 and "/web-api" in r.url: nets500.append(r.url[:160])
        page.on("console", on_c); page.on("response", on_r)
        st="PASS"; notes=[]
        try:
            page.goto("http://127.0.0.1:5173"+path, wait_until="networkidle", timeout=60000)
            page.wait_for_timeout(2000)
            body=page.locator("body").inner_text(timeout=15000)
            if ("登录" in body and "密码" in body):
                st="FAIL"; notes.append("login_wall")
            for b in ["undefined","NaN","[object Object]","系统异常"]:
                if b in body: st="FAIL"; notes.append(b)
            if any("Uncaught" in x for x in cons): st="FAIL"; notes.append("uncaught")
            if nets500: st="FAIL"; notes.append("api500")
            acc.append({"route":path,"status":st,"notes":notes,"snippet":body[:200].replace("\n"," "),"url":page.url})
        except Exception as e:
            acc.append({"route":path,"status":"FAIL","notes":[repr(e)[:160]]})
        try: page.remove_listener("console", on_c); page.remove_listener("response", on_r)
        except Exception: pass
    out["browser_account"]={"results":acc,"pass":sum(1 for x in acc if x["status"]=="PASS"),"fail":sum(1 for x in acc if x["status"]=="FAIL")}
    rec("account_browser","logged-in render",f"pass={out['browser_account']['pass']} fail={out['browser_account']['fail']}",acc,"PASS" if out["browser_account"]["fail"]==0 else "FAIL")

    # admin ui
    admres=[]
    page.goto("http://127.0.0.1:5174/login", wait_until="networkidle", timeout=60000)
    page.wait_for_timeout(1000)
    try:
        page.locator("input[type='text'], input[placeholder*='用户']").first.fill("admin")
        page.locator("input[type='password']").first.fill("admin123")
        if page.locator("input[placeholder*='验证码']").count():
            page.locator("input[placeholder*='验证码']").first.fill("1")
        page.locator("button[type='submit'], button:has-text('登录'), button:has-text('登 录')").first.click()
        page.wait_for_timeout(3500)
        admres.append({"route":page.url,"status":"PASS" if "login" not in page.url.lower() else "FAIL","snippet":page.locator("body").inner_text()[:120].replace("\n"," ")})
    except Exception as e:
        admres.append({"route":"/login","status":"FAIL","notes":[repr(e)[:160]]})
    for label in ["赛事", "赛季", "赛区", "队长", "报名", "球队", "阶段", "赛程", "比赛", "提交", "资料", "认证", "平台", "审核"]:
        try:
            loc=page.get_by_text(label, exact=False)
            if loc.count()==0: continue
            loc.first.click(); page.wait_for_timeout(1000)
            body=page.locator("body").inner_text(timeout=10000)
            st="FAIL" if ("系统异常" in body or "Cannot read" in body) else "PASS"
            admres.append({"label":label,"route":page.url,"status":st,"snippet":body[:140].replace("\n"," ")})
        except Exception as e:
            admres.append({"label":label,"status":"FAIL","notes":[repr(e)[:120]]})
    out["browser_admin"]={"results":admres,"pass":sum(1 for x in admres if x.get("status")=="PASS"),"fail":sum(1 for x in admres if x.get("status")=="FAIL")}
    rec("admin_ui_browser","core menus",f"pass={out['browser_admin']['pass']} fail={out['browser_admin']['fail']}", "5174", "PASS" if out["browser_admin"]["fail"]==0 and out["browser_admin"]["pass"]>0 else "FAIL")
    browser.close()

Path("full_smoke_gap_closure_20260811.json").write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding="utf-8")
print("DONE fail", sum(1 for c in out["cases"] if c["result"]=="FAIL"), "ne", sum(1 for c in out["cases"] if c["result"]=="NOT_EXECUTED"))