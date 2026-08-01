# CNPA Web「我的球队」Logo 展示修复报告

## 1. 当前问题

个人中心 → **我的球队** 页面存在 Logo 展示缺失：

1. 顶部 Hero 区域仅有球队名称与队长标识，右侧空白，未展示球队 Logo。
2. 概览 Tab「球队概览」只有文本字段，无 Logo 预览。
3. 球队资料编辑页有「更换 Logo」按钮，但无当前 Logo 预览；仅有 `logoUrl` 时才显示 `<img>`，无 Logo 时完全空白。
4. 接口已返回 `logoUrl`，但前端未在 Hero / 概览 / 资料页统一渲染。
5. 无 Logo 时缺少统一占位，页面显得空；图片加载失败时可能显示破图。

## 2. Logo 数据来源

**后端已有字段，本轮未改数据库、未改后端 VO。**

| 接口 | 字段 |
|------|------|
| `GET /basket/account/team/mine` | `TeamSummaryResp.logoUrl` |
| `GET /basket/account/team/{teamId}` | `TeamDetailResp.logoFileId`、`logoUrl` |
| `PUT /basket/account/team/{teamId}/logo` | 绑定后返回完整 `TeamDetailResp`（含 `logoUrl`） |

后端 `AccountTeamServiceImpl` 通过 `logoFileId` 查询 `basket_file_asset.accessUrl` 解析为 `logoUrl`。

前端 Store（`team.ts`）在 `loadTeamDetail`、`bindLogoAction`、`updateTeamAction` 中已同步 `logoUrl` 到 `teamDetail` 与 `myTeamSummary`。

## 3. Hero 区域展示修复

扩展 `AccountStatusBanner.vue`，新增可选 props：

- `logoUrl`
- `logoShortName`
- `logoTeamName`

当用户已有球队时，`team/index.vue` 传入上述字段，在 Hero 中间展示：

- **AccountTeamLogo**（88px）
- 球队简称（如 T1）

队长标识保持在右侧，布局为：`[标题/描述] [Logo+简称] [队长状态]`。窄屏下 Logo 区域自动换行，不遮挡状态标识。

## 4. 概览页展示修复

在「球队概览」面板顶部新增 `team-overview-header`：

- 左侧：球队 Logo（88px，有图显示图，无图占位）
- 右侧：球队名称、编号、简称、认证状态（队长可见）

下方保留原有详情列表（地区、成员、角色等）。

## 5. 球队资料编辑页预览修复

重构 Logo 区域为 `team-profile-logo`：

- 左侧：**AccountTeamLogo** 预览（96px）
- 右侧：「更换 Logo」按钮 + 上传说明

**上传模式：立即保存**

- 选择文件 → `uploadAccountFile` → `bindLogoAction(teamId, fileId)`
- 上传成功后立即写入数据库并返回 `logoUrl`
- 预览通过 `profileLogoPreviewUrl` 即时更新（先用 `accessUrl`，再以接口返回 `logoUrl` 为准）
- 「保存资料」仅保存名称/简称/公开状态/介绍，不影响 Logo

## 6. 占位符规则

新增 `AccountTeamLogo.vue` + `utils/teamLogo.ts`，统一处理：

| 优先级 | 占位内容 |
|--------|----------|
| 1 | 球队简称（如 T1） |
| 2 | 球队名称前 2 字 |
| 3 | CNPA |

样式：深灰/黑红渐变背景 + 红色描边与文字，与 CNPA 黑红风格一致。

**防破图：**

- 无 `logoUrl` → 不渲染 `<img>`，直接显示占位
- 有 URL 但加载失败 → `@error` 切换为占位
- `logoUrl` 变化时重置 error 状态

## 7. 是否补充后端 VO 字段

**否。** 后端 `TeamSummaryResp` / `TeamDetailResp` 已包含 `logoUrl`，`AccountTeamServiceImpl.resolveLogoUrl()` 已从 `logoFileId` 解析可访问 URL。

## 8. 修改文件列表

| 文件 | 变更 |
|------|------|
| `basketball-web-ui/src/utils/teamLogo.ts` | **新增** 占位文案与 URL 解析 |
| `basketball-web-ui/src/components/account/team/AccountTeamLogo.vue` | **新增** 统一 Logo / 占位组件 |
| `basketball-web-ui/src/components/account/AccountStatusBanner.vue` | Hero 可选 Logo 展示 |
| `basketball-web-ui/src/views/account/team/index.vue` | Hero / 概览 / 资料 / 创建页 Logo 渲染 |
| `docs/cnpa-web-my-team-logo-display-report.md` | 本报告 |

## 9. 测试结果

| # | 用例 | 结果 |
|---|------|------|
| 1 | 有 Logo 球队 Hero 显示 Logo | 代码审查通过 |
| 2 | 无 Logo 显示占位符 | AccountTeamLogo fallback |
| 3 | 概览页显示 Logo | team-overview-header |
| 4 | 资料页显示预览 | team-profile-logo |
| 5 | 更换 Logo 后预览更新 | profileLogoPreviewUrl + bindLogoAction |
| 6 | 刷新后 Logo 仍显示 | 后端持久化 + loadTeamDetail |
| 7 | logoUrl 为空不报错 | 占位分支 |
| 8 | 图片加载失败显示占位 | @error 处理 |
| 9 | 布局不被撑坏 | 固定 72–96px 尺寸 |
| 10 | 窄屏不溢出 | Hero brand 区域 responsive |
| 11 | npm run build | **通过** |
| 12 | mvn compile | **未改后端，无需编译** |

## 10. 构建结果

```
basketball-web-ui: npm run build — 成功
basketball-web: 本轮未改后端
```

## 11. 遗留问题

1. 创建球队页上传 Logo 仍为创建时一并提交（非立即绑定球队），与资料页「立即保存」模式不同，符合现有业务流程。
2. 个人中心「概览」首页（`/account/overview`）若需展示球队 Logo，可后续复用 `AccountTeamLogo`（非本轮范围）。
3. EventTeam / 正式赛事球队页 Logo 仍走本届快照字段，未改动。

---

## 最终结论

| 问题 | 答案 |
|------|------|
| 1. 我的球队 Hero 是否显示球队 Logo？ | **是** |
| 2. 概览页是否显示球队 Logo？ | **是** |
| 3. 球队资料编辑页是否显示当前 Logo 预览？ | **是** |
| 4. 无 Logo 时是否显示占位符？ | **是** |
| 5. 是否避免破图？ | **是** |
| 6. 是否改动数据库？ | **否** |
| 7. 是否改动球队业务逻辑？ | **否** |
| 8. 是否构建通过？ | **是** |
