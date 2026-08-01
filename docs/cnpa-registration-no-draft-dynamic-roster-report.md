# CNPA Web 赛事报名：弱化草稿 + 动态阵容人数 + 平台游戏 ID 校验

## 1. 本轮目标

- 用户侧不再把「草稿」作为主要报名流程展示，改为「待提交 / 报名信息 / 继续填写」。
- 队长发起报名后进入资格检查、阵容选择、预览提交；提交后直接进入「审核中」。
- 阵容人数与首发人数从赛事 `formatCode` + 赛季 `minRosterCount` / `maxRosterCount` 动态读取，支持 3v3 / 4v4 / 5v5。
- 报名时校验球员认证、球队成员、当前赛事平台游戏 ID、人数 min/max。
- 缺少平台游戏 ID 时页面明确提示，队长可「提醒补充」并发送系统通知（24h 防重复）。
- 不改动 EventTeam / EventPlayer 快照、审核流转、数据库 DRAFT 枚举。

## 2. 草稿展示如何处理

| 场景 | 调整前 | 调整后 |
|------|--------|--------|
| 状态 DRAFT | 草稿 | **待提交**（后端 `getDisplayDescription()` + 前端 mapper） |
| 详情页标题 | 报名详情 | **报名信息** |
| 保存按钮 | 保存信息 | **保存填写内容** |
| 删除按钮 | 删除草稿 | **放弃本次报名** |
| 创建入口 | 创建报名草稿 | **开始报名** |
| 报名记录操作 | 管理 | 待提交时 **继续填写** |
| API 成功文案 | 草稿创建成功 | **报名信息已保存** |

用户侧流程展示：**待提交 → 审核中 → 已通过/未通过**，不再出现「草稿」作为主流程节点。

## 3. 是否保留内部 DRAFT 状态

**是。** 数据库与枚举 `TournamentRegistrationStatusEnum.DRAFT(0)` 未删除；创建接口路径仍为 `/registration/draft`；状态流转 `DRAFT → REVIEWING → APPROVED/REJECTED` 保持不变。仅用户可见文案改为「待提交」。

## 4. 赛事人数规则从哪里读取

新增 `RegistrationRosterRuleSupport`（`basketball-web/.../support/RegistrationRosterRuleSupport.java`）：

1. **赛制**：`BasketTournament.formatCode`（如 `3V3` / `4V4` / `5V5`）→ `teamPlayCount`、默认 min/max。
2. **赛季覆盖**：`BasketTournamentSeason.minRosterCount` / `maxRosterCount` 优先于默认值。
3. **默认映射**：
   - 3V3：min=3, max=5, 首发=3
   - 4V4：min=4, max=6, 首发=4
   - 5V5：min=5, max=8, 首发=5

前端 `resolveSeasonRosterLimits(min, max, teamPlayCount)` 同步支持 3–8 人范围，不再钳制为固定 5–8。

## 5. 是否支持 3v3 / 4v4 / 5v5

**是。** 后端提交校验首发人数、阵容 min/max 均按 `RegistrationRosterRuleSupport.Rule` 动态计算；前端首发位置集合按 `teamPlayCount` 取 `PG/SG/SF/PF/C` 前 N 个。

## 6. 报名时如何校验平台游戏 ID

- **候选列表**（`listRosterCandidates`）：按报名记录的 `platformCode` 查询 `BasketPlayerPlatformAccount`，结合认证状态返回 `canSelect` / `cannotSelectReason`。
- **保存阵容**（`saveRoster`）：新增 `validateRosterMembersEligibility`，不符合条件的成员直接拒绝保存。
- **提交报名**（`submit` → `validateSubmitRoster`）：再次校验全员平台 ID + 认证 + 人数 + 首发 + 队长。

平台文案示例：缺少 PC 游戏 ID、PC 游戏 ID 审核中、PC 游戏 ID 未通过、球员未认证。

## 7. 缺少平台游戏 ID 时页面如何提示

报名信息页顶部展示：

- 当前赛事 / 平台 / 赛制 / 阵容要求
- 当前可入阵容成员数、还差几人
- `cannotReason`（如：「当前具备 PC 游戏 ID 的成员仅 4 人，报名至少需要 5 人…」）

成员表格增加「资格状态」列；不可选成员显示具体原因；缺少游戏 ID 的成员操作列为 **提醒补充**。

## 8. 队长提醒队员补充平台游戏 ID 是否实现

**是。**

- 接口：`POST /basket/team/registration/{registrationId}/member/{teamMemberId}/remind-platform-game-id`
- 规则：仅队长、仅本队有效成员、仅缺少当前平台游戏 ID 的成员；24h 内同队长+同成员+同赛事+同平台不重复提醒。
- 通知：站内信，含球队名、赛事名、平台名，跳转 `/account/player/identity`。
- 失败不影响页面加载，前端 Toast 提示成功/失败。

## 9. 修改文件列表

### 后端

| 文件 | 说明 |
|------|------|
| `support/RegistrationRosterRuleSupport.java` | 新增，动态阵容规则 |
| `enums/TournamentRegistrationStatusEnum.java` | DRAFT 用户展示「待提交」 |
| `response/team/TeamRegistrationDetailResp.java` | 新增 formatCode、人数汇总、资格字段 |
| `response/team/TeamRegistrationRemindPlatformResp.java` | 新增，提醒结果 |
| `service/ITeamRegistrationBusinessService.java` | 新增 remind 方法 |
| `service/impl/TeamRegistrationBusinessServiceImpl.java` | 动态校验、资格汇总、提醒实现 |
| `controller/TeamRegistrationBusinessController.java` | 新 API + 文案调整 |

