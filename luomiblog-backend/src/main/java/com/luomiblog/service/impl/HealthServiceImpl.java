package com.luomiblog.service.impl;

import com.luomiblog.dto.health.HealthCheckResponse;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康检查服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @Value("${app.version:unknown}")
    private String version;

    private static final String INSTALL_LOCK_FILE = "install.lock";

    @Override
    public HealthCheckResponse checkHealth() {
        log.debug("执行健康检查...");

        // 检查安装锁
        boolean installLock = isInstallLocked();

        // 检查数据库连接
        DatabaseCheckResult dbResult = checkDatabase();

        // 检查是否有数据
        boolean hasData = checkHasData(dbResult.isConnected());

        // 确定整体状态
        String status = determineOverallStatus(installLock, dbResult, hasData);

        // 生成人性化消息
        String message = generateMessage(status, installLock, dbResult.getStatus(), hasData);

        // 生成建议
        List<String> suggestions = generateSuggestions(status, installLock, dbResult, hasData);

        return HealthCheckResponse.builder()
                .status(status)
                .database(dbResult.getStatus())
                .installLock(installLock)
                .hasData(hasData)
                .version(version)
                .timestamp(LocalDateTime.now())
                .message(message)
                .suggestions(suggestions)
                .components(HealthCheckResponse.Components.builder()
                        .database(HealthCheckResponse.ComponentStatus.builder()
                                .status(dbResult.isConnected() ? "up" : "down")
                                .message(dbResult.getMessage())
                                .error(dbResult.getError())
                                .build())
                        .cache(HealthCheckResponse.ComponentStatus.builder()
                                .status("up")
                                .message("缓存服务正常")
                                .build())
                        .fileSystem(HealthCheckResponse.ComponentStatus.builder()
                                .status("up")
                                .message("文件系统正常")
                                .build())
                        .build())
                .build();
    }

    /**
     * 检查数据库连接状态
     */
    private DatabaseCheckResult checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            // 测试连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // 检查关键表是否存在
            boolean tablesExist = checkKeyTables();

            if (tablesExist) {
                return DatabaseCheckResult.success("数据库连接正常，所有表已创建");
            } else {
                return DatabaseCheckResult.successButNoTables("数据库连接正常，但缺少必要的表");
            }
        } catch (SQLException e) {
            log.warn("数据库连接失败: {}", e.getMessage());
            return DatabaseCheckResult.failed("无法连接到数据库", e.getMessage());
        } catch (Exception e) {
            log.warn("数据库检查失败: {}", e.getMessage());
            return DatabaseCheckResult.failed("数据库检查失败", e.getMessage());
        }
    }

    /**
     * 检查关键表是否存在
     */
    private boolean checkKeyTables() {
        try {
            // 检查 users 表是否存在
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                Integer.class
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否有数据（用户表是否有记录）
     */
    private boolean checkHasData(boolean dbConnected) {
        if (!dbConnected) {
            return false;
        }
        try {
            return userRepository.count() > 0;
        } catch (Exception e) {
            log.debug("无法检查用户数据: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 install.lock 文件是否存在
     */
    private boolean isInstallLocked() {
        return new File(INSTALL_LOCK_FILE).exists();
    }

    /**
     * 确定整体健康状态
     */
    private String determineOverallStatus(boolean installLock, DatabaseCheckResult dbResult, boolean hasData) {
        // 如果 install.lock 存在且数据库正常，认为是健康的
        if (installLock && dbResult.isConnected() && hasData) {
            return "healthy";
        }

        // 如果 install.lock 存在但数据库有问题，认为是降级状态
        if (installLock && !dbResult.isConnected()) {
            return "degraded";
        }

        // 如果没有 install.lock 但有数据，需要重新安装验证
        if (!installLock && hasData) {
            return "needs_reinstall";
        }

        // 如果都没有，是未安装状态
        if (!installLock && !hasData) {
            return "not_installed";
        }

        return "unhealthy";
    }

    /**
     * 生成人性化提示消息
     */
    private String generateMessage(String status, boolean installLock, String dbStatus, boolean hasData) {
        switch (status) {
            case "healthy":
                return "系统运行正常";
            case "degraded":
                if ("disconnected".equals(dbStatus)) {
                    return "系统已安装但数据库连接失败，部分功能不可用";
                }
                return "系统运行降级，部分功能可能不可用";
            case "needs_reinstall":
                return "检测到已有数据但需要验证，请完成安装向导";
            case "not_installed":
                return "系统尚未安装，请先完成安装向导";
            case "unhealthy":
            default:
                return "系统状态异常，请检查配置";
        }
    }

    /**
     * 生成建议列表
     */
    private List<String> generateSuggestions(String status, boolean installLock, DatabaseCheckResult dbResult, boolean hasData) {
        List<String> suggestions = new ArrayList<>();

        switch (status) {
            case "healthy":
                suggestions.add("系统运行正常，无需操作");
                break;
            case "degraded":
                if (!dbResult.isConnected()) {
                    suggestions.add("检查数据库服务是否启动");
                    suggestions.add("检查数据库配置是否正确（用户名、密码、数据库名）");
                    suggestions.add("检查数据库用户是否有足够权限");
                    suggestions.add("检查网络连接（如果是远程数据库）");
                }
                if (installLock && !hasData) {
                    suggestions.add("数据库已连接但没有数据，可能需要重新运行安装向导");
                }
                break;
            case "needs_reinstall":
                suggestions.add("访问 /install 页面完成安装验证");
                suggestions.add("或删除 install.lock 文件后重新安装");
                break;
            case "not_installed":
                suggestions.add("访问 /install 页面完成系统安装");
                break;
            case "unhealthy":
            default:
                suggestions.add("检查后端服务日志获取详细信息");
                suggestions.add("检查配置文件是否正确");
                break;
        }

        return suggestions;
    }

    /**
     * 数据库检查结果内部类
     */
    private static class DatabaseCheckResult {
        private final boolean connected;
        private final boolean tablesExist;
        private final String status;
        private final String message;
        private final String error;

        private DatabaseCheckResult(boolean connected, boolean tablesExist, String status,
                                    String message, String error) {
            this.connected = connected;
            this.tablesExist = tablesExist;
            this.status = status;
            this.message = message;
            this.error = error;
        }

        static DatabaseCheckResult success(String message) {
            return new DatabaseCheckResult(true, true, "connected", message, null);
        }

        static DatabaseCheckResult successButNoTables(String message) {
            return new DatabaseCheckResult(true, false, "connected_no_tables", message, null);
        }

        static DatabaseCheckResult failed(String message, String error) {
            return new DatabaseCheckResult(false, false, "disconnected", message, error);
        }

        boolean isConnected() {
            return connected;
        }

        String getStatus() {
            return status;
        }

        String getMessage() {
            return message;
        }

        String getError() {
            return error;
        }
    }
}
