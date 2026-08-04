-- CNPA V4 后台菜单运营名称优化（去 V4 展示前缀，不改 path/perms/component）
-- 执行环境：dev / staging / prod 按需执行

UPDATE sys_menu SET menu_name = '赛事发布', remark = 'CNPA V4 Series/Season/Division 发布' WHERE menu_id = 2280;
UPDATE sys_menu SET menu_name = '队长授权', remark = 'CNPA V4 Division 队长授权' WHERE menu_id = 2296;
UPDATE sys_menu SET menu_name = '报名审核', remark = 'CNPA V4 球队报名审核' WHERE menu_id = 2301;
UPDATE sys_menu SET menu_name = '参赛队伍', remark = 'CNPA V4 EventTeam/EventPlayer 快照只读' WHERE menu_id = 2306;
UPDATE sys_menu SET menu_name = '赛程管理', remark = 'CNPA V4 Division 单循环赛程生成' WHERE menu_id = 2311;
UPDATE sys_menu SET menu_name = '比赛列表', remark = 'CNPA V4 Match 管理' WHERE menu_id = 2316;
UPDATE sys_menu SET menu_name = '数据审核', remark = 'CNPA V4 比赛数据 submission 级审核' WHERE menu_id = 2321;
UPDATE sys_menu SET menu_name = '统计榜单', remark = 'CNPA V4 Standing/TeamStat/PlayerStat/Rank' WHERE menu_id = 2326;

-- 补缺失的参赛队伍菜单（若 round3 未完整执行）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2306, '参赛队伍', 2152, 3, 'event-v4-event-team', 'cnpa/event-v4/event-team/index', 'CnpaEventV4EventTeam', 1, 0, 'C', '0', '0', 'basket:v4EventTeam:list', 'team', 'admin', sysdate(), 'CNPA V4 EventTeam/EventPlayer 快照只读'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2306 OR component = 'cnpa/event-v4/event-team/index');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id IN (2306, 2307, 2308)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
