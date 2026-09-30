# 辰汐生态集成说明（通行证登录 / 正版授权 / 统计上报）

LuomiBlog 与姊妹项目 AstrNest 共用同一套"辰汐生态集成"统一规范：配置命名空间 `chenxi.*`、端点、行为逐条对齐。所有开关**默认关闭**，不配置任何内容时本站行为与未集成时完全一致。

本文档覆盖三块能力：

| 模块 | 配置前缀 | 状态 |
| --- | --- | --- |
| 辰汐通行证登录（OIDC 授权码 + PKCE） | `chenxi.passport.*` | **已完成**，默认关闭 |
| 正版授权校验（LicenseClient） | `chenxi.license.*` | N2 占位骨架，默认关闭 |
| 统计上报（StatsReporter） | `chenxi.stats.*` | N2 占位骨架，默认关闭 |

---

## 一、配置总表（application.yml → `chenxi.*`）

| 配置键 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `chenxi.passport.enabled` | `CHENXI_ENABLED` | `false` | 通行证登录总开关（默认关闭，本地账号登录不受影响） |
| `chenxi.passport.issuer` | `CHENXI_ISSUER` | 空 | 主站通行证 issuer（OIDC 签发方地址） |
| `chenxi.passport.client-id` | `CHENXI_CLIENT_ID` | 空 | 在通行证注册的客户端 ID（公共客户端，无密钥） |
| `chenxi.passport.redirect-uri` | `CHENXI_REDIRECT_URI` | 空 | 授权回调地址，必须与通行证侧注册完全一致 |
| `chenxi.passport.scopes` | `CHENXI_SCOPES` | `openid,profile` | 授权范围（逗号分隔，后端统一规范化为空格分隔再下发） |
| `chenxi.passport.access-token-days` | `CHENXI_ACCESS_TOKEN_DAYS` | `30` | 本站会话令牌不活动过期天数（滑动刷新，见下文） |
| `chenxi.license.enabled` | `CHENXI_LICENSE_ENABLED` | `false` | 正版授权校验总开关（N2 占位） |
| `chenxi.license.verify-url` | `CHENXI_LICENSE_VERIFY_URL` | 空 | 预留：授权校验端点 |
| `chenxi.license.cache-days` | `CHENXI_LICENSE_CACHE_DAYS` | `7` | 校验结果本地缓存天数 |
| `chenxi.license.offline-grace-days` | `CHENXI_LICENSE_OFFLINE_GRACE_DAYS` | `3` | 离线宽限天数（超期仅横幅提示，绝不锁数据） |
| `chenxi.stats.enabled` | `CHENXI_STATS_ENABLED` | `false` | 统计上报总开关（N2 占位） |
| `chenxi.stats.report-url` | `CHENXI_STATS_REPORT_URL` | 空 | 预留：统计上报端点 |

启用通行证登录示例（生产环境只需设置环境变量）：

```bash
CHENXI_ENABLED=true
CHENXI_CLIENT_ID=luomiblog-web
CHENXI_ISSUER=https://passport.luminouschenxi.com
CHENXI_REDIRECT_URI=https://your-site.com/auth/chenxi
```

---

## 二、通行证登录（已实现）

### 为什么是标准 OAuth 2.1 / OIDC + PKCE

本站**没有**对接任何通行证私有接口，而是完整实现了标准 OIDC **授权码模式 + PKCE（S256）**：

- 通行证侧要求 PKCE S256 强制启用（`plain` 会被拒绝），授权码即使被拦截也无法单独换 token；
- LuomiBlog 是**公共客户端（public client）**：浏览器中无法安全保存密钥，因此整个功能**没有任何 client_secret**，全部依赖 PKCE 保护授权码交换；
- 好处是双向的：本站既能接辰汐通行证，也能无缝切换/并存于任何标准 OIDC 提供商（Keycloak、Auth0、Logto 等）。

### 协议交互流程

