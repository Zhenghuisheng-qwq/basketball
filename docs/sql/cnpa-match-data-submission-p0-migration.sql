-- CNPA Match Data Submission P0 migration
-- MySQL 8.0+, idempotent where possible

-- ========== 1. basket_match_data_submission ==========
CREATE TABLE IF NOT EXISTS basket_match_data_submission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    submission_no VARCHAR(64) NOT NULL COMMENT '提交编号',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    stage_id BIGINT NOT NULL COMMENT '阶段ID',
    round_id BIGINT DEFAULT NULL COMMENT '轮次ID',
    group_id BIGINT DEFAULT NULL COMMENT '分组ID',
    platform_code VARCHAR(16) DEFAULT NULL COMMENT '平台代码',
    home_team_id BIGINT NOT NULL COMMENT '主队长期球队ID',
    away_team_id BIGINT NOT NULL COMMENT '客队长期球队ID',
    submitter_team_id BIGINT NOT NULL COMMENT '提交方球队ID',
    submitter_member_id BIGINT NOT NULL COMMENT '提交人会员ID',
    submitter_player_id BIGINT NOT NULL COMMENT '提交人球员ID',
    submitter_role VARCHAR(32) NOT NULL DEFAULT 'CAPTAIN' COMMENT '提交角色',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    previous_submission_id BIGINT DEFAULT NULL COMMENT '上一版本提交ID',
    home_score INT NOT NULL DEFAULT 0 COMMENT '主队最终得分',
    away_score INT NOT NULL DEFAULT 0 COMMENT '客队最终得分',
    winner_team_id BIGINT DEFAULT NULL COMMENT '获胜球队ID',
    submission_status TINYINT NOT NULL DEFAULT 1 COMMENT '0草稿 1审核中 2已通过 3已驳回 4被替代 5已取消',
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    submitted_time DATETIME NOT NULL COMMENT '提交时间',
    reviewed_time DATETIME DEFAULT NULL COMMENT '审核时间',
    reviewed_by BIGINT DEFAULT NULL COMMENT '审核人ID',
    reviewed_by_name VARCHAR(64) DEFAULT NULL COMMENT '审核人名称',
    reject_reason VARCHAR(1000) DEFAULT NULL COMMENT '驳回原因',
    audit_remark VARCHAR(1000) DEFAULT NULL COMMENT '审核备注',
    active_submission_slot VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN is_delete = 0 AND submission_status IN (1, 2)
            THEN CAST(match_id AS CHAR)
            ELSE NULL
        END
    ) STORED COMMENT '当前有效提交槽位',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_match_data_submission_no (submission_no),
    UNIQUE KEY uk_match_active_submission (active_submission_slot),
    UNIQUE KEY uk_match_submission_version (match_id, version_no, is_delete),
    KEY idx_submission_match_status (match_id, submission_status, is_delete),
    KEY idx_submission_season_status (season_id, audit_status, submitted_time),
    KEY idx_submission_submitter (submitter_member_id, submitted_time),
    KEY idx_submission_team (submitter_team_id, submitted_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据提交主表';

-- ========== 2. basket_match_data_submission_period ==========
CREATE TABLE IF NOT EXISTS basket_match_data_submission_period (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    period_no INT NOT NULL COMMENT '节次号',
    period_type VARCHAR(16) NOT NULL COMMENT '节次类型',
    home_score INT NOT NULL DEFAULT 0 COMMENT '主队节得分',
    away_score INT NOT NULL DEFAULT 0 COMMENT '客队节得分',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_period (submission_id, period_no, is_delete),
    KEY idx_submission_period_match (match_id, period_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据提交分节比分';

-- ========== 3. basket_match_data_submission_team_stat ==========
CREATE TABLE IF NOT EXISTS basket_match_data_submission_team_stat (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    stage_id BIGINT NOT NULL COMMENT '阶段ID',
    team_id BIGINT NOT NULL COMMENT '球队ID',
    opponent_team_id BIGINT NOT NULL COMMENT '对手球队ID',
    side_type VARCHAR(16) NOT NULL COMMENT 'HOME/AWAY',
    points INT NOT NULL DEFAULT 0,
    rebounds INT NOT NULL DEFAULT 0,
    offensive_rebounds INT NOT NULL DEFAULT 0,
    defensive_rebounds INT NOT NULL DEFAULT 0,
    assists INT NOT NULL DEFAULT 0,
    steals INT NOT NULL DEFAULT 0,
    blocks INT NOT NULL DEFAULT 0,
    turnovers INT NOT NULL DEFAULT 0,
    fouls INT NOT NULL DEFAULT 0,
    field_goals_made INT NOT NULL DEFAULT 0,
    field_goals_attempted INT NOT NULL DEFAULT 0,
    field_goal_pct DECIMAL(7,4) NOT NULL DEFAULT 0,
    three_points_made INT NOT NULL DEFAULT 0,
    three_points_attempted INT NOT NULL DEFAULT 0,
    three_point_pct DECIMAL(7,4) NOT NULL DEFAULT 0,
    free_throws_made INT NOT NULL DEFAULT 0,
    free_throws_attempted INT NOT NULL DEFAULT 0,
    free_throw_pct DECIMAL(7,4) NOT NULL DEFAULT 0,
    possessions DECIMAL(10,2) NOT NULL DEFAULT 0,
    efficiency DECIMAL(10,2) NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_team_stat (submission_id, team_id, is_delete),
    KEY idx_submission_team_stat_match (match_id, team_id),
    KEY idx_submission_team_stat_season (season_id, stage_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据提交球队统计';

-- ========== 4. basket_match_data_submission_player_stat ==========
CREATE TABLE IF NOT EXISTS basket_match_data_submission_player_stat (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    stage_id BIGINT NOT NULL COMMENT '阶段ID',
    team_id BIGINT NOT NULL COMMENT '球队ID',
    player_id BIGINT NOT NULL COMMENT '球员ID',
    roster_id BIGINT DEFAULT NULL COMMENT '出场名单ID',
    started_flag TINYINT NOT NULL DEFAULT 0,
    minutes_seconds INT NOT NULL DEFAULT 0,
    points INT NOT NULL DEFAULT 0,
    rebounds INT NOT NULL DEFAULT 0,
    offensive_rebounds INT NOT NULL DEFAULT 0,
    defensive_rebounds INT NOT NULL DEFAULT 0,
    assists INT NOT NULL DEFAULT 0,
    steals INT NOT NULL DEFAULT 0,
    blocks INT NOT NULL DEFAULT 0,
    turnovers INT NOT NULL DEFAULT 0,
    fouls INT NOT NULL DEFAULT 0,
    field_goals_made INT NOT NULL DEFAULT 0,
    field_goals_attempted INT NOT NULL DEFAULT 0,
    field_goal_pct DECIMAL(7,4) NOT NULL DEFAULT 0,
    three_points_made INT NOT NULL DEFAULT 0,
    three_points_attempted INT NOT NULL DEFAULT 0,
    three_point_pct DECIMAL(7,4) NOT NULL DEFAULT 0,
    free_throws_made INT NOT NULL DEFAULT 0,
    free_throws_attempted INT NOT NULL DEFAULT 0,
    free_throw_pct DECIMAL(7,4) NOT NULL DEFAULT 0,
    plus_minus INT NOT NULL DEFAULT 0,
    efficiency DECIMAL(10,2) NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_player_stat (submission_id, player_id, is_delete),
    KEY idx_submission_player_stat_match (match_id, player_id),
    KEY idx_submission_player_stat_season (season_id, stage_id, team_id),
    KEY idx_submission_player_stat_player (player_id, season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据提交球员统计';

-- ========== 5. basket_match_data_submission_material ==========
CREATE TABLE IF NOT EXISTS basket_match_data_submission_material (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    material_type VARCHAR(32) NOT NULL COMMENT '材料类型',
    material_name VARCHAR(128) DEFAULT NULL COMMENT '材料名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    material_status TINYINT NOT NULL DEFAULT 1 COMMENT '材料状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_submission_material (submission_id, material_type, is_delete),
    KEY idx_submission_material_match (match_id, is_delete),
    KEY idx_submission_material_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据提交材料';

-- ========== 6. basket_match_data_audit 扩展 ==========
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match_data_audit' AND COLUMN_NAME = 'submission_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match_data_audit ADD COLUMN submission_id BIGINT DEFAULT NULL COMMENT ''比赛数据提交ID'' AFTER match_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match_data_audit' AND COLUMN_NAME = 'audit_action');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match_data_audit ADD COLUMN audit_action VARCHAR(32) DEFAULT NULL COMMENT ''审核动作 SUBMIT/RESUBMIT/APPROVE/REJECT'' AFTER audit_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match_data_audit' AND COLUMN_NAME = 'before_status');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match_data_audit ADD COLUMN before_status VARCHAR(32) DEFAULT NULL COMMENT ''变更前状态'' AFTER audit_action',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match_data_audit' AND COLUMN_NAME = 'after_status');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match_data_audit ADD COLUMN after_status VARCHAR(32) DEFAULT NULL COMMENT ''变更后状态'' AFTER before_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match_data_audit' AND COLUMN_NAME = 'auditor_name');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match_data_audit ADD COLUMN auditor_name VARCHAR(64) DEFAULT NULL COMMENT ''审核人名称'' AFTER auditor_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match_data_audit' AND INDEX_NAME = 'idx_audit_submission');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE basket_match_data_audit ADD KEY idx_audit_submission (submission_id, audit_status, create_time)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
