package com.luomiblog.chenxi;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.common.ClientIpResolver;
import com.luomiblog.dto.AuthResponse;
import com.luomiblog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 辰汐通行证登录接口
 * 挂在 /api/auth/** 下，SecurityConfig 已对该前缀 permitAll，无需额外放行配置
 */
@RestController
@RequestMapping("/api/auth/chenxi")
@RequiredArgsConstructor
public class ChenxiAuthController {

    private final ChenxiAuthService chenxiAuthService;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    /**
     * 通行证登录配置（公开"门牌"信息，无论是否启用都返回，前端根据 enabled 决定是否展示入口）
     */
    @GetMapping("/config")
    public ApiResponse<ChenxiConfigResponse> config() {
        return ApiResponse.success(chenxiAuthService.getConfig());
    }

    /**
     * 授权码 + PKCE 校验器换取本站会话，响应结构与 /api/auth/login 完全一致
     */
    @PostMapping("/exchange")
    public ApiResponse<AuthResponse> exchange(@Valid @RequestBody ChenxiExchangeRequest request,
                                              HttpServletRequest httpRequest) {
        // 限流：固定 10 次/分钟/IP——exchange 是匿名端点且每次触发对上游通行证的
        // 出站 token/userinfo 请求，此前完全无限流，可被打爆上游或当转发跳板
        rateLimitService.checkChenxiExchange(clientIpResolver.resolve(httpRequest));
        return ApiResponse.success(chenxiAuthService.exchange(request.getCode(), request.getCodeVerifier()));
    }
}
