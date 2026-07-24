-- =============================================================================
-- CNPA Admin 菜单 / 动态路由 / 按钮权限（Round 4）
-- 文件：sql/cnpa-admin-menu.sql
-- =============================================================================
-- 执行前提：
--   1. 目标库已初始化若依表（sys_menu / sys_role_menu 等），字段与 ry_20260321.sql 一致
--   2. Admin 与 Web 当前均配置连接同一库时，请先确认库中已有 sys_* 表
--   3. 本脚本不修改若依原始初始化 SQL，不删除、不覆盖其他业务菜单
--
-- 执行顺序：
--   1) 查找或创建一级目录「CNPA赛事管理」
--   2) 查找或创建 7 个页面菜单（component 与 Vue 文件路径严格一致）
--   3) 查找或创建按钮权限（menu_type=F）
--   4) 不默认给普通角色授权（超级管理员 isAdmin 会加载全部菜单）
--
-- 可重复执行：按 parent_id + path / perms 判重后插入
-- =============================================================================

SET NAMES utf8mb4;
SET @cnpa_create_by := 'admin';
SET @cnpa_now := NOW();

-- -----------------------------------------------------------------------------
-- 1. 一级目录：CNPA赛事管理
--    path=basket → 动态路由 /basket
--    component 为空 → 后端 buildMenus 解析为 Layout
-- -----------------------------------------------------------------------------
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    'CNPA赛事管理', 0, 5, 'basket', NULL, '', 'Basket',
    1, 0, 'M', '0', '0', '', 'guide',
    @cnpa_create_by, @cnpa_now, 'CNPA赛事管理目录'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = 0
      AND menu_type = 'M'
      AND (
          path = 'basket'
          OR menu_name IN ('CNPA赛事管理', '篮球赛事', '赛事管理')
      )
    LIMIT 1
);

SELECT menu_id INTO @cnpa_root
FROM sys_menu
WHERE parent_id = 0
  AND menu_type = 'M'
  AND (
      path = 'basket'
      OR menu_name IN ('CNPA赛事管理', '篮球赛事', '赛事管理')
  )
ORDER BY CASE WHEN path = 'basket' THEN 0 WHEN menu_name = 'CNPA赛事管理' THEN 1 ELSE 2 END, menu_id
LIMIT 1;

-- 若复用了历史同名目录，校正 path / route_name / 显示状态，避免动态路由冲突
UPDATE sys_menu
SET path = 'basket',
    route_name = 'Basket',
    component = NULL,
    menu_type = 'M',
    visible = '0',
    status = '0',
    icon = IF(icon IS NULL OR icon = '' OR icon = '#', 'guide', icon),
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @cnpa_root;

-- -----------------------------------------------------------------------------
-- 2. 页面菜单（C）
--    component 必须与 basketball-admin-ui/src/views 下真实文件一致：
--      basket/playerCert/index.vue
--      basket/captainQualification/index.vue
--      basket/teamAudit/index.vue
--      basket/tournament/index.vue
--      basket/season/index.vue
--      basket/tournamentRule/index.vue
--      basket/registrationAudit/index.vue
--    详情均使用抽屉/弹窗，不另建隐藏详情路由
-- -----------------------------------------------------------------------------

-- 2.1 球员认证审核
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '球员认证审核', @cnpa_root, 1, 'player-certification', 'basket/playerCert/index', '', 'BasketPlayerCert',
    1, 0, 'C', '0', '0', 'basket:playerCert:list', 'peoples',
    @cnpa_create_by, @cnpa_now, '球员认证审核页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path IN ('player-certification', 'playerCert')
          OR perms = 'basket:playerCert:list'
          OR component = 'basket/playerCert/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_player_cert
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path IN ('player-certification', 'playerCert')
      OR perms = 'basket:playerCert:list'
      OR component = 'basket/playerCert/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'player-certification',
    component = 'basket/playerCert/index',
    route_name = 'BasketPlayerCert',
    perms = 'basket:playerCert:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_player_cert;

-- 2.2 队长资格管理
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '队长资格管理', @cnpa_root, 2, 'captain-qualification', 'basket/captainQualification/index', '', 'BasketCaptainQualification',
    1, 0, 'C', '0', '0', 'basket:captainQualification:list', 'validCode',
    @cnpa_create_by, @cnpa_now, '队长资格管理页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path IN ('captain-qualification', 'captainQualification')
          OR perms = 'basket:captainQualification:list'
          OR component = 'basket/captainQualification/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_captain
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path IN ('captain-qualification', 'captainQualification')
      OR perms = 'basket:captainQualification:list'
      OR component = 'basket/captainQualification/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'captain-qualification',
    component = 'basket/captainQualification/index',
    route_name = 'BasketCaptainQualification',
    perms = 'basket:captainQualification:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_captain;

