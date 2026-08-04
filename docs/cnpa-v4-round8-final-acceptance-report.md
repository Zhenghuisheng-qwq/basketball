# CNPA 赛事域 V4 第 8 轮：旧入口清理 + 全链路联调验收 + 最终收口

> 轮次：R8  
> 日期：2026-08-04  
> 范围：V4 最终收口（清理 / 隔离 / E2E 验收 / 报告）  
> **不进入第 9 轮部署**

---

## 1. 本轮完成内容

### 1.1 R7 遗留：`tournamentId` → `divisionId` 清理

个人中心 V4 主线 UI 类型与 Mapper 已完成字段统一：

| 文件 | 变更 |
|------|------|
| `types/accountEvents.ts` | `AvailableEvent` / `MyApplication` / `ParticipatingEvent` 的 `tournamentId` 改为 `divisionId` |
| `utils/v4AccountEventsMappers.ts` | V4 DTO 映射输出 `divisionId`，不再写 `tournamentId` |
| `views/account/events/MyEventsView.vue` | 报名跳转使用 `event.divisionId` / `application.divisionId` |
| `components/account/events/AvailableEventTable.vue` | 列表 key 使用 `divisionId` |
| `utils/participationMappers.ts` | legacy mapper 同步字段名（legacy 语义仍为 seasonId，仅类型对齐） |
| `utils/registrationNavigation.ts` | 标记 `@deprecated`，参数改为 `divisionId` |

**V4 主线文件扫描结果**：`MyEventsView.vue`、`V4MySchedulePanel.vue`、`V4RegistrationFlow.vue`、`V4MatchSubmissionView.vue`、`v4PlayerEvents.ts`、`v4MatchSubmission.ts`、`v4AccountEventsMappers.ts`、`accountEventsUi.ts` 中 **无 `tournamentId` 表示 divisionId 的用法**。

Legacy 保留（不在 V4 主线）：

- `types/eventTeam.ts`、`stores/eventTeam.ts` — 旧队长工作台仍用 `tournamentId` 表示 seasonId
- `views/events/composables/usePublicSeasonContext.ts` — 仅 `/tournament/legacy` 使用
- `views/hall-of-fame/**` — 名人堂独立域

### 1.2 旧入口清理

#### 公开前台

| 入口 | 处理 | 说明 |
|------|------|------|
| `/tournament` | **V4 主入口** | `V4EventsListPage.vue` |
| `/events/:seasonId` | **V4 主入口** | `V4SeasonDetailPage.vue` |
| `/events/match/:matchId` | **V4 主入口** | `V4MatchDetailPage.vue` |
| `/data` | **V4 主入口** | `V4DataPage.vue` |
| `/tournament/legacy` | **保留对照** | 旧 `EventsPage.vue`，主导航不进入 |
| `/data/legacy` | **保留对照** | 旧 `data/index.vue`，主导航不进入 |
| `/tournament/team/*` | **legacy 保留** | 旧 public team API，主入口不跳转 |
| Header 赛事下拉 | **V4 Series** | 非 legacy 路由走 V4 API |
| `publicTournament.ts` | **legacy 模块保留** | V4 主路径页面零引用 |

#### 个人中心

| 入口 | 处理 | 说明 |
|------|------|------|
| `/account/my-events` | **V4 主入口** | 四 Tab 全 V4 API |
| `/account/my-events/v4/registration/:divisionId` | **V4 报名** | |
| `/account/my-events/v4/match-submission/:matchId` | **V4 比赛数据** | |
| `/account/my-tournaments/*` | **redirect** | → `/account/my-events` 或 V4 提交页 |
| `/account/my-events/2026-open/*` | **redirect** | → `?tab=available` |
| `/account/events/open/register` | **修复 redirect** | 原断链 → `AccountMyEvents?tab=available` |
| `/account/events/open/status` | **修复 redirect** | 原断链 → `AccountMyEvents?tab=applications` |
| `/account/team` | **深链保留** | 侧边栏已隐藏（R7），不在主菜单 |

#### 后台 Admin

| 类别 | 处理 |
|------|------|
| V4 主线 8 项 | 可见：`event-v4/publish`、`captain-grant`、`registration-audit`、`event-team`、`schedule`、`match`、`match-submission-audit`、`stats` |
| R2–R4 旧赛事/赛程/比分 | 已 hidden |
| R5 旧统计菜单 | R8 执行 `docs/sql/cnpa-v4-round8-menu.sql` 补隐藏 2080–2102 |
| 旧 participation 审核 | R8 已隐藏 2223–2226 |
| 仍 hidden 的旧 participation | 球队报名审核(2007)、正式赛事球队(2221)、赛事阵容(2222) |

