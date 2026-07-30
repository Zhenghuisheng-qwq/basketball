-- 修正联调测试数据 ID 精度（Snowflake ID 必须精确，不可在 JS/JSON 中当作 Number）
-- RNG 队长 member_id=2081010979604316162 (111@qq.com)
-- PHX 队长 member_id=2081019227929698305 (1111111111@qq.com)
-- 材料 file_id=2081011198194663426

UPDATE basket_tournament_registration
SET captain_member_id = 2081010979604316162,
    captain_player_id = 2081010979671425026,
    update_time = NOW()
WHERE id = 900040001;

UPDATE basket_tournament_registration
SET captain_member_id = 2081019227929698305,
    captain_player_id = 2081019227996807169,
    update_time = NOW()
WHERE id = 900040002;

UPDATE basket_team
SET captain_member_id = 2081010979604316162,
    captain_player_id = 2081010979671425026,
    update_time = NOW()
WHERE id = 900010001;

UPDATE basket_team
SET captain_member_id = 2081019227929698305,
    captain_player_id = 2081019227996807169,
    update_time = NOW()
WHERE id = 900010002;

UPDATE basket_match_roster SET player_id = 2081010979671425026 WHERE id = 900070001;
UPDATE basket_match_roster SET player_id = 2081041427005038596 WHERE id = 900070002;
UPDATE basket_match_roster SET player_id = 2081072741716336642 WHERE id = 900070003;
UPDATE basket_match_roster SET player_id = 2081073127508418562 WHERE id = 900070004;
UPDATE basket_match_roster SET player_id = 2081073380093599747 WHERE id = 900070005;
UPDATE basket_match_roster SET player_id = 2081019227996807169 WHERE id = 900070006;
UPDATE basket_match_roster SET player_id = 2082478335308455940 WHERE id = 900070007;
UPDATE basket_match_roster SET player_id = 2082786571525836803 WHERE id = 900070008;

UPDATE basket_file_asset
SET owner_member_id = 2081010979604316162,
    usage_type = 'MATCH_DATA_SCREENSHOT',
    update_time = NOW()
WHERE id = 2081011198194663426 AND is_delete = 0;