-- 2.3 球队审核
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '球队审核', @cnpa_root, 3, 'team-audit', 'basket/teamAudit/index', '', 'BasketTeamAudit',
    1, 0, 'C', '0', '0', 'basket:teamAudit:list', 'tree',
    @cnpa_create_by, @cnpa_now, '球队审核页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path IN ('team-audit', 'teamAudit')
          OR perms = 'basket:teamAudit:list'
          OR component = 'basket/teamAudit/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_team_audit
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path IN ('team-audit', 'teamAudit')
      OR perms = 'basket:teamAudit:list'
      OR component = 'basket/teamAudit/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'team-audit',
    component = 'basket/teamAudit/index',
    route_name = 'BasketTeamAudit',
    perms = 'basket:teamAudit:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_team_audit;

-- 2.4 赛事管理
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '赛事管理', @cnpa_root, 4, 'tournament', 'basket/tournament/index', '', 'BasketTournament',
    1, 0, 'C', '0', '0', 'basket:tournament:list', 'list',
    @cnpa_create_by, @cnpa_now, '赛事管理页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path = 'tournament'
          OR perms = 'basket:tournament:list'
          OR component = 'basket/tournament/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_tournament
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path = 'tournament'
      OR perms = 'basket:tournament:list'
      OR component = 'basket/tournament/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'tournament',
    component = 'basket/tournament/index',
    route_name = 'BasketTournament',
    perms = 'basket:tournament:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_tournament;

-- 2.5 赛季管理
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '赛季管理', @cnpa_root, 5, 'season', 'basket/season/index', '', 'BasketSeason',
    1, 0, 'C', '0', '0', 'basket:season:list', 'date',
    @cnpa_create_by, @cnpa_now, '赛季管理页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path = 'season'
          OR perms = 'basket:season:list'
          OR component = 'basket/season/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_season
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path = 'season'
      OR perms = 'basket:season:list'
      OR component = 'basket/season/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'season',
    component = 'basket/season/index',
    route_name = 'BasketSeason',
    perms = 'basket:season:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_season;

-- 2.6 赛事规则
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '赛事规则', @cnpa_root, 6, 'tournament-rule', 'basket/tournamentRule/index', '', 'BasketTournamentRule',
    1, 0, 'C', '0', '0', 'basket:tournamentRule:list', 'form',
    @cnpa_create_by, @cnpa_now, '赛事规则页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path IN ('tournament-rule', 'tournamentRule')
          OR perms = 'basket:tournamentRule:list'
          OR component = 'basket/tournamentRule/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_rule
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path IN ('tournament-rule', 'tournamentRule')
      OR perms = 'basket:tournamentRule:list'
      OR component = 'basket/tournamentRule/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'tournament-rule',
    component = 'basket/tournamentRule/index',
    route_name = 'BasketTournamentRule',
    perms = 'basket:tournamentRule:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_rule;

-- 2.7 报名审核
INSERT INTO sys_menu (
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
SELECT
    '报名审核', @cnpa_root, 7, 'registration-audit', 'basket/registrationAudit/index', '', 'BasketRegistrationAudit',
    1, 0, 'C', '0', '0', 'basket:registrationAudit:list', 'checkbox',
    @cnpa_create_by, @cnpa_now, '报名审核页面'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @cnpa_root
      AND (
          path IN ('registration-audit', 'registrationAudit')
          OR perms = 'basket:registrationAudit:list'
          OR component = 'basket/registrationAudit/index'
      )
    LIMIT 1
);

SELECT menu_id INTO @m_reg_audit
FROM sys_menu
WHERE parent_id = @cnpa_root
  AND (
      path IN ('registration-audit', 'registrationAudit')
      OR perms = 'basket:registrationAudit:list'
      OR component = 'basket/registrationAudit/index'
  )
ORDER BY menu_id
LIMIT 1;

UPDATE sys_menu
SET path = 'registration-audit',
    component = 'basket/registrationAudit/index',
    route_name = 'BasketRegistrationAudit',
    perms = 'basket:registrationAudit:list',
    menu_type = 'C',
    visible = '0',
    status = '0',
    update_by = @cnpa_create_by,
    update_time = @cnpa_now
WHERE menu_id = @m_reg_audit;

-- -----------------------------------------------------------------------------
-- 3. 按钮权限（F）—— 与 @PreAuthorize / v-hasPermi 完全一致
-- -----------------------------------------------------------------------------

