package com.luomiblog.service;

import com.luomiblog.dto.ArticleRequest;
import com.luomiblog.dto.ArticleListItemResponse;
import com.luomiblog.dto.ArticleResponse;
import com.luomiblog.dto.ArticleStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ArticleService {

    ArticleResponse createArticle(ArticleRequest request, Long authorId);

    ArticleResponse updateArticle(Long id, ArticleRequest request, Long authorId);

    void deleteArticle(Long id, Long authorId);

    ArticleResponse getArticleBySlug(String slug);

    ArticleResponse getArticleById(Long id);

    Page<ArticleListItemResponse> getPublishedArticles(Pageable pageable);

    Page<ArticleListItemResponse> getArticlesByCategory(Long categoryId, Pageable pageable);

    Page<ArticleListItemResponse> getArticlesByAuthor(Long authorId, Pageable pageable);

    List<ArticleListItemResponse> searchArticles(String keyword);

    void incrementViewCount(Long articleId);

    void incrementLikeCount(Long articleId);

    Page<ArticleListItemResponse> getAdminArticles(Pageable pageable, String search, String category, String status);

    ArticleResponse publishArticle(Long id, Long authorId);

    ArticleResponse archiveArticle(Long id, Long authorId);

    ArticleResponse toggleTop(Long id, boolean top, Long authorId);

    /**
     * 获取文章统计信息
     */
    ArticleStatsResponse getArticleStats();
}
