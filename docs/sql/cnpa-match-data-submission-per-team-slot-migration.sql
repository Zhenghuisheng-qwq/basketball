-- 将比赛数据有效提交槽位从「每场比赛一条」调整为「每场比赛 + 提交方球队一条」
-- MySQL 8.0+，可重复执行前请确认列/索引尚未变更

ALTER TABLE basket_match_data_submission
    DROP INDEX uk_match_active_submission;

ALTER TABLE basket_match_data_submission
    DROP INDEX uk_match_submission_version;

ALTER TABLE basket_match_data_submission
    DROP COLUMN active_submission_slot;

ALTER TABLE basket_match_data_submission
    ADD COLUMN active_submission_slot VARCHAR(128) GENERATED ALWAYS AS (
        CASE
            WHEN is_delete = 0 AND submission_status IN (1, 2)
            THEN CONCAT(CAST(match_id AS CHAR), ':', CAST(submitter_team_id AS CHAR))
            ELSE NULL
        END
    ) STORED COMMENT '当前有效提交槽位（比赛+提交方）';

ALTER TABLE basket_match_data_submission
    ADD UNIQUE KEY uk_match_active_submission (active_submission_slot);

ALTER TABLE basket_match_data_submission
    ADD UNIQUE KEY uk_match_submission_version (match_id, submitter_team_id, version_no, is_delete);
