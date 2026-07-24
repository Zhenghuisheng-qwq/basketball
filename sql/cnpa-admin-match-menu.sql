-- =============================================================================
-- CNPA Admin 第五轮菜单 / 动态路由 / 按钮权限（赛事执行）
-- 文件：sql/cnpa-admin-match-menu.sql
-- =============================================================================
-- 执行前提：
--   1. 目标库已初始化若依 sys_menu 等表（见 ry_20260321.sql）
--   2. 已执行第四轮菜单 SQL：sql/cnpa-admin-menu.sql
--      必须存在一级目录：path=basket / menu_name=CNPA赛事管理
--   3. 本脚本不新建一级目录；若父菜单不存在则后续变量为空，插入不会生效
--
-- 执行顺序：
--   1) 定位一级目录 @cnpa_root
--   2) 新增 6 个页面菜单（C）
--   3) 新增按钮权限（F）
--   4) 不默认给普通角色授权
--
-- 可重复执行：按 parent_id + path / perms / component 判重
--
-- 组件路径（与 Vue 文件一致）：
--   basket/stage/index
--   basket/group/index
--   basket/match/index
--   basket/matchLineup/index
--   basket/matchScore/index
--   basket/matchStat/index
-- =============================================================================

SET NAMES utf8mb4;
SET @cnpa_create_by := 'admin';
SET @cnpa_now := NOW();

-- -----------------------------------------------------------------------------
-- 1. 定位一级目录（复用第四轮，不重复创建）
-- -----------------------------------------------------------------------------
SELECT menu_id INTO @cnpa_root
FROM sys_menu
WHERE parent_id = 0
  AND menu_type = 'M'
  AND (
      path = 'basket'
      OR menu_name IN ('CNPA赛事管理', '篮球赛事', '赛事管理')
  )
ORDER BY CASE WHEN path = 'basket' THEN 0 WHEN menu_name = 'CNPA赛事管理' THEN 1 ELSE 2 END, menu_id
LIMIT 1;

-- 若父菜单不存在，请先执行 sql/cnpa-admin-menu.sql
-- SELECT @cnpa_root AS cnpa_root_menu_id;

-- -----------------------------------------------------------------------------
-- 2. 页面菜单（C）order_num 接续第四轮 1~7 → 本轮从 8 起
-- -----------------------------------------------------------------------------

-- 2.1 赛事阶段
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '赛事阶段', @cnpa_root, 8, 'stage', 'basket/stage/index', '', 'BasketStage',
    1, 0, 'C', '0', '0', 'basket:stage:list', 'tree-table',
    @cnpa_create_by, @cnpa_now, '赛事阶段与轮次管理'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'stage' OR perms = 'basket:stage:list' OR component = 'basket/stage/index')
    LIMIT 1
);

SELECT menu_id INTO @m_stage
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'stage' OR perms = 'basket:stage:list' OR component = 'basket/stage/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'stage', component = 'basket/stage/index', route_name = 'BasketStage',
    perms = 'basket:stage:list', menu_type = 'C', visible = '0', status = '0',
    update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_stage;

-- 2.2 分组管理
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '分组管理', @cnpa_root, 9, 'group', 'basket/group/index', '', 'BasketGroup',
    1, 0, 'C', '0', '0', 'basket:group:list', 'cascader',
    @cnpa_create_by, @cnpa_now, '赛事分组与球队分配'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'group' OR perms = 'basket:group:list' OR component = 'basket/group/index')
    LIMIT 1
);

SELECT menu_id INTO @m_group
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'group' OR perms = 'basket:group:list' OR component = 'basket/group/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'group', component = 'basket/group/index', route_name = 'BasketGroup',
    perms = 'basket:group:list', menu_type = 'C', visible = '0', status = '0',
    update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_group;

-- 2.3 赛程管理
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '赛程管理', @cnpa_root, 10, 'match', 'basket/match/index', '', 'BasketMatch',
    1, 0, 'C', '0', '0', 'basket:match:list', 'date',
    @cnpa_create_by, @cnpa_now, '比赛对阵与赛程'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'match' OR perms = 'basket:match:list' OR component = 'basket/match/index')
    LIMIT 1
);

SELECT menu_id INTO @m_match
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'match' OR perms = 'basket:match:list' OR component = 'basket/match/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'match', component = 'basket/match/index', route_name = 'BasketMatch',
    perms = 'basket:match:list', menu_type = 'C', visible = '0', status = '0',
    update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_match;

-- 2.4 比赛名单
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '比赛名单', @cnpa_root, 11, 'match-lineup', 'basket/matchLineup/index', '', 'BasketMatchLineup',
    1, 0, 'C', '0', '0', 'basket:lineup:list', 'peoples',
    @cnpa_create_by, @cnpa_now, '单场参赛名单'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'match-lineup' OR perms = 'basket:lineup:list' OR component = 'basket/matchLineup/index')
    LIMIT 1
);

SELECT menu_id INTO @m_lineup
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'match-lineup' OR perms = 'basket:lineup:list' OR component = 'basket/matchLineup/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'match-lineup', component = 'basket/matchLineup/index', route_name = 'BasketMatchLineup',
    perms = 'basket:lineup:list', menu_type = 'C', visible = '0', status = '0',
    update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_lineup;

