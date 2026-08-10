#!/usr/bin/env python3
"""Run CNPA batch4 migration + menus on local dev DB."""
import pymysql

HOST = "192.168.31.48"
USER = "root"
PASSWORD = "123456"
DATABASE = "basketball"

MIGRATION_FILE = r"d:\code\dz\basketball\scaffolding-v2\docs\sql\cnpa-batch4-migration.sql"

MENU_SQL = [
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (2221, '正式赛事球队', 2204, 2, 'official-teams', 'cnpa/participation/official-teams/index', 'BasketEventTeamAdmin', 1, 0, 'C', '0', '0', 'basket:eventTeam:list', 'peoples', 'admin', NOW(), 'CNPA正式赛事球队')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms), order_num=VALUES(order_num)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (2222, '正式赛事阵容', 2204, 3, 'event-roster', 'cnpa/participation/event-roster/index', 'BasketSeasonRosterAdmin', 1, 0, 'C', '0', '0', 'basket:seasonRoster:list', 'user', 'admin', NOW(), 'CNPA正式赛事阵容')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms), order_num=VALUES(order_num)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (2223, '转会审核', 2204, 4, 'transfer-audit', 'cnpa/participation/transfer-audit/index', 'BasketTransferAudit', 1, 0, 'C', '0', '0', 'basket:rosterChange:list', 'swap', 'admin', NOW(), 'CNPA转会审核')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms), order_num=VALUES(order_num)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (2224, '自由球员补员', 2204, 5, 'free-agent-audit', 'cnpa/participation/free-agent-audit/index', 'BasketFreeAgentAudit', 1, 0, 'C', '0', '0', 'basket:rosterChange:list', 'plus', 'admin', NOW(), 'CNPA自由球员补员')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms), order_num=VALUES(order_num)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (2225, '队长移交审核', 2204, 6, 'captain-transfer', 'cnpa/participation/captain-transfer/index', 'BasketCaptainTransferAudit', 1, 0, 'C', '0', '0', 'basket:rosterChange:list', 'guide', 'admin', NOW(), 'CNPA队长移交')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms), order_num=VALUES(order_num)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (2226, '阵容特殊操作', 2204, 7, 'roster-operations', 'cnpa/participation/roster-operations/index', 'BasketRosterOperation', 1, 0, 'C', '0', '0', 'basket:rosterOperation:list', 'edit', 'admin', NOW(), 'CNPA阵容特殊操作')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms), order_num=VALUES(order_num)""",
]
for mid in range(2221, 2227):
    MENU_SQL.append(f"INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, {mid})")


def run_file(cur, path):
    sql = open(path, encoding="utf-8").read()
    stmts = []
    buf = []
    for line in sql.splitlines():
        s = line.strip()
        if not s or s.startswith("--"):
            continue
        buf.append(line)
        if s.endswith(";"):
            stmts.append("\n".join(buf).rstrip(";").strip())
            buf = []
    return stmts


def main():
    conn = pymysql.connect(host=HOST, user=USER, password=PASSWORD, database=DATABASE, charset="utf8mb4")
    cur = conn.cursor()
    skip = ("Duplicate column", "Duplicate key", "already exists")
    for stmt in run_file(cur, MIGRATION_FILE):
        try:
            cur.execute(stmt)
            print("OK:", stmt[:70].replace("\n", " "))
        except Exception as e:
            if any(x in str(e) for x in skip):
                print("SKIP:", str(e)[:90])
            else:
                raise
    for stmt in MENU_SQL:
        cur.execute(stmt)
    conn.commit()
    cur.close()
    conn.close()
    print("BATCH4 MIGRATION DONE")


if __name__ == "__main__":
    main()
