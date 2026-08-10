# CNPA「球队 Team」业务模型对齐分析

> 日期：2026-08-10  
> 范围：只读盘点（未改代码 / SQL / 表结构）  
> 覆盖模块：`basketball-admin` / `basketball-admin-ui` / `basketball-web` / `basketball-web-ui`  
> 依据：当前库表结构、Java/Vue 实现、以及库内真实数据抽样

---

## 1. 当前 Team 实际模型

### 结论

**`basket_team` 是跨赛季长期球队资产，不是 Division 级参赛球队。**

### 证据

| 项 | 现状 |
|---|---|
| 主表 | `basket_team`（DDL 在 `sql/basketball.sql`，不在 V4 38 表索引内） |
| Entity | Web `com.zhs.model.BasketTeam`；Admin `com.zhs.basket.domain.BasketTeam` |
| 是否绑定 `series_id` | **否** |
| 是否绑定 `season_id` | **否** |
| 是否绑定 `division_id` | **否** |
| 状态字段 | `team_status`：`TeamStatusEnum` DISABLED=0 / ACTIVE=1 / DISSOLVED=2 |
| 是否有 ARCHIVED | **否**（只有解散 DISSOLVED + 软删 `is_delete`） |
| 与 Division 关系 | **间接**：经 `basket_captain_grant.team_id`、`basket_registration.team_id`、`basket_event_team.source_team_id` |

### 创建链路

1. Admin 授予 Division 队长权限 → `BasketV4CaptainGrantServiceImpl.create`  
   - API：`POST /basket/v4/captain-grant`  
   - 页：`basketball-admin-ui/src/views/cnpa/event-v4/captain-grant/index.vue`
2. Web 创建球队 → `AccountTeamServiceImpl.create`  
   - API：`POST /basket/team/create`  
   - Controller：`AccountTeamController`  
   - 门禁：`captainPermissionService.requireValidQualification(memberId)`  
   - **取的是该会员任意一条 ACTIVE grant（`CaptainGrantSupport.findCurrentGrant`），请求体不带 divisionId**  
   - 插入 `basket_team`（无 division 字段）  
   - 插入队长 `basket_team_member`  
   - `captainGrantSupport.bindGrantToTeam(grant, teamId)` 回写 grant.`team_id`
3. 也可「继承历史球队」：`POST /basket/team/captain/grant/bind-team`  
   - `AccountTeamServiceImpl.bindGrantToTeam` / `listInheritableTeams`  
   - 把新 Division 授权绑到**同一条**历史 `basket_team` 上 → **允许跨届复用同一 team_id**

### 「我的球队」查什么

- API：`GET /basket/team/mine` → `AccountTeamServiceImpl.pageMine`
- 条件：当前用户 `basket_team_member.is_current=1 AND membership_status=ACTIVE`
- **查的是长期成员关系下的永久球队**，不是「当前赛事球队」列表
- 前端：`basketball-web-ui/src/views/account/team/index.vue` + `stores/team.ts`  
  - 取第 1 条作 `myTeamSummary`（一人一当前长期队模型）
  - 授权失效后显示「历史队长」，但仍指向同一 team 记录

### 跨 Season / Division 复用（库内已证实）

`basket_registration` 唯一键语义为 `(division_id, team_id)`，同一长期球队可报多个 Division。  
库内抽样已有多条 `team_id` 同时关联 2 个不同 `division_id`（含双 APPROVED）。

### 对第 2 节问题的直接回答

1. **未直接绑定** series/season/division。  
2. 与 Division 关系靠：`captain_grant`（创建/管理门禁）+ `registration`（报名）+ `event_team.source_team_id`（正式参赛）。  
3. **理论上可以**参加多个 Season / 多个 Division / 下一届复用；代码与数据均支持。  
4. 「我的球队」= **永久球队（当前 ACTIVE 成员关系）**，不是当前赛事球队清单。  
5. 成员关系长期存在（见第 3 节）。  
6. 队长概念是多套并存（见第 2 节）。  
7. 赛事结束后 Team/Member **不会自动归档**（见第 5 节）。

---

## 2. 当前队长权限实际模型

