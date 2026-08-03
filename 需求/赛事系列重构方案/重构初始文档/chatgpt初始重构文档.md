# CNPA 赛事域 V4 重构方案（评审稿）

> 日期：2026-08-03  
> 分支：`basketball-20260803`  
> 阶段：方案评审，不进入编码  
> 目标：先由 ChatGPT 输出完整重构方案，再交给 Cursor 做代码视角评估，双方对模型、流程、边界、轮次达成共识后再开始开发。

---

## 0. 本方案定位

本方案用于重构 CNPA 篮球官网的“赛事域”模型。

当前不急于写代码，不急于建表，不急于改页面。当前阶段只做：

1. 明确新赛事模型。
2. 明确哪些旧模型废弃。
3. 明确哪些业务保留。
4. 明确后台发布流程。
5. 明确前台展示流程。
6. 明确报名、审核、快照、赛程、比赛数据主链路。
7. 明确分轮实施步骤。
8. 明确交给 Cursor 评估的问题清单。

本方案属于 **评审稿**，不是最终开发指令。Cursor 需要基于真实代码继续评估可落地性、风险和调整建议。

---

## 1. 背景与问题

### 1.1 老板新的产品想法

老板希望 CNPA 赛事体系从“直接创建一个赛事赛季”调整为：

```text
先有固定赛事体系 / 赛事品牌：
League / Arena / Cup / Open

再在某个赛事体系下发布具体赛季：
League 2026 S1
Cup 2026 春季杯
Open 2026 第一期

再按平台拆分为赛区：
PS / Xbox / PC

再进行报名、审核、赛程、比赛、统计。
```

也就是说，新的产品主线是：

```text
Series 赛事系列
  ↓
Season 赛事赛季
  ↓
Division 平台赛区
  ↓
Registration 报名
  ↓
EventTeam / EventPlayer 快照
  ↓
Match 比赛
  ↓
MatchSubmission 比赛数据提交
  ↓
Standing / Ranking / Stats 统计榜单
```

### 1.2 现有模型的主要问题

根据 Cursor 的代码评估，当前赛事域不是简单的一层，而是存在：

```text
basket_competition_season
  ↓
basket_tournament
  ↓
basket_tournament_season
```

其中：

- `basket_competition_season` 更像年度/届别容器。
- `basket_tournament` 更像具体赛事实体，但包含单值 `game_terminal`。
- `basket_tournament_season` 实际承载报名、赛程、比赛、统计等运营主链路。
- 赛程模块中的 `eventId` 实际上经常等于 `seasonId`。
- `eventId / seasonId / tournamentId / competitionSeasonId` 存在语义混用。
- 当前多平台能力是半成品：有平台字段或平台表，但报名名额、赛程、排行榜、结算没有真正按平台赛区隔离。

### 1.3 本次策略调整

原先考虑“兼容旧结构”，但现在项目仍处于开始阶段，历史数据包袱较小，因此本方案建议：

```text
不再兼容旧赛事三层结构。
不再围绕旧 eventId / seasonId / tournamentId 做补丁。
直接重建赛事域主模型。
```

但是，不代表整个系统推倒重来。

---

## 2. 重构范围

### 2.1 建议推翻重来的部分

以下属于“赛事域”，建议按新模型重建：

```text
赛事系列
赛事赛季
平台赛区
赛事报名
报名阵容
报名审核
赛事队伍快照
赛事球员快照
赛程生成
比赛管理
比赛数据提交
比赛数据审核
战绩统计
排行榜
赛事详情
个人中心我的赛事 / 我的赛程中的赛事数据来源
```

### 2.2 建议保留的部分

以下不属于本次赛事模型重建核心，建议保留：

```text
用户账号
邮箱注册登录
球员认证
永久球员身份
球员平台身份
长期球队
球队成员关系
后台基础权限
系统字典
文件上传
公告内容
合作伙伴 / 赞助墙
基础布局和样式体系
```

### 2.3 本轮明确不做的部分

以下内容本阶段只做字段预留或产品占位，不实现完整流程：

```text
个人报名完整流程
个人报名池
后台手动组队
系统自动组队
小组赛完整晋级
季后赛完整晋级树
BO3 / BO5
种子队
轮空规则
复杂赛制引擎
直播 / 视频 / 回放
支付
商城
```

---

## 3. 总体设计原则

### 3.1 赛事域统一主线

后续所有赛事业务必须围绕以下主线：

```text
Series → Season → Division → Registration → EventTeam/EventPlayer → Match → Submission → Stats
```

不要再让 `eventId`、`seasonId`、`tournamentId` 混用。

### 3.2 多平台必须用 Division 隔离

一个 Season 可以选择多个平台：

```text
League 2026 S1
├── PS 赛区
└── Xbox 赛区
```

每个 Division 独立：

```text
报名名额
报名审核
正式参赛队伍
正式参赛球员
赛程
比赛
比赛数据
战绩
排行榜
```

### 3.3 当前只开放球队报名

当前正式开放：

```text
registration_mode = TEAM
```

预留但不开放：

```text
registration_mode = PERSONAL
```

如果前端或接口传 `PERSONAL`，后端必须明确拦截：

```text
个人报名暂未开放
```

### 3.4 所有报名最终统一到快照

不管未来报名来源是：

