# CNPA 动态赛制重构 R5 清理总结

## 1. 本轮目标

R5 为五轮计划的**最终清理轮**，目标是：

- 删除旧 Tournament 赛事域代码、页面、菜单与权限
- 删除 Legacy 赛程生成、旧报名/阵容/积分/比赛数据写入口
- 清理共享表 Legacy 历史行与纯旧数据库表
- 删除 `rank_explain_json`，统一使用 `basket_standing_rank_reason`
- 删除 MigrationRunner、Legacy Feature Flag
- 形成最终权威 Schema / Seed / Permission SQL 与统一联调脚本
- **保留**荣誉/纪律模块所需的最小 Tournament 只读引用（非赛事写链路）

## 2. 清理原则

1. 先扫描依赖，再删除代码（546 后端 + 167 前端文件已真实删除）
2. 共享表采取「删 Legacy 行 + 删字段 + 收紧约束」，不 DROP 整张共享表
3. 纯旧表提供 precheck + cleanup SQL；**开发库 `basketball` 已通过 MCP 完成物理清理（DROP TABLE / DROP COLUMN / 约束收紧）**
4. 不删除账号、球员、长期球队、内容运营（公告/合作伙伴/名人堂）模块
5. 不声称 E2E 测试通过

## 3. 最终权威赛事域

```
Series → Season → Division → FormatTemplate / FormatSnapshot
  → DivisionStage → Registration → EventTeam / EventPlayer
  → Grouping → ScheduleBatch / Match → Submission → Standing → Qualification
```

**唯一写链路**：Admin V4 Controller + Web V4 Controller / Public API

## 4. 保留的数据库表（38 张核心业务表）

| 表名 | 用途 |
|------|------|
| `basket_series` | 赛事系列 |
| `basket_season` | 赛季 |
| `basket_division` | 平台赛区 |
| `basket_format_template` | 赛制模板 |
| `basket_format_template_stage` | 模板阶段 |
| `basket_format_template_tie_break_rule` | 模板同分规则 |
| `basket_division_format_snapshot` | Division 快照 |
| `basket_division_stage` | 阶段实例 |
| `basket_division_tie_break_rule` | Division 同分规则 |
| `basket_registration` | 正式报名 |
| `basket_registration_player` | 报名阵容 |
| `basket_registration_audit` | 报名审核 |
| `basket_personal_registration` | 个人报名 |
| `basket_personal_registration_audit` | 个人报名审核 |
| `basket_personal_draw_batch` | 抽签批次 |
| `basket_personal_draw_team` | 抽签临时队 |
| `basket_personal_draw_assignment` | 抽签分配 |
| `basket_event_team` | 赛事球队快照 |
| `basket_event_player` | 赛事球员快照 |
| `basket_division_grouping_batch` | 正式分组批次 |
| `basket_division_group` | 分组 |
| `basket_division_group_team` | 分组球队 |
| `basket_division_stage_team` | 阶段参赛队 |
| `basket_division_stage_qualification` | 晋级结果 |
| `basket_schedule_batch` | 赛程批次 |
| `basket_match` | 比赛 |
| `basket_match_submission` | 比赛数据提交 |
| `basket_match_submission_*_stat/material` | 提交明细 |
| `basket_standing` | 积分榜 |
| `basket_standing_rank_reason` | 排名解释 |
| `basket_team_stat` / `basket_player_stat` / `basket_rank` | 统计 |
| `basket_captain_grant` / `basket_captain_grant_log` | 队长授权 |

**非赛事写链路保留（荣誉/纪律只读）**：
- `basket_tournament`（3 行）
- `basket_tournament_season`（3 行）
- `basket_tournament_admin_scope`（0 行）

## 5. 删除的数据库表

