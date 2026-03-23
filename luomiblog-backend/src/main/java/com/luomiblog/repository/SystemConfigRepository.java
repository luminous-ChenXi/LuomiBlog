package com.luomiblog.repository;

import com.luomiblog.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统配置仓库
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
}
