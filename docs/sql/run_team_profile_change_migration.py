#!/usr/bin/env python3
"""Rebuild basket_team_profile_change table for team profile change audit feature."""

import pymysql

HOST = "192.168.31.49"
PORT = 3306
USER = "root"
PASSWORD = "123456"
DATABASE = "basketball"

CREATE_SQL = """
CREATE TABLE basket_team_profile_change (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    change_no varchar(64) NOT NULL COMMENT '变更编号',
    team_id bigint NOT NULL COMMENT '长期球队ID',
    submit_user_id bigint NOT NULL COMMENT '提交用户ID',
    submit_player_id bigint DEFAULT NULL COMMENT '提交球员ID',
    old_team_name varchar(128) DEFAULT NULL COMMENT '变更前球队名称',
    new_team_name varchar(128) DEFAULT NULL COMMENT '变更后球队名称',
    old_team_short_name varchar(64) DEFAULT NULL COMMENT '变更前球队简称',
    new_team_short_name varchar(64) DEFAULT NULL COMMENT '变更后球队简称',
    old_logo_file_id bigint DEFAULT NULL COMMENT '变更前Logo文件ID',
    new_logo_file_id bigint DEFAULT NULL COMMENT '变更后Logo文件ID',
    old_logo_url varchar(512) DEFAULT NULL COMMENT '变更前Logo URL',
    new_logo_url varchar(512) DEFAULT NULL COMMENT '变更后Logo URL',
    old_description varchar(1000) DEFAULT NULL COMMENT '变更前球队介绍',
    new_description varchar(1000) DEFAULT NULL COMMENT '变更后球队介绍',
    old_public_status tinyint DEFAULT NULL COMMENT '变更前公开状态',
    new_public_status tinyint DEFAULT NULL COMMENT '变更后公开状态',
    change_reason varchar(500) DEFAULT NULL COMMENT '变更说明',
    audit_status tinyint NOT NULL DEFAULT 1 COMMENT '审核状态',
    audit_user_id bigint DEFAULT NULL COMMENT '审核人',
    audit_time datetime DEFAULT NULL COMMENT '审核时间',
    audit_remark varchar(500) DEFAULT NULL COMMENT '审核备注',
    cancel_time datetime DEFAULT NULL COMMENT '取消时间',
    cancel_reason varchar(500) DEFAULT NULL COMMENT '取消原因',
    active_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前待处理申请',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_change_no (change_no),
    KEY idx_team_active (team_id, active_flag, is_delete),
    KEY idx_team_audit_status (team_id, audit_status, create_time),
    KEY idx_submit_user (submit_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队资料变更申请表'
"""


def main() -> None:
    conn = pymysql.connect(
        host=HOST,
        port=PORT,
        user=USER,
        password=PASSWORD,
        database=DATABASE,
        charset="utf8mb4",
    )
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=%s AND table_name='basket_team_profile_change'", (DATABASE,))
            exists = cur.fetchone()[0] > 0
            if exists:
                cur.execute("SELECT COUNT(*) FROM basket_team_profile_change")
                row_count = cur.fetchone()[0]
                print(f"Existing table rows: {row_count}")
            cur.execute("DROP TABLE IF EXISTS basket_team_profile_change")
            cur.execute(CREATE_SQL)
            cur.execute("SHOW COLUMNS FROM basket_team_profile_change LIKE 'active_flag'")
            col = cur.fetchone()
            if not col:
                raise RuntimeError("active_flag column missing after migration")
            print("basket_team_profile_change rebuilt successfully")
        conn.commit()
    finally:
        conn.close()


if __name__ == "__main__":
    main()
