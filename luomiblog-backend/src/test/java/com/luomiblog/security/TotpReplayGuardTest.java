package com.luomiblog.security;

import com.luomiblog.service.impl.MemoryCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TOTP 防重放守卫：
 * - 存量用户（无记录）首验放行并落记录；
 * - 同一计数器（同码重放）拒绝；
 * - 计数器必须严格递增；
 * - 用户之间记录互不影响。
 */
class TotpReplayGuardTest {

    private TotpReplayGuard guard;

    @BeforeEach
    void setUp() {
        guard = new TotpReplayGuard(new MemoryCacheServiceImpl());
    }

    @Test
    @DisplayName("存量用户无记录：首验放行并落记录")
    void firstVerifyAllowed() {
        assertEquals(-1L, guard.lastUsedCounter(42L), "初始应无记录");
        assertTrue(guard.checkAndRecord(42L, 100L), "无记录首验应放行");
        assertEquals(100L, guard.lastUsedCounter(42L), "放行后应落记录");
    }

    @Test
    @DisplayName("同码重放（计数器相同）拒绝")
    void replaySameCounterRejected() {
        assertTrue(guard.checkAndRecord(1L, 200L));
        assertFalse(guard.checkAndRecord(1L, 200L), "同一计数器不得重复使用");
    }

    @Test
    @DisplayName("更旧计数器（窗口内旧码）拒绝")
    void olderCounterRejected() {
        assertTrue(guard.checkAndRecord(2L, 300L));
        assertFalse(guard.checkAndRecord(2L, 299L), "旧码（更小计数器）应拒绝");
        assertTrue(guard.checkAndRecord(2L, 301L), "新码（更大计数器）应放行");
    }

    @Test
    @DisplayName("用户之间互不影响；非法入参拒绝")
    void isolationAndInvalidInput() {
        assertTrue(guard.checkAndRecord(3L, 400L));
        assertTrue(guard.checkAndRecord(4L, 400L), "不同用户同一计数器互不影响");

        assertFalse(guard.checkAndRecord(null, 401L), "无用户拒绝");
        assertFalse(guard.checkAndRecord(3L, -1L), "非法计数器拒绝");
    }
}
