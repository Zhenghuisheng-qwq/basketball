-- CNPA V1.3 R10: V4 EventTeam standing point adjustment (additive).
-- Reuses basket_point_adjustment. Legacy team_id rows stay unused by V4 Standing.
SET NAMES utf8mb4;

ALTER TABLE `basket_point_adjustment`
  MODIFY COLUMN `team_id` bigint DEFAULT NULL COMMENT 'V4 EventTeam adjustment may leave team_id null';

ALTER TABLE `basket_point_adjustment`
  ADD COLUMN `division_id` bigint DEFAULT NULL COMMENT 'V4 Division' AFTER `season_id`;

ALTER TABLE `basket_point_adjustment`
  ADD COLUMN `event_team_id` bigint DEFAULT NULL COMMENT 'V4 EventTeam' AFTER `team_id`;

ALTER TABLE `basket_point_adjustment`
  ADD COLUMN `adjustment_type` varchar(32) DEFAULT NULL COMMENT 'ADMIN_MANUAL; NULL=legacy discipline' AFTER `discipline_id`;

ALTER TABLE `basket_point_adjustment`
  ADD COLUMN `remark` varchar(512) DEFAULT NULL COMMENT 'remark' AFTER `adjustment_reason`;

ALTER TABLE `basket_point_adjustment`
  ADD COLUMN `operator_name` varchar(64) DEFAULT NULL COMMENT 'operator name' AFTER `operator_id`;

ALTER TABLE `basket_point_adjustment`
  ADD KEY `idx_point_adjust_v4` (`division_id`, `stage_id`, `event_team_id`, `is_delete`);
