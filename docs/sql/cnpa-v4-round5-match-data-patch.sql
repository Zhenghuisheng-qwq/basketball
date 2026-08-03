-- CNPA V4 Round5: basket_match_submission 补 home_score / away_score / submit_remark
-- R5 P0 只做总比分 + 球队/球员统计，分节比分后置（不建 basket_match_submission_period_score）

-- 如列已存在，执行时会报错，可忽略重复列错误
ALTER TABLE `basket_match_submission`
  ADD COLUMN `home_score` INT DEFAULT NULL COMMENT '提交总比分-主队' AFTER `version_no`;

ALTER TABLE `basket_match_submission`
  ADD COLUMN `away_score` INT DEFAULT NULL COMMENT '提交总比分-客队' AFTER `home_score`;

ALTER TABLE `basket_match_submission`
  ADD COLUMN `submit_remark` VARCHAR(512) DEFAULT NULL COMMENT '提交备注' AFTER `reject_reason`;
