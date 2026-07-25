-- CNPA Batch 4 migration: formal event team/roster, open registration, roster changes
-- Database: basketball (local dev)

-- 1. Extend season for registration modes and windows
ALTER TABLE basket_tournament_season
    ADD COLUMN allow_team_registration tinyint NOT NULL DEFAULT 1 COMMENT '允许球队报名' AFTER max_roster_count,
    ADD COLUMN allow_open_registration tinyint NOT NULL DEFAULT 0 COMMENT '允许Open报名' AFTER allow_team_registration,
    ADD COLUMN transfer_enabled tinyint NOT NULL DEFAULT 0 COMMENT '允许转会' AFTER allow_open_registration,
    ADD COLUMN transfer_start_time datetime NULL COMMENT '转会窗口开始' AFTER transfer_enabled,
    ADD COLUMN transfer_end_time datetime NULL COMMENT '转会窗口结束' AFTER transfer_start_time,
    ADD COLUMN free_agent_enabled tinyint NOT NULL DEFAULT 0 COMMENT '允许自由球员补员' AFTER transfer_end_time,
    ADD COLUMN free_agent_start_time datetime NULL COMMENT '补员窗口开始' AFTER free_agent_enabled,
    ADD COLUMN free_agent_end_time datetime NULL COMMENT '补员窗口结束' AFTER free_agent_start_time,
    ADD COLUMN max_free_agent_add_per_team int NOT NULL DEFAULT 3 COMMENT '每队最大补员次数' AFTER free_agent_end_time;

-- 2. Extend formal event team (team season profile)
ALTER TABLE basket_team_season_profile
    ADD COLUMN registration_id bigint NULL COMMENT '来源报名申请主键' AFTER season_id,
    ADD COLUMN event_team_no varchar(32) NULL COMMENT '赛事球队编号' AFTER registration_id,
    ADD COLUMN team_name_snapshot varchar(128) NULL COMMENT '球队名称快照' AFTER event_team_no,
    ADD COLUMN short_name_snapshot varchar(32) NULL COMMENT '球队简称快照' AFTER team_name_snapshot,
    ADD COLUMN description_snapshot varchar(2000) NULL COMMENT '球队简介快照' AFTER short_name_snapshot,
    ADD COLUMN game_terminal varchar(16) NULL COMMENT '比赛平台快照' AFTER description_snapshot,
    ADD COLUMN registration_source varchar(32) NULL DEFAULT 'TEAM_REGISTRATION' COMMENT '报名来源' AFTER game_terminal,
    ADD COLUMN participation_status tinyint NOT NULL DEFAULT 1 COMMENT '参赛状态' AFTER registration_source,
    ADD COLUMN roster_lock_status tinyint NOT NULL DEFAULT 0 COMMENT '阵容锁定状态' AFTER participation_status,
    ADD COLUMN roster_locked_time datetime NULL COMMENT '阵容锁定时间' AFTER roster_lock_status,
    ADD COLUMN approved_time datetime NULL COMMENT '审核通过时间' AFTER roster_locked_time;

ALTER TABLE basket_team_season_profile
    ADD UNIQUE KEY uk_event_team_registration (registration_id, is_delete),
    ADD UNIQUE KEY uk_event_team_no_season (season_id, event_team_no, is_delete),
    ADD KEY idx_event_team_participation (season_id, participation_status, is_delete);

