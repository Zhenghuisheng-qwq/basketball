-- CNPA V1.3 R11: Event Disqualification auto-execution (additive, no JSON).
-- Penalty business record stays on basket_discipline_record.
-- Execution result is tracked by structured columns, not JSON.
SET NAMES utf8mb4;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `execution_status` VARCHAR(32) DEFAULT NULL
    COMMENT 'PENDING/PROCESSING/EFFECTIVE/PARTIAL/FAILED/SKIPPED/REVOKED' AFTER `record_status`;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `executed_time` DATETIME DEFAULT NULL
    COMMENT '赛事自动处理完成时间' AFTER `execution_status`;

ALTER TABLE `basket_discipline_record`
  ADD COLUMN `execution_error` VARCHAR(1000) DEFAULT NULL
    COMMENT '赛事自动处理失败原因' AFTER `executed_time`;

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

ALTER TABLE `basket_discipline_record`
  ADD KEY `idx_discipline_execution` (`penalty_code`, `execution_status`, `is_delete`);
