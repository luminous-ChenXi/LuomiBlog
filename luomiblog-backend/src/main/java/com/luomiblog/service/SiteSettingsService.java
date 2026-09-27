package com.luomiblog.service;

import java.util.Map;

/**
 * 站点设置服务（WordPress 式键值存储）
 *
 * <p>存储键：registration.email_verify_required、login.totp_required、smtp.*；
 * 仅 ADMIN 角色可修改。</p>
 */
public interface SiteSettingsService {

    /* ---------- 常用设置键 ---------- */
    String KEY_EMAIL_VERIFY_REQUIRED = "registration.email_verify_required";
    String KEY_TOTP_REQUIRED = "login.totp_required";
    String KEY_SMTP_HOST = "smtp.host";
    String KEY_SMTP_PORT = "smtp.port";
    String KEY_SMTP_USERNAME = "smtp.username";
    String KEY_SMTP_PASSWORD = "smtp.password";
    String KEY_SMTP_SSL = "smtp.ssl";
    String KEY_SMTP_FROM = "smtp.from";
    String KEY_SITE_NAME = "site.name";
    String KEY_SITE_DESCRIPTION = "site.description";
    String KEY_SITE_DEFAULT_THEME = "site.default_theme";
    String KEY_SITE_DEFAULT_LANGUAGE = "site.default_language";
    String KEY_SITE_TIMEZONE = "site.timezone";

    /** 读取字符串值，键不存在或为空时返回默认值 */
    String getString(String key, String defaultValue);

    /** 读取布尔值（"true"/"1" 视为 true），键不存在时返回默认值 */
    boolean getBool(String key, boolean defaultValue);

    /** 读取整数值，非法或不存在时返回默认值 */
    int getInt(String key, int defaultValue);

    /** 写入单个设置 */
    void set(String key, String value);

    /** 批量写入设置 */
    void setAll(Map<String, String> values);

    /**
     * SMTP 是否已完成配置：host 与 from 均非空即视为已配置，
     * 兼容无鉴权 SMTP（MailPit/内网中继）场景
     */
    boolean isSmtpConfigured();
}
