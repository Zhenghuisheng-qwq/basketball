-- CNPA Series Submission V1.0 additive migration
SET NAMES utf8mb4;

ALTER TABLE basket_match_submission_player_stat
  ADD COLUMN field_goal_made INT NOT NULL DEFAULT 0 COMMENT 'FGM' AFTER foul,
  ADD COLUMN field_goal_attempt INT NOT NULL DEFAULT 0 COMMENT 'FGA' AFTER field_goal_made,
  ADD COLUMN three_point_made INT NOT NULL DEFAULT 0 COMMENT '3PM' AFTER field_goal_attempt,
  ADD COLUMN three_point_attempt INT NOT NULL DEFAULT 0 COMMENT '3PA' AFTER three_point_made,
  ADD COLUMN free_throw_made INT NOT NULL DEFAULT 0 COMMENT 'FTM' AFTER three_point_attempt,
  ADD COLUMN free_throw_attempt INT NOT NULL DEFAULT 0 COMMENT 'FTA' AFTER free_throw_made;

ALTER TABLE basket_match_player_stat
  ADD COLUMN field_goal_made INT NOT NULL DEFAULT 0 COMMENT 'FGM' AFTER foul,
  ADD COLUMN field_goal_attempt INT NOT NULL DEFAULT 0 COMMENT 'FGA' AFTER field_goal_made,
  ADD COLUMN three_point_made INT NOT NULL DEFAULT 0 COMMENT '3PM' AFTER field_goal_attempt,
  ADD COLUMN three_point_attempt INT NOT NULL DEFAULT 0 COMMENT '3PA' AFTER three_point_made,
  ADD COLUMN free_throw_made INT NOT NULL DEFAULT 0 COMMENT 'FTM' AFTER three_point_attempt,
  ADD COLUMN free_throw_attempt INT NOT NULL DEFAULT 0 COMMENT 'FTA' AFTER free_throw_made;

ALTER TABLE basket_match_submission
  ADD COLUMN series_submission_id BIGINT DEFAULT NULL COMMENT 'series submission id' AFTER submit_remark,
  ADD COLUMN submission_mode VARCHAR(32) NOT NULL DEFAULT 'LEGACY_TEAM_SIDE' COMMENT 'LEGACY_TEAM_SIDE / FULL_MATCH' AFTER series_submission_id,
  ADD COLUMN draft_complete TINYINT NOT NULL DEFAULT 0 COMMENT 'draft complete flag' AFTER submission_mode;

ALTER TABLE basket_match_submission
  ADD KEY idx_submission_series_task (series_submission_id);

CREATE TABLE IF NOT EXISTS basket_match_series_submission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  match_series_id BIGINT NOT NULL,
  season_id BIGINT DEFAULT NULL,
  division_id BIGINT NOT NULL,
  stage_id BIGINT DEFAULT NULL,
  submit_event_team_id BIGINT DEFAULT NULL,
  submit_member_id BIGINT DEFAULT NULL,
  declared_home_win_count INT DEFAULT NULL,
  declared_away_win_count INT DEFAULT NULL,
  actual_game_count INT DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  version_no INT NOT NULL DEFAULT 1,
  submit_time DATETIME DEFAULT NULL,
  audit_user_id BIGINT DEFAULT NULL,
  audit_user_name VARCHAR(128) DEFAULT NULL,
  audit_time DATETIME DEFAULT NULL,
  audit_remark VARCHAR(512) DEFAULT NULL,
  reject_reason VARCHAR(512) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_series_submission_version (match_series_id, version_no, is_delete),
  KEY idx_series_submission_status (division_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS basket_match_submission_period_score (
  id BIGINT NOT NULL AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  match_id BIGINT NOT NULL,
  division_id BIGINT NOT NULL,
  period_no INT NOT NULL,
  home_score INT NOT NULL DEFAULT 0,
  away_score INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_submission_period (submission_id, period_no, is_delete),
  KEY idx_sub_period_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
