package com.luomiblog.repository;

import com.luomiblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.articleId = :articleId AND c.status = 'approved' AND c.deletedAt IS NULL AND c.parentId IS NULL ORDER BY c.isTop DESC, c.createdAt DESC")
    Page<Comment> findRootCommentsByArticleId(@Param("articleId") Long articleId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.parentId = :parentId AND c.status = 'approved' AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM Comment c WHERE c.articleId = :articleId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findAllByArticleId(@Param("articleId") Long articleId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.articleId = :articleId AND c.status = 'approved' AND c.deletedAt IS NULL")
    Long countApprovedByArticleId(@Param("articleId") Long articleId);

    @Query("SELECT c FROM Comment c WHERE c.status = 'pending' AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findPendingComments(Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.userId = :userId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findByUserId(@Param("userId") Long userId, Pageable pageable);

    Page<Comment> findByArticleIdAndStatusAndDeletedAtIsNull(Long articleId, String status, Pageable pageable);

    List<Comment> findByArticleIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(Long articleId, String status);

    Page<Comment> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    /**
     * 统计创建时间在某时间点之前的评论数
     */
    long countByCreatedAtBefore(java.time.LocalDateTime dateTime);

    // ==================== [BUG-017 修复] 原子计数 ====================

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    int incrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = CASE WHEN c.replyCount > 0 THEN c.replyCount - 1 ELSE 0 END WHERE c.id = :id")
    int decrementReplyCount(@Param("id") Long id);

    /** 轻量读取计数值（避免为拿一个 int 加载整行） */
    @Query("SELECT c.likeCount FROM Comment c WHERE c.id = :id")
    Optional<Integer> getLikeCountById(@Param("id") Long id);
}
