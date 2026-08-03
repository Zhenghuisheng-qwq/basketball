-- CNPA V4 Round4: 4 张旧 match 同名表 backup + cleanup + V4 重建
-- 执行前确认：仅处理 basket_match / basket_match_period_score / basket_match_team_stat / basket_match_player_stat
-- 不清理账号、球员、长期球队、报名、EventTeam、EventPlayer 等表

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 备份旧表（bak_r4_ 前缀，可重复执行前先删旧备份）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS bak_r4_basket_match_player_stat;
DROP TABLE IF EXISTS bak_r4_basket_match_team_stat;
DROP TABLE IF EXISTS bak_r4_basket_match_period_score;
DROP TABLE IF EXISTS bak_r4_basket_match;

CREATE TABLE bak_r4_basket_match AS SELECT * FROM basket_match;
CREATE TABLE bak_r4_basket_match_period_score AS SELECT * FROM basket_match_period_score;
CREATE TABLE bak_r4_basket_match_team_stat AS SELECT * FROM basket_match_team_stat;
CREATE TABLE bak_r4_basket_match_player_stat AS SELECT * FROM basket_match_player_stat;

-- ---------------------------------------------------------------------------
-- 2. DROP 旧结构（按依赖从叶子到根）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS basket_match_player_stat;
DROP TABLE IF EXISTS basket_match_team_stat;
DROP TABLE IF EXISTS basket_match_period_score;
DROP TABLE IF EXISTS basket_match;

-- ---------------------------------------------------------------------------
-- 3. 重建 V4 结构（摘自 cnpa-v4-r1-ddl.sql）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_match` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `schedule_batch_id` BIGINT DEFAULT NULL COMMENT '赛程批次ID',
  `round_no` INT NOT NULL DEFAULT 0 COMMENT '轮次',
  `match_no` VARCHAR(64) NOT NULL COMMENT '比赛编号',
  `stage_code` VARCHAR(32) NOT NULL DEFAULT 'REGULAR' COMMENT '阶段编码',
  `match_time` DATETIME DEFAULT NULL COMMENT '比赛时间',
  `home_event_team_id` BIGINT NOT NULL COMMENT '主队赛事快照ID',
  `away_event_team_id` BIGINT NOT NULL COMMENT '客队赛事快照ID',
  `home_score` INT DEFAULT NULL COMMENT '主队得分',
  `away_score` INT DEFAULT NULL COMMENT '客队得分',
  `winner_event_team_id` BIGINT DEFAULT NULL COMMENT '胜方赛事球队ID',
  `match_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '比赛状态',
  `publish_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态',
  `platform_code` VARCHAR(32) DEFAULT NULL COMMENT '平台冗余',
  `venue_text` VARCHAR(128) DEFAULT NULL COMMENT '场地文案',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_no` (`match_no`, `is_delete`),
  KEY `idx_match_division_time` (`division_id`, `match_time`, `publish_status`),
  KEY `idx_match_team` (`home_event_team_id`, `away_event_team_id`),
  KEY `idx_match_season` (`season_id`),
  KEY `idx_match_batch` (`schedule_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4比赛主表';

CREATE TABLE IF NOT EXISTS `basket_match_period_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `period_no` INT NOT NULL COMMENT '节次',
  `home_score` INT NOT NULL DEFAULT 0 COMMENT '主队本节得分',
  `away_score` INT NOT NULL DEFAULT 0 COMMENT '客队本节得分',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_period` (`match_id`, `period_no`, `is_delete`),
  KEY `idx_period_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4比赛节比分';

CREATE TABLE IF NOT EXISTS `basket_match_team_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `opponent_event_team_id` BIGINT DEFAULT NULL COMMENT '对手赛事球队ID',
  `is_winner` TINYINT NOT NULL DEFAULT 0 COMMENT '是否胜方',
  `score` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `points_for` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `points_against` INT NOT NULL DEFAULT 0 COMMENT '失分',
  `field_goal_made` INT NOT NULL DEFAULT 0 COMMENT '投篮命中',
  `field_goal_attempt` INT NOT NULL DEFAULT 0 COMMENT '投篮出手',
  `three_point_made` INT NOT NULL DEFAULT 0 COMMENT '三分命中',
  `three_point_attempt` INT NOT NULL DEFAULT 0 COMMENT '三分出手',
  `free_throw_made` INT NOT NULL DEFAULT 0 COMMENT '罚球命中',
  `free_throw_attempt` INT NOT NULL DEFAULT 0 COMMENT '罚球出手',
  `rebound` INT NOT NULL DEFAULT 0 COMMENT '篮板',
  `assist` INT NOT NULL DEFAULT 0 COMMENT '助攻',
  `steal` INT NOT NULL DEFAULT 0 COMMENT '抢断',
  `block` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnover` INT NOT NULL DEFAULT 0 COMMENT '失误',
  `foul` INT NOT NULL DEFAULT 0 COMMENT '犯规',
  `source_submission_id` BIGINT DEFAULT NULL COMMENT '来源提交ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_team_stat` (`match_id`, `event_team_id`, `is_delete`),
  KEY `idx_match_team_stat_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4单场正式球队统计';

CREATE TABLE IF NOT EXISTS `basket_match_player_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `event_player_id` BIGINT NOT NULL COMMENT '赛事球员ID',
  `points` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `score` INT NOT NULL DEFAULT 0 COMMENT '得分别名兼容',
  `rebound` INT NOT NULL DEFAULT 0 COMMENT '篮板',
  `assist` INT NOT NULL DEFAULT 0 COMMENT '助攻',
  `steal` INT NOT NULL DEFAULT 0 COMMENT '抢断',
  `block` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnover` INT NOT NULL DEFAULT 0 COMMENT '失误',
  `foul` INT NOT NULL DEFAULT 0 COMMENT '犯规',
  `minutes` DECIMAL(6,2) DEFAULT NULL COMMENT '出场时间',
  `source_submission_id` BIGINT DEFAULT NULL COMMENT '来源提交ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_player_stat` (`match_id`, `event_player_id`, `is_delete`),
  KEY `idx_match_player_stat_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4单场正式球员统计';
