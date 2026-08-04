# CNPA 动态赛制重构 R3 总结

## 1. 本轮目标

基于 R1/R2 已完成的 Series → Season → Division 主线、动态赛制模板、Division 规则快照、TEAM/PERSONAL 报名、个人抽签成队、EventTeam 赛事分组、Submission 审核链路，实现首期基础阶段执行闭环：

- ROUND_ROBIN 单循环
- GROUP_STAGE 小组赛（读取 LOCKED 分组）
- SINGLE_ELIMINATION BO1 单败淘汰
- 规则快照 → 阶段准备 → 赛程生成 → 比赛审核 → 积分/胜负 → 晋级预览 → 管理员确认 → 初始化下一阶段

## 2. 实际完成能力

| 能力 | 状态 |
|------|------|
| 阶段执行状态机（execution_status） | 已实现 |
| basket_division_stage_team 阶段参赛队伍 | 已实现 |
| basket_division_stage_qualification 晋级预览/确认 | 已实现 |
| 三种赛程 Strategy + Factory | 已实现 |
| Match/ScheduleBatch/Standing 阶段维度扩展 | 已实现 |
| 积分从 stage 规则读取（非固定 2/1） | 已实现 |
| 同分规则责任链（tie_break_rule） | 已实现 |
| 阶段完成检测 + RESULT_PENDING_CONFIRM | 已实现 |
| 晋级预览/确认/下一阶段初始化 | 已实现 |
| R2 队长授权/名额并发/长期球队冲突/权限 SQL | 已实现 |
| Admin 阶段执行页签 | 已实现 |
| Web 我的赛程阶段/轮次展示优化 | 部分实现 |
| 完整 E2E 冒烟 | 未做（按规格排除） |
| BO3/BO5、MatchSeries、双败、轮空 | 未做（按规格排除） |

## 3. 数据库变更

**迁移脚本**：`docs/sql/cnpa-dynamic-format-r3-migration.sql`

| 表 | 变更 |
|----|------|
| `basket_division_stage` | +execution_status, schedule_batch_id, participant_count, qualified_count, started/completed/confirmed 时间人, current_unique_key, execution_version |
| `basket_division_stage_team` | 新建：阶段参赛队伍 |
| `basket_division_stage_qualification` | 新建：晋级预览/确认 |
| `basket_match` | +stage_id, group_id, bracket_slot, source_match_id_home/away, loser_event_team_id |
| `basket_schedule_batch` | +stage_id, grouping_batch_id, generation_strategy, expected/actual_match_count, execution_version |
| `basket_standing` | +stage_id, group_id, group_scope_key, rank_explain_json, manual_rank_pending, active_unique_key；唯一键改为 active_unique_key |
| `basket_captain_grant` | +grant_source, source_draw_team_id |

## 4. 阶段执行模型

- **规则快照**（`basket_division_stage` 规则字段 + `stage_status`）：描述「应该怎么打」，LOCKED 后不随执行改变
- **执行状态**（`execution_status`）：描述「打到哪里」
  - DRAFT → READY → SCHEDULE_GENERATED → RUNNING → RESULT_PENDING_CONFIRM → COMPLETED
- **阶段队伍**：`basket_division_stage_team`，entry_type（INITIAL/OVERALL_RANK/GROUP_RANK/MATCH_WINNER/MANUAL）
- **晋级**：`basket_division_stage_qualification`，PREVIEW → CONFIRMED，确认后写入下一阶段 stage_team

核心服务：`DivisionStageExecutionApplicationServiceImpl`

## 5. 三种赛制策略

工厂：`DivisionStageScheduleStrategyFactory`

| 策略类 | 触发条件 |
|--------|----------|
| `RoundRobinStageScheduleStrategy` | competitionFormat=ROUND_ROBIN 且非 GROUP 阶段 |
| `GroupStageStageScheduleStrategy` | stageType=GROUP 或 groupingMode≠NONE |
| `SingleEliminationStageScheduleStrategy` | competitionFormat=SINGLE_ELIMINATION |

组合校验：`StageCombinationValidator`（允许单循环/小组/淘汰/循环→淘汰/小组→淘汰，最多两阶段）

## 6. 单循环赛程

- 算法：`BasketV4RoundRobinScheduler` 圆桌法
- 奇数队轮空不生成 Match
- bestOf≠1 明确拒绝，提示 BO3/BO5 不支持

**局部验证（单元测试 `RoundRobinStageScheduleStrategyTest`）**：
- 4 队：6 场，每队 3 场，3 轮，无轮空 ✓
- 5 队：10 场，每队 4 场，5 轮，有轮空 ✓
- bestOf=3：拒绝 ✓