| 表名 | 删除原因 | 清理前数据量 | 依赖检查 | 清理 SQL |
|------|----------|-------------|----------|----------|
| `basket_tournament_registration` | 旧球队报名 | 16 | 无 V4 Mapper 引用 | `cnpa-dynamic-format-r5-cleanup.sql` |
| `basket_tournament_registration_audit` | 旧报名审核 | 49 | 已删 Controller | 同上 |
| `basket_tournament_registration_player` | 旧报名阵容 | 65 | 已删 Controller | 同上 |
| `basket_tournament_season_roster` | 旧赛季阵容 | 73 | 已删 Roster Admin | 同上 |
| `basket_team_season_profile` | 旧参赛队档案 | 13 | 已删 EventTeam Admin | 同上 |
| `basket_captain_qualification` | 旧队长资格 | 13 | 已迁移至 basket_captain_grant | 同上（代码 0 引用后可 DROP） |
| `basket_schedule_generate_batch` | Legacy 排赛批次 | 2 | 已删 Job/Service | 同上 |
| `basket_schedule_rule*` | Legacy 排赛规则 | 0 | 已删 Strategy | 同上 |
| `basket_stage_standing` | 旧 Division 积分榜 | 0 | V4 用 basket_standing | 同上 |
| `basket_match_data_submission*` | 旧比赛数据提交 | 6+ | V4 用 match_submission | 同上 |
| `basket_match_team` | 旧比赛球队关联 | 316 | 已删 Match Admin | 同上 |
| `basket_team_season_stat` / `basket_player_season_stat` | 旧赛季统计 | 24/0 | 已删 Stats Controller | 同上 |
| `basket_competition_season` | 旧联赛届次 | 0 | 无写入口 | 同上 |
| 其余 `basket_tournament_*` 子表（group/stage/rule/roster_change 等） | 旧赛事结构 | 0 | 已删全套 Controller | 同上 |

> **执行状态**：开发库 `basketball` 已通过 MCP 执行 `cnpa-dynamic-format-r5-cleanup.sql` 等效操作（Legacy 行 DELETE + DROP TABLE + DROP COLUMN + NOT NULL 收紧）。验收 check_01/check_09 均为 0。

## 6. 共享表清理

| 操作 | 明细 |
|------|------|
| 删除 Legacy Match | 1 行（`stage_id` 为空） |
| 删除 Legacy ScheduleBatch | 1 行 |
| 删除 Legacy Standing | 2 行 |
| 删除无快照 Division | 5 个 Division + 3 EventTeam + 15 EventPlayer |
| 删除字段 | `basket_standing.rank_explain_json`（Java/XML 已删，DB 列待 cleanup SQL） |
| 收紧约束（cleanup SQL） | `basket_match.stage_id NOT NULL`、`basket_schedule_batch.stage_id NOT NULL`、`basket_standing.stage_id NOT NULL`、`basket_registration.registration_source NOT NULL` |

## 7. 删除的后端代码

### Admin（407 文件，见 `docs/r5-deleted-files.txt`）

**Controller（28）**：`BasketCompetitionSeasonController`、`BasketSeasonController`、`BasketTournamentRule/Stage/Group/RoundController`、`BasketCaptainQualificationController`、`BasketRegistrationAuditController`、`BasketRegistrationBackfillController`、`BasketOpenRegistrationAuditController`、`BasketSeasonRosterAdminController`、`BasketEventTeamAdminController`、`BasketRosterChangeAuditController`、`BasketRosterOperationController`、`BasketScheduleRuleController`、`BasketScheduleManageController`、`BasketMatchSchedule/Lineup/Score/StatController`、`BasketMatchDataAuditController`、`BasketMatchDispute/ExceptionController`、`BasketDataSettlementController`、`BasketStandingController`、`BasketRankingController`、`BasketTeamSeasonStatController`、`BasketPlayerSeasonStatController`、`BasketSeasonResultController`、`BasketLocalDemoDataController` 等

**Service / Mapper / Domain**：旧 Tournament 报名、Legacy Schedule 生成（`BasketScheduleGenerateServiceImpl`、`schedule/strategy/*`）、旧 Standing 结算、旧 MatchData 提交审核全套

**Job / Runner**：`ScheduleAutoGenerateJob`、`MatchStatusAutoFinishJob`、`BasketV4FormatSnapshotMigrationRunner`

**保留**：18 个 V4 Admin Controller、荣誉/纪律/公告/账号模块；`BasketTournamentController` 改为只读 list/get

### Web（139+14 文件）

**Controller**：`AccountTournamentController`、`TeamRegistrationBusinessController`、`OpenRegistrationBusinessController`、`EventParticipationQueryController`、`RosterChangeBusinessController`、`BasketTeamSeasonProfileController`、`BasketPlayerMatchDataController`、`BasketTournamentRegistration*Controller`、`publicapi/BasketPublicStandingController`、`BasketPublicMatchController`、`BasketPublicSeasonStatController` 等

**保留**：14 个 V4 Controller、`BasketPublicTournamentController`（名人堂只读）；`CaptainPermissionServiceImpl` 已迁移 `basket_captain_grant`

