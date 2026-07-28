-- CNPA 队长授权管理：菜单文案调整（幂等）
UPDATE sys_menu
SET menu_name = '队长授权管理',
    update_time = NOW()
WHERE menu_id = 2002
  AND menu_name <> '队长授权管理';
