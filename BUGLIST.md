# LuomiBlog BUG 审计清单(BUGLIST)

> 生成日期:2026-10-05 | 审计人:辰汐(由 ZCode 多轮 subagent 审计 + 人工逐行复核)
> 用途:**修复工作底稿**。每条 BUG 含:编号、严重度、位置、证据、影响、业界标准修复范式、验证方法。
> 状态标记:`[ ]` 待修复 / `[x]` 已修复 / `[w]` 决定不修(需写明理由)

---

## 0. 审计方法与两轮调查说明

- **第一轮(五路并行深审)**:后端正确性 / 安全专项 / 性能与数据库 / 前端与架构 / 工程化与 CI,共覆盖后端 174 个 Java 文件中的全部 controller/service/repository/security 包、前端 76 个源文件中的全部关键路径、git 全历史(129 commits)敏感文件扫描。
- **第二轮(链路实测+逐行复核)**:
  - 后端现有单测实跑:`mvn test` → **11/11 通过**(仅 TOTP 两套,2.3s);
  - 前端 `astro check` → 0 errors / 6 hints(其中 `apiBaseUrl` 两条为 `define:vars` 注入的**误报**,非 bug);`npm run build` → **成功**(server 7.5s);
  - 逐行走查五条主链路:注册→邮箱验证→写库、登录→2FA→JWT→刷新→登出、密码哈希、文章撰写/发布/修改/删除+MD 同步、浏览量/点赞统计、CI/CD 构建产物链。
- 本清单已合并两轮全部发现,并**剔除**了复核后不成立的项(见 §6)。

---

## 1. 链路实测结论总表

| 链路 | 结论 | 关键问题编号 |
|---|---|---|
| 注册→写库 | ✅ 可用(开关关=直发令牌;开=inactive+邮件激活) | BUG-020/021/022 |
| 注册→邮箱验证→激活 | ✅ 可用,token+code 双因子 | BUG-023(URL 泄露)/024(无失败限制) |
| 密码哈希 | ✅ BCrypt-12(业界标准),还原码亦 BCrypt 哈希存库 | BUG-025(无长度上限,72 字节截断) |
| 登录(密码) | ✅ 可用:IP 令牌桶+账号锁定+审计落库 | BUG-030/031 |
| 登录→2FA→JWT | ✅ 可用:TOTP 防重放+还原码一次性 | BUG-032(无失败限制)/033(计数 key 失配) |
| JWT 刷新/登出 | ⚠️ 功能可用但**撤销语义失效** | BUG-040/041/042(高危) |
| 文章创建/发布 | ✅ 可用 | BUG-050(slug 遍历)/051(无分类 500) |
| 文章修改/删除 | ⚠️ 后台编辑器前端断链;后端可用但 MD 失同步 | BUG-010/011/052 |
| 浏览量/点赞统计 | ✅ 功能可用,但计数有竞态、可刷 | BUG-060/061/062 |
| CI/构建 | ✅ 后端测试+前端构建均真实执行 | BUG-070/071/072 |
| 静态产物部署 | ⚠️ 构建产物 dist/ 正常产出 | BUG-073(硬编码域名)/074(每次构建打后端) |

---

## 2. P0 — 致命(发布阻断,共 8 条)

### BUG-001 `[x]` 文章上传接口任意路径写文件 → RCE
- **严重度**:P0 致命(安全) | **类型**:路径遍历
- **位置**:`luomiblog-backend/.../service/impl/ArticleSyncServiceImpl.java:511-542`;`dto/UploadArticleRequest.java`(全字段零校验);`controller/ArticleSyncController.java:79-85`
- **证据**:`filename` 来自请求体,直接 `blogDir.resolve(filename)` 后 `Files.writeString(...)`。`../../x.md`、绝对路径均可;DTO 无校验注解、controller 无 `@Valid`;`filename=null` 时 `:517 filename.replace(".md","")` 直接 NPE。
- **影响**:Blogger 角色即有 `article:create` 权限(data.sql),博主账号被盗=全服务器写权限,可覆盖配置/写 crontab→RCE。
- **业界范式**(OWASP Path Traversal):①文件名白名单 `^[A-Za-z0-9._\-\u4e00-\u9fa5]+\.md$`(`@Pattern` + controller `@Valid`);②`Path resolved = blogDir.resolve(filename).normalize(); if (!resolved.startsWith(blogDir.toAbsolutePath())) throw 400;` 双保险;③拒绝绝对路径(resolve 遇绝对路径直接返回自身,必须先判 `filename.isAbsolute()` 或依赖②)。
- **验证**:单测断言 `filename="../../evil.md"` 抛 400;集成测试写 `../` 路径被拒。

### BUG-002 `[x]` 文章 slug 路径遍历(写/删任意 .md)
- **严重度**:P0 致命(安全) | **类型**:路径遍历
- **位置**:`ArticleServiceImpl.java:462`(`blogDir.resolve(article.getSlug()+".md")` 写)、`:480-493`(删)、`:104-109`(slug 仅查重不校验字符)
- **证据**:slug 由 `request.getSlug()` 显式传入时原样入库并直接拼文件路径;`generateSlug()` 只在未传时净化。
- **影响**:发布 slug=`../../evil` 的文章 → 在任意可写目录创建 .md;取消发布/删除时删除任意 .md。
- **业界范式**:slug 统一白名单 `^[a-z0-9]+(?:-[a-z0-9]+)*$`(WordPress/Ghost 同款),创建/更新/删除三处共用同一个 `resolveSafely()` 校验函数(与 BUG-001 同一工具方法)。
- **验证**:单测断言非法 slug 400。

### BUG-003 `[x]` 本地 markdown 文章正文渲染成 `[object Object]`
- **严重度**:P0 致命(功能主路径已坏) | **类型**:运行时 bug
- **位置**:`luomiblog-frontend/src/pages/article/[slug].astro:75-76`(`contentHtml = String(rendered)`)、`:250`(`set:html={contentHtml}`)
- **证据**:Astro 5 `render()` 返回 `{ Content, headings, remarkPluginFrontmatter }` 对象,`String()` 结果即 `"[object Object]"`。已实测 build 通过(该页是 SSR 不在构建期触发),运行时必然复现。
- **影响**:README 核心卖点"本地 MD 为基底"的路径,正文是一行乱码;仅 DB 分支文章有正文。
- **业界范式**(Astro 官方):`const { Content } = await render(localPost);` 模板中 `<Content />`;如需字符串(如消毒管道),改用 `experimental.contentCollectioIntrospection` 或统一走后端渲染 HTML 分支。
- **验证**:本地起后端+`astro dev`,打开一篇本地 content 文章,正文正常渲染。

