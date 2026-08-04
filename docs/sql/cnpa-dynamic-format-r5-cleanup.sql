-- =============================================================================
-- CNPA 动态赛制重构 R5：开发库旧赛事域清理
-- ⚠️ 警告：破坏性脚本，仅允许在开发/测试库执行
-- ⚠️ 执行前必须：mysqldump 全库备份 或 至少备份 basket_* 与 sys_menu
-- ⚠️ 禁止在生产库自动执行
-- 事务：MySQL DDL 部分语句隐式提交；数据 DELETE 建议分段执行并记录行数
-- =============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 0. 执行前快照（行数）
-- ---------------------------------------------------------------------------
SELECT 'precheck_counts' AS step,
  (SELECT COUNT(*) FROM basket_tournament_registration) AS reg,
  (SELECT COUNT(*) FROM basket_team_season_profile) AS tsp,
  (SELECT COUNT(*) FROM basket_match WHERE stage_id IS NULL OR stage_id=0) AS legacy_match;

-- ---------------------------------------------------------------------------
-- 1. 清旧菜单角色关联
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_menu
WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE menu_id IN (2000,2004,2005,2006,2007,2017,2018,2019,2020,2021,2022,
      2035,2036,2037,2040,2041,2042,2043,2060,2061,2062,2063,2064,2065,2066,2067,
      2081,2088,2089,2090,2154,2204,2205,2230,2231,2232,2250)
       OR component LIKE 'cnpa/event/%'
       OR component LIKE 'cnpa/competition/%'
       OR component LIKE 'cnpa/statistics/%'
       OR component LIKE 'cnpa/participation/%'
       OR (parent_id = 2000 AND menu_type IN ('C','F'))
       OR (parent_id = 2154)
       OR (parent_id = 2205 AND menu_type = 'C')
  ) t
);

-- ---------------------------------------------------------------------------
-- 2. 清旧菜单权限（物理删除废弃菜单项）
-- ---------------------------------------------------------------------------
DELETE FROM sys_menu
WHERE menu_id IN (2000,2004,2005,2006,2007,2017,2018,2019,2020,2021,2022,
  2035,2036,2037,2040,2041,2042,2043,2060,2061,2062,2063,2064,2065,2066,2067,
  2081,2088,2089,2090,2154,2204,2205,2230,2231,2232,2250)
   OR component LIKE 'cnpa/event/%'
   OR component LIKE 'cnpa/competition/%'
   OR component LIKE 'cnpa/statistics/%'
   OR component LIKE 'cnpa/participation/%'
   OR (parent_id = 2000 AND menu_type IN ('C','F'))
   OR (parent_id = 2154)
   OR (parent_id = 2205 AND menu_type = 'C');

-- 隐藏空 legacy 父级
UPDATE sys_menu SET visible='1', status='0', remark=CONCAT(IFNULL(remark,''),' [R5 removed]')
WHERE menu_id IN (2000,2154,2204,2205);

-- ---------------------------------------------------------------------------
-- 3. 清共享表 Legacy 行（无 stage_id / 无新 registration_source）
-- ---------------------------------------------------------------------------
-- 3.1 旧比赛提交与统计（legacy match 关联）
DELETE ms FROM basket_match_submission_player_stat ms
INNER JOIN basket_match_submission s ON s.id = ms.submission_id
INNER JOIN basket_match m ON m.id = s.match_id
WHERE m.stage_id IS NULL OR m.stage_id = 0;

DELETE ms FROM basket_match_submission_team_stat ms
INNER JOIN basket_match_submission s ON s.id = ms.submission_id
INNER JOIN basket_match m ON m.id = s.match_id
WHERE m.stage_id IS NULL OR m.stage_id = 0;

DELETE mm FROM basket_match_submission_material mm
INNER JOIN basket_match_submission s ON s.id = mm.submission_id
INNER JOIN basket_match m ON m.id = s.match_id
WHERE m.stage_id IS NULL OR m.stage_id = 0;

DELETE s FROM basket_match_submission s
INNER JOIN basket_match m ON m.id = s.match_id
WHERE m.stage_id IS NULL OR m.stage_id = 0;

DELETE FROM basket_match WHERE stage_id IS NULL OR stage_id = 0;
DELETE FROM basket_schedule_batch WHERE stage_id IS NULL OR stage_id = 0;
DELETE FROM basket_standing WHERE stage_id IS NULL OR stage_id = 0;

-- 3.2 无快照 Division 及其关联新域数据
DELETE ep FROM basket_event_player ep
INNER JOIN basket_event_team et ON et.id = ep.event_team_id
INNER JOIN basket_division d ON d.id = et.division_id
WHERE d.format_snapshot_id IS NULL;

DELETE FROM basket_event_team et
USING basket_event_team et
INNER JOIN basket_division d ON d.id = et.division_id
WHERE d.format_snapshot_id IS NULL;