```text
球队报名
个人报名
后台导入
系统组队
```

只要进入正式比赛，最终都必须沉淀为：

```text
EventTeam 赛事球队快照
EventPlayer 赛事球员快照
```

赛程、比赛、比赛数据、统计只基于快照，不直接依赖长期球队或报名池。

### 3.5 新表命名必须清楚

建议统一使用 `basket_event_*` 作为新赛事域表前缀，避免继续混用 `tournament / season / competition`。

---

## 4. 新领域模型

## 4.1 Series：赛事系列

### 4.1.1 业务含义

Series 是固定赛事品牌或赛事体系。

固定四种：

```text
LEAGUE
ARENA
CUP
OPEN
```

它不是一次具体比赛，也不是某一届赛季。

### 4.1.2 示例

```text
League：长期联赛体系
Arena：竞技场体系
Cup：杯赛体系
Open：公开赛体系
```

### 4.1.3 建议表

表名建议：

```text
basket_event_series
```

核心字段建议：

```text
id
series_code          LEAGUE / ARENA / CUP / OPEN
series_name          League / Arena / Cup / Open
series_title         展示标题
series_desc          说明
default_format_type  默认赛制
cover_url            默认封面
banner_url           默认 Banner
icon_url             图标
sort_order           排序
enabled              是否启用
remark
create_time
update_time
is_delete
```

### 4.1.4 后台维护策略

建议后台允许维护：

```text
展示名称
描述
封面
Banner
默认赛制
排序
启用状态
```

但不建议开放随意新增第五种 Series。

也就是说，`series_code` 代码层仍固定四种。

---

## 4.2 Season：赛事赛季

### 4.2.1 业务含义

Season 是某个 Series 下的一期具体赛事。

例如：

```text
League 2026 S1
League 2026 S2
Cup 2026 春季杯
Open 2026 第一期
```

### 4.2.2 建议表

表名建议：

```text
basket_event_season
```

核心字段建议：

```text
id
series_id
series_code
season_year              2026
season_no                S1 / S2，可选
season_name              League S1
display_title            CNPA League 2026 S1
short_title              League S1
description              赛事介绍
cover_url
banner_url
rule_text                规则说明
registration_start_time  报名开始时间
registration_end_time    报名截止时间
match_start_time         比赛开始时间
match_end_time           预计结束时间
publish_status           DRAFT / PUBLISHED / OFFLINE
season_status            NOT_STARTED / REGISTRATION / REGISTRATION_ENDED / PLAYING / FINISHED / ARCHIVED
visible                  前台是否可见
sort_order
remark
create_time
update_time
is_delete
```

### 4.2.3 注意事项

Season 不直接承载平台多选结果。

平台下沉到 Division。

---

## 4.3 Division：平台赛区

### 4.3.1 业务含义

Division 是某个 Season 下的某个平台赛区。

例如：

```text
League 2026 S1 - PS
League 2026 S1 - Xbox
```

### 4.3.2 为什么必须有 Division

因为以下数据必须平台隔离：

```text
报名名额
报名审核
正式参赛队伍
赛程
比赛
比赛数据
战绩榜
球员榜
个人中心我的赛程
```

如果只在 Season 上保存 `PS,Xbox` 字符串，会导致：

```text
PS / Xbox 报名名额混算
PS / Xbox 队伍混排
PS / Xbox 排行榜混算
PS / Xbox 比赛数据混算
```

### 4.3.3 建议表

表名建议：

```text
basket_event_division
```

核心字段建议：

```text
id
season_id
series_id
series_code
platform_code           PC / PS / XBOX
division_name           PS赛区 / Xbox赛区
registration_mode       TEAM / PERSONAL
team_limit              队伍上限
personal_limit          个人报名上限，当前预留
lineup_min_count        当前 5
lineup_max_count        当前 8
format_type             REGULAR_PLAYOFF / GROUP_KNOCKOUT / KNOCKOUT
schedule_rule_code      赛程规则版本
registration_start_time 可为空，为空继承 season
registration_end_time   可为空，为空继承 season
match_start_time        可为空，为空继承 season
match_end_time          可为空，为空继承 season
publish_status          DRAFT / PUBLISHED / OFFLINE
division_status         NOT_STARTED / REGISTRATION / REGISTRATION_ENDED / PLAYING / FINISHED / ARCHIVED
visible
sort_order
remark
create_time
update_time
is_delete
```

### 4.3.4 当前限制

当前只允许：

```text
registration_mode = TEAM
```

`PERSONAL` 只能存在于枚举和字段，不开放业务流程。

---

## 4.4 Registration：赛事报名

### 4.4.1 业务含义

Registration 是报名记录。

当前只支持球队报名。

### 4.4.2 建议表

表名建议：

```text
basket_event_registration
```

核心字段建议：

```text
id
season_id
division_id
series_id
series_code
registration_mode       TEAM / PERSONAL，当前只允许 TEAM
team_id                 长期球队 ID
captain_player_id       队长球员 ID
submitter_user_id       提交用户 ID
status                  DRAFT / SUBMITTED / REVIEWING / APPROVED / REJECTED / WITHDRAWN / CANCELLED
reject_reason
submit_time
review_time
reviewer_id
version                 乐观锁 / 重提版本
remark
create_time
update_time
is_delete
```

