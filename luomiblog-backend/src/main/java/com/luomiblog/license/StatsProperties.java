package com.luomiblog.license;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 统计上报配置（N2 占位，与 AstrNest 孪生项目统一规范对齐）
 *
 * 配置前缀 chenxi.stats，默认 enabled=false。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chenxi.stats")
public class StatsProperties {

    /** 统计上报总开关（默认关闭） */
    private boolean enabled = false;

    /** 预留：统计上报端点 */
    private String reportUrl = "";
}
