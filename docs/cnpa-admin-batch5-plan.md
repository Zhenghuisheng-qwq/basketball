# CNPA Admin 第五批执行计划

> **日期**：2026-07-25  
> **范围**：比赛数据审核、正式结果、争议修订、名单快照、统计幂等  
> **前置**：第四批回填完成（17 event teams, 131 season rosters）  
> **设计文档**：`cnpa-match-current.md` / `cnpa-match-data-design.md` / `cnpa-statistics-design.md`

---

## 1. 阶段划分

| 阶段 | 内容 | 产出 | 状态 |
|------|------|------|------|
| **0** | 现状盘点 + 迁移前 SQL | `cnpa-match-current.md`, `cnpa-batch5-before.sql` | ✅ |
| **1** | 数据库迁移 | `cnpa-batch5-migration.sql` 执行 + 17 场 official_result 回填 | ⏳ |
| **2** | Admin 后端：正式结果 + 审核 + 修订 | Domain/Mapper/Service/Controller | ⏳ |
| **3** | Admin 后端：改造 confirm 链路 + 名单 + 确认集 | 改造现有 Service + Mapper | ⏳ |
| **4** | Admin 后端：结算幂等 + 批次 | `stat_sync_record`, `settlement_batch` | ⏳ |
| **5** | Admin-ui 新页面 + 改造现有比赛页 | 4 新菜单 + 改造 4 现有页 | ⏳ |
| **6** | Web 公开 API 读 official_result | `BasketPublicMatchServiceImpl` | ⏳ |
| **7** | 构建验证 + 集成测试 | `cnpa-admin-batch5-result.md` | ⏳ |

---

## 2. 数据库

### 2.1 脚本

| 脚本 | 路径 |
|------|------|
| 迁移前快照 | `docs/sql/cnpa-batch5-before.sql` |
| 迁移 | `docs/sql/cnpa-batch5-migration.sql` |
| 回滚 | `docs/sql/cnpa-batch5-rollback.sql` |
| 执行 | `docs/sql/run_batch5_migration.py` |

### 2.2 新建表（6）

- `basket_match_official_result`
- `basket_match_data_audit`
- `basket_match_data_dispute`
- `basket_match_data_revision`
- `basket_data_settlement_batch`
- `basket_stat_sync_record`

### 2.3 扩展表（2）

- `basket_match`：+5 列
- `basket_match_roster`：+8 列

### 2.4 菜单（4）

| menu_id | 名称 | 路径 | 权限 |
|---------|------|------|------|
| 2230 | 比赛数据审核 | `match-data-audit` | `basket:matchAudit:list` |
| 2231 | 数据争议处理 | `match-dispute` | `basket:matchDispute:list` |
| 2232 | 异常比赛处理 | `match-abnormal` | `basket:matchAbnormal:list` |
| 2233 | 驳回比赛数据 | (按钮) | `basket:matchAudit:reject` |

父菜单：2205（比赛管理）

---

## 3. Admin 后端文件清单

### 3.1 新建 Domain

```
basketball-admin/src/main/java/com/zhs/basket/domain/
  BasketMatchOfficialResult.java
  BasketMatchDataAudit.java
  BasketMatchDataDispute.java
  BasketMatchDataRevision.java
  BasketDataSettlementBatch.java
  BasketStatSyncRecord.java
```

### 3.2 新建 Enum

```
basketball-admin/src/main/java/com/zhs/basket/enums/
  MatchResultStatusEnum.java      # result_status 0-5
  MatchAuditTypeEnum.java         # SCORE/STAT/RESULT
  MatchAuditStatusEnum.java       # PENDING/APPROVED/REJECTED
  MatchDisputeStatusEnum.java
  MatchRevisionStatusEnum.java
  StatSyncTypeEnum.java
  SettlementBatchStatusEnum.java
```

### 3.3 新建 Mapper + XML

