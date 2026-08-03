-- =============================================================================
-- CNPA 赛事域 V4 第 1 轮：Seed 数据
-- 前置：已执行 cnpa-v4-r1-ddl.sql
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 四种固定 Series
-- ---------------------------------------------------------------------------
INSERT INTO basket_series (
  series_code, series_name, series_desc, default_format_type,
  icon_file_id, cover_file_id, banner_file_id,
  sort_order, enabled, create_time, update_time, is_delete
) VALUES
('LEAGUE', 'League', 'CNPA League 系列', 'REGULAR_ROUND_ROBIN', NULL, NULL, NULL, 1, 1, NOW(), NOW(), 0),
('ARENA',  'Arena',  'CNPA Arena 系列',  'REGULAR_ROUND_ROBIN', NULL, NULL, NULL, 2, 1, NOW(), NOW(), 0),
('CUP',    'Cup',    'CNPA Cup 系列',    'REGULAR_ROUND_ROBIN', NULL, NULL, NULL, 3, 1, NOW(), NOW(), 0),
('OPEN',   'Open',   'CNPA Open 系列',   'REGULAR_ROUND_ROBIN', NULL, NULL, NULL, 4, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  series_name = VALUES(series_name),
  series_desc = VALUES(series_desc),
  update_time = NOW();

-- ---------------------------------------------------------------------------
-- 2. 可选示例：League 2026 S1 + PS / Xbox Division
-- ---------------------------------------------------------------------------
INSERT INTO basket_season (
  series_id, season_year, season_name, display_title, description,
  cover_file_id, banner_file_id, registration_mode, format_type,
  registration_start_time, registration_end_time, match_start_time, match_end_time,
  publish_status, season_status, create_time, update_time, is_delete
)
SELECT
  s.id,
  2026,
  'S1',
  'CNPA League 2026 S1',
  'V4 示例赛季：League 2026 S1',
  NULL,
  NULL,
  'TEAM',
  'REGULAR_ROUND_ROBIN',
  '2026-08-10 00:00:00',
  '2026-08-31 23:59:59',
  '2026-09-01 00:00:00',
  '2026-12-31 23:59:59',
  'DRAFT',
  'NOT_STARTED',
  NOW(),
  NOW(),
  0
FROM basket_series s
WHERE s.series_code = 'LEAGUE' AND s.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_season x
    WHERE x.series_id = s.id AND x.season_year = 2026 AND x.season_name = 'S1' AND x.is_delete = 0
  );

INSERT INTO basket_division (
  season_id, platform_code, division_name, registration_mode, format_type,
  max_team_count, max_person_count, lineup_min_count, lineup_max_count,
  registration_start_time, registration_end_time, match_start_time, match_end_time,
  division_status, sort_order, create_time, update_time, is_delete
)
SELECT
  se.id,
  'PS',
  'League 2026 S1 - PS',
  'TEAM',
  'REGULAR_ROUND_ROBIN',
  16,
  NULL,
  5,
  8,
  se.registration_start_time,
  se.registration_end_time,
  se.match_start_time,
  se.match_end_time,
  'DRAFT',
  1,
  NOW(),
  NOW(),
  0
FROM basket_season se
INNER JOIN basket_series s ON s.id = se.series_id AND s.series_code = 'LEAGUE' AND s.is_delete = 0
WHERE se.season_year = 2026 AND se.season_name = 'S1' AND se.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_division d
    WHERE d.season_id = se.id AND d.platform_code = 'PS' AND d.is_delete = 0
  );

INSERT INTO basket_division (
  season_id, platform_code, division_name, registration_mode, format_type,
  max_team_count, max_person_count, lineup_min_count, lineup_max_count,
  registration_start_time, registration_end_time, match_start_time, match_end_time,
  division_status, sort_order, create_time, update_time, is_delete
)
SELECT
  se.id,
  'XBOX',
  'League 2026 S1 - Xbox',
  'TEAM',
  'REGULAR_ROUND_ROBIN',
  16,
  NULL,
  5,
  8,
  se.registration_start_time,
  se.registration_end_time,
  se.match_start_time,
  se.match_end_time,
  'DRAFT',
  2,
  NOW(),
  NOW(),
  0
FROM basket_season se
INNER JOIN basket_series s ON s.id = se.series_id AND s.series_code = 'LEAGUE' AND s.is_delete = 0
WHERE se.season_year = 2026 AND se.season_name = 'S1' AND se.is_delete = 0
  AND NOT EXISTS (
    SELECT 1 FROM basket_division d
    WHERE d.season_id = se.id AND d.platform_code = 'XBOX' AND d.is_delete = 0
  );
