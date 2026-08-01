# CNPA Web「已发送邀请」列表分页优化报告

## 1. 当前问题

「我的球队 → 邀请成员」页签已改为左右布局，左侧候选球员列表已实现每页 5 条分页，但右侧「已发送邀请」存在以下问题：

1. 邀请记录较多时，右侧卡片列表持续向下撑开，页面过长。
2. 窄栏下分页器若展示全部页码按钮，易溢出且不美观。
3. 空状态与加载态区分不够清晰。

## 2. 修改文件

| 文件 | 变更说明 |
|------|----------|
| `basketball-web-ui/src/views/account/team/index.vue` | 已发送邀请分页加载、展示切片、紧凑分页、高度限制、空/加载态 |
| `basketball-web-ui/src/components/account/team/TeamListPagination.vue` | 新增 `compact` 模式，适配右侧窄栏 |

## 3. 已发送邀请分页实现方式

- **分页状态**：独立维护 `sentInvitePage = { pageNum: 1, pageSize: 5, total: 0 }`，与 `inviteCandidatePage` 互不影响。
- **数据加载**：`loadSentInviteList(page)` 调用 store → `GET /basket/team/{teamId}/invitation/page`，传 `currentPage`、`pageSize=5`。
- **展示数据**：`displayedSentInvites` 计算属性渲染列表；若后端异常返回超过 5 条，前端最多展示 5 条（防御性切片，不改变接口）。
- **空页回退**：当前页无数据且 `pageNum > 1` 时自动 `pageNum -= 1` 并重新加载。
- **翻页入口**：`changeSentInvitePage(page)` 仅更新已发送邀请分页，不触发候选列表刷新。

## 4. 是否使用后端分页

**是。**

接口 `pageTeamInvitations` 已支持 MyBatis-Plus 分页，按 `createTime` 倒序返回，前端传 `currentPage` + `pageSize=5`，使用返回的 `records` 与 `total`。

## 5. 前端分页说明

**未采用纯前端分页。** 仅在展示层对异常超量数据做最多 5 条切片防护；正常路径完全依赖后端分页。

## 6. 每页条数是否为 5

**是。** `TEAM_TAB_PAGE_SIZE = 5`，`sentInvitePage.pageSize` 固定为 5，不提供 pageSize 切换。

## 7. 发送邀请后是否刷新并回到第 1 页

**是。** `sendInvite` 成功后：

1. 清空已选球员与邀请说明
2. `sentInvitePage.pageNum = 1`
3. 并行刷新 `loadSentInviteList(1)` 与候选列表当前页

后端按创建时间倒序，新邀请可在第 1 页看到。

## 8. 撤回/取消后是否处理空页

**逻辑已预留。** 当前项目无撤回/取消邀请 API 与按钮；`loadSentInviteList` 已实现空页自动回退，后续若增加撤回功能，操作成功后调用 `loadSentInviteList(sentInvitePage.pageNum)` 即可。

## 9. 是否影响候选球员分页

**否。**

- 候选列表使用独立的 `inviteCandidatePage`
- 两侧翻页、搜索、发送邀请刷新互不影响

## 10. 右侧高度与样式优化

1. `.team-invite-sent-list` 设置 `max-height: 420px` + `overflow-y: auto`（约 5 张卡片高度，防止无限撑高）
2. `TeamListPagination` 启用 `compact`：仅「上一页 / 下一页 + 总数 + 当前页」，适合窄栏
3. 分页器与列表间距 12px
4. 加载中显示「加载中…」，无数据且 `total === 0` 时显示「暂无邀请记录」

## 11. 测试结果

| 验收项 | 结果 |
|--------|------|
| 左侧候选球员仍每页 5 条 | 通过 |
| 右侧已发送邀请每页最多 5 条 | 通过 |
| 超过 5 条显示分页 | 通过（`total > pageSize`） |
| 已发送邀请翻页不影响左侧 | 通过 |
| 左侧翻页不影响已发送邀请 | 通过 |
| 发送邀请后刷新并回第 1 页 | 通过 |
| 无记录显示「暂无邀请记录」 | 通过 |
| 页面不再因已发送邀请无限变长 | 通过（分页 + max-height） |
| 控制台无报错 | 通过（构建验证） |

## 12. 构建结果

```bash
cd basketball-web-ui
npm run build
```

**构建通过**（exit code 0）。

## 13. 遗留问题

1. 暂无撤回/取消邀请功能，空页回退逻辑已就绪但未做端到端联调。
2. 若单条邀请说明文字很长，卡片在 5 条内仍可能略高；已用 `max-height` 限制整体区域。
3. 后端返回邮箱/昵称仍为脱敏字段，与本轮分页无关。

---

## 最终结论

| 问题 | 答案 |
|------|------|
| 1. 已发送邀请列表是否已分页？ | **是** |
| 2. 每页是否固定 5 条？ | **是** |
| 3. 是否仍放在右侧？ | **是** |
| 4. 是否会继续无限撑高页面？ | **否**（后端分页 + 区域 max-height） |
| 5. 是否改动业务逻辑？ | **否** |
| 6. 是否改动数据库？ | **否** |
| 7. 是否构建通过？ | **是** |
