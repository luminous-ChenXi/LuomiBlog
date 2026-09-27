package com.luomiblog.service;

import com.luomiblog.dto.AuthResponse;
import com.luomiblog.dto.LoginRequest;
import com.luomiblog.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String accessToken);

    /**
     * 校验注册邮箱验证码 / 激活链接令牌；通过后激活账号并自动登录。
     */
    AuthResponse verifyRegistrationEmail(String token, String code);

    /**
     * 重发注册验证邮件（30 分钟有效令牌 + 6 位验证码）。
     */
    void resendRegistrationEmail(String email);

    /**
     * 完成 2FA 强制绑定：确认 6 位验证码后落库密钥，
     * 并一次性返回 10 个 8 位还原码 + 正式 JWT。
     */
    AuthResponse enrollTwoFactor(String challengeToken, String code);

    /**
     * 2FA 挑战验证：6 位 TOTP 验证码或 8 位一次性还原码，通过后换发正式 JWT。
     */
    AuthResponse verifyTwoFactor(String challengeToken, String code, String recoveryCode);
}
