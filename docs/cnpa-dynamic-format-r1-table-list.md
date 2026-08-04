# CNPA 动态赛制 R1 — 表清单

## 新增表

| 表名 | 职责 |
|------|------|
| `basket_format_template` | 可复用赛制模板头（series 级或通用） |
| `basket_format_template_stage` | 模板阶段定义 |
| `basket_format_template_tie_break_rule` | 模板同分排序规则 |
| `basket_division_format_snapshot` | Division 本届规则快照头 |
| `basket_division_stage` | Division 阶段规则快照 |
| `basket_division_tie_break_rule` | Division 同分规则快照 |

## basket_division 扩展字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `game_mode` | varchar(32) | FIVE_V_FIVE / THREE_V_THREE |
| `format_template_id` | bigint | 创建时选择的模板 |
| `format_snapshot_id` | bigint | 当前有效规则快照 |

## 索引与唯一约束

- `basket_format_template`：`uk_template_code_version (template_code, version_no, is_delete)`
- `basket_format_template_stage`：`uk_template_stage_code`、`uk_template_stage_order`
- `basket_division_format_snapshot`：`uk_division_snapshot_active (division_id, is_delete)`
- `basket_division_stage`：`uk_division_stage_code`、`uk_division_stage_order`
- `basket_division`：`idx_division_game_mode`、`idx_division_format_snapshot`

## 关系

```
basket_series
  └── basket_format_template (series_id 可空=通用)
        ├── basket_format_template_stage
        └── basket_format_template_tie_break_rule

basket_division
  ├── format_template_id → basket_format_template
  └── format_snapshot_id → basket_division_format_snapshot
        ├── basket_division_stage
        └── basket_division_tie_break_rule → basket_division_stage
```

## 模板与快照

- **模板**：运营复用来源；修改不影响已锁定快照。
- **快照**：Division 创建时从模板复制；DRAFT 可重建；开放报名前 `LOCKED`。

## 种子模板

| template_code | 用途 |
|---------------|------|
| V4_LEGACY_ROUND_ROBIN_V1 | 兼容现有 V4 单循环 |
| LEAGUE_STANDARD_V1 | League 默认（常规赛→季后赛 BO3→总决赛 BO5） |