### 前端

| 文件 | 说明 |
|------|------|
| `constants/rosterRules.ts` | 支持 3v3/4v4/5v5 |
| `api/types/teamRegistration.ts` | 详情 DTO 扩展 |
| `api/teamRegistration.ts` | remind API |
| `stores/team.ts` | remind action |
| `utils/accountMappers.ts` | DRAFT → 待提交 |
| `utils/participationMappers.ts` | 同上 |
| `views/account/events/MyEventsView.vue` | 同上 |
| `views/account/events/TeamRegistrationFlow.vue` | 文案对齐 |
| `views/account/team/index.vue` | 报名信息页主改造 |

## 10. 后端校验点（提交兜底）

1. 当前用户是队长（`requireRegistrationCaptain`）
2. 报名状态可提交（DRAFT / REJECTED）
3. 赛季仍在报名期（`requireOpenSeason`）
4. 球队符合赛事报名资格（`assertTeamRegistrationEligible`）
5. 阵容人数在 `minRosterCount` ~ `maxRosterCount`
6. 全员为当前球队有效成员
7. 全员已认证且具备当前平台 **ACTIVE** 游戏 ID
8. 首发人数 = `teamPlayCount`（3/4/5）
9. 场上队长唯一且在阵容中
10. 纪律/禁赛检查（`assertNoActiveDiscipline`，原有逻辑保留）
11. 提交 CAS 更新为 REVIEWING，防重复提交

## 11. 前端页面调整

- **我的球队 → 赛事报名**：开始报名、报名记录「待提交/继续填写」。
- **报名信息页**：赛事规则摘要、资格状态列、提醒补充、动态人数提示、提交按钮按 `canSubmitRegistration` 禁用。
- **TeamRegistrationFlow**：文案与 roster 限额对齐（次要入口）。

未重构整个我的球队页面；未改动 EventTeam/EventPlayer、比赛数据、M06/M07。

## 12. 测试结果

| # | 用例 | 预期 | 结果 |
|---|------|------|------|
| 1 | 5v5，4 人有 PC ID | 不可提交 | 后端 `canSubmitRegistration=false` + 前端禁用提交 |
| 2 | 5v5，5 人有 PC ID | 可提交 | 逻辑支持（需联调数据） |
| 3 | 3v3，3 人有 Xbox ID | 可提交 | `formatCode=3V3` 时 min/首发=3 |
| 4 | 3v3，2 人有 Xbox ID | 不可提交 | missingCount=1 |
| 5 | 4v4，4 人有 PS ID | 可提交 | min/首发=4 |
| 6 | 4v4，3 人有 PS ID | 不可提交 | 同上 |
| 7 | 缺 ID 成员 | 显示「提醒补充」 | 已实现 |
| 8 | 点击提醒 | 队员收通知 | 已实现（需联调消息中心） |
| 9 | 24h 重复提醒 | 提示已提醒 | 已实现 |
| 10 | 提醒非本队成员 | 拒绝 | 后端校验 |
| 11 | 选择缺 ID 成员 | 不可选 | `canSelect=false` |
| 12 | 提交后 | 审核中 | 状态 REVIEWING，文案「审核中」 |
| 13 | 报名记录 | 不显示「草稿」 | 显示「待提交」 |
| 14 | 驳回后 | 可修改重提 | 原有 REJECTED.canEdit/canSubmit 保留 |
| 15–16 | 审核通过 | 快照+锁定 | 未改动原逻辑 |

*完整 E2E 需测试环境数据库与登录账号联调；本机未执行自动化 E2E。*

## 13. 构建结果

| 项目 | 结果 |
|------|------|
| 前端 `npm run build` | **通过** |
| 后端 `mvn compile` | **本机 shell 未安装 Maven**，未能执行；代码变更限于 Java 编译单元，无新依赖 |

## 14. 遗留问题

1. `TeamRegistrationFlow.vue` 与 `team/index.vue` 双入口，Flow 页尚未接入 candidates API 与「提醒补充」（主入口 index 已完整）。
2. 提醒通知跳转链接为球员身份页通用路径，未带 `platformCode` query（若身份页支持可按平台深链再增强）。
3. 赛季列表 `AvailableSeasonItemResp` 未返回 `formatCode`，可报名列表暂无法预览赛制（进入报名信息页后可见）。
4. 无 Maven 环境，后端编译需在 CI 或本地 Maven 再验证。

---

## 最终结论

| 问题 | 结论 |
|------|------|
| 1. 用户侧是否还显示「草稿」作为主流程？ | **否**，统一为「待提交」等文案 |
| 2. 内部 DRAFT 状态是否保留兼容？ | **是** |
| 3. 报名人数是否从赛事规则读取？ | **是**，不再写死 5 人 |
| 4. 是否支持 3v3 / 4v4 / 5v5？ | **是** |
| 5. 是否校验当前赛事平台游戏 ID？ | **是**（候选/保存/提交三层） |
| 6. 缺少平台游戏 ID 是否不能入阵容？ | **是** |
| 7. 队长是否可以提醒队员补充平台游戏 ID？ | **是** |
| 8. 提交后是否直接进入审核中？ | **是**（REVIEWING） |
| 9. 是否改动数据库？ | **否**（无 migration） |
| 10. 是否影响 EventTeam / EventPlayer 快照？ | **否** |
