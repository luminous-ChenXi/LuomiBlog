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
  <a href="./README.md">简体中文</a> · <b>English</b>
</p>

</div>

## Core Features

- **Visual Installation Wizard** - Complete installation via web interface like WordPress, no manual configuration needed
- **Static-First, Islands Architecture** - Leverage Astro's static site generation, 90%+ content pre-rendered as HTML
- **AI-Native Enhancement** - Agentic RAG Q&A powered by Alibaba Cloud Bailian
- **Developer-Friendly** - Git-native workflow, standard MD/MDX support
- **Four-Role Permission System** - Complete RBAC for Visitor/Member/Blogger/Admin
- **Multi-Language Support** - Internationalization in Chinese/English/Japanese
- **Lightweight & Efficient** - Optimized for 2-core 4GB lightweight servers

## Tech Stack

### Frontend
- [Astro](https://astro.build/) - Static site generator
- [Vue 3](https://vuejs.org/) - Interactive components
- [Tailwind CSS](https://tailwindcss.com/) - Styling framework
- [TypeScript](https://www.typescriptlang.org/) - Type safety
- [Element Plus](https://element-plus.org/) - UI component library

### Backend
- [SpringBoot](https://spring.io/projects/spring-boot) - Java backend framework
- [MySQL 8.0+](https://www.mysql.com/) - Database (**Minimum MySQL 8.0 required**)
- [JWT](https://jwt.io/) - Authentication mechanism
- [Alibaba Cloud Bailian](https://bailian.aliyun.com/) - AI capabilities

## System Requirements

| Component | Minimum Version | Notes |
|-----------|----------------|-------|
| Java | 17+ | **Java 17 or higher required**, lower versions cannot run |
| MySQL | 8.0+ | **MySQL 8.0 or higher required**, lower versions don't support some SQL syntax |
| Node.js | 18+ | Required for frontend build |

## Project Structure

```
LuomiBlog/
├── luomiblog-frontend/     # Astro frontend project
│   ├── src/
│   │   ├── components/     # Vue/Astro components
│   │   ├── pages/          # Pages
│   │   ├── layouts/        # Layouts
│   │   └── styles/         # Styles
│   └── public/             # Static assets
├── luomiblog-backend/      # SpringBoot backend project
│   └── src/main/
│       ├── java/           # Java source code
│       └── resources/
│           └── db/         # Database scripts
├── Demo/                   # Demo and references
└── 项目文档/               # Design documents
```

## Quick Start

### Method 1: Visual Installation (Recommended)

LuomiBlog provides a WordPress-like visual installation wizard, no manual configuration needed.

1. **Start Backend Service**
```bash
cd luomiblog-backend
./mvn spring-boot:run
```

2. **Start Frontend Dev Server**
```bash
cd luomiblog-frontend
npm install
npm run dev
```

3. **Access Installation Page**
Open browser and visit `http://localhost:4321/install`, follow the wizard:
   - Environment Check (Java version, MySQL version, backend status)
   - Database Configuration
   - Database Initialization
   - Site Configuration
   - Admin Account Creation
   - Complete Installation

4. **After Installation**
   - Install lock file will be generated automatically to prevent reinstallation
   - Visit `http://localhost:4321` for blog homepage
   - Visit `http://localhost:4321/login` for admin panel

### Method 2: Manual Configuration

For manual configuration, please refer to the following steps:

```bash
# 1. Database Initialization
mysql -u root -p < luomiblog-backend/src/main/resources/db/schema.sql
mysql -u root -p < luomiblog-backend/src/main/resources/db/data.sql

# 2. Modify Backend Configuration
# Edit luomiblog-backend/src/main/resources/application.yml
# Configure database connection info

# 3. Start Backend
cd luomiblog-backend
./mvnw spring-boot:run

# 4. Start Frontend
cd luomiblog-frontend
npm install
npm run dev
```

## Installation Security Mechanism

LuomiBlog adopts multiple security mechanisms to prevent reinstallation:

1. **Install Lock File** - Generates `install.lock` file after installation
2. **Database Mark** - Records installation status in database
3. **API Protection** - All installation APIs check status, return 403 if already installed
4. **Frontend Interception** - Installation page auto-detects, redirects to homepage if installed

**Note**: Even if `install.lock` is deleted, the system will still consider it installed as long as user data exists in the database.

## Deployment Guide

### Production Configuration

1. Copy `.env.production.example` to `.env.production`
2. Modify `PUBLIC_API_URL` to actual backend address
3. Build frontend: `npm run build`

### Nginx Configuration Example

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

## Database Design

Includes 36 tables covering:
- Permission system (RBAC)
- User management
- Article management (with version control)
- Comment interaction (with @ mention support)
- AI system
- Fubei coin check-in rewards
- Appreciation/tipping system
- Favorites functionality

## Core Design Philosophy

1. **Original Content as Core, AI as Amplifier** - Blogger's original content is the core, AI is just a magnifier
2. **Static-First, Islands Architecture** - Zero JS on first screen, interactive loads on demand
3. **Developer-Friendly, Git-Native** - Optional Git sync, standard MD at the core
4. **Lightweight & Efficient, Low-Dimension Landing** - Runs smoothly on 2-core 4GB

## Version History

### v0.3.0 (2026-03-12)
- Added visual installation wizard
- Strict environment checks (Java 17+, MySQL 8.0+)
- Improved installation security mechanism
- Optimized installation page styling

### v0.2.0 (2026-03-10)
- Improved database design (36 tables)
- Four-role permission system (Visitor/Member/Blogger/Admin)
- Added Fubei coin check-in reward system
- Added appreciation/tipping feature
- Added favorites feature
- Added comment @ mention feature
- Improved article version control and collaborative editing

## License

[MIT](LICENSE)

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/luminous-ChenXi">luminous-ChenXi</a>
</p>
