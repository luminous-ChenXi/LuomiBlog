# LuomiBlog 第三轮全链路调查报告(AUDIT-ROUND3)

> 调查日期:2026-10-07 | 审计人:辰汐(由 ZCode 五路并行深查 + 单测 + Docker 链路冒烟实测)
> 前置文档:[BUGLIST.md](BUGLIST.md)(前两轮审计底稿,86+ 项,三批修复已勾销 34 项)。本报告**只收录新发现与现状核实**,不重复前两轮已录条目,编号使用 `R3-xxx` 以便后续合并。
> 用途:修复工作底稿。每条含:严重度、位置(文件:行号)、证据、影响、业界标准修复范式、验证方法。

---

## 0. 本轮调查方法

| 手段 | 内容 | 结果 |
|---|---|---|
| 五路并行深查 | ①注册→邮箱验证→登录→2FA→JWT→刷新→登出 ②文章撰写→发布→修改→删除→MD 同步+统计 ③图片/文件上传+管理 API 全集 ④安全传输与加密专项 ⑤CI/CD 与构建部署 | 覆盖后端全部 19 个 controller、security/service 核心包、前端关键路径、全部 workflow 与部署脚本 |
| 单元测试 | `mvn test`(wrapper Maven 3.9.11 + JDK 21) | **28/28 全绿**(ClientIpResolver 8、SafePathValidator 9、TotpReplayGuard 4、TotpUtil 7,4.3s) |
| 链路冒烟实测 | Docker MySQL 8(3307) + 真实 jar 启动 + API 全链路 | 安装向导、注册/登录、发文、点赞/浏览实测,**10 项新问题实锤**(下表标 ⚡实测) |
| 已修项回归验证 | 对 BUGLIST 三批修复逐项代码核验 | 11 项确认修好;**2 项账面已修实未修**(R3-034/R3-035) |

---

## 1. 链路实测结论总表(本轮)

| 链路 | 结论 | 关键编号 |
|---|---|---|
| 安装向导(全新装) | ❌ **第 6 步 create-admin 必 500——全新安装主路径断裂** | R3-001(P0)⚡ |
| 注册→写库(邮箱验证关) | ✅ 可用,直发令牌 | — |
| 登录→JWT→/me | ✅ 可用 | — |
| 改密→会话踢出 | ⚠️ access 踢出生效,**refresh 绕过踢出** | R3-002(P1)⚡ |
| 发文(带 tagIds) | ✅ 可用 | — |
| 文章修改(PUT) | ❌ 部分更新 500;status 无白名单;软删可复活;非法 tagId 幽灵入库 | R3-013/014/015/012⚡ |
| 匿名点赞/浏览 | ❌ body userId 可冒充;visitorId 轮换可刷 | R3-003/004(P1)⚡ |
| 图片上传 | ✅ 确认"零实现",攻击面仅 MD-JSON 上传(校验良好) | §8 |
| 管理 API 权限矩阵 | ✅ 全部端点有 @PreAuthorize + URL 网关兜底,无缺失 | —(但见 R3-017/018) |
| CI(质量门禁) | ✅ typecheck 已进 CI,permissions/timeout 已硬化;无发版流水线 | R3-025 |
| 部署文档 | ⚠️ 主链路可走通,进程守护/HTTPS/防火墙/artifact 警示四大缺口 | R3-024/026 |

---

## 2. P0 — 致命(1 条,发布阻断)

### R3-001 ⚡ `[ ]` 全新安装 create-admin 必 500:token_version 列无默认值(Wave13 回归 × BUG-086)
- **严重度**:P0 致命(安装主路径断裂,所有新用户装不完) | **类型**:回归 bug
- **位置**:`InstallServiceImpl.java` create-admin 手写 INSERT(users 列清单不含 token_version);`entity/User.java`(Wave13 新增 tokenVersion 字段);根因 `application.yml` `ddl-auto: update`(BUG-086 未根治)
- **证据**(冒烟实测):全新 MySQL 8 + jar 启动 → 安装向导 test-database ✅ → execute-sql ✅ → **create-admin 500**:
  ```
  error code [1364]; Field 'token_version' doesn't have a default value
  ```
  schema.sql:88 本身有 `token_version INT NOT NULL DEFAULT 0`,但 Hibernate `ddl-auto: update` 抢先建表,Hibernate 生成的列**无默认值**,安装向导 `CREATE TABLE IF NOT EXISTS` 全部空转(BUG-086 的直接危害实例)。手工 `ALTER TABLE users MODIFY token_version INT NOT NULL DEFAULT 0` 后安装走通。
- **影响**:每个全新部署的用户都会在第 6 步卡死;且**证明三批修复后从未重跑过完整安装向导**。凡未来再给 User(或任何实体)加非空字段都会以同样方式炸掉全新安装与旧库升级。
- **业界范式**:①立即止血:安装向导 execute-sql 末尾追加幂等修复 `ALTER TABLE users MODIFY token_version BIGINT NOT NULL DEFAULT 0`(或 create-admin INSERT 显式包含 token_version=0);②根治:BUG-035 的 Flyway 化——安装前 `ddl-auto: none/validate`,schema.sql 为唯一事实源,新增字段一律走迁移脚本;③CI 增加"全新安装冒烟"job(Testcontainers 起空库→走 4 个安装 API→断言 admin 可登录),本次回归即可被 CI 拦截。
- **验证**:空库重走安装向导全程 200;同时给旧库用户一份"一次性数据修复 SQL"(与 BUG-087 同批)。

