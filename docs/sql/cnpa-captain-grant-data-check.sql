-- CNPA V2 队长授权 data-check（P0）

-- 1. ACTIVE 同 division+player 重复
SELECT 'P0_ACTIVE_DUP_DIVISION_PLAYER' AS check_code, COUNT(*) AS cnt
FROM (
    SELECT division_id, player_id
    FROM basket_captain_grant
    WHERE is_delete = 0 AND grant_status = 'ACTIVE' AND active_unique_key IS NOT NULL
    GROUP BY division_id, player_id
    HAVING COUNT(*) > 1
) t;

-- 2. ACTIVE active_unique_key 与 division+player 不一致
SELECT 'P0_ACTIVE_KEY_MISMATCH' AS check_code, COUNT(*) AS cnt
FROM basket_captain_grant
WHERE is_delete = 0
  AND grant_status = 'ACTIVE'
  AND active_unique_key IS NOT NULL
  AND active_unique_key <> CONCAT(division_id, '_', player_id);

-- 3. REVOKED/EXPIRED active_unique_key 非 NULL
SELECT 'P0_INACTIVE_KEY_NOT_NULL' AS check_code, COUNT(*) AS cnt
FROM basket_captain_grant
WHERE is_delete = 0
  AND grant_status IN ('REVOKED', 'EXPIRED')
  AND active_unique_key IS NOT NULL;

-- 4. 已过 valid_to 仍为 ACTIVE（应被懒过期或视为无效）
SELECT 'P0_ACTIVE_PAST_VALID_TO' AS check_code, COUNT(*) AS cnt
FROM basket_captain_grant g
WHERE g.is_delete = 0
  AND g.grant_status = 'ACTIVE'
  AND g.active_unique_key IS NOT NULL
  AND g.valid_to IS NOT NULL
  AND g.valid_to < NOW();

-- 5. FINISHED/ARCHIVED 赛季仍存在 ACTIVE grant
SELECT 'P0_ACTIVE_ENDED_SEASON' AS check_code, COUNT(*) AS cnt
FROM basket_captain_grant g
INNER JOIN basket_season se ON se.id = g.season_id AND se.is_delete = 0
WHERE g.is_delete = 0
  AND g.grant_status = 'ACTIVE'
  AND g.active_unique_key IS NOT NULL
  AND se.season_status IN ('FINISHED', 'ARCHIVED');

-- 6. 已绑定 team_id 指向不存在球队
SELECT 'P0_BOUND_TEAM_MISSING' AS check_code, COUNT(*) AS cnt
FROM basket_captain_grant g
LEFT JOIN basket_team t ON t.id = g.team_id AND t.is_delete = 0
WHERE g.is_delete = 0
  AND g.team_id IS NOT NULL
  AND t.id IS NULL;

-- 7. 同一 Grant 被两个 Team 使用（registration 与 grant 绑定冲突）
SELECT 'P0_GRANT_MULTI_TEAM_USAGE' AS check_code, COUNT(*) AS cnt
FROM basket_captain_grant g
INNER JOIN basket_registration r ON r.division_id = g.division_id
    AND r.captain_player_id = g.player_id
    AND r.is_delete = 0
    AND r.registration_status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN')
WHERE g.is_delete = 0
  AND g.grant_status = 'ACTIVE'
  AND g.team_id IS NOT NULL
  AND r.team_id <> g.team_id;

-- 8. Registration captain 与 Grant player 不一致（通过 division+team 报名但 captain 无 grant）
SELECT 'P0_REG_CAPTAIN_NO_GRANT' AS check_code, COUNT(*) AS cnt
FROM basket_registration r
LEFT JOIN basket_captain_grant g ON g.division_id = r.division_id
    AND g.player_id = r.captain_player_id
    AND g.is_delete = 0
    AND g.grant_status = 'ACTIVE'
    AND g.active_unique_key IS NOT NULL
WHERE r.is_delete = 0
  AND r.registration_mode = 'TEAM'
  AND r.registration_status IN ('PENDING_REVIEW', 'APPROVED')
  AND g.id IS NULL;
