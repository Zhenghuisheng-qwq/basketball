-- 内容运营：邮件通知
-- 管理员新建图文通知，并向状态正常、已填写邮箱的注册账号发送。
-- 幂等：可重复执行。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `basket_mail_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(120) NOT NULL COMMENT '标题，同时作为邮件主题',
  `content_html` MEDIUMTEXT NOT NULL COMMENT '图文正文',
  `send_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1发送中 2已完成',
  `recipient_count` INT NOT NULL DEFAULT 0 COMMENT '计划发送人数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '发送成功人数',
  `fail_count` INT NOT NULL DEFAULT 0 COMMENT '发送失败人数',
  `create_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送完成时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_mail_notice_status` (`send_status`, `is_delete`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件通知';

CREATE TABLE IF NOT EXISTS `basket_mail_notice_recipient` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `member_id` BIGINT NOT NULL COMMENT '会员ID',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '用户名',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '昵称',
  `email` VARCHAR(255) NOT NULL COMMENT '邮箱',
  `send_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待发送 1成功 2失败',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mail_notice_recipient_notice` (`notice_id`, `send_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件通知收件记录';

-- 页面菜单挂在内容运营（parent_id = 2101）下
SET @pageId = (
  SELECT menu_id FROM sys_menu
  WHERE menu_type = 'C' AND perms = 'basket:mailNotice:list'
  LIMIT 1
);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '邮件通知', 2101, 5, 'mail-notice', 'cnpa/content/mail-notice/index', 'BasketMailNotice',
       1, 0, 'C', '0', '0', 'basket:mailNotice:list', 'email', 'admin', sysdate(), '向已注册邮箱发送图文通知'
FROM DUAL
WHERE @pageId IS NULL;

SET @pageId = (
  SELECT menu_id FROM sys_menu
  WHERE menu_type = 'C' AND perms = 'basket:mailNotice:list'
  LIMIT 1
);

SET @queryId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:mailNotice:query' LIMIT 1);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '通知查询', @pageId, 1, '', '', '', 1, 0, 'F', '0', '0', 'basket:mailNotice:query', '#', 'admin', sysdate(), ''
FROM DUAL
WHERE @pageId IS NOT NULL AND @queryId IS NULL;

SET @addId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:mailNotice:add' LIMIT 1);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '通知新增', @pageId, 2, '', '', '', 1, 0, 'F', '0', '0', 'basket:mailNotice:add', '#', 'admin', sysdate(), ''
FROM DUAL
WHERE @pageId IS NOT NULL AND @addId IS NULL;

SET @editId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:mailNotice:edit' LIMIT 1);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '通知修改', @pageId, 3, '', '', '', 1, 0, 'F', '0', '0', 'basket:mailNotice:edit', '#', 'admin', sysdate(), ''
FROM DUAL
WHERE @pageId IS NOT NULL AND @editId IS NULL;

SET @removeId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:mailNotice:remove' LIMIT 1);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '通知删除', @pageId, 4, '', '', '', 1, 0, 'F', '0', '0', 'basket:mailNotice:remove', '#', 'admin', sysdate(), ''
FROM DUAL
WHERE @pageId IS NOT NULL AND @removeId IS NULL;

SET @sendId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:mailNotice:send' LIMIT 1);
SET @nextId = (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @nextId, '通知发送', @pageId, 5, '', '', '', 1, 0, 'F', '0', '0', 'basket:mailNotice:send', '#', 'admin', sysdate(), ''
FROM DUAL
WHERE @pageId IS NOT NULL AND @sendId IS NULL;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN (
  'basket:mailNotice:list',
  'basket:mailNotice:query',
  'basket:mailNotice:add',
  'basket:mailNotice:edit',
  'basket:mailNotice:remove',
  'basket:mailNotice:send'
)
WHERE r.role_id IN (1, 2)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
