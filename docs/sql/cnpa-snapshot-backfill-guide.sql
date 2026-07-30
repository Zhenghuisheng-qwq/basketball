-- 历史已通过报名缺少 EventTeam / EventPlayer 快照的数据修复说明
-- 1. 优先使用 Admin API：POST /basket/registrationBackfill/backfill-approved
-- 2. 或单条补偿：POST /basket/registrationBackfill/{registrationId}/backfill
-- 3. 若仅需修复锁定状态（快照已存在），执行 cnpa-roster-lock-repair.sql

-- 查询：已通过但缺少 EventTeam 的报名
SELECT r.id, r.registration_no, r.team_id, r.season_id, r.audit_time
FROM basket_tournament_registration r
WHERE r.is_delete = 0
  AND r.registration_status = 3
  AND NOT EXISTS (
    SELECT 1 FROM basket_team_season_profile p
    WHERE p.registration_id = r.id AND p.is_delete = 0
  );

-- 查询：已有 EventTeam 但缺少 EventPlayer 的报名
SELECT r.id, p.id AS event_team_id, p.event_team_no
FROM basket_tournament_registration r
INNER JOIN basket_team_season_profile p ON p.registration_id = r.id AND p.is_delete = 0
WHERE r.is_delete = 0
  AND r.registration_status = 3
  AND EXISTS (
    SELECT 1 FROM basket_tournament_registration_player rp
    WHERE rp.registration_id = r.id AND rp.is_delete = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM basket_tournament_season_roster sr
    WHERE sr.registration_id = r.id AND sr.is_delete = 0
  );
