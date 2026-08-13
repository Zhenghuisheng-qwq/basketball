-- CNPA V1.3 R5 data check (read-only)
SET NAMES utf8mb4;

SELECT 'PAIRING_EXEC_TABLE' AS check_code, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'basket_stage_pairing_execution';

SELECT 'PAIRING_ENTRY_TABLE' AS check_code, COUNT(*) AS cnt
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'basket_stage_pairing_entry';

SELECT 'STAGE_SF_FINAL_BO_COLS' AS check_code, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'basket_division_stage'
  AND column_name IN ('semifinal_best_of', 'final_best_of');

SELECT 'BATCH_PAIRING_EXEC_COL' AS check_code, COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'basket_schedule_batch'
  AND column_name = 'pairing_execution_id';

SELECT t.template_code, s.stage_code, s.best_of, s.semifinal_best_of, s.final_best_of
FROM basket_format_template t
JOIN basket_format_template_stage s ON s.template_id = t.id AND s.is_delete = 0
WHERE t.template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND t.version_no = 1 AND t.is_delete = 0
ORDER BY t.template_code, s.stage_order;

SELECT 'LOCKED_SNAPSHOTS_UNTOUCHED' AS check_code, COUNT(*) AS cnt
FROM basket_division_format_snapshot
WHERE is_delete = 0;
