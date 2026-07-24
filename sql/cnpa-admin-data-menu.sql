-- =============================================================================
-- CNPA Admin 第六轮菜单 / 动态路由 / 按钮权限（数据结算）
-- 文件：sql/cnpa-admin-data-menu.sql
-- =============================================================================
-- 执行前提：
--   1. 目标库已初始化若依 sys_menu 等表（见 ry_20260321.sql）
--   2. 已执行第四轮菜单 SQL：sql/cnpa-admin-menu.sql
--      必须存在一级目录：path=basket / menu_name=CNPA赛事管理
--   3. 建议已执行第五轮菜单 SQL：sql/cnpa-admin-match-menu.sql（order_num 1~13）
--   4. 本脚本不新建一级目录；若父菜单不存在则后续变量为空，插入不会生效
--
-- 执行顺序：
--   1) 定位一级目录 @cnpa_root
--   2) 新增 5 个页面菜单（C）
--   3) 新增按钮权限（F）
--   4) 不默认给普通角色授权
--
-- 可重复执行：按 parent_id + path / perms / component 判重
--
-- 组件路径（与 Vue 文件一致）：
--   basket/dataSettlement/index
--   basket/standing/index
--   basket/teamSeasonStat/index
--   basket/playerSeasonStat/index
--   basket/ranking/index
--
-- 权限标识与 BasketSettlementPerms 一致
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
-- 2. 页面菜单（C）order_num 接续第五轮 8~13 → 本轮从 14 起
-- -----------------------------------------------------------------------------

-- 2.1 数据结算
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '数据结算', @cnpa_root, 14, 'data-settlement', 'basket/dataSettlement/index', '', 'BasketDataSettlement',
    1, 0, 'C', '0', '0', 'basket:dataSettlement:list', 'money',
    @cnpa_create_by, @cnpa_now, '赛季结算校验与重算'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'data-settlement' OR perms = 'basket:dataSettlement:list' OR component = 'basket/dataSettlement/index')
    LIMIT 1
);

SELECT menu_id INTO @m_data_settlement
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'data-settlement' OR perms = 'basket:dataSettlement:list' OR component = 'basket/dataSettlement/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'data-settlement', component = 'basket/dataSettlement/index', route_name = 'BasketDataSettlement',
    perms = 'basket:dataSettlement:list', menu_type = 'C', visible = '0', status = '0',
    order_num = 14, update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_data_settlement;

-- 2.2 积分榜
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '积分榜', @cnpa_root, 15, 'standing', 'basket/standing/index', '', 'BasketStanding',
    1, 0, 'C', '0', '0', 'basket:standing:list', 'chart',
    @cnpa_create_by, @cnpa_now, '阶段/小组积分榜'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'standing' OR perms = 'basket:standing:list' OR component = 'basket/standing/index')
    LIMIT 1
);

SELECT menu_id INTO @m_standing
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'standing' OR perms = 'basket:standing:list' OR component = 'basket/standing/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'standing', component = 'basket/standing/index', route_name = 'BasketStanding',
    perms = 'basket:standing:list', menu_type = 'C', visible = '0', status = '0',
    order_num = 15, update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_standing;

-- 2.3 球队赛季统计
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '球队赛季统计', @cnpa_root, 16, 'team-season-stat', 'basket/teamSeasonStat/index', '', 'BasketTeamSeasonStat',
    1, 0, 'C', '0', '0', 'basket:teamSeasonStat:list', 'peoples',
    @cnpa_create_by, @cnpa_now, '球队赛季汇总统计'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'team-season-stat' OR perms = 'basket:teamSeasonStat:list' OR component = 'basket/teamSeasonStat/index')
    LIMIT 1
);

SELECT menu_id INTO @m_team_stat
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'team-season-stat' OR perms = 'basket:teamSeasonStat:list' OR component = 'basket/teamSeasonStat/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'team-season-stat', component = 'basket/teamSeasonStat/index', route_name = 'BasketTeamSeasonStat',
    perms = 'basket:teamSeasonStat:list', menu_type = 'C', visible = '0', status = '0',
    order_num = 16, update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_team_stat;

-- 2.4 球员赛季统计
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '球员赛季统计', @cnpa_root, 17, 'player-season-stat', 'basket/playerSeasonStat/index', '', 'BasketPlayerSeasonStat',
    1, 0, 'C', '0', '0', 'basket:playerSeasonStat:list', 'user',
    @cnpa_create_by, @cnpa_now, '球员赛季汇总统计'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'player-season-stat' OR perms = 'basket:playerSeasonStat:list' OR component = 'basket/playerSeasonStat/index')
    LIMIT 1
);

