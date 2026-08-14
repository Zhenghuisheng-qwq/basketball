-- CNPA V1.3 R8 UI closeout data check (read-only)
SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, visible, status
FROM sys_menu
WHERE menu_id IN (2280, 2301, 2350, 2351, 2311, 2352, 2296, 2306, 2326)
ORDER BY parent_id, order_num, menu_id;

SELECT role_id, menu_id
FROM sys_role_menu
WHERE menu_id IN (2350, 2351, 2352)
ORDER BY role_id, menu_id;
