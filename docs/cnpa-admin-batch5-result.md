# CNPA Admin 第五批执行结果

> **执行日期**：2026-07-25  
> **范围**：比赛执行、数据审核、争议、异常比赛、统计结算闭环

---

## 1. 执行概况

| 项 | 结果 |
|----|------|
| 新增数据库表 | **6**（official_result、data_audit、dispute、revision、settlement_batch、stat_sync_record） |
| 扩展数据库表 | **2**（basket_match、basket_match_roster） |
| 第四批回填 | ✅ 17 正式赛事球队、131 正式阵容 |
| 历史正式结果迁移 | ✅ 17 条 basket_match_official_result |
| 新增 Admin 菜单 | **4**（2230–2233） |
| 新增/修改代码 | **约 80+** |
| 新增单元测试 | **2** |
| 数据库迁移 | ✅ `run_batch5_migration.py` 已执行 |

---

## 2. 最终比赛数据模型

```text
basket_match（赛程/比分/状态）
  → basket_match_roster（比赛名单快照，关联 season_roster）
  → basket_match_team_stat / basket_match_player_stat（录入草稿，data_status）
  → [提交 SUBMITTED] → [确认比分 score_confirmed] → [审核 confirmResult]
  → basket_match_official_result（正式结果，is_current=1）
  → basket_stat_sync_record（幂等键防重复同步）
  → doRebuild 重算 → basket_stage_standing / team_season_stat / player_season_stat / rank

basket_match_data_audit（审核记录，预留）
basket_match_data_dispute（争议）
basket_match_data_revision（修订，预留表）
basket_data_settlement_batch（结算批次，预留表）
basket_stat_change_record（变更/结算审计）
```

---

## 3. 核心修复与增强

### P0：confirmScore 不再是 stub

- 更新 `score_confirmed=1`（CAS 条件更新）
- 提交比分时重置 `score_confirmed=0`
- `confirmResult` 要求比分已确认

### 审核驳回

- 新增 `MatchDataStatusEnum.REJECTED(4)`
- `POST /basket/match/{id}/result/reject`
- 保留历史版本（不覆盖 stat_change_record）

### 正式结果与幂等

- 审核通过写入 `basket_match_official_result`
- `basket_stat_sync_record` 幂等键：`MATCH:{id}:OR:{resultId}:TYPE:MATCH_CONFIRM`
- 重复同步跳过 rebuild，CAS 保证单次 confirm

### 统计策略（Strategy B）

- 沿用 `BasketDataSettlementServiceImpl.doRebuild`
- 确认集：`match_status=3 AND data_status=2`
- 正式结果为唯一事实来源，赛季级重算

### 比赛名单

- `generateFromSeasonRoster` 从正式赛事阵容生成快照
- `confirmLineup` 锁定名单（roster_status=1）

### 争议处理（最小实现）

- 表 + CRUD + 列表/处理 API
- 仅已确认比赛可发起；同类型待处理唯一

---

## 4. 数据库变更

| 对象 | 操作 |
|------|------|
| basket_match | +result_status, score_confirmed, data_version, origin_match_id, settlement_locked |
| basket_match_roster | +season_roster_id, event_team_id, 球员快照, roster_status, locked_time |
| basket_match_official_result | **新建** |
| basket_match_data_audit | **新建** |
| basket_match_data_dispute | **新建** |
| basket_match_data_revision | **新建** |
| basket_data_settlement_batch | **新建** |
| basket_stat_sync_record | **新建** |
| sys_menu 2230–2233 | **新建** |

脚本路径：
- `docs/sql/cnpa-batch5-before.sql`
- `docs/sql/cnpa-batch5-migration.sql`
- `docs/sql/cnpa-batch5-rollback.sql`
- `docs/sql/run_batch4_backfill.py`（第四批前置回填）
- `docs/sql/run_batch5_migration.py`

---

## 5. 状态流转

| 维度 | 枚举 |
|------|------|
| 比赛执行 | DRAFT/SCHEDULED/RUNNING/FINISHED/POSTPONED/CANCELLED |
| 数据状态 | DRAFT(0)/SUBMITTED(1)/CONFIRMED(2)/REOPENED(3)/REJECTED(4) |
| 正式结果类型 | NORMAL/FORFEIT/ADMIN_DECISION/REPLAY_RESULT/VOID |
| 争议 | PENDING/UPHELD/REJECTED/... |
| 公开 | public_status=1 + data_status=CONFIRMED |

---

## 6. 数据对账（迁移后）

| 指标 | 数量 |
|------|------|
| 比赛总数 | 31 |
| 已确认比赛 | 17 |
| 正式结果记录 | 17 |
| 正式赛事球队 | 17 |
| 正式阵容 | 131 |
| 统计同步记录 | 0（新确认流写入） |

---

## 7. 构建与测试

| 项目 | 结果 |
|------|------|
| basketball-web-ui | ✅ `npm run build` |
| basketball-admin-ui | ✅ `npm run build:prod` |
| basketball-admin | ⚠️ 本机无 mvn，已新增 JUnit 测试未本地编译 |
| basketball-web | ⚠️ 本机无 mvn |
| MySQL 迁移 | ✅ |
| Redis | 沿用既有 `basket:public:*` 失效逻辑 |

---

## 8. 遗留问题（第六批）

- 完整处罚联动
- 争议成立 → 自动修订版本 + 重算（当前需手工重开/重审）
- 赛事级数据权限完整接入
- 结算批次 UI 与 settlement_locked 联动
- 排名规则中相互战绩等待确认项
- basket_match_data_revision 完整工作流

---

## 9. 数据库安全确认

```text
本批次没有删除历史比赛、正式结果、统计和结算数据，所有迁移、修订和重算均保留了版本、批次及回滚方案。
```

---

## 10. 第六批准备条件

| 模块 | 状态 |
|------|------|
| 正式比赛结果作为事实来源 | ✅ |
| 统计重算闭环 | ✅ |
| 比赛名单快照 | ✅（基础） |
| 争议/修订框架 | ⚠️ 表+API，流程待深化 |
| 处罚/审计/荣誉/名人堂 | ❌ 第六批 |

---

## 11. 文档索引

- `docs/cnpa-match-current.md`
- `docs/cnpa-match-data-design.md`
- `docs/cnpa-statistics-design.md`
- `docs/cnpa-admin-batch5-plan.md`
