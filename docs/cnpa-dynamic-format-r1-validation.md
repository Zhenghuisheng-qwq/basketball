# CNPA 动态赛制 R1 — 验证记录

> 说明：本环境未配置 `mvn`，Java 编译需在本地 CI/开发机复验。以下基于代码路径与静态逻辑核对。

## 平台验证

| 场景 | 预期 | 实现位置 | 结果 |
|------|------|----------|------|
| 创建 PC Division | 成功 | `BasketV4DivisionServiceImpl.validateWritablePlatformCode` | 代码已实现 |
| 创建 PS_XBOX Division | 成功 | 同上 | 代码已实现 |
| 创建 PS / XBOX | 拒绝 | `PlatformCodeEnum.isWritableCode` | 代码已实现 |
| 历史 PS/XBOX 查询 | 可读 | Mapper 未过滤 legacy | 代码已实现 |
| 展示 PS/Xbox | 不展示 PS_XBOX 原文 | `V4FormatDisplaySupport` / `v4Display.ts` | 代码已实现 |

## 比赛模式与人数

| 场景 | 预期 | 实现位置 | 结果 |
|------|------|----------|------|
| FIVE_V_FIVE → 5~8 | 自动填充 | `GameModeEnum` + `RosterLimit` | 代码已实现 |
| THREE_V_THREE → 3~8 | 自动填充 | 同上 | 代码已实现 |
| Web 报名 min/max | 读 Division | `BasketV4PlayerRegistrationServiceImpl.buildLineup` | 代码已实现 |
| 非法人数服务端拒绝 | 抛业务异常 | admin/web Support | 代码已实现 |

## 模板与快照

| 场景 | 预期 | 实现位置 | 结果 |
|------|------|----------|------|
| 创建 Division + 模板 | DRAFT 快照 | `create` → `formatSnapshotService.createDraftSnapshot` | 代码已实现 |
| 阶段/同分复制 | 完整复制 | `DivisionFormatSnapshotFactory` | 代码已实现 |
| DRAFT 重建 | 条件允许时重建 | `rebuildDraftSnapshot` + `DivisionRuleMutableSpecification` | 代码已实现 |
| 有报名后重建 | 拒绝 | `assertNoBusinessData` | 代码已实现 |
| 开放报名锁定 | validate + lock | `openRegistration` | 代码已实现 |
| PERSONAL 开放报名 | 拒绝 | `openRegistration` 业务提示 | 代码已实现 |
| League 模板晋级 N 为空 | 锁定前校验失败 | `FormatSnapshotReadySpecification` | 代码已实现 |

## 历史兼容

| 场景 | 预期 | 结果 |
|------|------|------|
| game_mode 默认 FIVE_V_FIVE | DDL DEFAULT | SQL 已写 |
| 缺少快照检查 | data-check SQL | SQL 已写 |
| 可选快照迁移 | MigrationRunner | 已实现，默认关闭 |

## 待环境复验项

- [ ] 执行 migration + seed 后 Admin 创建 PS_XBOX + 3V3 Division
- [ ] 开放 TEAM Division 后快照 status=LOCKED
- [ ] 现有单循环 Division 报名/审核/赛程不受影响
- [ ] `cnpa-dynamic-format-r1-data-check.sql` 在目标库执行结果归档
