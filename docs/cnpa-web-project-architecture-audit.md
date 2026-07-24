# CNPA Web 项目架构调查报告

> 调查时间：2026-07-24  
> 调查范围：`basketball-admin` / `basketball-admin-ui` / `basketball-web` / `basketball-web-ui` / `generator`  
> 约束：本轮仅分析，未修改任何业务代码，未新增接口，未改数据库。

---

## 1. 调查结论摘要

1. **无 Maven 聚合父工程**：各模块独立 POM / npm，彼此无 `common` / `framework` 共享模块依赖。
2. **`basketball-admin`（8080）**：典型若依单体，管 `sys_*`，具备完整 JWT + Spring Security + Redis + 图形验证码 + 本地文件上传；**不含** `basket_*` 业务表。
3. **`basketball-web`（8081）**：官网/会员业务后端；**58 张 `basket_*` 表的生成式 CRUD 全部在本模块**；另含小程序遗留 `user` / `user_sensitive_info` / `user_login_session` 与未暴露 HTTP 的 `AuthService`。
4. **`basketball-web` 无 Spring Security、无 JWT、无 BCrypt、无邮件/短信发送、无文件实际上传**；仅有拦截器 + Redis UUID Token（半成品且与拦截器校验路径不一致）。
5. **官网会员表是 `basket_member_account` 等**，与 admin 的 `sys_user`、小程序遗留的 `user` **三套用户体系互不打通**。
6. **`basketball-web-ui` 个人中心与登录 100% Mock**；`src/api/request.ts` 已预留 `Authorization: Bearer`，但全项目未被引用；Vite 代理指向 **admin:8080**，未指向 **web:8081**。
7. **上一版设想中的统一 `/web` 前缀：当前代码中不存在**；业务 Controller 实际前缀是 `/basket/...`；鉴权白名单写的是 `/api/auth/*`（无对应 Controller）。
8. **后续开发应在 `basketball-web` 内复用本模块 Entity/Dao/Service，扩展业务方法；禁止依赖 admin Controller；禁止前端直接调 admin。**

---

## 2. 项目模块结构

### 根结构（无 parent pom）

```text
scaffolding-v2/
├── basketball-admin      # 若依管理端后端 :8080
├── basketball-admin-ui   # 管理端前端（Vue/Vite，含占位 pom）
├── basketball-web        # 官网/会员业务后端 :8081  ← 58 表 CRUD 在此
├── basketball-web-ui     # 官网前端（Vue3）
├── generator             # JavaFX 代码生成器
├── jsj-mini              # 历史/并列小程序后端残留（独立 pom）
├── sql/basketball.sql    # 58 张 basket_* 表 DDL
└── docs/
```

| 模块 | 启动类 | 职责 |
|------|--------|------|
| basketball-admin | `com.zhs.RuoYiApplication` | 后台管理员、菜单权限、系统配置 |
| basketball-web | `com.zhs.MiniApplication` | 官网会员、赛事业务、生成 CRUD |
| basketball-web-ui | Vite | 官网 + 个人中心 UI |
| basketball-admin-ui | Vite | 管理后台 UI |
| generator | — | 代码生成，不参与运行时 |

### 依赖关系（实际）

```text
basketball-admin  （独立，不依赖 web）
basketball-web    （独立，不依赖 admin）
basketball-web-ui （独立，开发代理误指 admin:8080）
```

**明确回答：**

| 问题 | 结论 |
|------|------|
| Web 复用哪个模块 Entity？ | **本模块** `basketball-web` 的 `com.zhs.model.Basket*` |
| Web 复用哪个 Mapper？ | **本模块** `com.zhs.dao.Basket*Dao`（及 `mapper/*.xml`） |
| Web 复用哪个 Service？ | **本模块** `IBasket*Service` / `*ServiceImpl` |
| 是否需要抽取公共业务模块？ | **当前无必要**；两后端无共享依赖，强行抽 common 成本高 |
| Web 是否会错误依赖 Admin Controller？ | 当前 POM **无** admin 依赖；后续也**禁止** HTTP 调 admin |

证据：`basketball-web/pom.xml` 无 `<parent>`、无对 `basketball-admin` 的 dependency。

---

## 3. 后端启动与配置

### basketball-web

