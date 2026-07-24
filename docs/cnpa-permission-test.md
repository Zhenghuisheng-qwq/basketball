# CNPA Admin 权限专项测试

> 三处必须一致：`sys_menu.perms` / `@PreAuthorize` / 前端 `v-hasPermi`

## 1. 角色准备（建议）

| 角色 | 用途 |
|------|------|
| 超级管理员 | 全权限基准 |
| 赛事运营 | 赛事/赛季/规则/阶段/赛程 |
| 审核人员 | 认证/球队/报名审核 |
| 数据录入 | 比分/名单/单场统计 |
| 只读 | 仅查询 |
| 无 CNPA 权限 | 负向对照 |

菜单 SQL：`sql/cnpa-admin-menu.sql`、`sql/cnpa-admin-match-menu.sql`、`sql/cnpa-admin-data-menu.sql`、`sql/cnpa-admin-content-menu.sql`  
可选授权：`sql/cnpa-admin-role-grant-optional.sql`  
**说明：** `sys_menu` 在若依库，业务库 `basketball` 不含菜单表；本环境若未在若依库执行菜单 SQL，则 `/getRouters` **标记为未验证**。

## 2. 用例矩阵

| # | 用例 | 预期 | 本轮结果 |
|---|------|------|----------|
| 1 | 菜单按权限显示 | 无权限不出现 | 菜单 SQL 未在若依库验证 → 未验证 |
| 2 | 按钮 `v-hasPermi` | 无权限不显示 | 静态代码存在；运行时未验证 |
| 3 | `/getRouters` 过滤 | 不含无权限路由 | 未验证 |
| 4 | 无权限直接调接口 | HTTP 403 | 代码层 `@PreAuthorize` 存在；未实测 |
| 5 | 审核人员改赛事规则 | 403 | 未验证 |
| 6 | 数据录入授予队长 | 403 | 未验证 |
| 7 | 只读改状态 | 403 | 未验证 |
| 8 | 普通运营 reopen 已确认比赛 | 按权限拒绝或允许（以 perms 为准） | 未验证 |
| 9 | 普通人员确认最终名次 | 403 | 未验证 |
| 10 | 普通人员发布荣誉/公告 | 403 | 未验证 |

## 3. 权限常量位置

- Admin：`com.zhs.basket.constant` / `BasketContentPerms` 等
- Controller：`@PreAuthorize("@ss.hasPermi('basket:...')")`

## 4. 结论

代码侧权限注解与菜单 SQL 已按第四～七轮补齐；**本机未完成若依库菜单执行与角色实测**，不得声称动态路由/按钮权限验收通过。
