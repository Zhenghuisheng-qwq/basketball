# CNPA Web「继续报名」空列表跳转修复报告

## 1. 当前问题

在个人中心 → **我的赛事 → 可报名赛事** 中，某赛事已存在球队报名记录（状态为 DRAFT / 待提交），列表显示「继续报名」按钮。点击后却进入 **球队报名 → 选择赛季** 页面，并提示 **「当前暂无可报名赛事」**，无法继续编辑已有报名。

## 2. 问题原因

| 环节 | 问题 |
|------|------|
| **跳转** | `MyEventsView.goTeamRegistration()` 仅跳转到 `/account/my-events/:seasonId/team-registration?platformCode=...`，**未携带 `registrationId`** |
| **页面加载** | `TeamRegistrationFlow.vue` 的 `bootstrap()` 调用 `loadAvailableSeasons()`，该接口只返回**可新发起报名**的赛季；已有报名记录的赛季不在列表中 |
| **匹配逻辑** | Flow 页仅在 `availableSeasons` 中查找 `hasRegistration && registrationId`，找不到则停留在「选择赛季」空态 |
| **状态文案** | 后端 `resolveTeamRegistrationStatusText()` 使用 `getDescription()`，DRAFT 对用户显示为「草稿」而非「待提交」 |

根因：**「继续报名」误走了新建报名流程入口**，而不是已有报名详情入口。

## 3. 「继续报名」原跳转路径

```
我的赛事 → 可报名赛事
  → 点击「继续报名」(actionType=MANAGE_REGISTRATION)
  → /account/my-events/{seasonId}/team-registration?platformCode=PC
  → TeamRegistrationFlow.vue
  → loadAvailableSeasons()（不含已有报名赛季）
  → 选择赛季空列表：「当前暂无可报名赛事」
```

## 4. 修复后的跳转路径

| 场景 | 按钮 | 修复后路径 |
|------|------|-----------|
| 无报名记录 | 立即报名 / 开始报名 | `/account/my-events/{seasonId}/team-registration?platformCode=...`（新建流程） |
| DRAFT / 待提交 | 继续报名 | `/account/team?tab=registration&registrationId={id}` |
| 审核中 | 查看状态 | `/account/team?tab=registration&registrationId={id}` |
| 已驳回 | 重新报名 | `/account/team?tab=registration&registrationId={id}`（有 id 时） |
| 已通过 | 查看阵容 | `/account/my-tournaments/{seasonId}/team/roster` |
| 我的报名列表 | 继续报名 / 查看状态 | `/account/team?tab=registration&registrationId={id}` |

**加固**：若用户仍通过旧 URL 进入 `TeamRegistrationFlow`，页面会先查 `registrations` 列表；若该赛季已有报名记录，**自动重定向**到 `/account/team?tab=registration&registrationId=...`，不再展示空的选择赛季页。

```
我的赛事 → 可报名赛事
  → 点击「继续报名」
  → /account/team?tab=registration&registrationId={registrationId}
  → team/index.vue → openRegistrationFromQuery()
  → GET /basket/team/registration/{registrationId}
  → 展示报名详情、阵容候选、平台 ID 校验、提交入口
```

## 5. 是否使用 registrationId 加载已有报名

**是。**

- 主入口：`MyEventsView` 在 `MANAGE_REGISTRATION` / `RE_REGISTER` / `VIEW_REGISTRATION` 等场景优先读取列表项中的 `registrationId`，跳转至球队页报名 Tab。
- 详情加载：`team/index.vue` 的 `openRegistrationFromQuery()` → `teamStore.loadRegistrationDetail(registrationId)`，复用已有接口 `GET /basket/team/registration/{registrationId}`。
- **未新增重复后端详情接口**。

## 6. 是否仍保留新发起报名流程

**是。**

