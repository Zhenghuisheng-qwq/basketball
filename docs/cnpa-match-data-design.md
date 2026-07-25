# CNPA 比赛数据目标模型（第五批）

> **日期**：2026-07-25  
> **前置文档**：`docs/cnpa-match-current.md`  
> **迁移脚本**：`docs/sql/cnpa-batch5-migration.sql`

---

## 1. 设计目标

1. **正式结果与录入分离**：录入层（比分/统计）可反复修改；**仅** `basket_match_official_result.is_current=1` 的行参与积分榜与赛季统计。
2. **可审计**：每次提交/审核/驳回/重开/修订均有结构化记录，不再仅依赖 `basket_match_status_log` 文本。
3. **可修订**：争议或纠错通过 supersede 旧正式结果 + 升版本，触发赛季级 rebuild。
4. **名单快照**：出场名单绑定 `season_roster_id`，展示字段取自 roster 快照列，与第四批正式阵容一致。

---

## 2. 表结构变更概览

### 2.1 扩展 `basket_match`

| 新列 | 类型 | 含义 |
|------|------|------|
| `result_status` | tinyint | 0无 1正常 2弃赛 3管理员裁定 4重赛 5作废 |
| `score_confirmed` | tinyint | 比分已确认标记 |
| `data_version` | int | 当前数据版本号（CAS 用） |
| `origin_match_id` | bigint | 重赛关联原比赛 |
| `settlement_locked` | tinyint | 结算锁定（修订进行中） |

索引：`idx_match_data_status (season_id, data_status, match_status)`

### 2.2 扩展 `basket_match_roster`

| 新列 | 含义 |
|------|------|
| `season_roster_id` | 正式赛事阵容主键 |
| `event_team_id` | 赛事球队主键（`team_season_profile.id`） |
| `player_nickname_snapshot` | 球员昵称快照 |
| `platform_snapshot` | 平台快照 |
| `game_id_display_snapshot` | 游戏 ID 展示快照 |
| `roster_status` | 0草稿 1已锁定 |
| `locked_time` | 锁定时间 |
| `data_version` | 名单数据版本 |

### 2.3 新建表

| 表名 | 职责 |
|------|------|
| `basket_match_official_result` | 正式比赛结果（比分/胜负/版本） |
| `basket_match_data_audit` | 比分/统计/结果审核记录 |
| `basket_match_data_dispute` | 数据争议 |
| `basket_match_data_revision` | 数据修订（版本跃迁） |
| `basket_data_settlement_batch` | 结算批次追踪 |
| `basket_stat_sync_record` | 统计同步幂等 |

---

## 3. 状态机

### 3.1 比赛执行状态（不变）

沿用 `MatchStatusEnum`：DRAFT → SCHEDULED → RUNNING → FINISHED / POSTPONED / CANCELLED。

### 3.2 数据状态（扩展语义）

保留现有 `MatchDataStatusEnum` code，**审核驳回不新增 data_status**，而通过 `basket_match_data_audit.audit_status=REJECTED` 表达：

```
                    ┌─────────────┐
                    │   DRAFT(0)  │ 录入中
                    └──────┬──────┘
                           │ submitScore / submitStats
                           ▼
                    ┌─────────────┐
         ┌─────────│ SUBMITTED(1)│─────────┐
         │         └──────┬──────┘         │
         │ audit reject   │ audit approve │ reopen (from CONFIRMED)
         │ (audit 表)     ▼               │
         │         ┌─────────────┐         │
         └────────►│ 仍 SUBMITTED│         │
                   │ 可继续修改   │         │
                   └──────┬──────┘         │
                          │ confirmResult  │
                          ▼                │
                   ┌─────────────┐         │
                   │ CONFIRMED(2)│◄────────┘
                   │ + official  │
                   │   result    │
                   └──────┬──────┘
                          │ reopenResult / revision
                          ▼
                   ┌─────────────┐
                   │ REOPENED(3) │
                   └─────────────┘
```

