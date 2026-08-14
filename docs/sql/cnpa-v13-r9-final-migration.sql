-- CNPA V1.3 R9: official Fresh-DB schema gap fix.
-- Long-term identity / audit tables existed in the development library
-- but were missing from the checked-in init chain. Safe on existing DBs.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `basket_player_platform_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `player_id` bigint NOT NULL,
  `platform` varchar(16) NOT NULL,
  `display_game_id` varchar(128) NOT NULL,
  `normalized_game_id` varchar(128) NOT NULL,
  `account_status` tinyint NOT NULL DEFAULT 1,
  `is_primary` tinyint NOT NULL DEFAULT 0,
  `source_application_id` bigint DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  `approved_time` datetime DEFAULT NULL,
  `effective_time` datetime DEFAULT NULL,
  `invalid_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_normalized_active` (`platform`,`normalized_game_id`,`is_delete`),
  UNIQUE KEY `uk_player_platform_active` (`player_id`,`platform`,`is_delete`),
  KEY `idx_platform_player` (`player_id`,`platform`,`account_status`,`is_delete`),
  KEY `idx_platform_primary` (`player_id`,`is_primary`,`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `basket_player_platform_change_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_no` varchar(64) NOT NULL,
  `player_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `change_type` varchar(32) NOT NULL,
  `platform` varchar(16) NOT NULL,
  `platform_account_id` bigint DEFAULT NULL,
  `old_display_game_id` varchar(128) DEFAULT NULL,
  `new_display_game_id` varchar(128) NOT NULL,
  `new_normalized_game_id` varchar(128) NOT NULL,
  `proof_file_id` bigint DEFAULT NULL,
  `request_status` tinyint NOT NULL DEFAULT 0,
  `submit_time` datetime DEFAULT NULL,
  `reviewed_time` datetime DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `review_remark` varchar(1000) DEFAULT NULL,
  `result_platform_account_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_change_request_no` (`request_no`),
  KEY `idx_platform_change_player` (`player_id`,`request_status`,`is_delete`),
  KEY `idx_platform_change_member` (`member_id`,`request_status`,`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `basket_player_platform_identity_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `player_id` bigint NOT NULL,
  `platform_identity_id` bigint NOT NULL,
  `platform_code` varchar(16) NOT NULL,
  `change_type` varchar(32) NOT NULL,
  `old_game_id` varchar(128) DEFAULT NULL,
  `new_game_id` varchar(128) NOT NULL,
  `effective_time` datetime NOT NULL,
  `audit_record_id` bigint DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `initial_bind_slot` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_identity_initial_bind_slot` (`initial_bind_slot`),
  KEY `idx_platform_identity_history_player` (`player_id`,`platform_code`,`effective_time`,`is_delete`),
  KEY `idx_platform_identity_history_user` (`user_id`,`effective_time`,`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `basket_business_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audit_no` varchar(64) NOT NULL,
  `biz_module` varchar(64) NOT NULL,
  `tournament_id` bigint DEFAULT NULL,
  `season_id` bigint DEFAULT NULL,
  `object_type` varchar(64) NOT NULL,
  `object_id` bigint NOT NULL,
  `biz_no` varchar(128) DEFAULT NULL,
  `action_type` varchar(64) NOT NULL,
  `action_result` varchar(32) NOT NULL DEFAULT 'SUCCESS',
  `change_summary` varchar(2000) DEFAULT NULL,
  `action_reason` varchar(2000) DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `operator_role` varchar(128) DEFAULT NULL,
  `related_apply_id` bigint DEFAULT NULL,
  `related_audit_id` bigint DEFAULT NULL,
  `related_discipline_id` bigint DEFAULT NULL,
  `related_match_id` bigint DEFAULT NULL,
  `object_version` int DEFAULT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_audit_no` (`audit_no`),
  KEY `idx_business_audit_module_time` (`biz_module`,`create_time`),
  KEY `idx_business_audit_object` (`object_type`,`object_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `basket_business_audit_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audit_id` bigint NOT NULL,
  `field_name` varchar(128) NOT NULL,
  `field_label` varchar(128) DEFAULT NULL,
  `before_value` varchar(2000) DEFAULT NULL,
  `after_value` varchar(2000) DEFAULT NULL,
  `data_type` varchar(32) DEFAULT 'STRING',
  `sensitive_flag` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_audit_field_audit` (`audit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- R9: schedule_revision must be present on first generate
ALTER TABLE basket_schedule_batch
  MODIFY COLUMN schedule_revision INT NOT NULL DEFAULT 0;

-- R9: official chain must include v4 round5 match-data columns (not reconcile-only).
-- Fresh DB from cnpa-v4-r1-ddl lacks these; without them match-submission SELECT 500.
ALTER TABLE `basket_match_submission`
  ADD COLUMN `home_score` INT DEFAULT NULL COMMENT '提交总比分-主队' AFTER `version_no`;
ALTER TABLE `basket_match_submission`
  ADD COLUMN `away_score` INT DEFAULT NULL COMMENT '提交总比分-客队' AFTER `home_score`;
ALTER TABLE `basket_match_submission`
  ADD COLUMN `submit_remark` VARCHAR(512) DEFAULT NULL COMMENT '提交备注' AFTER `reject_reason`;
