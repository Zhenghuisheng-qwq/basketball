-- CNPA Batch 5 before snapshot (2026-07-25)
-- Execute before docs/sql/cnpa-batch5-migration.sql

-- 比赛总量与数据状态分布
SELECT COUNT(*) AS total_matches FROM basket_match WHERE is_delete = 0;
SELECT data_status, COUNT(*) AS c FROM basket_match WHERE is_delete = 0 GROUP BY data_status ORDER BY data_status;
SELECT match_status, COUNT(*) AS c FROM basket_match WHERE is_delete = 0 GROUP BY match_status ORDER BY match_status;
SELECT COUNT(*) AS confirmed_matches FROM basket_match WHERE is_delete = 0 AND data_status = 2;

-- 比赛执行明细表
SELECT COUNT(*) AS match_roster FROM basket_match_roster WHERE is_delete = 0;
SELECT COUNT(*) AS match_team_stat FROM basket_match_team_stat WHERE is_delete = 0;
SELECT COUNT(*) AS match_player_stat FROM basket_match_player_stat WHERE is_delete = 0;
SELECT COUNT(*) AS match_status_log FROM basket_match_status_log;

-- 聚合统计表
SELECT COUNT(*) AS standings FROM basket_stage_standing WHERE is_delete = 0;
SELECT COUNT(*) AS team_season_stat FROM basket_team_season_stat WHERE is_delete = 0;
SELECT COUNT(*) AS player_season_stat FROM basket_player_season_stat WHERE is_delete = 0;
SELECT COUNT(*) AS team_rank FROM basket_team_rank WHERE is_delete = 0;
SELECT COUNT(*) AS player_rank FROM basket_player_rank WHERE is_delete = 0;
SELECT COUNT(*) AS stat_change_record FROM basket_stat_change_record WHERE is_delete = 0;

-- 第四批正式参赛（第五批名单快照依赖）
SELECT COUNT(*) AS event_teams FROM basket_team_season_profile WHERE is_delete = 0;
SELECT COUNT(*) AS season_rosters FROM basket_tournament_season_roster WHERE is_delete = 0;

-- 第五批新表（迁移前应不存在）
SELECT COUNT(*) AS official_result FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'basket_match_official_result';
SELECT COUNT(*) AS data_audit FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'basket_match_data_audit';
SELECT COUNT(*) AS data_dispute FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'basket_match_data_dispute';
SELECT COUNT(*) AS data_revision FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'basket_match_data_revision';
SELECT COUNT(*) AS settlement_batch FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'basket_data_settlement_batch';
SELECT COUNT(*) AS stat_sync_record FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'basket_stat_sync_record';