| 项 | 实际值 | 证据 |
|----|--------|------|
| 启动类 | `com.zhs.MiniApplication` | `basketball-web/src/main/java/com/zhs/MiniApplication.java` |
| 扫描范围 | 默认 `com.zhs` | 仅 `@SpringBootApplication`，无额外 `scanBasePackages` |
| MapperScan | **无** | 各 Dao `@Mapper` 自注册 |
| 端口 | **8081** | `application.yml` `server.port` |
| context-path | **未配置（`/`）** | 同上 |
| 统一前缀 | **无**；写在 Controller `@RequestMapping` | 如 `/basket/member/account` |
| Redis | 有 | `spring.data.redis.host=192.168.31.47` |
| DB | MySQL `basketball` + Druid | 同上 |
| 邮件/短信 | **无配置** | yml 无 `spring.mail` |
| 多环境 | **无** `application-dev.yml` 等 | resources 仅一份 `application.yml` |
| 跨域 | **未发现** 专用 CORS 配置 | — |

### basketball-admin

| 项 | 实际值 | 证据 |
|----|--------|------|
| 启动类 | `com.zhs.RuoYiApplication` | admin 源码 |
| 端口 | **8080** | `application.yml` |
| context-path | `/` | 同上 |
| 文件上传路径 | `ruoyi.profile: D:/jsj/uploadPath` | 同上 |
| 验证码 | `ruoyi.captchaType: math` | 同上 |
| 密码错误锁定 | `user.password.maxRetryCount: 5` | 同上 |

### Controller 实际访问路径

- Web 业务：`http://host:8081/basket/{resource}/getByPage` 等  
- Admin：`http://host:8080/login`、`/system/...`、`/common/upload`  
- **不存在** 运行时生效的统一 `/web` 前缀

**后续是否需要新增 `/web`？**  
以当前架构论：**不是必须**。现有规范是 `/basket/...`。若产品要求与 admin 路径隔离，可在新增业务接口时自选前缀，但应先统一鉴权拦截范围，而不是照搬上一版假设。

---

## 4. 登录流程

### 4.1 后台管理员（basketball-admin）— 真实可用

```text
SysLoginController.login (POST /login)
→ SysLoginService.login
→ 校验验证码（Redis）
→ 用户名密码（BCrypt）
→ TokenService.createToken(LoginUser)
→ JWT + Redis 缓存 LoginUser
→ 返回 token
```

证据：

- `basketball-admin/.../web/controller/system/SysLoginController.java`
- `.../framework/web/service/SysLoginService.java`
- `.../framework/web/service/TokenService.java`
- 表：`sys_user`（若依）

### 4.2 官网会员（basketball-web）— **未落地**

| 能力 | 状态 |
|------|------|
| 真实登录 Controller | **无**（无 `AuthController`） |
| `AuthService.wxLogin` | **有实现，无 HTTP 暴露** |
| 登录账号表（遗留） | `user` + `user_sensitive_info`（微信 openId） |
| 官网会员表 | `basket_member_account`（仅生成 CRUD） |
| 密码登录 / BCrypt | **无** |
| 注册 | **无**业务实现（仅有表与 CRUD） |
| 邮箱/短信验证码登录 | **无** |

微信登录 Service 调用链（无 Controller）：

```text
AuthServiceImpl.wxLogin
→ WeChatMiniService.getOpenIdByCode
→ 查/建 User + UserSensitiveInfo
→ UUID token → MiniRedisUtil.set(TokenCacheKey)
→ 返回 WxLoginResp
```

证据：`basketball-web/.../service/impl/AuthServiceImpl.java`

### 4.3 前端官网登录 — 纯 Mock

```text
login 页面
→ useAuth.login
→ mock/auth.mockLogin
→ localStorage cnpa_mock_auth
→ accountStore.loadAccountMock
```

证据：`basketball-web-ui/src/composables/useAuth.ts`、`src/mock/auth.ts`

**结论：官网会员不能复用 admin 登录；也不能直接复用小程序 `user` 登录当 `basket_member_account`。可复用的是「Token 解析 Header 约定」「Redis 工具思路」「拦截器模式」，需按会员表重做业务。**

---

## 5. Token 与会话机制

### basketball-web（半成品、双轨不一致）

| 项 | AuthServiceImpl（写） | LoginInterceptor（读） |
|----|----------------------|------------------------|
| Token 类型 | UUID 去横线字符串 | 期望 DB `user_login_session.token` |
| 存储 | Redis `mini:token:{token}` → userId，30 天 | MySQL `user_login_session` |
| 是否互通 | **否** | **否** |

