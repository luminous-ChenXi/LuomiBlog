package com.luomiblog.security;

import com.luomiblog.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录日志保留任务：login_logs 为安全审计表（密码登录 / 辰汐通行证 SSO / TOTP 验证），
 * 只增不删会无限增长，故每日 03:30 清理超过保留期的记录。
 * 保留天数由 app.security.login-log-retention-days 控制（默认 180 天）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogCleanupJob {

    private final LoginLogRepository loginLogRepository;

    /** 登录日志保留天数（超过即清理） */
    @Value("${app.security.login-log-retention-days:180}")
    private int retentionDays;

    /** 每日 03:30 执行一次清理 */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupExpiredLoginLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long deleted = loginLogRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("登录日志清理完成: 删除 {} 条 {} 之前的记录（保留 {} 天）", deleted, cutoff, retentionDays);
        }
    }
}
