-- CNPA 第一期：赛事阵容人数上限调整为 8（5 首发 + 3 替补）
UPDATE basket_tournament_season
SET max_roster_count = 8
WHERE max_roster_count IS NULL OR max_roster_count > 8;

UPDATE basket_tournament_season
SET min_roster_count = 8
WHERE min_roster_count IS NULL OR min_roster_count < 8;
