package com.luomiblog.controller;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.common.ClientIpResolver;
import com.luomiblog.dto.LikeRequest;
import com.luomiblog.dto.LikeResponse;
import com.luomiblog.security.UserPrincipal;
import com.luomiblog.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final ClientIpResolver clientIpResolver;
    private final com.luomiblog.service.RateLimitService rateLimitService;

    @PostMapping("/article")
    public ApiResponse<LikeResponse> toggleArticleLike(
            @Valid @RequestBody LikeRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
            HttpServletRequest httpRequest) {
        String ipAddress = clientIpResolver.resolve(httpRequest);
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        // 限流：60 次/分钟/身份（阈值见 app.rate-limit.interaction）
        rateLimitService.checkInteraction(interactionIdentity(userId, visitorId, ipAddress));

        return ApiResponse.success(likeService.toggleArticleLike(request, userId, visitorId, ipAddress));
    }

    @PostMapping("/comment")
    public ApiResponse<LikeResponse> toggleCommentLike(
            @Valid @RequestBody LikeRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId,
            HttpServletRequest httpRequest) {
        String ipAddress = clientIpResolver.resolve(httpRequest);
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        // 限流：60 次/分钟/身份
        rateLimitService.checkInteraction(interactionIdentity(userId, visitorId, ipAddress));

        return ApiResponse.success(likeService.toggleCommentLike(request, userId, visitorId, ipAddress));
    }

    @GetMapping("/article/{articleId}")
    public ApiResponse<LikeResponse> getArticleLikeStatus(
            @PathVariable Long articleId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        return ApiResponse.success(likeService.getArticleLikeStatus(articleId, userId, visitorId));
    }

    @GetMapping("/comment/{commentId}")
    public ApiResponse<LikeResponse> getCommentLikeStatus(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        return ApiResponse.success(likeService.getCommentLikeStatus(commentId, userId, visitorId));
    }

    /** 限流身份键：登录用户按 userId，访客按 visitorId，兜底 IP */
    private String interactionIdentity(Long userId, String visitorId, String ipAddress) {
        if (userId != null) {
            return "u:" + userId;
        }
        if (visitorId != null && !visitorId.isBlank()) {
            return "v:" + visitorId;
        }
        return "ip:" + ipAddress;
    }
}
