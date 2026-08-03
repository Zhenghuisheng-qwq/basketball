# CNPA 赛事域 V4 第 2 轮：后台赛事发布流程 — 完成报告

> 分支：basketball-20260803（当前工作分支）  
> 日期：2026-08-04  
> 范围：Series → Season → Division 后台发布主链路（不含报名/赛程/比赛数据）

---

## 1. 本轮完成内容

### 1.1 前置检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| V4 DDL 是否已执行 | **未执行（dev 库）** | `192.168.31.47/basketball` 库中 `basket_series` / `basket_season` / `basket_division` 表均不存在 |
| V4 Seed 是否已执行 | **未执行（dev 库）** | 同上，无种子数据 |
| basket_series 四条种子 | **无法验证** | 表不存在 |
| 示例 League 2026 S1 / PS / Xbox Division | **无法验证** | 表不存在 |
| sys_job 旧任务 | **未查到启用记录** | `scheduleAutoGenerateJob` / `matchStatusAutoFinishJob` 查询结果为空 |

**人工待办：**

1. 在目标环境执行第 1 轮 V4 DDL + Seed（及第 1 轮 DDL 小补丁）。
2. 执行 `docs/sql/cnpa-v4-round2-menu.sql` 注册 V4 菜单并隐藏旧入口。
3. 若生产/测试库存在旧 Job 且 `status='0'`（正常），参考下方停用草案（**执行前以实际 sys_job 口径为准**）：

```sql
-- 若依通常 0=正常，1=暂停；执行前必须确认当前项目口径
UPDATE sys_job
SET status = '1'
WHERE invoke_target LIKE '%scheduleAutoGenerateJob%'
   OR invoke_target LIKE '%matchStatusAutoFinishJob%';
```

### 1.2 后端

- 补全 `BasketV4SeasonServiceImpl`、`BasketV4DivisionServiceImpl`（Series 已有实现）。
- 新增 V4 Controller：`BasketV4SeriesController`、`BasketV4SeasonController`、`BasketV4DivisionController`。
- 新增权限常量 `BasketEventV4Perms`。
- Mapper XML / 接口 / DTO / VO / Query 沿用第 1 轮 + 本轮脚本生成部分，并补充 `id` 查询条件。

### 1.3 前端

- 新增 V4 赛事发布页面：`src/views/cnpa/event-v4/publish/index.vue`
- 新增 API：`src/api/cnpa/event-v4/{series,season,division}.ts`
- 旧发布页面顶部增加废弃提示（list / season / rule / stage / group）

### 1.4 旧入口处理

- SQL 脚本将旧菜单 `visible='1'`（隐藏）：赛事列表、赛季管理、赛事规则、赛事阶段、赛事分组。
- 旧 Vue 页面保留，顶部加 `【已废弃】` Alert，不破坏编译。

---

## 2. 新增 / 修改文件清单

### 2.1 后端（新增）

| 文件 |
|------|
| `basketball-admin/src/main/java/com/zhs/basket/constant/BasketEventV4Perms.java` |
| `basketball-admin/src/main/java/com/zhs/basket/service/impl/v4/BasketV4SeasonServiceImpl.java` |
| `basketball-admin/src/main/java/com/zhs/basket/service/impl/v4/BasketV4DivisionServiceImpl.java` |
| `basketball-admin/src/main/java/com/zhs/web/controller/basket/v4/BasketV4SeriesController.java` |
| `basketball-admin/src/main/java/com/zhs/web/controller/basket/v4/BasketV4SeasonController.java` |
| `basketball-admin/src/main/java/com/zhs/web/controller/basket/v4/BasketV4DivisionController.java` |

### 2.2 后端（本轮已有 / 脚本生成，沿用）

| 类型 | 路径 |
|------|------|
| Service 接口 | `service/v4/IBasketV4{Series,Season,Division}Service.java` |
| Service 实现 | `service/impl/v4/BasketV4SeriesServiceImpl.java` |
| DTO | `dto/v4/Basket{Season,Division}{Create,Update}Req.java`, `BasketSeriesEnabledReq.java` |
| VO | `vo/v4/Basket{Series,SeasonList,SeasonDetail,DivisionList}Vo.java` |
| Query | `query/v4/Basket{Season,Division}Query.java` |
| Mapper | `mapper/v4/Basket{Series,Season,Division}Mapper.java` + XML |

