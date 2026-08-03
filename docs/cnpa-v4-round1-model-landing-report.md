# CNPA 赛事域 V4 全量重构 — 第 1 轮模型落地报告

> 日期：2026-08-03  
> 分支：`basketball-20260803`  
> 阶段：第 1 轮（DDL / Enum / Entity / Mapper / Seed / 清库说明）  
> 状态：**已完成（含 R1 DDL 小补丁），未进入第 2 轮**

---

## 1. 本轮完成了什么

本轮仅落地新赛事域模型基础，未写复杂业务，未改前后台页面，未执行 DROP / TRUNCATE。

已完成：

1. 扫描旧赛事域表与保留资产表，输出精确清库候选清单与保留清单。
2. 生成清库 SQL 草案（全部注释，不可自动执行）。
3. 生成 P0 新表 DDL（22 张）。
4. 生成 Series 四种种子 + League 2026 S1 / PS / Xbox 示例数据脚本。
5. 新增 admin / web 两侧统一 V4 枚举（VARCHAR 字符串 code）。
6. 新增 admin Entity + Mapper + XML 基础 CRUD。
7. 新增 web Model + Dao（MyBatis-Plus BaseMapper）基础结构。
8. admin / web 后端编译通过。
9. 输出本报告。

未做（按边界禁止）：

- 报名 / 审核 / 授权 / 赛程 / 提交 / 结算业务
- 前台 / 后台页面改动
- PERSONAL / Open 流程
- 自动排赛 Job
- 旧数据迁移
- 真实清库执行

---

## 2. 涉及文件清单

### 2.1 SQL

| 文件 | 说明 |
|------|------|
| `docs/sql/cnpa-v4-r1-cleanup-draft.sql` | 清库草案（DROP 全注释） |
| `docs/sql/cnpa-v4-r1-ddl.sql` | P0 22 张新表 DDL |
| `docs/sql/cnpa-v4-r1-seed.sql` | Series 种子 + 示例 Season/Division |

### 2.2 辅助脚本

| 文件 | 说明 |
|------|------|
| `scripts/generate_v4_r1_java.py` | 生成 Entity/Enum/Mapper 的辅助脚本 |
| `scripts/rename_v4_conflicts.py` | 同名 Mapper/Dao 冲突重命名脚本 |

### 2.3 Admin 配置微调

| 文件 | 变更 |
|------|------|
| `basketball-admin/.../ApplicationConfig.java` | MapperScan 增加 `com.zhs.basket.mapper.v4` |
| `basketball-admin/.../application.yml` | 已移除对 `com.zhs.basket.domain.v4` 的显式 typeAliasesPackage；v4 XML 统一全限定类名 |

### 2.4 Admin 新增代码包

```text
basketball-admin/src/main/java/com/zhs/basket/enums/v4/     # 14 个枚举
basketball-admin/src/main/java/com/zhs/basket/domain/v4/    # 22 个 Entity
basketball-admin/src/main/java/com/zhs/basket/mapper/v4/    # 22 个 Mapper
basketball-admin/src/main/resources/mapper/basket/v4/       # 22 个 XML
```

### 2.5 Web 新增代码包

```text
basketball-web/src/main/java/com/zhs/enums/v4/   # 14 个枚举（与 admin 一致）
basketball-web/src/main/java/com/zhs/model/v4/   # 22 个 Model
basketball-web/src/main/java/com/zhs/dao/v4/     # 22 个 Dao
```

### 2.6 同名冲突处理（Entity + Bean 名）

为避免与旧实体 MyBatis alias / 旧 Mapper/Dao Spring Bean 名冲突，以下 **实体类与 Mapper/Dao** 均已改为 `BasketV4*`：

| 表 | Entity（v4 包） | Admin Mapper | Web Dao |
|----|-----------------|--------------|---------|
| basket_match | BasketV4Match | BasketV4MatchMapper | BasketV4MatchDao |
| basket_match_period_score | BasketV4MatchPeriodScore | BasketV4MatchPeriodScoreMapper | BasketV4MatchPeriodScoreDao |
| basket_match_team_stat | BasketV4MatchTeamStat | BasketV4MatchTeamStatMapper | BasketV4MatchTeamStatDao |
| basket_match_player_stat | BasketV4MatchPlayerStat | BasketV4MatchPlayerStatMapper | BasketV4MatchPlayerStatDao |

说明：

- v4 XML `resultMap type` 全部使用全限定类名（如 `com.zhs.basket.domain.v4.BasketV4Match`）。
- 其余无同名冲突的 v4 实体仍保留原业务名（如 `BasketSeries`）。

---

## 3. 清库候选表清单

以下为旧赛事域清库候选（执行前必须备份；本轮未执行）：

