# CNPA 比赛与统计现状（第五批前置）

> **日期**：2026-07-25  
> **第四批前置**：✅ 17 条正式赛事球队（`basket_team_season_profile`）、131 条正式赛事阵容（`basket_tournament_season_roster`）已回填

---

## 1. 数据库快照（迁移前）

| 指标 | 数量 |
|------|------|
| 比赛总数 `basket_match` | **31** |
| 已确认 `data_status=2` | **17** |
| 已提交 `data_status=1` | 2 |
| 草稿 `data_status=0` | 12 |
| 已重开 `data_status=3` | 0 |
| 阶段积分榜 `basket_stage_standing` | 33 |
| 球队赛季统计 `basket_team_season_stat` | 32 |
| 球员赛季统计 `basket_player_season_stat` | 256 |
| 球队排行 `basket_team_rank` | 160 |
| 球员排行 `basket_player_rank` | 1152 |
| 出场名单 `basket_match_roster` | 387 |
| 单场球队统计 `basket_match_team_stat` | 34 |
| 单场球员统计 `basket_match_player_stat` | 259 |
| 统计变更记录 `basket_stat_change_record` | 60 |
| 正式赛事球队 | 17 |
| 正式赛事阵容 | 131 |

**基线 SQL**：`docs/sql/cnpa-batch5-before.sql`

---

## 2. 已有数据表

### 2.1 比赛执行域

| 表名 | 用途 | 关键状态/审计 |
|------|------|---------------|
| `basket_match` | 比赛主表 | `match_status`, `data_status`, `public_status` |
| `basket_match_team` | 比赛双方球队行 | `result_type`, `match_score` |
| `basket_match_period_score` | 分节比分 | — |
| `basket_match_roster` | 出场名单 | 关联 `registration_player_id` |
| `basket_match_player_stat` | 球员单场技术统计 | `data_status` |
| `basket_match_team_stat` | 球队单场技术统计 | `data_status` |
| `basket_match_status_log` | 比赛状态变更日志 | `change_type`, `change_reason` |

### 2.2 聚合统计域

| 表名 | 用途 | 写入方式 |
|------|------|----------|
| `basket_stage_standing` | 阶段/小组积分榜 | 重算 soft-delete + insert |
| `basket_team_season_stat` | 球队赛季统计 | 从已确认比赛聚合 |
| `basket_player_season_stat` | 球员赛季统计 | 从已确认比赛聚合 |
| `basket_team_rank` | 球队排行榜快照 | 按日快照 |
| `basket_player_rank` | 球员排行榜快照 | 按日快照 |
| `basket_stat_change_record` | 统计变更审计 | `biz_type=MATCH` / `DATA_SETTLEMENT` |

### 2.3 第五批之前缺失

- ❌ 正式比赛结果表（比分/胜负以 `basket_match` 字段为准）
- ❌ 比赛数据审核记录表（仅有 `basket_match_status_log` 文本日志）
- ❌ 数据争议表
- ❌ 数据修订表
- ❌ 统计同步幂等表
- ❌ 结算批次表

---

## 3. 状态枚举（现状）

### 3.1 比赛状态 `MatchStatusEnum`

| code | 含义 |
|------|------|
| 0 | 草稿 |
| 1 | 已编排 |
| 2 | 进行中 |
| 3 | 已结束 |
| 4 | 已延期 |
| 5 | 已取消 |

文件：`basketball-admin/src/main/java/com/zhs/basket/enums/MatchStatusEnum.java`

### 3.2 数据状态 `MatchDataStatusEnum`

| code | 含义 | 可编辑 | 可确认 |
|------|------|--------|--------|
| 0 | 草稿 | ✅ | ❌ |
| 1 | 已提交 | ✅ | ✅ |
| 2 | 已确认 | ❌ | ❌ |
| 3 | 已重开 | ✅ | ✅（需重新提交后） |

文件：`basketball-admin/src/main/java/com/zhs/basket/enums/MatchDataStatusEnum.java`

**缺口**：无 `REJECTED` 状态；驳回仅能通过重开或手工改数实现，无独立审核流。

---

## 4. basketball-admin 实现盘点

### 4.1 赛程编排

| 组件 | 路径 |
|------|------|
| Controller | `basketball-admin/.../BasketMatchScheduleController.java` |
| Service | `BasketMatchScheduleServiceImpl.java` |
| Admin UI | `basketball-admin-ui/src/views/cnpa/competition/schedule/index.vue` |
| API 封装 | `basketball-admin-ui/src/api/basket/matchSchedule.ts` |

