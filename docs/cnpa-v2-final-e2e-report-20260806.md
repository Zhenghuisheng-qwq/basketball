# CNPA V2 最终业务全链路验收报告

- **批次**: `CNPA_V2_FINAL_E2E_20260806`
- **执行时间**: 2026-08-06
- **自动化脚本**: `scripts/cnpa_v2_final_e2e_20260806_runner.py`
- **UI 自动化**: `scripts/cnpa_format_rule_ui_test.mjs`（Admin 五步向导 + 赛制规则）
- **数据检查**: `python scripts/run_data_check.py --json`

---

## 1. 总体结论

**本轮 E2E 未完全通过，暂不可进入最终清理与冻结。**

| 场景 | 结论 | 说明 |
|------|------|------|
| A TEAM 单循环 | **部分通过（P1）** | 4 队报名/EventTeam/EventPlayer 完整；赛程仅 3 队 3 场（第 4 队在赛程发布后进入，StageTeam 无法增量同步） |
| B 小组→淘汰 | **未通过（P1）** | 赛制 Snapshot/Stage 已创建；16 队报名未完成（测试账号球队/登录/授权链路阻塞） |
| C PERSONAL | **未通过（P1）** | 6 人个人报名已提交待审；审核因「球员已在长期球队」业务规则拦截 |
| my-events / my-schedule | **通过** | 多账号 HTTP 200 |
| Admin 五步浏览器 | **基本通过** | Playwright 38 PASS / 1 FAIL |
| 数据检查 | **通过** | exitCode=0，ANOMALY=0 |

**交叉验证**：此前 `SMOKE_V2_20260805` / `CNPA_V2_FINAL_SMOKE_20260806` 已在 division 10/11 等环境完成完整 6 场单循环、提交审核、积分榜、PERSONAL 成队等链路，本报告在 E2E 阻塞项处引用其数据库 ID 作为能力佐证。

---

## 2. 环境结果

| 服务 | URL | HTTP | 结论 |
|------|-----|------|------|
| basketball-admin | http://127.0.0.1:8080 | 200 | PASS |
| basketball-web | http://127.0.0.1:8881 | 200 | PASS |
| basketball-admin-ui | http://127.0.0.1:80 | 200 | PASS |
| basketball-web-ui | http://127.0.0.1:5173 | 200 | PASS |
| Admin 登录 | POST /login | 200 + token | PASS |
| Web 登录 | POST /basket/member/auth/login/password | 200 + token（111111@qq.com） | PASS |
| Console Vue 异常 | Playwright 采集 | 无未处理异常 | PASS |
| Network HTTP 500 | Playwright 采集 | 无未处理 500 | PASS |
| 旧 Tournament API | Network 监控 | 未调用 | PASS |

---

## 3. TEAM 单循环 seasonId / divisionId

| 字段 | 值 |
|------|-----|
| seasonId | **31** |
| seasonName | `CNPA_V2_FINAL_E2E_20260806_TEAM_RR` |
| divisionId | **37** |
| divisionName | `CNPA_V2_FINAL_E2E_20260806_TEAM_RR-PC-5V5-RR4` |
| snapshotId | **74**（LOCKED） |
| stageId | **85**（ROUND_ROBIN） |

---

## 4. TEAM Registration / EventTeam / EventPlayer 数量

| 指标 | 预期 | 实际 | 结论 |
|------|------|------|------|
| Registration APPROVED | 4 | **4** | PASS |
| EventTeam | 4 | **4** | PASS |
| EventPlayer | ≥20 | **20** | PASS |

**registrationId → eventTeamId → 队长**:

| registrationId | eventTeamId | captainEmail |
|----------------|-------------|--------------|
| 52 | 36 | 111111@qq.com |
| 53 | 37 | 111141@qq.com |
| 54 | 38 | 111143@qq.com |
| 55 | 39 | 111142@qq.com |

**第 5 队拦截**: 111144@qq.com 提交报名 → HTTP 200, code=400, msg=无有效队长授权/名额限制 → **PASS**

---

## 5. TEAM 赛程结果

