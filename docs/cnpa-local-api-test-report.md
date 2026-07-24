# CNPA 本地 API 测试报告（第八轮）

> 禁止记录完整密码 / Token / SMTP 授权码 / 完整验证码 / 证件信息。  
> Web 实际端口：**8881**（非 8081）。

## 环境

| 项 | 结果 |
|----|------|
| MySQL 192.168.31.47/basketball | 连接成功；LOCAL_TEST 数据已加载 |
| Redis 192.168.31.47:6379 | TCP 历史可达 |
| CNPA_MAIL_* 环境变量 | **全部未设置** |
| SMTP 真实发送 | **未验证** |

## 用例摘录

| 编号 | 模块 | 接口 | Method | 账号 | 预期 | 实际 | 通过 | 备注 |
|------|------|------|--------|------|------|------|------|------|
| API-R8-001 | 验证码 | `/basket/member/auth/email-code/send` | POST | 匿名 | 未配置返回业务失败 | code=500，msg 为邮箱服务未配置（非假成功） | 是 | SMTP 未配置 |
| API-R8-002 | 注册 | `/basket/member/auth/register` | POST | 匿名 | 验证码消费后注册 | 未发信 → **未验证** | 否 | SMTP 阻断 |
| API-R8-003 | 密码登录 | `/basket/member/auth/login/password` | POST | LOCAL_TEST_U04 | code=200 + token | 实测通过（字段 `account`） | 是 | |
| API-R8-004 | 当前会话 | `/basket/member/auth/session` | GET | Bearer | 返回 memberId=920004 | 实测通过 | 是 | |
| API-R8-005 | 退出 | `/basket/member/auth/logout` | POST | Bearer | 旧 Token 401 | 实测通过 | 是 | |
| API-R8-006 | 公告分页 | `/basket/public/announcement/page` | GET | 匿名 | 仅已发布 | 修复 `-parameters` 后通过，仅 LOCAL_TEST_ANN_PUB | 是 | DEF-R8-011 |
| API-R8-007 | 首页 | `/basket/public/home` | GET | 匿名 | code=200 | 实测通过，含 LOCAL_TEST 比赛 | 是 | |
| API-R8-008 | 禁用登录 | login/password | POST | U05 | 拒绝 | 实测拒绝 | 是 | |
| API-R8-009 | 越权 | 改 memberId/teamId | * | 用户 A | 403/业务拒绝 | 静态归属校验存在 | 部分 | 未全量实测 |
| API-R8-010 | SQL 注入 `${}` | Mapper | - | - | 无前端拼进 `${}` | Web mapper 未检出危险 `${}` | 是 | 静态 |

## 验证码安全（静态）

| 规则 | 结果 |
|------|------|
| SecureRandom | 已修复 |
| 响应不回验证码 | 是（仅 bizNo） |
| 日志不打明文码 | 是（mask email） |
| 场景隔离 | purpose 字段隔离 |
| 冷却/日限/IP 限 | 配置存在 |
| 一次消费 | consumeVerifyCode |
| 发送失败不落成功码 | FAIL 状态插入后抛错 |

## 真实邮件收件（QQ/163/Outlook）

**全部未执行**（本地未配置 `CNPA_MAIL_AUTH_CODE` 等）。

## 启动后补测清单

1. 密码登录 LOCAL_TEST 账号  
2. session / logout  
3. 公开首页接口  
4. Admin 登录 + 一页列表  
5. 配置 SMTP 后再测注册/登录验证码
