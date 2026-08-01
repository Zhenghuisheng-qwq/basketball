-- CNPA 赛程管理：菜单指向 schedule-manage + 权限按钮（幂等）

UPDATE sys_menu
SET menu_name = '赛程管理',
    component = 'cnpa/competition/schedule-manage/index',
    route_name = 'BasketScheduleManage',
    perms = 'basket:schedule:list',
    remark = 'CNPA循环赛草稿生成与发布',
    update_time = NOW()
WHERE menu_id = 2040;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20401, '赛程查询', 2040, 1, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:query', '#', 'admin', NOW(), 'CNPA赛程查询'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20401);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20402, '生成草稿', 2040, 2, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:generate', '#', 'admin', NOW(), 'CNPA生成草稿赛程'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20402);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20403, '发布赛程', 2040, 3, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:publish', '#', 'admin', NOW(), 'CNPA发布/下架赛程'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20403);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20404, '删除草稿', 2040, 4, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:remove', '#', 'admin', NOW(), 'CNPA删除草稿赛程'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20404);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20405, '新增比赛', 2040, 5, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:add', '#', 'admin', NOW(), 'CNPA手动新增草稿比赛'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20405);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20406, '编辑比赛', 2040, 6, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:edit', '#', 'admin', NOW(), 'CNPA编辑/互换草稿比赛'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20406);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20407, '删除比赛', 2040, 7, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:delete', '#', 'admin', NOW(), 'CNPA删除草稿比赛'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20407);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 20408, '录入比分', 2040, 8, '#', '', '', 1, 0, 'F', '0', '0', 'basket:schedule:score', '#', 'admin', NOW(), 'CNPA录入比分'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 20408);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id BETWEEN 20401 AND 20408;