### BUG-004 `[x]` 后台文章编辑器链路断裂(slug 解析错误 + 保存后 404)
- **严重度**:P0 致命(后台核心功能不可用) | **类型**:路由/状态 bug
- **位置**:`ArticleEditor.vue:1036-1044`(`pathname` 最后一段=`'edit'` 被当成 slug);`pages/admin/articles/edit.astro`(未把 query 参数传给组件);`ArticleEditor.vue:958-959`(保存后跳 `/admin/articles/{slug}` 路由不存在)
- **证据**:列表页跳 `/admin/articles/edit?slug=xxx` → 组件 `currentSlug='edit'` → 请求 `/api/articles/edit` → 404"文章不存在"。
- **修复范式**:`edit.astro` 里 `const slug = Astro.url.searchParams.get('slug')` 传 `<ArticleEditor client:load slug={slug} />`;组件优先读 `new URLSearchParams(location.search).get('slug')`;保存成功后跳回 `/admin/articles` 列表页(或建真正的 `[slug]` 详情路由)。
- **验证**:浏览器实操:列表→编辑→改标题→保存→重新打开,内容一致。

### BUG-005 `[x]` 前后端 DTO 契约断裂:带标签文章保存必失败
- **严重度**:P0 致命(数据写入路径) | **类型**:契约漂移
- **位置**:前端 `ArticleEditor.vue:913-933` 提交 `tags: string[]`;后端 `ArticleRequest.java` 只认 `tagIds: List<Long>` → Jackson 400。另 `types/api.ts` 的 `contentHtml/coverImage/top` 在后端 `ArticleResponse.java` 不存在(DB 文章封面永远不显示、消毒分支成死代码)。
- **修复范式**:**以后端 DTO 为单一事实源**。短期:前端改发 `tagIds: number[]`,`isTop` 字段名对齐;中期:引入 OpenAPI(springdoc 已有 swagger 依赖)→ `openapi-typescript` 生成前端类型,杜绝再次漂移。
- **验证**:新建文章选 2 个标签保存成功;类型检查通过。

### BUG-006 `[x]` `/api/tags/popular` 必然报错(JPQL 参数无法绑定)
- **严重度**:P0(公开端点 500) | **类型**:查询定义错误
- **位置**:`TagRepository.java:38-39`
- **证据**:`@Query` JPQL 无任何占位符,方法带 `int limit`;`Top` 前缀在 `@Query` 存在时失效 → 绑定失败(部分版本启动期即炸)。
- **修复范式**:`@Query(nativeQuery=true, "...ORDER BY t.article_count DESC LIMIT :limit")`;或改 `Pageable`;或派生命名 `findFirst10ByDeletedAtIsNullOrderByArticleCountDesc()`。
- **验证**:`curl /api/tags/popular` 返回 200;补一个 `@DataJpaTest`。

### BUG-007 `[x]` 安装向导 SQL 注入 + JDBC URL 注入
- **严重度**:P0 致命(安全,叠加 BUG-008 竞态可匿名触达) | **类型**:注入
- **位置**:`InstallServiceImpl.java:349-350`(库名拼进 `CREATE DATABASE`)、`:1050-1063`(host/database 直接拼 JDBC URL)
- **修复范式**:标识符白名单 `^[A-Za-z0-9_]{1,64}$`(DDL 无法参数化,只能白名单);JDBC URL 参数用 `URLEncoder` 或只允许 `host:port` 格式 `^[A-Za-z0-9.\-]+(:\d{1,5})?$`。
- **验证**:单测断言库名含反引号/host 含 `/` 时 400。

### BUG-008 `[x]` 安装向导匿名开放 + 重装验证通过即删锁(TOCTOU 竞态)
- **严重度**:P0 致命(安全) | **类型**:访问控制/竞态
- **位置**:`SecurityConfig.java:47`(permitAll)、`InstallController.java:322-330`(verify 成功即 `resetInstallation()` 删锁)、`InstallLockFilter.java:45-61`(锁丢失仅告警不拦截)
- **影响**:首次部署窗口/锁文件丢失期间,任何人可 execute-sql(可指向任意 MySQL 主机)、create-admin——**谁先 complete 谁当站长**;`check-environment` 匿名泄露系统信息。
- **业界范式**(WordPress/Ghost/Typecho 安装器通用做法):①安装向导仅允许 localhost 来源(反代传 `X-Real-IP` 校验)或**一次性安装令牌**(安装时输出到服务器控制台/需在服务器创建 seed 文件才能继续);②锁文件缺失时 fail-closed(禁用向导+要求人工介入),而非仅打日志;③verify-reinstall 通过后签发短期(15min)一次性管理员票据,后续 execute-sql/reinstall/complete 必须携带。
- **验证**:手动删除 install.lock 后访问 /api/install/check-environment 应 403;未携带票据调 execute-sql 应 401。

### BUG-009 `[x]` 安装流程假事务 + 连接泄漏 + 半初始化数据丢失
- **严重度**:P0 致命(数据安全) | **类型**:事务/资源管理
- **位置**:`InstallServiceImpl.java:1084`(连接传 `ScriptUtils` 后永不关闭,`DriverManagerDataSource` 每次新建物理连接)、`:832-856`(`@Transactional` 管不到自建数据源)、`:876-948`(先 DROP 全表再建,中途失败不可回滚)、`:516-557`(管理员写入容器主数据源,可能与向导目标库不一致)
- **修复范式**:同一 `Connection` 上 `autoCommit=false` 执行脚本(`ResourceDatabasePopulator` 支持传 Connection),统一 commit/rollback;try-with-resources 管连接;建管理员与建库校验同库(比较 `JdbcTemplate.datasource` 与目标 URL)。
- **验证**:故意给错 schema.sql 场景,断言旧数据完好、连接数不涨(MySQL `SHOW PROCESSLIST`)。

---

## 3. P1 — 高危(安全/数据正确性,共 12 条)

