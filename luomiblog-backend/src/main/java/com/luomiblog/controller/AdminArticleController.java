package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.ArticleRequest;
import com.luomiblog.dto.ArticleResponse;
import com.luomiblog.dto.ArticleStatsResponse;
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

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminArticleController {

    private final ArticleService articleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<Page<ArticleResponse>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(articleService.getAdminArticles(pageable, search, category, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<ArticleResponse> createArticle(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.createArticle(request, userPrincipal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.updateArticle(id, request, userPrincipal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<Void> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        articleService.deleteArticle(id, userPrincipal.getId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<ArticleResponse> publishArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.publishArticle(id, userPrincipal.getId()));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<ArticleResponse> archiveArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.archiveArticle(id, userPrincipal.getId()));
    }

    @PostMapping("/{id}/top")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<ArticleResponse> toggleTop(
            @PathVariable Long id,
            @RequestParam boolean top,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(articleService.toggleTop(id, top, userPrincipal.getId()));
    }

    /**
     * 获取文章统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOGGER')")
    public ApiResponse<ArticleStatsResponse> getArticleStats() {
        return ApiResponse.success(articleService.getArticleStats());
    }
}
