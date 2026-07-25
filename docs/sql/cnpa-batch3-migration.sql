-- CNPA Batch 3 migration: platform game ID, change requests, cert normalized field
-- Database: basketball @ local dev
-- Run in transaction on local dev only

START TRANSACTION;

-- Normalized game account on cert application for submit-time duplicate check
ALTER TABLE basket_player_cert_application
    ADD COLUMN normalized_game_account varchar(128) NULL COMMENT '规范化游戏账号' AFTER game_account;

UPDATE basket_player_cert_application
SET normalized_game_account = LOWER(TRIM(game_account))
WHERE normalized_game_account IS NULL AND game_account IS NOT NULL;

ALTER TABLE basket_player_cert_application
    ADD KEY idx_cert_platform_normalized (game_terminal, normalized_game_account);

-- Permanent platform game ID bindings
CREATE TABLE IF NOT EXISTS basket_player_platform_account (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    player_id bigint NOT NULL COMMENT '球员主键',
    platform varchar(16) NOT NULL COMMENT '游戏平台',
    display_game_id varchar(128) NOT NULL COMMENT '展示游戏ID',
    normalized_game_id varchar(128) NOT NULL COMMENT '规范化游戏ID',
    account_status tinyint NOT NULL DEFAULT 1 COMMENT '账号状态 0待审核 1生效 2失效 3驳回',
    is_primary tinyint NOT NULL DEFAULT 0 COMMENT '是否主平台',
    source_application_id bigint NULL COMMENT '来源认证申请主键',
    approved_by bigint NULL COMMENT '审核人主键',
    approved_time datetime NULL COMMENT '审核通过时间',
    effective_time datetime NULL COMMENT '生效时间',
    invalid_time datetime NULL COMMENT '失效时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_normalized_active (platform, normalized_game_id, is_delete),
    KEY idx_platform_player (player_id, platform, account_status, is_delete),
    KEY idx_platform_primary (player_id, is_primary, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员平台游戏ID表';

-- Platform add / game ID change requests
CREATE TABLE IF NOT EXISTS basket_player_platform_change_request (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_no varchar(64) NOT NULL COMMENT '申请编号',
    player_id bigint NOT NULL COMMENT '球员主键',
    member_id bigint NOT NULL COMMENT '会员主键',
    change_type varchar(32) NOT NULL COMMENT '变更类型 ADD_PLATFORM CHANGE_GAME_ID',
    platform varchar(16) NOT NULL COMMENT '游戏平台',
    platform_account_id bigint NULL COMMENT '原平台账号主键',
    old_display_game_id varchar(128) NULL COMMENT '变更前展示游戏ID',
    new_display_game_id varchar(128) NOT NULL COMMENT '变更后展示游戏ID',
    new_normalized_game_id varchar(128) NOT NULL COMMENT '变更后规范化游戏ID',
    proof_file_id bigint NULL COMMENT '证明材料文件主键',
    request_status tinyint NOT NULL DEFAULT 0 COMMENT '申请状态 0待审核 1通过 2驳回 3撤回',
    submit_time datetime NULL COMMENT '提交时间',
    reviewed_time datetime NULL COMMENT '审核完成时间',
    reviewer_id bigint NULL COMMENT '审核人主键',
    review_remark varchar(1000) NULL COMMENT '审核说明',
    result_platform_account_id bigint NULL COMMENT '生效后平台账号主键',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_change_request_no (request_no),
    KEY idx_platform_change_player (player_id, request_status, is_delete),
    KEY idx_platform_change_member (member_id, request_status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='球员平台游戏ID变更申请表';

-- Backfill primary platform accounts from approved profiles
INSERT INTO basket_player_platform_account (
    player_id, platform, display_game_id, normalized_game_id, account_status, is_primary,
    approved_time, effective_time, create_time, update_time, is_delete
)
SELECT p.id, p.game_terminal, p.game_account, LOWER(TRIM(p.game_account)), 1, 1,
       p.certified_time, p.certified_time, NOW(), NOW(), 0
FROM basket_player_profile p
WHERE p.is_delete = 0
  AND p.certification_status = 3
  AND p.game_terminal IS NOT NULL AND p.game_account IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM basket_player_platform_account pa
      WHERE pa.player_id = p.id AND pa.is_delete = 0 AND pa.is_primary = 1
  );

COMMIT;
