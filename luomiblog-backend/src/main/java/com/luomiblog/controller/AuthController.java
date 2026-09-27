package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.AuthResponse;
import com.luomiblog.dto.LoginRequest;
import com.luomiblog.dto.RefreshTokenRequest;
import com.luomiblog.dto.RegisterRequest;
import com.luomiblog.service.AuthService;
import com.luomiblog.service.LoginSecurityService;
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

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        authService.logout(accessToken);
        return ApiResponse.success();
    }

    @GetMapping("/login-security")
    public ApiResponse<Map<String, Object>> getLoginSecurityInfo(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        long availableTokens = loginSecurityService.getAvailableTokens(clientIp);
        return ApiResponse.success(Map.of(
                "availableAttempts", availableTokens,
                "maxAttempts", 10
        ));
    }

    private String getClientIpAddress(HttpServletRequest request) {
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
}