### 4.4.3 状态流转

```text
DRAFT 草稿
  ↓
SUBMITTED 已提交
  ↓
REVIEWING 审核中
  ↓                ↘
APPROVED 已通过       REJECTED 已驳回
  ↓                  ↓
生成 EventTeam        可修改后重新提交
生成 EventPlayer
```

允许撤回：

```text
SUBMITTED / REVIEWING → WITHDRAWN
```

不允许撤回：

```text
APPROVED 后不可撤回
```

### 4.4.4 名额规则

只统计：

```text
APPROVED
```

不统计：

```text
DRAFT
SUBMITTED
REVIEWING
REJECTED
WITHDRAWN
CANCELLED
```

名额维度：

```text
division_id
```

不是 season_id。

---

## 4.5 RegistrationPlayer：报名阵容

### 4.5.1 业务含义

报名时队长从长期球队有效成员中选择 5–8 人阵容。

### 4.5.2 建议表

表名建议：

```text
basket_event_registration_player
```

核心字段建议：

```text
id
registration_id
season_id
division_id
team_id
player_id
platform_identity_id
platform_code
game_id_snapshot
nickname_snapshot
position
jersey_no
is_captain
is_starter
sort_order
remark
create_time
update_time
is_delete
```

### 4.5.3 校验规则

```text
1. 报名阵容人数 5–8。
2. 队长必须在阵容中。
3. 阵容成员必须是长期球队当前有效成员。
4. 阵容成员必须是认证球员。
5. 阵容成员必须拥有当前 division 平台对应的平台身份。
6. 同一 division 下，一个 player 只能属于一个 approved registration / event team。
7. 不强制 PG/SG/SF/PF/C 齐全。
```

---

## 4.6 EventTeam：赛事球队快照

### 4.6.1 业务含义

EventTeam 是某个 Division 下的正式参赛球队快照。

它不是长期球队本身。

长期球队可改名、换 logo，但历史赛事中的 EventTeam 必须保持当时快照。

### 4.6.2 建议表

表名建议：

```text
basket_event_team
```

核心字段建议：

```text
id
season_id
division_id
series_id
series_code
registration_id
team_id
team_name_snapshot
team_logo_snapshot
team_short_name_snapshot
captain_player_id
captain_name_snapshot
platform_code
seed_no
group_code
status                  ACTIVE / DISQUALIFIED / WITHDRAWN / ARCHIVED
remark
create_time
update_time
is_delete
```

### 4.6.3 生成时机

```text
报名审核通过后生成。
```

### 4.6.4 唯一性建议

```text
同一个 division 下，一个长期球队只能有一个有效 EventTeam。
同一个 division 下，一个 player 只能属于一个有效 EventTeam。
```

---

## 4.7 EventPlayer：赛事球员快照

### 4.7.1 业务含义

EventPlayer 是某个 EventTeam 下的正式参赛球员快照。

### 4.7.2 建议表

表名建议：

```text
basket_event_player
```

核心字段建议：

```text
id
season_id
division_id
event_team_id
team_id
player_id
user_id
platform_identity_id
platform_code
game_id_snapshot
nickname_snapshot
avatar_snapshot
position
jersey_no
is_captain
is_starter
roster_status           ACTIVE / REMOVED / REPLACED / SUSPENDED
locked                  1/0
remark
create_time
update_time
is_delete
```

### 4.7.3 注意事项

比赛、统计、历史榜单尽量引用 EventPlayer，而不是直接引用 Player。

---

## 4.8 Match：比赛

### 4.8.1 业务含义

Match 是 Division 下的一场比赛。

### 4.8.2 建议表

表名建议：

```text
basket_event_match
```

核心字段建议：

```text
id
season_id
division_id
series_id
series_code
platform_code
stage_code              REGULAR / PLAYOFF / GROUP / KNOCKOUT / FINAL
round_no
match_no
home_event_team_id
away_event_team_id
home_score
away_score
winner_event_team_id
match_time
match_status            DRAFT / SCHEDULED / PLAYING / FINISHED / CANCELLED / FORFEIT
publish_status          DRAFT / PUBLISHED
result_status           NONE / PENDING_REVIEW / CONFIRMED
venue_text
remark
create_time
update_time
is_delete
```

### 4.8.3 状态区分

必须区分：

```text
赛事状态 season_status
赛区状态 division_status
比赛状态 match_status
比赛数据提交状态 submission_status
```

不要因为某一方提交比赛数据，就把整场赛事或整个赛区置为审核中。

---

## 4.9 MatchSubmission：比赛数据提交

### 4.9.1 业务含义

比赛数据提交是某场比赛中某一方队伍提交的数据。

维度必须是：

```text
match_id + submit_event_team_id
```

不是：

```text
match_id
season_id
整个赛事
```

### 4.9.2 建议表

表名建议：

```text
basket_event_match_submission
```

核心字段建议：

```text
id
season_id
division_id
match_id
submit_event_team_id
opponent_event_team_id
submitter_user_id
submitter_player_id
home_score_submitted
away_score_submitted
submission_status       DRAFT / SUBMITTED / REVIEWING / APPROVED / REJECTED / WITHDRAWN
submit_time
review_time
reviewer_id
reject_reason
version
remark
create_time
update_time
is_delete
```

### 4.9.3 权限规则

