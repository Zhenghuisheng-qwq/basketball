-- CNPA V1.3 mixed navigation closeout. sys_menu display only. No business tables.
-- RuoYi: visible=0 show, visible=1 hide but route/permission remain. Do not set status=1.

UPDATE sys_menu SET menu_name = '赛季管理', order_num = 1, visible = '0', status = '0',
  remark = 'Series/Season/Division foundation'
WHERE menu_id = 2353;

UPDATE sys_menu SET menu_name = '赛事工作台', order_num = 2, visible = '0', status = '0',
  remark = 'Workspace Home / Overview'
WHERE menu_id = 2350;

UPDATE sys_menu SET menu_name = '报名与参赛名单', order_num = 3, visible = '0', status = '0'
WHERE menu_id = 2301;

UPDATE sys_menu SET menu_name = '队长授权', order_num = 4, visible = '0', status = '0'
WHERE menu_id = 2296;

UPDATE sys_menu SET menu_name = '参赛队伍', order_num = 5, visible = '0', status = '0'
WHERE menu_id = 2306;

UPDATE sys_menu SET menu_name = '统计榜单', order_num = 6, visible = '0', status = '0'
WHERE menu_id = 2326;

UPDATE sys_menu SET menu_name = '赛事规则', order_num = 11, visible = '1', status = '0',
  remark = 'Workspace Tab rule; hidden sidebar'
WHERE menu_id = 2280;

UPDATE sys_menu SET menu_name = '赛程结构', order_num = 12, visible = '1', status = '0',
  remark = 'Workspace Tab structure; hidden sidebar'
WHERE menu_id = 2351;

UPDATE sys_menu SET menu_name = '日期与时间', order_num = 13, visible = '1', status = '0',
  remark = 'Workspace Tab schedule; hidden sidebar'
WHERE menu_id = 2311;

UPDATE sys_menu SET menu_name = '赛程发布', order_num = 14, visible = '1', status = '0',
  remark = 'Workspace Tab publish; hidden sidebar'
WHERE menu_id = 2352;
