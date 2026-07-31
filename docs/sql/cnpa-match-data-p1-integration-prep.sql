-- CNPA 第四轮-B 联调数据准备
-- 1. 将 900020001 争议窗口基准时间刷新到当前（不破坏正式比分）
-- 2. 清理旧争议/异常测试残留
-- 3. 创建 M07 测试比赛 900020002

SET @match_dispute := 900020001;
SET @match_exception := 900020002;
SET @season_id := 1;
SET @stage_id := 900030001;
SET @team_rng := 900010001;
SET @team_phx := 900010002;

-- 刷新争议窗口：effective_time / confirmed_time 设为当前
UPDATE basket_match_official_result
SET effective_time = NOW(), confirmed_time = NOW(), update_time = NOW()
WHERE match_id = @match_dispute AND is_current = 1 AND is_delete = 0;

UPDATE basket_match
SET update_time = NOW()
WHERE id = @match_dispute;

-- 清理 M06 测试残留（保留正式 stat）
DELETE FROM basket_match_data_dispute_material WHERE match_id = @match_dispute;
DELETE FROM basket_match_data_dispute WHERE match_id = @match_dispute;

-- M07 测试比赛：未 CONFIRMED、双方 RNG vs PHX
INSERT INTO basket_match (id, match_no, season_id, stage_id, home_team_id, away_team_id, scheduled_start_time, scheduled_end_time, match_status, home_score, away_score, data_status, result_status, score_confirmed, data_version, public_status, sort_no, create_time, update_time, is_delete)
VALUES (@match_exception, 'MD-INT-900020002', @season_id, @stage_id, @team_rng, @team_phx,
        DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
        0, 0, 0, 0, 0, 0, 1, 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    match_status = 0, data_status = 0, home_score = 0, away_score = 0,
    winner_team_id = NULL, walkover_team_id = NULL, result_status = 0,
    scheduled_start_time = DATE_ADD(NOW(), INTERVAL 1 DAY), update_time = NOW();

INSERT INTO basket_match_team (id, match_id, team_id, side_type, score, result_type, is_winner, create_time, update_time, is_delete)
VALUES
(900060003, @match_exception, @team_rng, 'HOME', 0, 'LOSS', 0, NOW(), NOW(), 0),
(900060004, @match_exception, @team_phx, 'AWAY', 0, 'LOSS', 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE score = 0, result_type = 'LOSS', is_winner = 0, update_time = NOW();

INSERT INTO basket_match_roster (id, match_id, season_id, team_id, player_id, player_nickname_snapshot, jersey_no, roster_role, is_starting, play_status, captain_flag, roster_status, sort_no, create_time, update_time, is_delete)
VALUES
(900070101, @match_exception, @season_id, @team_rng, 2081010979671425026, 'RNG-P1', '1', 'CAPTAIN', 1, 1, 1, 1, 1, NOW(), NOW(), 0),
(900070102, @match_exception, @season_id, @team_phx, 2081019227996807169, 'PHX-P1', '1', 'CAPTAIN', 1, 1, 1, 1, 2, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE roster_status = 1, update_time = NOW();

-- 清理 M07 测试残留
DELETE FROM basket_match_exception_material WHERE match_id IN (@match_exception, 900020003, 900020004);
DELETE FROM basket_match_exception_audit WHERE application_id IN (SELECT id FROM basket_match_exception_application WHERE match_id IN (@match_exception, 900020003, 900020004));
DELETE FROM basket_match_exception_application WHERE match_id IN (@match_exception, 900020003, 900020004);

-- POSTPONE / CANCEL 专用比赛
INSERT INTO basket_match (id, match_no, season_id, stage_id, home_team_id, away_team_id, scheduled_start_time, scheduled_end_time, match_status, home_score, away_score, data_status, result_status, score_confirmed, data_version, public_status, sort_no, create_time, update_time, is_delete)
VALUES
(900020003, 'MD-INT-900020003', @season_id, @stage_id, @team_rng, @team_phx, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 0, 0, 0, 0, 0, 0, 1, 1, 0, NOW(), NOW(), 0),
(900020004, 'MD-INT-900020004', @season_id, @stage_id, @team_phx, @team_rng, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), 0, 0, 0, 0, 0, 0, 1, 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE match_status = 0, data_status = 0, home_score = 0, away_score = 0, update_time = NOW();

-- 确保队长账号与 registration 一致（111@qq.com = RNG 队长）
UPDATE basket_tournament_registration r
JOIN basket_member_account ma ON ma.email = '111@qq.com' AND ma.is_delete = 0
SET r.captain_member_id = ma.id, r.update_time = NOW()
WHERE r.team_id = 900010001 AND r.season_id = 1;

UPDATE basket_team t
JOIN basket_member_account ma ON ma.email = '111@qq.com' AND ma.is_delete = 0
SET t.captain_member_id = ma.id, t.update_time = NOW()
WHERE t.id = 900010001;

SELECT @match_dispute AS dispute_match_id, @match_exception AS exception_match_id;
