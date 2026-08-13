-- CNPA V1.3 R7 schedule / publish data check (read-only)
SET NAMES utf8mb4;

-- 1. BO>1 series schedule date/time must be paired
SELECT 'SERIES_DATE_TIME_PAIR' AS check_code, COUNT(*) AS cnt
FROM basket_match_series
WHERE is_delete = 0
  AND ((scheduled_date IS NULL) XOR (scheduled_start_time IS NULL));

-- 2. scheduled date without time (same as 1, explicit)
SELECT 'SERIES_DATE_WITHOUT_TIME' AS check_code, COUNT(*) AS cnt
FROM basket_match_series
WHERE is_delete = 0 AND scheduled_date IS NOT NULL AND scheduled_start_time IS NULL;

-- 3. Child matches should not carry an independent schedule different from series
SELECT 'CHILD_MATCH_INDEPENDENT_SCHEDULE' AS check_code, COUNT(*) AS cnt
FROM basket_match m
JOIN basket_match_series s ON s.id = m.series_id AND s.is_delete = 0
WHERE m.is_delete = 0
  AND m.series_id IS NOT NULL
  AND m.match_time IS NOT NULL
  AND s.scheduled_date IS NOT NULL
  AND (
    DATE(m.match_time) <> s.scheduled_date
    OR TIME(m.match_time) <> s.scheduled_start_time
  );

-- 4. PUBLISHED stage required units are scheduled
SELECT 'PUBLISHED_UNSCHEDULED_SERIES' AS check_code, COUNT(*) AS cnt
FROM basket_schedule_batch b
JOIN basket_match_series s ON s.schedule_batch_id = b.id AND s.is_delete = 0
WHERE b.is_delete = 0 AND b.publish_status = 'PUBLISHED' AND b.structure_status = 'ACTIVE'
  AND IFNULL(s.home_source_type,'') <> 'BYE' AND IFNULL(s.away_source_type,'') <> 'BYE'
  AND (s.scheduled_date IS NULL OR s.scheduled_start_time IS NULL);

SELECT 'PUBLISHED_UNSCHEDULED_BO1' AS check_code, COUNT(*) AS cnt
FROM basket_schedule_batch b
JOIN basket_match m ON m.schedule_batch_id = b.id AND m.is_delete = 0 AND m.series_id IS NULL
WHERE b.is_delete = 0 AND b.publish_status = 'PUBLISHED' AND b.structure_status = 'ACTIVE'
  AND m.match_time IS NULL;

-- 5. PUBLISHED structure is ACTIVE
SELECT 'PUBLISHED_STALE_STRUCTURE' AS check_code, COUNT(*) AS cnt
FROM basket_schedule_batch
WHERE is_delete = 0 AND publish_status = 'PUBLISHED' AND structure_status <> 'ACTIVE';

-- 6. PUBLISHED binding vs current locked versions (VERSIONED only)
SELECT 'PUBLISHED_RULE_MISMATCH' AS check_code, COUNT(*) AS cnt
FROM basket_schedule_batch b
JOIN basket_division_stage st ON st.id = b.stage_id AND st.is_delete = 0
JOIN basket_division_format_snapshot snap ON snap.id = st.snapshot_id AND snap.is_delete = 0
WHERE b.is_delete = 0 AND b.publish_status = 'PUBLISHED'
  AND snap.roster_workflow_mode = 'VERSIONED'
  AND snap.snapshot_status = 'LOCKED'
  AND (b.rule_version_id IS NULL OR b.rule_version_id <> snap.id);

-- 7. One current publish record per stage
SELECT 'MULTI_CURRENT_PUBLISH' AS check_code, COUNT(*) AS cnt
FROM (
  SELECT stage_id, COUNT(*) AS c
  FROM basket_stage_publish_record
  WHERE is_delete = 0 AND is_current = 1
  GROUP BY stage_id
  HAVING COUNT(*) > 1
) x;

-- 8. Publish no unique (constraint exists; list duplicates if any)
SELECT 'DUPLICATE_PUBLISH_NO' AS check_code, COUNT(*) AS cnt
FROM (
  SELECT stage_id, publish_no, action_type, COUNT(*) AS c
  FROM basket_stage_publish_record
  WHERE is_delete = 0
  GROUP BY stage_id, publish_no, action_type
  HAVING COUNT(*) > 1
) x;

-- 9. Withdraw reason required
SELECT 'WITHDRAW_EMPTY_REASON' AS check_code, COUNT(*) AS cnt
FROM basket_stage_publish_record
WHERE is_delete = 0 AND action_type = 'WITHDRAW'
  AND (reason IS NULL OR TRIM(reason) = '');

-- 10. Stale cannot be published (same as 5)
SELECT 'STALE_PUBLISHED' AS check_code, COUNT(*) AS cnt
FROM basket_schedule_batch
WHERE is_delete = 0 AND structure_status = 'STALE' AND publish_status = 'PUBLISHED';

-- 11. Schema presence
SELECT 'SCHEDULE_REVISION_COL' AS check_code, COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_schedule_batch' AND COLUMN_NAME = 'schedule_revision';

SELECT 'PUBLISH_RECORD_TABLE' AS check_code, COUNT(*) AS cnt
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_stage_publish_record';

SELECT 'R4_TEMPLATE_TYPE_COL' AS check_code, COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_format_template' AND COLUMN_NAME = 'template_type';

SELECT template_code, template_type, default_registration_mode, enabled, is_default
FROM basket_format_template
WHERE template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND version_no = 1 AND is_delete = 0
ORDER BY template_code;

SELECT 'LOCKED_SNAPSHOTS' AS check_code, COUNT(*) AS cnt
FROM basket_division_format_snapshot
WHERE is_delete = 0 AND snapshot_status = 'LOCKED';
