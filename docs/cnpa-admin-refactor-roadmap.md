# CNPA Admin 重构路线图（第一至六批）

> **盘点日期**：2026-07-25  
> **原则**：先盘点后重构；每批列范围、依赖、风险，不给虚假工期

---

## 批次总览

| 批次 | 主题 | 状态 | 风险 |
|------|------|------|------|
| 第一批 | 现状盘点、映射、基线文档 | ✅ 本批次完成 | 低 |
| 第二批 | 菜单、目录、动态路由 | ⚠️ **历史已执行**（cnpa 目录 + 一级菜单） | 中 |
| 第三批 | 账号、球员认证、队长、球队认证 | 待执行 | 中 |
| 第四批 | 报名、正式参赛、阵容变更 | 待执行 | **高** |
| 第五批 | 比赛执行、审核、争议、统计结算 | 待执行 | **高** |
| 第六批 | 处罚、永久档案、业务审计、内容运营、验收 | 待执行 | 中 |

---

## 第一批（已完成）

**范围**：只读代码 + MySQL + 文档  
**产出**：

- `docs/cnpa-admin-current-audit.md`
- `docs/cnpa-admin-feature-matrix.md`
- `docs/cnpa-admin-route-menu-mapping.md`
- `docs/cnpa-admin-database-inventory.md`
- `docs/cnpa-admin-refactor-roadmap.md`

**禁止项遵守**：未改业务代码、未写数据库

---

## 第二批（菜单/目录 — 历史已执行）

> 以下在 prior session 已落地；若需回滚见 `docs/sql/cnpa-admin-menu-rollback.sql`

### 涉及功能

- 取消「CNPA赛事管理」总目录（2000 停用）
- 8 个一级业务菜单
- 26 页 `views/basket/*` → `views/cnpa/*`
- sys_menu.component 更新

### 涉及页面

全部 `src/views/cnpa/**`（26 文件）

### 涉及后端

无接口变更

### 涉及数据库

- `sys_menu` parent_id / component / visible / status
- `sys_role_menu` 新菜单授权

### 风险与回滚

| 项 | 说明 |
|----|------|
| 风险等级 | 中（路由 404 若 component 不匹配） |
| 数据迁移 | 否（仅菜单元数据） |
| 回滚 SQL | `docs/sql/cnpa-admin-menu-rollback.sql` |

### 验收

- [x] 26 component 与 vue 文件一致
- [x] `npm run build:prod` 通过
- [ ] 合并静态 `/index` 与 2201（遗留）

### 第二批必须避免（已遵守）

未改：审核状态、积分计算、Req/DTO 字段、角色权限业务逻辑

---

## 第三批：账号与认证修正

### 涉及功能

| 功能 | 动作 |
|------|------|
| 球队审核 | 补 `GET /teamAudit/{id}/audit-record/list` |
| 账号与球员管理 | 评估是否新建只读 Admin 页（member + profile 列表） |
| 球员认证 | 确认 material 类型与前端展示一致 |
| 队长资格 | 与球队 captain_id 关系文档化 |
| 权限对齐 | 清理或实现 orphan 按钮权限 |

### 涉及页面

- `cnpa/identity/team-certification/index.vue`（补 audit 时间线）
- 可选新建 `cnpa/identity/account-player/index.vue`

### 涉及后端

- `BasketTeamAuditController` + Service
- 可选 `BasketMemberAccountController`（只读）

### 涉及数据库

- 只读查询为主；**不改表结构**（除非确认缺字段）

### 风险等级：**中**

### 数据迁移：否

### 依赖

- 第一批文档（本批）
- 第二批菜单已就位

### 验收主链路

1. 球员认证：提交(Web) → 审核(Admin) → player_profile 有 player_no  
2. 球队：创建(Web) → 审核(Admin) → team audit_status=通过  
3. 队长：授予 → 列表可见

### 禁止触碰

- 不删除 player_cert 字段  
- 不改审核通过核心状态机

---

## 第四批：报名、正式参赛、阵容变更

### 涉及功能（核心）

| 功能 | 现状 | 目标 |
|------|------|------|
| 球队报名审核 | approve 仅改 registration_status | 生成 group_team / season_profile |
| 正式赛事球队 | 仅分组手动入组 | 与审核联动 |
| 赛事阵容 | 无 | 正式 roster 与报名 roster 分离 |
| Open 报名 | Web Mock | 待产品确认后 Admin+表 |
| 转会/自由球员/队长移交 | 缺失 | 新审核流 + captain_history |

### 涉及页面（新建为主）

- `cnpa/participation/official-teams/`
- `cnpa/participation/roster/`
- `cnpa/participation/transfer/`
- `cnpa/participation/captain-transfer/`
- 菜单挂到 2204 报名与阵容

