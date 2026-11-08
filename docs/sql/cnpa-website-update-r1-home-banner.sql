-- CNPA 官网更新 R1：首页轮播补手机图和替代文字。
-- 可重复执行。不改已有横幅的图片、状态和排序。
-- 部署 basketball-web / basketball-admin 前先执行，否则横幅查询会因缺列失败。

SET NAMES utf8mb4;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'basket_home_banner'
        AND COLUMN_NAME = 'mobile_image_file_id'
    ),
    'SELECT 1',
    'ALTER TABLE `basket_home_banner` ADD COLUMN `mobile_image_file_id` BIGINT NULL COMMENT ''手机端图片文件主键，空则前台使用电脑端图片'' AFTER `image_file_id`'
  )
);
PREPARE cnpa_banner_r1_stmt FROM @ddl;
EXECUTE cnpa_banner_r1_stmt;
DEALLOCATE PREPARE cnpa_banner_r1_stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'basket_home_banner'
        AND COLUMN_NAME = 'alt_text'
    ),
    'SELECT 1',
    'ALTER TABLE `basket_home_banner` ADD COLUMN `alt_text` VARCHAR(256) NULL COMMENT ''图片替代文字'' AFTER `mobile_image_file_id`'
  )
);
PREPARE cnpa_banner_r1_stmt FROM @ddl;
EXECUTE cnpa_banner_r1_stmt;
DEALLOCATE PREPARE cnpa_banner_r1_stmt;
