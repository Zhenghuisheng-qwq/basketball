# CNPA 赛事域 V4 全量重构方案（第二版）

> 日期：2026-08-03  
> 分支：`basketball-20260803`  
> 状态：方案评审稿 V2  
> 本稿定位：用于继续交给 Cursor 做只读评审，不用于直接开发  
> 当前决策：赛事域全量重构；不迁旧赛事数据；不兼容旧三层赛事结构；非赛事基础资产继续保留

---

## 0. 第二版修订说明

本版基于上一版方案与 Cursor 第二次评审意见修订。

本版确认以下前提：

```text
1. 项目仍处开始阶段，赛事域可以全量重构。
2. 不迁移旧赛事业务数据。
3. 不兼容旧 competition_season → tournament → tournament_season 三层结构。
4. 不再保留 eventId = seasonId、tournamentId 装 seasonId 等旧 ID 语义。
5. 新赛事域以 Series → Season → Division 为唯一主线。
6. 账号、登录、球员认证、平台身份、长期球队、球队成员等非赛事域资产继续保留。
7. 当前只开放 TEAM 球队报名。
8. PERSONAL / Open 个人报名只预留并拦截，不开放，不半实现。
```

本版同时吸收 Cursor 的工程提醒：

```text
1. 全量重构不代表一轮写完所有模块，必须按切片分轮验收。
2. 新代码可以参考旧代码中的业务规则和算法，但新主路径不得依赖旧赛事表、旧接口、旧页面。
3. 必须列清清库白名单和保留资产白名单。
4. 必须列清本期重建、本期预留、本期删除入口。
5. 必须避免继续制造 eventId / seasonId / tournamentId 命名歧义。
6. 必须补齐赛程 batch、stage、submission 子表、统计、Open、Job、争议异常等周边模块的处理策略。
```

---

# 1. 重构目标

## 1.1 产品目标

老板的新赛事发布模型是：

```text
先有固定赛事系列：League / Arena / Cup / Open
再在某个赛事系列下发布具体赛季：League 2026 S1
一个赛季可以选择多个平台：PS / Xbox / PC
每个平台形成独立平台赛区 Division
每个 Division 独立报名、独立赛程、独立榜单、独立比赛数据
```

目标业务主线：

```text
Series 赛事系列
  ↓
Season 赛事赛季
  ↓
Division 平台赛区
  ↓
CaptainGrant 队长授权
  ↓
Registration 报名
  ↓
EventTeam / EventPlayer 正式参赛快照
  ↓
Schedule / Match 赛程比赛
  ↓
MatchSubmission 比赛数据提交
  ↓
Audit 审核
  ↓
OfficialResult 正式结果
  ↓
Standing / Stats / Rank 榜单统计
```

---

## 1.2 技术目标

本次技术目标不是在旧赛事域上打补丁，而是建立一条新的、清晰的赛事域主路径：

```text
1. 新表承载新赛事域。
2. 新接口只使用新 ID 语义。
3. 新后台页面只调用新接口。
4. 新前台赛事页面只读取新模型。
5. 旧赛事表、旧接口、旧页面只作为参考，不作为运行路径。
6. 旧赛事入口最终删除。
7. 所有多平台隔离统一依靠 division_id。
```

---

# 2. 重构边界

## 2.1 本次重建范围

以下属于赛事域，本次进入全量重构范围：

```text
赛事系列 Series
赛事赛季 Season
平台赛区 Division
赛事队长授权 CaptainGrant
球队报名 Registration
报名阵容 RegistrationPlayer
报名审核 RegistrationAudit
赛事球队快照 EventTeam
赛事球员快照 EventPlayer
赛事阶段 Stage
赛事分组 Group
赛事轮次 Round
赛程生成批次 ScheduleBatch
比赛 Match
比赛节比分 MatchPeriodScore
比赛数据提交 MatchSubmission
提交球队统计 MatchSubmissionTeamStat
提交球员统计 MatchSubmissionPlayerStat
提交材料 MatchSubmissionMaterial
比赛数据审核 MatchSubmissionAudit
正式结果 OfficialResult
积分榜 Standing
球队统计 TeamStat
球员统计 PlayerStat
排行榜 Rank
后台赛事发布与管理页面
后台报名审核页面
后台赛程管理页面
后台比赛数据审核页面
前台赛事列表 / 详情 / 报名 / 赛程 / 榜单
个人中心我的赛事 / 我的报名 / 已参加赛事 / 我的赛程
赛事相关 Demo 数据 / 自动任务 / 测试用例
```

---

## 2.2 本次保留范围

以下非赛事基础资产继续保留，不参与推翻重建：

```text
用户账号
邮箱注册登录
球员认证
永久球员档案
球员平台身份
长期球队
球队成员关系
后台用户
后台角色
后台权限基础能力
若依基础菜单能力
系统字典基础能力
文件上传
公告
合作伙伴 / 赞助墙
基础布局样式
基础工具类
通用响应结构
通用分页结构
```

---

## 2.3 本期明确不做

以下内容本期不做完整实现，只预留或隐藏：

```text
PERSONAL 个人报名完整流程
Open 个人报名旧流程接入新模型
后台组队
系统自动组队
个人报名池
小组赛完整晋级规则
季后赛完整晋级树
BO3 / BO5
种子队
复杂轮空规则
完整处罚体系
球员申诉中心
复杂荣誉自动生成
名人堂自动入选
直播 / 视频 / 回放
支付 / 商城
App / 小程序
```

---

# 3. 核心术语与 ID 规范

## 3.1 术语定义

