-- CNPA V4 Round5: 比赛数据审核 / 统计榜单 菜单
SET @eventParentId = 2152;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2321, '数据审核', @eventParentId, 6, 'event-v4-match-submission-audit', 'cnpa/event-v4/match-submission-audit/index', 'CnpaEventV4MatchSubmissionAudit', 1, 0, 'C', '0', '0', 'basket:v4MatchSubmissionAudit:list', 'edit', 'admin', sysdate(), 'CNPA V4 比赛数据 submission 级审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2321 OR component = 'cnpa/event-v4/match-submission-audit/index');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2326, '统计榜单', @eventParentId, 7, 'event-v4-stats', 'cnpa/event-v4/stats/index', 'CnpaEventV4Stats', 1, 0, 'C', '0', '0', 'basket:v4Stats:query', 'chart', 'admin', sysdate(), 'CNPA V4 Standing/TeamStat/PlayerStat/Rank'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2326 OR component = 'cnpa/event-v4/stats/index');

SET @v4AuditMenuId = 2321;
SET @v4StatsMenuId = 2326;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2322, 'V4 MatchSubmissionAudit查询', @v4AuditMenuId, 1, '', '', 'F', '0', '0', 'basket:v4MatchSubmissionAudit:query', '#', 'admin', sysdate()),
(2323, 'V4 MatchSubmissionAudit通过', @v4AuditMenuId, 2, '', '', 'F', '0', '0', 'basket:v4MatchSubmissionAudit:approve', '#', 'admin', sysdate()),
(2324, 'V4 MatchSubmissionAudit驳回', @v4AuditMenuId, 3, '', '', 'F', '0', '0', 'basket:v4MatchSubmissionAudit:reject', '#', 'admin', sysdate()),
(2325, 'V4 Stats查询', @v4StatsMenuId, 1, '', '', 'F', '0', '0', 'basket:v4Stats:query', '#', 'admin', sysdate())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 隐藏旧统计 / 结算入口（R5）
UPDATE sys_menu SET visible = '1', remark = CONCAT(IFNULL(remark, ''), ' [V4 R5 deprecated]')
WHERE menu_id IN (2042, 2044, 2045, 2046, 2047, 2048, 2233, 2234)
   OR component IN (
       'cnpa/competition/data-settlement/index',
       'cnpa/competition/standing/index',
       'cnpa/competition/ranking/index',
       'cnpa/competition/team-season-stat/index',
       'cnpa/competition/player-season-stat/index',
       'cnpa/competition/match-stat/index'
   );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id BETWEEN 2321 AND 2326
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
