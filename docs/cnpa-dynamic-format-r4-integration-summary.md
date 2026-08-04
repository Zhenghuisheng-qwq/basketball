# CNPA 动态赛制重构 R4 联调验收总结

**测试批次编码**：`R4_TEST_BATCH_20260805`  
**文档生成时间**：2026-08-05  
**本轮结论**：**不通过**

---

## 1. 本轮目标

R4 为五轮计划中唯一完整联调验收轮，目标包括：

- 真实执行 R1～R3 + R4 数据库脚本
- 修复 `rank_explain_json`、Standing 唯一键、MANUAL 同分、Legacy 入口收口
- Admin/Web 页面收口
- 跑通 TEAM/PERSONAL 链路与 ROUND_ROBIN / GROUP_STAGE / SINGLE_ELIMINATION BO1
- 异常、幂等、权限、数据一致性验证
- 修复全部 P0/P1
- 形成 R5 清理清单

---

## 2. 环境信息

| 项目 | 值 |
|------|-----|
| MySQL | `192.168.31.47:3306`，库名 `basketball`，账号 `root`（密码已脱敏） |
| Redis | 未验证（Admin/Web 服务未启动，未探测） |
| Admin 服务 | `http://127.0.0.1:8080` — **连接被拒绝，未运行** |
| Web 服务 | `http://127.0.0.1:8881` — **连接被拒绝，未运行** |
| Admin UI | 未启动联调 |
| Web UI | 未启动联调 |
| Maven | **未安装/不在 PATH**，单元测试未执行 |
| 测试时间 | 2026-08-05 |

---

## 3. SQL 实际执行记录

执行工具：`scripts/run_cnpa_migrations.py`（PyMySQL 直连联调库）

| 文件 | 是否执行 | 结果 | 说明 |
|------|----------|------|------|
| `cnpa-dynamic-format-r1-data-check.sql` | 部分 | 只读检查，迁移前未完整归档 | 通过 Python 脚本执行 SELECT |
| `cnpa-dynamic-format-r1-migration.sql` | 是 | **成功**，10 条语句 | 新建模板/快照/阶段表，扩展 division |
| `cnpa-dynamic-format-r1-seed.sql` | 是 | **成功**，13 条语句 | 模板种子 2 条 |
| `cnpa-dynamic-format-r2-migration.sql` | 是 | **成功**，16 条语句 | 快照版本键、个人报名、分组表等 |
| `cnpa-dynamic-format-r2-backfill.sql` | 否 | 跳过 | 历史无快照 Division，留待 MigrationRunner |
| `cnpa-dynamic-format-r2-data-check.sql` | 否 | 跳过 | — |
| `cnpa-dynamic-format-r3-migration.sql` | 是 | **成功**，14 条语句 | stage_team/qualification/match/standing 扩展 |
| `cnpa-dynamic-format-r3-permission.sql` | 是 | 成功，18 执行 / 6 跳过 | 菜单 2320-2336 部分已存在 |
| `cnpa-dynamic-format-r3-data-check.sql` | 否 | 跳过 | R3 表已建，无阶段业务数据 |
| `cnpa-dynamic-format-r4-migration.sql` | 是 | **成功**，9 执行 / 1 跳过 | rank_reason 表、MANUAL 字段、active_unique_key 回填 |
| `cnpa-dynamic-format-r4-permission.sql` | 是 | **成功**，6 条 | 菜单 2337-2339 |
| `cnpa-dynamic-format-r4-test-data.sql` | 是 | **成功** | 创建 `basket_r4_test_marker` |
| `cnpa-dynamic-format-r4-data-check.sql` | 是 | 见第 12 节 | 通过 `scripts/run_data_check.py` |

**迁移前数据库状态**：仅有 V4 基础表（series/season/division 等），**未执行** R1/R2/R3 动态赛制 DDL。  
**迁移后**：`basket_format_template`、`basket_division_format_snapshot`、`basket_division_stage`、`basket_division_stage_team`、`basket_standing_rank_reason` 等均已创建。

---

## 4. 数据库最终结构变化

### 4.1 rank reason（R4 新增）

- 表：`basket_standing_rank_reason`
- 字段：division_id, stage_id, group_id, standing_id, event_team_id, rule_code, priority_no, compare_value, reason_text, is_decisive
- `rank_explain_json`：**保留字段，应用层停止新写入**（`TieBreakStandingService` 已改）

### 4.2 Standing 唯一键（R4 修正）

- 新格式：`DIVISION:{divisionId}:STAGE:{stageId}:GROUP:GLOBAL|{groupId}:TEAM:{eventTeamId}`
- 代码：`TieBreakStandingService.buildActiveUniqueKey(divisionId, stageId, groupScopeKey, eventTeamId)`
- 历史 division 级 standing（无 stage_id）：active_unique_key 置 NULL，共 **2 条**（division_id=5）

### 4.3 MANUAL 同分（R4 新增）

- `basket_standing` 新增：manual_rank_no, manual_rank_reason, manual_rank_by, manual_rank_time
- API：`POST /basket/v4/admin/stages/{stageId}/manual-rank`
- 权限：`basket:v4Standing:manualRank`

