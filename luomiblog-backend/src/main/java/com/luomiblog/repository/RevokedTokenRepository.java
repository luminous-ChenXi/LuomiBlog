package com.luomiblog.repository;

import com.luomiblog.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenId(String tokenId);

    /** [BUG-017 同范式] 批量 JPQL 删除过期行（派生 delete 会先全量加载实体再逐条删） */
    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
}
