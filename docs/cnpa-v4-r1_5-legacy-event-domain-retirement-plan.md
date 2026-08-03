# CNPA 赛事域 V4 第 1.5 轮：旧赛事域隔离与退场清单

> 日期：2026-08-03  
> 分支：`basketball-20260803`  
> 阶段：第 1.5 轮（隔离 / 清单 / Job 关闭建议 / 混用检查）  
> 状态：**已完成，未进入第 2 轮**  
> 依据轮次：`需求/赛事系列重构方案/重构初始文档/chatgpt共识版本.md` 第 2–8 轮

---

## 0. 本轮结论摘要

| 项 | 结论 |
|----|------|
| 本轮目的 | 防止 V4 与旧赛事域长期并存形成屎山；先隔离、列清单、定退场轮次 |
| V4 是否引用旧 Service/Mapper/Enum | **否**（见第 10 节 grep） |
| 仍会写旧表的 Job | **是（存在可写旧表的 Job 入口）**：`ScheduleAutoGenerateJob`、`MatchStatusAutoFinishJob` |
| Entity 同名 alias 冲突 | R1 补丁后 **Match 四件套已改 BasketV4***；当前无 Entity/Mapper/Dao 简单名碰撞 |
| Enum 同名风险 | **仍有**：`MatchStatusEnum` / `MatchPublishStatusEnum` / `RegistrationStatusEnum` / `SeasonStatusEnum`（旧包 vs `enums.v4`） |
| 本轮是否删旧代码 / 改页面 / 写业务 | **否** |
| 编译 | admin / web `mvn -DskipTests compile` => BUILD SUCCESS |

---

## 1. 旧赛事域 Controller 清单

### 1.1 Admin（`basketball-admin/.../controller/basket`）

| Controller | 基础路径 | 退场轮次 |
|------------|----------|----------|
| BasketCompetitionSeasonController | `/basket/competition-season` | 第 2 轮隐藏/替换 → 第 8 轮删除 |
| BasketTournamentController | `/basket/tournament` | 第 2 轮隐藏/替换 → 第 8 轮删除 |
| BasketSeasonController | `/basket/season*`（方法级映射） | 第 2 轮隐藏/替换 → 第 8 轮删除 |
| BasketTournamentRuleController | `/basket/tournamentRule*` | 第 2 轮隐藏/替换 → 第 8 轮删除 |
| BasketTournamentStageController | `/basket/stage` | 第 2 轮起弱化 → 第 4/8 轮收口 |
| BasketTournamentGroupController | 方法级 | 第 4 轮隐藏 → 第 8 轮删除 |
| BasketTournamentRoundController | 方法级 | 第 4 轮隐藏 → 第 8 轮删除 |
| BasketTournamentScopeController | `/basket/tournamentScope` | 第 8 轮删除（纪律周边可评估保留） |
| BasketCaptainQualificationController | `/basket/captainQualification` | 第 3 轮替换 → 第 8 轮删除 |
| BasketRegistrationAuditController | `/basket/registrationAudit` | 第 3 轮替换 → 第 8 轮删除 |
| BasketRegistrationBackfillController | `/basket/registrationBackfill` | 第 3 轮停用 → 第 8 轮删除 |
| BasketOpenRegistrationAuditController | `/basket/openRegistrationAudit` | 第 3 轮隐藏（Open/PERSONAL 非主线）→ 第 8 轮删除 |
| BasketSeasonRosterAdminController | `/basket/seasonRoster` | 第 3 轮替换 → 第 8 轮删除 |
| BasketEventTeamAdminController | `/basket/eventTeam` | 第 3 轮替换 → 第 8 轮删除 |
| BasketRosterChangeAuditController | `/basket/rosterChangeAudit` | 第 3/7 轮替换 → 第 8 轮删除 |
| BasketRosterOperationController | `/basket/rosterOperation` | 第 3/7 轮替换 → 第 8 轮删除 |
| BasketScheduleRuleController | `/basket/schedule/rule` | 第 4 轮替换 → 第 8 轮删除 |
| BasketScheduleManageController | `/basket/schedule/manage` | 第 4 轮替换 → 第 8 轮删除 |
| BasketMatchScheduleController | `/basket/match` | 第 4 轮替换 → 第 8 轮删除 |
| BasketMatchLineupController | `/basket/match` | 第 4/5 轮替换 → 第 8 轮删除 |
| BasketMatchScoreController | `/basket/match` | 第 5 轮替换 → 第 8 轮删除 |
| BasketMatchStatController | `/basket/match` | 第 5 轮替换 → 第 8 轮删除 |
| BasketMatchDataAuditController | `/basket/matchAudit` | 第 5 轮替换 → 第 8 轮删除 |
| BasketMatchDisputeController | `/basket/matchDispute` | 第 5 轮评估/替换 → 第 8 轮删除 |
| BasketMatchExceptionController | `/basket/matchException` | 第 5 轮评估/替换 → 第 8 轮删除 |
| BasketDataSettlementController | `/basket/data-settlement` | 第 5 轮替换 → 第 8 轮删除 |
| BasketStandingController | `/basket/standing` | 第 5 轮替换 → 第 8 轮删除 |
| BasketRankingController | `/basket/ranking` | 第 5 轮替换 → 第 8 轮删除 |
| BasketTeamSeasonStatController | `/basket/team-season-stat` | 第 5 轮替换 → 第 8 轮删除 |
| BasketPlayerSeasonStatController | `/basket/player-season-stat` | 第 5 轮替换 → 第 8 轮删除 |
| BasketSeasonResultController | `/basket/season-result` | 第 5/6 轮评估 → 第 8 轮删除 |
| BasketHonorGenerationController | `/basket/honor-generation` | 第 5/6 轮评估 → 第 8 轮删除 |
| BasketPlayerHonorController | `/basket/player-honor` | 第 6 轮评估 → 第 8 轮删除 |
| BasketTeamHonorController | `/basket/team-honor` | 第 6 轮评估 → 第 8 轮删除 |
| BasketHallOfFameController | `/basket/hall-of-fame` | 第 6 轮评估（可保留展示改造）→ 第 8 轮收口 |
| BasketLocalDemoDataController | `/basket/local-demo-data` | **第 2 轮起停用** → 第 8 轮删除 |