| 术语 | 中文 | 含义 |
|------|------|------|
| Series | 赛事系列 | 固定赛事品牌，League / Arena / Cup / Open |
| Season | 赛事赛季 | 某个 Series 下的一期具体赛事，如 League 2026 S1 |
| Division | 平台赛区 | 某个 Season 下的具体平台分区，如 League 2026 S1 - PS |
| CaptainGrant | 队长授权 | 某球员在某 Division 下作为队长/负责人报名的授权 |
| Registration | 报名 | 某球队报名某个 Division |
| EventTeam | 赛事球队快照 | 审核通过后生成的正式参赛球队快照 |
| EventPlayer | 赛事球员快照 | 审核通过后生成的正式参赛球员快照 |
| Match | 比赛 | 某 Division 下两个 EventTeam 的一场比赛 |
| Submission | 数据提交 | 某场比赛某一方 EventTeam 提交的比赛数据 |
| OfficialResult | 正式结果 | 数据审核通过后沉淀的正式比分和结果 |
| Standing | 积分榜 | 某 Division 下的战绩/积分排名 |

---

## 3.2 ID 命名规范

新代码中必须统一使用以下命名：

```text
seriesId      赛事系列 ID
seasonId      赛事赛季 ID
divisionId    平台赛区 ID
captainGrantId 队长授权 ID
registrationId 报名 ID
eventTeamId   赛事球队快照 ID
eventPlayerId 赛事球员快照 ID
matchId       比赛 ID
submissionId  比赛数据提交 ID
```

禁止继续使用：

```text
eventId 表示 seasonId
tournamentId 表示 seasonId
competition 表示 tournamentId 或 seasonId
```

新接口中不得出现含糊语义：

```text
/event/{eventId}
/tournament/{tournamentId} 但实际传 seasonId
/competition?competition=xxx 但实际是 tournamentId
```

---

# 4. 表命名规范

## 4.1 最终推荐表前缀

为了避免 `eventId = seasonId` 的旧歧义，本版不再建议全量使用 `basket_event_*` 前缀。

最终推荐采用：

```text
基础聚合：basket_series / basket_season / basket_division
快照对象：basket_event_team / basket_event_player
报名对象：basket_registration / basket_registration_player
比赛对象：basket_match / basket_match_submission / basket_match_official_result
统计对象：basket_standing / basket_team_stat / basket_player_stat / basket_rank
```

命名原则：

```text
1. Series / Season / Division 不使用 event 前缀。
2. EventTeam / EventPlayer 保留 event 业务名，因为这是正式参赛快照的业务术语。
3. Match / Submission 不强行挂 event 前缀，避免名字过长。
4. 所有表仍以 basket_ 开头，保持项目数据库风格。
5. 所有新表统一使用 id / create_time / update_time / is_delete。
6. 所有新表字段使用下划线命名。
7. Java 实体使用驼峰命名。
```

---

## 4.2 新表总清单

### P0 主链路表

```text
basket_series
basket_season
basket_division
basket_captain_grant
basket_registration
basket_registration_player
basket_event_team
basket_event_player
basket_schedule_batch
basket_match
basket_match_period_score
basket_match_submission
basket_match_submission_team_stat
basket_match_submission_player_stat
basket_match_submission_material
basket_match_submission_audit_log
basket_match_official_result
basket_standing
basket_team_stat
basket_player_stat
basket_rank
```

### P1 扩展表，本期可建空表或后续补

```text
basket_stage
basket_stage_group
basket_round
basket_match_dispute
basket_match_exception
basket_roster_change_request
basket_event_honor
basket_stat_settlement_batch
basket_stat_settlement_log
```

### P2 暂不建或仅预留枚举

```text
basket_personal_registration
basket_personal_registration_pool
basket_team_formation_task
```

---

# 5. 新表设计建议

> 本节是方案级字段设计，不是最终 SQL。最终 SQL 由 Cursor 在第 1 轮根据项目字段规范输出。

---

## 5.1 basket_series：赛事系列

业务含义：固定赛事体系，League / Arena / Cup / Open。

核心字段：

```text
id
series_code          LEAGUE / ARENA / CUP / OPEN，唯一
series_name          League / Arena / Cup / Open
series_title         展示标题
series_desc          系列说明
default_format_type  默认赛制
cover_url            默认封面
banner_url           默认 Banner
icon_url             图标
sort_order           排序
enabled              是否启用：1启用 0禁用
remark
create_time
update_time
is_delete
```

约束：

```text
series_code 唯一
代码层固定四种 code
后台可以改展示信息，不允许随意新增第五种
```

---

## 5.2 basket_season：赛事赛季

业务含义：某个 Series 下发布的一期赛事。

示例：

```text
League 2026 S1
Cup 2026 春季杯
Open 2026 第一期
```

核心字段：

```text
id
series_id
season_year          2026
season_code          LEAGUE_2026_S1，可选，唯一
season_name          League S1
display_title        CNPA League 2026 S1
season_desc          赛事说明
cover_url
banner_url
registration_start_time
registration_end_time
match_start_time
match_end_time
registration_mode    TEAM / PERSONAL，当前强制 TEAM
publish_status       DRAFT / PUBLISHED / OFFLINE
season_status        NOT_STARTED / REGISTRATION / REGISTRATION_ENDED / PLAYING / FINISHED / CANCELLED
remark
create_time
update_time
is_delete
```

规则：

```text
1. Season 不直接保存多平台字符串。
2. 平台由 Division 表承载。
3. registration_mode 当前只能保存 TEAM。
4. PERSONAL 只作为枚举预留，后台不可选择，接口传入时直接拦截。
```

---

## 5.3 basket_division：平台赛区

业务含义：某个 Season 下某个平台的独立赛区。

示例：

```text
League 2026 S1 - PS
League 2026 S1 - Xbox
```

核心字段：