Token 解析（双方可用同一工具）：

- 类：`com.zhs.common.TokenUtil.resolveFrom`（另有重复类 `com.zhs.util.TokenUtil`，逻辑相同）
- Header：`Authorization`（支持 `Bearer `）→ `token` → `access-token`

拦截范围：

```text
WebMvcConfig: 仅拦截 /api/**
排除: /api/auth/wx-login, /api/auth/test-login, /api/auth/test-users
```

业务 CRUD 在 `/basket/**` → **当前不受拦截器保护**。

失败响应：HTTP 401 + JSON `{"code":401,"message":"未登录|登录已过期"}`（字段名是 `message`，与 `R.msg` 不一致）。

证据：

- `config/WebMvcConfig.java`
- `config/LoginInterceptor.java`
- `common/cachekey/user/TokenCacheKey.java`

### basketball-admin

- JWT（jjwt）+ Redis 存 `LoginUser`
- Header：`Authorization: Bearer <jwt>`
- Filter：`JwtAuthenticationTokenFilter`
- 证据：`framework/web/service/TokenService.java`、`SecurityConfig.java`

### 前端 Token 携带（预留）

```text
request.ts → Authorization: Bearer ${getToken()}
utils/auth.ts → localStorage key = cnpa_web_token
```

实际登录未写入 `cnpa_web_token`，只写 Mock 会话。

---

## 6. 自定义认证注解

### basketball-web

| 注解 | 定义文件 | 处理器 | 作用 | 使用示例 | 能否复用 |
|------|----------|--------|------|----------|----------|
| `@NoLoginRequest` | `com.zhs.common.NoLoginRequest` | `LoginInterceptor.preHandle` | 标注后跳过登录校验 | **全项目 0 处使用** | 可复用；需先让拦截器覆盖真实业务路径 |

无 `@Login` / `@PassToken` / `@CurrentUser` 等其它认证注解。

### basketball-admin

| 注解 | 定义 | 处理器 | 说明 |
|------|------|--------|------|
| `@Anonymous` | `common.annotation.Anonymous` | `PermitAllUrlProperties` | 匿名；代码中几乎未标注使用 |
| `@RepeatSubmit` | `common.annotation.RepeatSubmit` | `RepeatSubmitInterceptor` | 防重复提交 |
| `@Log` | … | `LogAspect` | 操作日志 |
| `@DataScope` | … | `DataScopeAspect` | 数据权限（后台） |
| `@RateLimiter` | … | `RateLimiterAspect` | 限流 |

**官网会员开发：应复用 web 模块的 `@NoLoginRequest` + `UserContext` 模式（扩展为会员 ID），不要引入 admin 的菜单权限注解。**

---

## 7. 当前用户上下文

### basketball-web

```java
// com.zhs.common.UserContext
UserContext.setUserId(Long);
Long id = UserContext.getUserId();
UserContext.clear(); // LoginInterceptor.afterCompletion
```

- ThreadLocal，请求结束清理：**有**
- 来源：拦截器查 `user_login_session` 后写入（当前与 Redis 登录未打通）
- 模型：小程序 `user.id`，**不是** `basket_member_account.id`
- 无角色/权限 API

**推荐后续（在现有类上扩展，而非另起炉灶）：**

```java
Long memberId = UserContext.getUserId(); // 登录打通后语义改为 memberId
```

或扩展 `UserContext` 增加 `getRequiredMemberId()`，避免新建平行上下文类。

### basketball-admin

```java
SecurityUtils.getLoginUser();
SecurityUtils.getUserId();
```

仅适用于后台 `LoginUser` / `sys_user`，**不能**给官网会员用。

---

## 8. Security、Filter 与拦截器

| 模块 | 机制 |
|------|------|
| basketball-web | **无** Spring Security / Sa-Token / Shiro；仅 `LoginInterceptor` |
| basketball-admin | Spring Security + `JwtAuthenticationTokenFilter` + 无 Session |

Web 白名单来源：`WebMvcConfig` Java 硬编码。  
Admin 白名单：`SecurityConfig` 硬编码（`/login`、`/register`、`/captchaImage` 等）。

**官网公开接口加入白名单方式（建议，本轮不改）：**  
扩展 `WebMvcConfig.excludePathPatterns`，或对公开方法加 `@NoLoginRequest`，并修正拦截路径覆盖业务 API。

