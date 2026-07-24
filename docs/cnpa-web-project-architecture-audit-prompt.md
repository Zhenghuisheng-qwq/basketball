# CNPA Web 项目架构调查提示词

> 用途：在正式开发官网登录、注册、个人中心业务接口之前，先让 Cursor **只盘点项目架构和已有能力**，输出调查报告。  
> 本轮 **不写业务代码、不修改现有实现**。  
> 调查报告建议输出为：`docs/cnpa-web-project-architecture-audit.md`

---

你现在先不要开发 CNPA 篮球官网的登录、注册、个人中心或其他业务接口。

本轮任务只有一个：

完整分析当前篮球项目脚手架，梳理现有后端和前端已经具备的公共能力，输出一份可以指导后续开发的项目架构调查报告。

特别注意：

- 不要根据常见 Spring Boot 项目经验猜测。
- 所有结论必须以当前项目实际代码为准。
- 每一项结论尽量给出具体文件路径、类名、方法名、注解名或配置项。
- 本轮不要新增业务接口。
- 不要重构项目。
- 不要修改数据库。
- 不要重新实现登录、Token、权限、Redis、文件上传等已有能力。
- 不要擅自增加 `/web`、`/api` 等统一前缀。
- 不要因为发现代码不规范就顺手修改。
- 本轮只分析、归纳、判断复用方式。
- 除调查报告外，不允许修改任何业务代码。

项目模块大致包括：

```text
basketball-admin
basketball-admin-ui
basketball-web
basketball-web-ui
```

当前已知情况：

- 58 张 CNPA 业务表已经完成建表。
- 58 张表对应的 Entity、Mapper、Service、Controller 等基础 CRUD 已经生成完成。
- 当前准备开始开发 `basketball-web` 与 `basketball-web-ui` 的真实业务。
- 官网前端目前存在 Mock 数据，后续需要逐步改为真实接口。
- 脚手架中可能已经存在：
  - 登录接口
  - Token 生成与解析
  - 自定义登录注解
  - 自定义 Token 注解
  - 当前登录用户获取工具
  - Redis 登录状态
  - 接口权限拦截
  - 文件上传
  - 验证码
  - 统一返回结构
  - 全局异常处理
  - 分页封装
  - MyBatis-Plus 公共配置
- 后续必须优先复用这些已有能力，不能重新写一套。

---

## 一、调查原则

请先全局搜索和阅读代码，再输出结论。

重点搜索但不限于以下关键词：

```text
login
logout
token
authorization
bearer
header
cookie
session
redis
captcha
verifyCode
code
currentUser
currentMember
loginUser
userContext
securityContext
principal
authentication
interceptor
filter
aspect
annotation
permission
anonymous
ignore
passToken
loginRequired
auth
upload
file
oss
cos
minio
mail
email
sms
R
Result
AjaxResult
ServiceException
GlobalException
Page
IPage
BaseController
SecurityUtils
TokenService
JwtUtils
RedisCache
```

同时搜索所有自定义注解：

```text
@interface
```

重点判断哪些注解与以下能力相关：

- 要求登录
- 无需登录
- 获取 Token
- 获取当前用户
- 权限校验
- 防重复提交
- 接口限流
- 数据权限
- 操作日志
- 参数校验
- 幂等控制

不能只看类名，需要继续查看：

- 注解定义
- AOP 实现
- 拦截器实现
- Filter 实现
- 注解使用位置
- 异常返回方式
- 实际是否生效

---

## 二、项目模块结构

梳理所有 Maven 或 Gradle 模块，并输出：

- 根项目结构
- 每个模块的职责
- 每个模块的启动类
- 每个模块的 `pom.xml` 依赖关系
- `basketball-admin` 和 `basketball-web` 是否依赖同一个公共模块
- Entity、Mapper、Service、Controller 分别放在哪个模块
- 58 张业务表的代码目前生成在哪个模块
- `basketball-web` 是否可以直接注入这些 Mapper 和 Service
- 是否存在循环依赖风险
- 是否已经有类似以下公共模块：`common` / `framework` / `system` / `security` / `core` / `business` / `model` / `service` / `mapper`

