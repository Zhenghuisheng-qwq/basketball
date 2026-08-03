# CNPA 赛事域 V4 第 6 轮：前台赛事列表 / 详情 / 赛程 / 榜单 / 数据展示

> 轮次：R6  
> 日期：2026-08-04  
> 范围：公开 Web 官网前台接 V4 赛事域数据（不做个人中心、不做报名/比赛数据提交、不进入 R7）

---

## 1. 本轮完成内容

### 1.1 Web 后端 V4 公开查询 API

新增 `/basket/v4/public/*` 公开 API 全套（`@NoLoginRequest`，无需登录）：

- Series / Season / Division 查询
- 参赛队伍 / EventPlayer 阵容
- 已发布 Match 赛程与比赛详情（含单场 team/player stat）
- 积分榜 / 球队统计 / 球员统计 / 排行榜

业务规则落地：

- 仅返回 `publishStatus=PUBLISHED` 的 Season
- 不返回 OFFLINE Season
- Match 仅返回 `publishStatus=PUBLISHED`
- 已结束（FINISHED）比赛正常展示
- 比分取自 `basket_match.home_score / away_score`
- 所有榜单/队伍/赛程按 **Division 隔离**，不跨 Division 混查
- `file_id` 经 `BasketPublicSupport.loadAccessUrlMap()` 转 `accessUrl`

### 1.2 web-ui 前台 V4 化

- **赛事列表** `/tournament`：V4 Season 卡片 + Series 筛选（League / Arena / Cup / Open）
- **赛事详情** `/events/:seasonId`：Hero + Division 平台切换 + 5 Tab（概况/队伍/赛程/积分榜/数据榜单）
- **比赛详情** `/events/match/:matchId`：双方、比分、单场球队/球员数据
- **数据中心** `/data`：Season + Division 筛选，积分榜/球队/球员/排行榜
- **SafeImage** 组件：空图/加载失败显示占位，避免破图
- 保持 CNPA 黑红风格与 `site-shell` 版心

### 1.3 旧入口处理

| 路由 | 处理 |
|------|------|
| `/tournament` | 已接 V4 列表页 |
| `/events` | 仍 redirect → `/tournament` |
| `/events/:seasonId` | 新增 V4 详情 |
| `/events/match/:matchId` | 新增 V4 比赛详情 |
| `/tournament/legacy` | 旧 EventsPage 保留，供对照 |
| `/data` | 已接 V4 数据中心 |
| `/data/legacy` | 旧 data/index 保留 |

Header 赛事下拉：非 legacy 路由时改调 V4 Series API，选择后跳转 `/tournament?series=XXX`。

---

## 2. 新增 / 修改文件清单

### 2.1 basketball-web 后端

**新增 Response DTO（`response/v4/publicapi/`）**

- `V4PublicDivisionBriefResp.java`
- `V4PublicSeriesItemResp.java`
- `V4PublicSeasonItemResp.java`
- `V4PublicSeasonDetailResp.java`
- `V4PublicDivisionDetailResp.java`
- `V4PublicEventTeamItemResp.java`
- `V4PublicEventTeamDetailResp.java`
- `V4PublicEventPlayerItemResp.java`
- `V4PublicMatchItemResp.java`
- `V4PublicMatchDetailResp.java`
- `V4PublicMatchTeamStatItemResp.java`
- `V4PublicMatchPlayerStatItemResp.java`
- `V4PublicStandingItemResp.java`
- `V4PublicTeamStatItemResp.java`
- `V4PublicPlayerStatItemResp.java`
- `V4PublicRankItemResp.java`

**新增 Service**

- `service/v4/IBasketV4PublicService.java`
- `service/impl/v4/BasketV4PublicServiceImpl.java`

**新增 Controller（`controller/v4/publicapi/`）**

- `BasketV4PublicSeriesController.java`
- `BasketV4PublicSeasonController.java`
- `BasketV4PublicDivisionController.java`
- `BasketV4PublicTeamController.java`
- `BasketV4PublicMatchController.java`
- `BasketV4PublicStatsController.java`

### 2.2 basketball-web-ui 前台

**新增**

- `src/api/types/v4Public.ts`
- `src/api/v4Public.ts`
- `src/utils/v4Display.ts`
- `src/components/common/SafeImage.vue`
- `src/views/events/v4/V4EventsListPage.vue`
- `src/views/events/v4/V4SeasonDetailPage.vue`
- `src/views/events/v4/V4MatchDetailPage.vue`
- `src/views/events/styles/v4-events.scss`
- `src/views/data/V4DataPage.vue`

