# CNPA 球队生命周期改造 — 5 个关键技术点确认

> 日期：2026-08-10  
> 性质：**READ ONLY**（未改代码 / SQL / 数据 / 配置）  
> 依据：当前 Java/Vue 实现 + MySQL 真实查询结果  
> 前置文档：`docs/cnpa-team-lifecycle-alignment-analysis-20260810.md`

---

## 一、CaptainGrant 与 Team 关系确认

### 1. 当前实现

| 维度 | 现状 |
|---|---|
| 表字段 | `basket_captain_grant.team_id` 可空，语义为「后绑定球队」 |
| 基数（schema） | **一个 Grant → 0..1 Team**（单列，不可能存多个 team_id） |
| Division×Player 唯一 | `uk_captain_grant_active_unique_key(active_unique_key)`；ACTIVE 时 key=`{divisionId}_{playerId}` |
| Admin 创建 | `BasketV4CaptainGrantServiceImpl.create`：先查 `selectActiveByDivisionPlayer`，已有可用授权则拒绝；`team_id=null` 插入 |
| Web 建队绑定 | `AccountTeamServiceImpl.create` → `CaptainGrantSupport.bindGrantToTeam` |
| 绑定 SQL | Admin `bindTeamById`：`where team_id is null`；Web `CaptainGrantSupport.bindGrantToTeam` 同样 `isNull(teamId)` 更新 |
| 反向 | **多个 Grant 可以指向同一个 Team**（库内已有：同一 team 跨 2 个 division 各有 grant） |

相关类：

- `BasketV4CaptainGrantServiceImpl` / `BasketV4CaptainGrantController` → `/basket/v4/captain-grant`
- `CaptainGrantSupport.bindGrantToTeam` / `findCurrentGrant` / `findActiveGrantForDivision`
- `CaptainPermissionServiceImpl.requireValidQualification` / `buildQualificationDetail`
- `AccountTeamServiceImpl.create` / `bindGrantToTeam`

### 2. 是否存在「一 Grant 多 Team」漏洞

**正常串行路径：基本不能。**

连续 `POST /basket/team/create` 创建多队会被挡住，原因不是 `create()` 开头检查了 `grant.teamId == null`，而是：

1. `teamMembershipSupport.assertEligibleToJoinTeam(memberId)`  
   → 全局已有 ACTIVE 成员则拒绝（建队后队长已入队）
2. 队长不能离队：`TeamMemberBusinessServiceImpl.leave` →「队长不能直接离队」
3. 前端门禁：`canCreateTeam = runtimeValid && !hasActiveLongTermTeam(...)`（`CaptainPermissionServiceImpl.buildQualificationDetail`）
4. 若绕过成员校验、grant 已绑定：`bindGrantToTeam` 发现 `grant.teamId != 新队` → 抛「该赛事队长授权已用于其他球队」

**残留风险（非主路径）：**

| 风险 | 说明 |
|---|---|
| `create()` 未显式断言 `grant.teamId == null` | 依赖成员槽 + bind 副作用 |
| 并发双创建竞态 | 两请求同时看到 `team_id=null`、无成员；两个 Team 可能插入成功；DB 上只有一个 grant.team_id 被 CAS 写上，另一个成孤儿队；`bindGrantToTeam` **未检查 update 行数** |
| `findCurrentGrant` 取「任意最新 ACTIVE grant」 | 多 Division 同时有授权时，建队绑到哪一届不明确 |

**结论：** 业务上接近 1 Grant : 1 Team，但 **DB 未强制「已绑定不可再建」**；真正硬约束是 `active_unique_key`（1 Division×Player 仅 1 份 ACTIVE Grant）+ 全局一人一队成员槽。

### 3. 推荐约束

与新口径一致，建议：

