-- CNPA 队长授权 + 关联赛事平台 数据库迁移（幂等，可重复执行）
-- 适用：已有库升级；新库请优先使用 sql/basketball.sql 全量建表
-- 执行：mysql -u... -p... your_db < docs/sql/cnpa-captain-grant-migration.sql

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- 工具过程：列 / 索引 幂等添加
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cnpa_add_column_if_missing;
DROP PROCEDURE IF EXISTS cnpa_add_index_if_missing;

DELIMITER //
CREATE PROCEDURE cnpa_add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @cnpa_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE cnpa_stmt FROM @cnpa_sql;
        EXECUTE cnpa_stmt;
        DEALLOCATE PREPARE cnpa_stmt;
    END IF;
END //

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
-- 1. 队长授权表 basket_captain_qualification 扩展
-- ---------------------------------------------------------------------------
CALL cnpa_add_column_if_missing('basket_captain_qualification', 'season_id',
    "BIGINT NULL COMMENT '赛事赛季主键（队长授权所属赛事）' AFTER player_id");
CALL cnpa_add_column_if_missing('basket_captain_qualification', 'team_id',
    "BIGINT NULL COMMENT '绑定球队主键，未建队前可为空' AFTER season_id");
CALL cnpa_add_column_if_missing('basket_captain_qualification', 'remark',
    "VARCHAR(500) NULL COMMENT '备注' AFTER revoke_reason");

-- 同一用户同一赛事仅允许一条 ACTIVE 授权（DB 层防重复）
CALL cnpa_add_column_if_missing('basket_captain_qualification', 'active_grant_slot',
    "VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN qualification_status = 1 AND is_delete = 0 AND season_id IS NOT NULL
                THEN CONCAT(season_id, ':', member_id)
            ELSE NULL
        END
    ) STORED COMMENT '有效授权槽位 seasonId:memberId' AFTER remark");

-- 同一球队同一赛事仅允许一条 ACTIVE 队长授权
CALL cnpa_add_column_if_missing('basket_captain_qualification', 'team_active_grant_slot',
    "VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN qualification_status = 1 AND is_delete = 0 AND season_id IS NOT NULL AND team_id IS NOT NULL
                THEN CONCAT(season_id, ':', team_id)
            ELSE NULL
        END
    ) STORED COMMENT '球队有效授权槽位 seasonId:teamId' AFTER active_grant_slot");

CALL cnpa_add_index_if_missing('basket_captain_qualification', 'uk_captain_grant_active_slot',
    'UNIQUE KEY uk_captain_grant_active_slot (active_grant_slot)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'uk_captain_grant_team_active_slot',
    'UNIQUE KEY uk_captain_grant_team_active_slot (team_active_grant_slot)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_captain_grant_season_member',
    'KEY idx_captain_grant_season_member (season_id, member_id, qualification_status, is_delete)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_captain_grant_season_status',
    'KEY idx_captain_grant_season_status (season_id, qualification_status, is_current, is_delete)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_captain_grant_team',
    'KEY idx_captain_grant_team (team_id, season_id, qualification_status, is_delete)');
CALL cnpa_add_index_if_missing('basket_captain_qualification', 'idx_captain_grant_valid_to',
    'KEY idx_captain_grant_valid_to (valid_to, qualification_status, is_delete)');

ALTER TABLE basket_captain_qualification
    COMMENT = '队长授权表';

