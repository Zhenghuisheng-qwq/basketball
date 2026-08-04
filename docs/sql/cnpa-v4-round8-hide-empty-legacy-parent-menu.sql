-- CNPA V4 R8：隐藏无可见子菜单的空父级旧菜单
-- 范围：报名与阵容、数据统计（sys_menu 父级）
-- 说明：阵容变更处理为前端虚拟菜单，需配合 permission.ts 修复；见验收报告

-- ============================================================
-- 1. 确认目标父级菜单
-- ============================================================
SELECT menu_id, menu_name, parent_id, visible, status, component, path
FROM sys_menu
WHERE menu_name IN ('报名与阵容', '阵容变更处理', '数据统计');

-- ============================================================
-- 2. 确认这些父菜单是否还有可见子菜单（visible=0 且 status=0）
-- ============================================================
SELECT p.menu_id AS parent_id,
       p.menu_name AS parent_name,
       p.visible AS parent_visible,
       COUNT(c.menu_id) AS visible_child_count
FROM sys_menu p
LEFT JOIN sys_menu c
  ON c.parent_id = p.menu_id
 AND c.visible = '0'
 AND c.status = '0'
WHERE p.menu_name IN ('报名与阵容', '阵容变更处理', '数据统计')
GROUP BY p.menu_id, p.menu_name, p.visible;

-- ============================================================
-- 3. 隐藏无可见子菜单的空父级（仅当 visible_child_count = 0）
-- ============================================================
UPDATE sys_menu
SET visible = '1',
    remark = CONCAT(IFNULL(remark, ''), ' [V4 R8 hidden empty legacy parent]')
WHERE menu_name IN ('报名与阵容', '阵容变更处理', '数据统计')
  AND menu_id IN (
      SELECT parent_id FROM (
          SELECT p.menu_id AS parent_id
          FROM sys_menu p
          LEFT JOIN sys_menu c
            ON c.parent_id = p.menu_id
           AND c.visible = '0'
           AND c.status = '0'
          WHERE p.menu_name IN ('报名与阵容', '阵容变更处理', '数据统计')
          GROUP BY p.menu_id
          HAVING COUNT(c.menu_id) = 0
      ) t
  );

-- ============================================================
-- 4. 执行后复核：确认新版赛事管理 8 项主菜单仍可见
-- ============================================================
SELECT menu_id, menu_name, parent_id, visible, status, path
FROM sys_menu
WHERE parent_id = 2152
  AND menu_name IN (
      '赛事发布', '队长授权', '报名审核', '参赛队伍',
      '赛程管理', '比赛列表', '数据审核', '统计榜单'
  )
ORDER BY order_num, menu_id;

-- ============================================================
-- 5. 执行后复核：空父级应已 hidden
-- ============================================================
SELECT menu_id, menu_name, visible, remark
FROM sys_menu
WHERE menu_name IN ('报名与阵容', '阵容变更处理', '数据统计');
