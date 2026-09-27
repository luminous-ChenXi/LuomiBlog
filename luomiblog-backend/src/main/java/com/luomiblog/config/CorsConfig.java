package com.luomiblog.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS 配置：单一来源为 application.yml 的 app.cors.allowed-origins
 * （逗号分隔列表，可用环境变量覆盖）。
 *
 * <p>刻意不设代码级默认列表：防止 yml 与代码双源漂移。
 * 若配置缺失导致本列表为空，启动时立即快速失败并给出明确提示。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {

    private List<String> allowedOrigins = new ArrayList<>();

    @PostConstruct
    void validateSingleSource() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins 未配置：CORS 来源必须且只能来自 application.yml（逗号分隔），"
                            + "例如 http://localhost:4321,https://blog.example.com");
        }
    }
}
