-- CNPA 数据中心 V2 第 3 轮：正式榜快照
-- 第 4 轮的全站推荐赛事也放在独立设置表，不改 basket_division。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS basket_leaderboard_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  run_id BIGINT NOT NULL,
  division_id BIGINT NOT NULL,
  scope_key VARCHAR(64) NOT NULL,
  policy_id BIGINT NOT NULL,
  source_revision VARCHAR(128) NULL,
  published_at DATETIME NULL,
  is_public TINYINT NOT NULL DEFAULT 0,
  public_scope_key VARCHAR(64) NULL COMMENT '当前公开指针，同一赛区同一范围只留一行',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lb_snapshot_public (division_id, public_scope_key),
  KEY idx_lb_snapshot_policy (policy_id),
  KEY idx_lb_snapshot_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心正式排行快照';

CREATE TABLE IF NOT EXISTS basket_leaderboard_setting (
  id BIGINT NOT NULL COMMENT '固定为 1',
  featured_division_id BIGINT NULL COMMENT '导航进入数据中心时的默认赛区',
  update_by BIGINT NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心排行设置';

INSERT INTO basket_leaderboard_setting (id, featured_division_id)
SELECT 1, NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_leaderboard_setting WHERE id = 1);
