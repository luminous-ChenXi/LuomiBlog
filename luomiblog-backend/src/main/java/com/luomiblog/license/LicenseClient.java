package com.luomiblog.license;

import com.luomiblog.license.dto.LicenseStatusResponse;
import com.luomiblog.license.dto.LicenseVerifyRequest;
import com.luomiblog.license.dto.LicenseVerifyResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 正版授权校验客户端（N2 占位骨架，服务端协议只定义到接口层）
 *
 * 行为约定（与 AstrNest 孪生项目统一规范对齐）：
 * - enabled=false：一切为空操作，状态恒为 disabled；
 * - enabled=true：应用启动时与每 6 小时定时调 verify-url 校验，
 *   结果本地缓存 cache-days 天；离线（校验失败）在 offline-grace-days 内放行，
 *   超宽限期仅置状态 invalid 供前端横幅提示——本类绝不锁数据、绝不拒绝服务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseClient {

    /** 定时校验间隔：6 小时 */
    private static final long VERIFY_FIXED_DELAY_MS = 6 * 3600 * 1000L;

    private final LicenseProperties properties;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    /** 最近一次校验成功时间（null = 从未成功过） */
    private volatile Instant lastVerifiedAt;

    /** 最近一次成功校验的结果 */
    private final AtomicReference<LicenseVerifyResponse> lastResult = new AtomicReference<>();

    /** 应用启动时间，作为"从未成功过"时的宽限锚点 */
    private volatile Instant startedAt;

    @PostConstruct
    void init() {
        this.startedAt = Instant.now();
        this.restClient = restClientBuilder.build();
    }

    /**
     * 启动后延迟 30 秒做首次校验（避免拖慢启动），之后每 6 小时一次。
     * enabled=false 或 verify-url 未配置时为空操作。
     */
    @Scheduled(initialDelay = 30_000L, fixedDelay = VERIFY_FIXED_DELAY_MS)
    public void scheduledVerify() {
        if (!properties.isEnabled()) {
            return;
        }
        verifyNow();
    }

    /**
     * 立即发起一次校验（供测试与手动触发）。
     * 任何网络异常都只影响本地缓存状态，绝不向上抛出、绝不影响业务。
     */
    public synchronized void verifyNow() {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getVerifyUrl())) {
            return;
        }
        try {
            LicenseVerifyResponse result = restClient.post()
                    .uri(properties.getVerifyUrl())
                    .body(buildVerifyRequest())
                    .retrieve()
                    .body(LicenseVerifyResponse.class);
            if (result != null) {
                lastResult.set(result);
                lastVerifiedAt = Instant.now();
                log.info("正版授权校验成功: valid={} licensedTo={}", result.isValid(), result.getLicensedTo());
            }
        } catch (RestClientException e) {
            // 离线：保留旧缓存，宽限期内放行；仅记录日志，不打扰业务
            log.warn("正版授权校验暂不可达（将在宽限期内放行）: {}", e.getMessage());
        }
    }

    /**
     * 当前授权状态（供前端横幅）。
     * 注意：无论返回什么状态，本站功能与数据都不受任何限制。
     */
    public LicenseStatusResponse getStatus() {
        if (!properties.isEnabled()) {
            return LicenseStatusResponse.builder()
                    .state(LicenseStatusResponse.STATE_DISABLED)
                    .enabled(false)
                    .build();
        }

        LicenseVerifyResponse cached = lastResult.get();
        boolean cacheFresh = cached != null && lastVerifiedAt != null
                && lastVerifiedAt.plusSeconds((long) properties.getCacheDays() * 86400L)
                        .isAfter(Instant.now());

        if (cached != null && cacheFresh && cached.isValid()) {
            return LicenseStatusResponse.builder()
                    .state(LicenseStatusResponse.STATE_VALID)
                    .enabled(true)
                    .build();
        }

        // 宽限判定：以"最近一次成功校验"为锚；从未成功过则以启动时间为锚
        Instant anchor = lastVerifiedAt != null ? lastVerifiedAt : startedAt;
        boolean withinGrace = anchor.plusSeconds((long) properties.getOfflineGraceDays() * 86400L)
                .isAfter(Instant.now());

        if (withinGrace) {
            return LicenseStatusResponse.builder()
                    .state(LicenseStatusResponse.STATE_GRACE)
                    .enabled(true)
                    .message("正版授权校验暂时不可用，正在离线宽限期内，功能不受影响")
                    .build();
        }

        // 超出宽限期：仅提示，绝不锁数据
        return LicenseStatusResponse.builder()
                .state(LicenseStatusResponse.STATE_INVALID)
                .enabled(true)
                .message("正版授权校验已超离线宽限期，请联系站点管理员完成校验（数据不受影响）")
                .build();
    }

    /**
     * 构建校验请求：实例指纹 = SHA-256(主机名:端口) 前 32 个十六进制字符
     */
    private LicenseVerifyRequest buildVerifyRequest() {
        String hostname = "unknown-host";
        String port = String.valueOf(8080);
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            hostname = localHost.getHostName();
        } catch (Exception e) {
            log.debug("获取主机名失败，使用占位符", e);
        }
        return LicenseVerifyRequest.builder()
                .instanceId(sha256Short(hostname + ":" + port))
                .siteName("LuomiBlog")
                .version(null) // 版本号由构建流程注入，占位阶段先置空
                .build();
    }

    private String sha256Short(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            // JVM 必带 SHA-256，理论上不可达
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