---

## 9. 统一响应与异常处理

### basketball-web 存在两套

| 类 | 字段 | 使用处 |
|----|------|--------|
| `com.zhs.util.R` | `int code`, `String msg`, `T data`；200/500 | 全部 Basket Controller / Service |
| `com.zhs.common.Result` | `Integer code`, `String msg`, `T data` | 仅 `GlobalExceptionHandler` |

证据：`util/R.java`、`common/Result.java`、`exception/GlobalExceptionHandler.java`

**后续 Web 业务应统一使用 `com.zhs.util.R`**（与生成 CRUD 一致）。异常处理器建议改回 `R`，避免双轨（本轮仅记录，不修改）。

`ServiceException`：`com.zhs.exception.ServiceException`，可带 `code`。  
`GlobalExceptionHandler.handleException` 会把 `e.getMessage()` 拼进响应（有泄露风险）。

### basketball-admin

主用 `AjaxResult`（HashMap：`code/msg/data`），与 web 的 `R` **不是同一类**，勿混用。

---

## 10. 参数校验与防重复提交

| 能力 | basketball-web | basketball-admin |
|------|----------------|------------------|
| `@Valid` / Jakarta Validation | **未发现**广泛使用 | 有 |
| `@RepeatSubmit` | **无** | 有实现 |
| 接口限流 | **无** | `@RateLimiter` |
| Redis 锁工具 | 可用 Redis，无统一锁封装 | 有 RedisCache |

Web 侧 Controller 多用手写 `if (StringUtils.isBlank)` + `ServiceException`。

---

## 11. MyBatis-Plus 与数据库能力

| 项 | 实际 |
|----|------|
| 版本 | 3.5.9（`pom.xml`） |
| 驼峰 | `map-underscore-to-camel-case: true` |
| `@TableLogic` | **未使用**；删除靠手动 `setIsDelete(1)` |
| 自动填充 | `ZhsMetaObjectHandler` 填 `createTime`/`updateTime` |
| 分页插件 Bean | **未发现** `PaginationInnerInterceptor`；Controller 直接 `new Page<>()` |
| 主键 | `BasketMemberAccount` 等：`Long` + `IdType.ASSIGN_ID`（雪花）；DDL 为 `AUTO_INCREMENT` → **策略不一致风险** |
| BaseEntity | **无**统一基类；字段散落各实体 |
| type-aliases-package | 配置为过时包名 `com.ruoyi.miniapp.domain` |

58 张表 DDL（`sql/basketball.sql`）均含 `id/create_time/update_time/is_delete`；与 Java 实体字段对应（此前 MCP 核对表名/字段名一致）。

---

## 12. 58 张表 CRUD 现状

### 位置

**全部在 `basketball-web` 模块**（不在 admin）。

### 统一模式（生成器）

