# CNPA 赛事域 V4 第 5 轮：比赛数据提交 + 审核 + 官方结果 + 统计结算

> 完成日期：2026-08-04  
> 范围：Web 比赛数据提交 API、Admin 审核 API、V4 统计结算、后台审核/榜单页、SQL 补丁、冒烟脚本  
> **不进入第 6 轮**

---

## 1. 本轮完成内容

| 模块 | 状态 | 说明 |
|------|------|------|
| 表结构补丁 | ✅ | `basket_match_submission` 补 `home_score` / `away_score` / `submit_remark` |
| Web 比赛数据提交 | ✅ | `/basket/v4/player/match-submission/*` |
| Admin 比赛数据审核 | ✅ | `/basket/v4/match-submission-audit/*` |
| V4 统计结算 Service | ✅ | `IBasketV4StatsSettlementService` |
| Admin 统计查询 | ✅ | `/basket/v4/stats/*` |
| 后台 V4 比赛数据审核页 | ✅ | `event-v4/match-submission-audit` |
| 后台 V4 统计榜单页 | ✅ | `event-v4/stats` |
| 菜单 SQL | ✅ | `cnpa-v4-round5-menu.sql` |
| 旧入口隐藏/废弃提示 | ✅ | SQL 隐藏 + 旧页面 banner |
| 冒烟脚本 | ✅ | `scripts/smoke_v4_r5_match_data_stats.py` |

**R5 P0 只做总比分 + 球队/球员统计，分节比分后置。** 未建 `basket_match_submission_period_score`。

---

## 2. 表结构补丁记录

文件：`docs/sql/cnpa-v4-round5-match-data-patch.sql`

```sql
ALTER TABLE basket_match_submission ADD COLUMN home_score INT ...
ALTER TABLE basket_match_submission ADD COLUMN away_score INT ...
ALTER TABLE basket_match_submission ADD COLUMN submit_remark VARCHAR(512) ...
```

执行脚本：`scripts/run_r5_match_data.py`（已在测试库执行成功）。

---

## 3. 新增 / 修改文件清单

### 后端 Admin

- `BasketEventV4Perms.java` — 新增审核/统计权限
- `SubmitterRoleEnum.java`
- DTO/Query/VO：`BasketMatchSubmissionRejectReq`, `BasketMatchSubmissionAuditQuery`, `BasketV4StatsQuery`, `BasketMatchSubmissionAuditListVo`, `BasketMatchSubmissionAuditDetailVo` 等
- `IBasketV4MatchSubmissionAuditService` + `BasketV4MatchSubmissionAuditServiceImpl`
- `IBasketV4StatsSettlementService` + `BasketV4StatsSettlementServiceImpl`
- `IBasketV4StatsQueryService` + `BasketV4StatsQueryServiceImpl`
- `BasketV4MatchSubmissionAuditController`, `BasketV4StatsController`
- Mapper 扩展：`BasketMatchSubmissionMapper`, `BasketV4MatchMapper`, `BasketV4MatchTeamStatMapper`, `BasketStandingMapper` 等 XML

### 后端 Web

- `BasketV4PlayerMatchSubmissionController`
- `IBasketV4PlayerMatchSubmissionService` + `BasketV4PlayerMatchSubmissionServiceImpl`
- `BasketV4PlayerMatchSubmissionMapper` + XML
- Request/Response：`V4MatchSubmissionSaveReq`, `V4MyMatchResp`, `V4MatchSubmissionDetailResp` 等
- 实体补丁：`BasketMatchSubmission` 增 `homeScore/awayScore/submitRemark`

### 前端

- `src/api/cnpa/event-v4/match-submission-audit.ts`
- `src/views/cnpa/event-v4/match-submission-audit/index.vue`
- `src/views/cnpa/event-v4/stats/index.vue`
- 旧页废弃 banner：`competition/score`, `competition/match-data-audit`

### SQL / 脚本

- `docs/sql/cnpa-v4-round5-match-data-patch.sql`
- `docs/sql/cnpa-v4-round5-menu.sql`
- `scripts/run_r5_match_data.py`
- `scripts/smoke_v4_r5_match_data_stats.py`

---

## 4. 新增 Web API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/basket/v4/player/match-submission/my-matches` | 我的可提交比赛 |
| GET | `/basket/v4/player/match-submission/{matchId}/detail` | 提交详情/草稿 |
| POST | `/basket/v4/player/match-submission/save` | 保存草稿 |
| POST | `/basket/v4/player/match-submission/submit` | 提交审核 |
| POST | `/basket/v4/player/match-submission/resubmit` | 驳回后重提 |
| GET | `/basket/v4/player/match-submission/my-list` | 我的提交列表 |

---

