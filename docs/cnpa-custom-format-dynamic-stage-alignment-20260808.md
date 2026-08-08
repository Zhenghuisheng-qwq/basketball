# CNPA V2 自定义赛制动态阶段能力 — 开发前执行方案对齐

**文档版本**：Alignment v1  
**生成时间**：2026-08-08  
**性质**：只读盘点 + 模型映射 + 影响分析 + 执行计划（**不含任何代码/DDL/数据变更**）  
**依据**：当前仓库真实代码、`docs/sql/` 迁移脚本、`DivisionFormatRulesPanel.vue`、V4 后端 Service/Validator

---

## 执行摘要

当前 V4 动态赛制基础设施（Template → Snapshot → DivisionStage → 执行链路）**已具备 70% 产品模型骨架**，但与最新「自定义 N 阶段」产品方案存在 **4 个核心差距**：

| 差距 | 现状 | 产品要求 |
|------|------|----------|
| 阶段数量 | 后端硬限制 **最多 2 阶段** | 1～N 阶段，末阶段标记「最终阶段」 |
| 维度表达 | `stage_type` + `grouping_mode` + `competition_format` 三字段交叉推断 | 显式 `organizationMode` + `competitionMode` + `bestOf` |
| BO 执行 | `best_of` 字段存在但 **强制 BO1** | 配置支持 BO1/3/5，执行层另轮开发 |
| 参赛来源 | 无 Stage 级 `participantSource` 字段 | 首阶段「本赛区全部队伍」/ 后续「上阶段晋级」需 UI 显式配置 |

**结论**：已具备正式开发条件，但必须先完成本轮对齐评审。**第一轮开发范围**应为 R1「Stage 模型解耦 + N 阶段自定义配置 + 校验/预览」，**不**包含 BO3/BO5 执行。

---

## 1. 当前模型现状

### 1.1 四层数据架构（已存在）

```
basket_format_template（模板头）
  └── basket_format_template_stage（模板阶段定义）
        └── basket_format_template_tie_break_rule（模板同分规则）

basket_division_format_snapshot（Division 规则快照，DRAFT→LOCKED→IN_USE）
  └── basket_division_stage（快照阶段实例，配置 + 执行态）
        └── basket_division_tie_break_rule（快照同分规则）

执行层（LOCKED 后、报名/赛程阶段）：
  basket_division_grouping_batch / group / group_team（分组执行）
  basket_division_stage_team（阶段参赛队）
  basket_division_stage_qualification（晋级预览/确认）
  basket_schedule_batch → basket_match（赛程/比赛）
  basket_match_submission*（提交）→ basket_match_*_stat（正式结果）
  basket_standing + basket_standing_rank_reason（积分榜）
```

模板与自定义 **已共用同一套 Snapshot + Stage 表**（`DivisionFormatSnapshotFactory` 从模板复制到 Snapshot）。快速创建走 `applyFormatPlan` / `rebuildFormatSnapshot`，自定义走逐阶段 CRUD——**底层数据结构一致**，符合产品「禁止两套执行代码」原则。

### 1.2 各核心对象现状

| 对象 | 表/实体 | 当前能力 | 关键限制 |
|------|---------|----------|----------|
| FormatTemplate | `basket_format_template` + `_stage` + `_tie_break_rule` | 种子模板 6+ 种；含 3 阶段示例（REGULAR→PLAYOFF→FINAL） | 模板管理无独立 Admin 页；部分模板已停用 |
| FormatSnapshot | `basket_division_format_snapshot` | DRAFT 可编辑；LOCK 冻结；`is_current` 版本管理 | LOCKED 后不可改；模板后续修改不影响已 LOCK 快照 |
| DivisionStage | `basket_division_stage` | 配置字段齐全；R3 加执行态字段 | **组合校验最多 2 阶段** |
| StageTeam | `basket_division_stage_team` | `entry_type` 表达来源；`source_stage_id/group_id/rank` | 首阶段 INITIAL；后续来自 Qualification |
| Qualification | `basket_division_stage_qualification` | PREVIEW→CONFIRMED；驱动下阶段初始化 | 淘汰晋级用 `MATCH_WINNER`；循环用 TOP_N |
| Match | `basket_match` | 单局比赛；R3 加 bracket 字段 | **无 Series 实体**；`best_of` 未参与执行 |
| ScheduleBatch | `basket_schedule_batch` | `generation_strategy` 标记策略 | 三策略：ROUND_ROBIN / GROUP_STAGE / SINGLE_ELIMINATION |
| Standing | `basket_standing` | 按 `group_scope_key` 分 scope 排名 | 依赖 tie-break 规则链 |
| Tie-break | `basket_division_tie_break_rule` | WIN_COUNT→HEAD_TO_HEAD→POINT_DIFF→TOTAL_POINTS→MANUAL | 循环赛阶段锁定前必填 |

### 1.3 前端现状（DivisionFormatRulesPanel.vue）

- 已支持：**快速创建 / 自定义赛制** 双模式切换
- 已支持：阶段 **新增 / 删除 / 上移 / 下移 / 编辑**
- 已支持：3 种阶段预设（单循环 / 分组循环 / 单败淘汰）
- 已支持：2 阶段组合一键应用（循环→淘汰、小组→淘汰）
- 已支持：赛制预览、锁定前校验、同分规则配置
- **限制**：`bestOf` 写死为 1；阶段形态仅 3 种；后端校验最多 2 阶段时，UI 虽可添加第 3 阶段但 **无法锁定**

---

## 2. 当前 Stage 字段真实语义

### 2.1 配置层字段（`basket_division_stage` / template_stage）

