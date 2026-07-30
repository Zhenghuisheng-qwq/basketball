-- CNPA Match Data Submission P0 rollback (drops P0 tables only; audit columns retained for safety)

DROP TABLE IF EXISTS basket_match_data_submission_material;
DROP TABLE IF EXISTS basket_match_data_submission_player_stat;
DROP TABLE IF EXISTS basket_match_data_submission_team_stat;
DROP TABLE IF EXISTS basket_match_data_submission_period;
DROP TABLE IF EXISTS basket_match_data_submission;

-- audit 扩展列如需回滚可手动执行：
-- ALTER TABLE basket_match_data_audit DROP COLUMN submission_id;
-- ALTER TABLE basket_match_data_audit DROP COLUMN audit_action;
-- ALTER TABLE basket_match_data_audit DROP COLUMN before_status;
-- ALTER TABLE basket_match_data_audit DROP COLUMN after_status;
-- ALTER TABLE basket_match_data_audit DROP COLUMN auditor_name;
