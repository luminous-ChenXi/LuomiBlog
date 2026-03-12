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
        boolean hasAdmin = userRepository.count() > 0;

        return InstallStatusResponse.builder()
                .installed(locked || hasAdmin)
                .locked(locked)
                .message(locked ? "系统已安装" : (hasAdmin ? "系统已初始化但未锁定" : "系统未安装"))
                .build();
    }

    @Override
    public EnvironmentCheckResponse checkEnvironment() {
        List<EnvironmentCheckResponse.CheckItem> checks = new ArrayList<>();
        boolean allPassed = true;

        // 检查 Java 版本
        String javaVersion = System.getProperty("java.version");
        boolean javaVersionOk = javaVersion.startsWith("17") || javaVersion.startsWith("21");
        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("Java 版本")
                .passed(javaVersionOk)
                .message("当前 Java 版本: " + javaVersion)
                .suggestion(javaVersionOk ? null : "建议使用 Java 17 或 21")
                .build());
        allPassed &= javaVersionOk;

        // 检查端口占用（8080）
        boolean portAvailable = isPortAvailable(8080);
        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("端口 8080")
                .passed(portAvailable)
                .message(portAvailable ? "端口 8080 可用" : "端口 8080 被占用")
                .suggestion(portAvailable ? null : "请关闭占用 8080 端口的程序，或修改配置文件使用其他端口")
                .build());
        allPassed &= portAvailable;

        // 检查 MySQL 驱动
        boolean mysqlDriverOk = checkMysqlDriver();
        checks.add(EnvironmentCheckResponse.CheckItem.builder()
                .name("MySQL 驱动")
                .passed(mysqlDriverOk)
                .message(mysqlDriverOk ? "MySQL 驱动已加载" : "MySQL 驱动未找到")
                .suggestion(mysqlDriverOk ? null : "请检查依赖配置")
                .build());
        allPassed &= mysqlDriverOk;

        return EnvironmentCheckResponse.builder()
                .allPassed(allPassed)
                .checks(checks)
                .build();
    }

    @Override
    public boolean testDatabaseConnection(DatabaseConfigRequest request) {
        try {
            DataSource dataSource = createDataSource(request);
            dataSource.getConnection().close();
            return true;
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            return false;
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

    private boolean isInstallLocked() {
        return new File(INSTALL_LOCK_FILE).exists();
    }

    private boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
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
