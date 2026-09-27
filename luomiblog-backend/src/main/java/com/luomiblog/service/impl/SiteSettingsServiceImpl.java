package com.luomiblog.service.impl;

import com.luomiblog.entity.SiteSetting;
import com.luomiblog.repository.SiteSettingRepository;
import com.luomiblog.service.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * 站点设置服务实现：system_config 之外的扩展配置统一落 site_settings 键值表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSettingsServiceImpl implements SiteSettingsService {

    private final SiteSettingRepository siteSettingRepository;

    @Override
    public String getString(String key, String defaultValue) {
        try {
            Optional<SiteSetting> setting = siteSettingRepository.findById(key);
            String value = setting.map(SiteSetting::getValue).orElse(null);
            return (value == null || value.isEmpty()) ? defaultValue : value;
        } catch (Exception e) {
            log.debug("读取站点设置失败（表可能未初始化）: {} - {}", key, e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public boolean getBool(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    @Transactional
    public void set(String key, String value) {
        SiteSetting setting = siteSettingRepository.findById(key).orElseGet(() -> {
            SiteSetting s = new SiteSetting();
            s.setName(key);
            return s;
        });
        setting.setValue(value);
        siteSettingRepository.save(setting);
    }

    @Override
    @Transactional
    public void setAll(Map<String, String> values) {
        values.forEach(this::set);
    }

    @Override
    public boolean isSmtpConfigured() {
        String host = getString(KEY_SMTP_HOST, "");
        String username = getString(KEY_SMTP_USERNAME, "");
        return !host.isBlank() && !username.isBlank();
    }
}
