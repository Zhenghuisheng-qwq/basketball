# CNPA 赛事域 V4 第 4 轮：赛程生成 + Match

> 轮次：R4 Schedule & Match  
> 日期：2026-08-04  
> 前置：R1 模型落地、R1 DDL、R1.5 旧域隔离、R2 赛事发布、R3 队长授权/报名/审核/快照

---

## 1. 本轮完成内容

完成 V4 赛事域从 **Division EventTeam 快照** 到 **ScheduleBatch 草稿赛程** 再到 **Match 发布** 的主链路：

```
Division (REGISTRATION_CLOSED)
  → EventTeam (ACTIVE)
  → ScheduleBatch (DRAFT → PUBLISHED)
  → Match (SCHEDULED, publish DRAFT → PUBLISHED)
  → Division (SCHEDULED)
```

**已实现：**

- 4 张旧 match 同名表 backup / cleanup / V4 重建
- V4 单循环赛程算法（REGULAR_ROUND_ROBIN_V1）
- 硬编码时间槽分配（周二/四/六 21:00/22:00，每槽最多 6 场）
- 后台 ScheduleBatch API（列表/生成/发布/作废/详情）
- 后台 Match API（列表/详情/编辑草稿/删除草稿）
- 后台 V4 赛程管理 + V4 比赛列表页面
- 菜单 SQL + 旧赛程/比赛入口隐藏/废弃提示
- 冒烟脚本 `scripts/smoke_v4_r4_schedule_match.py`

**未做（按本轮边界）：** 比赛数据提交、审核、统计结算、排行榜、Open/PERSONAL、自动排赛 Job、前台展示。

---

## 2. 4 张旧 match 同名表 cleanup / 重建记录

| 步骤 | 说明 | 结果 |
|------|------|------|
| 备份 | `bak_r4_basket_match` 等 4 张表 | match=160, period_score=24, team_stat=12, player_stat=60 |
| 确认 | 旧结构含 stage_id/home_team_id/tinyint match_status，属旧赛事域 | 已确认 |
| DROP | 按 `cnpa-v4-r1-cleanup-draft.sql` 仅处理 4 张同名表 | 已执行 |
| 重建 | 执行 `docs/sql/cnpa-v4-round4-match-rebuild.sql` | 4 表 V4 结构已创建 |
| 验证 | basket_match 含 division_id/season_id/schedule_batch_id/home_event_team_id/away_event_team_id/match_status/publish_status | 通过 |

执行脚本：`scripts/run_r4_match_rebuild.py`

---

## 3. 新增 / 修改文件清单

### 3.1 后端 Admin

| 类型 | 路径 |
|------|------|
| 枚举 | `enums/v4/GenerateStatusEnum.java` |
| Query | `query/v4/BasketScheduleBatchQuery.java`, `BasketV4MatchQuery.java` |
| DTO | `dto/v4/BasketScheduleBatchGenerateReq.java`, `BasketV4MatchUpdateReq.java` |
| VO | `vo/v4/BasketScheduleBatchListVo.java`, `BasketScheduleBatchDetailVo.java`, `BasketV4MatchListVo.java` |
| Support | `support/v4/BasketV4RoundRobinScheduler.java` |
| Service | `service/v4/IBasketV4ScheduleBatchService.java`, `IBasketV4MatchService.java` |
| ServiceImpl | `service/impl/v4/BasketV4ScheduleBatchServiceImpl.java`, `BasketV4MatchServiceImpl.java` |
| Controller | `web/controller/basket/v4/BasketV4ScheduleBatchController.java`, `BasketV4MatchController.java` |
| 权限 | `constant/BasketEventV4Perms.java`（扩展） |
| Mapper | `BasketScheduleBatchMapper.*`, `BasketV4MatchMapper.*`, `BasketEventTeamMapper.*`（扩展 selectActiveByDivisionId 等） |

### 3.2 前端 Admin UI

| 类型 | 路径 |
|------|------|
| API | `src/api/cnpa/event-v4/schedule-batch.ts` |
| 页面 | `src/views/cnpa/event-v4/schedule/index.vue`, `match/index.vue` |
| 旧页废弃提示 | `cnpa/competition/schedule/index.vue`, `schedule-manage/index.vue`, `lineup/index.vue`, `match-data-entry/index.vue` |

### 3.3 SQL / 脚本 / 文档

