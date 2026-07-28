-- CNPA platform identity effective history migration
-- Records INITIAL_BIND / UPDATE_GAME_ID for approved platform game IDs

START TRANSACTION;

CREATE TABLE IF NOT EXISTS basket_player_platform_identity_history (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '历史记录主键',
    user_id bigint NOT NULL COMMENT '登录用户主键 member_id',
    player_id bigint NOT NULL COMMENT '球员档案主键',
    platform_identity_id bigint NOT NULL COMMENT '平台游戏身份主键',
    platform_code varchar(16) NOT NULL COMMENT '平台代码 PC/PS/XBOX',
    change_type varchar(32) NOT NULL COMMENT 'INITIAL_BIND/UPDATE_GAME_ID',
    old_game_id varchar(128) NULL COMMENT '变更前游戏 ID',
    new_game_id varchar(128) NOT NULL COMMENT '变更后游戏 ID',
    effective_time datetime NOT NULL COMMENT '生效时间',
    audit_record_id bigint NULL COMMENT '来源审核记录主键',
    operator_id bigint NULL COMMENT '操作人主键',
    initial_bind_slot varchar(64) NULL COMMENT '首次绑定幂等槽位 playerId:platformCode',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_identity_initial_bind_slot (initial_bind_slot),
    KEY idx_platform_identity_history_player (player_id, platform_code, effective_time, is_delete),
    KEY idx_platform_identity_history_user (user_id, effective_time, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台游戏ID有效变更历史';

-- 幂等补偿：为已生效但无历史的平台身份补写 INITIAL_BIND
INSERT INTO basket_player_platform_identity_history (
    user_id, player_id, platform_identity_id, platform_code, change_type,
    old_game_id, new_game_id, effective_time, audit_record_id, operator_id,
    initial_bind_slot, create_time, is_delete
)
SELECT
    p.member_id,
    pa.player_id,
    pa.id,
    CASE
        WHEN UPPER(TRIM(pa.platform)) IN ('PS5', 'PSN') THEN 'PS'
        ELSE UPPER(TRIM(pa.platform))
    END,
    'INITIAL_BIND',
    NULL,
    pa.display_game_id,
    COALESCE(pa.approved_time, pa.effective_time, pa.update_time, pa.create_time),
    pa.source_application_id,
    pa.approved_by,
    CONCAT(pa.player_id, ':', CASE
        WHEN UPPER(TRIM(pa.platform)) IN ('PS5', 'PSN') THEN 'PS'
        ELSE UPPER(TRIM(pa.platform))
    END),
    NOW(),
    0
FROM basket_player_platform_account pa
INNER JOIN basket_player_profile p ON p.id = pa.player_id AND p.is_delete = 0
WHERE pa.is_delete = 0
  AND pa.account_status = 1
  AND pa.display_game_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM basket_player_platform_identity_history h
      WHERE h.is_delete = 0
        AND h.player_id = pa.player_id
        AND h.platform_code = CASE
            WHEN UPPER(TRIM(pa.platform)) IN ('PS5', 'PSN') THEN 'PS'
            ELSE UPPER(TRIM(pa.platform))
        END
        AND h.change_type = 'INITIAL_BIND'
  );

COMMIT;
