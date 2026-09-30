# 安全策略（Security Policy）

## 支持的版本

| 版本 | 支持状态 |
|------|----------|
| 0.4.x | ✅ 接收安全修复 |
| < 0.4.0 | ❌ 请升级 |

## 报告漏洞

**请勿通过公开 Issue 报告安全漏洞。**

请优先使用 GitHub 的「Security → Report a vulnerability」（私密安全通告）提交报告；若不可用，可通过仓库所有者（[luminous-ChenXi](https://github.com/luminous-ChenXi)）主页提供的渠道私下联系。

提交时请尽量包含：影响范围、复现步骤/POC、影响的版本或 commit、你的修复建议（如有）。我们会在确认后尽快响应并致谢报告者。

## 安全特性概览

- **密钥零硬编码**：`JWT_SECRET`（≥32 字符，启动强校验）、`DB_USERNAME`/`DB_PASSWORD` 全部环境变量注入，仓库内无任何密钥与默认值
- **安装向导防护**：安装锁 + 数据库标记 + API 状态检查 + 前端拦截；重装需管理员验证 + 显式确认 + 限流锁定；数据库故障时 fail-closed
- **认证**：JWT（HS384+，24h 访问令牌 + 7 天刷新令牌）、BCrypt 密码哈希、可选手动 TOTP 两步验证（RFC 6238，含重放防护与一次性还原码）、登录失败锁定、登录审计日志（默认保留 180 天）
- **限流**：注册 / 邮箱验证码 / 评论 / 互动（浏览点赞）/ 重装验证均有速率限制；`X-Forwarded-For` 仅在 `app.security.trust-proxy=true` 时信任
- **内容安全**：用户内容 HTML 输出经 DOMPurify 白名单消毒；头像 URL 协议白名单
- **辰汐通行证 SSO**（可选）：OAuth 2.1 / OIDC 授权码 + PKCE 公共客户端，仓库零密钥；影子账号密码为随机 UUID 哈希（不可本地登录）

## 部署安全要求（站长必读）

1. **必须**设置 `JWT_SECRET`（≥32 字符强随机串）与专用最小权限 MySQL 账号，勿用 root
2. 使用 HTTPS（nginx/CDN 终结 TLS）；反代后面将 `app.security.trust-proxy` 设为 `true` 以获取真实客户端 IP（直连部署保持 `false`）
3. 环境变量写入守护进程配置（systemd 等），不要写进仓库
4. 保持后端 8080 与前端 node 进程不对公网直接暴露，统一走 nginx 入口
5. JWT 存储于 localStorage，请保持严格的 CSP 策略以压缩 XSS 窃取面

## 已知边界（透明说明）

- SMTP 授权码明文存储于数据库 `site_settings`（不回传浏览器；留空表示沿用）
- TOTP 重放防护为单机内存态（单节点部署足够；多实例部署需共享存储）
- 评论区前端组件仍在开发中（后端 API 已就绪并有相应防护）