```text
1. 队长只能提交本方比赛数据。
2. A 队提交审核中，不影响 B 队提交或编辑本方数据。
3. 审核通过后，才能同步正式比分和统计。
4. 已结束比赛仍可展示，用于查看详情和补交数据。
```

---

## 4.10 Standing / Ranking / Stats

### 4.10.1 统计维度

所有统计榜单必须绑定：

```text
season_id + division_id
```

不要只按 season 统计。

### 4.10.2 建议表

可以拆为：

```text
basket_event_team_stat
basket_event_player_stat
basket_event_standing
basket_event_team_rank
basket_event_player_rank
```

也可以第一阶段简化，只保留：

```text
basket_event_standing
basket_event_team_stat
basket_event_player_stat
```

### 4.10.3 重建时机

```text
比赛数据审核通过后
  ↓
同步比赛正式比分
  ↓
重建当前 division 的球队战绩
  ↓
重建当前 division 的球员统计
  ↓
刷新排行榜
```

---

## 5. 枚举设计

## 5.1 SeriesCode

```text
LEAGUE
ARENA
CUP
OPEN
```

## 5.2 PlatformCode

```text
PC
PS
XBOX
```

## 5.3 RegistrationMode

```text
TEAM
PERSONAL
```

当前只开放 TEAM。

## 5.4 FormatType

```text
REGULAR_PLAYOFF      常规赛 + 季后赛
GROUP_KNOCKOUT       小组赛 + 淘汰赛
KNOCKOUT             直接淘汰赛
```

第一阶段建议：

```text
REGULAR_PLAYOFF 可开放基础版
KNOCKOUT 可预留或基础版
GROUP_KNOCKOUT 只预留，不开放完整规则
```

## 5.5 PublishStatus

```text
DRAFT
PUBLISHED
OFFLINE
```

## 5.6 SeasonStatus / DivisionStatus

```text
NOT_STARTED
REGISTRATION
REGISTRATION_ENDED
PLAYING
FINISHED
ARCHIVED
```

## 5.7 RegistrationStatus

```text
DRAFT
SUBMITTED
REVIEWING
APPROVED
REJECTED
WITHDRAWN
CANCELLED
```

## 5.8 MatchStatus

```text
DRAFT
SCHEDULED
PLAYING
FINISHED
CANCELLED
FORFEIT
```

## 5.9 SubmissionStatus

```text
DRAFT
SUBMITTED
REVIEWING
APPROVED
REJECTED
WITHDRAWN
```

---

## 6. 核心业务流程

# 6.1 后台发布赛事流程

## 6.1.1 目标

后台不再直接“新增赛事赛季”，而是按步骤创建：

```text
Series → Season → Division
```

## 6.1.2 步骤一：选择赛事系列

管理员选择：

```text
League
Arena
Cup
Open
```

系统带出默认配置：

```text
默认赛制
默认描述
默认 Banner
默认报名方式
```

## 6.1.3 步骤二：填写赛季信息

字段：

```text
年份：2026
赛季名称：League S1
展示标题：CNPA League 2026 S1
赛事简介
赛事规则说明
封面
Banner
```

## 6.1.4 步骤三：选择平台

管理员选择：

```text
PC
PS
XBOX
```

多选时系统创建多个 Division。

例如选择 PS / Xbox：

```text
League 2026 S1 - PS
League 2026 S1 - Xbox
```

## 6.1.5 步骤四：配置报名规则

当前只开放：

```text
球队报名
```

字段：

```text
报名方式：TEAM
队伍上限：例如 10 支
阵容最少人数：5
阵容最多人数：8
报名开始时间
报名截止时间
是否需要审核：是
```

如果平台不同，可以允许每个 Division 单独配置队伍上限。

## 6.1.6 步骤五：配置赛制

可选：

```text
常规赛 + 季后赛
小组赛 + 淘汰赛
直接淘汰赛
```

当前建议：

```text
League 默认 REGULAR_PLAYOFF
GROUP_KNOCKOUT 只预留，不开放完整生成
KNOCKOUT 可作为后续基础版
```

## 6.1.7 步骤六：配置比赛时间

字段：

```text
比赛开始时间
预计结束时间
比赛日规则
比赛时间段
每时间段最大场次
```

第一阶段可以继续默认：

```text
周二 / 周四 / 周六
21:00 / 22:00
每时间段最多 6 场
```

## 6.1.8 步骤七：预览并发布

预览信息：

```text
赛事系列：League
赛季：League 2026 S1
平台赛区：PS / Xbox
报名方式：球队报名
每赛区队伍上限：10
报名时间：xxx - xxx
比赛时间：xxx 起
赛制：常规赛 + 季后赛
```

确认后：

```text
保存 Season
保存多个 Division
发布到前台
```

---

# 6.2 前台赛事展示流程

## 6.2.1 赛事列表

前台赛事页面按 Series 展示：

```text
全部
League
Arena
Cup
Open
```

赛事卡片显示：

```text
赛事标题
赛事系列
年份 / 赛季
平台：PS / Xbox
报名状态
报名截止时间
报名方式：球队报名
赛制
```

## 6.2.2 赛事详情

进入赛事详情后，默认显示 Season 信息。

如果有多个 Division，则展示平台切换：

```text
PS
Xbox
PC
```