### 结论

**队长权限是 Division 级，不是永久队长；但球队本身仍是永久资产。**  
存在「Team 永久队长字段」+「Division 授权」双概念。

### 表与字段

表：`basket_captain_grant`

| 字段 | 含义 |
|---|---|
| `division_id` | 授权绑定平台赛区（必填） |
| `season_id` | 冗余赛季 |
| `team_id` | 后绑定长期球队（可空） |
| `player_id` / `member_id` | 授权对象 |
| `grant_status` | ACTIVE / REVOKED / EXPIRED |
| `active_unique_key` | ACTIVE 时 = `{divisionId}_{playerId}`（代码 `CaptainGrantSupport.buildActiveUniqueKey`） |
| `valid_from` / `valid_to` | 有效期；`valid_to` 优先 Division.`match_end_time`，否则 Season.`season_end_time` |
| `grant_source` | MANUAL / PERSONAL_DRAW |

### 关键类

| 职责 | 类 |
|---|---|
| Admin 创建/撤销 | `BasketV4CaptainGrantServiceImpl` / `BasketV4CaptainGrantController` |
| 运行时懒过期 | Admin `CaptainGrantRuntimeSupport.lazyExpireIfNeeded`；Web `CaptainGrantValiditySupport` |
| Web 门禁 | `CaptainPermissionServiceImpl` + `CaptainGrantSupport` |
| 有效性 | 状态、activeKey、球员认证、validFrom/To、**Season FINISHED/ARCHIVED** |

### 队长相关「多重身份」

| 概念 | 落点 | 是否永久 |
|---|---|---|
| Division 队长授权 | `basket_captain_grant` | 否（按 Division / 有效期） |
| Team 永久队长字段 | `basket_team.captain_member_id` / `captain_player_id` | 是（建队写入，不随赛事结束清理） |
| 成员角色队长 | `basket_team_member.role_type=CAPTAIN` | 随成员关系长期存在 |
| Registration 队长 | `basket_registration.captain_*` | 报名单快照 |
| EventTeam 队长 | `basket_event_team.captain_*` | 赛事快照 |

### 已符合新口径的部分

- 线下缴费 → Admin 按 Division 授权  
- 创建球队需有效授权  
- 下一届需重新授权  
- **没有**「永久队长权限」主路径

### 与新口径冲突的部分

- 授权可 `bind-team` 到历史长期球队，从而让**同一 team_id** 进入下一届  
- 创建球队时不记录 division_id，球队生命周期不随 Division 结束

---

## 3. 当前 TeamMember 实际模型

### 结论

**成员关系挂在长期 `team_id` 上，表达「张三属于某永久球队」，不是「张三属于某届 Division 球队」。**

### 表：`basket_team_member`

关键字段：`team_id`, `member_id`, `player_id`, `role_type`, `roster_type`, `join_source`, `membership_status`, `is_current`, `join_time/leave_time`, `active_member_slot`（唯一活跃槽）

状态：`TeamMembershipStatusEnum` LEFT=0 / ACTIVE=1 / REMOVED=2

### 邀请 / 申请

| 表 | 绑定维度 | 是否有 division/season/registration |
|---|---|---|
| `basket_team_invitation` | 仅 `team_id` | **无** |
| `basket_team_join_application` | 仅 `team_id` | **无** |

API：

- 邀请：`TeamInvitationBusinessController` → `/basket/team/{teamId}/invitation*`
- 申请：`TeamJoinApplicationBusinessController` → `/basket/team/{teamId}/join-application*`
- 成员：`TeamMemberBusinessController` → `/basket/team/{teamId}/member*`

生命周期：建队入队 → 邀请接受/申请通过入队 → 离队/踢人改为 LEFT/REMOVED；**无赛事结束归档逻辑**。

一人一活跃队约束：`active_member_slot` + `TeamMembershipSupport`，进一步强化「长期俱乐部」语义。

---

## 4. 当前 Registration / EventTeam / EventPlayer 职责

### 分层（当前真实职责）

