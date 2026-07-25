# CNPA 第六批执行计划

## 阶段与范围

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 现状盘点 + batch6 SQL | ✅ |
| 2 | 处罚记录/限制/Service | ✅ |
| 3 | 业务联动（报名/转会/名单） | ✅ |
| 4 | 永久档案 Admin API + 页 | ✅ |
| 5 | 业务审计 + 赛事授权 | ✅ 部分审计接入 |
| 6 | 运营工作台真实化 | ✅ |
| 7 | 内容运营增强 | ⏳ 表已建，自动生成待做 |
| 8 | Mock 清理 + 全链路验收 | ⏳ |
| 9 | 构建 + 发布清单 | ⏳ |

## SQL

- `docs/sql/cnpa-batch6-before.sql`
- `docs/sql/cnpa-batch6-migration.sql`（已执行）
- `docs/sql/cnpa-batch6-rollback.sql`

## 菜单

- 2207 下 2240–2244 + 按钮 2245–2248
- 2201 工作台权限 `basket:dashboard:summary`

## 不在本批

- 正式部署、Nginx、HTTPS、生产 COS/Redis 切换
