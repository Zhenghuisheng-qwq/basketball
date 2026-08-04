-- =============================================================================
-- CNPA 动态赛制重构 第 1 轮：安全回滚说明
-- 文件：cnpa-dynamic-format-r1-rollback.sql
--
-- 重要：
--   - 若已有 Division 规则快照或已锁定快照产生业务数据，禁止无条件 DROP
--   - 生产环境必须人工评估后执行
--   - 本脚本仅提供结构回滚参考，默认全部注释
-- =============================================================================

-- 回滚前检查：
-- SELECT COUNT(1) FROM basket_division_format_snapshot WHERE is_delete = 0;
-- SELECT COUNT(1) FROM basket_division WHERE format_snapshot_id IS NOT NULL;

-- 步骤 1：若确认无任何快照业务数据，可逻辑删除快照数据
-- UPDATE basket_division_tie_break_rule SET is_delete = 1 WHERE is_delete = 0;
-- UPDATE basket_division_stage SET is_delete = 1 WHERE is_delete = 0;
-- UPDATE basket_division_format_snapshot SET is_delete = 1 WHERE is_delete = 0;
-- UPDATE basket_format_template_tie_break_rule SET is_delete = 1 WHERE is_delete = 0;
-- UPDATE basket_format_template_stage SET is_delete = 1 WHERE is_delete = 0;
-- UPDATE basket_format_template SET is_delete = 1 WHERE is_delete = 0;

-- 步骤 2：清除 Division 引用
-- UPDATE basket_division SET format_template_id = NULL, format_snapshot_id = NULL;

-- 步骤 3：删除新增表（仅开发/空库环境）
-- DROP TABLE IF EXISTS basket_division_tie_break_rule;
-- DROP TABLE IF EXISTS basket_division_stage;
-- DROP TABLE IF EXISTS basket_division_format_snapshot;
-- DROP TABLE IF EXISTS basket_format_template_tie_break_rule;
-- DROP TABLE IF EXISTS basket_format_template_stage;
-- DROP TABLE IF EXISTS basket_format_template;

-- 步骤 4：删除 Division 扩展字段
-- ALTER TABLE basket_division
--   DROP COLUMN format_snapshot_id,
--   DROP COLUMN format_template_id,
--   DROP COLUMN game_mode;

-- 注意：game_mode 回填的 FIVE_V_FIVE 默认值回滚后将丢失；
-- PS/XBOX 历史 platform_code 不会被本回滚脚本修改。
