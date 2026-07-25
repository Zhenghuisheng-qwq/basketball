# CNPA 报名与参赛设计（第四批）

## 概念映射

| 概念 | 物理表 |
|------|--------|
| 球队报名申请 | basket_tournament_registration |
| 报名阵容 | basket_tournament_registration_player |
| 报名审核记录 | basket_tournament_registration_audit |
| 正式赛事球队 | basket_team_season_profile（扩展字段） |
| 正式赛事阵容 | basket_tournament_season_roster（新建） |
| Open 报名 | basket_tournament_open_registration（新建） |
| 阵容变更申请 | basket_tournament_roster_change_request（新建） |
| 阵容变更历史 | basket_tournament_roster_change_record（新建） |
| 管理员特殊操作 | basket_tournament_roster_operation_log（新建） |

## 审核通过事务（球队报名）

```text
CAS 更新 registration → APPROVED
→ INSERT team_season_profile（球队快照 + event_team_no）
→ INSERT season_roster 多条（球员快照，source=INITIAL_REGISTRATION）
→ INSERT registration_audit
→ 幂等：registration_id UNIQUE 于 event_team
```

## 唯一约束

- `uk_team_season_profile (team_id, season_id)` 已有
- `uk_season_roster_active_slot (active_slot)` active_slot = `{seasonId}:{playerId}` 仅 ACTIVE 行
- `uk_registration_event_team (registration_id, is_delete)` 防重复生成

## 阵容变更

- 不 UPDATE roster.team_id
- 原行：invalid_time + status=TRANSFERRED_OUT/REMOVED，active_slot=NULL
- 新行：INSERT + source_type

## Open 报名

- 审核通过：INSERT season_roster（event_team_id NULL，source=OPEN_REGISTRATION）
