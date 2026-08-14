-- CNPA V1.3 R9 final data-check (read-only). P0 queries should return 0 rows unless noted.
SET NAMES utf8mb4;

-- P0-1 duplicate current roster
SELECT 'P0_DUP_CURRENT_ROSTER' AS check_code, division_id, COUNT(*) AS cnt
FROM basket_division_roster_version
WHERE is_delete = 0 AND is_current = 1
GROUP BY division_id
HAVING COUNT(*) > 1;

-- P0-2 duplicate current pairing execution
SELECT 'P0_DUP_CURRENT_PAIRING' AS check_code, stage_id, COUNT(*) AS cnt
FROM basket_stage_pairing_execution
WHERE is_delete = 0 AND is_current = 1
GROUP BY stage_id
HAVING COUNT(*) > 1;

-- P0-3 PUBLISHED STALE structure
SELECT 'P0_PUBLISHED_STALE' AS check_code, id, division_id, stage_id, publish_status, structure_status
FROM basket_schedule_batch
WHERE is_delete = 0 AND publish_status = 'PUBLISHED' AND structure_status <> 'ACTIVE';

-- P0-4 invalid rule/roster binding on VERSIONED ACTIVE structure
SELECT 'P0_INVALID_RULE_ROSTER_BINDING' AS check_code, b.id, b.division_id, b.stage_id
FROM basket_schedule_batch b
JOIN basket_division d ON d.id = b.division_id AND d.is_delete = 0
JOIN basket_division_format_snapshot snap ON snap.id = d.format_snapshot_id AND snap.is_delete = 0
WHERE b.is_delete = 0 AND b.structure_status = 'ACTIVE'
  AND snap.roster_workflow_mode = 'VERSIONED'
  AND (b.rule_version_id IS NULL OR b.rule_version_id <> snap.id
       OR b.roster_version_id IS NULL);

-- P0-5 duplicate EventTeam in locked roster FORMAL
SELECT 'P0_DUP_EVENT_TEAM_LOCKED_ROSTER' AS check_code, e.roster_version_id, e.event_team_id, COUNT(*) AS cnt
FROM basket_division_roster_entry e
JOIN basket_division_roster_version v ON v.id = e.roster_version_id AND v.is_delete = 0 AND v.status = 'LOCKED'
WHERE e.is_delete = 0 AND e.event_team_id IS NOT NULL
GROUP BY e.roster_version_id, e.event_team_id
HAVING COUNT(*) > 1;

-- P0-6 duplicate participant in pairing (resolved team)
SELECT 'P0_DUP_PAIRING_RESOLVED_TEAM' AS check_code, execution_id, resolved_event_team_id, COUNT(*) AS cnt
FROM basket_stage_pairing_entry
WHERE is_delete = 0 AND resolved_event_team_id IS NOT NULL
GROUP BY execution_id, resolved_event_team_id
HAVING COUNT(*) > 1;

-- P0-7 fake BYE Match (BYE source still has both teams)
SELECT 'P0_FAKE_BYE_MATCH' AS check_code, m.id, m.series_id
FROM basket_match m
JOIN basket_match_series s ON s.id = m.series_id AND s.is_delete = 0
WHERE m.is_delete = 0
  AND (IFNULL(s.home_source_type,'') = 'BYE' OR IFNULL(s.away_source_type,'') = 'BYE')
  AND m.home_event_team_id IS NOT NULL AND m.away_event_team_id IS NOT NULL;

-- P0-8 duplicate MatchSeries path
SELECT 'P0_DUP_MATCH_SERIES_PATH' AS check_code, stage_id, round_no, bracket_slot, COUNT(*) AS cnt
FROM basket_match_series
WHERE is_delete = 0
GROUP BY stage_id, round_no, bracket_slot
HAVING COUNT(*) > 1;

