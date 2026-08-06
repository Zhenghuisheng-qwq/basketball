-- CNPA V2 remove legacy objects
-- Local/dev only. Do NOT run on production without review.
-- No DROP DATABASE. No passwords embedded.

SET NAMES utf8mb4;

-- ========== 1) Drop confirmed obsolete event tables (no Java/Mapper refs) ==========
-- Evidence: no runtime Mapper/Entity reference; R5 already removed Tournament domain code.
DROP TABLE IF EXISTS basket_tournament_admin_scope;
DROP TABLE IF EXISTS basket_tournament_season;
DROP TABLE IF EXISTS basket_tournament;

-- R4 migration backup tables (temporary)
DROP TABLE IF EXISTS bak_r4_basket_match_period_score;
DROP TABLE IF EXISTS bak_r4_basket_match_player_stat;
DROP TABLE IF EXISTS bak_r4_basket_match_team_stat;
DROP TABLE IF EXISTS bak_r4_basket_match;

-- Temporary marker table
DROP TABLE IF EXISTS basket_r4_test_marker;

-- ========== 2) Honor record column rename: tournament_id -> series_id ==========
-- Code/Mapper already use series_id; local DB still has tournament_id.
-- Safe rename only when series_id missing and tournament_id present.
SET @sql := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='basket_honor_record' AND COLUMN_NAME='tournament_id')
    AND NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='basket_honor_record' AND COLUMN_NAME='series_id'),
    'ALTER TABLE basket_honor_record CHANGE COLUMN tournament_id series_id BIGINT NULL',
    'SELECT ''skip honor tournament_id rename'' AS info'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 3) Remove obsolete menus / perms (broken components & orphan parents) ==========
-- captain-qualification page missing; replaced by event-v4 captain-grant
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu WHERE menu_id IN (2002,2011,2012,2013,2014)
      OR parent_id IN (2002)
      OR perms LIKE 'basket:captainQualification%'
  ) x
);
DELETE FROM sys_menu WHERE menu_id IN (2002,2011,2012,2013,2014)
  OR parent_id IN (2002)
  OR perms LIKE 'basket:captainQualification%';

-- tournament-scope page missing
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu WHERE menu_id=2244 OR perms LIKE 'basket:tournamentScope%'
  ) x
);
DELETE FROM sys_menu WHERE menu_id=2244 OR perms LIKE 'basket:tournamentScope%';

-- orphan old tournament/season/rule button menus (parents 2004/2005/2006 already gone)
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE parent_id IN (2004,2005,2006)
       OR perms LIKE 'basket:tournament:%'
       OR perms LIKE 'basket:tournamentRule:%'
       OR (perms LIKE 'basket:season:%' AND perms NOT LIKE 'basket:seasonResult:%' AND menu_id < 2200)
  ) x
);
DELETE FROM sys_menu
WHERE parent_id IN (2004,2005,2006)
   OR perms LIKE 'basket:tournament:%'
   OR perms LIKE 'basket:tournamentRule:%'
   OR (perms LIKE 'basket:season:%' AND perms NOT LIKE 'basket:seasonResult:%' AND menu_id < 2200);

-- empty legacy top menus without useful children
DELETE FROM sys_role_menu WHERE menu_id IN (2000,2204,2205,2154);
DELETE FROM sys_menu WHERE menu_id IN (2000,2204,2205,2154)
  AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id IN (2000,2204,2205,2154));

-- seasonResult button perms without parent page (legacy)
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu WHERE perms LIKE 'basket:seasonResult:%'
  ) x
);
DELETE FROM sys_menu WHERE perms LIKE 'basket:seasonResult:%';

-- ========== 4) Notes ==========
-- KEEP announcement/business_audit.tournament_id (runtime series alias column names).
-- KEEP sys_job sample tasks (RuoYi defaults).
-- Test business data cleanup is executed by companion Python FK-order cleanup.