**明确保留（非旧赛事主链路，本清单仅标注不退场）：**  
球员认证 / 平台变更 / 球队审核 / 账号球员 / 纪律 / 公告 / Banner / Partner / 私有文件 / Dashboard / 永久档案 / 业务审计等。

### 1.2 Web（`basketball-web/.../controller`）

| Controller | 基础路径 | 退场轮次 |
|------------|----------|----------|
| BasketTournamentController | `/basket/tournament` | 第 6 轮替换 → 第 8 轮删除 |
| BasketTournamentSeasonController | `/basket/tournament/season` | 第 6 轮替换 → 第 8 轮删除 |
| BasketTournamentStage/Group/Round/Rule/AwardConfig | `/basket/tournament/*` | 第 6 轮替换 → 第 8 轮删除 |
| BasketTournamentRegistration* | `/basket/tournament/registration*` | 第 6/7 轮替换 → 第 8 轮删除 |
| TeamRegistrationBusinessController | 业务编排 | 第 6/7 轮替换 → 第 8 轮删除 |
| OpenRegistrationBusinessController | Open 报名 | 第 6 轮隐藏 → 第 8 轮删除 |
| AccountTournamentController | `/basket/account/tournament` | 第 7 轮替换 → 第 8 轮删除 |
| BasketCaptainQualificationController / CaptainQualificationController | `/basket/captain/qualification` 等 | 第 3/7 轮替换 → 第 8 轮删除 |
| BasketMatch* / BasketMatchRoster / StatusLog | `/basket/match*` | 第 6/7 轮替换 → 第 8 轮删除 |
| BasketPlayerMatchData/Dispute/Exception | 成员比赛数据 | 第 7 轮替换 → 第 8 轮删除 |
| BasketStageStanding / PlayerRank / TeamRank / *SeasonStat | 榜单统计 | 第 6 轮替换 → 第 8 轮删除 |
| BasketHonor* | `/basket/honor/*` | 第 6 轮评估 → 第 8 轮删除 |
| BasketPublicTournament/Match/Standing/SeasonStat | `/basket/public*` | 第 6 轮替换 → 第 8 轮删除 |
| EventParticipationQueryController | 参赛查询 | 第 7 轮替换 → 第 8 轮删除 |
| RosterChangeBusinessController | 阵容变更 | 第 7 轮替换 → 第 8 轮删除 |
| BasketTeamSeasonProfileController | `/basket/team/season/profile` | 第 3/7 轮评估 → 第 8 轮删除 |
| BasketVenueController | `/basket/venue` | 可选评估 → 第 8 轮 |

