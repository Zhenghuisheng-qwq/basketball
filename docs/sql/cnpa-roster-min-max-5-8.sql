-- CNPA 正式参赛阵容：最少 5 人，最多 8 人
-- 幂等：可重复执行

UPDATE basket_tournament_season
SET max_roster_count = 8
WHERE max_roster_count IS NULL OR max_roster_count > 8;

UPDATE basket_tournament_season
SET min_roster_count = 5
WHERE min_roster_count IS NULL OR min_roster_count > 8 OR min_roster_count < 5
   OR (min_roster_count = max_roster_count AND min_roster_count >= 8);

UPDATE basket_tournament_season
SET min_roster_count = max_roster_count
WHERE min_roster_count > max_roster_count;
