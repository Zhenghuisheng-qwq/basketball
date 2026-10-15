-- 队长授权到期策略。历史行保持 NULL，表示尚未确认，不会随赛事时间自动改期。
-- 可重复执行。不改已有 valid_to。
-- 回滚：ALTER TABLE basket_captain_grant DROP COLUMN valid_to_mode;
--       DELETE FROM sys_role_menu WHERE menu_id = 2380;
--       DELETE FROM sys_menu WHERE menu_id = 2380;

SET @valid_to_mode_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'basket_captain_grant'
      AND COLUMN_NAME = 'valid_to_mode'
);
SET @valid_to_mode_sql = IF(
    @valid_to_mode_exists = 0,
    'ALTER TABLE `basket_captain_grant` ADD COLUMN `valid_to_mode` VARCHAR(32) DEFAULT NULL COMMENT ''到期策略：AUTO_EVENT跟随赛事 / CUSTOM手动指定；NULL为历史未确认'' AFTER `valid_to`',
    'SELECT 1'
);
PREPARE valid_to_mode_stmt FROM @valid_to_mode_sql;
EXECUTE valid_to_mode_stmt;
DEALLOCATE PREPARE valid_to_mode_stmt;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2380, 'V4 CaptainGrant调整有效期', 2296, 4, '', '', 'F', '0', '0', 'basket:v4CaptainGrant:edit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2380 OR perms = 'basket:v4CaptainGrant:edit');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2380 FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2380)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2380);
