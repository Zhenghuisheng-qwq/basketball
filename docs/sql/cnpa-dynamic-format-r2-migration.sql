-- =============================================================================
-- CNPA 动态赛制重构 第 2 轮：DDL 迁移
-- 文件：cnpa-dynamic-format-r2-migration.sql
--
-- 执行顺序：
--   1. 确认 R1 migration + seed 已执行
--   2. 执行本脚本
--   3. 按需执行 cnpa-dynamic-format-r2-backfill.sql（默认关闭自动执行）
--   4. 执行 cnpa-dynamic-format-r2-data-check.sql
--
-- 警告：
--   - 不在生产环境自动执行
--   - 不删除 V2 旧表
--   - 不自动随机抽签/分组
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. R1 修复：basket_division_format_snapshot 版本与当前唯一键
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_division_format_snapshot`
  ADD COLUMN `snapshot_version` INT NOT NULL DEFAULT 1
    COMMENT '快照版本号，同一 Division 内从1递增' AFTER `source_template_version`,
  ADD COLUMN `is_current` TINYINT NOT NULL DEFAULT 1
    COMMENT '是否当前有效快照：1是 0否' AFTER `snapshot_status`,
  ADD COLUMN `active_unique_key` VARCHAR(128) DEFAULT NULL
    COMMENT '当前快照唯一键：DIVISION:{divisionId}；历史快照为NULL' AFTER `is_current`,
  ADD COLUMN `superseded_snapshot_id` BIGINT DEFAULT NULL
    COMMENT '被本快照替代的上一快照ID' AFTER `active_unique_key`,
  ADD COLUMN `superseded_time` DATETIME DEFAULT NULL
    COMMENT '被替代为非当前的时间' AFTER `superseded_snapshot_id`;

-- 初始化已有数据：有效记录标记为 current
UPDATE `basket_division_format_snapshot`
SET `is_current` = 1,
    `active_unique_key` = CONCAT('DIVISION:', `division_id`),
    `snapshot_version` = 1
WHERE `is_delete` = 0;

UPDATE `basket_division_format_snapshot`
SET `is_current` = 0,
    `active_unique_key` = NULL
WHERE `is_delete` = 1;

-- 替换错误唯一约束：不再使用 (division_id, is_delete)
ALTER TABLE `basket_division_format_snapshot`
  DROP INDEX `uk_division_snapshot_active`,
  ADD UNIQUE KEY `uk_division_current_snapshot` (`active_unique_key`),
  ADD KEY `idx_division_snapshot_version` (`division_id`, `snapshot_version`);

-- ---------------------------------------------------------------------------
-- 2. R1 修复：模板版本永久唯一（不含 is_delete）
-- ---------------------------------------------------------------------------
-- 注意：若已存在同 code+version 的多条逻辑删除数据，需先人工清理后再执行
ALTER TABLE `basket_format_template`
  DROP INDEX `uk_template_code_version`,
  ADD UNIQUE KEY `uk_template_code_version` (`template_code`, `version_no`);

-- ---------------------------------------------------------------------------
-- 3. basket_registration 来源扩展（TEAM / PERSONAL_FORMATION 统一转换源）
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_registration`
  ADD COLUMN `registration_source` VARCHAR(32) NOT NULL DEFAULT 'TEAM_DIRECT'
    COMMENT '报名来源：TEAM_DIRECT/PERSONAL_FORMATION' AFTER `registration_mode`,
  ADD COLUMN `source_personal_draw_team_id` BIGINT DEFAULT NULL
    COMMENT '来源个人抽签临时队伍ID' AFTER `registration_source`,
  ADD COLUMN `source_personal_draw_batch_id` BIGINT DEFAULT NULL
    COMMENT '来源个人抽签批次ID' AFTER `source_personal_draw_team_id`;

ALTER TABLE `basket_registration`
  ADD KEY `idx_registration_source` (`registration_source`),
  ADD KEY `idx_registration_draw_team` (`source_personal_draw_team_id`);