用户切换平台后，刷新以下内容：

```text
报名入口
参赛队伍
赛程
排行榜
比赛数据
```

## 6.2.3 报名入口

用户在某个 Division 下报名。

流程：

```text
选择 League 2026 S1
选择 PS 赛区
点击球队报名
校验队长权限
选择长期球队
选择 5–8 人阵容
提交报名
等待审核
```

---

# 6.3 球队报名流程

## 6.3.1 报名前置条件

```text
1. 用户已登录。
2. 用户已完成球员认证。
3. 用户拥有有效球员身份。
4. 用户是某长期球队的队长或赛事授权负责人。
5. 用户拥有当前 Division 的报名权限。
6. 当前 Division 处于报名中。
7. 当前 Division 未超过报名上限。
```

## 6.3.2 创建报名草稿

```text
division_id
team_id
captain_player_id
registration_mode = TEAM
status = DRAFT
```

## 6.3.3 选择阵容

队长从长期球队有效成员中选择：

```text
5–8 人
```

系统校验：

```text
认证状态
球队成员状态
平台身份
同赛区唯一参赛
队长是否在阵容中
```

## 6.3.4 提交报名

状态变化：

```text
DRAFT → SUBMITTED
```

后台可进入审核。

---

# 6.4 报名审核流程

## 6.4.1 后台审核列表

后台报名审核需要支持筛选：

```text
赛事系列
赛事赛季
平台赛区
报名状态
队伍名称
队长
```

## 6.4.2 审核通过

通过时校验：

```text
1. registration 状态允许审核。
2. division 仍允许报名或允许后台审核通过。
3. 当前 division 已通过队伍数量 < team_limit。
4. 阵容人数仍为 5–8。
5. 阵容成员没有在同 division 其他 EventTeam 中。
6. 长期球队没有在同 division 其他 approved registration 中。
```

审核通过后：

```text
registration.status = APPROVED
生成 EventTeam
生成 EventPlayer
锁定阵容快照
```

## 6.4.3 审核驳回

状态：

```text
SUBMITTED / REVIEWING → REJECTED
```

记录：

```text
reject_reason
reviewer_id
review_time
```

允许队长修改后重新提交。

## 6.4.4 报名撤回

允许：

```text
SUBMITTED / REVIEWING → WITHDRAWN
```

不允许：

```text
APPROVED → WITHDRAWN
```

---

# 6.5 赛程生成流程

## 6.5.1 生成维度

赛程必须按 Division 生成。

```text
League 2026 S1 - PS 单独生成赛程
League 2026 S1 - Xbox 单独生成赛程
```

禁止只按 Season 取全部 EventTeam 混排。

## 6.5.2 生成前置条件

```text
1. Division 已发布。
2. Division 报名已截止或后台允许提前生成。
3. Division 下有足够 EventTeam。
4. 所有 EventTeam 来自同一 division。
5. 当前没有已发布赛程，或后台明确选择重新生成草稿。
```

## 6.5.3 常规赛基础版

第一阶段建议先支持：

```text
REGULAR_PLAYOFF 的常规赛阶段基础版
```

即：

```text
按 Division 下 EventTeam 数量生成单循环赛程
奇数队自动轮空
按比赛日规则分配时间
生成草稿比赛
后台确认后发布
```

季后赛阶段先预留，不自动生成完整晋级树。

## 6.5.4 发布赛程

状态：

```text
DRAFT → PUBLISHED
```

前台只展示已发布赛程。

---

# 6.6 比赛与比赛数据流程

## 6.6.1 比赛展示

比赛详情展示：

```text
赛事系列
赛季
平台赛区
主队
客队
比赛时间
比分
比赛状态
双方数据提交状态
```

## 6.6.2 队长提交数据

提交维度：

```text
match_id + submit_event_team_id
```

每一方独立提交。

A 队提交审核中，不影响 B 队提交。

## 6.6.3 后台审核数据

审核通过后：

```text
保存本方提交记录
根据规则确认正式比分
同步球员数据
重建 division 维度统计
刷新排行榜
```

## 6.6.4 已结束比赛展示

已结束比赛必须仍展示在：

```text
赛事详情赛程
个人中心我的赛程
比赛数据列表
```

用于查看详情、补交数据、审核记录追溯。

---

# 6.7 个人中心流程

## 6.7.1 我的赛事

个人中心“我的赛事”建议继续保留：

```text
可参与赛事
我的报名
已参加赛事
我的赛程
```

## 6.7.2 可参与赛事

展示用户可报名的 Division：

```text
League 2026 S1 - PS
League 2026 S1 - Xbox
```

需要判断：

```text
用户是否认证
是否有平台身份
是否是队长 / 是否有授权
报名是否开放
名额是否已满
```

## 6.7.3 我的报名

展示当前用户作为队长提交的报名记录。

字段：

```text
赛事系列
赛季
平台赛区
球队
报名状态
审核结果
操作入口
```

## 6.7.4 已参加赛事

展示当前用户作为 EventPlayer 参与的 Division。

字段：

```text
赛事系列
赛季
平台赛区
EventTeam
角色
赛程入口
数据入口
```

## 6.7.5 我的赛程

汇总用户作为 EventPlayer 所属 EventTeam 的比赛。

默认：

