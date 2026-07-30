# CNPA 比赛数据模块现状核查报告（第一批 · 只读）

> 核查日期：2026-07-31  
> 范围：`basketball-web` / `basketball-web-ui` / `basketball-admin` / `basketball-admin-ui` / `docs` / `docs/sql`  
> 本轮：**只核查，不改代码、不新增表/接口、不重构**

---

## 第一部分：总体结论

| 核查项 | 已实现 | 部分实现 | 未实现 | 是否 Mock | 代码证据 | 数据库证据 | 风险等级 | 建议批次 |
|--------|--------|----------|--------|-----------|----------|------------|----------|----------|
| M01 我的比赛 | | ✓ | | | `EventTeamManagementView.vue` 占位 | 无 Web 专用表 | **高** | P0 |
| M02 比赛数据提交 | | | ✓ | ✓ 占位 | `mockOnly: true` | 无 submission 层 | **高** | P0 |
| M03 数据审核中 | | | ✓ | | 无 Web 页/API | `data_status=1` 在 match 表 | 中 | P0 |
| M04 数据驳回重提 | | ✓ | | | Admin `rejectResult` | `REJECTED(4)` | **高** | P0 |
| M05 数据审核通过 | | ✓ | | | Admin `confirmResult` + 公开 API | `CONFIRMED(2)` | 中 | P0 |
| M06 数据争议 | | ✓ | | | Admin dispute CRUD | `basket_match_data_dispute` | **高** | P1 |
| M07 弃赛异常 | | ✓ | | | Admin forfeit + 导航壳页 | `walkover_team_id`/`result_status` | 中 | P1 |
| 队长权限校验 | | | ✓ | | 无 Web 提交接口 | — | **高** | P0 |
| 普通队员只读 | | | ✓ | | 无 Web 流程 | — | 中 | P0 |
| 一方提交后另一方不可重复提交 | | | ✓ | | 无 submission 锁 | — | **高** | P0 |
| 提交版本 | | ✓ | | | `data_version` 列 + revision 表 | 无 submission 版本表 | **高** | P0 |
| 审核驳回重提 | | ✓ | | | `rejectResult`→`REJECTED` | 覆盖式写入 stat 表 | **高** | P0 |
| 审核通过写正式数据 | ✓ | | | | `confirmResult` | `official_result` + stat 表 | 低 | — |
| 积分榜同步 | ✓ | | | | `rebuildMatch` | `basket_stage_standing` | 低 | — |
| 球队/球员统计同步 | ✓ | | | | `BasketDataSettlementServiceImpl` | season stat 表 | 低 | — |
| 排行榜同步 | ✓ | | | | settlement rebuild | rank 表 | 低 | — |
| 数据公开控制 | ✓ | | | | `BasketPublicMatchServiceImpl` | `data_status=2` 过滤 | 低 | — |
| 12 小时争议窗口 | | | ✓ | | 无时间校验 | — | 中 | P1 |
| 异常裁定 | | ✓ | | | Admin forfeit/schedule | 无 exception 申请表 | 中 | P1 |
| 材料上传 | | ✓ | | | dispute `evidence_file_ids` | 无材料关联表 | 中 | P1 |
| 审核日志 | ✓ | | | | `basket_match_status_log` | audit 表未写入 | 中 | P0 |
| 并发幂等（确认/结算） | ✓ | | | | 状态 CAS + sync 幂等键 | `basket_stat_sync_record` | 低 | P1 |
| 并发幂等（队长提交） | | | ✓ | | 无 Web 提交 | — | **高** | P0 |
| 统计重算 | ✓ | | | | settlement API + UI | `basket_data_settlement_batch` | 低 | — |
| 正式数据修正 | | ✓ | | | `reopenResult` + revision 表 | revision 无 UI/API | 中 | P2 |

**一句话结论：**

- **Admin 端已有「管理员录入 → 提交 → 确认 → 结算重算」闭环**，但不是「队长 Web 提交 → 管理员审核」模式。
- **Web 端 M01–M07 均未实现**；M02 仅为 `mockOnly` 占位。
- **无独立 submission 审核层表**；待审数据直接写入 `basket_match` / `basket_match_*_stat`（草稿态），确认后才进赛季统计。
- **`basket_match_data_audit` 表已建但提交流未写入**，Dashboard 待审数可能恒为 0，与审核列表数据源不一致。

---

## 第二部分：现有代码清单

### basketball-web

