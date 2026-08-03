# Cursor 评审意见：ChatGPT《CNPA 赛事域 V4 全量重构方案（第二版）》

> 评审日期：2026-08-03  
> 被评文档：`chatgpt重构v2版本.md`  
> 评审前提：赛事域全量重构；不迁旧赛事数据；不兼容旧三层；账号/球员/长期球队保留  
> 本轮：不改业务代码、不写 SQL；输出评审意见供 ChatGPT 继续评估与定稿

---

## 0. 给 ChatGPT 的任务说明

请基于本文档与 `chatgpt重构v2版本.md`，输出 **V2.1 / 最终共识稿** 修订意见，重点回答：

```text
1. 是否接受 Cursor 对 V2 的总体“可定稿、可进第 1 轮”结论？
2. 对 Cursor 指出的表清单缺口，准备如何补进终稿？
3. 同名新表 vs 旧表的创建顺序，终稿如何写死？
4. 封面字段用 cover_url 还是 file_id？
5. 赛程时间规则第 1 阶段硬编码还是建配置表？
6. 是否把 registration_audit、单场正式 match_team_stat/player_stat 纳入 P0？
7. 分轮微调是否采纳？
8. 输出一份“第 0 轮最终确认清单”，供双方签字后进入第 1 轮开发。
```

请不要在本轮让 Cursor 开始写业务代码；先完成方案终稿。

---

## 1. 总体结论

| 项 | Cursor 结论 |
|----|-------------|
| V2 是否对齐全重构前提 | **是** |
| 是否还有原则性反对 | **无** |
| 是否可进入第 0 轮收口 / 第 1 轮 DDL | **可以** |
| 相对 V1 的改进 | 命名、清库白名单、重建/预留/删除清单、周边模块、分轮边界均明显补齐 |

一句话：

```text
V2 可以从“方向评审”转为“执行前定稿”。
重构仍然大，但方案层面已具备共识基础。
剩余问题是表清单边角、清库执行顺序、字段规范，不是模型路线争议。
```

---

## 2. 明确同意并建议冻结的内容

以下内容建议双方直接冻结，不再反复讨论：

```text
1. 赛事域全量重构；不迁移旧赛事业务数据。
2. 废弃旧 competition_season → tournament → tournament_season 作为新主路径。
3. 新主线：Series → Season → Division。
4. division_id 贯穿报名、快照、赛程、比赛、提交、统计。
5. 账号 / 登录 / 球员认证 / 平台身份 / 长期球队 / 成员保留。
6. 当前只开放 TEAM；PERSONAL / Open 只预留并拦截，不半实现。
7. 第一阶段只做 REGULAR_ROUND_ROBIN 常规赛单循环基础版。
8. GROUP_KNOCKOUT / 完整淘汰树 / BO3BO5 / 复杂晋级后置。
9. 表命名采用：
   basket_series / basket_season / basket_division
   basket_event_team / basket_event_player
   basket_registration / basket_registration_player
   basket_match / basket_match_submission*
   basket_standing / basket_team_stat / basket_player_stat / basket_rank
10. 新接口禁止 eventId=seasonId、tournamentId 装 seasonId、competition 混用。
11. 接口风格继续 /basket/...，不强制 /basket/admin。
12. 同一长期球队允许报名同一 Season 下多个 Division。
13. 队长授权直接绑定 division_id。
14. 报名对外状态简化为 PENDING_REVIEW，不再对外强拆 SUBMITTED/REVIEWING。
15. 提交维度：matchId + submitEventTeamId。
16. 第 1 轮只做 DDL / Enum / Entity / Mapper / Seed / 清库说明，不写复杂业务。
17. 分 8 轮切片验收；旧赛事入口最终删除，不做长期双轨。
```

---

## 3. 对 V2 各章节的细评

### 3.1 修订说明与边界（§0–§2）—— 通过

重建范围、保留范围、本期不做，与代码现实和产品决策一致。  
Open / 争议异常 / 荣誉 / 阵容变更标成预留或删除入口，方向正确。

### 3.2 术语与 ID（§3）—— 通过

术语表清楚，禁止旧混用语义正确。  
建议终稿再加一句：

```text
新代码审查时，出现 eventId 参数名一律视为不合格，除非它明确表示“无关赛事域的其他业务事件”。
```

### 3.3 表命名（§4）—— 通过，附执行约束

命名方案优于 V1 全量 `basket_event_*`。

**必须写进终稿的执行约束：**

```text
新表名 basket_match / basket_match_submission 等与旧表同名。
第 1 轮建表前必须先完成旧同名表废弃/清空/删除顺序，
禁止新旧同名表并存、双写、混查。
```

建议终稿增加「建表顺序」小节：

```text
A. 开发库：备份 → 按清库清单 DROP/清空旧赛事表 → 创建新表 → Seed Series
B. 若暂时不能 DROP：临时使用过渡表名，待旧表删除后再 rename（不推荐，除非环境限制）
```

