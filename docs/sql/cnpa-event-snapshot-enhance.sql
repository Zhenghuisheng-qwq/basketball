-- CNPA EventTeam / EventPlayer 快照字段增强（幂等执行）
-- 为 basket_team_season_profile 补充队长/地区快照字段

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_team_season_profile' AND COLUMN_NAME = 'captain_grant_id') = 0,
    'ALTER TABLE basket_team_season_profile ADD COLUMN captain_grant_id bigint NULL COMMENT ''队长授权ID快照'' AFTER registration_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_team_season_profile' AND COLUMN_NAME = 'captain_name_snapshot') = 0,
    'ALTER TABLE basket_team_season_profile ADD COLUMN captain_name_snapshot varchar(128) NULL COMMENT ''队长名称快照'' AFTER captain_player_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_team_season_profile' AND COLUMN_NAME = 'team_region_snapshot') = 0,
    'ALTER TABLE basket_team_season_profile ADD COLUMN team_region_snapshot varchar(128) NULL COMMENT ''球队地区快照'' AFTER short_name_snapshot',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_team_season_profile' AND INDEX_NAME = 'idx_event_team_season_team') = 0,
    'ALTER TABLE basket_team_season_profile ADD KEY idx_event_team_season_team (season_id, team_id, is_delete)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'basket_tournament_season_roster' AND INDEX_NAME = 'idx_roster_event_team_player') = 0,
    'ALTER TABLE basket_tournament_season_roster ADD KEY idx_roster_event_team_player (event_team_id, player_id, is_delete)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
