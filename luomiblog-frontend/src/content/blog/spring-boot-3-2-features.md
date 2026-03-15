---
title: 'Spring Boot 3.2 新特性探索'
description: 'Spring Boot 3.2 带来了许多令人兴奋的新特性，包括虚拟线程支持、RestClient、JdbcClient 等，让 Java 开发更加高效和现代化。'
pubDate: 2026-02-28
author: '辰汐'
tags: ['Spring Boot', 'Java', '后端开发', '虚拟线程']
category: '后端开发'
cover: 'https://picsum.photos/seed/springboot/800/500'
views: 256
comments: 23
slug: 'spring-boot-3-2-features'
---

# Spring Boot 3.2 新特性探索

Spring Boot 3.2 是 2023 年底发布的一个重要版本，基于 Spring Framework 6.1 和 Java 21，带来了许多令人兴奋的新特性。本文将深入探讨这些新特性及其在实际开发中的应用。

## 1. 虚拟线程支持（Virtual Threads）

### 什么是虚拟线程？

虚拟线程是 Java 21 引入的轻量级线程实现，由 JVM 而非操作系统管理。与传统线程相比：

| 特性 | 平台线程 | 虚拟线程 |
|------|----------|----------|
| 内存占用 | ~1 MB | ~几百字节 |
| 创建速度 | 慢 | 快 |
| 数量限制 | 数千 | 数百万 |
| 调度 | 操作系统 | JVM |

### 在 Spring Boot 中启用虚拟线程

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

就这么简单！启用后，Spring Boot 会自动：
- 使用虚拟线程执行 `@Async` 方法
- 使用虚拟线程处理 Tomcat 请求
- 使用虚拟线程执行 `@Scheduled` 任务

### 实际性能对比

```java
@RestController
public class VirtualThreadController {
    
    @GetMapping("/virtual-thread-test")
    public String testVirtualThreads() throws InterruptedException {
        // 模拟 I/O 操作
        Thread.sleep(100);
        return "Current thread: " + Thread.currentThread();
    }
}
```

使用虚拟线程后，同样的硬件配置可以处理更多并发请求：

- **平台线程**：约 10,000 并发连接
- **虚拟线程**：超过 100,000 并发连接

## 2. RestClient - 现代化的 HTTP 客户端

### 为什么需要 RestClient？

`RestTemplate` 已经过时，`WebClient` 需要响应式编程。RestClient 提供了：

- 流畅的 API 设计
- 同步和异步支持
- 更好的错误处理

### 基本使用

```java
@Service
public class UserService {
    
    private final RestClient restClient;
    
    public UserService() {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.example.com")
            .defaultHeader("Authorization", "Bearer token")
            .build();
    }
    
    public User getUser(Long id) {
        return restClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .body(User.class);
    }
    
    public User createUser(User user) {
        return restClient.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(user)
            .retrieve()
            .body(User.class);
    }
}
```

### 错误处理

```java
public User getUserWithErrorHandling(Long id) {
    return restClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
            throw new UserNotFoundException("User not found: " + id);
        })
        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
            throw new ServiceUnavailableException("Service temporarily unavailable");
        })
        .body(User.class);
}
```

### 与 WebClient 的对比

```java
// RestClient - 同步
User user = restClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .body(User.class);

// WebClient - 响应式
Mono<User> userMono = webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .bodyToMono(User.class);
```

## 3. JdbcClient - 简化的数据库操作

### 什么是 JdbcClient？

JdbcClient 是对 JdbcTemplate 的现代封装，提供了更流畅的 API：

```java
@Service
public class ArticleService {
    
    private final JdbcClient jdbcClient;
    
    public ArticleService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
    
    public Article findById(Long id) {
        return jdbcClient.sql("SELECT * FROM articles WHERE id = ?")
            .param(id)
            .query(Article.class)
            .single();
    }
    
    public List<Article> findByCategory(String category) {
        return jdbcClient.sql("""
                SELECT * FROM articles 
                WHERE category = ? 
                ORDER BY created_at DESC
                """)
            .param(category)
            .query(Article.class)
            .list();
    }
    
    public int updateArticle(Long id, String title, String content) {
        return jdbcClient.sql("""
                UPDATE articles 
                SET title = ?, content = ?, updated_at = NOW() 
                WHERE id = ?
                """)
            .params(title, content, id)
            .update();
    }
}
```

