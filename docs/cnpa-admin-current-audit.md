# CNPA Admin 现状审计（第一批）

> **盘点日期**：2026-07-25  
> **数据库**：`basketball` @ MySQL 8.0.43（本地开发）  
> **范围**：`basketball-admin`、`basketball-admin-ui`；只读参考 `basketball-web`、`basketball-web-ui`  
> **本批次**：只读分析 + 文档输出；**未执行任何数据库写操作**

---

## 1. 技术基线

### 1.1 basketball-admin-ui

| 项 | 版本/说明 | 依据 |
|----|-----------|------|
| 基座 | RuoYi-Vue 3.9.2 | `package.json` |
| Vue | 3.5.26 | `package.json` |
| Vite | 6.4.1 | `package.json` |
| Element Plus | 2.13.1 | `package.json` |
| TypeScript | 5.6.3 | `package.json` |
| 路由 | 静态 `constantRoutes` + 动态 `GET /getRouters` | `src/router/index.ts`, `src/permission.ts` |
| 权限 | JWT + `v-hasPermi` + `stores/user.permissions` | `src/directive/permission/hasPermi.ts` |
| 动态组件 | `loadView(component)` 精确匹配 `views/{component}.vue` | `src/store/modules/permission.ts` |
| keep-alive | 依赖路由 `name`；CNPA 页保留 `Basket*` 组件名 | 各 `cnpa/**/index.vue` |
| API | `@/utils/request` → `/basket/*` | `src/api/basket/*` |

**CNPA 页面目录**：`src/views/cnpa/`（26 个 vue 文件，5 个业务域 + dashboard）。`views/basket/` **已不存在**（历史目录已迁移，见 git rename 记录）。

**重复目录**：无并存副本；API 仍用 `src/api/basket/` 命名（与 views 域不一致，属命名遗留）。

### 1.2 basketball-admin

| 项 | 值 | 依据 |
|----|-----|------|
| Spring Boot | 3.5.11 | `pom.xml` |
| Java | 21 | `pom.xml` |
| ORM | MyBatis（非 MP） | `pom.xml`, `mapper/**/*.xml` |
| 业务包 | `com.zhs.basket.*` | `src/main/java/com/zhs/basket/` |
| Controller | `com.zhs.web.controller.basket/*`（28 个） | 源码 |
| 权限 | `@PreAuthorize("@ss.hasPermi('basket:...')")` | 各 Controller |
| 事务 | 审核/结算类 Service 变更方法 `@Transactional` | 如 `BasketPlayerCertServiceImpl` |
| 分页 | RuoYi `TableDataInfo` + `PageHelper` | 通用模式 |
| 操作日志 | Controller `@Log` → `sys_oper_log` | RuoYi |
| Redis | 结算锁 `SettlementLockHelper`、公开缓存清理 | `com.zhs.basket.support/` |
| 业务审计 | 多表：`basket_*_audit`、`basket_match_status_log`、`basket_stat_change_record` | 各 Service |

**接口前缀**：主要为 `/basket/*`（camelCase 与 kebab-case 混用）。

### 1.3 Web 端重叠（只读）

| 域 | Web 实现 | Admin 重叠 |
|----|----------|------------|
| 邮箱注册/登录 | ✅ 真实 API | Admin 不管理会员账户 |
| 球员认证申请 | ✅ 无身份证字段 | Admin 审核同一套表 |
| 球队创建/成员 | ✅ 真实 API | Admin 球队审核 |
| 球队赛季报名 | ✅ 真实 API | Admin 报名审核 |
| Open 个人报名 | ❌ Mock | Admin 无对应页 |
| 公开赛程/荣誉 | ✅ Public API | Admin 录入/发布源数据 |

---

## 2. 菜单现状（sys_menu 只读）

### 2.1 统计

| 类型 | 数量 |
|------|------|
| CNPA 相关菜单总计 | 158 |
| 目录 (M) | 8 |
| 页面 (C) | 26 |
| 按钮 (F) | 124 |
| 重复 perms | 0 |

### 2.2 一级菜单（parent_id=0，CNPA 业务）

| order | menu_id | 名称 | path | 状态 |
|------:|--------|------|------|------|
| 5 | 2201 | 运营工作台 | operations-dashboard | 直接页面 C |
| 6 | 2152 | 赛事管理 | event | 目录 M |
| 7 | 2151 | 账号与认证 | identity | 目录 M |
| 8 | 2204 | 报名与阵容 | participation | 目录 M |
| 9 | 2153 | 比赛执行 | competition-exec | 目录 M |
| 10 | 2154 | 数据统计 | statistics | 目录 M |
| 11 | 2207 | 纪律与档案 | discipline | 目录 M（无子页） |
| 12 | 2101 | 内容运营 | content-ops | 目录 M |