```
basketball-admin/src/main/java/com/zhs/basket/mapper/
  BasketMatchOfficialResultMapper.java
  BasketMatchDataAuditMapper.java
  BasketMatchDataDisputeMapper.java
  BasketMatchDataRevisionMapper.java
  BasketDataSettlementBatchMapper.java
  BasketStatSyncRecordMapper.java

basketball-admin/src/main/resources/mapper/basket/
  BasketMatchOfficialResultMapper.xml
  BasketMatchDataAuditMapper.xml
  BasketMatchDataDisputeMapper.xml
  BasketMatchDataRevisionMapper.xml
  BasketDataSettlementBatchMapper.xml
  BasketStatSyncRecordMapper.xml
```

### 3.4 新建 Service

```
basketball-admin/src/main/java/com/zhs/basket/service/
  IBasketMatchDataAuditService.java
  IBasketMatchDisputeService.java
  IBasketMatchRevisionService.java
  IBasketMatchOfficialResultService.java

basketball-admin/src/main/java/com/zhs/basket/service/impl/
  BasketMatchDataAuditServiceImpl.java
  BasketMatchDisputeServiceImpl.java
  BasketMatchRevisionServiceImpl.java
  BasketMatchOfficialResultServiceImpl.java
```

### 3.5 新建 Controller

```
basketball-admin/src/main/java/com/zhs/web/controller/basket/
  BasketMatchDataAuditController.java
  BasketMatchDisputeController.java
  BasketMatchRevisionController.java
  BasketMatchAbnormalController.java
```

### 3.6 新建 Query / Req / Vo

```
basketball-admin/src/main/java/com/zhs/basket/query/
  MatchDataAuditQuery.java
  MatchDisputeQuery.java
  MatchRevisionQuery.java

basketball-admin/src/main/java/com/zhs/basket/req/
  MatchDataAuditSubmitReq.java
  MatchDataAuditApproveReq.java
  MatchDataAuditRejectReq.java
  MatchDisputeCreateReq.java
  MatchDisputeHandleReq.java
  MatchRevisionCreateReq.java
  MatchRevisionApproveReq.java
  MatchAbnormalVoidReq.java
  MatchAbnormalRematchReq.java

basketball-admin/src/main/java/com/zhs/basket/vo/
  MatchDataAuditListVo.java
  MatchDataAuditDetailVo.java
  MatchDisputeListVo.java
  MatchDisputeDetailVo.java
  MatchRevisionListVo.java
  MatchOfficialResultVo.java
  SettlementBatchVo.java
```

### 3.7 改造现有文件

| 文件 | 改动要点 |
|------|----------|
| `BasketMatchScoreServiceImpl.java` | `confirmScore` 写 audit + `score_confirmed` |
| `BasketMatchStatServiceImpl.java` | `confirmResult` 拆 audit approve；写 official_result |
| `BasketMatchLineupServiceImpl.java` | 可选球员改 `season_roster`；写快照列 |
| `BasketDataSettlementServiceImpl.java` | 写 batch + sync_record；确认集改 official_result |
| `BasketConfirmedMatchAdminMapper.xml` | JOIN official_result |
| `BasketMatchMapper.xml` | 支持 data_version CAS |
| `BasketMatchRosterMapper.xml` | season_roster 字段 |
| `domain/BasketMatch.java` | 新字段 |
| `domain/BasketMatchRoster.java` | 新字段 |

### 3.8 Support 工具

```
basketball-admin/src/main/java/com/zhs/basket/support/
  OfficialResultHelper.java       # supersede / create current
  MatchDataVersionHelper.java     # CAS 递增
  StatSyncIdempotencyHelper.java  # 幂等键生成与检查
  DisputeNoGenerator.java
  RevisionNoGenerator.java
```

---

## 4. Admin-ui 文件清单

### 4.1 新建 API

```
basketball-admin-ui/src/api/basket/
  matchDataAudit.ts
  matchDispute.ts
  matchRevision.ts
  matchAbnormal.ts
```

### 4.2 新建页面

```
basketball-admin-ui/src/views/cnpa/competition/
  match-data-audit/index.vue    # 待审列表 + 详情 + 通过/驳回
  match-dispute/index.vue       # 争议列表 + 处理
  match-abnormal/index.vue      # 弃赛/作废/重赛
  match-revision/index.vue      # 修订列表（可选合并到 dispute）
```

### 4.3 改造现有页面

