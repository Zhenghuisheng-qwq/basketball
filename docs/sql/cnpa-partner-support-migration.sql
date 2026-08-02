-- CNPA 合作伙伴赞助感谢墙：扩展 basket_partner + 新建支持记录表 + 权限
-- MySQL 8.0+

ALTER TABLE basket_partner
    ADD COLUMN partner_code VARCHAR(32) NULL COMMENT '合作伙伴编号' AFTER id,
    ADD COLUMN public_description VARCHAR(1000) NULL COMMENT '公开简介' AFTER website_url,
    ADD COLUMN create_by VARCHAR(64) NULL COMMENT '创建者' AFTER remark,
    ADD COLUMN update_by VARCHAR(64) NULL COMMENT '更新者' AFTER create_by;

UPDATE basket_partner
SET partner_code = CONCAT('CNPA-PT-', LPAD(id, 6, '0'))
WHERE partner_code IS NULL OR partner_code = '';

ALTER TABLE basket_partner
    ADD UNIQUE KEY uk_partner_code (partner_code);

ALTER TABLE basket_partner
    ADD KEY idx_partner_delete_publish_sort (is_delete, partner_status, sort_no);

CREATE TABLE IF NOT EXISTS basket_partner_support_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    partner_id BIGINT NOT NULL COMMENT '合作伙伴ID',
    support_type VARCHAR(32) NOT NULL COMMENT '支持方式',
    support_amount DECIMAL(12, 2) NULL COMMENT '支持金额',
    support_date DATE NOT NULL COMMENT '支持日期',
    public_description VARCHAR(1000) NULL COMMENT '公开说明',
    publish_status TINYINT NOT NULL DEFAULT 1 COMMENT '公开状态 0停用 1公开',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_by VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_partner_support_partner (partner_id),
    KEY idx_partner_support_public_sort (is_delete, publish_status, sort_order, support_date),
    KEY idx_partner_support_partner_public (partner_id, is_delete, publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作伙伴支持记录表';

-- 支持记录管理权限（菜单沿用 2108 合作伙伴管理）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2181, '支持记录查询', 2108, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:partnerSupport:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2181);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2182, '支持记录新增', 2108, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:partnerSupport:add', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2182);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2183, '支持记录修改', 2108, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:partnerSupport:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2183);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2184, '支持记录删除', 2108, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:partnerSupport:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2184);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2185, '支持记录状态', 2108, 5, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:partnerSupport:status', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2185);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2186, '支持记录列表', 2108, 6, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:partnerSupport:list', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2186);

UPDATE sys_menu SET visible = '0', menu_name = '合作伙伴管理' WHERE menu_id = 2108;
