package com.luomiblog.repository;

import com.luomiblog.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录日志仓库（安全审计）
 */
@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    /**
     * 删除指定时间之前的登录日志（保留任务用），返回删除条数。
     * 需在事务中执行（由 LoginLogCleanupJob / 本方法上的 @Transactional 保证）
     */
    @Modifying
    @Transactional
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