| 字段 | 类型 | 当前真实语义 | 代码证据 |
|------|------|-------------|----------|
| `stage_code` | VARCHAR | 阶段唯一编码，阶段链通过 `next_stage_code` 引用 | `FormatSnapshotReadySpecification` 校验唯一性与链路 |
| `stage_name` | VARCHAR | 展示名称（小组赛、8强赛等） | UI/模板种子 |
| `stage_order` | INT | 执行顺序，必须从 1 连续递增 | `validateStageOrderContinuous` |
| `stage_type` | ENUM | **阶段展示/分类标签**：REGULAR/GROUP/KNOCKOUT/SEMI_FINAL/FINAL/CUSTOM | `StageTypeEnum`；影响 `isGroupStage()` 判断 |
| `competition_format` | ENUM | **竞赛算法**：ROUND_ROBIN / SINGLE_ELIMINATION / MANUAL | `CompetitionFormatEnum`；驱动 ScheduleStrategy 选择 |
| `grouping_mode` | ENUM | **分组方式**（兼组织维度）：NONE / MANUAL / SEEDED / OFFLINE_RANDOM_RESULT | `GroupingModeEnum`；`NONE`=不分组，非 NONE=分组 |
| `group_count` | INT | 分组数量；`grouping_mode≠NONE` 时必填 | `validateGroupingMode` |
| `round_robin_cycles` | INT | 循环次数（单循环=1） | 仅 `competition_format=ROUND_ROBIN` 时校验 |
| `best_of` | INT | BO 局数；**DB 允许任意正奇数，运行时代码强制 =1** | `FormatSnapshotReadySpecification.validateBestOf` |
| `win_points` / `loss_points` | INT | 循环赛胜负积分 | 循环赛必填 |
| `forfeit_win_points` / `forfeit_loss_points` | INT | 弃权积分；**UI 未暴露** | API 有字段 |
| `advancement_type` | ENUM | 晋级方式：NONE / OVERALL_TOP_N / GROUP_TOP_N / SERIES_WINNER / MANUAL | `AdvancementTypeEnum`；SERIES_WINNER 被拒绝 |
| `advancement_count` | INT | 晋级数量（每组 N 或总前 N） | 与 `advancement_type` 联动 |
| `pairing_rule` | ENUM | 淘汰赛对阵：NONE / HIGH_VS_LOW / GROUP_CROSS / MANUAL | 淘汰阶段使用 |
| `next_stage_code` | VARCHAR | 指向下一 `stage_code`；末阶段为空 | 末阶段校验 `advancement_type=NONE` |
| `requires_manual_confirmation` | TINYINT | 淘汰赛晋级是否需人工确认 | 淘汰阶段 UI 可配 |

### 2.2 执行层字段（R3 扩展，非赛制设计数据）

| 字段 | 语义 |
|------|------|
| `stage_status` | 配置态：DRAFT / READY / RUNNING / COMPLETED / LOCKED / CANCELLED |
| `execution_status` | 执行态：DRAFT → READY → SCHEDULE_GENERATED → RUNNING → RESULT_PENDING_CONFIRM → COMPLETED |
| `participant_count` | **实际**参赛队数（执行时写入） |
| `qualified_count` | **实际**晋级队数（执行时写入） |
| `schedule_batch_id` | 关联赛程批次 |

### 2.3 赛程生成策略字段（`basket_schedule_batch`）

| 字段 | 语义 |
|------|------|
| `generation_strategy` | ROUND_ROBIN / **GROUP_STAGE** / SINGLE_ELIMINATION |
| `stage_id` | 所属阶段 |
| `grouping_batch_id` | 小组赛关联分组批次 |

> **重要**：`GROUP_STAGE` 是 **ScheduleBatch 层的合成策略标签**，不是 `competition_format` 枚举值。它表示「先分组、再组内单循环」的编排行为。

---

## 3. 字段概念混用分析

### 3.1 `stage_type` 与组织/竞赛维度混用 — **P0**

| 产品概念 | 当前承载字段 | 问题 |
|----------|-------------|------|
| 参赛组织=分组 | `stage_type=GROUP` **或** `grouping_mode≠NONE` | 两套判断路径，`isGroupStage()` 用 OR 逻辑 |
| 竞赛方式=循环积分 | `competition_format=ROUND_ROBIN` | 命名与产品 `ROUND_ROBIN_POINTS` 不一致但语义接近 |
| 竞赛方式=单败淘汰 | `competition_format=SINGLE_ELIMINATION` **且** 常配 `stage_type=KNOCKOUT` | `stage_type` 冗余 |
| 展示标签（半决赛/决赛） | `stage_type=SEMI_FINAL/FINAL` | 与竞赛方式无强制约束；模板种子有 3 阶段淘汰链 |

**结论**：`stage_type=GROUP` **确实同时承担了**「组织方式=分组」的职责，与 `grouping_mode` 形成双轨判断。产品要求的 `GROUPED` 应由 `grouping_mode≠NONE`（或新 `organization_mode`）表达，不应依赖 `stage_type=GROUP`。

### 3.2 `grouping_mode` 混合了「是否分组」与「如何分组」— **P1**

当前 `grouping_mode` 枚举：

- `NONE` → 不分组（≈ UNGROUPED）
- `MANUAL` / `SEEDED` / `OFFLINE_RANDOM_RESULT` → 既表示「分组」又表示「分组方法」

产品方案将「参赛组织方式（GROUPED/UNGROUPED）」与「分组方式（抽签/种子/人工）」分为两个 UI 维度，当前 **只有一个字段承担两层含义**。

### 3.3 `ScheduleGenerationStrategyEnum.GROUP_STAGE` 合成枚举 — **P1**

产品明确禁止 `GROUP_STAGE_BO1` 类组合枚举。当前 `GROUP_STAGE` 作为 generation_strategy 存在，本质是「分组 + 组内循环」的运行时合成标签，**不是**配置层组合枚举，但增加了理解成本。

### 3.4 计划人数 vs 实际人数 — **P1**

| 概念 | 当前字段 | 层级 | 问题 |
|------|----------|------|------|
| 报名上限（计划） | `basket_division.max_team_count` | Division | 正确，属于计划规则 |
| 预览/校验用队数 | UI `expectedTeamCount` = `maxTeamCount` | 前端 | 锁定前用上限做预览合理 |
| 阶段计划参赛 | **无独立字段** | Stage | 产品 `plannedParticipantCount` 缺失 |
| 阶段计划晋级 | `advancement_count` | Stage | 属于规则计划值，合理 |
| 阶段实际参赛 | `participant_count` | Stage 执行态 | 执行时写入，合理 |
| 阶段实际晋级 | `qualified_count` | Stage 执行态 | 执行时写入，合理 |

