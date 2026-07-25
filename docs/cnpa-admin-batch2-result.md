# CNPA Admin 第二批重构结果

> 执行日期：2026-07-25  
> 范围：后台菜单、页面目录、动态路由（结构性重构，非业务重写）

## 1. 执行概况

| 指标 | 数量 |
|------|------|
| 移动页面 | **26**（25 业务页 + 1 工作台副本） |
| 修改菜单记录 | **31**（28 更新 + 3 新增） |
| 新增一级菜单 | **3**（2201 运营工作台、2204 报名与阵容、2207 纪律与档案） |
| 提升为一级目录 | **5**（复用 2151/2152/2153/2154/2101） |
| 保留叶子 menu_id | **25**（2001~2108 等业务页 ID 未变） |
| 按钮 menu_id | **124**（2008~2150，未改 parent 除随页面迁移外） |
| 角色菜单调整 | 删除 role2→2000；新增 role2→2201/2204/2207 |
| 停用旧目录 | **1**（2000 CNPA赛事管理，visible=1 status=1） |

### 前置文档说明

规范要求的 `cnpa-admin-current-audit.md` 等 5 份文档中，**3 份未单独落盘**；本批以以下文档为映射依据：

- `docs/cnpa-admin-refactor-batch1-baseline.md`（盘点 + 页面/API/表映射）
- `docs/cnpa-admin-route-menu-mapping.md`（上一版，已被本结果 supersede）
- `docs/cnpa-admin-refactor-roadmap.md`

---

## 2. 新菜单树（数据库实际）

```text
[停用] CNPA赛事管理 (2000, hidden)

运营工作台 (2201, C)                    → cnpa/dashboard/index
赛事管理 (2152, M)
├── 赛季管理 (2005)
├── 赛事列表 (2004)                     ← 原「赛事管理」改名
├── 赛事规则 (2006)
├── 赛事阶段 (2038)
└── 分组管理 (2039)
账号与认证 (2151, M)
├── 球员认证审核 (2001)
├── 队长资格审核 (2002)                 ← 改名
└── 球队审核认证 (2003)                 ← 改名
报名与阵容 (2204, M)
└── 球队报名审核 (2007)                 ← 原报名审核迁入
比赛执行 (2153, M)
├── 赛程管理 (2040)
├── 比分录入 (2042)
├── 比赛数据录入 (2043)                 ← 原「比赛数据」改名
└── 比赛名单 (2041)                     ← 保留已有页，非目标树项
数据统计 (2154, M)
├── 数据结算 (2080)
├── 积分榜 (2081)
├── 球队赛季统计 (2082)
├── 球员赛季统计 (2083)
├── 排行榜 (2084)
└── 赛季名次 (2102)                     ← 保留已有页
纪律与档案 (2207, M)                    ← 空目录，第四批补页
内容运营 (2101, M)
├── 公告管理 (2106)
├── 球队荣誉 (2103)
├── 球员荣誉 (2104)
├── 名人堂管理 (2105)
├── [隐藏] 首页内容 (2107)
└── [隐藏] 合作伙伴 (2108)
```

若依系统模块（系统管理/监控/工具）**未改动**。

---

## 3. 文件移动清单

| 旧路径 | 新路径 |
|--------|--------|
| `views/index.vue`（内容复制） | `views/cnpa/dashboard/index.vue` |
| `views/basket/audit/player-cert/index.vue` | `views/cnpa/identity/player-certification/index.vue` |
| `views/basket/audit/captain-qualification/index.vue` | `views/cnpa/identity/captain-qualification/index.vue` |
| `views/basket/audit/team-audit/index.vue` | `views/cnpa/identity/team-certification/index.vue` |
| `views/basket/audit/registration-audit/index.vue` | `views/cnpa/participation/team-registration/index.vue` |
| `views/basket/tournament/season/index.vue` | `views/cnpa/event/season/index.vue` |
| `views/basket/tournament/manage/index.vue` | `views/cnpa/event/list/index.vue` |
| `views/basket/tournament/rule/index.vue` | `views/cnpa/event/rule/index.vue` |
| `views/basket/tournament/stage/index.vue` | `views/cnpa/event/stage/index.vue` |
| `views/basket/tournament/group/index.vue` | `views/cnpa/event/group/index.vue` |
| `views/basket/match/schedule/index.vue` | `views/cnpa/competition/schedule/index.vue` |
| `views/basket/match/score/index.vue` | `views/cnpa/competition/score/index.vue` |
| `views/basket/match/stat/index.vue` | `views/cnpa/competition/match-data-entry/index.vue` |
| `views/basket/match/lineup/index.vue` | `views/cnpa/competition/lineup/index.vue` |
| `views/basket/settlement/data/index.vue` | `views/cnpa/statistics/settlement/index.vue` |
| `views/basket/settlement/standing/index.vue` | `views/cnpa/statistics/standings/index.vue` |
| `views/basket/settlement/team-season-stat/index.vue` | `views/cnpa/statistics/team-season/index.vue` |
| `views/basket/settlement/player-season-stat/index.vue` | `views/cnpa/statistics/player-season/index.vue` |
| `views/basket/settlement/ranking/index.vue` | `views/cnpa/statistics/ranking/index.vue` |
| `views/basket/settlement/season-result/index.vue` | `views/cnpa/statistics/season-result/index.vue` |
| `views/basket/content/announcement/index.vue` | `views/cnpa/content/announcement/index.vue` |
| `views/basket/content/team-honor/index.vue` | `views/cnpa/content/team-honor/index.vue` |
| `views/basket/content/player-honor/index.vue` | `views/cnpa/content/player-honor/index.vue` |
| `views/basket/content/hall-of-fame/index.vue` | `views/cnpa/content/hall-of-fame/index.vue` |
| `views/basket/content/home-banner/index.vue` | `views/cnpa/content/home-banner/index.vue` |
| `views/basket/content/partner/index.vue` | `views/cnpa/content/partner/index.vue` |

