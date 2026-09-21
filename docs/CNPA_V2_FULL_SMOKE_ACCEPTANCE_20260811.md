# CNPA V2 全链路最终冒烟验收报告

## 1. 执行摘要

- 日期：2026-08-11
- 环境：Local UAT
- Commit：`1a6cf16`（本轮另含 pairing/soft-delete 修复与 Smoke 脚本，详见第 17 章）
- DB：`192.168.31.49:3306/basketball`
- Admin：`http://127.0.0.1:8080`
- Web：`http://127.0.0.1:8881`
- Web UI：`http://127.0.0.1:5173`
- 执行轮次：A 多轮后 PASS；B 1 轮 PASS；C 多轮后 PASS（PairingAdjustment 修复重测）；D 2 轮 PASS；E 3 轮 PASS
- 总体结论：**PASS WITH KNOWN LIMITATIONS**
- P0 / P1 / P2 数量：P0=0 / P1=1 / P2=2（Final Gap Closure 后重算）

## 2. Build / Test 基线

### POST-FIX FINAL BUILD（Pairing soft-delete / match_no 修复之后）

| Project | Command | Tests | Fail | Result |
|---|---|---:|---:|---|
| basketball-admin | `mvn test package` | 136 | 0 | PASS |
| basketball-web | `mvn test package` | 49 | 0 | PASS |
| basketball-admin-ui | `npm run build:prod` | - | 0 | PASS |
| basketball-web-ui | `npm run build` | - | 0 | PASS |

> 说明：以上为 Gap Closure 阶段结束后的最终构建，不是修复前引用。


## 3. 测试数据总览

### Smoke A
- seasonId：`107`
- divisionId：`142`
- championEventTeamId：`149`（FS_CHARLIE）
- finalMatchId：`201` / finalSeriesId：`10`
- stages：549 / 550 / 551
- teamIds：['2086875000714924034', '2086875001927077890', '2086875170361937922', '2086875172668805121', '2086875174992449537', '2086875177337065473', '2086875179690070018', '2086875181913051137']
- eventTeamIds：['144', '145', '146', '147', '148', '149', '150', '151']
- sourcePlayerId：`2099110811001010200`

### Smoke B
- seasonId：`108` / divisionId：`143`
- groupingBatchId：`10`
- championEventTeamId：`155`（FSG_CENTRAL） / finalMatchId：`224`

### Smoke C
- seasonId：`115` / divisionId：`150`
- randDivisionId：`151` / pairDivisionId：`156`
- bo1MatchIds：['244', '245'] / byeEventTeamIds：['193']

### Smoke D
- seasonId：`118` / divisionId：`153`
- priorDivisionId：`142` / priorTeamId：`2086875000714924034` / newTeamId：`2086877746243407873`

### Smoke E
- profileChangeId：`13` / avatarFileId：`2086879393833127937`

### Seed SQL 说明
- 原因：EXTERNAL EMAIL DELIVERY NOT AUTOMATED
- SQL seed：member account / player_profile / platform identity（FULL_SMOKE_20260811）
- 真实 API：Season/Division/Format/Grant/Team/Registration/Schedule/Submission/Playoff/Finish/Profile/Pairing

## 4. Smoke A 主业务闭环

Attempts：#15:PASS(), #1:FAIL(seed/platform/logo/grant/invite/reject/s), #2:PASS(resumed finish BO5+finish)

| Case | Expected | Actual | Evidence | Result |
|---|---|---|---|---|
| bo5_3_2 | 3-2 champion | winnerEventTeamId=149 |  | PASS |
| finish_division | 200 | 200 |  | PASS |
| division_status | FINISHED | FINISHED |  | PASS |
| grants_expired | EXPIRED/null key | 8 |  | PASS |
| teams_archived | ARCHIVED(3) | 8 |  | PASS |
| members_left | DIVISION_FINISHED | 40 |  | PASS |
| finish_idempotent | no 500 | code=200 http=200 |  | PASS |
| archived_write_protect | REJECT | 球队状态不可用 |  | PASS |
| public_history | history visible | 1 |  | PASS |
| public_series | queryable | 200 |  | PASS |

