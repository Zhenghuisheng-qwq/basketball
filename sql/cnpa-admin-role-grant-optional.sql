-- =============================================================================
-- CNPA Admin 可选角色授权 SQL（非默认执行）
-- 文件：sql/cnpa-admin-role-grant-optional.sql
-- =============================================================================
-- 说明：
--   1. 主菜单脚本 sql/cnpa-admin-menu.sql 不会给普通角色自动授权
--   2. 超级管理员（SecurityUtils.isAdmin / user_id=1）会加载全部菜单，无需本脚本
--   3. 运营角色请优先在「系统管理 → 角色管理」中勾选：
--        CNPA赛事管理
--        → 球员认证审核 / 队长资格管理 / 球队审核 / 赛事管理 / 赛季管理 / 赛事规则 / 报名审核
--        → 以及对应按钮权限
--   4. 仅当需要脚本化授权某个角色时，再执行本文件
--
-- 使用前请修改：
--   SET @target_role_id := <目标角色ID>;
-- =============================================================================

SET NAMES utf8mb4;

-- >>> 修改为目标运营角色 ID（不要填 1 以外的全部角色）
SET @target_role_id := 2;

-- 授权 CNPA 一级目录及其全部子菜单、按钮
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @target_role_id, m.menu_id
FROM sys_menu m
WHERE (
        m.path = 'basket' AND m.parent_id = 0 AND m.menu_type = 'M'
     OR m.parent_id IN (
            SELECT menu_id FROM sys_menu
            WHERE parent_id = 0 AND menu_type = 'M' AND path = 'basket'
        )
     OR m.parent_id IN (
            SELECT c.menu_id
            FROM sys_menu c
            WHERE c.parent_id IN (
                SELECT menu_id FROM sys_menu
                WHERE parent_id = 0 AND menu_type = 'M' AND path = 'basket'
            )
              AND c.menu_type = 'C'
        )
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = @target_role_id AND rm.menu_id = m.menu_id
);
