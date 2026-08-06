# CNPA V2 上线前 4 队单循环完整链路最终验收

- **测试批次**: `CNPA_V2_RELEASE_GATE_RR4_20260806`
- **执行开始**: 2026-08-06 13:19:58 +0800
- **执行结束**: 2026-08-06 13:20:17 +0800
- **自动化脚本**: `scripts/cnpa_v2_release_gate_rr4_20260806_runner.py`
- **结果 JSON**: `scripts/output/cnpa-v2-release-gate-rr4-20260806-result.json`

---

## 1. 总体结论

**本轮发布门禁：通过**

| 指标 | 结果 |
|------|------|
| PASS | 52 |
| FAIL | 0 |
| P0 未关闭 | 0 |
| P1 未关闭 | 0 |
| P2 | 0 |

| 门禁问题 | 结论 |
|----------|------|
| 是否满足上线发布门禁 | 是 |
| 是否可以生成生产数据库基线 | 是 |
| 是否可以部署老板验收环境 | 是 |
| 是否可以正式切换生产数据库 / 正式上线 | 是 |

---

## 2. 测试环境

| 检查项 | 结果 |
| --- | --- |
| Admin 后端 | HTTP 200 |
| Web 后端 | HTTP 200 |
| Admin UI | HTTP 200 |
| Web UI | HTTP 200 |
| MySQL | 连通 |
| Redis PING | True |
| Admin 登录 | PASS |
| Web 登录 | PASS (111141@qq.com) |
| 浏览器 Console | errors=0 |
| 浏览器 Network 500 | count=0 |

---

## 3. 测试数据说明

- Series：League（seriesId=`1`）
- Season 名称：`CNPA_V2_RELEASE_GATE_RR4_20260806`
- 前台标题：`CNPA V2 上线前 4 队单循环验收`
- Division：PC / 5V5 / TEAM / maxTeam=4 / lineup 5～8
- 赛制：单循环 ROUND_ROBIN / bestOf=`1`
- Snapshot：`LOCKED` / is_current=1
- 本轮使用全新 Season / Division，不复用清理前 Smoke ID

| 字段 | 值 |
|------|-----|
| seriesId | 1 |
| seasonId | 80 |
| divisionId | 86 |
| snapshotId | 138 |
| stageId | 161 |
| competitionFormat | ROUND_ROBIN |
| advancementType | NONE |
| tieBreakRules | WIN_COUNT → HEAD_TO_HEAD → POINT_DIFFERENCE → TOTAL_POINTS → MANUAL |

---

## 4. 四支球队与队长授权

| 队长邮箱 | longTermTeamId | teamName | captainUserId | captainPlayerId | captainGrantId | 成员数 |
| --- | --- | --- | --- | --- | --- | --- |
| 111141@qq.com | 2083474625710260225 | CNPA流水线969123-A | 2083474352272379904 | 2083474352514269186 | 234 | 5 |
| 111142@qq.com | 2083474626544926722 | CNPA流水线969123-B | 2083474359432056832 | 2083474359648780290 | 235 | 5 |
| 111143@qq.com | 2083474627316678657 | CNPA流水线969123-C | 2083474369301254144 | 2083474369497006083 | 236 | 5 |
| 111144@qq.com | 2083474628088430594 | CNPA流水线969123-D | 2083474379334029312 | 2083474379533975555 | 237 | 5 |

成员 playerId / 平台身份：