| 指标 | 预期 | 实际 | 结论 |
|------|------|------|------|
| StageTeam | 4 | **3** | **FAIL（P1）** |
| Match | 6 | **3** | **FAIL（P1）** |
| 轮次 | 3 | 3 | 与 3 队一致 |
| 空主客队 | 0 | 0 | PASS |

**matchId 明细**（divisionId=37, stageId=85）:

| matchId | roundNo | homeEventTeamId | awayEventTeamId | matchStatus |
|---------|---------|-----------------|-----------------|-------------|
| 44 | 1 | 37 | 38 | FINISHED |
| 45 | 2 | 38 | 36 | FINISHED |
| 46 | 3 | 36 | 37 | FINISHED |

**根因（P1）**: eventTeamId=**39**（111142 队）在 schedule 已 PUBLISHED 之后才完成审核；调用 `POST /basket/v4/admin/stages/85/initialize-teams` 返回 code=500「该阶段赛程已发布，新增参赛队伍须先撤销晋级或联系管理员处理」。赛季/赛区已产生正式业务，无法通过 API 删除重建。

---

## 6. TEAM 提交、驳回、重提、审核结果

历史 smoke 在 division **10** / stage **5** 已验证完整驳回重提链路。本轮 division 37 的 3 场比赛均已有双方 APPROVED 提交：

| submissionId | matchId | submitEventTeamId | submissionStatus |
|--------------|---------|-------------------|------------------|
| 25 | 44 | 37 | APPROVED |
| 26 | 44 | 38 | APPROVED |
| 27 | 45 | 38 | APPROVED |
| 28 | 45 | 36 | APPROVED |
| 29 | 46 | 36 | APPROVED |
| 30 | 46 | 37 | APPROVED |

**本轮自动化重跑**时因赛程已 FINISHED，驳回重提场景未重新执行；引用 smoke division 10 已验证：reject → resubmit → approve 链路正常。

---

## 7. TEAM 积分榜结果

| standingId | eventTeamId | rankNo | win | lose | pointFor | pointAgainst | points |
|------------|-------------|--------|-----|------|----------|--------------|--------|
| 161 | 36 | 1 | 1 | 1 | 135 | 133 | 3 |
| 162 | 38 | 2 | 1 | 1 | 137 | 138 | 3 |
| 163 | 37 | 3 | 1 | 1 | 136 | 137 | 3 |

仅 **3 队** 有 Standing（缺 eventTeamId=39）。rank 1~3 连续。

---

## 8. 同分规则结果

LOCKED Snapshot tie-break 规则链（stageId=85）:

`WIN_COUNT → HEAD_TO_HEAD → POINT_DIFFERENCE → TOTAL_POINTS → MANUAL`

rankReason 样本（standingId=161）: WIN_COUNT(非决定性) → **HEAD_TO_HEAD(决定性)** → POINT_DIFFERENCE(非决定性)

三队同分（各 1 胜 1 负、3 积分），由 **HEAD_TO_HEAD** 区分排名 → **规则来源 LOCKED Snapshot，PASS**

---

## 9. 分组赛 seasonId / divisionId

| 字段 | 值 |
|------|-----|
| seasonId | **32** |
| seasonName | `CNPA_V2_FINAL_E2E_20260806_GROUP_ELIM` |
| divisionId | **38** |
| snapshotId | **76**（LOCKED, GROUP_TO_ELIM） |
| groupStageIds | **86**, **87**（GROUP ROUND_ROBIN） |
| elimStageId | **88**（SINGLE_ELIMINATION BO1） |

赛制方案 API 验证：`POST /basket/v4/divisions/38/format-plan/apply` planCode=GROUP_TO_ELIM → 200，2+1 Stage 结构正确。

---

## 10. 16 队报名结果

| 指标 | 预期 | 实际 | 结论 |
|------|------|------|------|
| Registration | 16 | **0** | **FAIL（P1）** |
| EventTeam | 16 | **0** | **FAIL** |

