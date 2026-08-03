# CNPA 赛事域 V4 第 3 轮：队长授权 + 球队报名 + 报名审核 + 赛事快照

> 轮次：R3 Registration & Snapshot  
> 日期：2026-08-04  
> 前置：R1 模型落地、R1 DDL 补丁、R1.5 旧域隔离、R2 赛事发布、R2 落库冒烟

---

## 1. 本轮完成内容

完成 V4 赛事域从 **Division 平台赛区** 到 **EventTeam / EventPlayer 正式参赛快照** 的主链路：

```
Division (REGISTRATION_OPEN)
  → CaptainGrant (ACTIVE, divisionId_teamId)
  → Registration (DRAFT → PENDING_REVIEW → APPROVED/REJECTED/WITHDRAWN)
  → RegistrationPlayer (5–8 人阵容)
  → RegistrationAudit (SUBMIT/APPROVE/REJECT/WITHDRAW)
  → EventTeam + EventPlayer 快照（审核通过时生成）
```

**已实现：**

- 后台 V4 队长授权 CRUD（列表 / 新增 / 撤销）
- 后台 V4 报名审核（列表 / 详情 / 通过 / 驳回）
- 后台 V4 赛事队伍/球员快照只读查询
- Web V4 球员报名 API（可报名 Division / 保存 / 提交 / 撤回 / 我的报名）
- Web V4 我的赛事基础 API
- 后台 3 个 V4 页面 + API
- 菜单 SQL + 旧入口隐藏/废弃提示
- 冒烟脚本 `scripts/smoke_v4_r3_registration_snapshot.py`

**未做（按本轮边界）：** 赛程、Match、比赛数据、统计结算、Open/PERSONAL 报名、旧 match 表操作。

---

## 2. 新增 / 修改文件清单

### 2.1 后端 Admin

| 类型 | 路径 |
|------|------|
| 权限 | `basketball-admin/.../constant/BasketEventV4Perms.java` |
| Query | `query/v4/BasketCaptainGrantQuery.java`, `BasketRegistrationAuditQuery.java`, `BasketEventTeamQuery.java`, `BasketEventPlayerQuery.java` |
| DTO | `dto/v4/BasketCaptainGrantCreateReq.java`, `BasketRegistrationAuditRejectReq.java` |
| VO | `vo/v4/BasketCaptainGrantListVo.java`, `BasketRegistrationAudit*.java`, `BasketEventTeamListVo.java`, `BasketEventPlayerListVo.java` |
| Support | `support/v4/BasketV4ParticipationSupport.java` |
| Service | `service/v4/IBasketV4*Service.java` ×5 |
| ServiceImpl | `service/impl/v4/BasketV4CaptainGrantServiceImpl.java`, `BasketV4RegistrationAuditServiceImpl.java`, `BasketV4EventSnapshotServiceImpl.java`, `BasketV4EventTeamServiceImpl.java`, `BasketV4EventPlayerServiceImpl.java` |
| Controller | `web/controller/basket/v4/BasketV4CaptainGrantController.java`, `BasketV4RegistrationAuditController.java`, `BasketV4EventTeamController.java`, `BasketV4EventPlayerController.java` |
| Mapper 扩展 | `mapper/v4/BasketCaptainGrantMapper.*`, `BasketRegistrationMapper.*`, `BasketRegistrationPlayerMapper.*`, `BasketRegistrationAuditMapper.*`, `BasketEventTeamMapper.*`, `BasketEventPlayerMapper.*` |

### 2.2 后端 Web

| 类型 | 路径 |
|------|------|
| Request/Response | `request/v4/V4Registration*.java`, `response/v4/V4AvailableDivisionResp.java`, `V4MyRegistrationResp.java` |
| Mapper | `mapper/v4/BasketV4PlayerRegistrationMapper.java` + `resources/mapper/v4/BasketV4PlayerRegistrationMapper.xml` |
| Service | `service/v4/IBasketV4PlayerRegistrationService.java`, `service/impl/v4/BasketV4PlayerRegistrationServiceImpl.java` |
| Controller | `controller/v4/BasketV4PlayerRegistrationController.java`, `BasketV4PlayerMyEventsController.java` |

### 2.3 前端 Admin UI

| 类型 | 路径 |
|------|------|
| API | `src/api/cnpa/event-v4/captain-grant.ts`, `registration-audit.ts`, `event-team.ts` |
| 页面 | `src/views/cnpa/event-v4/captain-grant/index.vue`, `registration-audit/index.vue`, `event-team/index.vue` |
| 旧页废弃提示 | `cnpa/identity/captain-qualification/index.vue`, `cnpa/participation/team-registration/index.vue`, `official-teams/index.vue`, `event-roster/index.vue`, `open-registration/index.vue` |

### 2.4 脚本 / SQL / 文档

