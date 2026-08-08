# CNPA V2 自定义赛制 R1 — 开发计划最终修正与冻结

**文档版本**：R1 Plan Frozen v1  
**生成时间**：2026-08-08  
**前置文档**：`docs/cnpa-custom-format-dynamic-stage-alignment-20260808.md`（Alignment v1）  
**性质**：最终可执行开发方案（**本轮不写代码、不执行 DDL、不修改数据库**）  
**状态**：**已冻结，下一轮可直接进入编码**

---

## 冻结声明

本文档在 Alignment v1 基础上，根据产品评审意见完成 **R1 范围收缩、线性链约束、字段落库决策、晋级语义统一、BO 执行门禁** 等修正。除本文档明确列入 R1 的事项外，一律不做。

---

## 一、冻结后的产品模型（摘要）

```
自定义赛制 = Stage 1 → Stage 2 → … → Stage N（严格线性链，1～N）

每个 Stage 三个独立维度：
  organizationMode   : GROUPED | UNGROUPED
  competition_format: ROUND_ROBIN | SINGLE_ELIMINATION
  best_of           : 1 | 3 | 5

禁止组合枚举（如 GROUP_STAGE_BO1）。
禁止 DAG / 分叉 / 多来源汇流（R1）。
```

---

## 二、逐项答复（30 问）

### 1. 是否接受 R1 只新增 `organization_mode`？

**接受。**

R1 数据库增量 **仅新增** `organization_mode VARCHAR(32)`，同步至：

- `basket_division_stage`
- `basket_format_template_stage`

取值：`GROUPED` / `UNGROUPED`。

其余三个候选字段（`is_final_stage`、`participant_source`、`planned_participant_count`）经重新评估后 **R1 不落库**，理由见下文 2～4 节。

---

### 2. `is_final_stage` 为什么不需要落库？

**最终阶段由 Stage 链拓扑唯一确定，不需要第二事实源。**

冻结判定条件（三者同时满足）：

1. `stage_order` = 当前 Snapshot 内最大 `stage_order`
2. `next_stage_code IS NULL`
3. `advancement_type = NONE`

UI「本阶段为最终阶段」开关是 **配置操作语义**：提交时系统自动将当前 Stage 设为末阶段（清空 `nextStageCode`、设 `advancementType=NONE`），并取消原末阶段标记（若存在）。

若落库 `is_final_stage`，将与 `next_stage_code`、`advancement_type` 形成三套互相独立的“末阶段”事实，极易出现 `is_final_stage=1` 但 `next_stage_code` 仍有值的冲突。线性链下，**拓扑即语义**，无需冗余列。

---

### 3. `participant_source` 为什么 R1 不需要落库？

**R1 来源规则固定且可由 `stage_order` 严格推导。**

| stage_order | 参赛来源（固定） |
|-------------|------------------|
| `== 1` | 本 Division 全部正式 EventTeam |
| `> 1` | 上一 Stage 已确认晋级队伍 |

Admin UI 只读展示：

- 阶段 1：「本赛区全部正式参赛队伍」
- 阶段 2+：「上一阶段晋级队伍」

R1 **不允许**管理员选择其他来源（外卡、跨 Stage 引用、败者组等）。待未来出现 DAG / 双败 / 外卡时再引入 `participant_source` 字段。

当前代码已隐式实现：`DivisionStageExecutionApplicationServiceImpl.initializeStageTeams()` 首阶段读 `basket_event_team`，后续阶段读 `basket_division_stage_qualification`（CONFIRMED）。

---

### 4. `planned_participant_count` 为什么 R1 不需要落库？

**计划人数可由 Division 上限 + 晋级规则实时推导，落库会造成双事实源。**

| Stage | 计划参赛人数推导 |
|-------|------------------|
| Stage 1 | `division.max_team_count`（报名审核上限，非实际人数） |
| Stage 2+ | 上一 Stage 的晋级规则输出（如 4 组×每组前 2 = 8 支） |

UI 预览、场次估算 **实时计算**，不持久化。若上一阶段晋级规则从「每组前 2」改为「每组前 3」，持久化的 `planned_participant_count` 将过期。

执行层继续使用已有 `participant_count`（`basket_division_stage` 执行态字段）记录 **实际** 参赛人数。

---

### 5. 最终 Stage 三维模型