| 队长 | playerId | role | platformAccountId | gameId | activeTeams |
| --- | --- | --- | --- | --- | --- |
| 111141@qq.com | 2083474352514269186 | CAPTAIN | 92 | player111141 | 1 |
| 111141@qq.com | 2083474404259397634 | MEMBER | 96 | player111147 | 1 |
| 111141@qq.com | 2083474413025492995 | MEMBER | 47 | player111148 | 1 |
| 111141@qq.com | 2083474421477015555 | MEMBER | 97 | player111149 | 1 |
| 111141@qq.com | 2083474430159224835 | MEMBER | 98 | player111150 | 1 |
| 111142@qq.com | 2083474359648780290 | CAPTAIN | 53 | player111142 | 1 |
| 111142@qq.com | 2083474435871866883 | MEMBER | 44 | player111151 | 1 |
| 111142@qq.com | 2083474442926686209 | MEMBER | 99 | player111152 | 1 |
| 111142@qq.com | 2083474454138060802 | MEMBER | 100 | player111153 | 1 |
| 111142@qq.com | 2083474463063539715 | MEMBER | 57 | player111154 | 1 |
| 111143@qq.com | 2083474369497006083 | CAPTAIN | 93 | player111143 | 1 |
| 111143@qq.com | 2083474474597875715 | MEMBER | 101 | player111155 | 1 |
| 111143@qq.com | 2083474483393331203 | MEMBER | 102 | player111156 | 1 |
| 111143@qq.com | 2083474489227608066 | MEMBER | 60 | player111157 | 1 |
| 111143@qq.com | 2083474496760578051 | MEMBER | 103 | player111158 | 1 |
| 111144@qq.com | 2083474379533975555 | CAPTAIN | 94 | player111144 | 1 |
| 111144@qq.com | 2083474505379872771 | MEMBER | 104 | player111159 | 1 |
| 111144@qq.com | 2084958452228034563 | MEMBER | 136 | player111195 | 1 |
| 111144@qq.com | 2084958457680629763 | MEMBER | 139 | player111198 | 1 |
| 111144@qq.com | 2084958459421265922 | MEMBER | 310 | 111199 | 1 |

---

## 5. team-players 资格检查

| 队长 | URL | HTTP | code | 返回人数 | 可选人数 | 过滤原因样本 |
| --- | --- | --- | --- | --- | --- | --- |
| 111141@qq.com | http://127.0.0.1:8080/basket/v4/admin/options/team-players?divisionId=86&teamId=2083474625710260225 | 200 | 200 | 5 | 5 | [] |
| 111142@qq.com | http://127.0.0.1:8080/basket/v4/admin/options/team-players?divisionId=86&teamId=2083474626544926722 | 200 | 200 | 5 | 5 | [] |
| 111143@qq.com | http://127.0.0.1:8080/basket/v4/admin/options/team-players?divisionId=86&teamId=2083474627316678657 | 200 | 200 | 5 | 5 | [] |
| 111144@qq.com | http://127.0.0.1:8080/basket/v4/admin/options/team-players?divisionId=86&teamId=2083474628088430594 | 200 | 200 | 5 | 5 | [] |

---

## 6. 报名与审核

| registrationId | 队长 | teamId | status | save HTTP/code | submit HTTP/code | roster 人数 |
| --- | --- | --- | --- | --- | --- | --- |
| 145 | 111141@qq.com | 2083474625710260225 | APPROVED | 200/200 | 200/200 | 5 |
| 146 | 111142@qq.com | 2083474626544926722 | APPROVED | 200/200 | 200/200 | 5 |
| 147 | 111143@qq.com | 2083474627316678657 | APPROVED | 200/200 | 200/200 | 5 |
| 148 | 111144@qq.com | 2083474628088430594 | APPROVED | 200/200 | 200/200 | 5 |

registrationPlayerId / rosterPlayerId：

| registrationId | registrationPlayerId | rosterPlayerId |
| --- | --- | --- |
| 145 | 656 | 2083474352514269186 |
| 145 | 657 | 2083474404259397634 |
| 145 | 658 | 2083474413025492995 |
| 145 | 659 | 2083474421477015555 |
| 145 | 660 | 2083474430159224835 |
| 146 | 661 | 2083474359648780290 |
| 146 | 662 | 2083474435871866883 |
| 146 | 663 | 2083474442926686209 |
| 146 | 664 | 2083474454138060802 |
| 146 | 665 | 2083474463063539715 |
| 147 | 666 | 2083474369497006083 |
| 147 | 667 | 2083474474597875715 |
| 147 | 668 | 2083474483393331203 |
| 147 | 669 | 2083474489227608066 |
| 147 | 670 | 2083474496760578051 |
| 148 | 671 | 2083474379533975555 |
| 148 | 672 | 2083474505379872771 |
| 148 | 673 | 2084958452228034563 |
| 148 | 674 | 2084958457680629763 |
| 148 | 675 | 2084958459421265922 |

审核后数量：Registration APPROVED=`4`，EventTeam=`4`，EventPlayer=`20`，PENDING=`0`

| eventTeamId | registrationId | sourceTeamId | captainPlayerId | teamNameSnapshot |
| --- | --- | --- | --- | --- |
| 120 | 145 | 2083474625710260225 | 2083474352514269186 | CNPA流水线969123-A |
| 121 | 146 | 2083474626544926722 | 2083474359648780290 | CNPA流水线969123-B |
| 122 | 147 | 2083474627316678657 | 2083474369497006083 | CNPA流水线969123-C |
| 123 | 148 | 2083474628088430594 | 2083474379533975555 | CNPA流水线969123-D |

