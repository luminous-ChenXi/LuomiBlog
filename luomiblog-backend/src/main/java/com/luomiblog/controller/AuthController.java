package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.common.ClientIpResolver;
import com.luomiblog.dto.AuthResponse;
import com.luomiblog.dto.EmailResendRequest;
import com.luomiblog.dto.EmailVerifyRequest;
import com.luomiblog.dto.LoginRequest;
import com.luomiblog.dto.RefreshTokenRequest;
import com.luomiblog.dto.RegisterRequest;
import com.luomiblog.dto.TwoFactorEnrollRequest;
import com.luomiblog.dto.TwoFactorVerifyRequest;
import com.luomiblog.service.AuthService;
import com.luomiblog.service.LoginSecurityService;
import com.luomiblog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginSecurityService loginSecurityService;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletRequest httpRequest) {
        // 限流：5 次/小时/IP（阈值见 app.rate-limit.register）
        rateLimitService.checkRegister(clientIpResolver.resolve(httpRequest));
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 注册邮箱验证：输码或点激活链接（token+code）→ 激活账号并自动登录
     */
    @PostMapping("/email/verify")
    public ApiResponse<AuthResponse> verifyRegistrationEmail(@Valid @RequestBody EmailVerifyRequest request) {
        return ApiResponse.success(authService.verifyRegistrationEmail(request.getToken(), request.getCode()));
    }

    /**
     * 重发注册验证邮件（限流：60 秒冷却/邮箱 + 10 次/小时/IP）
     */
    @PostMapping("/email/resend")
    public ApiResponse<Void> resendRegistrationEmail(@Valid @RequestBody EmailResendRequest request,
                                                     HttpServletRequest httpRequest) {
        rateLimitService.checkEmailResend(request.getEmail(), clientIpResolver.resolve(httpRequest));
        authService.resendRegistrationEmail(request.getEmail());
        return ApiResponse.success();
    }

    /**
     * 2FA 强制绑定确认：输入认证器当前 6 位码完成绑定，
     * 返回正式 JWT + 一次性展示的 10 个还原码
     */
    @PostMapping("/2fa/enroll")
    public ApiResponse<AuthResponse> enrollTwoFactor(@Valid @RequestBody TwoFactorEnrollRequest request) {
        return ApiResponse.success(authService.enrollTwoFactor(request.getChallengeToken(), request.getCode()));
    }

    /**
     * 2FA 挑战验证：6 位验证码或 8 位一次性还原码 → 换发正式 JWT
     */
    @PostMapping("/2fa/verify")
    public ApiResponse<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return ApiResponse.success(authService.verifyTwoFactor(
                request.getChallengeToken(), request.getCode(), request.getRecoveryCode()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * 当前登录用户基本信息（前端现成调用；未认证返回 401）
     */
    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> me() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        authService.logout(accessToken);
        return ApiResponse.success();
    }

    @GetMapping("/login-security")
    public ApiResponse<Map<String, Object>> getLoginSecurityInfo(HttpServletRequest request) {
        String clientIp = clientIpResolver.resolve(request);
        long availableTokens = loginSecurityService.getAvailableTokens(clientIp);
        return ApiResponse.success(Map.of(
                "availableAttempts", availableTokens,
                "maxAttempts", 10
        ));
    }
}
