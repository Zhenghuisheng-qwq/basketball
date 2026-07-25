-- 修复「运营工作台」侧边栏重复显示
-- 原因：router/index.ts 静态 /index 与 sys_menu.menu_id=2201 重复
-- 处理：保留 DB 菜单 2201 作为唯一侧边栏入口；静态 /index 仅用于登录跳转（前端已 hidden:true）

-- 若仍出现两个入口，说明旧库重复插入了菜单，可执行以下清理（慎用，会删除动态菜单入口）：
-- DELETE FROM sys_role_menu WHERE menu_id = 2201;
-- DELETE FROM sys_menu WHERE menu_id = 2201;

-- 验证当前菜单：
-- SELECT menu_id, menu_name, path, component FROM sys_menu WHERE menu_name = '运营工作台';
