package com.luomiblog.service;

public interface MemoryCacheService {

    void set(String key, Object value, long ttlSeconds);

    Object get(String key);

    <T> T get(String key, Class<T> type);

    boolean exists(String key);

    void delete(String key);

    long increment(String key);

    void expire(String key, long ttlSeconds);

    long getCounter(String key);

    long getTtl(String key);
}
