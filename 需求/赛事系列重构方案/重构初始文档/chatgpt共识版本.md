# CNPA 赛事域 V4 全量重构最终共识稿

> 版本：V4 Final Consensus Draft / 2026-08-03  
> 分支：`basketball-20260803`  
> 适用范围：`basketball-admin` / `basketball-web` / `basketball-admin-ui` / `basketball-web-ui`  
> 当前阶段：方案终稿 / 第 0 轮共识收口  
> 重要原则：本稿用于进入第 1 轮开发前最终确认；第 1 轮只做模型、表结构、枚举、基础代码与种子数据，不写复杂业务。

---

## 0. 本版本定位

本稿是在前两轮 ChatGPT 方案与 Cursor 评审意见基础上形成的最终共识稿。

当前已经确认：CNPA 项目仍处于开始阶段，赛事域可以全量重构，不再为旧赛事三层结构保留兼容包袱。

因此，本方案以如下前提为准：

```text
1. 赛事域全量重构。
2. 不迁移旧赛事业务数据。
3. 不兼容旧 competition_season → tournament → tournament_season 三层结构。
4. 不兼容旧 eventId = seasonId、tournamentId 装 seasonId、competition 混用等旧语义。
5. 账号、邮箱登录、球员认证、球员平台身份、长期球队、球队成员、后台权限、公告、合作伙伴、文件能力继续保留。
6. 新赛事域统一使用 Series → Season → Division 作为唯一主线。
7. 当前只开放 TEAM 球队报名。
8. PERSONAL / Open 个人报名只预留并拦截，不开放流程。
9. 第一阶段只做 REGULAR_ROUND_ROBIN 常规赛单循环基础版。
10. GROUP_KNOCKOUT、小组赛晋级、完整季后赛树、BO3 / BO5、种子队、复杂轮空规则后置。
```

本稿不是需求脑图，而是给 Cursor / Codex 执行前使用的工程共识文档。

---

## 1. 已冻结共识

以下内容进入冻结状态，后续开发提示词、表设计、接口设计、前后台页面均以此为准。

### 1.1 业务主线冻结

新赛事域唯一主线为：

```text
Series 赛事系列
  ↓
Season 赛事赛季
  ↓
Division 平台赛区
  ↓
Registration 报名
  ↓
EventTeam 赛事球队快照
  ↓
EventPlayer 赛事球员快照
  ↓
Match 比赛
  ↓
MatchSubmission 比赛数据提交
  ↓
Standing / Stat / Rank 统计榜单
```

业务示例：

```text
Series：League
  ↓
Season：League 2026 S1
  ↓
Division：League 2026 S1 - PS
Division：League 2026 S1 - Xbox
  ↓
PS 赛区独立报名、独立名额、独立赛程、独立排行榜
Xbox 赛区独立报名、独立名额、独立赛程、独立排行榜
```

### 1.2 多平台规则冻结

一个 Season 可以选择多个平台，例如：

```text
PS / Xbox / PC
```

但系统内部必须拆成多个 Division：

```text
League 2026 S1
├── PS Division
├── Xbox Division
└── PC Division
```

每个 Division 独立：

```text
1. 独立报名名额
2. 独立报名审核
3. 独立 EventTeam / EventPlayer 快照
4. 独立赛程
5. 独立 Match
6. 独立比赛数据提交
7. 独立 Standing / Stat / Rank
8. 独立前台平台切换展示
9. 独立个人中心我的赛事 / 我的赛程筛选
```

禁止不同平台之间混排、混榜、混统计。

### 1.3 报名方式冻结

当前只开放：

```text
registration_mode = TEAM
```

当前不开放：

```text
registration_mode = PERSONAL
Open registration
```

处理策略：

```text
1. 表字段和枚举预留 PERSONAL。
2. 后台发布页 PERSONAL 选项置灰或选择时报错。
3. 后端收到 PERSONAL 时返回“个人报名暂未开放”。
4. 旧 Open 个人报名入口不挂新菜单。
5. 旧 Open 个人报名相关代码只作参考，不作为新链路依赖。
```

### 1.4 赛制冻结

第一阶段只实现：

```text
REGULAR_ROUND_ROBIN：常规赛单循环基础版
```

暂不实现：

```text
REGULAR_PLAYOFF 完整季后赛树
GROUP_KNOCKOUT 小组赛 + 淘汰赛
KNOCKOUT 单淘汰完整树
BO3 / BO5
种子队
复杂轮空规则
自动晋级规则
```

第一阶段默认赛程时间规则写死，不建后台配置表：

```text
比赛日：周二 / 周四 / 周六
比赛时间段：21:00 / 22:00
每时间段最多：6 场
每天最多：12 场
```

后续需要后台可配置时，再新增赛程规则配置表。

---

## 2. 重构边界

### 2.1 本次全量重构的赛事域范围

以下模块按新模型重建，不依赖旧赛事链路：

```text
1. 赛事系列 Series
2. 赛事赛季 Season
3. 平台赛区 Division
4. 队长授权 CaptainGrant
5. 球队报名 Registration
6. 报名阵容 RegistrationPlayer
7. 报名审核 RegistrationAudit
8. 赛事球队快照 EventTeam
9. 赛事球员快照 EventPlayer
10. 赛程生成 ScheduleBatch
11. 比赛 Match
12. 比赛节比分 MatchPeriodScore
13. 比赛数据提交 MatchSubmission
14. 提交方球队数据 MatchSubmissionTeamStat
15. 提交方球员数据 MatchSubmissionPlayerStat
16. 提交材料 MatchSubmissionMaterial
17. 单场正式球队数据 MatchTeamStat
18. 单场正式球员数据 MatchPlayerStat
19. 积分榜 Standing
20. 球队赛季统计 TeamStat
21. 球员赛季统计 PlayerStat
22. 排行榜 Rank
23. 后台赛事发布页
24. 后台报名审核页
25. 后台赛程管理页
26. 后台比赛数据审核页
27. 前台赛事列表
28. 前台赛事详情
29. 前台报名入口
30. 前台比赛详情
31. 个人中心我的赛事
32. 个人中心我的赛程
33. 比赛数据提交入口
```