```text
未开始 / 未比赛
按时间从近到远
```

可切换：

```text
全部赛程
```

查询维度：

```text
player_id / event_player_id
season_id
division_id
event_team_id
match_status
```

---

## 7. 后台菜单与页面建议

## 7.1 菜单建议

建议后台赛事域菜单统一到：

```text
CNPA 赛事管理
├── 赛事系列管理
├── 赛事赛季管理
├── 平台赛区管理
├── 报名审核
├── 正式参赛队伍
├── 正式参赛球员
├── 赛程管理
├── 比赛管理
├── 比赛数据审核
└── 数据统计 / 排行榜
```

## 7.2 赛事系列管理

用于维护：

```text
League / Arena / Cup / Open 的展示配置
```

## 7.3 赛事赛季管理

用于创建和管理 Season。

建议使用步骤式表单。

## 7.4 平台赛区管理

展示某 Season 下所有 Division。

可以支持：

```text
查看报名人数 / 队伍数
查看赛程状态
查看比赛状态
发布 / 下线
```

## 7.5 报名审核

必须支持按 Division 筛选。

## 7.6 赛程管理

必须先选择：

```text
Season
Division
```

再生成赛程。

## 7.7 比赛数据审核

必须展示：

```text
赛事系列
赛季
平台赛区
主客队 Logo
提交方
提交人
提交时间
审核状态
```

---

## 8. 接口设计方向

## 8.1 基本原则

新接口尽量避免：

```text
eventId
competition
```

建议明确使用：

```text
seriesId / seriesCode
seasonId
divisionId
eventTeamId
eventPlayerId
matchId
submissionId
```

## 8.2 前台公开接口

建议：

```text
GET /basket/public/series/list
GET /basket/public/season/page
GET /basket/public/season/{seasonId}
GET /basket/public/season/{seasonId}/divisions
GET /basket/public/division/{divisionId}/overview
GET /basket/public/division/{divisionId}/teams
GET /basket/public/division/{divisionId}/schedule/page
GET /basket/public/division/{divisionId}/standing
GET /basket/public/match/{matchId}
```

## 8.3 前台个人中心接口

建议：

```text
GET /basket/account/event/available-divisions
GET /basket/account/event/registrations
GET /basket/account/event/joined-divisions
GET /basket/account/event/my-schedules
POST /basket/account/event/division/{divisionId}/registration/draft
POST /basket/account/event/registration/{registrationId}/players
POST /basket/account/event/registration/{registrationId}/submit
POST /basket/account/event/registration/{registrationId}/withdraw
POST /basket/account/event/match/{matchId}/submission
```

## 8.4 后台接口

建议：

```text
GET /basket/admin/event-series/list
POST /basket/admin/event-season
PUT /basket/admin/event-season/{seasonId}
POST /basket/admin/event-season/{seasonId}/publish
GET /basket/admin/event-season/{seasonId}/divisions
POST /basket/admin/event-division
PUT /basket/admin/event-division/{divisionId}
GET /basket/admin/event-registration/page
POST /basket/admin/event-registration/{registrationId}/approve
POST /basket/admin/event-registration/{registrationId}/reject
POST /basket/admin/event-division/{divisionId}/schedule/generate
POST /basket/admin/event-division/{divisionId}/schedule/publish
GET /basket/admin/event-match/page
GET /basket/admin/event-match-submission/page
POST /basket/admin/event-match-submission/{submissionId}/approve
POST /basket/admin/event-match-submission/{submissionId}/reject
```

---

## 9. 旧赛事域处理策略

## 9.1 不再兼容旧结构

由于项目仍在开始阶段，本次建议：

```text
不迁移旧赛事业务数据
不兼容旧 eventId / seasonId / tournamentId
不继续围绕旧 tournament_season 修补
```

## 9.2 旧表处理方式

建议分阶段：

### 第一阶段

不删除旧表，只是新业务不再依赖。

```text
旧表保留
旧菜单隐藏
旧接口不再作为新页面数据源
```

### 第二阶段

新链路跑通后：

```text
清理旧菜单
清理旧接口入口
删除无用前端页面
确认无依赖后再考虑删除旧表
```

### 第三阶段

上线前：

```text
数据库做一次干净初始化
只保留新赛事域表
```

---

## 10. 分轮实施方案

# 第 0 轮：方案评审与共识确认

## 目标

不写代码，只确认方案。

## 输入

```text
本方案
Cursor 当前代码评估
老板新需求
```

## 输出

```text
最终确认的新模型
最终确认的表清单
最终确认的接口方向
最终确认的不做事项
最终确认的分轮计划
```

## 验收标准

```text
1. ChatGPT 与 Cursor 对 Series / Season / Division 理解一致。
2. 确认不兼容旧赛事三层结构。
3. 确认账号、球员、球队继续保留。
4. 确认当前只做 TEAM 报名。
5. 确认 PERSONAL 只预留不开放。
6. 确认第一期只做基础赛制。
```

---

# 第 1 轮：新赛事域数据模型设计

## 目标

建立新赛事域表结构和枚举。

## 主要内容

```text
basket_event_series
basket_event_season
basket_event_division
basket_event_registration
basket_event_registration_player
basket_event_team
basket_event_player
basket_event_match
basket_event_match_submission
统计相关表
```

## 需要 Cursor 评估