```text
1 Division × 1 Player → 最多 1 份 ACTIVE CaptainGrant   （已有 uk + 业务校验）
1 CaptainGrant → 最多 1 个 Team                         （字段已是 0..1，需加固）
1 Team → 恰好 1 个 division_id（改造后）                 （待加）
禁止历史 Team 再 bind 新 Grant                          （废除 bind-team 复活）
```

**数据库 + 业务校验同时使用：**

| 层 | 做法 |
|---|---|
| DB | 保留 `uk_captain_grant_active_unique_key`；可选新增 `uk_grant_bound_team` 类唯一（仅当需要「一队只被一个 ACTIVE grant 绑定」时，需小心历史多 grant 指向同队的存量） |
| 业务 | `create()` 开头强制 `grant.teamId == null`；`bindGrantToTeam` 检查 `update == 1`；废除/改造 `POST /basket/team/captain/grant/bind-team` |

存量已有「多 Grant → 同 Team」：不宜立刻加「team_id 全局唯一」，否则迁移先炸。新规则可用：`Team.division_id == Grant.division_id` + 创建时 grant 未绑定。

### 4. 最小修改点（仅分析）

| 位置 | 改动 |
|---|---|
| `AccountTeamServiceImpl.create` | 显式 `if (grant.getTeamId() != null) reject`；写入 `team.division_id = grant.divisionId` |
| `CaptainGrantSupport.bindGrantToTeam` | 校验 update 行数；禁止绑 ARCHIVED team；校验 division 一致 |
| `CaptainPermissionServiceImpl.buildQualificationDetail` | `canCreateTeam` 改为 `runtimeValid && grant.teamId==null`（不再只靠全局成员） |
| `AccountTeamController` `bind-team` / `inheritable/list` | 删除或改为「复制资料建新队」 |
| `BasketV4CaptainGrantServiceImpl` | 已有 division×player 校验，可保留；绑定时校验 team.division |
| Mapper | 已有 `bindTeamById`；可选批量 `expireByDivisionId` 供 Closing 使用 |

---

## 二、TeamMember 同 Division 一人一队

### 1. 当前 `active_member_slot` 实现

| 项 | 真实实现 |
|---|---|
| 字段 | `basket_team_member.active_member_slot` VARCHAR(64) NULL |
| 唯一索引 | `uk_team_member_active_member_slot (active_member_slot)` |
| 生成 | `TeamMembershipSupport.buildActiveMemberSlot(memberId)` → **`String.valueOf(memberId)`** |
| 写入 | `applyActiveMemberSlot` → 建队/入队时设置 |
| 释放 | `clearActiveMemberSlot` → `UpdateWrapper` 置 `NULL`（并清 `active_platform_slot`） |
| 业务注释 | 「一个用户当前最多加入一支有效球队，**不按平台区分**」 |
| 样本 | `active_member_slot = "2081010979604316162"`（等于 member_id） |

另有：`uk_team_member_active_platform_slot(active_platform_slot)`（旧平台槽，入队时一并清理）。

入队路径：

- 建队：`AccountTeamServiceImpl.create` / `createForPersonalFormation`
- 邀请接受 / 申请通过：`TeamMemberJoinHelper.addMember` → `assertEligibleToJoinTeam` → apply slot → insert（`DuplicateKeyException` → 「已加入球队」）
- 离队/踢人：`TeamMemberBusinessServiceImpl` → status LEFT/REMOVED + `clearActiveMemberSlot`

### 2. 当前唯一约束

```text
uk_team_member_active_member_slot (active_member_slot)
  → 全局同一时刻，一个 memberId 只能占一个非 NULL slot
  → 语义 = 全局一人一队
```

MySQL 允许多行 `NULL`，故离队后可再加入。

### 3. 方案 A / B / C 对比

