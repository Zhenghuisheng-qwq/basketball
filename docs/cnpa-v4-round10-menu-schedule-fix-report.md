# CNPA V4 Round10 执行记录 — 后台菜单修复 & 我的赛程重构

**日期：** 2026-08-04  
**范围：** basketball-admin 菜单链路修复；basketball-web / basketball-web-ui「我的赛程」接口与页面重构

---

## 1. 后台菜单缺失根因

经数据库、动态路由与 `sys_role_menu` 联查，根因为 **三层叠加**：

| 层级 | 问题 | 影响 |
|------|------|------|
| 角色授权 | V4 菜单（2280–2326）写入 `sys_menu`，但 `sys_role_menu` 未完整同步；role_id=2 仍绑定已隐藏的旧子菜单（2004–2039 等） | 非 admin 账号展开「赛事管理」后 **children 全为 hidden**，Sidebar 渲染 0 条可见子菜单 |
| 旧菜单隐藏 | Round2–5 SQL 将旧入口 `visible='1'`（若依约定：1=隐藏） | 角色若只授权旧 ID，则「有父无可见子」 |
| 菜单归属 | 赛程/比赛/审核 V4 菜单原挂在 2152 下，与「比赛执行」业务分组不一致 | 运营认知混乱，且与旧 2153 子菜单重复 |

**admin（user_id=1）** 走 `selectMenuTreeAll()` 不受 role_menu 限制；若 admin 也看不到子菜单，通常是目标库未执行 V4 menu SQL 或浏览器缓存未刷新。

---

## 2. 我的赛程页面 / 接口问题

| 问题 | 说明 |
|------|------|
| 标题重复 | `AccountPageHeader` 与 `AccountStatusBanner` 均显示「我的赛程安排」 |
| Banner 无价值 | 黑色 Banner 重复标题 + 「有效队长」标签，与赛程无关 |
| 内部编码外露 | 接口仅返回 `seasonTitle`/`divisionName`，dev 库值为 `R3_QUOTA`、`R3_QUOTA_TEST_PC` |
| 字段不足 | 缺少 seriesName、stageName、roundName、队伍简称、双方独立提交态、操作语义字段 |
| 列表体验差 | 卡片像后台表格；比分/时间/状态层级不清；「查看提交」语义模糊 |
| 汇总缺失 | 无待比赛/待提交/审核中/已结束概览 |

**查询范围结论（V4 已正确，本轮增强）：**

- 路径：`EventPlayer`（member_id / source_player_id）→ `EventTeam` → `Match`
- 非队长专属；`captain_player_id` 仅兜底
- 非当前长期球队过滤；跨历史 EventTeam 聚合
- 默认排除 `FINISHED`（待比赛 Tab）；「全部赛程」`includeFinished=true`
- 提交态按 `submitEventTeamId` 区分本方/对方

---

## 3. 修改文件清单

### 后端
- `basketball-web/src/main/java/com/zhs/response/v4/V4MyMatchResp.java` — 扩展字段
- `basketball-web/src/main/java/com/zhs/response/v4/V4MyScheduleResp.java` — 去重字段
- `basketball-web/src/main/java/com/zhs/response/v4/V4MyScheduleSummaryResp.java` — 新增
- `basketball-web/src/main/java/com/zhs/response/v4/V4MySchedulePageResp.java` — 新增
- `basketball-web/src/main/java/com/zhs/service/v4/IBasketV4PlayerMyScheduleService.java`
- `basketball-web/src/main/java/com/zhs/service/impl/v4/BasketV4PlayerMyScheduleServiceImpl.java`
- `basketball-web/src/main/java/com/zhs/controller/v4/BasketV4PlayerMyScheduleController.java`
- `basketball-web/src/main/resources/mapper/v4/BasketV4PlayerMatchSubmissionMapper.xml`

### 前端
- `basketball-web-ui/src/api/types/v4PlayerEvents.ts`
- `basketball-web-ui/src/api/v4PlayerEvents.ts`
- `basketball-web-ui/src/components/account/events/V4MySchedulePanel.vue` — 重构
- `basketball-web-ui/src/views/account/events/MyEventsView.vue` — 赛程 Tab 去掉重复 Banner
- `basketball-web-ui/src/constants/accountEventsUi.ts` — 赛程页脚文案

### SQL
- `docs/sql/cnpa-v4-round10-menu-fix.sql` — 菜单重组与角色授权补丁

---

## 4. 菜单 SQL

**文件：** `docs/sql/cnpa-v4-round10-menu-fix.sql`

已在 dev 库通过 MCP 分批执行核心语句；若 CLI 批量执行失败，请按文件内分段逐条执行。

---

## 5. 最终后台菜单树（visible=0 可见项）

