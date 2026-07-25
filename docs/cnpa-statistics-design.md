# CNPA 统计聚合设计（第五批）

> **日期**：2026-07-25  
> **策略**：**Strategy B — 从已确认正式结果全量重建**  
> **核心实现**：`BasketDataSettlementServiceImpl.doRebuild`  
> **前置**：`docs/cnpa-match-data-design.md`（正式结果表）

---

## 1. 策略选型

| 策略 | 描述 | 本批次 |
|------|------|--------|
| A 增量 | 每场 confirm 后 +1 更新 season stat | ❌ 修订/重开回滚复杂 |
| **B 全量重建** | 以赛季为单位 soft-delete + 从确认集重算 | ✅ **采用** |
| C 物化视图 | DB 层 MV 自动刷新 | ❌ 运维成本高 |

**理由**：已有 `doRebuild` 成熟链路；修订只需 supersede 正式结果后触发一次 season rebuild；幂等键防重复同步。

---

## 2. 确认集定义（改造后）

### 2.1 现状（第五批前）

`BasketConfirmedMatchAdminMapper.xml`：

```sql
m.match_status = 3          -- FINISHED
AND m.data_status = 2       -- CONFIRMED
AND m.home_team_id IS NOT NULL AND m.away_team_id IS NOT NULL
AND m.home_score >= 0 AND m.away_score >= 0
```

### 2.2 目标（第五批后）

```sql
m.match_status = 3
AND m.data_status = 2
AND EXISTS (
  SELECT 1 FROM basket_match_official_result r
  WHERE r.match_id = m.id
    AND r.is_current = 1
    AND r.is_delete = 0
    AND r.result_type NOT IN ('VOID')
)
-- 比分/胜负以 r.home_score, r.away_score, r.winner_team_id 为准
```

**弃赛/管理员裁定**：`result_type IN ('FORFEIT','ADMIN')` 仍纳入聚合，积分规则由 `StandingPointsRule` 处理。

**作废/重赛原场**：`result_type='VOID'` 或 `result_status=5` **排除**。

---

## 3. `doRebuild` 流程详解

文件：`basketball-admin/src/main/java/com/zhs/basket/service/impl/BasketDataSettlementServiceImpl.java`

### 3.1 触发入口

| 方法 | 触发类型 `SettlementTriggerTypeEnum` | 典型场景 |
|------|--------------------------------------|----------|
| `rebuildSeason` | `MANUAL_SEASON` | Admin 结算页手工重算 |
| `rebuildStage` | `MANUAL_STAGE` | 阶段级重算 |
| `rebuildGroup` | `MANUAL_GROUP` | 小组级重算 |
| `rebuildMatch` | `MATCH_CONFIRM` / `MATCH_REOPEN` / `MANUAL_MATCH` | confirmResult / reopenResult / 单场 |

### 3.2 执行步骤

```text
1. validateSeason(seasonId)
   └─ 自动触发(MATCH_CONFIRM/REOPEN)：仅「积分规则缺失」可放行

2. settlementLockHelper.tryLock(seasonId)
   └─ Redis: basket:data:settlement:season:{seasonId}  TTL 30min

3. confirmedMatchAdminMapper.countBySeasonAndStatuses(...)
   └─ 统计确认比赛数 → result.matchCount

4. standingService.rebuildSeasonStandings / rebuildStage / rebuildGroup
   └─ softDeleteByScope → 从确认集算积分 → batchInsert basket_stage_standing
   └─ 依赖 StandingPointsRule（basket_tournament_rule）

5. teamSeasonStatService.rebuildSeason(seasonId)
   └─ confirmedMatchAdminMapper.selectTeamStatAgg
   └─ softDelete + batchInsert basket_team_season_stat

6. playerSeasonStatService.rebuildSeason(seasonId)
   └─ selectPlayerStatAgg → basket_player_season_stat

7. rankingService.rebuildSeasonRankings(seasonId, snapshotDate)
   └─ basket_team_rank / basket_player_rank（按日快照）

8. writeAudit → basket_stat_change_record (biz_type=DATA_SETTLEMENT)

9. clearPublicCache → DELETE basket:public:*

10. settlementLockHelper.unlock(seasonId, token)
```