### 3.1 同名冲突表（最高优先级）

```text
basket_match
basket_match_period_score
basket_match_team_stat
basket_match_player_stat
```

### 3.2 比赛数据 / 争议 / 异常 / 结算

```text
basket_match_data_dispute_material
basket_match_data_dispute
basket_match_exception_material
basket_match_exception_audit
basket_match_exception_application
basket_match_data_submission_material
basket_match_data_submission_period
basket_match_data_submission_player_stat
basket_match_data_submission_team_stat
basket_match_data_revision
basket_match_data_audit
basket_match_data_submission
basket_match_official_result
basket_match_status_log
basket_match_roster
basket_match_team
basket_data_settlement_batch
basket_stat_sync_record
basket_stat_change_record
basket_point_adjustment
```

### 3.3 统计 / 榜单 / 荣誉

```text
basket_honor_lineup
basket_honor_record
basket_honor_generation_batch
basket_honor_category
basket_player_rank
basket_team_rank
basket_player_season_stat
basket_team_season_stat
basket_stage_standing
```

### 3.4 赛程规则 / 批次

```text
basket_schedule_rule_time_slot
basket_schedule_rule_weekday
basket_schedule_rule
basket_schedule_generate_batch
```

### 3.5 报名 / 阵容 / 队长授权

```text
basket_tournament_roster_operation_log
basket_tournament_roster_change_record
basket_tournament_roster_change_request
basket_tournament_open_registration_audit
basket_tournament_open_registration
basket_tournament_registration_audit
basket_tournament_registration_player
basket_tournament_registration
basket_tournament_season_roster
basket_team_season_profile
basket_captain_grant_log
basket_captain_qualification
```

### 3.6 旧赛事结构容器

```text
basket_tournament_admin_scope
basket_tournament_award_config
basket_tournament_rule
basket_tournament_group_team
basket_tournament_group
basket_tournament_round
basket_tournament_stage
basket_tournament_season_platform
basket_tournament_season
basket_tournament
basket_competition_season
```

### 3.7 可选评估

```text
basket_venue   # 若确认仅服务旧赛事且无保留需求，再评估清理
```

---

## 4. 保留表清单

禁止误清：

```text
# 账号 / 登录
basket_member_account
basket_member_profile
basket_member_agreement_record
basket_verify_code_record
basket_login_log
basket_account_security_log
basket_privacy_setting
basket_notification_setting

# 球员认证 / 平台身份
basket_player_profile
basket_player_cert_application
basket_player_cert_material
basket_player_cert_audit
basket_player_platform_account
basket_player_platform_change_request
basket_player_platform_identity_history

# 长期球队 / 成员
basket_team
basket_team_member
basket_team_audit_record
basket_team_join_application
basket_team_invitation
basket_team_captain_history
basket_team_profile_change

# 文件 / 内容运营
basket_file_asset
basket_announcement
basket_announcement_attachment
basket_announcement_rule_link
basket_partner
basket_partner_support_record
basket_home_banner
basket_site_config
basket_message_task
basket_message_receiver
basket_favorite

# 纪律 / 业务审计（非赛事主链路容器，默认保留）
basket_discipline_record
basket_discipline_restriction
basket_discipline_suspension_log
basket_business_audit
basket_business_audit_field

# 后台基础（若依）
sys_user / sys_role / sys_menu / sys_dept / sys_dict_* / sys_config
sys_job* / sys_logininfor / sys_oper_log 等
```

---

## 5. 新表清单（P0）

| # | 表名 | 说明 | division_id |
|---|------|------|-------------|
| 1 | basket_series | 赛事系列 | 否（顶层） |
| 2 | basket_season | 赛事赛季 | 否（上层） |
| 3 | basket_division | 平台赛区 | 自身 |
| 4 | basket_captain_grant | 队长授权 | 是 |
| 5 | basket_registration | 球队报名 | 是 |
| 6 | basket_registration_player | 报名阵容 | 是 |
| 7 | basket_registration_audit | 报名审核流水 | 是 |
| 8 | basket_event_team | 赛事球队快照 | 是 |
| 9 | basket_event_player | 赛事球员快照 | 是 |
| 10 | basket_schedule_batch | 赛程批次 | 是 |
| 11 | basket_match | 比赛 | 是 |
| 12 | basket_match_period_score | 节比分 | 是 |
| 13 | basket_match_submission | 比赛数据提交 | 是 |
| 14 | basket_match_submission_team_stat | 提交版球队数据 | 是 |
| 15 | basket_match_submission_player_stat | 提交版球员数据 | 是 |
| 16 | basket_match_submission_material | 提交材料 | 是 |
| 17 | basket_match_team_stat | 单场正式球队统计 | 是 |
| 18 | basket_match_player_stat | 单场正式球员统计 | 是 |
| 19 | basket_standing | 积分榜 | 是 |
| 20 | basket_team_stat | 赛区球队累计统计 | 是 |
| 21 | basket_player_stat | 赛区球员累计统计 | 是 |
| 22 | basket_rank | 排行榜 | 是 |

