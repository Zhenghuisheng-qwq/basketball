-- =============================================================================
-- CNPA 动态赛制重构 R4：数据检查（只读）
-- =============================================================================

SET NAMES utf8mb4;

-- 1. rank_explain_json 仍有新写入（迁移后不应增加）
SELECT id, division_id, stage_id, event_team_id, rank_explain_json, update_time
FROM basket_standing
WHERE is_delete = 0
  AND rank_explain_json IS NOT NULL
  AND rank_explain_json <> ''
  AND update_time > DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY update_time DESC
LIMIT 50;

-- 2. rank_reason 与 standing 关联完整性
SELECT s.id AS standing_id
FROM basket_standing s
WHERE s.is_delete = 0
  AND s.stage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_standing_rank_reason r
    WHERE r.standing_id = s.id AND r.is_delete = 0
  )
  AND EXISTS (
    SELECT 1 FROM basket_match m
    WHERE m.stage_id = s.stage_id AND m.is_delete = 0
      AND m.match_status IN ('FINISHED', 'FORFEITED')
  )
LIMIT 100;

-- 3. Standing active_unique_key 重复
SELECT active_unique_key, COUNT(*) AS cnt
FROM basket_standing
WHERE is_delete = 0 AND active_unique_key IS NOT NULL
GROUP BY active_unique_key
HAVING cnt > 1;

-- 4. 有效记录 active_unique_key 为空
SELECT id, division_id, stage_id, event_team_id, group_scope_key
FROM basket_standing
WHERE is_delete = 0 AND active_unique_key IS NULL
LIMIT 100;

-- 5. 历史记录缺 stage_id
SELECT id, division_id, event_team_id, rank_no
FROM basket_standing
WHERE is_delete = 0 AND stage_id IS NULL
LIMIT 100;

-- 6. group_scope_key 异常（非 0 且无对应 group）
SELECT s.id, s.division_id, s.stage_id, s.group_scope_key, s.group_id
FROM basket_standing s
WHERE s.is_delete = 0
  AND s.group_scope_key IS NOT NULL
  AND s.group_scope_key <> 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_division_group g
    WHERE g.id = s.group_scope_key AND g.is_delete = 0
  )
LIMIT 100;

-- 7. MANUAL 待处理但未填 manual_rank
SELECT id, stage_id, event_team_id, rank_no, manual_rank_pending, manual_rank_no
FROM basket_standing
WHERE is_delete = 0
  AND manual_rank_pending = 1
  AND manual_rank_no IS NULL
LIMIT 100;

-- 8. MANUAL 已处理但缺原因
SELECT id, stage_id, event_team_id, manual_rank_no, manual_rank_reason
FROM basket_standing
WHERE is_delete = 0
  AND manual_rank_no IS NOT NULL
  AND (manual_rank_reason IS NULL OR manual_rank_reason = '')
LIMIT 100;

-- 9. 同一 stage/group 人工排名重复
SELECT stage_id, group_scope_key, manual_rank_no, COUNT(*) AS cnt
FROM basket_standing
WHERE is_delete = 0
  AND manual_rank_no IS NOT NULL
GROUP BY stage_id, group_scope_key, manual_rank_no
HAVING cnt > 1;

-- 10. rank_explain_json 历史数据量（R5 清理参考）
SELECT COUNT(*) AS legacy_json_count
FROM basket_standing
WHERE is_delete = 0 AND rank_explain_json IS NOT NULL AND rank_explain_json <> '';

-- 11. R4 测试数据标识检查
SELECT id, division_name, division_status, format_snapshot_id
FROM basket_division
WHERE is_delete = 0 AND division_name LIKE 'R4_TEST_%';

-- 12. Division 无规则快照
SELECT d.id, d.division_name
FROM basket_division d
WHERE d.is_delete = 0
  AND d.format_snapshot_id IS NULL
  AND d.division_name NOT LIKE 'R4_TEST_%';

-- 13. 已开放报名但快照不是 LOCKED/IN_USE
SELECT d.id, d.division_name, d.division_status, fs.snapshot_status
FROM basket_division d
LEFT JOIN basket_division_format_snapshot fs ON fs.id = d.format_snapshot_id AND fs.is_delete = 0
WHERE d.is_delete = 0
  AND d.division_status IN ('REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'IN_PROGRESS')
  AND (fs.id IS NULL OR fs.snapshot_status NOT IN ('LOCKED', 'IN_USE'));

-- 14. Legacy 赛程批次（无 stage_id / generation_strategy）与新 Division 混用
SELECT sb.id, sb.division_id, d.division_name, d.format_snapshot_id, sb.stage_id, sb.generation_strategy
FROM basket_schedule_batch sb
JOIN basket_division d ON d.id = sb.division_id AND d.is_delete = 0
WHERE sb.is_delete = 0
  AND sb.stage_id IS NULL
  AND sb.generation_strategy IS NULL
  AND d.format_snapshot_id IS NOT NULL;

-- 15. 新 Division 无快照（排除测试）
SELECT d.id, d.division_name
FROM basket_division d
WHERE d.is_delete = 0
  AND d.format_snapshot_id IS NULL
  AND d.domain_version = 'V4'
  AND d.division_name NOT LIKE 'R4_TEST_%';

-- 16. 同一 Division 多个 current Snapshot
SELECT division_id, COUNT(*) AS cnt
FROM basket_division_format_snapshot
WHERE is_delete = 0 AND is_current = 1
GROUP BY division_id
HAVING cnt > 1;

-- 17. 新 Match 无 stage_id（动态赛制 Division）
SELECT m.id, m.division_id, d.format_snapshot_id
FROM basket_match m
JOIN basket_division d ON d.id = m.division_id AND d.is_delete = 0
WHERE m.is_delete = 0
  AND m.stage_id IS NULL
  AND d.format_snapshot_id IS NOT NULL
  AND m.create_time > DATE_SUB(NOW(), INTERVAL 30 DAY)
LIMIT 100;

-- 18. GROUP_STAGE Match 无 group_id
SELECT m.id, m.stage_id, m.division_id
FROM basket_match m
JOIN basket_division_stage ds ON ds.id = m.stage_id AND ds.is_delete = 0
WHERE m.is_delete = 0
  AND ds.stage_type = 'GROUP'
  AND m.group_id IS NULL
LIMIT 100;

-- 19. manual_rank_pending 状态异常（无同分待处理但标记 pending）
SELECT s.id, s.stage_id, s.event_team_id, s.manual_rank_pending, s.rank_no
FROM basket_standing s
WHERE s.is_delete = 0
  AND s.manual_rank_pending = 1
  AND s.manual_rank_no IS NOT NULL
LIMIT 100;

-- 20. Legacy Division 数量（统计，非异常）
SELECT COUNT(*) AS legacy_division_count
FROM basket_division d
WHERE d.is_delete = 0 AND d.format_snapshot_id IS NULL;

-- 21. Legacy ScheduleBatch 数量（统计）
SELECT COUNT(*) AS legacy_batch_count
FROM basket_schedule_batch sb
WHERE sb.is_delete = 0 AND sb.stage_id IS NULL AND sb.generation_strategy IS NULL;

-- 22. Legacy Match 数量（统计）
SELECT COUNT(*) AS legacy_match_count
FROM basket_match m
WHERE m.is_delete = 0 AND m.stage_id IS NULL;

-- 23. 无 stage_id Standing 数量（Legacy 统计）
SELECT COUNT(*) AS legacy_standing_count
FROM basket_standing s
WHERE s.is_delete = 0 AND s.stage_id IS NULL;