| 文件路径 | 类/组件 | 方法/接口 | 功能 | 真实业务 | Mock | 可复用 | 风险 |
|----------|---------|-----------|------|----------|------|--------|------|
| `service/impl/BasketPublicMatchServiceImpl.java` | 公开比赛服务 | 公开 schedule/detail/stat | 仅 `data_status=CONFIRMED` 展示统计 | ✓ | | ✓ 只读展示 | 低 |
| `service/impl/BasketPublicSeasonStatServiceImpl.java` | 公开赛季统计 | 积分榜/排行查询 | 过滤已确认比赛 | ✓ | | ✓ | 低 |
| `controller/BasketMatchController.java` | CRUD | `/basket/match/*` | 生成式 CRUD | | | | **高：可绕过状态机直写正式表** |
| `controller/BasketMatchTeamStatController.java` | CRUD | `/basket/match/team/stat/*` | 生成式 CRUD | | | | **高** |
| `controller/BasketMatchPlayerStatController.java` | CRUD | `/basket/match/player/stat/*` | 生成式 CRUD | | | | **高** |
| `controller/BasketMatchPeriodScoreController.java` | CRUD | 分节比分 CRUD | 生成式 CRUD | | | | **高** |
| `service/impl/AccountSettingServiceImpl.java` | 隐私设置 | `showMatchData` 开关 | 控制球员主页是否展示历史数据 | ✓ | | | 低 |

**Web 端无：** 队长提交、我的比赛、审核状态、争议、异常申请等 Controller/Service。

---

### basketball-web-ui

| 页面路径 | 组件 | 路由 | API | Mock | 缺失 |
|----------|------|------|-----|------|------|
| `views/account/tournaments/EventTeamManagementView.vue` | 球队管理 | `/account/my-tournaments/:tournamentId/team` | 无提交 API | **✓ mockOnly** | M02 完整提交流程 |
| `stores/eventTeam.ts` | store | — | — | `matchDataTodoCount` 恒为 0 | 真实待办计数 |
| `views/tournament/team/TeamMatches.vue` | 公开球队赛程 | `/tournament/:teamId/matches` | 公开 API | | 非「我的比赛」 |
| `views/events/components/*` | 公开赛程 | `/events` | 公开 API | | 非账号流程 |

**M01–M07 均无独立路由、组件、API 文件。**

---

### basketball-admin（核心业务）

#### 比赛数据录入 / 审核 / 确认

| 模块 | 文件 | 类 | 方法 | 接口路径 | 真实业务 | 权限 | 事务 | 幂等 | 日志 |
|------|------|-----|------|----------|----------|------|------|------|------|
| 比分 | `BasketMatchScoreController` | Score | save/submit/confirm/reject/reopen/forfeit | `/basket/match/{id}/score*` | ✓ | `@PreAuthorize` | ✓ | CAS 更新 | status_log |
| 统计 | `BasketMatchStatController` | Stat | save/submit/confirm/reject/reopen | `/basket/match/{id}/*-stat*` | ✓ | ✓ | ✓ | CAS | status_log |
| 待审列表 | `BasketMatchDataAuditController` | Audit | pending | `GET /basket/matchAudit/pending` | 半实现 | ✓ | | | 查 match 非 audit 表 |
| 争议 | `BasketMatchDisputeController` | Dispute | list/create/reject/uphold | `/basket/matchDispute/*` | ✓ | ✓ | ✓ | 状态 CAS | 无专用表外日志 |
| 结算 | `BasketDataSettlementController` | Settlement | rebuild* | `/basket/data-settlement/*` | ✓ | ✓ | ✓ | sync 幂等键 | batch 表 |

**Service 核心实现：**

- `BasketMatchScoreServiceImpl` — 分节比分、提交比分（`data_status→SUBMITTED`）、确认比分、判负（默认 20-0）
- `BasketMatchStatServiceImpl` — 保存/提交球队&球员统计、`confirmResult` 写正式结果并触发 settlement
- `BasketMatchDataAuditServiceImpl` — **仅 listPending**，包装 `BasketMatchDataAuditMapper.selectList`（实际查 `basket_match`）
- `BasketMatchDisputeServiceImpl` — 争议 CRUD；**uphold 不改赛果、不重算**
- `BasketMatchStatSyncServiceImpl` — `basket_stat_sync_record` 幂等键防重复 settlement
- `BasketDataSettlementServiceImpl` — 重算积分榜/赛季统计/排行榜（仅 `CONFIRMED` 比赛）

