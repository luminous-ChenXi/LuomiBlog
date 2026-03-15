package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.install.*;
import com.luomiblog.service.InstallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/install")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InstallController {

    private final InstallService installService;

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
     * 需要提供当前管理员密码进行二次验证
     */
    @PostMapping("/verify-reinstall")
    public ApiResponse<Map<String, Object>> verifyReinstallPermission(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return ApiResponse.error(400, "请提供验证密码");
        }

        boolean verified = installService.verifyReinstallPermission(password);
        if (verified) {
            // 验证通过后重置安装状态
            installService.resetInstallation();
            return ApiResponse.success(Map.of(
                "success", true,
                "message", "验证通过，可以重新安装"
            ));
        } else {
            return ApiResponse.error(403, "验证失败：密码不正确");
        }
    }
}
