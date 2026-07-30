# CNPA 比赛数据 Submission P0 实现报告（第二批）

> 完成日期：2026-07-31  
> 范围：`basketball-web` / `basketball-admin` / `basketball-web-ui` / `basketball-admin-ui` / `docs/sql`  
> 目标：队长 Web 提交 → 管理员审核 submission → 审核通过后同步正式数据

---

## 第一部分：实现总览

| 功能 | 已实现 | 部分实现 | 未实现 | 涉及文件 | 测试结果 | 风险 |
|------|--------|----------|--------|----------|----------|------|
| submission 主表 | ✓ | | | `docs/sql/cnpa-match-data-submission-p0-migration.sql` | 需手动执行 SQL | 未执行迁移前接口不可用 |
| period 子表 | ✓ | | | 同上 + Admin/Web Model/Mapper | — | 低 |
| team_stat 子表 | ✓ | | | 同上 | — | 低 |
| player_stat 子表 | ✓ | | | 同上 | — | 低 |
| material 子表 | ✓ | | | 同上 | — | 低 |
| audit 写入 | ✓ | | | `PlayerMatchDataServiceImpl` / `BasketMatchDataAuditLogSupport` | 单元测试未覆盖 | 低 |
| 队长提交接口 | ✓ | | | `BasketPlayerMatchDataController` | 未做集成测试 | 中 |
| 我的比赛接口 | ✓ | | | 同上 | 未做集成测试 | 低 |
| 提交状态接口 | ✓ | | | 同上 | 未做集成测试 | 低 |
| 驳回重提接口 | ✓ | | | 同上 | 未做集成测试 | 中 |
| Admin 审核列表 | ✓ | | | `BasketMatchDataAuditController` | 未做集成测试 | 低 |
| Admin 审核详情 | ✓ | | | `BasketMatchDataSubmissionAuditServiceImpl` | 未做集成测试 | 低 |
| Admin 审核通过 | ✓ | | | 同上 + `BasketMatchDataSubmissionApplyServiceImpl` | 未做集成测试 | 中 |
| Admin 审核驳回 | ✓ | | | `BasketMatchDataSubmissionAuditServiceImpl` | 未做集成测试 | 低 |
| 正式数据同步 | ✓ | | | `BasketMatchDataSubmissionApplyServiceImpl` | 未做集成测试 | 中 |
| 积分榜同步 | ✓ | | | 复用 `IBasketDataSettlementService.rebuildMatch` | 未做集成测试 | 低 |
| 统计同步 | ✓ | | | 复用 `IBasketMatchStatSyncService` | 未做集成测试 | 低 |
| 排行榜同步 | ✓ | | | settlement rebuild | 未做集成测试 | 低 |
| 一方提交锁 | ✓ | | | `uk_match_active_submission` + `DuplicateKeyException` | 未做并发测试 | 中 |
| 重复提交防护 | ✓ | | | match 行锁 + active slot | 未做并发测试 | 中 |
| 重复审核防护 | ✓ | | | `updateStatusIfMatch` 条件更新 | 未做并发测试 | 中 |
| Web 裸 CRUD 防护 | ✓ | | | `GeneratedCrudAccessInterceptor` | 未做接口测试 | 低 |
| Web UI 接入 | ✓ | | | `MatchData*.vue` + 路由 | `npm run build` 通过 | 提交页为简化表单 |
| Admin UI 接入 | ✓ | | | `match-data-audit/index.vue` | `npm run build:prod` 通过 | 低 |

**语义说明（重要）：**  
`basket_match.home_team_id` / `away_team_id` 保存的是**长期球队 ID（teamId）**，不是 EventTeam ID。队长校验基于 `basket_tournament_registration.captain_member_id` + 赛季 roster 归属。

---

## 第二部分：SQL 修改

### 新增表

| 表名 | 用途 |
|------|------|
| `basket_match_data_submission` | 提交主表，含 `active_submission_slot` 生成列 |
| `basket_match_data_submission_period` | 分节比分 |
| `basket_match_data_submission_team_stat` | 球队统计 |
| `basket_match_data_submission_player_stat` | 球员统计 |
| `basket_match_data_submission_material` | 材料（file_id 关联） |

### 扩展字段（`basket_match_data_audit`）

- `submission_id`
- `audit_action`（SUBMIT / RESUBMIT / APPROVE / REJECT 等）
- `before_status` / `after_status`
- `auditor_name`

### 索引与约束

- `uk_match_active_submission(active_submission_slot)`：同一比赛仅一条 status IN (1,2) 且未删除的提交
- `uk_match_submission_version(match_id, version_no, is_delete)`
- 各子表 `(submission_id, …)` 唯一键

### 脚本

| 文件 | 说明 |
|------|------|
| `docs/sql/cnpa-match-data-submission-p0-migration.sql` | 正向迁移 |
| `docs/sql/cnpa-match-data-submission-p0-rollback.sql` | 回滚（DROP 新表 + 移除 audit 扩展列） |

### 部署

