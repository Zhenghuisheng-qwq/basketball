# CNPA 第六批执行结果

> 完成时间：2026-07-25

## 1. 执行概况

| 指标 | 数量 |
|---|---|
| 新增数据库表 | 9（restriction、suspension_log、business_audit 等） |
| 扩展表 | discipline_record、announcement、honor_record |
| 新增 Admin 后端类 | ~25（discipline/dashboard/audit/archive/scope） |
| 新增 Admin API | 处罚 6、审计 2、档案 2、工作台 1、赛事授权 3 |
| 新增 Admin-ui 页面 | 6（处罚/档案×2/审计/授权/工作台改造） |
| 新增 API TS | 5 |
| 清理 Mock | 工作台若依默认页已替换（Web-ui Mock 仍部分遗留） |
| 构建 | basketball-admin-ui `npm run build:prod` ✅ |

## 2. 最终纪律模型

```text
discipline_record（事实）
    ↓ activate（乐观锁）
discipline_restriction（当前限制快照）
    ↓ 业务入口 assert/check
报名 / 转会 / 队长 / 名单

discipline_record（MATCH_SUSPENSION）
    ↓ lineup confirm
suspension_log（幂等消耗）→ remaining--

discipline_record（POINT_DEDUCTION）
    ↓ activate
point_adjustment（幂等 DISC:id:POINT）
```

## 3. 最终档案模型

永久球员/球队 Admin 详情聚合 profile + 历史 + 处罚 + 荣誉；赛事快照仍由 season_roster / team_season_profile 承担。

## 4. 业务审计模型

主表 + 字段明细；处罚与赛事授权已写入；只读查询接口。

## 5. 内容运营模型

表结构扩展已完成；荣誉自动生成与规则定时发布为 P1 遗留。

## 6. 工作台模型

真实 COUNT 聚合 + 赛事 scope 过滤 + 卡片快捷跳转。

## 7. 构建结果

| 项目 | 结果 |
|---|---|
| basketball-admin-ui | ✅ build:prod |
| basketball-web-ui | 未在本轮重跑（历史 build 通过） |
| basketball-admin / web | 本机无 mvn，未编译 |

## 8. 数据对账（MySQL 本地）

| 项 | 数量 |
|---|---|
| 球员档案 | 133 |
| 球队 | 19 |
| 处罚记录 | 0（待运营录入测试） |
| 业务审计 | 0 |
| 赛事授权 | 0 |

## 9. 遗留问题

### P0
- 无（前五批阻塞项已解除）

### P1
- 认证/报名/比赛审核等业务审计批量接入
- 荣誉从结算批次自动生成 + 冠军阵容快照写入
- 公告定时发布与规则替代 Admin 操作页
- Web-ui Mock 清理（accountMock、playerIdentityMock）
- `basket_player_team_relation` / `basket_team_profile_change` 业务写入钩子

### P2
- Web 公开：球员资格/处罚摘要 API
- 处罚审批多级流程 UI
- Redis 工作台短缓存

## 10. 数据库安全确认

**本批次没有删除历史处罚、档案、比赛、统计、公告和荣誉数据，所有结构调整均保留了迁移、版本、批次和回滚方案。**

## 11. 部署阶段准备

- **是否允许进入部署阶段**：有条件允许 — 核心链路代码就绪，需完成 P1 联调与 mvn 编译验证
- **阻塞部署**：Java 编译未验证、部分 Web Mock、荣誉/公告自动化未完成
- 详见 `docs/cnpa-release-readiness-checklist.md`
