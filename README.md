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
    <img src="https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square" alt="license">
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

## 核心特性

- **可视化安装向导** - 像 WordPress 一样，通过 Web 界面完成安装配置，无需手动修改配置文件
- **静态优先，岛屿架构** - 充分发挥 Astro 的静态站点生成优势，90%以上内容预渲染为 HTML
- **AI 原生增强** - 基于阿里云百炼的 Agentic RAG 问答能力
- **程序员友好** - 支持 Git 写作习惯，MD/MDX 原生支持
- **四角色权限** - 访客/会员/博主/管理员完善的 RBAC 权限体系
- **多语言支持** - 中/英/日三语国际化
- **轻量高效** - 适配 2核4G 轻量服务器

## 技术栈

### 前端
- [Astro](https://astro.build/) - 静态站点生成器
- [Vue 3](https://vuejs.org/) - 交互组件
- [Tailwind CSS](https://tailwindcss.com/) - 样式框架
- [TypeScript](https://www.typescriptlang.org/) - 类型安全
- [Element Plus](https://element-plus.org/) - UI 组件库

### 后端
- [SpringBoot](https://spring.io/projects/spring-boot) - Java 后端框架
- [MySQL 8.0+](https://www.mysql.com/) - 数据库（**最低要求 MySQL 8.0**）
- [JWT](https://jwt.io/) - 认证机制
- [阿里云百炼](https://bailian.aliyun.com/) - AI 能力

## 环境要求

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| Java | 17+ | **必须 Java 17 或更高版本**，低版本无法运行 |
| MySQL | 8.0+ | **必须 MySQL 8.0 或更高版本**，低版本不支持部分 SQL 语法 |
| Node.js | 18+ | 前端构建需要 |

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
├── Demo/                   # 演示和参考
└── 项目文档/               # 设计文档
```

## 快速开始

### 方式一：可视化安装（推荐）

LuomiBlog 提供像 WordPress 一样的可视化安装向导，无需手动修改配置文件。

1. **启动后端服务**
```bash
cd luomiblog-backend
./mvn spring-boot:run
```

2. **启动前端开发服务器**
```bash
cd luomiblog-frontend
npm install
npm run dev
```

3. **访问安装页面**
打开浏览器访问 `http://localhost:4321/install`，按照向导完成：
   - 环境检测（Java 版本、MySQL 版本、后端服务状态）
   - 数据库配置
   - 数据库初始化
   - 站点配置
   - 管理员账号创建
   - 完成安装

4. **安装完成后**
   - 安装锁文件会自动生成，防止重复安装
   - 访问 `http://localhost:4321` 进入博客首页
   - 访问 `http://localhost:4321/login` 登录后台

### 方式二：手动配置

如需手动配置，请参考以下步骤：

```bash
# 1. 数据库初始化
mysql -u root -p < luomiblog-backend/src/main/resources/db/schema.sql
mysql -u root -p < luomiblog-backend/src/main/resources/db/data.sql

# 2. 修改后端配置
# 编辑 luomiblog-backend/src/main/resources/application.yml
# 配置数据库连接信息

# 3. 启动后端
cd luomiblog-backend
./mvnw spring-boot:run

# 4. 启动前端
cd luomiblog-frontend
npm install
npm run dev
```

## 安装安全机制

LuomiBlog 采用多重安全机制防止重复安装：

1. **安装锁文件** - 安装完成后生成 `install.lock` 文件
2. **数据库标记** - 在数据库中记录安装状态
3. **API 防护** - 所有安装相关 API 检查安装状态，已安装返回 403
4. **前端拦截** - 安装页面自动检测，已安装则跳转到首页

**注意**：即使删除 `install.lock` 文件，只要数据库中存在用户数据，系统仍然会认为已安装。

## 部署说明

### 生产环境配置

1. 复制 `.env.production.example` 为 `.env.production`
2. 修改 `PUBLIC_API_URL` 为实际后端地址
3. 构建前端：`npm run build`

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        root /path/to/luomiblog-frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

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
- 严格环境检测（Java 17+、MySQL 8.0+）
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

[MIT](LICENSE)

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/luminous-ChenXi">luminous-ChenXi</a>
</p>
