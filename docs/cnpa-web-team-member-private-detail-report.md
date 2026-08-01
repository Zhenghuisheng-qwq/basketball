# CNPA Web 队内成员私有资料详情报告

> 生成时间：2026-08-01  
> 定位：修复「成员管理 → 查看资料」误跳公开球员主页；新增个人中心私有详情页与权限校验

---

## 1. 当前问题

队长在「我的球队 → 成员管理」点击「查看资料」时，原先通过 `goToPlayerDetail` 跳转到**公开球员主页**（无需 token），不符合队内管理场景。

---

## 2. 最终交互调整

| 操作 | 行为 |
|------|------|
| 查看资料 | 进入个人中心私有详情页（需登录 token） |
| 生涯数据 | 仍进入公开球员主页 / 生涯数据 Tab（可不依赖 token） |

口径：

- **查看资料** = 队内私有资料详情  
- **生涯数据** = 公开球员主页 / 公开数据页  

---

## 3. 新增或修改的路由

| 路由 | name | 说明 |
|------|------|------|
| `/account/team/:teamId/member/:teamMemberId` | `AccountTeamMemberDetail` | 个人中心布局内，`requiresAuth: true` |

页面：`basketball-web-ui/src/views/account/team/TeamMemberDetailView.vue`

---

## 4. 新增或修改的接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/basket/team/{teamId}/member/{teamMemberId}/detail` | 队内成员私有详情 |

实现：

- Controller：`TeamMemberBusinessController#getMemberDetail`
- Service：`ITeamMemberBusinessService#getMemberDetail` / `TeamMemberBusinessServiceImpl`
- Resp：`TeamMemberDetailResp`

未新增表、未改 migration、未改认证/报名/队长授权主链路。

---

## 5. 权限校验规则

1. 必须登录（`UserContext.getRequiredUserId`）。  
2. 当前用户必须是该 `teamId` 的有效成员。  
3. 目标 `teamMemberId` 必须属于同一 `teamId` 且为在队状态。  
4. **队长**（`roleType=CAPTAIN`）可查看本队任意成员。  
5. **普通成员**仅可查看**自己**的队内详情。  
6. 其他球队成员 / 外部用户 → `403 无权查看该队员资料`。  
7. 成员不存在或不属于该队 → `404`。

---

## 6. 成员管理列表按钮调整

文件：`TeamMemberList.vue`

- **查看资料** → `AccountTeamMemberDetail`（携带 `teamId` + `teamMemberId`）  
- **生涯数据** → 仍 `goToPlayerDetail(..., 'data', { dataMode: 'career' })`  

---

## 7. 队内成员详情页展示字段

已展示（有数据则显示）：

- 头像、昵称、CNPA 编号、当前球队、球队角色、位置、成员状态  
- 加入时间、球衣号、加入来源  
- 平台游戏 ID 列表 / 摘要（无则「未绑定」）  
- 本队参赛信息 / 数据摘要 / 记录区：本轮不做复杂统计，空态显示「暂无…」

---

## 8. 公开球员主页是否保留

**是。** 未删除公开球员主页；「生涯数据」仍跳转公开页。

---

## 9. 是否需要 token

| 页面 | 需要 token |
|------|------------|
| 队内成员私有详情 | **是**（`/account` + `requiresAuth` + 私有接口） |
| 公开球员生涯页 | 否（公开路由） |

---

## 10. 是否防止越权

**是。** 后端校验同队 + 队长/本人；前端仅隐藏入口不足以防越权，权限以服务端为准。

---

## 11. 修改文件列表

### basketball-web

- `controller/TeamMemberBusinessController.java`
- `service/ITeamMemberBusinessService.java`
- `service/impl/TeamMemberBusinessServiceImpl.java`
- `response/team/TeamMemberDetailResp.java`（新增）

### basketball-web-ui

- `router/index.ts`
- `components/account/team/TeamMemberList.vue`
- `views/account/team/TeamMemberDetailView.vue`（新增）
- `api/teamMember.ts`
- `api/types/teamMember.ts`

### docs

- `docs/cnpa-web-team-member-private-detail-report.md`（本报告）

---

## 12. 测试结果

| # | 测试项 | 结果 |
|---|--------|------|
| 1–5 | 队长 → 成员管理 → 查看资料进入私有页 | 代码已接（需重启 web 后目视） |
| 6 | 私有页需 token | 是（account 布局 + 接口） |
| 7 | 展示头像/昵称/编号/位置/角色/状态 | 是 |
| 8 | 展示游戏 ID | 是 |
| 9 | 生涯数据仍进公开页 | 是 |
| 10 | 未登录访问私有路由 | 由 `requiresAuth` 拦截 |
| 11–12 | 其他队队长 / 外部用户 | 后端 403 |
| 13 | 成员本人查看自己 | 允许 |
| 14 | 无游戏 ID 显示未绑定 | 是 |
| 15–16 | 构建 | 见下节 |

说明：本环境未做完整 HTTP 联调目视；权限与路由已静态落地并通过构建。

---

## 13. 构建结果

| 工程 | 命令 | 结果 |
|------|------|------|
| basketball-web-ui | `npm run build` | ✅ 通过 |
| basketball-web | `mvn compile -DskipTests` | ✅ 通过 |

---

## 14. 遗留问题

1. 参赛信息、数据摘要、最近比赛/处罚/审核记录本轮仅预留字段，未接复杂统计；页面空态为「暂无数据」。  
2. 若需普通队员互看队友简版资料，需另定产品规则后再放开权限。  
3. 请重启 `basketball-web` 后验收私有接口。

---

## 最终结论（必答）

| # | 问题 | 答案 |
|---|------|------|
| 1 | 「查看资料」是否仍跳公开页？ | **否** |
| 2 | 「查看资料」是否进入个人中心私有详情页？ | **是** |
| 3 | 私有详情页是否需要 token？ | **是** |
| 4 | 后端是否校验队长/同队权限？ | **是** |
| 5 | 「生涯数据」是否保留公开页跳转？ | **是** |
| 6 | 队员游戏 ID 是否展示？ | **是** |
| 7 | 是否修改了核心业务逻辑？ | **否** |

**本轮结论：查看资料已改为个人中心私有详情；生涯数据保留公开跳转；权限与 token 校验已落地；前后端构建通过。**
