# CNPA 动态赛制重构 R2 总结

## 1. 本轮目标

在 R1 已完成的 Series → Season → Division 主线与动态赛制模板/快照基础上，补齐以下业务闭环：

- R1 快照版本与唯一约束修复
- Division DRAFT 规则可视化编辑
- 历史 Division 规则快照幂等补齐
- TEAM 球队报名链路完善（来源字段、动态人数、审核重校验）
- PERSONAL 个人报名基础链路
- 线下个人抽签组队结果录入
- 队长建队、成员加入与正式参赛转换
- 正式参赛球队的赛事分组结果管理
- 明确区分「个人抽签组队」与「赛事球队分组」

## 2. 实际完成内容

### 已完成

1. **R1 遗留修复**
   - `basket_division_format_snapshot` 增加 `snapshot_version`、`is_current`、`active_unique_key` 等字段
   - 唯一索引从 `(division_id, is_delete)` 改为 `uk_division_current_snapshot (active_unique_key)`
   - `basket_format_template` 唯一索引改为 `(template_code, version_no)` 永久唯一
   - 历史快照保留、多次重建留痕（markSuperseded，不物理删除旧快照）

2. **Division DRAFT 规则编辑**
   - 后端：`BasketV4FormatRuleController` + `BasketV4FormatSnapshotServiceImpl` 阶段 CRUD、排序、同分规则、重建、锁定
   - 前端：`publish/index.vue` 赛制规则页签支持阶段卡片、编辑 Dialog、上移/下移、LEAGUE 晋级 N 配置

3. **历史 Division 快照补齐**
   - `BasketV4FormatSnapshotMigrationRunner`（默认关闭，`cnpa.v4.format-snapshot-migration.enabled=true`）
   - 按业务数据自动标记 DRAFT / LOCKED / IN_USE

4. **TEAM 报名完善**
   - `basket_registration` 增加 `registration_source`、`source_personal_draw_team_id`、`source_personal_draw_batch_id`
   - `TeamRegistrationEligibilitySpecification` + 审核时重校验
   - `BasketV4ParticipationSupport.buildRegistrationPlayers` 修复 `rows` 初始化 bug

5. **PERSONAL 个人报名**
   - 新表：`basket_personal_registration`、`basket_personal_registration_audit`
   - Admin/Web 审核与提交链路
   - 解除 R1「PERSONAL 永远禁止开放报名」拦截，改为快照 ready + lock 后开放

6. **个人线下抽签组队**
   - 新表：`basket_personal_draw_batch`、`basket_personal_draw_team`、`basket_personal_draw_assignment`
   - Admin 批次/临时队/分配/确认/取消/绑定/转换

7. **成队转换**
   - `PersonalFormationRegistrationFactory` + `PersonalTeamFormationApplicationServiceImpl`
   - 绑定长期球队 → 确认成队 → 生成 `basket_registration`(PERSONAL_FORMATION) → EventTeam/EventPlayer

8. **赛事球队分组**
   - 新表：`basket_division_grouping_batch`、`basket_division_group`、`basket_division_group_team`
   - 仅使用 `event_team_id`，与个人抽签完全隔离
   - Admin 分组批次创建、手工分配、锁定

9. **Admin 页签**
   - 报名审核：球队报名 / 个人报名 / 个人组队结果
   - 参赛队伍：参赛球队 / 参赛球员 / 赛事分组

10. **Web 个人中心**
    - 可参与赛事合并 TEAM + PERSONAL
    - 我的报名合并球队与个人记录
    - `V4PersonalRegistrationFlow.vue` 个人报名
    - `V4PersonalFormationPanel.vue` 成队任务

### 未完成（按本轮范围刻意排除）

- 在线随机抽签/自动分组
- 动态赛程、MatchSeries、BO 多场次、积分结算、晋级计算
- 完整冒烟测试
- V2 旧表/旧代码清理

## 3. 数据库变更

### 修改表

| 表 | 变更 |
|----|------|
| `basket_division_format_snapshot` | +snapshot_version, is_current, active_unique_key, superseded_snapshot_id, superseded_time；新唯一索引 |
| `basket_format_template` | uk_template_code_version 去掉 is_delete |
| `basket_registration` | +registration_source, source_personal_draw_team_id, source_personal_draw_batch_id |

### 新增表

| 表 | 用途 |
|----|------|
| `basket_personal_registration` | 个人报名主表 |
| `basket_personal_registration_audit` | 个人报名审核流水 |
| `basket_personal_draw_batch` | 线下抽签批次 |
| `basket_personal_draw_team` | 抽签临时队伍 |
| `basket_personal_draw_assignment` | 球员分配到临时队 |
| `basket_division_grouping_batch` | 赛事分组批次 |
| `basket_division_group` | A/B/C 小组 |
| `basket_division_group_team` | EventTeam 分配到小组 |