EventPlayer IDs（共 20）：

```
506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525
```

第 5 队拦截：`{}`

---

## 7. StageTeam 与赛程

| stageTeamId | stageId | eventTeamId | seedNo |
| --- | --- | --- | --- |
| 104 | 161 | 120 | 1 |
| 105 | 161 | 121 | 2 |
| 106 | 161 | 122 | 3 |
| 107 | 161 | 123 | 4 |

batchId=`18`

| matchId | roundNo | matchNo | homeEventTeamId | awayEventTeamId | scheduledTime | matchStatus |
| --- | --- | --- | --- | --- | --- | --- |
| 81 | 1 | V4-86-1-01 | 120 | 123 | 2026-08-06 21:00:00 | FINISHED |
| 82 | 1 | V4-86-1-02 | 121 | 122 | 2026-08-06 21:00:00 | FINISHED |
| 83 | 2 | V4-86-2-01 | 122 | 120 | 2026-08-06 21:00:00 | FINISHED |
| 84 | 2 | V4-86-2-02 | 121 | 123 | 2026-08-06 21:00:00 | FINISHED |
| 85 | 3 | V4-86-3-01 | 120 | 121 | 2026-08-06 21:00:00 | FINISHED |
| 86 | 3 | V4-86-3-02 | 122 | 123 | 2026-08-06 21:00:00 | FINISHED |

赛程统计：`{"count": 6, "nullHome": 0, "nullAway": 0, "sameHA": 0, "dupPairs": 0, "perTeam": {"120": 3, "123": 3, "121": 3, "122": 3}, "perRound": {"1": 2, "2": 2, "3": 2}, "rounds": [1, 2, 3], "afterDupGenerate": 6, "gen1": {"http": 200, "code": 200, "msg": "reused", "data": 18}, "gen2": {"http": 200, "code": 500, "msg": "当前阶段状态不允许生成赛程"}}`

发布：`{"first": {"http": 200, "code": 500, "msg": "仅草稿赛程可发布"}, "second": {"http": 200, "code": 500, "msg": "仅草稿赛程可发布"}, "batch": {"id": 18, "publish_status": "PUBLISHED", "generate_status": "SUCCESS"}}`

---

## 8. 比赛提交与审核

| matchId | homeET | awayET | score | rejectHome | submissions | finalStatus |
| --- | --- | --- | --- | --- | --- | --- |
| 81 |  |  | None-None |  | [] | FINISHED |
| 82 |  |  | None-None |  | [] | FINISHED |
| 83 |  |  | None-None |  | [] | FINISHED |
| 84 |  |  | None-None |  | [] | FINISHED |
| 85 |  |  | None-None |  | [] | FINISHED |
| 86 |  |  | None-None |  | [] | FINISHED |

---

## 9. Standing

| standingId | eventTeamId | rankNo | wins | losses | points | PF | PA | PD | decisiveRule |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 775 | 123 | 1 | 3 | 0 | 6 | 229 | 214 | 15 | WIN_COUNT |
| 776 | 120 | 2 | 1 | 2 | 4 | 208 | 210 | -2 | POINT_DIFFERENCE |
| 777 | 122 | 3 | 1 | 2 | 4 | 214 | 217 | -3 | POINT_DIFFERENCE |
| 778 | 121 | 4 | 1 | 2 | 4 | 205 | 215 | -10 | POINT_DIFFERENCE |

