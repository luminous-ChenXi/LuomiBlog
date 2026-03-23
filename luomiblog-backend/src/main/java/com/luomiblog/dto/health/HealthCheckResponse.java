package com.luomiblog.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康检查响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {

    /**
     * 整体状态: healthy, degraded, unhealthy
     */
    private String status;

    /**
     * 数据库状态: connected, disconnected, not_configured
     */
    private String database;

    /**
     * 是否已安装（install.lock存在）
     */
    private Boolean installLock;

    /**
     * 是否有数据（用户表是否有记录）
     */
    private Boolean hasData;

    /**
     * 后端服务版本
     */
    private String version;

    /**
     * 检查时间
     */
    private LocalDateTime timestamp;

    /**
     * 人性化提示信息
     */
    private String message;

    /**
     * 详细建议
     */
    private List<String> suggestions;

    /**
     * 各组件详细状态
     */
    private Components components;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Components {
        private ComponentStatus database;
        private ComponentStatus cache;
        private ComponentStatus fileSystem;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentStatus {
        private String status;
        private String message;
        private String error;
    }
}
