-- =============================================================================
-- CNPA 动态赛制重构 第 1 轮：迁移前/后数据检查
-- 文件：cnpa-dynamic-format-r1-data-check.sql
-- 说明：只读查询，不修改数据
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 同一 season_id 下是否同时存在 PS 和 XBOX
-- ---------------------------------------------------------------------------
SELECT
  d.season_id,
  se.display_title AS season_title,
  GROUP_CONCAT(DISTINCT d.platform_code ORDER BY d.platform_code) AS platforms,
  COUNT(DISTINCT d.platform_code) AS platform_count
FROM basket_division d
JOIN basket_season se ON se.id = d.season_id AND se.is_delete = 0
WHERE d.is_delete = 0
  AND d.platform_code IN ('PS', 'XBOX')
GROUP BY d.season_id, se.display_title
HAVING SUM(d.platform_code = 'PS') > 0 AND SUM(d.platform_code = 'XBOX') > 0;

-- ---------------------------------------------------------------------------
-- 2. PS/XBOX Division 业务数据关联检查
-- ---------------------------------------------------------------------------
SELECT
  d.id AS division_id,
  d.season_id,
  d.platform_code,
  d.division_name,
  d.division_status,
  (SELECT COUNT(1) FROM basket_registration r
   WHERE r.division_id = d.id AND r.is_delete = 0
     AND r.registration_status NOT IN ('WITHDRAWN', 'CANCELLED')) AS registration_count,
  (SELECT COUNT(1) FROM basket_event_team et
   WHERE et.division_id = d.id AND et.is_delete = 0) AS event_team_count,
  (SELECT COUNT(1) FROM basket_schedule_batch sb
   WHERE sb.division_id = d.id AND sb.is_delete = 0) AS schedule_batch_count,
  (SELECT COUNT(1) FROM basket_match m
   WHERE m.division_id = d.id AND m.is_delete = 0) AS match_count,
  (SELECT COUNT(1) FROM basket_match_submission ms
   WHERE ms.division_id = d.id AND ms.is_delete = 0) AS submission_count,
  (SELECT COUNT(1) FROM basket_standing st
   WHERE st.division_id = d.id AND st.is_delete = 0) AS standing_count
FROM basket_division d
WHERE d.is_delete = 0
  AND d.platform_code IN ('PS', 'XBOX')
ORDER BY d.season_id, d.platform_code;

-- ---------------------------------------------------------------------------
-- 3. 可安全迁移为 PS_XBOX 的记录（无业务数据且 season 下无冲突）
--    条件：platform=PS 或 XBOX，且无报名/参赛/赛程/比赛
-- ---------------------------------------------------------------------------
SELECT d.id, d.season_id, d.platform_code, d.division_name
FROM basket_division d
WHERE d.is_delete = 0
  AND d.platform_code IN ('PS', 'XBOX')
  AND NOT EXISTS (
    SELECT 1 FROM basket_registration r
    WHERE r.division_id = d.id AND r.is_delete = 0
      AND r.registration_status NOT IN ('WITHDRAWN', 'CANCELLED')
  )
  AND NOT EXISTS (SELECT 1 FROM basket_event_team et WHERE et.division_id = d.id AND et.is_delete = 0)
  AND NOT EXISTS (SELECT 1 FROM basket_schedule_batch sb WHERE sb.division_id = d.id AND sb.is_delete = 0)
  AND NOT EXISTS (SELECT 1 FROM basket_match m WHERE m.division_id = d.id AND m.is_delete = 0);

-- ---------------------------------------------------------------------------
-- 4. 存在冲突、需人工处理的记录（同 season 有 PS+XBOX 且有业务数据）
-- ---------------------------------------------------------------------------
SELECT d.id, d.season_id, d.platform_code, d.division_name, d.division_status,
  '同赛季存在 PS 与 XBOX 双平台 Division，不可自动合并' AS conflict_reason
FROM basket_division d
WHERE d.is_delete = 0
  AND d.platform_code IN ('PS', 'XBOX')
  AND EXISTS (
    SELECT 1 FROM basket_division d2
    WHERE d2.season_id = d.season_id AND d2.is_delete = 0
      AND d2.platform_code IN ('PS', 'XBOX')
      AND d2.platform_code != d.platform_code
  );

-- ---------------------------------------------------------------------------
-- 5. game_mode 回填检查
-- ---------------------------------------------------------------------------
SELECT
  game_mode,
  COUNT(1) AS division_count
FROM basket_division
WHERE is_delete = 0
GROUP BY game_mode;

SELECT id, platform_code, division_name, game_mode
FROM basket_division
WHERE is_delete = 0 AND (game_mode IS NULL OR game_mode = '');

-- ---------------------------------------------------------------------------
-- 6. 缺少规则快照的 Division
-- ---------------------------------------------------------------------------
SELECT d.id, d.division_name, d.platform_code, d.division_status, d.format_snapshot_id
FROM basket_division d
WHERE d.is_delete = 0
  AND (d.format_snapshot_id IS NULL
    OR NOT EXISTS (
      SELECT 1 FROM basket_division_format_snapshot s
      WHERE s.id = d.format_snapshot_id AND s.is_delete = 0
    ));

-- ---------------------------------------------------------------------------
-- 7. 模板阶段连续性检查
-- ---------------------------------------------------------------------------
SELECT
  t.template_code,
  t.version_no,
  COUNT(s.id) AS stage_count,
  MIN(s.stage_order) AS min_order,
  MAX(s.stage_order) AS max_order,
  CASE
    WHEN COUNT(s.id) = 0 THEN '无阶段'
    WHEN MIN(s.stage_order) != 1 THEN '阶段顺序未从1开始'
    WHEN MAX(s.stage_order) != COUNT(s.id) THEN '阶段顺序不连续'
    ELSE 'OK'
  END AS continuity_check
FROM basket_format_template t
LEFT JOIN basket_format_template_stage s ON s.template_id = t.id AND s.is_delete = 0
WHERE t.is_delete = 0
GROUP BY t.id, t.template_code, t.version_no;