能力：新增/编排/延期/开始/取消/删除；写 `basket_match_status_log`。

### 4.2 比分录入

| 组件 | 路径 |
|------|------|
| Controller | `BasketMatchScoreController.java` → `/basket/match/{id}/score/*` |
| Service | `BasketMatchScoreServiceImpl.java` |
| Admin UI | `basketball-admin-ui/src/views/cnpa/competition/score/index.vue` |
| API 封装 | `basketball-admin-ui/src/api/basket/matchScore.ts` |

| 方法 | 行为 |
|------|------|
| `savePeriodScores` | 写分节比分，汇总到 `basket_match.home_score/away_score` |
| `submitScore` | `match_status→FINISHED`, `data_status→SUBMITTED` |
| **`confirmScore`** | ⚠️ **Stub**：仅 `writeStatusLog(SCORE_CONFIRM)`，**不改** `data_status` |
| `reopenScore` | `data_status SUBMITTED→REOPENED`, `match_status FINISHED→RUNNING` |
| `forfeit` | 判负，默认 20-0，直接 SUBMITTED |

### 4.3 出场名单

| 组件 | 路径 |
|------|------|
| Controller | `BasketMatchLineupController.java` |
| Service | `BasketMatchLineupServiceImpl.java` |
| Admin UI | `basketball-admin-ui/src/views/cnpa/competition/lineup/index.vue` |
| API 封装 | `basketball-admin-ui/src/api/basket/matchLineup.ts` |

**关键事实**：可选球员来自 `BasketTournamentRegistrationPlayerMapper.selectAvailableBySeasonAndTeam`（**报名阵容**），保存时校验 `registration_player_id`。**尚未**关联 `basket_tournament_season_roster`。

### 4.4 技术统计与结果确认

| 组件 | 路径 |
|------|------|
| Controller | `BasketMatchStatController.java` |
| Service | `BasketMatchStatServiceImpl.java` |
| Admin UI | `basketball-admin-ui/src/views/cnpa/competition/match-data-entry/index.vue` |
| API 封装 | `basketball-admin-ui/src/api/basket/matchStat.ts` |

| 方法 | 行为 |
|------|------|
| `saveTeamStats` / `savePlayerStats` | soft-delete + batchInsert 单场统计 |
| `submitTeamStats` / `submitPlayerStats` | 校验一致性，写 status log |
| **`confirmResult`** | ✅ **完整流程**：校验比分与统计 → `data_status SUBMITTED→CONFIRMED` → 同步确认 stat 行 → 写 `stat_change_record` → **同事务** `dataSettlementService.rebuildMatch` |
| `reopenResult` | `CONFIRMED→REOPENED` + 同事务 `rebuildMatch` |
| `selectAuditLog` | 聚合 `basket_match_status_log` + `basket_stat_change_record` |

### 4.5 数据结算（聚合策略 B 雏形）

| 组件 | 路径 |
|------|------|
| Controller | `BasketDataSettlementController.java` → `/basket/data-settlement/*` |
| Service | `BasketDataSettlementServiceImpl.java` |
| 锁 | `SettlementLockHelper` → Redis key `basket:data:settlement:season:{seasonId}` |
| Admin UI | `basketball-admin-ui/src/views/cnpa/statistics/settlement/index.vue` |
| API 封装 | `basketball-admin-ui/src/api/basket/dataSettlement.ts` |

**`doRebuild` 流程**（`BasketDataSettlementServiceImpl.java:176`）：

1. `validateSeason`（自动触发时积分规则缺失可跳过积分榜）
2. Redis 分布式锁
3. 统计已确认比赛数（`match_status=3 AND data_status=2`）
4. `standingService.rebuild*` → `basket_stage_standing`
5. `teamSeasonStatService.rebuildSeason` → `basket_team_season_stat`
6. `playerSeasonStatService.rebuildSeason` → `basket_player_season_stat`
7. `rankingService.rebuildSeasonRankings` → rank 表
8. 写 `basket_stat_change_record`（`biz_type=DATA_SETTLEMENT`）
9. 清理公开缓存 `basket:public:*`

**已确认比赛数据源**：`BasketConfirmedMatchAdminMapper.xml` 的 `confirmedWhere`：

