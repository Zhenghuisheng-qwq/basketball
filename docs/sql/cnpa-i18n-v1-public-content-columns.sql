-- CNPA i18n V1: optional English display columns for public content.
-- Idempotent. Safe to run more than once.
-- Original Chinese columns stay the source of record.
-- Empty English values fall back to Chinese in the application.
-- Does not copy seasons, divisions, matches, registrations, or standings.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS cnpa_i18n_add_column;
DELIMITER $$
CREATE PROCEDURE cnpa_i18n_add_column(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_ddl TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = p_ddl;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL cnpa_i18n_add_column(
  'basket_season',
  'display_title_en',
  'ALTER TABLE `basket_season` ADD COLUMN `display_title_en` VARCHAR(128) NULL COMMENT ''英文展示标题'' AFTER `display_title`'
);

CALL cnpa_i18n_add_column(
  'basket_season',
  'description_en',
  'ALTER TABLE `basket_season` ADD COLUMN `description_en` TEXT NULL COMMENT ''英文赛季说明'' AFTER `description`'
);

CALL cnpa_i18n_add_column(
  'basket_division',
  'division_name_en',
  'ALTER TABLE `basket_division` ADD COLUMN `division_name_en` VARCHAR(128) NULL COMMENT ''英文赛区名称'' AFTER `division_name`'
);

CALL cnpa_i18n_add_column(
  'basket_announcement',
  'title_en',
  'ALTER TABLE `basket_announcement` ADD COLUMN `title_en` VARCHAR(256) NULL COMMENT ''英文公告标题'' AFTER `title`'
);

CALL cnpa_i18n_add_column(
  'basket_announcement',
  'summary_en',
  'ALTER TABLE `basket_announcement` ADD COLUMN `summary_en` VARCHAR(1000) NULL COMMENT ''英文公告摘要'' AFTER `summary`'
);

CALL cnpa_i18n_add_column(
  'basket_announcement',
  'content_text_en',
  'ALTER TABLE `basket_announcement` ADD COLUMN `content_text_en` LONGTEXT NULL COMMENT ''英文公告正文'' AFTER `content_text`'
);

DROP PROCEDURE IF EXISTS cnpa_i18n_add_column;
