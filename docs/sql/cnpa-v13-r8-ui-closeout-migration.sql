-- CNPA V1.3 R8 UI closeout: menu rename / reorder / 3 new entries. No core business tables.

SET @eventParentId = 2152;

UPDATE sys_menu SET parent_id = 2152, menu_name = '发布赛事', order_num = 1, visible = '0', status = '0',
  remark = 'R8 01 publish rules'
WHERE menu_id = 2280;

UPDATE sys_menu SET parent_id = 2152, menu_name = '报名与参赛名单', order_num = 2, visible = '0', status = '0',
  remark = 'R8 02 roster'
WHERE menu_id = 2301;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2350, '赛程管理概览', @eventParentId, 3, 'event-v4-overview', 'cnpa/event-v4/overview/index', 'CnpaEventV4Overview', 1, 0, 'C', '0', '0', 'basket:v4Stage:list', 'chart', 'admin', sysdate(), 'R8 03 overview'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2350 OR component = 'cnpa/event-v4/overview/index');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2351, '赛程结构生成', @eventParentId, 4, 'event-v4-structure', 'cnpa/event-v4/structure/index', 'CnpaEventV4Structure', 1, 0, 'C', '0', '0', 'basket:v4Stage:generate', 'tree-table', 'admin', sysdate(), 'R8 04 structure'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2351 OR component = 'cnpa/event-v4/structure/index');

UPDATE sys_menu SET parent_id = 2152, menu_name = '日期与时间排期', order_num = 5, visible = '0', status = '0',
  path = 'event-v4-schedule', component = 'cnpa/event-v4/schedule/index',
  route_name = 'CnpaEventV4Schedule', perms = 'basket:v4ScheduleBatch:list', icon = 'date',
  remark = 'R8 05 schedule'
WHERE menu_id = 2311;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2352, '赛程预览与发布', @eventParentId, 6, 'event-v4-publish-preview', 'cnpa/event-v4/publish-preview/index', 'CnpaEventV4PublishPreview', 1, 0, 'C', '0', '0', 'basket:v4Stage:publish', 'upload', 'admin', sysdate(), 'R8 06 publish'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2352 OR component = 'cnpa/event-v4/publish-preview/index');

UPDATE sys_menu SET parent_id = 2152, order_num = 7 WHERE menu_id = 2296;
UPDATE sys_menu SET parent_id = 2152, order_num = 8 WHERE menu_id = 2306;
UPDATE sys_menu SET parent_id = 2152, order_num = 9 WHERE menu_id = 2326;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (2350, 2351, 2352)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (2280, 2301, 2350, 2351, 2311, 2352)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 2 AND rm.menu_id = m.menu_id);
