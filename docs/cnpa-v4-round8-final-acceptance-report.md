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

---

## 10. 后台 V4 赛事发布页面体验优化记录

> 日期：2026-08-04  
> 范围：仅 Admin 后台 `cnpa/event-v4/publish`（V4 赛事发布）页面运营体验优化，**不新增业务功能、不改 V4 业务模型**。

### 10.1 优化页面

| 页面 | 路径 | 变更 |
|------|------|------|
| V4 赛事发布 | `basketball-admin-ui/src/views/cnpa/event-v4/publish/index.vue` | Season / Division 列表展示、新建 Season 弹窗 |
| 展示辅助 | `basketball-admin-ui/src/utils/v4Display.ts` | 新增纯展示映射（中文化、Tag 类型、时间简化） |

### 10.2 状态中文化

列表与筛选项中的枚举值统一映射为中文标签（API 仍传原始枚举，不改后端）：

| 枚举值 | 中文 |
|--------|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| OFFLINE | 已下线 |
| NOT_STARTED | 未开始 |
| REGISTRATION_OPEN | 报名中 |
| REGISTRATION_CLOSED | 报名结束 |
| SCHEDULED | 已排赛 |
| PLAYING | 进行中 |
| FINISHED | 已结束 |
| REGULAR_ROUND_ROBIN | 常规赛单循环 |
| TEAM | 球队报名 |

发布状态、赛季状态、Division 状态、报名方式、赛制列均使用 `el-tag` 展示。

### 10.3 表格与操作列

- 状态类列增加 `nowrap-tag` 样式，避免「已发布」「常规赛单循环」等标签被强制换行。
- 时间列使用 `formatV4CompactRange`：去掉秒、同年结束时间省略年份（如 `2026-08-04 10:00 ~ 08-10 18:00`）。
- 操作列使用 `table-actions` 弹性布局，按钮间距加大，Season 列宽 300px、Division 列宽 280px。

### 10.4 新建 / 编辑 Season 弹窗

- 「封面文件ID」→「封面图文件ID」；「Banner文件ID」→「Banner图文件ID」。
- 文件 ID 输入框增加 placeholder：`暂可填写文件ID，后续接上传组件`。
- 个人报名、非单循环赛制选项保持禁用，文案统一为「暂未开放」。
- 四个时间字段增加必填校验与 placeholder 提示。

### 10.5 测试数据说明（dev 库，未自动清理）

本轮**未删除** R2–R7 冒烟脚本产生的测试 Season / Division，供联调与回归使用。当前 dev 库存在如下冒烟测试数据，**正式环境部署前需清理或不要迁移**：

| id | season_name | display_title | publish_status | season_status | 用途 |
|----|-------------|---------------|----------------|---------------|------|
| 2 | SMOKE_S2 | CNPA League 2026 SMOKE S2 edited | PUBLISHED | REGISTRATION_OPEN | R2–R7 主链路样本 |
| 3 | SMOKE_003752 | CNPA League 2026 SMOKE 003752 edited | OFFLINE | NOT_STARTED | R2 冒烟临时数据 |
| 4 | R3_QUOTA_SEASON | R3_QUOTA | PUBLISHED | NOT_STARTED | R3 名额规则测试 |

关联 Division（如 id=3 SMOKE S2 - PC、id=5 R3_QUOTA_TEST_PC）同理保留于 dev，生产勿迁移。

### 10.6 业务逻辑

**无业务逻辑变更**：未修改 API 调用、状态流转、权限码或后端 Service；仅前端展示与表单 UX 优化。

---

## 11. 后台赛事管理运营体验优化记录

> 日期：2026-08-04  
> 范围：Admin 后台 CNPA 赛事域 8 项菜单页面运营体验优化，**不新增 Open/PERSONAL、不改 V4 业务模型、不改数据库主结构**。

### 11.1 菜单名称优化结果

执行 SQL：`docs/sql/cnpa-v4-round9-admin-ux-menu.sql`

| 原菜单名 | 新菜单名 | menu_id |
|----------|----------|---------|
| V4赛事发布 | 赛事发布 | 2280 |
| V4队长授权 | 队长授权 | 2296 |
| V4报名审核 | 报名审核 | 2301 |
| V4赛事队伍 | 参赛队伍 | 2306 |
| V4赛程管理 | 赛程管理 | 2311 |
| V4比赛列表 | 比赛列表 | 2316 |
| V4比赛数据审核 | 数据审核 | 2321 |
| V4统计榜单 | 统计榜单 | 2326 |

说明：仅改 `menu_name` 展示与 `remark` 保留 V4 标记；**未改** path、component、权限标识。

### 11.2 状态中文化结果

新增/完善统一展示工具：`basketball-admin-ui/src/utils/v4AdminDisplay.ts`（`v4Display.ts` 兼容 re-export）。

覆盖枚举：发布状态、赛季/赛区状态、授权状态、报名状态、赛程生成状态、赛制、报名方式、比赛状态、提交状态、审核动作/结果、排行榜类型。

全部 8 个页面表格与详情弹窗主展示使用中文 + `el-tag`；英文 code 保留在 tag `title` 或小字 meta 中供排查。

### 11.3 赛事发布页面优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/publish/index.vue`

- 页面标题/说明去 V4 化，改为「新版赛事管理：赛事系列 → 赛季 → 平台赛区」
- 赛事系列展示：联赛 League / 竞技场 Arena / 杯赛 Cup / 公开赛 Open
- Season/Division 表格列名、操作按钮、弹窗字段全面中文化
- 新建赛季弹窗补充封面/Banner 文件 ID 字段与 placeholder
- PERSONAL / 非单循环赛制保持禁用并标注「暂未开放」