SELECT menu_id INTO @m_player_stat
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'player-season-stat' OR perms = 'basket:playerSeasonStat:list' OR component = 'basket/playerSeasonStat/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'player-season-stat', component = 'basket/playerSeasonStat/index', route_name = 'BasketPlayerSeasonStat',
    perms = 'basket:playerSeasonStat:list', menu_type = 'C', visible = '0', status = '0',
    order_num = 17, update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_player_stat;

-- 2.5 排行榜
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '排行榜', @cnpa_root, 18, 'ranking', 'basket/ranking/index', '', 'BasketRanking',
    1, 0, 'C', '0', '0', 'basket:ranking:list', 'trophy',
    @cnpa_create_by, @cnpa_now, '球队/球员排行榜'
FROM DUAL
WHERE @cnpa_root IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (path = 'ranking' OR perms = 'basket:ranking:list' OR component = 'basket/ranking/index')
    LIMIT 1
);

SELECT menu_id INTO @m_ranking
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (path = 'ranking' OR perms = 'basket:ranking:list' OR component = 'basket/ranking/index')
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'ranking', component = 'basket/ranking/index', route_name = 'BasketRanking',
    perms = 'basket:ranking:list', menu_type = 'C', visible = '0', status = '0',
    order_num = 18, update_by = @cnpa_create_by, update_time = @cnpa_now
WHERE menu_id = @m_ranking;

-- -----------------------------------------------------------------------------
-- 3. 按钮权限（F）
-- -----------------------------------------------------------------------------

-- 3.1 数据结算按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '结算查询', @m_data_settlement, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:dataSettlement:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_data_settlement IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_data_settlement AND perms = 'basket:dataSettlement:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '结算重算', @m_data_settlement, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:dataSettlement:rebuild', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_data_settlement IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_data_settlement AND perms = 'basket:dataSettlement:rebuild' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '结算确认', @m_data_settlement, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:dataSettlement:confirm', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_data_settlement IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_data_settlement AND perms = 'basket:dataSettlement:confirm' LIMIT 1);

-- 3.2 积分榜按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '积分查询', @m_standing, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:standing:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_standing IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_standing AND perms = 'basket:standing:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '积分重算', @m_standing, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:standing:rebuild', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_standing IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_standing AND perms = 'basket:standing:rebuild' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '积分导出', @m_standing, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:standing:export', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_standing IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_standing AND perms = 'basket:standing:export' LIMIT 1);

-- 3.3 球队赛季统计按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球队统计查询', @m_team_stat, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:teamSeasonStat:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_team_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_team_stat AND perms = 'basket:teamSeasonStat:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球队统计重算', @m_team_stat, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:teamSeasonStat:rebuild', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_team_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_team_stat AND perms = 'basket:teamSeasonStat:rebuild' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球队统计导出', @m_team_stat, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:teamSeasonStat:export', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_team_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_team_stat AND perms = 'basket:teamSeasonStat:export' LIMIT 1);

-- 3.4 球员赛季统计按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球员统计查询', @m_player_stat, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:playerSeasonStat:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_player_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_player_stat AND perms = 'basket:playerSeasonStat:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球员统计重算', @m_player_stat, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:playerSeasonStat:rebuild', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_player_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_player_stat AND perms = 'basket:playerSeasonStat:rebuild' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球员统计导出', @m_player_stat, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:playerSeasonStat:export', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_player_stat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_player_stat AND perms = 'basket:playerSeasonStat:export' LIMIT 1);

-- 3.5 排行榜按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '排行查询', @m_ranking, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:ranking:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_ranking IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_ranking AND perms = 'basket:ranking:query' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '排行重算', @m_ranking, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:ranking:rebuild', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_ranking IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_ranking AND perms = 'basket:ranking:rebuild' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '排行发布', @m_ranking, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:ranking:publish', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_ranking IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_ranking AND perms = 'basket:ranking:publish' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '排行导出', @m_ranking, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'basket:ranking:export', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE @m_ranking IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_ranking AND perms = 'basket:ranking:export' LIMIT 1);

-- =============================================================================
-- 角色授权说明（本脚本不写入 sys_role_menu）
--   - 超级管理员按若依规则可见全部菜单
--   - 运营角色请在「系统管理 → 角色管理」勾选：
--       CNPA赛事管理 → 数据结算 / 积分榜 / 球队赛季统计 / 球员赛季统计 / 排行榜
--       及对应按钮权限（与 BasketSettlementPerms 一致）
-- =============================================================================
