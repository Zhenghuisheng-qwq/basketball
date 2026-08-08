-- =============================================================================
-- CNPA V2 自定义赛制 R2-1：MatchSeries 数据检查（只读）
-- =============================================================================

SET NAMES utf8mb4;

-- 1. MatchSeries 非法 best_of
SELECT 'SERIES_INVALID_BEST_OF' AS check_code, id, best_of
FROM basket_match_series
WHERE is_delete = 0 AND best_of NOT IN (3, 5);

-- 2. home = away
SELECT 'SERIES_HOME_EQ_AWAY' AS check_code, id, home_event_team_id, away_event_team_id
FROM basket_match_series
WHERE is_delete = 0 AND home_event_team_id = away_event_team_id;

-- 3. COMPLETED 但 winner NULL
SELECT 'SERIES_COMPLETED_NO_WINNER' AS check_code, id, series_status, winner_event_team_id
FROM basket_match_series
WHERE is_delete = 0 AND series_status = 'COMPLETED' AND winner_event_team_id IS NULL;

-- 4. 未完成但 winner 非 NULL
SELECT 'SERIES_INCOMPLETE_HAS_WINNER' AS check_code, id, series_status, winner_event_team_id
FROM basket_match_series
WHERE is_delete = 0
  AND series_status NOT IN ('COMPLETED', 'CANCELLED')
  AND winner_event_team_id IS NOT NULL;

-- 5. winner 不是 home/away
SELECT 'SERIES_WINNER_INVALID' AS check_code, id, winner_event_team_id, home_event_team_id, away_event_team_id
FROM basket_match_series
WHERE is_delete = 0
  AND winner_event_team_id IS NOT NULL
  AND winner_event_team_id NOT IN (home_event_team_id, away_event_team_id);

-- 6. home_win_count < 0
SELECT 'SERIES_HOME_WINS_NEGATIVE' AS check_code, id, home_win_count
FROM basket_match_series
WHERE is_delete = 0 AND home_win_count < 0;

-- 7. away_win_count < 0
SELECT 'SERIES_AWAY_WINS_NEGATIVE' AS check_code, id, away_win_count
FROM basket_match_series
WHERE is_delete = 0 AND away_win_count < 0;

-- 8. wins 总数 > best_of
SELECT 'SERIES_WINS_EXCEED_BEST_OF' AS check_code, id, best_of, home_win_count, away_win_count
FROM basket_match_series
WHERE is_delete = 0 AND home_win_count + away_win_count > best_of;

-- 9. duplicate stage+round+slot（活跃行）
SELECT 'DUP_SERIES_STAGE_ROUND_SLOT' AS check_code, stage_id, round_no, bracket_slot, COUNT(*) AS cnt
FROM basket_match_series
WHERE is_delete = 0
GROUP BY stage_id, round_no, bracket_slot
HAVING COUNT(*) > 1;

-- 10. Match series_id 指向不存在 Series
SELECT 'MATCH_SERIES_ORPHAN' AS check_code, m.id AS match_id, m.series_id
FROM basket_match m
WHERE m.is_delete = 0
  AND m.series_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_match_series s WHERE s.id = m.series_id AND s.is_delete = 0
  );

-- 11. Series child Match series_game_no 重复（活跃行）
SELECT 'DUP_MATCH_SERIES_GAME_NO' AS check_code, series_id, series_game_no, COUNT(*) AS cnt
FROM basket_match
WHERE is_delete = 0 AND series_id IS NOT NULL
GROUP BY series_id, series_game_no
HAVING COUNT(*) > 1;

-- 12. series_id 有值但 series_game_no NULL
SELECT 'MATCH_SERIES_NO_GAME_NO' AS check_code, id, series_id, series_game_no
FROM basket_match
WHERE is_delete = 0 AND series_id IS NOT NULL AND series_game_no IS NULL;

-- 13. series_game_no 超过 best_of
SELECT 'MATCH_GAME_NO_EXCEED_BEST_OF' AS check_code, m.id, m.series_id, m.series_game_no, s.best_of
FROM basket_match m
JOIN basket_match_series s ON s.id = m.series_id AND s.is_delete = 0
WHERE m.is_delete = 0
  AND m.series_id IS NOT NULL
  AND m.series_game_no IS NOT NULL
  AND m.series_game_no > s.best_of;

-- 14. BO1 历史 Match 被错误关联 Series（best_of=1 阶段下的 match 不应有 series_id）
SELECT 'BO1_MATCH_HAS_SERIES' AS check_code, m.id, m.series_id, st.best_of
FROM basket_match m
JOIN basket_division_stage st ON st.id = m.stage_id AND st.is_delete = 0
WHERE m.is_delete = 0
  AND m.series_id IS NOT NULL
  AND st.best_of = 1;