**混用风险**：`DivisionFormatPlanValidator` 和 UI `validateV4StageCombination` 用 `maxTeamCount` 校验分组合理性（如 `groupCount > expectedTeamCount`），这是 **计划值校验**，正确。但淘汰赛要求 `maxTeamCount` 为 2 的幂——这是把 **报名上限** 当作 **实际参赛数** 校验，与实际报名 14/16 场景不完全匹配（执行层应处理，配置层不应阻断）。

### 3.5 参赛来源（participantSource）— **P1**

产品要求 Stage 配置「本赛区全部正式参赛队伍」或「阶段 N 晋级队伍」。

当前 **无 `participant_source` 配置字段**，通过隐式规则实现：

- 首阶段（`stage_order=1`）：`StageTeamEntryTypeEnum.INITIAL`，从 `basket_event_team` 加载
- 后续阶段：从上一阶段 `basket_division_stage_qualification`（CONFIRMED）初始化，`entry_type` = OVERALL_RANK / GROUP_RANK / MATCH_WINNER

### 3.6 是否最终阶段 — **P2**

产品要求显式「本阶段为最终阶段」。

当前 **无 `is_final_stage` 字段**，通过以下规则隐式推断：

- 有序列表最后一个阶段
- `advancement_type = NONE`
- `next_stage_code` 为空

---

## 4. 最新产品模型与当前模型映射

### 4.1 维度映射总表

| 产品维度 | 产品枚举 | 当前字段/枚举 | 映射关系 | 差距 |
|----------|----------|--------------|----------|------|
| 阶段名称 | `stageName` | `stage_name` | 1:1 | 无 |
| 阶段顺序 | `stageOrder` | `stage_order` | 1:1 | 无 |
| 是否最终阶段 | boolean | 隐式（末阶段 + NONE 晋级） | 可推导 | 建议加显式字段或 UI 开关 |
| 参赛来源 | participantSource | 隐式（order=1 → 全部；否则上阶段晋级） | 可推导 | 建议加显式字段提升可读性 |
| 参赛组织方式 | GROUPED / UNGROUPED | `grouping_mode`：NONE=UNGROUPED；非 NONE=GROUPED | **部分映射** | `grouping_mode` 还承载分组方法 |
| 分组方式 | 抽签/种子/人工 | `grouping_mode`：MANUAL/SEEDED/OFFLINE_RANDOM_RESULT | 子维度 | 需从 organization 拆出 |
| 竞赛方式 | ROUND_ROBIN_POINTS / SINGLE_ELIMINATION | `competition_format`：ROUND_ROBIN / SINGLE_ELIMINATION | **近似映射** | 命名差异；MANUAL 产品未要求 |
| BO 规则 | BO1/BO3/BO5 | `best_of`：1/3/5 | 字段已有 | **执行层仅支持 1** |
| 小组数量 | groupCount | `group_count` | 1:1 | 无 |
| 循环次数 | cycleCount | `round_robin_cycles` | 1:1 | 无 |
| 积分规则 | winPoints/lossPoints | `win_points` / `loss_points` | 1:1 | 无 |
| 排名规则 | tieBreakRules | `basket_division_tie_break_rule` | 1:N | 无 |
| 晋级规则 | 每组前N/总前N/胜者 | `advancement_type` + `advancement_count` | 1:1 | 淘汰「胜者晋级」用 MATCH_WINNER（执行层）非 advancement_type |
| 计划参赛人数 | plannedParticipantCount | `division.max_team_count`（Division 级） | **级别不同** | Stage 级计划人数缺失 |
| 计划晋级人数 | plannedAdvanceCount | `advancement_count` | 1:1 | 无 |

### 4.2 产品示例 → 当前模型配置

#### 示例 A：16 队小组赛（阶段 1）

| 产品配置 | 当前等价配置 |
|----------|-------------|
| 组织=分组，4 组 | `grouping_mode=MANUAL`（或 OFFLINE_RANDOM_RESULT），`group_count=4` |
| 竞赛=循环积分赛 | `competition_format=ROUND_ROBIN` |
| BO1 | `best_of=1` |
| 每组前 2，晋级 8 | `advancement_type=GROUP_TOP_N`，`advancement_count=2` |
| 参赛来源=本赛区全部 | 隐式：`stage_order=1` |
| stage_type | 当前 UI 预设写 `stage_type=GROUP`（可改为 REGULAR + grouping_mode） |

#### 示例 B：8 强单败 BO3（阶段 2）

| 产品配置 | 当前等价配置 |
|----------|-------------|
| 组织=不分组 | `grouping_mode=NONE` |
| 竞赛=单败淘汰 | `competition_format=SINGLE_ELIMINATION` |
| BO3 | `best_of=3` — **配置可存，锁定/执行会被拒绝** |
| 参赛来源=阶段 1 晋级 | 隐式：`stage_order=2`，`next_stage_code` 指向阶段 1 |
| 胜者晋级 | 执行层 `QualificationTypeEnum.MATCH_WINNER` |

#### 示例 C：现有已验收「4 队单循环」

```
organizationMode = UNGROUPED  →  grouping_mode = NONE
competitionMode  = ROUND_ROBIN_POINTS  →  competition_format = ROUND_ROBIN
bestOf = 1
stage_type = REGULAR（建议废弃语义依赖，保留兼容）
advancement_type = NONE（单阶段即最终）
```

#### 示例 D：现有「5 队单循环 BYE」

同上 UNGROUPED + ROUND_ROBIN + BO1；BYE 由 `BasketV4RoundRobinScheduler` 奇数队轮空处理，与组织/竞赛维度无关。

---

## 5. 推荐最终 Stage 数据结构