| 阶段 | `basket_match.data_status` | 正式结果 | 可编辑 |
|------|---------------------------|----------|--------|
| 草稿 | 0 | 无 | 比分/统计/名单 |
| 已提交待审 | 1 | 无 | 比分/统计（审核中可选锁） |
| 审核驳回 | 1 | 无 | 修改后重新 submit audit |
| 已确认 | 2 | `official_result.is_current=1` | 否（须 reopen 或 revision） |
| 已重开 | 3 | 旧结果 `is_current=0` | 是 |

### 3.3 审核状态 `basket_match_data_audit.audit_status`

| 值 | 含义 |
|----|------|
| `PENDING` | 待审核 |
| `APPROVED` | 已通过 |
| `REJECTED` | 已驳回 |

`audit_type`：`SCORE` | `STAT` | `RESULT`（可组合 RESULT 表示整场比赛终审）

### 3.4 争议与修订

**争议** `basket_match_data_dispute.dispute_status`：

`PENDING` → `PROCESSING` → `RESOLVED` / `REJECTED`

**修订** `basket_match_data_revision.revision_status`：

`PENDING` → `APPROVED` → `EFFECTIVE` / `CANCELLED`

修订生效时：

1. 旧 `official_result.is_current` → 0
2. 插入新 `official_result`（`revision_no+1`, `data_version+1`）
3. 更新 `basket_match.data_version`
4. 写 `basket_match_data_revision` 关联 `from/to_official_result_id`
5. 触发 `doRebuild(seasonId)` + `basket_stat_sync_record` 幂等

---

## 4. 正式结果表 `basket_match_official_result`

每场比赛**至多一条** `is_current=1`（`uk_match_official_current`）。

| 字段 | 说明 |
|------|------|
| `match_id` | 比赛主键 |
| `data_version` | 与 `basket_match.data_version` 对齐 |
| `home_score` / `away_score` | 正式比分 |
| `winner_team_id` / `loser_team_id` | 胜/负方 |
| `is_draw` | 平局标记 |
| `result_type` | `NORMAL` / `FORFEIT` / `ADMIN` / `VOID` |
| `effective_time` | 生效时间 |
| `confirmed_by` / `confirmed_time` | 审核人 |
| `is_current` | 是否当前生效 |
| `is_public` | 是否对外公开 |
| `revision_no` | 修订序号（首版=1） |

**写入时机**：

- `confirmResult` 成功：INSERT 首条 official_result + 更新 match
- 修订生效：supersede + INSERT 新版
- **历史迁移**：17 条已确认比赛 INSERT（见 migration §migrate existing confirmed matches）

---

## 5. 审核记录设计

### 5.1 提交流

操作员完成录入后调用 `POST .../audit/submit`，创建 `basket_match_data_audit`：

- `audit_type=RESULT`
- `audit_status=PENDING`
- `data_version` = 当前 match 版本
- `submitter_id` / `submit_time`

### 5.2 审核通过

`POST .../audit/approve`：

1. CAS 更新 audit → `APPROVED`
2. 调用原 `confirmResult` 核心逻辑（或内聚为 `MatchResultConfirmationService`）
3. INSERT `official_result`
4. `match.data_status → CONFIRMED`, `score_confirmed=1`, `result_status=1`
5. 同步确认 stat 行
6. 同事务 `rebuildMatch` + 写 `stat_sync_record`

### 5.3 审核驳回

`POST .../audit/reject`：

- audit → `REJECTED`，写 `reject_reason`
- **不**改 `match.data_status`（保持 SUBMITTED 或 REOPENED）
- 通知提交人

### 5.4 与 status log 关系

`basket_match_status_log` **保留**，作为人类可读时间线；结构化审计以 `basket_match_data_audit` + `basket_stat_change_record` 为准。

---

## 6. 版本设计

| 对象 | 版本字段 | 递增时机 |
|------|----------|----------|
| `basket_match` | `data_version` | 每次 reopen / revision 生效 |
| `basket_match_roster` | `data_version` | 名单锁定或 reopen 后修改 |
| `basket_match_official_result` | `data_version` + `revision_no` | 每次新正式结果 |
| `basket_match_data_audit` | `data_version` | 绑定提交时的 match 版本 |
| `basket_match_data_revision` | `from_version` / `to_version` | 修订跨度 |

