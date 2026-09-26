package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.ArticleResponse;
import com.luomiblog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章公开读接口（仅 GET）
 * 写操作统一走 /api/admin/articles（AdminArticleController），
 * 浏览/点赞统计走 /{articleId}/view、/{articleId}/like（ArticleStatsController）
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ApiResponse<Page<ArticleResponse>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ApiResponse.success(articleService.getPublishedArticles(pageable));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ArticleResponse> getArticleBySlug(@PathVariable String slug) {
        return ApiResponse.success(articleService.getArticleBySlug(slug));
    }

    @GetMapping("/id/{id}")
    public ApiResponse<ArticleResponse> getArticleById(@PathVariable Long id) {
        return ApiResponse.success(articleService.getArticleById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<Page<ArticleResponse>> getArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ApiResponse.success(articleService.getArticlesByCategory(categoryId, pageable));
    }

    @GetMapping("/search")
    public ApiResponse<List<ArticleResponse>> searchArticles(@RequestParam String keyword) {
        return ApiResponse.success(articleService.searchArticles(keyword));
    }
}
