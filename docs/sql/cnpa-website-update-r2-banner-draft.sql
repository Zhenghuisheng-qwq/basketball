-- CNPA 官网更新 R2：首页轮播草稿列。
-- 可重复执行。不改已有横幅的图片、状态、排序和已发布内容。
-- has_draft 默认 0，已上线的横幅不会变成未发布修改。
-- 部署 basketball-admin 前先执行。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS cnpa_banner_r2_add_column;
DELIMITER $$
CREATE PROCEDURE cnpa_banner_r2_add_column(
  IN p_column VARCHAR(64),
  IN p_ddl TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'basket_home_banner'
      AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = p_ddl;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL cnpa_banner_r2_add_column('has_draft',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `has_draft` TINYINT NOT NULL DEFAULT 0 COMMENT ''1 表示有尚未发布的修改''');
CALL cnpa_banner_r2_add_column('draft_enabled',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_enabled` TINYINT NULL COMMENT ''发布后是否在首页显示，1 显示 0 停用''');
CALL cnpa_banner_r2_add_column('draft_remove',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_remove` TINYINT NOT NULL DEFAULT 0 COMMENT ''1 表示发布后删除''');
CALL cnpa_banner_r2_add_column('draft_image_file_id',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_image_file_id` BIGINT NULL COMMENT ''草稿电脑端图片''');
CALL cnpa_banner_r2_add_column('draft_mobile_image_file_id',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_mobile_image_file_id` BIGINT NULL COMMENT ''草稿手机端图片''');
CALL cnpa_banner_r2_add_column('draft_alt_text',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_alt_text` VARCHAR(256) NULL COMMENT ''草稿替代文字''');
CALL cnpa_banner_r2_add_column('draft_title',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_title` VARCHAR(256) NULL COMMENT ''草稿标题''');
CALL cnpa_banner_r2_add_column('draft_title_en',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_title_en` VARCHAR(256) NULL COMMENT ''草稿英文标题''');
CALL cnpa_banner_r2_add_column('draft_subtitle',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_subtitle` VARCHAR(500) NULL COMMENT ''草稿副标题''');
CALL cnpa_banner_r2_add_column('draft_subtitle_en',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_subtitle_en` VARCHAR(500) NULL COMMENT ''草稿英文副标题''');
CALL cnpa_banner_r2_add_column('draft_target_type',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_target_type` VARCHAR(32) NULL COMMENT ''草稿跳转类型''');
CALL cnpa_banner_r2_add_column('draft_target_value',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_target_value` VARCHAR(500) NULL COMMENT ''草稿跳转值''');
CALL cnpa_banner_r2_add_column('draft_sort_no',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_sort_no` INT NULL COMMENT ''草稿排序''');
CALL cnpa_banner_r2_add_column('draft_start_time',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_start_time` DATETIME NULL COMMENT ''草稿生效开始''');
CALL cnpa_banner_r2_add_column('draft_end_time',
  'ALTER TABLE `basket_home_banner` ADD COLUMN `draft_end_time` DATETIME NULL COMMENT ''草稿生效结束''');

DROP PROCEDURE IF EXISTS cnpa_banner_r2_add_column;