| 维度 | 字段 | 枚举 | 职责 |
|------|------|------|------|
| **参赛组织方式** | `organization_mode`（**R1 新增**） | `GROUPED` / `UNGROUPED` | 是否分组；**是否分组的唯一权威** |
| **竞赛方式** | `competition_format`（复用） | `ROUND_ROBIN` / `SINGLE_ELIMINATION` | 分组/不分组之后怎么比赛 |
| **BO 规则** | `best_of`（复用） | `1` / `3` / `5` | 对阵局数配置；R1 可存，R2 才执行 |

产品展示映射（仅 UI 文案，不改 DB 枚举）：

```
GROUPED + ROUND_ROBIN + BO1  → 「小组循环积分赛 BO1」
UNGROUPED + SINGLE_ELIMINATION + BO3 → 「淘汰赛 BO3」
```

---

### 6. `grouping_mode` 最终职责

**只表达「怎么分组」，不再表达「是否分组」。**

| organization_mode | grouping_mode | group_count |
|-------------------|---------------|-------------|
| `UNGROUPED` | 必须为 `NONE` | 必须 NULL 或 0 |
| `GROUPED` | 必须为 `MANUAL` / `SEEDED` / `OFFLINE_RANDOM_RESULT` | 必须 > 0 |

校验（R1 新增/强化）：

- `UNGROUPED` + `grouping_mode != NONE` → 拒绝
- `GROUPED` + `group_count <= 0` → 拒绝
- `GROUPED` + `grouping_mode == NONE` → 拒绝

---

### 7. `stage_type` 最终职责

**R1 不删列；从核心业务策略中彻底降级。**

| 允许用途 | 禁止用途 |
|----------|----------|
| 历史兼容 | 判断是否分组 |
| 展示标签（半决赛、总决赛） | 选择 Scheduler |
| 报表分类 | Standing scope |
| UI 辅助展示 | 晋级算法 |
| | 比赛执行方式 |

**R1 必须切换的 `stage_type` 策略依赖点（盘点清单）**：

| # | 文件 | 当前用法 | R1 改法 |
|---|------|----------|---------|
| 1 | `AbstractStageScheduleStrategy.isGroupStage()` | `stage_type==GROUP` OR `grouping_mode!=NONE` | 只读 `organization_mode==GROUPED`；旧数据 fallback |
| 2 | `GroupStageStageScheduleStrategy.supports()` | 同上 | 同上 |
| 3 | `RoundRobinStageScheduleStrategy.supports()` | `!isGroupStage()` | 同上 |
| 4 | `StageCombinationValidator.isGroupStage()` | 同上 | 同上 |
| 5 | `DivisionStageExecutionApplicationServiceImpl.isGroupStage()` | 同上 | 同上 |
| 6 | `TieBreakStandingService.isGroupStage()` | 同上 | 同上 |
| 7 | `BasketV4FormatSnapshotServiceImpl.applyFormatPlan()` | 写入 `stage_type=GROUP/KNOCKOUT` | 保留写入（展示用）；同步写 `organization_mode` |
| 8 | `DivisionFormatRulesPanel.vue` / `v4AdminDisplay.ts` | `isV4GroupCompetitionStage` 含 `stageType==GROUP` | 改读 `organizationMode` |
| 9 | `stats/index.vue` | `stageType === 'GROUP'` | 改读 `organizationMode` |
| 10 | `BasketV4PlayerMyScheduleServiceImpl` | `stageType` 判断展示 | R1 可保留展示；不用于策略 |

保存 Stage 时：`stage_type` 由 `organization_mode + competition_format` **自动推导填充**（如 GROUPED+ROUND_ROBIN→`GROUP`，UNGROUPED+SINGLE_ELIMINATION→`KNOCKOUT`），管理员不手选。

---

### 8. `competition_format` 最终职责

**竞赛算法唯一配置字段（复用，不改名）。**

| 值 | 含义 | 产品 UI 文案 |
|----|------|-------------|
| `ROUND_ROBIN` | 循环积分赛 | 「循环积分赛」 |
| `SINGLE_ELIMINATION` | 单败淘汰 | 「单败淘汰赛」 |
| `MANUAL` | 人工赛制 | R1 自定义 UI 不暴露；保留供历史/运营兜底 |

**不新增** `ROUND_ROBIN_POINTS` 数据库枚举。

---

### 9. `best_of` 最终职责

| 层级 | R1 行为 |
|------|---------|
| **配置层** | 允许 `1` / `3` / `5`；Snapshot LOCK 校验「正奇数且 ∈ {1,3,5}」 |
| **执行层** | `best_of > 1` 时 **禁止生成正式赛程**（见 §15 门禁） |
| **展示层** | UI 显示 BO1/BO3/BO5 |

