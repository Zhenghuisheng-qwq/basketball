-- 直接恢复 900020001 为 CONFIRMED（64:55），供 M06 联调
SET @match_id := 900020001;
SET @season_id := 1;
SET @stage_id := 900030001;
SET @home := 900010001;
SET @away := 900010002;

DELETE FROM basket_match_data_submission WHERE match_id = @match_id;
DELETE FROM basket_stat_sync_record WHERE match_id = @match_id;
DELETE FROM basket_match_official_result WHERE match_id = @match_id;
DELETE FROM basket_match_player_stat WHERE match_id = @match_id;
DELETE FROM basket_match_team_stat WHERE match_id = @match_id;
DELETE FROM basket_match_period_score WHERE match_id = @match_id;
DELETE FROM basket_match_data_dispute_material WHERE match_id = @match_id;
DELETE FROM basket_match_data_dispute WHERE match_id = @match_id;

UPDATE basket_match SET
  data_status = 2, home_score = 64, away_score = 55, winner_team_id = @home,
  match_status = 3, result_status = 1, score_confirmed = 1, update_time = NOW()
WHERE id = @match_id;

UPDATE basket_match_team SET score = 64, result_type = 'WIN', is_winner = 1 WHERE match_id = @match_id AND team_id = @home;
UPDATE basket_match_team SET score = 55, result_type = 'LOSS', is_winner = 0 WHERE match_id = @match_id AND team_id = @away;

INSERT INTO basket_match_period_score (match_id, period_no, period_type, home_score, away_score, period_status, create_time, update_time, is_delete)
VALUES
(@match_id, 1, 'REGULAR', 17, 23, 1, NOW(), NOW(), 0),
(@match_id, 2, 'REGULAR', 17, 8, 1, NOW(), NOW(), 0),
(@match_id, 3, 'REGULAR', 16, 15, 1, NOW(), NOW(), 0),
(@match_id, 4, 'REGULAR', 14, 9, 1, NOW(), NOW(), 0);

INSERT INTO basket_match_team_stat (match_id, season_id, stage_id, team_id, opponent_team_id, points, rebounds, offensive_rebounds, defensive_rebounds, assists, steals, blocks, turnovers, fouls, field_goals_made, field_goals_attempted, field_goal_pct, three_points_made, three_points_attempted, three_point_pct, free_throws_made, free_throws_attempted, free_throw_pct, possessions, efficiency, data_status, create_time, update_time, is_delete)
VALUES
(@match_id, @season_id, @stage_id, @home, @away, 64, 20, 0, 20, 19, 6, 3, 0, 0, 26, 46, 0.5652, 5, 15, 0.3333, 7, 9, 0.7778, 0, 0, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @away, @home, 55, 9, 0, 9, 10, 3, 1, 0, 0, 23, 28, 0.8214, 6, 8, 0.7500, 3, 3, 1.0000, 0, 0, 2, NOW(), NOW(), 0);

INSERT INTO basket_match_player_stat (match_id, season_id, stage_id, team_id, player_id, points, rebounds, offensive_rebounds, defensive_rebounds, assists, steals, blocks, turnovers, fouls, field_goals_made, field_goals_attempted, field_goal_pct, three_points_made, three_points_attempted, three_point_pct, free_throws_made, free_throws_attempted, free_throw_pct, data_status, create_time, update_time, is_delete)
VALUES
(@match_id, @season_id, @stage_id, @home, 2081010979671425026, 18, 5, 0, 5, 6, 2, 0, 0, 0, 7, 12, 0.5833, 2, 5, 0.4000, 2, 2, 1.0000, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @home, 2081041427005038596, 14, 8, 0, 8, 2, 1, 1, 0, 0, 6, 10, 0.6000, 0, 1, 0.0000, 2, 3, 0.6667, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @home, 2081072741716336642, 12, 4, 0, 4, 4, 1, 1, 0, 0, 5, 9, 0.5556, 1, 3, 0.3333, 1, 2, 0.5000, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @home, 2081073127508418562, 10, 2, 0, 2, 3, 1, 1, 0, 0, 4, 8, 0.5000, 2, 4, 0.5000, 0, 0, 0.0000, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @home, 2081073380093599747, 10, 1, 0, 1, 4, 1, 0, 0, 0, 4, 7, 0.5714, 0, 2, 0.0000, 2, 2, 1.0000, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @away, 2081019227996807169, 20, 3, 0, 3, 5, 1, 0, 0, 0, 8, 11, 0.7273, 4, 5, 0.8000, 0, 0, 0.0000, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @away, 2082478335308455940, 18, 4, 0, 4, 2, 1, 1, 0, 0, 7, 9, 0.7778, 2, 3, 0.6667, 2, 2, 1.0000, 2, NOW(), NOW(), 0),
(@match_id, @season_id, @stage_id, @away, 2082786571525836803, 17, 2, 0, 2, 3, 1, 0, 0, 0, 8, 8, 1.0000, 0, 0, 0.0000, 1, 1, 1.0000, 2, NOW(), NOW(), 0);

INSERT INTO basket_match_official_result (match_id, data_version, home_score, away_score, winner_team_id, loser_team_id, is_draw, result_type, effective_time, confirmed_by, confirmed_time, is_current, is_public, revision_no, create_time, update_time, is_delete)
VALUES (@match_id, 1, 64, 55, @home, @away, 0, 'NORMAL', NOW(), 1, NOW(), 1, 1, 1, NOW(), NOW(), 0);

INSERT INTO basket_stat_sync_record (match_id, official_result_id, sync_type, sync_status, idempotency_key, create_time, update_time, is_delete)
SELECT @match_id, id, 'SUBMISSION_APPROVE', 'SUCCESS', CONCAT('restore-', @match_id, '-', id), NOW(), NOW(), 0
FROM basket_match_official_result WHERE match_id = @match_id AND is_current = 1 LIMIT 1;
