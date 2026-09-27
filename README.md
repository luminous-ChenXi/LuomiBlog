# LuomiBlog

<div align="center">

**AI-Powered Knowledge Base Blog System | AI 知识库博客系统**

<p align="center">
  <a href="https://github.com/vuejs/core">
    <img src="https://img.shields.io/badge/vue-3.5.29-brightgreen.svg?style=flat-square&logo=vue.js" alt="vue">
  </a>
  <a href="https://github.com/element-plus/element-plus">
    <img src="https://img.shields.io/badge/element--plus-2.13.5-brightgreen.svg?style=flat-square&logo=element" alt="element-plus">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/spring--boot-3.4.1-brightgreen.svg?style=flat-square&logo=spring" alt="spring-boot">
  </a>
  <a href="https://astro.build/">
    <img src="https://img.shields.io/badge/astro-5.17.1-brightgreen.svg?style=flat-square&logo=astro" alt="astro">
  </a>
  <a href="https://github.com/luminous-ChenXi/LuomiBlog/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/license-GPL--3.0--with--Additional--Terms--(Non--Commercial)-blue.svg?style=flat-square" alt="license">
  </a>
  <a href="https://github.com/luminous-ChenXi/LuomiBlog/releases">
    <img src="https://img.shields.io/github/release/luminous-ChenXi/LuomiBlog.svg?style=flat-square" alt="GitHub release">
  </a>
  <a href="https://coderabbit.ai">
    <img src="https://img.shields.io/coderabbit/prs/github/luminous-ChenXi/LuomiBlog?labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews" alt="CodeRabbit Reviews">
  </a>
</p>

<p align="center">
  <b>简体中文</b> · <a href="./README_EN.md">English</a>
</p>

</div>

