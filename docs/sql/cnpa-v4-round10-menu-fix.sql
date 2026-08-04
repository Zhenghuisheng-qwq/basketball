-- CNPA V4 Round10: 修复后台菜单缺失 — 重组父子关系、隐藏旧入口、补全角色授权
-- 根因：V4 子菜单已写入 sys_menu，但 sys_role_menu 未同步；旧子菜单 visible=1 隐藏后角色仍绑定旧 ID
-- 执行：dev / staging / prod 按需执行（可重复）

-- ============================================================
-- 1. 赛事管理：V4 主线子菜单排序与命名
-- ============================================================
UPDATE sys_menu SET menu_name = '赛事发布', order_num = 1, visible = '0', status = '0',
  remark = 'Series/Season/Division 发布（赛事系列→赛季→赛区）'
WHERE menu_id = 2280;

UPDATE sys_menu SET menu_name = '队长授权', order_num = 2, visible = '0', status = '0', parent_id = 2152
WHERE menu_id = 2296;

UPDATE sys_menu SET menu_name = '报名审核', order_num = 3, visible = '0', status = '0', parent_id = 2152
WHERE menu_id = 2301;

UPDATE sys_menu SET menu_name = '参赛队伍', order_num = 4, visible = '0', status = '0', parent_id = 2152,
  remark = 'EventTeam 快照；页内「参赛球员」Tab 查看 EventPlayer'
WHERE menu_id = 2306;

-- 隐藏旧赛事域入口（保留 component 供历史兼容，不再展示）
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R10 deprecated]')
WHERE menu_id IN (2004, 2005, 2006, 2038, 2039)
   OR component IN (
     'cnpa/event/list/index', 'cnpa/event/season/index', 'cnpa/event/rule/index',
     'cnpa/event/stage/index', 'cnpa/event/group/index'
   );

-- ============================================================
-- 2. 比赛执行：迁入 V4 赛程/比赛/数据审核；隐藏旧 stub
-- ============================================================
UPDATE sys_menu SET parent_id = 2153, menu_name = '赛程管理', order_num = 1, visible = '0', status = '0'
WHERE menu_id = 2311;

UPDATE sys_menu SET parent_id = 2153, menu_name = '比赛管理', order_num = 2, visible = '0', status = '0'
WHERE menu_id = 2316;

UPDATE sys_menu SET parent_id = 2153, menu_name = '比赛数据审核', order_num = 3, visible = '0', status = '0'
WHERE menu_id = 2321;

UPDATE sys_menu SET menu_name = '数据争议处理', order_num = 4, visible = '0', status = '0', parent_id = 2153
WHERE menu_id = 2231;

UPDATE sys_menu SET menu_name = '弃赛与异常处理', order_num = 5, visible = '0', status = '0', parent_id = 2153
WHERE menu_id = 2232;

-- 隐藏旧比赛执行/比赛管理重复入口
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R10 deprecated]')
WHERE menu_id IN (2040, 2041, 2042, 2043, 2205)
   OR (parent_id = 2205 AND menu_type = 'C');

UPDATE sys_menu SET visible = '1' WHERE menu_id = 2205;

-- ============================================================
-- 3. 数据统计：迁入 V4 统计榜单，隐藏旧统计页
-- ============================================================
UPDATE sys_menu SET parent_id = 2154, menu_name = '统计榜单', order_num = 1, visible = '0', status = '0'
WHERE menu_id = 2326;

UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R10 deprecated]')
WHERE parent_id = 2154 AND menu_id != 2326 AND menu_type = 'C';

UPDATE sys_menu SET visible = '1' WHERE menu_id = 2154 AND NOT EXISTS (
  SELECT 1 FROM sys_menu c WHERE c.parent_id = 2154 AND c.visible = '0' AND c.menu_type = 'C' AND c.status = '0'
);

-- 若 2154 仍有可见子菜单则保持显示
UPDATE sys_menu SET visible = '0' WHERE menu_id = 2154;

-- ============================================================
-- 4. 报名与阵容：整组隐藏（功能已收口至赛事管理 V4 菜单）
-- ============================================================
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R10 deprecated]')
WHERE menu_id = 2204 OR parent_id = 2204;

