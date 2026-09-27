package com.luomiblog.security;

import com.luomiblog.service.MemoryCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TOTP 防重放守卫：记录每用户最近一次验证命中的计数器（时间步编号），
 * 同一计数器（即同码及其窗口内旧码）不得重复使用。
 *
 * <p>存量用户兼容：无记录时首验放行并落记录。</p>
 *
 * <p>记录存于 MemoryCacheService（单机内存缓存，条目上限 24 小时）。
 * TOTP 窗口仅 ±1 步（约 90 秒），缓存失效窗口远大于可用重放窗口；
 * 进程重启导致的极端重放窗口在当前单机部署形态下风险可接受（见交付报告遗留项）。</p>
 */
@Component
@RequiredArgsConstructor
public class TotpReplayGuard {

    private static final String KEY_PREFIX = "totp:lastused:";
    /** 记录 TTL：24 小时（与 MemoryCacheService 条目上限一致，命中即续期） */
    private static final long RECORD_TTL_SECONDS = 86_400L;

    private final MemoryCacheService memoryCacheService;

    /**
     * 校验该计数器是否可使用（必须严格大于上次命中值），可用则记录并返回 true。
     *
     * @param userId         用户 ID
     * @param matchedCounter TotpUtil.matchCounterAt 返回的计数器（须 >= 0）
     * @return true=放行（已落记录）；false=重放，拒绝
     */
    public boolean checkAndRecord(Long userId, long matchedCounter) {
        if (userId == null || matchedCounter < 0) {
            return false;
        }
        String key = KEY_PREFIX + userId;
        Long lastUsed = memoryCacheService.get(key, Long.class);
        if (lastUsed != null && matchedCounter <= lastUsed) {
            return false;
        }
        memoryCacheService.set(key, matchedCounter, RECORD_TTL_SECONDS);
        return true;
    }

    /** 读取上次命中计数器；无记录返回 -1（存量用户首次验证场景） */
    public long lastUsedCounter(Long userId) {
        if (userId == null) {
            return -1;
        }
        Long lastUsed = memoryCacheService.get(KEY_PREFIX + userId, Long.class);
        return lastUsed != null ? lastUsed : -1L;
    }
}