请输出模块依赖关系，例如：

```text
basketball-admin
  ├─ common
  ├─ framework
  └─ business

basketball-web
  ├─ common
  ├─ framework
  └─ business
```

必须以实际项目为准，不要照搬此示例。

需要明确回答：

- Web 后端应该复用哪个模块的 Entity？
- Web 后端应该复用哪个模块的 Mapper？
- Web 后端应该复用哪个模块的 Service？
- 是否需要抽取公共业务模块？
- 当前是否已经完成抽取，无需再次处理？
- Web 后端是否会错误依赖 Admin Controller？

---

## 三、应用启动与配置

分别检查 `basketball-admin` 和 `basketball-web`：

- Spring Boot 启动类路径
- `@SpringBootApplication` 扫描范围
- `@MapperScan` 扫描范围
- 配置文件位置
- 当前激活环境
- 服务端口
- `server.servlet.context-path`
- 是否已经配置统一接口前缀
- 是否有网关、Nginx 或反向代理前缀
- 是否配置跨域
- 是否配置静态资源映射
- 是否配置文件上传大小
- 是否配置 Redis
- 是否配置数据库
- 是否配置邮件或短信
- 是否存在环境变量占位符
- 是否存在开发、测试、生产多环境配置

重点回答：

- Controller 现在实际访问路径是什么？
- 是否存在 `/api`、`/web`、`/admin` 或其他前缀？
- 前缀是写在 Controller 上，还是配置在 `context-path` 中？
- 后续 Web 接口是否需要新增前缀？
- 如果不需要，说明应该沿用什么现有规范。

不要自行决定增加 `/web`。

---

## 四、登录业务现状

完整梳理当前已有登录流程。

需要查找：

- 登录 Controller
- 登录 Service
- 登录请求对象
- 登录响应对象
- 用户查询逻辑
- 密码校验逻辑
- 验证码逻辑
- Token 创建逻辑
- Token 返回逻辑
- 登录日志逻辑
- 登录失败处理
- 退出登录逻辑
- 当前用户查询接口
- 刷新 Token 逻辑
- 多端登录处理

输出完整调用链，例如：

```text
LoginController.login
→ LoginService.login
→ 用户查询
→ 密码校验
→ TokenService.createToken
→ Redis 写入
→ 返回 Token
```

必须替换为实际类名和方法名。

需要明确回答：

- 当前是否已经存在真实登录接口？
- 登录账号使用什么表？
- 当前登录针对后台管理员还是官网会员？
- 官网会员是否可以复用当前登录实现？
- 如果不能直接复用，哪些组件可以复用？
- 密码使用什么加密方式？
- 是否使用 BCrypt？
- 是否存在图形验证码？
- 是否存在短信或邮箱验证码？
- 是否存在登录失败次数限制？
- 是否存在账号锁定？
- 是否存在登录日志？
- 是否存在退出登录？
- 是否存在修改密码？
- 是否存在重置密码？
- 是否存在注册能力？

必须区分：

- 后台管理员登录
- 官网会员登录
- 玩家中心登录

不能把若依后台登录直接当成官网会员登录。

---

## 五、Token 与会话机制

重点调查当前 Token 的完整机制。

输出：

- Token 类型：JWT / UUID / 随机字符串 / Sa-Token / Session / 自定义方案
- Token 携带位置：Header / Cookie / 请求参数
- Header 名称，例如：`Authorization` / `token` / `access-token` / `X-Token`
- 是否有前缀：`Bearer`
- Token 生成类
- Token 解析类
- Token 校验位置
- Token 过期时间
- Redis Key 格式
- Token 与用户信息在 Redis 中的存储结构
- 是否支持续期
- 是否每次请求自动刷新
- 是否支持多设备登录
- 是否支持同账号互踢
- 退出登录时如何失效
- Token 失效时返回什么错误码
- 前端收到 401 后如何处理

需要绘制实际请求链：

```text
浏览器请求
→ Axios 请求拦截器
→ 携带哪个 Header
→ 后端哪个 Filter/Interceptor
→ Token 如何解析
→ Redis 如何查询
→ 当前用户如何写入上下文
→ Controller 如何获取用户
```