配置与执行 **刻意解耦**：赛制可提前设计完整 BO 链，执行能力 R2 补齐。

---

### 10. N Stage 线性链校验规则

**替换** `StageCombinationValidator` 当前「最多 2 阶段」限制，改为线性链校验器 `LinearStageChainValidator`（可重构现有类）：

| # | 规则 |
|---|------|
| 1 | Stage 数量 ≥ 1 |
| 2 | `stageOrder` 从 1 连续递增，无重复 |
| 3 | `stageCode` 全局唯一（同一 Snapshot 内） |
| 4 | 非末 Stage：`nextStageCode` 必须指向 `stageOrder + 1` 的 Stage |
| 5 | 末 Stage：`nextStageCode` 必须为空 |
| 6 | 非末 Stage：必须有有效晋级规则（见 §12） |
| 7 | 末 Stage：`advancement_type` 必须为 `NONE` |
| 8 | 不允许环（next 链无回指） |
| 9 | 不允许跳级（next 的 order 必须 = 当前 order + 1） |
| 10 | 不允许分叉（每个 Stage 最多一个 next） |
| 11 | 不允许 orphan（除 Stage 1 外，每个 Stage 必须被前一 Stage 的 next 指向） |

**阶段数量上限**：底层 **不设硬限制**。UI 可选软限制（如最多 8 个 Stage 防误操作），**非必须**；若产品无滥用顾虑，可不加。

---

### 11. 为什么不做 DAG

| 原因 | 说明 |
|------|------|
| 产品范围 | R1 目标是「自定义线性赛制」，非通用赛制引擎 |
| 复杂度 | DAG 需 `participant_source`、多父节点、拓扑排序、冲突检测 |
| 未来扩展 | 双败、败者组、多来源汇流 **明确后置**；届时再升级数据模型 |
| 现有代码 | 晋级初始化、StageTeam 来源、`next_stage_code` 均假设单前驱 |

R1 校验器 **禁止** 设计通用 DAG 算法，只实现线性链。

---

### 12. 淘汰赛「胜者晋级」最终建模建议

#### 当前真实枚举与调用关系

| 层级 | 枚举 | 用途 | 现状 |
|------|------|------|------|
| **Stage 配置** | `AdvancementTypeEnum` | Snapshot 晋级规则 | `NONE` / `OVERALL_TOP_N` / `GROUP_TOP_N` / `MANUAL` / `SERIES_WINNER`（**配置层被拒绝**） |
| **晋级记录** | `QualificationTypeEnum` | `basket_division_stage_qualification` | `MATCH_WINNER` / `CHAMPION` / `GROUP_TOP_N` / `OVERALL_TOP_N` / `MANUAL` |
| **阶段队伍来源** | `StageTeamEntryTypeEnum` | `basket_division_stage_team.entry_type` | `MATCH_WINNER` / `GROUP_RANK` / `OVERALL_RANK` / `INITIAL` / `MANUAL` |

当前问题：

- 非末淘汰 Stage 在 2 阶段方案中作为 **末阶段** 使用 `advancement_type=NONE`（合法）
- 多阶段淘汰链（8强→4强→决赛）中，中间淘汰 Stage **不能** 用 `NONE`，但也没有专用的配置枚举
- 执行层 `buildEliminationPreview()` 用 `QualificationTypeEnum.MATCH_WINNER`，与 `advancement_type` **脱节**
- `SERIES_WINNER` 在 `AdvancementTypeEnum` 存在但在 `FormatSnapshotReadySpecification` 和 `BasketV4FormatSnapshotServiceImpl` 中被拒绝

#### R1 统一方案（推荐，本轮冻结）

**在 `AdvancementTypeEnum` 新增配置层语义 `WINNER_ADVANCE`（胜者晋级）。**

| Stage 类型 | 是否末阶段 | advancement_type |
|------------|-----------|------------------|
| `ROUND_ROBIN` + GROUPED | 非末 | `GROUP_TOP_N` + `advancement_count` |
| `ROUND_ROBIN` + UNGROUPED | 非末 | `OVERALL_TOP_N` + `advancement_count` |
| `SINGLE_ELIMINATION` | 非末 | **`WINNER_ADVANCE`**（`advancement_count` 可不填，执行时 = 参赛数/2） |
| 任意 | 末 | `NONE` |
| 任意 | 特殊 | `MANUAL`（保留） |