## 7. 小组赛赛程

- 来源：stage_team + LOCKED grouping_batch + group_team
- 每组独立单循环，match.group_id 结构化存储
- 预览提示组间人数不均衡

**局部验证（`GroupStageScheduleStrategyTest`）**：
- 16 队 4 组：每组 4 队 6 场，总计 24 场 ✓

## 8. BO1 单败淘汰

- 队伍数须为 2 的幂（2/4/8/16/32），不支持轮空
- HIGH_VS_LOW 种子对阵（1 vs N）
- 完整 bracket 结构，round_no + source_match_id 关联
- 6 队：明确拒绝 ✓（`RoundRobinStageScheduleStrategyTest.validate_singleEliminationWith6Teams_rejectedByEliminationStrategy`）

## 9. 积分与同分排序

- `BasketV4StatsSettlementServiceImpl`：从 stage 读取 win_points/loss_points/forfeit_*
- 重建式结算：`TieBreakStandingService.rebuildStageStanding(stageId, groupScopeKey)`
- 责任链：`TieBreakComparatorBuilder` 按 priority_no 执行 HEAD_TO_HEAD → WIN_COUNT → POINT_DIFFERENCE → TOTAL_POINTS → MANUAL
- 末位 event_team_id 技术排序；MANUAL pending 时禁止确认晋级
- standing 维度：division_id + stage_id + group_scope_key + event_team_id

## 10. 阶段完成条件

- 循环/小组：全部比赛 FINISHED/FORFEITED，submission 审核完成，积分榜重建，无 MANUAL 未处理
- 淘汰：当前轮全部完成且 winner 已确认；非最终轮可继续；最终轮产生冠军
- 完成后 → `RESULT_PENDING_CONFIRM`，不自动确认晋级

## 11. 晋级预览与确认

- `previewQualification(stageId)`：写入 qualification 表 status=PREVIEW
  - ROUND_ROBIN：OVERALL_TOP_N
  - GROUP_STAGE：GROUP_TOP_N（每组）
  - SINGLE_ELIMINATION：MATCH_WINNER / CHAMPION
- `confirmQualification(stageId)`：CONFIRMED + stage COMPLETED + 初始化下一阶段 stage_team（READY）
- 幂等；下一阶段有赛程后不可取消确认

## 12. R2 前置问题修复

| 问题 | 修复 |
|------|------|
| PERSONAL 队长授权 | `PersonalDrawCaptainGrantSupport.ensureGrantOnBindTeam`；来源 PERSONAL_DRAW；取消批次 `revokeGrantsOnBatchCancel` |
| PERSONAL 名额并发 | `confirmFormation` 使用 `divisionMapper.selectByIdForUpdate` + countApproved 与 TEAM 共用 |
| 长期球队冲突 | `PersonalRegistrationEligibilitySpecification` + `countActiveMembershipByPlayer` |
| 权限 SQL | `docs/sql/cnpa-dynamic-format-r3-permission.sql`（R2+R3 幂等菜单 2320–2336） |

## 13. 设计模式

- **Strategy + Factory**：`DivisionStageScheduleStrategy` / `DivisionStageScheduleStrategyFactory`
- **Chain of Responsibility**：`TieBreakComparatorBuilder`
- **Application Service**：`DivisionStageExecutionApplicationServiceImpl`（阶段编排与事务边界）
- **Specification**（R2 延续）：报名/成队/分组校验

## 14. 后端文件清单（核心）

```
constant/BasketEventV4Perms.java
domain/v4/BasketDivisionStage.java (+execution fields)
domain/v4/BasketDivisionStageTeam.java
domain/v4/BasketDivisionStageQualification.java
domain/v4/BasketV4Match.java (+stage/group/bracket fields)
domain/v4/BasketScheduleBatch.java (+stage fields)
domain/v4/BasketStanding.java (+stage/group fields)
domain/v4/BasketCaptainGrant.java (+grantSource)
enums/v4/StageExecutionStatusEnum.java
enums/v4/ScheduleGenerationStrategyEnum.java
... (其他 R3 枚举)
support/v4/stage/DivisionStageScheduleStrategy.java
support/v4/stage/RoundRobinStageScheduleStrategy.java
support/v4/stage/GroupStageStageScheduleStrategy.java
support/v4/stage/SingleEliminationStageScheduleStrategy.java
support/v4/stage/DivisionStageScheduleStrategyFactory.java
support/v4/stage/StageCombinationValidator.java
support/v4/stage/TieBreakStandingService.java
support/v4/stage/TieBreakComparatorBuilder.java
support/v4/PersonalDrawCaptainGrantSupport.java
service/v4/IDivisionStageExecutionApplicationService.java
service/impl/v4/DivisionStageExecutionApplicationServiceImpl.java
service/impl/v4/BasketV4StatsSettlementServiceImpl.java (改造)
service/impl/v4/PersonalTeamFormationApplicationServiceImpl.java (改造)
web/controller/basket/v4/BasketV4StageExecutionController.java
mapper/v4/* (StageTeam, StageQualification, 及既有 Mapper 扩展)
resources/mapper/basket/v4/*.xml
test/.../RoundRobinStageScheduleStrategyTest.java
test/.../GroupStageScheduleStrategyTest.java
```

