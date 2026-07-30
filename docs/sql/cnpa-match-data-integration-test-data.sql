-- CNPA 比赛数据联调测试数据（第三轮）
-- 依赖：season_id=1 已存在；submission migration 已执行
-- 测试比赛：RNG 64 : 55 PHOENIX，match_id=900020001
-- RNG 队长 member_id=2081010979604316200
-- PHOENIX 队长 member_id=2081019227929698300

SET @season_id := 1;
SET @stage_id := 900030001;
SET @team_rng := 900010001;
SET @team_phx := 900010002;
SET @match_id := 900020001;
SET @reg_rng := 900040001;
SET @reg_phx := 900040002;

-- 阶段
INSERT INTO basket_tournament_stage (id, season_id, stage_no, stage_name, stage_type, format_type, start_time, end_time, sequence_no, stage_status, public_status, create_time, update_time, is_delete)
VALUES (@stage_id, @season_id, 'ST-INT-001', '常规赛联调', 'REGULAR', 'ROUND_ROBIN', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE stage_name = VALUES(stage_name), update_time = NOW();

-- 球队
INSERT INTO basket_team (id, team_no, team_name, short_name, captain_member_id, captain_player_id, max_member_count, current_member_count, team_status, create_time, update_time, is_delete)
VALUES
(@team_rng, 'TEAM-RNG-INT', 'RNG', 'RNG', 2081010979604316200, 2081010979671425000, 15, 5, 1, NOW(), NOW(), 0),
(@team_phx, 'TEAM-PHX-INT', 'PHOENIX', 'PHX', 2081019227929698300, 2081019227996807200, 15, 5, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE team_name = VALUES(team_name), captain_member_id = VALUES(captain_member_id), update_time = NOW();

-- 报名（APPROVED=2）
INSERT INTO basket_tournament_registration (id, registration_no, season_id, team_id, platform_code, captain_member_id, captain_player_id, contact_name, contact_mobile, registration_status, lineup_count, submitted_time, audit_time, create_time, update_time, is_delete)
VALUES
(@reg_rng, 'REG-RNG-INT', @season_id, @team_rng, 'STEAM', 2081010979604316200, 2081010979671425000, 'RNG Captain', '13800000001', 3, 5, NOW(), NOW(), NOW(), NOW(), 0),
(@reg_phx, 'REG-PHX-INT', @season_id, @team_phx, 'STEAM', 2081019227929698300, 2081019227996807200, 'PHX Captain', '13800000002', 3, 5, NOW(), NOW(), NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE captain_member_id = VALUES(captain_member_id), registration_status = 3, update_time = NOW();

-- 赛季 roster（roster_status=1 ACTIVE）
INSERT INTO basket_tournament_season_roster (id, roster_no, season_id, event_team_id, team_id, player_id, member_id, registration_id, player_nickname_snapshot, roster_role, is_captain, is_starting, roster_status, lock_status, create_time, update_time, is_delete)
VALUES
(900050001, 'RS-RNG-1', @season_id, @reg_rng, @team_rng, 2081010979671425000, 2081010979604316200, @reg_rng, 'RNG-P1', 'CAPTAIN', 1, 1, 1, 0, NOW(), NOW(), 0),
(900050002, 'RS-RNG-2', @season_id, @reg_rng, @team_rng, 2081041427005038600, 2081041427005038600, @reg_rng, 'RNG-P2', 'PLAYER', 0, 1, 1, 0, NOW(), NOW(), 0),
(900050003, 'RS-RNG-3', @season_id, @reg_rng, @team_rng, 2081072741716336600, 2081072741649227800, @reg_rng, 'RNG-P3', 'PLAYER', 0, 1, 1, 0, NOW(), NOW(), 0),
(900050004, 'RS-RNG-4', @season_id, @reg_rng, @team_rng, 2081073127508418600, 2081073127445504000, @reg_rng, 'RNG-P4', 'PLAYER', 0, 1, 1, 0, NOW(), NOW(), 0),
(900050005, 'RS-RNG-5', @season_id, @reg_rng, @team_rng, 2081073380093599700, 2081073380093599700, @reg_rng, 'RNG-P5', 'PLAYER', 0, 1, 1, 0, NOW(), NOW(), 0),
(900050006, 'RS-PHX-1', @season_id, @reg_phx, @team_phx, 2081019227996807200, 2081019227929698300, @reg_phx, 'PHX-P1', 'CAPTAIN', 1, 1, 1, 0, NOW(), NOW(), 0),
(900050007, 'RS-PHX-2', @season_id, @reg_phx, @team_phx, 2082478335308456000, 2082478335308456000, @reg_phx, 'PHX-P2', 'PLAYER', 0, 1, 1, 0, NOW(), NOW(), 0),
(900050008, 'RS-PHX-3', @season_id, @reg_phx, @team_phx, 2082786571525836800, 2082786571525836800, @reg_phx, 'PHX-P3', 'PLAYER', 0, 1, 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE roster_status = 1, update_time = NOW();

-- 比赛（已结束 match_status=3，data_status=0 DRAFT）
INSERT INTO basket_match (id, match_no, season_id, stage_id, home_team_id, away_team_id, scheduled_start_time, scheduled_end_time, actual_end_time, match_status, home_score, away_score, data_status, result_status, score_confirmed, data_version, public_status, sort_no, create_time, update_time, is_delete)
VALUES (@match_id, 'MD-INT-900020001', @season_id, @stage_id, @team_rng, @team_phx, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3, 0, 0, 0, 0, 0, 1, 1, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE match_status = 3, data_status = 0, home_score = 0, away_score = 0, update_time = NOW();

INSERT INTO basket_match_team (id, match_id, team_id, side_type, score, result_type, is_winner, create_time, update_time, is_delete)
VALUES
(900060001, @match_id, @team_rng, 'HOME', 0, 'LOSS', 0, NOW(), NOW(), 0),
(900060002, @match_id, @team_phx, 'AWAY', 0, 'LOSS', 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 出场名单 5+3（PHX 仅 3 名可用球员，联调可测）
INSERT INTO basket_match_roster (id, match_id, season_id, team_id, player_id, player_nickname_snapshot, jersey_no, roster_role, is_starting, play_status, captain_flag, roster_status, sort_no, create_time, update_time, is_delete)
VALUES
(900070001, @match_id, @season_id, @team_rng, 2081010979671425000, 'RNG-P1', '1', 'CAPTAIN', 1, 1, 1, 1, 1, NOW(), NOW(), 0),
(900070002, @match_id, @season_id, @team_rng, 2081041427005038600, 'RNG-P2', '2', 'PLAYER', 1, 1, 0, 1, 2, NOW(), NOW(), 0),
(900070003, @match_id, @season_id, @team_rng, 2081072741716336600, 'RNG-P3', '3', 'PLAYER', 1, 1, 0, 1, 3, NOW(), NOW(), 0),
(900070004, @match_id, @season_id, @team_rng, 2081073127508418600, 'RNG-P4', '4', 'PLAYER', 1, 1, 0, 1, 4, NOW(), NOW(), 0),
(900070005, @match_id, @season_id, @team_rng, 2081073380093599700, 'RNG-P5', '5', 'PLAYER', 1, 1, 0, 1, 5, NOW(), NOW(), 0),
(900070006, @match_id, @season_id, @team_phx, 2081019227996807200, 'PHX-P1', '1', 'CAPTAIN', 1, 1, 1, 1, 6, NOW(), NOW(), 0),
(900070007, @match_id, @season_id, @team_phx, 2082478335308456000, 'PHX-P2', '2', 'PLAYER', 1, 1, 0, 1, 7, NOW(), NOW(), 0),
(900070008, @match_id, @season_id, @team_phx, 2082786571525836800, 'PHX-P3', '3', 'PLAYER', 1, 1, 0, 1, 8, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE roster_status = 1, update_time = NOW();

-- 材料文件（复用已有截图并标记用途，供联调）
UPDATE basket_file_asset SET usage_type = 'MATCH_DATA_SCREENSHOT', update_time = NOW()
WHERE id = 2081011198194663426 AND is_delete = 0;

SELECT @match_id AS integration_match_id, @team_rng AS rng_team_id, @team_phx AS phx_team_id, @season_id AS season_id;
