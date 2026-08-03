-- CNPA V4 Round2: 赛事发布 V4 菜单与权限
-- 父菜单：赛事管理 (menu_id=2152, path=event)

SET @eventParentId = 2152;

-- 菜单 ID 2280-2295：避开已占用的 2260-2263（争议管理按钮权限）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2280, 'V4赛事发布', @eventParentId, 0, 'event-v4-publish', 'cnpa/event-v4/publish/index', 'CnpaEventV4Publish', 1, 0, 'C', '0', '0', 'basket:v4Season:list', 'guide', 'admin', sysdate(), 'CNPA V4 Series/Season/Division 发布'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2280 OR component = 'cnpa/event-v4/publish/index');

SET @v4PublishMenuId = 2280;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2281, 'V4 Series查询', @v4PublishMenuId, 1, '', '', 'F', '0', '0', 'basket:v4Series:list', '#', 'admin', sysdate()),
(2282, 'V4 Series详情', @v4PublishMenuId, 2, '', '', 'F', '0', '0', 'basket:v4Series:query', '#', 'admin', sysdate()),
(2283, 'V4 Series启停', @v4PublishMenuId, 3, '', '', 'F', '0', '0', 'basket:v4Series:edit', '#', 'admin', sysdate()),
(2284, 'V4 Season查询', @v4PublishMenuId, 4, '', '', 'F', '0', '0', 'basket:v4Season:query', '#', 'admin', sysdate()),
(2285, 'V4 Season新增', @v4PublishMenuId, 5, '', '', 'F', '0', '0', 'basket:v4Season:add', '#', 'admin', sysdate()),
(2286, 'V4 Season编辑', @v4PublishMenuId, 6, '', '', 'F', '0', '0', 'basket:v4Season:edit', '#', 'admin', sysdate()),
(2287, 'V4 Season发布', @v4PublishMenuId, 7, '', '', 'F', '0', '0', 'basket:v4Season:publish', '#', 'admin', sysdate()),
(2288, 'V4 Season下线', @v4PublishMenuId, 8, '', '', 'F', '0', '0', 'basket:v4Season:offline', '#', 'admin', sysdate()),
(2289, 'V4 Season删除', @v4PublishMenuId, 9, '', '', 'F', '0', '0', 'basket:v4Season:remove', '#', 'admin', sysdate()),
(2290, 'V4 Division查询', @v4PublishMenuId, 10, '', '', 'F', '0', '0', 'basket:v4Division:query', '#', 'admin', sysdate()),
(2291, 'V4 Division新增', @v4PublishMenuId, 11, '', '', 'F', '0', '0', 'basket:v4Division:add', '#', 'admin', sysdate()),
(2292, 'V4 Division编辑', @v4PublishMenuId, 12, '', '', 'F', '0', '0', 'basket:v4Division:edit', '#', 'admin', sysdate()),
(2293, 'V4 Division开放报名', @v4PublishMenuId, 13, '', '', 'F', '0', '0', 'basket:v4Division:openRegistration', '#', 'admin', sysdate()),
(2294, 'V4 Division关闭报名', @v4PublishMenuId, 14, '', '', 'F', '0', '0', 'basket:v4Division:closeRegistration', '#', 'admin', sysdate()),
(2295, 'V4 Division删除', @v4PublishMenuId, 15, '', '', 'F', '0', '0', 'basket:v4Division:remove', '#', 'admin', sysdate())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 隐藏旧赛事发布入口（保留页面文件，后续轮次删除）
UPDATE sys_menu SET visible = '1', remark = CONCAT(IFNULL(remark, ''), ' [V4 R2 deprecated]')
WHERE menu_id IN (2004, 2005, 2006, 2038, 2039)
   OR component IN ('cnpa/event/list/index', 'cnpa/event/season/index', 'cnpa/event/rule/index', 'cnpa/event/stage/index', 'cnpa/event/group/index');

-- admin 角色授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id BETWEEN 2280 AND 2295
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