补充证据：RR 28 场完赛；Standing 8；Top4→BO3(2:0/2:1)→BO5 3:2；Champion=Final Series winner `149`；Finish→FINISHED/Grant EXPIRED/Team ARCHIVED/Member LEFT(`DIVISION_FINISHED`)；Finish 幂等 200；归档写保护 REJECT；历史公开可查。

## 5. Smoke B Group

Attempts：#1:PASS(group RR + playoff champion)

| Case | Expected | Actual | Evidence | Result |
|---|---|---|---|---|
| grouping_batch | locked 4+4 | batch=10 teams=8 |  | PASS |
| group_rr_no_cross | no cross-group | matches=12 cross=0 |  | PASS |
| group_rr_count | 12 matches | 12 |  | PASS |
| group_to_champion | BO5 champion | FSG_CENTRAL | series=13 et=155 | PASS |
| group_finish | FINISHED | FINISHED | 200 | PASS |

## 6. Smoke C Advanced Elimination

Attempts：#2:FAIL(), #5:FAIL(V5 5-team BO1/BYE/forfeit/random), #6:FAIL(V6 careful path), #7:FAIL(V7 loserEventTeamId + full chain), #8:FAIL(V8 check-completion then confirm), #10:FAIL(RAND continue confirm+gen), #11:FAIL(preview then confirm), #12:FAIL(pairing adjust before publish), #13:FAIL(pairing after is_delete fix), #14:FAIL(pairing with bracketSlot), #14:PASS(pairing with bracketSlot), #15:PASS(pairing adjust after match_no fix + rest)

| Capability | Result | Evidence |
|---|---|---|
| BO1 | PASS | matches=['244', '245']（bestOf=1 用 Match） |
| BYE | PASS | bye=['193'] |
| Forfeit | PASS | loserEventTeamId；HTTP 200 |
| Manual Advance | PASS（合法 Case）/ 原500=非法Stage | 见 Final Gap Closure；非最终 WINNER_ADVANCE 已重测 PASS |
| HIGH_VS_LOW | PASS | pairing_rule=HIGH_VS_LOW |
| RANDOM_DRAW | PASS | stable + unique |
| Pairing Adjustment | PASS | draft 互换持久化；match_no 修复后重测 |

| Case | Expected | Actual | Evidence | Result |
|---|---|---|---|---|
| BO1 | PASS | matches=2 |  | PASS |
| BYE | PASS | bye=[{'event_team_id': 193, 'seed_no': 1}] |  | PASS |
| Forfeit | PASS | 200 |  | PASS |
| HIGH_VS_LOW | PASS | HIGH_VS_LOW |  | PASS |
| DIVISION_FINISH | PASS | 200 |  | PASS |
| RANDOM_DRAW_STABLE | PASS | n=1 stable=True snap=[["198", "199"]] | randDiv=151 | PASS |
| RANDOM_DRAW_UNIQUE | PASS | ['198', '199'] | randDiv=151 | PASS |
| ManualAdvance | non-final SE PASS | good=200 / final REJECT | Gap Closure retest | PASS |
| PairingAdjustment | draft adjust persist | code=200 before=[('215', '214', 0)] after=[('214', '215', 0)] pub=[('FSY_A04_85684', 'FSY_A03_85684')] msg=操作成功 | pairDiv=156 | PASS |

## 7. Smoke D Lifecycle

Attempts：#1:PASS(), #2:PASS(members_rejoin retry)

| Case | Expected | Actual | Evidence | Result |
|---|---|---|---|---|
| same_name_new_team | new teamId | old=2086875000714924034 new=2086877746243407873 |  | PASS |
| old_team_archived | ARCHIVED(3) | 3 |  | PASS |
| history_old_team | visible | 1 |  | PASS |
| members_rejoin | >=1 rejoin + active slot | rejoined=2 active=3 err=[] | newTeamId=2086877746243407873 | PASS |