| 方案 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| **A** | member 冗余 `division_id` + 唯一约束思路 | Closing 批量好写；查询直观 | 需背填；部分唯一在 MySQL 仍常靠 slot/NULL 模式 |
| **B** | 不冗余，运行时 `team_id → team.division_id` | 无冗余 | **无法用 DB 唯一索引直接保证**；并发靠应用锁，不安全 |
| **C** | 继续用 `active_member_slot`，值改为 `{divisionId}_{memberId}`，归档/离队置 NULL | **复用现有唯一索引与释放模式**；改动面小；并发安全（唯一键） | 依赖 Team 先有 `division_id`；slot 字符串约定要统一 |

### 4. 最推荐方案

**推荐：C 为主，A 为辅（轻量冗余）。**

1. **必须先有** `basket_team.division_id`  
2. **slot 改为** `divisionId + "_" + memberId`（与 grant 的 `divisionId_playerId` 风格一致；成员侧用 memberId 因现网 slot 已按 member）  
3. **可选**在 `basket_team_member` 冗余 `division_id`（入队时从 team 拷贝），便于 Division Closing 批量更新，避免大 join  
4. **不要用纯 B** 作为唯一手段  

业务校验同步改：

- `TeamMembershipSupport.assertEligibleToJoinTeam(memberId)` → `assertEligibleToJoinTeam(memberId, divisionId)`  
- `findActiveMembership` 按 division 查  

### 5. 归档后如何释放

建议状态配合：

| 字段 | ACTIVE 成员 | 离队 LEFT | 被踢 REMOVED | Division 结束归档 |
|---|---|---|---|---|
| `membership_status` | ACTIVE(1) | LEFT(0) | REMOVED(2) | 建议 **LEFT 或新 HISTORICAL**；若不想扩枚举，用 LEFT + leave_reason=`DIVISION_FINISHED` |
| `is_current` | 1 | 0 | 0 | **0** |
| `active_member_slot` | `{div}_{member}` | **NULL** | **NULL** | **NULL（必须）** |
| `leave_time` | null | 设置 | 设置 | 设置（收尾时间） |

**ARCHIVED 是 Team 状态，不是 Member 状态。**  
Member 归档 = 历史化行 + 释放 slot；不要用 `is_delete=1`。

释放必须走 `UpdateWrapper` 置 NULL（现有 `clearActiveMemberSlot` 已正确处理 MP 忽略 null 问题）。Closing 批量可用：

```sql
UPDATE basket_team_member m
JOIN basket_team t ON t.id = m.team_id
SET m.active_member_slot = NULL,
    m.active_platform_slot = NULL,
    m.is_current = 0,
    m.membership_status = 0,  -- LEFT
    m.leave_time = NOW(),
    m.leave_reason = 'DIVISION_FINISHED'
WHERE t.division_id = ? AND m.active_member_slot IS NOT NULL;
```

另：`basket_player_profile.current_team_id` 是**全局单值**指针（`TeamMemberJoinHelper.updatePlayerCurrentTeam`）。多 Division 并行参赛时该字段语义失效，需改为「不依赖 / 或改为当前主队展示字段」，否则会误导。

---

## 三、DISSOLVED / ARCHIVED

### 1. 当前 DISSOLVED 所有真实调用

| 位置 | 用途 |
|---|---|
| `TeamStatusEnum.DISSOLVED(2,"已解散")` | Admin/Web 枚举定义 |
| `dissolved_time` 字段 | Entity/DTO/Mapper 映射 |
| Admin UI `team-certification/index.vue` | 文案：「球队已解散或不可用」（`teamActive` 校验失败时） |
| 业务 Service | **未发现** `setTeamStatus(DISSOLVED)` / `dissolveTeam` / 解散 API |
| 库数据 | `team_status=2`：**0 行**；`dissolved_time IS NOT NULL`：**0 行**；45 支队全是 ACTIVE(1) |

搜索 `DISSOLVED` / `dissolve` / `解散球队`：无真实写路径，只有枚举与展示残留。

### 2. 是否存在主动解散流程

**不存在。**