### 2.3 后端（修改）

| 文件 | 变更 |
|------|------|
| `query/v4/BasketSeasonQuery.java` | 增加 `id` 字段 |
| `query/v4/BasketDivisionQuery.java` | 增加 `id` 字段 |
| `mapper/basket/v4/BasketSeasonMapper.xml` | 增加 `id` 条件 |
| `mapper/basket/v4/BasketDivisionMapper.xml` | 增加 `id` 条件 |

### 2.4 前端（新增）

| 文件 |
|------|
| `basketball-admin-ui/src/views/cnpa/event-v4/publish/index.vue` |
| `basketball-admin-ui/src/api/cnpa/event-v4/series.ts` |
| `basketball-admin-ui/src/api/cnpa/event-v4/season.ts` |
| `basketball-admin-ui/src/api/cnpa/event-v4/division.ts` |

### 2.5 前端（修改 — 废弃提示）

| 文件 |
|------|
| `basketball-admin-ui/src/views/cnpa/event/list/index.vue` |
| `basketball-admin-ui/src/views/cnpa/event/season/index.vue` |
| `basketball-admin-ui/src/views/cnpa/event/rule/index.vue` |
| `basketball-admin-ui/src/views/cnpa/event/stage/index.vue` |
| `basketball-admin-ui/src/views/cnpa/event/group/index.vue` |

### 2.6 SQL / 脚本

| 文件 |
|------|
| `docs/sql/cnpa-v4-round2-menu.sql` |
| `scripts/check_v4_r2_prereq.py`（已有） |
| `scripts/generate_v4_r2_backend.py`（已有） |
| `scripts/generate_v4_r2_backend_part2.py`（已有） |

---

## 3. 新增后端接口清单

### 3.1 Series — `/basket/v4/series`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 查询 Series 列表 |
| GET | `/{id}` | 查询 Series 详情 |
| PUT | `/{id}/enabled` | 启用 / 停用 Series |

### 3.2 Season — `/basket/v4/season`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | Season 分页查询 |
| GET | `/{id}` | Season 详情（含 Division 列表） |
| POST | `/` | 创建 Season |
| PUT | `/{id}` | 编辑 Season |
| PUT | `/{id}/publish` | 发布 Season |
| PUT | `/{id}/offline` | 下线 Season |
| DELETE | `/{id}` | 删除 Season（仅 DRAFT 且无 Division） |

### 3.3 Division — `/basket/v4/division`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | Division 分页查询 |
| GET | `/{id}` | Division 详情 |
| POST | `/` | 创建 Division |
| PUT | `/{id}` | 编辑 Division |
| PUT | `/{id}/open-registration` | 开放报名 |
| PUT | `/{id}/close-registration` | 关闭报名 |
| DELETE | `/{id}` | 删除 Division（仅 DRAFT） |

---

## 4. 新增前端页面 / API 清单

| 类型 | 路径 |
|------|------|
| 页面 | `basketball-admin-ui/src/views/cnpa/event-v4/publish/index.vue` |
| API | `basketball-admin-ui/src/api/cnpa/event-v4/series.ts` |
| API | `basketball-admin-ui/src/api/cnpa/event-v4/season.ts` |
| API | `basketball-admin-ui/src/api/cnpa/event-v4/division.ts` |
| 菜单 SQL | `docs/sql/cnpa-v4-round2-menu.sql` → 组件 `cnpa/event-v4/publish/index`，菜单名 **V4赛事发布** |

---

## 5. Season 发布流程说明

```
选择 Series (LEAGUE/ARENA/CUP/OPEN)
  → 创建 Season (DRAFT / NOT_STARTED)
  → 为该 Season 创建 Division (DRAFT，按平台 PC/PS/XBOX)
  → 发布 Season (至少 1 个 Division 且基础信息完整)
       publishStatus: DRAFT → PUBLISHED
       seasonStatus: 按报名时间 → NOT_STARTED / REGISTRATION_OPEN / REGISTRATION_CLOSED
  → （可选）Division 开放报名 (Season 已发布)
  → （可选）Season 下线 → OFFLINE
```

---

## 6. Division 管理流程说明

