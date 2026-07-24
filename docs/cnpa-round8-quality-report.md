# CNPA 第八轮质量验收报告

> 验收日期：2026-07-24  
> 原则：以当前代码与实际运行结果为准；未验证项如实标注，不使用 Mock 冒充通过。

---

## 一、前七轮完成度（摘要）

| 轮次 | 总体 | 说明 |
|------|------|------|
| 1 账号/验证码/登录 | 代码完成，部分实测 | 登录/session/logout 实测通过；SMTP 实发未配置 |
| 2 个人中心/认证 | 部分完成 | 后端接口存在；Web UI 账户 Store 仍混用 Mock |
| 3 队长/球队/报名 | 代码完成，未全链路实测 | LOCAL_TEST 数据已备 |
| 4 Admin 审核/赛事 | 代码+菜单 SQL 完成 | `/getRouters` 未在若依库验证 |
| 5 比赛/比分/名单 | 代码完成 | 有 LOCAL_TEST 比赛样例 |
| 6 结算/公开数据 | 代码完成并构建通过 | 首页/公开接口实测部分通过 |
| 7 荣誉/名人堂/公告 | 代码完成 | 公告分页缺陷已修；公开展示部分通过 |

### 已完成并验证

- 密码登录、`/session`、退出后 Token 失效（Web :8881）
- 未配置 SMTP 时发送验证码返回业务失败（非假成功）
- 公开首页 `/basket/public/home`、合作伙伴列表
- 公告分页修复后仅返回已发布（`LOCAL_TEST_ANN_PUB`）
- `currentMemberCount` 与有效成员数一致（通过球队=3）
- 确认比赛单节和=最终分；主队球员得分和=主队最终分
- 两后端 `mvn package`、两前端 `npm build` 成功
- Admin/Web 本地启动成功（8080 / 8881）

### 已完成但未验证

- 注册验证码真实送达（QQ/163/Outlook）
- 验证码过期/并发/日限额端到端
- Admin 全菜单动态路由与按钮权限
- 结算重算幂等、并发入队/审核
- Web 全量 IDOR 专项压测

### 未完成 / 部分完成

- Web UI 账户域运行时 Mock 清理（DEF-R8-003）
- 完整九条业务链路 HTTP 端到端（受 SMTP + 账户 Mock 影响）

### 因缺表未实现

- 无独立「最终名次表 / 名人堂表 / 数据结算表」——已用现有表承载（第七/六轮结论不变）

### 因环境问题未验证

- QQ 邮箱 SMTP（`CNPA_MAIL_*` 全部未设置）
- 若依库菜单 SQL / `/getRouters`

---

## 二、缺陷

见 `docs/cnpa-local-defect-list.md`

- P0：0  
- P1：已修 SecureRandom、SMTP 空配置、`-parameters` 分页 500；未修账户 Mock  
- P2/P3：见缺陷清单  

---

## 三、邮箱与验证码

| # | 项 | 结果 |
|---|----|------|
| 11-22 | 配置/SMTP/实发/Outlook/163/错误/过期/重复/限流/并发/授权码入 Git | **SMTP 未配置 → 实发类全部未验证**；代码静态：SecureRandom、不回显码、失败不假成功、场景隔离、冷却与限额配置存在；根 `.gitignore` 已补；授权码未写入仓库文件 |

---

## 四、测试数据与文档路径

| 产物 | 路径 |
|------|------|
| 测试数据 | `sql/cnpa-local-test-data.sql` |
| 清理脚本 | `sql/cnpa-local-test-data-clean.sql` |
| 账号说明 | `docs/cnpa-local-test-accounts.md` |
| 状态流转 | `docs/cnpa-state-transition-test.md` |
| 权限测试 | `docs/cnpa-permission-test.md` |
| Redis 审计 | `docs/cnpa-redis-key-audit.md` |
| Mock 审计 | `docs/cnpa-mock-audit.md` |
| 缺陷清单 | `docs/cnpa-local-defect-list.md` |
| API 报告 | `docs/cnpa-local-api-test-report.md` |
| UI 报告 | `docs/cnpa-local-ui-test-report.md` |

本地密码（仅文档说明）：`LocalTest@123`  
登录字段：`account`（非 email）。

---

## 五、一致性 / 权限 / Redis / 安全（摘要）

| 项 | 结果 |
|----|------|
| currentMemberCount | 通过球队 3=3 |
| 报名阵容 | LOCAL_TEST 报名阵容 3 人已入库；业务规则静态存在 |
| 比赛名单 | LOCAL_TEST_M_CONF 有 roster |
| 单节=终分 / 球员分=球队分 | 100/90 与 100 校验通过 |
| 积分榜样例 | stage_standing 有 1 条 |
| 赛季重算幂等 / 并发 | 未实测 |
| Admin 权限 / getRouters | 未验证 |
| Web 越权 | 静态有归属校验；未全量实测 |
| 文件私有访问 | 未专项实测 |
| Redis Key | 见审计文档；前缀隔离 OK |
| SQL `${}` | Web mapper 未发现危险动态拼接 |
| XSS | 公告纯文本；web-ui 无 v-html |
| N+1 | 公开接口多用批量 IN；未做全量 profiling |
| 接口契约 | 公告 `/page` 与前端一致；登录字段为 `account` |
| Mock | 公开链路已脱；账户域仍有运行时 Mock |

---

## 六、启动 / 编译 / 代理

| 项目 | 结果 |
|------|------|
| basketball-web | 启动成功，端口 **8881** |
| basketball-admin | 启动成功，端口 **8080** |
| basketball-web-ui | `npm run build` 成功；dev 代理 → 8881 |
| basketball-admin-ui | `npm run build:prod` 成功；代理 → 8080 |
| MySQL | 连接成功，LOCAL_TEST 已加载 |
| Redis | 配置指向 192.168.31.47，服务可连（历史+登录 Token 实测） |
| Admin package | BUILD SUCCESS |
| Web package | BUILD SUCCESS |

---

## 七、演示条件结论

**当前不具备完整端到端演示条件。**

阻断项：

1. 未配置 QQ 邮箱 SMTP 环境变量 → 无法真实注册/验证码登录演示  
2. Web UI 账户域仍有运行时 Mock → 个人中心/球队活动不能宣称全真  
3. 若依菜单未在系统库验证 → Admin 动态菜单不能宣称验收通过  

部署前仍需：配置 SMTP、清理账户 Mock、执行并授权菜单 SQL、补齐并发/越权实测。

---

## 八、下一轮建议

1. 配置本地 `CNPA_MAIL_*` 完成验证码实发与注册链路  
2. 清除 `stores/account.ts` / `eventTeam.ts` 运行时 Mock，全部改真实 API  
3. 在若依库执行菜单 SQL 并做权限角色矩阵实测  
4. 补并发与幂等自动化脚本  
5. （后续轮次）生产 Nginx/HTTPS/域名——**不在第八轮范围**
