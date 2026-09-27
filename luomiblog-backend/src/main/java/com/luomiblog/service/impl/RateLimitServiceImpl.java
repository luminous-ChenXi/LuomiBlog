package com.luomiblog.service.impl;

import com.luomiblog.common.exception.BusinessException;
import com.luomiblog.common.exception.ErrorCode;
import com.luomiblog.config.RateLimitProperties;
import com.luomiblog.service.MemoryCacheService;
import com.luomiblog.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通用接口限流实现：复用 MemoryCacheService 计数，
 * 键形如 rate:{bucket}:{identity}，窗口首次命中时设置 TTL（与 LoginSecurityService 同模式）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final String PREFIX = "rate:";
    private static final String BUCKET_REGISTER = "register:";
    private static final String BUCKET_EMAIL_RESEND = "emailresend:";
    private static final String BUCKET_EMAIL_COOLDOWN = "emailcooldown:";
    private static final String BUCKET_COMMENT = "comment:";
    private static final String BUCKET_INTERACTION = "interaction:";

    private final MemoryCacheService memoryCacheService;
    private final RateLimitProperties properties;

    @Override
    public void checkRegister(String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }
        RateLimitProperties.Rule rule = properties.getRegister();
        acquireOrThrow(BUCKET_REGISTER + clientIp, rule.getMaxRequests(), rule.getWindowSeconds(),
                "注册过于频繁，请一小时后再试");
    }

    @Override
    public void checkEmailResend(String email, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }
        RateLimitProperties.EmailResend rule = properties.getEmailResend();

        String cooldownKey = BUCKET_EMAIL_COOLDOWN + email;
        if (memoryCacheService.exists(cooldownKey)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "验证邮件已发送，请 " + memoryCacheService.getTtl(cooldownKey) + " 秒后再试");
        }

        RateLimitProperties.Rule ipRule = new RateLimitProperties.Rule(
                rule.getMaxRequests(), rule.getWindowSeconds());
        acquireOrThrow(BUCKET_EMAIL_RESEND + clientIp, ipRule.getMaxRequests(), ipRule.getWindowSeconds(),
                "验证邮件发送过于频繁，请稍后再试");

        // 频次通过后才落冷却键（发送失败可在 AuthService 中清除重试）
        memoryCacheService.set(cooldownKey, true, rule.getCooldownSeconds());
    }

    @Override
    public void checkComment(String identity) {
        if (!properties.isEnabled()) {
            return;
        }
        RateLimitProperties.Rule rule = properties.getComment();
        acquireOrThrow(BUCKET_COMMENT + identity, rule.getMaxRequests(), rule.getWindowSeconds(),
                "评论发表过于频繁，请稍后再试");
    }

    @Override
    public void checkInteraction(String identity) {
        if (!properties.isEnabled()) {
            return;
        }
        RateLimitProperties.Rule rule = properties.getInteraction();
        acquireOrThrow(BUCKET_INTERACTION + identity, rule.getMaxRequests(), rule.getWindowSeconds(),
                "操作过于频繁，请稍后再试");
    }

    /**
     * 窗口计数 +1，超限抛 429。
     */
    private void acquireOrThrow(String key, int maxRequests, int windowSeconds, String message) {
        String fullKey = PREFIX + key;
        long current = memoryCacheService.increment(fullKey);
        if (current == 1) {
            memoryCacheService.expire(fullKey, windowSeconds);
        }
        if (current > maxRequests) {
            log.info("限流触发: key={}, current={}/{}", fullKey, current, maxRequests);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
    }
}
