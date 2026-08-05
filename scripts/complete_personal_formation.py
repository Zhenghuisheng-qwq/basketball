#!/usr/bin/env python3
"""Complete PERSONAL formation via SQL team seed + admin HTTP bind/confirm."""
import json
import os
import urllib.request
from pathlib import Path
import pymysql

ROOT = Path(__file__).resolve().parents[1]
for line in (ROOT / "scripts" / ".env.v2.local").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip())

ADMIN = "http://127.0.0.1:8080"


def db():
    return pymysql.connect(
        host=os.environ["CNPA_DB_HOST"], user=os.environ["CNPA_DB_USER"],
        password=os.environ["CNPA_DB_PASSWORD"], database=os.environ["CNPA_DB_NAME"],
        charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor,
    )


def admin_token():
    req = urllib.request.Request(
        ADMIN + "/login", json.dumps({"username": "admin", "password": "admin123"}).encode(),
        {"Content-Type": "application/json"}, method="POST",
    )
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read())["token"]


def admin_post(path, body, token):
    req = urllib.request.Request(
        ADMIN + path, json.dumps(body).encode() if body is not None else None,
        {"Content-Type": "application/json", "Authorization": "Bearer " + token}, method="POST",
    )
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read())


def ensure_team(cur, draw_team_id, team_name, captain_member, captain_player, members):
    cur.execute("SELECT long_term_team_id FROM basket_personal_draw_team WHERE id=%s", (draw_team_id,))
    row = cur.fetchone()
    if row and row["long_term_team_id"]:
        return row["long_term_team_id"]

    team_no = f"P0DT{draw_team_id}"
    cur.execute("SELECT id FROM basket_team WHERE team_no=%s", (team_no,))
    existing = cur.fetchone()
    if existing:
        team_id = existing["id"]
    else:
        cur.execute(
            "INSERT INTO basket_team (team_no, team_name, short_name, game_terminal, captain_member_id, "
            "captain_player_id, contact_member_id, max_member_count, current_member_count, team_status, "
            "audit_status, public_status, established_date, is_delete) "
            "VALUES (%s,%s,%s,'PS',%s,%s,%s,20,%s,1,1,0,CURDATE(),0)",
            (team_no, team_name, team_name[:32], captain_member, captain_player, captain_member, len(members)),
        )
        team_id = cur.lastrowid
    for idx, m in enumerate(members):
        mem_no = f"{team_no}-M{idx+1}"
        cur.execute(
            "SELECT id FROM basket_team_member WHERE team_id=%s AND player_id=%s AND is_delete=0",
            (team_id, m["player_id"]),
        )
        if cur.fetchone():
            continue
        role = "CAPTAIN" if m["player_id"] == captain_player else "MEMBER"
        cur.execute(
            "INSERT INTO basket_team_member (membership_no, team_id, member_id, player_id, role_type, "
            "roster_type, join_source, membership_status, is_current, join_time, is_delete) "
            "VALUES (%s,%s,%s,%s,%s,'OFFICIAL','P0_SMOKE',1,1,NOW(),0)",
            (mem_no, team_id, m["member_id"], m["player_id"], role),
        )
    return team_id


def main():
    conn = db()
    cur = conn.cursor()
    cur.execute(
        "SELECT dt.id AS draw_team_id, dt.captain_player_id, dt.captain_user_id, "
        "a.player_id, pp.member_id "
        "FROM basket_personal_draw_team dt "
        "JOIN basket_personal_draw_assignment a ON a.draw_team_id=dt.id "
        "JOIN basket_player_profile pp ON pp.id=a.player_id "
        "WHERE dt.division_id=11 AND dt.is_delete=0 AND a.is_delete=0 ORDER BY dt.id, a.id"
    )
    grouped = {}
    for row in cur.fetchall():
        grouped.setdefault(row["draw_team_id"], {"captain_player": row["captain_player_id"],
                                                   "captain_member": row["captain_user_id"], "members": []})
        grouped[row["draw_team_id"]]["members"].append(
            {"player_id": row["player_id"], "member_id": row["member_id"]}
        )

    team_ids = {}
    for dt_id, info in grouped.items():
        tid = ensure_team(cur, dt_id, f"P0 Personal Team {dt_id}", info["captain_member"],
                          info["captain_player"], info["members"])
        team_ids[dt_id] = tid
        print(f"drawTeam {dt_id} -> longTermTeam {tid}")
    conn.commit()
    conn.close()

    token = admin_token()
    for dt_id, tid in team_ids.items():
        r1 = admin_post(f"/basket/v4/admin/personal-draw-batches/teams/{dt_id}/bind-team",
                        {"longTermTeamId": tid}, token)
        print(f"bind {dt_id}:", r1.get("code"), r1.get("msg"))
        r2 = admin_post(f"/basket/v4/admin/personal-draw-batches/teams/{dt_id}/confirm", None, token)
        print(f"confirm {dt_id}:", r2.get("code"), r2.get("data"), r2.get("msg"))

    conn = db()
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) c FROM basket_event_team WHERE division_id=11 AND is_delete=0")
    print("event_teams:", cur.fetchone()["c"])
    cur.execute(
        "SELECT COUNT(*) c FROM basket_event_player ep JOIN basket_event_team et ON et.id=ep.event_team_id "
        "WHERE et.division_id=11 AND ep.is_delete=0"
    )
    print("event_players:", cur.fetchone()["c"])
    cur.execute(
        "SELECT COUNT(*) c FROM basket_registration WHERE division_id=11 "
        "AND registration_source='PERSONAL_FORMATION' AND is_delete=0"
    )
    print("formation_regs:", cur.fetchone()["c"])
    cur.execute("SELECT id FROM basket_event_team WHERE division_id=11 AND is_delete=0")
    print("event_team_ids:", [r["id"] for r in cur.fetchall()])
    cur.execute("SELECT id, converted_registration_id FROM basket_personal_draw_team WHERE division_id=11")
    print("draw_teams:", cur.fetchall())
    conn.close()


if __name__ == "__main__":
    main()