#### 数据流（当前 Admin 实现）

```
管理员录入比分/统计 (DRAFT, 写入 basket_match_*)
  → submitScore (match.data_status=SUBMITTED)
  → submitTeamStats/submitPlayerStats (仅写 status_log，stat 行仍 DRAFT)
  → confirmScore (score_confirmed=1)
  → confirmResult (data_status=CONFIRMED, official_result, rebuildMatch)
  → 赛季积分榜/统计/排行榜更新
```

**与目标业务差异：** 操作者是**后台管理员**，不是**赛事队长**；无「一方提交锁定」；无 submission 版本快照。

---

### basketball-admin-ui

| 页面 | 路由 | 组件 | API 文件 | 真实接入 | Mock | 缺失 |
|------|------|------|----------|----------|------|------|
| 比赛数据审核列表 | `/competition/match-data-audit` | `match-data-audit/index.vue` | `matchAudit.ts` | ✓ | | 本页无「通过」按钮，需跳转录入页 |
| 比赛数据录入 | `/competition-exec/match-data-entry` | `match-data-entry/index.vue` | `matchStat.ts` | ✓ | | 无版本历史 UI |
| 数据争议 | `/competition/match-dispute` | `match-dispute/index.vue` | `matchDispute.ts` | ✓ | | uphold 不联动修正 |
| 弃赛异常 | `/competition/match-abnormal` | `match-abnormal/index.vue` | **无** | 导航壳 | | 无工单 CRUD |
| 比分/判负 | `/competition-exec/score` | `score/index.vue` | `matchScore.ts` | ✓ | | |
| 数据结算/重算 | `/statistics/settlement` | `settlement/index.vue` | `dataSettlement.ts` | ✓ | | 无 batch 历史 UI |

菜单来源：`docs/sql/cnpa-batch5-migration.sql`（menu_id 2230–2233）。

---

## 第三部分：现有表结构清单

### 正式数据层（`sql/basketball.sql` 基线，14 张均有）

| 表 | 职责 | 当前用法 |
|----|------|----------|
| `basket_match` | 比赛主表、比分、状态 | **同时承载草稿/待审/已确认**（`data_status`） |
| `basket_match_team` | 主客得分、胜负类型 | 正式层；判负时写 FORFEIT_WIN/LOSS |
| `basket_match_period_score` | 分节比分 | 录入即写同表，确认前为草稿态 |
| `basket_match_roster` | 出场名单 | 正式层；Batch5 增快照列 |
| `basket_match_team_stat` | 球队单场统计 | **录入即写同表**，`data_status=0` 草稿 |
| `basket_match_player_stat` | 球员单场统计 | 同上 |
| `basket_match_status_log` | 状态变更日志 | ✓ 有写入 |
| `basket_stage_standing` | 积分榜 | 仅 settlement 重算写入 |
| `basket_team_season_stat` | 球队赛季汇总 | 仅 settlement |
| `basket_player_season_stat` | 球员赛季汇总 | 仅 settlement |
| `basket_team_rank` / `basket_player_rank` | 排行榜快照 | 仅 settlement |
| `basket_stat_change_record` | 字段级变更审计 | confirm 时写摘要；**非 submission 流程** |
| `basket_file_asset` | 文件资源 | 可复用为材料存储 |

### Batch5 扩展（`docs/sql/cnpa-batch5-migration.sql`，未合并进基线）

| 表 | 职责 | 实现状态 |
|----|------|----------|
| `basket_match_official_result` | 正式结果快照 | ✓ `confirmResult` 写入 |
| `basket_match_data_audit` | 审核记录 | ⚠️ **表存在，提交流未 insert** |
| `basket_match_data_dispute` | 数据争议 | ✓ Admin CRUD |
| `basket_match_data_revision` | 数据修订 | ⚠️ 表存在，无 Controller/UI |
| `basket_data_settlement_batch` | 结算批次 | ✓ settlement 写入 |
| `basket_stat_sync_record` | 同步幂等记录 | ✓ confirm 时使用 |

### 缺失表（精确名，全仓库无 CREATE TABLE）

1. `basket_match_data_submission` 及 period/team_stat/player_stat/material 子表 — **完全缺失**
2. `basket_match_data_dispute_material` — 用 `evidence_file_ids` 字符串代替
3. `basket_match_exception_application/material/audit` — **完全缺失**
4. `basket_match_stat_sync_task` — 由 `basket_stat_sync_record` 替代（非任务队列）
5. `basket_match_stat_settlement_batch` — 实际为 `basket_data_settlement_batch`
6. `basket_match_data_version` — 仅为 `basket_match.data_version` **列**

