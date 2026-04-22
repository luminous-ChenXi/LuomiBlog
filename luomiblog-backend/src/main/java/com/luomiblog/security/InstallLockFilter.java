package com.luomiblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luomiblog.common.ApiResponse;
import com.luomiblog.service.InstallService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class InstallLockFilter extends OncePerRequestFilter {

    private final InstallService installService;
    private final ObjectMapper objectMapper;

    private static final String INSTALL_LOCK_FILE = "install.lock";
    private String cachedLockHash = null;
    private volatile boolean lockVerified = false;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/install/")) {
            if (isInstallLocked()) {
                if (!requestURI.equals("/api/install/status")) {
                    if (!requestURI.equals("/api/install/verify-reinstall") && !requestURI.equals("/api/install/reinstall-options")) {
                        sendLockedResponse(response, "系统已安装，无法执行安装操作");
                        return;
                    }
                }
            }

            if (!verifyLockIntegrity()) {
                log.warn("安装锁文件完整性校验失败，可能已被篡改");
            }
        }

        if (requestURI.startsWith("/api/admin/") && !isInstallLocked()) {
            sendLockedResponse(response, "系统未完成安装，请先完成安装向导");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInstallLocked() {
        return installService.getInstallStatus().isLocked();
    }

    private boolean verifyLockIntegrity() {
        try {
            Path lockPath = Paths.get(INSTALL_LOCK_FILE);
            if (!Files.exists(lockPath)) {
                if (lockVerified && cachedLockHash != null) {
                    log.error("安装锁文件已被删除！系统可能遭受攻击");
                    return false;
                }
                return true;
            }

            String currentHash = computeFileHash(lockPath);

            if (cachedLockHash == null) {
                cachedLockHash = currentHash;
                lockVerified = true;
                return true;
            }

            if (!cachedLockHash.equals(currentHash)) {
                log.error("安装锁文件内容已被篡改！原始哈希: {}, 当前哈希: {}", cachedLockHash, currentHash);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("安装锁完整性校验异常: {}", e.getMessage());
            return false;
        }
    }

    private String computeFileHash(Path path) throws Exception {
        byte[] fileBytes = Files.readAllBytes(path);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void sendLockedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Map<String, Object>> apiResponse = ApiResponse.error(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
