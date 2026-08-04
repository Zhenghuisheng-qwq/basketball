# CNPA 动态赛制重构 R4 联调验收总结

**测试批次编码**：`R4_TEST_BATCH_20260805`  
**文档更新时间**：2026-08-05  
**本轮结论**：**R4 代码收口与联调资产已完成，真实运行验收待用户本地环境执行。**

---

## 1. 本轮目标

R4 为五轮计划中唯一完整联调验收轮，本轮在 Cursor 环境无法稳定启动 Maven/Java/Redis/前后端的前提下，完成：

- R4 剩余前后台业务收口（MANUAL 排名 UI、Web 成队选择器、Stage 数据接入、Legacy 拦截）
- 数据库检查脚本与 backfill 资产完善
- HTTP 联调脚本与本地验收清单
- 静态引用与权限核对
- **不在 Cursor 环境伪造 HTTP/页面/单元测试通过结果**

---

## 2. 当前执行环境说明

| 项目 | 说明 |
|------|------|
| Cursor 环境 | **未要求** Maven；未启动 Admin/Web/Redis |
| 用户本地环境 | 用户已确认项目可正常运行 |
| 真实 HTTP 联调 | **未在 Cursor 执行**；场景 A～D 标记为待本地执行 |
| 结论定位 | Cursor 环境缺少 Maven **不是**项目代码 P0 |

---

## 3. 已完成的数据库迁移

执行工具：`scripts/run_cnpa_migrations.py`

| 文件 | 已执行 | 说明 |
|------|--------|------|
| `cnpa-dynamic-format-r1-migration.sql` | 是 | 模板/快照/阶段表 |
| `cnpa-dynamic-format-r1-seed.sql` | 是 | 模板种子 |
| `cnpa-dynamic-format-r2-migration.sql` | 是 | 个人报名/分组等 |
| `cnpa-dynamic-format-r3-migration.sql` | 是 | stage_team/qualification 等 |
| `cnpa-dynamic-format-r3-permission.sql` | 是 | 菜单 2320-2336 |
| `cnpa-dynamic-format-r4-migration.sql` | 是 | rank_reason、MANUAL 字段 |
| `cnpa-dynamic-format-r4-permission.sql` | 是 | 菜单 2337-2339 |
| `cnpa-dynamic-format-r4-test-data.sql` | 是 | marker 表 |
| `cnpa-dynamic-format-r2-backfill.sql` | 否（只读检查） | 推荐 Java MigrationRunner |
| `cnpa-dynamic-format-r4-data-check.sql` | 是（检查） | `scripts/run_data_check.py` |

**请勿重复执行**非幂等 DDL。data-check / backfill 查询段落可重复运行。

---

## 4. R4 实际代码收口内容

| 模块 | 状态 | 关键路径 |
|------|------|----------|
| rank reason 结构化 | 完成 | `TieBreakStandingService`、`basket_standing_rank_reason` |
| Standing active_unique_key | 完成 | R4 migration + 代码新格式 |
| MANUAL 排名后端 | 完成 | `POST /basket/v4/admin/stages/{stageId}/manual-rank` |
| MANUAL 排名 Admin UI | **完成** | `basketball-admin-ui/.../stats/index.vue` |
| Legacy 赛程拦截 | 完成 | `BasketV4ScheduleBatchServiceImpl` + schedule 页 |
| Web 成队球队选择器 | **完成** | `V4PersonalFormationPanel.vue` + available-teams API |
| Web 已参赛 Stage 数据 | **完成** | `BasketV4PlayerMyEventsMapper.xml` |
| Web 我的赛程 Stage/Group | **完成** | `BasketV4PlayerMatchSubmissionMapper.xml` |
| Web 公开赛事 Stage | **完成** | `BasketV4PublicStageController` |
| HTTP 联调脚本 | **完成** | `scripts/r4_integration_runner.py` |
| 数据检查脚本 | **完成** | `run_data_check.py` + 扩展 data-check SQL |

---

## 5. MANUAL 排名闭环

