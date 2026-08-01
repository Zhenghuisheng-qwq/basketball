# CNPA Web 队内私有队员资料页 — 权限与展示收口检查报告

**日期：** 2026-08-01  
**范围：** Web 端「我的球队 → 成员管理 → 查看资料」  
**原则：** 不做大改页面；不改库、不改认证/平台绑定/成员状态机/公开球员主页/比赛数据模块。

---

## 1. 私有资料页是否需要 token

**是，必须携带 token。**

| 层级 | 结论 |
|------|------|
| 前端路由 | `/account/team/:teamId/member/:teamMemberId`（`AccountTeamMemberDetail`）挂在 `/account` 下，`meta.requiresAuth: true`；未登录跳转登录页 |
| 前端请求 | `getTeamMemberDetail` → `GET /basket/team/{teamId}/member/{teamMemberId}/detail`；`request` 拦截器对有 token 的请求设置 `Authorization: Bearer …` |
| 后端拦截 | `TeamMemberBusinessController` 无 `@NoLoginRequest`；`LoginInterceptor` 对未登录返回 **HTTP 401** |
| 业务入口 | `UserContext.getRequiredUserId()` 取当前登录会员 |

未登录访问页面 → 前端跳登录；未登录打接口 → 401，前端拦截器触发重新登录。

---

## 2. 后端是否做队长 / 同队权限校验

**是。**

实现位置：`TeamMemberBusinessServiceImpl#getMemberDetail`

校验顺序：

1. `captainPermissionService.requireTeamMember(viewerId, teamPk)`  
   - 当前用户必须是该队 **ACTIVE + is_current** 成员  
   - 否则：`403 您不是该球队成员`  
   - 覆盖：其他球队队长、非本队成员、外部普通用户
2. 目标成员必须属于该 `teamId`
3. **队长**可查看本队任意成员；**普通成员仅可查看本人**  
   - 否则：`403 无权查看该队员资料`

权限矩阵：

| 访问者 | 目标 | 结果 |
|--------|------|------|
| 本队队长 | 本队任意成员 | 允许 |
| 成员本人 | 自己 | 允许 |
| 本队普通成员 | 其他队友 | 403 |
| 其他球队队长 | 非本队成员 | 403（非本队成员） |
| 非本队成员 / 外部用户 | 任意 | 403（非本队成员） |
| 未登录 | 任意 | 401 |

---

## 3. 「查看资料」是否不再跳公开页

**是，已跳私有队员资料详情页。**

- 入口：`TeamMemberList.vue` → `openProfile`
- 路由：`name: 'AccountTeamMemberDetail'`
- path：`/account/team/:teamId/member/:teamMemberId`
- **不再**调用 `goToPlayerDetail` / 公开 `/player/:playerId`

页面标题：队员资料 / `TEAM MEMBER DETAIL`。

---

## 4. 「生涯数据」是否仍跳公开页

**是，保持不变。**

| 入口 | 行为 |
|------|------|
| 成员列表「生涯数据」 | `openCareerData` → `goToPlayerDetail(..., 'data', { dataMode: 'career', teamId? })` |
| 私有详情页「生涯数据」 | `openPublicCareer` → 同上（公开球员主页数据 Tab） |

公开球员主页路由：`/player/:playerId`（`PlayerDetail`），本轮未改。

---

## 5. 游戏 ID 是否正常展示

**是（只读展示，未改绑定逻辑）。**

| 项 | 实现 |
|----|------|
| 后端组装 | `PlatformGameIdDisplaySupport`：仅 ACTIVE 且有 `displayGameId`；按平台去重；格式 `Xbox：xxx` / `PS：yyy` |
| 详情字段 | `platformGameIds` + `gameIdSummary` |
| 前端展示 | `TeamMemberDetailView`：`gameIdLines` 优先列表逐行展示；多平台多行；无数据显示「未绑定」 |

展示示例：

- 单平台：`Xbox：player111117`
- 多平台：多行（或摘要 `PS：a / Xbox：b`）
- 无绑定：`未绑定`

---

## 6. 本轮文案小优化

| 位置 | 调整前 | 调整后 |
|------|--------|--------|
| 详情页标签 | 加入来源 | **入队方式** |
| 入队方式文案（APPLY） | 申请 | **申请加入** |
| 入队方式文案（INVITE） | 邀请 | **队长邀请** |
| 入队方式文案（CREATE） | 创建 | 创建（不变） |
| 数据摘要空态 | 暂无比赛数据 | **暂无本队比赛数据** |

说明：仅在详情接口的 `resolveJoinSourceText` 与前端展示层调整文案，未改 `TeamJoinSourceEnum` 存库 code，未改成员状态机。

---

## 7. 修改文件

| 文件 | 变更 |
|------|------|
| `basketball-web-ui/src/views/account/team/TeamMemberDetailView.vue` | 「入队方式」标签；「暂无本队比赛数据」空态 |
| `basketball-web/src/main/java/com/zhs/service/impl/TeamMemberBusinessServiceImpl.java` | 入队方式展示文案：申请加入 / 队长邀请 |
| `basketball-web/src/main/java/com/zhs/response/team/TeamMemberDetailResp.java` | Swagger 字段说明：入队方式 |
| `docs/cnpa-web-team-member-private-detail-check-report.md` | 本报告 |

**未改动（按约束）：** 数据库、球员认证、平台游戏 ID 绑定、球队成员状态机、公开球员主页、比赛数据模块。

**已确认无需再改：**

- `TeamMemberList.vue` 查看资料 / 生涯数据跳转
- 路由 `AccountTeamMemberDetail` + `requiresAuth`
- Controller / Service 权限链路
- `PlatformGameIdDisplaySupport` 游戏 ID 展示

---

## 8. 构建结果

| 模块 | 命令 | 结果 |
|------|------|------|
| `basketball-web-ui` | `npm run build`（`vue-tsc -b && vite build`） | **通过**（exit 0；仅有 chunk 体积告警） |
| `basketball-web` | `D:\env\maven\apache-maven-3.9.11\bin\mvn.cmd -DskipTests compile` | **通过**（exit 0） |

---

## 9. 结论

队内「查看资料」已收口为个人中心私有详情页：强制登录 token、同队 +（队长或本人）鉴权、生涯数据仍走公开球员主页、游戏 ID 按平台只读展示。本轮仅完成文案与展示收口，无业务状态机与公开页改动。