```text
id
season_id
platform_code        PC / PS / XBOX
division_name        PS赛区 / Xbox赛区
registration_mode    TEAM / PERSONAL，当前强制 TEAM
team_limit           队伍上限
personal_limit       个人上限，当前预留
lineup_min_count     默认 5
lineup_max_count     默认 8
format_type          REGULAR_PLAYOFF / GROUP_KNOCKOUT / KNOCKOUT
schedule_rule_code   REGULAR_ROUND_ROBIN_V1 等
registration_start_time 可为空，默认继承 season
registration_end_time   可为空，默认继承 season
match_start_time        可为空，默认继承 season
match_end_time          可为空，默认继承 season
publish_status       DRAFT / PUBLISHED / OFFLINE
division_status      NOT_STARTED / REGISTRATION / REGISTRATION_ENDED / PLAYING / FINISHED / CANCELLED
sort_order
remark
create_time
update_time
is_delete
```

约束：

```text
同一 season 下 platform_code 唯一
所有报名、赛程、比赛、统计都必须绑定 division_id
```

产品规则：

```text
一个 Season 多选 PS / Xbox / PC 时，系统生成多个 Division。
每个 Division 独立报名名额、独立赛程、独立榜单。
不同 Division 不互相比赛。
```

---

## 5.4 basket_captain_grant：队长授权

业务含义：某个球员在某个 Division 下拥有队长/赛事负责人资格。

核心字段：

```text
id
division_id
season_id            冗余，便于查询
player_id
member_id            如当前系统有登录用户/会员映射，可保留
team_id              可为空；如果授权到具体长期球队则填写
grant_status         VALID / EXPIRED / REVOKED
valid_from
valid_to
granted_by
granted_time
revoked_by
revoked_time
remark
create_time
update_time
is_delete
```

规则：

```text
1. 队长授权无永久资格。
2. 队长授权直接绑定 division_id。
3. 某球员只有在对应 division 授权有效时，才可以作为队长提交该赛区报名。
4. 是否授权到 team_id 需要根据产品决定；建议支持 team_id 可为空，便于先授权人，报名时再选择自己所属长期球队。
```

---

## 5.5 basket_registration：球队报名

业务含义：某长期球队报名某个 Division。

核心字段：

```text
id
division_id
season_id            冗余，便于查询
registration_no      报名编号
registration_mode    TEAM，当前只允许 TEAM
team_id              长期球队 ID
captain_player_id
captain_member_id
status               DRAFT / PENDING_REVIEW / APPROVED / REJECTED / WITHDRAWN / CANCELLED
lineup_count
submit_time
withdraw_time
review_by
review_time
reject_reason
remark
create_time
update_time
is_delete
```

状态建议：

```text
DRAFT           草稿
PENDING_REVIEW  待审核，对外统一叫审核中
APPROVED        审核通过
REJECTED        已驳回
WITHDRAWN       已撤回
CANCELLED       已取消
```

注意：

```text
不再强制拆 SUBMITTED / REVIEWING 两个对外状态。
用户提交后直接进入 PENDING_REVIEW。
后台审核列表展示 PENDING_REVIEW。
审核通过后才占 division 名额。
PENDING_REVIEW 不占名额。
APPROVED 后不可撤回。
```

---

## 5.6 basket_registration_player：报名阵容

业务含义：报名时选择的 5–8 人阵容。

核心字段：

```text
id
registration_id
division_id
season_id
team_id
player_id
member_id
platform_identity_id
platform_code
game_id_snapshot
nickname_snapshot
position
jersey_no
is_captain
is_starter
sort_order
create_time
update_time
is_delete
```

规则：

```text
1. 阵容人数 5–8。
2. 不强制五个位置齐全。
3. 球员必须已认证。
4. 球员必须属于该长期球队有效成员。
5. 球员必须具备当前 division 平台身份。
6. 报名审核通过后，从该表生成 EventPlayer 快照。
```

---

## 5.7 basket_event_team：赛事球队快照

业务含义：审核通过后生成的正式参赛球队。

核心字段：

```text
id
division_id
season_id
registration_id
team_id              长期球队 ID
event_team_no        赛事内队伍编号
team_name_snapshot
team_logo_snapshot
captain_player_id
captain_member_id
platform_code
status               ACTIVE / WITHDRAWN / DISQUALIFIED / ARCHIVED
approved_time
create_time
update_time
is_delete
```

规则：

```text
1. EventTeam 是正式参赛对象。
2. 赛程、比赛、排行榜只引用 EventTeam，不直接引用长期球队。
3. 同一 division 下，同一长期球队只能有一个有效 EventTeam。
4. 同一长期球队允许报名同一 Season 下不同 Division，例如 PS 与 Xbox 各一队，前提是产品允许。
```

建议产品最终口径：

```text
允许同一长期球队报名同一 Season 的多个 Division。
原因：PS / Xbox 是独立赛区，阵容平台身份不同，赛程和榜单独立。
```

---

## 5.8 basket_event_player：赛事球员快照

业务含义：某 Division 下某 EventTeam 的正式参赛球员快照。

核心字段：

```text
id
division_id
season_id
event_team_id
team_id
player_id
member_id
platform_identity_id
platform_code
game_id_snapshot
nickname_snapshot
avatar_snapshot
position
jersey_no
is_captain
is_starter
status               ACTIVE / REMOVED / SUSPENDED / ARCHIVED
create_time
update_time
is_delete
```

规则：

```text
1. 比赛名单、数据统计基于 EventPlayer。
2. 长期球队后续改名、换 Logo、成员变更，不影响历史快照。
3. 同一 player_id 在同一 division 下只能属于一个有效 EventTeam。
4. 不再使用 seasonId_playerId 作为唯一槽位，必须以 division_id 为边界。
```

---

## 5.9 basket_schedule_batch：赛程生成批次

业务含义：一次赛程生成任务。

核心字段：

