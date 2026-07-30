-- CNPA Match Data Dispute & Exception P1 migration
-- MySQL 8.0+, idempotent

-- ========== 1. Extend basket_match_data_dispute ==========
SET @db := DATABASE();

-- submission_id
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'submission_id') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN submission_id BIGINT DEFAULT NULL COMMENT ''关联提交ID'' AFTER match_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- initiator_member_id
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'initiator_member_id') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN initiator_member_id BIGINT DEFAULT NULL COMMENT ''发起人会员ID'' AFTER initiator_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- initiator_player_id
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'initiator_player_id') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN initiator_player_id BIGINT DEFAULT NULL COMMENT ''发起人球员ID'' AFTER initiator_member_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- initiator_team_id
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'initiator_team_id') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN initiator_team_id BIGINT DEFAULT NULL COMMENT ''发起方球队ID'' AFTER initiator_player_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- dispute_deadline_time
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'dispute_deadline_time') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN dispute_deadline_time DATETIME DEFAULT NULL COMMENT ''争议截止时间'' AFTER dispute_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- processed_by
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'processed_by') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN processed_by BIGINT DEFAULT NULL COMMENT ''处理人ID'' AFTER dispute_deadline_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- processed_by_name
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'processed_by_name') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN processed_by_name VARCHAR(64) DEFAULT NULL COMMENT ''处理人名称'' AFTER processed_by',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- processed_time
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'processed_time') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN processed_time DATETIME DEFAULT NULL COMMENT ''处理时间'' AFTER processed_by_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- process_result
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'process_result') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN process_result VARCHAR(32) DEFAULT NULL COMMENT ''处理结果'' AFTER processed_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- reject_reason
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'reject_reason') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN reject_reason VARCHAR(1000) DEFAULT NULL COMMENT ''驳回原因'' AFTER process_result',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- correction_type
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'correction_type') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN correction_type VARCHAR(32) DEFAULT NULL COMMENT ''修正类型'' AFTER reject_reason',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- active_dispute_slot generated column
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'active_dispute_slot') = 0,
    'ALTER TABLE basket_match_data_dispute ADD COLUMN active_dispute_slot VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN is_delete = 0 AND dispute_status IN (''PENDING'',''PROCESSING'')
            THEN CAST(match_id AS CHAR)
            ELSE NULL
        END
    ) STORED COMMENT ''当前有效争议槽位''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_match_active_dispute
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND INDEX_NAME = 'uk_match_active_dispute') = 0,
    'ALTER TABLE basket_match_data_dispute ADD UNIQUE KEY uk_match_active_dispute (active_dispute_slot)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 2. basket_match_data_dispute_material ==========
CREATE TABLE IF NOT EXISTS basket_match_data_dispute_material (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dispute_id BIGINT NOT NULL COMMENT '争议ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    material_type VARCHAR(32) NOT NULL COMMENT '材料类型',
    material_name VARCHAR(128) DEFAULT NULL COMMENT '材料名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_dispute_material (dispute_id, is_delete),
    KEY idx_dispute_material_match (match_id, is_delete),
    KEY idx_dispute_material_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据争议材料表';

-- ========== 3. basket_match_exception_application ==========
CREATE TABLE IF NOT EXISTS basket_match_exception_application (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_no VARCHAR(64) NOT NULL COMMENT '申请编号',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    season_id BIGINT NOT NULL COMMENT '赛季ID',
    stage_id BIGINT DEFAULT NULL COMMENT '阶段ID',
    platform_code VARCHAR(16) DEFAULT NULL COMMENT '平台',
    applicant_member_id BIGINT NOT NULL COMMENT '申请人用户ID',
    applicant_player_id BIGINT NOT NULL COMMENT '申请人球员ID',
    applicant_team_id BIGINT NOT NULL COMMENT '申请方球队ID',
    opponent_team_id BIGINT NOT NULL COMMENT '对方球队ID',
    exception_type VARCHAR(32) NOT NULL COMMENT '异常类型',
    exception_reason VARCHAR(1000) NOT NULL COMMENT '异常说明',
    application_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态',
    decision_type VARCHAR(32) DEFAULT NULL COMMENT '裁定类型',
    decision_result VARCHAR(1000) DEFAULT NULL COMMENT '裁定说明',
    decision_score_home INT DEFAULT NULL COMMENT '裁定主队比分',
    decision_score_away INT DEFAULT NULL COMMENT '裁定客队比分',
    winner_team_id BIGINT DEFAULT NULL COMMENT '裁定胜方',
    walkover_team_id BIGINT DEFAULT NULL COMMENT '弃赛方',
    processed_by BIGINT DEFAULT NULL COMMENT '处理人',
    processed_by_name VARCHAR(64) DEFAULT NULL COMMENT '处理人名称',
    processed_time DATETIME DEFAULT NULL COMMENT '处理时间',
    active_application_slot VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN is_delete = 0 AND application_status IN ('PENDING','PROCESSING')
            THEN CAST(match_id AS CHAR)
            ELSE NULL
        END
    ) STORED COMMENT '当前有效异常申请槽位',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exception_application_no (application_no),
    UNIQUE KEY uk_match_active_exception (active_application_slot),
    KEY idx_exception_match (match_id, application_status, is_delete),
    KEY idx_exception_season (season_id, application_status, create_time),
    KEY idx_exception_applicant (applicant_member_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛异常申请表';

-- ========== 4. basket_match_exception_material ==========
CREATE TABLE IF NOT EXISTS basket_match_exception_material (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    material_type VARCHAR(32) NOT NULL COMMENT '材料类型',
    material_name VARCHAR(128) DEFAULT NULL COMMENT '材料名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_exception_material (application_id, is_delete),
    KEY idx_exception_material_match (match_id, is_delete),
    KEY idx_exception_material_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛异常材料表';

-- ========== 5. basket_match_exception_audit ==========
CREATE TABLE IF NOT EXISTS basket_match_exception_audit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    audit_action VARCHAR(32) NOT NULL COMMENT '审核动作',
    before_status VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
    after_status VARCHAR(32) DEFAULT NULL COMMENT '变更后状态',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    audit_remark VARCHAR(1000) DEFAULT NULL COMMENT '审核备注',
    reject_reason VARCHAR(1000) DEFAULT NULL COMMENT '驳回原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_exception_audit_app (application_id, create_time),
    KEY idx_exception_audit_match (match_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛异常审核日志表';

-- ========== 6. basket_match_data_dispute audit log (reuse basket_match_data_audit via application code) ==========
