-- CNPA Admin 菜单重构前快照（2026-07-25 第二批正式执行前）
-- 回滚 cnpa-admin-menu-refactor.sql 时使用
-- 环境：本地 basketball 库 ONLY

UPDATE sys_menu SET menu_name='CNPA赛事管理', parent_id=0, order_num=5, path='basket', component=NULL, menu_type='M', visible='0', status='0', perms='', icon='international', remark='CNPA赛事管理目录' WHERE menu_id=2000;

UPDATE sys_menu SET menu_name='准入审核', parent_id=2000, order_num=1, path='audit', component=NULL, menu_type='M', visible='0', status='0', perms='', icon='peoples' WHERE menu_id=2151;
UPDATE sys_menu SET menu_name='赛事组织', parent_id=2000, order_num=2, path='competition', component=NULL, menu_type='M', visible='0', status='0', perms='', icon='star' WHERE menu_id=2152;
UPDATE sys_menu SET menu_name='比赛运营', parent_id=2000, order_num=3, path='match-ops', component=NULL, menu_type='M', visible='0', status='0', perms='', icon='education' WHERE menu_id=2153;
UPDATE sys_menu SET menu_name='数据结算', parent_id=2000, order_num=4, path='settlement', component=NULL, menu_type='M', visible='0', status='0', perms='', icon='chart' WHERE menu_id=2154;
UPDATE sys_menu SET menu_name='荣誉与内容', parent_id=2000, order_num=5, path='content', component=NULL, menu_type='M', visible='0', status='0', perms='', icon='documentation' WHERE menu_id=2101;

UPDATE sys_menu SET menu_name='球员认证审核', parent_id=2151, order_num=1, path='player-certification', component='basket/audit/player-cert/index', menu_type='C', visible='0', perms='basket:playerCert:list' WHERE menu_id=2001;
UPDATE sys_menu SET menu_name='队长资格管理', parent_id=2151, order_num=2, path='captain-qualification', component='basket/audit/captain-qualification/index', menu_type='C', visible='0', perms='basket:captainQualification:list' WHERE menu_id=2002;
UPDATE sys_menu SET menu_name='球队审核', parent_id=2151, order_num=3, path='team-audit', component='basket/audit/team-audit/index', menu_type='C', visible='0', perms='basket:teamAudit:list' WHERE menu_id=2003;
UPDATE sys_menu SET menu_name='报名审核', parent_id=2151, order_num=4, path='registration-audit', component='basket/audit/registration-audit/index', menu_type='C', visible='0', perms='basket:registrationAudit:list' WHERE menu_id=2007;

UPDATE sys_menu SET menu_name='赛事管理', parent_id=2152, order_num=1, path='tournament', component='basket/tournament/manage/index', menu_type='C', visible='0', perms='basket:tournament:list' WHERE menu_id=2004;
UPDATE sys_menu SET menu_name='赛季管理', parent_id=2152, order_num=2, path='season', component='basket/tournament/season/index', menu_type='C', visible='0', perms='basket:season:list' WHERE menu_id=2005;
UPDATE sys_menu SET menu_name='赛事规则', parent_id=2152, order_num=3, path='tournament-rule', component='basket/tournament/rule/index', menu_type='C', visible='0', perms='basket:tournamentRule:list' WHERE menu_id=2006;
UPDATE sys_menu SET menu_name='赛事阶段', parent_id=2152, order_num=4, path='stage', component='basket/tournament/stage/index', menu_type='C', visible='0', perms='basket:stage:list' WHERE menu_id=2038;
UPDATE sys_menu SET menu_name='分组管理', parent_id=2152, order_num=5, path='group', component='basket/tournament/group/index', menu_type='C', visible='0', perms='basket:group:list' WHERE menu_id=2039;

