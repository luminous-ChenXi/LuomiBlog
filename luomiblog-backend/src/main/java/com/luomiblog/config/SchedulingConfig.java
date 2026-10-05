package com.luomiblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 定时任务与异步任务支持：
 * - @EnableScheduling：授权校验 LicenseClient 定时 verify、登录日志/已吊销令牌清理
 * - @EnableAsync：[BUG-022 修复] 注册验证邮件移出注册事务异步发送，
 *   SMTP 阻塞不再占用 Hikari 连接（池仅 10）拖垮注册链路
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {

    /** 邮件专用线程池：有界、有命名，拒绝时走调用方线程兜底（不丢邮件尝试） */
    @Bean("mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cx-mail-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
