# CNPA 赛事域 V4 第 7 轮：个人中心我的赛事 / 我的赛程 / 比赛数据入口 V4 化

> 轮次：R7  
> 日期：2026-08-04  
> 范围：个人中心「我的赛事」四 Tab V4 化、我的赛程聚合、比赛数据提交入口、旧入口收敛  
> **不进入第 8 轮**

---

## 1. 本轮完成内容

### 1.1 Web 后端 Player API 补充

- **已参加赛事** `GET /basket/v4/player/my-events`：基于 `EventPlayer` / `EventTeam` 快照，返回赛程数量、已完成数量、待提交数据数量
- **我的赛程** `GET /basket/v4/player/my-schedule`：跨 Division 聚合当前用户参与的全部已发布 Match；支持 `seasonId` / `divisionId` / `status` / `includeFinished` / `onlyPendingSubmit`
- **增强** `GET /basket/v4/player/match-submission/my-matches`：补充 `homeScore` / `awayScore` / `opponentSubmissionStatus` / `canSubmitData` / `latestVersionNo` / `latestAuditRemark` / `latestRejectReason`
- **增强** `GET /basket/v4/player/registration/available-divisions`：补充 `matchStartTime` / `matchEndTime` / `grantTeamId` / `grantTeamName`
- **增强** `GET /basket/v4/player/registration/my-list`：补充 `seasonId` / `divisionId` / `teamId`

### 1.2 web-ui 个人中心 V4 化

- `MyEventsView.vue` 四 Tab 全部切换为 V4 Player API，不再调用 legacy 报名/赛程/参赛接口
- 新增 `V4MySchedulePanel.vue`：卡片式赛程 + 未比赛/全部切换 + 本方 submission 状态 + 提交入口
- 新增 `V4RegistrationFlow.vue`：V4 球队报名（save / submit / withdraw）
- 新增 `V4MatchSubmissionView.vue`：V4 比赛数据 P0 表单（总比分、球队/球员统计、fileId 占位、草稿/提交/重提）
- 新增 `v4PlayerEvents.ts` / `v4MatchSubmission.ts` 及类型定义
- 新增 `v4AccountEventsMappers.ts`：V4 DTO → 个人中心 UI 模型

### 1.3 旧入口处理

- 侧边栏移除「我的球队」一级入口（保留路由 `/account/team` 供深链，不在 R7 主菜单展示）
- `/account/my-tournaments/*` 全部 redirect → `/account/my-events` 对应 Tab 或 V4 比赛数据页
- Open 报名路由 redirect → `my-events?tab=available`（本轮不做 Open/PERSONAL）
- Legacy 旧组件文件保留，未大规模删除

---

## 2. 新增 / 修改文件清单

### 2.1 basketball-web 后端

**新增**

| 文件 | 说明 |
|------|------|
| `response/v4/V4MyEventResp.java` | 已参加赛事响应 |
| `response/v4/V4MyScheduleResp.java` | 我的赛程响应 |
| `service/v4/IBasketV4PlayerMyEventsService.java` | 已参加赛事服务 |
| `service/v4/IBasketV4PlayerMyScheduleService.java` | 我的赛程服务 |
| `service/impl/v4/BasketV4PlayerMyEventsServiceImpl.java` | 实现 |
| `service/impl/v4/BasketV4PlayerMyScheduleServiceImpl.java` | 实现 |
| `mapper/v4/BasketV4PlayerMyEventsMapper.java` | Mapper |
| `resources/mapper/v4/BasketV4PlayerMyEventsMapper.xml` | SQL |
| `controller/v4/BasketV4PlayerMyScheduleController.java` | Controller |

**修改**

| 文件 | 说明 |
|------|------|
| `controller/v4/BasketV4PlayerMyEventsController.java` | EventTeam 聚合 |
| `response/v4/V4MyMatchResp.java` | submission/比分字段 |
| `response/v4/V4AvailableDivisionResp.java` | 比赛时间/授权球队 |
| `response/v4/V4MyRegistrationResp.java` | divisionId/teamId |
| `mapper/v4/BasketV4PlayerMatchSubmissionMapper.java` | selectMySchedule |
| `resources/mapper/v4/BasketV4PlayerMatchSubmissionMapper.xml` | 公共 SQL + 赛程 |
| `resources/mapper/v4/BasketV4PlayerRegistrationMapper.xml` | 补充字段 |
| `service/impl/v4/BasketV4PlayerMatchSubmissionServiceImpl.java` | canSubmitData |