### BUG-010 `[x]` JSON-LD 存储型 XSS:博主可提权管理员
- **位置**:`luomiblog-frontend/src/layouts/Layout.astro:127`
- **证据**:`<script type="application/ld+json" set:html={JSON.stringify(jsonLd)} />`;title/summary 从不消毒,`JSON.stringify` 不转义 `<`。
- **影响**:title 埋 `</script><script>…</script>` → 任何访问者(含管理员)执行任意 JS → 偷 localStorage 里的管理员 JWT(`stores/user.ts:44-46`)→ 提权。
- **业界范式**(OWASP JSON Injection):`JSON.stringify(jsonLd).replace(/</g,'\\u003c').replace(/\u2028/g,'\\u2028').replace(/\u2029/g,'\\u2029')`,封装成 `safeJsonLd()` 工具函数;或对 jsonLd 字段先过一遍文本剥离。
- **验证**:单测断言输出无裸 `<`;手测:标题含 payload 的文章页无脚本执行。

### BUG-011 `[x]` 后台管理页存储型 XSS(innerHTML 拼接零转义)
- **位置**:`admin/users.astro:271-355`(nickname/username/email/avatarUrl)、`admin/index.astro:193-198`(activity 字段)、`admin/articles.astro:550`(title)
- **影响**:低权用户昵称设为 `<img onerror=…>` → 管理员打开用户管理即执行,可调同域 `/api/admin/*` → 权限提升。
- **修复范式**:三页统一引入项目里已有的 `escapeHtml()`(admin/settings.astro:268 已有实现,复制或抽公共 util);长期:这些列表页改用 Vue 岛渲染,文本插值天然转义。
- **验证**:注册恶意昵称账号 → 管理员打开用户页 → 无脚本执行。

### BUG-012 `[x]` refresh token 撤销失效:轮换黑名单 TTL 错误 + logout 不拉黑 refresh
- **位置**:`AuthServiceImpl.java:661`(刷新轮换时 `blacklistToken(tokenId, accessTokenExpiresSeconds())`——旧 refresh 有效期 7 天,黑名单只写 24h)、`:671-679`(logout 只拉黑 access,接口不接收 refresh)
- **影响**:第 2~7 天旧 refresh token 黑名单过期后**可再次刷新**(会话无法撤销);泄露的 refresh 在受害者登出后仍可用 7 天。
- **业界范式**(Auth0/Supabase 轮换模式):①黑名单 TTL = 被拉黑令牌**自身**的剩余有效期(`parseToken` 拿 exp-now);②logout 请求体同时携带 refresh_token 一并拉黑;③更彻底:引入用户级 `token_version`(users 表),登出/改密/封禁时 +1,JWT 带 `ver` 声明,filter 比对不一致即 401——一次查询吊销该用户全部会话。
- **验证**:单测:refresh 一次 → 用旧 refresh 再刷 → 拒绝;模拟 25h 后仍拒绝(把 TTL 断言为剩余有效期而非固定值)。

### BUG-013 `[x]` 辰汐 SSO 会话令牌无 jti → 登出不可吊销、30 天滑动续期
- **位置**:`JwtUtil.java:79-89`(generateChenxiSessionToken 无 `.id()`)、`:115-127`(slideToken 同)、`AuthServiceImpl.java:672-679`(logout 取 jti 得 null,黑名单键 `token:blacklist:null`)
- **修复范式**:签发与续期都写入随机 jti;logout 按该 jti 拉黑(TTL=剩余有效期,与 BUG-012 同修);建议默认有效期 30d→7d 并保留滑动续期。
- **验证**:签发的辰汐令牌解析含 jti;登出后该令牌请求 401。

### BUG-014 `[x]` trust-proxy=true 时取 XFF 首段 → 所有限流/锁定可绕过
- **位置**:`ClientIpResolver.java:23-28`(`split(",")[0]` 取最左=客户端可伪造段)
- **影响**:登录爆破、注册 5/h、评论/点赞限流、审计 IP 全部可被随机 XFF 绕过(nginx `proxy_add_x_forwarded_for` 是追加模式)。
- **业界范式**(nginx `real_ip` / Spring `ForwardedHeaderFilter` 思路):配置**可信代理列表**,从 XFF **最右向左**取第一个非可信地址;或文档强制要求反代设置 `X-Real-IP` 且后端只信任该头;至少在 README 标注"trust-proxy=true 必须确保反代覆写(而非追加)XFF"。
- **验证**:单测:`X-Forwarded-For: 1.2.3.4, 10.0.0.1`(10.0.0.1 为可信代理)应返回 10.0.0.1。

### BUG-015 `[x]` 限流/黑名单/锁定/TOTP 防重放共用 10000 条 Caffeine,可被挤库驱逐
- **位置**:`MemoryCacheServiceImpl.java:22-25`;消费方:`AuthServiceImpl:88,97`、`TotpReplayGuard:21`、各限流
- **影响**:匿名 `/api/articles/{id}/view` 的 visitorId 参与限流键(`ArticleStatsController.java:144-153`)且客户端可控 → 高频轮换 visitorId 挤出真实用户的锁定/黑名单/2FA 记录(已登出令牌复活、被锁账号解锁)。
- **业界范式**:①token 黑名单独立存储(DB 表 `revoked_tokens(jti, expires_at)` + 每日清理任务,重启不丢、容量无界可查);②各关注点分桶(至少 blacklist / ratelimit / 2fa 三个独立 Caffeine 实例);③visitorId 限流键加长度/熵约束,并叠加 IP 键。
- **验证**:压测制造 1 万个 view 键后,断言早前的锁定记录仍在。

### BUG-016 `[x]` 2FA 验证无失败限制(TOTP 与还原码均可爆破)
- **位置**:`AuthController.java:69-81`(两端点无 IP 频控)、`AuthServiceImpl.java:464-521`(错码不销毁挑战、无失败计数)
- **影响**:拿到密码的攻击者可对 300s 挑战窗口内持续猜 6 位码;8 位还原码同理。IP 限流只挂在 /login。
- **业界范式**:挑战对象加 `failCount`,≥5 次即删除挑战(要求重新密码登录);`/2fa/verify`、`/2fa/enroll` 各挂 IP 限流(如 10/min);失败尝试写 login_logs(当前失败不落审计)。
- **验证**:连错 5 次后挑战失效;login_logs 有失败记录。

