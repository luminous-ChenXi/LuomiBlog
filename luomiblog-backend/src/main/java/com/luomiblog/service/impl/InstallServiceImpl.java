package com.luomiblog.service.impl;

import com.luomiblog.dto.install.*;
import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.InstallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InstallServiceImpl implements InstallService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String INSTALL_LOCK_FILE = "install.lock";
    private static final String CUSTOM_CONFIG_FILE = "config/custom-application.yml";

    @Override
    public InstallStatusResponse getInstallStatus() {
        boolean locked = isInstallLocked();
        boolean hasData = userRepository.count() > 0;

        // 安全策略：
        // 1. 如果有 install.lock 文件，认为已安装完成
        // 2. 如果没有 install.lock 但有数据，需要二次验证才能重新安装
        // 3. 如果都没有，可以正常安装
        if (locked) {
            return InstallStatusResponse.builder()
                    .installed(true)
                    .locked(true)
                    .hasData(true)
                    .message("系统已安装完成")
                    .build();
        }

        if (hasData) {
            return InstallStatusResponse.builder()
                    .installed(false)
                    .locked(false)
                    .hasData(true)
                    .message("系统已有数据，需要验证才能重新安装")
                    .build();
        }

        return InstallStatusResponse.builder()
                .installed(false)
                .locked(false)
                .hasData(false)
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
        try (Connection connection = createDataSource(request).getConnection()) {
            // 检查 MySQL 版本
            DatabaseMetaData metaData = connection.getMetaData();
            String version = metaData.getDatabaseProductVersion();
            int majorVersion = metaData.getDatabaseMajorVersion();

            // MySQL 8.0 或更高版本
            if (majorVersion < 8) {
                log.error("MySQL 版本过低: {}，需要 8.0 或更高版本", version);
                return false;
            }

            log.info("数据库连接成功，MySQL 版本: {}", version);
            return true;
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            return false;
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
    @Transactional
    public void executeSqlScripts(DatabaseConfigRequest request) {
        try {
            // 执行 schema.sql
            executeSqlFile("db/schema.sql");
            // 执行 data.sql
            executeSqlFile("db/data.sql");
            log.info("SQL 脚本执行成功");
        } catch (Exception e) {
            log.error("SQL 脚本执行失败", e);
            throw new RuntimeException("SQL 脚本执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void createAdminAccount(AdminAccountRequest request) {
        // 检查是否已存在管理员
        if (userRepository.count() > 0) {
            throw new RuntimeException("管理员账号已存在");
        }

        // 获取 admin 角色
        Role adminRole = roleRepository.findByCode("admin")
                .orElseThrow(() -> new RuntimeException("admin 角色不存在，请先执行 SQL 脚本"));

        // 创建管理员账号
        User admin = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .roleId(adminRole.getId())
                .status("active")
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(admin);
        log.info("管理员账号创建成功: {}", request.getUsername());
    }

    @Override
    public void saveSiteConfig(SiteConfigRequest request) {
        try {
            // 确保 config 目录存在
            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // 生成自定义配置文件
            String configContent = generateCustomConfig(request);
            try (FileWriter writer = new FileWriter(CUSTOM_CONFIG_FILE)) {
                writer.write(configContent);
            }

            log.info("站点配置保存成功");
        } catch (IOException e) {
            log.error("站点配置保存失败", e);
            throw new RuntimeException("站点配置保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void completeInstallation() {
        try {
            // 创建安装锁文件
            File lockFile = new File(INSTALL_LOCK_FILE);
            if (!lockFile.exists()) {
                lockFile.createNewFile();
            }
            log.info("安装完成，已创建安装锁");
        } catch (IOException e) {
            log.error("创建安装锁失败", e);
            throw new RuntimeException("安装完成操作失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyReinstallPermission(String verificationPassword) {
        if (verificationPassword == null || verificationPassword.isEmpty()) {
            return false;
        }

        try {
            // 获取管理员和博主角色
            Role adminRole = roleRepository.findByCode("admin").orElse(null);
            Role bloggerRole = roleRepository.findByCode("blogger").orElse(null);

            if (adminRole == null && bloggerRole == null) {
                log.warn("系统中未找到管理员或博主角色");
                return false;
            }

            // 验证逻辑：遍历所有管理员/博主用户，验证密码是否匹配任意一个
            // 这样多个管理员中的任何一个都可以验证通过
            Integer adminRoleId = adminRole != null ? adminRole.getId() : null;
            Integer bloggerRoleId = bloggerRole != null ? bloggerRole.getId() : null;

            // 获取所有管理员和博主用户
            boolean verified = userRepository.findAll().stream()
                    .filter(user -> {
                        Integer userRoleId = user.getRoleId();
                        return userRoleId != null &&
                               (userRoleId.equals(adminRoleId) || userRoleId.equals(bloggerRoleId));
                    })
                    .anyMatch(user -> {
                        boolean matches = passwordEncoder.matches(verificationPassword, user.getPasswordHash());
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
            // 删除安装锁文件
            File lockFile = new File(INSTALL_LOCK_FILE);
            if (lockFile.exists()) {
                lockFile.delete();
                log.info("已删除安装锁文件");
            }

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
    public boolean needsReinstallOptions() {
        // 当没有 install.lock 但有数据时，需要显示重新安装选项
        return !isInstallLocked() && userRepository.count() > 0;
    }

    @Override
    @Transactional
    public void executeReinstall(ReinstallOption option, DatabaseConfigRequest request) {
        log.info("执行重新安装，选项: {}", option.getName());

        switch (option) {
            case KEEP_DATA:
                // 保留数据，仅执行 schema.sql（使用 IF NOT EXISTS）
                // 不执行 data.sql，避免覆盖现有数据
                executeSchemaOnly();
                break;

            case UPDATE_SCHEMA:
                // 更新表结构，保留数据
                executeSchemaOnly();
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
    private void executeSchemaOnly() {
        try {
            executeSqlFile("db/schema.sql");
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

            // 重新执行所有脚本
            executeSqlScripts(request);

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
        return new File(INSTALL_LOCK_FILE).exists();
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
        dataSource.setUrl(String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                request.getHost(), request.getPort(), request.getDatabase()));
        dataSource.setUsername(request.getUsername());
        dataSource.setPassword(request.getPassword());
        return dataSource;
    }

    private void executeSqlFile(String resourcePath) throws IOException {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.warn("SQL 文件不存在: {}", resourcePath);
            return;
        }

        byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String sql = new String(bytes, StandardCharsets.UTF_8);

        // 分割 SQL 语句并执行
        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("/*")) {
                try {
                    jdbcTemplate.execute(trimmed);
                } catch (Exception e) {
                    log.warn("SQL 执行警告: {}", e.getMessage());
                    // 忽略已存在的表错误
                }
            }
        }
    }

    private String generateCustomConfig(SiteConfigRequest request) {
        return String.format("""
                # LuomiBlog 自定义配置文件（由安装向导生成）
                # 生成时间: %s
                # 警告: 此文件由系统自动生成，手动修改可能被覆盖

                blog:
                  name: %s
                  description: %s
                  theme: %s
                  language: %s
                  timezone: %s
                """,
                LocalDateTime.now(),
                request.getSiteName(),
                request.getSiteDescription() != null ? request.getSiteDescription() : "",
                request.getDefaultTheme() != null ? request.getDefaultTheme() : "auto",
                request.getDefaultLanguage() != null ? request.getDefaultLanguage() : "zh",
                request.getTimezone() != null ? request.getTimezone() : "Asia/Shanghai"
        );
    }
}
