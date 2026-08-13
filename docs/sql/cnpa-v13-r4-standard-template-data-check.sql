-- CNPA V1.3 R4 data check (read-only)
SET NAMES utf8mb4;

SELECT 'STANDARD_TEMPLATE_COUNT' AS check_code, COUNT(*) AS cnt
FROM basket_format_template
WHERE template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND version_no = 1 AND is_delete = 0 AND enabled = 1;

SELECT template_code, template_type, default_registration_mode, enabled, is_default
FROM basket_format_template
WHERE template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND is_delete = 0
ORDER BY template_code;

SELECT t.template_code, s.stage_code, s.stage_order, s.competition_format, s.advancement_type,
       s.advancement_count, s.pairing_rule, s.grouping_mode, s.round_robin_cycles, s.seed_rule, s.draw_rule, s.bye_rule
FROM basket_format_template t
JOIN basket_format_template_stage s ON s.template_id = t.id AND s.is_delete = 0
WHERE t.template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND t.is_delete = 0
ORDER BY t.template_code, s.stage_order;

SELECT 'CUSTOM_TEMPLATES_STILL_ENABLED' AS check_code, COUNT(*) AS cnt
FROM basket_format_template
WHERE template_code IN ('ROUND_ROBIN_BASIC_V1','GROUP_STAGE_BASIC_V1','SINGLE_ELIM_BO1_V1')
  AND is_delete = 0 AND enabled = 1;

SELECT 'OPEN_HAS_NO_FORMATION_STAGE' AS check_code, COUNT(*) AS cnt
FROM basket_format_template t
JOIN basket_format_template_stage s ON s.template_id = t.id AND s.is_delete = 0
WHERE t.template_code = 'OPEN_STANDARD_V1' AND t.is_delete = 0
  AND (s.stage_code LIKE '%FORMATION%' OR s.stage_type = 'FORMATION');

SELECT 'LOCKED_SNAPSHOTS_UNTOUCHED_BY_DESIGN' AS check_code, COUNT(*) AS cnt
FROM basket_division_format_snapshot
WHERE is_delete = 0;
