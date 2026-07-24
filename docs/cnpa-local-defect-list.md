# CNPA 第八轮本地缺陷清单

## 汇总

| 等级 | 数量 | 已修复 | 未修复/待环境 |
|------|------|--------|----------------|
| P0 | 0 | 0 | 0 |
| P1 | 4 | 3 | 1 |
| P2 | 4 | 1 | 3 |
| P3 | 3 | 1 | 2 |

---

### DEF-R8-001（P1）验证码使用非安全随机数

- 模块：Web 邮箱验证码
- 复现：阅读 `BasketMemberAuthServiceImpl#sendEmailCode`
- 预期：SecureRandom
- 实际：`RandomUtil.randomNumbers`
- 根因：Hutool 便捷 API
- 修复：`generateNumericCode` + `SecureRandom`
- 文件：`basketball-web/.../BasketMemberAuthServiceImpl.java`
- 回归：编译通过后静态确认；真实发送因 SMTP 未配置未测
- 状态：**已修复**

### DEF-R8-002（P1）SMTP 未配置时文案不统一 / 空密码可能误配

- 模块：邮件发送
- 预期：明确「邮箱服务未配置」，且无授权码不得假成功
- 实际：原校验未检查 password
- 修复：校验 host/username/password；文案改为「邮箱服务未配置」；支持 `from-name`
- 文件：`MemberMailServiceImpl.java`、`application.yml`
- 状态：**已修复**（真实 SMTP 发送：**未验证**，环境变量未配置）

### DEF-R8-011（P1）Web 公开分页接口因缺少 `-parameters` 编译导致 500

- 模块：`/basket/public/announcement/page` 等
- 复现：调用带 `currentPage/pageSize` 的 GET
- 实际：`IllegalArgumentException: Name for argument ... not specified`
- 根因：`basketball-web` 未开启 `javac -parameters`，`@RequestParam` 无显式 name 时失败
- 修复：`pom.xml` 增加 `maven-compiler-plugin` `-parameters`；公告 Controller 补全显式参数名
- 回归：重启后 `/announcement/page` 返回 200 且仅含已发布公告
- 状态：**已修复**

### DEF-R8-003（P1）Web UI 账户域仍引用运行时 Mock

- 模块：个人中心/球队活动
- 复现：`stores/account.ts`、`stores/eventTeam.ts` 等 import `@/mock/*`
- 预期：走真实 Web API
- 实际：部分页面/Store 仍 Mock
- 根因：第二～三轮遗留未完全切换
- 修复：本轮仅迁出密码校验；完整脱 Mock 列入技术债
- 状态：**未完全修复**（记录技术债）

### DEF-R8-004（P2）仓库根目录缺少 `.gitignore`，易误提交本地密钥

- 修复：新增根 `.gitignore`（忽略 `.env*`、`application-local.yml` 等）
- 状态：**已修复**（历史已入库的 yml 密码不在本轮清除，属技术债）

### DEF-R8-005（P2）架构文档端口与实际不符

- 文档写 Web 8081；实际 `application.yml` 为 **8881**
- 处理：测试报告按 8881；文档勘误列入技术债
- 状态：**未改架构审计原文**（避免大范围改历史报告）

### DEF-R8-006（P2）Admin `/getRouters` 与 CNPA 菜单未在若依库验证

- 状态：**未验证**（缺菜单执行环境确认）

### DEF-R8-007（P2）账号 Store 混用真实 API 与 Mock 易导致状态不同步

- 关联 DEF-R8-003
- 状态：待后续

### DEF-R8-008（P3）CommunitySection 使用 communityLinks Mock

- 非核心业务；保留外链配置级 Mock
- 状态：可接受 / 后续

### DEF-R8-009（P3）第六轮部分 Admin 类型仍用 Vo 命名

- 与第八轮命名规范不完全一致；不强制大面积改名
- 状态：技术债

### DEF-R8-010（P3）测试数据中队长曾关联多支球队样例

- 已通过 `is_current=0` 降低冲突；一致性专项仍以通过球队为准
- 状态：已缓解
