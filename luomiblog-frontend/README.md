# LuomiBlog 前端

LuomiBlog 的前端项目：一个面向程序员的 AI 原生增强型知识库博客，基于 [Astro](https://astro.build) 5 + Vue 3 构建的文章内容以静态方式生成，登录、评论、AI 问答等动态能力通过调用后端 API 实现。

## 技术栈

- [Astro 5](https://astro.build)（静态站点生成）
- [Vue 3](https://vuejs.org)（交互组件，如登录/注册弹窗、文章编辑器、AI 助手等）
- [Tailwind CSS 4](https://tailwindcss.com)
- [Element Plus](https://element-plus.org)（弹窗与消息提示）
- [TypeScript](https://www.typescriptlang.org)

## 快速开始

包管理器使用 **npm**（仓库内只保留 `package-lock.json`）。

```sh
# 安装依赖
npm install

# 启动开发服务器（http://localhost:4321）
npm run dev

# 生产构建（输出到 dist/）
npm run build

# 本地预览生产构建
npm run preview

# 类型检查
npm run typecheck
```

## 环境变量

复制 `.env.example` 为 `.env` 并按需修改；生产环境可参考 `.env.production.example`。

| 变量 | 说明 |
| :-- | :-- |
| `PUBLIC_API_URL` | 后端 API 地址，本地开发默认 `http://localhost:8080` |
| `PUBLIC_SITE_URL` | 站点对外访问地址，用于 RSS/Atom/robots.txt 等生成绝对链接 |
| `PUBLIC_SITE_NAME` | 站点名称 |
| `PUBLIC_SITE_DESCRIPTION` | 站点描述 |
| `PUBLIC_AI_ENABLED` | 是否启用 AI 问答 |
| `PUBLIC_AI_MAX_QUESTIONS_PER_MINUTE` | AI 问答每分钟提问上限 |
| `PUBLIC_COMMENTS_ENABLED` | 是否启用评论 |
| `PUBLIC_LIKES_ENABLED` | 是否启用点赞 |
| `PUBLIC_SHARE_ENABLED` | 是否启用分享 |

开发服务器内置了 `/api` 到 `http://localhost:8080` 的代理（见 `astro.config.mjs`）。

## 安全说明

管理后台（`/admin/*`）与用户中心（`/user/*`）页面均为静态生成的空壳页面，仅依赖浏览器端的 localStorage Token 做展示层门禁。**前端不是安全边界**——所有敏感操作必须以后端 API 的鉴权与权限校验为准。
