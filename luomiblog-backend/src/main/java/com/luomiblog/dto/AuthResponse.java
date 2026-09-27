package com.luomiblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    /* ---------- 注册邮箱验证 ---------- */

    /** true：注册成功但需先完成邮箱验证（无令牌，不可登录） */
    @Builder.Default
    private Boolean pendingEmailVerification = false;

    /* ---------- 登录 2FA（TOTP）挑战态 ---------- */

    /** true：账号密码已通过，需要 2FA 挑战（不发 JWT） */
    @Builder.Default
    private Boolean twoFactorRequired = false;

    /** true：首次登录强制绑定 TOTP（enrollment）；false：已绑定，直接验证 */
    @Builder.Default
    private Boolean enrollment = false;

    /** 2FA 挑战临时令牌（5 分钟有效，仅此一次流程内使用） */
    private String challengeToken;

    /** otpauth://totp/... URI（仅 enrollment 时返回，前端渲染二维码） */
    private String otpauthUri;

    /** Base32 密钥明文（仅 enrollment 时返回一次，配合 otpauthUri 使用） */
    private String secret;

    /** 绑定成功时一次性展示的 10 个 8 位还原码（此后任何接口不再返回） */
    private List<String> recoveryCodes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatarUrl;
        private String role;
        private String roleName;
        private List<String> permissions;
    }
}
