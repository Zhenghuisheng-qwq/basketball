-- =============================================================================
-- CNPA V2 最终权威赛事域 Schema（新环境初始化）
-- 生成轮次：R5 最终收口
-- 说明：
--   1. 本文件为赛事域最终结构权威参考，不含 R1~R5 过渡字段
--   2. 不含 rank_explain_json（已删除，排名解释使用 basket_standing_rank_reason）
--   3. 不含旧 Tournament 赛事业务表（registration/roster/schedule_generate 等）
--   4. 保留 basket_tournament / basket_tournament_season / basket_tournament_admin_scope
--      仅供荣誉生成与纪律 scope 只读引用，不属于赛事写链路
--   5. 完整建表语句来源：按序执行
--      docs/sql/cnpa-v4-r1-ddl.sql
--      docs/sql/cnpa-dynamic-format-r1-migration.sql
--      docs/sql/cnpa-dynamic-format-r2-migration.sql
--      docs/sql/cnpa-dynamic-format-r3-migration.sql
--      docs/sql/cnpa-dynamic-format-r4-migration.sql
--      再执行 docs/sql/cnpa-dynamic-format-r5-cleanup.sql 收紧约束
--   6. 不含若依 sys_* 系统表
-- =============================================================================
SET NAMES utf8mb4;

-- 赛事域权威表清单（共 38 张核心业务表）：
-- 基础：basket_series, basket_season, basket_division
-- 规则：basket_format_template, basket_format_template_stage,
--       basket_format_template_tie_break_rule, basket_division_format_snapshot,
--       basket_division_stage, basket_division_tie_break_rule
-- 报名：basket_registration, basket_registration_player, basket_registration_audit
-- 个人：basket_personal_registration, basket_personal_registration_audit,
--       basket_personal_draw_batch, basket_personal_draw_team, basket_personal_draw_assignment
-- 参赛：basket_event_team, basket_event_player
-- 分组：basket_division_grouping_batch, basket_division_group, basket_division_group_team
-- 阶段：basket_division_stage_team, basket_division_stage_qualification
-- 赛程：basket_schedule_batch, basket_match, basket_match_submission,
--       basket_match_submission_team_stat, basket_match_submission_player_stat,
--       basket_match_submission_material
-- 统计：basket_standing, basket_standing_rank_reason, basket_team_stat,
--       basket_player_stat, basket_rank
-- 授权：basket_captain_grant, basket_captain_grant_log

-- 核心关系：
-- Series → Season → Division → FormatSnapshot → DivisionStage
-- Registration → EventTeam/EventPlayer
-- DivisionStage → ScheduleBatch → Match → Submission → Standing → Qualification

-- 后续扩展字段（已存在于模板/阶段/比赛表，BO3/BO5 专项未启用）：
-- best_of, stage_type, competition_format, next_stage_code,
-- advancement_type, advancement_count, pairing_rule,
-- source_stage_id, source_match_id_home, source_match_id_away,
-- round_no, bracket_slot

-- 示例：basket_standing 最终结构（无 rank_explain_json）
CREATE TABLE IF NOT EXISTS `basket_standing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `stage_id` BIGINT NOT NULL COMMENT '阶段ID',
  `group_id` BIGINT DEFAULT NULL COMMENT '小组ID',
  `group_scope_key` BIGINT NOT NULL DEFAULT 0 COMMENT '分组范围键',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `rank_no` INT DEFAULT NULL COMMENT '排名',
  `win_count` INT NOT NULL DEFAULT 0,
  `lose_count` INT NOT NULL DEFAULT 0,
  `draw_count` INT NOT NULL DEFAULT 0,
  `point_for` INT NOT NULL DEFAULT 0,
  `point_against` INT NOT NULL DEFAULT 0,
  `point_diff` INT NOT NULL DEFAULT 0,
  `standing_points` INT NOT NULL DEFAULT 0,
  `manual_rank_pending` TINYINT NOT NULL DEFAULT 0,
  `manual_rank_no` INT DEFAULT NULL,
  `manual_rank_reason` VARCHAR(512) DEFAULT NULL,
  `manual_rank_by` BIGINT DEFAULT NULL,
  `manual_rank_time` DATETIME DEFAULT NULL,
  `active_unique_key` VARCHAR(128) DEFAULT NULL COMMENT '有效唯一键',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_standing_active` (`active_unique_key`),
  KEY `idx_standing_stage_scope` (`stage_id`, `group_scope_key`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4阶段积分榜';

CREATE TABLE IF NOT EXISTS `basket_standing_rank_reason` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `standing_id` BIGINT NOT NULL COMMENT '积分榜ID',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `priority_no` INT NOT NULL COMMENT '优先级',
  `reason_text` VARCHAR(512) NOT NULL COMMENT '解释文本',
  `compare_value` VARCHAR(128) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_rank_reason_standing` (`standing_id`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分榜排名解释';
