package com.luomiblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 登录日志（安全审计）：密码登录成功/失败、2FA 验证成功各记一条。
 * 表结构见 db/schema.sql 的 login_logs；存量库由 V4__normalize.sql 创建。
 */
@Entity
@Table(name = "login_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    /** 登录方式：password / chenxi_sso / totp */
    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 20)
    private LoginType loginType;

    @Column(name = "success", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean success;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 登录方式。枚举常量刻意用小写：@Enumerated(EnumType.STRING) 直接落枚举名，
     * 必须与 db/schema.sql / V4 中 login_type 的 ENUM('password','chenxi_sso','totp')
     * 字面值完全一致（大写常量会在存量库 ENUM 表上写入失败/被 Hibernate 改写列定义）。
     */
    public enum LoginType {
        /** 密码登录 */
        password,
        /** 辰汐通行证 SSO */
        chenxi_sso,
        /** TOTP 两步验证（6 位码 / 还原码） */
        totp
    }
}