## 5. 新增 Admin API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/basket/v4/match-submission-audit/list` | 审核列表（submission 级） |
| GET | `/basket/v4/match-submission-audit/{submissionId}` | 审核详情 |
| POST | `/basket/v4/match-submission-audit/{submissionId}/approve` | 审核通过 + 结算 |
| POST | `/basket/v4/match-submission-audit/{submissionId}/reject` | 审核驳回 |
| GET | `/basket/v4/stats/standing` | Division 积分榜 |
| GET | `/basket/v4/stats/team-stat` | Division 球队统计 |
| GET | `/basket/v4/stats/player-stat` | Division 球员统计 |
| GET | `/basket/v4/stats/rank` | Division 排名榜 |

---

## 6. 新增后台页面 / 菜单

| 菜单 | 路径 | 组件 |
|------|------|------|
| V4比赛数据审核 | `event-v4-match-submission-audit` | `cnpa/event-v4/match-submission-audit/index` |
| V4统计榜单 | `event-v4-stats` | `cnpa/event-v4/stats/index` |

---

## 7. 比赛数据提交流程

```
已发布 Match (PUBLISHED, 非 CANCELLED)
  → 球员/队长校验（EventPlayer 或 CaptainGrant）
  → POST save（DRAFT，matchId + submitEventTeamId）
  → POST submit（PENDING_REVIEW）
  → 不写官方 Match / Stat
```

维度固定：`matchId + submitEventTeamId + versionNo`。

---

## 8. 审核通过 / 驳回流程

**通过（仅 PENDING_REVIEW）：**

1. `submissionStatus = APPROVED`
2. 写 `basket_match`：`home_score`, `away_score`, `winner_event_team_id`, `match_status=FINISHED`
3. upsert `basket_match_team_stat`（提交方完整 stat + 对手最小 stat）
4. upsert `basket_match_player_stat`（提交方球员）
5. `rebuildDivisionStats(divisionId)` → standing / team_stat / player_stat / rank

**驳回：**

- `submissionStatus = REJECTED`
- 不写官方 Match / Stat，不触发重算

---

## 9. 官方 Match 结果写入说明

审核通过后以 submission 的 `homeScore/awayScore` 更新 `basket_match`。  
**P0 覆盖策略：** 后审核通过的 APPROVED submission 覆盖官方比分；`match_*_stat.source_submission_id` 标记来源；历史 APPROVED 不删除。

---

## 10. 单场 TeamStat / PlayerStat 写入说明

- `basket_match_team_stat`：按 `matchId + eventTeamId` upsert
- `basket_match_player_stat`：按 `matchId + eventPlayerId` upsert
- 仅 FINISHED + PUBLISHED 的 Match 参与 Division 汇总

---

## 11. Standing / TeamStat / PlayerStat / Rank 重算说明

`BasketV4StatsSettlementServiceImpl.rebuildDivisionStats`：

1. **Standing**：胜场 DESC → 净胜分 DESC → 得分 DESC → eventTeamId ASC；积分 win=2, lose=1
2. **TeamStat**：汇总 `match_team_stat`
3. **PlayerStat**：汇总 `match_player_stat`
4. **Rank**：`TEAM_STANDING`, `PLAYER_SCORE`, `PLAYER_ASSIST`, `PLAYER_REBOUND`

全部绑定 `divisionId`，不跨 Division。

---

## 12. 一方审核中不影响另一方提交

- submission 状态绑定 `matchId + submitEventTeamId`
- Home `PENDING_REVIEW` 时，Away 可独立 `save/submit`
- 审核列表/状态均为 submission 级别，不把 Match 整体置为审核中

---

## 13. 旧入口隐藏 / 废弃处理

| 类型 | 处理 |
|------|------|
| SQL 隐藏 | R4 已隐藏：旧赛程/比分录入/旧比赛数据审核等；R5 追加：旧结算/积分榜/排名/赛季统计等 menu |
| 页面 banner | `competition/score`, `competition/match-data-audit` 增加 `[已废弃]` 提示 |
| 旧代码 | 保留，第 7+ 轮再删 |
| 旧 Service | R5 新代码未调用 `MatchData*`, `DataSettlement*`, 旧 Standing Service |

---

## 14. 新旧代码隔离检查

| 检查项 | 结果 |
|--------|------|
| 不写 `basket_match_data_*` | ✅ |
| 不写 `basket_team_season_stat` / `basket_player_season_stat` | ✅ |
| 不调用旧 MatchData / Settlement Service | ✅ |
| V4 新 Service 仅用 `mapper.v4` / `service.v4` | ✅ |
| 未引入 `eventId`/`tournamentId` 作 seasonId | ✅ |

---

## 15. API 冒烟测试记录（初版）

脚本：`scripts/smoke_v4_r5_match_data_stats.py`（初版轻量冒烟，已被 full acceptance 取代）

---

## 16. 第 5 轮真实冒烟补验收记录

