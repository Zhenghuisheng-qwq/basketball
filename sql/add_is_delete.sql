-- 为 basketball 库所有业务表补充逻辑删除字段 is_delete
-- 约定：0=未删，1=已删（与 Java 实体 isDelete 字段对应）

USE `basketball`;

ALTER TABLE `basket_captain_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_captain_record` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_email_code` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_file_resource` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_group` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_honor` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_invite_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_join_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_leave_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_match` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_match_player_stat` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_match_team` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_notice` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_player` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_player_summary` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_registration` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_registration_player` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_remove_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_rename_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_round` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_score_audit` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_season` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_site_banner` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_site_message` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_team` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_team_log` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_team_member` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_team_summary` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_tournament` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_tournament_audit` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_tournament_player` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_tournament_rule` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_tournament_standing` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_tournament_team` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_transfer_apply` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_web_login_log` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_web_permission` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_web_role` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_web_role_permission` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_web_user` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
ALTER TABLE `basket_web_user_role` ADD COLUMN `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删';
