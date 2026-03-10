# LuomiBlog

<p align="center">
  <img src="https://img.shields.io/badge/version-v0.2.0-blue" alt="version">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="license">
  <img src="https://img.shields.io/badge/tech-Astro%20%2B%20Vue%20%2B%20SpringBoot-orange" alt="tech">
</p>

**LuomiBlog** - 一个基于 Astro + Vue + SpringBoot 的 AI 知识库博客系统

## 核心特性

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

### 后端
- [SpringBoot](https://spring.io/projects/spring-boot) - Java 后端框架
- [MySQL 8.0](https://www.mysql.com/) - 数据库
- [JWT](https://jwt.io/) - 认证机制
- [阿里云百炼](https://bailian.aliyun.com/) - AI 能力

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

## 快速开始

### 环境要求
- Node.js 18+
- Java 17+
- MySQL 8.0+

### 安装依赖

```bash
# 前端
cd luomiblog-frontend
npm install

# 后端
cd luomiblog-backend
./mvnw install
```

### 数据库初始化

```bash
mysql -u root -p < luomiblog-backend/src/main/resources/db/schema.sql
mysql -u root -p < luomiblog-backend/src/main/resources/db/data.sql
```

### 启动开发服务器

```bash
# 前端
cd luomiblog-frontend
npm run dev

# 后端
cd luomiblog-backend
./mvnw spring-boot:run
```

## 核心设计理念

1. **原创为核，AI 赋能** - 博主原创内容是核心，AI 只是放大镜
2. **静态优先，岛屿架构** - 首屏零 JS，交互按需加载
3. **程序员友好，Git 原生** - 可选 Git 同步，底层标准 MD
4. **轻量高效，低维落地** - 2核4G 跑起来且响应流畅

## 版本历史

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
