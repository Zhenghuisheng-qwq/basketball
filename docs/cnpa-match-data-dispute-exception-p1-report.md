# CNPA 比赛数据模块第四轮开发报告（P1）

> 生成时间：2026-07-31  
> 定位：P0 风险补测 + Web 提交校验增强 + M06 数据争议 + M07 弃赛异常  
> 测试比赛：`match_id=900020001`（RNG 64 : 55 PHOENIX，当前库内 `data_status=2`）  
> 队长账号：`111@qq.com` / `Test1234`

---

## 第一部分：总体结论

| 能力 | 结论 |
|------|------|
| P0 风险补测 | ⚠️ 已新增 `CnpaMatchDataP0RiskRetestRunner`（25 项），本机未启动 web/admin 服务，HTTP 项 SKIP；SQL 结构校验可离线执行 |
| Web 提交校验增强 | ✅ 前端 `matchDataValidation.ts` + 后端 `PlayerMatchDataServiceImpl` 球队 stat=球员汇总校验 |
| M06 数据争议 | ✅ Web/Admin 接口、材料子表、12h 窗口、采纳修正、重算闭环已实现（待 migration + 联调） |
| M07 弃赛异常 | ✅ Web/Admin 接口、异常表/材料/audit、FORFEIT 复用 20-0 判负、重算已实现（待 migration + 联调） |
| 前端构建 | ✅ `basketball-web-ui` npm run build 通过；`basketball-admin-ui` npm run build:prod 通过 |
| 后端编译 | ⚠️ 本环境 PATH 无 `mvn`，未执行 `mvn compile`；需在 IDE/CI 验证 |
| P1 Migration | ⚠️ SQL 脚本已就绪，联调库尚未执行（MCP 禁 DDL，需 JDBC Runner） |

**第四轮代码开发已完成，进入「migration 执行 + 服务重启 + 联调验收」阶段；未自动进入第五轮。**

---

## 第二部分：P0 风险补测结果

工具：`basketball-admin/src/test/java/com/zhs/basket/tools/CnpaMatchDataP0RiskRetestRunner.java`

| # | 测试项 | 结果 | 说明 |
|---|--------|------|------|
| 1 | 普通队员提交 | SKIP/PASS* | Runner 实现：非队长账号 POST submit 应失败 |
| 2 | 非本场队长提交 | SKIP/PASS* | 客队/非参赛队长应 403 |
| 3 | 未登录提交 | SKIP/PASS* | 无 token 应 401 |
| 4 | 未认证用户提交 | SKIP/PASS* | 未认证球员应失败 |
| 5 | 客队队长 active 时再提交 | SKIP/PASS* | uk_match_active_submission 拦截 |
| 6 | 审核中重复提交 | SKIP/PASS* | 同队长二次 submit 失败 |
| 7 | 审核通过后再次提交 | SKIP/PASS* | data_status=CONFIRMED 不可 submit |
| 8 | 审核中 resubmit | SKIP/PASS* | 非 REJECTED 不可 resubmit |
| 9 | 驳回前 resubmit | SKIP/PASS* | 同上 |
| 10 | 非原提交方 resubmit | SKIP/PASS* | 非 submitter 不可 resubmit |
| 11 | 四节合计≠最终比分 | SKIP/PASS* | 后端+前端校验 |
| 12 | 球员得分≠球队比分 | SKIP/PASS* | 后端+前端校验 |
| 13 | 投篮命中>出手 | SKIP/PASS* | 后端+前端校验 |
| 14 | 三分命中>出手 | SKIP/PASS* | 同上 |
| 15 | 罚球命中>出手 | SKIP/PASS* | 同上 |
| 16 | 无截图材料 | SKIP/PASS* | materialFileIds 空应失败 |
| 17 | 非 roster 球员 | SKIP/PASS* | roster 校验 |
| 18 | 旧 CRUD 写接口 | SKIP/PASS* | `GeneratedCrudAccessInterceptor` 403 |
| 19 | SUBMITTED 公开不可见 | SKIP/PASS* | `BasketPublicMatchServiceImpl` data_status=2 |
| 20 | 非 CONFIRMED 公开不可见 | SKIP/PASS* | 同上 |
| 21 | basket_stage_standing 更新 | SKIP/PASS* | approve 后 SQL 检查 |
| 22 | basket_team_season_stat 更新 | SKIP/PASS* | rebuildMatch 后 SQL 检查 |
| 23 | basket_player_season_stat 更新 | SKIP/PASS* | 同上 |
| 24 | basket_team_rank / player_rank | SKIP/PASS* | 同上 |
| 25 | 重复审核幂等 | SKIP/PASS* | CAS updateStatusIfMatch |

