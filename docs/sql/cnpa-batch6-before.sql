-- CNPA Batch6 baseline counts
SELECT 'discipline_record' AS item, COUNT(*) AS cnt FROM basket_discipline_record WHERE is_delete=0;
SELECT 'announcement' AS item, COUNT(*) AS cnt FROM basket_announcement WHERE is_delete=0;
SELECT 'honor_record' AS item, COUNT(*) AS cnt FROM basket_honor_record WHERE is_delete=0;
SELECT 'team_captain_history' AS item, COUNT(*) AS cnt FROM basket_team_captain_history WHERE is_delete=0;
SELECT 'player_profile' AS item, COUNT(*) AS cnt FROM basket_player_profile WHERE is_delete=0;
SELECT 'team' AS item, COUNT(*) AS cnt FROM basket_team WHERE is_delete=0;
SELECT 'official_result' AS item, COUNT(*) AS cnt FROM basket_match_official_result WHERE is_current=1 AND is_delete=0;