**CAS 示例**（确认结果）：

```sql
UPDATE basket_match
SET data_status = 2, data_version = data_version + 1, score_confirmed = 1
WHERE id = ? AND data_status = 1 AND data_version = ?
```

---

## 7. 出场名单：从 `season_roster` 快照

### 7.1 数据源切换

| 现状 | 目标 |
|------|------|
| `selectAvailablePlayers` → `registration_player` | → `season_roster WHERE roster_status=ACTIVE AND team 匹配 event_team` |
| 保存校验 `registration_player_id` | → 校验 `season_roster_id` + 复制快照列 |

### 7.2 写入 `basket_match_roster`

```text
season_roster_id          ← basket_tournament_season_roster.id
event_team_id             ← basket_team_season_profile.id
player_id                 ← season_roster.player_id
player_nickname_snapshot  ← season_roster.player_nickname_snapshot
platform_snapshot         ← season_roster.platform_snapshot
game_id_display_snapshot  ← season_roster.game_id_display_snapshot
registration_player_id    ← 保留（兼容/追溯），可为 NULL
```

### 7.3 锁定

- 比赛开始前：`roster_status=0` 可编辑
- `confirmLineup` 或开赛前自动：`roster_status=1`, `locked_time=now()`
- 已锁定名单 reopen 后 `data_version+1`，旧行 soft-delete 或标记 superseded

### 7.4 历史数据

387 条现有 `basket_match_roster` 仅含 `registration_player_id`；迁移脚本**不**强制回填 `season_roster_id`，新录入走新链路，旧数据只读展示。

---

## 8. 异常比赛 `result_status`

| code | 场景 | 正式结果 |
|------|------|----------|
| 1 | 正常结束 | NORMAL |
| 2 | 弃赛/判负 | FORFEIT |
| 3 | 管理员裁定比分 | ADMIN |
| 4 | 重赛 | 原场 VOID，新建 `origin_match_id` 关联的新比赛 |
| 5 | 作废 | VOID，不参与聚合 |

Admin 菜单「异常比赛处理」（menu_id=2232）承载 forfeit/void/rematch 操作。

---

## 9. 公开 API 读路径变更

`BasketPublicMatchServiceImpl` 赛果与技术统计判定改为：

```text
statsAvailable =
  public_status = PUBLIC
  AND EXISTS official_result WHERE match_id = ? AND is_current = 1 AND is_public = 1
```

比分展示优先读 `official_result.home_score/away_score`，fallback 到 `basket_match`（迁移过渡期）。

---

## 10. ER 关系（核心）

```text
basket_match (1) ──< (N) basket_match_official_result [is_current 唯一]
basket_match (1) ──< (N) basket_match_data_audit
basket_match (1) ──< (N) basket_match_data_dispute ──> basket_match_data_revision
basket_match (1) ──< (N) basket_match_roster ──> basket_tournament_season_roster
basket_match_official_result (1) ──< (N) basket_stat_sync_record
basket_tournament_season (1) ──< (N) basket_data_settlement_batch
```

---

## 11. 与现有代码的映射

| 现状方法 | 第五批改造 |
|----------|-----------|
| `BasketMatchScoreServiceImpl.confirmScore` | 写 audit + `score_confirmed=1`，或合并进 RESULT 审核 |
| `BasketMatchStatServiceImpl.confirmResult` | 拆为 submit audit + approve → 写 official_result |
| `BasketMatchLineupServiceImpl` | 改读 `season_roster` |
| `BasketConfirmedMatchAdminMapper.confirmedWhere` | JOIN `official_result WHERE is_current=1` |
| `BasketPublicMatchServiceImpl` | 读 official_result |

---

## 12. 迁移与回滚

| 脚本 | 路径 |
|------|------|
| 迁移前快照 | `docs/sql/cnpa-batch5-before.sql` |
| 迁移 | `docs/sql/cnpa-batch5-migration.sql` |
| 回滚 | `docs/sql/cnpa-batch5-rollback.sql` |
| 执行 | `docs/sql/run_batch5_migration.py` |

迁移会自动为 17 条 `data_status=2 AND match_status=3` 的比赛生成首版 `official_result`。
