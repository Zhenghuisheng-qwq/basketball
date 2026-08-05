-- =============================================================================
-- CNPA V2 最终权威 Seed（新环境初始化，不含测试业务数据）
-- =============================================================================
SET NAMES utf8mb4;

-- Series
INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'LEAGUE', 'League', 'CNPA 联赛系列', 'REGULAR_ROUND_ROBIN', 1, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code='LEAGUE' AND is_delete=0);
INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'ARENA', 'Arena', 'CNPA Arena 系列', 'REGULAR_ROUND_ROBIN', 2, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code='ARENA' AND is_delete=0);
INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'CUP', 'Cup', 'CNPA 杯赛系列', 'SINGLE_ELIMINATION', 3, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code='CUP' AND is_delete=0);
INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'OPEN', 'Open', 'CNPA 公开赛系列', 'REGULAR_ROUND_ROBIN', 4, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code='OPEN' AND is_delete=0);

-- 规则模板（不含 V4_LEGACY_ROUND_ROBIN_V1）
-- LEAGUE_STANDARD_V1 + 基础单循环/小组赛/BO1 淘汰模板见 cnpa-dynamic-format-r1-seed.sql 第 2 节及下方补充

INSERT INTO basket_format_template (series_id, template_code, template_name, version_no, template_description, enabled, is_default, create_time, update_time, is_delete)
SELECT NULL, 'ROUND_ROBIN_BASIC_V1', '基础单循环模板', 1, '单阶段单循环', 1, 0, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_format_template WHERE template_code='ROUND_ROBIN_BASIC_V1' AND is_delete=0);

INSERT INTO basket_format_template (series_id, template_code, template_name, version_no, template_description, enabled, is_default, create_time, update_time, is_delete)
SELECT NULL, 'GROUP_STAGE_BASIC_V1', '基础小组赛模板', 1, '小组循环赛', 1, 0, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_format_template WHERE template_code='GROUP_STAGE_BASIC_V1' AND is_delete=0);

INSERT INTO basket_format_template (series_id, template_code, template_name, version_no, template_description, enabled, is_default, create_time, update_time, is_delete)
SELECT NULL, 'SINGLE_ELIM_BO1_V1', '基础 BO1 淘汰模板', 1, '单败淘汰 BO1', 1, 0, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_format_template WHERE template_code='SINGLE_ELIM_BO1_V1' AND is_delete=0);

-- LEAGUE_STANDARD_V1 完整阶段/同分规则：执行 cnpa-dynamic-format-r1-seed.sql 第 2 节（已排除 V4_LEGACY 段）
-- 注意：LEAGUE_STANDARD_V1 含 BO3/BO5，已在下方禁用；请使用 ROUND_ROBIN_BASIC_V1 等 BO1 模板

-- ROUND_ROBIN_BASIC_V1 阶段
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', '单循环', 1, 'REGULAR', 'ROUND_ROBIN',
  'NONE', NULL, 1, 1,
  2, 1, 2, 0,
  'NONE', NULL, 'NONE', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ROUND_ROBIN_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'REGULAR' AND s.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'WIN_COUNT', 1, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ROUND_ROBIN_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'WIN_COUNT' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'HEAD_TO_HEAD', 2, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ROUND_ROBIN_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'HEAD_TO_HEAD' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'POINT_DIFFERENCE', 3, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ROUND_ROBIN_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'POINT_DIFFERENCE' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'TOTAL_POINTS', 4, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ROUND_ROBIN_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'TOTAL_POINTS' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'MANUAL', 5, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'ROUND_ROBIN_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'MANUAL' AND r.is_delete = 0
  );

-- GROUP_STAGE_BASIC_V1 阶段（小组循环）
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'GROUP', '小组赛', 1, 'GROUP', 'ROUND_ROBIN',
  'OFFLINE_RANDOM_RESULT', 4, 1, 1,
  2, 1, 2, 0,
  'GROUP_TOP_N', 2, 'NONE', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'GROUP_STAGE_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'GROUP' AND s.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'GROUP', 'WIN_COUNT', 1, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'GROUP_STAGE_BASIC_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'GROUP' AND r.rule_code = 'WIN_COUNT' AND r.is_delete = 0
  );

-- SINGLE_ELIM_BO1_V1 阶段
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'KNOCKOUT', '淘汰赛', 1, 'KNOCKOUT', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 1,
  NULL, NULL, NULL, NULL,
  'NONE', NULL, 'HIGH_VS_LOW', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'SINGLE_ELIM_BO1_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'KNOCKOUT' AND s.is_delete = 0
  );

-- League 系列默认模板改为 ROUND_ROBIN_BASIC_V1
UPDATE basket_format_template
SET is_default = 0, update_time = NOW()
WHERE template_code = 'LEAGUE_STANDARD_V1' AND is_delete = 0;

UPDATE basket_format_template
SET enabled = 0, is_default = 0, update_time = NOW()
WHERE template_code = 'LEAGUE_STANDARD_V1' AND is_delete = 0;

UPDATE basket_format_template
SET is_default = 1, update_time = NOW()
WHERE template_code = 'ROUND_ROBIN_BASIC_V1' AND is_delete = 0;