### 5.1 推荐配置模型（逻辑层，不一定全部新增列）

```text
DivisionStageConfig {
  // 基本信息
  stageCode, stageName, stageOrder
  isFinalStage: boolean                    // 新增或 UI 推导写回
  participantSource: ALL_DIVISION_TEAMS     // 新增
                  | PREVIOUS_STAGE_ADVANCE  //   | STAGE_ID（可选细化）
                  | MANUAL

  // 三维竞赛模型（产品核心）
  organizationMode: GROUPED | UNGROUPED     // 新增（或规范 grouping_mode 语义）
  groupingMethod: MANUAL | SEEDED | OFFLINE_RANDOM_RESULT  // grouping_mode 收窄
  groupCount: int?                           // GROUPED 时必填

  competitionMode: ROUND_ROBIN_POINTS | SINGLE_ELIMINATION  // 复用 competition_format（可改名）
  roundRobinCycles: int?                    // 复用 round_robin_cycles
  bestOf: 1 | 3 | 5                          // 复用 best_of

  // 积分与排名（循环赛）
  winPoints, lossPoints, forfeitWinPoints, forfeitLossPoints
  tieBreakRules: TieBreakRule[]

  // 晋级（非最终阶段）
  advancementRule: NONE | GROUP_TOP_N | OVERALL_TOP_N | MATCH_WINNER | MANUAL
  advancementCount: int?
  nextStageCode: string?

  // 淘汰专用
  pairingRule: HIGH_VS_LOW | ...
  requiresManualConfirmation: boolean

  // 计划值（可选，不阻断执行）
  plannedParticipantCount: int?             // 新增，默认继承 division.max_team_count
  plannedAdvanceCount: int?                 // 可复用 advancement_count

  // 展示/兼容（降级）
  stageType: string?                        // 降级为展示/报表标签，不参与策略选择
}
```

---

## 6. organizationMode 是否需要新增

**建议：A. 新增 `organization_mode` 字段（推荐）**

| 方案 | 说明 | 优劣 |
|------|------|------|
| A. 新增 `organization_mode` | GROUPED / UNGROUPED；`grouping_mode` 收窄为分组方法 | 语义清晰；增量 DDL；与产品 1:1 |
| B. 仅规范 `grouping_mode` | NONE=UNGROUPED；其余=GROUPED+方法 | 无 DDL；但字段名误导，API 文档难写 |
| C. 推导不写库 | `group_count > 0` 即 GROUPED | 无法表达「分组组织但尚未定 groupCount」 |

**推荐 A**：新增 `organization_mode VARCHAR(32)` 到 `basket_division_stage` 和 `basket_format_template_stage`，默认值按现有数据回填：

```sql
-- 回填逻辑（方案，不执行）
organization_mode = CASE WHEN grouping_mode = 'NONE' THEN 'UNGROUPED' ELSE 'GROUPED' END
```

`isGroupStage()` 改为优先读 `organization_mode == GROUPED`，`stage_type=GROUP` 仅作兼容兜底。

---

## 7. competitionMode 如何处理

**建议：B. 复用 `competition_format`，增加别名映射**

| 现有值 | 产品值 | 处理 |
|--------|--------|------|
| `ROUND_ROBIN` | `ROUND_ROBIN_POINTS` | API/DTO 层增加 display alias；DB 可不改 |
| `SINGLE_ELIMINATION` | `SINGLE_ELIMINATION` | 1:1 |
| `MANUAL` | 无 | 保留供运营兜底；自定义 UI 第一期可隐藏 |

不建议 rename 列（避免大面积迁移）；在 DTO/前端常量中建立双向映射即可。

---

## 8. stageType 如何处理

**建议：D. 降级为展示/分类字段 + E. 逐步废弃策略依赖**

| 选项 | 建议 |
|------|------|
| A. 删除 | **不建议**——模板种子、历史 Snapshot、报表已引用 |
| B. 保留且参与策略 | **当前问题根源**——与 organization/competition 三重推断 |
| C. 仅 UI 自动填充 | **推荐过渡期**——由 organization+competition 推导默认 stage_type |
| D. 展示/报表标签 | **最终态**——如「小组赛」「8强赛」「总决赛」 |

迁移路径：

1. R1：`isGroupStage()` 改读 `organization_mode`；`stage_type` 仅用于 UI 标签和模板展示
2. R1：新 UI 不再暴露 `stage_type` 下拉，由名称 + 维度自动建议标签
3. R2+：文档标记 `stage_type` 为 `@Deprecated for strategy`

---

## 9. bestOf 如何处理

| 层级 | 现状 | 建议 |
|------|------|------|
| 配置层（Snapshot/Stage） | 字段存在；校验强制 =1 | R1 **放开配置**（允许 1/3/5 写入 Snapshot）；锁定校验区分「配置合法」与「执行支持」 |
| 执行层（Schedule/Submission） | `AbstractStageScheduleStrategy.validateBestOfOne()` 拒绝 ≠1 | R2 前：配置 BO3 的阶段可锁定但标记 `executionCapability=BO1_ONLY`，或 R1 仅允许配置 BO1、UI 展示 BO3/5 为「下轮开放」 |
| 产品第一期 | 配置维度要支持 BO1/3/5 | **配置与执行解耦**：R1 存 BO 值；R2 才执行 |

**推荐**：

- R1：DB/DTO/前端均可配置 `best_of`；锁定校验改为「正奇数 1/3/5」；执行层仍拒绝非 1 的赛程生成（明确错误提示）
- R2：实现 MatchSeries 后放开执行

---

## 10. 晋级模型如何处理

### 10.1 当前晋级类型

| advancement_type | 适用场景 | 产品对应 |
|------------------|----------|----------|
| `NONE` | 最终阶段 | 不再晋级 |
| `GROUP_TOP_N` | GROUPED + ROUND_ROBIN | 每组前 N |
| `OVERALL_TOP_N` | UNGROUPED + ROUND_ROBIN | 总排名前 N |
| `MANUAL` | 任意 | 人工确认晋级 |
| `SERIES_WINNER` | BO 系列（预留） | 产品「胜者晋级」在 BO 场景 — **当前被拒绝** |