- Web：无解散按钮/API  
- Admin：无解散运营接口  
- 各阶段（未报名 / 审核中 / 已通过 / 参赛中）均无 DISSOLVED 分支  

踢人/离队只改 `basket_team_member`，不改 `team_status`。

### 3. 推荐状态语义

与业务口径对齐，建议：

| 状态 | 含义 | 典型触发 |
|---|---|---|
| `ACTIVE` | 本届有效球队 | 创建成功 |
| `ARCHIVED` | Division **正常结束**后的历史球队 | `finishDivision` |
| `DISSOLVED` | Division **未结束**时提前解散/弃队（尚未进入或未走赛事退赛） | 队长/Admin 主动解散（未来可做） |
| `DISABLED` | 运营禁用（已有 0） | Admin 风控 |

已 `Registration APPROVED` / 已有 `EventTeam` 后：

- **不要**用 Team=DISSOLVED 代替退赛  
- 应走赛事侧：取消资格 / Forfeit / 处罚等（现有比赛链路）  
- Team 可仍保持 ACTIVE 直至 Division FINISHED 再 ARCHIVED；或标记「不可再管理」但保留参赛身份由 EventTeam 承载  

### 4. 是否保留 DISSOLVED

**保留枚举值，重新定义语义；当前无生产占用，改造成本低。**

- 不废弃：避免魔法数字 2 被别处误解后误用  
- 不改名：Admin/Web 枚举已存在  
- **新增 ARCHIVED**（建议新 code，如 3，或改为 VARCHAR 状态码——若改类型成本高，用 3=ARCHIVED 最小）  
- 注意：现 `team_status` 是 TINYINT；加 ARCHIVED=3 最省事  

---

## 四、Division Closing

### 1. 建议 Service 位置

Admin 侧新建（推荐）：

```text
com.zhs.basket.service.impl.v4.BasketV4DivisionClosingServiceImpl
```

入口：

```text
BasketV4DivisionController
POST /basket/v4/division/{id}/finish
```

（或挂在现有 `BasketV4DivisionServiceImpl.finishDivision`，内部委托 ClosingService）

不要放 Web 模块。不要塞进 ScheduleBatch。

### 2. 事务边界

**建议：强事务包住整次收尾（单 Division 数据量通常很小）。**

同一 `@Transactional(rollbackFor = Exception.class)` 覆盖：

1. Division 状态 CAS → FINISHED  
2. CaptainGrant 批量 EXPIRED（清 `active_unique_key`）  
3. Team 批量 ARCHIVED  
4. TeamMember 历史化 + slot=NULL  
5. Pending Invitation → EXPIRED/CANCELLED  
6. Pending JoinApplication → CANCELLED/REJECTED  

不建议「主事务 + 异步补偿」作为第一版：当前无现成可靠 outbox；半成功比整单回滚更难修。

若未来单 Division 球队极多再拆分；以现状（跨 Division 污染队也只有个位数～十几）不必要。

### 3. 幂等方案

推荐与现有 `revokeById`/`expireById` 风格一致的 **状态机 CAS**：

```sql
UPDATE basket_division
SET division_status = 'FINISHED', update_time = NOW()
WHERE id = ? AND is_delete = 0
  AND division_status IN ('SCHEDULED','PLAYING', ...); -- 允许进入 FINISHED 的前置态
```

- `affected == 0`：若当前已是 FINISHED → **直接 return success（幂等）**  
- 若是非法态（如仍 DRAFT）→ 业务错误  
- 子步骤全部做成幂等：  
  - Grant：`WHERE division_id=? AND grant_status='ACTIVE'`  
  - Team：`WHERE division_id=? AND team_status=ACTIVE`  
  - Member/Invite/Apply：按 PENDING/ACTIVE 过滤  

重复点击第二次应几乎 no-op 并成功。

### 4. 失败回滚

强事务下任一步异常 → 整单回滚，避免「Division 已 FINISHED 但 Member 未释放」。

