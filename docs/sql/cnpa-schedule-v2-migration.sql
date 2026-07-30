-- CNPA Schedule V2: 时间槽容量 + 分配策略 + 正式规则（21:00/22:00，不破坏 V1 历史）
-- 幂等执行

-- ========== 1. 扩展规则主表 ==========
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_schedule_rule' AND COLUMN_NAME = 'slot_capacity');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_schedule_rule ADD COLUMN slot_capacity INT NOT NULL DEFAULT 12 COMMENT ''每个时间段最大比赛场次'' AFTER enabled',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'basket_schedule_rule' AND COLUMN_NAME = 'allocation_mode');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE basket_schedule_rule ADD COLUMN allocation_mode VARCHAR(32) NOT NULL DEFAULT ''FILL_FIRST'' COMMENT ''时间槽分配策略 FILL_FIRST/EVEN_SPLIT'' AFTER slot_capacity',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 2. V2 规则种子（CNPA 正式：周二/四/六，21:00/22:00，FILL_FIRST，每槽 12 场） ==========
INSERT INTO basket_schedule_rule (rule_name, rule_code, rule_version, event_type, platform_code, enabled,
                                  slot_capacity, allocation_mode, remark, is_delete)
SELECT '常规赛单循环 V2', 'REGULAR_ROUND_ROBIN', 'V2', 'REGULAR', NULL, 1,
       12, 'FILL_FIRST',
       'CNPA 正式：按实际 EventTeam 数量动态单循环；比赛日周二/四/六；时间段 21:00/22:00；每槽最多 12 场；优先填 21:00', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM basket_schedule_rule
    WHERE rule_code = 'REGULAR_ROUND_ROBIN' AND rule_version = 'V2' AND is_delete = 0
);

SET @rule_v2_id := (SELECT id FROM basket_schedule_rule
    WHERE rule_code = 'REGULAR_ROUND_ROBIN' AND rule_version = 'V2' AND is_delete = 0 LIMIT 1);

INSERT INTO basket_schedule_rule_weekday (rule_id, weekday, sort, is_delete)
SELECT @rule_v2_id, w.weekday, w.sort, 0
FROM (
    SELECT 2 AS weekday, 1 AS sort UNION ALL
    SELECT 4, 2 UNION ALL
    SELECT 6, 3
) w
WHERE @rule_v2_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_schedule_rule_weekday wd
    WHERE wd.rule_id = @rule_v2_id AND wd.weekday = w.weekday AND wd.is_delete = 0
);

INSERT INTO basket_schedule_rule_time_slot (rule_id, start_time, sort, is_delete)
SELECT @rule_v2_id, t.start_time, t.sort, 0
FROM (
    SELECT '21:00' AS start_time, 1 AS sort UNION ALL
    SELECT '22:00', 2
) t
WHERE @rule_v2_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM basket_schedule_rule_time_slot ts
    WHERE ts.rule_id = @rule_v2_id AND ts.start_time = t.start_time AND ts.is_delete = 0
);

-- V1 保留历史；若需补默认容量字段：
UPDATE basket_schedule_rule SET slot_capacity = 12 WHERE slot_capacity IS NULL OR slot_capacity <= 0;
UPDATE basket_schedule_rule SET allocation_mode = 'FILL_FIRST' WHERE allocation_mode IS NULL OR allocation_mode = '';
