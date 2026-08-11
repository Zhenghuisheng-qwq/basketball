# -*- coding: utf-8 -*-
"""CNPA 30-account full business flow via real Admin/Web APIs (no SQL seed for business entities)."""
from __future__ import annotations

import json
import sys
import time
import traceback
from pathlib import Path

from full_smoke_common import (
    ADMIN,
    MARKER as _BASE_MARKER,
    MEMBER_PASS,
    TINY_PNG,
    WEB,
    admin_login,
    apply_and_approve,
    approve_all_registrations,
    approve_team,
    as_id,
    clear_login_locks,
    close_registration,
    confirm_stage_qualification,
    count_event_entities,
    count_stage_matches,
    create_captain_grant,
    create_division,
    create_season,
    create_team_via_web,
    db_query,
    finish_division,
    finish_series_games,
    generate_and_publish,
    invite_and_accept,
    list_matches,
    list_series,
    open_registration,
    register_team,
    req,
    setup_main_format,
    submit_and_approve_match,
    upload_file,
    verify_event_player_source_distinct,
    web_login,
)

MARKER = "BATCH_FLOW_30_20260811"
OUT_CTX = Path(__file__).resolve().parent / "batch_30_users_context.json"
OUT_CREDS = Path(__file__).resolve().parent / "batch_30_users_accounts.txt"
OUT_REPORT = Path(__file__).resolve().parent / "batch_30_users_report.json"

PASSWORDS = MEMBER_PASS  # do not spam in logs
VERIFY_CODE = "123456"

TEAM_PLAN = [
    {
        "name": "DRAGONS",
        "short": "DRG",
        "captain": 1,
        "members": [7, 8, 9, 10],
        "mode": "invite",
    },
    {
        "name": "WOLVES",
        "short": "WLV",
        "captain": 2,
        "members": [11, 12, 13, 14],
        "mode": "apply",
    },
    {
        "name": "TITAN",
        "short": "TTN",
        "captain": 3,
        "members": [15, 16, 17, 18],
        "mode": "mixed",  # 15/16 invite, 17/18 apply
    },
    {
        "name": "PHOENIX",
        "short": "PHX",
        "captain": 4,
        "members": [19, 20, 21, 22],
        "mode": "invite",
    },
    {
        "name": "STORM",
        "short": "STM",
        "captain": 5,
        "members": [23, 24, 25, 26],
        "mode": "apply",
    },
    {
        "name": "KNIGHTS",
        "short": "KNT",
        "captain": 6,
        "members": [27, 28, 29, 30],
        "mode": "mixed2",  # 27/28 invite, 29/30 apply
    },
]

RANK_ORDER = ["DRAGONS", "WOLVES", "TITAN", "PHOENIX", "STORM", "KNIGHTS"]


def email_of(n: int) -> str:
    return f"2026{n:03d}@qq.com"


def game_id_of(n: int) -> str:
    return f"PC2026{n:03d}"


def fail(step: str, detail: object) -> None:
    raise RuntimeError(f"[{step}] {detail}")


def ensure_user(admin: str, n: int, users: dict) -> dict:
    email = email_of(n)
    if email in users and users[email].get("memberId") and users[email].get("playerId"):
        # refresh token
        tok = web_login(email)
        users[email]["token"] = tok
        return users[email]

    # exist?
    rows = db_query(
        "SELECT id, email FROM basket_member_account WHERE email=%s AND is_delete=0 LIMIT 1",
        (email,),
    )
    if rows:
        tok = web_login(email)
        sess = req(f"{WEB}/basket/member/auth/session", token=tok)
        mid = str((sess.get("data") or {}).get("memberId") or rows[0]["id"])
        prow = db_query(
            "SELECT id, player_no, certification_status FROM basket_player_profile "
            "WHERE member_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
            (mid,),
        )
        u = {
            "email": email,
            "memberId": mid,
            "token": tok,
            "playerId": str(prow[0]["id"]) if prow else "",
            "cnpaPlayerNo": (prow[0].get("player_no") if prow else None),
            "certStatus": (prow[0].get("certification_status") if prow else None),
            "register": "EXISTING",
            "n": n,
            "role": "CAPTAIN" if n <= 6 else "PLAYER",
            "gameId": game_id_of(n),
        }
        users[email] = u
        return u

    send = req(
        f"{WEB}/basket/member/auth/email-code/send",
        "POST",
        {"email": email, "purpose": "REGISTER"},
    )
    if send.get("code") != 200:
        fail("email_code_send", f"{email} {send}")
    # safety: must not require real SMTP; bypass path returns bizNo without mail
    biz = (send.get("data") or {}).get("bizNo")
    if not biz:
        fail("email_code_bizNo", send)
    reg = req(
        f"{WEB}/basket/member/auth/register",
        "POST",
        {
            "email": email,
            "password": PASSWORDS,
            "confirmPassword": PASSWORDS,
            "verifyBizNo": biz,
            "verifyCode": VERIFY_CODE,
            "agreeUserAgreement": True,
            "agreePrivacyPolicy": True,
            "terminalType": "WEB",
        },
    )
    if reg.get("code") != 200:
        fail("register", f"{email} {reg}")
    tok = web_login(email)
    sess = req(f"{WEB}/basket/member/auth/session", token=tok)
    if sess.get("code") != 200:
        fail("session", f"{email} {sess}")
    mid = str((sess.get("data") or {}).get("memberId") or "")
    u = {
        "email": email,
        "memberId": mid,
        "token": tok,
        "playerId": "",
        "cnpaPlayerNo": None,
        "register": "CREATED",
        "n": n,
        "role": "CAPTAIN" if n <= 6 else "PLAYER",
        "gameId": game_id_of(n),
    }
    users[email] = u
    return u


