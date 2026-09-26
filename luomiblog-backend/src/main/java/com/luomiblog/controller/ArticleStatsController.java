package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.common.ClientIpResolver;
import com.luomiblog.dto.ArticleStatsResult;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.ArticleStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleStatsController {

    private final ArticleStatsService articleStatsService;
    private final ClientIpResolver clientIpResolver;

    /**
     * 记录文章浏览（24小时去重）
     * 注意：用户归属只信任服务端登录态，不信任请求体中的 userId
     */
    @PostMapping("/{articleId}/view")
    public ApiResponse<ArticleStatsResult> recordView(
            @PathVariable Long articleId,
            @RequestBody(required = false) ViewRecordRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {

        String ipAddress = clientIpResolver.resolve(httpRequest);
        String visitorId = request != null ? request.getVisitorId() : null;
        // 归属用户只从登录态获取，匿名时为 null
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        // 如果没有visitorId，生成一个
        if (visitorId == null || visitorId.isEmpty()) {
            visitorId = UUID.randomUUID().toString();
        }

        String userAgent = request != null ? request.getUserAgent() : null;

        boolean recorded = articleStatsService.recordView(
                articleId, userId, visitorId, ipAddress, userAgent);

        ArticleStatsResult result = articleStatsService.getArticleStats(articleId, userId, visitorId);
        result.setSuccess(recorded);
        result.setAction("view");

        return ApiResponse.success(result);
    }

    /**
     * 切换点赞状态
     * 注意：用户归属只信任服务端登录态，不信任请求体中的 userId
     */
    @PostMapping("/{articleId}/like")
    public ApiResponse<ArticleStatsResult> toggleLike(
            @PathVariable Long articleId,
            @RequestBody(required = false) LikeRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {

        String ipAddress = clientIpResolver.resolve(httpRequest);
        String visitorId = request != null ? request.getVisitorId() : null;
        // 归属用户只从登录态获取，匿名时为 null
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        ArticleStatsResult result = articleStatsService.toggleLike(
                articleId, userId, visitorId, ipAddress);

        return ApiResponse.success(result);
    }

    /**
     * 切换收藏状态（需要登录）
     */
    @PostMapping("/{articleId}/favorite")
    public ApiResponse<ArticleStatsResult> toggleFavorite(
            @PathVariable Long articleId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ApiResponse.error(401, "请先登录");
        }

        ArticleStatsResult result = articleStatsService.toggleFavorite(articleId, userPrincipal.getId());
        return ApiResponse.success(result);
    }

    /**
     * 获取文章统计信息
     */
    @GetMapping("/{articleId}/stats")
    public ApiResponse<ArticleStatsResult> getStats(
            @PathVariable Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String visitorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 优先使用登录用户ID
        if (userPrincipal != null) {
            userId = userPrincipal.getId();
        }

        ArticleStatsResult result = articleStatsService.getArticleStats(articleId, userId, visitorId);
        return ApiResponse.success(result);
    }

    /**
     * 检查当前用户是否已点赞/收藏
     */
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

    // DTO classes
    @lombok.Data
    public static class ViewRecordRequest {
        private Long userId;      // 已废弃：仅保留兼容旧客户端，服务端不信任该字段
        private String visitorId;
        private String userAgent;
    }

    @lombok.Data
    public static class LikeRequest {
        private Long userId;      // 已废弃：仅保留兼容旧客户端，服务端不信任该字段
        private String visitorId;
    }

    @lombok.Data
    @lombok.Builder
    public static class ArticleCheckResult {
        private Boolean hasLiked;
        private Boolean hasFavorited;
    }
}
