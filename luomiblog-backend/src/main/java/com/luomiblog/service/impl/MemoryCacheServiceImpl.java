package com.luomiblog.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.luomiblog.service.MemoryCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MemoryCacheServiceImpl implements MemoryCacheService {

    private final Cache<String, CacheEntry> cache;

    public MemoryCacheServiceImpl() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        long expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        cache.put(key, new CacheEntry(value, expireAt));
    }

    @Override
    public Object get(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            cache.invalidate(key);
            return null;
        }
        return entry.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public void delete(String key) {
        cache.invalidate(key);
    }

    @Override
    public long increment(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null || System.currentTimeMillis() > entry.expireAt) {
            set(key, 1L, 1800L);
            return 1L;
        }
        long newValue = ((Number) entry.value).longValue() + 1;
        entry.value = newValue;
        return newValue;
    }

    @Override
    public void expire(String key, long ttlSeconds) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null) {
            entry.expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        }
    }

    @Override
    public long getCounter(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    @Override
    public long getTtl(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return 0L;
        }
        long remainingMs = entry.expireAt - System.currentTimeMillis();
        return remainingMs > 0 ? remainingMs / 1000 : 0L;
    }

    private static class CacheEntry {
        Object value;
        long expireAt;

        CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
