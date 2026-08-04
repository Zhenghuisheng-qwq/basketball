-- CNPA V4 Round11: 后台菜单最终收口（可重复执行）
-- 根因：旧域「比赛管理」一级目录(2205)与 V4「比赛执行/比赛管理」(2316)并存；
--       旧比赛执行子菜单(2040/2230)未隐藏；前端 permission.ts 曾注入重复路由掩盖问题。

-- ============================================================
-- 1. 一级菜单排序与废弃目录
-- ============================================================
UPDATE sys_menu SET order_num = 0, visible = '0', status = '0' WHERE menu_id = 2201;
UPDATE sys_menu SET order_num = 1, visible = '0', status = '0' WHERE menu_id = 2152;
UPDATE sys_menu SET order_num = 2, visible = '0', status = '0' WHERE menu_id = 2151;
UPDATE sys_menu SET order_num = 3, visible = '0', status = '0' WHERE menu_id = 2153;
UPDATE sys_menu SET order_num = 4, visible = '0', status = '0' WHERE menu_id = 2207;
UPDATE sys_menu SET order_num = 5, visible = '0', status = '0' WHERE menu_id = 2101;
UPDATE sys_menu SET order_num = 100, visible = '0', status = '0' WHERE menu_id = 1;

UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 deprecated]')
WHERE menu_id IN (2154, 2204, 2205, 2000);

UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 deprecated]')
WHERE parent_id = 2000;

-- ============================================================
-- 2. 赛事管理（Series/Season/Division 主线）
-- ============================================================
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 deprecated]')
WHERE menu_id IN (2004, 2005, 2006, 2038, 2039)
   OR component IN (
     'cnpa/event/list/index', 'cnpa/event/season/index', 'cnpa/event/rule/index',
     'cnpa/event/stage/index', 'cnpa/event/group/index'
   );

UPDATE sys_menu SET parent_id = 2152, menu_name = '赛事发布', order_num = 1, visible = '0', status = '0',
  path = 'event-v4-publish', component = 'cnpa/event-v4/publish/index', route_name = 'CnpaEventV4Publish',
  perms = 'basket:v4Season:list', icon = 'guide',
  remark = 'Series/Season/Division 发布与赛制规则配置入口'
WHERE menu_id = 2280;

UPDATE sys_menu SET parent_id = 2152, menu_name = '队长授权', order_num = 2, visible = '0', status = '0',
  path = 'event-v4-captain-grant', component = 'cnpa/event-v4/captain-grant/index',
  route_name = 'CnpaEventV4CaptainGrant', perms = 'basket:v4CaptainGrant:list', icon = 'peoples'
WHERE menu_id = 2296;

UPDATE sys_menu SET parent_id = 2152, menu_name = '报名审核', order_num = 3, visible = '0', status = '0',
  path = 'event-v4-registration-audit', component = 'cnpa/event-v4/registration-audit/index',
  route_name = 'CnpaEventV4RegistrationAudit', perms = 'basket:v4RegistrationAudit:list', icon = 'edit'
WHERE menu_id = 2301;

UPDATE sys_menu SET parent_id = 2152, menu_name = '参赛队伍', order_num = 4, visible = '0', status = '0',
  path = 'event-v4-event-team', component = 'cnpa/event-v4/event-team/index',
  route_name = 'CnpaEventV4EventTeam', perms = 'basket:v4EventTeam:list', icon = 'tree',
  remark = 'EventTeam 快照；页内 Tab 查看 EventPlayer'
WHERE menu_id = 2306;

UPDATE sys_menu SET parent_id = 2152, menu_name = '统计榜单', order_num = 5, visible = '0', status = '0',
  path = 'event-v4-stats', component = 'cnpa/event-v4/stats/index',
  route_name = 'CnpaEventV4Stats', perms = 'basket:v4Stats:query', icon = 'chart'
WHERE menu_id = 2326;

UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 deprecated]')
WHERE parent_id = 2154;

-- ============================================================
-- 3. 比赛执行（赛程 → 比赛 → 数据审核）
-- ============================================================
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 deprecated]')
WHERE menu_id IN (2040, 2041, 2042, 2043, 2230)
   OR (parent_id = 2205 AND menu_type = 'C');

UPDATE sys_menu SET parent_id = 2153, menu_name = '赛程管理', order_num = 1, visible = '0', status = '0',
  path = 'event-v4-schedule', component = 'cnpa/event-v4/schedule/index',
  route_name = 'CnpaEventV4Schedule', perms = 'basket:v4ScheduleBatch:list', icon = 'date'
WHERE menu_id = 2311;

UPDATE sys_menu SET parent_id = 2153, menu_name = '比赛管理', order_num = 2, visible = '0', status = '0',
  path = 'event-v4-match', component = 'cnpa/event-v4/match/index',
  route_name = 'CnpaEventV4Match', perms = 'basket:v4Match:list', icon = 'list'
WHERE menu_id = 2316;

