#!/usr/bin/env python3
"""
LOCAL DEVELOPMENT ONLY — CNPA V2 业务数据基线重置（Full Business Reset）。

默认：DRY RUN（只统计，不删除）
执行：python cleanup_local_test_data.py --execute --confirm LOCAL-BASKETBALL-CLEANUP

安全：
- 仅允许 host=192.168.31.48 database=basketball
- NO DDL / NO DROP / NO ALTER / 不对混合配置表 TRUNCATE
- 保留：sys_*、Series、Format Template*、File Asset
- 清空：全部 CNPA 动态业务数据（含 Member / Player / Team / Season / Match 等）
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from typing import Any

import pymysql

ALLOWED_HOST = "192.168.31.48"
ALLOWED_DB = "basketball"
CONFIRM_TOKEN = "LOCAL-BASKETBALL-CLEANUP"

DB = dict(
    host=ALLOWED_HOST,
    port=3306,
    user="root",
    password="123456",
    database=ALLOWED_DB,
    charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
    autocommit=False,
)

# 系统静态 / 启动必需：禁止清理
KEEP_BASKET_TABLES = {
    "basket_series",
    "basket_format_template",
    "basket_format_template_stage",
    "basket_format_template_tie_break_rule",
    "basket_file_asset",
    "basket_site_config",  # 若有配置行则保留；空表也无妨
}

# 动态业务表删除顺序（子 → 父）。未列出的 basket_* 运行表会追加在末尾清空。
DELETE_ORDER = [
    # match / stats / submissions
    "basket_standing_rank_reason",
    "basket_match_submission_player_stat",
    "basket_match_submission_team_stat",
    "basket_match_submission_material",
    "basket_match_player_stat",
    "basket_match_team_stat",
    "basket_match_submission",
    "basket_stat_sync_record",
    "basket_stat_change_record",
    "basket_discipline_suspension_log",
    "basket_discipline_restriction",
    "basket_discipline_record",
    "basket_match",
    "basket_match_series",
    "basket_schedule_batch",
    "basket_standing",
    "basket_player_stat",
    "basket_team_stat",
    "basket_rank",
    "basket_player_rank",
    "basket_team_rank",
    "basket_point_adjustment",
    # stage / group / format runtime
    "basket_division_stage_qualification",
    "basket_division_stage_team",
    "basket_division_group_team",
    "basket_division_group",
    "basket_division_grouping_batch",
    "basket_division_tie_break_rule",
    "basket_division_stage",
    "basket_division_format_snapshot",
    # event / registration
    "basket_event_player",
    "basket_event_team",
    "basket_registration_player",
    "basket_registration_audit",
    "basket_registration",
    "basket_personal_registration_audit",
    "basket_personal_draw_assignment",
    "basket_personal_draw_team",
    "basket_personal_draw_batch",
    "basket_personal_registration",
    # team / grant
    "basket_captain_grant_log",
    "basket_captain_grant",
    "basket_team_invitation",
    "basket_team_join_application",
    "basket_team_member",
    "basket_team_profile_change",
    "basket_team_audit_record",
    "basket_team_captain_history",
    "basket_player_team_relation",
    "basket_team",
    # division / season
    "basket_division",
    "basket_season",
    # player / identity / cert
    "basket_player_platform_change_request",
    "basket_player_platform_identity_history",
    "basket_player_platform_account",
    "basket_player_cert_material",
    "basket_player_cert_audit",
    "basket_player_cert_application",
    "basket_player_profile",
    # member / account side
    "basket_member_profile_change",
    "basket_member_profile",
    "basket_member_agreement_record",
    "basket_notification_setting",
    "basket_privacy_setting",
    "basket_favorite",
    "basket_message_receiver",
    "basket_message_task",
    "basket_login_log",
    "basket_account_security_log",
    "basket_verify_code_record",
    "basket_member_account",
    # content / misc business
    "basket_announcement_attachment",
    "basket_announcement_rule_link",
    "basket_announcement",
    "basket_business_audit_field",
    "basket_business_audit",
    "basket_honor_lineup",
    "basket_honor_record",
    "basket_honor_generation_batch",
    "basket_honor_category",
    "basket_home_banner",
    "basket_partner_support_record",
    "basket_partner",
    "basket_venue",
]

ENTITY_SQL = {
    "Member": "SELECT COUNT(*) AS c FROM basket_member_account",
    "PermanentPlayer": "SELECT COUNT(*) AS c FROM basket_player_profile",
    "Profile": "SELECT COUNT(*) AS c FROM basket_member_profile",
    "PlatformIdentity": "SELECT COUNT(*) AS c FROM basket_player_platform_account",
    "Season": "SELECT COUNT(*) AS c FROM basket_season",
    "Division": "SELECT COUNT(*) AS c FROM basket_division",
    "Team": "SELECT COUNT(*) AS c FROM basket_team",
    "TeamMember": "SELECT COUNT(*) AS c FROM basket_team_member",
    "CaptainGrant": "SELECT COUNT(*) AS c FROM basket_captain_grant",
    "Registration": "SELECT COUNT(*) AS c FROM basket_registration",
    "EventTeam": "SELECT COUNT(*) AS c FROM basket_event_team",
    "EventPlayer": "SELECT COUNT(*) AS c FROM basket_event_player",
    "Stage": "SELECT COUNT(*) AS c FROM basket_division_stage",
    "Group": "SELECT COUNT(*) AS c FROM basket_division_group",
    "MatchSeries": "SELECT COUNT(*) AS c FROM basket_match_series",
    "Match": "SELECT COUNT(*) AS c FROM basket_match",
    "Standing": "SELECT COUNT(*) AS c FROM basket_standing",
    "TeamStat": "SELECT COUNT(*) AS c FROM basket_team_stat",
    "PlayerStat": "SELECT COUNT(*) AS c FROM basket_player_stat",
    "Rank": "SELECT COUNT(*) AS c FROM basket_rank",
}


def db_conn():
    return pymysql.connect(**DB)


def assert_safe_target(conn) -> None:
    if DB["host"] != ALLOWED_HOST or DB["database"] != ALLOWED_DB:
        raise SystemExit(f"REFUSE: config host/db not allowed: {DB['host']}/{DB['database']}")
    with conn.cursor() as cur:
        cur.execute("SELECT DATABASE() AS db")
        row = cur.fetchone() or {}
    if (row.get("db") or "") != ALLOWED_DB:
        raise SystemExit(f"REFUSE: DATABASE()={row.get('db')}")


def q(cur, sql: str, args: tuple | list | None = None) -> list[dict[str, Any]]:
    cur.execute(sql, args or ())
    return list(cur.fetchall() or [])


def q1(cur, sql: str, args: tuple | list | None = None) -> int:
    rows = q(cur, sql, args)
    if not rows:
        return 0
    return int(next(iter(rows[0].values())) or 0)


def list_basket_tables(cur) -> list[str]:
    # 避免 pymysql 把 LIKE 中的 %/_ 当格式化占位
    cur.execute("SHOW FULL TABLES WHERE Table_type='BASE TABLE'")
    rows = cur.fetchall() or []
    names = []
    for r in rows:
        vals = list(r.values())
        name = vals[0] if vals else None
        if isinstance(name, str) and name.startswith("basket_"):
            names.append(name)
    return sorted(names)


def build_clear_tables(cur) -> list[str]:
    existing = set(list_basket_tables(cur))
    ordered: list[str] = []
    for t in DELETE_ORDER:
        if t in existing and t not in KEEP_BASKET_TABLES:
            ordered.append(t)
    # 任何未列入顺序的动态 basket 表也清空
    for t in sorted(existing):
        if t in KEEP_BASKET_TABLES or t in ordered:
            continue
        ordered.append(t)
    return ordered


def entity_counts(cur) -> dict[str, int]:
    out = {}
    for name, sql in ENTITY_SQL.items():
        try:
            out[name] = q1(cur, sql)
        except Exception:
            out[name] = -1
    return out


def keep_counts(cur) -> dict[str, int]:
    return {
        "Series": q1(cur, "SELECT COUNT(*) FROM basket_series"),
        "Format Templates": q1(cur, "SELECT COUNT(*) FROM basket_format_template"),
        "Stage Templates": q1(cur, "SELECT COUNT(*) FROM basket_format_template_stage"),
        "TieBreak Templates": q1(cur, "SELECT COUNT(*) FROM basket_format_template_tie_break_rule"),
        "File Asset": q1(cur, "SELECT COUNT(*) FROM basket_file_asset"),
        "Admin User": q1(cur, "SELECT COUNT(*) FROM sys_user WHERE del_flag='0'"),
        "Menus": q1(cur, "SELECT COUNT(*) FROM sys_menu"),
        "Roles": q1(cur, "SELECT COUNT(*) FROM sys_role"),
        "Dict Types": q1(cur, "SELECT COUNT(*) FROM sys_dict_type"),
        "Dict Data": q1(cur, "SELECT COUNT(*) FROM sys_dict_data"),
        "Sys Config": q1(cur, "SELECT COUNT(*) FROM sys_config"),
    }


def estimate_delete_counts(cur, clear_tables: list[str]) -> dict[str, int]:
    out = {}
    for t in clear_tables:
        out[t] = q1(cur, f"SELECT COUNT(*) FROM `{t}`")
    return {k: v for k, v in out.items() if v > 0}


def execute_full_reset(cur, clear_tables: list[str]) -> dict[str, int]:
    deleted: dict[str, int] = {}
    cur.execute("SET FOREIGN_KEY_CHECKS=0")
    try:
        for t in clear_tables:
            cur.execute(f"DELETE FROM `{t}`")
            deleted[t] = int(cur.rowcount)
    finally:
        cur.execute("SET FOREIGN_KEY_CHECKS=1")
    return {k: v for k, v in deleted.items() if v}


def orphan_checks(cur) -> dict[str, int]:
    checks = {
        "event_player_wo_event_team": q1(
            cur,
            "SELECT COUNT(*) FROM basket_event_player ep "
            "LEFT JOIN basket_event_team et ON et.id=ep.event_team_id WHERE et.id IS NULL",
        ),
        "event_team_wo_division": q1(
            cur,
            "SELECT COUNT(*) FROM basket_event_team et "
            "LEFT JOIN basket_division d ON d.id=et.division_id WHERE d.id IS NULL",
        ),
        "match_wo_division": q1(
            cur,
            "SELECT COUNT(*) FROM basket_match m "
            "LEFT JOIN basket_division d ON d.id=m.division_id WHERE d.id IS NULL",
        ),
        "match_home_wo_event_team": q1(
            cur,
            "SELECT COUNT(*) FROM basket_match m "
            "LEFT JOIN basket_event_team et ON et.id=m.home_event_team_id "
            "WHERE m.home_event_team_id IS NOT NULL AND et.id IS NULL",
        ),
        "match_away_wo_event_team": q1(
            cur,
            "SELECT COUNT(*) FROM basket_match m "
            "LEFT JOIN basket_event_team et ON et.id=m.away_event_team_id "
            "WHERE m.away_event_team_id IS NOT NULL AND et.id IS NULL",
        ),
        "match_series_wo_stage": q1(
            cur,
            "SELECT COUNT(*) FROM basket_match_series ms "
            "LEFT JOIN basket_division_stage st ON st.id=ms.stage_id "
            "WHERE ms.stage_id IS NOT NULL AND st.id IS NULL",
        ),
        "standing_wo_event_team": q1(
            cur,
            "SELECT COUNT(*) FROM basket_standing s "
            "LEFT JOIN basket_event_team et ON et.id=s.event_team_id "
            "WHERE s.event_team_id IS NOT NULL AND et.id IS NULL",
        ),
        "player_stat_wo_event_player": q1(
            cur,
            "SELECT COUNT(*) FROM basket_player_stat ps "
            "LEFT JOIN basket_event_player ep ON ep.id=ps.event_player_id "
            "WHERE ps.event_player_id IS NOT NULL AND ep.id IS NULL",
        ),
        "team_member_wo_team": q1(
            cur,
            "SELECT COUNT(*) FROM basket_team_member tm "
            "LEFT JOIN basket_team t ON t.id=tm.team_id WHERE t.id IS NULL",
        ),
        "registration_wo_team": q1(
            cur,
            "SELECT COUNT(*) FROM basket_registration r "
            "LEFT JOIN basket_team t ON t.id=r.team_id "
            "WHERE r.team_id IS NOT NULL AND t.id IS NULL",
        ),
        "registration_wo_division": q1(
            cur,
            "SELECT COUNT(*) FROM basket_registration r "
            "LEFT JOIN basket_division d ON d.id=r.division_id WHERE d.id IS NULL",
        ),
        "grant_wo_division": q1(
            cur,
            "SELECT COUNT(*) FROM basket_captain_grant g "
            "LEFT JOIN basket_division d ON d.id=g.division_id WHERE d.id IS NULL",
        ),
        "stage_wo_division": q1(
            cur,
            "SELECT COUNT(*) FROM basket_division_stage st "
            "LEFT JOIN basket_division d ON d.id=st.division_id WHERE d.id IS NULL",
        ),
        "current_team_id_dangling": q1(
            cur,
            "SELECT COUNT(*) FROM basket_player_profile p "
            "LEFT JOIN basket_team t ON t.id=p.current_team_id "
            "WHERE p.current_team_id IS NOT NULL AND t.id IS NULL",
        ),
    }
    return checks


def smoke_id_counts(cur) -> dict[str, int]:
    return {
        "division_118": q1(cur, "SELECT COUNT(*) FROM basket_division WHERE id=118"),
        "division_141": q1(cur, "SELECT COUNT(*) FROM basket_division WHERE id=141"),
        "season_81": q1(cur, "SELECT COUNT(*) FROM basket_season WHERE id=81"),
        "season_106": q1(cur, "SELECT COUNT(*) FROM basket_season WHERE id=106"),
        "season_1": q1(cur, "SELECT COUNT(*) FROM basket_season WHERE id=1"),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="LOCAL ONLY: CNPA Full Business Reset")
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--confirm", default="")
    parser.add_argument("--json-out", default="")
    args = parser.parse_args()

    if args.execute and args.confirm != CONFIRM_TOKEN:
        print(f"REFUSE execute: missing --confirm {CONFIRM_TOKEN}", file=sys.stderr)
        return 2

    mode = "execute" if args.execute else "dry-run"
    conn = db_conn()
    try:
        assert_safe_target(conn)
        print(f"HOST={ALLOWED_HOST}")
        print(f"DB={ALLOWED_DB}")
        print("MODE=Full Business Reset")
        with conn.cursor() as cur:
            clear_tables = build_clear_tables(cur)
            before = entity_counts(cur)
            keep_before = keep_counts(cur)
            delete_est = estimate_delete_counts(cur, clear_tables)
            smoke_before = smoke_id_counts(cur)

            plan = {
                "generated_at": datetime.now().isoformat(timespec="seconds"),
                "mode": mode,
                "strategy": "Full Business Reset",
                "keep_tables": sorted(KEEP_BASKET_TABLES),
                "clear_tables": clear_tables,
                "before": before,
                "keep_before": keep_before,
                "delete_estimate": delete_est,
                "smoke_before": smoke_before,
                "web_test_account_policy": "DELETE ALL (dev-mail-bypass=true, can re-register)",
            }
            if args.json_out:
                with open(args.json_out, "w", encoding="utf-8") as f:
                    json.dump(plan, f, ensure_ascii=False, indent=2)

            if mode != "execute":
                conn.rollback()
                print(json.dumps(plan, ensure_ascii=False, indent=2))
                print("是否已真正删除数据：NO")
                return 0

            deleted = execute_full_reset(cur, clear_tables)
            after = entity_counts(cur)
            keep_after = keep_counts(cur)
            orphans = orphan_checks(cur)
            smoke_after = smoke_id_counts(cur)

            # 保护闸：系统静态不得减少
            for key in ("Series", "Format Templates", "Stage Templates", "File Asset", "Admin User", "Menus", "Roles"):
                if keep_after.get(key, 0) < keep_before.get(key, 0):
                    conn.rollback()
                    raise SystemExit(f"ROLLBACK: protected {key} decreased {keep_before[key]} -> {keep_after[key]}")
            if any(v != 0 for v in orphans.values()):
                conn.rollback()
                raise SystemExit(f"ROLLBACK: orphan check failed: {orphans}")
            # 业务目标闸
            for key in (
                "Season",
                "Division",
                "Team",
                "Registration",
                "EventTeam",
                "EventPlayer",
                "Match",
                "MatchSeries",
                "Standing",
                "Member",
                "PermanentPlayer",
            ):
                if after.get(key, 0) != 0:
                    conn.rollback()
                    raise SystemExit(f"ROLLBACK: expected {key}=0 after reset, got {after.get(key)}")

            conn.commit()
            result = {
                "strategy": "Full Business Reset",
                "before": before,
                "after": after,
                "deleted_rows": deleted,
                "keep_before": keep_before,
                "keep_after": keep_after,
                "orphans": orphans,
                "smoke_after": smoke_after,
                "web_test_accounts_kept": 0,
            }
            print(json.dumps(result, ensure_ascii=False, indent=2))
            print("是否已真正删除数据：YES")
            return 0
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
