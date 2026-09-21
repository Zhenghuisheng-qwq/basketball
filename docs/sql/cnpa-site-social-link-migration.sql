-- CNPA 官网社群与媒体入口配置
-- 固定四平台：KOOK / WECHAT / BILIBILI / DOUYIN
-- 幂等：可重复执行

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `basket_site_social_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` VARCHAR(32) NOT NULL COMMENT '平台编码：KOOK/WECHAT/BILIBILI/DOUYIN',
  `title` VARCHAR(64) NOT NULL COMMENT '标题',
  `subtitle` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '描述',
  `button_text` VARCHAR(32) NOT NULL COMMENT '按钮文案',
  `jump_url` VARCHAR(512) DEFAULT NULL COMMENT '跳转链接，允许为空',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，升序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_social_link_platform` (`platform_code`),
  KEY `idx_site_social_link_status_sort` (`status`, `sort_order`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网社群与媒体入口';

INSERT INTO `basket_site_social_link`
  (`platform_code`, `title`, `subtitle`, `button_text`, `jump_url`, `sort_order`, `status`, `is_delete`)
SELECT 'KOOK', '加入 KOOK', '官方赛事社区', '加入社区 →', NULL, 10, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `basket_site_social_link` WHERE `platform_code` = 'KOOK'
);

INSERT INTO `basket_site_social_link`
  (`platform_code`, `title`, `subtitle`, `button_text`, `jump_url`, `sort_order`, `status`, `is_delete`)
SELECT 'WECHAT', '加入微信群', '球员与观众交流群', '加入群聊 →', NULL, 20, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `basket_site_social_link` WHERE `platform_code` = 'WECHAT'
);

INSERT INTO `basket_site_social_link`
  (`platform_code`, `title`, `subtitle`, `button_text`, `jump_url`, `sort_order`, `status`, `is_delete`)
SELECT 'BILIBILI', '关注 B站', '赛事直播与精彩内容', '访问主页 →', NULL, 30, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `basket_site_social_link` WHERE `platform_code` = 'BILIBILI'
);

INSERT INTO `basket_site_social_link`
  (`platform_code`, `title`, `subtitle`, `button_text`, `jump_url`, `sort_order`, `status`, `is_delete`)
SELECT 'DOUYIN', '关注抖音', '赛事短视频与即时动态', '立即关注 →', NULL, 40, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `basket_site_social_link` WHERE `platform_code` = 'DOUYIN'
);

SET @contentParentId = 2101;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2360, '社群与媒体', @contentParentId, 4, 'social-links', 'cnpa/content/social-links/index', 'BasketSiteSocialLinks', 1, 0, 'C', '0', '0', 'basket:socialLink:list', 'peoples', 'admin', sysdate(), '官网社群及官方媒体入口'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE menu_id = 2360 OR component = 'cnpa/content/social-links/index' OR path = 'social-links'
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2361, '社群媒体查询', 2360, 1, '', '', '', 1, 0, 'F', '0', '0', 'basket:socialLink:query', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2361 OR perms = 'basket:socialLink:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2362, '社群媒体编辑', 2360, 2, '', '', '', 1, 0, 'F', '0', '0', 'basket:socialLink:edit', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2362 OR perms = 'basket:socialLink:edit');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (2360, 2361, 2362)
WHERE r.role_id IN (1, 2)
  AND EXISTS (SELECT 1 FROM sys_menu x WHERE x.menu_id = m.menu_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);
