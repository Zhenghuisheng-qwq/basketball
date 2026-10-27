-- =============================================================================
-- 荣誉分类基础数据
-- 与 basketball-admin HonorCodeEnum 对齐。
-- 管理端启动、以及保存荣誉时，会按该枚举自动补齐；本脚本只给空库手工执行。
-- =============================================================================

SET NAMES utf8mb4;

INSERT INTO basket_honor_category (
  honor_code, honor_name, honor_scope, recipient_type, description,
  display_order, honor_status, create_time, update_time, is_delete
) VALUES
('CHAMPION',          '冠军',     'TOURNAMENT', 'TEAM',   '赛季冠军',   10, 1, NOW(), NOW(), 0),
('RUNNER_UP',         '亚军',     'TOURNAMENT', 'TEAM',   '赛季亚军',   20, 1, NOW(), NOW(), 0),
('THIRD_PLACE',       '季军',     'TOURNAMENT', 'TEAM',   '赛季季军',   30, 1, NOW(), NOW(), 0),
('TEAM_HONOR',        '球队荣誉', 'TEAM',       'TEAM',   '人工球队荣誉', 40, 1, NOW(), NOW(), 0),
('SCORING_CHAMPION',  '得分王',   'PLAYER',     'PLAYER', '赛季得分王', 50, 1, NOW(), NOW(), 0),
('REBOUND_CHAMPION',  '篮板王',   'PLAYER',     'PLAYER', '赛季篮板王', 60, 1, NOW(), NOW(), 0),
('ASSIST_CHAMPION',   '助攻王',   'PLAYER',     'PLAYER', '赛季助攻王', 70, 1, NOW(), NOW(), 0),
('STEAL_CHAMPION',    '抢断王',   'PLAYER',     'PLAYER', '赛季抢断王', 80, 1, NOW(), NOW(), 0),
('BLOCK_CHAMPION',    '盖帽王',   'PLAYER',     'PLAYER', '赛季盖帽王', 90, 1, NOW(), NOW(), 0),
('PLAYER_HONOR',      '球员荣誉', 'PLAYER',     'PLAYER', '人工球员荣誉', 100, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  honor_name = VALUES(honor_name),
  honor_scope = VALUES(honor_scope),
  recipient_type = VALUES(recipient_type),
  display_order = VALUES(display_order),
  honor_status = VALUES(honor_status),
  update_time = NOW();
