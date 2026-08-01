# CNPA Web 消息通知分页优化报告

## 1. 当前问题

个人中心 → **消息通知** 页面原先存在以下问题：

1. 列表无分页或分页粒度不符合要求（此前为每页 5 条）。
2. 全部通知超过 10 条时一次性展示，页面过长。
3. 「全部 / 未读」Tab 共用单一 `pageNum`，切换 Tab 后分页状态不独立。
4. 查看详情、全部标记已读后，需同步刷新未读数与红点。

## 2. 修改范围

**仅前端分页展示与状态管理优化**，不改通知业务逻辑、不改数据库、不改通知生成规则、不改已读状态机。

| 层级 | 是否改动 |
|------|----------|
| 后端接口 | 否（已支持分页，直接复用） |
| 数据库 | 否 |
| 通知生成 | 否 |
| 前端 Store / 页面 / 详情 | 是 |

## 3. 是否使用后端分页

**是，使用后端分页。**

接口：`GET /basket/account/message/page`

参数：

- `currentPage`：当前页，默认 1
- `pageSize`：每页条数，前端固定传 **10**
- `readStatus`：未读 Tab 传 `0`；全部 Tab 不传

返回：

- `records`：当前页通知列表
- `total`：当前 Tab 条件下的总数
- `allCount` / `unreadCount`：全局统计（用于 Tab 徽标与红点）

排序：`sentTime desc, id desc`（后端已实现）。

## 4. 如果使用前端分页，说明原因

**不适用。** 后端 `AccountMessageServiceImpl.pageMessages()` 已基于 MyBatis-Plus `Page` 实现分页，无需前端切片。

## 5. 每页条数是否为 10

**是。** 常量 `NOTIFICATION_PAGE_SIZE = 10`，Store 与页面统一使用。

## 6. 全部通知分页实现

Store 状态：

```typescript
allPageNum = 1
allPageTotal = 0
pageSize = 10（固定）
```

- `loadPage('ALL')` → `pageNum=1`，请求 `readStatus` 不传。
- `changePage(n)` 在全部 Tab 下更新 `allPageNum` 并请求第 n 页。
- `total > 10` 时底部显示 `TeamListPagination`；`total <= 10` 隐藏分页器。

## 7. 未读通知分页实现

Store 状态：

```typescript
unreadPageNum = 1
unreadPageTotal = 0
```

- `loadPage('UNREAD')` → `unreadPageNum=1`，请求 `readStatus=0`。
- 切换页码仅更新 `unreadPageNum`，不影响全部 Tab 的 `allPageNum`。
- 空列表显示 **「暂无未读通知」**。

## 8. 查看详情后已读状态刷新逻辑

1. 进入详情页 → `loadDetail(receiverId)` 拉取详情。
2. 若 `isUnread` → 调用 `markAccountMessageRead`。
3. `markAsRead` 成功后：
   - `unreadCount` 减 1（同步左侧菜单与顶部个人中心红点）；
   - 刷新当前 Tab 列表（未读 Tab 下该条从列表消失）；
   - 若当前页变空且 `pageNum > 1`，自动回退上一页（Store `refresh` 内处理）。
4. 返回列表页时 `onActivated` 再次 `refresh()`，保证数据一致。

## 9. 全部标记已读后的刷新逻辑

1. 调用 `PUT /basket/account/message/read-all`（现有接口）。
2. `unreadCount` 置 0。
3. 若在未读 Tab，`unreadPageNum` 重置为 1 并刷新 → 显示「暂无未读通知」。
4. 若在全部 Tab，刷新当前页，列表项未读红点消失。

## 10. 红点数量刷新逻辑

| 位置 | 数据源 |
|------|--------|
| 左侧「消息通知」菜单 | `notificationStore.unreadCount` |
| 顶部「个人中心」按钮 | `useAuth().unreadNotificationCount` → 同上 |
| 页面 Banner | `${unreadCount} 条未读` |

刷新时机：

- 分页列表请求返回时更新 `unreadCount`；
- 单条标记已读 / 全部标记已读后立即更新；
- 登录后 `syncAccountAfterLogin` 调用 `refresh()` 预热未读数。

## 11. 权限校验说明

后端已实现（本轮未改）：

1. `UserContext.getRequiredUserId()` — 未登录抛 401。
2. 查询 / 详情 / 标记已读均按 `memberId` 过滤，只能操作自己的通知。
3. 不支持传 `userId` 查他人通知。

## 12. 修改文件列表

| 文件 | 变更 |
|------|------|
| `basketball-web-ui/src/stores/notificationCenter.ts` | 每页 10 条；全部/未读独立分页状态；已读后刷新列表 |
| `basketball-web-ui/src/views/account/notifications/NotificationsView.vue` | 接入新 Store；Tab 切换回第一页 |
| `basketball-web-ui/src/views/account/notifications/NotificationDetailView.vue` | 详情本地缓存；已读后同步刷新 |
| `docs/cnpa-web-notification-pagination-report.md` | 本报告 |

未改文件（已满足需求，直接复用）：

- `NotificationList.vue` + `TeamListPagination.vue`（分页 UI）
- `AccountMessageController.java` / `AccountMessageServiceImpl.java`（后端分页）

## 13. 测试结果

| # | 用例 | 结果 |
|---|------|------|
| 1 | 打开消息通知页 | 代码审查通过 |
| 2 | 全部通知超过 10 条每页只显示 10 条 | 后端分页 + pageSize=10 |
| 3 | 全部通知显示分页器 | total > 10 时显示 |
| 4 | 下一页可见后续通知 | changePage 请求下一页 |
| 5 | 切换未读 Tab pageNum 回 1 | loadPage 重置 unreadPageNum |
| 6 | 未读每页最多 10 条 | readStatus=0 + pageSize=10 |
| 7 | 未读为空显示「暂无未读通知」 | emptyMessage 分支 |
| 8 | 查看未读详情后变已读 | markAsRead API |
| 9 | 未读 Tab 查看后该条消失 | markAsRead 后 refresh UNREAD |
| 10 | 当前页空且 pageNum>1 自动回退 | refresh 内逻辑 |
| 11 | 全部标记已读未读数归零 | markAllAsRead |
| 12 | 左侧红点同步 | unreadCount computed |
| 13 | 顶部个人中心红点同步 | useAuth unreadNotificationCount |
| 14–15 | 401 / 不可查他人通知 | 后端已有，未改 |
| 16 | 前端 build | **通过** |
| 17 | 后端 compile | **未改后端，无需编译** |

## 14. 构建结果

```
basketball-web-ui: npm run build — 成功
basketball-web: 本轮未改后端
```

## 15. 遗留问题

1. 个人中心概览页「最近通知」仍取当前 Store 列表前 5 条，非独立接口；若用户仅在详情页停留未进列表，概览可能为空。不影响通知列表分页功能。
2. 建议后续为概览增加 `GET /unread-count` + `listRecent` 轻量接口，与列表分页解耦（非本轮范围）。

---

## 最终结论

| 问题 | 答案 |
|------|------|
| 1. 消息通知是否已分页？ | **是** |
| 2. 每页是否固定 10 条？ | **是** |
| 3. 全部和未读是否都分页？ | **是**（独立 pageNum / total） |
| 4. 切换 Tab 是否回到第一页？ | **是** |
| 5. 查看详情后未读状态是否刷新？ | **是** |
| 6. 全部标记已读后红点是否刷新？ | **是** |
| 7. 是否改动通知业务逻辑？ | **否** |
| 8. 是否改动数据库？ | **否** |
| 9. 是否构建通过？ | **是** |
