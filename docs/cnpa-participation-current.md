# CNPA 报名与参赛现状（第四批）

> **日期**：2026-07-25  
> **第三批前置**：✅ 132 已认证球员、17 已审核永久球队、平台游戏 ID 表已建

## 已有实现

| 能力 | Web | Admin | 数据表 |
|------|-----|-------|--------|
| 球队报名草稿/阵容/提交/撤回 | ✅ `TeamRegistrationBusinessServiceImpl` | — | registration, registration_player |
| 球队报名审核 | — | ✅ 列表/详情/校验/通过/驳回 | registration_audit |
| Open 报名 | ❌ Mock | ❌ 无 | 无 |
| 正式赛事球队 | ❌ approve 不写库 | ❌ 无页 | team_season_profile **0 行** |
| 正式赛事阵容 | ❌ | ❌ | **无独立表** |
| 转会/补员/队长移交 | ❌ | ❌ | 无 |
| 阵容锁定/历史 | ❌ | ❌ | 无 |

## 关键缺口（P0）

1. **17 条已通过报名**（status=3）未生成 `team_season_profile` / 正式阵容  
2. `BasketRegistrationAuditServiceImpl.approve` 仅改状态  
3. Web 球队报名页仍为 `TeamRegistrationPlaceholder.vue`  
4. Open 报名全 Mock  

## 可复用

- `RegistrationStatusEnum` 已含 DRAFT/SUBMITTED/REVIEWING/APPROVED/REJECTED/WITHDRAWN/CANCELLED  
- `uk_registration_season_team` 防重复报名  
- Web 报名 API 完整（`/basket/team/.../registration/*`）  
- Admin 审核页已有阵容校验 UI  
