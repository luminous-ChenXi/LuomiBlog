package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.ArticleRequest;
import com.luomiblog.dto.ArticleResponse;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:create')")
    public ApiResponse<ArticleResponse> createArticle(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.createArticle(request, userPrincipal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:update')")
    public ApiResponse<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.updateArticle(id, request, userPrincipal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER') and hasAuthority('PERM_article:delete')")
    public ApiResponse<Void> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        articleService.deleteArticle(id, userPrincipal.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/view")
    public ApiResponse<Void> incrementViewCount(@PathVariable Long id) {
        articleService.incrementViewCount(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Void> incrementLikeCount(@PathVariable Long id) {
        articleService.incrementLikeCount(id);
        return ApiResponse.success();
    }
}