**修改**

- `src/router/index.ts` — 主路由接 V4，legacy 路由保留
- `src/views/events/composables/useCompetitionSelector.ts` — V4 Series 导航
- `src/components/layout/WebHeader.vue` — 扩展赛事路由匹配
- `src/views/events/EventsPage.vue` — 添加 @deprecated 说明

---

## 3. 新增 Web 后端公开 API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/basket/v4/public/series/list` | Series 列表 |
| GET | `/basket/v4/public/season/list` | Season 列表（仅 PUBLISHED，支持 seriesCode/seasonYear/seasonStatus 筛选） |
| GET | `/basket/v4/public/season/{seasonId}` | Season 详情 + Division 列表 + 默认 Division + 计数 |
| GET | `/basket/v4/public/division/{divisionId}` | Division 详情 |
| GET | `/basket/v4/public/team/list?divisionId=` | 参赛队伍列表 |
| GET | `/basket/v4/public/team/{eventTeamId}` | 参赛队伍详情 + EventPlayer 阵容 |
| GET | `/basket/v4/public/match/list?divisionId=` | 已发布 Match 列表（支持 roundNo/matchStatus/upcomingOnly/finishedOnly/eventTeamId） |
| GET | `/basket/v4/public/match/{matchId}` | Match 详情 + 阵容 + 单场统计 |
| GET | `/basket/v4/public/stats/standing?divisionId=` | 积分榜 |
| GET | `/basket/v4/public/stats/team-stat?divisionId=` | 球队统计 |
| GET | `/basket/v4/public/stats/player-stat?divisionId=` | 球员统计 |
| GET | `/basket/v4/public/stats/rank?divisionId=&rankType=` | 排行榜 |

---

## 4. 新增 / 修改 web-ui 页面清单

| 路径 | 组件 | 状态 |
|------|------|------|
| `/tournament` | `V4EventsListPage.vue` | V4 主入口 |
| `/events/:seasonId` | `V4SeasonDetailPage.vue` | 新增 |
| `/events/match/:matchId` | `V4MatchDetailPage.vue` | 新增 |
| `/data` | `V4DataPage.vue` | V4 主入口 |
| `/tournament/legacy` | `EventsPage.vue` | 旧版保留 |
| `/data/legacy` | `data/index.vue` | 旧版保留 |

---

## 5. 赛事列表 V4 化说明

- 数据源：`GET /basket/v4/public/season/list`
- Series 筛选：League / Arena / Cup / Open（query: `?series=LEAGUE`）
- Season 卡片展示：系列名、赛季标题、报名/比赛时间、状态、Division 平台标签（PC/PS/Xbox）
- OFFLINE Season 后端已过滤，前端空状态不报错
- 点击进入 `/events/:seasonId`

---

## 6. 赛事详情 V4 化说明

- 数据源：`GET /basket/v4/public/season/{seasonId}`
- Hero：标题、Series、年份、Banner/Cover（SafeImage 占位）
- 5 个 Tab：overview / teams / schedule / standing / stats
- query 支持：`?tab=schedule&divisionId=xxx`

---

## 7. Division 平台切换说明

- 详情页顶部按钮切换 Division（PC / PS / Xbox）
- 切换后重新加载当前 Tab 数据（队伍/赛程/积分榜/统计均带 `divisionId`）
- 不允许跨 Division 混展示

---

## 8. 参赛队伍展示说明

- 列表：`GET /basket/v4/public/team/list?divisionId=`
- 卡片：队名、Logo（SafeImage）、队长、平台
- 点击展开阵容：`GET /basket/v4/public/team/{eventTeamId}` → EventPlayer 表格

---

## 9. 赛程展示说明

- 列表：`GET /basket/v4/public/match/list?divisionId=`
- 按轮次分组展示
- 筛选：全部 / 未开始 / 已结束
- 已结束比赛展示比分（home_score/away_score）
- 未结束显示 VS + 状态文案
- 点击跳转 `/events/match/:matchId`

---

## 10. 积分榜 / 统计 / 排行榜展示说明

| Tab | API |
|-----|-----|
| 积分榜 | `/basket/v4/public/stats/standing` |
| 球队统计 | `/basket/v4/public/stats/team-stat` |
| 球员统计 | `/basket/v4/public/stats/player-stat` |
| 排行榜 | `/basket/v4/public/stats/rank?rankType=POINTS/ASSIST/REBOUND` |