### BUG-017 `[x]` 计数器全面读-改-写:并发丢更新 + 匿名无 visitorId 可无限刷赞
- **位置**:`ArticleStatsServiceImpl.java:80-81,119-121,142-143`、`LikeServiceImpl.java:34-46,63-64,95-96,113-114`、`CommentServiceImpl.java:49-54,159-173`;`LikeRequest.java:17`(visitorId 无 `@NotNull`)
- **证据**:`article.setXxx(xxx+1); save()`;`existsByArticleIdAndVisitorId(id, null)` 生成 `visitor_id=NULL` 恒 false;MySQL 唯一索引对 NULL 不去重 → 每次点赞都插入成功+计数+1。
- **修复范式**:**原子 UPDATE**(业界标准):`@Modifying @Query("UPDATE Article a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")`;去重插入用 `INSERT IGNORE` 语义(捕获 `DataIntegrityViolationException` 返回幂等结果);visitorId 加 `@NotBlank @Size(min:8,max:64)`;高并发站点可再加 Redis INCR + 定时落库(当前规模原子 UPDATE 已够)。
- **验证**:并发 50 线程同文章点赞,断言计数与行数一致;匿名不带 visitorId 请求 400。

### BUG-018 `[x]` 业务异常全兜底 500(40+ 处 RuntimeException)
- **位置**:`GlobalExceptionHandler.java:94-99` 兜底;调用方如 `ArticleServiceImpl.java:97,100,106,154,157`、`UserServiceImpl.java:68,72,75`(旧密码错误也 500!)、`CommentServiceImpl`、`CategoryServiceImpl`、`TagServiceImpl`、`LikeServiceImpl`
- **修复范式**:统一异常语义(REST 标准):资源不存在→`BusinessException(404)`、无权→403、参数→400、冲突→409;保留 Exception 兜底给真异常。建议加 ArchUnit 单测禁止 service 层直接 `new RuntimeException(`。
- **验证**:全仓 grep `throw new RuntimeException` 归零(或白名单);API 错误码回归清单。

### BUG-019 `[w→ ]` 部分修复: 改 slug 残留已修, 软删文件/afterCommit 未做 | 原条目: MD 文件与 DB 状态系统性失同步
- **位置**:`ArticleServiceImpl.java:104-108,140`(改 slug 后按**新** slug 删文件,旧 `<old-slug>.md` 永久残留且公开可见)、`:150-162`(软删除不删 MD 文件)、`:415-475`(事务内文件 IO,回滚后孤儿文件)
- **修复范式**:①改 slug 前先记 oldSlug,状态/文件操作一律用 oldSlug 删旧、newSlug 写新;②deleteArticle/archive 同步 `deleteMarkdownFile`;③文件操作移入 `TransactionSynchronization.afterCommit()`(Spring 标准范式:事务成功后执行副作用),失败补偿任务(每日对账 DB published 文章 ↔ 磁盘文件)。
- **验证**:改 slug 后旧文件不存在;软删除后公开页 404 且文件消失。

---

## 4. P2 — 中(正确性/性能/架构,共 22 条)

### 后端正确性

- **BUG-020 `[ ]` GET /api/comments/user 匿名必 500**:`CommentController.java:45-52` 匿名时 `userPrincipal` 为 null 直接 `.getId()`;`SecurityConfig.java:52` 却 permitAll。修复:方法加 `@PreAuthorize("isAuthenticated()")`(同文件 myFavorites 已是此写法)。
- **BUG-021 `[ ]` 发布无分类文章 500**:`ArticleServiceImpl.java:423` `findById(null categoryId)` 抛 `InvalidDataAccessApiUsageException`——注意它**不是 IOException,不会被 `:471` 的 catch 捕获**,直接炸穿事务。修复:`Long cid = article.getCategoryId();` 判空跳过查询用"未分类";`ArticleRequest.categoryId` 加校验或后端兜底 null→未分类 id。
- **BUG-022 `[ ]` 注册事务内同步发邮件**:`AuthServiceImpl.java:171,211-219` SMTP 阻塞最长 ~25s,占 Hikari 连接(池仅 10),SMTP 抖动拖垮注册。业界范式:`@TransactionalEventListener(phase=AFTER_COMMIT)` + `@Async` 线程池发送;发送失败改异步重试+站点内提示(不再回滚账号——当前回滚逻辑导致"邮箱瞬断=注册失败")。
- **BUG-023 `[ ]` 邮箱验证 token+code 同置于 URL query**:链接含 `?verify-token=…&verify-code=…`(`AuthServiceImpl.java:198`),落入浏览器历史/邮件网关日志。范式:magic link 用 path 参数 + 短时效(已有 30min✓),code 走页面输入;至少 token 用 path、code 不进 URL。
- **BUG-024 `[ ]` 邮箱验证码无失败限制**:verify 端点(`AuthController.java:49-52`)无频控、错码不销毁 token。范式:同 BUG-016(failCount≥5 销毁)。
- **BUG-025 `[ ]` 密码无最大长度**:`RegisterRequest` password 无 `@Size(max=72)`;BCrypt 只处理前 72 字节,超长部分静默截断。范式:`@Size(min=8, max=72)`(NIST:至少允许 64 字符;72 是 BCrypt 硬限制,截断前显式拒绝优于静默)。
- **BUG-026 `[ ]` 2FA 成功后清失败计数 key 失配(本轮链路走查新发现)**:`AuthServiceImpl.java:512` 清 `username:ip`,而 `:287` 登录失败记录的是 `usernameOrEmail:ip`——**用邮箱登录的用户 2FA 成功后清不掉失败计数**,反复失败后会被误锁。修复:统一用 `user.getUsername()+":"+ip` 作为锁定 key(在 authenticate 成功、拿到实体后记录)。
- **BUG-027 `[ ]` 评论模块五连**:`CommentServiceImpl.java:31-57` ①parentId 可跨文章/指向未审核评论;②不校验文章存在与 allowComments 开关(FK 异常 500);③replyCount 读-改-写竞态;④`article.commentCount` 全项目从未维护(前端展示永远 0);⑤树构建把父评论软删后的整棵子树静默丢弃。修复:parent 校验同文章且 approved;评论数用原子 UPDATE 维护(approve/delete 时);孤儿挂根或"已删除"占位(walk 评论树通用范式)。
- **BUG-028 `[ ]` 后台分类筛选静默失效**:`ArticleRepository.java:39-47` JPQL 无 `category` 参数。修复:补 `AND (:categoryId IS NULL OR a.categoryId = :categoryId)`(参数改 Long);同时把 `a.content LIKE` 改为仅 title LIKE 或 MATCH AGAINST(见 BUG-063)。
- **BUG-029 `[ ]` admin 也不能修改他人文章**:`ArticleServiceImpl.java:99-101,156-158` 一律强校验属主。范式(Ghost/WordPress):ADMIN 角色跳过属主校验(或仅 DRAFT 限制属主);保留 Blogger 严格属主。
- **BUG-030 `[ ]` 登录 catch-all 泄露内部异常信息**:`AuthServiceImpl.java:413-416` `INTERNAL_ERROR, e.getMessage()`;`InstallController` 多处同。范式:对外统一文案,细节入日志(带 traceId)。
- **BUG-031 `[ ]` 用户名/邮箱枚举**:`AuthServiceImpl.java:313` 登录返回 `USER_NOT_FOUND`(可枚举存在性,且不计入失败锁定);resend 返回"该邮箱未注册待验证账号"(`:269`)。范式:登录失败统一 `BAD_CREDENTIALS`(注册/重发场景酌情模糊化,注意与 UX 平衡)。
- **BUG-032 `[ ]` 分页 size 无上限**:`ArticleController.java:27-33` 等 4+ 处;`app.pagination.max-page-size:100` 配置零引用。范式:统一 `PageRequest` 工厂 clamp `1<=size<=max`;search 端点加分页。
- **BUG-033 `[ ]` 改密/重置密码后旧会话不失效**:`UserServiceImpl.java:66-80`、`AdminUserServiceImpl.java:191-199`。范式:token_version 机制(与 BUG-012③ 同一方案,一次实现两处受益)。
- **BUG-034 `[ ]` CORS 少暴露 X-New-Token**:`WebConfig.java:27-31` vs `JwtAuthenticationFilter.java:83-84` 注释宣称已暴露——跨域部署时辰汐滑动续期头前端读不到,续期静默失效。修复:`exposedHeaders("X-New-Token")`;顺带评估 `allowCredentials(true)` 是否必要(纯 Authorization 头方案可关)。
- **BUG-035 `[ ]` 生产默认配置不安全**:`application.yml:25` show-sql=true、`:44` security DEBUG、`:24` ddl-auto:update、无 prod profile。范式:`application-prod.yml`(INFO、show-sql=false、ddl-auto=validate)+ README 指引;实体↔schema 漂移(Comment 实体列在 schema.sql 不存在)→ 引入 **Flyway**(现有 V1-V4 手工脚本直接改造为迁移版本),`ddl-auto` 退化为 validate。

