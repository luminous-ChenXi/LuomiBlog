package com.luomiblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务支持（目前仅授权校验 LicenseClient 的定时 verify 使用）
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
