# CNPA 球员游戏 ID 展示优化报告

> 生成时间：2026-08-01  
> 定位：仅展示层优化；不改库表、不改认证/报名/队长/球队/赛事核心业务逻辑

---

## 1. 本轮目标

在 Admin / Web 关键列表与选择器中补充「平台游戏 ID」展示，帮助审核人员、队长、管理员识别球员在游戏中的身份。

统一口径：游戏平台 / 游戏 ID 是**游戏身份资料**，不是「球员归属某平台」。

---

## 2. 修改范围

| 层级 | 改动类型 | 说明 |
|------|----------|------|
| Admin 后端 | VO + 只读查询 + Service 填充 | 用户列表、可授权队长列表补充 `platformGameIds` / `gameIdSummary` |
| Web 后端 | Resp + 只读查询 + Service 填充 | 球队成员列表补充游戏 ID 摘要；阵容空值展示统一 |
| Admin UI | 列表列 / 下拉文案 / 说明 Alert | 用户管理、队长授权、球员认证审核 |
| Web UI | 成员列表列 / 阵容列 / 认证说明文案 | 我的球队、查看阵容、球员认证申请 |
| 数据库 | **无** | 无新表、无 migration、无字段变更 |

数据来源：现有 `basket_player_platform_account`（仅 `account_status=ACTIVE`）。

---

## 3. 未修改的业务逻辑说明

本轮**未改动**：

1. 球员认证必填规则、提交接口、审核通过/驳回状态机  
2. 平台游戏 ID 绑定、变更、审核逻辑  
3. 赛事报名校验、阵容校验、EventPlayer 生成逻辑  
4. 队长授权提交字段与授权业务  
5. 球队加入 / 邀请 / 退出 / 踢人逻辑  
6. 表结构与 Entity 入库字段  
7. 比赛数据模块前四轮成果（提交/审核/争议/异常）

展示字段为空时前端兜底为「未绑定 / 未填写 / 未选择 / -」，不影响提交与审核结果。

---

## 4. Admin 用户管理展示效果

页面：`basketball-admin-ui/src/views/cnpa/identity/account-player/index.vue`

| 场景 | 展示 |
|------|------|
| 无绑定 | `未绑定` |
| 单平台 | `PC：player111128` |
| 多平台（≤2） | `PC：xxx / PS：yyy` |
| 多平台（>2，表格紧凑） | `已绑定 N 个：PC / PS / Xbox`，悬浮 tooltip 显示完整 `gameIdSummary` |

列名：**绑定游戏ID**。空白占位列问题通过真实数据列替代解决。

后端：`AccountPlayerListVo` + `BasketAccountPlayerServiceImpl.attachPlatformGameIds`，批量 `selectActiveByPlayerIds`。

---

## 5. Admin 队长授权弹窗展示效果

页面：`basketball-admin-ui/src/views/cnpa/identity/captain-qualification/index.vue`

下拉选项文案示例：

- `CNPA用户111116（CNPA-P-000033）｜PC：player111116`
- 多平台：`…｜PC：player_pc / PS：player_ps`
- 未绑定：`…｜未绑定游戏ID`

选择值仍为原 `playerId` / 相关 ID，**提交字段与授权逻辑不变**。  
可授权球员表格同步增加「绑定游戏ID」列。

后端：`EligibleCaptainVo` 在 `selectEligibleList` 中批量补游戏 ID 展示字段。

---

## 6. Admin 球员认证审核页面处理结果

页面：`basketball-admin-ui/src/views/cnpa/identity/player-certification/index.vue`

1. **保留**「游戏平台」「游戏ID」「位置」列  
2. 空值：平台 →「未选择」；游戏 ID →「未填写」  
3. 顶部增加说明：  
   > 游戏平台和游戏ID用于确认球员在游戏中的身份，不代表球员只能参加该平台赛事。  
4. **未改**审核通过/驳回逻辑与表单必填规则

---

## 7. Web 球员认证页面处理结果

页面：`basketball-web-ui/src/views/account/player-identity/apply.vue`（及身份页相关说明）

1. **保留**游戏平台、游戏 ID、常用位置、账号证明字段  
2. 说明文案调整为：  
   > 请填写你当前常用的平台游戏ID，方便赛事方确认你在游戏中的身份。认证通过后会生成永久 CNPA 球员ID，后续可继续补充或变更其他平台游戏ID。  
3. **未改**提交接口、必填规则、状态流转、上传逻辑

---

## 8. Web 球队成员列表展示效果

组件：`basketball-web-ui/src/components/account/team/TeamMemberList.vue`

| 场景 | 展示 |
|------|------|
| 无绑定 | `未绑定` |
| 单/多平台 | `gameIdSummary`（如 `PC：xxx / PS：yyy`） |

长期球队成员列表展示**全部有效平台游戏 ID**（无赛事平台上下文）。  
后端：`TeamMemberItemResp.platformGameIds` / `gameIdSummary`，在 `TeamMemberBusinessServiceImpl` 批量填充。

---

## 9. Web 查看阵容页面展示效果

组件：`basketball-web-ui/src/components/account/tournament/RosterTable.vue`

1. 优先展示阵容快照 `platformSnapshot` + `gameIdDisplaySnapshot`  
2. 快照为空时显示「未绑定」/「-」，**不回落**当前最新平台游戏 ID（避免覆盖历史）  
3. 有值时展示为 `平台：游戏ID`

后端注释说明见 `EventParticipationQueryServiceImpl.toRosterMemberResp`。

---