```text
id
division_id
season_id
batch_no
schedule_rule_code
format_type
team_count
match_count
publish_status       DRAFT / PUBLISHED / CANCELLED
generate_status      PROCESSING / SUCCESS / FAILED
generated_by
generated_time
published_by
published_time
error_message
create_time
update_time
is_delete
```

规则：

```text
1. 赛程只能按 division 生成。
2. 同一 division 同一阶段只能有一个有效已发布批次。
3. 草稿赛程可以删除重生成。
4. 发布后前台可见。
```

---

## 5.10 basket_match：比赛

业务含义：某 Division 下的一场比赛。

核心字段：

```text
id
division_id
season_id
schedule_batch_id
stage_code           REGULAR / PLAYOFF / GROUP / KNOCKOUT，当前 REGULAR 为主
round_no
match_no
home_event_team_id
away_event_team_id
match_time
venue_name
platform_code
match_status         NOT_STARTED / PLAYING / FINISHED / CANCELLED / POSTPONED / FORFEITED
publish_status       DRAFT / PUBLISHED / HIDDEN
result_status        NONE / PENDING_REVIEW / CONFIRMED / DISPUTED / VOIDED
home_score
away_score
winner_event_team_id
remark
create_time
update_time
is_delete
```

规则：

```text
1. Match 必须绑定 division_id。
2. home_event_team_id / away_event_team_id 必须属于同一个 division。
3. 不允许跨平台比赛。
4. 已结束比赛仍要在比赛数据列表中展示。
5. match_status、result_status、submission_status 不得混用。
```

---

## 5.11 basket_match_period_score：节比分

核心字段：

```text
id
match_id
division_id
period_no
home_score
away_score
create_time
update_time
is_delete
```

---

## 5.12 basket_match_submission：比赛数据提交

业务含义：某场比赛某一方 EventTeam 提交的数据。

核心字段：

```text
id
match_id
division_id
season_id
submit_event_team_id
opponent_event_team_id
submitted_by_member_id
submitted_by_player_id
submitter_role       CAPTAIN / ADMIN
version_no
status               DRAFT / PENDING_REVIEW / APPROVED / REJECTED / SUPERSEDED / CANCELLED
home_score
away_score
submit_time
review_by
review_time
reject_reason
is_current           是否当前版本
remark
create_time
update_time
is_delete
```

规则：

```text
1. 提交维度是 match_id + submit_event_team_id。
2. 一方审核中不影响另一方提交。
3. 驳回后允许重新提交，新版本 version_no +1。
4. 审核通过后旧版本保留。
5. 审核通过后才进入 OfficialResult / Standing / Stats / Rank。
6. 未审核数据不公开。
```

---

## 5.13 submission 子表

### basket_match_submission_team_stat

```text
id
submission_id
match_id
division_id
event_team_id
points
rebounds
assists
steals
blocks
turnovers
fouls
field_goals
three_points
free_throws
create_time
update_time
is_delete
```

### basket_match_submission_player_stat

```text
id
submission_id
match_id
division_id
event_team_id
event_player_id
points
rebounds
assists
steals
blocks
turnovers
fouls
minutes
field_goals
three_points
free_throws
create_time
update_time
is_delete
```

### basket_match_submission_material

```text
id
submission_id
match_id
division_id
material_type        SCREENSHOT / VIDEO / OTHER
material_url
remark
create_time
update_time
is_delete
```

### basket_match_submission_audit_log

```text
id
submission_id
match_id
division_id
audit_action         SUBMIT / APPROVE / REJECT / RESUBMIT / CANCEL
audit_by
audit_time
audit_remark
create_time
update_time
is_delete
```

---

## 5.14 basket_match_official_result：正式结果

业务含义：审核通过后沉淀的正式比赛结果。

核心字段：

```text
id
match_id
division_id
season_id
source_submission_id
home_event_team_id
away_event_team_id
home_score
away_score
winner_event_team_id
result_type          NORMAL / FORFEIT / CANCELLED / WALKOVER / VOIDED
confirmed_by
confirmed_time
settlement_status    PENDING / SETTLED / FAILED
create_time
update_time
is_delete
```

规则：

```text
1. 同一 match 只能有一个当前有效正式结果。
2. 审核通过要幂等，不能重复累计。
3. 如后续修订，旧正式结果保留但不参与当前统计。
```

---

## 5.15 榜单与统计表

### basket_standing：积分榜

```text
id
division_id
season_id
event_team_id
rank_no
wins
losses
draws
points_for
points_against
point_diff
standing_points
settlement_batch_id
create_time
update_time
is_delete
```

### basket_team_stat：球队统计

```text
id
division_id
season_id
event_team_id
matches_played
wins
losses
total_points
total_rebounds
total_assists
total_steals
total_blocks
total_turnovers
create_time
update_time
is_delete
```

### basket_player_stat：球员统计

```text
id
division_id
season_id
event_team_id
event_player_id
matches_played
total_points
total_rebounds
total_assists
total_steals
total_blocks
total_turnovers
avg_points
avg_rebounds
avg_assists
create_time
update_time
is_delete
```

### basket_rank：排行榜

```text
id
division_id
season_id
rank_type            TEAM_WIN / PLAYER_SCORE / PLAYER_REBOUND / PLAYER_ASSIST 等
rank_target_type     TEAM / PLAYER
rank_target_id
rank_no
rank_value
create_time
update_time
is_delete
```

---

# 6. 状态枚举

## 6.1 SeriesStatus

```text
ENABLED
DISABLED
```

## 6.2 PublishStatus

```text
DRAFT       草稿
PUBLISHED   已发布
OFFLINE     已下线
HIDDEN      已隐藏
```

## 6.3 SeasonStatus / DivisionStatus

```text
NOT_STARTED           未开始
REGISTRATION          报名中
REGISTRATION_ENDED    报名截止
PLAYING               比赛中
FINISHED              已结束
CANCELLED             已取消
```

