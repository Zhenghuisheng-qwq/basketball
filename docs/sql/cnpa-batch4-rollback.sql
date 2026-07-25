-- CNPA Batch 4 rollback (local dev)
-- Order: drop new tables, then drop added columns

DROP TABLE IF EXISTS basket_tournament_roster_operation_log;
DROP TABLE IF EXISTS basket_tournament_roster_change_record;
DROP TABLE IF EXISTS basket_tournament_roster_change_request;
DROP TABLE IF EXISTS basket_tournament_open_registration_audit;
DROP TABLE IF EXISTS basket_tournament_open_registration;
DROP TABLE IF EXISTS basket_tournament_season_roster;

ALTER TABLE basket_team_season_profile
    DROP INDEX uk_event_team_participation,
    DROP INDEX uk_event_team_no_season,
    DROP INDEX uk_event_team_registration,
    DROP COLUMN approved_time,
    DROP COLUMN roster_locked_time,
    DROP COLUMN roster_lock_status,
    DROP COLUMN participation_status,
    DROP COLUMN registration_source,
    DROP COLUMN game_terminal,
    DROP COLUMN description_snapshot,
    DROP COLUMN short_name_snapshot,
    DROP COLUMN team_name_snapshot,
    DROP COLUMN event_team_no,
    DROP COLUMN registration_id;

ALTER TABLE basket_tournament_season
    DROP COLUMN max_free_agent_add_per_team,
    DROP COLUMN free_agent_end_time,
    DROP COLUMN free_agent_start_time,
    DROP COLUMN free_agent_enabled,
    DROP COLUMN transfer_end_time,
    DROP COLUMN transfer_start_time,
    DROP COLUMN transfer_enabled,
    DROP COLUMN allow_open_registration,
    DROP COLUMN allow_team_registration;
