-- CNPA V2 R3-1 执行异常数据检查（只读）
SET NAMES utf8mb4;

-- 1. BYE 不应伪造 source_series_id / 指向不存在比赛
SELECT 'BYE_FAKE_SOURCE' AS check_code, id, source_series_id
FROM basket_division_stage_qualification
WHERE is_delete = 0 AND qualification_type = 'BYE_ADVANCE' AND source_series_id IS NOT NULL;

-- 2. BYE Qualification 重复（同 stage + team 有效）
SELECT 'DUP_BYE_QUALIFICATION' AS check_code, stage_id, event_team_id, COUNT(*) AS cnt
FROM basket_division_stage_qualification
WHERE is_delete = 0 AND qualification_type = 'BYE_ADVANCE'
  AND qualification_status IN ('PREVIEW', 'CONFIRMED')
GROUP BY stage_id, event_team_id HAVING COUNT(*) > 1;

-- 3. MANUAL Qualification 重复
SELECT 'DUP_MANUAL_QUALIFICATION' AS check_code, stage_id, event_team_id, COUNT(*) AS cnt
FROM basket_division_stage_qualification
WHERE is_delete = 0 AND qualification_type = 'MANUAL'
  AND qualification_status IN ('PREVIEW', 'CONFIRMED')
GROUP BY stage_id, event_team_id HAVING COUNT(*) > 1;

-- 4. Series FORFEIT 无 winner
SELECT 'SERIES_FORFEIT_NO_WINNER' AS check_code, id, resolution_type, winner_event_team_id
FROM basket_match_series
WHERE is_delete = 0 AND resolution_type = 'FORFEIT' AND winner_event_team_id IS NULL;

-- 5. Series FORFEIT winner 非法
SELECT 'SERIES_FORFEIT_WINNER_INVALID' AS check_code, id, winner_event_team_id, home_event_team_id, away_event_team_id
FROM basket_match_series
WHERE is_delete = 0 AND resolution_type = 'FORFEIT'
  AND winner_event_team_id IS NOT NULL
  AND winner_event_team_id NOT IN (home_event_team_id, away_event_team_id);

-- 6. Series FORFEIT 仍 RUNNING
SELECT 'SERIES_FORFEIT_STILL_RUNNING' AS check_code, id, series_status, resolution_type
FROM basket_match_series
WHERE is_delete = 0 AND resolution_type = 'FORFEIT' AND series_status <> 'COMPLETED';

-- 7. FORFEIT Series 完成后仍有新 FINISHED child（异常）
SELECT 'SERIES_FORFEIT_NEW_FINISHED_GAME' AS check_code, s.id AS series_id, m.id AS match_id
FROM basket_match_series s
JOIN basket_match m ON m.series_id = s.id AND m.is_delete = 0
WHERE s.is_delete = 0 AND s.resolution_type = 'FORFEIT'
  AND m.match_status IN ('FINISHED', 'FORFEITED')
  AND m.update_time > s.resolved_time;

-- 8. fake event_team_id
SELECT 'FAKE_EVENT_TEAM_ZERO' AS check_code, id, event_team_id
FROM basket_division_stage_qualification
WHERE is_delete = 0 AND (event_team_id IS NULL OR event_team_id = 0);

-- 9. Match FORFEITED winner NULL
SELECT 'MATCH_FORFEITED_NO_WINNER' AS check_code, id, match_status, winner_event_team_id
FROM basket_match
WHERE is_delete = 0 AND match_status = 'FORFEITED' AND winner_event_team_id IS NULL;

-- 10. Match FORFEITED winner 非法
SELECT 'MATCH_FORFEITED_WINNER_INVALID' AS check_code, id, winner_event_team_id, home_event_team_id, away_event_team_id
FROM basket_match
WHERE is_delete = 0 AND match_status = 'FORFEITED'
  AND winner_event_team_id NOT IN (home_event_team_id, away_event_team_id);

-- 11. Series / child Match 对手不一致
SELECT 'SERIES_MATCH_OPPONENT_MISMATCH' AS check_code, s.id AS series_id, m.id AS match_id
FROM basket_match_series s
JOIN basket_match m ON m.series_id = s.id AND m.is_delete = 0
WHERE s.is_delete = 0
  AND (m.home_event_team_id NOT IN (s.home_event_team_id, s.away_event_team_id)
    OR m.away_event_team_id NOT IN (s.home_event_team_id, s.away_event_team_id));
