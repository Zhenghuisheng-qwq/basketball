-- CNPA V2 MatchSeries R2-3 数据检查（只读）
SET NAMES utf8mb4;

-- 1. COMPLETED Series winner NULL
SELECT 'SERIES_COMPLETED_NO_WINNER' AS check_code, id, series_status, winner_event_team_id
FROM basket_match_series
WHERE is_delete = 0 AND series_status = 'COMPLETED' AND winner_event_team_id IS NULL;

-- 2. RUNNING/READY winner 非 NULL
SELECT 'SERIES_INCOMPLETE_HAS_WINNER' AS check_code, id, series_status, winner_event_team_id
FROM basket_match_series
WHERE is_delete = 0
  AND series_status IN ('READY', 'RUNNING', 'PENDING')
  AND winner_event_team_id IS NOT NULL;

-- 3. Series home 胜场与正式 Match 不一致
SELECT 'SERIES_SCORE_MISMATCH' AS check_code, s.id AS series_id, s.home_win_count AS stored_home,
       (
         SELECT COUNT(*) FROM basket_match m
         WHERE m.series_id = s.id AND m.is_delete = 0
           AND m.match_status IN ('FINISHED', 'FORFEITED')
           AND m.winner_event_team_id = s.home_event_team_id
       ) AS calc_home
FROM basket_match_series s
WHERE s.is_delete = 0
  AND s.series_status <> 'CANCELLED'
HAVING stored_home <> calc_home;

-- 4. 胜场超过 requiredWins
SELECT 'SERIES_WINS_EXCEED_REQUIRED' AS check_code, id, best_of, home_win_count, away_win_count
FROM basket_match_series
WHERE is_delete = 0
  AND GREATEST(home_win_count, away_win_count) > (best_of / 2 + 1);

-- 5. winner 不是双方之一
SELECT 'SERIES_WINNER_INVALID' AS check_code, id, winner_event_team_id, home_event_team_id, away_event_team_id
FROM basket_match_series
WHERE is_delete = 0
  AND winner_event_team_id IS NOT NULL
  AND winner_event_team_id NOT IN (home_event_team_id, away_event_team_id);

-- 6. COMPLETED 非最终 Series 缺少 Qualification
SELECT 'SERIES_NO_QUALIFICATION' AS check_code, s.id AS series_id, s.stage_id, s.winner_event_team_id
FROM basket_match_series s
JOIN basket_division_stage st ON st.id = s.stage_id AND st.is_delete = 0
WHERE s.is_delete = 0
  AND s.series_status = 'COMPLETED'
  AND s.winner_event_team_id IS NOT NULL
  AND st.advancement_type = 'WINNER_ADVANCE'
  AND st.next_stage_code IS NOT NULL AND st.next_stage_code <> ''
  AND NOT EXISTS (
    SELECT 1 FROM basket_division_stage_qualification q
    WHERE q.is_delete = 0
      AND q.source_series_id = s.id
      AND q.event_team_id = s.winner_event_team_id
      AND q.qualification_status IN ('PREVIEW', 'CONFIRMED')
  );

-- 7. 同一 Series 重复有效 Qualification
SELECT 'DUP_SERIES_QUALIFICATION' AS check_code, source_series_id, COUNT(*) AS cnt
FROM basket_division_stage_qualification
WHERE is_delete = 0
  AND source_series_id IS NOT NULL
  AND qualification_status IN ('PREVIEW', 'CONFIRMED')
GROUP BY source_series_id
HAVING COUNT(*) > 1;

-- 8. Qualification sourceSeries 孤儿
SELECT 'QUALIFICATION_ORPHAN_SERIES' AS check_code, q.id, q.source_series_id
FROM basket_division_stage_qualification q
WHERE q.is_delete = 0
  AND q.source_series_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_match_series s WHERE s.id = q.source_series_id AND s.is_delete = 0
  );

-- 9. Series 非 COMPLETED 仍有有效 Qualification
SELECT 'SERIES_NOT_DONE_HAS_QUAL' AS check_code, s.id, s.series_status, q.id AS qual_id
FROM basket_match_series s
JOIN basket_division_stage_qualification q ON q.source_series_id = s.id AND q.is_delete = 0
WHERE s.is_delete = 0
  AND s.series_status <> 'COMPLETED'
  AND q.qualification_status IN ('PREVIEW', 'CONFIRMED');

-- 10. Series/Match stage 不一致
SELECT 'SERIES_MATCH_STAGE_MISMATCH' AS check_code, s.id AS series_id, m.id AS match_id, s.stage_id, m.stage_id
FROM basket_match_series s
JOIN basket_match m ON m.series_id = s.id AND m.is_delete = 0
WHERE s.is_delete = 0 AND s.stage_id <> m.stage_id;