补充确认：

- `basket_registration_audit` 已纳入 P0
- `basket_match_team_stat` / `basket_match_player_stat` 已纳入 P0
- 文件字段使用 `icon_file_id` / `cover_file_id` / `banner_file_id` / `team_logo_file_id` / `avatar_file_id` / `file_id`

---

## 6. 枚举说明

统一选择：**VARCHAR 字符串 code**（不使用旧整型 code，避免语义冲突）。

Admin / Web 两侧枚举清单一致，均位于 `enums.v4` 包：

```text
SeriesCodeEnum
PlatformCodeEnum
RegistrationModeEnum
FormatTypeEnum
PublishStatusEnum
SeasonStatusEnum
DivisionStatusEnum
CaptainGrantStatusEnum
RegistrationStatusEnum   # 含 PENDING_REVIEW，不再强拆 SUBMITTED/REVIEWING
AuditResultEnum
MatchStatusEnum
MatchPublishStatusEnum
SubmissionStatusEnum
RankTypeEnum
```

关键冻结值（与本轮开发指令一致）：

- SeasonStatus：`NOT_STARTED / REGISTRATION_OPEN / REGISTRATION_CLOSED / PLAYING / FINISHED / ARCHIVED`
- DivisionStatus：`DRAFT / REGISTRATION_OPEN / REGISTRATION_CLOSED / SCHEDULED / PLAYING / FINISHED / ARCHIVED`
- CaptainGrantStatus：`ACTIVE / REVOKED / EXPIRED`
- MatchStatus：`DRAFT / SCHEDULED / PLAYING / FINISHED / CANCELLED / FORFEITED`

---

## 7. Seed 数据说明

脚本：`docs/sql/cnpa-v4-r1-seed.sql`

### 7.1 四种 Series

| series_code | series_name | sort_order |
|-------------|-------------|------------|
| LEAGUE | League | 1 |
| ARENA | Arena | 2 |
| CUP | Cup | 3 |
| OPEN | Open | 4 |

### 7.2 可选示例

```text
Series: LEAGUE
  └─ Season: League 2026 S1（display_title = CNPA League 2026 S1）
       ├─ Division: League 2026 S1 - PS
       └─ Division: League 2026 S1 - Xbox
```

示例默认：

- registration_mode = TEAM
- format_type = REGULAR_ROUND_ROBIN
- lineup 5–8
- max_team_count = 16
- publish_status / season_status / division_status = DRAFT / NOT_STARTED / DRAFT

---

## 8. 验收对照

| # | 验收项 | 结果 |
|---|--------|------|
| 1 | 新增 SQL 文件 | 通过（cleanup/ddl/seed） |
| 2 | 新增 Entity | 通过（admin domain.v4 / web model.v4 各 22） |
| 3 | 新增 Mapper/XML/Dao | 通过 |
| 4 | 新增枚举 | 通过（两侧各 14） |
| 5 | Series 四种种子 | 通过（SQL 已写） |
| 6 | League 2026 S1 → PS/Xbox | 通过（SQL 已写） |
| 7 | 主链路表具备 division_id | 通过（除 series/season 容器层） |
| 8 | registration_audit / match_*_stat 入 P0 | 通过 |
| 9 | file_id 字段 | 通过 |
| 10 | 无复杂业务代码 | 通过 |
| 11 | 无前端页面改动 | 通过 |
| 12 | 未执行 DROP/TRUNCATE | 通过 |
| 13 | 编译通过 | 通过（见第 9 节） |

---

## 9. 编译结果

```text
# 初版落地
basketball-admin : mvn -DskipTests compile  => BUILD SUCCESS
basketball-web   : mvn -DskipTests compile  => BUILD SUCCESS

# R1 DDL 小补丁后复编（2026-08-03）
basketball-admin : mvn -DskipTests compile  => BUILD SUCCESS
basketball-web   : mvn -DskipTests compile  => BUILD SUCCESS
```

说明：本轮未连接数据库执行 DDL/Seed；仅保证代码可编译。未执行 DROP/TRUNCATE。

---

## 10. 风险与待确认项

1. **同名旧表未清理前不可执行 DDL**  
   `CREATE TABLE IF NOT EXISTS basket_match` 在旧表存在时不会重建结构，会导致新旧混用。必须先执行清库草案中的同名表 DROP。

2. **清库尚未人工确认**  
   候选清单已产出，但未执行。建议开发库备份后，人工确认白名单，再执行 cleanup → ddl → seed。

