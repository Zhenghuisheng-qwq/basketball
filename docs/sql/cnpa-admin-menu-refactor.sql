-- CNPA Admin 第二批菜单重构（提升为一级菜单 + component 对齐 cnpa/ 目录）
-- 环境：本地 basketball 库 ONLY
-- 前置：menu_id 2000~2154 已存在

-- ---------------------------------------------------------------------------
-- 1. 新增一级菜单（运营工作台 / 报名与阵容 / 纪律与档案）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component,
  query, route_name, is_frame, is_cache, menu_type, visible, status,
  perms, icon, create_by, create_time, remark
) VALUES
(2201, '运营工作台', 0, 5, 'operations-dashboard', 'cnpa/dashboard/index', '', 'OperationsDashboard', 1, 0, 'C', '0', '0', '', 'dashboard', 'admin', sysdate(), 'CNPA运营工作台'),
(2204, '报名与阵容', 0, 8, 'participation', NULL, '', '', 1, 0, 'M', '0', '0', '', 'form', 'admin', sysdate(), 'CNPA报名与阵容'),
(2207, '纪律与档案', 0, 11, 'discipline', NULL, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), 'CNPA纪律与档案-待第四批页面');

-- ---------------------------------------------------------------------------
-- 2. 原二级目录提升为一级（复用 menu_id，保留角色关联）
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET menu_name='赛事管理', parent_id=0, order_num=6, path='event', component=NULL, icon='star', remark='CNPA赛事管理' WHERE menu_id=2152;
UPDATE sys_menu SET menu_name='账号与认证', parent_id=0, order_num=7, path='identity', component=NULL, icon='peoples', remark='CNPA账号与认证' WHERE menu_id=2151;
UPDATE sys_menu SET menu_name='比赛执行', parent_id=0, order_num=9, path='competition-exec', component=NULL, icon='education', remark='CNPA比赛执行' WHERE menu_id=2153;
UPDATE sys_menu SET menu_name='数据统计', parent_id=0, order_num=10, path='statistics', component=NULL, icon='chart', remark='CNPA数据统计' WHERE menu_id=2154;
UPDATE sys_menu SET menu_name='内容运营', parent_id=0, order_num=12, path='content-ops', component=NULL, icon='international', remark='CNPA内容运营' WHERE menu_id=2101;

-- ---------------------------------------------------------------------------
-- 3. 停用旧总目录「CNPA赛事管理」
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET visible='1', status='1', remark='已停用-第二批提升为一级菜单' WHERE menu_id=2000;
DELETE FROM sys_role_menu WHERE menu_id = 2000;

-- ---------------------------------------------------------------------------
-- 4. 赛事管理（2152）子页面
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET parent_id=2152, order_num=1, menu_name='赛季管理', path='season', component='cnpa/event/season/index' WHERE menu_id=2005;
UPDATE sys_menu SET parent_id=2152, order_num=2, menu_name='赛事列表', path='list', component='cnpa/event/list/index' WHERE menu_id=2004;
UPDATE sys_menu SET parent_id=2152, order_num=3, menu_name='赛事规则', path='rule', component='cnpa/event/rule/index' WHERE menu_id=2006;
UPDATE sys_menu SET parent_id=2152, order_num=4, menu_name='赛事阶段', path='stage', component='cnpa/event/stage/index' WHERE menu_id=2038;
UPDATE sys_menu SET parent_id=2152, order_num=5, menu_name='分组管理', path='group', component='cnpa/event/group/index' WHERE menu_id=2039;

-- ---------------------------------------------------------------------------
-- 5. 账号与认证（2151）子页面 — 报名审核迁至报名与阵容
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET parent_id=2151, order_num=1, menu_name='球员认证审核', path='player-certification', component='cnpa/identity/player-certification/index' WHERE menu_id=2001;
UPDATE sys_menu SET parent_id=2151, order_num=2, menu_name='队长资格审核', path='captain-qualification', component='cnpa/identity/captain-qualification/index' WHERE menu_id=2002;
UPDATE sys_menu SET parent_id=2151, order_num=3, menu_name='球队审核认证', path='team-certification', component='cnpa/identity/team-certification/index' WHERE menu_id=2003;

