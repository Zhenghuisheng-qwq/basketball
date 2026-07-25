# CNPA Admin 菜单与路由映射（第一批）

> **盘点日期**：2026-07-25  
> **数据源**：`basketball.sys_menu`（只读 SELECT）  
> **说明**：库中菜单树已接近目标一级结构（含历史第二批变更）；本文档记录**当前真实状态** + **目标结构** + **映射关系**

---

## 1. 当前菜单结构（数据库树）

```text
[2201] 运营工作台 (C) path=operations-dashboard component=cnpa/dashboard/index perms=
[2152] 赛事管理 (M) path=event
├── [2005] 赛季管理 (C) path=season component=cnpa/event/season/index perms=basket:season:list
├── [2004] 赛事列表 (C) path=list component=cnpa/event/list/index perms=basket:tournament:list
├── [2006] 赛事规则 (C) path=rule component=cnpa/event/rule/index perms=basket:tournamentRule:list
├── [2038] 赛事阶段 (C) path=stage component=cnpa/event/stage/index perms=basket:stage:list
└── [2039] 分组管理 (C) path=group component=cnpa/event/group/index perms=basket:group:list
[2151] 账号与认证 (M) path=identity
├── [2001] 球员认证审核 (C) path=player-certification component=cnpa/identity/player-certification/index perms=basket:playerCert:list
├── [2002] 队长资格审核 (C) path=captain-qualification component=cnpa/identity/captain-qualification/index perms=basket:captainQualification:list
└── [2003] 球队审核认证 (C) path=team-certification component=cnpa/identity/team-certification/index perms=basket:teamAudit:list
[2204] 报名与阵容 (M) path=participation
└── [2007] 球队报名审核 (C) path=team-registration component=cnpa/participation/team-registration/index perms=basket:registrationAudit:list
[2153] 比赛执行 (M) path=competition-exec
├── [2040] 赛程管理 (C) path=schedule component=cnpa/competition/schedule/index perms=basket:match:list
├── [2042] 比分录入 (C) path=score component=cnpa/competition/score/index perms=basket:score:query
├── [2043] 比赛数据录入 (C) path=match-data-entry component=cnpa/competition/match-data-entry/index perms=basket:matchStat:query
└── [2041] 比赛名单 (C) path=lineup component=cnpa/competition/lineup/index perms=basket:lineup:list
[2154] 数据统计 (M) path=statistics
├── [2080] 数据结算 (C) path=settlement component=cnpa/statistics/settlement/index perms=basket:dataSettlement:list
├── [2081] 积分榜 (C) path=standings component=cnpa/statistics/standings/index perms=basket:standing:list
├── [2082] 球队赛季统计 (C) path=team-season component=cnpa/statistics/team-season/index perms=basket:teamSeasonStat:list
├── [2083] 球员赛季统计 (C) path=player-season component=cnpa/statistics/player-season/index perms=basket:playerSeasonStat:list
├── [2084] 排行榜 (C) path=ranking component=cnpa/statistics/ranking/index perms=basket:ranking:list
└── [2102] 赛季名次 (C) path=season-result component=cnpa/statistics/season-result/index perms=basket:seasonResult:list
[2207] 纪律与档案 (M) path=discipline
    （无子菜单）
[2101] 内容运营 (M) path=content-ops
├── [2106] 公告管理 (C) path=announcement component=cnpa/content/announcement/index perms=basket:announcement:list
├── [2103] 球队荣誉 (C) path=team-honor component=cnpa/content/team-honor/index perms=basket:teamHonor:list
├── [2104] 球员荣誉 (C) path=player-honor component=cnpa/content/player-honor/index perms=basket:playerHonor:list
├── [2105] 名人堂管理 (C) path=hall-of-fame component=cnpa/content/hall-of-fame/index perms=basket:hallOfFame:list
├── [2107] 首页内容 (C, hidden) path=home-banner component=cnpa/content/home-banner/index perms=basket:homeContent:list
└── [2108] 合作伙伴 (C, hidden) path=partner component=cnpa/content/partner/index perms=basket:partner:list

[2000] CNPA赛事管理 (M, 已停用 visible=1 status=1) path=basket — 不再展示
```

### 1.1 菜单统计

| 项 | 值 |
|----|-----|
| CNPA 菜单总数 (menu_id 2000–2999) | 158 |
| 目录 M | 8 + 1 停用 |
| 页面 C | 26 |
| 按钮 F | 124 |
| 孤儿菜单 | 0 |
| 重复 perms | 0 |
| component 与 vue 文件 | 26/26 一致 |

