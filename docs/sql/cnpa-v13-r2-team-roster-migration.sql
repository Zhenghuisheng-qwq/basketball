-- =============================================================================
-- CNPA 赛事发布与赛程管理 V1.3 R2：TEAM 最终参赛名单 + 候补 + Roster Entry
-- 文件：cnpa-v13-r2-team-roster-migration.sql
--
-- 前置：cnpa-v13-r1-version-skeleton-migration.sql
-- 范围：
--   1. basket_registration 工作态 allocation 字段
--   2. basket_division_roster_entry 名单版本条目（保证历史可还原）
--   3. basket_division_roster_version.allocation_fingerprint（Lock 幂等）
-- 不回填历史 EventTeam；Legacy 无 RosterVersion 仍按旧语义读取
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. Registration 工作态：审核状态与参赛角色分离
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_registration`
  ADD COLUMN `participation_role` VARCHAR(32) NOT NULL DEFAULT 'UNALLOCATED'
    COMMENT '参赛分配：UNALLOCATED/FORMAL/CANDIDATE/EXCLUDED；与 registration_status 分离' AFTER `registration_status`,
  ADD COLUMN `candidate_order` INT DEFAULT NULL
    COMMENT '候补顺序，仅 CANDIDATE 有值，从1连续' AFTER `participation_role`;

ALTER TABLE `basket_registration`
  ADD KEY `idx_reg_division_role` (`division_id`, `participation_role`, `is_delete`),
  ADD KEY `idx_reg_division_candidate` (`division_id`, `candidate_order`, `is_delete`);

-- ---------------------------------------------------------------------------
-- 2. Roster Version 幂等指纹
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_division_roster_version`
  ADD COLUMN `allocation_fingerprint` VARCHAR(128) DEFAULT NULL
    COMMENT '当前分配指纹，用于 Lock 幂等' AFTER `excluded_count`;

-- ---------------------------------------------------------------------------
-- 3. 名单版本条目：锁定瞬间的 FORMAL/CANDIDATE/EXCLUDED 快照
--    不复制 EventTeam/EventPlayer。历史还原只读本表。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `basket_division_roster_entry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `roster_version_id` BIGINT NOT NULL COMMENT '名单版本ID',
  `division_id` BIGINT NOT NULL COMMENT 'Division ID',
  `registration_id` BIGINT NOT NULL COMMENT '来源报名ID',
  `team_id` BIGINT NOT NULL COMMENT '长期球队ID',
  `participation_role` VARCHAR(32) NOT NULL COMMENT 'FORMAL/CANDIDATE/EXCLUDED',
  `candidate_order` INT DEFAULT NULL COMMENT '候补顺序，仅 CANDIDATE',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roster_entry_version_reg` (`roster_version_id`, `registration_id`),
  KEY `idx_roster_entry_division_role` (`division_id`, `participation_role`, `is_delete`),
  KEY `idx_roster_entry_version_role` (`roster_version_id`, `participation_role`, `candidate_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4 Division名单版本条目';
