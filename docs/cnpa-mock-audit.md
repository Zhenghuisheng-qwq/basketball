# CNPA 全站运行时 Mock 审计

> 审计时间：2026-07-24  
> 原则：生产运行链路禁止业务 Mock；接口失败不得回退假数据。

## 1. 已脱离运行时 Mock（第六/七轮）

- 首页赛事/排行/运营内容
- 赛事中心、赛程赛果、积分榜、数据中心
- 球队/球员详情、球队对比
- 名人堂、公告公开页
- Admin 审核/结算/荣誉相关页面（接真实 API）

## 2. 仍被页面/Store 引用的 Mock（需后续清理）

| 引用方 | Mock | 是否运行时 | 处理 |
|--------|------|------------|------|
| `stores/account.ts` | `account/teamRelations`、`playerIdentityMock`、`tournamentAccountMock`、`openRegistrationMock` 等 | **是**（账户域部分路径仍混用） | P1：缺陷 DEF-R8-003 |
| `stores/eventTeam.ts` | `eventTeamMock`、`auth`、`freeAgentMock`、`transferMock` | **是** | P1：账户/球队活动页遗留 |
| `views/account/events/MyEventsView.vue` | `accountEventsMock` | **是** | P2 |
| `components/account/events/MyEventsTabs.vue` | `accountEventsMock` | **是** | P2 |
| `views/account/my-tournaments/*` | `eventTeamMock` / `openRegistrationMock` | **是** | P2 |
| `views/account/player-identity/*` | `playerIdentityMock`（映射工具/驳回样例） | 部分 | P2：纯映射可迁 utils |
| `views/events/components/CommunitySection.vue` | `league.mock` communityLinks | **是**（社区外链展示） | P3：非核心业务 |
| `views/events/mock/*` | 赛事演示数据模块 | 若未被页面 import 则保留 | 需持续确认 import 链 |

## 3. 本轮已处理

- `validatePasswordRules` 迁出 Mock → `src/utils/passwordRules.ts`
- `security/index.vue`、`settings/index.vue`、`stores/account.ts` 改用新工具

## 4. 允许保留

| 类型 | 说明 |
|------|------|
| 单元测试/组件演示数据 | 不进入生产请求链路 |
| 类型示例 | TS 类型 |
| `views/events/mock` 未引用文件 | 死代码可后续删除，不参与运行 |

## 5. 结论

- **第六～七轮公开数据链路：不以 Mock 掩盖失败**（静态复核通过）。
- **账户个人中心/球队活动部分页面仍存在运行时 Mock → 未达到「全站零业务 Mock」**。
- 不得在第八轮报告中声称「全站 Mock 已清零」。