-- P0-9 duplicate publishNo
SELECT 'P0_DUP_PUBLISH_NO' AS check_code, stage_id, publish_no, COUNT(*) AS cnt
FROM basket_stage_publish_record
WHERE is_delete = 0
GROUP BY stage_id, publish_no
HAVING COUNT(*) > 1;

-- P0-10 empty withdraw reason
SELECT 'P0_EMPTY_WITHDRAW_REASON' AS check_code, id, stage_id
FROM basket_stage_publish_record
WHERE is_delete = 0 AND action_type = 'WITHDRAW'
  AND (reason IS NULL OR TRIM(reason) = '');

-- P0-11 invalid temporary team source (PERSONAL locked with long-term team id)
SELECT 'P0_INVALID_TEMPORARY_TEAM_SOURCE' AS check_code, et.id, et.division_id
FROM basket_event_team et
JOIN basket_division_roster_version v
  ON v.id = et.roster_version_id AND v.is_delete = 0 AND v.registration_mode = 'PERSONAL'
WHERE et.is_delete = 0
  AND et.source_team_id IS NOT NULL
  AND et.source_draw_team_id IS NULL;

-- P0-12 orphan roster entry
SELECT 'P0_ORPHAN_ROSTER_ENTRY' AS check_code, e.id, e.roster_version_id
FROM basket_division_roster_entry e
LEFT JOIN basket_division_roster_version v ON v.id = e.roster_version_id AND v.is_delete = 0
WHERE e.is_delete = 0 AND v.id IS NULL;

-- P1: current pairing slot uniqueness
SELECT 'P1_DUP_PAIRING_SLOT' AS check_code, execution_id, slot_no, COUNT(*) AS cnt
FROM basket_stage_pairing_entry
WHERE is_delete = 0
GROUP BY execution_id, slot_no
HAVING COUNT(*) > 1;

-- P0-13 match-submission score columns required by web entity
SELECT 'P0_MISSING_SUBMISSION_SCORE_COLS' AS check_code, req.column_name
FROM (
  SELECT 'home_score' AS column_name
  UNION ALL SELECT 'away_score'
  UNION ALL SELECT 'submit_remark'
) req
LEFT JOIN information_schema.columns c
  ON c.table_schema = DATABASE()
 AND c.table_name = 'basket_match_submission'
 AND c.column_name = req.column_name
WHERE c.column_name IS NULL;

-- P0-14 published batch still has draft matches (natural-resolution child games)
SELECT 'P0_PUBLISHED_BATCH_DRAFT_MATCH' AS check_code, m.id, m.stage_id, m.series_id, m.publish_status
FROM basket_match m
JOIN basket_schedule_batch b ON b.id = m.schedule_batch_id AND b.is_delete = 0
WHERE m.is_delete = 0
  AND b.publish_status = 'PUBLISHED'
  AND IFNULL(m.publish_status, 'DRAFT') <> 'PUBLISHED';

-- Required tables
SELECT 'REQUIRED_TABLE' AS check_code, table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'basket_division_roster_version','basket_division_roster_entry',
    'basket_stage_pairing_execution','basket_stage_pairing_entry',
    'basket_stage_publish_record','basket_match_series',
    'basket_player_platform_account','basket_business_audit'
  )
ORDER BY table_name;

-- STANDARD_V1
SELECT template_code, template_type, default_registration_mode, enabled, is_default
FROM basket_format_template
WHERE template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND version_no = 1 AND is_delete = 0
ORDER BY template_code;

SELECT t.template_code, s.stage_code, s.competition_format, s.pairing_rule,
       s.best_of, s.semifinal_best_of, s.final_best_of, s.organization_mode
FROM basket_format_template t
JOIN basket_format_template_stage s ON s.template_id = t.id AND s.is_delete = 0
WHERE t.template_code IN ('LEAGUE_STANDARD_V1','ARENA_STANDARD_V1','CUP_STANDARD_V1','OPEN_STANDARD_V1')
  AND t.version_no = 1 AND t.is_delete = 0
ORDER BY t.template_code, s.stage_order;