## 6.4 RegistrationMode

```text
TEAM        球队报名，当前开放
PERSONAL    个人报名，当前只预留，不开放
```

## 6.5 RegistrationStatus

```text
DRAFT
PENDING_REVIEW
APPROVED
REJECTED
WITHDRAWN
CANCELLED
```

## 6.6 FormatType

```text
REGULAR_PLAYOFF     常规赛 + 季后赛，第一阶段只实现常规赛基础版
GROUP_KNOCKOUT      小组赛 + 淘汰赛，当前预留
KNOCKOUT            直接淘汰赛，当前可预留或后续实现
```

## 6.7 ScheduleRuleCode

```text
REGULAR_ROUND_ROBIN_V1   常规赛单循环基础版
```

## 6.8 MatchStatus

```text
NOT_STARTED
PLAYING
FINISHED
POSTPONED
CANCELLED
FORFEITED
```

## 6.9 SubmissionStatus

```text
DRAFT
PENDING_REVIEW
APPROVED
REJECTED
SUPERSEDED
CANCELLED
```

## 6.10 ResultStatus

```text
NONE
PENDING_REVIEW
CONFIRMED
DISPUTED
VOIDED
```

---

# 7. 核心业务流程

## 7.1 后台发布赛事流程

步骤式发布：

```text
1. 选择赛事系列
   League / Arena / Cup / Open

2. 填写赛季基础信息
   年份
   赛季名称
   展示标题
   简介
   封面 / Banner

3. 选择平台
   PC / PS / Xbox
   多选后生成多个 Division

4. 配置报名规则
   当前只允许 TEAM
   队伍上限
   阵容人数 5–8
   报名开始 / 截止时间

5. 配置赛制规则
   REGULAR_PLAYOFF，当前基础常规赛
   GROUP_KNOCKOUT 置灰/预留
   KNOCKOUT 置灰/后续

6. 配置比赛时间
   比赛开始时间
   比赛日规则
   时间段规则
   默认：周二/周四/周六，21:00/22:00，每时段最多 6 场

7. 预览并发布
   发布 Season
   发布 Division
   前台可见
```

发布结果示例：

```text
Series: League
Season: League 2026 S1
Divisions:
  - PS赛区，TEAM，10支，REGULAR_PLAYOFF
  - Xbox赛区，TEAM，10支，REGULAR_PLAYOFF
```

---

## 7.2 队长授权流程

```text
后台选择 Season
后台选择 Division
后台选择已认证球员
后台授予该 Division 队长授权
授权有效期可选
授权后该球员可以为其长期球队提交该 Division 报名
```

校验：

```text
1. 球员必须已认证。
2. 授权必须绑定 division_id。
3. 当前日期必须在授权有效期内。
4. 授权状态必须 VALID。
5. 无永久队长资格。
```

---

## 7.3 球队报名流程

```text
队长进入前台赛事详情
选择目标 Division，例如 PS 赛区
系统校验队长授权
系统校验报名时间
系统校验 Division 状态
系统校验报名方式为 TEAM
队长选择长期球队
队长选择 5–8 名有效成员
队长提交报名
报名状态进入 PENDING_REVIEW
```

名额规则：

```text
1. 只统计 APPROVED。
2. PENDING_REVIEW 不占名额。
3. REJECTED / WITHDRAWN 不占名额。
4. 名额按 division_id 统计，不按 season 统计。
5. 达到 team_limit 后，新的审核通过操作必须被拦截。
```

---

## 7.4 报名审核与快照生成流程

```text
后台进入报名审核
按 Season / Division 筛选报名
审核通过时：
  1. 校验 Division 名额
  2. 校验报名状态为 PENDING_REVIEW
  3. 校验阵容 5–8
  4. 校验球员平台身份
  5. 创建 EventTeam 快照
  6. 创建 EventPlayer 快照
  7. 报名状态改 APPROVED
  8. 写审核日志
```

幂等要求：

```text
1. 同一 registration 只能生成一次有效 EventTeam。
2. 重复点击审核通过不能重复占名额。
3. 同一 division 下同一 player 只能进入一个有效 EventTeam。
4. 同一 division 下同一 team 只能有一个有效 EventTeam。
```

---

## 7.5 赛程生成流程

第一阶段只实现基础常规赛：

```text
规则：REGULAR_ROUND_ROBIN_V1
范围：某一个 Division
参赛对象：该 Division 下 ACTIVE EventTeam
生成方式：单循环
奇数队：支持 BYE
时间规则：周二 / 周四 / 周六，21:00 / 22:00，每时段最多 6 场
发布流程：先草稿，再发布
```

流程：

```text
后台选择 Season
后台选择 Division
点击生成赛程
系统读取该 Division EventTeam
生成 ScheduleBatch
生成 Match 草稿
管理员检查后发布
发布后前台可见
```

禁止：

```text
禁止按 season 全量 EventTeam 生成赛程
禁止跨 Division 排赛
禁止 PS 队伍和 Xbox 队伍互相比赛
```

---

## 7.6 比赛数据提交流程

```text
比赛结束后
队长进入我的赛程 / 比赛详情
选择本方 EventTeam
提交本方比赛数据
系统生成 basket_match_submission
状态 PENDING_REVIEW
后台审核
```

关键规则：

```text
1. 提交维度：matchId + submitEventTeamId。
2. 一方提交审核中，不影响另一方提交。
3. 已结束比赛仍展示在比赛数据列表中。
4. 未审核数据不进入统计。
5. 审核通过后才沉淀 OfficialResult。
6. 驳回后可重新提交，版本递增。
```

---

## 7.7 数据审核与统计结算流程

```text
后台进入比赛数据审核
查看双方队伍、Logo、提交人、提交时间、比分、球员数据、材料
审核通过
生成或更新 OfficialResult
重算 Standing
重算 TeamStat
重算 PlayerStat
重算 Rank
写审核日志和结算批次
```

