-- CNPA 动态赛制 R3 数据检查

-- 1. 阶段队伍重复
SELECT stage_id, event_team_id, COUNT(*) AS cnt
FROM basket_division_stage_team
WHERE is_delete = 0 AND active_unique_key IS NOT NULL
GROUP BY stage_id, event_team_id HAVING cnt > 1;

-- 2. 跨 Division EventTeam
SELECT st.id, st.division_id, et.division_id AS et_division
FROM basket_division_stage_team st
JOIN basket_event_team et ON et.id = st.event_team_id AND et.is_delete = 0
WHERE st.is_delete = 0 AND st.division_id <> et.division_id;

-- 3. GROUP 阶段未锁定分组
SELECT ds.id, ds.stage_code, ds.execution_status
FROM basket_division_stage ds
WHERE ds.is_delete = 0 AND ds.stage_type = 'GROUP'
  AND ds.execution_status IN ('READY', 'SCHEDULE_GENERATED', 'RUNNING')
  AND NOT EXISTS (
    SELECT 1 FROM basket_division_grouping_batch gb
    WHERE gb.stage_id = ds.id AND gb.grouping_status = 'LOCKED' AND gb.is_delete = 0
  );

-- 4. 赛程比赛数异常
SELECT sb.id, sb.expected_match_count, sb.actual_match_count,
       (SELECT COUNT(*) FROM basket_match m WHERE m.schedule_batch_id = sb.id AND m.is_delete = 0) AS real_count
FROM basket_schedule_batch sb
WHERE sb.is_delete = 0 AND sb.expected_match_count IS NOT NULL
  AND sb.expected_match_count <> (SELECT COUNT(*) FROM basket_match m WHERE m.schedule_batch_id = sb.id AND m.is_delete = 0);

-- 5. 同一对球队重复（同阶段）
SELECT stage_id, LEAST(home_event_team_id, away_event_team_id) AS a,
       GREATEST(home_event_team_id, away_event_team_id) AS b, COUNT(*) AS cnt
FROM basket_match WHERE is_delete = 0 AND stage_id IS NOT NULL
GROUP BY stage_id, a, b HAVING cnt > 1;

-- 6. 小组跨组比赛
SELECT m.id, m.group_id, gt.group_id AS team_group
FROM basket_match m
JOIN basket_division_group_team gt ON gt.event_team_id = m.home_event_team_id AND gt.is_delete = 0
WHERE m.is_delete = 0 AND m.group_id IS NOT NULL AND m.group_id <> gt.group_id;

-- 7. 淘汰赛队伍数非法（非2幂）
SELECT ds.id, ds.stage_code, COUNT(st.id) AS team_cnt
FROM basket_division_stage ds
JOIN basket_division_stage_team st ON st.stage_id = ds.id AND st.is_delete = 0
WHERE ds.is_delete = 0 AND ds.competition_format = 'SINGLE_ELIMINATION'
  AND ds.execution_status IN ('READY', 'SCHEDULE_GENERATED')
GROUP BY ds.id HAVING team_cnt > 0 AND (team_cnt & (team_cnt - 1)) <> 0;

-- 8. Match 缺 stage_id（R3 新批次）
SELECT id, match_no FROM basket_match
WHERE is_delete = 0 AND stage_id IS NULL AND schedule_batch_id IN (
  SELECT id FROM basket_schedule_batch WHERE generation_strategy IS NOT NULL AND is_delete = 0
);

-- 9. Standing 维度重复
SELECT active_unique_key, COUNT(*) AS cnt FROM basket_standing
WHERE is_delete = 0 AND active_unique_key IS NOT NULL
GROUP BY active_unique_key HAVING cnt > 1;

-- 10. 晋级记录重复
SELECT active_unique_key, COUNT(*) AS cnt FROM basket_division_stage_qualification
WHERE is_delete = 0 AND active_unique_key IS NOT NULL
GROUP BY active_unique_key HAVING cnt > 1;

-- 11. 已完成阶段存在未完成比赛
SELECT ds.id, ds.stage_code, COUNT(m.id) AS pending_matches
FROM basket_division_stage ds
JOIN basket_match m ON m.stage_id = ds.id AND m.is_delete = 0
WHERE ds.execution_status = 'COMPLETED' AND ds.is_delete = 0
  AND m.match_status NOT IN ('FINISHED', 'FORFEITED')
GROUP BY ds.id;
