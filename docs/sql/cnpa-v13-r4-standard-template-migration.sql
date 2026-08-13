-- =============================================================================
-- CNPA V1.3 R4: STANDARD templates League / Arena / Cup / Open
-- File: cnpa-v13-r4-standard-template-migration.sql
-- Prerequisite: cnpa-v13-r3-personal-formation-migration.sql
-- Idempotent seed. Do NOT UPDATE basket_division_format_snapshot.
-- =============================================================================

SET NAMES utf8mb4;

-- 1. Template type + default registration mode
ALTER TABLE `basket_format_template`
  ADD COLUMN `template_type` VARCHAR(16) NOT NULL DEFAULT 'CUSTOM'
    COMMENT 'STANDARD / CUSTOM' AFTER `template_description`,
  ADD COLUMN `default_registration_mode` VARCHAR(16) DEFAULT NULL
    COMMENT 'Default registration mode, not a domain hard bind' AFTER `template_type`;

-- 2. Snapshot stores expanded final registration mode + template type
ALTER TABLE `basket_division_format_snapshot`
  ADD COLUMN `source_template_type` VARCHAR(16) DEFAULT NULL
    COMMENT 'STANDARD / CUSTOM copied at clone time' AFTER `source_template_version`,
  ADD COLUMN `registration_mode` VARCHAR(16) DEFAULT NULL
    COMMENT 'Final registration mode stored on Rule Snapshot' AFTER `source_template_type`;

-- 3. Seed / draw / bye as structured columns, not JSON
ALTER TABLE `basket_format_template_stage`
  ADD COLUMN `seed_rule` VARCHAR(32) DEFAULT NULL COMMENT 'Seed rule' AFTER `next_stage_code`,
  ADD COLUMN `draw_rule` VARCHAR(32) DEFAULT NULL COMMENT 'Draw rule' AFTER `seed_rule`,
  ADD COLUMN `bye_rule` VARCHAR(32) DEFAULT NULL COMMENT 'Bye rule' AFTER `draw_rule`;

ALTER TABLE `basket_division_stage`
  ADD COLUMN `seed_rule` VARCHAR(32) DEFAULT NULL COMMENT 'Seed rule' AFTER `next_stage_code`,
  ADD COLUMN `draw_rule` VARCHAR(32) DEFAULT NULL COMMENT 'Draw rule' AFTER `seed_rule`,
  ADD COLUMN `bye_rule` VARCHAR(32) DEFAULT NULL COMMENT 'Bye rule' AFTER `draw_rule`;

-- 4. Backfill template_type without touching snapshots
UPDATE `basket_format_template`
SET `template_type` = 'CUSTOM'
WHERE `template_type` IS NULL OR `template_type` = '';

UPDATE `basket_format_template`
SET `template_type` = 'STANDARD',
    `default_registration_mode` = CASE `template_code`
      WHEN 'OPEN_STANDARD_V1' THEN 'PERSONAL'
      ELSE 'TEAM'
    END
WHERE `template_code` IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND `is_delete` = 0;

-- 5. Ensure four STANDARD templates exist (insert missing only)
INSERT INTO basket_format_template (
  series_id, template_code, template_name, version_no, template_description,
  template_type, default_registration_mode, enabled, is_default, create_time, update_time, is_delete
)
SELECT s.id, 'LEAGUE_STANDARD_V1', 'League STANDARD V1', 1,
  'ROUND_ROBIN then SINGLE_ELIMINATION playoff',
  'STANDARD', 'TEAM', 1, 1, NOW(), NOW(), 0
FROM basket_series s
WHERE s.series_code = 'LEAGUE' AND s.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template t
    WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  );

INSERT INTO basket_format_template (
  series_id, template_code, template_name, version_no, template_description,
  template_type, default_registration_mode, enabled, is_default, create_time, update_time, is_delete
)
SELECT s.id, 'ARENA_STANDARD_V1', 'Arena STANDARD V1', 1,
  'SINGLE_ELIMINATION knockout',
  'STANDARD', 'TEAM', 1, 1, NOW(), NOW(), 0
