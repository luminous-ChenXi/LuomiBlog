package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.site.SiteConfigDTO;
import com.luomiblog.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点配置控制器
 * 提供公开的站点配置信息，不需要认证
 */
@Slf4j
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    /**
     * 获取站点公开配置
     * 包括站点名称、描述、favicon等公开信息
     */
    @GetMapping("/config")
    public ApiResponse<SiteConfigDTO> getSiteConfig() {
        try {
            SiteConfigDTO config = siteConfigService.getPublicConfig();
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.warn("获取站点配置失败: {}", e.getMessage());
            // 返回默认配置
            return ApiResponse.success(SiteConfigDTO.builder()
                    .siteName("LuomiBlog")
                    .siteDescription("程序员向AI原生增强型知识库博客")
                    .defaultLanguage("zh")
                    .defaultTheme("auto")
                    .build());
        }
    }

    /**
     * 获取站点 favicon
     */
    @GetMapping("/favicon")
    public ApiResponse<String> getFavicon() {
        try {
            String favicon = siteConfigService.getFavicon();
            return ApiResponse.success(favicon);
        } catch (Exception e) {
            log.warn("获取 favicon 失败: {}", e.getMessage());
            return ApiResponse.success(null);
        }
    }
}