### 4.4 权限（R4 补充）

- 2337 V4人工排名处理
- 2338 V4阶段初始化队伍
- 2339 V4取消晋级确认

---

## 5. 前后台页面收口

| 页面 | 状态 | 说明 |
|------|------|------|
| 赛事发布 | 静态代码已有（R1/R2） | 本轮未做 UI 联调 |
| 报名审核 | 静态代码已有 | 未联调 |
| 个人组队 | 静态代码已有 | 未联调 |
| 赛事分组 | 静态代码已有 | 未联调 |
| 阶段执行 | 已有页签 | 未联调 |
| 赛程 Legacy 收口 | **已改代码** | 历史赛程只读标签；新 Division（有 formatSnapshotId）隐藏生成按钮；后端拒绝 Legacy 生成 |
| 比赛/数据审核 | 静态代码已有 | 未联调 |
| 统计榜单 | **已改代码** | 接入阶段维度积分榜、结构化排名依据 |
| Web 我的赛事/赛程 | 部分（R3） | 未联调 |
| Web 公开赛事详情 | 部分 | 未联调 |

**修改文件**：
- `basketball-admin-ui/src/views/cnpa/event-v4/schedule/index.vue`
- `basketball-admin-ui/src/views/cnpa/event-v4/stats/index.vue`
- `basketball-admin-ui/src/api/cnpa/event-v4/stage-execution.ts`
- `basketball-admin-ui/src/api/cnpa/event-v4/options.ts`

---

## 6. 场景 A 结果 — TEAM 5V5 四队单循环

| 项 | 值 |
|----|-----|
| 状态 | **未执行** |
| 阻塞原因 | Admin/Web 服务未启动，无法通过接口/页面创建 Division 并完成 6 场比赛链路 |
| Division ID | — |
| Match 数 | — |
| 是否通过 | **否** |

---

## 7. 场景 B 结果 — TEAM 3V3 五队单循环

| 项 | 值 |
|----|-----|
| 状态 | **未执行** |
| 阻塞原因 | 同上 |
| 是否通过 | **否** |

---

## 8. 场景 C 结果 — 16 队小组赛→8 强淘汰

| 项 | 值 |
|----|-----|
| 状态 | **未执行** |
| 阻塞原因 | 同上 |
| 是否通过 | **否** |

---

## 9. 场景 D 结果 — PERSONAL 3V3 两队单败淘汰

| 项 | 值 |
|----|-----|
| 状态 | **未执行** |
| 阻塞原因 | 同上 |
| 是否通过 | **否** |

---

## 10. 异常与幂等测试结果

| 场景 | 预期 | 实际 | 是否通过 |
|------|------|------|----------|
| 全部 20.x 子项 | 接口拒绝/幂等 | **未执行** | **否** |
| Legacy 新 Division 生成赛程 | 后端拒绝 | 代码已实现，**未接口复验** | 待复验 |
| MANUAL 未处理禁止确认晋级 | 拒绝 | 代码已有（R3），**未联调** | 待复验 |

---

## 11. 权限与越权测试

| 项 | 结果 |
|----|------|
| R4 权限 SQL | 已执行，菜单 2337-2339 已插入 |
| 接口越权实测 | **未执行**（服务未启动） |
| 前端按钮权限 | **未执行** |

---

## 12. 数据一致性检查

通过 `scripts/run_data_check.py` 执行 `cnpa-dynamic-format-r4-data-check.sql`：

| 检查项 | 异常数 | 处理 |
|--------|--------|------|
| rank_explain_json 新写入 | 0 | — |
| active_unique_key 重复 | 0 | — |
| 有效记录 active_unique_key 为空 | **2** | division_id=5 历史 standing，无 stage_id，已置 NULL |
| 历史缺 stage_id | **2** | 同上，legacy division 级积分榜 |
| Division 无 format_snapshot_id | **5** | 历史 Division 1-5，需 MigrationRunner 或手工创建快照 |
| 已开放报名但快照非 LOCKED | **2** | division 3、4，历史数据 |
| Legacy 与新 Division 混用赛程 | 0 | — |

---

## 13. 业务审计验证

**未执行**（无业务操作联调）。

---

## 14. 实际修复的 Bug

### P0

| 问题 | 根因 | 修改 | 验证 | 关闭 |
|------|------|------|------|------|
| 联调库未执行 R1-R3 DDL | 迁移脚本未落地 | `run_cnpa_migrations.py` 执行全套迁移 | SQL 表已创建 | **是** |
| rank_explain_json 违反 DB 规范 | R3 使用 JSON 存排名解释 | 新增 `basket_standing_rank_reason` + 改 `TieBreakStandingService` | 代码审查；DB 表已建 | **是（代码+DDL）** |
| active_unique_key 格式不规范 | R3 使用 `{stageId}_{scope}_{teamId}` | R4 migration 回填 + 代码新格式 | data-check 无重复 | **是（代码+DDL）** |

### P1

