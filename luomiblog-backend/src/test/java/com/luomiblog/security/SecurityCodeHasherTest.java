package com.luomiblog.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证码摘要存储器（审计 Round3）：
 * - 摘要确定性：同 pepper 同码摘要一致，可跨请求校验；
 * - 正确性：matches 只放行原码；
 * - 恒定时间比较入口对 null/空安全；
 * - pepper 差异改变摘要（不同进程/环境互不相通）。
 */
class SecurityCodeHasherTest {

    @Test
    @DisplayName("同 pepper 同码摘要一致且为 64 位小写 hex")
    void hashIsDeterministicHex() {
        SecurityCodeHasher hasher = new SecurityCodeHasher("unit-test-pepper");
        String h1 = hasher.hash("012345");
        String h2 = hasher.hash("012345");
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertEquals(h1, h1.toLowerCase());
        assertDoesNotThrow(() -> HexFormat.of().parseHex(h1));
    }

    @Test
    @DisplayName("matches 只放行原码，null/空摘要一律拒绝")
    void matchesOnlyRawCode() {
        SecurityCodeHasher hasher = new SecurityCodeHasher("unit-test-pepper");
        String stored = hasher.hash("654321");
        assertTrue(hasher.matches("654321", stored));
        assertFalse(hasher.matches("654322", stored), "错码必须拒绝");
        assertFalse(hasher.matches(null, stored));
        assertFalse(hasher.matches("654321", null));
        assertFalse(hasher.matches("654321", " "));
    }

    @Test
    @DisplayName("不同 pepper 摘要互不相通")
    void differentPeppersProduceDifferentHashes() {
        String a = new SecurityCodeHasher("pepper-a").hash("111111");
        String b = new SecurityCodeHasher("pepper-b").hash("111111");
        assertNotEquals(a, b);
        // 未配置 pepper（随机回落）也应能正常工作
        SecurityCodeHasher random = new SecurityCodeHasher("");
        assertTrue(random.matches("222222", random.hash("222222")));
    }
}