FROM basket_series s
WHERE s.series_code = 'ARENA' AND s.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template t
    WHERE t.template_code = 'ARENA_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  );

INSERT INTO basket_format_template (
  series_id, template_code, template_name, version_no, template_description,
  template_type, default_registration_mode, enabled, is_default, create_time, update_time, is_delete
)
SELECT s.id, 'CUP_STANDARD_V1', 'Cup STANDARD V1', 1,
  'GROUP_STAGE then SINGLE_ELIMINATION',
  'STANDARD', 'TEAM', 1, 1, NOW(), NOW(), 0
FROM basket_series s
WHERE s.series_code = 'CUP' AND s.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template t
    WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  );

INSERT INTO basket_format_template (
  series_id, template_code, template_name, version_no, template_description,
  template_type, default_registration_mode, enabled, is_default, create_time, update_time, is_delete
)
SELECT s.id, 'OPEN_STANDARD_V1', 'Open STANDARD V1', 1,
  'SINGLE_ELIMINATION; PERSONAL formation is roster preprocessing',
  'STANDARD', 'PERSONAL', 1, 1, NOW(), NOW(), 0
FROM basket_series s
WHERE s.series_code = 'OPEN' AND s.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template t
    WHERE t.template_code = 'OPEN_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  );

-- Compatible update for existing LEAGUE_STANDARD_V1 (do not rewrite historical snapshots)
UPDATE basket_format_template
SET template_name = 'League STANDARD V1',
    template_description = 'ROUND_ROBIN then SINGLE_ELIMINATION playoff',
    template_type = 'STANDARD',
    default_registration_mode = 'TEAM',
    enabled = 1,
    is_default = 1,
    update_time = NOW()
WHERE template_code = 'LEAGUE_STANDARD_V1' AND version_no = 1 AND is_delete = 0;

UPDATE basket_format_template t
JOIN basket_series s ON s.id = t.series_id AND s.is_delete = 0
SET t.is_default = 0, t.update_time = NOW()
WHERE s.series_code = 'LEAGUE'
  AND t.template_code <> 'LEAGUE_STANDARD_V1'
  AND t.is_delete = 0
  AND t.is_default = 1;

-- Soft-delete obsolete League FINAL competition stage from template definition only
UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.is_delete = 1, s.update_time = NOW()
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'FINAL' AND s.is_delete = 0;

-- League REGULAR
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, organization_mode, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'Regular Season', 1, 'REGULAR', 'UNGROUPED', 'ROUND_ROBIN',
  'NONE', NULL, 1, 1,
  2, 1, 2, 0,
  'OVERALL_TOP_N', 8, 'NONE', 'PLAYOFF',
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'REGULAR' AND s.is_delete = 0
  );

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.stage_name = 'Regular Season',
    s.stage_order = 1,
    s.stage_type = 'REGULAR',
    s.organization_mode = 'UNGROUPED',
    s.competition_format = 'ROUND_ROBIN',
    s.grouping_mode = 'NONE',
    s.group_count = NULL,
    s.round_robin_cycles = 1,
    s.best_of = 1,
    s.win_points = 2,
    s.loss_points = 1,
    s.forfeit_win_points = 2,
    s.forfeit_loss_points = 0,
    s.advancement_type = 'OVERALL_TOP_N',
    s.advancement_count = 8,
    s.pairing_rule = 'NONE',
    s.next_stage_code = 'PLAYOFF',
    s.update_time = NOW()
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'REGULAR' AND s.is_delete = 0;

-- League PLAYOFF
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, organization_mode, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'PLAYOFF', 'Playoff', 2, 'KNOCKOUT', 'UNGROUPED', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 3,
  NULL, NULL, NULL, NULL,
  'NONE', NULL, 'HIGH_VS_LOW', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'PLAYOFF' AND s.is_delete = 0
  );

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.stage_name = 'Playoff',
    s.stage_order = 2,
    s.stage_type = 'KNOCKOUT',
    s.organization_mode = 'UNGROUPED',
    s.competition_format = 'SINGLE_ELIMINATION',
    s.grouping_mode = 'NONE',
    s.group_count = NULL,
    s.round_robin_cycles = NULL,
    s.best_of = 3,
    s.win_points = NULL,
    s.loss_points = NULL,
    s.forfeit_win_points = NULL,
    s.forfeit_loss_points = NULL,
    s.advancement_type = 'NONE',
    s.advancement_count = NULL,
    s.pairing_rule = 'HIGH_VS_LOW',
    s.next_stage_code = NULL,
    s.update_time = NOW()
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'PLAYOFF' AND s.is_delete = 0;

