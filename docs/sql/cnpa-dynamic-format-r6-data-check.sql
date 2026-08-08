-- =============================================================================
-- CNPA 动态赛制 R6：organization_mode 数据验收（只读）
-- 文件：cnpa-dynamic-format-r6-data-check.sql
-- 说明：异常项仅输出数量与样例 ID，不自动修改业务数据
-- =============================================================================

SET NAMES utf8mb4;

-- 1. division_stage organization_mode NULL
SELECT '1_division_stage_org_mode_null' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage
WHERE is_delete = 0 AND organization_mode IS NULL;

SELECT id, division_id, snapshot_id, stage_code, grouping_mode, organization_mode
FROM basket_division_stage
WHERE is_delete = 0 AND organization_mode IS NULL
LIMIT 20;

-- 2. template_stage organization_mode NULL
SELECT '2_template_stage_org_mode_null' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_format_template_stage
WHERE is_delete = 0 AND organization_mode IS NULL;

SELECT id, template_id, stage_code, grouping_mode, organization_mode
FROM basket_format_template_stage
WHERE is_delete = 0 AND organization_mode IS NULL
LIMIT 20;

-- 3. UNGROUPED 但 grouping_mode != NONE
SELECT '3_ungrouped_but_grouping_mode_not_none' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode = 'UNGROUPED'
  AND grouping_mode IS NOT NULL
  AND TRIM(grouping_mode) <> ''
  AND UPPER(TRIM(grouping_mode)) <> 'NONE';

SELECT id, division_id, stage_code, grouping_mode, organization_mode, group_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode = 'UNGROUPED'
  AND grouping_mode IS NOT NULL
  AND TRIM(grouping_mode) <> ''
  AND UPPER(TRIM(grouping_mode)) <> 'NONE'
LIMIT 20;

-- 4. GROUPED 但 grouping_mode = NONE
SELECT '4_grouped_but_grouping_mode_none' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode = 'GROUPED'
  AND (grouping_mode IS NULL OR TRIM(grouping_mode) = '' OR UPPER(TRIM(grouping_mode)) = 'NONE');

SELECT id, division_id, stage_code, grouping_mode, organization_mode, group_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode = 'GROUPED'
  AND (grouping_mode IS NULL OR TRIM(grouping_mode) = '' OR UPPER(TRIM(grouping_mode)) = 'NONE')
LIMIT 20;

-- 5. GROUPED 但 group_count <= 0
SELECT '5_grouped_but_group_count_invalid' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode = 'GROUPED'
  AND (group_count IS NULL OR group_count <= 0);

SELECT id, division_id, stage_code, organization_mode, grouping_mode, group_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode = 'GROUPED'
  AND (group_count IS NULL OR group_count <= 0)
LIMIT 20;

-- 6. organization_mode 非法枚举
SELECT '6_invalid_organization_mode' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode NOT IN ('GROUPED', 'UNGROUPED');

SELECT id, division_id, stage_code, organization_mode
FROM basket_division_stage
WHERE is_delete = 0
  AND organization_mode NOT IN ('GROUPED', 'UNGROUPED')
LIMIT 20;

-- 7. stage_type 分布
SELECT '7_stage_type_distribution' AS check_item, stage_type, COUNT(*) AS cnt
FROM basket_division_stage
WHERE is_delete = 0
GROUP BY stage_type
ORDER BY cnt DESC;

-- 8. competition_format 分布
SELECT '8_competition_format_distribution' AS check_item, competition_format, COUNT(*) AS cnt
FROM basket_division_stage
WHERE is_delete = 0
GROUP BY competition_format
ORDER BY cnt DESC;

-- 9. best_of 分布
SELECT '9_best_of_distribution' AS check_item, best_of, COUNT(*) AS cnt
FROM basket_division_stage
WHERE is_delete = 0
GROUP BY best_of
ORDER BY best_of;

-- 10. SERIES_WINNER 历史 advancement_type 数量
SELECT '10_series_winner_advancement_count' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage
WHERE is_delete = 0
  AND advancement_type = 'SERIES_WINNER';

SELECT id, division_id, stage_code, advancement_type
FROM basket_division_stage
WHERE is_delete = 0 AND advancement_type = 'SERIES_WINNER'
LIMIT 20;

-- 11. Snapshot Stage 孤儿（无有效 snapshot）
SELECT '11_orphan_division_stage_no_snapshot' AS check_item,
       COUNT(*) AS anomaly_count
FROM basket_division_stage ds
LEFT JOIN basket_division_format_snapshot snap
  ON snap.id = ds.snapshot_id AND snap.is_delete = 0
WHERE ds.is_delete = 0 AND snap.id IS NULL;

-- 12. Template → Snapshot organization_mode 差异统计（INFO，非异常；Snapshot 独立演化后允许不一致）
SELECT '12_template_snapshot_org_mode_diff_info' AS check_item,
       COUNT(*) AS diff_count
FROM basket_division_stage ds
INNER JOIN basket_division_format_snapshot snap
  ON snap.id = ds.snapshot_id AND snap.is_delete = 0
INNER JOIN basket_format_template_stage ts
  ON ts.template_id = snap.source_template_id
 AND ts.stage_code = ds.stage_code
 AND ts.is_delete = 0
WHERE ds.is_delete = 0
  AND ds.organization_mode <> ts.organization_mode;

SELECT ds.id, ds.stage_code, ds.organization_mode AS snapshot_org, ts.organization_mode AS template_org
FROM basket_division_stage ds
INNER JOIN basket_division_format_snapshot snap
  ON snap.id = ds.snapshot_id AND snap.is_delete = 0
INNER JOIN basket_format_template_stage ts
  ON ts.template_id = snap.source_template_id
 AND ts.stage_code = ds.stage_code
 AND ts.is_delete = 0
WHERE ds.is_delete = 0
  AND ds.organization_mode <> ts.organization_mode
LIMIT 20;