---

## 3. P1 — 高危(4 条)

### R3-002 ⚡ `[ ]` Refresh 令牌完全绕过 token_version——改密后旧 refresh 仍可续命 7 天
- **位置**:`security/JwtUtil.java:73-86`(generateRefreshToken 不写 `ver` claim);`service/impl/AuthServiceImpl.java:685-722`(refreshToken() 无版本比对);`AuthServiceImpl.java:716`(刷新时还把**当前 DB 版本**烘焙进新 access,替被盗会话"洗白")
- **证据**(冒烟实测):登录 → 改密成功 → 旧 access `/me` **401**(踢出生效✓)→ 旧 refresh 调 `/api/auth/refresh` → **200,换取全新 accessToken+refreshToken**。
- **影响**:BUG-033 的修复只覆盖 access 通道。攻击者窃得 refresh 后,用户改密也无法止损,7 天内随时复活会话——这正是 token_version 想防的核心场景在最持久令牌上复活。
- **业界范式**(OWASP JWT Cheat Sheet / Auth0 轮换):①refresh 签发写入与 access 相同的 `ver`,刷新时与 `user.getTokenVersion()` 比对,不一致 401;②更彻底:refresh 改 opaque 随机串、SHA-256 落库,验证查库,天然支持全量吊销(可复用 revoked_tokens 基建)。
- **验证**:同冒烟步骤,修复后第 4 步应 401。

### R3-003 ⚡ `[ ]` 匿名分支采信请求体 userId——可冒充任意用户点赞/污染行为流水
- **位置**:`controller/ArticleStatsController.java:41,73,102-104`(`Long userId = userPrincipal != null ? userPrincipal.getId() : request.getUserId();`);对照正确写法 `LikeController.java:31`(匿名一律 null)
- **证据**(冒烟实测):无 token POST `/api/articles/1/like` body `{"userId":1,"visitorId":"attacker-aaa"}` → **以管理员(user_id=1)身份点赞成功**,`article_likes` 出现 `user_id=1` 行;再发一次同 body → 把管理员的赞取消。POST `/view` 带 userId=1 → `user_behavior` 留下 `user_id=1, VIEW` 流水。
- **影响**:①恶意清除任意用户的点赞/在其名下伪造点赞;②把 VIEW 流水记到真用户名下,吃掉其 24h 去重额度(正常浏览不计);③`GET /stats?userId=2` 可探测任意用户点赞状态。注释自称"身份以 JWT principal 为准",实现却只在登录分支成立。
- **业界范式**(OWASP A01:2021 Broken Access Control——"不信任客户端身份声明"):匿名分支 userId **一律置 null**(identity 只能来自服务端可验证凭据);全局排查 `request.getUserId()` 类模式。
- **验证**:匿名 curl 带 userId 点赞两次,断言 `article_likes` 无该用户行(修复后)。

### R3-004 ⚡ `[ ]` visitorId 轮换刷赞/刷浏览量 + 限流失效(无 IP 维度去重)
- **位置**:`ArticleStatsServiceImpl.java:96-107`(24h 去重只按 visitorId);`ArticleStatsController.java:143-153`、`LikeController.java:73-82`(限流键 `v:{visitorId}|{ip}` **含客户端可控的 visitorId**);`ArticleStatsServiceImpl.java:43-45`(visitorId 缺失时服务端发随机 UUID=放弃去重);`article_likes.ip_address` 落库但从不参与去重
- **证据**(冒烟实测):同一 IP 轮换 5 个 visitorId → like_count 0→5,5 行点赞记录,无任何拦截。
- **影响**:脚本每请求换 visitorId 即可无限刷赞、刷 PV(每分钟上限形同虚设),统计/热门排序全部可被操纵。
- **业界范式**(防刷纵深):①匿名限流键**只用 IP**(`ip:` 桶),visitorId 桶作附加约束取更严者;②匿名点赞加 IP 维度二次去重(同 IP 同文章 24h 计数封顶,超限不加计数);③浏览去重补 `(articleId, ip, 24h)` 查询;④visitorId 服务端缺省时拒绝(400)而非发 UUID;⑤`@Size(min=8,max=64)` 约束(顺带修 R3-036)。
- **验证**:循环 100 次随机 visitorId 点赞,断言 like_count 增幅≈0。

### R3-005 `[ ]` 安全状态与匿名可控限流键共用 1 万条 Caffeine(BUG-015 修复不彻底)
- **位置**:`service/impl/MemoryCacheServiceImpl.java:22-25`(单一 `maximumSize(10000)` 实例);消费方:`LoginSecurityServiceImpl.java:20-22`(锁定/失败计数)、`AuthServiceImpl.java:93-99`(邮箱验证 token、2FA 挑战)、`TotpReplayGuard.java:21`(防重放);匿名写入源:`ArticleStatsController.java:49-50,76-77`(visitorId 进 interaction 键,可无限轮换制造新键)
- **影响**:revoked_tokens 已 DB 化(✓),但**账号锁定、2FA 挑战、TOTP 防重放、邮箱验证令牌**仍在内存桶里——脚本刷 1 万个 interaction 键即可把它们 LRU 逐出:被锁账号提前解锁、防重放失效、验证中途的挑战被挤掉。
- **业界范式**:分桶——`security-challenge`(2FA/邮箱/防重放/锁定)与 `ratelimit` 至少两个独立实例;安全桶条目少但不可驱逐(Caffeine 可按桶给大 maximumSize),或落 DB(同 revoked_tokens 范式);visitorId 键加长度/熵约束。
- **验证**:刷满 1 万键后,先前 `login:lock:{user}` 应仍生效(修复后)。

