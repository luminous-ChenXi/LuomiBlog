package com.luomiblog.service.impl;

import com.luomiblog.entity.RevokedToken;
import com.luomiblog.repository.RevokedTokenRepository;
import com.luomiblog.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Override
    @Transactional
    public void revoke(String tokenId, long ttlSeconds) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        // 幂等：同一 jti 重复吊销（refresh 轮换+logout 并发）不报错
        if (revokedTokenRepository.existsByTokenId(tokenId)) {
            return;
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(Math.max(1, ttlSeconds));
        try {
            revokedTokenRepository.save(RevokedToken.builder()
                    .tokenId(tokenId)
                    .expiresAt(expiresAt)
                    .build());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 并发吊销撞唯一键：目标已达成
            log.debug("并发吊销唯一键冲突（幂等忽略）: {}", tokenId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        return revokedTokenRepository.existsByTokenId(tokenId);
    }
}
