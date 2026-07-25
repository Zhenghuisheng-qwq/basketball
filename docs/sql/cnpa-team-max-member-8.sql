-- CNPA 第一期：球队最大成员数调整为 8（5 首发 + 3 替补）
UPDATE basket_team SET max_member_count = 8 WHERE max_member_count IS NULL OR max_member_count > 8;