-- ============================================================
-- 5. 账号与认证：整理显示名与排序
-- ============================================================
UPDATE sys_menu SET menu_name = '用户管理', order_num = 1, visible = '0' WHERE menu_id = 2210;
UPDATE sys_menu SET menu_name = '球员认证审核', order_num = 2, visible = '0' WHERE menu_id = 2001;
UPDATE sys_menu SET visible = '1', remark = CONCAT(IFNULL(remark, ''), ' [V4 R10 暂不开放]') WHERE menu_id = 2002;
UPDATE sys_menu SET menu_name = '球队认证审核', order_num = 3, visible = '0' WHERE menu_id = 2003;
UPDATE sys_menu SET menu_name = '平台游戏ID变更审核', order_num = 4, visible = '0' WHERE menu_id = 2211;
UPDATE sys_menu SET menu_name = '球队资料变更审核', order_num = 5, visible = '0' WHERE menu_id = 2215;

-- ============================================================
-- 6. 纪律与档案：运营名称
-- ============================================================
UPDATE sys_menu SET menu_name = '处罚管理', order_num = 1, visible = '0' WHERE menu_id = 2240;
UPDATE sys_menu SET menu_name = '球员赛事档案', order_num = 2, visible = '0' WHERE menu_id = 2241;
UPDATE sys_menu SET menu_name = '球队赛事档案', order_num = 3, visible = '0' WHERE menu_id = 2242;
UPDATE sys_menu SET menu_name = '操作审计', order_num = 4, visible = '0' WHERE menu_id = 2243;
UPDATE sys_menu SET visible = '1', remark = CONCAT(IFNULL(remark, ''), ' [V4 R10 内部能力]') WHERE menu_id = 2244;

-- ============================================================
-- 7. 内容运营：修正首页内容可见性
-- ============================================================
UPDATE sys_menu SET menu_name = '首页内容配置', visible = '0', order_num = 6 WHERE menu_id = 2107;

-- ============================================================
-- 8. 一级菜单排序
-- ============================================================
UPDATE sys_menu SET order_num = 0 WHERE menu_id = 2201;
UPDATE sys_menu SET order_num = 1 WHERE menu_id = 2152;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 2151;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 2153;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 2207;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 2101;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 2154;
UPDATE sys_menu SET order_num = 100 WHERE menu_id = 1;

-- ============================================================
-- 9. 角色授权：超级管理员 role_id=1
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.status = '0'
  AND (
    m.menu_id IN (2201, 2152, 2151, 2153, 2154, 2207, 2101)
    OR m.menu_id BETWEEN 2280 AND 2295
    OR m.menu_id BETWEEN 2296 AND 2305
    OR m.menu_id BETWEEN 2306 AND 2310
    OR m.menu_id BETWEEN 2311 AND 2330
    OR m.parent_id IN (2152, 2153, 2154, 2151, 2207, 2101, 2201)
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- ============================================================
-- 10. 角色授权：运营角色 role_id=2（CNPA 业务菜单，不含系统管理）
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.menu_id FROM sys_menu m
WHERE m.status = '0'
  AND m.visible = '0'
  AND m.menu_type IN ('M', 'C')
  AND (
    m.menu_id IN (2201, 2152, 2151, 2153, 2154, 2207, 2101)
    OR m.parent_id IN (2152, 2153, 2154, 2151, 2207, 2101)
    OR m.menu_id BETWEEN 2280 AND 2330
  )
  AND m.menu_id NOT IN (1)
  AND m.parent_id != 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 2 AND rm.menu_id = m.menu_id);

-- 清理 role_id=2 对已废弃隐藏菜单的绑定（减少空目录干扰）
DELETE rm FROM sys_role_menu rm
WHERE rm.role_id = 2
  AND rm.menu_id IN (
    2004, 2005, 2006, 2038, 2039,
    2040, 2041, 2042, 2043,
    2007, 2221, 2222, 2223, 2224, 2225, 2226,
    2080, 2081, 2082, 2083, 2084, 2102,
    2204, 2205
  );
