package com.luomiblog.license;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 正版授权校验配置（N2 占位，与 AstrNest 孪生项目统一规范对齐）
 *
 * 配置前缀 chenxi.license，默认 enabled=false。
 * 服务端协议目前只定义到接口层（{@link LicenseClient}），不实现服务端。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chenxi.license")
public class LicenseProperties {

    /** 正版授权校验总开关（默认关闭） */
    private boolean enabled = false;

    /** 预留：授权校验端点 */
    private String verifyUrl = "";

    /** 校验结果本地缓存天数 */
    private int cacheDays = 7;

    /** 离线宽限天数（超期仅前端横幅提示，绝不锁数据） */
    private int offlineGraceDays = 3;
}