-- ---------------------------------------------------------------------------
-- 2. 赛事赛季开放平台（报名 platformCode 校验依赖）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_tournament_season_platform (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    season_id BIGINT NOT NULL COMMENT '赛事赛季主键',
    platform_code VARCHAR(16) NOT NULL COMMENT '平台代码 PC/PS/XBOX',
    platform_status TINYINT NOT NULL DEFAULT 1 COMMENT '开放状态 1开放 0关闭',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_season_platform (season_id, platform_code, is_delete),
    KEY idx_season_platform_status (season_id, platform_status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事赛季开放平台表';

INSERT INTO basket_tournament_season_platform (season_id, platform_code, platform_status, is_delete)
SELECT s.id,
       CASE
           WHEN UPPER(TRIM(t.game_terminal)) IN ('PS5', 'PSN') THEN 'PS'
           WHEN UPPER(TRIM(t.game_terminal)) IN ('PC', 'PS', 'XBOX') THEN UPPER(TRIM(t.game_terminal))
           ELSE 'PC'
       END,
       1,
       0
FROM basket_tournament_season s
         INNER JOIN basket_tournament t ON t.id = s.tournament_id AND t.is_delete = 0
WHERE s.is_delete = 0
  AND t.game_terminal IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_tournament_season_platform p
    WHERE p.season_id = s.id AND p.is_delete = 0
);

-- ---------------------------------------------------------------------------
-- 3. 报名记录 platform_code（与 batch1 一致，幂等补列）
-- ---------------------------------------------------------------------------
CALL cnpa_add_column_if_missing('basket_tournament_registration', 'platform_code',
    "VARCHAR(16) NULL COMMENT '报名平台 PC/PS/XBOX' AFTER team_id");

UPDATE basket_tournament_registration r
    INNER JOIN basket_team tm ON tm.id = r.team_id
SET r.platform_code = CASE
    WHEN UPPER(TRIM(tm.game_terminal)) IN ('PS5', 'PSN') THEN 'PS'
    WHEN UPPER(TRIM(tm.game_terminal)) IN ('PC', 'PS', 'XBOX') THEN UPPER(TRIM(tm.game_terminal))
    ELSE 'PC'
END
WHERE r.platform_code IS NULL
  AND r.is_delete = 0;

-- 同一球队同一赛季可按不同平台重复报名：调整唯一键（若旧键存在则删除）
SET @cnpa_drop_old_reg_uk = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'basket_tournament_registration'
              AND INDEX_NAME = 'uk_registration_season_team'
        ),
        'ALTER TABLE basket_tournament_registration DROP INDEX uk_registration_season_team',
        'SELECT 1'
    )
);
PREPARE cnpa_stmt FROM @cnpa_drop_old_reg_uk;
EXECUTE cnpa_stmt;
DEALLOCATE PREPARE cnpa_stmt;

CALL cnpa_add_index_if_missing('basket_tournament_registration', 'uk_registration_season_team_platform',
    'UNIQUE KEY uk_registration_season_team_platform (season_id, team_id, platform_code, is_delete)');

-- ---------------------------------------------------------------------------
-- 4. 球队成员：一人一队 active_member_slot（与 batch1 一致，幂等补列）
-- ---------------------------------------------------------------------------
CALL cnpa_add_column_if_missing('basket_team_member', 'active_member_slot',
    "VARCHAR(64) NULL COMMENT '活跃成员槽位 memberId' AFTER active_platform_slot");

UPDATE basket_team_member
SET active_member_slot = CAST(member_id AS CHAR)
WHERE is_delete = 0
  AND is_current = 1
  AND membership_status = 1
  AND active_member_slot IS NULL;

CALL cnpa_add_index_if_missing('basket_team_member', 'uk_team_member_active_member_slot',
    'UNIQUE KEY uk_team_member_active_member_slot (active_member_slot)');

-- ---------------------------------------------------------------------------
-- 5. 球队本体去平台化默认值（历史兼容）
-- ---------------------------------------------------------------------------
ALTER TABLE basket_team
    MODIFY COLUMN game_terminal VARCHAR(16) NOT NULL DEFAULT 'GENERAL'
        COMMENT '已废弃-历史比赛终端，球队本体不绑定平台';

UPDATE basket_team
SET game_terminal = 'GENERAL'
WHERE game_terminal IS NULL OR TRIM(game_terminal) = '';

-- ---------------------------------------------------------------------------
-- 6. 后台菜单文案（队长授权管理）
-- ---------------------------------------------------------------------------
UPDATE sys_menu
SET menu_name = '队长授权管理',
    update_time = NOW()
WHERE menu_id = 2002
  AND menu_name <> '队长授权管理';

-- ---------------------------------------------------------------------------
-- 清理工具过程
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cnpa_add_column_if_missing;
DROP PROCEDURE IF EXISTS cnpa_add_index_if_missing;

COMMIT;