### 涉及后端

- `BasketRegistrationAuditServiceImpl.approve` 扩展
- 新 Service：Roster / Transfer / CaptainTransfer
- `BasketTeamCaptainHistoryMapper` 启用

### 涉及数据库

- 可能扩展 `basket_team_season_profile`
- 可能新增阵容历史表（**需设计评审**）
- 转会：禁止直接 UPDATE team_member 覆盖

### 风险等级：**高（P0）**

| 风险 | 说明 |
|------|------|
| 历史报名 | 17 条已通过 registration 无 group_team 对应 |
| 重复入组 | approve 幂等 |
| 阵容锁定 | 比赛开始后不可改 |

### 数据迁移：**可能需要**

- 脚本：已通过 registration → 补写 group_team（需人工核对 season/group）

### 回滚 SQL：需第四批设计时编写

### 依赖

- 第三批认证/球队审核稳定
- 赛事分组配置完成

### 验收主链路

报名提交 → 审核通过 → 正式参赛球队可见 → 分组/赛程可引用 → 比赛 roster 来自正式阵容

---

## 第五批：比赛执行、审核、争议、统计结算

### 涉及功能

| 功能 | 动作 |
|------|------|
| 比分 confirmScore | 补 data_status 更新，与 stat 一致 |
| 比赛数据审核 | 独立页或工作台待办 |
| 数据争议 | 基于 stat_change_record UI |
| 异常比赛 | forfeit/postpone 独立入口 |
| 结算幂等 | 验证重复 confirm 不重复累计 |
| 权限 | 实现 ranking:publish、dataSettlement:confirm 或删菜单 |

### 涉及页面

- `cnpa/competition/match-data-audit/`
- `cnpa/competition/dispute/`
- `cnpa/competition/exception/`
- 增强 `cnpa/dashboard/`（待办）

### 涉及后端

- `BasketMatchScoreServiceImpl.confirmScore`
- `BasketDataSettlementServiceImpl` 幂等测试
- 新 DisputeController（可选）

### 涉及数据库

- basket_match.data_status 历史数据修复脚本（谨慎）
- stat_change_record 索引优化

### 风险等级：**高（P0）**

- 重复审核 → 积分重复累计
- 驳回数据进入统计

### 数据迁移：可能（status 修正）

### 依赖

- 第四批正式参赛数据正确
- 赛程/比赛已生成

### 验收主链路

录入比分/技术统计 → 审核 confirm → 结算 → 积分榜/排行与比赛一致 → 重算幂等

---

## 第六批：纪律、档案、内容、全链路验收

### 涉及功能

| 功能 | 动作 |
|------|------|
| 纪律处罚 | discipline_record CRUD + 生效限制 |
| 永久档案 | 区分展示 permanent vs tournament snapshot |
| 业务审计 | 统一查询页（聚合各 audit 表） |
| 公告 | 定时发布、置顶、关联赛事 |
| 赛事荣誉 | 独立页 + honor_lineup 防重复 |
| 全链路验收 | 跨 Web + Admin 回归 |

### 涉及页面

- `cnpa/discipline/penalty/`
- `cnpa/discipline/permanent-team/`
- `cnpa/discipline/permanent-player/`
- `cnpa/discipline/audit-log/`
- `cnpa/content/tournament-honor/`

### 涉及后端

- 新 DisciplineController
- AuditAggregationService
- Announcement 增强

### 涉及数据库

- discipline_record 启用
- 可选 archive 视图（不一定新表）

### 风险等级：**中**

### 数据迁移：否（新功能为主）

### 依赖

- 前五批核心链路

### 验收主链路

处罚 → 报名/比赛受限 → 审计可查 → 公告/荣誉官网可见

---

## 下一批（若继续）建议

**若第二批已落地**：直接进入 **第三批**（认证修正），避免重复移动目录。

**第三批必做**：

- 球队审核 audit-record API
- 合并 `/index` 与 menu 2201 工作台入口

**第三批禁止**：

- 改 registration approve 逻辑（留第四批）
- 改 confirmScore / 结算算法（留第五批）

---

## 文档索引

| 文档 | 用途 |
|------|------|
| cnpa-admin-current-audit.md | 总审计 |
| cnpa-admin-feature-matrix.md | 功能矩阵 A–F |
| cnpa-admin-route-menu-mapping.md | 菜单树与映射 |
| cnpa-admin-database-inventory.md | 表盘点 |
| cnpa-admin-batch2-result.md | 第二批执行记录 |
| docs/sql/cnpa-admin-menu-*.sql | 菜单快照/迁移/回滚 |
