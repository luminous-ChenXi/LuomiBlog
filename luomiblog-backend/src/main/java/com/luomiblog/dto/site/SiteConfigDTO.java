package com.luomiblog.dto.site;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站点配置DTO
 * 公开的站点配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfigDTO {

    /**
     * 站点名称
     */
    private String siteName;

    /**
     * 站点描述
     */
    private String siteDescription;

    /**
     * 站点Logo
     */
    private String siteLogo;

    /**
     * 站点Favicon
     */
    private String siteFavicon;

    /**
     * 默认语言
     */
    private String defaultLanguage;

    /**
     * 默认主题
     */
    private String defaultTheme;

    /**
     * ICP备案号
     */
    private String icp;

    /**
     * SEO标题
     */
    private String seoTitle;

    /**
     * SEO关键词
     */
    private String seoKeywords;

    /**
     * SEO描述
     */
    private String seoDescription;
}
