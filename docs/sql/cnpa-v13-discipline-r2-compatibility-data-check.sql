-- CNPA Discipline V2 R2 compatibility data-check (read-only). Do not UPDATE/DELETE.
SET NAMES utf8mb4;

SELECT 'INVALID_TARGET_PENALTY' AS check_code,
       id AS discipline_id, target_type, penalty_code, record_status,
       player_id, team_id, event_team_id, season_id, create_time
FROM basket_discipline_record
WHERE is_delete = 0
  AND (
        (penalty_code = 'WARNING' AND target_type NOT IN ('PLAYER', 'PERMANENT_TEAM', 'EVENT_TEAM'))
     OR (penalty_code IN ('MATCH_SUSPENSION', 'DATE_RANGE_SUSPENSION', 'REGISTRATION_BAN', 'TRANSFER_BAN', 'CAPTAIN_BAN')
         AND target_type <> 'PLAYER')
     OR (penalty_code IN ('POINT_DEDUCTION', 'EVENT_DISQUALIFICATION') AND target_type <> 'EVENT_TEAM')
     OR (penalty_code IN ('MATCH_ELIGIBILITY_CANCEL', 'TEAM_SUSPENSION', 'PLAYER_QUALIFICATION_SUSPENSION'))
     OR (target_type = 'ACCOUNT')
  )
ORDER BY id;

SELECT 'P0_NON_EVENT_TEAM_DQ' AS check_code,
       id AS discipline_id, target_type, penalty_code, record_status,
       player_id, team_id, event_team_id, season_id, create_time
FROM basket_discipline_record
WHERE is_delete = 0
  AND penalty_code = 'EVENT_DISQUALIFICATION'
  AND target_type <> 'EVENT_TEAM'
ORDER BY id;
