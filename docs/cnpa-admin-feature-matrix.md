# CNPA Admin 功能映射矩阵（第一批）

> **盘点日期**：2026-07-25  
> **分类标准**：A 可直接使用 | B 小幅修正 | C 页面保留逻辑需重构 | D 尚未实现 | E 重复需合并 | F 错误实现

---

## 1. 总览矩阵

| 业务域 | 功能 | 当前菜单 | menu_id | 页面路径 | API 文件 | 接口 | Controller | Service | Mapper | 数据表 | 当前状态 | 分类 | 主要问题 | 下一批处理建议 |
|--------|------|----------|---------|----------|----------|------|------------|---------|--------|--------|----------|------|----------|----------------|
| 运营工作台 | 运营工作台 | 运营工作台 | 2201 | cnpa/dashboard/index.vue | 无 | 无 | 无 | 无 | 无 | 无 | 若依默认首页文案 | **D** | 无汇总/待办 API | 第四批后新建 Dashboard API |
| 赛事管理 | 赛季管理 | 赛季管理 | 2005 | cnpa/event/season/index.vue | season.ts | GET/POST/PUT/DELETE `/basket/season/*` | BasketSeasonController | BasketSeasonServiceImpl | BasketTournamentSeasonMapper | basket_tournament_season | 完整 CRUD | **A** | — | 保留 |
| 赛事管理 | 赛事列表 | 赛事列表 | 2004 | cnpa/event/list/index.vue | tournament.ts | `/basket/tournament/*` | BasketTournamentController | BasketTournamentServiceImpl | BasketTournamentMapper | basket_tournament | 完整 CRUD + 状态 | **A** | — | 保留 |
| 赛事管理 | 赛事规则 | 赛事规则 | 2006 | cnpa/event/rule/index.vue | tournamentRule.ts | `/basket/tournamentRule/*` | BasketTournamentRuleController | BasketTournamentRuleServiceImpl | BasketTournamentRuleMapper | basket_tournament_rule | 完整 CRUD | **A** | — | 保留 |
| 赛事管理 | 赛事阶段 | 赛事阶段 | 2038 | cnpa/event/stage/index.vue | tournamentStage.ts | `/basket/stage/*` | BasketTournamentStageController | BasketTournamentStageServiceImpl | BasketTournamentStageMapper | basket_tournament_stage | 完整 CRUD | **A** | — | 保留 |
| 赛事管理 | 分组管理 | 分组管理 | 2039 | cnpa/event/group/index.vue | tournamentGroup.ts | `/basket/group/*` | BasketTournamentGroupController | BasketTournamentGroupServiceImpl | BasketTournamentGroupMapper | basket_tournament_group, basket_tournament_group_team | 分组+抽签入组 | **A** | 与报名审核未自动联动 | 第四批串联 |
| 账号与认证 | 球员认证审核 | 球员认证审核 | 2001 | cnpa/identity/player-certification/index.vue | playerCert.ts | `/basket/playerCert/*` | BasketPlayerCertController | BasketPlayerCertServiceImpl | BasketPlayerCert*Mapper | basket_player_cert_application/audit/material, basket_player_profile | 审核闭环，生成 playerId | **A** | 无身份证字段（正确） | 第三批字段对齐 |
| 账号与认证 | 队长资格审核 | 队长资格审核 | 2002 | cnpa/identity/captain-qualification/index.vue | captainQualification.ts | `/basket/captainQualification/*` | BasketCaptainQualificationController | BasketCaptainQualificationServiceImpl | BasketCaptainQualificationMapper | basket_captain_qualification | 授予/撤销 | **A** | — | 保留 |
| 账号与认证 | 球队审核认证 | 球队审核认证 | 2003 | cnpa/identity/team-certification/index.vue | teamAudit.ts | `/basket/teamAudit/*` | BasketTeamAuditController | BasketTeamAuditServiceImpl | BasketTeamMapper, BasketTeamAuditRecordMapper | basket_team, basket_team_audit_record | 审核主流程完整 | **B** | 无 `audit-record/list` API（球员认证有） | 第三批补 API |
| 账号与认证 | 账号与球员管理 | 无 | — | 无 | 无 | 无 | 无 | 无 | 无 | basket_member_account, basket_player_profile | 未实现 Admin 页 | **D** | Web 端有个人中心 | 第三批评估是否需要 |
| 账号与认证 | 平台与游戏 ID 管理 | 无 | — | 无 | 无 | 无 | 无 | 无 | 无 | player_profile.game_* | 字段在认证/profile 内 | **D** | 无独立 Admin 页 | 待确认 |
| 报名与阵容 | 球队报名审核 | 球队报名审核 | 2007 | cnpa/participation/team-registration/index.vue | registrationAudit.ts | `/basket/registrationAudit/*` | BasketRegistrationAuditController | BasketRegistrationAuditServiceImpl | BasketTournamentRegistration*Mapper | basket_tournament_registration/audit/player | 审核+校验+消息 | **C** | approve 仅改状态，未写 group_team/season_profile | **第四批核心** |
| 报名与阵容 | Open 报名审核 | 无 | — | 无 | 无 | 无 | 无 | 无 | 无 | 待确认表 | Web Open 为 Mock | **D** | 无 Admin 入口 | 第四批 |
| 报名与阵容 | 正式赛事球队 | 无 | — | 无 | 无 | 无 | 无 | 无 | groupTeamMapper | basket_tournament_group_team | 仅分组管理手动入组 | **C** | 与报名审核分离 | 第四批 |
| 报名与阵容 | 赛事阵容管理 | 无 | — | 无 | 无 | 无 | 无 | 无 | 无 | registration_player, team_member | 无独立页 | **D** | — | 第四批 |
| 报名与阵容 | 转会审核 | 无 | — | 无 | 无 | 无 | 无 | 无 | 无 | 无表 | 完全缺失 | **D** | — | 第四批 |
| 报名与阵容 | 自由球员补充 | 无 | — | 无 | 无 | 无 | 无 | 无 | 无 | 无 | 完全缺失 | **D** | — | 第四批 |
| 报名与阵容 | 队长移交审核 | 无 | — | 无 | 无 | 无 | 无 | 无 | BasketTeamCaptainHistoryMapper | basket_team_captain_history | 表存在无 Admin | **D** | — | 第四批 |
| 比赛执行 | 赛程管理 | 赛程管理 | 2040 | cnpa/competition/schedule/index.vue | matchSchedule.ts | `/basket/match/*` | BasketMatchScheduleController | BasketMatchScheduleServiceImpl | BasketMatchMapper | basket_match, basket_match_team | 完整 | **A** | — | 保留 |
| 比赛执行 | 比分录入 | 比分录入 | 2042 | cnpa/competition/score/index.vue | matchScore.ts | `/basket/score/*` | BasketMatchScoreController | BasketMatchScoreServiceImpl | BasketMatchMapper, BasketMatchPeriodScoreMapper | basket_match, basket_match_period_score | submit 完整 | **B** | confirmScore 仅写 status_log | 第五批补 data_status |
| 比赛执行 | 比赛数据录入 | 比赛数据录入 | 2043 | cnpa/competition/match-data-entry/index.vue | matchStat.ts | `/basket/matchStat/*` | BasketMatchStatController | BasketMatchStatServiceImpl | BasketMatch*StatMapper | basket_match_player_stat, basket_match_team_stat | confirm 触发结算 | **A** | — | 保留 |
| 比赛执行 | 比赛名单 | 比赛名单 | 2041 | cnpa/competition/lineup/index.vue | matchLineup.ts | `/basket/lineup/*` | BasketMatchLineupController | BasketMatchLineupServiceImpl | BasketMatchRosterMapper | basket_match_roster | 完整 | **A** | — | 保留 |
| 比赛执行 | 比赛数据审核（独立页） | 无 | — | 无 | matchStat.ts confirm | 合并在 matchStat | BasketMatchStatController | BasketMatchStatServiceImpl | — | — | 功能在录入页 | **D** | 无独立审核工作台 | 第五批 |
| 比赛执行 | 数据争议处理 | 无 | — | 无 | 无 | 无 | 无 | 无 | basket_stat_change_record | 有变更记录无 UI | **D** | — | 第五批 |
| 比赛执行 | 弃赛/异常比赛 | 无 | — | 无 | matchScore forfeit 等 | 部分在 score | BasketMatchScoreController | BasketMatchScoreServiceImpl | basket_match_status_log | 有 forfeit API | **D** | 无独立异常页 | 第五批 |
| 数据统计 | 数据结算 | 数据结算 | 2080 | cnpa/statistics/settlement/index.vue | dataSettlement.ts | `/basket/dataSettlement/*` | BasketDataSettlementController | BasketDataSettlementServiceImpl | 多 Mapper | standing/rank/season_stat | Redis 锁+重算 | **A** | 菜单 confirm 权限无 API | 第五批对齐权限 |
| 数据统计 | 积分榜 | 积分榜 | 2081 | cnpa/statistics/standings/index.vue | standing.ts | `/basket/standing/*` | BasketStandingController | BasketStandingServiceImpl | BasketStageStandingAdminMapper | basket_stage_standing | 查询 | **A** | — | 保留 |
| 数据统计 | 球队赛季统计 | 球队赛季统计 | 2082 | cnpa/statistics/team-season/index.vue | teamSeasonStat.ts | `/basket/teamSeasonStat/*` | BasketTeamSeasonStatController | — | BasketTeamSeasonStatAdminMapper | basket_team_season_stat | 查询 | **A** | — | 保留 |
| 数据统计 | 球员赛季统计 | 球员赛季统计 | 2083 | cnpa/statistics/player-season/index.vue | playerSeasonStat.ts | `/basket/playerSeasonStat/*` | BasketPlayerSeasonStatController | — | BasketPlayerSeasonStatAdminMapper | basket_player_season_stat | 查询 | **A** | — | 保留 |
| 数据统计 | 排行榜 | 排行榜 | 2084 | cnpa/statistics/ranking/index.vue | ranking.ts | `/basket/ranking/*` | BasketRankingController | BasketRankingServiceImpl | BasketPlayerRankMapper, BasketTeamRankMapper | basket_player_rank, basket_team_rank | 查询+重算 | **A** | publish 权限无 API | 第五批 |
| 数据统计 | 赛季名次 | 赛季名次 | 2102 | cnpa/statistics/season-result/index.vue | seasonResult.ts | `/basket/seasonResult/*` | BasketSeasonResultController | — | — | honor/award 相关 | 查询维护 | **A** | — | 保留 |
| 纪律与档案 | 球队与球员处罚 | 无 | — | 无 | 无 | 无 | 无 | 无 | BasketDisciplineRecordMapper | basket_discipline_record (0行) | 仅报名校验引用 | **D** | 无 Admin 模块 | 第六批 |
| 纪律与档案 | 永久球队/球员档案 | 无 | — | 无 | 无 | 无 | 无 | 无 | — | basket_team, basket_player_profile | 与赛事实体混在同一表 | **C** | 无永久/赛事分离模型 | 第六批 |
| 纪律与档案 | 业务审计记录 | 无 | — | 无 | 无 | 无 | 无 | 无 | 多 audit 表 | 分散在各业务表 | **D** | ≠ sys_oper_log | 第六批 |
| 内容运营 | 公告管理 | 公告管理 | 2106 | cnpa/content/announcement/index.vue | announcement.ts | `/basket/announcement/*` | BasketAnnouncementController | — | BasketAnnouncementMapper | basket_announcement | CRUD | **A** | 定时/规则公告待确认 | 第六批增强 |
| 内容运营 | 球队荣誉 | 球队荣誉 | 2103 | cnpa/content/team-honor/index.vue | teamHonor.ts | `/basket/teamHonor/*` | BasketTeamHonorController | — | BasketHonorRecordMapper | basket_honor_record | CRUD | **A** | — | 保留 |
| 内容运营 | 球员荣誉 | 球员荣誉 | 2104 | cnpa/content/player-honor/index.vue | playerHonor.ts | `/basket/playerHonor/*` | BasketPlayerHonorController | — | basket_honor_record | CRUD | **A** | — | 保留 |
| 内容运营 | 名人堂 | 名人堂管理 | 2105 | cnpa/content/hall-of-fame/index.vue | hallOfFame.ts | `/basket/hallOfFame/*` | BasketHallOfFameController | — | — | CRUD | **A** | — | 保留 |
| 内容运营 | 首页内容 | 首页内容(hidden) | 2107 | cnpa/content/home-banner/index.vue | homeContent.ts | `/basket/homeContent/*` | BasketHomeBannerController | — | basket_home_banner | CRUD | **A** | 菜单 hidden | **E** 目标菜单外 |
| 内容运营 | 合作伙伴 | 合作伙伴(hidden) | 2108 | cnpa/content/partner/index.vue | partner.ts | `/basket/partner/*` | BasketPartnerController | — | basket_partner | CRUD | **A** | 菜单 hidden | **E** |
| 内容运营 | 赛事荣誉（独立） | 无 | — | 无 | seasonResult/honor | 部分合并 | — | — | basket_honor_lineup | 冠军阵容表存在 | **B** | 无独立「赛事荣誉」页 | 第六批 |
| — | 静态首页 | router /index | — | cnpa/dashboard/index.vue | 无 | 无 | 无 | 无 | 无 | 与 2201 重复 | **E** | 双入口 | 第二批后待合并 |

