# CNPA Admin 第四批执行结果

> **执行日期**：2026-07-25  
> **范围**：四项目 + MySQL `basketball`

---

## 1. 执行概况

| 项 | 结果 |
|----|------|
| 新增数据库表 | **6** |
| 扩展数据库表 | **2** |
| 新增 Admin 菜单 | **7**（2220–2226） |
| 新增/修改代码文件 | **约 200+**（含 admin/web 后端 + 双端 UI） |
| 新增单元测试 | **4**（admin 2 + web 2） |
| 数据库迁移 | ✅ `run_batch4_migration.py` 已执行 |

---

## 2. 最终业务模型

```text
basket_team (永久)
  → basket_tournament_registration (报名申请)
      → basket_tournament_registration_player (报名阵容)
      → basket_tournament_registration_audit
  → [审核通过] basket_team_season_profile (正式赛事球队 + 快照)
      → basket_tournament_season_roster (正式赛事阵容 + 球员快照)

basket_player_profile
  → basket_tournament_open_registration (Open)
      → [审核通过] season_roster (event_team_id NULL, source=OPEN_REGISTRATION)

阵容变更:
  basket_tournament_roster_change_request → 审核 → 失效旧 roster + 新增 roster + change_record
  basket_tournament_roster_operation_log (管理员特殊操作)
```

---

## 3. 核心实现

### 球队报名审核通过（P0 修复）

`BasketRegistrationAuditServiceImpl.approve` 在 CAS 成功后调用：

`BasketParticipationGenerationServiceImpl.generateFromTeamRegistration`

事务内：

- INSERT `basket_team_season_profile`（球队名称/Logo/队长/平台快照，`CNPA-ET-*` 编号）
- INSERT `basket_tournament_season_roster`（球员/平台/游戏ID快照，`active_slot=seasonId:playerId`）
- 幂等：`uk_event_team_registration (registration_id, is_delete)`

### Web 端

- 球队报名四步流程 `TeamRegistrationFlow.vue`（接既有 `teamRegistration.ts`）
- Open 报名真实 API（`openRegistration.ts`）
- 赛事阵容/转会/补员/队长移交申请（`rosterChange.ts`）
- 赛事球队与阵容查询（`eventParticipation.ts`）

### Admin 端

- Open 报名审核、正式赛事球队、赛事阵容、转会/补员/队长移交审核、阵容特殊操作
- 历史回填接口：`POST /basket/registrationBackfill/backfill-approved`

---

## 4. 数据库变更

| 对象 | 操作 |
|------|------|
| basket_tournament_season | +allow_team/open, transfer/free_agent 窗口字段 |
| basket_team_season_profile | +registration_id, event_team_no, 快照, 锁定/参赛状态 |
| basket_tournament_season_roster | **新建** |
| basket_tournament_open_registration + audit | **新建** |
| basket_tournament_roster_change_request/record | **新建** |
| basket_tournament_roster_operation_log | **新建** |
| sys_menu 2220–2226 | **新建** |

**迁移**：`docs/sql/cnpa-batch4-migration.sql`  
**回滚**：`docs/sql/cnpa-batch4-rollback.sql`  
**执行脚本**：`docs/sql/run_batch4_migration.py`

---

## 5. 状态流转

| 域 | 状态 |
|----|------|
| 球队/Open 报名 | DRAFT→SUBMITTED→REVIEWING→APPROVED/REJECTED/WITHDRAWN/CANCELLED |
| 正式赛事球队 | participation_status ACTIVE/SUSPENDED/WITHDRAWN/... |
| 正式阵容 | roster_status ACTIVE/LOCKED/TRANSFERRED_OUT/... |
| 变更申请 | PENDING→APPROVED/REJECTED/WITHDRAWN |

---

## 6. 幂等与并发

- 报名审核：`updateStatusIfMatch` + `uk_registration_season_team`
- 正式球队：`uk_event_team_registration (registration_id)`
- 正式阵容：`uk_season_roster_active_slot (active_slot)` 每赛季每球员仅一条 ACTIVE
- 变更审核：request_status CAS + 事务内失效旧行/插入新行

---

## 7. 快照设计

| 快照 | 字段位置 |
|------|----------|
| 球队 | team_season_profile.team_name_snapshot, short_name_snapshot, logo, description, captain |
| 球员 | season_roster.player_nickname_snapshot, avatar, platform, game_id, position |

**待第五批切换读取快照的页面**：官网赛程、比赛名单、历史荣誉展示（当前仍可能读永久档案）

---

## 8. 构建结果

| 项目 | 结果 |
|------|------|
| basketball-web-ui | ✅ `npm run build` |
| basketball-admin-ui | ✅ `npm run build:prod` |
| basketball-web / basketball-admin | ⚠️ 本机无 `mvn` |

---

## 9. 历史数据

- 迁移前：**17** 条已通过球队报名、**0** 条正式赛事球队
- 迁移后需管理员调用 **backfill-approved** 或对新通过报名自动生成
- **未删除**原 registration / registration_player 数据

---

## 10. 遗留问题（第五批）

- 赛程/比赛名单应关联 `season_roster` 快照 ID
- 赛事级 Admin 数据权限完整接入
- Open 审核通过后「分组/组队」产品规则待确认
- 跨赛事冲突规则待产品确认
- 完整业务审计平台（第六批）

---

## 11. 数据库安全确认

**本批次没有删除原报名、球队成员和阵容历史数据，所有结构调整均保留了迁移和回滚方案。**

---

## 12. 第五批准备条件

| 模块 | 状态 |
|------|------|
| 正式赛事球队/阵容 | ✅ 模型与生成链路已建立 |
| 赛事阶段/分组 | ✅ 已有（第二批） |
| 赛程/比赛名单 | ⏳ 可接入 season_roster_id |
| 比分/技术统计/结算 | ❌ 第五批 |

---

## 13. 相关文档

- `docs/cnpa-participation-current.md`
- `docs/cnpa-participation-design.md`
- `docs/cnpa-admin-batch4-plan.md`
