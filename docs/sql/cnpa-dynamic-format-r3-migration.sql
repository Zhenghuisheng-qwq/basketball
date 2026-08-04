-- =============================================================================
-- CNPA 动态赛制重构 R3：阶段执行、赛程生成、积分排名与晋级
-- 前置：R1 + R2 migration 已执行
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. basket_division_stage 执行字段
-- ---------------------------------------------------------------------------
ALTER TABLE basket_division_stage
  ADD COLUMN execution_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '执行状态' AFTER stage_status,
  ADD COLUMN schedule_batch_id BIGINT DEFAULT NULL COMMENT '当前赛程批次ID' AFTER execution_status,
  ADD COLUMN participant_count INT DEFAULT NULL COMMENT '参赛队伍数' AFTER schedule_batch_id,
  ADD COLUMN qualified_count INT DEFAULT NULL COMMENT '晋级名额数' AFTER participant_count,
  ADD COLUMN started_time DATETIME DEFAULT NULL COMMENT '开始时间' AFTER qualified_count,
  ADD COLUMN completed_time DATETIME DEFAULT NULL COMMENT '完成时间' AFTER started_time,
  ADD COLUMN confirmed_time DATETIME DEFAULT NULL COMMENT '晋级确认时间' AFTER completed_time,
  ADD COLUMN confirmed_by BIGINT DEFAULT NULL COMMENT '晋级确认人' AFTER confirmed_time,
  ADD COLUMN current_unique_key VARCHAR(128) DEFAULT NULL COMMENT '当前有效唯一键' AFTER confirmed_by,
  ADD COLUMN execution_version INT NOT NULL DEFAULT 0 COMMENT '执行版本号' AFTER current_unique_key;

ALTER TABLE basket_division_stage
  ADD KEY idx_stage_execution (division_id, execution_status);

-- ---------------------------------------------------------------------------
-- 2. basket_captain_grant 授权来源（R2 收口）
-- ---------------------------------------------------------------------------
ALTER TABLE basket_captain_grant
  ADD COLUMN grant_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '授权来源：MANUAL/PERSONAL_DRAW' AFTER remark,
  ADD COLUMN source_draw_team_id BIGINT DEFAULT NULL COMMENT '个人抽签临时队ID' AFTER grant_source;

