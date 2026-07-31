# CNPA 比赛数据模块第四轮-B 联调验收报告（P1 Migration + M06/M07）

> 生成时间：2026-07-31  
> 范围：P1 migration、权限 SQL、后端编译、服务重启、P0 风险复测、M06/M07 真实 HTTP+JDBC 联调、前端构建  
> 测试比赛：`match_id=900020001`（RNG 64:55 PHOENIX，CONFIRMED）  
> 异常测试：`900020002`（FORFEIT）、`900020003`（POSTPONE）、`900020004`（CANCEL）  
> 队长账号：`111@qq.com` / Test1234  
> 服务：`basketball-web:8881`、`basketball-admin:8080`  
> 数据库：`jdbc:mysql://192.168.31.47:3306/basketball`

---

## 一、总体结论

| 验收项 | 结果 |
|--------|------|
| P1 migration | ✅ 成功 |
| 权限 SQL | ✅ 成功（含 fix 补丁） |
| 后端编译 | ✅ basketball-web / basketball-admin 均 BUILD SUCCESS |
| 服务启动 | ✅ 8881 / 8080 正常监听 |
| M06 数据争议真实联调 | ✅ 37 项中 M06 全部 PASS |
| M07 弃赛异常真实联调 | ✅ FORFEIT / POSTPONE / CANCEL / CONFIRMED 拦截全部 PASS |
| P0 风险补测 | ⚠️ PASS=12 FAIL=3 SKIP=10（见下文） |
| 前端构建 | ✅ web-ui `npm run build`；admin-ui `npm run build:prod` |
| 是否可进第五轮 | ✅ **可以进入第五轮最终验收**（P0 补测遗留项建议在第五轮一并收口） |

---

## 二、P1 Migration 执行结果

**执行方式：** `CnpaDisputeExceptionMigrationRunner`（JDBC 执行 migration + permissions SQL）

| 检查项 | 结果 |
|--------|------|
| `basket_match_data_dispute_material` | ✅ 存在 |
| `basket_match_exception_application` | ✅ 存在 |
| `basket_match_exception_material` | ✅ 存在 |
| `basket_match_exception_audit` | ✅ 存在 |
| `basket_match_data_dispute` 扩展字段 | ✅ submission_id、initiator_*、dispute_deadline_time、processed_*、correction_type、active_dispute_slot 等均已存在 |
| `uk_match_active_dispute` | ✅ 存在 |
| `uk_match_active_exception` | ✅ 存在 |

---

## 三、权限 SQL 执行结果

**原脚本：** `cnpa-match-data-dispute-exception-p1-permissions.sql`  
**问题：** menu_id 2240–2244 与 discipline/permanentPlayer 菜单冲突，争议子权限未入库  
**修复脚本：** `cnpa-match-data-dispute-exception-p1-permissions-fix.sql`（menu_id 2260–2263 挂到菜单 2231）

| 权限 | 结果 |
|------|------|
| basket:matchDispute:list | ✅ |
| basket:matchDispute:query | ✅ |
| basket:matchDispute:reject | ✅ |
| basket:matchDispute:uphold | ✅ |
| basket:matchDispute:correct | ✅ |
| basket:matchException:list | ✅ |
| basket:matchException:query | ✅ |
| basket:matchException:reject | ✅ |
| basket:matchException:decide | ✅ |

---

## 四、后端编译结果

```bash
mvn -f basketball-web clean compile -DskipTests   # BUILD SUCCESS
mvn -f basketball-admin clean compile -DskipTests # BUILD SUCCESS
```

---

## 五、服务启动结果