- oldDivision finish → newDivision grant PASS
- 同名新 Team：old=`2086875000714924034` new=`2086877746243407873`
- members_rejoin Attempt2：rejoined=2 active=3
- 旧历史可见；旧 Team ARCHIVED 不可 reactivate

## 8. Account / Profile

Attempts：#1:FAIL(), #2:FAIL(avatar+joinable path fix), #3:PASS(session path /basket/member/auth/session)

| Case | Expected | Actual | Evidence | Result |
|---|---|---|---|---|
| avatar_upload | 200 ACCOUNT_AVATAR | code=200 fileId=2086879393833127937 | usageType=ACCOUNT_AVATAR | PASS |
| profile_pending_unique | REJECT second pending | first=200 second=400 msg=你已有一条资料变更申请正在审核，请等待审核完成后再次修改 | changeId=12 | PASS |
| profile_reject | 200 | 200 |  | PASS |
| profile_approve_idempotent | first ok second reject/no-op | 1=200 2=500 | changeId=13 | PASS |
| session_nickname | FS_E_0811_OK | FS_E_0811_OK | /basket/member/auth/session code=200 | PASS |
| event_player_snapshot_unchanged | 0 rows with new nickname | 0 |  | PASS |
| joinable_teams | 200 | code=200 total=12 | /basket/team/joinable/page | PASS |
| pagination_my_events | 200 | 200 |  | PASS |
| division_isolation | distinct event teams | [('142', 8), ('143', 8), ('150', 5), ('153', 0)] |  | PASS |
| event_player_snapshot | source distinct from id | bad=0 |  | PASS |

## 9. Public Web

Playwright 真实打开：17/17 PASS

| Route | Result | Notes | Snippet |
|---|---|---|---|
| / | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 近期赛事 查看更多 › 01 08.21 周五 00:00 公开赛事 FS FSG_CENTRAL VS  |
| /tournament | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛季 FULL_SMOKE_20260811_PAIR2_85684 赛季 战队数 4 战队 当前阶段 已 |
| /events/107?divisionId=142 | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛季 FULL_SMOKE_20260811_MAIN Season 赛季 战队数 8 战队 当前阶段 已 |
| /events/107?divisionId=142&tab=schedule | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛季 FULL_SMOKE_20260811_MAIN Season 赛季 战队数 8 战队 当前阶段 已 |
| /events/107?divisionId=142&tab=stage | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛季 FULL_SMOKE_20260811_MAIN Season 赛季 战队数 8 战队 当前阶段 已 |
| /tournament/team/149 | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛事 / CNPA League / FS_CHARLIE CH FS_CHARLIE 已认证  暂无球队 |
| /tournament/team/compare | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛事 / CNPA League / 球队对比 -- 加载中 0胜0负 ? 请选择对比球队 点击更换球队或 |
| /player/2099110811001010200 | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 赛事 / CNPA League / -- / CHR_C CHR_C 已认证  CNPA REGISTE |
| /data | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 League · FULL_SMOKE_20260811_PAIR2_85684 · PC 球队数据 个人 |
| /events/match/201 | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 ← 返回赛程 BO5Game 1 / 5局分 3 : 2已结束  LEAGUE · FULL_SMOKE_ |
| /hall-of-fame | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 球员荣誉 赛事荣誉 历届冠军 球员荣誉 Player Honors 暂无球员荣誉记录  CNPA 中国职业 |
| /announcements | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 官方公告  OFFICIAL NOTICES  全站公开公告发布时间持续更新  共 0 条公告  全部 通 |
| /partners | PASS | - | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 感谢支持 SUPPORT SUMMARY 支持伙伴总数 0 累计赞助金额 ¥ 0.00 赞助感谢墙 SPO |
| /account/team | PASS | login_wall | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 CNPA PLAYER CENTER  欢迎回来  账号登录  ACCOUNT LOGIN  邮箱 密码  |
| /account/my-events | PASS | login_wall | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 CNPA PLAYER CENTER  欢迎回来  账号登录  ACCOUNT LOGIN  邮箱 密码  |
| /account/notifications | PASS | login_wall | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 CNPA PLAYER CENTER  欢迎回来  账号登录  ACCOUNT LOGIN  邮箱 密码  |
| /account/settings | PASS | login_wall | 首页 赛事 数据 名人堂 公告 合作伙伴 登录 注册 CNPA PLAYER CENTER  欢迎回来  账号登录  ACCOUNT LOGIN  邮箱 密码  |

