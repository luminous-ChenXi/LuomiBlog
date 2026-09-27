package com.luomiblog.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * TOTP（RFC 6238）工具：手写实现，零第三方依赖。
 *
 * <p>规范要点：HMAC-SHA1、6 位数字、30 秒时间步长、±1 窗口容差；
 * 动态截断（dynamic truncation）遵循 RFC 4226 第 5.3 节。</p>
 */
@Component
public class TotpUtil {

    /** 时间步长：30 秒（RFC 6238 默认） */
    private static final long TIME_STEP_SECONDS = 30;
    /** 验证窗口容差：±1 步（允许时钟轻微偏移） */
    private static final int WINDOW = 1;
    /** 验证码位数 */
    private static final int CODE_DIGITS = 6;
    /** HMAC 算法（RFC 6238 的 TOTP-SHA1，即 Google Authenticator 兼容模式） */
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    /** Base32 字母表（RFC 4648，不含填充） */
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    /** 密钥字节数：20 字节（RFC 4226 推荐，即 160 位） */
    private static final int SECRET_BYTES = 20;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成 20 字节随机密钥并 Base32 编码（无填充）。
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 构造 otpauth:// URI（Google Authenticator 兼容格式）。
     *
     * @param siteName 站点名称（issuer 标签的一部分）
     * @param username 用户名
     * @param secret   Base32 密钥
     */
    public String buildOtpauthUri(String siteName, String username, String secret) {
        String issuer = "LuomiBlog";
        String label = URLEncoder.encode(
                (siteName == null || siteName.isBlank() ? issuer : siteName) + ":" + username,
                StandardCharsets.UTF_8);
        String issuerParam = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s", label, secret, issuerParam);
    }

    /**
     * 按给定 Unix 时间（秒）计算 6 位 TOTP 码。
     * 供验证逻辑与单元测试（RFC 6238 附录 B 标准向量）复用。
     *
     * @param base32Secret Base32 密钥
     * @param unixSeconds  Unix 时间戳（秒）
     */
    public String generateCode(String base32Secret, long unixSeconds) {
        long counter = unixSeconds / TIME_STEP_SECONDS;
        return generateCodeForCounter(base32Secret, counter);
    }

    /**
     * 校验当前 6 位验证码，允许 ±1 个时间窗口。
     * 使用常数时间比较防时序攻击。
     */
    public boolean verifyCode(String base32Secret, String code) {
        return verifyCodeAt(base32Secret, code, System.currentTimeMillis() / 1000L);
    }

    /**
     * 按给定 Unix 时间（秒）校验 6 位验证码，允许 ±1 个时间窗口。
     * 供验证逻辑与单元测试（窗口容差）复用。
     */
    public boolean verifyCodeAt(String base32Secret, String code, long unixSeconds) {
        if (base32Secret == null || base32Secret.isBlank()
                || code == null || !code.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }
        long currentStep = unixSeconds / TIME_STEP_SECONDS;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            String candidate = generateCodeForCounter(base32Secret, currentStep + i);
            if (constantTimeEquals(candidate, code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按计数器计算 TOTP：
     * 1. 8 字节大端计数器作为 HMAC-SHA1 输入；
     * 2. 取最后一字节低 4 位为偏移 offset；
     * 3. 从 offset 起取 4 字节，去掉最高位符号位；
     * 4. 对 10^CODE_DIGITS 取模得到 6 位码。
     */
    private String generateCodeForCounter(String base32Secret, long counter) {
        byte[] key = base32Decode(base32Secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xFF);
            counter >>>= 8;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int modulus = (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", binary % modulus);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP 计算失败: " + e.getMessage(), e);
        }
    }

    /** Base32 编码（包内可见，供单元测试使用 RFC 6238 附录 B 确定性密钥） */
    String base32Encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String input) {
        String clean = input.trim().replace("=", "").toUpperCase();
        int byteCount = clean.length() * 5 / 8;
        byte[] result = new byte[byteCount];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("非法 Base32 字符: " + c);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
