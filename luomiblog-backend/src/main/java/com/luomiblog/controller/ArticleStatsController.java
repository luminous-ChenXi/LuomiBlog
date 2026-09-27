package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.ArticleStatsResult;
import com.luomiblog.dto.MyFavoritesResponse;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.ArticleStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleStatsController {

    private final ArticleStatsService articleStatsService;

    @PostMapping("/{articleId}/view")
    public ApiResponse<ArticleStatsResult> recordView(
            @PathVariable Long articleId,
            @RequestBody ViewRecordRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String ipAddress = getClientIpAddress(httpRequest);
        String visitorId = request.getVisitorId();
        // 身份以 JWT principal 为准：登录用户忽略请求体里的 userId（防冒充），
        // 匿名访客落 visitorId 维度
        Long userId = userPrincipal != null ? userPrincipal.getId() : request.getUserId();

        if (visitorId == null || visitorId.isEmpty()) {
            visitorId = UUID.randomUUID().toString();
        }

        boolean recorded = articleStatsService.recordView(
                articleId, userId, visitorId, ipAddress, request.getUserAgent());

        ArticleStatsResult result = articleStatsService.getArticleStats(articleId, userId, visitorId);
        result.setSuccess(recorded);
        result.setAction("view");

        return ApiResponse.success(result);
    }

    @PostMapping("/{articleId}/like")
    public ApiResponse<ArticleStatsResult> toggleLike(
            @PathVariable Long articleId,
            @RequestBody LikeRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String ipAddress = getClientIpAddress(httpRequest);
        // 登录用户身份以 JWT principal 为准（保证 article_likes.user_id 落库正确），
        // 匿名访客按 visitorId 切换
        Long userId = userPrincipal != null ? userPrincipal.getId() : request.getUserId();

        ArticleStatsResult result = articleStatsService.toggleLike(
                articleId, userId, request.getVisitorId(), ipAddress);

        return ApiResponse.success(result);
    }

    @PostMapping("/{articleId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ArticleStatsResult> toggleFavorite(
            @PathVariable Long articleId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        ArticleStatsResult result = articleStatsService.toggleFavorite(articleId, userPrincipal.getId());
        return ApiResponse.success(result);
    }

    @GetMapping("/{articleId}/stats")
    public ApiResponse<ArticleStatsResult> getStats(
            @PathVariable Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String visitorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal != null) {
            userId = userPrincipal.getId();
        }

        ArticleStatsResult result = articleStatsService.getArticleStats(articleId, userId, visitorId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{articleId}/check")
    public ApiResponse<ArticleCheckResult> checkStatus(
            @PathVariable Long articleId,
            @RequestParam(required = false) String visitorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        ArticleCheckResult result = ArticleCheckResult.builder()
                .hasLiked(articleStatsService.hasLiked(articleId, userId, visitorId))
                .hasFavorited(articleStatsService.hasFavorited(articleId, userId))
                .build();

        return ApiResponse.success(result);
    }

    /**
     * 我的收藏列表：GET /api/articles/favorites/my?folder=&page=&size=
     * URL 层因 GET /api/articles/** 放行，方法级 @PreAuthorize 强制登录
     */
    @GetMapping("/favorites/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MyFavoritesResponse> myFavorites(
            @RequestParam(required = false) String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        MyFavoritesResponse result = articleStatsService.getMyFavorites(
                userPrincipal.getId(), folder, page, size);
        return ApiResponse.success(result);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @lombok.Data
    public static class ViewRecordRequest {
        private Long userId;
        private String visitorId;
        private String userAgent;
    }

    @lombok.Data
    public static class LikeRequest {
        private Long userId;
        private String visitorId;
    }

    @lombok.Data
    @lombok.Builder
    public static class ArticleCheckResult {
        private Boolean hasLiked;
        private Boolean hasFavorited;
    }
}