-- 2.5 比分录入（列表权限使用 query，与后端 score 模块一致）
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '比分录入', @cnpa_root, 12, 'match-score', 'basket/matchScore/index', '', 'BasketMatchScore',
    1, 0, 'C', '0', '0', 'basket:score:query', 'chart',
    @cnpa_create_by, @cnpa_now, '单节比分与结果'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'match-score' OR perms = 'basket:score:query' OR component = 'basket/matchScore/index')
    LIMIT 1
);

SELECT menu_id INTO @m_score
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'match-score' OR perms = 'basket:score:query' OR component = 'basket/matchScore/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'match-score', component = 'basket/matchScore/index', route_name = 'BasketMatchScore',
    perms = 'basket:score:query', menu_type = 'C', visible = '0', status = '0',
    update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_score;

-- 2.6 比赛数据
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '比赛数据', @cnpa_root, 13, 'match-stat', 'basket/matchStat/index', '', 'BasketMatchStat',
    1, 0, 'C', '0', '0', 'basket:matchStat:query', 'documentation',
    @cnpa_create_by, @cnpa_now, '球队/球员单场数据'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'match-stat' OR perms = 'basket:matchStat:query' OR component = 'basket/matchStat/index')
    LIMIT 1
);

SELECT menu_id INTO @m_stat
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'match-stat' OR perms = 'basket:matchStat:query' OR component = 'basket/matchStat/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'match-stat', component = 'basket/matchStat/index', route_name = 'BasketMatchStat',
    perms = 'basket:matchStat:query', menu_type = 'C', visible = '0', status = '0',
    update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_stat;

-- -----------------------------------------------------------------------------
-- 3. 按钮权限（F）
-- -----------------------------------------------------------------------------

-- 3.1 赛事阶段按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '阶段查询', @m_stage, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:stage:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:stage:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '阶段新增', @m_stage, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:stage:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:stage:add' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '阶段修改', @m_stage, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:stage:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:stage:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '阶段删除', @m_stage, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:stage:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:stage:remove' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '阶段状态', @m_stage, 5, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:stage:status', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:stage:status' LIMIT 1);

-- 轮次按钮挂在阶段菜单下（轮次在阶段页内管理）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮次查询', @m_stage, 11, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:round:list', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:round:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮次详情', @m_stage, 12, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:round:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:round:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮次新增', @m_stage, 13, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:round:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:round:add' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮次修改', @m_stage, 14, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:round:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:round:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮次删除', @m_stage, 15, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:round:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:round:remove' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '轮次状态', @m_stage, 16, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:round:status', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stage IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stage AND perms = 'basket:round:status' LIMIT 1);

-- 3.2 分组按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分组查询', @m_group, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:group:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_group AND perms = 'basket:group:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分组新增', @m_group, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:group:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_group AND perms = 'basket:group:add' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分组修改', @m_group, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:group:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_group AND perms = 'basket:group:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分组删除', @m_group, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:group:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_group AND perms = 'basket:group:remove' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分配球队', @m_group, 5, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:group:assignTeam', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_group AND perms = 'basket:group:assignTeam' LIMIT 1);

-- 3.3 赛程按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比赛查询', @m_match, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比赛新增', @m_match, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:add' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比赛修改', @m_match, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '安排比赛', @m_match, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:schedule', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:schedule' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比赛延期', @m_match, 5, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:postpone', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:postpone' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比赛取消', @m_match, 6, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:cancel', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:cancel' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '开始比赛', @m_match, 7, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:start', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:start' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比赛删除', @m_match, 8, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:match:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_match IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_match AND perms = 'basket:match:remove' LIMIT 1);

-- 3.4 名单按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '名单查询', @m_lineup, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:lineup:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_lineup IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_lineup AND perms = 'basket:lineup:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '名单编辑', @m_lineup, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:lineup:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_lineup IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_lineup AND perms = 'basket:lineup:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '名单确认', @m_lineup, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:lineup:confirm', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_lineup IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_lineup AND perms = 'basket:lineup:confirm' LIMIT 1);

-- 3.5 比分按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比分编辑', @m_score, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:score:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_score IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_score AND perms = 'basket:score:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比分提交', @m_score, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:score:submit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_score IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_score AND perms = 'basket:score:submit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比分确认', @m_score, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:score:confirm', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_score IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_score AND perms = 'basket:score:confirm' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '比分重开', @m_score, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:score:reopen', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_score IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_score AND perms = 'basket:score:reopen' LIMIT 1);

-- 3.6 比赛数据按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据编辑', @m_stat, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:matchStat:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stat AND perms = 'basket:matchStat:edit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据提交', @m_stat, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:matchStat:submit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stat AND perms = 'basket:matchStat:submit' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '结果确认', @m_stat, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:matchStat:confirm', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stat AND perms = 'basket:matchStat:confirm' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '结果重开', @m_stat, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:matchStat:reopen', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stat AND perms = 'basket:matchStat:reopen' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据审计', @m_stat, 5, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:matchStat:audit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_stat AND perms = 'basket:matchStat:audit' LIMIT 1);

-- =============================================================================
-- 角色授权说明（本脚本不写入 sys_role_menu）
--   - 超级管理员按若依规则可见全部菜单
--   - 运营角色请在「系统管理 → 角色管理」勾选：
--       CNPA赛事管理 → 赛事阶段 / 分组管理 / 赛程管理 / 比赛名单 / 比分录入 / 比赛数据
--       及对应按钮权限
-- =============================================================================