```sql
match_status = 3 AND data_status = 2
AND home/away 有效 AND home_score/away_score >= 0
```

### 4.6 积分榜 / 赛季统计 / 排行（只读 + 手工重算入口）

| 模块 | Controller | Admin UI |
|------|------------|----------|
| 积分榜 | `BasketStandingController.java` | `statistics/standings/index.vue` |
| 球队赛季统计 | `BasketTeamSeasonStatController.java` | `statistics/team-season/index.vue` |
| 球员赛季统计 | `BasketPlayerSeasonStatController.java` | `statistics/player-season/index.vue` |
| 排行榜 | `BasketRankingController.java` | `statistics/ranking/index.vue` |
| 赛季成绩 | — | `statistics/season-result/index.vue` |

---

## 5. basketball-web 公开 API

| 组件 | 路径 |
|------|------|
| Controller | `basketball-web/.../BasketPublicMatchController.java` |
| Service | `BasketPublicMatchServiceImpl.java` |
| Web UI API | `basketball-web-ui/src/api/publicMatch.ts` |
| 页面 | `events/components/MatchList.vue`, `MatchDetailModal.vue`, `StatsModal.vue` |

**过滤规则**（`BasketPublicMatchServiceImpl`）：

- 列表/详情：`public_status=PUBLIC` + 赛季公开校验
- 赛果分页：额外 `match_status=FINISHED` + **`data_status=CONFIRMED(2)`**
- 详情技术统计：`statsAvailable = FINISHED && CONFIRMED` 时才加载 `basket_match_team_stat` / `basket_match_player_stat`

赛季统计公开：`BasketPublicSeasonStatController` / `BasketPublicSeasonStatServiceImpl` / `publicSeasonStat.ts`

---

## 6. 关键缺口（第五批待解决）

| # | 问题 | 影响 |
|---|------|------|
| 1 | `confirmScore` 仅写日志，比分确认与结果确认脱节 | 运营误以为比分已锁定 |
| 2 | 无正式结果表，比分存在 `basket_match` 可变字段 | 修订无版本、无 supersede |
| 3 | 无审核/驳回/争议/修订表 | 无法追溯谁改了什么 |
| 4 | 出场名单仍读 `registration_player` | 与第四批 `season_roster` 快照不一致 |
| 5 | 聚合直接读 `basket_match.data_status=2` | 修订后需全量 rebuild，无幂等键 |
| 6 | `basket_match` 无 `data_version` | 并发修订无法 CAS |

---

## 7. 第四批回填状态（第五批依赖）

| 对象 | 迁移前 | 当前 |
|------|--------|------|
| `basket_team_season_profile` | 0 | **17** |
| `basket_tournament_season_roster` | 0 | **131** |
| 回填接口 | — | `POST /basket/registrationBackfill/backfill-approved` |

详见 `docs/cnpa-admin-batch4-result.md` §9。

---

## 8. 相关文件索引

```
basketball-admin/src/main/java/com/zhs/basket/
  service/impl/BasketMatchScoreServiceImpl.java      # confirmScore stub
  service/impl/BasketMatchStatServiceImpl.java       # confirmResult 完整
  service/impl/BasketDataSettlementServiceImpl.java  # doRebuild
  service/impl/BasketMatchLineupServiceImpl.java     # registration_player
  service/impl/BasketStandingServiceImpl.java
  service/impl/BasketTeamSeasonStatServiceImpl.java
  service/impl/BasketPlayerSeasonStatServiceImpl.java
  mapper/BasketConfirmedMatchAdminMapper.java
  support/SettlementLockHelper.java
  support/ConfirmedMatchHelper.java
  enums/MatchDataStatusEnum.java
  enums/MatchStatusEnum.java

basketball-admin/src/main/resources/mapper/basket/
  BasketConfirmedMatchAdminMapper.xml
  BasketMatchStatusLogMapper.xml

basketball-web/src/main/java/com/zhs/
  controller/publicapi/BasketPublicMatchController.java
  service/impl/BasketPublicMatchServiceImpl.java

basketball-admin-ui/src/views/cnpa/competition/
  schedule/index.vue | score/index.vue | lineup/index.vue | match-data-entry/index.vue
basketball-admin-ui/src/views/cnpa/statistics/
  settlement/index.vue | standings/index.vue | team-season/index.vue | player-season/index.vue
```