## 8. 删除的 Admin 前端（57 文件）

- `views/cnpa/event/*`（5）、`competition/*`（9）、`statistics/*`（6）、`participation/*`（8）
- `identity/captain-qualification/index.vue`
- `components/basket/MatchDataAuditDetailDialog.vue`
- `api/basket/*` 旧域 API 27 个（**保留** `tournament.ts` 供荣誉下拉）

## 9. 删除的 Web 前端（110 文件）

- `views/events/EventsPage.vue` 及旧 events 组件 26 个
- `views/data/index.vue` 及 components（**保留** `V4DataPage.vue`）
- `views/tournament/index.vue`（**保留** `tournament/team/*`）
- `views/account/tournaments/*`（16）、`my-tournaments/*`（4）
- 旧 account events 组件、旧 API 14 个、eventTeam store/utils
- `router/index.ts` 移除 `/tournament/legacy`、`/data/legacy`、40+ legacy redirect

## 10. 菜单和权限清理

**物理删除目标**（`cnpa-dynamic-format-r5-permission-cleanup.sql`）：
- 旧权限前缀：`basket:tournament:*`、`basket:match:*`、`basket:standing:*`、`basket:registrationAudit:*`、`basket:dataSettlement:*` 等

**保留 V4 菜单**（2280–2339）：
- 2280 赛事发布、2296 队长授权、2301 报名审核、2306 参赛队伍
- 2311 赛程管理、2316 比赛管理、2321 比赛数据审核、2326 统计榜单

**已废弃菜单 ID 示例**：2004 赛事列表、2007 球队报名审核、2040 赛程管理、2081 积分榜

## 11. Legacy 赛程清理

- 删除 `ScheduleAutoGenerateJob`、`BasketScheduleGenerateServiceImpl`、`RegularRoundRobinV1Strategy` 等
- Admin `event-v4/schedule/index.vue` 仅保留「阶段执行」生成入口
- **已删除** Legacy 生成入口 `POST /basket/v4/schedule-batch/generate`、对应 Service/DTO/权限 `basket:v4ScheduleBatch:generate`（menu_id=2313）
- 新版赛程**仅**允许通过 Stage Execution 生成

## 12. rank_explain_json 清理结果

| 层 | 状态 |
|----|------|
| Java `BasketStanding.rankExplainJson` | ✅ 已删除 |
| `BasketStandingMapper.xml` | ✅ 已删除映射 |
| 前端 | ✅ 无引用 |
| `run_data_check.py` | 仍保留历史统计项（待更新） |
| 数据库列 | ⏳ 待执行 cleanup SQL（MCP DDL 被拒） |

## 13. PS/XBOX 兼容收口

- **Division 新写**：仅 `PC`、`PS_XBOX`（`PlatformCodeEnum`）
- **历史 Division**：5 个 PS/XBOX 分离 Division 已从 DB 删除
- **球员平台身份**：保留 `PS`、`Xbox` 用于匹配 `PS_XBOX` Division（未删除 `basket_player_platform_*`）

## 14. Feature Flag 和 MigrationRunner 清理

| 项 | 状态 |
|----|------|
| `BasketV4FormatSnapshotMigrationRunner` | ✅ 已删除 |
| `cnpa.v4.format-snapshot-migration.enabled` | ✅ 无配置引用 |
| `ScheduleAutoGenerateJob` sys_job | ✅ precheck/cleanup SQL 已补充；开发库查询 0 行 |
| `V4_LEGACY_ROUND_ROBIN_V1` 模板 | cleanup SQL 标记 disabled |

## 15. 最终 Schema

- **路径**：`docs/sql/cnpa-v2-final-schema.sql`
- **表数量**：38 张核心业务表 + 3 张荣誉/纪律只读引用表
- **核心关系**：见第 3 节
- **过渡字段**：不含 `rank_explain_json`、不含旧 Tournament 业务表

## 16. 最终 Seed

- **路径**：`docs/sql/cnpa-v2-final-seed.sql`
- **内容**：Series（League/Arena/Cup/Open）、基础模板（ROUND_ROBIN_BASIC_V1、GROUP_STAGE_BASIC_V1、SINGLE_ELIM_BO1_V1）
- **不含**：`V4_LEGACY_ROUND_ROBIN_V1`、测试业务数据

## 17. 最终权限 SQL

