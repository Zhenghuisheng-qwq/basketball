-- =============================================================================
-- CNPA 动态赛制重构 R5：清理前只读检查（不得包含 DROP/DELETE/UPDATE）
-- 环境：开发库 / 预发布库；禁止在生产库直接执行 cleanup
-- =============================================================================
SET NAMES utf8mb4;

SELECT '=== 1. 纯旧赛事表列表与数据量 ===' AS section;
SELECT TABLE_NAME, TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'basket_tournament_registration', 'basket_tournament_registration_audit',
    'basket_tournament_registration_player', 'basket_tournament_season_roster',
    'basket_tournament_roster_change_record', 'basket_tournament_roster_change_request',
    'basket_tournament_roster_operation_log', 'basket_tournament_open_registration',
    'basket_tournament_open_registration_audit', 'basket_tournament_group',
    'basket_tournament_group_team', 'basket_tournament_round', 'basket_tournament_rule',
    'basket_tournament_stage', 'basket_tournament_award_config', 'basket_tournament_season_platform',
    'basket_team_season_profile', 'basket_captain_qualification', 'basket_competition_season',
    'basket_schedule_generate_batch', 'basket_schedule_rule', 'basket_schedule_rule_time_slot',
    'basket_schedule_rule_weekday', 'basket_stage_standing', 'basket_team_season_stat',
    'basket_player_season_stat', 'basket_data_settlement_batch',
    'basket_match_data_submission', 'basket_match_data_submission_player_stat',
    'basket_match_data_submission_team_stat', 'basket_match_data_submission_period',
    'basket_match_data_submission_material', 'basket_match_data_audit',
    'basket_match_data_dispute', 'basket_match_data_dispute_material',
    'basket_match_data_revision', 'basket_match_exception_application',
    'basket_match_exception_audit', 'basket_match_exception_material',
    'basket_match_roster', 'basket_match_team', 'basket_match_official_result',
    'basket_match_period_score', 'basket_match_status_log'
  )
ORDER BY TABLE_NAME;

SELECT '=== 2. 荣誉/纪律保留表（不 DROP）===' AS section;
SELECT TABLE_NAME, TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('basket_tournament', 'basket_tournament_season', 'basket_tournament_admin_scope')
ORDER BY TABLE_NAME;

SELECT '=== 3. 无快照 Division ===' AS section;
SELECT d.id, d.season_id, d.platform_code, d.division_status, d.format_snapshot_id, d.create_time
FROM basket_division d
WHERE d.is_delete = 0 AND d.format_snapshot_id IS NULL;

SELECT '=== 4. Legacy ScheduleBatch（stage_id 为空）===' AS section;
SELECT id, division_id, stage_id, batch_no, publish_status, create_time
FROM basket_schedule_batch
WHERE is_delete = 0 AND (stage_id IS NULL OR stage_id = 0);

SELECT '=== 5. Legacy Match（stage_id 为空）===' AS section;
SELECT id, division_id, stage_id, schedule_batch_id, match_no, match_status
FROM basket_match
WHERE is_delete = 0 AND (stage_id IS NULL OR stage_id = 0);

SELECT '=== 6. Legacy Standing（stage_id 为空）===' AS section;
SELECT id, division_id, stage_id, event_team_id, rank_no, active_unique_key
FROM basket_standing
WHERE is_delete = 0 AND (stage_id IS NULL OR stage_id = 0);

SELECT '=== 7. Legacy Registration（registration_source 为空或非新值）===' AS section;
SELECT id, division_id, registration_source, registration_status, team_id
FROM basket_registration
WHERE is_delete = 0
  AND (registration_source IS NULL
       OR registration_source NOT IN ('TEAM_DIRECT', 'PERSONAL_FORMATION'));

SELECT '=== 8. rank_explain_json 非空 ===' AS section;
SELECT COUNT(*) AS rank_explain_json_non_empty
FROM basket_standing
WHERE is_delete = 0 AND rank_explain_json IS NOT NULL AND rank_explain_json <> '';

SELECT '=== 9. 旧赛事菜单（visible=1 或旧 component）===' AS section;
SELECT menu_id, menu_name, parent_id, path, component, perms, visible, status
FROM sys_menu
WHERE menu_id IN (2000, 2004, 2005, 2006, 2007, 2040, 2081, 2154, 2204, 2205)
   OR component LIKE 'cnpa/event/%'
   OR component LIKE 'cnpa/competition/%'
   OR component LIKE 'cnpa/statistics/%'
   OR component LIKE 'cnpa/participation/%'
   OR perms LIKE 'basket:tournament:%'
   OR perms LIKE 'basket:standing:%'
   OR perms LIKE 'basket:registrationAudit:%'
ORDER BY menu_id;

SELECT '=== 10. 旧赛事权限按钮 ===' AS section;
SELECT menu_id, menu_name, perms
FROM sys_menu
WHERE perms IN (
  'basket:tournament:list', 'basket:tournament:add', 'basket:tournament:edit',
  'basket:match:list', 'basket:match:schedule', 'basket:standing:list',
  'basket:standing:rebuild', 'basket:registrationAudit:list',
  'basket:dataSettlement:list', 'basket:ranking:list'
)
ORDER BY menu_id;

SELECT '=== 13. 旧赛事 Quartz Job（invoke_target 指向已删除类）===' AS section;
SELECT job_id, job_name, job_group, invoke_target, status, cron_expression
FROM sys_job
WHERE invoke_target LIKE '%ScheduleAutoGenerateJob%'
   OR invoke_target LIKE '%MatchStatusAutoFinishJob%'
   OR invoke_target LIKE '%BasketV4FormatSnapshotMigrationRunner%'
   OR invoke_target LIKE '%basket%Tournament%Job%'
   OR invoke_target LIKE '%ScheduleGenerate%'
ORDER BY job_id;

SELECT '=== 14. PS/XBOX 作为 Division platform_code ===' AS section;
SELECT id, season_id, platform_code, division_name
FROM basket_division
WHERE is_delete = 0 AND platform_code IN ('PS', 'XBOX', 'PS5');

SELECT '=== 12. 共享表 Legacy 行汇总 ===' AS section;
SELECT
  (SELECT COUNT(*) FROM basket_schedule_batch WHERE is_delete=0 AND (stage_id IS NULL OR stage_id=0)) AS legacy_batch,
  (SELECT COUNT(*) FROM basket_match WHERE is_delete=0 AND (stage_id IS NULL OR stage_id=0)) AS legacy_match,
  (SELECT COUNT(*) FROM basket_standing WHERE is_delete=0 AND (stage_id IS NULL OR stage_id=0)) AS legacy_standing,
  (SELECT COUNT(*) FROM basket_registration WHERE is_delete=0 AND (registration_source IS NULL OR registration_source NOT IN ('TEAM_DIRECT','PERSONAL_FORMATION'))) AS legacy_reg;
