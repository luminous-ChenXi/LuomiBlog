package com.luomiblog.security;

import com.luomiblog.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 已吊销令牌清理任务（BUG-015 配套）：
 * revoked_tokens 中 expires_at 已过的行对应的令牌本身也已自然过期，
 * 保留无意义，每日清理。错开 03:30 的 login_logs 清理（单线程调度器）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedTokenCleanupJob {

    private final RevokedTokenRepository revokedTokenRepository;

    /** 每日 03:47 执行 */
    @Scheduled(cron = "0 47 3 * * ?")
    @Transactional
    public void cleanupExpiredRevokedTokens() {
        int deleted = revokedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("已吊销令牌清理完成: 删除 {} 条过期记录", deleted);
        }
    }
}