Cursor 推荐默认走 A。

### 3.4 新表设计（§5）—— 主链路通过，需补缺口

#### 已做得好的点

- Series 枚举四码 + 配置表  
- Season 不存多平台字符串  
- Division 独立名额 / 状态 / 赛制  
- CaptainGrant 绑 division  
- Registration 名额只算 APPROVED  
- EventTeam / EventPlayer 以 division 为唯一槽  
- ScheduleBatch / Match / Submission 版本与幂等  
- Standing / Stat / Rank 按 division  

#### 建议补进 P0 或明确策略的缺口

| 缺口 | 建议 | 理由 |
|------|------|------|
| `basket_registration_audit` | **纳入 P0** | §12 写了 RegistrationAudit，表清单未单列；审核追溯需要 |
| `basket_match_team_stat` | **建议纳入 P0** | 审核通过后单场正式球队数据；比赛详情常用 |
| `basket_match_player_stat` | **建议纳入 P0** | 同上，单场正式球员数据 |
| 赛程时间规则表 | **第 1 阶段可硬编码**，方案写死默认规则；若要后台可配再加表 | 避免第 1–4 轮范围膨胀 |
| `basket_stage` | 可维持 P1；常规赛可用 `match.stage_code=REGULAR` | 不要再强依赖旧 stage 表 |

请 ChatGPT 在终稿中明确以上四项的归属：P0 / P1 / 硬编码。

### 3.5 字段规范提醒

1. **文件字段**  
   V2 使用 `cover_url` / `banner_url`。  
   现网更常见 `file_id` + 文件服务。  
   **建议改为 `cover_file_id` / `banner_file_id`**，与保留的文件上传体系一致。

2. **冗余字段**  
   V2 已比 V1 克制。建议终稿明确：

   ```text
   必存：division_id
   可冗余：season_id、platform_code
   不建议层层冗余：series_id / series_code 到 match/submission
   ```

3. **枚举存储**  
   现网大量整型 code；V2 多用字符串枚举。  
   两者都可，但第 1 轮必须冻结一种，admin/web 统一。

### 3.6 流程（§7–§9）—— 通过

后台步骤式发布、授权、报名、审核快照、按 division 排赛、双方独立提交、按 division 结算，均正确。  
个人中心继续收敛在「我的赛事」四 Tab，同意。  
菜单最终删除旧入口、不做长期双轨，同意。

### 3.7 Open / PERSONAL（§10）—— 通过

拦截策略完整。请终稿再补一句验收标准：

```text
任何新页面、新接口、新菜单都不得再暴露旧 Open 入口。
```

### 3.8 清库白名单（§11）—— 方向通过，需落成精确表名

类别清单正确，但执行前不够。

请 ChatGPT 终稿增加附录，或要求第 1 轮 Cursor 先产出：

```text
附录 A：精确清空/废弃旧表名列表
附录 B：精确保留表名列表
```

安全策略同意：

```text
开发库可清
联调库执行前再确认并备份
禁止误伤账号/球员/球队/权限/文件
不做旧赛事数据迁移
Seed 初始化四种 Series
```

### 3.9 分轮（§13）—— 总体通过，建议两处微调

| 轮次 | 评价 |
|------|------|
| 0 共识 | 必要 |
| 1 DDL/Enum/Seed | 正确，坚持不写复杂业务 |
| 2 后台发布 | 正确 |
| 3 授权+报名+快照 | 正确，核心大块 |
| 4 赛程 | 正确；旧 Job 必须关闭 |
| 5 提交+结算 | 正确 |
| 6 前台赛事 | 正确 |
| 7 个人中心 | 正确 |
| 8 清理联调 | 必要 |

**微调建议：**

1. 第 2 轮验收中的“前台公开接口能查到”应限定为 **season/division 基础信息查询**，不做完整详情站。  
2. 第 5 轮先完成后端结算 API；第 6 轮前台再接榜单，避免同一轮前后端同时大改结算语义。  
3. 第 4 轮若暂不做自动 Job，终稿写死“关闭旧 Job，不新建自动排赛”。

---

## 4. 风险评审

### P0（同意 V2 所列，并补充）

```text
1. division_id 未贯穿
2. 名额按 season 统计
3. 赛程按 season 混排
4. 榜单按 season 混算
5. 新代码继续 eventId=seasonId
6. PERSONAL/Open 半开
7. 新 Match 建了但 submission/official/stats 未配套
8.【补充】新旧同名表并存导致查错库/写错表
9.【补充】清库误删账号/球员/球队
```

### P1

```text
1. 菜单新旧并存误导运营
2. 枚举整型/字符串不统一
3. Demo/Job 仍扫旧表
4. 前端路由残留 tournamentId 混用
5. 缺少 registration_audit / 单场正式统计导致详情页能力回退
```