```
浏览器                          LuomiBlog 后端                    辰汐通行证
  │  1. GET /api/auth/chenxi/config                                        │
  │ ────────────────────────>│  （返回 issuer/client_id 等公开门牌信息）      │
  │  2. 生成 code_verifier/state/nonce（sessionStorage，10 分钟有效）         │
  │  3. 跳转授权页                                                            │
  │ ───────────────────────────────────────────────────────────────────────> │
  │  4. 用户登录并授权，重定向回 /auth/chenxi?code=...&state=...               │
  │ <─────────────────────────────────────────────────────────────────────── │
  │  5. 校验 state，POST /api/auth/chenxi/exchange {code, codeVerifier}       │
  │ ────────────────────────>│  6. POST {issuer}/oauth2/token                │
  │                          │ ────────────────────────────────────────────> │
  │                          │  7. GET {issuer}/userinfo (Bearer token)      │
  │                          │ ────────────────────────────────────────────> │
  │                          │  8. 按 chenxi_sub 匹配/创建影子账号，签发本站 JWT │
  │  9. 拿到与 /api/auth/login 完全一致结构的响应，写本地登录态，跳回首页          │
  │ <────────────────────────│                                               │
```

### 后端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/chenxi/config` | 返回登录配置（`enabled/issuer/clientId/redirectUri/scopes`），无论是否启用都可访问。公共客户端没有密钥，这些都是可公开的"门牌"信息 |
| POST | `/api/auth/chenxi/exchange` | 请求体 `{code, codeVerifier}`；后端向通行证换 token、拉取 userinfo、匹配/创建影子账号后，返回与 `/api/auth/login` **完全一致**的 `AuthResponse` 结构 |

### 令牌设计：30 天不活动过期（滑动刷新）vs 授权按年（解耦）

两套时间体系**有意解耦**，互不感知：

| 体系 | 有效期 | 刷新方式 | 说明 |
| --- | --- | --- | --- |
| 本站会话 JWT（`chenxi.passport.access-token-days`，默认 30 天） | 不活动过期 | **滑动刷新**：`JwtAuthenticationFilter` 发现令牌活跃使用且剩余寿命不足一半时，通过 `X-New-Token` 响应头下发新令牌，前端 `api.ts` 静默替换本地登录态 | 只要用户持续活跃就永不过期；停止活动 30 天后自然失效 |
| 通行证侧授权关系（按年计） | 按年 | 由通行证侧管理 | 本站 JWT 过期 ≠ 通行证授权过期；重新走一次 OIDC 登录即可同步 |

通行证的 access_token 是不透明的（`lcx_` 前缀），本站用完即弃，不缓存、不刷新——会话始终由**本站自己的 JWT** 承载。`X-New-Token` 响应头已在 CORS `exposed-headers` 中放行，跨域场景同样生效。

### 数据库变更

`users` 表新增一列：

```sql
chenxi_sub VARCHAR(64) NULL，UNIQUE KEY uk_users_chenxi_sub (chenxi_sub)
```

- **全新安装**：无需任何操作，安装向导执行的 `db/schema.sql` 已包含该字段；
- **存量数据库升级**：手动执行一次 `luomiblog-backend/src/main/resources/db/migration/V2__add_chenxi_sub.sql`（MySQL 8.0 唯一索引允许多个 NULL，不影响普通账号）。

### 安全说明

- **影子账号机制**：通行证用户首次登录时自动创建本站账号（角色 `member`），`users.chenxi_sub` 作为唯一锚点；用户名与本站已有用户冲突时自动追加 `_cx` 后缀；通行证邮箱与本站已有邮箱冲突时，邮箱列留空（该列有唯一约束）；密码为随机 UUID 的 BCrypt 哈希，**设计上即无法用密码登录**。
- **两步验证（2FA）的范围**：本站的 2FA 强制开关是站点设置 `login.totp_required`，该开关仅约束本站密码登录；辰汐通行证 SSO 登录的安全性由通行证侧（passport）自身的 2FA/会话策略保障，本站影子账号默认 member 角色且密码不可登录。
- **仓库中没有任何密钥**：公共客户端只有公开的 `client_id`，不存在需要保密的配置。
- **吊销/封禁**：本站侧将影子账号 `status` 置为非 `active`（如 `disabled`）即可拒绝其登录；通行证侧吊销授权则直接无法完成 OAuth 流程。
- **防 CSRF / 重放**：`state` 一次性校验 + 10 分钟 TTL；`nonce` 按协议传入授权请求。

