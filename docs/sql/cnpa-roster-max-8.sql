-- CNPA 正式参赛阵容：上限 8；最小人数见 cnpa-roster-min-max-5-8.sql
UPDATE basket_tournament_season
SET max_roster_count = 8
WHERE max_roster_count IS NULL OR max_roster_count > 8;
