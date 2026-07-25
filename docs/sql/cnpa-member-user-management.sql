-- CNPA 用户管理：历史会员补建球员档案 + 菜单调整
-- 数据库：basketball

START TRANSACTION;

-- 1. 为已注册但缺少球员档案的会员补档（certification_status=0 表示未认证）
INSERT INTO basket_player_profile (
    member_id, certification_status, player_status, profile_visible, is_delete, create_time, update_time
)
SELECT acc.id, 0, 1, 1, 0, acc.create_time, NOW()
FROM basket_member_account acc
LEFT JOIN basket_player_profile p ON p.member_id = acc.id AND p.is_delete = 0
WHERE acc.is_delete = 0
  AND p.id IS NULL;

-- 2. 「账号与球员管理」改名为「用户管理」，并调整到「账号与认证」第一位
UPDATE sys_menu
SET menu_name = '用户管理',
    order_num = 1,
    route_name = 'BasketMemberUser',
    remark = 'CNPA官网注册用户与球员档案统一管理'
WHERE menu_id = 2210;

UPDATE sys_menu SET order_num = 2 WHERE menu_id = 2001;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 2002;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 2003;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 2211;

COMMIT;
