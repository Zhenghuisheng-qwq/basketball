# CNPA 业务审计设计

## 1. 与若依操作日志区别

| 维度 | 若依 sys_oper_log | CNPA basket_business_audit |
|---|---|---|
| 目的 | 技术排错 | 业务追溯 |
| 内容 | 请求/参数/返回 | 业务对象、变更摘要、字段明细 |
| 可删改 | 管理员可清理 | 只读，不提供删除接口 |
| 敏感信息 | 可能含参数 | 明细表标记 sensitive，不写密码/Token |

## 2. 表结构

- `basket_business_audit`：audit_no、module、target_type/id、operation_type、summary、reason、operator
- `basket_business_audit_field`：field_name、field_label、old_value、new_value、data_type、sensitive_flag

## 3. 写入入口

- `BasketBusinessAuditWriter.writeAudit(...)`
- 已接入：处罚创建/生效/撤销、赛事授权授予/撤销
- 待接入：认证审核、报名审核、比赛数据审核、结算、公告发布、荣誉生成

## 4. Admin 查询

- `GET /basket/businessAudit/list`
- `GET /basket/businessAudit/{id}` — 含 fields 明细

## 5. 防重复

- 同一业务对象同一 operation + 版本可在外层 Service 用状态条件更新保证单次写入
