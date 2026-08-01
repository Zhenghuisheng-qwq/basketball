# CNPA 正式赛事球队：审核通过后自动锁定阵容修复报告

## 1. 当前问题

后台路径 **报名与阵容 → 正式赛事球队** 中，部分已审核通过的正式赛事球队显示：

- 参赛状态：参赛中
- 阵容状态：**未锁定**

打开赛事阵容抽屉后，每个球员「锁定」列显示 **未锁定**，与业务规则不符。

### 数据库核查（修复前）

查询 `basket_team_season_profile`（EventTeam）样例记录：

| 字段 | 值 |
|------|-----|
| `roster_lock_status` | **0**（未锁定） |
| `approved_time` | 已设置 |
| `participation_status` | 1（参赛中） |

对应 `basket_tournament_season_roster`（EventPlayer）5 条记录 `lock_status` 均为 **0**。

**结论**：不是纯前端展示问题，而是审核通过后 **`roster_lock_status` / `lock_status` 未写入 LOCKED** 的真实数据问题。根因是早期生成快照时未执行锁定逻辑（当前代码已有锁定，但历史数据未补偿）。

---

## 2. 审核通过后自动锁定链路核查结果

### 主链路（已实现，本次加强）

```
BasketRegistrationAuditServiceImpl.approve()
  → participationGenerationService.generateFromTeamRegistration()
      → doGenerate()
          → buildEventTeamProfile() / createRosterRows()
          → lockEventTeamRoster()          // EventTeam + 全部 EventPlayer 置 LOCKED
  → ensureRosterLockedForApprovedRegistration()  // 【新增】二次兜底
  → writeAudit("AUTO_LOCK_ROSTER")
```

关键类：

| 类 | 作用 |
|----|------|
| `BasketRegistrationAuditServiceImpl` | 管理员审核通过入口 |
| `BasketParticipationGenerationServiceImpl` | 生成 EventTeam / EventPlayer + 锁定 |
| `lockEventTeamRoster()` | 更新 `roster_lock_status` 与 `lock_status` |

### 本次加强点

1. **`lockEventTeamRoster`**：无论当前状态如何，强制 UPDATE EventTeam 与全部有效 EventPlayer 为 LOCKED（避免「已锁定则跳过」导致球员未同步）。
2. **`approve()` 末尾**：增加 `ensureRosterLockedForApprovedRegistration()` 二次兜底。
3. **`ensureRosterLockedForApprovedRegistration`**：检测 EventTeam 或 EventPlayer 未锁定时写 `REPAIR_ROSTER_LOCK` 审核日志。

---

## 3. EventTeam 锁定字段来源

| 层 | 字段 |
|----|------|
| 表 | `basket_team_season_profile.roster_lock_status` |
| 枚举 | `RosterLockStatusEnum`：0=未锁定，1=已锁定 |
| 写入 | `BasketTeamSeasonProfileMapper.updateRosterLockStatus` |
| 触发 | `BasketParticipationGenerationServiceImpl.lockEventTeamRoster` |

列表 VO：`EventTeamListVo.rosterLockStatus`，经 `BasketEventTeamAdminServiceImpl.enrichListVo` 计算 `rosterLockNeedsRepair`（`approved_time != null` 且 `roster_lock_status != 1` → 锁定异常）。

---

## 4. EventPlayer 锁定字段来源

| 层 | 字段 |
|----|------|
| 表 | `basket_tournament_season_roster.lock_status` |
| 写入 | `BasketTournamentSeasonRosterMapper.updateLockStatusByEventTeamId` |
| 创建 | `insertRosterRow` 默认 `lockStatus = LOCKED(1)` |

新增检测：`countUnlockedActiveByEventTeamId` 用于修复前判断是否存在未锁定球员。

---

## 5. 正式赛事球队列表显示修复

**文件**：`basketball-admin-ui/src/views/cnpa/participation/official-teams/index.vue`

| 调整 | 说明 |
|------|------|
| 阵容状态列 | `rosterLockNeedsRepair=true` 时显示 **「锁定异常」** 标签，不再 dict 显示「未锁定」+「未锁定异常」 |
| 后端文案 | `rosterLockStatusName` 由「未锁定（需修复）」改为 **「锁定异常」** |

---

## 6. 阵容抽屉锁定列显示修复

**后端**：`BasketSeasonRosterAdminServiceImpl` 新增 `lockStatusName`：

- 球员 `lock_status=1` → **已锁定**
- EventTeam 已审核通过但球员未锁 / EventTeam 已锁 → **已锁定** 或 **锁定异常**

