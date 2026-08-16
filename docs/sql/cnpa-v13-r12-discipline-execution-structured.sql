-- CNPA V1.3 R12: replace execution_result_json with structured columns + detail table.
-- Additive. Do not DROP the discipline table.
SET NAMES utf8mb4;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `execution_match_forfeit_count` INT NOT NULL DEFAULT 0
    COMMENT '自动比赛判负次数' AFTER `execution_error`;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `execution_series_forfeit_count` INT NOT NULL DEFAULT 0
    COMMENT '自动系列赛判负次数' AFTER `execution_match_forfeit_count`;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `execution_qualification_removed_count` INT NOT NULL DEFAULT 0
    COMMENT '未确认晋级预览移除次数' AFTER `execution_series_forfeit_count`;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `execution_warning_count` INT NOT NULL DEFAULT 0
    COMMENT '执行警告次数' AFTER `execution_qualification_removed_count`;

CREATE TABLE IF NOT EXISTS `basket_discipline_execution_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `discipline_record_id` BIGINT NOT NULL,
  `action_type` VARCHAR(64) NOT NULL COMMENT 'EVENT_TEAM_DISQUALIFIED/MATCH_AUTO_FORFEIT/SERIES_AUTO_FORFEIT/QUALIFICATION_BLOCK/DOWNSTREAM_GUARD/REQUIRES_ADMIN_ADJUDICATION',
  `target_type` VARCHAR(32) NOT NULL COMMENT 'EVENT_TEAM/MATCH/MATCH_SERIES/STAGE',
  `target_id` BIGINT NOT NULL,
  `execution_status` VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILED/SKIPPED',
  `result_message` VARCHAR(1000) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_discipline_exec_action` (`discipline_record_id`, `action_type`, `target_type`, `target_id`),
  KEY `idx_discipline_exec_record` (`discipline_record_id`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处罚赛事自动处理明细';

ALTER TABLE `basket_discipline_record`
  DROP COLUMN `execution_result_json`;
