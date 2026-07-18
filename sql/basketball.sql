SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `basket_email_code`;
CREATE TABLE `basket_email_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `email` varchar(100) NOT NULL COMMENT '邮箱账号',
  `code` varchar(10) NOT NULL COMMENT '验证码',
  `scene` varchar(30) NOT NULL DEFAULT 'REGISTER' COMMENT '业务场景',
  `send_address` varchar(128) DEFAULT '' COMMENT '发送地址',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `verify_time` datetime DEFAULT NULL COMMENT '验证时间',
  `used_flag` char(1) NOT NULL DEFAULT '0' COMMENT '使用标志',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_email_code_email` (`email`),
  KEY `idx_basket_email_code_scene` (`scene`),
  KEY `idx_basket_email_code_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='邮箱验证码表';

DROP TABLE IF EXISTS `basket_player`;
CREATE TABLE `basket_player` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `system_user_id` bigint NOT NULL COMMENT '系统用户编号',
  `player_no` varchar(32) NOT NULL COMMENT '球员编号',
  `email` varchar(100) NOT NULL COMMENT '邮箱账号',
  `nickname` varchar(50) NOT NULL COMMENT '球员昵称',
  `avatar_url` varchar(255) DEFAULT '' COMMENT '头像地址',
  `terminal_type` char(1) NOT NULL COMMENT '比赛终端',
  `gender` char(1) DEFAULT '2' COMMENT '性别',
  `age` int DEFAULT NULL COMMENT '年龄',
  `city` varchar(80) DEFAULT '' COMMENT '所在城市',
  `game_account` varchar(80) DEFAULT '' COMMENT '游戏账号',
  `game_role_name` varchar(80) DEFAULT '' COMMENT '游戏昵称',
  `main_position` varchar(30) DEFAULT '' COMMENT '常用位置',
  `play_style` varchar(100) DEFAULT '' COMMENT '打法特点',
  `profile_desc` varchar(500) DEFAULT '' COMMENT '个人简介',
  `captain_flag` char(1) NOT NULL DEFAULT '0' COMMENT '队长标志',
  `player_status` char(1) NOT NULL DEFAULT '0' COMMENT '球员状态',
  `register_time` datetime DEFAULT NULL COMMENT '注册时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_player_user` (`system_user_id`),
  UNIQUE KEY `uk_basket_player_no` (`player_no`),
  KEY `idx_basket_player_email` (`email`),
  KEY `idx_basket_player_terminal` (`terminal_type`),
  KEY `idx_basket_player_status` (`player_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球员资料表';

DROP TABLE IF EXISTS `basket_captain_record`;
CREATE TABLE `basket_captain_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `player_id` bigint NOT NULL COMMENT '球员编号',
  `old_captain_flag` char(1) NOT NULL DEFAULT '0' COMMENT '原队长标志',
  `new_captain_flag` char(1) NOT NULL DEFAULT '0' COMMENT '新队长标志',
  `reason` varchar(500) DEFAULT '' COMMENT '处理原因',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作用户编号',
  `operate_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_basket_captain_player` (`player_id`),
  KEY `idx_basket_captain_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='队长授权记录表';

DROP TABLE IF EXISTS `basket_team`;
CREATE TABLE `basket_team` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_no` varchar(32) NOT NULL COMMENT '球队编号',
  `team_name` varchar(80) NOT NULL COMMENT '球队名称',
  `logo_url` varchar(255) DEFAULT '' COMMENT '球队标志',
  `terminal_type` char(1) NOT NULL COMMENT '比赛终端',
  `captain_player_id` bigint NOT NULL COMMENT '队长球员编号',
  `city` varchar(80) DEFAULT '' COMMENT '所在城市',
  `slogan` varchar(200) DEFAULT '' COMMENT '球队口号',
  `team_desc` varchar(500) DEFAULT '' COMMENT '球队介绍',
  `member_count` int NOT NULL DEFAULT 1 COMMENT '成员数量',
  `locked_flag` char(1) NOT NULL DEFAULT '0' COMMENT '锁定标志',
  `team_status` char(1) NOT NULL DEFAULT '0' COMMENT '球队状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_team_no` (`team_no`),
  KEY `idx_basket_team_name` (`team_name`),
  KEY `idx_basket_team_captain` (`captain_player_id`),
  KEY `idx_basket_team_terminal` (`terminal_type`),
  KEY `idx_basket_team_status` (`team_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球队信息表';

DROP TABLE IF EXISTS `basket_team_member`;
CREATE TABLE `basket_team_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `player_id` bigint NOT NULL COMMENT '球员编号',
  `member_role` char(1) NOT NULL DEFAULT '1' COMMENT '成员角色',
  `jersey_no` int DEFAULT NULL COMMENT '球衣号码',
  `position_name` varchar(30) DEFAULT '' COMMENT '场上位置',
  `join_time` datetime DEFAULT NULL COMMENT '加入时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离队时间',
  `member_status` char(1) NOT NULL DEFAULT '0' COMMENT '成员状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_basket_team_member_team` (`team_id`),
  KEY `idx_basket_team_member_player` (`player_id`),
  KEY `idx_basket_team_member_status` (`member_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球队成员表';

DROP TABLE IF EXISTS `basket_join_apply`;
CREATE TABLE `basket_join_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `player_id` bigint NOT NULL COMMENT '球员编号',
  `apply_reason` varchar(500) DEFAULT '' COMMENT '申请原因',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_join_team` (`team_id`),
  KEY `idx_basket_join_player` (`player_id`),
  KEY `idx_basket_join_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入队申请表';

DROP TABLE IF EXISTS `basket_invite_apply`;
CREATE TABLE `basket_invite_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `captain_player_id` bigint NOT NULL COMMENT '队长球员编号',
  `target_player_id` bigint NOT NULL COMMENT '目标球员编号',
  `invite_message` varchar(500) DEFAULT '' COMMENT '邀请说明',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `player_confirm_status` char(1) NOT NULL DEFAULT '0' COMMENT '确认状态',
  `player_confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_invite_team` (`team_id`),
  KEY `idx_basket_invite_target` (`target_player_id`),
  KEY `idx_basket_invite_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='邀请入队申请表';

DROP TABLE IF EXISTS `basket_remove_apply`;
CREATE TABLE `basket_remove_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `captain_player_id` bigint NOT NULL COMMENT '队长球员编号',
  `target_player_id` bigint NOT NULL COMMENT '目标球员编号',
  `remove_reason` varchar(500) DEFAULT '' COMMENT '移除原因',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_remove_team` (`team_id`),
  KEY `idx_basket_remove_target` (`target_player_id`),
  KEY `idx_basket_remove_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='移除成员申请表';

DROP TABLE IF EXISTS `basket_leave_apply`;
CREATE TABLE `basket_leave_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `player_id` bigint NOT NULL COMMENT '球员编号',
  `leave_reason` varchar(500) DEFAULT '' COMMENT '离队原因',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_leave_team` (`team_id`),
  KEY `idx_basket_leave_player` (`player_id`),
  KEY `idx_basket_leave_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='离队申请表';

DROP TABLE IF EXISTS `basket_transfer_apply`;
CREATE TABLE `basket_transfer_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `player_id` bigint NOT NULL COMMENT '球员编号',
  `from_team_id` bigint NOT NULL COMMENT '原球队编号',
  `to_team_id` bigint NOT NULL COMMENT '新球队编号',
  `from_captain_player_id` bigint DEFAULT NULL COMMENT '原队长球员编号',
  `to_captain_player_id` bigint DEFAULT NULL COMMENT '新队长球员编号',
  `transfer_reason` varchar(500) DEFAULT '' COMMENT '转会原因',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `from_confirm_status` char(1) NOT NULL DEFAULT '0' COMMENT '原队确认状态',
  `from_confirm_time` datetime DEFAULT NULL COMMENT '原队确认时间',
  `to_confirm_status` char(1) NOT NULL DEFAULT '0' COMMENT '新队确认状态',
  `to_confirm_time` datetime DEFAULT NULL COMMENT '新队确认时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_transfer_player` (`player_id`),
  KEY `idx_basket_transfer_from_team` (`from_team_id`),
  KEY `idx_basket_transfer_to_team` (`to_team_id`),
  KEY `idx_basket_transfer_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='转会申请表';

DROP TABLE IF EXISTS `basket_captain_apply`;
CREATE TABLE `basket_captain_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `old_captain_player_id` bigint NOT NULL COMMENT '原队长球员编号',
  `new_captain_player_id` bigint NOT NULL COMMENT '新队长球员编号',
  `transfer_reason` varchar(500) DEFAULT '' COMMENT '转让原因',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `new_captain_confirm_status` char(1) NOT NULL DEFAULT '0' COMMENT '新队长确认状态',
  `new_captain_confirm_time` datetime DEFAULT NULL COMMENT '新队长确认时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_captain_apply_team` (`team_id`),
  KEY `idx_basket_captain_apply_new` (`new_captain_player_id`),
  KEY `idx_basket_captain_apply_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='队长转让申请表';

DROP TABLE IF EXISTS `basket_rename_apply`;
CREATE TABLE `basket_rename_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `captain_player_id` bigint NOT NULL COMMENT '队长球员编号',
  `old_team_name` varchar(80) NOT NULL COMMENT '原球队名称',
  `new_team_name` varchar(80) NOT NULL COMMENT '新球队名称',
  `change_reason` varchar(500) DEFAULT '' COMMENT '更名原因',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_rename_team` (`team_id`),
  KEY `idx_basket_rename_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球队更名申请表';

DROP TABLE IF EXISTS `basket_team_log`;
CREATE TABLE `basket_team_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `player_id` bigint DEFAULT NULL COMMENT '球员编号',
  `operate_type` varchar(40) NOT NULL COMMENT '操作类型',
  `operate_content` varchar(1000) DEFAULT '' COMMENT '操作内容',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作用户编号',
  `operate_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_team_log_team` (`team_id`),
  KEY `idx_basket_team_log_player` (`player_id`),
  KEY `idx_basket_team_log_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球队操作记录表';

DROP TABLE IF EXISTS `basket_season`;
CREATE TABLE `basket_season` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `season_no` varchar(32) NOT NULL COMMENT '赛季编号',
  `season_name` varchar(80) NOT NULL COMMENT '赛季名称',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `current_flag` char(1) NOT NULL DEFAULT '0' COMMENT '当前标志',
  `archive_flag` char(1) NOT NULL DEFAULT '0' COMMENT '归档标志',
  `season_status` char(1) NOT NULL DEFAULT '0' COMMENT '赛季状态',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_season_no` (`season_no`),
  KEY `idx_basket_season_current` (`current_flag`),
  KEY `idx_basket_season_status` (`season_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛季信息表';

DROP TABLE IF EXISTS `basket_tournament`;
CREATE TABLE `basket_tournament` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `season_id` bigint NOT NULL COMMENT '赛季编号',
  `tournament_no` varchar(32) NOT NULL COMMENT '赛事编号',
  `tournament_name` varchar(100) NOT NULL COMMENT '赛事名称',
  `tournament_type` char(1) NOT NULL COMMENT '赛事类型',
  `match_format` varchar(20) NOT NULL COMMENT '比赛形式',
  `terminal_type` char(1) NOT NULL COMMENT '比赛终端',
  `cover_url` varchar(255) DEFAULT '' COMMENT '封面地址',
  `intro` varchar(1000) DEFAULT '' COMMENT '赛事简介',
  `sign_start_time` datetime DEFAULT NULL COMMENT '报名开始时间',
  `sign_end_time` datetime DEFAULT NULL COMMENT '报名结束时间',
  `match_start_time` datetime DEFAULT NULL COMMENT '比赛开始时间',
  `match_end_time` datetime DEFAULT NULL COMMENT '比赛结束时间',
  `min_member_count` int NOT NULL DEFAULT 1 COMMENT '最少人数',
  `max_member_count` int NOT NULL DEFAULT 1 COMMENT '最多人数',
  `starter_count` int NOT NULL DEFAULT 1 COMMENT '首发人数',
  `substitute_count` int NOT NULL DEFAULT 0 COMMENT '替补人数',
  `team_limit` int DEFAULT NULL COMMENT '队伍上限',
  `registration_count` int NOT NULL DEFAULT 0 COMMENT '报名数量',
  `publish_flag` char(1) NOT NULL DEFAULT '0' COMMENT '发布标志',
  `tournament_status` char(1) NOT NULL DEFAULT '0' COMMENT '赛事状态',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_tournament_no` (`tournament_no`),
  KEY `idx_basket_tournament_season` (`season_id`),
  KEY `idx_basket_tournament_type` (`tournament_type`),
  KEY `idx_basket_tournament_status` (`tournament_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事信息表';

DROP TABLE IF EXISTS `basket_tournament_rule`;
CREATE TABLE `basket_tournament_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `rule_title` varchar(100) NOT NULL COMMENT '规则标题',
  `rule_content` text COMMENT '规则内容',
  `score_win` int NOT NULL DEFAULT 3 COMMENT '胜场积分',
  `score_draw` int NOT NULL DEFAULT 1 COMMENT '平场积分',
  `score_lose` int NOT NULL DEFAULT 0 COMMENT '负场积分',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_rule_tournament` (`tournament_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事规则表';

DROP TABLE IF EXISTS `basket_registration`;
CREATE TABLE `basket_registration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `captain_player_id` bigint NOT NULL COMMENT '队长球员编号',
  `team_name` varchar(80) NOT NULL COMMENT '报名队名',
  `terminal_type` char(1) NOT NULL COMMENT '比赛终端',
  `member_count` int NOT NULL DEFAULT 0 COMMENT '成员数量',
  `starter_count` int NOT NULL DEFAULT 0 COMMENT '首发数量',
  `substitute_count` int NOT NULL DEFAULT 0 COMMENT '替补数量',
  `apply_time` datetime DEFAULT NULL COMMENT '报名时间',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `locked_flag` char(1) NOT NULL DEFAULT '0' COMMENT '锁定标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_registration_team` (`tournament_id`, `team_id`),
  KEY `idx_basket_registration_tournament` (`tournament_id`),
  KEY `idx_basket_registration_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事报名表';

DROP TABLE IF EXISTS `basket_registration_player`;
CREATE TABLE `basket_registration_player` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `registration_id` bigint NOT NULL COMMENT '报名编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `team_id` bigint NOT NULL COMMENT '球队编号',
  `player_id` bigint NOT NULL COMMENT '球员编号',
  `player_name` varchar(50) NOT NULL COMMENT '球员名称',
  `jersey_no` int DEFAULT NULL COMMENT '球衣号码',
  `position_name` varchar(30) DEFAULT '' COMMENT '场上位置',
  `starter_flag` char(1) NOT NULL DEFAULT '0' COMMENT '首发标志',
  `captain_flag` char(1) NOT NULL DEFAULT '0' COMMENT '队长标志',
  `join_status` char(1) NOT NULL DEFAULT '0' COMMENT '参赛状态',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_reg_player_reg` (`registration_id`),
  KEY `idx_basket_reg_player_player` (`player_id`),
  KEY `idx_basket_reg_player_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报名球员表';

DROP TABLE IF EXISTS `basket_tournament_team`;
CREATE TABLE `basket_tournament_team` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `registration_id` bigint DEFAULT NULL COMMENT '报名编号',
  `source_team_id` bigint NOT NULL COMMENT '来源球队编号',
  `team_no` varchar(32) NOT NULL COMMENT '球队编号',
  `team_name` varchar(80) NOT NULL COMMENT '球队名称',
  `logo_url` varchar(255) DEFAULT '' COMMENT '球队标志',
  `captain_player_id` bigint NOT NULL COMMENT '队长球员编号',
  `terminal_type` char(1) NOT NULL COMMENT '比赛终端',
  `member_count` int NOT NULL DEFAULT 0 COMMENT '成员数量',
  `team_status` char(1) NOT NULL DEFAULT '0' COMMENT '球队状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_tournament_team` (`tournament_id`, `source_team_id`),
  KEY `idx_basket_tournament_team_name` (`team_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事球队快照表';

DROP TABLE IF EXISTS `basket_tournament_player`;
CREATE TABLE `basket_tournament_player` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `tournament_team_id` bigint NOT NULL COMMENT '赛事球队编号',
  `source_player_id` bigint NOT NULL COMMENT '来源球员编号',
  `player_no` varchar(32) NOT NULL COMMENT '球员编号',
  `player_name` varchar(50) NOT NULL COMMENT '球员名称',
  `avatar_url` varchar(255) DEFAULT '' COMMENT '头像地址',
  `game_account` varchar(80) DEFAULT '' COMMENT '游戏账号',
  `game_role_name` varchar(80) DEFAULT '' COMMENT '游戏昵称',
  `jersey_no` int DEFAULT NULL COMMENT '球衣号码',
  `position_name` varchar(30) DEFAULT '' COMMENT '场上位置',
  `starter_flag` char(1) NOT NULL DEFAULT '0' COMMENT '首发标志',
  `captain_flag` char(1) NOT NULL DEFAULT '0' COMMENT '队长标志',
  `player_status` char(1) NOT NULL DEFAULT '0' COMMENT '球员状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_tournament_player_t` (`tournament_id`),
  KEY `idx_basket_tournament_player_team` (`tournament_team_id`),
  KEY `idx_basket_tournament_player_src` (`source_player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事球员快照表';

DROP TABLE IF EXISTS `basket_tournament_audit`;
CREATE TABLE `basket_tournament_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `registration_id` bigint DEFAULT NULL COMMENT '报名编号',
  `audit_type` varchar(40) NOT NULL COMMENT '审核类型',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `audit_content` varchar(1000) DEFAULT '' COMMENT '审核内容',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_tournament_audit_t` (`tournament_id`),
  KEY `idx_basket_tournament_audit_r` (`registration_id`),
  KEY `idx_basket_tournament_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事审核记录表';

DROP TABLE IF EXISTS `basket_group`;
CREATE TABLE `basket_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `group_name` varchar(50) NOT NULL COMMENT '分组名称',
  `group_sort` int NOT NULL DEFAULT 0 COMMENT '分组排序',
  `group_status` char(1) NOT NULL DEFAULT '0' COMMENT '分组状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_group_tournament` (`tournament_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事分组表';

DROP TABLE IF EXISTS `basket_round`;
CREATE TABLE `basket_round` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `round_name` varchar(50) NOT NULL COMMENT '轮次名称',
  `round_type` char(1) NOT NULL DEFAULT '1' COMMENT '轮次类型',
  `round_sort` int NOT NULL DEFAULT 0 COMMENT '轮次排序',
  `round_status` char(1) NOT NULL DEFAULT '0' COMMENT '轮次状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_round_tournament` (`tournament_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事轮次表';

DROP TABLE IF EXISTS `basket_match`;
CREATE TABLE `basket_match` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `group_id` bigint DEFAULT NULL COMMENT '分组编号',
  `round_id` bigint DEFAULT NULL COMMENT '轮次编号',
  `match_no` varchar(32) NOT NULL COMMENT '比赛编号',
  `match_name` varchar(100) DEFAULT '' COMMENT '比赛名称',
  `match_time` datetime DEFAULT NULL COMMENT '比赛时间',
  `room_name` varchar(100) DEFAULT '' COMMENT '房间名称',
  `home_team_id` bigint NOT NULL COMMENT '主队编号',
  `away_team_id` bigint NOT NULL COMMENT '客队编号',
  `home_score` int NOT NULL DEFAULT 0 COMMENT '主队得分',
  `away_score` int NOT NULL DEFAULT 0 COMMENT '客队得分',
  `winner_team_id` bigint DEFAULT NULL COMMENT '胜方球队编号',
  `match_status` char(1) NOT NULL DEFAULT '0' COMMENT '比赛状态',
  `data_status` char(1) NOT NULL DEFAULT '0' COMMENT '数据状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_match_no` (`match_no`),
  KEY `idx_basket_match_tournament` (`tournament_id`),
  KEY `idx_basket_match_group` (`group_id`),
  KEY `idx_basket_match_round` (`round_id`),
  KEY `idx_basket_match_time` (`match_time`),
  KEY `idx_basket_match_status` (`match_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='比赛赛程表';

DROP TABLE IF EXISTS `basket_match_team`;
CREATE TABLE `basket_match_team` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `match_id` bigint NOT NULL COMMENT '比赛编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `tournament_team_id` bigint NOT NULL COMMENT '赛事球队编号',
  `team_side` char(1) NOT NULL COMMENT '球队方向',
  `team_name` varchar(80) NOT NULL COMMENT '球队名称',
  `score` int NOT NULL DEFAULT 0 COMMENT '得分',
  `win_flag` char(1) NOT NULL DEFAULT '0' COMMENT '胜利标志',
  `forfeit_flag` char(1) NOT NULL DEFAULT '0' COMMENT '弃权标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_match_team_match` (`match_id`),
  KEY `idx_basket_match_team_team` (`tournament_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='比赛球队数据表';

DROP TABLE IF EXISTS `basket_match_player_stat`;
CREATE TABLE `basket_match_player_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `match_id` bigint NOT NULL COMMENT '比赛编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `tournament_team_id` bigint NOT NULL COMMENT '赛事球队编号',
  `tournament_player_id` bigint NOT NULL COMMENT '赛事球员编号',
  `player_name` varchar(50) NOT NULL COMMENT '球员名称',
  `starter_flag` char(1) NOT NULL DEFAULT '0' COMMENT '首发标志',
  `play_minutes` int NOT NULL DEFAULT 0 COMMENT '出场分钟',
  `points` int NOT NULL DEFAULT 0 COMMENT '得分',
  `rebounds` int NOT NULL DEFAULT 0 COMMENT '篮板',
  `assists` int NOT NULL DEFAULT 0 COMMENT '助攻',
  `steals` int NOT NULL DEFAULT 0 COMMENT '抢断',
  `blocks` int NOT NULL DEFAULT 0 COMMENT '盖帽',
  `turnovers` int NOT NULL DEFAULT 0 COMMENT '失误',
  `fouls` int NOT NULL DEFAULT 0 COMMENT '犯规',
  `three_points` int NOT NULL DEFAULT 0 COMMENT '三分命中',
  `free_throws` int NOT NULL DEFAULT 0 COMMENT '罚球命中',
  `field_goals` int NOT NULL DEFAULT 0 COMMENT '投篮命中',
  `stat_status` char(1) NOT NULL DEFAULT '0' COMMENT '数据状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_player_stat_match` (`match_id`),
  KEY `idx_basket_player_stat_player` (`tournament_player_id`),
  KEY `idx_basket_player_stat_team` (`tournament_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='比赛球员数据表';

DROP TABLE IF EXISTS `basket_score_audit`;
CREATE TABLE `basket_score_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `match_id` bigint NOT NULL COMMENT '比赛编号',
  `submit_player_id` bigint DEFAULT NULL COMMENT '提交球员编号',
  `submit_user_id` bigint DEFAULT NULL COMMENT '提交用户编号',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `score_desc` varchar(1000) DEFAULT '' COMMENT '数据说明',
  `audit_status` char(1) NOT NULL DEFAULT '0' COMMENT '审核状态',
  `auditor_user_id` bigint DEFAULT NULL COMMENT '审核用户编号',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT '' COMMENT '审核备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_score_audit_match` (`match_id`),
  KEY `idx_basket_score_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='比赛数据审核表';

DROP TABLE IF EXISTS `basket_tournament_standing`;
CREATE TABLE `basket_tournament_standing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tournament_id` bigint NOT NULL COMMENT '赛事编号',
  `group_id` bigint DEFAULT NULL COMMENT '分组编号',
  `tournament_team_id` bigint NOT NULL COMMENT '赛事球队编号',
  `team_name` varchar(80) NOT NULL COMMENT '球队名称',
  `standing_rank` int NOT NULL DEFAULT 0 COMMENT '排名',
  `match_count` int NOT NULL DEFAULT 0 COMMENT '比赛场次',
  `win_count` int NOT NULL DEFAULT 0 COMMENT '胜场',
  `lose_count` int NOT NULL DEFAULT 0 COMMENT '负场',
  `draw_count` int NOT NULL DEFAULT 0 COMMENT '平场',
  `score_for` int NOT NULL DEFAULT 0 COMMENT '总得分',
  `score_against` int NOT NULL DEFAULT 0 COMMENT '总失分',
  `score_diff` int NOT NULL DEFAULT 0 COMMENT '净胜分',
  `points` int NOT NULL DEFAULT 0 COMMENT '积分',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_basket_standing_team` (`tournament_id`, `tournament_team_id`),
  KEY `idx_basket_standing_rank` (`tournament_id`, `standing_rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='赛事积分榜表';

DROP TABLE IF EXISTS `basket_player_summary`;
CREATE TABLE `basket_player_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `season_id` bigint DEFAULT NULL COMMENT '赛季编号',
  `tournament_id` bigint DEFAULT NULL COMMENT '赛事编号',
  `source_player_id` bigint NOT NULL COMMENT '来源球员编号',
  `player_name` varchar(50) NOT NULL COMMENT '球员名称',
  `match_count` int NOT NULL DEFAULT 0 COMMENT '比赛场次',
  `points_total` int NOT NULL DEFAULT 0 COMMENT '总得分',
  `rebounds_total` int NOT NULL DEFAULT 0 COMMENT '总篮板',
  `assists_total` int NOT NULL DEFAULT 0 COMMENT '总助攻',
  `steals_total` int NOT NULL DEFAULT 0 COMMENT '总抢断',
  `blocks_total` int NOT NULL DEFAULT 0 COMMENT '总盖帽',
  `points_avg` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '场均得分',
  `rebounds_avg` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '场均篮板',
  `assists_avg` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '场均助攻',
  `summary_type` char(1) NOT NULL DEFAULT '1' COMMENT '汇总类型',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_player_summary_player` (`source_player_id`),
  KEY `idx_basket_player_summary_tournament` (`tournament_id`),
  KEY `idx_basket_player_summary_season` (`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球员统计汇总表';

DROP TABLE IF EXISTS `basket_team_summary`;
CREATE TABLE `basket_team_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `season_id` bigint DEFAULT NULL COMMENT '赛季编号',
  `tournament_id` bigint DEFAULT NULL COMMENT '赛事编号',
  `source_team_id` bigint NOT NULL COMMENT '来源球队编号',
  `team_name` varchar(80) NOT NULL COMMENT '球队名称',
  `match_count` int NOT NULL DEFAULT 0 COMMENT '比赛场次',
  `win_count` int NOT NULL DEFAULT 0 COMMENT '胜场',
  `lose_count` int NOT NULL DEFAULT 0 COMMENT '负场',
  `draw_count` int NOT NULL DEFAULT 0 COMMENT '平场',
  `score_for` int NOT NULL DEFAULT 0 COMMENT '总得分',
  `score_against` int NOT NULL DEFAULT 0 COMMENT '总失分',
  `score_diff` int NOT NULL DEFAULT 0 COMMENT '净胜分',
  `points` int NOT NULL DEFAULT 0 COMMENT '积分',
  `summary_type` char(1) NOT NULL DEFAULT '1' COMMENT '汇总类型',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_team_summary_team` (`source_team_id`),
  KEY `idx_basket_team_summary_tournament` (`tournament_id`),
  KEY `idx_basket_team_summary_season` (`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='球队统计汇总表';

DROP TABLE IF EXISTS `basket_honor`;
CREATE TABLE `basket_honor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `season_id` bigint DEFAULT NULL COMMENT '赛季编号',
  `tournament_id` bigint DEFAULT NULL COMMENT '赛事编号',
  `honor_type` varchar(40) NOT NULL COMMENT '荣誉类型',
  `honor_name` varchar(100) NOT NULL COMMENT '荣誉名称',
  `team_id` bigint DEFAULT NULL COMMENT '球队编号',
  `player_id` bigint DEFAULT NULL COMMENT '球员编号',
  `team_name` varchar(80) DEFAULT '' COMMENT '球队名称',
  `player_name` varchar(50) DEFAULT '' COMMENT '球员名称',
  `cover_url` varchar(255) DEFAULT '' COMMENT '封面地址',
  `honor_desc` varchar(1000) DEFAULT '' COMMENT '荣誉说明',
  `publish_flag` char(1) NOT NULL DEFAULT '0' COMMENT '发布标志',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_basket_honor_season` (`season_id`),
  KEY `idx_basket_honor_tournament` (`tournament_id`),
  KEY `idx_basket_honor_type` (`honor_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='名人堂荣誉表';

DROP TABLE IF EXISTS `basket_notice`;
CREATE TABLE `basket_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `notice_title` varchar(120) NOT NULL COMMENT '公告标题',
  `notice_type` char(1) NOT NULL DEFAULT '1' COMMENT '公告类型',
  `cover_url` varchar(255) DEFAULT '' COMMENT '封面地址',
  `summary` varchar(300) DEFAULT '' COMMENT '公告摘要',
  `content` longtext COMMENT '公告内容',
  `top_flag` char(1) NOT NULL DEFAULT '0' COMMENT '置顶标志',
  `publish_flag` char(1) NOT NULL DEFAULT '0' COMMENT '发布标志',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览数量',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `notice_status` char(1) NOT NULL DEFAULT '0' COMMENT '公告状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_basket_notice_type` (`notice_type`),
  KEY `idx_basket_notice_publish` (`publish_flag`, `publish_time`),
  KEY `idx_basket_notice_status` (`notice_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告内容表';

DROP TABLE IF EXISTS `basket_site_banner`;
CREATE TABLE `basket_site_banner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `banner_title` varchar(120) NOT NULL COMMENT '轮播标题',
  `image_url` varchar(255) NOT NULL COMMENT '图片地址',
  `link_url` varchar(255) DEFAULT '' COMMENT '跳转地址',
  `link_type` char(1) NOT NULL DEFAULT '0' COMMENT '跳转类型',
  `show_position` varchar(40) NOT NULL DEFAULT 'HOME' COMMENT '展示位置',
  `publish_flag` char(1) NOT NULL DEFAULT '0' COMMENT '发布标志',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_basket_banner_position` (`show_position`),
  KEY `idx_basket_banner_publish` (`publish_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='官网轮播图表';

DROP TABLE IF EXISTS `basket_site_message`;
CREATE TABLE `basket_site_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `receiver_player_id` bigint NOT NULL COMMENT '接收球员编号',
  `sender_user_id` bigint DEFAULT NULL COMMENT '发送用户编号',
  `message_type` varchar(40) NOT NULL COMMENT '消息类型',
  `message_title` varchar(120) NOT NULL COMMENT '消息标题',
  `message_content` varchar(1000) DEFAULT '' COMMENT '消息内容',
  `business_type` varchar(40) DEFAULT '' COMMENT '业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务编号',
  `read_flag` char(1) NOT NULL DEFAULT '0' COMMENT '已读标志',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `message_status` char(1) NOT NULL DEFAULT '0' COMMENT '消息状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_basket_message_receiver` (`receiver_player_id`),
  KEY `idx_basket_message_read` (`read_flag`),
  KEY `idx_basket_message_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息表';

DROP TABLE IF EXISTS `basket_file_resource`;
CREATE TABLE `basket_file_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `file_name` varchar(200) NOT NULL COMMENT '文件名称',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) NOT NULL COMMENT '文件地址',
  `file_type` varchar(40) DEFAULT '' COMMENT '文件类型',
  `file_size` bigint DEFAULT 0 COMMENT '文件大小',
  `business_type` varchar(40) DEFAULT '' COMMENT '业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务编号',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传用户编号',
  `upload_time` datetime DEFAULT NULL COMMENT '上传时间',
  `file_status` char(1) NOT NULL DEFAULT '0' COMMENT '文件状态',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_basket_file_business` (`business_type`, `business_id`),
  KEY `idx_basket_file_status` (`file_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件资源表';

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `basket_web_role_permission`;
DROP TABLE IF EXISTS `basket_web_user_role`;
DROP TABLE IF EXISTS `basket_web_permission`;
DROP TABLE IF EXISTS `basket_web_role`;
DROP TABLE IF EXISTS `basket_web_login_log`;
DROP TABLE IF EXISTS `basket_web_user`;

CREATE TABLE `basket_web_user` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                   `user_no` varchar(32) NOT NULL COMMENT '账号编号',
                                   `email` varchar(100) NOT NULL COMMENT '邮箱账号',
                                   `password` varchar(100) NOT NULL COMMENT '登录密码',
                                   `nickname` varchar(50) NOT NULL COMMENT '用户昵称',
                                   `avatar_url` varchar(255) DEFAULT '' COMMENT '头像地址',
                                   `terminal_type` char(1) NOT NULL COMMENT '比赛终端',
                                   `account_status` char(1) NOT NULL DEFAULT '0' COMMENT '账号状态',
                                   `register_ip` varchar(128) DEFAULT '' COMMENT '注册地址',
                                   `register_time` datetime DEFAULT NULL COMMENT '注册时间',
                                   `last_login_ip` varchar(128) DEFAULT '' COMMENT '最后登录地址',
                                   `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                                   `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_basket_web_user_no` (`user_no`),
                                   UNIQUE KEY `uk_basket_web_user_email` (`email`),
                                   KEY `idx_basket_web_user_status` (`account_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网页账号表';

CREATE TABLE `basket_web_role` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                   `role_code` varchar(50) NOT NULL COMMENT '角色编码',
                                   `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                                   `role_sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
                                   `default_flag` char(1) NOT NULL DEFAULT '0' COMMENT '默认标志',
                                   `role_status` char(1) NOT NULL DEFAULT '0' COMMENT '角色状态',
                                   `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_basket_web_role_code` (`role_code`),
                                   KEY `idx_basket_web_role_status` (`role_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网页角色表';

CREATE TABLE `basket_web_permission` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                         `permission_code` varchar(80) NOT NULL COMMENT '权限编码',
                                         `permission_name` varchar(80) NOT NULL COMMENT '权限名称',
                                         `module_name` varchar(50) NOT NULL COMMENT '模块名称',
                                         `permission_type` char(1) NOT NULL DEFAULT '1' COMMENT '权限类型',
                                         `permission_status` char(1) NOT NULL DEFAULT '0' COMMENT '权限状态',
                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_basket_web_permission_code` (`permission_code`),
                                         KEY `idx_basket_web_permission_module` (`module_name`),
                                         KEY `idx_basket_web_permission_status` (`permission_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网页权限表';

CREATE TABLE `basket_web_user_role` (
                                        `web_user_id` bigint NOT NULL COMMENT '网页账号编号',
                                        `role_id` bigint NOT NULL COMMENT '角色编号',
                                        PRIMARY KEY (`web_user_id`, `role_id`),
                                        KEY `idx_basket_web_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网页账号角色表';

CREATE TABLE `basket_web_role_permission` (
                                              `role_id` bigint NOT NULL COMMENT '角色编号',
                                              `permission_id` bigint NOT NULL COMMENT '权限编号',
                                              PRIMARY KEY (`role_id`, `permission_id`),
                                              KEY `idx_basket_web_role_perm_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网页角色权限表';

CREATE TABLE `basket_web_login_log` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                        `web_user_id` bigint DEFAULT NULL COMMENT '网页账号编号',
                                        `email` varchar(100) DEFAULT '' COMMENT '邮箱账号',
                                        `login_ip` varchar(128) DEFAULT '' COMMENT '登录地址',
                                        `browser` varchar(80) DEFAULT '' COMMENT '浏览器',
                                        `os_name` varchar(80) DEFAULT '' COMMENT '操作系统',
                                        `login_status` char(1) NOT NULL DEFAULT '0' COMMENT '登录状态',
                                        `login_msg` varchar(255) DEFAULT '' COMMENT '登录提示',
                                        `login_time` datetime DEFAULT NULL COMMENT '登录时间',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_basket_web_login_user` (`web_user_id`),
                                        KEY `idx_basket_web_login_status` (`login_status`),
                                        KEY `idx_basket_web_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网页登录日志表';

INSERT INTO `basket_web_role` (`id`, `role_code`, `role_name`, `role_sort`, `default_flag`, `role_status`, `create_by`, `create_time`, `remark`) VALUES
                                                                                                                                                     (1, 'PLAYER', '普通球员', 1, '1', '0', 'admin', NOW(), '默认角色'),
                                                                                                                                                     (2, 'CAPTAIN', '球队队长', 2, '0', '0', 'admin', NOW(), '队长角色');

INSERT INTO `basket_web_permission` (`id`, `permission_code`, `permission_name`, `module_name`, `permission_type`, `permission_status`, `create_by`, `create_time`, `remark`) VALUES
                                                                                                                                                                                  (1, 'PROFILE_VIEW', '查看资料', '个人中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (2, 'PROFILE_EDIT', '修改资料', '个人中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (3, 'TEAM_VIEW', '查看球队', '球队中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (4, 'TEAM_CREATE', '创建球队', '球队中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (5, 'TEAM_MANAGE', '管理球队', '球队中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (6, 'TEAM_JOIN_APPLY', '申请入队', '球队中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (7, 'TOURNAMENT_VIEW', '查看赛事', '赛事中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (8, 'TOURNAMENT_REGISTER', '赛事报名', '赛事中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (9, 'MATCH_DATA_SUBMIT', '提交数据', '比赛中心', '1', '0', 'admin', NOW(), ''),
                                                                                                                                                                                  (10, 'MESSAGE_VIEW', '查看消息', '消息中心', '1', '0', 'admin', NOW(), '');

INSERT INTO `basket_web_role_permission` (`role_id`, `permission_id`) VALUES
                                                                          (1, 1),
                                                                          (1, 2),
                                                                          (1, 3),
                                                                          (1, 6),
                                                                          (1, 7),
                                                                          (1, 10),
                                                                          (2, 1),
                                                                          (2, 2),
                                                                          (2, 3),
                                                                          (2, 4),
                                                                          (2, 5),
                                                                          (2, 6),
                                                                          (2, 7),
                                                                          (2, 8),
                                                                          (2, 9),
                                                                          (2, 10);
ALTER TABLE `basket_player`
    CHANGE COLUMN `system_user_id` `web_user_id` bigint NOT NULL COMMENT '网页账号编号';

ALTER TABLE `basket_captain_record`
    ADD COLUMN `operator_type` char(1) NOT NULL DEFAULT '1' COMMENT '操作来源' AFTER `reason`,
  CHANGE COLUMN `operator_user_id` `operator_id` bigint DEFAULT NULL COMMENT '操作人编号';

ALTER TABLE `basket_team_log`
    ADD COLUMN `operator_type` char(1) NOT NULL DEFAULT '1' COMMENT '操作来源' AFTER `log_content`,
  CHANGE COLUMN `operator_user_id` `operator_id` bigint DEFAULT NULL COMMENT '操作人编号';

ALTER TABLE `basket_score_audit`
DROP COLUMN `submit_user_id`;

ALTER TABLE `basket_site_message`
    ADD COLUMN `sender_type` char(1) NOT NULL DEFAULT '1' COMMENT '发送来源' AFTER `sender_user_id`,
  CHANGE COLUMN `sender_user_id` `sender_id` bigint DEFAULT NULL COMMENT '发送人编号';

ALTER TABLE `basket_file_resource`
    ADD COLUMN `upload_type` char(1) NOT NULL DEFAULT '1' COMMENT '上传来源' AFTER `upload_user_id`,
  CHANGE COLUMN `upload_user_id` `upload_id` bigint DEFAULT NULL COMMENT '上传人编号';

SET FOREIGN_KEY_CHECKS = 1;