**后端**：`DivisionStageExecutionApplicationServiceImpl.applyManualRank`

- 校验：同分集合完整覆盖、序号不重复、必须填写原因、已确认晋级（COMPLETED）禁止修改、仅 ROUND_ROBIN/GROUP 阶段
- 保存后：`qualificationMapper.softDeletePreviewByStageId` 使 PREVIEW 失效

**Admin UI**：`basketball-admin-ui/src/views/cnpa/event-v4/stats/index.vue`

- 入口条件：已选 Division + Stage；积分型阶段；`manualRankPending=true`；权限 `basket:v4Standing:manualRank`；Stage 未 COMPLETED
- GROUP_STAGE 按 scope（小组）分别显示「人工处理排名」按钮
- Dialog：展示同分球队、积分、rank reason、人工序号、排序原因；调用 `applyV4StageManualRank`

**权限**：`basket:v4Standing:manualRank`（SQL 2337）

---

## 6. Web 成队球队选择器

**API**：`GET /basket/v4/player/personal-formation/tasks/{drawTeamId}/available-teams`

**后端**：

- `BasketV4PersonalFormationController`
- `BasketV4PersonalFormationServiceImpl.listAvailableTeams`
- `BasketV4PersonalFormationMapper.selectCandidateTeamsByCaptainMemberId`

**校验**：队长身份、球队状态、成员匹配、平台身份、人数、任务占用、Division EventTeam 冲突（绑定时 `validateTeamForBind` 复验）

**前端**：`basketball-web-ui/src/components/account/events/V4PersonalFormationPanel.vue` — 候选球队单选、不可绑定原因、创建球队入口

---

## 7. Web 阶段数据收口

### 已参加赛事

- API：`GET /basket/v4/player/my-events`
- 扩展字段：currentStage、group、standingRank、qualification、next/latest match
- 历史 Division（无 snapshot）标记 `legacyEvent`

### 我的赛程

- API：`GET /basket/v4/player/my-schedule`
- SQL 关联：`basket_division_stage`、`basket_division_group`
- 展示：stageName、groupName、roundName、双方 submission 状态

### 公开赛事详情

- `GET /basket/v4/public/stage/list`
- `GET /basket/v4/public/stage/{stageId}/standings`
- `GET /basket/v4/public/stage/{stageId}/bracket`
- 页面：`V4SeasonDetailPage.vue` 积分榜按小组 scope 展示

---

## 8. Legacy 入口收口

**后端**：`BasketV4ScheduleBatchServiceImpl.validateDivisionForGenerate` — `formatSnapshotId` 非空时拒绝 Legacy 生成

**Admin UI**：`schedule/index.vue`

- Tab：阶段执行 / 历史赛程（只读）
- 新 Division（`formatSnapshotId` 非空）隐藏 Legacy「生成草稿」

---

## 9. 历史 Division 快照处理方案

**推荐**：启动 Admin 时一次性执行

```yaml
cnpa:
  v4:
    format-snapshot-migration:
      enabled: true
```

类：`basketball-admin/.../BasketV4FormatSnapshotMigrationRunner.java`

**SQL 资产**：`docs/sql/cnpa-dynamic-format-r2-backfill.sql`（A0/A1 只读检查 + 注释模板）

**规则**：已有业务数据 → IN_USE；已开放报名无业务 → LOCKED；DRAFT Division → DRAFT

执行后关闭 enabled，不影响日常启动。

---

## 10. HTTP 联调脚本

**路径**：`scripts/r4_integration_runner.py`

**配置**：`scripts/.env.r4.example` → 复制为 `scripts/.env.r4.local`

```bash
python scripts/r4_integration_runner.py --dry-run
python scripts/r4_integration_runner.py --scenario A
python scripts/r4_integration_runner.py --scenario B
python scripts/r4_integration_runner.py --scenario C
python scripts/r4_integration_runner.py --scenario D
python scripts/r4_integration_runner.py --all
python scripts/r4_integration_runner.py --exception-only
```

**输出**：`scripts/output/r4-integration-result.json`

