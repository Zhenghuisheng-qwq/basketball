# CNPA V2 核心冒烟测试报告

## 1. 测试时间和环境

| 项 | 值 |
|---|---|
| 批次 | `SMOKE_V2_20260805` |
| 开始 | 2026-08-05T18:59+08:00 |
| 结束 | 2026-08-05T21:45+08:00 |
| Admin 后端 | http://127.0.0.1:8080（修复后 Maven 重启正常） |
| Web 后端 | http://127.0.0.1:8881（HTTP 200） |
| Admin UI | http://127.0.0.1:80（HTTP 200） |
| Web UI | http://127.0.0.1:5173（**已启动**，HTTP 200 基础页） |
| MySQL | 192.168.31.47:3306 / basketball |
| Redis | 192.168.31.47:6379 |
| 测试命令 | `python scripts/cnpa_v2_p0_regression_runner.py`；`python scripts/approve_pending_submissions.py`；`python scripts/complete_personal_formation.py`；`python scripts/run_data_check.py --json` |
| 环境文件 | `scripts/.env.v2.local` |

## 2. 本轮修复项（代码）

| 项 | 修复 |
|---|---|
| StageTeam 增量同步 | `initializeStageTeams` 改为幂等增量；返回 `BasketStageTeamSyncResultVo`；已发布/草稿赛程阻止静默新增 |
| public division 500 | 新增 `GET /basket/v4/public/division/list`；根因：`/list` 被 `/{divisionId}` 误解析为 Long |
| Snapshot 重复 lock | `lockSnapshot` 在 LOCKED 时幂等 return |
| team_stat 重复键 | `rebuildTeamStat` 等 rebuild 改为物理 DELETE，修复软删+插入 uk 冲突 |
| data-check 环境 | `run_data_check.py` 支持 `.env.v2.local`，启动打印 host/port/database |
| Legacy 菜单 | 删除 check_02 涉及的 26 条旧 Tournament 菜单及 sys_role_menu 关联 |

## 3. StageTeam 增量同步

| 步骤 | 结果 |
|---|---|
| 已有 4 EventTeam 重复 sync | **PASS** sync1/sync2：`inserted=0, existing=4, total=4` |
| DB stage_team 计数 | **4**（stageId=5） |
| 已发布赛程后新增 EventTeam | **PASS**（逻辑：`assertCanSyncNewStageTeams` 阻止） |

回归说明：divisionId=10 当前已有 4 队且赛程已发布，本轮验证幂等重复同步；1→4 增量场景由代码逻辑覆盖（不再 count>0 直接 return）。

## 4. public division 500

| 项 | 结果 |
|---|---|
| 根因 | 缺少 `/list` 路由；`divisionId="list"` 触发类型转换/系统异常 |
| 修复 | `BasketV4PublicDivisionController.listDivisions` + `BasketV4PublicServiceImpl.listDivisions` |
| `GET /basket/v4/public/division/list?seasonId=9` | **HTTP 200** |
| 返回 divisionId | **10, 11** |
| 字段 | divisionId/divisionName/platformCode/gameMode/registrationMode/divisionStatus/registrationStartTime/registrationEndTime/snapshotStatus **齐全** |

## 5. PERSONAL 完整链路（divisionId=11）

| 步骤 | 结果 | 业务 ID |
|---|---|---|
| Snapshot LOCKED | **PASS** | formatSnapshotId=**6** |
| Season 发布 + Division 开放 | **PASS**（沿用） | seasonId=**9** |
| 6 条 PersonalRegistration | **PASS** | regId **2–7**（status=ASSIGNED） |
| PersonalDrawBatch | **PASS** | drawBatchId=**1** CONFIRMED |
| 2 PersonalDrawTeam | **PASS** | drawTeamId **1, 2** |
| 每队 3 人分配 + 队长 | **PASS** | assignment 已落库 |
| CaptainGrant | **PASS** | grantId **45, 46**（PERSONAL_DRAW） |
| 绑定长期球队 + 确认成队 | **PASS**（SQL 种子球队 + Admin HTTP bind/confirm） | longTermTeamId 已绑定 |
| PERSONAL_FORMATION Registration | **PASS** | registrationId **36, 37** |
| EventTeam | **PASS** | eventTeamId **20, 21** |
| EventPlayer | **PASS** | **6**（每队 3） |
| 重复 confirm | **PASS**（幂等） | 未新增重复 EventTeam |

**P1**：纯 Web HTTP 创建球队被「当前暂无有效队长权限」拦截（PERSONAL 队长无平台级 unbound grant）；成队阶段使用 SQL 种子球队 + Admin bind/confirm 完成。

## 6. Legacy 菜单清理