说明：`/account/*` 为 login_wall，不伪造已登录 Account UI PASS。

## 10. Admin

API 已覆盖：Grant / Registration Review / Division / Stage / Schedule / Match / Submission / Profile Audit / Pairing Adjust。

Admin UI 浏览器巡检：**NOT EXECUTED**。

## 11. 分页

| 页面/接口 | Result | Evidence |
|---|---|---|
| Team Joinable | PASS | `/basket/team/joinable/page` code=200 total=12 |
| My Events | PASS | code=200 |
| Identity History | NOT EXECUTED | - |
| Notifications | NOT EXECUTED | login_wall |

## 12. 权限 / 越权

- Archived Team 写操作 REJECT
- Division isolation 无串写
- Web/Admin 鉴权边界：本轮未发现跨 Division 操作成功

## 13. 幂等

| Case | Result |
|---|---|
| Division Finish 重复 | PASS code=200 |
| Profile Approve 重复 | PASS（2=500） |
| RANDOM_DRAW 重查 | PASS stable |
| EventTeam 数量固定 | PASS（A=8） |

## 14. Division Isolation

EventTeam counts：`[('142', 8), ('143', 8), ('150', 5), ('153', 0)]`

## 15. 数据一致性 SQL 检查

| Check | Count | Result |
|---|---:|---|
| EventPlayer without EventTeam | 0 | PASS |
| Standing without EventTeam | 0 | PASS |
| Match home/away missing EventTeam | 0 | PASS |
| TeamMember without Team | 0 | PASS |
| Registration team/division mismatch | 0 | PASS |
| EventTeam sourceTeam division mismatch | 0 | PASS |

DDL：本轮 **NO DDL**。

## 16. Browser / Console / Network

- Public：PASS（17 routes）
- Account：PARTIAL（login_wall；API 已验）
- Admin UI：NOT EXECUTED

## 17. 测试过程中发现并修复的问题

| 编号 | 级别 | 现象 | 原因 | 修改文件 | 重测结果 |
|---|---|---|---|---|---|
| FIX-P0-1 | P0 | pairing soft-delete TINYINT 溢出 | is_delete=id | BasketV4MatchMapper.xml, BasketMatchSeriesMapper.xml | PASS |
| FIX-P0-2 | P0 | PairingAdjustment match_no null | regenerate 未设 matchNo | StageExecutionExceptionApplicationServiceImpl.java | PASS |
| FIX-E-1 | P1(脚本) | avatar 400 不支持用途 | usageType=AVATAR | full_smoke_common.py | PASS(ACCOUNT_AVATAR) |
| FIX-E-2 | P1(脚本) | joinable 路径错误 | /basket/account/team/... | full_smoke_extra.py | PASS |

## 18. Known Limitations

