-- =============================================================================
-- CNPA 动态赛制重构 第 1 轮：DDL 迁移
-- 文件：cnpa-dynamic-format-r1-migration.sql
--
-- 执行顺序：
--   1. 先执行 cnpa-dynamic-format-r1-data-check.sql（迁移前检查）
--   2. 再执行本脚本
--   3. 再执行 cnpa-dynamic-format-r1-seed.sql（模板种子）
--   4. 最后执行数据检查脚本中的快照缺失检查
--
-- 警告：
--   - 不在生产环境自动执行
--   - 不自动合并 PS / XBOX Division
--   - 不修改 V2 旧表
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. basket_division 扩展字段
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_division`
  ADD COLUMN `game_mode` VARCHAR(32) NOT NULL DEFAULT 'FIVE_V_FIVE'
    COMMENT '比赛模式：FIVE_V_FIVE/THREE_V_THREE；历史回填默认值仅兼容，不代表业务确认' AFTER `platform_code`,
  ADD COLUMN `format_template_id` BIGINT DEFAULT NULL
    COMMENT '创建时选择的赛制模板ID' AFTER `format_type`,
  ADD COLUMN `format_snapshot_id` BIGINT DEFAULT NULL
    COMMENT '本届实际规则快照ID' AFTER `format_template_id`;

ALTER TABLE `basket_division`
  ADD KEY `idx_division_game_mode` (`game_mode`),
  ADD KEY `idx_division_format_snapshot` (`format_snapshot_id`);

-- ---------------------------------------------------------------------------
-- 2. basket_format_template 赛制模板头
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_format_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `series_id` BIGINT DEFAULT NULL COMMENT '关联赛事系列ID，NULL表示通用模板',
  `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码，全局唯一',
  `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `template_description` VARCHAR(500) DEFAULT NULL COMMENT '模板说明',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系列默认模板：1是 0否',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code_version` (`template_code`, `version_no`, `is_delete`),
  KEY `idx_template_series` (`series_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4动态赛制模板';

-- ---------------------------------------------------------------------------
-- 3. basket_format_template_stage 模板阶段
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_format_template_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '阶段编码',
  `stage_name` VARCHAR(128) NOT NULL COMMENT '阶段名称',
  `stage_order` INT NOT NULL COMMENT '阶段顺序，从1开始',
  `stage_type` VARCHAR(32) NOT NULL COMMENT '阶段类型：REGULAR/GROUP/KNOCKOUT/SEMI_FINAL/FINAL/CUSTOM',
  `competition_format` VARCHAR(32) NOT NULL COMMENT '竞赛形式：ROUND_ROBIN/SINGLE_ELIMINATION/MANUAL',
  `grouping_mode` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '分组方式',
  `group_count` INT DEFAULT NULL COMMENT '分组数量',
  `round_robin_cycles` INT DEFAULT NULL COMMENT '循环次数，单循环=1',
  `best_of` INT NOT NULL DEFAULT 1 COMMENT 'BO数，必须为正奇数',
  `win_points` INT DEFAULT NULL COMMENT '胜场积分',
  `loss_points` INT DEFAULT NULL COMMENT '负场积分',
  `forfeit_win_points` INT DEFAULT NULL COMMENT '弃权胜积分',
  `forfeit_loss_points` INT DEFAULT NULL COMMENT '弃权负积分',
  `advancement_type` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '晋级类型',
  `advancement_count` INT DEFAULT NULL COMMENT '晋级数量',
  `pairing_rule` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '对阵规则',
  `next_stage_code` VARCHAR(64) DEFAULT NULL COMMENT '下一阶段编码',
  `requires_manual_confirmation` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要人工确认',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_stage_code` (`template_id`, `stage_code`, `is_delete`),
  UNIQUE KEY `uk_template_stage_order` (`template_id`, `stage_order`, `is_delete`),
  KEY `idx_template_stage_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛制模板阶段';

-- ---------------------------------------------------------------------------
-- 4. basket_format_template_tie_break_rule 模板同分规则
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_format_template_tie_break_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '阶段编码',
  `rule_code` VARCHAR(32) NOT NULL COMMENT '同分规则编码',
  `priority_no` INT NOT NULL COMMENT '优先级序号，从1开始',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_tie_priority` (`template_id`, `stage_code`, `priority_no`, `is_delete`),
  UNIQUE KEY `uk_template_tie_rule` (`template_id`, `stage_code`, `rule_code`, `is_delete`),
  KEY `idx_template_tie_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4赛制模板同分规则';

-- ---------------------------------------------------------------------------
-- 5. basket_division_format_snapshot Division 规则快照头
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_format_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `source_template_id` BIGINT DEFAULT NULL COMMENT '来源模板ID',
  `source_template_code` VARCHAR(64) NOT NULL COMMENT '来源模板编码',
  `source_template_name` VARCHAR(128) NOT NULL COMMENT '来源模板名称',
  `source_template_version` INT NOT NULL DEFAULT 1 COMMENT '来源模板版本',
  `snapshot_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '快照状态：DRAFT/LOCKED/IN_USE/ARCHIVED',
  `locked_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
  `locked_by` BIGINT DEFAULT NULL COMMENT '锁定操作人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_division_snapshot_active` (`division_id`, `is_delete`),
  KEY `idx_snapshot_status` (`snapshot_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 Division规则快照';

-- ---------------------------------------------------------------------------
-- 6. basket_division_stage Division 阶段规则快照
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID冗余',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '阶段编码',
  `stage_name` VARCHAR(128) NOT NULL COMMENT '阶段名称',
  `stage_order` INT NOT NULL COMMENT '阶段顺序',
  `stage_type` VARCHAR(32) NOT NULL COMMENT '阶段类型',
  `competition_format` VARCHAR(32) NOT NULL COMMENT '竞赛形式',
  `grouping_mode` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '分组方式',
  `group_count` INT DEFAULT NULL COMMENT '分组数量',
  `round_robin_cycles` INT DEFAULT NULL COMMENT '循环次数',
  `best_of` INT NOT NULL DEFAULT 1 COMMENT 'BO数',
  `win_points` INT DEFAULT NULL COMMENT '胜场积分',
  `loss_points` INT DEFAULT NULL COMMENT '负场积分',
  `forfeit_win_points` INT DEFAULT NULL COMMENT '弃权胜积分',
  `forfeit_loss_points` INT DEFAULT NULL COMMENT '弃权负积分',
  `advancement_type` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '晋级类型',
  `advancement_count` INT DEFAULT NULL COMMENT '晋级数量',
  `pairing_rule` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '对阵规则',
  `next_stage_code` VARCHAR(64) DEFAULT NULL COMMENT '下一阶段编码',
  `requires_manual_confirmation` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要人工确认',
  `stage_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '阶段状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_division_stage_code` (`snapshot_id`, `stage_code`, `is_delete`),
  UNIQUE KEY `uk_division_stage_order` (`snapshot_id`, `stage_order`, `is_delete`),
  KEY `idx_division_stage_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 Division阶段规则快照';

-- ---------------------------------------------------------------------------
-- 7. basket_division_tie_break_rule Division 同分规则快照
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_tie_break_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID冗余',
  `stage_id` BIGINT NOT NULL COMMENT '阶段快照ID',
  `rule_code` VARCHAR(32) NOT NULL COMMENT '同分规则编码',
  `priority_no` INT NOT NULL COMMENT '优先级序号',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_division_tie_priority` (`snapshot_id`, `stage_id`, `priority_no`, `is_delete`),
  UNIQUE KEY `uk_division_tie_rule` (`snapshot_id`, `stage_id`, `rule_code`, `is_delete`),
  KEY `idx_division_tie_division` (`division_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 Division同分规则快照';

-- ---------------------------------------------------------------------------
-- 8. 历史 Division game_mode 回填说明
-- ---------------------------------------------------------------------------
-- 默认值 FIVE_V_FIVE 已在 ADD COLUMN 时写入。
-- 该默认值仅用于兼容现有数据，不代表所有历史赛事都经过业务确认是 5V5。
-- ---------------------------------------------------------------------------
-- 9. 历史 Division 兼容：关联 V4_LEGACY 模板（幂等，需先执行 seed）
--    有报名/比赛数据的 Division 快照状态设为 LOCKED，否则 DRAFT
-- ---------------------------------------------------------------------------
-- 说明：完整快照阶段复制建议通过 Java Migration Runner 或运维脚本执行；
-- 以下 SQL 仅更新 Division 模板引用，不自动复制阶段明细。

UPDATE basket_division d
JOIN basket_format_template t ON t.template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND t.version_no = 1 AND t.is_delete = 0
SET d.format_template_id = t.id
WHERE d.is_delete = 0 AND d.format_template_id IS NULL;