### 2.2 保留的非赛事资产

以下模块保留，不跟随赛事域全量清空：

```text
1. 用户账号
2. 邮箱注册 / 登录 / 验证码
3. 球员认证
4. 球员永久身份
5. 球员平台身份
6. 长期球队
7. 球队成员关系
8. 后台管理员账号
9. 后台角色权限基础能力
10. 文件上传 / 文件存储
11. 公告
12. 合作伙伴
13. 基础字典
14. 系统参数
15. 登录日志 / 操作日志基础能力
16. 全局布局样式和非赛事页面
```

### 2.3 本期明确不做

以下内容本期不实现，只保留扩展空间或删除入口：

```text
1. PERSONAL 个人报名完整流程
2. Open 个人报名完整流程
3. 后台手动组队
4. 系统自动组队
5. 小组赛完整晋级
6. 完整季后赛树
7. 单淘汰完整树
8. BO3 / BO5
9. 种子队
10. 复杂轮空规则
11. 荣誉自动生成
12. 争议处理
13. 异常处理
14. 阵容变更审核
15. 自动排赛 Job
16. 赛程时间规则后台配置表
```

如本期不做的旧入口已存在，应从新菜单、新页面、新路由中移除或隐藏，避免运营误用。

---

## 3. 术语与 ID 规范

### 3.1 固定术语

| 术语 | 含义 | 示例 |
|---|---|---|
| Series | 固定赛事系列 / 赛事品牌 | League / Arena / Cup / Open |
| Season | 某个 Series 下的一届具体赛事 | League 2026 S1 |
| Division | 某个 Season 下的一个平台赛区 | League 2026 S1 - PS |
| Registration | 报名记录 | 某长期球队报名某 Division |
| EventTeam | 正式参赛球队快照 | RNG 在 PS 赛区的赛事队伍快照 |
| EventPlayer | 正式参赛球员快照 | 某球员在 PS 赛区的参赛快照 |
| Match | 比赛 | PS 赛区第 1 轮 RNG vs TITAN |
| MatchSubmission | 某一方提交的比赛数据 | RNG 提交本方数据 |
| Standing | 积分榜 | PS 赛区积分榜 |
| Stat | 技术统计 | 球队 / 球员统计 |
| Rank | 排行榜 | 得分榜 / 助攻榜 / 篮板榜 |

### 3.2 ID 命名规范

新代码中只允许以下 ID 语义：

```text
seriesId        赛事系列 ID
seasonId        赛事赛季 ID
divisionId      平台赛区 ID
registrationId  报名 ID
eventTeamId     赛事球队快照 ID
eventPlayerId   赛事球员快照 ID
matchId         比赛 ID
submissionId    比赛数据提交 ID
```

### 3.3 禁止旧语义

新代码中禁止出现以下语义：

```text
1. eventId 表示 seasonId
2. tournamentId 表示 seasonId
3. competition 表示 tournamentId 或 seasonId
4. gameTerminal 与 platformCode 双真源
5. seasonId + platformCode 拼接代替 divisionId
```

代码审查规则：

```text
新赛事域中出现 eventId 参数名，一律视为不合格。
除非它明确表示“非赛事域的通用业务事件”，否则必须改为 seasonId / divisionId / eventTeamId 等准确命名。
```

---

## 4. 最终表命名规范

为避免旧代码中 `eventId = seasonId` 的歧义，新表不采用“所有表都 basket_event_*”的方式。

最终命名采用混合清晰命名：

```text
核心容器：
basket_series
basket_season
basket_division

报名与审核：
basket_captain_grant
basket_registration
basket_registration_player
basket_registration_audit

正式参赛快照：
basket_event_team
basket_event_player

赛程与比赛：
basket_schedule_batch
basket_match
basket_match_period_score

比赛数据提交：
basket_match_submission
basket_match_submission_team_stat
basket_match_submission_player_stat
basket_match_submission_material

正式比赛统计：
basket_match_team_stat
basket_match_player_stat

赛季统计与榜单：
basket_standing
basket_team_stat
basket_player_stat
basket_rank
```

说明：

```text
1. Series / Season / Division 不使用 event 前缀，避免 eventId 旧歧义。
2. EventTeam / EventPlayer 使用 event 前缀，因为业务上确实是“赛事快照”。
3. Match / Submission 使用比赛语义，不强行挂 event 前缀。
4. 新表如与旧表同名，必须先完成旧表清理，再创建新表。
5. 禁止新旧同名表并存、双写、混查。
```

---

## 5. P0 / P1 / P2 新表清单

### 5.1 P0：第一阶段主链路必须具备

P0 表用于支撑：发布赛事、生成赛区、队长授权、球队报名、审核快照、生成赛程、比赛数据提交、审核结算、积分榜和基础统计。

```text
basket_series
basket_season
basket_division
basket_captain_grant
basket_registration
basket_registration_player
basket_registration_audit
basket_event_team
basket_event_player
basket_schedule_batch
basket_match
basket_match_period_score
basket_match_submission
basket_match_submission_team_stat
basket_match_submission_player_stat
basket_match_submission_material
basket_match_team_stat
basket_match_player_stat
basket_standing
basket_team_stat
basket_player_stat
basket_rank
```

### 5.2 P1：第二阶段增强能力

P1 表用于扩展争议、异常、阵容变更、赛程规则可配置等能力。

```text
basket_match_dispute
basket_match_exception
basket_roster_change_request
basket_schedule_rule
basket_schedule_weekday
basket_schedule_time_slot
basket_stage
basket_group
basket_round
```

本期处理策略：

```text
1. 不建或建空壳均可，但不接入口。
2. 如果不建，则必须删除/隐藏旧入口。
3. 本期不允许旧 dispute / exception / roster change 继续挂旧 match。
```

### 5.3 P2：后续运营增强

P2 用于奖项、荣誉、复杂赛制、自动任务等。

```text
basket_honor_rule
basket_honor_result
basket_playoff_bracket
basket_playoff_series
basket_seed_rule
basket_auto_schedule_job_log
basket_personal_registration
basket_personal_team_assignment
```

