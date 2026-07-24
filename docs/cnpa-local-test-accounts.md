# CNPA 本地测试账号说明

> 仅供本地开发使用。禁止写入生产环境。  
> 不含 SMTP 授权码、数据库密码、完整 Token、真实个人信息。

## 1. 官网会员（basketball-web :8881）

数据来源：`sql/cnpa-local-test-data.sql`  
统一本地密码：`LocalTest@123`（BCrypt 入库，仅本地）

| 用途 | 登录邮箱 | account_no | 角色/状态 | 球员认证 | 队长资格 | 所属球队 | 场景 |
|------|----------|------------|-----------|----------|----------|----------|------|
| 普通未认证 | local.test.user01@example.com | LOCAL_TEST_U01 | 正常 | 草稿/驳回样例申请 | 无 | 无 | 注册后资料、草稿认证 |
| 待认证 | local.test.user02@example.com | LOCAL_TEST_U02 | 正常 | 已提交 | 无 | 无 | 认证审核队列 |
| 已认证球员 | local.test.user03@example.com | LOCAL_TEST_U03 | 正常 | 已通过 | 已过期样例 | 本地测试通过球队 | 成员/邀请历史 |
| 队长 | local.test.captain@example.com | LOCAL_TEST_U04 | 正常 | 已通过 | 有效 | 本地测试通过球队（队长） | 建队、邀请、报名 |
| 禁用账号 | local.test.disabled@example.com | LOCAL_TEST_U05 | 禁用 | 无 | 无 | 无 | 登录拒绝 |
| 正式成员 | local.test.member06@example.com | LOCAL_TEST_U06 | 正常 | 已通过 | 已撤销样例 | 通过球队替补 | 成员权限边界 |
| 入队申请人 | local.test.joiner@example.com | LOCAL_TEST_U07 | 正常 | 已通过 | 无 | 待处理邀请/申请 | 接受邀请、入队 |

**优先建议：** 新注册链路仍通过真实邮箱验证码流程创建账号；上表仅用于本地联调样例。

## 2. Admin（basketball-admin :8080）

优先通过若依「用户管理」创建本地角色账号，不在本文件记录真实口令。

建议角色（见 `docs/cnpa-permission-test.md`）：

- 超级管理员
- 赛事运营
- 审核人员
- 数据录入
- 只读
- 无 CNPA 权限

菜单 SQL：`sql/cnpa-admin-menu.sql` 等需在**若依系统库**执行并授权后，`/getRouters` 才可验证。

## 3. 清理

执行 `sql/cnpa-local-test-data-clean.sql` 仅删除 LOCAL_TEST 标识数据。
