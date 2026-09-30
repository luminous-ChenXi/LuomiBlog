package com.luomiblog.service.impl;

import com.luomiblog.dto.install.*;
import com.luomiblog.entity.Role;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.InstallService;
import com.luomiblog.service.LoginSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InstallServiceImpl implements InstallService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final LoginSecurityService loginSecurityService;
    private final com.luomiblog.service.SiteSettingsService siteSettingsService;
    private final com.luomiblog.security.InstallLockLocator installLockLocator;

    private static final String CUSTOM_CONFIG_FILE = "config/custom-application.yml";
    /** 半安装过程标记（装库开始写入、完成安装/重置时清理） */
    private static final String INSTALL_PROGRESS_MARKER = "config/install-in-progress.flag";

    /** 重新安装验证限流：容量 5，每分钟恢复 */
    private static final int REINSTALL_VERIFY_CAPACITY = 5;
    private static final long REINSTALL_VERIFY_REFILL_INTERVAL_MS = 60_000;
    /** 限流桶缓存上限，防止 Map 无限增长 */
    private static final int MAX_RATE_LIMITER_ENTRIES = 10_000;
    /** 重新安装验证锁定标识前缀（复用 LoginSecurityService 的失败计数/锁定机制） */
    private static final String REINSTALL_VERIFY_LOCK_PREFIX = "install-verify:";

    /** 重新安装验证限流桶（IP -> 桶） */
    private final Map<String, ReinstallVerifyBucket> reinstallVerifyBuckets = new ConcurrentHashMap<>();

    @Override
    public InstallStatusResponse getInstallStatus() {
        boolean locked = isInstallLocked();
        boolean hasData = false;

        // 检查是否有数据（捕获异常，因为数据库可能未配置）
        try {
            hasData = userRepository.count() > 0;
        } catch (Exception e) {
            // 数据库未配置或连接失败，认为没有数据
            log.debug("无法检查用户数据，数据库可能未配置: {}", e.getMessage());
            hasData = false;
        }

        // 安全策略：
        // 1. 如果有 install.lock 文件，认为已安装完成
        // 2. 如果没有 install.lock 但有数据，需要二次验证才能重新安装
        // 3. 如果都没有，可以正常安装
        if (locked) {
            return InstallStatusResponse.builder()
                    .installed(true)
                    .locked(true)
                    .hasData(true)
                    .inProgress(false)
                    .message("系统已安装完成")
                    .build();
        }

        if (hasData) {
            return InstallStatusResponse.builder()
                    .installed(false)
                    .locked(false)
                    .hasData(true)
                    .inProgress(isInProgress())
                    .message("系统已有数据，需要验证才能重新安装")
                    .build();
        }

        return InstallStatusResponse.builder()
                .installed(false)
                .locked(false)
                .hasData(false)
                .inProgress(isInProgress())
                .message("系统未安装")
                .build();
    }

    @Override
    public EnvironmentCheckResponse checkEnvironment() {
        List<EnvironmentCheckResponse.CheckItem> checks = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        boolean allPassed = true;

        logs.add("[INFO] 开始环境检测...");
        logs.add("[INFO] 检测时间: " + LocalDateTime.now());
        logs.add("[INFO] 操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));

        // 检查 Java 版本
        logs.add("[INFO] 正在检查 Java 版本...");
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        int majorVersion = parseJavaVersion(javaVersion);
        boolean javaVersionOk = majorVersion >= 17;

        List<String> javaDetails = new ArrayList<>();
        javaDetails.add("Java 版本: " + javaVersion);
        javaDetails.add("Java 厂商: " + javaVendor);
        javaDetails.add("主版本号: " + majorVersion);

        if (javaVersionOk) {
            logs.add("[INFO] ✓ Java 版本检查通过: " + javaVersion);
        } else {
            logs.add("[ERROR] ✗ Java 版本过低: " + javaVersion + "，需要 Java 17+");
        }

        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("Java 版本")
                .passed(javaVersionOk)
                .message("当前 Java 版本: " + javaVersion)
                .suggestion(javaVersionOk ? null : "需要 Java 17 或更高版本")
                .details(javaDetails)
                .build());
        allPassed &= javaVersionOk;

        // 检查后端服务配置
        logs.add("[INFO] 正在检查后端服务配置...");
        boolean backendConfigOk = checkBackendConfiguration();

        List<String> backendDetails = new ArrayList<>();
        backendDetails.add("服务状态: " + (backendConfigOk ? "运行中" : "异常"));
        backendDetails.add("配置文件: application.yml");

        if (backendConfigOk) {
            logs.add("[INFO] ✓ 后端服务运行正常");
        } else {
            logs.add("[ERROR] ✗ 后端服务配置异常");
        }

        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("后端服务")
                .passed(backendConfigOk)
                .message(backendConfigOk ? "后端服务运行正常" : "后端服务配置异常")
                .suggestion(backendConfigOk ? null : "请确保后端服务已正确启动")
                .details(backendDetails)
                .build());
        allPassed &= backendConfigOk;

        // 检查 MySQL 驱动
        logs.add("[INFO] 正在检查 MySQL 驱动...");
        boolean mysqlDriverOk = checkMysqlDriver();

        List<String> driverDetails = new ArrayList<>();
        driverDetails.add("驱动类: com.mysql.cj.jdbc.Driver");
        driverDetails.add("驱动状态: " + (mysqlDriverOk ? "已加载" : "未找到"));

        if (mysqlDriverOk) {
            logs.add("[INFO] ✓ MySQL 驱动已加载");
        } else {
            logs.add("[ERROR] ✗ MySQL 驱动未找到");
        }

        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("MySQL 驱动")
                .passed(mysqlDriverOk)
                .message(mysqlDriverOk ? "MySQL 驱动已加载" : "MySQL 驱动未找到")
                .suggestion(mysqlDriverOk ? null : "请检查依赖配置")
                .details(driverDetails)
                .build());
        allPassed &= mysqlDriverOk;

        // 检查应用磁盘可写（上传目录）
        logs.add("[INFO] 正在检查应用磁盘可写性（上传目录）...");
        boolean diskWritableOk = checkUploadDirectoryWritable();
        List<String> diskDetails = new ArrayList<>();
        diskDetails.add("上传目录: uploads/");
        diskDetails.add("可写状态: " + (diskWritableOk ? "可写" : "不可写"));
        if (diskWritableOk) {
            logs.add("[INFO] ✓ 上传目录可写");
        } else {
            logs.add("[ERROR] ✗ 上传目录不可写");
        }
        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("磁盘可写")
                .passed(diskWritableOk)
                .message(diskWritableOk ? "应用磁盘可写（上传目录读写正常）" : "应用磁盘不可写，文件上传功能将不可用")
                .suggestion(diskWritableOk ? null : "请检查应用工作目录的文件系统权限")
                .details(diskDetails)
                .build());
        allPassed &= diskWritableOk;

        // SMTP 配置检测（只展示，不阻塞安装）
        logs.add("[INFO] 正在检查 SMTP 配置...");
        boolean smtpConfigured = smtpConfigured();
        List<String> smtpDetails = new ArrayList<>();
        smtpDetails.add(smtpConfigured ? "SMTP 已配置，可发送邮箱验证/通知邮件" : "SMTP 未配置，安装后可在后台系统设置中配置");
        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("SMTP 配置")
                .passed(true)
                .blocking(false)
                .message(smtpConfigured ? "已配置" : "未配置（不阻塞安装，可稍后在管理后台配置）")
                .suggestion(smtpConfigured ? null : "如需注册邮箱验证/邮件通知，请在管理后台配置 SMTP")
                .details(smtpDetails)
                .build());
        if (smtpConfigured) {
            logs.add("[INFO] ✓ SMTP 已配置");
        } else {
            logs.add("[WARN] SMTP 未配置（不阻塞安装）");
        }

        // 检查安装状态
        logs.add("[INFO] 正在检查安装状态...");
        InstallStatusResponse status = getInstallStatus();
        logs.add("[INFO] 安装状态: " + status.getMessage());
        if (status.isLocked()) {
            logs.add("[WARN] 系统已安装完成，install.lock 存在");
        } else if (status.isHasData()) {
            logs.add("[WARN] 检测到已有数据，可能需要重新安装验证");
        } else {
            logs.add("[INFO] 系统未安装，可以进行全新安装");
        }

        logs.add("[INFO] 环境检测完成，结果: " + (allPassed ? "通过" : "未通过"));

        return EnvironmentCheckResponse.builder()
                .allPassed(allPassed)
                .checks(checks)
                .logs(logs)
                .build();
    }

    @Override
    public boolean testDatabaseConnection(DatabaseConfigRequest request) {
        return testDatabaseDetailed(request).isSuccess();
    }

    @Override
    public DatabaseTestResponse testDatabaseDetailed(DatabaseConfigRequest request) {
        try (Connection connection = createDataSource(request).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String version = metaData.getDatabaseProductVersion();
            int majorVersion = metaData.getDatabaseMajorVersion();

            if (majorVersion < 8) {
                log.error("MySQL 版本过低: {}，需要 8.0 或更高版本", version);
                return DatabaseTestResponse.builder()
                        .success(false)
                        .message("MySQL 版本过低（" + version + "），需要 8.0 或更高版本")
                        .mysqlVersion(version)
                        .errorType("VERSION_TOO_LOW")
                        .build();
            }

            // 读取服务器字符集（读取失败不影响连接成功判定）
            String charset = null;
            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("SHOW VARIABLES LIKE 'character_set_server'")) {
                if (rs.next()) {
                    charset = rs.getString(2);
                }
            } catch (Exception ignore) {
                log.debug("读取服务器字符集失败: {}", ignore.getMessage());
            }

            log.info("数据库连接成功，MySQL 版本: {}, 字符集: {}", version, charset);
            return DatabaseTestResponse.builder()
                    .success(true)
                    .message("数据库连接成功")
                    .mysqlVersion(version)
                    .characterSet(charset)
                    .build();
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            return classifyDatabaseError(e, request);
        }
    }

    /**
     * 将连接异常翻译为具体原因分类（连接拒绝 / 认证失败 / 库不存在 / 其他）
     */
    private DatabaseTestResponse classifyDatabaseError(Exception e, DatabaseConfigRequest request) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String lower = msg.toLowerCase();
        String cause = e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage().toLowerCase() : "";

        if (lower.contains("access denied") || cause.contains("access denied")) {
            return DatabaseTestResponse.builder()
                    .success(false)
                    .errorType("AUTH_FAILED")
                    .message("认证失败：用户名或密码错误（或该用户无权从当前主机访问）")
                    .build();
        }
        if (lower.contains("unknown database") || cause.contains("unknown database")) {
            return DatabaseTestResponse.builder()
                    .success(false)
                    .errorType("DATABASE_NOT_EXISTS")
                    .databaseMissing(true)
                    .message("数据库 " + request.getDatabase() + " 不存在，可尝试使用\"创建数据库\"功能（需要建库权限）")
                    .build();
        }
        if (lower.contains("communications link failure") || lower.contains("connection refused")
                || lower.contains("connect timed out") || lower.contains("connection timed out")
                || lower.contains("unknownhostexception") || cause.contains("connection refused")
                || cause.contains("unknownhost")) {
            return DatabaseTestResponse.builder()
                    .success(false)
                    .errorType("CONNECTION_REFUSED")
                    .message("连接被拒绝：无法连接到 " + request.getHost() + ":" + request.getPort()
                            + "，请确认数据库地址/端口正确、服务已启动且防火墙放行")
                    .build();
        }
        return DatabaseTestResponse.builder()
                .success(false)
                .errorType("UNKNOWN")
                .message("数据库连接失败: " + msg)
                .build();
    }

    @Override
    public String createDatabase(DatabaseConfigRequest request) {
        log.info("尝试创建数据库: {}@{}:{}/{}", request.getUsername(), request.getHost(), request.getPort(), request.getDatabase());
        try (Connection connection = createServerDataSource(request).getConnection();
             var stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + request.getDatabase()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            log.info("数据库创建成功（或已存在）: {}", request.getDatabase());
            return "数据库 " + request.getDatabase() + " 已创建（或已存在），请重新测试连接";
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.toLowerCase().contains("access denied")) {
                throw new RuntimeException("创建数据库失败：当前账号没有建库权限（需要 CREATE 权限），请使用管理员账号或手动建库");
            }
            throw new RuntimeException("创建数据库失败: " + msg, e);
        }
    }

    @Override
    public DatabaseCheckResponse checkDatabase(DatabaseConfigRequest request) {
        List<String> logs = new ArrayList<>();
        logs.add("[INFO] 开始检查数据库连接...");
        logs.add("[INFO] 目标数据库: " + request.getHost() + ":" + request.getPort() + "/" + request.getDatabase());

        try (Connection connection = createDataSource(request).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String version = metaData.getDatabaseProductVersion();
            int majorVersion = metaData.getDatabaseMajorVersion();

            logs.add("[INFO] 数据库连接成功");
            logs.add("[INFO] MySQL 版本: " + version);
            logs.add("[INFO] 数据库名称: " + request.getDatabase());

            // 检查 MySQL 版本
            if (majorVersion < 8) {
                logs.add("[ERROR] ✗ MySQL 版本过低: " + majorVersion + "，需要 8.0+");
                return DatabaseCheckResponse.builder()
                        .connected(true)
                        .message("MySQL 版本过低，需要 8.0 或更高版本")
                        .mysqlVersion(version)
                        .databaseName(request.getDatabase())
                        .hasExistingData(false)
                        .needsReinstallOptions(false)
                        .logs(logs)
                        .build();
            }

            logs.add("[INFO] ✓ MySQL 版本检查通过");

            // 检查是否已有数据
            logs.add("[INFO] 正在检查数据库中是否已有数据...");
            List<String> existingTables = new ArrayList<>();
            boolean hasExistingData = false;

            try {
                // 查询数据库中的表
                java.sql.ResultSet tables = metaData.getTables(request.getDatabase(), null, "%", new String[]{"TABLE"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    existingTables.add(tableName);
                }
                tables.close();

                // 检查关键表是否存在
                boolean hasUsersTable = existingTables.stream()
                        .anyMatch(t -> t.equalsIgnoreCase("users") || t.equalsIgnoreCase("user"));
                boolean hasArticlesTable = existingTables.stream()
                        .anyMatch(t -> t.equalsIgnoreCase("articles") || t.equalsIgnoreCase("article"));

                hasExistingData = !existingTables.isEmpty();

                if (hasExistingData) {
                    logs.add("[WARN] 检测到 " + existingTables.size() + " 个现有表");
                    logs.add("[WARN] 关键表 - 用户表: " + (hasUsersTable ? "存在" : "不存在"));
                    logs.add("[WARN] 关键表 - 文章表: " + (hasArticlesTable ? "存在" : "不存在"));

                    // 检查是否有用户数据
                    if (hasUsersTable) {
                        try {
                            JdbcTemplate template = new JdbcTemplate(createDataSource(request));
                            Integer userCount = template.queryForObject(
                                    "SELECT COUNT(*) FROM " + existingTables.stream()
                                            .filter(t -> t.equalsIgnoreCase("users") || t.equalsIgnoreCase("user"))
                                            .findFirst().orElse("users"),
                                    Integer.class
                            );
                            logs.add("[WARN] 现有用户数量: " + userCount);
                        } catch (Exception e) {
                            logs.add("[WARN] 无法读取用户数量: " + e.getMessage());
                        }
                    }
                } else {
                    logs.add("[INFO] 数据库为空，可以进行全新安装");
                }

            } catch (Exception e) {
                logs.add("[WARN] 检查表信息时出错: " + e.getMessage());
            }

            logs.add("[INFO] 数据库检查完成");

            return DatabaseCheckResponse.builder()
                    .connected(true)
                    .message("数据库连接成功")
                    .mysqlVersion(version)
                    .databaseName(request.getDatabase())
                    .hasExistingData(hasExistingData)
                    .existingDataMessage(hasExistingData ?
                            "检测到数据库中已有 " + existingTables.size() + " 个表，可能包含现有数据" :
                            "数据库为空")
                    .existingTables(existingTables)
                    .needsReinstallOptions(hasExistingData && !isInstallLocked())
                    .logs(logs)
                    .build();

        } catch (Exception e) {
            logs.add("[ERROR] 数据库连接失败: " + e.getMessage());
            return DatabaseCheckResponse.builder()
                    .connected(false)
                    .message("数据库连接失败: " + e.getMessage())
                    .hasExistingData(false)
                    .needsReinstallOptions(false)
                    .logs(logs)
                    .build();
        }
    }

    @Override
    public void executeSqlScripts(DatabaseConfigRequest request) {
        log.info("开始执行 SQL 脚本，数据库: {}@{}:{}/{}",
            request.getUsername(), request.getHost(), request.getPort(), request.getDatabase());

        // 写入半安装标记（完成安装/重置安装状态时清理）
        writeProgressMarker();

        // 使用用户配置的数据源执行 SQL 脚本
        DataSource dataSource = createDataSource(request);
        JdbcTemplate template = new JdbcTemplate(dataSource);

        try {
            // 执行 schema.sql
            executeSqlFile("db/schema.sql", template);
            // 执行 data.sql
            executeSqlFile("db/data.sql", template);
            log.info("SQL 脚本执行成功");

            // 验证表是否创建成功
            try {
                Integer tableCount = template.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                    Integer.class
                );
                if (tableCount == null || tableCount == 0) {
                    throw new RuntimeException("验证失败：users 表未创建成功");
                }
                log.info("验证成功：users 表已创建");
            } catch (Exception e) {
                log.error("验证表创建失败", e);
                throw new RuntimeException("验证表创建失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("SQL 脚本执行失败", e);
            throw new RuntimeException("SQL 脚本执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void createAdminAccount(AdminAccountRequest request) {
        // 使用 JDBC 直接执行 SQL，不依赖 JPA Repository
        try {
            // 检查 users 表是否存在
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                Integer.class
            );
            if (tableCount == null || tableCount == 0) {
                throw new RuntimeException("数据库表不存在，请先执行 SQL 脚本初始化数据库");
            }

            // 检查是否已存在管理员
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Integer.class
            );
            if (userCount != null && userCount > 0) {
                throw new RuntimeException("管理员账号已存在");
            }

            // 获取 admin 角色 ID
            Long adminRoleId;
            try {
                adminRoleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM roles WHERE code = 'admin'",
                    Long.class
                );
            } catch (Exception e) {
                throw new RuntimeException("admin 角色不存在，请先执行 SQL 脚本");
            }

            // 使用 JDBC 直接插入管理员账号（totp_enabled 显式给默认值，兼容 Hibernate 建表无列默认值的情况）
            String sql = "INSERT INTO users (username, email, password, nickname, role_id, status, email_verified, totp_enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, false, NOW(), NOW())";
            String passwordHash = passwordEncoder.encode(request.getPassword());
            String nickname = request.getNickname() != null ? request.getNickname() : request.getUsername();

            jdbcTemplate.update(sql,
                request.getUsername(),
                request.getEmail(),
                passwordHash,
                nickname,
                adminRoleId,
                "active",
                true
            );

            log.info("管理员账号创建成功: {}", request.getUsername());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建管理员账号失败", e);
            throw new RuntimeException("创建管理员账号失败: " + e.getMessage());
        }
    }

    @Override
    public String checkReadyForCompletion() {
        try {
            // 前置 1：装库已完成（execute-sql 步骤的产物：users 表存在）
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                Integer.class
            );
            if (tableCount == null || tableCount == 0) {
                return "安装流程未完成：数据库尚未初始化（users 表不存在），请先完成「初始化数据」步骤";
            }
            // 前置 2：管理员账号已创建（users 表存在 admin 角色账号）
            if (!hasAdminAccount()) {
                return "安装流程未完成：管理员账号尚未创建，请先完成「创建管理员」步骤";
            }
            return null;
        } catch (com.luomiblog.common.exception.BusinessException e) {
            // 数据库暂不可用（hasAdminAccount fail closed）：原样上抛保留 503 语义，
            // 绝不能被下面的兜底 catch 吞成"未完成安装"
            throw e;
        } catch (Exception e) {
            log.warn("完成安装前置校验失败: {}", e.getMessage());
            return "安装流程未完成：数据库尚未初始化或无法访问，请先完成「初始化数据」步骤";
        }
    }

    /**
     * users 表中是否已存在 admin 角色账号。
     * 数据库不可用/查询失败时抛 503 业务异常（fail closed）：
     * 上层 canResetInstallState / 完成安装前置校验据此拒绝操作，
     * 防止瞬时数据库故障期间被误判为"零管理员"，导致匿名接口删除 install.lock。
     */
    private boolean hasAdminAccount() {
        Integer adminCount;
        try {
            adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN roles r ON u.role_id = r.id WHERE r.code = 'admin'",
                Integer.class
            );
        } catch (DataAccessException e) {
            log.warn("检查管理员账号失败（数据库暂不可用?）: {}", e.getMessage());
            throw new com.luomiblog.common.exception.BusinessException(
                    503, "数据库暂不可用，无法校验管理员账户，请稍后重试");
        }
        return adminCount != null && adminCount > 0;
    }

    @Override
    public boolean canResetInstallState() {
        // 未锁定（未完成/半安装）→ 允许；
        // 异常锁死态（install.lock 存在但无任何 ADMIN 账号，verify-reinstall 不可达）→ 允许恢复；
        // 正常已安装（锁定且存在管理员）→ 不允许，必须走 verify-reinstall 流程。
        // 数据库不可用时 hasAdminAccount 抛 503（fail closed）：直接向上抛，
        // 由 GlobalExceptionHandler 返回"暂不可用"，绝不放行匿名重置
        return !isInstallLocked() || !hasAdminAccount();
    }

    @Override
    public void saveSiteConfig(SiteConfigRequest request) {
        // WordPress 式：站点配置落 site_settings 键值表，/api/site/config 实时读取，立即生效且重启持久
        Map<String, String> toSave = new java.util.LinkedHashMap<>();
        toSave.put(com.luomiblog.service.SiteSettingsService.KEY_SITE_NAME, request.getSiteName());
        toSave.put(com.luomiblog.service.SiteSettingsService.KEY_SITE_DESCRIPTION,
                request.getSiteDescription() != null ? request.getSiteDescription() : "");
        toSave.put(com.luomiblog.service.SiteSettingsService.KEY_SITE_DEFAULT_THEME,
                request.getDefaultTheme() != null ? request.getDefaultTheme() : "auto");
        toSave.put(com.luomiblog.service.SiteSettingsService.KEY_SITE_DEFAULT_LANGUAGE,
                request.getDefaultLanguage() != null ? request.getDefaultLanguage() : "zh");
        toSave.put(com.luomiblog.service.SiteSettingsService.KEY_SITE_TIMEZONE,
                request.getTimezone() != null ? request.getTimezone() : "Asia/Shanghai");
        try {
            siteSettingsService.setAll(toSave);
            log.info("站点配置保存成功（已落 site_settings）: siteName={}", request.getSiteName());
        } catch (Exception e) {
            log.error("站点配置保存失败", e);
            throw new RuntimeException("站点配置保存失败: " + e.getMessage()
                    + "（请确认已先完成「初始化数据」步骤）", e);
        }
    }

    @Override
    public void completeInstallation() {
        // 前置状态校验：装库完成 + 管理员已创建，缺一步都不允许锁定系统
        String missingStep = checkReadyForCompletion();
        if (missingStep != null) {
            log.warn("拒绝完成安装: {}", missingStep);
            throw new com.luomiblog.common.exception.InstallNotReadyException(missingStep);
        }
        try {
            // 创建安装锁文件（路径由 app.install.lock-file 配置，默认 config/install.lock）
            Path lockPath = installLockLocator.resolve();
            if (!Files.exists(lockPath)) {
                if (lockPath.getParent() != null) {
                    Files.createDirectories(lockPath.getParent());
                }

                // 写入提示性文字（英文+中文）
                String lockContent = generateInstallLockContent();
                try (FileWriter writer = new FileWriter(lockPath.toFile(), StandardCharsets.UTF_8)) {
                    writer.write(lockContent);
                }
            }
            log.info("安装完成，已创建安装锁文件: {}", lockPath.toAbsolutePath());

            // 安装完成，清理半安装标记
            deleteProgressMarker();
        } catch (IOException e) {
            log.error("创建安装锁失败", e);
            throw new RuntimeException("安装完成操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成安装锁文件内容
     * 参考 WordPress 的 .maintenance 文件设计思想
     */
    private String generateInstallLockContent() {
        StringBuilder sb = new StringBuilder();
        String separator = "=".repeat(70);

        sb.append(separator).append("\n");
        sb.append("  LUOMIBLOG INSTALLATION LOCK FILE").append("\n");
        sb.append("  洛米博客安装锁定文件").append("\n");
        sb.append(separator).append("\n\n");

        sb.append("ENGLISH:").append("\n");
        sb.append("-".repeat(70)).append("\n");
        sb.append("This file indicates that LuomiBlog has been successfully installed.").append("\n");
        sb.append("DO NOT DELETE THIS FILE unless you want to reinstall the system.").append("\n\n");

        sb.append("WARNING:").append("\n");
        sb.append("- Deleting this file will expose the installation wizard to the public.").append("\n");
        sb.append("- This could allow unauthorized users to reconfigure your system.").append("\n");
        sb.append("- Only delete this file if you are performing a legitimate reinstallation.").append("\n\n");

        sb.append("If you need to reinstall:").append("\n");
        sb.append("1. Backup your database and files first").append("\n");
        sb.append("2. Use the admin panel's reinstall feature, OR").append("\n");
        sb.append("3. Manually delete this file and visit /install").append("\n\n");

        sb.append(separator).append("\n\n");

        sb.append("中文:").append("\n");
        sb.append("-".repeat(70)).append("\n");
        sb.append("此文件表示洛米博客已成功安装。").append("\n");
        sb.append("除非您要重新安装系统，否则请勿删除此文件！").append("\n\n");

        sb.append("警告:").append("\n");
        sb.append("- 删除此文件会将安装向导暴露给公众访问").append("\n");
        sb.append("- 这可能允许未授权用户重新配置您的系统").append("\n");
        sb.append("- 只有在执行合法重新安装时才删除此文件").append("\n\n");

        sb.append("如需重新安装:").append("\n");
        sb.append("1. 首先备份您的数据库和文件").append("\n");
        sb.append("2. 使用后台管理面板的重新安装功能，或").append("\n");
        sb.append("3. 手动删除此文件并访问 /install").append("\n\n");

        sb.append(separator).append("\n");
        sb.append("Installation Time / 安装时间: ").append(LocalDateTime.now()).append("\n");
        sb.append("System / 系统: LuomiBlog").append("\n");
        sb.append(separator).append("\n");

        return sb.toString();
    }

    @Override
    public boolean verifyReinstallPermission(String verificationPassword) {
        if (verificationPassword == null || verificationPassword.isEmpty()) {
            return false;
        }

        try {
            // 获取管理员和博主角色
            Role adminRole = roleRepository.findByCode(com.luomiblog.common.Roles.ADMIN).orElse(null);
            Role bloggerRole = roleRepository.findByCode(com.luomiblog.common.Roles.BLOGGER).orElse(null);

            if (adminRole == null && bloggerRole == null) {
                log.warn("系统中未找到管理员或博主角色");
                return false;
            }

            // 验证逻辑：遍历所有管理员/博主用户，验证密码是否匹配任意一个
            // 这样多个管理员中的任何一个都可以验证通过
            Long adminRoleId = adminRole != null ? adminRole.getId() : null;
            Long bloggerRoleId = bloggerRole != null ? bloggerRole.getId() : null;

            boolean verified = userRepository.findAll().stream()
                    .filter(user -> {
                        Long userRoleId = user.getRoleId();
                        return userRoleId != null &&
                               (userRoleId.equals(adminRoleId) || userRoleId.equals(bloggerRoleId));
                    })
                    .anyMatch(user -> {
                        boolean matches = passwordEncoder.matches(verificationPassword, user.getPassword());
                        if (matches) {
                            log.info("重新安装权限验证通过：用户 '{}' 验证成功", user.getUsername());
                        }
                        return matches;
                    });

            if (!verified) {
                log.warn("重新安装权限验证失败：密码与任何管理员/博主账号不匹配");
            }

            return verified;
        } catch (Exception e) {
            log.error("验证重新安装权限时发生错误", e);
            return false;
        }
    }

    @Override
    public void resetInstallation() {
        try {
            // 删除安装锁文件（路径解析含旧版位置兼容）
            installLockLocator.deleteIfExists();
            log.info("已删除安装锁文件（如存在）");

            // 删除自定义配置文件
            File configFile = new File(CUSTOM_CONFIG_FILE);
            if (configFile.exists()) {
                configFile.delete();
                log.info("已删除自定义配置文件");
            }

            log.info("安装状态已重置");
        } catch (Exception e) {
            log.error("重置安装状态失败", e);
            throw new RuntimeException("重置安装状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void resetInstallState() {
        // 正常已安装（锁定且存在管理员）必须走 verify-reinstall 流程；
        // 异常锁死态（install.lock 存在但 users 表无任何 ADMIN，向导与 verify-reinstall 均不可达）放行，
        // 让正常安装能重来（对齐 canResetInstallState）
        if (!canResetInstallState()) {
            throw new RuntimeException("系统已安装，不允许重置安装状态；如需重装请先完成管理员验证");
        }
        boolean abnormalLocked = isInstallLocked();
        resetInstallation();
        // 清理半安装标记
        deleteProgressMarker();
        if (abnormalLocked) {
            log.warn("检测到异常锁死态（install.lock 存在但无任何管理员账号），已重置安装状态以供恢复");
        } else {
            log.info("半安装状态已重置（对齐 scripts/reset-install.ps1 逻辑）");
        }
    }

    @Override
    public boolean needsReinstallOptions() {
        // 当没有 install.lock 但有数据时，需要显示重新安装选项
        try {
            return !isInstallLocked() && userRepository.count() > 0;
        } catch (Exception e) {
            // 数据库未配置或连接失败，不需要显示重新安装选项
            log.debug("无法检查用户数据，数据库可能未配置: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void executeReinstall(ReinstallOption option, DatabaseConfigRequest request) {
        log.info("执行重新安装，选项: {}", option.getName());

        switch (option) {
            case KEEP_DATA:
                // 保留数据，仅执行 schema.sql（使用 IF NOT EXISTS）
                // 不执行 data.sql，避免覆盖现有数据
                executeSchemaOnly(request);
                break;

            case UPDATE_SCHEMA:
                // 更新表结构，保留数据
                executeSchemaOnly(request);
                break;

            case FRESH_INSTALL:
            default:
                // 全新安装：清空数据并重新执行所有脚本
                executeFreshInstall(request);
                break;
        }

        log.info("重新安装完成，选项: {}", option.getName());
    }

    /**
     * 仅执行 schema.sql（使用 IF NOT EXISTS，不会删除现有数据）
     */
    private void executeSchemaOnly(DatabaseConfigRequest request) {
        try {
            DataSource dataSource = createDataSource(request);
            JdbcTemplate template = new JdbcTemplate(dataSource);
            executeSqlFile("db/schema.sql", template);
            log.info("数据库结构更新完成（保留数据）");
        } catch (Exception e) {
            log.error("更新数据库结构失败", e);
            throw new RuntimeException("更新数据库结构失败: " + e.getMessage(), e);
        }
    }

    /**
     * 全新安装：清空所有数据
     */
    private void executeFreshInstall(DatabaseConfigRequest request) {
        try {
            // 警告：这会删除所有数据！
            log.warn("执行全新安装，将清空所有数据");

            // 获取当前数据源
            DataSource dataSource = createDataSource(request);
            JdbcTemplate template = new JdbcTemplate(dataSource);

            // 删除所有表（危险操作！）
            dropAllTables(template);

            // 重新执行所有脚本（使用同一个template）
            log.info("开始执行 SQL 脚本，数据库: {}@{}:{}/{}",
                request.getUsername(), request.getHost(), request.getPort(), request.getDatabase());

            // 执行 schema.sql
            executeSqlFile("db/schema.sql", template);
            // 执行 data.sql
            executeSqlFile("db/data.sql", template);

            // 验证表是否创建成功
            try {
                Integer tableCount = template.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                    Integer.class
                );
                if (tableCount == null || tableCount == 0) {
                    throw new RuntimeException("验证失败：users 表未创建成功");
                }
                log.info("验证成功：users 表已创建");
            } catch (Exception e) {
                log.error("验证表创建失败", e);
                throw new RuntimeException("验证表创建失败: " + e.getMessage());
            }

            log.info("全新安装完成");
        } catch (Exception e) {
            log.error("全新安装失败", e);
            throw new RuntimeException("全新安装失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除所有表（仅用于全新安装）
     */
    private void dropAllTables(JdbcTemplate template) {
        log.warn("正在删除所有数据库表...");

        // 禁用外键检查
        template.execute("SET FOREIGN_KEY_CHECKS = 0");

        // 获取所有表名
        List<String> tables = template.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
            String.class
        );

        // 删除每个表
        for (String table : tables) {
            try {
                template.execute("DROP TABLE IF EXISTS `" + table + "`");
                log.debug("已删除表: {}", table);
            } catch (Exception e) {
                log.warn("删除表 {} 失败: {}", table, e.getMessage());
            }
        }

        // 启用外键检查
        template.execute("SET FOREIGN_KEY_CHECKS = 1");

        log.warn("所有表已删除");
    }

    private boolean isInstallLocked() {
        return installLockLocator.exists();
    }

    /**
     * 半安装标记：装库开始时写入，完成安装/重置安装状态时清理
     */
    private boolean isInProgress() {
        return new File(INSTALL_PROGRESS_MARKER).exists();
    }

    private void writeProgressMarker() {
        try {
            Path marker = Paths.get(INSTALL_PROGRESS_MARKER);
            if (marker.getParent() != null && !Files.exists(marker.getParent())) {
                Files.createDirectories(marker.getParent());
            }
            Files.writeString(marker, "started=" + LocalDateTime.now(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("写入半安装标记失败: {}", e.getMessage());
        }
    }

    private void deleteProgressMarker() {
        try {
            File marker = new File(INSTALL_PROGRESS_MARKER);
            if (marker.exists()) {
                marker.delete();
                log.info("已清理半安装标记");
            }
        } catch (Exception e) {
            log.warn("清理半安装标记失败: {}", e.getMessage());
        }
    }

    /**
     * SMTP 是否已配置（自检报告项）：host 与 from 均填写即视为已配置，
     * 兼容无鉴权 SMTP（MailPit/内网中继）场景
     */
    private boolean smtpConfigured() {
        return siteSettingsService.isSmtpConfigured();
    }

    /**
     * 上传目录可写检测：尝试在 uploads/ 写入并删除临时文件
     */
    private boolean checkUploadDirectoryWritable() {
        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path probe = uploadDir.resolve(".write-probe-" + System.currentTimeMillis());
            Files.writeString(probe, "probe", StandardCharsets.UTF_8);
            Files.deleteIfExists(probe);
            return true;
        } catch (Exception e) {
            log.warn("上传目录可写检测失败: {}", e.getMessage());
            return false;
        }
    }

    private int parseJavaVersion(String version) {
        try {
            // 处理版本号格式如 "21.0.1" 或 "17.0.8"
            String[] parts = version.split("\\.");
            if (parts[0].equals("1")) {
                // 旧版本格式如 "1.8.0"
                return Integer.parseInt(parts[1]);
            }
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean checkBackendConfiguration() {
        // 检查关键配置是否正确加载
        try {
            // 检查数据库配置是否可用
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("后端服务配置检查失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkMysqlDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private DataSource createDataSource(DatabaseConfigRequest request) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull",
                request.getHost(), request.getPort(), request.getDatabase()));
        dataSource.setUsername(request.getUsername());
        dataSource.setPassword(request.getPassword());
        return dataSource;
    }

    /**
     * 服务器级数据源（不带库名），用于 CREATE DATABASE
     */
    private DataSource createServerDataSource(DatabaseConfigRequest request) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(String.format("jdbc:mysql://%s:%d/?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull",
                request.getHost(), request.getPort()));
        dataSource.setUsername(request.getUsername());
        dataSource.setPassword(request.getPassword());
        return dataSource;
    }

    private void executeSqlFile(String resourcePath, JdbcTemplate template) throws IOException {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.error("SQL 文件不存在: {}", resourcePath);
            throw new RuntimeException("SQL 文件不存在: " + resourcePath);
        }

        log.info("开始执行 SQL 文件: {}", resourcePath);

        // 使用 Spring 的 ScriptUtils 来执行 SQL 脚本，它内置了正确的 SQL 分割逻辑
        try {
            org.springframework.core.io.support.EncodedResource encodedResource =
                new org.springframework.core.io.support.EncodedResource(resource, StandardCharsets.UTF_8);
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                template.getDataSource().getConnection(),
                encodedResource,
                false,  // continueOnError
                true,   // ignoreFailedDrops
                org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_COMMENT_PREFIX,
                org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
            );
            log.info("SQL 文件执行成功: {}", resourcePath);
        } catch (Exception e) {
            log.error("SQL 文件执行失败: {}", resourcePath, e);
            throw new RuntimeException("SQL 脚本执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyLockIntegrity() {
        try {
            Path lockPath = installLockLocator.resolve();
            if (!Files.exists(lockPath)) {
                return false;
            }
            String hash = computeFileHash(lockPath);
            return hash != null;
        } catch (Exception e) {
            log.error("安装锁完整性校验失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getLockHash() {
        try {
            Path lockPath = installLockLocator.resolve();
            if (!Files.exists(lockPath)) {
                return null;
            }
            return computeFileHash(lockPath);
        } catch (Exception e) {
            log.error("获取安装锁哈希失败: {}", e.getMessage());
            return null;
        }
    }

    private String computeFileHash(Path path) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算文件哈希失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean tryAcquireReinstallVerifyAttempt(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) {
            return false;
        }
        if (reinstallVerifyBuckets.size() >= MAX_RATE_LIMITER_ENTRIES && !reinstallVerifyBuckets.containsKey(clientIp)) {
            // 简单的容量保护：满了就清空重建（极端情况下才发生）
            reinstallVerifyBuckets.clear();
        }
        return reinstallVerifyBuckets
                .computeIfAbsent(clientIp, k -> new ReinstallVerifyBucket())
                .tryConsume();
    }

    @Override
    public boolean isReinstallVerifyLocked(String clientIp) {
        return loginSecurityService.isLocked(REINSTALL_VERIFY_LOCK_PREFIX + clientIp);
    }

    @Override
    public void recordReinstallVerifyFailure(String clientIp) {
        loginSecurityService.recordFailedAttempt(REINSTALL_VERIFY_LOCK_PREFIX + clientIp);
    }

    /**
     * 重新安装验证限流桶（容量 5，每分钟恢复）
     */
    private static class ReinstallVerifyBucket {
        private int tokens = REINSTALL_VERIFY_CAPACITY;
        private long lastRefillTime = System.currentTimeMillis();

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= REINSTALL_VERIFY_REFILL_INTERVAL_MS) {
                int tokensToAdd = (int) (elapsed / REINSTALL_VERIFY_REFILL_INTERVAL_MS);
                tokens = Math.min(REINSTALL_VERIFY_CAPACITY, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
