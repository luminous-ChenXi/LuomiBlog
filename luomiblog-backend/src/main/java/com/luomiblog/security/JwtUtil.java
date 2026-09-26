package com.luomiblog.security;

import com.luomiblog.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    /**
     * JWT 密钥最小长度（字节），低于该长度容易被暴力破解
     */
    private static final int MIN_SECRET_BYTES = 32;

    /** 辰汐通行证会话标记声明名 */
    private static final String CLAIM_CHENXI_SESSION = "cxs";

    /** 会话总有效期（毫秒）声明名，用于滑动续期计算 */
    private static final String CLAIM_DURATION = "dur";

    private final JwtConfig jwtConfig;

    /**
     * 启动时解析后的最终签名密钥（可能来自配置，也可能是随机生成的临时密钥）
     */
    private String resolvedSecret;

    @PostConstruct
    void initSecret() {
        String configured = jwtConfig.getSecret();
        if (!StringUtils.hasText(configured)) {
            // 未配置 JWT_SECRET：生成随机密钥，保证应用可以启动，但 token 不能跨重启使用
            resolvedSecret = generateRandomSecret();
            log.warn("================================================================");
            log.warn("警告: 未配置 jwt.secret/JWT_SECRET，已生成随机临时密钥。");
            log.warn("重启后所有已签发的 token 将失效！生产环境请设置环境变量 JWT_SECRET（至少 {} 字节）。", MIN_SECRET_BYTES);
            log.warn("================================================================");
        } else {
            if (configured.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                        "jwt.secret/JWT_SECRET 长度不足：至少需要 " + MIN_SECRET_BYTES + " 字节（"
                                + (MIN_SECRET_BYTES * 8) + " 位），当前为 "
                                + configured.getBytes(StandardCharsets.UTF_8).length + " 字节。请更换更强的密钥。");
            }
            resolvedSecret = configured;
        }
    }

    /**
     * 生成 256 位随机密钥（十六进制编码）
     */
    private String generateRandomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername());
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 辰汐通行证会话令牌：有效期由 chenxi.passport.access-token-days 决定，
     * 携带 cxs（会话来源）与 dur（总有效期毫秒）声明，用于滑动续期判定
     */
    public String generateChenxiSessionToken(String username, long durationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + durationMs))
                .claim(CLAIM_CHENXI_SESSION, true)
                .claim(CLAIM_DURATION, durationMs)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 是否需要滑动续期：仅辰汐会话令牌（cxs=true）参与，
     * 活跃使用中且剩余寿命不足总有效期一半时续期为完整有效期
     */
    public boolean shouldSlide(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!Boolean.TRUE.equals(claims.get(CLAIM_CHENXI_SESSION, Boolean.class))) {
                return false;
            }
            Long durationMs = claims.get(CLAIM_DURATION, Long.class);
            if (durationMs == null || durationMs <= 0) {
                return false;
            }
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            return remaining > 0 && remaining < durationMs / 2;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 滑动续期：以相同主体/声明签发一个完整有效期的新令牌
     */
    public String slideToken(String token) {
        Claims claims = parseClaims(token);
        long durationMs = claims.get(CLAIM_DURATION, Long.class);
        Date now = new Date();
        return Jwts.builder()
                .subject(claims.getSubject())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + durationMs))
                .claim(CLAIM_CHENXI_SESSION, true)
                .claim(CLAIM_DURATION, durationMs)
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
