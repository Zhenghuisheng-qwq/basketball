-- CNPA Admin menu tree and admin role assignment
SET @cnpaParentId = NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('CNPA赛事管理', 0, 5, 'basket', NULL, '', '', 1, 0, 'M', '0', '0', '', 'international', 'admin', sysdate(), 'CNPA赛事管理目录');
SET @cnpaParentId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
('球员认证审核', @cnpaParentId, 1, 'playerCert', 'basket/playerCert/index', '', '', 1, 0, 'C', '0', '0', 'basket:playerCert:list', 'user', 'admin', sysdate(), ''),
('队长资格管理', @cnpaParentId, 2, 'captainQualification', 'basket/captainQualification/index', '', '', 1, 0, 'C', '0', '0', 'basket:captainQualification:list', 'peoples', 'admin', sysdate(), ''),
('球队审核', @cnpaParentId, 3, 'teamAudit', 'basket/teamAudit/index', '', '', 1, 0, 'C', '0', '0', 'basket:teamAudit:list', 'tree', 'admin', sysdate(), ''),
('赛事管理', @cnpaParentId, 4, 'tournament', 'basket/tournament/index', '', '', 1, 0, 'C', '0', '0', 'basket:tournament:list', 'star', 'admin', sysdate(), ''),
('赛季管理', @cnpaParentId, 5, 'season', 'basket/season/index', '', '', 1, 0, 'C', '0', '0', 'basket:season:list', 'date', 'admin', sysdate(), ''),
('赛事规则', @cnpaParentId, 6, 'tournamentRule', 'basket/tournamentRule/index', '', '', 1, 0, 'C', '0', '0', 'basket:tournamentRule:list', 'documentation', 'admin', sysdate(), ''),
('报名审核', @cnpaParentId, 7, 'registrationAudit', 'basket/registrationAudit/index', '', '', 1, 0, 'C', '0', '0', 'basket:registrationAudit:list', 'form', 'admin', sysdate(), '');

-- player cert buttons
SET @playerCertMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:playerCert:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('球员认证查询', @playerCertMenuId, 1, '', '', 'F', '0', '0', 'basket:playerCert:query', '#', 'admin', sysdate()),
('球员认证审核', @playerCertMenuId, 2, '', '', 'F', '0', '0', 'basket:playerCert:audit', '#', 'admin', sysdate()),
('球员认证导出', @playerCertMenuId, 3, '', '', 'F', '0', '0', 'basket:playerCert:export', '#', 'admin', sysdate());

-- captain qualification buttons
SET @captainMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:captainQualification:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('队长资格查询', @captainMenuId, 1, '', '', 'F', '0', '0', 'basket:captainQualification:query', '#', 'admin', sysdate()),
('队长资格授予', @captainMenuId, 2, '', '', 'F', '0', '0', 'basket:captainQualification:grant', '#', 'admin', sysdate()),
('队长资格编辑', @captainMenuId, 3, '', '', 'F', '0', '0', 'basket:captainQualification:edit', '#', 'admin', sysdate()),
('队长资格撤销', @captainMenuId, 4, '', '', 'F', '0', '0', 'basket:captainQualification:revoke', '#', 'admin', sysdate());

-- team audit buttons
SET @teamAuditMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:teamAudit:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('球队审核查询', @teamAuditMenuId, 1, '', '', 'F', '0', '0', 'basket:teamAudit:query', '#', 'admin', sysdate()),
('球队审核操作', @teamAuditMenuId, 2, '', '', 'F', '0', '0', 'basket:teamAudit:audit', '#', 'admin', sysdate());

-- tournament buttons
SET @tournamentMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:tournament:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('赛事查询', @tournamentMenuId, 1, '', '', 'F', '0', '0', 'basket:tournament:query', '#', 'admin', sysdate()),
('赛事新增', @tournamentMenuId, 2, '', '', 'F', '0', '0', 'basket:tournament:add', '#', 'admin', sysdate()),
('赛事编辑', @tournamentMenuId, 3, '', '', 'F', '0', '0', 'basket:tournament:edit', '#', 'admin', sysdate()),
('赛事发布', @tournamentMenuId, 4, '', '', 'F', '0', '0', 'basket:tournament:publish', '#', 'admin', sysdate()),
('赛事关闭', @tournamentMenuId, 5, '', '', 'F', '0', '0', 'basket:tournament:close', '#', 'admin', sysdate()),
('赛事删除', @tournamentMenuId, 6, '', '', 'F', '0', '0', 'basket:tournament:remove', '#', 'admin', sysdate());

-- season buttons
SET @seasonMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:season:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('赛季查询', @seasonMenuId, 1, '', '', 'F', '0', '0', 'basket:season:query', '#', 'admin', sysdate()),
('赛季新增', @seasonMenuId, 2, '', '', 'F', '0', '0', 'basket:season:add', '#', 'admin', sysdate()),
('赛季编辑', @seasonMenuId, 3, '', '', 'F', '0', '0', 'basket:season:edit', '#', 'admin', sysdate()),
('赛季发布', @seasonMenuId, 4, '', '', 'F', '0', '0', 'basket:season:publish', '#', 'admin', sysdate()),
('开启报名', @seasonMenuId, 5, '', '', 'F', '0', '0', 'basket:season:openRegistration', '#', 'admin', sysdate()),
('截止报名', @seasonMenuId, 6, '', '', 'F', '0', '0', 'basket:season:closeRegistration', '#', 'admin', sysdate()),
('赛季开始', @seasonMenuId, 7, '', '', 'F', '0', '0', 'basket:season:start', '#', 'admin', sysdate()),
('赛季结束', @seasonMenuId, 8, '', '', 'F', '0', '0', 'basket:season:finish', '#', 'admin', sysdate()),
('赛季删除', @seasonMenuId, 9, '', '', 'F', '0', '0', 'basket:season:remove', '#', 'admin', sysdate());

-- rule buttons
SET @ruleMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:tournamentRule:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('规则查询', @ruleMenuId, 1, '', '', 'F', '0', '0', 'basket:tournamentRule:list', '#', 'admin', sysdate()),
('规则新增', @ruleMenuId, 2, '', '', 'F', '0', '0', 'basket:tournamentRule:add', '#', 'admin', sysdate()),
('规则编辑', @ruleMenuId, 3, '', '', 'F', '0', '0', 'basket:tournamentRule:edit', '#', 'admin', sysdate()),
('规则删除', @ruleMenuId, 4, '', '', 'F', '0', '0', 'basket:tournamentRule:remove', '#', 'admin', sysdate());

-- registration audit buttons
SET @regMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:registrationAudit:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('报名审核查询', @regMenuId, 1, '', '', 'F', '0', '0', 'basket:registrationAudit:query', '#', 'admin', sysdate()),
('报名审核操作', @regMenuId, 2, '', '', 'F', '0', '0', 'basket:registrationAudit:audit', '#', 'admin', sysdate()),
('报名审核导出', @regMenuId, 3, '', '', 'F', '0', '0', 'basket:registrationAudit:export', '#', 'admin', sysdate());

-- assign all CNPA menus to admin role
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id = @cnpaParentId
   OR parent_id = @cnpaParentId
   OR parent_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = @cnpaParentId);
