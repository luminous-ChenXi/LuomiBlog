package com.luomiblog.service.impl;

import com.luomiblog.entity.AuthLockState;
import com.luomiblog.entity.AuthLockState.LockScope;
import com.luomiblog.repository.AuthLockStateRepository;
import com.luomiblog.service.LoginSecurityService;
import com.luomiblog.service.MemoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 登录防爆破实现（审计 P1-4 修复）：锁定/失败计数持久化到 auth_lock_states 表，重启不清零。
 *
 * <p>三维度互补（对照 AstrNest 思想自行实现，参数刻意不同）：</p>
 * <ul>
 *   <li>ACCOUNT_IP：10 次失败/30min 窗口 → 锁 30min（沿用原参数，调用方体验不变）；</li>
 *   <li>ACCOUNT：跨 IP 账号维度 20 次/60min → 锁 30min（换 IP 不再绕过账号锁，
 *       阈值放宽于单 IP 维度以降低误伤——攻击成本转嫁为打满多个 IP 才触发）；</li>
 *   <li>IP_ONLY：纯 IP 维度 30 次/60min → 升级锁（30min 起逐次翻倍、24h 封顶）。
 *       刻意不做 AstrNest 式 365 天永久拉黑：博客访客常共享出口 NAT，永久封 IP 误伤不可控。</li>
 * </ul>
 *
 * <p>IP 令牌桶（tryAcquire，10 次/分）仍走进程内 MemoryCacheService：60s 短窗口节流
 * 非安全锁定状态，重启损失一次窗口可接受（AstrNest 端点级兜底同思路），避免每请求写 DB。</p>
 *
 * <p>清理策略：过期行（锁定已过且窗口关闭超保留期）由登录失败路径惰性清理（限频），
 * 无需调度任务，防表膨胀。</p>
 *
 * <p>兼容性说明：原内存中的存量锁定/失败计数不迁移，升级部署后清零属预期。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private final MemoryCacheService memoryCacheService;
    private final AuthLockStateRepository authLockStateRepository;

    // ---- ACCOUNT_IP：单账号+单 IP（原口径）----
    private static final int ACCOUNT_IP_MAX_ATTEMPTS = 10;
    private static final long ACCOUNT_IP_WINDOW_SECONDS = 1800L;
    private static final long ACCOUNT_IP_LOCK_SECONDS = 1800L;

    // ---- ACCOUNT：跨 IP 账号维度 ----
    private static final int ACCOUNT_MAX_ATTEMPTS = 20;
    private static final long ACCOUNT_WINDOW_SECONDS = 3600L;
    private static final long ACCOUNT_LOCK_SECONDS = 1800L;

    // ---- IP_ONLY：纯 IP 维度（升级锁）----
    private static final int IP_ONLY_MAX_ATTEMPTS = 30;
    private static final long IP_ONLY_WINDOW_SECONDS = 3600L;
    private static final long IP_ONLY_LOCK_BASE_SECONDS = 1800L;
    private static final long IP_ONLY_LOCK_CAP_SECONDS = 86400L;

    // ---- IP 令牌桶（短窗口节流，进程内）----
    private static final int LOGIN_RATE_LIMIT = 10;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60L;
    private static final String RATE_PREFIX = "login:rate:";

    /** 惰性清理限频与保留期：锁定/窗口过期后再保留 24h（便于事后审计），至多 10 分钟清一次 */
    private static final long CLEANUP_INTERVAL_MS = 600_000L;
    private static final long RETENTION_SECONDS = 86_400L;

    private final AtomicLong lastCleanupAt = new AtomicLong(0);

    // =====================================================================
    // IP 令牌桶：保持进程内实现（见类注释），行为与旧版完全一致
    // =====================================================================

    @Override
    public boolean tryAcquire(String clientIp) {
        String key = RATE_PREFIX + clientIp;
        long current = memoryCacheService.increment(key);
        if (current == 1) {
            memoryCacheService.expire(key, RATE_LIMIT_WINDOW_SECONDS);
        }
        return current <= LOGIN_RATE_LIMIT;
    }

    @Override
    public long getAvailableTokens(String clientIp) {
        String key = RATE_PREFIX + clientIp;
        long current = memoryCacheService.getCounter(key);
        return Math.max(0, LOGIN_RATE_LIMIT - current);
    }

    // =====================================================================
    // 失败计数与锁定：DB 持久化，三维度各记一行
    // =====================================================================

    @Override
    @Transactional
    public void recordFailedAttempt(String identifier) {
        try {
            bump(LockScope.ACCOUNT_IP, identifier);
            Parsed p = parse(identifier);
            if (p.principal() != null) {
                bump(LockScope.ACCOUNT, p.principal());
            }
            if (p.ip() != null) {
                bump(LockScope.IP_ONLY, p.ip());
            }
            lazyCleanup();
        } catch (Exception e) {
            // 防爆破是纵深防御而非登录主链路：持久层异常不阻断失败处理（IP 限流/审计日志仍生效）
            log.error("防爆破状态写入失败: identifier={}", identifier, e);
        }
    }

    /** 单维度累加：窗口过期则清零重开；达到阈值则按维度策略锁定并重开计数窗口 */
    private void bump(LockScope scope, String key) {
        LocalDateTime now = LocalDateTime.now();
        AuthLockState row = authLockStateRepository.findByLockScopeAndIdentifier(scope, key).orElse(null);
        if (row == null) {
            AuthLockState fresh = AuthLockState.builder()
                    .lockScope(scope).identifier(key)
                    .failedCount(1).windowStartedAt(now).lockCount(0)
                    .build();
            try {
                authLockStateRepository.save(fresh);
                return;
            } catch (DataIntegrityViolationException e) {
                // 并发首败撞唯一键：回读改走累加分支
                row = authLockStateRepository.findByLockScopeAndIdentifier(scope, key).orElse(null);
                if (row == null) {
                    return;
                }
            }
        }

        long windowAge = Duration.between(row.getWindowStartedAt(), now).getSeconds();
        boolean windowExpired = windowAge >= windowSeconds(scope);
        int used = windowExpired ? 0 : row.getFailedCount();
        if (windowExpired) {
            row.setWindowStartedAt(now);
        }

        int attempts = used + 1;
        if (attempts >= maxAttempts(scope)) {
            int newLockCount = row.getLockCount() + 1;
            row.setLockCount(newLockCount);
            row.setLockedUntil(now.plusSeconds(lockSeconds(scope, newLockCount)));
            row.setFailedCount(0);
            row.setWindowStartedAt(now);
            log.warn("防爆破锁定生效: scope={}, key={}, 累计第 {} 次锁定", scope, maskKey(key), newLockCount);
        } else {
            row.setFailedCount(attempts);
        }
        authLockStateRepository.save(row);
    }

    @Override
    @Transactional
    public void clearFailedAttempts(String identifier) {
        try {
            Parsed p = parse(identifier);
            deleteRow(LockScope.ACCOUNT_IP, identifier);
            if (p.principal() != null) {
                deleteRow(LockScope.ACCOUNT, p.principal());
            }
            // 刻意不清 IP_ONLY：同 IP 对其他账号的爆破计数不因单个账号登录成功而清零
            // （AstrNest 同思想：成功只清账号维度，堵死「自助解锁」）
        } catch (Exception e) {
            log.error("防爆破状态清除失败: identifier={}", identifier, e);
        }
    }

    @Override
    public boolean isLocked(String identifier) {
        LocalDateTime now = LocalDateTime.now();
        Parsed p = parse(identifier);
        return lockedUntilFor(LockScope.ACCOUNT_IP, identifier, now).isPresent()
                || (p.principal() != null && lockedUntilFor(LockScope.ACCOUNT, p.principal(), now).isPresent())
                || (p.ip() != null && lockedUntilFor(LockScope.IP_ONLY, p.ip(), now).isPresent());
    }

    @Override
    public long getRemainingLockoutTime(String identifier) {
        LocalDateTime now = LocalDateTime.now();
        Parsed p = parse(identifier);
        long remaining = remainingSeconds(LockScope.ACCOUNT_IP, identifier, now);
        if (p.principal() != null) {
            remaining = Math.max(remaining, remainingSeconds(LockScope.ACCOUNT, p.principal(), now));
        }
        if (p.ip() != null) {
            remaining = Math.max(remaining, remainingSeconds(LockScope.IP_ONLY, p.ip(), now));
        }
        return remaining;
    }

    @Override
    public int getRemainingAttempts(String identifier) {
        LocalDateTime now = LocalDateTime.now();
        Parsed p = parse(identifier);
        int remaining = remainingAttempts(LockScope.ACCOUNT_IP, identifier,
                ACCOUNT_IP_MAX_ATTEMPTS, ACCOUNT_IP_WINDOW_SECONDS, now);
        if (p.principal() != null) {
            remaining = Math.min(remaining, remainingAttempts(LockScope.ACCOUNT, p.principal(),
                    ACCOUNT_MAX_ATTEMPTS, ACCOUNT_WINDOW_SECONDS, now));
        }
        return remaining;
    }

    // =====================================================================
    // 内部工具
    // =====================================================================

    /**
     * 拆解 identifier（约定格式为 AuthServiceImpl 构造的「账号或邮箱:IP」）：
     * 取最后一个冒号分隔——IPv4/域名下精确；裸 IPv6 直连时账号维度键会退化
     * （仅影响跨 IP 聚合精度，ACCOUNT_IP/IP_ONLY 维度仍精确，且只会漏计不会错锁）。
     * 无冒号段（如 getClientIp 兜底的 "unknown"）只落 ACCOUNT_IP 单维度。
     * 注意：install-verify:<ip> 也会派生出 ACCOUNT 维度，等效于给安装重装验证加了跨 IP 聚合锁，属顺带加固。
     */
    private Parsed parse(String identifier) {
        int idx = identifier.lastIndexOf(':');
        if (idx <= 0 || idx == identifier.length() - 1) {
            return new Parsed(null, null);
        }
        // 账号键统一小写：users.username/email 唯一键为 CI 排序规则，大小写变体视为同一账号
        return new Parsed(
                identifier.substring(0, idx).toLowerCase(Locale.ROOT),
                identifier.substring(idx + 1));
    }

    private record Parsed(String principal, String ip) {}

    private int maxAttempts(LockScope scope) {
        return switch (scope) {
            case ACCOUNT_IP -> ACCOUNT_IP_MAX_ATTEMPTS;
            case ACCOUNT -> ACCOUNT_MAX_ATTEMPTS;
            case IP_ONLY -> IP_ONLY_MAX_ATTEMPTS;
        };
    }

    private long windowSeconds(LockScope scope) {
        return switch (scope) {
            case ACCOUNT_IP -> ACCOUNT_IP_WINDOW_SECONDS;
            case ACCOUNT -> ACCOUNT_WINDOW_SECONDS;
            case IP_ONLY -> IP_ONLY_WINDOW_SECONDS;
        };
    }

    /** 锁定时长：ACCOUNT_IP/ACCOUNT 固定 30min；IP_ONLY 按累计锁定次数翻倍、24h 封顶 */
    private long lockSeconds(LockScope scope, int lockCount) {
        if (scope != LockScope.IP_ONLY) {
            return scope == LockScope.ACCOUNT_IP ? ACCOUNT_IP_LOCK_SECONDS : ACCOUNT_LOCK_SECONDS;
        }
        int shift = Math.min(Math.max(lockCount - 1, 0), 6);
        return Math.min(IP_ONLY_LOCK_BASE_SECONDS << shift, IP_ONLY_LOCK_CAP_SECONDS);
    }

    private Optional<LocalDateTime> lockedUntilFor(LockScope scope, String key, LocalDateTime now) {
        return authLockStateRepository.findByLockScopeAndIdentifier(scope, key)
                .map(AuthLockState::getLockedUntil)
                .filter(u -> u.isAfter(now));
    }

    private long remainingSeconds(LockScope scope, String key, LocalDateTime now) {
        return authLockStateRepository.findByLockScopeAndIdentifier(scope, key)
                .map(AuthLockState::getLockedUntil)
                .map(u -> Duration.between(now, u).getSeconds())
                .filter(s -> s > 0)
                .orElse(0L);
    }

    private int remainingAttempts(LockScope scope, String key,
                                  int maxAttempts, long windowSeconds, LocalDateTime now) {
        return authLockStateRepository.findByLockScopeAndIdentifier(scope, key)
                .map(row -> {
                    long windowAge = Duration.between(row.getWindowStartedAt(), now).getSeconds();
                    int used = windowAge >= windowSeconds ? 0 : row.getFailedCount();
                    return Math.max(0, maxAttempts - used);
                })
                .orElse(maxAttempts);
    }

    private void deleteRow(LockScope scope, String key) {
        authLockStateRepository.findByLockScopeAndIdentifier(scope, key)
                .ifPresent(authLockStateRepository::delete);
    }

    /** 过期行惰性清理（限频 10 分钟一次，失败不影响主流程） */
    private void lazyCleanup() {
        long nowMs = System.currentTimeMillis();
        long last = lastCleanupAt.get();
        if (nowMs - last < CLEANUP_INTERVAL_MS || !lastCleanupAt.compareAndSet(last, nowMs)) {
            return;
        }
        try {
            int removed = authLockStateRepository.deleteStaleRows(
                    LocalDateTime.now().minusSeconds(RETENTION_SECONDS));
            if (removed > 0) {
                log.info("防爆破过期行惰性清理: {} 行", removed);
            }
        } catch (Exception e) {
            log.warn("防爆破过期行清理失败（不影响主流程）", e);
        }
    }

    /** 日志脱敏：键含账号/IP，只留前 3 字符 + 长度 */
    private String maskKey(String key) {
        if (key.length() <= 3) {
            return "***(" + key.length() + ")";
        }
        return key.substring(0, 3) + "***(" + key.length() + ")";
    }
}
