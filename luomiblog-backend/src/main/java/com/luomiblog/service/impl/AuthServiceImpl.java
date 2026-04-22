package com.luomiblog.service.impl;

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
import com.luomiblog.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        if (!isPasswordStrong(request.getPassword())) {
            throw new RuntimeException("密码强度不足，需包含大小写字母和数字，至少8位");
        }

        Role memberRole = roleRepository.findByCode("member")
                .orElseThrow(() -> new RuntimeException("默认角色不存在"));

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
        String token = jwtUtil.generateToken(user.getUsername(), memberRole.getCode(), permissions.stream().toList());

        return buildAuthResponse(token, user, memberRole.getCode(), permissions);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String clientIp = getClientIp();
        String identifier = request.getUsernameOrEmail() + ":" + clientIp;

        if (!loginSecurityService.tryAcquire(clientIp)) {
            long availableTokens = loginSecurityService.getAvailableTokens(clientIp);
            throw new RuntimeException("登录过于频繁，请稍后重试。剩余可用次数：" + availableTokens);
        }

        if (loginSecurityService.isLocked(identifier)) {
            long remainingTime = loginSecurityService.getRemainingLockoutTime(identifier);
            throw new RuntimeException("账户已锁定，请 " + (remainingTime / 60) + " 分钟后重试");
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
                            .orElseThrow(() -> new RuntimeException("用户不存在")));

            if ("banned".equals(user.getStatus())) {
                throw new RuntimeException("账号已被封禁，请联系管理员");
            }

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new RuntimeException("角色不存在"));

            Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());

            String token = jwtUtil.generateToken(authentication);

            loginSecurityService.clearFailedAttempts(identifier);

            updateUserLoginInfo(user, clientIp);

            log.info("用户登录成功: {} from {}, 角色: {}", request.getUsernameOrEmail(), clientIp, role.getCode());

            return buildAuthResponse(token, user, role.getCode(), permissions);
        } catch (BadCredentialsException e) {
            loginSecurityService.recordFailedAttempt(identifier);
            int remainingAttempts = loginSecurityService.getRemainingAttempts(identifier);
            log.warn("登录失败: {} from {}, 剩余尝试次数: {}", request.getUsernameOrEmail(), clientIp, remainingAttempts);
            if (remainingAttempts <= 2) {
                throw new RuntimeException("用户名或密码错误，还剩 " + remainingAttempts + " 次机会，之后将锁定账户");
            }
            throw new RuntimeException("用户名或密码错误，还剩 " + remainingAttempts + " 次机会");
        } catch (LockedException e) {
            throw new RuntimeException("账号已被锁定，请稍后重试");
        } catch (DisabledException e) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        } catch (RuntimeException e) {
            if (!e.getMessage().contains("用户名或密码错误") &&
                !e.getMessage().contains("账号已被") &&
                !e.getMessage().contains("锁定")) {
                loginSecurityService.recordFailedAttempt(identifier);
            }
            throw e;
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

    @Override
    public AuthResponse refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的token");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        User user = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if ("banned".equals(user.getStatus())) {
            throw new RuntimeException("账号已被封禁");
        }

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());
        String newToken = jwtUtil.generateToken(username, role.getCode(), permissions.stream().toList());

        return buildAuthResponse(newToken, user, role.getCode(), permissions);
    }

    private AuthResponse buildAuthResponse(String token, User user, String roleCode, Set<String> permissions) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(roleCode)
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
}
