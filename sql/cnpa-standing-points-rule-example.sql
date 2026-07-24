-- =============================================================================
-- CNPA 第六轮：积分规则配置示例（basket_tournament_rule）
-- =============================================================================
-- 说明：
--   1. 数据库无结构化胜场积分字段，结算引擎读取 rule_type=STANDING_POINTS 的 rule_content
--   2. 未配置时，手工重算赛季会返回「积分规则未配置」
--   3. 请将 @season_id 替换为真实赛季 ID 后执行
-- =============================================================================

SET NAMES utf8mb4;
SET @season_id := 0; -- TODO: 替换为真实 seasonId
SET @now := NOW();

INSERT INTO basket_tournament_rule (
    season_id, rule_type, rule_title, rule_content, version_no,
    effective_time, rule_status, display_order, create_time, update_time, is_delete
)
SELECT
    @season_id,
    'STANDING_POINTS',
    '积分与同分规则',
    '# CNPA 积分规则（KEY=VALUE）
WIN_POINTS=2
LOSS_POINTS=1
FORFEIT_WIN_POINTS=2
FORFEIT_LOSS_POINTS=0
TIE_BREAK=STANDING_POINTS,HEAD_TO_HEAD_POINTS,HEAD_TO_HEAD_DIFF,POINT_DIFF,POINTS_FOR,WINS,TEAM_ID
',
    'v1',
    @now,
    1,
    1,
    @now,
    @now,
    0
FROM DUAL
WHERE @season_id > 0
  AND NOT EXISTS (
      SELECT 1 FROM basket_tournament_rule
      WHERE season_id = @season_id
        AND rule_type = 'STANDING_POINTS'
        AND version_no = 'v1'
        AND is_delete = 0
      LIMIT 1
  );