必须给出实际文件路径和类名。

---

## 六、自定义登录注解与权限注解

全局扫描所有自定义注解，并单独列出与登录和 Token 相关的注解。

每个注解输出以下信息：

| 注解 | 定义文件 | 处理器 | 作用 | 使用示例 | 后续能否复用 |
|------|----------|--------|------|----------|--------------|

例如可能存在：

- `@Login`
- `@LoginRequired`
- `@NoLogin`
- `@PassToken`
- `@Auth`
- `@CurrentUser`
- `@Token`
- `@RequiresPermissions`
- `@Anonymous`

但不要假设名称，必须以项目实际代码为准。

对于每个注解，需要继续检查：

- 注解作用于类还是方法
- 是否通过 AOP 实现
- 是否通过 HandlerInterceptor 实现
- 是否通过 Filter 实现
- 未携带 Token 时如何处理
- Token 无效时如何处理
- 是否能直接获取当前用户 ID
- 是否只适用于后台管理员
- 是否支持官网会员
- 是否需要扩展用户类型
- 是否存在重复的登录校验机制

特别关注：

- 登录自定义注解
- 携带 Token 的自定义注解

必须找出真实注解，不允许重新设计一套替代它。

---

## 七、当前用户上下文

检查项目获取当前登录用户的真实方式。

可能包括但不限于：

- `SecurityUtils`
- `LoginUser`
- `TokenService`
- `UserContext`
- `ThreadLocal`
- `SecurityContextHolder`
- `RequestContextHolder`
- 自定义注解参数解析器

输出：

- 获取当前用户的方法
- 获取当前用户 ID 的方法
- 获取用户名的方法
- 获取角色和权限的方法
- 用户信息来源
- 是否依赖 Redis
- 是否使用 ThreadLocal
- 请求结束后是否清理 ThreadLocal
- 当前用户模型是后台管理员还是普通会员
- 后续官网会员应该：直接复用 / 扩展 / 新增官网会员适配器 / 不能复用

不要直接创建 `CurrentMemberContext`，先判断现有能力是否已经满足。

需要给出推荐调用方式，例如实际项目中应使用：

```java
Long userId = XXX.getUserId();
```

必须使用真实类名，不要使用虚构示例。

---

## 八、Spring Security、Filter 与拦截器

检查项目是否使用：

- Spring Security
- Apache Shiro
- Sa-Token
- 自定义 Filter
- 自定义 HandlerInterceptor
- AOP 登录校验

如果使用 Spring Security，重点检查：

- `SecurityFilterChain`
- `WebSecurityConfigurerAdapter`
- `AuthenticationProvider`
- `UserDetailsService`
- `PasswordEncoder`
- JWT Filter
- `AuthenticationEntryPoint`
- `AccessDeniedHandler`
- 白名单路径
- 静态资源路径
- OPTIONS 请求
- CORS 配置
- CSRF 配置
- Session 策略

输出白名单路径的实际来源：

- 配置文件
- 注解扫描
- Java 配置
- 数据库菜单权限
- 多种机制组合

需要判断官网公开接口后续应该如何加入白名单。

不要本轮直接修改白名单。

---

## 九、统一返回结构

检查项目已有统一响应对象，例如：

- `R`
- `AjaxResult`
- `Result`
- `ResponseResult`
- `ApiResult`

当前示例代码使用：

```text
com.zhs.util.R
```

请打开并分析 `R`：

- 成功响应结构
- 失败响应结构
- 状态码字段
- 消息字段
- 数据字段
- 泛型使用方式
- 是否支持分页
- 是否已有 `ok` / `fail` / `error` 方法
- 是否和前端响应拦截器一致
- 是否存在另一套重复返回对象

输出后续 Web 业务应统一使用哪一套返回结构。

不要新建第二套响应对象。

---

## 十、全局异常处理

检查：

- `ServiceException`
- 参数校验异常
- 登录异常
- Token 异常
- 权限异常
- 数据库异常
- 文件上传异常
- 全局异常处理器

