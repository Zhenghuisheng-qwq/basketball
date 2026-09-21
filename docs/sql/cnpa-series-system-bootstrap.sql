-- CNPA system series bootstrap. Idempotent by series_code where is_delete=0.
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- SYSTEM_KEEP series bootstrap. Idempotent.
-- ---------------------------------------------------------------------------
INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'LEAGUE', 'League', 'CNPA League 系列', 'REGULAR_ROUND_ROBIN', 1, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code = 'LEAGUE' AND is_delete = 0);

INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'ARENA', 'Arena', 'CNPA Arena 系列', 'REGULAR_ROUND_ROBIN', 2, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code = 'ARENA' AND is_delete = 0);

INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'CUP', 'Cup', 'CNPA Cup 系列', 'REGULAR_ROUND_ROBIN', 3, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code = 'CUP' AND is_delete = 0);

INSERT INTO basket_series (series_code, series_name, series_desc, default_format_type, sort_order, enabled, create_time, update_time, is_delete)
SELECT 'OPEN', 'Open', 'CNPA Open 系列', 'REGULAR_ROUND_ROBIN', 4, 1, NOW(), NOW(), 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_series WHERE series_code = 'OPEN' AND is_delete = 0);

-- STANDARD templates bind by series_code. Global CUSTOM templates stay series_id NULL.
UPDATE basket_format_template t
JOIN basket_series s ON s.series_code = 'LEAGUE' AND s.is_delete = 0
SET t.series_id = s.id, t.update_time = NOW()
WHERE t.template_code = 'LEAGUE_STANDARD_V1' AND t.is_delete = 0
  AND (t.series_id IS NULL OR t.series_id <> s.id);

UPDATE basket_format_template t
JOIN basket_series s ON s.series_code = 'ARENA' AND s.is_delete = 0
SET t.series_id = s.id, t.update_time = NOW()
WHERE t.template_code = 'ARENA_STANDARD_V1' AND t.is_delete = 0
  AND (t.series_id IS NULL OR t.series_id <> s.id);

UPDATE basket_format_template t
JOIN basket_series s ON s.series_code = 'CUP' AND s.is_delete = 0
SET t.series_id = s.id, t.update_time = NOW()
WHERE t.template_code = 'CUP_STANDARD_V1' AND t.is_delete = 0
  AND (t.series_id IS NULL OR t.series_id <> s.id);

UPDATE basket_format_template t
JOIN basket_series s ON s.series_code = 'OPEN' AND s.is_delete = 0
SET t.series_id = s.id, t.update_time = NOW()
WHERE t.template_code = 'OPEN_STANDARD_V1' AND t.is_delete = 0
  AND (t.series_id IS NULL OR t.series_id <> s.id);