### 10.2 淘汰「胜者晋级」

当前淘汰阶段 **不在 advancement_type 层配置**，而由：

- `competition_format=SINGLE_ELIMINATION`
- 执行完成后 `QualificationTypeEnum.MATCH_WINNER` 写入 qualification
- 下阶段 `StageTeamEntryTypeEnum.MATCH_WINNER` 初始化

产品「SINGLE_ELIMINATION → 胜者晋级」应在 UI 层 **自动隐含**，advancement 区域对淘汰阶段显示「对阵胜者自动晋级」只读说明。

### 10.3 N 阶段晋级链

当前通过 `next_stage_code` 串联，**已支持任意长度链**（校验器限制为 2）。放开 `StageCombinationValidator` 后，需新增：

- 末阶段：`isFinalStage=true`，`advancement_type=NONE`
- 中间阶段：按 organization+competition 动态展示 advancement 可选项
- 非末淘汰阶段：`advancement_type` 可为 NONE（胜者自动晋级）或显式 `MATCH_WINNER`（建议 R1 统一为隐式）

---

## 11. planned / actual 人数如何处理

### 11.1 拆分方案（本轮仅方案）

| 字段 | 建议位置 | 类型 | 说明 |
|------|----------|------|------|
| `planned_participant_count` | Stage 配置 | INT NULL | 计划参赛数；默认 NULL 表示继承 `division.max_team_count` |
| `planned_advance_count` | Stage 配置 | INT | 复用 `advancement_count` 或别名 |
| `division.max_team_count` | Division | INT | 报名上限（计划上限） |
| `participant_count` | Stage 执行态 | INT | 实际参赛（已有） |
| `qualified_count` | Stage 执行态 | INT | 实际晋级（已有） |
| EventTeam 实际审核通过数 | 执行层 | COUNT | 不以配置校验阻断 |

### 11.2 规则

- 配置/预览层：用 `planned_participant_count` 或 `max_team_count` 估算场次、分组
- 执行层：用 `participant_count`（初始化 StageTeam 时写入）
- **禁止**：实际 14 队时自动修改 Stage 结构或 BO 配置
- 执行层差异处理（R3）：轮空、弃赛、直接晋级、人工调对阵

---

## 12. Snapshot 调整方案

### 12.1 现有冻结机制（已满足产品要求）

| 机制 | 实现 | 评估 |
|------|------|------|
| 状态机 | DRAFT → LOCKED → IN_USE → ARCHIVED | 满足 |
| 锁定写入 | `BasketV4FormatSnapshotServiceImpl.lockSnapshot` | 满足 |
| 锁定后不可编辑 | `DivisionRuleMutableSpecification` 检查 snapshot_status | 满足 |
| 有业务数据不可改规则 | `assertNoBusinessData`（报名/EventTeam/赛程/比赛） | 满足 |
| 模板变更隔离 | Snapshot 从模板 **复制** 阶段；模板后续修改不影响已 LOCK 快照 | 满足 |
| 报名开放后不改关键字段 | `assertCanChangeKeyFields` | 满足 |

### 12.2 R1 需增强

1. **LOCK 冻结内容清单文档化**：在 lock 时序列化完整 Stage 链 JSON 摘要（可选 `snapshot_config_hash` 字段，P2）
2. **新增字段默认值**：R1 migration 对已有 DRAFT 快照回填 `organization_mode`
3. **LOCKED 快照读取兼容**：旧快照无 `organization_mode` 时，按 `grouping_mode` 推导

### 12.3 报名后人数变化

现有设计 **已正确隔离**：报名人数变化不触发 Snapshot 变更。缺口在执行层（赛程发布后增队），属 R3 范围。

---

## 13. 模板调整方案

### 13.1 原则

- 保留「快速创建」与「自定义赛制」双入口
- 模板 = 预置 Stage 链 + 默认参数；应用时 **复制** 到 Snapshot（现有 `rebuildFormatSnapshot` / `applyFormatPlan`）
- 自定义 = 空白 Stage 1 起步，手动添加（现有 custom 模式）
- **同一套** `DivisionStageScheduleStrategy` 执行，禁止分叉

### 13.2 R1 模板工作

| 任务 | 说明 |
|------|------|
| 更新种子模板 | 为模板 stage 补 `organization_mode`；废弃 `stage_type` 策略依赖 |
| 新增多阶段模板种子（可选） | 如「16 队小组→8强→4强→决赛」4 阶段示例（产品示例，非写死） |
| 快速创建方案扩展 | 现有 6 种保留；多阶段模板通过 custom 或新 plan code |
| 停用模板清理 | `V4_LEGACY_*` 含 BO3 的已停用，保持 |

---

## 14. 自定义 UI 调整方案

### 14.1 目标交互（基于现有 DivisionFormatRulesPanel 改造）

```
顶栏：快速创建 | 自定义赛制（保留）
自定义模式：
  ├─ 阶段卡片列表（可拖拽排序）
  │    └─ 每卡片：名称、组织方式、竞赛方式、BO、晋级摘要、是否最终
  ├─ 「+ 添加下一比赛阶段」
  └─ 右侧/底部：完整赛制预览（自然语言 + 场次估算）

阶段编辑抽屉/对话框：
  1. 阶段名称
  2. 参赛来源（首阶段固定 ALL_TEAMS；后续固定 PREV_STAGE）
  3. 参赛组织方式（GROUPED / UNGROUPED）
     └─ GROUPED 时：groupCount、groupingMethod
  4. 竞赛方式（ROUND_ROBIN_POINTS / SINGLE_ELIMINATION）
     └─ 循环赛：cycleCount、winPoints、lossPoints、tieBreakRules
  5. BO 规则（BO1/BO3/BO5）— R1 可选配置，标注执行能力
  6. 晋级规则（动态可选项）
  7. 「本阶段为最终阶段」开关
  8. 操作：删除 / 上移 / 下移
```

