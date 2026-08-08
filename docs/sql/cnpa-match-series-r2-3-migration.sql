-- CNPA V2 MatchSeries R2-3：Qualification 来源 Series 追溯
SET NAMES utf8mb4;

ALTER TABLE basket_division_stage_qualification
  ADD COLUMN source_series_id BIGINT DEFAULT NULL COMMENT '来源系列赛ID（SERIES_WINNER）' AFTER target_stage_id;

ALTER TABLE basket_division_stage_qualification
  ADD KEY idx_qualification_source_series (source_series_id, is_delete);
