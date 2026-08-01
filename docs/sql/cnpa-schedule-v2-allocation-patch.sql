-- CNPA Schedule V2：分配策略改为 EVEN_SPLIT_WITH_LATE_EXTRA（奇数场次晚场多 1 场）
UPDATE basket_schedule_rule
SET allocation_mode = 'EVEN_SPLIT_WITH_LATE_EXTRA',
    remark = 'CNPA 正式：按实际 EventTeam 数量动态单循环；比赛日周二/四/六；时间段 21:00/22:00；奇数场次优先晚场',
    update_time = NOW()
WHERE rule_code = 'REGULAR_ROUND_ROBIN'
  AND rule_version = 'V2'
  AND is_delete = 0;