| 问题 | 修改 | 验证 | 关闭 |
|------|------|------|------|
| Legacy 入口可能误操作新 Division | 前后端双拦截 + UI 隐藏 | 未接口复验 | **部分** |
| MANUAL 排名无处理入口 | 后端 API + DB 字段 | Admin UI 对话框未完整实现 | **部分** |
| Stats 页仍用 Division legacy API | 接入 stage standings + rank reason 展示 | 未页面复验 | **部分** |
| Admin options 无法判断新旧 Division | options 增加 formatSnapshotId | 未复验 | **部分** |

### P2

- MANUAL 排名 Admin 弹窗 UI 未完整实现（仅后端 API）
- Web 公开赛事阶段维度展示仍待联调

---

## 15. 未完成事项

### 五轮范围内仍应完成

1. 启动 Admin/Web/Redis，执行场景 A-D 全链路
2. Maven 安装后执行 `RoundRobinStageScheduleStrategyTest`、`GroupStageScheduleStrategyTest`
3. 异常/幂等/并发/权限实测并记录
4. 历史 5 个 Division 快照迁移（MigrationRunner 或 backfill）
5. MANUAL 排名 Admin UI 完整对话框
6. Web 端我的赛程/公开详情阶段维度联调

### 五轮后专项

- BO3/BO5、MatchSeries、双败等（规格已排除）

### 非阻塞 P2

- Stats 页球队/球员统计阶段筛选
- Web 排名依据仅展示 decisive reason（后端已支持）

---

## 16. R4 验收结论

**不通过**

依据：

- P0 联调项：核心 TEAM/PERSONAL 链路 **未真实运行**
- 三种基础赛制 **未 E2E 验证**
- Admin/Web 服务 **未启动**，场景 A-D 全部阻塞
- Maven 单元测试 **未执行**（仅静态代码存在）
- 不得将“代码已实现”等同于“联调通过”

---

## 17. R5 清理候选清单

| 类别 | 路径/表名 | 读写依赖 | R5 可删 | 前置条件 | 风险 |
|------|-----------|----------|---------|----------|------|
| Legacy Schedule Service | `BasketV4ScheduleBatchServiceImpl` Legacy 生成 | 历史 Division 只读 | 部分 | 无 Legacy 批次 | 中 |
| Legacy Schedule UI | `schedule/index.vue` batch 页签 | 只读查看 | 是 | 历史迁移完成 | 低 |
| 旧 Tournament Controller | `BasketTournamentScopeController` 等 | 旧菜单已隐藏 | 是 | 确认无读依赖 | 高 |
| 旧 Tournament Service | `BasketTournamentServiceImpl` 等 | Web legacy 路由 | 是 | `/tournament/legacy` 下线 | 高 |
| rank_explain_json 字段 | `basket_standing.rank_explain_json` | 已停止写入 | 是 | 确认 rank_reason 全覆盖 | 低 |
| V2 旧表 | tournament 相关表 | 部分只读 | 是 | 数据归档 | 高 |
| PS/XBOX 兼容 | `PlatformCodeEnum.legacy` | 历史 Division | 部分 | PS_XBOX 迁移完成 | 中 |
| MigrationRunner | `BasketV4FormatSnapshotMigrationRunner` | 启动时一次性 | 是 | 全部 Division 有快照 | 低 |
| 旧 Schedule 菜单 | `cnpa/competition/schedule-manage` | Legacy 赛季 | 是 | 使用量归零 | 中 |
| Mock/Smoke 脚本 | `scripts/smoke_v4_r3_*.py` | 测试 | 保留至 R5 后 | — | 低 |

---

## 18. R5 前置条件

1. R4 验收结论改为 **通过** 或 **有条件通过**
2. 场景 A-D 全链路真实通过
3. 全部 P0/P1 关闭
4. 历史 Division 快照迁移完成
5. Legacy 赛程只读访问量归零
6. `rank_explain_json` 确认零新写入且 rank_reason 覆盖全部 stage standing
7. 物理删除前完成数据归档与 rollback 演练

---

## 附录：本轮代码变更摘要

### 后端

- `BasketStandingRankReason` 实体/Mapper
- `TieBreakStandingService` — 结构化 rank reason，新 active_unique_key，停止 JSON 写入
- `DivisionStageExecutionApplicationServiceImpl` — listStandings 聚合 reason；applyManualRank
- `BasketV4StageExecutionController` — manual-rank 端点
- `BasketV4ScheduleBatchServiceImpl` — 拒绝 formatSnapshotId 非空 Division Legacy 生成
- `BasketV4AdminOptionVo` — formatSnapshotId

### SQL

- `docs/sql/cnpa-dynamic-format-r4-migration.sql`
- `docs/sql/cnpa-dynamic-format-r4-data-check.sql`
- `docs/sql/cnpa-dynamic-format-r4-permission.sql`
- `docs/sql/cnpa-dynamic-format-r4-test-data.sql`
- `docs/sql/cnpa-dynamic-format-r4-test-data-cleanup.sql`

### 工具

- `scripts/run_cnpa_migrations.py`
- `scripts/run_data_check.py`