-- ---------------------------------------------------------------------------
-- 4. 个人报名主表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_personal_registration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `season_id` BIGINT NOT NULL COMMENT 'Season ID',
  `series_id` BIGINT NOT NULL COMMENT 'Series ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `player_id` BIGINT NOT NULL COMMENT '球员ID',
  `player_platform_id` BIGINT NOT NULL COMMENT '球员平台身份ID',
  `actual_platform_code` VARCHAR(32) NOT NULL COMMENT '实际平台编码：PC/PS/XBOX',
  `game_account_id` VARCHAR(128) DEFAULT NULL COMMENT '游戏账号快照',
  `preferred_position` VARCHAR(32) DEFAULT NULL COMMENT '偏好位置',
  `registration_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING_REVIEW/APPROVED/REJECTED/WITHDRAWN/ASSIGNED/CONVERTED',
  `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `approved_time` DATETIME DEFAULT NULL COMMENT '审核通过时间',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
  `assigned_draw_team_id` BIGINT DEFAULT NULL COMMENT '已分配临时队伍ID',
  `converted_registration_id` BIGINT DEFAULT NULL COMMENT '转换后的正式报名ID',
  `active_unique_key` VARCHAR(128) DEFAULT NULL COMMENT '当前有效唯一键：DIVISION:{divisionId}:PLAYER:{playerId}',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_personal_reg_active` (`active_unique_key`),
  KEY `idx_personal_reg_division` (`division_id`, `registration_status`),
  KEY `idx_personal_reg_player` (`player_id`),
  KEY `idx_personal_reg_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4个人报名';

-- ---------------------------------------------------------------------------
-- 5. 个人报名审核流水
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_personal_registration_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `personal_registration_id` BIGINT NOT NULL COMMENT '个人报名ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `before_status` VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
  `after_status` VARCHAR(32) NOT NULL COMMENT '变更后状态',
  `audit_action` VARCHAR(32) NOT NULL COMMENT '动作：SUBMIT/APPROVE/REJECT/WITHDRAW/ASSIGN/CONVERT',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_personal_reg_audit_reg` (`personal_registration_id`),
  KEY `idx_personal_reg_audit_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4个人报名审核流水';

-- ---------------------------------------------------------------------------
-- 6. 个人线下抽签批次
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_personal_draw_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `batch_code` VARCHAR(64) NOT NULL COMMENT '批次编码',
  `batch_name` VARCHAR(128) NOT NULL COMMENT '批次名称',
  `batch_version` INT NOT NULL DEFAULT 1 COMMENT '批次版本',
  `batch_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/CONFIRMED/CANCELLED/CONVERTED',
  `active_unique_key` VARCHAR(128) DEFAULT NULL COMMENT '当前有效唯一键：DIVISION:{divisionId}:BATCH:{batchVersion}',
  `result_source` VARCHAR(32) NOT NULL DEFAULT 'OFFLINE_RANDOM' COMMENT '结果来源：OFFLINE_RANDOM/MANUAL_ADJUSTMENT/IMPORT',
  `result_remark` VARCHAR(500) DEFAULT NULL COMMENT '结果备注',
  `unassigned_policy` VARCHAR(32) DEFAULT NULL COMMENT '未分配处理：KEEP_FOR_NEXT/DISQUALIFY/REMARK',
  `unassigned_remark` VARCHAR(500) DEFAULT NULL COMMENT '未分配说明',
  `confirmed_by` BIGINT DEFAULT NULL COMMENT '确认人',
  `confirmed_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_personal_draw_batch_active` (`active_unique_key`),
  UNIQUE KEY `uk_personal_draw_batch_code` (`division_id`, `batch_code`, `is_delete`),
  KEY `idx_personal_draw_batch_division` (`division_id`, `batch_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4个人线下抽签批次';