| 文件 | 说明 |
|------|------|
| `docs/sql/cnpa-v4-round4-match-rebuild.sql` | 4 表 backup + drop + 重建 |
| `docs/sql/cnpa-v4-round4-menu.sql` | V4 R4 菜单与权限 |
| `scripts/run_r4_match_rebuild.py` | 表重建执行器 |
| `scripts/run_r4_menu.py` | 菜单 SQL 执行器 |
| `scripts/smoke_v4_r4_schedule_match.py` | API 冒烟 |

---

## 4. 新增后端接口清单

### 4.1 ScheduleBatch

| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| GET | `/basket/v4/schedule-batch/list` | `basket:v4ScheduleBatch:list` | 分页查询 |
| GET | `/basket/v4/schedule-batch/{id}` | `basket:v4ScheduleBatch:query` | 批次详情 + Match 列表 |
| POST | `/basket/v4/schedule-batch/generate` | `basket:v4ScheduleBatch:generate` | 生成草稿赛程 |
| PUT | `/basket/v4/schedule-batch/{id}/publish` | `basket:v4ScheduleBatch:publish` | 发布赛程 |
| PUT | `/basket/v4/schedule-batch/{id}/cancel` | `basket:v4ScheduleBatch:cancel` | 作废草稿 |

### 4.2 Match

| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| GET | `/basket/v4/match/list` | `basket:v4Match:list` | 分页查询 |
| GET | `/basket/v4/match/{id}` | `basket:v4Match:query` | 详情 |
| PUT | `/basket/v4/match/{id}` | `basket:v4Match:edit` | 编辑草稿（时间/场地/备注） |
| DELETE | `/basket/v4/match/{id}` | `basket:v4Match:remove` | 删除草稿 |

---

## 5. 新增后台页面 / API 清单

| 菜单 | 路径 | 组件 |
|------|------|------|
| V4赛程管理 | `event-v4-schedule` | `cnpa/event-v4/schedule/index.vue` |
| V4比赛列表 | `event-v4-match` | `cnpa/event-v4/match/index.vue` |

前端 API：`src/api/cnpa/event-v4/schedule-batch.ts`

---

## 6. 单循环赛程算法说明

实现类：`BasketV4RoundRobinScheduler`

1. 输入：Division 下 ACTIVE EventTeam ID 列表（按 ID 排序）
2. 奇数队：追加 BYE（null），轮空轮不生成 Match
3. 圈算法：固定首位，其余轮转，每轮 n/2 对阵
4. 主客交替：偶数轮 home=A/away=B，奇数轮互换
5. 场数：`N * (N-1) / 2`（N 为真实球队数，不含 BYE）
6. 校验：同轮每队最多 1 场；主客不同；对阵组合不重复

---

## 7. 时间槽分配规则说明

| 规则 | 值 |
|------|-----|
| 允许星期 | 周二、周四、周六 |
| 允许时刻 | 21:00、22:00 |
| 每槽上限 | 6 场 |
| 起始时间 | Division.matchStartTime（空则当前时间） |
| 分配策略 | 从起始日起扫描合法槽，槽满则顺延下一槽 |

---

## 8. ScheduleBatch 状态流转

```
(无) --generate--> DRAFT/SUCCESS
DRAFT --publish--> PUBLISHED
DRAFT --cancel--> (逻辑删除)
PUBLISHED --generate--> 拒绝（不可重复生成）
```

字段：

- `generateStatus`: SUCCESS（生成成功）
- `publishStatus`: DRAFT → PUBLISHED

---

## 9. Match 状态流转

```
生成时: matchStatus=SCHEDULED, publishStatus=DRAFT
发布时: publishStatus=DRAFT → PUBLISHED（批次级批量更新）
编辑: 仅 DRAFT 可改 matchTime/venueText/remark
删除: 仅 DRAFT 可逻辑删除
```

本轮不做 FINISHED/PLAYING 自动流转。

---

## 10. 发布赛程流程说明

1. 校验批次 publishStatus=DRAFT 且下属 Match > 0
2. 批次 publishStatus → PUBLISHED，记录 publishTime/publishedBy
3. 批次下所有 DRAFT Match → PUBLISHED
4. Division.divisionStatus → SCHEDULED

---

## 11. 旧入口隐藏 / 废弃处理结果

