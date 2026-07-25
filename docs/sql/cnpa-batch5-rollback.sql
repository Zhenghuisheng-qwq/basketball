-- CNPA Batch5 rollback
ALTER TABLE basket_match_roster
    DROP COLUMN data_version,
    DROP COLUMN locked_time,
    DROP COLUMN roster_status,
    DROP COLUMN game_id_display_snapshot,
    DROP COLUMN platform_snapshot,
    DROP COLUMN player_nickname_snapshot,
    DROP COLUMN event_team_id,
    DROP COLUMN season_roster_id;

ALTER TABLE basket_match
    DROP COLUMN settlement_locked,
    DROP COLUMN origin_match_id,
    DROP COLUMN data_version,
    DROP COLUMN score_confirmed,
    DROP COLUMN result_status;

DROP TABLE IF EXISTS basket_stat_sync_record;
DROP TABLE IF EXISTS basket_data_settlement_batch;
DROP TABLE IF EXISTS basket_match_data_revision;
DROP TABLE IF EXISTS basket_match_data_dispute;
DROP TABLE IF EXISTS basket_match_data_audit;
DROP TABLE IF EXISTS basket_match_official_result;

DELETE FROM sys_role_menu WHERE menu_id IN (2230, 2231, 2232, 2233);
DELETE FROM sys_menu WHERE menu_id IN (2230, 2231, 2232, 2233);