```text
basket_team              = 长期球队身份 + 成员运营容器
basket_team_member       = 长期成员关系
basket_registration      = 某 Division 的报名申请/审核状态机
basket_registration_player = 报名阵容草案
basket_event_team        = 审核通过后的正式参赛队快照（赛程/比赛直接引用）
basket_event_player      = 审核通过后的正式参赛球员快照
```

### Registration

- 表：`basket_registration`
- Service：Web `BasketV4PlayerRegistrationServiceImpl`；Admin 审核 `BasketV4RegistrationAuditServiceImpl`
- 状态：DRAFT → PENDING_REVIEW → APPROVED / REJECTED / WITHDRAWN / CANCELLED
- 唯一：`(division_id, team_id)`（同 Division 不可重复有效报名）
- **即使 Team 将来绑定 Division，Registration 仍有必要**：它承载报名审核流，不等于球队实体

### EventTeam / EventPlayer

- 生成时机：**仅审核通过**  
  `BasketV4EventSnapshotServiceImpl.generateFromApprovedRegistration`
- `basket_event_team.source_team_id` 注释即「来源长期球队ID」
- `event_team_status` 默认 ACTIVE；另有快照类枚举（与 FormatSnapshot 的 ARCHIVED 不同体系）
- **比赛 / 对阵 / 晋级引用的是 `event_team_id`，不是 `basket_team.id`**  
  例：`MatchSeriesEliminationScheduleGenerator` 写 `homeEventTeamId` / `awayEventTeamId`

### 模型判断（本次最关键）

| 问题 | 答案 |
|---|---|
| EventTeam 是不是「报名审核后的赛事快照」？ | **是** |
| EventTeam 是不是「事实上的赛事球队」？ | **对赛程/比赛/统计链路而言，是正式参赛身份** |
| Team 与 EventTeam 是否重复？ | **职责未完全重复**：Team 负责报名前置的组队运营；EventTeam 负责审核后的冻结参赛身份 |
| 若 Team 再绑 Division 是否重复模型？ | **概念层有重叠，但不宜合并**：合并会冲击 Match/Standing/Stat 全链路；最小改造应保留 EventTeam |

---

## 5. 当前赛事结束时实际执行逻辑

### 结论

**没有统一的 Division 收尾 Service；不会自动归档 Team / TeamMember；队长权限靠时间与 Season 状态懒失效。**

### Division 状态机现状

`DivisionStatusEnum`：含 PLAYING / FINISHED / ARCHIVED  

代码里实际写入：

| 状态 | 写入位置 |
|---|---|
| DRAFT | `BasketV4DivisionServiceImpl` 创建 |
| REGISTRATION_OPEN / CLOSED | `BasketV4DivisionServiceImpl` 开关报名 |
| SCHEDULED | `BasketV4ScheduleBatchServiceImpl` 发布赛程 |
| PLAYING / FINISHED / ARCHIVED | **未见业务写入入口**（`closeDivision` / `finishDivision` **不存在**） |

当前库抽样：`division_status` 仅见 `DRAFT,REGISTRATION_OPEN,REGISTRATION_CLOSED`；尚无 FINISHED/ARCHIVED 实例。

### 队长权限如何结束

1. `valid_to` 到期 → 懒过期（访问时 `lazyExpireIfNeeded` / `expireById`）  
2. Season 状态为 FINISHED/ARCHIVED → `CaptainGrantValiditySupport` 判无效  
3. Admin 手动 `revoke`  
4. **不会**因 Division 结束批量改 Team / Member

### Team / Member / Registration / EventTeam

| 对象 | 赛事结束时 |
|---|---|
| `basket_team` | 无变化（仍 ACTIVE） |
| `basket_team_member` | 无变化（仍 ACTIVE） |
| `basket_registration` | 保持 APPROVED 等终态，无归档动作 |
| `basket_event_team` | 保持 ACTIVE 快照，无归档动作 |
| 永久档案 | `BasketPermanentArchiveServiceImpl.getTeamArchive` 只读追溯，不解散球队 |

### 定时任务

未发现 Team/Grant/Invite 专用 `@Scheduled`；Grant/Invite 均为懒过期。

---

## 6. 新业务口径与现有实现的差异

