-- =============================================================================
-- CNPA 第七轮：荣誉分类种子数据（basket_honor_category）
-- 可重复执行；不修改表结构
-- =============================================================================
SET NAMES utf8mb4;
SET @now := NOW();

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'CHAMPION', '冠军', 'TOURNAMENT', 'TEAM', '赛季冠军', 1, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'CHAMPION' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'RUNNER_UP', '亚军', 'TOURNAMENT', 'TEAM', '赛季亚军', 2, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'RUNNER_UP' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'THIRD_PLACE', '季军', 'TOURNAMENT', 'TEAM', '赛季季军', 3, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'THIRD_PLACE' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'SCORING_CHAMPION', '得分王', 'PLAYER', 'PLAYER', '赛季得分王（管理员确认）', 10, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'SCORING_CHAMPION' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'REBOUND_CHAMPION', '篮板王', 'PLAYER', 'PLAYER', '赛季篮板王（管理员确认）', 11, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'REBOUND_CHAMPION' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'ASSIST_CHAMPION', '助攻王', 'PLAYER', 'PLAYER', '赛季助攻王（管理员确认）', 12, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'ASSIST_CHAMPION' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'STEAL_CHAMPION', '抢断王', 'PLAYER', 'PLAYER', '赛季抢断王（管理员确认）', 13, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'STEAL_CHAMPION' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'BLOCK_CHAMPION', '盖帽王', 'PLAYER', 'PLAYER', '赛季盖帽王（管理员确认）', 14, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'BLOCK_CHAMPION' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'TEAM_HONOR', '球队荣誉', 'TEAM', 'TEAM', '通用球队荣誉', 20, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'TEAM_HONOR' AND is_delete = 0);

INSERT INTO basket_honor_category (honor_code, honor_name, honor_scope, recipient_type, description, display_order, honor_status, create_time, update_time, is_delete)
SELECT 'PLAYER_HONOR', '球员荣誉', 'PLAYER', 'PLAYER', '通用球员荣誉（人工评选需填写说明）', 21, 1, @now, @now, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM basket_honor_category WHERE honor_code = 'PLAYER_HONOR' AND is_delete = 0);