输出：

- 全局异常处理类路径
- 每类异常的处理方法
- 返回状态码
- 前端如何读取错误信息
- `ServiceException` 的构造方式
- 是否会将堆栈或 SQL 返回前端
- 是否存在多个全局异常处理器
- 新业务是否直接复用现有异常体系

不要重新创建 `BusinessException` 或另一套异常体系，除非报告明确证明当前体系无法使用。

---

## 十一、参数校验与防重复提交

调查是否已有：

- `@Valid`
- `@Validated`
- Jakarta Validation
- Hibernate Validator
- 自定义参数校验
- 防重复提交注解
- Redis 幂等
- 接口限流
- 请求签名
- XSS 过滤

输出：

- 当前请求对象校验方式
- 参数错误返回格式
- 是否存在 `@RepeatSubmit` 或类似注解
- 防重复提交的实现原理
- 哪些接口已经使用
- 后续注册、认证提交、赛事报名能否复用
- 是否有接口限流能力
- 是否有 Redis 锁工具

---

## 十二、MyBatis-Plus 与数据库公共能力

检查：

- MyBatis-Plus 版本
- 分页插件
- 逻辑删除配置
- `is_delete` 是否通过 `@TableLogic` 处理
- 主键生成策略
- `BIGINT` 是否映射为 `Long`
- `create_time`、`update_time` 是否自动填充
- 是否存在 `MetaObjectHandler`
- 字段命名策略
- Mapper XML 路径
- 是否启用驼峰转换
- 是否存在乐观锁插件
- 是否存在多租户插件
- 是否存在数据权限插件
- 是否存在 SQL 日志
- 是否存在分页统一对象
- 是否已经有 `BaseEntity`

特别检查 58 张业务表：

- 是否全部包含 `id`
- 是否全部包含 `create_time`
- 是否全部包含 `update_time`
- 是否全部包含 `is_delete`
- Java 实体是否一致
- 是否统一继承公共实体
- 是否存在部分主键错误使用 `String`
- 是否存在时间字段类型不一致
- 是否存在逻辑删除未生效

本轮只报告问题，不修改。

---

## 十三、已有 CRUD 代码结构

统计 58 张业务表当前代码情况：

- Entity 数量
- Mapper 数量
- Mapper XML 数量
- Service 数量
- ServiceImpl 数量
- Controller 数量
- DTO 数量
- 代码所在模块
- 包名
- URL 命名规律

抽样检查至少以下类别：

- 账号表
- 安全日志表
- 球员认证表
- 球队表
- 赛事表
- 报名表
- 比赛表
- 消息表
- 公告表

输出基础 CRUD 的统一模式，例如：

- 分页
- 按 ID 查询
- 新增或修改
- 单个逻辑删除
- 批量逻辑删除

并明确：

- 哪些表可以保留完整 CRUD
- 哪些日志表不应该向 Web 用户开放新增、修改、删除
- 哪些审核表只能由业务 Service 更新
- 哪些状态字段不能通过通用 `addOrUpdate` 修改
- Web 后端如何复用已有 Service，而不是重新生成 CRUD

---

## 十四、验证码能力

检查项目是否已有：

- 图片验证码
- 邮箱验证码
- 短信验证码
- Redis 验证码
- 验证码记录表
- 验证码频率限制
- 验证码过期机制

需要回答：

- 当前验证码用途是什么？
- 只用于后台登录还是可以复用到会员注册？
- 是否已经有邮件发送工具？
- 是否已经有 QQ 邮箱 SMTP 配置？
- 是否已经有短信供应商接入？
- 验证码是否明文存储？
- 验证码是否一次性消费？
- 是否限制错误次数？
- 是否限制发送频率？
- 是否有现成接口可以直接复用？

若不存在某项能力，只写“当前没有发现”，不要本轮实现。

---

## 十五、文件上传能力

检查当前文件上传完整链路：

- 上传 Controller
- 上传 Service
- 文件工具类
- 本地存储
- OSS
- COS
- MinIO
- 静态资源访问
- 文件删除
- 文件大小限制
- 文件类型限制
- 文件表记录