-- 通用：按 parent + perms 判重插入按钮
-- 球员认证
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球员认证查询', @m_player_cert, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:playerCert:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_player_cert AND perms = 'basket:playerCert:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球员认证审核', @m_player_cert, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:playerCert:audit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_player_cert AND perms = 'basket:playerCert:audit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球员认证导出', @m_player_cert, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:playerCert:export', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_player_cert AND perms = 'basket:playerCert:export');

-- 队长资格
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '队长资格查询', @m_captain, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:captainQualification:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_captain AND perms = 'basket:captainQualification:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '队长资格授予', @m_captain, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:captainQualification:grant', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_captain AND perms = 'basket:captainQualification:grant');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '队长资格编辑', @m_captain, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:captainQualification:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_captain AND perms = 'basket:captainQualification:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '队长资格撤销', @m_captain, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:captainQualification:revoke', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_captain AND perms = 'basket:captainQualification:revoke');

-- 球队审核
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球队审核查询', @m_team_audit, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:teamAudit:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_team_audit AND perms = 'basket:teamAudit:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '球队审核操作', @m_team_audit, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:teamAudit:audit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_team_audit AND perms = 'basket:teamAudit:audit');

-- 赛事管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛事查询', @m_tournament, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournament:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_tournament AND perms = 'basket:tournament:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛事新增', @m_tournament, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournament:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_tournament AND perms = 'basket:tournament:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛事编辑', @m_tournament, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournament:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_tournament AND perms = 'basket:tournament:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛事发布', @m_tournament, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournament:publish', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_tournament AND perms = 'basket:tournament:publish');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛事关闭', @m_tournament, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournament:close', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_tournament AND perms = 'basket:tournament:close');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛事删除', @m_tournament, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournament:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_tournament AND perms = 'basket:tournament:remove');

-- 赛季管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季查询', @m_season, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季新增', @m_season, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季编辑', @m_season, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季发布', @m_season, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:publish', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:publish');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '开放报名', @m_season, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:openRegistration', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:openRegistration');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '截止报名', @m_season, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:closeRegistration', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:closeRegistration');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季开始', @m_season, 7, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:start', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:start');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季结束', @m_season, 8, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:finish', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:finish');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '赛季删除', @m_season, 9, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:season:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_season AND perms = 'basket:season:remove');

-- 赛事规则
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则新增', @m_rule, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournamentRule:add', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_rule AND perms = 'basket:tournamentRule:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则编辑', @m_rule, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournamentRule:edit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_rule AND perms = 'basket:tournamentRule:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '规则删除', @m_rule, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:tournamentRule:remove', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_rule AND perms = 'basket:tournamentRule:remove');

-- 报名审核
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名审核查询', @m_reg_audit, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:registrationAudit:query', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_reg_audit AND perms = 'basket:registrationAudit:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名审核操作', @m_reg_audit, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:registrationAudit:audit', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_reg_audit AND perms = 'basket:registrationAudit:audit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报名审核导出', @m_reg_audit, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'basket:registrationAudit:export', '#', @cnpa_create_by, @cnpa_now, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @m_reg_audit AND perms = 'basket:registrationAudit:export');

-- -----------------------------------------------------------------------------
-- 4. 清理历史重复菜单（若曾执行旧版 path=cnpa / camelCase path 的重复插入）
--    仅删除：同父级下重复且非当前规范的页面菜单；按钮随 parent 手工清理风险高，故不级联硬删
-- -----------------------------------------------------------------------------
-- 可选：将旧一级目录 path=cnpa 且无子节点时忽略；有子节点则上面已复用并 UPDATE 为 basket

-- -----------------------------------------------------------------------------
-- 5. 校验查询（执行后人工检查）
-- -----------------------------------------------------------------------------
-- SELECT menu_id, menu_name, parent_id, path, component, route_name, menu_type, perms, visible, status
-- FROM sys_menu
-- WHERE menu_id = @cnpa_root
--    OR parent_id = @cnpa_root
--    OR parent_id IN (@m_player_cert, @m_captain, @m_team_audit, @m_tournament, @m_season, @m_rule, @m_reg_audit)
-- ORDER BY parent_id, order_num, menu_id;

SELECT
    @cnpa_root AS cnpa_root_id,
    @m_player_cert AS player_cert_menu_id,
    @m_captain AS captain_menu_id,
    @m_team_audit AS team_audit_menu_id,
    @m_tournament AS tournament_menu_id,
    @m_season AS season_menu_id,
    @m_rule AS rule_menu_id,
    @m_reg_audit AS registration_audit_menu_id;
