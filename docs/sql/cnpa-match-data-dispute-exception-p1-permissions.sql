-- CNPA Match Data Dispute & Exception P1 permissions (idempotent)

-- M06 数据争议
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2240, '数据争议', 2200, 6, 'match-dispute', 'cnpa/competition/match-dispute/index', NULL, 'MatchDispute', 1, 0, 'C', '0', '0', 'basket:matchDispute:list', 'form', 'admin', NOW(), '', NULL, 'CNPA比赛数据争议'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2240);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2241, '争议查询', 2240, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2241);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2242, '争议驳回', 2240, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:reject', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2242);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2243, '争议采纳', 2240, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:uphold', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2243);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2244, '争议修正', 2240, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchDispute:correct', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2244);

-- M07 弃赛异常
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2250, '异常比赛', 2200, 7, 'match-exception', 'cnpa/competition/match-exception/index', NULL, 'MatchException', 1, 0, 'C', '0', '0', 'basket:matchException:list', 'bug', 'admin', NOW(), '', NULL, 'CNPA异常比赛申请'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2250);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2251, '异常查询', 2250, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchException:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2251);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2252, '异常驳回', 2250, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchException:reject', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2252);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2253, '异常裁定', 2250, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:matchException:decide', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2253);

-- 管理员角色授权（role_id=1 超级管理员）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (2240,2241,2242,2243,2244,2250,2251,2252,2253)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
