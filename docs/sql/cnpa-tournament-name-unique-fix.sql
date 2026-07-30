-- 赛事名称唯一约束修正：由全局唯一改为「同一顶层赛季内唯一」
-- 适用：已有库升级；可重复执行（幂等）
-- 执行：mysql -u... -p... your_db < docs/sql/cnpa-tournament-name-unique-fix.sql

START TRANSACTION;

DROP PROCEDURE IF EXISTS cnpa_drop_index_if_exists;
DROP PROCEDURE IF EXISTS cnpa_add_index_if_missing;

DELIMITER //
CREATE PROCEDURE cnpa_drop_index_if_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @cnpa_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
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

CALL cnpa_drop_index_if_exists('basket_tournament', 'uk_tournament_name_delete');
CALL cnpa_add_index_if_missing('basket_tournament', 'uk_tournament_season_name_delete',
    'UNIQUE KEY uk_tournament_season_name_delete (competition_season_id, tournament_name, is_delete)');

DROP PROCEDURE IF EXISTS cnpa_drop_index_if_exists;
DROP PROCEDURE IF EXISTS cnpa_add_index_if_missing;

COMMIT;
