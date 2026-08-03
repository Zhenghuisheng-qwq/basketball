-- =============================================================================
-- CNPA 赛事域 V4 第 1 轮：P0 新表 DDL
-- 分支：basketball-20260803
-- 前置：必须先完成旧同名表清理（见 cnpa-v4-r1-cleanup-draft.sql），再执行本脚本。
-- 枚举存储：新赛事域统一使用 VARCHAR 字符串枚举（与旧整型 code 隔离）。
--
-- 重要警告：
--   basket_match / basket_match_period_score / basket_match_team_stat /
--   basket_match_player_stat 与旧表同名。
--   若旧表仍存在，CREATE TABLE IF NOT EXISTS 不会重建结构，会导致新旧混用。
--   执行本脚本前必须先按清库草案删除上述旧同名表。
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. basket_series 赛事系列
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_series` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `series_code` VARCHAR(32) NOT NULL COMMENT '系列编码：LEAGUE/ARENA/CUP/OPEN',
  `series_name` VARCHAR(64) NOT NULL COMMENT '展示名',
  `series_desc` VARCHAR(512) DEFAULT NULL COMMENT '简介',
  `default_format_type` VARCHAR(32) NOT NULL DEFAULT 'REGULAR_ROUND_ROBIN' COMMENT '默认赛制',
  `icon_file_id` BIGINT DEFAULT NULL COMMENT '图标文件ID',
  `cover_file_id` BIGINT DEFAULT NULL COMMENT '封面文件ID',
  `banner_file_id` BIGINT DEFAULT NULL COMMENT 'Banner文件ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_series_code` (`series_code`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事系列';

-- ---------------------------------------------------------------------------
-- 2. basket_season 赛事赛季
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_season` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `series_id` BIGINT NOT NULL COMMENT '赛事系列ID',
  `season_year` INT NOT NULL COMMENT '赛季年份',
  `season_name` VARCHAR(128) NOT NULL COMMENT '赛季名称，如 S1',
  `display_title` VARCHAR(128) NOT NULL COMMENT '展示标题，如 CNPA League 2026 S1',
  `description` TEXT COMMENT '赛季说明',
  `cover_file_id` BIGINT DEFAULT NULL COMMENT '封面文件ID',
  `banner_file_id` BIGINT DEFAULT NULL COMMENT 'Banner文件ID',
  `registration_mode` VARCHAR(32) NOT NULL DEFAULT 'TEAM' COMMENT '报名方式：TEAM/PERSONAL，当前强制TEAM',
  `format_type` VARCHAR(32) NOT NULL DEFAULT 'REGULAR_ROUND_ROBIN' COMMENT '赛制',
  `registration_start_time` DATETIME DEFAULT NULL COMMENT '报名开始时间',
  `registration_end_time` DATETIME DEFAULT NULL COMMENT '报名结束时间',
  `match_start_time` DATETIME DEFAULT NULL COMMENT '比赛开始时间',
  `match_end_time` DATETIME DEFAULT NULL COMMENT '比赛结束时间',
  `publish_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态',
  `season_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '赛季状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_series_year_name` (`series_id`, `season_year`, `season_name`, `is_delete`),
  KEY `idx_season_series` (`series_id`),
  KEY `idx_season_publish_status` (`publish_status`, `season_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事赛季';

-- ---------------------------------------------------------------------------
-- 3. basket_division 平台赛区
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID',
  `platform_code` VARCHAR(32) NOT NULL COMMENT '平台：PC/PS/XBOX',
  `division_name` VARCHAR(128) NOT NULL COMMENT '赛区名称',
  `registration_mode` VARCHAR(32) NOT NULL DEFAULT 'TEAM' COMMENT '报名方式，当前强制TEAM',
  `format_type` VARCHAR(32) NOT NULL DEFAULT 'REGULAR_ROUND_ROBIN' COMMENT '赛制',
  `max_team_count` INT NOT NULL DEFAULT 16 COMMENT '队伍上限（独立名额）',
  `max_person_count` INT DEFAULT NULL COMMENT '个人报名上限预留',
  `lineup_min_count` INT NOT NULL DEFAULT 5 COMMENT '阵容最少人数',
  `lineup_max_count` INT NOT NULL DEFAULT 8 COMMENT '阵容最多人数',
  `registration_start_time` DATETIME DEFAULT NULL COMMENT '报名开始，空则继承Season',
  `registration_end_time` DATETIME DEFAULT NULL COMMENT '报名结束，空则继承Season',
  `match_start_time` DATETIME DEFAULT NULL COMMENT '比赛开始，空则继承Season',
  `match_end_time` DATETIME DEFAULT NULL COMMENT '比赛结束，空则继承Season',
  `division_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '赛区状态',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_season_platform` (`season_id`, `platform_code`, `is_delete`),
  KEY `idx_division_season` (`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4平台赛区';

-- ---------------------------------------------------------------------------
-- 4. basket_captain_grant 队长授权
-- 唯一约束说明（R1 补丁）：
--   不再使用 uk(division_id, team_id, grant_status, is_delete)，
--   避免多条 REVOKED/EXPIRED 历史记录互相冲突。
--   改用 active_unique_key：
--     - ACTIVE 且未删除：写入 CONCAT(division_id, '_', team_id)，如 12_34
--     - REVOKED / EXPIRED / 软删除：写 NULL（MySQL UNIQUE 允许多个 NULL）
--   业务写入时必须维护该字段，保证一队一区最多一条当前有效授权。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_captain_grant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `division_id` BIGINT NOT NULL COMMENT '平台赛区ID',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `team_id` BIGINT NOT NULL COMMENT '长期球队ID',
  `player_id` BIGINT NOT NULL COMMENT '队长球员ID',
  `member_id` BIGINT NOT NULL COMMENT '账号成员ID',
  `grant_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '授权状态：ACTIVE/REVOKED/EXPIRED',
  `active_unique_key` VARCHAR(128) DEFAULT NULL COMMENT '仅ACTIVE有效授权写入 divisionId_teamId；REVOKED/EXPIRED/软删除写NULL',
  `valid_from` DATETIME DEFAULT NULL COMMENT '生效时间',
  `valid_to` DATETIME DEFAULT NULL COMMENT '失效时间',
  `granted_by` BIGINT DEFAULT NULL COMMENT '授权管理员ID',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_captain_grant_active_unique_key` (`active_unique_key`),
  KEY `idx_captain_player` (`player_id`),
  KEY `idx_captain_team` (`team_id`),
  KEY `idx_captain_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4队长授权';

-- ---------------------------------------------------------------------------
-- 5. basket_registration 报名主表
-- 重提策略（R1 明确，保持本唯一键不变）：
--   uk_registration_division_team(division_id, team_id, is_delete)
--   表示同一队伍同一 Division 仅允许一条未删除报名主记录。
--   V4 报名撤回/驳回后：复用同一条 registration 重新提交（改状态/阵容/时间字段）；
--   历史操作记录写入 basket_registration_audit；
--   不新增第二条报名主记录。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_registration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '平台赛区ID',
  `registration_mode` VARCHAR(32) NOT NULL DEFAULT 'TEAM' COMMENT '报名方式',
  `team_id` BIGINT NOT NULL COMMENT '长期球队ID',
  `captain_player_id` BIGINT NOT NULL COMMENT '队长球员ID',
  `captain_member_id` BIGINT NOT NULL COMMENT '队长账号ID',
  `registration_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '报名状态',
  `lineup_count` INT NOT NULL DEFAULT 0 COMMENT '阵容人数',
  `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `approve_time` DATETIME DEFAULT NULL COMMENT '通过时间',
  `reject_reason` VARCHAR(512) DEFAULT NULL COMMENT '驳回原因',
  `withdraw_time` DATETIME DEFAULT NULL COMMENT '撤回时间',
  `audit_user_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_division_team` (`division_id`, `team_id`, `is_delete`),
  KEY `idx_registration_status` (`division_id`, `registration_status`),
  KEY `idx_registration_season` (`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4球队报名';

-- ---------------------------------------------------------------------------
-- 6. basket_registration_player 报名阵容
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_registration_player` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `registration_id` BIGINT NOT NULL COMMENT '报名ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `team_id` BIGINT NOT NULL COMMENT '长期球队ID',
  `player_id` BIGINT NOT NULL COMMENT '球员ID',
  `member_id` BIGINT DEFAULT NULL COMMENT '账号ID',
  `platform_identity_id` BIGINT DEFAULT NULL COMMENT '平台身份ID',
  `platform_code` VARCHAR(32) NOT NULL COMMENT '平台编码',
  `game_id_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '游戏ID快照',
  `player_name_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '昵称快照',
  `position` VARCHAR(32) DEFAULT NULL COMMENT '位置',
  `jersey_no` VARCHAR(32) DEFAULT NULL COMMENT '球衣号',
  `is_captain` TINYINT NOT NULL DEFAULT 0 COMMENT '是否队长',
  `is_starter` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首发',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_reg_player_registration` (`registration_id`),
  KEY `idx_reg_player_division_player` (`division_id`, `player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4报名阵容';

-- ---------------------------------------------------------------------------
-- 7. basket_registration_audit 报名审核流水
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_registration_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `registration_id` BIGINT NOT NULL COMMENT '报名ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `audit_action` VARCHAR(32) NOT NULL COMMENT '操作：SUBMIT/APPROVE/REJECT/WITHDRAW/CANCEL',
  `audit_result` VARCHAR(32) DEFAULT NULL COMMENT '审核结果：APPROVED/REJECTED',
  `before_status` VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
  `after_status` VARCHAR(32) DEFAULT NULL COMMENT '变更后状态',
  `audit_user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `audit_user_name` VARCHAR(128) DEFAULT NULL COMMENT '操作人名称',
  `audit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_reg_audit_registration` (`registration_id`),
  KEY `idx_reg_audit_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4报名审核流水';

-- ---------------------------------------------------------------------------
-- 8. basket_event_team 赛事球队快照
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_event_team` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `registration_id` BIGINT NOT NULL COMMENT '报名ID',
  `source_team_id` BIGINT NOT NULL COMMENT '来源长期球队ID',
  `team_name_snapshot` VARCHAR(128) NOT NULL COMMENT '队名快照',
  `team_logo_file_id` BIGINT DEFAULT NULL COMMENT '队标文件ID',
  `team_short_name` VARCHAR(64) DEFAULT NULL COMMENT '简称快照',
  `captain_player_id` BIGINT DEFAULT NULL COMMENT '队长球员ID',
  `captain_member_id` BIGINT DEFAULT NULL COMMENT '队长账号ID',
  `event_team_no` VARCHAR(64) DEFAULT NULL COMMENT '赛事队伍编号',
  `event_team_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '快照状态',
  `platform_code` VARCHAR(32) DEFAULT NULL COMMENT '平台冗余',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_team_division_source_team` (`division_id`, `source_team_id`, `is_delete`),
  KEY `idx_event_team_division` (`division_id`),
  KEY `idx_event_team_season` (`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事球队快照';

-- ---------------------------------------------------------------------------
-- 9. basket_event_player 赛事球员快照
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_event_player` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队快照ID',
  `source_player_id` BIGINT NOT NULL COMMENT '来源球员ID',
  `member_id` BIGINT DEFAULT NULL COMMENT '账号ID',
  `platform_identity_id` BIGINT DEFAULT NULL COMMENT '平台身份ID',
  `platform_code` VARCHAR(32) NOT NULL COMMENT '平台编码',
  `game_id_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '游戏ID快照',
  `player_name_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '昵称快照',
  `avatar_file_id` BIGINT DEFAULT NULL COMMENT '头像文件ID',
  `position` VARCHAR(32) DEFAULT NULL COMMENT '位置',
  `jersey_no` VARCHAR(32) DEFAULT NULL COMMENT '球衣号',
  `is_captain` TINYINT NOT NULL DEFAULT 0 COMMENT '是否队长',
  `is_starter` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首发',
  `event_player_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '快照状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_player_division_source_player` (`division_id`, `source_player_id`, `is_delete`),
  KEY `idx_event_player_team` (`event_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事球员快照';

-- ---------------------------------------------------------------------------
-- 10. basket_schedule_batch 赛程生成批次
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_schedule_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `batch_no` VARCHAR(64) DEFAULT NULL COMMENT '批次号',
  `format_type` VARCHAR(32) NOT NULL DEFAULT 'REGULAR_ROUND_ROBIN' COMMENT '赛制',
  `rule_code` VARCHAR(64) NOT NULL DEFAULT 'REGULAR_ROUND_ROBIN_V1' COMMENT '规则编码',
  `team_count` INT NOT NULL DEFAULT 0 COMMENT '参赛队数',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '比赛场数',
  `generate_type` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '生成方式',
  `generate_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '生成状态',
  `publish_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态',
  `generate_time` DATETIME DEFAULT NULL COMMENT '生成时间',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `generated_by` BIGINT DEFAULT NULL COMMENT '生成人',
  `published_by` BIGINT DEFAULT NULL COMMENT '发布人',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_batch_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛程生成批次';

-- ---------------------------------------------------------------------------
-- 11. basket_match 比赛主表（同名旧表必须先清理）
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
  KEY `idx_match_season` (`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4比赛主表';

-- ---------------------------------------------------------------------------
-- 12. basket_match_period_score 节比分
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 13. basket_match_submission 比赛数据提交
-- 版本唯一约束（R1 补丁）：
--   uk_submission_match_team_version(match_id, submit_event_team_id, version_no, is_delete)
--   保证同一比赛同一提交方同一版本仅一条未删除记录。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_match_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `submit_event_team_id` BIGINT NOT NULL COMMENT '提交方赛事球队ID',
  `opponent_event_team_id` BIGINT DEFAULT NULL COMMENT '对手赛事球队ID',
  `submit_event_player_id` BIGINT DEFAULT NULL COMMENT '提交球员快照ID',
  `submit_member_id` BIGINT DEFAULT NULL COMMENT '提交账号ID',
  `submitter_role` VARCHAR(32) DEFAULT NULL COMMENT '提交角色',
  `submission_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '提交状态',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `audit_user_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_user_name` VARCHAR(128) DEFAULT NULL COMMENT '审核人名称',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `reject_reason` VARCHAR(512) DEFAULT NULL COMMENT '驳回原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_match_team_version` (`match_id`, `submit_event_team_id`, `version_no`, `is_delete`),
  KEY `idx_submission_match_team` (`match_id`, `submit_event_team_id`),
  KEY `idx_submission_division` (`division_id`, `submission_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4比赛数据提交';

-- ---------------------------------------------------------------------------
-- 14. basket_match_submission_team_stat
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_match_submission_team_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id` BIGINT NOT NULL COMMENT '提交ID',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `score` INT NOT NULL DEFAULT 0 COMMENT '得分',
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
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_team_stat` (`submission_id`, `event_team_id`, `is_delete`),
  KEY `idx_sub_team_stat_match` (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4提交版球队数据';

-- ---------------------------------------------------------------------------
-- 15. basket_match_submission_player_stat
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_match_submission_player_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id` BIGINT NOT NULL COMMENT '提交ID',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `event_player_id` BIGINT NOT NULL COMMENT '赛事球员ID',
  `points` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `rebound` INT NOT NULL DEFAULT 0 COMMENT '篮板',
  `assist` INT NOT NULL DEFAULT 0 COMMENT '助攻',
  `steal` INT NOT NULL DEFAULT 0 COMMENT '抢断',
  `block` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnover` INT NOT NULL DEFAULT 0 COMMENT '失误',
  `foul` INT NOT NULL DEFAULT 0 COMMENT '犯规',
  `minutes` DECIMAL(6,2) DEFAULT NULL COMMENT '出场时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_player_stat` (`submission_id`, `event_player_id`, `is_delete`),
  KEY `idx_sub_player_stat_match` (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4提交版球员数据';

-- ---------------------------------------------------------------------------
-- 16. basket_match_submission_material
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_match_submission_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id` BIGINT NOT NULL COMMENT '提交ID',
  `match_id` BIGINT NOT NULL COMMENT '比赛ID',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `material_type` VARCHAR(32) NOT NULL DEFAULT 'IMAGE' COMMENT '材料类型',
  `file_id` BIGINT NOT NULL COMMENT '文件ID',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_sub_material_submission` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4提交材料';

-- ---------------------------------------------------------------------------
-- 17. basket_match_team_stat 单场正式球队统计
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 18. basket_match_player_stat 单场正式球员统计
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 19. basket_standing 积分榜
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_standing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `rank_no` INT NOT NULL DEFAULT 0 COMMENT '排名',
  `win_count` INT NOT NULL DEFAULT 0 COMMENT '胜场',
  `lose_count` INT NOT NULL DEFAULT 0 COMMENT '负场',
  `draw_count` INT NOT NULL DEFAULT 0 COMMENT '平场预留',
  `point_for` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `point_against` INT NOT NULL DEFAULT 0 COMMENT '失分',
  `point_diff` INT NOT NULL DEFAULT 0 COMMENT '净胜分',
  `standing_points` INT NOT NULL DEFAULT 0 COMMENT '积分',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_standing_division_team` (`division_id`, `event_team_id`, `is_delete`),
  KEY `idx_standing_rank` (`division_id`, `rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4积分榜';

-- ---------------------------------------------------------------------------
-- 20. basket_team_stat 赛区球队累计统计
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_team_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '场次',
  `win_count` INT NOT NULL DEFAULT 0 COMMENT '胜场',
  `lose_count` INT NOT NULL DEFAULT 0 COMMENT '负场',
  `points_for` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `points_against` INT NOT NULL DEFAULT 0 COMMENT '失分',
  `avg_points_for` DECIMAL(10,2) DEFAULT NULL COMMENT '场均得分',
  `avg_points_against` DECIMAL(10,2) DEFAULT NULL COMMENT '场均失分',
  `rebound` INT NOT NULL DEFAULT 0 COMMENT '篮板',
  `assist` INT NOT NULL DEFAULT 0 COMMENT '助攻',
  `steal` INT NOT NULL DEFAULT 0 COMMENT '抢断',
  `block` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnover` INT NOT NULL DEFAULT 0 COMMENT '失误',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_stat_division_team` (`division_id`, `event_team_id`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛区球队累计统计';

-- ---------------------------------------------------------------------------
-- 21. basket_player_stat 赛区球员累计统计
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_player_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `event_team_id` BIGINT NOT NULL COMMENT '赛事球队ID',
  `event_player_id` BIGINT NOT NULL COMMENT '赛事球员ID',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '场次',
  `points` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `avg_points` DECIMAL(10,2) DEFAULT NULL COMMENT '场均得分',
  `rebound` INT NOT NULL DEFAULT 0 COMMENT '篮板',
  `avg_rebound` DECIMAL(10,2) DEFAULT NULL COMMENT '场均篮板',
  `assist` INT NOT NULL DEFAULT 0 COMMENT '助攻',
  `avg_assist` DECIMAL(10,2) DEFAULT NULL COMMENT '场均助攻',
  `steal` INT NOT NULL DEFAULT 0 COMMENT '抢断',
  `block` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnover` INT NOT NULL DEFAULT 0 COMMENT '失误',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_player_stat_division_player` (`division_id`, `event_player_id`, `is_delete`),
  KEY `idx_player_stat_team` (`event_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛区球员累计统计';

-- ---------------------------------------------------------------------------
-- 22. basket_rank 排行榜
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_rank` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `season_id` BIGINT NOT NULL COMMENT '赛季ID冗余',
  `division_id` BIGINT NOT NULL COMMENT '赛区ID',
  `rank_type` VARCHAR(32) NOT NULL COMMENT '榜单类型',
  `rank_no` INT NOT NULL DEFAULT 0 COMMENT '排名',
  `target_type` VARCHAR(32) NOT NULL COMMENT '对象类型：TEAM/PLAYER',
  `event_team_id` BIGINT DEFAULT NULL COMMENT '赛事球队ID',
  `event_player_id` BIGINT DEFAULT NULL COMMENT '赛事球员ID',
  `rank_value` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '榜单值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_rank_division_type` (`division_id`, `rank_type`, `rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4排行榜';

-- =============================================================================
-- R1 DDL 小补丁（增量 ALTER）
-- 适用：若已执行过本文件早期版本建表，可单独执行以下语句；禁止 DROP/TRUNCATE。
-- 若尚未建表，直接执行上方 CREATE 即可，无需再跑本节。
-- =============================================================================

-- 4. basket_captain_grant：active_unique_key 方案
-- ALTER TABLE `basket_captain_grant` DROP INDEX `uk_active_captain_grant`;
-- ALTER TABLE `basket_captain_grant`
--   ADD COLUMN `active_unique_key` VARCHAR(128) DEFAULT NULL
--     COMMENT '仅ACTIVE有效授权写入 divisionId_teamId；REVOKED/EXPIRED/软删除写NULL'
--     AFTER `grant_status`;
-- ALTER TABLE `basket_captain_grant`
--   ADD UNIQUE KEY `uk_captain_grant_active_unique_key` (`active_unique_key`);

-- 13. basket_match_submission：版本唯一约束
-- ALTER TABLE `basket_match_submission`
--   ADD UNIQUE KEY `uk_submission_match_team_version` (`match_id`, `submit_event_team_id`, `version_no`, `is_delete`);
