# CNPA V2 核心冒烟测试报告（最终关闭轮）

## 1. 测试时间和环境

| 项 | 值 |
|---|---|
| 批次 | `SMOKE_V2_20260805_FINAL` |
| 时间 | 2026-08-05 22:00 ~ 23:05 +08:00 |
| Admin | http://127.0.0.1:8080 |
| Web API | http://127.0.0.1:8881 |
| Admin UI | http://127.0.0.1:80 |
| Web UI | http://127.0.0.1:5173 |
| MySQL | 192.168.31.47:3306 / basketball |
| 命令 | `python scripts/cnpa_v2_core_smoke_final.py`；`python scripts/run_data_check.py --json` |
| 环境 | `scripts/.env.v2.local` |

## 2. 本轮代码修复

| 项 | 说明 |
|---|---|
| PERSONAL bind | `validateTeamForBind` 移除阵容人数/全员成员校验，仅保留队长、占用、冲突检查 |
| PERSONAL 成员 | bind 后按 `PersonalDrawAssignment` 自动加入长期球队，`join_source=PERSONAL_DRAW` |
| PERSONAL confirm | 完整阵容校验移至 confirm，人数规则从 `Division.gameMode` 读取（3V3: 3~8，5V5: 5~8） |
| Standing H2H | `HEAD_TO_HEAD` 改为成对相互战绩比较；`rank_reason` 与下一名次球队对比并标记 decisive |
| 模板种子 | 默认规则链 `WIN_COUNT → HEAD_TO_HEAD → POINT_DIFFERENCE → TOTAL_POINTS → MANUAL` |

## 3. PERSONAL bind / confirm 全链路

| 步骤 | 结果 | ID / 说明 |
|---|---|---|
| DrawTeam + 队长指定 | PASS | drawTeamId=**7**, divisionId=**12** |
| Web create-team | PASS | createdTeamId=**2085010794122125300** |
| available-teams | PASS | 返回新球队 |
| bind-team | **PASS HTTP 200** | 不再校验最终阵容人数 |
| 成员加入 | **PASS** | 3 人；sources=`CREATE`, `PERSONAL_DRAW`, `PERSONAL_DRAW` |
| formation readiness | PASS | `READY_TO_CONFIRM` |
| confirm-team | **PASS HTTP 200** | 幂等成功 |
| Registration | PASS | formationRegistrationId=**38**（`PERSONAL_FORMATION`） |
| EventTeam | PASS | eventTeamId=**22** |
| EventPlayer | PASS | eventPlayerIds=**102, 103, 104**（3 人） |
| 重复 confirm | PASS | 不新增 Registration/EventTeam/EventPlayer |

**bind 根因**：bind 阶段错误调用正式报名阵容校验（含 5V5 最少 5 人逻辑），在抽签成员尚未全部入队时即拒绝绑定。

**成员加入方式**：bind 成功后系统按 `PersonalDrawAssignment` 自动建立长期球队成员关系（`TeamMemberJoinHelper` + `PERSONAL_DRAW`），无需 SQL 手工插入。

### create-team 授权与球队认证

| 检查项 | 结果 |
|---|---|
| 仅 drawTeam.captainUserId 可调用 | PASS（非队长返回 403） |
| drawTeam 须属 PERSONAL Division | PASS |
| 已转换 drawTeam 不可重复建队 | PASS |
| Grant 仅用于当前成队任务 | PASS |
| 审计日志 | PASS（`securityLogHelper.write` event=personal-formation） |

**球队认证**：PERSONAL 成队任务专用入口 `createForPersonalFormation` 创建的球队 `auditStatus=APPROVED`，作为 **PERSONAL_DRAW 业务例外**（队长 create 时即写入成员关系）；不通过普通 `/basket/team/create` 复制该能力。正式 confirm 前仍校验球员认证与 Division 平台匹配。

## 4. StageTeam 1→4 增量（division 14 / stage 9）

| 轮次 | totalEventTeam | existingStageTeam | insertedStageTeam | DB stage_team |
|---|---|---|---|---|
| 第一次同步 | 1 | 0 | **1** | **1** |
| 第二次增量 | 4 | 1 | **3** | **4** |
| 第三次幂等 | 4 | 4 | **0** | **4** |