### 是否足够支撑 M01–M07

| 模块 | 现有表是否足够 | 说明 |
|------|----------------|------|
| M01–M05 | **不足** | 缺 submission 层、队长身份绑定、版本历史 |
| M06 | **部分** | dispute 表有，缺 12h 窗口、Web 端、材料子表 |
| M07 | **不足** | 无 exception 申请表；forfeit 走 admin 比分 API |

### 复用建议

1. **`basket_file_asset`** — ✓ 可复用为截图/证明材料存储，需 biz_type + biz_id 关联表或 submission_material 表
2. **`basket_stat_change_record`** — ✓ 适合**正式数据修正**留痕；**不适合**承载首次队长提交审核（无版本、无材料、无 submitter 链路）
3. **正式 stat 表** — 当前**录入与正式未分离**，驳回重提会**覆盖**同表数据，不符合「驳回版本保留」规则

---

## 第四部分：现有状态枚举

### match_status（Java: `MatchStatusEnum`）

| code | 枚举 | 业务含义 |
|------|------|----------|
| 0 | DRAFT | 草稿 |
| 1 | SCHEDULED | 已编排/未开始 |
| 2 | RUNNING | 进行中 |
| 3 | FINISHED | 已结束 |
| 4 | POSTPONED | 已延期 |
| 5 | CANCELLED | 已取消 |

**缺失（相对目标规则）：** 异常、弃赛、待裁定 — 弃赛通过 `walkover_team_id` + `result_status=2` 表达，非独立 match_status。

### data_status（Java: `MatchDataStatusEnum`）

| code | 枚举 | 业务含义 |
|------|------|----------|
| 0 | DRAFT | 草稿/未提交 |
| 1 | SUBMITTED | 已提交（待审） |
| 2 | CONFIRMED | 已确认（正式生效） |
| 3 | REOPENED | 已重开 |
| 4 | REJECTED | 已驳回 |

**映射目标规则：** 无「已公开」「争议中」「异常处理中」独立值；公开靠 `public_status` + 公开 API 过滤。

### public_status / publish_status

- `public_status`：0/1 二元，无 Java Enum
- 赛程模块 `publish_status`（`MatchPublishStatusEnum`）：0草稿/1已发布/2已下架 — **用于赛程发布，非比赛数据审核公开**

### result_status（Batch5 列）

| 值 | 含义 |
|----|------|
| 0 | 无 |
| 1 | 正常 |
| 2 | 弃赛 |
| 3 | 管理员裁定 |
| 4 | 重赛 |
| 5 | 作废 |

### submission_status — **无独立枚举**

复用 `MatchDataStatusEnum`；无「被新版本替代」「已作废」细分。

### audit_status（`basket_match_data_audit` 表，varchar）

`PENDING` / `APPROVED` / `REJECTED` — **无 Java Enum**，且**提交流未写入**。

### dispute_status（`basket_match_data_dispute` 表）

代码中使用：`PENDING`、`PROCESSING`、`REJECTED`、`UPHELD` — **无 Java Enum**；无「部分采纳」「已过期」。

### exception_status — **不存在**

异常处理复用赛程 `POSTPONED/CANCELLED` + 判负 API。

---

## 第五部分：当前流程还差什么

### 队长提交

| 缺失项 |
|--------|
| Web Controller/Service：`/basket/player/match/{id}/data/submit` 等 |
| 队长身份校验（EventTeam 队长 + 本场双方） |
| submission 主表 + 子表（比分/统计/材料） |
| 「一方提交锁定」唯一约束 |
| M02 前端页面与 API |
| 截图上传与材料关联 |

### 审核中

| 缺失项 |
|--------|
| Web M03 页面 |
| 向 `basket_match_data_audit` 写入 PENDING 记录 |
| 审核中禁止编辑的后端校验（当前 SUBMITTED 仍 canEdit） |
| 队长端查看审核进度 API |

### 驳回 / 重新提交

| 已有 | 缺失 |
|------|------|
| Admin `rejectResult` → REJECTED | Web M04 页面 |
| | **新版本 submission**，旧版本保留 |
| | 驳回时不覆盖 stat 表（当前 save 会 softDelete 重写） |
| | audit 表写入驳回记录 |

