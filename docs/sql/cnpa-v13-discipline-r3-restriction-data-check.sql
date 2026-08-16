-- CNPA Discipline V2 R3 restriction aggregate data-check (read-only). Do not UPDATE/DELETE.
SET NAMES utf8mb4;

SELECT 'MULTI_ACTIVE_SAME_SUBJECT' AS check_code,
       target_type, season_id,
       CASE target_type
           WHEN 'PLAYER' THEN player_id
           WHEN 'PERMANENT_TEAM' THEN team_id
           WHEN 'EVENT_TEAM' THEN event_team_id
           WHEN 'ACCOUNT' THEN member_id
       END AS subject_id,
       COUNT(*) AS active_count,
       GROUP_CONCAT(id ORDER BY id) AS discipline_ids,
       GROUP_CONCAT(penalty_code ORDER BY id) AS penalty_codes
FROM basket_discipline_record
WHERE is_delete = 0 AND record_status = 'ACTIVE'
GROUP BY target_type, season_id,
         CASE target_type
             WHEN 'PLAYER' THEN player_id
             WHEN 'PERMANENT_TEAM' THEN team_id
             WHEN 'EVENT_TEAM' THEN event_team_id
             WHEN 'ACCOUNT' THEN member_id
         END
HAVING COUNT(*) > 1
ORDER BY active_count DESC, target_type, season_id;

SELECT 'FUTURE_START_BUT_RESTRICTION_ACTIVE' AS check_code,
       d.id AS discipline_id, d.target_type, d.penalty_code, d.record_status,
       d.player_id, d.team_id, d.event_team_id, d.season_id,
       d.effective_start_time, r.id AS restriction_id, r.active_flag,
       r.allow_lineup, r.allow_captain, r.allow_registration, r.allow_participation,
       r.remaining_match_suspensions
FROM basket_discipline_record d
JOIN basket_discipline_restriction r
  ON r.is_delete = 0 AND r.active_flag = 1
 AND r.season_id <=> d.season_id
 AND (
        (d.target_type = 'PLAYER' AND r.subject_type = 'PLAYER' AND r.subject_id = d.player_id)
     OR (d.target_type = 'PERMANENT_TEAM' AND r.subject_type = 'PERMANENT_TEAM' AND r.subject_id = d.team_id)
     OR (d.target_type = 'EVENT_TEAM' AND r.subject_type = 'EVENT_TEAM' AND r.subject_id = d.event_team_id)
     OR (d.target_type = 'ACCOUNT' AND r.subject_type = 'ACCOUNT' AND r.subject_id = d.member_id)
 )
WHERE d.is_delete = 0 AND d.record_status = 'ACTIVE'
  AND d.effective_start_time IS NOT NULL AND d.effective_start_time > NOW()
  AND (r.allow_registration = 0 OR r.allow_transfer = 0 OR r.allow_captain = 0
       OR r.allow_lineup = 0 OR r.allow_participation = 0 OR r.remaining_match_suspensions > 0)
ORDER BY d.id;

SELECT 'EXPIRED_END_BUT_RESTRICTION_ACTIVE' AS check_code,
       d.id AS discipline_id, d.target_type, d.penalty_code, d.record_status,
       d.player_id, d.team_id, d.event_team_id, d.season_id,
       d.effective_end_time, r.id AS restriction_id, r.active_flag,
       r.allow_lineup, r.allow_captain, r.allow_registration, r.allow_participation,
       r.remaining_match_suspensions
FROM basket_discipline_record d
JOIN basket_discipline_restriction r
  ON r.is_delete = 0 AND r.active_flag = 1
 AND r.season_id <=> d.season_id
 AND (
        (d.target_type = 'PLAYER' AND r.subject_type = 'PLAYER' AND r.subject_id = d.player_id)
     OR (d.target_type = 'PERMANENT_TEAM' AND r.subject_type = 'PERMANENT_TEAM' AND r.subject_id = d.team_id)
     OR (d.target_type = 'EVENT_TEAM' AND r.subject_type = 'EVENT_TEAM' AND r.subject_id = d.event_team_id)
     OR (d.target_type = 'ACCOUNT' AND r.subject_type = 'ACCOUNT' AND r.subject_id = d.member_id)
 )
WHERE d.is_delete = 0 AND d.record_status = 'ACTIVE'
  AND d.effective_end_time IS NOT NULL AND d.effective_end_time < NOW()
  AND (r.allow_registration = 0 OR r.allow_transfer = 0 OR r.allow_captain = 0
       OR r.allow_lineup = 0 OR r.allow_participation = 0 OR r.remaining_match_suspensions > 0)
ORDER BY d.id;

SELECT 'ACTIVE_DISC_BUT_RESTRICTION_INACTIVE' AS check_code,
       d.id AS discipline_id, d.target_type, d.penalty_code, d.record_status,
       d.player_id, d.team_id, d.event_team_id, d.season_id,
       r.id AS restriction_id, r.active_flag
FROM basket_discipline_record d
JOIN basket_discipline_restriction r
  ON r.is_delete = 0 AND r.active_flag = 0
 AND r.season_id <=> d.season_id
 AND (
        (d.target_type = 'PLAYER' AND r.subject_type = 'PLAYER' AND r.subject_id = d.player_id)
     OR (d.target_type = 'PERMANENT_TEAM' AND r.subject_type = 'PERMANENT_TEAM' AND r.subject_id = d.team_id)
     OR (d.target_type = 'EVENT_TEAM' AND r.subject_type = 'EVENT_TEAM' AND r.subject_id = d.event_team_id)
     OR (d.target_type = 'ACCOUNT' AND r.subject_type = 'ACCOUNT' AND r.subject_id = d.member_id)
 )
WHERE d.is_delete = 0 AND d.record_status = 'ACTIVE'
  AND NOT EXISTS (
        SELECT 1 FROM basket_discipline_restriction r2
        WHERE r2.is_delete = 0 AND r2.active_flag = 1
          AND r2.season_id <=> d.season_id
          AND (
                (d.target_type = 'PLAYER' AND r2.subject_type = 'PLAYER' AND r2.subject_id = d.player_id)
             OR (d.target_type = 'PERMANENT_TEAM' AND r2.subject_type = 'PERMANENT_TEAM' AND r2.subject_id = d.team_id)
             OR (d.target_type = 'EVENT_TEAM' AND r2.subject_type = 'EVENT_TEAM' AND r2.subject_id = d.event_team_id)
             OR (d.target_type = 'ACCOUNT' AND r2.subject_type = 'ACCOUNT' AND r2.subject_id = d.member_id)
          )
  )
ORDER BY d.id;
