-- CNPA 球队资料变更审核：重建 basket_team_profile_change 表结构
-- 执行前请备份。旧表若含 JSON 式 before_value/after_value 将被替换为分列 old/new 字段。

DROP TABLE IF EXISTS basket_team_profile_change;

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
    audit_status tinyint NOT NULL DEFAULT 1 COMMENT '审核状态：1待审核 2审核中 3已通过 4已驳回 5已取消',
    audit_user_id bigint DEFAULT NULL COMMENT '审核人',
    audit_time datetime DEFAULT NULL COMMENT '审核时间',
    audit_remark varchar(500) DEFAULT NULL COMMENT '审核备注',
    cancel_time datetime DEFAULT NULL COMMENT '取消时间',
    cancel_reason varchar(500) DEFAULT NULL COMMENT '取消原因',
    active_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前待处理申请：1是 0否',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_change_no (change_no),
    KEY idx_team_active (team_id, active_flag, is_delete),
    KEY idx_team_audit_status (team_id, audit_status, create_time),
    KEY idx_submit_user (submit_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队资料变更申请表';

-- 后台菜单（parent_id=2151 账号与认证，请按环境确认 parent_id）
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    query, route_name, is_frame, is_cache, menu_type, visible, status,
    perms, icon, create_by, create_time, remark
) VALUES (
    2215, '球队资料变更审核', 2151, 6, 'team-profile-change',
    'cnpa/identity/team-profile-change/index', '', 'BasketTeamProfileChange',
    1, 0, 'C', '0', '0', 'basket:teamProfileChange:list', 'edit', 'admin', NOW(),
    'CNPA球队资料变更审核'
) ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    path = VALUES(path),
    component = VALUES(component),
    perms = VALUES(perms),
    remark = VALUES(remark);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2215);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2216, '球队资料变更查询', 2215, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:teamProfileChange:query', '#', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2217, '球队资料变更审核操作', 2215, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:teamProfileChange:audit', '#', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE perms = VALUES(perms);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2216);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2217);
