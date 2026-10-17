-- 已报名球队的成员变更审核。
-- 不改历史球队认证，不补造上线前的审批记录。
-- 可重复执行。生产库需人工执行，不要连到生产自动跑。
-- 回滚：
--   DELETE FROM sys_role_menu WHERE menu_id IN (2218, 2219, 2220);
--   DELETE FROM sys_menu WHERE menu_id IN (2218, 2219, 2220);
--   DROP TABLE IF EXISTS basket_team_membership_change;

CREATE TABLE IF NOT EXISTS `basket_team_membership_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `change_no` VARCHAR(40) NOT NULL,
  `team_id` BIGINT NOT NULL,
  `division_id` BIGINT DEFAULT NULL,
  `registration_id` BIGINT DEFAULT NULL,
  `change_type` VARCHAR(32) NOT NULL COMMENT 'INVITE_JOIN/APPLY_JOIN/CAPTAIN_REMOVE/VOLUNTARY_LEAVE',
  `subject_player_id` BIGINT NOT NULL,
  `subject_member_id` BIGINT NOT NULL,
  `initiator_member_id` BIGINT NOT NULL,
  `initiator_role` VARCHAR(16) NOT NULL COMMENT 'CAPTAIN/PLAYER/ADMIN',
  `source_biz_type` VARCHAR(32) DEFAULT NULL COMMENT 'INVITATION/JOIN_APPLICATION/TEAM_MEMBER',
  `source_biz_id` BIGINT DEFAULT NULL,
  `before_summary` VARCHAR(512) DEFAULT NULL,
  `after_summary` VARCHAR(512) DEFAULT NULL,
  `change_status` VARCHAR(32) NOT NULL COMMENT 'PENDING_USER/PENDING_CAPTAIN/PENDING_ADMIN/APPROVED/REJECTED/CANCELLED/EFFECTIVE',
  `effect_stage` VARCHAR(32) DEFAULT NULL COMMENT 'OPEN_ROSTER/LOCKED_ROSTER/IN_SEASON/UNCONTROLLED',
  `impact_summary` VARCHAR(512) DEFAULT NULL,
  `audit_user_id` BIGINT DEFAULT NULL,
  `audit_user_name` VARCHAR(64) DEFAULT NULL,
  `audit_remark` VARCHAR(512) DEFAULT NULL,
  `audit_time` DATETIME DEFAULT NULL,
  `effective_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_change_no` (`change_no`),
  KEY `idx_membership_change_team` (`team_id`, `change_status`, `is_delete`),
  KEY `idx_membership_change_player` (`team_id`, `subject_player_id`, `change_status`),
  KEY `idx_membership_change_source` (`source_biz_type`, `source_biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队成员变更审核';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2218, '球队成员变更审核', 2151, 7, 'team-membership-change', 'cnpa/identity/team-membership-change/index', NULL, '', 1, 0, 'C', '0', '0', 'basket:teamMembershipChange:list', 'peoples', 'admin', NOW(), '已报名球队成员变更'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2218);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2219, '球队成员变更查询', 2218, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:teamMembershipChange:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2219);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2220, '球队成员变更审核操作', 2218, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'basket:teamMembershipChange:audit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2220);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2218 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2218);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2219 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2219);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2220 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2220);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, 2218 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 2218);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, 2219 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 2219);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, 2220 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 2220);
