package com.luomiblog.service.impl;

import com.luomiblog.common.exception.AuthenticationException;
import com.luomiblog.common.exception.BusinessException;
import com.luomiblog.common.exception.ErrorCode;
import com.luomiblog.dto.AuthResponse;
import com.luomiblog.dto.LoginRequest;
import com.luomiblog.dto.RegisterRequest;
import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.security.JwtUtil;
import com.luomiblog.service.AuthService;
import com.luomiblog.service.LoginSecurityService;
import com.luomiblog.service.MemoryCacheService;
import com.luomiblog.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final LoginSecurityService loginSecurityService;
    private final PermissionService permissionService;
    private final MemoryCacheService memoryCacheService;

    @Value("${app.registration-enabled:true}")
    private boolean registrationEnabled;

    private static final long ACCESS_TOKEN_EXPIRES_SECONDS = 86400L;
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!registrationEnabled) {
            throw new BusinessException(ErrorCode.REGISTRATION_DISABLED);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (!isPasswordStrong(request.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_WEAK,
                    "需包含大小写字母和数字，至少8位");
        }

        Role memberRole = roleRepository.findByCode("member")
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "默认会员角色不存在"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .roleId(memberRole.getId())
                .status("active")
                .emailVerified(false)
                .build();

        userRepository.save(user);

        Set<String> permissions = permissionService.getPermissionCodesByRoleId(memberRole.getId());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), memberRole.getCode(), permissions.stream().toList());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        log.info("用户注册成功: {}, 角色: {}", request.getUsername(), memberRole.getCode());

        return buildAuthResponse(accessToken, refreshToken, user, memberRole, permissions);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String clientIp = getClientIp();
        String identifier = request.getUsernameOrEmail() + ":" + clientIp;

        if (!loginSecurityService.tryAcquire(clientIp)) {
            long availableTokens = loginSecurityService.getAvailableTokens(clientIp);
            throw new BusinessException(ErrorCode.LOGIN_TOO_FREQUENT,
                    "剩余可用次数：" + availableTokens);
        }

        if (loginSecurityService.isLocked(identifier)) {
            long remainingTime = loginSecurityService.getRemainingLockoutTime(identifier);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    (remainingTime / 60) + " 分钟后重试");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findActiveByUsername(request.getUsernameOrEmail())
                    .orElseGet(() -> userRepository.findActiveByEmail(request.getUsernameOrEmail())
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));

            if (user.isBanned()) {
                throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
            }

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

            Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());

            String accessToken = jwtUtil.generateAccessToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            loginSecurityService.clearFailedAttempts(identifier);

            updateUserLoginInfo(user, clientIp);

            log.info("用户登录成功: {} from {}, 角色: {}", request.getUsernameOrEmail(), clientIp, role.getCode());

            return buildAuthResponse(accessToken, refreshToken, user, role, permissions);
        } catch (BadCredentialsException e) {
            loginSecurityService.recordFailedAttempt(identifier);
            int remainingAttempts = loginSecurityService.getRemainingAttempts(identifier);
            log.warn("登录失败: {} from {}, 剩余尝试次数: {}", request.getUsernameOrEmail(), clientIp, remainingAttempts);
            if (remainingAttempts <= 2) {
                throw new AuthenticationException(ErrorCode.USER_NOT_FOUND,
                        "还剩 " + remainingAttempts + " 次机会，之后将锁定账户");
            }
            throw new AuthenticationException(ErrorCode.USER_NOT_FOUND,
                    "还剩 " + remainingAttempts + " 次机会");
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        } catch (AuthenticationException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            loginSecurityService.recordFailedAttempt(identifier);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AuthenticationException(ErrorCode.TOKEN_INVALID);
        }

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new AuthenticationException(ErrorCode.TOKEN_INVALID, "不是有效的刷新令牌");
        }

        String tokenId = jwtUtil.getTokenId(refreshToken);
        if (isTokenBlacklisted(tokenId)) {
            throw new AuthenticationException(ErrorCode.TOKEN_INVALID, "令牌已被撤销");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isBanned()) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());

        blacklistToken(tokenId, 86400L);

        String newAccessToken = jwtUtil.generateAccessToken(username, role.getCode(), permissions.stream().toList());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        log.info("令牌刷新成功: {}", username);

        return buildAuthResponse(newAccessToken, newRefreshToken, user, role, permissions);
    }

    @Override
    public void logout(String accessToken) {
        if (jwtUtil.validateToken(accessToken)) {
            String tokenId = jwtUtil.getTokenId(accessToken);
            long ttl = ACCESS_TOKEN_EXPIRES_SECONDS;
            blacklistToken(tokenId, ttl);
            log.info("用户登出成功, tokenId: {}", tokenId);
        }
    }

    private void updateUserLoginInfo(User user, String clientIp) {
        try {
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(clientIp);
            userRepository.save(user);
        } catch (Exception e) {
            log.warn("更新登录信息失败: {}", e.getMessage());
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp.trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("获取客户端IP失败", e);
        }
        return "unknown";
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user, Role role, Set<String> permissions) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRES_SECONDS)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(role.getCode())
                        .roleName(role.getName())
                        .permissions(permissions.stream().toList())
                        .build())
                .build();
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }

    private void blacklistToken(String tokenId, long ttlSeconds) {
        memoryCacheService.set(TOKEN_BLACKLIST_PREFIX + tokenId, true, ttlSeconds);
    }

    private boolean isTokenBlacklisted(String tokenId) {
        return memoryCacheService.exists(TOKEN_BLACKLIST_PREFIX + tokenId);
    }
}