## 10. 涉及接口和 VO 修改

### Admin

| 文件 | 变更 |
|------|------|
| `PlatformGameIdItemVo`（新增） | `{ platformCode, gameId }` |
| `PlatformGameIdDisplaySupport`（新增） | summary / compactSummary |
| `AccountPlayerListVo` | `platformGameIds`、`gameIdSummary`、`gameIdCompactSummary` |
| `EligibleCaptainVo` | 同上展示字段 |
| `BasketPlayerPlatformAccountMapper` + XML | 只读 `selectActiveByPlayerIds` |
| `BasketAccountPlayerServiceImpl` | `attachPlatformGameIds` |
| `BasketCaptainQualificationServiceImpl` | eligible 列表填充游戏 ID |

接口路径复用原用户列表 / 可授权球员查询，仅扩展响应字段。

### Web

| 文件 | 变更 |
|------|------|
| `PlatformGameIdDisplaySupport`（新增） | 与 Admin 同口径摘要 |
| `PlatformGameIdItemResp` / `TeamMemberItemResp` | `platformGameIds`、`gameIdSummary` |
| `TeamMemberBusinessServiceImpl` | 成员列表填充摘要 |
| `EventParticipationQueryServiceImpl` | 阵容空快照展示「未绑定」 |

---

## 11. 涉及页面文件

### Admin UI

- `src/views/cnpa/identity/account-player/index.vue`
- `src/views/cnpa/identity/captain-qualification/index.vue`
- `src/views/cnpa/identity/player-certification/index.vue`
- `src/types/api/basket/index.ts`

### Web UI

- `src/components/account/team/TeamMemberList.vue`
- `src/components/account/tournament/RosterTable.vue`
- `src/views/account/player-identity/apply.vue`
- `src/api/types/teamMember.ts`
- （及相关 participation mapper 空值兜底）

---

## 12. 测试结果

| # | 测试项 | 结果 | 说明 |
|---|--------|------|------|
| 1 | Admin 用户管理显示游戏 ID 摘要 | 代码已接 | 依赖列表接口新字段 |
| 2 | 无绑定显示「未绑定」 | 代码已接 | `PlatformGameIdDisplaySupport.UNBOUND` |
| 3 | 单平台显示平台+游戏 ID | 代码已接 | |
| 4 | 多平台显示多个 / 紧凑摘要 | 代码已接 | >2 用 compact + tooltip |
| 5 | 队长授权下拉显示游戏 ID | 代码已接 | 文案拼接，值不变 |
| 6 | 授权提交字段不变 | 通过（静态） | 未改 submit payload |
| 7 | 球员认证审核列保留 | 通过 | 仅空值与说明优化 |
| 8 | Web 认证申请可打开 | 构建通过 | 文案已更新 |
| 9 | Web 球队成员列表显示游戏 ID | 代码已接 | |
| 10 | Web 查看阵容显示赛事平台游戏 ID | 代码已接 | 优先快照 |
| 11 | 无游戏 ID 不报错 | 代码已接 | 前端兜底 |
| 12–14 | 构建 / 编译 | 见下节 | |

说明：本环境未启动完整 Admin/Web 服务做 HTTP 联调；P0 展示逻辑已静态校验并通过前后端构建。

---

## 13. 构建结果

| 工程 | 命令 | 结果 |
|------|------|------|
| basketball-web-ui | `npm run build` | ✅ 通过 |
| basketball-admin-ui | `npm run build:prod` | ✅ 通过 |
| basketball-web | `mvn compile -DskipTests` | ✅ 通过 |
| basketball-admin | `mvn compile -DskipTests` | ✅ 通过 |

---

## 14. 遗留问题

1. **P1**：比赛数据提交页球员列表尚未统一加游戏 ID 列（当前无对应 UI 列；可后续补）。  
2. **P2**：更多详情弹窗、列宽与 tooltip 可再精修。  
3. **联调**：需在已部署环境用真实多平台账号目视验收用户管理 / 队长下拉 / 成员列表 / 阵容。  
4. 邀请成员选择器、加入申请审核页等 P2 页面未强制加列（本轮优先 P0）。

---

## 15. 下一步建议

1. 部署后按测试清单做一次目视验收（单平台 / 多平台 / 未绑定三类账号）。  
2. 视需要为比赛数据提交球员行补充 `gameId` 只读展示（P1）。  
3. 保持「快照优先」原则，任何阵容历史展示禁止用最新游戏 ID 覆盖。  
4. 后续若产品需要「按赛事平台筛选成员 ID」，仅在有明确 `platformCode` 上下文的页面做过滤展示，仍不改绑定规则。

---

## 最终结论（必答）

| # | 问题 | 答案 |
|---|------|------|
| 1 | 是否删除了球员认证中的游戏平台/游戏ID字段？ | **否** |
| 2 | 是否改变了球员认证业务逻辑？ | **否** |
| 3 | 是否改变了平台游戏ID绑定规则？ | **否** |
| 4 | 是否改变了赛事报名校验？ | **否** |
| 5 | 是否在 Admin 用户管理展示绑定游戏ID？ | **是** |
| 6 | 是否在队长授权选择球员时展示游戏ID？ | **是** |
| 7 | 是否在 Web 球队成员列表展示游戏ID？ | **是** |
| 8 | 是否在 Web 查看阵容展示游戏ID？ | **是** |
| 9 | 是否构建通过？ | **是** |

**本轮结论：展示层优化已完成；业务逻辑与数据库结构未改动；前后端构建均通过。**
