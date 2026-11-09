-- CNPA 官网更新 R3：已有页面头图。
-- 可重复执行。不读取、不改写 basket_season.banner_file_id。
-- 通用页 season_id = 0。赛事页使用 basket_season.id。
-- 同一 page_key + season_id 只有一条未删除记录。
-- 部署 basketball-admin 与 basketball-web 的头图接口前先执行。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `basket_page_hero` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `page_key` VARCHAR(32) NOT NULL COMMENT 'EVENT_SEASON/DATA/HALL/ANNOUNCEMENT/PARTNER',
  `season_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛事页为赛季 ID，通用页为 0',
  `mode` VARCHAR(32) NOT NULL DEFAULT 'BACKGROUND_TEXT' COMMENT 'FULL_IMAGE 或 BACKGROUND_TEXT',
  `image_file_id` BIGINT NULL COMMENT '已发布电脑端图片',
  `mobile_image_file_id` BIGINT NULL COMMENT '已发布手机端图片',
  `focus_x` INT NOT NULL DEFAULT 50 COMMENT '水平焦点 0-100',
  `focus_y` INT NOT NULL DEFAULT 50 COMMENT '垂直焦点 0-100',
  `mask_opacity` INT NOT NULL DEFAULT 40 COMMENT '遮罩 0-100',
  `alt_text` VARCHAR(256) NULL COMMENT '替代文字',
  `hero_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 草稿 1 已发布 2 已停用',
  `has_draft` TINYINT NOT NULL DEFAULT 0 COMMENT '1 表示有尚未发布的修改',
  `draft_mode` VARCHAR(32) NULL COMMENT '草稿模式',
  `draft_image_file_id` BIGINT NULL COMMENT '草稿电脑端图片',
  `draft_mobile_image_file_id` BIGINT NULL COMMENT '草稿手机端图片',
  `draft_focus_x` INT NULL COMMENT '草稿水平焦点',
  `draft_focus_y` INT NULL COMMENT '草稿垂直焦点',
  `draft_mask_opacity` INT NULL COMMENT '草稿遮罩',
  `draft_alt_text` VARCHAR(256) NULL COMMENT '草稿替代文字',
  `draft_enabled` TINYINT NULL COMMENT '发布后是否显示，1 显示 0 停用',
  `version_no` INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `active_key` VARCHAR(80) NULL COMMENT '未删除时 page_key#season_id',
  `update_by` VARCHAR(64) NULL COMMENT '最后操作人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_hero_active` (`active_key`),
  KEY `idx_page_hero_lookup` (`page_key`, `season_id`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网页面头图';

SET @contentParentId = 2101;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2370, '页面头图', @contentParentId, 7, 'page-hero', 'cnpa/content/page-hero/index', 'BasketPageHero', 1, 0, 'C', '0', '0', 'basket:pageHero:list', 'picture', 'admin', sysdate(), '数据中心、名人堂、公告、合作伙伴头图'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE menu_id = 2370 OR component = 'cnpa/content/page-hero/index' OR perms = 'basket:pageHero:list'
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2371, '页面头图查询', 2370, 1, '', '', '', 1, 0, 'F', '0', '0', 'basket:pageHero:query', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2371 OR perms = 'basket:pageHero:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2372, '页面头图编辑', 2370, 2, '', '', '', 1, 0, 'F', '0', '0', 'basket:pageHero:edit', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2372 OR perms = 'basket:pageHero:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2373, '页面头图发布', 2370, 3, '', '', '', 1, 0, 'F', '0', '0', 'basket:pageHero:publish', '#', 'admin', sysdate(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2373 OR perms = 'basket:pageHero:publish');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (2370, 2371, 2372, 2373)
WHERE r.role_id IN (1, 2)
  AND EXISTS (SELECT 1 FROM sys_menu x WHERE x.menu_id = m.menu_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);
