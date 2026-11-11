-- 首页轮播入口。菜单仍是 2107。
-- 线上曾把该菜单标成隐藏，普通角色只有按钮权限、没有菜单本身。
-- 可重复执行。不改表。

UPDATE sys_menu
SET menu_name = '首页轮播',
    visible = '0',
    remark = '首页顶部轮播，上传后直接上架，列表里上下架'
WHERE menu_id = 2107
  AND component = 'cnpa/content/home-banner/index';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (2107, 2139, 2140, 2141, 2142, 2143, 2144)
WHERE r.role_id IN (1, 2)
  AND r.del_flag = '0'
  AND EXISTS (SELECT 1 FROM sys_menu x WHERE x.menu_id = m.menu_id)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