3. **队长授权唯一约束（R1 补丁已改）**  
   已改为 `active_unique_key` + `uk_captain_grant_active_unique_key`。业务写入时必须维护：ACTIVE=`divisionId_teamId`，REVOKED/EXPIRED/软删除=`NULL`。

4. **旧代码仍在仓库中**  
   第 1 轮未删除旧 Controller / Job / 页面。旧入口删除安排在第 8 轮；当前新旧代码共存，但新模型代码位于 `v4` 包，避免直接混用。

5. **枚举存储与旧体系不同**  
   V4 使用 VARCHAR 字符串；旧赛事域仍为整型 code。后续业务开发必须引用 `enums.v4`，禁止混用旧枚举写新表。

6. **纪律 / 消息 / 收藏等周边表**  
   默认保留。若其中有强依赖旧 seasonId/tournamentId 的数据，第 8 轮收口时再评估清理或改造。

7. **数据库尚未落表**  
   本地/开发库需在确认清库后手动执行三份 SQL，才能真正插入 Series 种子与示例数据。  
   若已执行过早期 R1 DDL，需按 `cnpa-v4-r1-ddl.sql` 文末增量 ALTER（注释块）补丁。

---

## 11. R1 DDL 小补丁记录（验收后追加，未进入第 2 轮）

### 11.1 basket_captain_grant 唯一键

- **问题**：`uk_active_captain_grant(division_id, team_id, grant_status, is_delete)` 会让多条 `REVOKED/EXPIRED` 历史互相冲突。
- **方案**：新增可空字段 `active_unique_key varchar(128)` + `UNIQUE(active_unique_key)`。
  - ACTIVE 当前有效授权：写入 `divisionId_teamId`（如 `12_34`）
  - REVOKED / EXPIRED / 软删除：写 `NULL`（MySQL UNIQUE 允许多个 NULL，历史不冲突）
- **实体/XML**：admin/web `BasketCaptainGrant` 已加 `activeUniqueKey`；Mapper XML 已映射；软删除同步清空该键。

### 11.2 basket_registration 重提策略（保持唯一键）

同意并冻结如下策略，**不调整** `uk_registration_division_team(division_id, team_id, is_delete)`：

- 同一队伍同一 Division 只允许一条未删除报名主记录；
- V4 报名撤回/驳回后：**复用同一条 registration** 重新提交（更新状态/阵容/时间等字段）；
- 历史操作记录进入 `basket_registration_audit`；
- **不新增**第二条报名主记录。

SQL 表头注释与本报告均已写明。

### 11.3 basket_match_submission 版本唯一约束

新增：

```text
uk_submission_match_team_version(match_id, submit_event_team_id, version_no, is_delete)
```

保证同一比赛、同一提交方、同一版本仅一条未删除提交记录。

### 11.4 MyBatis typeAliasesPackage / 同名实体风险

- **检查结果**：v4 XML 原本已全部使用全限定类名；但 admin `typeAliasesPackage` 扫描 `com.zhs.basket.domain` 时会递归扫到 `domain.v4`，与旧实体 `BasketMatch` / `BasketMatchTeamStat` / `BasketMatchPlayerStat` / `BasketMatchPeriodScore` 存在 alias 冲突风险。
- **处理**：
  1. 上述 4 个 v4 实体重命名为 `BasketV4Match` / `BasketV4MatchTeamStat` / `BasketV4MatchPlayerStat` / `BasketV4MatchPeriodScore`（admin + web 同步）；
  2. 对应 Mapper/XML/Dao 引用已更新；
  3. 移除 application.yml 中对 `com.zhs.basket.domain.v4` 的显式别名包依赖。

### 11.5 本补丁边界

- 未写报名/审核/赛程/提交/结算业务
- 未改前台/后台页面
- 未执行 DROP/TRUNCATE
- 未进入第 2 轮

---

## 12. 建议的下一步（第 2 轮，本轮不执行）

```text
1. 人工确认清库白名单并备份
2. 执行 cleanup → ddl → seed（含 R1 补丁后的 DDL）
3. 进入第 2 轮：后台步骤式发布 Season + 多平台生成 Division
```

---

## 13. 一句话结论

```text
第 1 轮已完成：V4 新赛事域 22 张 P0 表 DDL、统一字符串枚举、admin/web Entity/Mapper 基础结构、
Series 种子与示例 Season/Division 脚本、清库草案与本报告均已就绪；
R1 DDL 小补丁已落地（captain_grant active_unique_key、registration 重提策略注释、
submission 版本唯一约束、同名实体 BasketV4* 重命名）；两端编译通过；
未执行破坏性 SQL，未写业务，未进入第 2 轮。
```