每个 `Basket*Controller` 典型端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/getByPage` | `currentPage`/`pageSize` |
| GET | `/getById` | id 参数常为 String |
| GET | `/delete/{id}` | 逻辑删除 |
| POST | `/deleteByIds` | 批量逻辑删除 |
| POST | `/addOrUpdate` | 新增或更新 |

URL 规律：`/basket/{kebab-domain}`，例：

- `/basket/member/account`
- `/basket/player/cert/application`
- `/basket/message/receiver`

### 数量说明

`com.zhs.controller` 下存在大量 `Basket*Controller`（含历史命名如 `BasketWebUserController`、`BasketEmailCodeController`、`BasketFileResourceController` 等），与当前 `sql/basketball.sql` 的 **58 张新表**并存/混杂。  
**数据库侧 58 张新表已对齐 SQL**；Java 侧存在旧生成残留，后续业务应优先使用与新表对应的 Entity/Service。

### 不可直接给 Web 前端调用的 CRUD

| 类型 | 表/Controller | 原因 |
|------|---------------|------|
| 日志 | `basket_login_log`、`basket_account_security_log` | 仅系统写入，禁止用户增删改 |
| 验证码 | `basket_verify_code_record` | 禁止暴露 codeHash / 通用增删 |
| 审核 | `basket_player_cert_audit`、报名审核等 | 仅后台审核流程更新 |
| 状态敏感字段 | `certification_status`、`player_no`、`account_status` 等 | 禁止通用 `addOrUpdate` 由前端改 |

**复用方式：** 在现有 `IBasket*Service` 上增加业务方法；Controller 上增加会员向接口（或沿用现有 Controller 增方法），**不新建 58 套 CRUD，不新建平行 Controller 层替代表级 Controller（按产品约定）**。

---

## 13. 验证码能力

| 能力 | 结论 | 已检查范围 |
|------|------|------------|
| 图形验证码（web） | **无** | web 无 Kaptcha |
| 图形验证码（admin） | **有** | `CaptchaController` `/captchaImage` |
| 邮箱验证码发送 | **无** | 无 `JavaMailSender`、无 `spring.mail` |
| 短信发送 | **无** | 无短信 SDK/配置 |
| 验证码记录表 | **有表+CRUD** | `BasketVerifyCodeRecord` / `/basket/verify/code/record` |
| Redis 频控/一次性消费业务 | **无业务实现** | 仅有表字段设计（`code_hash` 等） |

**结论：会员注册验证码需在 web 模块新增发送/校验业务；可复用 Redis 工具与 `basket_verify_code_record` 表，不能复用 admin 图形验证码当邮箱验证码。**

---

## 14. 文件上传能力

| 模块 | 结论 |
|------|------|
| basketball-web | **无** `MultipartFile` 上传接口；仅有 `BasketFileAsset` 元数据 CRUD（`/basket/file/asset`） |
| basketball-admin | **有** `/common/upload`（本地 `ruoyi.profile`） |

证据搜索：web 源码无实际上传；admin `CommonController` 存在。

**结论：官网头像/认证材料上传不能调 admin；需在 web 新增存储服务并写入 `basket_file_asset`，可参考 admin 的本地上传实现思路，但不依赖 admin。**

---

## 15. 邮件与短信能力

| 渠道 | basketball-web | basketball-admin |
|------|----------------|------------------|
| SMTP / JavaMail | 未发现 | 未发现独立邮件模块 |
| 短信 SDK | 未发现 | 未发现 |
| 配置环境变量化 | 无邮件配置 | 无邮件配置 |

已检查：`spring.mail`、`JavaMailSender`、名称含 Mail/Email/Sms 的发送实现 — web 侧均无发送类。

---

## 16. Redis 公共能力

### basketball-web

| 类 | 作用 |
|----|------|
| `com.zhs.common.RedisCache` | `RedisTemplate` 封装 |
| `com.zhs.common.RedisUtil` | `StringRedisTemplate` KV |
| `com.zhs.util.MiniRedisUtil` | 结合 `MiniCacheKey` 读写（登录 Token 使用） |
| `TokenCacheKey` | `mini:token:{token}`，30 天 |

支持过期时间；可基于现有封装做验证码频控与登录失败计数（需新增业务 Key，不必重写 Redis 基础设施）。

### basketball-admin

`com.zhs.common.core.redis.RedisCache`（若依标准），与 web **不是同一模块类**。

---

## 17. 后台管理员与官网会员边界

| 维度 | 后台管理员 | 小程序遗留用户 | 官网会员（目标） |
|------|------------|----------------|------------------|
| 表 | `sys_user` | `user` / `user_sensitive_info` | `basket_member_account` |
| 后端 | basketball-admin | AuthService（web，未暴露） | 待建设 |
| Token | JWT + Redis LoginUser | UUID Redis / DB session 双轨 | 待统一 |
| 权限 | 菜单/角色 `@PreAuthorize` | 无 | 登录即可 + 业务状态 |
| ID 空间 | 独立 | 独立 | 独立 |

**冲突风险：**

- 若 web-ui 代理继续指向 8080，会员接口会打到 admin → 错误。
- 若混用 admin JWT 与 web UUID，Header 同为 Bearer 但解析端不同 → 必须端口/网关隔离。
- 官网会员**不要**纳入若依 `LoginUser` / 菜单权限体系。

**建议：** 复用 web 侧「UUID/Redis 会话 + Interceptor + UserContext」骨架，用户主体改为 `basket_member_account`；**不要**复用 admin 登录。

---

## 18. basketball-web-ui 请求架构

| 项 | 实际 |
|----|------|
| Vue | 3.5.x |
| TS | 是 |
| Vite | 6.x |
| Axios | `src/api/request.ts`（**无人引用**） |
| Base URL | `VITE_API_BASE_URL=/web-api` |
| 开发代理 | `/web-api` → `http://localhost:8080`（**admin**） |
| Token Header | `Authorization: Bearer ...`（预留） |
| Storage | `cnpa_web_token`（预留，未与 Mock 打通） |
| Pinia | `stores/user.ts`、`stores/account.ts`、`notificationCenter` 等 |

