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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
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

        String token = jwtUtil.generateToken(user.getUsername());

        return buildAuthResponse(token, user, memberRole.getCode());
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

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new RuntimeException("角色不存在"));

            String token = jwtUtil.generateToken(authentication);
            
            loginSecurityService.clearFailedAttempts(identifier);
            log.info("用户登录成功: {} from {}", request.getUsernameOrEmail(), clientIp);

            return buildAuthResponse(token, user, role.getCode());
        } catch (BadCredentialsException e) {
            loginSecurityService.recordFailedAttempt(identifier);
            int remainingAttempts = loginSecurityService.getRemainingAttempts(identifier);
            log.warn("登录失败: {} from {}, 剩余尝试次数: {}", request.getUsernameOrEmail(), clientIp, remainingAttempts);
            throw new RuntimeException("用户名或密码错误，还剩 " + remainingAttempts + " 次机会");
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

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        String newToken = jwtUtil.generateToken(username);

        return buildAuthResponse(newToken, user, role.getCode());
    }

    private AuthResponse buildAuthResponse(String token, User user, String roleCode) {
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
                        .build())
                .build();
    }
}