> 🪺 **辰汐生态（Chenxi Ecosystem）** 成员项目 —— 与 [LuomiNest](https://github.com/LuminousCX/LuomiNest)（桌面 AI 陪伴）、[AstrNest](https://github.com/luminous-ChenXi/AstrNest)（图床/媒体管理）、Teachenxi（学习陪伴 App）同属辰汐生态。本项目的角色：静态优先的 AI 知识库博客系统。

## 目录

- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速开始（本地开发）](#快速开始本地开发)
- [五分钟部署（生产上线）](#五分钟部署生产上线)
- [Nginx 反代样例](#nginx-反代样例)
- [从源码到上线：CI/CD](#从源码到上线cicd)
- [安装安全机制](#安装安全机制)
- [安装后配置：站长安全开关](#安装后配置站长安全开关)
- [常见问题 FAQ](#常见问题-faq)

## 核心特性

- **可视化安装向导** - 像 WordPress 一样，通过 Web 界面完成安装：环境自检（Java/后端服务/MySQL 驱动/磁盘可写/SMTP）→ 数据库配置 → 初始化 → 站点配置 → SMTP → 网站图标 → 管理员创建 → 完成，共八步；带四重防重装与 reset 恢复通道
- **静态优先，岛屿架构** - 充分发挥 Astro 的静态站点生成优势，90%以上内容预渲染为 HTML，仅少数页面 SSR
- **互动能力已打通** - 点赞（登录/游客双轨防重）、收藏（个人收藏页）、浏览量 24h 去重；评论后端 API 已就绪（前端评论区开发中）
- **站长安全开关** - 注册邮箱验证（依赖 SMTP）、登录两步验证（TOTP：绑定 + 10 个一次性还原码 + 管理员可重置）
- **AI 原生增强** - 基于阿里云百炼的 Agentic RAG 问答能力
- **程序员友好** - 支持 Git 写作习惯，MD/MDX 原生支持
- **四角色权限** - 访客/会员/博主/管理员完善的 RBAC 权限体系
- **多语言支持** - 中/英/日三语国际化
- **轻量高效** - 适配 2核4G 轻量服务器

## 技术栈

### 前端
- [Astro](https://astro.build/) 5 - 静态站点生成器：**默认全静态，仅 5 个页面 SSR**（文章详情、文章列表、后台编辑器、后台用户页），使用 `@astrojs/node` 的 **node standalone** 适配器承载
- [Vue 3](https://vuejs.org/) - 交互组件
- [Tailwind CSS](https://tailwindcss.com/) - 样式框架
- [TypeScript](https://www.typescriptlang.org/) - 类型安全
- [Element Plus](https://element-plus.org/) - UI 组件库

### 后端
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 后端框架
- [MySQL 8.0+](https://www.mysql.com/) - 数据库（**最低要求 MySQL 8.0**）
- [JWT](https://jwt.io/) - 认证机制（密钥由环境变量 `JWT_SECRET` 注入，**无默认值，缺失/过短拒绝启动**）
- [阿里云百炼](https://bailian.aliyun.com/) - AI 能力

**与 WordPress 的差异一句话**：WordPress 是"动态渲染 + 插件生态 + 后台改配置"；LuomiBlog 是"静态预渲染 + 少量 SSR + 环境变量/安装向导配置"。没有插件体系，但换来了首屏零 JS 与 2核4G 也能流畅运行的资源占用。

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | **21+** | 后端按 Java 21 编译（`pom.xml` `java.version=21`），Java 17 无法构建与运行 |
| MySQL | 8.0+ | **必须 MySQL 8.0 或更高版本**，低版本不支持部分 SQL 语法 |
| Node.js | 18.17+（建议 20 LTS+） | 前端构建与 SSR 运行需要（Astro 5 要求） |
| Maven | 3.9+ | 构建后端需要。**仓库未自带 Maven Wrapper**（`mvnw` 仅存在于 `Demo/` 参考项目中），请自行安装 Maven |

## 项目结构

```
LuomiBlog/
├── luomiblog-frontend/     # Astro 前端项目
│   ├── src/
│   │   ├── components/     # Vue/Astro 组件
│   │   ├── pages/          # 页面
│   │   ├── layouts/        # 布局
│   │   └── styles/         # 样式
│   └── public/             # 静态资源
├── luomiblog-backend/      # SpringBoot 后端项目
│   └── src/main/
│       ├── java/           # Java 源码
│       └── resources/
│           └── db/         # 数据库脚本
├── scripts/                # 运维脚本（如 reset-install.ps1）
├── .github/workflows/      # GitHub Actions CI
├── Demo/                   # 演示和参考
└── 项目文档/               # 设计文档
```

## 快速开始（本地开发）

```bash
# 1. 准备数据库（也可交给安装向导：JDBC URL 自带 createDatabaseIfNotExist）
#    后端连接所需的用户名/密码通过环境变量传入，见下方「后端环境变量」

# 2. 启动后端（需已安装 Maven 3.9+ 与 JDK 21+）
cd luomiblog-backend
JWT_SECRET='<至少32字符的随机串>' DB_USERNAME=root DB_PASSWORD=你的密码 mvn spring-boot:run

# 3. 启动前端开发服务器（4321 端口，/api 自动代理到 8080）
cd luomiblog-frontend
npm install
npm run dev

# 4. 打开 http://localhost:4321/install 走安装向导
```

> 后端环境变量是硬要求，漏掉 `JWT_SECRET` 后端会**拒绝启动**。完整说明见[五分钟部署](#五分钟部署生产上线)第 1 步。

## 五分钟部署（生产上线）

### 第 0 步：准备数据库

在 MySQL 8.0+ 上执行（推荐专用账号，不要用 root）：

```sql
CREATE DATABASE luomiblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'luomiblog'@'localhost' IDENTIFIED BY '换成强密码';
GRANT ALL PRIVILEGES ON luomiblog.* TO 'luomiblog'@'localhost';
FLUSH PRIVILEGES;
```

> 安装向导的「测试连接」在库不存在时也可尝试 `CREATE DATABASE`（需要账号有建库权限）；上面的 SQL 是更规范的做法。

### 第 1 步：启动后端

后端**必须**通过环境变量注入以下配置（缺一不可）：

| 环境变量 | 必填 | 说明 |
|---------|:----:|------|
| `JWT_SECRET` | **是** | JWT 签名密钥，**至少 32 字符随机串**。缺失或过短时后端启动即失败（启动时强校验，无公开默认值） |
| `DB_USERNAME` | **是** | MySQL 用户名（默认值为空，必须显式提供） |
| `DB_PASSWORD` | **是** | MySQL 密码（同上） |
| `SPRING_DATASOURCE_URL` | 否 | 覆盖数据库连接地址。默认 `jdbc:mysql://localhost:3306/luomiblog`（自带 `createDatabaseIfNotExist=true`）；远程数据库/自定库名时设置此项 |
| `CHENXI_ENABLED` 等 | 否 | 辰汐通行证登录，见 [docs/chenxi-integration.md](docs/chenxi-integration.md) |

生成强随机密钥：

```bash
# Linux / macOS / Git Bash
openssl rand -base64 48
```

```powershell
# Windows PowerShell
$b = [byte[]]::new(48); (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($b); [Convert]::ToBase64String($b)
```

开发/试运行（Maven 直接启动，端口 **8080**）：

```bash
cd luomiblog-backend
JWT_SECRET='<上一步生成的密钥>' DB_USERNAME=luomiblog DB_PASSWORD='你的密码' mvn spring-boot:run
```

生产建议打包为 jar 常驻运行：

```bash
cd luomiblog-backend
mvn -B -DskipTests package
JWT_SECRET='<密钥>' DB_USERNAME=luomiblog DB_PASSWORD='你的密码' \
  java -jar target/luomiblog-backend-0.2.0.jar
```

生产环境请用 systemd / supervisor 等守护进程托管，并把环境变量写进其配置文件（不要写进仓库）。

### 第 2 步：构建并启动前端

```bash
cd luomiblog-frontend
cp .env.production.example .env.production   # 与 nginx 同域部署时保持 PUBLIC_API_URL=/api 即可
npm install
npm run build
PORT=4321 node ./dist/server/entry.mjs
```

说明：

- 构建产物分两部分：`dist/client/`（静态资源）与 `dist/server/`（SSR 服务器）。
- **必须用 node standalone 启动**（`node ./dist/server/entry.mjs`）。虽然有 90%+ 页面是预渲染 HTML，但文章详情/列表等 5 个页面是 SSR，纯静态托管（如对象存储直传）会 404。
- **必须显式设置 `PORT`**：node standalone 的默认端口是 **8080，与后端冲突**；上面示例用 4321，`HOST` 环境变量可绑定监听地址。
- `PUBLIC_API_URL` 是**构建时**注入的：同域反代用 `/api`（推荐）；前后端分域名则填后端完整地址，并注意 CORS 配置（后端 `app.cors.allowed-origins`）。

### 第 3 步：进入安装向导（八步）

浏览器访问 `http://你的域名/install`（本机调试为 `http://localhost:4321/install`）：

1. **环境检测** - 自检 Java 版本、后端服务、MySQL 驱动、磁盘可写、SMTP 配置（SMTP 为非阻塞项，可跳过后续在后台配置）
2. **数据库配置** - 本机库自动预填 `localhost:3306`；远程库填 host + 端口。「测试连接」成功会显示数据库版本与字符集，失败给出分类提示；库不存在时可一键尝试创建
3. **初始化数据** - 执行 schema/data 脚本建表写入初始数据
4. **站点配置** - 站点名/描述等，保存落库、即时生效
5. **SMTP 邮箱配置** - 可在此预配置，也可跳过装完后在管理后台配置
6. **网站图标配置** - favicon 设置
7. **创建管理员账号** - **邮箱必填**；系统生成 16 位强密码**仅明文展示一次**，需要二次输入确认（关闭提示后无法再查看，请立即复制保存）
8. **安装成功** - 汇总页。此后安装锁自动生效，防止重复安装

安装完成后：前台 `http://你的域名/`，后台登录 `http://你的域名/login`。

### 第 4 步：装完第一件事——SMTP 与安全开关

进入管理后台「系统设置」：

1. 配置 **SMTP 邮件服务** 并点「发送测试邮件」确认（详见[安装后配置](#安装后配置站长安全开关)）；
2. 按需开启 **注册邮箱验证**（依赖 SMTP，未配 SMTP 时开启会警告）与 **登录两步验证（2FA）**。

## Nginx 反代样例

生产推荐「nginx 统一入口」：浏览器只访问 nginx（80/443），页面请求转发给前端 node standalone（4321），`/api/` 转发给后端 Spring Boot（8080）。可直接复制修改域名使用：

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # 上传/接口体积上限（按需调整；预留大文件场景）
    client_max_body_size 20m;

    # 1) 后端 API → Spring Boot (8080)
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout    120s;
        proxy_read_timeout    120s;
    }

    # 2) 其余请求 → Astro node standalone (4321)：静态页 + 5 个 SSR 页
    location / {
        proxy_pass http://127.0.0.1:4321;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout    120s;
    }

    # 可选优化：带内容哈希的构建产物直接由 nginx 返回，减轻 node 压力
    # location /_astro/ {
    #     root /path/to/LuomiBlog/luomiblog-frontend/dist/client;
    #     expires 30d;
    #     add_header Cache-Control "public, immutable";
    # }
}
```

**node standalone 与 `/api` 的关系**：前端在构建时把 `PUBLIC_API_URL=/api` 内联进代码，浏览器里的交互组件都请求同域 `/api/...`，由 nginx 转发到 8080 后端；而页面本身（静态 HTML + SSR）由 4321 的 node 进程产出。开发环境下这个转发由 Astro dev server 内置 proxy（`astro.config.mjs` 中 `/api → localhost:8080`）完成，生产环境则交给 nginx——两者职责等价，不要同时开。

## 从源码到上线：CI/CD

### 仓库自带的 CI（GitHub Actions）

仓库内置 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)，push / PR 到 `main` 时自动运行两个独立任务：

| 任务 | 内容 | 说明 |
|------|------|------|
| backend | JDK 21 (Temurin) + `mvn -B -ntp test` | 跑后端单元测试（当前为 TOTP 等纯单元测试，**不依赖 MySQL**，已验证无数据库可完整执行），同时覆盖编译检查 |
| frontend | Node 20 + `npm ci` + `npm run build` | 验证前端可独立构建（构建过程允许后端不在线，静态页自动降级），产物以 artifact 上传（`actions/upload-artifact@v4`，保留 7 天） |

依赖缓存使用 `setup-java@v4` / `setup-node@v4` 的内置 cache（Maven `~/.m2` 与 npm），全部动作锁定在 v4 版本。

### 发版现状（tag / Release）

- 仓库**当前没有 git tag，也没有自动发版流水线**；GitHub Release（若有）为手动发布。
- CI 只做质量门禁（测试 + 构建），不做部署、不发版。站长部署请以 `main` 分支或自行打 tag 的 commit 为准。

### 站长自建等价校验

不依赖 GitHub Actions，任何一台装好 JDK 21 + Maven 3.9 + Node 的机器都能做同等的上线前校验：

```bash
# 后端：编译 + 单元测试（无需 MySQL）
cd luomiblog-backend
mvn -B -ntp test

# 前端：依赖安装 + 构建
cd luomiblog-frontend
npm ci
npm run build
```

两项都绿即可按[五分钟部署](#五分钟部署生产上线)的产物（jar + `dist/`）上线。

## 安装安全机制

LuomiBlog 采用四重安全机制防止重复安装：

1. **安装锁文件** - 安装完成后在后端工作目录生成 `install.lock` 文件
2. **数据库标记** - 在数据库中记录安装状态
3. **API 防护** - 所有安装相关 API 检查安装状态，已安装返回 403
4. **前端拦截** - 安装页面自动检测，已安装则跳转到首页

**注意**：即使删除 `install.lock` 文件，只要数据库中存在用户数据，系统仍然会认为已安装。

**重装/恢复通道**（详见 [FAQ](#常见问题-faq)）：

- 正常已安装：在向导页输入管理员/博主密码验证后进入重装选项（保留数据仅重建表结构 / 全新安装），验证有速率限制（每分钟 5 次）；
- 半安装/装库失败：向导提供「重置安装状态」回到数据库配置步；
- 异常锁死态自愈：`install.lock` 存在但数据库中无任何管理员账号时，系统放行状态重置，无需手动删文件；
- 服务器手动恢复：仓库根目录运行 `powershell -ExecutionPolicy Bypass -File .\scripts\reset-install.ps1`（删除 `install.lock` 与 `config/custom-application.yml`），再按脚本提示清理数据库与浏览器缓存。

## 安装后配置：站长安全开关

全部位于管理后台「系统设置」页。

### SMTP 邮件服务

| 字段 | 说明 |
|------|------|
| SMTP 服务器 | 如 `smtp.qq.com`、`smtpdm.aliyun.com` |
| 端口 | 常用 `465`（SSL/TLS）或 `587`（STARTTLS） |
| 邮箱账号（登录用户名） | 一般为完整邮箱地址 |
| 密码 / 授权码 | 多数服务商要求使用「授权码」而非邮箱登录密码；已保存的密码不回传浏览器，留空表示沿用 |
| 发件人地址 | 如 `noreply@yourdomain.com` |
| 加密方式 | SSL/TLS 开关（打开对应 465 场景） |

- 保存后用页面的「**发送测试邮件**」验证（填一个测试收件邮箱）。
- **判定规则**：填了「SMTP 服务器 + 发件人地址」即视为已配置 SMTP；**无鉴权中继可用**——内网 relay 场景账号/密码可留空。
- 注册邮箱验证、密码找回等邮件功能都依赖此处配置。

### 注册邮箱验证开关

开启后，新用户注册需先完成邮箱验证（6 位验证码或激活链接）才能登录。**依赖 SMTP**：未配置 SMTP 时开启会收到警告，新用户将收不到验证邮件。

### 登录两步验证（2FA / TOTP）

- 开启后，用户登录需在密码之外输入动态验证码（Google Authenticator / 1Password 等 TOTP 应用扫码绑定）；
- 绑定时生成 **10 个一次性还原码**，手机丢失时可用其登录，请提示用户妥善保存；
- 用户丢失认证器无法登录时，管理员可在「用户两步验证管理」中**重置其 2FA 绑定**，该用户下次登录将重新进入绑定流程。

## 常见问题 FAQ

**Q：后端启动即退出（或被守护进程反复拉起反复退出）？**
A：十有八九是环境变量缺失。启动日志出现 `jwt.secret` 校验失败即表示 `JWT_SECRET` 未设置或不足 32 字符（后端启动时强校验、无默认值，属预期安全行为）；`DB_USERNAME` / `DB_PASSWORD` 未设置也无法连接数据库。按[环境变量表](#第-1-步启动后端)补齐后重启。

**Q：访问 `/install` 提示"已安装"，或安装中途失败想重来？**
A：见[安装安全机制](#安装安全机制)一节的重装/恢复通道：半安装可在向导内重置；已安装需管理员密码验证；异常锁死态（有锁文件但无管理员账号）系统会自愈放行；服务器上也可运行 `scripts/reset-install.ps1` 后清理数据库重来。

**Q：注册/测试邮件收不到？**
A：四步排查——
1. 后台 SMTP 配置已保存且状态为"已配置"，用「发送测试邮件」自检；
2. 端口与加密方式匹配：465 对应 SSL/TLS 开关打开、587 为 STARTTLS；密码必须是服务商签发的**授权码**；
3. 查收件方垃圾箱 / 域名 SPF、DKIM 记录；内网无鉴权中继场景账号密码可留空（仅 host + 发件人）；
4. 看后端日志中的邮件异常堆栈。注意「注册邮箱验证」开关依赖 SMTP，未配置时不要开启。

**Q：怎么传图片？有图床吗？**
A：后端当前**未内建**图片上传/图床接口（无 multipart 端点）。文章图片建议：外链辰汐生态图床 [AstrNest](https://github.com/luminous-ChenXi/AstrNest)，或把图片放进 `luomiblog-frontend/public/` 随前端构建发布。nginx 样例已预留 `client_max_body_size 20m`。

**Q：评论区在哪里？**
A：评论后端 API 已就绪，**前端评论区组件开发中**，当前页面暂未接入。

**Q：前端 node 进程起不来或报端口占用？**
A：node standalone 默认端口是 **8080**，会与后端冲突——启动时请显式 `PORT=4321 node ./dist/server/entry.mjs`。

## 辰汐通行证登录（可选）

LuomiBlog 支持通过"辰汐通行证"一键登录（标准 OAuth 2.1 / OIDC 授权码 + PKCE 公共客户端，**无任何密钥**）。功能默认关闭，需要时在通行证侧注册客户端后设置 `CHENXI_ENABLED=true` 等环境变量即可启用；首次登录会自动创建关联的影子账号。详见 [docs/chenxi-integration.md](docs/chenxi-integration.md)。

## 数据库设计

包含 36 张表，涵盖：
- 权限系统（RBAC）
- 用户管理
- 文章管理（支持版本控制）
- 评论互动（支持@功能）
- AI 系统
- 芙贝币签到奖励
- 赞赏系统
- 收藏功能

## 核心设计理念

1. **原创为核，AI 赋能** - 博主原创内容是核心，AI 只是放大镜
2. **静态优先，岛屿架构** - 首屏零 JS，交互按需加载
3. **程序员友好，Git 原生** - 可选 Git 同步，底层标准 MD
4. **轻量高效，低维落地** - 2核4G 跑起来且响应流畅

## 版本历史

### v0.3.0 (2026-03-12)
- 新增可视化安装向导
- 严格环境检测（Java 版本、MySQL 版本）
- 完善安装安全机制
- 优化安装页面样式

### v0.2.0 (2026-03-10)
- 完善数据库设计（36张表）
- 四角色权限系统（访客/会员/博主/管理员）
- 新增芙贝币签到奖励系统
- 新增赞赏/打赏功能
- 新增收藏功能
- 新增评论@功能
- 完善文章版本控制与协作编辑

## 许可证

本项目基于 **GPL-3.0 with Additional Terms (Non-Commercial)**（GPL-3.0 + 非商用附加条款）协议发布，完整文本见 [LICENSE](LICENSE)。

- 你可以自由地部署、运行、修改和发布本软件；
- 未经版权人（ChenXi / 辰汐）事先书面授权，禁止将本软件或其衍生作品用于任何商业用途（包括但不限于出售、付费服务/SaaS 收费、商业产品捆绑、广告变现）；
- 任何衍生作品必须保留原始版权声明与本协议全文，并在显著位置（关于页/README/文档）注明原始项目名称与仓库地址，且以相同的"GPL-3.0 + 附加条款"协议发布。

> 若受本项目启发，欢迎附上出处链接。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/luminous-ChenXi">luminous-ChenXi</a>
</p>
