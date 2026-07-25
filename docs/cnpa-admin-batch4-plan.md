# CNPA Admin 第四批执行计划

## 阶段

1. ✅ 现状文档 + SQL
2. ✅ Admin/Web 后端（生成服务、Open、变更审核）
3. ✅ Admin-ui / Web-ui 页面
4. ✅ 数据库迁移 + 菜单 2220–2226
5. ⏳ 历史回填（17 条已通过报名）— 调用 `POST /basket/registrationBackfill/backfill-approved`
6. 构建验证 + batch4-result

## 核心改动

- `BasketRegistrationAuditServiceImpl.approve` → `participationGenerationService.generateFromTeamRegistration`
- 新表：season_roster, open_registration, roster_change_request/record, roster_operation_log
- 扩展：team_season_profile, tournament_season

## 禁止触碰

比赛、积分、统计、公告、荣誉
