---
title: 'Docker 容器化部署实战'
description: '从零开始学习 Docker，掌握容器化部署的核心概念与实战技巧，让你的应用部署更加高效和可靠。'
pubDate: 2026-02-20
author: '辰汐'
tags: ['Docker', 'DevOps', '部署', '容器化']
category: 'DevOps'
cover: 'https://picsum.photos/seed/docker/800/500'
views: 178
comments: 6
slug: 'docker-deployment'
---

# Docker 容器化部署实战

容器化技术已经成为现代应用部署的标准。本文将从零开始，带你掌握 Docker 的核心概念和实战技巧，让你的应用部署更加高效和可靠。

## 什么是 Docker？

Docker 是一个开源的容器化平台，它允许开发者将应用及其依赖打包成一个标准化的单元——容器。

### 容器 vs 虚拟机

| 特性 | 虚拟机 (VM) | Docker 容器 |
|------|-------------|-------------|
| 启动时间 | 分钟级 | 秒级 |
| 资源占用 | 占用整个操作系统 | 共享宿主机内核 |
| 性能 | 有虚拟化开销 | 接近原生性能 |
| 隔离级别 | 强隔离 | 进程级隔离 |
| 镜像大小 | GB 级 | MB 级 |

```
┌─────────────────────────────────────────────────────────────┐
│                      虚拟机架构                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   App A     │  │   App B     │  │   App C             │  │
│  │   Bin/Libs  │  │   Bin/Libs  │  │   Bin/Libs          │  │
│  ├─────────────┤  ├─────────────┤  ├─────────────────────┤  │
│  │  Guest OS   │  │  Guest OS   │  │  Guest OS           │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Hypervisor (VMware/KVM)                    ││
│  └─────────────────────────────────────────────────────────┘│
│                    Host OS + Hardware                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      Docker 架构                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   App A     │  │   App B     │  │   App C             │  │
│  │   Bin/Libs  │  │   Bin/Libs  │  │   Bin/Libs          │  │
│  ├─────────────┤  ├─────────────┤  ├─────────────────────┤  │
│  │   Docker    │  │   Docker    │  │   Docker            │  │
│  │   Engine    │  │   Engine    │  │   Engine            │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Host OS (共享内核)                          ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## 核心概念

### 1. 镜像（Image）

镜像是容器的只读模板，包含运行应用所需的所有内容：

```dockerfile
# Dockerfile 示例
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .

EXPOSE 3000

USER node

CMD ["node", "server.js"]
```

### 2. 容器（Container）

容器是镜像的运行实例：

```bash
# 运行容器
docker run -d -p 3000:3000 --name myapp myimage

# 查看运行中的容器
docker ps

# 停止容器
docker stop myapp

# 删除容器
docker rm myapp
```

### 3. 仓库（Registry）

用于存储和分发镜像：

```bash
# 登录 Docker Hub
docker login

# 推送镜像
docker tag myimage username/myimage:v1.0
docker push username/myimage:v1.0

# 拉取镜像
docker pull username/myimage:v1.0
```

## Dockerfile 最佳实践

### 1. 多阶段构建

```dockerfile
# 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# 生产阶段
FROM node:18-alpine AS production

WORKDIR /app

# 只复制生产依赖
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

# 从构建阶段复制构建产物
COPY --from=builder /app/dist ./dist

USER node

EXPOSE 3000

CMD ["node", "dist/main.js"]
```

### 2. 层缓存优化

```dockerfile
# ❌ 不好：每次代码变更都会重新安装依赖
COPY . .
RUN npm install

# ✅ 好：利用层缓存
COPY package*.json ./
RUN npm ci --only=production
COPY . .
```

### 3. 安全最佳实践

```dockerfile
# 使用非 root 用户
RUN addgroup -g 1001 -S nodejs
RUN adduser -S nextjs -u 1001

# 使用 distroless 镜像
FROM gcr.io/distroless/nodejs18-debian11

