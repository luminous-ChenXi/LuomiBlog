package com.luomiblog.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ClientIpResolver（BUG-014 XFF 伪造防护）单元测试。
 * 覆盖：直连模式忽略转发头、可信对端才采信、X-Real-IP 优先、XFF 从右向左。
 */
class ClientIpResolverTest {

    private static final String NGINX = "127.0.0.1";

    private HttpServletRequest request(String remoteAddr, String xff, String realIp) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn(remoteAddr);
        when(req.getHeader("X-Forwarded-For")).thenReturn(xff);
        when(req.getHeader("X-Real-IP")).thenReturn(realIp);
        return req;
    }

    @Test
    @DisplayName("直连模式(trust-proxy=false)：XFF/X-Real-IP 一律忽略")
    void directModeIgnoresForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver(false, "");
        assertEquals("203.0.113.9",
                resolver.resolve(request("203.0.113.9", "1.2.3.4", "5.6.7.8")));
    }

    @Test
    @DisplayName("trust-proxy=true 但对端不可信：转发头不采信（防直连伪造）")
    void untrustedPeerIgnoresForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver(true, "");
        // 对端是公网地址（攻击者直连），转发头不可信
        assertEquals("203.0.113.9",
                resolver.resolve(request("203.0.113.9", "1.2.3.4", "5.6.7.8")));
    }

    @Test
    @DisplayName("X-Real-IP 优先于 XFF")
    void realIpTakesPrecedence() {
        ClientIpResolver resolver = new ClientIpResolver(true, "");
        assertEquals("198.51.100.7",
                resolver.resolve(request(NGINX, "1.2.3.4, 198.51.100.7", "198.51.100.7")));
    }

    @Test
    @DisplayName("XFF 从右向左：跳过可信代理层，取最右非可信地址（伪造的最左段被忽略）")
    void xffRightToLeftSkipsTrustedProxies() {
        ClientIpResolver resolver = new ClientIpResolver(true, "");
        // 攻击者伪造 X-Forwarded-For: 6.6.6.6（最左），真实客户端 198.51.100.7 由 nginx 追加
        assertEquals("198.51.100.7",
                resolver.resolve(request(NGINX, "6.6.6.6, 198.51.100.7", null)));
    }

    @Test
    @DisplayName("多级代理：内网中间层被跳过")
    void multiLayerProxiesSkipped() {
        ClientIpResolver resolver = new ClientIpResolver(true, "");
        // 链路: 客户端伪造值, 真实客户端, docker 网关(内网,可信), nginx(127.0.0.1,对端)
        assertEquals("198.51.100.7",
                resolver.resolve(request("127.0.0.1", "6.6.6.6, 198.51.100.7, 172.17.0.1", null)));
    }

    @Test
    @DisplayName("显式配置的可信代理列表生效")
    void configuredTrustedProxiesHonored() {
        ClientIpResolver resolver = new ClientIpResolver(true, "100.64.0.5");
        assertEquals("198.51.100.7",
                resolver.resolve(request("100.64.0.5", "6.6.6.6, 198.51.100.7", null)));
    }

    @Test
    @DisplayName("XFF 全部为可信地址时回退对端地址")
    void allTrustedFallsBackToRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(true, "");
        assertEquals("127.0.0.1",
                resolver.resolve(request("127.0.0.1", "10.0.0.1, 192.168.1.2", null)));
    }

    @Test
    @DisplayName("null 请求与空 XFF 回退 remoteAddr")
    void nullSafe() {
        ClientIpResolver resolver = new ClientIpResolver(true, "");
        assertEquals("unknown", resolver.resolve(null));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", null, null)));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", " , ", null)));
    }
}
