-- =============================================================================
-- CNPA V1.3 R7 precheck schema reconcile
-- File: cnpa-v13-r7-precheck-schema-reconcile.sql
-- Do NOT modify historical R1~R6 migrations.
-- Adds missing R2/R3/R4 columns/tables that R5/R6 already assumed.
-- Does NOT UPDATE basket_division_format_snapshot LOCKED rows.
-- =============================================================================

SET NAMES utf8mb4;

-- Helper: add column if missing
DROP PROCEDURE IF EXISTS cnpa_r7_add_column;
DELIMITER $$
CREATE PROCEDURE cnpa_r7_add_column(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_ddl TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
  ) THEN
    SET @sql = p_ddl;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS cnpa_r7_add_index;
DELIMITER $$
CREATE PROCEDURE cnpa_r7_add_index(
  IN p_table VARCHAR(64),
  IN p_index VARCHAR(64),
  IN p_ddl TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
  ) THEN
    SET @sql = p_ddl;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- R2: registration allocation + roster entry
-- ---------------------------------------------------------------------------
CALL cnpa_r7_add_column('basket_registration', 'participation_role',
  'ALTER TABLE `basket_registration` ADD COLUMN `participation_role` VARCHAR(32) NOT NULL DEFAULT ''UNALLOCATED'' COMMENT ''UNALLOCATED/FORMAL/CANDIDATE/EXCLUDED'' AFTER `registration_status`');
CALL cnpa_r7_add_column('basket_registration', 'candidate_order',
  'ALTER TABLE `basket_registration` ADD COLUMN `candidate_order` INT DEFAULT NULL COMMENT ''候补顺序'' AFTER `participation_role`');
CALL cnpa_r7_add_index('basket_registration', 'idx_reg_division_role',
  'ALTER TABLE `basket_registration` ADD KEY `idx_reg_division_role` (`division_id`, `participation_role`, `is_delete`)');
CALL cnpa_r7_add_index('basket_registration', 'idx_reg_division_candidate',
  'ALTER TABLE `basket_registration` ADD KEY `idx_reg_division_candidate` (`division_id`, `candidate_order`, `is_delete`)');

CALL cnpa_r7_add_column('basket_division_roster_version', 'allocation_fingerprint',
  'ALTER TABLE `basket_division_roster_version` ADD COLUMN `allocation_fingerprint` VARCHAR(128) DEFAULT NULL COMMENT ''Lock 幂等指纹'' AFTER `excluded_count`');

