-- =============================================================================
-- CNPA 动态赛制重构 R4：排名解释结构化、Standing 唯一键修正、MANUAL 同分处理
-- 前置：R1 + R2 + R3 migration 已执行
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. basket_standing_rank_reason 结构化排名解释（替代 rank_explain_json）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_standing_rank_reason (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  division_id BIGINT NOT NULL COMMENT '赛区ID',
  stage_id BIGINT NOT NULL COMMENT '阶段ID',
  group_id BIGINT DEFAULT NULL COMMENT '小组ID，非小组赛为NULL',
  standing_id BIGINT NOT NULL COMMENT '积分榜记录ID',
  event_team_id BIGINT NOT NULL COMMENT '赛事球队ID',
  rule_code VARCHAR(32) NOT NULL COMMENT '排序规则编码',
  priority_no INT NOT NULL COMMENT '规则优先级',
  compare_value VARCHAR(128) DEFAULT NULL COMMENT '比较数值',
  reason_text VARCHAR(500) DEFAULT NULL COMMENT '排名解释文案',
  is_decisive TINYINT NOT NULL DEFAULT 0 COMMENT '是否决定性依据：1是 0否',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_rank_reason_standing (standing_id, is_delete),
  KEY idx_rank_reason_stage_team (stage_id, event_team_id, is_delete),
  KEY idx_rank_reason_scope (division_id, stage_id, group_id, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4积分榜排名解释明细';

-- ---------------------------------------------------------------------------
-- 2. basket_standing MANUAL 人工排名字段
-- ---------------------------------------------------------------------------
ALTER TABLE basket_standing
  ADD COLUMN manual_rank_no INT DEFAULT NULL COMMENT '人工指定排名' AFTER manual_rank_pending,
  ADD COLUMN manual_rank_reason VARCHAR(500) DEFAULT NULL COMMENT '人工排名原因' AFTER manual_rank_no,
  ADD COLUMN manual_rank_by BIGINT DEFAULT NULL COMMENT '人工排名操作人' AFTER manual_rank_reason,
  ADD COLUMN manual_rank_time DATETIME DEFAULT NULL COMMENT '人工排名时间' AFTER manual_rank_by;

-- ---------------------------------------------------------------------------
-- 3. Standing active_unique_key 格式修正与历史回填
--    新格式：
--    非小组：DIVISION:{divisionId}:STAGE:{stageId}:GROUP:GLOBAL:TEAM:{eventTeamId}
--    小组：  DIVISION:{divisionId}:STAGE:{stageId}:GROUP:{groupId}:TEAM:{eventTeamId}
--    历史 division 级（无 stage_id）：保留旧键或置 NULL 待人工处理
-- ---------------------------------------------------------------------------

-- 3.1 逻辑删除记录清空 active_unique_key
UPDATE basket_standing
SET active_unique_key = NULL
WHERE is_delete = 1 AND active_unique_key IS NOT NULL;

-- 3.2 有效记录但缺 stage_id 的历史 division 级积分榜：清空键避免冲突，标记待迁移
UPDATE basket_standing
SET active_unique_key = NULL
WHERE is_delete = 0
  AND stage_id IS NULL
  AND active_unique_key IS NOT NULL;

-- 3.3 回填 stage 维度有效记录
UPDATE basket_standing s
SET active_unique_key = CASE
      WHEN s.group_scope_key IS NULL OR s.group_scope_key = 0 THEN
        CONCAT('DIVISION:', s.division_id, ':STAGE:', s.stage_id, ':GROUP:GLOBAL:TEAM:', s.event_team_id)
      ELSE
        CONCAT('DIVISION:', s.division_id, ':STAGE:', s.stage_id, ':GROUP:', s.group_scope_key, ':TEAM:', s.event_team_id)
    END
WHERE s.is_delete = 0
  AND s.stage_id IS NOT NULL;

-- 3.4 旧格式 {stageId}_{groupScopeKey}_{eventTeamId} 或 {divisionId}_0_{eventTeamId} 迁移
UPDATE basket_standing s
SET active_unique_key = CASE
      WHEN s.group_scope_key IS NULL OR s.group_scope_key = 0 THEN
        CONCAT('DIVISION:', s.division_id, ':STAGE:', s.stage_id, ':GROUP:GLOBAL:TEAM:', s.event_team_id)
      ELSE
        CONCAT('DIVISION:', s.division_id, ':STAGE:', s.stage_id, ':GROUP:', s.group_scope_key, ':TEAM:', s.event_team_id)
    END
WHERE s.is_delete = 0
  AND s.stage_id IS NOT NULL
  AND s.active_unique_key IS NOT NULL
  AND s.active_unique_key NOT LIKE 'DIVISION:%';

-- 3.5 处理 active_unique_key 重复：保留 id 最小，其余置 NULL 待人工
UPDATE basket_standing s
JOIN (
  SELECT active_unique_key, MIN(id) AS keep_id
  FROM basket_standing
  WHERE is_delete = 0 AND active_unique_key IS NOT NULL
  GROUP BY active_unique_key
  HAVING COUNT(*) > 1
) dup ON dup.active_unique_key = s.active_unique_key AND s.id <> dup.keep_id
SET s.active_unique_key = NULL, s.update_time = NOW()
WHERE s.is_delete = 0;

-- ---------------------------------------------------------------------------
-- 4. rank_explain_json 停止新写入说明（字段保留至 R5 物理删除）
-- ---------------------------------------------------------------------------
-- 应用层已改为写入 basket_standing_rank_reason，不再写入 rank_explain_json

-- ---------------------------------------------------------------------------
-- 5. 索引补充
-- ---------------------------------------------------------------------------
ALTER TABLE basket_standing
  ADD KEY idx_standing_manual_pending (stage_id, manual_rank_pending, is_delete);

ALTER TABLE basket_division
  ADD KEY idx_division_format_snapshot (format_snapshot_id);
