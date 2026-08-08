-- =============================================================================
-- CNPA V2 自定义赛制 R2-1：MatchSeries 数据模型
-- 前置：R1 + dynamic-format migration 已执行
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. basket_match_series
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_match_series (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  division_id BIGINT NOT NULL COMMENT '赛区ID',
  stage_id BIGINT NOT NULL COMMENT '阶段ID',
  schedule_batch_id BIGINT DEFAULT NULL COMMENT '赛程批次ID',
  series_code VARCHAR(64) NOT NULL COMMENT '系列赛业务编码（稳定标识）',
  round_no INT NOT NULL COMMENT '淘汰赛轮次',
  bracket_slot INT NOT NULL COMMENT '对阵槽位',
  home_event_team_id BIGINT NOT NULL COMMENT '主队赛事球队ID',
  away_event_team_id BIGINT NOT NULL COMMENT '客队赛事球队ID',
  best_of INT NOT NULL COMMENT 'BO 局数：仅 3/5',
  home_win_count INT NOT NULL DEFAULT 0 COMMENT '主队已确认胜场',
  away_win_count INT NOT NULL DEFAULT 0 COMMENT '客队已确认胜场',
  winner_event_team_id BIGINT DEFAULT NULL COMMENT '系列赛获胜球队ID',
  series_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '系列赛状态',
  source_series_id_home BIGINT DEFAULT NULL COMMENT '主队席位来源系列赛ID',
  source_series_id_away BIGINT DEFAULT NULL COMMENT '客队席位来源系列赛ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_series_code (series_code, is_delete),
  UNIQUE KEY uk_series_stage_round_slot (stage_id, round_no, bracket_slot, is_delete),
  KEY idx_series_division (division_id, is_delete),
  KEY idx_series_stage (stage_id, is_delete),
  KEY idx_series_batch (schedule_batch_id, is_delete),
  KEY idx_series_source_home (source_series_id_home),
  KEY idx_series_source_away (source_series_id_away)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 BO3/BO5 系列赛';

-- ---------------------------------------------------------------------------
-- 2. basket_match 关联系列赛
-- ---------------------------------------------------------------------------
ALTER TABLE basket_match
  ADD COLUMN series_id BIGINT DEFAULT NULL COMMENT '所属系列赛ID（BO1 为 NULL）' AFTER stage_id,
  ADD COLUMN series_game_no INT DEFAULT NULL COMMENT '系列赛场次序号（1~best_of）' AFTER series_id;

ALTER TABLE basket_match
  ADD KEY idx_match_series (series_id, series_game_no, is_delete);

-- 同一系列赛内 game_no 唯一（软删沿用 is_delete=0 活跃约束；删除行 is_delete=id 释放槽位）
ALTER TABLE basket_match
  ADD UNIQUE KEY uk_match_series_game (series_id, series_game_no, is_delete);
