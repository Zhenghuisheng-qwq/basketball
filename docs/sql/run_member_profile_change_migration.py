#!/usr/bin/env python3
"""Create basket_member_profile_change + Admin menu for nickname/avatar audit."""

import pymysql

HOST = "192.168.31.48"
PORT = 3306
USER = "root"
PASSWORD = "123456"
DATABASE = "basketball"

CREATE_SQL = """
CREATE TABLE IF NOT EXISTS basket_member_profile_change (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    change_no varchar(64) NOT NULL COMMENT '变更编号',
    member_id bigint NOT NULL COMMENT '会员账号ID',
    player_id bigint DEFAULT NULL COMMENT '球员ID（如有）',
    old_nickname varchar(64) DEFAULT NULL COMMENT '变更前昵称',
    new_nickname varchar(64) DEFAULT NULL COMMENT '变更后昵称',
    old_avatar_file_id bigint DEFAULT NULL COMMENT '变更前头像文件ID',
    new_avatar_file_id bigint DEFAULT NULL COMMENT '变更后头像文件ID',
    old_avatar_url varchar(512) DEFAULT NULL COMMENT '变更前头像URL',
    new_avatar_url varchar(512) DEFAULT NULL COMMENT '变更后头像URL',
    change_summary varchar(200) DEFAULT NULL COMMENT '变更摘要',
    audit_status tinyint NOT NULL DEFAULT 1 COMMENT '1待审核 3已通过 4已驳回 5已取消',
    audit_user_id bigint DEFAULT NULL COMMENT '审核人',
    audit_time datetime DEFAULT NULL COMMENT '审核时间',
    audit_remark varchar(500) DEFAULT NULL COMMENT '审核备注',
    active_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前待处理申请',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_change_no (change_no),
    KEY idx_member_active (member_id, active_flag, is_delete),
    KEY idx_member_audit_status (member_id, audit_status, create_time),
    KEY idx_audit_status_time (audit_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员昵称头像变更申请表'
"""

PENDING_SLOT_SQL = """
ALTER TABLE basket_member_profile_change
  ADD COLUMN pending_slot bigint
    GENERATED ALWAYS AS (
      CASE WHEN active_flag = 1 AND audit_status = 1 AND is_delete = 0 THEN member_id ELSE NULL END
    ) STORED,
  ADD UNIQUE KEY uk_member_profile_change_pending_slot (pending_slot)
"""

MENU_SQL = """
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    query, route_name, is_frame, is_cache, menu_type, visible, status,
    perms, icon, create_by, create_time, remark
) VALUES (
    2216, %s, 2151, 7, 'member-profile-change',
    'cnpa/identity/member-profile-change/index', '', 'BasketMemberProfileChange',
    1, 0, 'C', '0', '0', 'basket:memberProfileChange:list', 'user', 'admin', NOW(), %s
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    path = VALUES(path),
    component = VALUES(component),
    perms = VALUES(perms),
    visible = '0',
    status = '0'
"""

BUTTONS = [
    (22161, "变更查询", "basket:memberProfileChange:query", 1),
    (22162, "变更审核", "basket:memberProfileChange:audit", 2),
]


def main() -> None:
    conn = pymysql.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD, database=DATABASE, charset="utf8mb4"
    )
    try:
        with conn.cursor() as cur:
            cur.execute(CREATE_SQL)
            cur.execute(MENU_SQL, ("用户资料变更审核", "CNPA用户昵称/头像变更审核"))
            for menu_id, name, perms, order_num in BUTTONS:
                cur.execute(
                    """
                    INSERT INTO sys_menu (
                        menu_id, menu_name, parent_id, order_num, path, component,
                        query, route_name, is_frame, is_cache, menu_type, visible, status,
                        perms, icon, create_by, create_time, remark
                    ) VALUES (
                        %s, %s, 2216, %s, '', '',
                        '', '', 1, 0, 'F', '0', '0', %s, '#', 'admin', NOW(), %s
                    ) ON DUPLICATE KEY UPDATE
                        menu_name = VALUES(menu_name),
                        perms = VALUES(perms)
                    """,
                    (menu_id, name, order_num, perms, name),
                )
            for role_id in (1, 2):
                cur.execute("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (%s, 2216)", (role_id,))
                for menu_id, *_ in BUTTONS:
                    cur.execute(
                        "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (%s, %s)",
                        (role_id, menu_id),
                    )
            cur.execute(
                "SELECT COUNT(*) FROM information_schema.columns "
                "WHERE table_schema=%s AND table_name='basket_member_profile_change' AND column_name='pending_slot'",
                (DATABASE,),
            )
            if cur.fetchone()[0] == 0:
                cur.execute(PENDING_SLOT_SQL)
            cur.execute(
                "SELECT COUNT(*) FROM information_schema.tables "
                "WHERE table_schema=%s AND table_name='basket_member_profile_change'",
                (DATABASE,),
            )
            print("table_ok", cur.fetchone()[0])
            cur.execute("SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id IN (2216,22161,22162)")
            print("menus", cur.fetchall())
        conn.commit()
    finally:
        conn.close()


if __name__ == "__main__":
    main()