### 14.2 动态字段显示规则

| 条件 | 显示字段 |
|------|----------|
| GROUPED | groupCount, groupingMethod |
| UNGROUPED | 隐藏分组字段 |
| ROUND_ROBIN_POINTS | cycleCount, winPoints, lossPoints, tieBreakRules |
| SINGLE_ELIMINATION | pairingRule, requiresManualConfirmation |
| GROUPED + ROUND_ROBIN | advancement: GROUP_TOP_N |
| UNGROUPED + ROUND_ROBIN | advancement: OVERALL_TOP_N |
| SINGLE_ELIMINATION | advancement: 只读「胜者晋级」 |
| isFinalStage | 隐藏晋级与 nextStage |

### 14.3 需修改的前端文件

| 文件 | 改动 |
|------|------|
| `DivisionFormatRulesPanel.vue` | 主改造：维度拆分 UI、N 阶段、最终阶段开关 |
| `v4AdminDisplay.ts` | 校验函数、预览文案、映射常量 |
| `format-template.ts` | DTO 增字段（organizationMode 等） |
| `PublishWizardDialog.vue` | 联动校验调整 |

---

## 15. BO3/BO5 执行是否需要 MatchSeries

### 15.1 现状盘点

| 问题 | 结论 | 证据 |
|------|------|------|
| `basket_match` 是否只能表达一局 | **是** | 一行 = 一场单局；有 home/away score、winner |
| 是否有 MatchSeries 表 | **否** | 全库无 `basket_match_series` |
| submission 是否按 match_id | **是** | `basket_match_submission.match_id` 唯一关联 |
| best_of 是否参与赛程生成 | **否** | 每个对阵生成 1 场 match |
| 淘汰晋级依据 | **Match winner** | `winner_event_team_id` → qualification |

### 15.2 回答产品八个问题

| # | 问题 | 回答 |
|---|------|------|
| 1 | BO3 是否需要 MatchSeries | **是** |
| 2 | BO5 是否需要 MatchSeries | **是** |
| 3 | 一个 BO3 对阵如何关联多个 match | 建议 `basket_match_series` 1:N `basket_match`（series 记录总比分 2-1） |
| 4 | submission 能否继续按单局 Match | **可以**——每局独立提交/审核；series 层汇总晋级 |
| 5 | Series 胜负如何计算 | 先胜 N 局（N = ceil(best_of/2)）；写 `series_winner_event_team_id` |
| 6 | 淘汰晋级依据 | **Series winner**（非单局 winner） |
| 7 | 是否与 Stage 动态配置同轮实现 | **不建议** |
| 8 | 建议拆分 | **R2 独立轮** |

### 15.3 预留字段（cnpa-v2-final-schema.sql 已注释）

`best_of`, `source_match_id_home/away`, `bracket_slot`, `round_no` 已在 match/stage 表预留，但无 series 外键。

---

## 16. MatchSeries 是否本轮开发

**否。本轮（R1）仅：**

- Stage 配置层支持 `best_of` 存储与校验
- UI 展示 BO 选择与「执行能力下轮开放」提示
- 执行层继续拒绝非 BO1 赛程生成（或允许生成但标记 WARNING）

---

## 17. 数据库增量调整清单

> **以下仅为方案，本轮不执行 DDL。**

### A. 复用字段

| 字段 | 说明 |
|------|------|
| `stage_name`, `stage_order`, `stage_code` | 不变 |
| `competition_format` | 映射 competitionMode |
| `group_count`, `round_robin_cycles` | 不变 |
| `best_of` | 放开校验范围 |
| `win_points`, `loss_points`, `forfeit_*` | 不变 |
| `advancement_type`, `advancement_count` | 不变 |
| `next_stage_code`, `pairing_rule` | 不变 |
| `participant_count`, `qualified_count` | 执行态不变 |
| `basket_division_tie_break_rule` | 不变 |

### B. 新增字段（R1）

| 表 | 字段 | 类型 | 说明 |
|----|------|------|------|
| `basket_division_stage` | `organization_mode` | VARCHAR(32) | GROUPED / UNGROUPED |
| `basket_format_template_stage` | `organization_mode` | VARCHAR(32) | 同上 |
| `basket_division_stage` | `is_final_stage` | TINYINT | 0/1，可与末阶段校验互推 |
| `basket_format_template_stage` | `is_final_stage` | TINYINT | 同上 |
| `basket_division_stage` | `participant_source` | VARCHAR(32) | ALL_DIVISION_TEAMS / PREVIOUS_STAGE_ADVANCE |
| `basket_format_template_stage` | `participant_source` | VARCHAR(32) | 同上 |
| `basket_division_stage` | `planned_participant_count` | INT NULL | 计划参赛数（可选） |
| `basket_format_template_stage` | `planned_participant_count` | INT NULL | 同上 |

### C. 废弃字段（逻辑废弃，不删列）

| 字段 | 处理 |
|------|------|
| `stage_type` | 降级为展示标签；策略不再依赖 |
| `grouping_mode` 的「是否分组」语义 | 迁移到 `organization_mode`；保留为分组方法 |

### D. 新增表（R2，非 R1）

| 表 | 说明 |
|----|------|
| `basket_match_series` | BO 系列赛：division_id, stage_id, home/away event_team, best_of, home_wins, away_wins, series_winner, status |
| `basket_match.series_id` | FK 到 match_series（R2 ALTER） |

### E. 暂不需要新增的表（R1）

- Stage 链本身不需要新表（已有多阶段 `basket_division_stage` 行）
- 晋级关系不需要新表（`next_stage_code` + qualification 足够）

### F. 回填迁移脚本（R1 建议新增，不执行）

`docs/sql/cnpa-dynamic-format-r6-organization-mode-migration.sql`：

1. ADD COLUMN organization_mode / is_final_stage / participant_source / planned_participant_count
2. BACKFILL organization_mode FROM grouping_mode
3. BACKFILL is_final_stage FROM stage_order = MAX(stage_order)
4. BACKFILL participant_source：order=1 → ALL_DIVISION_TEAMS，else PREVIOUS_STAGE_ADVANCE

