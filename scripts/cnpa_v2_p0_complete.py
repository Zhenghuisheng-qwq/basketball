#!/usr/bin/env python3
"""Complete standing + personal formation for P0 regression."""
import json, os, urllib.request, urllib.error
from pathlib import Path
import pymysql

ROOT = Path(__file__).resolve().parents[1]
for line in (ROOT / "scripts" / ".env.v2.local").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip())

admin_base = "http://127.0.0.1:8080"
web_base = "http://127.0.0.1:8881"
pwd = os.environ.get("CNPA_MEMBER_PASSWORD", "111111aa")


def http(base, method, path, body=None, token=None, allow=False):
    url = base + path
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            p = json.loads(r.read())
            p["_http_status"] = r.status
    except urllib.error.HTTPError as e:
        p = json.loads(e.read() or b"{}")
        p["_http_status"] = e.code
        if not allow:
            raise RuntimeError(f"{method} {path} {p}")
    else:
        if not allow and p.get("code") not in (200, None):
            raise RuntimeError(f"{method} {path} {p}")
    return p


def login(email):
    return http(web_base, "POST", "/basket/member/auth/login/password",
                {"email": email, "password": pwd})["data"]["token"]


def db():
    return pymysql.connect(
        host=os.environ["CNPA_DB_HOST"], user=os.environ["CNPA_DB_USER"],
        password=os.environ["CNPA_DB_PASSWORD"], database=os.environ["CNPA_DB_NAME"],
        charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor,
    )


def complete_standing():
    admin = http(admin_base, "POST", "/login", {"username": "admin", "password": "admin123"})["token"]
    emails = ["111111@qq.com", "111141@qq.com", "111142@qq.com", "111143@qq.com"]
    tokens = {e: login(e) for e in emails}
    conn = db()
    cur = conn.cursor()
    cur.execute(
        "SELECT id, home_event_team_id, away_event_team_id, match_status FROM basket_match "
        "WHERE division_id=10 AND stage_id=5 AND is_delete=0 ORDER BY id"
    )
    for m in cur.fetchall():
        if m["match_status"] == "FINISHED":
            continue
        mid = m["id"]
        for et_id in [m["home_event_team_id"], m["away_event_team_id"]]:
            cur.execute(
                "SELECT ma.email FROM basket_event_team et "
                "JOIN basket_team t ON t.id=et.source_team_id "
                "JOIN basket_member_account ma ON ma.id=t.captain_member_id WHERE et.id=%s",
                (et_id,),
            )
            row = cur.fetchone()
            tok = tokens.get(row["email"])
            if not tok:
                print("SKIP no token", et_id, row)
                continue
            sv = http(web_base, "POST", "/basket/v4/player/match-submission/save", {
                "matchId": mid, "submitEventTeamId": et_id, "homeScore": 72, "awayScore": 68,
            }, token=tok, allow=True)
            sid = sv.get("data")
            if sid:
                http(web_base, "POST", "/basket/v4/player/match-submission/submit",
                     {"submissionId": sid}, token=tok, allow=True)
                ar = http(admin_base, "POST", f"/basket/v4/match-submission-audit/{sid}/approve",
                          {"auditRemark": "p0 complete"}, token=admin, allow=True)
                print("match", mid, "team", et_id, "sub", sid, "approve", ar.get("code"))
    http(admin_base, "POST", "/basket/v4/admin/stages/5/check-completion", token=admin, allow=True)
    cur.execute(
        "SELECT COUNT(*) AS c FROM basket_match WHERE division_id=10 AND stage_id=5 "
        "AND match_status='FINISHED' AND is_delete=0"
    )
    print("finished_matches", cur.fetchone()["c"])
    cur.execute(
        "SELECT event_team_id, rank_no, win_count, lose_count, point_for, point_against, standing_points "
        "FROM basket_standing WHERE division_id=10 AND stage_id=5 AND is_delete=0 ORDER BY rank_no"
    )
    rows = cur.fetchall()
    print("standing_rows", len(rows), rows)
    conn.close()
    return rows


def complete_personal():
    conn = db()
    cur = conn.cursor()
    cur.execute(
        "SELECT dt.id, ma.email FROM basket_personal_draw_team dt "
        "JOIN basket_member_account ma ON ma.id=dt.captain_user_id "
        "WHERE dt.division_id=11 AND dt.converted_registration_id IS NULL AND dt.is_delete=0"
    )
    teams = cur.fetchall()
    conn.close()
    for t in teams:
        dt_id = t["id"]
        email = t["email"]
        print("forming drawTeam", dt_id, email)
        ct = login(email)
        conn = db()
        cur = conn.cursor()
        cur.execute(
            "SELECT a.player_id, pp.member_id FROM basket_personal_draw_assignment a "
            "JOIN basket_player_profile pp ON pp.id=a.player_id "
            "WHERE a.draw_team_id=%s AND a.is_delete=0", (dt_id,)
        )
        members = cur.fetchall()
        cur.execute("SELECT captain_player_id FROM basket_personal_draw_team WHERE id=%s", (dt_id,))
        captain_player = cur.fetchone()["captain_player_id"]
        conn.close()

        tc = http(web_base, "POST", "/basket/team/create", {
            "teamName": f"P0-Personal-{dt_id}", "gameTerminal": "PS", "publicStatus": 0,
        }, token=ct, allow=True)
        team_id = (tc.get("data") or {}).get("teamId")
        if not team_id:
            mine = http(web_base, "GET", "/basket/team/mine?pageSize=20", token=ct, allow=True)
            recs = ((mine.get("data") or {}).get("records") or [])
            if recs:
                team_id = recs[0].get("teamId")
        print("  team_id", team_id, tc.get("msg"))
        if not team_id:
            continue
        for m in members:
            if str(m["player_id"]) == str(captain_player):
                continue
            inv = http(web_base, "POST", f"/basket/team/{team_id}/invitation", {
                "inviteePlayerId": str(m["player_id"]),
            }, token=ct, allow=True)
            inv_id = (inv.get("data") or {}).get("invitationId")
            print("  invite", m["player_id"], inv.get("msg"), inv_id)
            if inv_id:
                cur2 = db().cursor()
                cur2.execute("SELECT email FROM basket_member_account WHERE id=%s", (m["member_id"],))
                mem_email = cur2.fetchone()["email"]
                cur2.connection.close()
                mt = login(mem_email)
                resp = http(web_base, "PUT", f"/basket/team/invitation/{inv_id}/respond",
                             {"action": "ACCEPT"}, token=mt, allow=True)
                print("  accept", mem_email, resp.get("msg"))
        av = http(web_base, "GET", f"/basket/v4/player/personal-formation/tasks/{dt_id}/available-teams",
                  token=ct, allow=True)
        print("  available", av.get("data"))
        bd = http(web_base, "POST", "/basket/v4/player/personal-formation/bind-team", {
            "drawTeamId": dt_id, "longTermTeamId": int(team_id),
        }, token=ct, allow=True)
        print("  bind", bd.get("msg"))
        cf = http(web_base, "POST", f"/basket/v4/player/personal-formation/tasks/{dt_id}/confirm",
                  token=ct, allow=True)
        print("  confirm", cf.get("data"), cf.get("msg"))

    conn = db()
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=11 AND is_delete=0")
    print("personal_event_teams", cur.fetchone()["c"])
    cur.execute(
        "SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        "WHERE et.division_id=11 AND ep.is_delete=0"
    )
    print("personal_event_players", cur.fetchone()["c"])
    conn.close()


if __name__ == "__main__":
    print("=== STANDING ===")
    complete_standing()
    print("=== PERSONAL ===")
    complete_personal()
