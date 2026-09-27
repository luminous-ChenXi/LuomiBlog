package com.luomiblog.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * 必填：至少 32 字符随机串（通过环境变量 JWT_SECRET 注入，无公开默认值）。
     * 启动时由 @Validated 校验非空与最小长度，缺失或不达标直接启动失败。
     */
    @NotBlank
    @Size(min = 32)
    private String secret;

    private long expiration = 86400000L;
}
