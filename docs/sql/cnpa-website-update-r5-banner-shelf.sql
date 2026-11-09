-- 首页轮播入口改名。菜单仍是原来的 2107，只改显示名称。
-- 可重复执行。不改表，不改权限。

UPDATE sys_menu
SET menu_name = '首页轮播',
    remark = '首页顶部轮播，上传后直接上架，列表里上下架'
WHERE menu_id = 2107
  AND component = 'cnpa/content/home-banner/index';