# 只复制必要的文件
COPY --chown=nextjs:nodejs ./public ./public
COPY --chown=nextjs:nodejs ./.next/standalone ./
COPY --chown=nextjs:nodejs ./.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT 3000

CMD ["server.js"]
```

## Spring Boot 应用容器化

### 1. 基础 Dockerfile

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 复制 Maven wrapper 和配置
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# 复制源码并构建
COPY src ./src
RUN ./mvnw clean package -DskipTests

# 生产镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 设置权限
RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 分层构建（Spring Boot 2.3+）

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS builder

WORKDIR /app

COPY target/*.jar application.jar

# 提取分层
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 按层复制，优化缓存
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### 3. Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/luomiblog
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
    depends_on:
      - db
      - redis
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
      - MYSQL_DATABASE=luomiblog
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    networks:
      - app-network
    restart: unless-stopped
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    networks:
      - app-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - app
    networks:
      - app-network
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:

networks:
  app-network:
    driver: bridge
```

## 前端应用容器化

### 1. Astro 应用 Dockerfile

```dockerfile
# 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

# 安装依赖
COPY package*.json ./
RUN npm ci

# 复制源码
COPY . .

# 构建
RUN npm run build

# 生产阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 2. Nginx 配置

```nginx
# nginx.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://app:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

## CI/CD 集成

### GitHub Actions 工作流

```yaml
# .github/workflows/docker-build.yml
name: Docker Build and Push

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout
      uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Build with Maven
      run: ./mvnw clean package -DskipTests

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3

    - name: Login to Docker Hub
      uses: docker/login-action@v3
      with:
        username: ${{ secrets.DOCKER_USERNAME }}
        password: ${{ secrets.DOCKER_PASSWORD }}

    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ secrets.DOCKER_USERNAME }}/luomiblog
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=semver,pattern={{version}}
          type=semver,pattern={{major}}.{{minor}}

    - name: Build and push
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
```

## 生产环境最佳实践

### 1. 资源限制

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 2. 日志管理

```yaml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 3.  secrets 管理

```bash
# 创建 secret
echo "mysecretpassword" | docker secret create db_password -

# 在 compose 中使用
version: '3.8'
services:
  db:
    image: mysql:8.0
    secrets:
      - db_password
    environment:
      MYSQL_ROOT_PASSWORD_FILE: /run/secrets/db_password

secrets:
  db_password:
    external: true
```

## 常用命令速查

```bash
# 镜像管理
docker images                    # 列出镜像
docker rmi image:tag            # 删除镜像
docker build -t name:tag .      # 构建镜像
docker tag old:tag new:tag      # 标记镜像

# 容器管理
docker ps -a                     # 列出所有容器
docker run -d -p 80:80 nginx    # 运行容器
docker stop container_id        # 停止容器
docker rm container_id          # 删除容器
docker logs -f container_id     # 查看日志
docker exec -it container_id sh # 进入容器

# 数据卷
docker volume ls                 # 列出卷
docker volume create myvol      # 创建卷
docker volume rm myvol          # 删除卷

# 网络
docker network ls                # 列出网络
docker network create mynet     # 创建网络

# Compose
docker-compose up -d             # 后台启动
docker-compose down              # 停止并删除
docker-compose logs -f           # 查看日志
docker-compose build             # 重新构建
docker-compose ps                # 查看状态
```

## 总结

Docker 容器化部署带来了诸多好处：

1. **环境一致性**：开发、测试、生产环境完全一致
2. **快速部署**：秒级启动，快速扩缩容
3. **资源高效**：相比虚拟机，资源利用率更高
4. **易于维护**：版本控制、回滚方便

掌握 Docker 技术，将让你的应用部署更加专业和高效。

---

**参考资源**：
- [Docker 官方文档](https://docs.docker.com/)
- [Dockerfile 最佳实践](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Spring Boot Docker](https://spring.io/guides/topicals/spring-boot-docker/)