### 11.4 队长授权选择器优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/captain-grant/index.vue`

- 新增授权改为级联：赛季 → 平台赛区 → 球队 → 队长球员
- Member ID 自动带出只读；有效期/备注保留
- 未绑定平台身份前端提示，后端仍兜底校验
- 主筛选增加赛季/赛区/球队名称/队长昵称/状态；ID 筛选折叠为高级筛选

### 11.5 报名审核页面优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/registration-audit/index.vue`

- 状态/列名中文化；筛选项改为赛季+赛区选择器
- 待审核优先排序（SQL `PENDING_REVIEW` 置顶）
- 详情展示赛事、平台赛区、球队、队长、阵容、审核流水、驳回原因
- Player/Member ID 仅在详情阵容小字展示

### 11.6 赛程管理页面优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/schedule/index.vue`

- 标题/字段中文化；生成状态/发布状态/赛制 tag 展示
- 生成草稿弹窗改为赛季+赛区选择，展示参赛队伍数与预计场数
- 队伍数小于 2 时禁用生成并提示原因

### 11.7 比赛列表页面优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/match/index.vue`

- 筛选项：赛季、平台赛区、批次、轮次、发布状态、比赛状态、球队名称
- 新增比分列：有分数显示 `88 : 75`，否则 `VS`
- 比赛状态/发布状态中文化

### 11.8 比赛数据审核页面优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/match-submission-audit/index.vue`

- 顶部说明中文化；表格字段/状态中文化
- 待审核优先排序
- 详情展示双方队伍、比分、提交方/人/角色、版本、球队/球员统计、材料、审核记录

### 11.9 统计榜单页面优化结果

文件：`basketball-admin-ui/src/views/cnpa/event-v4/stats/index.vue`

- Tab：积分榜 / 球队统计 / 球员统计 / 排行榜
- Division ID 输入改为赛季 + 平台赛区选择器
- 各 Tab 表格列名全面中文化

### 11.10 是否新增 options API

**是**，新增轻量只读 options API（不改业务表结构）：

| 端点 | 用途 |
|------|------|
| `GET /basket/v4/admin/options/seasons` | 赛季下拉 |
| `GET /basket/v4/admin/options/divisions?seasonId=` | 平台赛区下拉（含 eventTeamCount） |
| `GET /basket/v4/admin/options/teams?divisionId=` | 可授权球队下拉 |
| `GET /basket/v4/admin/options/team-players?teamId=&divisionId=` | 球队成员下拉（含认证/平台身份标记） |

后端：`BasketV4AdminOptionsController` + `BasketV4AdminOptionsMapper`。

### 11.11 是否改动业务逻辑

**否（核心业务流程未改）**。变更限于：

- 前端展示/筛选/选择器 UX
- 轻量 options 只读查询 API
- 列表排序优化（待审核置顶）
- 查询条件扩展（球队名称/队长昵称筛选）

未改：状态流转、授权/审核/赛程生成/统计结算逻辑、权限码、数据库主结构。

### 11.12 是否仍有英文 code 直接展示

主列表/详情/筛选项**已无**英文枚举作为主展示。

仍可能出现在：

- tag `title` 悬停（供技术排查）
- 详情中小字 ID（Player # / Member #）
- 后端 ruleCode 等内部字段（赛程批次详情「规则」列，若值为英文 code）
- 旧废弃页面 redirect 提示（非本 8 项主线）

### 11.13 测试数据处理说明

- 当前 dev 库**存在** SMOKE / R3_QUOTA 等冒烟测试赛季与关联数据
- **正式环境不迁移**这些测试数据
- 如需清理 dev 数据，请**单独执行备份后清理 SQL**
- **本轮未自动清理**测试数据

---

## 12. 空父级旧菜单隐藏记录

> 日期：2026-08-04  
> 问题：R8 隐藏旧子菜单后，部分父级菜单仍 `visible=0`，侧边栏出现空下拉。

### 12.1 处理对象

| 菜单名 | menu_id | 处理方式 | 说明 |
|--------|---------|----------|------|
| 报名与阵容 | 2204 | SQL 隐藏 `visible=1` | 子菜单均已 hidden，无可见子项 |
| 数据统计 | 2154 | SQL 隐藏 `visible=1` | 子菜单均已 hidden，无可见子项 |
| 阵容变更处理 | — | 前端 permission.ts 修复 | **非 sys_menu 记录**；由 `applyCnpaSidebarMenuStructure` 虚拟拆分生成 |

### 12.2 SQL 脚本

`docs/sql/cnpa-v4-round8-hide-empty-legacy-parent-menu.sql`

- 先查询确认父级及 `visible_child_count`
- 仅当 `visible_child_count = 0` 时隐藏父级
- 执行后复核新版赛事管理 8 项菜单仍 `visible=0`

### 12.3 前端补充修复

`basketball-admin-ui/src/store/modules/permission.ts`

- 跳过已 hidden 的「报名与阵容」父级，不再虚拟拆分
- 拆分时忽略 `hidden` 子菜单
- 无可见子项时不创建「阵容变更处理」虚拟父级
- 报名侧无可见子项时将「报名与阵容」标记 hidden

### 12.4 新版赛事管理 8 项菜单

**未受影响**，8 项主菜单仍正常可见（parent_id=2152，visible=0）。

### 12.5 dev 库执行结果

- 隐藏父级：报名与阵容（2204）、数据统计（2154）
- 阵容变更处理：库中无对应记录；依赖前端修复后不再空展示
