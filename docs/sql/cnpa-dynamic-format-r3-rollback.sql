-- CNPA 动态赛制 R3 回滚
-- 警告：仅在尚未产生 R3 正式业务数据时执行
-- 不回滚 R1/R2 表；不删除现有比赛和 Submission 数据

-- 检查是否有 R3 业务数据
SELECT 'stage_team' AS tbl, COUNT(*) AS cnt FROM basket_division_stage_team WHERE is_delete = 0
UNION ALL
SELECT 'qualification', COUNT(*) FROM basket_division_stage_qualification WHERE is_delete = 0
UNION ALL
SELECT 'r3_schedule_batch', COUNT(*) FROM basket_schedule_batch WHERE generation_strategy IS NOT NULL AND is_delete = 0;

-- 以下 DDL 需确认 cnt=0 后手工执行

-- DROP TABLE IF EXISTS basket_division_stage_qualification;
-- DROP TABLE IF EXISTS basket_division_stage_team;

-- ALTER TABLE basket_division_stage DROP COLUMN execution_status, ...;
-- ALTER TABLE basket_match DROP COLUMN stage_id, ...;
-- ALTER TABLE basket_schedule_batch DROP COLUMN stage_id, ...;
-- ALTER TABLE basket_standing DROP COLUMN stage_id, ...;
-- ALTER TABLE basket_captain_grant DROP COLUMN grant_source, DROP COLUMN source_draw_team_id;