UPDATE sys_menu SET parent_id = 2153, menu_name = '比赛数据审核', order_num = 3, visible = '0', status = '0',
  path = 'event-v4-match-submission-audit', component = 'cnpa/event-v4/match-submission-audit/index',
  route_name = 'CnpaEventV4MatchSubmissionAudit', perms = 'basket:v4MatchSubmissionAudit:list', icon = 'checkbox'
WHERE menu_id = 2321;

-- 争议/异常并入「比赛数据审核」页签，不再单独展示
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 merged to match-submission-audit]')
WHERE menu_id IN (2231, 2232, 2250);

-- ============================================================
-- 4. 账号与认证
-- ============================================================
UPDATE sys_menu SET menu_name = '用户管理', order_num = 1, visible = '0', status = '0' WHERE menu_id = 2210;
UPDATE sys_menu SET menu_name = '球员认证审核', order_num = 2, visible = '0', status = '0' WHERE menu_id = 2001;
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 deprecated - use event captain grant]')
WHERE menu_id = 2002;
UPDATE sys_menu SET menu_name = '球队认证审核', order_num = 3, visible = '0', status = '0' WHERE menu_id = 2003;
UPDATE sys_menu SET menu_name = '平台游戏ID变更审核', order_num = 4, visible = '0', status = '0' WHERE menu_id = 2211;
UPDATE sys_menu SET menu_name = '球队资料变更审核', order_num = 5, visible = '0', status = '0' WHERE menu_id = 2215;

-- ============================================================
-- 5. 纪律与档案
-- ============================================================
UPDATE sys_menu SET menu_name = '处罚管理', order_num = 1, visible = '0', status = '0', icon = 'build' WHERE menu_id = 2240;
UPDATE sys_menu SET menu_name = '球员档案', order_num = 2, visible = '0', status = '0', icon = 'user' WHERE menu_id = 2241;
UPDATE sys_menu SET menu_name = '球队档案', order_num = 3, visible = '0', status = '0', icon = 'peoples' WHERE menu_id = 2242;
UPDATE sys_menu SET menu_name = '业务审计记录', order_num = 4, visible = '0', status = '0', icon = 'log' WHERE menu_id = 2243;

-- 赛事运营授权 → 系统管理
UPDATE sys_menu SET parent_id = 1, menu_name = '赛事运营授权', order_num = 0, visible = '0', status = '0',
  path = 'tournament-scope', component = 'cnpa/discipline/tournament-scope/index',
  route_name = 'BasketTournamentScope', perms = 'basket:tournamentScope:list', icon = 'lock'
WHERE menu_id = 2244;

-- ============================================================
-- 6. 内容运营（荣誉收口至名人堂管理）
-- ============================================================
UPDATE sys_menu SET menu_name = '公告管理', order_num = 1, visible = '0', status = '0' WHERE menu_id = 2106;
UPDATE sys_menu SET menu_name = '合作伙伴管理', order_num = 2, visible = '0', status = '0' WHERE menu_id = 2108;
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 merged to hall-of-fame]')
WHERE menu_id IN (2103, 2104);
UPDATE sys_menu SET menu_name = '名人堂管理', order_num = 3, visible = '0', status = '0' WHERE menu_id = 2105;
UPDATE sys_menu SET visible = '1', status = '0',
  remark = CONCAT(IFNULL(remark, ''), ' [V4 R11 internal]')
WHERE menu_id = 2107;

-- ============================================================
-- 7. 角色菜单：超级管理员
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.status = '0'
  AND (
    m.menu_id IN (2201, 2152, 2151, 2153, 2207, 2101, 1, 2244)
    OR m.menu_id BETWEEN 2280 AND 2330
    OR m.parent_id IN (2152, 2153, 2151, 2207, 2101, 2201, 1)
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- ============================================================
-- 8. 角色菜单：运营角色 role_id=2（不含系统管理）
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.menu_id FROM sys_menu m
WHERE m.status = '0'
  AND m.visible = '0'
  AND m.menu_type IN ('M', 'C')
  AND (
    m.menu_id IN (2201, 2152, 2151, 2153, 2207, 2101)
    OR m.parent_id IN (2152, 2153, 2151, 2207, 2101)
    OR m.menu_id BETWEEN 2280 AND 2330
  )
  AND m.menu_id NOT IN (1, 2244)
  AND m.parent_id NOT IN (1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 2 AND rm.menu_id = m.menu_id);

UPDATE sys_menu SET order_num = 3 WHERE menu_id = 2153;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 2207;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 2101;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 2215;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 2105;

DELETE rm FROM sys_role_menu rm
WHERE rm.role_id = 2
  AND rm.menu_id IN (
    2000, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016,
    2038, 2039, 2040, 2041, 2042, 2043, 2044, 2045, 2046, 2047, 2048,
    2080, 2081, 2082, 2083, 2084, 2102, 2103, 2104, 2107,
    2204, 2205, 2230, 2231, 2232, 2250, 2154, 2244
  );
