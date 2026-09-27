package com.luomiblog.chenxi;

import com.luomiblog.common.BusinessException;
import com.luomiblog.common.UserStatus;
import com.luomiblog.dto.AuthResponse;
import com.luomiblog.entity.Role;
import com.luomiblog.entity.User;
import com.luomiblog.repository.RoleRepository;
import com.luomiblog.repository.UserRepository;
import com.luomiblog.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * 辰汐通行证登录服务实现
 *
 * 流程：授权码 + code_verifier 换 access_token（表单提交，公共客户端只带 client_id，
 * 不做 Basic 认证）→ Bearer 访问 /userinfo → 按 chenxi_sub 匹配或创建影子账号 → 签发本站 JWT。
 *
 * 注意：通行证的 access_token 对本站是不透明的，本站不缓存、不刷新，
 * 会话始终由本站自己的 JWT 承载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ChenxiAuthServiceImpl implements ChenxiAuthService {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<Map<String, Object>>() {
            };

    /** 用户名列长度上限（users.username 为 VARCHAR(64)，预留 _cx 去重后缀的空间） */
    private static final int USERNAME_MAX_LENGTH = 40;

    private final ChenxiProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    @PostConstruct
    void initRestClient() {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ChenxiConfigResponse getConfig() {
        return ChenxiConfigResponse.builder()
                .enabled(properties.isEnabled())
                .issuer(properties.getIssuer())
                .clientId(properties.getClientId())
                .redirectUri(properties.getRedirectUri())
                // OAuth 协议要求 scope 以空格分隔，这里统一规范化后再下发给前端
                .scopes(properties.getScopesForAuthorize())
                .build();
    }

    @Override
    public AuthResponse exchange(String code, String codeVerifier) {
        if (!properties.isEnabled()) {
            throw new BusinessException(404, "辰汐通行证登录未启用");
        }

        Map<String, Object> tokenResponse = requestToken(code, codeVerifier);
        String accessToken = textValue(tokenResponse, "access_token");
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(502, "辰汐通行证登录失败：通行证未返回访问令牌");
        }

        Map<String, Object> claims = requestUserInfo(accessToken);
        // sub 是跨生态唯一的用户标识（数字串），是影子账号的锚点，必须存在
        String sub = textValue(claims, "sub");
        if (!StringUtils.hasText(sub)) {
            throw new BusinessException(502, "辰汐通行证登录失败：用户信息不完整");
        }

        User user = findOrCreateShadowUser(sub, claims);

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BusinessException(500, "角色不存在"));

        // 本站会话 JWT：有效期 access-token-days，带 cxs 标记以支持滑动续期
        long durationMs = properties.getAccessTokenDays() * 86400000L;
        String token = jwtUtil.generateChenxiSessionToken(user.getUsername(), durationMs);
        log.info("辰汐通行证登录成功: sub={} username={}", sub, user.getUsername());

        return buildAuthResponse(token, user, role.getCode());
    }

    /**
     * POST {issuer}/oauth2/token
     * 表单编码：grant_type/client_id/code/redirect_uri/code_verifier。
     * 公共客户端没有密钥，client_id 放在表单里，不使用 HTTP Basic 认证。
     */
    private Map<String, Object> requestToken(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code_verifier", codeVerifier);

        try {
            Map<String, Object> body = restClient.post()
                    .uri(properties.getIssuer() + "/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JSON_MAP);
            if (body == null) {
                throw new BusinessException(502, "辰汐通行证登录失败：通行证返回为空");
            }
            if (body.containsKey("error")) {
                // 标准 OAuth 错误响应（如 invalid_grant：授权码无效/过期/PKCE 不匹配）
                log.warn("辰汐通行证 token 接口返回错误: {}", body.get("error"));
                throw new BusinessException(401, "辰汐通行证登录失败：授权码无效或已过期，请重新登录");
            }
            return body;
        } catch (RestClientResponseException e) {
            log.warn("辰汐通行证 token 接口调用失败: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                throw new BusinessException(401, "辰汐通行证登录失败：授权码无效或已过期，请重新登录");
            }
            throw new BusinessException(502, "辰汐通行证登录失败，请稍后重试");
        } catch (RestClientException e) {
            log.warn("连接辰汐通行证失败", e);
            throw new BusinessException(502, "辰汐通行证登录失败：通行证服务暂时不可用");
        }
    }

    /**
     * GET {issuer}/userinfo，携带 Bearer access_token
     */
    private Map<String, Object> requestUserInfo(String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(properties.getIssuer() + "/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JSON_MAP);
            if (body == null) {
                throw new BusinessException(502, "辰汐通行证登录失败：获取用户信息为空");
            }
            return body;
        } catch (RestClientResponseException e) {
            log.warn("辰汐通行证 userinfo 接口调用失败: {}", e.getStatusCode());
            throw new BusinessException(502, "辰汐通行证登录失败：获取用户信息失败");
        } catch (RestClientException e) {
            log.warn("连接辰汐通行证失败", e);
            throw new BusinessException(502, "辰汐通行证登录失败：通行证服务暂时不可用");
        }
    }

    /**
     * 按 chenxi_sub 查找影子账号：已有则校验状态并同步资料，没有则创建
     */
    private User findOrCreateShadowUser(String sub, Map<String, Object> claims) {
        User user = userRepository.findByChenxiSub(sub).orElse(null);
        if (user == null) {
            return createShadowUser(sub, claims);
        }
        ensureActive(user);
        syncShadowProfile(user, claims);
        return user;
    }

    private void ensureActive(User user) {
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
    }

    /**
     * 登录时顺带同步通行证侧的最新资料（claims 缺失时绝不覆盖本地值）
     */
    private void syncShadowProfile(User user, Map<String, Object> claims) {
        boolean changed = false;

        String nickname = extractNickname(claims);
        if (StringUtils.hasText(nickname) && !nickname.equals(user.getNickname())) {
            user.setNickname(nickname);
            changed = true;
        }

        String email = textValue(claims, "email");
        if (StringUtils.hasText(email) && !email.equals(user.getEmail())
                // email 列有唯一约束：本地已有其他账号占用该邮箱时不回填
                && user.getEmail() == null && !userRepository.existsByEmail(email)) {
            user.setEmail(email);
            changed = true;
        }

        String avatar = extractAvatar(claims);
        if (StringUtils.hasText(avatar) && !avatar.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatar);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }

    /**
     * 创建影子账号：
     * - 角色 member，状态 active，chenxi_sub 作为唯一锚点；
     * - 用户名取 preferred_username/username，与本站已有用户名冲突时追加 _cx 后缀；
     * - 邮箱为空或与本站账号冲突时置 NULL（email 列有唯一约束）；
     * - 密码为随机 UUID 的 BCrypt 哈希，设计上即无法用密码登录。
     */
    private User createShadowUser(String sub, Map<String, Object> claims) {
        Role memberRole = roleRepository.findByCodeIgnoreCase(com.luomiblog.common.Roles.MEMBER)
                .orElseThrow(() -> new BusinessException(500, "默认角色不存在"));

        String username = allocateUsername(resolveUsername(claims));

        String nickname = extractNickname(claims);
        if (!StringUtils.hasText(nickname)) {
            nickname = username;
        }

        String email = textValue(claims, "email");
        if (!StringUtils.hasText(email) || userRepository.existsByEmail(email)) {
            email = null;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .nickname(nickname)
                .avatarUrl(extractAvatar(claims))
                .roleId(memberRole.getId())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .chenxiSub(sub)
                .build();

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 并发首次登录撞了 chenxi_sub 唯一键：说明另一请求刚创建好，直接取回
            User existing = userRepository.findByChenxiSub(sub)
                    .orElseThrow(() -> new BusinessException(502, "辰汐通行证登录失败，请稍后重试"));
            log.info("辰汐通行证影子账号并发创建冲突，复用已有账号: sub={} username={}", sub, existing.getUsername());
            ensureActive(existing);
            return existing;
        }
    }

    /**
     * 用户名优先级：preferred_username → username → cx_{sub}（兜底一定有值）
     */
    private String resolveUsername(Map<String, Object> claims) {
        String username = textValue(claims, "preferred_username");
        if (!StringUtils.hasText(username)) {
            username = textValue(claims, "username");
        }
        if (!StringUtils.hasText(username)) {
            username = "cx_" + textValue(claims, "sub");
        }
        if (username.length() > USERNAME_MAX_LENGTH) {
            username = username.substring(0, USERNAME_MAX_LENGTH);
        }
        return username;
    }

    /**
     * 与本地已有用户名去重：冲突时追加 _cx、_cx2、_cx3……直到可用
     */
    private String allocateUsername(String base) {
        String candidate = base;
        int suffix = 0;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = suffix == 1 ? base + "_cx" : base + "_cx" + suffix;
            if (suffix >= 100) {
                throw new BusinessException(502, "辰汐通行证登录失败：用户名分配失败");
            }
        }
        return candidate;
    }

    /** 昵称优先级：nickname → preferred_username → username（claims 可能缺省） */
    private String extractNickname(Map<String, Object> claims) {
        String nickname = textValue(claims, "nickname");
        if (!StringUtils.hasText(nickname)) {
            nickname = textValue(claims, "preferred_username");
        }
        if (!StringUtils.hasText(nickname)) {
            nickname = textValue(claims, "username");
        }
        return nickname;
    }

    /**
     * 头像优先级：avatar → picture（通行证自定义字段在前）。
     * 协议白名单：claims 是外部输入，仅放行 http(s) 与站内相对路径，非法值一律置 null。
     */
    private String extractAvatar(Map<String, Object> claims) {
        String avatar = textValue(claims, "avatar");
        if (!StringUtils.hasText(avatar)) {
            avatar = textValue(claims, "picture");
        }
        if (!com.luomiblog.common.SafeUrlValidator.isAllowedMediaUrl(avatar)) {
            log.warn("通行证返回的头像 URL 未通过协议白名单校验，已忽略: {}", avatar);
            return null;
        }
        return avatar;
    }

    /** 宽容地读取 claims：值缺失/为空一律返回 null，绝不假设字段存在 */
    private String textValue(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 与 AuthServiceImpl 的登录响应保持完全一致，前端 setAuth 流程无需任何改动。
     *
     * 会话令牌有效期来自 chenxi.passport.access-token-days（默认 30 天），
     * 并打上 cxs 会话标记：JwtAuthenticationFilter 在令牌活跃使用且剩余寿命
     * 不足一半时滑动续期（响应头 X-New-Token），实现"不活动过期"语义——
     * 与通行证侧按年计的授权体系完全解耦（本站 JWT 过期≠通行证授权过期）。
     */
    private AuthResponse buildAuthResponse(String token, User user, String roleCode) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn((long) properties.getAccessTokenDays() * 86400L)
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
