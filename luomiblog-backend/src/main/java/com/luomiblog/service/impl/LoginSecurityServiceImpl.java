package com.luomiblog.service.impl;

import com.luomiblog.service.LoginSecurityService;
import com.luomiblog.service.MemoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private final MemoryCacheService memoryCacheService;

    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final int LOGIN_RATE_LIMIT = 10;
    private static final long LOCKOUT_DURATION_SECONDS = 1800L;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60L;
    private static final String ATTEMPT_PREFIX = "login:attempt:";
    private static final String LOCK_PREFIX = "login:lock:";
    private static final String RATE_PREFIX = "login:rate:";

    @Override
    public boolean tryAcquire(String clientIp) {
        String key = RATE_PREFIX + clientIp;
        long current = memoryCacheService.increment(key);
        if (current == 1) {
            memoryCacheService.expire(key, RATE_LIMIT_WINDOW_SECONDS);
        }
        return current <= LOGIN_RATE_LIMIT;
    }

    @Override
    public long getAvailableTokens(String clientIp) {
        String key = RATE_PREFIX + clientIp;
        long current = memoryCacheService.getCounter(key);
        return Math.max(0, LOGIN_RATE_LIMIT - current);
    }

    @Override
    public void recordFailedAttempt(String identifier) {
        String key = ATTEMPT_PREFIX + identifier;
        long attempts = memoryCacheService.increment(key);
        if (attempts == 1) {
            memoryCacheService.expire(key, LOCKOUT_DURATION_SECONDS);
        }
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            String lockKey = LOCK_PREFIX + identifier;
            memoryCacheService.set(lockKey, true, LOCKOUT_DURATION_SECONDS);
            log.warn("账户已锁定: {}, 尝试次数: {}", identifier, attempts);
        }
    }

    @Override
    public void clearFailedAttempts(String identifier) {
        memoryCacheService.delete(ATTEMPT_PREFIX + identifier);
    }

    @Override
    public boolean isLocked(String identifier) {
        return memoryCacheService.exists(LOCK_PREFIX + identifier);
    }

    @Override
    public long getRemainingLockoutTime(String identifier) {
        return memoryCacheService.getTtl(LOCK_PREFIX + identifier);
    }

    @Override
    public int getRemainingAttempts(String identifier) {
        long attempts = memoryCacheService.getCounter(ATTEMPT_PREFIX + identifier);
        return Math.max(0, MAX_LOGIN_ATTEMPTS - (int) attempts);
    }
}
