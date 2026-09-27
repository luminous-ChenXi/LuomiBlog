package com.luomiblog.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.luomiblog.common.exception.AuthenticationException;
import com.luomiblog.common.exception.BusinessException;
import com.luomiblog.common.exception.ErrorCode;
import com.luomiblog.dto.AuthResponse;
import com.luomiblog.dto.LoginRequest;
import com.luomiblog.dto.RegisterRequest;
import com.luomiblog.entity.LoginLog;
import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.LoginLogRepository;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.security.JwtUtil;
import com.luomiblog.security.TotpUtil;
import com.luomiblog.service.AuthService;
import com.luomiblog.service.LoginSecurityService;
import com.luomiblog.service.MailService;
import com.luomiblog.service.MemoryCacheService;
import com.luomiblog.service.PermissionService;
import com.luomiblog.service.SiteSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final SiteSettingsService siteSettingsService;
    private final MailService mailService;
    private final TotpUtil totpUtil;
    private final com.luomiblog.security.TotpReplayGuard totpReplayGuard;
    private final com.luomiblog.common.ClientIpResolver clientIpResolver;
    private final com.luomiblog.repository.LoginLogRepository loginLogRepository;
    private final ObjectMapper objectMapper;
    private final com.luomiblog.config.JwtConfig jwtConfig;

    @Value("${app.registration-enabled:true}")
    private boolean registrationEnabled;

    /** 前端基础地址（用于拼接邮箱激活链接），部署时可用环境变量覆盖 */
    @Value("${app.base-url:${APP_BASE_URL:http://localhost:4321}}")
    private String baseUrl;

    /**
     * 访问令牌有效期（秒）：唯一来源为 application.yml 的 jwt.expiration（毫秒），
     * 与 JwtUtil 签发口径一致，不再维护代码内常量。
     */
    private long accessTokenExpiresSeconds() {
        return jwtConfig.getExpiration() / 1000L;
    }

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 注册邮箱验证：验证令牌缓存前缀（TTL 30 分钟） */
    private static final String EMAIL_VERIFY_TOKEN_PREFIX = "emailverify:token:";
    /** 注册邮箱验证：6 位验证码缓存前缀 */
    private static final String EMAIL_VERIFY_CODE_PREFIX = "emailverify:code:";
    private static final long EMAIL_VERIFY_TTL_SECONDS = 1800L;

    /** 2FA 挑战临时令牌缓存前缀（TTL 5 分钟，一次性） */
    private static final String TWO_FACTOR_CHALLENGE_PREFIX = "2fa:challenge:";
    private static final long TWO_FACTOR_CHALLENGE_TTL_SECONDS = 300L;

    private final SecureRandom secureRandom = new SecureRandom();

    // =====================================================================
    // 注册（站点开关：registration.email_verify_required）
    // =====================================================================

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

        Role memberRole = roleRepository.findByCode(com.luomiblog.common.Roles.MEMBER)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "默认会员角色不存在"));

        boolean emailVerifyRequired = siteSettingsService.getBool(
                SiteSettingsService.KEY_EMAIL_VERIFY_REQUIRED, false);

        // 开关关闭（现状行为）：注册即激活，直接签发令牌
        if (!emailVerifyRequired) {
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

        // 开关开启：先建待激活账号（inactive 不可登录），发验证邮件后再激活
        if (!mailService.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "邮箱验证已开启但管理员尚未配置邮件服务，请联系管理员");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .roleId(memberRole.getId())
                // status=inactive：findActiveByUsername 查不到，验证通过前无法登录
                .status("inactive")
                .emailVerified(false)
                .build();
        userRepository.save(user);

        String token = sendRegistrationEmail(user);
        log.info("用户注册成功（待邮箱验证）: {}", user.getUsername());

        return AuthResponse.builder()
                .pendingEmailVerification(true)
                .challengeToken(token)
                .user(AuthResponse.UserInfo.builder()
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .role(memberRole.getCode())
                        .build())
                .build();
    }

    /**
     * 生成并缓存验证令牌 + 6 位验证码，发送验证邮件，返回令牌。
     */
    private String sendRegistrationEmail(User user) {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        memoryCacheService.set(EMAIL_VERIFY_TOKEN_PREFIX + token,
                Map.of("userId", user.getId(), "email", user.getEmail(), "code", code),
                EMAIL_VERIFY_TTL_SECONDS);
        memoryCacheService.set(EMAIL_VERIFY_CODE_PREFIX + user.getEmail(), code, EMAIL_VERIFY_TTL_SECONDS);

        String verifyUrl = baseUrl + "/register?verify-token=" + token + "&verify-code=" + code;
        String html = """
                <div style="max-width:520px;margin:0 auto;font-family:Arial,'PingFang SC','Microsoft YaHei',sans-serif;">
                  <h2 style="color:#ff6b9d;">欢迎注册 %s</h2>
                  <p>您好，<b>%s</b>！请使用以下验证码完成邮箱验证（30 分钟内有效）：</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:8px;background:#fdf2f7;padding:12px 16px;border-radius:8px;text-align:center;">%s</p>
                  <p>也可以点击下方链接直接激活账号：</p>
                  <p><a href="%s">%s</a></p>
                  <p style="color:#999;font-size:12px;">如果这不是您的操作，请忽略本邮件。</p>
                </div>
                """.formatted(siteSettingsService.getString("site.name", "LuomiBlog"),
                user.getUsername(), code, verifyUrl, verifyUrl);

        try {
            mailService.sendMail(user.getEmail(),
                    "注册邮箱验证 - " + siteSettingsService.getString("site.name", "LuomiBlog"), html);
        } catch (Exception e) {
            memoryCacheService.delete(EMAIL_VERIFY_TOKEN_PREFIX + token);
            memoryCacheService.delete(EMAIL_VERIFY_CODE_PREFIX + user.getEmail());
            log.error("注册验证邮件发送失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "验证邮件发送失败，请稍后重试或联系管理员");
        }
        return token;
    }

    // =====================================================================
    // 邮箱验证：输码或点激活链接 → 激活账号并自动登录
    // =====================================================================

    @Override
    @Transactional
    public AuthResponse verifyRegistrationEmail(String token, String code) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证令牌不能为空");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = memoryCacheService.get(EMAIL_VERIFY_TOKEN_PREFIX + token, Map.class);
        if (payload == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "验证链接无效或已过期，请重新获取验证邮件");
        }

        Long userId = ((Number) payload.get("userId")).longValue();
        String expectedCode = (String) payload.get("code");

        if (code == null || code.isBlank() || !expectedCode.equals(code.trim())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "验证码不正确");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setEmailVerified(true);
        user.setStatus("active");
        userRepository.save(user);

        memoryCacheService.delete(EMAIL_VERIFY_TOKEN_PREFIX + token);
        memoryCacheService.delete(EMAIL_VERIFY_CODE_PREFIX + user.getEmail());

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), role.getCode(), permissions.stream().toList());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        log.info("用户邮箱验证成功并激活: {}", user.getUsername());
        return buildAuthResponse(accessToken, refreshToken, user, role, permissions);
    }

    @Override
    public void resendRegistrationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册待验证账号"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该邮箱已验证，无需重复验证");
        }
        if (!mailService.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "邮件服务未配置，请联系管理员");
        }
        sendRegistrationEmail(user);
        log.info("已重发注册验证邮件: {}", email);
    }

    // =====================================================================
    // 登录（站点开关：login.totp_required → 2FA 挑战态）
    // =====================================================================

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

            // 站点开关：登录 2FA（TOTP）——账号密码通过后进入挑战态，不发 JWT
            boolean totpRequired = siteSettingsService.getBool(SiteSettingsService.KEY_TOTP_REQUIRED, false);
            if (totpRequired) {
                if (!Boolean.TRUE.equals(user.getTotpEnabled())) {
                    // 未绑定：进入强制绑定流程（密钥仅在确认绑定前临时保存于挑战缓存）
                    String secret = totpUtil.generateSecret();
                    String challengeToken = java.util.UUID.randomUUID().toString().replace("-", "");
                    memoryCacheService.set(TWO_FACTOR_CHALLENGE_PREFIX + challengeToken,
                            Map.of("userId", user.getId(), "secret", secret),
                            TWO_FACTOR_CHALLENGE_TTL_SECONDS);
                    Role role = roleRepository.findById(user.getRoleId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
                    String siteName = siteSettingsService.getString("site.name", "LuomiBlog");
                    log.info("用户 {} 首次登录进入 2FA 强制绑定流程", user.getUsername());
                    return AuthResponse.builder()
                            .twoFactorRequired(true)
                            .enrollment(true)
                            .challengeToken(challengeToken)
                            .otpauthUri(totpUtil.buildOtpauthUri(siteName, user.getUsername(), secret))
                            .secret(secret)
                            .user(AuthResponse.UserInfo.builder()
                                    .id(user.getId())
                                    .username(user.getUsername())
                                    .email(user.getEmail())
                                    .nickname(user.getNickname())
                                    .avatarUrl(user.getAvatarUrl())
                                    .role(role.getCode())
                                    .roleName(role.getName())
                                    .build())
                            .build();
                }

                // 已绑定：返回挑战态，等待 6 位验证码 / 还原码
                String challengeToken = java.util.UUID.randomUUID().toString().replace("-", "");
                memoryCacheService.set(TWO_FACTOR_CHALLENGE_PREFIX + challengeToken,
                        Map.of("userId", user.getId()),
                        TWO_FACTOR_CHALLENGE_TTL_SECONDS);
                Role role = roleRepository.findById(user.getRoleId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
                log.info("用户 {} 密码验证通过，等待 2FA 验证码", user.getUsername());
                return AuthResponse.builder()
                        .twoFactorRequired(true)
                        .enrollment(false)
                        .challengeToken(challengeToken)
                        .user(AuthResponse.UserInfo.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .nickname(user.getNickname())
                                .avatarUrl(user.getAvatarUrl())
                                .role(role.getCode())
                                .roleName(role.getName())
                                .build())
                        .build();
            }

            Role role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

            Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());

            String accessToken = jwtUtil.generateAccessToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            loginSecurityService.clearFailedAttempts(identifier);

            updateUserLoginInfo(user, clientIp);

            recordLoginLog(user.getId(), request.getUsernameOrEmail(),
                    LoginLog.LoginType.password, true);

            log.info("用户登录成功: {} from {}, 角色: {}", request.getUsernameOrEmail(), clientIp, role.getCode());

            return buildAuthResponse(accessToken, refreshToken, user, role, permissions);
        } catch (BadCredentialsException e) {
            loginSecurityService.recordFailedAttempt(identifier);
            recordLoginLog(null, request.getUsernameOrEmail(), LoginLog.LoginType.password, false);
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
            recordLoginLog(null, request.getUsernameOrEmail(), LoginLog.LoginType.password, false);
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        } catch (AuthenticationException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            loginSecurityService.recordFailedAttempt(identifier);
            recordLoginLog(null, request.getUsernameOrEmail(), LoginLog.LoginType.password, false);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    // =====================================================================
    // 2FA：强制绑定确认 / 挑战验证
    // =====================================================================

    @Override
    @Transactional
    public AuthResponse enrollTwoFactor(String challengeToken, String code) {
        TwoFactorChallenge challenge = loadChallenge(challengeToken);
        if (challenge.secret == null || challenge.secret.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该挑战不是绑定流程，请重新登录");
        }

        User user = userRepository.findById(challenge.userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!verifyTotpNotReplayed(user, challenge.secret, code,
                "验证码不正确，请输入认证器当前 6 位码")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已被使用，请等待下一个 6 位码刷新后再试");
        }

        // 绑定落库 + 生成 10 个 8 位还原码（BCrypt 存库，明文仅此一次返回）
        user.setTotpSecret(challenge.secret);
        user.setTotpEnabled(true);
        List<String> recoveryCodes = generateRecoveryCodes();
        user.setRecoveryCodes(encodeRecoveryCodes(recoveryCodes));
        userRepository.save(user);

        memoryCacheService.delete(TWO_FACTOR_CHALLENGE_PREFIX + challengeToken);

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), role.getCode(), permissions.stream().toList());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        AuthResponse response = buildAuthResponse(accessToken, refreshToken, user, role, permissions);
        response.setRecoveryCodes(recoveryCodes);
        recordLoginLog(user.getId(), user.getUsername(), LoginLog.LoginType.totp, true);
        log.info("用户 {} 完成 2FA 绑定", user.getUsername());
        return response;
    }

    @Override
    @Transactional
    public AuthResponse verifyTwoFactor(String challengeToken, String code, String recoveryCode) {
        TwoFactorChallenge challenge = loadChallenge(challengeToken);

        User user = userRepository.findById(challenge.userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!Boolean.TRUE.equals(user.getTotpEnabled()) || user.getTotpSecret() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该账号尚未绑定两步验证");
        }

        boolean verified = false;
        List<String> remainingRecoveryCodes = null;

        if (code != null && !code.isBlank()) {
            if (!verifyTotpNotReplayed(user, user.getTotpSecret(), code.trim(), "两步验证码不正确")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "两步验证码已被使用，请等待刷新后重试");
            }
        } else if (recoveryCode != null && !recoveryCode.isBlank()) {
            String normalized = recoveryCode.trim();
            List<String> hashes = decodeRecoveryCodes(user.getRecoveryCodes());
            String matchedHash = null;
            for (String hash : hashes) {
                if (passwordEncoder.matches(normalized, hash)) {
                    matchedHash = hash;
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "还原码不正确或已使用");
            }
            // 还原码一次性：使用后从列表移除
            hashes.remove(matchedHash);
            remainingRecoveryCodes = hashes;
            user.setRecoveryCodes(encodeRecoveryCodes(hashes));
            userRepository.save(user);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入 6 位验证码或 8 位还原码");
        }

        memoryCacheService.delete(TWO_FACTOR_CHALLENGE_PREFIX + challengeToken);

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), role.getCode(), permissions.stream().toList());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        updateUserLoginInfo(user, getClientIp());
        loginSecurityService.clearFailedAttempts((user.getUsername() + ":" + getClientIp()));

        if (remainingRecoveryCodes != null) {
            log.info("用户 {} 使用还原码登录，剩余还原码 {} 个", user.getUsername(), remainingRecoveryCodes.size());
        }
        // 6 位码与还原码同属 TOTP 第二因子通道，统一记为 TOTP
        recordLoginLog(user.getId(), user.getUsername(), LoginLog.LoginType.totp, true);
        log.info("用户 {} 2FA 验证通过，登录成功", user.getUsername());
        return buildAuthResponse(accessToken, refreshToken, user, role, permissions);
    }

    /**
     * TOTP 校验 + 防重放：
     * 1. 码本身不匹配 → 返回 false 并抛 invalidMessage；
     * 2. 码匹配但计数器不严格递增（同码重放）→ 返回 false（调用方抛“已被使用”）；
     * 3. 匹配且计数器递增 → 记录并返回 true（存量用户无记录首验放行，由守卫落记录）。
     *
     * @return true=校验通过且放行；false=拒绝（具体文案已通过异常给出）
     */
    private boolean verifyTotpNotReplayed(User user, String secret, String code, String invalidMessage) {
        long matched = totpUtil.matchCounterAt(secret, code, System.currentTimeMillis() / 1000L);
        if (matched < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, invalidMessage);
        }
        return totpReplayGuard.checkAndRecord(user.getId(), matched);
    }

    private TwoFactorChallenge loadChallenge(String challengeToken) {
        if (challengeToken == null || challengeToken.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "挑战令牌不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = memoryCacheService.get(TWO_FACTOR_CHALLENGE_PREFIX + challengeToken, Map.class);
        if (payload == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "挑战已过期，请重新登录");
        }
        Long userId = ((Number) payload.get("userId")).longValue();
        String secret = (String) payload.get("secret");
        return new TwoFactorChallenge(userId, secret);
    }

    private record TwoFactorChallenge(Long userId, String secret) {
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            codes.add(String.format("%08d", secureRandom.nextLong(100_000_000L)));
        }
        return codes;
    }

    private String encodeRecoveryCodes(List<String> plainCodes) {
        try {
            List<String> hashes = plainCodes.stream().map(passwordEncoder::encode).toList();
            return objectMapper.writeValueAsString(hashes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "还原码生成失败");
        }
    }

    private List<String> decodeRecoveryCodes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // =====================================================================
    // 当前登录用户信息
    // =====================================================================

    @Override
    public AuthResponse.UserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user;
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.luomiblog.security.UserPrincipal userPrincipal) {
            user = userPrincipal.getUser();
        } else if (principal instanceof String username && !username.isBlank()) {
            // JwtAuthenticationFilter 以用户名字符串为 principal
            user = userRepository.findActiveByUsername(username)
                    .or(() -> userRepository.findActiveByEmail(username))
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态无效，请重新登录"));
        } else {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态无效，请重新登录");
        }

        if (user.isBanned()) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        Set<String> permissions = permissionService.getPermissionCodesByRoleId(user.getRoleId());

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(role.getCode())
                .roleName(role.getName())
                .permissions(permissions.stream().toList())
                .build();
    }

    // =====================================================================
    // 原有辅助逻辑
    // =====================================================================

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

        blacklistToken(tokenId, accessTokenExpiresSeconds());

        String newAccessToken = jwtUtil.generateAccessToken(username, role.getCode(), permissions.stream().toList());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        log.info("令牌刷新成功: {}", username);

        return buildAuthResponse(newAccessToken, newRefreshToken, user, role, permissions);
    }

    @Override
    public void logout(String accessToken) {
        if (jwtUtil.validateToken(accessToken)) {
            String tokenId = jwtUtil.getTokenId(accessToken);
            long ttl = accessTokenExpiresSeconds();
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

    /**
     * 写入登录日志（安全审计，login_logs 表）。
     * 任何写日志失败都不影响登录主流程。
     */
    private void recordLoginLog(Long userId, String username, LoginLog.LoginType loginType, boolean success) {
        try {
            loginLogRepository.save(LoginLog.builder()
                    .userId(userId)
                    .username(username)
                    .loginType(loginType)
                    .success(success)
                    .ipAddress(getClientIp())
                    .userAgent(getCurrentUserAgent())
                    .build());
        } catch (Exception e) {
            log.warn("写入登录日志失败: {}", e.getMessage());
        }
    }

    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ua = attributes.getRequest().getHeader("User-Agent");
                if (ua != null && ua.length() > 500) {
                    return ua.substring(0, 500);
                }
                return ua;
            }
        } catch (Exception e) {
            log.debug("获取 User-Agent 失败: {}", e.getMessage());
        }
        return null;
    }

    private String getClientIp() {
        // 统一走 ClientIpResolver：受 app.security.trust-proxy 控制，
        // 直连部署下忽略可伪造的 X-Forwarded-For，防止绕过登录限流/锁定
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return clientIpResolver.resolve(attributes.getRequest());
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
                .expiresIn(accessTokenExpiresSeconds())
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