| 文件 | 说明 |
|------|------|
| `docs/sql/cnpa-v4-round3-menu.sql` | V4 R3 菜单与权限 |
| `scripts/smoke_v4_r3_registration_snapshot.py` | API 冒烟脚本 |
| `scripts/generate_v4_r3_backend.py` | 生成器占位（Perms 已写入） |

---

## 3. 新增后端接口清单

### 3.1 后台 Admin

| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| GET | `/basket/v4/captain-grant/list` | `basket:v4CaptainGrant:list` | 队长授权分页 |
| POST | `/basket/v4/captain-grant` | `basket:v4CaptainGrant:add` | 新增授权 |
| PUT | `/basket/v4/captain-grant/{id}/revoke` | `basket:v4CaptainGrant:revoke` | 撤销授权 |
| GET | `/basket/v4/registration-audit/list` | `basket:v4RegistrationAudit:list` | 报名审核分页 |
| GET | `/basket/v4/registration-audit/{registrationId}` | `basket:v4RegistrationAudit:query` | 报名详情+阵容+流水 |
| POST | `/basket/v4/registration-audit/{registrationId}/approve` | `basket:v4RegistrationAudit:approve` | 审核通过+生成快照 |
| POST | `/basket/v4/registration-audit/{registrationId}/reject` | `basket:v4RegistrationAudit:reject` | 审核驳回 |
| GET | `/basket/v4/event-team/list` | `basket:v4EventTeam:list` | EventTeam 分页 |
| GET | `/basket/v4/event-team/{id}` | `basket:v4EventTeam:query` | EventTeam 详情 |
| GET | `/basket/v4/event-player/list` | `basket:v4EventPlayer:list` | EventPlayer 分页 |

### 3.2 Web

| Method | Path | 说明 |
|--------|------|------|
| GET | `/basket/v4/player/registration/available-divisions` | 可报名 Division |
| POST | `/basket/v4/player/registration/save` | 保存/更新报名草稿 |
| POST | `/basket/v4/player/registration/submit` | 提交报名 |
| POST | `/basket/v4/player/registration/withdraw` | 撤回报名 |
| GET | `/basket/v4/player/registration/my-list` | 我的报名 |
| GET | `/basket/v4/player/my-events` | 我的赛事（基础，返回我的报名） |

---

## 4. 新增前端页面 / API 清单

| 菜单 | 路由 component | 页面路径 |
|------|----------------|----------|
| V4队长授权 | `cnpa/event-v4/captain-grant/index` | `basketball-admin-ui/src/views/cnpa/event-v4/captain-grant/index.vue` |
| V4报名审核 | `cnpa/event-v4/registration-audit/index` | `basketball-admin-ui/src/views/cnpa/event-v4/registration-audit/index.vue` |
| V4赛事队伍 | `cnpa/event-v4/event-team/index` | `basketball-admin-ui/src/views/cnpa/event-v4/event-team/index.vue` |

---

## 5. CaptainGrant 授权流程说明

1. 管理员选择 **Division**（须已发布且建议 REGISTRATION_OPEN）、**长期球队**、**已认证球员**。
2. 校验：球员属于球队有效成员；球员有 Division.platformCode 对应平台身份；同一 division+team 无其他 ACTIVE 授权。
3. 写入 `basket_captain_grant`：`grant_status=ACTIVE`，`active_unique_key=divisionId_teamId`。
4. 撤销：仅 ACTIVE 可撤销 → `REVOKED` + `active_unique_key=NULL`。
5. 过期：`validTo` 过期在查询/校验时视为不可用（本轮无自动 Job）。

---

## 6. Registration 报名流程说明

1. Web 端登录用户须有关联 **已认证球员**。
2. 须存在 **ACTIVE 且未过期** 的 CaptainGrant，且 teamId 与授权一致。
3. **save**：阵容 5–8；成员来自球队有效 roster；须有平台身份；同 division+team 复用一条 Registration；APPROVED 不可编辑。
4. **submit**：DRAFT / REJECTED / WITHDRAWN → `PENDING_REVIEW`，写 audit `SUBMIT`。
5. **withdraw**：仅 PENDING_REVIEW → `WITHDRAWN`，写 audit `WITHDRAW`，可复用同条报名再提交。

---

## 7. RegistrationAudit 审核流程说明

1. 后台列表筛选 season / division / status / 队名 / 队长。
2. 详情展示主信息、阵容、审核流水。
3. **approve**：仅 PENDING_REVIEW；再次校验 Division 名额（只计 APPROVED）；写 audit `APPROVE`；调用快照服务。
4. **reject**：仅 PENDING_REVIEW；写 audit `REJECT`；不生成快照；可复用原报名再提交。

---

## 8. EventTeam / EventPlayer 快照生成说明

