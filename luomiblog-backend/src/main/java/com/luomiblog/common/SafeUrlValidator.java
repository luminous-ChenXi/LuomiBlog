package com.luomiblog.common;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * 媒体/头像 URL 协议白名单校验：
 * 仅放行 http(s) 绝对地址与站内相对路径（以 / 开头），
 * 拒绝 javascript: / data: / vbscript: 等危险协议与无法解析的字符串。
 */
public final class SafeUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private SafeUrlValidator() {
    }

    /**
     * @param url 待校验 URL（null/空白视为合法，交由调用方按空值处理）
     * @return true 表示允许存储并渲染
     */
    public static boolean isAllowedMediaUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String trimmed = url.trim();
        // 站内相对路径（上传文件等）
        if (trimmed.startsWith("/")) {
            return true;
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            return scheme != null && ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return false;
        }
    }
}
