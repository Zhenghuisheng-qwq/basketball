-- =============================================================================
-- CNPA V1.3 R3: PERSONAL Formation + Temporary EventTeam + Personal Roster
-- File: cnpa-v13-r3-personal-formation-migration.sql
-- Prerequisite: cnpa-v13-r2-team-roster-migration.sql
-- =============================================================================

SET NAMES utf8mb4;

-- 1. Rule Version workflow identifier
ALTER TABLE `basket_division_format_snapshot`
  ADD COLUMN `roster_workflow_mode` VARCHAR(32) NOT NULL DEFAULT 'LEGACY'
    COMMENT 'Roster workflow: LEGACY / VERSIONED' AFTER `snapshot_status`;

UPDATE `basket_division_format_snapshot`
SET `roster_workflow_mode` = 'LEGACY'
WHERE `roster_workflow_mode` IS NULL OR `roster_workflow_mode` = '';

-- 2. RosterEntry supports PERSONAL Temporary EventTeam
ALTER TABLE `basket_division_roster_entry`
  MODIFY COLUMN `registration_id` BIGINT DEFAULT NULL
    COMMENT 'Source TEAM registration ID; nullable for PERSONAL',
  MODIFY COLUMN `team_id` BIGINT DEFAULT NULL
    COMMENT 'Long-term basket_team ID; nullable for PERSONAL temporary team',
  ADD COLUMN `event_team_id` BIGINT DEFAULT NULL
    COMMENT 'EventTeam snapshot ID for PERSONAL locked temporary team' AFTER `team_id`;

ALTER TABLE `basket_division_roster_entry`
  ADD KEY `idx_roster_entry_event_team` (`event_team_id`);

-- 3. EventTeam: PERSONAL temporary teams have no long-term basket_team
ALTER TABLE `basket_event_team`
  MODIFY COLUMN `registration_id` BIGINT DEFAULT NULL
    COMMENT 'TEAM registration ID; nullable for PERSONAL temporary EventTeam',
  MODIFY COLUMN `source_team_id` BIGINT DEFAULT NULL
    COMMENT 'Long-term basket_team ID; NULL for PERSONAL temporary EventTeam',
  ADD COLUMN `source_draw_team_id` BIGINT DEFAULT NULL
    COMMENT 'Formation working team ID' AFTER `source_team_id`;

ALTER TABLE `basket_event_team`
  ADD KEY `idx_event_team_source_draw_team` (`source_draw_team_id`);

-- 4. EventPlayer uniqueness per roster version (keep V1 history)
ALTER TABLE `basket_event_player`
  ADD COLUMN `roster_version_id` BIGINT DEFAULT NULL
    COMMENT 'Owning roster version; NULL means Legacy' AFTER `division_id`;

ALTER TABLE `basket_event_player`
  ADD COLUMN `roster_scope` BIGINT GENERATED ALWAYS AS (IFNULL(`roster_version_id`, 0)) STORED
    COMMENT 'Legacy=0, otherwise roster_version_id';

ALTER TABLE `basket_event_player`
  DROP INDEX `uk_event_player_division_source_player`;

ALTER TABLE `basket_event_player`
  ADD UNIQUE KEY `uk_event_player_division_roster_source` (`division_id`, `roster_scope`, `source_player_id`, `is_delete`);

ALTER TABLE `basket_event_player`
  ADD KEY `idx_event_player_roster_version` (`roster_version_id`);

-- 5. Formation batch audit fields
ALTER TABLE `basket_personal_draw_batch`
  ADD COLUMN `random_seed` BIGINT DEFAULT NULL COMMENT 'Balanced random seed' AFTER `result_remark`,
  ADD COLUMN `roster_version_id` BIGINT DEFAULT NULL COMMENT 'Locked roster version' AFTER `random_seed`;

ALTER TABLE `basket_personal_draw_batch`
  ADD KEY `idx_personal_draw_batch_roster_version` (`roster_version_id`);