---

## 19. 前端登录与路由守卫

```text
router.beforeEach
→ mock/auth.isAuthenticated()
→ meta.requiresAuth 未登录 → Login
→ meta.guestOnly 已登录 → 个人中心
```

证据：`src/router/index.ts`  
启动：`main.ts` → `initAuthState()`（Mock）

**account 页面未 import `@/api/request`（全 src 搜索 0 命中）。**

---

## 20. Mock 数据分布

### 认证 / 个人中心核心

| 文件 | 用途 |
|------|------|
| `src/mock/auth.ts` | 登录会话 |
| `src/mock/account/*.ts` | 账号/资料/认证/安全/通知/球队关系 |
| `src/mock/accountMock.ts` | 聚合导出 |
| `src/mock/playerIdentityMock.ts` | 球员身份 |
| `src/mock/accountEventsMock.ts` / `tournamentAccountMock.ts` | 我的赛事 |
| `src/mock/accountNotificationsMock.ts` | 通知中心 |
| `src/mock/eventTeamMock.ts` 等 | 赛事球队管理 |

### 其它页面 Mock（本轮可不删）

首页、赛事中心、球队、球员、名人堂等仍有独立 mock，**不在个人中心闭环内**。

**替换 Mock 最小范围：**  
`useAuth` + `accountStore` + `notificationCenterStore` + 个人中心路由页；先接通真实 API，再删除对应 mock 引用（本轮调查不删）。

---

## 21. 接口路径和前缀结论

| 问题 | 结论 |
|------|------|
| 后端业务实际前缀 | **`/basket/...`**（Controller 上声明） |
| context-path | 无 |
| `/web` 是否存在 | **不存在** |
| `/api` 是否存在业务实现 | 仅拦截器白名单字符串，**无 AuthController** |
| 前端页面路由 | `/account/...`、`/login`、`/register` 等 |
| 前端 API Base | `/web-api`（代理到 8080，错误指向） |
| Admin / Web 区分 | **端口区分**（8080 vs 8081），非路径前缀 |

**后续官网接口路径规范（基于现状）：**

1. **优先沿用 `/basket/...` 下在对应表 Controller 增加业务方法**（符合「不新建 Controller」约束时）；或  
2. 若需与生成 CRUD 隔离，再议新前缀——**不得默认假设必须 `/web`**。  
3. 前端代理必须改为指向 **`localhost:8081`**（或网关到 web）。

---

## 22. 现有能力复用矩阵

| 能力 | 是否存在 | 实现位置 | 直接复用 | 需扩展 | 建议 |
|------|----------|----------|----------|--------|------|
| 登录（会员） | 否（仅微信 Service 未暴露） | `AuthServiceImpl` | 否 | 是 | 按 `basket_member_account` 新做；可参考 Token 写入 Redis |
| Token 生成 | 部分 | UUID + `MiniRedisUtil` | 部分 | 是 | 统一写 Redis，并与拦截器校验一致 |
| Token 校验 | 部分 | `LoginInterceptor`（DB） | 部分 | 是 | 改为读 Redis；覆盖业务路径 |
| 自定义登录注解 | 有 | `@NoLoginRequest` | 是 | 是 | 补使用点 + 修正拦截范围 |
| 当前用户获取 | 有 | `UserContext` | 是 | 是 | 语义改为 memberId |
| Redis | 有 | `RedisCache`/`MiniRedisUtil` | 是 | 否 | 直接复用 |
| 图形验证码 | 仅 admin | CaptchaController | 否（跨模块） | — | 会员用邮箱验证码另做 |
| 邮箱验证码 | 无 | — | — | 新增 | 新业务 + 表 `basket_verify_code_record` |
| 短信验证码 | 无 | — | — | 新增/配置 | 无配置勿伪造成功 |
| 文件上传 | 仅 admin | CommonController | 否 | 新增于 web | 写 `basket_file_asset` |
| 密码加密 | 仅 admin BCrypt | SecurityConfig | 思路可学 | 新增于 web | web 引入 spring-security-crypto 即可 |
| 统一响应 | 有 | `com.zhs.util.R` | 是 | 统一异常也用 R | 勿新建 |
| 全局异常 | 有 | `GlobalExceptionHandler` | 是 | 小改 | 勿新建体系 |
| 分页 | 有用法 | MP `Page` | 是 | 可加插件 | 参数名 `currentPage/pageSize` |
| 逻辑删除 | 手动 | Controller | 部分 | 可配 `@TableLogic` | 本轮可不改全局 |
| 自动填时间 | 有 | `ZhsMetaObjectHandler` | 是 | 否 | — |
| 防重复提交 | 仅 admin | RepeatSubmit | 否 | 可后补 | — |
| 接口限流 | 仅 admin | RateLimiter | 否 | 可用 Redis 自建频控 | — |
| 权限校验 | 仅 admin 菜单 | Security | 否 | 会员用登录态+数据归属 | — |
| 操作/登录日志表 | 有表+CRUD | Basket*Log | 表复用 | 业务写入 | 禁止开放增删改给前端 |
| 前端 request | 有未用 | `api/request.ts` | 是 | 接 401 | 改代理到 8081 |
| 前端登录 Store | Mock | useAuth/userStore | 部分结构 | 换真实 API | — |
| 前端路由守卫 | 有 | router + mock | 是 | 改鉴权数据源 | — |

