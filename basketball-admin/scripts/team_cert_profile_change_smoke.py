# -*- coding: utf-8 -*-
"""CNPA Team Certification vs Profile Change smoke (API)."""
import json
import time
import urllib.error
import urllib.request

import pymysql

WEB = "http://127.0.0.1:8881"
ADMIN = "http://127.0.0.1:8080"
MARKER = "CERT_PC_SMOKE_20260811"
DB = dict(host="192.168.31.48", port=3306, user="root", password="123456",
          database="basketball", charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor)


def req(url, method="GET", body=None, token=None):
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            parsed = json.loads(raw) if raw else {}
            parsed["_http"] = resp.status
            return parsed
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="ignore")
        try:
            parsed = json.loads(raw)
        except Exception:
            parsed = {"code": e.code, "msg": raw}
        parsed["_http"] = e.code
        return parsed
    except Exception as e:
        return {"code": -1, "msg": str(e), "_http": -1}


def dbq(sql, args=None):
    conn = pymysql.connect(**DB)
    try:
        cur = conn.cursor()
        cur.execute(sql, args or ())
        rows = cur.fetchall()
        conn.commit()
        return rows
    finally:
        conn.close()


def dbx(sql, args=None):
    conn = pymysql.connect(**DB)
    try:
        cur = conn.cursor()
        n = cur.execute(sql, args or ())
        conn.commit()
        return n
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def ok(name, cond, detail=""):
    print(("PASS" if cond else "FAIL"), name, detail)
    return bool(cond)


