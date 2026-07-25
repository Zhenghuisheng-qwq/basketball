# CNPA 全链路测试报告（第六批）

> 测试日期：2026-07-25  
> 环境：本地开发库 192.168.31.47 / 四项目源码

## 1. 自动化构建

| 项目 | 命令 | 结果 |
|---|---|---|
| basketball-admin-ui | `npm run build:prod` | ✅ 通过 |
| basketball-web-ui | 历史轮次 build | ✅（本轮未重跑） |
| basketball-admin | `mvn package` | ⚠️ 本机无 mvn |
| basketball-web | `mvn package` | ⚠️ 本机无 mvn |

## 2. 主链路（代码审查 + 局部验证）

| 链路 | 状态 | 说明 |
|---|---|---|
| 账号→球员认证→档案 | ✅ | 第五批已闭环 |
| 队长→球队认证 | ✅ | 第三 batch |
| 报名→正式球队/阵容 | ✅ | batch4 回填 17/131 |
| 比赛→审核→正式结果→统计 | ✅ | batch5 |
| 处罚→限制→报名/转会/名单 | ✅ | batch6 Service + 联动 |
| 结算→荣誉→名人堂 | ⏳ | 表就绪，自动生成待实现 |
| 公告→官网 | ⏳ | 公告 CRUD 有，定时/规则替代待完善 |
| 业务审计追溯 | ⏳ | 处罚/授权已接，其他待接 |

## 3. 权限与越权（设计级）

- Admin 接口均带 `@PreAuthorize`
- 赛事数据：`BasketTournamentScopeSupport` 已用于 dashboard/discipline create
- **待实测**：非授权赛事管理员直接调 API

## 4. 并发幂等（设计级）

| 场景 | 机制 |
|---|---|
| 处罚生效 | updateRecordStatusIfMatch |
| 禁赛消耗 | suspension_log 唯一键 |
| 扣分 | point_adjustment 唯一键 |
| 赛事授权 | countActiveGrant + uk |

## 5. 数据一致性

- 正式结果 vs 草稿：batch5 official_result 分离 ✅
- 处罚 vs 限制：分表 ✅
- 永久 vs 快照：字段分离 ✅

## 6. 建议人工回归清单

1. Admin 创建 MATCH_SUSPENSION 处罚 → 生效 → 保存名单应失败
2. 锁定名单 → 检查 suspension_log 与 remaining 减 1
3. REGISTRATION_BAN → 报名 revalidate 应 FAIL
4. 工作台数字与各列表 COUNT 一致
5. 赛事授权后 dashboard 仅显示授权赛事待办

## 7. 结论

第六批核心纪律闭环与运营工作台已具备本地验收基础；荣誉/公告自动化与 Web Mock 清理需在部署前完成 P1 项。