---

## 23. 上一版设想冲突检查

| 预设 | 判定 | 原因 |
|------|------|------|
| 增加 `/web` 前缀 | **错误（作为必然前提）** | 现网规范是 `/basket`；`/web` 不存在 |
| 新建 JWT 工具 | **错误** | web 现用 UUID+Redis；admin 的 JWT 属另一模块 |
| 新建 CurrentMemberContext | **部分正确** | 已有 `UserContext`，应扩展而非平行新建 |
| 新建登录注解 | **错误** | 已有 `@NoLoginRequest` |
| 新建 Token 注解 | **错误** | 无必要；Header 解析已有 `TokenUtil` |
| 新建 Redis 工具 | **错误** | 已有 `MiniRedisUtil`/`RedisCache` |
| 新建文件上传服务 | **正确（需新增）** | web 确实没有 |
| 新建统一返回对象 | **错误** | 用 `R` |
| 新建全局异常 | **错误** | 已有，最多统一返回类型 |
| 新建分页结构 | **错误** | 用 MP `IPage` |
| 新建登录 Store | **部分正确** | 已有 Store，应改数据源非推倒重来 |
| 新建 Axios 实例 | **错误** | 已有 `request.ts` |
| web 依赖 admin | **错误** | 禁止 |
| 复用后台管理员登录 | **错误** | 用户表/权限模型不同 |
| 会员纳入 admin 认证体系 | **错误** | 会引入菜单权限与角色依赖 |

---

## 24. 不需要重复开发的能力

- `com.zhs.util.R` 统一响应  
- `ServiceException` + `GlobalExceptionHandler`  
- `TokenUtil.resolveFrom` Header 解析约定  
- `@NoLoginRequest` 注解机制  
- `UserContext` ThreadLocal 模式（需改语义）  
- Redis：`MiniRedisUtil` / `RedisCache` / `TokenCacheKey` 模式  
- MyBatis-Plus：`Basket*Dao` / `IBasket*Service` / 实体  
- 生成 CRUD 骨架（保留，前端不直连）  
- 前端：`request.ts`、`utils/auth.ts`、Pinia、路由守卫结构  
- `ZhsMetaObjectHandler` 时间填充  

---

## 25. 后续确实需要新增的能力

| 类型 | 内容 |
|------|------|
| 完全新增 | 会员注册/密码登录/验证码登录 HTTP；邮箱发送；文件上传存储；会员业务聚合 Service（概览/赛事/消息） |
| 扩展现有 | 拦截器改为校验 Redis Token；覆盖业务路径；`UserContext`→会员；AuthService 对接 `basket_member_account`；异常统一返回 `R` |
| 只加业务方法 | 在现有表对应 Service/Controller 上增加会员向 API（非再生成 CRUD） |
| 只调配置 | web-ui 代理改为 8081；邮件 SMTP 环境变量；上传目录可配置 |
| 不要做 | 依赖 admin；复用 sys_user 登录；前端直连 `/getByPage`/`addOrUpdate`；固定验证码 |

---

## 26. 下一轮开发建议

1. **打通鉴权骨架（先于业务）**  
   - 统一 Token：登录写 Redis，拦截器读 Redis  
   - 拦截 `/basket/**` 中需登录的路径，公开接口 `@NoLoginRequest`  
   - `UserContext` 存 `memberId`