---

## 2. 旧赛事域 Service 清单

### 2.1 Admin Service（接口，路径前缀 `basketball-admin/.../basket/service/`）

**发布 / 结构：**  
`IBasketCompetitionSeasonService`、`IBasketTournamentService`、`IBasketSeasonService`、`IBasketTournamentRuleService`、`IBasketTournamentGroupService`、`IBasketTournamentScopeService`

**报名 / 授权 / 阵容：**  
`IBasketCaptainQualificationService`、`IBasketRegistrationAuditService`、`IBasketRegistrationBackfillService`、`IBasketOpenRegistrationAuditService`、`IBasketSeasonRosterAdminService`、`IBasketEventTeamAdminService`、`IBasketRosterChangeAuditService`、`IBasketRosterOperationService`、`IBasketParticipationGenerationService`

**赛程 / 比赛：**  
`IBasketScheduleRuleService`、`IBasketScheduleGenerateService`、`IBasketScheduleMatchAdminService`、`IBasketMatchAutoFinishService`、`IBasketMatchLineupService`、`IBasketMatchScoreService`、`IBasketMatchStatService`、`IBasketMatchValidationService`、`IBasketMatchOfficialResultService`、`IBasketMatchOfficialDataCorrectionService`、`IBasketMatchStatSyncService`

**提交 / 争议 / 异常 / 结算：**  
`IBasketMatchDataSubmissionApplyService`、`IBasketMatchDataSubmissionAuditService`、`IBasketMatchDataSubmissionValidationService`、`IBasketMatchDisputeService`、`IBasketMatchExceptionService`、`IBasketDataSettlementService`、`IBasketDataValidationService`、`IBasketStandingService`、`IBasketRankingService`、`IBasketTeamSeasonStatService`、`IBasketPlayerSeasonStatService`、`IBasketSeasonResultService`、`IBasketHonorGenerationService`、`IBasketHonorRecordService`

对应 `service/impl/*ServiceImpl` 同步退场；**第 8 轮统一删除**（各轮先隐藏入口/停 Job）。

### 2.2 Web Service

旧赛事相关主要位于：

- `basketball-web/.../service/impl/BasketTournament*`
- `Basket*Registration*` / `TeamRegistrationBusiness*`
- `Basket*Match*` / `BasketPlayerMatch*`
- `Basket*Standing*` / `Basket*Rank*` / `Basket*SeasonStat*`
- `BasketHonor*` / `public` 赛事查询服务
- `AccountTournament*` / `OpenRegistration*` / `RosterChange*`

退场轮次同对应 Controller（第 6/7 轮替换入口，第 8 轮删代码）。

---

## 3. 旧赛事域 Mapper / XML 清单

### 3.1 Admin Mapper（节选完整扫描结果，均在 `mapper/` 非 `mapper/v4`）

```text
BasketCaptainGrantLogMapper
BasketCaptainQualificationMapper
BasketCompetitionSeasonMapper
BasketHonorCategoryMapper / BasketHonorGenerationBatchMapper / BasketHonorLineupMapper / BasketHonorRecordMapper
BasketLocalDemoDataMapper
BasketMatchMapper / BasketMatchPeriodScoreMapper / BasketMatchTeamStatMapper / BasketMatchPlayerStatMapper
BasketMatchData* / BasketMatchException* / BasketMatchRosterMapper / BasketMatchStatusLogMapper
BasketPlayerRankAdminMapper / BasketPlayerSeasonStatAdminMapper
BasketPointAdjustmentMapper
BasketScheduleGenerateBatchMapper / BasketScheduleRule*Mapper
BasketStageStandingAdminMapper / BasketStatSyncRecordMapper
BasketTeamRankAdminMapper / BasketTeamSeasonStatAdminMapper
BasketTournament*Mapper（含 Registration / Roster / Stage / Group / Round / Rule / Season / Platform / AdminScope / OpenRegistration）
```

XML：`basketball-admin/src/main/resources/mapper/basket/*.xml`（不含 `mapper/basket/v4/`），与上表一一对应。

### 3.2 Web Dao（非 `dao/v4`）