**执行层映射（R1 BO1）**：

```
advancement_type = WINNER_ADVANCE
  → QualificationTypeEnum.MATCH_WINNER
  → StageTeamEntryTypeEnum.MATCH_WINNER
```

**R2 BO3/BO5 映射（不在 R1 实现）**：

```
advancement_type = WINNER_ADVANCE（配置不变）
  → QualificationTypeEnum 新增 SERIES_WINNER 或使用 MATCH_WINNER 在 Series 粒度
  → 由 MatchSeries 汇总后写入
```

**废弃/冻结**：

- `AdvancementTypeEnum.SERIES_WINNER`：**不再作为 Stage 配置枚举**；保留枚举值仅供历史数据只读，新数据禁止写入
- 配置层不再区分 MATCH_WINNER vs SERIES_WINNER；**产品统一文案「胜者晋级」**，BO 粒度由 `best_of` 决定

**UI 行为**：

- 竞赛方式 = 单败淘汰 且 非最终阶段 → 晋级规则只读显示「胜者晋级」（底层写 `WINNER_ADVANCE`）
- 最终阶段 → 无晋级规则

---

### 13. `NONE` 是否只允许最终阶段？

**是。R1 强制。**

| 场景 | advancement_type |
|------|------------------|
| 最终阶段 | `NONE`（必须） |
| 非最终 + 循环赛 | `GROUP_TOP_N` 或 `OVERALL_TOP_N` |
| 非最终 + 淘汰赛 | `WINNER_ADVANCE`（**禁止 `NONE`**） |
| 非最终 + 任何 | **禁止 `NONE` 后靠执行代码猜测** |

当前 2 阶段方案的末位淘汰 Stage 已用 `NONE`，符合规则。R1 放开 N 阶段后，中间淘汰 Stage 必须从 `NONE` 迁移为 `WINNER_ADVANCE`。

锁定校验（`FormatSnapshotReadySpecification`）需同步强化此规则。

---

### 14. R1 BO3/BO5 配置如何处理

| 动作 | R1 是否允许 |
|------|------------|
| 创建含 BO3/BO5 的 Stage | ✅ |
| 编辑 | ✅ |
| 保存到 DRAFT Snapshot | ✅ |
| 预览（文案 + 场次估算） | ✅（标注「系列赛执行 R2 开放」） |
| LOCK Snapshot | ✅（`best_of ∈ {1,3,5}` 校验通过即可） |

**LOCK 不再拒绝 `best_of > 1`**（与 Alignment v1 不同，本轮修正）。

---

### 15. BO3/BO5 执行门禁放在哪里

**主门禁：`DivisionStageExecutionApplicationServiceImpl.generateSchedule()` 调用链入口。**

建议在以下位置增加检查（R1 实现时）：

```
generateSchedule(stageId)
  → validateStageForGenerate(stage)          // 已有
  → 【新增】BestOfExecutionGate.assertCanGenerateSchedule(stage)  // best_of>1 且 MatchSeries 未启用 → 拒绝
  → schedulePrerequisiteValidator.validateBeforeGenerateSchedule(...)
  → strategy.validate(context)               // AbstractStageScheduleStrategy.validateBestOfOne 改为调用 Gate
  → strategy.generate(context)
```

**错误文案（冻结）**：

> 当前阶段配置为 BO3/BO5，系列赛执行能力尚未启用，暂不能生成该阶段赛程。

要求：

- HTTP 400 业务异常（`ServiceException` + `BAD_REQUEST`）
- **禁止** HTTP 500、数据库异常、静默生成单场 Match

**次要门禁（可选）**：`publishSchedule()` 前二次校验（防绕过）。

**不在 R1 拦截**：报名开放、Snapshot LOCK、阶段队伍初始化（BO1 阶段可正常执行）。

---

### 16. 是否允许 BO3/BO5 赛制先 open-registration？

**本报告不自行决定，提供决策材料：**

| 选项 | 理由 |
|------|------|
| **允许开放报名（倾向推荐）** | 赛制已在 LOCK Snapshot 中完整确定；报名与赛制配置解耦；BO 只影响 **赛程生成** 时刻；若 R2 在赛事进入 BO3 Stage 前上线，业务可闭环 |
| **禁止开放报名（保守）** | 若运营可能在 R2 前推进到 BO3 Stage 的赛程生成，将触发门禁阻断；若缺乏赛程排期管控，可能造成「报名已开但打不了」的运营困惑 |

**风险缓解（若选择允许）**：

