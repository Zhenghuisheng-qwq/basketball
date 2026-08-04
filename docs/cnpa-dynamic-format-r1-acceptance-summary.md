# CNPA 动态赛制 R1 — 验收摘要

## 验收结论

**代码与 SQL 已按 R1 范围落地，具备继续第 2 轮的数据与快照基础。**  
因本执行环境缺少 Maven/数据库运行时，**尚未完成联调级端到端验收**；需在目标环境执行 DDL/Seed 后按 `cnpa-dynamic-format-r1-validation.md` 复验。

## 已完成能力

1. 平台模型：PC + PS_XBOX（历史 PS/XBOX 只读）
2. 比赛模式：FIVE_V_FIVE / THREE_V_THREE + 统一阵容限制
3. 结构化模板三表 + Division 快照三表
4. 种子：`V4_LEGACY_ROUND_ROBIN_V1`、`LEAGUE_STANDARD_V1`
5. 快照创建/重建/校验/锁定服务
6. Division 创建、编辑、开放报名接入
7. Admin 发布页：平台/模式/模板/规则只读页签
8. Web：平台/模式/动态人数展示；PERSONAL 不入 TEAM 链路

## 未完成能力（R1 明确排除）

季后赛生成、BO 多场次、积分责任链、晋级、个人报名、在线抽签/分组、V2 迁移删除。

## P0 风险

| 级别 | 风险 | 缓解 |
|------|------|------|
| P0 | 未执行 DDL 的环境 Division 创建将失败 | 按顺序执行 migration → seed |
| P0 | 历史 Division 无快照时开放报名失败 | 运行 MigrationRunner 或手工补快照 |
| P1 | 同 Season 并存 PS+XBOX legacy Division | 使用 data-check SQL，人工决定是否合并 |
| P1 | LEAGUE 模板 REGULAR 阶段 advancement_count 为空 | 锁定前必须配置或换 LEGACY 模板 |

## 第 2 轮建议范围

1. Division 规则 DRAFT 阶段的可视化编辑（阶段 BO、晋级 N）
2. 常规赛赛程生成读取 `basket_division_stage`
3. 积分榜结算读取快照积分/同分规则
4. PS/XBOX 历史 Division 人工迁移工具
5. 个人报名草稿 → 抽签结果录入（若产品优先级确认）