UPDATE sys_menu SET menu_name='赛程管理', parent_id=2153, order_num=1, path='match', component='basket/match/schedule/index', menu_type='C', visible='0', perms='basket:match:list' WHERE menu_id=2040;
UPDATE sys_menu SET menu_name='比赛名单', parent_id=2153, order_num=2, path='match-lineup', component='basket/match/lineup/index', menu_type='C', visible='0', perms='basket:lineup:list' WHERE menu_id=2041;
UPDATE sys_menu SET menu_name='比分录入', parent_id=2153, order_num=3, path='match-score', component='basket/match/score/index', menu_type='C', visible='0', perms='basket:score:query' WHERE menu_id=2042;
UPDATE sys_menu SET menu_name='比赛数据', parent_id=2153, order_num=4, path='match-stat', component='basket/match/stat/index', menu_type='C', visible='0', perms='basket:matchStat:query' WHERE menu_id=2043;

UPDATE sys_menu SET menu_name='数据结算', parent_id=2154, order_num=1, path='data-settlement', component='basket/settlement/data/index', menu_type='C', visible='0', perms='basket:dataSettlement:list' WHERE menu_id=2080;
UPDATE sys_menu SET menu_name='积分榜', parent_id=2154, order_num=2, path='standing', component='basket/settlement/standing/index', menu_type='C', visible='0', perms='basket:standing:list' WHERE menu_id=2081;
UPDATE sys_menu SET menu_name='球队赛季统计', parent_id=2154, order_num=3, path='team-season-stat', component='basket/settlement/team-season-stat/index', menu_type='C', visible='0', perms='basket:teamSeasonStat:list' WHERE menu_id=2082;
UPDATE sys_menu SET menu_name='球员赛季统计', parent_id=2154, order_num=4, path='player-season-stat', component='basket/settlement/player-season-stat/index', menu_type='C', visible='0', perms='basket:playerSeasonStat:list' WHERE menu_id=2083;
UPDATE sys_menu SET menu_name='排行榜', parent_id=2154, order_num=5, path='ranking', component='basket/settlement/ranking/index', menu_type='C', visible='0', perms='basket:ranking:list' WHERE menu_id=2084;
UPDATE sys_menu SET menu_name='赛季名次', parent_id=2154, order_num=6, path='season-result', component='basket/settlement/season-result/index', menu_type='C', visible='0', perms='basket:seasonResult:list' WHERE menu_id=2102;

UPDATE sys_menu SET menu_name='球队荣誉', parent_id=2101, order_num=1, path='team-honor', component='basket/content/team-honor/index', menu_type='C', visible='0', perms='basket:teamHonor:list' WHERE menu_id=2103;
UPDATE sys_menu SET menu_name='球员荣誉', parent_id=2101, order_num=2, path='player-honor', component='basket/content/player-honor/index', menu_type='C', visible='0', perms='basket:playerHonor:list' WHERE menu_id=2104;
UPDATE sys_menu SET menu_name='名人堂', parent_id=2101, order_num=3, path='hall-of-fame', component='basket/content/hall-of-fame/index', menu_type='C', visible='0', perms='basket:hallOfFame:list' WHERE menu_id=2105;
UPDATE sys_menu SET menu_name='公告管理', parent_id=2101, order_num=4, path='announcement', component='basket/content/announcement/index', menu_type='C', visible='0', perms='basket:announcement:list' WHERE menu_id=2106;
UPDATE sys_menu SET menu_name='首页内容', parent_id=2101, order_num=5, path='home-content', component='basket/content/home-banner/index', menu_type='C', visible='0', perms='basket:homeContent:list' WHERE menu_id=2107;
UPDATE sys_menu SET menu_name='合作伙伴', parent_id=2101, order_num=6, path='partner', component='basket/content/partner/index', menu_type='C', visible='0', perms='basket:partner:list' WHERE menu_id=2108;

DELETE FROM sys_role_menu WHERE menu_id IN (2201, 2204, 2207);
DELETE FROM sys_menu WHERE menu_id IN (2201, 2204, 2207);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2000);
