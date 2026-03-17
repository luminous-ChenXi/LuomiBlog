package com.luomiblog.service.impl;

import com.luomiblog.service.MemoryCacheService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MemoryCacheServiceImpl implements MemoryCacheService {
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    private static class CacheEntry {
        final Object value;
        final long expireTime;
        
        CacheEntry(Object value, long ttlSeconds) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
        
        long remainingTTL() {
            long remaining = expireTime - System.currentTimeMillis();
            return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : 0;
        }
    }
    
    @Override
    public void set(String key, Object value, long ttlSeconds) {
        cache.put(key, new CacheEntry(value, ttlSeconds));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return (T) entry.value;
    }
    
    @Override
    public void delete(String key) {
        cache.remove(key);
    }
    
    @Override
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }
    
    @Override
    public long getTTL(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return -2;
        }
        return entry.remainingTTL();
    }
    
    @Override
    public void increment(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.put(key, new CacheEntry(1, 900));
        } else {
            Integer count = (Integer) entry.value;
            cache.put(key, new CacheEntry(count + 1, entry.remainingTTL()));
        }
    }
    
    @Override
    public Integer getAsInteger(String key) {
        return get(key, Integer.class);
    }
    
    @Scheduled(fixedRate = 60000)
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