```text
BasketCaptainQualificationDao
BasketCompetitionSeasonDao
BasketHonorCategoryDao / BasketHonorLineupDao / BasketHonorRecordDao
BasketMatch*Dao（含 DataSubmission / Dispute / Exception / Roster / StatusLog 等）
BasketPlayerRankDao / BasketPlayerSeasonStatDao
BasketStageStandingDao
BasketTeamRankDao / BasketTeamSeasonStatDao
BasketTournament*Dao（含 Registration / Roster / Stage / Group / Round / Rule / Season / Platform / AwardConfig / OpenRegistration）
BasketMatchDao / BasketMatchPeriodScoreDao / BasketMatchTeamStatDao / BasketMatchPlayerStatDao（旧）
```

退场：**第 8 轮删除**；第 2–7 轮仅停入口，不提前大批量删 Mapper（避免编译断裂）。

---

## 4. 旧赛事域 Entity / Enum 清单

### 4.1 Admin Domain（旧赛事相关，约 56 个）

见扫描：`BasketTournament*`、`BasketCompetitionSeason`、`BasketMatch*`、`BasketSchedule*`、`Basket*Standing*`、`Basket*Rank*`、`Basket*SeasonStat*`、`BasketHonor*`、`BasketCaptain*`、`BasketPointAdjustment`、`BasketStatSyncRecord`、`BasketVenue` 等。

V4 新实体在 `domain/v4`，同名冲突四件套已改为：

| 旧 Entity | V4 Entity |
|-----------|-----------|
| BasketMatch | BasketV4Match |
| BasketMatchPeriodScore | BasketV4MatchPeriodScore |
| BasketMatchTeamStat | BasketV4MatchTeamStat |
| BasketMatchPlayerStat | BasketV4MatchPlayerStat |

### 4.2 旧 Enum（赛事相关，Admin `enums/` 非 `enums.v4`）

重点：`TournamentStatusEnum`、`CompetitionSeasonStatusEnum`、`SeasonStatusEnum`、`RegistrationStatusEnum`、`MatchStatusEnum`、`MatchPublishStatusEnum`、`MatchData*Enum`、`Schedule*Enum`、`CaptainQualificationStatusEnum`、`OpenRegistrationStatusEnum`、`SettlementTriggerTypeEnum`、`HonorCodeEnum`、`PlayerRankTypeEnum` 等。

### 4.3 Enum 同名风险（新旧并存）

| 简单类名 | 旧包 | 新包 |
|----------|------|------|
| MatchStatusEnum | `com.zhs.basket.enums` | `com.zhs.basket.enums.v4` |
| MatchPublishStatusEnum | 同上 | `enums.v4` |
| RegistrationStatusEnum | 同上 | `enums.v4` |
| SeasonStatusEnum | 同上 | `enums.v4` |

**约束：** 新 V4 业务只允许 `import ...enums.v4.*`；禁止短名混用。  
（MyBatis `typeAliasesPackage` 当前不扫 enums，故非启动 alias 冲突，但是开发期混用高风险。）

---

## 5. 旧赛事域 Job 清单

| Job | Bean / invokeTarget | 行为 | 写入旧表？ | 关闭建议 | 退场轮次 |
|-----|---------------------|------|------------|----------|----------|
| `ScheduleAutoGenerateJob` | `scheduleAutoGenerateJob.runAutoGenerate()` | 报名截止后自动生成草稿赛程 | **会**（经 `IBasketScheduleGenerateService` 写旧赛程/比赛相关表） | **立即在 Quartz `sys_job` 停用/删除该任务**；代码已标 `@Deprecated`；第 4 轮验收要求「旧 ScheduleAutoGenerateJob 关闭」且不新建自动排赛 Job | 第 4 轮强制关闭 → 第 8 轮删类 |
| `MatchStatusAutoFinishJob` | `matchStatusAutoFinishJob.runAutoFinish()` | 到点自动结束比赛并初始化数据状态 | **会**（`BasketMatchMapper.autoFinishBySchedule` 更新旧 `basket_match`） | **建议立即停用**；若开发库仍跑旧联调可临时保留，但 V4 清库/`basket_match` 同名切换前必须停，否则污染新旧结构 | 第 4/5 轮前停用 → 第 8 轮删类 |

说明：

1. 仓库 SQL 种子 `ry_*.sql` 未预置上述两个 invokeTarget；是否在运行环境启用取决于 **DB `sys_job` 配置**，上线前必须人工核查。
2. 本轮未执行任何 DROP/TRUNCATE，也未改 Quartz 数据；仅代码注释 + 本清单给出关闭建议。

---