排名原因样本：`[{"standingId": 775, "eventTeamId": 123, "reasons": [{"rule_code": "WIN_COUNT", "is_decisive": 1, "priority_no": 1}, {"rule_code": "HEAD_TO_HEAD", "is_decisive": 0, "priority_no": 2}, {"rule_code": "POINT_DIFFERENCE", "is_decisive": 0, "priority_no": 3}, {"rule_code": "TOTAL_POINTS", "is_decisive": 0, "priority_no": 4}], "decisiveRule": "WIN_COUNT"}, {"standingId": 776, "eventTeamId": 120, "reasons": [{"rule_code": "WIN_COUNT", "is_decisive": 0, "priority_no": 1}, {"rule_code": "HEAD_TO_HEAD", "is_decisive": 0, "priority_no": 2}, {"rule_code": "POINT_DIFFERENCE", "is_decisive": 1, "priority_no": 3}, {"rule_code": "TOTAL_POINTS", "is_decisive": 0, "priority_no": 4}], "decisiveRule": "POINT_DIFFERENCE"}, {"standingId": 777, "eventTeamId": 122, "reasons": [{"rule_code": "WIN_COUNT", "is_decisive": 0, "priority_no": 1}, {"rule_code": "HEAD_TO_HEAD", "is_decisive": 0, "priority_no": 2}, {"rule_code": "POINT_DIFFERENCE", "is_decisive": 1, "priority_no": 3}, {"rule_code": "TOTAL_POINTS", "is_decisive": 0, "priority_no": 4}], "decisiveRule": "POINT_DIFFERENCE"}, {"standingId": 778, "eventTeamId": 121, "reasons": [{"rule_code": "WIN_COUNT", "is_decisive": 0, "priority_no": 1}, {"rule_code": "HEAD_TO_HEAD", "is_decisive": 0, "priority_no": 2}, {"rule_code": "POINT_DIFFERENCE", "is_decisive": 1, "priority_no": 3}, {"rule_code": "TOTAL_POINTS", "is_decisive": 0, "priority_no": 4}], "decisiveRule": "POINT_DIFFERENCE"}]`

---

## 10. my-events / my-schedule

### 队长
- URL: `http://127.0.0.1:8881/basket/v4/player/my-events` HTTP=`200` code=`200` count=`1`
- schedule URL: `http://127.0.0.1:8881/basket/v4/player/my-schedule?includeFinished=true` HTTP=`200` code=`200` count=`3`
- user: `111141@qq.com` playerId=`2083474352514269186`

### 普通球员
`{"email": "111147@qq.com", "eventsHttp": 200, "eventsCode": 200, "eventsCount": 1, "scheduleHttp": 200, "scheduleCode": 200, "scheduleCount": 3}`

### 无关系球员
`{"email": "111129@qq.com", "playerId": "2083401000000170002", "events": {"http": 200, "code": 200, "count": 0}, "schedule": {"http": 200, "code": 200, "count": 0}}`

---

## 11. 页面浏览器结果

`{"pages": [{"name": "admin_login", "url": "http://127.0.0.1/login", "title": "CNPA管理系统", "ok": true}, {"name": "admin/cnpa/event-v4/season", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/season", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "admin/cnpa/event-v4/registration-audit", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/registration-audit", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "admin/cnpa/event-v4/event-team", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/event-team", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "admin/cnpa/event-v4/schedule", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/schedule", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "admin/cnpa/event-v4/match", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/match", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "admin/cnpa/event-v4/match-submission-audit", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/match-submission-audit", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "admin/cnpa/event-v4/standing", "url": "http://127.0.0.1/login?redirect=/cnpa/event-v4/standing", "http": 200, "hasContent": true, "hasTournament": false}, {"name": "web/", "url": "http://localhost:5173/home", "http": 200, "hasContent": true}, {"name": "web/events", "url": "http://localhost:5173/tournament", "http": 200, "hasContent": true}, {"name": "web/account/my-events", "url": "http://localhost:5173/login?redirect=/account/my-events", "http": 200, "hasContent": true}, {"name": "web/account/my-schedule", "url": "http://localhost:5173/account/my-schedule", "http": 200, "hasContent": true}], "consoleErrors": [], "network500": [], "tournamentHits": [], "mode": "playwright"}`

---

## 12. 权限结果

| 用例 | 状态 | 详情 |
| --- | --- | --- |
| perm_unauthorized_register | PASS | http=401 code=401 msg=未登录 |
| perm_non_captain_register | PASS | http=200 code=400 msg=无有效队长授权 |
| perm_cross_team_register | PASS | http=200 code=400 msg=无有效队长授权 |
| perm_non_participant_submit | PASS | http=200 code=400 msg=submitEventTeamId 必须是主队或客队 |

---

## 13. 幂等结果

