package com.luomiblog.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RFC 6238 TOTP 实现验证：
 * 使用 RFC 6238 附录 B 的标准测试向量（20 字节 ASCII 密钥 "12345678901234567890"）
 * 验证 6 位验证码的正确性（附录 B 为 8 位码，同一截断值取 6 位即为 6 位 TOTP 结果）。
 */
class TotpUtilTest {

    private TotpUtil totpUtil;

    /** RFC 6238 附录 B 测试密钥：ASCII "12345678901234567890"（20 字节 = SHA1 场景） */
    private static final String RFC_KEY_ASCII = "12345678901234567890";

    /**
     * RFC 6238 附录 B（TOTP SHA1）标准时间与 8 位码，以及对应的 6 位码：
     * T=59           → 94287082 → 287082
     * T=1111111109   → 07081804 → 081804
     * T=1111111111   → 14050471 → 050471
     * T=1234567890   → 89005924 → 005924
     * T=2000000000   → 69279037 → 279037
     * T=20000000000  → 65353130 → 353130
     */
    private static final long[] TIMES = {
            59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L
    };
    private static final String[] EXPECTED_8 = {
            "94287082", "07081804", "14050471", "89005924", "69279037", "65353130"
    };
    private static final String[] EXPECTED_6 = {
            "287082", "081804", "050471", "005924", "279037", "353130"
    };

    @BeforeEach
    void setUp() {
        totpUtil = new TotpUtil();
    }

    /** RFC 附录 B 密钥的确定性 Base32 编码（与实现内部编码同一字节串） */
    private String base32RfcKey() {
        return totpUtil.base32Encode(RFC_KEY_ASCII.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("RFC 6238 附录 B 标准向量：6 位码全部匹配")
    void rfc6238AppendixBTestVectors() {
        String base32Secret = base32RfcKey();

        for (int i = 0; i < TIMES.length; i++) {
            String code = totpUtil.generateCode(base32Secret, TIMES[i]);
            assertEquals(EXPECTED_6[i], code,
                    "T=" + TIMES[i] + " 应生成 6 位码 " + EXPECTED_6[i] + "，实际 " + code);
        }
    }

    @Test
    @DisplayName("窗口容差：当前/±1 窗口可通过，±2 窗口外拒绝")
    void verifyWindowTolerance() {
        String base32Secret = base32RfcKey();
        long now = 1000L; // counter = 33

        assertTrue(totpUtil.verifyCodeAt(base32Secret, totpUtil.generateCode(base32Secret, now), now));

        String prev = totpUtil.generateCode(base32Secret, now - 30L);
        String next = totpUtil.generateCode(base32Secret, now + 30L);
        assertTrue(totpUtil.verifyCodeAt(base32Secret, prev, now), "前 1 窗口的码应通过（±1 容差）");
        assertTrue(totpUtil.verifyCodeAt(base32Secret, next, now), "后 1 窗口的码应通过（±1 容差）");

        String prev2 = totpUtil.generateCode(base32Secret, now - 60L);
        String next2 = totpUtil.generateCode(base32Secret, now + 60L);
        assertFalse(totpUtil.verifyCodeAt(base32Secret, prev2, now), "±2 窗口外的码应拒绝");
        assertFalse(totpUtil.verifyCodeAt(base32Secret, next2, now), "±2 窗口外的码应拒绝");
    }

    @Test
    @DisplayName("非法输入与错误码拒绝")
    void rejectInvalidInputs() {
        String secret = totpUtil.generateSecret();
        assertFalse(totpUtil.verifyCode(secret, null));
        assertFalse(totpUtil.verifyCode(secret, ""));
        assertFalse(totpUtil.verifyCode(secret, "12345"));   // 5 位
        assertFalse(totpUtil.verifyCode(secret, "1234567")); // 7 位
        assertFalse(totpUtil.verifyCode(secret, "abcdef"));  // 非数字
        assertFalse(totpUtil.verifyCode(null, "123456"));
        assertFalse(totpUtil.verifyCode("", "123456"));
    }

    @Test
    @DisplayName("generateSecret：Base32 字符集、32 字符长度、随机不重复")
    void generateSecretProperties() {
        String s1 = totpUtil.generateSecret();
        String s2 = totpUtil.generateSecret();
        // 20 字节 → 160 bit / 5 = 32 个 Base32 字符
        assertEquals(32, s1.length());
        assertTrue(s1.matches("[A-Z2-7]+"), "必须是 RFC 4648 Base32 字母表: " + s1);
        assertEquals(32, s2.length());
        assertNotEquals(s1, s2);
    }

    @Test
    @DisplayName("otpauth URI：包含 secret 与 issuer=LuomiBlog")
    void otpauthUriFormat() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = totpUtil.buildOtpauthUri("LuomiBlog", "admin", secret);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("issuer=LuomiBlog"));
        assertTrue(uri.contains("%3A"), "标签应包含 URL 编码的冒号");
    }

    @Test
    @DisplayName("otpauth URI：显式携带 algorithm=SHA1&digits=6&period=30（部分验证器依赖）")
    void otpauthUriExplicitParams() {
        String uri = totpUtil.buildOtpauthUri("LuomiBlog", "admin", "JBSWY3DPEHPK3PXP");
        assertTrue(uri.contains("algorithm=SHA1"), "应显式声明 SHA1 算法: " + uri);
        assertTrue(uri.contains("digits=6"), "应显式声明 6 位码: " + uri);
        assertTrue(uri.contains("period=30"), "应显式声明 30 秒步长: " + uri);
    }

    @Test
    @DisplayName("matchCounterAt：命中返回计数器，窗口外/非法输入返回 -1")
    void matchCounterReturnsCounter() {
        String base32Secret = base32RfcKey();
        long now = 1000L; // counter = 33

        // 当前窗口命中 → 计数器 33
        assertEquals(33L, totpUtil.matchCounterAt(base32Secret, totpUtil.generateCode(base32Secret, now), now));
        // 前一窗口的码命中 → 计数器 32
        assertEquals(32L, totpUtil.matchCounterAt(
                base32Secret, totpUtil.generateCode(base32Secret, now - 30L), now));
        // 后一窗口的码命中 → 计数器 34
        assertEquals(34L, totpUtil.matchCounterAt(
                base32Secret, totpUtil.generateCode(base32Secret, now + 30L), now));
        // ±2 窗口外 → -1
        assertEquals(-1L, totpUtil.matchCounterAt(
                base32Secret, totpUtil.generateCode(base32Secret, now + 60L), now));
        // 非法输入 → -1
        assertEquals(-1L, totpUtil.matchCounterAt(base32Secret, null, now));
        assertEquals(-1L, totpUtil.matchCounterAt(base32Secret, "abcdef", now));
    }
}
