package com.luomiblog.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.luomiblog.service.MemoryCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class MemoryCacheServiceImpl implements MemoryCacheService {

    /** 计数器默认 TTL（秒）：increment 首次创建键时使用，通常随后被 expire() 按窗口覆盖 */
    private static final long DEFAULT_COUNTER_TTL_SECONDS = 1800L;

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
        // 通过 cache.asMap().compute 让"检查过期 + 创建 + 自增"成为对同一 key 的单步原子操作，
        // 消除原 getIfPresent → 判断 → set/自增 读改写竞态（并发首刷可能同时拿到 1，窗口计数偏小）
        long[] result = new long[1];
        cache.asMap().compute(key, (k, entry) -> {
            long now = System.currentTimeMillis();
            if (entry == null || now > entry.expireAt) {
                // 不存在或已过期：新建计数器（默认 TTL，通常随后被 expire() 按窗口覆盖）
                result[0] = 1L;
                return new CacheEntry(new AtomicLong(1L), now + DEFAULT_COUNTER_TTL_SECONDS * 1000);
            }
            if (entry.value instanceof AtomicLong counter) {
                // 同一窗口内原子自增，保留原过期时间
                result[0] = counter.incrementAndGet();
            } else {
                // 兼容先经 set() 写入的普通数值：升级为 AtomicLong 后续自增
                long newValue = ((Number) entry.value).longValue() + 1;
                entry.value = new AtomicLong(newValue);
                result[0] = newValue;
            }
            return entry;
        });
        return result[0];
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

    /**
     * 缓存条目：value 为普通值或计数器（increment 写入的为 AtomicLong，
     * 读取计数请走 getCounter()，其兼容任意 Number）
     */
    private static class CacheEntry {
        Object value;
        long expireAt;

        CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