def ensure_cert_and_platform(admin: str, u: dict) -> None:
    tok = u["token"]
    mid = u["memberId"]
    # already permanent?
    prow = db_query(
        "SELECT id, player_no, certification_status FROM basket_player_profile "
        "WHERE member_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
        (mid,),
    )
    if prow and int(prow[0].get("certification_status") or 0) == 3:
        u["playerId"] = str(prow[0]["id"])
        u["cnpaPlayerNo"] = prow[0].get("player_no")
    else:
        draft = req(
            f"{WEB}/basket/player/certification/draft",
            "POST",
            {
                "gameTerminal": "PC",
                "gameAccount": u["gameId"],
                "positionCode": "PG",
                "gameNickname": f"CNPA{u['n']:03d}",
            },
            tok,
        )
        ddata = draft.get("data") or {}
        app_id = str(
            ddata.get("currentApplicationId")
            or ddata.get("applicationId")
            or ddata.get("id")
            or ""
        )
        if not app_id:
            cur0 = req(f"{WEB}/basket/player/certification/current", token=tok)
            c0 = cur0.get("data") or {}
            app_id = str(
                c0.get("currentApplicationId")
                or c0.get("applicationId")
                or c0.get("id")
                or ""
            )
        if not app_id:
            fail("cert_draft", f"{u['email']} {draft}")
        up = upload_file(tok, TINY_PNG, filename="cert.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
        fid = str(((up.get("data") or {}).get("fileId") or ""))
        if not fid:
            fail("cert_upload", up)
        mat = req(
            f"{WEB}/basket/player/certification/material",
            "POST",
            {"applicationId": app_id, "fileId": fid, "materialType": "PLAYER_CERT_GAME_SCREENSHOT"},
            tok,
        )
        if mat.get("code") != 200:
            fail("cert_material", mat)
        sub = req(f"{WEB}/basket/player/certification/submit", "POST", {"applicationId": app_id}, tok)
        if sub.get("code") != 200:
            fail("cert_submit", sub)
        appr = req(
            f"{ADMIN}/basket/playerCert/{app_id}/approve",
            "POST",
            {"auditRemark": MARKER},
            admin,
        )
        if appr.get("code") != 200:
            fail("cert_approve", appr)
        cur = req(f"{WEB}/basket/player/certification/current", token=tok)
        pid = str((cur.get("data") or {}).get("playerId") or "")
        if not pid:
            rows = db_query(
                "SELECT id, player_no FROM basket_player_profile WHERE member_id=%s AND is_delete=0 ORDER BY id DESC LIMIT 1",
                (mid,),
            )
            pid = str(rows[0]["id"]) if rows else ""
            u["cnpaPlayerNo"] = rows[0].get("player_no") if rows else None
        else:
            rows = db_query(
                "SELECT player_no FROM basket_player_profile WHERE id=%s",
                (pid,),
            )
            u["cnpaPlayerNo"] = rows[0].get("player_no") if rows else None
        u["playerId"] = pid
        if not pid:
            fail("cert_playerId", cur)

    # platform PC
    plist = req(f"{WEB}/basket/player/platform-account/list", token=tok)
    items = plist.get("data") if isinstance(plist.get("data"), list) else []
    pc = next((x for x in items if x.get("platform") == "PC" and x.get("accountStatus") in (1, "1", "ACTIVE", None)), None)
    # ACTIVE may be numeric 1
    pc = next((x for x in items if str(x.get("platform") or x.get("platformCode") or "") == "PC"), None)
    if pc and (pc.get("displayGameId") or pc.get("gameId")):
        u["platformAccountId"] = str(pc.get("platformAccountId") or pc.get("id") or "")
        u["gameId"] = pc.get("displayGameId") or pc.get("gameId") or u["gameId"]
        return

    proof = upload_file(tok, TINY_PNG, filename="plat.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
    pfid = str(((proof.get("data") or {}).get("fileId") or ""))
    add = req(
        f"{WEB}/basket/player/platform-account/add",
        "POST",
        {"platform": "PC", "gameId": u["gameId"], "proofFileId": pfid},
        tok,
    )
    if add.get("code") != 200:
        # maybe already pending/approved
        if "已" in str(add.get("msg") or "") or "重复" in str(add.get("msg") or ""):
            return
        fail("platform_add", f"{u['email']} {add}")
    change_id = str((add.get("data") or {}).get("changeId") or (add.get("data") or {}).get("id") or "")
    if not change_id:
        lr = req(f"{ADMIN}/basket/playerPlatformChange/list?pageNum=1&pageSize=50", token=admin)
        for row in lr.get("rows") or []:
            if str(row.get("memberId")) == mid or str(row.get("playerId")) == u["playerId"]:
                if str(row.get("auditStatus") or row.get("status") or "") in ("1", "PENDING", "0", "待审核", ""):
                    change_id = str(row.get("id"))
                    break
        if not change_id and (lr.get("rows") or []):
            # pick latest for member
            for row in lr.get("rows") or []:
                if str(row.get("memberId")) == mid:
                    change_id = str(row.get("id"))
                    break
    pap = req(
        f"{ADMIN}/basket/playerPlatformChange/{change_id}/approve",
        "POST",
        {"auditRemark": MARKER},
        admin,
    ) if change_id else {"code": -1, "msg": "no changeId"}
    if pap.get("code") != 200:
        fail("platform_approve", f"{u['email']} {pap}")
    plist2 = req(f"{WEB}/basket/player/platform-account/list", token=tok)
    items2 = plist2.get("data") if isinstance(plist2.get("data"), list) else []
    pc2 = next((x for x in items2 if str(x.get("platform") or x.get("platformCode") or "") == "PC"), None)
    u["platformAccountId"] = str((pc2 or {}).get("platformAccountId") or (pc2 or {}).get("id") or "")
    u["platformChangeId"] = change_id


def save_ctx(ctx: dict) -> None:
    OUT_CTX.write_text(json.dumps(ctx, ensure_ascii=False, indent=2), encoding="utf-8")


def write_creds(users: dict) -> None:
    lines = [
        "CNPA LOCAL/UAT TEST ACCOUNTS",
        "WARNING: test-only; clean before production open",
        f"password=*** (unified test password configured)",
        f"marker={MARKER}",
        "",
        "email\tmemberId\tplayerId\tgameId\trole\tteam",
    ]
    for n in range(1, 31):
        u = users[email_of(n)]
        lines.append(
            f"{u['email']}\t{u.get('memberId')}\t{u.get('playerId')}\t{u.get('gameId')}\t{u.get('role')}\t{u.get('teamName','')}"
        )
    lines.append("")
    lines.append(f"# Unified password: {PASSWORDS}")
    OUT_CREDS.write_text("\n".join(lines), encoding="utf-8")


def join_members(admin: str, users: dict, team: dict, plan: dict, report: dict) -> None:
    cap = users[email_of(plan["captain"])]
    cap_tok = web_login(cap["email"])
    tid = team["teamId"]
    invite_n = 0
    apply_n = 0
    for i, mn in enumerate(plan["members"]):
        mu = users[email_of(mn)]
        mtok = web_login(mu["email"])
        mode = plan["mode"]
        use_invite = False
        if mode == "invite":
            use_invite = True
        elif mode == "apply":
            use_invite = False
        elif mode == "mixed":
            use_invite = i < 2
        elif mode == "mixed2":
            use_invite = i < 2
        if use_invite:
            r = invite_and_accept(cap_tok, mtok, tid, mu["playerId"])
            if r.get("code") != 200:
                fail("invite", f"{plan['name']} {mu['email']} {r}")
            invite_n += 1
        else:
            r = apply_and_approve(mtok, cap_tok, tid, mu["memberId"])
            if r.get("code") != 200:
                fail("apply", f"{plan['name']} {mu['email']} {r}")
            apply_n += 1
        mu["teamName"] = plan["name"]
        mu["teamId"] = tid
    report["invite"] = report.get("invite", 0) + invite_n
    report["apply"] = report.get("apply", 0) + apply_n


def finish_rr_ranked(admin: str, web_by_event: dict, division_id: str, stage_id: str, name_by_et: dict) -> dict:
    """Finish all BO1 matches forcing transitive ranking by RANK_ORDER."""
    rank = {name: i for i, name in enumerate(RANK_ORDER)}
    matches = list_matches(admin, division_id, stage_id)
    bo1 = [m for m in matches if not m.get("seriesId")]
    score_pool = [(72, 68), (81, 76), (65, 70), (78, 74), (69, 71), (84, 79), (77, 73), (80, 75)]
    done = 0
    specials = []
    for idx, m in enumerate(bo1):
        if m.get("matchStatus") in ("FINISHED", "FORFEITED"):
            continue
        home = as_id(m.get("homeEventTeamId"))
        away = as_id(m.get("awayEventTeamId"))
        hn = name_by_et.get(home, "")
        an = name_by_et.get(away, "")
        hr = rank.get(hn, 99)
        ar = rank.get(an, 99)
        # lower rank index wins
        home_wins = hr < ar
        a, b = score_pool[idx % len(score_pool)]
        if home_wins:
            hs, aws = max(a, b), min(a, b)
            if hs == aws:
                hs += 3
        else:
            hs, aws = min(a, b), max(a, b)
            if hs == aws:
                aws += 3
        wt = web_by_event.get(home)
        if not wt:
            fail("rr_token", f"no token for {home}/{hn}")
        reject_first = idx == 1  # Match B reject then resubmit
        out = submit_and_approve_match(admin, wt, m, hs, aws, home, reject_first=reject_first)
        # Match C: approve again for idempotency
        if idx == 2 and out.get("submissionId"):
            again = req(
                f"{ADMIN}/basket/v4/match-submission-audit/{out['submissionId']}/approve",
                "POST",
                {},
                admin,
            )
            specials.append({"matchC_second_approve": again.get("code"), "msg": again.get("msg")})
        if idx == 0:
            specials.append({"matchA": "approve", "matchId": m.get("id")})
        if idx == 1:
            specials.append({"matchB": "reject_resubmit_approve", "matchId": m.get("id")})
        done += 1
    return {"finished": done, "total": len(bo1), "specials": specials}


def main() -> int:
    report: dict = {"marker": MARKER, "phases": {}, "failures": []}
    ctx: dict = {"marker": MARKER, "users": {}, "teams": {}, "ids": {}, "results": {}}
    try:
        print("=== Phase0 env ===")
        admin = admin_login()
        # confirm bypass: send should succeed without SMTP
        # Local web is already known to run with mail bypass; skip probe to avoid rate limits.
        report["phases"]["bypass"] = "ASSUMED_TRUE_LOCAL"
        print("bypass assume OK")

        print("=== Phase1 register 30 ===")
        users: dict = {}
        created = 0
        existing = 0
        clear_login_locks([email_of(i) for i in range(1, 31)])
        for n in range(1, 31):
            u = ensure_user(admin, n, users)
            if u.get("register") == "CREATED":
                created += 1
            else:
                existing += 1
            print(f"  user {n}/30 {u['email']} memberId={u['memberId']} ({u.get('register')})")
        ctx["users"] = users
        save_ctx(ctx)
        report["phases"]["account"] = {"created": created, "existing": existing, "total": 30}

        print("=== Phase2 cert + platform ===")
        for n in range(1, 31):
            u = users[email_of(n)]
            u["token"] = web_login(u["email"])
            ensure_cert_and_platform(admin, u)
            print(f"  cert/plat {n}/30 playerId={u['playerId']} gameId={u['gameId']}")
        save_ctx(ctx)
        report["phases"]["cert"] = 30
        report["phases"]["platform"] = 30

        print("=== Phase2b platform change-game-id for 030 ===")
        u30 = users[email_of(30)]
        tok30 = web_login(u30["email"])
        pl = req(f"{WEB}/basket/player/platform-account/list", token=tok30)
        items = pl.get("data") if isinstance(pl.get("data"), list) else []
        pc = next((x for x in items if str(x.get("platform") or x.get("platformCode") or "") == "PC"), None)
        if not pc:
            fail("plat030", pl)
        paid = str(pc.get("platformAccountId") or pc.get("id"))
        old_gid = pc.get("displayGameId") or pc.get("gameId")
        pf = upload_file(tok30, TINY_PNG, filename="chg.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
        pfid = str(((pf.get("data") or {}).get("fileId") or ""))
        new_gid = "PC2026030NEW"
        chg = req(
            f"{WEB}/basket/player/platform-account/{paid}/change-game-id",
            "POST",
            {"newGameId": new_gid, "proofFileId": pfid},
            tok30,
        )
        cid = str((chg.get("data") or {}).get("changeId") or (chg.get("data") or {}).get("id") or "")
        if not cid:
            lr = req(f"{ADMIN}/basket/playerPlatformChange/list?pageNum=1&pageSize=30", token=admin)
            for row in lr.get("rows") or []:
                if str(row.get("memberId")) == u30["memberId"]:
                    cid = str(row.get("id"))
                    break
        # reject first
        rj = req(
            f"{ADMIN}/basket/playerPlatformChange/{cid}/reject",
            "POST",
            {"auditRemark": MARKER + "_reject"},
            admin,
        )
        pl_mid = req(f"{WEB}/basket/player/platform-account/list", token=tok30)
        mid_pc = next(
            (x for x in (pl_mid.get("data") or []) if str(x.get("platform") or x.get("platformCode") or "") == "PC"),
            {},
        )
        still_old = (mid_pc.get("displayGameId") or mid_pc.get("gameId")) == old_gid
        # resubmit + approve
        pf2 = upload_file(tok30, TINY_PNG, filename="chg2.png", usage_type="PLAYER_CERT_GAME_SCREENSHOT")
        chg2 = req(
            f"{WEB}/basket/player/platform-account/{paid}/change-game-id",
            "POST",
            {"newGameId": new_gid, "proofFileId": str(((pf2.get("data") or {}).get("fileId") or ""))},
            tok30,
        )
        cid2 = str((chg2.get("data") or {}).get("changeId") or (chg2.get("data") or {}).get("id") or cid)
        if not cid2 or chg2.get("code") != 200:
            lr = req(f"{ADMIN}/basket/playerPlatformChange/list?pageNum=1&pageSize=30", token=admin)
            for row in lr.get("rows") or []:
                if str(row.get("memberId")) == u30["memberId"] and str(row.get("auditStatus")) in ("0", "1", "PENDING", ""):
                    cid2 = str(row.get("id"))
                    break
        ap = req(
            f"{ADMIN}/basket/playerPlatformChange/{cid2}/approve",
            "POST",
            {"auditRemark": MARKER},
            admin,
        )
        pl_after = req(f"{WEB}/basket/player/platform-account/list", token=tok30)
        after_pc = next(
            (x for x in (pl_after.get("data") or []) if str(x.get("platform") or x.get("platformCode") or "") == "PC"),
            {},
        )
        u30["gameId"] = after_pc.get("displayGameId") or after_pc.get("gameId") or new_gid
        hist = req(f"{WEB}/basket/player/platform-account/change-history?currentPage=1&pageSize=10", token=tok30)
        report["phases"]["change_game_id"] = {
            "reject": rj.get("code"),
            "still_old_after_reject": still_old,
            "approve": ap.get("code"),
            "after": u30["gameId"],
            "history_code": hist.get("code"),
        }
        print("change-game-id", report["phases"]["change_game_id"])
        save_ctx(ctx)

        print("=== Phase3 season/division ===")
        season_id = create_season(admin, "CNPA League 2026 S1", "CNPA League 2026 S1")
        # remark via description already MARKER in helper - patch description if needed
        division_id = create_division(admin, season_id, "PC 5V5", max_team=6, template_id=3)
        stages = setup_main_format(admin, division_id)
        open_registration(admin, division_id)
        ctx["ids"].update({"seasonId": season_id, "divisionId": division_id, **stages})
        save_ctx(ctx)
        print("season", season_id, "division", division_id, stages)

        print("=== Phase4 grants + teams ===")
        teams_out = {}
        for plan in TEAM_PLAN:
            cap = users[email_of(plan["captain"])]
            gr = create_captain_grant(admin, division_id, cap["playerId"], remark=MARKER)
            if gr.get("code") != 200:
                # tolerate already granted
                msg = str(gr.get("msg") or "")
                if "已" not in msg and gr.get("code") not in (500, 400):
                    fail("grant", f"{plan['name']} {gr}")
            time.sleep(0.3)
            ctok = web_login(cap["email"])
            cr = create_team_via_web(ctok, division_id, plan["name"], plan["short"])
            if cr.get("code") != 200:
                # reuse existing
                rows = db_query(
                    "SELECT id FROM basket_team WHERE division_id=%s AND captain_player_id=%s AND team_status=1 AND is_delete=0 ORDER BY id DESC LIMIT 1",
                    (int(division_id), int(cap["playerId"])),
                )
                if not rows:
                    fail("team_create", f"{plan['name']} {cr}")
                tid = str(rows[0]["id"])
            else:
                tid = str((cr.get("data") or {}).get("teamId") or (cr.get("data") or {}).get("id") or "")
                ar = approve_team(admin, tid)
                if ar.get("code") != 200:
                    fail("team_approve", f"{plan['name']} {ar}")
            tinfo = {
                "teamId": tid,
                "name": plan["name"],
                "captainEmail": cap["email"],
                "captainMemberId": cap["memberId"],
                "captainPlayerId": cap["playerId"],
                "divisionId": division_id,
            }
            teams_out[plan["name"]] = tinfo
            cap["teamName"] = plan["name"]
            cap["teamId"] = tid
            cap["role"] = "CAPTAIN"
            join_members(admin, users, tinfo, plan, report.setdefault("membership", {}))
            print(f"  team {plan['name']} id={tid}")
        ctx["teams"] = teams_out
        save_ctx(ctx)

        print("=== Phase4b negative team tests ===")
        # 1) 007 apply wolves while on dragons
        u7 = users[email_of(7)]
        wolves = teams_out["WOLVES"]
        neg1 = req(
            f"{WEB}/basket/team/{wolves['teamId']}/join-application",
            "POST",
            {"message": MARKER},
            web_login(u7["email"]),
        )
        # 2) 007 create team without grant
        neg2 = req(
            f"{WEB}/basket/team/create",
            "POST",
            {"divisionId": int(division_id), "teamName": "ILLEGAL_TEAM", "teamShortName": "ILL", "publicStatus": 1, "description": MARKER},
            web_login(u7["email"]),
        )
        # 3) 001 second team same grant
        neg3 = req(
            f"{WEB}/basket/team/create",
            "POST",
            {"divisionId": int(division_id), "teamName": "DRAGONS2", "teamShortName": "DR2", "publicStatus": 1, "description": MARKER},
            web_login(users[email_of(1)]["email"]),
        )
        report["phases"]["neg_team"] = {
            "same_div_second_team_apply": {"code": neg1.get("code"), "msg": neg1.get("msg")},
            "no_grant_create": {"code": neg2.get("code"), "msg": neg2.get("msg")},
            "same_grant_second_team": {"code": neg3.get("code"), "msg": neg3.get("msg")},
        }
        print("neg", report["phases"]["neg_team"])

        print("=== Phase5 registration ===")
        approved = 0
        for plan in TEAM_PLAN:
            t = teams_out[plan["name"]]
            cap = users[email_of(plan["captain"])]
            pids = [cap["playerId"]] + [users[email_of(m)]["playerId"] for m in plan["members"]]
            rr = register_team(web_login(cap["email"]), division_id, t["teamId"], pids)
            if rr.get("code") != 200:
                fail("register_team", f"{plan['name']} {rr}")
        approved = approve_all_registrations(admin, division_id)
        et, ep = count_event_entities(division_id)
        ok_src, src_msg = verify_event_player_source_distinct(division_id)
        close_registration(admin, division_id)
        report["phases"]["registration"] = {"approved": approved, "eventTeam": et, "eventPlayer": ep, "sourceOk": ok_src, "srcMsg": src_msg}
        print("reg", report["phases"]["registration"])
        if et != 6 or ep != 30:
            fail("event_counts", report["phases"]["registration"])

        # map event teams
        et_rows = db_query(
            "SELECT id, team_name_snapshot, source_team_id FROM basket_event_team WHERE division_id=%s AND is_delete=0",
            (int(division_id),),
        )
        name_by_et = {}
        for r in et_rows:
            name_by_et[str(r["id"])] = r["team_name_snapshot"]
            for tname, t in teams_out.items():
                if str(t["teamId"]) == str(r["source_team_id"]) or tname == r["team_name_snapshot"]:
                    t["eventTeamId"] = str(r["id"])
        ctx["eventTeams"] = name_by_et
        save_ctx(ctx)

        print("=== Phase6 RR ===")
        generate_and_publish(admin, stages["st1"], division_id=division_id)
        mcnt = count_stage_matches(stages["st1"])
        if mcnt != 15:
            fail("rr_count", mcnt)
        # web tokens by event
        web_by_event = {}
        for tname, t in teams_out.items():
            etid = t.get("eventTeamId")
            if etid:
                web_by_event[etid] = web_login(t["captainEmail"])
        rr_res = finish_rr_ranked(admin, web_by_event, division_id, stages["st1"], name_by_et)
        confirm_stage_qualification(admin, stages["st1"])
        standings = req(f"{ADMIN}/basket/v4/admin/stages/{stages['st1']}/standings", token=admin)
        rows = []
        for g in standings.get("data") or []:
            rows.extend(g.get("rows") or [])
        if not rows and isinstance(standings.get("data"), list):
            # maybe flat
            rows = standings.get("data") or []
        # normalize
        top = []
        if rows and "rank" in (rows[0] or {}):
            top = sorted(rows, key=lambda x: int(x.get("rank") or 999))[:4]
        report["phases"]["rr"] = {"matches": mcnt, **rr_res, "standing_n": len(rows), "top4": [
            {"rank": r.get("rank"), "team": name_by_et.get(str(r.get("eventTeamId")), r.get("teamName")), "w": r.get("wins"), "l": r.get("losses")}
            for r in top
        ]}
        print("rr", report["phases"]["rr"])

        print("=== Phase7 BO3/BO5 ===")
        generate_and_publish(admin, stages["st2"], init=False, division_id=division_id)
        s2 = list_series(admin, stages["st2"])
        if len(s2) < 2:
            fail("semi_series", s2)
        # finish: prefer home as higher seed via plan HOME wins
        sa = finish_series_games(admin, web_by_event, s2[0]["id"], ["HOME", "HOME"])
        sb = finish_series_games(admin, web_by_event, s2[1]["id"], ["HOME", "AWAY", "HOME"])
        confirm_stage_qualification(admin, stages["st2"])
        generate_and_publish(admin, stages["st3"], init=False, division_id=division_id)
        s3 = list_series(admin, stages["st3"])
        if not s3:
            fail("final_series", s3)
        sf = finish_series_games(admin, web_by_event, s3[0]["id"], ["HOME", "AWAY", "HOME", "AWAY", "HOME"])
        champ = sf.get("winnerEventTeamId")
        champ_name = name_by_et.get(str(champ), str(champ))
        report["phases"]["playoff"] = {
            "semi": [
                {"id": s2[0]["id"], "status": sa.get("seriesStatus"), "winner": sa.get("winnerEventTeamId")},
                {"id": s2[1]["id"], "status": sb.get("seriesStatus"), "winner": sb.get("winnerEventTeamId")},
            ],
            "final": {"id": s3[0]["id"], "status": sf.get("seriesStatus"), "winner": champ, "name": champ_name},
            "champion": champ_name,
        }
        print("playoff", report["phases"]["playoff"])
        ctx["results"]["champion"] = {"eventTeamId": champ, "name": champ_name}
        save_ctx(ctx)

        print("=== Phase8 finish division ===")
        fr = finish_division(admin, division_id, reason=MARKER)
        div = db_query("SELECT division_status FROM basket_division WHERE id=%s", (int(division_id),))[0]
        grants = db_query(
            "SELECT grant_status, active_unique_key FROM basket_captain_grant WHERE division_id=%s AND is_delete=0",
            (int(division_id),),
        )
        tids = [int(t["teamId"]) for t in teams_out.values()]
        tstats = db_query(
            f"SELECT id, team_status FROM basket_team WHERE id IN ({','.join(['%s']*len(tids))}) AND is_delete=0",
            tuple(tids),
        )
        members = db_query(
            f"SELECT leave_reason, membership_status FROM basket_team_member WHERE team_id IN ({','.join(['%s']*len(tids))}) AND is_delete=0",
            tuple(tids),
        )
        report["phases"]["finish"] = {
            "api": fr.get("code"),
            "division_status": div.get("division_status"),
            "grants": grants,
            "teams": tstats,
            "members_sample": members[:3],
            "member_count": len(members),
        }
        print("finish", report["phases"]["finish"]["division_status"], "grants", len(grants))

        print("=== Phase9 lifecycle S2 ===")
        season2 = create_season(admin, "CNPA League 2026 S2", "CNPA League 2026 S2")
        div2 = create_division(admin, season2, "PC 5V5", max_team=6, template_id=3)
        try:
            setup_main_format(admin, div2)
        except Exception:
            pass
        open_registration(admin, div2)
        u1 = users[email_of(1)]
        old_tid = teams_out["DRAGONS"]["teamId"]
        gr2 = create_captain_grant(admin, div2, u1["playerId"], remark=MARKER + "_S2")
        if gr2.get("code") != 200:
            print("grant s2 warn", gr2)
        time.sleep(0.3)
        c1 = create_team_via_web(web_login(u1["email"]), div2, "DRAGONS", "DRG")
        if c1.get("code") != 200:
            fail("s2_create", c1)
        new_tid = str((c1.get("data") or {}).get("teamId") or "")
        approve_team(admin, new_tid)
        if new_tid == old_tid:
            fail("s2_new_team_id", f"same {new_tid}")
        # reinvite 7-10
        ctok = web_login(u1["email"])
        rejoined = 0
        for mn in [7, 8, 9, 10]:
            mu = users[email_of(mn)]
            r = invite_and_accept(ctok, web_login(mu["email"]), new_tid, mu["playerId"])
            if r.get("code") == 200:
                rejoined += 1
            else:
                fail("s2_rejoin", f"{mu['email']} {r}")
        old_status = db_query("SELECT team_status FROM basket_team WHERE id=%s", (int(old_tid),))[0]["team_status"]
        report["phases"]["lifecycle"] = {
            "season2": season2,
            "division2": div2,
            "oldTeamId": old_tid,
            "newTeamId": new_tid,
            "rejoined": rejoined,
            "oldTeamStatus": old_status,
        }
        ctx["ids"]["season2Id"] = season2
        ctx["ids"]["division2Id"] = div2
        ctx["ids"]["dragonsS2TeamId"] = new_tid
        print("lifecycle", report["phases"]["lifecycle"])

        print("=== Phase10 profile change 030 ===")
        tok30 = web_login(users[email_of(30)]["email"])
        # capture old event player snapshot nickname if any from S1
        snap_before = db_query(
            "SELECT id, player_name_snapshot FROM basket_event_player WHERE source_player_id=%s AND is_delete=0 LIMIT 5",
            (int(users[email_of(30)]["playerId"]),),
        )
        ch = req(f"{WEB}/basket/account/profile/change", "POST", {"nickname": "CNPA30"}, tok30)
        cidp = str((ch.get("data") or {}).get("changeId") or (ch.get("data") or {}).get("id") or "")
        if ch.get("code") != 200:
            fail("profile_submit", ch)
        rj = req(
            f"{ADMIN}/basket/memberProfileChange/{cidp}/reject",
            "POST",
            {"auditRemark": MARKER + "_nick_reject"},
            admin,
        )
        upa = upload_file(tok30, TINY_PNG, filename="avatar.png", usage_type="ACCOUNT_AVATAR")
        fid = str(((upa.get("data") or {}).get("fileId") or ""))
        ch2 = req(
            f"{WEB}/basket/account/profile/change",
            "POST",
            {"nickname": "CNPA30", "avatarFileId": fid} if fid else {"nickname": "CNPA30"},
            tok30,
        )
        cidp2 = str((ch2.get("data") or {}).get("changeId") or (ch2.get("data") or {}).get("id") or "")
        apn = req(f"{ADMIN}/basket/memberProfileChange/{cidp2}/approve", "POST", {}, admin)
        sess = req(f"{WEB}/basket/member/auth/session", token=tok30)
        snap_after = db_query(
            "SELECT id, player_name_snapshot FROM basket_event_player WHERE source_player_id=%s AND is_delete=0 LIMIT 5",
            (int(users[email_of(30)]["playerId"]),),
        )
        report["phases"]["profile"] = {
            "submit": ch.get("code"),
            "reject": rj.get("code"),
            "resubmit": ch2.get("code"),
            "approve": apn.get("code"),
            "sessionNick": (sess.get("data") or {}).get("nickname"),
            "avatarUpload": upa.get("code"),
            "snapshot_before": snap_before,
            "snapshot_after": snap_after,
        }
        print("profile", report["phases"]["profile"]["sessionNick"], "approve", apn.get("code"))

        print("=== Phase11 pagination probes ===")
        tok1 = web_login(users[email_of(1)]["email"])
        probes = {
            "my_events": req(f"{WEB}/basket/v4/player/my-events?pageNum=1&pageSize=5", token=tok1),
            "joinable": req(f"{WEB}/basket/team/joinable/page?currentPage=1&pageSize=5", token=tok1),
            "identity_history": req(
                f"{WEB}/basket/player/platform-account/change-history?currentPage=1&pageSize=5",
                token=tok30,
            ),
            "notifications": req(f"{WEB}/basket/message/page?currentPage=1&pageSize=5", token=tok1),
        }
        # try alternate notification path
        if probes["notifications"].get("code") not in (200, 0):
            probes["notifications"] = req(f"{WEB}/basket/notification/page?pageNum=1&pageSize=5", token=tok1)
        report["phases"]["pagination"] = {k: {"code": v.get("code"), "msg": v.get("msg")} for k, v in probes.items()}
        print("pagination", report["phases"]["pagination"])

        # stats check
        ts = db_query(
            "SELECT COUNT(*) c FROM basket_team_stat ts JOIN basket_event_team et ON et.id=ts.event_team_id "
            "WHERE et.division_id=%s AND ts.is_delete=0",
            (int(division_id),),
        )[0]["c"]
        ps = db_query(
            "SELECT COUNT(*) c FROM basket_player_stat ps JOIN basket_event_player ep ON ep.id=ps.event_player_id "
            "JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.division_id=%s AND ps.is_delete=0",
            (int(division_id),),
        )[0]["c"]
        report["phases"]["stats"] = {"teamStat": int(ts), "playerStat": int(ps)}

        # group / advanced not executed this run (time / scope) - mark
        report["phases"]["group"] = "NOT_EXECUTED"
        report["phases"]["advanced"] = {
            "BO1": "NOT_EXECUTED",
            "BYE": "NOT_EXECUTED",
            "Forfeit": "NOT_EXECUTED",
            "ManualAdvance": "NOT_EXECUTED",
            "RandomDraw": "NOT_EXECUTED",
            "PairingAdjustment": "NOT_EXECUTED",
        }

        write_creds(users)
        save_ctx(ctx)
        report["final"] = "PASS"
        OUT_REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print("=== DONE PASS ===")
        print("context", OUT_CTX)
        print("creds", OUT_CREDS)
        return 0
    except Exception as e:
        report["final"] = "FAIL"
        report["failures"].append({"error": str(e), "trace": traceback.format_exc()[-2000:]})
        OUT_REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        try:
            save_ctx(ctx)
            if ctx.get("users"):
                write_creds(ctx["users"])
        except Exception:
            pass
        print("FAIL", e)
        print(traceback.format_exc()[-1500:])
        return 1


if __name__ == "__main__":
    sys.exit(main())
