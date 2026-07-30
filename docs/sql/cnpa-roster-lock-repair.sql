-- 一次性修复：已通过报名但未写入阵容锁定状态的历史数据
-- 执行前请先备份相关表

UPDATE basket_team_season_profile p
INNER JOIN basket_tournament_registration r ON r.id = p.registration_id AND r.is_delete = 0
SET p.roster_lock_status = 1,
    p.roster_locked_time = COALESCE(p.roster_locked_time, p.approved_time, r.audit_time, NOW()),
    p.update_time = NOW()
WHERE p.is_delete = 0
  AND r.registration_status = 3
  AND (p.roster_lock_status IS NULL OR p.roster_lock_status = 0);

UPDATE basket_tournament_season_roster sr
INNER JOIN basket_team_season_profile p ON p.id = sr.event_team_id AND p.is_delete = 0
INNER JOIN basket_tournament_registration r ON r.id = p.registration_id AND r.is_delete = 0
SET sr.lock_status = 1,
    sr.update_time = NOW()
WHERE sr.is_delete = 0
  AND sr.roster_status = 1
  AND r.registration_status = 3
  AND (sr.lock_status IS NULL OR sr.lock_status = 0);
