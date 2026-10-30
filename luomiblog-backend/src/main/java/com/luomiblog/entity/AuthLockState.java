package com.luomiblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 登录防爆破锁定状态表（审计 P1-4 修复）：失败计数与锁定持久化到 DB，重启不再清零。
 *
 * <p>三个维度互补封堵（参数与升级策略见 LoginSecurityServiceImpl）：</p>
 * <ul>
 *   <li>{@code ACCOUNT_IP}：单账号+单 IP（沿用原内存版口径，快速反馈）；</li>
 *   <li>{@code ACCOUNT}：跨 IP 账号维度——攻击者换 IP 也不再绕过账号锁；</li>
 *   <li>{@code IP_ONLY}：纯 IP 维度——单 IP 横向打多个账号的散弹式爆破受限，
 *       且按累计锁定次数逐级加长锁定时长（封顶 24h）。刻意不做 365 天永久拉黑：
 *       博客访客常共享出口 NAT，永久封 IP 误伤不可控。</li>
 * </ul>
 *
 * <p>锁定到期自动解（locked_until 判定，无解锁任务）；过期行由登录失败路径惰性清理防表膨胀。</p>
 *
 * <p>表结构见 db/schema.sql 的 auth_lock_states；存量库由 V5__auth_lock_states.sql 创建。
 * 字段与 SQL 逐列对齐（BUG-086：ddl-auto=update 与手写 SQL 双源约束）。</p>
 */
@Entity
@Table(name = "auth_lock_states",
        uniqueConstraints = @UniqueConstraint(name = "uk_auth_lock_scope_identifier",
                columnNames = {"lock_scope", "identifier"}),
        indexes = {
                @Index(name = "idx_auth_lock_locked_until", columnList = "locked_until"),
                @Index(name = "idx_auth_lock_window", columnList = "window_started_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLockState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 锁定维度。枚举顺序与 SQL ENUM 定义保持一致（Hibernate 双源建表对齐） */
    public enum LockScope { ACCOUNT_IP, ACCOUNT, IP_ONLY }

    /** 锁定维度 */
    @Enumerated(EnumType.STRING)
    @Column(name = "lock_scope", nullable = false, length = 20)
    private LockScope lockScope;

    /** 维度键：ACCOUNT_IP=原始 identifier（账号:IP）；ACCOUNT=账号小写；IP_ONLY=客户端 IP */
    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier;

    /** 当前失败计数窗口内的失败次数 */
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    /** 失败计数窗口起点：窗口过期后计数惰性清零重开 */
    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;

    /** 锁定截止时间：NULL 或已过期即未锁定（到期自动解） */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /** 累计被锁定次数（IP_ONLY 升级锁的升级依据） */
    @Column(name = "lock_count", nullable = false)
    private Integer lockCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
