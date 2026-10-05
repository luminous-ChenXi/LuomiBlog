package com.luomiblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 已吊销令牌表（BUG-015 修复）：token 黑名单的唯一事实源。
 *
 * <p>旧实现存于 10000 条上限的共享 Caffeine——容量逐出会让已登出/已轮换的令牌"复活"，
 * 且重启即丢。DB 存储无淘汰、跨重启；expired 行由每日清理任务回收。</p>
 */
@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_revoked_tokens_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 令牌 jti（唯一） */
    @Column(name = "token_id", nullable = false, unique = true, length = 64)
    private String tokenId;

    @CreationTimestamp
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private LocalDateTime revokedAt;

    /** 吊销有效期：与令牌自身剩余寿命对齐，到期后行可安全清理（令牌本身也已过期） */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
