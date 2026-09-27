package com.luomiblog.service.impl;

import com.luomiblog.dto.site.SiteConfigDTO;
import com.luomiblog.entity.SystemConfig;
import com.luomiblog.repository.SystemConfigRepository;
import com.luomiblog.service.SiteConfigService;
import com.luomiblog.service.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 站点配置服务实现
 *
 * <p>读取优先级：site_settings 键值表（安装向导/站点设置落库值，WordPress 式）
 * → system_config（id=1 初始化行）→ 内置默认值。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final SiteSettingsService siteSettingsService;

    @Override
    public SiteConfigDTO getPublicConfig() {
        // 优先读 site_settings（安装向导「站点配置」保存后立即生效，重启持久）
        String siteName = siteSettingsService.getString(SiteSettingsService.KEY_SITE_NAME, "");
        String siteDescription = siteSettingsService.getString(SiteSettingsService.KEY_SITE_DESCRIPTION, "");
        String defaultLanguage = siteSettingsService.getString(SiteSettingsService.KEY_SITE_DEFAULT_LANGUAGE, "");
        String defaultTheme = siteSettingsService.getString(SiteSettingsService.KEY_SITE_DEFAULT_THEME, "");

        // 回退 system_config 初始化行
        if (siteName.isBlank() || siteDescription.isBlank()
                || defaultLanguage.isBlank() || defaultTheme.isBlank()) {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(1L);
            if (configOpt.isPresent()) {
                SystemConfig config = configOpt.get();
                if (siteName.isBlank()) {
                    siteName = config.getSiteName();
                }
                if (siteDescription.isBlank()) {
                    siteDescription = config.getSiteDescription();
                }
                if (defaultLanguage.isBlank()) {
                    defaultLanguage = config.getDefaultLanguage();
                }
                if (defaultTheme.isBlank()) {
                    defaultTheme = config.getDefaultTheme();
                }
            }
        }

        // 最终回退内置默认值
        if (siteName == null || siteName.isBlank()) {
            siteName = "LuomiBlog";
        }
        if (siteDescription == null || siteDescription.isBlank()) {
            siteDescription = "程序员向AI原生增强型知识库博客";
        }
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            defaultLanguage = "zh";
        }
        if (defaultTheme == null || defaultTheme.isBlank()) {
            defaultTheme = "auto";
        }

        SiteConfigDTO.SiteConfigDTOBuilder builder = SiteConfigDTO.builder()
                .siteName(siteName)
                .siteDescription(siteDescription)
                .defaultLanguage(defaultLanguage)
                .defaultTheme(defaultTheme);

        // Logo/Favicon/ICP/SEO 等仍以 system_config 为准
        systemConfigRepository.findById(1L).ifPresent(config -> builder
                .siteLogo(config.getSiteLogo())
                .siteFavicon(config.getSiteFavicon())
                .icp(config.getIcp())
                .seoTitle(config.getSeoTitle())
                .seoKeywords(config.getSeoKeywords())
                .seoDescription(config.getSeoDescription()));

        return builder.build();
    }

    @Override
    public String getFavicon() {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(1L);
        return configOpt.map(SystemConfig::getSiteFavicon).orElse(null);
    }
}