| 页面 | 改动 |
|------|------|
| `competition/score/index.vue` | 展示 audit 状态；confirm 走审核流 |
| `competition/match-data-entry/index.vue` | 展示 official_result / revision 历史 |
| `competition/lineup/index.vue` | 球员来源 season_roster；展示快照 |
| `statistics/settlement/index.vue` | 展示 settlement_batch 历史 |

### 4.4 类型扩展

```
basketball-admin-ui/src/types/api/basket/index.ts
  MatchOfficialResultVo, MatchDataAuditVo, MatchDisputeVo, ...
```

---

## 5. Web 后端文件清单

| 文件 | 改动 |
|------|------|
| `basketball-web/.../BasketPublicMatchServiceImpl.java` | 赛果/详情读 official_result |
| `basketball-web/.../model/BasketMatch.java` | 新字段（若 ORM 同步） |
| 新建 `BasketMatchOfficialResultDao.java` | 读当前正式结果 |

Web-ui **无需**新页面；现有 `publicMatch.ts` 响应字段兼容（比分仍返回 homeScore/awayScore）。

---

## 6. API 列表

### 6.1 比赛数据审核（新建）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/basket/match-data-audit/list` | `basket:matchAudit:list` | 待审/已审分页 |
| GET | `/basket/match-data-audit/{auditId}` | `basket:matchAudit:query` | 审核详情 |
| POST | `/basket/match/{matchId}/audit/submit` | `basket:matchStat:submit` | 提交审核 |
| POST | `/basket/match-data-audit/{auditId}/approve` | `basket:matchAudit:approve` | 通过 → confirm + official_result |
| POST | `/basket/match-data-audit/{auditId}/reject` | `basket:matchAudit:reject` | 驳回 |

### 6.2 数据争议（新建）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/basket/match-dispute/list` | `basket:matchDispute:list` | 争议列表 |
| GET | `/basket/match-dispute/{id}` | `basket:matchDispute:query` | 详情 |
| POST | `/basket/match-dispute` | `basket:matchDispute:add` | 发起争议 |
| POST | `/basket/match-dispute/{id}/handle` | `basket:matchDispute:handle` | 处理 |

### 6.3 数据修订（新建）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/basket/match-revision/list` | `basket:matchRevision:list` | 修订列表 |
| POST | `/basket/match-revision` | `basket:matchRevision:add` | 创建修订 |
| POST | `/basket/match-revision/{id}/approve` | `basket:matchRevision:approve` | 生效 → supersede + rebuild |

### 6.4 异常比赛（新建）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/basket/match-abnormal/list` | `basket:matchAbnormal:list` | 异常比赛列表 |
| POST | `/basket/match/{matchId}/void` | `basket:matchAbnormal:void` | 作废 |
| POST | `/basket/match/{matchId}/rematch` | `basket:matchAbnormal:rematch` | 重赛 |
| POST | `/basket/match/{matchId}/forfeit` | `basket:score:edit` | 已有，增强写 official_result |

### 6.5 改造现有 API

| 方法 | 路径 | 改动 |
|------|------|------|
| POST | `/basket/match/{id}/score/confirm` | 写 audit + score_confirmed |
| POST | `/basket/match/{id}/result/confirm` | 改为内部 approve 或 deprecated |
| POST | `/basket/match/{id}/result/reopen` | supersede official_result |
| GET | `/basket/match/{id}/available-player/{teamId}` | 返回 season_roster 球员 |
| PUT | `/basket/match/{id}/lineup/{teamId}` | 接受 season_roster_id |
| POST | `/basket/data-settlement/season/{id}/rebuild` | 写 batch + sync_record |

### 6.6 公开 API（Web，改造）

| 方法 | 路径 | 改动 |
|------|------|------|
| GET | `/basket/public/season/{id}/result/page` | 比分读 official_result |
| GET | `/basket/public/match/{id}` | statsAvailable 查 official_result |

---

## 7. 核心业务流程

### 7.1 正常比赛闭环

```text
编排 → 开始 → 录名单(season_roster) → 录比分 → 录统计
  → submitAudit → 管理员 approve
  → INSERT official_result + data_status=CONFIRMED
  → rebuildMatch (同事务)
  → stat_sync_record 幂等键
```