空数据展示「暂无数据」，不抛前端异常。

---

## 11. 比赛详情展示说明

- 路由：`/events/match/:matchId`
- API：`GET /basket/v4/public/match/{matchId}`
- 展示：双方队伍、Logo、比分、时间、状态、venue
- 单场球队数据（`basket_match_team_stat`）
- 单场球员数据（`basket_match_player_stat`）
- 无数据时显示「暂无数据」

---

## 12. `/data` 数据中心处理说明

- 主路由 `/data` 已改接 `V4DataPage.vue`
- Season 下拉 + Division 下拉（来自 V4 Season 列表的 divisions）
- 四个面板：积分榜 / 球队统计 / 球员统计 / 排行榜
- 旧版保留于 `/data/legacy`（仍调用 `/basket/public/data-center/*`）

---

## 13. 旧前台入口处理结果

| 项目 | 结果 |
|------|------|
| `/tournament` 主入口 | 已接 V4 |
| `/events` | redirect，间接 V4 |
| Header 赛事下拉 | 非 legacy 时接 V4 Series |
| 旧 EventsPage | `/tournament/legacy` + @deprecated |
| 旧 data 页 | `/data/legacy` |
| Open 报名入口 | 账号侧路由未开放（本轮未改） |
| `competition` query | 仅 legacy 页使用，V4 主逻辑用 `series` / `seasonId` / `divisionId` |

**R7/R8 清理建议：**

- 删除 `/tournament/legacy`、`/data/legacy`
- 移除 `publicTournament.ts` 等在公开页的引用
- 清理 `usePublicSeasonContext.ts` 旧 tournament 逻辑
- Header 完全移除 legacy fallback

---

## 14. 新旧 API 隔离检查结果

### V4 前台新代码（`views/events/v4/*`、`views/data/V4DataPage.vue`、`api/v4Public.ts`）

| 检查项 | 结果 |
|--------|------|
| `/basket/public/tournament` | 未发现 |
| `/basket/public/season` | 未发现 |
| `/basket/public/match` | 未发现 |
| `/basket/public/standing` | 未发现 |
| `tournamentId` 作为主逻辑 | 未发现 |
| `eventId` | 未发现 |
| `competition` query 作为主逻辑 | 未发现 |

### 仍调用旧 API 的位置（兼容用途）

| 位置 | 用途 |
|------|------|
| `views/events/EventsPage.vue` + composables | `/tournament/legacy` 专用 |
| `views/data/index.vue` | `/data/legacy` 专用 |
| `useCompetitionSelector.ts` | legacy 路由 + inject 旧 context 时 fallback |
| 账号侧 `/account/*` | 本轮未改造（符合范围） |

---

## 15. UI 冒烟记录

| 场景 | 预期 | 代码层结论 |
|------|------|------------|
| 访问 `/tournament` | V4 Season 列表 + Series 筛选 | 路由已指向 V4EventsListPage |
| Series 筛选 League/Cup 等 | query `series` 过滤 | selectSeries + watch 已实现 |
| Season 卡片 Division 标签 | PC/PS/Xbox | divisions 循环渲染 |
| 进入 `/events/:seasonId` | 详情 Hero + Tabs | V4SeasonDetailPage |
| Division 切换 | 各 Tab 数据按 divisionId 重载 | setDivision + loadTabData |
| 参赛队伍 + 阵容展开 | team list + detail | toggleTeamRoster |
| 赛程按轮次 + 筛选 | match list 分组 | matchesByRound + scheduleFilter |
| 已结束比赛比分 | formatScore | FINISHED 或有 score 时展示 |
| 积分榜 / 统计 / 排行 | stats API | 各 Tab 已实现 |
| 比赛详情 | `/events/match/:id` | V4MatchDetailPage |
| `/data` V4 榜单 | V4DataPage | Season/Division 筛选 + 4 面板 |
| 空数据 | 空状态文案 | v4-state 占位 |
| 图片为空 | SafeImage 占位 | 不破图 |

> 注：本轮按需求不做 Maven/构建验收；冒烟为代码路径与 API 接线审查。

---

## 16. 风险与待确认项

