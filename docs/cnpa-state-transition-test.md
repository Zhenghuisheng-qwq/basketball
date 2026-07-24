# CNPA 状态流转专项测试

> 测试时间：2026-07-24  
> 依据：当前代码枚举 + 本地静态审查；完整 HTTP 流转需服务启动后补测。

## 约定说明

比赛「待确认」在本系统拆分为：

- `match_status = 3`（FINISHED 已结束）
- `data_status = 1`（SUBMITTED 已提交，待确认）→ 确认后 `data_status = 2`

并非独立 PENDING_CONFIRM 枚举值。

## 1. 球员认证

合法：

| 流转 | 预期 | 代码依据 | 实测 |
|------|------|----------|------|
| DRAFT→SUBMITTED→REVIEWING→APPROVED | 通过 | `PlayerCertStatusEnum` + Admin 审核服务 | 未完整 HTTP 跑通（待服务联调） |
| DRAFT→…→REJECTED→修改→再提交 | 允许 | `canEdit/canSubmit` | 静态通过 |

非法（须后端拒绝）：

| 场景 | 预期 | 实测/静态 |
|------|------|-----------|
| APPROVED 再次提交 | 拒绝 | 静态：`canSubmit` 不含 APPROVED |
| REVIEWING 用户继续编辑 | 拒绝 | 静态：`canEdit` 仅 DRAFT/REJECTED |
| 两管理员重复审核 | 条件更新/状态校验拒绝第二次 | 需并发实测 |
| 已通过被普通审核覆盖 | 拒绝 | 需权限+状态实测 |

## 2. 队长资格

合法：未授予 → ACTIVE → EXPIRED/REVOKED  
非法：重复授予有效资格；管理球队中直接撤销（业务应拦截或提示）

本地样例：`LOCAL_TEST_CAP_ACTIVE/EXPIRED/REVOKED`

## 3. 球队

合法：创建→待审→审核中→通过 / 驳回→修改→重提  
非法：非队长改审；已通过非法回退（后端审核服务）

本地样例：`LOCAL_TEST_TEAM_PENDING/OK/REJECT`

## 4. 报名

合法：DRAFT→SUBMITTED→REVIEWING→APPROVED / REJECTED→修改→再提交；WITHDRAWN  
非法：非队长提交他人球队；重复有效报名（唯一索引 `season+team`）

## 5. 比赛

合法：

- DRAFT→SCHEDULED→RUNNING→FINISHED(+data SUBMITTED)→CONFIRMED
- SCHEDULED→POSTPONED→重新安排
- SCHEDULED→CANCELLED
- CONFIRMED→REOPENED→再提交→CONFIRMED

非法：CANCELLED 进入结算；未确认进入结算；CONFIRMED 再确认

本地样例：`LOCAL_TEST_M_*`

## 6. 荣誉 / 公告

荣誉 display_status：0草稿 / 1发布 / 2下线 / 3撤销  
公告 announcement_status：0草稿 / 1发布 / 2下线  

非法：撤销/下线仍公开（公开接口过滤）

## 结论

- 枚举与非法流转门禁在服务层有定义（静态复核通过）。
- 完整「合法路径 + 非法路径」HTTP 结果见 `docs/cnpa-local-api-test-report.md`（启动后补填）。
