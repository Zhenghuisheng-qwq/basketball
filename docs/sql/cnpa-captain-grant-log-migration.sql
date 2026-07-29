-- CNPA 队长授权历史日志表迁移（幂等，可重复执行）
-- 主表复用 basket_captain_qualification（语义等同 basket_captain_grant）
-- 执行：mysql -u... -p... your_db < docs/sql/cnpa-captain-grant-log-migration.sql

START TRANSACTION;

DROP PROCEDURE IF EXISTS cnpa_add_index_if_missing;

DELIMITER //
CREATE PROCEDURE cnpa_add_index_if_missing(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @cnpa_sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_definition);
        PREPARE cnpa_stmt FROM @cnpa_sql;
        EXECUTE cnpa_stmt;
        DEALLOCATE PREPARE cnpa_stmt;
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 队长授权历史日志表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_captain_grant_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    grant_id BIGINT NOT NULL COMMENT '队长授权主键（basket_captain_qualification.id）',
    member_id BIGINT NOT NULL COMMENT '用户ID',
    player_id BIGINT NULL COMMENT '球员档案ID',
    team_id BIGINT NULL COMMENT '球队ID',
    season_id BIGINT NOT NULL COMMENT '赛事赛季ID（eventId）',
    action_type VARCHAR(32) NOT NULL COMMENT '操作类型 GRANT/BIND_TEAM/REVOKE/EXPIRE/...',
    before_status TINYINT NULL COMMENT '操作前状态',
    after_status TINYINT NULL COMMENT '操作后状态',
    payment_confirmed TINYINT NULL COMMENT '缴费是否确认',
    payment_reference VARCHAR(128) NULL COMMENT '缴费参考号',
    grant_time DATETIME NULL COMMENT '授权时间快照',
    expire_time DATETIME NULL COMMENT '失效时间快照',
    revoke_time DATETIME NULL COMMENT '收回时间快照',
    operator_id BIGINT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) NULL COMMENT '操作人名称',
    operator_type VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT '操作人类型 ADMIN/SYSTEM',
    reason VARCHAR(500) NULL COMMENT '操作原因',
    remark VARCHAR(500) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_log_grant (grant_id),
    KEY idx_log_user (member_id),
    KEY idx_log_event (season_id),
    KEY idx_log_time (create_time),
    KEY idx_log_action (action_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队长授权历史日志表';

-- 主表补充查询索引（如迁移脚本已执行则跳过）
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_grant_user_event',
    'KEY idx_grant_user_event (member_id, season_id, qualification_status, is_delete)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_grant_team_event',
    'KEY idx_grant_team_event (team_id, season_id, qualification_status, is_delete)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_grant_status',
    'KEY idx_grant_status (qualification_status, is_delete)');

DROP PROCEDURE IF EXISTS cnpa_add_index_if_missing;

COMMIT;
