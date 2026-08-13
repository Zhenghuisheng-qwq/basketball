-- =============================================================================
-- CNPA V1.3 R7: MatchSeries scheduling + Stage Publish Record
-- File: cnpa-v13-r7-schedule-publish-migration.sql
-- Prerequisite: cnpa-v13-r7-precheck-schema-reconcile.sql (or complete R1~R6)
-- 1 new table. Schedule fields hang on existing MatchSeries / Match / ScheduleBatch.
-- =============================================================================

SET NAMES utf8mb4;

-- 1. Structure execution header: schedule revision is independent of execution_version
ALTER TABLE `basket_schedule_batch`
  ADD COLUMN `schedule_revision` INT NOT NULL DEFAULT 0
    COMMENT 'Date/time assignment revision; NOT execution_version'
    AFTER `execution_version`;

-- 2. MatchSeries is the BO>1 schedule unit
ALTER TABLE `basket_match_series`
  ADD COLUMN `scheduled_date` DATE DEFAULT NULL COMMENT 'Series match date' AFTER `best_of`,
  ADD COLUMN `scheduled_start_time` TIME DEFAULT NULL COMMENT 'Series start time' AFTER `scheduled_date`,
  ADD COLUMN `schedule_source` VARCHAR(16) DEFAULT NULL COMMENT 'MANUAL / RECOMMENDED' AFTER `scheduled_start_time`,
  ADD COLUMN `schedule_status` VARCHAR(16) NOT NULL DEFAULT 'UNSCHEDULED' COMMENT 'UNSCHEDULED / SCHEDULED' AFTER `schedule_source`;

CREATE INDEX `idx_match_series_stage_date` ON `basket_match_series` (`stage_id`, `scheduled_date`);
CREATE INDEX `idx_match_series_stage_slot` ON `basket_match_series` (`stage_id`, `scheduled_date`, `scheduled_start_time`);

-- 3. BO1 Match schedule source (date/time remain match_time)
ALTER TABLE `basket_match`
  ADD COLUMN `schedule_source` VARCHAR(16) DEFAULT NULL COMMENT 'MANUAL / RECOMMENDED' AFTER `match_time`,
  ADD COLUMN `schedule_status` VARCHAR(16) DEFAULT NULL COMMENT 'UNSCHEDULED / SCHEDULED; NULL=legacy' AFTER `schedule_source`;

CREATE INDEX `idx_match_stage_time` ON `basket_match` (`stage_id`, `match_time`);

-- Backfill BO1 schedule_status from existing match_time (child games stay NULL)
UPDATE `basket_match`
SET `schedule_status` = CASE WHEN `match_time` IS NULL THEN 'UNSCHEDULED' ELSE 'SCHEDULED' END,
    `schedule_source` = CASE WHEN `match_time` IS NULL THEN NULL ELSE 'MANUAL' END
WHERE `is_delete` = 0
  AND `series_id` IS NULL
  AND (`schedule_status` IS NULL OR `schedule_status` = '');

-- 4. Stage publish audit header (no item table; fingerprint covers schedule identity)
CREATE TABLE IF NOT EXISTS `basket_stage_publish_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `division_id` BIGINT NOT NULL,
  `stage_id` BIGINT NOT NULL,
  `structure_batch_id` BIGINT NOT NULL COMMENT 'ScheduleBatch id = structure execution',
  `structure_execution_version` INT NOT NULL,
  `rule_version_id` BIGINT DEFAULT NULL,
  `roster_version_id` BIGINT DEFAULT NULL,
  `pairing_execution_id` BIGINT DEFAULT NULL,
  `schedule_revision` INT NOT NULL DEFAULT 0,
  `publish_no` INT NOT NULL COMMENT 'Stage-scoped publish version',
  `action_type` VARCHAR(16) NOT NULL COMMENT 'PUBLISH / WITHDRAW / REPUBLISH',
  `publish_status` VARCHAR(16) NOT NULL COMMENT 'Resulting stage publish status',
  `schedule_fingerprint` VARCHAR(64) DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `operation_time` DATETIME NOT NULL,
  `reason` VARCHAR(500) DEFAULT NULL COMMENT 'Required for WITHDRAW',
  `is_current` TINYINT NOT NULL DEFAULT 0,
  `current_unique_key` VARCHAR(64) DEFAULT NULL COMMENT 'STAGE:{id} when current published',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_publish_no` (`stage_id`, `publish_no`, `action_type`),
  UNIQUE KEY `uk_stage_publish_current` (`current_unique_key`),
  KEY `idx_stage_publish_division_stage` (`division_id`, `stage_id`, `publish_no`),
  KEY `idx_stage_publish_batch` (`structure_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stage schedule publish / withdraw / republish record';
