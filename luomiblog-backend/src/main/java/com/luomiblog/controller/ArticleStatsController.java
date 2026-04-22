package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.dto.ArticleStatsResult;
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
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String visitorId = request.getVisitorId();
        Long userId = request.getUserId();

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
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);

        ArticleStatsResult result = articleStatsService.toggleLike(
                articleId, request.getUserId(), request.getVisitorId(), ipAddress);

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
