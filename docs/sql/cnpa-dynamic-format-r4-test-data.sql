-- =============================================================================
-- CNPA 动态赛制 R4 测试数据准备
-- 说明：仅准备账号/球员/基础标识，业务状态流转通过接口完成
-- 测试批次编码：R4_TEST_BATCH_20260805
-- =============================================================================

SET NAMES utf8mb4;

SET @test_batch_code = 'R4_TEST_BATCH_20260805';

-- ---------------------------------------------------------------------------
-- 1. 测试标识记录（便于清理）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS basket_r4_test_marker (
  id BIGINT NOT NULL AUTO_INCREMENT,
  test_batch_code VARCHAR(64) NOT NULL,
  entity_type VARCHAR(32) NOT NULL COMMENT 'SERIES/SEASON/DIVISION/PLAYER/TEAM/USER',
  entity_id BIGINT NOT NULL,
  remark VARCHAR(200) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_r4_marker_batch (test_batch_code, entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R4联调测试数据标记';

-- ---------------------------------------------------------------------------
-- 2. 测试说明（不直接伪造报名/EventTeam/比赛/积分）
--    场景 A-D 的 Division 应通过 Admin 发布页 + 接口创建
--    统一命名前缀：R4_TEST_
-- ---------------------------------------------------------------------------
-- 场景 A: R4_TEST_TEAM_5V5_RR_4TEAMS
-- 场景 B: R4_TEST_TEAM_3V3_RR_5TEAMS
-- 场景 C: R4_TEST_TEAM_5V5_GROUP16_KO8
-- 场景 D: R4_TEST_PERSONAL_3V3_KO2

-- 若需预置测试球员账号，请使用已有 dev 账号并在 marker 表标记
-- 示例（需替换为实际 user_id / player_id）：
-- INSERT INTO basket_r4_test_marker (test_batch_code, entity_type, entity_id, remark)
-- SELECT @test_batch_code, 'USER', id, 'R4测试管理员' FROM sys_user WHERE user_name = 'admin' LIMIT 1;

SELECT @test_batch_code AS test_batch_code, 'R4 test data marker table ready' AS status;