```
选中已创建 Season
  → 创建 Division（platformCode 唯一，默认 lineup 5-8，时间可继承 Season）
  → 编辑（DRAFT 全量；报名开放后锁定 platform/mode/format/lineup）
  → Season 发布后 → 开放报名 → REGISTRATION_OPEN
  → 关闭报名 → REGISTRATION_CLOSED
  → DRAFT 可逻辑删除
```

---

## 7. 字段校验说明

| 实体 | 规则 |
|------|------|
| Season | seriesId 存在且 enabled=1；year/name/displayTitle 必填；TEAM only；REGULAR_ROUND_ROBIN only；seriesId+year+seasonName 唯一；报名时间/比赛时间前后合法 |
| Division | seasonId 存在；platformCode ∈ PC/PS/XBOX；同 season 平台唯一；maxTeamCount>0；lineup 5-8；TEAM/REGULAR_ROUND_ROBIN only；时间可继承 Season |
| 拒绝 | PERSONAL、非 REGULAR_ROUND_ROBIN 提交时 Service 层直接拒绝 |

---

## 8. 状态流转说明

### Season

| publishStatus | 说明 |
|---------------|------|
| DRAFT | 草稿，可编辑/删除（无 Division） |
| PUBLISHED | 已发布 |
| OFFLINE | 已下架，不删数据 |

| seasonStatus | 触发 |
|--------------|------|
| NOT_STARTED | 创建默认；发布时未到报名开始 |
| REGISTRATION_OPEN | 发布时处于报名时间窗 |
| REGISTRATION_CLOSED | 发布时已过报名结束 |

### Division

| divisionStatus | 说明 |
|----------------|------|
| DRAFT | 默认，可编辑/删除 |
| REGISTRATION_OPEN | 开放报名 |
| REGISTRATION_CLOSED | 关闭报名 |

---

## 9. 旧后台发布入口处理结果

### 已隐藏（SQL，`visible='1'`）

| menu_id | 名称 | 组件 |
|---------|------|------|
| 2004 | 赛事列表 | cnpa/event/list/index |
| 2005 | 赛季管理 | cnpa/event/season/index |
| 2006 | 赛事规则 | cnpa/event/rule/index |
| 2038 | 赛事阶段 | cnpa/event/stage/index |
| 2039 | 赛事分组 | cnpa/event/group/index |

### 仍保留（原因）

- 旧 Vue 文件未删除，避免大规模改动影响编译与历史数据运维。
- 页面顶部已加废弃 Alert，引导至 V4 发布页。
- **建议第 5~6 轮**（旧域完全退场后）再删除页面与 Controller。

### 未改动

- 赛程管理、报名审核、统计等仍依赖旧 tournament/season 的页面 — 本轮不删，后续轮次随业务迁移。

---

## 10. 旧 Job 检查结果

| Job | dev 库查询结果 |
|-----|----------------|
| scheduleAutoGenerateJob | 未查到 |
| matchStatusAutoFinishJob | 未查到 |

- 本轮 **未新增** 自动排赛 Job。
- V4 发布流程 **不触发** 旧 Job。
- 若其他环境存在且启用，请人工执行停用 SQL（见第 1 节）。

---

## 11. 新旧代码隔离检查结果

对 V4 新代码 grep：

```
eventId / tournamentId / competitionSeasonId
BasketTournament / BasketTournamentSeason / BasketCompetitionSeason
IBasketTournamentService / IBasketSeasonService / IBasketScheduleGenerateService
```

**结果：V4 新代码中未发现上述引用。**

- 枚举均使用 `com.zhs.basket.enums.v4.*`
- Service 仅依赖 `mapper.v4` / `domain.v4`
- 接口路径 `/basket/v4/*`，与旧 `/basket/tournament` 等隔离

---

## 12. 是否发现 V4 误引用旧赛事代码

**未发现。**

---

## 13. 编译 / 构建结果

| 模块 | 结果 |
|------|------|
| basketball-admin (mvn compile) | **未执行** — 当前环境 PATH 无 `mvn` |
| basketball-admin-ui (`npm run build:prod`) | **通过** ✓ |
| basketball-admin-ui (`vue-tsc --noEmit`) | 项目存量 TS 问题较多，非本轮引入；生产构建已通过 |

---

## 14. 风险与待确认项