### 审核通过 / 正式公开

| 已有 | 缺失 |
|------|------|
| Admin `confirmResult` + official_result | 与「管理员审核队长 submission」解耦 |
| 公开 API 过滤 CONFIRMED | 12 小时争议窗口起点（公开时间戳） |
| settlement 重算 | Web M05 队长端视图 |

### 争议

| 已有 | 缺失 |
|------|------|
| Admin dispute CRUD | Web M06 |
| evidence_file_ids | 12 小时窗口校验 |
| | 队长 initiator 校验（当前默认 ADMIN） |
| | uphold → revision + 重算 |

### 异常比赛

| 已有 | 缺失 |
|------|------|
| Admin forfeit/schedule cancel | Web M07 |
| match-abnormal 导航页 | exception 申请表 + 材料 + 裁定流 |

### 统计同步

| 已有 | 缺失 |
|------|------|
| confirm 同事务触发 rebuildMatch | 异步任务队列（当前同步调用） |
| stat_sync_record 幂等 | submission 确认与 stat 同步分离 |

---

## 第六部分：是否可以复用现有表

| # | 问题 | 结论 |
|---|------|------|
| 1 | `basket_match` 是否只作正式结果表 | **否**。当前同时存草稿/待审/已确认 |
| 2 | `basket_match_period_score` 是否只作正式分节表 | **否**。录入即写 |
| 3 | `basket_match_team_stat` 是否只作正式球队单场 | **否**。`data_status=0` 草稿与正式同表 |
| 4 | `basket_match_player_stat` 同上 | **否** |
| 5 | `basket_stat_change_record` 是否只适合修正记录 | **是**。不适合首次提交审核流 |
| 6 | `basket_file_asset` 是否可复用材料 | **是**。需关联表 |
| 7 | 是否需要新增 submission 层 | **是（P0）**。否则无法隔离待审/驳回版本 |

---

## 第七部分：第二批最小实现方案（建议，不实施）

### P0 — 必须实现（队长提交 + 审核隔离）

**SQL（建议新增）**

- `basket_match_data_submission` — 提交主表（match_id, submitter_member_id, submitter_event_team_id, version_no, status, submitted_time, …）
- `basket_match_data_submission_period` — 分节比分快照
- `basket_match_data_submission_team_stat` / `player_stat` — 统计快照
- `basket_match_data_submission_material` — 材料（file_asset_id, material_type）
- 唯一索引：`(match_id, status IN active)` 或 `uk_match_active_submission`

**后端（basketball-web）**

- `BasketPlayerMatchDataController` — 我的比赛列表、提交、查看状态、重提
- `BasketPlayerMatchDataServiceImpl` — 队长校验、一方提交锁、校验规则、写 submission 表（**不写正式 stat**）
- 枚举：`SubmissionStatusEnum`

**后端（basketball-admin）**

- 改造 `BasketMatchDataAuditServiceImpl` — 审核列表查 submission 表
- `approveSubmission` / `rejectSubmission` — 写入 audit 表、通过后复制到正式表 + confirmResult 链路
- 修复：`basket_match_data_audit.insert` 在 submit/approve/reject 时调用

**前端**

- Web：M01–M05 页面 + `api/matchData.ts`
- Admin：审核详情页展示 submission 版本 + 材料；审核通过与录入页解耦

**测试**

- 队长/队员权限、双方提交互斥、驳回版本保留、确认幂等

### P1 — 上线前

- M06 争议 Web 端 + 12 小时窗口
- M07 异常申请 `basket_match_exception_application`
- dispute uphold → revision + settlement
- 禁用/保护 web 端裸 CRUD 写 match stat
- Dashboard 待审数与列表数据源统一

### P2 — 可延后

- revision 专用 UI
- settlement batch / sync 历史展示
- 异步结算任务队列
- EVEN_SPLIT 等非本模块项

---

## 第八部分：风险清单

| # | 风险 | 等级 | 说明 |
|---|------|------|------|
| 1 | 待审核数据污染正式统计 | **中** | confirm 前不进 standing；但**同表存储**易误读 |
| 2 | 重复提交 | **高** | Web 无提交；Admin 无「一方锁定」 |
| 3 | 重复审核 | 低 | `updateDataStatus` CAS |
| 4 | 重复累计积分 | 低 | `stat_sync_record` 幂等键 |
| 5 | 同一比赛多个正式结果 | 低 | `official_result` uk_current |
| 6 | 缺少提交版本 | **高** | 驳回覆盖写 |
| 7 | 缺少材料表 | 中 | dispute 用字符串 |
| 8 | 缺少争议完整流程 | **高** | 无 Web；uphold 不改数据 |
| 9 | 缺少异常流程 | 中 | 仅 admin 判负 |
| 10 | 统计同步幂等 | 低 | 已实现 |
| 11 | 前端权限漏洞 | **高** | web CRUD 可直写 stat；无队长校验 |