## 6. 旧后台菜单和页面清单

### 6.1 菜单脚本（部分）

`sql/cnpa-admin-menu.sql` 含：

- CNPA赛事管理（目录）
- 队长资格管理 / 赛事管理 / 赛季管理 / 赛事规则 / 报名审核  
（另含保留项：球员认证、球队审核）

实际运行菜单可能比该脚本更全（库内历史菜单 + `admin-ui` 运行时结构整理）。

### 6.2 Admin UI 页面（`basketball-admin-ui/src/views/cnpa/**`，旧赛事相关）

| 目录/页面 | 退场轮次 |
|-----------|----------|
| `event/list` `event/season` `event/rule` `event/stage` `event/group` | 第 2 轮隐藏/替换 |
| `identity/captain-qualification` | 第 3 轮替换 |
| `participation/team-registration` `open-registration` `event-roster` `roster-operations` `transfer-audit` `free-agent-audit` `captain-transfer` `official-teams` | 第 3/7 轮替换 |
| `competition/schedule*` `score` `lineup` `match-*` | 第 4/5 轮替换 |
| `statistics/settlement` `standings` `ranking` `team-season` `player-season` `season-result` | 第 5 轮替换 |
| `content/player-honor` `team-honor` `hall-of-fame` | 第 6 轮评估 |
| `discipline/tournament-scope` | 第 8 轮评估（纪律可保留改造） |

运行时菜单整理：`basketball-admin-ui/src/store/modules/permission.ts`（比赛执行菜单合并/隐藏逻辑）。

---

## 7. 旧前台路由和页面清单

### 7.1 路由（`basketball-web-ui/src/router/index.ts`）

| 路由 | 说明 | 退场轮次 |
|------|------|----------|
| `/tournament`、`/events`→`/tournament` | 赛事详情/赛程/榜单入口 | 第 6 轮替换 |
| `/data` | 数据中心（旧 tournament 选择） | 第 6 轮替换 |
| `/hall-of-fame` | 名人堂 | 第 6 轮评估 |
| `/tournament/team/:teamId`、`/tournament/team/compare` | 赛事球队 | 第 6 轮替换 |
| `/account/my-events*` | 我的赛事/比赛 | 第 7 轮替换 |
| `/account/my-events/:tournamentId/team-registration` | 球队报名（tournamentId） | 第 7 轮替换 |
| `/account/my-tournaments*` | 旧我的赛事/球队管理/比赛数据 | 第 7 轮替换 → 第 8 轮删除 |
| Open 相关 redirect（`events/open/*`、`2026-open/*`） | 个人报名入口 | 第 6/7 轮隐藏 → 第 8 轮删除 |

### 7.2 页面目录（旧赛事强相关）

```text
basketball-web-ui/src/views/events/**
basketball-web-ui/src/views/tournament/**
basketball-web-ui/src/views/data/**
basketball-web-ui/src/views/hall-of-fame/**
basketball-web-ui/src/views/account/events/**
basketball-web-ui/src/views/account/tournaments/**
basketball-web-ui/src/views/account/my-tournaments/**
basketball-web-ui/src/components/account/tournament/**
basketball-web-ui/src/components/account/team/CaptainGrantPanel.vue
```

本轮**未改**任何前台页面。

---

## 8. 旧接口路径清单（聚合）

### 8.1 Admin 旧赛事 API 前缀

```text
/basket/competition-season
/basket/tournament
/basket/season*
/basket/tournamentRule* /basket/stage /basket/tournamentScope
/basket/captainQualification
/basket/registrationAudit /basket/registrationBackfill /basket/openRegistrationAudit
/basket/seasonRoster /basket/eventTeam
/basket/rosterChangeAudit /basket/rosterOperation
/basket/schedule/rule /basket/schedule/manage
/basket/match* /basket/matchAudit /basket/matchDispute /basket/matchException
/basket/data-settlement /basket/standing /basket/ranking
/basket/team-season-stat /basket/player-season-stat /basket/season-result
/basket/honor-generation /basket/player-honor /basket/team-honor /basket/hall-of-fame
/basket/local-demo-data
```

### 8.2 Web 旧赛事 API 前缀

```text
/basket/tournament/**
/basket/tournament/registration**
/basket/account/tournament
/basket/captain/qualification /basket/team/captain/qualification
/basket/match/**
/basket/stage/standing /basket/player/rank /basket/team/rank
/basket/player/season/stat /basket/team/season/stat /basket/team/season/profile
/basket/honor/**
/basket/public（赛事/赛程/积分榜/赛季统计等公开查询）
/basket/venue
```