- **需要手动执行** migration SQL（MySQL 8.0+）
- **不影响历史正式数据**：仅新增表与 audit 列扩展，不修改已有 match/stat 行
- 执行前请备份；rollback 会删除 submission 层全部数据

---

## 第三部分：后端修改清单

### basketball-web

| 文件 | 类/方法 | 接口路径 | 功能 | 权限 | 事务 | 幂等 | 校验 |
|------|---------|----------|------|------|------|------|------|
| `controller/BasketPlayerMatchDataController.java` | 6 个 API | `/basket/player/match-data/*` | 队长提交全流程 | 登录 + 认证球员 | submit/resubmit `@Transactional` | active slot 唯一 | Service 层完整校验 |
| `service/impl/PlayerMatchDataServiceImpl.java` | `pageMyMatches` | GET `/page`, `/my-matches` | 我的比赛 | 赛季 roster 成员 | 只读 | — | 过滤非相关比赛 |
| | `getSubmitDetail` | GET `/{matchId}/submit-detail` | 提交页初始化 | 队长 | 只读 | — | 已结束、无 active submission |
| | `submit` | POST `/{matchId}/submit` | 首次提交 | 队长 | ✓ | slot + 行锁 | 比分/分节/roster/材料 |
| | `getStatus` | GET `/{matchId}/status` | 审核状态 | 双方成员 | 只读 | — | 成员归属 |
| | `getResubmitDetail` | POST `/{matchId}/resubmit-detail` | 驳回预填 | 原提交方队长 | 只读 | — | REJECTED 态 |
| | `resubmit` | POST `/{matchId}/resubmit` | 重提 | 原提交方队长 | ✓ | version_no+1 | 同上 |
| `config/GeneratedCrudAccessInterceptor.java` | — | — | 白名单 `/basket/player/match-data/`；拦截生成式 CRUD 写接口 | 403 | — | — | — |
| `model/BasketMatchDataSubmission*.java` + `dao/*` | — | — | submission 层 ORM | — | — | — | — |
| `dao/BasketMatchDao` | `selectByIdForUpdate` / `updateDataStatusIfAllowed` | — | 并发控制 | — | — | CAS 更新 | — |

**submitter 信息：** 本批已改为**后端根据 token + registration 队长身份计算** `submitterTeamId`，不再信任前端传入。

### basketball-admin

| 文件 | 类/方法 | 接口路径 | 功能 | 权限 | 事务 | 幂等 | 校验 |
|------|---------|----------|------|------|------|------|------|
| `web/controller/basket/BasketMatchDataAuditController.java` | submission 审核 API | `/basket/matchAudit/submission/*` | 审核列表/详情/通过/驳回/版本 | `@PreAuthorize basket:matchAudit:*` | approve/reject `@Transactional` | 条件更新 status | 二次校验 |
| `service/impl/BasketMatchDataSubmissionAuditServiceImpl.java` | `pageSubmissions` / `getDetail` / `approve` / `reject` / `listVersions` | 见上 | 审核主流程 | 管理员 | ✓ | 重复审核失败 | validation 服务 |
| `service/impl/BasketMatchDataSubmissionApplyServiceImpl.java` | `applyApprovedSubmissionToOfficialData` | 内部 | 同步正式表 + official_result + statSync + rebuildMatch | 系统/审核人 | ✓ | 同一 submission 同事务一次 | 覆盖非叠加 |
| `service/impl/BasketMatchDataSubmissionValidationServiceImpl.java` | `validateSubmissionData` | 内部 | 审核前数据一致性 | — | — | — | 分节/球队/球员/材料 |
| `support/BasketMatchDataAuditLogSupport.java` | 写 audit | — | SUBMIT/RESUBMIT/APPROVE/REJECT | — | 同事务 | — | — |
| `enums/MatchDataSubmissionStatusEnum.java` | SUBMITTED(1) 等 | — | 状态枚举 | — | — | — | — |
| `enums/MatchDataAuditActionEnum.java` | SUBMIT/APPROVE/… | — | 审核动作 | — | — | — | — |

**旧 Admin 录入接口保留**，不影响 submission 审核流；新审核列表优先查 submission 表。

---

## 第四部分：前端修改清单

### basketball-web-ui

| 页面/文件 | 路由 | API | Mock | TODO |
|-----------|------|-----|------|------|
| `src/api/matchData.ts` | — | 6 个接口 | 已去除 | — |
| `MatchDataListView.vue` | `.../team/match-data` | `pageMyMatches` | 真实接口 | 分页 UI 可精修 |
| `MatchDataSubmitView.vue` | `.../submit`, `.../resubmit` | submit/resubmit | 真实接口 | 材料上传仍为 fileId 手填；球队/球员 stat 表单简化 |
| `MatchDataStatusView.vue` | `.../status` | `getMatchDataStatus` | 真实接口 | 步骤条可精修 |
| `EventTeamManagementView.vue` | — | — | **已去除 mockOnly** | — |
| `stores/eventTeam.ts` | — | `pageMyMatches` 统计待提交 | 真实 | — |
| `router/index.ts` | 4 条 match-data 路由 | — | — | — |

