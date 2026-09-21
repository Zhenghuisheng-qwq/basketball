-- CNPA 篮球赛事平台官网业务数据库设计
-- 适用数据库 MySQL 8.0
-- 字符集 utf8mb4
-- 存储引擎 InnoDB
-- 设计说明
-- 1 所有业务表均包含 id create_time update_time is_delete
-- 2 不使用 JSON 字段 复杂结构通过子表拆分
-- 3 为降低系统耦合和便于软删除迁移 本脚本不创建物理外键
-- 4 所有关联字段均建立必要索引 数据一致性由应用层和事务保证
-- 5 表名统一使用 basket 前缀 避免与若依系统表冲突
-- 6 update_time 由数据库自动维护
-- 7 逻辑删除值 0 表示正常 1 表示删除

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 一 账号与公共资源

DROP TABLE IF EXISTS `basket_file_asset`;
CREATE TABLE `basket_file_asset` (
                                     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `storage_provider` VARCHAR(32) NOT NULL COMMENT '存储服务商',
                                     `bucket_name` VARCHAR(128) DEFAULT NULL COMMENT '存储桶名称',
                                     `object_key` VARCHAR(512) NOT NULL COMMENT '对象存储键',
                                     `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
                                     `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名称',
                                     `file_ext` VARCHAR(32) DEFAULT NULL COMMENT '文件扩展名',
                                     `mime_type` VARCHAR(128) DEFAULT NULL COMMENT '文件媒体类型',
                                     `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
                                     `file_hash` VARCHAR(128) DEFAULT NULL COMMENT '文件哈希',
                                     `access_url` VARCHAR(1000) NOT NULL COMMENT '访问地址',
                                     `usage_type` VARCHAR(64) NOT NULL COMMENT '业务用途',
                                     `owner_member_id` BIGINT DEFAULT NULL COMMENT '所属会员主键',
                                     `file_status` TINYINT NOT NULL DEFAULT 1 COMMENT '文件状态',
                                     `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_file_object_key (object_key),
                                     KEY idx_file_owner_usage (owner_member_id, usage_type, is_delete),
                                     KEY idx_file_hash (file_hash),
                                     KEY idx_file_status_time (file_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件资源表';

DROP TABLE IF EXISTS `basket_member_account`;
CREATE TABLE `basket_member_account` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `account_no` VARCHAR(32) NOT NULL COMMENT '账号编号',
                                         `username` VARCHAR(64) DEFAULT NULL COMMENT '登录名称',
                                         `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '密码摘要',
                                         `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱地址',
                                         `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱验证状态',
                                         `account_status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态',
                                         `register_source` VARCHAR(32) NOT NULL DEFAULT 'WEB' COMMENT '注册来源',
                                         `register_terminal` VARCHAR(16) DEFAULT NULL COMMENT '注册比赛终端',
                                         `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
                                         `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录地址',
                                         `password_update_time` DATETIME DEFAULT NULL COMMENT '密码更新时间',
                                         `lock_expire_time` DATETIME DEFAULT NULL COMMENT '锁定到期时间',
                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_member_account_no (account_no),
                                         UNIQUE KEY uk_member_email (email),
                                         UNIQUE KEY uk_member_username (username),
                                         KEY idx_member_status_time (account_status, create_time),
                                         KEY idx_member_last_login (last_login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员账号表';

DROP TABLE IF EXISTS `basket_member_profile`;
CREATE TABLE `basket_member_profile` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                         `nickname` VARCHAR(64) NOT NULL COMMENT '会员昵称',
                                         `avatar_file_id` BIGINT DEFAULT NULL COMMENT '头像文件主键',
                                         `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别',
                                         `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
                                         `province_code` VARCHAR(32) DEFAULT NULL COMMENT '省份编码',
                                         `province_name` VARCHAR(64) DEFAULT NULL COMMENT '省份名称',
                                         `city_code` VARCHAR(32) DEFAULT NULL COMMENT '城市编码',
                                         `city_name` VARCHAR(64) DEFAULT NULL COMMENT '城市名称',
                                         `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
                                         `profile_status` TINYINT NOT NULL DEFAULT 1 COMMENT '资料状态',
                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_profile_member (member_id),
                                         KEY idx_profile_city (city_code, is_delete),
                                         KEY idx_profile_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员资料表';

DROP TABLE IF EXISTS `basket_member_agreement_record`;
CREATE TABLE `basket_member_agreement_record` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                                  `agreement_type` VARCHAR(32) NOT NULL COMMENT '协议类型',
                                                  `agreement_version` VARCHAR(32) NOT NULL COMMENT '协议版本',
                                                  `agreement_title` VARCHAR(128) NOT NULL COMMENT '协议标题',
                                                  `agree_status` TINYINT NOT NULL DEFAULT 1 COMMENT '同意状态',
                                                  `agree_time` DATETIME NOT NULL COMMENT '同意时间',
                                                  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端地址',
                                                  `terminal_type` VARCHAR(32) DEFAULT NULL COMMENT '终端类型',
                                                  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '客户端标识',
                                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                  PRIMARY KEY (id),
                                                  KEY idx_agreement_member_type (member_id, agreement_type, agree_time),
                                                  KEY idx_agreement_version (agreement_type, agreement_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员协议确认记录表';

DROP TABLE IF EXISTS `basket_verify_code_record`;
CREATE TABLE `basket_verify_code_record` (
                                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `biz_no` VARCHAR(64) NOT NULL COMMENT '业务编号',
                                             `channel_type` VARCHAR(16) NOT NULL COMMENT '发送渠道',
                                             `receiver` VARCHAR(128) NOT NULL COMMENT '接收地址',
                                             `purpose_type` VARCHAR(32) NOT NULL COMMENT '验证码用途',
                                             `code_hash` VARCHAR(255) NOT NULL COMMENT '验证码摘要',
                                             `send_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态',
                                             `verify_status` TINYINT NOT NULL DEFAULT 0 COMMENT '验证状态',
                                             `expire_time` DATETIME NOT NULL COMMENT '过期时间',
                                             `verify_time` DATETIME DEFAULT NULL COMMENT '验证时间',
                                             `send_provider` VARCHAR(64) DEFAULT NULL COMMENT '发送服务商',
                                             `provider_message_id` VARCHAR(128) DEFAULT NULL COMMENT '服务商消息编号',
                                             `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端地址',
                                             `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
                                             `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                             PRIMARY KEY (id),
                                             UNIQUE KEY uk_verify_biz_no (biz_no),
                                             KEY idx_verify_receiver_purpose (receiver, purpose_type, create_time),
                                             KEY idx_verify_expire_status (expire_time, verify_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码记录表';

DROP TABLE IF EXISTS `basket_login_log`;
CREATE TABLE `basket_login_log` (
                                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `member_id` BIGINT DEFAULT NULL COMMENT '会员主键',
                                    `account_no` VARCHAR(32) DEFAULT NULL COMMENT '账号编号',
                                    `login_type` VARCHAR(32) NOT NULL COMMENT '登录方式',
                                    `login_result` TINYINT NOT NULL COMMENT '登录结果',
                                    `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
                                    `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端地址',
                                    `client_region` VARCHAR(128) DEFAULT NULL COMMENT '客户端地区',
                                    `terminal_type` VARCHAR(32) DEFAULT NULL COMMENT '终端类型',
                                    `device_id` VARCHAR(128) DEFAULT NULL COMMENT '设备编号',
                                    `user_agent` VARCHAR(1000) DEFAULT NULL COMMENT '客户端标识',
                                    `login_time` DATETIME NOT NULL COMMENT '登录时间',
                                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                    PRIMARY KEY (id),
                                    KEY idx_login_member_time (member_id, login_time),
                                    KEY idx_login_result_time (login_result, login_time),
                                    KEY idx_login_ip_time (client_ip, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员登录日志表';

DROP TABLE IF EXISTS `basket_account_security_log`;
CREATE TABLE `basket_account_security_log` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                               `event_type` VARCHAR(64) NOT NULL COMMENT '安全事件类型',
                                               `event_title` VARCHAR(128) NOT NULL COMMENT '安全事件标题',
                                               `old_value_masked` VARCHAR(255) DEFAULT NULL COMMENT '变更前脱敏值',
                                               `new_value_masked` VARCHAR(255) DEFAULT NULL COMMENT '变更后脱敏值',
                                               `operator_type` VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '操作人类型',
                                               `operator_id` BIGINT DEFAULT NULL COMMENT '操作人主键',
                                               `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端地址',
                                               `terminal_type` VARCHAR(32) DEFAULT NULL COMMENT '终端类型',
                                               `event_time` DATETIME NOT NULL COMMENT '事件时间',
                                               `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                               PRIMARY KEY (id),
                                               KEY idx_security_member_time (member_id, event_time),
                                               KEY idx_security_event_time (event_type, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账号安全日志表';

DROP TABLE IF EXISTS `basket_privacy_setting`;
CREATE TABLE `basket_privacy_setting` (
                                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                          `profile_visible` TINYINT NOT NULL DEFAULT 1 COMMENT '资料公开状态',
                                          `allow_team_invite` TINYINT NOT NULL DEFAULT 1 COMMENT '允许球队邀请',
                                          `show_team_history` TINYINT NOT NULL DEFAULT 1 COMMENT '展示球队历史',
                                          `show_match_data` TINYINT NOT NULL DEFAULT 1 COMMENT '展示比赛数据',
                                          `show_online_status` TINYINT NOT NULL DEFAULT 0 COMMENT '展示在线状态',
                                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                          PRIMARY KEY (id),
                                          UNIQUE KEY uk_privacy_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员隐私设置表';

DROP TABLE IF EXISTS `basket_notification_setting`;
CREATE TABLE `basket_notification_setting` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                               `system_notice` TINYINT NOT NULL DEFAULT 1 COMMENT '系统通知开关',
                                               `certification_notice` TINYINT NOT NULL DEFAULT 1 COMMENT '认证通知开关',
                                               `tournament_notice` TINYINT NOT NULL DEFAULT 1 COMMENT '赛事通知开关',
                                               `team_notice` TINYINT NOT NULL DEFAULT 1 COMMENT '球队通知开关',
                                               `announcement_notice` TINYINT NOT NULL DEFAULT 1 COMMENT '公告通知开关',
                                               `email_notice` TINYINT NOT NULL DEFAULT 1 COMMENT '邮件通知开关',
                                               `sms_notice` TINYINT NOT NULL DEFAULT 0 COMMENT '短信通知开关',
                                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                               PRIMARY KEY (id),
                                               UNIQUE KEY uk_notify_setting_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员通知设置表';

-- 二 球员身份与认证

DROP TABLE IF EXISTS `basket_player_profile`;
CREATE TABLE `basket_player_profile` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                         `player_no` VARCHAR(32) DEFAULT NULL COMMENT '永久球员编号',
                                         `game_terminal` VARCHAR(16) DEFAULT NULL COMMENT '比赛终端',
                                         `game_account` VARCHAR(128) DEFAULT NULL COMMENT '游戏账号',
                                         `game_nickname` VARCHAR(128) DEFAULT NULL COMMENT '游戏昵称',
                                         `position_code` VARCHAR(16) DEFAULT NULL COMMENT '场上位置',
                                         `dominant_hand` VARCHAR(16) DEFAULT NULL COMMENT '惯用手',
                                         `height_cm` DECIMAL(5,2) DEFAULT NULL COMMENT '身高',
                                         `weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT '体重',
                                         `current_team_id` BIGINT DEFAULT NULL COMMENT '当前球队主键',
                                         `certification_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态',
                                         `player_status` TINYINT NOT NULL DEFAULT 1 COMMENT '球员状态',
                                         `certified_time` DATETIME DEFAULT NULL COMMENT '认证通过时间',
                                         `profile_visible` TINYINT NOT NULL DEFAULT 1 COMMENT '档案公开状态',
                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_player_member (member_id),
                                         UNIQUE KEY uk_player_no (player_no),
                                         KEY idx_player_team_status (current_team_id, player_status, is_delete),
                                         KEY idx_player_cert_status (certification_status, create_time),
                                         KEY idx_player_terminal_position (game_terminal, position_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员档案表';

DROP TABLE IF EXISTS `basket_player_cert_application`;
CREATE TABLE `basket_player_cert_application` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `application_no` VARCHAR(64) NOT NULL COMMENT '申请编号',
                                                  `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                                  `player_id` BIGINT DEFAULT NULL COMMENT '球员主键',
                                                  `application_type` VARCHAR(32) NOT NULL DEFAULT 'INITIAL' COMMENT '申请类型',
                                                  `game_terminal` VARCHAR(16) NOT NULL COMMENT '比赛终端',
                                                  `game_account` VARCHAR(128) NOT NULL COMMENT '游戏账号',
                                                  `game_nickname` VARCHAR(128) DEFAULT NULL COMMENT '游戏昵称',
                                                  `position_code` VARCHAR(16) NOT NULL COMMENT '场上位置',
                                                  `application_status` TINYINT NOT NULL DEFAULT 0 COMMENT '申请状态',
                                                  `submitted_time` DATETIME DEFAULT NULL COMMENT '提交时间',
                                                  `reviewed_time` DATETIME DEFAULT NULL COMMENT '审核完成时间',
                                                  `current_auditor_id` BIGINT DEFAULT NULL COMMENT '当前审核人主键',
                                                  `review_remark` VARCHAR(1000) DEFAULT NULL COMMENT '审核说明',
                                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                  PRIMARY KEY (id),
                                                  UNIQUE KEY uk_cert_application_no (application_no),
                                                  KEY idx_cert_member_status (member_id, application_status, create_time),
                                                  KEY idx_cert_auditor_status (current_auditor_id, application_status),
                                                  KEY idx_cert_submitted_time (submitted_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员认证申请表';

DROP TABLE IF EXISTS `basket_player_cert_material`;
CREATE TABLE `basket_player_cert_material` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `application_id` BIGINT NOT NULL COMMENT '认证申请主键',
                                               `material_type` VARCHAR(32) NOT NULL COMMENT '材料类型',
                                               `file_id` BIGINT NOT NULL COMMENT '文件主键',
                                               `material_name` VARCHAR(128) DEFAULT NULL COMMENT '材料名称',
                                               `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                               `material_status` TINYINT NOT NULL DEFAULT 1 COMMENT '材料状态',
                                               `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                               PRIMARY KEY (id),
                                               KEY idx_cert_material_app (application_id, material_type, is_delete),
                                               KEY idx_cert_material_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员认证材料表';

DROP TABLE IF EXISTS `basket_player_cert_audit`;
CREATE TABLE `basket_player_cert_audit` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `application_id` BIGINT NOT NULL COMMENT '认证申请主键',
                                            `audit_action` VARCHAR(32) NOT NULL COMMENT '审核动作',
                                            `before_status` TINYINT DEFAULT NULL COMMENT '审核前状态',
                                            `after_status` TINYINT NOT NULL COMMENT '审核后状态',
                                            `auditor_id` BIGINT NOT NULL COMMENT '审核人主键',
                                            `auditor_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人名称',
                                            `audit_remark` VARCHAR(1000) DEFAULT NULL COMMENT '审核说明',
                                            `audit_time` DATETIME NOT NULL COMMENT '审核时间',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                            PRIMARY KEY (id),
                                            KEY idx_cert_audit_app_time (application_id, audit_time),
                                            KEY idx_cert_audit_user_time (auditor_id, audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员认证审核记录表';

DROP TABLE IF EXISTS `basket_captain_qualification`;
CREATE TABLE `basket_captain_qualification` (
                                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `qualification_no` VARCHAR(64) NOT NULL COMMENT '授权编号',
                                                `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                                `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                                `season_id` BIGINT DEFAULT NULL COMMENT '赛事赛季主键（队长授权所属赛事）',
                                                `team_id` BIGINT DEFAULT NULL COMMENT '绑定球队主键，未建队前可为空',
                                                `qualification_status` TINYINT NOT NULL DEFAULT 1 COMMENT '授权状态 0已收回 1有效 2已过期',
                                                `is_current` TINYINT NOT NULL DEFAULT 1 COMMENT '当前有效标记',
                                                `payment_confirmed` TINYINT NOT NULL DEFAULT 0 COMMENT '线下缴费确认状态',
                                                `payment_reference` VARCHAR(128) DEFAULT NULL COMMENT '线下缴费参考号',
                                                `grant_reason` VARCHAR(500) DEFAULT NULL COMMENT '授予备注',
                                                `granted_by` BIGINT NOT NULL COMMENT '授予人主键',
                                                `granted_time` DATETIME NOT NULL COMMENT '授予时间',
                                                `valid_from` DATETIME DEFAULT NULL COMMENT '授权生效时间',
                                                `valid_to` DATETIME DEFAULT NULL COMMENT '授权失效时间（随赛事结束）',
                                                `revoked_by` BIGINT DEFAULT NULL COMMENT '收回人主键',
                                                `revoked_time` DATETIME DEFAULT NULL COMMENT '收回时间',
                                                `revoke_reason` VARCHAR(500) DEFAULT NULL COMMENT '收回原因',
                                                `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                                `active_grant_slot` VARCHAR(128) GENERATED ALWAYS AS (
                                                    CASE
                                                        WHEN `qualification_status` = 1 AND `is_delete` = 0 AND `season_id` IS NOT NULL
                                                            THEN CONCAT(`season_id`, ':', `member_id`)
                                                        ELSE NULL
                                                    END
                                                ) STORED COMMENT '有效授权槽位 seasonId:memberId',
                                                `team_active_grant_slot` VARCHAR(128) GENERATED ALWAYS AS (
                                                    CASE
                                                        WHEN `qualification_status` = 1 AND `is_delete` = 0 AND `season_id` IS NOT NULL AND `team_id` IS NOT NULL
                                                            THEN CONCAT(`season_id`, ':', `team_id`)
                                                        ELSE NULL
                                                    END
                                                ) STORED COMMENT '球队有效授权槽位 seasonId:teamId',
                                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                PRIMARY KEY (id),
                                                UNIQUE KEY uk_captain_qualification_no (qualification_no),
                                                UNIQUE KEY uk_captain_grant_active_slot (active_grant_slot),
                                                UNIQUE KEY uk_captain_grant_team_active_slot (team_active_grant_slot),
                                                KEY idx_captain_member_current (member_id, is_current, qualification_status),
                                                KEY idx_captain_player_current (player_id, is_current, qualification_status),
                                                KEY idx_captain_grant_season_member (season_id, member_id, qualification_status, is_delete),
                                                KEY idx_captain_grant_season_status (season_id, qualification_status, is_current, is_delete),
                                                KEY idx_captain_grant_team (team_id, season_id, qualification_status, is_delete),
                                                KEY idx_captain_grant_valid_to (valid_to, qualification_status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队长授权表';

DROP TABLE IF EXISTS `basket_captain_grant_log`;
CREATE TABLE `basket_captain_grant_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `grant_id` BIGINT NOT NULL COMMENT '队长授权主键',
    `member_id` BIGINT NOT NULL COMMENT '用户ID',
    `player_id` BIGINT DEFAULT NULL COMMENT '球员档案ID',
    `team_id` BIGINT DEFAULT NULL COMMENT '球队ID',
    `season_id` BIGINT NOT NULL COMMENT '赛事赛季ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '操作类型',
    `before_status` TINYINT DEFAULT NULL COMMENT '操作前状态',
    `after_status` TINYINT DEFAULT NULL COMMENT '操作后状态',
    `payment_confirmed` TINYINT DEFAULT NULL COMMENT '缴费是否确认',
    `payment_reference` VARCHAR(128) DEFAULT NULL COMMENT '缴费参考号',
    `grant_time` DATETIME DEFAULT NULL COMMENT '授权时间快照',
    `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间快照',
    `revoke_time` DATETIME DEFAULT NULL COMMENT '收回时间快照',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    `operator_type` VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT '操作人类型 ADMIN/SYSTEM',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_log_grant` (`grant_id`),
    KEY `idx_log_user` (`member_id`),
    KEY `idx_log_event` (`season_id`),
    KEY `idx_log_time` (`create_time`),
    KEY `idx_log_action` (`action_type`, `create_time`),
    KEY `idx_grant_user_event` (`member_id`, `season_id`),
    KEY `idx_grant_team_event` (`team_id`, `season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队长授权历史日志表';

-- 三 球队与成员关系

DROP TABLE IF EXISTS `basket_team`;
CREATE TABLE `basket_team` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `team_no` VARCHAR(32) NOT NULL COMMENT '球队编号',
                               `team_name` VARCHAR(128) NOT NULL COMMENT '球队名称',
                               `short_name` VARCHAR(32) NOT NULL COMMENT '球队简称',
                               `english_name` VARCHAR(128) DEFAULT NULL COMMENT '英文名称',
                               `logo_file_id` BIGINT DEFAULT NULL COMMENT '球队标志文件主键',
                               `region_code` VARCHAR(32) DEFAULT NULL COMMENT '地区编码',
                               `region_name` VARCHAR(64) DEFAULT NULL COMMENT '地区名称',
                               `game_terminal` VARCHAR(16) NOT NULL DEFAULT 'GENERAL' COMMENT '已废弃-历史比赛终端，球队本体不绑定平台',
                               `description` VARCHAR(2000) DEFAULT NULL COMMENT '球队介绍',
                               `contact_member_id` BIGINT DEFAULT NULL COMMENT '联系人会员主键',
                               `captain_member_id` BIGINT NOT NULL COMMENT '队长会员主键',
                               `captain_player_id` BIGINT NOT NULL COMMENT '队长球员主键',
                               `division_id` BIGINT DEFAULT NULL COMMENT '所属赛事Division ID，NULL表示Legacy历史球队',
                               `max_member_count` INT NOT NULL DEFAULT 8 COMMENT '最大成员数量',
                               `current_member_count` INT NOT NULL DEFAULT 0 COMMENT '当前成员数量',
                               `team_status` TINYINT NOT NULL DEFAULT 1 COMMENT '球队状态：0DISABLED/1ACTIVE/2DISSOLVED/3ARCHIVED',
                               `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态',
                               `public_status` TINYINT NOT NULL DEFAULT 1 COMMENT '公开状态',
                               `established_date` DATE DEFAULT NULL COMMENT '成立日期',
                               `dissolved_time` DATETIME DEFAULT NULL COMMENT '解散时间',
                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_team_no (team_no),
                               UNIQUE KEY uk_team_division_name_delete (division_id, team_name, is_delete),
                               KEY idx_team_division_id (division_id),
                               KEY idx_team_captain (captain_member_id, team_status),
                               KEY idx_team_terminal_status (game_terminal, team_status, audit_status),
                               KEY idx_team_region (region_code, team_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事球队表（Division级；division_id为空表示Legacy历史球队）';

DROP TABLE IF EXISTS `basket_team_audit_record`;
CREATE TABLE `basket_team_audit_record` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                            `audit_type` VARCHAR(32) NOT NULL COMMENT '审核类型',
                                            `audit_action` VARCHAR(32) NOT NULL COMMENT '审核动作',
                                            `before_status` TINYINT DEFAULT NULL COMMENT '审核前状态',
                                            `after_status` TINYINT NOT NULL COMMENT '审核后状态',
                                            `auditor_id` BIGINT NOT NULL COMMENT '审核人主键',
                                            `auditor_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人名称',
                                            `audit_remark` VARCHAR(1000) DEFAULT NULL COMMENT '审核说明',
                                            `audit_time` DATETIME NOT NULL COMMENT '审核时间',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                            PRIMARY KEY (id),
                                            KEY idx_team_audit_team_time (team_id, audit_time),
                                            KEY idx_team_audit_user_time (auditor_id, audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队审核记录表';

DROP TABLE IF EXISTS `basket_team_member`;
CREATE TABLE `basket_team_member` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `membership_no` VARCHAR(64) NOT NULL COMMENT '成员关系编号',
                                      `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                      `division_id` BIGINT DEFAULT NULL COMMENT '所属赛事Division ID，NULL表示Legacy历史成员关系',
                                      `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                      `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                      `platform_code` VARCHAR(16) DEFAULT NULL COMMENT '平台代码 PC/PS/XBOX',
                                      `platform_identity_id` BIGINT DEFAULT NULL COMMENT '平台游戏身份主键',
                                      `active_platform_slot` VARCHAR(64) DEFAULT NULL COMMENT '活跃平台槽位 memberId:platformCode',
                                      `active_member_slot` VARCHAR(64) DEFAULT NULL COMMENT '活跃成员槽位 memberId',
                                      `role_type` VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色',
                                      `roster_type` VARCHAR(32) NOT NULL DEFAULT 'OFFICIAL' COMMENT '阵容类型',
                                      `jersey_no` VARCHAR(16) DEFAULT NULL COMMENT '球衣号码',
                                      `join_source` VARCHAR(32) NOT NULL COMMENT '加入来源',
                                      `membership_status` TINYINT NOT NULL DEFAULT 1 COMMENT '成员状态',
                                      `is_current` TINYINT NOT NULL DEFAULT 1 COMMENT '当前成员标记',
                                      `join_time` DATETIME NOT NULL COMMENT '加入时间',
                                      `leave_time` DATETIME DEFAULT NULL COMMENT '离队时间',
                                      `leave_reason` VARCHAR(500) DEFAULT NULL COMMENT '离队原因',
                                      `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                      PRIMARY KEY (id),
                                      UNIQUE KEY uk_team_membership_no (membership_no),
                                      KEY idx_team_member_team_current (team_id, is_current, membership_status),
                                      KEY idx_team_member_division_id (division_id),
                                      KEY idx_team_member_player_current (player_id, is_current, membership_status),
                                      KEY idx_team_member_member_current (member_id, is_current, membership_status),
                                      KEY idx_team_member_join_time (join_time),
                                      KEY idx_team_member_platform_active (member_id, platform_code, is_current, membership_status, is_delete),
                                      UNIQUE KEY uk_team_member_active_platform_slot (active_platform_slot),
                                      UNIQUE KEY uk_team_member_active_member_slot (active_member_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队成员表';

DROP TABLE IF EXISTS `basket_team_join_application`;
CREATE TABLE `basket_team_join_application` (
                                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `application_no` VARCHAR(64) NOT NULL COMMENT '申请编号',
                                                `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                                `applicant_member_id` BIGINT NOT NULL COMMENT '申请会员主键',
                                                `applicant_player_id` BIGINT NOT NULL COMMENT '申请球员主键',
                                                `apply_reason` VARCHAR(1000) DEFAULT NULL COMMENT '申请原因',
                                                `application_status` TINYINT NOT NULL DEFAULT 0 COMMENT '申请状态',
                                                `processed_by` BIGINT DEFAULT NULL COMMENT '处理人主键',
                                                `processed_time` DATETIME DEFAULT NULL COMMENT '处理时间',
                                                `process_remark` VARCHAR(1000) DEFAULT NULL COMMENT '处理说明',
                                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                PRIMARY KEY (id),
                                                UNIQUE KEY uk_team_join_application_no (application_no),
                                                KEY idx_join_team_status (team_id, application_status, create_time),
                                                KEY idx_join_player_status (applicant_player_id, application_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队加入申请表';

DROP TABLE IF EXISTS `basket_team_invitation`;
CREATE TABLE `basket_team_invitation` (
                                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `invitation_no` VARCHAR(64) NOT NULL COMMENT '邀请编号',
                                          `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                          `inviter_member_id` BIGINT NOT NULL COMMENT '邀请人会员主键',
                                          `invitee_member_id` BIGINT NOT NULL COMMENT '被邀请会员主键',
                                          `invitee_player_id` BIGINT NOT NULL COMMENT '被邀请球员主键',
                                          `invitation_message` VARCHAR(1000) DEFAULT NULL COMMENT '邀请说明',
                                          `invitation_status` TINYINT NOT NULL DEFAULT 0 COMMENT '邀请状态',
                                          `expire_time` DATETIME NOT NULL COMMENT '过期时间',
                                          `responded_time` DATETIME DEFAULT NULL COMMENT '响应时间',
                                          `response_remark` VARCHAR(500) DEFAULT NULL COMMENT '响应说明',
                                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                          PRIMARY KEY (id),
                                          UNIQUE KEY uk_team_invitation_no (invitation_no),
                                          KEY idx_invite_team_status (team_id, invitation_status, create_time),
                                          KEY idx_invite_member_status (invitee_member_id, invitation_status, create_time),
                                          KEY idx_invite_expire_status (expire_time, invitation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队邀请表';

DROP TABLE IF EXISTS `basket_team_captain_history`;
CREATE TABLE `basket_team_captain_history` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                               `old_captain_member_id` BIGINT DEFAULT NULL COMMENT '原队长会员主键',
                                               `old_captain_player_id` BIGINT DEFAULT NULL COMMENT '原队长球员主键',
                                               `new_captain_member_id` BIGINT NOT NULL COMMENT '新队长会员主键',
                                               `new_captain_player_id` BIGINT NOT NULL COMMENT '新队长球员主键',
                                               `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型',
                                               `change_reason` VARCHAR(1000) DEFAULT NULL COMMENT '变更原因',
                                               `effective_time` DATETIME NOT NULL COMMENT '生效时间',
                                               `operator_id` BIGINT NOT NULL COMMENT '操作人主键',
                                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                               PRIMARY KEY (id),
                                               KEY idx_captain_history_team_time (team_id, effective_time),
                                               KEY idx_captain_history_new_player (new_captain_player_id, effective_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队队长变更记录表';

DROP TABLE IF EXISTS `basket_team_season_profile`;
CREATE TABLE `basket_team_season_profile` (
                                              `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                              `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                              `display_name` VARCHAR(128) NOT NULL COMMENT '赛季展示名称',
                                              `logo_file_id` BIGINT DEFAULT NULL COMMENT '赛季球队标志文件主键',
                                              `captain_player_id` BIGINT DEFAULT NULL COMMENT '赛季队长球员主键',
                                              `registration_status` TINYINT NOT NULL DEFAULT 0 COMMENT '赛季报名状态',
                                              `seed_no` INT DEFAULT NULL COMMENT '种子编号',
                                              `group_id` BIGINT DEFAULT NULL COMMENT '所属分组主键',
                                              `final_rank` INT DEFAULT NULL COMMENT '最终排名',
                                              `season_status` TINYINT NOT NULL DEFAULT 1 COMMENT '赛季状态',
                                              `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                              `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                              PRIMARY KEY (id),
                                              UNIQUE KEY uk_team_season_profile (team_id, season_id),
                                              KEY idx_team_season_group (season_id, group_id, season_status),
                                              KEY idx_team_season_rank (season_id, final_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队赛季档案表';

-- 四 赛事赛季与报名

DROP TABLE IF EXISTS `basket_venue`;
CREATE TABLE `basket_venue` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `venue_no` VARCHAR(32) NOT NULL COMMENT '场地编号',
                                `venue_name` VARCHAR(128) NOT NULL COMMENT '场地名称',
                                `province_code` VARCHAR(32) DEFAULT NULL COMMENT '省份编码',
                                `province_name` VARCHAR(64) DEFAULT NULL COMMENT '省份名称',
                                `city_code` VARCHAR(32) DEFAULT NULL COMMENT '城市编码',
                                `city_name` VARCHAR(64) DEFAULT NULL COMMENT '城市名称',
                                `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
                                `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人',
                                `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
                                `capacity` INT DEFAULT NULL COMMENT '场地容量',
                                `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
                                `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
                                `venue_status` TINYINT NOT NULL DEFAULT 1 COMMENT '场地状态',
                                `remark` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_venue_no (venue_no),
                                KEY idx_venue_city_status (city_code, venue_status),
                                KEY idx_venue_name (venue_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛场地表';

DROP TABLE IF EXISTS `basket_tournament`;
CREATE TABLE `basket_tournament` (
                                     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `tournament_no` VARCHAR(32) NOT NULL COMMENT '赛事编号',
                                     `tournament_name` VARCHAR(128) NOT NULL COMMENT '赛事名称',
                                     `short_name` VARCHAR(64) NOT NULL COMMENT '赛事简称',
                                     `english_name` VARCHAR(128) DEFAULT NULL COMMENT '英文名称',
                                     `logo_file_id` BIGINT DEFAULT NULL COMMENT '赛事标志文件主键',
                                     `cover_file_id` BIGINT DEFAULT NULL COMMENT '赛事封面文件主键',
                                     `tournament_type` VARCHAR(32) NOT NULL COMMENT '赛事类型',
                                     `game_terminal` VARCHAR(16) NOT NULL COMMENT '比赛终端',
                                     `format_code` VARCHAR(16) NOT NULL DEFAULT '5V5' COMMENT '比赛赛制',
                                     `organizer_name` VARCHAR(128) DEFAULT NULL COMMENT '主办方名称',
                                     `description` LONGTEXT NULL COMMENT '赛事介绍',
                                     `tournament_status` TINYINT NOT NULL DEFAULT 1 COMMENT '赛事状态',
                                     `public_status` TINYINT NOT NULL DEFAULT 1 COMMENT '公开状态',
                                     `display_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
                                     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_tournament_no (tournament_no),
                                     UNIQUE KEY uk_tournament_name_delete (tournament_name, is_delete),
                                     KEY idx_tournament_terminal_status (game_terminal, tournament_status, public_status),
                                     KEY idx_tournament_display (public_status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事主表';

DROP TABLE IF EXISTS `basket_tournament_season`;
CREATE TABLE `basket_tournament_season` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `season_no` VARCHAR(32) NOT NULL COMMENT '赛季编号',
                                            `tournament_id` BIGINT NOT NULL COMMENT '赛事主键',
                                            `season_name` VARCHAR(128) NOT NULL COMMENT '赛季名称',
                                            `season_year` INT NOT NULL COMMENT '赛季年份',
                                            `registration_start_time` DATETIME DEFAULT NULL COMMENT '报名开始时间',
                                            `registration_end_time` DATETIME DEFAULT NULL COMMENT '报名结束时间',
                                            `season_start_time` DATETIME DEFAULT NULL COMMENT '赛季开始时间',
                                            `season_end_time` DATETIME DEFAULT NULL COMMENT '赛季结束时间',
                                            `venue_id` BIGINT DEFAULT NULL COMMENT '主场地主键',
                                            `max_team_count` INT NOT NULL DEFAULT 16 COMMENT '最大球队数量',
                                            `min_roster_count` INT NOT NULL DEFAULT 5 COMMENT '最小阵容人数',
                                            `max_roster_count` INT NOT NULL DEFAULT 8 COMMENT '最大阵容人数',
                                            `season_status` TINYINT NOT NULL DEFAULT 0 COMMENT '赛季状态',
                                            `current_stage_id` BIGINT DEFAULT NULL COMMENT '当前阶段主键',
                                            `champion_team_id` BIGINT DEFAULT NULL COMMENT '冠军球队主键',
                                            `runner_up_team_id` BIGINT DEFAULT NULL COMMENT '亚军球队主键',
                                            `public_status` TINYINT NOT NULL DEFAULT 0 COMMENT '公开状态',
                                            `description` LONGTEXT NULL COMMENT '赛季说明',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                            PRIMARY KEY (id),
                                            UNIQUE KEY uk_tournament_season_no (season_no),
                                            UNIQUE KEY uk_tournament_year_name (tournament_id, season_year, season_name, is_delete),
                                            KEY idx_season_status_time (season_status, season_start_time, season_end_time),
                                            KEY idx_season_registration_time (registration_start_time, registration_end_time),
                                            KEY idx_season_public (public_status, season_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事赛季表';

DROP TABLE IF EXISTS `basket_tournament_season_platform`;
CREATE TABLE `basket_tournament_season_platform` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
    `platform_code` VARCHAR(16) NOT NULL COMMENT '平台代码 PC/PS/XBOX',
    `platform_status` TINYINT NOT NULL DEFAULT 1 COMMENT '开放状态 1开放 0关闭',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_season_platform (season_id, platform_code, is_delete),
    KEY idx_season_platform_status (season_id, platform_status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事赛季开放平台表';

DROP TABLE IF EXISTS `basket_tournament_stage`;
CREATE TABLE `basket_tournament_stage` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                           `stage_no` VARCHAR(32) NOT NULL COMMENT '阶段编号',
                                           `stage_name` VARCHAR(128) NOT NULL COMMENT '阶段名称',
                                           `stage_type` VARCHAR(32) NOT NULL COMMENT '阶段类型',
                                           `format_type` VARCHAR(32) NOT NULL COMMENT '阶段赛制',
                                           `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
                                           `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
                                           `sequence_no` INT NOT NULL DEFAULT 0 COMMENT '阶段顺序',
                                           `stage_status` TINYINT NOT NULL DEFAULT 0 COMMENT '阶段状态',
                                           `qualification_rule_text` LONGTEXT NULL COMMENT '晋级规则',
                                           `public_status` TINYINT NOT NULL DEFAULT 1 COMMENT '公开状态',
                                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_stage_season_no (season_id, stage_no),
                                           KEY idx_stage_season_order (season_id, sequence_no, stage_status),
                                           KEY idx_stage_time_status (start_time, end_time, stage_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事阶段表';

DROP TABLE IF EXISTS `basket_tournament_round`;
CREATE TABLE `basket_tournament_round` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                           `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                           `round_no` VARCHAR(32) NOT NULL COMMENT '轮次编号',
                                           `round_name` VARCHAR(128) NOT NULL COMMENT '轮次名称',
                                           `round_type` VARCHAR(32) NOT NULL COMMENT '轮次类型',
                                           `sequence_no` INT NOT NULL DEFAULT 0 COMMENT '轮次顺序',
                                           `best_of` INT NOT NULL DEFAULT 1 COMMENT '比赛局数',
                                           `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
                                           `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
                                           `round_status` TINYINT NOT NULL DEFAULT 0 COMMENT '轮次状态',
                                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_round_stage_no (stage_id, round_no),
                                           KEY idx_round_stage_order (stage_id, sequence_no, round_status),
                                           KEY idx_round_season_time (season_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事轮次表';

DROP TABLE IF EXISTS `basket_tournament_group`;
CREATE TABLE `basket_tournament_group` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                           `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                           `group_code` VARCHAR(32) NOT NULL COMMENT '分组编码',
                                           `group_name` VARCHAR(64) NOT NULL COMMENT '分组名称',
                                           `qualify_count` INT NOT NULL DEFAULT 0 COMMENT '晋级数量',
                                           `sequence_no` INT NOT NULL DEFAULT 0 COMMENT '分组顺序',
                                           `group_status` TINYINT NOT NULL DEFAULT 1 COMMENT '分组状态',
                                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_group_stage_code (stage_id, group_code),
                                           KEY idx_group_season_stage (season_id, stage_id, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事分组表';

DROP TABLE IF EXISTS `basket_tournament_group_team`;
CREATE TABLE `basket_tournament_group_team` (
                                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                                `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                                `group_id` BIGINT NOT NULL COMMENT '赛事分组主键',
                                                `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                                `seed_no` INT DEFAULT NULL COMMENT '种子编号',
                                                `draw_order` INT DEFAULT NULL COMMENT '抽签顺序',
                                                `group_team_status` TINYINT NOT NULL DEFAULT 1 COMMENT '分组球队状态',
                                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                PRIMARY KEY (id),
                                                UNIQUE KEY uk_group_team (group_id, team_id),
                                                KEY idx_group_team_season (season_id, stage_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事分组球队表';

DROP TABLE IF EXISTS `basket_tournament_rule`;
CREATE TABLE `basket_tournament_rule` (
                                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                          `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型',
                                          `rule_title` VARCHAR(256) NOT NULL COMMENT '规则标题',
                                          `rule_content` LONGTEXT NOT NULL COMMENT '规则内容',
                                          `version_no` VARCHAR(32) NOT NULL COMMENT '规则版本',
                                          `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
                                          `rule_status` TINYINT NOT NULL DEFAULT 1 COMMENT '规则状态',
                                          `display_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
                                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                          PRIMARY KEY (id),
                                          UNIQUE KEY uk_rule_season_type_version (season_id, rule_type, version_no),
                                          KEY idx_rule_season_display (season_id, rule_status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事规则表';

DROP TABLE IF EXISTS `basket_tournament_award_config`;
CREATE TABLE `basket_tournament_award_config` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                                  `award_code` VARCHAR(32) NOT NULL COMMENT '奖项编码',
                                                  `award_name` VARCHAR(128) NOT NULL COMMENT '奖项名称',
                                                  `recipient_type` VARCHAR(16) NOT NULL COMMENT '获奖对象类型',
                                                  `award_level` VARCHAR(32) DEFAULT NULL COMMENT '奖项级别',
                                                  `selection_rule` LONGTEXT NULL COMMENT '评选规则',
                                                  `prize_description` VARCHAR(1000) DEFAULT NULL COMMENT '奖励说明',
                                                  `award_status` TINYINT NOT NULL DEFAULT 1 COMMENT '奖项状态',
                                                  `display_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
                                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                  PRIMARY KEY (id),
                                                  UNIQUE KEY uk_award_season_code (season_id, award_code),
                                                  KEY idx_award_season_display (season_id, award_status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事奖项配置表';

DROP TABLE IF EXISTS `basket_tournament_registration`;
CREATE TABLE `basket_tournament_registration` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `registration_no` VARCHAR(64) NOT NULL COMMENT '报名编号',
                                                  `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                                  `team_id` BIGINT NOT NULL COMMENT '报名球队主键',
                                                  `platform_code` VARCHAR(16) DEFAULT NULL COMMENT '报名平台 PC/PS/XBOX',
                                                  `captain_member_id` BIGINT NOT NULL COMMENT '报名队长会员主键',
                                                  `captain_player_id` BIGINT NOT NULL COMMENT '报名队长球员主键',
                                                  `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人',
                                                  `contact_mobile` VARCHAR(32) NOT NULL COMMENT '联系电话',
                                                  `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
                                                  `registration_status` TINYINT NOT NULL DEFAULT 0 COMMENT '报名状态',
                                                  `lineup_count` INT NOT NULL DEFAULT 0 COMMENT '报名阵容人数',
                                                  `submitted_time` DATETIME DEFAULT NULL COMMENT '提交时间',
                                                  `audit_time` DATETIME DEFAULT NULL COMMENT '审核完成时间',
                                                  `withdrawal_time` DATETIME DEFAULT NULL COMMENT '撤回时间',
                                                  `withdrawal_reason` VARCHAR(1000) DEFAULT NULL COMMENT '撤回原因',
                                                  `current_auditor_id` BIGINT DEFAULT NULL COMMENT '当前审核人主键',
                                                  `remark` VARCHAR(1000) DEFAULT NULL COMMENT '报名备注',
                                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                  PRIMARY KEY (id),
                                                  UNIQUE KEY uk_registration_no (registration_no),
                                                  UNIQUE KEY uk_registration_season_team_platform (season_id, team_id, platform_code, is_delete),
                                                  KEY idx_registration_status_time (season_id, registration_status, submitted_time),
                                                  KEY idx_registration_captain (captain_member_id, create_time),
                                                  KEY idx_registration_auditor (current_auditor_id, registration_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事报名表';

DROP TABLE IF EXISTS `basket_tournament_registration_player`;
CREATE TABLE `basket_tournament_registration_player` (
                                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                         `registration_id` BIGINT NOT NULL COMMENT '赛事报名主键',
                                                         `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                                         `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                                         `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                                         `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                                         `team_member_id` BIGINT DEFAULT NULL COMMENT '球队成员主键',
                                                         `roster_role` VARCHAR(32) NOT NULL COMMENT '报名阵容角色',
                                                         `jersey_no` VARCHAR(16) DEFAULT NULL COMMENT '球衣号码',
                                                         `is_starting` TINYINT NOT NULL DEFAULT 0 COMMENT '首发标记',
                                                         `is_captain` TINYINT NOT NULL DEFAULT 0 COMMENT '场上队长标记',
                                                         `validation_status` TINYINT NOT NULL DEFAULT 0 COMMENT '资格校验状态',
                                                         `reject_reason` VARCHAR(1000) DEFAULT NULL COMMENT '不通过原因',
                                                         `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                         `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                         PRIMARY KEY (id),
                                                         UNIQUE KEY uk_registration_player (registration_id, player_id, is_delete),
                                                         KEY idx_reg_player_season_team (season_id, team_id, validation_status),
                                                         KEY idx_reg_player_player (player_id, season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事报名球员表';

DROP TABLE IF EXISTS `basket_tournament_registration_audit`;
CREATE TABLE `basket_tournament_registration_audit` (
                                                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                        `registration_id` BIGINT NOT NULL COMMENT '赛事报名主键',
                                                        `audit_action` VARCHAR(32) NOT NULL COMMENT '审核动作',
                                                        `before_status` TINYINT DEFAULT NULL COMMENT '审核前状态',
                                                        `after_status` TINYINT NOT NULL COMMENT '审核后状态',
                                                        `auditor_id` BIGINT NOT NULL COMMENT '审核人主键',
                                                        `auditor_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人名称',
                                                        `audit_remark` VARCHAR(1000) DEFAULT NULL COMMENT '审核说明',
                                                        `audit_time` DATETIME NOT NULL COMMENT '审核时间',
                                                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                        `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                        `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                        PRIMARY KEY (id),
                                                        KEY idx_registration_audit_time (registration_id, audit_time),
                                                        KEY idx_registration_auditor_time (auditor_id, audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事报名审核记录表';

-- 五 比赛赛程与数据统计

DROP TABLE IF EXISTS `basket_match`;
CREATE TABLE `basket_match` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `match_no` VARCHAR(64) NOT NULL COMMENT '比赛编号',
                                `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                `round_id` BIGINT DEFAULT NULL COMMENT '赛事轮次主键',
                                `group_id` BIGINT DEFAULT NULL COMMENT '赛事分组主键',
                                `venue_id` BIGINT DEFAULT NULL COMMENT '比赛场地主键',
                                `home_team_id` BIGINT NOT NULL COMMENT '主队主键',
                                `away_team_id` BIGINT NOT NULL COMMENT '客队主键',
                                `scheduled_start_time` DATETIME NOT NULL COMMENT '计划开始时间',
                                `scheduled_end_time` DATETIME DEFAULT NULL COMMENT '计划结束时间',
                                `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
                                `actual_end_time` DATETIME DEFAULT NULL COMMENT '实际结束时间',
                                `match_status` TINYINT NOT NULL DEFAULT 0 COMMENT '比赛状态',
                                `home_score` INT NOT NULL DEFAULT 0 COMMENT '主队得分',
                                `away_score` INT NOT NULL DEFAULT 0 COMMENT '客队得分',
                                `winner_team_id` BIGINT DEFAULT NULL COMMENT '获胜球队主键',
                                `overtime_count` INT NOT NULL DEFAULT 0 COMMENT '加时次数',
                                `walkover_team_id` BIGINT DEFAULT NULL COMMENT '判胜球队主键',
                                `cancel_reason` VARCHAR(1000) DEFAULT NULL COMMENT '取消原因',
                                `data_status` TINYINT NOT NULL DEFAULT 0 COMMENT '数据状态',
                                `public_status` TINYINT NOT NULL DEFAULT 1 COMMENT '公开状态',
                                `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_match_no (match_no),
                                KEY idx_match_season_stage_time (season_id, stage_id, scheduled_start_time),
                                KEY idx_match_status_time (match_status, scheduled_start_time),
                                KEY idx_match_home_time (home_team_id, scheduled_start_time),
                                KEY idx_match_away_time (away_team_id, scheduled_start_time),
                                KEY idx_match_round_group (round_id, group_id, sort_no),
                                KEY idx_match_public_status (public_status, match_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛主表';

DROP TABLE IF EXISTS `basket_match_team`;
CREATE TABLE `basket_match_team` (
                                     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `match_id` BIGINT NOT NULL COMMENT '比赛主键',
                                     `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                     `side_type` VARCHAR(16) NOT NULL COMMENT '主客队类型',
                                     `score` INT NOT NULL DEFAULT 0 COMMENT '比赛得分',
                                     `result_type` VARCHAR(16) DEFAULT NULL COMMENT '比赛结果',
                                     `seed_no` INT DEFAULT NULL COMMENT '种子编号',
                                     `timeout_count` INT NOT NULL DEFAULT 0 COMMENT '暂停次数',
                                     `fouls_total` INT NOT NULL DEFAULT 0 COMMENT '犯规总数',
                                     `is_winner` TINYINT NOT NULL DEFAULT 0 COMMENT '获胜标记',
                                     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_match_team (match_id, team_id, is_delete),
                                     UNIQUE KEY uk_match_side (match_id, side_type, is_delete),
                                     KEY idx_match_team_team (team_id, match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛球队表';

DROP TABLE IF EXISTS `basket_match_period_score`;
CREATE TABLE `basket_match_period_score` (
                                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `match_id` BIGINT NOT NULL COMMENT '比赛主键',
                                             `period_no` INT NOT NULL COMMENT '节次编号',
                                             `period_type` VARCHAR(16) NOT NULL COMMENT '节次类型',
                                             `home_score` INT NOT NULL DEFAULT 0 COMMENT '主队本节得分',
                                             `away_score` INT NOT NULL DEFAULT 0 COMMENT '客队本节得分',
                                             `period_status` TINYINT NOT NULL DEFAULT 0 COMMENT '节次状态',
                                             `start_time` DATETIME DEFAULT NULL COMMENT '本节开始时间',
                                             `end_time` DATETIME DEFAULT NULL COMMENT '本节结束时间',
                                             `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                             PRIMARY KEY (id),
                                             UNIQUE KEY uk_match_period (match_id, period_no, is_delete),
                                             KEY idx_period_match_status (match_id, period_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛分节比分表';

DROP TABLE IF EXISTS `basket_match_roster`;
CREATE TABLE `basket_match_roster` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `match_id` BIGINT NOT NULL COMMENT '比赛主键',
                                       `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                       `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                       `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                       `registration_player_id` BIGINT DEFAULT NULL COMMENT '报名球员主键',
                                       `jersey_no` VARCHAR(16) DEFAULT NULL COMMENT '球衣号码',
                                       `roster_role` VARCHAR(32) NOT NULL COMMENT '出场角色',
                                       `is_starting` TINYINT NOT NULL DEFAULT 0 COMMENT '首发标记',
                                       `position_code` VARCHAR(16) DEFAULT NULL COMMENT '场上位置',
                                       `play_status` TINYINT NOT NULL DEFAULT 1 COMMENT '出场状态',
                                       `captain_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '场上队长标记',
                                       `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                       PRIMARY KEY (id),
                                       UNIQUE KEY uk_match_roster_player (match_id, player_id, is_delete),
                                       KEY idx_roster_team_match (team_id, match_id, play_status),
                                       KEY idx_roster_player_season (player_id, season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛出场名单表';

DROP TABLE IF EXISTS `basket_match_team_stat`;
CREATE TABLE `basket_match_team_stat` (
                                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `match_id` BIGINT NOT NULL COMMENT '比赛主键',
                                          `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                          `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                          `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                          `opponent_team_id` BIGINT NOT NULL COMMENT '对手球队主键',
                                          `points` INT NOT NULL DEFAULT 0 COMMENT '得分',
                                          `rebounds` INT NOT NULL DEFAULT 0 COMMENT '篮板',
                                          `offensive_rebounds` INT NOT NULL DEFAULT 0 COMMENT '进攻篮板',
                                          `defensive_rebounds` INT NOT NULL DEFAULT 0 COMMENT '防守篮板',
                                          `assists` INT NOT NULL DEFAULT 0 COMMENT '助攻',
                                          `steals` INT NOT NULL DEFAULT 0 COMMENT '抢断',
                                          `blocks` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
                                          `turnovers` INT NOT NULL DEFAULT 0 COMMENT '失误',
                                          `fouls` INT NOT NULL DEFAULT 0 COMMENT '犯规',
                                          `field_goals_made` INT NOT NULL DEFAULT 0 COMMENT '投篮命中数',
                                          `field_goals_attempted` INT NOT NULL DEFAULT 0 COMMENT '投篮出手数',
                                          `field_goal_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '投篮命中率',
                                          `three_points_made` INT NOT NULL DEFAULT 0 COMMENT '三分命中数',
                                          `three_points_attempted` INT NOT NULL DEFAULT 0 COMMENT '三分出手数',
                                          `three_point_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '三分命中率',
                                          `free_throws_made` INT NOT NULL DEFAULT 0 COMMENT '罚球命中数',
                                          `free_throws_attempted` INT NOT NULL DEFAULT 0 COMMENT '罚球出手数',
                                          `free_throw_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '罚球命中率',
                                          `possessions` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '进攻回合',
                                          `efficiency` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '效率值',
                                          `data_status` TINYINT NOT NULL DEFAULT 0 COMMENT '数据状态',
                                          `confirmed_by` BIGINT DEFAULT NULL COMMENT '确认人主键',
                                          `confirmed_time` DATETIME DEFAULT NULL COMMENT '确认时间',
                                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                          PRIMARY KEY (id),
                                          UNIQUE KEY uk_match_team_stat (match_id, team_id, is_delete),
                                          KEY idx_team_stat_season_team (season_id, team_id, data_status),
                                          KEY idx_team_stat_stage_team (stage_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛球队数据表';

DROP TABLE IF EXISTS `basket_match_player_stat`;
CREATE TABLE `basket_match_player_stat` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `match_id` BIGINT NOT NULL COMMENT '比赛主键',
                                            `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                            `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                            `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                            `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                            `roster_id` BIGINT DEFAULT NULL COMMENT '出场名单主键',
                                            `started_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '首发标记',
                                            `minutes_seconds` INT NOT NULL DEFAULT 0 COMMENT '出场秒数',
                                            `points` INT NOT NULL DEFAULT 0 COMMENT '得分',
                                            `rebounds` INT NOT NULL DEFAULT 0 COMMENT '篮板',
                                            `offensive_rebounds` INT NOT NULL DEFAULT 0 COMMENT '进攻篮板',
                                            `defensive_rebounds` INT NOT NULL DEFAULT 0 COMMENT '防守篮板',
                                            `assists` INT NOT NULL DEFAULT 0 COMMENT '助攻',
                                            `steals` INT NOT NULL DEFAULT 0 COMMENT '抢断',
                                            `blocks` INT NOT NULL DEFAULT 0 COMMENT '盖帽',
                                            `turnovers` INT NOT NULL DEFAULT 0 COMMENT '失误',
                                            `fouls` INT NOT NULL DEFAULT 0 COMMENT '犯规',
                                            `field_goals_made` INT NOT NULL DEFAULT 0 COMMENT '投篮命中数',
                                            `field_goals_attempted` INT NOT NULL DEFAULT 0 COMMENT '投篮出手数',
                                            `field_goal_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '投篮命中率',
                                            `three_points_made` INT NOT NULL DEFAULT 0 COMMENT '三分命中数',
                                            `three_points_attempted` INT NOT NULL DEFAULT 0 COMMENT '三分出手数',
                                            `three_point_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '三分命中率',
                                            `free_throws_made` INT NOT NULL DEFAULT 0 COMMENT '罚球命中数',
                                            `free_throws_attempted` INT NOT NULL DEFAULT 0 COMMENT '罚球出手数',
                                            `free_throw_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '罚球命中率',
                                            `plus_minus` INT NOT NULL DEFAULT 0 COMMENT '正负值',
                                            `efficiency` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '效率值',
                                            `double_double` TINYINT NOT NULL DEFAULT 0 COMMENT '两双标记',
                                            `triple_double` TINYINT NOT NULL DEFAULT 0 COMMENT '三双标记',
                                            `data_status` TINYINT NOT NULL DEFAULT 0 COMMENT '数据状态',
                                            `confirmed_by` BIGINT DEFAULT NULL COMMENT '确认人主键',
                                            `confirmed_time` DATETIME DEFAULT NULL COMMENT '确认时间',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                            PRIMARY KEY (id),
                                            UNIQUE KEY uk_match_player_stat (match_id, player_id, is_delete),
                                            KEY idx_player_stat_season_player (season_id, player_id, data_status),
                                            KEY idx_player_stat_team_match (team_id, match_id),
                                            KEY idx_player_stat_points (season_id, points),
                                            KEY idx_player_stat_efficiency (season_id, efficiency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛球员数据表';

DROP TABLE IF EXISTS `basket_match_status_log`;
CREATE TABLE `basket_match_status_log` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `match_id` BIGINT NOT NULL COMMENT '比赛主键',
                                           `before_status` TINYINT DEFAULT NULL COMMENT '变更前状态',
                                           `after_status` TINYINT NOT NULL COMMENT '变更后状态',
                                           `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型',
                                           `change_reason` VARCHAR(1000) DEFAULT NULL COMMENT '变更原因',
                                           `operator_id` BIGINT NOT NULL COMMENT '操作人主键',
                                           `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
                                           `change_time` DATETIME NOT NULL COMMENT '变更时间',
                                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                           PRIMARY KEY (id),
                                           KEY idx_match_status_log_time (match_id, change_time),
                                           KEY idx_match_status_operator (operator_id, change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛状态变更记录表';

DROP TABLE IF EXISTS `basket_stage_standing`;
CREATE TABLE `basket_stage_standing` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                         `stage_id` BIGINT NOT NULL COMMENT '赛事阶段主键',
                                         `group_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛事分组主键',
                                         `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                         `rank_no` INT NOT NULL DEFAULT 0 COMMENT '排名',
                                         `matches_played` INT NOT NULL DEFAULT 0 COMMENT '比赛场次',
                                         `wins` INT NOT NULL DEFAULT 0 COMMENT '胜场',
                                         `losses` INT NOT NULL DEFAULT 0 COMMENT '负场',
                                         `points_for` INT NOT NULL DEFAULT 0 COMMENT '总得分',
                                         `points_against` INT NOT NULL DEFAULT 0 COMMENT '总失分',
                                         `point_diff` INT NOT NULL DEFAULT 0 COMMENT '净胜分',
                                         `standing_points` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '积分',
                                         `win_rate` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '胜率',
                                         `streak_type` VARCHAR(16) DEFAULT NULL COMMENT '连胜连败类型',
                                         `streak_count` INT NOT NULL DEFAULT 0 COMMENT '连胜连败场次',
                                         `qualified_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '晋级标记',
                                         `snapshot_time` DATETIME NOT NULL COMMENT '快照时间',
                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_standing_team (season_id, stage_id, group_id, team_id, is_delete),
                                         KEY idx_standing_rank (season_id, stage_id, group_id, rank_no),
                                         KEY idx_standing_snapshot (snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事阶段积分表';

DROP TABLE IF EXISTS `basket_team_season_stat`;
CREATE TABLE `basket_team_season_stat` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                           `stage_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛事阶段主键',
                                           `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                           `games` INT NOT NULL DEFAULT 0 COMMENT '比赛场次',
                                           `wins` INT NOT NULL DEFAULT 0 COMMENT '胜场',
                                           `losses` INT NOT NULL DEFAULT 0 COMMENT '负场',
                                           `points_for` INT NOT NULL DEFAULT 0 COMMENT '总得分',
                                           `points_against` INT NOT NULL DEFAULT 0 COMMENT '总失分',
                                           `avg_points` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均得分',
                                           `avg_points_against` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均失分',
                                           `avg_rebounds` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均篮板',
                                           `avg_assists` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均助攻',
                                           `avg_steals` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均抢断',
                                           `avg_blocks` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均盖帽',
                                           `avg_turnovers` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均失误',
                                           `field_goal_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '投篮命中率',
                                           `three_point_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '三分命中率',
                                           `free_throw_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '罚球命中率',
                                           `win_rate` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '胜率',
                                           `last_calculated_time` DATETIME DEFAULT NULL COMMENT '最后计算时间',
                                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_team_season_stat (season_id, stage_id, team_id, is_delete),
                                           KEY idx_team_season_stat_rank (season_id, stage_id, win_rate, avg_points),
                                           KEY idx_team_stat_team (team_id, season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队赛季汇总数据表';

DROP TABLE IF EXISTS `basket_player_season_stat`;
CREATE TABLE `basket_player_season_stat` (
                                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                             `stage_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛事阶段主键',
                                             `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                             `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                             `games` INT NOT NULL DEFAULT 0 COMMENT '比赛场次',
                                             `starts` INT NOT NULL DEFAULT 0 COMMENT '首发场次',
                                             `minutes_seconds` INT NOT NULL DEFAULT 0 COMMENT '总出场秒数',
                                             `points` INT NOT NULL DEFAULT 0 COMMENT '总得分',
                                             `rebounds` INT NOT NULL DEFAULT 0 COMMENT '总篮板',
                                             `assists` INT NOT NULL DEFAULT 0 COMMENT '总助攻',
                                             `steals` INT NOT NULL DEFAULT 0 COMMENT '总抢断',
                                             `blocks` INT NOT NULL DEFAULT 0 COMMENT '总盖帽',
                                             `turnovers` INT NOT NULL DEFAULT 0 COMMENT '总失误',
                                             `fouls` INT NOT NULL DEFAULT 0 COMMENT '总犯规',
                                             `avg_minutes` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均出场分钟',
                                             `avg_points` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均得分',
                                             `avg_rebounds` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均篮板',
                                             `avg_assists` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均助攻',
                                             `avg_steals` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均抢断',
                                             `avg_blocks` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均盖帽',
                                             `field_goal_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '投篮命中率',
                                             `three_point_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '三分命中率',
                                             `free_throw_pct` DECIMAL(7,4) NOT NULL DEFAULT 0 COMMENT '罚球命中率',
                                             `avg_efficiency` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '场均效率值',
                                             `double_double_count` INT NOT NULL DEFAULT 0 COMMENT '两双次数',
                                             `triple_double_count` INT NOT NULL DEFAULT 0 COMMENT '三双次数',
                                             `last_calculated_time` DATETIME DEFAULT NULL COMMENT '最后计算时间',
                                             `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                             PRIMARY KEY (id),
                                             UNIQUE KEY uk_player_season_stat (season_id, stage_id, player_id, is_delete),
                                             KEY idx_player_season_team (season_id, stage_id, team_id),
                                             KEY idx_player_stat_points_rank (season_id, stage_id, avg_points),
                                             KEY idx_player_stat_assists_rank (season_id, stage_id, avg_assists),
                                             KEY idx_player_stat_rebounds_rank (season_id, stage_id, avg_rebounds),
                                             KEY idx_player_stat_eff_rank (season_id, stage_id, avg_efficiency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员赛季汇总数据表';

DROP TABLE IF EXISTS `basket_team_rank`;
CREATE TABLE `basket_team_rank` (
                                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                    `stage_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛事阶段主键',
                                    `rank_type` VARCHAR(32) NOT NULL COMMENT '排行类型',
                                    `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                    `rank_no` INT NOT NULL COMMENT '当前排名',
                                    `previous_rank` INT DEFAULT NULL COMMENT '上期排名',
                                    `stat_value` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '排行指标值',
                                    `snapshot_date` DATE NOT NULL COMMENT '快照日期',
                                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                    PRIMARY KEY (id),
                                    UNIQUE KEY uk_team_rank_snapshot (season_id, stage_id, rank_type, team_id, snapshot_date, is_delete),
                                    KEY idx_team_rank_order (season_id, stage_id, rank_type, snapshot_date, rank_no),
                                    KEY idx_team_rank_team (team_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球队排行榜快照表';

DROP TABLE IF EXISTS `basket_player_rank`;
CREATE TABLE `basket_player_rank` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `season_id` BIGINT NOT NULL COMMENT '赛事赛季主键',
                                      `stage_id` BIGINT NOT NULL DEFAULT 0 COMMENT '赛事阶段主键',
                                      `rank_type` VARCHAR(32) NOT NULL COMMENT '排行类型',
                                      `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                      `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                      `rank_no` INT NOT NULL COMMENT '当前排名',
                                      `previous_rank` INT DEFAULT NULL COMMENT '上期排名',
                                      `stat_value` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '排行指标值',
                                      `snapshot_date` DATE NOT NULL COMMENT '快照日期',
                                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                      PRIMARY KEY (id),
                                      UNIQUE KEY uk_player_rank_snapshot (season_id, stage_id, rank_type, player_id, snapshot_date, is_delete),
                                      KEY idx_player_rank_order (season_id, stage_id, rank_type, snapshot_date, rank_no),
                                      KEY idx_player_rank_player (player_id, snapshot_date),
                                      KEY idx_player_rank_team (team_id, rank_type, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员排行榜快照表';

DROP TABLE IF EXISTS `basket_stat_change_record`;
CREATE TABLE `basket_stat_change_record` (
                                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
                                             `biz_id` BIGINT NOT NULL COMMENT '业务主键',
                                             `field_name` VARCHAR(128) NOT NULL COMMENT '变更字段',
                                             `before_value` LONGTEXT NULL COMMENT '变更前值',
                                             `after_value` LONGTEXT NULL COMMENT '变更后值',
                                             `change_reason` VARCHAR(1000) NOT NULL COMMENT '变更原因',
                                             `operator_id` BIGINT NOT NULL COMMENT '操作人主键',
                                             `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
                                             `approval_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审批状态',
                                             `approved_by` BIGINT DEFAULT NULL COMMENT '审批人主键',
                                             `approved_time` DATETIME DEFAULT NULL COMMENT '审批时间',
                                             `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                             PRIMARY KEY (id),
                                             KEY idx_stat_change_biz (biz_type, biz_id, create_time),
                                             KEY idx_stat_change_operator (operator_id, create_time),
                                             KEY idx_stat_change_approval (approval_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据变更记录表';

-- 六 荣誉公告消息与官网内容

DROP TABLE IF EXISTS `basket_honor_category`;
CREATE TABLE `basket_honor_category` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `honor_code` VARCHAR(32) NOT NULL COMMENT '荣誉编码',
                                         `honor_name` VARCHAR(128) NOT NULL COMMENT '荣誉名称',
                                         `honor_scope` VARCHAR(32) NOT NULL COMMENT '荣誉范围',
                                         `recipient_type` VARCHAR(16) NOT NULL COMMENT '获奖对象类型',
                                         `description` VARCHAR(1000) DEFAULT NULL COMMENT '荣誉说明',
                                         `display_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
                                         `honor_status` TINYINT NOT NULL DEFAULT 1 COMMENT '荣誉状态',
                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_honor_code (honor_code),
                                         KEY idx_honor_scope_display (honor_scope, honor_status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉分类表';

DROP TABLE IF EXISTS `basket_honor_record`;
CREATE TABLE `basket_honor_record` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `tournament_id` BIGINT DEFAULT NULL COMMENT '赛事主键',
                                       `season_id` BIGINT DEFAULT NULL COMMENT '赛事赛季主键',
                                       `honor_category_id` BIGINT NOT NULL COMMENT '荣誉分类主键',
                                       `team_id` BIGINT DEFAULT NULL COMMENT '获奖球队主键',
                                       `player_id` BIGINT DEFAULT NULL COMMENT '获奖球员主键',
                                       `match_id` BIGINT DEFAULT NULL COMMENT '关联比赛主键',
                                       `honor_title` VARCHAR(256) NOT NULL COMMENT '荣誉标题',
                                       `honor_subtitle` VARCHAR(256) DEFAULT NULL COMMENT '荣誉副标题',
                                       `honor_value` VARCHAR(128) DEFAULT NULL COMMENT '荣誉数值',
                                       `award_date` DATE DEFAULT NULL COMMENT '授予日期',
                                       `cover_file_id` BIGINT DEFAULT NULL COMMENT '荣誉封面文件主键',
                                       `source_type` VARCHAR(32) DEFAULT NULL COMMENT '数据来源类型',
                                       `source_id` BIGINT DEFAULT NULL COMMENT '数据来源主键',
                                       `display_status` TINYINT NOT NULL DEFAULT 1 COMMENT '展示状态',
                                       `display_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
                                       `remark` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                       PRIMARY KEY (id),
                                       KEY idx_honor_season_category (season_id, honor_category_id, display_status),
                                       KEY idx_honor_team (team_id, award_date),
                                       KEY idx_honor_player (player_id, award_date),
                                       KEY idx_honor_display (display_status, display_order, award_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉记录表';

DROP TABLE IF EXISTS `basket_honor_lineup`;
CREATE TABLE `basket_honor_lineup` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `honor_record_id` BIGINT NOT NULL COMMENT '荣誉记录主键',
                                       `team_id` BIGINT NOT NULL COMMENT '球队主键',
                                       `player_id` BIGINT NOT NULL COMMENT '球员主键',
                                       `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                       `role_type` VARCHAR(32) NOT NULL COMMENT '阵容角色',
                                       `jersey_no` VARCHAR(16) DEFAULT NULL COMMENT '球衣号码',
                                       `position_code` VARCHAR(16) DEFAULT NULL COMMENT '场上位置',
                                       `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                       PRIMARY KEY (id),
                                       UNIQUE KEY uk_honor_lineup_player (honor_record_id, player_id, is_delete),
                                       KEY idx_honor_lineup_team (team_id, honor_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='冠军荣誉阵容表';

DROP TABLE IF EXISTS `basket_announcement`;
CREATE TABLE `basket_announcement` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `announcement_no` VARCHAR(64) NOT NULL COMMENT '公告编号',
                                       `category_code` VARCHAR(32) NOT NULL COMMENT '公告分类',
                                       `title` VARCHAR(256) NOT NULL COMMENT '公告标题',
                                       `summary` VARCHAR(1000) DEFAULT NULL COMMENT '公告摘要',
                                       `cover_file_id` BIGINT DEFAULT NULL COMMENT '封面文件主键',
                                       `content_text` LONGTEXT NOT NULL COMMENT '公告正文',
                                       `author_id` BIGINT DEFAULT NULL COMMENT '发布人主键',
                                       `author_name` VARCHAR(64) DEFAULT NULL COMMENT '发布人名称',
                                       `announcement_status` TINYINT NOT NULL DEFAULT 0 COMMENT '公告状态',
                                       `public_scope` VARCHAR(32) NOT NULL DEFAULT 'PUBLIC' COMMENT '公开范围',
                                       `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '置顶标记',
                                       `is_important` TINYINT NOT NULL DEFAULT 0 COMMENT '重要标记',
                                       `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
                                       `offline_time` DATETIME DEFAULT NULL COMMENT '下线时间',
                                       `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                       PRIMARY KEY (id),
                                       UNIQUE KEY uk_announcement_no (announcement_no),
                                       KEY idx_announcement_public (announcement_status, public_scope, publish_time),
                                       KEY idx_announcement_category_time (category_code, publish_time),
                                       KEY idx_announcement_top_time (is_top, is_important, publish_time),
                                       FULLTEXT KEY ft_announcement_title_content (title, content_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

DROP TABLE IF EXISTS `basket_announcement_attachment`;
CREATE TABLE `basket_announcement_attachment` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `announcement_id` BIGINT NOT NULL COMMENT '公告主键',
                                                  `file_id` BIGINT NOT NULL COMMENT '文件主键',
                                                  `attachment_name` VARCHAR(255) NOT NULL COMMENT '附件名称',
                                                  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                                  `download_count` BIGINT NOT NULL DEFAULT 0 COMMENT '下载次数',
                                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                                  PRIMARY KEY (id),
                                                  KEY idx_announcement_attachment (announcement_id, sort_no, is_delete),
                                                  KEY idx_attachment_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告附件表';

DROP TABLE IF EXISTS `basket_message_task`;
CREATE TABLE `basket_message_task` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
                                       `message_type` VARCHAR(32) NOT NULL COMMENT '消息类型',
                                       `title` VARCHAR(256) NOT NULL COMMENT '消息标题',
                                       `content_text` LONGTEXT NOT NULL COMMENT '消息内容',
                                       `biz_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型',
                                       `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务主键',
                                       `send_scope` VARCHAR(32) NOT NULL COMMENT '发送范围',
                                       `send_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态',
                                       `scheduled_time` DATETIME DEFAULT NULL COMMENT '计划发送时间',
                                       `sent_time` DATETIME DEFAULT NULL COMMENT '实际发送时间',
                                       `total_count` INT NOT NULL DEFAULT 0 COMMENT '接收总数',
                                       `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数量',
                                       `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数量',
                                       `creator_id` BIGINT DEFAULT NULL COMMENT '创建人主键',
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                       PRIMARY KEY (id),
                                       UNIQUE KEY uk_message_task_no (task_no),
                                       KEY idx_message_task_status_time (send_status, scheduled_time),
                                       KEY idx_message_task_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息任务表';

DROP TABLE IF EXISTS `basket_message_receiver`;
CREATE TABLE `basket_message_receiver` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `task_id` BIGINT NOT NULL COMMENT '消息任务主键',
                                           `member_id` BIGINT NOT NULL COMMENT '接收会员主键',
                                           `delivery_channel` VARCHAR(16) NOT NULL DEFAULT 'SITE' COMMENT '发送渠道',
                                           `delivery_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态',
                                           `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态',
                                           `sent_time` DATETIME DEFAULT NULL COMMENT '发送时间',
                                           `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
                                           `failure_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
                                           `redirect_type` VARCHAR(32) DEFAULT NULL COMMENT '跳转类型',
                                           `redirect_value` VARCHAR(500) DEFAULT NULL COMMENT '跳转值',
                                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_message_receiver (task_id, member_id, delivery_channel, is_delete),
                                           KEY idx_message_member_read (member_id, read_status, create_time),
                                           KEY idx_message_delivery_status (delivery_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息接收记录表';

DROP TABLE IF EXISTS `basket_favorite`;
CREATE TABLE `basket_favorite` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `member_id` BIGINT NOT NULL COMMENT '会员主键',
                                   `target_type` VARCHAR(32) NOT NULL COMMENT '收藏对象类型',
                                   `target_id` BIGINT NOT NULL COMMENT '收藏对象主键',
                                   `favorite_time` DATETIME NOT NULL COMMENT '收藏时间',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uk_favorite_member_target (member_id, target_type, target_id, is_delete),
                                   KEY idx_favorite_target (target_type, target_id, favorite_time),
                                   KEY idx_favorite_member_time (member_id, favorite_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员收藏表';

DROP TABLE IF EXISTS `basket_discipline_record`;
CREATE TABLE `basket_discipline_record` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `record_no` VARCHAR(64) NOT NULL COMMENT '处罚编号',
                                            `season_id` BIGINT DEFAULT NULL COMMENT '赛事赛季主键',
                                            `match_id` BIGINT DEFAULT NULL COMMENT '比赛主键',
                                            `team_id` BIGINT DEFAULT NULL COMMENT '球队主键',
                                            `player_id` BIGINT DEFAULT NULL COMMENT '球员主键',
                                            `member_id` BIGINT DEFAULT NULL COMMENT '会员主键',
                                            `discipline_type` VARCHAR(32) NOT NULL COMMENT '处罚类型',
                                            `title` VARCHAR(256) NOT NULL COMMENT '处罚标题',
                                            `reason_text` LONGTEXT NOT NULL COMMENT '处罚原因',
                                            `penalty_content` LONGTEXT NOT NULL COMMENT '处罚内容',
                                            `effective_start_time` DATETIME DEFAULT NULL COMMENT '处罚开始时间',
                                            `effective_end_time` DATETIME DEFAULT NULL COMMENT '处罚结束时间',
                                            `discipline_status` TINYINT NOT NULL DEFAULT 1 COMMENT '处罚状态',
                                            `decision_by` BIGINT NOT NULL COMMENT '决定人主键',
                                            `decision_time` DATETIME NOT NULL COMMENT '决定时间',
                                            `announcement_id` BIGINT DEFAULT NULL COMMENT '关联公告主键',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                            PRIMARY KEY (id),
                                            UNIQUE KEY uk_discipline_record_no (record_no),
                                            KEY idx_discipline_player_status (player_id, discipline_status, effective_end_time),
                                            KEY idx_discipline_team_status (team_id, discipline_status, effective_end_time),
                                            KEY idx_discipline_season_time (season_id, decision_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事处罚记录表';

DROP TABLE IF EXISTS `basket_home_banner`;
CREATE TABLE `basket_home_banner` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `banner_name` VARCHAR(128) NOT NULL COMMENT '横幅名称',
                                      `title` VARCHAR(256) DEFAULT NULL COMMENT '横幅标题',
                                      `subtitle` VARCHAR(500) DEFAULT NULL COMMENT '横幅副标题',
                                      `image_file_id` BIGINT NOT NULL COMMENT '图片文件主键',
                                      `position_code` VARCHAR(32) NOT NULL COMMENT '展示位置',
                                      `target_type` VARCHAR(32) DEFAULT NULL COMMENT '跳转类型',
                                      `target_value` VARCHAR(500) DEFAULT NULL COMMENT '跳转值',
                                      `start_time` DATETIME DEFAULT NULL COMMENT '展示开始时间',
                                      `end_time` DATETIME DEFAULT NULL COMMENT '展示结束时间',
                                      `banner_status` TINYINT NOT NULL DEFAULT 1 COMMENT '横幅状态',
                                      `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                      PRIMARY KEY (id),
                                      KEY idx_banner_position_status (position_code, banner_status, sort_no),
                                      KEY idx_banner_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页横幅表';

DROP TABLE IF EXISTS `basket_partner`;
CREATE TABLE `basket_partner` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `partner_name` VARCHAR(128) NOT NULL COMMENT '合作伙伴名称',
                                  `logo_file_id` BIGINT DEFAULT NULL COMMENT '标志文件主键',
                                  `website_url` VARCHAR(1000) DEFAULT NULL COMMENT '网站地址',
                                  `partner_type` VARCHAR(32) DEFAULT NULL COMMENT '合作类型',
                                  `cooperation_start_date` DATE DEFAULT NULL COMMENT '合作开始日期',
                                  `cooperation_end_date` DATE DEFAULT NULL COMMENT '合作结束日期',
                                  `partner_status` TINYINT NOT NULL DEFAULT 1 COMMENT '合作状态',
                                  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                  `remark` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_partner_name_delete (partner_name, is_delete),
                                  KEY idx_partner_status_sort (partner_status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作伙伴表';

DROP TABLE IF EXISTS `basket_site_config`;
CREATE TABLE `basket_site_config` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
                                      `config_name` VARCHAR(128) NOT NULL COMMENT '配置名称',
                                      `config_value` LONGTEXT NULL COMMENT '配置值',
                                      `value_type` VARCHAR(16) NOT NULL DEFAULT 'STRING' COMMENT '值类型',
                                      `group_code` VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' COMMENT '配置分组',
                                      `public_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '公开标记',
                                      `config_status` TINYINT NOT NULL DEFAULT 1 COMMENT '配置状态',
                                      `remark` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
                                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
                                      PRIMARY KEY (id),
                                      UNIQUE KEY uk_site_config_key (config_key),
                                      KEY idx_site_config_group (group_code, config_status, public_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网业务配置表';

DROP TABLE IF EXISTS `basket_site_social_link`;
CREATE TABLE `basket_site_social_link` (
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

SET FOREIGN_KEY_CHECKS = 1;