DELETE rp FROM basket_registration_player rp
INNER JOIN basket_registration r ON r.id = rp.registration_id
INNER JOIN basket_division d ON d.id = r.division_id
WHERE d.format_snapshot_id IS NULL;

DELETE ra FROM basket_registration_audit ra
INNER JOIN basket_registration r ON r.id = ra.registration_id
INNER JOIN basket_division d ON d.id = r.division_id
WHERE d.format_snapshot_id IS NULL;

DELETE r FROM basket_registration r
INNER JOIN basket_division d ON d.id = r.division_id
WHERE d.format_snapshot_id IS NULL;

DELETE cg FROM basket_captain_grant cg
INNER JOIN basket_division d ON d.id = cg.division_id
WHERE d.format_snapshot_id IS NULL;

DELETE FROM basket_division WHERE format_snapshot_id IS NULL;

-- 3.3 Legacy registration 行
DELETE FROM basket_registration_player WHERE registration_id IN (
  SELECT id FROM (SELECT id FROM basket_registration
    WHERE registration_source IS NULL OR registration_source NOT IN ('TEAM_DIRECT','PERSONAL_FORMATION')) t
);
DELETE FROM basket_registration_audit WHERE registration_id IN (
  SELECT id FROM (SELECT id FROM basket_registration
    WHERE registration_source IS NULL OR registration_source NOT IN ('TEAM_DIRECT','PERSONAL_FORMATION')) t
);
DELETE FROM basket_registration
WHERE registration_source IS NULL OR registration_source NOT IN ('TEAM_DIRECT','PERSONAL_FORMATION');

-- ---------------------------------------------------------------------------
-- 4. 清纯旧子表数据
-- ---------------------------------------------------------------------------
TRUNCATE TABLE basket_tournament_registration_player;
TRUNCATE TABLE basket_tournament_registration_audit;
TRUNCATE TABLE basket_tournament_registration;
TRUNCATE TABLE basket_tournament_season_roster;
TRUNCATE TABLE basket_tournament_open_registration_audit;
TRUNCATE TABLE basket_tournament_open_registration;
TRUNCATE TABLE basket_tournament_roster_change_record;
TRUNCATE TABLE basket_tournament_roster_change_request;
TRUNCATE TABLE basket_tournament_roster_operation_log;
TRUNCATE TABLE basket_tournament_group_team;
TRUNCATE TABLE basket_tournament_group;
TRUNCATE TABLE basket_tournament_round;
TRUNCATE TABLE basket_tournament_rule;
TRUNCATE TABLE basket_tournament_stage;
TRUNCATE TABLE basket_tournament_award_config;
TRUNCATE TABLE basket_tournament_season_platform;
TRUNCATE TABLE basket_team_season_profile;
TRUNCATE TABLE basket_captain_qualification;
TRUNCATE TABLE basket_competition_season;
TRUNCATE TABLE basket_schedule_generate_batch;
TRUNCATE TABLE basket_stage_standing;
TRUNCATE TABLE basket_team_season_stat;
TRUNCATE TABLE basket_player_season_stat;
TRUNCATE TABLE basket_data_settlement_batch;

-- 旧比赛数据域
TRUNCATE TABLE basket_match_data_submission_player_stat;
TRUNCATE TABLE basket_match_data_submission_team_stat;
TRUNCATE TABLE basket_match_data_submission_period;
TRUNCATE TABLE basket_match_data_submission_material;
TRUNCATE TABLE basket_match_data_submission;
TRUNCATE TABLE basket_match_data_audit;
TRUNCATE TABLE basket_match_data_dispute_material;
TRUNCATE TABLE basket_match_data_dispute;
TRUNCATE TABLE basket_match_data_revision;
TRUNCATE TABLE basket_match_exception_material;
TRUNCATE TABLE basket_match_exception_audit;
TRUNCATE TABLE basket_match_exception_application;
TRUNCATE TABLE basket_match_roster;
TRUNCATE TABLE basket_match_team;
TRUNCATE TABLE basket_match_official_result;
TRUNCATE TABLE basket_match_period_score;
TRUNCATE TABLE basket_match_status_log;
TRUNCATE TABLE basket_match_player_stat;
TRUNCATE TABLE basket_match_team_stat;

-- schedule rule
TRUNCATE TABLE basket_schedule_rule_weekday;
TRUNCATE TABLE basket_schedule_rule_time_slot;
TRUNCATE TABLE basket_schedule_rule;