### 性能(2核4G 基准)

- **BUG-040 `[ ]` 列表接口返回全文 LONGTEXT**:`ArticleServiceImpl.java:342` convertToResponse 带 content,列表 4 个方法共用;前端拿全文只为算阅读时长(`articles/index.astro:44`);schema 的 `word_count` 列闲置。范式:拆 `ArticleListItemDTO`(投影接口或 JPQL constructor expression),详情页单独取全文;阅读时长用 word_count 落库值。
- **BUG-041 `[ ]` N+1 遍地**:每篇文章 3 次额外查询(`convertToResponse` → 分类/标签/作者),每页 31 SQL;`AdminUserServiceImpl.java:272-283` 为 count 拉全量文章实体(含全文);`CommentServiceImpl:187` 每条评论查一次用户。范式:列表查询 `JOIN FETCH`/`@EntityGraph` 一次取齐,或投影 DTO;count 用 `countByAuthorId(Long)` 派生查询;评论用户 `findAllById` 批量。
- **BUG-042 `[ ]` 单次 PV 打 8 条 SQL、加载全文两次**:`ArticleStatsController.java:52-55` recordView 后又调 getArticleStats 再 findById。范式:recordView 返回计数结果,controller 不再二次查询;recordView 本身按 BUG-017 原子化后降至 2-3 条 SQL;叠加 `JwtAuthenticationFilter.java:61` 每请求回库查用户 → 权限已入 JWT claims,可信任 claims 或 60s 本地缓存。
- **BUG-043 `[ ]` SSR 双层零缓存**:`SiteConfigServiceImpl` 每请求 6 条 SELECT 无缓存(`spring.cache` 配了 caffeine 没用上);`Layout.astro:37-52` 每个 SSR 请求同步 fetch site/config 阻塞 TTFB;`[slug].astro:28` 每请求 `getCollection('blog')` 全量遍历只为取相关文章。范式:site/config 加 `@Cacheable`(TTL 5-10min,配置变更时 evict);Astro 侧模块级 TTL 缓存;相关文章构建期预算或按分类查询。
- **BUG-044 `[ ]` Element Plus 全量引入 1.1MB**:`app.ts` 全量注册+全部图标;`astro.config.mjs:36-38` 把 chunk 警告调到 1000 掩盖问题;静态页每岛都背。范式:`unplugin-vue-components` + `ElementPlusResolver` 按需引入;图标按需 import;`manualChunks` 把 EP 拆异步 chunk;`chunkSizeWarningLimit` 回 500。
- **BUG-045 `[ ]` 每日清理任务伪分批**:`UserBehaviorCleanupTask.java:39-55` 注释称分批实则 `@Transactional` 包全程(5 万行);`LoginLogRepository` 派生 delete 先全量加载再逐行删;两任务同一 cron 03:30 且调度器单线程。范式:`@Modifying @Query("DELETE … WHERE createdAt < :c")` + 每批 2000 条独立事务循环;cron 错开;`SchedulingConfig` 配置 `ThreadPoolTaskScheduler`。
- **BUG-046 `[ ]` 后台搜索全表扫 LONGTEXT**:`ArticleRepository.java:39-47` `content LIKE %…%` 无法走索引(schema 的 ngram FULLTEXT 只服务 MATCH)。范式:搜索仅 title LIKE(或 MATCH AGAINST ft_article_title);补 `user_behavior(article_id, behavior_type, created_at)` 复合索引(增长最快的表)。
- **BUG-047 `[ ]` 远程调用零超时**:`ChenxiAuthServiceImpl.java:146-148`、`LicenseClient` 无 connect/read timeout(JDK 默认无限等)→ 通行证抖动时 Tomcat 线程耗尽。范式:`ClientHttpRequestFactorySettings` 统一 3-5s 超时 + 有限重试。
- **BUG-048 `[ ]` 限流桶满即整体清空**:`InstallServiceImpl.java:1150-1153` 满万条 `clear()` 重置所有人。范式:过期键惰性清理(读时判过期),或分片轮转清最旧。

### 前端/架构