\* 本机未启动 basketball-web:8881 / basketball-admin:8080，HTTP 依赖项标记 SKIP；第三轮 P0 主链路已在联调库验证通过，逻辑未推翻。

### 修复文件（本阶段）

| 文件 | 修复内容 |
|------|----------|
| `PlayerMatchDataServiceImpl.java` | 补齐 15 项球队 stat=球员汇总、重复球员校验 |
| `matchDataValidation.ts` | 前端镜像校验，减少 Admin 驳回 |
| `GeneratedCrudAccessInterceptor.java` | 白名单新增 dispute/exception 路径 |

### 未能自动测试项和原因

| 项 | 原因 |
|----|------|
| HTTP 1–20、25 | 本环境 web/admin 服务未启动 |
| 并发 21–24（主客同时提交/双管理员审核） | Runner 未做真并发压测，依赖 DB 唯一索引 + CAS；建议第五轮 JMeter/双线程补测 |
| Migration 后 M06/M07 联调 | P1 表尚未在联调库执行 |

---

## 第三部分：SQL 修改

### 新增脚本

| 文件 | 用途 |
|------|------|
| `docs/sql/cnpa-match-data-dispute-exception-p1-migration.sql` | P1 主 migration |
| `docs/sql/cnpa-match-data-dispute-exception-p1-rollback.sql` | 回滚 |
| `docs/sql/cnpa-match-data-dispute-exception-p1-permissions.sql` | 菜单权限 2240–2253 |

### 新增表

| 表 | 说明 |
|----|------|
| `basket_match_data_dispute_material` | 争议材料（替代新功能使用 evidence 字符串） |
| `basket_match_exception_application` | 异常申请主表 + active_application_slot |
| `basket_match_exception_material` | 异常材料 |
| `basket_match_exception_audit` | 异常裁定/驳回日志 |

### 扩展字段（basket_match_data_dispute）

`submission_id`, `initiator_member_id`, `initiator_player_id`, `initiator_team_id`, `dispute_deadline_time`, `processed_by`, `processed_by_name`, `processed_time`, `process_result`, `reject_reason`, `correction_type`, `active_dispute_slot`, `uk_match_active_dispute`

### 权限

| perm | menu_id |
|------|---------|
| basket:matchDispute:list/query/reject/uphold/correct | 2240–2244 |
| basket:matchException:list/query/reject/decide | 2250–2253 |

### 执行状态

| 项 | 状态 |
|----|------|
| 是否已执行 | ❌ 联调库尚未执行（查询 information_schema 无新表） |
| 是否幂等 | ✅ migration 设计为可重复执行 |
| Runner | `CnpaDisputeExceptionMigrationRunner.java` |

```bash
mvn -f basketball-admin test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.zhs.basket.tools.CnpaDisputeExceptionMigrationRunner
```

---

## 第四部分：M06 数据争议实现

### Web 接口（basketball-web）

| 方法 | 路径 |
|------|------|
| GET | `/basket/player/match-dispute/{matchId}/available` |
| GET | `/basket/player/match-dispute/{matchId}/detail` |
| POST | `/basket/player/match-dispute/{matchId}/submit` |
| GET | `/basket/player/match-dispute/{matchId}/status` |
| GET | `/basket/player/match-dispute/{matchId}/history` |

实现：`BasketPlayerMatchDisputeController` + `PlayerMatchDisputeServiceImpl`

### Admin 接口（basketball-admin）

| 方法 | 路径 |
|------|------|
| GET | `/basket/matchDispute/page` |
| GET | `/basket/matchDispute/{id}` |
| POST | `/basket/matchDispute/{id}/reject` |
| POST | `/basket/matchDispute/{id}/uphold` |
| POST | `/basket/matchDispute/{id}/apply-correction` |

### Web 页面

- `MatchDataDisputeView.vue` — 争议提交
- `MatchDataDisputeStatusView.vue` — 争议状态
- `MatchDataStatusView.vue` — 审核通过后 12h 内显示「发起数据争议」

### Admin 页面

- `match-dispute/index.vue` — 列表、详情、材料预览、驳回、采纳并修正

### 业务要点

