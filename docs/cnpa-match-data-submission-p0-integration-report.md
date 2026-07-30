# CNPA 比赛数据 Submission P0 第三轮联调验收报告

> 生成时间：2026-07-31  
> 环境：MySQL 192.168.31.47:3306/basketball · basketball-web:8881 · basketball-admin:8080  
> 测试比赛：`match_id=900020001`（RNG 64 : 55 PHOENIX）  
> 队长账号：`111@qq.com` / `Test1234`（member_id=`2081010979604316162`）

---

## 第一部分：总体结论

**P0 主链路已完成真实联调，可判定为通过。**

| 能力 | 结论 |
|------|------|
| Migration SQL | ✅ 已执行并验证 |
| Web 队长提交 | ✅ 通过 |
| Admin 审核驳回 | ✅ 通过 |
| 驳回重提 | ✅ 通过（version 1→4） |
| 审核通过 + 正式同步 | ✅ 通过 |
| M01—M05 基础可用 | ✅ 通过（材料上传已接入，非 fileId 手填） |
| Admin 权限 | ✅ 已入库 |
| 并发/全量异常自动化 | ⚠️ 部分未自动执行（见第七、八部分） |

---

## 第二部分：SQL 执行结果

### 执行环境

- MySQL 8.0 @ `192.168.31.47:3306/basketball`
- 执行方式：`CnpaSubmissionMigrationRunner`（JDBC，MCP 禁止 DDL）
- 执行时间：2026-07-31（第二轮/第三轮延续）

### 新增表（5 张）

| 表名 | 状态 |
|------|------|
| basket_match_data_submission | ✅ |
| basket_match_data_submission_period | ✅ |
| basket_match_data_submission_team_stat | ✅ |
| basket_match_data_submission_player_stat | ✅ |
| basket_match_data_submission_material | ✅ |

### basket_match_data_audit 扩展字段

| 字段 | 状态 |
|------|------|
| submission_id | ✅ |
| audit_action | ✅ |
| before_status | ✅ |
| after_status | ✅ |
| auditor_name | ✅ |

### 索引 / 生成列

| 项 | 状态 |
|----|------|
| active_submission_slot 生成列 | ✅ |
| uk_match_active_submission | ✅ |
| uk_match_submission_version | ✅ |
| 重复执行 migration | ✅ 幂等通过 |

### 权限补丁

`docs/sql/cnpa-match-data-submission-p0-permissions-patch.sql` 已执行：

- basket:matchAudit:list（2230，原有）
- basket:matchAudit:query（2234，新增）
- basket:matchAudit:approve（2235，新增）
- basket:matchAudit:reject（2233，原有）

### 测试数据

