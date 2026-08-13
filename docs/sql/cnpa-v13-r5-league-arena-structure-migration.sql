-- =============================================================================
-- CNPA V1.3 R5: League + Arena Structure Engine
-- File: cnpa-v13-r5-league-arena-structure-migration.sql
-- Prerequisite: cnpa-v13-r4-standard-template-migration.sql
-- Do NOT UPDATE basket_division_format_snapshot (LOCKED history).
-- STANDARD_V1 template defaults may be corrected once, then frozen.
-- =============================================================================

SET NAMES utf8mb4;

-- 1. Stage BO mapping: best_of = Regular / Knockout Early; SF / Final separate
ALTER TABLE `basket_format_template_stage`
  ADD COLUMN `semifinal_best_of` INT DEFAULT NULL COMMENT 'Semifinal BO' AFTER `best_of`,
  ADD COLUMN `final_best_of` INT DEFAULT NULL COMMENT 'Final BO' AFTER `semifinal_best_of`;

ALTER TABLE `basket_division_stage`
  ADD COLUMN `semifinal_best_of` INT DEFAULT NULL COMMENT 'Semifinal BO' AFTER `best_of`,
  ADD COLUMN `final_best_of` INT DEFAULT NULL COMMENT 'Final BO' AFTER `semifinal_best_of`;

-- 2. Structure header binds pairing/draw execution
ALTER TABLE `basket_schedule_batch`
  ADD COLUMN `pairing_execution_id` BIGINT DEFAULT NULL COMMENT 'Confirmed pairing/draw execution' AFTER `roster_version_id`;

CREATE INDEX `idx_schedule_batch_pairing_exec` ON `basket_schedule_batch` (`pairing_execution_id`);

-- 3. Seed / Draw / Slot execution (not personal formation draw)
CREATE TABLE IF NOT EXISTS `basket_stage_pairing_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `division_id` BIGINT NOT NULL,
  `stage_id` BIGINT NOT NULL,
  `rule_version_id` BIGINT DEFAULT NULL,
  `roster_version_id` BIGINT DEFAULT NULL,
  `execution_no` INT NOT NULL,
  `seed_rule` VARCHAR(32) DEFAULT NULL,
  `draw_rule` VARCHAR(32) DEFAULT NULL,
  `bye_rule` VARCHAR(32) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / CONFIRMED / SUPERSEDED',
  `random_seed` BIGINT DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `generated_time` DATETIME DEFAULT NULL,
  `confirmed_time` DATETIME DEFAULT NULL,
  `confirmed_by` BIGINT DEFAULT NULL,
  `is_current` TINYINT NOT NULL DEFAULT 0,
  `current_unique_key` VARCHAR(64) DEFAULT NULL COMMENT 'STAGE:{id} when current',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_pairing_exec_no` (`stage_id`, `execution_no`),
  UNIQUE KEY `uk_stage_pairing_current` (`current_unique_key`),
  KEY `idx_stage_pairing_division` (`division_id`),
  KEY `idx_stage_pairing_stage_current` (`stage_id`, `is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stage seed/draw/slot execution header';

CREATE TABLE IF NOT EXISTS `basket_stage_pairing_entry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `execution_id` BIGINT NOT NULL,
  `slot_no` INT NOT NULL,
  `event_team_id` BIGINT DEFAULT NULL,
  `seed_no` INT DEFAULT NULL,
  `ranking_no` INT DEFAULT NULL,
  `is_bye` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pairing_entry_slot` (`execution_id`, `slot_no`),
  UNIQUE KEY `uk_pairing_entry_team` (`execution_id`, `event_team_id`),
  UNIQUE KEY `uk_pairing_entry_seed` (`execution_id`, `seed_no`),
  KEY `idx_pairing_entry_exec` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stage pairing execution slots';

-- MySQL unique allows multiple NULLs: BYE rows have null event_team_id / seed_no.

-- 4. STANDARD_V1 BO default correction (templates only, not snapshots)
UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.best_of = 1,
    s.semifinal_best_of = NULL,
    s.final_best_of = NULL,
    s.update_time = NOW()
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'REGULAR' AND s.is_delete = 0;

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.best_of = 3,
    s.semifinal_best_of = 5,
    s.final_best_of = 5,
    s.update_time = NOW()
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'PLAYOFF' AND s.is_delete = 0;

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.best_of = 3,
    s.semifinal_best_of = 5,
    s.final_best_of = 5,
    s.update_time = NOW()
WHERE t.template_code IN ('ARENA_STANDARD_V1', 'OPEN_STANDARD_V1') AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0;

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.best_of = 1,
    s.semifinal_best_of = NULL,
    s.final_best_of = NULL,
    s.update_time = NOW()
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'GROUP' AND s.is_delete = 0;

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.best_of = 3,
    s.semifinal_best_of = 5,
    s.final_best_of = 5,
    s.update_time = NOW()
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0;
