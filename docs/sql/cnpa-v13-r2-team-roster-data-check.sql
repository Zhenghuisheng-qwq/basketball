-- =============================================================================
-- CNPA V1.3 R2 TEAM Roster 数据检查
-- 文件：cnpa-v13-r2-team-roster-data-check.sql
-- 期望：以下查询均返回 0 行（或标注允许的 Legacy）
-- =============================================================================

SET NAMES utf8mb4;

-- 1. 同一 Division 不能有多个 current roster
SELECT division_id, COUNT(*) AS current_cnt
FROM basket_division_roster_version
WHERE is_delete = 0 AND is_current = 1 AND status = 'LOCKED'
GROUP BY division_id
HAVING COUNT(*) > 1;

-- 2. 同一 Division 当前工作态候补顺序重复
SELECT division_id, candidate_order, COUNT(*) AS cnt
FROM basket_registration
WHERE is_delete = 0
  AND participation_role = 'CANDIDATE'
  AND candidate_order IS NOT NULL
GROUP BY division_id, candidate_order
HAVING COUNT(*) > 1;

-- 3. FORMAL 数超过 Division 正式名额
SELECT r.division_id, COUNT(*) AS formal_cnt, d.max_team_count
FROM basket_registration r
JOIN basket_division d ON d.id = r.division_id AND d.is_delete = 0
WHERE r.is_delete = 0
  AND r.registration_status = 'APPROVED'
  AND r.participation_role = 'FORMAL'
  AND d.max_team_count IS NOT NULL
GROUP BY r.division_id, d.max_team_count
HAVING COUNT(*) > d.max_team_count;

-- 4. REJECTED 不能是 FORMAL/CANDIDATE
SELECT id, division_id, registration_status, participation_role
FROM basket_registration
WHERE is_delete = 0
  AND registration_status = 'REJECTED'
  AND participation_role IN ('FORMAL', 'CANDIDATE');

-- 5. RosterEntry 必须属于存在的 Version
SELECT e.id, e.roster_version_id
FROM basket_division_roster_entry e
LEFT JOIN basket_division_roster_version v ON v.id = e.roster_version_id AND v.is_delete = 0
WHERE e.is_delete = 0 AND v.id IS NULL;

-- 6. 当前 LOCKED Roster 的 FORMAL EventTeam 必须绑定该 roster_version_id
SELECT et.id, et.division_id, et.roster_version_id, v.id AS current_roster_id
FROM basket_event_team et
JOIN basket_division_roster_version v
  ON v.division_id = et.division_id AND v.is_current = 1 AND v.status = 'LOCKED' AND v.is_delete = 0
JOIN basket_division_roster_entry e
  ON e.roster_version_id = v.id AND e.registration_id = et.registration_id AND e.is_delete = 0
WHERE et.is_delete = 0
  AND et.event_team_status = 'ACTIVE'
  AND e.participation_role = 'FORMAL'
  AND (et.roster_version_id IS NULL OR et.roster_version_id <> v.id);

-- 7. Legacy：无 RosterVersion 的 ACTIVE EventTeam 允许 roster_version_id 为 NULL（仅展示，不报错）
SELECT et.id, et.division_id, et.roster_version_id
FROM basket_event_team et
LEFT JOIN basket_division_roster_version v
  ON v.division_id = et.division_id AND v.is_delete = 0
WHERE et.is_delete = 0
  AND et.event_team_status = 'ACTIVE'
  AND et.roster_version_id IS NULL
  AND v.id IS NULL;
