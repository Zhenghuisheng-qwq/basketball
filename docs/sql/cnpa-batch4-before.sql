-- CNPA Batch 4 before snapshot
SELECT COUNT(*) AS registrations FROM basket_tournament_registration WHERE is_delete=0;
SELECT registration_status, COUNT(*) c FROM basket_tournament_registration WHERE is_delete=0 GROUP BY registration_status;
SELECT COUNT(*) AS team_season_profiles FROM basket_team_season_profile WHERE is_delete=0;
SELECT COUNT(*) AS reg_players FROM basket_tournament_registration_player WHERE is_delete=0;