```text
1. 是否全部新建表，还是部分复用？
2. 表命名是否与现有项目规范冲突？
3. 是否需要保留原有字段命名习惯？
4. 是否需要统一 bigint ID / 逻辑删除 / create_time / update_time？
5. 是否需要为若依代码生成器准备表注释？
```

## 验收标准

```text
1. 新表能完整表达 Series → Season → Division 主线。
2. 所有比赛、统计、报名都能绑定 division_id。
3. 不再使用 eventId 作为 seasonId 的别名。
4. PERSONAL 字段存在但不会开放。
```

---

# 第 2 轮：后台赛事发布流程重做

## 目标

后台能够基于新模型发布赛事。

## 页面流程

```text
1. 选择赛事系列
2. 填写赛季信息
3. 选择平台
4. 配置报名规则
5. 配置赛制规则
6. 配置比赛时间
7. 预览并发布
```

## 主要内容

```text
后台赛事系列列表
后台赛事赛季列表
后台赛事发布页面
平台赛区配置
发布 / 草稿 / 下线
```

## 验收标准

```text
1. 可以创建 League 2026 S1。
2. 可以选择 PS / Xbox。
3. 系统生成两个 Division。
4. 每个 Division 可以配置队伍上限。
5. 当前只允许 TEAM 报名。
6. PERSONAL 选择时明确提示暂未开放。
```

---

# 第 3 轮：球队报名、审核、快照重做

## 目标

让球队报名完全基于 Division。

## 主要内容

```text
队长授权绑定 division 或 season+platform
球队报名草稿
报名阵容 5–8
提交报名
撤回报名
后台审核
审核通过生成 EventTeam / EventPlayer
```

## 验收标准

```text
1. PS 赛区报名只占 PS 名额。
2. Xbox 赛区报名只占 Xbox 名额。
3. 审核中不占名额。
4. 审核通过才占名额。
5. 审核通过后生成对应 Division 的 EventTeam / EventPlayer。
6. 同一 player 不能在同一 Division 多队参赛。
7. 队长授权不再是永久资格。
```

---

# 第 4 轮：赛程、比赛、比赛数据重做

## 目标

让赛程、比赛、比赛数据全部基于 Division。

## 主要内容

```text
按 Division 生成赛程
生成 Match
发布赛程
比赛详情
队长提交本方数据
后台审核比赛数据
同步比分
重建 Division 统计和排行榜
```

## 验收标准

```text
1. PS 和 Xbox 不会混排。
2. 赛程生成只读取当前 Division 的 EventTeam。
3. Match 必须绑定 division_id。
4. MatchSubmission 维度为 match_id + submit_event_team_id。
5. A 队审核中不影响 B 队提交。
6. 排行榜按 Division 独立统计。
```

---

# 第 5 轮：前台赛事详情和报名入口重做

## 目标

前台用户可以按新模型查看赛事并报名。

## 主要内容

```text
首页赛事模块
赛事列表
Series 筛选
Season 卡片
赛事详情
平台切换
参赛队伍
赛程
排行榜
报名入口
```

## 验收标准

```text
1. 前台展示 League / Arena / Cup / Open。
2. 赛事详情能展示 Season 信息。
3. 多平台赛事能切换 Division。
4. 切换 PS / Xbox 后，参赛队伍、赛程、榜单刷新。
5. 报名入口绑定 division_id。
```

---

# 第 6 轮：个人中心重做

## 目标

个人中心适配新赛事域。

## 主要内容

```text
可参与赛事
我的报名
已参加赛事
我的赛程
比赛数据入口
```

## 验收标准

```text
1. 可参与赛事展示 Division。
2. 我的报名按 Registration 查询。
3. 已参加赛事按 EventPlayer 查询。
4. 我的赛程按 EventPlayer/EventTeam/Match 查询。
5. 默认展示未开始比赛。
6. 可切换全部赛程。
7. 已结束比赛也能查看。
```

---

# 第 7 轮：旧代码清理与全链路联调

## 目标

完成新赛事域闭环，清理旧入口。

## 联调测试数据

```text
Series：League
Season：League 2026 S1
Division：PS / Xbox
报名方式：TEAM
PS 上限：10 支
Xbox 上限：10 支
```

## 测试链路

```text
后台创建 League 2026 S1
选择 PS / Xbox
发布赛事
队长进入 PS 赛区报名
队长进入 Xbox 赛区报名
后台审核报名
生成 EventTeam / EventPlayer
分别生成 PS / Xbox 赛程
发布赛程
前台查看赛事详情
个人中心查看我的赛事 / 我的赛程
比赛结束
队长提交本方数据
后台审核通过
排行榜刷新
```

## 验收标准

```text
1. PS / Xbox 报名不串。
2. PS / Xbox 赛程不串。
3. PS / Xbox 排行榜不串。
4. 个人中心数据不串。
5. 旧菜单不会误导运营。
6. 旧接口不会被新页面调用。
7. 构建通过。
8. 本地完整链路跑通。
```

---

## 11. 风险与控制

## 11.1 P0 风险

```text
eventId / seasonId / tournamentId 继续混用
未引入 division_id 导致多平台串数据
报名名额按 season 统计
赛程按 season 混排
排行榜按 season 混算
比赛数据状态上浮到整个赛事
个人报名半实现
```

