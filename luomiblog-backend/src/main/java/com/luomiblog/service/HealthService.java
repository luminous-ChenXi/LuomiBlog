package com.luomiblog.service;

import com.luomiblog.dto.health.HealthCheckResponse;

/**
 * 健康检查服务接口
 */
public interface HealthService {

    /**
     * 执行健康检查
     *
     * @return 健康检查结果
     */
    HealthCheckResponse checkHealth();
}