本期不做。

---

## 6. P0 新表字段设计

> 字段为方案级设计，Cursor 第 1 轮可以根据当前项目统一字段规范补充 Java Entity、Mapper、注释和索引。

### 6.1 `basket_series` 赛事系列

固定四种：

```text
LEAGUE
ARENA
CUP
OPEN
```

核心字段：

```text
id                  bigint PK
series_code         varchar(32)  唯一：LEAGUE / ARENA / CUP / OPEN
series_name         varchar(64)  展示名
series_desc         varchar(512) 简介
default_format_type varchar(32) 默认赛制
icon_file_id        bigint      图标文件 ID
cover_file_id       bigint      默认封面文件 ID
banner_file_id      bigint      默认 Banner 文件 ID
sort_order          int         排序
enabled             tinyint     是否启用
create_time         datetime
update_time         datetime
is_delete           tinyint
```

约束：

```text
uk_series_code(series_code, is_delete)
```

### 6.2 `basket_season` 赛事赛季

代表某个 Series 下的一届具体赛事。

核心字段：

```text
id                      bigint PK
series_id               bigint
season_year             int
season_name             varchar(128)   例：League S1
display_title           varchar(128)   例：CNPA League 2026 S1
season_desc             text
cover_file_id           bigint
banner_file_id          bigint
registration_mode       varchar(32)    当前强制 TEAM，可预留 PERSONAL
format_type             varchar(32)    第一阶段默认 REGULAR_ROUND_ROBIN
registration_start_time datetime
registration_end_time   datetime
match_start_time        datetime
match_end_time          datetime
publish_status          varchar(32)    DRAFT / PUBLISHED / OFFLINE
season_status           varchar(32)    NOT_STARTED / REGISTRATION / REGISTRATION_ENDED / PLAYING / FINISHED
create_time             datetime
update_time             datetime
is_delete               tinyint
```

说明：

```text
1. Season 不直接存多平台字符串。
2. 多平台通过 basket_division 表表达。
3. registration_mode 放 Season 层，Division 可冗余或覆盖，但第一阶段统一 TEAM。
4. 封面、Banner 使用 file_id，不直接存 URL。
```

### 6.3 `basket_division` 平台赛区

代表某个 Season 下某个平台的独立赛区。

核心字段：

```text
id                      bigint PK
season_id               bigint
platform_code           varchar(32)    PC / PS / XBOX
division_name           varchar(128)   例：PS赛区
registration_mode       varchar(32)    TEAM / PERSONAL，当前强制 TEAM
format_type             varchar(32)    REGULAR_ROUND_ROBIN 等
max_team_count          int            队伍上限
personal_limit          int            个人报名预留
lineup_min_count        int            默认 5
lineup_max_count        int            默认 8
registration_start_time datetime       可为空；为空时继承 Season
registration_end_time   datetime       可为空；为空时继承 Season
match_start_time        datetime       可为空；为空时继承 Season
match_end_time          datetime       可为空；为空时继承 Season
division_status         varchar(32)    NOT_STARTED / REGISTRATION / PLAYING / FINISHED / DISABLED
sort_order              int
create_time             datetime
update_time             datetime
is_delete               tinyint
```

约束：

```text
uk_division_platform(season_id, platform_code, is_delete)
idx_division_season(season_id)
```

### 6.4 `basket_captain_grant` 队长授权

队长授权直接绑定 Division，不再绑定 Season + Platform。

核心字段：

```text
id                  bigint PK
division_id         bigint
season_id           bigint       可冗余，便于查询
team_id             bigint       长期球队 ID
player_id           bigint       队长球员 ID
member_id           bigint       账号 / 成员 ID，按现有模型确定
grant_status        varchar(32)  ACTIVE / CANCELLED / EXPIRED
valid_from          datetime
valid_to            datetime
granted_by          bigint       后台管理员 ID
remark              varchar(512)
create_time         datetime
update_time         datetime
is_delete           tinyint
```

约束建议：

```text
uk_active_captain_grant(division_id, team_id, player_id, grant_status, is_delete)
idx_captain_player(player_id)
idx_captain_team(team_id)
```

### 6.5 `basket_registration` 报名主表

当前只做球队报名。

核心字段：

```text
id                    bigint PK
division_id           bigint
season_id             bigint       冗余查询
team_id               bigint       长期球队 ID
captain_player_id     bigint
captain_member_id     bigint
registration_mode     varchar(32)  TEAM / PERSONAL，当前 TEAM
registration_status   varchar(32)  DRAFT / PENDING_REVIEW / APPROVED / REJECTED / WITHDRAWN / CANCELLED
lineup_count          int
submit_time           datetime
audit_time            datetime
audit_user_id         bigint
audit_remark          varchar(512)
withdraw_time         datetime
withdraw_reason       varchar(512)
create_time           datetime
update_time           datetime
is_delete             tinyint
```

约束：

```text
uk_registration_division_team(division_id, team_id, is_delete)
idx_registration_status(division_id, registration_status)
```

规则：

```text
1. 只有 APPROVED 占名额。
2. PENDING_REVIEW 不占名额。
3. APPROVED 后生成 EventTeam / EventPlayer。
4. APPROVED 后不可撤回。
5. DRAFT / PENDING_REVIEW 可撤回或取消。
```

### 6.6 `basket_registration_player` 报名阵容表

核心字段：

```text
id                    bigint PK
registration_id        bigint
division_id            bigint
season_id              bigint
team_id                bigint
player_id              bigint
member_id              bigint
platform_identity_id   bigint
platform_code          varchar(32)
game_id_snapshot       varchar(128)
nickname_snapshot      varchar(128)
position_code          varchar(32)
jersey_no              varchar(32)
is_captain             tinyint
is_starter             tinyint
sort_order             int
create_time            datetime
update_time            datetime
is_delete              tinyint
```

规则：

```text
1. 阵容人数 5–8。
2. 不强制 PG / SG / SF / PF / C 齐全。
3. 当前不做逐人二次确认。
4. 选择阵容时必须校验球员认证、平台身份、长期球队成员关系。
```

