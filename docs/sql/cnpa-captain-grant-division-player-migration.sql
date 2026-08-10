-- CNPA V2 队长授权：division + player 唯一维度迁移
-- 执行前请备份 basket_captain_grant

-- ---------------------------------------------------------------------------
-- 0. Fail-fast：同一 division + player 不得存在多条 ACTIVE
-- ---------------------------------------------------------------------------
SELECT division_id, player_id, COUNT(*) AS cnt
FROM basket_captain_grant
WHERE is_delete = 0
  AND grant_status = 'ACTIVE'
  AND active_unique_key IS NOT NULL
GROUP BY division_id, player_id
HAVING cnt > 1;

-- 若上一查询有结果，请先人工处理后再继续。

-- ---------------------------------------------------------------------------
-- 1. team_id 改为 NULLABLE
-- ---------------------------------------------------------------------------
ALTER TABLE basket_captain_grant
    MODIFY COLUMN team_id BIGINT NULL COMMENT '后绑定长期球队ID，授权创建时可为空';

-- ---------------------------------------------------------------------------
-- 2. 重建 ACTIVE 记录的 active_unique_key = divisionId_playerId
-- ---------------------------------------------------------------------------
UPDATE basket_captain_grant
SET active_unique_key = CONCAT(division_id, '_', player_id),
    update_time = NOW()
WHERE is_delete = 0
  AND grant_status = 'ACTIVE'
  AND active_unique_key IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. REVOKED / EXPIRED 清空 active_unique_key
-- ---------------------------------------------------------------------------
UPDATE basket_captain_grant
SET active_unique_key = NULL,
    update_time = NOW()
WHERE is_delete = 0
  AND grant_status IN ('REVOKED', 'EXPIRED')
  AND active_unique_key IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. 索引
-- ---------------------------------------------------------------------------
ALTER TABLE basket_captain_grant
    ADD INDEX idx_captain_division_player (division_id, player_id);

ALTER TABLE basket_captain_grant
    ADD INDEX idx_captain_member (member_id);

-- ---------------------------------------------------------------------------
-- 5. 注释更新
-- ---------------------------------------------------------------------------
ALTER TABLE basket_captain_grant
    MODIFY COLUMN active_unique_key VARCHAR(128) DEFAULT NULL
        COMMENT '仅ACTIVE有效授权写入 divisionId_playerId；REVOKED/EXPIRED/软删除写NULL';
