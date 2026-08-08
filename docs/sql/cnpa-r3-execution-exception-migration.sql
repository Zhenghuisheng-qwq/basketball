-- CNPA V2 R3-1: 执行异常（轮空 / 弃赛 / 人工晋级）最小扩展
SET NAMES utf8mb4;

-- Series 完成原因（status=COMPLETED 时区分正常/弃赛/人工）
ALTER TABLE basket_match_series
    ADD COLUMN resolution_type VARCHAR(32) DEFAULT NULL COMMENT 'NORMAL/FORFEIT/MANUAL' AFTER series_status,
    ADD COLUMN resolution_reason VARCHAR(500) DEFAULT NULL COMMENT '异常判定原因' AFTER resolution_type,
    ADD COLUMN resolved_time DATETIME DEFAULT NULL COMMENT '异常判定时间' AFTER resolution_reason,
    ADD COLUMN resolved_by BIGINT DEFAULT NULL COMMENT '异常判定操作人' AFTER resolved_time;

-- 人工晋级备注（MANUAL / BYE 运营说明）
ALTER TABLE basket_division_stage_qualification
    ADD COLUMN remark VARCHAR(512) DEFAULT NULL COMMENT '晋级备注/原因' AFTER confirmed_time;