### 关系

```
Division ──1:N──> basket_division_format_snapshot (is_current=1 仅一条 current)
PersonalRegistration ──N:1──> Division
PersonalDrawBatch ──1:N──> PersonalDrawTeam ──1:N──> PersonalDrawAssignment ──N:1──> PersonalRegistration
PersonalDrawTeam ──转换──> basket_registration (PERSONAL_FORMATION) ──> EventTeam
DivisionGroupingBatch ──1:N──> DivisionGroup ──1:N──> DivisionGroupTeam ──N:1──> EventTeam
```

## 4. R1 遗留修复

| 问题 | 修复方式 |
|------|----------|
| 快照 `(division_id, is_delete)` 多次重建冲突 | `active_unique_key=DIVISION:{id}` + `is_current` + `snapshot_version` 递增 |
| 模板版本逻辑删除后占号 | `(template_code, version_no)` 永久唯一 |
| LEAGUE 常规赛 advancement_count 无法配置 | DRAFT 阶段编辑 UI + API |
| 历史 Division 无快照 | MigrationRunner + backfill SQL 指引 |

## 5. 规则编辑能力

### 可编辑字段

阶段：stageCode/Name/Order/Type、competitionFormat、groupingMode/groupCount、roundRobinCycles、bestOf、积分四字段、advancementType/Count、pairingRule、nextStageCode、requiresManualConfirmation；循环赛同分规则。

### 可编辑条件（DivisionRuleMutableSpecification）

Division=DRAFT、快照=DRAFT、is_current=1、无有效球队报名、无个人报名、无 EventTeam、无赛程/比赛。

### 锁定条件

`FormatSnapshotReadySpecification` 校验通过后 `lockSnapshot` → LOCKED。

### 不支持

JSON 规则 DSL、拖拽设计器、在线执行阶段/积分/晋级。

## 6. 球队报名流程

```
队长授权 → Web save/submit (TEAM_DIRECT)
  → TeamRegistrationEligibilitySpecification
  → basket_registration + basket_registration_player
  → Admin approve + 重校验
  → BasketV4EventSnapshotServiceImpl.generateFromApprovedRegistration
  → basket_event_team + basket_event_player
```

## 7. 个人报名流程

```
Web 个人报名 save/submit
  → PersonalRegistrationEligibilitySpecification
  → basket_personal_registration (PENDING_REVIEW)
  → Admin approve
  → 线下抽签（系统外）
  → Admin 录入 basket_personal_draw_batch/team/assignment
  → confirm batch → personal_registration=ASSIGNED
  → 队长绑定长期球队 + 邀请成员
  → confirm formation
  → PersonalFormationRegistrationFactory → basket_registration (PERSONAL_FORMATION)
  → EventTeam/EventPlayer
```

## 8. 两类分组的区别

| | 个人抽签组队 | 赛事球队分组 |
|--|-------------|-------------|
| 对象 | 个人球员 PersonalRegistration | 正式参赛 EventTeam |
| 表 | basket_personal_draw_* | basket_division_grouping_* |
| 目的 | 组成长期球队并转正式报名 | 小组赛 A/B/C 分配 |
| 前置 | 个人报名审核通过 | 球队报名审核通过、EventTeam 已生成 |

## 9. 状态机与关键状态

- **PersonalRegistrationStatus**：DRAFT → PENDING_REVIEW → APPROVED → ASSIGNED → CONVERTED
- **DrawBatchStatus**：DRAFT → CONFIRMED → CONVERTED / CANCELLED
- **FormationStatus**：DRAFT → CAPTAIN_ASSIGNED → TEAM_FORMING → READY_TO_CONFIRM → CONFIRMED → CONVERTED
- **GroupingStatus**：DRAFT → LOCKED / CANCELLED
- **SnapshotStatus**：DRAFT → LOCKED → IN_USE → ARCHIVED

## 10. 使用的设计模式

| 模式 | 类 |
|------|-----|
| Specification | DivisionRuleMutableSpecification, FormatSnapshotReadySpecification, TeamRegistrationEligibilitySpecification, PersonalRegistrationEligibilitySpecification, PersonalDrawBatchConfirmSpecification, PersonalTeamFormationConfirmSpecification, DivisionGroupingLockSpecification |
| Factory | PersonalFormationRegistrationFactory, DivisionFormatSnapshotFactory |
| Application Service | PersonalRegistrationApplicationServiceImpl, PersonalTeamFormationApplicationServiceImpl, DivisionGroupingApplicationServiceImpl |
| 状态转换 | PersonalRegistrationStatusTransition |

## 11. 后端文件清单（核心）

