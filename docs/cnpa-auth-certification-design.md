# CNPA 账号与认证设计（第三批）

## 1. 实体关系

```text
basket_member_account (1) ── (0..1) basket_player_profile
basket_player_profile (1) ── (*) basket_player_platform_account
basket_player_profile (1) ── (*) basket_player_platform_change_request
basket_player_profile (1) ── (*) basket_player_cert_application
basket_player_cert_application (1) ── (*) basket_player_cert_audit
basket_player_profile (1) ── (0..1) basket_captain_qualification (is_current=1)
basket_team ── captain_player_id → basket_player_profile
```

## 2. 平台游戏 ID

- 表：`basket_player_platform_account`
- 唯一：`uk_platform_normalized_active (platform, normalized_game_id, is_delete)`
- 规范化：`trim` + `toLowerCase`，保留 display 原值
- 首次认证通过：Admin approve 事务内 insert primary account
- 新增平台 / 改 ID：走 `basket_player_platform_change_request`，Admin 审核

## 3. 状态

### 球员认证（沿用 PlayerCertStatusEnum code）

| Code | 含义 |
|------|------|
| 0 | DRAFT |
| 1 | SUBMITTED |
| 2 | REVIEWING |
| 3 | APPROVED |
| 4 | REJECTED |
| 5 | CANCELLED |

### 平台变更 request_status

| Code | 含义 |
|------|------|
| 0 | PENDING |
| 1 | APPROVED |
| 2 | REJECTED |
| 3 | WITHDRAWN |

### 球员资格 player_status

| Code | 含义 |
|------|------|
| 1 | NORMAL |
| 2 | RESTRICTED |

## 4. 幂等

- 认证 approve：`updateStatusIfMatch` + 唯一 player_no + 唯一 platform_game_id
- 平台变更 approve：request_status CAS + 唯一索引
- 队长 grant：`countCurrentActive` + clearCurrent

## 5. 密码

- `VerifyPurposeEnum.RESET_PASSWORD`
- 找回：邮箱验证码 → resetPassword
- 改密：旧密码 + 新密码（登录态）