## 11.2 P1 风险

```text
后台发布流程太复杂
前台平台切换状态混乱
个人中心查询链路改错
旧接口未清理导致新旧数据混用
测试数据不足
```

## 11.3 P2 风险

```text
Series 展示配置不够丰富
赛制模板不够灵活
排行榜样式不完善
后台页面体验不够好
```

## 11.4 控制原则

```text
1. 不进入编码前先达成模型共识。
2. 每一轮只做一个主目标。
3. 不在同一轮同时做后台、前台、赛程、统计大改。
4. 每轮结束必须有可验证链路。
5. Cursor 不要默认生成 docs 报告，除非明确要求。
6. 复杂赛制和个人报名全部后置。
```

---

## 12. 需要 Cursor 重点评估的问题

请 Cursor 基于真实代码评估以下问题：

```text
1. 是否同意废弃旧 competition_season → tournament → tournament_season 赛事三层结构？
2. 新建 basket_event_* 表是否比复用旧表更稳？
3. 哪些旧表可以保留但不再依赖？
4. 哪些旧接口和旧页面必须隐藏？
5. 是否需要保留旧 match / submission 表，还是新建 basket_event_match / basket_event_match_submission？
6. 队长授权应该绑定 division_id，还是 season_id + platform_code？
7. EventTeam / EventPlayer 新表如何与长期球队、永久球员、平台身份关联最稳？
8. 赛程生成器是否建议重写，还是改造现有生成器？
9. 比赛数据审核和统计结算是否建议新建一套，还是改造现有 service？
10. 前台路由是否建议重做，避免 :tournamentId 实际传 seasonId？
11. 后台菜单是否建议直接切换到新菜单，不保留旧菜单？
12. 是否需要一次性删除旧代码，还是先隐藏旧入口？
13. 每一轮预计风险和工作量如何？
14. 是否建议把第 1 轮先控制在 SQL + Entity + Enum，不碰业务？
15. 是否有更好的表命名建议？
```

---

## 13. Cursor 评审输出格式建议

请 Cursor 不要改代码，只输出评审意见，格式如下：

```text
1. 总体结论
   - 是否赞同新赛事域重建
   - 是否建议全部新建 basket_event_* 表
   - 是否有更稳的替代方案

2. 模型评审
   - Series 是否合理
   - Season 是否合理
   - Division 是否合理
   - Registration 是否合理
   - EventTeam / EventPlayer 是否合理
   - Match / Submission 是否合理

3. 表设计评审
   - 建议保留字段
   - 建议删除字段
   - 建议新增字段
   - 唯一索引建议
   - 普通索引建议

4. 流程评审
   - 后台发布流程
   - 球队报名流程
   - 审核快照流程
   - 赛程生成流程
   - 比赛数据流程
   - 个人中心流程

5. 分轮评审
   - 是否认可 7 轮
   - 是否需要拆成更多轮
   - 哪些轮可以合并
   - 哪些轮必须拆开

6. 风险评审
   - P0 风险
   - P1 风险
   - P2 风险

7. 建议第一轮开发边界
   - 第一轮只做什么
   - 第一轮不做什么
   - 第一轮验收标准

8. 需要产品确认的问题
```

---

## 14. 当前建议的最终共识方向

在 Cursor 评审前，当前建议暂定方向如下：

```text
1. 赛事域重建，不兼容旧三层赛事结构。
2. 账号、球员、长期球队保留。
3. 新赛事域统一采用 Series → Season → Division 主线。
4. 新业务统一使用 division_id 隔离平台赛区。
5. 当前只开放 TEAM 球队报名。
6. PERSONAL 只预留字段，不开放流程。
7. 所有报名最终都生成 EventTeam / EventPlayer 快照。
8. 赛程、比赛、比赛数据、统计、排行榜全部基于 Division。
9. 旧赛事菜单和接口先隐藏，不立刻删除。
10. 等新链路跑通后，再清理旧赛事代码和旧表。
```

---

## 15. 第一轮开发前的确认清单

进入第一轮编码前，必须确认：

```text
1. 表前缀是否确定为 basket_event_*？
2. Series 是否用表 + 枚举？
3. Season 是否不再依赖 basket_tournament？
4. Division 是否作为平台赛区唯一边界？
5. 队长授权是否直接绑定 division_id？
6. Match 是否新建 basket_event_match？
7. Submission 是否新建 basket_event_match_submission？
8. 旧后台菜单是否先隐藏？
9. 旧前台赛事页面是否重做还是逐步替换？
10. 第一阶段是否只支持 REGULAR_PLAYOFF 基础常规赛？
11. GROUP_KNOCKOUT 是否只预留？
12. PERSONAL 是否只预留并拦截？
13. 当前旧测试数据是否可以清空？
14. 本地数据库是否允许重新初始化？
```

---

## 16. 推荐下一步动作

下一步不是让 Cursor 改代码，而是把本方案发给 Cursor，要求它：

```text
基于当前代码评审本方案。
不要改代码。
不要新增 docs。
不要写 SQL。
只输出同意/不同意/风险/调整建议。
重点评价表模型、分轮、旧代码处理策略。
```

等 Cursor 给出评审意见后，再由 ChatGPT 整理第二版方案，直到双方达成共识，再进入第一轮开发。