- `actionType === 'TEAM_REGISTRATION'` 且无 `registrationId` 时，仍进入 `/account/my-events/{seasonId}/team-registration` 新建流程。
- `TeamRegistrationFlow.vue` 继续调用 `loadAvailableSeasons()` 供**无报名记录**的赛季选择，逻辑未改 API 语义。

## 7. 是否避免重复创建报名

**是。**

- 「继续报名」不再进入选择赛季页，不会触发 `createRegistrationDraftAction`。
- Flow 页若检测到该赛季已有 `registrationId`，直接重定向到详情页。
- 可新报名赛事列表接口未改动，仍只展示无报名记录、可新发起的赛事。

## 8. 状态文案是否改为「待提交」

**是。**

- 后端 `TeamRegistrationBusinessServiceImpl.resolveTeamRegistrationStatusText()` 改为使用 `TournamentRegistrationStatusEnum.getDisplayDescription()`，DRAFT → **待提交**。
- 前端 `MyEventsView.registrationStatusText()`、`accountMappers.mapTeamRegistrationStatusText()` 此前已映射 0 → 待提交。
- 用户侧不再突出「草稿」。

## 9. 修改文件列表

| 文件 | 变更 |
|------|------|
| `basketball-web-ui/src/utils/registrationNavigation.ts` | **新增** 统一报名跳转工具 |
| `basketball-web-ui/src/views/account/events/MyEventsView.vue` | 修复「继续报名」等 action 跳转；修复我的报名列表 fallback |
| `basketball-web-ui/src/views/account/events/TeamRegistrationFlow.vue` | 支持 `query.registrationId`；已有报名自动重定向至详情 |
| `basketball-web/src/main/java/com/zhs/service/impl/TeamRegistrationBusinessServiceImpl.java` | DRAFT 状态展示文案改为「待提交」 |
| `docs/cnpa-continue-registration-empty-list-fix-report.md` | 本报告 |

## 10. 测试结果

| # | 用例 | 结果 |
|---|------|------|
| 1 | 存在 DRAFT 报名记录 | 列表含 `registrationId` + action `MANAGE_REGISTRATION`（代码审查通过） |
| 2 | 可报名赛事点击「继续报名」 | 跳转 `/account/team?tab=registration&registrationId=...`（代码审查通过） |
| 3 | 直接进入报名详情页 | `openRegistrationFromQuery` 加载详情（代码审查通过） |
| 4 | 展示报名编号、平台、联系人、候选成员 | 复用 `team/index.vue` 已有详情 UI（代码审查通过） |
| 5 | 不进入选择赛季空列表 | Flow 页有报名则 `goToRegistrationDetail` 重定向（代码审查通过） |
| 6 | 状态显示「待提交」 | 后端 `getDisplayDescription()` + 前端 mapper（代码审查通过） |
| 7–11 | 提交/审核/驳回/通过各按钮 | 按 action 映射至详情或 roster 页（代码审查通过） |
| 12 | 无报名记录仍可开始报名 | `TEAM_REGISTRATION` 无 id 时仍走 Flow（代码审查通过） |
| 13 | 前端 build | **`npm run build` 通过** |

> 说明：本环境未启动完整前后端联调，上述 1–12 项基于路由/接口/组件逻辑静态验证；建议在测试环境用队长账号 `111111@qq.com` 等做一次手工回归。

## 11. 构建结果

```
basketball-web-ui: npm run build — 成功（vue-tsc + vite build）
```

---

## 最终结论

| 问题 | 答案 |
|------|------|
| 1. 点击「继续报名」是否还会进入空的选择赛季页？ | **否**（主路径直达球队报名详情；旧 URL 也会重定向） |
| 2. 点击「继续报名」是否直接进入已有报名详情？ | **是** |
| 3. 是否使用 registrationId 加载数据？ | **是** |
| 4. 是否避免重复创建报名？ | **是** |
| 5. 是否改动数据库？ | **否** |
| 6. 是否影响审核通过后的 EventTeam/EventPlayer 快照？ | **否** |
