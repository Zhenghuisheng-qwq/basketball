-- =============================================================================
-- CNPA 动态赛制重构 第 1 轮：模板种子数据
-- 前置：已执行 cnpa-dynamic-format-r1-migration.sql
--
-- 初始化模板：
--   1. V4_LEGACY_ROUND_ROBIN_V1 — 兼容现有 V4 单循环
--   2. LEAGUE_STANDARD_V1 — League 默认可编辑模板
--
-- 积分默认值依据当前 V4 StatsSettlementServiceImpl：
--   WIN=2, LOSS=1, FORFEIT_WIN=2, FORFEIT_LOSS=0
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. V4_LEGACY_ROUND_ROBIN_V1（通用兼容模板）
-- ---------------------------------------------------------------------------
INSERT INTO basket_format_template (
  series_id, template_code, template_name, version_no, template_description,
  enabled, is_default, create_time, update_time, is_delete
)
SELECT NULL, 'V4_LEGACY_ROUND_ROBIN_V1', 'V4 兼容单循环模板', 1,
  '兼容现有 V4 Division 和单循环流程，不代表最终业务规则',
  1, 0, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM basket_format_template
  WHERE template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND version_no = 1 AND is_delete = 0
);

INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', '常规赛', 1, 'REGULAR', 'ROUND_ROBIN',
  'NONE', NULL, 1, 1,
  2, 1, 2, 0,
  'NONE', NULL, 'NONE', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'REGULAR' AND s.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'WIN_COUNT', 1, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'WIN_COUNT' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'POINT_DIFFERENCE', 2, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'POINT_DIFFERENCE' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'TOTAL_POINTS', 3, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'TOTAL_POINTS' AND r.is_delete = 0
  );

-- ---------------------------------------------------------------------------
-- 2. LEAGUE_STANDARD_V1（已禁用：含 BO3/BO5，当前版本不支持）
-- ---------------------------------------------------------------------------
INSERT INTO basket_format_template (
  series_id, template_code, template_name, version_no, template_description,
  enabled, is_default, create_time, update_time, is_delete
)
SELECT s.id, 'LEAGUE_STANDARD_V1', 'League 标准赛制（已停用）', 1,
  '已停用：含 BO3/BO5 与 SERIES_WINNER，请使用 ROUND_ROBIN_BASIC_V1 等 BO1 模板',
  0, 0, NOW(), NOW(), 0
FROM basket_series s
WHERE s.series_code = 'LEAGUE' AND s.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template
    WHERE template_code = 'LEAGUE_STANDARD_V1' AND version_no = 1 AND is_delete = 0
  );

-- 阶段一：常规赛
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', '常规赛', 1, 'REGULAR', 'ROUND_ROBIN',
  'NONE', NULL, 1, 1,
  2, 1, 2, 0,
  'OVERALL_TOP_N', NULL, 'NONE', 'PLAYOFF',
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'REGULAR' AND s.is_delete = 0
  );

-- 阶段二：季后赛
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'PLAYOFF', '季后赛', 2, 'KNOCKOUT', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 3,
  NULL, NULL, NULL, NULL,
  'SERIES_WINNER', NULL, 'HIGH_VS_LOW', 'FINAL',
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'PLAYOFF' AND s.is_delete = 0
  );

-- 阶段三：总决赛
INSERT INTO basket_format_template_stage (
  template_id, stage_code, stage_name, stage_order, stage_type, competition_format,
  grouping_mode, group_count, round_robin_cycles, best_of,
  win_points, loss_points, forfeit_win_points, forfeit_loss_points,
  advancement_type, advancement_count, pairing_rule, next_stage_code,
  requires_manual_confirmation, create_time, update_time, is_delete
)
SELECT t.id, 'FINAL', '总决赛', 3, 'FINAL', 'SINGLE_ELIMINATION',
  'NONE', NULL, NULL, 5,
  NULL, NULL, NULL, NULL,
  'NONE', NULL, 'NONE', NULL,
  0, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_stage s
    WHERE s.template_id = t.id AND s.stage_code = 'FINAL' AND s.is_delete = 0
  );

-- League 常规赛同分规则
INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'WIN_COUNT', 1, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'WIN_COUNT' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'POINT_DIFFERENCE', 2, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'POINT_DIFFERENCE' AND r.is_delete = 0
  );

INSERT INTO basket_format_template_tie_break_rule (
  template_id, stage_code, rule_code, priority_no, enabled, create_time, update_time, is_delete
)
SELECT t.id, 'REGULAR', 'TOTAL_POINTS', 3, 1, NOW(), NOW(), 0
FROM basket_format_template t
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.version_no = 1 AND t.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_format_template_tie_break_rule r
    WHERE r.template_id = t.id AND r.stage_code = 'REGULAR' AND r.rule_code = 'TOTAL_POINTS' AND r.is_delete = 0
  );
