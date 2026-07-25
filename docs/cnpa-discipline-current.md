# CNPA 第六批：纪律处罚现状盘点

> 盘点时间：2026-07-25

## 1. 数据库

| 表 | 说明 | 本地数据量 |
|---|---|---|
| `basket_discipline_record` | 处罚记录（batch6 扩展字段） | 0 |
| `basket_discipline_restriction` | 当前生效限制快照 | 0 |
| `basket_discipline_suspension_log` | 禁赛场次消耗日志 | 0 |
| `basket_point_adjustment` | 积分调整（扣分来源） | 已有（第五批） |
| `basket_business_audit` / `basket_business_audit_field` | 业务审计主表/明细 | 0 |

## 2. 后端（basketball-admin）

| 能力 | 状态 |
|---|---|
| 处罚 CRUD / 生效 / 撤销 | ✅ `BasketDisciplineServiceImpl` |
| 限制快照写入 | ✅ 生效时 upsert `basket_discipline_restriction` |
| 扣分幂等 | ✅ `DISC:{id}:POINT` 调整键 |
| 禁赛消耗 | ✅ `consumeMatchSuspension`，名单锁定触发 |
| 报名校验 | ✅ 审核 revalidate 接入 restriction + 旧 discipline 记录 |
| 转会/补员/队长移交 | ✅ 阵容变更审核接入 |
| Open 报名 | ✅ 审核通过前校验 |
| 比赛名单 | ✅ 保存名单 + 锁定消耗禁赛 |
| Admin API | ✅ `/basket/discipline` |

## 3. 前端（basketball-admin-ui）

| 页面 | 状态 |
|---|---|
| 球队与球员处罚 | ✅ `cnpa/discipline/penalty/index.vue` |
| 运营工作台 | ✅ 真实待办计数 |
| 永久档案/审计/赛事授权 | ✅ 已接入 |

## 4. 风险与遗留

| 级别 | 项 | 说明 |
|---|---|---|
| P1 | 处罚审批流 | 当前 DRAFT 可直接生效，未单独 PENDING/APPROVED 页面 |
| P1 | 处罚公示自动生成 | 依赖公告模块联动，未全自动 |
| P1 | 关键业务全面审计 | 目前处罚/赛事授权已写审计，认证/报名/比赛审核待批量接入 |
| P2 | Web 端处罚摘要 API | 官网展示依赖后续 web 公开接口补全 |

## 5. 与前五批关系

- 依赖第五批：`basket_point_adjustment`、正式比赛结果、结算批次
- 不阻塞第六批：处罚表为空，可通过 Admin 创建测试数据验证联动
