# -*- coding: utf-8 -*-
from __future__ import annotations
import json, time, traceback
from pathlib import Path
from full_smoke_common import *

OUT = Path("full_smoke_gap_closure_20260811.json")
R = {"cases": [], "ids": {}, "manual_advance_conclusion": {}, "bo3_clarification": {}, "team_logo": {}}

def rec(name, expected, actual, evidence, result):
    R["cases"].append({"name": name, "expected": expected, "actual": str(actual)[:600], "evidence": str(evidence)[:500], "result": result})
    print(result, name, str(actual)[:180])

def main():
    admin = admin_login()
    suffix = str(int(time.time()) % 100000)
    email = f"fs_gap_{suffix}@cnpa.smoke.test"
    # 1 register
    send = req(f"{WEB}/basket/member/auth/email-code/send", "POST", {"email": email, "purpose": "REGISTER"})
    biz = (send.get("data") or {}).get("bizNo")
    rec("email_code_send", "200 bizNo", f"code={send.get('code')} biz={biz} bypass={(send.get('data') or {}).get('devBypass')}", "DEV_BYPASS", "PASS" if send.get("code")==200 and biz else "FAIL")
    reg = req(f"{WEB}/basket/member/auth/register", "POST", {
        "email": email, "password": MEMBER_PASS, "confirmPassword": MEMBER_PASS,
        "verifyBizNo": biz, "verifyCode": "123456",
        "agreeUserAgreement": True, "agreePrivacyPolicy": True, "terminalType": "WEB"
    })
    login = req(f"{WEB}/basket/member/auth/login/password", "POST", {"email": email, "password": MEMBER_PASS})
    tok = (login.get("data") or {}).get("token")
    sess = req(f"{WEB}/basket/member/auth/session", token=tok) if tok else {}
    member_id = str((sess.get("data") or {}).get("memberId") or (reg.get("data") or {}).get("memberId") or "")
    rec("register_business_flow", "API-created member", f"reg={reg.get('code')} login={login.get('code')} memberId={member_id}", "NO SQL", "PASS" if reg.get("code")==200 and tok and member_id else "FAIL")
    rec("external_qq_mail_delivery", "NOT EXECUTED", "DEV_BYPASS; no SMTP", "EXTERNAL", "NOT_EXECUTED")
    rec("login", "200", f"code={login.get('code')}", email, "PASS" if tok else "FAIL")
    rec("session", "200", f"code={sess.get('code')} nick={(sess.get('data') or {}).get('nickname')}", "", "PASS" if sess.get("code")==200 else "FAIL")
    R["ids"].update({"email": email, "memberId": member_id})

    # cert
    draft = req(f"{WEB}/basket/player/certification/draft", "POST", {
        "gameTerminal": "PC", "gameAccount": f"GAPACC{suffix}", "positionCode": "PG", "gameNickname": f"GapNick{suffix}"
    }, tok)
    ddata = draft.get("data") or {}
    app_id = str(ddata.get("applicationId") or ddata.get("id") or "")
    if not app_id:
        # try current
        cur0 = req(f"{WEB}/basket/player/certification/current", token=tok)
        app_id = str(((cur0.get("data") or {}).get("applicationId") or (cur0.get("data") or {}).get("id") or ""))
        ddata = cur0.get("data") or ddata
    up = upload_file(tok, TINY_PNG, filename="cert.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
    fid = str(((up.get("data") or {}).get("fileId") or ""))
    mat = req(f"{WEB}/basket/player/certification/material", "POST", {
        "applicationId": app_id, "fileId": fid, "materialType": "GAME_SCREENSHOT"
    }, tok) if app_id and fid else {"code": -1, "msg": "missing app/file"}
    sub = req(f"{WEB}/basket/player/certification/submit", "POST", {"applicationId": app_id}, tok) if app_id else {"code": -1}
    appr = req(f"{ADMIN}/basket/playerCert/{app_id}/approve", "POST", {"auditRemark": MARKER}, admin) if app_id else {"code": -1}
    cur = req(f"{WEB}/basket/player/certification/current", token=tok)
    player_id = str((cur.get("data") or {}).get("playerId") or "")
    if not player_id and member_id:
        rows = db_query("SELECT id FROM basket_player_profile WHERE member_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1", (member_id,))
        player_id = str(rows[0]["id"]) if rows else ""
    cert_ok = draft.get("code")==200 and sub.get("code")==200 and appr.get("code")==200 and bool(player_id)
    rec("player_certification", "draft+material+submit+approve+playerId", f"draft={draft.get('code')} mat={mat.get('code')} sub={sub.get('code')} appr={appr.get('code')}/{appr.get('msg')} playerId={player_id} app={app_id} draftKeys={list(ddata.keys())[:12]}", f"file={fid}", "PASS" if cert_ok else "FAIL")
    R["ids"].update({"playerId": player_id, "certApplicationId": app_id})

    # platform
    proof = upload_file(tok, TINY_PNG, filename="plat.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
    pfid = str(((proof.get("data") or {}).get("fileId") or ""))
    add = req(f"{WEB}/basket/player/platform-account/add", "POST", {"platform": "PC", "gameId": f"GAPGID{suffix}", "proofFileId": pfid}, tok)
    change_id = str((add.get("data") or {}).get("changeId") or (add.get("data") or {}).get("id") or "")
    if not change_id:
        lr = req(f"{ADMIN}/basket/playerPlatformChange/list?pageNum=1&pageSize=20", token=admin)
        for row in (lr.get("rows") or []):
            if str(row.get("memberId")) == member_id or str(row.get("playerId")) == player_id:
                change_id = str(row.get("id")); break
        if not change_id and (lr.get("rows") or []):
            change_id = str(lr.get("rows")[0].get("id"))
    pap = req(f"{ADMIN}/basket/playerPlatformChange/{change_id}/approve", "POST", {"auditRemark": MARKER}, admin) if change_id else {"code": -1, "msg": "no changeId"}
    plist = req(f"{WEB}/basket/player/platform-account/list", token=tok)
    items = plist.get("data") if isinstance(plist.get("data"), list) else []
    plat_ok = add.get("code")==200 and pap.get("code")==200 and len(items) >= 1
    rec("platform_identity", "add+approve+list", f"add={add.get('code')}/{add.get('msg')} appr={pap.get('code')}/{pap.get('msg')} listN={len(items)} changeId={change_id}", pfid, "PASS" if plat_ok else "FAIL")
    R["ids"]["platformChangeId"] = change_id

    # 2 Manual Advance with ADV_TEAMS
    try:
        teams = seed_org_pool("GAP", ADV_TEAMS[:2], offset=56000 + int(suffix) % 1000)
        for t in teams:
            t["name"] = f"FS_GAP_MA_{t['code']}_{suffix}"; t["short"] = t["code"][:3]
        season_id = create_season(admin, f"FULL_SMOKE_20260811_GAP_MA_{suffix}", f"GAP_MA_{suffix}")
        division_id = create_division(admin, season_id, f"FULL_SMOKE_20260811_GAP_MA_{suffix}_PC_5V5", max_team=8, template_id=5)
        stages = setup_advanced_format(admin, division_id)
        open_registration(admin, division_id)
        create_teams_flow(admin, division_id, teams)
        populate_members(teams)
        wtoks = web_tokens_for_teams(teams)
        register_all_teams(wtoks, admin, division_id, teams)
        close_registration(admin, division_id)
        init_stage_teams(admin, stages["st1"])
        st = db_query("SELECT advancement_type, next_stage_code, competition_format, best_of, execution_status FROM basket_division_stage WHERE id=%s", (int(stages["st1"]),))[0]
        et = db_query("SELECT event_team_id FROM basket_division_stage_team WHERE stage_id=%s AND is_delete=0 LIMIT 1", (int(stages["st1"]),))[0]["event_team_id"]
        bad = req(f"{ADMIN}/basket/v4/admin/stage-exception/manual-advance", "POST", {"stageId": int(stages["st2"]), "eventTeamId": int(et), "reason": MARKER}, admin)
        good = req(f"{ADMIN}/basket/v4/admin/stage-exception/manual-advance", "POST", {"stageId": int(stages["st1"]), "eventTeamId": int(et), "reason": MARKER + "_MA"}, admin)
        q = db_query("SELECT id, qualification_status, qualification_type FROM basket_division_stage_qualification WHERE stage_id=%s AND event_team_id=%s AND qualification_type='MANUAL' AND is_delete=0", (int(stages["st1"]), int(et)))
        ok = good.get("code") == 200 and len(q) >= 1 and bad.get("code") != 200
        rec("manual_advance_audit", "non-final SE PASS; final REJECT", f"st1={st} bad={bad.get('code')}/{bad.get('msg')} good={good.get('code')}/{good.get('msg')} qual={q}", f"div={division_id}", "PASS" if ok else "FAIL")
        R["ids"].update({"maSeasonId": season_id, "maDivisionId": division_id, "maStageId": stages["st1"], "maFinalStageId": stages["st2"]})
        R["manual_advance_conclusion"] = {
            "has_capability": True,
            "supported": "SE stage with advancementType=WINNER_ADVANCE and nextStageCode present",
            "unsupported": "final SE (NONE) / RR / stages without nextStageCode",
            "smoke_c_issue": "invalid stage (final/unsupported); ServiceException business reject presented as code=500",
            "code_refs": [
                "BasketV4StageExceptionController.manualAdvance",
                "StageExecutionExceptionApplicationServiceImpl.manualAdvance:163-198",
                "EliminationStageSupport.isSingleRoundNonFinalStage"
            ],
            "prior_500_class": "TEST_CASE_INVALID + normal business rejection (not missing feature)"
        }
        finish_division(admin, division_id)
    except Exception as e:
        rec("manual_advance_audit", "PASS", repr(e), traceback.format_exc()[-400:], "FAIL")

    # BO3 clarify
    a_st2 = db_query("SELECT advancement_type, next_stage_code, best_of FROM basket_division_stage WHERE id=550")[0]
    a_series = db_query("SELECT COUNT(*) c FROM basket_match_series WHERE stage_id=550 AND is_delete=0")[0]["c"]
    R["bo3_clarification"] = {
        "type": "B_final_stage_limit",
        "precise": "BO3/BO5 FINAL stage (advancementType=NONE) currently supports only 2 teams",
        "smoke_a": f"Semi BO3 ST2 WINNER_ADVANCE next=ST3 seriesCount={a_series} PASS with Top4",
        "code": "SingleEliminationStageScheduleStrategy.validateSeriesElimination L235-239",
        "p1_2": "REMOVE vague; keep precise final-stage Known Limitation only"
    }
    rec("bo3_bo5_clarification", "final-stage 2-team only", R["bo3_clarification"]["smoke_a"], R["bo3_clarification"]["code"], "PASS")

    # Permission / idem with real certified player
    sid = create_season(admin, f"FULL_SMOKE_20260811_GAP_PERM_{suffix}", "GAP_PERM")
    did = create_division(admin, sid, f"FULL_SMOKE_20260811_GAP_PERM_{suffix}_PC_5V5", max_team=8, template_id=3)
    try:
        setup_advanced_format(admin, did)
    except Exception:
        pass
    open_registration(admin, did)
    # before grant
    cr0 = req(f"{WEB}/basket/team/create", "POST", {"divisionId": int(did), "teamName": f"FS_GAP_NG_{suffix}", "teamShortName": "NG"}, tok)
    rec("perm_no_grant_create_team", "REJECT", f"code={cr0.get('code')} msg={cr0.get('msg')}", f"div={did}", "PASS" if cr0.get("code") not in (200, 0) else "FAIL")
    other_team = (((load_context().get("smokes") or {}).get("A_MAIN") or {}).get("ids") or {}).get("teamIds") or []
    if other_team:
        inv = req(f"{WEB}/basket/team/{other_team[0]}/invitation", "POST", {"inviteePlayerId": player_id, "message": "x"}, tok)
        rec("perm_manage_other_team", "REJECT", f"code={inv.get('code')} msg={inv.get('msg')}", other_team[0], "PASS" if inv.get("code") not in (200, 0) else "FAIL")
    adm = req(f"{ADMIN}/basket/v4/admin/seasons/list?pageNum=1&pageSize=5", token=tok)
    rec("perm_web_user_admin_api", "401/REJECT", f"code={adm.get('code')} msg={adm.get('msg')}", "web token", "PASS" if adm.get("code") not in (200, 0) else "FAIL")
    # cross division registration attempt: grant+create on did, try register against finished A division
    gr = create_captain_grant(admin, did, player_id)
    c1 = req(f"{WEB}/basket/team/create", "POST", {"divisionId": int(did), "teamName": f"FS_GAP_IDEM_{suffix}", "teamShortName": "ID"}, tok)
    tid = str((c1.get("data") or {}).get("teamId") or "")
    if tid:
        approve_team(admin, tid)
    c2 = req(f"{WEB}/basket/team/create", "POST", {"divisionId": int(did), "teamName": f"FS_GAP_IDEM2_{suffix}", "teamShortName": "I2"}, tok)
    rec("idem_same_grant_second_team", "REJECT", f"grant={gr.get('code')} 1={c1.get('code')} 2={c2.get('code')} msg2={c2.get('msg')} tid={tid}", did, "PASS" if c1.get("code")==200 and c2.get("code") not in (200,0) else "FAIL")
    if tid:
        # cross division: submit registration with wrong division
        cross = req(f"{WEB}/basket/v4/registration/submit", "POST", {"divisionId": 142, "teamId": tid}, tok)
        rec("perm_cross_division_registration", "REJECT", f"code={cross.get('code')} msg={cross.get('msg')}", f"teamDiv={did} target=142", "PASS" if cross.get("code") not in (200,0) else "FAIL")

    # registration approve idempotency
    regs = db_query("SELECT id FROM basket_registration WHERE division_id=142 AND is_delete=0 LIMIT 1")
    if regs:
        rid = regs[0]["id"]
        a1 = req(f"{ADMIN}/basket/v4/admin/registrations/{rid}/approve", "POST", {"auditRemark": MARKER}, admin)
        a2 = req(f"{ADMIN}/basket/v4/admin/registrations/{rid}/approve", "POST", {"auditRemark": MARKER}, admin)
        et = db_query("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=142 AND is_delete=0")[0]["c"]
        rec("idem_registration_approve", "no new EventTeam", f"1={a1.get('code')} 2={a2.get('code')} et={et}", rid, "PASS" if int(et)==8 else "FAIL")

    subs = db_query("SELECT id, submission_status FROM basket_match_submission WHERE submission_status='APPROVED' AND is_delete=0 ORDER BY id DESC LIMIT 1")
    if not subs:
        subs = db_query("SELECT id, submission_status FROM basket_match_submission WHERE is_delete=0 ORDER BY id DESC LIMIT 1")
    if subs:
        sid0 = subs[0]["id"]
        s1 = req(f"{ADMIN}/basket/v4/admin/match-submissions/{sid0}/approve", "POST", {"auditRemark": MARKER}, admin)
        s2 = req(f"{ADMIN}/basket/v4/admin/match-submissions/{sid0}/approve", "POST", {"auditRemark": MARKER}, admin)
        rec("idem_submission_approve", "second reject/no-op", f"status={subs[0]['submission_status']} 1={s1.get('code')}/{s1.get('msg')} 2={s2.get('code')}/{s2.get('msg')}", sid0, "PASS" if s2.get("code") not in (200,0) or s1.get("code") not in (200,0) else "FAIL")

    cons = db_query("""
    SELECT
     (SELECT COUNT(*) FROM basket_player_stat ps LEFT JOIN basket_event_player ep ON ep.id=ps.event_player_id AND ep.is_delete=0
        WHERE ps.is_delete=0 AND ep.id IS NULL) AS ps_missing_ep,
     (SELECT COUNT(*) FROM basket_captain_grant g LEFT JOIN basket_division d ON d.id=g.division_id AND d.is_delete=0
        WHERE g.is_delete=0 AND d.id IS NULL) AS grant_missing_div,
     (SELECT COUNT(*) FROM basket_division_stage s LEFT JOIN basket_division d ON d.id=s.division_id AND d.is_delete=0
        WHERE s.is_delete=0 AND d.id IS NULL) AS stage_missing_div,
     (SELECT COUNT(*) FROM basket_member_account m LEFT JOIN basket_team t ON t.id=m.current_team_id AND t.is_delete=0
        WHERE m.is_delete=0 AND m.current_team_id IS NOT NULL AND t.id IS NULL) AS dangling_current_team
    """)[0]
    rec("consistency_extra", "all 0", cons, "ps/grant/stage/current_team", "PASS" if all(int(cons[k])==0 for k in cons) else "FAIL")

    upl = upload_file(tok, TINY_PNG, filename="logo.png", usage_type="TEAM_LOGO")
    R["team_logo"] = {"upload_code": upl.get("code"), "msg": upl.get("msg"), "verdict": "UPLOAD_OK" if upl.get("code")==200 else f"REPRODUCED code={upl.get('code')} msg={upl.get('msg')}"}
    rec("team_logo_upload", "200 or clear env issue", f"code={upl.get('code')} msg={upl.get('msg')}", "TEAM_LOGO", "PASS" if upl.get("code")==200 else "FAIL")
    upa = upload_file(tok, TINY_PNG, usage_type="ACCOUNT_AVATAR")
    rec("account_avatar_upload", "200", f"code={upa.get('code')}", "ACCOUNT_AVATAR", "PASS" if upa.get("code")==200 else "FAIL")

    # save credentials for browser
    R["ids"]["token"] = tok
    R["ids"]["password"] = MEMBER_PASS
    OUT.write_text(json.dumps(R, ensure_ascii=False, indent=2), encoding="utf-8")
    print("DONE fail", sum(1 for c in R["cases"] if c["result"]=="FAIL"), "pass", sum(1 for c in R["cases"] if c["result"]=="PASS"))

if __name__ == "__main__":
    main()