2. **在 `BasketMemberAccountController`（及验证码/资料等已有 Controller）上增加会员业务方法**  
   - 不新建平行 AuthController（若坚持「不新建 Controller」）  
   - 或：若必须路径清晰，需产品确认是否允许唯一的认证入口 Controller——**以调查后产品决策为准**

3. **路径**  
   - 默认继续 `/basket/...`；前端 `VITE`/`proxy` 指向 8081  
   - 不强制引入 `/web`

4. **第一批真实闭环**  
   ```text
   注册/登录 → session/me → 概览 → 资料 → 球员认证提交 → 我的赛事列表 → 消息 → 账号设置
   ```

5. **前端**  
   - 启用 `api/request.ts`  
   - `useAuth` / `accountStore` 换真实 API  
   - 去掉个人中心 Mock 引用（分步）

6. **禁止**  
   - Web 调 Admin CRUD  
   - 前端调生成式 `addOrUpdate` 改认证状态  
   - 日志表开放写接口  

---

## 27. 已知问题与风险

1. Token 双轨（Redis vs DB session）导致鉴权不可用。  
2. 拦截器未覆盖 `/basket/**`，CRUD 当前裸奔。  
3. 无 AuthController，白名单路径空悬。  
4. `R` vs `Result`；401 用 `message` 字段。  
5. springfox + springdoc 双依赖；注解仍用 `io.swagger.annotations`。  
6. 主键 `ASSIGN_ID` vs DDL `AUTO_INCREMENT`。  
7. 无 `@TableLogic`，逻辑删除靠手写。  
8. web-ui 代理指向 admin:8080。  
9. 三套用户表并存，易误接。  
10. Controller 层存在历史生成类与新 58 表混杂，需按表名谨慎选用。  
11. `GlobalExceptionHandler` 可能回传内部异常信息。  

---

## 28. 证据文件清单

### basketball-web

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/com/zhs/MiniApplication.java`
- `src/main/java/com/zhs/config/WebMvcConfig.java`
- `src/main/java/com/zhs/config/LoginInterceptor.java`
- `src/main/java/com/zhs/common/NoLoginRequest.java`
- `src/main/java/com/zhs/common/UserContext.java`
- `src/main/java/com/zhs/common/TokenUtil.java`
- `src/main/java/com/zhs/util/TokenUtil.java`（重复）
- `src/main/java/com/zhs/util/R.java`
- `src/main/java/com/zhs/common/Result.java`
- `src/main/java/com/zhs/exception/GlobalExceptionHandler.java`
- `src/main/java/com/zhs/exception/ServiceException.java`
- `src/main/java/com/zhs/service/impl/AuthServiceImpl.java`
- `src/main/java/com/zhs/common/cachekey/user/TokenCacheKey.java`
- `src/main/java/com/zhs/handler/ZhsMetaObjectHandler.java`
- `src/main/java/com/zhs/model/BasketMemberAccount.java`
- `src/main/java/com/zhs/controller/BasketMemberAccountController.java`

### basketball-admin

- `src/main/resources/application.yml`
- `src/main/java/com/zhs/RuoYiApplication.java`
- `.../web/controller/system/SysLoginController.java`
- `.../framework/web/service/TokenService.java`
- `.../framework/config/SecurityConfig.java`
- `.../web/controller/common/CommonController.java`
- `.../web/controller/common/CaptchaController.java`
- `.../common/annotation/*.java`

### basketball-web-ui

- `package.json`
- `vite.config.ts`
- `.env.development`
- `src/api/request.ts`
- `src/utils/auth.ts`
- `src/composables/useAuth.ts`
- `src/mock/auth.ts`
- `src/stores/user.ts` / `account.ts`
- `src/router/index.ts`
- `src/mock/account/**`

### 数据库

- `sql/basketball.sql`（58 表）
- MySQL MCP 库 `basketball` 表结构已与 SQL 对齐（另次核对）

---

## 附录：本轮执行说明

| 项 | 结果 |
|----|------|
| 是否修改业务代码 | **否** |
| 是否创建调查报告 | **是** |
| 报告路径 | `docs/cnpa-web-project-architecture-audit.md` |
| 编译检查 | 本轮未执行 `mvn package` / `npm run build`（只读分析） |

---

**本轮结束。请基于本报告修订正式开发提示词后再开工。**
