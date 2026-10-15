-- 已审核报名的名单变更。原有效阵容在审核通过前保持不变。
-- 可重复执行。不改已有报名状态。
-- 回滚：
--   DROP TABLE IF EXISTS basket_registration_roster_change_player;
--   DROP TABLE IF EXISTS basket_registration_roster_change;
--   ALTER TABLE basket_registration DROP COLUMN lineup_version;

SET @lineup_version_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'basket_registration'
      AND COLUMN_NAME = 'lineup_version'
);
SET @lineup_version_sql = IF(
    @lineup_version_exists = 0,
    'ALTER TABLE `basket_registration` ADD COLUMN `lineup_version` INT NOT NULL DEFAULT 0 COMMENT ''有效阵容版本，名单变更通过时加一'' AFTER `lineup_count`',
    'SELECT 1'
);
PREPARE lineup_version_stmt FROM @lineup_version_sql;
EXECUTE lineup_version_stmt;
DEALLOCATE PREPARE lineup_version_stmt;

CREATE TABLE IF NOT EXISTS `basket_registration_roster_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `registration_id` BIGINT NOT NULL,
  `division_id` BIGINT NOT NULL,
  `season_id` BIGINT NOT NULL,
  `team_id` BIGINT NOT NULL,
  `change_status` VARCHAR(32) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED',
  `base_lineup_version` INT NOT NULL DEFAULT 0,
  `submit_member_id` BIGINT NOT NULL,
  `audit_user_id` BIGINT DEFAULT NULL,
  `audit_remark` VARCHAR(512) DEFAULT NULL,
  `audit_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_roster_change_registration` (`registration_id`, `change_status`, `is_delete`),
  KEY `idx_roster_change_division` (`division_id`, `change_status`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名名单变更申请';

CREATE TABLE IF NOT EXISTS `basket_registration_roster_change_player` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `change_id` BIGINT NOT NULL,
  `player_id` BIGINT NOT NULL,
  `is_captain` TINYINT NOT NULL DEFAULT 0,
  `is_starter` TINYINT NOT NULL DEFAULT 1,
  `sort_order` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_roster_change_player` (`change_id`, `player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='名单变更目标阵容';
