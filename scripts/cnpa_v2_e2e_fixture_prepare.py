#!/usr/bin/env python3
"""TEST FIXTURE ONLY — local E2E identity prep for CNPA V2 20260806."""
from __future__ import annotations

import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
GROUP_PREFIX = "CNPA_V2_E2E_FIXTURE_GROUP_20260806"
PERSONAL_PREFIX = "CNPA_V2_E2E_FIXTURE_PERSONAL_20260806"
GROUP_EMAILS = [f"111{160 + i}@qq.com" for i in range(16)]
RR4_EMAILS = [f"111{176 + i}@qq.com" for i in range(4)]
TIEBREAK_EMAILS = [f"111{180 + i}@qq.com" for i in range(3)]
PERSONAL_EMAILS = [f"111{210 + i}@qq.com" for i in range(6)]
RESERVED = set(GROUP_EMAILS + RR4_EMAILS + TIEBREAK_EMAILS + PERSONAL_EMAILS)


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


def env(k: str, default: str = "") -> str:
    return os.environ.get(k, default)


def db_query(sql: str) -> list:
    import pymysql
    conn = pymysql.connect(
        host=env("CNPA_DB_HOST", "192.168.31.47"),
        port=int(env("CNPA_DB_PORT", "3306")),
        user=env("CNPA_DB_USER", "root"),
        password=env("CNPA_DB_PASSWORD", "123456"),
        database=env("CNPA_DB_NAME", "basketball"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            if cur.description:
                return list(cur.fetchall())
            conn.commit()
            return []
    finally:
        conn.close()


def member_pool() -> list[str]:
    rows = db_query(
        "SELECT ma.email FROM basket_member_account ma "
        "JOIN basket_player_profile pp ON pp.member_id=ma.id AND pp.is_delete=0 "
        "WHERE ma.email LIKE '1111%@qq.com' AND ma.is_delete=0 AND pp.certification_status=3 "
        "ORDER BY ma.email"
    )
    return [r["email"] for r in rows if r["email"] not in RESERVED]


FIXTURE_PLATFORM = "PS"  # PS_XBOX division accepts PS/XBOX accounts, not literal PS_XBOX


def platform_accounts_for(member_platform: str) -> list[str]:
    if member_platform in ("PS_XBOX", "PS"):
        return ["PS"]
    return [member_platform]


def ensure_platform(player_id: int, platform: str, label: str) -> None:
    for pf in platform_accounts_for(platform):
        existing = db_query(
            f"SELECT id FROM basket_player_platform_account WHERE player_id={player_id} "
            f"AND platform='{pf}' AND is_delete=0 LIMIT 1"
        )
        if existing:
            continue
        db_query(
            f"INSERT INTO basket_player_platform_account (player_id, platform, display_game_id, normalized_game_id, "
            f"account_status, is_primary, create_time, update_time, is_delete) "
            f"VALUES ({player_id}, '{pf}', '{label}', '{label}', 1, 1, NOW(), NOW(), 0)"
        )


def ensure_member_player_platform(email: str, platforms: list[str]) -> dict:
    rows = db_query(
        f"SELECT ma.id AS member_id, pp.id AS player_id, pp.certification_status "
        f"FROM basket_member_account ma LEFT JOIN basket_player_profile pp ON pp.member_id=ma.id AND pp.is_delete=0 "
        f"WHERE ma.email='{email}' AND ma.is_delete=0 LIMIT 1"
    )
    if not rows:
        raise RuntimeError(f"missing member account: {email}")
    member_id = rows[0]["member_id"]
    player_id = rows[0]["player_id"]
    if not player_id:
        db_query(
            f"INSERT INTO basket_player_profile (member_id, game_nickname, certification_status, create_time, update_time, is_delete) "
            f"VALUES ({member_id}, '{email.split('@')[0]}', 3, NOW(), NOW(), 0)"
        )
        player_id = db_query(f"SELECT id FROM basket_player_profile WHERE member_id={member_id} AND is_delete=0 LIMIT 1")[0]["id"]
    elif rows[0]["certification_status"] != 3:
        db_query(f"UPDATE basket_player_profile SET certification_status=3, update_time=NOW() WHERE id={player_id}")
    label = email.split("@")[0]
    for platform in platforms:
        ensure_platform(player_id, platform, label)
    return {"member_id": member_id, "player_id": player_id, "email": email}


def upsert_team_member(team_id: int, person: dict, role: str, member_platform: str) -> None:
    """Idempotent member attach — uk_team_membership_no spans soft-deleted rows."""
    membership_no = f"TF{team_id}{person['player_id']}"
    deleted = db_query(
        f"SELECT id FROM basket_team_member WHERE membership_no='{membership_no}' AND is_delete=1 LIMIT 1"
    )
    if deleted:
        db_query(
            f"UPDATE basket_team_member SET is_delete=0, is_current=1, membership_status=1, "
            f"member_id={person['member_id']}, player_id={person['player_id']}, platform_code='{member_platform}', "
            f"role_type='{role}', roster_type='OFFICIAL', join_source='INVITE', leave_time=NULL, leave_reason=NULL, "
            f"update_time=NOW() WHERE id={deleted[0]['id']}"
        )
        return
    active = db_query(
        f"SELECT id FROM basket_team_member WHERE team_id={team_id} AND player_id={person['player_id']} "
        f"AND is_delete=0 AND is_current=1 LIMIT 1"
    )
    if active:
        return
    db_query(
        f"INSERT INTO basket_team_member (membership_no, team_id, member_id, player_id, platform_code, role_type, "
        f"roster_type, join_source, membership_status, is_current, join_time, create_time, update_time, is_delete) "
        f"VALUES ('{membership_no}', {team_id}, {person['member_id']}, {person['player_id']}, '{member_platform}', "
        f"'{role}', 'OFFICIAL', 'INVITE', 1, 1, NOW(), NOW(), NOW(), 0)"
    )


def ensure_fixture_team_sql(captain: dict, marker: str, extra_emails: list[str], member_platform: str = "PS_XBOX") -> int:
    email = captain["email"]
    existing = db_query(
        f"SELECT t.id FROM basket_team t WHERE t.captain_member_id={captain['member_id']} "
        f"AND t.team_name LIKE '%{marker}%' AND t.is_delete=0 LIMIT 1"
    )
    if existing:
        team_id = existing[0]["id"]
        db_query(
            f"UPDATE basket_team_member SET is_delete=1, is_current=0, update_time=NOW() "
            f"WHERE team_id={team_id} AND role_type!='CAPTAIN' AND is_delete=0"
        )
    else:
        team_no = f"TF{email.split('@')[0]}"
        team_name = f"TEST FIXTURE {marker}-{email.split('@')[0]}"
        db_query(
            f"INSERT INTO basket_team (team_no, team_name, short_name, game_terminal, captain_member_id, captain_player_id, "
            f"max_member_count, current_member_count, team_status, audit_status, public_status, create_time, update_time, is_delete) "
            f"VALUES ('{team_no}', '{team_name}', 'TF', 'PS', {captain['member_id']}, {captain['player_id']}, "
            f"8, {1 + len(extra_emails)}, 1, 1, 0, NOW(), NOW(), 0)"
        )
        team_id = db_query(f"SELECT id FROM basket_team WHERE team_no='{team_no}' AND is_delete=0 LIMIT 1")[0]["id"]

    upsert_team_member(team_id, captain, "CAPTAIN", member_platform)

    roster = [captain["email"]] + extra_emails
    member_cnt = 0
    for role_email in roster:
        person = ensure_member_player_platform(role_email, [member_platform, "PS_XBOX"] if member_platform == "PC" else [member_platform])
        role = "CAPTAIN" if role_email == email else "MEMBER"
        upsert_team_member(team_id, person, role, member_platform)
        member_cnt += 1
    db_query(f"UPDATE basket_team SET current_member_count={member_cnt}, update_time=NOW() WHERE id={team_id}")
    return int(team_id)


def ensure_personal_player(email: str) -> int:
    captain = ensure_member_player_platform(email, [FIXTURE_PLATFORM])
    team_cnt = db_query(
        f"SELECT COUNT(*) c FROM basket_team_member tm WHERE tm.player_id={captain['player_id']} "
        f"AND tm.is_delete=0 AND tm.is_current=1 AND tm.membership_status=1"
    )[0]["c"]
    if team_cnt > 0:
        # TEST FIXTURE ONLY — prior PERSONAL E2E may have created long-term teams.
        db_query(
            f"UPDATE basket_team_member SET is_delete=1, is_current=0, update_time=NOW() "
            f"WHERE player_id={captain['player_id']} AND is_delete=0"
        )
    return int(captain["player_id"])


def main() -> int:
    load_dotenv(ENV_LOCAL)
    pool = member_pool()
    used_members: set[str] = set()
    out = {"groupPrefix": GROUP_PREFIX, "personalPrefix": PERSONAL_PREFIX, "teams": {}, "players": {}}

    def take(n: int) -> list[str]:
        picked: list[str] = []
        for email in pool:
            if email in used_members:
                continue
            picked.append(email)
            used_members.add(email)
            if len(picked) >= n:
                break
        if len(picked) < n:
            raise RuntimeError(f"member pool exhausted need={n} got={len(picked)} pool={len(pool)} used={len(used_members)}")
        return picked

    for email in GROUP_EMAILS:
        captain = ensure_member_player_platform(email, [FIXTURE_PLATFORM])
        extras = take(2)
        team_id = ensure_fixture_team_sql(captain, GROUP_PREFIX, extras, FIXTURE_PLATFORM)
        out["teams"][email] = team_id
        print(f"[OK] GROUP fixture team email={email} teamId={team_id} members={extras}")

    for email in RR4_EMAILS:
        captain = ensure_member_player_platform(email, ["PC"])
        extras = take(4)
        team_id = ensure_fixture_team_sql(captain, GROUP_PREFIX, extras, "PC")
        out["teams"][email] = team_id
        print(f"[OK] RR4 fixture team email={email} teamId={team_id}")

    for email in TIEBREAK_EMAILS:
        captain = ensure_member_player_platform(email, ["PC"])
        extras = take(4)
        team_id = ensure_fixture_team_sql(captain, GROUP_PREFIX, extras, "PC")
        out["teams"][email] = team_id
        print(f"[OK] TIEBREAK fixture team email={email} teamId={team_id}")

    for email in PERSONAL_EMAILS:
        ensure_member_player_platform(email, [FIXTURE_PLATFORM])
        pid = ensure_personal_player(email)
        out["players"][email] = pid
        print(f"[OK] PERSONAL fixture player email={email} playerId={pid}")

    result_path = ROOT / "scripts" / "output" / "cnpa-v2-e2e-fixture-20260806.json"
    result_path.parent.mkdir(parents=True, exist_ok=True)
    result_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"JSON: {result_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