审核通过时 `BasketV4EventSnapshotServiceImpl`：

1. 校验 division 下无重复 EventTeam（sourceTeamId）/ EventPlayer（sourcePlayerId）。
2. 从长期球队复制队名、Logo、简称、队长信息 → `basket_event_team`（绑定 division_id）。
3. 从 `basket_registration_player` 复制球员快照 → `basket_event_player`（绑定 division_id、event_team_id）。
4. 快照生成后不受长期球队/球员后续变更影响。

---

## 9. 名额占用规则说明

- **审核中（PENDING_REVIEW）不占** Division.max_team_count。
- **仅 APPROVED** 计入 `countApprovedByDivisionId`。
- 审核通过时若 `approvedCount >= maxTeamCount` 拒绝通过。
- `remainingTeamCount = maxTeamCount - approvedCount`（Web 可报名列表展示）。

---

## 10. 阵容 5–8 校验说明

- 默认使用 Division.lineupMinCount / lineupMaxCount（R2 创建时默认 5–8）。
- save / buildLineup 时：少于 min 或多于 max 拒绝；队长必须在阵容中；球员不可重复。

---

## 11. 平台身份校验说明

- Division.platformCode（PC/PS/XBOX）与 `basket_player_platform_account.platform` 经 `GameTerminalSupport.normalize` 对齐。
- 要求 `account_status=1`（有效平台账号）。
- 授权、报名、审核均复用同一套校验逻辑（Admin Support + Web Service）。

---

## 12. 旧入口隐藏 / 废弃处理结果

| 旧入口 | menu_id | 处理 |
|--------|---------|------|
| 队长资格审核 | 2002 | 菜单 hidden + 页面顶部废弃 Alert |
| 球队报名审核 | 2007 | 菜单 hidden + 页面废弃 Alert |
| 正式赛事球队 | 2221 | 菜单 hidden + 页面废弃 Alert |
| 赛事阵容管理 | 2222 | 菜单 hidden + 页面废弃 Alert |
| Open 报名审核 | 无菜单 | 页面废弃 Alert（原本无菜单） |

**后续删除建议：** 第 5–6 轮删除旧 Service/页面/SQL 种子。

---

## 13. 新旧代码隔离检查结果

对 `service/impl/v4`、`controller/basket/v4`、`service/impl/v4`（web）检索：

- **未发现** `eventId` / `tournamentId` / `competitionSeasonId` / `BasketTournament*` / `CaptainQualification` / `TournamentRegistration` / `SeasonRoster` 引用。
- V4 业务 **未调用** 旧 `IBasketCaptainQualificationService`、`IBasketRegistrationAuditService`、`IBasketParticipationGenerationService` 等。
- 合法复用：**账号域** Mapper（Team / Player / PlatformAccount）用于成员与认证校验，不涉及旧赛事表。

---

## 14. API 冒烟测试记录

脚本：`scripts/smoke_v4_r3_full_acceptance.py`（完整补验收，28/28 PASS）

| 步骤 | 结果 | 说明 |
|------|------|------|
| Admin + Web 登录 | **PASS** | 8080 + 8881 |
| 测试数据补全 | **PASS** | Season 2 发布、Division 3 开放、Quota Division 5 |
| 主链路 9 步 | **PASS** | Web 报名 + Admin 审核 + 快照 |
| 拒绝场景 10 项 | **PASS** | 见补验收记录 |
| 名额边界 3 项 | **PASS** | maxTeamCount=2 |
| 旧入口/隔离 | **PASS** | 菜单 hidden + 页面 Alert |

**前置：** 需启动含 R3 Controller 的 `basketball-admin` 与 `basketball-web`；Division 报名时间窗须覆盖当前时间。

---

## 15. 风险与待确认项

1. ~~**Web 冒烟**~~ — **已补验**（`smoke_v4_r3_full_acceptance.py`）。
2. **CaptainGrant 页面** 当前新增表单使用 ID 输入，后续可接球队/球员选择器（非本轮必须）。
3. **jerseyNo** 未从 team_member 带入（成员表无 position，取自 player_profile.positionCode）。
4. **名额并发** 审核通过时已校验 approvedCount，高并发下建议 R4 前加 division 行锁。
5. **OFFLINE Season** API 无法重新发布，测试环境需 SQL 补发布或新建 Season。

---

## 16. 是否建议进入第 4 轮

**建议：可以进入第 4 轮（赛程生成）。**

1. ~~完整冒烟~~ — **2026-08-04 已通过真实 API 冒烟**（见下方补验收记录）。
2. 确认 menu SQL 已在各环境执行。
3. 第 4 轮基于 **EventTeam** 快照生成 Schedule/Match，仍不操作旧 match 同名表。

---

## 附录：菜单 ID 分配（2296–2308）