| 入口 | 处理 |
|------|------|
| 旧赛程管理 schedule/index | visible=1 + 页面 warning |
| 旧赛程管理 schedule-manage | visible=1 + 页面 warning |
| 旧比赛名单 lineup | visible=1 + 页面 warning |
| 旧比分录入 match-data-entry | visible=1 + 页面 warning |
| 旧比赛管理/审核/异常等 | sys_menu visible=1 + remark [V4 R4 deprecated] |

菜单 SQL：`docs/sql/cnpa-v4-round4-menu.sql`

---

## 12. 旧 Job 停用 / 未使用确认

| Job | 状态 |
|-----|------|
| ScheduleAutoGenerateJob | 代码 @Deprecated；sys_job 无配置记录 |
| MatchStatusAutoFinishJob | 代码 @Deprecated；sys_job 无配置记录 |
| V4 新 Job | 未新增 |

V4 新代码未引用上述 Job 或旧 Schedule/Match Service。

---

## 13. 新旧代码隔离检查结果

对 `service/impl/v4/BasketV4Schedule*`、`BasketV4Match*`、`controller/basket/v4/*`、`support/v4/*` 检索：

| 禁止项 | 结果 |
|--------|------|
| eventId / tournamentId / competitionSeasonId | 未发现 |
| BasketTournament / BasketScheduleGenerate | 未发现 |
| ScheduleAutoGenerateJob / MatchStatusAutoFinishJob | 未发现 |
| BasketMatchData / 旧 schedule_rule 表 | 未发现 |

V4 仅使用：`basket_schedule_batch`、`basket_match`、`basket_event_team`、`basket_division`、`basket_season` 及 mapper.v4 / service.v4 / enums.v4。

---

## 14. API 冒烟测试记录

脚本：`scripts/smoke_v4_r4_schedule_match.py`  
环境：Division ID=5（R3_QUOTA_TEST_PC，2 支 ACTIVE EventTeam）

| 用例 | 结果 |
|------|------|
| V4 basket_match 字段验证 | PASS |
| 4 张备份表存在 | PASS |
| 无效 Division 拒绝生成 | PASS |
| 生成草稿 ScheduleBatch | PASS（batchId=1） |
| 2 队 → 1 场 (N*(N-1)/2) | PASS |
| 时间槽 21:00 | PASS |
| 发布赛程 | PASS |
| Division → SCHEDULED | PASS |
| Match publishStatus=PUBLISHED | PASS（1/1） |
| Match 列表查询 | PASS |
| PUBLISHED 后拒绝重复生成 | PASS |

**Match 生成数量：** 1（2 支球队单循环）

---

## 15. 风险与待确认项

1. **生产清库**：4 表 backup 在目标库执行前需再次确认；本环境旧 match 数据已备份至 bak_r4_*。
2. **Division 状态门槛**：当前要求 REGISTRATION_CLOSED 或 SCHEDULED；REGISTRATION_OPEN 不可生成，需在业务上先 close registration。
3. **matchStartTime 为空**：默认从当前时间找槽；建议发布 Division 时填写 matchStartTime。
4. **已发布不可作废**：需后续轮次设计「赛程变更/追加」流程。
5. **旧 basket_match_data_* 等表**：本轮未清理，仍存于库中但不写入；后续轮次按需处理。
6. **前台展示**：本轮仅后台管理；Web 端赛程展示待后续轮次。

---

## 16. 是否建议进入第 5 轮

**建议进入第 5 轮（比赛数据提交）**，前提：

- 本轮冒烟在生产/预发环境复现
- 至少一个 Division 完成「报名截止 → 生成 → 发布」全链路
- 确认旧 Job 在目标环境 sys_job 均为停用

本轮 **不进入第 5 轮实现**。

---

## 附录：验收标准对照

| # | 标准 | 状态 |
|---|------|------|
| 1-3 | 4 表 backup/cleanup/重建 | ✅ |
| 4 | basket_match 含 division_id | ✅ |
| 5-6 | EventTeam 查询 / <2 拒绝 | ✅ |
| 7-12 | 生成 DRAFT / 场数 / BYE / 不重复 / 不跨 Division | ✅ |
| 13-14 | 时间槽规则 | ✅ |
| 15-19 | 查询/发布/状态变更 | ✅ |
| 20-22 | PUBLISHED 不可重复/DRAFT 可作废/不可删 | ✅ |
| 23-25 | 新旧隔离 / 旧入口废弃 | ✅ |
| 26-27 | 不做数据提交/不进入 R5 | ✅ |
