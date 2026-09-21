-- =============================================================================
-- CNPA PRODUCTION BUSINESS DATA RESET
-- DO NOT RUN WITHOUT DATABASE BACKUP
-- =============================================================================
--
-- 目标：保留 Schema / 系统表 / 超管登录 / 赛制模板目录，清空全部业务数据。
--
-- 禁止：
--   DROP DATABASE
--   DROP TABLE
--   TRUNCATE system tables
--   重置 AUTO_INCREMENT
--
-- 目标库必须是：basketball
-- 执行前必须已有：cnpa-before-production-reset-YYYYMMDD-HHmmss.sql
--
-- information_schema.KEY_COLUMN_USAGE：当前库 0 条物理外键。
-- 仍按业务对象链从下游向上 DELETE。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- Stats / Standing / Rank / Submission / Match
-- ---------------------------------------------------------------------------
DELETE FROM basket_standing_rank_reason;
DELETE FROM basket_standing;
DELETE FROM basket_point_adjustment;
DELETE FROM basket_rank;
DELETE FROM basket_player_stat;
DELETE FROM basket_player_rank;
DELETE FROM basket_team_stat;
DELETE FROM basket_team_rank;
DELETE FROM basket_stat_change_record;
DELETE FROM basket_stat_sync_record;
DELETE FROM basket_match_player_stat;
DELETE FROM basket_match_team_stat;
DELETE FROM basket_match_period_score;
DELETE FROM basket_match_submission_player_stat;
DELETE FROM basket_match_submission_team_stat;
DELETE FROM basket_match_submission_period_score;
DELETE FROM basket_match_submission_material;
DELETE FROM basket_match_submission;
DELETE FROM basket_match_series_submission;
DELETE FROM basket_match;
DELETE FROM basket_match_series;

-- ---------------------------------------------------------------------------
-- Schedule / Pairing / Qualification / Stage / Group / Roster snapshot
-- ---------------------------------------------------------------------------
DELETE FROM basket_stage_pairing_entry;
DELETE FROM basket_stage_pairing_execution;
DELETE FROM basket_stage_publish_record;
DELETE FROM basket_schedule_batch;
DELETE FROM basket_division_stage_qualification;
DELETE FROM basket_division_stage_team;
DELETE FROM basket_division_tie_break_rule;
DELETE FROM basket_division_stage;
DELETE FROM basket_division_group_team;
DELETE FROM basket_division_group;
DELETE FROM basket_division_grouping_batch;
DELETE FROM basket_division_roster_entry;
DELETE FROM basket_division_roster_version;
DELETE FROM basket_division_format_snapshot;

-- ---------------------------------------------------------------------------
-- Event snapshot / Registration
-- ---------------------------------------------------------------------------
DELETE FROM basket_event_player;
DELETE FROM basket_event_team;
DELETE FROM basket_registration_player;
DELETE FROM basket_registration_audit;
DELETE FROM basket_registration;
DELETE FROM basket_personal_draw_assignment;
DELETE FROM basket_personal_draw_team;
DELETE FROM basket_personal_draw_batch;
DELETE FROM basket_personal_registration_audit;
DELETE FROM basket_personal_registration;

-- ---------------------------------------------------------------------------
-- Division / Season
-- ---------------------------------------------------------------------------
-- basket_series (LEAGUE/ARENA/CUP/OPEN) is SYSTEM_KEEP. Do not delete.
DELETE FROM basket_division;
DELETE FROM basket_season;

-- ---------------------------------------------------------------------------
-- Captain grant
-- ---------------------------------------------------------------------------
DELETE FROM basket_captain_grant_log;
DELETE FROM basket_captain_grant;

-- ---------------------------------------------------------------------------
-- Discipline
-- ---------------------------------------------------------------------------
DELETE FROM basket_discipline_execution_detail;
DELETE FROM basket_discipline_suspension_log;
DELETE FROM basket_discipline_restriction;
DELETE FROM basket_discipline_record;

-- ---------------------------------------------------------------------------
-- Team
-- ---------------------------------------------------------------------------
DELETE FROM basket_team_invitation;
DELETE FROM basket_team_join_application;
DELETE FROM basket_team_member;
DELETE FROM basket_team_audit_record;
DELETE FROM basket_team_captain_history;
DELETE FROM basket_team_profile_change;
DELETE FROM basket_player_team_relation;
DELETE FROM basket_team;