### 2.2 basketball-web-ui

**新增**：`v4PlayerEvents.ts`、`v4MatchSubmission.ts`、types、`v4AccountEventsMappers.ts`、`V4MySchedulePanel.vue`、`V4RegistrationFlow.vue`、`V4MatchSubmissionView.vue`

**修改**：`MyEventsView.vue`、`accountEventsUi.ts`、`AccountSidebar.vue`、`router/index.ts`

---

## 3. 新增 / 修改 Web Player API 清单

| Method | Path | 状态 |
|--------|------|------|
| GET | `/basket/v4/player/registration/available-divisions` | 增强 |
| GET | `/basket/v4/player/registration/my-list` | 增强 |
| GET | `/basket/v4/player/registration/{registrationId}/detail` | **新增**（补验收） |
| POST | `/basket/v4/player/registration/save` | R3 复用 |
| POST | `/basket/v4/player/registration/submit` | R3 复用 |
| POST | `/basket/v4/player/registration/withdraw` | R3 复用 |
| GET | `/basket/v4/player/my-events` | **重写** |
| GET | `/basket/v4/player/my-schedule` | **新增** |
| GET | `/basket/v4/player/match-submission/my-matches` | 增强 |
| GET/POST | `/basket/v4/player/match-submission/*` | R5 复用 |

---

## 4. 新增 / 修改个人中心页面路径

| 路径 | 说明 |
|------|------|
| `/account/my-events` | 四 Tab 主入口 |
| `/account/my-events/v4/registration/:divisionId` | V4 报名 |
| `/account/my-events/v4/match-submission/:matchId` | V4 比赛数据 |

---

## 5–11. 功能说明（摘要）

- **可参与赛事**：V4 Division + CaptainGrant + canRegister
- **我的报名**：V4 Registration 全状态 + 审核中不占名额提示
- **已参加赛事**：EventTeam/EventPlayer 快照 + 待提交计数
- **我的赛程**：跨赛事 Match 聚合，默认未结束，可切全部
- **比赛数据**：R5 submission 流程，P0 表单已实现
- **单方审核隔离**：canSubmit 仅看本方 status

---

## 12. 一方审核中不影响另一方入口

`BasketV4PlayerMatchSubmissionServiceImpl.enrichCanSubmit` 与 `BasketV4PlayerMyScheduleServiceImpl.enrichCanSubmit` 只判断 `mySubmissionStatus`；`opponentSubmissionStatus` 仅前端展示。

---

## 13. 旧个人中心入口处理结果

| 旧入口 | 处理 |
|--------|------|
| 侧边栏「我的球队」 | 隐藏 |
| my-tournaments / 旧比赛数据 / Open | redirect → my-events |
| Legacy 组件 | 保留，R8 删除 |

---

## 14. 新旧 API 隔离检查

V4 主线（MyEventsView、V4MySchedulePanel、V4RegistrationFlow、V4MatchSubmissionView）**未调用** legacy 赛事/比赛数据 API。

`AvailableEvent.tournamentId` / `MyApplication.tournamentId` 等 UI 类型字段仍作为 **divisionId 短期别名**（映射层写入 divisionId），第 8 轮应重命名为 `divisionId` 并删除 legacy 类型依赖。

---

## 15. 冒烟记录（初版）

初版冒烟时运行中后端未加载 R7，`my-schedule` 返回系统异常。补验收见第 18 节。

---

## 16. 风险与待确认项

1. ~~后端需重新编译部署后完整验证 my-schedule~~（补验收已通过）
2. ~~报名详情/阵容只读 API 待补充~~（已新增 `GET /registration/{id}/detail`）
3. 材料上传为 fileId 占位
4. R6 遗留 TS 问题未在本轮修复
5. UI 类型 `tournamentId` 别名待 R8 清理（见 §14）

---

## 17. 是否建议进入第 8 轮

**不建议本轮进入。** R7 补验收已通过；第 8 轮再专注 Legacy 物理删除与 UI 字段重命名。

---

## 18. 第 7 轮真实冒烟补验收记录

> 脚本：`scripts/smoke_v4_r7_personal_center.py`  
> 结果文件：`docs/cnpa-v4-round7-smoke-results.json`  
> 冒烟时间：2026-08-04 02:35:50