1. Admin 赛程页对 `best_of > 1` 的 Stage 显示醒目提示
2. 发布向导标注「含 BO3/BO5 阶段，需 R2 上线后方可生成该阶段赛程」
3. 项目排期保证：**任何 Division 进入首个 BO3 Stage 执行前，R2 必须已部署**

请产品/研发负责人择一冻结。

---

### 17. 模板 R1 调整范围

| 做 | 不做 |
|----|------|
| 种子/现有模板补 `organization_mode` 回填 | 新增「16 队小组→8 强→4 强→决赛」等预制模板 |
| 模板 → Snapshot 复制时带上 `organization_mode` | 扩展模板管理能力 |
| 保证旧模板继续可用（fallback 推导） | 修改模板业务语义 |
| `stage_type` 自动填充但不作为策略权威 | 快速创建方案数量/逻辑变更 |
| 快速创建 **回归测试** | |

**R1 真正目标：自定义赛制 N 阶段线性链。**

---

### 18. 赛制与报名人数解耦最终规则

| 概念 | 字段 | 性质 |
|------|------|------|
| 报名审核上限 | `division.max_team_count` | 计划上限，非实际参赛数 |
| 实际审核通过数 | EventTeam 计数 | 执行事实，可能 < 上限 |
| Stage 计划参赛 | UI 实时计算 | 不持久化 |
| Stage 实际参赛 | `participant_count` | 执行态 |

**冻结规则**：

1. 报名上限 16、实际 14 → Snapshot Stage 链 **不变**
2. 配置阶段允许基于上限做 **预估/预览/提示**
3. **禁止** 因人数非 2 的幂而 **阻止 LOCK 整个赛制**
4. 人数差异、轮空、弃赛等 → **R3 执行层**处理

**R1 需修正**：`v4AdminDisplay.validateV4QuickCreateParams` / `validateV4StageCombination` 中「非 2 的幂则拒绝锁定」的硬阻断 → 降级为 **警告/提示**，不阻断 LOCK。

---

### 19. R3 为什么取消「赛程发布后增队」

**产品规则冻结**：

- 正式赛程发布前：所有正式参赛队伍必须确认完毕
- 赛程发布后：**不允许** 追加新 EventTeam 到当前 Stage 并重新生成赛程

**代码已有部分支撑**：

- `StageSchedulePrerequisiteValidator.MSG_PUBLISHED_NO_MORE_TEAMS`
- `DivisionStageExecutionApplicationServiceImpl.assertCanSyncNewStageTeams()` 在已发布/已生成赛程时拒绝同步

**R3 只处理现场异常**：弃赛、判负、空位、轮空、直接晋级、人工对阵调整——**不包含**「发布后新增球队并重生成」。

---

### 20. R1 最终数据库变更

> **方案冻结，R1 编码轮执行，本文档轮不执行。**

| 操作 | 对象 | 说明 |
|------|------|------|
| **ADD** | `basket_division_stage.organization_mode` | VARCHAR(32) NOT NULL DEFAULT 'UNGROUPED'（回填后） |
| **ADD** | `basket_format_template_stage.organization_mode` | 同上 |
| **BACKFILL** | 两表历史数据 | `grouping_mode='NONE'` → `UNGROUPED`；否则 → `GROUPED` |
| **不新增** | `is_final_stage` / `participant_source` / `planned_participant_count` | 见 §2～4 |
| **不删除** | `stage_type` | 降级为展示 |
| **枚举扩展** | `AdvancementTypeEnum` 增加 `WINNER_ADVANCE` | 仅 Java 枚举 + 校验；DB 存 VARCHAR 无需改表 |

建议迁移文件命名：`docs/sql/cnpa-dynamic-format-r6-organization-mode-migration.sql`（含 data-check）

---

### 21. R1 最终后端变更