### 在通行证侧注册客户端

在辰汐闭源主站的 `PassportClientSeeder` 中注册一个公共客户端：

- **client_id**：建议使用 `luomiblog-web`
- **client_secret**：留空（公共客户端）
- **redirect_uri**（必须逐字符一致）：`http://localhost:4321/auth/chenxi`（开发）+ 生产域名回调
- **授权范围**：`openid profile`
- **PKCE**：强制 S256

---

## 三、正版授权校验（N2 占位骨架）

`chenxi.license.enabled=false`（默认）时一切为空操作。启用后（N2 落地时）：

- `LicenseClient` 在应用启动后 30 秒与之后每 6 小时调 `verify-url` 校验；
- 校验结果本地缓存 `cache-days`（默认 7 天）；
- 离线（校验不可达）在 `offline-grace-days`（默认 3 天）内放行；超宽限期**仅在前端横幅提示，绝不锁数据、绝不拒绝服务**；
- 当前状态可通过 `GET /api/license/status` 读取（`disabled / valid / grace / invalid`），公开只读。

**服务端协议只定义到接口层**（`license/dto/LicenseVerifyRequest|Response`），verify 服务端不在此仓库实现。授权按年计，与通行证会话令牌的 30 天滑动过期完全解耦（见上文表格）。

## 四、统计上报（N2 占位骨架）

`chenxi.stats.enabled=false`（默认）时 `StatsReporter` 为**空实现 stub**。启用后仅做尽力而为（best-effort）上报到 `report-url`：任何失败都被吞掉并降级为调试日志，绝不影响业务。上报服务端协议同样不在本仓库实现。

## 五、相关代码

| 位置 | 说明 |
| --- | --- |
| `luomiblog-backend/src/main/java/com/luomiblog/chenxi/` | 通行证登录：配置、服务与控制器（`ChenxiProperties` / `ChenxiAuthService(Impl)` / `ChenxiAuthController`） |
| `luomiblog-backend/src/main/java/com/luomiblog/license/` | N2 占位：`LicenseClient` / `StatsReporter` / DTO / `LicenseProperties` / `StatsProperties` |
| `luomiblog-backend/src/main/java/com/luomiblog/security/JwtUtil.java` | 会话令牌：`generateChenxiSessionToken` / `shouldSlide` / `slideToken`（滑动刷新） |
| `luomiblog-backend/src/main/resources/db/schema.sql` | `users.chenxi_sub` 建表定义 |
| `luomiblog-backend/src/main/resources/db/migration/V2__add_chenxi_sub.sql` | 存量库手动迁移脚本 |
| `luomiblog-frontend/src/utils/chenxi.ts` | PKCE 工具（Web Crypto S256、state/nonce、startChenxiLogin） |
| `luomiblog-frontend/src/pages/auth/chenxi.astro` | OAuth 回调页 |
| `luomiblog-frontend/src/components/LoginModal.vue` | 登录弹窗中的"辰汐通行证登录"入口 |
| `luomiblog-frontend/src/utils/api.ts` | `X-New-Token` 滑动续期的前端静默落地 |

## 六、有意不做的事（保持模板简单）

- 不做通行证 token 内省（introspection）、不做 refresh token 绑定：会话由本站 JWT 承载，通行证 token 用完即弃；
- 不做通行证硬币（芙贝币）等生态业务 API；
- 不提供"已有本地账号绑定通行证"的 UI：如需绑定，管理员可在数据库中为该账号补填 `chenxi_sub`；
- 未做账号注销联动：通行证侧注销不影响本站已签发的 JWT（不活动 30 天自然失效）；
- 授权校验与统计上报仅做到客户端骨架与协议接口层，服务端由辰汐生态 N2 统一提供。
