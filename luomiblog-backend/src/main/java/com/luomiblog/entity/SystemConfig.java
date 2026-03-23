package com.luomiblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_config")
public class SystemConfig {

    @Id
    private Long id;

    @Column(name = "site_name", length = 120, nullable = false)
    private String siteName;

    @Column(name = "site_description", length = 500)
    private String siteDescription;

    @Column(name = "site_logo", length = 512)
    private String siteLogo;

    @Column(name = "site_favicon", length = 512)
    private String siteFavicon;

    @Column(name = "default_language", length = 10, nullable = false)
    private String defaultLanguage;

    @Column(name = "default_theme", length = 10, nullable = false)
    private String defaultTheme;

    @Column(name = "registration_enabled", nullable = false)
    private Boolean registrationEnabled;

    @Column(name = "comment_audit", nullable = false)
    private Boolean commentAudit;

    @Column(name = "visitor_comment", nullable = false)
    private Boolean visitorComment;

    @Column(name = "ai_moderation_enabled", nullable = false)
    private Boolean aiModerationEnabled;

    @Column(name = "max_upload_size", nullable = false)
    private Long maxUploadSize;

    @Column(name = "max_image_width")
    private Integer maxImageWidth;

    @Column(name = "max_image_height")
    private Integer maxImageHeight;

    @Column(name = "icp", length = 100)
    private String icp;

    @Column(name = "analytics_code", columnDefinition = "TEXT")
    private String analyticsCode;

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "custom_js", columnDefinition = "TEXT")
    private String customJs;

    @Column(name = "seo_title", length = 255)
    private String seoTitle;

    @Column(name = "seo_keywords", length = 500)
    private String seoKeywords;

    @Column(name = "seo_description", length = 1000)
    private String seoDescription;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
