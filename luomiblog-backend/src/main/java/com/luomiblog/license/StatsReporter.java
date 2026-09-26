package com.luomiblog.license;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 统计上报器（N2 占位）
 *
 * chenxi.stats.enabled=false（默认）时为空实现 stub：所有调用直接返回，无任何副作用。
 * 启用时也仅做尽力而为（best-effort）上报：任何失败都被吞掉并降级为调试日志，
 * 绝不影响业务请求的成败与耗时（同步调用外层必须自行评估是否异步）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsReporter {

    private final StatsProperties properties;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    @PostConstruct
    void init() {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 上报一个统计事件（N2 占位：enabled=false 时为空操作）
     *
     * @param eventType 事件类型（如 article_view / install_complete）
     * @param payload   事件负载（无敏感信息，调用方负责脱敏）
     */
    public void report(String eventType, Map<String, Object> payload) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getReportUrl())) {
            log.debug("统计上报未启用，忽略事件: {}", eventType);
            return;
        }
        try {
            restClient.post()
                    .uri(properties.getReportUrl())
                    .body(Map.of("event", eventType, "payload", payload == null ? Map.of() : payload))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // 上报失败永远不影响业务
            log.debug("统计上报失败（已忽略）: event={} error={}", eventType, e.getMessage());
        }
    }
}