### 18.1 服务是否已加载 R7 后端代码

**是。** 已停止旧进程（PID 34112）并重新执行 `mvn compile spring-boot:run`（MiniApplication，端口 8881）。登录态下 `GET /basket/v4/player/my-schedule` 返回 200，不再报系统异常。

### 18.2 测试账号

| 角色 | 邮箱 | 用途 |
|------|------|------|
| 主队队长/球员 | `111111@qq.com` | 可参与赛事、我的报名、已参加赛事、我的赛程、比赛数据 |
| 客队球员 | `111122@qq.com` | A/B 队提交独立性验证 |

密码：`111111aa`

### 18.3–18.7 测试 ID

| 字段 | 值 |
|------|-----|
| PlayerId / MemberId | `2082478335308456000` |
| SeasonId | `4` |
| DivisionId | `5` |
| EventTeamId（主队） | `14` |
| EventTeamId（客队） | `15` |
| MatchId | `1` |

### 18.8 available-divisions 结果 — **PASS**

- count=1，含 `grantTeamId` / `grantTeamName` / `matchStartTime` / `matchEndTime`
- 当前账号 `canRegister=0`，`cannotRegisterReason` 有值（已有 APPROVED 报名）
- 未调用旧 `/basket/tournament/registration/*`

### 18.9 my-list 结果 — **PASS**

- count=2，状态含 `APPROVED`
- 返回 `seasonId` / `divisionId` / `teamId`
- APPROVED 报名可在 my-events 中看到对应 EventTeam

### 18.10 my-events 结果 — **PASS**

- count=2（基于 EventPlayer 快照，非长期球队实时成员）
- 返回 `eventTeamId` / `divisionId` / `seasonId` / `matchCount` / `finishedMatchCount` / `pendingSubmissionCount`

### 18.11 my-schedule 结果 — **PASS**

| 场景 | 结果 |
|------|------|
| 默认（未完成） | count=0（唯一已发布比赛为 FINISHED，符合预期） |
| `includeFinished=true` | count=1，含 FINISHED、homeScore/awayScore |
| 字段 | myEventTeamId、opponentEventTeamId、mySubmissionStatus、opponentSubmissionStatus、canSubmitData、cannotSubmitReason 均返回 |
| Division 隔离 | 查询 scoped 至用户所属 EventTeam 的 Match |

### 18.12 match-submission my-matches 结果 — **PASS**

- count=1，matchId=1
- 主队：`mySubmissionStatus=APPROVED`，`canSubmitData=false`
- 客队：`mySubmissionStatus=DRAFT`，`canSubmitData=true`
- `opponentSubmissionStatus` 仅展示，不参与本方 canSubmit 计算

### 18.13 /account/my-events 四 Tab 页面 — **PASS**

| Tab | HTTP |
|-----|------|
| 可参与赛事 | 200 |
| 我的报名 | 200 |
| 已参加赛事 | 200 |
| 我的赛程 | 200 |

### 18.14 比赛数据入口 — **PASS**

- `/account/my-events/v4/match-submission/1?submitEventTeamId=14` HTTP 200
- `GET .../match-submission/1/detail?submitEventTeamId=14` 返回 `submissionStatus=APPROVED`（只读）
- 客队 DRAFT 状态可 `canSubmitData=true`（见 my-matches）

### 18.15 A 队审核中是否不影响 B 队入口 — **PASS**

临时将主队 submission 设为 `PENDING_REVIEW` 后复测：

- 主队：`mySubmissionStatus=PENDING_REVIEW`，不可提交
- 客队：`canSubmitData=true`（**不受主队审核中影响**）

### 18.16 是否仍调用旧赛事 API — **否（V4 主线）**

个人中心 V4 主线组件仅调用 `/basket/v4/player/*`。Legacy 旧 Vue 文件仍保留但路由已 redirect，不再作为主线。

### 18.17 补验收修复项

1. 重启 basketball-web 加载 R7 后端（解决 my-schedule 不可用）
2. 新增 `GET /basket/v4/player/registration/{registrationId}/detail` + 前端回显报名阵容
3. 增强冒烟脚本：登录态 API 全量验证 + A 队 PENDING_REVIEW / B 队可提交专项用例

### 18.18 是否可以进入第 8 轮

**不建议本轮进入第 8 轮。** R7 个人中心 V4 闭环与补验收均已通过。