**已停用**：`menu_id=2000`「CNPA赛事管理」`visible=1, status=1`，不再展示。

**说明**：菜单树已接近目标一级结构；与规范「第一批不执行菜单重构」相比，库中状态可能含后续批次变更，本审计以 **当前库快照** 为准。

### 2.3 component 与文件一致性

对 26 个页面级 `component`（`cnpa/.../index`）与 `src/views/cnpa/` 逐一比对：**全部存在对应 vue 文件**（依据：glob + loadView 规则）。

### 2.4 角色授权

| role_id | 角色 | CNPA menu 授权数 |
|---------|------|------------------|
| 1 | 超级管理员 | 0（若依全量规则） |
| 2 | 普通角色 | 157 |

---

## 3. 前端页面清单（26 业务页 + 1 工作台）

| 页面 | 路径 | 组件 name | 真实 API | Mock | 分类 |
|------|------|-----------|----------|------|------|
| 运营工作台 | cnpa/dashboard/index.vue | Index | 无 | 无 | **D**（若依占位文案） |
| 赛事列表 | cnpa/event/list/index.vue | BasketTournament | tournament.ts | 无 | **A** |
| 赛季管理 | cnpa/event/season/index.vue | BasketSeason | season.ts | 无 | **A** |
| 赛事规则 | cnpa/event/rule/index.vue | BasketTournamentRule | tournamentRule.ts | 无 | **A** |
| 赛事阶段 | cnpa/event/stage/index.vue | BasketStage | tournamentStage/Round | 无 | **A** |
| 分组管理 | cnpa/event/group/index.vue | BasketGroup | tournamentGroup.ts | 无 | **A** |
| 球员认证审核 | cnpa/identity/player-certification/index.vue | BasketPlayerCert | playerCert.ts | 无 | **A** |
| 队长资格审核 | cnpa/identity/captain-qualification/index.vue | BasketCaptainQualification | captainQualification.ts | 无 | **A** |
| 球队审核认证 | cnpa/identity/team-certification/index.vue | BasketTeamAudit | teamAudit.ts | 无 | **B**（缺 audit-record API） |
| 球队报名审核 | cnpa/participation/team-registration/index.vue | BasketRegistrationAudit | registrationAudit.ts | 无 | **B**（详情无阵容列表） |
| 赛程管理 | cnpa/competition/schedule/index.vue | BasketMatch | matchSchedule.ts | 无 | **A** |
| 比分录入 | cnpa/competition/score/index.vue | BasketMatchScore | matchScore.ts | 无 | **B**（confirmScore 后端 stub） |
| 比赛数据录入 | cnpa/competition/match-data-entry/index.vue | BasketMatchStat | matchStat.ts | 无 | **A** |
| 比赛名单 | cnpa/competition/lineup/index.vue | BasketMatchLineup | matchLineup.ts | 无 | **A** |
| 数据结算 | cnpa/statistics/settlement/index.vue | BasketDataSettlement | dataSettlement.ts | 无 | **A** |
| 积分榜 | cnpa/statistics/standings/index.vue | BasketStanding | standing.ts | 无 | **A** |
| 球队/球员赛季统计 | cnpa/statistics/team-season, player-season | Basket*SeasonStat | *SeasonStat.ts | 无 | **A** |
| 排行榜 | cnpa/statistics/ranking/index.vue | BasketRanking | ranking.ts | 无 | **A** |
| 赛季名次 | cnpa/statistics/season-result/index.vue | BasketSeasonResult | seasonResult.ts | 无 | **A** |
| 公告/荣誉/名人堂等 | cnpa/content/* | Basket* | 对应 api | 无 | **A** |
| 首页内容/合作伙伴 | cnpa/content/home-banner, partner | Basket* | 对应 api | 无 | **A**（菜单 hidden） |

**Admin-ui 无 Mock**（`src/views/cnpa` 内无 mock 引用）。

---

## 4. 后端接口链路摘要

28 个 `Basket*Controller`，全部有 `@PreAuthorize`。核心链路：

| 域 | Service | 状态校验 | 事务 | 业务表 |
|----|---------|----------|------|--------|
| 球员认证 | BasketPlayerCertServiceImpl | PlayerCertStatusEnum | ✅ | cert_application/audit/material, player_profile |
| 球队审核 | BasketTeamAuditServiceImpl | TeamAuditStatusEnum | ✅ | team, team_audit_record |
| 报名审核 | BasketRegistrationAuditServiceImpl | RegistrationStatusEnum | ✅ | tournament_registration*, discipline |
| 比分 | BasketMatchScoreServiceImpl | MatchStatusEnum | ✅ | match, period_score；**confirmScore 仅写 log** |
| 技术统计 | BasketMatchStatServiceImpl | MatchDataStatusEnum | ✅ | match_*_stat, stat_change_record + 触发结算 |
| 数据结算 | BasketDataSettlementServiceImpl | 校验服务 | ✅ + Redis 锁 | standing/rank/season_stat 等 |

**球员认证 Domain**（`BasketPlayerCertApplication.java`）：含 `gameTerminal/gameAccount/positionCode`，**无** `mobile/idCard/realName`。

**未发现** Service 层 TODO/mock 字符串；LocalDemo 仅 `@Profile(local)`。

---

## 5. 功能分类统计

| 分类 | 数量 | 说明 |
|------|------|------|
| **A** 可直接使用 | 18 | 赛事结构、审核主流程、比赛/统计/内容 CRUD |
| **B** 小幅修正 | 4 | 球队审核缺 audit API、报名详情、比分 confirm、权限菜单对齐 |
| **C** 页面可保留逻辑需重构 | 2 | 报名通过→正式参赛数据链；永久档案 vs 赛事球队 |
| **D** 尚未实现 | 15+ | 工作台、账号球员管理、Open报名、转会、纪律、业务审计等 |
| **E** 重复需合并 | 1 | 静态 `/index` 与动态「运营工作台」双入口 |
| **F** 错误实现 | 0（Admin） | Admin 侧已无身份证字段；Web 端 Mock 不属 Admin 范围 |

---

## 6. 最高风险（P0/P1/P2）

### P0

| ID | 问题 | 依据 |
|----|------|------|
| P0-1 | 报名审核通过后 **无独立「正式赛事球队/阵容」表**，与 `basket_tournament_group_team` 关系待确认 | 表 `basket_team_season_profile` 0 行；无 admin 正式参赛页 |
| P0-2 | **比分 confirmScore** 不更新 data_status，与统计 confirm 链路不一致 | `BasketMatchScoreServiceImpl.confirmScore` |

### P1

| ID | 问题 | 依据 |
|----|------|------|
| P1-1 | 运营工作台无真实待办 API | `cnpa/dashboard/index.vue` 无 API |
| P1-2 | 菜单权限 `ranking:publish`、`dataSettlement:confirm` 无后端端点 | sys_menu F 型 vs Controller 缺失 |
| P1-3 | 无赛事级数据权限（Admin 操作未按 tournament 隔离） | 待确认：Controller 无 tournament scope 参数 |
| P1-4 | 业务审计 ≠ sys_oper_log，无独立业务审计页 | 有 stat_change_record 但无查询 UI |

### P2

| ID | 问题 |
|----|------|
| P2-1 | URL camelCase/kebab-case 混用 |
| P2-2 | `BasketMatchPerms` 常量未在 Controller 引用 |
| P2-3 | 纪律表 `basket_discipline_record` 0 行，仅 registration 校验引用 |

---

## 7. 构建结果（只读验证）

| 项目 | 命令 | 结果 |
|------|------|------|
| basketball-admin-ui | `npm run build:prod` | ✅ 通过（2026-07-25 复验，exit 0） |
| basketball-admin | `mvn compile -DskipTests` | ⚠️ 本环境 PATH 无 `mvn`；`target/classes` 存在历史编译产物，本次未复验 |

---

## 8. 数据库操作确认

**本批次只执行了 SELECT / SHOW / DESCRIBE 类只读查询，没有执行 INSERT、UPDATE、DELETE、ALTER、DROP、TRUNCATE。**

---

## 9. 相关文档

- [`cnpa-admin-feature-matrix.md`](./cnpa-admin-feature-matrix.md)
- [`cnpa-admin-route-menu-mapping.md`](./cnpa-admin-route-menu-mapping.md)
- [`cnpa-admin-database-inventory.md`](./cnpa-admin-database-inventory.md)
- [`cnpa-admin-refactor-roadmap.md`](./cnpa-admin-refactor-roadmap.md)
