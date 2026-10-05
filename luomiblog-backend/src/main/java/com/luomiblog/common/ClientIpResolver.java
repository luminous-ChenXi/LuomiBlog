package com.luomiblog.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 客户端 IP 解析（BUG-014 修复：防 XFF 伪造绕过限流/锁定/审计）
 *
 * <p>受 app.security.trust-proxy 配置控制：</p>
 * <ul>
 *   <li><b>false（默认，直连部署）</b>：仅使用 request.getRemoteAddr()，
 *       彻底忽略 X-Forwarded-For / X-Real-IP；</li>
 *   <li><b>true（部署在反向代理之后）</b>：仅当<b>直接对端是可信代理</b>时才采信转发头——
 *       ① 优先 X-Real-IP（反代显式覆写，非追加模式，客户端无法伪造）；
 *       ② 其次 X-Forwarded-For <b>从右向左</b>取第一个非可信代理地址
 *       （nginx proxy_add_x_forwarded_for 为追加模式，最左段恰是客户端可伪造值）。</li>
 * </ul>
 *
 * <p>可信代理 = 直接对端自动可信 + app.security.trusted-proxies 显式列表（逗号分隔）
 * + 内网/环回地址内置信任（同机 nginx、Docker 网关、局域网 LB 等常见部署形态）。
 * 范式对齐 Tomcat RemoteIpValve / nginx real_ip 模块语义。</p>
 */
@Component
public class ClientIpResolver {

    private final boolean trustProxy;

    /** 额外可信代理列表（精确 IP），如多级反代中的中间层 */
    private final String trustedProxies;

    public ClientIpResolver(
            @Value("${app.security.trust-proxy:false}") boolean trustProxy,
            @Value("${app.security.trusted-proxies:}") String trustedProxies) {
        this.trustProxy = trustProxy;
        this.trustedProxies = trustedProxies;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String remoteAddr = request.getRemoteAddr();
        if (!trustProxy || !isTrustedProxy(remoteAddr)) {
            // 直连或对端不可信：转发头一律不采信（防伪造）
            return remoteAddr;
        }

        // 1. X-Real-IP 优先：由可信反代覆写，单值、不可追加伪造
        String realIp = trimmed(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        // 2. XFF 从右向左：跳过可信代理层，返回第一个非可信地址
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isEmpty() && !isTrustedProxy(candidate)) {
                    return candidate;
                }
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        if (isLoopbackOrPrivate(ip)) {
            return true;
        }
        String configured = trustedProxies;
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .anyMatch(ip::equals);
    }

    /** IPv4 环回/私有/链路本地段 + IPv6 环回（覆盖同机反代/Docker/局域网部署） */
    private boolean isLoopbackOrPrivate(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("127.") || ip.startsWith("10.")
                || ip.startsWith("192.168.") || ip.startsWith("169.254.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            // 172.16.0.0/12
            String[] seg = ip.split("\\.");
            if (seg.length == 4) {
                try {
                    int second = Integer.parseInt(seg[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private String trimmed(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
