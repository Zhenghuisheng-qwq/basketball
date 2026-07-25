# CNPA 上线准备清单

> 第六批输出 — 不含正式部署执行

## 代码

- [ ] 四项目分支合并与 tag
- [ ] `basketball-admin` / `basketball-web` Maven 编译通过
- [ ] `basketball-admin-ui` / `basketball-web-ui` 生产构建通过
- [ ] 敏感配置：COS Secret、SMTP、JWT、加密密钥仅环境变量

## 数据库

- [ ] 初始化：`sql/basketball.sql`
- [ ] 增量：`docs/sql/cnpa-batch5-migration.sql` → `cnpa-batch6-migration.sql`
- [ ] 回滚：`cnpa-batch6-rollback.sql` 演练
- [ ] 菜单 2201/2207/2240–2248 已导入
- [ ] 字符集 utf8mb4，时区 Asia/Shanghai

## Redis

- [ ] 确认使用场景（验证码/锁/缓存）
- [ ] 禁止 FLUSHALL 清缓存策略文档化

## COS

- [ ] 公私桶策略、上传大小/MIME 校验
- [ ] 审核材料私有，公开附件只读 URL

## 邮箱

- [ ] SMTP 发件账号与频率限制
- [ ] 验证码模板与防刷

## 安全

- [ ] 越权回归（Admin 赛事 scope + 按钮权限）
- [ ] 日志脱敏（审计不写密码/Token）
- [ ] 文件上传校验

## 部署前待办（后续阶段）

- [ ] 服务器 / 域名 / 备案
- [ ] Nginx + HTTPS
- [ ] MySQL / Redis 生产实例
- [ ] Java 服务 systemd / 容器
- [ ] 前端静态资源 CDN
- [ ] 备份与监控

## 第六批遗留必须在部署前关闭

- [ ] P1：荣誉自动生成
- [ ] P1：公告定时发布
- [ ] P1：关键业务审计全覆盖
- [ ] P1：Web-ui Mock 清理
- [ ] P1：Java mvn 编译与冒烟

## 启动命令（本地参考）

```bash
# basketball-admin / basketball-web
mvn spring-boot:run

# basketball-admin-ui
npm run dev

# basketball-web-ui
npm run dev
```

## 结论

**具备进入部署准备阶段的基础**，但正式切生产前必须完成 Java 编译验证与 P1 遗留项。