### 命名参数支持

```java
public Article findBySlug(String slug) {
    return jdbcClient.sql("""
            SELECT * FROM articles 
            WHERE slug = :slug AND deleted_at IS NULL
            """)
        .param("slug", slug)
        .query(Article.class)
        .optional()
        .orElseThrow(() -> new ArticleNotFoundException(slug));
}
```

## 4. 改进的 Docker 支持

### 更好的镜像构建

Spring Boot 3.2 优化了 GraalVM 原生镜像的构建：

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <configuration>
        <imageName>myapp</imageName>
        <mainClass>com.example.Application</mainClass>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

构建命令：

```bash
./mvnw spring-boot:build-image -Pnative
```

### 启动时间对比

| 部署方式 | 启动时间 | 内存占用 |
|----------|----------|----------|
| JVM | ~3 秒 | ~200 MB |
| 原生镜像 | ~0.1 秒 | ~50 MB |

## 5. 其他重要改进

### 5.1 改进的日志关联

```java
@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    public void processOrder(Order order) {
        // 自动关联 traceId 和 spanId
        log.info("Processing order: {}", order.getId());
        // 输出: 2024-01-15 10:30:45 [traceId=abc123,spanId=def456] Processing order: 12345
    }
}
```

### 5.2 改进的测试容器支持

```java
@SpringBootTest
@Testcontainers
public class ArticleRepositoryTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Test
    void shouldSaveArticle() {
        Article article = new Article();
        article.setTitle("Test Article");
        article.setContent("Test content");
        
        Article saved = articleRepository.save(article);
        
        assertThat(saved.getId()).isNotNull();
    }
}
```

### 5.3 改进的配置属性绑定

```java
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
    String type,
    LocalPath local,
    CloudPath cloud
) {
    public record LocalPath(String root) {}
    public record CloudPath(String bucket, String region) {}
}
```

```yaml
app:
  storage:
    type: local
    local:
      root: /data/uploads
    cloud:
      bucket: my-bucket
      region: ap-northeast-1
```

## 6. 升级指南

### 从 Spring Boot 3.1 升级

1. **更新版本号**：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
```

2. **检查废弃的 API**：

```bash
./mvnw spring-boot:run -Dspring.boot.deprecation-level=error
```

3. **更新依赖**：

```bash
./mvnw versions:update-properties
```

### 常见问题

**Q: 虚拟线程是否适合所有场景？**
A: 不是。虚拟线程适合 I/O 密集型任务，对于 CPU 密集型任务，传统线程仍然更合适。

**Q: RestClient 会替代 WebClient 吗？**
A: 不会。RestClient 适合同步场景，WebClient 适合响应式编程。两者会共存。

**Q: 原生镜像有什么限制？**
A: 反射、动态代理、JNI 等需要额外配置。部分库可能不完全支持。

## 总结

Spring Boot 3.2 带来了许多令人兴奋的改进：

1. **虚拟线程**：大幅提升并发处理能力
2. **RestClient**：现代化的 HTTP 客户端
3. **JdbcClient**：简化的数据库操作
4. **原生镜像**：更快的启动时间和更低的内存占用
5. **改进的测试支持**：更好的测试容器集成

这些特性让 Spring Boot 在现代云原生环境中更加强大。建议新项目直接使用 3.2，现有项目也可以考虑升级以获得更好的性能和开发体验。

---

**参考资源**：
- [Spring Boot 3.2 Release Notes](https://spring.io/blog/2023/11/23/spring-boot-3-2-0-available-now)
- [Virtual Threads Documentation](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [Spring Boot Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2-Release-Notes)
