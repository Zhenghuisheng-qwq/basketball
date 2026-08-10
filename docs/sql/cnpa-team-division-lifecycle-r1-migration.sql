-- CNPA 球队赛事生命周期 R1：Team / TeamMember 绑定 Division 数据地基
-- 范围：仅结构（字段/索引/唯一约束），不回填歧义历史归属，不改 EventTeam/Match
-- 执行前建议备份 basket_team / basket_team_member

-- ---------------------------------------------------------------------------
-- 0. 预检：全局同名（旧 uk）应为空；新唯一键在 division_id 全 NULL 时对 Legacy 安全
-- ---------------------------------------------------------------------------
SELECT team_name, is_delete, COUNT(*) AS cnt
FROM basket_team
GROUP BY team_name, is_delete
HAVING cnt > 1;

-- ---------------------------------------------------------------------------
-- 1. basket_team.division_id
-- ---------------------------------------------------------------------------
ALTER TABLE basket_team
    ADD COLUMN division_id BIGINT NULL COMMENT '所属赛事Division ID，NULL表示Legacy历史球队'
        AFTER captain_player_id;

ALTER TABLE basket_team
    ADD KEY idx_team_division_id (division_id);

-- 队名唯一：全局 → Division 内
ALTER TABLE basket_team
    DROP INDEX uk_team_name_delete;

ALTER TABLE basket_team
    ADD UNIQUE KEY uk_team_division_name_delete (division_id, team_name, is_delete);

ALTER TABLE basket_team
    COMMENT = '赛事球队表（Division级；division_id为空表示Legacy历史球队）';

ALTER TABLE basket_team
    MODIFY COLUMN team_status TINYINT NOT NULL DEFAULT 1
        COMMENT '球队状态：0DISABLED/1ACTIVE/2DISSOLVED/3ARCHIVED';

-- ---------------------------------------------------------------------------
-- 2. basket_team_member.division_id
-- ---------------------------------------------------------------------------
ALTER TABLE basket_team_member
    ADD COLUMN division_id BIGINT NULL COMMENT '所属赛事Division ID，NULL表示Legacy历史成员关系'
        AFTER team_id;

ALTER TABLE basket_team_member
    ADD KEY idx_team_member_division_id (division_id);

-- leave_reason 已存在 VARCHAR(500)，足够存储 DIVISION_FINISHED，无需变更

-- ---------------------------------------------------------------------------
-- 3. Legacy 策略：本脚本不做 division_id 回填
--    跨 Division / 歧义 Team 与全部存量保持 NULL，待第二批新创建强制写入
-- ---------------------------------------------------------------------------