1. ~~**dev 库 V4 表未落地**~~ — 已于 2026-08-04 执行 DDL + Seed（见第 16 节）。
2. ~~**菜单 SQL 未自动执行**~~ — 已执行，menu_id 调整为 2280-2295。
3. **4 张旧同名表仍为旧结构** — `basket_match` 等未执行 cleanup DROP，V4 赛程/比赛轮次前须处理。
4. **Season 发布不自动开放 Division 报名** — Division 需单独调用 `open-registration`（符合本轮范围）。
5. **旧 Job** — dev 库无记录，其他环境需人工复核。

---

## 15. 是否建议进入第 3 轮

**落库与冒烟验收已通过，可以进入第 3 轮准备。**

前提：第 3 轮仍不涉及旧同名表 cleanup；若第 3 轮需写 V4 比赛相关表，须先完成 cleanup + 重建。

---

## 16. 第 2 轮落库与冒烟验收记录

> 验收时间：2026-08-04  
> 目标库：`192.168.31.47/basketball`（dev）  
> 备份文件：`docs/sql/backups/backup-cnpa-v4-r2-smoke-20260804_003533.sql`（605 行 INSERT）

### 16.1 执行了哪些 SQL

| 顺序 | 脚本 | 结果 |
|------|------|------|
| 0 | `scripts/backup_v4_r2_tables.py` | 备份 V4 新表 + sys_menu/sys_role_menu |
| 1 | `docs/sql/cnpa-v4-r1-ddl.sql` | 34 语句全部 OK（18 张新表创建 + 4 张同名旧表 IF NOT EXISTS 跳过） |
| 2 | `docs/sql/cnpa-v4-r1-seed.sql` | Series 4 条 + League 2026 S1 + PS/Xbox Division |
| 3 | `docs/sql/cnpa-v4-round2-menu.sql` | V4 菜单 2280-2295 + 旧入口隐藏 + admin 授权 |

执行方式：`python scripts/apply_v4_r1r2_sql.py`（幂等，可重复执行）。

### 16.2 是否执行 cleanup

**未执行。** 本轮发布链路不依赖旧同名表，遵循「不盲目 DROP」原则。

### 16.3 是否发现旧同名表

| 表 | 存在 | 结构 | 行数 |
|----|------|------|------|
| basket_match | 是 | **旧结构**（无 division_id，有 stage_id） | 160 |
| basket_match_period_score | 是 | 旧结构 | 24 |
| basket_match_team_stat | 是 | 旧结构 | 12 |
| basket_match_player_stat | 是 | 旧结构 | 60 |

`CREATE TABLE IF NOT EXISTS` 未重建上述 4 表。后续赛程/比赛轮次执行 cleanup 草案第 1 节后再跑 DDL。

### 16.4 basket_series 数据记录

| id | series_code | series_name | enabled |
|----|-------------|-------------|---------|
| 1 | LEAGUE | League | 1 |
| 2 | ARENA | Arena | 1 |
| 3 | CUP | Cup | 1 |
| 4 | OPEN | Open | 1 |

### 16.5 Season 创建/发布测试结果

| 用例 | 方式 | 结果 |
|------|------|------|
| Seed 示例 S1 | SQL Seed | DRAFT，2 个 Division |
| 新建 Season | API `POST /basket/v4/season` | id=3 创建成功 |
| 编辑 Season | API `PUT /basket/v4/season/3` | displayTitle 更新成功 |
| 发布 Season | API `PUT .../publish` | publishStatus → PUBLISHED |
| 下线 Season | API `PUT .../offline` | publishStatus → OFFLINE |
| PERSONAL 拒绝 | API POST registrationMode=PERSONAL | code=500，Service 拒绝 |
| 非 REGULAR_ROUND_ROBIN 拒绝 | API POST formatType=KNOCKOUT | code=500，Service 拒绝 |

### 16.6 Division 创建/开放报名/关闭报名测试结果

| 用例 | 方式 | 结果 |
|------|------|------|
| Seed PS / Xbox | SQL Seed | DRAFT |
| 新建 PC Division | API `POST /basket/v4/division` | id=4 创建成功 |
| 同平台重复 | API 再次 POST PC | 拒绝（平台唯一） |
| 开放报名 | API `PUT .../open-registration` | divisionStatus → REGISTRATION_OPEN |
| 关闭报名 | API `PUT .../close-registration` | divisionStatus → REGISTRATION_CLOSED |

