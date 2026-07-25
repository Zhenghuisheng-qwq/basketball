# CNPA 内容运营设计

## 1. 已有能力

| 模块 | 表/页面 | 状态 |
|---|---|---|
| 公告 | `basket_announcement` + admin/content/announcement | 已有 CRUD |
| 荣誉 | `basket_honor_record` + team/player honor 页 | 已有 |
| 名人堂 | `basket_hall_of_fame` + hall-of-fame 页 | 已有 |
| 冠军阵容 | `basket_honor_lineup` | 表已有，自动生成待接结算 |

## 2. Batch6 扩展

- `basket_announcement`：season_id、scheduled_publish_time、rule_version、superseded_flag 等
- `basket_announcement_rule_link`：规则公告替代关系
- `basket_honor_generation_batch`：荣誉生成批次 + `generation_batch_id`

## 3. 规则公告替代

```text
旧 RULE 公告 --(rule_link)--> 新 RULE 公告
旧公告 superseded_flag=1，仍保留历史
官网只读 current_flag=1 且 PUBLISHED
```

## 4. 荣誉生成原则

- 来源：`AUTO_SETTLEMENT` 必须绑定有效 `basket_data_settlement_batch`
- 唯一：同一 batch + honor_type + target 不可重复
- 手工荣誉不覆盖自动批次

## 5. 遗留

- P1：定时发布公告 Quartz 任务
- P1：结算确认后 honor generation preview/confirm API
- P2：处罚公示与公告 PENALTY 分类自动关联
