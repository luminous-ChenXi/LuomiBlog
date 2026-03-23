package com.luomiblog.service;

import com.luomiblog.dto.site.SiteConfigDTO;

/**
 * 站点配置服务接口
 */
public interface SiteConfigService {

    /**
     * 获取公开站点配置
     *
     * @return 站点配置DTO
     */
    SiteConfigDTO getPublicConfig();

    /**
     * 获取站点 favicon
     *
     * @return favicon URL 或 SVG 代码
     */
    String getFavicon();
}
