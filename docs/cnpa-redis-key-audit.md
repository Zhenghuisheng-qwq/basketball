# CNPA Redis Key 审计

> 不含完整 Token / 验证码 / 授权码。

## 1. Key 清单

| 用途 | Key 模式 | 模块 | TTL | 隔离 |
|------|----------|------|-----|------|
| 官网会员 Token | `member:token:{uuid}` | Web | 约 30 天 | 与小程序隔离 |
| 小程序 Token（遗留） | `mini:token:{uuid}` | Web | 30 天 | 不同前缀 |
| Admin JWT | `login_tokens:{uuid}` | Admin | token.expireTime | 不同前缀 |
| 邮箱发送冷却 | `member:email:code:limit:{purpose}:{email}` | Web | resend 秒 | 按场景 |
| 邮箱日限额 | `member:email:code:daily:{day}:{email}` | Web | 至日切 | |
| IP 小时限额 | `member:email:code:ip:{hour}:{ip}` | Web | 小时 | |
| 验证码失败次数 | `member:email:code:fail:{bizNo}` | Web | 验证码有效期 | |
| 登录失败 | `member:login:fail:{account}` | Web | lock 分钟 | |
| 公开接口缓存 | `basket:public:{suffix}` | Web | 按构造参数 | |
| 数据结算锁 | `basket:data:settlement:season:{seasonId}` | Admin | 锁超时 | |

## 2. 检查结论

| 项 | 结果 |
|----|------|
| 与小程序 Token 冲突 | 否（`member:` vs `mini:`） |
| 与 Admin JWT 冲突 | 否（`login_tokens:`） |
| 退出清理 Token | 代码有 logout 删除 | 需启动实测 |
| 验证码使用后清理 | 消费时更新 DB + fail key；明文不入 Redis | 静态通过 |
| SMTP 未配置时假成功 | 否，抛「邮箱服务未配置」 | 静态通过 |
| 发布/下线清公开缓存 | 内容服务应调用缓存清理 | 需联调确认 |
| 结算/重开清排行缓存 | 结算服务有缓存失效逻辑 | 需联调确认 |
| 锁释放校验所有者 | 依赖 Redis 锁实现 | 需代码复核+实测 |

## 3. 本轮环境

- Redis `192.168.31.47:6379` TCP 可达（历史检查）。
- 本轮未在文档中记录任何完整 Token。
