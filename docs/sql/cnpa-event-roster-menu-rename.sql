-- CNPA 正式赛事阵容：菜单文案调整（幂等）
UPDATE sys_menu
SET menu_name = '正式赛事阵容',
    update_time = NOW()
WHERE menu_id = 2222
  AND menu_name <> '正式赛事阵容';