- **BUG-050 `[ ]` 一次后端抖动=全员强制登出**:`api.ts:107-114` + `useBackendStatus.ts:154-168`——单次 500/健康检查超时即 `clearAuthState()`。范式(标准 SPA 会话处理):仅"连续 N 次不可达"标记后端离线;清凭据前先 `/api/auth/me` 验证 token 是否真的失效;401 才清,5xx 保留。
- **BUG-051 `[ ]` admin 守卫纯前端**:`AdminLayout.astro:340-389` 读 localStorage role 显示遮罩;`middleware.ts` 只探活不验签。缓解:后端 `@PreAuthorize` 已兜底(已核实✓)。范式:中间件验 JWT 签名+角色 claim(至少存在性),失败 302 登录;401 统一清态跳转(`apiCall` 现仅 throw)。
- **BUG-052 `[ ]` 假功能成片(诚信风险)**:`AIAssistant.vue:8-17`(setTimeout 假回复,零 API)、`SearchBox.vue:7-12`(仅 console.log)、`admin/comments.astro`/`announcements.astro`/`mail.astro`(硬编码假数据零脚本)、`user/*` 多页假壳、i18n 仅 locales 声明无任何翻译资源。范式:未实现功能 UI 标注 "WIP" 或移除入口;README 把"已实现/规划中"分开列(见 BUG-072)。
- **BUG-053 `[ ]` 三套认证状态/多套 API 封装并存**:`stores/user.ts` 模块级 ref 冒充 Pinia+每岛重复 `initAuth` 挂监听器;admin 页裸 fetch 不走 `utils/api.ts`;`HealthCheckResponse` 两处定义;token 双读 sessionStorage 死分支。范式:真 Pinia(依赖已在)+ `if(!initialized)` 守卫;全部页面收敛到统一 api 封装;删除 `useApi.ts`/`ErrorBoundary.vue`/`AuthGuard.vue` 死代码与 `@pinia/nuxt`、`vue-router`、`lucide-vue-next` 死依赖。
- **BUG-054 `[ ]` 竞态:列表/筛选快速切换旧响应覆盖新数据**:`admin/users.astro:238-279`、`user/favorites.astro:154-177`、`user/subscription.astro:602-628` 无 AbortController/序号。范式:`request()` 支持 AbortSignal,组件 cleanup 时 abort;或递增 sequence token 丢弃过期响应。
- **BUG-055 `[ ]` 内存泄漏群**:多处 addEventListener 无对应 remove(`UserAuth.vue:48-51`、`BackendStatusBanner.vue:126-174` 复活的轮询、`ProfileCard.vue` interval);KaTeX 动态加载竞态(`ArticleEditor.vue:549-578` onload 前调渲染)。范式:Vue `onUnmounted` 对称清理;script load 用 promise 化 onload。
- **BUG-056 `[ ]` mermaid loose 模式 XSS 面**:`[slug].astro:1229`(`securityLevel:'loose'`)、`:1240`(输出 SVG 未消毒直进 innerHTML)——绕过正文 DOMPurify。范式:`securityLevel:'strict'` 或渲染产物再过 DOMPurify(svg 白名单 profile)。
- **BUG-057 `[ ]` 安装向导 v-html SVG + dangerouslyUseHTMLString**:`InstallWizard.vue:1464,183-185,269-409`。范式:预览改 `<img src="data:image/svg+xml,…">`(img 上下文 SVG 不执行脚本);HTML 字符串内容一律转义。
- **BUG-058 `[ ]` TOTP 防重放非原子**:`TotpReplayGuard.java:39-43` get→set 并发双放行。范式:`ConcurrentHashMap.compute()` 原子化(或 Caffeine.asMap().compute)。
- **BUG-059 `[ ]` 同步锁非原子+裸线程**:`ArticleSyncServiceImpl.java:423-430` check-then-set;`ArticleSyncController.java:23` `new Thread` 无池无上限。范式:`ConcurrentHashMap.putIfAbsent` 语义占锁 + finally 释放;`@Async`/`TaskExecutor` 线程池(命名、有界)。
- **BUG-060 `[ ]` 软删除过滤不一致**:`CategoryRepository.java:14`、`TagRepository.java:16`、`ArticleRepository.java:17` findBySlug 不过滤 deletedAt;`DashboardServiceImpl.java:44-63` count 口径含软删。范式:公开路径统一 `findBySlugAndDeletedAtIsNull`;仪表盘口径统一并注释说明。
- **BUG-061 `[ ]` 参数校验异常处理器会 ClassCastException**:`GlobalExceptionHandler.java:74-86` 对类级约束强转 `FieldError`;缺 `HttpMessageNotReadableException`(坏 JSON→500)等映射。范式:`instanceof FieldError` 判断 + 补常见异常→400 映射表。
- **BUG-062 `[ ]` 最近动态排序恒 0**:`DashboardServiceImpl.java:146-149` `sort((a,b)->0)` 自认简化。范式:按时间戳排序;顺带修评论循环内 findById N+1(`:129`)。

---

## 5. P3 — 低(工程化/文档/杂项,共 15 条)

