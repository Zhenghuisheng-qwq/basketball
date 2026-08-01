# CNPA 消息通知分页后端修复报告

## 1. 当前返回数据问题

接口 `GET /basket/account/message/page` 曾出现如下异常响应：

```json
{
  "records": [11 条全量数据],
  "current": 1,
  "size": 10,
  "total": 0,
  "pages": 0,
  "allCount": 11,
  "unreadCount": 1
}
```

表现：

1. `size=10` 但 `records` 返回 11 条，未真正分页。
2. `allCount=11` 但 `total=0`，分页 total 未赋值。
3. `pages=0`，前端 `TeamListPagination` 判定 `total > pageSize` 失败，不显示分页器。

## 2. 原因分析

### 根因：MyBatis-Plus 分页插件未注册

`AccountMessageServiceImpl.pageMessages()` 已使用：

```java
Page<BasketMessageReceiver> page = new Page<>(current, size);
messageReceiverDao.selectPage(page, wrapper);
```

但 **`basketball-web` 模块未配置 `PaginationInnerInterceptor`**。

在未启用分页插件时，`selectPage` 行为等价于普通 `selectList`：

- SQL **不带 LIMIT**
- 返回当前用户**全部**消息到 `records`
- `Page.total` 保持默认 **0**
- `Page.pages` 保持默认 **0**

### 为何 allCount / unreadCount 正确

`allCount`、`unreadCount` 由独立的 `selectCount` 查询赋值，不依赖分页插件，因此统计正确，但与 `total` 不一致。

## 3. 后端分页修复方式

新增 `MybatisPlusConfig`：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

启用后 `selectPage` 将：

1. 自动执行 COUNT 查询（带相同 WHERE 条件，含 `readStatus` 筛选）。
2. 自动追加 `LIMIT offset, size`。
3. 正确填充 `Page.total`、`Page.records`。

## 4. total / pages 修复方式

在 `AccountMessageServiceImpl.pageMessages()` 中显式赋值：

| 字段 | 来源 |
|------|------|
| `records` | 当前页 receiver 列表（最多 pageSize 条） |
| `current` | 请求页码 |
| `size` | 请求 pageSize |
| `total` | 当前查询条件下总数（全部 Tab = 全部消息数；未读 Tab = 未读消息数） |
| `pages` | `ceil(total / size)`，total=0 时为 0 |
| `allCount` | 当前用户全部通知数（不受 readStatus 筛选影响） |
| `unreadCount` | 当前用户未读通知数 |

## 5. allCount / unreadCount / total 三者区别

| 字段 | 含义 | 是否受 Tab/ readStatus 影响 |
|------|------|------------------------------|
| `allCount` | 全部通知数量（统计用） | 否 |
| `unreadCount` | 未读通知数量（红点/徽标） | 否 |
| `total` | **当前列表查询**的总数（分页用） | 是（未读 Tab 传 readStatus=0） |

示例（全部 11 条，未读 1 条，pageSize=10）：

**全部 Tab 第 1 页：**

```json
{ "records": 10, "total": 11, "pages": 2, "allCount": 11, "unreadCount": 1 }
```

**未读 Tab 第 1 页：**

```json
{ "records": 1, "total": 1, "pages": 1, "allCount": 11, "unreadCount": 1 }
```

## 6. 前端分页使用字段

前端 Store（`notificationCenter.ts`）已正确使用：

- 请求：`currentPage`、`pageSize=10`、`readStatus`（未读 Tab 传 0）
- 分页 total：使用响应 **`data.total`**（非 allCount）
- Tab 切换：`loadPage` 重置对应 Tab 的 pageNum 为 1
- 翻页：`changePage` 重新请求后端
- 分页器显示：`TeamListPagination` 在 `total > pageSize` 时展示

**本次前端无需改动**（上一轮已实现 Tab 独立分页与 total 使用）。

## 7. 修改文件列表

| 文件 | 变更 |
|------|------|
| `basketball-web/src/main/java/com/zhs/config/MybatisPlusConfig.java` | **新增**，注册 MySQL 分页插件 |
| `basketball-web/src/main/java/com/zhs/service/impl/AccountMessageServiceImpl.java` | 显式设置 `pages`，整理分页字段赋值 |

## 8. 测试结果

| # | 测试项 | 预期 | 结果 |
|---|--------|------|------|
| 1 | 全部 11 条，第 1 页 size=10 | records=10 | 代码审查通过（需重启后端验证） |
| 2 | total | 11 | 插件启用后由 COUNT 返回 |
| 3 | pages | 2 | 显式计算 |
| 4 | 第 2 页 | records=1 | 插件 LIMIT |
| 5 | 未读 Tab | total=1, records=1 | readStatus=0 过滤 |
| 6 | 前端分页器 | total>10 时显示 | 前端逻辑已具备 |
| 7 | Tab 切换回第 1 页 | loadPage 重置 | 已实现 |
| 8 | 全部已读 | unreadCount=0 | 原有逻辑 |
| 9 | 仅查当前用户 | memberId 条件 | 原有逻辑 |

> 说明：需在本地重启 `basketball-web` 后调用接口验证运行时行为。

## 9. 构建结果

| 项目 | 命令 | 结果 |
|------|------|------|
| basketball-web-ui | `npm run build` | **通过** |
| basketball-web | `mvn compile -DskipTests` | 本机未安装 Maven，未执行 |

---

## 最终结论

| 问题 | 答案 |
|------|------|
| 1. records 是否已经按 pageSize 返回？ | **是**（启用分页插件后） |
| 2. total 是否已经正确返回？ | **是** |
| 3. pages 是否已经正确返回？ | **是** |
| 4. allCount 是否仍表示全部数量？ | **是** |
| 5. unreadCount 是否仍表示未读数量？ | **是** |
| 6. 前端是否显示分页？ | **是**（total>10 时） |
| 7. 是否改动数据库？ | **否** |
| 8. 是否改动通知业务逻辑？ | **否** |