---

## 2. 分类统计

| 分类 | 数量 | 代表功能 |
|------|------|----------|
| **A** | 18 | 赛事 CRUD、三类审核（球员/队长/球队）、比赛录入、统计查询、内容 CRUD |
| **B** | 4 | 球队审核缺 audit API、比分 confirm、权限菜单对齐、赛事荣誉页 |
| **C** | 3 | 报名审核→正式参赛、永久档案模型、分组与报名联动 |
| **D** | 15 | 工作台、Open报名、转会、纪律、业务审计、独立审核页等 |
| **E** | 2 | 静态 /index 与 2201；hidden 首页内容/合作伙伴 |
| **F** | 0 | Admin 侧球员认证已无身份证/手机号字段 |

---

## 3. 隐藏路由与详情页

| 类型 | 说明 |
|------|------|
| 详情抽屉/对话框 | 各 list 页内嵌，无独立 menu_id；均调用 `GET /{id}` |
| 审核操作 | 列表页按钮 + `v-hasPermi`；非独立路由 |
| ParentView | CNPA 无三级菜单页面，未使用 ParentView 嵌套 |
| InnerLink | CNPA 未使用 |
| 单届赛事概览 | **D** — 无隐藏路由 `event/:id/overview` |

---

## 4. API 与页面对照缺口

| 缺口 | 依据 |
|------|------|
| 页面无 API | dashboard/index.vue 无任何 import from `@/api` |
| API 无页面 | BasketPrivateFileController — 被各页引用，无独立菜单 |
| 菜单无后端 | `basket:ranking:publish`, `basket:dataSettlement:confirm` (sys_menu F 型) |
| 后端无前端 | LocalDemoDataController — `@Profile(local)` 开发专用 |

---

## 5. Web 端重叠（只读）

| 实体 | Web | Admin | 结论 |
|------|-----|-------|------|
| basket_member_account | 注册/登录 | 无管理页 | 不重复开发 |
| basket_player_cert_application | 申请 | 审核 | 共享表，正确 |
| basket_team | 创建 | 审核 | 共享表，正确 |
| basket_tournament_registration | 提交 | 审核 | 共享表；Admin approve 链路不完整 |

---

*详见 [`cnpa-admin-current-audit.md`](./cnpa-admin-current-audit.md)*
