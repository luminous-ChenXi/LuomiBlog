package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.health.HealthCheckResponse;
import com.luomiblog.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 提供系统健康状态检查接口，不需要认证
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    /**
     * 健康检查接口
     * 返回系统整体健康状态，包括数据库连接、安装状态等
     */
    @GetMapping
    public ApiResponse<HealthCheckResponse> healthCheck() {
        try {
            HealthCheckResponse health = healthService.checkHealth();
            return ApiResponse.success(health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            // 即使出错也返回一个基本的健康状态
            return ApiResponse.success(HealthCheckResponse.builder()
                    .status("unhealthy")
                    .message("系统检查失败: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 简单健康检查
     * 仅返回是否可用，用于快速检测
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong");
    }
}
