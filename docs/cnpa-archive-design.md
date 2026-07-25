# CNPA 永久档案设计

## 1. 模型关系

```text
basket_player_profile（永久球员）
  ├── basket_member_account（账号，1:1）
  ├── basket_player_platform_change_request / history（平台 ID 历史，已有表）
  ├── basket_player_team_relation（batch6 新增，关系历史）
  ├── basket_tournament_season_roster + 快照字段（赛事参赛历史）
  └── basket_discipline_record（处罚历史）

basket_team（永久球队）
  ├── basket_team_profile_change（batch6 新增：名称/Logo/简介/队长）
  ├── basket_team_captain_history（已有）
  ├── basket_team_member（当前成员摘要）
  └── basket_team_season_profile（赛事球队快照，不随永久资料变化）
```

## 2. Admin 档案页

- 永久球员：`GET /basket/permanentArchive/player/{playerId}`
- 永久球队：`GET /basket/permanentArchive/team/{teamId}`
- 展示：基本资料、关系/认证历史、处罚、荣誉

## 3. 原则

- 永久档案不因处罚/账号禁用而删除
- 赛事快照字段（nickname_snapshot 等）与永久资料分离
- 历史页面优先读快照表，不用当前 profile 覆盖

## 4. 遗留

- P1：`basket_player_team_relation` 写入需在转会/补员/离队流程中补钩子
- P1：球队资料变更审核流与 `basket_team_profile_change` 自动写入
