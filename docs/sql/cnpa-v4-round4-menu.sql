-- CNPA V4 Round4: 赛程管理 / 比赛列表 菜单与权限
SET @eventParentId = 2152;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2311, 'V4赛程管理', @eventParentId, 4, 'event-v4-schedule', 'cnpa/event-v4/schedule/index', 'CnpaEventV4Schedule', 1, 0, 'C', '0', '0', 'basket:v4ScheduleBatch:list', 'date', 'admin', sysdate(), 'CNPA V4 Division 单循环赛程生成'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2311 OR component = 'cnpa/event-v4/schedule/index');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2316, 'V4比赛列表', @eventParentId, 5, 'event-v4-match', 'cnpa/event-v4/match/index', 'CnpaEventV4Match', 1, 0, 'C', '0', '0', 'basket:v4Match:list', 'list', 'admin', sysdate(), 'CNPA V4 Match 管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2316 OR component = 'cnpa/event-v4/match/index');

SET @v4ScheduleMenuId = 2311;
SET @v4MatchMenuId = 2316;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2312, 'V4 ScheduleBatch查询', @v4ScheduleMenuId, 1, '', '', 'F', '0', '0', 'basket:v4ScheduleBatch:query', '#', 'admin', sysdate()),
(2313, 'V4 ScheduleBatch生成', @v4ScheduleMenuId, 2, '', '', 'F', '0', '0', 'basket:v4ScheduleBatch:generate', '#', 'admin', sysdate()),
(2314, 'V4 ScheduleBatch发布', @v4ScheduleMenuId, 3, '', '', 'F', '0', '0', 'basket:v4ScheduleBatch:publish', '#', 'admin', sysdate()),
(2315, 'V4 ScheduleBatch作废', @v4ScheduleMenuId, 4, '', '', 'F', '0', '0', 'basket:v4ScheduleBatch:cancel', '#', 'admin', sysdate()),
(2317, 'V4 Match查询', @v4MatchMenuId, 1, '', '', 'F', '0', '0', 'basket:v4Match:query', '#', 'admin', sysdate()),
(2318, 'V4 Match编辑', @v4MatchMenuId, 2, '', '', 'F', '0', '0', 'basket:v4Match:edit', '#', 'admin', sysdate()),
(2319, 'V4 Match删除', @v4MatchMenuId, 3, '', '', 'F', '0', '0', 'basket:v4Match:remove', '#', 'admin', sysdate())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 隐藏/标废弃旧赛程与比赛入口
UPDATE sys_menu SET visible = '1', remark = CONCAT(IFNULL(remark, ''), ' [V4 R4 deprecated]')
WHERE menu_id IN (2040, 2041, 2043, 2205, 2230, 2231, 2232, 2250)
   OR component IN (
       'cnpa/competition/schedule/index',
       'cnpa/competition/schedule-manage/index',
       'cnpa/competition/lineup/index',
       'cnpa/competition/match-data-entry/index',
       'cnpa/competition/match-data-audit/index',
       'cnpa/competition/match-dispute/index',
       'cnpa/competition/match-exception/index'
   );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id BETWEEN 2311 AND 2319
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