完整类→路径对照见扫描产物：`scripts/controller_paths.out.txt`。

---

## 9. 旧表清理清单与第 1 轮 cleanup SQL 对照

来源：`docs/sql/cnpa-v4-r1-cleanup-draft.sql`（仍全部注释，本轮未执行）。

| 分组 | cleanup 草案表 | 代码退场轮次建议 |
|------|----------------|------------------|
| 同名冲突最高优先 | `basket_match` / `period_score` / `team_stat` / `player_stat` | 执行 DDL 前必须清；对应旧 Match* 代码第 8 轮删 |
| 比赛数据/争议/异常/结算 | `basket_match_data_*`、`exception_*`、`official_result`、`status_log`、`roster`、`match_team`、`data_settlement_batch`、`stat_*`、`point_adjustment` | 第 5 轮停写 → 第 8 轮删代码+表已清 |
| 统计/榜单/荣誉 | `basket_*_rank`、`*_season_stat`、`stage_standing`、`honor_*` | 第 5/6 轮替换 → 第 8 轮 |
| 赛程规则/批次 | `basket_schedule_rule*`、`schedule_generate_batch` | 第 4 轮停 Job/替换 → 第 8 轮 |
| 报名/阵容/队长 | `basket_tournament_registration*`、`roster*`、`open_registration*`、`season_roster`、`team_season_profile`、`captain_*` | 第 3 轮替换 → 第 8 轮 |
| 结构容器 | `basket_tournament*`、`competition_season` | 第 2 轮停发布 → 第 8 轮 |
| 可选 | `basket_venue` | 评估后第 8 轮 |

**保留表（禁止清）：** 账号/球员认证/长期球队/文件/公告/Partner/纪律/消息/收藏/若依基础表 —— 与 cleanup 草案第 3 节一致。

---

## 10. 新 V4 代码是否引用旧赛事域代码

### 10.1 Grep / 扫描结果

| 检查 | 命令/方式 | 结果 |
|------|-----------|------|
| v4 包 import 旧 service/mapper/domain/enums | `scripts/scan_v4_r1_5_legacy.py` → `V4_BAD_IMPORTS` | **count = 0** |
| admin `mapper/v4` import | 仅 `com.zhs.basket.domain.v4.*` | 通过 |
| web `dao/v4` import | 仅 `com.zhs.model.v4.*` | 通过 |
| v4 XML `resultMap type` | 全限定 `com.zhs.basket.domain.v4.*` | 通过 |
| Entity/Mapper/Dao 简单名碰撞 | PowerShell `Compare-Object` 旧包 vs v4 包 | **无碰撞**（Match 四件套已 BasketV4*） |
| Enum 简单名碰撞 | 同上 | **4 个同名**（见 4.3） |

### 10.2 MyBatis alias / Bean 名

| 风险点 | 现状 | 处置 |
|--------|------|------|
| `BasketMatch` 等 Entity alias | R1 已改 `BasketV4Match*` | 已缓解 |
| `typeAliasesPackage` 含 `com.zhs.basket.domain` 会递归扫到 `domain.v4` | 仍递归扫描，但无同名 Entity | 可接受；v4 XML 不依赖短别名 |
| Mapper Bean：`BasketMatchMapper` vs `BasketV4MatchMapper` | 名称已区分 | 通过 |
| `BasketStandingMapper` 仅存在于 `mapper.v4` | 旧站用 `BasketStageStandingAdminMapper` 等 | 当前无 Bean 重名 |
| Enum 同名 | 包路径不同，需纪律约束 | 开发期风险，列入第 2 轮起强制 review |

**结论：未发现 V4 模型层引用旧赛事 Service/Mapper/Enum；存在 Enum 同名混用风险，不存在已确认的 Entity/Mapper Bean 启动级冲突。**

---

## 11. 按轮次的退场计划（隐藏 / 替换 / 删除）

### 第 2 轮 — 后台发布（隐藏/替换）

- 隐藏/替换：旧「赛事/赛季/竞赛季/规则」后台菜单与发布页  
- 停用：`BasketLocalDemoDataController` 及本地 Demo 灌数入口  
- 建议：核查并暂停会写旧表的 Job（至少 `ScheduleAutoGenerateJob`）  
- **不删除**旧 Service/Mapper