### basketball-admin-ui

| 页面/文件 | 路由 | API | Mock | TODO |
|-----------|------|-----|------|------|
| `src/api/basket/matchAudit.ts` | — | submission 审核 API | 真实 | — |
| `views/cnpa/competition/match-data-audit/index.vue` | `/competition/match-data-audit` | 列表+详情弹窗+通过/驳回 | 真实 | 详情 UI 可精修 |

---

## 第五部分：核心流程验证

| 场景 | 代码实现 | 自动化测试 | 说明 |
|------|----------|------------|------|
| 队长提交 | ✓ | 未执行 | 写 submission 层 + audit SUBMIT + match.data_status=SUBMITTED |
| 普通队员禁止 | ✓ | 未执行 | `findCaptainTeamIdsForMatch` 为空则 403 |
| 一方提交锁定 | ✓ | 未执行 | `uk_match_active_submission` + 捕获 DuplicateKey |
| 审核中不可重复提交 | ✓ | 未执行 | `assertNoActiveSubmission` |
| 驳回版本保留 | ✓ | 未执行 | REJECTED 不删；重提新建 version，旧版可标 SUPERSEDED |
| 驳回后重提 | ✓ | 未执行 | version_no+1 + audit RESUBMIT |
| 审核通过同步正式数据 | ✓ | 未执行 | ApplyService 覆盖正式表 |
| 公开接口过滤 | ✓（既有） | 未执行 | `BasketPublicMatchServiceImpl` 仍按 data_status=CONFIRMED |
| 重复审核幂等 | ✓ | 未执行 | 条件更新 submission_status |
| 旧 CRUD 防护 | ✓ | 未执行 | Interceptor 403 生成式写接口 |

### 单元测试

已新增：

- `basketball-admin/src/test/java/.../BasketMatchDataSubmissionValidationServiceImplTest.java`  
  - 校验通过 / 分节合计不匹配 / 缺少截图

**未执行原因：** 当前环境无 `mvn` 命令，未能运行 Maven 测试套件。

---

## 第六部分：构建结果

| 项目 | 命令 | 结果 |
|------|------|------|
| basketball-web-ui | `npm run build` | **通过** |
| basketball-admin-ui | `npm run build:prod` | **通过** |
| basketball-web | `mvn compile` | **未执行**（环境无 Maven） |
| basketball-admin | `mvn test` | **未执行**（环境无 Maven） |

---

## 第七部分：遗留问题（P1/P2）

### P1

- M06 数据争议 Web 端完整流程
- 12 小时争议窗口
- M07 弃赛异常申请
- 争议采纳后修正正式数据
- 异常裁定后同步比分和积分
- Web 提交页材料上传组件（替换 fileId 手填）
- 完整集成/并发测试（12 项测试场景）
- Admin 菜单权限 `basket:matchAudit:approve` / `query` 若 batch5 未含需补 SQL

### P2

- revision 专用 UI
- settlement batch 历史展示
- 异步结算任务队列
- 正式数据修正完整界面
- M01–M05 视觉稿精修

---

## 第八部分：最终结论

| # | 问题 | 结论 |
|---|------|------|
| 1 | 是否已新增 submission 审核层？ | **是** — 5 张 submission 表 + audit 扩展 |
| 2 | 队长提交是否不再写正式统计表？ | **是** — 仅写 submission 层；正式表由 ApplyService 在审核通过后写入 |
| 3 | 一场比赛是否只能一方提交当前有效数据？ | **是** — `active_submission_slot` 唯一约束 |
| 4 | 普通队员是否无法提交？ | **是** — 队长校验基于 registration.captainMemberId |
| 5 | 审核驳回后旧版本是否保留？ | **是** — REJECTED 记录保留；重提时旧版可标 SUPERSEDED |
| 6 | 重新提交是否生成新版本？ | **是** — version_no 递增 |
| 7 | 审核通过是否同步正式数据？ | **是** — ApplyService 覆盖 match/period/team_stat/player_stat |
| 8 | 审核通过是否触发已有统计结算？ | **是** — 复用 officialResult + statSync + rebuildMatch |
| 9 | 官网是否只展示审核通过数据？ | **是** — 沿用既有 CONFIRMED 公开过滤 |
| 10 | 是否可进入第三批（页面精修 + M06/M07）？ | **可以** — 前提是先执行 migration SQL 并完成联调验证 |

---

## 部署检查清单

1. 执行 `docs/sql/cnpa-match-data-submission-p0-migration.sql`
2. 重启 `basketball-web`、`basketball-admin`
3. 确认 Admin 角色具备 `basket:matchAudit:query` / `approve` 权限
4. 联调：队长提交 → Admin 审核通过 → 公开页可查数据
5. 联调：驳回 → 重提 → 版本历史可见

**本批已完成 P0 主链路开发，未自动启动第三批，等待确认。**
