-- CNPA Batch6 migration
-- Execute via docs/sql/run_batch6_migration.py

-- Extend discipline record
ALTER TABLE basket_discipline_record
    ADD COLUMN target_type varchar(32) NOT NULL DEFAULT 'PLAYER' COMMENT '处罚对象类型' AFTER record_no,
    ADD COLUMN event_team_id bigint DEFAULT NULL COMMENT '赛事球队主键' AFTER team_id,
    ADD COLUMN penalty_code varchar(64) DEFAULT NULL COMMENT '处罚措施编码' AFTER discipline_type,
    ADD COLUMN match_suspension_count int NOT NULL DEFAULT 0 COMMENT '禁赛场次' AFTER penalty_content,
    ADD COLUMN match_suspension_remaining int NOT NULL DEFAULT 0 COMMENT '剩余禁赛场次' AFTER match_suspension_count,
    ADD COLUMN point_deduction int NOT NULL DEFAULT 0 COMMENT '扣分数量' AFTER match_suspension_remaining,
    ADD COLUMN record_status varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '记录状态' AFTER discipline_status,
    ADD COLUMN approved_by bigint DEFAULT NULL COMMENT '审核人' AFTER decision_time,
    ADD COLUMN approved_time datetime DEFAULT NULL COMMENT '审核时间' AFTER approved_by,
    ADD COLUMN revoked_by bigint DEFAULT NULL COMMENT '撤销人' AFTER approved_time,
    ADD COLUMN revoked_time datetime DEFAULT NULL COMMENT '撤销时间' AFTER revoked_by,
    ADD COLUMN revoke_reason varchar(1000) DEFAULT NULL COMMENT '撤销原因' AFTER revoked_time,
    ADD COLUMN source_type varchar(32) DEFAULT 'ADMIN' COMMENT '来源类型' AFTER revoke_reason,
    ADD COLUMN idempotency_key varchar(128) DEFAULT NULL COMMENT '幂等键' AFTER source_type;

ALTER TABLE basket_discipline_record ADD UNIQUE KEY uk_discipline_idempotency (idempotency_key);