`views/basket/` 目录已清空删除。静态 `/index` 路由改为加载 `cnpa/dashboard/index.vue`。

---

## 4. 路由与 component 映射（叶子页）

| menu_id | 菜单名 | 旧 component | 新 component | perms |
|---------|--------|--------------|--------------|-------|
| 2201 | 运营工作台 | — | cnpa/dashboard/index | — |
| 2005 | 赛季管理 | basket/tournament/season/index | cnpa/event/season/index | basket:season:list |
| 2004 | 赛事列表 | basket/tournament/manage/index | cnpa/event/list/index | basket:tournament:list |
| 2006 | 赛事规则 | basket/tournament/rule/index | cnpa/event/rule/index | basket:tournamentRule:list |
| 2038 | 赛事阶段 | basket/tournament/stage/index | cnpa/event/stage/index | basket:stage:list |
| 2039 | 分组管理 | basket/tournament/group/index | cnpa/event/group/index | basket:group:list |
| 2001 | 球员认证审核 | basket/audit/player-cert/index | cnpa/identity/player-certification/index | basket:playerCert:list |
| 2002 | 队长资格审核 | basket/audit/captain-qualification/index | cnpa/identity/captain-qualification/index | basket:captainQualification:list |
| 2003 | 球队审核认证 | basket/audit/team-audit/index | cnpa/identity/team-certification/index | basket:teamAudit:list |
| 2007 | 球队报名审核 | basket/audit/registration-audit/index | cnpa/participation/team-registration/index | basket:registrationAudit:list |
| 2040 | 赛程管理 | basket/match/schedule/index | cnpa/competition/schedule/index | basket:match:list |
| 2042 | 比分录入 | basket/match/score/index | cnpa/competition/score/index | basket:score:query |
| 2043 | 比赛数据录入 | basket/match/stat/index | cnpa/competition/match-data-entry/index | basket:matchStat:query |
| 2041 | 比赛名单 | basket/match/lineup/index | cnpa/competition/lineup/index | basket:lineup:list |
| 2080~2102 | 统计六页 | basket/settlement/* | cnpa/statistics/* | 原 perms 不变 |
| 2103~2108 | 内容六页 | basket/content/* | cnpa/content/* | 原 perms 不变 |

---

## 5. 数据库修改

| 表 | 操作 |
|----|------|
| `sys_menu` | INSERT 3；UPDATE 28；停用 2000 |
| `sys_role_menu` | DELETE 2000 授权；INSERT 2201/2204/2207 给 role_id=2 |
| `basket_*` | **未修改** |

SQL 文件：

- 快照/回滚：`docs/sql/cnpa-admin-menu-before.sql`
- 正向迁移：`docs/sql/cnpa-admin-menu-refactor.sql`
- 回滚入口：`docs/sql/cnpa-admin-menu-rollback.sql`

---

## 6. 保留内容

- 全部页面 **Vue 业务逻辑、API 调用、v-hasPermi 权限字符** 未改
- 全部后端 **Controller 路径与接口契约** 未改
- 全部叶子 **menu_id** 与按钮 **perms** 未改
- role_id=2 叶子权限集合未扩大（仅补父级目录 + 工作台）

---

## 7. 验证结果

| 项 | 结果 |
|----|------|
| 前端 `npm run build:prod` | ✅ 通过 |
| 后端 `mvn compile -DskipTests` | ✅ 通过 |
| 菜单树 DB 查询 | ✅ 与上文一致 |
| 登录/UI 手工点验 | ⚠️ 需本地重新登录后验证侧边栏与页面加载 |

---

## 8. 遗留问题（后续批次）

| 项 | 批次 |
|----|------|
| 账号与球员管理页 | 第三批 |
| Open 报名、正式赛事球队、阵容/转会/自由球员/队长移交 | 第四批 |
| 比赛数据审核、数据争议、弃赛异常（独立页） | 第四/五批 |
| 纪律与档案四页 | 第四批 |
| 赛事荣誉独立页、荣誉拆分 | 第四批评估 |
| 业务审计记录（非若依操作日志） | 第六批 |
| 球员认证字段去 KYC | 第三批 |
| `ranking:publish` / `dataSettlement:confirm` 菜单权限无后端 API | 第二批后发现，第三批对齐 |

---

## 9. 数据库安全确认

**本批次仅修改了 `sys_menu` 与 `sys_role_menu`，未修改任何 CNPA 业务表或业务数据。**

---

## 10. 第三批准备情况

可开始：

- 账号与球员管理（新建页 + 菜单）
- 球员认证 / 队长 / 球队审核页业务字段整理（去手机号/身份证）
- 永久球员基础档案设计

待确认：

- 账号与球员管理是否只读聚合 Web 侧数据，还是 Admin 独立 CRUD
- 永久球员 ID 生成规则
