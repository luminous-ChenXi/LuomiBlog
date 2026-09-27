package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.service.MailService;
import com.luomiblog.service.SiteSettingsService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端站点设置：站长开关（注册邮箱验证 / 登录 2FA）+ SMTP 配置 + 测试发送。
 * 仅 ADMIN 角色可访问（/api/admin/** 由 SecurityConfig 统一约束）。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final SiteSettingsService siteSettingsService;
    private final MailService mailService;

    /**
     * 读取设置（SMTP password 永不回传，仅返回 passwordSet 标记）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registrationEmailVerifyRequired",
                siteSettingsService.getBool(SiteSettingsService.KEY_EMAIL_VERIFY_REQUIRED, false));
        result.put("loginTotpRequired",
                siteSettingsService.getBool(SiteSettingsService.KEY_TOTP_REQUIRED, false));
        result.put("smtpConfigured", siteSettingsService.isSmtpConfigured());
        result.put("smtp", smtpView());
        return ApiResponse.success(result);
    }

    /**
     * 保存站长开关。
     * 注意：未配置 SMTP 时邮箱验证开关允许开启（可先行打开），但响应携带 warning 标记。
     */
    @PutMapping("/switches")
    public ApiResponse<Map<String, Object>> updateSwitches(@RequestBody Map<String, Boolean> request) {
        String warning = null;
        if (request.containsKey("registrationEmailVerifyRequired")) {
            boolean enable = Boolean.TRUE.equals(request.get("registrationEmailVerifyRequired"));
            siteSettingsService.set(SiteSettingsService.KEY_EMAIL_VERIFY_REQUIRED, String.valueOf(enable));
            if (enable && !siteSettingsService.isSmtpConfigured()) {
                warning = "邮箱验证已开启，但 SMTP 尚未配置，新用户将无法收到验证邮件；请尽快在下方配置 SMTP";
            }
        }
        if (request.containsKey("loginTotpRequired")) {
            siteSettingsService.set(SiteSettingsService.KEY_TOTP_REQUIRED,
                    String.valueOf(Boolean.TRUE.equals(request.get("loginTotpRequired"))));
        }
        log.info("站长开关已更新: {} warning={}", request, warning);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registrationEmailVerifyRequired",
                siteSettingsService.getBool(SiteSettingsService.KEY_EMAIL_VERIFY_REQUIRED, false));
        result.put("loginTotpRequired",
                siteSettingsService.getBool(SiteSettingsService.KEY_TOTP_REQUIRED, false));
        result.put("smtpConfigured", siteSettingsService.isSmtpConfigured());
        result.put("warning", warning);
        return ApiResponse.success(result);
    }

    /**
     * 保存 SMTP 配置（password 留空表示沿用原密码）
     */
    @PutMapping("/smtp")
    public ApiResponse<Map<String, Object>> updateSmtp(@RequestBody Map<String, Object> request) {
        Map<String, String> toSave = new LinkedHashMap<>();
        toSave.put(SiteSettingsService.KEY_SMTP_HOST, str(request.get("host")));
        toSave.put(SiteSettingsService.KEY_SMTP_PORT, str(request.get("port") == null ? "587" : request.get("port")));
        toSave.put(SiteSettingsService.KEY_SMTP_USERNAME, str(request.get("username")));
        toSave.put(SiteSettingsService.KEY_SMTP_SSL, str(request.get("ssl") == null ? "true" : request.get("ssl")));
        toSave.put(SiteSettingsService.KEY_SMTP_FROM, str(request.get("from")));

        String password = str(request.get("password"));
        if (!password.isBlank()) {
            toSave.put(SiteSettingsService.KEY_SMTP_PASSWORD, password);
        }
        siteSettingsService.setAll(toSave);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("smtp", smtpView());
        result.put("smtpConfigured", siteSettingsService.isSmtpConfigured());
        return ApiResponse.success(result);
    }

    /**
     * 发送测试邮件（使用已保存的 SMTP 配置）
     */
    @PostMapping("/smtp/test")
    public ApiResponse<Map<String, Object>> testSmtp(@RequestBody TestSmtpRequest request) {
        if (!siteSettingsService.isSmtpConfigured()) {
            return ApiResponse.error(400, "请先保存 SMTP 配置");
        }
        try {
            mailService.sendMail(request.getTo(), "LuomiBlog SMTP 测试邮件",
                    "<p>这是一封来自 LuomiBlog 管理后台的 SMTP 测试邮件，收到即说明邮件服务配置成功。</p>");
            return ApiResponse.success(Map.of("success", true, "message", "测试邮件已发送至 " + request.getTo()));
        } catch (Exception e) {
            log.warn("SMTP 测试发送失败: {}", e.getMessage());
            return ApiResponse.error(500, "测试邮件发送失败: " + e.getMessage());
        }
    }

    /** SMTP 脱敏视图：password 只给 passwordSet 标记 */
    private Map<String, Object> smtpView() {
        Map<String, Object> smtp = new LinkedHashMap<>();
        smtp.put("host", siteSettingsService.getString(SiteSettingsService.KEY_SMTP_HOST, ""));
        smtp.put("port", siteSettingsService.getInt(SiteSettingsService.KEY_SMTP_PORT, 587));
        smtp.put("username", siteSettingsService.getString(SiteSettingsService.KEY_SMTP_USERNAME, ""));
        smtp.put("ssl", siteSettingsService.getBool(SiteSettingsService.KEY_SMTP_SSL, true));
        smtp.put("from", siteSettingsService.getString(SiteSettingsService.KEY_SMTP_FROM, ""));
        smtp.put("passwordSet", !siteSettingsService.getString(SiteSettingsService.KEY_SMTP_PASSWORD, "").isBlank());
        return smtp;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Data
    public static class TestSmtpRequest {
        /** 测试邮件收件人 */
        @NotBlank(message = "收件邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String to;
    }
}
