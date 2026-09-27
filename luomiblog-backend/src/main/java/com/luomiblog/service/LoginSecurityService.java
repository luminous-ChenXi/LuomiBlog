package com.luomiblog.service;

public interface LoginSecurityService {

    boolean tryAcquire(String clientIp);

    long getAvailableTokens(String clientIp);

    void recordFailedAttempt(String identifier);

    void clearFailedAttempts(String identifier);

    boolean isLocked(String identifier);

    long getRemainingLockoutTime(String identifier);

    int getRemainingAttempts(String identifier);
}