-- ---------------------------------------------------------------------------
-- Player
-- ---------------------------------------------------------------------------
DELETE FROM basket_player_platform_change_request;
DELETE FROM basket_player_platform_identity_history;
DELETE FROM basket_player_platform_account;
DELETE FROM basket_player_cert_audit;
DELETE FROM basket_player_cert_material;
DELETE FROM basket_player_cert_application;
DELETE FROM basket_player_profile;

-- ---------------------------------------------------------------------------
-- C-end member (Admin sys_user 不在此范围)
-- ---------------------------------------------------------------------------
DELETE FROM basket_member_profile_change;
DELETE FROM basket_member_agreement_record;
DELETE FROM basket_member_profile;
DELETE FROM basket_notification_setting;
DELETE FROM basket_privacy_setting;
DELETE FROM basket_favorite;
DELETE FROM basket_verify_code_record;
DELETE FROM basket_login_log;
DELETE FROM basket_account_security_log;
DELETE FROM basket_member_account;

-- ---------------------------------------------------------------------------
-- Content / files / messages / business audit
-- ---------------------------------------------------------------------------
DELETE FROM basket_announcement_attachment;
DELETE FROM basket_announcement_rule_link;
DELETE FROM basket_announcement;
DELETE FROM basket_home_banner;
DELETE FROM basket_partner_support_record;
DELETE FROM basket_partner;
DELETE FROM basket_honor_lineup;
DELETE FROM basket_honor_record;
DELETE FROM basket_honor_generation_batch;
DELETE FROM basket_honor_category;
DELETE FROM basket_file_asset;
DELETE FROM basket_venue;
DELETE FROM basket_message_receiver;
DELETE FROM basket_message_task;
DELETE FROM basket_business_audit_field;
DELETE FROM basket_business_audit;

SET FOREIGN_KEY_CHECKS = 1;

-- SYSTEM_KEEP（本脚本不删除）：
--   sys_user / sys_role / sys_menu / sys_role_menu / sys_user_role
--   sys_dept / sys_post / sys_user_post / sys_role_dept
--   sys_config / sys_dict_type / sys_dict_data
--   sys_job / sys_job_log / sys_logininfor / sys_oper_log
--   sys_notice / sys_notice_read
--   gen_table / gen_table_column
--   basket_format_template / basket_format_template_stage / basket_format_template_tie_break_rule
--   basket_site_config
--   basket_site_social_link
--   basket_series (LEAGUE / ARENA / CUP / OPEN)

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

-- Rebind STANDARD templates by series_code. Do not null series_id. Global CUSTOM templates stay unbound.
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

-- ---------------------------------------------------------------------------
-- SYSTEM_KEEP site social links. Idempotent seed, do not DELETE.
-- ---------------------------------------------------------------------------
INSERT INTO basket_site_social_link (platform_code, title, subtitle, button_text, jump_url, sort_order, status, is_delete)
SELECT 'KOOK', '加入 KOOK', '官方赛事社区', '加入社区 →', NULL, 10, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_site_social_link WHERE platform_code = 'KOOK');

INSERT INTO basket_site_social_link (platform_code, title, subtitle, button_text, jump_url, sort_order, status, is_delete)
SELECT 'WECHAT', '加入微信群', '球员与观众交流群', '加入群聊 →', NULL, 20, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_site_social_link WHERE platform_code = 'WECHAT');

INSERT INTO basket_site_social_link (platform_code, title, subtitle, button_text, jump_url, sort_order, status, is_delete)
SELECT 'BILIBILI', '关注 B站', '赛事直播与精彩内容', '访问主页 →', NULL, 30, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_site_social_link WHERE platform_code = 'BILIBILI');

INSERT INTO basket_site_social_link (platform_code, title, subtitle, button_text, jump_url, sort_order, status, is_delete)
SELECT 'DOUYIN', '关注抖音', '赛事短视频与即时动态', '立即关注 →', NULL, 40, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_site_social_link WHERE platform_code = 'DOUYIN');