### 1.2 角色授权

| role_id | 角色名 | CNPA 授权数 |
|---------|--------|-------------|
| 1 | 超级管理员 | 0（全量） |
| 2 | 普通角色 | 157 |

---

## 2. 目标菜单结构（方案，未完全落地）

```text
运营工作台

赛事管理
├── 赛季管理
├── 赛事列表
├── 赛事规则
├── 赛事阶段
└── 分组管理

账号与认证
├── 账号与球员管理          ← 缺失
├── 球员认证审核
├── 队长资格审核
└── 球队审核认证

报名与阵容
├── 球队报名审核
├── Open 报名审核           ← 缺失
├── 正式赛事球队            ← 缺失
├── 赛事阵容管理            ← 缺失
├── 转会审核                ← 缺失
├── 自由球员补充审核        ← 缺失
├── 队长移交审核            ← 缺失
└── 阵容特殊操作            ← 缺失

比赛执行
├── 赛程管理
├── 比分录入
├── 比赛数据录入
├── 比赛数据审核            ← 缺失（当前合并在录入页）
├── 数据争议处理            ← 缺失
└── 弃赛与异常比赛处理      ← 缺失

数据统计
├── 数据结算
├── 积分榜
├── 球队赛季统计
├── 球员赛季统计
└── 排行榜
    （赛季名次：保留或并入荣誉）

纪律与档案
├── 球队与球员处罚          ← 缺失
├── 永久球队档案            ← 缺失
├── 永久球员档案            ← 缺失
└── 业务审计记录            ← 缺失

内容运营
├── 公告管理
├── 赛事荣誉                ← 缺失独立页
├── 球队荣誉
├── 球员荣誉
└── 名人堂管理
```

---

## 3. 新旧映射表（已实现页面）

| 当前菜单 | menu_id | 当前 path | 当前 component | 目标一级 | 目标名称 | 建议 path | 建议 component | 保留 menu_id |
|----------|---------|-----------|----------------|----------|----------|-----------|----------------|--------------|
| 运营工作台 | 2201 | operations-dashboard | cnpa/dashboard/index | 运营工作台 | 运营工作台 | operations-dashboard | cnpa/dashboard/index | ✅ |
| 赛季管理 | 2005 | season | cnpa/event/season/index | 赛事管理 | 赛季管理 | season | cnpa/event/season/index | ✅ |
| 赛事列表 | 2004 | list | cnpa/event/list/index | 赛事管理 | 赛事列表 | list | cnpa/event/list/index | ✅ |
| 赛事规则 | 2006 | rule | cnpa/event/rule/index | 赛事管理 | 赛事规则 | rule | cnpa/event/rule/index | ✅ |
| 赛事阶段 | 2038 | stage | cnpa/event/stage/index | 赛事管理 | 赛事阶段 | stage | cnpa/event/stage/index | ✅ |
| 分组管理 | 2039 | group | cnpa/event/group/index | 赛事管理 | 分组管理 | group | cnpa/event/group/index | ✅ |
| 球员认证审核 | 2001 | player-certification | cnpa/identity/player-certification/index | 账号与认证 | 球员认证审核 | player-certification | cnpa/identity/player-certification/index | ✅ |
| 队长资格审核 | 2002 | captain-qualification | cnpa/identity/captain-qualification/index | 账号与认证 | 队长资格审核 | captain-qualification | cnpa/identity/captain-qualification/index | ✅ |
| 球队审核认证 | 2003 | team-certification | cnpa/identity/team-certification/index | 账号与认证 | 球队审核认证 | team-certification | cnpa/identity/team-certification/index | ✅ |
| 球队报名审核 | 2007 | team-registration | cnpa/participation/team-registration/index | 报名与阵容 | 球队报名审核 | team-registration | cnpa/participation/team-registration/index | ✅ |
| 赛程管理 | 2040 | schedule | cnpa/competition/schedule/index | 比赛执行 | 赛程管理 | schedule | cnpa/competition/schedule/index | ✅ |
| 比分录入 | 2042 | score | cnpa/competition/score/index | 比赛执行 | 比分录入 | score | cnpa/competition/score/index | ✅ |
| 比赛数据录入 | 2043 | match-data-entry | cnpa/competition/match-data-entry/index | 比赛执行 | 比赛数据录入 | match-data-entry | cnpa/competition/match-data-entry/index | ✅ |
| 比赛名单 | 2041 | lineup | cnpa/competition/lineup/index | 比赛执行 | 比赛名单 | lineup | cnpa/competition/lineup/index | ✅ |
| 数据结算 | 2080 | settlement | cnpa/statistics/settlement/index | 数据统计 | 数据结算 | settlement | cnpa/statistics/settlement/index | ✅ |
| 积分榜 | 2081 | standings | cnpa/statistics/standings/index | 数据统计 | 积分榜 | standings | cnpa/statistics/standings/index | ✅ |
| 球队赛季统计 | 2082 | team-season | cnpa/statistics/team-season/index | 数据统计 | 球队赛季统计 | team-season | cnpa/statistics/team-season/index | ✅ |
| 球员赛季统计 | 2083 | player-season | cnpa/statistics/player-season/index | 数据统计 | 球员赛季统计 | player-season | cnpa/statistics/player-season/index | ✅ |
| 排行榜 | 2084 | ranking | cnpa/statistics/ranking/index | 数据统计 | 排行榜 | ranking | cnpa/statistics/ranking/index | ✅ |
| 赛季名次 | 2102 | season-result | cnpa/statistics/season-result/index | 数据统计 | 赛季名次 | season-result | cnpa/statistics/season-result/index | ✅ |
| 公告管理 | 2106 | announcement | cnpa/content/announcement/index | 内容运营 | 公告管理 | announcement | cnpa/content/announcement/index | ✅ |
| 球队荣誉 | 2103 | team-honor | cnpa/content/team-honor/index | 内容运营 | 球队荣誉 | team-honor | cnpa/content/team-honor/index | ✅ |
| 球员荣誉 | 2104 | player-honor | cnpa/content/player-honor/index | 内容运营 | 球员荣誉 | player-honor | cnpa/content/player-honor/index | ✅ |
| 名人堂 | 2105 | hall-of-fame | cnpa/content/hall-of-fame/index | 内容运营 | 名人堂管理 | hall-of-fame | cnpa/content/hall-of-fame/index | ✅ |

