# CNPA 动态赛制重构 R5 清理总结（V2 最终收口）

**更新时间**：2026-08-05  
**唯一权威业务流程文档**：`docs/cnpa-v2-final-business-flow.md`

## 1. 本轮目标（已完成）

R5 为五轮计划的**最终清理轮**，已完成：

- 输出并收口唯一权威业务流程文档 `docs/cnpa-v2-final-business-flow.md`（30 章 + 附录 + Mermaid）
- **彻底删除**旧 Tournament 域（Admin/Web Controller、Service、Mapper、Entity、UI API/页面 + SQL DROP）
- 荣誉/纪律/公告/名人堂关联迁移至 `basket_series` / `basket_season`（部分 DTO 字段名仍保留 `tournamentId`，语义为 `seriesId`）
- 队长授权统一 `basket_captain_grant`（`CaptainPermissionServiceImpl` 已迁移）
- 账号中心「当前参赛赛事」接入 `EventPlayer → EventTeam → Division` 新域（`AccountCenterServiceImpl.loadCurrentTournaments`）
- Legacy 赛程生成入口删除；`sys_job` 旧任务已纳入 cleanup
- `rank_explain_json` 全层删除，统一 `basket_standing_rank_reason`
- 形成最终 Schema / Seed / Permission / 联调脚本 / data-check

## 2. 最终权威赛事域

```
Series → Season → Division → FormatTemplate / FormatSnapshot
  → DivisionStage → Registration → EventTeam / EventPlayer
  → Grouping → ScheduleBatch / Match → Submission → Standing → Qualification
```

**唯一写链路**：Admin V4 Controller + Web V4 Controller / Public API

## 3. 删除的旧 Tournament 域

| 类别 | 明细 |
|------|------|
| Admin Controller/Service | `BasketTournamentController`、`BasketTournamentScopeController`、`BasketTournamentServiceImpl`、`BasketTournamentScopeServiceImpl` |
| Web Controller/Service | `BasketTournamentController`、`BasketPublicTournamentController` 及 70+ model/dao/mapper |
| Admin UI API/页面 | `tournament.ts`、`tournamentScope.ts`、`discipline/tournament-scope/index.vue` |
| 数据库表 | `basket_tournament`、`basket_tournament_season`、`basket_tournament_admin_scope`（cleanup §11-12 DROP） |
| 荣誉字段 | `basket_honor_record.tournament_id` → `series_id` |

**删除规模**：见 `docs/r5-final-deleted-files.txt`，累计约 **854** 文件。

## 4. 队长权限统一（basket_captain_grant）

| 项 | 结果 |
|----|------|
| `CaptainPermissionServiceImpl` | 读取 `BasketCaptainGrant` + `CaptainGrantSupport` |
| `CaptainGrantSeasonSupport` | 已改为 V4 `BasketSeason` / `SeasonStatusEnum` |
| `basket_captain_qualification` | 运行时代码引用 **0**；cleanup SQL DROP |

## 5. 账号中心当前参赛赛事

| 项 | 结果 |
|----|------|
| 实现类 | `AccountCenterServiceImpl.loadCurrentTournaments` |
| 数据源 | `IBasketV4PlayerMyEventsService.listMyEvents()` |
| 映射 | `EventPlayer → EventTeam → Division → Season → Series → Stage/Group/Standing/Qualification/NextMatch` |
| 限制 | 概览最多 5 条；不再返回固定空列表 |

## 6. Legacy 赛程生成删除

| 项 | 结果 |
|----|------|
| 旧入口 | `POST /basket/v4/schedule-batch/generate` **已删除** |
| 唯一生成入口 | `POST /basket/v4/admin/stages/{stageId}/generate-schedule` |
| Admin UI | `cnpa/event-v4/schedule/index.vue` 仅阶段执行面板，无 Legacy Tab/按钮 |
| 联调脚本 | `legacyGenerateRemoved` 用例保留（期望 404） |

## 7. sys_job 清理