---

## 4. P2 — 中(正确性/安全/架构)

### 会话与账号安全

- **R3-006 `[ ]` 2FA 强制绑定可"仅凭密码"完成**:`AuthServiceImpl.java:314-341` 开启 `totp_required` 后,登录响应直接下发 `secret`+`otpauthUri`+challengeToken,攻击者持密码即可用自己的认证器完成绑定并拿走 10 个还原码,真实用户反被锁死。范式:绑定需第二信道确认(向已验证邮箱发确认链接,Google/GitHub 范式)或 re-auth;至少不做"挑战态自动下发 secret"。验证:开启开关,仅凭密码走完 enroll 应被拒。
- **R3-007 `[ ]` 改绑邮箱零验证**:`UserServiceImpl.java:42-44` 直接 `setEmail`,不重置 `emailVerified`、不预检占用(撞唯一键 500)、不发确认邮件。范式:双邮箱确认流程(旧邮箱撤销通知+新邮箱验证),确认前不落库;预检 `existsByEmail` 返回 409。验证:改成他人邮箱应 409 而非 500;改后 `email_verified` 应置 false。
- **R3-008 `[ ]` `/api/auth/chenxi/exchange` 无限流**:`ChenxiAuthController.java:38-44` permitAll 且无频控,每次触发后端向通行证发起真实出站请求——可被用来放大打通行证/灌爆 login_logs。范式:对齐注册/2FA 通道挂 IP 频控(5-10/min)+失败退避熔断。验证:连发 100 次假 code,第 N 次起应 429。
- **R3-009 `[ ]` SMTP 授权码与 TOTP 密钥 DB 明文**:`InstallController.java:220` 落库、`MailServiceImpl.java:40` 明文读、`User.java:64-65` totp_secret 原样存。倒库即丢第二因子(TOTP 种子=可无限生成验证码)。范式:应用层 AES-256-GCM,KEK 走环境变量(`SECURITY_ENC_KEY`);至少 README 标注 DB 属信任边界、备份加密。验证:SELECT 两列当前均明文。
- **R3-010 `[ ]` trust-proxy 未入 yml 的实际后果=全站限流桶共享(BUG-076 加重)**:`ClientIpResolver.java:35` 默认 false,而主推部署方式是 nginx 反代——此时 `remoteAddr` 恒 127.0.0.1:注册 5 次/h 的桶变全站共用,**攻击者注册 5 次即可让全站 24h 无人能注册**;登录锁定键、审计 IP 全部失真。解析器代码本身正确,只差配置落地。范式:`application.yml` 增加 `app.security.trust-proxy: ${APP_SECURITY_TRUST_PROXY:false}` + README"反代后必设 true"。验证:反代下登录失败 5 次,换浏览器仍提示锁定即复现。
- **R3-011 `[ ]` 辰汐 issuer 未强制 https**:`ChenxiProperties.java:30` 无格式校验,配 `http://` 不报错——PKCE code_verifier 明文过网可被中间人截换 token。范式:`@PostConstruct` 校验 enabled 时 issuer 必须 `https://`,fail-fast。

### 文章链路(含"修改文章"专项)