1. **环境数据依赖**：若库中无 PUBLISHED Season / PUBLISHED Match，前台将显示空状态（符合预期，需 R2–R5 种子数据或真实发布数据验证）。
2. **Open Series**：列表筛选含 Open，但本轮不做 Open/PERSONAL 报名入口；Open 赛季若存在仅展示，不提供报名。
3. **Header 下拉与列表页 Series 筛选**：两处独立，需产品确认是否合并交互。
4. **旧 `/tournament/team/*` 公开球队页**：仍走旧 public team API，与 V4 EventTeam 未打通（建议 R7 处理）。
5. **分页**：V4 公开 API 当前全量返回（Division 内数据量可控）；大数据量赛季后续可加 PageHelper。
6. **缓存**：未加 Redis 短缓存（旧 public API 有 cacheGetOrLoad）；高流量时可补。

---

## 17. 是否建议进入第 7 轮

**建议进入第 7 轮**，方向：

- 个人中心「我的赛事 / 我的赛程」V4 化（`/basket/v4/player/*` 已有 R3/R5 基础）
- 清理 legacy 路由与旧 public tournament API 引用
- 公开球队页 `/tournament/team/*` 对接 EventTeam 或 sourceTeam 映射
- Open/PERSONAL 如需开放，单独评审

---

## 18. 验收标准对照（R6）

| # | 标准 | 状态 |
|---|------|------|
| 1 | 前台可见 V4 Season 列表 | ✅ |
| 2 | League/Arena/Cup/Open 筛选 | ✅ |
| 3 | Season 卡片 Division 标签 | ✅ |
| 4 | 进入 V4 赛事详情 | ✅ |
| 5 | Division 切换 | ✅ |
| 6 | 参赛队伍展示 | ✅ |
| 7 | EventPlayer 阵容 | ✅ |
| 8 | 已发布赛程 | ✅ |
| 9 | 已结束比赛展示 | ✅ |
| 10 | 比分展示 | ✅ |
| 11 | 积分榜 | ✅ |
| 12 | 球队统计 | ✅ |
| 13 | 球员统计/排行榜 | ✅ |
| 14 | 比赛详情 | ✅ |
| 15 | `/data` 不依赖旧 tournament | ✅（主路由 V4） |
| 16 | V4 前台不调旧 public tournament API | ✅（主路径） |
| 17 | Open 报名不开放 | ✅ |
| 18 | 图片空不破图 | ✅ SafeImage |
| 19 | 空数据不报错 | ✅ |
| 20 | 不做个人中心完整改造 | ✅ |
| 21 | 不做比赛数据提交页 | ✅ |
| 22 | 不进入第 7 轮实现 | ✅（仅建议） |

---

## 19. 第 6 轮真实页面冒烟补验收记录

**验收时间：** 2026-08-04  
**后端：** `http://127.0.0.1:8881`（重启加载 R6 代码后）  
**前端：** `http://127.0.0.1:5173`（Vite dev + proxy `/web-api`）  
**冒烟脚本：** `scripts/smoke_v4_r6_public_web.py`  
**输出 JSON：** `docs/cnpa-v4-round6-smoke-output.json`

### 19.1 测试 ID

| 项 | 值 | 说明 |
|----|-----|------|
| **SeasonId（详情/队伍）** | `2` | `publish_status=PUBLISHED`，League 系列 |
| **DivisionId（详情/队伍）** | `3` | PC 赛区，1 支 EventTeam |
| **EventTeamId（阵容）** | `13` | CNPA战队Alpha，5 名 EventPlayer |
| **SeasonId（赛程/统计）** | `4` | R5 验收赛季，冒烟前补发布为 PUBLISHED |
| **DivisionId（赛程/统计）** | `5` | R5 单赛区 PC |
| **MatchId（比赛详情）** | `1` | FINISHED，比分 88-75，含单场/累计统计 |

### 19.2 `/tournament` 验收结果 — **通过**

| 检查项 | 结果 |
|--------|------|
| 页面 HTTP 200 | ✅ |
| `GET /basket/v4/public/season/list` | ✅ code=200，返回 2 条 PUBLISHED Season |
| Series 筛选 `?seriesCode=LEAGUE` | ✅ 返回 2 条 |
| Season 卡片 / Division 标签 | ✅ API 含 divisions[0].platformCode=PC |
| 空数据不报错 | ✅ 无 OFFLINE Season 泄漏 |

### 19.3 `/events/:seasonId` 验收结果 — **通过**

**测试 URL：** `/events/2`（队伍/概况）；`/events/4?divisionId=5&tab=schedule`（赛程/统计）