-- Arena KNOCKOUT
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, organization_mode, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  seed_rule, draw_rule, bye_rule,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'KNOCKOUT', 'Knockout', 1, 'KNOCKOUT', 'UNGROUPED', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 1,
  NULL, NULL, NULL, NULL,
  'NONE', NULL, 'RANDOM_DRAW', NULL,
  'ADMIN_SEED', 'RANDOM_CONFIRM', 'SEED_PRIORITY',
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ARENA_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  );

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.stage_name = 'Knockout',
    s.stage_order = 1,
    s.stage_type = 'KNOCKOUT',
    s.organization_mode = 'UNGROUPED',
    s.competition_format = 'SINGLE_ELIMINATION',
    s.grouping_mode = 'NONE',
    s.best_of = 1,
    s.advancement_type = 'NONE',
    s.pairing_rule = 'RANDOM_DRAW',
    s.next_stage_code = NULL,
    s.seed_rule = 'ADMIN_SEED',
    s.draw_rule = 'RANDOM_CONFIRM',
    s.bye_rule = 'SEED_PRIORITY',
    s.update_time = NOW()
WHERE t.template_code = 'ARENA_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0;

-- Cup GROUP
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, organization_mode, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'GROUP', 'Group Stage', 1, 'GROUP', 'GROUPED', 'ROUND_ROBIN',
  'POT_RANDOM', 4, 1, 1,
  2, 1, 2, 0,
  'GROUP_TOP_N', 2, 'NONE', 'KNOCKOUT',
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'GROUP' AND s.is_delete = 0
  );

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.stage_name = 'Group Stage',
    s.stage_order = 1,
    s.stage_type = 'GROUP',
    s.organization_mode = 'GROUPED',
    s.competition_format = 'ROUND_ROBIN',
    s.grouping_mode = 'POT_RANDOM',
    s.group_count = 4,
    s.round_robin_cycles = 1,
    s.best_of = 1,
    s.win_points = 2,
    s.loss_points = 1,
    s.forfeit_win_points = 2,
    s.forfeit_loss_points = 0,
    s.advancement_type = 'GROUP_TOP_N',
    s.advancement_count = 2,
    s.pairing_rule = 'NONE',
    s.next_stage_code = 'KNOCKOUT',
    s.update_time = NOW()
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'GROUP' AND s.is_delete = 0;

-- Cup KNOCKOUT
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, organization_mode, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'KNOCKOUT', 'Knockout', 2, 'KNOCKOUT', 'UNGROUPED', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 1,
  NULL, NULL, NULL, NULL,
  'NONE', NULL, 'GROUP_CROSS', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  );

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.stage_name = 'Knockout',
    s.stage_order = 2,
    s.stage_type = 'KNOCKOUT',
    s.organization_mode = 'UNGROUPED',
    s.competition_format = 'SINGLE_ELIMINATION',
    s.grouping_mode = 'NONE',
    s.best_of = 1,
    s.advancement_type = 'NONE',
    s.pairing_rule = 'GROUP_CROSS',
    s.next_stage_code = NULL,
    s.update_time = NOW()
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0;

-- Open KNOCKOUT
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, organization_mode, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  seed_rule, draw_rule, bye_rule,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'KNOCKOUT', 'Knockout', 1, 'KNOCKOUT', 'UNGROUPED', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 1,
  NULL, NULL, NULL, NULL,
  'NONE', NULL, 'RANDOM_DRAW', NULL,
  'NONE', 'RANDOM_CONFIRM', 'RANDOM',
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'OPEN_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  );