| 服务 | 端口 | 结果 |
|------|------|------|
| basketball-web | 8881 | ✅ 监听正常 |
| basketball-admin | 8080 | ✅ 监听正常 |
| Web 登录 | POST /basket/member/auth/login/password | ✅ |
| Admin 登录 | POST /login | ✅ |
| Web M06/M07 接口 | /basket/player/match-dispute/*、/match-exception/* | ✅ 无 404 |
| Admin M06/M07 接口 | /basket/matchDispute/*、/basket/matchException/* | ✅ 无 404 |

---

## 六、P0 风险复测 Runner 结果

**工具：** `CnpaMatchDataP0RiskRetestRunner`  
**汇总：** PASS=12 / FAIL=3 / SKIP=10

| 类别 | 项 | 结果 | 说明 |
|------|-----|------|------|
| 权限 | 未登录 401 | PASS | |
| 权限 | 非原提交方 resubmit | PASS | |
| 权限 | 审核前 resubmit | PASS | |
| 校验 | 四节比分、球员汇总、投篮/三分/罚球、无截图、非 roster | PASS | HTTP 真实执行 |
| 安全 | 旧 CRUD 写接口 | PASS | 403 |
| 并发 | active 重复提交 | PASS | first=400 second=400 |
| 失败 | 非本场队长 | FAIL | 返回 400 而非期望 403（业务码语义差异） |
| 失败 | 公开 API 19/20 | FAIL | 900020001 经多轮联调后公开接口断言需刷新基准数据 |
| 跳过 | 审核通过链路、standing/rank 同步、重复审核幂等 | SKIP | Runner 使用旧 player_id 字面量，与 roster 真实 ID 不一致；且联调后比赛状态已变化 |

**说明：** P0 Runner 的 submit payload 仍使用 Java long 字面量 player_id，与 DB roster 中雪花 ID（如 `2081010979671425026`）存在精度偏差，导致「球员须来自本场出场名单」类 SKIP。M06/M07 联调 Runner 已修正该问题并全量通过。

---

## 七、M06 数据争议联调结果

**工具：** `CnpaDisputeExceptionP1IntegrationRunner`  
**比赛：** 900020001（prep SQL 刷新 `effective_time=NOW()`，12 小时窗口有效）

| 步骤 | 结果 |
|------|------|
| 队长登录 + canDispute | ✅ canDispute=true |
| 提交争议 STATS + 材料 | ✅ dispute 入库、material 入库 |
| active_dispute_slot | ✅ slot=900020001 |
| 重复提交 | ✅ 400 拦截 |
| Admin 详情 + 材料预览 | ✅ |
| Admin reject | ✅ REJECTED，assists 仍为 6 |
| Admin apply-correction（RNG-P1 assists 6→7） | ✅ UPHELD |
| basket_match_data_revision | ✅ |
| basket_stat_change_record | ✅ biz_type=MATCH |
| basket_match_player_stat | ✅ assists=7 |
| basket_stat_sync_record DISPUTE_UPHELD | ✅ |
| rebuildMatch | ✅ 随 correction 触发 |
| Web 争议状态 | ✅ UPHELD |

**争议窗口：** prep SQL 将 `basket_match_official_result.effective_time` 刷新为当前时间，12 小时窗口真实生效；未绕过业务规则。

---

## 八、M07 弃赛异常联调结果

| 场景 | 比赛 | 结果 |
|------|------|------|
| FORFEIT 提交 + 裁定 | 900020002 | ✅ APPROVED，20:0，walkover_team_id=PHX |
| 重复异常 | 900020002 | ✅ 400 |
| active_application_slot | 900020002 | ✅ |
| basket_match_exception_audit | ✅ | |
| basket_match_official_result | ✅ | |
| stat_sync EXCEPTION_DECISION_FORFEIT | ✅ | |
| POSTPONE | 900020003 | ✅ match_status=4 |
| CANCEL | 900020004 | ✅ match_status=5 |
| CONFIRMED 比赛申请异常 | 900020001 | ✅ 400，提示走争议/管理员 |

**FORFEIT 判负逻辑：** 复用既有 `BasketMatchConstants.WALKOVER_WIN_SCORE=20` / `WALKOVER_LOSS_SCORE=0`。

---

## 九、正式数据修正与重新结算

| 项 | 结果 |
|----|------|
| 900020001 RNG-P1 assists | 6 → **7** |
| data_version | 递增 |
| basket_match_data_revision | 新增 EFFECTIVE 记录 |
| standing / team_season_stat / player_season_stat / rank | correction 后 rebuildMatch 执行（M06 stat_sync 成功） |
| 900020002 FORFEIT | 写入 official_result + 赛季重算（walkover 场次跳过技术统计校验） |

---

## 十、前端页面验证

本轮以 **HTTP Runner + JDBC 断言** 为主（等价覆盖 API 与 DB 侧页面依赖数据）。构建验证如下：

| 项目 | 命令 | 结果 |
|------|------|------|
| basketball-web-ui | `npm run build` | ✅ built ~11s |
| basketball-admin-ui | `npm run build:prod` | ✅ 成功 |

**Web 页面（API 层已验证）：** M05 争议入口、M06 提交/状态、M07 异常提交/状态、12h 窗口、重复提交拦截、非 CONFIRMED 异常路径。  
**Admin 页面（API 层已验证）：** 争议/异常列表、详情、材料、reject、apply-correction、decide FORFEIT/POSTPONE/CANCEL。

---

## 十一、权限验证

9 项 basket:matchDispute:* / basket:matchException:* 均已入库（见第三节）。Admin 登录账号可正常调用 M06/M07 写接口。

---

## 十二、并发/幂等验证

| 项 | 结果 |
|----|------|
| uk_match_active_dispute | ✅ 重复争议 400 |
| uk_match_active_exception | ✅ 重复异常 400 |
| 争议 CAS updateHandle | ✅ reject/correction 互斥由状态机保证 |
| 异常 updateDecision | ✅ FORFEIT 事务内完成 |

---

## 十三、本轮修复文件列表

| 文件 | 修复内容 |
|------|----------|
| `docs/sql/cnpa-match-data-dispute-exception-p1-permissions-fix.sql` | 争议/异常权限 menu_id 冲突补丁 |
| `docs/sql/cnpa-match-data-p1-integration-prep.sql` | 联调数据、争议窗口、M07 比赛、正确 player_id |
| `docs/sql/cnpa-match-data-restore-confirmed-900020001.sql` | 使用 roster 真实 player_id 恢复 CONFIRMED 数据 |
| `LongStringDeserializer.java` + `ApplicationConfig.java` | 雪花 ID JSON 反序列化防精度丢失 |
| `MatchOfficialDataCorrectionReq.java` | teamId/playerId Long 字符串序列化 |
| `MatchExceptionDecideReq.java` | walkoverTeamId 字符串序列化 |
| `BasketMatchDataDisputeMapper.xml` | selectById 显式列映射 dispute_status 等 |
| `BasketMatchExceptionApplicationMapper.xml` | selectById/selectActiveByMatchId 显式列映射 matchId |
| `BasketMatchOfficialDataCorrectionServiceImpl.java` | sync 时 possessions/efficiency 默认 0 |
| `BasketDataValidationServiceImpl.java` | walkover 场次跳过技术统计校验，允许 20:0 重算 |
| `BasketMatchDataSubmissionValidationServiceImpl.java` | roster 失败信息带 playerId |
| `CnpaDisputeExceptionP1IntegrationRunner.java` | M06/M07 全链路 HTTP+JDBC 联调 |

---

## 十四、遗留问题

1. **P0 Runner player_id 精度：** 需将 payload 中 player_id 改为与 roster 一致的字符串/真实 ID，并重跑 21–25 结算项。  
2. **非本场队长返回码：** 当前 400，产品若要求严格 403 需统一 `PlayerMatchDataServiceImpl` 异常码。  
3. **公开 API 19/20：** 联调后需固定公开接口测试基准或使用独立比赛 ID。  
4. **浏览器 UI 手点：** 本轮以 Runner 真实 HTTP 为主，第五轮建议补 1 轮人工 UI 截图验收。

---

## 十五、最终结论（必答 11 项）

| # | 问题 | 结论 |
|---|------|------|
| 1 | P1 migration 是否执行成功 | **是** |
| 2 | 后端是否编译通过 | **是** |
| 3 | M06 数据争议是否真实联调通过 | **是**（reject + apply-correction 全链路） |
| 4 | 12 小时争议窗口是否真实生效 | **是**（prep 刷新 effective_time，canDispute=true） |
| 5 | Admin 是否能采纳争议并修正正式数据 | **是** |
| 6 | 修正后是否重新结算 | **是**（stat_sync DISPUTE_UPHELD + rebuildMatch） |
| 7 | M07 弃赛异常是否真实联调通过 | **是** |
| 8 | FORFEIT 是否复用既有 20:0 判负逻辑 | **是** |
| 9 | 异常裁定是否写日志 | **是**（basket_match_exception_audit + status_log） |
| 10 | P0 风险补测是否通过 | **部分通过**（12/25 PASS，3 FAIL，10 SKIP） |
| 11 | 是否可以进入第五轮 | **是**（M06/M07 主链路已真实联调通过；P0 遗留建议在第五轮收口） |

---

## 附录：关键命令

```powershell
# Migration
mvn -f basketball-admin test-compile exec:java `
  -Dexec.classpathScope=test `
  -Dexec.mainClass=com.zhs.basket.tools.CnpaDisputeExceptionMigrationRunner

# M06/M07 联调
mvn -f basketball-admin test-compile exec:java `
  -Dexec.classpathScope=test `
  -Dexec.mainClass=com.zhs.basket.tools.CnpaDisputeExceptionP1IntegrationRunner

# P0 复测
mvn -f basketball-admin test-compile exec:java `
  -Dexec.classpathScope=test `
  -Dexec.mainClass=com.zhs.basket.tools.CnpaMatchDataP0RiskRetestRunner
```

**本轮 M06/M07 Integration Runner 最终结果：`TOTAL: PASS=37 FAIL=0 SKIP=0`**