| Tab / 功能 | 结果 | 说明 |
|------------|------|------|
| Hero | ✅ | seasonId=2 displayTitle 正常 |
| Division 切换 | ✅ | season2 仅 PC 赛区；season4 可切 divisionId=5 |
| 概况 Tab | ✅ | 报名/赛制/时间字段 API 有值 |
| 参赛队伍 Tab | ✅ | divisionId=3 返回 1 支队伍 |
| 阵容展开 | ✅ | eventTeamId=13 返回 5 名球员 |
| 赛程 Tab | ✅ | divisionId=5 返回 1 场 PUBLISHED Match |
| 已结束比分 | ✅ | matchId=1 homeScore=88 awayScore=75 |
| 积分榜 Tab | ✅ | divisionId=5 返回 2 条 standing |
| 数据榜单 Tab | ✅ | team-stat=2、player-stat=1 |
| 排行榜 Tab | ✅ | rankType=PLAYER_SCORE 返回 1 条（修复前 POINTS 为空） |

### 19.4 Division 切换结果 — **通过**

- seasonId=2：唯一 Division `3`（PC），切换无异常
- seasonId=4：Division `5`（PC），赛程/积分榜/统计均按 divisionId 隔离返回

### 19.5 参赛队伍 / 阵容结果 — **通过**

- `GET /team/list?divisionId=3` → 1 条 EventTeam
- `GET /team/13` → 5 条 EventPlayer（含 captain/starter 字段）
- SafeImage：Logo/avatar 为空时走占位（组件已实现）

### 19.6 赛程 / 比分结果 — **通过**

- `GET /match/list?divisionId=5` → 1 场 FINISHED
- 比分 88-75 与 `basket_match` 一致
- divisionId=3 无赛程 → 前端空状态（不报错）

### 19.7 积分榜 / 统计 / 排行榜结果 — **通过**

| API | divisionId=5 条数 |
|-----|-------------------|
| standing | 2 |
| team-stat | 2 |
| player-stat | 1 |
| rank (PLAYER_SCORE) | 1 |

### 19.8 `/events/match/:matchId` 结果 — **通过**

**测试 URL：** `/events/match/1`

| 检查项 | 结果 |
|--------|------|
| 页面 HTTP 200 | ✅ |
| 双方队伍 | ✅ homeTeam/awayTeam 有值 |
| 比分 88-75 | ✅ |
| 单场球队数据 | ✅ teamStats=2 |
| 单场球员数据 | ✅ playerStats=1 |
| 无数据占位 | ✅ 组件层「暂无数据」已实现 |

### 19.9 `/data` 结果 — **通过**

| 检查项 | 结果 |
|--------|------|
| 页面 HTTP 200 | ✅ |
| V4 Season 下拉 | ✅ season/list 2 条 |
| Division 切换 | ✅ 来自 season.divisions |
| 积分榜/球队/球员/排行榜 | ✅ 同 stats API，rankType 已修正 |
| 旧 public tournament 主逻辑 | ✅ 未调用 |

### 19.10 是否发现旧 public API 调用 — **主路径未发现**

- V4 页面（`views/events/v4/*`、`views/data/V4DataPage.vue`）仅调用 `/basket/v4/public/*`
- 冒烟脚本验证 `/basket/public/tournament/page` 仍存在（legacy 兼容），**V4 主路径未使用**
- Header 非 legacy 路由时使用 V4 Series API

### 19.11 修复了哪些问题

| # | 问题 | 修复 |
|---|------|------|
| 1 | 运行中 web 服务无 R6 Controller（500 NoResourceFoundException） | 重启 `basketball-web` 加载新代码 |
| 2 | `V4PublicEventTeamDetailResp` 编译失败 | 改为 extends `V4PublicEventTeamItemResp` |
| 3 | 排行榜 rankType 前端用 POINTS/ASSIST/REBOUND，后端为 PLAYER_SCORE/PLAYER_ASSIST/PLAYER_REBOUND | 修正 `v4Display.ts`、`V4SeasonDetailPage`、`V4DataPage` |
| 4 | 本地 dev 无 `VITE_API_BASE_URL`，API 无法走 proxy | 新增 `.env.development`：`VITE_API_BASE_URL=/web-api` |
| 5 | R5 Match 所属 Season4 原為 DRAFT，公开 API 拒绝 | 冒烟验收时将 seasonId=4 补发布为 PUBLISHED（测试数据对齐） |

### 19.12 是否可以进入第 7 轮 — **建议可以**

R6 主路径 API + 页面真实冒烟 **16/16 PASS**。建议 R7 继续：个人中心 V4 化、清理 legacy 路由、公开球队页对接 EventTeam。

---

**本轮状态：R6 已完成（代码交付 + 真实冒烟补验收）**