-- ---------------------------------------------------------------------------
-- 3. basket_division_stage_team 阶段参赛队伍
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_division_stage_team (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  division_id BIGINT NOT NULL COMMENT '赛区ID',
  snapshot_id BIGINT NOT NULL COMMENT '快照ID',
  stage_id BIGINT NOT NULL COMMENT '阶段ID',
  event_team_id BIGINT NOT NULL COMMENT '赛事球队ID',
  source_stage_id BIGINT DEFAULT NULL COMMENT '来源阶段ID',
  source_group_id BIGINT DEFAULT NULL COMMENT '来源小组ID',
  source_rank INT DEFAULT NULL COMMENT '来源排名',
  seed_no INT DEFAULT NULL COMMENT '种子序号',
  entry_type VARCHAR(32) NOT NULL DEFAULT 'INITIAL' COMMENT '入队类型',
  qualification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '资格状态',
  active_unique_key VARCHAR(128) DEFAULT NULL COMMENT '有效唯一键 stageId_eventTeamId',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stage_team_active (active_unique_key),
  KEY idx_stage_team_stage (stage_id, is_delete),
  KEY idx_stage_team_division (division_id, is_delete),
  KEY idx_stage_team_event (event_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4阶段参赛队伍';

-- ---------------------------------------------------------------------------
-- 4. basket_division_stage_qualification 晋级结果
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_division_stage_qualification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  division_id BIGINT NOT NULL COMMENT '赛区ID',
  stage_id BIGINT NOT NULL COMMENT '来源阶段ID',
  group_id BIGINT DEFAULT NULL COMMENT '来源小组ID',
  event_team_id BIGINT NOT NULL COMMENT '赛事球队ID',
  qualification_type VARCHAR(32) NOT NULL COMMENT '晋级类型',
  source_rank INT DEFAULT NULL COMMENT '来源排名',
  target_stage_id BIGINT DEFAULT NULL COMMENT '目标阶段ID',
  qualification_status VARCHAR(32) NOT NULL DEFAULT 'PREVIEW' COMMENT 'PREVIEW/CONFIRMED/CANCELLED',
  confirmed_by BIGINT DEFAULT NULL COMMENT '确认人',
  confirmed_time DATETIME DEFAULT NULL COMMENT '确认时间',
  active_unique_key VARCHAR(128) DEFAULT NULL COMMENT '有效唯一键',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_qualification_active (active_unique_key),
  KEY idx_qualification_stage (stage_id, qualification_status, is_delete),
  KEY idx_qualification_target (target_stage_id, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V4阶段晋级结果';

-- ---------------------------------------------------------------------------
-- 5. basket_match 阶段/分组/淘汰关联字段
-- ---------------------------------------------------------------------------
ALTER TABLE basket_match
  ADD COLUMN stage_id BIGINT DEFAULT NULL COMMENT '阶段ID' AFTER stage_code,
  ADD COLUMN group_id BIGINT DEFAULT NULL COMMENT '小组ID' AFTER stage_id,
  ADD COLUMN bracket_slot INT DEFAULT NULL COMMENT '淘汰 bracket 槽位' AFTER round_no,
  ADD COLUMN source_match_id_home BIGINT DEFAULT NULL COMMENT '主队来源比赛ID' AFTER bracket_slot,
  ADD COLUMN source_match_id_away BIGINT DEFAULT NULL COMMENT '客队来源比赛ID' AFTER source_match_id_home,
  ADD COLUMN loser_event_team_id BIGINT DEFAULT NULL COMMENT '负方赛事球队ID' AFTER winner_event_team_id;

ALTER TABLE basket_match
  ADD KEY idx_match_stage (division_id, stage_id, group_id),
  ADD KEY idx_match_bracket (stage_id, round_no, bracket_slot);

-- ---------------------------------------------------------------------------
-- 6. basket_schedule_batch 扩展
-- ---------------------------------------------------------------------------
ALTER TABLE basket_schedule_batch
  ADD COLUMN stage_id BIGINT DEFAULT NULL COMMENT '阶段ID' AFTER division_id,
  ADD COLUMN grouping_batch_id BIGINT DEFAULT NULL COMMENT '分组批次ID' AFTER stage_id,
  ADD COLUMN generation_strategy VARCHAR(64) DEFAULT NULL COMMENT '生成策略' AFTER rule_code,
  ADD COLUMN expected_match_count INT DEFAULT NULL COMMENT '预计比赛数' AFTER match_count,
  ADD COLUMN actual_match_count INT DEFAULT NULL COMMENT '实际比赛数' AFTER expected_match_count,
  ADD COLUMN execution_version INT DEFAULT NULL COMMENT '执行版本' AFTER actual_match_count;

ALTER TABLE basket_schedule_batch
  ADD KEY idx_schedule_batch_stage (division_id, stage_id);

-- ---------------------------------------------------------------------------
-- 7. basket_standing 阶段和分组维度
-- group_scope_key: 0=全阶段, group_id=小组
-- ---------------------------------------------------------------------------
ALTER TABLE basket_standing
  ADD COLUMN stage_id BIGINT DEFAULT NULL COMMENT '阶段ID' AFTER division_id,
  ADD COLUMN group_id BIGINT DEFAULT NULL COMMENT '小组ID' AFTER stage_id,
  ADD COLUMN group_scope_key BIGINT NOT NULL DEFAULT 0 COMMENT '分组范围键：0=无小组' AFTER group_id,
  ADD COLUMN rank_explain_json TEXT DEFAULT NULL COMMENT '排名解释JSON' AFTER standing_points,
  ADD COLUMN manual_rank_pending TINYINT NOT NULL DEFAULT 0 COMMENT '是否待人工排序' AFTER rank_explain_json,
  ADD COLUMN active_unique_key VARCHAR(128) DEFAULT NULL COMMENT '有效唯一键' AFTER manual_rank_pending;

-- 迁移旧数据：division 级积分榜 group_scope_key=0
UPDATE basket_standing SET group_scope_key = 0, active_unique_key = CONCAT(division_id, '_0_', event_team_id)
WHERE active_unique_key IS NULL AND is_delete = 0;

ALTER TABLE basket_standing
  DROP INDEX uk_standing_division_team;

ALTER TABLE basket_standing
  ADD UNIQUE KEY uk_standing_scope_team (active_unique_key),
  ADD KEY idx_standing_stage (division_id, stage_id, group_scope_key, rank_no);