- stage_team IDs: 8~11；event_team_id: 23, 24, 25, 26
- 无 `event_team_id` / `active_unique_key` 重复

### 赛程后门禁

| 场景 | 结果 |
|---|---|
| division 14 本地赛程生成 | FAIL（`home_event_team_id cannot be null`，待查 schedule 策略） |
| division 10 / stage 5 已发布赛程 gate | **PASS** — 明确拒绝新增 StageTeam，msg=「该阶段赛程已发布，新增参赛队伍须先撤销晋级或联系管理员处理」 |

## 5. Standing 同分（division 10 / stage 5）

**最终 tie-break 规则链**（Division 10 补录，不修改 LOCKED Snapshot 元数据）：

1. WIN_COUNT
2. HEAD_TO_HEAD
3. POINT_DIFFERENCE
4. TOTAL_POINTS
5. MANUAL

**EventTeam 17 vs 18 验证**（match 3：17 胜 18）：

| rank | eventTeamId | win_count | 说明 |
|---|---|---|---|
| 2 | **17** | 2 | HEAD_TO_HEAD compare_value=**1**, is_decisive=**1** |
| 3 | **18** | 2 | 相互战绩落败 |

`event_team_id` 仅作展示稳定序，**不作为正式业务排名依据**。

## 6. Admin 五步向导 / Web UI

| 项 | 结果 |
|---|---|
| Admin 五步向导（Dialog/保存/发布/Snapshot/开放报名） | **MANUAL** — 本轮无自动浏览器，需人工打开 Admin UI |
| Web UI 核心页面（首页/赛事中心/登录/个人中心/报名/赛程/数据中心/已结束） | **PASS** HTTP 200 + HTML 内容 |
| Web API division/list、season/9 | **PASS** |
| Web API my-events、my-schedule/list | **FAIL** HTTP 500（P2） |
| PERSONAL 成队页面深度操作 | **MANUAL** — HTTP bind/confirm 已覆盖 |

## 7. data-check

| 项 | 结果 |
|---|---|
| exit code | **0** |
| ANOMALY | **0** |
| check_16 approved reg no event_team | **0** |
| check_13 duplicate event_team | **0** |
| check_14 duplicate registration | **0** |
| legacy write perms | **0** |
| active_unique_key 重复 | **0** |

## 8. 业务 ID 汇总

```json
{
  "personalWebDrawTeamId": 7,
  "personalWebDivisionId": 12,
  "personalWebCreatedTeamId": 2085010794122125300,
  "personalWebMemberRelationIds": [2085010794185040000, 2085016069658943500, 2085016069851881500],
  "personalWebRegistrationId": 38,
  "personalWebEventTeamId": 22,
  "personalWebEventPlayerIds": [102, 103, 104],
  "stageTeamTestDivisionId": 14,
  "stageTeamTestStageId": 9,
  "standingTestDivisionId": 10,
  "standingTestStageId": 5,
  "standingH2hDecisiveSample": {
    "event_team_id": 17,
    "rule_code": "HEAD_TO_HEAD",
    "is_decisive": 1,
    "compare_value": "1"
  }
}
```

## 9. P0 / P1 / P2

### P0：无

### P1

1. Admin 五步向导人工回归未完成（MANUAL）
2. division 14 赛程生成失败（`home_event_team_id cannot be null`），本场景门禁改由 division 10/stage 5 验证

### P2

1. Web API `my-events` / `my-schedule/list` 返回 500
2. Web UI / PERSONAL 页面深度浏览器操作未在本轮执行（MANUAL）

## 10. 结论

**核心冒烟部分通过，存在非阻塞问题。**

PERSONAL bind/confirm 正式闭环、StageTeam 1→4 增量与幂等、Standing HEAD_TO_HEAD  decisive 规则、data-check 全绿均已达成。**尚不满足**进入扩大测试的全部 16 项标准：Admin 五步向导与 Web 深度人工回归未完成；`my-events`/`my-schedule` API 500 待修。

---

**JSON**：`scripts/output/cnpa-v2-core-smoke-result.json`  
**data-check JSON**：`scripts/output/r5-data-check-result.json`