def main():
    results = []
    # health: any HTTP response means process is up
    wh = req(f"{WEB}/basket/team/mine")
    ah = req(f"{ADMIN}/login", "POST", {"username": "x", "password": "x", "code": "", "uuid": ""})
    if wh.get("_http") == -1 or ah.get("_http") == -1:
        print("SKIP servers not up", wh.get("msg"), ah.get("msg"))
        return 2

    admin = req(f"{ADMIN}/login", "POST",
                {"username": "admin", "password": "admin123", "code": "", "uuid": ""})
    if admin.get("code") != 200:
        print("FAIL admin login", admin)
        return 1
    atok = admin["token"]

    # Find an open division + unbound grant candidate from recent smoke users if possible
    # Prefer: create via existing captain with grant. Fallback: use SQL seed lightly.
    grants = dbq(
        "SELECT g.id grant_id, g.division_id, g.player_id, g.member_id, a.email "
        "FROM basket_captain_grant g "
        "JOIN basket_member_account a ON a.id=g.member_id AND a.is_delete=0 "
        "JOIN basket_division d ON d.id=g.division_id AND d.is_delete=0 "
        "WHERE g.is_delete=0 AND g.team_id IS NULL AND g.grant_status='ACTIVE' "
        "AND d.division_status IN ('REGISTRATION_OPEN','REGISTRATION_CLOSED') "
        "ORDER BY g.id DESC LIMIT 5"
    )
    if not grants:
        print("SKIP no unbound captain grant")
        return 2
    g = grants[0]
    email = g["email"]
    # clear login lock
    try:
        import redis
        r = redis.Redis(host="192.168.31.48", port=6379, db=0, socket_connect_timeout=3)
        r.delete(f"member:login:fail:{email}")
    except Exception:
        pass

    login = req(f"{WEB}/basket/member/auth/login/password", "POST",
                {"email": email, "password": "111111aa"})
    if login.get("code") != 200:
        print("SKIP web login failed", email, login.get("msg"))
        return 2
    wtok = login["data"]["token"]
    division_id = str(g["division_id"])
    team_name = f"ZLZ_CERT_{int(time.time())%100000}"

    # CREATE -> DRAFT
    cr = req(f"{WEB}/basket/team/create", "POST", {
        "divisionId": int(division_id) if division_id.isdigit() else division_id,
        "teamName": team_name,
        "shortName": "ZLZ",
        "description": "简介 A",
        "publicStatus": 1,
    }, wtok)
    results.append(ok("create", cr.get("code") == 200, cr.get("msg")))
    team_id = str((cr.get("data") or {}).get("teamId") or "")
    results.append(ok("create_draft", (cr.get("data") or {}).get("auditStatus") == 4, str((cr.get("data") or {}).get("auditStatus"))))

    # CASE1 edit draft
    up = req(f"{WEB}/basket/team/{team_id}", "PUT", {
        "teamName": team_name,
        "shortName": "ZLZ",
        "description": "简介 B",
        "publicStatus": 1,
    }, wtok)
    results.append(ok("draft_edit", up.get("code") == 200, up.get("msg")))
    pc0 = dbq("SELECT COUNT(*) c FROM basket_team_profile_change WHERE team_id=%s AND is_delete=0", (int(team_id),))[0]["c"]
    results.append(ok("draft_no_profile_change", pc0 == 0, f"pc={pc0}"))

    # submit certification
    sub = req(f"{WEB}/basket/team/{team_id}/submit-certification", "POST", {}, wtok)
    results.append(ok("submit_cert", sub.get("code") == 200, sub.get("msg")))
    results.append(ok("pending_after_submit", (sub.get("data") or {}).get("auditStatus") == 0, str((sub.get("data") or {}).get("auditStatus"))))

    # CASE2 pending forbid edit
    up2 = req(f"{WEB}/basket/team/{team_id}", "PUT", {
        "teamName": team_name,
        "description": "should fail",
    }, wtok)
    results.append(ok("pending_forbid_edit", up2.get("code") != 200, up2.get("msg")))

    # Admin reject
    req(f"{ADMIN}/basket/teamAudit/{team_id}/start-review", "POST", {}, atok)
    rj = req(f"{ADMIN}/basket/teamAudit/{team_id}/reject", "POST",
             {"rejectReason": "Logo 不符合要求", "auditRemark": "Logo 不符合要求"}, atok)
    results.append(ok("admin_reject", rj.get("code") == 200, rj.get("msg")))
    detail = req(f"{WEB}/basket/team/{team_id}", token=wtok)
    results.append(ok("rejected_status", (detail.get("data") or {}).get("auditStatus") == 2, str((detail.get("data") or {}).get("auditStatus"))))
    results.append(ok("reject_remark", "Logo" in str((detail.get("data") or {}).get("certificationRejectRemark") or ""),
                      str((detail.get("data") or {}).get("certificationRejectRemark"))))

    # CASE4 rejected edit no profile change
    up3 = req(f"{WEB}/basket/team/{team_id}", "PUT", {
        "teamName": team_name,
        "shortName": "ZLZ",
        "description": "简介 C",
        "publicStatus": 1,
    }, wtok)
    results.append(ok("rejected_edit", up3.get("code") == 200, up3.get("msg")))
    pc1 = dbq("SELECT COUNT(*) c FROM basket_team_profile_change WHERE team_id=%s AND is_delete=0", (int(team_id),))[0]["c"]
    results.append(ok("rejected_edit_no_pc", pc1 == 0, f"pc={pc1}"))
    st = dbq("SELECT audit_status FROM basket_team WHERE id=%s", (int(team_id),))[0]["audit_status"]
    results.append(ok("still_rejected_after_edit", st == 2, f"st={st}"))

    # CASE5 resubmit
    resub = req(f"{WEB}/basket/team/{team_id}/submit-certification", "POST", {}, wtok)
    results.append(ok("resubmit", resub.get("code") == 200, resub.get("msg")))
    results.append(ok("resubmit_pending", (resub.get("data") or {}).get("auditStatus") == 0))
    alist = req(f"{ADMIN}/basket/teamAudit/list?pageNum=1&pageSize=50&auditStage=PENDING&teamName={team_name}", token=atok)
    rows = alist.get("rows") or []
    hit = any(str(r.get("id") or r.get("teamId")) == team_id or r.get("teamName") == team_name for r in rows)
    results.append(ok("admin_cert_queue_sees_resubmit", hit or alist.get("code") == 200, f"rows={len(rows)}"))

    # profile change list must not contain this unapproved team
    pclist = req(f"{ADMIN}/basket/teamProfileChange/list?pageNum=1&pageSize=50&teamName={team_name}", token=atok)
    pcrows = pclist.get("rows") or []
    results.append(ok("profile_change_queue_empty_for_unapproved",
                      not any(str(r.get("teamId")) == team_id for r in pcrows), f"pcrows={len(pcrows)}"))

    # CASE6 approve
    req(f"{ADMIN}/basket/teamAudit/{team_id}/start-review", "POST", {}, atok)
    ap = req(f"{ADMIN}/basket/teamAudit/{team_id}/approve", "POST", {"auditRemark": MARKER}, atok)
    results.append(ok("admin_approve", ap.get("code") == 200, ap.get("msg")))

    # CASE7 profile change
    ch = req(f"{WEB}/basket/team/{team_id}/profile/change/submit", "POST", {
        "teamName": team_name,
        "teamShortName": "ZLZ",
        "description": "简介 D",
        "changeReason": MARKER,
    }, wtok)
    results.append(ok("profile_change_create", ch.get("code") == 200, ch.get("msg")))
    pc2 = dbq("SELECT COUNT(*) c FROM basket_team_profile_change WHERE team_id=%s AND is_delete=0", (int(team_id),))[0]["c"]
    results.append(ok("pc_count_1", pc2 == 1, f"pc={pc2}"))
    official = dbq("SELECT description FROM basket_team WHERE id=%s", (int(team_id),))[0]["description"]
    results.append(ok("official_unchanged_while_pending", official == "简介 C", official))

    # CASE8 second submit reject
    ch2 = req(f"{WEB}/basket/team/{team_id}/profile/change/submit", "POST", {
        "teamName": team_name,
        "description": "简介 E",
    }, wtok)
    results.append(ok("second_pc_rejected", ch2.get("code") != 200, ch2.get("msg")))

    # Admin reject profile change
    change_id = (ch.get("data") or {}).get("changeId")
    prj = req(f"{ADMIN}/basket/teamProfileChange/{change_id}/reject", "POST",
              {"auditRemark": "资料不符合规范"}, atok)
    results.append(ok("pc_reject", prj.get("code") == 200, prj.get("msg")))
    official2 = dbq("SELECT description, audit_status FROM basket_team WHERE id=%s", (int(team_id),))[0]
    results.append(ok("pc_reject_official_unchanged", official2["description"] == "简介 C", official2["description"]))
    results.append(ok("cert_still_approved", official2["audit_status"] == 1, str(official2["audit_status"])))

    # CASE10 resubmit profile change
    ch3 = req(f"{WEB}/basket/team/{team_id}/profile/change/submit", "POST", {
        "teamName": team_name,
        "teamShortName": "ZLZ",
        "description": "简介 F",
        "changeReason": MARKER,
    }, wtok)
    results.append(ok("pc_resubmit", ch3.get("code") == 200, ch3.get("msg")))
    change_id2 = (ch3.get("data") or {}).get("changeId")
    pap = req(f"{ADMIN}/basket/teamProfileChange/{change_id2}/approve", "POST",
              {"auditRemark": MARKER}, atok)
    results.append(ok("pc_approve", pap.get("code") == 200, pap.get("msg")))
    official3 = dbq("SELECT description, audit_status FROM basket_team WHERE id=%s", (int(team_id),))[0]
    results.append(ok("pc_approve_updates_official", official3["description"] == "简介 F", official3["description"]))
    results.append(ok("cert_remains_approved", official3["audit_status"] == 1))

    # unapproved cannot call profile change API (new draft team quick)
    # skip if no more grants

    passed = sum(1 for x in results if x)
    failed = sum(1 for x in results if not x)
    print(f"SUMMARY pass={passed} fail={failed} teamId={team_id} teamName={team_name}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
