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
    private final Map<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();
    
    private static class TokenBucket {
        private int tokens;
        private long lastRefillTime;
        private static final long REFILL_INTERVAL_MS = 60000;
        
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
            if (elapsed >= REFILL_INTERVAL_MS) {
                int tokensToAdd = (int) (elapsed / REFILL_INTERVAL_MS);
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
        
        cacheService.set(key, attempts, ATTEMPT_WINDOW_SECONDS);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            String lockKey = "login:lockout:" + identifier;
            cacheService.set(lockKey, true, LOCKOUT_DURATION_SECONDS);
            
            log.warn("账户锁定: {} 连续失败 {} 次，锁定 30 分钟", identifier, attempts);
        }
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
        TokenBucket bucket = rateLimiters.computeIfAbsent(clientIp, k -> new TokenBucket());
        return bucket.tryConsume();
    }
    
    @Override
    public long getAvailableTokens(String clientIp) {
        TokenBucket bucket = rateLimiters.get(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : RATE_LIMIT_CAPACITY;
    }
}
