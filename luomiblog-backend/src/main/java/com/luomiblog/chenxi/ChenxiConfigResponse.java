package com.luomiblog.chenxi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 辰汐通行证登录配置（公开"门牌"信息）
 *
 * 公共客户端没有 client_secret，这里暴露的全部字段都可以公开给前端，
 * 前端据此拼接授权页跳转地址。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChenxiConfigResponse {

    /** 是否启用辰汐通行证登录 */
    private boolean enabled;

    /** 通行证签发方地址（OIDC issuer） */
    private String issuer;

    /** 客户端 ID */
    private String clientId;

    /** 授权回调地址 */
    private String redirectUri;

    /** 授权范围 */
    private String scopes;
}
