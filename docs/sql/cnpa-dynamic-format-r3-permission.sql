-- CNPA 动态赛制 R2/R3 权限（幂等插入，不新增一级菜单）
SET @eventParentId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/publish/index' LIMIT 1);
SET @publishMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/publish/index' LIMIT 1);
SET @regAuditMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/registration-audit/index' LIMIT 1);
SET @eventTeamMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/event-team/index' LIMIT 1);
SET @scheduleMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/schedule/index' LIMIT 1);
SET @statsMenuId = (SELECT menu_id FROM sys_menu WHERE component = 'cnpa/event-v4/stats/index' LIMIT 1);

-- R2 赛制规则按钮（挂在发布页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2320, 'V4赛制规则查看', @publishMenuId, 10, '', '', 'F', '0', '0', 'basket:v4FormatRule:view', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4FormatRule:view');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2321, 'V4赛制规则编辑', @publishMenuId, 11, '', '', 'F', '0', '0', 'basket:v4FormatRule:edit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4FormatRule:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2322, 'V4赛制规则锁定', @publishMenuId, 12, '', '', 'F', '0', '0', 'basket:v4FormatRule:lock', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4FormatRule:lock');

-- R2 个人报名/抽签（挂在报名审核页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2323, 'V4个人报名列表', @regAuditMenuId, 10, '', '', 'F', '0', '0', 'basket:v4PersonalReg:list', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4PersonalReg:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2324, 'V4个人报名审核', @regAuditMenuId, 11, '', '', 'F', '0', '0', 'basket:v4PersonalReg:audit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4PersonalReg:audit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2325, 'V4个人成队列表', @regAuditMenuId, 12, '', '', 'F', '0', '0', 'basket:v4PersonalFormation:list', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4PersonalFormation:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2326, 'V4个人成队编辑', @regAuditMenuId, 13, '', '', 'F', '0', '0', 'basket:v4PersonalFormation:edit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4PersonalFormation:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2327, 'V4个人成队确认', @regAuditMenuId, 14, '', '', 'F', '0', '0', 'basket:v4PersonalFormation:confirm', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4PersonalFormation:confirm');

-- R2 赛事分组（挂在参赛队伍页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2328, 'V4赛事分组列表', @eventTeamMenuId, 10, '', '', 'F', '0', '0', 'basket:v4Grouping:list', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Grouping:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2329, 'V4赛事分组编辑', @eventTeamMenuId, 11, '', '', 'F', '0', '0', 'basket:v4Grouping:edit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Grouping:edit');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2330, 'V4赛事分组锁定', @eventTeamMenuId, 12, '', '', 'F', '0', '0', 'basket:v4Grouping:lock', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Grouping:lock');

-- R3 阶段执行（挂在赛程管理页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2331, 'V4阶段列表', @scheduleMenuId, 10, '', '', 'F', '0', '0', 'basket:v4Stage:list', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:list');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2332, 'V4阶段生成赛程', @scheduleMenuId, 11, '', '', 'F', '0', '0', 'basket:v4Stage:generate', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:generate');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2333, 'V4阶段发布赛程', @scheduleMenuId, 12, '', '', 'F', '0', '0', 'basket:v4Stage:publish', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:publish');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2334, 'V4晋级预览', @scheduleMenuId, 13, '', '', 'F', '0', '0', 'basket:v4Stage:qualificationPreview', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:qualificationPreview');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2335, 'V4晋级确认', @scheduleMenuId, 14, '', '', 'F', '0', '0', 'basket:v4Stage:qualificationConfirm', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:qualificationConfirm');

-- R3 积分榜查看（挂在统计页）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2336, 'V4积分榜查看', @statsMenuId, 10, '', '', 'F', '0', '0', 'basket:v4Standing:view', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Standing:view');

-- 管理员角色关联
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id BETWEEN 2320 AND 2336
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
