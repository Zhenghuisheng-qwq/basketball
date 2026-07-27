-- CNPA platform-scoped team membership migration
-- Adds platform_code / platform_identity_id to team members and concurrency guard

START TRANSACTION;

ALTER TABLE basket_team_member
    ADD COLUMN platform_code varchar(16) NULL COMMENT '平台代码 PC/PS/XBOX' AFTER player_id,
    ADD COLUMN platform_identity_id bigint NULL COMMENT '平台游戏身份主键' AFTER platform_code,
    ADD COLUMN active_platform_slot varchar(64) NULL COMMENT '活跃平台槽位 memberId:platformCode' AFTER platform_identity_id;

-- Backfill platform_code from team game_terminal
UPDATE basket_team_member tm
    INNER JOIN basket_team t ON tm.team_id = t.id
SET tm.platform_code = CASE
    WHEN UPPER(TRIM(t.game_terminal)) IN ('PS5', 'PSN') THEN 'PS'
    ELSE UPPER(TRIM(t.game_terminal))
END
WHERE tm.platform_code IS NULL
  AND t.game_terminal IS NOT NULL
  AND tm.is_delete = 0;

-- Backfill platform_identity_id from active platform accounts
UPDATE basket_team_member tm
    INNER JOIN basket_player_platform_account pa
        ON pa.player_id = tm.player_id
        AND pa.platform = tm.platform_code
        AND pa.is_delete = 0
        AND pa.account_status = 1
SET tm.platform_identity_id = pa.id
WHERE tm.platform_identity_id IS NULL
  AND tm.is_delete = 0
  AND tm.is_current = 1
  AND tm.membership_status = 1;

-- Backfill active_platform_slot for current active memberships
UPDATE basket_team_member
SET active_platform_slot = CONCAT(member_id, ':', platform_code)
WHERE is_delete = 0
  AND is_current = 1
  AND membership_status = 1
  AND platform_code IS NOT NULL
  AND active_platform_slot IS NULL;

ALTER TABLE basket_team_member
    ADD KEY idx_team_member_platform_active (member_id, platform_code, is_current, membership_status, is_delete),
    ADD UNIQUE KEY uk_team_member_active_platform_slot (active_platform_slot);

-- One active platform account per player per platform
ALTER TABLE basket_player_platform_account
    ADD UNIQUE KEY uk_player_platform_active (player_id, platform, is_delete);

COMMIT;