cleanup SQL §9：`ScheduleAutoGenerateJob`、`MatchStatusAutoFinishJob`、`BasketV4FormatSnapshotMigrationRunner` 对应 job 行 UPDATE status + DELETE。  
**运行时代码引用**：上述类名 **0**。

## 8. rank_explain_json 删除（全层完成）

| 层 | 状态 |
|----|------|
| Java `BasketStanding` | 属性已删除 |
| Mapper/XML | 映射已删除 |
| 前端 | 引用已删除 |
| cleanup SQL §6 | `ALTER TABLE basket_standing DROP COLUMN rank_explain_json` |
| data-check | `check_09_rank_explain_json_column = 0`（列不存在为通过） |
| `run_data_check.py` | 默认执行 R5 data-check；含 information_schema 列存在性检查 |
| 运行时 | 排名解释仅 `basket_standing_rank_reason`；`BasketStandingRankReason` 注释提及旧字段名（允许） |

## 9. V4_LEGACY_ROUND_ROBIN_V1 删除

| 项 | 结果 |
|----|------|
| cleanup SQL §11 | DELETE `basket_format_template*` 中 `template_code='V4_LEGACY_ROUND_ROBIN_V1'` |
| final-seed | 不含该模板 |
| data-check | `check_17_v4_legacy_template = 0` |

## 10. 最终保留的数据库表（38 张核心业务表）

`basket_series`、`basket_season`、`basket_division`、`basket_format_template*`、`basket_division_format_snapshot`、`basket_division_stage*`、`basket_registration*`、`basket_personal_*`、`basket_event_team`、`basket_event_player`、`basket_division_grouping_*`、`basket_division_stage_team`、`basket_division_stage_qualification`、`basket_schedule_batch`、`basket_match`、`basket_match_submission*`、`basket_standing`、`basket_standing_rank_reason`、`basket_team_stat`、`basket_player_stat`、`basket_rank`、`basket_captain_grant`、`basket_captain_grant_log`

**内容运营保留**：`basket_honor_*`、`basket_discipline_*`、`basket_announcement` 等（关联已改为 series/season）

## 11. 旧 Tournament 运行时引用数量

**2026-08-05 静态扫描结论：旧 Tournament 域运行时代码引用 = 0**

| 关键词 | Java/XML/Vue/TS 运行时 |
|--------|------------------------|
| `basket_tournament` | **0** |
| `basket_tournament_season` | **0** |
| `basket_tournament_admin_scope` | **0** |
| `BasketTournamentController` | **0** |
| `BasketPublicTournamentController` | **0** |
| `BasketTournamentMapper` / `BasketTournamentService` | **0** |
| `tournament.ts` | **0**（文件已删） |

**允许保留位置**：cleanup SQL 的 DROP/DELETE、本总结删除记录、Git 历史、`final-schema` 注释。

**兼容字段（非旧域）**：部分 DTO/表列仍命名 `tournamentId`，值映射 `seriesId`（公告、荣誉、公开 API）。

**已知残留（非 Tournament 表 JOIN）**：

| 项 | 说明 | 级别 |
|----|------|------|
| `HallOfFameDisplayPanel.vue` 调用 `listTournament` | API 已删，页面需本地修复 | P1 |
| `basketball-web` 孤儿 `BasketStageStandingDao/Mapper.xml` | 引用已删表 `basket_stage_standing`，无 Service 注入 | P2 |
| `WebHeader.vue` 路由判断 `/tournament/legacy` | 历史路由兼容判断 | P2 |
| `BasketMatchMapper.xml` 别名 `tournamentName` | 实为 `series_name` 显示别名，非旧表 JOIN | 文档说明 |

**已修复（相对 R5 初版）**：

- `BasketDashboardAdminMapper.xml` 待办统计已改用 `basket_registration`（不再 JOIN `basket_tournament_registration`）

## 12. 静态零引用检查（扩展关键词）

