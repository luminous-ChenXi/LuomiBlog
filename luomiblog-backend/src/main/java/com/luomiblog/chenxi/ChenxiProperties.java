package com.luomiblog.chenxi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 辰汐通行证（Chenxi Passport）登录配置
 *
 * 对接方式为标准 OAuth 2.1 / OIDC 授权码 + PKCE，本站是公共客户端（public client），
 * 整个功能没有任何客户端密钥，issuer/clientId/redirectUri 均为可公开的"门牌"信息。
 *
 * 配置前缀 chenxi.passport（与 AstrNest 孪生项目统一规范对齐），可通过环境变量覆盖（见 application.yml）。
 * 默认 enabled=false，即不配置任何内容时该功能完全关闭，本地账号登录行为与未集成时完全一致。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chenxi.passport")
public class ChenxiProperties {

    /** 是否启用辰汐通行证登录（默认关闭） */
    private boolean enabled = false;

    /** 主站通行证 issuer（OIDC 签发方地址），留空表示未配置 */
    private String issuer = "";

    /** 在通行证注册的客户端 ID（公共客户端，只有 ID 没有密钥） */
    private String clientId = "";

    /** 授权回调地址，必须与通行证侧注册的 redirect_uri 完全一致 */
    private String redirectUri = "";

    /** 授权范围，逗号分隔（如 openid,profile），兼容空格分隔 */
    private String scopes = "openid,profile";

    /** 本站会话令牌不活动过期天数（滑动刷新：活跃使用自动续期） */
    private int accessTokenDays = 30;

    /**
     * 规范化后的授权 scope（OAuth 协议要求空格分隔）：
     * 逗号/空白统一按分隔符切分后以单个空格连接
     */
    public String getScopesForAuthorize() {
        if (!StringUtils.hasText(scopes)) {
            return "";
        }
        return Arrays.stream(scopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
    }
}