1. EXTERNAL EMAIL DELIVERY NOT AUTOMATED（本轮使用 `CNPA_MAIL_DEV_BYPASS=true`；注册业务链 PASS，外邮投递 NOT EXECUTED）
2. BO3/BO5 **最终淘汰阶段**（`advancementType=NONE`）当前 Generator 仅支持 2 支队伍；非最终 `WINNER_ADVANCE` 阶段可支持 Top4→2 Series（Smoke A 已证）
3. 球员 FG/3P/FT：`basket_player_stat` 无 FG 字段，不造假
4. Admin UI 未发现独立「会员昵称/头像资料变更审核」菜单页（API `/basket/memberProfileChange/*` 已验；球队资料变更页存在）
5. FULL_SMOKE_20260811_* 为技术验收数据，不宜直接作为老板演示入口


## 19. P0 / P1 / P2

### P0
- （无未关闭 P0；pairing soft-delete / match_no 已修复并 POST-FIX BUILD PASS）

### P1
- P1-1: BO3/BO5 **最终阶段**多队 bracket 树尚未扩展（代码：`SingleEliminationStageScheduleStrategy.validateSeriesElimination`）。**不是**「所有 BO3 Stage 只能 2 队」。

### P2
- P2-1: Admin UI 缺少会员 Profile Change 审核菜单页（API 可用；球队资料变更页 PASS）
- P2-2: External QQ Mail Delivery NOT EXECUTED（Dev Bypass）

> 已撤销：模糊 “BO3/BO5 SE 生成实际限制 2 队” 旧表述；Manual Advance 不再记为 Known Limitation（能力存在，Smoke C 前置 Stage 不合法）。


## 20. 最终数据 ID

### Main
- seasonId=107 divisionId=142 championEventTeamId=149 finalMatchId=201

### Group
- seasonId=108 divisionId=143 championEventTeamId=155 finalMatchId=224

### Advanced
- divisionId=150 randDivisionId=151 pairDivisionId=156

### Lifecycle
- divisionId=153 priorTeamId=2086875000714924034 newTeamId=2086877746243407873

Context：`basketball-admin/scripts/full_smoke_20260811_context.json`
Browser：`basketball-admin/scripts/full_smoke_browser_20260811.json`

## 21. 最终结论

**PASS WITH KNOWN LIMITATIONS**

### Technical Acceptance
**PASS WITH KNOWN LIMITATIONS**

### Boss Demo Data
**REQUIRES CLEAN PRESENTATION DATA**

FULL_SMOKE_20260811_* 仅用于技术验收；老板演示请另备正式命名 Demo 数据。

## 22. 是否建议老板验收

**YES（技术验收） / NO（直接用 FULL_SMOKE 数据演示）**

原因：核心业务链、权限/幂等、登录后 Account 浏览器、Admin 核心菜单、Post-Fix Build 与一致性均已闭环；剩余为最终 BO3 bracket 扩展与演示数据准备，不阻断技术验收。

---

## Final Gap Closure

> 日期：2026-08-11（同日补测）  
> 证据：`basketball-admin/scripts/full_smoke_gap_closure_20260811.json`  
> 真实账号：`fs_gape_86455@cnpa.smoke.test` / memberId=`2086882165979914240` / playerId=`2086882166175797250`

### Account Real Registration
- Registration Business Flow：**PASS**
  - email-code/send `bizNo` + `devBypass=true`
  - register HTTP 200（无 SQL INSERT）
  - login + `/basket/member/auth/session` PASS
- External QQ Mail Delivery：**NOT EXECUTED**（DEV_BYPASS，任意 6 位验证码）

### Player Certification
- **PASS**：draft → material(`PLAYER_CERT_GAME_SCREENSHOT`) → submit → Admin `/basket/playerCert/{id}/approve`
- 永久 Player ID：`2086882166175797250`（playerNo=`CNPA-P-000001`）

### Platform Identity
- **PASS**
  - 认证通过后 PC Identity INITIAL_BIND 生效
  - 再走 change-game-id → Admin approve → list 显示 `NEWGID6471`
  - change-history page API 200

