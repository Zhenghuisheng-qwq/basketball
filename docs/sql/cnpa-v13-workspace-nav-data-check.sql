-- CNPA V1.3 workspace nav data-check (read-only)

SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, visible, status
FROM sys_menu
WHERE parent_id = 2152 AND menu_type = 'C'
ORDER BY order_num, menu_id;

SELECT
  SUM(CASE WHEN visible = '0' AND menu_id IN (2353, 2350, 2301, 2296, 2306, 2326) THEN 1 ELSE 0 END) AS visible_count,
  SUM(CASE WHEN visible = '1' AND menu_id IN (2280, 2351, 2311, 2352) THEN 1 ELSE 0 END) AS hidden_count,
  SUM(CASE WHEN status = '0' AND menu_id IN (2353, 2350, 2301, 2296, 2306, 2326, 2280, 2351, 2311, 2352) THEN 1 ELSE 0 END) AS enabled_count
FROM sys_menu
WHERE menu_id IN (2353, 2350, 2301, 2296, 2306, 2326, 2280, 2351, 2311, 2352);

SELECT menu_id, menu_name, parent_id, menu_type, perms, visible, status
FROM sys_menu
WHERE parent_id IN (2280, 2351, 2311, 2352)
ORDER BY parent_id, menu_id;

SELECT role_id, menu_id
FROM sys_role_menu
WHERE menu_id IN (2353, 2350, 2301, 2296, 2306, 2326, 2280, 2351, 2311, 2352)
ORDER BY role_id, menu_id;
