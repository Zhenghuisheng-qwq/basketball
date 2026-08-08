# CNPA V2 自定义赛制 R1-1 完成报告

> 轮次：数据库与后端基础模型解耦  
> 日期：2026-08-08  
> 状态：**R1-1 已完成**，未进入 R1-2

---

## 1. migration 路径

- `docs/sql/cnpa-dynamic-format-r6-organization-mode-migration.sql`
- 验收脚本：`docs/sql/cnpa-dynamic-format-r6-data-check.sql`

**执行结果**：已在 `basketball` 库执行成功（ADD COLUMN → 回填 → NOT NULL）。

---

## 2. 新增数据库字段

| 表 | 字段 | 类型 | 约束 |
|---|---|---|---|
| `basket_division_stage` | `organization_mode` | `VARCHAR(32)` | NOT NULL |
| `basket_format_template_stage` | `organization_mode` | `VARCHAR(32)` | NOT NULL |

位置：均在 `stage_type` 之后。

---

## 3. 历史数据回填数量

| 表 | 回填行数 | UNGROUPED | GROUPED |
|---|---:|---:|---:|
| `basket_division_stage` | **14** | 10 | 4 |
| `basket_format_template_stage` | **7** | 6 | 1 |

回填规则：`grouping_mode` 为 NULL / 空串 / `NONE` → `UNGROUPED`；否则 → `GROUPED`。

---

## 4. organizationMode NULL 数量

| 检查项 | 数量 |
|---|---:|
| `basket_division_stage.organization_mode IS NULL` | **0** |
| `basket_format_template_stage.organization_mode IS NULL` | **0** |

---

## 5. OrganizationModeEnum 结果

**路径**：`basketball-admin/src/main/java/com/zhs/basket/enums/v4/OrganizationModeEnum.java`

| code | description |
|---|---|
| `GROUPED` | 分组 |
| `UNGROUPED` | 不分组 |

提供 `getCode()` / `getDescription()` / `fromCode()`，与项目现有枚举风格一致。

---

## 6. AdvancementTypeEnum.WINNER_ADVANCE 结果

**路径**：`basketball-admin/src/main/java/com/zhs/basket/enums/v4/AdvancementTypeEnum.java`

| 枚举 | 状态 | 说明 |
|---|---|---|
| `WINNER_ADVANCE` | **新增** | code=`WINNER_ADVANCE`，description=胜者晋级 |
| `SERIES_WINNER` | **@Deprecated** | 保留只读兼容，新业务写入在 `validateStageReq` 中拒绝 |

**识别能力**：
- `AdvancementTypeEnum.fromCode("WINNER_ADVANCE")` 正常
- `BasketV4FormatSnapshotServiceImpl.validateStageReq` 已识别 `WINNER_ADVANCE`（不抛错，执行语义留 R1-2）
- 未修改 Qualification / StageTeam / 比赛结束晋级逻辑

**单测**：`AdvancementTypeEnumR1Test` — 通过

---

## 7. Entity 修改清单

| 文件 | 变更 |
|---|---|
| `BasketDivisionStage.java` | 新增 `organizationMode` |
| `BasketFormatTemplateStage.java` | 新增 `organizationMode` |

---

## 8. DTO/VO 修改清单

| 文件 | 变更 |
|---|---|
| `BasketDivisionStageSaveReq.java` | 新增 `organizationMode` |
| `BasketDivisionStageVo.java` | 新增 `organizationMode` / `organizationModeName` |
| `BasketFormatTemplateStageVo.java` | 新增 `organizationMode` / `organizationModeName` |
| `V4PublicStageItemResp.java` (web) | 新增 `organizationMode` / `organizationModeName` |

**Service 层**：
- `BasketV4FormatSnapshotServiceImpl.applyStageReq` — 写入 `organizationMode`
- `BasketV4FormatSnapshotServiceImpl.buildFormatPlanStages` — 快速方案写入 `organizationMode`
- `V4FormatDisplaySupport` — `organizationModeLabel()` + VO enrich
- `BasketV4PublicServiceImpl` — Public API 回显 `organizationModeName`

**本轮未改**：Admin UI、`DivisionFormatRulesPanel.vue`、前端 TS 类型。

---

## 9. Mapper/XML 修改清单

| 文件 | 变更 |
|---|---|
| `BasketDivisionStageMapper.xml` | resultMap / insert / update 含 `organization_mode` |
| `BasketFormatTemplateStageMapper.xml` | resultMap 含 `organization_mode` |
| `BasketV4PublicStageMapper.xml` (web) | SELECT 增 `organization_mode` |

`select *` 类查询随表结构自动包含新列，无需额外改动。

---

## 10. Template → Snapshot 复制结果

**路径**：`DivisionFormatSnapshotFactory.buildStages()`

```java
stage.setOrganizationMode(ts.getOrganizationMode());
```

- 模板复制路径：`BasketV4FormatSnapshotServiceImpl.createSnapshotFromTemplate()` → `snapshotFactory.buildStages()` — **字段已复制**
- 单测：`DivisionFormatSnapshotFactoryTest.buildStages_copiesOrganizationModeFromTemplate` — **通过**

