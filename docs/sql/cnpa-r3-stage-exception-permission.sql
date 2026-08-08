-- CNPA V2 R3: 阶段异常操作权限（判负/弃赛/人工晋级/对阵调整）
SET NAMES utf8mb4;

SET @scheduleMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'basket:v4ScheduleBatch:list' LIMIT 1);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2340, 'V4阶段异常操作', @scheduleMenuId, 17, '', '', 'F', '0', '0', 'basket:v4Stage:exception', '#', 'admin', sysdate(), '判负/系列赛弃赛/人工晋级/对阵调整'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'basket:v4Stage:exception');

-- 赋予 admin 角色（role_id=1 为超级管理员）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE perms = 'basket:v4Stage:exception'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
  WHERE rm.role_id = 1 AND m.perms = 'basket:v4Stage:exception'
);
