# CNPA 纪律处罚设计

## 1. 核心分离

```text
basket_discipline_record   → 事实记录（不可删除，可撤销）
basket_discipline_restriction → 当前限制快照（生效时写入，结束/撤销时 deactivate）
basket_discipline_suspension_log → 禁赛按场消耗（幂等键 MATCH:{matchId}:MEMBER:{memberId}）
basket_point_adjustment → 扣分来源（幂等键 DISC:{disciplineId}:POINT）
```

## 2. 对象与措施枚举

- 对象：`DisciplineTargetTypeEnum` — PLAYER / PERMANENT_TEAM / EVENT_TEAM / ACCOUNT
- 措施：`DisciplinePenaltyCodeEnum` — WARNING、MATCH_SUSPENSION、REGISTRATION_BAN 等
- 记录状态：`DisciplineRecordStatusEnum` — DRAFT / PENDING / APPROVED / ACTIVE / COMPLETED / REVOKED

## 3. 状态流转

```text
DRAFT → ACTIVE（管理员生效）
ACTIVE → REVOKED（撤销，deactivate restriction）
ACTIVE → COMPLETED（到期或禁赛场次耗尽，待定时任务补偿）
```

## 4. 业务联动点

| 措施 | 限制字段 | 拦截入口 |
|---|---|---|
| REGISTRATION_BAN | allow_registration=0 | 报名 revalidate、Open 报名 approve |
| TRANSFER_BAN | allow_transfer=0 | 转会/补员审核 approve |
| CAPTAIN_BAN | allow_captain=0 | 队长移交 approve |
| MATCH_SUSPENSION | allow_lineup=0 + remaining | 名单 save；锁定 consume |
| POINT_DEDUCTION | — | 生效写 point_adjustment |

## 5. 幂等

- 创建：`idempotency_key` 唯一
- 生效：`updateRecordStatusIfMatch` 乐观状态更新
- 禁赛消耗：`basket_discipline_suspension_log.idempotency_key` 唯一
- 扣分：`basket_point_adjustment.idempotency_key` 唯一

## 6. 权限

- 功能：`basket:discipline:*` 按钮权限
- 数据：`BasketTournamentScopeSupport.assertTournamentAccess(season.tournamentId)`
