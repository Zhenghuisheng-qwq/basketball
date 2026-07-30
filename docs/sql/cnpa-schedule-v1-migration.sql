-- CNPA Schedule V1: 赛程规则体系 + 比赛表扩展
-- 幂等执行：表/列/索引/种子数据均可重复运行

-- ========== 1. 赛程规则主表 ==========
CREATE TABLE IF NOT EXISTS basket_schedule_rule (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name       VARCHAR(128) NOT NULL COMMENT '规则名称',
    rule_code       VARCHAR(64)  NOT NULL COMMENT '规则编码',
    rule_version    VARCHAR(16)  NOT NULL COMMENT '规则版本',
    event_type      VARCHAR(32)  DEFAULT NULL COMMENT '赛事类型',
    platform_code   VARCHAR(16)  DEFAULT NULL COMMENT '游戏端',
    enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '启用 0否 1是',
    remark          VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_rule_code_ver (rule_code, rule_version, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛程规则主表';

-- ========== 2. 规则比赛日 ==========
CREATE TABLE IF NOT EXISTS basket_schedule_rule_weekday (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id     BIGINT   NOT NULL COMMENT '规则主键',
    weekday     TINYINT  NOT NULL COMMENT '1周一..7周日',
    sort        INT      NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_rule_weekday_rule (rule_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛程规则比赛日';

-- ========== 3. 规则时间段 ==========
CREATE TABLE IF NOT EXISTS basket_schedule_rule_time_slot (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id     BIGINT      NOT NULL COMMENT '规则主键',
    start_time  VARCHAR(8)  NOT NULL COMMENT '开始时间 HH:mm',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete   TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_rule_slot_rule (rule_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛程规则时间段';

-- ========== 4. 赛程生成批次 ==========
CREATE TABLE IF NOT EXISTS basket_schedule_generate_batch (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no         VARCHAR(64)  NOT NULL COMMENT '批次编号',
    event_id         BIGINT       NOT NULL COMMENT '赛事赛季ID(eventId=seasonId)',
    rule_id          BIGINT       NOT NULL COMMENT '规则主键',
    rule_code        VARCHAR(64)  NOT NULL COMMENT '规则编码快照',
    rule_version     VARCHAR(16)  NOT NULL COMMENT '规则版本快照',
    event_team_count INT          NOT NULL DEFAULT 0 COMMENT '参赛球队数',
    match_count      INT          NOT NULL DEFAULT 0 COMMENT '生成比赛数',
    generate_status  TINYINT      NOT NULL DEFAULT 1 COMMENT '生成状态 1成功 2失败',
    publish_status   TINYINT      NOT NULL DEFAULT 0 COMMENT '发布状态 0草稿 1已发布 2已下架',
    generated_by     BIGINT       DEFAULT NULL COMMENT '生成人',
    generated_time   DATETIME     DEFAULT NULL COMMENT '生成时间',
    publish_time     DATETIME     DEFAULT NULL COMMENT '发布时间',
    remark           VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_batch_no (batch_no),
    KEY idx_schedule_batch_event (event_id, publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛程生成批次';

-- ========== 5. basket_match 扩展 ==========
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'schedule_batch_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN schedule_batch_id BIGINT DEFAULT NULL COMMENT ''赛程批次主键'' AFTER match_no',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'platform_code');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN platform_code VARCHAR(16) DEFAULT NULL COMMENT ''游戏端'' AFTER season_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'rule_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN rule_id BIGINT DEFAULT NULL COMMENT ''规则主键'' AFTER platform_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'rule_code');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN rule_code VARCHAR(64) DEFAULT NULL COMMENT ''规则编码'' AFTER rule_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'rule_version');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN rule_version VARCHAR(16) DEFAULT NULL COMMENT ''规则版本'' AFTER rule_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'round_no');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN round_no INT DEFAULT NULL COMMENT ''轮次号'' AFTER round_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'home_event_team_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN home_event_team_id BIGINT DEFAULT NULL COMMENT ''主队赛事球队ID'' AFTER away_team_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'away_event_team_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN away_event_team_id BIGINT DEFAULT NULL COMMENT ''客队赛事球队ID'' AFTER home_event_team_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'home_team_name_snapshot');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN home_team_name_snapshot VARCHAR(128) DEFAULT NULL COMMENT ''主队名称快照'' AFTER away_event_team_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'away_team_name_snapshot');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN away_team_name_snapshot VARCHAR(128) DEFAULT NULL COMMENT ''客队名称快照'' AFTER home_team_name_snapshot',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'home_team_logo_snapshot');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN home_team_logo_snapshot VARCHAR(512) DEFAULT NULL COMMENT ''主队Logo快照'' AFTER away_team_name_snapshot',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'away_team_logo_snapshot');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN away_team_logo_snapshot VARCHAR(512) DEFAULT NULL COMMENT ''客队Logo快照'' AFTER home_team_logo_snapshot',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'publish_status');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN publish_status TINYINT NOT NULL DEFAULT 0 COMMENT ''发布状态 0草稿 1已发布 2已下架'' AFTER public_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'winner_event_team_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN winner_event_team_id BIGINT DEFAULT NULL COMMENT ''胜方赛事球队ID'' AFTER winner_team_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND COLUMN_NAME = 'schedule_remark');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_match ADD COLUMN schedule_remark VARCHAR(1000) DEFAULT NULL COMMENT ''赛程备注'' AFTER cancel_reason',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引（幂等）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND INDEX_NAME = 'idx_match_publish_status');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE basket_match ADD KEY idx_match_publish_status (season_id, publish_status, scheduled_start_time)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_match' AND INDEX_NAME = 'idx_match_event_team_pair');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE basket_match ADD KEY idx_match_event_team_pair (season_id, home_event_team_id, away_event_team_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 6. V1 种子规则（比赛日/时间段仅存配置表，不写死在代码里） ==========
INSERT INTO basket_schedule_rule (rule_name, rule_code, rule_version, event_type, platform_code, enabled, remark, is_delete)
SELECT '常规赛单循环 V1', 'REGULAR_ROUND_ROBIN', 'V1', 'REGULAR', NULL, 1,
       '每两支正式参赛 EventTeam 生成一场比赛，比赛日与时间段由本规则配置决定', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM basket_schedule_rule
    WHERE rule_code = 'REGULAR_ROUND_ROBIN' AND rule_version = 'V1' AND is_delete = 0
);

SET @rule_id := (SELECT id FROM basket_schedule_rule
    WHERE rule_code = 'REGULAR_ROUND_ROBIN' AND rule_version = 'V1' AND is_delete = 0 LIMIT 1);

INSERT INTO basket_schedule_rule_weekday (rule_id, weekday, sort, is_delete)
SELECT @rule_id, w.weekday, w.sort, 0
FROM (
    SELECT 2 AS weekday, 1 AS sort UNION ALL
    SELECT 4, 2 UNION ALL
    SELECT 6, 3
) w
WHERE @rule_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_schedule_rule_weekday wd
    WHERE wd.rule_id = @rule_id AND wd.weekday = w.weekday AND wd.is_delete = 0
);

INSERT INTO basket_schedule_rule_time_slot (rule_id, start_time, sort, is_delete)
SELECT @rule_id, t.start_time, t.sort, 0
FROM (
    SELECT '09:00' AS start_time, 1 AS sort UNION ALL
    SELECT '10:00', 2 UNION ALL
    SELECT '20:00', 3 UNION ALL
    SELECT '21:00', 4
) t
WHERE @rule_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_schedule_rule_time_slot ts
    WHERE ts.rule_id = @rule_id AND ts.start_time = t.start_time AND ts.is_delete = 0
);

-- 兼容旧数据：原 public_status=1 的比赛视为已发布
UPDATE basket_match SET publish_status = 1
WHERE public_status = 1 AND is_delete = 0 AND publish_status = 0;

-- ========== 7. 后台菜单（幂等，需根据实际 parent_id 调整） ==========
-- 建议在「比赛执行」目录下新增「赛程规则管理」菜单，component=cnpa/competition/schedule-manage/index
-- 权限前缀：basket:schedule:list/query/generate/publish/remove/add/edit/score