## 15. Admin 前端文件清单

```
src/api/cnpa/event-v4/stage-execution.ts
src/views/cnpa/event-v4/schedule/index.vue（新增「阶段执行」Tab）
src/utils/v4AdminDisplay.ts（+executionStatus 标签）
```

统计页 `stats/index.vue`：仍使用 Division 级 legacy API；阶段积分榜可通过 `/stages/{stageId}/standings` API 扩展（本轮未完全改造 stats Tab）。

## 16. Web 前端文件清单

```
basketball-web/.../BasketV4PlayerMyScheduleServiceImpl.java
  - resolveStageName：循环赛/小组赛/淘汰赛/半决赛/决赛
  - resolveRoundName：八强/半决赛/决赛/小组轮次
```

官网公开页完整阶段/积分榜展示：本轮最小改动，未新增独立 DAO 查询 stage 执行态。

## 17. SQL 文件

| 文件 | 用途 |
|------|------|
| `docs/sql/cnpa-dynamic-format-r3-migration.sql` | DDL |
| `docs/sql/cnpa-dynamic-format-r3-data-check.sql` | 数据检查 |
| `docs/sql/cnpa-dynamic-format-r3-permission.sql` | R2+R3 权限 |
| `docs/sql/cnpa-dynamic-format-r3-rollback.sql` | 条件回滚说明 |

## 18. 局部验证结果

| # | 场景 | 结果 |
|---|------|------|
| 1 | 4 队单循环 6 场 | 单元测试通过 |
| 2 | 5 队单循环 10 场+轮空 | 单元测试通过 |
| 3 | 16 队 4 组 24 场 | 单元测试通过 |
| 4 | 每组前 2 晋级 8 支预览 | 服务逻辑已实现，未跑 DB 联调 |
| 5 | 8 队淘汰首轮 4 场 | 策略已实现，未跑 DB 联调 |
| 6 | 6 队淘汰拒绝 | 单元测试通过 |
| 7 | bestOf=3 拒绝 | 单元测试通过 |
| 8 | 审核重复结算不重复加分 | 重建式结算，未跑 DB 联调 |
| 9 | 小组独立积分 | 架构已实现，未跑 DB 联调 |
| 10 | 晋级确认幂等 | 服务逻辑已实现，未跑 DB 联调 |

**说明**：当前环境未配置 Maven 命令行，单元测试需在本地 `mvn test -Dtest=RoundRobinStageScheduleStrategyTest,GroupStageScheduleStrategyTest` 复验。

## 19. 未完成范围

- MatchSeries / BO3 / BO5 / StageRound / 双败 / 瑞士轮 / 轮空
- 自动随机分组、复杂种子算法、阶段画布、Flowable、JSON DSL
- Admin stats 页完整阶段/小组筛选 UI
- Web 公开赛事详情阶段积分榜/晋级区展示
- 完整 E2E 冒烟、V2 旧代码删除
- Legacy `BasketV4ScheduleBatchServiceImpl` 单循环入口仍保留（批次列表 Tab）

## 20. P0/P1/P2 风险

| 级别 | 风险 |
|------|------|
| P0 | 生产须先执行 r3-migration + permission；standing 唯一键变更需确认历史数据 active_unique_key 回填 |
| P0 | 未做 DB 联调，阶段执行全流程需在目标环境手工验证 |
| P1 | Legacy 与 Stage 双轨赛程入口并存，运营可能误用 Legacy Tab |
| P1 | Web 公开页未接 stage 执行态，C 端仍主要看 stage_code |
| P2 | stats 页未接阶段筛选；MANUAL 同分后台处理 UI 待完善 |

## 21. R4 前置条件

1. R3 migration + permission 已在目标环境执行并通过 data-check
2. 至少一条 Division 完整跑通：锁定快照 → EventTeam → 阶段初始化 → 生成/发布 → 审核 → 积分 → 晋级确认
3. 决定是否废弃 Legacy 赛程批次入口
4. 若需 BO3/BO5：单独 MatchSeries 专项设计
5. Web 公开 API 接入 stage_id 维度查询与展示