-- ---------------------------------------------------------------------------
-- 6. 报名与阵容（2204）— 仅已有页面
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET parent_id=2204, order_num=1, menu_name='球队报名审核', path='team-registration', component='cnpa/participation/team-registration/index' WHERE menu_id=2007;

-- ---------------------------------------------------------------------------
-- 7. 比赛执行（2153）
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET parent_id=2153, order_num=1, menu_name='赛程管理', path='schedule', component='cnpa/competition/schedule-manage/index', route_name='BasketScheduleManage', perms='basket:schedule:list' WHERE menu_id=2040;
UPDATE sys_menu SET parent_id=2153, order_num=2, menu_name='比分录入', path='score', component='cnpa/competition/score/index' WHERE menu_id=2042;
UPDATE sys_menu SET parent_id=2153, order_num=3, menu_name='比赛数据录入', path='match-data-entry', component='cnpa/competition/match-data-entry/index' WHERE menu_id=2043;
UPDATE sys_menu SET parent_id=2153, order_num=4, menu_name='比赛名单', path='lineup', component='cnpa/competition/lineup/index', visible='0' WHERE menu_id=2041;

-- ---------------------------------------------------------------------------
-- 8. 数据统计（2154）
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET parent_id=2154, order_num=1, menu_name='数据结算', path='settlement', component='cnpa/statistics/settlement/index' WHERE menu_id=2080;
UPDATE sys_menu SET parent_id=2154, order_num=2, menu_name='积分榜', path='standings', component='cnpa/statistics/standings/index' WHERE menu_id=2081;
UPDATE sys_menu SET parent_id=2154, order_num=3, menu_name='球队赛季统计', path='team-season', component='cnpa/statistics/team-season/index' WHERE menu_id=2082;
UPDATE sys_menu SET parent_id=2154, order_num=4, menu_name='球员赛季统计', path='player-season', component='cnpa/statistics/player-season/index' WHERE menu_id=2083;
UPDATE sys_menu SET parent_id=2154, order_num=5, menu_name='排行榜', path='ranking', component='cnpa/statistics/ranking/index' WHERE menu_id=2084;
UPDATE sys_menu SET parent_id=2154, order_num=6, menu_name='赛季名次', path='season-result', component='cnpa/statistics/season-result/index' WHERE menu_id=2102;

-- ---------------------------------------------------------------------------
-- 9. 内容运营（2101）— 隐藏非目标页（首页内容/合作伙伴）
-- ---------------------------------------------------------------------------
UPDATE sys_menu SET parent_id=2101, order_num=1, menu_name='公告管理', path='announcement', component='cnpa/content/announcement/index', visible='0' WHERE menu_id=2106;
UPDATE sys_menu SET parent_id=2101, order_num=2, menu_name='球队荣誉', path='team-honor', component='cnpa/content/team-honor/index', visible='0' WHERE menu_id=2103;
UPDATE sys_menu SET parent_id=2101, order_num=3, menu_name='球员荣誉', path='player-honor', component='cnpa/content/player-honor/index', visible='0' WHERE menu_id=2104;
UPDATE sys_menu SET parent_id=2101, order_num=4, menu_name='名人堂管理', path='hall-of-fame', component='cnpa/content/hall-of-fame/index', visible='0' WHERE menu_id=2105;
UPDATE sys_menu SET parent_id=2101, order_num=5, menu_name='首页内容', path='home-banner', component='cnpa/content/home-banner/index', visible='1' WHERE menu_id=2107;
UPDATE sys_menu SET parent_id=2101, order_num=6, menu_name='合作伙伴', path='partner', component='cnpa/content/partner/index', visible='1' WHERE menu_id=2108;

-- ---------------------------------------------------------------------------
-- 10. 角色 2 补充新增一级菜单授权
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 2201), (2, 2204), (2, 2207);