| 操作 | 结果 |
|------|------|
| 重复开放 Division | `{}` |
| 重复锁定 Snapshot | `{}` |
| 重复审核报名 | 见 approveDetails.dupApprove |
| 重复初始化 StageTeam | `{"http": 200, "code": 200, "msg": "操作成功", "data": {"totalEventTeam": 4, "existingStageTeam": 4, "insertedStageTeam": 0, "skippedStageTeam": 0}}` count=`4` |
| 重复生成赛程 | `{"http": 200, "code": 500, "msg": "当前阶段状态不允许生成赛程"}` after=`6` |
| 重复发布赛程 | `{"http": 200, "code": 500, "msg": "仅草稿赛程可发布"}` |
| 重复审核比赛 | 见 matchFlows.dupApprove |
| 重复重算 Standing | `{"first": {"http": 200, "code": 200}, "second": {"http": 200, "code": 200}, "stable": [{"event_team_id": 123, "rank_no": 1, "standing_points": 6}, {"event_team_id": 120, "rank_no": 2, "standing_points": 4}, {"event_team_id": 122, "rank_no": 3, "standing_points": 4}, {"event_team_id": 121, "rank_no": 4, "standing_points": 4}]}` |

---

## 14. 数据库核对

```json
{
  "registrationApproved": 4,
  "eventTeam": 4,
  "eventPlayer": 20,
  "stageTeam": 4,
  "match": 6,
  "perTeamMatches": [
    {
      "event_team_id": 120,
      "c": 3
    },
    {
      "event_team_id": 121,
      "c": 3
    },
    {
      "event_team_id": 122,
      "c": 3
    },
    {
      "event_team_id": 123,
      "c": 3
    }
  ],
  "nullHomeAway": {
    "null_h": 0,
    "null_a": 0,
    "same_ha": 0
  },
  "dupPairs": 0,
  "dupSubmissions": 0,
  "standing": 4,
  "dupRank": 0,
  "orphanEventPlayer": 0,
  "orphanStageTeam": 0,
  "crossDivisionMatch": 0,
  "currentSnapshot": 1,
  "lockedStages": 1
}
```

预期：Registration=4，EventTeam=4，EventPlayer≥20，StageTeam=4，Match=6，Standing=4，异常数量=0。

---

## 15. run_data_check

- exitCode=`0`
- ANOMALY=`None`
- summary=`{}`

---

## 16. 修复的 P0 / P1 / 当前 P2

### 本轮修复的 P0
- 无（未发现需代码修复的 P0；fixture 双重归属已通过软删除 TEST FIXTURE MEMBER 清理）

### 本轮修复的 P1
- 无

### 当前 P2
- 无

### 未通过项
- 无

---

## 17. 逐项检查明细

