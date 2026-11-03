-- CNPA 数据中心 V2 第 2 轮：试算作业与资格结果
-- 试算行的 snapshot_id 为空，不作为官网公开指针。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS basket_leaderboard_run (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  division_id BIGINT NOT NULL,
  scope_key VARCHAR(64) NOT NULL,
  policy_id BIGINT NOT NULL,
  purpose VARCHAR(32) NOT NULL COMMENT 'PREVIEW/PUBLISH/RECALC',
  source_revision VARCHAR(128) NULL COMMENT '本次读取的正式统计修订号',
  state VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILED',
  error_message VARCHAR(1000) NULL,
  started_time DATETIME NOT NULL,
  finished_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lb_run_scope (division_id, scope_key, policy_id, source_revision),
  KEY idx_lb_run_policy (policy_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心排行重算作业';

CREATE TABLE IF NOT EXISTS basket_leaderboard_entry (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  run_id BIGINT NOT NULL,
  snapshot_id BIGINT NULL COMMENT '正式快照 ID，试算为空',
  division_id BIGINT NOT NULL,
  scope_key VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL COMMENT 'TEAM/PLAYER',
  event_team_id BIGINT NOT NULL,
  event_player_id BIGINT NOT NULL DEFAULT 0 COMMENT '球队行为 0',
  eligible TINYINT NOT NULL DEFAULT 0,
  failed_conditions VARCHAR(512) NULL COMMENT '逗号分隔的排除编码',
  valid_games INT NOT NULL DEFAULT 0,
  played_rounds INT NOT NULL DEFAULT 0,
  played_series INT NOT NULL DEFAULT 0,
  due_fixture_rate DECIMAL(10,4) NULL,
  required_games INT NULL,
  rank_no INT NULL COMMENT '默认得分场均名次，公开接口会按请求重排',
  points INT NOT NULL DEFAULT 0,
  rebound INT NOT NULL DEFAULT 0,
  assist INT NOT NULL DEFAULT 0,
  steal INT NOT NULL DEFAULT 0,
  block INT NOT NULL DEFAULT 0,
  turnover INT NOT NULL DEFAULT 0,
  wins INT NOT NULL DEFAULT 0,
  losses INT NOT NULL DEFAULT 0,
  field_goal_made INT NOT NULL DEFAULT 0,
  field_goal_attempt INT NOT NULL DEFAULT 0,
  three_point_made INT NOT NULL DEFAULT 0,
  three_point_attempt INT NOT NULL DEFAULT 0,
  free_throw_made INT NOT NULL DEFAULT 0,
  free_throw_attempt INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lb_entry_run_subject (run_id, subject_type, event_team_id, event_player_id),
  KEY idx_lb_entry_snapshot (snapshot_id, subject_type, eligible),
  KEY idx_lb_entry_run (run_id, subject_type, eligible)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心排行资格与指标';
