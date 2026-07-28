-- CNPA core business model batch 1
-- Team decoupled from platform; captain qualification evolved to event-scoped captain grant

START TRANSACTION;

-- 1. Captain grant: bind to season/event and optional team
ALTER TABLE basket_captain_qualification
    ADD COLUMN season_id BIGINT NULL COMMENT '赛事赛季主键（队长授权所属赛段）' AFTER player_id,
    ADD COLUMN team_id BIGINT NULL COMMENT '绑定球队主键，未建队前可为空' AFTER season_id,
    ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注' AFTER revoke_reason;

ALTER TABLE basket_captain_qualification
    ADD KEY idx_captain_grant_season_member (season_id, member_id, is_current, qualification_status, is_delete),
    ADD KEY idx_captain_grant_team (team_id, qualification_status, is_delete);

-- 2. Event open platforms per season
CREATE TABLE IF NOT EXISTS basket_tournament_season_platform (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    season_id BIGINT NOT NULL COMMENT '赛事赛季主键',
    platform_code VARCHAR(16) NOT NULL COMMENT '平台代码 PC/PS/XBOX',
    platform_status TINYINT NOT NULL DEFAULT 1 COMMENT '开放状态 1开放 0关闭',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_season_platform (season_id, platform_code, is_delete),
    KEY idx_season_platform_status (season_id, platform_status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事赛季开放平台表';

-- Backfill open platforms from tournament.game_terminal when season has single platform
INSERT INTO basket_tournament_season_platform (season_id, platform_code, platform_status, is_delete)
SELECT s.id,
       CASE
           WHEN UPPER(TRIM(t.game_terminal)) IN ('PS5', 'PSN') THEN 'PS'
           WHEN UPPER(TRIM(t.game_terminal)) IN ('PC', 'PS', 'XBOX') THEN UPPER(TRIM(t.game_terminal))
           ELSE 'PC'
       END,
       1,
       0
FROM basket_tournament_season s
         INNER JOIN basket_tournament t ON t.id = s.tournament_id AND t.is_delete = 0
WHERE s.is_delete = 0
  AND t.game_terminal IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_tournament_season_platform p
    WHERE p.season_id = s.id AND p.is_delete = 0
);

-- 3. Registration platform belongs to registration, not team
ALTER TABLE basket_tournament_registration
    ADD COLUMN platform_code VARCHAR(16) NULL COMMENT '报名平台 PC/PS/XBOX' AFTER team_id;

-- Backfill registration platform from team.game_terminal (legacy)
UPDATE basket_tournament_registration r
    INNER JOIN basket_team tm ON tm.id = r.team_id
SET r.platform_code = CASE
    WHEN UPPER(TRIM(tm.game_terminal)) IN ('PS5', 'PSN') THEN 'PS'
    WHEN UPPER(TRIM(tm.game_terminal)) IN ('PC', 'PS', 'XBOX') THEN UPPER(TRIM(tm.game_terminal))
    ELSE 'PC'
END
WHERE r.platform_code IS NULL
  AND r.is_delete = 0;

-- Allow same team register different platforms in same season
ALTER TABLE basket_tournament_registration
    DROP INDEX uk_registration_season_team;

ALTER TABLE basket_tournament_registration
    ADD UNIQUE KEY uk_registration_season_team_platform (season_id, team_id, platform_code, is_delete);

-- 4. Team member: one active team per user (member-level slot)
ALTER TABLE basket_team_member
    ADD COLUMN active_member_slot VARCHAR(64) NULL COMMENT '活跃成员槽位 memberId' AFTER active_platform_slot;

UPDATE basket_team_member
SET active_member_slot = CAST(member_id AS CHAR)
WHERE is_delete = 0
  AND is_current = 1
  AND membership_status = 1
  AND active_member_slot IS NULL;

ALTER TABLE basket_team_member
    ADD UNIQUE KEY uk_team_member_active_member_slot (active_member_slot);

-- 5. Deprecate team.game_terminal as core attribute: keep column, use neutral default for new teams
ALTER TABLE basket_team
    MODIFY COLUMN game_terminal VARCHAR(16) NOT NULL DEFAULT 'GENERAL' COMMENT '已废弃-历史比赛终端，球队本体不绑定平台';

UPDATE basket_team
SET game_terminal = 'GENERAL'
WHERE game_terminal IS NULL OR TRIM(game_terminal) = '';

COMMIT;