-- Current restriction snapshot
CREATE TABLE IF NOT EXISTS basket_discipline_restriction (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject_type varchar(32) NOT NULL COMMENT '对象类型 PLAYER PERMANENT_TEAM EVENT_TEAM ACCOUNT',
    subject_id bigint NOT NULL COMMENT '对象主键',
    season_id bigint DEFAULT NULL COMMENT '赛季主键',
    allow_registration tinyint NOT NULL DEFAULT 1 COMMENT '允许报名',
    allow_transfer tinyint NOT NULL DEFAULT 1 COMMENT '允许转会',
    allow_captain tinyint NOT NULL DEFAULT 1 COMMENT '允许担任队长',
    allow_lineup tinyint NOT NULL DEFAULT 1 COMMENT '允许进入比赛名单',
    allow_participation tinyint NOT NULL DEFAULT 1 COMMENT '允许参赛',
    remaining_match_suspensions int NOT NULL DEFAULT 0 COMMENT '剩余禁赛场次',
    effective_discipline_id bigint DEFAULT NULL COMMENT '当前生效处罚主键',
    active_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前有效',
    effective_start_time datetime DEFAULT NULL COMMENT '生效开始',
    effective_end_time datetime DEFAULT NULL COMMENT '生效结束',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_restriction_subject_active (subject_type, subject_id, season_id, active_flag, is_delete),
    KEY idx_restriction_season (season_id, active_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处罚当前限制状态表';

-- Match suspension consumption log
CREATE TABLE IF NOT EXISTS basket_discipline_suspension_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    discipline_id bigint NOT NULL COMMENT '处罚主键',
    match_id bigint NOT NULL COMMENT '比赛主键',
    member_id bigint NOT NULL COMMENT '会员主键',
    consume_count int NOT NULL DEFAULT 1 COMMENT '消耗场次',
    idempotency_key varchar(128) NOT NULL COMMENT '幂等键',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suspension_consume (idempotency_key),
    KEY idx_suspension_discipline (discipline_id, match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='禁赛场次消耗记录表';

-- Point adjustment linked to discipline
CREATE TABLE IF NOT EXISTS basket_point_adjustment (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    season_id bigint NOT NULL COMMENT '赛季主键',
    stage_id bigint DEFAULT NULL COMMENT '阶段主键',
    group_id bigint DEFAULT NULL COMMENT '分组主键',
    team_id bigint NOT NULL COMMENT '球队主键',
    discipline_id bigint DEFAULT NULL COMMENT '处罚主键',
    adjustment_points int NOT NULL COMMENT '调整积分',
    adjustment_reason varchar(1000) NOT NULL COMMENT '调整原因',
    idempotency_key varchar(128) NOT NULL COMMENT '幂等键',
    operator_id bigint NOT NULL COMMENT '操作人',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_adjust_idempotency (idempotency_key),
    KEY idx_point_adjust_team_season (season_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分调整记录表';

-- Business audit main
CREATE TABLE IF NOT EXISTS basket_business_audit (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    audit_no varchar(64) NOT NULL COMMENT '审计编号',
    biz_module varchar(64) NOT NULL COMMENT '业务模块',
    tournament_id bigint DEFAULT NULL COMMENT '赛事主键',
    season_id bigint DEFAULT NULL COMMENT '赛季主键',
    object_type varchar(64) NOT NULL COMMENT '对象类型',
    object_id bigint NOT NULL COMMENT '对象主键',
    biz_no varchar(128) DEFAULT NULL COMMENT '业务编号',
    action_type varchar(64) NOT NULL COMMENT '操作类型',
    action_result varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
    change_summary varchar(2000) DEFAULT NULL COMMENT '变更摘要',
    action_reason varchar(2000) DEFAULT NULL COMMENT '操作原因',
    operator_id bigint NOT NULL COMMENT '操作人',
    operator_name varchar(64) DEFAULT NULL COMMENT '操作人名称',
    operator_role varchar(128) DEFAULT NULL COMMENT '操作人角色',
    related_apply_id bigint DEFAULT NULL COMMENT '关联申请',
    related_audit_id bigint DEFAULT NULL COMMENT '关联审核',
    related_discipline_id bigint DEFAULT NULL COMMENT '关联处罚',
    related_match_id bigint DEFAULT NULL COMMENT '关联比赛',
    object_version int DEFAULT NULL COMMENT '对象版本',
    trace_id varchar(64) DEFAULT NULL COMMENT '链路追踪',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_audit_no (audit_no),
    KEY idx_business_audit_module_time (biz_module, create_time),
    KEY idx_business_audit_object (object_type, object_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务审计主表';

CREATE TABLE IF NOT EXISTS basket_business_audit_field (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    audit_id bigint NOT NULL COMMENT '审计主键',
    field_name varchar(128) NOT NULL COMMENT '字段名',
    field_label varchar(128) DEFAULT NULL COMMENT '字段中文名',
    before_value varchar(2000) DEFAULT NULL COMMENT '变更前值',
    after_value varchar(2000) DEFAULT NULL COMMENT '变更后值',
    data_type varchar(32) DEFAULT 'STRING' COMMENT '数据类型',
    sensitive_flag tinyint NOT NULL DEFAULT 0 COMMENT '是否敏感',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_audit_field_audit (audit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务审计字段明细表';

-- Player team relation history
CREATE TABLE IF NOT EXISTS basket_player_team_relation (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    player_id bigint NOT NULL COMMENT '球员主键',
    team_id bigint NOT NULL COMMENT '球队主键',
    member_id bigint DEFAULT NULL COMMENT '会员主键',
    relation_type varchar(32) NOT NULL DEFAULT 'MEMBER' COMMENT '关系类型',
    captain_flag tinyint NOT NULL DEFAULT 0 COMMENT '是否队长',
    join_time datetime NOT NULL COMMENT '加入时间',
    leave_time datetime DEFAULT NULL COMMENT '离开时间',
    join_source varchar(32) DEFAULT NULL COMMENT '加入来源',
    leave_reason varchar(1000) DEFAULT NULL COMMENT '离开原因',
    related_change_id bigint DEFAULT NULL COMMENT '关联变更主键',
    active_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前有效',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_player_team_active (player_id, active_flag),
    KEY idx_team_player_history (team_id, player_id, join_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员球队关系历史表';

-- Team profile change history
CREATE TABLE IF NOT EXISTS basket_team_profile_change (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    team_id bigint NOT NULL COMMENT '球队主键',
    change_type varchar(32) NOT NULL COMMENT '变更类型',
    before_value varchar(2000) DEFAULT NULL COMMENT '变更前值',
    after_value varchar(2000) DEFAULT NULL COMMENT '变更后值',
    change_reason varchar(1000) DEFAULT NULL COMMENT '变更原因',
    applicant_id bigint DEFAULT NULL COMMENT '申请人',
    auditor_id bigint DEFAULT NULL COMMENT '审核人',
    apply_time datetime DEFAULT NULL COMMENT '申请时间',
    audit_time datetime DEFAULT NULL COMMENT '审核时间',
    effective_time datetime DEFAULT NULL COMMENT '生效时间',
    change_status varchar(32) NOT NULL DEFAULT 'EFFECTIVE' COMMENT '变更状态',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_team_profile_change (team_id, change_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队资料变更历史表';

-- Tournament admin scope
CREATE TABLE IF NOT EXISTS basket_tournament_admin_scope (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    admin_user_id bigint NOT NULL COMMENT '管理员用户主键',
    tournament_id bigint NOT NULL COMMENT '赛事主键',
    scope_code varchar(64) NOT NULL DEFAULT 'MANAGE' COMMENT '授权范围',
    granted_by bigint DEFAULT NULL COMMENT '授权人',
    granted_time datetime NOT NULL COMMENT '授权时间',
    expire_time datetime DEFAULT NULL COMMENT '过期时间',
    active_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否有效',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_tournament_scope (admin_user_id, tournament_id, scope_code, active_flag, is_delete),
    KEY idx_tournament_scope_tournament (tournament_id, active_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员赛事授权表';

-- Announcement rule supersede
CREATE TABLE IF NOT EXISTS basket_announcement_rule_link (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    tournament_id bigint DEFAULT NULL COMMENT '赛事主键',
    season_id bigint DEFAULT NULL COMMENT '赛季主键',
    old_announcement_id bigint NOT NULL COMMENT '旧公告主键',
    new_announcement_id bigint NOT NULL COMMENT '新公告主键',
    rule_version varchar(64) DEFAULT NULL COMMENT '规则版本号',
    effective_time datetime NOT NULL COMMENT '生效时间',
    invalid_time datetime DEFAULT NULL COMMENT '失效时间',
    current_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前有效',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_current (season_id, current_flag, is_delete),
    KEY idx_rule_old (old_announcement_id),
    KEY idx_rule_new (new_announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则公告替代关系表';

ALTER TABLE basket_announcement
    ADD COLUMN tournament_id bigint DEFAULT NULL COMMENT '关联赛事主键' AFTER category_code,
    ADD COLUMN season_id bigint DEFAULT NULL COMMENT '关联赛季主键' AFTER tournament_id,
    ADD COLUMN scheduled_publish_time datetime DEFAULT NULL COMMENT '定时发布时间' AFTER publish_time,
    ADD COLUMN superseded_by bigint DEFAULT NULL COMMENT '被替代的新公告主键' AFTER offline_time,
    ADD COLUMN rule_version varchar(64) DEFAULT NULL COMMENT '规则版本号' AFTER superseded_by;

-- Honor generation batch
CREATE TABLE IF NOT EXISTS basket_honor_generation_batch (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no varchar(64) NOT NULL COMMENT '批次编号',
    season_id bigint NOT NULL COMMENT '赛季主键',
    settlement_batch_id bigint DEFAULT NULL COMMENT '结算批次主键',
    batch_status varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '批次状态',
    honor_count int NOT NULL DEFAULT 0 COMMENT '荣誉数量',
    operator_id bigint NOT NULL COMMENT '操作人',
    error_message varchar(2000) DEFAULT NULL COMMENT '错误信息',
    current_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否当前有效批次',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_honor_gen_batch_no (batch_no),
    KEY idx_honor_gen_season (season_id, current_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉生成批次表';

ALTER TABLE basket_honor_record
    ADD COLUMN generation_batch_id bigint DEFAULT NULL COMMENT '荣誉生成批次主键' AFTER source_id;

-- Batch6 menus under 2207 discipline & archive
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2240, '球队与球员处罚', 2207, 1, 'penalty', 'cnpa/discipline/penalty/index', 'BasketDisciplinePenalty', 1, 0, 'C', '0', '0', 'basket:discipline:list', 'warning', 'admin', NOW(), 'CNPA处罚管理')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2241, '永久球员档案', 2207, 2, 'permanent-player', 'cnpa/discipline/permanent-player/index', 'BasketPermanentPlayer', 1, 0, 'C', '0', '0', 'basket:permanentPlayer:list', 'user', 'admin', NOW(), 'CNPA永久球员档案')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2242, '永久球队档案', 2207, 3, 'permanent-team', 'cnpa/discipline/permanent-team/index', 'BasketPermanentTeam', 1, 0, 'C', '0', '0', 'basket:permanentTeam:list', 'peoples', 'admin', NOW(), 'CNPA永久球队档案')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2243, '业务审计记录', 2207, 4, 'audit-log', 'cnpa/discipline/audit-log/index', 'BasketBusinessAudit', 1, 0, 'C', '0', '0', 'basket:businessAudit:list', 'log', 'admin', NOW(), 'CNPA业务审计')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2244, '赛事管理员授权', 2207, 5, 'tournament-scope', 'cnpa/discipline/tournament-scope/index', 'BasketTournamentScope', 1, 0, 'C', '0', '0', 'basket:tournamentScope:list', 'lock', 'admin', NOW(), 'CNPA赛事授权')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2240);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2241);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2242);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2243);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2244);

-- Fix batch5 orphan parent: ensure 2205 exists as alias to competition parent if missing
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2205, '比赛管理', 0, 3, 'competition', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), 'CNPA比赛管理目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2205);

UPDATE sys_menu SET parent_id = 2205 WHERE menu_id IN (2230, 2231, 2232) AND parent_id <> 2205;

UPDATE sys_menu SET perms = 'basket:dashboard:summary' WHERE menu_id = 2201 AND (perms IS NULL OR perms = '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2245, '处罚生效', 2240, 1, '#', '', '', 1, 0, 'F', '0', '0', 'basket:discipline:activate', '#', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE perms=VALUES(perms);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2246, '处罚撤销', 2240, 2, '#', '', '', 1, 0, 'F', '0', '0', 'basket:discipline:revoke', '#', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE perms=VALUES(perms);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2247, '赛事授权授予', 2244, 1, '#', '', '', 1, 0, 'F', '0', '0', 'basket:tournamentScope:grant', '#', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE perms=VALUES(perms);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2248, '赛事授权撤销', 2244, 2, '#', '', '', 1, 0, 'F', '0', '0', 'basket:tournamentScope:revoke', '#', 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE perms=VALUES(perms);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2245), (2, 2246), (2, 2247), (2, 2248);
