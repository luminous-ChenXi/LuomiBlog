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
  <a href="./README.md">简体中文</a> · <b>English</b>
</p>

</div>

> 🪺 A member of the **Chenxi Ecosystem (辰汐生态)** — alongside [LuomiNest](https://github.com/LuminousCX/LuomiNest) (desktop AI companion), [AstrNest](https://github.com/luminous-ChenXi/AstrNest) (image hosting / media management), and Teachenxi (learning companion app). This project's role: a static-first AI-powered knowledge base blog system.

## Table of Contents

- [Core Features](#core-features)
- [Tech Stack](#tech-stack)
- [System Requirements](#system-requirements)
- [Quick Start (Local Development)](#quick-start-local-development)
- [Five-Minute Deployment (Production)](#five-minute-deployment-production)
- [Nginx Reverse Proxy Example](#nginx-reverse-proxy-example)
- [From Source to Production: CI/CD](#from-source-to-production-cicd)
- [Installation Security Mechanism](#installation-security-mechanism)
- [Post-Install Configuration: Admin Security Switches](#post-install-configuration-admin-security-switches)
- [FAQ](#faq)

## Core Features

- **Visual Installation Wizard** - Complete installation via web interface like WordPress, in 8 steps: environment self-check (Java / backend service / MySQL driver / disk / SMTP) → database configuration → initialization → site configuration → SMTP → favicon → admin account → done; with quadruple re-install protection and reset recovery channels
- **Static-First, Islands Architecture** - Leverage Astro's static site generation, 90%+ content pre-rendered as HTML, only a handful of SSR pages
- **Working Interactions** - Likes (dual-track de-duplication for logged-in and guest users), favorites (personal favorites page), view counts with 24h de-duplication; comment backend API is ready (frontend comment UI in development)
- **Admin Security Switches** - Registration email verification (requires SMTP), login two-factor authentication (TOTP: binding + 10 one-time recovery codes + admin reset)
- **AI-Native Enhancement** - Agentic RAG Q&A powered by Alibaba Cloud Bailian
- **Developer-Friendly** - Git-native workflow, standard MD/MDX support
- **Four-Role Permission System** - Complete RBAC for Visitor/Member/Blogger/Admin
- **Multi-Language Support** - Internationalization in Chinese/English/Japanese
- **Lightweight & Efficient** - Optimized for 2-core 4GB lightweight servers

## Tech Stack

### Frontend
- [Astro](https://astro.build/) 5 - Static site generator: **fully static by default with only 5 SSR pages** (article detail, article list, admin editors, admin users), served by the **node standalone** adapter (`@astrojs/node`)
- [Vue 3](https://vuejs.org/) - Interactive components
- [Tailwind CSS](https://tailwindcss.com/) - Styling framework
- [TypeScript](https://www.typescriptlang.org/) - Type safety
- [Element Plus](https://element-plus.org/) - UI component library

### Backend
- [Spring Boot](https://spring.io/projects/spring-boot) - Java backend framework
- [MySQL 8.0+](https://www.mysql.com/) - Database (**Minimum MySQL 8.0 required**)
- [JWT](https://jwt.io/) - Authentication (secret injected via the `JWT_SECRET` environment variable, **no default value; startup fails if missing or too short**)
- [Alibaba Cloud Bailian](https://bailian.aliyun.com/) - AI capabilities

**One-line difference vs WordPress**: WordPress is "dynamic rendering + plugin ecosystem + settings in admin panel"; LuomiBlog is "static pre-rendering + a few SSR pages + environment variables / installation wizard". No plugin system, but in exchange you get zero-JS first screens and smooth operation on a 2-core 4GB box.

## System Requirements

| Component | Version | Notes |
|-----------|---------|-------|
| Java | **21+** | The backend is compiled with Java 21 (`java.version=21` in `pom.xml`); Java 17 cannot build or run it |
| MySQL | 8.0+ | **MySQL 8.0 or higher required**, lower versions don't support some SQL syntax |
| Node.js | 18.17+ (20 LTS+ recommended) | Required for frontend build and SSR runtime (Astro 5 requirement) |
| Maven | 3.9+ | Required to build the backend. **The repo does not ship a Maven Wrapper** (`mvnw` exists only under `Demo/` as reference), install Maven yourself |

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
├── scripts/                # Ops scripts (e.g. reset-install.ps1)
├── .github/workflows/      # GitHub Actions CI
├── Demo/                   # Demo and references
└── 项目文档/               # Design documents
```

## Quick Start (Local Development)

```bash
# 1. Prepare a database (or let the install wizard do it:
#    the JDBC URL ships with createDatabaseIfNotExist)
#    DB credentials are provided via environment variables, see below

# 2. Start the backend (requires Maven 3.9+ and JDK 21+)
cd luomiblog-backend
JWT_SECRET='<random string, at least 32 chars>' DB_USERNAME=root DB_PASSWORD=yourpassword mvn spring-boot:run

# 3. Start the frontend dev server (port 4321, /api proxied to 8080)
cd luomiblog-frontend
npm install
npm run dev

# 4. Open http://localhost:4321/install and follow the wizard
```

> The backend environment variables are hard requirements — without `JWT_SECRET` the backend **refuses to start**. See step 1 of [Five-Minute Deployment](#five-minute-deployment-production).

## Five-Minute Deployment (Production)

### Step 0: Prepare the Database

Run on MySQL 8.0+ (a dedicated account is recommended, avoid root):

```sql
CREATE DATABASE luomiblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'luomiblog'@'localhost' IDENTIFIED BY 'change-me-strong-password';
GRANT ALL PRIVILEGES ON luomiblog.* TO 'luomiblog'@'localhost';
FLUSH PRIVILEGES;
```

> The wizard's "test connection" can also attempt `CREATE DATABASE` when the database doesn't exist (requires CREATE privilege); the SQL above is the cleaner approach.

### Step 1: Start the Backend

The backend **must** be configured via environment variables (all of them):

| Environment Variable | Required | Description |
|---------------------|:--------:|-------------|
| `JWT_SECRET` | **Yes** | JWT signing secret, **random string of at least 32 characters**. Startup fails immediately if missing or too short (validated at boot, no public default) |
| `DB_USERNAME` | **Yes** | MySQL username (default is empty, must be provided explicitly) |
| `DB_PASSWORD` | **Yes** | MySQL password (same as above) |
| `SPRING_DATASOURCE_URL` | No | Overrides the JDBC URL. Default: `jdbc:mysql://localhost:3306/luomiblog` (with `createDatabaseIfNotExist=true`); set this for remote databases or custom database names |
| `CHENXI_ENABLED` etc. | No | Chenxi Passport login, see [docs/chenxi-integration.md](docs/chenxi-integration.md) |

Generate a strong random secret:

```bash
# Linux / macOS / Git Bash
openssl rand -base64 48
```

```powershell
# Windows PowerShell
$b = [byte[]]::new(48); (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($b); [Convert]::ToBase64String($b)
```

Development / trial run (Maven, port **8080**):

```bash
cd luomiblog-backend
JWT_SECRET='<secret from above>' DB_USERNAME=luomiblog DB_PASSWORD='yourpassword' mvn spring-boot:run
```

For production, package a jar and keep it running:

```bash
cd luomiblog-backend
mvn -B -DskipTests package
JWT_SECRET='<secret>' DB_USERNAME=luomiblog DB_PASSWORD='yourpassword' \
  java -jar target/luomiblog-backend-0.2.0.jar
```

In production, supervise the process with systemd / supervisor and keep the environment variables in its unit configuration (never commit them).

### Step 2: Build and Start the Frontend

```bash
cd luomiblog-frontend
cp .env.production.example .env.production   # keep PUBLIC_API_URL=/api when behind the same-domain nginx
npm install
npm run build
PORT=4321 node ./dist/server/entry.mjs
```

Notes:

- The build produces two parts: `dist/client/` (static assets) and `dist/server/` (SSR server).
- **You must run the node standalone server** (`node ./dist/server/entry.mjs`). Although 90%+ pages are pre-rendered HTML, 5 pages including article detail/list are SSR — pure static hosting (e.g. object storage) would 404.
- **Always set `PORT` explicitly**: the node standalone default port is **8080, which collides with the backend**; the example above uses 4321. Use the `HOST` environment variable to bind the listening address.
- `PUBLIC_API_URL` is inlined **at build time**: use `/api` for same-domain reverse proxy (recommended); if frontend and backend live on separate domains, use the full backend URL and mind CORS (`app.cors.allowed-origins` on the backend).

### Step 3: Follow the Install Wizard (8 Steps)

Open `http://yourdomain.com/install` in a browser (`http://localhost:4321/install` for local debugging):

1. **Environment Check** - Verifies Java version, backend service, MySQL driver, writable disk, and SMTP configuration (SMTP is non-blocking and can also be configured later in the admin panel)
2. **Database Configuration** - Local databases are pre-filled with `localhost:3306`; remote databases use host + port. A successful "test connection" shows database version and charset; failures get classified error hints; the wizard can attempt to create the database if it doesn't exist
3. **Initialize Data** - Executes schema/data scripts to create tables and seed initial data
4. **Site Configuration** - Site name/description etc., saved to the database and effective immediately
5. **SMTP Configuration** - Can be pre-configured here or skipped and set up later in the admin panel
6. **Favicon Configuration** - Site icon setup
7. **Create Admin Account** - **Email is required**; a 16-character strong password is generated and shown **in plaintext only once**, and must be re-entered to confirm (once dismissed it can never be viewed again — copy and store it immediately)
8. **Installation Complete** - Summary page. The install lock takes effect automatically afterwards, preventing reinstallation

After installation: site at `http://yourdomain/`, admin login at `http://yourdomain/login`.

### Step 4: First Things After Installation — SMTP and Security Switches

Open the admin panel "Settings":

1. Configure the **SMTP mail service** and hit "Send test email" (see [Post-Install Configuration](#post-install-configuration-admin-security-switches));
2. Enable **registration email verification** (requires SMTP; a warning shows if SMTP is not configured) and **login 2FA** as needed.

## Nginx Reverse Proxy Example

The recommended production setup is "nginx as the single entry": browsers only talk to nginx (80/443), page requests are forwarded to the frontend node standalone (4321), and `/api/` is forwarded to the backend Spring Boot (8080). Copy and adjust the domain:

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # Upload/request size limit (adjust as needed; reserved for large files)
    client_max_body_size 20m;

    # 1) Backend API -> Spring Boot (8080)
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

    # 2) Everything else -> Astro node standalone (4321): static pages + 5 SSR pages
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

    # Optional optimization: serve hashed build assets directly from nginx
    # location /_astro/ {
    #     root /path/to/LuomiBlog/luomiblog-frontend/dist/client;
    #     expires 30d;
    #     add_header Cache-Control "public, immutable";
    # }
}
```

**How node standalone relates to `/api`**: at build time `PUBLIC_API_URL=/api` is inlined into the frontend code, so interactive components in the browser request same-origin `/api/...`, which nginx forwards to the backend on 8080; the pages themselves (static HTML + SSR) are produced by the node process on 4321. In development the same forwarding is done by Astro's built-in dev proxy (`/api -> localhost:8080` in `astro.config.mjs`) — in production nginx takes over that job. Don't run both at once.

## From Source to Production: CI/CD

### Built-in CI (GitHub Actions)

The repository ships with [.github/workflows/ci.yml](.github/workflows/ci.yml). On every push / PR to `main`, two independent jobs run:

| Job | Content | Notes |
|-----|---------|-------|
| backend | JDK 21 (Temurin) + `mvn -B -ntp test` | Runs backend unit tests (currently pure unit tests such as TOTP, **no MySQL required** — verified to run fully without a database) and covers compilation |
| frontend | Node 20 + `npm ci` + `npm run build` | Verifies the frontend builds standalone (the build tolerates the backend being offline; static pages degrade gracefully); artifacts are uploaded as an artifact (`actions/upload-artifact@v4`, kept 7 days) |

Dependency caching uses the built-in cache of `setup-java@v4` / `setup-node@v4` (Maven `~/.m2` and npm); all actions are pinned to v4.

### Release Status (tags / Releases)

- The repository currently has **no git tags and no automated release pipeline**; GitHub Releases (if any) are published manually.
- CI is a quality gate (tests + build) only — no deployments, no releases. Site owners should deploy from the `main` branch or a tagged commit.

### Equivalent Checks on Your Own Machine

Without GitHub Actions, any machine with JDK 21 + Maven 3.9 + Node can run the same pre-launch checks:

```bash
# Backend: compile + unit tests (no MySQL needed)
cd luomiblog-backend
mvn -B -ntp test

# Frontend: install + build
cd luomiblog-frontend
npm ci
npm run build
```

With both green, ship the artifacts (jar + `dist/`) following [Five-Minute Deployment](#five-minute-deployment-production).

## Installation Security Mechanism

LuomiBlog adopts quadruple security mechanisms to prevent reinstallation:

1. **Install Lock File** - Generates `install.lock` in the backend working directory after installation
2. **Database Mark** - Records installation status in database
3. **API Protection** - All installation APIs check status, return 403 if already installed
4. **Frontend Interception** - Installation page auto-detects, redirects to homepage if installed

**Note**: Even if `install.lock` is deleted, the system will still consider it installed as long as user data exists in the database.

**Reinstall / recovery channels** (see [FAQ](#faq)):

- Installed normally: verify with an admin/blogger password on the wizard page to access reinstall options (keep data and rebuild schema only / fresh install), with rate limiting (5 attempts per minute);
- Half-installed / database setup failed: the wizard offers "reset install state" to return to the database step;
- Abnormal locked state self-healing: if `install.lock` exists but the database contains no ADMIN account, the system allows a state reset without manually deleting files;
- Manual recovery on the server: run `powershell -ExecutionPolicy Bypass -File .\scripts\reset-install.ps1` from the repository root (deletes `install.lock` and `config/custom-application.yml`), then clean the database and browser cache as the script instructs.

## Post-Install Configuration: Admin Security Switches

All located in the admin panel "Settings" page.

### SMTP Mail Service

| Field | Description |
|-------|-------------|
| SMTP server | e.g. `smtp.qq.com`, `smtpdm.aliyun.com` |
| Port | Usually `465` (SSL/TLS) or `587` (STARTTLS) |
| Account (login username) | Usually the full email address |
| Password / authorization code | Most providers require an "authorization code" rather than the mailbox login password; saved passwords are never sent back to the browser — leave empty to keep the existing one |
| Sender address | e.g. `noreply@yourdomain.com` |
| Encryption | SSL/TLS toggle (on for the 465 scenario) |

- After saving, verify with "Send test email" on the same page.
- **Configured rule**: filling in "SMTP server + sender address" counts as configured; **unauthenticated relays are supported** — for intranet relays, account/password may be left empty.
- Registration email verification, password reset, and other email features all depend on this configuration.

### Registration Email Verification Toggle

When enabled, new users must complete email verification (6-digit code or activation link) before they can log in. **Requires SMTP**: enabling it without SMTP configured shows a warning, and new users won't receive verification emails.

### Login Two-Factor Authentication (2FA / TOTP)

- When enabled, users must enter a dynamic code on top of their password (scan to bind with any TOTP app such as Google Authenticator / 1Password);
- Binding generates **10 one-time recovery codes** for logging in when the phone is lost — remind users to store them safely;
- If a user loses their authenticator, an admin can **reset their 2FA binding** in "User 2FA management"; that user goes through the binding flow again on next login.

## FAQ

**Q: The backend exits immediately on startup (or crash-loops under a supervisor)?**
A: Almost always missing environment variables. A `jwt.secret` validation failure in the startup log means `JWT_SECRET` is unset or shorter than 32 characters (validated at boot with no default — expected security behavior); unset `DB_USERNAME` / `DB_PASSWORD` also prevent database connections. Fill them in per the [environment variable table](#step-1-start-the-backend) and restart.

**Q: `/install` says "already installed", or a failed install needs a retry?**
A: See the reinstall/recovery channels under [Installation Security Mechanism](#installation-security-mechanism): half-installed states can be reset inside the wizard; installed systems require admin password verification; abnormal locked states (lock file present but no admin account) self-heal; on the server you can also run `scripts/reset-install.ps1` and clean the database to start over.

**Q: Registration / test emails never arrive?**
A: Four-step troubleshooting —
1. The SMTP config is saved and shows "configured"; use "Send test email" to self-check;
2. Port matches encryption: 465 with the SSL/TLS toggle on, 587 for STARTTLS; the password must be the provider-issued **authorization code**;
3. Check the recipient's spam folder and the domain's SPF/DKIM records; for unauthenticated intranet relays, leave account/password empty (host + sender only);
4. Check backend logs for mail exception stack traces. Note that the "registration email verification" toggle depends on SMTP — don't enable it without SMTP.

**Q: How do I upload images? Is there an image host?**
A: The backend currently has **no built-in** image upload/image-host endpoint (no multipart endpoints). For article images: link to the ecosystem image host [AstrNest](https://github.com/luminous-ChenXi/AstrNest), or put images in `luomiblog-frontend/public/` to ship with the frontend build. The nginx example reserves `client_max_body_size 20m`.

**Q: Where is the comment section?**
A: The comment backend API is ready; the **frontend comment UI is in development** and not yet wired into pages.

**Q: The frontend node process fails to start, or reports the port is taken?**
A: The node standalone default port is **8080**, which collides with the backend — start it explicitly with `PORT=4321 node ./dist/server/entry.mjs`.

## Chenxi Passport Login (Optional)

LuomiBlog supports one-click login via the "Chenxi Passport" (standard OAuth 2.1 / OIDC authorization code + PKCE public client, **no secrets at all**). The feature is disabled by default; register a client on the Passport side and set `CHENXI_ENABLED=true` and related environment variables to enable it. The first login automatically creates a linked shadow account. See [docs/chenxi-integration.md](docs/chenxi-integration.md) for details.

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
- Strict environment checks (Java version, MySQL version)
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

This project is released under **GPL-3.0 with Additional Terms (Non-Commercial)**. See [LICENSE](LICENSE) for the full text.

- You are free to deploy, run, modify, and distribute this software;
- Commercial use of this software or its derivative works without prior written authorization from the copyright holder (ChenXi) is prohibited (including but not limited to selling, paid services/SaaS, commercial bundling, or advertising monetization);
- Any derivative work must retain the original copyright notice and this license in full, credit the original project name and repository URL in a prominent location (About page / README / documentation), and be released under the same "GPL-3.0 + Additional Terms".

> If this project inspires your work, a link back is appreciated.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/luminous-ChenXi">luminous-ChenXi</a>
</p>