- **R3-012 ⚡ `[ ]` 软删文章可经 update/publish 复活,MD 文件重新落盘公开可见**:`ArticleServiceImpl.java:102-107,287-302` 的 findById 不过滤 deletedAt;实测:DELETE 后 PUT status=published → 200,`bad-tag-post.md` **重新出现在 content/blog**,前台文件驱动渲染恢复可见( deleted_at 仍在但无拦截)。范式:管理写路径统一 `findByIdAndDeletedAtIsNull`(或命中软删即 409)。
- **R3-013 ⚡ `[ ]` updateArticle 状态字段无白名单**:实测 PUT `status:"PUBLISHED"` → 200 且**大写原样入库**——`findPublishedArticles` 只认小写,该文从所有公开列表消失、统计口径漏计、MD 同步状态机对它不生效。范式:`@Pattern("draft|published|archived")` + 显式状态迁移表(合法迁移白名单)。验证:大写/任意串应 400。
- **R3-014 ⚡ `[ ]` tagIds 不校验存在性——非法 tagId 幽灵关联行静默入库**:实测 POST tagIds=[999999] → **200**,`article_tags` 留下 `(article_id=2, tag_id=999999)` 脏行。比前两轮预测的"FK 500"更糟:Hibernate `ddl-auto: update` 建的表**连外键约束都没有**(BUG-086 连带危害),装配时 filter 不可见但脏数据持久化。范式:先 `findAllById`+deletedAt 过滤,缺失 400;分类同批(BUG-021 已修的 categoryId 判空之外补存在性校验)。验证:非法 tagId 应 400;库中无幽灵行。
- **R3-015 ⚡ `[ ]` updateArticle 部分更新即 500 + 无并发编辑保护**:实测 PUT 只传 `{"title":...}` → 500(`language/allowComments/allowSuggestions` nullable=false 列被 null 直灌);`entity/Article.java` 无 `@Version`,请求无 expectedUpdatedAt——双窗口编辑后保存者整体覆盖先保存者,无冲突检测。范式:①"未传=不修改"(null 跳过 set);②乐观锁:实体 `@Version` 或请求带 `expectedUpdatedAt`,不匹配 409(Ghost/WordPress 并发编辑保护同款)。验证:部分 PUT 应 200 且其余字段保持;双编辑第二次应 409。
- **R3-016 `[ ]` publishArticle 专用端点不建 MD、contentHash 全程不维护 → 反向同步可覆盖 DB**:`ArticleServiceImpl.java:287-302` 无 createMarkdownFile 调用;create/update 均从不写 `content_hash` → `ArticleSyncServiceImpl.isSyncNeeded:161-168` 对所有后台撰写的文章**恒报"需要同步"**,管理员一旦点"全部同步",`updateArticleFromFile` 用(可能落后的)文件内容反向覆盖 DB。范式:publish 复用 update 状态机入口;createMarkdownFile 成功后同步计算落 contentHash(与 sync 侧同一内容口径),使 hash 成为双源一致判定依据。验证:publish 后 isSyncNeeded 应 false。
- **R3-017 `[ ]` BLOGGER 被 URL 网关挡死在 /api/admin/** 外,与注解/菜单种子矛盾,403 还误清会话**:`SecurityConfig.java:59` `/api/admin/** hasRole("ADMIN")` vs `AdminArticleController.java:28` `hasAnyRole('ADMIN','BLOGGER')` vs `data.sql:111` 菜单 `allowed_roles ["admin","blogger"]`;前端 `admin/articles.astro:497-503` 收到 403 → 清 token 踢登录(循环)。BLOGGER 打开"文章管理"即被踢。范式:URL 层放宽 `hasAnyRole('ADMIN','BLOGGER')`(细粒度交方法注解+service 属主校验)或菜单 seeding 收敛 admin-only,二者取一;403 与 401 的前端处理分开(403 不清凭据)。
- **R3-018 `[ ]` "禁止封禁管理员"被任意非 banned 状态绕过**:`AdminUserServiceImpl.java:148-157` 只拦字面量 `"banned"`,而 `UserPrincipal.isEnabled()` 要求 `status=="active"`——管理员 A 可对 B 调 `{"status":"disabled"}` 绕过 CANNOT_BAN_ADMIN 把 B 全请求锁死(互锁);任意脏字符串还持久化进 users.status。范式:状态枚举白名单(`common/UserStatus.java` 已有常量未被引用);保护条件改"目标是 ADMIN 且新状态 != active 一律拒绝"。验证:admin 对 admin 设 disabled 应 400。

### 注册输入面

- **R3-019 `[ ]` 公开注册用户名无白名单/保留字/归一化(与安装向导双标)**:`RegisterRequest.java:11-13` 无 `@Pattern`(安装向导 `AdminAccountRequest` 有 `^[a-zA-Z0-9_]+$`);可注册 `admin`、`root`、`admin `(尾随空格,MySQL NO PAD collation 下与 admin 共存)、全角同形字 `аdmin`、`<b>x</b>`——前台展示冒充/钓鱼面。范式:`@Pattern` + 服务端保留字黑名单(含变体)+ NFKC 归一化 + trim 查重。验证:上述用户名注册应 400。

### 前端会话/安全头

- **R3-020 `[ ]` 前端"半套会话":X-New-Token 不消费 + refresh token 存而不用**:`utils/api.ts:156-259` 不读任何响应头(全前端 grep X-New-Token=0);`stores/user.ts:50-51` 把 7 天 refresh JWT 存 localStorage,唯一用途是登出时交后端吊销——纯增 XSS 窃取收益,零价值。范式二选一:(a) request() 接住 X-New-Token 更新本地(配合修 BUG-034 暴露头);(b) **后端停发 refresh、前端删整条链路**(最诚实,推荐先 b 后按需做 a)。
- **R3-021 `[ ]` 安全响应头打错靶子**:`config/SecurityHeadersConfig.java:20-37` 把 CSP(含 unsafe-inline/eval)/HSTS/X-XSS-Protection 无差别下发到 **API JSON**;真正面向访客的 Astro 前端(astro.config.mjs)零安全头。JSON 不执行脚本,CSP 无意义;HTTP 上的 HSTS 浏览器按 RFC 6797 直接忽略;`X-XSS-Protection` 已被现代浏览器移除(死代码)。范式:CSP/HSTS/Referrer-Policy 下沉到 nginx(443 server 块)或 Astro middleware;API 侧保留 X-Content-Type-Options/X-Frame-Options 即可。

### CI/部署(新发现部分)

- **R3-022 `[ ]` rss/atom 作者硬编码原作者身份**:`pages/rss.xml.ts:5-6` `AUTHOR_EMAIL='chenxi@luminouschenxi.net'`、`AUTHOR_NAME='辰汐'`(atom.xml.ts 同源)——自建用户的每条 RSS 作者都是原作者(邮箱可被爬虫收割)。范式:作者取自站点设置(安装向导已落库)。验证:build 后 grep dist/client/rss.xml。
- **R3-023 `[ ]` PUBLIC_API_URL 缺构建期校验 + 危险 fallback**:`src/config/api.ts:1` `|| 'http://localhost:8080'`——漏建 .env.production 时构建静默成功,浏览器侧全部交互指向访客自己机器的 8080,全站互动静默全挂。范式:PROD 构建缺变量直接 throw(或至少 warning);生产默认相对 `/api`。

---

## 5. P3 — 低(硬化工单)

**认证细节**
- **R3-024 `[ ]` BUG-026 账面已修实未修**:2FA 成功清 `user.getUsername()+":"+ip`(`AuthServiceImpl.java:521`),失败记 `usernameOrEmail+":"+ip`(`:280`)——邮箱登录用户失败计数永不清零,反复误锁。修:authenticate 成功拿到实体后统一用 `user.getUsername()+":"+ip`。
- **R3-025 `[ ]` TotpReplayGuard 仍非原子(BUG-058 未修确认)**:`TotpReplayGuard.java:39-44` get→比较→set,并发同码双放行。修:`cache.asMap().compute()`(同项目 MemoryCacheServiceImpl.increment 已示范)。
- **R3-026 `[ ]` 邮箱验证码比较非常数时间**:`AuthServiceImpl.java:233` `equals()`;改 `MessageDigest.isEqual`(与 TOTP 侧统一)。
- **R3-027 `[ ]` banned/inactive 预检在密码校验前抛出**——免密码探测账号封禁状态。修:自定义 AuthenticationProvider 折叠进 BAD_CREDENTIALS。
- **R3-028 `[ ]` LoginRequest 字段无长度上限**——2MB usernameOrEmail 直入 Caffeine 键(条数限制不限字节),慢速灌入可内存放大;超长审计记录静默丢失。修:`@Size` + service 层截断。
- **R3-029 `[ ]` 注册 nickname 无长度校验**——>120 字符触发 SQL 截断 500。修 `@Size(max=120)`。
- **R3-030 `[ ]` 密码策略不一致**——改密无强度校验不拒新旧同密(`UserServiceImpl.java:67-83`);管理员重置仅 `@NotBlank`(可 1 位密码);>72 字节 BCrypt 截断(R3 相关注:注册侧已 `@Size(max=100)`,100 字符仍超 72 字节线)。修:三处共用 `PasswordPolicy.validate()`,`max=72`。
- **R3-031 `[ ]` refresh 轮换无家族吊销**——重放已轮换 refresh 仅拒绝该 jti;范式:令牌带 family,重放即吊销全族。
- **R3-032 `[ ]` 挑战 failCount 读改写竞态**——并发失败可超 5 次(IP 频控兜底)。修:compute 原子自增。

**文章/前端残余**
- **R3-033 `[ ]` visitorId 无长度校验**——>64 字符插入时截断 500。修 `@Size(min=8,max=64)`(并入 R3-004)。
- **R3-034b `[ ]` 非原子计数死代码地雷**:`ArticleServiceImpl.java:260-275` incrementViewCount/LikeCount 读改写仍在(无调用方)。删或改调原子方法。
- **R3-035b `[ ]` 编辑器死 UI**——封面/置顶/作者三控件不进 payload(`ArticleEditor.vue:262-299` vs `:958-969`),填写即静默丢弃。补 DTO 或删控件(对齐 WIP 诚实化)。
- **R3-036b `[ ]` KaTeX 竞态残余+无草稿自动保存**——CDN script 无 onload promise(首次预览公式不渲染);组件无任何草稿保存(误关丢稿)。修:promise 化;beforeunload+localStorage 草稿。
- **R3-037b `[ ]` 评论链路残余**——不校验文章存在/allowComments 开关(不存在文章 FK 500);游客评论字段不可达(死字段)。
- **R3-038b `[ ]` `[slug].astro:120` contentHtml 契约残渣**——后端无此字段,恒 undefined(确认无"传 HTML 不消毒"入口,该 XSS 面不存在;mermaid loose 仍是 BUG-056)。删残渣。
- **R3-039b `[ ]` SafePathValidator 残余**——`ArticleSyncServiceImpl.java:131` resolveConflict USE_FILE 分支用 DB slug 拼 path 未过校验(只读,P3)。
- **R3-040b `[ ]` 后台"创建副本"调不存在端点**(`admin/articles.astro:1348` POST /duplicate,全仓 0 命中)必 404;"**查看文章**"跳 `/articles/{id}` 路由与参数双错(`:1332`,应为 `/article/${slug}`)。修:实现或删;跳转改 slug。
- **R3-041b `[ ]` 管理端更新用户绕过 SafeUrlValidator**(`AdminUserServiceImpl.java:111-113` 无校验直存 avatarUrl,与自助端点双标)。复用 applyAvatarUrl。

**上传/配置/工程化**
- **R3-042b `[ ]` MD 上传无大小上限**——content 可 50MB 直写盘(磁盘 DoS+构建卡死);`spring.servlet.multipart` 全局配置未显式声明(未来加端点会静默吃默认 1MB/10MB)。修:`@Size(max=2_000_000)`+yml 显式声明。
- **R3-043b `[ ]` favicon-config 假成功 stub**(`InstallController.java:187-200` 直 return success,site_favicon 恒 NULL)——"接口先承诺、实现后补课"模具,未来补实现最顺手做法就是把未消毒 SVG 存库内联输出=现成存储型 XSS。修:改 501/WIP;实现时服务端 SVG 消毒+`Content-Type: image/svg+xml`+仅 `<img>` 引用(OWASP SVG 范式)。
- **R3-044b `[ ]` swagger/actuator permitAll 死规则地雷**(`SecurityConfig.java:53-54`)——当前依赖不存在零暴露(好事),但日后引入即匿名公开且不触发评审。删 matchers 或注释警示。
- **R3-045b `[ ]` 依赖版本面**——Boot 3.4.13(OSS 支持 2025-12-31 止,无安全补丁)、tomcat 11.0.21 跨主版本 override(Boot 3.4 认证组合是 10.1.x)、okhttp 4.9.2(2021)、bcprov 1.80(CVE-2025-8885/8916,1.81 修)、四处 BOM 手工覆盖未受测试矩阵覆盖。修:整体升 Boot 3.5.x 并移除 tomcat override;okhttp 4.12/bcprov 1.81+;确认无传递依赖的"休眠 pin"直接删。
- **R3-046b `[ ]` 安全态单机内存缓存**——多实例/容器漂移即验证码在 A 发 B 验必失败、限流按实例翻倍。修:迁移 Redis 或 README 明确"仅支持单实例"。
- **R3-047b `[ ]` qodana.yaml 孤儿+JDK 口径不一**(25 vs 21,无 workflow 引用)。接入或删。
- **R3-048b `[ ]` .gitignore 缺口**——新锁路径 `luomiblog-backend/config/install.lock`、`uploads/` 未忽略(新装后 `git status` 出现未跟踪锁文件,可能误提交部署状态)。
- **R3-049b `[ ]` .env.production.example 大半死变量**——PUBLIC_SITE_NAME/PUBLIC_AI_ENABLED/PUBLIC_COMMENTS_ENABLED 等 7 个在 src 零引用,用户改了毫无效果(站点名实际由安装向导落库驱动)。示例只留实际生效变量并注明。

---

## 6. 已修项回归验证(本轮确认通过)

| 项 | 证据 |
|---|---|
| 注册邮件异步化 | `AuthServiceImpl.java:208` 事务内只发事件;`RegistrationEmailListener.java:38-39` @Async(mailExecutor)+AFTER_COMMIT;失败仅删缓存告警,不再回滚注册 ✓ |
| 邮箱验证 token 熵/时效 | UUID(122 位 SecureRandom)+6 位 code;内存缓存 TTL 1800s;验证后删除+5 次失败作废+IP 频控 ✓ |
| logout 双令牌吊销 | `AuthController.java:106-112` 接收 refreshToken;按 jti+剩余寿命写 DB 黑名单,幂等;每日 03:47 错峰清理 ✓ |
| slug/文件名路径安全 | 创建/更新/删除/上传四处全过 isSafeSlug/isSafeArticleFilename+isInsideRoot 纵深 ✓(唯一残余见 R3-039b) |
| MD 删除侧一致性 | deleteMarkdownFileAfterCommit/软删撤文件/归档撤文件/改 slug 清旧文件 ✓(写入侧仍在事务内=已知取舍,无对账任务兜底) |
| 计数原子性 | ArticleRepository/CommentRepository 原子 UPDATE;24h 去重;visitorId 缺失拒绝;并发双击唯一键幂等 ✓(轮换面除外=R3-004) |
| 列表不泄露全文 | ArticleListItemResponse 无 content/aiSummary;4 个列表方法全走 toListItemResponses ✓ |
| 管理 API 权限矩阵 | 全部 /api/admin/* 有 @PreAuthorize+URL 网关;自我保护(不可自封/自改角色/删 admin)齐全;降权即时生效(filter 每请求回库载角色)✓ |
| TOTP 质量 | 20 字节 SecureRandom 密钥、±1 窗口、MessageDigest.isEqual 常量时间比较 ✓(防重放原子性除外=R3-025) |
| CI 硬化 | astro check 进 CI、permissions: contents:read、concurrency、timeout-minutes ✓(SHA 固定未做) |
| JWT 协议层 | jjwt 0.12.6 parseSignedClaims+verifyWith,杜绝 alg:none;secret 无默认值,缺省/短于 32 字符拒绝启动 ✓ |
| 安装票据 | 256bit 随机+SHA-256 落盘+常量时间比较;锁后向导 403 ✓ |

---

## 7. 已知未修项现状核实(BUGLIST 遗留,本轮确认仍在)

| BUGLIST 编号 | 现状 | 备注 |
|---|---|---|
| BUG-023 | 仍在 | token+code 同置 URL query |
| BUG-025 | 部分改进 | 注册/改密已 `@Size(max=100)`,但 100>72 字节截断线(见 R3-030) |
| BUG-028 | 仍在 | 后台分类筛选 JPQL 无 category 条件 |
| BUG-029 | 仍在 | admin 也不能改他人文章(一刀切属主校验) |
| BUG-030 | 仍在 | 登录 catch-all 返回 e.getMessage() |
| BUG-031 | 部分 | 主路径已统一 BAD_CREDENTIALS;resend/注册仍枚举;新观察面见 R3-027 |
| BUG-032 | 仍在 | 分页 size 无 clamp(仅收藏列表 clamp 了) |
| BUG-034 | 仍在 | CORS 未暴露 X-New-Token;`JwtAuthenticationFilter.java:27` 注释谎称已暴露(误导,顺手修注释) |
| BUG-035 | 仍在 | 无 prod profile;ddl-auto:update/show-sql:true/security DEBUG |
| BUG-056 | 仍在 | mermaid securityLevel:'loose' |
| BUG-058 | 仍在 | =R3-025 |
| BUG-059 | 仍在 | ArticleSync 锁非原子+裸线程(new Thread 无上界) |
| BUG-060 | 仍在 | findBySlug 不滤 deletedAt;**已升级**:见 R3-012(写路径复活) |
| BUG-062 | 仍在 | 最近动态 sort((a,b)->0) |
| BUG-073 | 仍在 | pom 0.2.0/package 0.0.1/CHANGELOG 0.4.0/零 tag/README 引用 0.2.0.jar/badge 3.4.1 vs 实 3.4.13 |
| BUG-074 | 仍在 | reset-install.ps1 删旧路径(实际 config/install.lock);且无 $PSScriptRoot 锚定、无 sh 版 |
| BUG-077 | 仍在 | astro site 硬编码 luomiblog.com;新增 RSS 作者硬编码(见 R3-022) |
| BUG-079/080 | 仍在 | 构建期 favicon 打后端+RSS/atom/robots 构建期快照(CI artifact 里 RSS 是空快照) |
| BUG-085 | 仍在 | 默认作者兜底 authorId=1(无用户时 FK 500) |
| BUG-086 | 仍在 | **本轮 P0(R3-001)即其危害实例**;连带:Hibernate 建表无外键(R3-014 实锤) |
| BUG-052 关联 | 新增 | R3-040b 两处假按钮(duplicate/查看) |

**BUGLIST 元问题(修底稿时一并处理)**:
1. §1 链路表把"硬编码域名/构建期打后端"标为 BUG-073/074,与 §5 定义(077/079)编号错位;
2. §8 标记 BUG-026 已修、§4 标记 BUG-058→实均未修(本轮 R3-024/R3-025 实锤)——"声称已修"需复核机制(每批修复后跑对应验证,而非只跑 mvn test)。

---

## 8. 图片上传链路专项结论(用户点名)

**结论:图片/媒体上传为"零实现",当前无任何二进制上传端点。** 全局 grep:0 处 MultipartFile、0 处 transferTo/Files.copy。全部 6 个沾边入口:

| 入口 | 类型 | 校验 | 风险 |
|---|---|---|---|
| POST /api/admin/articles/upload | JSON 传 .md 文本(非 multipart) | 文件名 @Pattern 白名单+SafePathValidator 双重防遍历 | P3:content 无大小上限(R3-042b) |
| POST /api/install/favicon-config | JSON:任意 SVG 代码 | 仅 @NotBlank type | **不落库——stub 假成功**(R3-043b 模具) |
| POST /api/users/avatar | avatarUrl 外链引用 | SafeUrlValidator 协议白名单 | 低 |
| PUT /api/users/profile | avatarUrl 字段 | 同上 | 低 |
| PUT /api/admin/users/{id} | avatarUrl 字段 | **无校验**(R3-041b 双标) | P3 |
| GET /api/site/favicon、/config | 只读输出 system_config | — | 低(恒 NULL 无写入者) |

`attachments` 表(biz_type ENUM avatar/article_cover...)、`system_config.max_upload_size`(种子 5MB)**零代码消费**——表结构先行、功能未实现;前端 attachment 管理页是 WIP 假壳;工作目录 `uploads/` 无 HTTP 映射不可访问。**未来实现媒体上传时的强制清单**(OWASP File Upload Cheat Sheet):magic bytes 校验(不信扩展名/Content-Type)、SVG 默认拒绝或消毒后隔离域提供、随机文件名(不用用户输入)、大小上限、存储目录不可执行、返回 URL 走独立域名或严格 CSP。

## 9. "修改文章"链路现状完整流程(用户点名)

1. **入口**:后台列表 →"编辑"→ `/admin/articles/edit?slug=xxx`(BUG-004 已修)→ edit.astro 取 query 传组件。
2. **加载**:GET `/api/articles/{slug}`(公开端点+checkArticleVisible 放行 ADMIN/BLOGGER 草稿预览)→ 回填表单(标签名数组)。
3. **提交**:编辑态 slug 锁定不可改 → resolveTagIds 名称→id 映射(不存在的跳过并提示)→ PUT `/api/articles/{id}`。
4. **后端 updateArticle**:findById(**不滤软删**=R3-012)→ 强属主校验(BUG-029,admin 亦不能改他人)→ slug 变更时 isSafeSlug+查重 → 逐字段覆盖(**null 直灌**=R3-015;status 无白名单=R3-013)→ tagIds 先删后建(**不校验存在性**=R3-014)→ 按(旧状态,新状态,slugChanged)三态写/删 MD(published 持续=重写、draft→published=建文件、published→非=afterCommit 删、slug 变=afterCommit 删旧)。
5. **MD 写入**:事务内拼 frontmatter→Files.writeString→回写 file_path;**contentHash 全程不维护**(R3-016)。
6. **反向同步**:sync/trigger(非原子锁+裸线程=BUG-059)扫盘比对 contentHash——后台写的文章恒 null→**恒报需同步**,一键同步=文件内容反向覆盖 DB(R3-016 的覆盖风险)。
7. **并发/草稿**:无乐观锁(R3-015)、无草稿自动保存(R3-036b)、双窗口编辑静默覆盖。

---

## 10. CI/CD 与构建部署链路(用户点名:帮其他用户构建与上传静态文件)

**CI 现状**(.github/workflows/ci.yml,唯一 workflow):push/PR→main;backend `mvn test`(纯单测不依赖 MySQL ✓);frontend `npm ci→astro check→build→upload-artifact(7 天)`;permissions 最小化+concurrency+timeout ✓;**无 tag→jar→release 流水线**(README 自认)——用户只能 clone main,无法锁定/审计版本(R3-025/BUG-073);actions 全 @v4 未固定 SHA。

**用户拿到代码到跑起来的真实路径**:
1. 自装 JDK21/Maven/Node20/MySQL8(仓库无 mvnw,README 已声明);
2. 建库(README 手动 SQL 或 JDBC createDatabaseIfNotExist)——**注意全新安装会在 create-admin 炸**(R3-001);
3. 后端 `mvn package` → 环境变量 JWT_SECRET/DB_* → `java -jar`("systemd 托管"仅一句无样例);
4. 前端 `cp .env.production.example .env.production` → `npm run build` → **不是纯静态!** @astrojs/node standalone:产物分 dist/client+dist/server,5 个页面(文章列表/详情、admin/users、编辑、新建)必须 node 进程实时渲染,README 已警告纯静态托管会 404;
5. 安装向导八步;6. nginx 统一入口(/api→8080,/→4321,README 样例质量不错)。

**"上传静态文件"的正确答案**:没有这一步——必须整目录 dist/(client+server)+node 常驻 `entry.mjs`;唯一例外是 README"路线 B(COS+CDN)"且自身标注进阶待改造。**CI artifact 不可直接部署**(无 .env.production+后端不在线构建:RSS 空快照、favicon 默认值)而 README 未警示——这是自建用户最容易踩的坑。

**部署文档缺口群(R3-050 汇总,成本最低收益最高)**:
- 前端 node 进程守护零覆盖(systemd unit/pm2/宝塔 Node 托管均未写)——掉坑率最高;
- HTTPS 零说明(nginx 样例只 listen 80);
- 防火墙/监听面:未提示 8080/4321 只绑 127.0.0.1(java 默认 0.0.0.0,公网直连绕过 nginx);
- artifact 警示缺失;CORS 分域名改法未给样例;
- 无 Dockerfile/docker-compose(全项目仅 Demo/AstrNest 有且被 gitignore)、无一键脚本;
- migration V1-V4 是手动迁移体系,但 README 从未说明升级老站该跑哪些 SQL(升级路径缺失);
- custom-application.yml 是死配置(全仓只删不写,spring.config.import 无引用),README/FAQ/脚本仍在描述不存在的文件(R3-053);
- technical-implementation-guide.md 部署章节与实际严重不符(幸而未入库)。

---

## 11. 修复路线图建议(第四批,结合市面范式)

**第一批:安装止血 + 令牌收口(约 1 人日,P0/P1)**
1. R3-001 安装向导 SQL 止血(一条 ALTER)+ CI 全新安装冒烟 job(Testcontainers);
2. R3-002 refresh 带 ver(或 opaque 落库,推荐后者一次到位)+ logout/refresh 全链路回归;
3. R3-003 匿名 userId 置 null(几行代码)+ R3-004 IP 维度限流/去重(同批做);
4. R3-005 缓存分桶(安全桶独立)。

**第二批:文章链路加固(约 1.5 人日,P2 主体)**
5. R3-012/013/014/015 一个 PR:updateArticle 重写(软删过滤+状态白名单+tagIds 预检+null 跳过+乐观锁 @Version);
6. R3-016 publish 复用状态机+contentHash 落库;
7. R3-017 BLOGGER 网关放宽(或菜单收敛)+ 前端 403 不清凭据;
8. R3-018 状态枚举白名单(UserStatus 常量启用)。

**第三批:安全纵深(约 1 人日)**
9. R3-006 2FA 绑定第二信道;R3-007 改邮箱双确认;R3-008 chenxi/exchange 频控;R3-009 密钥加密落库;R3-011 issuer https 校验;
10. R3-019 用户名白名单;R3-024/025(BUG-026/058 真修);R3-030 密码策略统一(max=72);
11. R3-010/BUG-076 trust-proxy 入 yml + R3-021 安全头搬家(nginx 样例一起给)。

**第四批:部署体验+工程化(约 1 人日,多为文档/配置)**
12. R3-024(BUG-074)脚本修路径+sh 版;R3-025(BUG-073)版本统一+打 tag+release job;R3-022/023 去硬编码+构建校验;
13. R3-050 部署文档四件套(systemd unit+nginx HTTPS 样例+防火墙提示+artifact 警示);R3-048b .gitignore;R3-049b env 示例;
14. BUG-035 Flyway 化(根治 BUG-086,建议下个功能版本前,越晚成本越高);
15. R3-045b 依赖升级(Boot 3.5.x 为主轴,tomcat override 移除)。

**每批修完的回归口径**(吸取 BUG-026/058"账面已修"教训):
- [ ] mvn test 全绿 + 新增测试(本批每条 P0/P1 至少 1 个单测)
- [ ] **Testcontainers 全新安装冒烟**(空库→4 个安装 API→admin 登录)
- [ ] Docker 冒烟五链路(注册→登录→改密后双令牌验证→发文→改/删/复活攻击→点赞冒充/轮换攻击)
- [ ] 逐条在 BUGLIST 打勾时贴出对应验证证据

---

## 12. 统计总览

| 严重度 | 本轮新发现 | 其中冒烟实测实锤 |
|---|---|---|
| P0 | 1 | 1 |
| P1 | 4 | 3 |
| P2 | 18 | 3 |
| P3 | 30+ | 1(幽灵关联归 P2) |
| 合计 | 53+ | 8 项 API 实测复现 |

前两轮 86 项中:三批修复经本轮回归验证**确认有效**;2 项账面已修实未修(BUG-026/058);约 20 项仍未动(多为 P2/P3 与工程化)。
