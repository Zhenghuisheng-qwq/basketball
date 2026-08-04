-- =============================================================================
-- CNPA 动态赛制重构 第 2 轮：历史数据补齐（默认关闭自动执行）
--
-- 使用说明：
--   1. 本脚本默认全部注释，需运维按需手工打开对应段落
--   2. 幂等：可重复检查，不重复生成已有 current 快照
--   3. 不重新生成赛程、积分、比赛、提交和统计
--   4. 不自动合并 PS/XBOX
--   5. Java 侧推荐使用：cnpa.v4.format-snapshot-migration.enabled=true
--      （BasketV4FormatSnapshotMigrationRunner，一次性）
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- A0. 查询无快照 Division（只读）
-- ---------------------------------------------------------------------------
-- SELECT d.id, d.division_name, d.division_status, d.format_template_id, d.format_snapshot_id
-- FROM basket_division d
-- WHERE d.is_delete = 0 AND (d.format_snapshot_id IS NULL OR d.format_snapshot_id = 0);

-- ---------------------------------------------------------------------------
-- A1. 查询每个 Division 业务数据概况（只读）
-- ---------------------------------------------------------------------------
-- SELECT d.id AS division_id, d.division_name,
--   (SELECT COUNT(*) FROM basket_registration r WHERE r.division_id = d.id AND r.is_delete = 0) AS registration_cnt,
--   (SELECT COUNT(*) FROM basket_event_team et WHERE et.division_id = d.id AND et.is_delete = 0) AS event_team_cnt,
--   (SELECT COUNT(*) FROM basket_schedule_batch sb WHERE sb.division_id = d.id AND sb.is_delete = 0) AS schedule_batch_cnt,
--   (SELECT COUNT(*) FROM basket_match m WHERE m.division_id = d.id AND m.is_delete = 0) AS match_cnt,
--   (SELECT COUNT(*) FROM basket_match_submission ms JOIN basket_match m ON m.id = ms.match_id
--      WHERE m.division_id = d.id AND ms.is_delete = 0) AS submission_cnt
-- FROM basket_division d
-- WHERE d.is_delete = 0 AND (d.format_snapshot_id IS NULL OR d.format_snapshot_id = 0);

-- ---------------------------------------------------------------------------
-- A. 快照版本与 current 标记初始化检查
-- ---------------------------------------------------------------------------
-- SELECT d.id, d.format_snapshot_id, s.snapshot_version, s.is_current, s.active_unique_key, s.snapshot_status
-- FROM basket_division d
-- LEFT JOIN basket_division_format_snapshot s ON s.id = d.format_snapshot_id
-- WHERE d.is_delete = 0;

-- ---------------------------------------------------------------------------
-- B. 缺失 format_template_id 的 Division 绑定 V4_LEGACY_ROUND_ROBIN_V1
-- ---------------------------------------------------------------------------
-- UPDATE basket_division d
-- JOIN basket_format_template t
--   ON t.template_code = 'V4_LEGACY_ROUND_ROBIN_V1' AND t.version_no = 1
-- SET d.format_template_id = t.id
-- WHERE d.is_delete = 0 AND d.format_template_id IS NULL;

-- ---------------------------------------------------------------------------
-- C. 已有快照补齐版本字段（若 migration 未覆盖到）
-- ---------------------------------------------------------------------------
-- UPDATE basket_division_format_snapshot s
-- SET s.snapshot_version = 1,
--     s.is_current = 1,
--     s.active_unique_key = CONCAT('DIVISION:', s.division_id)
-- WHERE s.is_delete = 0
--   AND (s.active_unique_key IS NULL OR s.is_current IS NULL OR s.snapshot_version IS NULL);

-- ---------------------------------------------------------------------------
-- D. 兼容快照生成方案（推荐走 Java Migration Runner，而非纯 SQL 复制阶段）
--
-- 状态规则：
--   1) 已有报名/EventTeam/赛程/比赛/提交 → snapshot_status=IN_USE, is_current=1, 禁止编辑
--   2) 已开放报名但无后续业务数据 → snapshot_status=LOCKED, 禁止编辑
--   3) Division=DRAFT 且无业务数据 → snapshot_status=DRAFT, 可编辑
--
-- 伪流程：
--   for each division where format_snapshot_id is null:
--     create draft snapshot from V4_LEGACY_ROUND_ROBIN_V1
--     copy stages + tie-break rules
--     set format_template_id / format_snapshot_id
--     apply status rule above
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- E. 正式报名来源回填
-- ---------------------------------------------------------------------------
-- UPDATE basket_registration
-- SET registration_source = 'TEAM_DIRECT'
-- WHERE registration_source IS NULL OR registration_source = '';