### 已符合

| 口径 | 现状 |
|---|---|
| 队长权限按 Division 授权 | `basket_captain_grant.division_id` |
| 创建球队需队长授权 | `AccountTeamServiceImpl.create` |
| 成员进入：邀请 / 申请 | invitation + join_application |
| 报名审核后成正式参赛 | Registration → EventTeam/EventPlayer |
| 比赛/统计走赛事快照，不直接绑长期 Team | Match 用 `event_team_id` |
| 历史数据不物理删除 | 普遍用 `is_delete` / 状态，无物理删主路径 |
| 下一届需重新交费授权 | 新 Division 需新 grant |

### 部分符合

| 口径 | 现状 | 缺口 |
|---|---|---|
| 球队属于具体赛事生命周期 | 创建依赖某次 grant，但 Team **不存** division | 生命周期与 Division 脱钩 |
| 历史球队只读 | 前端历史队长 Tab 收缩；后端主要靠「无有效授权」 | 无正式 ARCHIVED；仍可被 joinable/继承复活 |
| Registration 保留 | 已保留且职责正确 | 需在 Team 改 Division 化后继续保留 |
| 队长权限赛事结束失效 | 有 valid_to / Season 结束判断 | Division FINISHED 本身未落地；无统一收尾 |

### 不符合（核心冲突）

| 新口径 | 当前实现 |
|---|---|
| Team = 某届 Division 参赛球队 | Team = 长期俱乐部资产 |
| 历史球队不可复活再报名 | `inheritable/list` + `bind-team` **明确支持复活** |
| 名称可同、实例必须新 | 同一 `team_id` 可跨 Division 报名（库内已有） |
| 成员关系随赛事归档 | 成员长期 ACTIVE，一人一永久队 |
| Division 结束 → 球队归档 | 无 close/finish；Team 不归档 |
| 「我的球队」分当前/历史赛事队 | 只有长期「我的球队」+ 授权失效文案 |
| 禁止历史队再邀请/申请/改资料 | 授权若重新绑定旧队，管理能力恢复 |

---

## 7. 需要修改的数据库表

### 必须修改

| 表 | 改动建议 | 原因 |
|---|---|---|
| `basket_team` | 新增 `division_id`（建议同时冗余 `season_id`）；新增生命周期状态（建议 `ARCHIVED`，不要复用 `is_delete`，慎用 DISSOLVED） | 让 Team 归属 Division，并支持归档而非删除 |
| `basket_team` 约束 | 建议唯一：同 Division 下队名/活跃队规则；禁止跨 Division 复用同一行 | 阻断「一队打多届」 |
| `basket_team_member` | 建议增加归档态或明确赛事结束后批量 LEFT/ARCHIVED 语义；可选冗余 `division_id` | 成员不再是永久关系 |
| `basket_captain_grant` | 注释/校验对齐：绑定的 team 必须属于同一 division；收尾时批量 EXPIRE | 防跨届 bind |

### 建议修改

| 表 | 改动建议 |
|---|---|
| `basket_team_invitation` | 可选加 `division_id`；归档时取消 PENDING |
| `basket_team_join_application` | 同上 |
| `basket_registration` | 字段可不动；业务上强制 `registration.team.division_id == registration.division_id` |
| `basket_event_team` | 字段可不动；注释把 `source_team_id` 从「长期球队」改为「来源本届球队」 |
| `basket_division` | 落地 FINISHED/ARCHIVED 流转，作为收尾触发点 |
| 新建归档日志表（可选） | 记录 Division 收尾幂等批次 |

### 不需要修改（为保护稳定链路）

| 表 | 原因 |
|---|---|
| `basket_match*` / `basket_match_series*` | 已用 `event_team_id` |
| `basket_standing*` / `basket_team_stat` / `basket_player_stat` / `basket_rank` | 挂赛事快照/赛区统计 |
| `basket_match_submission*` | 赛事比赛提交流程 |
| FormatSnapshot / DivisionStage / ScheduleBatch | 赛程核心，勿动 |
| `basket_event_player` 结构 | 保持快照职责即可 |

---

## 8. 需要修改的后端模块