涉及 Mapper（建议）：

| 表 | Mapper（Admin） |
|---|---|
| `basket_division` | `BasketDivisionMapper` |
| `basket_captain_grant` | `BasketCaptainGrantMapper`（扩展 `expireByDivisionId`） |
| `basket_captain_grant_log` | 现有 log support（可批量记一条 DIVISION_FINISH） |
| `basket_team` | `BasketTeamMapper`（扩展 `archiveByDivisionId`） |
| `basket_team_member` | 需 Admin 侧补 Mapper/XML 或复用通用更新 |
| `basket_team_invitation` | 同上 |
| `basket_team_join_application` | 同上 |

Web 表目前多在 Web 模块 Dao；Closing 在 Admin，需确认 Admin 是否已有对应 Mapper——**这是改造时要补的基础设施点**。

### 5. 是否需要 closing log

**不建议新建 `division_closing_log`。**

已有足够追溯：

- `basket_captain_grant_log`（授权变更）  
- Division `update_time` / `division_status`  
- 若依操作日志（Admin 按钮操作）  
- Team/Member 自身状态与 `leave_reason`

不为「看起来完整」加表。若以后要审计批次，再加。

### 6. 需要涉及的表 / Mapper / Service

```text
表：
  basket_division
  basket_captain_grant (+ log)
  basket_team
  basket_team_member
  basket_team_invitation
  basket_team_join_application

Service：
  BasketV4DivisionClosingServiceImpl（新）
  BasketV4DivisionServiceImpl（入口委托）
  CaptainGrantRuntimeSupport / GrantLogSupport（复用过期写法）

不碰：
  basket_match*, standing*, event_team/event_player（保持只读历史）
  registration 终态可不动（APPROVED 保留作历史报名）
```

---

## 五、存量跨 Division Team 数据

> 以下均为只读 `SELECT` 真实结果（库：当前 MCP 连接库）。

### 1. Registration 统计

| 指标 | 值 |
|---|---|
| 跨 Division 的 team 数 | **5** |
| 这些队涉及的 registration 行数 | **10** |
| 单队最大跨 Division 数 | **2** |

前 20（实际仅 5 条）：

| team_name | division_ids | registration_count | statuses |
|---|---|---|---|
| CNPA战队Beta | 5,118 | 2 | APPROVED,CANCELLED |
| CNPA战队Gamma | 5,118 | 2 | APPROVED,PENDING_REVIEW |
| CNPA流水线969123-A | 86,118 | 2 | APPROVED |
| CNPA流水线969123-B | 86,118 | 2 | APPROVED |
| CNPA流水线969123-C | 86,118 | 2 | APPROVED |

说明：`division_id=5` 在 `basket_division` **已不存在**（孤儿报名引用）；Alpha 在 grant 跨 Division 列表中，registration 侧 division 5 为 CANCELLED。

### 2. CaptainGrant 统计

| 指标 | 值 |
|---|---|
| 跨 Division 的 team 数 | **8** |
| 这些队涉及的 grant 行数 | **25** |
| 单队最大跨 Division 数 | **2** |

典型：Alpha/Beta/Gamma（5,118，含 ACTIVE+REVOKED）；流水线 A–E（86,118，多为 ACTIVE）。

### 3. EventTeam 统计（最关键）

| 指标 | 值 |
|---|---|
| 跨 Division 的 `source_team_id` 数 | **3** |
| 对应 event_team 行数 | **6** |
| 单队最大跨 Division 数 | **2** |
| 名称形态 | 6/6 均为 `CNPA流水线969123-*` |

正式参赛且两边都有比赛（样本）：

| team | div 86 match_cnt | div 118 match_cnt |
|---|---|---|
| 流水线-A | 3 | 7 |
| 流水线-B | 3 | 15 |
| 流水线-C | 3 | 10 |

