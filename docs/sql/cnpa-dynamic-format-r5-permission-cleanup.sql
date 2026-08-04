-- =============================================================================
-- CNPA 动态赛制重构 R5：菜单与权限物理清理
-- 前置：cnpa-dynamic-format-r5-cleanup.sql 步骤 1-2 或独立执行
-- =============================================================================
SET NAMES utf8mb4;

-- 删除旧域按钮权限
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE perms LIKE 'basket:tournament:%'
       OR perms LIKE 'basket:season:%'
       OR perms LIKE 'basket:stage:%'
       OR perms LIKE 'basket:group:%'
       OR perms LIKE 'basket:tournamentRule:%'
       OR perms LIKE 'basket:match:%'
       OR perms LIKE 'basket:lineup:%'
       OR perms LIKE 'basket:score:%'
       OR perms LIKE 'basket:matchStat:%'
       OR perms LIKE 'basket:standing:%'
       OR perms LIKE 'basket:ranking:%'
       OR perms LIKE 'basket:dataSettlement:%'
       OR perms LIKE 'basket:registrationAudit:%'
       OR perms LIKE 'basket:eventTeam:%'
       OR perms LIKE 'basket:seasonRoster:%'
       OR perms LIKE 'basket:rosterChange:%'
       OR perms LIKE 'basket:rosterOperation:%'
       OR perms LIKE 'basket:matchDispute:%'
       OR perms LIKE 'basket:matchException:%'
       OR perms LIKE 'basket:captainQualification:%'
   OR perms = 'basket:v4ScheduleBatch:generate'
  ) t
);

DELETE FROM sys_menu
WHERE perms LIKE 'basket:tournament:%'
   OR perms LIKE 'basket:season:%'
   OR perms LIKE 'basket:stage:%'
   OR perms LIKE 'basket:group:%'
   OR perms LIKE 'basket:tournamentRule:%'
   OR perms LIKE 'basket:match:%'
   OR perms LIKE 'basket:lineup:%'
   OR perms LIKE 'basket:score:%'
   OR perms LIKE 'basket:matchStat:%'
   OR perms LIKE 'basket:standing:%'
   OR perms LIKE 'basket:ranking:%'
   OR perms LIKE 'basket:dataSettlement:%'
   OR perms LIKE 'basket:registrationAudit:%'
   OR perms LIKE 'basket:eventTeam:%'
   OR perms LIKE 'basket:seasonRoster:%'
   OR perms LIKE 'basket:rosterChange:%'
   OR perms LIKE 'basket:rosterOperation:%'
   OR perms LIKE 'basket:matchDispute:%'
   OR perms LIKE 'basket:matchException:%'
   OR perms LIKE 'basket:captainQualification:%'
   OR perms = 'basket:v4ScheduleBatch:generate'
   OR perms = 'basket:v4ScheduleBatch:generate';

-- 确保 V4 主线菜单可见
UPDATE sys_menu SET visible='0', status='0'
WHERE menu_id IN (2280,2296,2301,2306,2311,2316,2321,2326);

-- 确保 admin 角色拥有 V4 权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id BETWEEN 2280 AND 2339
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=1 AND rm.menu_id=m.menu_id);

SELECT 'permission_cleanup_done' AS step;
