package com.luomiblog.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 验证码/敏感挑战值的哈希存储器（审计 Round3 纵深防御）：
 *
 * <p>邮箱验证码等一次性凭据此前以明文存于内存缓存，缓存内容泄露（堆转储/进程内注入）
 * 即可直接通过验证。现统一以 HmacSHA256(code, pepper) 摘要存储，比较走
 * {@link MessageDigest#isEqual} 恒定时间比较，关闭时序侧信道。</p>
 *
 * <p><b>pepper 来源</b>：环境变量 {@code SECURITY_CODE_PEPPER}（application.yml 键
 * {@code app.security.code-pepper}）。未配置时回落为启动期随机生成的进程级 pepper——
 * 语义变化：重启后旧验证码/待确认 2FA 绑定失效，用户需重新获取（一次性凭据，可接受）。</p>
 *
 * <p>注意：pepper 长度建议 >= 32 字符随机串；一旦配置请保持稳定，更换 pepper
 * 等效于作废全部现存验证码。</p>
 */
@Slf4j
@Component
public class SecurityCodeHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 进程级 pepper：环境变量优先，为空则启动时随机生成（重启即更换） */
    private final byte[] pepper;

    public SecurityCodeHasher(
            @Value("${app.security.code-pepper:${SECURITY_CODE_PEPPER:}}") String configuredPepper) {
        if (configuredPepper != null && !configuredPepper.isBlank()) {
            this.pepper = configuredPepper.getBytes(StandardCharsets.UTF_8);
            log.info("验证码 pepper 已从环境变量加载（SECURITY_CODE_PEPPER）");
        } else {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.pepper = random;
            log.warn("未配置 SECURITY_CODE_PEPPER，已使用启动期随机 pepper："
                    + "重启后旧验证码/待确认 2FA 绑定将失效（一次性凭据，不影响已绑定会话）");
        }
    }

    /**
     * 计算验证码的 HmacSHA256 摘要（小写 hex，64 字符）。缓存侧只允许存摘要。
     *
     * @param code 原始验证码（不可为 null）
     */
    public String hash(String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(
                    mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // HmacSHA256 为 JVM 标准算法，此处仅为受检异常兜底
            throw new IllegalStateException("验证码摘要计算失败", e);
        }
    }

    /**
     * 恒定时间比较：原始码摘要与存储摘要是否一致。
     *
     * @param rawCode    用户提交的原始码（可为 null）
     * @param storedHash 缓存中的摘要（可为 null）
     */
    public boolean matches(String rawCode, String storedHash) {
        if (rawCode == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(rawCode).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
