-- =============================================================================
-- CNPA V2 最终权威菜单与权限（赛事域）
-- 基于 cnpa-v4-round11-menu-final.sql + cnpa-dynamic-format-r3/r4-permission.sql 收口
-- 不含旧 Tournament/Standing/Registration 权限
-- =============================================================================
SET NAMES utf8mb4;

-- 执行完整菜单对齐（幂等）
SOURCE docs/sql/cnpa-v4-round11-menu-final.sql;

-- R3/R4 动态赛制按钮权限
SOURCE docs/sql/cnpa-dynamic-format-r3-permission.sql;
SOURCE docs/sql/cnpa-dynamic-format-r4-permission.sql;

-- R5 物理删除旧域权限（若仍存在）
SOURCE docs/sql/cnpa-dynamic-format-r5-permission-cleanup.sql;

-- 最终 V4 权限清单（参考，不含旧域）：
-- basket:v4Series:*, basket:v4Season:*, basket:v4Division:*
-- basket:v4FormatRule:*, basket:v4CaptainGrant:*
-- basket:v4RegistrationAudit:*, basket:v4PersonalFormation:*
-- basket:v4EventTeam:*, basket:v4EventPlayer:*
-- basket:v4Grouping:*, basket:v4ScheduleBatch:*
-- basket:v4Match:*, basket:v4MatchSubmissionAudit:*
-- basket:v4Stats:*, basket:v4Standing:*, basket:v4Stage:*