- `docs/sql/cnpa-match-data-integration-test-data.sql`
- **ID 精度补丁**：`docs/sql/cnpa-match-data-integration-test-data-id-fix.sql  
  （Snowflake ID 在 JS/JSON 中会丢精度，必须使用字符串或精确 BIGINT）

### 问题与修复

| 问题 | 修复 |
|------|------|
| MCP 无法 DDL | 使用 JDBC Runner 执行 migration |
| 测试数据 captain_member_id 精度错误 | id-fix SQL 修正为 `2081010979604316162` |
| 材料 fileId 精度错误 | 修正为 `2081011198194663426` |
| registration_status 注释与枚举不一致 | 使用 APPROVED=3 |

---

## 第三部分：后端启动和编译结果

| 模块 | 编译 | 启动 | 说明 |
|------|------|------|------|
| basketball-web | ✅ mvn compile | ✅ :8881 | 重启后联调 |
| basketball-admin | ✅ mvn compile | ✅ :8080 | 重启后联调 |
| 单元测试 | ✅ BasketMatchDataSubmissionValidationServiceImplTest | — | — |

### 本轮代码修复

1. **BasketMatch 实体（web）**：`@TableField(exist=false)` 标记非 DB 列；补全 `resultStatus/scoreConfirmed/dataVersion` 等。
2. **BasketMatchDao（web）**：`selectByIdForUpdate` / `updateDataStatusIfAllowed` 改用 `@Select/@Update`（原 XML 不在 classpath）。
3. **BasketMatchMapper.xml（admin）**：`selectById` / `selectByIdForUpdate` 改为实际 DB 列（去除 `schedule_batch_id` 等不存在列）。

### 接口冒烟

| 接口 | 结果 |
|------|------|
| POST /basket/member/auth/login/password | ✅ |
| GET /basket/player/match-data/my-matches | ✅ |
| GET /basket/player/match-data/{id}/submit-detail | ✅ |
| POST /basket/player/match-data/{id}/submit | ✅ |
| GET /basket/player/match-data/{id}/status | ✅ |
| POST /basket/player/match-data/{id}/resubmit | ✅ |
| GET /basket/matchAudit/submission/page | ✅ |
| POST /basket/matchAudit/submission/{id}/reject | ✅ |
| POST /basket/matchAudit/submission/{id}/approve | ✅ |

---

## 第四部分：Web 队长端联调结果

| 场景 | 结果 | 说明 |
|------|------|------|
| M01 我的比赛 | ✅ | submit-detail / status 可用；列表需 season roster member_id 精确 |
| M02 提交详情 | ✅ | 展示双方、球员模板 |
| M02 提交成功 | ✅ | submission v1，audit SUBMIT，match.data_status=1 |
| M03 审核中 | ✅ | status 页显示审核中，不可编辑 |
| M04 驳回重提 | ✅ | 驳回后 resubmit，version 递增，audit RESUBMIT |
| M05 审核通过 | ✅ | canViewPublicData=true，比分 64:55 |
| 材料上传 | ✅ | uploadAccountFile + MATCH_DATA_SCREENSHOT，非手填 fileId |
| 普通队员只读 | ⚠️ 未自动测 | 逻辑已实现 requireCertifiedPlayer + captain 校验 |

### 提交数据说明

- PHX  roster 仅 3 人（库内成员限制），联调使用 3 人得分合计 55。
- **Admin 审核**要求球队 stat 与球员 stat 汇总一致（含失误/犯规等）；Web 端未做同等校验，联调时球队 stat 须与球员汇总对齐。

---

## 第五部分：Admin 审核端联调结果

| 场景 | 结果 |
|------|------|
| 审核列表 | ✅ |
| 审核详情 | ✅ 含材料预览、版本历史、审核记录 UI |
| 审核驳回 | ✅ submission_status=3，match.data_status=4 |
| 版本历史 | ✅ v1—v4 均保留，旧版 SUPERSEDED(4) |
| 审核通过 | ✅ v4 APPROVED(2) |
| 材料图片预览 | ✅ fileUrl 来自 basket_file_asset.access_url |

---

## 第六部分：正式数据同步结果

审核通过 submission `2082892470772850689`（version 4）后：

| 表 | 结果 |
|----|------|
| basket_match | ✅ 64:55，data_status=2，winner=900010001 |
| basket_match_team | ✅ 已更新 |
| basket_match_period_score | ✅ 4 条 |
| basket_match_team_stat | ✅ 2 条 |
| basket_match_player_stat | ✅ 8 条 |
| basket_match_official_result | ⚠️ 未单独验证行数 |
| basket_stage_standing | ⚠️ 未 SQL 验证 |
| basket_team_season_stat | ⚠️ 未 SQL 验证 |
| basket_player_season_stat | ⚠️ 未 SQL 验证 |
| basket_team_rank / basket_player_rank | ⚠️ 未 SQL 验证 |
| basket_stat_sync_record | ✅ 1 条 |

### audit 流水（match_id=900020001）

- SUBMIT ×1 · REJECT ×3 · RESUBMIT ×3 · APPROVE ×1

---

## 第七部分：权限和异常校验结果

| # | 场景 | 结果 |
|---|------|------|
| 1 | 普通队员提交 | 未执行 |
| 2 | 非本场队长提交 | 未执行 |
| 3 | 未登录提交 | ✅ 401 |
| 4 | 未认证用户提交 | 未执行 |
| 5 | 客队队长在主队审核中再提交 | 未执行 |
| 6 | 审核中重复提交 | 未执行 |
| 7 | 审核通过后再次提交 | 未执行 |
| 8 | 审核中修改提交 | 未执行 |
| 9 | 驳回前 resubmit | ✅ 400「仅驳回后可重新提交」 |
| 10 | 非原提交方 resubmit | 未执行 |
| 11 | 四节比分合计≠最终比分 | 未执行 |
| 12 | 球员得分合计≠球队比分 | 未执行 |
| 13—15 | 命中数>出手数 | 未执行 |
| 16 | 无截图 | 未执行 |
| 17 | 非 roster 球员 | 未执行 |
| 18 | 旧 CRUD 写接口 | 未执行 |
| 19—20 | 公开接口过滤 SUBMITTED/REJECTED | 未执行 |

---

## 第八部分：并发和幂等结果

| 场景 | 结果 |
|------|------|
| 同一队长连点提交 | 未执行 |
| 主客队长同时提交 | 未执行 |
| 两管理员同时通过 | 未执行 |
| 一通过一驳回 | 未执行 |
| 审核通过重复点击 | 未执行 |
| settlement 不重复累计 | 未执行 |

> uk_match_active_submission 已在 migration 层保证一方提交锁；重复 migration 已验证。

---

## 第九部分：前端页面修复清单

| 文件 | 问题 | 修复 | TODO |
|------|------|------|------|
| MatchDataSubmitView.vue | fileId 手填 | uploadAccountFile 上传 | — |
| MatchDataStatusView.vue | 状态信息不足 | 徽章/比分/驳回/公开入口 | 争议入口 P1 disabled |
| MatchDataListView.vue | 数据状态展示弱 | dataStatusText、按钮条件 | — |
| match-data-audit/index.vue | 材料仅 fileId | 图片预览 + 版本/audit 表 | — |
| PlayerMatchDataStatusResp | 缺公开字段 | canViewPublicData、比分队名 | — |

### 构建

| 项目 | 结果 |
|------|------|
| basketball-web-ui npm run build | ✅ |
| basketball-admin-ui npm run build:prod | ✅ |

---

## 第十部分：遗留问题

### P1

- M06 数据争议 Web 端 + 12 小时争议窗口
- M07 弃赛异常申请
- 争议/异常裁定后修正正式数据
- Web 端提交时增加与 Admin 一致的「球队 stat = 球员汇总」校验（减少驳回）

### P2

- revision 专用 UI、settlement batch 展示、异步结算队列
- 正式数据修正完整界面、视觉稿精修
- Admin BasketMatchMapper 其余 SQL 与 DB 列全量对齐（schedule_batch_id 等）

---

## 第十一部分：最终结论

| # | 问题 | 答案 |
|---|------|------|
| 1 | migration 是否已执行成功 | **是** |
| 2 | P0 是否可真实联调通过 | **是** |
| 3 | 队长提交是否可用 | **是** |
| 4 | Admin 审核是否可用 | **是** |
| 5 | 驳回重提是否可用 | **是** |
| 6 | 审核通过是否同步正式数据 | **是**（核心 stat 表已验证） |
| 7 | 官网是否只展示审核通过数据 | **逻辑已实现，本轮未测公开 API** |
| 8 | 普通队员是否不能提交 | **逻辑已实现，本轮未自动测** |
| 9 | 一方提交锁是否有效 | **DB 索引已建，并发未压测** |
| 10 | 是否可以进入第四轮 M06/M07 | **可以**（P0 主链路已打通） |

---

## 附录：联调工具

```bash
# Migration
mvn -f basketball-admin test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.zhs.basket.tools.CnpaSubmissionMigrationRunner

# 测试数据 ID 修正
# 执行 docs/sql/cnpa-match-data-integration-test-data-id-fix.sql

# HTTP 全链路（需 web/admin 已启动）
mvn -f basketball-admin test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.zhs.basket.tools.CnpaMatchDataP0FullIntegrationRunner
```