→ **同一长期 Team 已在两个 Division 生成 EventTeam 并打过比赛。**  
但全部落在验收/Smoke Division，不是业务联赛品牌队。

### 4. 测试数据 / 真实数据区分

| Division | 名称 | 判断 |
|---|---|---|
| 86 | `CNPA_V2_RELEASE_GATE_RR4_20260806-PC-5V5-RR4` / Season「上线前 4 队单循环验收」 | **测试/验收** |
| 118 | `R2_4_MATCH_SERIES_SMOKE` / Season「CNPA League 2026 S9」 | **测试/Smoke**（名称含 SMOKE） |
| 5 | 报名残留，Division 行不存在 | **历史测试残留/脏数据** |

跨 Division 污染队命名：`CNPA战队Alpha/Beta/Gamma`、`CNPA流水线969123-*` → **判定为测试数据**。  
本库抽样中，**未发现**「非测试命名」的跨 Division EventTeam。

### 5. 典型样本

```text
source_team 流水线-A
  ├─ EventTeam@86 (id=120, matches=3)   ← RELEASE_GATE
  └─ EventTeam@118 (id=126, matches=7)  ← MATCH_SERIES_SMOKE
```

比赛挂的是各自 `event_team_id`，不是 `basket_team.id`。

---

## 六、存量迁移方案

### 1. Legacy 兼容方案（方案 A）

做法：

- 老 Team：`division_id` 允许 NULL，或 `lifecycle_mode=LEGACY`
- 新创建 Team：强制 `division_id NOT NULL` + 新唯一规则
- **不拆** `source_team_id`，不改写 Match

| 优点 | 缺点 |
|---|---|
| 零风险触碰历史比赛 | 长期存在双轨模型 |
| 不动 EventTeam/Match | 公开「队史」若按 team_id 聚合，LEGACY 队仍像俱乐部 |
| 改造可立刻上新规则 | 唯一约束要对 NULL/LEGACY 放行，设计要小心 |
| 存量跨 Division 全是测试数据，债很小 | 需产品接受 LEGACY 只读展示 |

### 2. 拆 Team 方案（方案 B）

按 Division 克隆 Team，改写：

`registration.team_id` / `grant.team_id` / `team_member` / invite / apply / **`event_team.source_team_id`**

| 优点 | 缺点 |
|---|---|
| 数据模型最干净 | 改写面大；`source_team_id` 变更需回归所有按 source 聚合的公开页 |
| | Match 虽不改 event_team_id，但队史/荣誉/档案若 join source_team 会碎 |
| | 对已是测试数据的 3 个跨 Division 队，收益低、风险不低 |

**EventTeam 本身已有完整快照**（队名/Logo/队长）。历史比赛展示不依赖拆 `source_team_id`。  
因此 **没有强理由为了洁癖去改 source_team_id**。

### 3. 风险对比

| 优先级 | Legacy 兼容 | 拆 Team |
|---|---|---|
| 不破坏历史比赛 | ✅ | ⚠️ 间接风险 |
| 不破坏 EventTeam/Match | ✅ | ⚠️ 若改 source |
| 不大规模改写 | ✅ | ❌ |
| 新业务完全新规则 | ✅（新数据） | ✅ |
| 区分 Legacy/新数据 | ✅ 需标志位 | ✅ 但成本高 |

### 4. 最终推荐方案

**推荐：Legacy 兼容（方案 A），不对跨 Division 存量做拆队重写。**

具体策略：

1. 新增 `basket_team.division_id`（允许 NULL）+ 建议 `team_lifecycle`/`data_version` 或简单约定「`division_id IS NULL` = LEGACY」  
2. 新创建必须带 division_id；新唯一/成员槽规则只约束非 LEGACY  
3. 存量跨 Division 队（当前均为测试）可：  
   - 保持不动；或  
   - 测试库直接清理；生产若确认无业务依赖，可不迁  