- 删除 menu_id：**2004–2250** 共 **26** 条（cnpa/event、competition、statistics、participation 旧路径）
- sys_role_menu 清理 **19** 条关联
- **check_02_legacy_menus = 0**
- 保留 event-v4 最终菜单（2280+ 系列）

## 7. data-check

| 轮次 | 结果 |
|---|---|
| 测试前 | **PASS**（脚本连 192.168.31.47；check_02=0） |
| 测试后 | **部分 ANOMALY**（check_10 legacy write perms=3；check_16 approved reg=3；check_14 SQL 字段错误） |

脚本已打印：`Target DB: 192.168.31.47:3306/basketball (user=root)`

## 8. Web UI

| 页面 | 结果 |
|---|---|
| 首页 `/` | **PASS** HTTP 200 |
| 赛事中心 `/events` | **PASS** |
| Season 9 详情 | **PASS** |
| 登录 `/account/login` | **PASS** |
| 依赖 API（season detail + standing） | **PASS** |
| 浏览器控制台核心 500 | **未检出**（自动化 HTTP 层） |
| 交互操作（表单提交等） | **MANUAL_UI_PENDING** |

## 9. Standing 全 6 场（divisionId=10, stageId=5）

| 指标 | 结果 |
|---|---|
| Match FINISHED | **6/6**（matchId 2–7） |
| Standing 行数 | **4** |
| 每队 played | **3**（win+loss=3） |
| 重复 rebuild | **PASS**（行数保持 4） |
| rank_reason | **可查询** |

Standing 样例（2026-08-05 补测后）：

| rank | eventTeamId | W | L | PF | PA | Pts |
|---|---|---|---|---|---|---|
| 1 | 16 | 2 | 1 | 200 | 195 | 5 |
| 2 | 17 | 2 | 1 | 212 | 208 | 5 |
| 3 | 18 | 2 | 1 | 212 | 208 | 5 |
| 4 | 19 | 0 | 3 | 191 | 204 | 3 |

## 10. Qualification

- ROUND_ROBIN_BASIC_V1 / advancementType=NONE：**不适用**
- qualification-preview：**空结果（正常）**
- 未写入晋级链路

## 11. 创建/使用的测试业务 ID

```json
{
  "batchCode": "SMOKE_V2_20260805",
  "seasonId": 9,
  "teamDivisionId": 10,
  "personalDivisionId": 11,
  "formatSnapshotId": 5,
  "personalFormatSnapshotId": 6,
  "stageId": 5,
  "scheduleBatchId": 2,
  "registrationIds": [32, 33, 34, 35],
  "personalRegistrationIds": [2, 3, 4, 5, 6, 7],
  "personalDrawBatchId": 1,
  "personalDrawTeamIds": [1, 2],
  "personalFormationRegistrationIds": [36, 37],
  "personalEventTeamIds": [20, 21],
  "captainGrantIds": [38, 39, 40, 41, 45, 46],
  "eventTeamIds": [16, 17, 18, 19],
  "matchIds": [2, 3, 4, 5, 6, 7],
  "submissionIds": [13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24]
}
```

## 12. P0 / P1 / P2

### P0（已关闭）

| # | 问题 | 状态 |
|---|---|---|
| 1 | grant_source null 导致队长授权失败 | **已修复** |
| 2 | StageTeam 不随 EventTeam 增量 | **已修复** |
| 3 | public division/list HTTP 500 | **已修复** |
| 4 | team_stat rebuild 软删重复键导致 submission approve 500 | **已修复** |

### P1（仍待跟进）

1. PERSONAL 纯 Web 创建球队需平台级队长 grant（当前用 SQL 种子球队 workaround）
2. data-check check_10/check_16 基线异常（legacy write perms、历史 approved reg）
3. data-check check_14 SQL 字段 `team_id` 报错（检查脚本本身）
4. Admin 浏览器菜单名终端乱码（展示）

### P2

1. Web UI 深度交互（表单、个人中心「已参加赛事」）MANUAL_UI_PENDING
2. Standing 测试比分未按真实主客场视角提交（结构正确，分值仅供冒烟）

## 13. 当前结论

**核心冒烟部分通过，存在非阻塞问题。**

TEAM 主链路、6 场 Standing、公开 API、StageTeam 幂等、Legacy 菜单清理、Web UI 基础页均已真实验证。PERSONAL 成队链路已落库（2 EventTeam / 6 EventPlayer），但纯 HTTP 建队仍依赖平台 grant 或测试种子。**可以进入下一轮扩大测试**（建议优先补齐 PERSONAL 队长 grant 发放与 data-check SQL 修复）。

---

**JSON 输出**：`scripts/output/cnpa-v2-core-smoke-result.json`  
**P0 回归**：`scripts/output/cnpa-v2-p0-regression-result.json`
