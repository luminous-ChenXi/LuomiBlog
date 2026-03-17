package com.luomiblog.service;

public interface MemoryCacheService {
    
    void set(String key, Object value, long ttlSeconds);
    
    <T> T get(String key, Class<T> type);
    
    void delete(String key);
    
    boolean exists(String key);
    
    long getTTL(String key);
    
    void increment(String key);
    
    Integer getAsInteger(String key);
}