### 6.7 `basket_registration_audit` 报名审核流水

P0 纳入。

核心字段：

```text
id                    bigint PK
registration_id        bigint
division_id            bigint
season_id              bigint
audit_action           varchar(32)  SUBMIT / APPROVE / REJECT / WITHDRAW / CANCEL
audit_before_status    varchar(32)
audit_after_status     varchar(32)
audit_user_id          bigint
audit_user_name        varchar(128)
audit_remark           varchar(512)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

说明：

```text
1. 队长提交、撤回也写审核/操作流水。
2. 后台通过、驳回写流水。
3. 用于后台追溯和问题排查。
```

### 6.8 `basket_event_team` 赛事球队快照

审核通过后生成。

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
registration_id        bigint
team_id                bigint
team_name_snapshot     varchar(128)
team_logo_file_id      bigint
team_short_name        varchar(64)
captain_player_id      bigint
captain_member_id      bigint
event_team_no          varchar(64)
event_team_status      varchar(32) ACTIVE / LOCKED / DISBANDED / CANCELLED
create_time            datetime
update_time            datetime
is_delete              tinyint
```

约束：

```text
uk_event_team_division_team(division_id, team_id, is_delete)
idx_event_team_division(division_id)
```

### 6.9 `basket_event_player` 赛事球员快照

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
event_team_id          bigint
team_id                bigint
player_id              bigint
member_id              bigint
platform_identity_id   bigint
platform_code          varchar(32)
game_id_snapshot       varchar(128)
nickname_snapshot      varchar(128)
avatar_file_id         bigint
position_code          varchar(32)
jersey_no              varchar(32)
is_captain             tinyint
is_starter             tinyint
event_player_status    varchar(32) ACTIVE / INACTIVE / REMOVED
create_time            datetime
update_time            datetime
is_delete              tinyint
```

约束：

```text
uk_event_player_division_player(division_id, player_id, is_delete)
idx_event_player_team(event_team_id)
```

规则：

```text
1. 快照生成后，历史赛事只读。
2. 长期球队队名、Logo、成员变化不回写历史快照。
3. 比赛、统计、排行榜只引用 EventTeam / EventPlayer。
```

### 6.10 `basket_schedule_batch` 赛程生成批次

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
batch_no               varchar(64)
generate_type          varchar(32) MANUAL / AUTO
generate_status        varchar(32) DRAFT / PUBLISHED / CANCELLED
format_type            varchar(32) REGULAR_ROUND_ROBIN
team_count             int
match_count            int
generated_by           bigint
generated_time         datetime
published_by           bigint
published_time         datetime
remark                 varchar(512)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

说明：

```text
1. 第一阶段不做自动排赛 Job。
2. 旧 ScheduleAutoGenerateJob 关闭或删除入口。
3. 只支持手动触发生成草稿、手动发布。
```

### 6.11 `basket_match` 比赛表

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
schedule_batch_id      bigint
round_no               int
match_no               varchar(64)
stage_code             varchar(32) REGULAR
home_event_team_id     bigint
away_event_team_id     bigint
match_time             datetime
match_status           varchar(32) SCHEDULED / PLAYING / FINISHED / CANCELLED
publish_status         varchar(32) DRAFT / PUBLISHED
home_score             int
away_score             int
winner_event_team_id   bigint
platform_code          varchar(32) 冗余
venue_name             varchar(128)
remark                 varchar(512)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

索引：

```text
idx_match_division_time(division_id, match_time, publish_status)
idx_match_team(home_event_team_id, away_event_team_id)
```

### 6.12 `basket_match_period_score` 节比分

核心字段：

```text
id                    bigint PK
match_id               bigint
division_id            bigint
period_no              int
home_score             int
away_score             int
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.13 `basket_match_submission` 比赛数据提交主表

维度：

```text
matchId + submit_event_team_id
```

核心字段：

```text
id                    bigint PK
match_id               bigint
division_id            bigint
season_id              bigint
submit_event_team_id   bigint
opponent_event_team_id bigint
submitter_member_id    bigint
submitter_player_id    bigint
submitter_role         varchar(32) CAPTAIN / ADMIN
submission_status      varchar(32) DRAFT / PENDING_REVIEW / APPROVED / REJECTED / SUPERSEDED / CANCELLED
version_no             int
submit_time            datetime
audit_time             datetime
audit_user_id          bigint
audit_remark           varchar(512)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

规则：

```text
1. 一方提交审核中，不阻塞另一方编辑和提交。
2. 审核通过后才同步正式比分和统计。
3. 驳回后允许重提，version_no 递增。
4. 已完成比赛也必须展示，可继续查看详情和数据。
```

### 6.14 `basket_match_submission_team_stat` 提交方球队数据

核心字段：

```text
id                    bigint PK
submission_id          bigint
match_id               bigint
division_id            bigint
event_team_id          bigint
score                  int
field_goal_made        int
field_goal_attempt     int
three_point_made       int
three_point_attempt    int
free_throw_made        int
free_throw_attempt     int
rebound                int
assist                 int
steal                  int
block                  int
turnover               int
foul                   int
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.15 `basket_match_submission_player_stat` 提交方球员数据

核心字段：

```text
id                    bigint PK
submission_id          bigint
match_id               bigint
division_id            bigint
event_team_id          bigint
event_player_id        bigint
points                 int
rebound                int
assist                 int
steal                  int
block                  int
turnover               int
foul                   int
minutes                decimal(6,2)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.16 `basket_match_submission_material` 提交材料

核心字段：

```text
id                    bigint PK
submission_id          bigint
match_id               bigint
division_id            bigint
material_type          varchar(32) IMAGE / VIDEO / FILE
file_id                bigint
file_name              varchar(255)
remark                 varchar(512)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.17 `basket_match_team_stat` 单场正式球队数据

P0 纳入，用于审核通过后的正式比赛详情。

核心字段：