CREATE TABLE IF NOT EXISTS `basket_division_roster_entry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `roster_version_id` BIGINT NOT NULL COMMENT '名单版本ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `registration_id` BIGINT DEFAULT NULL COMMENT '来源报名ID；PERSONAL 可空',
  `team_id` BIGINT DEFAULT NULL COMMENT '长期球队ID；PERSONAL 可空',
  `event_team_id` BIGINT DEFAULT NULL COMMENT 'PERSONAL 锁定临时球队',
  `participation_role` VARCHAR(32) NOT NULL COMMENT 'FORMAL/CANDIDATE/EXCLUDED',
  `candidate_order` INT DEFAULT NULL COMMENT '候补顺序，仅 CANDIDATE',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roster_entry_version_reg` (`roster_version_id`, `registration_id`),
  KEY `idx_roster_entry_division_role` (`division_id`, `participation_role`, `is_delete`),
  KEY `idx_roster_entry_version_role` (`roster_version_id`, `participation_role`, `candidate_order`),
  KEY `idx_roster_entry_event_team` (`event_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 Division名单版本条目';

-- ---------------------------------------------------------------------------
-- R3: workflow + PERSONAL formation columns
-- ---------------------------------------------------------------------------
CALL cnpa_r7_add_column('basket_division_format_snapshot', 'roster_workflow_mode',
  'ALTER TABLE `basket_division_format_snapshot` ADD COLUMN `roster_workflow_mode` VARCHAR(32) NOT NULL DEFAULT ''LEGACY'' COMMENT ''LEGACY / VERSIONED'' AFTER `snapshot_status`');

CALL cnpa_r7_add_column('basket_event_team', 'source_draw_team_id',
  'ALTER TABLE `basket_event_team` ADD COLUMN `source_draw_team_id` BIGINT DEFAULT NULL COMMENT ''Formation working team ID'' AFTER `source_team_id`');
CALL cnpa_r7_add_index('basket_event_team', 'idx_event_team_source_draw_team',
  'ALTER TABLE `basket_event_team` ADD KEY `idx_event_team_source_draw_team` (`source_draw_team_id`)');

CALL cnpa_r7_add_column('basket_event_player', 'roster_version_id',
  'ALTER TABLE `basket_event_player` ADD COLUMN `roster_version_id` BIGINT DEFAULT NULL COMMENT ''Owning roster version; NULL=Legacy'' AFTER `division_id`');
CALL cnpa_r7_add_column('basket_event_player', 'roster_scope',
  'ALTER TABLE `basket_event_player` ADD COLUMN `roster_scope` BIGINT GENERATED ALWAYS AS (IFNULL(`roster_version_id`, 0)) STORED COMMENT ''Legacy=0''');
CALL cnpa_r7_add_index('basket_event_player', 'idx_event_player_roster_version',
  'ALTER TABLE `basket_event_player` ADD KEY `idx_event_player_roster_version` (`roster_version_id`)');

-- Recreate EventPlayer uniqueness only when old UK still exists and new UK does not
SET @old_uk := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_event_player'
    AND INDEX_NAME = 'uk_event_player_division_source_player'
);
SET @new_uk := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_event_player'
    AND INDEX_NAME = 'uk_event_player_division_roster_source'
);
SET @sql := IF(@old_uk > 0 AND @new_uk = 0,
  'ALTER TABLE `basket_event_player` DROP INDEX `uk_event_player_division_source_player`, ADD UNIQUE KEY `uk_event_player_division_roster_source` (`division_id`, `roster_scope`, `source_player_id`, `is_delete`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CALL cnpa_r7_add_column('basket_personal_draw_batch', 'random_seed',
  'ALTER TABLE `basket_personal_draw_batch` ADD COLUMN `random_seed` BIGINT DEFAULT NULL COMMENT ''Balanced random seed'' AFTER `result_remark`');
CALL cnpa_r7_add_column('basket_personal_draw_batch', 'roster_version_id',
  'ALTER TABLE `basket_personal_draw_batch` ADD COLUMN `roster_version_id` BIGINT DEFAULT NULL COMMENT ''Locked roster version'' AFTER `random_seed`');
CALL cnpa_r7_add_index('basket_personal_draw_batch', 'idx_personal_draw_batch_roster_version',
  'ALTER TABLE `basket_personal_draw_batch` ADD KEY `idx_personal_draw_batch_roster_version` (`roster_version_id`)');

-- ---------------------------------------------------------------------------
-- R4: template type / snapshot registration / seed-draw-bye
-- ---------------------------------------------------------------------------
CALL cnpa_r7_add_column('basket_format_template', 'template_type',
  'ALTER TABLE `basket_format_template` ADD COLUMN `template_type` VARCHAR(16) NOT NULL DEFAULT ''CUSTOM'' COMMENT ''STANDARD / CUSTOM'' AFTER `template_description`');
CALL cnpa_r7_add_column('basket_format_template', 'default_registration_mode',
  'ALTER TABLE `basket_format_template` ADD COLUMN `default_registration_mode` VARCHAR(16) DEFAULT NULL COMMENT ''Default registration mode'' AFTER `template_type`');

CALL cnpa_r7_add_column('basket_division_format_snapshot', 'source_template_type',
  'ALTER TABLE `basket_division_format_snapshot` ADD COLUMN `source_template_type` VARCHAR(16) DEFAULT NULL COMMENT ''STANDARD / CUSTOM copied at clone time'' AFTER `source_template_version`');
CALL cnpa_r7_add_column('basket_division_format_snapshot', 'registration_mode',
  'ALTER TABLE `basket_division_format_snapshot` ADD COLUMN `registration_mode` VARCHAR(16) DEFAULT NULL COMMENT ''Final registration mode on Rule Snapshot'' AFTER `source_template_type`');

CALL cnpa_r7_add_column('basket_format_template_stage', 'seed_rule',
  'ALTER TABLE `basket_format_template_stage` ADD COLUMN `seed_rule` VARCHAR(32) DEFAULT NULL COMMENT ''Seed rule'' AFTER `next_stage_code`');
CALL cnpa_r7_add_column('basket_format_template_stage', 'draw_rule',
  'ALTER TABLE `basket_format_template_stage` ADD COLUMN `draw_rule` VARCHAR(32) DEFAULT NULL COMMENT ''Draw rule'' AFTER `seed_rule`');
CALL cnpa_r7_add_column('basket_format_template_stage', 'bye_rule',
  'ALTER TABLE `basket_format_template_stage` ADD COLUMN `bye_rule` VARCHAR(32) DEFAULT NULL COMMENT ''Bye rule'' AFTER `draw_rule`');

CALL cnpa_r7_add_column('basket_division_stage', 'seed_rule',
  'ALTER TABLE `basket_division_stage` ADD COLUMN `seed_rule` VARCHAR(32) DEFAULT NULL COMMENT ''Seed rule'' AFTER `next_stage_code`');
CALL cnpa_r7_add_column('basket_division_stage', 'draw_rule',
  'ALTER TABLE `basket_division_stage` ADD COLUMN `draw_rule` VARCHAR(32) DEFAULT NULL COMMENT ''Draw rule'' AFTER `seed_rule`');
CALL cnpa_r7_add_column('basket_division_stage', 'bye_rule',
  'ALTER TABLE `basket_division_stage` ADD COLUMN `bye_rule` VARCHAR(32) DEFAULT NULL COMMENT ''Bye rule'' AFTER `draw_rule`');

-- Template lookup only. Never UPDATE LOCKED snapshots.
UPDATE `basket_format_template`
SET `template_type` = 'CUSTOM', `update_time` = NOW()
WHERE `template_code` IN ('ROUND_ROBIN_BASIC_V1','GROUP_STAGE_BASIC_V1','SINGLE_ELIM_BO1_V1')
  AND `is_delete` = 0
  AND (`template_type` IS NULL OR `template_type` = '' OR `template_type` = 'CUSTOM');

UPDATE `basket_format_template`
SET `template_type` = 'STANDARD',
    `default_registration_mode` = CASE `template_code`
      WHEN 'OPEN_STANDARD_V1' THEN 'PERSONAL'
      ELSE 'TEAM'
    END,
    `enabled` = 1,
    `is_default` = 1,
    `update_time` = NOW()
WHERE `template_code` IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND `version_no` = 1
  AND `is_delete` = 0;

UPDATE `basket_format_template_stage` s
JOIN `basket_format_template` t ON t.id = s.template_id
SET s.seed_rule = 'ADMIN_SEED',
    s.draw_rule = 'RANDOM_CONFIRM',
    s.bye_rule = 'SEED_PRIORITY',
    s.update_time = NOW()
WHERE t.template_code = 'ARENA_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  AND (s.seed_rule IS NULL OR s.seed_rule = '');

UPDATE `basket_format_template_stage` s
JOIN `basket_format_template` t ON t.id = s.template_id
SET s.seed_rule = 'NONE',
    s.draw_rule = 'RANDOM_CONFIRM',
    s.bye_rule = 'RANDOM',
    s.update_time = NOW()
WHERE t.template_code = 'OPEN_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  AND (s.seed_rule IS NULL OR s.seed_rule = '');

-- R5 BO defaults on templates only (Arena/Open may have missed the original UPDATE)
UPDATE `basket_format_template_stage` s
JOIN `basket_format_template` t ON t.id = s.template_id
SET s.best_of = 3,
    s.semifinal_best_of = 5,
    s.final_best_of = 5,
    s.update_time = NOW()
WHERE t.template_code IN ('ARENA_STANDARD_V1', 'OPEN_STANDARD_V1') AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  AND (s.semifinal_best_of IS NULL OR s.final_best_of IS NULL OR s.best_of IS NULL OR s.best_of = 1);

DROP PROCEDURE IF EXISTS cnpa_r7_add_column;
DROP PROCEDURE IF EXISTS cnpa_r7_add_index;
