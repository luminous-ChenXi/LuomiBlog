package com.luomiblog.repository;

import com.luomiblog.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    Optional<CommentLike> findByCommentIdAndVisitorId(Long commentId, String visitorId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndVisitorId(Long commentId, String visitorId);

    long countByCommentId(Long commentId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);

    void deleteByCommentIdAndVisitorId(Long commentId, String visitorId);
}