---

## 第九部分：最终结论（12 问）

| # | 问题 | 答案 |
|---|------|------|
| 1 | M01–M07 是否完整实现 | **否**。Web 全无；Admin 仅有管理员录入/审核/争议/判负，非队长流程 |
| 2 | 是否有队长端提交真实接口 | **否** |
| 3 | 是否有后台比赛数据审核真实接口 | **部分**。待审列表 + 录入页 confirm/reject；非 submission 驱动 |
| 4 | 是否有提交版本表 | **否**（仅有 `data_version` 列 + 未接线的 revision 表） |
| 5 | 是否有审核记录表 | **表有**（`basket_match_data_audit`），**业务未写入** |
| 6 | 是否有争议表 | **是**（`basket_match_data_dispute`），Admin 已用 |
| 7 | 是否有异常比赛申请表 | **否** |
| 8 | 是否有材料关联表 | **否**（dispute 用 file_ids 字符串） |
| 9 | 是否有审核通过同步正式统计 | **是**（`confirmResult` → `official_result` → `rebuildMatch`） |
| 10 | 是否可以直接接页面 | **Admin 部分可接**；**Web 必须先补 API + submission 层** |
| 11 | 是否必须先补 SQL 和后端 submission 层 | **是** |
| 12 | 第二批从哪里开始 | **P0：submission 表 + Web 队长 API + Admin 审核改造 + 禁用 web 裸 CRUD** |

---

## 附录 A：数据校验规则核查

| 规则 | Admin 实现 | Web |
|------|------------|-----|
| 主客四节比分之和 = 最终得分 | ✓ submitScore 校验 | — |
| 球队得分 = 比赛比分 | ✓ validateTeamStatsAgainstMatch | — |
| 球员得分合计 = 比赛总得分 | ✓ validatePlayerStatsConsistency | — |
| 球员必须来自本场 roster | **未实现** | — |
| 投篮/三分/罚球命中 ≤ 出手 | ✓ validateShooting | — |
| 统计非负 | 部分（nvl 默认 0） | — |
| 必须上传结算截图 | **未实现** | — |
| 审核通过前不公开 | ✓ 公开 API 过滤 | — |
| 确认/settlement 幂等 | ✓ sync record | — |

---

## 附录 B：并发与幂等

| 场景 | 当前方案 |
|------|----------|
| 双方队长同时提交 | **未防护**（无 Web 提交） |
| 重复点击提交 | Admin 部分 CAS；Web 无 |
| 双管理员同时审核 | `updateDataStatus` 条件更新 |
| 重复 confirm 累计 | `stat_sync_record` 唯一键 + DuplicateKeyException |
| 争议与审核并发 | **未专门防护** |

---

## 附录 C：关键代码索引

```
basketball-admin/.../BasketMatchScoreServiceImpl.java      — 比分/判负
basketball-admin/.../BasketMatchStatServiceImpl.java       — 统计/confirm/reject
basketball-admin/.../BasketMatchDataAuditServiceImpl.java  — 待审列表（查 match）
basketball-admin/.../BasketMatchDisputeServiceImpl.java    — 争议
basketball-admin/.../BasketMatchStatSyncServiceImpl.java   — 同步幂等
basketball-admin/.../BasketDataSettlementServiceImpl.java — 重算
basketball-web/.../BasketPublicMatchServiceImpl.java       — 公开只读
basketball-web-ui/.../EventTeamManagementView.vue           — M02 占位
basketball-admin-ui/.../match-data-audit/index.vue          — 审核列表
basketball-admin-ui/.../match-data-entry/index.vue          — 录入/确认
docs/sql/cnpa-batch5-migration.sql                          — audit/dispute/official_result
sql/basketball.sql                                          — 正式表基线
docs/cnpa-match-data-design.md                              — 设计文档（与实现有 gap）
```

---

*本报告为第一批只读核查。第二批实施前请确认 P0 范围与 submission 表设计。*