**说明**：场景 A～D 框架与断言清单已准备；完整 TEAM/PERSONAL 链路需本地服务与用户测试账号执行，脚本不会伪造通过。

---

## 11. 数据一致性检查

**SQL**：`docs/sql/cnpa-dynamic-format-r4-data-check.sql`（23 项，含 Legacy 统计）

**运行**：

```bash
python scripts/run_data_check.py
python scripts/run_data_check.py --json
```

**输出**：`scripts/output/r4-data-check-result.json`

**Legacy 隔离**：检查 20～23 为 STAT 统计项，不与新主链路异常混计。

---

## 12. 权限收口

| 编码 | 用途 |
|------|------|
| `basket:v4Standing:manualRank` | 人工排名 |
| `basket:v4Stage:initialize` | 阶段初始化队伍 |
| `basket:v4Stage:qualificationCancel` | 取消晋级确认 |
| `basket:v4Stage:*` | 阶段执行全链路 |
| `basket:v4FormatRule:*` | 规则查看/编辑/锁定 |

Admin 前端 `v-hasPermi` / `checkPermi` 与后端 `@PreAuthorize` 常量 `BasketEventV4Perms` 一致。Web Player 接口不复用 Admin 权限。

---

## 13. 静态引用检查

| 层 | 结论 |
|----|------|
| 后端 Entity/Mapper/XML | MANUAL 字段、stage_id/group_id、active_unique_key 已对齐 |
| Admin API ↔ Vue | `stage-execution.ts` manual-rank DTO 与 Controller 一致 |
| Web API ↔ Vue | available-teams、my-events、my-schedule、public stage 路径已接入 |
| Legacy 拦截 | 前后端均检查 `formatSnapshotId` |

**未执行**：Maven 编译与运行时类加载验证（待用户本地）。

---

## 14. 用户本地联调执行清单

### 14.1 数据库

1. 确认 R1～R4 migration 已执行（见第 3 节）
2. 运行 `python scripts/run_data_check.py --json`
3. 历史 Division：启用 MigrationRunner 一次后关闭
4. backfill 只读检查：打开 `r2-backfill.sql` A0/A1 注释执行

### 14.2 Redis

检查 Admin/Web 配置文件中的 `spring.redis.host/port`；`redis-cli ping` 预期 `PONG`

### 14.3 Admin 启动

- 模块：`basketball-admin`
- 启动类：项目 Spring Boot 主类（IDEA 运行 admin 模块）
- Profile：与用户本地一致（如 `dev`）
- 预期：`http://127.0.0.1:8080` 可访问

### 14.4 Web 启动

- 模块：`basketball-web`
- 预期：`http://127.0.0.1:8881`

### 14.5 Admin UI

```bash
cd basketball-admin-ui
npm install
npm run dev
```

配置 API Base URL 指向 Admin 服务

### 14.6 Web UI

```bash
cd basketball-web-ui
npm install
npm run dev
```

### 14.7 HTTP 联调脚本

见第 10 节；先 `--dry-run`，再分场景执行

### 14.8 数据检查

见第 11 节；新主链路 ANOMALY 应为 0（Legacy 统计项除外）

### 14.9 测试数据清理

`docs/sql/cnpa-dynamic-format-r4-test-data-cleanup.sql` — 仅 `R4_TEST_%`；清理前先 SELECT 预览

---

## 15. 场景 A 本地验收项

TEAM + PC + 5V5 + 4 队单循环

**状态**：测试资产已准备，待用户本地运行。

断言：EventTeam=4；Match=6；每队 3 场；重复对阵=0

---

## 16. 场景 B 本地验收项

TEAM + PS_XBOX + 3V3 + 5 队单循环

**状态**：测试资产已准备，待用户本地运行。

---

## 17. 场景 C 本地验收项

16 队小组赛 → 8 强 BO1

**状态**：测试资产已准备，待用户本地运行。

---

## 18. 场景 D 本地验收项

PERSONAL + PS_XBOX + 3V3 + 两队 BO1