---

## 5. 对 V2 §15 十个确认题的建议答复

| # | 问题 | Cursor 建议答复 |
|---|------|-----------------|
| 1 | 表命名是否确认 | **确认**（附同名旧表清理顺序） |
| 2 | 同队跨 Division 报名 | **确认允许** |
| 3 | 队长授权绑 division_id | **确认** |
| 4 | PERSONAL/Open 本期关闭 | **确认** |
| 5 | 只做单循环基础版 | **确认** |
| 6 | 小组/淘汰只预留 | **确认** |
| 7 | 开发/联调可清空赛事域 | **确认**（执行前再确认一次） |
| 8 | 旧菜单最终删除 | **确认** |
| 9 | 新接口不兼容旧 ID | **确认** |
| 10 | 旧代码只作参考 | **确认** |

---

## 6. 建议 ChatGPT 在终稿中必须新增的段落

### 6.1 建表与清库顺序

```text
1. 产出精确旧表名清单
2. 备份
3. 清空/删除旧赛事表
4. 创建新表
5. Seed 四种 Series
6. 可选插入示例 Season + Division
7. 标记旧代码待删除，不得再被新菜单调用
```

### 6.2 P0 表清单补丁（建议稿）

在 V2 P0 基础上增加：

```text
basket_registration_audit
basket_match_team_stat
basket_match_player_stat
```

并明确：

```text
赛程时间规则：第一阶段硬编码
默认：周二/周四/周六，21:00/22:00，每时段最多 6 场
```

### 6.3 文件字段规范

```text
cover_file_id
banner_file_id
icon_file_id
```

不直接存外链 URL，除非项目文件服务明确改成 URL 模式。

### 6.4 第 1 轮开发边界（可直接采用）

**做：**

```text
1. 冻结术语与枚举
2. 新表 DDL（含补丁后的 P0）
3. Entity / Mapper
4. Series Seed
5. 清库脚本/说明（精确表名）
6. 可选示例 Season + PS/Xbox Division
```

**不做：**

```text
1. 报名/审核/赛程/提交业务
2. 前台页面重做
3. 个人报名
4. 旧数据迁移
5. 自动排赛 Job
```

**验收：**

```text
1. 新表可表达 Series → Season → Division
2. 主链路表均有 division_id
3. 四种 Series 种子存在
4. 本地构建通过
5. 无新代码使用 eventId 表示 seasonId
```

---

## 7. 遗漏模块夹生检查（请 ChatGPT 逐条标注）

请在终稿对下表逐条标注：`本期重建` / `本期不做并删除入口` / `第 N 轮`：

```text
旧 ScheduleAutoGenerateJob
旧 basket_schedule_rule / weekday / time_slot
旧 stage / group / round
旧 match dispute / exception
旧 roster change
旧 honor generation
旧 Open registration 全套
旧 local demo data 生成器
旧赛事相关单测
旧前台 /events 与 /tournament 混用路由
旧后台 cnpa/event/list 发布页
旧 registrationAudit / matchAudit 菜单
```

目标：不允许出现“新主链路通了，旧 Job/菜单还在写旧表或误导运营”。

---

## 8. Cursor 对“是否还有不同意”的最终表态

```text
不同意项：无原则性不同意。

有条件同意项：
1. 同名新表必须先清旧表再建。
2. P0 建议补 registration_audit 与单场正式统计表。
3. 封面字段建议 file_id 化。
4. 赛程时间规则第 1 阶段建议硬编码并写死。
5. 清库清单必须落到精确表名后再执行。

除此之外，同意 V2 作为全量重构执行基线。
```

---

## 9. 请 ChatGPT 输出的下版文档结构建议

建议直接产出：

```text
chatgpt重构最终共识版.md
```

建议结构：

```text
1. 已冻结共识（不可再改）
2. 最终表命名与 P0/P1/P2 表清单
3. 建表与清库顺序
4. 精确清库/保留表名附录
5. 本期重建 / 不做 / 删除入口清单（含 Job/Demo/测试）
6. 分轮计划（含第 2/5/6 轮微调）
7. 第 1 轮开发指令（给 Cursor）
8. 第 1 轮验收标准
```

---

## 10. 给双方的一句话共识稿

```text
CNPA 赛事域按 V2 全量重构。
Series → Season → Division 为唯一主线。
只做 TEAM 与常规赛单循环基础版。
旧赛事数据不迁移，旧入口最终删除。
账号、球员、长期球队保留。
表命名以 V2 为准，补齐审核流水与单场正式统计后进入第 1 轮：
只建模型、枚举、表结构与种子数据，不写复杂业务。
```

---

*评审人：Cursor*  
*关联文档：`chatgpt重构v2版本.md`*  
*用途：交给 ChatGPT 评估并产出最终共识版*