### 3.3 事务边界

- `confirmResult` / `reopenResult`：**同一事务**内调用 `rebuildMatch`；rebuild 失败则确认/重开整体回滚。
- 手工 `rebuildSeason`：独立事务。

### 3.4 并发控制

| 机制 | 实现 |
|------|------|
| 赛季级互斥 | `SettlementLockHelper` Redis 锁 |
| 重复 rebuild | 第五批新增 `basket_stat_sync_record` 幂等键 |
| 修订进行中 | `basket_match.settlement_locked=1` 阻止并行 confirm |

---

## 4. 下游聚合模块

### 4.1 积分榜 `BasketStandingServiceImpl`

- 输入：`List<ConfirmedMatchBriefVo>` + `StandingPointsRule`
- 算法：按 group/stage 分组 → 胜/负/平积分 → 净胜分 → 相互战绩 → `StandingTieBreakEnum`
- 输出：`basket_stage_standing`（含 `standing_points`, `rank_position`, `snapshot_time`）

### 4.2 球队赛季统计 `BasketTeamSeasonStatServiceImpl`

- SQL 聚合：`games, wins, losses, pointsFor, pointsAgainst` + 投篮等从 `basket_match_team_stat`（仅确认比赛）
- 输出：赛季级 + 各 stage 级行（`stage_id=0` 表示赛季汇总）

### 4.3 球员赛季统计 `BasketPlayerSeasonStatServiceImpl`

- 从 `basket_match_player_stat` 聚合确认比赛
- 场均：`StatMathUtils.avg(total, games)`

### 4.4 排行榜 `BasketRankingServiceImpl`

- 按 `snapshotDate`（当日 00:00）写入 rank 表
- 球队/球员榜分维度排序（积分、篮板等）

---

## 5. 幂等设计 `basket_stat_sync_record`

### 5.1 目的

防止同一正式结果重复触发 rebuild 时重复写 season 表或重复通知。

### 5.2 幂等键格式

```text
{idempotency_key} = {sync_type}:{official_result_id}:{data_version}

示例：
  MATCH_CONFIRM:10001:3
  REVISION:10001:4
  MANUAL_SEASON:20260725:{seasonId}
```

### 5.3 写入时机

在 `doRebuild` **成功完成后** INSERT：

| 字段 | 值 |
|------|-----|
| `match_id` | 触发比赛（season 级可为 NULL） |
| `official_result_id` | 当前生效正式结果 |
| `sync_type` | `MATCH_CONFIRM` / `REVISION` / `MANUAL_SEASON` 等 |
| `sync_status` | `SUCCESS` / `FAILED` |
| `idempotency_key` | 唯一 |

唯一约束：`uk_stat_sync_idempotency (idempotency_key)`

### 5.4 失败处理

- INSERT 冲突 → 跳过 rebuild（已同步）
- rebuild 异常 → 写 `sync_status=FAILED` + `error_message`，**不**写幂等 SUCCESS 键

---

## 6. 修订场景下的统计一致性

### 6.1 修订流程

```text
1. 管理员创建 revision（PENDING）
2. 修改比分/统计（data_status=REOPENED 或专用修订态）
3. revision APPROVED → EFFECTIVE：
   a. old official_result.is_current = 0
   b. INSERT new official_result (revision_no+1)
   c. match.data_version++
   d. doRebuild(seasonId) — 全量重算
   e. stat_sync_record REVISION:{newOfficialResultId}:{version}
```

### 6.2 为何全量 rebuild 而非增量回滚

- 一场修订可能影响积分榜相互战绩、净胜分排序
- 球员榜场均需重算 games 分母
- soft-delete + 全量 insert 比增量回滚更不易出错
- 31 场比赛规模下性能可接受

### 6.3 重开 `reopenResult`

- `data_status CONFIRMED→REOPENED`
- 旧 official_result `is_current=0`（第五批新增）
- 立即 `rebuildMatch` → 确认集减 1 → season 统计/积分榜回退