```text
id                    bigint PK
match_id               bigint
division_id            bigint
season_id              bigint
event_team_id          bigint
opponent_event_team_id bigint
is_winner              tinyint
score                  int
points_for             int
points_against         int
field_goal_made        int
field_goal_attempt     int
three_point_made       int
three_point_attempt    int
free_throw_made        int
free_throw_attempt     int
rebound                int
assist                 int
steal                  int
block                  int
turnover               int
foul                   int
source_submission_id   bigint
create_time            datetime
update_time            datetime
is_delete              tinyint
```

约束：

```text
uk_match_team_stat(match_id, event_team_id, is_delete)
```

### 6.18 `basket_match_player_stat` 单场正式球员数据

P0 纳入。

核心字段：

```text
id                    bigint PK
match_id               bigint
division_id            bigint
season_id              bigint
event_team_id          bigint
event_player_id        bigint
points                 int
rebound                int
assist                 int
steal                  int
block                  int
turnover               int
foul                   int
minutes                decimal(6,2)
source_submission_id   bigint
create_time            datetime
update_time            datetime
is_delete              tinyint
```

约束：

```text
uk_match_player_stat(match_id, event_player_id, is_delete)
```

### 6.19 `basket_standing` 积分榜

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
event_team_id          bigint
rank_no                int
win_count              int
loss_count             int
draw_count             int
points_for             int
points_against         int
point_diff             int
standing_points        int
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.20 `basket_team_stat` 球队赛季统计

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
event_team_id          bigint
match_count            int
win_count              int
loss_count             int
points_for             int
points_against         int
avg_points_for         decimal(10,2)
avg_points_against     decimal(10,2)
rebound                int
assist                 int
steal                  int
block                  int
turnover               int
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.21 `basket_player_stat` 球员赛季统计

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
event_team_id          bigint
event_player_id        bigint
match_count            int
points                 int
avg_points             decimal(10,2)
rebound                int
avg_rebound            decimal(10,2)
assist                 int
avg_assist             decimal(10,2)
steal                  int
block                  int
turnover               int
create_time            datetime
update_time            datetime
is_delete              tinyint
```

### 6.22 `basket_rank` 排行榜

核心字段：

```text
id                    bigint PK
division_id            bigint
season_id              bigint
rank_type              varchar(32) TEAM_WIN / PLAYER_POINTS / PLAYER_REBOUND / PLAYER_ASSIST 等
rank_no                int
target_type            varchar(32) TEAM / PLAYER
event_team_id          bigint
event_player_id        bigint
rank_value             decimal(12,2)
create_time            datetime
update_time            datetime
is_delete              tinyint
```

---

## 7. 状态枚举

### 7.1 SeriesStatus

```text
ENABLED
DISABLED
```

### 7.2 PublishStatus

```text
DRAFT
PUBLISHED
OFFLINE
```

### 7.3 SeasonStatus

```text
NOT_STARTED
REGISTRATION
REGISTRATION_ENDED
PLAYING
FINISHED
CANCELLED
```

### 7.4 DivisionStatus

```text
NOT_STARTED
REGISTRATION
REGISTRATION_ENDED
PLAYING
FINISHED
DISABLED
```

### 7.5 RegistrationMode

```text
TEAM
PERSONAL
```

当前只允许 TEAM。

### 7.6 RegistrationStatus

对外统一为：

```text
DRAFT
PENDING_REVIEW
APPROVED
REJECTED
WITHDRAWN
CANCELLED
```

说明：

```text
不再对外强拆 SUBMITTED / REVIEWING。
用户提交后进入 PENDING_REVIEW。
后台待审核列表查询 PENDING_REVIEW。
```

### 7.7 FormatType

```text
REGULAR_ROUND_ROBIN
REGULAR_PLAYOFF
GROUP_KNOCKOUT
KNOCKOUT
```

第一阶段只实现 REGULAR_ROUND_ROBIN。

### 7.8 MatchStatus

```text
SCHEDULED
PLAYING
FINISHED
CANCELLED
```

### 7.9 MatchSubmissionStatus

```text
DRAFT
PENDING_REVIEW
APPROVED
REJECTED
SUPERSEDED
CANCELLED
```

### 7.10 枚举存储方式

第 1 轮必须冻结枚举存储方式。

建议：

```text
新赛事域统一使用字符串枚举。
理由：状态语义清晰、前后端联调直观、避免数字码误读。
```

如项目必须沿用整型 code，则第 1 轮必须为每个枚举制定 code 表，admin / web 统一，不允许混用。

---

## 8. 建表与清库顺序

### 8.1 默认执行方案

采用 Cursor 推荐的 A 方案：开发库先清旧赛事表，再创建新表。

执行顺序：

```text
1. Cursor 第 1 轮先扫描当前项目，产出精确旧赛事表清单。
2. 人工确认清库白名单与保留资产白名单。
3. 备份当前开发库 / 联调库。
4. DROP 或清空旧赛事域表。
5. 创建新赛事域表。
6. 初始化四种 Series 种子数据。
7. 可选插入 League 2026 S1 + PS / Xbox Division 示例数据。
8. 标记旧赛事接口、旧赛事页面、旧赛事菜单为待删除。
9. 后续新开发不得再依赖旧赛事表。
```

### 8.2 禁止方案

禁止：

```text
1. 新旧同名表并存。
2. 新旧表双写。
3. 新接口查新表、旧页面查旧表同时运营。
4. basket_match 新旧同名混用。
5. basket_registration 新旧同名混用。
6. eventId 继续作为 seasonId 传递。
```

### 8.3 如果暂时不能 DROP

只有在环境限制下才使用过渡方案：

```text
1. 临时使用新表过渡名。
2. 等旧表确认删除后再 rename。
3. 该方案不推荐，只作为异常兜底。
```

---

## 9. 清库白名单与保留白名单

### 9.1 清空 / 废弃旧赛事域对象

以下是当前已知旧赛事域对象，最终执行前由 Cursor 第 1 轮扫描项目 SQL、Mapper、Entity、Controller、菜单后产出精确清单。

已知需要清理或废弃的旧赛事域对象包括但不限于：

```text
basket_competition_season
basket_tournament
basket_tournament_season
basket_tournament_season_platform
basket_tournament_registration
basket_tournament_registration_player
basket_tournament_open_registration
basket_team_season_profile
basket_tournament_season_roster
basket_captain_qualification
basket_schedule_generate_batch
basket_schedule_rule
basket_schedule_weekday
basket_schedule_time_slot
basket_tournament_stage
basket_tournament_group
basket_tournament_round
basket_match
basket_match_data_submission
basket_match_data_submission_* 相关子表
basket_stage_standing
basket_team_season_stat
basket_player_season_stat
basket_team_rank
basket_player_rank
旧比赛争议表
旧比赛异常表
旧荣誉生成表
旧阵容变更审核表
旧赛事菜单 SQL
旧赛事 Demo 数据
旧赛事相关单测数据
```

说明：

```text
1. 以上是方案级清单，不直接作为执行 SQL。
2. 第 1 轮必须由 Cursor 根据真实仓库产出精确表名。
3. 清库前必须备份。
4. 禁止误删账号、球员、长期球队、后台权限、文件等保留资产。
```

### 9.2 保留资产白名单

以下表或模块原则上保留，具体表名由 Cursor 第 1 轮扫描确认：

```text
用户账号表
用户登录 / 邮箱验证码相关表
球员认证表
永久球员表
球员平台身份表
长期球队表
球队成员表
后台管理员表
后台角色表
后台菜单基础表
后台权限表
系统字典表
系统参数表
文件上传 / 文件资源表
公告表
合作伙伴表
登录日志 / 操作日志表
非赛事内容运营表
```

原则：

```text
赛事域可以清。
账号、球员、球队不能误清。
后台基础权限不能误清。
文件资源不能误清。
```

---

## 10. 本期重建 / 本期不做 / 删除入口清单

### 10.1 本期重建

```text
Series
Season
Division
CaptainGrant
Registration
RegistrationPlayer
RegistrationAudit
EventTeam
EventPlayer
ScheduleBatch
Match
MatchPeriodScore
MatchSubmission
MatchSubmissionTeamStat
MatchSubmissionPlayerStat
MatchSubmissionMaterial
MatchTeamStat
MatchPlayerStat
Standing
TeamStat
PlayerStat
Rank
后台步骤式赛事发布
后台队长授权
后台报名审核
后台赛程生成与发布
后台比赛数据审核
前台赛事列表
前台赛事详情
前台平台赛区切换
前台球队报名入口
前台比赛详情
个人中心我的赛事
个人中心我的赛程
比赛数据提交入口
```

### 10.2 本期不做并隐藏 / 删除入口

```text
旧 Open registration 全套入口
PERSONAL 个人报名入口
旧 ScheduleAutoGenerateJob
旧复杂 schedule rule 配置入口
旧 stage / group / round 管理入口
旧 match dispute / exception 入口
旧 roster change 入口
旧 honor generation 入口
旧 /events 与 /tournament 混用路由
旧后台 cnpa/event/list 发布页
旧 registrationAudit 菜单
旧 matchAudit 菜单
旧赛事相关 Demo 旧数据生成器
旧赛事相关单测入口
```

处理原则：

```text
1. 本期不做的旧功能必须从新菜单移除。
2. 不允许新主链路调用旧入口。
3. 不允许旧 Job 继续扫描旧表。
4. 不允许运营在新旧菜单之间混用。
```

---

## 11. 后台业务流程

### 11.1 后台发布赛事流程

后台发布使用步骤式表单。

```text
第 1 步：选择赛事系列
- League
- Arena
- Cup
- Open

