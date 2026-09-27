package com.luomiblog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret = "luomiblog-default-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private long expiration = 86400000L;
}
