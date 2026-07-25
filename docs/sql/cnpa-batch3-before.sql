-- CNPA Batch 3 snapshot markers (run before migration)
-- Database: basketball @ local dev
-- Generated: 2026-07-25

SELECT 'basket_member_account' AS tbl, COUNT(*) AS cnt FROM basket_member_account;
SELECT 'basket_player_profile' AS tbl, COUNT(*) AS cnt FROM basket_player_profile;
SELECT 'basket_player_cert_application' AS tbl, COUNT(*) AS cnt FROM basket_player_cert_application;
SELECT 'basket_captain_qualification' AS tbl, COUNT(*) AS cnt FROM basket_captain_qualification;
SELECT 'basket_team' AS tbl, COUNT(*) AS cnt FROM basket_team;

-- Tables that batch3 will create (should not exist yet)
SELECT COUNT(*) AS platform_account_exists
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'basket_player_platform_account';

SELECT COUNT(*) AS platform_change_exists
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'basket_player_platform_change_request';
