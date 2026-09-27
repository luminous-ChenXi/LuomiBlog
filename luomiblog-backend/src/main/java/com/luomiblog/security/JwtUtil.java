package com.luomiblog.security;

import com.luomiblog.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    /** 辰汐通行证会话标记声明名 */
    private static final String CLAIM_CHENXI_SESSION = "cxs";

    /** 会话总有效期（毫秒）声明名，用于滑动续期计算 */
    private static final String CLAIM_DURATION = "dur";

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(
                userPrincipal.getUsername(),
                userPrincipal.getRoleCode(),
                userPrincipal.getPermissions().stream().toList()
        );
    }

    public String generateAccessToken(String username, String roleCode, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(username)
                .claim("role", roleCode)
                .claim("perms", permissions)
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        long refreshExpiration = jwtConfig.getExpiration() * 7;
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
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
            Claims claims = parseToken(token);
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
        Claims claims = parseToken(token);
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

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        Object perms = claims.get("perms");
        if (perms instanceof List) {
            return (List<String>) perms;
        }
        return List.of();
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        return "refresh".equals(claims.get("type", String.class));
    }

    public String getTokenId(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = parseToken(token);
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
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

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