4. **禁止**改写历史 `basket_event_team.source_team_id` 与 Match  
5. 公开历史优先展示 EventTeam 快照；Team 页对 LEGACY 标注「历史兼容数据」  
6. 同步处理全局队名唯一：`uk_team_name_delete(team_name,is_delete)` + `assertTeamNameUnique` **必须改为 Division 范围**，否则新口径「同名不同届」直接撞车  

---

## 七、其他唯一约束冲突

「一个球员可同时加入不同 Division 的球队」——会挡住的现有约束：

| 约束 | 位置 | 影响 |
|---|---|---|
| **`uk_team_member_active_member_slot`** slot=`memberId` | DB + `TeamMembershipSupport` | **全局一人一队（硬挡）** |
| `assertEligibleToJoinTeam` 全局查 ACTIVE 成员 | `TeamMembershipSupport` | 硬挡 |
| `basket_player_profile.current_team_id` 单值 | `TeamMemberJoinHelper.updatePlayerCurrentTeam` | 软冲突（展示/派生，非唯一索引） |
| `uk_team_member_active_platform_slot` | DB | 若再启用平台槽，也可能全局化冲突 |
| `canCreateTeam` / `hasActiveLongTermTeam` | `CaptainPermissionServiceImpl` | 有全局队则不能再建（即使另一 Division 有新授权） |
| `uk_team_name_delete(team_name,is_delete)` | DB | **挡同名跨 Division 建队**（与「允许同名新队」冲突） |
| `assertTeamNameUnique` 全局 | `AccountTeamServiceImpl` | 同上 |

**不会挡住跨 Division 的约束（已按 Division 隔离，友好）：**

| 约束 | 说明 |
|---|---|
| `uk_captain_grant_active_unique_key` | 按 division×player |
| `uk_registration_division_team` | 按 division×team |
| `uk_event_team_division_source_team` | 按 division×source_team |
| `uk_event_player_division_source_player` | 按 division×player |
| `countActivePlayerInDivision` | 报名阵容冲突检查仅限同 Division（`TeamRegistrationEligibilitySpecification`） |
| `idx_reg_player_division_player` | 非唯一，仅索引 |

**结论：** 赛事侧 Registration/EventPlayer/Grant 已支持「同球员多 Division」；**真正挡住的是 TeamMember 全局 slot + 全局队名唯一 + current_team_id 单值。**

---

## 八、最终结论

### 现在是否具备开始正式改造的条件？

**具备，可以进入「正式改造提示词 / 实施方案」阶段。** 但开工前改造提示词必须写死以下决策：

| # | 决策 | 建议锁定 |
|---|---|---|
| 1 | Grant↔Team | 业务上 1:1；`create` 强制未绑定；废除复活 bind；DB 保持 active_unique_key |
| 2 | 一人一队 | **slot 方案 C**（`{divisionId}_{memberId}`）+ 可选 member.division_id 冗余 |
| 3 | 状态 | 新增 ARCHIVED；**保留并重定义 DISSOLVED**（当前无数据、无流程） |
| 4 | Closing | Admin `DivisionClosingService` 强事务 + CAS 幂等；**不新建 closing_log 表** |
| 5 | 存量 | **Legacy 兼容，不拆 Team、不改 EventTeam.source_team_id**；当前跨 Division 污染均为测试数据 |
| 6 | 附带必改 | 全局 `uk_team_name` → 改为 Division 范围；弱化 `current_team_id` |
| 7 | 不动 | Match / Standing / BO / 晋级 / Submission（继续 `event_team_id`） |

### 一句话

技术风险点已查清：Grant 侧接近可落地的 1:1；成员槽与队名唯一是跨 Division 并行参赛的主障碍；DISSOLVED 可安全重定义；Closing 适合强事务幂等；存量跨 Division 数量很小且为测试数据，**应用 Legacy 兼容而不是拆历史队。**
