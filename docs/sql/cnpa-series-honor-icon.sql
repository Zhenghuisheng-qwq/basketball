-- 球员荣誉头像：每个赛事系列一张徽章图，并挂到内容运营菜单。
-- 列和菜单都按不存在才写入。父菜单取「名人堂管理」。

SET @honorIconColumn = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'basket_series'
    AND COLUMN_NAME = 'honor_icon_url'
);
SET @ddl = IF(
  @honorIconColumn = 0,
  'ALTER TABLE basket_series ADD COLUMN honor_icon_url VARCHAR(512) NULL COMMENT ''球员荣誉头像URL'' AFTER banner_file_id',
  'SELECT 1'
);
PREPARE honor_icon_stmt FROM @ddl;
EXECUTE honor_icon_stmt;
DEALLOCATE PREPARE honor_icon_stmt;

SET @hallMenuId = (
  SELECT menu_id FROM sys_menu
  WHERE menu_type = 'C' AND component = 'cnpa/content/hall-of-fame/index'
  LIMIT 1
);
SET @pageId = (
  SELECT menu_id FROM sys_menu
  WHERE component = 'cnpa/content/hall-of-fame/HonorIconPanel'
  LIMIT 1
);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '荣誉头像', parent_id, order_num + 1, 'honor-icon',
       'cnpa/content/hall-of-fame/HonorIconPanel', 'BasketHonorIcon',
       1, 0, 'C', '0', '0', 'basket:hallOfFame:list', 'star', 'admin', sysdate(),
       '赛事系列对应的球员荣誉头像'
FROM sys_menu
WHERE menu_id = @hallMenuId
  AND @pageId IS NULL;

SET @pageId = (
  SELECT menu_id FROM sys_menu
  WHERE component = 'cnpa/content/hall-of-fame/HonorIconPanel'
  LIMIT 1
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, @pageId
FROM sys_role_menu rm
WHERE rm.menu_id = @hallMenuId
  AND @pageId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu exists_rm
    WHERE exists_rm.role_id = rm.role_id AND exists_rm.menu_id = @pageId
  );