- **BUG-070 `[ ]` 测试覆盖:全后端仅 11 个 TOTP 单测**(已实测通过)。认证/安装锁/限流/SSO/JWT 全部零测试=README 宣称的安全体系无回归保护。范式(测试金字塔):P0 修复后立即为 BUG-001/002/012/014/016/017 补单测;集成测试用 **Testcontainers-MySQL**(本机 Docker 已可用);前端至少 vitest 覆盖 `sanitizeHtml.ts` 与 api.ts 错误分类;CI 加 JaCoCo 阈值(如核心包 60%)。
- **BUG-071 `[ ]` CI 质量门禁缺失**:`.github/workflows/ci.yml` frontend 仅 build;`npm run lint` 是死脚本(eslint 不在 devDependencies、无配置文件);`typecheck` 可用但未接入。修复:CI 加 `npm run typecheck`;eslint 三选一(补齐依赖+flat config / 删脚本);加 `permissions: contents: read`、`concurrency` 组、`timeout-minutes`;可选 SHA 固定 actions。
- **BUG-072 `[ ]` README 诚信问题**:AI RAG(假实现)、中/英/日三语(无实现)、36 张表(实际 39)与代码不符;CHANGELOG "重写 git 历史"不实(`a0f101e` 的公开默认 JWT secret 仍在 main 可达历史;用过默认密钥上线的站必须轮换)。修复:README 特性表加"已实现/规划中"列;CHANGELOG 修正表述+显著提示轮换义务。
- **BUG-073 `[ ]` 版本号三处脱节+零 tag**:pom 0.2.0 / CHANGELOG 0.4.0 / package.json 0.0.1;README 部署命令引用 0.2.0.jar;README 徽章 Spring Boot 3.4.1 实为 3.4.13。修复:统一 0.4.0,打 `v0.4.0` tag;CI 加 tag 触发的 jar 构建 release。
- **BUG-074 `[ ]` scripts/reset-install.ps1 删旧路径**:删 `install.lock`(根)但现行默认 `config/install.lock`(`application.yml:58`),FAQ 引导用户用它恢复会失败。修复:改删 `config\install.lock` 并兼容旧路径。
- **BUG-075 `[ ]` qodana.yaml 死配置**:无 workflow 引用、failureConditions 全注释、projectJDK 25 vs pom 21。修复:接入或删除;同时加 `.github/dependabot.yml`(maven+npm+actions)与 CodeQL(项目含鉴权/注入面,值得开)。
- **BUG-076 `[ ]` 安全配置键未文档化/未入 yml**:`app.security.trust-proxy` 只在代码 `@Value` 默认 false,application.yml 无该键、README 环境变量表未列;CORS origins 硬编码 6 个 localhost 无环境变量占位。修复:`app.security.trust-proxy: ${APP_SECURITY_TRUST_PROXY:false}` 入 yml;CORS 加 `APP_CORS_ALLOWED_ORIGINS`;README 变量表补全。
- **BUG-077 `[ ]` astro.config.mjs 硬编码生产域名**:`site:'https://luomiblog.com'`——他人自建 sitemap 指向原作者域名。修复:`site: process.env.PUBLIC_SITE_URL || 'http://localhost:4321'`。
- **BUG-078 `[ ]` 依赖健康**:tomcat.version=11.0.21 跨主版本 override Boot 3.4(兼容风险);okhttp 4.9.2 旧;`@pinia/nuxt` Nuxt 专用死依赖。修复:回退 Boot 托管 Tomcat 版本观察;okhttp 升 4.12.x;删死依赖。
- **BUG-079 `[ ]` 构建期每页打后端拿 favicon**:build 日志显示每个静态页 "[Layout] 后端不可用,使用默认 favicon"——构建环境依赖后端在线(在线时 favicon 烧进静态页,离线时静默降级,行为不一致)。修复:favicon 在构建期从 DB 拉一次存构建缓存,或运行时由 SSR Layout 统一注入并加缓存头。
- **BUG-080 `[ ]` feed/RSS 构建期快照与 SSR 实时列表不一致**:新文章即时出现在 /articles 但 RSS 要等下次构建。修复:rss.xml/atom.xml 改 `prerender=false` 动态输出。
- **BUG-081 `[ ]` 杂项**:`/api/health` 匿名暴露 DB 错误详情/安装状态(`HealthServiceImpl.java:57-81`);`/api/install/lock-integrity` 匿名返回锁哈希前缀;`/api/auth/login-security` 泄露当前 IP 剩余尝试次数(攻击者可探测锁定状态);swagger/actuator 生产暴露(`SecurityConfig.java:53-54`);`meta name="api-base-url"` 向访客暴露后端源;`robots.txt` Disallow 指向前端域不存在的 `/api/`;中文阅读时长按 `\s+` 分词严重失真(`[slug].astro:39-40`);`InstallController.executeReinstall` 对 port 强转 Integer(前端传字符串即 500);verifyReinstallPermission 全表加载逐个 BCrypt(`InstallServiceImpl.java:754-766`,应先按角色 SQL 过滤);邮箱验证 HTML 内插 username/nickname 未转义(`AuthServiceImpl.java:199-209`)。
- **BUG-082 `[ ]` 仓库卫生**:本地未跟踪的 `fffflog.log`、`dist/`、`.env` 建议清理;`tools/`、`database/` 空目录删除;`.idea/dataSources.xml` 含本地 DB 连接信息,**严禁 `git add -f`**;历史 blob 中旧 install.lock/custom-application.yml 可达(低危,可接受)。
- **BUG-083 `[ ]` Dashboard count 口径**:`DashboardServiceImpl.java:44-63` `count()` 含软删数据,与 countActiveUsers 口径打架。修复:统一加 `deletedAt IS NULL`。
- **BUG-084 `[ ]` InstallWizard 字符串模板写 Vue 组件**:`:271` HTML 字符串里 `<el-icon><Warning/></el-icon>` 不会被编译,图标不显示(纯 UI bug)。
- **BUG-085 `[ ]` getDefaultAuthor 兜底 authorId=1**:`ArticleSyncServiceImpl.java:353` 用户 1 不存在时 FK 500。修复:兜底改查最小 ADMIN。

---

## 6. 复核后排除的"伪 BUG"(不要修)

| 项 | 排除理由 |
|---|---|
| `astro check` 报 `AdminLayout.astro:269`、`register.astro:881` 的 `apiBaseUrl` 未定义 | `define:vars={{ apiBaseUrl: API_BASE_URL }}` 运行时注入,实测确认是 astro check 的 hint 级误报,代码正确 |
| `createMarkdownFile` 丢标签的怀疑 | `:428` 标签从 DB(`tagRepository.findByArticleId`)读取而非请求参数,更新后调用顺序正确,无丢失 |
| 浏览量 24h 去重"内存 Map 会 OOM"的猜测 | 实际基于 `user_behavior` 表查询,无 OOM 风险(真正的代价是每 PV 一次 DB 写,见 BUG-042) |
| Caffeine maximumSize=10000 "内存无限增长" | 有上界不会 OOM;问题在**安全状态被驱逐**(BUG-015),内存角度安全 |
| SSR 页 fetch 后端"包括 localhost 硬编码" | `PUBLIC_API_URL` 环境变量机制存在且有 `.env.production.example` 提示,仅缺构建期校验(并入 BUG-077 处理) |

---

## 7. 修复路线图(建议顺序与依赖)

### 第一批:安全止血(P0 安全 + P1 安全,约 2-3 人日)
1. BUG-001/002(同一 `PathSanitizer` 工具函数,一次修两处)+ 单测
2. BUG-010(JSON-LD 转义,一行)+ BUG-011(三页 escapeHtml)
3. BUG-007 + BUG-008(安装向导:白名单 + 一次性票据/fail-closed)
4. BUG-012 + BUG-013 + BUG-033(token 黑名单 TTL 修正 + logout 拉黑 refresh + 辰汐 jti + token_version——**一次重构同时解决三个**)
5. BUG-014(XFF 右取)+ BUG-015(黑名单独立存储)
6. BUG-016 + BUG-024(2FA/邮箱验证失败限制,同一 challenge 对象加 failCount)

