# CNPA 运营工作台设计

## 1. 数据来源

`GET /basket/dashboard/summary` 聚合以下待办计数：

| 字段 | SQL 来源 |
|---|---|
| pendingPlayerCert | basket_player_cert_application 待审 |
| pendingTeamAudit | basket_team audit_status |
| pendingRegistration | basket_tournament_registration |
| pendingOpenRegistration | basket_tournament_open_registration |
| pendingRosterChange | basket_tournament_roster_change_request |
| pendingPlatformChange | basket_player_platform_change_request |
| pendingMatchDataAudit | basket_match_data_audit PENDING |
| pendingMatchDispute | basket_match_data_dispute PENDING/PROCESSING |

## 2. 赛事权限过滤

- 超级管理员：全库统计
- 赛事管理员：`BasketTournamentScopeSupport.accessibleTournamentIds()` 过滤 registration/open/roster/match 相关计数
- 无授权：返回空摘要

## 3. 前端

- 页面：`cnpa/dashboard/index.vue`
- 卡片点击跳转对应 CNPA 菜单路由
- 权限：menu 2201 perms = `basket:dashboard:summary`

## 4. 性能

- 单请求多条 COUNT SQL，带 tournament_id 索引过滤
- 未使用 FLUSHALL；可选短 TTL 缓存（当前未启用 Redis 缓存）