### Web（高优先）

| 模块 | 类 / API | 改动点 |
|---|---|---|
| 建队 | `AccountTeamServiceImpl.create` | 必须指定/推导 `divisionId`；写入 Team.division_id；禁止无 Division 建队 |
| 继承复活 | `listInheritableTeams` / `bindGrantToTeam` | **删除或改造成「仅复制资料创建新队」**，禁止绑旧 team_id |
| 我的球队 | `pageMine` | 拆当前（ACTIVE+Division未结束）/ 历史（ARCHIVED） |
| 可加入 | `pageJoinable` | 仅当前开放报名的 Division 活跃队，不含历史 |
| 管理门禁 | `CaptainGrantSupport.assertCanManageTeam` 等 | ARCHIVED 一律拒绝写操作 |
| 邀请/申请/成员 | `TeamInvitation*`, `TeamJoinApplication*`, `TeamMember*` | 校验球队 ACTIVE 且 Division 未结束 |
| 报名 | `BasketV4PlayerRegistrationServiceImpl` | team.division_id 必须等于报名 divisionId；禁止历史队报名 |
| 个人成队 | `createForPersonalFormation` | 同样写入 division 归属 |
| 队长资格 | `CaptainPermissionServiceImpl` / `GET .../qualification/current` | 返回按 Division 的当前可建队/可管理信息 |

### Admin（高优先）

| 模块 | 类 / API | 改动点 |
|---|---|---|
| Division 收尾 | **新建** `closeDivision`/`finishDivision`（建议放 `BasketV4DivisionServiceImpl` 或独立 ClosingService） | FINISHED → 过期 grant → 归档 Team/Member → 取消 pending invite/apply |
| 队长授权 | `BasketV4CaptainGrantServiceImpl` | 绑定时校验 team.division_id |
| 报名审核 | `BasketV4RegistrationAuditServiceImpl` | 防御：拒绝历史队/跨 Division 队 |
| 球队认证 | `BasketTeamAuditServiceImpl` | 语义从「永久俱乐部认证」评估是否改为「本届球队资料审核」 |
| 永久档案 | `BasketPermanentArchive*` | 改为读取 ARCHIVED 历史队，避免与「解散删除」混淆 |

### 明确不要动（除非发现硬依赖永久 Team）

赛程生成、RR/Group/SE、BO1/3/5、BYE、Forfeit、Manual Advance、Pairing Adjustment、晋级、Submission、审核结算、Standing/Stat —— 它们已依赖 `event_team_id`。

最小风险点：仅确认 `source_team_id` 在「一届一队」后仍唯一即可（`uk_event_team_division_source_team` 本身已按 division 隔离）。

---

## 9. 需要修改的 Admin 页面

| 页面 | 路径 | 改动 |
|---|---|---|
| 队长授权 | `views/cnpa/event-v4/captain-grant/index.vue` | 文案从「绑定长期球队」改为「绑定本届球队」；展示 team.division |
| 报名审核 | `views/cnpa/event-v4/registration-audit/index.vue` | 展示本届球队；拦截异常跨届数据 |
| 参赛队伍 | `views/cnpa/event-v4/event-team/index.vue` | 明确 EventTeam=正式快照；source team=本届 Team |
| Division 管理/发布 | `views/cnpa/event-v4/**`（division/publish） | 增加「结束赛区 / 归档」操作与结果提示 |
| 球队认证 | `views/cnpa/identity/team-certification/index.vue` | 评估是否按 Division 过滤/展示 |
| 永久档案 | `views/cnpa/discipline/permanent-team/index.vue` | 对齐「历史球队只读」 |

---

## 10. 需要修改的 Web 页面