### 3.1 待新增菜单（无 menu_id）

| 目标一级 | 目标名称 | 建议 component | 批次 |
|----------|----------|----------------|------|
| 账号与认证 | 账号与球员管理 | cnpa/identity/account-player/index | 第三批 |
| 报名与阵容 | Open 报名审核 | cnpa/participation/open-registration/index | 第四批 |
| 报名与阵容 | 正式赛事球队 | cnpa/participation/official-teams/index | 第四批 |
| 报名与阵容 | 赛事阵容管理 | cnpa/participation/roster/index | 第四批 |
| 报名与阵容 | 转会审核 | cnpa/participation/transfer/index | 第四批 |
| 纪律与档案 | 球队与球员处罚 | cnpa/discipline/penalty/index | 第六批 |
| 纪律与档案 | 业务审计记录 | cnpa/discipline/audit-log/index | 第六批 |
| 比赛执行 | 比赛数据审核 | cnpa/competition/match-data-audit/index | 第五批 |

---

## 4. 前端路由机制

| 机制 | 说明 | 文件 |
|------|------|------|
| 动态路由 | 登录后 `GET /getRouters` → `permission.generateRoutes` | `src/permission.ts` |
| 组件加载 | `import.meta.glob('./../../views/**/*.vue')` 精确匹配 | `src/store/modules/permission.ts` |
| 刷新恢复 | Token 有效则重新拉取路由 | `src/permission.ts` |
| keep-alive | 路由 `meta.noCache` + 组件 `name` | 菜单 `is_cache` 字段 |
| 隐藏路由 | `visible='1'` 或静态 `router/index.ts` hidden | 2107/2108 |
| 静态 /index | 仍指向 `cnpa/dashboard/index.vue` | `src/router/index.ts` — **与 2201 重复** |

---

## 5. 第二批执行状态（历史）

以下已在历史会话中落地（本第一批只记录，不重复执行）：

- `views/basket/*` → `views/cnpa/*`
- 8 个一级业务菜单提升
- `2000 CNPA赛事管理` 停用
- 26 个叶子 menu component 更新为 `cnpa/...`

回滚 SQL：`docs/sql/cnpa-admin-menu-rollback.sql`