-- ---------------------------------------------------------------------------
-- 5. DROP 纯旧表（保留 basket_tournament / basket_tournament_season / admin_scope 供荣誉纪律）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS basket_tournament_registration_player;
DROP TABLE IF EXISTS basket_tournament_registration_audit;
DROP TABLE IF EXISTS basket_tournament_registration;
DROP TABLE IF EXISTS basket_tournament_season_roster;
DROP TABLE IF EXISTS basket_tournament_open_registration_audit;
DROP TABLE IF EXISTS basket_tournament_open_registration;
DROP TABLE IF EXISTS basket_tournament_roster_change_record;
DROP TABLE IF EXISTS basket_tournament_roster_change_request;
DROP TABLE IF EXISTS basket_tournament_roster_operation_log;
DROP TABLE IF EXISTS basket_tournament_group_team;
DROP TABLE IF EXISTS basket_tournament_group;
DROP TABLE IF EXISTS basket_tournament_round;
DROP TABLE IF EXISTS basket_tournament_rule;
DROP TABLE IF EXISTS basket_tournament_stage;
DROP TABLE IF EXISTS basket_tournament_award_config;
DROP TABLE IF EXISTS basket_tournament_season_platform;
DROP TABLE IF EXISTS basket_team_season_profile;
DROP TABLE IF EXISTS basket_captain_qualification;
DROP TABLE IF EXISTS basket_competition_season;
DROP TABLE IF EXISTS basket_schedule_generate_batch;
DROP TABLE IF EXISTS basket_schedule_rule_weekday;
DROP TABLE IF EXISTS basket_schedule_rule_time_slot;
DROP TABLE IF EXISTS basket_schedule_rule;
DROP TABLE IF EXISTS basket_stage_standing;
DROP TABLE IF EXISTS basket_team_season_stat;
DROP TABLE IF EXISTS basket_player_season_stat;
DROP TABLE IF EXISTS basket_data_settlement_batch;
DROP TABLE IF EXISTS basket_match_data_submission_player_stat;
DROP TABLE IF EXISTS basket_match_data_submission_team_stat;
DROP TABLE IF EXISTS basket_match_data_submission_period;
DROP TABLE IF EXISTS basket_match_data_submission_material;
DROP TABLE IF EXISTS basket_match_data_submission;
DROP TABLE IF EXISTS basket_match_data_audit;
DROP TABLE IF EXISTS basket_match_data_dispute_material;
DROP TABLE IF EXISTS basket_match_data_dispute;
DROP TABLE IF EXISTS basket_match_data_revision;
DROP TABLE IF EXISTS basket_match_exception_material;
DROP TABLE IF EXISTS basket_match_exception_audit;
DROP TABLE IF EXISTS basket_match_exception_application;
DROP TABLE IF EXISTS basket_match_roster;
DROP TABLE IF EXISTS basket_match_team;
DROP TABLE IF EXISTS basket_match_official_result;
DROP TABLE IF EXISTS basket_match_period_score;
DROP TABLE IF EXISTS basket_match_status_log;

-- ---------------------------------------------------------------------------
-- 6. 删除废弃字段 rank_explain_json
-- ---------------------------------------------------------------------------
ALTER TABLE basket_standing DROP COLUMN IF EXISTS rank_explain_json;

-- ---------------------------------------------------------------------------
-- 7. 收紧新主线字段（Legacy 行已清空后）
-- ---------------------------------------------------------------------------
-- basket_division: series_id / format 字段（DRAFT 创建流程允许暂缺 snapshot，不设 NOT NULL）
ALTER TABLE basket_division
  MODIFY COLUMN platform_code VARCHAR(32) NOT NULL COMMENT '平台：PC/PS_XBOX（历史 PS/XBOX 只读）',
  MODIFY COLUMN game_mode VARCHAR(32) NOT NULL COMMENT '比赛模式：FIVE_V_FIVE/THREE_V_THREE',
  MODIFY COLUMN registration_mode VARCHAR(32) NOT NULL COMMENT '报名方式：TEAM/PERSONAL';

ALTER TABLE basket_registration
  MODIFY COLUMN registration_source VARCHAR(32) NOT NULL COMMENT 'TEAM_DIRECT/PERSONAL_FORMATION';

ALTER TABLE basket_match
  MODIFY COLUMN stage_id BIGINT NOT NULL COMMENT '阶段ID',
  MODIFY COLUMN division_id BIGINT NOT NULL COMMENT '赛区ID';

ALTER TABLE basket_schedule_batch
  MODIFY COLUMN stage_id BIGINT NOT NULL COMMENT '阶段ID',
  MODIFY COLUMN division_id BIGINT NOT NULL COMMENT '赛区ID';

ALTER TABLE basket_standing
  MODIFY COLUMN stage_id BIGINT NOT NULL COMMENT '阶段ID',
  MODIFY COLUMN group_scope_key BIGINT NOT NULL DEFAULT 0 COMMENT '分组范围键';

-- ---------------------------------------------------------------------------
-- 8. 删除 V4_LEGACY 迁移模板种子（若存在）
-- ---------------------------------------------------------------------------
UPDATE basket_format_template SET is_delete=1, enabled=0
WHERE template_code='V4_LEGACY_ROUND_ROBIN_V1' AND is_delete=0;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'cleanup_done' AS step, NOW() AS finished_at;
