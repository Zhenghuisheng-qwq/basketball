-- =============================================================================
-- CNPA 动态赛制 R6：organization_mode 字段
-- 文件：cnpa-dynamic-format-r6-organization-mode-migration.sql
--
-- 执行顺序：
--   1. 先执行 cnpa-dynamic-format-r6-data-check.sql（迁移前可选基线）
--   2. 再执行本脚本
--   3. 最后执行 cnpa-dynamic-format-r6-data-check.sql（迁移后验收）
--
-- 警告：不在生产环境自动执行
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. basket_division_stage.organization_mode
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_division_stage`
  ADD COLUMN `organization_mode` VARCHAR(32) DEFAULT NULL
    COMMENT '参赛组织方式：GROUPED/UNGROUPED' AFTER `stage_type`;

-- ---------------------------------------------------------------------------
-- 2. basket_format_template_stage.organization_mode
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_format_template_stage`
  ADD COLUMN `organization_mode` VARCHAR(32) DEFAULT NULL
    COMMENT '参赛组织方式：GROUPED/UNGROUPED' AFTER `stage_type`;

-- ---------------------------------------------------------------------------
-- 3. 历史回填：grouping_mode = NONE 或 NULL 或空串 → UNGROUPED，否则 → GROUPED
-- ---------------------------------------------------------------------------
UPDATE `basket_division_stage`
SET `organization_mode` = CASE
    WHEN `grouping_mode` IS NULL OR TRIM(`grouping_mode`) = '' OR UPPER(TRIM(`grouping_mode`)) = 'NONE'
      THEN 'UNGROUPED'
    ELSE 'GROUPED'
  END
WHERE `is_delete` = 0;

UPDATE `basket_format_template_stage`
SET `organization_mode` = CASE
    WHEN `grouping_mode` IS NULL OR TRIM(`grouping_mode`) = '' OR UPPER(TRIM(`grouping_mode`)) = 'NONE'
      THEN 'UNGROUPED'
    ELSE 'GROUPED'
  END
WHERE `is_delete` = 0;

-- ---------------------------------------------------------------------------
-- 4. 异常数据检查（仅输出，不自动修正非法 grouping_mode）
--    若以下 SELECT 有结果，需人工处理后再设 NOT NULL
-- ---------------------------------------------------------------------------
-- SELECT id, division_id, stage_code, grouping_mode, organization_mode
-- FROM basket_division_stage
-- WHERE is_delete = 0
--   AND organization_mode IS NULL;

-- ---------------------------------------------------------------------------
-- 5. 确认无 NULL 后收紧约束（若仍有 NULL 则本语句失败，需人工介入）
-- ---------------------------------------------------------------------------
ALTER TABLE `basket_division_stage`
  MODIFY COLUMN `organization_mode` VARCHAR(32) NOT NULL
    COMMENT '参赛组织方式：GROUPED/UNGROUPED';

ALTER TABLE `basket_format_template_stage`
  MODIFY COLUMN `organization_mode` VARCHAR(32) NOT NULL
    COMMENT '参赛组织方式：GROUPED/UNGROUPED';