结算规则：

```text
1. 结算范围必须是 division_id。
2. 同一比赛不能重复累计。
3. 只有 APPROVED submission 参与正式结果。
4. 旧版本数据保留但不参与当前统计。
5. 排行榜只来源于正式有效结果。
```

---

# 8. 前台页面流程

## 8.1 赛事列表

页面结构：

```text
全部 / League / Arena / Cup / Open
```

赛事卡片展示：

```text
Series
Season 标题
年份
平台：PS / Xbox / PC
报名方式：球队报名
报名状态
报名截止时间
赛制
```

---

## 8.2 赛事详情

详情上下文：

```text
seasonId
divisionId
```

顶部展示：

```text
CNPA League 2026 S1
Series：League
平台赛区：PS / Xbox 切换
报名状态
报名时间
比赛时间
赛制
```

平台切换：

```text
切换 Division 后刷新：
参赛队伍
赛程
榜单
比赛数据
报名入口
```

---

## 8.3 报名入口

```text
未登录：提示登录
未认证球员：提示先认证
无队长授权：提示无当前赛区队长资格
报名未开始：显示未开始
报名已截止：显示已截止
PERSONAL：显示暂未开放
TEAM：进入球队报名流程
```

---

## 8.4 个人中心

个人中心仍保持一级入口收敛，不新增独立左侧菜单。

建议在“我的赛事”中保持四个页签：

```text
可参与赛事
我的报名
已参加赛事
我的赛程
```

各页签基于新模型：

```text
可参与赛事：展示可报名的 Season + Division
我的报名：展示当前用户队长提交过的 Registration
已参加赛事：展示用户作为 EventPlayer 所属的 EventTeam / Division
我的赛程：展示用户所属 EventTeam 的 Match，默认未开始，从近到远；支持全部赛程
```

---

# 9. 后台页面流程

## 9.1 菜单建议

一级仍放在：

```text
CNPA 赛事管理 / 运营工作台
```

新赛事域菜单：

```text
赛事系列管理
赛事赛季管理
平台赛区管理
队长授权
球队报名审核
正式参赛球队
正式参赛球员
赛程生成与发布
比赛管理
比赛数据审核
数据榜单
```

旧入口处理：

```text
开发期：旧菜单可暂留但不推荐进入，或者直接隐藏。
新链路完成后：删除旧赛事菜单、旧 Open 入口、旧赛程入口。
不要让运营同时看到新旧两套赛事入口。
```

---

## 9.2 后台接口路径建议

为了避免与现有 `/basket/admin/...` 风格不一致，本版不强制新增 `/basket/admin` 前缀。

建议采用：

```text
/basket/series
/basket/season
/basket/division
/basket/captain-grant
/basket/registration
/basket/event-team
/basket/event-player
/basket/schedule
/basket/match
/basket/match-submission
/basket/standing
/basket/stat
```

公开接口建议：

```text
/basket/public/series
/basket/public/season
/basket/public/division
/basket/public/match
/basket/public/standing
```

个人中心接口建议：

```text
/basket/account/my-events
/basket/account/my-registrations
/basket/account/my-event-teams
/basket/account/my-schedules
```

注意：

```text
接口参数必须使用 seasonId + divisionId。
不再使用 eventId 作为 seasonId。
不再使用 tournamentId 装 seasonId。
```

---

# 10. Open / PERSONAL 处置

## 10.1 当前处理

```text
PERSONAL 个人报名只保留枚举和字段。
后台页面不开放 PERSONAL 选择，或者显示置灰“暂未开放”。
接口收到 PERSONAL 直接返回“个人报名暂未开放”。
旧 Open 报名菜单隐藏。
旧 Open 审核入口不挂新菜单。
旧 Open 代码不作为新链路依赖。
```

## 10.2 后续可能演进

未来个人报名上线时：

```text
PersonalRegistration
个人报名池
后台组队 / 系统组队
生成临时 EventTeam
生成 EventPlayer
进入统一赛程与比赛数据流程
```

核心原则不变：

```text
不管报名来源是什么，进入正式比赛前必须变成 EventTeam / EventPlayer。
```

---

# 11. 清库与保留白名单

> 注意：本节是给 Cursor 执行前确认的清单。真正删除/清空前必须再由用户确认。

## 11.1 建议清空 / 废弃的旧赛事域

包括但不限于：

```text
旧 competition season 相关表
旧 tournament 相关表
旧 tournament season 相关表
旧 tournament season platform 相关表
旧 captain qualification 赛事授权表
旧 tournament registration 相关表
旧 team season profile 快照表
旧 tournament season roster 快照表
旧 schedule batch 表
旧 stage / group / round / rule 表
旧 match 表
旧 match period score 表
旧 match official result 表
旧 match data submission 表
旧 match data submission 子表
旧 standing 表
旧 team season stat 表
旧 player season stat 表
旧 rank 表
旧 Open registration 表
旧 dispute / exception / roster change / honor 赛事周边表
旧赛事相关 Demo 数据
旧赛事相关自动任务配置
旧赛事菜单与按钮权限
```

## 11.2 必须保留的非赛事资产

包括但不限于：

```text
系统用户
系统角色
系统权限基础表
登录账号
邮箱验证码相关表
球员认证表
永久球员档案表
球员平台身份表
长期球队表
长期球队成员表
文件上传 / 附件表
公告内容表
合作伙伴 / 赞助墙表
系统字典基础表
操作日志基础表
```

## 11.3 清库策略建议

```text
1. 开发库：允许按白名单清空旧赛事域。
2. 联调库：执行前再次确认，最好先备份。
3. 生产库：当前阶段如未上线可清；如已有运营数据，必须单独确认。
4. 不做旧赛事数据迁移。
5. 新赛事域通过 seed 初始化四种 Series。
```

