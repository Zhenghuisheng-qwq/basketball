-- CNPA 比赛数据审核权限补丁（第三轮）
-- 补齐 query / approve 按钮权限，不破坏已有菜单

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2234, '查询比赛数据提交', 2230, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'basket:matchAudit:query', '#', 'admin', NOW(), 'CNPA比赛数据提交详情/版本')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), menu_name = VALUES(menu_name);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2235, '审核通过比赛数据', 2230, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'basket:matchAudit:approve', '#', 'admin', NOW(), 'CNPA比赛数据审核通过')
ON DUPLICATE KEY UPDATE perms = VALUES(perms), menu_name = VALUES(menu_name);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2234);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2235);