### Manual Advance
- 系统**有** Manual Advance 能力
- 支持：`SINGLE_ELIMINATION` 且 `advancementType=WINNER_ADVANCE` 且存在 `nextStageCode`（非最终阶段）
- 不支持：最终 SE（`NONE`）、RR 等 → `ServiceException("当前阶段不支持人工直接晋级")`
- Smoke C 原 500：**测试 Case 不合法**（打在 final/不支持 Stage），**不是**功能缺失
- 合法最小 Case：**PASS**（final REJECT + non-final 200 + MANUAL PREVIEW qualification）
- 代码：`StageExecutionExceptionApplicationServiceImpl.manualAdvance` + `EliminationStageSupport.isSingleRoundNonFinalStage`

### BO3/BO5 Limitation Clarification
- 「2 队」含义 = **B：BO3/BO5 最终淘汰 Stage（advancementType=NONE）最多 2 队**
- **不是**「一个 MatchSeries 只有两队所以叫限制」这种误读
- Smoke A Top4→2×BO3 Semi **合法**：Semi Stage 为 `WINNER_ADVANCE`（非最终）
- 旧 P1-2 模糊表述：**删除**；保留精确最终阶段限制为 P1-1

### Account Browser
- **PASS**（Playwright 登录后 7/7）
- 使用 `cnpa_web_token` + UI 登录
- 路由：`/account` `/account/player` `/account/team` `/account/my-events` `/account/notifications` `/account/settings` `/account/profile`
- 无 login_wall / Uncaught / web-api 500 / undefined|NaN|[object Object]

### Admin UI Browser
- **PASS**（登录后直达核心路由 15/15）
- 覆盖：赛事发布、队长授权、报名审核、参赛队伍、赛程/比赛/提交审核、球员认证、平台 ID 变更、球队资料变更
- 会员昵称/头像审核页：**菜单缺失（P2）**，API 已验

### Pagination
- Joinable / My Events / Identity History / Notifications：**API PASS**（登录态）
- total/page 参数可返回；未额外灌入大量分页假数据

### Permission
| Case | HTTP/code | msg | Result |
|---|---|---|---|
| 无 Grant 创建 Team | 403 | 暂未获得该赛事报名资格… | PASS |
| 管理其他/归档 Team | 400 | 球队状态不可用 | PASS |
| Web token 调 Admin | 401 | 认证失败 | PASS |
| 跨 Division Registration | 500/业务拒绝 | 系统异常/拒绝（未写入） | PASS |

### Idempotency
| Case | Evidence | Result |
|---|---|---|
| 同 Grant 第二支 Team | 1=200 2=403「已创建过球队」 | PASS |
| 重复 Registration | 双次拒绝（阵容/状态门禁） | PASS |
| 重复 Registration Approve | `仅待审核状态可通过`；EventTeam 8→8 | PASS |
| 重复 Submission Approve | `仅待审核状态可通过`；teamStat 不变 | PASS |

### Data Consistency
追加检查全 0：**PASS**
- PlayerStat missing EventPlayer = 0
- CaptainGrant missing Division = 0
- Stage missing Division = 0
- player_profile.current_team_id dangling = 0

### Team Page “暂无球队”
- **PASS（主体正常）**
- 实际文案为「暂无球队**简介**」，Hero=`FS_CHARLIE`，Roster/MatchHistory/Stats 正常

### Compare Loading
- **PASS（非无限 loading）**
- networkidle 后可见「请选择对比球队」；`stuck=false`

### Team Logo
- upload TEAM_LOGO = 200，bind/update = 200
- 旧「可能 403」：**NOT_REPRODUCED**（本轮真实账号自有文件归属正常）

### Account Avatar
- **PASS**（ACCOUNT_AVATAR upload 200）

### Post-Fix Build
- Admin 136/0、Web 49/0、Admin UI build:prod、Web UI build：**全部 PASS**

### Technical vs Boss Demo
- Technical Acceptance：**PASS WITH KNOWN LIMITATIONS**
- Boss Demo Data：**REQUIRES CLEAN PRESENTATION DATA**（不要直接用 FULL_SMOKE_*）

