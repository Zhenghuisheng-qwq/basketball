#!/usr/bin/env python3
import pymysql

conn = pymysql.connect(
    host="192.168.31.49",
    port=3306,
    user="root",
    password="123456",
    database="basketball",
    charset="utf8mb4",
)
cur = conn.cursor()
cur.execute(
    """
    INSERT INTO sys_menu (
        menu_id, menu_name, parent_id, order_num, path, component,
        query, route_name, is_frame, is_cache, menu_type, visible, status,
        perms, icon, create_by, create_time, remark
    ) VALUES (
        2215, %s, 2151, 6, 'team-profile-change',
        'cnpa/identity/team-profile-change/index', '', 'BasketTeamProfileChange',
        1, 0, 'C', '0', '0', 'basket:teamProfileChange:list', 'edit', 'admin', NOW(), %s
    ) ON DUPLICATE KEY UPDATE
        menu_name = VALUES(menu_name),
        path = VALUES(path),
        component = VALUES(component),
        perms = VALUES(perms)
    """,
    ("球队资料变更审核", "CNPA球队资料变更审核"),
)
cur.execute("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2215)")
conn.commit()
cur.execute("SELECT menu_id, menu_name FROM sys_menu WHERE menu_id = 2215")
print(cur.fetchone())
cur.close()
conn.close()
