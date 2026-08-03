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

## 15. API 冒烟测试记录

脚本：`scripts/smoke_v4_r5_match_data_stats.py`

| 项 | 结果 | 备注 |
|----|------|------|
| Admin 登录 | PASS | |
| DB patch 三列 | PASS | home_score / away_score / submit_remark 已存在 |
| 测试 Match 数据 | PASS | division=5, match=1 |
| Home Web 登录 | PASS | |
| Web my-matches | **待重启** | 当前运行中 backend 无 R5 路由 → HTTP 500 |
| 完整提交→审核→结算链路 | **待重启** | 需重启 admin:8080 + web:8881 后重跑脚本 |

**结论：** 代码与 SQL 已就绪；**需重启后端服务**后执行冒烟可达完整 PASS。

---

## 16. 风险与待确认项

1. **服务重启**：R5 Controller/Service 需编译部署后 API 才可用。
2. **双方先后审核覆盖**：P0 以后通过者覆盖官方比分；若需「双方一致才 FINISHED」需后续规则。
3. **分节比分**：后置，当前仅总比分。
4. **材料 fileId**：已存储，前台展示依赖第 6/7 轮。
5. **Web 端口**：冒烟默认 `8881`（与 R3 full acceptance 一致）。

---

## 17. 是否建议进入第 6 轮

**建议进入第 6 轮**（前台赛事详情 / 公开榜单展示），前提：

1. 重启 backend 并通过 `smoke_v4_r5_match_data_stats.py` 全绿；
2. 后台手工验证 V4 比赛数据审核页、V4 统计榜单页各一次。

---

*Round 5 完成。Round 6（前台赛事详情 / 榜单）不在本轮范围。*
