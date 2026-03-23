package com.luomiblog.service.impl;

import com.luomiblog.dto.site.SiteConfigDTO;
import com.luomiblog.entity.SystemConfig;
import com.luomiblog.repository.SystemConfigRepository;
import com.luomiblog.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 站点配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public SiteConfigDTO getPublicConfig() {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(1L);

        if (configOpt.isPresent()) {
            SystemConfig config = configOpt.get();
            return SiteConfigDTO.builder()
                    .siteName(config.getSiteName())
                    .siteDescription(config.getSiteDescription())
                    .siteLogo(config.getSiteLogo())
                    .siteFavicon(config.getSiteFavicon())
                    .defaultLanguage(config.getDefaultLanguage())
                    .defaultTheme(config.getDefaultTheme())
                    .icp(config.getIcp())
                    .seoTitle(config.getSeoTitle())
                    .seoKeywords(config.getSeoKeywords())
                    .seoDescription(config.getSeoDescription())
                    .build();
        }

        // 返回默认配置
        return SiteConfigDTO.builder()
                .siteName("LuomiBlog")
                .siteDescription("程序员向AI原生增强型知识库博客")
                .defaultLanguage("zh")
                .defaultTheme("auto")
                .build();
    }

    @Override
    public String getFavicon() {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(1L);
        return configOpt.map(SystemConfig::getSiteFavicon).orElse(null);
    }
}