**状态**：测试资产已准备，待用户本地运行。

---

## 19. 异常、幂等和权限测试清单

| 项 | 脚本/入口 | 状态 |
|----|-----------|------|
| Legacy 新 Division 生成拒绝 | `r4_integration_runner.py --exception-only` | 待本地 |
| 未授权 Admin API | 同上 | 待本地 |
| MANUAL 未处理禁止确认晋级 | 阶段执行页 | 待本地 |
| 重复审核/晋级幂等 | 手工/扩展脚本 | 待本地 |

---

## 20. 已修复问题

### P0（代码）

| 问题 | 修改 |
|------|------|
| MANUAL 无 Admin UI | stats 页 Dialog + API 闭环 |
| Web 成队手填 teamId | available-teams + 选择器 |
| Web Stage 数据缺失 | my-events/my-schedule/public stage API |
| Legacy 误入口 | 前后端双拦截（延续 R4 前期） |

### P1

| 问题 | 修改 |
|------|------|
| data-check 误报 COUNT | run_data_check.py STAT/ANOMALY 区分 |
| 无 HTTP 联调 runner | r4_integration_runner.py |
| backfill 资产不足 | r2-backfill A0/A1 + MigrationRunner 说明 |

### P2

- 公开页 bracket 可视化待 UI 美化
- Stats 页球队/球员统计阶段筛选（非阻塞）

---

## 21. 尚待用户本地验证内容

1. 场景 A～D 真实 HTTP 全链路
2. 异常/幂等/并发/权限实测
3. MigrationRunner 对 5 个历史 Division 执行结果
4. Maven 单元测试（RoundRobin/GroupStage Strategy）
5. Admin/Web UI 浏览器人工走查

---

## 22. R4 当前结论

**R4 代码收口与联调资产已完成，真实运行验收待用户本地环境执行。**

- 不声称 R4 通过/不通过
- 不声称场景 A～D 通过
- 不声称 E2E 通过

---

## 23. 用户本地验收通过标准

- TEAM / PERSONAL 链路通过
- ROUND_ROBIN / GROUP_STAGE / BO1 SINGLE_ELIMINATION 通过
- Submission 审核、Standing、Qualification 通过
- 权限越权、名额并发通过
- 新主链路 data-check ANOMALY 为 0

---

## 24. R5 前置条件

R5 只能在用户本地完成核心链路检查后进入：

1. 场景 A～D 真实通过
2. 历史 Division 快照补齐
3. Legacy 只读访问量归零评估
4. rank_explain_json 零新写入且 rank_reason 覆盖
5. 数据归档与 rollback 演练

---

## 附录：本轮主要变更文件

### 后端（basketball-admin）

- `DivisionStageExecutionApplicationServiceImpl.java`
- `BasketV4ScheduleBatchServiceImpl.java`（已有 Legacy 拦截）

### 后端（basketball-web）

- `BasketV4PersonalFormationController/ServiceImpl/Mapper`
- `BasketV4PlayerMyEventsMapper.xml`、`V4MyEventResp.java`
- `BasketV4PlayerMatchSubmissionMapper.xml`、`V4MyMatchResp.java`
- `BasketV4PublicStageController/Mapper/ServiceImpl`
- `BasketV4Match.java`（stageId/groupId）

### Admin 前端

- `basketball-admin-ui/src/views/cnpa/event-v4/stats/index.vue`
- `basketball-admin-ui/src/api/cnpa/event-v4/stage-execution.ts`

### Web 前端

- `V4PersonalFormationPanel.vue`
- `ParticipatingEventList.vue`、`v4AccountEventsMappers.ts`
- `V4MySchedulePanel.vue`
- `V4SeasonDetailPage.vue`、`v4Public.ts`

### SQL / 脚本

- `docs/sql/cnpa-dynamic-format-r4-data-check.sql`
- `docs/sql/cnpa-dynamic-format-r2-backfill.sql`
- `scripts/r4_integration_runner.py`
- `scripts/run_data_check.py`
- `scripts/.env.r4.example`