第 2 步：填写赛季基础信息
- 年份
- 赛季名称
- 展示标题
- 简介
- 封面文件
- Banner 文件

第 3 步：选择平台生成 Division
- PC
- PS
- Xbox

第 4 步：配置报名规则
- 当前只允许 TEAM
- 队伍上限
- 阵容人数 5–8
- 报名开始时间
- 报名截止时间

第 5 步：配置赛制
- 第一阶段只允许 REGULAR_ROUND_ROBIN
- 其他赛制置灰或提示后续开放

第 6 步：配置比赛时间
- 比赛开始时间
- 预计结束时间
- 第一阶段默认按固定规则排赛

第 7 步：预览并发布
- Series
- Season
- Divisions
- 报名规则
- 赛制
- 时间
```

发布后生成：

```text
1 条 basket_season
N 条 basket_division
```

### 11.2 队长授权流程

```text
后台选择 Season
↓
选择 Division
↓
选择长期球队
↓
选择认证球员作为队长
↓
授予 captain grant
```

规则：

```text
1. 授权绑定 division_id。
2. 没有永久队长资格。
3. 新赛季 / 新赛区需重新授权。
4. 队长只能在被授权的 Division 下报名。
```

### 11.3 球队报名流程

```text
队长登录
↓
进入可报名赛事
↓
选择 Season + Division
↓
选择长期球队
↓
选择 5–8 名当前有效成员
↓
提交报名
↓
状态进入 PENDING_REVIEW
```

校验：

```text
1. 当前账号必须是认证球员。
2. 当前球员必须拥有该 division 的 captain grant。
3. 长期球队必须有效。
4. 阵容成员必须是长期球队有效成员。
5. 阵容成员必须有当前 platform 的平台身份。
6. 阵容人数 5–8。
7. 当前只允许 TEAM。
8. 同一长期球队允许报名同一 Season 下多个不同 Division。
9. 同一 Division 下同一长期球队只能有一条有效报名。
```

### 11.4 审核与快照流程

```text
后台审核 PENDING_REVIEW 报名
↓
通过时按 division 统计 APPROVED 数量
↓
未超过 max_team_count 才可通过
↓
生成 EventTeam
↓
生成 EventPlayer
↓
锁定快照
```

规则：

```text
1. 只有 APPROVED 占名额。
2. PENDING_REVIEW 不占名额。
3. APPROVED 后不可撤回。
4. 快照生成后用于赛程、比赛、统计。
5. 历史快照不受长期球队后续变化影响。
```

---

## 12. 赛程与比赛流程

### 12.1 赛程生成

```text
后台选择 Season
↓
选择 Division
↓
系统读取该 Division 下 APPROVED 的 EventTeam
↓
按 REGULAR_ROUND_ROBIN 生成单循环赛程
↓
奇数队使用 BYE
↓
按固定时间规则分配 match_time
↓
生成 DRAFT Match
↓
管理员发布
```

固定时间规则：

```text
周二 / 周四 / 周六
21:00 / 22:00
每时间段最多 6 场
每天最多 12 场
```

### 12.2 比赛状态

```text
SCHEDULED：已发布未开始
PLAYING：比赛中
FINISHED：已结束
CANCELLED：已取消
```

### 12.3 发布规则

```text
1. DRAFT Match 前台不可见。
2. PUBLISHED Match 前台可见。
3. 前台赛事详情按 divisionId 查询赛程。
4. 个人中心我的赛程按 eventPlayer / eventTeam 反查 match。
5. 已结束比赛也必须展示。
```

---

## 13. 比赛数据提交、审核与结算

### 13.1 提交维度

比赛数据提交维度固定为：

```text
matchId + submitEventTeamId
```

规则：

```text
1. 主队提交主队数据。
2. 客队提交客队数据。
3. 一方提交审核中，不影响另一方编辑和提交。
4. 后台按 submission 审核，不按整个 match 一次性审核。
```

### 13.2 审核通过后同步

```text
Submission APPROVED
↓
写入 / 更新 Match 正式比分
↓
写入 basket_match_team_stat
↓
写入 basket_match_player_stat
↓
重建 basket_standing
↓
重建 basket_team_stat
↓
重建 basket_player_stat
↓
重建 basket_rank
```

所有结算均以 division_id 为边界。

---

## 14. 前台流程

### 14.1 赛事列表

前台赛事栏目：

```text
全部
League
Arena
Cup
Open
```

赛事卡片展示：

```text
Series
Season display_title
平台：PS / Xbox / PC
报名方式：球队报名
报名状态
报名截止时间
队伍上限
赛制
```

### 14.2 赛事详情

进入 Season 详情后展示 Division 切换：

```text
PS
Xbox
PC
```

切换 Division 后刷新：

```text
1. 报名状态
2. 参赛队伍
3. 赛程
4. 比赛结果
5. 积分榜
6. 球队统计
7. 球员排行榜
```

### 14.3 报名入口

```text
选择 Season
↓
选择 Division
↓
校验队长授权
↓
进入球队报名
```

### 14.4 个人中心

个人中心“我的赛事”继续收敛为四个 Tab：

```text
可参与赛事
我的报名
已参加赛事
我的赛程
```

展示时必须带：

```text
Series
Season
Division
Platform
Team
MatchTime
MatchStatus
SubmissionStatus
```

---

## 15. API 设计原则

### 15.1 路径风格

继续沿用现有 `/basket/...` 风格，不强制改成 `/basket/admin/...`。

### 15.2 参数规范

禁止：

```text
eventId
competition
tournamentId 装 seasonId
seasonId + platformCode 代替 divisionId
```

推荐：

```text
seriesId
seasonId
divisionId
registrationId
eventTeamId
eventPlayerId
matchId
submissionId
```

### 15.3 常用接口方向

后台：

```text
/basket/series
/basket/season
/basket/division
/basket/captain-grant
/basket/registration
/basket/registration/audit
/basket/schedule
/basket/match
/basket/match-submission/audit
```

前台：

```text
/basket/public/series
/basket/public/season/page
/basket/public/season/{seasonId}
/basket/public/division/{divisionId}/overview
/basket/public/division/{divisionId}/schedule
/basket/public/division/{divisionId}/standing
/basket/account/my-events
/basket/account/my-schedules
/basket/player/match-submission
```

实际接口命名由 Cursor 按项目已有 Controller 规范微调，但必须遵守 ID 语义。

---

## 16. 分轮计划

### 第 0 轮：最终共识

目标：

```text
确认本稿内容
确认清库策略
确认表命名
确认 P0 表清单
确认第 1 轮边界
```

不写代码。

### 第 1 轮：新赛事域模型落地

只做：

```text
1. 术语 / 枚举冻结
2. 新表 DDL
3. Entity
4. Mapper
5. Series Seed
6. 清库说明 / 清库脚本草案
7. 可选示例 Season + PS/Xbox Division 数据
```

不做：

```text
1. 报名业务
2. 审核业务
3. 赛程业务
4. 比赛数据业务
5. 前台页面
6. 个人报名
7. 自动排赛 Job
8. 旧数据迁移
```

验收：

```text
1. 新表可表达 Series → Season → Division。
2. 主链路表均有 division_id。
3. 四种 Series 种子存在。
4. 可插入 League 2026 S1 + PS / Xbox Division。
5. 本地构建通过。
6. 无新代码使用 eventId 表示 seasonId。
```

### 第 2 轮：后台发布流程

目标：后台可创建 Series 下的 Season，并生成多个 Division。

验收：

```text
1. 后台可步骤式发布 Season。
2. 可多选平台生成 Divisions。
3. 当前只能选 TEAM。
4. PERSONAL 置灰或拦截。
5. 前台或后台基础接口可查询 season/division 基础信息。
```

### 第 3 轮：队长授权 + 报名 + 审核 + 快照

目标：球队报名主链路闭环。

验收：

```text
1. 队长授权绑定 division_id。
2. 队长可报名指定 Division。
3. 阵容 5–8 校验。
4. 报名审核通过按 division 名额校验。
5. 生成 EventTeam / EventPlayer。
6. PENDING_REVIEW 不占名额，APPROVED 占名额。
```

### 第 4 轮：赛程生成 + Match

目标：按 Division 生成赛程，禁止跨平台混排。

验收：

```text
1. PS 赛区只排 PS EventTeam。
2. Xbox 赛区只排 Xbox EventTeam。
3. 生成 DRAFT Match。
4. 发布后前台可见。
5. 旧 ScheduleAutoGenerateJob 关闭。
6. 第一阶段不新建自动排赛 Job。
```

### 第 5 轮：比赛数据提交 + 审核 + 结算

目标：提交、审核、正式统计、榜单后端闭环。

验收：

```text
1. submission 维度为 matchId + submitEventTeamId。
2. 一方审核中不阻塞另一方。
3. 审核通过写正式比分。
4. 写入 match_team_stat / match_player_stat。
5. 按 division 重建 standing / team_stat / player_stat / rank。
6. 后端接口可查榜单。
```

### 第 6 轮：前台赛事列表与详情

目标：用户侧赛事浏览与报名入口闭环。

验收：

```text
1. 赛事列表按 Series 展示。
2. Season 详情支持 Division 切换。
3. 赛程 / 参赛队伍 / 榜单按 Division 查询。
4. 报名入口进入新 Registration 流程。
```

### 第 7 轮：个人中心我的赛事 / 我的赛程 / 比赛数据入口

目标：个人中心闭环。

验收：

```text
1. 可参与赛事展示新 Season / Division。
2. 我的报名展示新 Registration。
3. 已参加赛事展示 EventTeam / EventPlayer。
4. 我的赛程展示所有关联 Match。
5. 比赛数据提交入口进入新 Submission。
6. 旧 tournamentId 混用路由删除或替换。
```

### 第 8 轮：旧入口删除 + 全链路联调

目标：收口。

验收：

```text
1. 旧赛事菜单删除或隐藏。
2. 旧 Open 入口不暴露。
3. 旧赛事 Demo / Job / 单测处理完成。
4. 全链路可跑通：发布赛事 → 授权 → 报名 → 审核 → 快照 → 排赛 → 提交 → 审核 → 统计 → 前台展示 → 个人中心展示。
5. Admin / Web 前后端构建通过。
```

---

## 17. 第 0 轮最终确认清单

进入第 1 轮前，双方确认以下内容：

```text
[ ] 是否确认赛事域全量重构，不迁旧赛事数据？
[ ] 是否确认旧三层 competition_season / tournament / tournament_season 不再作为新主线？
[ ] 是否确认 Series → Season → Division 为唯一主线？
[ ] 是否确认表命名采用本稿第 4 节？
[ ] 是否确认 P0 表清单采用本稿第 5.1 节？
[ ] 是否确认封面 / 图标 / Banner 使用 file_id？
[ ] 是否确认第一阶段赛程时间规则硬编码？
[ ] 是否确认 registration_audit 纳入 P0？
[ ] 是否确认 match_team_stat / match_player_stat 纳入 P0？
[ ] 是否确认当前只开放 TEAM？
[ ] 是否确认 PERSONAL / Open 只预留和拦截？
[ ] 是否确认同一长期球队允许报名同一 Season 下多个 Division？
[ ] 是否确认队长授权绑定 division_id？
[ ] 是否确认第 1 轮只做 DDL / Enum / Entity / Mapper / Seed？
[ ] 是否确认第 1 轮不写报名 / 赛程 / 提交业务？
[ ] 是否确认清库前必须备份并产出精确旧表清单？
[ ] 是否确认账号、球员、长期球队、后台权限、文件、公告、合作伙伴保留？
```

---

## 18. 给 Cursor 的最终评审提示词

```text
请评审《CNPA 赛事域 V4 全量重构最终共识稿》。

