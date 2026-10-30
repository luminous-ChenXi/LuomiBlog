package com.luomiblog.repository;

import com.luomiblog.entity.AuthLockState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthLockStateRepository extends JpaRepository<AuthLockState, Long> {

    Optional<AuthLockState> findByLockScopeAndIdentifier(AuthLockState.LockScope lockScope, String identifier);

    /**
     * 惰性清理过期行：锁定已过期超保留期、且计数窗口早已关闭的行可安全删除（防表膨胀）。
     * [BUG-017 同范式] JPQL 批量删除，避免派生 delete 先全量加载实体再逐条删。
     */
    @Modifying
    @Query("DELETE FROM AuthLockState a WHERE "
            + "(a.lockedUntil IS NOT NULL AND a.lockedUntil < :cutoff) "
            + "OR (a.lockedUntil IS NULL AND a.windowStartedAt < :cutoff)")
    int deleteStaleRows(@Param("cutoff") LocalDateTime cutoff);
}
