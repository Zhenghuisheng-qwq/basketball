# CNPA 第三批执行结果

> **执行日期**：2026-07-25  
> **范围**：basketball-web-ui、basketball-web、basketball-admin-ui、basketball-admin、MySQL `basketball`

---

## 1. 执行概况

| 项 | 数量/结果 |
|----|-----------|
| Git 变更文件（约） | **148** |
| 新增数据库表 | **2**（platform_account、platform_change_request） |
| 修改数据库表 | **1**（cert_application + normalized_game_account） |
| 新增菜单 | **5**（2210–2214） |
| 平台账号回填 | **132** 条（已认证球员） |
| 新增单元测试 | **3**（web 2 + admin 1） |
| 停止使用敏感字段读写 | mobile/idCard/realName/KYC 字段（此前已 DROP，本批清理 Mock/UI） |

---

## 2. 当前最终业务模型

```text
basket_member_account (邮箱唯一)
    └── basket_player_profile (member_id UNIQUE, player_no UNIQUE)
            ├── basket_player_cert_application (+ audit/material)
            ├── basket_player_platform_account (platform+normalized UNIQUE)
            ├── basket_player_platform_change_request
            └── basket_captain_qualification (is_current)
basket_team (team_no, audit_status)
    └── basket_team_audit_record
    └── captain → basket_player_profile
```

**关系要点**：
- 一个账号最多一个有效球员档案（`uk_player_member`）
- 认证通过生成 `CNPA-P-*` 与首个 primary 平台账号
- 队长资格与 `team.captain_player_id` 分离
- 球队审核通过可分配 `CNPA-T-*` 永久编号

---

## 3. 修改文件清单（按项目）

### basketball-web（后端 + 测试）
- 新增：平台账号 Service/Controller、GameAccountNormalizer、枚举、Model/Dao、密码重置/改密 Req、测试
- 修改：`PlayerCertificationServiceImpl`（撤回、驳回新申请、normalized、去重）、`BasketMemberAuthServiceImpl`（resetPassword）、`VerifyPurposeEnum`、`AccountCenterServiceImpl`

### basketball-web-ui
- 新增：`api/playerPlatformAccount.ts`、`views/account/ForgotPassword.vue`
- 修改：`stores/account.ts`（平台/变更/资格接 API）、platforms/change-history/eligibility/pending、login、security 改密

### basketball-admin
- 新增：AccountPlayer、PlayerPlatformChange Controller/Service/Mapper、TeamNoGenerator、Platform Domain
- 修改：`BasketPlayerCertServiceImpl`（唯一性+platform_account）、`BasketTeamAuditServiceImpl`（CNPA-T 编号）、`BasketTeamAuditController`（audit-record）

### basketball-admin-ui
- 新增：`cnpa/identity/account-player/index.vue`、`platform-change/index.vue`、`api/basket/accountPlayer.ts`、`playerPlatformChange.ts`
- 修改：`team-certification/index.vue`（审核时间线）

### docs / sql
- `docs/cnpa-auth-certification-current.md`
- `docs/cnpa-auth-certification-design.md`
- `docs/cnpa-admin-batch3-plan.md`
- `docs/sql/cnpa-batch3-before.sql`
- `docs/sql/cnpa-batch3-migration.sql`
- `docs/sql/cnpa-batch3-rollback.sql`
- `docs/sql/run_batch3_migration.py`

---

## 4. API 清单（新增/扩展）

### Web

| 接口 | 方法 | 说明 | 新增 |
|------|------|------|------|
| `/basket/member/auth/password/reset` | POST | 邮箱验证码重置密码 | ✅ |
| `/basket/account/security/change-password` | POST | 登录态改密 | ✅ |
| `/basket/player/certification/withdraw` | POST | 撤回审核中申请 | ✅ |
| `/basket/player/platform-account/list` | GET | 平台账号列表 | ✅ |
| `/basket/player/platform-account/add` | POST | 新增平台（待审） | ✅ |
| `/basket/player/platform-account/{id}/change-game-id` | POST | 修改游戏 ID 申请 | ✅ |
| `/basket/player/platform-account/change-history` | GET | 变更历史 | ✅ |
| `/basket/player/platform-account/eligibility` | GET | 资格受限状态 | ✅ |

### Admin

| 接口 | 方法 | 权限 | 新增 |
|------|------|------|------|
| `/basket/accountPlayer/list` | GET | basket:accountPlayer:list | ✅ |
| `/basket/accountPlayer/{memberId}` | GET | basket:accountPlayer:query | ✅ |
| `/basket/playerPlatformChange/list` | GET | basket:playerPlatformChange:list | ✅ |
| `/basket/playerPlatformChange/{id}/approve\|reject` | POST | basket:playerPlatformChange:audit | ✅ |
| `/basket/teamAudit/{teamId}/audit-record/list` | GET | basket:teamAudit:query | ✅ |

