-- Restore Series/Season foundation page after R8 reused menu 2280 as Rule workspace.
-- No business tables. Idempotent.

SET @eventParentId = 2152;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2353, '赛季管理', @eventParentId, 0, 'event-v4-season-manage', 'cnpa/event-v4/season-manage/index', 'CnpaEventV4SeasonManage', 1, 0, 'C', '0', '0', 'basket:v4Season:list', 'list', 'admin', sysdate(), 'Series/Season/Division 基础资料（从发布赛事页拆出）'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE menu_id = 2353 OR component = 'cnpa/event-v4/season-manage/index' OR path = 'event-v4-season-manage'
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 2353
FROM sys_role r
WHERE r.role_id IN (1, 2)
  AND EXISTS (SELECT 1 FROM sys_menu m WHERE m.menu_id = 2353)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 2353);
