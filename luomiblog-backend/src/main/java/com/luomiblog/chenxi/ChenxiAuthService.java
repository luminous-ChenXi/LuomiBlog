package com.luomiblog.chenxi;

import com.luomiblog.dto.AuthResponse;

/**
 * 辰汐通行证（Chenxi Passport）登录服务
 * 标准 OAuth 2.1 / OIDC 授权码 + PKCE，本站作为公共客户端接入
 */
public interface ChenxiAuthService {

    /**
     * 获取通行证登录配置（公开"门牌"信息，无论是否启用都可访问）
     */
    ChenxiConfigResponse getConfig();

    /**
     * 用授权码 + PKCE 校验器向通行证换取用户身份，
     * 匹配或创建影子账号后签发本站 JWT。
     *
     * @param code         通行证回调带回的授权码
     * @param codeVerifier 前端保存的 PKCE 校验器（code_verifier）
     * @return 与 /api/auth/login 完全一致的响应结构
     */
    AuthResponse exchange(String code, String codeVerifier);
}
