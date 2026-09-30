package com.luomiblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务支持（授权校验 LicenseClient 的定时 verify、登录日志保留任务 LoginLogCleanupJob）
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