| 关键词 | 运行时结果 |
|--------|-----------|
| `basket_captain_qualification` | **0** ✅ |
| `basket_schedule_generate_batch` | **0** ✅ |
| `ScheduleAutoGenerateJob` | **0** ✅ |
| `MatchStatusAutoFinishJob` | **0** ✅ |
| `BasketV4FormatSnapshotMigrationRunner` | **0** ✅ |
| `legacyGenerateAllowed` | **0** ✅ |
| `V4_LEGACY_ROUND_ROBIN_V1` | **0** ✅（运行时；cleanup/data-check 允许） |
| `/schedule-batch/generate` | 仅 `scripts/cnpa_v2_full_integration_runner.py` 404 探测 ✅ |

## 13. 最终文档与 SQL

| 文件 | 用途 |
|------|------|
| `docs/cnpa-v2-final-business-flow.md` | **唯一**业务流程文档 |
| `docs/sql/cnpa-v2-final-schema.sql` | 新环境最终 Schema |
| `docs/sql/cnpa-v2-final-seed.sql` | 最终 Seed（LEAGUE/ARENA/CUP/OPEN） |
| `docs/sql/cnpa-v2-final-permission.sql` | 最终权限 |
| `docs/sql/cnpa-dynamic-format-r5-precheck.sql` | 已有库清理前检查 |
| `docs/sql/cnpa-dynamic-format-r5-cleanup.sql` | 已有库清理执行 |
| `docs/sql/cnpa-dynamic-format-r5-permission-cleanup.sql` | 旧权限清理 |
| `docs/sql/cnpa-dynamic-format-r5-data-check.sql` | 清理后验收 |
| `scripts/cnpa_v2_full_integration_runner.py` | 联调脚本 |
| `scripts/run_data_check.py` | data-check 执行器 |

## 14. P0 / P1 / P2（真实未完成项）

### P0

- 本地尚未执行 Maven 全量编译与服务启动 E2E（环境限制，非文档阻塞项）
- A-D 场景、权限越权、并发/幂等需用户本地统一验证
- `run_data_check.py` / 联调脚本需连真实开发库执行并归档结果

### P1

- `HallOfFameDisplayPanel.vue` 仍调用已删除的 `listTournament`（Admin 名人堂展示页）
- Maven 编译可能暴露 web 模块孤儿 `BasketStageStanding*` 文件（引用已删表）

### P2

- 公告表 `basket_announcement.tournament_id` 字段名保留，语义已映射 `series_id`
- Web 公开 API `PublicHonorItemResp.tournamentId` 等兼容字段保留，值为 `seriesId`
- `BasketMatchMapper.xml` 结果列别名 `tournamentName`（实为 series 名称）

## 15. 最终结论

**V2 最终版本代码与 `docs/cnpa-v2-final-business-flow.md` 已完成收口对齐。旧 Tournament 写链路与只读 Controller 已从运行时代码删除；cleanup SQL 已包含 tournament 三表 DROP 与 `V4_LEGACY_ROUND_ROBIN_V1` 清理。**

**数据库状态**：

- 新环境：仅执行 `final-schema` + `final-seed` + `final-permission`
- 已有开发库：若 R5 cleanup 已执行且 data-check 通过，**无需重复** cleanup

**下一步**：用户本地 `mvn compile` → 启动 Admin/Web → 执行 §30 验收清单 → 仅修 Bug，不再大规模业务重构。

## 16. 本地统一测试清单（摘要）

1. 账号注册登录
2. 球员认证
3. TEAM 报名全链路
4. PERSONAL 报名 + 线下组队 + 成队
5. ROUND_ROBIN / GROUP_STAGE / SINGLE_ELIMINATION 赛程生成
6. 比赛 Submission 双端提交 + 审核
7. Standing 重建 + MANUAL 排名 + 晋级预览/确认/撤销
8. 账号中心「当前参赛赛事」「我的赛程」
9. Admin 运营工作台全流程
10. 旧接口 404（含 `/schedule-batch/generate`）
11. 并发报名名额 / 幂等成队 / 重复晋级确认
12. 权限越权探测

完整 30 项见 `docs/cnpa-v2-final-business-flow.md` 第 30 章。