UPDATE basket_format_template_stage s
JOIN basket_format_template t ON t.id = s.template_id
SET s.stage_name = 'Knockout',
    s.stage_order = 1,
    s.stage_type = 'KNOCKOUT',
    s.organization_mode = 'UNGROUPED',
    s.competition_format = 'SINGLE_ELIMINATION',
    s.grouping_mode = 'NONE',
    s.best_of = 1,
    s.advancement_type = 'NONE',
    s.pairing_rule = 'RANDOM_DRAW',
    s.next_stage_code = NULL,
    s.seed_rule = 'NONE',
    s.draw_rule = 'RANDOM_CONFIRM',
    s.bye_rule = 'RANDOM',
    s.update_time = NOW()
WHERE t.template_code = 'OPEN_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0;

-- Tie-break for RR stages (insert missing)
INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, x.stage_code, x.rule_code, x.priority_no, 1, NOW(), NOW(), 0
FROM basket_format_template t
JOIN (
  SELECT 'LEAGUE_STANDARD_V1' AS template_code, 'REGULAR' AS stage_code, 'WIN_COUNT' AS rule_code, 1 AS priority_no
  UNION ALL SELECT 'LEAGUE_STANDARD_V1', 'REGULAR', 'HEAD_TO_HEAD', 2
  UNION ALL SELECT 'LEAGUE_STANDARD_V1', 'REGULAR', 'POINT_DIFFERENCE', 3
  UNION ALL SELECT 'LEAGUE_STANDARD_V1', 'REGULAR', 'TOTAL_POINTS', 4
  UNION ALL SELECT 'LEAGUE_STANDARD_V1', 'REGULAR', 'MANUAL', 5
  UNION ALL SELECT 'CUP_STANDARD_V1', 'GROUP', 'WIN_COUNT', 1
  UNION ALL SELECT 'CUP_STANDARD_V1', 'GROUP', 'HEAD_TO_HEAD', 2
  UNION ALL SELECT 'CUP_STANDARD_V1', 'GROUP', 'POINT_DIFFERENCE', 3
  UNION ALL SELECT 'CUP_STANDARD_V1', 'GROUP', 'TOTAL_POINTS', 4
  UNION ALL SELECT 'CUP_STANDARD_V1', 'GROUP', 'MANUAL', 5
) x ON x.template_code = t.template_code
WHERE t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = x.stage_code AND r.rule_code = x.rule_code AND r.is_delete = 0
  );

-- CUSTOM templates remain CUSTOM / enabled
UPDATE basket_format_template
SET template_type = 'CUSTOM', update_time = NOW()
WHERE template_code IN ('ROUND_ROBIN_BASIC_V1','GROUP_STAGE_BASIC_V1','SINGLE_ELIM_BO1_V1')
  AND is_delete = 0
  AND (template_type IS NULL OR template_type = '' OR template_type = 'CUSTOM');

UPDATE basket_format_template
SET template_type = 'STANDARD', default_registration_mode = 'TEAM', enabled = 1, is_default = 1, update_time = NOW()
WHERE template_code = 'ARENA_STANDARD_V1' AND version_no = 1 AND is_delete = 0;

UPDATE basket_format_template
SET template_type = 'STANDARD', default_registration_mode = 'TEAM', enabled = 1, is_default = 1, update_time = NOW()
WHERE template_code = 'CUP_STANDARD_V1' AND version_no = 1 AND is_delete = 0;

UPDATE basket_format_template
SET template_type = 'STANDARD', default_registration_mode = 'PERSONAL', enabled = 1, is_default = 1, update_time = NOW()
WHERE template_code = 'OPEN_STANDARD_V1' AND version_no = 1 AND is_delete = 0;

UPDATE basket_format_template t
JOIN basket_series s ON s.id = t.series_id AND s.is_delete = 0
SET t.is_default = 0, t.update_time = NOW()
WHERE s.series_code IN ('ARENA','CUP','OPEN')
  AND t.template_code NOT IN ('ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND t.is_delete = 0
  AND t.is_default = 1;