| 页面 / 组件 | 路径 | 改动 |
|---|---|---|
| 我的球队 | `views/account/team/index.vue` | 拆「当前球队 / 历史球队」；历史只读 |
| Team Store | `stores/team.ts` | 去掉/改造 `canInheritTeam` 复活逻辑；ARCHIVED 禁管理 |
| 创建球队 | 同页 create Tab + `CaptainGrantPanel.vue` | 创建时选择/绑定具体 Division |
| 可加入列表 | `JoinableTeamPanel.vue` | 仅当前赛事开放队 |
| 我的赛事报名 | `views/account/events/V4RegistrationFlow.vue` | 只能用本 Division 的 ACTIVE 队 |
| 个人成队 | `V4PersonalFormationPanel.vue` | 禁止跳到复活长期队；创建本届新队 |
| 侧栏/概览 | `AccountSidebar.vue` / `overview/index.vue` | 恢复或明确「我的球队」入口；文案去「永久队」 |
| 公开球队页 | `views/tournament/team/**` | 评估按历史实例展示，避免把跨届同名队合成一个俱乐部主页 |

相关 API 封装：`api/accountTeam.ts`、`captainQualification.ts`、`teamMember.ts`、`teamInvitation.ts`、`teamJoinApplication.ts`、`v4PlayerEvents.ts`。

---

## 11. 对现有比赛 / 赛程 / BO / 晋级 / 统计的潜在影响

### 低风险（主链路已隔离）

以下已使用 `event_team_id` / 赛事快照，**不依赖「永久 Team」语义**：

- 赛程生成（RR / Group / SE）
- BO1 / BO3 / BO5 / Series
- BYE / Forfeit / Manual Advance / Pairing Adjustment
- 晋级 Qualification
- Match Submission / 审核 / Standing / TeamStat / PlayerStat

只要改造时：

1. **不改** EventTeam 生成时机与主键；  
2. **不改** Match 外键；  
3. 继续由 Registration 审核生成快照；  

则核心赛事能力可保持不动。

### 中风险（需回归，但可局部修）

| 点 | 风险 |
|---|---|
| `source_team_id` 语义 | 从「长期队」变为「本届队」后，公开队史/荣誉若按 source_team 聚合，会从「跨届俱乐部史」变成「单届史」——这恰是新口径，但公开页要改展示 |
| 一人一活跃队槽 | 归档后必须释放 `active_member_slot`，否则无法加入新一届队 |
| 个人成队 PERSONAL | 依赖创建/绑定长期队，需改成创建本届队 |
| 历史 E2E 数据 | 已存在同一 team 跨 Division 报名；迁移需决定：保留为历史例外，或拆分新 team 行并改写 registration/event_team.source_team_id |

### 高风险（若选错方案）

- 试图用 EventTeam **替换** Team 去做邀请/申请（报名前置没有 EventTeam）  
- 试图让 Match 改挂 `basket_team.id`  
- 用 `is_delete=1` 当作历史归档（破坏审计与公开历史）

---

## 12. 推荐的最小改造方案

### 目标模型（推荐）

```text
Division
 ├─ CaptainGrant          （赛事级权限，已有）
 ├─ Team                  （本届球队实体：新建时绑定 division_id）
 │   └─ TeamMember        （本届成员；结束后归档）
 ├─ Registration          （报名申请/审核，保留）
 ├─ EventTeam/EventPlayer （审核通过后的正式参赛快照，保留，供赛程比赛使用）
 ├─ Match / Standing / Stat
 └─ Closing               （Division 结束统一收尾）
```

### 为什么不合并 Team 与 EventTeam

- Team 必须在报名**之前**存在（邀请、申请、凑人数）  
- EventTeam 必须在审核**之后**冻结（赛程稳定性）  
- 合并成本高，且会碰到比赛主链路  
→ **并存，但让 Team 也变成 Division 级**，消除「永久俱乐部」语义

### 最小改造步骤（原则）

1. **Team 绑定 Division**（结构 + 创建写入）  
2. **禁止复活旧 team_id**（删/改 `bind-team` 与 inheritable）  
3. **增加 ARCHIVED 生命周期**（非 delete）  
4. **补 Division 收尾**（过期 grant + 归档 team/member + 取消 pending 邀请申请）  
5. **前端改为当前/历史分区，历史只读**  
6. **Registration / EventTeam / Match 不动主结构**  
7. UX 可后续做「参考历史球队创建」（只复制名称/Logo，可选展示上届成员便于重新邀请；**不复制成员关系，不复用 team_id**）

### 明确不做