### 第 3 轮 — 授权 + 报名 + 审核 + 快照

- 隐藏/替换：队长资格、报名审核、Open 报名审核、赛季阵容、赛事球队管理旧页  
- 前台报名入口暂可并存，但新链路必须走 V4 Registration  
- 旧 backfill 工具停用

### 第 4 轮 — 赛程 + Match

- **强制关闭** `ScheduleAutoGenerateJob`  
- 建议同步关闭 `MatchStatusAutoFinishJob`（避免写旧 `basket_match`）  
- 隐藏/替换：赛程规则、赛程管理、比赛管理旧页  
- 不新建自动排赛 Job

### 第 5 轮 — 提交 + 审核 + 结算

- 隐藏/替换：比赛数据录入/审核/争议/异常、结算、积分榜、排行、赛季统计旧页与 API  
- 停止旧结算编排对旧表的重算入口（运营侧禁用菜单）

### 第 6/7 轮 — 前台替换

- **第 6 轮**：`/tournament`、`/data`、公开赛事/赛程/榜单 → V4 Series/Season/Division  
- **第 7 轮**：`/account/my-events*`、`/account/my-tournaments*`、比赛数据提交入口 → V4  
- 删除或重定向所有 `tournamentId` 混用路由

### 第 8 轮 — 最终删除

- 删除本清单全部旧 Controller / Service / Mapper / XML / Entity / Enum（赛事域）  
- 删除旧 Job 类与 Demo  
- 删除/隐藏全部旧赛事菜单  
- 确认 cleanup 表已清、V4 表在用  
- 全链路联调 + admin/web 前后端构建通过

---

## 12. 当前风险

1. **Job 仍可能写旧表**：`ScheduleAutoGenerateJob`、`MatchStatusAutoFinishJob` 代码仍在；若 `sys_job` 启用会继续写旧赛程/比赛数据。  
2. **同名表未清前执行 V4 DDL 会混结构**：`CREATE TABLE IF NOT EXISTS basket_match` 危险（R1 已警告）。  
3. **Enum 同名**：新旧 `MatchStatusEnum` 等易被错误 import。  
4. **菜单/页面仍全量暴露旧运营能力**：新旧并存窗口内，运营可能继续用旧后台造数。  
5. **前台仍以 tournamentId 为中心**：个人中心报名/赛事路由大量绑定旧模型。  
6. **荣誉/名人堂/纪律 scope** 与旧 tournament/season 耦合，第 8 轮前需单独评估是否改造而非简单删除。  
7. **本轮未连库核查 `sys_job` 实际启用状态**（MySQL MCP 不可用）；上线前必须人工查库。

---

## 13. 下一步建议

1. **立即（本轮后续运维动作，不写业务）**  
   - 在运行库检查 `sys_job`：`invoke_target like '%scheduleAutoGenerateJob%'` / `'%matchStatusAutoFinishJob%'`  
   - 存在则暂停（status=1）或删除任务  
2. **进入第 2 轮前**  
   - 人工确认 cleanup 白名单并备份  
   - 停旧发布菜单或加「已废弃」标识（可在第 2 轮做）  
3. **第 2 轮**  
   - 只做 V4 Season/Division 后台发布  
   - 禁止把旧 Service 接入新 V4 业务  
4. **持续纪律**  
   - Code review：`enums.v4` only；禁止 v4 包依赖旧 tournament/match service

---

## 14. 本轮已做的最小代码标注

| 文件 | 变更 |
|------|------|
| `basketball-admin/.../job/ScheduleAutoGenerateJob.java` | `@Deprecated` + 退场说明注释 |
| `basketball-admin/.../job/MatchStatusAutoFinishJob.java` | `@Deprecated` + 退场说明注释 |

未删除旧代码，未改前后台页面，未写 V4 业务，未执行 DROP/TRUNCATE。

---

## 15. 编译结果

```text
basketball-admin : mvn -DskipTests compile  => BUILD SUCCESS
basketball-web   : mvn -DskipTests compile  => BUILD SUCCESS
```

---

## 16. 一句话结论

```text
第 1.5 轮完成：旧赛事域 Controller/Service/Mapper/Job/菜单/前台路由退场清单已固化；
V4 模型层未引用旧赛事代码；存在可写旧表的 Job 入口（建议立即停用）；
Enum 同名需开发纪律约束；未进入第 2 轮。
```
