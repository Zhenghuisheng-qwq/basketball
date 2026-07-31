-- CNPA P1 争议权限补丁：2240-2244 已被占用，改用 2260-2263 挂载到现有菜单 2231
-- idempotent

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2260, '争议查询', 2231, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:matchDispute:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2261, '争议驳回', 2231, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:reject', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:matchDispute:reject');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2262, '争议采纳', 2231, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:uphold', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:matchDispute:uphold');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2263, '争议修正', 2231, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:correct', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:matchDispute:correct');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN ('basket:matchDispute:query','basket:matchDispute:reject','basket:matchDispute:uphold','basket:matchDispute:correct')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