| # | 模块 | 变更 |
|---|------|------|
| 1 | `OrganizationModeEnum` | 新增 `GROUPED` / `UNGROUPED` |
| 2 | `AdvancementTypeEnum` | 新增 `WINNER_ADVANCE`；`SERIES_WINNER` 标记 @Deprecated 禁止新写入 |
| 3 | Entity / DTO / VO | `BasketDivisionStage*`、`BasketFormatTemplateStage*` 增加 `organizationMode` |
| 4 | `LinearStageChainValidator` | 替换 2 阶段限制为 N 阶段线性链校验（重构 `StageCombinationValidator`） |
| 5 | `FormatSnapshotReadySpecification` | ① `organization_mode` 与 `grouping_mode` 联动校验 ② `best_of` 允许 1/3/5 ③ `NONE` 仅末阶段 ④ 非末淘汰必须 `WINNER_ADVANCE` ⑤ 移除 `validateBestOf` 仅允许 1 |
| 6 | `isGroupStage()` 全部实现 | 改读 `organization_mode`；旧 Snapshot fallback `grouping_mode` |
| 7 | `BestOfExecutionGate` | 新增；`best_of>1` 拦截 `generateSchedule` |
| 8 | `AbstractStageScheduleStrategy` | `validateBestOfOne` 委托 Gate；不删方法签名 |
| 9 | `BasketV4FormatSnapshotServiceImpl` | 保存 Stage 时写 `organization_mode`；自动填充 `stage_type`（展示） |
| 10 | `DivisionFormatSnapshotFactory` | 模板复制时复制 `organization_mode` |
| 11 | 模板种子 SQL | 补 `organization_mode`（随 r6 migration） |
| 12 | 旧 Snapshot 兼容 | 读取时 fallback；保存时强制写入 |

**不在 R1 修改**：Submission 主链、Standing 主算法、MatchSeries、赛程生成核心算法（BO1 路径保持不变）。

---

### 22. R1 最终 Admin UI 变更

**主文件**：`DivisionFormatRulesPanel.vue` + `v4AdminDisplay.ts` + `format-template.ts`

#### 自定义赛制交互（冻结）

```
默认展示 Stage 1

每 Stage 编辑卡片：
  1. 阶段名称
  2. 参赛来源（只读：Stage1=全部队伍 / Stage2+=上阶段晋级）
  3. 参赛组织方式：分组 / 不分组
     └─ 分组时：groupCount + groupingMethod
  4. 竞赛方式：循环积分赛 / 单败淘汰
     └─ 循环赛：cycleCount, winPoints, lossPoints, tieBreakRules
  5. BO：BO1 / BO3 / BO5
  6. 晋级规则（动态）
     └─ 循环+分组：每组前 N
     └─ 循环+不分组：总排名前 N
     └─ 淘汰+非末：只读「胜者晋级」
     └─ 末阶段：无
  7. 「本阶段为最终阶段」开关（操作语义，不落库）

卡片操作：添加下一阶段 / 删除 / 上移 / 下移
底部：完整 Stage 链实时预览
```

#### 其他 UI 修正

| 项 | 说明 |
|----|------|
| 移除 `bestOf=1` 写死 | 允许选择 1/3/5 |
| `isV4GroupCompetitionStage` | 改读 `organizationMode` |
| LOCK 前校验 | 调用更新后的 `validateV4StageCombination`（线性链 + 警告非阻断） |
| 2 的幂校验 | 降级为警告，不阻断 LOCK |
| 快速创建 Tab | 仅兼容回归，不扩能力 |
| BO3/5 提示 | 预览/保存时标注「赛程生成需 R2」 |

---

### 23. R1 Web 影响

| 模块 | 变更 | 工作量 |
|------|------|--------|
| `BasketV4PublicServiceImpl` | 赛制摘要展示：优先读 `organizationMode` + `competitionFormat` + `bestOf` 组合文案 | 小 |
| 公开 Stage 列表 API | 响应增加 `organizationMode`（可选） | 小 |
| `BasketV4PlayerMyScheduleServiceImpl` | 展示逻辑可逐步改用 `organizationMode`；R1 可最小改动 | 极小 |
| my-events / my-schedule / submission | **无变更** | — |

---

### 24. R1 不做事项（冻结清单）

1. MatchSeries 表与实体  
2. BO3 / BO5 实际比赛执行  
3. 多局 Match 生成  
4. Series winner 执行逻辑  
5. 轮空执行能力升级  
6. 弃赛执行升级  
7. 直接晋级执行升级  
8. 赛程发布后增队  
9. DAG / 分叉 / 多来源  
10. 双败淘汰 / 败者组  
11. 新预制模板  
12. 大范围数据库重构  
13. 删除 `stage_type` 列  
14. 修改 Submission 主链  
15. 修改 Standing 主算法  
16. `participant_source` / `is_final_stage` / `planned_participant_count` 落库  
17. 新增 `ROUND_ROBIN_POINTS` DB 枚举  

---

### 25. R2 范围（冻结）

