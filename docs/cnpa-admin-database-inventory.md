# CNPA Admin 数据库盘点（第一批）

> **盘点日期**：2026-07-25  
> **数据库**：`basketball` @ MySQL 8.0.43（本地开发）  
> **总表数**：78 | **CNPA 业务表**：58 (`basket_*`)  
> **本批次**：仅 SELECT / SHOW / DESCRIBE，无写操作

---

## 1. 若依系统表（CNPA 相关引用）

| 表名 | 用途 |
|------|------|
| sys_menu | 动态菜单（CNPA menu_id 2000–2999，158 条） |
| sys_role_menu | 角色菜单（role_id=2 授权 157 条 CNPA） |
| sys_user_role | 用户角色 |
| sys_role | 角色定义 |
| sys_dict_type / sys_dict_data | 字典（basket 业务字典待前端 usage 确认） |
| sys_oper_log | 若依操作日志（≠ 业务审计） |

---

## 2. 按业务域分类

### 2.1 账号与会员（Web 主写，Admin 只读/审核）

| 表名 | 业务含义 | 主键 | 关键唯一约束 | 状态字段 | 数据量 | Mapper 使用 | 风险 |
|------|----------|------|--------------|----------|--------|-------------|------|
| basket_member_account | 会员账号（邮箱唯一） | id | email, username, account_no | account_status | 140 | Web + Admin 间接 | 与 player_profile 分离 ✅ |
| basket_member_profile | 会员资料 | id | member_id | — | 待确认 | Web | — |
| basket_login_log | 登录日志 | id | — | — | — | Web | 敏感字段仅存 hash |
| basket_account_security_log | 安全日志 | id | — | — | — | Web | — |
| basket_verify_code_record | 验证码 | id | — | — | — | Web | 不输出验证码值 |
| basket_notification_setting | 通知设置 | id | — | — | — | Web | — |
| basket_privacy_setting | 隐私设置 | id | — | — | — | Web | — |
| basket_favorite | 收藏 | id | — | — | — | Web | — |
| basket_message_task / basket_message_receiver | 站内消息 | id | — | — | — | Admin 审核发消息 | — |

**混用判断**：账号表不含 player_no；`basket_player_profile.member_id` UNIQUE → **账号与永久球员档案已分离**。

### 2.2 球员认证与档案

| 表名 | 业务含义 | 主键 | 关键字段 | 审核字段 | 数据量 | Admin Mapper | 风险 |
|------|----------|------|----------|----------|--------|--------------|------|
| basket_player_cert_application | 认证申请 | id | game_terminal, game_account, position_code | application_status, reviewed_time, current_auditor_id, review_remark | 135 | BasketPlayerCertApplicationMapper | **无 mobile/idCard** ✅ |
| basket_player_cert_audit | 审核记录 | id | application_id | auditor_id, audit_time, audit_remark | — | BasketPlayerCertAuditMapper | 业务审计片段 |
| basket_player_cert_material | 认证材料 | id | application_id, material_type | — | — | BasketPlayerCertMaterialMapper | 截图证明 |
| basket_player_profile | 永久球员档案 | id | player_no UNIQUE, member_id UNIQUE | certification_status | 133 | BasketPlayerProfileMapper | 审核通过写入 |
| basket_captain_qualification | 队长资格 | id | member_id | qualification_status | — | BasketCaptainQualificationMapper | 与球队 captain 分离 |

**application_status 分布**：0→1, 1→3, 2→1, 3→129(已通过), 4→1

### 2.3 球队

| 表名 | 业务含义 | 主键 | 状态/审核 | 数据量 | Admin | 风险 |
|------|----------|------|-----------|--------|-------|------|
| basket_team | 球队（永久+当前） | id | audit_status, team_status | 19 | BasketTeamMapper | **永久与赛事球队未分表** |
| basket_team_audit_record | 球队审核记录 | id | audit_action, auditor_id | — | BasketTeamAuditRecordMapper | — |
| basket_team_member | 球队成员 | id | member_status | — | Web 主写 | 转会逻辑缺失 |
| basket_team_captain_history | 队长变更历史 | id | effective_time | 0 | 无 Admin Service | 表存在未用 |
| basket_team_invitation | 邀请 | id | — | — | Web | — |
| basket_team_join_application | 加入申请 | id | — | — | Web | — |
| basket_team_season_profile | 球队赛季档案 | id | — | **0** | 仅 LocalDemo | **报名通过未写入** |

### 2.4 赛事结构

| 表名 | 业务含义 | 主键 | 赛事关联 | 数据量 | Admin |
|------|----------|------|----------|--------|-------|
| basket_tournament | 赛事 | id | — | 3 | ✅ |
| basket_tournament_season | 赛季 | id | tournament_id | — | ✅ |
| basket_tournament_rule | 规则 | id | season_id | — | ✅ |
| basket_tournament_stage | 阶段 | id | season_id | — | ✅ |
| basket_tournament_round | 轮次 | id | stage_id | — | ✅ |
| basket_tournament_group | 分组 | id | stage_id | — | ✅ |
| basket_tournament_group_team | 分组球队（正式参赛） | id | group_id, team_id | 17 | 分组管理手动入组 |
| basket_tournament_award_config | 奖项配置 | id | season_id | — | 待确认 |
| basket_venue | 场地 | id | — | — | match 引用 |

### 2.5 报名

