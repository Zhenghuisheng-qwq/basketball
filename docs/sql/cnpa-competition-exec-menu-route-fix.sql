-- CNPA 比赛执行：三个菜单 404 修复（仅 sys_menu 数据，不改表结构）
-- 1) 将比赛数据审核/数据争议/异常比赛挂到「比赛执行」(2153)
-- 2) 异常比赛处理指向真实 M07 页面 match-exception，沿用 basket:matchException:* 权限
-- 3) 停用孤儿菜单 2250（parent 2200 不存在），按钮权限挂到 2232

-- 比赛数据审核
UPDATE sys_menu
SET parent_id = 2153,
    order_num = 4,
    path = 'match-data-audit',
    component = 'cnpa/competition/match-data-audit/index',
    route_name = 'BasketMatchDataAudit',
    menu_name = '比赛数据审核',
    perms = 'basket:matchAudit:list',
    visible = '0',
    status = '0',
    update_time = NOW()
WHERE menu_id = 2230;

-- 数据争议处理
UPDATE sys_menu
SET parent_id = 2153,
    order_num = 5,
    path = 'match-dispute',
    component = 'cnpa/competition/match-dispute/index',
    route_name = 'BasketMatchDispute',
    menu_name = '数据争议处理',
    perms = 'basket:matchDispute:list',
    visible = '0',
    status = '0',
    update_time = NOW()
WHERE menu_id = 2231;

-- 异常比赛处理 → 真实 M07 页
UPDATE sys_menu
SET parent_id = 2153,
    order_num = 6,
    path = 'match-exception',
    component = 'cnpa/competition/match-exception/index',
    route_name = 'BasketMatchException',
    menu_name = '异常比赛处理',
    perms = 'basket:matchException:list',
    icon = 'bug',
    visible = '0',
    status = '0',
    remark = 'CNPA异常比赛申请(M07)',
    update_time = NOW()
WHERE menu_id = 2232;

-- 异常按钮权限挂到 2232
UPDATE sys_menu SET parent_id = 2232, update_time = NOW() WHERE menu_id IN (2251, 2252, 2253);

-- 停用孤儿「异常比赛」菜单（避免顶层/重复路由）
UPDATE sys_menu
SET visible = '1',
    status = '1',
    remark = '已停用-合并至比赛执行/异常比赛处理(2232)',
    update_time = NOW()
WHERE menu_id = 2250;

-- 隐藏「比赛管理」目录（子菜单已并入比赛执行；前端也会隐藏）
UPDATE sys_menu
SET visible = '1',
    remark = '已停用-子菜单已并入比赛执行',
    update_time = NOW()
WHERE menu_id = 2205;

-- 角色授权：比赛执行下三菜单 + 异常按钮（幂等）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM (SELECT 1 AS role_id UNION ALL SELECT 2) r
JOIN sys_menu m ON m.menu_id IN (2230, 2231, 2232, 2251, 2252, 2253);
