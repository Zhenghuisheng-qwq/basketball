# CNPA Admin 第三批执行计划

## 阶段

1. ✅ 现状文档 + SQL（before/migration/rollback）
2. 后端 web + admin（平台 ID、密码、账号管理、audit-record）
3. 前端 web-ui + admin-ui（去 Mock、新页）
4. 本地执行 migration SQL
5. 构建 + 测试 + batch3-result

## 修改清单（概要）

### basketball-web
- VerifyPurposeEnum + 密码重置/改密
- PlayerPlatformAccountService + Controller
- PlayerCertificationService：withdraw、resubmit 新版本、normalized 校验
- GameAccountNormalizer

### basketball-admin
- BasketAccountPlayerController + Service
- BasketPlayerPlatformChangeController（审核平台变更）
- BasketTeamAuditController audit-record
- BasketPlayerCertServiceImpl：platform account + game id 唯一
- TeamNoGenerator

### basketball-web-ui
- platforms/change-history/eligibility 接 API
- 找回密码页
- security 改密接 API

### basketball-admin-ui
- cnpa/identity/account-player/index.vue
- team-certification audit 时间线
- sys_menu 新增 account-player（menu_id 待定 2210）

## 禁止触碰

报名、比赛、统计、处罚完整实现