---

## 18. 后端调整清单

### R1 范围

| 模块 | 文件/类 | 改动 |
|------|---------|------|
| DDL | 新 migration r6 | 增字段 + 回填 |
| Entity | `BasketDivisionStage`, `BasketFormatTemplateStage` | 新字段 |
| DTO | `BasketDivisionStageSaveReq`, Snapshot VO | 新字段 + 别名 |
| 校验 | `StageCombinationValidator` | **移除 max 2 限制**；改为通用 N 阶段链校验 |
| 校验 | `FormatSnapshotReadySpecification` | `validateBestOf` 改为 1/3/5；`isGroupStage` 改读 organization_mode |
| 校验 | 新增 `StageAdvancementOptionResolver` | 按 organization+competition 返回合法 advancement 选项 |
| 策略 | `AbstractStageScheduleStrategy.isGroupStage` | 改读 organization_mode |
| 策略 | `AbstractStageScheduleStrategy.validateBestOfOne` | R1 保持仅 BO1 执行 |
| Service | `BasketV4FormatSnapshotServiceImpl` | 锁定逻辑兼容旧快照 |
| Service | `DivisionFormatPlanValidator` | 用 planned 而非强制 max=actual |
| API | `BasketV4FormatRuleController` | DTO 扩展；无新端点 |
| 枚举 | 新增 `OrganizationModeEnum` | GROUPED, UNGROUPED |
| 枚举 | 新增 `ParticipantSourceEnum` | ALL_DIVISION_TEAMS, PREVIOUS_STAGE_ADVANCE |
| 文档 | `cnpa-v2-final-business-flow.md` | 补充 N 阶段自定义章节 |

### 不在 R1

- MatchSeries 相关 Service
- BO3/BO5 赛程生成
- 轮空/弃赛/直接晋级执行

---

## 19. Admin UI 调整清单

| 项 | 说明 |
|----|------|
| `DivisionFormatRulesPanel.vue` | 阶段卡片重构；organization/competition/BO 三维；最终阶段开关；N 阶段添加 |
| `v4AdminDisplay.ts` | 新常量、校验、预览生成器 |
| `format-template.ts` | 类型扩展 |
| 快速创建 Tab | 保留 6 方案；标注与 custom 关系 |
| 赛制预览 | 支持 N 阶段自然语言链 |
| BO3/BO5 | 可选配置 + Tooltip「执行能力将于下版本开放」 |
| `validateFormatPlan` | 组合方案应用前调用（已定义未用） |

---

## 20. Web 端影响

| 模块 | 影响 | R1 是否需要改 |
|------|------|--------------|
| 公开赛事 API | 读 Snapshot 展示赛制摘要 | **只读适配**——解析新字段生成文案 |
| `BasketV4PublicServiceImpl` | `stage_type/competition_format` 映射展示 | 增加 organization_mode 展示 |
| my-events / my-schedule | 不读赛制配置 | 无 |
| 队长比赛提交 | 按 match_id 提交 | 无（R2 BO 才影响） |
| 公开积分榜/赛程 | 读 stage_id / group_id | 无 |

**Web R1 工作量：小**——仅公开展示文案/赛制说明适配。

---

## 21. 现有功能兼容方案

| 已验收能力 | 兼容策略 |
|------------|----------|
| 4 队单循环 | UNGROUPED + ROUND_ROBIN + BO1 + 单阶段 FINAL；回填 organization_mode=UNGROUPED |
| 5 队单循环 BYE | 同上；scheduler 不变 |
| TEAM 报名 | 不涉及赛制配置层 |
| PERSONAL | 不涉及 |
| EventTeam / EventPlayer | 不涉及 |
| StageTeam 初始化 | 首阶段仍从 EventTeam；后续仍从 Qualification |
| schedule 生成 | 三策略不变；isGroupStage 判断源切换 |
| match / submission | 不变 |
| standing / qualification | 不变 |
| my-events / my-schedule | 不变 |

**LOCKED 旧快照**：通过 `organization_mode` 回填脚本兼容；`stage_type=GROUP` 仍可作为 fallback。

---

## 22. 推荐开发轮次

### R1：Stage 模型解耦 + N 阶段自定义配置（4～6 周）

| 层 | 内容 |
|----|------|
| 数据库 | r6 migration：organization_mode 等 4 字段 + 回填 |
| Admin 后端 | 校验器放开 N 阶段；枚举/DTO/Service 扩展；执行仍 BO1 |
| Admin 前端 | DivisionFormatRulesPanel 三维配置 + N 阶段 UI |
| Web 后端 | 公开展示文案适配 |
| Web 前端 | 赛制说明组件适配（如有） |
| 验收 | 见 §23 R1 |

### R2：BO3/BO5 MatchSeries 执行（3～4 周）

| 层 | 内容 |
|----|------|
| 数据库 | basket_match_series + match.series_id |
| Admin 后端 | 系列赛程生成；submission 按局；series 汇总；淘汰按 series winner |
| Admin 前端 | 系列赛比分展示；审核页 |
| Web 后端 | 公开赛程 series 展示 |
| Web 前端 | 队长按局提交（或按 series 提交向导） |
| 验收 | BO3 淘汰完整链路 |

### R3：执行层弹性（2～3 周）

| 层 | 内容 |
|----|------|
| 执行 | 轮空、弃赛、直接晋级、人工调对阵、报名后增队 |
| 验收 | 14/16 队场景；赛程发布后调整 |

---

## 23. 每轮验收标准

### R1 验收

- [ ] 自定义模式：默认阶段 1，可添加至 ≥4 阶段，末阶段标记最终
- [ ] 每阶段可独立配置 GROUPED/UNGROUPED + 竞赛方式 + BO（1/3/5 可存）
- [ ] 16 队小组→8强→4强→决赛 配置可保存并 LOCK
- [ ] LOCK 后模板修改不影响 Snapshot
- [ ] 报名 14/16 队不自动改 Stage 结构
- [ ] 现有 4 队单循环发布门禁 **52 PASS 回归通过**
- [ ] 现有 5 队 BYE 回归通过
- [ ] PERSONAL 成队 smoke 回归通过
- [ ] BO3 配置可存但执行提示「下版本支持」或拒绝生成赛程