**执行时间：** 2026-08-04 02:05+  
**脚本：** `scripts/smoke_v4_r5_full_acceptance.py`  
**结果 JSON：** `docs/r5-smoke-results.json`  
**结果：** **39/39 PASS，0 FAIL**（1 SKIP：无 CANCELLED Match 测试数据）

### 16.1 服务重启

| 服务 | 地址 | 状态 |
|------|------|------|
| basketball-admin | `http://127.0.0.1:8080` | ✅ 已 kill 旧进程并用 Maven 重新 `spring-boot:run` 加载 R5 代码 |
| basketball-web | `http://127.0.0.1:8881` | ✅ 已重启（`MiniApplication`） |

补验收期间修复：`BasketV4StatsSettlementServiceImpl` 对手最小 `match_team_stat` 写入时 `field_goal_made` 等 NOT NULL 字段默认值。

### 16.2 测试数据

| 项 | 值 |
|----|-----|
| Division | **5**（R3_QUOTA_TEST_PC） |
| Match | **1**（V4-5-1-01） |
| Home EventTeam | **14**（CNPA战队Alpha） |
| Away EventTeam | **15**（CNPA战队Beta） |

### 16.3 Home 方提交结果

| 步骤 | 结果 |
|------|------|
| GET my-matches | PASS，`canSubmit=true`，返回 `mySubmissionStatus` |
| POST save | submissionId=**10**，DRAFT，homeScore=88/awayScore=75，teamStat/playerStat/material 各 1 条 |
| save 不写官方 | Match 仍 SCHEDULED、无比分；match_team_stat=0；standing=0 |
| POST submit | **PENDING_REVIEW**，submitTime 有值 |

### 16.4 Away 方提交结果

| 步骤 | 结果 |
|------|------|
| Home PENDING_REVIEW 期间 Away save+submit | submissionId=**11**，**PENDING_REVIEW** ✅ |
| Match 整体状态 | 仍为 **SCHEDULED**（无整体审核中） ✅ |

### 16.5 A 队审核中 B 队是否可提交

**是。** Home 方 PENDING_REVIEW 时，Away 方独立完成 save + submit，双方 submission 在审核列表中各自为 PENDING_REVIEW。

### 16.6 驳回结果

- Away submissionId=11 → **REJECTED**
- 驳回后官方 Match 仍无比分（home_score=NULL）

### 16.7 重提 versionNo 结果

- oldSubmissionId=11（v1 REJECTED 保留）
- resubmit → newSubmissionId=**12**，**versionNo=2**，status=**DRAFT**

### 16.8 审核通过结果

- Home submissionId=10 → **APPROVED**

### 16.9 Match 官方比分写入

| 字段 | 值 |
|------|-----|
| home_score | 88 |
| away_score | 75 |
| winner_event_team_id | 14（Home） |
| match_status | FINISHED |

### 16.10 match_team_stat 写入

- 双方各 1 条：`((eventTeamId=14, sourceSubmissionId=10), (eventTeamId=15, sourceSubmissionId=10))`

### 16.11 match_player_stat 写入

- count=1，source_submission_id=10

### 16.12 ~ 16.17 统计重算

| 表 | Division=5 行数 / 类型 |
|----|------------------------|
| basket_standing | 2 |
| basket_team_stat | 2 |
| basket_player_stat | 1 |
| basket_rank | 5 行；含 TEAM_STANDING / PLAYER_SCORE / PLAYER_ASSIST / PLAYER_REBOUND |

统计 API（standing / team-stat / player-stat / rank）均返回 division 隔离数据。

### 16.18 拒绝场景测试

| 场景 | 结果 |
|------|------|
| 未发布 Match 提交 | PASS |
| CANCELLED Match | SKIP（无测试数据） |
| 无效 submitEventTeamId | PASS |
| 非本队 EventPlayer | PASS |
| PENDING_REVIEW 覆盖 save | PASS |
| APPROVED 不可 save | PASS |
| 非 PENDING 审核通过 | PASS |
| 非 PENDING 驳回 | PASS |

### 16.19 V4 是否写旧 match_data 表

**否。** `basket_match_data_submission` / `basket_match_data_audit` 行数=0。

### 16.20 是否可以进入第 6 轮

**可以。** R5 主链路已真实跑通，建议进入第 6 轮（前台赛事详情 / 公开榜单展示）。

---

## 17. 风险与待确认项

1. **双方先后审核覆盖**：P0 以后通过者覆盖官方比分；若需「双方一致才 FINISHED」需后续规则。
2. **分节比分**：后置，当前仅总比分。
3. **CANCELLED Match 拒绝场景**：补验收库中无 CANCELLED 样本，未实测。
4. **材料 fileId**：已存储，前台展示依赖第 6/7 轮。

---

## 18. 是否建议进入第 6 轮

**建议进入第 6 轮。** R5 真实冒烟 39/39 PASS，提交→审核→官方结果→统计结算全链路已验证。

---

*Round 5 完成（含补验收）。Round 6 不在本轮范围。*