| 层 | 内容 |
|----|------|
| 数据库 | `basket_match_series` + `basket_match.series_id` |
| 后端 | BO3/BO5 赛程生成；Series 胜负汇总；`WINNER_ADVANCE` → Series 级晋级 |
| Admin UI | 系列赛比分展示；审核页适配 |
| Web | 公开赛程 Series 展示；按局提交策略 |
| 验收 | 含 BO3 淘汰的完整链路 |

---

### 26. R3 范围（冻结）

**实际赛事执行适配**（不含发布后增队）：

- 报名人数不足（实际上线 < 上限）
- 分组人数差异（组间不均）
- 空位 / 轮空
- 弃赛 / 判负
- 直接晋级
- 人工调整对阵

---

### 27. 现有能力兼容方案

| 已验收能力 | R1 兼容策略 |
|------------|------------|
| **4 队单循环** | `organization_mode=UNGROUPED` + `competition_format=ROUND_ROBIN` + `best_of=1` + 单 Stage 末位 `NONE`；回填兼容 |
| **5 队单循环 BYE** | 同上；`BasketV4RoundRobinScheduler` 不变 |
| **小组 BO1** | `organization_mode=GROUPED` + `grouping_mode=MANUAL` + `ROUND_ROBIN` + `GROUP_TOP_N` |
| **淘汰 BO1（2 阶段）** | Stage1 循环/小组 + Stage2 `SINGLE_ELIMINATION` + `NONE`；中间 Stage 若扩展为 N 链，末位前改用 `WINNER_ADVANCE` |
| **TEAM 报名** | 不涉及赛制层；回归不测赛制 |
| **PERSONAL 报名** | 同上 |
| **LOCKED 旧 Snapshot** | 读取 fallback：`grouping_mode` 推导 `organization_mode`；首次编辑后强制写入新字段 |

**回归门禁**：`CNPA_V2_RELEASE_GATE_RR4`（52 PASS）必须在 R1 PR 合并前通过。

---

### 28. R1 风险 P0/P1/P2

| # | 风险 | 级别 | 缓解 |
|---|------|------|------|
| 1 | `isGroupStage()` 漏改导致策略选错 | **P0** | 按 §7 清单逐项切换 + 单元测试 |
| 2 | 线性链校验遗漏（跳级/orphan） | **P0** | `LinearStageChainValidator` 单测矩阵（1/2/4/N 阶段） |
| 3 | 中间淘汰 Stage 仍用 `NONE` | **P0** | 引入 `WINNER_ADVANCE` + LOCK 校验 |
| 4 | BO3 配置可 LOCK 但运营误点生成赛程 | **P0** | `BestOfExecutionGate` + Admin 醒目提示 |
| 5 | 旧 Snapshot 无 `organization_mode` | **P0** | 回填 + 读取 fallback |
| 6 | 快速创建 2 阶段方案回归失败 | **P1** | 专用回归用例 |
| 7 | UI 与后端校验不一致 | **P1** | 共享校验规则文档化；前端对齐后端错误码 |
| 8 | `stage_type` 展示自动填充与历史数据不一致 | **P1** | 仅新保存时重写；旧数据只读不改 |
| 9 | BO3 赛制开放报名但 R2 延期 | **P1** | 见 §16 决策 + 赛程页提示 |
| 10 | 2 的幂警告改为不阻断后，运营忽视提示 | **P2** | 预览文案强化 |
| 11 | N 阶段 UI 操作复杂度 | **P2** | 可选 8 阶段软限制 |
| 12 | `SERIES_WINNER` 历史数据 | **P2** | 只读兼容，禁止新写入 |

---

### 29. R1 是否已经可以直接进入开发？

**是。**

进入编码前最后确认项：

- [ ] 产品确认 §16「BO3/5 赛制是否允许 open-registration」择一
- [ ] 确认 `WINNER_ADVANCE` 枚举名（或改用其他命名）
- [ ] 确认 UI 是否加 8 阶段软限制（可选）

除上述 3 项外，**无需再出对齐文档，可直接开工。**

---

### 30. 建议下一条正式开发提示词应包含的模块

下轮编码任务建议按以下模块拆分 PR / 任务（可直接复制为开发提示词骨架）：