本轮仍然不要改代码、不要写 SQL、不要新增 docs 文件、不要重构接口，只在对话中输出最终评审意见。

请重点确认：

1. 是否同意本稿作为最终共识稿？
2. 是否同意表命名？
3. 是否同意 P0 / P1 / P2 表清单？
4. 是否同意 registration_audit、match_team_stat、match_player_stat 纳入 P0？
5. 是否同意 cover_file_id / banner_file_id / icon_file_id？
6. 是否同意第一阶段赛程时间规则硬编码？
7. 是否同意建表与清库顺序？
8. 是否同意第 1 轮只做 DDL / Enum / Entity / Mapper / Seed，不写复杂业务？
9. 是否还有会导致新旧夹生的遗漏模块？
10. 如果同意，请明确回复：可以进入第 1 轮模型落地。

如果仍有不同意，请只列出不同意点、原因、替代方案，不要再大范围发散。
```

---

## 19. 给 Cursor 的第 1 轮开发边界预案

只有在最终评审通过后，才允许进入第 1 轮。

第 1 轮任务边界：

```text
你现在在 basketball-20260803 分支。

本轮是 CNPA 赛事域 V4 全量重构第 1 轮：新赛事域模型落地。

请只做：
1. 精确扫描旧赛事域表和保留资产表，输出清库清单到对话，不要先执行清库。
2. 根据最终共识稿创建新赛事域 P0 表 DDL。
3. 创建对应 Enum。
4. 创建 Entity / Mapper 基础结构。
5. 初始化四种 Series 种子数据。
6. 可选初始化 League 2026 S1 + PS / Xbox Division 示例数据。
7. 确保本地构建通过。

本轮禁止：
1. 不写报名业务。
2. 不写审核业务。
3. 不写赛程业务。
4. 不写比赛数据提交业务。
5. 不改前台页面。
6. 不实现个人报名。
7. 不实现小组赛 / 淘汰赛。
8. 不启用自动排赛 Job。
9. 不做旧数据迁移。
10. 不生成额外 docs 报告文件，完成后只在对话中说明修改内容和构建结果。

验收标准：
1. 新表能表达 Series → Season → Division。
2. 主链路表都有 division_id。
3. 四种 Series 种子存在。
4. 示例 Season + PS/Xbox Division 可插入。
5. 枚举统一。
6. 无新代码使用 eventId 表示 seasonId。
7. 构建通过。
```

---

## 20. 最终一句话共识

```text
CNPA 赛事域按 V4 全量重构：Series → Season → Division 为唯一主线；只做 TEAM 与常规赛单循环基础版；旧赛事数据不迁移，旧入口最终删除；账号、球员、长期球队保留；第 1 轮只落地新模型、枚举、表结构与种子数据，不写复杂业务。
```