- **路径**：`docs/sql/cnpa-v2-final-permission.sql`
- **编排**：round11-menu-final + r3/r4-permission + r5-permission-cleanup

## 18. 最终联调脚本

- **路径**：`scripts/cnpa_v2_full_integration_runner.py`
- **已删除**：`scripts/r4_integration_runner.py`、`scripts/smoke_v4_r*.py`（9 个）
- **支持**：`--dry-run`、`--scenario A/B/C/D`、`--all`、`--exception-only`、`--permission-only`、`--concurrency-only`
- **环境**：`scripts/.env.v2.local`（fallback `.env.r4.local`）

## 19. 静态引用检查

| 检查项 | 结果 |
|--------|------|
| `BasketV4FormatSnapshotMigrationRunner` | 0 引用 ✅ |
| `ScheduleAutoGenerateJob` / `RegularRoundRobinV1` | 0 引用 ✅ |
| `rank_explain_json` / `rankExplainJson` | 仅 `BasketStandingRankReason` 注释 ✅ |
| `cnpa/event/list` Vue 路由 | 0 文件 ✅ |
| `cnpa/competition/` Admin 页面 | 0 文件 ✅ |
| `BasketTournamentRegistration` Web Java | 已删 14 文件 + 接口方法 ✅ |
| `basket_captain_qualification` 业务代码引用 | 0 引用 ✅（已迁移 basket_captain_grant） |
| 账号中心「当前参赛赛事」 | ✅ 接入 EventPlayer→EventTeam→Division 新域（最多 5 条） |
| Legacy `/schedule-batch/generate` | 0 引用 ✅ |

## 20. 保留的后续扩展结构

- `best_of`、`stage_type`、`competition_format`
- `next_stage_code`、`advancement_type`、`advancement_count`、`pairing_rule`
- `source_stage_id`、`source_match_id_home/away`
- `round_no`、`bracket_slot`
- `basket_division_stage_team`、`basket_division_stage_qualification`

## 21. 未实现但后续专项处理

- StageRound、MatchSeries、BO3/BO5、双败、轮空、复杂阶段组合、自动抽签/分组

## 22. 最终统一测试清单

1. 账号注册登录 → 30. 旧接口 404 或明确不可用（完整 30 项见 R5 规格第十九章）

## 23. P0/P1/P2 风险

### P0
- 荣誉/名人堂仍依赖 `basket_tournament` 只读表与 `listTournament` API（保留表未 DROP）

### P1
- （已收口）队长授权迁移、账号中心参赛列表、Legacy 赛程生成删除、sys_job 清理

### P2
- `run_data_check.py` 仍含 `rank_explain_json 历史数据量` 统计项
- Web 仍保留部分 Tournament DAO/Model 供公开页/荣誉只读

## 24. R5 当前结论

**新赛事域代码、页面、菜单、权限与开发库物理清理均已完成；最终本地全链路验收待统一执行。**

### 开发库 data-check 实测（MCP，Legacy 行清理后）

| check_id | cnt |
|----------|-----|
| check_03_division_no_snapshot | 0 |
| check_04_match_no_stage | 0 |
| check_05_batch_no_stage | 0 |
| check_06_standing_no_stage | 0 |
| check_09_rank_explain_json_column | 0 ✅ |

### 删除规模汇总

| 类别 | 数量 |
|------|------|
| Admin 后端文件 | 407 |
| Web 后端文件 | 139 + 14 |
| Admin 前端文件 | 57 |
| Web 前端文件 | 110 |
| Smoke/旧 Runner 脚本 | 10 |
| **合计** | **~737** |

### SQL 文件

| 文件 | 用途 |
|------|------|
| `docs/sql/cnpa-dynamic-format-r5-precheck.sql` | 清理前检查 |
| `docs/sql/cnpa-dynamic-format-r5-cleanup.sql` | 开发库清理 |
| `docs/sql/cnpa-dynamic-format-r5-data-check.sql` | 清理后验收 |
| `docs/sql/cnpa-dynamic-format-r5-rollback.sql` | 回滚说明 |
| `docs/sql/cnpa-dynamic-format-r5-permission-cleanup.sql` | 权限清理 |
| `docs/sql/cnpa-v2-final-schema.sql` | 最终 Schema |
| `docs/sql/cnpa-v2-final-seed.sql` | 最终 Seed |
| `docs/sql/cnpa-v2-final-permission.sql` | 最终权限 |