### 第二批:功能修复(P0 功能 + P2 正确性,约 2-3 人日)
7. BUG-003(`[object Object]`)+ BUG-004(编辑器)+ BUG-005(DTO 对齐)——前端三连,修完后台才算能用
8. BUG-006(tags/popular)+ BUG-018(异常语义统一,可配合 ArchUnit)
9. BUG-017 + BUG-021 + BUG-027 + BUG-028 + BUG-019(后端正确性批)
10. BUG-050(误杀登出)+ BUG-026(2FA 计数 key)

### 第三批:性能(约 2 人日)
11. BUG-040 + BUG-041 + BUG-042(DTO 拆分 + JOIN FETCH + 原子计数,一揽子改 convertToResponse)
12. BUG-043 + BUG-044(两处缓存 + EP 按需)
13. BUG-045/046/047/048

### 第四批:工程化(约 2 人日)
14. BUG-070(为第一、二批的修复**同步补测试**,Testcontainers)+ BUG-071(CI 门禁:typecheck 先接入,eslint 补齐)
15. BUG-072/073/074/075/076/077(文档与配置一致性)
16. BUG-052/053(假功能标注或下线、状态层收敛)——可与前端重构合并做
17. BUG-035(Flyway + prod profile)——建议在下一功能版本前做,越晚成本越高

### 修完后必做的回归
- [ ] `mvn test` 全绿 + 新增测试全绿
- [ ] `npm run typecheck && npm run build` 通过
- [ ] 手工回归五链路:注册(开/关邮箱验证两种)→ 登录(含 2FA 开/关)→ 发文章(带/不带分类、带标签)→ 改 slug/删除后检查 MD 文件与前台可见性 → 浏览量/点赞并发正确性
- [ ] gitleaks 扫描 + `git log` 复查无新密钥

---

## 8. 修复进度记录(2026-10-05 第一批修复完成)

### 已完成提交(Wave1-6, 均经 mvn test + 前端 build 验证)
| 提交 | 内容 | 对应 BUG |
|---|---|---|
| 7299385 | 路径遍历双修+JSON-LD XSS+admin XSS+tags/popular | 001/002/006/010/011/019部分/021 |
| d2844a3 | 安装向导票据(TOCTOU)+DB配置服务层白名单 | 007/008 |
| 0a0b308 | token 撤销链路(黑名单TTL+refresh吊销+辰汐jti+前端真登出) | 012/013 |
| 714ebfd | XFF 可信代理解析+2FA/邮箱验证失败限制+IP频控 | 014/016/024 |
| ea0c709 | 前端主路径三连([object Object]/编辑器断链/DTO契约) | 003/004/005 |
| d111040 | 安装事务/连接泄漏+计数器全面原子化+commentCount 维护 | 009/017/027部分/026 |

### Docker MySQL 8 集成冒烟实测结果(2026-10-05)
| 链路 | 结果 |
|---|---|
| 安装向导: test-database→execute-sql→create-admin→complete→锁 403 | ✅ 全通 |
| 注册(邮箱验证关=直发令牌)→登录→/me | ✅ 全通 |
| 发文(带 tagIds)→MD 落盘→article_tags 关联→详情返回标签 | ✅ 全通 |
| 浏览量 24h 去重(同 visitor 2 次=1, 换 visitor=2) | ✅ 实测正确 |
| 点赞原子开关(1→0)+匿名缺 visitorId 被拒 | ✅ 实测正确 |
| GET /api/tags/popular | ✅ 200(修复前必 500) |
| /2fa/verify 频控: 第 11 发精准 429 | ✅ 实测生效 |
| logout 吊销 refresh 后旧令牌 | ✅ 401(修复前可复活) |
| 重装窗口: 无票据 403 / 带票据 200 / complete 后锁恢复 | ✅ 实测生效 |

### 实测新发现
- **BUG-086 `[ ]`(P1) 双源建表竞争**:`ddl-auto: update` 使 Hibernate 在应用启动时抢先建表(Hibernate 命名的唯一键, schema.sql 的默认值/注释/索引从不生效), 安装向导 execute-sql 的 `CREATE TABLE IF NOT EXISTS` 全部空转。本轮已修 data.sql 侧(BUG-087), **根治需 BUG-035 的 Flyway 化**(安装前禁用 Hibernate 建表, schema.sql 为唯一事实源)。AstrNest 同样存在此问题(其 InstallGuardFilter+SchemaAlignmentRunner 是另一套兜底)。
- **BUG-087 `[x]`(P1) data.sql 零日期**: 种子 INSERT 未提供 created_at/updated_at, 落进无 DEFAULT 的 Hibernate 版表 → '0000-00-00', 依赖 JDBC `zeroDateTimeBehavior=convertToNull` 掩码, 换连接配置即全线崩溃(登录/注册全挂)。已修: 全部种子行显式 NOW(), role_permissions 改 INSERT IGNORE 幂等。**部署过旧版本的站需要一次性数据修复 SQL。**
- **BUG-088 `[ ]`(P2) POST /api/tags 建标签 500**(冒烟实测): 请求 `{"name","slug"}` 返回 500, 需查 TagController 入参与 TagService 是否要求必填字段/角色; 前端编辑器"新标签自动创建"依赖此接口。
- BUG-061 获实测佐证: 非法 JSON(Git Bash GBK 编码中文)→500 而非 400。
- 媒体上传链路: 后端无独立媒体上传端点(仅 favicon/站点配置), 前端 attachment 管理页是假壳(BUG-052 已列), 无需单测。

---

## 9. 修复进度记录(2026-10-05 第二批: Wave8-11)

| 提交 | 内容 | 对应 BUG |
|---|---|---|
| 77d081f | 异常语义统一 38 处 + createTag usage_count + 异常映射补全 | 018/088/061 |
| 4bb346f | token 黑名单 DB 化(revoked_tokens 表+清理任务+双消费点切换) | 015 |
| edffb7c | 列表 DTO 拆分+批量装配+统计轻量化 | 040/041/042 |
| (本次) | CI 门禁: astro check 进 CI + permissions/concurrency/timeout + 死 lint 脚本移除 | 071(部分) |

第二批后剩余高价值项: BUG-019 余量(MD afterCommit)、BUG-022(注册邮件异步)、BUG-028/029(评论校验/ADMIN 越权边界)、BUG-033(token_version 改密踢会话)、BUG-035(Flyway, 含 BUG-086 根治)、BUG-043/044(缓存+EP 按需)、BUG-050(误杀登出)、BUG-051(admin 中间件验签)、BUG-052/053(假功能/状态收敛)、BUG-088 关联(前端新标签自动建)。
