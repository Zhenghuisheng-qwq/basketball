-- CNPA Match Data Dispute & Exception P1 rollback
-- Only drops/additions from P1 migration; does NOT delete historical dispute rows

DROP TABLE IF EXISTS basket_match_exception_audit;
DROP TABLE IF EXISTS basket_match_exception_material;
DROP TABLE IF EXISTS basket_match_exception_application;
DROP TABLE IF EXISTS basket_match_data_dispute_material;

SET @db := DATABASE();

-- Drop uk_match_active_dispute
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND INDEX_NAME = 'uk_match_active_dispute') > 0,
    'ALTER TABLE basket_match_data_dispute DROP INDEX uk_match_active_dispute',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop active_dispute_slot
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_match_data_dispute' AND COLUMN_NAME = 'active_dispute_slot') > 0,
    'ALTER TABLE basket_match_data_dispute DROP COLUMN active_dispute_slot',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop added columns (safe if exist)
SET @cols := 'correction_type,reject_reason,process_result,processed_time,processed_by_name,processed_by,dispute_deadline_time,initiator_team_id,initiator_player_id,initiator_member_id,submission_id';
-- Note: MySQL does not support DROP MULTIPLE in dynamic easily; drop one by one
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='correction_type')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN correction_type','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='reject_reason')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN reject_reason','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='process_result')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN process_result','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='processed_time')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN processed_time','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='processed_by_name')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN processed_by_name','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='processed_by')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN processed_by','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='dispute_deadline_time')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN dispute_deadline_time','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='initiator_team_id')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN initiator_team_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='initiator_player_id')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN initiator_player_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='initiator_member_id')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN initiator_member_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='basket_match_data_dispute' AND COLUMN_NAME='submission_id')>0,'ALTER TABLE basket_match_data_dispute DROP COLUMN submission_id','SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
