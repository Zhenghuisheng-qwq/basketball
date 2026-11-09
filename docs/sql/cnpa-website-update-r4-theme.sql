-- CNPA 官网更新 R4：站点主题。
-- 可重复执行。不写入示例配色。没有绑定的页面继续用现在的颜色。
-- 复制主题会新建一行，不和原主题共用草稿 JSON。
-- 部署主题接口前先执行。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `basket_site_theme` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `theme_name` VARCHAR(64) NOT NULL COMMENT '主题名称',
  `description` VARCHAR(500) NULL COMMENT '说明',
  `bg_color` VARCHAR(16) NOT NULL DEFAULT '#050505' COMMENT '已发布背景色',
  `bg_image_file_id` BIGINT NULL COMMENT '已发布背景图',
  `mobile_bg_image_file_id` BIGINT NULL COMMENT '已发布手机背景图',
  `bg_display` VARCHAR(16) NOT NULL DEFAULT 'TOP_CENTER' COMMENT 'TOP_CENTER/SCROLL/FIXED',
  `colors_json` VARCHAR(2000) NOT NULL COMMENT '已发布语义色',
  `draft_json` VARCHAR(4000) NULL COMMENT '草稿，含名称、背景和语义色',
  `has_draft` TINYINT NOT NULL DEFAULT 0 COMMENT '1 表示有尚未发布的修改',
  `theme_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 草稿 1 已发布 2 已停用',
  `version_no` INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `update_by` VARCHAR(64) NULL COMMENT '最后操作人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网站点主题';

CREATE TABLE IF NOT EXISTS `basket_site_theme_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scope_type` VARCHAR(16) NOT NULL COMMENT 'HOME/SEASON/BASE',
  `scope_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛季 ID，首页和基础主题为 0',
  `theme_id` BIGINT NOT NULL COMMENT '主题 ID',
  `theme_version` INT NOT NULL DEFAULT 0 COMMENT '绑定时的已发布版本',
  `active_key` VARCHAR(80) NULL COMMENT '未删除时 scope_type#scope_id',
  `update_by` VARCHAR(64) NULL COMMENT '最后操作人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_theme_binding_active` (`active_key`),
  KEY `idx_theme_binding_theme` (`theme_id`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网主题绑定';

SET @contentParentId = 2101;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2374, '站点主题', @contentParentId, 8, 'site-theme', 'cnpa/content/site-theme/index', 'BasketSiteTheme', 1, 0, 'C', '0', '0', 'basket:siteTheme:list', 'color', 'admin', sysdate(), '首页、基础和赛季主题'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE menu_id = 2374 OR component = 'cnpa/content/site-theme/index' OR perms = 'basket:siteTheme:list'
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2375, '站点主题查询', 2374, 1, '', '', '', 1, 0, 'F', '0', '0', 'basket:siteTheme:query', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2375 OR perms = 'basket:siteTheme:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2376, '站点主题编辑', 2374, 2, '', '', '', 1, 0, 'F', '0', '0', 'basket:siteTheme:edit', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2376 OR perms = 'basket:siteTheme:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2377, '站点主题发布', 2374, 3, '', '', '', 1, 0, 'F', '0', '0', 'basket:siteTheme:publish', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2377 OR perms = 'basket:siteTheme:publish');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (2374, 2375, 2376, 2377)
WHERE r.role_id IN (1, 2)
  AND EXISTS (SELECT 1 FROM sys_menu x WHERE x.menu_id = m.menu_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);
