package com.luomiblog.service;

/**
 * 令牌吊销（黑名单）服务（BUG-015 修复）。
 *
 * <p>唯一事实源为 revoked_tokens 表：无容量淘汰、跨重启持久，
 * 替代旧实现共享 10000 条 Caffeine（逐出=已登出令牌复活）。</p>
 */
public interface TokenBlacklistService {

    /**
     * 吊销令牌。
     *
     * @param tokenId    令牌 jti
     * @param ttlSeconds 吊销有效期（应等于令牌自身剩余寿命，到期行可清理）
     */
    void revoke(String tokenId, long ttlSeconds);

    /** 令牌是否已被吊销 */
    boolean isRevoked(String tokenId);
}
