-- =============================================================================
-- CNPA 动态赛制重构 第 2 轮：数据检查（只读）
-- =============================================================================

SET NAMES utf8mb4;

-- 1. 同一 Division 出现多个 current 快照
SELECT 'DUP_CURRENT_SNAPSHOT' AS check_code, division_id, COUNT(*) AS cnt
FROM basket_division_format_snapshot
WHERE is_delete = 0 AND is_current = 1
GROUP BY division_id
HAVING COUNT(*) > 1;

-- 2. 当前快照缺少 active_unique_key
SELECT 'MISSING_ACTIVE_KEY' AS check_code, id, division_id
FROM basket_division_format_snapshot
WHERE is_delete = 0 AND is_current = 1
  AND (active_unique_key IS NULL OR active_unique_key = '');

-- 3. 历史快照仍占用 active_unique_key
SELECT 'HISTORY_HAS_ACTIVE_KEY' AS check_code, id, division_id, active_unique_key
FROM basket_division_format_snapshot
WHERE (is_delete = 1 OR is_current = 0)
  AND active_unique_key IS NOT NULL;

-- 4. Division 缺少当前快照
SELECT 'MISSING_SNAPSHOT' AS check_code, d.id AS division_id, d.division_name, d.division_status
FROM basket_division d
LEFT JOIN basket_division_format_snapshot s
  ON s.id = d.format_snapshot_id AND s.is_delete = 0 AND s.is_current = 1
WHERE d.is_delete = 0 AND (d.format_snapshot_id IS NULL OR s.id IS NULL);

-- 5. 模板版本重复（应永久唯一）
SELECT 'DUP_TEMPLATE_VERSION' AS check_code, template_code, version_no, COUNT(*) AS cnt
FROM basket_format_template
GROUP BY template_code, version_no
HAVING COUNT(*) > 1;

-- 6. 个人报名重复有效记录
SELECT 'DUP_PERSONAL_REG' AS check_code, division_id, player_id, COUNT(*) AS cnt
FROM basket_personal_registration
WHERE is_delete = 0 AND active_unique_key IS NOT NULL
GROUP BY division_id, player_id
HAVING COUNT(*) > 1;

-- 7. 抽签人员重复分配
SELECT 'DUP_DRAW_ASSIGN' AS check_code, draw_batch_id, player_id, COUNT(*) AS cnt
FROM basket_personal_draw_assignment
WHERE is_delete = 0
GROUP BY draw_batch_id, player_id
HAVING COUNT(*) > 1;

-- 8. 临时队伍无队长
SELECT 'DRAW_TEAM_NO_CAPTAIN' AS check_code, t.id, t.temporary_team_code, t.draw_batch_id
FROM basket_personal_draw_team t
JOIN basket_personal_draw_batch b ON b.id = t.draw_batch_id AND b.is_delete = 0
WHERE t.is_delete = 0
  AND b.batch_status IN ('DRAFT', 'CONFIRMED')
  AND (t.captain_personal_registration_id IS NULL
       OR NOT EXISTS (
           SELECT 1 FROM basket_personal_draw_assignment a
           WHERE a.draw_team_id = t.id AND a.is_delete = 0 AND a.assignment_role = 'CAPTAIN'
       ));

-- 9. 临时队伍人数异常（相对 Division 人数规则）
SELECT 'DRAW_TEAM_SIZE_ABNORMAL' AS check_code,
       t.id, t.temporary_team_code, t.actual_member_count,
       d.game_mode, d.lineup_min_count, d.lineup_max_count
FROM basket_personal_draw_team t
JOIN basket_division d ON d.id = t.division_id
WHERE t.is_delete = 0
  AND (
    t.actual_member_count < IFNULL(d.lineup_min_count, 0)
    OR t.actual_member_count > IFNULL(d.lineup_max_count, 99)
  );

-- 10. EventTeam 在同一批次重复分组
SELECT 'DUP_GROUP_EVENT_TEAM' AS check_code, grouping_batch_id, event_team_id, COUNT(*) AS cnt
FROM basket_division_group_team
WHERE is_delete = 0
GROUP BY grouping_batch_id, event_team_id
HAVING COUNT(*) > 1;

-- 11. 跨 Division 分组
SELECT 'CROSS_DIVISION_GROUP' AS check_code, gt.id, gt.division_id AS batch_division_id, et.division_id AS event_team_division_id
FROM basket_division_group_team gt
JOIN basket_event_team et ON et.id = gt.event_team_id AND et.is_delete = 0
WHERE gt.is_delete = 0 AND gt.division_id <> et.division_id;

-- 12. 已锁定分组批次数据异常：空组
SELECT 'LOCKED_EMPTY_GROUP' AS check_code, g.id, g.group_code, g.grouping_batch_id
FROM basket_division_group g
JOIN basket_division_grouping_batch b ON b.id = g.grouping_batch_id AND b.is_delete = 0
WHERE g.is_delete = 0
  AND b.grouping_status = 'LOCKED'
  AND NOT EXISTS (
      SELECT 1 FROM basket_division_group_team gt
      WHERE gt.group_id = g.id AND gt.is_delete = 0
  );
