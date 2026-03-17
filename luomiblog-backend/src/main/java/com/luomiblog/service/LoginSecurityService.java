package com.luomiblog.service;

public interface LoginSecurityService {
    
    void recordFailedAttempt(String identifier);
    
    boolean isLocked(String identifier);
    
    void clearFailedAttempts(String identifier);
    
    long getRemainingLockoutTime(String identifier);
    
    int getRemainingAttempts(String identifier);
    
    boolean tryAcquire(String clientIp);
    
    long getAvailableTokens(String clientIp);
}