| menu_id | 名称 |
|---------|------|
| 2296 | V4队长授权 (C) |
| 2297–2299 | CaptainGrant 按钮权限 |
| 2301 | V4报名审核 (C) |
| 2302–2304 | RegistrationAudit 按钮权限 |
| 2306 | V4赛事队伍 (C) |
| 2307–2308 | EventTeam/EventPlayer 查询权限 |

---

---

---

## 第 3 轮真实冒烟补验收记录

> 验收时间：2026-08-04 01:21:37  
> 目标库：`192.168.31.47/basketball`  
> 脚本：`scripts/smoke_v4_r3_full_acceptance.py`  
> Admin：`http://127.0.0.1:8080` | Web：`http://127.0.0.1:8881`

> 结果：**28/28 PASS**

### 测试数据

| 项 | 值 |
|----|-----|
| Season | id=**2**（SMOKE_S2），`PUBLISHED`（OFFLINE 经 SQL 补发布） |
| Division（主链路） | id=**3**（SMOKE S2 - PC），`REGISTRATION_OPEN` |
| Division（名额） | id=**5**（R3_QUOTA_TEST_PC），`maxTeamCount=2` |
| 球队 | Alpha `2083464934804459521`；拒绝/名额用 Beta、Gamma |
| 队长 | playerId=`2082478335308455940`，email=`111111@qq.com` |
| 阵容 | **5** 人，PC 平台身份齐全 |
| Web 登录 | `POST /basket/member/auth/login/password`，密码 `111111aa` |

**补数据：** Season 2 发布；Division 3 开放并修正报名时间窗；新建 Season 4 + Division 5；旧菜单 hidden；复用 CNPA 流水线球队与球员。

### 主链路结果

| 步骤 | 结果 | 说明 |
|------|------|------|
| CaptainGrant 创建 | PASS | grantId=33 status=ACTIVE key=3_2083464934804459521 |
| Web 可报名 Division | PASS | canRegister=True |
| Web 保存报名草稿 | PASS | regId=27 status=DRAFT players=5 |
| Web 提交报名 | PASS | status=PENDING_REVIEW audits=['SUBMIT'] |
| 后台审核详情 | PASS | regId=27 players=5 audits=1 |
| 后台审核驳回 | PASS | status=REJECTED eventTeam=0 audits=['SUBMIT', 'REJECT'] |
| Web 驳回后重提 | PASS | sameRegId=27 status=PENDING_REVIEW submitCount=2 |
| 后台审核通过+快照 | PASS | status=APPROVED eventTeam=1 eventPlayer=5 audits=['SUBMIT', 'REJECT', 'SUBMIT', 'APPROVE'] |
| V4 赛事队伍查询 | PASS | eventTeamTotal=1 eventPlayerTotal=5 |

### EventTeam / EventPlayer

| 指标 | 结果 |
|------|------|
| EventTeam 生成 | **1** 条（divisionId=3） |
| EventPlayer 生成 | **5** 条（与阵容一致） |
| division_id 绑定 | 通过（EventTeam/EventPlayer 均绑定 divisionId=3） |

### 拒绝场景

| # | 场景 | 结果 |
|---|------|------|
| 1 | 无 ACTIVE CaptainGrant 不能保存 | PASS |
| 2 | 阵容少于 5 人 | PASS |
| 3 | 阵容多于 8 人 | PASS |
| 4 | 成员不属于长期球队 | PASS |
| 5 | 无对应平台身份 | PASS |
| 6 | 重复 ACTIVE 授权 | PASS |
| 7 | 同 Division 球员不可进另一 EventTeam | PASS |
| 8 | APPROVED 不可编辑 | PASS |
| 9 | 审核中不占名额 | PASS |
| 10 | APPROVED 占名额 | PASS |

### 名额边界

| 用例 | 结果 | 说明 |
|------|------|------|
| 审核中不占名额 | PASS | approvedCount=0 |
| APPROVED 占名额 | PASS | approvedCount=1 |
| 满额后审核拒绝 | PASS | POST /basket/v4/registration-audit/31/approve code=500 msg=该 Division 报名名额已满 |

### 旧入口与隔离

| 检查项 | 结果 |
|--------|------|
| 旧队长资格/报名审核/正式球队/阵容菜单 hidden | PASS |
| 旧页面废弃 Alert | PASS |
| Open 报名页面废弃提示 | PASS |
| V4 未引用旧 Service | PASS |
| V4 未写旧授权/报名/阵容表 | PASS（仅 basket_captain_grant / basket_registration* / basket_event_*） |

### 结论

| 项 | 结论 |
|----|------|
| R3 补验收 | **通过** |
| 主链路真实跑通 | 是 |
| 阻断问题 | 无 |
| 是否建议进入第 4 轮 | **可以**（主链路+拒绝+名额均已验证） |