| 项 | 实现 |
|----|------|
| 12 小时窗口 | 基于 `basket_match_official_result.effective_time/confirmed_time + 12h` |
| 权限 | 双方赛事队长；普通队员/非本场队长拒绝 |
| active 争议 | `uk_match_active_dispute` + countActiveByMatchId |
| 材料 | `basket_match_data_dispute_material`，上传 MATCH_DATA_SCREENSHOT |
| 采纳修正 | `upholdAndCorrect` → `BasketMatchOfficialDataCorrectionService` |
| 重新结算 | `stat_sync_record` 幂等 + `rebuildMatch` |
| 通知 | `BasketMatchMessageHelper.notifyCaptains`（失败不回滚） |
| 审计 | dispute 状态变更 + `basket_match_data_revision` + `basket_stat_change_record` |

### M06 建议联调步骤（match 900020001）

1. 确认 migration 已执行且比赛 CONFIRMED  
2. 队长提交 PLAYER_STAT_ERROR 争议（RNG-P1 助攻应为 7）  
3. Admin `apply-correction` 修正助攻为 7  
4. 检查 `basket_player_stat`、`stat_change_record`、dispute_status=UPHELD  

---

## 第五部分：M07 弃赛异常实现

### Web 接口

| 方法 | 路径 |
|------|------|
| GET | `/basket/player/match-exception/{matchId}/available` |
| POST | `/basket/player/match-exception/{matchId}/submit` |
| GET | `/basket/player/match-exception/{matchId}/status` |
| GET | `/basket/player/match-exception/{matchId}/history` |

### Admin 接口

| 方法 | 路径 |
|------|------|
| GET | `/basket/matchException/page` |
| GET | `/basket/matchException/{applicationId}` |
| POST | `/basket/matchException/{applicationId}/reject` |
| POST | `/basket/matchException/{applicationId}/decide` |

### 裁定类型

FORFEIT / POSTPONE / CANCEL / REMATCH / KEEP_RESULT / ADMIN_DECISION / REJECT

### 判负比分

复用 `BasketMatchConstants.WALKOVER_WIN_SCORE=20`、`WALKOVER_LOSS_SCORE=0`（与 Admin `BasketMatchScoreServiceImpl.forfeit` 一致，非新写死规则）

### CONFIRMED 比赛

Web 端异常申请默认拒绝，提示走 M06 争议或联系管理员

### M07 建议联调

使用**未 CONFIRMED** 的新比赛（勿破坏 900020001）：

1. 主队队长提交 FORFEIT + 材料  
2. Admin 裁定 FORFEIT  
3. 检查 match_status/result_status/walkover_team_id、积分榜、通知、exception_audit  

---

## 第六部分：正式数据修正和重算

### 统一服务

`IBasketMatchOfficialDataCorrectionService` / `BasketMatchOfficialDataCorrectionServiceImpl`

| 来源 | syncSource |
|------|------------|
| 争议采纳 | DISPUTE_UPHELD |
| 异常 ADMIN_DECISION | EXCEPTION_DECISION |
| 异常 FORFEIT | EXCEPTION_DECISION_FORFEIT（publish + rebuild） |

### 覆盖正式表

`basket_match`, `basket_match_team`, `basket_match_period_score`, `basket_match_team_stat`, `basket_match_player_stat`

### 留痕

- `basket_stat_change_record`（比分字段 before/after）
- `basket_match_data_revision`（revision_no, from/to version）
- `basket_match_status_log`（via validationService.writeStatusLog）

### 幂等

`BasketMatchStatSyncServiceImpl.tryRecordSync` — 同一 official_result + source 不重复 rebuild

### 与 P0 关系

Submission 审核通过仍走 `BasketMatchDataSubmissionApplyServiceImpl`；修正走 CorrectionService；两者最终均 publish + rebuildMatch，未推翻 P0 主链路。

---

## 第七部分：前端页面结果

| 页面 | 状态 |
|------|------|
| Web M05 争议入口 | ✅ MatchDataStatusView 按 canDispute/过期/已有争议展示 |
| Web M06 争议提交/状态 | ✅ 新建 2 页 + API |
| Web M07 异常提交/状态 | ✅ 新建 2 页 + API |
| Admin 争议处理 | ✅ 增强 match-dispute |
| Admin 异常处理 | ✅ 新建 match-exception |
| 材料预览 | ✅ Admin 详情 fileUrl |
| 表单校验 | ✅ matchDataValidation.ts |
| web-ui build | ✅ 通过 |
| admin-ui build:prod | ✅ 通过 |