输出：

- 当前上传接口地址
- 请求参数名称
- 返回结构
- 返回的是文件 URL、相对路径还是文件 ID
- 文件存储位置
- 开发和生产环境是否不同
- 是否支持私有文件
- 是否有文件归属校验
- 是否支持删除
- 是否支持头像上传
- 是否适合存储球员认证材料
- 前端已有上传组件如何调用
- 后续是否需要扩展，而不是重新实现

---

## 十六、邮件与短信能力

检查：

- `JavaMailSender`
- SMTP 配置
- QQ 邮箱授权码
- 邮件模板
- 短信 SDK
- 短信配置
- 邮件或短信发送记录
- 异步发送
- 线程池
- MQ

输出：

- 当前实际具备哪些发送渠道
- 配置文件位置
- 密钥是否环境变量化
- 是否存在测试代码
- 是否有统一发送服务
- 后续注册验证码可以直接复用什么
- 当前缺少什么

不要因为未找到实现就新增代码。

---

## 十七、Redis 公共工具

检查：

- `RedisTemplate`
- `StringRedisTemplate`
- `RedisCache`
- `CacheUtils`
- 分布式锁
- Token 缓存
- 验证码缓存
- 限流缓存
- 防重复提交缓存

输出：

- Redis 工具类
- 常用方法
- Key 前缀规范
- 默认过期时间
- 序列化方式
- Token 使用的 Redis 数据结构
- 是否支持 `setIfAbsent`
- 是否支持自增计数
- 是否支持锁
- 后续验证码频控和登录失败计数如何复用

---

## 十八、后台权限与官网会员权限的边界

重点分析当前认证体系是否只针对若依后台管理员。

需要明确区分：

- 后台管理员
- 官网会员
- 认证球员
- 队长
- 游客

回答：

- 后台用户表是哪张表？
- 后台登录使用哪个实体？
- 官网会员表是哪张表？
- 两类用户是否共享 ID 空间？
- 是否共享 `TokenService`？
- 是否共享 Redis Key？
- 是否共享 `LoginUser` 对象？
- 是否有用户类型字段？
- 当前权限注解是否依赖后台菜单权限？
- 官网普通会员是否会因为没有后台角色而被拒绝？
- 官网会员登录能否在现有认证体系中增加独立用户类型？
- 是否应该复用 Token 底层能力，但使用独立会员登录模型？
- 是否有 Token 冲突风险？
- 是否有 URL 白名单冲突风险？

本轮只分析，不调整认证体系。

---

## 十九、前端工程结构

检查 `basketball-web-ui`：

- Vue 版本
- TypeScript 或 JavaScript
- Vite 配置
- API 请求封装
- Axios 实例
- Base URL
- 开发代理配置
- 请求拦截器
- 响应拦截器
- Token 保存位置
- Token Header 名称
- Pinia Store
- 用户 Store
- 路由守卫
- 登录页
- 注册页
- 个人中心
- 文件上传组件
- 全局消息组件
- 错误处理方式

输出实际调用链：

```text
登录页面
→ 调用哪个 API 文件
→ Axios 实例
→ 请求 Header
→ 后端接口
→ Token 保存位置
→ 路由守卫
→ 刷新页面恢复登录状态
```

如果目前登录和个人中心使用 Mock，需要明确指出：

- Mock 文件路径
- Mock 数据来源
- 哪些组件直接引用
- 是否使用 `setTimeout`
- 是否使用本地固定用户
- 是否有接口失败回退 Mock
- 后续替换 Mock 的最小修改范围

本轮不要删除 Mock。

---

## 二十、前端路由与接口前缀

重点检查：

- 前端页面路由是否使用：`/account` / `/member` / `/user` / `/personal` / 其他路径
- Axios Base URL 是什么
- Vite Proxy 的前缀是什么
- 后端 Controller 是否已经有统一前缀
- Nginx 是否存在路径转发规则
- 当前 Admin UI 和 Web UI 是否使用不同接口前缀
- 是否已经通过端口区分两个后端
- 后续是否真的需要增加 `/web` 前缀

必须明确回答：