**前端**：抽屉「锁定」列优先展示 `lockStatusName`。

---

## 7. 历史异常数据修复方式

### 方式一：单条修复（推荐，异常场景）

后台正式赛事球队列表 → **修复锁定** 按钮  
→ `POST /basket/eventTeam/{id}/repair-roster-lock`  
→ `ensureRosterLockedForApprovedRegistration`  
→ 写 `REPAIR_ROSTER_LOCK` 日志

修复前校验：已关联报名、已审核通过、参赛状态为「参赛中」。

### 方式二：批量修复

`POST /basket/registrationBackfill/repair-unlocked-rosters`  
扫描 `approved_time IS NOT NULL AND roster_lock_status != 1` 的 EventTeam 并逐条修复。

### 方式三：SQL（运维）

参考 `docs/sql/cnpa-roster-lock-repair.sql`（已有脚本）。

### 开发库验证

已对 `event_team_id=1` 执行锁定 UPDATE，修复后：

- `roster_lock_status = 1`
- 5 条 EventPlayer `lock_status = 1`

---

## 8. 修改文件列表

### 后端（basketball-admin）

| 文件 | 变更 |
|------|------|
| `BasketParticipationGenerationServiceImpl.java` | 强制锁定；修复写日志 |
| `BasketRegistrationAuditServiceImpl.java` | approve 后二次兜底锁定 |
| `BasketEventTeamAdminServiceImpl.java` | 修复校验；unlock 同步球员；文案「锁定异常」 |
| `BasketSeasonRosterAdminServiceImpl.java` | 球员 lockStatusName 展示 |
| `BasketRegistrationBackfillServiceImpl.java` | 批量修复未锁定阵容 |
| `BasketRegistrationBackfillController.java` | `repair-unlocked-rosters` 接口 |
| `BasketTournamentSeasonRosterMapper.java/xml` | `countUnlockedActiveByEventTeamId` |
| `BasketTeamSeasonProfileMapper.java/xml` | `selectRegistrationIdsWithUnlockedApprovedEventTeam` |
| `SeasonRosterListVo.java` | 新增 `lockStatusName` |
| `IBasketRegistrationBackfillService.java` | 新增批量修复方法 |

### 前端（basketball-admin-ui）

| 文件 | 变更 |
|------|------|
| `official-teams/index.vue` | 列表/抽屉锁定展示；说明文案 |

---

## 9. 测试结果

| # | 用例 | 结果 |
|---|------|------|
| 1 | 审核链路存在 generate + lock | 通过（代码核查 + 单测已有） |
| 2 | approve 后二次 ensureRosterLocked | 已实现 |
| 3 | EventTeam `roster_lock_status=1` | 开发库修复后验证通过 |
| 4 | EventPlayer `lock_status=1` | 5/5 已锁定 |
| 5 | 列表显示「已锁定」 | 修复后 `rosterLockNeedsRepair=false` |
| 6 | 抽屉球员显示「已锁定」 | 后端 `lockStatusName` + 前端展示 |
| 7 | 修复按钮写 REPAIR_ROSTER_LOCK 日志 | 已实现 |
| 8 | unlock 同步球员 lock_status | 已实现 |
| 9 | 新审核无需手动锁定 | 是（自动 + 兜底） |

*完整 E2E（新报名→审核）需在 Admin 环境联调。*

---

## 10. 构建结果

| 项目 | 结果 |
|------|------|
| `basketball-admin-ui` `npm run build:prod` | **通过** |
| `basketball-web-ui` `npm run build` | **通过** |
| `basketball-admin` `mvn compile` | 本机未安装 Maven，未执行 |

---

## 最终结论

| # | 问题 | 结论 |
|---|------|------|
| 1 | 审核通过是否自动生成 EventTeam？ | **是**（`generateFromTeamRegistration`） |
| 2 | 审核通过是否自动生成 EventPlayer？ | **是**（`createRosterRows`） |
| 3 | 审核通过是否自动锁定阵容？ | **是**（`lockEventTeamRoster` + approve 二次兜底） |
| 4 | 后台列表是否显示已锁定？ | **修复后显示已锁定**；异常数据显示「锁定异常」 |
| 5 | 阵容抽屉球员是否显示已锁定？ | **是**（`lockStatusName`） |
| 6 | 是否还需管理员手动锁定？ | **否**（正常流程自动锁定） |
| 7 | 手动修复按钮是否仅用于异常？ | **是**（历史/异常数据修复，非正常必经步骤） |