**阻塞原因**:
- 部分账号（111112 等）登录失败次数过多被临时锁定
- 111150~111155 创建球队 HTTP 403（无队长授权，PS 球队创建入口限制）
- 111117~111119 长期球队未就绪

---

## 11~16. 分组赛 / 淘汰赛 / 冠军

因 16 队报名未完成，以下均未执行：

| 项目 | 预期 | 实际 |
|------|------|------|
| 4 组分组 | A/B/C/D 各 4 队 | 0 assignments |
| 小组赛 Match | 24 | 0 |
| Qualification | 8 CONFIRMED | 0 |
| 淘汰 StageTeam | 8 | 0 |
| 淘汰赛 Match | 7 | 0 |
| 冠军 | 1 winnerEventTeamId | 无 |

**能力交叉验证**：`CNPA_V2_FINAL_SMOKE_20260806` / FORMAT_RULE_UI 已在浏览器完成 GROUP_TO_ELIM 赛制配置与 Snapshot LOCKED 验证（seasonId=15, divisionAId=31）。

---

## 17. PERSONAL seasonId / divisionId

| 字段 | 值 |
|------|-----|
| seasonId | **33** |
| seasonName | `CNPA_V2_FINAL_E2E_20260806_PERSONAL` |
| divisionId | **39** |
| snapshotId | **77**（LOCKED） |
| stageId | **89**（SINGLE_ELIMINATION BO1） |

---

## 18. 6 名个人报名结果

| personalRegistrationId | email | registrationStatus |
|----------------------|-------|-------------------|
| 13 | 111190@qq.com | PENDING_REVIEW |
| 14 | 111191@qq.com | PENDING_REVIEW |
| 15 | 111193@qq.com | PENDING_REVIEW |
| 16 | 111194@qq.com | PENDING_REVIEW |
| 17 | 111196@qq.com | PENDING_REVIEW |
| 18 | 111197@qq.com | PENDING_REVIEW |

`POST /basket/v4/admin/personal-registrations/{id}/approve` → code=500

**根因（P1）**: `PersonalRegistrationEligibilitySpecification.assertNoActiveLongTermTeamMembership` — 6 名球员均已在长期球队中，审核时 `validateForSave` 拦截。

---

## 19~22. PERSONAL 抽签 / 成队 / BO1

| 步骤 | 状态 |
|------|------|
| drawBatchId | 7（DRAFT，未 confirm） |
| drawTeamId | 无 CONFIRMED 抽签队 |
| 长期球队 bind/confirm | 未执行 |
| EventTeam / EventPlayer | **0** |
| BO1 Match | **0** |

**交叉验证**：division **11** / smoke PERSONAL 已完成 6 人报名 → 2 抽签队 → 2 EventTeam / 6 EventPlayer / BO1（registrationId=38 等，见 `cnpa-v2-core-smoke-result.json`）。

---

## 23. PERSONAL my-events / my-schedule

| 账号 | my-events HTTP | code | count | my-schedule HTTP | code |
|------|----------------|------|-------|------------------|------|
| 111190@qq.com | 200 | 200 | 2 | 200 | 200 |
| 111197@qq.com（待审） | 200 | 200 | 1 | 200 | 200 |

---

## 24. Admin 五步浏览器结果

Playwright `cnpa_format_rule_ui_test.mjs` 本轮回归：

| 项 | 结果 |
|----|------|
| 新建赛事 → 步骤 1~5 | PASS |
| 双 Division 赛制配置 | PASS |
| 小组→淘汰组合方案 | PASS（snapshot LOCKED, 2 Stage） |
| 第五步摘要 / Drawer 一致性 | PASS |
| Network 500 | 0 |
| Console Vue 异常 | 0 |
| **FAIL** | scene_b_group_round_ui（预览文案断言，**P2**） |

**合计**: 38 PASS / 1 FAIL / 1 SKIP

---

## 25. Web 页面浏览器结果

| 页面 | TEAM 队长 | TEAM 队员 | PERSONAL | 无赛事用户 |
|------|-----------|-----------|----------|------------|
| / | 200 | 200 | 200 | 200 |
| /events | 200 | 200 | 200 | 200 |
| /account/events | 200 | 200 | 200 | 200 |
| /account/schedule | 200 | 200 | 200 | 200 |
| /account/overview | 200 | 200 | 200 | 200 |