原有球员/队长/球队审核 API **路径不变**，行为增强。

---

## 5. 数据库变更

| 对象 | 操作 |
|------|------|
| `basket_player_cert_application.normalized_game_account` | ADD + INDEX |
| `basket_player_platform_account` | CREATE + 回填 132 行 |
| `basket_player_platform_change_request` | CREATE |
| `sys_menu` 2210–2214 | INSERT |
| `sys_role_menu` role_id=2 | INSERT |

**迁移执行**：`python docs/sql/run_batch3_migration.py`（2026-07-25 已成功）  
**回滚**：`docs/sql/cnpa-batch3-rollback.sql`

---

## 6. 敏感字段处理

| 层 | 处理 |
|----|------|
| 前端 | 移除手机号/身份证表单项与 backupContact；认证仅头像/平台/游戏ID/位置/截图 |
| 后端 | 不再校验/返回 KYC 字段 |
| 数据库 | 旧字段已 DROP（历史迁移）；本批 **未 DELETE 列** |
| Mock | 平台/资格/改密 Mock 已替换为真实 API（Open 报名 Mock 保留，属第四批） |

---

## 7. 状态流转

| 域 | 状态 |
|----|------|
| 球员认证 | DRAFT→SUBMITTED→REVIEWING→APPROVED/REJECTED/CANCELLED；驳回后 **新申请记录** 重提 |
| 平台变更 | PENDING→APPROVED/REJECTED/WITHDRAWN |
| 队长资格 | NOT_GRANTED/ACTIVE/REVOKED（is_current） |
| 球队审核 | PENDING→REVIEWING→APPROVED/REJECTED |
| 球员资格 | player_status NORMAL(1) / RESTRICTED(2) |

---

## 8. 幂等与并发

| 机制 | 实现 |
|------|------|
| 重复提交认证 | `assertNoReviewingApplication` + 已通过拦截 |
| 重复审批 | `updateStatusIfMatch` CAS，rows≠1 抛错 |
| 永久球员编号 | `PlayerNoGenerator` + `uk_player_no` |
| 平台游戏 ID 唯一 | `uk_platform_normalized_active` + Service 前置 count |
| 队长 grant 竞态 | 事务 + clearCurrent（建议后续加 DB 唯一约束） |
| 球队重复 approve | 已 APPROVED 时幂等跳过 |

---

## 9. 构建与测试

| 项目 | 结果 |
|------|------|
| basketball-web-ui | ✅ `npm run build` |
| basketball-admin-ui | ✅ `npm run build:prod` |
| basketball-web | ⚠️ 环境无 `mvn`，未编译；已添加 2 个单元测试源文件 |
| basketball-admin | ⚠️ 环境无 `mvn`；已添加 1 个单元测试源文件 |
| MySQL 迁移 | ✅ 已执行并验证 platform_accounts=132 |

---

## 10. 遗留问题（第四批及以后）

- 赛事阵容锁定与平台 ID 变更联动
- 队长移交实际业务
- 正式赛事球队 / Open 报名
- 完整处罚体系与业务审计中心
- Admin 账号球员列表 `gameTerminal` 筛选需 Mapper 补条件
- 队长 grant 建议增加 `(player_id, is_current=1)` 部分唯一索引

---

## 11. 数据库安全确认

**本批次没有删除历史敏感字段列**；仅停止 CNPA 新业务对 KYC/手机号的使用，并完成平台账号表迁移与回滚 SQL。

---

## 12. 第四批准备条件

| 模块 | 第三批后状态 |
|------|--------------|
| 球队报名审核 | 后端已有，approve 链路仍待正式参赛数据（第四批） |
| Open 报名 | 仍 Mock，未实现 |
| 正式赛事球队 | 表存在，与报名未自动联动 |
| 赛事阵容 | 未实现 |
| 转会/自由球员/队长移交 | 未实现 |
| 账号+球员+平台 ID 基础链 | ✅ **可进入第四批** |

---

## 13. 本批验收核对（用户强调四项）

1. **账号与球员分离**：✅ `basket_member_account` + `basket_player_profile.member_id UNIQUE`
2. **审核通过幂等**：✅ CAS 更新 + 唯一 player_no / 重复 approve 失败或幂等
3. **平台游戏 ID 数据库唯一**：✅ `uk_platform_normalized_active (platform, normalized_game_id, is_delete)`
4. **敏感字段**：✅ 认证链路不再读写身份证/手机号；证明材料走后端鉴权私有文件