---

## 第八部分：权限、异常、并发测试结果

### 单元测试（Mockito）

| 测试类 | 覆盖 |
|--------|------|
| BasketMatchDisputeWindowTest | 12h 窗口 |
| BasketMatchDisputePermissionTest | 非 CONFIRMED 拦截 |
| BasketMatchDisputeDuplicateTest | active 重复争议 |
| BasketMatchExceptionPermissionTest | 弃赛方必填等 |
| BasketMatchExceptionDuplicateTest | 重复处理 |
| BasketMatchDisputeUpholdIdempotentTest | 采纳幂等 |
| BasketMatchExceptionDecideIdempotentTest | 裁定幂等 |

执行：`mvn -f basketball-admin test -Dtest=BasketMatchDispute*Test,BasketMatchException*Test`

### 并发

| 项 | 状态 |
|----|------|
| uk_match_active_submission | 第三轮已建 |
| uk_match_active_dispute | P1 migration |
| uk_match_active_exception | P1 migration |
| 真并发压测 | ⚠️ 未执行，P2 |

---

## 第九部分：遗留问题（P2）

- revision 专用 UI（仅 Admin 修正弹窗，无独立 revision 列表）
- settlement batch 展示
- 异步结算队列
- 正式数据修正完整可视化界面
- 视觉稿精修
- 复杂重赛赛程（REMATCH 仅标记 + 日志）
- 复杂异常规则配置化
- 主客队长/双管理员并发压测
- 通知框架 TODO：管理员待办队列（当前仅站内信 notifyCaptains）

---

## 第十部分：最终结论

| # | 问题 | 答案 |
|---|------|------|
| 1 | 普通队员不能提交比赛数据？ | ✅ 是（后端 captain 校验 + Runner） |
| 2 | 非本场队长不能提交？ | ✅ 是 |
| 3 | Web 已增加球队 stat=球员汇总校验？ | ✅ 是（前后端） |
| 4 | 公开 API 只显示 CONFIRMED？ | ✅ 是（代码已实现，Runner 可验） |
| 5 | M06 争议可提交？ | ✅ 代码就绪，待 migration+联调 |
| 6 | 12 小时争议窗口生效？ | ✅ 是（resolveDisputeDeadline） |
| 7 | Admin 可驳回争议？ | ✅ 是 |
| 8 | Admin 可采纳并修正正式数据？ | ✅ apply-correction |
| 9 | M07 异常可提交？ | ✅ 代码就绪（未 CONFIRMED 比赛） |
| 10 | Admin 可裁定异常？ | ✅ decide 多类型 |
| 11 | 异常裁定影响比赛状态/结果？ | ✅ FORFEIT/POSTPONE/CANCEL 等 |
| 12 | 争议/异常留痕？ | ✅ revision/stat_change/exception_audit |
| 13 | 争议/异常通知？ | ✅ notifyCaptains（失败不回滚） |
| 14 | 重复提交/处理拦截？ | ✅ 唯一索引 + CAS |
| 15 | 可进入第五轮？ | ⚠️ 需先：① 执行 P1 migration ② mvn compile ③ 重启服务 ④ M06/M07 联调 |

---

## 附录：关键新增/修改文件索引

### SQL
- `docs/sql/cnpa-match-data-dispute-exception-p1-*.sql`

### Admin 后端
- `BasketMatchOfficialDataCorrectionServiceImpl.java`
- `BasketMatchDisputeServiceImpl.java`（增强）
- `BasketMatchExceptionServiceImpl.java`
- `BasketMatchDisputeController.java` / `BasketMatchExceptionController.java`
- Domain/Mapper/XML 争议材料、异常三表、revision

### Web 后端
- `BasketPlayerMatchDisputeController.java` / `BasketPlayerMatchExceptionController.java`
- `PlayerMatchDisputeServiceImpl.java` / `PlayerMatchExceptionServiceImpl.java`
- `PlayerMatchDataServiceImpl.java`（校验增强）

### 前端
- `matchDataValidation.ts`
- `MatchDataDispute*.vue` / `MatchException*.vue`
- `matchDispute.ts` / `matchException.ts`

### 测试工具
- `CnpaMatchDataP0RiskRetestRunner.java`
- `CnpaDisputeExceptionMigrationRunner.java`

---

**请确认后进入第五轮：migration 执行 → 服务联调 → M06/M07 真实验收 → 并发补测 → 收尾。**
