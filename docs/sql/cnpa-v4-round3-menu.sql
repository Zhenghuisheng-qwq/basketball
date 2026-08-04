-- CNPA V4 Round3: 队长授权 / 报名审核 / 赛事队伍快照 菜单与权限
-- 父菜单：赛事管理 (menu_id=2152, path=event)

SET @eventParentId = 2152;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2296, '队长授权', @eventParentId, 1, 'event-v4-captain-grant', 'cnpa/event-v4/captain-grant/index', 'CnpaEventV4CaptainGrant', 1, 0, 'C', '0', '0', 'basket:v4CaptainGrant:list', 'peoples', 'admin', sysdate(), 'CNPA V4 Division 队长授权'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2296 OR component = 'cnpa/event-v4/captain-grant/index');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2301, '报名审核', @eventParentId, 2, 'event-v4-registration-audit', 'cnpa/event-v4/registration-audit/index', 'CnpaEventV4RegistrationAudit', 1, 0, 'C', '0', '0', 'basket:v4RegistrationAudit:list', 'edit', 'admin', sysdate(), 'CNPA V4 球队报名审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2301 OR component = 'cnpa/event-v4/registration-audit/index');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2306, '参赛队伍', @eventParentId, 3, 'event-v4-event-team', 'cnpa/event-v4/event-team/index', 'CnpaEventV4EventTeam', 1, 0, 'C', '0', '0', 'basket:v4EventTeam:list', 'team', 'admin', sysdate(), 'CNPA V4 EventTeam/EventPlayer 快照只读'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2306 OR component = 'cnpa/event-v4/event-team/index');

SET @v4CaptainGrantMenuId = 2296;
SET @v4RegistrationAuditMenuId = 2301;
SET @v4EventTeamMenuId = 2306;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2297, 'V4 CaptainGrant查询', @v4CaptainGrantMenuId, 1, '', '', 'F', '0', '0', 'basket:v4CaptainGrant:query', '#', 'admin', sysdate()),
(2298, 'V4 CaptainGrant新增', @v4CaptainGrantMenuId, 2, '', '', 'F', '0', '0', 'basket:v4CaptainGrant:add', '#', 'admin', sysdate()),
(2299, 'V4 CaptainGrant撤销', @v4CaptainGrantMenuId, 3, '', '', 'F', '0', '0', 'basket:v4CaptainGrant:revoke', '#', 'admin', sysdate()),
(2302, 'V4 RegistrationAudit查询', @v4RegistrationAuditMenuId, 1, '', '', 'F', '0', '0', 'basket:v4RegistrationAudit:query', '#', 'admin', sysdate()),
(2303, 'V4 RegistrationAudit通过', @v4RegistrationAuditMenuId, 2, '', '', 'F', '0', '0', 'basket:v4RegistrationAudit:approve', '#', 'admin', sysdate()),
(2304, 'V4 RegistrationAudit驳回', @v4RegistrationAuditMenuId, 3, '', '', 'F', '0', '0', 'basket:v4RegistrationAudit:reject', '#', 'admin', sysdate()),
(2307, 'V4 EventTeam查询', @v4EventTeamMenuId, 1, '', '', 'F', '0', '0', 'basket:v4EventTeam:query', '#', 'admin', sysdate()),
(2308, 'V4 EventPlayer查询', @v4EventTeamMenuId, 2, '', '', 'F', '0', '0', 'basket:v4EventPlayer:list', '#', 'admin', sysdate())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 隐藏旧授权/报名/快照入口（保留页面文件，后续轮次删除）
UPDATE sys_menu SET visible = '1', remark = CONCAT(IFNULL(remark, ''), ' [V4 R3 deprecated]')
WHERE menu_id IN (2002, 2007, 2221, 2222)
   OR component IN (
       'cnpa/identity/captain-qualification/index',
       'cnpa/participation/team-registration/index',
       'cnpa/participation/official-teams/index',
       'cnpa/participation/event-roster/index'
   );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id BETWEEN 2296 AND 2308
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
