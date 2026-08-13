-- =============================================================================
-- CNPA V1.3 R3 data checks. Expect 0 rows unless noted.
-- =============================================================================

SET NAMES utf8mb4;

-- 1. VERSIONED PERSONAL without locked roster should not have ACTIVE structure batches
--    (informational: list VERSIONED PERSONAL divisions missing locked roster)
SELECT d.id AS division_id, d.division_name, s.roster_workflow_mode, s.snapshot_status
FROM basket_division d
JOIN basket_division_format_snapshot s
  ON s.division_id = d.id AND s.is_current = 1 AND s.is_delete = 0
LEFT JOIN basket_division_roster_version v
  ON v.division_id = d.id AND v.is_current = 1 AND v.status = 'LOCKED' AND v.is_delete = 0
WHERE d.is_delete = 0
  AND d.registration_mode = 'PERSONAL'
  AND s.roster_workflow_mode = 'VERSIONED'
  AND v.id IS NULL;

-- 2. Each locked PERSONAL roster has at least 2 EventTeams
SELECT v.id AS roster_version_id, v.division_id, COUNT(et.id) AS team_cnt
FROM basket_division_roster_version v
LEFT JOIN basket_event_team et
  ON et.roster_version_id = v.id AND et.is_delete = 0 AND et.event_team_status = 'ACTIVE'
WHERE v.is_delete = 0 AND v.status = 'LOCKED' AND v.registration_mode = 'PERSONAL'
GROUP BY v.id, v.division_id
HAVING COUNT(et.id) < 2;

-- 3. Temporary EventTeam size vs GameMode min/max
SELECT et.id, et.division_id, d.game_mode, COUNT(ep.id) AS member_cnt
FROM basket_event_team et
JOIN basket_division d ON d.id = et.division_id AND d.is_delete = 0
JOIN basket_division_roster_version v ON v.id = et.roster_version_id AND v.registration_mode = 'PERSONAL'
LEFT JOIN basket_event_player ep ON ep.event_team_id = et.id AND ep.is_delete = 0
WHERE et.is_delete = 0 AND et.source_team_id IS NULL
GROUP BY et.id, et.division_id, d.game_mode
HAVING (d.game_mode = 'THREE_V_THREE' AND (COUNT(ep.id) < 3 OR COUNT(ep.id) > 8))
    OR (d.game_mode = 'FIVE_V_FIVE' AND (COUNT(ep.id) < 5 OR COUNT(ep.id) > 8));

-- 4. EventPlayer belongs to one EventTeam per roster version
SELECT roster_version_id, source_player_id, COUNT(*) AS cnt
FROM basket_event_player
WHERE is_delete = 0 AND roster_version_id IS NOT NULL
GROUP BY roster_version_id, source_player_id
HAVING COUNT(*) > 1;

-- 5. Missing APPROVED personal registrations in current locked PERSONAL roster
SELECT pr.id, pr.division_id, pr.player_id
FROM basket_personal_registration pr
JOIN basket_division_roster_version v
  ON v.division_id = pr.division_id AND v.is_current = 1 AND v.status = 'LOCKED' AND v.registration_mode = 'PERSONAL'
LEFT JOIN basket_event_player ep
  ON ep.roster_version_id = v.id AND ep.source_player_id = pr.player_id AND ep.is_delete = 0
WHERE pr.is_delete = 0 AND pr.registration_status = 'APPROVED' AND ep.id IS NULL;

-- 6. Captain belongs to EventTeam
SELECT et.id, et.captain_player_id
FROM basket_event_team et
JOIN basket_division_roster_version v ON v.id = et.roster_version_id AND v.registration_mode = 'PERSONAL'
LEFT JOIN basket_event_player ep
  ON ep.event_team_id = et.id AND ep.source_player_id = et.captain_player_id AND ep.is_delete = 0
WHERE et.is_delete = 0 AND et.captain_player_id IS NOT NULL AND ep.id IS NULL;

-- 7. Captain should be earliest user.create_time in the team
SELECT et.id AS event_team_id, et.captain_member_id, MIN(acc.create_time) AS earliest, MIN(acc.id) AS earliest_id
FROM basket_event_team et
JOIN basket_event_player ep ON ep.event_team_id = et.id AND ep.is_delete = 0
JOIN basket_member_account acc ON acc.id = ep.member_id AND acc.is_delete = 0
JOIN basket_division_roster_version v ON v.id = et.roster_version_id AND v.registration_mode = 'PERSONAL'
WHERE et.is_delete = 0
GROUP BY et.id, et.captain_member_id
HAVING et.captain_member_id <> (
  SELECT ep2.member_id
  FROM basket_event_player ep2
  JOIN basket_member_account acc2 ON acc2.id = ep2.member_id
  WHERE ep2.event_team_id = et.id AND ep2.is_delete = 0
  ORDER BY acc2.create_time ASC, acc2.id ASC
  LIMIT 1
);

-- 8. PERSONAL EventTeam must not point to a new basket_team
SELECT et.id, et.source_team_id
FROM basket_event_team et
JOIN basket_division_roster_version v ON v.id = et.roster_version_id AND v.registration_mode = 'PERSONAL'
WHERE et.is_delete = 0 AND et.source_team_id IS NOT NULL;

-- 9. RosterEntry can restore PERSONAL roster
SELECT v.id
FROM basket_division_roster_version v
LEFT JOIN basket_division_roster_entry e ON e.roster_version_id = v.id AND e.is_delete = 0
WHERE v.is_delete = 0 AND v.status = 'LOCKED' AND v.registration_mode = 'PERSONAL'
GROUP BY v.id
HAVING COUNT(e.id) < 2 OR SUM(CASE WHEN e.event_team_id IS NULL THEN 1 ELSE 0 END) > 0;

-- 10. Legacy EventTeam without roster still readable
SELECT et.id, et.division_id
FROM basket_event_team et
LEFT JOIN basket_division_roster_version v ON v.division_id = et.division_id AND v.is_delete = 0
WHERE et.is_delete = 0 AND et.event_team_status = 'ACTIVE' AND et.roster_version_id IS NULL AND v.id IS NULL;
