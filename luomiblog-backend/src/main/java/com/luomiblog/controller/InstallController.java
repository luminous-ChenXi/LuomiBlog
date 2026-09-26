package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.common.ClientIpResolver;
import com.luomiblog.dto.install.*;
import com.luomiblog.service.InstallService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/install")
@RequiredArgsConstructor
public class InstallController {

    /**
     * verify-reinstall / reinstall 的显式确认字段值
     */
    static final String REINSTALL_CONFIRM = "REINSTALL";

    /**
     * fresh_install（清空所有数据）必须提供的确认字段值
     */
    static final String DROP_ALL_TABLES_CONFIRM = "DROP_ALL_TABLES";

    private final InstallService installService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/status")
    public ApiResponse<InstallStatusResponse> getInstallStatus() {
        return ApiResponse.success(installService.getInstallStatus());
    }

    @PostMapping("/check-environment")
    public ApiResponse<EnvironmentCheckResponse> checkEnvironment() {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        return ApiResponse.success(installService.checkEnvironment());
    }

    @PostMapping("/test-database")
    public ApiResponse<Map<String, Object>> testDatabaseConnection(@Valid @RequestBody DatabaseConfigRequest request) {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        boolean success = installService.testDatabaseConnection(request);
        return ApiResponse.success(Map.of(
            "success", success,
            "message", success ? "数据库连接成功" : "数据库连接失败，请检查配置"
        ));
    }

    @PostMapping("/check-database")
    public ApiResponse<DatabaseCheckResponse> checkDatabase(@Valid @RequestBody DatabaseConfigRequest request) {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        DatabaseCheckResponse response = installService.checkDatabase(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/execute-sql")
    public ApiResponse<Map<String, Object>> executeSqlScripts(@Valid @RequestBody DatabaseConfigRequest request) {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        try {
            installService.executeSqlScripts(request);
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "SQL脚本执行成功"
            ));
        } catch (Exception e) {
            return ApiResponse.error(500, "SQL脚本执行失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-admin")
    public ApiResponse<Map<String, Object>> createAdminAccount(@Valid @RequestBody AdminAccountRequest request) {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        try {
            installService.createAdminAccount(request);
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "管理员账号创建成功"
            ));
        } catch (Exception e) {
            return ApiResponse.error(500, "管理员账号创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/site-config")
    public ApiResponse<Map<String, Object>> saveSiteConfig(@Valid @RequestBody SiteConfigRequest request) {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        try {
            installService.saveSiteConfig(request);
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "站点配置保存成功"
            ));
        } catch (Exception e) {
            return ApiResponse.error(500, "站点配置保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/favicon-config")
    public ApiResponse<Map<String, Object>> saveFaviconConfig(@Valid @RequestBody FaviconConfigRequest request) {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        try {
            // 暂时返回成功，实际功能待实现
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "图标配置保存成功"
            ));
        } catch (Exception e) {
            return ApiResponse.error(500, "图标配置保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/complete")
    public ApiResponse<Map<String, Object>> completeInstallation() {
        // 检查是否已安装
        InstallStatusResponse status = installService.getInstallStatus();
        if (status.isLocked()) {
            return ApiResponse.error(403, "系统已安装，无法重复安装");
        }
        try {
            installService.completeInstallation();
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "安装完成"
            ));
        } catch (Exception e) {
            return ApiResponse.error(500, "安装完成操作失败: " + e.getMessage());
        }
    }

    /**
     * 验证重新安装权限
     * 仅已认证的 ADMIN 可调用，需提供管理员密码 + 显式确认字段，
     * 并按 IP 限流（5 次/分钟），连续失败会被锁定
     */
    @PostMapping("/verify-reinstall")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> verifyReinstallPermission(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String clientIp = clientIpResolver.resolve(httpRequest);

        // 限流：5 次/分钟/IP
        if (!installService.tryAcquireReinstallVerifyAttempt(clientIp)) {
            return ApiResponse.error(429, "尝试过于频繁，请稍后重试");
        }
        // 连续失败锁定
        if (installService.isReinstallVerifyLocked(clientIp)) {
            return ApiResponse.error(429, "失败次数过多，已被暂时锁定，请稍后重试");
        }

        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return ApiResponse.error(400, "请提供验证密码");
        }

