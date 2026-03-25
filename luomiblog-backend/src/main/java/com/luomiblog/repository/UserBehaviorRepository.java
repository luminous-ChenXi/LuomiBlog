package com.luomiblog.repository;

import com.luomiblog.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {

    /**
     * 检查用户在24小时内是否已浏览过文章
     */
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.articleId = :articleId " +
           "AND ub.behaviorType = 'VIEW' " +
           "AND ((ub.userId = :userId AND :userId IS NOT NULL) OR (ub.visitorId = :visitorId AND :visitorId IS NOT NULL)) " +
           "AND ub.createdAt > :since")
    Optional<UserBehavior> findRecentView(@Param("articleId") Long articleId,
                                          @Param("userId") Long userId,
                                          @Param("visitorId") String visitorId,
                                          @Param("since") LocalDateTime since);

    /**
     * 检查用户是否已点赞文章
     */
    boolean existsByArticleIdAndUserIdAndBehaviorType(Long articleId, Long userId, UserBehavior.BehaviorType behaviorType);

    boolean existsByArticleIdAndVisitorIdAndBehaviorType(Long articleId, String visitorId, UserBehavior.BehaviorType behaviorType);

    /**
     * 统计文章的浏览次数（去重）
     */
    @Query("SELECT COUNT(DISTINCT COALESCE(ub.userId, ub.visitorId)) FROM UserBehavior ub " +
           "WHERE ub.articleId = :articleId AND ub.behaviorType = 'VIEW'")
    long countUniqueViews(@Param("articleId") Long articleId);

    /**
     * 统计某行为类型在某时间点之后的记录数
     */
    long countByBehaviorTypeAndCreatedAtAfter(UserBehavior.BehaviorType behaviorType, java.time.LocalDateTime dateTime);

    /**
     * 统计某行为类型在某时间范围内的记录数
     */
    long countByBehaviorTypeAndCreatedAtBetween(UserBehavior.BehaviorType behaviorType, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
