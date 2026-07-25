# CNPA Admin 菜单与权限说明

> 库：`basketball`（与 `basketball-admin` 数据源一致）  
> 已于 2026-07-24 通过 MCP/脚本执行第五～七轮菜单，并给角色 2 补齐授权。

## 1. 表关联

```text
sys_user (user_id)
   └─ sys_user_role (user_id, role_id)
        └─ sys_role (role_id, role_key)
             └─ sys_role_menu (role_id, menu_id)
                  └─ sys_menu (menu_id, parent_id, path, component, perms, menu_type)
```

权限生效链路：

1. 登录用户 → `sys_user_role` 找角色  
2. 角色 → `sys_role_menu` 找菜单  
3. `/getRouters` 返回 `menu_type in (M,C)` 生成动态路由（`component` → Vue 页面）  
4. 按钮：`menu_type=F` 的 `perms` → 前端 `v-hasPermi` + 后端 `@PreAuthorize`  

超级管理员（`user_id=1`）按若依规则加载全部菜单，可不依赖 `sys_role_menu`。

## 2. SQL 文件与执行顺序

| 顺序 | 文件 | 内容 |
|------|------|------|
| 1 | `sql/cnpa-admin-menu.sql` | 一级目录 + 认证/队长/球队/赛事/赛季/规则/报名 |
| 2 | `sql/cnpa-admin-match-menu.sql` | 阶段/分组/比赛/名单/比分/单场数据 |
| 3 | `sql/cnpa-admin-data-menu.sql` | 结算/积分榜/赛季统计/排行榜 |
| 4 | `sql/cnpa-admin-content-menu.sql` | 二级目录「荣誉与内容」+ 名次/荣誉/名人堂/公告/首页/伙伴 |
| 5 | `sql/cnpa-admin-role-grant-optional.sql` | 可选：把整棵 CNPA 树授给 `@target_role_id` |

汇总入口说明：`sql/cnpa-admin-menu-all.sql`  
本地一键执行：`python sql/_run_cnpa_menus.py`

## 3. 当前库菜单树（页面级）

> **第二批（2026-07-25）** 已重组为五域三级菜单。详见 [`cnpa-admin-route-menu-mapping.md`](./cnpa-admin-route-menu-mapping.md)。

一级目录：`menu_id=2000`，`path=basket`，`menu_name=CNPA赛事管理`

| menu_id | 二级目录 | path | 说明 |
|---------|----------|------|------|
| 2151 | 准入审核 | audit | 4 个审核页 |
| 2152 | 赛事组织 | competition | 赛事/赛季/规则/阶段/分组 |
| 2153 | 比赛运营 | match-ops | 赛程/名单/比分/数据 |
| 2154 | 数据结算 | settlement | 结算/积分/统计/排行/赛季名次 |
| 2101 | 荣誉与内容 | content | 荣誉/名人堂/公告/Banner/伙伴 |

页面 `component` 均已对齐 `views/basket/{域}/.../index.vue`，例如：

- `basket/audit/player-cert/index`
- `basket/tournament/manage/index`
- `basket/match/schedule/index`
- `basket/settlement/data/index`
- `basket/content/announcement/index`

按钮权限为各页下的 `menu_type=F`，`perms` 与 `BasketMatchPerms` / `BasketSettlementPerms` / `BasketContentPerms` 及第四轮脚本一致（约 `2008~2150`）。

## 4. 角色授权现状（执行后）

| role_id | role_name | CNPA 菜单授权 |
|---------|-----------|----------------|
| 1 | 超级管理员 | 不依赖 role_menu（全量） |
| 2 | 普通角色 | 已授权整棵 CNPA 树（含第五～七轮） |

如需改授其他角色：编辑 `cnpa-admin-role-grant-optional.sql` 中的 `@target_role_id` 后重跑。

## 5. 校验 SQL

```sql
-- 页面菜单
SELECT menu_id, menu_name, parent_id, path, component, perms
FROM sys_menu
WHERE menu_type IN ('M','C')
  AND (menu_id = 2000 OR parent_id = 2000
       OR parent_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = 2000 AND menu_type = 'M'))
ORDER BY menu_id;

-- 角色已授 CNPA 菜单数
SELECT r.role_id, r.role_name, COUNT(rm.menu_id) AS cnt
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id AND rm.menu_id BETWEEN 2000 AND 2999
GROUP BY r.role_id, r.role_name;

-- 按钮权限清单
SELECT menu_id, menu_name, parent_id, perms
FROM sys_menu
WHERE menu_type = 'F' AND perms LIKE 'basket:%'
ORDER BY parent_id, menu_id;
```

## 6. 使用注意

1. 执行后请重新登录 Admin，或清 Redis 登录缓存后再调 `/getRouters`。  
2. `component` 必须对应 `basketball-admin-ui/src/views/...` 真实文件。  
3. 不要手工改乱 `parent_id`，否则动态路由父子错乱。  
4. 普通角色若看不到新菜单，重跑 `cnpa-admin-role-grant-optional.sql`。