### 1.3 新旧 API / Service 隔离

**前端 V4 主线**：未发现 `/basket/public/tournament`、`/basket/member/team-registration/**`、`/basket/match-data/**` 调用。

**后端 V4 包**：零引用旧赛事 Service 与 deprecated Job；V4 mapper 仅 V4 表；response DTO 均用 `divisionId`。

软耦合（可接受）：`BasketPublicSupport.loadAccessUrlMap()`、共享球员/球队 DAO。

### 1.4 旧 Job 检查

`sys_job` 中无 `ScheduleAutoGenerateJob` / `MatchStatusAutoFinishJob` 配置（0 行）。Job 类在 `basketball-admin` 已 `@Deprecated`，V4 零引用。

---

## 2. 新增 / 修改文件清单

**新增**：`docs/cnpa-v4-round8-final-acceptance-report.md`、`docs/cnpa-v4-round8-smoke-results.json`、`docs/sql/cnpa-v4-round8-menu.sql`、`scripts/smoke_v4_r8_final_acceptance.py`

**修改**：`accountEvents.ts`、`v4AccountEventsMappers.ts`、`MyEventsView.vue`、`AvailableEventTable.vue`、`participationMappers.ts`、`registrationNavigation.ts`、`router/index.ts`

**删除**：无（遵循不盲目删代码原则）

---

## 3. 全链路 E2E 验收记录

```bash
python scripts/smoke_v4_r8_final_acceptance.py
python scripts/smoke_v4_r6_public_web.py
python scripts/smoke_v4_r7_personal_center.py
```

| 结果 | 说明 |
|------|------|
| R8 冒烟 | 18/18 PASS |
| R6 回归 | 16/16 PASS |
| R7 回归 | 20/20 PASS |

关键业务验证（R3–R7 样本 divisionId=5）：

- 发布 → 报名 → 审核 → EventTeam/EventPlayer 快照 → 赛程 → 比赛数据 → 统计：PASS
- A 队 PENDING_REVIEW 不影响 B 队提交：PASS
- 审核中不占名额 / APPROVED 占名额：PASS（R3 规则）
- 全链路 division_id 隔离：PASS

---

## 4. 页面验收摘要

| 域 | 结果 |
|----|------|
| 公开前台 `/tournament` `/events/*` `/data` | PASS（空态/SafeImage/Division 隔离） |
| 个人中心 my-events 四 Tab + V4 子页 | PASS |
| 后台 V4 八项 | PASS；旧菜单已 hidden |

---

## 5. 旧表处理建议

15 张 legacy 表均仍存在；V4 主线不再写入。有历史数据的表（registration/roster/stat 等）建议上线稳定后归档再 DROP；空表（`basket_tournament_open_registration`、`basket_stage_standing`、`basket_data_settlement_batch`）可优先评估删除。

---

## 6. 修复的问题

1. `tournamentId` → `divisionId`（R7 遗留）
2. Open 报名 redirect 断链
3. 旧 statistics 菜单补 hidden
4. R8 冒烟 API 路径修正

---

## 7. 遗留风险

- `/tournament/team/*` 仍为 legacy，主入口不跳转
- `/tournament/legacy`、`/data/legacy` 对照页保留
- legacy 视图源码未物理删除
- 名人堂仍用旧 API（非 V4 主线）
- 旧表 DROP 需单独归档轮次

---

## 8. 验收标准与第 9 轮建议

第 8 轮 20 项验收标准 **全部达成**。

**建议可以进入第 9 轮部署准备**，前提：目标环境执行 `cnpa-v4-round8-menu.sql`、跑通 R8/R6/R7 冒烟、确认生产无旧 Job、旧表本轮不 DROP。

---

## 9. 删除 / 隐藏 / 保留速查

**隐藏**：Admin 旧赛事/赛程/统计/participation 审核菜单；个人中心「我的球队」；Header 不暴露 legacy。

**保留 redirect/legacy**：`/tournament/legacy`、`/data/legacy`、`/my-tournaments/*`、`/account/team` 深链、legacy 源码。

**V4 主入口**：前台四路由 + `/account/my-events` + Admin `cnpa/event-v4/*` 八项。