```
【CNPA V2 自定义赛制 R1 — 编码任务】

约束：
- 严格遵循 docs/cnpa-custom-format-dynamic-stage-plan-frozen-20260808.md
- 不实现 MatchSeries / BO3/BO5 执行
- 不做 DAG
- 不修改 Submission / Standing 主算法

模块 1 — 数据库迁移（r6）
- cnpa-dynamic-format-r6-organization-mode-migration.sql
- cnpa-dynamic-format-r6-data-check.sql
- 回填 organization_mode

模块 2 — 后端枚举与实体
- OrganizationModeEnum
- AdvancementTypeEnum.WINNER_ADVANCE
- Entity/DTO/VO/Mapper 增 organizationMode

模块 3 — 线性链校验器
- 重构 StageCombinationValidator → LinearStageChainValidator
- FormatSnapshotReadySpecification 联动更新
- NONE 仅末阶段；非末淘汰必须 WINNER_ADVANCE

模块 4 — organization_mode 策略切换
- AbstractStageScheduleStrategy.isGroupStage
- GroupStageStageScheduleStrategy / RoundRobinStageScheduleStrategy
- TieBreakStandingService
- DivisionStageExecutionApplicationServiceImpl
- 旧 Snapshot fallback

模块 5 — BO 执行门禁
- BestOfExecutionGate
- generateSchedule 入口拦截
- 移除 LOCK 阶段 best_of=1 限制

模块 6 — 模板与 Snapshot 服务
- DivisionFormatSnapshotFactory 复制 organization_mode
- BasketV4FormatSnapshotServiceImpl 保存/自动填充 stage_type
- 模板种子更新

模块 7 — Admin UI
- DivisionFormatRulesPanel.vue 三维配置 + N 阶段 + 最终阶段开关
- v4AdminDisplay.ts 校验/预览/organizationMode
- format-template.ts 类型扩展
- 2 的幂降级为警告

模块 8 — Web 展示适配
- BasketV4PublicServiceImpl 赛制文案

模块 9 — 测试
- LinearStageChainValidator 单测
- OrganizationMode isGroupStage 单测
- BestOfExecutionGate 单测
- 回归：4队RR / 5队BYE / 小组BO1 / 淘汰BO1 发布门禁
```

---

## 三、R1/R2/R3 轮次总览（最终冻结）

```
R1（本轮编码）
├── organization_mode 落库
├── Stage 三维模型解耦
├── 1～N 线性阶段链
├── WINNER_ADVANCE 晋级语义
├── BO1/3/5 配置 + LOCK
├── BO3/5 执行门禁（仅拦截赛程生成）
├── Admin 自定义赛制 UI
├── 模板兼容（不扩模板）
└── Web 展示适配

R2
├── basket_match_series
├── BO3/BO5 多局执行
└── Series Winner 晋级

R3
├── 报名人数不足 / 分组差异
├── 空位 / 轮空 / 弃赛 / 判负 / 直接晋级 / 人工调对阵
└── ❌ 不含赛程发布后增队
```

---

## 四、ScheduleGenerationStrategy 定位（重申）

| 层 | 枚举/字段 | 定位 |
|----|-----------|------|
| 产品配置 | `organization_mode` + `competition_format` + `best_of` | 管理员配置 |
| 运行时调度 | `ScheduleGenerationStrategyEnum`：ROUND_ROBIN / GROUP_STAGE / SINGLE_ELIMINATION | 内部执行策略 |
| 映射关系 | `GROUPED + ROUND_ROBIN` → 运行时 `GROUP_STAGE` strategy | 允许 |
| 禁止反向 | 用 `GROUP_STAGE` 作为 DB 配置表达分组+循环 | **禁止** |

---

## 五、附录：关键代码索引

| 用途 | 路径 |
|------|------|
| 2 阶段限制（待替换） | `basketball-admin/.../stage/StageCombinationValidator.java:23-25` |
| LOCK 时 BO1 强制（待改） | `basketball-admin/.../FormatSnapshotReadySpecification.java:329-335` |
| 执行 BO1 强制（待改） | `basketball-admin/.../stage/AbstractStageScheduleStrategy.java:57-61` |
| 赛程生成入口（加门禁） | `basketball-admin/.../DivisionStageExecutionApplicationServiceImpl.java:170` |
| 发布后禁止增队（已有） | `DivisionStageExecutionApplicationServiceImpl.assertCanSyncNewStageTeams()` |
| 淘汰晋级预览 | `DivisionStageExecutionApplicationServiceImpl.buildEliminationPreview()` |
| 赛制 UI | `basketball-admin-ui/.../DivisionFormatRulesPanel.vue` |
| UI 校验 | `basketball-admin-ui/src/utils/v4AdminDisplay.ts` |

---

*本文档为 R1 开发计划最终冻结版。Alignment v1 中与本文件冲突的条目，以本文档为准。*
