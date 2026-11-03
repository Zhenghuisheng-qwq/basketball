-- CNPA 数据中心 V2 第 1 轮：排行规则草稿
-- 只新增表，不修改比赛、积分榜或历史统计。
-- 上线时由人工在目标库执行。不要对已有业务表做 UPDATE / DELETE。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS basket_leaderboard_policy (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  division_id BIGINT NOT NULL COMMENT '赛区，对应文档 eventId',
  scope_key VARCHAR(64) NOT NULL COMMENT 'STAGE:{stageId} 或 OVERALL',
  stage_id BIGINT NULL COMMENT '阶段 ID，OVERALL 为空',
  version_no INT NOT NULL COMMENT '规则版本，发布后不回改',
  lifecycle VARCHAR(32) NOT NULL COMMENT 'DRAFT/READY/ACTIVE/SUPERSEDED/ARCHIVED',
  template_code VARCHAR(64) NULL COMMENT '加载模板时的赛制代码',
  ranking_audience VARCHAR(32) NOT NULL DEFAULT 'QUALIFIED_ONLY' COMMENT 'QUALIFIED_ONLY 或 ALL_PARTICIPANTS',
  config_json JSON NOT NULL COMMENT '门槛配置',
  based_on_policy_id BIGINT NULL COMMENT '修订来源',
  expected_lock INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  draft_scope_key VARCHAR(64) NULL COMMENT 'DRAFT/READY 时等于 scope_key',
  active_scope_key VARCHAR(64) NULL COMMENT 'ACTIVE 时等于 scope_key',
  publish_note VARCHAR(512) NULL COMMENT '发布或归档说明',
  create_by BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lb_policy_version (division_id, scope_key, version_no, is_delete),
  UNIQUE KEY uk_lb_policy_open_draft (division_id, draft_scope_key, is_delete),
  UNIQUE KEY uk_lb_policy_active (division_id, active_scope_key, is_delete),
  KEY idx_lb_policy_division (division_id, scope_key, lifecycle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心排行规则版本';

CREATE TABLE IF NOT EXISTS basket_leaderboard_policy_audit (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  policy_id BIGINT NULL,
  division_id BIGINT NOT NULL,
  scope_key VARCHAR(64) NULL,
  action VARCHAR(32) NOT NULL COMMENT 'SAVE/VALIDATE/TEMPLATE/PREVIEW/PUBLISH/RECALC/ARCHIVE/FEATURE',
  operator_id BIGINT NULL,
  operator_name VARCHAR(128) NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  reason VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lb_policy_audit_division (division_id, create_time),
  KEY idx_lb_policy_audit_policy (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心排行规则审计';
