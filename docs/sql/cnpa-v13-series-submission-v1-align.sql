-- CNPA Series Submission V1.0 align: FULL_MATCH -> WINNER_FULL_MATCH
SET NAMES utf8mb4;
UPDATE basket_match_submission
SET submission_mode = 'WINNER_FULL_MATCH'
WHERE submission_mode = 'FULL_MATCH';