### Controller
- `BasketV4FormatRuleController` — `/basket/v4/divisions/{id}/format-*`
- `BasketV4PersonalRegistrationController` — Admin 个人报名
- `BasketV4PersonalDrawController` — Admin 抽签组队
- `BasketV4DivisionGroupingController` — Admin 赛事分组
- Web: `BasketV4PersonalRegistrationController`, `BasketV4PersonalFormationController`

### Service
- `BasketV4FormatSnapshotServiceImpl`（扩展阶段编辑）
- `PersonalRegistrationApplicationServiceImpl`
- `PersonalTeamFormationApplicationServiceImpl`
- `DivisionGroupingApplicationServiceImpl`
- `BasketV4RegistrationAuditServiceImpl`（审核重校验）

### Migration
- `BasketV4FormatSnapshotMigrationRunner`

## 12. 前端文件清单 — Admin

- `basketball-admin-ui/src/views/cnpa/event-v4/publish/index.vue` — 赛制规则 DRAFT 编辑
- `basketball-admin-ui/src/views/cnpa/event-v4/registration-audit/index.vue` — 三页签审核
- `basketball-admin-ui/src/views/cnpa/event-v4/event-team/index.vue` — 赛事分组页签
- `basketball-admin-ui/src/api/cnpa/event-v4/format-template.ts`
- `basketball-admin-ui/src/api/cnpa/event-v4/personal-registration.ts`
- `basketball-admin-ui/src/api/cnpa/event-v4/personal-draw.ts`
- `basketball-admin-ui/src/api/cnpa/event-v4/division-grouping.ts`
- `basketball-admin-ui/src/utils/v4AdminDisplay.ts`

## 13. 前端文件清单 — Web

- `basketball-web-ui/src/views/account/events/MyEventsView.vue`
- `basketball-web-ui/src/views/account/events/V4PersonalRegistrationFlow.vue`
- `basketball-web-ui/src/components/account/events/V4PersonalFormationPanel.vue`
- `basketball-web-ui/src/api/v4PlayerEvents.ts`
- `basketball-web-ui/src/utils/v4AccountEventsMappers.ts`

## 14. SQL 文件

- `docs/sql/cnpa-dynamic-format-r2-migration.sql`
- `docs/sql/cnpa-dynamic-format-r2-data-check.sql`
- `docs/sql/cnpa-dynamic-format-r2-backfill.sql`
- `docs/sql/cnpa-dynamic-format-r2-rollback.sql`

## 15. 局部验证结果

| 检查项 | 结果 |
|--------|------|
| 快照版本字段与 active_unique_key 逻辑 | 代码路径已实现；需执行 migration 后 DB 验证 |
| DRAFT 阶段 CRUD / 排序 / 同分规则 API | Controller + Service 已实现 |
| LEAGUE 晋级 N 编辑入口 | publish 规则页签 + stage Dialog |
| TEAM 5V5/3V3 人数校验 | RosterLimit + TeamRegistrationEligibilitySpecification |
| PERSONAL 开放报名 | openRegistration 移除永久拦截 |
| 个人/赛事分组表隔离 | 独立表 + 规格类约束 event_team_id |
| 无自动随机抽签/分组 | 代码检索无 Random 分配逻辑 |
| Maven 编译 | 本地环境未安装 mvn，未执行编译 |

## 16. 未完成范围

- 在线随机抽签、在线随机赛事分组
- 小组赛/淘汰赛赛程生成、MatchSeries、BO 多场次
- 动态积分、同分排名执行、晋级、冠军、归档
- Flowable、JSON 规则 DSL
- 完整冒烟测试
- V2 旧表和旧代码删除

## 17. 风险与问题

| 级别 | 问题 |
|------|------|
| P0 | 生产需先执行 R2 migration，否则新字段/表不存在会导致运行失败 |
| P1 | 本地未 Maven 编译验证，可能存在遗漏 import 或 XML 映射错误 |
| P1 | Web 成队任务绑定球队当前为手动输入 teamId，后续可接球队选择器 |
| P2 | Admin 权限菜单 seed 需运维同步 `basket:v4FormatRule:*`、`basket:v4PersonalReg:*` 等 |
| P2 | 历史 PS/XBOX Division 合并仍属人工处理，本轮未自动合并 |

## 18. 第 3 轮前置条件

1. R2 migration + seed 已在目标环境执行并通过 data-check
2. 至少一个 Division 完成：规则锁定 → TEAM/PERSONAL 报名 → EventTeam 生成
3. 赛事分组批次已 LOCKED（如需小组赛）
4. 规则快照阶段定义完整（groupCount、advancementCount、同分规则）
5. R3 可基于 `basket_division_stage` + LOCKED 分组批次生成赛程

---

*文档生成时间：2026-08-05*
*基于仓库实际修改填写，非预先假设全部完成。*
