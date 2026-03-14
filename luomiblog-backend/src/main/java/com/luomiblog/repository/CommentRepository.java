package com.luomiblog.repository;

import com.luomiblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
