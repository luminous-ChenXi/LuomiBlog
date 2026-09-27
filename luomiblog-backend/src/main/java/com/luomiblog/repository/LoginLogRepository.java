package com.luomiblog.repository;

import com.luomiblog.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 登录日志仓库（安全审计）
 */
@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