-- 3. Formal event roster
CREATE TABLE IF NOT EXISTS basket_tournament_season_roster (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    roster_no varchar(32) NULL COMMENT '赛事阵容编号',
    season_id bigint NOT NULL COMMENT '赛季主键',
    event_team_id bigint NULL COMMENT '赛事球队主键',
    team_id bigint NULL COMMENT '永久球队主键',
    player_id bigint NOT NULL COMMENT '永久球员主键',
    member_id bigint NOT NULL COMMENT '会员主键',
    registration_id bigint NULL COMMENT '来源球队报名主键',
    registration_player_id bigint NULL COMMENT '来源报名阵容主键',
    open_registration_id bigint NULL COMMENT '来源Open报名主键',
    player_nickname_snapshot varchar(128) NULL COMMENT '球员昵称快照',
    avatar_file_id_snapshot bigint NULL COMMENT '头像文件快照',
    platform_snapshot varchar(16) NULL COMMENT '平台快照',
    game_id_display_snapshot varchar(128) NULL COMMENT '游戏ID展示快照',
    game_id_normalized_snapshot varchar(128) NULL COMMENT '游戏ID规范快照',
    position_snapshot varchar(16) NULL COMMENT '位置快照',
    roster_role varchar(32) NOT NULL DEFAULT 'PLAYER' COMMENT '赛事身份',
    is_captain tinyint NOT NULL DEFAULT 0 COMMENT '是否队长',
    is_starting tinyint NOT NULL DEFAULT 0 COMMENT '是否首发',
    is_substitute tinyint NOT NULL DEFAULT 0 COMMENT '是否替补',
    source_type varchar(32) NOT NULL DEFAULT 'INITIAL_REGISTRATION' COMMENT '来源类型',
    roster_status tinyint NOT NULL DEFAULT 1 COMMENT '阵容状态',
    lock_status tinyint NOT NULL DEFAULT 0 COMMENT '锁定状态',
    active_slot varchar(64) NULL COMMENT '有效槽位键',
    effective_time datetime NULL COMMENT '生效时间',
    invalid_time datetime NULL COMMENT '失效时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_roster_no (roster_no),
    UNIQUE KEY uk_season_roster_active_slot (active_slot),
    KEY idx_roster_event_team (event_team_id, roster_status, is_delete),
    KEY idx_roster_season_player (season_id, player_id, is_delete),
    KEY idx_roster_registration (registration_id, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='正式赛事阵容表';

-- 4. Open registration
CREATE TABLE IF NOT EXISTS basket_tournament_open_registration (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    registration_no varchar(64) NOT NULL COMMENT '报名编号',
    season_id bigint NOT NULL COMMENT '赛季主键',
    member_id bigint NOT NULL COMMENT '会员主键',
    player_id bigint NOT NULL COMMENT '球员主键',
    platform varchar(16) NOT NULL COMMENT '游戏平台',
    display_game_id varchar(128) NOT NULL COMMENT '展示游戏ID',
    normalized_game_id varchar(128) NOT NULL COMMENT '规范游戏ID',
    position_code varchar(16) NULL COMMENT '常用位置',
    registration_status tinyint NOT NULL DEFAULT 0 COMMENT '报名状态',
    submitted_time datetime NULL COMMENT '提交时间',
    audit_time datetime NULL COMMENT '审核时间',
    withdrawal_time datetime NULL COMMENT '撤回时间',
    withdrawal_reason varchar(1000) NULL COMMENT '撤回原因',
    current_auditor_id bigint NULL COMMENT '当前审核人',
    remark varchar(1000) NULL COMMENT '备注',
    result_roster_id bigint NULL COMMENT '生效阵容主键',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_open_registration_no (registration_no),
    UNIQUE KEY uk_open_season_player (season_id, player_id, is_delete),
    KEY idx_open_status_time (season_id, registration_status, submitted_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Open个人报名表';

CREATE TABLE IF NOT EXISTS basket_tournament_open_registration_audit (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    registration_id bigint NOT NULL COMMENT 'Open报名主键',
    audit_action varchar(32) NOT NULL COMMENT '审核动作',
    before_status tinyint NULL COMMENT '变更前状态',
    after_status tinyint NULL COMMENT '变更后状态',
    auditor_id bigint NULL COMMENT '审核人',
    auditor_name varchar(64) NULL COMMENT '审核人名称',
    audit_remark varchar(1000) NULL COMMENT '审核说明',
    audit_time datetime NULL COMMENT '审核时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_open_audit_reg (registration_id, audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Open报名审核记录表';

-- 5. Roster change requests (transfer / free agent / captain transfer)
CREATE TABLE IF NOT EXISTS basket_tournament_roster_change_request (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_no varchar(64) NOT NULL COMMENT '申请编号',
    season_id bigint NOT NULL COMMENT '赛季主键',
    change_type varchar(32) NOT NULL COMMENT '变更类型',
    request_status tinyint NOT NULL DEFAULT 0 COMMENT '申请状态',
    applicant_member_id bigint NOT NULL COMMENT '申请人会员主键',
    player_id bigint NOT NULL COMMENT '球员主键',
    source_event_team_id bigint NULL COMMENT '原赛事球队主键',
    target_event_team_id bigint NULL COMMENT '目标赛事球队主键',
    source_roster_id bigint NULL COMMENT '原阵容主键',
    new_captain_player_id bigint NULL COMMENT '新队长球员主键',
    target_roster_role varchar(32) NULL COMMENT '目标身份',
    target_is_starting tinyint NULL COMMENT '目标首发标记',
    target_is_substitute tinyint NULL COMMENT '目标替补标记',
    apply_reason varchar(1000) NULL COMMENT '申请原因',
    reject_reason varchar(1000) NULL COMMENT '驳回原因',
    submit_time datetime NULL COMMENT '提交时间',
    reviewed_time datetime NULL COMMENT '审核时间',
    reviewer_id bigint NULL COMMENT '审核人',
    review_remark varchar(1000) NULL COMMENT '审核备注',
    result_roster_id bigint NULL COMMENT '结果阵容主键',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_roster_change_request_no (request_no),
    KEY idx_roster_change_player (season_id, player_id, request_status, is_delete),
    KEY idx_roster_change_target (target_event_team_id, request_status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事阵容变更申请表';

CREATE TABLE IF NOT EXISTS basket_tournament_roster_change_record (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    season_id bigint NOT NULL COMMENT '赛季主键',
    change_request_id bigint NULL COMMENT '变更申请主键',
    change_type varchar(32) NOT NULL COMMENT '变更类型',
    player_id bigint NOT NULL COMMENT '球员主键',
    source_event_team_id bigint NULL COMMENT '原赛事球队',
    target_event_team_id bigint NULL COMMENT '目标赛事球队',
    source_roster_id bigint NULL COMMENT '原阵容主键',
    target_roster_id bigint NULL COMMENT '新阵容主键',
    before_snapshot varchar(2000) NULL COMMENT '变更前摘要',
    after_snapshot varchar(2000) NULL COMMENT '变更后摘要',
    operator_id bigint NULL COMMENT '操作人',
    operator_name varchar(64) NULL COMMENT '操作人名称',
    operate_reason varchar(1000) NULL COMMENT '操作原因',
    effective_time datetime NULL COMMENT '生效时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_roster_change_record_season (season_id, player_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事阵容变更历史表';

CREATE TABLE IF NOT EXISTS basket_tournament_roster_operation_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    season_id bigint NOT NULL COMMENT '赛季主键',
    event_team_id bigint NULL COMMENT '赛事球队主键',
    operation_type varchar(32) NOT NULL COMMENT '操作类型',
    target_roster_id bigint NULL COMMENT '目标阵容主键',
    target_player_id bigint NULL COMMENT '目标球员主键',
    before_value varchar(2000) NULL COMMENT '变更前',
    after_value varchar(2000) NULL COMMENT '变更后',
    operate_reason varchar(1000) NOT NULL COMMENT '操作原因',
    effective_time datetime NULL COMMENT '生效时间',
    operator_id bigint NOT NULL COMMENT '操作管理员',
    operator_name varchar(64) NULL COMMENT '操作管理员名称',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_roster_op_season (season_id, event_team_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事阵容特殊操作日志表';

-- 6. Backfill approved team registrations -> event team + roster
-- (Executed in application layer or follow-up script after deploy)