### 16.7 菜单 SQL 执行结果

- 主菜单 **V4赛事发布** menu_id=**2280**，component=`cnpa/event-v4/publish/index`，visible=0（显示）
- 按钮权限 2281-2295 已写入，`sys_role_menu` role_id=1 已授权
- **修正**：原脚本 2260-2275 与争议管理按钮冲突，已改为 2280-2295

### 16.8 旧入口隐藏结果

| menu_id | 名称 | visible（1=隐藏） |
|---------|------|-------------------|
| 2004 | 赛事列表 | 1 |
| 2005 | 赛季管理 | 1 |
| 2006 | 赛事规则 | 1 |
| 2038 | 赛事阶段 | 1 |
| 2039 | 分组管理 | 1 |

5 个旧 Vue 页面顶部均有 `【已废弃】` Alert（静态检查通过）。

### 16.9 冒烟发现的问题

1. 菜单 ID 2260-2263 已被「争议管理」占用，原 round2 脚本无法直接执行。
2. 旧同名 4 表存在时 DDL 不会升级结构（预期行为，需文档警示）。

### 16.10 已修复的问题

| 问题 | 修复 |
|------|------|
| 菜单 ID 冲突 | `cnpa-v4-round2-menu.sql` 改用 2280-2295 |
| 缺少一键落库/冒烟脚本 | 新增 `apply_v4_r1r2_sql.py`、`backup_v4_r2_tables.py`、`smoke_v4_r2_admin_publish.py` |
| 部署说明缺失 | 新增 `docs/sql/cnpa-v4-r1r2-deploy.sql` |

本轮冒烟 **未发现** Mapper/Controller/前端 API 路径/字段映射类缺陷。

### 16.11 未解决风险

1. 4 张旧同名表结构仍为 V3，V4 比赛域上线前必须 cleanup + 重建。
2. 冒烟通过 HTTP API（admin:8080），未逐页手工点 UI；菜单/权限/组件路径已 DB + 静态双重确认。
3. dev 库存在冒烟测试残留 Season（id=2/3），不影响业务，可按需清理。

### 16.12 是否可以进入第 3 轮

**可以。** 第 2 轮落库 + 后台 V4 发布主链路冒烟已通过。第 3 轮（队长授权 / 球队报名）可在当前 `basket_series/season/division` 基础上继续，无需先 cleanup 旧同名表。

---

## 验收对照（本轮）

| # | 验收项 | 状态 |
|---|--------|------|
| 1 | 后台可查看 Series | ✓ API 冒烟通过 |
| 2 | 创建 Season | ✓ API 冒烟通过 |
| 3 | 编辑 Season | ✓ API 冒烟通过 |
| 4 | 发布 Season | ✓ API 冒烟通过 |
| 5 | 下线 Season | ✓ API 冒烟通过 |
| 6 | 查看 Season 下 Division | ✓ Seed + API |
| 7 | 创建 Division | ✓ API 冒烟通过 |
| 8 | 编辑 Division | ✓ 代码就绪（冒烟未单独测编辑 API） |
| 9 | 开放 Division 报名 | ✓ API 冒烟通过 |
| 10 | 关闭 Division 报名 | ✓ API 冒烟通过 |
| 11 | 删除 DRAFT Division | ✓ 代码就绪 |
| 12 | PERSONAL 不可提交 | ✓ API 拒绝 |
| 13 | 非 REGULAR_ROUND_ROBIN 不可提交 | ✓ API 拒绝 |
| 14 | 同 Season 同平台不可重复 | ✓ API 拒绝 |
| 15 | 同 Series+Year+SeasonName 不可重复 | ✓ Service 逻辑 |
| 16 | V4 不调用旧 Service | ✓ |
| 17 | V4 不 import 旧 Enum | ✓ |
| 18 | 旧入口已隐藏/标废弃 | ✓ DB + 静态 |
| 19 | 旧 Job 已检查记录 | ✓ dev 无启用记录 |
| 20 | SQL 落库 | ✓ 见第 16 节 |
| 21 | admin-ui 构建 | ✓（前轮） |
| 22 | 不改 web-ui | ✓ |
| 23 | 不写报名/赛程/比赛 | ✓ |
| 24 | 不进入第 3 轮 | ✓ 本轮仅验收，建议下轮进入 |
