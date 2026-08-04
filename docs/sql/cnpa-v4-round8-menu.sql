-- CNPA V4 Round8: 补隐藏旧统计/结算/参与审核菜单（修正 R5 错误 component 路径）
SET @eventParentId = 2152;

UPDATE sys_menu
SET visible = '1',
    remark = CONCAT(IFNULL(remark, ''), ' [V4 R8 deprecated]')
WHERE component IN (
    'cnpa/statistics/settlement/index',
    'cnpa/statistics/standings/index',
    'cnpa/statistics/ranking/index',
    'cnpa/statistics/team-season/index',
    'cnpa/statistics/player-season/index',
    'cnpa/statistics/season-result/index',
    'cnpa/participation/open-registration/index',
    'cnpa/participation/transfer-audit/index',
    'cnpa/participation/free-agent-audit/index',
    'cnpa/participation/captain-transfer/index',
    'cnpa/participation/roster-operations/index'
)
   OR menu_id IN (2080, 2081, 2082, 2083, 2084, 2085, 2086, 2090, 2091, 2092, 2093, 2100, 2101, 2102);

-- 确保旧自动 Job 停用（若存在）
UPDATE sys_job
SET status = '1',
    remark = CONCAT(IFNULL(remark, ''), ' [V4 R8 disabled - use V4 manual flow]')
WHERE invoke_target LIKE '%ScheduleAutoGenerateJob%'
   OR invoke_target LIKE '%MatchStatusAutoFinishJob%';