> 后续官网接口应该使用什么路径规范？

答案必须来自现有架构。

不要为了“看起来规范”擅自增加 `/web`。

---

## 二十一、代码规范和现有约定

根据当前代码总结实际约定：

- Controller 命名
- URL 命名
- Service 接口命名
- ServiceImpl 命名
- Mapper 命名
- Entity 命名
- Req 命名
- Resp 命名
- DTO 命名
- VO 命名
- 枚举命名
- 常量命名
- 分页参数命名
- 时间字段类型
- 状态字段类型
- 注入方式
- Lombok 使用
- Swagger 注解版本

特别检查当前示例使用：

```text
io.swagger.annotations.Api
io.swagger.annotations.ApiOperation
```

确认项目究竟使用：

- Swagger 2
- Springfox
- Knife4j
- OpenAPI 3
- Springdoc

后续必须沿用现有版本，不要混写。

---

## 二十二、现有能力复用矩阵

最终必须输出一张复用矩阵：

| 能力 | 当前是否存在 | 实现位置 | 是否可直接复用 | 是否需要扩展 | 后续建议 |
|------|--------------|----------|----------------|--------------|----------|
| 登录 | | | | | |
| Token 生成 | | | | | |
| Token 校验 | | | | | |
| 自定义登录注解 | | | | | |
| 当前用户获取 | | | | | |
| Redis | | | | | |
| 图形验证码 | | | | | |
| 邮箱验证码 | | | | | |
| 短信验证码 | | | | | |
| 文件上传 | | | | | |
| 密码加密 | | | | | |
| 统一响应 | | | | | |
| 全局异常 | | | | | |
| 分页 | | | | | |
| 逻辑删除 | | | | | |
| 自动填充时间 | | | | | |
| 防重复提交 | | | | | |
| 接口限流 | | | | | |
| 权限校验 | | | | | |
| 操作日志 | | | | | |
| 登录日志 | | | | | |
| 前端请求封装 | | | | | |
| 前端登录 Store | | | | | |
| 前端路由守卫 | | | | | |

“是否存在”不能只写是或否，必须附实际证据路径。

---

## 二十三、上一版设想冲突检查

根据实际代码，检查下面这些预设是否正确：

- 是否应该增加 `/web` 接口前缀
- 是否需要创建新的 JWT 工具
- 是否需要创建新的 `CurrentMemberContext`
- 是否需要创建新的登录注解
- 是否需要创建新的 Token 注解
- 是否需要创建新的 Redis 工具
- 是否需要创建新的文件上传服务
- 是否需要创建新的统一返回对象
- 是否需要创建新的全局异常处理
- 是否需要创建新的分页结构
- 是否需要创建新的登录 Store
- 是否需要创建新的 Axios 实例
- 是否应该让 `basketball-web` 依赖 Admin
- 是否可以复用后台管理员登录
- 是否应该将官网会员纳入当前认证体系

每一项输出：

- 正确
- 部分正确
- 错误
- 当前无法确认

并说明原因。

---

## 二十四、需要新增与不需要新增的清单

根据调查结果，输出两份清单。

**第一份：现有能力已经满足，不需要重新开发**

例如：

- Token 解析
- 登录注解
- Redis 工具
- 文件上传

但必须以实际结果为准。

**第二份：当前项目确实缺少，后续需要新增或扩展**

需要区分：

- 完全新增
- 在现有能力上扩展
- 只增加业务 Service
- 只增加官网会员适配
- 只调整配置

禁止将“代码写法不符合个人习惯”判定为必须重写。

---

## 二十五、推荐的后续开发边界

在完成调查后，根据实际项目结构，提出下一轮建议，但不要编码。

建议内容包括：

- 官网会员登录应该如何接入现有认证体系
- 官网会员 Token 是否复用当前 TokenService
- 当前登录用户如何获取
- Controller 应使用什么路径前缀
- Web 业务 Controller 应放在哪个包
- 58 张基础 Service 如何复用
- 哪些通用 CRUD 不能直接给 Web 前端调用
- 哪些聚合 Service 需要新增
- 前端如何逐步移除 Mock
- 第一批最适合打通的真实业务链路

