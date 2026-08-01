# CNPA 长期球队页面移除赛事规则文案修复报告

## 1. 当前问题

Web 端「个人中心 → 我的球队 → 概览」中，成员字段错误展示为：

```
成员：5/8 · 正式阵容 5–8 人（5v5，首发 5 人）
```

根因：`team/index.vue` 的概览页复用了 **赛事报名详情**（`registrationDetail`）中的 `minRosterCount` / `maxRosterCount` / `formatCode` / `teamPlayCount`，将具体赛事的 5v5 阵容规则当作长期球队基础属性展示。

长期球队是俱乐部/战队资产，**不绑定**游戏平台、具体赛事或 3v3/4v4/5v5 赛制。

## 2. 长期球队与赛事球队的区别

| 维度 | 长期球队（BasketTeam） | 赛事球队（EventTeam / 报名） |
|------|------------------------|------------------------------|
| 生命周期 | 长期存在 | 某届赛事快照 |
| 平台 | 不绑定 | 绑定 platformCode |
| 赛制 | 不绑定 | 3v3 / 4v4 / 5v5 |
| 阵容人数 | 仅当前有效成员数 | min/max roster、首发人数 |
| 展示位置 | 我的球队概览/资料/成员 | 赛事报名、正式赛事球队、比赛数据 |

## 3. Web 端检查范围

| 页面 | 结果 |
|------|------|
| 我的球队 → 概览 | **已修复**（成员改为「N 人」） |
| 我的球队 → Hero / 资料 / 成员 / 邀请 / 审核 | 无赛事规则文案（成员列表「正式/试训」为成员类型，非赛事阵容） |
| 我的球队 → 赛事报名 Tab | **保留**赛事平台、赛制、阵容要求（有 registrationDetail 上下文） |
| 可加入球队列表 | **已修复**（成员数改为「N 人」，不再 x/y） |
| 公开球队详情 `/tournament/team` | **已修复**（移除 gameTerminal 平台展示；Hero「注册阵容」→「当前成员」） |
| 我的赛事 / 报名流程 / EventTeam 页 | 未改动，仍展示赛事规则 |

## 4. Admin 端检查范围

| 页面 | 结果 |
|------|------|
| 球队审核认证列表/详情 | **已修复**（移除「比赛终端」列/字段；成员数改为「N 人」） |
| 正式赛事球队 / 报名审核 / 赛程 / 比赛数据 | 未改动，仍展示赛事平台与阵容 |
| 球队档案（permanent-team） | 本身无赛事规则字段 |
| 球员认证 / 平台 ID 变更 | 属球员维度，非长期球队基础资料，未改 |

## 5. 已删除或替换的长期球队错误文案

| 位置 | 修复前 | 修复后 |
|------|--------|--------|
| Web 概览 · 成员 | `5/8 · 正式阵容 5–8 人（5v5，首发 5 人）` | `5 人` |
| Web 概览 | 删除 `teamMemberLimitHint` 计算属性 | — |
| Web 可加入球队 | `5/8` | `5 人` |
| Admin 球队审核列表 | 「比赛终端」列 | 已移除 |
| Admin 球队审核详情 | 「比赛终端」「成员数 5/8」 | 仅「成员数 5 人」 |
| 公开球队 Hero | 「比赛平台」+「注册阵容」 | 无平台卡片；「当前成员 N 人」 |

## 6. 保留赛事报名规则展示的页面

以下页面 **未改动**，继续在具体赛事上下文中展示平台、赛制、阵容要求：

- 我的球队 → **赛事报名** Tab（`registrationRuleSummary`、阵容候选、首发列）
- `TeamRegistrationFlow.vue`
- 我的赛事相关页面
- Admin 正式赛事球队、报名审核、阵容、比赛数据等

## 7. 修改文件列表

| 文件 | 变更 |
|------|------|
| `basketball-web-ui/src/views/account/team/index.vue` | 概览成员字段；删除 `teamMemberLimitHint` |
| `basketball-web-ui/src/components/account/team/JoinableTeamPanel.vue` | 成员数文案 |
| `basketball-web-ui/src/views/tournament/team/composables/usePublicTeamPage.ts` | 不再用 gameTerminal 填充 platform |
| `basketball-web-ui/src/components/team/TeamHero.vue` | 隐藏空平台；「当前成员」；动态 info 卡片 |
| `basketball-admin-ui/src/views/cnpa/identity/team-certification/index.vue` | 移除比赛终端；成员数简化 |
| `docs/cnpa-long-term-team-remove-event-rule-wording-report.md` | 本报告 |

**未改后端 VO / 数据库** — 赛事规则字段仍保留在 `TeamRegistrationDetailResp` 等报名 VO 中，仅前端展示层分离。

## 8. 测试结果

| # | 用例 | 结果 |
|---|------|------|
| 1–3 | Web 概览无 5v5 / 首发 / 正式阵容 5–8 | 代码审查通过 |
| 4 | Web 概览成员为「N 人」 | 代码审查通过 |
| 5–7 | 资料/成员/队员详情无赛事规则 | 代码审查通过 |
| 8–10 | 赛事报名 Tab 仍展示规则与校验 | 未改动 registration 区块 |
| 11–12 | Admin 球队审核无 5v5/平台/ x/y 成员 | 代码审查通过 |
| 13–15 | 正式赛事球队/报名审核/赛程不受影响 | 未改相关文件 |
| 构建 | web-ui + admin-ui | **均通过** |

## 9. 构建结果

```
basketball-web-ui: npm run build — 成功
basketball-admin-ui: npm run build:prod — 成功
basketball-web / basketball-admin: 本轮未改后端
```

## 10. 遗留问题

1. `TeamDetailResp.gameTerminal` 等字段仍存在于后端长期球队 VO，但 Web/Admin 长期球队展示页已不再使用；若未来需彻底废弃该 DB 字段，需单独产品决策（本轮不改库）。
2. Admin 球队审核详情中审核提示仍提及「比赛终端」合规校验（审核业务规则），与「不在基础资料展示平台」不冲突。
3. 成员管理页「正式/试训」为 **成员关系类型**，非赛事「正式阵容」，保留不变。

---

## 最终结论

| 问题 | 答案 |
|------|------|
| 1. 长期球队概览是否还显示 5v5？ | **否** |
| 2. 长期球队概览是否还显示首发人数？ | **否** |
| 3. 长期球队概览是否还显示阵容上下限？ | **否** |
| 4. 长期球队是否仍被错误展示为绑定平台？ | **否**（Web 概览/公开页/Admin 审核已清理） |
| 5. Web 长期球队页面是否已清理？ | **是** |
| 6. Admin 长期球队页面是否已清理？ | **是**（球队审核认证） |
| 7. 赛事报名页是否仍保留赛事规则校验？ | **是** |
| 8. 是否改动数据库？ | **否** |
| 9. 是否影响 EventTeam / EventPlayer 快照？ | **否** |
| 10. 是否构建通过？ | **是** |
