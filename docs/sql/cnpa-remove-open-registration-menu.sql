-- CNPA 第一期：移除「Open报名审核」菜单入口
-- 说明：仅禁用/删除后台菜单与角色授权，不删除 Open 报名业务数据与 API

DELETE FROM sys_role_menu WHERE menu_id = 2220;

DELETE FROM sys_menu WHERE menu_id = 2220;

UPDATE sys_menu SET order_num = 2 WHERE menu_id = 2221;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 2222;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 2223;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 2224;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 2225;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 2226;