### R2 验收

- [ ] UNGROUPED + SINGLE_ELIMINATION + BO3：完整淘汰赛链路
- [ ] 系列胜负正确；晋级按 series winner
- [ ] 每局独立 submission/审核

### R3 验收

- [ ] 报名不足 16 仍可执行小组赛
- [ ] 赛程发布后增队有运营处理路径
- [ ] 弃赛/直接晋级不污染 standing

---

## 24. P0/P1/P2 风险

| # | 风险 | 级别 | 缓解 |
|---|------|------|------|
| 1 | `stage_type` 与 organization/competition 三重推断导致策略选错 | **P0** | R1 改 `isGroupStage()` 单一数据源 |
| 2 | 放开 N 阶段后 `StageCombinationValidator` 遗漏非法链 | **P0** | 通用 DAG 校验 + 单测矩阵 |
| 3 | 旧 LOCKED 快照无新字段 | **P0** | 回填 + 读取 fallback |
| 4 | 赛程生成器仅支持 2 阶段组合测试 | **P0** | 补充 3/4 阶段生成集成测试 |
| 5 | Standing 在 GROUPED 切换 organization 后 scope 错误 | **P1** | 保持 group_scope_key 逻辑不变 |
| 6 | Qualification 对多阶段淘汰链 MATCH_WINNER 传播 | **P1** | 多阶段 E2E |
| 7 | BO3 配置可 LOCK 但无法执行引发运营困惑 | **P1** | UI 明确标注；或 LOCK 时仅允许 BO1 |
| 8 | 模板种子与新区 organization_mode 不一致 | **P1** | 种子脚本同步更新 |
| 9 | 自定义 UI 复杂度上升导致校验遗漏 | **P1** | 前后端共用 `StageAdvancementOptionResolver` |
| 10 | `max_team_count` 与淘汰赛 2 的幂校验过严 | **P1** | 改为 planned 估算 + 执行层处理 |
| 11 | 现有 data-check 脚本未覆盖新字段 | **P2** | 扩展 r6-data-check.sql |
| 12 | 线上准备阶段改造范围需冻结 | **P2** | R1 仅配置层，不动执行主链路 |

---

## 25. 是否已经具备正式开发条件

**是，具备 R1 开发条件。**

前提：

1. 本轮对齐文档评审通过（本文档）
2. 确认 R1 不包含 BO3/BO5 执行
3. 确认 N 阶段上限（建议首期软限制 ≤8 阶段，防滥用）
4. 现有发布门禁测试纳入 R1 回归 CI

---

## 26. 第一轮开发建议范围

### 纳入 R1

1. DDL：organization_mode、is_final_stage、participant_source、planned_participant_count
2. 后端：N 阶段链校验；维度解耦；best_of 配置放开（1/3/5）；执行仍 BO1
3. Admin UI：DivisionFormatRulesPanel 按产品方案改造
4. 模板种子：补 organization_mode 回填
5. 文档：更新 `cnpa-v2-final-business-flow.md` 自定义赛制章节
6. 测试：4 队 RR + 5 队 BYE + 16→8→4→2 四阶段配置 LOCK 冒烟

### 明确不纳入 R1

1. MatchSeries / BO3/BO5 执行
2. 轮空/弃赛/直接晋级执行层
3. 删除 `stage_type` 列
4. 重构 submission / standing 核心算法
5. 修改已 LOCKED 历史快照数据

---

## 附录 A：关键代码索引

| 用途 | 路径 |
|------|------|
| 阶段组合校验（2 阶段限制） | `basketball-admin/.../stage/StageCombinationValidator.java:23-25` |
| BO1 强制 | `basketball-admin/.../FormatSnapshotReadySpecification.java` |
| BO1 执行拒绝 | `basketball-admin/.../stage/AbstractStageScheduleStrategy.java:57-61` |
| 快照锁定 | `basketball-admin/.../BasketV4FormatSnapshotServiceImpl.java` |
| 规则可变性 | `basketball-admin/.../DivisionRuleMutableSpecification.java` |
| 小组赛策略 | `basketball-admin/.../stage/GroupStageStageScheduleStrategy.java` |
| 单循环策略 | `basketball-admin/.../stage/RoundRobinStageScheduleStrategy.java` |
| 淘汰策略 | `basketball-admin/.../stage/SingleEliminationStageScheduleStrategy.java` |
| 阶段执行/晋级 | `basketball-admin/.../DivisionStageExecutionApplicationServiceImpl.java` |
| 赛制 UI | `basketball-admin-ui/.../DivisionFormatRulesPanel.vue` |
| UI 校验常量 | `basketball-admin-ui/src/utils/v4AdminDisplay.ts` |
| R1 DDL 基础 | `docs/sql/cnpa-dynamic-format-r1-migration.sql` |
| V4 match DDL | `docs/sql/cnpa-v4-r1-ddl.sql` |

## 附录 B：GROUP_STAGE 职责混用结论

**产品问题：「GROUP_STAGE 是否同时承担组织方式=分组 + 竞赛方式=循环积分？」**

**答案：是，在运行时层面混用；在存储层面部分分离。**

- **存储层**：`grouping_mode`（组织）+ `competition_format=ROUND_ROBIN`（竞赛）已分开
- **推断层**：`stage_type=GROUP` 或 `grouping_mode≠NONE` 均可触发分组行为（双轨）
- **执行层**：`ScheduleGenerationStrategyEnum.GROUP_STAGE` 将「分组 + 组内循环」合并为一个编排策略

R1 目标即是消除推断层混用，使产品三维模型（organization / competition / bestOf）与代码一一对应。

---

*本文档由开发前对齐盘点自动生成，不包含任何代码或数据库变更。*