**历史一致性检查（data-check #12）**：发现 **2 条** Snapshot Stage 与当前 Template 的 `organization_mode` 不一致（见 §14），原因为 Snapshot 创建后 `grouping_mode` 被人工修改为 `MANUAL` / `OFFLINE_RANDOM_RESULT`，回填按当前 `grouping_mode` 推导为 `GROUPED`，而 Template 仍为 `NONE`/`UNGROUPED`。属历史漂移，本轮不自动修正。

---

## 11. 自定义 Stage stage_type=CUSTOM 结果

**路径**：`BasketV4FormatSnapshotServiceImpl.addStage()`

```java
applyStageReq(stage, req);
stage.setStageType(StageTypeEnum.CUSTOM.getCode());  // 强制 CUSTOM，覆盖 req
```

- 自定义新增 Stage：**统一 `stage_type = CUSTOM`**
- 不再根据 `organization_mode` / `competition_format` 自动推导 `GROUP` / `KNOCKOUT` / `REGULAR`
- 快速模板复制 / `applyFormatPlan`：**保留原 stage_type**（`GROUP` / `REGULAR` / `KNOCKOUT` 等）

---

## 12. 旧 Snapshot fallback 结果

**路径**：`OrganizationModeSupport.java`

| 方法 | 用途 |
|---|---|
| `resolveOrganizationMode(BasketDivisionStage)` | 读取兼容：优先 `organizationMode`，否则由 `groupingMode` 推导 |
| `resolveOrganizationMode(BasketFormatTemplateStage)` | 同上 |
| `resolveFromGroupingMode(String)` | `NONE`/null/空 → `UNGROUPED`，否则 `GROUPED` |
| `resolveForPersist(String, String)` | 保存路径：优先显式 `organizationMode`，否则推导并落库 |

**单测**：`OrganizationModeSupportTest`（6 用例）— **全部通过**

---

## 13. data-check 结果

| # | 检查项 | 结果 |
|---|---|---|
| 1 | division_stage `organization_mode` NULL | **0** |
| 2 | template_stage `organization_mode` NULL | **0** |
| 3 | UNGROUPED 但 grouping_mode ≠ NONE | **0** |
| 4 | GROUPED 但 grouping_mode = NONE | **0** |
| 5 | GROUPED 但 group_count ≤ 0 | **0** |
| 6 | 非法 organization_mode 枚举 | **0** |
| 7 | stage_type 分布 | REGULAR=9, KNOCKOUT=3, FINAL=1, GROUP=1 |
| 8 | competition_format 分布 | ROUND_ROBIN=10, SINGLE_ELIMINATION=4 |
| 9 | best_of 分布 | 1=12, 3=1, 5=1 |
| 10 | SERIES_WINNER 历史数量 | **1** |
| 11 | Snapshot Stage 孤儿 | **0** |
| 12 | Template→Snapshot organization_mode 不一致 | **2** |

---

## 14. 历史异常数据清单

### 14.1 SERIES_WINNER（1 条）

| id | division_id | stage_code | advancement_type |
|---:|---:|---|---|
| 2 | 8 | PLAYOFF | SERIES_WINNER |

处理：只读兼容，禁止新写入；R1-2 可考虑迁移为 `WINNER_ADVANCE`。

### 14.2 Template↔Snapshot organization_mode 不一致（2 条）

| ds.id | stage_code | snapshot_org | template_org | grouping_mode | 原因 |
|---:|---|---|---|---|
| 12 | REGULAR | GROUPED | UNGROUPED | MANUAL | Snapshot 创建后分组方式被修改 |
| 14 | REGULAR | GROUPED | UNGROUPED | OFFLINE_RANDOM_RESULT | 同上 |

处理：本轮不自动修正；R1-2 可评估是否同步 Template 或标记为人工漂移。

### 14.3 其他异常（#3～#5）

**无**。

---

## 15. stage_type 策略依赖扫描清单

> 本轮仅记录，未批量重构。

### GROUP

| 文件 | 用途 |
|---|---|
| `AbstractStageScheduleStrategy.java:44` | 赛程策略分支 |
| `GroupStageStageScheduleStrategy.java:39` | 小组赛策略 supports |
| `StageCombinationValidator.java:76` | 组合校验 |
| `TieBreakStandingService.java:254` | 同分规则 |
| `DivisionStageExecutionApplicationServiceImpl.java:797,897` | 执行服务 `isGroupStage()` |
| `BasketV4FormatSnapshotServiceImpl.java:242` | 快速方案 GROUP_TO_ELIM 写入 |

### KNOCKOUT

| 文件 | 用途 |
|---|---|
| `BasketV4FormatSnapshotServiceImpl.java:224` | 快速方案淘汰赛阶段写入 |
| `RoundRobinStageScheduleStrategyTest.java` | 测试数据 |

### REGULAR

| 文件 | 用途 |
|---|---|
| `BasketV4FormatSnapshotServiceImpl.java:254` | 快速方案常规赛写入 |
| `RoundRobinStageScheduleStrategyTest.java` | 测试数据 |

### FINAL

