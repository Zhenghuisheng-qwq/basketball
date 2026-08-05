-- =============================================================================
-- CNPA 动态赛制重构 R5：清理后只读验收（核心检查项应全部为 0 或空结果集）
-- =============================================================================
SET NAMES utf8mb4;

SELECT 'check_01_pure_legacy_tables' AS check_id, COUNT(*) AS cnt
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'basket_tournament_registration', 'basket_team_season_profile',
    'basket_schedule_generate_batch', 'basket_stage_standing',
    'basket_match_data_submission', 'basket_captain_qualification'
  );

SELECT 'check_02_legacy_menus' AS check_id, COUNT(*) AS cnt
FROM sys_menu
WHERE component LIKE 'cnpa/event/%'
   OR component LIKE 'cnpa/competition/%'
   OR component LIKE 'cnpa/statistics/%'
   OR component LIKE 'cnpa/participation/%'
   OR perms IN ('basket:tournament:list','basket:standing:list','basket:registrationAudit:list');

SELECT 'check_03_division_no_snapshot' AS check_id, COUNT(*) AS cnt
FROM basket_division WHERE is_delete=0 AND format_snapshot_id IS NULL;

SELECT 'check_04_match_no_stage' AS check_id, COUNT(*) AS cnt
FROM basket_match WHERE is_delete=0 AND (stage_id IS NULL OR stage_id=0);

SELECT 'check_05_batch_no_stage' AS check_id, COUNT(*) AS cnt
FROM basket_schedule_batch WHERE is_delete=0 AND (stage_id IS NULL OR stage_id=0);

SELECT 'check_06_standing_no_stage' AS check_id, COUNT(*) AS cnt
FROM basket_standing WHERE is_delete=0 AND (stage_id IS NULL OR stage_id=0);

SELECT 'check_07_standing_no_active_key' AS check_id, COUNT(*) AS cnt
FROM basket_standing WHERE is_delete=0 AND (active_unique_key IS NULL OR active_unique_key='');

SELECT 'check_08_active_key_duplicate' AS check_id, active_unique_key, COUNT(*) AS cnt
FROM basket_standing WHERE is_delete=0 AND active_unique_key IS NOT NULL AND active_unique_key<>''
GROUP BY active_unique_key HAVING COUNT(*)>1;

SELECT 'check_09_rank_explain_json_column' AS check_id, COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='basket_standing' AND COLUMN_NAME='rank_explain_json';

SELECT 'check_10_legacy_write_perms' AS check_id, COUNT(*) AS cnt
FROM sys_menu
WHERE perms IN ('basket:tournament:add','basket:match:schedule','basket:standing:rebuild','basket:dataSettlement:list');

SELECT 'check_11_ps_xbox_division_write' AS check_id, COUNT(*) AS cnt
FROM basket_division WHERE is_delete=0 AND platform_code IN ('PS','XBOX','PS5');

SELECT 'check_12_legacy_registration' AS check_id, COUNT(*) AS cnt
FROM basket_registration
WHERE is_delete=0 AND (registration_source IS NULL OR registration_source NOT IN ('TEAM_DIRECT','PERSONAL_FORMATION'));

SELECT 'check_13_duplicate_event_team' AS check_id, division_id, source_team_id, COUNT(*) AS cnt
FROM basket_event_team WHERE is_delete=0
GROUP BY division_id, source_team_id HAVING COUNT(*)>1;

SELECT 'check_14_duplicate_registration' AS check_id, division_id, team_id, COUNT(*) AS cnt
FROM basket_registration WHERE is_delete=0 AND registration_status='APPROVED'
GROUP BY division_id, team_id HAVING COUNT(*)>1;

SELECT 'check_15_event_team_no_player' AS check_id, COUNT(*) AS cnt
FROM basket_event_team et
WHERE et.is_delete=0 AND NOT EXISTS (
  SELECT 1 FROM basket_event_player ep WHERE ep.event_team_id=et.id AND ep.is_delete=0
);

SELECT 'check_16_approved_reg_no_event_team' AS check_id, COUNT(*) AS cnt
FROM basket_registration r
WHERE r.is_delete=0 AND r.registration_status='APPROVED'
  AND NOT EXISTS (
    SELECT 1 FROM basket_event_team et WHERE et.registration_id=r.id AND et.is_delete=0
  );

SELECT 'check_17_v4_legacy_template' AS check_id, COUNT(*) AS cnt
FROM basket_format_template WHERE template_code='V4_LEGACY_ROUND_ROBIN_V1' AND is_delete=0 AND enabled=1;

SELECT 'check_18_match_null_teams' AS check_id, COUNT(*) AS cnt
FROM basket_match
WHERE is_delete=0 AND (home_event_team_id IS NULL OR away_event_team_id IS NULL);

SELECT 'check_19_duplicate_match_pair' AS check_id, stage_id, LEAST(home_event_team_id, away_event_team_id) AS team_a,
       GREATEST(home_event_team_id, away_event_team_id) AS team_b, COUNT(*) AS cnt
FROM basket_match
WHERE is_delete=0 AND home_event_team_id IS NOT NULL AND away_event_team_id IS NOT NULL
GROUP BY stage_id, LEAST(home_event_team_id, away_event_team_id), GREATEST(home_event_team_id, away_event_team_id)
HAVING COUNT(*)>1;

SELECT 'check_20_duplicate_stage_team' AS check_id, stage_id, event_team_id, COUNT(*) AS cnt
FROM basket_division_stage_team WHERE is_delete=0
GROUP BY stage_id, event_team_id HAVING COUNT(*)>1;

SELECT 'check_21_duplicate_schedule_batch' AS check_id, stage_id, publish_status, COUNT(*) AS cnt
FROM basket_schedule_batch WHERE is_delete=0 AND publish_status IN ('DRAFT','PUBLISHED')
GROUP BY stage_id, publish_status HAVING COUNT(*)>1;
