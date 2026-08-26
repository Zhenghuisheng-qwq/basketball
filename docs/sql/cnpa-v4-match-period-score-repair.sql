-- Repair missing V4 official period-score table.
-- Authoritative columns match docs/sql/cnpa-v4-r1-ddl.sql.

CREATE TABLE IF NOT EXISTS `basket_match_period_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `match_id` BIGINT NOT NULL COMMENT 'match id',
  `division_id` BIGINT NOT NULL COMMENT 'division id',
  `period_no` INT NOT NULL COMMENT 'period no',
  `home_score` INT NOT NULL DEFAULT 0 COMMENT 'home period score',
  `away_score` INT NOT NULL DEFAULT 0 COMMENT 'away period score',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT 'delete flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_period` (`match_id`, `period_no`, `is_delete`),
  KEY `idx_period_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 match period score';