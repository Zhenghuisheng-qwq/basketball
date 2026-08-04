# CNPA 动态赛制重构 R1 — 执行记录

## 背景

第 1 轮目标：在 **Series → Season → Division** 主线下，建立平台合并（PC / PS·Xbox）、比赛模式（5V5 / 3V3）、阵容人数规则、动态赛制模板与 Division 规则快照机制，并接入 Admin 发布页与 Web 报名展示。

本轮 **不** 实现完整规则引擎、季后赛执行、个人报名流程。

## 实际修改摘要

### 数据库

- 扩展 `basket_division`：`game_mode`、`format_template_id`、`format_snapshot_id`
- 新增模板表 3 张、快照表 3 张（见 `cnpa-dynamic-format-r1-table-list.md`）
- SQL 文件：`docs/sql/cnpa-dynamic-format-r1-*.sql`

### 后端（basketball-admin）

- 枚举：`PlatformCodeEnum`（PC/PS_XBOX + 历史 PS/XBOX）、`GameModeEnum`、`SnapshotStatusEnum` 及赛制相关枚举
- 值对象：`RosterLimit`
- 工厂：`DivisionFormatSnapshotFactory`
- 规格：`FormatSnapshotReadySpecification`、`DivisionRuleMutableSpecification`
- 服务：`BasketV4FormatTemplateServiceImpl`、`BasketV4FormatSnapshotServiceImpl`
- 主流程：`BasketV4DivisionServiceImpl` 创建/编辑/开放报名接入模板与快照锁定
- `BasketV4SeasonServiceImpl`：不再静默覆盖 Season 默认值；`format_type` 标记为过渡字段
- `BasketV4ParticipationSupport`：阵容校验改读 Division 规则
- 控制器：`BasketV4FormatTemplateController`
- 可选迁移：`BasketV4FormatSnapshotMigrationRunner`（默认关闭）

### 后端（basketball-web）

- 镜像枚举与 `GameTerminalSupport.matchesDivisionPlatform`
- `BasketV4PlayerRegistrationServiceImpl`：动态 min/max；PERSONAL 拦截；PS_XBOX 平台身份匹配

### 前端

- Admin：`publish/index.vue` 平台/模式/模板/规则页签
- Web：`v4Display.ts`、`V4RegistrationFlow.vue` 动态人数与平台展示

## 设计决策

1. **平台**：新写入仅 `PC` / `PS_XBOX`；历史 `PS`/`XBOX` 只读展示「（历史）」；不自动合并同 Season 双平台 Division。
2. **人数**：由 `GameModeEnum` → `RosterLimit` 统一解析，禁止三端各写 magic number。
3. **规则**：模板可版本化；Division 快照为执行真相；开放报名时 `lockSnapshot`。
4. **Season.format_type**：保留作默认值，不作为最终执行规则。
5. **PERSONAL**：可建草稿 Division，开放报名明确拒绝并提示后续模块。

## 兼容方案

- `game_mode` 历史回填 `FIVE_V_FIVE`（注释标明非业务确认）
- 种子模板 `V4_LEGACY_ROUND_ROBIN_V1` 对齐现有单循环积分（胜2负1，弃权胜2负0）
- 数据检查 SQL：`cnpa-dynamic-format-r1-data-check.sql`
- 可选 Java 迁移：`cnpa.v4.format-snapshot-migration.enabled=true`

## 未实现范围

个人报名、抽签、分组/赛程生成、BO 多场次、积分动态结算、晋级流转、V2 表删除/迁移、Flowable、规则 DSL。