| 表名 | 业务含义 | 主键 | 状态 | 数据量 | 风险 |
|------|----------|------|------|--------|------|
| basket_tournament_registration | 报名主表 | id | registration_status | 18 | approve 仅改状态 |
| basket_tournament_registration_player | 报名阵容 | id | validation_status | — | 无「正式阵容」表 |
| basket_tournament_registration_audit | 报名审核记录 | id | audit_action | — | 有 audit 表 ✅ |

**registration_status 分布**：0→1, 3→17(已通过)

**混用判断**：报名通过 **未自动** insert `basket_tournament_group_team`（依据：`BasketRegistrationAuditServiceImpl.approve` 无 groupTeam 调用；group_team 17 行来自分组管理/LocalDemo）。

### 2.6 比赛执行

| 表名 | 业务含义 | 主键 | 状态字段 | 版本/审计 | 数据量 |
|------|----------|------|----------|-----------|--------|
| basket_match | 比赛主表 | id | match_status, data_status | match_status_log | 31 |
| basket_match_team | 比赛球队 | id | — | — | — |
| basket_match_period_score | 分节比分 | id | — | — | — |
| basket_match_roster | 出场名单 | id | — | — | — |
| basket_match_player_stat | 球员技术统计 | id | data_status | stat_change_record | — |
| basket_match_team_stat | 球队技术统计 | id | data_status | — | — |
| basket_match_status_log | 比赛状态日志 | id | change_type | 审核人/时间 | 业务审计片段 |

**混用判断**：比分与技术统计共用 `basket_match.data_status`；confirmScore 不更新 data_status（P1）。

### 2.7 统计与结算

| 表名 | 业务含义 | 写入方式 | Admin |
|------|----------|----------|-------|
| basket_stage_standing | 阶段积分 | 结算落表 | 查询 |
| basket_team_season_stat | 球队赛季汇总 | 结算落表 | 查询 |
| basket_player_season_stat | 球员赛季汇总 | 结算落表 | 查询 |
| basket_player_rank | 球员排行快照 | 结算/重算 | 查询 |
| basket_team_rank | 球队排行快照 | 结算/重算 | 查询 |

**风险**：结算 Service 有 Redis 锁；重复 confirm 需验证幂等（待集成测试确认）。

### 2.8 纪律

| 表名 | 业务含义 | 数据量 | 使用 |
|------|----------|--------|------|
| basket_discipline_record | 赛事处罚 | **0** | RegistrationAudit 校验禁赛 member_id |

**混用判断**：无「当前生效限制」独立表；处罚记录与限制 **未分离**。

### 2.9 内容与荣誉

| 表名 | 业务含义 | Admin |
|------|----------|-------|
| basket_announcement | 公告 | ✅ |
| basket_announcement_attachment | 公告附件 | ✅ |
| basket_honor_record | 荣誉记录 | ✅ |
| basket_honor_lineup | 冠军阵容 | ✅ |
| basket_honor_category | 荣誉分类 | ✅ |
| basket_hall_of_fame | 名人堂 | ✅（表名 honor 相关） |
| basket_home_banner | 首页横幅 | ✅ |
| basket_partner | 合作伙伴 | ✅ |
| basket_site_config | 站点配置 | Web |
| basket_file_asset | 文件资源 | 共用 |

### 2.10 其他

| 表名 | 说明 |
|------|------|
| basket_stat_change_record | 比赛数据变更审计 |
| basket_member_agreement_record | 协议确认 |

---

## 3. 模型混用标记汇总

| 混用类型 | 现状 | 建议批次 |
|----------|------|----------|
| 账号 vs 球员档案 | **已分离**（member_account + player_profile） | — |
| 永久球队 vs 赛事球队 | **混用** basket_team + group_team | 第四批 |
| 报名 vs 正式参赛 | **未闭环**（approve 不写 group_team） | 第四批 |
| 当前阵容 vs 历史阵容 | team_member 无失效时间链；captain_history 未用 | 第四批 |
| 录入数据 vs 审核数据 | match.data_status + stat_change_record | 第五批 |
| 处罚 vs 当前限制 | 仅 discipline_record | 第六批 |
| sys_oper_log vs 业务审计 | 分散 audit 表，无统一查询 | 第六批 |

---

## 4. 敏感字段（仅记录字段名，不输出值）

| 表 | 字段 | 用途 |
|----|------|------|
| basket_member_account | email, password_hash | 登录 |
| basket_player_cert_application | **已无** id_card/mobile/real_name | 迁移后移除 |
| basket_verify_code_record | target, code_hash | 验证码 |

---

## 5. 主要表数据量（COUNT，无敏感内容）

| 表 | 总数 |
|----|------|
| basket_member_account | 140 |
| basket_player_profile | 133 |
| basket_player_cert_application | 135 |
| basket_team | 19 |
| basket_tournament | 3 |
| basket_tournament_registration | 18 |
| basket_tournament_group_team | 17 |
| basket_match | 31 |
| basket_discipline_record | 0 |
| basket_team_season_profile | 0 |

---

## 6. 疑似废弃 / 待确认

| 表 | 说明 |
|----|------|
| basket_team_season_profile | 0 行，Mapper 仅 LocalDemo |
| basket_team_captain_history | 0 行，无 Admin 入口 |
| basket_discipline_record | 0 行，业务未上线 |

---

*Mapper 完整列表见 `basketball-admin/src/main/resources/mapper/basket/`（27 个 XML）*