---

## 7. 结算批次 `basket_data_settlement_batch`

手工「全赛季重算」时创建批次记录：

| 字段 | 说明 |
|------|------|
| `batch_no` | 唯一批次号 |
| `season_id` / `stage_id` | 范围 |
| `settlement_scope` | `SEASON` / `STAGE` / `GROUP` / `MATCH` |
| `batch_status` | `RUNNING` → `SUCCESS` / `FAILED` |
| `match_count` / `success_count` / `fail_count` | 统计 |
| `is_current` | 最近一次成功批次 |

用途：Admin 结算页展示历史、故障排查。

---

## 8. 审计与可追溯

| 事件 | 记录位置 |
|------|----------|
| 单场 stat 确认 | `basket_stat_change_record` biz_type=MATCH |
| 赛季 rebuild | `basket_stat_change_record` biz_type=DATA_SETTLEMENT |
| 同步幂等 | `basket_stat_sync_record` |
| 比赛状态 | `basket_match_status_log` |
| 审核 | `basket_match_data_audit` |

`SeasonSettlementSummaryVo`（结算摘要页）继续聚合：

- `confirmedMatchCount` / `reopenedMatchCount`
- `standingCount` / `teamSeasonStatCount` / `playerSeasonStatCount`
- `lastSettlementTime` ← 最新 DATA_SETTLEMENT audit

---

## 9. 公开 API 与缓存

### 9.1 缓存键

`BasketDataSettlementServiceImpl` rebuild 后清理 `basket:public:*`。

### 9.2 Web 读路径

| API | 数据源 |
|-----|--------|
| `/basket/public/season/{id}/result/page` | match + official_result |
| `/basket/public/match/{id}` | stats 仅 CONFIRMED + official current |
| 赛季统计公开 API | `basket_team_season_stat` 等 rebuild 产物 |

---

## 10. 校验 `IBasketDataValidationService`

`validateSeason` 检查项（手工 rebuild 必须全过）：

- 确认比赛主客队合法、比分非负
- 确认比赛是否在分组/阶段范围内
- 积分规则是否配置（`basket_tournament_rule`）
- **第五批新增**：每场确认比赛是否有 `is_current=1` 的 official_result
- **第五批新增**：是否存在 pending dispute 未处理

---

## 11. 第五批代码改动清单（统计域）

| 文件 | 改动 |
|------|------|
| `BasketConfirmedMatchAdminMapper.xml` | JOIN `official_result`，比分改读正式结果 |
| `BasketDataSettlementServiceImpl.java` | 写 `settlement_batch` + `stat_sync_record` |
| `BasketStandingServiceImpl.java` | 确认集 VO 增加 `officialResultId` |
| `BasketTeamSeasonStatServiceImpl.java` | 聚合 SQL 对齐新确认集 |
| `BasketPlayerSeasonStatServiceImpl.java` | 同上 |
| `BasketPublicSeasonStatServiceImpl.java` | 无逻辑变，读 rebuild 产物 |
| 新建 `BasketStatSyncRecordMapper` | 幂等 CRUD |
| 新建 `BasketDataSettlementBatchMapper` | 批次 CRUD |

---

## 12. 性能与规模假设

| 指标 | 当前 | 预期 |
|------|------|------|
| 赛季比赛数 | ≤50 | 单赛季 rebuild <3s |
| 确认比赛 | 17 | 全量 rebuild 毫秒级 SQL |
| 球员 season stat | 256 行 | 线性增长 |

若未来赛季 >500 场，可引入「仅受影响 stage/group 局部 rebuild」，本批次不实现。

---

## 13. 测试要点

1. confirm 17 场后 rebuild → standing/stat/rank 与迁移前一致
2. reopen 1 场 → 确认集 -1，积分榜/score 回退
3. revision supersede → 新版 official_result 生效，旧版不参与聚合
4. 并发两次 rebuildSeason → 第二次获锁失败或幂等跳过
5. official_result VOID → 该场不计入 games/wins
6. rebuild 后 `basket:public:*` 缓存失效，Web 读到新数据
