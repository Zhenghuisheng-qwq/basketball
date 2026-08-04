-- =============================================================================
-- CNPA 动态赛制重构 第 2 轮：回滚参考
--
-- 警告：
--   - 仅允许在尚未产生 R2 业务数据时回滚
--   - 不得在生产环境无条件 DROP
--   - 有业务数据后必须人工评估后再处理
-- =============================================================================

SET NAMES utf8mb4;

-- ========== 回滚前检查（有数据则停止） ==========
-- SELECT COUNT(*) AS personal_reg_cnt FROM basket_personal_registration WHERE is_delete = 0;
-- SELECT COUNT(*) AS draw_batch_cnt FROM basket_personal_draw_batch WHERE is_delete = 0;
-- SELECT COUNT(*) AS grouping_batch_cnt FROM basket_division_grouping_batch WHERE is_delete = 0;
-- SELECT COUNT(*) AS personal_formation_reg_cnt
-- FROM basket_registration WHERE registration_source = 'PERSONAL_FORMATION' AND is_delete = 0;

-- ========== 无业务数据时的人工回滚步骤（默认注释） ==========

-- 1. 删除 R2 新表（仅空库/无数据环境）
-- DROP TABLE IF EXISTS basket_division_group_team;
-- DROP TABLE IF EXISTS basket_division_group;
-- DROP TABLE IF EXISTS basket_division_grouping_batch;
-- DROP TABLE IF EXISTS basket_personal_draw_assignment;
-- DROP TABLE IF EXISTS basket_personal_draw_team;
-- DROP TABLE IF EXISTS basket_personal_draw_batch;
-- DROP TABLE IF EXISTS basket_personal_registration_audit;
-- DROP TABLE IF EXISTS basket_personal_registration;

-- 2. 回滚 basket_registration 扩展字段
-- ALTER TABLE basket_registration
--   DROP INDEX idx_registration_source,
--   DROP INDEX idx_registration_draw_team,
--   DROP COLUMN source_personal_draw_batch_id,
--   DROP COLUMN source_personal_draw_team_id,
--   DROP COLUMN registration_source;

-- 3. 回滚模板唯一索引到 R1 形态（含 is_delete）——仅在确认无冲突时执行
-- ALTER TABLE basket_format_template
--   DROP INDEX uk_template_code_version,
--   ADD UNIQUE KEY uk_template_code_version (template_code, version_no, is_delete);

-- 4. 回滚快照版本字段与唯一索引——仅在确认可接受历史冲突风险时执行
-- UPDATE basket_division_format_snapshot SET is_delete = 1 WHERE is_current = 0;
-- ALTER TABLE basket_division_format_snapshot
--   DROP INDEX uk_division_current_snapshot,
--   DROP INDEX idx_division_snapshot_version,
--   DROP COLUMN superseded_time,
--   DROP COLUMN superseded_snapshot_id,
--   DROP COLUMN active_unique_key,
--   DROP COLUMN is_current,
--   DROP COLUMN snapshot_version,
--   ADD UNIQUE KEY uk_division_snapshot_active (division_id, is_delete);

-- ========== 已有 R2 业务数据时的处理 ==========
-- 1. 禁止 DROP 新表
-- 2. 禁止物理删除个人报名/抽签/分组历史
-- 3. 如需停用能力：关闭前端入口 + 后端权限，保留数据
-- 4. 联系研发评估快照版本字段回退影响
