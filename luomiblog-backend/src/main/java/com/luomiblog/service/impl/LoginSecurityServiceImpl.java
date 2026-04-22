package com.luomiblog.service.impl;

import com.luomiblog.service.LoginSecurityService;
import com.luomiblog.service.MemoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private final MemoryCacheService cacheService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 1800;
    private static final long ATTEMPT_WINDOW_SECONDS = 900;

    private static final int RATE_LIMIT_CAPACITY = 10;
    private static final long RATE_LIMIT_REFILL_MS = 60000;
    private final Map<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();

    private static final int MAX_GLOBAL_ATTEMPTS_PER_MINUTE = 30;
    private static final String GLOBAL_RATE_KEY = "login:global_rate";

    private static class TokenBucket {
        private int tokens;
        private long lastRefillTime;

        TokenBucket() {
            this.tokens = RATE_LIMIT_CAPACITY;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        synchronized long getAvailableTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= RATE_LIMIT_REFILL_MS) {
                int tokensToAdd = (int) (elapsed / RATE_LIMIT_REFILL_MS);
                tokens = Math.min(RATE_LIMIT_CAPACITY, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }

    @Override
    public void recordFailedAttempt(String identifier) {
        String key = "login:failed:" + identifier;
        Integer attempts = cacheService.getAsInteger(key);

        if (attempts == null) {
            attempts = 0;
        }
        attempts++;

        long lockoutSeconds = calculateLockoutDuration(attempts);
        cacheService.set(key, attempts, ATTEMPT_WINDOW_SECONDS);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            String lockKey = "login:lockout:" + identifier;
            cacheService.set(lockKey, true, lockoutSeconds);
            log.warn("账户锁定: {} 连续失败 {} 次，锁定 {} 秒", identifier, attempts, lockoutSeconds);
        }

        String globalKey = "login:global_failed:" + identifier;
        cacheService.increment(globalKey);
    }

    @Override
    public boolean isLocked(String identifier) {
        String lockKey = "login:lockout:" + identifier;
        return cacheService.exists(lockKey);
    }

    @Override
    public void clearFailedAttempts(String identifier) {
        String key = "login:failed:" + identifier;
        cacheService.delete(key);
    }

    @Override
    public long getRemainingLockoutTime(String identifier) {
        String lockKey = "login:lockout:" + identifier;
        return cacheService.getTTL(lockKey);
    }

    @Override
    public int getRemainingAttempts(String identifier) {
        String key = "login:failed:" + identifier;
        Integer attempts = cacheService.getAsInteger(key);
        if (attempts == null) {
            return MAX_FAILED_ATTEMPTS;
        }
        return Math.max(0, MAX_FAILED_ATTEMPTS - attempts);
    }

    @Override
    public boolean tryAcquire(String clientIp) {
        if (!checkGlobalRateLimit()) {
            log.warn("全局登录频率超限，IP: {}", clientIp);
            return false;
        }

        TokenBucket bucket = rateLimiters.computeIfAbsent(clientIp, k -> new TokenBucket());
        return bucket.tryConsume();
    }

    @Override
    public long getAvailableTokens(String clientIp) {
        TokenBucket bucket = rateLimiters.get(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : RATE_LIMIT_CAPACITY;
    }

    private long calculateLockoutDuration(int failedAttempts) {
        if (failedAttempts <= MAX_FAILED_ATTEMPTS) {
            return LOCKOUT_DURATION_SECONDS;
        }
        int multiplier = (failedAttempts - MAX_FAILED_ATTEMPTS) / MAX_FAILED_ATTEMPTS + 1;
        return Math.min(LOCKOUT_DURATION_SECONDS * multiplier, 86400);
    }

    private boolean checkGlobalRateLimit() {
        Integer globalCount = cacheService.getAsInteger(GLOBAL_RATE_KEY);
        if (globalCount == null) {
            globalCount = 0;
        }
        globalCount++;
        cacheService.set(GLOBAL_RATE_KEY, globalCount, 60);
        return globalCount <= MAX_GLOBAL_ATTEMPTS_PER_MINUTE;
    }
}
