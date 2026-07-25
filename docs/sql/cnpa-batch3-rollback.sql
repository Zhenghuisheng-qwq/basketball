-- CNPA Batch 3 rollback (local dev)
START TRANSACTION;

DROP TABLE IF EXISTS basket_player_platform_change_request;
DROP TABLE IF EXISTS basket_player_platform_account;

ALTER TABLE basket_player_cert_application
    DROP INDEX idx_cert_platform_normalized,
    DROP COLUMN normalized_game_account;

COMMIT;