| 文件 | 用途 |
|---|---|
| `BasketV4FormatSnapshotServiceImpl.java:544` | 校验 FINAL 不可配置 nextStageCode |

### 展示/解析（非策略）

| 文件 | 用途 |
|---|---|
| `V4FormatDisplaySupport.java` | stageTypeLabel 展示 |
| `DivisionStageExecutionApplicationServiceImpl.java:865-866` | VO 展示 |
| `BasketV4FormatSnapshotServiceImpl.java:511` | validateStageReq 枚举校验 |

**R1-2 切换目标**：上述策略判断逐步改为 `organization_mode` + `competition_format` + `best_of` 三维模型。

---

## 16. Admin 后端启动结果

- 命令：`mvn spring-boot:run`（`basketball-admin`）
- 结果：**成功** — `Started RuoYiApplication`

---

## 17. Web 后端启动结果

- 命令：`mvn spring-boot:run`（`basketball-web`）
- 结果：**成功** — `Started MiniApplication`

---

## 18. 当前 P0

| # | 风险 | R1-1 状态 |
|---|---|---|
| 1 | `isGroupStage()` 漏改导致策略选错 | **未解决** — 仍依赖 `stage_type=GROUP`，留 R1-2 |
| 2 | 线性链校验遗漏 | **未解决** — `LinearStageChainValidator` 未实现，留 R1-2 |
| 3 | 中间淘汰 Stage 仍用 `NONE` | **部分** — `WINNER_ADVANCE` 枚举已加，LOCK/执行留 R1-2 |
| 4 | BO3 配置可 LOCK 但误点生成 | **未解决** — `BestOfExecutionGate` 未实现，留 R1-2/R1-3 |
| 5 | 旧 Snapshot 无 `organization_mode` | **已解决** — migration 回填 + `OrganizationModeSupport` fallback |

---

## 19. 当前 P1

| # | 风险 | R1-1 状态 |
|---|---|---|
| 6 | 快速创建 2 阶段方案回归 | 未专项回归；`buildFormatPlanStages` 已写入 `organizationMode` |
| 7 | UI 与后端校验不一致 | **未改 UI** — 留 R1-2 前后端对齐 |
| 8 | stage_type 展示与历史不一致 | **已处理新写入** — `addStage` 强制 CUSTOM；旧数据未批量改写 |
| 9 | BO3 开放报名但 R2 延期 | 未涉及 |

---

## 20. 当前 P2

| # | 风险 | R1-1 状态 |
|---|---|---|
| 10 | 2 的幂警告改为不阻断 | 未涉及 |
| 11 | N 阶段 UI 操作复杂度 | 未涉及 |
| 12 | `SERIES_WINNER` 历史数据 | **已标记 Deprecated**；库内 1 条历史只读保留 |

---

## 21. 是否可以进入 R1-2

**可以进入 R1-2**，前提条件已满足：

- [x] `organization_mode` DB 字段 + 回填 + NOT NULL
- [x] `OrganizationModeEnum` + Entity/DTO/VO/Mapper 全链路
- [x] Template → Snapshot 复制
- [x] `OrganizationModeSupport` 旧数据 fallback
- [x] `WINNER_ADVANCE` 枚举 + DTO 识别
- [x] 自定义 `addStage` → `stage_type=CUSTOM`
- [x] 基础单测通过（3 个测试类，9 用例）
- [x] Admin/Web 编译 + 启动成功

**R1-2 范围**（本轮未做）：

1. `LinearStageChainValidator` — 线性 Stage 链校验
2. `organization_mode` ↔ `grouping_mode` 组合业务校验
3. 策略切换：`isGroupStage()` 等从 `stage_type` 迁移到三维模型
4. `WINNER_ADVANCE` LOCK 校验 + Qualification 执行
5. Admin UI 对齐

---

## 附录：新增/修改文件一览

### SQL
- `docs/sql/cnpa-dynamic-format-r6-organization-mode-migration.sql`
- `docs/sql/cnpa-dynamic-format-r6-data-check.sql`

### 新增 Java
- `OrganizationModeEnum.java`
- `OrganizationModeSupport.java`
- `OrganizationModeSupportTest.java`
- `DivisionFormatSnapshotFactoryTest.java`
- `AdvancementTypeEnumR1Test.java`

### 修改 Java（核心）
- `AdvancementTypeEnum.java`
- `BasketDivisionStage.java` / `BasketFormatTemplateStage.java`
- `BasketDivisionStageSaveReq.java`
- `BasketDivisionStageVo.java` / `BasketFormatTemplateStageVo.java`
- `DivisionFormatSnapshotFactory.java`
- `BasketV4FormatSnapshotServiceImpl.java`
- `V4FormatDisplaySupport.java`
- `V4PublicStageItemResp.java` / `BasketV4PublicServiceImpl.java`

### 修改 XML
- `BasketDivisionStageMapper.xml`
- `BasketFormatTemplateStageMapper.xml`
- `BasketV4PublicStageMapper.xml`

### 测试执行

```
mvn test -Dtest=OrganizationModeSupportTest,DivisionFormatSnapshotFactoryTest,AdvancementTypeEnumR1Test
→ BUILD SUCCESS（9 tests passed）
```
