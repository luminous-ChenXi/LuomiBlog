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
        return ApiResponse.success(installService.checkEnvironment());
    }

    @PostMapping("/test-database")
    public ApiResponse<Map<String, Object>> testDatabaseConnection(@Valid @RequestBody DatabaseConfigRequest request) {
        boolean success = installService.testDatabaseConnection(request);
        return ApiResponse.success(Map.of(
            "success", success,
            "message", success ? "数据库连接成功" : "数据库连接失败，请检查配置"
        ));
    }

    @PostMapping("/execute-sql")
    public ApiResponse<Map<String, Object>> executeSqlScripts(@Valid @RequestBody DatabaseConfigRequest request) {
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
}
