-- CNPA 数据中心 V2：后台权限按钮
-- 页面路由由前端动态路由注册，这里只补权限，避免和工作台路由重复。
-- 父菜单 2326 是「统计榜单」。若该 ID 不存在，执行前先改成实际父菜单。
-- 上线时人工执行。本脚本不更新比赛或统计业务数据。

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2461, '数据排行查看', 2326, 1, '#', '', '', 1, 0, 'F', '0', '0', 'basket:v4Leaderboard:view', '#', 'admin', sysdate(), '数据中心排行规则查看'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Leaderboard:view');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2462, '数据排行编辑', 2326, 2, '#', '', '', 1, 0, 'F', '0', '0', 'basket:v4Leaderboard:edit', '#', 'admin', sysdate(), '数据中心排行草稿'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Leaderboard:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2463, '数据排行校验', 2326, 3, '#', '', '', 1, 0, 'F', '0', '0', 'basket:v4Leaderboard:validate', '#', 'admin', sysdate(), '数据中心排行校验'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Leaderboard:validate');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2464, '数据排行发布', 2326, 4, '#', '', '', 1, 0, 'F', '0', '0', 'basket:v4Leaderboard:publish', '#', 'admin', sysdate(), '数据中心排行发布'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Leaderboard:publish');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2465, '数据排行重算', 2326, 5, '#', '', '', 1, 0, 'F', '0', '0', 'basket:v4Leaderboard:recalc', '#', 'admin', sysdate(), '数据中心排行重算'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Leaderboard:recalc');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2466, '数据排行归档', 2326, 6, '#', '', '', 1, 0, 'F', '0', '0', 'basket:v4Leaderboard:archive', '#', 'admin', sysdate(), '数据中心排行归档'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Leaderboard:archive');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN (
  'basket:v4Leaderboard:view',
  'basket:v4Leaderboard:edit',
  'basket:v4Leaderboard:validate',
  'basket:v4Leaderboard:publish',
  'basket:v4Leaderboard:recalc',
  'basket:v4Leaderboard:archive'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);
