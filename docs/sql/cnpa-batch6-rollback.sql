-- CNPA Batch6 rollback
DROP TABLE IF EXISTS basket_honor_generation_batch;
DROP TABLE IF EXISTS basket_announcement_rule_link;
DROP TABLE IF EXISTS basket_tournament_admin_scope;
DROP TABLE IF EXISTS basket_team_profile_change;
DROP TABLE IF EXISTS basket_player_team_relation;
DROP TABLE IF EXISTS basket_business_audit_field;
DROP TABLE IF EXISTS basket_business_audit;
DROP TABLE IF EXISTS basket_point_adjustment;
DROP TABLE IF EXISTS basket_discipline_suspension_log;
DROP TABLE IF EXISTS basket_discipline_restriction;

ALTER TABLE basket_honor_record DROP COLUMN generation_batch_id;
ALTER TABLE basket_announcement
    DROP COLUMN rule_version,
    DROP COLUMN superseded_by,
    DROP COLUMN scheduled_publish_time,
    DROP COLUMN season_id,
    DROP COLUMN tournament_id;

ALTER TABLE basket_discipline_record
    DROP INDEX uk_discipline_idempotency,
    DROP COLUMN idempotency_key,
    DROP COLUMN source_type,
    DROP COLUMN revoke_reason,
    DROP COLUMN revoked_time,
    DROP COLUMN revoked_by,
    DROP COLUMN approved_time,
    DROP COLUMN approved_by,
    DROP COLUMN record_status,
    DROP COLUMN point_deduction,
    DROP COLUMN match_suspension_remaining,
    DROP COLUMN match_suspension_count,
    DROP COLUMN penalty_code,
    DROP COLUMN event_team_id,
    DROP COLUMN target_type;

DELETE FROM sys_role_menu WHERE menu_id IN (2240, 2241, 2242, 2243, 2244, 2245, 2246, 2247, 2248);
DELETE FROM sys_menu WHERE menu_id IN (2240, 2241, 2242, 2243, 2244, 2245, 2246, 2247, 2248);
UPDATE sys_menu SET perms = '' WHERE menu_id = 2201;