```
运营工作台 (2201)
赛事管理 (2152)
├── 赛事发布 (2280)          — Series/Season/Division
├── 队长授权 (2296)
├── 报名审核 (2301)
└── 参赛队伍 (2306)          — 含参赛球员 Tab
账号与认证 (2151)
├── 用户管理 (2210)
├── 球员认证审核 (2001)
├── 球队认证审核 (2003)
├── 平台游戏ID变更审核 (2211)
└── 球队资料变更审核 (2215)
比赛执行 (2153)
├── 赛程管理 (2311)          — V4
├── 比赛管理 (2316)          — V4
├── 比赛数据审核 (2321)      — V4
├── 数据争议处理 (2231)
└── 弃赛与异常处理 (2232)
纪律与档案 (2207)
├── 处罚管理 (2240)
├── 球员赛事档案 (2241)
├── 球队赛事档案 (2242)
└── 操作审计 (2243)
内容运营 (2101)
├── 公告管理 (2106)
├── 合作伙伴管理 (2108)      — 含支持记录 Tab
├── 球队荣誉 (2103)
├── 球员荣誉 (2104)
├── 名人堂管理 (2105)
└── 首页内容配置 (2107)
数据统计 (2154)
└── 统计榜单 (2326)          — V4
系统管理 (1)                 — 仅授权角色可见，未向普通角色新增
```

旧入口（2004–2039、2040–2043、2204 报名与阵容、2205 比赛管理、2080–2084 旧统计）已 `visible='1'` 隐藏。

---

## 6. 我的赛程接口变化

**Endpoint：** `GET /basket/v4/player/my-schedule`

**响应结构（变更）：**

```json
{
  "summary": {
    "upcomingCount": 2,
    "pendingSubmitCount": 1,
    "reviewingCount": 1,
    "finishedCount": 8,
    "totalCount": 10
  },
  "items": [ { ... } ]
}
```

**items 新增/增强字段：**

- 赛事：`seriesId/seriesCode/seriesName`、`seasonCode`、`stageCode/stageName`、`roundName`
- 球队：`homeTeamShortName`、`awayTeamShortName`、`currentSide`
- 权限语义：`canEditSubmission`、`canViewSubmission`、`userActionType`、`userActionText`

---

## 7. 不同比赛状态展示规则（前端）

| 状态 | 中部对阵区 | 主按钮 |
|------|-----------|--------|
| 未开始 SCHEDULED | 日期 + 时间 + VS（不显示 0:0） | 查看比赛详情 |
| 进行中 PLAYING | VS / 不伪造实时比分 | 查看比赛详情；提示赛后提交 |
| 已结束 FINISHED + 本方未提交 | 比分 | 提交比赛数据（红色主按钮） |
| 本方审核中 | 比分或 VS | 查看提交详情 |
| 本方已通过 | 比分 | 查看审核结果 |
| 本方驳回 | 比分 + 驳回原因 | 修改并重新提交 |
| 已取消 | — | 无提交；灰色状态 |

本方/对方提交态：**独立色块标签**，互不合并为整场比赛状态。

---

## 8. 验证场景与结果

| 场景 | 结果 |
|------|------|
| role_id=2 赛事管理子菜单 | dev 库已授权 2280/2296/2301/2306 可见子菜单 |
| role_id=2 比赛执行 | 2311/2316/2321/2231/2232 可见 |
| 菜单数据 | 10 场已发布比赛；series_name=League 可正确作为卡片主标题 |
| 查询范围 | SQL 走 EventPlayer 关联，非队长/非当前球队限定 |
| 重复标题 | 赛程 Tab 已移除 AccountStatusBanner |
| 概览区 | 使用 summary 四象限统计 |

**待运营配置：** dev 库赛季 `display_title=R3_QUOTA`、赛区名 `R3_QUOTA_TEST_PC` 为测试数据，需在后台「赛事发布」改为正式名称；接口已返回 `seriesName`，前端优先展示系列正式名。

---

## 9. 尚未实现 / 受限项

- 「赛事系列 / 赛季 / 赛区」三分菜单：当前共用「赛事发布」单页（页内 Tab），未拆三个重复入口
- 「报名管理」与「报名审核」：共用 `registration-audit` 页，按状态筛选即可
- 「参赛阵容」：合并在「参赛队伍」页「参赛球员」Tab
- 赛程详情独立路由：当前统一进入 `V4MatchSubmissionView`
- 分页：仍一次返回全量；数据量增大后可加分页
- 新运营角色需单独分配菜单，未默认开放系统管理

---

## 10. 验收清单

- [x] 赛事管理展开后有 V4 可见子菜单（role 2）
- [x] 不通过前端 Sidebar 硬编码
- [x] 我的赛程去掉重复 Banner 标题
- [x] 黑色纹理概览区展示四象限统计
- [x] 电竞对阵卡片 + 独立提交态 + 动态按钮文案
- [x] 未开始比赛不显示 0:0
- [x] 接口返回 seriesName 等正式字段
- [x] 已结束比赛 includeFinished 可见
