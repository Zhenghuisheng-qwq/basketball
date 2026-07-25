# CNPA 账号与认证现状（第三批专项）

> **日期**：2026-07-25  
> **范围**：四项目 + 本地库 `basketball`

## 1. 已实现

| 域 | 状态 | 依据 |
|----|------|------|
| 邮箱注册/登录 | ✅ | `BasketMemberAuthServiceImpl` 仅 email，无 mobile |
| 邮箱规范化 | ✅ | `normalizeEmail()` 小写+trim |
| 球员认证字段 | ✅ | gameTerminal/gameAccount/position + 截图，无 KYC |
| Web 认证申请 API | ✅ | draft/material/submit/current |
| Admin 球员认证审核 | ✅ | approve 生成 CNPA-P-* playerNo |
| 队长资格 grant/revoke | ✅ | 含队长球队校验 |
| 球队审核 | ✅ | startReview/approve/reject + audit_record 表 |
| COS 上传 | ✅ | `AccountFileServiceImpl` |

## 2. 缺口（第三批需补）

| 缺口 | 严重度 |
|------|--------|
| 找回密码 / 改密后端 | 高 |
| 平台游戏 ID 表与 API | 高 |
| 游戏 ID 唯一约束 | 高 |
| 认证撤回 | 中 |
| 驳回后新申请版本 | 中 |
| Admin 账号球员管理页 | 中 |
| 球队 audit-record API | 中 |
| Web platforms/change-history/eligibility Mock | 中 |
| approve 写 platform_account | 高 |
| TeamNo CNPA-T 格式（approve 时） | 中 |

## 3. 敏感字段

- `basket_member_account`：mobile 已 DROP（migrate_member_drop_mobile.sql）
- `basket_player_cert_application`：KYC 已 DROP（migrate_player_cert_drop_kyc.sql）
- Web Mock 仍含 `backupContact`（verification.ts）— 第三批清理

## 4. 数据库待迁移

见 `docs/sql/cnpa-batch3-migration.sql`（MCP 禁止 DDL，需本地 mysql 客户端执行）