### 7.2 争议修订闭环

```text
CONFIRMED 比赛 → 发起 dispute → 管理员 handle
  → 创建 revision → 修改数据 → revision approve
  → supersede official_result → rebuildSeason
```

### 7.3 重开闭环

```text
reopenResult → official_result.is_current=0
  → data_status=REOPENED → rebuildMatch（确认集减少）
  → 修改后重新 submitAudit
```

---

## 8. 禁止触碰

- 第四批报名/正式阵容生成链路（仅只读 `season_roster`）
- 球员/球队认证审核
- 公告/荣誉/内容管理
- uniapp 端（本批次不含）

---

## 9. 测试计划

### 9.1 数据库迁移

- [ ] 执行 `cnpa-batch5-before.sql` 记录基线
- [ ] 执行 `run_batch5_migration.py`
- [ ] 验证 17 条 `basket_match_official_result` 生成
- [ ] 验证新表为空、扩展列默认值正确
- [ ] 验证菜单 2230–2233 可见

### 9.2 审核流

- [ ] 提交一场 SUBMITTED 比赛 audit → 列表可见 PENDING
- [ ] approve → data_status=2, official_result.is_current=1
- [ ] reject → data_status 仍为 1，reject_reason 可查
- [ ] approve 后球队/球员 stat 不可编辑

### 9.3 统计 rebuild

- [ ] approve 后 standing/stat/rank 与迁移前 17 场一致
- [ ] reopen 一场 → 确认集减 1，积分榜变化正确
- [ ] 连续两次 rebuildSeason → 第二次幂等或锁拒绝
- [ ] `basket_stat_sync_record` 幂等键不重复

### 9.4 修订

- [ ] 创建 revision → 修改比分 → approve
- [ ] 旧 official_result.is_current=0，新 revision_no=2
- [ ] season stat 全量正确

### 9.5 名单

- [ ] 可选球员来自 season_roster（131 条范围内）
- [ ] 保存后 roster 行含 snapshot 列
- [ ] confirmLineup 后 roster_status=1

### 9.6 公开 API

- [ ] 赛果页仅展示有 current official_result 的比赛
- [ ] 详情 statsAvailable 与 admin 一致
- [ ] rebuild 后公开缓存失效

### 9.7 回归

- [ ] 现有 4 比赛 Admin 页（schedule/score/lineup/match-data-entry）不报错
- [ ] settlement 摘要数字正确
- [ ] LocalDemo seed 仍可 confirm（兼容层）

### 9.8 构建

- [ ] `basketball-admin-ui` → `npm run build:prod`
- [ ] `basketball-web-ui` → `npm run build`
- [ ] `basketball-admin` / `basketball-web` → `mvn -DskipTests package`

---

## 10. 回滚方案

1. 停止 Admin 服务
2. 执行 `docs/sql/cnpa-batch5-rollback.sql`
3. 回退代码到 batch5 之前
4. 重新 deploy

**注意**：回滚会删除 `official_result` 及 audit/dispute/revision 数据；`basket_match.data_version` 等扩展列一并移除。原 17 场确认状态仍在 `basket_match.data_status=2`。

---

## 11. 交付物

| 文档 | 路径 |
|------|------|
| 现状盘点 | `docs/cnpa-match-current.md` |
| 比赛数据设计 | `docs/cnpa-match-data-design.md` |
| 统计设计 | `docs/cnpa-statistics-design.md` |
| 执行计划 | `docs/cnpa-admin-batch5-plan.md` |
| 执行结果（阶段 7） | `docs/cnpa-admin-batch5-result.md` |
| SQL | `docs/sql/cnpa-batch5-*.sql` |

---

## 12. 风险与缓解

| 风险 | 缓解 |
|------|------|
| confirmScore/confirmResult 双轨混淆 | UI 合并为「提交审核」单按钮；score confirm 写 audit |
| 旧 roster 无 season_roster_id | 只读展示；新录入走新链路 |
| rebuild 与 confirm 事务过长 | 保持现有同事务（已验证）；锁 TTL 30min |
| 17 场迁移 official_result 比分不准 | 迁移 SQL 读 basket_match 当前字段；人工抽检 |
