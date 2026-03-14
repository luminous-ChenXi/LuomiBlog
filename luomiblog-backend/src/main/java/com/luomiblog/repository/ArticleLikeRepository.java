package com.luomiblog.repository;

import com.luomiblog.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    Optional<ArticleLike> findByArticleIdAndUserId(Long articleId, Long userId);

    Optional<ArticleLike> findByArticleIdAndVisitorId(Long articleId, String visitorId);

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    boolean existsByArticleIdAndVisitorId(Long articleId, String visitorId);

    long countByArticleId(Long articleId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleIdAndVisitorId(Long articleId, String visitorId);
}