建议必须贴合当前代码，不要输出通用模板方案。

---

## 二十六、报告输出格式

本轮最终输出一份 Markdown 调查报告，建议文件名：

```text
docs/cnpa-web-project-architecture-audit.md
```

如果项目中没有 `docs` 目录，可以只在对话中输出，不要为了报告大范围调整目录。

报告必须包含以下章节：

1. 调查结论摘要
2. 项目模块结构
3. 后端启动与配置
4. 登录流程
5. Token 与会话机制
6. 自定义认证注解
7. 当前用户上下文
8. Security、Filter 与拦截器
9. 统一响应与异常处理
10. 参数校验与防重复提交
11. MyBatis-Plus 与数据库能力
12. 58 张表 CRUD 现状
13. 验证码能力
14. 文件上传能力
15. 邮件与短信能力
16. Redis 公共能力
17. 后台管理员与官网会员边界
18. basketball-web-ui 请求架构
19. 前端登录与路由守卫
20. Mock 数据分布
21. 接口路径和前缀结论
22. 现有能力复用矩阵
23. 上一版预设冲突检查
24. 不需要重复开发的能力
25. 后续确实需要新增的能力
26. 下一轮开发建议
27. 已知问题与风险
28. 证据文件清单

---

## 二十七、证据要求

报告中的重要结论必须给出证据。

正确示例：

> Token 请求头名称为 `Authorization`。
>
> 证据：
> - `basketball-web-ui/src/utils/request.ts`
> - `basketball-web/src/main/java/com/zhs/filter/JwtAuthenticationTokenFilter.java`
> - `basketball-web/src/main/resources/application.yml`

错误示例：

> 项目应该使用 Authorization。

对于不存在的能力，也要说明搜索范围，例如：

> 未发现邮箱验证码发送实现。
>
> 已检查：
> - 所有 `JavaMailSender` 引用
> - 所有 `spring.mail` 配置
> - 所有名称包含 Email、Mail、VerifyCode 的类

---

## 二十八、本轮严格禁止

本轮禁止：

- 开发登录接口
- 开发注册接口
- 开发验证码接口
- 开发个人中心接口
- 开发球员认证接口
- 删除 Mock 数据
- 修改前端页面
- 新增 `/web` 前缀
- 新建 `TokenService`
- 新建登录注解
- 新建用户上下文
- 重写 Redis 工具
- 重写文件上传
- 重写统一返回对象
- 重写全局异常处理
- 修改 Security 配置
- 修改白名单
- 调整数据库表
- 修改 58 张表 CRUD
- 顺手修复不相关问题
- 根据经验猜测现有实现

本轮只允许：

- 搜索代码
- 阅读代码
- 分析配置
- 执行不会修改代码的查询命令
- 必要的编译检查
- 输出架构调查报告

---

## 二十九、最终回复要求

完成后不要只回复“分析完成”。

最终回复必须说明：

- 是否修改了业务代码
- 是否创建了调查报告文件
- 报告文件路径
- 当前实际登录方式
- 当前实际 Token 方式
- 已发现的登录和 Token 自定义注解
- 当前用户获取方式
- 当前接口实际前缀
- 当前文件上传方式
- 当前验证码能力
- Web UI Token 携带方式
- 已经存在、无需重写的主要能力
- 后续确实缺少的主要能力
- 下一轮最合理的开发范围
- 编译或检查结果

本轮完成后停止继续开发业务代码，等待根据调查报告重新制定下一版实施提示词。

---

## 使用说明

1. 将本文完整发给 Cursor，执行架构调查。
2. Cursor 完成后应产出：`docs/cnpa-web-project-architecture-audit.md`
3. 拿到报告后，再根据真实复用边界制定正式开发提示词，明确：
   - 哪些登录能力直接复用
   - 自定义注解怎么使用
   - Token 从哪里获取
   - Controller 应该放在哪个模块
   - 接口到底需不需要统一前缀
   - 官网会员和后台管理员是否共用认证底层
   - 哪些地方只补业务 Service，不重新造公共组件