-- ---------------------------------------------------------------------------
-- 7. 个人抽签临时队伍
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_personal_draw_team` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `draw_batch_id` BIGINT NOT NULL COMMENT '抽签批次ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `temporary_team_code` VARCHAR(64) NOT NULL COMMENT '临时队伍编码',
  `temporary_team_name` VARCHAR(128) NOT NULL COMMENT '临时队伍名称',
  `captain_personal_registration_id` BIGINT DEFAULT NULL COMMENT '队长个人报名ID',
  `captain_player_id` BIGINT DEFAULT NULL COMMENT '队长球员ID',
  `captain_user_id` BIGINT DEFAULT NULL COMMENT '队长用户ID',
  `expected_member_count` INT DEFAULT NULL COMMENT '期望人数',
  `actual_member_count` INT NOT NULL DEFAULT 0 COMMENT '实际分配人数',
  `long_term_team_id` BIGINT DEFAULT NULL COMMENT '绑定的长期球队ID',
  `converted_registration_id` BIGINT DEFAULT NULL COMMENT '转换后的正式报名ID',
  `formation_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '成队状态',
  `confirmed_by` BIGINT DEFAULT NULL COMMENT '确认人',
  `confirmed_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_personal_draw_team_code` (`draw_batch_id`, `temporary_team_code`, `is_delete`),
  KEY `idx_personal_draw_team_batch` (`draw_batch_id`),
  KEY `idx_personal_draw_team_captain` (`captain_user_id`),
  KEY `idx_personal_draw_team_long_term` (`long_term_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4个人抽签临时队伍';

-- ---------------------------------------------------------------------------
-- 8. 个人抽签分配
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_personal_draw_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `draw_batch_id` BIGINT NOT NULL COMMENT '抽签批次ID',
  `draw_team_id` BIGINT NOT NULL COMMENT '临时队伍ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `personal_registration_id` BIGINT NOT NULL COMMENT '个人报名ID',
  `player_id` BIGINT NOT NULL COMMENT '球员ID',
  `assignment_role` VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：CAPTAIN/MEMBER',
  `join_status` VARCHAR(32) NOT NULL DEFAULT 'WAITING_TEAM_CREATION' COMMENT '加入状态',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_personal_draw_assign_reg` (`draw_batch_id`, `personal_registration_id`, `is_delete`),
  UNIQUE KEY `uk_personal_draw_assign_player` (`draw_batch_id`, `player_id`, `is_delete`),
  KEY `idx_personal_draw_assign_team` (`draw_team_id`),
  KEY `idx_personal_draw_assign_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4个人抽签分配';

-- ---------------------------------------------------------------------------
-- 9. 赛事球队分组批次（对象是 EventTeam，与个人抽签隔离）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_grouping_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `stage_id` BIGINT NOT NULL COMMENT '阶段快照ID',
  `batch_version` INT NOT NULL DEFAULT 1 COMMENT '批次版本',
  `grouping_mode` VARCHAR(32) NOT NULL COMMENT '分组方式快照',
  `grouping_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/LOCKED/CANCELLED',
  `active_unique_key` VARCHAR(128) DEFAULT NULL COMMENT '当前有效唯一键：DIVISION:{divisionId}:STAGE:{stageId}',
  `group_count` INT NOT NULL COMMENT '小组数量',
  `result_source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '结果来源：OFFLINE_RANDOM/MANUAL/IMPORT',
  `confirmed_by` BIGINT DEFAULT NULL COMMENT '锁定人',
  `confirmed_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_division_grouping_active` (`active_unique_key`),
  KEY `idx_division_grouping_stage` (`division_id`, `stage_id`, `grouping_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事球队分组批次';

-- ---------------------------------------------------------------------------
-- 10. 赛事小组
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `grouping_batch_id` BIGINT NOT NULL COMMENT '分组批次ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `stage_id` BIGINT NOT NULL COMMENT '阶段快照ID',
  `group_code` VARCHAR(32) NOT NULL COMMENT '小组编码：A/B/C...',
  `group_name` VARCHAR(64) NOT NULL COMMENT '小组名称',
  `group_order` INT NOT NULL COMMENT '小组顺序',
  `qualify_count` INT DEFAULT NULL COMMENT '小组晋级名额',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_division_group_code` (`grouping_batch_id`, `group_code`, `is_delete`),
  UNIQUE KEY `uk_division_group_order` (`grouping_batch_id`, `group_order`, `is_delete`),
  KEY `idx_division_group_batch` (`grouping_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事小组';

-- ---------------------------------------------------------------------------
-- 11. 赛事小组球队（仅 EventTeam）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_group_team` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `grouping_batch_id` BIGINT NOT NULL COMMENT '分组批次ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `stage_id` BIGINT NOT NULL COMMENT '阶段快照ID',
  `group_id` BIGINT NOT NULL COMMENT '小组ID',
  `event_team_id` BIGINT NOT NULL COMMENT '参赛球队快照ID（禁止用长期team_id替代）',
  `seed_no` INT DEFAULT NULL COMMENT '种子序号',
  `draw_order` INT DEFAULT NULL COMMENT '抽签顺序',
  `assignment_source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '分配来源',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_division_group_event_team` (`grouping_batch_id`, `event_team_id`, `is_delete`),
  KEY `idx_division_group_team_group` (`group_id`),
  KEY `idx_division_group_team_event` (`event_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛事小组球队分配';