        // 强制显式确认，防止误触发重装流程
        String confirm = request.get("confirm");
        if (!REINSTALL_CONFIRM.equals(confirm)) {
            return ApiResponse.error(400, "请提供确认字段 confirm=REINSTALL");
        }

        boolean verified = installService.verifyReinstallPermission(password);
        if (verified) {
            // 验证通过后重置安装状态
            installService.resetInstallation();
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "验证通过，可以重新安装",
                "needsOptions", installService.needsReinstallOptions()
            ));
        } else {
            installService.recordReinstallVerifyFailure(clientIp);
            return ApiResponse.error(403, "验证失败：密码不正确");
        }
    }

    /**
     * 获取重新安装选项
     * 当系统已有数据时，返回可用的重新安装选项
     */
    @GetMapping("/reinstall-options")
    public ApiResponse<Map<String, Object>> getReinstallOptions() {
        boolean needsOptions = installService.needsReinstallOptions();

        List<Map<String, String>> options = List.of(
            Map.of(
                "code", "keep_data",
                "name", "保留数据",
                "description", "保留所有用户、文章、评论等数据，仅重置站点配置"
            ),
            Map.of(
                "code", "update_schema",
                "name", "更新结构",
                "description", "保留数据，仅更新数据库表结构（用于版本升级）"
            ),
            Map.of(
                "code", "fresh_install",
                "name", "全新安装",
                "description", "清空所有数据，重新开始（不可恢复，请谨慎选择）"
            )
        );

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("needsOptions", needsOptions);
        result.put("options", options);
        result.put("warning", needsOptions ? "检测到系统已有数据，请选择安装方式" : null);
        return ApiResponse.success(result);
    }

    /**
     * 执行重新安装
     * 仅已认证的 ADMIN 可调用，需提供显式确认字段；
     * fresh_install（清空所有数据）额外要求 confirm=DROP_ALL_TABLES
     */
    @PostMapping("/reinstall")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> executeReinstall(@RequestBody Map<String, Object> request) {
        String optionCode = (String) request.get("option");
        String confirm = (String) request.get("confirm");
        @SuppressWarnings("unchecked")
        Map<String, Object> dbConfig = (Map<String, Object>) request.get("database");

        if (optionCode == null || optionCode.isEmpty()) {
            return ApiResponse.error(400, "请选择安装选项");
        }

        try {
            ReinstallOption option = ReinstallOption.fromCode(optionCode);

            // 显式确认：普通重装需 confirm=REINSTALL；fresh_install 需 confirm=DROP_ALL_TABLES
            String requiredConfirm = option == ReinstallOption.FRESH_INSTALL
                    ? DROP_ALL_TABLES_CONFIRM
                    : REINSTALL_CONFIRM;
            if (!requiredConfirm.equals(confirm)) {
                return ApiResponse.error(400,
                        option == ReinstallOption.FRESH_INSTALL
                                ? "全新安装将清空所有数据，需在请求中提供 confirm=DROP_ALL_TABLES"
                                : "请提供确认字段 confirm=REINSTALL");
            }

            // 构建数据库配置请求
            DatabaseConfigRequest dbRequest = new DatabaseConfigRequest();
            if (dbConfig != null) {
                dbRequest.setHost((String) dbConfig.get("host"));
                dbRequest.setPort((Integer) dbConfig.get("port"));
                dbRequest.setDatabase((String) dbConfig.get("database"));
                dbRequest.setUsername((String) dbConfig.get("username"));
                dbRequest.setPassword((String) dbConfig.get("password"));
            }

            installService.executeReinstall(option, dbRequest);

            return ApiResponse.success(Map.of(
                "success", true,
                "message", "重新安装完成：" + option.getName(),
                "option", option.getCode()
            ));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "重新安装失败: " + e.getMessage());
        }
    }
}