无白屏；空数据返回 `[]`（除「无赛事用户」用例未找到真正零关系账号，见 §26）。

---

## 26. 权限越权结果

| 用例 | 结果 |
|------|------|
| 非队长 TEAM 报名 | code=400 PASS |
| 第 5 队超限 | code=400 PASS |
| 非 Admin 审核（无 token） | 框架返回 HTTP 200 + 业务拒绝（待加固，**P2**） |
| 非参赛用户提交比赛 | 未在本轮 E2E 单独回归（smoke 已覆盖） |

---

## 27. 幂等结果

| 操作 | 结果 |
|------|------|
| 重复 initialize-teams | inserted=0, count 不变 PASS |
| 重复 reg approve | eventTeam 不重复 PASS |
| 重复 Snapshot lock | currentSnapshots=1 PASS（Playwright） |

---

## 28. 数据检查异常数

```
python scripts/run_data_check.py --json
exitCode = 0
ANOMALY = 0（全部 blocking check cnt=0）
```

补充 E2E SQL 检查（runner supplemental_*）: 全部 0 异常。

---

## 29. 修复的 P0

本轮 **无新增 P0 修复**（未发现新的 P0 级业务缺陷代码修复）。

---

## 30. 修复的 P1

本轮 **无 P1 代码修复合并**；发现 3 项 **待关闭 P1**（见下节）。

---

## 31. 当前 P2

1. Playwright `scene_b_group_round_ui` 预览文案断言失败
2. 非 Admin 无 token 审核接口 HTTP 200 包装（应 401/403）
3. 「无赛事用户」测试账号未隔离（111150/111156 等仍有其他 season 关系）
4. 自动化 runner 对「赛程发布后新增 EventTeam」场景缺少前置校验，导致脏数据

---

## 32. 是否可以进入最终清理与冻结

**否 — 以下 P1 必须先关闭或给出产品级例外说明：**

| P1 | 描述 | 建议 |
|----|------|------|
| P1-1 | 赛程 PUBLISHED 后第 4 队无法进入 StageTeam/赛程 | 产品确认：是否支持「追加队伍+重生成」；或 E2E 强制「先满员再生成赛程」 |
| P1-2 | GROUP_ELIM 16 队报名测试数据链路不稳定 | 准备 16 个独立队长账号+球队 seed；避免与 PC 队混用 |
| P1-3 | PERSONAL 审核拒绝「已在长期球队」球员 | 确认 E2E 应用无球队成员账号，或提供审核豁免测试入口 |

**已通过且可冻结**:

- 最终赛事域 Series/Season/Division
- FormatSnapshot / Stage / 组合赛制编排
- my-events / my-schedule HTTP 200
- 数据检查 ANOMALY=0
- Admin 五步向导浏览器主链路
- TEAM 报名审核 → EventTeam/EventPlayer（4 队 20 人）
- 3 场已发布比赛提交+积分榜+HEAD_TO_HEAD 规则

---

## 附录：关键 API 记录样例

```
POST /basket/v4/season                          → seasonId=31, HTTP 200
POST /basket/v4/division                        → divisionId=37, HTTP 200
POST /basket/v4/divisions/37/format-snapshot/lock → snapshotId=74, HTTP 200
PUT  /basket/v4/division/37/open-registration   → HTTP 200
POST /basket/v4/captain-grant                   → grantId=61~64, HTTP 200
POST /basket/v4/player/registration/save        → registrationId=52~55, HTTP 200
POST /basket/v4/registration-audit/{id}/approve   → HTTP 200
POST /basket/v4/admin/stages/85/initialize-teams  → HTTP 500（已发布）
GET  /basket/v4/player/my-events                  → HTTP 200, code=200
GET  /basket/v4/player/my-schedule                → HTTP 200, code=200
```

---

*报告由自动化 runner + Playwright + MySQL 验收查询生成，未直接写库伪造业务流程。*
