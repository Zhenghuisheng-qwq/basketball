-- CNPA 动态赛制 R4 权限（幂等插入，不新增一级菜单）
SET @scheduleMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/schedule/index' LIMIT 1);
SET @statsMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/stats/index' LIMIT 1);

-- R4 MANUAL 人工排名（挂在统计页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2337, 'V4人工排名处理', @statsMenuId, 11, '', '', 'F', '0', '0', 'basket:v4Standing:manualRank', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Standing:manualRank');

-- R4 阶段初始化队伍（挂在赛程管理页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2338, 'V4阶段初始化队伍', @scheduleMenuId, 15, '', '', 'F', '0', '0', 'basket:v4Stage:initialize', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:initialize');

-- R4 取消晋级确认
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2339, 'V4取消晋级确认', @scheduleMenuId, 16, '', '', 'F', '0', '0', 'basket:v4Stage:qualificationCancel', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:qualificationCancel');

-- 管理员角色关联
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id BETWEEN 2337 AND 2339
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
