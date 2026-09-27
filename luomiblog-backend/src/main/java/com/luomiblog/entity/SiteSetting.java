package com.luomiblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 站点设置键值实体（WordPress 式 wp_options 风格）。
 *
 * <p>键约定：{@code registration.email_verify_required}、{@code login.totp_required}、
 * {@code smtp.host} / {@code smtp.port} / {@code smtp.username} / {@code smtp.password} /
 * {@code smtp.ssl} / {@code smtp.from}。仅 ADMIN 角色可修改。</p>
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "site_settings")
public class SiteSetting {

    @Id
    @Column(name = "name", length = 191, nullable = false)
    private String name;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