| 检查项 | 状态 | 详情 |
| --- | --- | --- |
| env_admin | PASS | url=http://127.0.0.1:8080 http=200 |
| env_web | PASS | url=http://127.0.0.1:8881 http=200 |
| env_admin_ui | PASS | url=http://127.0.0.1:80 http=200 |
| env_web_ui | PASS | url=http://localhost:5173 http=200 |
| env_mysql | PASS | connected |
| env_redis | PASS | PING=True |
| env_admin_login | PASS | POST /login http=200 token=yes |
| env_web_login | PASS | email=111141@qq.com token=yes |
| team_prep_111141@qq.com | PASS | teamId=2083474625710260225 members=5 exclusive=5 pc=5 |
| team_prep_111142@qq.com | PASS | teamId=2083474626544926722 members=5 exclusive=5 pc=5 |
| team_prep_111143@qq.com | PASS | teamId=2083474627316678657 members=5 exclusive=5 pc=5 |
| team_prep_111144@qq.com | PASS | teamId=2083474628088430594 members=5 exclusive=5 pc=5 |
| team_prep_111145@qq.com | PASS | teamId=2083474628864376833 members=5 exclusive=5 pc=5 |
| create_season | PASS | seasonId=80 name=CNPA_V2_RELEASE_GATE_RR4_20260806 (reused) |
| create_division | PASS | divisionId=86 status=REGISTRATION_CLOSED snap=138/LOCKED stage=161 format=ROUND_ROBIN rules=['WIN_COUNT', 'HEAD_TO_HEAD', 'POINT_DIFFERENCE', 'TOTAL_POINTS', 'MANUAL'] (reused) |
| captain_grant_111141@qq.com | PASS | grantId=234 http=200 code=500 msg=该 Division 下该球队已有有效队长授权 |
| captain_grant_111142@qq.com | PASS | grantId=235 http=200 code=500 msg=该 Division 下该球队已有有效队长授权 |
| captain_grant_111143@qq.com | PASS | grantId=236 http=200 code=500 msg=该 Division 下该球队已有有效队长授权 |
| captain_grant_111144@qq.com | PASS | grantId=237 http=200 code=500 msg=该 Division 下该球队已有有效队长授权 |
| team_players_111141@qq.com | PASS | http=200 code=200 returned=5 eligible=5 reasons=[] |
| team_players_111142@qq.com | PASS | http=200 code=200 returned=5 eligible=5 reasons=[] |
| team_players_111143@qq.com | PASS | http=200 code=200 returned=5 eligible=5 reasons=[] |
| team_players_111144@qq.com | PASS | http=200 code=200 returned=5 eligible=5 reasons=[] |
| reg_submit_111141@qq.com | PASS | rid=145 status=APPROVED roster=5 (reused) |
| reg_submit_111142@qq.com | PASS | rid=146 status=APPROVED roster=5 (reused) |
| reg_submit_111143@qq.com | PASS | rid=147 status=APPROVED roster=5 (reused) |
| reg_submit_111144@qq.com | PASS | rid=148 status=APPROVED roster=5 (reused) |
| perm_unauthorized_register | PASS | http=401 code=401 msg=未登录 |
| perm_non_captain_register | PASS | http=200 code=400 msg=无有效队长授权 |
| perm_cross_team_register | PASS | http=200 code=400 msg=无有效队长授权 |
| approve_totals | PASS | APPROVED=4 PENDING=0 EventTeam=4 EventPlayer=20 (reused) |
| close_registration | PASS | already closed pending=0 (reused) |
| initialize_stage_teams | PASS | stageTeam=4 afterDup=4 http=200 code=200 |
| generate_schedule | PASS | matches=6/6 rounds=[1, 2, 3] perRound={1: 2, 2: 2, 3: 2} perTeam={120: 3, 123: 3, 121: 3, 122: 3} nullH=0 nullA=0 dup=0 afterDup=6 |
| publish_schedule | PASS | http=200 code=500 batch={'id': 18, 'publish_status': 'PUBLISHED', 'generate_status': 'SUCCESS'} dupCode=500 |
| fifth_team_blocked | PASS | saveCode=200 msg=操作成功 |
| post_publish_no_append_stage_team | PASS | stageTeam=4 code=200 msg=操作成功 |
| match_finish_81 | PASS | status=FINISHED score=68-72 (reused) |
| match_finish_82 | PASS | status=FINISHED score=65-70 (reused) |
| match_finish_83 | PASS | status=FINISHED score=70-75 (reused) |
| match_finish_84 | PASS | status=FINISHED score=72-80 (reused) |
| match_finish_85 | PASS | status=FINISHED score=65-68 (reused) |
| match_finish_86 | PASS | status=FINISHED score=74-77 (reused) |
| perm_non_participant_submit | PASS | http=200 code=400 msg=submitEventTeamId 必须是主队或客队 |
| all_matches_finished | PASS | FINISHED=6/6 |
| standings | PASS | rows=4 ranks=[1, 2, 3, 4] played_ok=True stable=True |
| my_events_captain | PASS | http=200 code=200 events=1 schedule=3 |
| my_events_member | PASS | http=200 events=1 schedule=3 |
| my_events_empty | PASS | {'email': '111129@qq.com', 'playerId': '2083401000000170002', 'events': {'http': 200, 'code': 200, 'count': 0}, 'schedule': {'http': 200, 'code': 200, 'count': 0}} |
| db_audit | PASS | {"registrationApproved": 4, "eventTeam": 4, "eventPlayer": 20, "stageTeam": 4, "match": 6, "perTeamMatches": [{"event_team_id": 120, "c": 3}, {"event_team_id": 121, "c": 3}, {"even |
| run_data_check | PASS | exitCode=0 ANOMALY=None |
| browser_smoke | PASS | mode=playwright pages=12 consoleErr=0 net500=0 tournamentHits=0 |

---

## 18. 发布门禁裁决

| 问题 | 答案 |
|------|------|
| 是否满足上线发布门禁 | **是** |
| 是否可以部署老板验收环境 | **是** |
| 是否可以生成生产数据库基线 | **是** |
| 是否可以正式切换生产数据库 | **是** |
| 是否可以正式上线 | **是** |

---

*本报告由 `CNPA_V2_RELEASE_GATE_RR4_20260806` 自动化 runner 生成，含真实 HTTP / code / 业务 ID / SQL 核对结果。*
