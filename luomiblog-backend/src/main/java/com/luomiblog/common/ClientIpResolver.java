package com.luomiblog.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 客户端 IP 解析
 * 受 app.security.trust-proxy 配置控制：
 * - false（默认，直连部署）：仅使用 request.getRemoteAddr()，防止伪造 X-Forwarded-For 绕过限流
 * - true（部署在反向代理之后）：优先取 X-Forwarded-For 的第一个条目
 */
@Component
public class ClientIpResolver {

    @Value("${app.security.trust-proxy:false}")
    private boolean trustProxy;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        if (trustProxy) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
