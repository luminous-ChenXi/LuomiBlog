package com.luomiblog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {

    private List<String> allowedOrigins = List.of(
            "http://localhost:4321",
            "http://localhost:4322",
            "http://localhost:3000",
            "http://127.0.0.1:4321",
            "http://127.0.0.1:4322",
            "http://127.0.0.1:3000"
    );
}