- 不重构赛程/BO/晋级/统计  
- 不物理删除历史队  
- 不把队长改回永久制  
- 不把 EventTeam 删掉或与 Team 二合一

---

## 13. 推荐改造顺序

1. **口径冻结评审**（本文）→ 确认采用「Team 绑 Division + 保留 EventTeam」  
2. **数据迁移设计**（只设计不执行也可先评审）  
   - 存量 Team 如何回填 `division_id`（按最近 grant/registration）  
   - 已跨 Division 复用的 team 如何拆分  
3. **DDL**：`basket_team.division_id/season_id` + `ARCHIVED` 状态  
4. **后端门禁**：创建/报名/邀请/申请/成员写操作校验 Division 归属与 ACTIVE  
5. **废除跨届 bind-team 复活**；改为「参考创建」API（可二期）  
6. **Division 收尾 Service**（幂等）：FINISHED → expire grants → archive teams/members → cancel pending invites/applies  
7. **Web「我的球队」当前/历史** + Admin 文案/结束操作  
8. **回归**：  
   - 建队/邀请/申请/报名/审核 → EventTeam  
   - 历史队不可再报名  
   - 新一届同名必须新 team_id  
   - 既有 RR/SE/BO/Submission/Standing E2E 不回归失败  

---

## 附录 A：关键代码地图

### 表

`basket_team`, `basket_team_member`, `basket_team_invitation`, `basket_team_join_application`,  
`basket_captain_grant`, `basket_captain_grant_log`,  
`basket_registration`, `basket_registration_player`, `basket_registration_audit`,  
`basket_event_team`, `basket_event_player`, `basket_division`, `basket_season`

### 后端核心类

- Web：`AccountTeamServiceImpl`, `CaptainPermissionServiceImpl`, `CaptainGrantSupport`, `CaptainGrantValiditySupport`,  
  `TeamInvitationBusinessServiceImpl`, `TeamJoinApplicationBusinessServiceImpl`, `TeamMemberBusinessServiceImpl`,  
  `BasketV4PlayerRegistrationServiceImpl`, `BasketV4PersonalFormationServiceImpl`
- Admin：`BasketV4CaptainGrantServiceImpl`, `BasketV4RegistrationAuditServiceImpl`,  
  `BasketV4EventSnapshotServiceImpl`, `BasketV4DivisionServiceImpl`, `BasketV4ScheduleBatchServiceImpl`,  
  `BasketV4SeasonServiceImpl`, `BasketPermanentArchiveServiceImpl`

### 前端核心

- Web：`views/account/team/index.vue`, `stores/team.ts`, `views/account/events/*`, `api/accountTeam.ts`, `api/v4PlayerEvents.ts`
- Admin：`views/cnpa/event-v4/captain-grant`, `registration-audit`, `event-team`, `identity/team-certification`

---

## 附录 B：潜在漏洞清单（相对新口径）

| ID | 问题 | 现状 |
|---|---|---|
| A | 创建球队不落 division_id | 已确认 |
| B | 我的球队按永久成员关系展示 | 已确认 |
| C | 历史队可被 bind-team 复活并再报名 | 已确认 |
| D | 邀请/申请无 division 维度 | 已确认 |
| E | 同 team 跨 Division 报名 | 库内已有实例 |
| F | Division 结束无统一收尾 | 无 close/finish |
| G | 无 Team ARCHIVED，只有 DISSOLVED/is_delete | 已确认 |
| H | 前端历史队长只读依赖「无授权」，一旦重新绑旧队即恢复写权限 | 已确认 |

---

## 附录 C：一句话总判

当前系统已经用 **Division 级 CaptainGrant + Registration + EventTeam** 跑通了赛事主链路；  
但 **`basket_team` / `basket_team_member` 仍是「永久俱乐部」模型**，并通过 **继承绑定** 允许同一 `team_id` 跨届复活——这与新口径「球队=某届 Division 参赛实例，结束即归档且不可复活」直接冲突。  

**最小正确改法**：让 Team（及成员）也绑定并跟随 Division 生命周期，同时保留 Registration/EventTeam，不动比赛主链路。