---

# 12. 本期重建 / 禁用 / 后续清理清单

## 12.1 本期必须重建

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
MatchSubmission 子表
MatchSubmissionAuditLog
OfficialResult
Standing
TeamStat
PlayerStat
Rank
后台发布流程
后台审核流程
后台赛程流程
后台数据审核流程
前台赛事详情
前台报名入口
个人中心我的赛事
个人中心我的赛程
```

## 12.2 本期禁用 / 只预留

```text
PERSONAL
Open
GROUP_KNOCKOUT 完整晋级
KNOCKOUT 完整淘汰树
BO3 / BO5
种子队
复杂轮空
自动荣誉生成
争议异常完整闭环
阵容变更完整闭环
```

## 12.3 本期删除或隐藏入口

```text
旧赛事发布入口
旧赛事详情入口
旧报名审核入口
旧 Open 报名入口
旧赛程生成入口
旧比赛管理入口
旧比赛数据审核入口
旧 eventId 路由
旧 tournamentId 混用路由
```

---

# 13. 分轮实施计划

## 第 0 轮：方案共识轮

目标：

```text
不写代码，只确认最终方案。
```

输出：

```text
最终模型
最终命名
最终表清单
最终清库范围
最终分轮计划
Cursor 最后一轮评审意见
```

验收：

```text
1. ChatGPT 与 Cursor 对全重构方向无分歧。
2. 表命名定稿。
3. 清库白名单定稿。
4. 本期重建/不做/删除入口清单定稿。
5. 第一轮开发边界定稿。
```

---

## 第 1 轮：新赛事域数据模型与枚举

目标：

```text
建立新赛事域基础表结构、枚举、Entity、Mapper 基础，不写复杂业务。
```

做：

```text
1. 新建核心表 DDL。
2. 新建枚举。
3. 新建 Entity / Mapper / 基础 Service 框架。
4. 初始化四种 Series。
5. 可选初始化 League 2026 S1 + PS/Xbox Division 示例。
6. 标记旧赛事表待清理。
7. 编写清库脚本或清库说明。
```

不做：

```text
1. 不写报名业务。
2. 不写赛程生成业务。
3. 不写比赛数据提交业务。
4. 不改前台页面。
5. 不实现个人报名。
```

验收：

```text
1. 新表可以表达 Series → Season → Division。
2. 所有主链路表都有 division_id。
3. 不再出现 eventId 表示 seasonId 的新代码。
4. Series 四种种子数据存在。
5. 本地构建通过。
```

---

## 第 2 轮：后台赛事发布流程

目标：

```text
后台可以通过步骤式流程发布 Season 和 Division。
```

做：

```text
1. 赛事系列选择。
2. 赛季基础信息填写。
3. 平台多选生成 Division。
4. 报名规则配置，当前强制 TEAM。
5. 赛制配置，当前只开放基础 REGULAR_PLAYOFF。
6. 比赛时间配置。
7. 预览发布。
8. 隐藏或隔离旧赛事发布入口。
```

验收：

```text
1. 可创建 League 2026 S1。
2. 可生成 PS / Xbox 两个 Division。
3. PERSONAL 不能被保存为开放状态。
4. 前台公开接口能查到已发布 Season 和 Division 基础信息。
```

---

## 第 3 轮：队长授权、球队报名、审核快照

目标：

```text
新模型下跑通 TEAM 报名到 EventTeam/EventPlayer 快照。
```

做：

```text
1. Division 队长授权。
2. 前台球队报名接口。
3. 报名阵容 5–8 校验。
4. 平台身份校验。
5. 报名撤回。
6. 后台报名审核。
7. 审核通过生成 EventTeam / EventPlayer。
8. 名额按 division 统计。
9. PERSONAL / Open 拦截。
```

验收：

```text
1. PS 赛区名额和 Xbox 赛区名额独立。
2. 审核中不占名额。
3. 审核通过才占名额。
4. 第 N+1 支审核通过时被拦截。
5. 同一 division 下同一球员不能多队。
6. EventTeam / EventPlayer 快照正确。
```

---

## 第 4 轮：赛程生成与比赛管理

目标：

```text
按 Division 生成和发布赛程。
```

做：

```text
1. REGULAR_ROUND_ROBIN_V1 单循环生成。
2. 支持奇数队 BYE。
3. 支持默认比赛日和时间段。
4. 生成 ScheduleBatch。
5. 生成 Match 草稿。
6. 发布 Match。
7. 后台比赛列表按 Season/Division 筛选。
8. 自动 Job 如暂不做，必须关闭旧 Job 入口。
```

验收：

```text
1. PS 只排 PS 的 EventTeam。
2. Xbox 只排 Xbox 的 EventTeam。
3. 不跨 Division。
4. 发布后前台可查。
5. 草稿未发布不公开。
```

---

## 第 5 轮：比赛数据提交、审核与结算

目标：

```text
比赛结束后，双方队长可分别提交本方数据；后台审核后生成正式结果与榜单统计。
```

做：

```text
1. 前台比赛数据提交。
2. 支持 matchId + submitEventTeamId 维度。
3. 支持提交版本。
4. 后台数据审核。
5. 审核通过生成 OfficialResult。
6. 按 division 重算 Standing / TeamStat / PlayerStat / Rank。
7. 审核日志。
8. 数据幂等。
```

验收：

```text
1. 一方审核中不影响另一方提交。
2. 未审核数据不进统计。
3. 审核通过才生成正式结果。
4. 同一比赛不会重复累计。
5. PS 和 Xbox 榜单不串。
```

---

## 第 6 轮：前台赛事列表、详情、报名入口

目标：

```text
前台赛事页面全面使用新模型。
```

做：

```text
1. 赛事列表按 Series 展示。
2. 赛事详情基于 seasonId + divisionId。
3. 平台赛区切换。
4. Division 下参赛队伍。
5. Division 下赛程。
6. Division 下榜单。
7. 报名入口。
8. 删除旧 competition/tournament 混用路由。
```

验收：

```text
1. League / Arena / Cup / Open 栏目清晰。
2. 多平台赛事可切换平台赛区。
3. 切换平台后队伍/赛程/榜单刷新正确。
4. 报名入口状态正确。
```

---

## 第 7 轮：个人中心与比赛数据入口

目标：

```text
个人中心基于新 EventTeam/EventPlayer/Match 展示我的赛事和我的赛程。
```

做：

```text
1. 可参与赛事。
2. 我的报名。
3. 已参加赛事。
4. 我的赛程。
5. 比赛详情入口。
6. 比赛数据提交入口。
7. 历史比赛可查看。
```

验收：

```text
1. 我的赛事展示 Series / Season / Division。
2. 我的赛程默认显示未开始，按时间从近到远。
3. 可切换全部赛程。
4. 已结束比赛仍可进入详情和数据提交入口。
5. 不显示旧赛事数据。
```

---

## 第 8 轮：旧入口清理、联调、测试验收

目标：

```text
新赛事域全链路可跑通，旧赛事入口不再暴露。
```

做：

```text
1. 隐藏/删除旧赛事菜单。
2. 删除旧前台赛事入口。
3. 关闭旧 Open 入口。
4. 清理旧自动任务。
5. 重写 Demo 数据。
6. 补核心测试。
7. 四项目构建。
8. 全链路测试。
```

验收链路：

```text
后台创建 League 2026 S1
生成 PS / Xbox Division
授予 PS 队长
PS 队长提交球队报名
后台审核通过
生成 EventTeam / EventPlayer
生成 PS 赛程
发布 PS 赛程
前台查看 PS 赛程
比赛结束
队长提交数据
后台审核通过
生成正式结果
更新 PS 榜单
个人中心我的赛事 / 我的赛程正确展示
Xbox 数据不受影响
```

---

# 14. 风险与控制

## 14.1 P0 风险

```text
1. division_id 未贯穿全链路，导致多平台串区。
2. 名额仍按 season 统计。
3. 赛程仍按 season 排全量队伍。
4. 榜单仍按 season 混算。
5. 新代码继续使用 eventId 表示 seasonId。
6. PERSONAL / Open 半开导致流程断裂。
7. 新 Match 表已建但 submission / official result / stats 未配套。
```

控制方式：

```text
1. 每张赛事主链路表必须有 division_id。
2. 所有业务接口必须明确 divisionId。
3. 每轮验收都要验证 PS / Xbox 不串。
4. PERSONAL / Open 全部拦截。
5. 旧入口在新链路可用后全部隐藏。
```

---

## 14.2 P1 风险

```text
1. 清库误删账号/球员/球队。
2. 后台菜单新旧混在一起。
3. 状态枚举定义不统一。
4. Demo 数据仍指向旧表。
5. 自动任务仍扫旧表。
6. 前端路由仍传 tournamentId 装 seasonId。
```

控制方式：

```text
1. 清库白名单先人工确认。
2. 新旧菜单分阶段控制。
3. 第 1 轮先冻结枚举。
4. 第 8 轮统一清理旧 Demo / Job / 菜单。
```

---

# 15. 需要最终确认的问题

进入第 1 轮开发前，必须确认：

```text
1. 是否确认使用本版表命名：basket_series / basket_season / basket_division / basket_event_team / basket_event_player / basket_match 等？
2. 是否确认同一长期球队允许报名同一 Season 下多个 Division？
3. 是否确认队长授权直接绑定 division_id？
4. 是否确认 PERSONAL / Open 本期全部关闭？
5. 是否确认第一阶段只实现 REGULAR_ROUND_ROBIN_V1 常规赛基础版？
6. 是否确认 GROUP_KNOCKOUT / KNOCKOUT 只预留或后置？
7. 是否确认开发库 / 联调库赛事域可以清空？
8. 是否确认旧赛事菜单最终删除，不做长期双轨？
9. 是否确认新接口不兼容 eventId = seasonId？
10. 是否确认旧代码只作为参考，不作为新运行路径？
```

---

# 16. 给 Cursor 的下一轮评审要求

请 Cursor 对本第二版方案做最后评审。

要求：

```text
1. 不改代码。
2. 不写 SQL。
3. 不新增 docs。
4. 只在对话中输出评审意见。
5. 重点评审：表命名、表清单、清库白名单、分轮、遗漏模块、第一轮边界。
6. 如仍不同意，请指出具体原因和替代方案。
7. 如同意，请明确哪些内容可以作为最终共识进入第 1 轮开发。
```

重点问题：

```text
1. 表命名是否最终可接受？
2. 新表清单是否完整？
3. 本期重建/预留/删除入口清单是否完整？
4. 清库与保留白名单是否安全？
5. 分 8 轮是否合理？
6. 第 1 轮是否应该只做 DDL/Entity/Enum/Seed，不写业务？
7. 是否还有遗漏模块会导致新旧夹生？
```

---

# 17. 当前暂定共识结论

```text
CNPA 赛事域 V4 采用全量重构。
旧赛事三层结构不再作为新业务主线。
新赛事域统一使用 Series → Season → Division。
Division 是平台赛区唯一隔离边界。
报名、快照、赛程、比赛、提交、统计全部围绕 division_id。
账号、球员、平台身份、长期球队保留。
当前只做 TEAM 球队报名。
PERSONAL / Open 只预留并拦截。
第一阶段只做基础常规赛单循环。
复杂赛制后置。
旧赛事入口最终删除。
第 1 轮只做模型、枚举、表结构和种子数据，不写复杂业务。
```
