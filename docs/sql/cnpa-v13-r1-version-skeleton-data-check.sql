-- CNPA V1.3 R1 版本骨架迁移后检查
SET NAMES utf8mb4;

SELECT 'roster_version_table' AS check_name,
       COUNT(*) AS ok
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'basket_division_roster_version';

SELECT 'event_team_roster_col' AS check_name,
       COUNT(*) AS ok
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'basket_event_team'
  AND column_name = 'roster_version_id';

SELECT 'schedule_batch_version_cols' AS check_name,
       COUNT(*) AS ok
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'basket_schedule_batch'
  AND column_name IN ('rule_version_id', 'roster_version_id', 'structure_status');
